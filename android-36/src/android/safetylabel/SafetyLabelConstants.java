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

package android.safetylabel;

import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import android.annotation.SystemApi;

import androidx.annotation.RequiresApi;

/**
 * Constants relating to the Safety Label feature.
 *
 * @hide
 */
@SystemApi
@RequiresApi(UPSIDE_DOWN_CAKE)
public final class SafetyLabelConstants {

    /**
     * Constant to be used as Device Config flag determining whether the Permission Rationale
     * feature is enabled.
     *
     * <p>When this flag is enabled, permission rationale messaging will be displayed in permission
     * settings and the runtime permissions grant dialog.
     *
     * @hide
     */
    @SystemApi
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final String PERMISSION_RATIONALE_ENABLED = "permission_rationale_enabled";

    /**
     * Constant to be used as Device Config flag determining whether the Safety Label Change
     * Notifications feature is enabled.
     *
     * <p>When this flag is enabled, a system notification will be sent to users if any apps they
     * have installed have made recent updates to their data sharing policy in their app safety
     * labels.
     *
     * @hide
     */
    @SystemApi
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final String SAFETY_LABEL_CHANGE_NOTIFICATIONS_ENABLED =
            "safety_label_change_notifications_enabled";

    private SafetyLabelConstants() {}
}
