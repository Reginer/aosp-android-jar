/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.telephony.domainselection;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.AsyncResult;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.AccessNetworkConstants.RadioAccessNetworkType;
import android.telephony.Annotation.DisconnectCauses;
import android.telephony.DomainSelectionService;
import android.telephony.DomainSelectionService.EmergencyScanType;
import android.telephony.DomainSelector;
import android.telephony.EmergencyRegResult;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.TransportSelectorCallback;
import android.telephony.WwanSelectorCallback;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.util.TelephonyUtils;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


/**
 * Manages the information of request and the callback binder.
 */
public class DomainSelectionConnection {

    private static final boolean DBG = TelephonyUtils.IS_DEBUGGABLE;

    protected static final int EVENT_EMERGENCY_NETWORK_SCAN_RESULT = 1;
    protected static final int EVENT_QUALIFIED_NETWORKS_CHANGED = 2;

    /** Callback to receive responses from DomainSelectionConnection. */
    public interface DomainSelectionConnectionCallback {
        /**
         * Notifies that selection has terminated because there is no decision that can be made
         * or a timeout has occurred. The call should be terminated when this method is called.
         *
         * @param cause Indicates the reason.
         */
        void onSelectionTerminated(@DisconnectCauses int cause);
    }

    /** An internal class implementing {@link TransportSelectorCallback} interface. */
    private final class TransportSelectorCallbackWrapper implements TransportSelectorCallback {
        @Override
        public void onCreated(@NonNull DomainSelector selector) {
            mDomainSelector = selector;
            DomainSelectionConnection.this.onCreated();
        }

        @Override
        public void onWlanSelected(boolean useEmergencyPdn) {
            DomainSelectionConnection.this.onWlanSelected(useEmergencyPdn);
        }

        @Override
        public @NonNull WwanSelectorCallback onWwanSelected() {
            if (mWwanSelectorCallback == null) {
                mWwanSelectorCallback = new WwanSelectorCallbackWrapper();
            }
            DomainSelectionConnection.this.onWwanSelected();
            return mWwanSelectorCallback;
        }

        @Override
        public void onWwanSelected(final Consumer<WwanSelectorCallback> consumer) {
            if (mWwanSelectorCallback == null) {
                mWwanSelectorCallback = new WwanSelectorCallbackWrapper();
            }
            if (mWwanSelectedExecutor == null) {
                mWwanSelectedExecutor = Executors.newSingleThreadExecutor();
            }
            mWwanSelectedExecutor.execute(() -> {
                DomainSelectionConnection.this.onWwanSelected();
                consumer.accept(mWwanSelectorCallback);
            });
        }

        @Override
        public void onSelectionTerminated(int cause) {
            DomainSelectionConnection.this.onSelectionTerminated(cause);
            dispose();
        }
    }

    /** An internal class implementing {@link WwanSelectorCallback} interface. */
    private final class WwanSelectorCallbackWrapper
            implements WwanSelectorCallback, CancellationSignal.OnCancelListener {
        @Override
        public void onRequestEmergencyNetworkScan(@NonNull List<Integer> preferredNetworks,
                @EmergencyScanType int scanType, @NonNull CancellationSignal signal,
                @NonNull Consumer<EmergencyRegResult> consumer) {
            if (signal != null) signal.setOnCancelListener(this);
            mResultCallback = consumer;
            initHandler();
            DomainSelectionConnection.this.onRequestEmergencyNetworkScan(
                    preferredNetworks.stream().mapToInt(Integer::intValue).toArray(), scanType);
        }

        @Override
        public void onDomainSelected(@NetworkRegistrationInfo.Domain int domain,
                boolean useEmergencyPdn) {
            DomainSelectionConnection.this.onDomainSelected(domain, useEmergencyPdn);
        }

        @Override
        public void onCancel() {
            DomainSelectionConnection.this.onCancel();
        }
    }

