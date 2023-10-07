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
package com.android.internal.car;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.annotation.UserIdInt;
import android.car.builtin.annotation.PlatformVersion;
import android.os.Build;
import android.os.UserHandle;

import com.android.annotation.AddedIn;

import java.io.File;

/**
 * Interface implemented by CarServiceHelperService.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public interface CarServiceHelperInterface {

    /**
     * Sets safety mode
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    void setSafetyMode(boolean safe);

    /**
     * Creates user even when disallowed
     */
    @Nullable
    @AddedIn(PlatformVersion.TIRAMISU_0)
    UserHandle createUserEvenWhenDisallowed(@Nullable String name, @NonNull String userType,
            int flags);

    /**
     * Gets the main display assigned to the user.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    int getMainDisplayAssignedToUser(@UserIdInt int userId);

    /**
     * Gets the full user (i.e., not profile) assigned to the display.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    int getUserAssignedToDisplay(int displayId);

    /**
     * Dumps service stacks
     */
    @Nullable
    @AddedIn(PlatformVersion.TIRAMISU_0)
    File dumpServiceStacks();

    /** Check {@link android.os.Process#setProcessGroup(int, int)}. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    void setProcessGroup(int pid, int group);

    /** Check {@link android.os.Process#getProcessGroup(int)}. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    int getProcessGroup(int pid);

    /** Check {@link ActivityManager#startUserInBackgroundVisibleOnDisplay(int, int)}. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    boolean startUserInBackgroundVisibleOnDisplay(@UserIdInt int userId, int displayId);

    /** Check {@link android.os.Process#setProcessProfile(int, int, String)}. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    void setProcessProfile(int pid, int uid, @NonNull String profile);

    /**
     * Returns the PID for the AIDL VHAL service.
     *
     * On error, returns {@link com.android.car.internal.common.CommonConstants#INVALID_PID}.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    int fetchAidlVhalPid();
}
