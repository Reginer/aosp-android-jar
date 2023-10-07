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

package com.android.server.wm;

import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.annotation.UserIdInt;
import android.car.builtin.annotation.PlatformVersion;
import android.hardware.display.DisplayManager;
import android.os.Build;

import com.android.annotation.AddedIn;

/**
 * Updatable interface of CarLaunchParamsModifier.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public interface CarLaunchParamsModifierUpdatable {

    /** Returns {@link DisplayManager.DisplayListener} of CarLaunchParamsModifierUpdatable. */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    DisplayManager.DisplayListener getDisplayListener();

    /** Notifies user switching. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    void handleUserVisibilityChanged(@UserIdInt int userId, boolean visible);

    /** Notifies user switching. */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    void handleCurrentUserSwitching(@UserIdInt int newUserId);

    /** Notifies user starting. */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    void handleUserStarting(@UserIdInt int startingUser);

    /** Notifies user stopped. */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    void handleUserStopped(@UserIdInt int stoppedUser);

    /**
     * Calculates {@code outParams} based on the given arguments.
     * See {@code LaunchParamsController.LaunchParamsModifier.onCalculate()} for the detail.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    int calculate(CalculateParams params);
}
