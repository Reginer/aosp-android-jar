/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.telephony.metrics;

import android.os.SystemClock;

import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession;

import java.util.ArrayDeque;
import java.util.Deque;

/** The ongoing Call session */
public class InProgressCallSession {

    /** Maximum events stored in the session */
    private static final int MAX_EVENTS = 300;

    /** Phone id */
    public final int phoneId;

    /** Call session events */
    public final Deque<TelephonyCallSession.Event> events;

    /** Call session starting system time in minute */
    public final int startSystemTimeMin;

    /** Call session starting elapsed time in milliseconds */
    public final long startElapsedTimeMs;

    /** The last event's time */
    private long mLastElapsedTimeMs;

    /** Indicating events dropped */
    private boolean mEventsDropped = false;

    /** Last known phone state */
    private int mLastKnownPhoneState;

    /** Check if events dropped */
    public boolean isEventsDropped() { return mEventsDropped; }

    /**
     * Constructor
     *
     * @param phoneId Phone id
     */
    public InProgressCallSession(int phoneId) {
        this.phoneId = phoneId;
        events = new ArrayDeque<>();
        // Save session start with lowered precision due to the privacy requirements
        startSystemTimeMin = TelephonyMetrics.roundSessionStart(System.currentTimeMillis());
        startElapsedTimeMs = SystemClock.elapsedRealtime();
        mLastElapsedTimeMs = startElapsedTimeMs;
    }

    /**
     * Add event
     *
     * @param builder Event builder
     */
    public void addEvent(CallSessionEventBuilder builder) {
        addEvent(SystemClock.elapsedRealtime(), builder);
    }

    /**
     * Add event
     *
     * @param timestamp Timestamp to be recoded with the event
     * @param builder Event builder
     */
    synchronized public void addEvent(long timestamp, CallSessionEventBuilder builder) {
        if (events.size() >= MAX_EVENTS) {
            events.removeFirst();
            mEventsDropped = true;
        }

        builder.setDelay(TelephonyMetrics.toPrivacyFuzzedTimeInterval(
                mLastElapsedTimeMs, timestamp));

        events.add(builder.build());
        mLastElapsedTimeMs = timestamp;
    }

    /**
     * Check if the Call Session contains CS calls
     * @return true if there are CS calls in the call list
     */
    public synchronized boolean containsCsCalls() {
        for (TelephonyCallSession.Event event : events) {
            if (event.type == TelephonyCallSession.Event.Type.RIL_CALL_LIST_CHANGED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set Phone State
     * @param state
     */
    public void setLastKnownPhoneState(int state) {
        mLastKnownPhoneState = state;
    }

    /**
     * Checks if Phone is in Idle state
     * @return true if device is in Phone is idle state.
     *
     */
    public boolean isPhoneIdle() {
        return (mLastKnownPhoneState == TelephonyCallSession.Event.PhoneState.STATE_IDLE);
    }
}
