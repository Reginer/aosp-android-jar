/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.dataconnection;

import android.annotation.Nullable;
import android.os.Handler;
import android.os.Message;
import android.os.RegistrantList;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation.ApnType;
import android.telephony.CarrierConfigManager;
import android.telephony.data.ApnSetting;
import android.util.LocalLog;
import android.util.SparseIntArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.AccessNetworksManager;
import com.android.internal.telephony.data.TelephonyNetworkFactory;
import com.android.telephony.Rlog;

import java.util.concurrent.TimeUnit;

/**
 * This class represents the transport manager which manages available transports (i.e. WWAN or
 * WLAN) and determine the correct transport for {@link TelephonyNetworkFactory} to handle the data
 * requests.
 *
 * The device can operate in the following modes, which is stored in the system properties
 * ro.telephony.iwlan_operation_mode. If the system properties is missing, then it's tied to
 * IRadio version. For 1.4 or above, it's AP-assisted mdoe. For 1.3 or below, it's legacy mode.
 *
 * Legacy mode:
 *      Frameworks send all data requests to the default data service, which is the cellular data
 *      service. IWLAN should be still reported as a RAT on cellular network service.
 *
 * AP-assisted mode:
 *      IWLAN is handled by IWLAN data service extending {@link android.telephony.data.DataService},
 *      IWLAN network service extending {@link android.telephony.NetworkService}, and qualified
 *      network service extending {@link android.telephony.data.QualifiedNetworksService}.
 *
 *      The following settings for service package name need to be configured properly for
 *      frameworks to bind.
 *
 *      Package name of data service:
 *          The resource overlay 'config_wlan_data_service_package' or,
 *          the carrier config
 *          {@link CarrierConfigManager#KEY_CARRIER_DATA_SERVICE_WLAN_PACKAGE_OVERRIDE_STRING}.
 *          The carrier config takes precedence over the resource overlay if both exist.
 *
 *      Package name of network service
 *          The resource overlay 'config_wlan_network_service_package' or
 *          the carrier config
 *          {@link CarrierConfigManager#KEY_CARRIER_NETWORK_SERVICE_WLAN_PACKAGE_OVERRIDE_STRING}.
 *          The carrier config takes precedence over the resource overlay if both exist.
 *
 *      Package name of qualified network service
 *          The resource overlay 'config_qualified_networks_service_package' or
 *          the carrier config
 *          {@link CarrierConfigManager#
 *          KEY_CARRIER_QUALIFIED_NETWORKS_SERVICE_PACKAGE_OVERRIDE_STRING}.
 *          The carrier config takes precedence over the resource overlay if both exist.
 */
public class TransportManager extends Handler {
    private final String mLogTag;

    private static final int EVENT_QUALIFIED_NETWORKS_CHANGED = 1;

    private static final int EVENT_EVALUATE_TRANSPORT_PREFERENCE = 2;

    // Delay the re-evaluation if transport fall back. QNS will need to quickly change the
    // preference back to the original transport to avoid another handover request.
    private static final long FALL_BACK_REEVALUATE_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(3);

    private final Phone mPhone;

    private final LocalLog mLocalLog = new LocalLog(64);

    @Nullable
    private AccessNetworksManager mAccessNetworksManager;

    /**
     * The pending handover list. This is a list of APNs that are being handover to the new
     * transport. The entry will be removed once handover is completed. The key
     * is the APN type, and the value is the target transport that the APN is handovered to.
     */
    private final SparseIntArray mPendingHandoverApns;

    /**
     * The registrants for listening data handover needed events.
     */
    private final RegistrantList mHandoverNeededEventRegistrants;

    /**
     * Handover parameters
     */
    @VisibleForTesting
    public static final class HandoverParams {
        /**
         * The callback for handover complete.
         */
        public interface HandoverCallback {
            /**
             * Called when handover is completed.
             *
             * @param success {@true} if handover succeeded, otherwise failed.
             * @param fallback {@true} if handover failed, the data connection fallback to the
             * original transport
             */
            void onCompleted(boolean success, boolean fallback);
        }

        public final @ApnType int apnType;
        public final int targetTransport;
        public final HandoverCallback callback;

        @VisibleForTesting
        public HandoverParams(int apnType, int targetTransport, HandoverCallback callback) {
            this.apnType = apnType;
            this.targetTransport = targetTransport;
            this.callback = callback;
        }
    }

