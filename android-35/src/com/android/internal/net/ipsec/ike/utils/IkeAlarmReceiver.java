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

package com.android.internal.net.ipsec.ike.utils;

import static android.net.ipsec.ike.IkeManager.getIkeLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

import java.util.HashSet;

/**
 * IkeAlarmReceiver represents a class that receives all the alarms set by IKE library
 *
 * <p>Alarm Manager holds a CPU wake lock as long as the alarm receiver's onReceive() method is
 * executing. Once onReceive() returns, the Alarm Manager releases this wake lock. Thus actions that
 * contain asynchronous process to complete might need acquire a wake lock later.
 */
public class IkeAlarmReceiver extends BroadcastReceiver {
    // Broadcast intent actions when an IKE Session event alarm is fired
    public static final String ACTION_DELETE_CHILD = "IkeAlarmReceiver.ACTION_DELETE_CHILD";
    public static final String ACTION_REKEY_CHILD = "IkeAlarmReceiver.ACTION_REKEY_CHILD";
    public static final String ACTION_DELETE_IKE = "IkeAlarmReceiver.ACTION_DELETE_IKE";
    public static final String ACTION_REKEY_IKE = "IkeAlarmReceiver.ACTION_REKEY_IKE";
    public static final String ACTION_DPD = "IkeAlarmReceiver.ACTION_DPD";
    public static final String ACTION_KEEPALIVE = "IkeAlarmReceiver.ACTION_KEEPALIVE";

    private static final HashSet<String> sIkeSessionActionsSet = new HashSet<>();

    static {
        sIkeSessionActionsSet.add(ACTION_DELETE_CHILD);
        sIkeSessionActionsSet.add(ACTION_REKEY_CHILD);
        sIkeSessionActionsSet.add(ACTION_DELETE_IKE);
        sIkeSessionActionsSet.add(ACTION_REKEY_IKE);
        sIkeSessionActionsSet.add(ACTION_DPD);
    }

    /** Parcelable name of Message that is owned by IKE Session StateMachine */
    public static final String PARCELABLE_NAME_IKE_SESSION_MSG =
            "IkeAlarmReceiver.PARCELABLE_NAME_IKE_SESSION_MSG";

    private final SparseArray<Handler> mIkeSessionIdToHandlerMap = new SparseArray<>();

    /**
     * Called when an alarm fires.
     *
     * <p>This is method is guaranteed to run on IkeSessionStateMachine thread since
     * IkeAlarmReceiver is registered with IkeSessionStateMachine Handler
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        getIkeLog()
                .d(
                        "IkeAlarmReceiver",
                        "Alarm fired: action " + action + " id " + intent.getIdentifier());
        switch (action) {
            case ACTION_DELETE_CHILD: // fallthrough
            case ACTION_REKEY_CHILD: // fallthrough
            case ACTION_DELETE_IKE: // fallthrough
            case ACTION_REKEY_IKE: // fallthrough
            case ACTION_DPD: // fallthrough
            case ACTION_KEEPALIVE:
                // This Message has lost its target information after being sent as a Broadcast
                Message message =
                        (Message) intent.getExtras().getParcelable(PARCELABLE_NAME_IKE_SESSION_MSG);
                Handler ikeHandler = mIkeSessionIdToHandlerMap.get(message.arg1);

                if (ikeHandler != null) {
                    // Use #dispatchMessage so that this method won't return until the message is
                    // processed
                    ikeHandler.dispatchMessage(message);
                }
                return;
            default:
                getIkeLog().d("IkeAlarmReceiver", "Received unrecognized alarm intent");
        }
    }

    /** Register a newly created IkeSessionStateMachine handler */
    public void registerIkeSession(int ikeSessionId, Handler ikeSesisonHandler) {
        mIkeSessionIdToHandlerMap.put(ikeSessionId, ikeSesisonHandler);
    }

    /** Unregistered the deleted IkeSessionStateMachine */
    public void unregisterIkeSession(int ikeSessionId) {
        mIkeSessionIdToHandlerMap.remove(ikeSessionId);
    }
}
