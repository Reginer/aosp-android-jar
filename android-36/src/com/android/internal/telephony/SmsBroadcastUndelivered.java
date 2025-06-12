/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static java.util.Map.entry;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.analytics.TelephonyAnalytics;
import com.android.internal.telephony.analytics.TelephonyAnalytics.SmsMmsAnalytics;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.telephony.Rlog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Called when the credential-encrypted storage is unlocked, collecting all acknowledged messages
 * and deleting any partial message segments older than 7 days. Called from a worker thread to
 * avoid delaying phone app startup. The last step is to broadcast the first pending message from
 * the main thread, then the remaining pending messages will be broadcast after the previous
 * ordered broadcast completes.
 */
public class SmsBroadcastUndelivered {
    private static final String TAG = "SmsBroadcastUndelivered";
    private static final boolean DBG = InboundSmsHandler.DBG;

    /** Delete any partial message segments older than 7 days. */
    static final long DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE = (long) (60 * 60 * 1000) * 24 * 7;

    /**
     * Query projection for dispatching pending messages at boot time.
     * Column order must match the {@code *_COLUMN} constants in {@link InboundSmsHandler}.
     */
    private static final String[] PDU_PENDING_MESSAGE_PROJECTION = {
            "pdu",
            "sequence",
            "destination_port",
            "date",
            "reference_number",
            "count",
            "address",
            "_id",
            "message_body",
            "display_originating_addr",
            "sub_id"
    };

    /** Mapping from DB COLUMN to PDU_PENDING_MESSAGE_PROJECTION index */
    static final Map<Integer, Integer> PDU_PENDING_MESSAGE_PROJECTION_INDEX_MAPPING =
            Map.ofEntries(
                entry(InboundSmsHandler.PDU_COLUMN, 0),
                entry(InboundSmsHandler.SEQUENCE_COLUMN, 1),
                entry(InboundSmsHandler.DESTINATION_PORT_COLUMN, 2),
                entry(InboundSmsHandler.DATE_COLUMN, 3),
                entry(InboundSmsHandler.REFERENCE_NUMBER_COLUMN, 4),
                entry(InboundSmsHandler.COUNT_COLUMN, 5),
                entry(InboundSmsHandler.ADDRESS_COLUMN, 6),
                entry(InboundSmsHandler.ID_COLUMN, 7),
                entry(InboundSmsHandler.MESSAGE_BODY_COLUMN, 8),
                entry(InboundSmsHandler.DISPLAY_ADDRESS_COLUMN, 9),
                entry(InboundSmsHandler.SUBID_COLUMN, 10));


    private static SmsBroadcastUndelivered instance;

    /** Content resolver to use to access raw table from SmsProvider. */
    private final ContentResolver mResolver;

    private final UserManager mUserManager;

    private final FeatureFlags mFeatureFlags;

