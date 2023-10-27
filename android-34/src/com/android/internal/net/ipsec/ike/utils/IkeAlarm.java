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

package com.android.internal.net.ipsec.ike.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.WakeupMessage;

/** IkeAlarm provides interfaces to use AlarmManager for scheduling system alarm. */
// TODO: b/191056695 Improve test coverage for scheduling system alarms.
public abstract class IkeAlarm {
    private static final Dependencies sDeps = new Dependencies();

    protected final AlarmManager mAlarmManager;
    protected final String mTag;
    protected final long mDelayMs;

    private IkeAlarm(IkeAlarmConfig alarmConfig) {
        mAlarmManager = alarmConfig.context.getSystemService(AlarmManager.class);
        mTag = alarmConfig.tag;
        mDelayMs = alarmConfig.delayMs;
    }

    /** Creates an alarm to be delivered precisely at the stated time. */
    public static IkeAlarm newExactAlarm(IkeAlarmConfig alarmConfig) {
        return new IkeAlarmWithListener(alarmConfig, sDeps);
    }

    /** Creates an alarm with a Dependencies instance for testing */
    @VisibleForTesting
    static IkeAlarm newExactAlarm(IkeAlarmConfig alarmConfig, Dependencies deps) {
        return new IkeAlarmWithListener(alarmConfig, deps);
    }

    /**
     * Creates an alarm to be delivered precisely at the stated time, even when the system is in
     * low-power idle (a.k.a. doze) modes.
     */
    public static IkeAlarm newExactAndAllowWhileIdleAlarm(IkeAlarmConfig alarmConfig) {
        return newExactAndAllowWhileIdleAlarm(alarmConfig, sDeps);
    }

    /** Creates an alarm with a Dependencies instance for testing */
    @VisibleForTesting
    static IkeAlarm newExactAndAllowWhileIdleAlarm(IkeAlarmConfig alarmConfig, Dependencies deps) {
        if (deps.getMyUid() == Process.SYSTEM_UID) {
            // By using listener instead of PendingIntent, the system service does not need to
            // declare the PendingIntent broadcast as protected in the AndroidManifest.
            return new IkeAlarmWithListener(alarmConfig, deps);
        } else {
            return new IkeAlarmWithPendingIntent(alarmConfig);
        }
    }

    /**
     * Build an alarm intent for an action, an intent ID and a message to send to the state machine.
     *
     * @param context The context for the target package.
     * @param intentAction The action to use in the alarm intent.
     * @param intentId The identifier to use in the intent, see {@link Intent#setIdentifier(String)}
     * @param ikeSmMsg The message that should be sent to the state machine when the alarm fires
     * @return A constructed PendingIntent for the passed arguments.
     */
    public static PendingIntent buildIkeAlarmIntent(
            Context context, String intentAction, String intentId, Message ikeSmMsg) {
        Intent intent = new Intent(intentAction);
        intent.setIdentifier(intentId);
        intent.setPackage(context.getPackageName());

        Bundle bundle = new Bundle();
        bundle.putParcelable(IkeAlarmReceiver.PARCELABLE_NAME_IKE_SESSION_MSG, ikeSmMsg);
        intent.putExtras(bundle);

        return PendingIntent.getBroadcast(
                context, 0 /* requestCode; unused */, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    /** Cancel the alarm */
    public abstract void cancel();

    /** Schedule/re-schedule the alarm */
    public abstract void schedule();

    /** External dependencies, for injection in tests */
    @VisibleForTesting
    static class Dependencies {
        /** Get the UID of the current process */
        public int getMyUid() {
            return Process.myUid();
        }

        /** Construct a WakeupMessage */
        public WakeupMessage newWakeMessage(IkeAlarmConfig alarmConfig) {
            Message alarmMessage = alarmConfig.message;
            return new WakeupMessage(
                    alarmConfig.context,
                    alarmMessage.getTarget(),
                    alarmConfig.tag,
                    alarmMessage.what,
                    alarmMessage.arg1,
                    alarmMessage.arg2,
                    alarmMessage.obj);
        }
    }

    /** Alarm that will be using a PendingIntent and will be set with setExactAndAllowWhileIdle */
    @VisibleForTesting
    static class IkeAlarmWithPendingIntent extends IkeAlarm {
        private final PendingIntent mPendingIntent;

        IkeAlarmWithPendingIntent(IkeAlarmConfig alarmConfig) {
            super(alarmConfig);
            android.util.Log.d("IKE", "new IkeAlarmWithPendingIntent for " + mTag);

            mPendingIntent = alarmConfig.pendingIntent;
        }

        @Override
        public void cancel() {
            mAlarmManager.cancel(mPendingIntent);
            mPendingIntent.cancel();
        }

        @Override
        public void schedule() {
            mAlarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + mDelayMs,
                    mPendingIntent);
        }
    }

    /**
     * Alarm that will be using a OnAlarmListener and will be set with setExact
     *
     * <p>If the caller is a system service, the alarm can still be fired in doze mode.
     */
    @VisibleForTesting
    static class IkeAlarmWithListener extends IkeAlarm {
        private final WakeupMessage mWakeupMsg;

        IkeAlarmWithListener(IkeAlarmConfig alarmConfig, Dependencies deps) {
            super(alarmConfig);
            android.util.Log.d("IKE", "new IkeAlarmWithListener for " + mTag);

            mWakeupMsg = deps.newWakeMessage(alarmConfig);
        }

        @Override
        public void cancel() {
            mWakeupMsg.cancel();
        }

        @Override
        public void schedule() {
            mWakeupMsg.schedule(SystemClock.elapsedRealtime() + mDelayMs);
        }
    }

    public static class IkeAlarmConfig {
        public final Context context;
        public final String tag;
        public final long delayMs;
        public final Message message;
        public final PendingIntent pendingIntent;

        public IkeAlarmConfig(
                Context context,
                String tag,
                long delayMs,
                PendingIntent pendingIntent,
                Message message) {
            this.context = context;
            this.tag = tag;
            this.delayMs = delayMs;
            this.message = message;
            this.pendingIntent = pendingIntent;
        }

        /** Create a copy with a different delay */
        public IkeAlarmConfig buildCopyWithDelayMs(long updatedDelayMs) {
            return new IkeAlarmConfig(context, tag, updatedDelayMs, pendingIntent, message);
        }
    }
}
