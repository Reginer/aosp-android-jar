/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.ims.rcs.uce;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.content.Context;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsContactUceCapability.CapabilityMechanism;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.RcsUceAdapter.PublishState;
import android.telephony.ims.RcsUceAdapter.StackPublishTriggerType;
import android.telephony.ims.aidl.IOptionsRequestCallback;
import android.telephony.ims.aidl.IRcsUceControllerCallback;
import android.telephony.ims.aidl.IRcsUcePublishStateCallback;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Log;

import com.android.ims.RcsFeatureManager;
import com.android.ims.rcs.uce.UceDeviceState.DeviceStateResult;
import com.android.ims.rcs.uce.eab.EabCapabilityResult;
import com.android.ims.rcs.uce.eab.EabController;
import com.android.ims.rcs.uce.eab.EabControllerImpl;
import com.android.ims.rcs.uce.options.OptionsController;
import com.android.ims.rcs.uce.options.OptionsControllerImpl;
import com.android.ims.rcs.uce.presence.publish.PublishController;
import com.android.ims.rcs.uce.presence.publish.PublishControllerImpl;
import com.android.ims.rcs.uce.presence.subscribe.SubscribeController;
import com.android.ims.rcs.uce.presence.subscribe.SubscribeControllerImpl;
import com.android.ims.rcs.uce.request.UceRequestManager;
import com.android.ims.rcs.uce.util.UceUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The UceController will manage the RCS UCE requests on a per subscription basis. When it receives
 * the UCE requests from the RCS applications and from the ImsService, it will coordinate the
 * cooperation between the publish/subscribe/options components to complete the requests.
 */
public class UceController {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "UceController";

    /**
     * The callback interface is called by the internal controllers to receive information from
     * others controllers.
     */
    public interface UceControllerCallback {
        /**
         * Retrieve the capabilities associated with the given uris from the cache.
         */
        List<EabCapabilityResult> getCapabilitiesFromCache(@NonNull List<Uri> uris);

        /**
         * Retrieve the capabilities associated with the given uris from the cache including
         * expired capabilities.
         */
        List<EabCapabilityResult> getCapabilitiesFromCacheIncludingExpired(@NonNull List<Uri> uris);

        /**
         * Retrieve the contact's capabilities from the availability cache.
         */
        EabCapabilityResult getAvailabilityFromCache(@NonNull Uri uri);

        /**
         * Retrieve the contact's capabilities from the availability cache including expired
         * capabilities
         */
        EabCapabilityResult getAvailabilityFromCacheIncludingExpired(@NonNull Uri uri);

        /**
         * Store the given capabilities to the cache.
         */
        void saveCapabilities(List<RcsContactUceCapability> contactCapabilities);

        /**
         * Retrieve the device's capabilities.
         */
        RcsContactUceCapability getDeviceCapabilities(@CapabilityMechanism int mechanism);

        /**
         * Refresh the device state. It is called when receive the UCE request response.
         * @param sipCode The SIP code of the request response.
         * @param reason The reason from the network response.
         * @param type The type of the request
         */
        void refreshDeviceState(int sipCode, String reason, @RequestType int type);

        /**
         * Reset the device state when then device disallowed state is expired.
         */
        void resetDeviceState();

        /**
         * Get the current device state to check if the device is allowed to send UCE requests.
         */
        DeviceStateResult getDeviceState();

        /**
         * Setup timer to exit device disallowed state.
         */
        void setupResetDeviceStateTimer(long resetAfterSec);

        /**
         * The device state is already reset, clear the timer.
         */
        void clearResetDeviceStateTimer();

        /**
         * The method is called when the given contacts' capabilities are expired and need to be
         * refreshed.
         */
        void refreshCapabilities(@NonNull List<Uri> contactNumbers,
                @NonNull IRcsUceControllerCallback callback) throws RemoteException;
    }

