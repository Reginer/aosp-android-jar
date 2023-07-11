/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.telephony.data;

import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.TelephonyNetworkSpecifier;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation.ApnType;
import android.telephony.SubscriptionManager;
import android.telephony.data.ApnSetting;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.DcTracker.ReleaseNetworkType;
import com.android.internal.telephony.dataconnection.DcTracker.RequestNetworkType;
import com.android.internal.telephony.dataconnection.TransportManager.HandoverParams;
import com.android.internal.telephony.metrics.NetworkRequestsStats;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Telephony network factory is responsible for dispatching network requests from the connectivity
 * service to the data network controller.
 */
public class TelephonyNetworkFactory extends NetworkFactory {
    public final String LOG_TAG;
    protected static final boolean DBG = true;

    private static final int REQUEST_LOG_SIZE = 256;

    private static final int ACTION_NO_OP   = 0;
    private static final int ACTION_REQUEST = 1;
    private static final int ACTION_RELEASE = 2;

    private static final int TELEPHONY_NETWORK_SCORE = 50;

    @VisibleForTesting
    public static final int EVENT_ACTIVE_PHONE_SWITCH               = 1;
    @VisibleForTesting
    public static final int EVENT_SUBSCRIPTION_CHANGED              = 2;
    private static final int EVENT_NETWORK_REQUEST                  = 3;
    private static final int EVENT_NETWORK_RELEASE                  = 4;
    private static final int EVENT_DATA_HANDOVER_NEEDED             = 5;
    private static final int EVENT_DATA_HANDOVER_COMPLETED          = 6;

    private final PhoneSwitcher mPhoneSwitcher;
    private final SubscriptionController mSubscriptionController;
    private final LocalLog mLocalLog = new LocalLog(REQUEST_LOG_SIZE);

    // Key: network request. Value: the transport of DcTracker it applies to,
    // AccessNetworkConstants.TRANSPORT_TYPE_INVALID if not applied.
    private final Map<TelephonyNetworkRequest, Integer> mNetworkRequests = new HashMap<>();

    private final Map<Message, HandoverParams> mPendingHandovers = new HashMap<>();

    private final Phone mPhone;

    private AccessNetworksManager mAccessNetworksManager;

    private int mSubscriptionId;

    @VisibleForTesting
    public final Handler mInternalHandler;


