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

package com.android.clockwork.display.burninprotection;

import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;

import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.annotations.VisibleForTesting;
import com.android.wearable.resources.R;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * This class preventing screen burn-in by image masking on the top of the windows. Triggered by
 * {@link DisplayManager.DisplayListener#onDisplayChanged(int)}.
 */
public class BurnInProtector implements DisplayManager.DisplayListener,
        AlarmManager.OnAlarmListener {
    static final boolean DEBUG = false;

    private static final String TAG = "BurnInProtector";

    private static BurnInProtector sInstance = null;
    private final DisplayManager mDisplayManager;
    private final ImageMaskViewController mImageMaskViewController;
    private final Supplier<Long> mSystemClockSupplier;
    private final Handler mHandler;
    private final AlarmManager mAlarmManager;
    private final BrightnessObserver mBrightnessObserver;

    private int mDisplayState = Display.STATE_UNKNOWN;
    private long mLastOffloadStartTimestamp = -1;

    @VisibleForTesting
    BurnInProtector(
            DisplayManager displayManager,
            ImageMaskViewController imageMaskViewController,
            Supplier<Long> systemClockSupplier,
            Handler handler,
            AlarmManager alarmManager,
            BrightnessObserver brightnessObserver) {
        mDisplayManager = displayManager;
        mImageMaskViewController = imageMaskViewController;
        mSystemClockSupplier = systemClockSupplier;
        mHandler = handler;
        mAlarmManager = alarmManager;
        mBrightnessObserver = brightnessObserver;
    }

    public static BurnInProtector create(final Context context) {
        sInstance =
                new BurnInProtector(
                        context.getSystemService(DisplayManager.class),
                        new ImageMaskViewController(context),
                        SystemClock::elapsedRealtime,
                        new Handler(Looper.getMainLooper()),
                        context.getSystemService(AlarmManager.class),
                        new BrightnessObserver(context));
        return sInstance;
    }

    /**
     * Get the current instance of BurnInProtector
     *
     * @return BurnInProtector singleton instance
     */
    public static BurnInProtector instance() {
        return sInstance;
    }

    /**
     * Initialize the BurnInProtector.
     *
     * @param context Context
     */
    public void init(Context context) {
        final Resources wearableResources = WearResourceUtil.getWearableResources(context);
        if (wearableResources == null) {
            return;
        }

        int burnInMaskThreshold = wearableResources.getInteger(
                R.integer.config_burnInProtectionMaskThreshold);

        // Not initialize when threshold config is 0
        if (burnInMaskThreshold == 0) {
            return;
        }

        mDisplayManager.registerDisplayListener(this, mHandler);
    }

    @Override
    public void onDisplayAdded(int displayId) {
        //Not used
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        //Not used
    }

    @Override
    public void onDisplayChanged(int displayId) {
        if (displayId != Display.DEFAULT_DISPLAY) {
            return;
        }

        int displayState = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY).getState();
        if (mDisplayState == displayState) {
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "onDisplayChanged: displayState=" + displayState);
        }
        mDisplayState = displayState;

        switch (mDisplayState) {
            case Display.STATE_DOZE:
                if (mLastOffloadStartTimestamp < 0) {
                    // Register alarm for when the device keeps doze mode. This alarm will be
                    // canceled if the device enters DOZE_SUSPEND.
                    mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            mSystemClockSupplier.get() + TimeUnit.MINUTES.toMillis(2),
                            "burn-in-protector_uadpte",
                            this, mHandler);
                } else {
                    // Start BrightnessObserver to listen if the device entered STATE_DOZE.
                    // BrightnessObserver will call tearDown() by itself after sampling once.
                    mBrightnessObserver.observe();
                }
                break;

            case Display.STATE_DOZE_SUSPEND:
                if (mLastOffloadStartTimestamp < 0) {
                    // Set mLastOffloadStartTimestamp only once for the duration when the image
                    // mask is enabled.
                    mLastOffloadStartTimestamp = mSystemClockSupplier.get();
                }
                // Cancel alarm that was registered in STATE_DOZE
                mAlarmManager.cancel(this);

                // Stop observing brightness if entering STATE_DOZE_SUSPEND.
                mBrightnessObserver.tearDown();
                break;
            default:
                // Always disable burn in protection mask in these states.
                disableBurnInProtection();
                break;
        }
    }

    /**
     * This is called 2 minutes after the device enters STATE_DOZE.
     */
    @Override
    public void onAlarm() {
        if (mDisplayState != Display.STATE_DOZE && mDisplayState != Display.STATE_DOZE_SUSPEND) {
            // Stop the alarm when we're no longer in DOZE/DOZE_SUSPEND
            return;
        }

        enableBurnInProtection();

        // Register an alarm to update image mask by every single minute.
        mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                mSystemClockSupplier.get() + TimeUnit.MINUTES.toMillis(1),
                "burn-in-protector_image-update",
                this, mHandler);
    }

    @VisibleForTesting
    void disableBurnInProtection() {
        mAlarmManager.cancel(this);
        mImageMaskViewController.disableImageMask();
        mLastOffloadStartTimestamp = -1;
        mBrightnessObserver.tearDown();
    }

    void enableBurnInProtection() {
        if (mImageMaskViewController.isEnabled()) {
            mImageMaskViewController.updateImageMask();
        } else {
            mImageMaskViewController.enableImageMask();
        }
    }

    /**
     * Trigger single shot update.
     *
     * @return true if burn-in protection enabled.
     */
    public boolean singleShotUpdate() {
        if (mLastOffloadStartTimestamp > -1
                && mSystemClockSupplier.get() - mLastOffloadStartTimestamp
                > TimeUnit.MINUTES.toMillis(1)
                && mBrightnessObserver.shouldEnableBurnInProtector()) {
            Log.d(TAG, "singleShotUpdate: burn-in protector is enabled by display offload.");
            enableBurnInProtection();
            return true;
        }
        return false;
    }
}
