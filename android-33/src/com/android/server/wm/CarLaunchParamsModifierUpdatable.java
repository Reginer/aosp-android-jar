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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.UserIdInt;
import android.car.app.CarActivityManager;
import android.content.ComponentName;
import android.hardware.display.DisplayManager;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.Display;
import android.window.DisplayAreaOrganizer;

import com.android.internal.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Updatable interface of CarLaunchParamsModifier.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public interface CarLaunchParamsModifierUpdatable {

    /** Returns {@link DisplayManager.DisplayListener} of CarLaunchParamsModifierUpdatable. */
    DisplayManager.DisplayListener getDisplayListener();

    /** Notifies user switching. */
    void handleCurrentUserSwitching(@UserIdInt int newUserId);

    /** Notifies user starting. */
    void handleUserStarting(@UserIdInt int startingUser);

    /** Notifies user stopped. */
    void handleUserStopped(@UserIdInt int stoppedUser);

    /**
     * Calculates {@code outParams} based on the given arguments.
     * See {@code LaunchParamsController.LaunchParamsModifier.onCalculate()} for the detail.
     */
    int calculate(CalculateParams params);
}
