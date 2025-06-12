/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.content.pm.PackageManager;

import com.android.server.wm.utils.OptPropFactory;

import java.io.PrintWriter;

/**
 * Allows the interaction with all the app compat policies and configurations
 */
class AppCompatController {
    @NonNull
    private final TransparentPolicy mTransparentPolicy;
    @NonNull
    private final AppCompatOrientationPolicy mOrientationPolicy;
    @NonNull
    private final AppCompatAspectRatioPolicy mAspectRatioPolicy;
    @NonNull
    private final AppCompatSafeRegionPolicy mSafeRegionPolicy;
    @NonNull
    private final AppCompatReachabilityPolicy mReachabilityPolicy;
    @NonNull
    private final DesktopAppCompatAspectRatioPolicy mDesktopAspectRatioPolicy;
    @NonNull
    private final AppCompatOverrides mAppCompatOverrides;
    @NonNull
    private final AppCompatDeviceStateQuery mDeviceStateQuery;
    @NonNull
    private final AppCompatLetterboxPolicy mLetterboxPolicy;
    @NonNull
    private final AppCompatSizeCompatModePolicy mSizeCompatModePolicy;
    @NonNull
    private final AppCompatSandboxingPolicy mSandboxingPolicy;
    @NonNull
    private final AppCompatDisplayCompatModePolicy mDisplayCompatModePolicy;

    AppCompatController(@NonNull WindowManagerService wmService,
                        @NonNull ActivityRecord activityRecord) {
        final PackageManager packageManager = wmService.mContext.getPackageManager();
        final OptPropFactory optPropBuilder = new OptPropFactory(packageManager,
                activityRecord.packageName, activityRecord.mUserId);
        mDeviceStateQuery = new AppCompatDeviceStateQuery(activityRecord);
        mTransparentPolicy = new TransparentPolicy(activityRecord,
                wmService.mAppCompatConfiguration);
        mAppCompatOverrides = new AppCompatOverrides(activityRecord, packageManager,
                wmService.mAppCompatConfiguration, optPropBuilder, mDeviceStateQuery);
        mOrientationPolicy = new AppCompatOrientationPolicy(activityRecord, mAppCompatOverrides);
        mAspectRatioPolicy = new AppCompatAspectRatioPolicy(activityRecord,
                mTransparentPolicy, mAppCompatOverrides);
        mSafeRegionPolicy = new AppCompatSafeRegionPolicy(activityRecord, packageManager);
        mReachabilityPolicy = new AppCompatReachabilityPolicy(activityRecord,
                wmService.mAppCompatConfiguration);
        mLetterboxPolicy = new AppCompatLetterboxPolicy(activityRecord,
                wmService.mAppCompatConfiguration);
        mDesktopAspectRatioPolicy = new DesktopAppCompatAspectRatioPolicy(activityRecord,
                mAppCompatOverrides, mTransparentPolicy, wmService.mAppCompatConfiguration);
        mSizeCompatModePolicy = new AppCompatSizeCompatModePolicy(activityRecord,
                mAppCompatOverrides);
        mSandboxingPolicy = new AppCompatSandboxingPolicy(activityRecord);
        mDisplayCompatModePolicy = new AppCompatDisplayCompatModePolicy();
    }

    @NonNull
    TransparentPolicy getTransparentPolicy() {
        return mTransparentPolicy;
    }

    @NonNull
    AppCompatOrientationPolicy getOrientationPolicy() {
        return mOrientationPolicy;
    }

    @NonNull
    AppCompatAspectRatioPolicy getAspectRatioPolicy() {
        return mAspectRatioPolicy;
    }

    @NonNull
    AppCompatSafeRegionPolicy getSafeRegionPolicy() {
        return mSafeRegionPolicy;
    }

    @NonNull
    DesktopAppCompatAspectRatioPolicy getDesktopAspectRatioPolicy() {
        return mDesktopAspectRatioPolicy;
    }

    @NonNull
    AppCompatOrientationOverrides getOrientationOverrides() {
        return mAppCompatOverrides.getOrientationOverrides();
    }

    @NonNull
    AppCompatCameraOverrides getCameraOverrides() {
        return mAppCompatOverrides.getCameraOverrides();
    }

    @NonNull
    AppCompatAspectRatioOverrides getAspectRatioOverrides() {
        return mAppCompatOverrides.getAspectRatioOverrides();
    }

    @NonNull
    AppCompatResizeOverrides getResizeOverrides() {
        return mAppCompatOverrides.getResizeOverrides();
    }

    @NonNull
    AppCompatReachabilityPolicy getReachabilityPolicy() {
        return mReachabilityPolicy;
    }

    @NonNull
    AppCompatLetterboxPolicy getLetterboxPolicy() {
        return mLetterboxPolicy;
    }

    @NonNull
    AppCompatFocusOverrides getFocusOverrides() {
        return mAppCompatOverrides.getFocusOverrides();
    }

    @NonNull
    AppCompatReachabilityOverrides getReachabilityOverrides() {
        return mAppCompatOverrides.getReachabilityOverrides();
    }

    @NonNull
    AppCompatDeviceStateQuery getDeviceStateQuery() {
        return mDeviceStateQuery;
    }

    @NonNull
    AppCompatLetterboxOverrides getLetterboxOverrides() {
        return mAppCompatOverrides.getLetterboxOverrides();
    }

    @NonNull
    AppCompatSizeCompatModePolicy getSizeCompatModePolicy() {
        return mSizeCompatModePolicy;
    }

    @NonNull
    AppCompatSandboxingPolicy getSandboxingPolicy() {
        return mSandboxingPolicy;
    }

    @NonNull
    AppCompatDisplayCompatModePolicy getDisplayCompatModePolicy() {
        return mDisplayCompatModePolicy;
    }

    void dump(@NonNull PrintWriter pw, @NonNull String prefix) {
        getTransparentPolicy().dump(pw, prefix);
        getLetterboxPolicy().dump(pw, prefix);
        getSizeCompatModePolicy().dump(pw, prefix);
        getSafeRegionPolicy().dump(pw, prefix);
    }
}
