/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import static com.android.clockwork.displayoffload.Utils.ACTION_AMBIENT_STARTED;
import static com.android.clockwork.displayoffload.Utils.ACTION_AMBIENT_STOPPED;
import static com.android.clockwork.displayoffload.Utils.ACTION_STOP_AMBIENT_DREAM;
import static com.android.clockwork.displayoffload.Utils.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

public class AmbientModeTracker extends BroadcastReceiver {

    private final Runnable mOnAmbientStateChanged;
    private final Handler mHandler;

    private boolean mIsInAmbient;
    private boolean mIsAmbientEarlyStop;

    public AmbientModeTracker(Runnable onAmbientStateChanged, Handler handler) {
        mOnAmbientStateChanged = onAmbientStateChanged;
        mHandler = handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DebugUtils.DEBUG_AMBIENT_TRACKER) {
            Log.d(TAG, "AmbientModeTracker: action = " + action);
        }
        switch (action) {
            case ACTION_AMBIENT_STARTED:
                // We wait until boot completed to load settings and send to Offload
                mIsAmbientEarlyStop = false;
                mIsInAmbient = true;
                break;
            case ACTION_STOP_AMBIENT_DREAM:
                if (mIsInAmbient) {
                    mIsAmbientEarlyStop = true;
                }
                break;
            case ACTION_AMBIENT_STOPPED:
                mIsInAmbient = false;
                mIsAmbientEarlyStop = false;
                break;
            default:
                // Do nothing, shouldn't be possible
                break;
        }
        mHandler.removeCallbacks(mOnAmbientStateChanged);
        mHandler.post(mOnAmbientStateChanged);
    }

    public boolean isAmbientEarlyStop() {
        return mIsAmbientEarlyStop;
    }

    public boolean isTheaterMode(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.THEATER_MODE_ON, 0) == 1;
    }

    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_STOP_AMBIENT_DREAM);
        intentFilter.addAction(ACTION_AMBIENT_STARTED);
        intentFilter.addAction(ACTION_AMBIENT_STOPPED);
        return intentFilter;
    }

    @VisibleForTesting
    boolean isInAmbient() {
        return mIsInAmbient;
    }
}
