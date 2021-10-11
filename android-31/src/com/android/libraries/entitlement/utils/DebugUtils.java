/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.libraries.entitlement.utils;

import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

/** Provides API for debugging and not allow to debug on user build. */
public final class DebugUtils {
    private static final String TAG = "ServiceEntitlement";

    private static final String PROP_PII_LOGGABLE = "dbg.se.pii_loggable";
    private static final String BUILD_TYPE_USER = "user";

    private DebugUtils() {}

    /** Logs PII data if allowed. */
    public static void logPii(String message) {
        if (isPiiLoggable()) {
            Log.d(TAG, message);
        }
    }

    private static boolean isDebugBuild() {
        return !BUILD_TYPE_USER.equals(Build.TYPE);
    }

    private static boolean isPiiLoggable() {
        if (!isDebugBuild()) {
            return false;
        }

        return SystemProperties.getBoolean(PROP_PII_LOGGABLE, false);
    }
}
