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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.IBluetoothGatt;
import android.content.AttributionSource;
import android.os.Binder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/**
 * This class provides a way to control an active distance measurement session.
 * <p>It also defines the required {@link DistanceMeasurementSession.Callback} that must be
 * implemented in order to be notified of distance measurement results and status events related to
 * the {@link DistanceMeasurementSession}.
 *
 * <p>To get an instance of {@link DistanceMeasurementSession}, first use
 * {@link DistanceMeasurementManager#startMeasurementSession(DistanceMeasurementParams, Executor,
 * DistanceMeasurementSession.Callback)} to request to start a session. Once the session is started,
 * a {@link DistanceMeasurementSession} object is provided through
 * {@link DistanceMeasurementSession.Callback#onStarted(DistanceMeasurementSession)}.
 * If starting a session fails, the failure is reported through
 * {@link DistanceMeasurementSession.Callback#onStartFail(int)} with the failure reason.
 *
 * @hide
 */
@SystemApi
public final class DistanceMeasurementSession {
    private static final String TAG = "DistanceMeasurementSession";

    private final IBluetoothGatt mGatt;
    private final ParcelUuid mUuid;
    private final DistanceMeasurementParams mDistanceMeasurementParams;
    private final Executor mExecutor;
    private final Callback mCallback;
    private final AttributionSource mAttributionSource;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_DISTANCE_MEASUREMENT_INTERNAL,
    })
    public @interface StopSessionReturnValues{}

    /**
     * @hide
     */
    public DistanceMeasurementSession(IBluetoothGatt gatt, ParcelUuid uuid,
            DistanceMeasurementParams params, Executor executor,
            AttributionSource attributionSource, Callback callback) {
        Objects.requireNonNull(gatt, "gatt is null");
        Objects.requireNonNull(params, "params is null");
        Objects.requireNonNull(executor, "executor is null");
        Objects.requireNonNull(callback, "callback is null");
        mGatt = gatt;
        mUuid = uuid;
        mDistanceMeasurementParams = params;
        mExecutor = executor;
        mAttributionSource = attributionSource;
        mCallback = callback;
    }

    /**
     * Stops actively ranging, {@link Callback#onStopped} will be invoked if this succeeds.
     *
     * @return whether successfully stop or not
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public @StopSessionReturnValues int stopSession() {
        final int defaultValue = BluetoothStatusCodes.ERROR_TIMEOUT;
        try {
            final SynchronousResultReceiver<Integer> recv = SynchronousResultReceiver.get();
            mGatt.stopDistanceMeasurement(mUuid, mDistanceMeasurementParams.getDevice(),
                    mDistanceMeasurementParams.getMethodId(), mAttributionSource, recv);
            return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
        } catch (TimeoutException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        return defaultValue;
    }

    /**
     * @hide
     */
    void onStarted() {
        executeCallback(() -> mCallback.onStarted(this));
    }

    /**
     * @hide
     */
    void onStartFail(int reason) {
        executeCallback(() -> mCallback.onStartFail(reason));
    }


    /**
     * @hide
     */
    void onStopped(int reason) {
        executeCallback(() -> mCallback.onStopped(this, reason));
    }

    /**
     * @hide
     */
    void onResult(@NonNull BluetoothDevice device,
            @NonNull DistanceMeasurementResult result) {
        executeCallback(() -> mCallback.onResult(device, result));
    }


    /**
     * @hide
     */
    private void executeCallback(@NonNull Runnable runnable) {
        final long identity = Binder.clearCallingIdentity();
        try {
            mExecutor.execute(runnable);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * Interface for receiving {@link DistanceMeasurementSession} events.
     *
     * @hide
     */
    @SystemApi
    public interface Callback {
        /**
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                BluetoothStatusCodes.ERROR_UNKNOWN,
                BluetoothStatusCodes.FEATURE_NOT_SUPPORTED,
                BluetoothStatusCodes.ERROR_REMOTE_OPERATION_NOT_SUPPORTED,
                BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST,
                BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST,
                BluetoothStatusCodes.REASON_REMOTE_REQUEST,
                BluetoothStatusCodes.ERROR_TIMEOUT,
                BluetoothStatusCodes.ERROR_NO_LE_CONNECTION,
                BluetoothStatusCodes.ERROR_BAD_PARAMETERS,
                BluetoothStatusCodes.ERROR_DISTANCE_MEASUREMENT_INTERNAL,
        })
        @interface Reason {}

        /**
         * Invoked when {@link DistanceMeasurementManager#startMeasurementSession(
         * DistanceMeasurementParams, Executor, DistanceMeasurementSession.Callback)} is successful.
         *
         * @param session the started {@link DistanceMeasurementSession}
         *
         * @hide
         */
        @SystemApi
        void onStarted(@NonNull DistanceMeasurementSession session);

         /**
         * Invoked if {@link DistanceMeasurementManager#startMeasurementSession(
         * DistanceMeasurementParams, Executor, DistanceMeasurementSession.Callback)} fails.
         *
         * @param reason the failure reason
         *
         * @hide
         */
        @SystemApi
        void onStartFail(@NonNull @Reason int reason);

        /**
         * Invoked when a distance measurement session stopped.
         *
         * @param reason reason for the session stop
         *
         * @hide
         */
        @SystemApi
        void onStopped(@NonNull DistanceMeasurementSession session, @NonNull @Reason int reason);

        /**
         * Invoked when get distance measurement result.
         *
         * @param device remote device
         * @param result {@link DistanceMeasurementResult} for this device
         *
         * @hide
         */
        @SystemApi
        void onResult(@NonNull BluetoothDevice device,
                @NonNull DistanceMeasurementResult result);
    }
}
