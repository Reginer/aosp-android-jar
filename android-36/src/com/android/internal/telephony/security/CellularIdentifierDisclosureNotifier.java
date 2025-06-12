/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony.security;

import android.content.Context;
import android.telephony.CellularIdentifierDisclosure;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.metrics.CellularSecurityTransparencyStats;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.telephony.Rlog;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates logic to emit notifications to the user that their cellular identifiers were
 * disclosed in the clear. Callers add CellularIdentifierDisclosure instances by calling
 * addDisclosure.
 *
 * <p>This class is thread safe and is designed to do costly work on worker threads. The intention
 * is to allow callers to add disclosures from a Looper thread without worrying about blocking for
 * IPC.
 *
 * @hide
 */
public class CellularIdentifierDisclosureNotifier {

    private static final String TAG = "CellularIdentifierDisclosureNotifier";
    private static final long DEFAULT_WINDOW_CLOSE_DURATION_IN_MINUTES = 15;
    private static CellularIdentifierDisclosureNotifier sInstance = null;
    private final long mWindowCloseDuration;
    private final TimeUnit mWindowCloseUnit;
    private final CellularNetworkSecuritySafetySource mSafetySource;
    private final Object mEnabledLock = new Object();

    @GuardedBy("mEnabledLock")
    private boolean mEnabled = false;
    // This is a single threaded executor. This is important because we want to ensure certain
    // events are strictly serialized.
    private ScheduledExecutorService mSerializedWorkQueue;

    // This object should only be accessed from within the thread of mSerializedWorkQueue. Access
    // outside of that thread would require additional synchronization.
    private Map<Integer, DisclosureWindow> mWindows;
    private SubscriptionManagerService mSubscriptionManagerService;
    private CellularSecurityTransparencyStats mCellularSecurityTransparencyStats;

    public CellularIdentifierDisclosureNotifier(CellularNetworkSecuritySafetySource safetySource) {
        this(Executors.newSingleThreadScheduledExecutor(), DEFAULT_WINDOW_CLOSE_DURATION_IN_MINUTES,
                TimeUnit.MINUTES, safetySource, SubscriptionManagerService.getInstance(),
                new CellularSecurityTransparencyStats());
    }

    /**
     * Construct a CellularIdentifierDisclosureNotifier by injection. This should only be used for
     * testing.
     *
     * @param notificationQueue a ScheduledExecutorService that should only execute on a single
     *     thread.
     */
    @VisibleForTesting
    public CellularIdentifierDisclosureNotifier(
            ScheduledExecutorService notificationQueue,
            long windowCloseDuration,
            TimeUnit windowCloseUnit,
            CellularNetworkSecuritySafetySource safetySource,
            SubscriptionManagerService subscriptionManagerService,
            CellularSecurityTransparencyStats cellularSecurityTransparencyStats) {
        mSerializedWorkQueue = notificationQueue;
        mWindowCloseDuration = windowCloseDuration;
        mWindowCloseUnit = windowCloseUnit;
        mWindows = new HashMap<>();
        mSafetySource = safetySource;
        mSubscriptionManagerService = subscriptionManagerService;
        mCellularSecurityTransparencyStats = cellularSecurityTransparencyStats;
    }

    /**
     * Add a CellularIdentifierDisclosure to be tracked by this instance. If appropriate, this will
     * trigger a user notification.
     */
    public void addDisclosure(Context context, int subId, CellularIdentifierDisclosure disclosure) {
        Rlog.d(TAG, "Identifier disclosure reported: " + disclosure);

        logDisclosure(subId, disclosure);

        synchronized (mEnabledLock) {
            if (!mEnabled) {
                Rlog.d(TAG, "Skipping disclosure because notifier was disabled.");
                return;
            }

            // Don't notify if this disclosure happened in service of an emergency. That's a user
            // initiated action that we don't want to interfere with.
            if (disclosure.isEmergency()) {
                Rlog.i(TAG, "Ignoring identifier disclosure associated with an emergency.");
                return;
            }

            // Don't notify if the modem vendor indicates this is a benign disclosure.
            if (disclosure.isBenign()) {
                Rlog.i(TAG, "Ignoring identifier disclosure that is claimed to be benign.");
                return;
            }

            // Schedule incrementAndNotify from within the lock because we're sure at this point
            // that we're enabled. This allows incrementAndNotify to avoid re-checking mEnabled
            // because we know that any actions taken on disabled will be scheduled after this
            // incrementAndNotify call.
            try {
                mSerializedWorkQueue.execute(incrementAndNotify(context, subId));
            } catch (RejectedExecutionException e) {
                Rlog.e(TAG, "Failed to schedule incrementAndNotify: " + e.getMessage());
            }
        } // end mEnabledLock
    }

