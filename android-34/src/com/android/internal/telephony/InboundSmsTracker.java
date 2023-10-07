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

import android.compat.annotation.UnsupportedAppUsage;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;
import com.android.telephony.Rlog;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

/**
 * Tracker for an incoming SMS message ready to broadcast to listeners.
 * This is similar to {@link com.android.internal.telephony.SMSDispatcher.SmsTracker} used for
 * outgoing messages.
 */
public class InboundSmsTracker {
    // Need 8 bytes to get a message id as a long.
    private static final int NUM_OF_BYTES_HASH_VALUE_FOR_MESSAGE_ID = 8;

    // Fields for single and multi-part messages
    private final byte[] mPdu;
    private final long mTimestamp;
    private final int mDestPort;
    private final boolean mIs3gpp2;
    private final boolean mIs3gpp2WapPdu;
    private final String mMessageBody;
    private final boolean mIsClass0;
    private final int mSubId;
    private final long mMessageId;
    private final @InboundSmsHandler.SmsSource int mSmsSource;

    // Fields for concatenating multi-part SMS messages
    private final String mAddress;
    private final int mReferenceNumber;
    private final int mSequenceNumber;
    private final int mMessageCount;

    // Fields for deleting this message after delivery
    private String mDeleteWhere;
    private String[] mDeleteWhereArgs;

    // BroadcastReceiver associated with this tracker
    private InboundSmsHandler.SmsBroadcastReceiver mSmsBroadcastReceiver;
    /**
     * Copied from SmsMessageBase#getDisplayOriginatingAddress used for blocking messages.
     * DisplayAddress could be email address if this message was from an email gateway, otherwise
     * same as mAddress. Email gateway might set a generic gateway address as the mAddress which
     * could not be used for blocking check and append the display email address at the beginning
     * of the message body. In that case, display email address is only available for the first SMS
     * in the Multi-part SMS.
     */
    private final String mDisplayAddress;

    @VisibleForTesting
    /** Destination port flag bit for no destination port. */
    public static final int DEST_PORT_FLAG_NO_PORT = (1 << 16);

    /** Destination port flag bit to indicate 3GPP format message. */
    private static final int DEST_PORT_FLAG_3GPP = (1 << 17);

    @VisibleForTesting
    /** Destination port flag bit to indicate 3GPP2 format message. */
    public static final int DEST_PORT_FLAG_3GPP2 = (1 << 18);

    @VisibleForTesting
    /** Destination port flag bit to indicate 3GPP2 format WAP message. */
    public static final int DEST_PORT_FLAG_3GPP2_WAP_PDU = (1 << 19);

    /** Destination port mask (16-bit unsigned value on GSM and CDMA). */
    private static final int DEST_PORT_MASK = 0xffff;

    @VisibleForTesting
    public static final String SELECT_BY_REFERENCE = "address=? AND reference_number=? AND "
            + "count=? AND (destination_port & " + DEST_PORT_FLAG_3GPP2_WAP_PDU
            + "=0) AND deleted=0";

    @VisibleForTesting
    public static final String SELECT_BY_REFERENCE_3GPP2WAP = "address=? AND reference_number=? "
            + "AND count=? AND (destination_port & "
            + DEST_PORT_FLAG_3GPP2_WAP_PDU + "=" + DEST_PORT_FLAG_3GPP2_WAP_PDU + ") AND deleted=0";

    /**
     * Create a tracker for a single-part SMS.
     *
     * @param context
     * @param pdu the message PDU
     * @param timestamp the message timestamp
     * @param destPort the destination port
     * @param is3gpp2 true for 3GPP2 format; false for 3GPP format
     * @param is3gpp2WapPdu true for 3GPP2 format WAP PDU; false otherwise
     * @param address originating address
     * @param displayAddress email address if this message was from an email gateway, otherwise same
     *                       as originating address
     * @param smsSource the source of the SMS message
     */
    public InboundSmsTracker(Context context, byte[] pdu, long timestamp, int destPort,
            boolean is3gpp2, boolean is3gpp2WapPdu, String address, String displayAddress,
            String messageBody, boolean isClass0, int subId,
            @InboundSmsHandler.SmsSource int smsSource) {
        mPdu = pdu;
        mTimestamp = timestamp;
        mDestPort = destPort;
        mIs3gpp2 = is3gpp2;
        mIs3gpp2WapPdu = is3gpp2WapPdu;
        mMessageBody = messageBody;
        mAddress = address;
        mDisplayAddress = displayAddress;
        mIsClass0 = isClass0;
        // fields for multi-part SMS
        mReferenceNumber = -1;
        mSequenceNumber = getIndexOffset();     // 0 or 1, depending on type
        mMessageCount = 1;
        mSubId = subId;
        mMessageId = createMessageId(context, timestamp, subId);
        mSmsSource = smsSource;
    }

