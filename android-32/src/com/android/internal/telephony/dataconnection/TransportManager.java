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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.ApnType;
import android.telephony.CarrierConfigManager;
import android.telephony.data.ApnSetting;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.SparseIntArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.dataconnection.AccessNetworksManager.QualifiedNetworks;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    public static final String SYSTEM_PROPERTIES_IWLAN_OPERATION_MODE =
            "ro.telephony.iwlan_operation_mode";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef(prefix = {"IWLAN_OPERATION_MODE_"},
            value = {
                    IWLAN_OPERATION_MODE_DEFAULT,
                    IWLAN_OPERATION_MODE_LEGACY,
                    IWLAN_OPERATION_MODE_AP_ASSISTED})
    public @interface IwlanOperationMode {}

    /**
     * IWLAN default mode. On device that has IRadio 1.4 or above, it means
     * {@link #IWLAN_OPERATION_MODE_AP_ASSISTED}. On device that has IRadio 1.3 or below, it means
     * {@link #IWLAN_OPERATION_MODE_LEGACY}.
     */
    public static final String IWLAN_OPERATION_MODE_DEFAULT = "default";

    /**
     * IWLAN legacy mode. IWLAN is completely handled by the modem, and when the device is on
     * IWLAN, modem reports IWLAN as a RAT.
     */
    public static final String IWLAN_OPERATION_MODE_LEGACY = "legacy";

    /**
     * IWLAN application processor assisted mode. IWLAN is handled by the bound IWLAN data service
     * and network service separately.
     */
    public static final String IWLAN_OPERATION_MODE_AP_ASSISTED = "AP-assisted";

    private final Phone mPhone;

    private final LocalLog mLocalLog = new LocalLog(100);

    /** The available transports. Must be one or more of AccessNetworkConstants.TransportType.XXX */
    private final int[] mAvailableTransports;

    @Nullable
    private AccessNetworksManager mAccessNetworksManager;

    /**
     * The current transport of the APN type. The key is the APN type, and the value is the
     * transport.
     */
    private final Map<Integer, Integer> mCurrentTransports;

    /**
     * The preferred transport of the APN type. The key is the APN type, and the value is the
     * transport. The preferred transports are updated as soon as QNS changes the preference, while
     * the current transports are updated after handover complete.
     */
    private final Map<Integer, Integer> mPreferredTransports;

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
        mCurrentTransports = new ConcurrentHashMap<>();
        mPreferredTransports = new ConcurrentHashMap<>();
        mPendingHandoverApns = new SparseIntArray();
        mHandoverNeededEventRegistrants = new RegistrantList();
        mLogTag = TransportManager.class.getSimpleName() + "-" + mPhone.getPhoneId();

        if (isInLegacyMode()) {
            log("operates in legacy mode.");
            // For legacy mode, WWAN is the only transport to handle all data connections, even
            // the IWLAN ones.
            mAvailableTransports = new int[]{AccessNetworkConstants.TRANSPORT_TYPE_WWAN};
        } else {
            log("operates in AP-assisted mode.");
            mAccessNetworksManager = new AccessNetworksManager(phone);
            mAccessNetworksManager.registerForQualifiedNetworksChanged(this,
                    EVENT_QUALIFIED_NETWORKS_CHANGED);
            mAvailableTransports = new int[]{AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                    AccessNetworkConstants.TRANSPORT_TYPE_WLAN};
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_QUALIFIED_NETWORKS_CHANGED:
                AsyncResult ar = (AsyncResult) msg.obj;
                List<QualifiedNetworks> networks = (List<QualifiedNetworks>) ar.result;
                setPreferredTransports(networks);
                // There might be already delayed evaluate event in the queue due to fallback. Don't
                // send redudant ones.
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
        Integer previousTransport = mCurrentTransports.put(apnType, transport);
        if (previousTransport == null || previousTransport != transport) {
            logl("setCurrentTransport: apnType=" + ApnSetting.getApnTypeString(apnType)
                    + ", transport=" + AccessNetworkConstants.transportTypeToString(transport));
        }
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
            int targetTransport = getPreferredTransport(apnType);
            if (targetTransport != getCurrentTransport(apnType)) {
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
     * @return The available transports. Note that on legacy devices, the only available transport
     * would be WWAN only. If the device is configured as AP-assisted mode, the available transport
     * will always be WWAN and WLAN (even if the device is not camped on IWLAN).
     * See {@link #isInLegacyMode()} for mode details.
     */
    public synchronized @NonNull int[] getAvailableTransports() {
        return mAvailableTransports;
    }

    /**
     * @return {@code true} if the device operates in legacy mode, otherwise {@code false}.
     */
    public boolean isInLegacyMode() {
        // Get IWLAN operation mode from the system property. If the system property is configured
        // to default or not configured, the mode is tied to IRadio version. For 1.4 or above, it's
        // AP-assisted mode, for 1.3 or below, it's legacy mode.
        String mode = SystemProperties.get(SYSTEM_PROPERTIES_IWLAN_OPERATION_MODE);

        if (mode.equals(IWLAN_OPERATION_MODE_AP_ASSISTED)) {
            return false;
        } else if (mode.equals(IWLAN_OPERATION_MODE_LEGACY)) {
            return true;
        }

        return mPhone.getHalVersion().less(RIL.RADIO_HAL_VERSION_1_4);
    }

    /**
     * Get the transport based on the APN type.
     *
     * @param apnType APN type
     * @return The transport type
     */
    public int getCurrentTransport(@ApnType int apnType) {
        // In legacy mode, always route to cellular.
        if (isInLegacyMode()) {
            return AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
        }

        // If we can't find the corresponding transport, always route to cellular.
        return mCurrentTransports.get(apnType) == null
                ? AccessNetworkConstants.TRANSPORT_TYPE_WWAN : mCurrentTransports.get(apnType);
    }

    /**
     * Check if there is any APN type's current transport is on IWLAN.
     *
     * @return {@code true} if there is any APN is on IWLAN, otherwise {@code false}.
     */
    public boolean isAnyApnOnIwlan() {
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            if (getCurrentTransport(apnType) == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                return true;
            }
        }
        return false;
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

    /**
     * Get the  preferred transport.
     *
     * @param apnType APN type
     * @return The preferred transport.
     */
    public @TransportType int getPreferredTransport(@ApnType int apnType) {
        // In legacy mode, always preferred on cellular.
        if (isInLegacyMode()) {
            return AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
        }

        return mPreferredTransports.get(apnType) == null
                ? AccessNetworkConstants.TRANSPORT_TYPE_WWAN : mPreferredTransports.get(apnType);
    }

    private static @TransportType int getTransportFromAccessNetwork(int accessNetwork) {
        return accessNetwork == AccessNetworkType.IWLAN
                ? AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                : AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
    }

    private void setPreferredTransports(@NonNull List<QualifiedNetworks> networksList) {
        for (QualifiedNetworks networks : networksList) {
            // Todo: We should support zero-lengthed qualifiedNetworks in the future. It means
            // network should not be setup on either WWAN or WLAN.
            if (networks.qualifiedNetworks.length > 0) {
                int transport = getTransportFromAccessNetwork(networks.qualifiedNetworks[0]);
                mPreferredTransports.put(networks.apnType, transport);
                logl("setPreferredTransports: apnType="
                        + ApnSetting.getApnTypeString(networks.apnType)
                        + ", transport=" + AccessNetworkConstants.transportTypeToString(transport));
            }
        }
    }

    /**
     * Dump the state of transport manager
     *
     * @param fd File descriptor
     * @param printwriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printwriter, "  ");
        pw.println(mLogTag);
        pw.increaseIndent();
        pw.println("mPendingHandoverApns=" + mPendingHandoverApns);
        pw.println("current transports=");
        pw.increaseIndent();
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            pw.println(ApnSetting.getApnTypeString(apnType)
                    + ": " + AccessNetworkConstants.transportTypeToString(
                            getCurrentTransport(apnType)));
        }
        pw.decreaseIndent();
        pw.println("preferred transports=");
        pw.increaseIndent();
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            pw.println(ApnSetting.getApnTypeString(apnType)
                    + ": " + AccessNetworkConstants.transportTypeToString(
                            getPreferredTransport(apnType)));
        }

        pw.decreaseIndent();
        pw.println("isInLegacy=" + isInLegacyMode());
        pw.println("IWLAN operation mode="
                + SystemProperties.get(SYSTEM_PROPERTIES_IWLAN_OPERATION_MODE));
        pw.println("Local logs=");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.flush();
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
