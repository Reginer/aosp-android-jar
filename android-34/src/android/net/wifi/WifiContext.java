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

package android.net.wifi;

import android.annotation.NonNull;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.wifi.util.Environment;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper for context to override getResources method. Resources for wifi mainline jar needs to be
 * fetched from the resources APK.
 *
 * @hide
 */
public class WifiContext extends ContextWrapper {
    private static final String TAG = "WifiContext";
    /** Intent action that is used to identify ServiceWifiResources.apk */
    private static final String ACTION_RESOURCES_APK =
            "com.android.server.wifi.intent.action.SERVICE_WIFI_RESOURCES_APK";
    /** Intent action that is used to identify WifiDialog.apk */
    private static final String ACTION_WIFI_DIALOG_APK =
            "com.android.server.wifi.intent.action.WIFI_DIALOG_APK";

    /** Since service-wifi runs within system_server, its package name is "android". */
    private static final String SERVICE_WIFI_PACKAGE_NAME = "android";

    private String mWifiOverlayApkPkgName;
    private String mWifiDialogApkPkgName;

    // Cached resources from the resources APK.
    private AssetManager mWifiAssetsFromApk;
    private Resources mWifiResourcesFromApk;
    private Resources.Theme mWifiThemeFromApk;

    public WifiContext(@NonNull Context contextBase) {
        super(contextBase);
    }

    /** Get the package name of ServiceWifiResources.apk */
    public @Nullable String getWifiOverlayApkPkgName() {
        if (mWifiOverlayApkPkgName != null) {
            return mWifiOverlayApkPkgName;
        }
        mWifiOverlayApkPkgName = getApkPkgNameForAction(ACTION_RESOURCES_APK, null);
        if (mWifiOverlayApkPkgName == null) {
            // Resource APK not loaded yet, print a stack trace to see where this is called from
            Log.e(TAG, "Attempted to fetch resources before Wifi Resources APK is loaded!",
                    new IllegalStateException());
            return null;
        }
        Log.i(TAG, "Found Wifi Resources APK at: " + mWifiOverlayApkPkgName);
        return mWifiOverlayApkPkgName;
    }

    /** Get the package name of WifiDialog.apk */
    public @Nullable String getWifiDialogApkPkgName() {
        if (mWifiDialogApkPkgName != null) {
            return mWifiDialogApkPkgName;
        }
        mWifiDialogApkPkgName = getApkPkgNameForAction(ACTION_WIFI_DIALOG_APK,
                UserHandle.of(ActivityManager.getCurrentUser()));
        if (mWifiDialogApkPkgName == null) {
            // WifiDialog APK not loaded yet, print a stack trace to see where this is called from
            Log.e(TAG, "Attempted to fetch WifiDialog apk before it is loaded!",
                    new IllegalStateException());
            return null;
        }
        Log.i(TAG, "Found Wifi Dialog APK at: " + mWifiDialogApkPkgName);
        return mWifiDialogApkPkgName;
    }

    /** Gets the package name of the apk responding to the given intent action */
    private String getApkPkgNameForAction(@NonNull String action, UserHandle userHandle) {

        List<ResolveInfo> resolveInfos;
        if (userHandle != null) {
            resolveInfos = getPackageManager().queryIntentActivitiesAsUser(
                    new Intent(action),
                    PackageManager.MATCH_SYSTEM_ONLY,
                    userHandle);
        } else {
            resolveInfos = getPackageManager().queryIntentActivities(
                    new Intent(action),
                    PackageManager.MATCH_SYSTEM_ONLY);
        }
        Log.i(TAG, "Got resolveInfos for " + action + ": " + resolveInfos);

        // remove apps that don't live in the Wifi apex
        resolveInfos.removeIf(info ->
                !Environment.isAppInWifiApex(info.activityInfo.applicationInfo));

        if (resolveInfos.isEmpty()) {
            return null;
        }

        if (resolveInfos.size() > 1) {
            // multiple apps found, log a warning, but continue
            Log.w(TAG, "Found > 1 APK that can resolve " + action + ": "
                    + resolveInfos.stream()
                    .map(info -> info.activityInfo.applicationInfo.packageName)
                    .collect(Collectors.joining(", ")));
        }

        // Assume the first ResolveInfo is the one we're looking for
        ResolveInfo info = resolveInfos.get(0);
        return info.activityInfo.applicationInfo.packageName;
    }

    private Context getResourcesApkContext() {
        try {
            String packageName = getWifiOverlayApkPkgName();
            if (packageName != null) {
                return createPackageContext(packageName, 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Failed to load resources", e);
        }
        return null;
    }

    /**
     * Retrieve assets held in the wifi resources APK.
     */
    @Override
    public AssetManager getAssets() {
        if (mWifiAssetsFromApk == null) {
            Context resourcesApkContext = getResourcesApkContext();
            if (resourcesApkContext != null) {
                mWifiAssetsFromApk = resourcesApkContext.getAssets();
            }
        }
        return mWifiAssetsFromApk;
    }

    /**
     * Retrieve resources held in the wifi resources APK.
     */
    @Override
    public Resources getResources() {
        if (mWifiResourcesFromApk == null) {
            Context resourcesApkContext = getResourcesApkContext();
            if (resourcesApkContext != null) {
                mWifiResourcesFromApk = resourcesApkContext.getResources();
            }
        }
        return mWifiResourcesFromApk;
    }

    /**
     * Retrieve theme held in the wifi resources APK.
     */
    @Override
    public Resources.Theme getTheme() {
        if (mWifiThemeFromApk == null) {
            Context resourcesApkContext = getResourcesApkContext();
            if (resourcesApkContext != null) {
                mWifiThemeFromApk = resourcesApkContext.getTheme();
            }
        }
        return mWifiThemeFromApk;
    }

    /** Get the package name that service-wifi runs under. */
    public String getServiceWifiPackageName() {
        return SERVICE_WIFI_PACKAGE_NAME;
    }

    /**
     * Reset the resource cache which will cause it to be reloaded next time it is accessed.
     */
    public void resetResourceCache() {
        mWifiOverlayApkPkgName = null;
        mWifiAssetsFromApk = null;
        mWifiResourcesFromApk = null;
        mWifiThemeFromApk = null;
    }

    /**
     * Returns an instance of WifiStringResourceWrapper with the given subId and carrierId.
     */
    public WifiStringResourceWrapper getStringResourceWrapper(int subId, int carrierId) {
        return new WifiStringResourceWrapper(this, subId, carrierId);
    }
}
