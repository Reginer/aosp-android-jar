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
import android.car.builtin.annotation.PlatformVersion;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;

import com.android.annotation.AddedIn;
import com.android.server.wm.CarActivityInterceptorUpdatable;
import com.android.server.wm.CarLaunchParamsModifierUpdatable;

import java.io.PrintWriter;
import java.util.function.BiConsumer;

/**
 * Contains calls from CarServiceHelperService (which is a built-in class) to
 * CarServiceHelperServiceUpdatableImpl (which is a updatable class as part of car-module).
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public interface CarServiceHelperServiceUpdatable {

    @AddedIn(PlatformVersion.TIRAMISU_0)
    void onUserRemoved(@NonNull UserHandle userHandle);

    @AddedIn(PlatformVersion.TIRAMISU_0)
    void onStart();

    @AddedIn(PlatformVersion.TIRAMISU_0)
    void dump(@NonNull PrintWriter pw, @Nullable String[] args);

    @AddedIn(PlatformVersion.TIRAMISU_0)
    void sendUserLifecycleEvent(int eventType, @Nullable UserHandle userFrom,
            @NonNull UserHandle userTo);

    @AddedIn(PlatformVersion.TIRAMISU_0)
    void onFactoryReset(@NonNull BiConsumer<Integer, Bundle> processFactoryReset);

    @AddedIn(PlatformVersion.TIRAMISU_0)
    void initBootUser();

    @AddedIn(PlatformVersion.TIRAMISU_0)
    CarLaunchParamsModifierUpdatable getCarLaunchParamsModifierUpdatable();

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    CarActivityInterceptorUpdatable getCarActivityInterceptorUpdatable();
}
