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

package com.android.modules.utils.build.testing;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.DeviceProperties;
import com.android.tradefed.device.ITestDevice;

/**
 * Utility class to check device SDK level from a host side test.
 */
public final class DeviceSdkLevel {

    private final ITestDevice device;

    public DeviceSdkLevel(ITestDevice device) {
        this.device = device;
    }

    /** Checks if the device is running on a release version of Android R or newer. */
    public boolean isDeviceAtLeastR() throws DeviceNotAvailableException {
        return device.getApiLevel() >= 30;
    }

    /** Checks if the device is running on a release version of Android S or newer. */
    public boolean isDeviceAtLeastS() throws DeviceNotAvailableException {
        return device.getApiLevel() >= 31;
    }

    /**
     * Checks if the device is running on a pre-release version of Android T or a release version of
     * Android T or newer.
     */
    public boolean isDeviceAtLeastT() throws DeviceNotAvailableException {
        return device.getApiLevel() >= 33;
    }

    private boolean isDeviceAtLeastPreReleaseCodename(@NonNull String codename)
            throws DeviceNotAvailableException {
        String deviceCodename = device.getProperty(DeviceProperties.BUILD_CODENAME);

        // Special case "REL", which means the build is not a pre-release build.
        if ("REL".equals(deviceCodename)) {
            return false;
        }

        // Otherwise lexically compare them. Return true if the build codename is equal to or
        // greater than the requested codename.
        return deviceCodename.compareTo(codename) >= 0;
    }

}
