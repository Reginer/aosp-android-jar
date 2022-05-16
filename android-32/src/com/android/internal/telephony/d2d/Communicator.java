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
import android.annotation.Nullable;
import android.telecom.Connection;
import android.telecom.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Responsible for facilitating device-to-device communication between both ends of a call.
 */
public class Communicator implements TransportProtocol.Callback {

    /**
     * Callback for events out of communicator.
     */
    public interface Callback {
        void onMessagesReceived(@NonNull Set<Message> messages);
        void onD2DAvailabilitychanged(boolean isAvailable);
    }

    public static final int MESSAGE_CALL_RADIO_ACCESS_TYPE = 1;
    public static final int MESSAGE_CALL_AUDIO_CODEC = 2;
    public static final int MESSAGE_DEVICE_BATTERY_STATE = 3;
    public static final int MESSAGE_DEVICE_NETWORK_COVERAGE = 4;

    public static final int RADIO_ACCESS_TYPE_LTE = 1;
    public static final int RADIO_ACCESS_TYPE_IWLAN = 2;
    public static final int RADIO_ACCESS_TYPE_NR = 3;

    public static final int AUDIO_CODEC_EVS = 1;
    public static final int AUDIO_CODEC_AMR_WB = 2;
    public static final int AUDIO_CODEC_AMR_NB = 3;

    public static final int BATTERY_STATE_LOW = 1;
    public static final int BATTERY_STATE_GOOD = 2;
    public static final int BATTERY_STATE_CHARGING = 3;

    public static final int COVERAGE_POOR = 1;
    public static final int COVERAGE_GOOD = 2;

    /**
     * Encapsulates a D2D communication message.
     */
    public static class Message {
        private int mType;
        private int mValue;

        public Message(int type, int value) {
            mType = type;
            mValue = value;
        }

        public int getType() {
            return mType;
        }

        public int getValue() {
            return mValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message message = (Message) o;
            return mType == message.mType && mValue == message.mValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mType, mValue);
        }

