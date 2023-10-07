/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.net.module.util.async;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * Manages Async IO files and scheduled alarms, and executes all related callbacks
 * in its own thread.
 *
 * All callbacks of AsyncFile, Alarm and EventManager will execute on EventManager's thread.
 *
 * Methods of this interface can be called from any thread.
 *
 * @hide
 */
public interface EventManager extends Executor {
    /**
     * Represents a scheduled alarm, allowing caller to attempt to cancel that alarm
     * before it executes.
     *
     * @hide
     */
    public interface Alarm {
        /** @hide */
        public interface Listener {
            void onAlarm(Alarm alarm, long elapsedTimeMs);
            void onAlarmCancelled(Alarm alarm);
        }

        /**
         * Attempts to cancel this alarm. Note that this request is inherently
         * racy if executed close to the alarm's expiration time.
         */
        void cancel();
    }

    /**
     * Requests EventManager to manage the given file.
     *
     * The file descriptors are not cloned, and EventManager takes ownership of all files passed.
     *
     * No event callbacks are enabled by this method.
     */
    AsyncFile registerFile(FileHandle fileHandle, AsyncFile.Listener listener) throws IOException;

    /**
     * Schedules Alarm with the given timeout.
     *
     * Timeout of zero can be used for immediate execution.
     */
    Alarm scheduleAlarm(long timeout, Alarm.Listener callback);

    /** Schedules Runnable for immediate execution. */
    @Override
    void execute(Runnable callback);

    /** Throws a runtime exception if the caller is not executing on this EventManager's thread. */
    void assertInThread();
}
