/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.internal.util.ArrayUtils;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.List;

/**
 *
 * Used to load or store ADNs (Abbreviated Dialing Numbers).
 *
 * {@hide}
 *
 */
public class AdnRecord implements Parcelable {
    static final String LOG_TAG = "AdnRecord";

    //***** Instance Variables

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    String mAlphaTag = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    String mNumber = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    String[] mEmails;
    String[] mAdditionalNumbers = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int mExtRecord = 0xff;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int mEfid;                   // or 0 if none
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int mRecordNumber;           // or 0 if none


    //***** Constants

    // In an ADN record, everything but the alpha identifier
    // is in a footer that's 14 bytes
    static final int FOOTER_SIZE_BYTES = 14;

    // Maximum size of the un-extended number field
    static final int MAX_NUMBER_SIZE_BYTES = 11;

    static final int EXT_RECORD_LENGTH_BYTES = 13;
    static final int EXT_RECORD_TYPE_ADDITIONAL_DATA = 2;
    static final int EXT_RECORD_TYPE_MASK = 3;
    static final int MAX_EXT_CALLED_PARTY_LENGTH = 0xa;

    // ADN offset
    static final int ADN_BCD_NUMBER_LENGTH = 0;
    static final int ADN_TON_AND_NPI = 1;
    static final int ADN_DIALING_NUMBER_START = 2;
    static final int ADN_DIALING_NUMBER_END = 11;
    static final int ADN_CAPABILITY_ID = 12;
    static final int ADN_EXTENSION_ID = 13;

    //***** Static Methods

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final Parcelable.Creator<AdnRecord> CREATOR
            = new Parcelable.Creator<AdnRecord>() {
        @Override
        public AdnRecord createFromParcel(Parcel source) {
            int efid;
            int recordNumber;
            String alphaTag;
            String number;
            String[] emails;
            String[] additionalNumbers;

            efid = source.readInt();
            recordNumber = source.readInt();
            alphaTag = source.readString();
            number = source.readString();
            emails = source.createStringArray();
            additionalNumbers = source.createStringArray();

            return new AdnRecord(efid, recordNumber, alphaTag, number, emails, additionalNumbers);
        }

        @Override
        public AdnRecord[] newArray(int size) {
            return new AdnRecord[size];
        }
    };

    /**
     * Returns the maximum number of characters that supported by the alpha tag for a record with
     * the specified maximum size.
     */
    public static int getMaxAlphaTagBytes(int maxRecordLength) {
        return Math.max(0, maxRecordLength - FOOTER_SIZE_BYTES);
    }

    /**
     * Encodes the alphaTag to a binary representation supported by the SIM.
     *
     * <p>This is the same representation as is used for this field in buildAdnString but there
     * is no restriction on the length.
     */
    @NonNull
    public static byte[] encodeAlphaTag(String alphaTag) {
        if (TextUtils.isEmpty(alphaTag)) {
            return new byte[0];
        }
        return IccUtils.stringToAdnStringField(alphaTag);
    }

    /**
     * Decodes an encoded alphaTag from a record or encoded tag.
     *
     * <p>This is the same as is used to construct an AdnRecord from byte[]
     */
    public static String decodeAlphaTag(byte[] encodedTagOrRecord, int offset, int length) {
        return IccUtils.adnStringFieldToString(encodedTagOrRecord, offset, length);
    }

    /**
     * Returns the maximum number of digits (or other dialable characters) that can be stored in
     * the phone number.
     *
     * <p>Additional length is supported via the ext1 entity file but the current implementation
     * doesn't support writing of that file so it is not included in this calculation.
     */
    public static int getMaxPhoneNumberDigits() {
        // Multiply by 2 because it is packed BCD encoded (2 digits per byte).
        return (ADN_DIALING_NUMBER_END - ADN_DIALING_NUMBER_START + 1) * 2;
    }

