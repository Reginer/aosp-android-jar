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

package com.android.internal.policy;

import static android.content.pm.ActivityInfo.INSETS_DECOUPLED_CONFIGURATION_ENFORCED;
import static android.content.pm.ActivityInfo.OVERRIDE_EXCLUDE_CAPTION_INSETS_FROM_APP_BOUNDS;

import android.annotation.NonNull;
import android.content.pm.ActivityInfo;
import android.window.DesktopModeFlags;

/**
 * Utility functions for app compat in desktop windowing used by both window manager and System UI.
 * @hide
 */
public final class DesktopModeCompatUtils {

    /**
     * Whether the caption insets should be excluded from configuration for system to handle.
     * The caller should ensure the activity is in or entering desktop view.
     *
     * <p> The treatment is enabled when all the of the following is true:
     * <li> Any flags to forcibly consume caption insets are enabled.
     * <li> Top activity have configuration coupled with insets.
     * <li> Task is not resizeable or per-app override
     * {@link ActivityInfo#OVERRIDE_EXCLUDE_CAPTION_INSETS_FROM_APP_BOUNDS} is enabled.
     */
    public static boolean shouldExcludeCaptionFromAppBounds(@NonNull ActivityInfo info,
            boolean isResizeable, boolean optOutEdgeToEdge) {
        return DesktopModeFlags.EXCLUDE_CAPTION_FROM_APP_BOUNDS.isTrue()
                && isAnyForceConsumptionFlagsEnabled()
                && !isConfigurationDecoupled(info, optOutEdgeToEdge)
                && (!isResizeable
                    || info.isChangeEnabled(OVERRIDE_EXCLUDE_CAPTION_INSETS_FROM_APP_BOUNDS));
    }

    private static boolean isConfigurationDecoupled(@NonNull ActivityInfo info,
            boolean optOutEdgeToEdge) {
        return info.isChangeEnabled(INSETS_DECOUPLED_CONFIGURATION_ENFORCED) && !optOutEdgeToEdge;
    }

    private static boolean isAnyForceConsumptionFlagsEnabled() {
        return DesktopModeFlags.ENABLE_CAPTION_COMPAT_INSET_FORCE_CONSUMPTION_ALWAYS.isTrue()
                || DesktopModeFlags.ENABLE_CAPTION_COMPAT_INSET_FORCE_CONSUMPTION.isTrue();
    }
}
