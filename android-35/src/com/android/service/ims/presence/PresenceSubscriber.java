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

import android.content.Context;
import android.net.Uri;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsContactUceCapability.PresenceBuilder;
import android.text.TextUtils;

import com.android.ims.ResultCode;
import com.android.ims.internal.ContactNumberUtils;
import com.android.ims.internal.Logger;
import com.android.service.ims.RcsSettingUtils;
import com.android.service.ims.Task;
import com.android.service.ims.TaskManager;
import java.util.ArrayList;
import java.util.List;

public class PresenceSubscriber extends PresenceBase {
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private SubscribePublisher mSubscriber;
    private final Object mSubscriberLock = new Object();

    private String mAvailabilityRetryNumber = null;
    private int mAssociatedSubscription = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private final String[] mConfigVolteProvisionErrorOnSubscribeResponse;
    private final String[] mConfigRcsProvisionErrorOnSubscribeResponse;

    /*
     * Constructor
     */
    public PresenceSubscriber(SubscribePublisher subscriber, Context context,
            String[] configVolteProvisionErrorOnSubscribeResponse,
            String[] configRcsProvisionErrorOnSubscribeResponse){
        super(context);
        synchronized(mSubscriberLock) {
            this.mSubscriber = subscriber;
        }
        mConfigVolteProvisionErrorOnSubscribeResponse
                = configVolteProvisionErrorOnSubscribeResponse;
        mConfigRcsProvisionErrorOnSubscribeResponse = configRcsProvisionErrorOnSubscribeResponse;
    }

    public void updatePresenceSubscriber(SubscribePublisher subscriber) {
        synchronized(mSubscriberLock) {
            logger.print("Update PresencePublisher");
            this.mSubscriber = subscriber;
        }
    }

    public void removePresenceSubscriber() {
        synchronized(mSubscriberLock) {
                logger.print("Remove PresenceSubscriber");
            this.mSubscriber = null;
        }
    }

    public void handleAssociatedSubscriptionChanged(int newSubId) {
        if (mAssociatedSubscription == newSubId) {
            return;
        }
        mAssociatedSubscription = newSubId;
    }

    private String numberToUriString(String number) {
        String formattedContact = number;
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        if (tm != null && !formattedContact.startsWith("sip:")
                && !formattedContact.startsWith("tel:")){
            String domain = tm.getIsimDomain();
            logger.debug("domain=" + domain);
            if (domain == null || domain.length() == 0){
                formattedContact = "tel:" + formattedContact;
            } else {
                formattedContact = "sip:" + formattedContact + "@" + domain;
            }
        }

        logger.print("numberToUriString formattedContact=" + formattedContact);
        return formattedContact;
    }

    private String numberToTelString(String number){
        String formatedContact = number;
        if(!formatedContact.startsWith("sip:") && !formatedContact.startsWith("tel:")){
            formatedContact = "tel:" + formatedContact;
        }

        logger.print("numberToTelString formatedContact=" + formatedContact);
        return formatedContact;
    }

