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

package android.ranging.oob;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SuppressLint;

import com.android.ranging.flags.Flags;

import java.util.concurrent.Executor;

/**
 * TransportHandle is used as the Out-Of-Band (OOB) transport mechanism by ranging module.
 * In cases where module is used in a non-raw ranging mode, the user shall provide an implementation
 * of the TransportHandle, allowing ranging module to do the necessary OOB communication with a peer
 * device using the provided transport handle. Some examples of OOB transport between two peer
 * devices are:
 * <ul>
 *     <li>BLE GATT connection</li>
 *     <li>Wi-Fi MDNS link</li>
 *     <li>Internet</li>
 * </ul>
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public interface TransportHandle extends AutoCloseable {

    /** Send data to the peer device via the implemented OOB transport.
     *
     * @param data the data to be sent to the peer device. Must not be null.
     * @throws IllegalArgumentException if the provided data is null or invalid
     */
    void sendData(@NonNull byte[] data);

    /**
     * Registers a callback to receive updates from the transport mechanism.
     *
     * <p>The callback should be used to notify information about the peer device including the
     * data received from the peer device.
     *
     * @param executor the {@link Executor} on which the callback should be invoked. Must not be
     *                 null.
     * @param callback the {@link ReceiveCallback} instance to receive updates. Must not be null.
     * @throws IllegalArgumentException if either {@code executor} or {@code callback} is null.
     */
    @SuppressLint({"PairedRegistration"})
    void registerReceiveCallback(@NonNull Executor executor, @NonNull ReceiveCallback callback);

    /** TransportHandle callback. */
    interface ReceiveCallback {
        /**
         * Notifies and provides data received from the peer device.
         *
         * @param data the data received from the peer device. Must not be null.
         */
        void onReceiveData(@NonNull byte[] data);

        /**
         * Called when a data send operation fails.
         */
        void onSendFailed();

        /**
         * Notifies the receiver that the TransportHandle instance can't be used to receive or send
         * data until {@see onReconnect()} is called.
         */
        void onDisconnect();

        /**
         * Notifies the receiver the TransportHandle instance can be used again to send and receive
         * data. Should only be called if {@see onDisconnect()} preceded it.
         */
        void onReconnect();

        /**
         * Notifies the receiver that the TransportHandle instance can't be used anymore to receive
         * or send data. Also call this in {@link AutoCloseable#close()}.
         */
        void onClose();
    }
}
