/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.bluetooth.le;

import static android.Manifest.permission.BLUETOOTH_ADVERTISE;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothAdvertise;
import android.bluetooth.annotations.RequiresBluetoothAdvertisePermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothAdminPermission;
import android.content.AttributionSource;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a way to perform Bluetooth LE advertise operations, such as starting and
 * stopping advertising. An advertiser can broadcast up to 31 bytes of advertisement data
 * represented by {@link AdvertiseData}.
 *
 * <p>To get an instance of {@link BluetoothLeAdvertiser}, call the {@link
 * BluetoothAdapter#getBluetoothLeAdvertiser()} method.
 *
 * @see AdvertiseData
 */
public final class BluetoothLeAdvertiser {
    private static final String TAG = "BluetoothLeAdvertiser";

    private static final int MAX_LEGACY_ADVERTISING_DATA_BYTES = 31;
    // Each fields need one byte for field length and another byte for field type.
    private static final int OVERHEAD_BYTES_PER_FIELD = 2;
    // Flags field will be set by system.
    private static final int FLAGS_FIELD_BYTES = 3;
    private static final int MANUFACTURER_SPECIFIC_DATA_LENGTH = 2;

    private final BluetoothAdapter mBluetoothAdapter;
    private final AttributionSource mAttributionSource;

    private final Handler mHandler;
    private final Map<AdvertiseCallback, AdvertisingSetCallback> mLegacyAdvertisers =
            new HashMap<>();
    private final Map<AdvertisingSetCallback, IAdvertisingSetCallback> mCallbackWrappers =
            Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, AdvertisingSet> mAdvertisingSets =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * Use BluetoothAdapter.getLeAdvertiser() instead.
     *
     * @hide
     */
    public BluetoothLeAdvertiser(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = requireNonNull(bluetoothAdapter);
        mAttributionSource = mBluetoothAdapter.getAttributionSource();
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Start Bluetooth LE Advertising. On success, the {@code advertiseData} will be broadcasted.
     * Returns immediately, the operation status is delivered through {@code callback}.
     *
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission only when
     * {@code settings.getOwnAddressType()} is different from {@code
     * AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT}.
     *
     * <p>The {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} permission is always enforced.
     *
     * @param settings Settings for Bluetooth LE advertising.
     * @param advertiseData Advertisement data to be broadcasted.
     * @param callback Callback for advertising status.
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(
            allOf = {BLUETOOTH_ADVERTISE, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public void startAdvertising(
            AdvertiseSettings settings,
            AdvertiseData advertiseData,
            final AdvertiseCallback callback) {
        startAdvertising(settings, advertiseData, null, callback);
    }

    /**
     * Start Bluetooth LE Advertising. The {@code advertiseData} will be broadcasted if the
     * operation succeeds. The {@code scanResponse} is returned when a scanning device sends an
     * active scan request. This method returns immediately, the operation status is delivered
     * through {@code callback}.
     *
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission only when
     * {@code settings.getOwnAddressType()} is different from {@code
     * AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT}.
     *
     * <p>The {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} permission is always enforced.
     *
     * @param settings Settings for Bluetooth LE advertising.
     * @param advertiseData Advertisement data to be advertised in advertisement packet.
     * @param scanResponse Scan response associated with the advertisement data.
     * @param callback Callback for advertising status.
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(
            allOf = {BLUETOOTH_ADVERTISE, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public void startAdvertising(
            AdvertiseSettings settings,
            AdvertiseData advertiseData,
            AdvertiseData scanResponse,
            final AdvertiseCallback callback) {
        synchronized (mLegacyAdvertisers) {
            BluetoothLeUtils.checkAdapterStateOn(mBluetoothAdapter);
            if (callback == null) {
                throw new IllegalArgumentException("callback cannot be null");
            }
            boolean isConnectable = settings.isConnectable();
            boolean isDiscoverable = settings.isDiscoverable();
            boolean hasFlags = isConnectable && isDiscoverable;
            if (totalBytes(advertiseData, hasFlags) > MAX_LEGACY_ADVERTISING_DATA_BYTES
                    || totalBytes(scanResponse, false) > MAX_LEGACY_ADVERTISING_DATA_BYTES) {
                postStartFailure(callback, AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE);
                return;
            }
            if (mLegacyAdvertisers.containsKey(callback)) {
                postStartFailure(callback, AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED);
                return;
            }

            AdvertisingSetParameters.Builder parameters = new AdvertisingSetParameters.Builder();
            parameters.setLegacyMode(true);
            parameters.setConnectable(isConnectable);
            parameters.setDiscoverable(isDiscoverable);
            parameters.setScannable(true); // legacy advertisements we support are always scannable
            parameters.setOwnAddressType(settings.getOwnAddressType());
            if (settings.getMode() == AdvertiseSettings.ADVERTISE_MODE_LOW_POWER) {
                parameters.setInterval(1600); // 1s
            } else if (settings.getMode() == AdvertiseSettings.ADVERTISE_MODE_BALANCED) {
                parameters.setInterval(400); // 250ms
            } else if (settings.getMode() == AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) {
                parameters.setInterval(160); // 100ms
            }

            if (settings.getTxPowerLevel() == AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW) {
                Log.d(TAG, "TxPower == ADVERTISE_TX_POWER_ULTRA_LOW");
                parameters.setTxPowerLevel(-21);
            } else if (settings.getTxPowerLevel() == AdvertiseSettings.ADVERTISE_TX_POWER_LOW) {
                Log.d(TAG, "TxPower == ADVERTISE_TX_POWER_LOW");
                parameters.setTxPowerLevel(-15);
            } else if (settings.getTxPowerLevel() == AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM) {
                Log.d(TAG, "TxPower == ADVERTISE_TX_POWER_MEDIUM");
                parameters.setTxPowerLevel(-7);
            } else if (settings.getTxPowerLevel() == AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) {
                Log.d(TAG, "TxPower == ADVERTISE_TX_POWER_HIGH");
                parameters.setTxPowerLevel(1);
            }

            int duration = 0;
            int timeoutMillis = settings.getTimeout();
            if (timeoutMillis > 0) {
                duration = (timeoutMillis < 10) ? 1 : timeoutMillis / 10;
            }

            AdvertisingSetCallback wrapped = wrapOldCallback(callback, settings);
            mLegacyAdvertisers.put(callback, wrapped);
            startAdvertisingSet(
                    parameters.build(),
                    advertiseData,
                    scanResponse,
                    null,
                    null,
                    duration,
                    0,
                    wrapped);
        }
    }

    @SuppressLint({
        "AndroidFrameworkBluetoothPermission",
        "AndroidFrameworkRequiresPermission",
    })
    AdvertisingSetCallback wrapOldCallback(AdvertiseCallback callback, AdvertiseSettings settings) {
        return new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(
                    AdvertisingSet advertisingSet, int txPower, int status) {
                if (status != AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                    postStartFailure(callback, status);
                    return;
                }

                postStartSuccess(callback, settings);
            }

            /* Legacy advertiser is disabled on timeout */
            @Override
            public void onAdvertisingEnabled(
                    AdvertisingSet advertisingSet, boolean enabled, int status) {
                if (enabled) {
                    Log.e(
                            TAG,
                            "Legacy advertiser should be only disabled on timeout,"
                                    + " but was enabled!");
                    return;
                }

                stopAdvertising(callback);
            }
        };
    }

    /**
     * Stop Bluetooth LE advertising. The {@code callback} must be the same one use in {@link
     * BluetoothLeAdvertiser#startAdvertising}.
     *
     * @param callback {@link AdvertiseCallback} identifies the advertising instance to stop.
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(BLUETOOTH_ADVERTISE)
    public void stopAdvertising(final AdvertiseCallback callback) {
        synchronized (mLegacyAdvertisers) {
            if (callback == null) {
                throw new IllegalArgumentException("callback cannot be null");
            }
            AdvertisingSetCallback wrapper = mLegacyAdvertisers.get(callback);
            if (wrapper == null) return;

            stopAdvertisingSet(wrapper);

            mLegacyAdvertisers.remove(callback);
        }
    }

    /**
     * Creates a new advertising set. If operation succeed, device will start advertising. This
     * method returns immediately, the operation status is delivered through {@code
     * callback.onAdvertisingSetStarted()}.
     *
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission when
     * {@code parameters.getOwnAddressType()} is different from {@code
     * AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT} or {@code parameters.isDirected()} is true.
     *
     * <p>The {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} permission is always enforced.
     *
     * @param parameters advertising set parameters.
     * @param advertiseData Advertisement data to be broadcasted. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}. If the advertisement is connectable,
     *     three bytes will be added for flags.
     * @param scanResponse Scan response associated with the advertisement data. Size must not
     *     exceed {@link BluetoothAdapter#getLeMaximumAdvertisingDataLength}.
     * @param periodicParameters periodic advertisng parameters. If null, periodic advertising will
     *     not be started.
     * @param periodicData Periodic advertising data. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}.
     * @param callback Callback for advertising set.
     * @throws IllegalArgumentException when any of the data parameter exceed the maximum allowable
     *     size, or unsupported advertising PHY is selected, or when attempt to use Periodic
     *     Advertising feature is made when it's not supported by the controller.
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(
            allOf = {BLUETOOTH_ADVERTISE, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public void startAdvertisingSet(
            AdvertisingSetParameters parameters,
            AdvertiseData advertiseData,
            AdvertiseData scanResponse,
            PeriodicAdvertisingParameters periodicParameters,
            AdvertiseData periodicData,
            AdvertisingSetCallback callback) {
        startAdvertisingSet(
                parameters,
                advertiseData,
                scanResponse,
                periodicParameters,
                periodicData,
                0,
                0,
                callback,
                new Handler(Looper.getMainLooper()));
    }

    /**
     * Creates a new advertising set. If operation succeed, device will start advertising. This
     * method returns immediately, the operation status is delivered through {@code
     * callback.onAdvertisingSetStarted()}.
     *
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission when
     * {@code parameters.getOwnAddressType()} is different from {@code
     * AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT} or {@code parameters.isDirected()} is true.
     *
     * <p>The {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} permission is always enforced.
     *
     * @param parameters advertising set parameters.
     * @param advertiseData Advertisement data to be broadcasted. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}. If the advertisement is connectable,
     *     three bytes will be added for flags.
     * @param scanResponse Scan response associated with the advertisement data. Size must not
     *     exceed {@link BluetoothAdapter#getLeMaximumAdvertisingDataLength}.
     * @param periodicParameters periodic advertisng parameters. If null, periodic advertising will
     *     not be started.
     * @param periodicData Periodic advertising data. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}.
     * @param callback Callback for advertising set.
     * @param handler thread upon which the callbacks will be invoked.
     * @throws IllegalArgumentException when any of the data parameter exceed the maximum allowable
     *     size, or unsupported advertising PHY is selected, or when attempt to use Periodic
     *     Advertising feature is made when it's not supported by the controller.
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(
            allOf = {BLUETOOTH_ADVERTISE, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public void startAdvertisingSet(
            AdvertisingSetParameters parameters,
            AdvertiseData advertiseData,
            AdvertiseData scanResponse,
            PeriodicAdvertisingParameters periodicParameters,
            AdvertiseData periodicData,
            AdvertisingSetCallback callback,
            Handler handler) {
        startAdvertisingSet(
                parameters,
                advertiseData,
                scanResponse,
                periodicParameters,
                periodicData,
                0,
                0,
                callback,
                handler);
    }

    /**
     * Creates a new advertising set. If operation succeed, device will start advertising. This
     * method returns immediately, the operation status is delivered through {@code
     * callback.onAdvertisingSetStarted()}.
     *
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission when
     * {@code parameters.getOwnAddressType()} is different from {@code
     * AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT} or {@code parameters.isDirected()} is true.
     *
     * <p>The {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} permission is always enforced.
     *
     * @param parameters advertising set parameters.
     * @param advertiseData Advertisement data to be broadcasted. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}. If the advertisement is connectable,
     *     three bytes will be added for flags.
     * @param scanResponse Scan response associated with the advertisement data. Size must not
     *     exceed {@link BluetoothAdapter#getLeMaximumAdvertisingDataLength}.
     * @param periodicParameters periodic advertisng parameters. If null, periodic advertising will
     *     not be started.
     * @param periodicData Periodic advertising data. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}.
     * @param duration advertising duration, in 10ms unit. Valid range is from 1 (10ms) to 65535
     *     (655,350 ms). 0 means advertising should continue until stopped.
     * @param maxExtendedAdvertisingEvents maximum number of extended advertising events the
     *     controller shall attempt to send prior to terminating the extended advertising, even if
     *     the duration has not expired. Valid range is from 1 to 255. 0 means no maximum.
     * @param callback Callback for advertising set.
     * @throws IllegalArgumentException when any of the data parameter exceed the maximum allowable
     *     size, or unsupported advertising PHY is selected, or when attempt to use Periodic
     *     Advertising feature is made when it's not supported by the controller.
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(
            allOf = {BLUETOOTH_ADVERTISE, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public void startAdvertisingSet(
            AdvertisingSetParameters parameters,
            AdvertiseData advertiseData,
            AdvertiseData scanResponse,
            PeriodicAdvertisingParameters periodicParameters,
            AdvertiseData periodicData,
            int duration,
            int maxExtendedAdvertisingEvents,
            AdvertisingSetCallback callback) {
        startAdvertisingSet(
                parameters,
                advertiseData,
                scanResponse,
                periodicParameters,
                periodicData,
                duration,
                maxExtendedAdvertisingEvents,
                callback,
                new Handler(Looper.getMainLooper()));
    }

    /**
     * Creates a new advertising set. If operation succeed, device will start advertising. This
     * method returns immediately, the operation status is delivered through {@code
     * callback.onAdvertisingSetStarted()}.
     *
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission when
     * {@code parameters.getOwnAddressType()} is different from {@code
     * AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT} or {@code parameters.isDirected()} is true.
     *
     * <p>The {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} permission is always enforced.
     *
     * @param parameters Advertising set parameters.
     * @param advertiseData Advertisement data to be broadcasted. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}. If the advertisement is connectable,
     *     three bytes will be added for flags.
     * @param scanResponse Scan response associated with the advertisement data. Size must not
     *     exceed {@link BluetoothAdapter#getLeMaximumAdvertisingDataLength}
     * @param periodicParameters Periodic advertisng parameters. If null, periodic advertising will
     *     not be started.
     * @param periodicData Periodic advertising data. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}
     * @param duration advertising duration, in 10ms unit. Valid range is from 1 (10ms) to 65535
     *     (655,350 ms). 0 means advertising should continue until stopped.
     * @param maxExtendedAdvertisingEvents maximum number of extended advertising events the
     *     controller shall attempt to send prior to terminating the extended advertising, even if
     *     the duration has not expired. Valid range is from 1 to 255. 0 means no maximum.
     * @param callback Callback for advertising set.
     * @param handler Thread upon which the callbacks will be invoked.
     * @throws IllegalArgumentException When any of the data parameter exceed the maximum allowable
     *     size, or unsupported advertising PHY is selected, or when attempt to use Periodic
     *     Advertising feature is made when it's not supported by the controller, or when
     *     maxExtendedAdvertisingEvents is used on a controller that doesn't support the LE Extended
     *     Advertising
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(
            allOf = {BLUETOOTH_ADVERTISE, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public void startAdvertisingSet(
            AdvertisingSetParameters parameters,
            AdvertiseData advertiseData,
            AdvertiseData scanResponse,
            PeriodicAdvertisingParameters periodicParameters,
            AdvertiseData periodicData,
            int duration,
            int maxExtendedAdvertisingEvents,
            AdvertisingSetCallback callback,
            Handler handler) {
        startAdvertisingSet(
                parameters,
                advertiseData,
                scanResponse,
                periodicParameters,
                periodicData,
                duration,
                maxExtendedAdvertisingEvents,
                null,
                callback,
                handler);
    }

    /**
     * Creates a new advertising set. If operation succeed, device will start advertising. This
     * method returns immediately, the operation status is delivered through {@code
     * callback.onAdvertisingSetStarted()}.
     *
     * <p>If the {@code gattServer} is provided, connections to this advertisement will only see the
     * services/characteristics in this server, rather than the union of all GATT services (across
     * all opened servers).
     *
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_PRIVILEGED} permission when
     * {@code parameters.getOwnAddressType()} is different from {@code
     * AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT} or {@code parameters.isDirected()} is true or
     * when the {@code gattServer} is already registered
     *
     * <p>The {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} permission is always enforced.
     *
     * @param parameters Advertising set parameters.
     * @param advertiseData Advertisement data to be broadcasted. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}. If the advertisement is connectable,
     *     three bytes will be added for flags.
     * @param scanResponse Scan response associated with the advertisement data. Size must not
     *     exceed {@link BluetoothAdapter#getLeMaximumAdvertisingDataLength}
     * @param periodicParameters Periodic advertisng parameters. If null, periodic advertising will
     *     not be started.
     * @param periodicData Periodic advertising data. Size must not exceed {@link
     *     BluetoothAdapter#getLeMaximumAdvertisingDataLength}
     * @param duration advertising duration, in 10ms unit. Valid range is from 1 (10ms) to 65535
     *     (655,350 ms). 0 means advertising should continue until stopped.
     * @param maxExtendedAdvertisingEvents maximum number of extended advertising events the
     *     controller shall attempt to send prior to terminating the extended advertising, even if
     *     the duration has not expired. Valid range is from 1 to 255. 0 means no maximum.
     * @param gattServer the GATT server that will "own" connections derived from this advertising
     *     set.
     * @param callback Callback for advertising set.
     * @param handler Thread upon which the callbacks will be invoked.
     * @throws IllegalArgumentException When any of the data parameter exceed the maximum allowable
     *     size, or unsupported advertising PHY is selected, or when attempt to use Periodic
     *     Advertising feature is made when it's not supported by the controller, or when
     *     maxExtendedAdvertisingEvents is used on a controller that doesn't support the LE Extended
     *     Advertising
     * @hide
     */
    @SystemApi
    @SuppressLint("ExecutorRegistration")
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(
            allOf = {BLUETOOTH_ADVERTISE, BLUETOOTH_PRIVILEGED},
            conditional = true)
    public void startAdvertisingSet(
            @NonNull AdvertisingSetParameters parameters,
            @Nullable AdvertiseData advertiseData,
            @Nullable AdvertiseData scanResponse,
            @Nullable PeriodicAdvertisingParameters periodicParameters,
            @Nullable AdvertiseData periodicData,
            int duration,
            int maxExtendedAdvertisingEvents,
            @Nullable BluetoothGattServer gattServer,
            @Nullable AdvertisingSetCallback callback,
            @SuppressLint("ListenerLast") @NonNull Handler handler) {
        BluetoothLeUtils.checkAdapterStateOn(mBluetoothAdapter);
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }

        boolean isConnectable = parameters.isConnectable();
        boolean isDiscoverable = parameters.isDiscoverable();
        boolean hasFlags = isConnectable && isDiscoverable;
        if (parameters.isLegacy()) {
            if (totalBytes(advertiseData, hasFlags) > MAX_LEGACY_ADVERTISING_DATA_BYTES) {
                throw new IllegalArgumentException("Legacy advertising data too big");
            }

            if (totalBytes(scanResponse, false) > MAX_LEGACY_ADVERTISING_DATA_BYTES) {
                throw new IllegalArgumentException("Legacy scan response data too big");
            }
        } else {
            boolean supportCodedPhy = mBluetoothAdapter.isLeCodedPhySupported();
            boolean support2MPhy = mBluetoothAdapter.isLe2MPhySupported();
            int pphy = parameters.getPrimaryPhy();
            int sphy = parameters.getSecondaryPhy();
            if (pphy == BluetoothDevice.PHY_LE_CODED && !supportCodedPhy) {
                throw new IllegalArgumentException("Unsupported primary PHY selected");
            }

            if ((sphy == BluetoothDevice.PHY_LE_CODED && !supportCodedPhy)
                    || (sphy == BluetoothDevice.PHY_LE_2M && !support2MPhy)) {
                throw new IllegalArgumentException("Unsupported secondary PHY selected");
            }

            int maxData = mBluetoothAdapter.getLeMaximumAdvertisingDataLength();
            if (totalBytes(advertiseData, hasFlags) > maxData) {
                throw new IllegalArgumentException("Advertising data too big");
            }

            if (totalBytes(scanResponse, false) > maxData) {
                throw new IllegalArgumentException("Scan response data too big");
            }

            if (totalBytes(periodicData, false) > maxData) {
                throw new IllegalArgumentException("Periodic advertising data too big");
            }

            boolean supportPeriodic = mBluetoothAdapter.isLePeriodicAdvertisingSupported();
            if (periodicParameters != null && !supportPeriodic) {
                throw new IllegalArgumentException(
                        "Controller does not support LE Periodic Advertising");
            }
        }

        if (maxExtendedAdvertisingEvents < 0 || maxExtendedAdvertisingEvents > 255) {
            throw new IllegalArgumentException(
                    "maxExtendedAdvertisingEvents out of range: " + maxExtendedAdvertisingEvents);
        }

        if (maxExtendedAdvertisingEvents != 0
                && !mBluetoothAdapter.isLeExtendedAdvertisingSupported()) {
            throw new IllegalArgumentException(
                    "Can't use maxExtendedAdvertisingEvents with controller that doesn't support "
                            + "LE Extended Advertising");
        }

        if (duration < 0 || duration > 65535) {
            throw new IllegalArgumentException("duration out of range: " + duration);
        }

        IBluetoothAdvertise advertise = mBluetoothAdapter.getBluetoothAdvertise();

        if (advertise == null) {
            Log.e(TAG, "Bluetooth Advertise is null");
            postStartSetFailure(
                    handler, callback, AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR);
            return;
        }

        IAdvertisingSetCallback wrapped = wrap(callback, handler);
        if (mCallbackWrappers.putIfAbsent(callback, wrapped) != null) {
            throw new IllegalArgumentException(
                    "callback instance already associated with advertising");
        }

        try {
            advertise.startAdvertisingSet(
                    parameters,
                    advertiseData,
                    scanResponse,
                    periodicParameters,
                    periodicData,
                    duration,
                    maxExtendedAdvertisingEvents,
                    gattServer == null ? 0 : gattServer.getServerIf(),
                    wrapped,
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start advertising set - ", e);
            postStartSetFailure(
                    handler, callback, AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR);
            return;
        } catch (SecurityException e) {
            mCallbackWrappers.remove(callback);
            throw e;
        }
    }

    /**
     * Used to dispose of a {@link AdvertisingSet} object, obtained with {@link
     * BluetoothLeAdvertiser#startAdvertisingSet}.
     */
    @RequiresLegacyBluetoothAdminPermission
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(BLUETOOTH_ADVERTISE)
    public void stopAdvertisingSet(AdvertisingSetCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }

        IAdvertisingSetCallback wrapped = mCallbackWrappers.remove(callback);
        if (wrapped == null) {
            return;
        }

        IBluetoothAdvertise advertise = mBluetoothAdapter.getBluetoothAdvertise();
        if (advertise == null) {
            Log.e(TAG, "Bluetooth Advertise is null");
            return;
        }
        try {
            advertise.stopAdvertisingSet(wrapped, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop advertising - ", e);
        }
    }

    /**
     * Cleans up advertisers. Should be called when bluetooth is down.
     *
     * @hide
     */
    @RequiresNoPermission
    public void cleanup() {
        mLegacyAdvertisers.clear();
        mCallbackWrappers.clear();
        mAdvertisingSets.clear();
    }

    // Compute the size of advertisement data or scan resp
    @RequiresBluetoothAdvertisePermission
    @RequiresPermission(BLUETOOTH_ADVERTISE)
    private int totalBytes(AdvertiseData data, boolean isFlagsIncluded) {
        if (data == null) return 0;
        // Flags field is omitted if the advertising is not connectable.
        int size = (isFlagsIncluded) ? FLAGS_FIELD_BYTES : 0;
        if (data.getServiceUuids() != null) {
            int num16BitUuids = 0;
            int num32BitUuids = 0;
            int num128BitUuids = 0;
            for (ParcelUuid uuid : data.getServiceUuids()) {
                if (BluetoothUuid.is16BitUuid(uuid)) {
                    ++num16BitUuids;
                } else if (BluetoothUuid.is32BitUuid(uuid)) {
                    ++num32BitUuids;
                } else {
                    ++num128BitUuids;
                }
            }
            // 16 bit service uuids are grouped into one field when doing advertising.
            if (num16BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num16BitUuids * BluetoothUuid.UUID_BYTES_16_BIT;
            }
            // 32 bit service uuids are grouped into one field when doing advertising.
            if (num32BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num32BitUuids * BluetoothUuid.UUID_BYTES_32_BIT;
            }
            // 128 bit service uuids are grouped into one field when doing advertising.
            if (num128BitUuids != 0) {
                size +=
                        OVERHEAD_BYTES_PER_FIELD
                                + num128BitUuids * BluetoothUuid.UUID_BYTES_128_BIT;
            }
        }
        if (data.getServiceSolicitationUuids() != null) {
            int num16BitUuids = 0;
            int num32BitUuids = 0;
            int num128BitUuids = 0;
            for (ParcelUuid uuid : data.getServiceSolicitationUuids()) {
                if (BluetoothUuid.is16BitUuid(uuid)) {
                    ++num16BitUuids;
                } else if (BluetoothUuid.is32BitUuid(uuid)) {
                    ++num32BitUuids;
                } else {
                    ++num128BitUuids;
                }
            }
            // 16 bit service uuids are grouped into one field when doing advertising.
            if (num16BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num16BitUuids * BluetoothUuid.UUID_BYTES_16_BIT;
            }
            // 32 bit service uuids are grouped into one field when doing advertising.
            if (num32BitUuids != 0) {
                size += OVERHEAD_BYTES_PER_FIELD + num32BitUuids * BluetoothUuid.UUID_BYTES_32_BIT;
            }
            // 128 bit service uuids are grouped into one field when doing advertising.
            if (num128BitUuids != 0) {
                size +=
                        OVERHEAD_BYTES_PER_FIELD
                                + num128BitUuids * BluetoothUuid.UUID_BYTES_128_BIT;
            }
        }
        for (TransportDiscoveryData transportDiscoveryData : data.getTransportDiscoveryData()) {
            size += OVERHEAD_BYTES_PER_FIELD + transportDiscoveryData.totalBytes();
        }
        for (ParcelUuid uuid : data.getServiceData().keySet()) {
            int uuidLen = BluetoothUuid.uuidToBytes(uuid).length;
            size +=
                    OVERHEAD_BYTES_PER_FIELD
                            + uuidLen
                            + byteLength(data.getServiceData().get(uuid));
        }
        for (int i = 0; i < data.getManufacturerSpecificData().size(); ++i) {
            size +=
                    OVERHEAD_BYTES_PER_FIELD
                            + MANUFACTURER_SPECIFIC_DATA_LENGTH
                            + byteLength(data.getManufacturerSpecificData().valueAt(i));
        }
        if (data.getIncludeTxPowerLevel()) {
            size += OVERHEAD_BYTES_PER_FIELD + 1; // tx power level value is one byte.
        }
        if (data.getIncludeDeviceName()) {
            final int length = mBluetoothAdapter.getNameLengthForAdvertise();
            if (length >= 0) {
                size += OVERHEAD_BYTES_PER_FIELD + length;
            }
        }
        return size;
    }

    private int byteLength(byte[] array) {
        return array == null ? 0 : array.length;
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    IAdvertisingSetCallback wrap(AdvertisingSetCallback callback, Handler handler) {
        return new IAdvertisingSetCallback.Stub() {
            @Override
            public void onAdvertisingSetStarted(
                    IBinder advertiseBinder, int advertiserId, int txPower, int status) {
                handler.post(
                        () -> {
                            if (status != AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                                callback.onAdvertisingSetStarted(null, 0, status);
                                mCallbackWrappers.remove(callback);
                                return;
                            }

                            AdvertisingSet advertisingSet =
                                    new AdvertisingSet(
                                            IBluetoothAdvertise.Stub.asInterface(advertiseBinder),
                                            advertiserId,
                                            mBluetoothAdapter,
                                            mAttributionSource);
                            mAdvertisingSets.put(advertiserId, advertisingSet);
                            callback.onAdvertisingSetStarted(advertisingSet, txPower, status);
                        });
            }

            @Override
            public void onOwnAddressRead(int advertiserId, int addressType, String address) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onOwnAddressRead(advertisingSet, addressType, address);
                        });
            }

            @Override
            public void onAdvertisingSetStopped(int advertiserId) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onAdvertisingSetStopped(advertisingSet);
                            mAdvertisingSets.remove(advertiserId);
                            mCallbackWrappers.remove(callback);
                        });
            }

            @Override
            public void onAdvertisingEnabled(int advertiserId, boolean enabled, int status) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onAdvertisingEnabled(advertisingSet, enabled, status);
                        });
            }

            @Override
            public void onAdvertisingDataSet(int advertiserId, int status) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onAdvertisingDataSet(advertisingSet, status);
                        });
            }

            @Override
            public void onScanResponseDataSet(int advertiserId, int status) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onScanResponseDataSet(advertisingSet, status);
                        });
            }

            @Override
            public void onAdvertisingParametersUpdated(int advertiserId, int txPower, int status) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onAdvertisingParametersUpdated(
                                    advertisingSet, txPower, status);
                        });
            }

            @Override
            public void onPeriodicAdvertisingParametersUpdated(int advertiserId, int status) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onPeriodicAdvertisingParametersUpdated(advertisingSet, status);
                        });
            }

            @Override
            public void onPeriodicAdvertisingDataSet(int advertiserId, int status) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onPeriodicAdvertisingDataSet(advertisingSet, status);
                        });
            }

            @Override
            public void onPeriodicAdvertisingEnabled(int advertiserId, boolean enable, int status) {
                handler.post(
                        () -> {
                            AdvertisingSet advertisingSet = mAdvertisingSets.get(advertiserId);
                            callback.onPeriodicAdvertisingEnabled(advertisingSet, enable, status);
                        });
            }
        };
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private void postStartSetFailure(
            Handler handler, final AdvertisingSetCallback callback, final int error) {
        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        callback.onAdvertisingSetStarted(null, 0, error);
                    }
                });
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private void postStartFailure(final AdvertiseCallback callback, final int error) {
        mHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        callback.onStartFailure(error);
                    }
                });
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private void postStartSuccess(
            final AdvertiseCallback callback, final AdvertiseSettings settings) {
        mHandler.post(
                new Runnable() {

                    @Override
                    public void run() {
                        callback.onStartSuccess(settings);
                    }
                });
    }
}
