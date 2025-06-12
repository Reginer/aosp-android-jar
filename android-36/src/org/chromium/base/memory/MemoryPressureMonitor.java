// Copyright 2018 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.base.memory;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;

import androidx.annotation.VisibleForTesting;

import org.chromium.base.ContextUtils;
import org.chromium.base.MemoryPressureLevel;
import org.chromium.base.MemoryPressureListener;
import org.chromium.base.ResettersForTesting;
import org.chromium.base.ThreadUtils;
import org.chromium.base.supplier.Supplier;
import org.chromium.base.task.PostTask;
import org.chromium.base.task.TaskTraits;

/**
 *
 *
 * <pre> This class monitors memory pressure and reports it to the native side.
 * Even though there can be other callbacks besides MemoryPressureListener (which reports
 * pressure to the native side, and is added implicitly), the class is designed to suite
 * needs of native MemoryPressureListeners.
 *
 * There are two groups of MemoryPressureListeners:
 *
 * 1. Stateless, i.e. ones that simply free memory (caches, etc.) in response to memory
 *    pressure. These listeners need to be called periodically (to have effect), but not
 *    too frequently (to avoid regressing performance too much).
 *
 * 2. Stateful, i.e. ones that change their behavior based on the last received memory
 *    pressure (in addition to freeing memory). These listeners need to know when the
 *    pressure subsides, i.e. they need to be notified about CRITICAL->MODERATE changes.
 *
 * Android notifies about memory pressure through onTrimMemory() / onLowMemory() callbacks
 * from ComponentCallbacks2, but these are unreliable (e.g. called too early, called just
 * once, not called when memory pressure subsides, etc., see https://crbug.com/813909 for
 * more examples).
 *
 * There is also ActivityManager.getMyMemoryState() API which returns current pressure for
 * the calling process. It has its caveats, for example it can't be called from isolated
 * processes (renderers). Plus we don't want to poll getMyMemoryState() unnecessarily, for
 * example there is no reason to poll it when Chrome is in the background.
 *
 * This class implements the following principles:
 *
 * 1. Throttle pressure signals sent to callbacks.
 *    Callbacks are called at most once during throttling interval. If same pressure is
 *    reported several times during the interval, all reports except the first one are
 *    ignored.
 *
 * 2. Always report changes in pressure.
 *    If pressure changes during the interval, the change is not ignored, but delayed
 *    until the end of the interval.
 *
 * 3. Poll on CRITICAL memory pressure.
 *    Once CRITICAL pressure is reported, getMyMemoryState API is used to periodically
 *    query pressure until it subsides (becomes non-CRITICAL).
 *
 * Zooming out, the class is used as follows:
 *
 * 1. Only the browser process / WebView process poll, and it only polls when it makes
 *    sense to do so (when Chrome is in the foreground / there are WebView instances
 *    around).
 *
 * 2. Services (GPU, renderers) don't poll, instead they get additional pressure signals
 *    from the main process.
 *
 * NOTE: This class should only be used on UiThread as defined by ThreadUtils (which is
 *       Android main thread for Chrome, but can be some other thread for WebView).</pre>
 */
public class MemoryPressureMonitor {
    private static final int DEFAULT_THROTTLING_INTERVAL_MS = 60 * 1000;

    private final int mThrottlingIntervalMs;

    // Pressure reported to callbacks in the current throttling interval.
    private @MemoryPressureLevel int mLastReportedPressure = MemoryPressureLevel.NONE;

    // Pressure received (but not reported) during the current throttling interval,
    // or null if no pressure was received.
    private @MemoryPressureLevel Integer mThrottledPressure;

    // Whether we need to throttle pressure signals.
    private boolean mIsInsideThrottlingInterval;

    private boolean mPollingEnabled;

    // That's for an experiment to run the broadcast receiver in the background
    private boolean mPostToBackgroundIsEnabled;

    private Supplier<Integer> mCurrentPressureSupplierForTesting;
    private MemoryPressureCallback mReportingCallbackForTesting;

    private final Runnable mThrottlingIntervalTask = this::onThrottlingIntervalFinished;

    // The only instance.
    public static final MemoryPressureMonitor INSTANCE =
            new MemoryPressureMonitor(DEFAULT_THROTTLING_INTERVAL_MS);

    @VisibleForTesting
    protected MemoryPressureMonitor(int throttlingIntervalMs) {
        mThrottlingIntervalMs = throttlingIntervalMs;
    }

    /** Starts listening to ComponentCallbacks2. */
    public void registerComponentCallbacks() {
        ThreadUtils.assertOnUiThread();

        ContextUtils.getApplicationContext()
                .registerComponentCallbacks(
                        new ComponentCallbacks2() {
                            @Override
                            public void onTrimMemory(int level) {
                                Integer pressure = memoryPressureFromTrimLevel(level);
                                if (pressure != null) {
                                    PostTask.runOrPostTask(
                                            TaskTraits.UI_DEFAULT, () -> notifyPressure(pressure));
                                }

                                // We start from Android U due to changes in
                                // how App Freezer works in that release.
                                //
                                // See |PreFreezeBackgroundMemoryTrimmer| for
                                // more details.
                                if (level == ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
                                        && android.os.Build.VERSION.SDK_INT
                                                >= android.os.Build.VERSION_CODES
                                                        .UPSIDE_DOWN_CAKE) {
                                    MemoryPressureListener.onPreFreeze();
                                }
                            }

                            @Override
                            public void onLowMemory() {
                                PostTask.runOrPostTask(
                                        TaskTraits.UI_DEFAULT,
                                        () -> notifyPressure(MemoryPressureLevel.CRITICAL));
                            }

                            @Override
                            public void onConfigurationChanged(Configuration configuration) {}
                        });
    }

