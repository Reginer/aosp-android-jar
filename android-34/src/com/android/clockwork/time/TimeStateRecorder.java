package com.android.clockwork.time;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.os.BackgroundThread;

/**
 * Records, on the filesystem, the last time the time was updated in order that non-java processes
 * can determine whether the time is inaccurate.
 */
public class TimeStateRecorder extends BroadcastReceiver implements Runnable {
    private static final String TAG = TimeState.class.getSimpleName();

    private static final boolean localLOGV = false;

    private static final long MIN_TIME_BETWEEN_WRITES = 60000;

    private long mLastTimeChangeWriteTime = 0;
    private long mLastTimeChangeClockTime = 0;

    private boolean mRecorded12HourMode = false;
    private boolean mSystem12HourMode = false;

    private TimeState mTimeState;
    private ContentResolver mContentResolver;

    boolean init(Context context, TimeState timeState) {
        if (localLOGV) {
            Log.v(TAG, "TimeStateRecorder initializing");
        }
        if (!timeState.init()) {
            return false;
        }
        mTimeState = timeState;
        mContentResolver = context.getContentResolver();

        mRecorded12HourMode = mTimeState.is12HourModeRecorded();
        mSystem12HourMode = !mTimeState.isSystem24HourFormat(context);

        sync12HourFormat();
        observeSetupWizard();

        return true;
    }

    private boolean isSetupCompleted() {
        final boolean setupWizardCompleted = Settings.System.getInt(
                mContentResolver, Settings.System.SETUP_WIZARD_HAS_RUN, 0) == 1;
        return setupWizardCompleted;
    }

    private void observeSetupWizard() {
        if (isSetupCompleted()) {
            return;
        }

        ContentObserver observer = new ContentObserver(BackgroundThread.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                mContentResolver.unregisterContentObserver(this);
                BackgroundThread.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (localLOGV) {
                           Log.v(TAG, "Recording last time change after the setup");
                        }
                        sync12HourFormat();
                        mTimeState.updateLastChangeValue();
                    }
                });
            }
        };

        mContentResolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.SETUP_WIZARD_HAS_RUN), false, observer);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (localLOGV) {
            Log.v(TAG, "Broadcast received");
        }

        if (!isSetupCompleted()) {
            if (localLOGV) {
               Log.v(TAG, "Postpone recording last time change during the setup");
            }
            return;
        }

        mLastTimeChangeClockTime = mTimeState.getCurrentTime();

        if (!intent.hasExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT) &&
            mLastTimeChangeClockTime < mLastTimeChangeWriteTime + MIN_TIME_BETWEEN_WRITES) {
            return;
        }
        mLastTimeChangeWriteTime = mLastTimeChangeClockTime;
        mSystem12HourMode = !mTimeState.isSystem24HourFormat(context);

        BackgroundThread.getHandler().post(this);
    }

    private void sync12HourFormat() {
        if (localLOGV) {
          Log.v(TAG, "Syncing 12 hour format");
        }
        if (mRecorded12HourMode != mSystem12HourMode) {
            if (localLOGV) {
                Log.v(TAG, "Updating 12 hour flag file");
            }
            boolean success;
            if (mSystem12HourMode) {
                success = mTimeState.set12HourMode();
                mRecorded12HourMode = true;
            } else {
                success = mTimeState.set24HourMode();
                mRecorded12HourMode = false;
            }
            if (!success) {
                Log.e(TAG, "Could not update recorded 12 hour state");
            }
        }
    }

    @Override
    public void run() {
        if (localLOGV) {
            Log.v(TAG, "Recording last time change");
        }

        sync12HourFormat();
        mTimeState.updateLastChangeValue();
    }
}