    /**
     * Create a tracker for a multi-part SMS. Sequence numbers start at 1 for 3GPP and regular
     * concatenated 3GPP2 messages, but CDMA WAP push sequence numbers start at 0. The caller will
     * subtract 1 if necessary so that the sequence number is always 0-based. When loading and
     * saving to the raw table, the sequence number is adjusted if necessary for backwards
     * compatibility.
     *
     * @param pdu the message PDU
     * @param timestamp the message timestamp
     * @param destPort the destination port
     * @param is3gpp2 true for 3GPP2 format; false for 3GPP format
     * @param address originating address, or email if this message was from an email gateway
     * @param displayAddress email address if this message was from an email gateway, otherwise same
     *                       as originating address
     * @param referenceNumber the concatenated reference number
     * @param sequenceNumber the sequence number of this segment (0-based)
     * @param messageCount the total number of segments
     * @param is3gpp2WapPdu true for 3GPP2 format WAP PDU; false otherwise
     * @param smsSource the source of the SMS message
     */
    public InboundSmsTracker(Context context, byte[] pdu, long timestamp, int destPort,
             boolean is3gpp2, String address, String displayAddress, int referenceNumber,
             int sequenceNumber, int messageCount, boolean is3gpp2WapPdu, String messageBody,
             boolean isClass0, int subId, @InboundSmsHandler.SmsSource int smsSource) {
        mPdu = pdu;
        mTimestamp = timestamp;
        mDestPort = destPort;
        mIs3gpp2 = is3gpp2;
        mIs3gpp2WapPdu = is3gpp2WapPdu;
        mMessageBody = messageBody;
        mIsClass0 = isClass0;
        // fields used for check blocking message
        mDisplayAddress = displayAddress;
        // fields for multi-part SMS
        mAddress = address;
        mReferenceNumber = referenceNumber;
        mSequenceNumber = sequenceNumber;
        mMessageCount = messageCount;
        mSubId = subId;
        mMessageId = createMessageId(context, timestamp, subId);
        mSmsSource = smsSource;
    }

    /**
     * Create a new tracker from the row of the raw table pointed to by Cursor.
     * Since this constructor is used only for recovery during startup, the Dispatcher is null.
     * @param cursor a Cursor pointing to the row to construct this SmsTracker for
     */
    public InboundSmsTracker(Context context, Cursor cursor, boolean isCurrentFormat3gpp2) {
        mPdu = HexDump.hexStringToByteArray(cursor.getString(InboundSmsHandler.PDU_COLUMN));

        // TODO: add a column to raw db to store this
        mIsClass0 = false;

        if (cursor.isNull(InboundSmsHandler.DESTINATION_PORT_COLUMN)) {
            mDestPort = -1;
            mIs3gpp2 = isCurrentFormat3gpp2;
            mIs3gpp2WapPdu = false;
        } else {
            int destPort = cursor.getInt(InboundSmsHandler.DESTINATION_PORT_COLUMN);
            if ((destPort & DEST_PORT_FLAG_3GPP) != 0) {
                mIs3gpp2 = false;
            } else if ((destPort & DEST_PORT_FLAG_3GPP2) != 0) {
                mIs3gpp2 = true;
            } else {
                mIs3gpp2 = isCurrentFormat3gpp2;
            }
            mIs3gpp2WapPdu = ((destPort & DEST_PORT_FLAG_3GPP2_WAP_PDU) != 0);
            mDestPort = getRealDestPort(destPort);
        }

        mTimestamp = cursor.getLong(InboundSmsHandler.DATE_COLUMN);
        mAddress = cursor.getString(InboundSmsHandler.ADDRESS_COLUMN);
        mDisplayAddress = cursor.getString(InboundSmsHandler.DISPLAY_ADDRESS_COLUMN);
        mSubId = cursor.getInt(SmsBroadcastUndelivered.PDU_PENDING_MESSAGE_PROJECTION_INDEX_MAPPING
                .get(InboundSmsHandler.SUBID_COLUMN));

        if (cursor.getInt(InboundSmsHandler.COUNT_COLUMN) == 1) {
            // single-part message
            long rowId = cursor.getLong(InboundSmsHandler.ID_COLUMN);
            mReferenceNumber = -1;
            mSequenceNumber = getIndexOffset();     // 0 or 1, depending on type
            mMessageCount = 1;
            mDeleteWhere = InboundSmsHandler.SELECT_BY_ID;
            mDeleteWhereArgs = new String[]{Long.toString(rowId)};
        } else {
            // multi-part message
            mReferenceNumber = cursor.getInt(InboundSmsHandler.REFERENCE_NUMBER_COLUMN);
            mMessageCount = cursor.getInt(InboundSmsHandler.COUNT_COLUMN);

            // GSM sequence numbers start at 1; CDMA WDP datagram sequence numbers start at 0
            mSequenceNumber = cursor.getInt(InboundSmsHandler.SEQUENCE_COLUMN);
            int index = mSequenceNumber - getIndexOffset();

            if (index < 0 || index >= mMessageCount) {
                throw new IllegalArgumentException("invalid PDU sequence " + mSequenceNumber
                        + " of " + mMessageCount);
            }

            mDeleteWhere = getQueryForSegments();
            mDeleteWhereArgs = new String[]{mAddress,
                    Integer.toString(mReferenceNumber), Integer.toString(mMessageCount)};
        }
        mMessageBody = cursor.getString(InboundSmsHandler.MESSAGE_BODY_COLUMN);
        mMessageId = createMessageId(context, mTimestamp, mSubId);
        // TODO(b/167713264): Use the correct SMS source
        mSmsSource = InboundSmsHandler.SOURCE_NOT_INJECTED;
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put("pdu", HexDump.toHexString(mPdu));
        values.put("date", mTimestamp);
        // Always set the destination port, since it now contains message format flags.
        // Port is a 16-bit value, or -1, so clear the upper bits before setting flags.
        int destPort;
        if (mDestPort == -1) {
            destPort = DEST_PORT_FLAG_NO_PORT;
        } else {
            destPort = mDestPort & DEST_PORT_MASK;
        }
        if (mIs3gpp2) {
            destPort |= DEST_PORT_FLAG_3GPP2;
        } else {
            destPort |= DEST_PORT_FLAG_3GPP;
        }
        if (mIs3gpp2WapPdu) {
            destPort |= DEST_PORT_FLAG_3GPP2_WAP_PDU;
        }
        values.put("destination_port", destPort);
        if (mAddress != null) {
            values.put("address", mAddress);
            values.put("display_originating_addr", mDisplayAddress);
            values.put("reference_number", mReferenceNumber);
            values.put("sequence", mSequenceNumber);
        }
        values.put("count", mMessageCount);
        values.put("message_body", mMessageBody);
        values.put("sub_id", mSubId);
        return values;
    }

