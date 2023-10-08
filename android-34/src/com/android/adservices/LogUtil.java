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

package com.android.adservices;

import android.util.Log;

import java.util.Locale;

/**
 * Utility class for logging to logcat with the "AdServices" tag.
 *
 * @hide
 */
public class LogUtil {

    public static final String TAG = "adservices";

    /** Log the message as VERBOSE. Return The number of bytes written. */
    public static int v(String msg) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            return Log.v(TAG, msg);
        }
        return 0;
    }

    /** Log the message as VERBOSE. Return The number of bytes written. */
    public static int v(String format, Object... params) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            String msg = format(format, params);
            return Log.v(TAG, msg);
        }
        return 0;
    }

    /** Log the message as DEBUG. Return The number of bytes written. */
    public static int d(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            return Log.d(TAG, msg);
        }
        return 0;
    }

    /** Log the message as DEBUG. Return The number of bytes written. */
    public static int d(String format, Object... params) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            String msg = format(format, params);
            return Log.d(TAG, msg);
        }
        return 0;
    }

    /** Log the message as DEBUG. Return The number of bytes written. */
    public static int d(Throwable tr, String format, Object... params) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            String msg = format(format, params);
            return Log.d(TAG, msg, tr);
        }
        return 0;
    }

    /** Log the message as INFO. Return The number of bytes written. */
    public static int i(String msg) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            return Log.i(TAG, msg);
        }
        return 0;
    }

    /** Log the message as INFO. Return The number of bytes written */
    public static int i(String format, Object... params) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            String msg = format(format, params);
            return Log.i(TAG, msg);
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written */
    public static int w(String msg) {
        if (Log.isLoggable(TAG, Log.WARN)) {
            return Log.w(TAG, msg);
        }
        return 0;
    }

    /** Log the message as WARNING. Return The number of bytes written */
    public static int w(String format, Object... params) {
        if (Log.isLoggable(TAG, Log.WARN)) {
            String msg = format(format, params);
            return Log.w(TAG, msg);
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written */
    public static int e(String msg) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            return Log.e(TAG, msg);
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written */
    public static int e(String format, Object... params) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            String msg = format(format, params);
            return Log.e(TAG, msg);
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written */
    public static int e(Throwable tr, String msg) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                return Log.e(TAG, msg, tr);
            } else {
                // If not DEBUG level, only print the throwable type and message.
                msg = msg + ": " + tr;
                return Log.e(TAG, msg);
            }
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written */
    public static int e(Throwable tr, String format, Object... params) {
        return Log.isLoggable(TAG, Log.ERROR) ? e(tr, format(format, params)) : 0;
    }

    /** Log the message as WARNING. Return The number of bytes written */
    public static int w(Throwable tr, String format, Object... params) {
        if (Log.isLoggable(TAG, Log.WARN)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                String msg = format(format, params);
                return Log.w(TAG, msg, tr);
            } else {
                // If not DEBUG level, only print the throwable type and message.
                String msg = format(format, params) + ": " + tr;
                return Log.w(TAG, msg);
            }
        }
        return 0;
    }

    /**
     * Logs the message as DEBUG and returns the number of bytes written.
     *
     * @deprecated This method is an overload for safety; please use {@link #d(Throwable, String,
     *     Object...)} instead.
     */
    @Deprecated
    public static int d(String msg, Throwable tr) {
        return d(tr, msg);
    }

    /**
     * Logs the message as WARNING and returns the number of bytes written.
     *
     * @deprecated This method is an overload for safety; please use {@link #w(Throwable, String,
     *     Object...)} instead.
     */
    @Deprecated
    public static int w(String msg, Throwable tr) {
        return w(tr, msg);
    }

    /**
     * Logs the message as ERROR and returns the number of bytes written.
     *
     * @deprecated This method is an overload for safety; please use {@link #e(Throwable, String)}
     *     or {@link #e(Throwable, String, Object...)} instead.
     */
    @Deprecated
    public static int e(String msg, Throwable tr) {
        return e(tr, msg);
    }

    private static String format(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }
}