    public int requestCapability(List<String> contactsNumber,
            ContactCapabilityResponse listener) {

        SubscribePublisher subscriber = null;
        synchronized(mSubscriberLock) {
            subscriber = mSubscriber;
        }

        if (subscriber == null) {
            logger.error("requestCapability Subscribe not registered");
            return ResultCode.SUBSCRIBE_NOT_REGISTERED;
        }

        if (!RcsSettingUtils.hasUserEnabledContactDiscovery(mContext, mAssociatedSubscription)) {
            logger.warn("requestCapability request has been denied due to contact discovery being "
                    + "disabled by the user");
            return ResultCode.ERROR_SERVICE_NOT_ENABLED;
        }

        int ret = subscriber.getStackStatusForCapabilityRequest();
        if (ret < ResultCode.SUCCESS) {
            logger.error("requestCapability ret=" + ret);
            return ret;
        }

        if(contactsNumber == null || contactsNumber.size() ==0){
            ret = ResultCode.SUBSCRIBE_INVALID_PARAM;
            return ret;
        }

        logger.debug("check contact size ...");
        if (contactsNumber.size() > RcsSettingUtils.getMaxNumbersInRCL(mAssociatedSubscription)) {
            ret = ResultCode.SUBSCRIBE_TOO_LARGE;
            logger.error("requestCapability contctNumber size=" + contactsNumber.size());
            return ret;
        }

        String[] formatedNumbers = ContactNumberUtils.getDefault().format(contactsNumber);
        int formatResult = ContactNumberUtils.getDefault().validate(formatedNumbers);
        if (formatResult != ContactNumberUtils.NUMBER_VALID) {
            logger.error("requestCapability formatResult=" + formatResult);
            return ResultCode.SUBSCRIBE_INVALID_PARAM;
        }

        String[] formatedContacts = new String[formatedNumbers.length];
        for(int i=0; i<formatedContacts.length; i++){
            formatedContacts[i] = numberToTelString(formatedNumbers[i]);
        }
        // In ms
        long timeout = RcsSettingUtils.getCapabPollListSubExp(mAssociatedSubscription) * 1000;
        timeout += RcsSettingUtils.getSIPT1Timer(mAssociatedSubscription);

        // The terminal notification may be received shortly after the time limit of
        // the subscription due to network delays or retransmissions.
        // Device shall wait for 3sec after the end of the subscription period in order to
        // accept such notifications without returning spurious errors (e.g. SIP 481)
        timeout += 3000;

        logger.print("add to task manager, formatedNumbers=" +
                PresenceUtils.toContactString(formatedNumbers));
        int taskId = TaskManager.getDefault().addCapabilityTask(mContext, formatedNumbers,
                listener, timeout);
        logger.print("taskId=" + taskId);

        ret = subscriber.requestCapability(formatedContacts, taskId);
        if(ret < ResultCode.SUCCESS) {
            logger.error("requestCapability ret=" + ret + " remove taskId=" + taskId);
            TaskManager.getDefault().removeTask(taskId);
        }

        ret = taskId;

        return  ret;
    }

    public int requestAvailability(String contactNumber, ContactCapabilityResponse listener,
            boolean forceToNetwork) {

        String formatedContact = ContactNumberUtils.getDefault().format(contactNumber);
        int ret = ContactNumberUtils.getDefault().validate(formatedContact);
        if(ret != ContactNumberUtils.NUMBER_VALID){
            return ret;
        }

        if (!RcsSettingUtils.hasUserEnabledContactDiscovery(mContext, mAssociatedSubscription)) {
            logger.warn("requestCapability request has been denied due to contact discovery being "
                    + "disabled by the user");
            return ResultCode.ERROR_SERVICE_NOT_ENABLED;
        }

        if(!forceToNetwork){
            logger.debug("check if we can use the value in cache");
            int availabilityExpire =
                    RcsSettingUtils.getAvailabilityCacheExpiration(mAssociatedSubscription);
            availabilityExpire = availabilityExpire>0?availabilityExpire*1000:
                    60*1000; // by default is 60s
            logger.print("requestAvailability availabilityExpire=" + availabilityExpire);

            TaskManager.getDefault().clearTimeoutAvailabilityTask(availabilityExpire);

            Task task = TaskManager.getDefault().getAvailabilityTaskByContact(formatedContact);
            if(task != null && task instanceof PresenceAvailabilityTask) {
                PresenceAvailabilityTask availabilityTask = (PresenceAvailabilityTask)task;
                if(availabilityTask.getNotifyTimestamp() == 0) {
                    // The previous one didn't get response yet.
                    logger.print("requestAvailability: the request is pending in queue");
                    return ResultCode.SUBSCRIBE_ALREADY_IN_QUEUE;
                }else {
                    // not expire yet. Can use the previous value.
                    logger.print("requestAvailability: the prevous valuedoesn't be expired yet");
                    return ResultCode.SUBSCRIBE_TOO_FREQUENTLY;
                }
            }
        }

        // Only poll/fetch capability/availability on LTE
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        if(tm == null || (tm.getDataNetworkType() != TelephonyManager.NETWORK_TYPE_LTE)) {
            logger.error("requestAvailability return ERROR_SERVICE_NOT_AVAILABLE" +
                    " for it is not LTE network");
            return ResultCode.ERROR_SERVICE_NOT_AVAILABLE;
        }

        SubscribePublisher subscriber = null;
        synchronized(mSubscriberLock) {
            subscriber = mSubscriber;
        }

        if (subscriber == null) {
            logger.error("requestAvailability Subscribe not registered");
            return ResultCode.SUBSCRIBE_NOT_REGISTERED;
        }

        ret = subscriber.getStackStatusForCapabilityRequest();
        if (ret < ResultCode.SUCCESS) {
            logger.error("requestAvailability=" + ret);
            return ret;
        }

        // user number format in TaskManager.
        int taskId = TaskManager.getDefault().addAvailabilityTask(formatedContact, listener);

        // Change it to URI format.
        formatedContact = numberToUriString(formatedContact);

        logger.print("addAvailabilityTask formatedContact=" + formatedContact);

        ret = subscriber.requestAvailability(formatedContact, taskId);
        if (ret < ResultCode.SUCCESS) {
            logger.error("requestAvailability ret=" + ret + " remove taskId=" + taskId);
            TaskManager.getDefault().removeTask(taskId);
        }

        ret = taskId;

        return ret;
    }

