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

package com.android.server.security.advancedprotection.features;

import static android.security.advancedprotection.AdvancedProtectionManager.ADVANCED_PROTECTION_SYSTEM_ENTITY;
import static android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_CELLULAR_2G;

import android.annotation.NonNull;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.security.advancedprotection.AdvancedProtectionFeature;
import android.util.Slog;

/** @hide */
public final class DisallowCellular2GAdvancedProtectionHook extends AdvancedProtectionHook {
    private static final String TAG = "AdvancedProtectionDisallowCellular2G";

    private final AdvancedProtectionFeature mFeature =
            new AdvancedProtectionFeature(FEATURE_ID_DISALLOW_CELLULAR_2G);
    private final DevicePolicyManager mDevicePolicyManager;
    private final PackageManager mPackageManager;

    public DisallowCellular2GAdvancedProtectionHook(@NonNull Context context, boolean enabled) {
        super(context, enabled);
        mDevicePolicyManager = context.getSystemService(DevicePolicyManager.class);
        mPackageManager = context.getPackageManager();

        onAdvancedProtectionChanged(enabled);
    }

    @NonNull
    @Override
    public AdvancedProtectionFeature getFeature() {
        return mFeature;
    }

    @Override
    public boolean isAvailable() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @Override
    public void onAdvancedProtectionChanged(boolean enabled) {
        if (enabled) {
            Slog.d(TAG, "Setting DISALLOW_CELLULAR_2G_GLOBALLY restriction");
            mDevicePolicyManager.addUserRestrictionGlobally(
                    ADVANCED_PROTECTION_SYSTEM_ENTITY, UserManager.DISALLOW_CELLULAR_2G);
        } else {
            Slog.d(TAG, "Clearing DISALLOW_CELLULAR_2G_GLOBALLY restriction");
            mDevicePolicyManager.clearUserRestrictionGlobally(
                    ADVANCED_PROTECTION_SYSTEM_ENTITY, UserManager.DISALLOW_CELLULAR_2G);
        }
    }
}
