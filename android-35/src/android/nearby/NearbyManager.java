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

package android.nearby;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.LocationManager;
import android.nearby.aidl.IOffloadCallback;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * This class provides a way to perform Nearby related operations such as scanning, broadcasting
 * and connecting to nearby devices.
 *
 * <p> To get a {@link NearbyManager} instance, call the
 * <code>Context.getSystemService(NearbyManager.class)</code>.
 *
 * @hide
 */
@SystemApi
@SystemService(Context.NEARBY_SERVICE)
public class NearbyManager {

    /**
     * Represents the scanning state.
     *
     * @hide
     */
    @IntDef({
            ScanStatus.UNKNOWN,
            ScanStatus.SUCCESS,
            ScanStatus.ERROR,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanStatus {
        // The undetermined status, some modules may be initializing. Retry is suggested.
        int UNKNOWN = 0;
        // The successful state.
        int SUCCESS = 1;
        // Failed state.
        int ERROR = 2;
    }

    /**
     * Return value of {@link #getPoweredOffFindingMode()} when this powered off finding is not
     * supported the device.
     */
    @FlaggedApi("com.android.nearby.flags.powered_off_finding")
    public static final int POWERED_OFF_FINDING_MODE_UNSUPPORTED = 0;

    /**
     * Return value of {@link #getPoweredOffFindingMode()} and argument of {@link
     * #setPoweredOffFindingMode(int)} when powered off finding is supported but disabled. The
     * device will not start to advertise when powered off.
     */
    @FlaggedApi("com.android.nearby.flags.powered_off_finding")
    public static final int POWERED_OFF_FINDING_MODE_DISABLED = 1;

    /**
     * Return value of {@link #getPoweredOffFindingMode()} and argument of {@link
     * #setPoweredOffFindingMode(int)} when powered off finding is enabled. The device will start to
     * advertise when powered off.
     */
    @FlaggedApi("com.android.nearby.flags.powered_off_finding")
    public static final int POWERED_OFF_FINDING_MODE_ENABLED = 2;

    /**
     * Powered off finding modes.
     *
     * @hide
     */
    @IntDef(
            prefix = {"POWERED_OFF_FINDING_MODE"},
            value = {
                    POWERED_OFF_FINDING_MODE_UNSUPPORTED,
                    POWERED_OFF_FINDING_MODE_DISABLED,
                    POWERED_OFF_FINDING_MODE_ENABLED,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PoweredOffFindingMode {}

    private static final String TAG = "NearbyManager";

    private static final int POWERED_OFF_FINDING_EID_LENGTH = 20;

    private static final String POWER_OFF_FINDING_SUPPORTED_PROPERTY =
            "ro.bluetooth.finder.supported";

    /**
     * TODO(b/286137024): Remove this when CTS R5 is rolled out.
     * Whether allows Fast Pair to scan.
     *
     * (0 = disabled, 1 = enabled)
     *
     * @hide
     */
    public static final String FAST_PAIR_SCAN_ENABLED = "fast_pair_scan_enabled";

    @GuardedBy("sScanListeners")
    private static final WeakHashMap<ScanCallback, WeakReference<ScanListenerTransport>>
            sScanListeners = new WeakHashMap<>();
    @GuardedBy("sBroadcastListeners")
    private static final WeakHashMap<BroadcastCallback, WeakReference<BroadcastListenerTransport>>
            sBroadcastListeners = new WeakHashMap<>();

    private final Context mContext;
    private final INearbyManager mService;

    /**
     * Creates a new NearbyManager.
     *
     * @param service the service object
     */
    NearbyManager(@NonNull Context context, @NonNull INearbyManager service) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(service);
        mContext = context;
        mService = service;
    }

    // This can be null when NearbyDeviceParcelable field not set for Presence device
    // or the scan type is not recognized.
    @Nullable
    private static NearbyDevice toClientNearbyDevice(
            NearbyDeviceParcelable nearbyDeviceParcelable,
            @ScanRequest.ScanType int scanType) {
        if (scanType == ScanRequest.SCAN_TYPE_FAST_PAIR) {
            return new FastPairDevice.Builder()
                    .setName(nearbyDeviceParcelable.getName())
                    .addMedium(nearbyDeviceParcelable.getMedium())
                    .setRssi(nearbyDeviceParcelable.getRssi())
                    .setTxPower(nearbyDeviceParcelable.getTxPower())
                    .setModelId(nearbyDeviceParcelable.getFastPairModelId())
                    .setBluetoothAddress(nearbyDeviceParcelable.getBluetoothAddress())
                    .setData(nearbyDeviceParcelable.getData()).build();
        }

        if (scanType == ScanRequest.SCAN_TYPE_NEARBY_PRESENCE) {
            PresenceDevice presenceDevice = nearbyDeviceParcelable.getPresenceDevice();
            if (presenceDevice == null) {
                Log.e(TAG,
                        "Cannot find any Presence device in discovered NearbyDeviceParcelable");
            }
            return presenceDevice;
        }
        return null;
    }

    /**
     * Start scan for nearby devices with given parameters. Devices matching {@link ScanRequest}
     * will be delivered through the given callback.
     *
     * @param scanRequest various parameters clients send when requesting scanning
     * @param executor executor where the listener method is called
     * @param scanCallback the callback to notify clients when there is a scan result
     *
     * @return whether scanning was successfully started
     */
    @RequiresPermission(allOf = {android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED})
    @ScanStatus
    public int startScan(@NonNull ScanRequest scanRequest,
            @CallbackExecutor @NonNull Executor executor,
            @NonNull ScanCallback scanCallback) {
        Objects.requireNonNull(scanRequest, "scanRequest must not be null");
        Objects.requireNonNull(scanCallback, "scanCallback must not be null");
        Objects.requireNonNull(executor, "executor must not be null");

        try {
            synchronized (sScanListeners) {
                WeakReference<ScanListenerTransport> reference = sScanListeners.get(scanCallback);
                ScanListenerTransport transport = reference != null ? reference.get() : null;
                if (transport == null) {
                    transport = new ScanListenerTransport(scanRequest.getScanType(), scanCallback,
                            executor);
                } else {
                    Preconditions.checkState(transport.isRegistered());
                    transport.setExecutor(executor);
                }
                @ScanStatus int status = mService.registerScanListener(scanRequest, transport,
                        mContext.getPackageName(), mContext.getAttributionTag());
                if (status != ScanStatus.SUCCESS) {
                    return status;
                }
                sScanListeners.put(scanCallback, new WeakReference<>(transport));
                return ScanStatus.SUCCESS;
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Stops the nearby device scan for the specified callback. The given callback
     * is guaranteed not to receive any invocations that happen after this method
     * is invoked.
     *
     * Suppressed lint: Registration methods should have overload that accepts delivery Executor.
     * Already have executor in startScan() method.
     *
     * @param scanCallback the callback that was used to start the scan
     */
    @SuppressLint("ExecutorRegistration")
    @RequiresPermission(allOf = {android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED})
    public void stopScan(@NonNull ScanCallback scanCallback) {
        Preconditions.checkArgument(scanCallback != null,
                "invalid null scanCallback");
        try {
            synchronized (sScanListeners) {
                WeakReference<ScanListenerTransport> reference = sScanListeners.remove(
                        scanCallback);
                ScanListenerTransport transport = reference != null ? reference.get() : null;
                if (transport != null) {
                    transport.unregister();
                    mService.unregisterScanListener(transport, mContext.getPackageName(),
                            mContext.getAttributionTag());
                } else {
                    Log.e(TAG, "Cannot stop scan with this callback "
                            + "because it is never registered.");
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Start broadcasting the request using nearby specification.
     *
     * @param broadcastRequest request for the nearby broadcast
     * @param executor executor for running the callback
     * @param callback callback for notifying the client
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED})
    public void startBroadcast(@NonNull BroadcastRequest broadcastRequest,
            @CallbackExecutor @NonNull Executor executor, @NonNull BroadcastCallback callback) {
        try {
            synchronized (sBroadcastListeners) {
                WeakReference<BroadcastListenerTransport> reference = sBroadcastListeners.get(
                        callback);
                BroadcastListenerTransport transport = reference != null ? reference.get() : null;
                if (transport == null) {
                    transport = new BroadcastListenerTransport(callback, executor);
                } else {
                    Preconditions.checkState(transport.isRegistered());
                    transport.setExecutor(executor);
                }
                mService.startBroadcast(new BroadcastRequestParcelable(broadcastRequest), transport,
                        mContext.getPackageName(), mContext.getAttributionTag());
                sBroadcastListeners.put(callback, new WeakReference<>(transport));
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Stop the broadcast associated with the given callback.
     *
     * @param callback the callback that was used for starting the broadcast
     */
    @SuppressLint("ExecutorRegistration")
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED})
    public void stopBroadcast(@NonNull BroadcastCallback callback) {
        try {
            synchronized (sBroadcastListeners) {
                WeakReference<BroadcastListenerTransport> reference = sBroadcastListeners.remove(
                        callback);
                BroadcastListenerTransport transport = reference != null ? reference.get() : null;
                if (transport != null) {
                    transport.unregister();
                    mService.stopBroadcast(transport, mContext.getPackageName(),
                            mContext.getAttributionTag());
                } else {
                    Log.e(TAG, "Cannot stop broadcast with this callback "
                            + "because it is never registered.");
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Query offload capability in a device. The query is asynchronous and result is called back
     * in {@link Consumer}, which is set to true if offload is supported.
     *
     * @param executor the callback will take place on this {@link Executor}
     * @param callback the callback invoked with {@link OffloadCapability}
     */
    public void queryOffloadCapability(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<OffloadCapability> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.queryOffloadCapability(new OffloadTransport(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class OffloadTransport extends IOffloadCallback.Stub {

        private final Executor mExecutor;
        // Null when cancelled
        volatile @Nullable Consumer<OffloadCapability> mConsumer;

        OffloadTransport(Executor executor, Consumer<OffloadCapability> consumer) {
            Preconditions.checkArgument(executor != null, "illegal null executor");
            Preconditions.checkArgument(consumer != null, "illegal null consumer");
            mExecutor = executor;
            mConsumer = consumer;
        }

        @Override
        public void onQueryComplete(OffloadCapability capability) {
            mExecutor.execute(() -> {
                if (mConsumer != null) {
                    mConsumer.accept(capability);
                }
            });
        }
    }

    private static class ScanListenerTransport extends IScanListener.Stub {

        private @ScanRequest.ScanType int mScanType;
        private volatile @Nullable ScanCallback mScanCallback;
        private Executor mExecutor;

        ScanListenerTransport(@ScanRequest.ScanType int scanType, ScanCallback scanCallback,
                @CallbackExecutor Executor executor) {
            Preconditions.checkArgument(scanCallback != null,
                    "invalid null callback");
            Preconditions.checkState(ScanRequest.isValidScanType(scanType),
                    "invalid scan type : " + scanType
                            + ", scan type must be one of ScanRequest#SCAN_TYPE_");
            mScanType = scanType;
            mScanCallback = scanCallback;
            mExecutor = executor;
        }

        void setExecutor(Executor executor) {
            Preconditions.checkArgument(
                    executor != null, "invalid null executor");
            mExecutor = executor;
        }

        boolean isRegistered() {
            return mScanCallback != null;
        }

        void unregister() {
            mScanCallback = null;
        }

        @Override
        public void onDiscovered(NearbyDeviceParcelable nearbyDeviceParcelable)
                throws RemoteException {
            mExecutor.execute(() -> {
                NearbyDevice nearbyDevice = toClientNearbyDevice(nearbyDeviceParcelable, mScanType);
                if (mScanCallback != null && nearbyDevice != null) {
                    mScanCallback.onDiscovered(nearbyDevice);
                }
            });
        }

        @Override
        public void onUpdated(NearbyDeviceParcelable nearbyDeviceParcelable)
                throws RemoteException {
            mExecutor.execute(() -> {
                NearbyDevice nearbyDevice = toClientNearbyDevice(nearbyDeviceParcelable, mScanType);
                if (mScanCallback != null && nearbyDevice != null) {
                    mScanCallback.onUpdated(
                            toClientNearbyDevice(nearbyDeviceParcelable, mScanType));
                }
            });
        }

        @Override
        public void onLost(NearbyDeviceParcelable nearbyDeviceParcelable) throws RemoteException {
            mExecutor.execute(() -> {
                NearbyDevice nearbyDevice = toClientNearbyDevice(nearbyDeviceParcelable, mScanType);
                if (mScanCallback != null && nearbyDevice != null) {
                    mScanCallback.onLost(
                            toClientNearbyDevice(nearbyDeviceParcelable, mScanType));
                }
            });
        }

        @Override
        public void onError(int errorCode) {
            mExecutor.execute(() -> {
                if (mScanCallback != null) {
                    mScanCallback.onError(errorCode);
                }
            });
        }
    }

    private static class BroadcastListenerTransport extends IBroadcastListener.Stub {
        private volatile @Nullable BroadcastCallback mBroadcastCallback;
        private Executor mExecutor;

        BroadcastListenerTransport(BroadcastCallback broadcastCallback,
                @CallbackExecutor Executor executor) {
            mBroadcastCallback = broadcastCallback;
            mExecutor = executor;
        }

        void setExecutor(Executor executor) {
            Preconditions.checkArgument(
                    executor != null, "invalid null executor");
            mExecutor = executor;
        }

        boolean isRegistered() {
            return mBroadcastCallback != null;
        }

        void unregister() {
            mBroadcastCallback = null;
        }

        @Override
        public void onStatusChanged(int status) {
            mExecutor.execute(() -> {
                if (mBroadcastCallback != null) {
                    mBroadcastCallback.onStatusChanged(status);
                }
            });
        }
    }

    /**
     * TODO(b/286137024): Remove this when CTS R5 is rolled out.
     * Read from {@link Settings} whether Fast Pair scan is enabled.
     *
     * @param context the {@link Context} to query the setting
     * @return whether the Fast Pair is enabled
     * @hide
     */
    public static boolean getFastPairScanEnabled(@NonNull Context context) {
        final int enabled = Settings.Secure.getInt(
                context.getContentResolver(), FAST_PAIR_SCAN_ENABLED, 0);
        return enabled != 0;
    }

    /**
     * TODO(b/286137024): Remove this when CTS R5 is rolled out.
     * Write into {@link Settings} whether Fast Pair scan is enabled
     *
     * @param context the {@link Context} to set the setting
     * @param enable whether the Fast Pair scan should be enabled
     * @hide
     */
    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    public static void setFastPairScanEnabled(@NonNull Context context, boolean enable) {
        Settings.Secure.putInt(
                context.getContentResolver(), FAST_PAIR_SCAN_ENABLED, enable ? 1 : 0);
        Log.v(TAG, String.format(
                "successfully %s Fast Pair scan", enable ? "enables" : "disables"));
    }

    /**
     * Sets the precomputed EIDs for advertising when the phone is powered off. The Bluetooth
     * controller will store these EIDs in its memory, and will start advertising them in Find My
     * Device network EID frames when powered off, only if the powered off finding mode was
     * previously enabled by calling {@link #setPoweredOffFindingMode(int)}.
     *
     * <p>The EIDs are cryptographic ephemeral identifiers that change periodically, based on the
     * Android clock at the time of the shutdown. They are used as the public part of asymmetric key
     * pairs. Members of the Find My Device network can use them to encrypt the location of where
     * they sight the advertising device. Only someone in possession of the private key (the device
     * owner or someone that the device owner shared the key with) can decrypt this encrypted
     * location.
     *
     * <p>Android will typically call this method during the shutdown process. Even after the
     * method was called, it is still possible to call {#link setPoweredOffFindingMode() to disable
     * the advertisement, for example to temporarily disable it for a single shutdown.
     *
     * <p>If called more than once, the EIDs of the most recent call overrides the EIDs from any
     * previous call.
     *
     * @throws IllegalArgumentException if the length of one of the EIDs is not 20 bytes
     */
    @FlaggedApi("com.android.nearby.flags.powered_off_finding")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public void setPoweredOffFindingEphemeralIds(@NonNull List<byte[]> eids) {
        Objects.requireNonNull(eids);
        if (!isPoweredOffFindingSupported()) {
            throw new UnsupportedOperationException(
                    "Powered off finding is not supported on this device");
        }
        List<PoweredOffFindingEphemeralId> ephemeralIdList = eids.stream().map(
                eid -> {
                    Preconditions.checkArgument(eid.length == POWERED_OFF_FINDING_EID_LENGTH);
                    PoweredOffFindingEphemeralId ephemeralId = new PoweredOffFindingEphemeralId();
                    ephemeralId.bytes = eid;
                    return ephemeralId;
                }).toList();
        try {
            mService.setPoweredOffFindingEphemeralIds(ephemeralIdList);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

    }

    /**
     * Turns the powered off finding on or off. Power off finding will operate only if this method
     * was called at least once since boot, and the value of the argument {@code
     * poweredOffFindinMode} was {@link #POWERED_OFF_FINDING_MODE_ENABLED} the last time the method
     * was called.
     *
     * <p>When an Android device with the powered off finding feature is turned off (either as part
     * of a normal shutdown or due to dead battery), its Bluetooth chip starts to advertise Find My
     * Device network EID frames with the EID payload that were provided by the last call to {@link
     * #setPoweredOffFindingEphemeralIds(List)}. These EIDs can be sighted by other Android devices
     * in BLE range that are part of the Find My Device network. The Android sighters use the EID to
     * encrypt the location of the Android device and upload it to the server, in a way that only
     * the owner of the advertising device, or people that the owner shared their encryption key
     * with, can decrypt the location.
     *
     * @param poweredOffFindingMode {@link #POWERED_OFF_FINDING_MODE_ENABLED} or {@link
     * #POWERED_OFF_FINDING_MODE_DISABLED}
     *
     * @throws IllegalStateException if called with {@link #POWERED_OFF_FINDING_MODE_ENABLED} when
     * Bluetooth or location services are disabled
     */
    @FlaggedApi("com.android.nearby.flags.powered_off_finding")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public void setPoweredOffFindingMode(@PoweredOffFindingMode int poweredOffFindingMode) {
        Preconditions.checkArgument(
                poweredOffFindingMode == POWERED_OFF_FINDING_MODE_ENABLED
                        || poweredOffFindingMode == POWERED_OFF_FINDING_MODE_DISABLED,
                "invalid poweredOffFindingMode");
        if (!isPoweredOffFindingSupported()) {
            throw new UnsupportedOperationException(
                    "Powered off finding is not supported on this device");
        }
        if (poweredOffFindingMode == POWERED_OFF_FINDING_MODE_ENABLED) {
            Preconditions.checkState(areLocationAndBluetoothEnabled(),
                    "Location services and Bluetooth must be on");
        }
        try {
            mService.setPoweredOffModeEnabled(
                    poweredOffFindingMode == POWERED_OFF_FINDING_MODE_ENABLED);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the state of the powered off finding feature.
     *
     * <p>{@link #POWERED_OFF_FINDING_MODE_UNSUPPORTED} if the feature is not supported by the
     * device, {@link #POWERED_OFF_FINDING_MODE_DISABLED} if this was the last value set by {@link
     * #setPoweredOffFindingMode(int)} or if no value was set since boot, {@link
     * #POWERED_OFF_FINDING_MODE_ENABLED} if this was the last value set by {@link
     * #setPoweredOffFindingMode(int)}
     */
    @FlaggedApi("com.android.nearby.flags.powered_off_finding")
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public @PoweredOffFindingMode int getPoweredOffFindingMode() {
        if (!isPoweredOffFindingSupported()) {
            return POWERED_OFF_FINDING_MODE_UNSUPPORTED;
        }
        try {
            return mService.getPoweredOffModeEnabled()
                    ? POWERED_OFF_FINDING_MODE_ENABLED : POWERED_OFF_FINDING_MODE_DISABLED;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean isPoweredOffFindingSupported() {
        return Boolean.parseBoolean(SystemProperties.get(POWER_OFF_FINDING_SUPPORTED_PROPERTY));
    }

    private boolean areLocationAndBluetoothEnabled() {
        return mContext.getSystemService(BluetoothManager.class).getAdapter().isEnabled()
                && mContext.getSystemService(LocationManager.class).isLocationEnabled();
    }
}
