/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.net.module.util;

import static android.Manifest.permission.NETWORK_STACK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.net.NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK;

import android.annotation.NonNull;
import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Collection of permission utilities.
 * @hide
 */
public final class PermissionUtils {
    /**
     * Return true if the context has one of given permission.
     */
    public static boolean checkAnyPermissionOf(@NonNull Context context,
            @NonNull String... permissions) {
        for (String permission : permissions) {
            if (context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enforce permission check on the context that should have one of given permission.
     */
    public static void enforceAnyPermissionOf(@NonNull Context context,
            @NonNull String... permissions) {
        if (!checkAnyPermissionOf(context, permissions)) {
            throw new SecurityException("Requires one of the following permissions: "
                    + String.join(", ", permissions) + ".");
        }
    }

    /**
     * If the NetworkStack, MAINLINE_NETWORK_STACK are not allowed for a particular process, throw a
     * {@link SecurityException}.
     *
     * @param context {@link android.content.Context} for the process.
     */
    public static void enforceNetworkStackPermission(final @NonNull Context context) {
        enforceNetworkStackPermissionOr(context);
    }

    /**
     * If the NetworkStack, MAINLINE_NETWORK_STACK or other specified permissions are not allowed
     * for a particular process, throw a {@link SecurityException}.
     *
     * @param context {@link android.content.Context} for the process.
     * @param otherPermissions The set of permissions that could be the candidate permissions , or
     *                         empty string if none of other permissions needed.
     */
    public static void enforceNetworkStackPermissionOr(final @NonNull Context context,
            final @NonNull String... otherPermissions) {
        ArrayList<String> permissions = new ArrayList<String>(Arrays.asList(otherPermissions));
        permissions.add(NETWORK_STACK);
        permissions.add(PERMISSION_MAINLINE_NETWORK_STACK);
        enforceAnyPermissionOf(context, permissions.toArray(new String[0]));
    }
}