        @Override
        public String toString() {
            return "Message{" + "mType=" + messageToString(mType) +", mValue="
                    + valueToString(mType, mValue) + '}';
        }
    }

    private boolean mIsNegotiated;
    private boolean mIsNegotiationAttempted;
    private TransportProtocol mActiveTransport;
    private List<TransportProtocol> mTransportProtocols = new ArrayList<>();
    private Callback mCallback;

    public Communicator(@NonNull List<TransportProtocol> transportProtocols,
            @NonNull Callback callback) {
        Log.i(this, "Initializing communicator with transports: %s",
                transportProtocols.stream().map(p -> p.getClass().getSimpleName()).collect(
                        Collectors.joining(",")));
        mTransportProtocols.addAll(transportProtocols);
        mTransportProtocols.forEach(p -> p.setCallback(this));
        mCallback = callback;
    }

    /**
     * @return the active {@link TransportProtocol} which is being used for sending/receiving
     * messages.
     */
    public @Nullable TransportProtocol getActiveTransport() {
        return mActiveTransport;
    }

    /**
     * Handles state changes for a call.
     * @param id The call in question.
     * @param state The new state.
     */
    public void onStateChanged(String id, @Connection.ConnectionState int state) {
        Log.i(this, "onStateChanged: id=%s, newState=%d", id, state);
        if (state == Connection.STATE_ACTIVE) {
            // Protocol negotiation can start as we are active
            if (mActiveTransport == null && !mIsNegotiationAttempted) {
                mIsNegotiated = false;
                mIsNegotiationAttempted = true;
                Log.i(this, "onStateChanged: call active; negotiate D2D.");
                negotiateNextProtocol();
            }
        }
    }

    /**
     * Called by a {@link TransportProtocol} when negotiation of that protocol has succeeded.
     * @param protocol The protocol.
     */
    @Override
    public void onNegotiationSuccess(@NonNull TransportProtocol protocol) {
        if (protocol != mActiveTransport) {
            // Uh oh, shouldn't happen.
            String activeTransportName = mActiveTransport == null ? "none"
                    : mActiveTransport.getClass().getSimpleName();
            Log.w(this, "onNegotiationSuccess: ignored - %s negotiated but active transport is %s.",
                    protocol.getClass().getSimpleName(), activeTransportName);
        }
        Log.i(this, "onNegotiationSuccess: %s negotiated; setting active.",
                protocol.getClass().getSimpleName());
        mIsNegotiated = true;
        notifyD2DStatus(true /* isAvailable */);
    }

    /**
     * Called by a {@link TransportProtocol} when negotiation of that protocol has failed.
     * @param protocol The protocol.
     */
    @Override
    public void onNegotiationFailed(@NonNull TransportProtocol protocol) {
        if (protocol != mActiveTransport) {
            // Uh oh, shouldn't happen.
        }
        Log.i(this, "onNegotiationFailed: %s failed to negotiate.",
                protocol.getClass().getSimpleName());
        mIsNegotiated = false;
        negotiateNextProtocol();
    }

    /**
     * Called by a {@link TransportProtocol} to report incoming messages received via that
     * transport.
     * @param messages The received messages.
     */
    @Override
    public void onMessagesReceived(@NonNull Set<Message> messages) {
        if (mCallback != null) {
            mCallback.onMessagesReceived(messages);
        }
    }

    /**
     * Use the {@link Communicator} to send a set of device-to-device messages.
     * @param messages The {@link Message}s to send.
     */
    public void sendMessages(@NonNull Set<Message> messages) {
        if (mActiveTransport == null || !mIsNegotiated) {
            Log.w(this, "sendMessages: no active transport");
            return;
        }

        Log.i(this, "sendMessages: msgs=%d, activeTransport=%s",
                messages.size(), mActiveTransport.getClass().getSimpleName());
        mActiveTransport.sendMessages(messages);
    }

    /**
     * Find a new protocol to use and start negotiation.
     */
    private void negotiateNextProtocol() {
        mActiveTransport = getNextCandidateProtocol();
        if (mActiveTransport == null) {
            // No more protocols, exit.
            Log.i(this, "negotiateNextProtocol: no remaining transports.");
            notifyD2DStatus(false /* isAvailable */);
            return;
        }
        Log.i(this, "negotiateNextProtocol: trying %s",
                mActiveTransport.getClass().getSimpleName());
        mActiveTransport.startNegotiation();
    }

    /**
     * @return the next protocol to attempt to use.  If there is no active protocol, use the first
     * one; otherwise use the one after the currently active one.
     */
    private TransportProtocol getNextCandidateProtocol() {
        TransportProtocol candidateProtocol = null;
        if (mActiveTransport == null) {
            if (mTransportProtocols.size() > 0) {
                candidateProtocol = mTransportProtocols.get(0);
            } else {
                mIsNegotiated = false;
            }
        } else {
            for (int ix = 0; ix < mTransportProtocols.size(); ix++) {
                TransportProtocol protocol = mTransportProtocols.get(ix);
                if (protocol == mActiveTransport) {
                    if (ix + 1 < mTransportProtocols.size()) {
                        // Next one is candidate
                        candidateProtocol = mTransportProtocols.get(ix + 1);
                    }
                    break;
                }
            }
        }
        return candidateProtocol;
    }

    /**
     * Notifies listeners (okay, {@link com.android.services.telephony.TelephonyConnection} when
     * the availability of D2D communication changes.
     * @param isAvailable {@code true} if D2D is available, {@code false} otherwise.
     */
    private void notifyD2DStatus(boolean isAvailable) {
        if (mCallback != null) {
            mCallback.onD2DAvailabilitychanged(isAvailable);
        }
    }

    public static String messageToString(int messageType) {
        switch (messageType) {
            case MESSAGE_CALL_RADIO_ACCESS_TYPE:
                return "MESSAGE_CALL_RADIO_ACCESS_TYPE";
            case MESSAGE_CALL_AUDIO_CODEC:
                return "MESSAGE_CALL_AUDIO_CODEC";
            case MESSAGE_DEVICE_BATTERY_STATE:
                return "MESSAGE_DEVICE_BATTERY_STATE";
            case MESSAGE_DEVICE_NETWORK_COVERAGE:
                return "MESSAGE_DEVICE_NETWORK_COVERAGE";
        }
        return "";
    }

    public static String valueToString(int messageType, int value) {
        switch (messageType) {
            case MESSAGE_CALL_RADIO_ACCESS_TYPE:
                switch (value) {
                    case RADIO_ACCESS_TYPE_LTE:
                        return "RADIO_ACCESS_TYPE_LTE";
                    case RADIO_ACCESS_TYPE_IWLAN:
                        return "RADIO_ACCESS_TYPE_IWLAN";
                    case RADIO_ACCESS_TYPE_NR:
                        return "RADIO_ACCESS_TYPE_NR";
                }
                return "";
            case MESSAGE_CALL_AUDIO_CODEC:
                switch (value) {
                    case AUDIO_CODEC_EVS:
                        return "AUDIO_CODEC_EVS";
                    case AUDIO_CODEC_AMR_WB:
                        return "AUDIO_CODEC_AMR_WB";
                    case AUDIO_CODEC_AMR_NB:
                        return "AUDIO_CODEC_AMR_NB";
                }
                return "";
            case MESSAGE_DEVICE_BATTERY_STATE:
                switch (value) {
                    case BATTERY_STATE_LOW:
                        return "BATTERY_STATE_LOW";
                    case BATTERY_STATE_GOOD:
                        return "BATTERY_STATE_GOOD";
                    case BATTERY_STATE_CHARGING:
                        return "BATTERY_STATE_CHARGING";
                }
                return "";
            case MESSAGE_DEVICE_NETWORK_COVERAGE:
                switch (value) {
                    case COVERAGE_POOR:
                        return "COVERAGE_POOR";
                    case COVERAGE_GOOD:
                        return "COVERAGE_GOOD";
                }
                return "";
        }
        return "";
    }

    /**
     * Test method used to force a transport type to be the active transport.
     * @param transport The transport to activate.
     */
    public void setTransportActive(@NonNull String transport) {
        Optional<TransportProtocol> tp = mTransportProtocols.stream()
                .filter(t -> t.getClass().getSimpleName().equals(transport))
                .findFirst();
        if (!tp.isPresent()) {
            Log.w(this, "setTransportActive: %s is not a valid transport.");
            return;
        }

        mTransportProtocols.stream()
                .filter(p -> p != tp.get())
                .forEach(t -> t.forceNotNegotiated());
        tp.get().forceNegotiated();
        mActiveTransport = tp.get();
        mIsNegotiated = true;
        Log.i(this, "setTransportActive: %s has been forced active.", transport);
    }

    /**
     * @return the list of {@link TransportProtocol} which are configured at the current time.
     */
    public @NonNull List<TransportProtocol> getTransportProtocols() {
        return mTransportProtocols;
    }
}
