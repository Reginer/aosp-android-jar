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

package android.net.wifi.usd;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.net.wifi.flags.Flags;
import android.os.Build;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Base class for USD session events callbacks. Should be extended by applications wanting
 * notifications. The callbacks are set when a publish or subscribe session is created.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public abstract class SessionCallback {
    /**
     * Failure code
     *
     * @hide
     */
    @IntDef({FAILURE_UNKNOWN, FAILURE_TIMEOUT, FAILURE_NOT_AVAILABLE, FAILURE_MAX_SESSIONS_REACHED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FailureCode {
    }

    /**
     * Failure is unknown.
     */
    public static final int FAILURE_UNKNOWN = 0;

    /**
     * Failure due to timeout in the requested operation.
     */
    public static final int FAILURE_TIMEOUT = 1;

    /**
     * Failure due to the requested operation is not available currently.
     */
    public static final int FAILURE_NOT_AVAILABLE = 2;
    /**
     * Failure due to the maximum session reached. Maximum number of publish and subscribe sessions
     * are limited by  {@link Characteristics#getMaxNumberOfPublishSessions()} and
     * {@link Characteristics#getMaxNumberOfSubscribeSessions()} respectively.
     */
    public static final int FAILURE_MAX_SESSIONS_REACHED = 3;


    /**
     * Termination reason code
     *
     * @hide
     */
    @IntDef({TERMINATION_REASON_UNKNOWN, TERMINATION_REASON_NOT_AVAILABLE,
            TERMINATION_REASON_USER_INITIATED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TerminationReasonCode {
    }

    /**
     * Termination due to unknown reason.
     */
    public static final int TERMINATION_REASON_UNKNOWN = 0;

    /**
     * Termination due the USD session is not available.
     */
    public static final int TERMINATION_REASON_NOT_AVAILABLE = 1;

    /**
     * Termination due to user initiated {@link PublishSession#cancel()} or
     * {@link SubscribeSession#cancel()}
     */
    public static final int TERMINATION_REASON_USER_INITIATED = 2;

    /**
     * Called when a publish or subscribe session terminates. Termination may be due to
     * <ul>
     * <li> user request e.g. {@link SubscribeSession#cancel()} or {@link PublishSession#cancel()}
     * <li> application expiration e.g. {@link PublishConfig.Builder#setTtlSeconds(int)} or
     * {@link SubscribeConfig.Builder#setTtlSeconds(int)}.
     * </ul>
     * @param reason reason code as in {@code TERMINATION_REASON_*}
     */
    public void onSessionTerminated(@TerminationReasonCode int reason) {
    }

    /**
     * Called when a message is received from another USD peer.
     *
     * @param peerId    an identifier of the remote peer
     * @param message a byte array containing the message.
     */
    public void onMessageReceived(int peerId, @Nullable byte[] message) {
    }
}