    /**
     * Get the port number, or -1 if there is no destination port.
     * @param destPort the destination port value, with flags
     * @return the real destination port, or -1 for no port
     */
    public static int getRealDestPort(int destPort) {
        if ((destPort & DEST_PORT_FLAG_NO_PORT) != 0) {
            return -1;
        } else {
           return destPort & DEST_PORT_MASK;
        }
    }

    /**
     * Update the values to delete all rows of the message from raw table.
     * @param deleteWhere the selection to use
     * @param deleteWhereArgs the selection args to use
     */
    public void setDeleteWhere(String deleteWhere, String[] deleteWhereArgs) {
        mDeleteWhere = deleteWhere;
        mDeleteWhereArgs = deleteWhereArgs;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("SmsTracker{timestamp=");
        builder.append(new Date(mTimestamp));
        builder.append(" destPort=").append(mDestPort);
        builder.append(" is3gpp2=").append(mIs3gpp2);
        if (InboundSmsHandler.VDBG) {
            builder.append(" address=").append(mAddress);
            builder.append(" timestamp=").append(mTimestamp);
            builder.append(" messageBody=").append(mMessageBody);
        }
        builder.append(" display_originating_addr=").append(mDisplayAddress);
        builder.append(" refNumber=").append(mReferenceNumber);
        builder.append(" seqNumber=").append(mSequenceNumber);
        builder.append(" msgCount=").append(mMessageCount);
        if (mDeleteWhere != null) {
            builder.append(" deleteWhere(").append(mDeleteWhere);
            builder.append(") deleteArgs=(").append(Arrays.toString(mDeleteWhereArgs));
            builder.append(')');
        }
        builder.append(" ");
        builder.append(SmsController.formatCrossStackMessageId(mMessageId));
        builder.append("}");
        return builder.toString();
    }

