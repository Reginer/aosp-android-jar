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

package com.android.ims.internal;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

/**
 * @hide
 */
public class ContactNumberUtils {
    /**
     * Sample code:
     *
     * ContactNumberUtils mNumberUtils = ContactNumberUtils.getDefault();
     * mNumberUtils.setContext(this);
     *
     * number = mNumberUtils.format(number);
     * int result = mNumberUtils.validate(number);
     * if (ContactNumberUtils.NUMBER_VALID == result) {
     * }
     */
    public static ContactNumberUtils getDefault() {
        if(sInstance == null) {
            sInstance = new ContactNumberUtils();
        }

        return sInstance;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    /**
     * Format contact number to the common format
     *
     * @param phoneNumber read from contact db.
     * @return formatted contact number.
     */
    public String format(final String phoneNumber) {
        String number = phoneNumber;
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        if(number.startsWith("*67") || number.startsWith("*82")) {
            number = number.substring(3);
            if (TextUtils.isEmpty(number)) {
                return null;
            }
        }

        number = PhoneNumberUtils.stripSeparators(number);

        int len = number.length();
        if (len == NUMBER_LENGTH_NO_AREA_CODE) {
            number = addAreaCode(number);
        }

        number = PhoneNumberUtils.normalizeNumber(number);

        len = number.length();
        if (len == NUMBER_LENGTH_NORMAL) {
            if (!number.startsWith("+1")) {
                number = "+1" + number;
            }
        } else if (len == NUMBER_LENGTH_NORMAL + 1) {
            if (number.startsWith("1")) {
                number = "+" + number;
            }
        } else if(len >= NUMBER_LENGTH_NORMAL + 2) {
            if ((len >= NUMBER_LENGTH_NORMAL + 4) && (number.startsWith("011"))) {
                number = "+" + number.substring(3);
            }

            if (!number.startsWith("+")) {
                if (number.startsWith("1")) {
                    number = "+" + number;;
                } else {
                    number = "+1" + number;
                }
            }
        }

        if(number.length() > NUMBER_LENGTH_MAX) {
            return null;
        }

        return number;
    }

    /**
     * Contact nubmer error code.
     */
    public static int NUMBER_VALID = 0;
    public static int NUMBER_EMERGENCY = 1;
    public static int NUMBER_SHORT_CODE = 2;
    public static int NUMBER_PRELOADED_ENTRY = 3;
    public static int NUMBER_FREE_PHONE = 4;
    public static int NUMBER_INVALID = 5;

    /**
     * Check if it is a valid contact number for presence.
     *
     * Note: mContext must be set via setContext() before calling this method.
     *
     * @param phoneNumber read from contact db.
     * @return contact number error code.
     */
    public int validate(final String phoneNumber) {
        String number = phoneNumber;
        if (TextUtils.isEmpty(number)) {
            return NUMBER_INVALID;
        }

        if(number.startsWith("*67") || number.startsWith("*82")) {
            number = number.substring(3);
            if (TextUtils.isEmpty(number)) {
                return NUMBER_INVALID;
            }
        }

        if(number.contains("*")) {
            return NUMBER_PRELOADED_ENTRY;
        }

        number = PhoneNumberUtils.stripSeparators(number);
        if (!number.equals(PhoneNumberUtils.convertKeypadLettersToDigits(number))) {
            return NUMBER_INVALID;
        }

        boolean isEmergencyNumber;
        if (mContext == null) {
            Log.e(TAG, "context is unexpectedly null to provide emergency identification service");
            isEmergencyNumber = false;
        } else {
            TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
            isEmergencyNumber = tm.isEmergencyNumber(number);
        }

        if (isEmergencyNumber) {
            return NUMBER_EMERGENCY;
        // TODO: To handle short code
        //} else if ((mContext != null) && PhoneNumberUtils.isN11Number(mContext, number)) {
        //    return NUMBER_SHORT_CODE;
        } else if (number.startsWith("#")) {
            return NUMBER_PRELOADED_ENTRY;
        } else if (isInExcludedList(number)) {
            return NUMBER_FREE_PHONE;
        }

        int len = number.length();
        if (len < NUMBER_LENGTH_NORMAL) {
            return NUMBER_INVALID;
        }

        number = format(number);
        if (number.startsWith("+")) {
            len = number.length();
            // make sure the number after stripped the national number still be 10 digits
            if (len >= NUMBER_LENGTH_NORMAL + 2) {
                return NUMBER_VALID;
            }
        }

        return NUMBER_INVALID;
    }


    /**
     * Some utility functions for presence service only.
     * @hide
     */
    public String[] format(List<String> numbers) {
        if ((numbers == null) || (numbers.size() == 0)) {
            return null;
        }

        int size = numbers.size();
        String[] outContactsArray = new String[size];
        for (int i = 0; i < size; i++) {
            String number = numbers.get(i);
            outContactsArray[i] = format(number);
            if (DEBUG) {
                Log.d(TAG, "outContactsArray[" + i + "] = " + outContactsArray[i]);
            }
        }

        return outContactsArray;
    }

    public String[] format(String[] numbers) {
        if ((numbers == null) || (numbers.length == 0)) {
            return null;
        }

        int length = numbers.length;
        String[] outContactsArray = new String[length];
        for (int i = 0; i < length; i++) {
            String number = numbers[i];
            outContactsArray[i] = format(number);
            if (DEBUG) {
                Log.d(TAG, "outContactsArray[" + i + "] = " + outContactsArray[i]);
            }
        }

        return outContactsArray;
    }
    public int validate(List<String> numbers) {
        if ((numbers == null) || (numbers.size() == 0)) {
            return NUMBER_INVALID;
        }

        int size = numbers.size();
        for (int i = 0; i < size; i++) {
            String number = numbers.get(i);
            int result = validate(number);
            if (result != NUMBER_VALID) {
                return result;
            }
        }

        return NUMBER_VALID;
    }

    public int validate(String[] numbers) {
        if ((numbers == null) || (numbers.length == 0)) {
            return NUMBER_INVALID;
        }

        int length = numbers.length;
        for (int i = 0; i < length; i++) {
            String number = numbers[i];
            int result = validate(number);
            if (result != NUMBER_VALID) {
                return result;
            }
        }

        return NUMBER_VALID;
    }

    /**
     * The logger related.
     */
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final String TAG = "ContactNumberUtils";

    /**
     * Contact number length.
     */
    // As per E164 the maximum number length should be 15.
    // But as per implemention of libphonenumber it found longer length in Germany.
    // So we use the same length as libphonenumber.
    private int NUMBER_LENGTH_MAX = 17;
    private int NUMBER_LENGTH_NORMAL = 10;
    private int NUMBER_LENGTH_NO_AREA_CODE = 7;

    /**
     * Save the singleton instance.
     */
    private static ContactNumberUtils sInstance = null;
    private Context mContext = null;

    /**
     * Constructor
     */
    private ContactNumberUtils() {
        if (DEBUG) {
            Log.d(TAG, "ContactNumberUtils constructor");
        }
    }

    /**
     * Add device's own area code to the number which length is 7.
     */
    private String addAreaCode(String number) {
        if (mContext == null) {
            if (DEBUG) {
                Log.e(TAG, "mContext is null, please update context.");
            }
            return number;
        }

        String mdn = null;
        TelephonyManager tm = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mdn = tm.getLine1Number();

        if ((mdn == null) || (mdn.length() == 0) ||  mdn.startsWith("00000")) {
            return number;
        }

        mdn = PhoneNumberUtils.stripSeparators(mdn);
        if (mdn.length() >= NUMBER_LENGTH_NORMAL) {
            mdn = mdn.substring(mdn.length() - NUMBER_LENGTH_NORMAL);
        }
        mdn = mdn.substring(0, 3);

        number = mdn + number;
        return number;
    }

    /**
     * The excluded number list.
     */
    private static ArrayList<String> sExcludes = null;

    private boolean isInExcludedList(String number){
        if (sExcludes == null) {
            sExcludes = new ArrayList<String>();
            sExcludes.add("800");
            sExcludes.add("822");
            sExcludes.add("833");
            sExcludes.add("844");
            sExcludes.add("855");
            sExcludes.add("866");
            sExcludes.add("877");
            sExcludes.add("880882");
            sExcludes.add("888");
            sExcludes.add("900");
            sExcludes.add("911");
        }

        String tempNumber = format(number);
        if(TextUtils.isEmpty(tempNumber)) {
            return true; //exclude empty/null string.
        }

        if(tempNumber.startsWith("1")) {
            tempNumber = tempNumber.substring(1);
        } else if(tempNumber.startsWith("+1")) {
            tempNumber = tempNumber.substring(2);
        }

        if(TextUtils.isEmpty(tempNumber)) {
            return true; //exclude empty/null string.
        }

        for (String num : sExcludes) {
            if(tempNumber.startsWith(num)) {
                return true;
            }
        }

        return false;
    }
}

