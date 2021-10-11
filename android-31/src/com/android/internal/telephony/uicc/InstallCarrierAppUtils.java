/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.util.NotificationChannelController;

import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for installing the carrier app when a SIM is insterted without the carrier app
 * for that SIM installed.
 */
@VisibleForTesting
public class InstallCarrierAppUtils {
    // TODO(b/72714040) centralize notification IDs
    private static final int ACTIVATE_CELL_SERVICE_NOTIFICATION_ID = 12;
    private static CarrierAppInstallReceiver sCarrierAppInstallReceiver = null;

    static void showNotification(Context context, String pkgName) {
        Resources res = Resources.getSystem();
        String title = res.getString(
                com.android.internal.R.string.install_carrier_app_notification_title);
        String appName = getAppNameFromPackageName(context, pkgName);
        String message;
        if (TextUtils.isEmpty(appName)) {
            message = res.getString(R.string.install_carrier_app_notification_text);
        } else {
            message = res.getString(R.string.install_carrier_app_notification_text_app_name,
                    appName);
        }
        String downloadButtonText = res.getString(R.string.install_carrier_app_notification_button);

        boolean persistent = Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.INSTALL_CARRIER_APP_NOTIFICATION_PERSISTENT, 1) == 1;

        PendingIntent goToStore = PendingIntent.getActivity(context, 0,
                getPlayStoreIntent(pkgName), PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        Notification.Action goToStoreAction =
                new Notification.Action.Builder(null, downloadButtonText, goToStore).build();

        Notification notification = new Notification.Builder(context,
                NotificationChannelController.CHANNEL_ID_SIM)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_signal_cellular_alt_24px)
                .addAction(goToStoreAction)
                .setOngoing(persistent)
                .setVisibility(Notification.VISIBILITY_SECRET) // Should not appear on lock screen
                .build();

        getNotificationManager(context).notify(
                pkgName,
                ACTIVATE_CELL_SERVICE_NOTIFICATION_ID,
                notification);
    }

    static void hideAllNotifications(Context context) {
        NotificationManager notificationManager = getNotificationManager(context);
        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();

        if (activeNotifications == null) {
            return;
        }

        for (StatusBarNotification notification : activeNotifications) {
            if (notification.getId() == ACTIVATE_CELL_SERVICE_NOTIFICATION_ID) {
                notificationManager.cancel(notification.getTag(), notification.getId());
            }
        }
    }

    static void hideNotification(Context context, String pkgName) {
        getNotificationManager(context).cancel(pkgName, ACTIVATE_CELL_SERVICE_NOTIFICATION_ID);
    }

    static Intent getPlayStoreIntent(String pkgName) {
        // Open play store to download package
        Intent storeIntent = new Intent(Intent.ACTION_VIEW);
        storeIntent.setData(Uri.parse("market://details?id=" + pkgName));
        storeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return storeIntent;
    }

    static void showNotificationIfNotInstalledDelayed(Context context,
            String pkgName, long delayMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                ShowInstallAppNotificationReceiver.get(context, pkgName),
                PendingIntent.FLAG_IMMUTABLE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + delayMillis,
                pendingIntent);

    }

    static void registerPackageInstallReceiver(Context context) {
        if (sCarrierAppInstallReceiver == null) {
            sCarrierAppInstallReceiver = new CarrierAppInstallReceiver();
            context = context.getApplicationContext();
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            intentFilter.addDataScheme("package");
            context.registerReceiver(sCarrierAppInstallReceiver, intentFilter);
        }
    }

    static void unregisterPackageInstallReceiver(Context context) {
        if (sCarrierAppInstallReceiver == null) {
            return;
        }
        context = context.getApplicationContext();
        context.unregisterReceiver(sCarrierAppInstallReceiver);
        sCarrierAppInstallReceiver = null;
    }

    static boolean isPackageInstallNotificationActive(Context context) {
        StatusBarNotification[] activeNotifications =
                getNotificationManager(context).getActiveNotifications();

        for (StatusBarNotification notification : activeNotifications) {
            if (notification.getId() == ACTIVATE_CELL_SERVICE_NOTIFICATION_ID) {
                return true;
            }
        }
        return false;
    }

    static String getAppNameFromPackageName(Context context, String packageName) {
        String whitelistSetting = Settings.Global.getString(
                context.getContentResolver(),
                Settings.Global.CARRIER_APP_NAMES);
        return getAppNameFromPackageName(packageName, whitelistSetting);
    }

    /**
     * @param packageName the name of the package.  Will be used as a key into the map
     * @param mapString map of package name to application name in the format:
     *                  packageName1:appName1;packageName2:appName2;
     * @return the name of the application for the package name provided.
     */
    @VisibleForTesting
    public static String getAppNameFromPackageName(String packageName, String mapString) {
        packageName = packageName.toLowerCase();
        final String pairDelim = "\\s*;\\s*";
        final String keyValueDelim = "\\s*:\\s*";

        if (TextUtils.isEmpty(mapString)) {
            return null;
        }

        List<String> keyValuePairList = Arrays.asList(mapString.split(pairDelim));

        if (keyValuePairList.isEmpty()) {
            return null;
        }

        for (String keyValueString: keyValuePairList) {
            String[] keyValue = keyValueString.split(keyValueDelim);

            if (keyValue.length == 2) {
                if (keyValue[0].equals(packageName)) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }
    private static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
