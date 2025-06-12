/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.ondevicepersonalization.internal.util;

import android.util.Log;

import java.util.Locale;

/**
 * Logger factory to create logger for logging to logcat with the various logcat tags.
 *
 * @hide
 */
public class LoggerFactory {
    // TODO(b/286404204): Add additional log tags
    public static final String TAG = "ondevicepersonalization";

    private static final Logger sLogger = new Logger(TAG);

    public static Logger getLogger() {
        return sLogger;
    }

    /**
     * Logger for logging to logcat with the various logcat tags.
     *
     * @hide
     */
    public static class Logger {
        private final String mTag;

        private Logger(String mTag) {
            this.mTag = mTag;
        }

        /** Log the message as VERBOSE. Return The number of bytes written. */
        public int v(String msg) {
            if (Log.isLoggable(mTag, Log.VERBOSE)) {
                return Log.v(mTag, msg);
            }
            return 0;
        }

        /** Log the message as VERBOSE. Return The number of bytes written. */
        public int v(String format, Object... params) {
            if (Log.isLoggable(mTag, Log.VERBOSE)) {
                String msg = format(format, params);
                return Log.v(mTag, msg);
            }
            return 0;
        }

        /** Log the message as DEBUG. Return The number of bytes written. */
        public int d(String msg) {
            if (Log.isLoggable(mTag, Log.DEBUG)) {
                return Log.d(mTag, msg);
            }
            return 0;
        }

        /** Log the message as DEBUG. Return The number of bytes written. */
        public int d(String format, Object... params) {
            if (Log.isLoggable(mTag, Log.DEBUG)) {
                String msg = format(format, params);
                return Log.d(mTag, msg);
            }
            return 0;
        }

        /** Log the message as DEBUG. Return The number of bytes written. */
        public int d(Throwable tr, String format, Object... params) {
            if (Log.isLoggable(mTag, Log.DEBUG)) {
                String msg = format(format, params);
                return Log.d(mTag, msg, tr);
            }
            return 0;
        }

        /** Log the message as INFO. Return The number of bytes written. */
        public int i(String msg) {
            if (Log.isLoggable(mTag, Log.INFO)) {
                return Log.i(mTag, msg);
            }
            return 0;
        }

        /** Log the message as INFO. Return The number of bytes written */
        public int i(String format, Object... params) {
            if (Log.isLoggable(mTag, Log.INFO)) {
                String msg = format(format, params);
                return Log.i(mTag, msg);
            }
            return 0;
        }

        /** Log the message as ERROR. Return The number of bytes written */
        public int w(String msg) {
            if (Log.isLoggable(mTag, Log.WARN)) {
                return Log.w(mTag, msg);
            }
            return 0;
        }

        /** Log the message as WARNING. Return The number of bytes written */
        public int w(String format, Object... params) {
            if (Log.isLoggable(mTag, Log.WARN)) {
                String msg = format(format, params);
                return Log.w(mTag, msg);
            }
            return 0;
        }

        /** Log the message as ERROR. Return The number of bytes written */
        public int e(String msg) {
            if (Log.isLoggable(mTag, Log.ERROR)) {
                return Log.e(mTag, msg);
            }
            return 0;
        }

        /** Log the message as ERROR. Return The number of bytes written */
        public int e(String format, Object... params) {
            if (Log.isLoggable(mTag, Log.ERROR)) {
                String msg = format(format, params);
                return Log.e(mTag, msg);
            }
            return 0;
        }

        /** Log the message as ERROR. Return The number of bytes written */
        public int e(Throwable tr, String msg) {
            if (Log.isLoggable(mTag, Log.ERROR)) {
                if (Log.isLoggable(mTag, Log.DEBUG)) {
                    return Log.e(mTag, msg, tr);
                } else {
                    // If not DEBUG level, only print the throwable type and message.
                    msg = msg + ": " + tr;
                    return Log.e(mTag, msg);
                }
            }
            return 0;
        }

        /** Log the message as ERROR. Return The number of bytes written */
        public int e(Throwable tr, String format, Object... params) {
            return Log.isLoggable(mTag, Log.ERROR) ? e(tr, format(format, params)) : 0;
        }

        /** Log the message as WARNING. Return The number of bytes written */
        public int w(Throwable tr, String format, Object... params) {
            if (Log.isLoggable(mTag, Log.WARN)) {
                if (Log.isLoggable(mTag, Log.DEBUG)) {
                    String msg = format(format, params);
                    return Log.w(mTag, msg, tr);
                } else {
                    // If not DEBUG level, only print the throwable type and message.
                    String msg = format(format, params) + ": " + tr;
                    return Log.w(mTag, msg);
                }
            }
            return 0;
        }

        /**
         * Logs the message as DEBUG and returns the number of bytes written.
         *
         * @deprecated This method is an overload for safety; please use {@link #d(Throwable,
         *     String, Object...)} instead.
         */
        @Deprecated
        public int d(String msg, Throwable tr) {
            return d(tr, msg);
        }

        /**
         * Logs the message as WARNING and returns the number of bytes written.
         *
         * @deprecated This method is an overload for safety; please use {@link #w(Throwable,
         *     String, Object...)} instead.
         */
        @Deprecated
        public int w(String msg, Throwable tr) {
            return w(tr, msg);
        }

        /**
         * Logs the message as ERROR and returns the number of bytes written.
         *
         * @deprecated This method is an overload for safety; please use {@link #e(Throwable,
         *     String)} or {@link #e(Throwable, String, Object...)} instead.
         */
        @Deprecated
        public int e(String msg, Throwable tr) {
            return e(tr, msg);
        }

        private static String format(String format, Object... args) {
            return String.format(Locale.US, format, args);
        }
    }
}
