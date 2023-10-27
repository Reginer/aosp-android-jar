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

package android.app.sdksandbox;

import android.util.Log;

/**
 * Utility class for logging behind {@link Log#isLoggable} check.
 *
 * <p>Logging can be enabled by running {@code adb shell setprop persist.log.tag.SDK_SANDBOX DEBUG}
 * or individual tag used while logging with {@link LogUtil}.
 *
 * @hide
 */
public class LogUtil {

    public static final String GUARD_TAG = "SDK_SANDBOX";

    /** Log the message as VERBOSE. Return The number of bytes written. */
    public static int v(String tag, String msg) {
        if (Log.isLoggable(GUARD_TAG, Log.VERBOSE) || Log.isLoggable(tag, Log.VERBOSE)) {
            return Log.v(tag, msg);
        }
        return 0;
    }

    /** Log the message as DEBUG. Return The number of bytes written. */
    public static int d(String tag, String msg) {
        if (Log.isLoggable(GUARD_TAG, Log.DEBUG) || Log.isLoggable(tag, Log.DEBUG)) {
            return Log.d(tag, msg);
        }
        return 0;
    }
}
