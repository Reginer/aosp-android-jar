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

package android.app.timezonedetector;

import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.content.Context;

/**
 * The interface through which system components can query and send signals to the
 * TimeZoneDetectorService.
 *
 * <p>SDK APIs are exposed on {@link android.app.time.TimeManager} to obscure the internal split
 * between time and time zone detection services. Migrate APIs there if they need to be part of an
 * SDK API.
 *
 * @hide
 */
@SystemService(Context.TIME_ZONE_DETECTOR_SERVICE)
public interface TimeZoneDetector {

    /**
     * The name of the service for shell commands.
     * @hide
     */
    String SHELL_COMMAND_SERVICE_NAME = "time_zone_detector";

    /**
     * A shell command that prints the current "auto time zone detection" global setting value.
     * @hide
     */
    String SHELL_COMMAND_IS_AUTO_DETECTION_ENABLED = "is_auto_detection_enabled";

    /**
     * A shell command that sets the current "auto time zone detection" global setting value.
     * @hide
     */
    String SHELL_COMMAND_SET_AUTO_DETECTION_ENABLED = "set_auto_detection_enabled";

    /**
     * A shell command that prints whether the telephony-based time zone detection feature is
     * supported on the device.
     * @hide
     */
    String SHELL_COMMAND_IS_TELEPHONY_DETECTION_SUPPORTED = "is_telephony_detection_supported";

    /**
     * A shell command that prints whether the geolocation-based time zone detection feature is
     * supported on the device.
     * @hide
     */
    String SHELL_COMMAND_IS_GEO_DETECTION_SUPPORTED = "is_geo_detection_supported";

    /**
     * A shell command that prints the current user's "location-based time zone detection enabled"
     * setting.
     * @hide
     */
    String SHELL_COMMAND_IS_GEO_DETECTION_ENABLED = "is_geo_detection_enabled";

    /**
     * A shell command that sets the current user's "location-based time zone detection enabled"
     * setting.
     * @hide
     */
    String SHELL_COMMAND_SET_GEO_DETECTION_ENABLED = "set_geo_detection_enabled";

    /**
     * A shell command that injects a location algorithm event (as if from the
     * location_time_zone_manager).
     * @hide
     */
    String SHELL_COMMAND_HANDLE_LOCATION_ALGORITHM_EVENT = "handle_location_algorithm_event";

    /**
     * A shell command that injects a manual time zone suggestion (as if from the SettingsUI or
     * similar).
     * @hide
     */
    String SHELL_COMMAND_SUGGEST_MANUAL_TIME_ZONE = "suggest_manual_time_zone";

    /**
     * A shell command that injects a telephony time zone suggestion (as if from the phone app).
     * @hide
     */
    String SHELL_COMMAND_SUGGEST_TELEPHONY_TIME_ZONE = "suggest_telephony_time_zone";

    /**
     * A shell command that enables telephony time zone fallback. See {@link
     * com.android.server.timezonedetector.TimeZoneDetectorStrategy} for details.
     * @hide
     */
    String SHELL_COMMAND_ENABLE_TELEPHONY_FALLBACK = "enable_telephony_fallback";

    /**
     * A shell command that retrieves the current time zone setting state.
     * @hide
     */
    String SHELL_COMMAND_GET_TIME_ZONE_STATE = "get_time_zone_state";

    /**
     * A shell command that sets the current time zone state for testing.
     * @hide
     */
    String SHELL_COMMAND_SET_TIME_ZONE_STATE = "set_time_zone_state_for_tests";

    /**
     * A shell command that sets the confidence in the current time zone state for testing.
     * @hide
     */
    String SHELL_COMMAND_CONFIRM_TIME_ZONE = "confirm_time_zone";

    /**
     * A shell command that dumps a {@link
     * com.android.server.timezonedetector.MetricsTimeZoneDetectorState} object to stdout for
     * debugging.
     * @hide
     */
    String SHELL_COMMAND_DUMP_METRICS = "dump_metrics";

    /**
     * A shared utility method to create a {@link ManualTimeZoneSuggestion}.
     *
     * @hide
     */
    static ManualTimeZoneSuggestion createManualTimeZoneSuggestion(String tzId, String debugInfo) {
        ManualTimeZoneSuggestion suggestion = new ManualTimeZoneSuggestion(tzId);
        suggestion.addDebugInfo(debugInfo);
        return suggestion;
    }

    /**
     * Suggests the current time zone, determined from the user's manually entered information, to
     * the detector. Returns {@code false} if the suggestion was invalid, or the device
     * configuration / user capabilities prevents the suggestion being used (even if it is the same
     * as the current device time zone), {@code true} if the suggestion was accepted. A suggestion
     * that is valid but does not change the time zone because it matches the current device time
     * zone is considered accepted.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.SUGGEST_MANUAL_TIME_AND_ZONE)
    boolean suggestManualTimeZone(@NonNull ManualTimeZoneSuggestion timeZoneSuggestion);

    /**
     * Suggests the current time zone, determined using telephony signals, to the detector. The
     * detector may ignore the signal based on system settings, whether better information is
     * available, and so on.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.SUGGEST_TELEPHONY_TIME_AND_ZONE)
    void suggestTelephonyTimeZone(@NonNull TelephonyTimeZoneSuggestion timeZoneSuggestion);
}
