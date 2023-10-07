/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.internal.telephony.nitz;

import android.annotation.NonNull;
import android.app.timedetector.TelephonyTimeSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.io.PrintWriter;

/**
 * An interface to various time / time zone detection behaviors that are centralized into services.
 * This interface exists to separate out behavior to enable easier testing of the
 * {@link NitzStateMachineImpl}.
 */
@VisibleForTesting
public interface TimeServiceHelper {

    /**
     * Suggests the time to the time detector service.
     *
     * @param suggestion the time suggestion
     */
    void suggestDeviceTime(@NonNull TelephonyTimeSuggestion suggestion);

    /**
     * Suggests the time zone to the time zone detector service.
     *
     * <p>NOTE: The {@link TelephonyTimeZoneSuggestion} cannot be null. The zoneId it contains can
     * be null to indicate there is no active suggestion; this can be used to clear a previous
     * suggestion.
     *
     * @param suggestion the time zone suggestion
     */
    void maybeSuggestDeviceTimeZone(@NonNull TelephonyTimeZoneSuggestion suggestion);

    /**
     * Dumps any logs held to the supplied writer.
     */
    void dumpLogs(IndentingPrintWriter ipw);

    /**
     * Dumps internal state such as field values.
     */
    void dumpState(PrintWriter pw);
}
