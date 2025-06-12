/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.server;

import java.util.Calendar;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import java.util.concurrent.TimeUnit;

public class MountServiceIdler extends JobService {
    private static final String TAG = "MountServiceIdler";

    private static ComponentName sIdleService =
            new ComponentName("android", MountServiceIdler.class.getName());

    private static int MOUNT_JOB_ID = 808;

    private boolean mStarted;
    private JobParameters mJobParams;
    private Runnable mFinishCallback = new Runnable() {
        @Override
        public void run() {
            Slog.i(TAG, "Got mount service completion callback");
            synchronized (mFinishCallback) {
                if (mStarted) {
                    jobFinished(mJobParams, false);
                    mStarted = false;
                }
            }
            // ... and try again right away or tomorrow
            scheduleIdlePass(MountServiceIdler.this);
        }
    };

    @Override
    public boolean onStartJob(JobParameters params) {
        // First have the activity manager do its idle maintenance.  (Yes this job
        // is really more than just mount, some day it should be renamed to be system
        // idleer).
        try {
            ActivityManager.getService().performIdleMaintenance();
        } catch (RemoteException e) {
        }
        // The mount service will run an fstrim operation asynchronously
        // on a designated separate thread, so we provide it with a callback
        // that lets us cleanly end our idle timeslice.  It's safe to call
        // finishIdle() from any thread.
        mJobParams = params;
        StorageManagerService ms = StorageManagerService.sSelf;
        if (ms != null) {
            synchronized (mFinishCallback) {
                mStarted = true;
            }
            ms.runIdleMaint(mFinishCallback);
        }
        return ms != null;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Once we kick off the fstrim we aren't actually interruptible; just note
        // that we don't need to call jobFinished(), and let everything happen in
        // the callback from the mount service.
        StorageManagerService ms = StorageManagerService.sSelf;
        if (ms != null) {
            ms.abortIdleMaint(mFinishCallback);
            synchronized (mFinishCallback) {
                mStarted = false;
            }
        }
        return false;
    }

    /**
     * Schedule the idle job that will ping the mount service
     */
    public static void scheduleIdlePass(Context context) {
        JobScheduler tm = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        final long today3AM = offsetFromTodayMidnight(0, 3).getTimeInMillis();
        final long today4AM = offsetFromTodayMidnight(0, 4).getTimeInMillis();
        final long tomorrow3AM = offsetFromTodayMidnight(1, 3).getTimeInMillis();

        long nextScheduleTime;
        if (System.currentTimeMillis() > today3AM && System.currentTimeMillis() < today4AM) {
            nextScheduleTime = TimeUnit.SECONDS.toMillis(10);
        } else {
            nextScheduleTime = tomorrow3AM - System.currentTimeMillis(); // 3AM tomorrow
        }

        JobInfo.Builder builder = new JobInfo.Builder(MOUNT_JOB_ID, sIdleService);
        builder.setRequiresDeviceIdle(true);
        builder.setRequiresBatteryNotLow(true);
        builder.setRequiresCharging(true);
        builder.setMinimumLatency(nextScheduleTime);
        tm.schedule(builder.build());
    }

    private static Calendar offsetFromTodayMidnight(int nDays, int nHours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, nHours);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, nDays);
        return calendar;
    }
}
