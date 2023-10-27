/*
 * Copyright 2020 The Android Open Source Project
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

package android.uwb;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.Manifest.permission;
import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.GuardedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * This class provides a way to perform Ultra Wideband (UWB) operations such as querying the
 * device's capabilities and determining the distance and angle between the local device and a
 * remote device.
 *
 * <p>To get a {@link UwbManager}, call the <code>Context.getSystemService(UwbManager.class)</code>.
 *
 * <p> Note: This API surface uses opaque {@link PersistableBundle} params. These params are to be
 * created using the provided UWB support library. The support library is present in this
 * location on AOSP: <code>packages/modules/Uwb/service/support_lib/</code>
 *
 * @hide
 */
@SystemApi
@SystemService(Context.UWB_SERVICE)
public final class UwbManager {
    private static final String TAG = "UwbManager";

    private final Context mContext;
    private final IUwbAdapter mUwbAdapter;
    private final AdapterStateListener mAdapterStateListener;
    private final RangingManager mRangingManager;
    private final UwbVendorUciCallbackListener mUwbVendorUciCallbackListener;
    private final UwbOemExtensionCallbackListener mUwbOemExtensionCallbackListener;

    /**
     * Interface for receiving UWB adapter state changes
     */
    public interface AdapterStateCallback {
        /**
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                STATE_CHANGED_REASON_SESSION_STARTED,
                STATE_CHANGED_REASON_ALL_SESSIONS_CLOSED,
                STATE_CHANGED_REASON_SYSTEM_POLICY,
                STATE_CHANGED_REASON_SYSTEM_BOOT,
                STATE_CHANGED_REASON_ERROR_UNKNOWN,
                STATE_CHANGED_REASON_SYSTEM_REGULATION})
        @interface StateChangedReason {}

        /**
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                STATE_ENABLED_INACTIVE,
                STATE_ENABLED_ACTIVE,
                STATE_DISABLED})
        @interface State {}

        /**
         * Indicates that the state change was due to opening of first UWB session
         */
        int STATE_CHANGED_REASON_SESSION_STARTED = 0;

        /**
         * Indicates that the state change was due to closure of all UWB sessions
         */
        int STATE_CHANGED_REASON_ALL_SESSIONS_CLOSED = 1;

        /**
         * Indicates that the state change was due to changes in system policy
         */
        int STATE_CHANGED_REASON_SYSTEM_POLICY = 2;

        /**
         * Indicates that the current state is due to a system boot
         */
        int STATE_CHANGED_REASON_SYSTEM_BOOT = 3;

        /**
         * Indicates that the state change was due to some unknown error
         */
        int STATE_CHANGED_REASON_ERROR_UNKNOWN = 4;

        /**
         * Indicates that the state change is due to a system regulation.
         */
        int STATE_CHANGED_REASON_SYSTEM_REGULATION = 5;

        /**
         * Indicates that UWB is disabled on device
         */
        int STATE_DISABLED = 0;
        /**
         * Indicates that UWB is enabled on device but has no active ranging sessions
         */
        int STATE_ENABLED_INACTIVE = 1;

        /**
         * Indicates that UWB is enabled and has active ranging session
         */
        int STATE_ENABLED_ACTIVE = 2;

        /**
         * Invoked when underlying UWB adapter's state is changed
         * <p>Invoked with the adapter's current state after registering an
         * {@link AdapterStateCallback} using
         * {@link UwbManager#registerAdapterStateCallback(Executor, AdapterStateCallback)}.
         *
         * <p>Possible reasons for the state to change are
         * {@link #STATE_CHANGED_REASON_SESSION_STARTED},
         * {@link #STATE_CHANGED_REASON_ALL_SESSIONS_CLOSED},
         * {@link #STATE_CHANGED_REASON_SYSTEM_POLICY},
         * {@link #STATE_CHANGED_REASON_SYSTEM_BOOT},
         * {@link #STATE_CHANGED_REASON_ERROR_UNKNOWN}.
         * {@link #STATE_CHANGED_REASON_SYSTEM_REGULATION}.
         *
         * <p>Possible values for the UWB state are
         * {@link #STATE_ENABLED_INACTIVE},
         * {@link #STATE_ENABLED_ACTIVE},
         * {@link #STATE_DISABLED}.
         *
         * @param state the UWB state; inactive, active or disabled
         * @param reason the reason for the state change
         */
        void onStateChanged(@State int state, @StateChangedReason int reason);
    }

