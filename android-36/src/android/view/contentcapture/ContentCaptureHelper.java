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
package android.view.contentcapture;

import static android.view.contentcapture.ContentCaptureManager.DEVICE_CONFIG_PROPERTY_LOGGING_LEVEL;
import static android.view.contentcapture.ContentCaptureManager.LOGGING_LEVEL_DEBUG;
import static android.view.contentcapture.ContentCaptureManager.LOGGING_LEVEL_OFF;
import static android.view.contentcapture.ContentCaptureManager.LOGGING_LEVEL_VERBOSE;

import android.annotation.Nullable;
import android.os.Build;
import android.provider.DeviceConfig;
import android.util.ArraySet;
import android.util.Log;
import android.view.contentcapture.ContentCaptureManager.LoggingLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper class for this package and server's.
 *
 * @hide
 */
public final class ContentCaptureHelper {

    private static final String TAG = ContentCaptureHelper.class.getSimpleName();

    public static boolean sVerbose = false;
    public static boolean sDebug = true;

    /**
     * Used to log text that could contain PII.
     */
    @Nullable
    public static String getSanitizedString(@Nullable CharSequence text) {
        return text == null ? null : text.length() + "_chars";
    }

    /**
     * Gets the default logging level for the device.
     */
    @LoggingLevel
    public static int getDefaultLoggingLevel() {
        return Build.IS_DEBUGGABLE ? LOGGING_LEVEL_DEBUG : LOGGING_LEVEL_OFF;
    }

    /**
     * Sets the value of the static logging level constants based on device config.
     */
    public static void setLoggingLevel() {
        final int defaultLevel = getDefaultLoggingLevel();
        final int level = DeviceConfig.getInt(DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                DEVICE_CONFIG_PROPERTY_LOGGING_LEVEL, defaultLevel);
        setLoggingLevel(level);
    }

    /**
     * Sets the value of the static logging level constants based the given level.
     */
    public static void setLoggingLevel(@LoggingLevel int level) {
        Log.i(TAG, "Setting logging level to " + getLoggingLevelAsString(level));
        sVerbose = sDebug = false;
        switch (level) {
            case LOGGING_LEVEL_VERBOSE:
                sVerbose = true;
                // fall through
            case LOGGING_LEVEL_DEBUG:
                sDebug = true;
                return;
            case LOGGING_LEVEL_OFF:
                // You log nothing, Jon Snow!
                return;
            default:
                Log.w(TAG, "setLoggingLevel(): invalud level: " + level);
        }
    }

    /**
     * Gets a user-friendly value for a content capture logging level.
     */
    public static String getLoggingLevelAsString(@LoggingLevel int level) {
        switch (level) {
            case LOGGING_LEVEL_OFF:
                return "OFF";
            case LOGGING_LEVEL_DEBUG:
                return "DEBUG";
            case LOGGING_LEVEL_VERBOSE:
                return "VERBOSE";
            default:
                return "UNKNOWN-" + level;
        }
    }

    /**
     * Converts a set to a list.
     */
    @Nullable
    public static <T> ArrayList<T> toList(@Nullable Set<T> set) {
        return set == null ? null : new ArrayList<T>(set);
    }

    /**
     * Converts a list to a set.
     */
    @Nullable
    public static <T> ArraySet<T> toSet(@Nullable List<T> list) {
        return list == null ? null : new ArraySet<T>(list);
    }

    private ContentCaptureHelper() {
        throw new UnsupportedOperationException("contains only static methods");
    }
}
