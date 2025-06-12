/*
 * Copyright 2021 The Android Open Source Project
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

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.KeepalivePacketData;
import android.net.NetworkAgent;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkProvider;
import android.net.NetworkScore;
import android.net.QosFilter;
import android.net.Uri;
import android.os.Looper;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;

import com.android.internal.telephony.Phone;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * TelephonyNetworkAgent class represents a single PDN (Packet Data Network). It is an agent
 * for telephony to propagate network related information to the connectivity service. It always
 * has an associated parent {@link DataNetwork}.
 */
public class TelephonyNetworkAgent extends NetworkAgent {
    private final String mLogTag;
    private final LocalLog mLocalLog = new LocalLog(128);

    /** Max unregister network agent delay. */
    private static final int NETWORK_AGENT_TEARDOWN_DELAY_MS = 5_000;

    /** The parent data network. */
    @NonNull
    private final DataNetwork mDataNetwork;

    /** This is the id from {@link NetworkAgent#register()}. */
    private final int mId;

    /**
     * Indicates if this network agent is abandoned. if {@code true}, it ignores the
     * @link NetworkAgent#onNetworkUnwanted()} calls from connectivity service.
     */
    private boolean mAbandoned = false;

    /**
     * The callbacks that are used to pass information to {@link DataNetwork} and
     * {@link QosCallbackTracker}.
     */
    @NonNull
    private final Set<TelephonyNetworkAgentCallback> mTelephonyNetworkAgentCallbacks =
            new ArraySet<>();