    public TelephonyNetworkFactory(Looper looper, Phone phone) {
        super(looper, phone.getContext(), "TelephonyNetworkFactory[" + phone.getPhoneId()
                + "]", null);
        mPhone = phone;
        mInternalHandler = new InternalHandler(looper);

        mSubscriptionController = SubscriptionController.getInstance();
        mAccessNetworksManager = mPhone.getAccessNetworksManager();

        setCapabilityFilter(makeNetworkFilter(mSubscriptionController, mPhone.getPhoneId()));
        setScoreFilter(TELEPHONY_NETWORK_SCORE);

        mPhoneSwitcher = PhoneSwitcher.getInstance();
        LOG_TAG = "TelephonyNetworkFactory[" + mPhone.getPhoneId() + "]";

        mPhoneSwitcher.registerForActivePhoneSwitch(mInternalHandler, EVENT_ACTIVE_PHONE_SWITCH,
                null);
        if (!phone.isUsingNewDataStack()) {
            mPhone.getTransportManager().registerForHandoverNeededEvent(mInternalHandler,
                    EVENT_DATA_HANDOVER_NEEDED);
        }

        mSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        SubscriptionManager.from(mPhone.getContext()).addOnSubscriptionsChangedListener(
                mSubscriptionsChangedListener);

        register();
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    mInternalHandler.sendEmptyMessage(EVENT_SUBSCRIPTION_CHANGED);
                }
            };

    private NetworkCapabilities makeNetworkFilter(SubscriptionController subscriptionController,
            int phoneId) {
        final int subscriptionId = subscriptionController.getSubIdUsingPhoneId(phoneId);
        return makeNetworkFilter(subscriptionId);
    }

    /**
     * Build the network request filter used by this factory.
     * @param subscriptionId the subscription ID to listen to
     * @return the filter to send to the system server
     */
    // This is used by the test to simulate the behavior of the system server, which is to
    // send requests that match the network filter.
    @VisibleForTesting
    public NetworkCapabilities makeNetworkFilter(int subscriptionId) {
        final NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_DUN)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_IMS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_CBS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_IA)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_RCS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MMTEL)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_EIMS)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MCX)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_1)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_2)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_3)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_4)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_5)
                .setNetworkSpecifier(new TelephonyNetworkSpecifier.Builder()
                .setSubscriptionId(subscriptionId).build());
        return builder.build();
    }

    private class InternalHandler extends Handler {
        InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_ACTIVE_PHONE_SWITCH: {
                    onActivePhoneSwitch();
                    break;
                }
                case EVENT_SUBSCRIPTION_CHANGED: {
                    onSubIdChange();
                    break;
                }
                case EVENT_NETWORK_REQUEST: {
                    onNeedNetworkFor(msg);
                    break;
                }
                case EVENT_NETWORK_RELEASE: {
                    onReleaseNetworkFor(msg);
                    break;
                }
                case EVENT_DATA_HANDOVER_NEEDED: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    HandoverParams handoverParams = (HandoverParams) ar.result;
                    onDataHandoverNeeded(handoverParams.apnType, handoverParams.targetTransport,
                            handoverParams);
                    break;
                }
                case EVENT_DATA_HANDOVER_COMPLETED: {
                    Bundle bundle = msg.getData();
                    NetworkRequest nr = bundle.getParcelable(
                            DcTracker.DATA_COMPLETE_MSG_EXTRA_NETWORK_REQUEST);
                    boolean success = bundle.getBoolean(
                            DcTracker.DATA_COMPLETE_MSG_EXTRA_SUCCESS);
                    int transport = bundle.getInt(
                            DcTracker.DATA_COMPLETE_MSG_EXTRA_TRANSPORT_TYPE);
                    boolean fallback = bundle.getBoolean(
                            DcTracker.DATA_COMPLETE_MSG_EXTRA_HANDOVER_FAILURE_FALLBACK);
                    HandoverParams handoverParams = mPendingHandovers.remove(msg);
                    if (handoverParams != null) {
                        onDataHandoverSetupCompleted(nr, success, transport, fallback,
                                handoverParams);
                    } else {
                        logl("Handover completed but cannot find handover entry!");
                    }
                    break;
                }
            }
        }
    }

    private int getTransportTypeFromNetworkRequest(TelephonyNetworkRequest networkRequest) {
        if (PhoneFactory.getDefaultPhone().isUsingNewDataStack()) {
            int transport = AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
            int capability = networkRequest.getApnTypeNetworkCapability();
            if (capability >= 0) {
                transport = mAccessNetworksManager
                        .getPreferredTransportByNetworkCapability(capability);
            }
            return transport;
        } else {
            int apnType = ApnContext.getApnTypeFromNetworkRequest(
                    networkRequest.getNativeNetworkRequest());
            return mAccessNetworksManager.getCurrentTransport(apnType);
        }
    }

    /**
     * Request network
     *
     * @param networkRequest Network request from clients
     * @param requestType The request type
     * @param transport Transport type
     * @param onHandoverCompleteMsg When request type is handover, this message will be sent when
     * handover is completed. For normal request, this should be null.
     */
    private void requestNetworkInternal(TelephonyNetworkRequest networkRequest,
            @RequestNetworkType int requestType, int transport, Message onHandoverCompleteMsg) {
        NetworkRequestsStats.addNetworkRequest(networkRequest.getNativeNetworkRequest(),
                mSubscriptionId);

        if (mPhone.isUsingNewDataStack()) {
            mPhone.getDataNetworkController().addNetworkRequest(networkRequest);
        } else {
            if (mPhone.getDcTracker(transport) != null) {
                mPhone.getDcTracker(transport).requestNetwork(
                        networkRequest.getNativeNetworkRequest(), requestType,
                        onHandoverCompleteMsg);
            }
        }
    }

    private void releaseNetworkInternal(TelephonyNetworkRequest networkRequest) {
        mPhone.getDataNetworkController().removeNetworkRequest(networkRequest);
    }

    // TODO: Clean this up after old data stack removed.
    private void releaseNetworkInternal(TelephonyNetworkRequest networkRequest,
                                        @ReleaseNetworkType int releaseType,
                                        int transport) {
        if (mPhone.isUsingNewDataStack()) {
            mPhone.getDataNetworkController().removeNetworkRequest(networkRequest);
        } else {
            if (mPhone.getDcTracker(transport) != null) {
                mPhone.getDcTracker(transport).releaseNetwork(
                        networkRequest.getNativeNetworkRequest(), releaseType);
            }
        }
    }

    private static int getAction(boolean wasActive, boolean isActive) {
        if (!wasActive && isActive) {
            return ACTION_REQUEST;
        } else if (wasActive && !isActive) {
            return ACTION_RELEASE;
        } else {
            return ACTION_NO_OP;
        }
    }

    // apply or revoke requests if our active-ness changes
    private void onActivePhoneSwitch() {
        logl("onActivePhoneSwitch");
        for (Map.Entry<TelephonyNetworkRequest, Integer> entry : mNetworkRequests.entrySet()) {
            TelephonyNetworkRequest networkRequest = entry.getKey();
            boolean applied = entry.getValue() != AccessNetworkConstants.TRANSPORT_TYPE_INVALID;

            boolean shouldApply = mPhoneSwitcher.shouldApplyNetworkRequest(
                    networkRequest, mPhone.getPhoneId());

            int action = getAction(applied, shouldApply);
            if (action == ACTION_NO_OP) continue;

            logl("onActivePhoneSwitch: " + ((action == ACTION_REQUEST)
                    ? "Requesting" : "Releasing") + " network request " + networkRequest);
            int transportType = getTransportTypeFromNetworkRequest(networkRequest);
            if (action == ACTION_REQUEST) {
                requestNetworkInternal(networkRequest, DcTracker.REQUEST_TYPE_NORMAL,
                        getTransportTypeFromNetworkRequest(networkRequest), null);
            } else if (action == ACTION_RELEASE) {
                if (mPhone.isUsingNewDataStack()) {
                    releaseNetworkInternal(networkRequest);
                } else {
                    releaseNetworkInternal(networkRequest, DcTracker.RELEASE_TYPE_DETACH,
                            getTransportTypeFromNetworkRequest(networkRequest));
                }
            }

            mNetworkRequests.put(networkRequest,
                    shouldApply ? transportType : AccessNetworkConstants.TRANSPORT_TYPE_INVALID);
        }
    }

    // watch for phone->subId changes, reapply new filter and let
    // that flow through to apply/revoke of requests
    private void onSubIdChange() {
        final int newSubscriptionId = mSubscriptionController.getSubIdUsingPhoneId(
                mPhone.getPhoneId());
        if (mSubscriptionId != newSubscriptionId) {
            if (DBG) logl("onSubIdChange " + mSubscriptionId + "->" + newSubscriptionId);
            mSubscriptionId = newSubscriptionId;
            setCapabilityFilter(makeNetworkFilter(mSubscriptionId));
        }
    }

    @Override
    public void needNetworkFor(NetworkRequest networkRequest) {
        Message msg = mInternalHandler.obtainMessage(EVENT_NETWORK_REQUEST);
        msg.obj = networkRequest;
        msg.sendToTarget();
    }

    private void onNeedNetworkFor(Message msg) {
        TelephonyNetworkRequest networkRequest =
                new TelephonyNetworkRequest((NetworkRequest) msg.obj, mPhone);
        boolean shouldApply = mPhoneSwitcher.shouldApplyNetworkRequest(
                networkRequest, mPhone.getPhoneId());

        mNetworkRequests.put(networkRequest, shouldApply
                ? getTransportTypeFromNetworkRequest(networkRequest)
                : AccessNetworkConstants.TRANSPORT_TYPE_INVALID);

        logl("onNeedNetworkFor " + networkRequest + " shouldApply " + shouldApply);

        if (shouldApply) {
            requestNetworkInternal(networkRequest, DcTracker.REQUEST_TYPE_NORMAL,
                    getTransportTypeFromNetworkRequest(networkRequest), null);
        }
    }

    @Override
    public void releaseNetworkFor(NetworkRequest networkRequest) {
        Message msg = mInternalHandler.obtainMessage(EVENT_NETWORK_RELEASE);
        msg.obj = networkRequest;
        msg.sendToTarget();
    }

    private void onReleaseNetworkFor(Message msg) {
        TelephonyNetworkRequest networkRequest =
                new TelephonyNetworkRequest((NetworkRequest) msg.obj, mPhone);
        boolean applied = mNetworkRequests.get(networkRequest)
                != AccessNetworkConstants.TRANSPORT_TYPE_INVALID;

        mNetworkRequests.remove(networkRequest);

        logl("onReleaseNetworkFor " + networkRequest + " applied " + applied);

        if (applied) {
            if (mPhone.isUsingNewDataStack()) {
                releaseNetworkInternal(networkRequest);
            } else {
                // Most of the time, the network request only exists in one of the DcTracker, but in
                // the middle of handover, the network request temporarily exists in both
                // DcTrackers. If connectivity service releases the network request while handover
                // is ongoing, we need to remove network requests from both DcTrackers.
                // Note that this part will be refactored in T, where we won't even have DcTracker
                // at all.
                releaseNetworkInternal(networkRequest, DcTracker.RELEASE_TYPE_NORMAL,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
                releaseNetworkInternal(networkRequest, DcTracker.RELEASE_TYPE_NORMAL,
                        AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
            }
        }
    }

    private void onDataHandoverNeeded(@ApnType int apnType, int targetTransport,
                                      HandoverParams handoverParams) {
        log("onDataHandoverNeeded: apnType=" + ApnSetting.getApnTypeString(apnType)
                + ", target transport="
                + AccessNetworkConstants.transportTypeToString(targetTransport));
        if (mAccessNetworksManager.getCurrentTransport(apnType) == targetTransport) {
            log("APN type " + ApnSetting.getApnTypeString(apnType) + " is already on "
                    + AccessNetworkConstants.transportTypeToString(targetTransport));
            return;
        }

        boolean handoverPending = false;
        for (Map.Entry<TelephonyNetworkRequest, Integer> entry : mNetworkRequests.entrySet()) {
            TelephonyNetworkRequest networkRequest = entry.getKey();
            int currentTransport = entry.getValue();
            boolean applied = currentTransport != AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
            if (ApnContext.getApnTypeFromNetworkRequest(
                    networkRequest.getNativeNetworkRequest()) == apnType
                    && applied
                    && currentTransport != targetTransport) {
                DcTracker dcTracker = mPhone.getDcTracker(currentTransport);
                if (dcTracker != null) {
                    DataConnection dc = dcTracker.getDataConnectionByApnType(
                            ApnSetting.getApnTypeString(apnType));
                    if (dc != null && (dc.isActive())) {
                        Message onCompleteMsg = mInternalHandler.obtainMessage(
                                EVENT_DATA_HANDOVER_COMPLETED);
                        onCompleteMsg.getData().putParcelable(
                                DcTracker.DATA_COMPLETE_MSG_EXTRA_NETWORK_REQUEST,
                                networkRequest.getNativeNetworkRequest());
                        mPendingHandovers.put(onCompleteMsg, handoverParams);
                        requestNetworkInternal(networkRequest, DcTracker.REQUEST_TYPE_HANDOVER,
                                targetTransport, onCompleteMsg);
                        log("Requested handover " + ApnSetting.getApnTypeString(apnType)
                                + " to "
                                + AccessNetworkConstants.transportTypeToString(targetTransport)
                                + ". " + networkRequest);
                        handoverPending = true;
                    } else {
                        // Request is there, but no actual data connection. In this case, just move
                        // the request to the new transport.
                        log("The network request is on transport " + AccessNetworkConstants
                                .transportTypeToString(currentTransport) + ", but no live data "
                                + "connection. Just move the request to transport "
                                + AccessNetworkConstants.transportTypeToString(targetTransport)
                                + ", dc=" + dc);
                        entry.setValue(targetTransport);
                        releaseNetworkInternal(networkRequest, DcTracker.RELEASE_TYPE_NORMAL,
                                currentTransport);
                        requestNetworkInternal(networkRequest, DcTracker.REQUEST_TYPE_NORMAL,
                                targetTransport, null);
                    }
                } else {
                    log("DcTracker on " + AccessNetworkConstants.transportTypeToString(
                            currentTransport) + " is not available.");
                }
            }
        }

        if (!handoverPending) {
            log("No handover request pending. Handover process is now completed");
            handoverParams.callback.onCompleted(true, false);
        }
    }

    private void onDataHandoverSetupCompleted(NetworkRequest request, boolean success,
                                              int targetTransport, boolean fallback,
                                              HandoverParams handoverParams) {
        log("onDataHandoverSetupCompleted: " + request + ", success=" + success
                + ", targetTransport="
                + AccessNetworkConstants.transportTypeToString(targetTransport)
                + ", fallback=" + fallback);

        TelephonyNetworkRequest networkRequest = new TelephonyNetworkRequest(request, mPhone);
        // At this point, handover setup has been completed on the target transport.
        // If it succeeded, or it failed without falling back to the original transport,
        // we should release the request from the original transport.
        if (!fallback) {
            int originTransport = DataUtils.getSourceTransport(targetTransport);
            int releaseType = success
                    ? DcTracker.RELEASE_TYPE_HANDOVER
                    // If handover fails, we need to tear down the existing connection, so the
                    // new data connection can be re-established on the new transport. If we leave
                    // the existing data connection in current transport, then DCT and qualified
                    // network service will be out of sync. Specifying release type to detach
                    // the transport is moved to the other transport, but network request is still
                    // there, connectivity service will not call unwanted to tear down the network.
                    // We need explicitly tear down the data connection here so the new data
                    // connection can be re-established on the other transport.
                    : DcTracker.RELEASE_TYPE_DETACH;
            releaseNetworkInternal(networkRequest, releaseType, originTransport);

            // Before updating the network request with the target transport, make sure the request
            // is still there because it's possible that connectivity service has already released
            // the network while handover is ongoing. If connectivity service already released
            // the network request, we need to tear down the just-handovered data connection on the
            // target transport.
            if (mNetworkRequests.containsKey(networkRequest)) {
                // Update it with the target transport.
                mNetworkRequests.put(networkRequest, targetTransport);
            }
        } else {
            // If handover fails and requires to fallback, the context of target transport needs to
            // be released
            if (!success) {
                releaseNetworkInternal(networkRequest,
                        DcTracker.RELEASE_TYPE_NORMAL, targetTransport);
            }
        }

        handoverParams.callback.onCompleted(success, fallback);
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    protected void logl(String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of telephony network factory
     *
     * @param fd File descriptor
     * @param writer Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println("TelephonyNetworkFactory-" + mPhone.getPhoneId());
        pw.increaseIndent();
        pw.println("Network Requests:");
        pw.increaseIndent();
        for (Map.Entry<TelephonyNetworkRequest, Integer> entry : mNetworkRequests.entrySet()) {
            TelephonyNetworkRequest nr = entry.getKey();
            int transport = entry.getValue();
            pw.println(nr + (transport != AccessNetworkConstants.TRANSPORT_TYPE_INVALID
                    ? (" applied on " + transport) : " not applied"));
        }
        pw.decreaseIndent();
        pw.print("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}
