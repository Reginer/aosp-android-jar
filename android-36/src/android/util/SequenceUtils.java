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

package android.util;

/**
 * Utilities to manage an info change seq id to ensure the update is in sync between client and
 * system server. This should be used for info that can be updated though multiple IPC channel.
 *
 * To use it:
 * 1. The system server should store the current seq as the source of truth, with initializing to
 * {@link #getInitSeq}.
 * 2. Whenever a newer info needs to be sent to the client side, the system server should first
 * update its seq with {@link #getNextSeq}, then send the new info with the new seq to the client.
 * 3. On the client side, when receiving a new info, it should only consume it if it is not stale by
 * checking {@link #isIncomingSeqStale}.
 *
 * @hide
 */
public final class SequenceUtils {

    private SequenceUtils() {
    }

    /**
     * Returns {@code true} if the incomingSeq is stale, which means the client should not consume
     * it.
     */
    public static boolean isIncomingSeqStale(int curSeq, int incomingSeq) {
        if (curSeq == getInitSeq()) {
            // curSeq can be set to the initial seq in the following cases:
            // 1. The client process/field is newly created/recreated.
            // 2. The field is not managed by the system server, such as WindowlessWindowManager.
            // The client should always consume the incoming in these cases.
            return false;
        }
        // Convert to long for comparison.
        final long diff = (long) incomingSeq - curSeq;
        // When diff is 0, allow client to consume.
        // When there has been a sufficiently large jump, assume the sequence has wrapped around.
        return (diff < 0 && diff > Integer.MIN_VALUE) || diff > Integer.MAX_VALUE;
    }

    /** Returns the initial seq. */
    public static int getInitSeq() {
        return Integer.MIN_VALUE;
    }

    /** Returns the next seq. */
    public static int getNextSeq(int seq) {
        return seq == Integer.MAX_VALUE
                // Skip the initial seq, so that when the app process is relaunched, the incoming
                // seq from the server is always treated as newer.
                ? getInitSeq() + 1
                : ++seq;
    }
}