    private void logDisclosure(int subId, CellularIdentifierDisclosure disclosure) {
        try {
            mSerializedWorkQueue.execute(runLogDisclosure(subId, disclosure));
        } catch (RejectedExecutionException e) {
            Rlog.e(TAG, "Failed to schedule runLogDisclosure: " + e.getMessage());
        }
    }

    private Runnable runLogDisclosure(int subId,
            CellularIdentifierDisclosure disclosure) {
        return () -> {
            SubscriptionInfoInternal subInfo =
                    mSubscriptionManagerService.getSubscriptionInfoInternal(subId);
            String mcc = null;
            String mnc = null;
            if (subInfo != null) {
                mcc = subInfo.getMcc();
                mnc = subInfo.getMnc();
            }

            mCellularSecurityTransparencyStats.logIdentifierDisclosure(disclosure, mcc, mnc,
                    isEnabled());
        };
    }

    /**
     * Re-enable if previously disabled. This means that {@code addDisclsoure} will start tracking
     * disclosures again and potentially emitting notifications.
     */
    public void enable(Context context) {
        synchronized (mEnabledLock) {
            Rlog.d(TAG, "enabled");
            mEnabled = true;
            try {
                mSerializedWorkQueue.execute(onEnableNotifier(context));
            } catch (RejectedExecutionException e) {
                Rlog.e(TAG, "Failed to schedule onEnableNotifier: " + e.getMessage());
            }
        }
    }

    /**
     * Clear all internal state and prevent further notifications until optionally re-enabled.
     * This can be used to in response to a user disabling the feature to emit notifications.
     * If {@code addDisclosure} is called while in a disabled state, disclosures will be dropped.
     */
    public void disable(Context context) {
        Rlog.d(TAG, "disabled");
        synchronized (mEnabledLock) {
            mEnabled = false;
            try {
                mSerializedWorkQueue.execute(onDisableNotifier(context));
            } catch (RejectedExecutionException e) {
                Rlog.e(TAG, "Failed to schedule onDisableNotifier: " + e.getMessage());
            }
        }
    }

    public boolean isEnabled() {
        synchronized (mEnabledLock) {
            return mEnabled;
        }
    }

    /** Get a singleton CellularIdentifierDisclosureNotifier. */
    public static synchronized CellularIdentifierDisclosureNotifier getInstance(
            CellularNetworkSecuritySafetySource safetySource) {
        if (sInstance == null) {
            sInstance = new CellularIdentifierDisclosureNotifier(safetySource);
        }

        return sInstance;
    }

    private Runnable incrementAndNotify(Context context, int subId) {
        return () -> {
            DisclosureWindow window = mWindows.get(subId);
            if (window == null) {
                window = new DisclosureWindow(subId);
                mWindows.put(subId, window);
            }

            window.increment(context, this);

            int disclosureCount = window.getDisclosureCount();

            Rlog.d(
                    TAG,
                    "Emitting notification for subId: "
                            + subId
                            + ". New disclosure count "
                            + disclosureCount);

            mSafetySource.setIdentifierDisclosure(
                    context,
                    subId,
                    disclosureCount,
                    window.getFirstOpen(),
                    window.getCurrentEnd());
        };
    }