    private int translateResponse403(String reasonPhrase){
        if(reasonPhrase == null){
            // No retry. The PS provisioning has not occurred correctly. UX Decision to show errror.
            return ResultCode.SUBSCRIBE_GENIRIC_FAILURE;
        }

        reasonPhrase = reasonPhrase.toLowerCase();
        if(reasonPhrase.contains("user not registered")){
            // Register to IMS then retry the single resource subscription if capability polling.
            // availability fetch: no retry. ignore the availability and allow LVC? (PLM decision)
            return ResultCode.SUBSCRIBE_NOT_REGISTERED;
        }

        if(reasonPhrase.contains("not authorized for presence")){
            // No retry.
            return ResultCode.SUBSCRIBE_NOT_AUTHORIZED_FOR_PRESENCE;
        }

        // unknown phrase: handle it as the same as no phrase
        return ResultCode.SUBSCRIBE_FORBIDDEN;
    }

    private int translateResponseCode(int responseCode, String reasonPhrase) {
        // pSipResponse should not be null.
        logger.debug("translateResponseCode getSipResponseCode=" +responseCode);
        int ret = ResultCode.SUBSCRIBE_GENIRIC_FAILURE;

        if(responseCode < 100 || responseCode > 699){
            logger.debug("internal error code sipCode=" + responseCode);
            ret = ResultCode.SUBSCRIBE_TEMPORARY_ERROR; //it is internal issue. ignore it.
            return ret;
        }

        switch(responseCode){
            case 200:
                ret = ResultCode.SUCCESS;
                break;

            case 403:
                ret = translateResponse403(reasonPhrase);
                break;

            case 404:
               // Target MDN is not provisioned for VoLTE or it is not  known as VzW IMS subscriber
               // Device shall not retry. Device shall remove the VoLTE status of the target MDN
               // and update UI
               ret = ResultCode.SUBSCRIBE_NOT_FOUND;
               break;

            case 408:
                // Request Timeout
                // Device shall retry with exponential back-off
                ret = ResultCode.SUBSCRIBE_TEMPORARY_ERROR;
                break;

            case 413:
                // Too Large.
                // Application need shrink the size of request contact list and resend the request
                ret = ResultCode.SUBSCRIBE_TOO_LARGE;
                break;

            case 423:
                // Interval Too Short. Requested expiry interval too short and server rejects it
                // Device shall re-attempt subscription after changing the expiration interval in
                // the Expires header field to be equal to or greater than the expiration interval
                // within the Min-Expires header field of the 423 response
                ret = ResultCode.SUBSCRIBE_TEMPORARY_ERROR;
                break;

            case 500:
                // 500 Server Internal Error
                // capability polling: exponential back-off retry (same rule as resource list)
                // availability fetch: no retry. ignore the availability and allow LVC
                // (PLM decision)
                ret = ResultCode.SUBSCRIBE_TEMPORARY_ERROR;
                break;

            case 503:
                // capability polling: exponential back-off retry (same rule as resource list)
                // availability fetch: no retry. ignore the availability and allow LVC?
                // (PLM decision)
                ret = ResultCode.SUBSCRIBE_TEMPORARY_ERROR;
                break;

                // capability polling: Device shall retry with exponential back-off
                // Availability Fetch: device shall ignore the error and shall not retry
            case 603:
                ret = ResultCode.SUBSCRIBE_TEMPORARY_ERROR;
                break;

            default:
                // Other 4xx/5xx/6xx
                // Device shall not retry
                ret = ResultCode.SUBSCRIBE_GENIRIC_FAILURE;
        }

        logger.debug("translateResponseCode ret=" + ret);
        return ret;
    }

