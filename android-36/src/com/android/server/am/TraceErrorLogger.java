/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.server.am;

import android.os.Trace;

import java.util.UUID;

/**
 * Adds a unique id to a trace.
 *
 * @hide
 */
public class TraceErrorLogger {
    private static final String COUNTER_PREFIX = "ErrorId:";
    private static final int PLACEHOLDER_VALUE = 1;

    public boolean isAddErrorIdEnabled() {
        return true;
    }

    /**
     * Generates a unique id with which to tag a trace.
     */
    public UUID generateErrorId() {
        return UUID.randomUUID();
    }

    /**
     * Pushes a counter containing a unique id and a label {@link #COUNTER_PREFIX} so that traces
     * can be uniquely identified. We also add the same id to the dropbox entry of the error, so
     * that we can join the trace and the error server-side.
     *
     * @param processName The name of the ANRing process.
     * @param pid         The pid of the ANRing process.
     * @param errorId     The unique id with which to tag the trace.
     */
    public void addProcessInfoAndErrorIdToTrace(String processName, int pid, UUID errorId) {
        Trace.traceCounter(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                COUNTER_PREFIX + processName + " " + pid + "#" + errorId.toString(),
                PLACEHOLDER_VALUE);
    }

    /**
     * Pushes a counter containing an ANR/Watchdog subject and a unique id so that the subject
     * can be uniquely identified.
     *
     * @param subject The subject to include in the trace.
     * @param errorId The unique id with which to tag the trace.
     */
    public void addSubjectToTrace(String subject, UUID errorId) {
        Trace.traceCounter(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                String.format("Subject(for ErrorId %s):%s", errorId.toString(), subject),
                PLACEHOLDER_VALUE);
    }
}
