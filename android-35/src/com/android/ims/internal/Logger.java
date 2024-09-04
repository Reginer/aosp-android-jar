/*
 * Copyright (c) 2015, Motorola Mobility LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of Motorola Mobility nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MOTOROLA MOBILITY LLC BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.android.ims.internal;

import java.lang.String;
import android.util.Log;

import android.text.TextUtils;

/**
 * Logger
 *
 * @hide
 */
public class Logger {

    private static boolean VERBOSE = isLoggable(android.util.Log.VERBOSE);
    private static boolean DEBUG = isLoggable(android.util.Log.DEBUG);
    private static boolean INFO = isLoggable(android.util.Log.INFO);
    private static boolean WARN = isLoggable(android.util.Log.WARN);
    private static boolean ERROR = isLoggable(android.util.Log.ERROR);

    /**
     * RCS test mode flag
     */
    private static boolean mRcsTestMode = false;

    /**
     * Log tag name
     */
    private static String TAG = "rcs";

    /**
     * Classname
     */
    private String mClassName;

    /**
     * Constructor
     *
     * @param mClassName Classname
     */
    private Logger(String tagName, String mClassName) {
        if(!TextUtils.isEmpty(tagName)) {
            TAG = tagName;
        }

        int index = mClassName.lastIndexOf('.');
        if (index != -1) {
            this.mClassName = mClassName.substring(index+1);
        } else {
            this.mClassName = mClassName;
        }
    }

    public static void setRcsTestMode(boolean test) {
        mRcsTestMode = test;
        // Reset log-ability of each mode.
        DEBUG = isLoggable(android.util.Log.DEBUG);
        INFO = isLoggable(android.util.Log.INFO);
        VERBOSE = isLoggable(android.util.Log.VERBOSE);
        WARN = isLoggable(android.util.Log.WARN);
        ERROR = isLoggable(android.util.Log.ERROR);
    }

    /**
     * Is logger activated. Reserved for future debug tool to turn on/off the log only.
     *
     * @return boolean
     */
    private boolean isActivated() {
        return true;
    }

    /**
     * Verbose trace
     *
     * @param trace Trace
     */
    public void verbose(String trace) {
        if (isActivated() && VERBOSE) {
            Log.d(TAG, "[" + mClassName +"] " + trace);
        }
    }

    /**
     * Debug trace
     *
     * @param trace Trace
     */
    public void debug(String trace) {
        if (isActivated() && DEBUG) {
            Log.d(TAG, "[" + mClassName +"] " + trace);
        }
    }

    /**
     * Debug trace
     *
     * @param trace Trace
     * @param e the exception which need to be printed.
     */
    public void debug(String trace, Throwable e) {
        if (isActivated() && DEBUG) {
            Log.d(TAG, "[" + mClassName +"] " + trace, e);
        }
    }

    /**
     * Info trace
     *
     * @param trace Trace
     */
    public void info(String trace) {
        if (isActivated() && INFO) {
            Log.i(TAG, "[" + mClassName +"] " + trace);
        }
    }

    /**
     * Warning trace
     *
     * @param trace Trace
     */
    public void warn(String trace) {
        if (isActivated() && WARN) {
            Log.w(TAG, "[" + mClassName +"] " + trace);
        }
    }

    /**
     * Error trace
     *
     * @param trace Trace
     */
    public void error(String trace) {
        if (isActivated() && ERROR) {
            Log.e(TAG, "[" + mClassName +"] " + trace);
        }
    }

    /**
     * Error trace
     *
     * @param trace Trace
     * @param e Exception
     */
    public void error(String trace, Throwable e) {
        if (isActivated() && ERROR) {
            Log.e(TAG, "[" + mClassName +"] " + trace, e);
        }
    }

    /*
     * Print the debug log and don't consider the traceLevel
     *
     * @param trace Trace
     * @param e Exception
     */
    public void print(String trace) {
        Log.i(TAG, "[" + mClassName +"] " + trace);
    }

    /**
     * Print the debug log and don't consider the traceLevel
     *
     * @param trace Trace
     * @param e Exception
     */
    public void print(String trace, Throwable e) {
        Log.i(TAG, "[" + mClassName +"] " + trace, e);
    }

    // Hide all numbers except for the last two
    public static String hidePhoneNumberPii(String number) {
        if(TextUtils.isEmpty(number) || mRcsTestMode || number.length() <= 2) {
            return number;
        }
        StringBuilder sb = new StringBuilder(number.length());
        sb.append("...*");
        sb.append(number.substring(number.length()-2, number.length()));
        return sb.toString();
    }

    /**
     * Determines if the debug level is currently loggable.
     */
    private static boolean isLoggable(int level) {
        return mRcsTestMode || android.util.Log.isLoggable(TAG, level);
    }

    /**
     * Create a static instance
     *
     * @param classname Classname
     * @return Instance
     */
    public static synchronized Logger getLogger(String tagName, String classname) {
        return new Logger(tagName, classname);
    }

    /**
     * Create a static instance
     *
     * @param classname Classname
     * @return Instance
     */
    public static synchronized Logger getLogger(String classname) {
        return new Logger(TAG, classname);
    }
}

