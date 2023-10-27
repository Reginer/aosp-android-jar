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

package android.devicelock;

import android.Manifest.permission;
import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresFeature;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Manager used to interact with the system device lock service.
 * The device lock feature is used by special applications ('kiosk apps', downloaded and installed
 * by the device lock solution) to lock and unlock a device.
 * A typical use case is a financed device, where the financing entity has the capability to lock
 * the device in case of a missed payment.
 * When a device is locked, only a limited set of interactions with the device is allowed (for
 * example, placing emergency calls).
 * <p>
 * Use {@link android.content.Context#getSystemService(java.lang.String)}
 * with {@link Context#DEVICE_LOCK_SERVICE} to create a {@link DeviceLockManager}.
 * </p>
 *
 */
@SystemService(Context.DEVICE_LOCK_SERVICE)
@RequiresFeature(PackageManager.FEATURE_DEVICE_LOCK)
public final class DeviceLockManager {
    private static final String TAG = "DeviceLockManager";
    private final IDeviceLockService mService;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "DEVICE_LOCK_ROLE_", value = {
        DEVICE_LOCK_ROLE_FINANCING,
    })
    public @interface DeviceLockRole {}

    /**
     * Constant representing a financed device role, returned by {@link #getKioskApps}.
     */
    public static final int DEVICE_LOCK_ROLE_FINANCING = 0;

    private static SecurityException getSecurityException() {
        return new SecurityException("Need MANAGE_DEVICE_LOCK_STATE permission");
    }

    private static Exception getUnknwnException() {
        return new Exception("Unknown error");
    }

    private static final Map<Class, Map<Integer, Exception>> ERROR_MAP = Map.of(
            ILockUnlockDeviceCallback.class, Map.of(
                    ILockUnlockDeviceCallback.ERROR_UNKNOWN, getUnknwnException(),
                    ILockUnlockDeviceCallback.ERROR_SECURITY, getSecurityException()
            ),
            IIsDeviceLockedCallback.class, Map.of(
                    IIsDeviceLockedCallback.ERROR_UNKNOWN, getUnknwnException(),
                    IIsDeviceLockedCallback.ERROR_SECURITY, getSecurityException()
            ),
            IGetDeviceIdCallback.class, Map.of(
                    IGetDeviceIdCallback.ERROR_UNKNOWN, getUnknwnException(),
                    IGetDeviceIdCallback.ERROR_SECURITY, getSecurityException(),
                    IGetDeviceIdCallback.ERROR_INVALID_DEVICE_ID_TYPE_BITMAP,
                        new Exception("Invalid device type"),
                    IGetDeviceIdCallback.ERROR_CANNOT_GET_DEVICE_ID,
                        new Exception("Unable to get device id")
            ),
            IGetKioskAppsCallback.class, Map.of(
                    IGetKioskAppsCallback.ERROR_UNKNOWN, getUnknwnException()
            )
    );

    private static Exception getException(Class clazz, int error) {
        final Map<Integer, Exception> map = ERROR_MAP.get(clazz);
        if (map == null) {
            Log.e(TAG, "Cannot find error map for: " + clazz.getSimpleName());

            return new Exception();
        }

        final Exception ex = map.get(error);
        if (ex == null) {
            return new Exception();
        }

        return ex;
    }

    /**
     * @hide
     */
    public DeviceLockManager(Context context, IDeviceLockService service) {
        mService = service;
    }

    /**
     * Lock the device.
     *
     * @param executor the {@link Executor} on which to invoke the callback.
     * @param callback this returns either success or an exception.
     */
    @RequiresPermission(permission.MANAGE_DEVICE_LOCK_STATE)
    public void lockDevice(@NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, Exception> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.lockDevice(
                    new ILockUnlockDeviceCallback.Stub() {
                        @Override
                        public void onDeviceLockedUnlocked() {
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(int error) {
                            callback.onError(getException(ILockUnlockDeviceCallback.class,
                                    error));
                        }
                    });
        } catch (RemoteException e) {
            executor.execute(() -> callback.onError(new RuntimeException(e)));
        }
    }

    /**
     * Unlock the device.
     *
     * @param executor the {@link Executor} on which to invoke the callback.
     * @param callback this returns either success or an exception.
     */
    @RequiresPermission(permission.MANAGE_DEVICE_LOCK_STATE)
    public void unlockDevice(@NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, Exception> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.unlockDevice(
                    new ILockUnlockDeviceCallback.Stub() {
                        @Override
                        public void onDeviceLockedUnlocked() {
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(int error) {
                            callback.onError(getException(ILockUnlockDeviceCallback.class,
                                    error));
                        }
                    });
        } catch (RemoteException e) {
            executor.execute(() -> callback.onError(new RuntimeException(e)));
        }
    }

    /**
     * Check if the device is locked or not.
     *
     * @param executor the {@link Executor} on which to invoke the callback.
     * @param callback this returns either the lock status or an exception.
     */
    @RequiresPermission(permission.MANAGE_DEVICE_LOCK_STATE)
    public void isDeviceLocked(@NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Boolean, Exception> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.isDeviceLocked(
                    new IIsDeviceLockedCallback.Stub() {
                        @Override
                        public void onIsDeviceLocked(boolean locked) {
                            executor.execute(() -> callback.onResult(locked));
                        }

                        @Override
                        public void onError(int error) {
                            executor.execute(() ->
                                    callback.onError(getException(IIsDeviceLockedCallback.class,
                                            error)));
                        }
                    });
        } catch (RemoteException e) {
            executor.execute(() -> callback.onError(new RuntimeException(e)));
        }
    }

    /**
     * Get the device id.
     *
     * @param executor the {@link Executor} on which to invoke the callback.
     * @param callback this returns either the {@link DeviceId} or an exception.
     */
    @RequiresPermission(permission.MANAGE_DEVICE_LOCK_STATE)
    public void getDeviceId(@NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<DeviceId, Exception> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getDeviceId(
                    new IGetDeviceIdCallback.Stub() {
                        @Override
                        public void onDeviceIdReceived(int type, String id) {
                            if (TextUtils.isEmpty(id)) {
                                executor.execute(() -> {
                                    callback.onError(new Exception("Cannot get device id (empty)"));
                                });
                            } else {
                                executor.execute(() -> {
                                    callback.onResult(new DeviceId(type, id));
                                });
                            }
                        }

                        @Override
                        public void onError(int error) {
                            callback.onError(getException(IGetDeviceIdCallback.class,
                                    error));
                        }
                    }
            );
        } catch (RemoteException e) {
            executor.execute(() -> callback.onError(new RuntimeException(e)));
        }
    }

    /**
     * Get the kiosk app roles and packages.
     *
     * @param executor the {@link Executor} on which to invoke the callback.
     * @param callback this returns either a {@link Map} of device roles/package names,
     *                 or an exception. The Integer in the map represent the device lock role
     *                 (at this moment, the only supported role is
     *                 {@value #DEVICE_LOCK_ROLE_FINANCING}. The String represents tha package
     *                 name of the kiosk app for that role.
     */
    @RequiresNoPermission
    public void getKioskApps(@NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Map<Integer, String>, Exception> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getKioskApps(
                    new IGetKioskAppsCallback.Stub() {
                        @Override
                        public void onKioskAppsReceived(Map kioskApps) {
                            executor.execute(() -> callback.onResult(kioskApps));
                        }

                        @Override
                        public void onError(int error) {
                            callback.onError(getException(IGetKioskAppsCallback.class,
                                    error));
                        }
                    }
            );
        } catch (RemoteException e) {
            executor.execute(() -> callback.onError(new RuntimeException(e)));
        }
    }
}
