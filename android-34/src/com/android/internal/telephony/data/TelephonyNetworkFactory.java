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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.AccessNetworkConstants;
import android.telephony.SubscriptionManager;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
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

    private final PhoneSwitcher mPhoneSwitcher;
    private final LocalLog mLocalLog = new LocalLog(REQUEST_LOG_SIZE);

    // Key: network request. Value: the transport of the network request applies to,
    // AccessNetworkConstants.TRANSPORT_TYPE_INVALID if not applied.
    private final Map<TelephonyNetworkRequest, Integer> mNetworkRequests = new HashMap<>();

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

        mAccessNetworksManager = mPhone.getAccessNetworksManager();

        setCapabilityFilter(makeNetworkFilterByPhoneId(mPhone.getPhoneId()));
        setScoreFilter(TELEPHONY_NETWORK_SCORE);

        mPhoneSwitcher = PhoneSwitcher.getInstance();
        LOG_TAG = "TelephonyNetworkFactory[" + mPhone.getPhoneId() + "]";

        mPhoneSwitcher.registerForActivePhoneSwitch(mInternalHandler, EVENT_ACTIVE_PHONE_SWITCH,
                null);

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

    private NetworkCapabilities makeNetworkFilterByPhoneId(int phoneId) {
        return makeNetworkFilter(SubscriptionManager.getSubscriptionId(phoneId));
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
            }
        }
    }

    private int getTransportTypeFromNetworkRequest(TelephonyNetworkRequest networkRequest) {
        int transport = AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
        int capability = networkRequest.getApnTypeNetworkCapability();
        if (capability >= 0) {
            transport = mAccessNetworksManager
                    .getPreferredTransportByNetworkCapability(capability);
        }
        return transport;
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
                NetworkRequestsStats.addNetworkRequest(networkRequest.getNativeNetworkRequest(),
                        mSubscriptionId);
                mPhone.getDataNetworkController().addNetworkRequest(networkRequest);
            } else if (action == ACTION_RELEASE) {
                mPhone.getDataNetworkController().removeNetworkRequest(networkRequest);
            }

            mNetworkRequests.put(networkRequest,
                    shouldApply ? transportType : AccessNetworkConstants.TRANSPORT_TYPE_INVALID);
        }
    }

    // watch for phone->subId changes, reapply new filter and let
    // that flow through to apply/revoke of requests
    private void onSubIdChange() {
        int newSubscriptionId = SubscriptionManager.getSubscriptionId(mPhone.getPhoneId());
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
            NetworkRequestsStats.addNetworkRequest(networkRequest.getNativeNetworkRequest(),
                    mSubscriptionId);
            mPhone.getDataNetworkController().addNetworkRequest(networkRequest);
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
            mPhone.getDataNetworkController().removeNetworkRequest(networkRequest);
        }
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
