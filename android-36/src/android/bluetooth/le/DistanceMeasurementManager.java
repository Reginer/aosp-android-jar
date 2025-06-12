/*
 * Copyright 2022 The Android Open Source Project
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

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_PRIVILEGED;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IDistanceMeasurement;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.le.ChannelSoundingParams.CsSecurityLevel;
import android.content.AttributionSource;
import android.os.CancellationSignal;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.bluetooth.flags.Flags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * This class provides methods to perform distance measurement related operations. An application
 * can start distance measurement by using {@link
 * DistanceMeasurementManager#startMeasurementSession}.
 *
 * <p>Use {@link BluetoothAdapter#getDistanceMeasurementManager()} to get an instance of {@link
 * DistanceMeasurementManager}.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementManager {
    private static final String TAG = "DistanceMeasurementManager";

    private final ConcurrentHashMap<BluetoothDevice, DistanceMeasurementSession> mSessionMap =
            new ConcurrentHashMap<>();
    private final BluetoothAdapter mBluetoothAdapter;
    private final AttributionSource mAttributionSource;
    private final ParcelUuid mUuid;

    /**
     * Use {@link BluetoothAdapter#getDistanceMeasurementManager()} instead.
     *
     * @hide
     */
    public DistanceMeasurementManager(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = requireNonNull(bluetoothAdapter);
        mAttributionSource = mBluetoothAdapter.getAttributionSource();
        mUuid = new ParcelUuid(UUID.randomUUID());
    }

    /**
     * Get the supported methods of distance measurement.
     *
     * <p>This can be used to check supported methods before start distance measurement.
     *
     * @return a list of {@link DistanceMeasurementMethod}
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @NonNull List<DistanceMeasurementMethod> getSupportedMethods() {
        final List<DistanceMeasurementMethod> supportedMethods = new ArrayList<>();
        try {
            IDistanceMeasurement distanceMeasurement = mBluetoothAdapter.getDistanceMeasurement();
            if (distanceMeasurement == null) {
                Log.e(TAG, "Distance Measurement is null");
                return supportedMethods;
            }
            return distanceMeasurement.getSupportedDistanceMeasurementMethods(mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get supported methods - ", e);
        }
        return supportedMethods;
    }

    /**
     * Start distance measurement and create a {@link DistanceMeasurementSession} for this
     * operation. Once the session is started, a {@link DistanceMeasurementSession} object is
     * provided through {@link
     * DistanceMeasurementSession.Callback#onStarted(DistanceMeasurementSession)}. If starting a
     * session fails, the failure is reported through {@link
     * DistanceMeasurementSession.Callback#onStartFail(int)} with the failure reason.
     *
     * @param params parameters of this operation
     * @param executor Executor to run callback
     * @param callback callback to associate with the {@link DistanceMeasurementSession} that is
     *     being started. The callback is registered by this function and unregistered when {@link
     *     DistanceMeasurementSession.Callback#onStartFail(int)} or {@link
     *     DistanceMeasurementSession .Callback#onStopped(DistanceMeasurementSession, int)}
     * @return a CancellationSignal that may be used to cancel the starting of the {@link
     *     DistanceMeasurementSession}
     * @throws NullPointerException if any input parameter is null
     * @throws IllegalStateException if the session is already registered
     * @hide
     */
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @Nullable CancellationSignal startMeasurementSession(
            @NonNull DistanceMeasurementParams params,
            @NonNull Executor executor,
            @NonNull DistanceMeasurementSession.Callback callback) {
        requireNonNull(params);
        requireNonNull(executor);
        requireNonNull(callback);
        try {
            IDistanceMeasurement distanceMeasurement = mBluetoothAdapter.getDistanceMeasurement();
            if (distanceMeasurement == null) {
                Log.e(TAG, "Distance Measurement is null");
                return null;
            }
            DistanceMeasurementSession session =
                    new DistanceMeasurementSession(
                            distanceMeasurement,
                            mUuid,
                            params,
                            executor,
                            mAttributionSource,
                            callback);
            CancellationSignal cancellationSignal = new CancellationSignal();
            cancellationSignal.setOnCancelListener(() -> session.stopSession());

            if (mSessionMap.containsKey(params.getDevice())) {
                throw new IllegalStateException(
                        params.getDevice().getAnonymizedAddress() + " already registered");
            }

            mSessionMap.put(params.getDevice(), session);
            distanceMeasurement.startDistanceMeasurement(
                    mUuid, params, mCallbackWrapper, mAttributionSource);
            return cancellationSignal;
        } catch (RemoteException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        }
        return null;
    }

    /**
     * Get the maximum supported security level of channel sounding between the local device and a
     * specific remote device.
     *
     * <p>See: Vol 3 Part C, Chapter 10.11.1 of
     * https://bluetooth.com/specifications/specs/core60-html/
     *
     * @param remoteDevice remote device of channel sounding
     * @return max supported security level, {@link ChannelSoundingParams#CS_SECURITY_LEVEL_UNKNOWN}
     *     when Channel Sounding is not supported or encounters an internal error.
     * @deprecated do not use it, this is meaningless, no alternative API.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING_25Q2_APIS)
    @Deprecated
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    @CsSecurityLevel
    public int getChannelSoundingMaxSupportedSecurityLevel(@NonNull BluetoothDevice remoteDevice) {
        requireNonNull(remoteDevice);
        final int defaultValue = ChannelSoundingParams.CS_SECURITY_LEVEL_UNKNOWN;
        try {
            IDistanceMeasurement distanceMeasurement = mBluetoothAdapter.getDistanceMeasurement();
            if (distanceMeasurement == null) {
                Log.e(TAG, "Distance Measurement is null");
                return defaultValue;
            }
            return distanceMeasurement.getChannelSoundingMaxSupportedSecurityLevel(
                    remoteDevice, mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get supported security Level - ", e);
        }
        return defaultValue;
    }

    /**
     * Get the maximum supported security level of channel sounding of the local device.
     *
     * <p>See: Vol 3 Part C, Chapter 10.11.1 of
     * https://bluetooth.com/specifications/specs/core60-html/
     *
     * @return max supported security level, {@link ChannelSoundingParams#CS_SECURITY_LEVEL_UNKNOWN}
     *     when Channel Sounding is not supported or encounters an internal error.
     * @deprecated use {@link #getChannelSoundingSupportedSecurityLevels} instead.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING_25Q2_APIS)
    @Deprecated
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @CsSecurityLevel int getLocalChannelSoundingMaxSupportedSecurityLevel() {
        final int defaultValue = ChannelSoundingParams.CS_SECURITY_LEVEL_UNKNOWN;
        try {
            IDistanceMeasurement distanceMeasurement = mBluetoothAdapter.getDistanceMeasurement();
            if (distanceMeasurement == null) {
                Log.e(TAG, "Distance Measurement is null");
                return defaultValue;
            }
            return distanceMeasurement.getLocalChannelSoundingMaxSupportedSecurityLevel(
                    mAttributionSource);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get supported security Level - ", e);
        }
        return defaultValue;
    }

    /**
     * Get the set of supported security levels of channel sounding.
     *
     * <p>See: Vol 3 Part C, Chapter 10.11.1 of
     * https://bluetooth.com/specifications/specs/core60-html/
     *
     * @return the set of supported security levels, empty when encounters an internal error.
     * @throws UnsupportedOperationException if the {@link
     *     android.content.pm.PackageManager#FEATURE_BLUETOOTH_LE_CHANNEL_SOUNDING} is not
     *     supported.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING_25Q2_APIS)
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {BLUETOOTH_CONNECT, BLUETOOTH_PRIVILEGED})
    public @NonNull Set<@CsSecurityLevel Integer> getChannelSoundingSupportedSecurityLevels() {
        try {
            IDistanceMeasurement distanceMeasurement = mBluetoothAdapter.getDistanceMeasurement();
            if (distanceMeasurement == null) {
                Log.e(TAG, "Distance Measurement is null");
                return Collections.emptySet();
            }
            return Arrays.stream(
                            distanceMeasurement.getChannelSoundingSupportedSecurityLevels(
                                    mAttributionSource))
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get supported security Level - ", e);
        }
        return Collections.emptySet();
    }

    @SuppressLint("AndroidFrameworkBluetoothPermission")
    private final IDistanceMeasurementCallback mCallbackWrapper =
            new IDistanceMeasurementCallback.Stub() {
                @Override
                public void onStarted(BluetoothDevice device) {
                    DistanceMeasurementSession session = mSessionMap.get(device);
                    session.onStarted();
                }

                @Override
                public void onStartFail(BluetoothDevice device, int reason) {
                    DistanceMeasurementSession session = mSessionMap.get(device);
                    session.onStartFail(reason);
                    mSessionMap.remove(device);
                }

                @Override
                public void onStopped(BluetoothDevice device, int reason) {
                    DistanceMeasurementSession session = mSessionMap.get(device);
                    session.onStopped(reason);
                    mSessionMap.remove(device);
                }

                @Override
                public void onResult(BluetoothDevice device, DistanceMeasurementResult result) {
                    DistanceMeasurementSession session = mSessionMap.get(device);
                    session.onResult(device, result);
                }
            };
}