    public TransportManager(Phone phone) {
        mPhone = phone;
        mPendingHandoverApns = new SparseIntArray();
        mHandoverNeededEventRegistrants = new RegistrantList();
        mLogTag = TransportManager.class.getSimpleName() + "-" + mPhone.getPhoneId();
        mAccessNetworksManager = mPhone.getAccessNetworksManager();
        mAccessNetworksManager.registerForQualifiedNetworksChanged(this,
                EVENT_QUALIFIED_NETWORKS_CHANGED);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_QUALIFIED_NETWORKS_CHANGED:
                if (!hasMessages(EVENT_EVALUATE_TRANSPORT_PREFERENCE)) {
                    sendEmptyMessage(EVENT_EVALUATE_TRANSPORT_PREFERENCE);
                }
                break;
            case EVENT_EVALUATE_TRANSPORT_PREFERENCE:
                evaluateTransportPreference();
                break;
            default:
                loge("Unexpected event " + msg.what);
                break;
        }
    }

    /**
     * Set the current transport of apn type.
     *
     * @param apnType The APN type
     * @param transport The transport. Must be WWAN or WLAN.
     */
    private synchronized void setCurrentTransport(@ApnType int apnType, int transport) {
        mAccessNetworksManager.setCurrentTransport(apnType, transport);
    }

    private boolean isHandoverPending() {
        return mPendingHandoverApns.size() > 0;
    }

    /**
     * Evaluate the preferred transport for each APN type to see if handover is needed.
     */
    private void evaluateTransportPreference() {
        // Simultaneously handover is not supported today. Preference will be re-evaluated after
        // handover completed.
        if (isHandoverPending()) return;
        logl("evaluateTransportPreference");
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            int targetTransport = mAccessNetworksManager.getPreferredTransport(apnType);
            if (targetTransport != mAccessNetworksManager.getCurrentTransport(apnType)) {
                logl("Handover started for APN type: "
                        + ApnSetting.getApnTypeString(apnType)
                        + ", target transport: "
                        + AccessNetworkConstants.transportTypeToString(targetTransport));
                mPendingHandoverApns.put(apnType, targetTransport);
                mHandoverNeededEventRegistrants.notifyResult(
                        new HandoverParams(apnType, targetTransport,
                                (success, fallback) -> {
                                    // The callback for handover completed.
                                    if (success) {
                                        logl("Handover succeeded for APN type "
                                                + ApnSetting.getApnTypeString(apnType));
                                    } else {
                                        logl("APN type "
                                                + ApnSetting.getApnTypeString(apnType)
                                                + " handover to "
                                                + AccessNetworkConstants.transportTypeToString(
                                                targetTransport) + " failed"
                                                + ", fallback=" + fallback);
                                    }

                                    long delay = 0;
                                    if (fallback) {
                                        // No need to change the preference because we should
                                        // fallback. Re-evaluate after few seconds to give QNS
                                        // some time to change the preference back to the original
                                        // transport.
                                        delay = FALL_BACK_REEVALUATE_DELAY_MILLIS;
                                    } else {
                                        // If handover succeeds or failed without falling back
                                        // to the original transport, we should move to the new
                                        // transport (even if it is failed).
                                        setCurrentTransport(apnType, targetTransport);
                                    }
                                    mPendingHandoverApns.delete(apnType);
                                    sendEmptyMessageDelayed(EVENT_EVALUATE_TRANSPORT_PREFERENCE,
                                            delay);
                                }));

                // Return here instead of processing the next APN type. The next APN type for
                // handover will be evaluate again once current handover is completed.
                return;
            }
        }
    }

    /**
     * Register for data handover needed event
     *
     * @param h The handler of the event
     * @param what The id of the event
     */
    public void registerForHandoverNeededEvent(Handler h, int what) {
        if (h != null) {
            mHandoverNeededEventRegistrants.addUnique(h, what, null);
        }
    }

    /**
     * Unregister for data handover needed event
     *
     * @param h The handler
     */
    public void unregisterForHandoverNeededEvent(Handler h) {
        mHandoverNeededEventRegistrants.remove(h);
    }

    /**
     * Registers the data throttler with DcTracker.
     */
    public void registerDataThrottler(DataThrottler dataThrottler) {
        if (mAccessNetworksManager != null) {
            mAccessNetworksManager.registerDataThrottler(dataThrottler);
        }
    }

    private void logl(String s) {
        log(s);
        mLocalLog.log(s);
    }

    private void log(String s) {
        Rlog.d(mLogTag, s);
    }

    private void loge(String s) {
        Rlog.e(mLogTag, s);
    }
}
