/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.window.DesktopModeFlags.EXCLUDE_CAPTION_FROM_APP_BOUNDS;

import static com.android.server.wm.AppCompatUtils.isInDesktopMode;

import android.annotation.NonNull;
import android.app.WindowConfiguration.WindowingMode;
import android.content.res.Configuration;
import android.graphics.Rect;

/**
 * Encapsulate logic related to sandboxing for app compatibility.
 */
class AppCompatSandboxingPolicy {

    @NonNull
    private final ActivityRecord mActivityRecord;

    AppCompatSandboxingPolicy(@NonNull ActivityRecord activityRecord) {
        mActivityRecord = activityRecord;
    }

    /**
     * In freeform, the container bounds are scaled with app bounds. Activity bounds can be
     * outside of its container bounds if insets are coupled with configuration outside of
     * freeform and maintained in freeform for size compat mode.
     *
     * <p>Sandbox activity bounds in freeform to app bounds to force app to display within the
     * container. This prevents UI cropping when activities can draw below insets which are
     * normally excluded from appBounds before targetSDK < 35
     * (see ConfigurationContainer#applySizeOverrideIfNeeded).
     */
    void sandboxBoundsIfNeeded(@NonNull Configuration resolvedConfig,
            @WindowingMode int windowingMode) {
        if (!EXCLUDE_CAPTION_FROM_APP_BOUNDS.isTrue()) {
            return;
        }

        if (isInDesktopMode(mActivityRecord.mAtmService.mContext, windowingMode)) {
            Rect appBounds = resolvedConfig.windowConfiguration.getAppBounds();
            if (appBounds == null || appBounds.isEmpty()) {
                // When there is no override bounds, the activity will inherit the bounds from
                // parent.
                appBounds = mActivityRecord.mResolveConfigHint.mParentAppBoundsOverride;
            }
            resolvedConfig.windowConfiguration.setBounds(appBounds);
        }
    }
}