    public byte[] getPdu() {
        return mPdu;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public int getDestPort() {
        return mDestPort;
    }

    public boolean is3gpp2() {
        return mIs3gpp2;
    }

    public boolean isClass0() {
        return mIsClass0;
    }

    public int getSubId() {
        return mSubId;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String getFormat() {
        return mIs3gpp2 ? SmsConstants.FORMAT_3GPP2 : SmsConstants.FORMAT_3GPP;
    }

    public String getQueryForSegments() {
        return mIs3gpp2WapPdu ? SELECT_BY_REFERENCE_3GPP2WAP : SELECT_BY_REFERENCE;
    }

    /**
     * Get the query to find the exact same message/message segment in the db.
     * @return Pair with where as Pair.first and whereArgs as Pair.second
     */
    public Pair<String, String[]> getExactMatchDupDetectQuery() {
        // convert to strings for query
        String address = getAddress();
        String refNumber = Integer.toString(getReferenceNumber());
        String count = Integer.toString(getMessageCount());
        String seqNumber = Integer.toString(getSequenceNumber());
        String date = Long.toString(getTimestamp());
        String messageBody = getMessageBody();

        String where = "address=? AND reference_number=? AND count=? AND sequence=? AND "
                + "date=? AND message_body=?";
        where = addDestPortQuery(where);
        String[] whereArgs = new String[]{address, refNumber, count, seqNumber, date, messageBody};

        return new Pair<>(where, whereArgs);
    }

    /**
     * The key differences here compared to exact match are:
     * - this is applicable only for multi-part message segments
     * - this does not match date or message_body
     * - this matches deleted=0 (undeleted segments)
     * The only difference as compared to getQueryForSegments() is that this checks for sequence as
     * well.
     * @return Pair with where as Pair.first and whereArgs as Pair.second
     */
    public Pair<String, String[]> getInexactMatchDupDetectQuery() {
        if (getMessageCount() == 1) return null;

        // convert to strings for query
        String address = getAddress();
        String refNumber = Integer.toString(getReferenceNumber());
        String count = Integer.toString(getMessageCount());
        String seqNumber = Integer.toString(getSequenceNumber());

        String where = "address=? AND reference_number=? AND count=? AND sequence=? AND "
                + "deleted=0";
        where = addDestPortQuery(where);
        String[] whereArgs = new String[]{address, refNumber, count, seqNumber};

        return new Pair<>(where, whereArgs);
    }

    private String addDestPortQuery(String where) {
        String whereDestPort;
        if (mIs3gpp2WapPdu) {
            whereDestPort = "destination_port & " + DEST_PORT_FLAG_3GPP2_WAP_PDU + "="
                + DEST_PORT_FLAG_3GPP2_WAP_PDU;
        } else {
            whereDestPort = "destination_port & " + DEST_PORT_FLAG_3GPP2_WAP_PDU + "=0";
        }
        return where + " AND (" + whereDestPort + ")";
    }

    private static long createMessageId(Context context, long timestamp, int subId) {
        int slotId = SubscriptionManager.getSlotIndex(subId);
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = telephonyManager.getImei(slotId);
        if (TextUtils.isEmpty(deviceId)) {
            return 0L;
        }
        String messagePrint = deviceId + timestamp;
        return getShaValue(messagePrint);
    }

    private static long getShaValue(String messagePrint) {
        try {
            return ByteBuffer.wrap(getShaBytes(messagePrint,
                    NUM_OF_BYTES_HASH_VALUE_FOR_MESSAGE_ID)).getLong();
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Rlog.e("InboundSmsTracker", "Exception while getting SHA value for message",
                    e);
        }
        return 0L;
    }

    private static byte[] getShaBytes(String messagePrint, int maxNumOfBytes)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.reset();
        messageDigest.update(messagePrint.getBytes("UTF-8"));
        byte[] hashResult = messageDigest.digest();
        if (hashResult.length >= maxNumOfBytes) {
            byte[] truncatedHashResult = new byte[maxNumOfBytes];
            System.arraycopy(hashResult, 0, truncatedHashResult, 0, maxNumOfBytes);
            return truncatedHashResult;
        }
        return hashResult;
    }

    /**
     * Sequence numbers for concatenated messages start at 1. The exception is CDMA WAP PDU
     * messages, which use a 0-based index.
     * @return the offset to use to convert between mIndex and the sequence number
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int getIndexOffset() {
        return (mIs3gpp2 && mIs3gpp2WapPdu) ? 0 : 1;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getDisplayAddress() {
        return mDisplayAddress;
    }

    public String getMessageBody() {
        return mMessageBody;
    }

    public int getReferenceNumber() {
        return mReferenceNumber;
    }

    public int getSequenceNumber() {
        return mSequenceNumber;
    }

    public int getMessageCount() {
        return mMessageCount;
    }

    public String getDeleteWhere() {
        return mDeleteWhere;
    }

    public String[] getDeleteWhereArgs() {
        return mDeleteWhereArgs;
    }

    public long getMessageId() {
        return mMessageId;
    }

    public @InboundSmsHandler.SmsSource int getSource() {
        return mSmsSource;
    }

    /**
     * Get/create the SmsBroadcastReceiver corresponding to the current tracker.
     */
    public InboundSmsHandler.SmsBroadcastReceiver getSmsBroadcastReceiver(
            InboundSmsHandler handler) {
        // lazy initialization
        if (mSmsBroadcastReceiver == null) {
            mSmsBroadcastReceiver = handler.new SmsBroadcastReceiver(this);
        }
        return mSmsBroadcastReceiver;
    }
}
