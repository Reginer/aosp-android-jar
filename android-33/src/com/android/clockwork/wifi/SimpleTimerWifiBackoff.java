package com.android.clockwork.wifi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of WifiBackoff that uses simple time-based rules for scheduling and
 * activating:
 *
 * - Backoff is entered on mBackoffDelayMs (default: 30m) after it's initially scheduled
 * - Backoff exits automatically after mBackoffDurationMs (default: 2h)
 */
public class SimpleTimerWifiBackoff implements WifiBackoff {
    private static final String TAG = "WearWifiMediator";

    private static final String WIFI_BACKOFF_DELAY_KEY = "cw_wifi_backoff_delay";
    private static final String WIFI_BACKOFF_DURATION_KEY = "cw_wifi_backoff_duration";

    private static final long DEFAULT_BACKOFF_DELAY = TimeUnit.MINUTES.toMillis(30);
    private static final long DEFAULT_BACKOFF_DURATION = TimeUnit.HOURS.toMillis(2);

    private static final String ACTION_ENTER_BACKOFF =
            "com.android.clockwork.wifi.ENTER_BACKOFF";
    private static final String ACTION_EXIT_BACKOFF =
            "com.android.clockwork.wifi.EXIT_BACKOFF";
    private final AlarmManager mAlarmManager;
    private final PendingIntent mEnterBackoffIntent;
    private final PendingIntent mExitBackoffIntent;
    private Listener mListener;

    private enum State {
        INACTIVE,
        BACKOFF_SCHEDULED,
        IN_BACKOFF
    }

    private long mBackoffDelayMs = DEFAULT_BACKOFF_DELAY;
    private long mBackoffDurationMs = DEFAULT_BACKOFF_DURATION;

    // use updateState to update this variable
    private State mState = State.INACTIVE;
    private long mLastStateChangedTime = 0;

    private final Context mContext;
    private final WifiLogger mWifiLogger;

    public SimpleTimerWifiBackoff(Context context, WifiLogger wifiLogger) {
        mContext = context;
        mWifiLogger = wifiLogger;
        mAlarmManager = context.getSystemService(AlarmManager.class);

        mEnterBackoffIntent =
                PendingIntent.getBroadcastAsUser(
                        context,
                        0,
                        new Intent(ACTION_ENTER_BACKOFF),
                        PendingIntent.FLAG_IMMUTABLE,
                        UserHandle.SYSTEM);
        mExitBackoffIntent =
                PendingIntent.getBroadcastAsUser(
                        context,
                        0,
                        new Intent(ACTION_EXIT_BACKOFF),
                        PendingIntent.FLAG_IMMUTABLE,
                        UserHandle.SYSTEM);

        IntentFilter intent = new IntentFilter();
        intent.addAction(ACTION_ENTER_BACKOFF);
        intent.addAction(ACTION_EXIT_BACKOFF);
        context.registerReceiver(mAlarmReceiver, intent,
                Context.RECEIVER_NOT_EXPORTED);

        WifiBackoffSettingsObserver observer = new WifiBackoffSettingsObserver(
                new Handler(Looper.getMainLooper()));
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(WIFI_BACKOFF_DELAY_KEY),
                false,
                observer);
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(WIFI_BACKOFF_DURATION_KEY),
                false,
                observer);

        fetchWifiBackoffSettings();
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public boolean isInBackoff() {
        return State.IN_BACKOFF.equals(mState);
    }

    /**
     * Must be idempotent and should not schedule an alarm if one is already set,
     * or if already in backoff.
     */
    @Override
    public void scheduleBackoff() {
        switch (mState) {
            case INACTIVE:
                Log.d(TAG, "Scheduling wifi backoff");
                mAlarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + mBackoffDelayMs,
                        mEnterBackoffIntent);
                updateState(State.BACKOFF_SCHEDULED);
                break;
            case BACKOFF_SCHEDULED:
                break;
            case IN_BACKOFF:
                break;
            default:
                // pass
        }
    }

    /**
     * Must be idempotent.
     */
    @Override
    public void cancelBackoff() {
        switch (mState) {
            case INACTIVE:
                break;
            case BACKOFF_SCHEDULED:
                Log.d(TAG, "Scheduled Wifi Backoff canceled");
                mAlarmManager.cancel(mEnterBackoffIntent);
                updateState(State.INACTIVE);
                break;
            case IN_BACKOFF:
                Log.d(TAG, "Cancelled Wifi Backoff");
                mAlarmManager.cancel(mExitBackoffIntent);
                updateState(State.INACTIVE);
                break;
            default:
                // pass
        }
    }

    private void fetchWifiBackoffSettings() {
        mBackoffDelayMs = Settings.System.getLong(mContext.getContentResolver(),
                WIFI_BACKOFF_DELAY_KEY, DEFAULT_BACKOFF_DELAY);
        mBackoffDurationMs = Settings.System.getLong(mContext.getContentResolver(),
                WIFI_BACKOFF_DURATION_KEY, DEFAULT_BACKOFF_DURATION);
    }

    private void updateState(State newState) {
        State prevState = mState;
        mState = newState;
        if (prevState.equals(mState)) {
            return;
        }

        mLastStateChangedTime = System.currentTimeMillis();

        if (prevState.equals(State.IN_BACKOFF) || mState.equals(State.IN_BACKOFF)) {
            mListener.onWifiBackoffChanged();
        }
    }

    private final class WifiBackoffSettingsObserver extends ContentObserver {
        public WifiBackoffSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            fetchWifiBackoffSettings();
        }
    }

    private final BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_ENTER_BACKOFF:
                    if (!State.BACKOFF_SCHEDULED.equals(mState)) {
                        Log.w(TAG, "Got ENTER_BACKOFF alarm in state: " + mState.name());
                        return;
                    }
                    Log.d(TAG, "Entering Wifi Backoff");
                    mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + mBackoffDurationMs,
                            mExitBackoffIntent);
                    updateState(State.IN_BACKOFF);
                    mWifiLogger.recordWifiBackoffEvent();
                    break;
                case ACTION_EXIT_BACKOFF:
                    if (!State.IN_BACKOFF.equals(mState)) {
                        Log.w(TAG, "Got EXIT_BACKOFF alarm in state: " + mState.name());
                    }
                    Log.d(TAG, "Wifi Backoff expired.");
                    updateState(State.INACTIVE);
                    break;
                default:
                    // pass
            }
        }
    };

    @Override
    public void dump(IndentingPrintWriter ipw) {
        String lastUpdated = "0";
        if (mLastStateChangedTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
            lastUpdated = sdf.format(new Date(mLastStateChangedTime));
        }

        ipw.println("SimpleTimerWifiBackoff");
        ipw.printPair("Current state", mState.name());
        ipw.printPair("Last updated", lastUpdated);
    }
}
