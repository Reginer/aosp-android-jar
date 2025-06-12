/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.internal.telephony.cdma;

import android.telephony.TelephonyManager;

public final class EriInfo {

    public static final int ROAMING_INDICATOR_ON    = TelephonyManager.ERI_ON;
    public static final int ROAMING_INDICATOR_OFF   = TelephonyManager.ERI_OFF;
    public static final int ROAMING_INDICATOR_FLASH = TelephonyManager.ERI_FLASH;

    public static final int ROAMING_ICON_MODE_NORMAL    = TelephonyManager.ERI_ICON_MODE_NORMAL;
    public static final int ROAMING_ICON_MODE_FLASH     = TelephonyManager.ERI_ICON_MODE_FLASH;

    public int roamingIndicator;
    public int iconIndex;
    public int iconMode;
    public String eriText;
    public int callPromptId;
    public int alertId;

    public EriInfo (int roamingIndicator, int iconIndex, int iconMode, String eriText,
            int callPromptId, int alertId) {

        this.roamingIndicator = roamingIndicator;
        this.iconIndex = iconIndex;
        this.iconMode = iconMode;
        this.eriText = eriText;
        this.callPromptId = callPromptId;
        this.alertId = alertId;
    }
}
