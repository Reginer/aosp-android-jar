/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.net.wifi.usd;

import static android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.net.wifi.IBooleanListener;
import android.net.wifi.flags.Flags;
import android.net.wifi.util.Environment;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * This class provides the APIs for managing Unsynchronized Service Discovery (USD). USD is a
 * mechanism that allows devices to discover services offered by other devices without requiring
 * prior time and channel synchronization. This feature is especially useful for quickly finding
 * services on new devices entering the range.
 *
 * <p>A publisher device makes its services discoverable, and a subscriber device actively
 * or passively searches for those services. Publishers in USD operate continuously, switching
 * between single and multiple channel states to advertise their services. When a subscriber
 * device receives a relevant service advertisement, it sends a follow-up message to the
 * publisher, temporarily pausing the publisher on its current channel to facilitate further
 * communication.
 *
 * <p>Once the discovery of device and service is complete, the subscriber and publisher perform
 * further service discovery in which they exchange follow-up messages. The follow-up messages
 * carry the service specific information useful for device and service configuration.
 *
 * <p>Note: This implementation adhere with Wi-Fi Aware Specification Version 4.0.
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@SystemService(Context.WIFI_USD_SERVICE)
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public class UsdManager {
    private final Context mContext;
    private final IUsdManager mService;
    private static final String TAG = UsdManager.class.getName();
    private static final SparseArray<IBooleanListener> sPublisherAvailabilityListenerMap =
            new SparseArray<>();
    private static final SparseArray<IBooleanListener> sSubscriberAvailabilityListenerMap =
            new SparseArray<>();

    /** @hide */
    public UsdManager(@NonNull Context context, @NonNull IUsdManager service) {
        mContext = context;
        mService = service;
    }

    /** @hide */
    public void sendMessage(int sessionId, int peerId, @NonNull byte[] message,
            @NonNull Executor executor, @NonNull Consumer<Boolean> resultCallback) {
        try {
            mService.sendMessage(sessionId, peerId, message, new IBooleanListener.Stub() {
                @Override
                public void onResult(boolean value) throws RemoteException {
                    Binder.clearCallingIdentity();
                    executor.execute(() -> resultCallback.accept(value));
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void cancelSubscribe(int sessionId) {
        try {
            mService.cancelSubscribe(sessionId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void cancelPublish(int sessionId) {
        try {
            mService.cancelPublish(sessionId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void updatePublish(int sessionId, byte[] serviceSpecificInfo) {
        try {
            mService.updatePublish(sessionId, serviceSpecificInfo);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets the characteristics of USD: a set of parameters which specify limitations on
     * configurations, e.g. maximum service name length.
     *
     * @return An object specifying the configuration limitation of USD. Return {@code null} if
     * USD feature is not supported.
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public @Nullable Characteristics getCharacteristics() {
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        try {
            return mService.getCharacteristics();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class PublishSessionCallbackProxy extends IPublishSessionCallback.Stub {
        private final Executor mExecutor;
        private final PublishSessionCallback mPublishSessionCallback;
        private final UsdManager mUsdManager;

        private PublishSessionCallbackProxy(UsdManager usdManager, Executor executor,
                PublishSessionCallback publishSessionCallback) {
            mUsdManager = usdManager;
            mExecutor = executor;
            mPublishSessionCallback = publishSessionCallback;
        }

        @Override
        public void onPublishFailed(int reasonCode) throws RemoteException {
            Log.d(TAG, "onPublishFailed (reasonCode = " + reasonCode + " )");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mPublishSessionCallback.onPublishFailed(reasonCode));
        }

        @Override
        public void onPublishStarted(int sessionId) throws RemoteException {
            Log.d(TAG, "onPublishStarted ( sessionId = " + sessionId + " )");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mPublishSessionCallback.onPublishStarted(
                    new PublishSession(mUsdManager, sessionId)));
        }

        @Override
        public void onPublishReplied(int peerId, byte[] ssi, int protoType, boolean isFsdEnabled)
                throws RemoteException {
            Log.d(TAG, "onPublishReplied ( peerId = " + peerId + ", protoType = " + protoType
                    + ", isFsdEnabled = " + isFsdEnabled + " )");
            Binder.clearCallingIdentity();
            DiscoveryResult discoveryResult = new DiscoveryResult.Builder(peerId)
                    .setServiceSpecificInfo(ssi)
                    .setServiceProtoType(protoType)
                    .setFsdEnabled(isFsdEnabled)
                    .build();
            mExecutor.execute(() -> mPublishSessionCallback.onPublishReplied(discoveryResult));
        }

        @Override
        public void onPublishSessionTerminated(int reasonCode) throws RemoteException {
            Log.d(TAG, "onPublishSessionTerminated ( reasonCode = " + reasonCode + " )");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mPublishSessionCallback.onSessionTerminated(reasonCode));
        }

        @Override
        public void onMessageReceived(int peerId, byte[] message) throws RemoteException {
            Log.d(TAG, "onMessageReceived ( peerId = " + peerId + " )");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mPublishSessionCallback.onMessageReceived(peerId, message));
        }
    }


    /**
     * Issue a request to the USD service to create a new publish session using the specified
     * {@link PublishConfig} configuration. The result of the publish operation are routed to the
     * callbacks of {@link PublishSessionCallback}.
     *
     * <p>Note: Maximum number of publish sessions are limited by
     * {@link Characteristics#getMaxNumberOfPublishSessions()}.
     *
     * @param publishConfig          The {@link PublishConfig} specifying the configuration of the
     *                               requested publish session.
     * @param executor               The Executor on whose thread to execute the callbacks of the
     *                               {@link PublishSessionCallback}
     * @param publishSessionCallback A {@link PublishSessionCallback} object to be used for session
     *                               event callback
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void publish(@NonNull PublishConfig publishConfig,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull PublishSessionCallback publishSessionCallback) {
        Objects.requireNonNull(publishConfig, "publishConfig must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(publishSessionCallback, "publishSessionCallback must not be null");
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        try {
            PublishSessionCallbackProxy publishSessionCallbackProxy =
                    new PublishSessionCallbackProxy(this, executor, publishSessionCallback);
            mService.publish(publishConfig, publishSessionCallbackProxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class SubscribeSessionCallbackProxy extends ISubscribeSessionCallback.Stub {
        private final UsdManager mUsdManager;
        private final Executor mExecutor;
        private final SubscribeSessionCallback mSubscribeSessionCallback;

        private SubscribeSessionCallbackProxy(UsdManager usdManager, Executor executor,
                SubscribeSessionCallback subscribeSessionCallback) {
            mUsdManager = usdManager;
            mExecutor = executor;
            mSubscribeSessionCallback = subscribeSessionCallback;
        }

        @Override
        public void onSubscribeFailed(int reasonCode) throws RemoteException {
            Log.d(TAG, "onSubscribeFailed (reasonCode = " + reasonCode + " )");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mSubscribeSessionCallback.onSubscribeFailed(reasonCode));
        }

        @Override
        public void onSubscribeStarted(int sessionId) throws RemoteException {
            Log.d(TAG, "onSubscribeStarted ( sessionId = " + sessionId + " )");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mSubscribeSessionCallback.onSubscribeStarted(
                    new SubscribeSession(mUsdManager, sessionId)));
        }

        @Override
        public void onSubscribeDiscovered(int peerId, byte[] ssi, int protoType,
                boolean isFsdEnabled)
                throws RemoteException {
            Log.d(TAG, "onSubscribeDiscovered ( peerId = " + peerId + ", protoType = " + protoType
                    + ", isFsdEnabled = " + isFsdEnabled + " )");
            Binder.clearCallingIdentity();
            DiscoveryResult discoveryResult = new DiscoveryResult.Builder(peerId)
                    .setServiceSpecificInfo(ssi)
                    .setServiceProtoType(protoType)
                    .setFsdEnabled(isFsdEnabled)
                    .build();
            mExecutor.execute(() -> mSubscribeSessionCallback.onServiceDiscovered(discoveryResult));
        }

        @Override
        public void onSubscribeSessionTerminated(int reasonCode) throws RemoteException {
            Log.d(TAG, "onSubscribeSessionTerminated ( reasonCode = " + reasonCode + " )");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mSubscribeSessionCallback.onSessionTerminated(reasonCode));
        }

        @Override
        public void onMessageReceived(int peerId, byte[] message) throws RemoteException {
            Log.d(TAG, "onMessageReceived ( peerId = " + peerId + " )");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mSubscribeSessionCallback.onMessageReceived(peerId, message));
        }
    }


    /**
     * Issue a request to the USD service to create a new subscribe session using the specified
     * {@link SubscribeConfig} configuration. The result of the subscribe operation are
     * routed to
     * the callbacks of {@link SubscribeSessionCallback}.
     *
     * <p>Note: Maximum number of subscribe sessions are limited by
     * {@link Characteristics#getMaxNumberOfSubscribeSessions()}.
     *
     * @param subscribeConfig          The {@link SubscribeConfig} specifying the
     *                                 configuration of the requested subscribe session.
     * @param executor                 The Executor on whose thread to execute the callbacks of
     *                                 the {@link SubscribeSessionCallback}
     * @param subscribeSessionCallback A {@link SubscribeSessionCallback} object to be used for
     *                                 session event callback
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void subscribe(@NonNull SubscribeConfig subscribeConfig,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull SubscribeSessionCallback subscribeSessionCallback) {
        Objects.requireNonNull(subscribeConfig, "subscribeConfig must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(subscribeSessionCallback,
                "subscribeSessionCallback must not be null");
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        try {
            SubscribeSessionCallbackProxy subscribeSessionCallbackProxy =
                    new SubscribeSessionCallbackProxy(this, executor, subscribeSessionCallback);
            mService.subscribe(subscribeConfig, subscribeSessionCallbackProxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Register for subscriber availability. Concurrent operations such as Station, SoftAP, Wi-Fi
     * Aware, Wi-Fi Direct ..etc. impact the current availability of subscriber functionality.
     * Current availability status will be returned immediately after registering.
     *
     * @param executor The executor on which callback will be invoked.
     * @param callback An asynchronous callback that will return {@code Boolean} indicating
     *                 whether subscriber is available or not. {@code true} if subscriber is
     *                 available, otherwise unavailable.
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void registerSubscriberStatusListener(@NonNull Executor executor,
            @NonNull Consumer<Boolean> callback) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        final int callbackHash = System.identityHashCode(callback);
        synchronized (sSubscriberAvailabilityListenerMap) {
            try {
                IBooleanListener listener = new IBooleanListener.Stub() {
                    @Override
                    public void onResult(boolean value) throws RemoteException {
                        Binder.clearCallingIdentity();
                        executor.execute(() -> callback.accept(value));
                    }
                };
                sSubscriberAvailabilityListenerMap.put(callbackHash, listener);
                mService.registerSubscriberStatusListener(listener);
            } catch (RemoteException e) {
                sSubscriberAvailabilityListenerMap.remove(callbackHash);
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Unregister the callback previously registered for subscriber availability.
     *
     * @param callback A registered callback.
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void unregisterSubscriberStatusListener(@NonNull Consumer<Boolean> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        final int callbackHash = System.identityHashCode(callback);
        synchronized (sSubscriberAvailabilityListenerMap) {
            if (!sSubscriberAvailabilityListenerMap.contains(callbackHash)) {
                Log.w(TAG, "Unknown callback");
                return;
            }
            try {
                mService.unregisterSubscriberStatusListener(
                        sSubscriberAvailabilityListenerMap.get(callbackHash));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } finally {
                sSubscriberAvailabilityListenerMap.remove(callbackHash);
            }
        }
    }

    /**
     * Register for publisher availability. Concurrent operations such as Station, SoftAP, Wi-Fi
     * Aware, Wi-Fi Direct ..etc. impact the current availability of publisher functionality.
     * Current availability status will be returned immediately after registering.
     *
     * @param executor The executor on which callback will be invoked.
     * @param callback An asynchronous callback that will return {@code Boolean} indicating
     *                 whether publisher is available or not. {@code true} if publisher is
     *                 available, otherwise unavailable.
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void registerPublisherStatusListener(@NonNull Executor executor,
            @NonNull Consumer<Boolean> callback) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        final int callbackHash = System.identityHashCode(callback);
        synchronized (sPublisherAvailabilityListenerMap) {
            try {
                IBooleanListener listener = new IBooleanListener.Stub() {
                    @Override
                    public void onResult(boolean value) throws RemoteException {
                        Binder.clearCallingIdentity();
                        executor.execute(() -> callback.accept(value));
                    }
                };
                sPublisherAvailabilityListenerMap.put(callbackHash, listener);
                mService.registerPublisherStatusListener(listener);
            } catch (RemoteException e) {
                sPublisherAvailabilityListenerMap.remove(callbackHash);
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Unregister the callback previously registered for publisher availability.
     *
     * @param callback A registered callback.
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void unregisterPublisherStatusListener(@NonNull Consumer<Boolean> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        final int callbackHash = System.identityHashCode(callback);
        synchronized (sPublisherAvailabilityListenerMap) {
            if (!sPublisherAvailabilityListenerMap.contains(callbackHash)) {
                Log.w(TAG, "Unknown callback");
                return;
            }
            try {
                mService.unregisterPublisherStatusListener(
                        sPublisherAvailabilityListenerMap.get(callbackHash));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } finally {
                sPublisherAvailabilityListenerMap.remove(callbackHash);
            }
        }
    }
}
