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

package android.adservices.ondevicepersonalization;

import android.annotation.SystemApi;

/**
 * OnDevicePersonalization permission settings.
 *
 * @hide
*/
@SystemApi
public class OnDevicePersonalizationPermissions {
    private OnDevicePersonalizationPermissions() {}

    /**
     * The permission that lets it modify ODP's enablement state.
     */
    public static final String MODIFY_ONDEVICEPERSONALIZATION_STATE =
            "android.permission.ondevicepersonalization.MODIFY_ONDEVICEPERSONALIZATION_STATE";

    /**
     * The permission required for callers to send measurement events to ODP.
     */
    public static final String NOTIFY_MEASUREMENT_EVENT =
            "android.permission.ondevicepersonalization.NOTIFY_MEASUREMENT_EVENT";

    /**
     * The permission required to connect to the ODP system server component.
     * @hide
     */
    public static final String ACCESS_SYSTEM_SERVER_SERVICE =
            "android.permission.ondevicepersonalization.ACCESS_SYSTEM_SERVER_SERVICE";
}