    protected final class DomainSelectionConnectionHandler extends Handler {
        DomainSelectionConnectionHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_EMERGENCY_NETWORK_SCAN_RESULT:
                    mIsWaitingForScanResult = false;
                    if (mResultCallback == null) break;
                    ar = (AsyncResult) msg.obj;
                    EmergencyRegResult regResult = (EmergencyRegResult) ar.result;
                    if (DBG) logd("EVENT_EMERGENCY_NETWORK_SCAN_RESULT result=" + regResult);
                    CompletableFuture.runAsync(
                            () -> mResultCallback.accept(regResult),
                            mController.getDomainSelectionServiceExecutor()).join();
                    break;
                case EVENT_QUALIFIED_NETWORKS_CHANGED:
                    onQualifiedNetworksChanged();
                    break;
                default:
                    loge("handleMessage unexpected msg=" + msg.what);
                    break;
            }
        }
    }

    protected String mTag = "DomainSelectionConnection";

    private final LocalLog mLocalLog = new LocalLog(30);
    private final @NonNull TransportSelectorCallback mTransportSelectorCallback;

    /**
     * Controls the communication between {@link DomainSelectionConnection} and
     * {@link DomainSelectionService}.
     */
    private final @NonNull DomainSelectionController mController;
    /** Indicates whether the requested service is for emergency services. */
    private final boolean mIsEmergency;

    /** Interface to receive the request to trigger emergency network scan and selected domain. */
    private @Nullable WwanSelectorCallback mWwanSelectorCallback;
    /** Interface to return the result of emergency network scan. */
    private @Nullable Consumer<EmergencyRegResult> mResultCallback;
    /** Interface to the {@link DomainSelector} created for this service. */
    private @Nullable DomainSelector mDomainSelector;

    /** The slot requested this connection. */
    protected @NonNull Phone mPhone;
    /** The requested domain selector type. */
    private @DomainSelectionService.SelectorType int mSelectorType;

    /** The attributes required to determine the domain. */
    private @Nullable DomainSelectionService.SelectionAttributes mSelectionAttributes;

    private @Nullable Looper mLooper;
    protected @Nullable DomainSelectionConnectionHandler mHandler;
    private boolean mRegisteredRegistrant;
    private boolean mIsWaitingForScanResult;

    private @NonNull AndroidFuture<Integer> mOnComplete;

    private @Nullable Executor mWwanSelectedExecutor;

    /**
     * Creates an instance.
     *
     * @param phone For which this service is requested.
     * @param selectorType Indicates the type of the requested service.
     * @param isEmergency Indicates whether this request is for emergency service.
     * @param controller The controller to communicate with the domain selection service.
     */
    public DomainSelectionConnection(@NonNull Phone phone,
            @DomainSelectionService.SelectorType int selectorType, boolean isEmergency,
            @NonNull DomainSelectionController controller) {
        mController = controller;
        mPhone = phone;
        mSelectorType = selectorType;
        mIsEmergency = isEmergency;

        mTransportSelectorCallback = new TransportSelectorCallbackWrapper();
        mOnComplete = new AndroidFuture<>();
    }

    /**
     * Returns the attributes required to determine the domain for a telephony service.
     *
     * @return The attributes required to determine the domain.
     */
    public @Nullable DomainSelectionService.SelectionAttributes getSelectionAttributes() {
        return mSelectionAttributes;
    }

    /**
     * Returns the interface for the callbacks.
     *
     * @return The {@link TransportSelectorCallback} interface.
     */
    @VisibleForTesting
    public @NonNull TransportSelectorCallback getTransportSelectorCallback() {
        return mTransportSelectorCallback;
    }

    /**
     * Returns the {@link CompletableFuture} to receive the selected domain.
     *
     * @return The callback to receive response.
     */
    public @NonNull CompletableFuture<Integer> getCompletableFuture() {
        return mOnComplete;
    }

    /**
     * Returs the {@link Phone} which requested this connection.
     *
     * @return The {@link Phone} instance.
     */
    public @NonNull Phone getPhone() {
        return mPhone;
    }

    /**
     * Requests the domain selection servic to select a domain.
     *
     * @param attr The attributes required to determine the domain.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PROTECTED)
    public void selectDomain(@NonNull DomainSelectionService.SelectionAttributes attr) {
        mSelectionAttributes = attr;
        mController.selectDomain(attr, getTransportSelectorCallback());
    }

    /**
     * Notifies that {@link DomainSelector} instance has been created for the selection request.
     */
    public void onCreated() {
        // Can be overridden if required
    }

    /**
     * Notifies that WLAN transport has been selected.
     */
    public void onWlanSelected() {
        // Can be overridden.
    }

    /**
     * Notifies that WLAN transport has been selected.
     *
     * @param useEmergencyPdn Indicates whether Wi-Fi emergency services use emergency PDN or not.
     */
    public void onWlanSelected(boolean useEmergencyPdn) {
        // Can be overridden.
        onWlanSelected();
    }

    /**
     * Notifies that WWAN transport has been selected.
     */
    public void onWwanSelected() {
        // Can be overridden.
    }

    /**
     * Notifies that selection has terminated because there is no decision that can be made
     * or a timeout has occurred. The call should be terminated when this method is called.
     *
     * @param cause Indicates the reason.
     */
    public void onSelectionTerminated(@DisconnectCauses int cause) {
        // Can be overridden.
    }

    /**
     * Requests the emergency network scan.
     *
     * @param preferredNetworks The ordered list of preferred networks to scan.
     * @param scanType Indicates the scan preference, such as full service or limited service.
     */
    public void onRequestEmergencyNetworkScan(
            @NonNull @RadioAccessNetworkType int[] preferredNetworks,
            @EmergencyScanType int scanType) {
        // Can be overridden if required
        if (!mRegisteredRegistrant) {
            mPhone.registerForEmergencyNetworkScan(mHandler,
                    EVENT_EMERGENCY_NETWORK_SCAN_RESULT, null);
            mRegisteredRegistrant = true;
        }
        mIsWaitingForScanResult = true;
        mPhone.triggerEmergencyNetworkScan(preferredNetworks, scanType, null);
    }

    /**
     * Notifies the domain selected.
     *
     * @param domain The selected domain.
     */
    public void onDomainSelected(@NetworkRegistrationInfo.Domain int domain) {
        // Can be overridden if required
        CompletableFuture<Integer> future = getCompletableFuture();
        future.complete(domain);
    }

    /**
     * Notifies the domain selected.
     *
     * @param domain The selected domain.
     * @param useEmergencyPdn Indicates whether emergency services use emergency PDN or not.
     */
    public void onDomainSelected(@NetworkRegistrationInfo.Domain int domain,
            boolean useEmergencyPdn) {
        // Can be overridden if required
        onDomainSelected(domain);
    }

    /**
     * Notifies that the emergency network scan is canceled.
     */
    public void onCancel() {
        // Can be overridden if required
        onCancel(false);
    }

    private void onCancel(boolean resetScan) {
        if (mIsWaitingForScanResult) {
            mIsWaitingForScanResult = false;
            mPhone.cancelEmergencyNetworkScan(resetScan, null);
        }
    }

    /**
     * Cancels an ongoing selection operation. It is up to the {@link DomainSelectionService}
     * to clean up all ongoing operations with the framework.
     */
    public void cancelSelection() {
        if (mDomainSelector == null) return;
        mDomainSelector.cancelSelection();
        dispose();
    }

    /**
     * Requests the domain selection service to reselect a domain.
     *
     * @param attr The attributes required to determine the domain.
     * @return The callback to receive the response.
     */
    public @NonNull CompletableFuture<Integer> reselectDomain(
            @NonNull DomainSelectionService.SelectionAttributes attr) {
        mSelectionAttributes = attr;
        if (mDomainSelector == null) return null;
        mOnComplete = new AndroidFuture<>();
        mDomainSelector.reselectDomain(attr);
        return mOnComplete;
    }

    /**
     * Finishes the selection procedure and cleans everything up.
     */
    public void finishSelection() {
        if (mDomainSelector == null) return;
        mDomainSelector.finishSelection();
        dispose();
    }

    /** Indicates that the service connection has been removed. */
    public void onServiceDisconnected() {
        // Can be overridden.
        dispose();
    }

    private void dispose() {
        if (mRegisteredRegistrant) {
            mPhone.unregisterForEmergencyNetworkScan(mHandler);
            mRegisteredRegistrant = false;
        }
        onCancel(true);
        mController.removeConnection(this);
        if (mLooper != null) mLooper.quitSafely();
        mLooper = null;
        mHandler = null;
    }

    protected void initHandler() {
        if (mLooper == null) {
            HandlerThread handlerThread = new HandlerThread(mTag);
            handlerThread.start();
            mLooper = handlerThread.getLooper();
        }
        if (mHandler == null) mHandler = new DomainSelectionConnectionHandler(mLooper);
    }

    /**
     * Notifies the change of qualified networks.
     */
    protected void onQualifiedNetworksChanged() {
        if (mIsEmergency
                && (mSelectorType == DomainSelectionService.SELECTOR_TYPE_CALLING)) {
            // DomainSelectionConnection for emergency calls shall override this.
            throw new IllegalStateException("DomainSelectionConnection for emergency calls"
                    + " should override onQualifiedNetworksChanged()");
        }
    }

    /**
     * Dumps local log.
     */
    public void dump(@NonNull PrintWriter printWriter) {
        mLocalLog.dump(printWriter);
    }

    protected void logd(String msg) {
        Log.d(mTag, msg);
    }

    protected void logi(String msg) {
        Log.i(mTag, msg);
        mLocalLog.log(msg);
    }

    protected void loge(String msg) {
        Log.e(mTag, msg);
        mLocalLog.log(msg);
    }
}
