/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.compat.annotation.UnsupportedAppUsage;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SubscriptionManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

/**
 * Monitors the device and ICC storage, and sends the appropriate events.
 *
 * This code was formerly part of {@link SMSDispatcher}, and has been moved
 * into a separate class to support instantiation of multiple SMSDispatchers on
 * dual-mode devices that require support for both 3GPP and 3GPP2 format messages.
 */
public class SmsStorageMonitor extends Handler {
    private static final String TAG = "SmsStorageMonitor1";

    /** Maximum number of times to retry memory status reporting */
    private static final int MAX_RETRIES = 1;

    /** Delay before next attempt on a failed memory status reporting, in milliseconds. */
    private static final int RETRY_DELAY = 5000; // 5 seconds

    /** SIM/RUIM storage is full */
    private static final int EVENT_ICC_FULL = 1;

    /** Report memory status */
    private static final int EVENT_REPORT_MEMORY_STATUS = 2;

    /** Memory status reporting is acknowledged by RIL */
    private static final int EVENT_REPORT_MEMORY_STATUS_DONE = 3;

    /** Retry memory status reporting */
    private static final int EVENT_RETRY_MEMORY_STATUS_REPORTING = 4;

    /** Radio is ON */
    private static final int EVENT_RADIO_ON = 5;

    /** Context from phone object passed to constructor. */
    private final Context mContext;

    /** Wake lock to ensure device stays awake while dispatching the SMS intent. */
    private PowerManager.WakeLock mWakeLock;

    private int mMaxRetryCount = MAX_RETRIES;
    private int mRetryDelay = RETRY_DELAY;
    private int mRetryCount = 0;
    private boolean mIsWaitingResponse = false;
    private boolean mNeedNewReporting = false;
    private boolean mIsMemoryStatusReportingFailed = false;

    /** it is use to put in to extra value for SIM_FULL_ACTION and SMS_REJECTED_ACTION */
    Phone mPhone;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    final CommandsInterface mCi;
    boolean mStorageAvailable = true;

    boolean mInitialStorageAvailableStatus = true;

    private boolean mMemoryStatusOverrideFlag = false;

    /**
     * Hold the wake lock for 5 seconds, which should be enough time for
     * any receiver(s) to grab its own wake lock.
     */
    private static final int WAKE_LOCK_TIMEOUT = 5000;

