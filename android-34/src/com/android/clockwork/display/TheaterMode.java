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

package com.android.clockwork.display;

import static com.android.wearable.resources.R.string.theater_mode_off_context;
import static com.android.wearable.resources.R.string.theater_mode_off_title;

import android.annotation.Nullable;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.clockwork.common.WearResourceUtil;
import com.android.clockwork.remote.Home;
import com.android.internal.annotations.VisibleForTesting;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TheaterMode {
    private static final String TAG = "TheaterMode";
    private static final String ACTION_EXIT = "com.google.android.clockwork.settings.hide";

    @VisibleForTesting
    static final long EXIT_DELAY_MS = TimeUnit.HOURS.toMillis(24); // 24 hours in millis

    private static final String THEATER_MODE_CHANNEL = "theater_mode_channel";
    private static final int THEATER_MODE_NOTIFICATION_ID = 0x2028;

    private static final String THEATER_MODE_START_TIME_PREFERENCE =
            "theater_mode_start_time_preference";
    @VisibleForTesting
    static final String THEATER_MODE_START_TIME = "theater_mode_start_time";

    private final Context mContext;
    private final PowerManager mPowerManager;
    private final AlarmManager mAlarmMgr;
    @VisibleForTesting
    SharedPreferences mSharedPreferences;

    @VisibleForTesting
    PendingIntent mExitIntent;
    private ExitReceiver mExitReceiver;
    private IntentFilter mScreenStateAndExitFilter;
    private final ContentObserver mCoolDownModeObserver;
    private boolean mIsInCDM;

    @VisibleForTesting
    TheaterModeSettingsObserver mTheaterModeObserver;

    // Represents if theater mode is wanted.
    // True doesn't necessarily mean theater mode has been configured(started) already.
    private boolean mIsUserSettingEnabled;

    // Represents if theater mode has been configured(started).
    private boolean mIsTheaterModeRunning;

    @Nullable
    private String mOffTitleText;
    @Nullable
    private String mOffContentText;

    public TheaterMode(Context context) {
        this(context, null, null);
    }

    @VisibleForTesting
    TheaterMode(
            Context context, @Nullable String offTitleText, @Nullable String offContentText) {
        this(context, offTitleText, offContentText, null);
    }

    @VisibleForTesting
    TheaterMode(Context context, @Nullable String offTitleText, @Nullable String offContentText,
            @Nullable SharedPreferences sharedPreferences) {
        mContext = context;
        mPowerManager = mContext.getSystemService(PowerManager.class);
        mSharedPreferences = sharedPreferences == null ? getSharedPreferences(mContext)
                : sharedPreferences;

        mOffTitleText = offTitleText;
        mOffContentText = offContentText;

        // Set up alarm manager.
        mAlarmMgr = mContext.getSystemService(AlarmManager.class);

        mExitReceiver = new ExitReceiver();
        mScreenStateAndExitFilter = new IntentFilter(ACTION_EXIT);

        ContentResolver contentResolver = mContext.getContentResolver();

        // Set up content observer.
        mTheaterModeObserver =
                new TheaterModeSettingsObserver(new Handler(Looper.getMainLooper()));
        contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.THEATER_MODE_ON),
                /* notifyForDescendants= */ false,
                mTheaterModeObserver);

        mCoolDownModeObserver =
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        mIsInCDM =
                                Settings.Global.getInt(
                                                mContext.getContentResolver(),
                                                Settings.Global.Wearable.COOLDOWN_MODE_ON,
                                                0)
                                        == 1;
                    }
                };
        contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.Wearable.COOLDOWN_MODE_ON),
                /* notifyForDescendants= */ false,
                mCoolDownModeObserver);

        // Upon reboot, restore theater mode if it's set by user or end it if expired.
        mIsUserSettingEnabled = Settings.Global.getInt(contentResolver,
                Settings.Global.THEATER_MODE_ON, 0) == 1;
        if (!mIsUserSettingEnabled) {
            if (Log.isLoggable(TAG, Log.DEBUG) || !Build.TYPE.equals("user")) {
                Log.d(TAG, "theater mode is disabled. Not initializing.");
            }
            return;
        }
        long expectedTheaterModeAlarm = getExpectedTheaterModeEndTime();
        if (expectedTheaterModeAlarm < System.currentTimeMillis()) {
            if (Log.isLoggable(TAG, Log.DEBUG) || !Build.TYPE.equals("user")) {
                Log.d(TAG, "theater mode was enabled but 24h time limited is exceeded. "
                        + "Initializing to off.");
            }
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.THEATER_MODE_ON,
                    0);
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG) || !Build.TYPE.equals("user")) {
                Log.d(TAG, "theater mode was enabled. Initializing to on.");
            }
            startTheaterMode(expectedTheaterModeAlarm);
        }
    }

    private void startTheaterMode(long theaterModeAlarmTime) {
        if (mIsTheaterModeRunning) {
            return;
        }

        if (Log.isLoggable(TAG, Log.DEBUG) || !Build.TYPE.equals("user")) {
            Log.d(TAG, String.format("Configuring theater mode: %b", mIsUserSettingEnabled));
        }

        mPowerManager.wakeUp(SystemClock.uptimeMillis(), "startTheaterMode");

        // Listen to screen changes.
        mContext.registerReceiver(mExitReceiver, mScreenStateAndExitFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);

        // Listen to explicit exit theater mode.
        Intent intent = new Intent(ACTION_EXIT);
        intent.setPackage(mContext.getPackageName());
        mExitIntent =
                PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set up alarm to exit theater mode.
        mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP, theaterModeAlarmTime, mExitIntent);
        // Check if the device is in Cool Down Mode
        if (!mIsInCDM) {
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }

        mIsTheaterModeRunning = true;
    }

    private void endTheaterMode() {
        mPowerManager.wakeUp(SystemClock.uptimeMillis());

        // Enable touch, in case it was locked.
        mContext.sendBroadcast(new Intent(Home.ACTION_ENABLE_TOUCH_THEATER_MODE_END));

        if (!mIsTheaterModeRunning) {
            return;
        }
        if (Log.isLoggable(TAG, Log.DEBUG) || !Build.TYPE.equals("user")) {
            Log.d(TAG, String.format("Ending running theater mode: %b", mIsUserSettingEnabled));
        }

        // Unregister broadcast receiver.
        mContext.unregisterReceiver(mExitReceiver);

        // Cancel alarm.
        mAlarmMgr.cancel(mExitIntent);

        mSharedPreferences.edit().remove(THEATER_MODE_START_TIME).apply();
        mIsTheaterModeRunning = false;
    }

    private long getExpectedTheaterModeEndTime() {
        long savedTheaterModeStartTime = mSharedPreferences.getLong(THEATER_MODE_START_TIME, -1);
        return savedTheaterModeStartTime + EXIT_DELAY_MS;
    }

    @VisibleForTesting
    static SharedPreferences getSharedPreferences(Context context) {
        try {
            File prefsFile = new File(
                    Environment.getDataSystemDeDirectory(UserHandle.USER_SYSTEM),
                    THEATER_MODE_START_TIME_PREFERENCE);
            return context.createDeviceProtectedStorageContext()
                    .getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
        } catch (Throwable e) {
            Log.e(TAG, "Error loading shared prefs", e);
            return null;
        }
    }

    private void showNotification() {
        if (Log.isLoggable(TAG, Log.DEBUG) || !Build.TYPE.equals("user")) {
            Log.d(TAG, "Creating theater mode ended notification");
        }
        NotificationManager manager = mContext.getSystemService(NotificationManager.class);
        NotificationChannel channel =
                new NotificationChannel(
                        THEATER_MODE_CHANNEL,
                        "Theater Mode Notifier",
                        NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(channel);
        Notification.Builder builder =
                new Notification.Builder(mContext, THEATER_MODE_CHANNEL)
                        .setCategory(Notification.CATEGORY_ERROR)
                        .setLocalOnly(true)
                        .setContentTitle(getTheaterModeOffTitle())
                        .setContentText(getTheaterModeOffContext())
                        .setSmallIcon(mContext.getApplicationInfo().icon);
        manager.notify(THEATER_MODE_NOTIFICATION_ID, builder.build());
    }

    private void onTheaterModeStateChanged() {
        if (Log.isLoggable(TAG, Log.DEBUG) || !Build.TYPE.equals("user")) {
            Log.d(TAG, "Theater mode changed: "
                    + (mIsUserSettingEnabled ? "on" : "off"));
        }
        if (mIsUserSettingEnabled) {
            mSharedPreferences.edit().putLong(THEATER_MODE_START_TIME,
                    System.currentTimeMillis()).apply();
            startTheaterMode(getExpectedTheaterModeEndTime());
        } else {
            endTheaterMode();
        }
    }

    @VisibleForTesting
    String getTheaterModeOffTitle() {
        if (mOffTitleText == null) {
            mOffTitleText = Objects.requireNonNull(
                    WearResourceUtil.getWearableResources(mContext)).getString(
                    theater_mode_off_title);
        }
        return mOffTitleText;
    }

    @VisibleForTesting
    String getTheaterModeOffContext() {
        if (mOffContentText == null) {
            mOffContentText = Objects.requireNonNull(
                    WearResourceUtil.getWearableResources(mContext)).getString(
                    theater_mode_off_context);
        }
        return mOffContentText;
    }

    @VisibleForTesting
    final class TheaterModeSettingsObserver extends ContentObserver {
        private TheaterModeSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mIsUserSettingEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.THEATER_MODE_ON, 0) == 1;
            onTheaterModeStateChanged();
        }
    }

    /** Receiver to capture when exit timer has fired. */
    private class ExitReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable(TAG, Log.DEBUG) || !Build.TYPE.equals("user")) {
                Log.d(TAG, String.format("received action: %s", intent.getAction()));
            }
            if (ACTION_EXIT.equals(intent.getAction())) {
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.THEATER_MODE_ON, 0);
                showNotification();
            }
        }
    }
}
