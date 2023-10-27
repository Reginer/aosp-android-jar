/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.net.eap;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.statemachine.EapMethodStateMachine.MIN_EMSK_LEN_BYTES;
import static com.android.internal.net.eap.statemachine.EapMethodStateMachine.MIN_MSK_LEN_BYTES;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.eap.EapInfo;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.exceptions.InvalidEapResponseException;
import com.android.internal.net.eap.message.EapMessage;

/**
 * EapResult represents the return type R for a process operation within the EapStateMachine.
 */
public abstract class EapResult {
    /**
     * EapSuccess represents a success response from the EapStateMachine.
     *
     * @see <a href="https://tools.ietf.org/html/rfc3748">RFC 3748, Extensible Authentication
     * Protocol (EAP)</a>
     */
    public static class EapSuccess extends EapResult {
        private static final String TAG = EapSuccess.class.getSimpleName();

        public final byte[] msk;
        public final byte[] emsk;
        public final EapInfo mEapInfo;

        public EapSuccess(@NonNull byte[] msk, @NonNull byte[] emsk) {
            this(msk, emsk, null);
        }

        public EapSuccess(@NonNull byte[] msk, @NonNull byte[] emsk, @Nullable EapInfo eapInfo) {
            if (msk == null || emsk == null) {
                throw new IllegalArgumentException("msk and emsk must not be null");
            }
            if (msk.length < MIN_MSK_LEN_BYTES || emsk.length < MIN_EMSK_LEN_BYTES) {
                LOG.wtf(
                        TAG,
                        "MSK or EMSK does not meet the required key length: MSK="
                                + LOG.pii(msk)
                                + " EMSK="
                                + LOG.pii(emsk));
            }
            this.msk = msk;
            this.emsk = emsk;
            this.mEapInfo = eapInfo;
        }

        @Nullable
        public EapInfo getEapInfo() {
            return mEapInfo;
        }
    }

    /**
     * EapFailure represents a failure response from the EapStateMachine.
     *
     * @see <a href="https://tools.ietf.org/html/rfc3748">RFC 3748, Extensible Authentication
     * Protocol (EAP)</a>
     */
    public static class EapFailure extends EapResult {}

    /**
     * EapResponse represents an outgoing message from the EapStateMachine.
     *
     * @see <a href="https://tools.ietf.org/html/rfc3748">RFC 3748, Extensible Authentication
     * Protocol (EAP)</a>
     */
    public static class EapResponse extends EapResult {
        public final byte[] packet;
        // holds bitmask representing multiple flags
        public final int flagMask;

        // flags to capture additional high level state information
        @IntDef({RESPONSE_FLAG_EAP_AKA_SERVER_AUTHENTICATED})
        public @interface EapResponseFlag {}

        public static final int RESPONSE_FLAG_EAP_AKA_SERVER_AUTHENTICATED = 0;

        @VisibleForTesting
        protected EapResponse(byte[] packet, @EapResponseFlag int[] flagsToAdd) {
            this.packet = packet;
            this.flagMask = createFlagMask(flagsToAdd);
        }

        /**
         * Constructs and returns an EapResult for the given EapMessage.
         *
         * <p>If the given EapMessage is not of type EAP-Response, an EapError object will be
         * returned.
         *
         * @param message the EapMessage to be encoded in the EapResponse instance.
         * @param flagsToAdd list of EAP auth state related flags
         * @return an EapResponse instance for the given message. If message.eapCode != {@link
         *     EapMessage#EAP_CODE_RESPONSE}, an EapError instance is returned.
         */
        public static EapResult getEapResponse(
                @NonNull EapMessage message, @EapResponseFlag int[] flagsToAdd) {
            if (message == null) {
                throw new IllegalArgumentException("EapMessage should not be null");
            } else if (message.eapCode != EapMessage.EAP_CODE_RESPONSE) {
                return new EapError(new InvalidEapResponseException(
                        "Cannot construct an EapResult from a non-EAP-Response message"));
            }

            return new EapResponse(message.encode(), flagsToAdd);
        }

        /**
         * Constructs and returns an EapResult for the given EapMessage.
         *
         * <p>If the given EapMessage is not of type EAP-Response, an EapError object will be
         * returned.
         *
         * @param message the EapMessage to be encoded in the EapResponse instance.
         * @return an EapResponse instance for the given message. If message.eapCode != {@link
         *     EapMessage#EAP_CODE_RESPONSE}, an EapError instance is returned.
         */
        public static EapResult getEapResponse(@NonNull EapMessage message) {
            return getEapResponse(message, null);
        }

        /**
         * Utility that clients should use to check presence of a EapResponseFlag in the bitmask
         * received in IEapCallback.onResponse()
         *
         * @param flagMask flags that client received in {@link IEapCallback.onResponse()}
         * @param flagToCheck flag to test. See {@link EapResult.EapResponse.EapResponseFlag}
         * @return returns true if flagToCheck is present
         */
        public static boolean hasFlag(int flagMask, @EapResponseFlag int flagToCheck) {
            return ((flagMask & (1 << flagToCheck)) != 0);
        }

        private static int createFlagMask(@EapResponseFlag int[] flagsToAdd) {
            int flagMask = 0;
            if ((flagsToAdd != null) && flagsToAdd.length > 0) {
                for (int flag : flagsToAdd) {
                    flagMask |= (1 << flag);
                }
            }
            return flagMask;
        }
    }

    /**
     * EapError represents an error that occurred in the EapStateMachine.
     *
     * @see <a href="https://tools.ietf.org/html/rfc3748">RFC 3748, Extensible Authentication
     * Protocol (EAP)</a>
     */
    public static class EapError extends EapResult {
        public final Exception cause;

        /**
         * Constructs an EapError instance for the given cause.
         *
         * @param cause the Exception that caused the EapError to be returned from the
         *     EapStateMachine
         */
        public EapError(Exception cause) {
            this.cause = cause;
        }
    }
}
