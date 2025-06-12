/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.vibrator;

import android.util.Log;

class VibratorDebugUtils {

    /**
     * Checks if debugging is enabled for the specified tag or globally.
     *
     * <p>To enable debugging:<br>
     * {@code adb shell setprop persist.log.tag.Vibrator_All DEBUG}<br>
     * To disable debugging:<br>
     * {@code adb shell setprop persist.log.tag.Vibrator_All \"\" }
     *
     * @param tag The tag to check for debugging. Use the tag name from the calling class.
     * @return True if debugging is enabled for the tag or globally (Vibrator_All), false otherwise.
     */
    public static boolean isDebuggable(String tag) {
        return Log.isLoggable(tag, Log.DEBUG) || Log.isLoggable("Vibrator_All", Log.DEBUG);
    }
}
