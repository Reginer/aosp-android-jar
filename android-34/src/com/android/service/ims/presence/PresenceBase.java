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

import android.annotation.IntDef;
import android.content.Context;
import android.content.Intent;
import android.telephony.ims.ImsManager;

import com.android.ims.ResultCode;
import com.android.ims.internal.Logger;
import com.android.service.ims.Task;
import com.android.service.ims.TaskManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public class PresenceBase {
    static private Logger logger = Logger.getLogger("PresenceBase");

    protected Context mContext;

    /**
     * The phone is PUBLISH_STATE_200_OK when
     * the response of the last publish is "200 OK"
     */
    public static final int PUBLISH_STATE_200_OK = 0;

    /**
     * The phone didn't publish after power on.
     * the phone didn't get any publish response yet.
     */
    public static final int PUBLISH_STATE_NOT_PUBLISHED = 1;

    /**
     * The phone is PUBLISH_STATE_VOLTE_PROVISION_ERROR when the response is one of items
     * in config_volte_provision_error_on_publish_response for PUBLISH or
     * in config_volte_provision_error_on_subscribe_response for SUBSCRIBE.
     */
    public static final int PUBLISH_STATE_VOLTE_PROVISION_ERROR = 2;

    /**
     * The phone is PUBLISH_STATE_RCS_PROVISION_ERROR when the response is one of items
     * in config_rcs_provision_error_on_publish_response for PUBLISH or
     * in config_rcs_provision_error_on_subscribe_response for SUBSCRIBE.Publ
     */
    public static final int PUBLISH_STATE_RCS_PROVISION_ERROR = 3;

    /**
     * The phone is PUBLISH_STATE_REQUEST_TIMEOUT when
     * The response of the last publish is "408 Request Timeout".
     */
    public static final int PUBLISH_STATE_REQUEST_TIMEOUT = 4;

    /**
     * The phone is PUBLISH_STATE_OTHER_ERROR when
     * the response of the last publish is other temp error. Such as
     * 503 Service Unavailable
     * Device shall retry with exponential back-off
     *
     * 423 Interval Too Short. Requested expiry interval too short and server rejects it
     * Device shall re-attempt subscription after changing the expiration interval in
     * the Expires header field to be equal to or greater than the expiration interval
     * within the Min-Expires header field of the 423 response.
     */
    public static final int PUBLISH_STATE_OTHER_ERROR = 5;

    @IntDef(value = {
            PUBLISH_STATE_200_OK,
            PUBLISH_STATE_NOT_PUBLISHED,
            PUBLISH_STATE_VOLTE_PROVISION_ERROR,
            PUBLISH_STATE_RCS_PROVISION_ERROR,
            PUBLISH_STATE_REQUEST_TIMEOUT,
            PUBLISH_STATE_OTHER_ERROR
    }, prefix="PUBLISH_STATE_")
    @Retention(RetentionPolicy.SOURCE)
    public @interface PresencePublishState {}

    public PresenceBase(Context context) {
        mContext = context;
    }

    protected void handleCallback(Task task, int resultCode, boolean forCmdStatus) {
        if (task == null) {
            logger.debug("task == null");
            return;
        }

        if (task.mListener != null) {
            if(resultCode >= ResultCode.SUCCESS){
                if(!forCmdStatus){
                    task.mListener.onSuccess(task.mTaskId);
                }
            }else{
                task.mListener.onError(task.mTaskId, resultCode);
            }
        }

        // remove task when error
        // remove task when SIP response success.
        // For list capability polling we will waiting for the terminated notify or timeout.
        if (resultCode != ResultCode.SUCCESS) {
            if(task instanceof PresencePublishTask){
                PresencePublishTask publishTask = (PresencePublishTask) task;
                logger.debug("handleCallback for publishTask=" + publishTask);
                if(resultCode == PUBLISH_STATE_VOLTE_PROVISION_ERROR) {
                    // retry 3 times for "403 Not Authorized for Presence".
                    if (publishTask.getRetryCount() >= 3) {
                        //remove capability after try 3 times by PresencePolling
                        logger.debug("handleCallback remove task=" + task);
                        TaskManager.getDefault().removeTask(task.mTaskId);
                    } else {
                        // Continue retry
                        publishTask.setRetryCount(publishTask.getRetryCount() + 1);
                    }
                } else {
                    logger.debug("handleCallback remove task=" + task);
                    TaskManager.getDefault().removeTask(task.mTaskId);
                }
            } else {
                logger.debug("handleCallback remove task=" + task);
                TaskManager.getDefault().removeTask(task.mTaskId);
            }
        }else{
            if(forCmdStatus || !forCmdStatus && (task instanceof PresenceCapabilityTask)){
                logger.debug("handleCallback remove task later");

                //waiting for Notify from network
                if(!forCmdStatus){
                    ((PresenceCapabilityTask)task).setWaitingForNotify(true);
                }
            }else{
                if(!forCmdStatus && (task instanceof PresenceAvailabilityTask) &&
                        (resultCode == ResultCode.SUCCESS)){
                    // Availiablity, cache for 60s, remove it later.
                    logger.debug("handleCallback PresenceAvailabilityTask cache for 60s task="
                            + task);
                    return;
                }

                logger.debug("handleCallback remove task=" + task);
                TaskManager.getDefault().removeTask(task.mTaskId);
            }
        }
    }

    public void onCommandStatusUpdated(int taskId, int requestId, int resultCode) {
        Task task = TaskManager.getDefault().getTask(taskId);
        if (task != null){
            task.mSipRequestId = requestId;
            task.mCmdStatus = resultCode;
            TaskManager.getDefault().putTask(task.mTaskId, task);
        }

        handleCallback(task, resultCode, true);
    }

    protected void notifyDm() {
        logger.debug("notifyDm");
        Intent intent = new Intent(
                ImsManager.ACTION_FORBIDDEN_NO_SERVICE_AUTHORIZATION);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        mContext.sendBroadcast(intent);
    }

    protected boolean isInConfigList(int errorNo, String phrase, String[] errorArray) {
        String inErrorString = ("" + errorNo).trim();

        logger.debug("errorArray length=" + errorArray.length
                + " errorArray=" + Arrays.toString(errorArray));
        for (String errorStr : errorArray) {
            if (errorStr != null && errorStr.startsWith(inErrorString)) {
                String errorPhrase = errorStr.substring(inErrorString.length());
                if(errorPhrase == null || errorPhrase.isEmpty()) {
                    return true;
                }

                if(phrase == null || phrase.isEmpty()) {
                    return false;
                }

                return phrase.toLowerCase().contains(errorPhrase.toLowerCase());
            }
        }
        return false;
    }
}

