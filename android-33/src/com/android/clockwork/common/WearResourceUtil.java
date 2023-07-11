/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.clockwork.common;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;


/**
 * This class provides Utils for Wear resources
 */
public class WearResourceUtil {
    public static String WEAR_RESOURCE_PACKAGE = "com.android.wearable.resources";
    private static final String TAG = "WearResourceUtils";

    /**
     * Retrieve resources for the {@link #WEAR_RESOURCE_PACKAGE} resource package.
     *
     * @param context the context used to retrieve the resources
     *
     * @return the Resources for the Wear package or null if the resources could not be loaded
     */
    @Nullable
    public static Resources getWearableResources(Context context) {
        try {
            return context.getPackageManager()
                    .getResourcesForApplication(WEAR_RESOURCE_PACKAGE);
        }  catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Resources could not be loaded for the package - "
                    + WEAR_RESOURCE_PACKAGE);
        }
        return null;
    }

    /**
     * Retrieve a resource identifier for the {@link #WEAR_RESOURCE_PACKAGE} resource package.
     *
     * @param context The context used to retrieve the resources.
     * @param name resource name as String.
     * @param type resource type as String.
     *
     * @return Associated resources id, or 0 if no resource was not found.
     */
    public static int getIdentifier(Context context, String name, String type) {
        Resources resources = getWearableResources(context);
        if (resources != null) {
            return resources.getIdentifier(
                    WEAR_RESOURCE_PACKAGE + ":" + type + "/" + name, null, null);
        }
        return 0;
    }
}