    /**
     * Abstract class for receiving ADF provisioning state.
     * Should be extended by applications and set when calling
     * {@link UwbManager#provisionProfileAdfByScript(PersistableBundle, Executor,
     * AdfProvisionStateCallback)}
     */
    public abstract static class AdfProvisionStateCallback {
        private final AdfProvisionStateCallbackProxy mAdfProvisionStateCallbackProxy;

        public AdfProvisionStateCallback() {
            mAdfProvisionStateCallbackProxy = new AdfProvisionStateCallbackProxy();
        }

        /**
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                REASON_INVALID_OID,
                REASON_SE_FAILURE,
                REASON_UNKNOWN
        })
        @interface Reason { }

        /**
         * Indicates that the OID provided was not valid.
         */
        public static final int REASON_INVALID_OID = 1;

        /**
         * Indicates that there was some SE (secure element) failure while provisioning.
         */
        public static final int REASON_SE_FAILURE = 2;

        /**
         * No known reason for the failure.
         */
        public static final int REASON_UNKNOWN = 3;

        /**
         * Invoked when {@link UwbManager#provisionProfileAdfByScript(PersistableBundle, Executor,
         * AdfProvisionStateCallback)} is successful.
         *
         * @param params protocol specific params that provide the caller with provisioning info
         **/
        public abstract void onProfileAdfsProvisioned(@NonNull PersistableBundle params);

        /**
         * Invoked when {@link UwbManager#provisionProfileAdfByScript(PersistableBundle, Executor,
         * AdfProvisionStateCallback)} fails.
         *
         * @param reason Reason for failure
         * @param params protocol specific parameters to indicate failure reason
         */
        public abstract void onProfileAdfsProvisionFailed(
                @Reason int reason, @NonNull PersistableBundle params);

        /*package*/
        @NonNull
        AdfProvisionStateCallbackProxy getProxy() {
            return mAdfProvisionStateCallbackProxy;
        }

        private static class AdfProvisionStateCallbackProxy extends
                IUwbAdfProvisionStateCallbacks.Stub {
            private final Object mLock = new Object();
            @Nullable
            @GuardedBy("mLock")
            private Executor mExecutor;
            @Nullable
            @GuardedBy("mLock")
            private AdfProvisionStateCallback mCallback;

            AdfProvisionStateCallbackProxy() {
                mCallback = null;
                mExecutor = null;
            }

            /*package*/ void initProxy(@NonNull Executor executor,
                    @NonNull AdfProvisionStateCallback callback) {
                synchronized (mLock) {
                    mExecutor = executor;
                    mCallback = callback;
                }
            }

            /*package*/ void cleanUpProxy() {
                synchronized (mLock) {
                    mExecutor = null;
                    mCallback = null;
                }
            }

            @Override
            public void onProfileAdfsProvisioned(@NonNull PersistableBundle params) {
                Log.v(TAG, "AdfProvisionStateCallbackProxy: onProfileAdfsProvisioned : " + params);
                AdfProvisionStateCallback callback;
                Executor executor;
                synchronized (mLock) {
                    executor = mExecutor;
                    callback = mCallback;
                }
                if (callback == null || executor == null) {
                    return;
                }
                Binder.clearCallingIdentity();
                executor.execute(() -> callback.onProfileAdfsProvisioned(params));
                cleanUpProxy();
            }

            @Override
            public void onProfileAdfsProvisionFailed(@AdfProvisionStateCallback.Reason int reason,
                    @NonNull PersistableBundle params) {
                Log.v(TAG, "AdfProvisionStateCallbackProxy: onProfileAdfsProvisionFailed : "
                        + reason + ", " + params);
                AdfProvisionStateCallback callback;
                Executor executor;
                synchronized (mLock) {
                    executor = mExecutor;
                    callback = mCallback;
                }
                if (callback == null || executor == null) {
                    return;
                }
                Binder.clearCallingIdentity();
                executor.execute(() -> callback.onProfileAdfsProvisionFailed(reason, params));
                cleanUpProxy();
            }
        }
    }

