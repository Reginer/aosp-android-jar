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

import static android.view.WindowManager.PROPERTY_COMPAT_ALLOW_SAFE_REGION_LETTERBOXING;

import android.annotation.NonNull;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;

import java.io.PrintWriter;
import java.util.function.BooleanSupplier;

/**
 * Encapsulate app compat policy logic related to a safe region.
 */
class AppCompatSafeRegionPolicy {
    @NonNull
    private final ActivityRecord mActivityRecord;
    @NonNull
    final PackageManager mPackageManager;
    // Whether the Activity needs to be in the safe region bounds.
    private boolean mNeedsSafeRegionBounds = false;
    // Denotes the latest safe region bounds. Can be empty if the activity or the ancestors do
    // not have any safe region bounds.
    @NonNull
    private final Rect mLatestSafeRegionBounds = new Rect();
    // Whether the activity has allowed safe region letterboxing. This can be set through the
    // manifest and the default value is true.
    @NonNull
    private final BooleanSupplier mAllowSafeRegionLetterboxing;

    AppCompatSafeRegionPolicy(@NonNull ActivityRecord activityRecord,
            @NonNull PackageManager packageManager) {
        mActivityRecord = activityRecord;
        mPackageManager = packageManager;
        mAllowSafeRegionLetterboxing = AppCompatUtils.asLazy(() -> {
            // Application level property.
            if (allowSafeRegionLetterboxing(packageManager)) {
                return true;
            }
            // Activity level property.
            try {
                return packageManager.getPropertyAsUser(
                        PROPERTY_COMPAT_ALLOW_SAFE_REGION_LETTERBOXING,
                        mActivityRecord.mActivityComponent.getPackageName(),
                        mActivityRecord.mActivityComponent.getClassName(),
                        mActivityRecord.mUserId).getBoolean();
            } catch (PackageManager.NameNotFoundException e) {
                return true;
            }
        });
    }

    private boolean allowSafeRegionLetterboxing(PackageManager pm) {
        try {
            return pm.getPropertyAsUser(
                    PROPERTY_COMPAT_ALLOW_SAFE_REGION_LETTERBOXING,
                    mActivityRecord.packageName,
                    /* className */ null,
                    mActivityRecord.mUserId).getBoolean();
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    /**
     * Computes the latest safe region bounds in
     * {@link ActivityRecord#resolveOverrideConfiguration(Configuration)} since the activity has not
     * been attached to the parent container when the ActivityRecord is instantiated. Note that the
     * latest safe region bounds will be empty if activity has not allowed safe region letterboxing.
     *
     * @return latest safe region bounds as set on an ancestor window container.
     */
    public Rect getLatestSafeRegionBounds() {
        if (!allowSafeRegionLetterboxing()) {
            mLatestSafeRegionBounds.setEmpty();
            return null;
        }
        // Get the latest safe region bounds since the bounds could have changed
        final Rect latestSafeRegionBounds = mActivityRecord.getSafeRegionBounds();
        if (latestSafeRegionBounds != null) {
            mLatestSafeRegionBounds.set(latestSafeRegionBounds);
        } else {
            mLatestSafeRegionBounds.setEmpty();
        }
        return latestSafeRegionBounds;
    }

    /**
     * Computes bounds when letterboxing is required only for the safe region bounds if needed.
     */
    public void resolveSafeRegionBoundsConfigurationIfNeeded(@NonNull Configuration resolvedConfig,
            @NonNull Configuration newParentConfig) {
        if (mLatestSafeRegionBounds.isEmpty()) {
            return;
        }
        // If activity can not be letterboxed for a safe region only or it has not been attached
        // to a WindowContainer yet.
        if (!isLetterboxedForSafeRegionOnlyAllowed() || mActivityRecord.getParent() == null) {
            return;
        }
        resolvedConfig.windowConfiguration.setBounds(mLatestSafeRegionBounds);
        mActivityRecord.computeConfigByResolveHint(resolvedConfig, newParentConfig);
    }

    /**
     * Safe region bounds can either be applied along with size compat, fixed orientation or
     * aspect ratio conditions by sandboxing them to the safe region bounds. Or it can be applied
     * independently when no other letterboxing condition is triggered. This method helps detecting
     * the latter case.
     *
     * @return {@code true} if this application or activity has allowed safe region letterboxing and
     * can be letterboxed only due to the safe region being set on the current or ancestor window
     * container.
     */
    boolean isLetterboxedForSafeRegionOnlyAllowed() {
        return !mActivityRecord.areBoundsLetterboxed() && getNeedsSafeRegionBounds()
                && getLatestSafeRegionBounds() != null;
    }

    /**
     * Set {@code true} if this activity needs to be within the safe region bounds, else false.
     */
    public void setNeedsSafeRegionBounds(boolean needsSafeRegionBounds) {
        mNeedsSafeRegionBounds = needsSafeRegionBounds;
    }

    /**
     * @return {@code true} if this activity needs to be within the safe region bounds.
     */
    public boolean getNeedsSafeRegionBounds() {
        return mNeedsSafeRegionBounds;
    }

    /** @see android.view.WindowManager#PROPERTY_COMPAT_ALLOW_SAFE_REGION_LETTERBOXING */
    boolean allowSafeRegionLetterboxing() {
        return mAllowSafeRegionLetterboxing.getAsBoolean();
    }

    void dump(@NonNull PrintWriter pw, @NonNull String prefix) {
        if (mNeedsSafeRegionBounds) {
            pw.println(prefix + " mNeedsSafeRegionBounds=true");
            pw.println(
                    prefix + " latestSafeRegionBoundsOnActivity=" + getLatestSafeRegionBounds());
        }
        if (isLetterboxedForSafeRegionOnlyAllowed()) {
            pw.println(prefix + " isLetterboxForSafeRegionOnlyAllowed=true");
        }
        if (!allowSafeRegionLetterboxing()) {
            pw.println(prefix + " allowSafeRegionLetterboxing=false");
        }
    }
}
