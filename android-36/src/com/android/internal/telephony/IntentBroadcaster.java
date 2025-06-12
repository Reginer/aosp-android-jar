/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is used to broadcast intents that need to be rebroadcast after the device is unlocked.
 * NOTE: Currently this is used only for SIM_STATE_CHANGED so logic is hardcoded for that;
 * for example broadcasts are always sticky, only the last intent for the slotId is rebroadcast,
 * etc.
 */
public class IntentBroadcaster {
    private static final String TAG = "IntentBroadcaster";

    private Map<Integer, Intent> mRebroadcastIntents = new HashMap<>();
    private static IntentBroadcaster sIntentBroadcaster;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_UNLOCKED)) {
                synchronized (mRebroadcastIntents) {
                    // rebroadcast intents
                    Iterator iterator = mRebroadcastIntents.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry pair = (Map.Entry) iterator.next();
                        Intent i = (Intent) pair.getValue();
                        i.putExtra(Intent.EXTRA_REBROADCAST_ON_UNLOCK, true);
                        iterator.remove();
                        logd("Rebroadcasting intent " + i.getAction() + " "
                                + i.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE)
                                + " for slotId " + pair.getKey());
                        context.sendStickyBroadcastAsUser(i, UserHandle.ALL);
                    }
                }
            }
        }
    };

    private IntentBroadcaster(Context context) {
        context.registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
    }

    /**
     * Method to get an instance of IntentBroadcaster after creating one if needed.
     * @return IntentBroadcaster instance
     */
    public static IntentBroadcaster getInstance(Context context) {
        if (sIntentBroadcaster == null) {
            sIntentBroadcaster = new IntentBroadcaster(context);
        }
        return sIntentBroadcaster;
    }

    public static IntentBroadcaster getInstance() {
        return sIntentBroadcaster;
    }

    /**
     * Wrapper for ActivityManager.broadcastStickyIntent() that also stores intent to be rebroadcast
     * on USER_UNLOCKED
     */
    public void broadcastStickyIntent(Context context, Intent intent, int phoneId) {
        logd("Broadcasting and adding intent for rebroadcast: " + intent.getAction() + " "
                + intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE)
                + " for phoneId " + phoneId);
        synchronized (mRebroadcastIntents) {
            context.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            mRebroadcastIntents.put(phoneId, intent);
        }
    }

    private void logd(String s) {
        Log.d(TAG, s);
    }
}