    /**
     * Interface for receiving vendor UCI responses and notifications.
     */
    public interface UwbVendorUciCallback {
        /**
         * Invoked when a vendor specific UCI response is received.
         *
         * @param gid Group ID of the command. This needs to be one of the vendor reserved GIDs from
         *            the UCI specification.
         * @param oid Opcode ID of the command. This is left to the OEM / vendor to decide.
         * @param payload containing vendor Uci message payload.
         */
        void onVendorUciResponse(
                @IntRange(from = 0, to = 15) int gid, int oid, @NonNull byte[] payload);

        /**
         * Invoked when a vendor specific UCI notification is received.
         *
         * @param gid Group ID of the command. This needs to be one of the vendor reserved GIDs from
         *            the UCI specification.
         * @param oid Opcode ID of the command. This is left to the OEM / vendor to decide.
         * @param payload containing vendor Uci message payload.
         */
        void onVendorUciNotification(
                @IntRange(from = 9, to = 15) int gid, int oid, @NonNull byte[] payload);
    }


    /**
     * @hide
     * Vendor configuration successful for the session
     */
    public static final int VENDOR_SET_SESSION_CONFIGURATION_SUCCESS = 0;

    /**
     * @hide
     * Failure to set vendor configuration for the session
     */
    public static final int VENDOR_SET_SESSION_CONFIGURATION_FAILURE = 1;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            VENDOR_SET_SESSION_CONFIGURATION_SUCCESS,
            VENDOR_SET_SESSION_CONFIGURATION_FAILURE,
    })
    @interface VendorConfigStatus {}


    /**
     * Interface for Oem extensions on ongoing session
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public interface UwbOemExtensionCallback {
        /**
         * Invoked when session status changes.
         *
         * @param sessionStatusBundle session related info
         */
        void onSessionStatusNotificationReceived(@NonNull PersistableBundle sessionStatusBundle);

        /**
         * Invoked when DeviceStatusNotification is received from UCI.
         *
         * @param deviceStatusBundle device state
         */
        void onDeviceStatusNotificationReceived(@NonNull PersistableBundle deviceStatusBundle);

        /**
         * Invoked when session configuration is complete.
         *
         * @param openSessionBundle Session Params
         * @return Error code
         */
        @NonNull @VendorConfigStatus int onSessionConfigurationComplete(
                @NonNull PersistableBundle openSessionBundle);

        /**
         * Invoked when ranging report is generated.
         *
         * @param rangingReport ranging report generated
         * @return Oem modified ranging report
         */
        @NonNull RangingReport onRangingReportReceived(
                @NonNull RangingReport rangingReport);

        /**
         * Invoked to check pointed target decision by Oem.
         *
         * @param pointedTargetBundle pointed target params
         * @return Oem pointed status
         */
        boolean onCheckPointedTarget(@NonNull PersistableBundle pointedTargetBundle);
    }

    /**
     * Use <code>Context.getSystemService(UwbManager.class)</code> to get an instance.
     *
     * @param ctx Context of the client.
     * @param adapter an instance of an {@link android.uwb.IUwbAdapter}
     * @hide
     */
    public UwbManager(@NonNull Context ctx, @NonNull IUwbAdapter adapter) {
        mContext = ctx;
        mUwbAdapter = adapter;
        mAdapterStateListener = new AdapterStateListener(adapter);
        mRangingManager = new RangingManager(adapter);
        mUwbVendorUciCallbackListener = new UwbVendorUciCallbackListener(adapter);
        mUwbOemExtensionCallbackListener = new UwbOemExtensionCallbackListener(adapter);
    }

    /**
     * Register an {@link AdapterStateCallback} to listen for UWB adapter state changes
     * <p>The provided callback will be invoked by the given {@link Executor}.
     *
     * <p>When first registering a callback, the callbacks's
     * {@link AdapterStateCallback#onStateChanged(int, int)} is immediately invoked to indicate
     * the current state of the underlying UWB adapter with the most recent
     * {@link AdapterStateCallback.StateChangedReason} that caused the change.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link AdapterStateCallback}
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public void registerAdapterStateCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull AdapterStateCallback callback) {
        mAdapterStateListener.register(executor, callback);
    }

    /**
     * Unregister the specified {@link AdapterStateCallback}
     * <p>The same {@link AdapterStateCallback} object used when calling
     * {@link #registerAdapterStateCallback(Executor, AdapterStateCallback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link AdapterStateCallback}
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public void unregisterAdapterStateCallback(@NonNull AdapterStateCallback callback) {
        mAdapterStateListener.unregister(callback);
    }

    /**
     * Register an {@link UwbVendorUciCallback} to listen for UWB vendor responses and notifications
     * <p>The provided callback will be invoked by the given {@link Executor}.
     *
     * <p>When first registering a callback, the callbacks's
     * {@link UwbVendorUciCallback#onVendorUciCallBack(byte[])} is immediately invoked to
     * notify the vendor notification.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link UwbVendorUciCallback}
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public void registerUwbVendorUciCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull UwbVendorUciCallback callback) {
        mUwbVendorUciCallbackListener.register(executor, callback);
    }

    /**
     * Unregister the specified {@link UwbVendorUciCallback}
     *
     * <p>The same {@link UwbVendorUciCallback} object used when calling
     * {@link #registerUwbVendorUciCallback(Executor, UwbVendorUciCallback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when application process goes away
     *
     * @param callback user implementation of the {@link UwbVendorUciCallback}
     */
    public void unregisterUwbVendorUciCallback(@NonNull UwbVendorUciCallback callback) {
        mUwbVendorUciCallbackListener.unregister(callback);
    }

    /**
     * Register an {@link UwbOemExtensionCallback} to listen for UWB oem extension callbacks
     * <p>The provided callback will be invoked by the given {@link Executor}.
     *
     * @param executor an {@link Executor} to execute given callback
     * @param callback oem implementation of {@link UwbOemExtensionCallback}
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public void registerUwbOemExtensionCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull UwbOemExtensionCallback callback) {
        mUwbOemExtensionCallbackListener.register(executor, callback);
    }

    /**
     * Unregister the specified {@link UwbOemExtensionCallback}
     *
     * <p>The same {@link UwbOemExtensionCallback} object used when calling
     * {@link #registerUwbOemExtensionCallback(Executor, UwbOemExtensionCallback)} must be used.
     *
     * <p>Callbacks are automatically unregistered when an application process goes away
     *
     * @param callback oem implementation of {@link UwbOemExtensionCallback}
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public void unregisterUwbOemExtensionCallback(@NonNull UwbOemExtensionCallback callback) {
        mUwbOemExtensionCallbackListener.unregister(callback);
    }

    /**
     * Get a {@link PersistableBundle} with the supported UWB protocols and parameters.
     * <p>The {@link PersistableBundle} should be parsed using a support library
     *
     * <p>Android reserves the '^android.*' namespace</p>
     *
     * @return {@link PersistableBundle} of the device's supported UWB protocols and parameters
     */
    @NonNull
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public PersistableBundle getSpecificationInfo() {
        return getSpecificationInfoInternal(/* chipId= */ null);
    }

    /**
     * Get a {@link PersistableBundle} with the supported UWB protocols and parameters.
     *
     * @see #getSpecificationInfo() if you don't need multi-HAL support
     *
     * @param chipId identifier of UWB chip for multi-HAL devices
     *
     * @return {@link PersistableBundle} of the device's supported UWB protocols and parameters
     */
    // TODO(b/205614701): Add documentation about how to find the relevant chipId
    @NonNull
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public PersistableBundle getSpecificationInfo(@NonNull String chipId) {
        checkNotNull(chipId);
        return getSpecificationInfoInternal(chipId);
    }

    private PersistableBundle getSpecificationInfoInternal(String chipId) {
        try {
            return mUwbAdapter.getSpecificationInfo(chipId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the timestamp resolution for events in nanoseconds
     * <p>This value defines the maximum error of all timestamps for events reported to
     * {@link RangingSession.Callback}.
     *
     * @return the timestamp resolution in nanoseconds
     */
    @SuppressLint("MethodNameUnits")
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public long elapsedRealtimeResolutionNanos() {
        return elapsedRealtimeResolutionNanosInternal(/* chipId= */ null);
    }

    /**
     * Get the timestamp resolution for events in nanoseconds
     *
     * @see #elapsedRealtimeResolutionNanos() if you don't need multi-HAL support
     *
     * @param chipId identifier of UWB chip for multi-HAL devices
     *
     * @return the timestamp resolution in nanoseconds
     */
    @SuppressLint("MethodNameUnits")
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public long elapsedRealtimeResolutionNanos(@NonNull String chipId) {
        checkNotNull(chipId);
        return elapsedRealtimeResolutionNanosInternal(chipId);
    }

    private long elapsedRealtimeResolutionNanosInternal(String chipId) {
        try {
            return mUwbAdapter.getTimestampResolutionNanos(chipId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Open a {@link RangingSession} with the given parameters
     * <p>The {@link RangingSession.Callback#onOpened(RangingSession)} function is called with a
     * {@link RangingSession} object used to control ranging when the session is successfully
     * opened.
     *
     * if this session uses FIRA defined profile (not custom profile), this triggers:
     *   - OOB discovery using service UUID
     *   - OOB connection establishment after discovery for session params
     *     negotiation.
     *   - Secure element interactions needed for dynamic STS based session establishment.
     *   - Setup the UWB session based on the parameters negotiated via OOB.
     *   - Note: The OOB flow requires additional BLE Permissions
     *     {permission.BLUETOOTH_ADVERTISE/permission.BLUETOOTH_SCAN
     *      and permission.BLUETOOTH_CONNECT}.
     *
     * <p>If a session cannot be opened, then
     * {@link RangingSession.Callback#onClosed(int, PersistableBundle)} will be invoked with the
     * appropriate {@link RangingSession.Callback.Reason}.
     *
     * <p>An open {@link RangingSession} will be automatically closed if client application process
     * dies.
     *
     * <p>A UWB support library must be used in order to construct the {@code parameter}
     * {@link PersistableBundle}.
     *
     * @param parameters the parameters that define the ranging session
     * @param executor {@link Executor} to run callbacks
     * @param callbacks {@link RangingSession.Callback} to associate with the
     *                  {@link RangingSession} that is being opened.
     *
     * @return an {@link CancellationSignal} that is able to be used to cancel the opening of a
     *         {@link RangingSession} that has been requested through {@link #openRangingSession}
     *         but has not yet been made available by
     *         {@link RangingSession.Callback#onOpened(RangingSession)}.
     */
    @NonNull
    @RequiresPermission(allOf = {
            permission.UWB_PRIVILEGED,
            permission.UWB_RANGING
    })
    public CancellationSignal openRangingSession(@NonNull PersistableBundle parameters,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull RangingSession.Callback callbacks) {
        return openRangingSessionInternal(parameters, executor, callbacks, /* chipId= */ null);
    }

    /**
     * Open a {@link RangingSession} with the given parameters on a specific UWB subsystem
     *
     * @see #openRangingSession(PersistableBundle, Executor, RangingSession.Callback) if you don't
     * need multi-HAL support
     *
     * @param parameters the parameters that define the ranging session
     * @param executor {@link Executor} to run callbacks
     * @param callbacks {@link RangingSession.Callback} to associate with the
     *                  {@link RangingSession} that is being opened.
     * @param chipId identifier of UWB chip for multi-HAL devices
     *
     * @return an {@link CancellationSignal} that is able to be used to cancel the opening of a
     *         {@link RangingSession} that has been requested through {@link #openRangingSession}
     *         but has not yet been made available by
     *         {@link RangingSession.Callback#onOpened(RangingSession)}.
     */
    @NonNull
    @RequiresPermission(allOf = {
            permission.UWB_PRIVILEGED,
            permission.UWB_RANGING
    })
    public CancellationSignal openRangingSession(@NonNull PersistableBundle parameters,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull RangingSession.Callback callbacks,
            @SuppressLint("ListenerLast") @NonNull String chipId) {
        checkNotNull(chipId);
        return openRangingSessionInternal(parameters, executor, callbacks, chipId);
    }

    private CancellationSignal openRangingSessionInternal(PersistableBundle parameters,
            Executor executor, RangingSession.Callback callbacks, String chipId) {
        return mRangingManager.openSession(
                mContext.getAttributionSource(), parameters, executor, callbacks, chipId);
    }

    /**
     * Returns the current enabled/disabled state for UWB.
     *
     * Possible values are:
     * AdapterStateCallback#STATE_DISABLED
     * AdapterStateCallback#STATE_ENABLED_INACTIVE
     * AdapterStateCallback#STATE_ENABLED_ACTIVE
     *
     * @return value representing current enabled/disabled state for UWB.
     */
    public @AdapterStateCallback.State int getAdapterState() {
        return mAdapterStateListener.getAdapterState();
    }

    /**
     * Whether UWB is enabled or disabled.
     *
     * <p>
     * If disabled, this could indicate that either
     * <li> User has toggled UWB off from settings, OR </li>
     * <li> UWB subsystem has shut down due to a fatal error. </li>
     * </p>
     *
     * @return true if enabled, false otherwise.
     *
     * @see #getAdapterState()
     * @see #setUwbEnabled(boolean)
     */
    public boolean isUwbEnabled() {
        int adapterState = getAdapterState();
        return adapterState == AdapterStateCallback.STATE_ENABLED_ACTIVE
                || adapterState == AdapterStateCallback.STATE_ENABLED_INACTIVE;

    }

    /**
     * Disables or enables UWB by the user.
     *
     * If enabled any subsequent calls to
     * {@link #openRangingSession(PersistableBundle, Executor, RangingSession.Callback)} will be
     * allowed. If disabled, all active ranging sessions will be closed and subsequent calls to
     * {@link #openRangingSession(PersistableBundle, Executor, RangingSession.Callback)} will be
     * disallowed.
     *
     * @param enabled value representing intent to disable or enable UWB.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public void setUwbEnabled(boolean enabled) {
        mAdapterStateListener.setEnabled(enabled);
    }


    /**
     * Returns a list of UWB chip infos in a {@link PersistableBundle}.
     *
     * Callers can invoke methods on a specific UWB chip by passing its {@code chipId} to the
     * method, which can be determined by calling:
     * <pre>
     * List<PersistableBundle> chipInfos = getChipInfos();
     * for (PersistableBundle chipInfo : chipInfos) {
     *     String chipId = ChipInfoParams.fromBundle(chipInfo).getChipId();
     * }
     * </pre>
     *
     * @return list of {@link PersistableBundle} containing info about UWB chips for a multi-HAL
     * system, or a list of info for a single chip for a single HAL system.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    @NonNull
    public List<PersistableBundle> getChipInfos() {
        try {
            return mUwbAdapter.getChipInfos();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the default UWB chip identifier.
     *
     * If callers do not pass a specific {@code chipId} to UWB methods, then the method will be
     * invoked on the default chip, which is determined at system initialization from a
     * configuration file.
     *
     * @return default UWB chip identifier for a multi-HAL system, or the identifier of the only UWB
     * chip in a single HAL system.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    @NonNull
    public String getDefaultChipId() {
        try {
            return mUwbAdapter.getDefaultChipId();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Register the UWB service profile.
     * This profile instance is persisted by the platform until explicitly removed
     * using {@link #removeServiceProfile(PersistableBundle)}
     *
     * @param parameters the parameters that define the service profile.
     * @return Protocol specific params to be used as handle for triggering the profile.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    @NonNull
    public PersistableBundle addServiceProfile(@NonNull PersistableBundle parameters) {
        try {
            return mUwbAdapter.addServiceProfile(parameters);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Successfully removed the service profile.
     */
    public static final int REMOVE_SERVICE_PROFILE_SUCCESS = 0;

    /**
     * Failed to remove service since the service profile is unknown.
     */
    public static final int REMOVE_SERVICE_PROFILE_ERROR_UNKNOWN_SERVICE = 1;

    /**
     * Failed to remove service due to some internal error while processing the request.
     */
    public static final int REMOVE_SERVICE_PROFILE_ERROR_INTERNAL = 2;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            REMOVE_SERVICE_PROFILE_SUCCESS,
            REMOVE_SERVICE_PROFILE_ERROR_UNKNOWN_SERVICE,
            REMOVE_SERVICE_PROFILE_ERROR_INTERNAL
    })
    @interface RemoveServiceProfile {}

    /**
     * Remove the service profile registered with {@link #addServiceProfile} and
     * all related resources.
     *
     * @param parameters the parameters that define the service profile.
     *
     * @return true if the service profile is removed, false otherwise.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public @RemoveServiceProfile int removeServiceProfile(@NonNull PersistableBundle parameters) {
        try {
            return mUwbAdapter.removeServiceProfile(parameters);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get all service profiles initialized with {@link #addServiceProfile}
     *
     * @return the parameters that define the service profiles.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    @NonNull
    public PersistableBundle getAllServiceProfiles() {
        try {
            return mUwbAdapter.getAllServiceProfiles();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the list of ADF (application defined file) provisioning authorities available for the UWB
     * applet in SE (secure element).
     *
     * @param serviceProfileBundle Parameters representing the profile to use.
     * @return The list of key information of ADF provisioning authority defined in FiRa
     * CSML 8.2.2.7.2.4 and 8.2.2.14.4.1.2.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    @NonNull
    public PersistableBundle getAdfProvisioningAuthorities(
            @NonNull PersistableBundle serviceProfileBundle) {
        try {
            return mUwbAdapter.getAdfProvisioningAuthorities(serviceProfileBundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get certificate information for the UWB applet in SE (secure element) that can be used to
     * provision ADF (application defined file).
     *
     * @param serviceProfileBundle Parameters representing the profile to use.
     * @return The Fira applet certificate information defined in FiRa CSML 7.3.4.3 and
     * 8.2.2.14.4.1.1
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    @NonNull
    public PersistableBundle getAdfCertificateInfo(
            @NonNull PersistableBundle serviceProfileBundle) {
        try {
            return mUwbAdapter.getAdfCertificateAndInfo(serviceProfileBundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Mechanism to provision ADFs (application defined file) in the UWB applet present in SE
     * (secure element) for a profile instance.
     *
     * @param serviceProfileBundle Parameters representing the profile to use.
     * @param executor an {@link Executor} to execute given callback
     * @param callback user implementation of the {@link AdapterStateCallback}
     */
    public void provisionProfileAdfByScript(@NonNull PersistableBundle serviceProfileBundle,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdfProvisionStateCallback callback) {
        if (executor == null) throw new IllegalArgumentException("executor must not be null");
        if (callback == null) throw new IllegalArgumentException("callback must not be null");
        AdfProvisionStateCallback.AdfProvisionStateCallbackProxy proxy = callback.getProxy();
        proxy.initProxy(executor, callback);
        try {
            mUwbAdapter.provisionProfileAdfByScript(serviceProfileBundle, proxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Successfully removed the profile ADF.
     */
    public static final int REMOVE_PROFILE_ADF_SUCCESS = 0;

    /**
     * Failed to remove ADF since the service profile is unknown.
     */
    public static final int REMOVE_PROFILE_ADF_ERROR_UNKNOWN_SERVICE = 1;

    /**
     * Failed to remove ADF due to some internal error while processing the request.
     */
    public static final int REMOVE_PROFILE_ADF_ERROR_INTERNAL = 2;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            REMOVE_PROFILE_ADF_SUCCESS,
            REMOVE_PROFILE_ADF_ERROR_UNKNOWN_SERVICE,
            REMOVE_PROFILE_ADF_ERROR_INTERNAL
    })
    @interface RemoveProfileAdf {}

    /**
     * Remove the ADF (application defined file) provisioned by {@link #provisionProfileAdfByScript}
     *
     * @param serviceProfileBundle Parameters representing the profile to use.
     * @return true if the ADF is removed, false otherwise.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public @RemoveProfileAdf int removeProfileAdf(@NonNull PersistableBundle serviceProfileBundle) {
        try {
            return mUwbAdapter.removeProfileAdf(serviceProfileBundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Successfully sent the UCI message.
     */
    public static final int SEND_VENDOR_UCI_SUCCESS = 0;

    /**
     * Failed to send the UCI message because of an error returned from the HAL interface.
     */
    public static final int SEND_VENDOR_UCI_ERROR_HW = 1;

    /**
     * Failed to send the UCI message since UWB is toggled off.
     */
    public static final int SEND_VENDOR_UCI_ERROR_OFF = 2;

    /**
     * Failed to send the UCI message since UWB UCI command is malformed.
     * GID.
     */
    public static final int SEND_VENDOR_UCI_ERROR_INVALID_ARGS = 3;

    /**
     * Failed to send the UCI message since UWB GID used is invalid.
     */
    public static final int SEND_VENDOR_UCI_ERROR_INVALID_GID = 4;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            SEND_VENDOR_UCI_SUCCESS,
            SEND_VENDOR_UCI_ERROR_HW,
            SEND_VENDOR_UCI_ERROR_OFF,
            SEND_VENDOR_UCI_ERROR_INVALID_ARGS,
            SEND_VENDOR_UCI_ERROR_INVALID_GID,
    })
    @interface SendVendorUciStatus {}

    /**
     * Message Type for UCI Command.
     */
    public static final int MESSAGE_TYPE_COMMAND = 1;
    /**
     * Message Type for C-APDU (Command - Application Protocol Data Unit),
     * used for communication with secure component.
     */
    public static final int MESSAGE_TYPE_TEST_1 = 4;

    /**
     * Message Type for R-APDU (Response - Application Protocol Data Unit),
     * used for communication with secure component.
     */
    public static final int MESSAGE_TYPE_TEST_2 = 5;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            MESSAGE_TYPE_COMMAND,
            MESSAGE_TYPE_TEST_1,
            MESSAGE_TYPE_TEST_2,
    })
    @interface MessageType {}

    /**
     * Send Vendor specific Uci Messages.
     *
     * The format of the UCI messages are defined in the UCI specification. The platform is
     * responsible for fragmenting the payload if necessary.
     *
     * @param gid Group ID of the command. This needs to be one of the vendor reserved GIDs from
     *            the UCI specification.
     * @param oid Opcode ID of the command. This is left to the OEM / vendor to decide.
     * @param payload containing vendor Uci message payload.
     */
    @NonNull
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public @SendVendorUciStatus int sendVendorUciMessage(
            @IntRange(from = 0, to = 15) int gid, int oid, @NonNull byte[] payload) {
        try {
            return mUwbAdapter.sendVendorUciMessage(MESSAGE_TYPE_COMMAND, gid, oid, payload);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Send Vendor specific Uci Messages with custom message type.
     *
     * The format of the UCI messages are defined in the UCI specification. The platform is
     * responsible for fragmenting the payload if necessary.
     *
     * Note that mt (message type) is added at the beginning of method parameters as it is more
     * distinctive than other parameters and was requested from vendor.
     *
     * @param mt Message Type of the command
     * @param gid Group ID of the command. This needs to be one of the vendor reserved GIDs from
     *            the UCI specification
     * @param oid Opcode ID of the command. This is left to the OEM / vendor to decide
     * @param payload containing vendor Uci message payload
     */
    @NonNull
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public @SendVendorUciStatus int sendVendorUciMessage(@MessageType int mt,
            @IntRange(from = 0, to = 15) int gid, int oid, @NonNull byte[] payload) {
        Objects.requireNonNull(payload, "Payload must not be null");
        try {
            return mUwbAdapter.sendVendorUciMessage(mt, gid, oid, payload);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class OnUwbActivityEnergyInfoProxy
            extends IOnUwbActivityEnergyInfoListener.Stub {
        private final Object mLock = new Object();
        @Nullable @GuardedBy("mLock") private Executor mExecutor;
        @Nullable @GuardedBy("mLock") private Consumer<UwbActivityEnergyInfo> mListener;

        OnUwbActivityEnergyInfoProxy(Executor executor,
                Consumer<UwbActivityEnergyInfo> listener) {
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public void onUwbActivityEnergyInfo(UwbActivityEnergyInfo info) {
            Executor executor;
            Consumer<UwbActivityEnergyInfo> listener;
            synchronized (mLock) {
                if (mExecutor == null || mListener == null) {
                    return;
                }
                executor = mExecutor;
                listener = mListener;
                // null out to allow garbage collection, prevent triggering listener more than once
                mExecutor = null;
                mListener = null;
            }
            Binder.clearCallingIdentity();
            executor.execute(() -> listener.accept(info));
        }
    }

    /**
     * Request to get the current {@link UwbActivityEnergyInfo} asynchronously.
     *
     * @param executor the executor that the listener will be invoked on
     * @param listener the listener that will receive the {@link UwbActivityEnergyInfo} object
     *                 when it becomes available. The listener will be triggered at most once for
     *                 each call to this method.
     */
    @RequiresPermission(permission.UWB_PRIVILEGED)
    public void getUwbActivityEnergyInfoAsync(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<UwbActivityEnergyInfo> listener) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        try {
            mUwbAdapter.getUwbActivityEnergyInfoAsync(
                    new OnUwbActivityEnergyInfoProxy(executor, listener));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
