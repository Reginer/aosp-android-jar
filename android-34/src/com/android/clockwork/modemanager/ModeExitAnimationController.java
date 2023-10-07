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

package com.android.clockwork.modemanager;

import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.clockwork.common.WearResourceUtil;
import com.android.wearable.resources.R;

/** Handles exit logic for specific Wear modes */
public class ModeExitAnimationController {
    private static final String TAG = "ModeExitAnimationController";

    // The increment in milliseconds by which the power long press countdown should progress
    private static final long PROGRESS_DELAY = 10L;
    // Duration, in milliseconds, for which power key needs to be held to exit mode
    private static final long POWER_KEY_EXIT_TIMEOUT = 1900;
    /*
     * Duration, in milliseconds, by which the mode exit instruction will be displayed after
     * mode exit is canceled
     */
    private static final long EXIT_VIEW_REMOVE_TIMEOUT = 2000L;
    /*
     * Duration, in milliseconds, for a short power key press. If user releases power key within
     * this duration, cancel mode exit and show exit instruction for EXIT_VIEW_REMOVE_TIMEOUT
     */
    private static final long SHORT_PRESS_TIMEOUT = 100L;

    private static final WindowManager.LayoutParams WINDOW_MANAGER_PARAMS =
            new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

    private final Runnable mShowCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (mShowingCountdown) {
                return;
            }
            Log.d(TAG, "Showing count down view");
            mShowingCountdown = true;
            mCountDownTimer.start();
        }
    };
    private final Runnable mRemoveExitViewRunnable = () -> setExitViewVisible(false);
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final WindowManager mWindowManager;
    private final View mExitView;
    private final ProgressBar mProgressBar;

    /** The exit view is displayed if this is {@code true}. */
    private boolean mExitViewVisible;
    /** The progress bar displays an ongoing countdown if this is {@code true}. */
    private boolean mShowingCountdown;
    private CountDownTimer mCountDownTimer;

    ModeExitAnimationController(Context context, int exitTextResId) {
        mContext = context;
        mWindowManager = context.getSystemService(WindowManager.class);

        mExitView = WearResourceUtil.getInflatedView(context, R.layout.mode_exit_screen);

        ((TextView) mExitView.findViewById(R.id.exit_text)).setText(exitTextResId);

        mProgressBar = mExitView.findViewById(R.id.exit_progress_bar);
        mProgressBar.setMin(0);
        mProgressBar.setMax((int) POWER_KEY_EXIT_TIMEOUT);
        setupCountdownTimer();

        setExitImage();
    }

    private void setExitImage() {
        ImageView exitImage = mExitView.findViewById(R.id.exit_icon);
        Drawable leftTouchLockIconDrawable = mContext.getDrawable(
                R.drawable.ic_touchlock_icon_left);
        Drawable rightTouchLockIconDrawable = mContext.getDrawable(
                R.drawable.ic_touchlock_icon_right);
        boolean isLeftyMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.USER_ROTATION, ROTATION_0) == ROTATION_180;
        exitImage.setImageDrawable(
                isLeftyMode ? leftTouchLockIconDrawable : rightTouchLockIconDrawable);
    }

    /** Creates timer to be used to count down mode exit */
    private void setupCountdownTimer() {
        mCountDownTimer = new CountDownTimer(mProgressBar.getMax(), PROGRESS_DELAY) {
            @Override
            public void onTick(long millisUntilFinished) {
                mProgressBar.setProgress((int) (mProgressBar.getMax() - millisUntilFinished));
            }
            @Override
            public void onFinish() {
                mProgressBar.setProgress(mProgressBar.getMax());
                setExitViewVisible(false);
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.Wearable.WET_MODE_ON, 0);
            }
        };
    }

    private void setExitViewVisible(boolean visible) {
        if (visible && !mExitViewVisible) {
            mWindowManager.addView(mExitView, WINDOW_MANAGER_PARAMS);
            mExitViewVisible = true;
        }
        if (!visible && mExitViewVisible) {
            mWindowManager.removeView(mExitView);
            mExitViewVisible = false;
        }
    }

    /**
     * Shows exit view for a short duration. Called when any other button aside from the power
     * button is pressed.
     */
    void showExitInstruction() {
        if (mExitViewVisible) {
            return;
        }
        setExitViewVisible(true);
        mHandler.postDelayed(mRemoveExitViewRunnable, EXIT_VIEW_REMOVE_TIMEOUT);
    }

    /** Shows exit view then starts mode exit count down */
    void startExit() {
        if (mExitViewVisible) {
            mHandler.removeCallbacks(mRemoveExitViewRunnable);
        } else {
            setExitViewVisible(true);
        }
        mHandler.postDelayed(mShowCountdownRunnable, SHORT_PRESS_TIMEOUT);
    }

    /** Halts mode exit */
    void haltExit() {
        cancelCountdown();
        if (mExitViewVisible) {
            mHandler.postDelayed(mRemoveExitViewRunnable, EXIT_VIEW_REMOVE_TIMEOUT);
        }
    }

    private void cancelCountdown() {
        if (mShowingCountdown) {
            mCountDownTimer.cancel();
            mProgressBar.setProgress(mProgressBar.getMin());
            mShowingCountdown = false;
        } else {
            // Power button released on a short press, cancel countdown to start
            mHandler.removeCallbacks(mShowCountdownRunnable);
        }
    }
}
