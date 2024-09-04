/*
 * Copyright (c) 2015, Motorola Mobility LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of Motorola Mobility nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MOTOROLA MOBILITY LLC BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.android.service.ims.presence;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.android.ims.internal.Logger;
import com.android.service.ims.TaskManager;

/**
 * PresenceCapabilityTask
 */
public class PresenceCapabilityTask extends PresenceTask{
    /*
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * PendingIntent Action that will be scheduled via AlarmManager as a task timeout.
     */
    public static final String ACTION_TASK_TIMEOUT_ALARM =
            "com.android.service.ims.presence.task.timeout";

    private Context mContext = null;

    // The result code will be used for retry.
    public int mResultCode;

    // The alarm manager.
    static AlarmManager sAlarmManager = null;
    PendingIntent mAlarmIntent = null;
    boolean mTimerStarted = false;

    // it will be set to true after got sip response.
    public boolean mWaitingForNotify;

    // The time when created the task.
    private long mCreatedTimeStamp;

    private long mTimeout;

    public PresenceCapabilityTask(Context context, int taskId, int cmdId,
            ContactCapabilityResponse listener, String[] contacts,
            long timeout){
        super(taskId, cmdId, listener, contacts);
        mContext = context;
        mWaitingForNotify = false;

        mCreatedTimeStamp = System.currentTimeMillis();
        mTimeout = timeout;

        if(mTimeout <=0){
            // The terminal notification may be received shortly after the time limit of
            // the subscription due to network delays or retransmissions.
            // Device shall wait for 3sec after the end of the subscription period in order to
            // accept such notifications without returning spurious errors (e.g. SIP 481).
            mTimeout = 36000;
        }

        if(listener != null){
            startTimer();
        } //else it will be removed after got sip response.
    }

    public String toString(){
        return super.toString() +
                " mCreatedTimeStamp=" + mCreatedTimeStamp +
                " mTimeout=" + mTimeout;
    }

    private void startTimer(){
        if(mContext == null){
            logger.error("startTimer mContext is null");
            return;
        }

        Intent intent = new Intent(ACTION_TASK_TIMEOUT_ALARM);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra("taskId", mTaskId);
        PendingIntent mAlarmIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        if(sAlarmManager == null){
            sAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        }

        long triggerAt = SystemClock.elapsedRealtime() + mTimeout;
        logger.debug("startTimer taskId=" + mTaskId + " mTimeout=" + mTimeout +
                " triggerAt=" + triggerAt + " mAlarmIntent=" + mAlarmIntent);
        sAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, mAlarmIntent);
        mTimerStarted = true;
    }

    public void cancelTimer(){
        if(mTimerStarted){
            logger.debug("cancelTimer, taskId=" + mTaskId);
            if(mAlarmIntent != null && sAlarmManager != null) {
                sAlarmManager.cancel(mAlarmIntent);
            }
            mTimerStarted = false;
        }
    }

    public void onTimeout(){
        logger.debug("onTimeout, taskId=" + mTaskId);
        if(mListener != null){
            mListener.onTimeout(mTaskId);
        }
        TaskManager.getDefault().removeTask(mTaskId);
    }

    public void setWaitingForNotify(boolean waitingForNotify){
        mWaitingForNotify = waitingForNotify;
    }

    public boolean isWaitingForNotify(){
        return mWaitingForNotify;
    }

    public void onTerminated(String reason){
        if(!mWaitingForNotify){
            logger.debug("onTerminated mWaitingForNotify is false. task=" + this);
            return;
        }

        cancelTimer();
        if(mListener != null){
            mListener.onFinish(mTaskId);
        }

        TaskManager.getDefault().removeTask(mTaskId);
    }
}