    /**
     * Enables memory pressure polling. See class comment for specifics. This method also does a
     * single pressure check to get the current pressure.
     */
    public void enablePolling(boolean postToBackground) {
        ThreadUtils.assertOnUiThread();
        mPostToBackgroundIsEnabled = postToBackground;
        if (mPollingEnabled) return;

        mPollingEnabled = true;
        if (!mIsInsideThrottlingInterval) {
            queryCurrentPressure();
        }
    }

    /** Disables memory pressure polling. */
    public void disablePolling() {
        ThreadUtils.assertOnUiThread();
        if (!mPollingEnabled) return;

        mPollingEnabled = false;
    }

    /**
     * Notifies the class about change in memory pressure.
     * Note that |pressure| might get throttled or delayed, i.e. calling this method doesn't
     * necessarily call the callbacks. See the class comment.
     */
    public void notifyPressure(@MemoryPressureLevel int pressure) {
        ThreadUtils.assertOnUiThread();

        if (mIsInsideThrottlingInterval) {
            // We've already reported during this interval. Save |pressure| and act on
            // it later, when the interval finishes.
            mThrottledPressure = pressure;
            return;
        }

        reportPressure(pressure);
    }

    /**
     * Last pressure that was reported to MemoryPressureListener.
     * Returns MemoryPressureLevel.NONE if nothing was reported yet.
     */
    public @MemoryPressureLevel int getLastReportedPressure() {
        ThreadUtils.assertOnUiThread();
        return mLastReportedPressure;
    }

    private void reportPressure(@MemoryPressureLevel int pressure) {
        assert !mIsInsideThrottlingInterval : "Can't report pressure when throttling.";

        startThrottlingInterval();

        mLastReportedPressure = pressure;
        if (mReportingCallbackForTesting != null) {
            mReportingCallbackForTesting.onPressure(pressure);
        } else {
            MemoryPressureListener.notifyMemoryPressure(pressure);
        }
    }

    private void onThrottlingIntervalFinished() {
        mIsInsideThrottlingInterval = false;
        // If there was a pressure change during the interval, report it.
        if (mThrottledPressure != null && mLastReportedPressure != mThrottledPressure) {
            int throttledPressure = mThrottledPressure;
            mThrottledPressure = null;
            reportPressure(throttledPressure);
            return;
        }

        // The pressure didn't change during the interval. Report current pressure
        // (starting a new interval) if we need to.
        if (mPollingEnabled && mLastReportedPressure == MemoryPressureLevel.CRITICAL) {
            queryCurrentPressure();
        }
    }

    private void queryCurrentPressure() {
        if (mCurrentPressureSupplierForTesting != null) {
            Integer pressure = mCurrentPressureSupplierForTesting.get();
            if (pressure != null) reportPressure(pressure);
            return;
        }

        if (mPostToBackgroundIsEnabled) {
            PostTask.postTask(
                    TaskTraits.BEST_EFFORT_MAY_BLOCK,
                    () -> {
                        Integer pressure = MemoryPressureMonitor.getCurrentMemoryPressure();
                        if (pressure != null) {
                            PostTask.postTask(
                                    TaskTraits.UI_DEFAULT, () -> notifyPressure(pressure));
                        }
                    });
        } else {
            Integer pressure = MemoryPressureMonitor.getCurrentMemoryPressure();
            if (pressure != null) notifyPressure(pressure);
        }
    }

    private void startThrottlingInterval() {
        ThreadUtils.postOnUiThreadDelayed(mThrottlingIntervalTask, mThrottlingIntervalMs);
        mIsInsideThrottlingInterval = true;
    }

    public void setCurrentPressureSupplierForTesting(Supplier<Integer> supplier) {
        mCurrentPressureSupplierForTesting = supplier;
        ResettersForTesting.register(() -> mCurrentPressureSupplierForTesting = null);
    }

    public void setReportingCallbackForTesting(MemoryPressureCallback callback) {
        mReportingCallbackForTesting = callback;
        ResettersForTesting.register(() -> mReportingCallbackForTesting = null);
    }

    /**
     * Queries current memory pressure.
     * Returns null if the pressure couldn't be determined.
     */
    private static @MemoryPressureLevel Integer getCurrentMemoryPressure() {
        // We used to have a histogram here to measure the duration of each successful
        // ActivityManager.getMyMemoryState() call called
        // Android.MemoryPressureMonitor.GetMyMemoryState.Succeeded.Time. 50th percentile was 0.8ms.
        try {
            ActivityManager.RunningAppProcessInfo processInfo =
                    new ActivityManager.RunningAppProcessInfo();
            ActivityManager.getMyMemoryState(processInfo);
            return memoryPressureFromTrimLevel(processInfo.lastTrimLevel);
        } catch (Exception e) {
            // Defensively catch all exceptions, just in case.
            return null;
        }
    }

    /**
     * Maps ComponentCallbacks2.TRIM_* value to MemoryPressureLevel.
     * Returns null if |level| couldn't be mapped and should be ignored.
     */
    @VisibleForTesting
    public static @MemoryPressureLevel Integer memoryPressureFromTrimLevel(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
                || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            return MemoryPressureLevel.CRITICAL;
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            // Don't notify on TRIM_MEMORY_UI_HIDDEN, since this class only
            // dispatches actionable memory pressure signals to native.
            return MemoryPressureListener.isTrimMemoryBackgroundCritical()
                    ? MemoryPressureLevel.CRITICAL
                    : MemoryPressureLevel.MODERATE;
        }
        return null;
    }
}
