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

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.SystemService;

/**
 * A {@link SystemService} that listens to changes in the state of specific
 * modes and informs relevant mode controllers of the change.
 */
public class ModeManagerService extends SystemService {
    private static final String TAG = "ModeManagerService";

    private final Context mContext;

    private WetModeController mWetModeController;

    /** Receives call backs on changes to wet mode state */
    @VisibleForTesting
    final ContentObserver mWetModeStateContentObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    boolean isWetModeEnabled = mWetModeController.isWetModeEnabled();
                    Log.d(TAG, "WetMode state changed"
                            + (isWetModeEnabled ? "enabled" : "disabled"));
                    mWetModeController.onWetModeStateChange(isWetModeEnabled);
                }
            };

    public ModeManagerService(Context context) {
        super(context);
        mContext = context;
        mWetModeController = new WetModeController(mContext);
    }

    @VisibleForTesting
    public ModeManagerService(Context context, WetModeController wetModeController) {
        this(context);
        mWetModeController = wetModeController;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "System service started");
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(WET_MODE_ON),
                false, mWetModeStateContentObserver);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            Log.d(TAG, "Boot complete");
            if (mWetModeController != null) {
                mWetModeController.onBootComplete();
            }
        }
    }
}