    /**
     * Used to inject RequestManger instances for testing.
     */
    @VisibleForTesting
    public interface RequestManagerFactory {
        UceRequestManager createRequestManager(Context context, int subId, Looper looper,
                UceControllerCallback callback);
    }

    private RequestManagerFactory mRequestManagerFactory = (context, subId, looper, callback) ->
            new UceRequestManager(context, subId, looper, callback);

    /**
     * Used to inject Controller instances for testing.
     */
    @VisibleForTesting
    public interface ControllerFactory {
        /**
         * @return an {@link EabController} associated with the subscription id specified.
         */
        EabController createEabController(Context context, int subId, UceControllerCallback c,
                Looper looper);

        /**
         * @return an {@link PublishController} associated with the subscription id specified.
         */
        PublishController createPublishController(Context context, int subId,
                UceControllerCallback c, Looper looper);

        /**
         * @return an {@link SubscribeController} associated with the subscription id specified.
         */
        SubscribeController createSubscribeController(Context context, int subId);

        /**
         * @return an {@link OptionsController} associated with the subscription id specified.
         */
        OptionsController createOptionsController(Context context, int subId);
    }

    private ControllerFactory mControllerFactory = new ControllerFactory() {
        @Override
        public EabController createEabController(Context context, int subId,
                UceControllerCallback c, Looper looper) {
            return new EabControllerImpl(context, subId, c, looper);
        }

        @Override
        public PublishController createPublishController(Context context, int subId,
                UceControllerCallback c, Looper looper) {
            return new PublishControllerImpl(context, subId, c, looper);
        }

        @Override
        public SubscribeController createSubscribeController(Context context, int subId) {
            return new SubscribeControllerImpl(context, subId);
        }

        @Override
        public OptionsController createOptionsController(Context context, int subId) {
            return new OptionsControllerImpl(context, subId);
        }
    };

    /**
     * Cache the capabilities events triggered by the ImsService during the RCS connected procedure.
     */
    private static class CachedCapabilityEvent {
        private Optional<Integer> mRequestPublishCapabilitiesEvent;
        private Optional<Boolean> mUnpublishEvent;
        private Optional<SomeArgs> mPublishUpdatedEvent;
        private Optional<SomeArgs> mRemoteCapabilityRequestEvent;

        public CachedCapabilityEvent() {
            mRequestPublishCapabilitiesEvent = Optional.empty();
            mUnpublishEvent = Optional.empty();
            mPublishUpdatedEvent = Optional.empty();
            mRemoteCapabilityRequestEvent = Optional.empty();
        }

        /**
         * Cache the publish capabilities request event triggered by the ImsService.
         */
        public synchronized void setRequestPublishCapabilitiesEvent(int triggerType) {
            mRequestPublishCapabilitiesEvent = Optional.of(triggerType);
        }

        /**
         * Cache the unpublish event triggered by the ImsService.
         */
        public synchronized void setOnUnpublishEvent() {
            mUnpublishEvent = Optional.of(Boolean.TRUE);
        }

        /**
         * Cache the publish update event triggered by the ImsService.
         */
        public synchronized void setOnPublishUpdatedEvent(int reasonCode, String reasonPhrase,
                int reasonHeaderCause, String reasonHeaderText) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = reasonCode;
            args.arg2 = reasonPhrase;
            args.arg3 = reasonHeaderCause;
            args.arg4 = reasonHeaderText;
            mPublishUpdatedEvent = Optional.of(args);
        }

