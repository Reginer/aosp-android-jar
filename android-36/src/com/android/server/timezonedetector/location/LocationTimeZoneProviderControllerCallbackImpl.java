/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.server.LocalServices;
import com.android.server.timezonedetector.LocationAlgorithmEvent;
import com.android.server.timezonedetector.TimeZoneDetectorInternal;

/**
 * The real implementation of {@link LocationTimeZoneProviderController.Callback} used by
 * {@link LocationTimeZoneProviderController} to interact with other server components.
 */
class LocationTimeZoneProviderControllerCallbackImpl
        extends LocationTimeZoneProviderController.Callback {

    LocationTimeZoneProviderControllerCallbackImpl(@NonNull ThreadingDomain threadingDomain) {
        super(threadingDomain);
    }

    @Override
    void sendEvent(@NonNull LocationAlgorithmEvent event) {
        mThreadingDomain.assertCurrentThread();

        TimeZoneDetectorInternal timeZoneDetector =
                LocalServices.getService(TimeZoneDetectorInternal.class);
        timeZoneDetector.handleLocationAlgorithmEvent(event);
    }
}
