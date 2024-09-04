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

package com.android.service.ims;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneNumberUtils;

import com.android.ims.internal.Logger;
import com.android.service.ims.presence.ContactCapabilityResponse;
import com.android.service.ims.presence.PresenceAvailabilityTask;
import com.android.service.ims.presence.PresenceCapabilityTask;
import com.android.service.ims.presence.PresenceTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * TaskManager
 */
public class TaskManager{
    /*
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static TaskManager sTaskManager = null;

    private int mTaskId = 0;

    public final static int TASK_TYPE_GET_CAPABILITY   = 1;
    public final static int TASK_TYPE_GET_AVAILABILITY = 2;
    public final static int TASK_TYPE_PUBLISH          = 3;

    private Map<String, Task> mTaskMap;

    private final Object mSyncObj = new Object();

    private static final int TASK_MANAGER_ON_TERMINATED = 1;
    private static final int TASK_MANAGER_ON_TIMEOUT = 2;

    private static MessageHandler sMsgHandler;

    public TaskManager(){
        logger.debug("TaskManager created.");
        mTaskMap = new HashMap<String, Task>();

        HandlerThread messageHandlerThread = new HandlerThread("MessageHandler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);

        messageHandlerThread.start();
        Looper messageHandlerLooper = messageHandlerThread.getLooper();
        sMsgHandler = new MessageHandler(messageHandlerLooper);
    }

    public static synchronized TaskManager getDefault(){
        if(sTaskManager == null){
            sTaskManager = new TaskManager();
        }

        return sTaskManager;
    }

    public synchronized int generateTaskId(){
        return mTaskId++;
    }

    public void putTask(int taskId, Task task){
        synchronized (mSyncObj){
            putTaskInternal(taskId, task);
        }
    }

    private synchronized void putTaskInternal(int taskId, Task task){
        Task sameKeyTask = mTaskMap.put(String.valueOf(taskId), task);

        logger.debug("Added Task: " + task + "Original same key task:" + sameKeyTask);
    }

    public int addCapabilityTask(Context context, String[] contacts,
            ContactCapabilityResponse listener, long timeout){
        int taskId = TaskManager.getDefault().generateTaskId();
        synchronized (mSyncObj){
            Task task = new PresenceCapabilityTask(context, taskId, TASK_TYPE_GET_CAPABILITY,
                    listener, contacts, timeout);
            putTaskInternal(taskId, task);
        }

        return taskId;
    }

    public int addAvailabilityTask(String contact, ContactCapabilityResponse listener){
        int taskId = TaskManager.getDefault().generateTaskId();
        synchronized (mSyncObj){
            String[] contacts = new String[1];
            contacts[0] = contact;
            Task task = new PresenceAvailabilityTask(taskId, TASK_TYPE_GET_AVAILABILITY,
                    listener, contacts);
            putTaskInternal(taskId, task);
        }

        return taskId;
    }

    public int addPublishTask(String contact){
        int taskId = TaskManager.getDefault().generateTaskId();
        synchronized (mSyncObj){
            String[] contacts = new String[1];
            contacts[0] = contact;
            Task task = new PresenceTask(taskId, TASK_TYPE_PUBLISH, null /*listener*/, contacts);
            putTaskInternal(taskId, task);
        }

        return taskId;
    }

    // If need to call getTask in this class please add another one getTaskInternal
    public Task getTask(int taskId){
        synchronized (mSyncObj){
            return mTaskMap.get(String.valueOf(taskId));
        }
    }

    public void removeTask(int taskId){
        synchronized (mSyncObj){
            Task task = mTaskMap.remove(String.valueOf(taskId));
            if(task instanceof PresenceCapabilityTask){
                ((PresenceCapabilityTask)task).cancelTimer();
            }
            logger.debug("Removed Task: " + task);
        }
    }

    public Task getTaskForSingleContactQuery(String contact) {
        synchronized (mSyncObj){
            Set<String> keys= mTaskMap.keySet();
            if(keys == null){
                logger.debug("getTaskByContact keys=null");
                return null;
            }

            for(String key:keys){
                Task task = mTaskMap.get(key);
                if(task == null){
                    continue;
                }

                if (task instanceof PresenceTask) {
                    PresenceTask presenceTask = (PresenceTask) task;
                    if(presenceTask.mContacts.length == 1 &&
                            PhoneNumberUtils.compare(contact, presenceTask.mContacts[0])){
                        return task;
                    }
                }
            }
        }
        return null;
    }

    public Task getTaskByRequestId(int sipRequestId){
        synchronized (mSyncObj){
            Set<String> keys= mTaskMap.keySet();
            if(keys == null){
                logger.debug("getTaskByRequestId keys=null");
                return null;
            }

            for(String key:keys){
                if(mTaskMap.get(key).mSipRequestId == sipRequestId){
                    logger.debug("getTaskByRequestId, sipRequestId=" + sipRequestId +
                            " task=" + mTaskMap.get(key));
                    return mTaskMap.get(key);
                }
            }
        }

        logger.debug("getTaskByRequestId, sipRequestId=" + sipRequestId + " task=null");
        return null;
    }

    public void onTerminated(String contact){ // for single number capability polling
        if(contact == null){
            return;
        }

        synchronized (mSyncObj){
            Set<String> keys= mTaskMap.keySet();
            if(keys == null){
                logger.debug("onTerminated keys is null");
                return;
            }

            for(String key:keys){
                Task task = mTaskMap.get(key);
                if(task == null){
                    continue;
                }

                if(task instanceof PresenceCapabilityTask){
                    PresenceCapabilityTask capabilityTask = (PresenceCapabilityTask)task;
                    if(capabilityTask.mContacts != null && capabilityTask.mContacts[0] != null &&
                            PhoneNumberUtils.compare(contact, capabilityTask.mContacts[0])){
                        if(!capabilityTask.isWaitingForNotify()){
                            logger.debug("onTerminated the tesk is not waiting for NOTIFY yet");
                            continue;
                        }

                        MessageData messageData = new MessageData();
                        messageData.mTask = capabilityTask;
                        messageData.mReason = null;

                        Message notifyMessage = sMsgHandler.obtainMessage(
                                TASK_MANAGER_ON_TERMINATED,
                                messageData);
                        sMsgHandler.sendMessage(notifyMessage);
                    }
                }
            }
        }
    }

    public void onTerminated(int requestId, String reason){
        logger.debug("onTerminated requestId=" + requestId + " reason=" + reason);

        Task task = getTaskByRequestId(requestId);
        if(task == null){
            logger.debug("onTerminated Can't find request " + requestId);
            return;
        }

        synchronized (mSyncObj){
            if(task instanceof PresenceCapabilityTask){
                MessageData messageData = new MessageData();
                messageData.mTask = (PresenceCapabilityTask)task;
                messageData.mReason = reason;

                Message notifyMessage = sMsgHandler.obtainMessage(TASK_MANAGER_ON_TERMINATED,
                        messageData);
                sMsgHandler.sendMessage(notifyMessage);
            }
        }
    }

    public void onTimeout(int taskId){
        logger.debug("onTimeout taskId=" + taskId);

        Task task = getTask(taskId);
        if(task == null){
            logger.debug("onTimeout task = null");
            return;
        }
        synchronized (mSyncObj){
            if(task instanceof PresenceCapabilityTask){
                MessageData messageData = new MessageData();
                messageData.mTask = (PresenceCapabilityTask)task;
                messageData.mReason = null;

                Message timeoutMessage = sMsgHandler.obtainMessage(TASK_MANAGER_ON_TIMEOUT,
                        messageData);
                sMsgHandler.sendMessage(timeoutMessage);
            }else{
                logger.debug("not PresenceCapabilityTask, taskId=" + taskId);
            }
        }
    }

    public class MessageData{
        public PresenceCapabilityTask mTask;
        public String mReason;
    }

    public class MessageHandler extends Handler{
        MessageHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            logger.debug( "Thread=" + Thread.currentThread().getName() + " received "
                    + msg);



            if(msg == null){
                logger.error("msg=null");
                return;
            }

            switch (msg.what) {
                case  TASK_MANAGER_ON_TERMINATED:
                {
                    MessageData messageData = (MessageData) msg.obj;
                    if(messageData != null && messageData.mTask != null){
                        messageData.mTask.onTerminated(messageData.mReason);
                    }
                    break;
                }

                case TASK_MANAGER_ON_TIMEOUT:
                {
                    MessageData messageData = (MessageData) msg.obj;
                    if(messageData != null && messageData.mTask != null){
                        messageData.mTask.onTimeout();
                    }
                    break;
                }

                default:
                    logger.debug("handleMessage unknown msg=" + msg.what);
            }
        }
    }

    public void clearTimeoutAvailabilityTask(long availabilityExpire) {
       logger.debug("clearTimeoutAvailabilityTask");

        synchronized (mSyncObj) {
            long currentTime = System.currentTimeMillis();

            Iterator<Map.Entry<String, Task>> iterator = mTaskMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Task> entry = iterator.next();

                Task task = (Task) entry.getValue();
                logger.debug("Currently existing Availability task, key: " + entry.getKey()
                        + ", Task: " + task);

                if ((task != null) && (task instanceof PresenceAvailabilityTask)) {
                    PresenceAvailabilityTask presenceTask = (PresenceAvailabilityTask)task;

                    long notifyTimestamp = presenceTask.getNotifyTimestamp();
                    long createTimestamp = presenceTask.getCreateTimestamp();
                    logger.debug("createTimestamp=" + createTimestamp + " notifyTimestamp=" +
                            notifyTimestamp + " currentTime=" + currentTime);

                     // remove it if it didn't get notify in 60s.
                     // or get notify for 60s
                    if(((notifyTimestamp != 0) &&
                            (notifyTimestamp + availabilityExpire < currentTime)) ||
                            (notifyTimestamp == 0) &&
                            (createTimestamp + availabilityExpire < currentTime)) {
                        logger.debug("remove expired availability task:" + presenceTask);
                        iterator.remove();
                    }
                }
            }
        }
    }

    public PresenceAvailabilityTask getAvailabilityTaskByContact(String contact){
        synchronized (mSyncObj){
            Set<String> keys= mTaskMap.keySet();
            if(keys == null){
                logger.debug("getTaskByContact keys=null");
                return null;
            }

            for(String key:keys){
                Task task = mTaskMap.get(key);
                if(task == null){
                    continue;
                }

                if(task instanceof PresenceAvailabilityTask){
                    PresenceAvailabilityTask availabilityTask = (PresenceAvailabilityTask)task;
                    if(PhoneNumberUtils.compare(contact, availabilityTask.mContacts[0])){
                        return availabilityTask;
                    }
                }
            }
        }

        return null;
    }
}

