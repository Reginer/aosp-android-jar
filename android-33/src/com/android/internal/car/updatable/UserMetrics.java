/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.car.updatable;

import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STARTING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STOPPED;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STOPPING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_SWITCHING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_UNLOCKED;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_UNLOCKING;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.car.builtin.util.Slogf;
import android.car.builtin.util.TimeUtils;
import android.util.SparseArray;

import com.android.car.internal.common.CommonConstants.UserLifecycleEventType;
import com.android.car.internal.util.IndentingPrintWriter;
import com.android.car.internal.util.LocalLog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Metrics for user switches.
 *
 * <p>It stores 2 types of metrics:
 *
 * <ol>
 *   <li>Time to start a user (from start to unlock)
 *   <li>Time to stop a user (from stop to shutdown)
 * </ol>
 *
 * <p>It keeps track of the users being started and stopped, then logs the last
 * {{@link #INITIAL_CAPACITY}} occurrences of each when the operation finished (so it can be dumped
 * later).
 */
final class UserMetrics {

    private static final String TAG = UserMetrics.class.getSimpleName();

    /**
     * Initial capacity for the current operations.
     */
    // Typically there are at most 2 users (system and 1st full), although it could be higher on
    // garage mode
    private static final int INITIAL_CAPACITY = 2;

    // TODO(b/150413515): read from resources
    private static final int LOG_SIZE = 10;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private SparseArray<UserStartingMetric> mUserStartingMetrics;
    @GuardedBy("mLock")
    private SparseArray<UserStoppingMetric> mUserStoppingMetrics;

    @GuardedBy("mLock")
    private final LocalLog mUserStartedLogs = new LocalLog(LOG_SIZE);
    @GuardedBy("mLock")
    private final LocalLog mUserStoppedLogs = new LocalLog(LOG_SIZE);

    /**
     * Logs a user lifecycle event.
     */
    public void onEvent(@UserLifecycleEventType int eventType, long timestampMs,
            @UserIdInt int fromUserId, @UserIdInt int toUserId) {
        synchronized (mLock) {
            switch(eventType) {
                case USER_LIFECYCLE_EVENT_TYPE_STARTING:
                    onUserStartingEventLocked(timestampMs, toUserId);
                    return;
                case USER_LIFECYCLE_EVENT_TYPE_SWITCHING:
                    onUserSwitchingEventLocked(timestampMs, fromUserId, toUserId);
                    return;
                case USER_LIFECYCLE_EVENT_TYPE_UNLOCKING:
                    onUserUnlockingEventLocked(timestampMs, toUserId);
                    return;
                case USER_LIFECYCLE_EVENT_TYPE_UNLOCKED:
                    onUserUnlockedEventLocked(timestampMs, toUserId);
                    return;
                case USER_LIFECYCLE_EVENT_TYPE_STOPPING:
                    onUserStoppingEventLocked(timestampMs, toUserId);
                    return;
                case USER_LIFECYCLE_EVENT_TYPE_STOPPED:
                    onUserStoppedEventLocked(timestampMs, toUserId);
                    return;
                default:
                    Slogf.w(TAG, "Invalid event: " + eventType);
            }
        }
    }

    @VisibleForTesting
    SparseArray<UserStartingMetric> getUserStartMetrics() {
        synchronized (mLock) {
            return mUserStartingMetrics;
        }
    }

    @VisibleForTesting
    SparseArray<UserStoppingMetric> getUserStopMetrics() {
        synchronized (mLock) {
            return mUserStoppingMetrics;
        }
    }

    private void onUserStartingEventLocked(long timestampMs, @UserIdInt int userId) {
        if (mUserStartingMetrics == null) {
            mUserStartingMetrics = new SparseArray<>(INITIAL_CAPACITY);
        }

        UserStartingMetric existingMetrics = mUserStartingMetrics.get(userId);
        if (existingMetrics != null) {
            Slogf.w(TAG, "user re-started: " + existingMetrics);
            finishUserStartingLocked(existingMetrics, /* removeMetric= */ false);
        }

        mUserStartingMetrics.put(userId, new UserStartingMetric(userId, timestampMs));
    }

    private void onUserSwitchingEventLocked(long timestampMs, @UserIdInt int fromUserId,
            @UserIdInt int toUserId) {
        UserStartingMetric metrics = getExistingMetricsLocked(mUserStartingMetrics, toUserId);
        if (metrics == null) return;

        metrics.switchFromUserId = fromUserId;
        metrics.switchTime = timestampMs;
    }

    private void onUserUnlockingEventLocked(long timestampMs, @UserIdInt int userId) {
        UserStartingMetric metrics = getExistingMetricsLocked(mUserStartingMetrics, userId);
        if (metrics == null) return;

        metrics.unlockingTime = timestampMs;
    }

    private void onUserUnlockedEventLocked(long timestampMs, @UserIdInt int userId) {
        UserStartingMetric metrics = getExistingMetricsLocked(mUserStartingMetrics, userId);
        if (metrics == null) return;

        metrics.unlockedTime = timestampMs;

        finishUserStartingLocked(metrics, /* removeMetric= */ true);
    }

    private void onUserStoppingEventLocked(long timestampMs, @UserIdInt int userId) {
        if (mUserStoppingMetrics == null) {
            mUserStoppingMetrics = new SparseArray<>(INITIAL_CAPACITY);
        }
        UserStoppingMetric existingMetrics = mUserStoppingMetrics.get(userId);
        if (existingMetrics != null) {
            Slogf.w(TAG, "user re-stopped: " + existingMetrics);
            finishUserStoppingLocked(existingMetrics, /* removeMetric= */ false);
        }
        mUserStoppingMetrics.put(userId, new UserStoppingMetric(userId, timestampMs));
    }

    private void onUserStoppedEventLocked(long timestampMs, @UserIdInt int userId) {
        UserStoppingMetric metrics = getExistingMetricsLocked(mUserStoppingMetrics, userId);
        if (metrics == null) return;

        metrics.shutdownTime = timestampMs;
        finishUserStoppingLocked(metrics, /* removeMetric= */ true);
    }

    @Nullable
    private <T extends BaseUserMetric> T getExistingMetricsLocked(
            @NonNull SparseArray<? extends BaseUserMetric> metrics, @UserIdInt int userId) {
        if (metrics == null) {
            Slogf.w(TAG, "getExistingMetricsLocked() should not pass null metrics, except on "
                    + "tests");
            return null;
        }
        @SuppressWarnings("unchecked")
        T metric = (T) metrics.get(userId);
        if (metric == null) {
            String name = metrics == mUserStartingMetrics ? "starting" : "stopping";
            Slogf.w(TAG, "no " + name + " metrics for user " + userId);
        }
        return metric;
    }

    private void removeExistingMetricsLogged(@NonNull SparseArray<? extends BaseUserMetric> metrics,
            @UserIdInt int userId) {
        metrics.remove(userId);
        if (metrics.size() != 0) return;

        if (metrics == mUserStartingMetrics) {
            mUserStartingMetrics = null;
        } else {
            mUserStoppingMetrics = null;
        }
    }

    private void finishUserStartingLocked(@NonNull UserStartingMetric metrics,
            boolean removeMetric) {
        mUserStartedLogs.log(metrics.toString());
        if (removeMetric) {
            removeExistingMetricsLogged(mUserStartingMetrics, metrics.userId);
        }
    }

    private void finishUserStoppingLocked(@NonNull UserStoppingMetric metrics,
            boolean removeMetric) {
        mUserStoppedLogs.log(metrics.toString());
        if (removeMetric) {
            removeExistingMetricsLogged(mUserStoppingMetrics, metrics.userId);
        }
    }

    /**
     * Dumps its contents.
     */
    public void dump(@NonNull IndentingPrintWriter pw) {
        pw.println("* User Metrics *");
        synchronized (mLock) {

            dump(pw, "starting", mUserStartingMetrics);
            dump(pw, "stopping", mUserStoppingMetrics);

            pw.printf("Last %d started users\n", LOG_SIZE);
            mUserStartedLogs.dump("  ", pw);

            pw.printf("Last %d stopped users\n", LOG_SIZE);
            mUserStoppedLogs.dump("  ", pw);

            pw.println();
        }
    }

    private void dump(@NonNull IndentingPrintWriter pw, @NonNull String message,
            @NonNull SparseArray<? extends BaseUserMetric> metrics) {
        pw.increaseIndent();
        try {
            if (metrics == null) {
                pw.printf("no users %s\n", message);
                return;
            }
            int size = metrics.size();
            pw.printf("%d users %s\n", size, message);
            for (int i = 0; i < size; i++) {
                BaseUserMetric metric = metrics.valueAt(i);
                pw.printf("%d: ", i);
                metric.dump(pw);
                pw.println();
            }
        } finally {
            pw.decreaseIndent();
        }
    }

    private abstract class BaseUserMetric {
        public final @UserIdInt int userId;

        protected BaseUserMetric(@UserIdInt int userId) {
            this.userId = userId;
        }

        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            try (IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ")) {
                dump(ipw);
            }
            pw.flush();
            return sw.toString();
        }

        abstract void dump(@NonNull IndentingPrintWriter pw);
    }

    @VisibleForTesting
    final class UserStartingMetric extends BaseUserMetric {
        public final long startTime;
        public long switchTime;
        public long unlockingTime;
        public long unlockedTime;
        public @UserIdInt int switchFromUserId;

        UserStartingMetric(@UserIdInt int userId, long startTime) {
            super(userId);
            this.startTime = startTime;
        }

        @Override
        public void dump(@NonNull IndentingPrintWriter pw) {
            pw.printf("user=%d start=", userId);
            TimeUtils.dumpTime(pw, startTime);

            if (switchTime > 0) {
                long delta = switchTime - startTime;
                pw.print(" switch");
                if (switchFromUserId != 0) {
                    pw.printf("(from %d)", switchFromUserId);
                }
                pw.print('=');
                TimeUtils.formatDuration(delta, pw);
            }

            if (unlockingTime > 0) {
                long delta = unlockingTime - startTime;
                pw.print(" unlocking=");
                TimeUtils.formatDuration(delta, pw);
            }
            if (unlockedTime > 0) {
                long delta = unlockedTime - startTime;
                pw.print(" unlocked=");
                TimeUtils.formatDuration(delta, pw);
            }
        }
    }

    @VisibleForTesting
    final class UserStoppingMetric extends BaseUserMetric {
        public final long stopTime;
        public long shutdownTime;

        UserStoppingMetric(@UserIdInt int userId, long stopTime) {
            super(userId);
            this.stopTime = stopTime;
        }

        @Override
        public void dump(@NonNull IndentingPrintWriter pw) {
            pw.printf("user=%d stop=", userId);
            TimeUtils.dumpTime(pw, stopTime);

            if (shutdownTime > 0) {
                long delta = shutdownTime - stopTime;
                pw.print(" shutdown=");
                TimeUtils.formatDuration(delta, pw);
            }
        }
    }
}
