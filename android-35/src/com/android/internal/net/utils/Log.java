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

package com.android.internal.net.utils;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Locale;
import java.util.Objects;

/**
 * Manages logging for all IKE packages. Wraps Android's Log class to prevent leakage of PII.
 */
public class Log {
    private static final boolean VDBG = false;

    private final String mTAG;
    private final boolean mIsVdbg;
    private final boolean mLogSensitive;

    /**
     * Constructs a Log instance configured with the given tag and logSensitive flag
     *
     * @param tag the String tag to be used for this Log's logging
     * @param logSensitive boolean flag marking whether sensitive data (PII) should be logged
     */
    public Log(String tag, boolean logSensitive) {
        this(tag, VDBG, logSensitive);
    }

    @VisibleForTesting
    Log(String tag, boolean isVdbg, boolean logSensitive) {
        this.mTAG = tag;
        this.mIsVdbg = isVdbg;
        this.mLogSensitive = logSensitive;
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#VERBOSE} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     */
    public void v(String prefix, String msg) {
        if (isLoggable(android.util.Log.VERBOSE)) {
            android.util.Log.v(mTAG, prefix + ": " + msg);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#VERBOSE} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     * @param tr an Exception to log
     */
    public void v(String prefix, String msg, Throwable tr) {
        if (isLoggable(android.util.Log.VERBOSE)) {
            android.util.Log.v(mTAG, prefix + ": " + msg, tr);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#DEBUG} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     */
    public void d(String prefix, String msg) {
        if (isLoggable(android.util.Log.DEBUG)) {
            android.util.Log.d(mTAG, prefix + ": " + msg);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#DEBUG} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     * @param tr an Exception to log
     */
    public void d(String prefix, String msg, Throwable tr) {
        if (isLoggable(android.util.Log.DEBUG)) {
            android.util.Log.d(mTAG, prefix + ": " + msg, tr);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#INFO} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     */
    public void i(String prefix, String msg) {
        if (isLoggable(android.util.Log.INFO)) {
            android.util.Log.i(mTAG, prefix + ": " + msg);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#INFO} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     * @param tr an Exception to log
     */
    public void i(String prefix, String msg, Throwable tr) {
        if (isLoggable(android.util.Log.INFO)) {
            android.util.Log.i(mTAG, prefix + ": " + msg, tr);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#WARN} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     */
    public void w(String prefix, String msg) {
        if (isLoggable(android.util.Log.WARN)) {
            android.util.Log.w(mTAG, prefix + ": " + msg);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#WARN} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     * @param tr an Exception to log
     */
    public void w(String prefix, String msg, Throwable tr) {
        if (isLoggable(android.util.Log.WARN)) {
            android.util.Log.w(mTAG, prefix + ": " + msg, tr);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#ERROR} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     */
    public void e(String prefix, String msg) {
        if (isLoggable(android.util.Log.ERROR)) {
            android.util.Log.e(mTAG, prefix + ": " + msg);
        }
    }

    /**
     * Logs the given prefix and msg Strings.
     *
     * <p>Note: Logging is only done if this instance's logging level is {@link
     * android.util.Log#ERROR} or higher.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     * @param tr an Exception to log
     */
    public void e(String prefix, String msg, Throwable tr) {
        if (isLoggable(android.util.Log.ERROR)) {
            android.util.Log.e(mTAG, prefix + ": " + msg, tr);
        }
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     */
    public void wtf(String prefix, String msg) {
        android.util.Log.wtf(mTAG, prefix + ": " + msg);
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     *
     * @param prefix the String prefix to be used for this log entry
     * @param msg the String msg to be logged
     * @param tr an Exception to log
     */
    public void wtf(String prefix, String msg, Throwable tr) {
        android.util.Log.wtf(mTAG, prefix + ": " + msg, tr);
    }

    /**
     * Returns a String-formatted version of the given PII.
     *
     * <p>Depending on the logging configurations and build-type, the returned PII may be
     * obfuscated.
     *
     * @param pii the PII to be formatted
     * @return the String-formatted version of the PII
     */
    public String pii(Object pii) {
        if (!mIsVdbg || !mLogSensitive) {
            return String.valueOf(Objects.hashCode(pii));
        } else {
            if (pii instanceof byte[]) {
                return byteArrayToHexString((byte[]) pii);
            }
            return String.valueOf(pii);
        }
    }

    /**
     * Checks whether the given logging level (defined in {@link android.util.Log}) is loggable for
     * this Log.
     *
     * @param level the logging level to be checked for being loggable
     * @return true iff level is at the configured logging level or higher
     */
    private boolean isLoggable(int level) {
        return android.util.Log.isLoggable(mTAG, level);
    }

    /**
     * Returns the hex-String representation of the given byte[].
     *
     * @param data the byte[] to be represented
     * @return the String representation of data
     */
    public static String byteArrayToHexString(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format(Locale.US, "%02X", b));
        }
        return sb.toString();
    }
}
