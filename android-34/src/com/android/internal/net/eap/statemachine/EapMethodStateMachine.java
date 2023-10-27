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

package com.android.internal.net.eap.statemachine;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.EapData.EAP_NOTIFICATION;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_FAILURE;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_SUCCESS;

import android.annotation.Nullable;
import android.net.eap.EapSessionConfig.EapMethodConfig.EapMethod;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapFailure;
import com.android.internal.net.eap.exceptions.EapInvalidRequestException;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.utils.SimpleStateMachine;

/**
 * EapMethodStateMachine is an abstract class representing a state machine for EAP Method
 * implementations.
 */
public abstract class EapMethodStateMachine extends SimpleStateMachine<EapMessage, EapResult> {
    // Minimum key lengths specified in RFC 3748#1.2
    public static final int MIN_MSK_LEN_BYTES = 64;
    public static final int MIN_EMSK_LEN_BYTES = 64;

    /*
     * Used for transitioning to a state where EAP-Failure messages are expected next. This
     * allows all EAP methods to easily transition to a pre-failure state in the event of errors,
     * failed authentication, etc.
     */
    protected boolean mIsExpectingEapFailure = false;

    /**
     * Returns the EAP Method type for this EapMethodStateMachine implementation.
     *
     * @return the IANA value for the EAP Method represented by this EapMethodStateMachine
     */
    @EapMethod
    abstract int getEapMethod();

    @VisibleForTesting
    protected SimpleState getState() {
        return mState;
    }

    @VisibleForTesting
    protected void transitionTo(EapMethodState newState) {
        LOG.d(
                this.getClass().getSimpleName(),
                "Transitioning from " + mState.getClass().getSimpleName()
                        + " to " + newState.getClass().getSimpleName());
        super.transitionTo(newState);
    }

    abstract EapResult handleEapNotification(String tag, EapMessage message);

    protected abstract class EapMethodState extends SimpleState {
        /**
         * Handles premature EAP-Success and EAP-Failure messages, as well as EAP-Notification
         * messages.
         *
         * @param tag the String logging tag to be used while handing message
         * @param message the EapMessage to be checked for early Success/Failure/Notification
         *                messages
         * @return the EapResult generated from handling the give EapMessage, or null if the message
         * Type matches that of the current EAP method
         */
        @Nullable
        EapResult handleEapSuccessFailureNotification(String tag, EapMessage message) {
            if (message.eapCode == EAP_CODE_SUCCESS) {
                // EAP-SUCCESS is required to be the last EAP message sent during the EAP protocol,
                // so receiving a premature SUCCESS message is an unrecoverable error.
                return new EapError(
                        new EapInvalidRequestException(
                                "Received an EAP-Success in the " + tag));
            } else if (message.eapCode == EAP_CODE_FAILURE) {
                transitionTo(new FinalState());
                return new EapFailure();
            } else if (message.eapData.eapType == EAP_NOTIFICATION) {
                return handleEapNotification(tag, message);
            } else if (mIsExpectingEapFailure) {
                // Expecting EAP-Failure message. Didn't receive EAP-Failure or EAP-Notification,
                // so log and return EAP-Error.
                LOG.e(tag, "Expecting EAP-Failure. Received non-Failure/Notification message");
                return new EapError(
                        new EapInvalidRequestException(
                                "Expecting EAP-Failure. Received received "
                                        + message.eapData.eapType));
            } else if (message.eapData.eapType != getEapMethod()) {
                return new EapError(new EapInvalidRequestException(
                        "Expected EAP Type " + getEapMethod()
                                + ", received " + message.eapData.eapType));
            }

            return null;
        }
    }

    protected class FinalState extends EapMethodState {
        @Override
        public EapResult process(EapMessage msg) {
            return new EapError(
                    new IllegalStateException("Attempting to process from a FinalState"));
        }
    }
}