        /**
         * Cache the remote capability request event triggered by the ImsService.
         */
        public synchronized void setRemoteCapabilityRequestEvent(Uri contactUri,
                List<String> remoteCapabilities, IOptionsRequestCallback callback) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = contactUri;
            args.arg2 = remoteCapabilities;
            args.arg3 = callback;
            mRemoteCapabilityRequestEvent = Optional.of(args);
        }

        /** @Return the cached publish request event */
        public synchronized Optional<Integer> getRequestPublishEvent() {
            return mRequestPublishCapabilitiesEvent;
        }

        /** @Return the cached unpublish event */
        public synchronized Optional<Boolean> getUnpublishEvent() {
            return mUnpublishEvent;
        }

        /** @Return the cached pubilsh update event */
        public synchronized Optional<SomeArgs> getPublishUpdatedEvent() {
            return mPublishUpdatedEvent;
        }

        /** @Return the cached remote capability request event */
        public synchronized Optional<SomeArgs> getRemoteCapabilityRequestEvent() {
            return mRemoteCapabilityRequestEvent;
        }

        /** Clear the cached */
        public synchronized void clear() {
            mRequestPublishCapabilitiesEvent = Optional.empty();
            mUnpublishEvent = Optional.empty();
            mPublishUpdatedEvent.ifPresent(args -> args.recycle());
            mPublishUpdatedEvent = Optional.empty();
            mRemoteCapabilityRequestEvent.ifPresent(args -> args.recycle());
            mRemoteCapabilityRequestEvent = Optional.empty();
        }
    }

    /**
     * The request type is PUBLISH.
     */
    public static final int REQUEST_TYPE_PUBLISH = 1;

    /**
     * The request type is CAPABILITY.
     */
    public static final int REQUEST_TYPE_CAPABILITY = 2;

    @IntDef(value = {
            REQUEST_TYPE_PUBLISH,
            REQUEST_TYPE_CAPABILITY,
    }, prefix="REQUEST_TYPE_")
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestType {}

    public static final Map<Integer, String> REQUEST_TYPE_DESCRIPTION = new HashMap<>();
    static {
        REQUEST_TYPE_DESCRIPTION.put(REQUEST_TYPE_PUBLISH, "REQUEST_TYPE_PUBLISH");
        REQUEST_TYPE_DESCRIPTION.put(REQUEST_TYPE_CAPABILITY, "REQUEST_TYPE_CAPABILITY");
    }

    /** The RCS state is disconnected */
    private static final int RCS_STATE_DISCONNECTED = 0;

    /** The RCS state is connecting */
    private static final int RCS_STATE_CONNECTING = 1;

    /** The RCS state is connected */
    private static final int RCS_STATE_CONNECTED = 2;

    @IntDef(value = {
        RCS_STATE_DISCONNECTED,
        RCS_STATE_CONNECTING,
        RCS_STATE_CONNECTED,
    }, prefix="RCS_STATE_")
    @Retention(RetentionPolicy.SOURCE)
    @interface RcsConnectedState {}

    private final int mSubId;
    private final Context mContext;
    private final LocalLog mLocalLog = new LocalLog(UceUtils.LOG_SIZE);

    private volatile Looper mLooper;
    private volatile boolean mIsDestroyedFlag;
    private volatile @RcsConnectedState int mRcsConnectedState;

    private RcsFeatureManager mRcsFeatureManager;
    private EabController mEabController;
    private PublishController mPublishController;
    private SubscribeController mSubscribeController;
    private OptionsController mOptionsController;
    private UceRequestManager mRequestManager;
    // The device state to execute UCE requests.
    private UceDeviceState mDeviceState;
    // The cache of the capability request event triggered by ImsService
    private final CachedCapabilityEvent mCachedCapabilityEvent;

    public UceController(Context context, int subId) {
        mSubId = subId;
        mContext = context;
        mCachedCapabilityEvent = new CachedCapabilityEvent();
        mRcsConnectedState = RCS_STATE_DISCONNECTED;
        logi("create");

        initLooper();
        initControllers();
        initRequestManager();
        initUceDeviceState();
    }

    @VisibleForTesting
    public UceController(Context context, int subId, UceDeviceState deviceState,
            ControllerFactory controllerFactory, RequestManagerFactory requestManagerFactory) {
        mSubId = subId;
        mContext = context;
        mDeviceState = deviceState;
        mControllerFactory = controllerFactory;
        mRequestManagerFactory = requestManagerFactory;
        mCachedCapabilityEvent = new CachedCapabilityEvent();
        mRcsConnectedState = RCS_STATE_DISCONNECTED;
        initLooper();
        initControllers();
        initRequestManager();
    }

    private void initLooper() {
        // Init the looper, it will be passed to each controller.
        HandlerThread handlerThread = new HandlerThread("UceControllerHandlerThread");
        handlerThread.start();
        mLooper = handlerThread.getLooper();
    }

    private void initControllers() {
        mEabController = mControllerFactory.createEabController(mContext, mSubId, mCtrlCallback,
                mLooper);
        mPublishController = mControllerFactory.createPublishController(mContext, mSubId,
                mCtrlCallback, mLooper);
        mSubscribeController = mControllerFactory.createSubscribeController(mContext, mSubId);
        mOptionsController = mControllerFactory.createOptionsController(mContext, mSubId);
    }

    private void initRequestManager() {
        mRequestManager = mRequestManagerFactory.createRequestManager(mContext, mSubId, mLooper,
                mCtrlCallback);
        mRequestManager.setSubscribeController(mSubscribeController);
        mRequestManager.setOptionsController(mOptionsController);
    }

    private void initUceDeviceState() {
        mDeviceState = new UceDeviceState(mSubId, mContext, mCtrlCallback);
        mDeviceState.checkSendResetDeviceStateTimer();
    }

    /**
     * The RcsFeature has been connected to the framework. This method runs on main thread.
     */
    public void onRcsConnected(RcsFeatureManager manager) {
        logi("onRcsConnected");
        // Set the RCS is connecting flag
        mRcsConnectedState = RCS_STATE_CONNECTING;

        // Listen to the capability exchange event which is triggered by the ImsService
        mRcsFeatureManager = manager;
        mRcsFeatureManager.addCapabilityEventCallback(mCapabilityEventListener);

        // Notify each controllers that RCS is connected.
        mEabController.onRcsConnected(manager);
        mPublishController.onRcsConnected(manager);
        mSubscribeController.onRcsConnected(manager);
        mOptionsController.onRcsConnected(manager);

        // Set the RCS is connected flag and check if there is any capability event received during
        // the connecting process.
        mRcsConnectedState = RCS_STATE_CONNECTED;
        handleCachedCapabilityEvent();
    }

    /**
     * The framework has lost the binding to the RcsFeature. This method runs on main thread.
     */
    public void onRcsDisconnected() {
        logi("onRcsDisconnected");
        mRcsConnectedState = RCS_STATE_DISCONNECTED;
        // Remove the listener because RCS is disconnected.
        if (mRcsFeatureManager != null) {
            mRcsFeatureManager.removeCapabilityEventCallback(mCapabilityEventListener);
            mRcsFeatureManager = null;
        }
        // Notify each controllers that RCS is disconnected.
        mEabController.onRcsDisconnected();
        mPublishController.onRcsDisconnected();
        mSubscribeController.onRcsDisconnected();
        mOptionsController.onRcsDisconnected();
    }

    /**
     * Notify to destroy this instance. This instance is unusable after destroyed.
     */
    public void onDestroy() {
        logi("onDestroy");
        mIsDestroyedFlag = true;
        // Remove the listener because the UceController instance is destroyed.
        if (mRcsFeatureManager != null) {
            mRcsFeatureManager.removeCapabilityEventCallback(mCapabilityEventListener);
            mRcsFeatureManager = null;
        }
        // Destroy all the controllers
        mRequestManager.onDestroy();
        mEabController.onDestroy();
        mPublishController.onDestroy();
        mSubscribeController.onDestroy();
        mOptionsController.onDestroy();

        // Execute all the existing requests before quitting the looper.
        mLooper.quitSafely();
    }

    /**
     * Notify all associated classes that the carrier configuration has changed for the subId.
     */
    public void onCarrierConfigChanged() {
        mEabController.onCarrierConfigChanged();
        mPublishController.onCarrierConfigChanged();
        mSubscribeController.onCarrierConfigChanged();
        mOptionsController.onCarrierConfigChanged();
    }

    private void handleCachedCapabilityEvent() {
        Optional<Integer> requestPublishEvent = mCachedCapabilityEvent.getRequestPublishEvent();
        requestPublishEvent.ifPresent(triggerType ->
            onRequestPublishCapabilitiesFromService(triggerType));

        Optional<Boolean> unpublishEvent = mCachedCapabilityEvent.getUnpublishEvent();
        unpublishEvent.ifPresent(unpublish -> onUnpublish());

        Optional<SomeArgs> publishUpdatedEvent = mCachedCapabilityEvent.getPublishUpdatedEvent();
        publishUpdatedEvent.ifPresent(args -> {
            int reasonCode = (Integer) args.arg1;
            String reasonPhrase = (String) args.arg2;
            int reasonHeaderCause = (Integer) args.arg3;
            String reasonHeaderText = (String) args.arg4;
            onPublishUpdated(reasonCode, reasonPhrase, reasonHeaderCause, reasonHeaderText);
        });

        Optional<SomeArgs> remoteRequest = mCachedCapabilityEvent.getRemoteCapabilityRequestEvent();
        remoteRequest.ifPresent(args -> {
            Uri contactUri = (Uri) args.arg1;
            List<String> remoteCapabilities = (List<String>) args.arg2;
            IOptionsRequestCallback callback = (IOptionsRequestCallback) args.arg3;
            retrieveOptionsCapabilitiesForRemote(contactUri, remoteCapabilities, callback);
        });
        mCachedCapabilityEvent.clear();
    }

    /*
     * The implementation of the interface UceControllerCallback. These methods are called by other
     * controllers.
     */
    private UceControllerCallback mCtrlCallback = new UceControllerCallback() {
        @Override
        public List<EabCapabilityResult> getCapabilitiesFromCache(List<Uri> uris) {
            return mEabController.getCapabilities(uris);
        }

        @Override
        public List<EabCapabilityResult> getCapabilitiesFromCacheIncludingExpired(List<Uri> uris) {
            return mEabController.getCapabilitiesIncludingExpired(uris);
        }

        @Override
        public EabCapabilityResult getAvailabilityFromCache(Uri contactUri) {
            return mEabController.getAvailability(contactUri);
        }

        @Override
        public EabCapabilityResult getAvailabilityFromCacheIncludingExpired(Uri contactUri) {
            return mEabController.getAvailabilityIncludingExpired(contactUri);
        }

        @Override
        public void saveCapabilities(List<RcsContactUceCapability> contactCapabilities) {
            mEabController.saveCapabilities(contactCapabilities);
        }

        @Override
        public RcsContactUceCapability getDeviceCapabilities(@CapabilityMechanism int mechanism) {
            return mPublishController.getDeviceCapabilities(mechanism);
        }

        @Override
        public void refreshDeviceState(int sipCode, String reason, @RequestType int type) {
            mDeviceState.refreshDeviceState(sipCode, reason, type);
        }

        @Override
        public void resetDeviceState() {
            mDeviceState.resetDeviceState();
        }

        @Override
        public DeviceStateResult getDeviceState() {
            return mDeviceState.getCurrentState();
        }

        @Override
        public void setupResetDeviceStateTimer(long resetAfterSec) {
            mPublishController.setupResetDeviceStateTimer(resetAfterSec);
        }

        @Override
        public void clearResetDeviceStateTimer() {
            mPublishController.clearResetDeviceStateTimer();
        }

        @Override
        public void refreshCapabilities(@NonNull List<Uri> contactNumbers,
                @NonNull IRcsUceControllerCallback callback) throws RemoteException{
            logd("refreshCapabilities: " + contactNumbers.size());
            UceController.this.requestCapabilitiesInternal(contactNumbers, true, callback);
        }
    };

    @VisibleForTesting
    public void setUceControllerCallback(UceControllerCallback callback) {
        mCtrlCallback = callback;
    }

    /*
     * Setup the listener to listen to the requests and updates from ImsService.
     */
    private RcsFeatureManager.CapabilityExchangeEventCallback mCapabilityEventListener =
            new RcsFeatureManager.CapabilityExchangeEventCallback() {
                @Override
                public void onRequestPublishCapabilities(
                        @StackPublishTriggerType int triggerType) {
                    if (isRcsConnecting()) {
                        mCachedCapabilityEvent.setRequestPublishCapabilitiesEvent(triggerType);
                        return;
                    }
                    onRequestPublishCapabilitiesFromService(triggerType);
                }

                @Override
                public void onUnpublish() {
                    if (isRcsConnecting()) {
                        mCachedCapabilityEvent.setOnUnpublishEvent();
                        return;
                    }
                    UceController.this.onUnpublish();
                }

                @Override
                public void onPublishUpdated(int reasonCode, String reasonPhrase,
                        int reasonHeaderCause, String reasonHeaderText) {
                    if (isRcsConnecting()) {
                        mCachedCapabilityEvent.setOnPublishUpdatedEvent(reasonCode, reasonPhrase,
                                reasonHeaderCause, reasonHeaderText);
                        return;
                    }
                    UceController.this.onPublishUpdated(reasonCode, reasonPhrase,
                            reasonHeaderCause, reasonHeaderText);
                }

                @Override
                public void onRemoteCapabilityRequest(Uri contactUri,
                        List<String> remoteCapabilities, IOptionsRequestCallback cb) {
                    if (contactUri == null || remoteCapabilities == null || cb == null) {
                        logw("onRemoteCapabilityRequest: parameter cannot be null");
                        return;
                    }
                    if (isRcsConnecting()) {
                        mCachedCapabilityEvent.setRemoteCapabilityRequestEvent(contactUri,
                                remoteCapabilities, cb);
                        return;
                    }
                    retrieveOptionsCapabilitiesForRemote(contactUri, remoteCapabilities, cb);
                }
            };

    /**
     * Request to get the contacts' capabilities. This method will retrieve the capabilities from
     * the cache If the capabilities are out of date, it will trigger another request to get the
     * latest contact's capabilities from the network.
     */
    public void requestCapabilities(@NonNull List<Uri> uriList,
            @NonNull IRcsUceControllerCallback c) throws RemoteException {
        requestCapabilitiesInternal(uriList, false, c);
    }

    private void requestCapabilitiesInternal(@NonNull List<Uri> uriList, boolean skipFromCache,
            @NonNull IRcsUceControllerCallback c) throws RemoteException {
        if (uriList == null || uriList.isEmpty() || c == null) {
            logw("requestCapabilities: parameter is empty");
            if (c != null) {
                c.onError(RcsUceAdapter.ERROR_GENERIC_FAILURE, 0L);
            }
            return;
        }

        if (isUnavailable()) {
            logw("requestCapabilities: controller is unavailable");
            c.onError(RcsUceAdapter.ERROR_GENERIC_FAILURE, 0L);
            return;
        }

        // Return if the device is not allowed to execute UCE requests.
        DeviceStateResult deviceStateResult = mDeviceState.getCurrentState();
        if (deviceStateResult.isRequestForbidden()) {
            int deviceState = deviceStateResult.getDeviceState();
            int errorCode = deviceStateResult.getErrorCode()
                    .orElse(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            long retryAfterMillis = deviceStateResult.getRequestRetryAfterMillis();
            logw("requestCapabilities: The device is disallowed, deviceState= " + deviceState +
                    ", errorCode=" + errorCode + ", retryAfterMillis=" + retryAfterMillis);
            c.onError(errorCode, retryAfterMillis);
            return;
        }

        // Trigger the capabilities request task
        logd("requestCapabilities: size=" + uriList.size());
        mRequestManager.sendCapabilityRequest(uriList, skipFromCache, c);
    }

    /**
     * Request to get the contact's capabilities. It will check the availability cache first. If
     * the capability in the availability cache is expired then it will retrieve the capability
     * from the network.
     */
    public void requestAvailability(@NonNull Uri uri, @NonNull IRcsUceControllerCallback c)
            throws RemoteException {
        if (uri == null || c == null) {
            logw("requestAvailability: parameter is empty");
            if (c != null) {
                c.onError(RcsUceAdapter.ERROR_GENERIC_FAILURE, 0L);
            }
            return;
        }

        if (isUnavailable()) {
            logw("requestAvailability: controller is unavailable");
            c.onError(RcsUceAdapter.ERROR_GENERIC_FAILURE, 0L);
            return;
        }

        // Return if the device is not allowed to execute UCE requests.
        DeviceStateResult deviceStateResult = mDeviceState.getCurrentState();
        if (deviceStateResult.isRequestForbidden()) {
            int deviceState = deviceStateResult.getDeviceState();
            int errorCode = deviceStateResult.getErrorCode()
                    .orElse(RcsUceAdapter.ERROR_GENERIC_FAILURE);
            long retryAfterMillis = deviceStateResult.getRequestRetryAfterMillis();
            logw("requestAvailability: The device is disallowed, deviceState= " + deviceState +
                    ", errorCode=" + errorCode + ", retryAfterMillis=" + retryAfterMillis);
            c.onError(errorCode, retryAfterMillis);
            return;
        }

        // Trigger the availability request task
        logd("requestAvailability");
        mRequestManager.sendAvailabilityRequest(uri, c);
    }

    /**
     * Publish the device's capabilities. This request is triggered from the ImsService.
     */
    public void onRequestPublishCapabilitiesFromService(@StackPublishTriggerType int triggerType) {
        logd("onRequestPublishCapabilitiesFromService: " + triggerType);
        // Reset the device state when the service triggers to publish the device's capabilities
        mDeviceState.resetDeviceState();
        // Send the publish request.
        mPublishController.requestPublishCapabilitiesFromService(triggerType);
    }

    /**
     * This method is triggered by the ImsService to notify framework that the device's
     * capabilities has been unpublished from the network.
     */
    public void onUnpublish() {
        logi("onUnpublish");
        mPublishController.onUnpublish();
    }

    /**
     * This method is triggered by the ImsService to notify framework that the device's
     * publish status has been changed.
     */
    public void onPublishUpdated(int reasonCode, String reasonPhrase,
            int reasonHeaderCause, String reasonHeaderText) {
        logi("onPublishUpdated");
        mPublishController.onPublishUpdated(reasonCode, reasonPhrase,
                reasonHeaderCause, reasonHeaderText);
    }

    /**
     * Request publish the device's capabilities. This request is from the ImsService to send the
     * capabilities to the remote side.
     */
    public void retrieveOptionsCapabilitiesForRemote(@NonNull Uri contactUri,
            @NonNull List<String> remoteCapabilities, @NonNull IOptionsRequestCallback c) {
        logi("retrieveOptionsCapabilitiesForRemote");
        mRequestManager.retrieveCapabilitiesForRemote(contactUri, remoteCapabilities, c);
    }

    /**
     * Register a {@link PublishStateCallback} to receive the published state changed.
     */
    public void registerPublishStateCallback(@NonNull IRcsUcePublishStateCallback c,
            boolean supportPublishingState) {
        mPublishController.registerPublishStateCallback(c, supportPublishingState);
    }

    /**
     * Removes an existing {@link PublishStateCallback}.
     */
    public void unregisterPublishStateCallback(@NonNull IRcsUcePublishStateCallback c) {
        mPublishController.unregisterPublishStateCallback(c);
    }

    /**
     * Get the UCE publish state if the PUBLISH is supported by the carrier.
     */
    public @PublishState int getUcePublishState(boolean isSupportPublishingState) {
        return mPublishController.getUcePublishState(isSupportPublishingState);
    }

    /**
     * Add new feature tags to the Set used to calculate the capabilities in PUBLISH.
     * <p>
     * Used for testing ONLY.
     * @return the new capabilities that will be used for PUBLISH.
     */
    public RcsContactUceCapability addRegistrationOverrideCapabilities(Set<String> featureTags) {
        return mPublishController.addRegistrationOverrideCapabilities(featureTags);
    }

    /**
     * Remove existing feature tags to the Set used to calculate the capabilities in PUBLISH.
     * <p>
     * Used for testing ONLY.
     * @return the new capabilities that will be used for PUBLISH.
     */
    public RcsContactUceCapability removeRegistrationOverrideCapabilities(Set<String> featureTags) {
        return mPublishController.removeRegistrationOverrideCapabilities(featureTags);
    }

    /**
     * Clear all overrides in the Set used to calculate the capabilities in PUBLISH.
     * <p>
     * Used for testing ONLY.
     * @return the new capabilities that will be used for PUBLISH.
     */
    public RcsContactUceCapability clearRegistrationOverrideCapabilities() {
        return mPublishController.clearRegistrationOverrideCapabilities();
    }

    /**
     * @return current RcsContactUceCapability instance that will be used for PUBLISH.
     */
    public RcsContactUceCapability getLatestRcsContactUceCapability() {
        return mPublishController.getLatestRcsContactUceCapability();
    }

    /**
     * Get the PIDF XML associated with the last successful publish or null if not PUBLISHed to the
     * network.
     */
    public String getLastPidfXml() {
        return mPublishController.getLastPidfXml();
    }

    /**
     * Remove the device disallowed state.
     * <p>
     * Used for testing ONLY.
     */
    public void removeRequestDisallowedStatus() {
        logd("removeRequestDisallowedStatus");
        mDeviceState.resetDeviceState();
        mRequestManager.resetThrottlingList();
    }

    /**
     * Set the milliseconds of capabilities request timeout.
     * <p>
     * Used for testing ONLY.
     */
    public void setCapabilitiesRequestTimeout(long timeoutAfterMs) {
        logd("setCapabilitiesRequestTimeout: " + timeoutAfterMs);
        UceUtils.setCapRequestTimeoutAfterMillis(timeoutAfterMs);
    }

    /**
     * Get the subscription ID.
     */
    public int getSubId() {
        return mSubId;
    }

    /**
     * Check if the UceController is available.
     * @return true if RCS is connected without destroyed.
     */
    public boolean isUnavailable() {
        if (!isRcsConnected() || mIsDestroyedFlag) {
            return true;
        }
        return false;
    }

    private boolean isRcsConnecting() {
        return mRcsConnectedState == RCS_STATE_CONNECTING;
    }

    private boolean isRcsConnected() {
        return mRcsConnectedState == RCS_STATE_CONNECTED;
    }

    public void dump(PrintWriter printWriter) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("UceController" + "[subId: " + mSubId + "]:");
        pw.increaseIndent();

        pw.println("Log:");
        pw.increaseIndent();
        mLocalLog.dump(pw);
        pw.decreaseIndent();
        pw.println("---");

        mPublishController.dump(pw);

        pw.decreaseIndent();
    }

    private void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[D] " + log);
    }

    private void logi(String log) {
        Log.i(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[I] " + log);
    }

    private void logw(String log) {
        Log.w(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[W] " + log);
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId);
        builder.append("] ");
        return builder;
    }
}
