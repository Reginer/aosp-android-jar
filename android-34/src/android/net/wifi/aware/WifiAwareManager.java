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

package android.net.wifi.aware;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.NEARBY_WIFI_DEVICES;
import static android.Manifest.permission.OVERRIDE_WIFI_CONFIG;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.IBooleanListener;
import android.net.wifi.IIntegerListener;
import android.net.wifi.IListListener;
import android.net.wifi.WifiManager;
import android.net.wifi.util.HexEncoding;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.HandlerExecutor;
import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * This class provides the primary API for managing Wi-Fi Aware operations:
 * discovery and peer-to-peer data connections.
 * <p>
 * The class provides access to:
 * <ul>
 * <li>Initialize a Aware cluster (peer-to-peer synchronization). Refer to
 * {@link #attach(AttachCallback, Handler)}.
 * <li>Create discovery sessions (publish or subscribe sessions). Refer to
 * {@link WifiAwareSession#publish(PublishConfig, DiscoverySessionCallback, Handler)} and
 * {@link WifiAwareSession#subscribe(SubscribeConfig, DiscoverySessionCallback, Handler)}.
 * <li>Create a Aware network specifier to be used with
 * {@link ConnectivityManager#requestNetwork(NetworkRequest, ConnectivityManager.NetworkCallback)}
 * to set-up a Aware connection with a peer. Refer to {@link WifiAwareNetworkSpecifier.Builder}.
 * </ul>
 * <p>
 *     Aware may not be usable when Wi-Fi is disabled (and other conditions). To validate that
 *     the functionality is available use the {@link #isAvailable()} function. To track
 *     changes in Aware usability register for the {@link #ACTION_WIFI_AWARE_STATE_CHANGED}
 *     broadcast. Note that this broadcast is not sticky - you should register for it and then
 *     check the above API to avoid a race condition.
 * <p>
 *     An application must use {@link #attach(AttachCallback, Handler)} to initialize a
 *     Aware cluster - before making any other Aware operation. Aware cluster membership is a
 *     device-wide operation - the API guarantees that the device is in a cluster or joins a
 *     Aware cluster (or starts one if none can be found). Information about attach success (or
 *     failure) are returned in callbacks of {@link AttachCallback}. Proceed with Aware
 *     discovery or connection setup only after receiving confirmation that Aware attach
 *     succeeded - {@link AttachCallback#onAttached(WifiAwareSession)}. When an
 *     application is finished using Aware it <b>must</b> use the
 *     {@link WifiAwareSession#close()} API to indicate to the Aware service that the device
 *     may detach from the Aware cluster. The device will actually disable Aware once the last
 *     application detaches.
 * <p>
 *     Once a Aware attach is confirmed use the
 *     {@link WifiAwareSession#publish(PublishConfig, DiscoverySessionCallback, Handler)}
 *     or
 *     {@link WifiAwareSession#subscribe(SubscribeConfig, DiscoverySessionCallback,
 *     Handler)} to create publish or subscribe Aware discovery sessions. Events are called on the
 *     provided callback object {@link DiscoverySessionCallback}. Specifically, the
 *     {@link DiscoverySessionCallback#onPublishStarted(PublishDiscoverySession)}
 *     and
 *     {@link DiscoverySessionCallback#onSubscribeStarted(
 *SubscribeDiscoverySession)}
 *     return {@link PublishDiscoverySession} and
 *     {@link SubscribeDiscoverySession}
 *     objects respectively on which additional session operations can be performed, e.g. updating
 *     the session {@link PublishDiscoverySession#updatePublish(PublishConfig)} and
 *     {@link SubscribeDiscoverySession#updateSubscribe(SubscribeConfig)}. Sessions can
 *     also be used to send messages using the
 *     {@link DiscoverySession#sendMessage(PeerHandle, int, byte[])} APIs. When an
 *     application is finished with a discovery session it <b>must</b> terminate it using the
 *     {@link DiscoverySession#close()} API.
 * <p>
 *    Creating connections between Aware devices is managed by the standard
 *    {@link ConnectivityManager#requestNetwork(NetworkRequest,
 *    ConnectivityManager.NetworkCallback)}.
 *    The {@link NetworkRequest} object should be constructed with:
 *    <ul>
 *        <li>{@link NetworkRequest.Builder#addTransportType(int)} of
 *        {@link android.net.NetworkCapabilities#TRANSPORT_WIFI_AWARE}.
 *        <li>{@link NetworkRequest.Builder#setNetworkSpecifier(String)} using
 *        {@link WifiAwareNetworkSpecifier.Builder}.
 *    </ul>
 */
@SystemService(Context.WIFI_AWARE_SERVICE)
public class WifiAwareManager {
    private static final String TAG = "WifiAwareManager";
    private static final boolean DBG = false;
    private static final boolean VDBG = false; // STOPSHIP if true

    /**
     * Broadcast intent action to indicate that the state of Wi-Fi Aware availability has changed
     * and all active Aware sessions are no longer usable. Use the {@link #isAvailable()} to query
     * the current status.
     * This broadcast is <b>not</b> sticky, use the {@link #isAvailable()} API after registering
     * the broadcast to check the current state of Wi-Fi Aware.
     * <p>Note: The broadcast is only delivered to registered receivers - no manifest registered
     * components will be launched.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_WIFI_AWARE_STATE_CHANGED =
            "android.net.wifi.aware.action.WIFI_AWARE_STATE_CHANGED";
    /**
     * Intent broadcast sent whenever Wi-Fi Aware resource availability has changed. The resources
     * are attached with the {@link #EXTRA_AWARE_RESOURCES} extra. The resources can also be
     * obtained using the {@link #getAvailableAwareResources()} method. To receive this broadcast,
     * apps must hold {@link android.Manifest.permission#ACCESS_WIFI_STATE}.
     * <p>Note: The broadcast is only delivered to registered receivers - no manifest registered
     * components will be launched.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(ACCESS_WIFI_STATE)
    public static final String ACTION_WIFI_AWARE_RESOURCE_CHANGED =
            "android.net.wifi.aware.action.WIFI_AWARE_RESOURCE_CHANGED";

    /**
     * Sent as a part of {@link #ACTION_WIFI_AWARE_RESOURCE_CHANGED} that contains an instance of
     * {@link AwareResources} representing the current Wi-Fi Aware resources.
     */
    public static final String EXTRA_AWARE_RESOURCES =
            "android.net.wifi.aware.extra.AWARE_RESOURCES";

    /** @hide */
    @IntDef({
            WIFI_AWARE_DATA_PATH_ROLE_INITIATOR, WIFI_AWARE_DATA_PATH_ROLE_RESPONDER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DataPathRole {
    }

    /**
     * Connection creation role is that of INITIATOR. Used to create a network specifier string
     * when requesting a Aware network.
     *
     * @see WifiAwareSession#createNetworkSpecifierOpen(int, byte[])
     * @see WifiAwareSession#createNetworkSpecifierPassphrase(int, byte[], String)
     */
    public static final int WIFI_AWARE_DATA_PATH_ROLE_INITIATOR = 0;

    /**
     * Connection creation role is that of RESPONDER. Used to create a network specifier string
     * when requesting a Aware network.
     *
     * @see WifiAwareSession#createNetworkSpecifierOpen(int, byte[])
     * @see WifiAwareSession#createNetworkSpecifierPassphrase(int, byte[], String)
     */
    public static final int WIFI_AWARE_DATA_PATH_ROLE_RESPONDER = 1;

    /** @hide */
    @IntDef({
            WIFI_AWARE_DISCOVERY_LOST_REASON_UNKNOWN,
            WIFI_AWARE_DISCOVERY_LOST_REASON_PEER_NOT_VISIBLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DiscoveryLostReasonCode {
    }

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onServiceLost(PeerHandle, int)}
     * indicating that the service was lost for unknown reason.
     */
    public static final int WIFI_AWARE_DISCOVERY_LOST_REASON_UNKNOWN = 0;

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onServiceLost(PeerHandle, int)}
     * indicating that the service advertised by the peer is no longer visible. This may be because
     * the peer is out of range or because the peer stopped advertising this service.
     */
    public static final int WIFI_AWARE_DISCOVERY_LOST_REASON_PEER_NOT_VISIBLE = 1;

    /** @hide */
    @IntDef({
            WIFI_AWARE_SUSPEND_REDUNDANT_REQUEST,
            WIFI_AWARE_SUSPEND_INVALID_SESSION,
            WIFI_AWARE_SUSPEND_CANNOT_SUSPEND,
            WIFI_AWARE_SUSPEND_INTERNAL_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SessionSuspensionFailedReasonCode {}

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onSessionSuspendFailed(int)} when the
     * session is already suspended.
     * @hide
     */
    @SystemApi
    public static final int WIFI_AWARE_SUSPEND_REDUNDANT_REQUEST = 0;

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onSessionSuspendFailed(int)} when the
     * specified session does not support suspension.
      @hide
     */
    @SystemApi
    public static final int WIFI_AWARE_SUSPEND_INVALID_SESSION = 1;

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onSessionSuspendFailed(int)} when the
     * session could not be suspended due to more than one app using it.
      @hide
     */
    @SystemApi
    public static final int WIFI_AWARE_SUSPEND_CANNOT_SUSPEND = 2;

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onSessionSuspendFailed(int)} when an
     * error is encountered with the request.
      @hide
     */
    @SystemApi
    public static final int WIFI_AWARE_SUSPEND_INTERNAL_ERROR = 3;

    /** @hide */
    @IntDef({
            WIFI_AWARE_RESUME_REDUNDANT_REQUEST,
            WIFI_AWARE_RESUME_INVALID_SESSION,
            WIFI_AWARE_RESUME_INTERNAL_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SessionResumptionFailedReasonCode {}

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onSessionResumeFailed(int)} when the
     * session is not suspended.
     * @hide
     */
    @SystemApi
    public static final int WIFI_AWARE_RESUME_REDUNDANT_REQUEST = 0;

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onSessionResumeFailed(int)} when the
     * specified session does not support suspension.
      @hide
     */
    @SystemApi
    public static final int WIFI_AWARE_RESUME_INVALID_SESSION = 1;

    /**
     * Reason code provided in {@link DiscoverySessionCallback#onSessionResumeFailed(int)} when an
     * error is encountered with the request.
      @hide
     */
    @SystemApi
    public static final int WIFI_AWARE_RESUME_INTERNAL_ERROR = 2;

    private final Context mContext;
    private final IWifiAwareManager mService;

    private final Object mLock = new Object(); // lock access to the following vars

    /** @hide */
    public WifiAwareManager(@NonNull Context context, @NonNull IWifiAwareManager service) {
        mContext = context;
        mService = service;
    }

    /**
     * Returns the current status of Aware API: whether or not Aware is available. To track
     * changes in the state of Aware API register for the
     * {@link #ACTION_WIFI_AWARE_STATE_CHANGED} broadcast.
     *
     * @return A boolean indicating whether the app can use the Aware API at this time (true) or
     * not (false).
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public boolean isAvailable() {
        try {
            return mService.isUsageEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the current status of the Aware service: whether or not the device is already attached
     * to an Aware cluster. To attach to an Aware cluster, please use
     * {@link #attach(AttachCallback, Handler)} or
     * {@link #attach(AttachCallback, IdentityChangedListener, Handler)}.
     * @return A boolean indicating whether the device is attached to a cluster at this time (true)
     *         or not (false).
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public boolean isDeviceAttached() {
        try {
            return mService.isDeviceAttached();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the device support for setting a channel requirement in a data-path request. If true
     * the channel set by
     * {@link WifiAwareNetworkSpecifier.Builder#setChannelFrequencyMhz(int, boolean)} will be
     * honored, otherwise it will be ignored.
     * @return True is the device support set channel on data-path request, false otherwise.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public boolean isSetChannelOnDataPathSupported() {
        try {
            return mService.isSetChannelOnDataPathSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable the Wifi Aware Instant communication mode. If the device doesn't support this feature
     * calling this API will result no action.
     * <p>
     * Note: before {@link android.os.Build.VERSION_CODES#TIRAMISU}, only system app can use this
     * API. Start with {@link android.os.Build.VERSION_CODES#TIRAMISU} apps hold
     * {@link android.Manifest.permission#OVERRIDE_WIFI_CONFIG} are allowed to use it.
     *
     * @see Characteristics#isInstantCommunicationModeSupported()
     * @param enable true for enable, false otherwise.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = {CHANGE_WIFI_STATE, OVERRIDE_WIFI_CONFIG})
    public void enableInstantCommunicationMode(boolean enable) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        try {
            mService.enableInstantCommunicationMode(mContext.getOpPackageName(), enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the current status of the Wifi Aware instant communication mode.
     * If the device doesn't support this feature, return will always be false.
     * @see Characteristics#isInstantCommunicationModeSupported()
     * @return true if it is enabled, false otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(ACCESS_WIFI_STATE)
    public boolean isInstantCommunicationModeEnabled() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        try {
            return mService.isInstantCommunicationModeEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the characteristics of the Wi-Fi Aware interface: a set of parameters which specify
     * limitations on configurations, e.g. the maximum service name length.
     * <p>
     * May return {@code null} if the Wi-Fi Aware service is not initialized. Use
     * {@link #attach(AttachCallback, Handler)} or
     * {@link #attach(AttachCallback, IdentityChangedListener, Handler)} to initialize the Wi-Fi
     * Aware service.
     *
     * @return An object specifying configuration limitations of Aware.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public @Nullable Characteristics getCharacteristics() {
        try {
            return mService.getCharacteristics();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the available resources of the Wi-Fi aware service: a set of parameters which specify
     * limitations on service usage, e.g the number of data-paths which could be created.
     * <p>
     * May return {@code null} if the Wi-Fi Aware service is not initialized. Use
     * {@link #attach(AttachCallback, Handler)} or
     * {@link #attach(AttachCallback, IdentityChangedListener, Handler)} to initialize the Wi-Fi
     * Aware service.
     *
     * @return An object specifying the currently available resource of the Wi-Fi Aware service.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public @Nullable AwareResources getAvailableAwareResources() {
        try {
            return mService.getAvailableAwareResources();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Attach to the Wi-Fi Aware service - enabling the application to create discovery sessions or
     * create connections to peers. The device will attach to an existing cluster if it can find
     * one or create a new cluster (if it is the first to enable Aware in its vicinity). Results
     * (e.g. successful attach to a cluster) are provided to the {@code attachCallback} object.
     * An application <b>must</b> call {@link WifiAwareSession#close()} when done with the
     * Wi-Fi Aware object.
     * <p>
     * Note: a Aware cluster is a shared resource - if the device is already attached to a cluster
     * then this function will simply indicate success immediately using the same {@code
     * attachCallback}.
     *
     * @param attachCallback A callback for attach events, extended from
     * {@link AttachCallback}.
     * @param handler The Handler on whose thread to execute the callbacks of the {@code
     * attachCallback} object. If a null is provided then the application's main thread will be
     *                used.
     */
    @RequiresPermission(allOf = {
            ACCESS_WIFI_STATE,
            CHANGE_WIFI_STATE
    })
    public void attach(@NonNull AttachCallback attachCallback, @Nullable Handler handler) {
        attach(handler, null, attachCallback, null, false, null);
    }

    /**
     * Attach to the Wi-Fi Aware service - enabling the application to create discovery sessions or
     * create connections to peers. The device will attach to an existing cluster if it can find
     * one or create a new cluster (if it is the first to enable Aware in its vicinity). Results
     * (e.g. successful attach to a cluster) are provided to the {@code attachCallback} object.
     * An application <b>must</b> call {@link WifiAwareSession#close()} when done with the
     * Wi-Fi Aware object.
     * <p>
     * Note: a Aware cluster is a shared resource - if the device is already attached to a cluster
     * then this function will simply indicate success immediately using the same {@code
     * attachCallback}.
     * <p>
     * This version of the API attaches a listener to receive the MAC address of the Aware interface
     * on startup and whenever it is updated (it is randomized at regular intervals for privacy).
     *
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * Apps without {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} or
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} can use the
     * {@link #attach(AttachCallback, Handler)} version.
     * Note that aside from permission requirements the {@link IdentityChangedListener} will wake up
     * the host at regular intervals causing higher power consumption, do not use it unless the
     * information is necessary (e.g. for out-of-band discovery).
     *
     * @param attachCallback A callback for attach events, extended from
     * {@link AttachCallback}.
     * @param identityChangedListener A callback for changed identity or cluster ID, extended from
     * {@link IdentityChangedListener}.
     * @param handler The Handler on whose thread to execute the callbacks of the {@code
     * attachCallback} and {@code identityChangedListener} objects. If a null is provided then the
     *                application's main thread will be used.
     */
    @RequiresPermission(allOf = {
            ACCESS_WIFI_STATE,
            CHANGE_WIFI_STATE,
            ACCESS_FINE_LOCATION,
            NEARBY_WIFI_DEVICES}, conditional = true)
    public void attach(@NonNull AttachCallback attachCallback,
            @NonNull IdentityChangedListener identityChangedListener,
            @Nullable Handler handler) {
        attach(handler, null, attachCallback, identityChangedListener, false, null);
    }

    /** @hide */
    public void attach(Handler handler, ConfigRequest configRequest,
            AttachCallback attachCallback,
            IdentityChangedListener identityChangedListener, boolean forOffloading,
            Executor executor) {
        if (VDBG) {
            Log.v(TAG, "attach(): handler=" + handler + ", callback=" + attachCallback
                    + ", configRequest=" + configRequest + ", identityChangedListener="
                    + identityChangedListener + ", forOffloading" + forOffloading);
        }

        if (attachCallback == null) {
            throw new IllegalArgumentException("Null callback provided");
        }

        synchronized (mLock) {
            Executor localExecutor = executor;
            if (localExecutor == null) {
                localExecutor = new HandlerExecutor((handler == null)
                        ? new Handler(Looper.getMainLooper()) : handler);
            }

            try {
                Binder binder = new Binder();
                Bundle extras = new Bundle();
                if (SdkLevel.isAtLeastS()) {
                    extras.putParcelable(WifiManager.EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                            mContext.getAttributionSource());
                }
                mService.connect(binder, mContext.getOpPackageName(), mContext.getAttributionTag(),
                        new WifiAwareEventCallbackProxy(this, localExecutor, binder,
                                attachCallback, identityChangedListener), configRequest,
                        identityChangedListener != null, extras, forOffloading);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /** @hide */
    public void disconnect(int clientId, Binder binder) {
        if (VDBG) Log.v(TAG, "disconnect()");

        try {
            mService.disconnect(clientId, binder);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void setMasterPreference(int clientId, Binder binder, int mp) {
        if (VDBG) Log.v(TAG, "setMasterPreference()");

        try {
            mService.setMasterPreference(clientId, binder, mp);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    public void getMasterPreference(int clientId, Binder binder, @NonNull Executor executor,
            @NonNull Consumer<Integer> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.getMasterPreference(clientId, binder,
                    new IIntegerListener.Stub() {
                        public void onResult(int value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> resultsCallback.accept(value));
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void publish(int clientId, Looper looper, PublishConfig publishConfig,
            DiscoverySessionCallback callback) {
        if (VDBG) Log.v(TAG, "publish(): clientId=" + clientId + ", config=" + publishConfig);

        if (callback == null) {
            throw new IllegalArgumentException("Null callback provided");
        }

        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(WifiManager.EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            mService.publish(mContext.getOpPackageName(), mContext.getAttributionTag(), clientId,
                    publishConfig,
                    new WifiAwareDiscoverySessionCallbackProxy(this, looper, true, callback,
                            clientId), extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void updatePublish(int clientId, int sessionId, PublishConfig publishConfig) {
        if (VDBG) {
            Log.v(TAG, "updatePublish(): clientId=" + clientId + ",sessionId=" + sessionId
                    + ", config=" + publishConfig);
        }

        try {
            mService.updatePublish(clientId, sessionId, publishConfig);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void subscribe(int clientId, Looper looper, SubscribeConfig subscribeConfig,
            DiscoverySessionCallback callback) {
        if (VDBG) {
            if (VDBG) {
                Log.v(TAG,
                        "subscribe(): clientId=" + clientId + ", config=" + subscribeConfig);
            }
        }

        if (callback == null) {
            throw new IllegalArgumentException("Null callback provided");
        }

        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(WifiManager.EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            mService.subscribe(mContext.getOpPackageName(), mContext.getAttributionTag(), clientId,
                    subscribeConfig,
                    new WifiAwareDiscoverySessionCallbackProxy(this, looper, false, callback,
                            clientId), extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void updateSubscribe(int clientId, int sessionId, SubscribeConfig subscribeConfig) {
        if (VDBG) {
            Log.v(TAG, "updateSubscribe(): clientId=" + clientId + ",sessionId=" + sessionId
                    + ", config=" + subscribeConfig);
        }

        try {
            mService.updateSubscribe(clientId, sessionId, subscribeConfig);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void terminateSession(int clientId, int sessionId) {
        if (VDBG) {
            Log.d(TAG,
                    "terminateSession(): clientId=" + clientId + ", sessionId=" + sessionId);
        }

        try {
            mService.terminateSession(clientId, sessionId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public void sendMessage(int clientId, int sessionId, PeerHandle peerHandle, byte[] message,
            int messageId, int retryCount) {
        if (peerHandle == null) {
            throw new IllegalArgumentException(
                    "sendMessage: invalid peerHandle - must be non-null");
        }

        if (VDBG) {
            Log.v(TAG, "sendMessage(): clientId=" + clientId + ", sessionId=" + sessionId
                    + ", peerHandle=" + peerHandle.peerId + ", messageId="
                    + messageId + ", retryCount=" + retryCount);
        }

        try {
            mService.sendMessage(clientId, sessionId, peerHandle.peerId, message, messageId,
                    retryCount);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    public void initiateNanPairingSetupRequest(int clientId, int sessionId, PeerHandle peerHandle,
            String password, String pairingDeviceAlias, int cipherSuite) {
        if (peerHandle == null) {
            throw new IllegalArgumentException(
                    "initiateNanPairingRequest: invalid peerHandle - must be non-null");
        }
        if (VDBG) {
            Log.v(TAG, "initiateNanPairingRequest(): clientId=" + clientId
                    + ", sessionId=" + sessionId + ", peerHandle=" + peerHandle.peerId);
        }
        try {
            mService.initiateNanPairingSetupRequest(clientId, sessionId, peerHandle.peerId,
                    password, pairingDeviceAlias, cipherSuite);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    public void responseNanPairingSetupRequest(int clientId, int sessionId, PeerHandle peerHandle,
            int requestId, String password, String pairingDeviceAlias, boolean accept,
            int cipherSuite) {
        if (peerHandle == null) {
            throw new IllegalArgumentException(
                    "initiateNanPairingRequest: invalid peerHandle - must be non-null");
        }
        if (VDBG) {
            Log.v(TAG, "initiateNanPairingRequest(): clientId=" + clientId
                    + ", sessionId=" + sessionId + ", peerHandle=" + peerHandle.peerId);
        }
        try {
            mService.responseNanPairingSetupRequest(clientId, sessionId, peerHandle.peerId,
                    requestId, password, pairingDeviceAlias, accept, cipherSuite);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    public void initiateBootStrappingSetupRequest(int clientId, int sessionId,
            PeerHandle peerHandle, int method) {
        if (peerHandle == null) {
            throw new IllegalArgumentException(
                    "initiateBootStrappingSetupRequest: invalid peerHandle - must be non-null");
        }
        if (VDBG) {
            Log.v(TAG, "initiateBootStrappingSetupRequest(): clientId=" + clientId
                    + ", sessionId=" + sessionId + ", peerHandle=" + peerHandle.peerId);
        }
        try {
            mService.initiateBootStrappingSetupRequest(clientId, sessionId, peerHandle.peerId,
                    method);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    public void requestMacAddresses(int uid, int[] peerIds,
            IWifiAwareMacAddressProvider callback) {
        try {
            mService.requestMacAddresses(uid, peerIds, callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public NetworkSpecifier createNetworkSpecifier(int clientId, int role, int sessionId,
            @NonNull PeerHandle peerHandle, @Nullable byte[] pmk, @Nullable String passphrase) {
        if (VDBG) {
            Log.v(TAG, "createNetworkSpecifier: role=" + role + ", sessionId=" + sessionId
                    + ", peerHandle=" + ((peerHandle == null) ? peerHandle : peerHandle.peerId)
                    + ", pmk=" + ((pmk == null) ? "null" : "non-null")
                    + ", passphrase=" + ((passphrase == null) ? "null" : "non-null"));
        }

        if (!WifiAwareUtils.isLegacyVersion(mContext, Build.VERSION_CODES.Q)) {
            throw new UnsupportedOperationException(
                    "API deprecated - use WifiAwareNetworkSpecifier.Builder");
        }

        if (role != WIFI_AWARE_DATA_PATH_ROLE_INITIATOR
                && role != WIFI_AWARE_DATA_PATH_ROLE_RESPONDER) {
            throw new IllegalArgumentException(
                    "createNetworkSpecifier: Invalid 'role' argument when creating a network "
                            + "specifier");
        }
        if (role == WIFI_AWARE_DATA_PATH_ROLE_INITIATOR || !WifiAwareUtils.isLegacyVersion(mContext,
                Build.VERSION_CODES.P)) {
            if (peerHandle == null) {
                throw new IllegalArgumentException(
                        "createNetworkSpecifier: Invalid peer handle - cannot be null");
            }
        }

        return new WifiAwareNetworkSpecifier(
                (peerHandle == null) ? WifiAwareNetworkSpecifier.NETWORK_SPECIFIER_TYPE_IB_ANY_PEER
                        : WifiAwareNetworkSpecifier.NETWORK_SPECIFIER_TYPE_IB,
                role,
                clientId,
                sessionId,
                peerHandle != null ? peerHandle.peerId : 0, // 0 is an invalid peer ID
                null, // peerMac (not used in this method)
                pmk,
                passphrase,
                0, // no port info for deprecated IB APIs
                -1); // no transport info for deprecated IB APIs
    }

    /** @hide */
    public NetworkSpecifier createNetworkSpecifier(int clientId, @DataPathRole int role,
            @NonNull byte[] peer, @Nullable byte[] pmk, @Nullable String passphrase) {
        if (VDBG) {
            Log.v(TAG, "createNetworkSpecifier: role=" + role
                    + ", pmk=" + ((pmk == null) ? "null" : "non-null")
                    + ", passphrase=" + ((passphrase == null) ? "null" : "non-null"));
        }

        if (role != WIFI_AWARE_DATA_PATH_ROLE_INITIATOR
                && role != WIFI_AWARE_DATA_PATH_ROLE_RESPONDER) {
            throw new IllegalArgumentException(
                    "createNetworkSpecifier: Invalid 'role' argument when creating a network "
                            + "specifier");
        }
        if (role == WIFI_AWARE_DATA_PATH_ROLE_INITIATOR || !WifiAwareUtils.isLegacyVersion(mContext,
                Build.VERSION_CODES.P)) {
            if (peer == null) {
                throw new IllegalArgumentException(
                        "createNetworkSpecifier: Invalid peer MAC - cannot be null");
            }
        }
        if (peer != null && peer.length != 6) {
            throw new IllegalArgumentException("createNetworkSpecifier: Invalid peer MAC address");
        }

        return new WifiAwareNetworkSpecifier(
                (peer == null) ? WifiAwareNetworkSpecifier.NETWORK_SPECIFIER_TYPE_OOB_ANY_PEER
                        : WifiAwareNetworkSpecifier.NETWORK_SPECIFIER_TYPE_OOB,
                role,
                clientId,
                0, // 0 is an invalid session ID
                0, // 0 is an invalid peer ID
                peer,
                pmk,
                passphrase,
                0, // no port info for OOB APIs
                -1); // no transport protocol info for OOB APIs
    }

    private static class WifiAwareEventCallbackProxy extends IWifiAwareEventCallback.Stub {
        private final WeakReference<WifiAwareManager> mAwareManager;
        private final Binder mBinder;
        private final Executor mExecutor;

        private final AttachCallback mAttachCallback;

        private final IdentityChangedListener mIdentityChangedListener;

        /**
         * Constructs a {@link AttachCallback} using the specified looper.
         * All callbacks will delivered on the thread of the specified looper.
         *
         * @param executor The executor to execute the callbacks.
         */
        WifiAwareEventCallbackProxy(WifiAwareManager mgr, Executor executor, Binder binder,
                final AttachCallback attachCallback,
                final IdentityChangedListener identityChangedListener) {
            mAwareManager = new WeakReference<>(mgr);
            mExecutor = executor;
            mBinder = binder;
            mAttachCallback = attachCallback;
            mIdentityChangedListener = identityChangedListener;
        }

        @Override
        public void onConnectSuccess(int clientId) {
            if (VDBG) Log.v(TAG, "onConnectSuccess");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                WifiAwareManager mgr = mAwareManager.get();
                if (mgr == null) {
                    Log.w(TAG, "WifiAwareEventCallbackProxy: handleMessage post GC");
                    return;
                }
                mAttachCallback.onAttached(new WifiAwareSession(mgr, mBinder, clientId));
            });
        }

        @Override
        public void onConnectFail(int reason) {
            if (VDBG) Log.v(TAG, "onConnectFail: reason=" + reason);
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                WifiAwareManager mgr = mAwareManager.get();
                if (mgr == null) {
                    Log.w(TAG, "WifiAwareEventCallbackProxy: handleMessage post GC");
                    return;
                }
                mAwareManager.clear();
                mAttachCallback.onAttachFailed();
            });
        }

        @Override
        public void onIdentityChanged(byte[] mac) {
            if (VDBG) Log.v(TAG, "onIdentityChanged: mac=" + new String(HexEncoding.encode(mac)));
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                WifiAwareManager mgr = mAwareManager.get();
                if (mgr == null) {
                    Log.w(TAG, "WifiAwareEventCallbackProxy: handleMessage post GC");
                    return;
                }
                if (mIdentityChangedListener == null) {
                    Log.e(TAG, "CALLBACK_IDENTITY_CHANGED: null listener.");
                } else {
                    mIdentityChangedListener.onIdentityChanged(mac);
                }
            });
        }

        @Override
        public void onAttachTerminate() {
            if (VDBG) Log.v(TAG, "onAwareSessionTerminated");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                WifiAwareManager mgr = mAwareManager.get();
                if (mgr == null) {
                    Log.w(TAG, "WifiAwareEventCallbackProxy: handleMessage post GC");
                    return;
                }
                mAwareManager.clear();
                mAttachCallback.onAwareSessionTerminated();
            });
        }

        @Override
        public void onClusterIdChanged(
                @IdentityChangedListener.ClusterChangeEvent int clusterEventType,
                byte[] clusterId) {
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                WifiAwareManager mgr = mAwareManager.get();
                if (mgr == null) {
                    Log.w(TAG, "WifiAwareEventCallbackProxy: handleMessage post GC");
                    return;
                }
                if (mIdentityChangedListener == null) {
                    Log.e(TAG, "CALLBACK_CLUSTER_ID_CHANGED: null listener.");
                } else {
                    try {
                        mIdentityChangedListener.onClusterIdChanged(
                                clusterEventType, MacAddress.fromBytes(clusterId));
                    } catch (IllegalArgumentException iae) {
                        Log.e(TAG, " Invalid MAC address, " + iae);
                    }
                }
            });
        }
    }

    private static class WifiAwareDiscoverySessionCallbackProxy extends
            IWifiAwareDiscoverySessionCallback.Stub {
        private final WeakReference<WifiAwareManager> mAwareManager;
        private final boolean mIsPublish;
        private final DiscoverySessionCallback mOriginalCallback;
        private final int mClientId;

        private final Handler mHandler;
        private DiscoverySession mSession;

        WifiAwareDiscoverySessionCallbackProxy(WifiAwareManager mgr, Looper looper,
                boolean isPublish, DiscoverySessionCallback originalCallback,
                int clientId) {
            mAwareManager = new WeakReference<>(mgr);
            mIsPublish = isPublish;
            mOriginalCallback = originalCallback;
            mClientId = clientId;

            if (VDBG) {
                Log.v(TAG, "WifiAwareDiscoverySessionCallbackProxy ctor: isPublish=" + isPublish);
            }

            mHandler = new Handler(looper);
        }

        @Override
        public void onSessionStarted(int sessionId) {
            if (VDBG) Log.v(TAG, "onSessionStarted: sessionId=" + sessionId);
            mHandler.post(() -> onProxySessionStarted(sessionId));
        }

        @Override
        public void onSessionConfigSuccess() {
            if (VDBG) Log.v(TAG, "onSessionConfigSuccess");
            mHandler.post(mOriginalCallback::onSessionConfigUpdated);
        }

        @Override
        public void onSessionConfigFail(int reason) {
            if (VDBG) Log.v(TAG, "onSessionConfigFail: reason=" + reason);
            mHandler.post(() -> {
                mOriginalCallback.onSessionConfigFailed();
                if (mSession == null) {
                    /* creation failed (as opposed to update failing) */
                    mAwareManager.clear();
                }
            });
        }

        @Override
        public void onSessionTerminated(int reason) {
            if (VDBG) Log.v(TAG, "onSessionTerminated: reason=" + reason);
            mHandler.post(() -> onProxySessionTerminated(reason));
        }

        @Override
        public void onSessionSuspendSucceeded() {
            if (VDBG) Log.v(TAG, "onSessionSuspendSucceeded");
            mHandler.post(mOriginalCallback::onSessionSuspendSucceeded);
        }

        @Override
        public void onSessionSuspendFail(int reason) {
            if (VDBG) Log.v(TAG, "onSessionSuspendFail: reason=" + reason);
            mHandler.post(() -> mOriginalCallback.onSessionSuspendFailed(reason));
        }

        @Override
        public void onSessionResumeSucceeded() {
            if (VDBG) Log.v(TAG, "onSessionResumeSucceeded");
            mHandler.post(mOriginalCallback::onSessionResumeSucceeded);
        }

        @Override
        public void onSessionResumeFail(int reason) {
            if (VDBG) Log.v(TAG, "onSessionResumeFail: reason=" + reason);
            mHandler.post(() -> mOriginalCallback.onSessionResumeFailed(reason));
        }

        @Override
        public void onMatch(int peerId, byte[] serviceSpecificInfo, byte[] matchFilter,
                int peerCipherSuite, byte[] scid, String pairingAlias,
                AwarePairingConfig pairingConfig) {
            if (VDBG) Log.v(TAG, "onMatch: peerId=" + peerId);

            mHandler.post(() -> {
                List<byte[]> matchFilterList = getMatchFilterList(matchFilter);
                mOriginalCallback.onServiceDiscovered(new PeerHandle(peerId), serviceSpecificInfo,
                        matchFilterList);
                mOriginalCallback.onServiceDiscovered(
                        new ServiceDiscoveryInfo(new PeerHandle(peerId), peerCipherSuite,
                                serviceSpecificInfo, matchFilterList, scid, pairingAlias,
                                pairingConfig));
            });
        }

        private List<byte[]> getMatchFilterList(byte[] matchFilter) {
            List<byte[]> matchFilterList = null;
            try {
                matchFilterList = new TlvBufferUtils.TlvIterable(0, 1, matchFilter).toList();
            } catch (BufferOverflowException e) {
                matchFilterList = Collections.emptyList();
                Log.e(TAG, "onServiceDiscovered: invalid match filter byte array '"
                        + new String(HexEncoding.encode(matchFilter))
                        + "' - cannot be parsed: e=" + e);
            }
            return matchFilterList;
        }

        @Override
        public void onMatchWithDistance(int peerId, byte[] serviceSpecificInfo, byte[] matchFilter,
                int distanceMm, int peerCipherSuite, byte[] scid, String pairingAlias,
                AwarePairingConfig pairingConfig) {
            if (VDBG) {
                Log.v(TAG, "onMatchWithDistance: peerId=" + peerId + ", distanceMm=" + distanceMm);
            }
            mHandler.post(() -> {
                List<byte[]> matchFilterList = getMatchFilterList(matchFilter);
                mOriginalCallback.onServiceDiscoveredWithinRange(
                        new PeerHandle(peerId),
                        serviceSpecificInfo,
                        matchFilterList, distanceMm);
                mOriginalCallback.onServiceDiscoveredWithinRange(
                        new ServiceDiscoveryInfo(
                                new PeerHandle(peerId),
                                peerCipherSuite,
                                serviceSpecificInfo,
                                matchFilterList,
                                scid,
                                pairingAlias,
                                pairingConfig),
                        distanceMm);
            });
        }
        @Override
        public void onMatchExpired(int peerId) {
            if (VDBG) {
                Log.v(TAG, "onMatchExpired: peerId=" + peerId);
            }
            mHandler.post(() ->
                    mOriginalCallback.onServiceLost(new PeerHandle(peerId),
                            WIFI_AWARE_DISCOVERY_LOST_REASON_PEER_NOT_VISIBLE));
        }

        @Override
        public void onMessageSendSuccess(int messageId) {
            if (VDBG) Log.v(TAG, "onMessageSendSuccess");
            mHandler.post(() -> mOriginalCallback.onMessageSendSucceeded(messageId));
        }

        @Override
        public void onMessageSendFail(int messageId, int reason) {
            if (VDBG) Log.v(TAG, "onMessageSendFail: reason=" + reason);
            mHandler.post(() -> mOriginalCallback.onMessageSendFailed(messageId));
        }

        @Override
        public void onMessageReceived(int peerId, byte[] message) {
            if (VDBG) {
                Log.v(TAG, "onMessageReceived: peerId=" + peerId);
            }
            mHandler.post(() -> mOriginalCallback.onMessageReceived(new PeerHandle(peerId),
                    message));
        }

        @Override
        public void onPairingSetupRequestReceived(int peerId, int requestId) {
            mHandler.post(() ->
                    mOriginalCallback.onPairingSetupRequestReceived(new PeerHandle(peerId),
                            requestId));
        }
        @Override
        public void onPairingSetupConfirmed(int peerId, boolean accept, String alias) {
            if (accept) {
                mHandler.post(() -> mOriginalCallback
                        .onPairingSetupSucceeded(new PeerHandle(peerId), alias));
            } else {
                mHandler.post(() -> mOriginalCallback
                        .onPairingSetupFailed(new PeerHandle(peerId)));
            }
        }
        @Override
        public void onPairingVerificationConfirmed(int peerId, boolean accept, String alias) {
            if (accept) {
                mHandler.post(() -> mOriginalCallback.onPairingVerificationSucceed(
                        new PeerHandle(peerId), alias));
            } else {
                mHandler.post(() -> mOriginalCallback
                        .onPairingVerificationFailed(new PeerHandle(peerId)));
            }
        }
        @Override
        public void onBootstrappingVerificationConfirmed(int peerId, boolean accept, int method) {
            if (accept) {
                mHandler.post(() -> mOriginalCallback.onBootstrappingSucceeded(
                        new PeerHandle(peerId), method));
            } else {
                mHandler.post(() -> mOriginalCallback.onBootstrappingFailed(
                        new PeerHandle(peerId)));
            }
        }

        /*
         * Proxies methods
         */
        public void onProxySessionStarted(int sessionId) {
            if (VDBG) Log.v(TAG, "Proxy: onSessionStarted: sessionId=" + sessionId);
            if (mSession != null) {
                Log.e(TAG,
                        "onSessionStarted: sessionId=" + sessionId + ": session already created!?");
                throw new IllegalStateException(
                        "onSessionStarted: sessionId=" + sessionId + ": session already created!?");
            }

            WifiAwareManager mgr = mAwareManager.get();
            if (mgr == null) {
                Log.w(TAG, "onProxySessionStarted: mgr GC'd");
                return;
            }

            if (mIsPublish) {
                PublishDiscoverySession session = new PublishDiscoverySession(mgr,
                        mClientId, sessionId);
                mSession = session;
                mOriginalCallback.onPublishStarted(session);
            } else {
                SubscribeDiscoverySession
                        session = new SubscribeDiscoverySession(mgr, mClientId, sessionId);
                mSession = session;
                mOriginalCallback.onSubscribeStarted(session);
            }
        }

        public void onProxySessionTerminated(int reason) {
            if (VDBG) Log.v(TAG, "Proxy: onSessionTerminated: reason=" + reason);
            if (mSession != null) {
                mSession.setTerminated();
                mSession = null;
            } else {
                Log.w(TAG, "Proxy: onSessionTerminated called but mSession is null!?");
            }
            mAwareManager.clear();
            mOriginalCallback.onSessionTerminated();
        }
    }

    /**
     * Set Wi-Fi Aware protocol parameters.
     * @hide
     * @param params An object contain specified parameters. Use {@code null} to remove previously
     *               set configuration and restore default behavior.
     */
    @SystemApi
    @RequiresPermission(allOf = {OVERRIDE_WIFI_CONFIG,
            CHANGE_WIFI_STATE})
    public void setAwareParams(@Nullable AwareParams params) {
        try {
            mService.setAwareParams(params);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     *  Set all Wi-Fi Aware sessions created by the calling app to be opportunistic. Opportunistic
     *  Wi-Fi Aware sessions are considered low priority and may be torn down (the sessions or the
     *  Aware interface) if there are resource conflicts.
     *
     * @param enabled True to configure all Wi-Fi Aware sessions by the calling app as
     *                Opportunistic. False by default.
     */
    @RequiresPermission(CHANGE_WIFI_STATE)
    public void setOpportunisticModeEnabled(boolean enabled) {
        try {
            mService.setOpportunisticModeEnabled(mContext.getOpPackageName(), enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Indicate whether all Wi-Fi Aware sessions created by the calling app are opportunistic as
     * defined and configured by {@link #setOpportunisticModeEnabled(boolean)}
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return boolean
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void isOpportunisticModeEnabled(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");

        try {
            mService.isOpportunisticModeEnabled(mContext.getOpPackageName(),
                    new IBooleanListener.Stub() {
                        @Override
                        public void onResult(boolean value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Reset all paired devices setup by the caller by
     * {@link DiscoverySession#initiatePairingRequest(PeerHandle, String, int, String)} and
     * {@link DiscoverySession#acceptPairingRequest(int, PeerHandle, String, int, String)}
     */
    @RequiresPermission(CHANGE_WIFI_STATE)
    public void resetPairedDevices() {
        try {
            mService.resetPairedDevices(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove the target paired device setup by the caller by
     * {@link DiscoverySession#initiatePairingRequest(PeerHandle, String, int, String)} and
     * {@link DiscoverySession#acceptPairingRequest(int, PeerHandle, String, int, String)}
     * @param alias The alias set by the caller
     */
    @RequiresPermission(CHANGE_WIFI_STATE)
    public void removePairedDevice(@NonNull String alias) {
        try {
            mService.removePairedDevice(mContext.getOpPackageName(), alias);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get all the paired devices configured by the calling app.
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return a list of paired devices'
     *                        alias
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void getPairedDevices(@NonNull Executor executor,
            @NonNull Consumer<List<String>> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.getPairedDevices(
                    mContext.getOpPackageName(),
                    new IListListener.Stub() {
                        public void onResult(List value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> resultsCallback.accept(value));
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    public void suspend(int clientId, int sessionId) {
        try {
            mService.suspend(clientId, sessionId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    public void resume(int clientId, int sessionId) {
        try {
            mService.resume(clientId, sessionId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
    /**
     * Attach to the Wi-Fi Aware service as an offload session. All discovery sessions and
     * connections will be handled via out-of-band connections.
     * The Aware session created by this attach method will have the lowest priority when resource
     * conflicts arise (e.g. Aware has to be torn down to create other WiFi interfaces).
     *
     * @param executor       The executor to execute the listener of the {@code attachCallback}
     *                       object.
     * @param attachCallback A callback for attach events, extended from
     *                       {@link AttachCallback}.
     * @hide
     * @see #attach(AttachCallback, Handler)
     */
    @SystemApi
    @RequiresPermission(allOf = {ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, OVERRIDE_WIFI_CONFIG})
    public void attachOffload(@NonNull @CallbackExecutor Executor executor,
            @NonNull AttachCallback attachCallback) {
        if (executor == null) {
            throw new IllegalArgumentException("Null executor provided");
        }
        attach(null, null, attachCallback, null, true, executor);
    }

}
