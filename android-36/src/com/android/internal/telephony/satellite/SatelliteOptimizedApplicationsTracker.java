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

package com.android.internal.telephony.satellite;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.internal.telephony.PackageChangeReceiver;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SatelliteOptimizedApplicationsTracker will track the packages if they are Satellite optimized or
 * not. According to this it is maintain a cache which store data about that packages.
 */
public class SatelliteOptimizedApplicationsTracker {

    @NonNull private final Map<Integer, Set<String>> mSatelliteApplications = new HashMap<>();

    /** Action used to initialize the state of the Tracker. */
    private static final int ACTION_INITIALIZE_TRACKER = 0;

    private static final int ACTION_USER_ADDED = 1;
    private static final int ACTION_PACKAGE_ADDED = 2;
    private static final int ACTION_PACKAGE_UPDATED = 3;
    private static final int ACTION_PACKAGE_MODIFIED = 4;
    private static final int ACTION_PACKAGE_REMOVED = 5;

    private static final String APP_PROPERTY =
            "android.telephony.PROPERTY_SATELLITE_DATA_OPTIMIZED";
    private static final String TAG = "SatelliteAppTracker";

    private final Context mContext;
    private PackageManager mPackageManager;
    private final UserManager mUserManager;
    private volatile Handler mCurrentHandler;

    public SatelliteOptimizedApplicationsTracker(@NonNull Looper looper, @NonNull Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mCurrentHandler =
                new Handler(looper) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        switch (msg.what) {
                            case ACTION_INITIALIZE_TRACKER, ACTION_USER_ADDED -> {
                                log("HandleInitializeTracker() STARTED");
                                handleInitializeTracker();
                            }
                            case ACTION_PACKAGE_ADDED,
                                ACTION_PACKAGE_UPDATED,
                                ACTION_PACKAGE_MODIFIED-> {
                                    String packageName = (String) msg.obj;
                                    handlePackageMonitor(packageName);
                            }
                            case ACTION_PACKAGE_REMOVED -> {
                                String packageName = (String) msg.obj;
                                handlePackageRemoved(packageName);
                            }
                        }
                    }
                };
        mCurrentHandler.sendMessage(mCurrentHandler.obtainMessage(ACTION_INITIALIZE_TRACKER));
        Context mReceiverContext =
                context.createContextAsUser(UserHandle.ALL, PackageManager.GET_META_DATA);
        mReceiverContext.registerReceiver(
                mBootCompleted, new IntentFilter(Intent.ACTION_USER_ADDED));
        PackageChangeReceiver packageMonitor = new SatelliteApplicationPackageMonitor();
        packageMonitor.register(context, mCurrentHandler.getLooper(), UserHandle.ALL);
    }

    private final BroadcastReceiver mBootCompleted =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    loge("new user added");
                    // Recalculate all cached services to pick up ones that have just been enabled
                    // since new user is added
                    mCurrentHandler.obtainMessage(ACTION_USER_ADDED, null).sendToTarget();
                }
            };

    private ApplicationInfo getApplicationInfo(String packageName) {
        mPackageManager = mContext.getPackageManager();
        try {
            PackageInfo packageInfo =
                    mPackageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            if (packageInfo == null) {
                loge("packageInfo is NULL");
                return null;
            }
            return packageInfo.applicationInfo;
        } catch (PackageManager.NameNotFoundException exp) {
            loge(
                    "Exception while reading packageInfo [ "
                            + packageName
                            + " ] exp = "
                            + exp.getMessage());
            return null;
        }
    }

    private class SatelliteApplicationPackageMonitor extends PackageChangeReceiver {
        @Override
        public void onPackageAdded(String packageName) {
            log("onPackageAdded : " + packageName);
            mCurrentHandler.obtainMessage(ACTION_PACKAGE_ADDED, packageName).sendToTarget();
        }

        @Override
        public void onPackageRemoved(String packageName) {
            log("onPackageRemoved : " + packageName);
            mCurrentHandler.obtainMessage(ACTION_PACKAGE_REMOVED, packageName).sendToTarget();
        }

        @Override
        public void onPackageUpdateFinished(String packageName) {
            log("onPackageUpdateFinished : " + packageName);
            mCurrentHandler.obtainMessage(ACTION_PACKAGE_UPDATED, packageName).sendToTarget();
        }

        @Override
        public void onPackageModified(String packageName) {
            log("onPackageModified : " + packageName);
            mCurrentHandler.obtainMessage(ACTION_PACKAGE_MODIFIED, packageName).sendToTarget();
        }
    }

    private void handlePackageRemoved(String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo != null && isOptimizedSatelliteApplication(applicationInfo)) {
            removeCacheOptimizedSatelliteApplication(packageName);
        }
    }

    private void handlePackageMonitor(String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo != null) {
            if (isOptimizedSatelliteApplication(applicationInfo)) {
                addCacheOptimizedSatelliteApplication(packageName);
            } else {
                removeCacheOptimizedSatelliteApplication(packageName);
            }
        }
    }

    private void handleInitializeTracker() {
        List<UserInfo> users = mUserManager.getUsers();
        for (UserInfo user : users) {
            int userId = user.getUserHandle().getIdentifier();
            mSatelliteApplications.put(userId, new HashSet<>());
        }
        // Get a list of installed packages
        List<PackageInfo> packages =
                mPackageManager.getInstalledPackages(PackageManager.GET_META_DATA);
        // Iterate through the packages
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.applicationInfo != null
                    && isOptimizedSatelliteApplication(packageInfo.applicationInfo)) {
                addCacheOptimizedSatelliteApplication(packageInfo.packageName);
            }
        }
    }

    private boolean isOptimizedSatelliteApplication(@NonNull ApplicationInfo applicationInfo) {
        boolean flag = false;
        if (applicationInfo.metaData != null) {
            // Get the application's metadata
            Bundle metadata = applicationInfo.metaData;
            flag = metadata.containsKey(APP_PROPERTY);
        }
        return flag;
    }

    private void addCacheOptimizedSatelliteApplication(@NonNull String packageName) {
        List<UserInfo> users = mUserManager.getUsers();
        for (UserInfo user : users) {
            int userId = user.getUserHandle().getIdentifier();
            try {
                mPackageManager.getPackageUidAsUser(
                        packageName, PackageManager.GET_META_DATA, userId);
                mSatelliteApplications.get(userId).add(packageName);
            } catch (java.lang.Exception e) {
                // package is not present for current user
            }
        }
    }

    private void removeCacheOptimizedSatelliteApplication(@NonNull String packageName) {
        List<UserInfo> users = mUserManager.getUsers();
        for (UserInfo user : users) {
            int userId = user.getUserHandle().getIdentifier();
            try {
                mPackageManager.getPackageUidAsUser(
                        packageName, PackageManager.GET_META_DATA, userId);
            } catch (java.lang.Exception e) {
                // package is not present for current user
                if (mSatelliteApplications.get(userId).contains(packageName)) {
                    mSatelliteApplications.get(userId).remove(packageName);
                }
            }
        }
    }

    /**
     * Get list of applications that are optimized for low bandwidth satellite data.
     *
     * @param userId is Identifier of user
     *
     * @return List of applications package names with data optimized network property. {@link
     *     #PROPERTY_SATELLITE_DATA_OPTIMIZED}
     */
    public @NonNull List<String> getSatelliteOptimizedApplications(int userId) {
        return new ArrayList<>(mSatelliteApplications.get(userId));
    }

    private void log(String str) {
        Rlog.i(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }
}
