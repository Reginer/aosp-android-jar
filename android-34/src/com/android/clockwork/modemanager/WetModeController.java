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

import static android.provider.Settings.Global.Wearable.WET_MODE_ON;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Class that defines implementation details of WetMode.
 */
public class WetModeController {
    private static final String TAG = "WetModeController";

    // TODO (b/241021516) change the location of the touch permission
    private static final String PERMISSION_TOUCH =
            "com.google.android.clockwork.settings.WATCH_TOUCH";
    @VisibleForTesting
    static final String ACTION_WET_MODE_STARTED =
            "com.google.android.clockwork.actions.WET_MODE_STARTED";
    @VisibleForTesting
    static final String ACTION_WET_MODE_ENDED =
            "com.google.android.clockwork.actions.WET_MODE_ENDED";

    private final boolean mWetModeSupported;
    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final PowerManager mPowerManager;

    @VisibleForTesting
    WetModeController(Context context, PowerManager powerManager, boolean wetModeSupported) {
        mPowerManager = powerManager;
        mWetModeSupported = wetModeSupported;
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    WetModeController(Context context) {
        this(context, context.getSystemService(PowerManager.class), isWetModeSupported());
    }

    /**
     * Checks the value of the global flag WET_MODE_ON.
     * @return 1 if WetMode is enabled, 0 otherwise.
     */
    boolean isWetModeEnabled() {
        return Settings.Global.getInt(mContentResolver, WET_MODE_ON, 0) == 1;
    }

    /** Restores WetMode behavior after device completes booting process. */
    void onBootComplete() {
        if (!mWetModeSupported) {
            Log.d(TAG, "OnBootComplete: WetMode not supported on device");
            return;
        }

        // TODO (b/241022991) restore wet mode behavior after boot
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.Wearable.WET_MODE_ON, 0);
    }

    /** Entry point when WetMode state changes, that is either enabled or disabled. */
    void onWetModeStateChange(boolean enabled) {
        Log.d(TAG, "WetMode changed: " + (enabled ? "on" : "off"));

        if (!mWetModeSupported) {
            Log.d(TAG, "onWetModeStateChange: WetMode not supported on device");
            return;
        }
        if (enabled) {
            startWetMode();
        } else {
            endWetMode();
        }
    }

    private void startWetMode() {
        // TODO: set up and start WetMode
        // Send device into ambient mode
        mPowerManager.goToSleep(SystemClock.uptimeMillis());
        mContext.sendBroadcast(new Intent(ACTION_WET_MODE_STARTED), PERMISSION_TOUCH);
    }

    private void endWetMode() {
        // TODO: exit WetMode
        mContext.sendBroadcast(new Intent(ACTION_WET_MODE_ENDED), PERMISSION_TOUCH);
    }

    private static boolean isWetModeSupported() {
        boolean isEmulator = Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("cutf_cvm");
        if (isEmulator) {
            Log.w(TAG, "Emulator in use. WetMode not supported");
            return false;
        }
        return true;
    }

}

