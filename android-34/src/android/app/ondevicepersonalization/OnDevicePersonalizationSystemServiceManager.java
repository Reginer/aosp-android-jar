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

package android.app.ondevicepersonalization;

import android.annotation.NonNull;
import android.os.IBinder;

/**
 * API for ODP module to interact with ODP System Server Service.
 * @hide
 */
public class OnDevicePersonalizationSystemServiceManager {
    /** Identifier for the ODP binder service in system_server. */
    public static final String ON_DEVICE_PERSONALIZATION_SYSTEM_SERVICE =
            "ondevicepersonalization_system_service";

    @NonNull private final IOnDevicePersonalizationSystemService mService;

    /** @hide */
    public OnDevicePersonalizationSystemServiceManager(@NonNull IBinder service) {
        mService = IOnDevicePersonalizationSystemService.Stub.asInterface(service);
    }

    /** @hide */
    @NonNull public IOnDevicePersonalizationSystemService getService() {
        return mService;
    }
}
