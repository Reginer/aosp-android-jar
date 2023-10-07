/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.setupwizardlib.partner;

import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * A potentially cross-package resource entry, which can then be retrieved using {@link
 * PackageManager#getResourcesForApplication(String)}. This class can also be sent across to other
 * packages on IPC via the Bundle representation.
 */
public final class ResourceEntry {
    @VisibleForTesting static final String KEY_PACKAGE_NAME = "packageName";
    @VisibleForTesting static final String KEY_RESOURCE_NAME = "resourceName";
    @VisibleForTesting static final String KEY_RESOURCE_ID = "resourceId";

    private final String mPackageName;
    private final String mResourceName;
    private final int mResourceId;

    /**
     * Creates a {@code ResourceEntry} object from a provided bundle.
     *
     * @param bundle the source bundle needs to have all the information for a {@code ResourceEntry}
     */
    public static ResourceEntry fromBundle(@Nullable Bundle bundle) {
        if (bundle == null
                || !bundle.containsKey(KEY_PACKAGE_NAME)
                || !bundle.containsKey(KEY_RESOURCE_NAME)
                || !bundle.containsKey(KEY_RESOURCE_ID)) {
            return null;
        }

        String packageName = bundle.getString(KEY_PACKAGE_NAME);
        String resourceName = bundle.getString(KEY_RESOURCE_NAME);
        int resourceId = bundle.getInt(KEY_RESOURCE_ID);
        return new ResourceEntry(packageName, resourceName, resourceId);
    }

    public ResourceEntry(String packageName, String resourceName, int resourceId) {
        mPackageName = packageName;
        mResourceName = resourceName;
        mResourceId = resourceId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getResourceName() {
        return this.mResourceName;
    }

    public int getResourceId() {
        return this.mResourceId;
    }

    /**
     * Returns a bundle representation of this resource entry, which can then be sent over IPC.
     *
     * @see #fromBundle(Bundle)
     */
    public Bundle toBundle() {
        Bundle result = new Bundle();
        result.putString(KEY_PACKAGE_NAME, mPackageName);
        result.putString(KEY_RESOURCE_NAME, mResourceName);
        result.putInt(KEY_RESOURCE_ID, mResourceId);
        return result;
    }
}
