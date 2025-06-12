/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.sensors;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.SensorDirectChannel;
import android.os.ParcelFileDescriptor;

import java.util.concurrent.Executor;

/**
 * Local system service interface for sensors.
 *
 * @hide Only for use within system server.
 */
public abstract class SensorManagerInternal {
    /**
     * Adds a listener for changes in proximity sensor state.
     * @param executor The {@link Executor} to {@link Executor#execute invoke} the listener on.
     * @param listener The listener to add.
     *
     * @throws IllegalArgumentException when adding a listener that is already listening
     */
    public abstract void addProximityActiveListener(@NonNull Executor executor,
            @NonNull ProximityActiveListener listener);

    /**
     * Removes a previously registered listener of proximity sensor state changes.
     * @param listener The listener to remove.
     */
    public abstract void removeProximityActiveListener(@NonNull ProximityActiveListener listener);

    /**
     * Creates a sensor that is registered at runtime by the system with the sensor service.
     *
     * The runtime sensors created here are different from the
     * <a href="https://source.android.com/docs/core/interaction/sensors/sensors-hal2#dynamic-sensors">
     * dynamic sensor support in the HAL</a>. These sensors have no HAL dependency and correspond to
     * sensors that belong to an external (virtual) device.
     *
     * @param deviceId The identifier of the device this sensor is associated with.
     * @param type The generic type of the sensor.
     * @param name The name of the sensor.
     * @param vendor The vendor string of the sensor.
     * @param callback The callback to get notified when the sensor listeners have changed.
     * @return The sensor handle.
     */
    public abstract int createRuntimeSensor(int deviceId, int type, @NonNull String name,
            @NonNull String vendor, float maximumRange, float resolution, float power,
            int minDelay, int maxDelay, int flags, @NonNull RuntimeSensorCallback callback);

    /**
     * Unregisters the sensor with the given handle from the framework.
     */
    public abstract void removeRuntimeSensor(int handle);

    /**
     * Sends an event for the runtime sensor with the given handle to the framework.
     *
     * <p>Only relevant for sending runtime sensor events. @see #createRuntimeSensor.</p>
     *
     * @param handle The sensor handle.
     * @param type The type of the sensor.
     * @param timestampNanos When the event occurred.
     * @param values The values of the event.
     * @return Whether the event injection was successful.
     */
    public abstract boolean sendSensorEvent(int handle, int type, long timestampNanos,
            @NonNull float[] values);

    /**
     * Sends an additional info event for the runtime sensor with the given handle to the framework.
     *
     * <p>Only relevant for runtime sensors. @see #createRuntimeSensor.</p>
     *
     * @param handle The sensor handle.
     * @param type The type of payload data.
     * @param serial The sequence number of this frame for this type.
     * @param timestampNanos Timestamp of the event.
     * @param values The payload data represented in float values.
     * @return Whether the event injection was successful.
     */
    public abstract boolean sendSensorAdditionalInfo(int handle, int type, int serial,
            long timestampNanos, @Nullable float[] values);

    /**
     * Listener for proximity sensor state changes.
     */
    public interface ProximityActiveListener {
        /**
         * Callback invoked when the proximity sensor state changes
         * @param isActive whether the sensor is being enabled or disabled.
         */
        void onProximityActive(boolean isActive);
    }

    /**
     * Callback for runtime sensor state changes. Only relevant to sensors created via
     * {@link #createRuntimeSensor}, i.e. the dynamic sensors created via the dynamic sensor HAL are
     * not covered.
     */
    public interface RuntimeSensorCallback {
        /**
         * Invoked when the listeners of the runtime sensor have changed.
         * Returns zero on success, negative error code otherwise.
         */
        int onConfigurationChanged(int handle, boolean enabled, int samplingPeriodMicros,
                int batchReportLatencyMicros);

        /**
         * Invoked when a direct sensor channel has been created.
         * Wraps the file descriptor in a {@link android.os.SharedMemory} object and passes it to
         * the client process.
         * Returns a positive identifier of the channel on success, negative error code otherwise.
         */
        int onDirectChannelCreated(ParcelFileDescriptor fd);

        /**
         * Invoked when a direct sensor channel has been destroyed.
         */
        void onDirectChannelDestroyed(int channelHandle);

        /**
         * Invoked when a direct sensor channel has been configured for a sensor.
         * If the invocation is unsuccessful, a negative error code is returned.
         * On success, the return value is zero if the rate level is {@code RATE_STOP}, and a
         * positive report token otherwise.
         */
        int onDirectChannelConfigured(int channelHandle, int sensorHandle,
                @SensorDirectChannel.RateLevel int rateLevel);
    }
}
