/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony.d2d;

import android.annotation.NonNull;

import java.util.Set;

/**
 * Base definition for a device-to-device communication protocol.
 */
public interface TransportProtocol {

    /**
     * Callbacks from the {@link TransportProtocol} to the {@link Communicator} which indicates
     * important events like protocol negotiation status as well as incoming messages.
     */
    public interface Callback {
        /**
         * The {@link TransportProtocol} calls this method when protocol negotiation has completed
         * successfully.
         * @param protocol The protocol which succeeded (should be {@code this}).
         */
        void onNegotiationSuccess(@NonNull TransportProtocol protocol);

        /**
         * The {@link TransportProtocol} calls this method when protocol negotiation has failed.
         * @param protocol The protocol which failed (should be {@code this}).
         */
        void onNegotiationFailed(@NonNull TransportProtocol protocol);

        /**
         * The {@link TransportProtocol} calls this method when the protocol has received incoming
         * messages.
         * @param messages The received messages.
         */
        void onMessagesReceived(@NonNull Set<Communicator.Message> messages);
    }

    /**
     * Called by the {@link Communicator} to register for callbacks regarding the transport's
     * progress.
     * @param callback the callback to use.
     */
    void setCallback(Callback callback);

    /**
     * Called by {@link Communicator} when negotiation of device-to-device communication using a
     * protocol should be started.
     */
    void startNegotiation();

    /**
     * Called by {@link Communicator} when a message should be sent using device-to-device
     * communication.
     * @param messages the messages to send via the transport.
     */
    void sendMessages(Set<Communicator.Message> messages);

    /**
     * Forces this transport to be in a negotiated state.
     */
    void forceNegotiated();

    /**
     * Forces this transport to be in a non-negotiated state.
     */
    void forceNotNegotiated();
}