    private Runnable onDisableNotifier(Context context) {
        return () -> {
            Rlog.d(TAG, "On disable notifier");
            for (DisclosureWindow window : mWindows.values()) {
                window.close();
            }
            mSafetySource.setIdentifierDisclosureIssueEnabled(context, false);
        };
    }

    private Runnable onEnableNotifier(Context context) {
        return () -> {
            Rlog.i(TAG, "On enable notifier");
            mSafetySource.setIdentifierDisclosureIssueEnabled(context, true);
        };
    }

    /**
     * Get the disclosure count for a given subId. NOTE: This method is not thread safe. Without
     * external synchronization, one should only call it if there are no pending tasks on the
     * Executor passed into this class.
     */
    @VisibleForTesting
    public int getCurrentDisclosureCount(int subId) {
        DisclosureWindow window = mWindows.get(subId);
        if (window != null) {
            return window.getDisclosureCount();
        }

        return 0;
    }

    /**
     * Get the open time for a given subId. NOTE: This method is not thread safe. Without
     * external synchronization, one should only call it if there are no pending tasks on the
     * Executor passed into this class.
     */
    @VisibleForTesting
    public Instant getFirstOpen(int subId) {
        DisclosureWindow window = mWindows.get(subId);
        if (window != null) {
            return window.getFirstOpen();
        }

        return null;
    }

    /**
     * Get the current end time for a given subId. NOTE: This method is not thread safe. Without
     * external synchronization, one should only call it if there are no pending tasks on the
     * Executor passed into this class.
     */
    @VisibleForTesting
    public Instant getCurrentEnd(int subId) {
        DisclosureWindow window = mWindows.get(subId);
        if (window != null) {
            return window.getCurrentEnd();
        }

        return null;
    }

    /**
     * A helper class that maintains all state associated with the disclosure window for a single
     * subId. No methods are thread safe. Callers must implement all synchronization.
     */
    private class DisclosureWindow {
        private int mDisclosureCount;
        private Instant mWindowFirstOpen;
        private Instant mLastEvent;
        private ScheduledFuture<?> mWhenWindowCloses;

        private int mSubId;

        DisclosureWindow(int subId) {
            mDisclosureCount = 0;
            mWindowFirstOpen = null;
            mLastEvent = null;
            mSubId = subId;
            mWhenWindowCloses = null;
        }

        void increment(Context context, CellularIdentifierDisclosureNotifier notifier) {

            mDisclosureCount++;

            Instant now = Instant.now();
            if (mDisclosureCount == 1) {
                // Our window was opened for the first time
                mWindowFirstOpen = now;
            }

            mLastEvent = now;

            cancelWindowCloseFuture();

            try {
                mWhenWindowCloses =
                        notifier.mSerializedWorkQueue.schedule(
                                closeWindowRunnable(context),
                                notifier.mWindowCloseDuration,
                                notifier.mWindowCloseUnit);
            } catch (RejectedExecutionException e) {
                Rlog.e(
                        TAG,
                        "Failed to schedule closeWindow for subId "
                                + mSubId
                                + " :  "
                                + e.getMessage());
            }
        }

        int getDisclosureCount() {
            return mDisclosureCount;
        }

        Instant getFirstOpen() {
            return mWindowFirstOpen;
        }

        Instant getCurrentEnd() {
            return mLastEvent;
        }

        void close() {
            mDisclosureCount = 0;
            mWindowFirstOpen = null;
            mLastEvent = null;

            if (mWhenWindowCloses == null) {
                return;
            }
            mWhenWindowCloses = null;
        }

        private Runnable closeWindowRunnable(Context context) {
            return () -> {
                Rlog.i(
                        TAG,
                        "Disclosure window closing for subId "
                                + mSubId
                                + ". Disclosure count was "
                                + getDisclosureCount());
                close();
                mSafetySource.clearIdentifierDisclosure(context, mSubId);
            };
        }

        private boolean cancelWindowCloseFuture() {
            if (mWhenWindowCloses == null) {
                return false;
            }

            // Pass false to not interrupt a running Future. Nothing about our notifier is ready
            // for this type of preemption.
            return mWhenWindowCloses.cancel(false);
        }

    }
}

