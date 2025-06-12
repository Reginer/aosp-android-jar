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

package com.android.server.timezonedetector.location;

import android.annotation.NonNull;
import android.service.timezone.TimeZoneProviderEvent;

/**
 * Used by {@link LocationTimeZoneProvider} to ensure that all time zone IDs are understood by the
 * platform.
 */
public interface TimeZoneProviderEventPreProcessor {

    /**
     * May return uncertain event if {@code timeZoneProviderEvent} is ill-formed or drop/rewrite
     * time zone IDs.
     */
    TimeZoneProviderEvent preProcess(@NonNull TimeZoneProviderEvent timeZoneProviderEvent);

}