    /**
     * Creates an SmsStorageMonitor and registers for events.
     * @param phone the Phone to use
     */
    public SmsStorageMonitor(Phone phone) {
        mPhone = phone;
        mContext = phone.getContext();
        mCi = phone.mCi;

        createWakelock();

        mCi.setOnIccSmsFull(this, EVENT_ICC_FULL, null);
        mCi.registerForOn(this, EVENT_RADIO_ON, null);

        // Register for device storage intents.  Use these to notify the RIL
        // that storage for SMS is or is not available.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_FULL);
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_NOT_FULL);
        mContext.registerReceiver(mResultReceiver, filter);
    }

    /**
     * Overriding of the Memory Status by the TestApi and send the event to Handler to test
     * the RP-SMMA feature
     * @param isStorageAvailable boolean value specifies the MemoryStatus to be
     * sent to Handler
     */
    public void sendMemoryStatusOverride(boolean isStorageAvailable) {
        if (!mMemoryStatusOverrideFlag) {
            mInitialStorageAvailableStatus = mStorageAvailable;
            mMemoryStatusOverrideFlag = true;
        }
        mStorageAvailable = isStorageAvailable;
        if (isStorageAvailable) {
            sendMessage(obtainMessage(EVENT_REPORT_MEMORY_STATUS));
        }
    }

    /**
     * reset Memory Status change made by {@link #sendMemoryStatusOverride}
     */
    public void clearMemoryStatusOverride() {
        mStorageAvailable = mInitialStorageAvailableStatus;
        mMemoryStatusOverrideFlag = false;
    }

    @VisibleForTesting
    public void setMaxRetries(int maxCount) {
        mMaxRetryCount = maxCount;
    }

    @VisibleForTesting
    public void setRetryDelayInMillis(int delay) {
        mRetryDelay = delay;
    }

    public void dispose() {
        mCi.unSetOnIccSmsFull(this);
        mCi.unregisterForOn(this);
        mContext.unregisterReceiver(mResultReceiver);
    }

    /**
     * Handles events coming from the phone stack. Overridden from handler.
     * @param msg the message to handle
     */
    @Override
    public void handleMessage(Message msg) {
        boolean isAvailable = mStorageAvailable;

        switch (msg.what) {
            case EVENT_ICC_FULL:
                handleIccFull();
                break;

            case EVENT_REPORT_MEMORY_STATUS:
                if (mIsWaitingResponse) {
                    Rlog.v(TAG, "EVENT_REPORT_MEMORY_STATUS - deferred");
                    // Previous reporting is on-going now. New reporting will be issued in
                    // EVENT_REPORT_MEMORY_STATUS_DONE.
                    mNeedNewReporting = true;
                } else {
                    Rlog.v(TAG, "EVENT_REPORT_MEMORY_STATUS - report sms memory status"
                            + (isAvailable ? "(not full)" : "(full)"));
                    // Clear a delayed EVENT_RETRY_MEMORY_STATUS_REPORTING
                    // and mIsMemoryStatusReportingFailed.
                    removeMessages(EVENT_RETRY_MEMORY_STATUS_REPORTING);
                    mIsMemoryStatusReportingFailed = false;
                    mRetryCount = 0;
                    sendMemoryStatusReport(isAvailable);
                }
                break;

            case EVENT_REPORT_MEMORY_STATUS_DONE:
                AsyncResult ar = (AsyncResult) msg.obj;
                mIsWaitingResponse = false;
                Rlog.v(TAG, "EVENT_REPORT_MEMORY_STATUS_DONE - "
                        + (ar.exception == null ? "succeeded" : "failed"));
                if (mNeedNewReporting) {
                    Rlog.v(TAG, "EVENT_REPORT_MEMORY_STATUS_DONE - report again now"
                            + (isAvailable ? "(not full)" : "(full)"));
                    // New reportings have been requested, report last memory status here.
                    mNeedNewReporting = false;
                    mRetryCount = 0;
                    sendMemoryStatusReport(isAvailable);
                } else {
                    if (ar.exception != null) {
                        if (mRetryCount++ < mMaxRetryCount) {
                            Rlog.v(TAG, "EVENT_REPORT_MEMORY_STATUS_DONE - retry in "
                                    + mRetryDelay + "ms");
                            sendMessageDelayed(
                                    obtainMessage(EVENT_RETRY_MEMORY_STATUS_REPORTING),
                                    mRetryDelay);
                        } else {
                            Rlog.v(TAG, "EVENT_REPORT_MEMORY_STATUS_DONE - "
                                    + "no retry anymore(pended)");
                            mRetryCount = 0;
                            mIsMemoryStatusReportingFailed = true;
                        }
                    } else {
                        mRetryCount = 0;
                        mIsMemoryStatusReportingFailed = false;
                    }
                }
                break;

            case EVENT_RETRY_MEMORY_STATUS_REPORTING:
                Rlog.v(TAG, "EVENT_RETRY_MEMORY_STATUS_REPORTING - retry"
                        + (isAvailable ? "(not full)" : "(full)"));
                sendMemoryStatusReport(isAvailable);
                break;

            case EVENT_RADIO_ON:
                if (mIsMemoryStatusReportingFailed) {
                    Rlog.v(TAG, "EVENT_RADIO_ON - report failed sms memory status"
                            + (isAvailable ? "(not full)" : "(full)"));
                    sendMemoryStatusReport(isAvailable);
                }
                break;
        }
    }

    private void sendMemoryStatusReport(boolean isAvailable) {
        mIsWaitingResponse = true;
        Resources r = mContext.getResources();
        if (r.getBoolean(com.android.internal.R.bool.config_smma_notification_supported_over_ims)) {
            IccSmsInterfaceManager smsIfcMngr = mPhone.getIccSmsInterfaceManager();
            if (smsIfcMngr != null) {
                Rlog.d(TAG, "sendMemoryStatusReport: smsIfcMngr is available");
                if (smsIfcMngr.mDispatchersController.isIms() && isAvailable) {
                    smsIfcMngr.mDispatchersController.reportSmsMemoryStatus(
                            obtainMessage(EVENT_REPORT_MEMORY_STATUS_DONE));
                    return;
                }
            }
        }
        mCi.reportSmsMemoryStatus(isAvailable, obtainMessage(EVENT_REPORT_MEMORY_STATUS_DONE));
    }

    private void createWakelock() {
        PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmsStorageMonitor");
        mWakeLock.setReferenceCounted(true);
    }

    /**
     * Called when SIM_FULL message is received from the RIL. Notifies the default SMS application
     * that SIM storage for SMS messages is full.
     */
    private void handleIccFull() {
        UserHandle userHandle = TelephonyUtils.getSubscriptionUserHandle(mContext,
                mPhone.getSubId());
        ComponentName componentName = SmsApplication.getDefaultSimFullApplicationAsUser(mContext,
                false, userHandle);

        // broadcast SIM_FULL intent
        Intent intent = new Intent(Intents.SIM_FULL_ACTION);
        intent.setComponent(componentName);
        mWakeLock.acquire(WAKE_LOCK_TIMEOUT);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
        mContext.sendBroadcast(intent, android.Manifest.permission.RECEIVE_SMS);
    }

    /** Returns whether or not there is storage available for an incoming SMS. */
    public boolean isStorageAvailable() {
        return mStorageAvailable;
    }

    private final BroadcastReceiver mResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_DEVICE_STORAGE_FULL.equals(action)
                    || Intent.ACTION_DEVICE_STORAGE_NOT_FULL.equals(action)) {
                mStorageAvailable =
                        Intent.ACTION_DEVICE_STORAGE_FULL.equals(action) ? false : true;
                sendMessage(obtainMessage(EVENT_REPORT_MEMORY_STATUS));
            }
        }
    };
}
