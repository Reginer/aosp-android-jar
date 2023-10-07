/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.car.builtin.annotation.PlatformVersion;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;

import com.android.annotation.AddedIn;

/**
 * A wrapper over {@link com.android.server.wm.ActivityInterceptorCallback.ActivityInterceptorInfo}.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class ActivityInterceptorInfoWrapper {
    private final ActivityInterceptorCallback.ActivityInterceptorInfo mActivityInterceptorInfo;

    private ActivityInterceptorInfoWrapper(
            ActivityInterceptorCallback.ActivityInterceptorInfo interceptorInfo) {
        mActivityInterceptorInfo = interceptorInfo;
    }

    /**
     * Creates an instance of {@link ActivityInterceptorInfoWrapper}.
     *
     * @param interceptorInfo the original interceptorInfo that needs to be wrapped.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    public static ActivityInterceptorInfoWrapper create(
            ActivityInterceptorCallback.ActivityInterceptorInfo interceptorInfo) {
        return new ActivityInterceptorInfoWrapper(interceptorInfo);
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    public Intent getIntent() {
        return mActivityInterceptorInfo.getIntent();
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    public ActivityInfo getActivityInfo() {
        return mActivityInterceptorInfo.getActivityInfo();
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    public ActivityOptionsWrapper getCheckedOptions() {
        return ActivityOptionsWrapper.create(mActivityInterceptorInfo.getCheckedOptions());

    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    public String getCallingPackage() {
        return mActivityInterceptorInfo.getCallingPackage();
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    public int getUserId() {
        return mActivityInterceptorInfo.getUserId();
    }
}