    public void onSipResponse(int requestId, int responseCode, String reasonPhrase) {
        SubscribePublisher subscriber = null;
        synchronized(mSubscriberLock) {
            subscriber = mSubscriber;
        }

        if(isInConfigList(responseCode, reasonPhrase,
                mConfigVolteProvisionErrorOnSubscribeResponse)) {
            logger.print("volte provision sipCode=" + responseCode + " phrase=" + reasonPhrase);
            if (subscriber != null) {
                subscriber.updatePublisherState(PUBLISH_STATE_VOLTE_PROVISION_ERROR);
            }

            notifyDm();
        } else if(isInConfigList(responseCode, reasonPhrase,
                mConfigRcsProvisionErrorOnSubscribeResponse)) {
            logger.print("rcs proRcsPresence.vision sipCode=" + responseCode + " phrase="
                    + reasonPhrase);
            if (subscriber != null) {
                subscriber.updatePublisherState(PUBLISH_STATE_RCS_PROVISION_ERROR);
            }
        }

        int errorCode = translateResponseCode(responseCode, reasonPhrase);
        logger.print("handleSipResponse errorCode=" + errorCode);

        if(errorCode == ResultCode.SUBSCRIBE_NOT_REGISTERED){
            logger.debug("setPublishState to unknown for subscribe error 403 not registered");
            if (subscriber != null) {
                subscriber.updatePublisherState(PUBLISH_STATE_OTHER_ERROR);
            }
        }

        if(errorCode == ResultCode.SUBSCRIBE_NOT_AUTHORIZED_FOR_PRESENCE) {
            logger.debug("ResultCode.SUBSCRIBE_NOT_AUTHORIZED_FOR_PRESENCE");
        }

        if(errorCode == ResultCode.SUBSCRIBE_FORBIDDEN){
            logger.debug("ResultCode.SUBSCRIBE_FORBIDDEN");
        }

        // Suppose the request ID had been set when IQPresListener_CMDStatus
        Task task = TaskManager.getDefault().getTaskByRequestId(requestId);
        logger.debug("handleSipResponse task=" + task);
        if(task != null){
            task.mSipResponseCode = responseCode;
            task.mSipReasonPhrase = reasonPhrase;
            TaskManager.getDefault().putTask(task.mTaskId, task);
        }

        if(errorCode == ResultCode.SUBSCRIBE_NOT_REGISTERED &&
                task != null && task.mCmdId == TaskManager.TASK_TYPE_GET_AVAILABILITY) {
            String[] contacts = ((PresenceTask)task).mContacts;
            if(contacts != null && contacts.length>0){
                mAvailabilityRetryNumber = contacts[0];
            }
            logger.debug("retry to get availability for " + mAvailabilityRetryNumber);
        }

        // 404 error for single contact only as per requirement
        // need handle 404 for multiple contacts as per CV 3.24.
        if(errorCode == ResultCode.SUBSCRIBE_NOT_FOUND &&
                task != null && ((PresenceTask)task).mContacts != null) {
            String[] contacts = ((PresenceTask)task).mContacts;
            ArrayList<RcsContactUceCapability> contactCapabilities = new ArrayList<>();

            for(int i=0; i<contacts.length; i++){
                if(TextUtils.isEmpty(contacts[i])){
                    continue;
                }
                logger.debug("onSipResponse: contact= " + contacts[i] + ", not found.");
                // Build contacts with no capabilities.
                contactCapabilities.add(buildContactWithNoCapabilities(
                        PresenceUtils.convertContactNumber(contacts[i])));
            }
            handleCapabilityUpdate(task, contactCapabilities, true);

        } else if(errorCode == ResultCode.SUBSCRIBE_GENIRIC_FAILURE) {
            updateAvailabilityToUnknown(task);
        }

        handleCallback(task, errorCode, false);
    }

    private RcsContactUceCapability buildContactWithNoCapabilities(Uri contactUri) {
        PresenceBuilder presenceBuilder = new PresenceBuilder(contactUri,
                RcsContactUceCapability.SOURCE_TYPE_CACHED,
                RcsContactUceCapability.REQUEST_RESULT_FOUND);
        return presenceBuilder.build();
    }

