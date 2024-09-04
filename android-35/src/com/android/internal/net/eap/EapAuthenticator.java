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

import android.content.Context;
import android.net.eap.EapSessionConfig;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapResponse;
import com.android.internal.net.eap.EapResult.EapSuccess;
import com.android.internal.net.eap.statemachine.EapStateMachine;
import com.android.internal.net.utils.Log;

import java.security.SecureRandom;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * EapAuthenticator represents an EAP peer implementation.
 *
 * @see <a href="https://tools.ietf.org/html/rfc3748#section-4">RFC 3748, Extensible Authentication
 * Protocol (EAP)</a>
 */
public class EapAuthenticator extends Handler {
    private static final String EAP_TAG = "EAP";
    private static final boolean LOG_SENSITIVE = false;
    public static final Log LOG = new Log(EAP_TAG, LOG_SENSITIVE);

    private static final String TAG = EapAuthenticator.class.getSimpleName();
    private static final long DEFAULT_TIMEOUT_MILLIS = 7000L;

    private final Executor mWorkerPool;
    private final EapStateMachine mStateMachine;
    private final IEapCallback mCb;
    private final long mTimeoutMillis;
    private boolean mCallbackFired = false;

    /**
     * Constructor for EapAuthenticator.
     *
     * @param eapContext the context of this EapAuthenticator
     * @param cb IEapCallback for callbacks to the client
     * @param eapSessionConfig Configuration for an EapAuthenticator
     * @hide
     */
    public EapAuthenticator(
            EapContext eapContext, IEapCallback cb, EapSessionConfig eapSessionConfig) {
        this(
                eapContext.getLooper(),
                cb,
                new EapStateMachine(
                        eapContext.getContext(),
                        eapSessionConfig,
                        createNewRandomIfNull(eapContext.getRandomnessFactory().getRandom())),
                Executors.newSingleThreadExecutor(),
                DEFAULT_TIMEOUT_MILLIS);
    }

    @VisibleForTesting
    EapAuthenticator(
            Looper looper,
            IEapCallback cb,
            EapStateMachine eapStateMachine,
            Executor executor,
            long timeoutMillis) {
        super(looper);

        mCb = cb;
        mStateMachine = eapStateMachine;
        mWorkerPool = executor;
        mTimeoutMillis = timeoutMillis;
    }

    private static SecureRandom createNewRandomIfNull(SecureRandom random) {
        return random == null ? new SecureRandom() : random;
    }

    /** SecureRandom factory for EAP */
    public interface EapRandomFactory {
        /** Returns a SecureRandom instance */
        SecureRandom getRandom();
    }

    /** EapContext provides interface to retrieve context information for this EapAuthenticator */
    public interface EapContext {
        /** Gets the Looper */
        Looper getLooper();
        /** Gets the Context */
        Context getContext();
        /** Gets the EapRandomFactory which will control if the EapAuthenticator is in test mode */
        EapRandomFactory getRandomnessFactory();
    }

    @Override
    public void handleMessage(Message msg) {
        // No messages processed here. Only runnables. Drop all messages.
    }

    /**
     * Processes the given msgBytes within the context of the current EAP Session.
     *
     * <p>If the given message is successfully processed, the relevant {@link IEapCallback} function
     * is used. Otherwise, {@link IEapCallback#onError(Throwable)} is called.
     *
     * @param msgBytes the byte-array encoded EAP message to be processed
     */
    public void processEapMessage(byte[] msgBytes) {
        // reset
        mCallbackFired = false;

        postDelayed(
                () -> {
                    if (!mCallbackFired) {
                        // Fire failed callback
                        mCallbackFired = true;
                        LOG.e(TAG, "Timeout occurred in EapStateMachine");
                        mCb.onError(new TimeoutException("Timeout while processing message"));
                    }
                },
                EapAuthenticator.this,
                mTimeoutMillis);

        // proxy to worker thread for async processing
        mWorkerPool.execute(
                () -> {
                    // Any unhandled exceptions within the state machine are caught here to make
                    // sure that the caller does not wait for the full timeout duration before being
                    // notified of a failure.
                    EapResult processResponse;
                    try {
                        processResponse = mStateMachine.process(msgBytes);
                    } catch (Exception ex) {
                        LOG.e(TAG, "Exception thrown while processing message", ex);
                        processResponse = new EapError(ex);
                    }

                    final EapResult finalProcessResponse = processResponse;
                    EapAuthenticator.this.post(
                            () -> {
                                // No synchronization needed, since Handler serializes
                                if (!mCallbackFired) {
                                    LOG.i(
                                            TAG,
                                            "EapStateMachine returned "
                                                    + finalProcessResponse
                                                            .getClass()
                                                            .getSimpleName());

                                    if (finalProcessResponse instanceof EapResponse) {
                                        mCb.onResponse(
                                                ((EapResponse) finalProcessResponse).packet,
                                                ((EapResponse) finalProcessResponse).flagMask);
                                    } else if (finalProcessResponse instanceof EapError) {
                                        EapError eapError = (EapError) finalProcessResponse;
                                        LOG.e(
                                                TAG,
                                                "EapError returned with cause=" + eapError.cause);
                                        mCb.onError(eapError.cause);
                                    } else if (finalProcessResponse instanceof EapSuccess) {
                                        EapSuccess eapSuccess = (EapSuccess) finalProcessResponse;
                                        LOG.d(
                                                TAG,
                                                "EapSuccess with"
                                                        + " MSK="
                                                        + LOG.pii(eapSuccess.msk)
                                                        + " EMSK="
                                                        + LOG.pii(eapSuccess.emsk));
                                        mCb.onSuccess(
                                                eapSuccess.msk,
                                                eapSuccess.emsk,
                                                eapSuccess.getEapInfo());
                                    } else { // finalProcessResponse instanceof EapFailure
                                        mCb.onFail();
                                    }

                                    mCallbackFired = true;

                                    // Ensure delayed timeout runnable does not fire
                                    EapAuthenticator.this.removeCallbacksAndMessages(
                                            EapAuthenticator.this);
                                }
                            });
                });
    }
}
