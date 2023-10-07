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

import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.car.builtin.annotation.PlatformVersion;
import android.os.Build;

import com.android.annotation.AddedIn;

/**
 * Updatable interface of CarActivityInterceptor.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public interface CarActivityInterceptorUpdatable {
    /**
     * Intercepts the activity launch.
     *
     * @param info the activity info of the activity being launched.
     * @return the result of the interception in the form of the modified intent & activity options.
     *         {@code null} is returned when no modification is required on intent or activity
     *         options.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    @Nullable
    ActivityInterceptResultWrapper onInterceptActivityLaunch(
            ActivityInterceptorInfoWrapper info);
}