    private void handleCapabilityUpdate(Task task, List<RcsContactUceCapability> capabilities,
            boolean updateLastTimestamp) {
        if (task == null || task.mListener == null ) {
            logger.warn("handleCapabilityUpdate, invalid listener!");
            return;
        }
        task.mListener.onCapabilitiesUpdated(task.mTaskId, capabilities, updateLastTimestamp);
    }

    public void retryToGetAvailability() {
        if(mAvailabilityRetryNumber == null){
            return;
        }
        requestAvailability(mAvailabilityRetryNumber, null, true);
        //retry one time only
        mAvailabilityRetryNumber = null;
    }

    public void updatePresence(RcsContactUceCapability capabilities) {
        if(mContext == null){
            logger.error("updatePresence mContext == null");
            return;
        }

        ArrayList<RcsContactUceCapability> presenceInfos = new ArrayList<>();
        presenceInfos.add(capabilities);

        String contactNumber = capabilities.getContactUri().getSchemeSpecificPart();
        // For single contact number we got 1 NOTIFY only. So regard it as terminated.
        TaskManager.getDefault().onTerminated(contactNumber);

        PresenceAvailabilityTask availabilityTask = TaskManager.getDefault().
                getAvailabilityTaskByContact(contactNumber);
        if (availabilityTask != null) {
            availabilityTask.updateNotifyTimestamp();
        }
        Task task = TaskManager.getDefault().getTaskForSingleContactQuery(contactNumber);
        handleCapabilityUpdate(task, presenceInfos, true);
    }

    public void updatePresences(int requestId, List<RcsContactUceCapability> contactsCapabilities,
            boolean isTerminated, String terminatedReason) {
        if(mContext == null){
            logger.error("updatePresences: mContext == null");
            return;
        }

        if (isTerminated) {
            TaskManager.getDefault().onTerminated(requestId, terminatedReason);
        }

        Task task = TaskManager.getDefault().getTaskByRequestId(requestId);
        if (contactsCapabilities.size() > 0 || task != null) {
            handleCapabilityUpdate(task, contactsCapabilities, true);
        }
    }

    public void onCommandStatusUpdated(int taskId, int requestId, int resultCode) {
        Task taskTmp = TaskManager.getDefault().getTask(taskId);
        logger.print("handleCmdStatus resultCode=" + resultCode);
        PresenceTask task = null;
        if(taskTmp != null && (taskTmp instanceof PresenceTask)){
            task = (PresenceTask)taskTmp;
            task.mSipRequestId = requestId;
            task.mCmdStatus = resultCode;
            TaskManager.getDefault().putTask(task.mTaskId, task);

            // handle error as the same as temporary network error
            // set availability to false, keep old capability
            if(resultCode != ResultCode.SUCCESS && task.mContacts != null){
                updateAvailabilityToUnknown(task);
            }
        }

        handleCallback(task, resultCode, true);
    }

    private void updateAvailabilityToUnknown(Task inTask){
        //only used for serviceState is offline or unknown.
        if(mContext == null){
            logger.error("updateAvailabilityToUnknown mContext=null");
            return;
        }

        if(inTask == null){
            logger.error("updateAvailabilityToUnknown task=null");
            return;
        }

        if(!(inTask instanceof PresenceTask)){
            logger.error("updateAvailabilityToUnknown not PresencTask");
            return;
        }

        PresenceTask task = (PresenceTask)inTask;

        if(task.mContacts == null || task.mContacts.length ==0){
            logger.error("updateAvailabilityToUnknown no contacts");
            return;
        }

        ArrayList<RcsContactUceCapability> presenceInfoList = new ArrayList<>();
        for (int i = 0; i< task.mContacts.length; i++) {
            if(TextUtils.isEmpty(task.mContacts[i])){
                continue;
            }
            // Add each contacts with no capabilities.
            Uri uri = PresenceUtils.convertContactNumber(task.mContacts[i]);
            PresenceBuilder presenceBuilder = new PresenceBuilder(uri,
                    RcsContactUceCapability.SOURCE_TYPE_CACHED,
                    RcsContactUceCapability.REQUEST_RESULT_FOUND);
            presenceInfoList.add(presenceBuilder.build());
        }

        if(presenceInfoList.size() > 0) {
             handleCapabilityUpdate(task, presenceInfoList, false);
        }
    }
}

