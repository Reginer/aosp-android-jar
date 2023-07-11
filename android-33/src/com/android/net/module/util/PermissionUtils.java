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

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS;
import static android.Manifest.permission.NETWORK_STACK;
import static android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.net.NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Binder;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    /**
     * If the CONNECTIVITY_USE_RESTRICTED_NETWORKS is not allowed for a particular process, throw a
     * {@link SecurityException}.
     *
     * @param context {@link android.content.Context} for the process.
     * @param message A message to include in the exception if it is thrown.
     */
    public static void enforceRestrictedNetworkPermission(
            final @NonNull Context context, final @Nullable String message) {
        context.enforceCallingOrSelfPermission(CONNECTIVITY_USE_RESTRICTED_NETWORKS, message);
    }

    /**
     * If the ACCESS_NETWORK_STATE is not allowed for a particular process, throw a
     * {@link SecurityException}.
     *
     * @param context {@link android.content.Context} for the process.
     * @param message A message to include in the exception if it is thrown.
     */
    public static void enforceAccessNetworkStatePermission(
            final @NonNull Context context, final @Nullable String message) {
        context.enforceCallingOrSelfPermission(ACCESS_NETWORK_STATE, message);
    }

    /**
     * Return true if the context has DUMP permission.
     */
    public static boolean checkDumpPermission(Context context, String tag, PrintWriter pw) {
        if (context.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump " + tag + " from from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid()
                    + " due to missing android.permission.DUMP permission");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Enforce that a given feature is available and if not, throw an
     * {@link UnsupportedOperationException}.
     *
     * @param context {@link android.content.Context} for the process.
     * @param feature the feature name to enforce.
     * @param errorMessage an optional error message to include.
     */
    public static void enforceSystemFeature(final @NonNull Context context,
            final @NonNull String feature, final @Nullable String errorMessage) {
        final boolean hasSystemFeature =
                context.getPackageManager().hasSystemFeature(feature);
        if (!hasSystemFeature) {
            if (null == errorMessage) {
                throw new UnsupportedOperationException();
            }
            throw new UnsupportedOperationException(errorMessage);
        }
    }

    /**
     * Get the list of granted permissions for a package info.
     *
     * PackageInfo contains the list of requested permissions, and their state (whether they
     * were granted or not, in particular) as a parallel array. Most users care only about
     * granted permissions. This method returns the list of them.
     *
     * @param packageInfo the package info for the relevant uid.
     * @return the list of granted permissions.
     */
    public static List<String> getGrantedPermissions(final @NonNull PackageInfo packageInfo) {
        if (null == packageInfo.requestedPermissions) return Collections.emptyList();
        final ArrayList<String> result = new ArrayList<>(packageInfo.requestedPermissions.length);
        for (int i = 0; i < packageInfo.requestedPermissions.length; ++i) {
            if (0 != (REQUESTED_PERMISSION_GRANTED & packageInfo.requestedPermissionsFlags[i])) {
                result.add(packageInfo.requestedPermissions[i]);
            }
        }
        return result;
    }
}
