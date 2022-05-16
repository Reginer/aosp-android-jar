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

package com.android.wifitrackerlib;

import android.content.Context;
import android.os.UserManager;
import android.util.ArraySet;

import androidx.annotation.NonNull;

import java.util.Set;

/**
 * Wrapper class for commonly referenced objects and static data.
 */
public class WifiTrackerInjector {
    private final boolean mIsDemoMode;
    @NonNull private final Set<String> mNoAttributionAnnotationPackages;

    // TODO(b/201571677): Migrate the rest of the common objects to WifiTrackerInjector.
    public WifiTrackerInjector(@NonNull Context context) {
        mIsDemoMode = UserManager.isDeviceInDemoMode(context);
        mNoAttributionAnnotationPackages = new ArraySet<>();
        String[] noAttributionAnnotationPackages = context.getString(
                R.string.wifitrackerlib_no_attribution_annotation_packages).split(",");
        for (int i = 0; i < noAttributionAnnotationPackages.length; i++) {
            mNoAttributionAnnotationPackages.add(noAttributionAnnotationPackages[i]);
        }
    }

    public boolean isDemoMode() {
        return mIsDemoMode;
    }

    /**
     * Returns the set of package names which we should not show attribution annotations for.
     */
    @NonNull public Set<String> getNoAttributionAnnotationPackages() {
        return mNoAttributionAnnotationPackages;
    }
}
