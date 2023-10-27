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

import static android.bluetooth.le.BluetoothLeUtils.getSyncTimeout;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManager;
import android.content.AttributionSource;
import android.os.CancellationSignal;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/**
 * This class provides methods to perform distance measurement related
 * operations. An application can start distance measurement by using
 * {@link DistanceMeasurementManager#startMeasurementSession}.
 * <p>
 * Use {@link BluetoothAdapter#getDistanceMeasurementManager()} to get an instance of
 * {@link DistanceMeasurementManager}.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementManager {
    private static final String TAG = "DistanceMeasurementManager";

    private final ConcurrentHashMap<BluetoothDevice, DistanceMeasurementSession> mSessionMap =
            new ConcurrentHashMap<>();
    private final BluetoothAdapter mBluetoothAdapter;
    private final IBluetoothManager mBluetoothManager;
    private final AttributionSource mAttributionSource;
    private final ParcelUuid mUuid;

    /**
     * Use {@link BluetoothAdapter#getDistanceMeasurementManager()} instead.
     *
     * @hide
     */
    public DistanceMeasurementManager(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = Objects.requireNonNull(bluetoothAdapter);
        mBluetoothManager = mBluetoothAdapter.getBluetoothManager();
        mAttributionSource = mBluetoothAdapter.getAttributionSource();
        mUuid = new ParcelUuid(UUID.randomUUID());
    }

    /**
     * Get the supported methods of distance measurement.
     *
     * <p> This can be used to check supported methods before start distance measurement.
     *
     * @return a list of {@link DistanceMeasurementMethod}
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @NonNull List<DistanceMeasurementMethod> getSupportedMethods() {
        final ArrayList<DistanceMeasurementMethod> supportedMethods =
                new ArrayList<DistanceMeasurementMethod>();
        try {
            IBluetoothGatt gatt = mBluetoothManager.getBluetoothGatt();
            if (gatt == null) {
                Log.e(TAG, "Bluetooth GATT is null");
                return supportedMethods;
            }
            final SynchronousResultReceiver<List<DistanceMeasurementMethod>> recv =
                        SynchronousResultReceiver.get();
            gatt.getSupportedDistanceMeasurementMethods(mAttributionSource, recv);
            return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(new ArrayList<>());
        } catch (TimeoutException | RemoteException e) {
            Log.e(TAG, "Failed to get supported methods - ", e);
        }
        return supportedMethods;
    }

    /**
     * Start distance measurement and create a {@link DistanceMeasurementSession} for this
     * operation. Once the session is started, a {@link DistanceMeasurementSession} object is
     * provided through
     * {@link DistanceMeasurementSession.Callback#onStarted(DistanceMeasurementSession)}.
     * If starting a session fails, the failure is reported through
     * {@link DistanceMeasurementSession.Callback#onStartFail(int)} with the failure reason.
     *
     * @param params parameters of this operation
     * @param executor Executor to run callback
     * @param callback callback to associate with the
     *                 {@link DistanceMeasurementSession} that is being started. The callback is
     *                 registered by this function and unregisted when
     *                 {@link DistanceMeasurementSession.Callback#onStartFail(int)} or
     *                 {@link DistanceMeasurementSession
     *                 .Callback#onStopped(DistanceMeasurementSession, int)}
     * @return a CancellationSignal that may be used to cancel the starting of the
     * {@link DistanceMeasurementSession}
     * @throws NullPointerException if any input parameter is null
     * @throws IllegalStateException if the session is already registered
     * @hide
     */
    @SystemApi
    @Nullable
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public CancellationSignal startMeasurementSession(
            @NonNull DistanceMeasurementParams params,
            @NonNull Executor executor,
            @NonNull DistanceMeasurementSession.Callback callback) {
        Objects.requireNonNull(params, "params is null");
        Objects.requireNonNull(executor, "executor is null");
        Objects.requireNonNull(callback, "callback is null");
        try {
            IBluetoothGatt gatt = mBluetoothManager.getBluetoothGatt();
            if (gatt == null) {
                Log.e(TAG, "Bluetooth GATT is null");
                return null;
            }
            DistanceMeasurementSession session = new DistanceMeasurementSession(gatt, mUuid,
                        params, executor, mAttributionSource, callback);
            CancellationSignal cancellationSignal = new CancellationSignal();
            cancellationSignal.setOnCancelListener(() -> session.stopSession());

            if (mSessionMap.containsKey(params.getDevice())) {
                throw new IllegalStateException(params.getDevice().getAnonymizedAddress()
                        + " already registered");
            }

            mSessionMap.put(params.getDevice(), session);
            final SynchronousResultReceiver recv = SynchronousResultReceiver.get();
            gatt.startDistanceMeasurement(mUuid, params, mCallbackWrapper, mAttributionSource,
                    recv);
            recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(null);
            return cancellationSignal;
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
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