    //***** Constructor
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public AdnRecord (byte[] record) {
        this(0, 0, record);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public AdnRecord (int efid, int recordNumber, byte[] record) {
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        parseRecord(record);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public AdnRecord (String alphaTag, String number) {
        this(0, 0, alphaTag, number);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public AdnRecord (String alphaTag, String number, String[] emails) {
        this(0, 0, alphaTag, number, emails);
    }

    public AdnRecord(String alphaTag, String number, String[] emails, String[] additionalNumbers) {
        this(0, 0, alphaTag, number, emails, additionalNumbers);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public AdnRecord (int efid, int recordNumber, String alphaTag, String number, String[] emails) {
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.mAdditionalNumbers = null;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails,
            String[] additionalNumbers) {
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.mAdditionalNumbers = additionalNumbers;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public AdnRecord(int efid, int recordNumber, String alphaTag, String number) {
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = null;
        this.mAdditionalNumbers = null;
    }

    //***** Instance Methods

    public String getAlphaTag() {
        return mAlphaTag;
    }

    public int getEfid() {
        return mEfid;
    }

    public int getRecId() {
        return mRecordNumber;
    }

    public void setRecId(int recordId) {
        mRecordNumber = recordId;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        mNumber = number;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String[] getEmails() {
        return mEmails;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void setEmails(String[] emails) {
        this.mEmails = emails;
    }

    public String[] getAdditionalNumbers() {
        return mAdditionalNumbers;
    }

    public void setAdditionalNumbers(String[] additionalNumbers) {
        mAdditionalNumbers = additionalNumbers;
    }

    @Override
    public String toString() {
        return "ADN Record '" + mAlphaTag + "' '" + Rlog.pii(LOG_TAG, mNumber) + " "
                + Rlog.pii(LOG_TAG, mEmails) + " "
                + Rlog.pii(LOG_TAG, mAdditionalNumbers) + "'";
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isEmpty() {
        return TextUtils.isEmpty(mAlphaTag) && TextUtils.isEmpty(mNumber)
                && mEmails == null && mAdditionalNumbers == null;
    }

    public boolean hasExtendedRecord() {
        return mExtRecord != 0 && mExtRecord != 0xff;
    }

    /** Helper function for {@link #isEqual}. */
    private static boolean stringCompareNullEqualsEmpty(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            s1 = "";
        }
        if (s2 == null) {
            s2 = "";
        }
        return (s1.equals(s2));
    }

    /** Help function for ANR/EMAIL array compare. */
    private static boolean arrayCompareNullEqualsEmpty(String s1[], String s2[]) {
        if (s1 == s2) {
            return true;
        }

        s1 = ArrayUtils.emptyIfNull(s1, String.class);
        s2 = ArrayUtils.emptyIfNull(s2, String.class);

        List<String> src = Arrays.asList(s1);
        List<String> dest = Arrays.asList(s2);

        if (src.size() != dest.size()) {
            return false;
        }

        return src.containsAll(dest);
    }

    public boolean isEqual(AdnRecord adn) {
        return ( stringCompareNullEqualsEmpty(mAlphaTag, adn.mAlphaTag) &&
                stringCompareNullEqualsEmpty(mNumber, adn.mNumber) &&
                arrayCompareNullEqualsEmpty(mEmails, adn.mEmails) &&
                arrayCompareNullEqualsEmpty(mAdditionalNumbers, adn.mAdditionalNumbers));
    }
    //***** Parcelable Implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mEfid);
        dest.writeInt(mRecordNumber);
        dest.writeString(mAlphaTag);
        dest.writeString(mNumber);
        dest.writeStringArray(mEmails);
        dest.writeStringArray(mAdditionalNumbers);
    }

    /**
     * Build adn hex byte array based on record size
     * The format of byte array is defined in 51.011 10.5.1
     *
     * @param recordSize is the size X of EF record
     * @return hex byte[recordSize] to be written to EF record
     *          return null for wrong format of dialing number or tag
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public byte[] buildAdnString(int recordSize) {
        byte[] bcdNumber;
        byte[] byteTag;
        byte[] adnString;
        int footerOffset = recordSize - FOOTER_SIZE_BYTES;

        // create an empty record
        adnString = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            adnString[i] = (byte) 0xFF;
        }

        if (TextUtils.isEmpty(mNumber) && TextUtils.isEmpty(mAlphaTag)) {
            Rlog.w(LOG_TAG, "[buildAdnString] Empty dialing number");
            return adnString;   // return the empty record (for delete)
        } else if (mNumber != null && mNumber.length()
                > (ADN_DIALING_NUMBER_END - ADN_DIALING_NUMBER_START + 1) * 2) {
            Rlog.w(LOG_TAG,
                    "[buildAdnString] Max length of dialing number is 20");
            return null;
        }

        byteTag = encodeAlphaTag(mAlphaTag);

        if (byteTag.length > footerOffset) {
            Rlog.w(LOG_TAG, "[buildAdnString] Max length of tag is " + footerOffset);
            return null;
        } else {
            if (!TextUtils.isEmpty(mNumber)) {
                bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(
                        mNumber, PhoneNumberUtils.BCD_EXTENDED_TYPE_EF_ADN);

                System.arraycopy(bcdNumber, 0, adnString,
                        footerOffset + ADN_TON_AND_NPI, bcdNumber.length);

                adnString[footerOffset + ADN_BCD_NUMBER_LENGTH]
                        = (byte) (bcdNumber.length);
            }
            adnString[footerOffset + ADN_CAPABILITY_ID]
                    = (byte) 0xFF; // Capability Id
            adnString[footerOffset + ADN_EXTENSION_ID]
                    = (byte) 0xFF; // Extension Record Id

            if (byteTag.length > 0) {
                System.arraycopy(byteTag, 0, adnString, 0, byteTag.length);
            }

            return adnString;
        }
    }

    /**
     * See TS 51.011 10.5.10
     */
    public void
    appendExtRecord (byte[] extRecord) {
        try {
            if (extRecord.length != EXT_RECORD_LENGTH_BYTES) {
                return;
            }

            if ((extRecord[0] & EXT_RECORD_TYPE_MASK)
                    != EXT_RECORD_TYPE_ADDITIONAL_DATA) {
                return;
            }

            if ((0xff & extRecord[1]) > MAX_EXT_CALLED_PARTY_LENGTH) {
                // invalid or empty record
                return;
            }

            mNumber += PhoneNumberUtils.calledPartyBCDFragmentToString(
                    extRecord,
                    2,
                    0xff & extRecord[1],
                    PhoneNumberUtils.BCD_EXTENDED_TYPE_EF_ADN);

            // We don't support ext record chaining.

        } catch (RuntimeException ex) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord ext record", ex);
        }
    }

    //***** Private Methods

    /**
     * alphaTag and number are set to null on invalid format
     */
    private void
    parseRecord(byte[] record) {
        try {
            mAlphaTag = decodeAlphaTag(
                            record, 0, record.length - FOOTER_SIZE_BYTES);

            int footerOffset = record.length - FOOTER_SIZE_BYTES;

            int numberLength = 0xff & record[footerOffset];

            if (numberLength > MAX_NUMBER_SIZE_BYTES) {
                // Invalid number length
                mNumber = "";
                return;
            }

            // Please note 51.011 10.5.1:
            //
            // "If the Dialling Number/SSC String does not contain
            // a dialling number, e.g. a control string deactivating
            // a service, the TON/NPI byte shall be set to 'FF' by
            // the ME (see note 2)."

            mNumber = PhoneNumberUtils.calledPartyBCDToString(
                    record,
                    footerOffset + 1,
                    numberLength,
                    PhoneNumberUtils.BCD_EXTENDED_TYPE_EF_ADN);


            mExtRecord = 0xff & record[record.length - 1];

            mEmails = null;
            mAdditionalNumbers = null;
        } catch (RuntimeException ex) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord", ex);
            mNumber = "";
            mAlphaTag = "";
            mEmails = null;
            mAdditionalNumbers = null;
        }
    }
}