    /** Broadcast receiver that processes the raw table when the user unlocks the phone for the
     *  first time after reboot and the credential-encrypted storage is available.
     */
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Rlog.d(TAG, "Received broadcast " + intent.getAction());
            if (Intent.ACTION_USER_UNLOCKED.equals(intent.getAction())) {
                if (mFeatureFlags.smsMmsDeliverBroadcastsRedirectToMainUser()) {
                    int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL);
                    if (userId != getMainUser().getIdentifier()) {
                        return;
                    }
                }
                new ScanRawTableThread(context).start();
            }
        }
    };

    private class ScanRawTableThread extends Thread {
        private final Context context;

        private ScanRawTableThread(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            scanRawTable(context,
                    System.currentTimeMillis() - getUndeliveredSmsExpirationTime(context));
            InboundSmsHandler.cancelNewMessageNotification(context);
        }
    }

    public static void initialize(Context context, GsmInboundSmsHandler gsmInboundSmsHandler,
        CdmaInboundSmsHandler cdmaInboundSmsHandler, FeatureFlags featureFlags) {
        if (instance == null) {
            instance = new SmsBroadcastUndelivered(context, featureFlags);
        }

        // Tell handlers to start processing new messages and transit from the startup state to the
        // idle state. This method may be called multiple times for multi-sim devices. We must make
        // sure the state transition happen to all inbound sms handlers.
        if (gsmInboundSmsHandler != null) {
            gsmInboundSmsHandler.sendMessage(InboundSmsHandler.EVENT_START_ACCEPTING_SMS);
        }
        if (cdmaInboundSmsHandler != null) {
            cdmaInboundSmsHandler.sendMessage(InboundSmsHandler.EVENT_START_ACCEPTING_SMS);
        }
    }

    @UnsupportedAppUsage
    private SmsBroadcastUndelivered(Context context, FeatureFlags featureFlags) {
        mResolver = context.getContentResolver();

        mUserManager = context.getSystemService(UserManager.class);
        mFeatureFlags = featureFlags;

        UserHandle mainUser = getMainUser();
        boolean isUserUnlocked = mFeatureFlags.smsMmsDeliverBroadcastsRedirectToMainUser() ?
                        mUserManager.isUserUnlocked(mainUser) : mUserManager.isUserUnlocked();
        if (isUserUnlocked) {
            new ScanRawTableThread(context).start();
        } else {
            IntentFilter userFilter = new IntentFilter();
            userFilter.addAction(Intent.ACTION_USER_UNLOCKED);
            if (mFeatureFlags.smsMmsDeliverBroadcastsRedirectToMainUser()) {
                context.registerReceiverAsUser(
                        mBroadcastReceiver, mainUser, userFilter, null, null);
            } else {
                context.registerReceiver(mBroadcastReceiver, userFilter);
            }
        }
    }

    /** Returns the MainUser, which is the user designated for sending SMS broadcasts. */
    private UserHandle getMainUser() {
        UserHandle mainUser = null;
        if (mFeatureFlags.smsMmsDeliverBroadcastsRedirectToMainUser()) {
            mainUser = mUserManager.getMainUser();
        }
        return mainUser != null ? mainUser : UserHandle.SYSTEM;
    }

    /**
     * Scan the raw table for complete SMS messages to broadcast, and old PDUs to delete.
     */
    static void scanRawTable(Context context, long oldMessageTimestamp) {
        if (DBG) Rlog.d(TAG, "scanning raw table for undelivered messages");
        long startTime = System.nanoTime();
        ContentResolver contentResolver = context.getContentResolver();
        HashMap<SmsReferenceKey, Integer> multiPartReceivedCount =
                new HashMap<SmsReferenceKey, Integer>(4);
        HashSet<SmsReferenceKey> oldMultiPartMessages = new HashSet<SmsReferenceKey>(4);
        Cursor cursor = null;
        try {
            // query only non-deleted ones
            cursor = contentResolver.query(InboundSmsHandler.sRawUri,
                    PDU_PENDING_MESSAGE_PROJECTION, "deleted = 0", null, null);
            if (cursor == null) {
                Rlog.e(TAG, "error getting pending message cursor");
                return;
            }

            boolean isCurrentFormat3gpp2 = InboundSmsHandler.isCurrentFormat3gpp2();
            while (cursor.moveToNext()) {
                InboundSmsTracker tracker;
                try {
                    tracker = TelephonyComponentFactory.getInstance()
                            .inject(InboundSmsTracker.class.getName()).makeInboundSmsTracker(
                                    context,
                                    cursor,
                                    isCurrentFormat3gpp2);
                } catch (IllegalArgumentException e) {
                    Rlog.e(TAG, "error loading SmsTracker: " + e);
                    continue;
                }

                if (tracker.getMessageCount() == 1) {
                    // deliver single-part message
                    broadcastSms(tracker);
                } else {
                    SmsReferenceKey reference = new SmsReferenceKey(tracker);
                    Integer receivedCount = multiPartReceivedCount.get(reference);
                    if (receivedCount == null) {
                        multiPartReceivedCount.put(reference, 1);    // first segment seen
                        if (tracker.getTimestamp() < oldMessageTimestamp) {
                            // older than oldMessageTimestamp; delete if we don't find all the
                            // segments
                            oldMultiPartMessages.add(reference);
                        }
                    } else {
                        int newCount = receivedCount + 1;
                        if (newCount == tracker.getMessageCount()) {
                            // looks like we've got all the pieces; send a single tracker
                            // to state machine which will find the other pieces to broadcast
                            if (DBG) Rlog.d(TAG, "found complete multi-part message");
                            broadcastSms(tracker);
                            // don't delete this old message until after we broadcast it
                            oldMultiPartMessages.remove(reference);
                        } else {
                            multiPartReceivedCount.put(reference, newCount);
                        }
                    }
                }
            }
            // Retrieve the phone and phone id, required for metrics
            // TODO don't hardcode to the first phone (phoneId = 0) but this is no worse than
            //  earlier. Also phoneId for old messages may not be known (messages may be from an
            //  inactive sub)
            Phone phone = PhoneFactory.getPhone(0);
            int phoneId = 0;

            // Delete old incomplete message segments
            for (SmsReferenceKey message : oldMultiPartMessages) {
                // delete permanently
                int rows = contentResolver.delete(InboundSmsHandler.sRawUriPermanentDelete,
                        message.getDeleteWhere(), message.getDeleteWhereArgs());
                if (rows == 0) {
                    Rlog.e(TAG, "No rows were deleted from raw table!");
                } else if (DBG) {
                    Rlog.d(TAG, "Deleted " + rows + " rows from raw table for incomplete "
                            + message.mMessageCount + " part message");
                }
                // Update metrics with dropped SMS
                if (rows > 0) {
                    TelephonyMetrics metrics = TelephonyMetrics.getInstance();
                    metrics.writeDroppedIncomingMultipartSms(phoneId, message.mFormat, rows,
                            message.mMessageCount);
                    if (phone != null) {
                        phone.getSmsStats().onDroppedIncomingMultipartSms(message.mIs3gpp2, rows,
                                message.mMessageCount, TelephonyManager.from(context)
                                        .isEmergencyNumber(message.mAddress));
                        TelephonyAnalytics telephonyAnalytics = phone.getTelephonyAnalytics();
                        if (telephonyAnalytics != null) {
                            SmsMmsAnalytics smsMmsAnalytics =
                                    telephonyAnalytics.getSmsMmsAnalytics();
                            if (smsMmsAnalytics != null) {
                                smsMmsAnalytics.onDroppedIncomingMultipartSms();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            Rlog.e(TAG, "error reading pending SMS messages", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (DBG) Rlog.d(TAG, "finished scanning raw table in "
                    + ((System.nanoTime() - startTime) / 1000000) + " ms");
        }
    }

    /**
     * Send tracker to appropriate (3GPP or 3GPP2) inbound SMS handler for broadcast.
     */
    private static void broadcastSms(InboundSmsTracker tracker) {
        int subId = tracker.getSubId();
        int phoneId = SubscriptionManagerService.getInstance().getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            Rlog.e(TAG, "broadcastSms: ignoring message; no phone found for subId " + subId);
            return;
        }
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null) {
            Rlog.e(TAG, "broadcastSms: ignoring message; no phone found for subId " + subId
                    + " phoneId " + phoneId);
            return;
        }
        InboundSmsHandler handler = phone.getInboundSmsHandler(tracker.is3gpp2());
        if (handler != null) {
            handler.sendMessage(InboundSmsHandler.EVENT_BROADCAST_SMS, tracker);
        } else {
            Rlog.e(TAG, "null handler for " + tracker.getFormat() + " format, can't deliver.");
        }
    }

    private long getUndeliveredSmsExpirationTime(Context context) {
        int subId = SubscriptionManager.getDefaultSmsSubscriptionId();
        CarrierConfigManager configManager =
                (CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle bundle = null;
        if (configManager != null) bundle = configManager.getConfigForSubId(subId);

        if (bundle != null) {
            return bundle.getLong(CarrierConfigManager.KEY_UNDELIVERED_SMS_MESSAGE_EXPIRATION_TIME,
                    DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE);
        } else {
            return DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE;
        }
    }

    /**
     * Used as the HashMap key for matching concatenated message segments.
     */
    private static class SmsReferenceKey {
        final String mAddress;
        final int mReferenceNumber;
        final int mMessageCount;
        final String mQuery;
        final boolean mIs3gpp2;
        final String mFormat;

        SmsReferenceKey(InboundSmsTracker tracker) {
            mAddress = tracker.getAddress();
            mReferenceNumber = tracker.getReferenceNumber();
            mMessageCount = tracker.getMessageCount();
            mQuery = tracker.getQueryForSegments();
            mIs3gpp2 = tracker.is3gpp2();
            mFormat = tracker.getFormat();
        }

        String[] getDeleteWhereArgs() {
            return new String[]{mAddress, Integer.toString(mReferenceNumber),
                    Integer.toString(mMessageCount)};
        }

        String getDeleteWhere() {
            return mQuery;
        }

        @Override
        public int hashCode() {
            return ((mReferenceNumber * 31) + mMessageCount) * 31 + mAddress.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof SmsReferenceKey) {
                SmsReferenceKey other = (SmsReferenceKey) o;
                return other.mAddress.equals(mAddress)
                        && (other.mReferenceNumber == mReferenceNumber)
                        && (other.mMessageCount == mMessageCount);
            }
            return false;
        }
    }
}
