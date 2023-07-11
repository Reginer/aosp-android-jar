package com.android.clockwork.common;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Cell Only Mode is a temporary mode which forces cellular to be the only
 * enabled radio.
 *
 * This is used for eSIM activation scenarios where the device must attach
 * and connect to the cell network in order to get a SIM OTA.
 *
 * CellOnlyMode automatically disables itself after a configured time or
 * if none is configured, after a default time has elapsed.
 *
 * To use this feature from a system app:
 *  1. Optionally set Settings.System.clockwork_cell_only_mode_duration_seconds
 *  2. Write "1" to Settings.System.clockwork_cell_only_mode
 */
public class CellOnlyMode {

    private static final String TAG = "WearConnectivityService";

    public interface Listener {
        void onCellOnlyModeChanged(boolean enabled);
    }

    /**
     * valid values for this key are 0 and 1
     */
    static final String CELL_ONLY_MODE_SETTING_KEY = "clockwork_cell_only_mode";

    static final String CELL_ONLY_MODE_DURATION_SECONDS_SETTING_KEY =
            "clockwork_cell_only_mode_duration_seconds";

    static final String ACTION_EXIT_CELL_ONLY_MODE =
            "com.android.clockwork.connectivity.action.ACTION_EXIT_CELL_ONLY_MODE";

    /**
     * If no duration is specified in the setting, use this value.
     */
    static final long DEFAULT_CELL_ONLY_MODE_DURATION_MS = TimeUnit.MINUTES.toMillis(10);
    /**
     * If duration is specified in the setting exceeds this value, use this value instead.
     */
    static final long MAX_CELL_ONLY_MODE_DURATION_MS = TimeUnit.MINUTES.toMillis(30);

    /** Do not delay at all when turning the radio back off. */
    static final long EXIT_CELL_ONLY_LINGER_DURATION_MS = 0;

    private final Context mContext;
    private final AlarmManager mAlarmManager;
    private final ContentResolver mContentResolver;
    private final SettingsObserver mSettingsObserver;
    private final Set<Listener> mListeners;
    private boolean mCellOnlyModeEnabled;


    @VisibleForTesting final PendingIntent exitCellOnlyModeIntent;
    @VisibleForTesting
    BroadcastReceiver exitCellOnlyModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_EXIT_CELL_ONLY_MODE.equals(intent.getAction())) {
                Log.d(TAG, "Alarm received. Exiting cell only mode.");
                resetCellOnlyMode();
                for (Listener listener : mListeners) {
                    listener.onCellOnlyModeChanged(false);
                }
            }
        }
    };

    public CellOnlyMode(Context context,
                        ContentResolver contentResolver,
                        AlarmManager alarmManager) {
        mContext = context;
        mAlarmManager = alarmManager;
        mContentResolver = contentResolver;
        mCellOnlyModeEnabled = false;
        mListeners = new HashSet<>();
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
        mContentResolver.registerContentObserver(
                Settings.System.getUriFor(CELL_ONLY_MODE_SETTING_KEY), false, mSettingsObserver);

        exitCellOnlyModeIntent =
                PendingIntent.getBroadcast(
                        context,
                        0,
                        new Intent(ACTION_EXIT_CELL_ONLY_MODE),
                        PendingIntent.FLAG_IMMUTABLE);
    }

    @VisibleForTesting
    SettingsObserver getSettingsObserver() {
        return mSettingsObserver;
    }

    /**
     * If CellOnlyMode was enabled but not reset before the device power cycled, ensure that
     * it is turned off immediately.
     */
    public void onBootCompleted() {
        mContext.registerReceiver(exitCellOnlyModeReceiver,
                new IntentFilter(ACTION_EXIT_CELL_ONLY_MODE),
                Context.RECEIVER_NOT_EXPORTED);

        resetCellOnlyMode();
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public boolean isCellOnlyModeEnabled() {
        return mCellOnlyModeEnabled;
    }

    @VisibleForTesting
    final class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(CELL_ONLY_MODE_SETTING_KEY))) {
                mCellOnlyModeEnabled = fetchCellOnlyModeEnabled();
                Log.d(TAG, "CellOnlyMode Settings changed: " + mCellOnlyModeEnabled);

                for (Listener listener : mListeners) {
                    listener.onCellOnlyModeChanged(mCellOnlyModeEnabled);
                }

                if (mCellOnlyModeEnabled) {
                    mAlarmManager.setWindow(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + getCellOnlyModeDuration(),
                            EXIT_CELL_ONLY_LINGER_DURATION_MS,
                            exitCellOnlyModeIntent);
                } else {
                    mAlarmManager.cancel(exitCellOnlyModeIntent);
                }
            }
        }
    }

    private void resetCellOnlyMode() {
        mCellOnlyModeEnabled = false;
        Settings.System.putInt(mContentResolver, CELL_ONLY_MODE_SETTING_KEY, 0);
    }

    private boolean fetchCellOnlyModeEnabled() {
        return Settings.System.getInt(mContentResolver, CELL_ONLY_MODE_SETTING_KEY, 0) != 0;
    }

    @VisibleForTesting
    long getCellOnlyModeDuration() {
        int settingsDurationSeconds = Settings.System.getInt(mContentResolver, CELL_ONLY_MODE_DURATION_SECONDS_SETTING_KEY, -1);

        if (settingsDurationSeconds <= 0) {
            return DEFAULT_CELL_ONLY_MODE_DURATION_MS;
        }
        return Math.min(TimeUnit.SECONDS.toMillis(settingsDurationSeconds), MAX_CELL_ONLY_MODE_DURATION_MS);
    }
}
