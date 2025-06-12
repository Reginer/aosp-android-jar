/**
 * Copyright (c) 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app.usage;

import android.app.PendingIntent;
import android.app.usage.BroadcastResponseStats;
import android.app.usage.BroadcastResponseStatsList;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEventsQuery;
import android.content.pm.ParceledListSlice;
import android.os.PersistableBundle;

/**
 * System private API for talking with the UsageStatsManagerService.
 *
 * {@hide}
 */
interface IUsageStatsManager {
    @UnsupportedAppUsage(maxTargetSdk = 30, trackingBug = 170729553)
    ParceledListSlice queryUsageStats(int bucketType, long beginTime, long endTime,
            String callingPackage, int userId);
    @UnsupportedAppUsage(maxTargetSdk = 30, trackingBug = 170729553)
    ParceledListSlice queryConfigurationStats(int bucketType, long beginTime, long endTime,
            String callingPackage);
    ParceledListSlice queryEventStats(int bucketType, long beginTime, long endTime,
            String callingPackage);
    UsageEvents queryEvents(long beginTime, long endTime, String callingPackage);
    UsageEvents queryEventsForPackage(long beginTime, long endTime, String callingPackage);
    UsageEvents queryEventsForUser(long beginTime, long endTime, int userId, String callingPackage);
    UsageEvents queryEventsForPackageForUser(long beginTime, long endTime, int userId, String pkg, String callingPackage);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.PACKAGE_USAGE_STATS)")
    UsageEvents queryEventsWithFilter(in UsageEventsQuery query, String callingPackage);
    @UnsupportedAppUsage(maxTargetSdk = 30, trackingBug = 170729553)
    void setAppInactive(String packageName, boolean inactive, int userId);
    boolean isAppStandbyEnabled();
    @UnsupportedAppUsage(maxTargetSdk = 30, trackingBug = 170729553)
    boolean isAppInactive(String packageName, int userId, String callingPackage);
    void onCarrierPrivilegedAppsChanged();
    void reportChooserSelection(String packageName, int userId, String contentType,
            in String[] annotations, String action);
    int getAppStandbyBucket(String packageName, String callingPackage, int userId);
    @EnforcePermission("CHANGE_APP_IDLE_STATE")
    void setAppStandbyBucket(String packageName, int bucket, int userId);
    ParceledListSlice getAppStandbyBuckets(String callingPackage, int userId);
    @EnforcePermission("CHANGE_APP_IDLE_STATE")
    void setAppStandbyBuckets(in ParceledListSlice appBuckets, int userId);
    int getAppMinStandbyBucket(String packageName, String callingPackage, int userId);
    @EnforcePermission("CHANGE_APP_LAUNCH_TIME_ESTIMATE")
    void setEstimatedLaunchTime(String packageName, long estimatedLaunchTime, int userId);
    @EnforcePermission("CHANGE_APP_LAUNCH_TIME_ESTIMATE")
    void setEstimatedLaunchTimes(in ParceledListSlice appLaunchTimes, int userId);
    void registerAppUsageObserver(int observerId, in String[] packages, long timeLimitMs,
            in PendingIntent callback, String callingPackage);
    void unregisterAppUsageObserver(int observerId, String callingPackage);
    void registerUsageSessionObserver(int sessionObserverId, in String[] observed, long timeLimitMs,
            long sessionThresholdTimeMs, in PendingIntent limitReachedCallbackIntent,
            in PendingIntent sessionEndCallbackIntent, String callingPackage);
    void unregisterUsageSessionObserver(int sessionObserverId, String callingPackage);
    void registerAppUsageLimitObserver(int observerId, in String[] packages, long timeLimitMs,
            long timeUsedMs, in PendingIntent callback, String callingPackage);
    void unregisterAppUsageLimitObserver(int observerId, String callingPackage);
    void reportUsageStart(in IBinder activity, String token, String callingPackage);
    void reportPastUsageStart(in IBinder activity, String token, long timeAgoMs,
            String callingPackage);
    void reportUsageStop(in IBinder activity, String token, String callingPackage);
    void reportUserInteraction(String packageName, int userId);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.REPORT_USAGE_STATS)")
    void reportUserInteractionWithBundle(String packageName, int userId, in PersistableBundle eventExtras);
    int getUsageSource();
    void forceUsageSourceSettingRead();
    long getLastTimeAnyComponentUsed(String packageName, String callingPackage);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.ACCESS_BROADCAST_RESPONSE_STATS)")
    BroadcastResponseStatsList queryBroadcastResponseStats(
            String packageName, long id, String callingPackage, int userId);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.ACCESS_BROADCAST_RESPONSE_STATS)")
    void clearBroadcastResponseStats(String packageName, long id, String callingPackage,
            int userId);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.ACCESS_BROADCAST_RESPONSE_STATS)")
    void clearBroadcastEvents(String callingPackage, int userId);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.DUMP)")
    boolean isPackageExemptedFromBroadcastResponseStats(String packageName, int userId);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.READ_DEVICE_CONFIG)")
    String getAppStandbyConstant(String key);
}
