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

package com.android.federatedcompute.internal.util;

import android.util.Log;

import java.util.Locale;

/**
 * Logger for logging to logcat with the various logcat tags.
 *
 * @hide
 */
public final class LogUtil {
    /* Unified TAG for logging across Federated Compute Program app. */
    public static final String TAG = "federatedcompute";

    /* This is a static wrapper over standard Android logger.
     *  No instantiation assumed. */
    private LogUtil() {}

    /** Log the message as VERBOSE. Return The number of bytes written. */
    public static int v(String msg) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            return Log.v(TAG, msg);
        }
        return 0;
    }

    /** Log the message as VERBOSE. Return The number of bytes written. */
    public static int v(String tag, String msg) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            return Log.v(TAG, tag + " - " + msg);
        }
        return 0;
    }

    /** Log the message as DEBUG. Return The number of bytes written. */
    public static int d(String tag, String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            return Log.d(TAG, tag + " - " + msg);
        }
        return 0;
    }

    /** Log the message as DEBUG. Return The number of bytes written. */
    public static int d(String tag, Throwable throwable, String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            return Log.d(TAG, tag + " - " + msg, throwable);
        }
        return 0;
    }

    /** Log the message as DEBUG. Return The number of bytes written. */
    public static int d(String tag, String format, Object... params) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            String msg = format(format, params);
            return Log.d(TAG, tag + " - " + msg);
        }
        return 0;
    }

    /** Log the message as INFO. Return The number of bytes written. */
    public static int i(String tag, String format, Object... params) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            String msg = format(format, params);
            return Log.i(TAG, tag + " - " + msg);
        }
        return 0;
    }

    /** Log the message as WARN. Return The number of bytes written. */
    public static int w(String tag, String msg) {
        if (Log.isLoggable(TAG, Log.WARN)) {
            return Log.w(TAG, tag + " - " + msg);
        }
        return 0;
    }

    /** Log the message as WARN. Return The number of bytes written. */
    public static int w(String tag, Throwable throwable, String msg) {
        if (Log.isLoggable(TAG, Log.WARN)) {
            return Log.w(TAG, tag + " - " + msg, throwable);
        }
        return 0;
    }

    /** Log the message as WARN. Return The number of bytes written. */
    public static int w(String tag, String format, Object... params) {
        if (Log.isLoggable(TAG, Log.WARN)) {
            String msg = format(format, params);
            return Log.w(TAG, tag + " - " + msg);
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written. */
    public static int e(String tag, String msg) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            return Log.e(TAG, tag + " - " + msg);
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written. */
    public static int e(String tag, Throwable throwable, String msg) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            return Log.e(TAG, tag + " - " + msg, throwable);
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written. */
    public static int e(String tag, String format, Object... params) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            String msg = format(format, params);
            return Log.e(TAG, tag + " - " + msg);
        }
        return 0;
    }

    /** Log the message as ERROR. Return The number of bytes written. */
    public static int e(String tag, Throwable throwable, String format, Object... params) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
            String msg = format(format, params);
            return Log.e(TAG, tag + " - " + msg, throwable);
        }
        return 0;
    }

    private static String format(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }
}
