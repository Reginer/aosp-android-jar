/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.graphics.pdf.utils;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.util.Log;

/**
 * Utility class for temporarily disabling StrictMode to do I/O on UI threads.
 *
 * @hide
 */
public class StrictModeUtils {

    private static final String TAG = "StrictModeUtils";

    /** Temporarily disable StrictMode, execute a code block. */
    public static void bypass(Runnable callback) {
        ThreadPolicy policy = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(
                    new ThreadPolicy.Builder().permitDiskReads().permitDiskWrites().build());
            callback.run();
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while running the callback", e);
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }
}