    /**
     * Telephony network agent callback. This should be only used by {@link DataNetwork}.
     */
    public abstract static class TelephonyNetworkAgentCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public TelephonyNetworkAgentCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when the system determines the usefulness of this network.
         *
         * @param status one of {@link NetworkAgent#VALIDATION_STATUS_VALID} or
         * {@link NetworkAgent#VALIDATION_STATUS_NOT_VALID}.
         * @param redirectUri If internet connectivity is being redirected (e.g., on a captive
         * portal),
         * this is the destination the probes are being redirected to, otherwise {@code null}.
         *
         * @see NetworkAgent#onValidationStatus(int, Uri)
         */
        public void onValidationStatus(@android.telephony.Annotation.ValidationStatus int status,
                @Nullable Uri redirectUri) {}

        /**
         * Called when a qos callback is registered with a filter.
         *
         * @param qosCallbackId the id for the callback registered
         * @param filter the filter being registered
         */
        public void onQosCallbackRegistered(int qosCallbackId, @NonNull QosFilter filter) {}

        /**
         * Called when a qos callback is registered with a filter.
         * <p>
         * Any QoS events that are sent with the same callback id after this method is called are a
         * no-op.
         *
         * @param qosCallbackId the id for the callback being unregistered.
         */
        public void onQosCallbackUnregistered(int qosCallbackId) {}

        /**
         * Requests that the network hardware send the specified packet at the specified interval.
         *
         * @param slot the hardware slot on which to start the keepalive.
         * @param interval the interval between packets, between 10 and 3600. Note that this API
         *                 does not support sub-second precision and will round off the request.
         * @param packet the packet to send.
         */
        public void onStartSocketKeepalive(int slot, @NonNull Duration interval,
                @NonNull KeepalivePacketData packet) {}

        /**
         * Requests that the network hardware stop a previously-started keepalive.
         *
         * @param slot the hardware slot on which to stop the keepalive.
         */
        public void onStopSocketKeepalive(int slot) {}
    }

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param dataNetwork The data network which owns this network agent.
     * @param score The initial score of the network.
     * @param config The network agent config.
     * @param provider The network provider.
     */
    public TelephonyNetworkAgent(@NonNull Phone phone, @NonNull Looper looper,
            @NonNull DataNetwork dataNetwork, @NonNull NetworkScore score,
            @NonNull NetworkAgentConfig config, @NonNull NetworkProvider provider,
            @NonNull TelephonyNetworkAgentCallback callback) {
        super(phone.getContext(), looper, "TelephonyNetworkAgent",
                // Connectivity service does not allow an agent created in suspended state.
                // Always create the network agent with NOT_SUSPENDED and immediately update the
                // suspended state afterwards.
                new NetworkCapabilities.Builder(dataNetwork.getNetworkCapabilities())
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                        .build(),
                dataNetwork.getLinkProperties(), score,
                config, provider);
        register();
        mDataNetwork = dataNetwork;
        mTelephonyNetworkAgentCallbacks.add(callback);
        mId = getNetwork().getNetId();
        mLogTag = "TNA-" + mId;

        log("TelephonyNetworkAgent created, nc="
                + new NetworkCapabilities.Builder(dataNetwork.getNetworkCapabilities())
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                .build() + ", score=" + score);
    }

    /**
     * Called when connectivity service has indicated they no longer want this network.
     */
    @Override
    public void onNetworkUnwanted() {
        if (mAbandoned) {
            log("The agent is already abandoned. Ignored onNetworkUnwanted.");
            return;
        }

        mDataNetwork.tearDown(DataNetwork.TEAR_DOWN_REASON_CONNECTIVITY_SERVICE_UNWANTED);
    }

    /**
     * @return The unique id of the agent.
     */
    public int getId() {
        return mId;
    }

    /**
     * Called when the system determines the usefulness of this network.
     *
     * @param status one of {@link NetworkAgent#VALIDATION_STATUS_VALID} or
     * {@link NetworkAgent#VALIDATION_STATUS_NOT_VALID}.
     * @param redirectUri If internet connectivity is being redirected (e.g., on a captive portal),
     * this is the destination the probes are being redirected to, otherwise {@code null}.
     *
     * @see NetworkAgent#onValidationStatus(int, Uri)
     */
    @Override
    public void onValidationStatus(@android.telephony.Annotation.ValidationStatus int status,
            @Nullable Uri redirectUri) {
        if (mAbandoned) {
            log("The agent is already abandoned. Ignored onValidationStatus.");
            return;
        }
        mTelephonyNetworkAgentCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onValidationStatus(status, redirectUri)));
    }

    /**
     * Called when connectivity service request a bandwidth update.
     */
    @Override
    public void onBandwidthUpdateRequested() {
        // Drop the support for IRadio 1.0 and 1.1. On newer HAL, LCE should be reported from
        // modem unsolicited.
        loge("onBandwidthUpdateRequested: RIL.pullLceData is not supported anymore.");
    }

    /**
     * Called when connectivity service requests that the network hardware send the specified
     * packet at the specified interval.
     *
     * @param slot the hardware slot on which to start the keepalive.
     * @param interval the interval between packets, between 10 and 3600. Note that this API
     *                 does not support sub-second precision and will round off the request.
     * @param packet the packet to send.
     */
    @Override
    public void onStartSocketKeepalive(int slot, @NonNull Duration interval,
            @NonNull KeepalivePacketData packet) {
        if (mAbandoned) {
            log("The agent is already abandoned. Ignored onStartSocketKeepalive.");
            return;
        }
        mTelephonyNetworkAgentCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onStartSocketKeepalive(slot, interval, packet)));
    }

    /**
     * Called when connectivity service requests that the network hardware stop a previously-started
     * keepalive.
     *
     * @param slot the hardware slot on which to stop the keepalive.
     */
    @Override
    public void onStopSocketKeepalive(int slot) {
        if (mAbandoned) {
            log("The agent is already abandoned. Ignored onStopSocketKeepalive.");
            return;
        }
        mTelephonyNetworkAgentCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onStopSocketKeepalive(slot)));
    }

    /**
     * Called when a qos callback is registered with a filter.
     *
     * @param qosCallbackId the id for the callback registered
     * @param filter the filter being registered
     */
    @Override
    public void onQosCallbackRegistered(final int qosCallbackId, @NonNull final QosFilter filter) {
        if (mAbandoned) {
            log("The agent is already abandoned. Ignored onQosCallbackRegistered.");
            return;
        }
        mTelephonyNetworkAgentCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onQosCallbackRegistered(qosCallbackId, filter)));
    }

    /**
     * Called when a qos callback is registered with a filter.
     * <p>
     * Any QoS events that are sent with the same callback id after this method is called are a
     * no-op.
     *
     * @param qosCallbackId the id for the callback being unregistered.
     */
    @Override
    public void onQosCallbackUnregistered(final int qosCallbackId) {
        if (mAbandoned) {
            log("The agent is already abandoned. Ignored onQosCallbackUnregistered.");
            return;
        }
        mTelephonyNetworkAgentCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onQosCallbackUnregistered(qosCallbackId)));
    }

    /**
     * Abandon the network agent. This is used for telephony to re-create the network agent when
     * immutable capabilities got changed, where telephony calls
     * {@link NetworkAgent#unregisterAfterReplacement} and then create another network agent with
     * new capabilities. Abandon this network agent allowing it ignore the subsequent
     * {@link #onNetworkUnwanted()} invocation caused by
     * {@link NetworkAgent#unregisterAfterReplacement}.
     */
    public void abandon() {
        mAbandoned = true;
        unregisterAfterReplacement(NETWORK_AGENT_TEARDOWN_DELAY_MS);
    }

    /**
     * Register the callback for receiving information from {@link TelephonyNetworkAgent}.
     *
     * @param callback The callback.
     */
    public void registerCallback(@NonNull TelephonyNetworkAgentCallback callback) {
        mTelephonyNetworkAgentCallbacks.add(callback);
    }

    /**
     * Unregister the previously registered {@link TelephonyNetworkAgentCallback}.
     *
     * @param callback The callback to unregister.
     */
    public void unregisterCallback(@NonNull TelephonyNetworkAgentCallback callback) {
        mTelephonyNetworkAgentCallbacks.remove(callback);
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    protected void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }

    /**
     * Dump the state of TelephonyNetworkAgent
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(mLogTag + ":");
        pw.increaseIndent();
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}
