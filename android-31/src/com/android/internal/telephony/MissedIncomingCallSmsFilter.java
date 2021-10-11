/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The SMS filter for parsing SMS from carrier to notify users about the missed incoming call.
 */
public class MissedIncomingCallSmsFilter {
    private static final String TAG = MissedIncomingCallSmsFilter.class.getSimpleName();

    private static final boolean VDBG = false;    // STOPSHIP if true

    private static final String SMS_YEAR_TAG = "year";

    private static final String SMS_MONTH_TAG = "month";

    private static final String SMS_DAY_TAG = "day";

    private static final String SMS_HOUR_TAG = "hour";

    private static final String SMS_MINUTE_TAG = "minute";

    private static final String SMS_CALLER_ID_TAG = "callerId";

    private static final ComponentName PSTN_CONNECTION_SERVICE_COMPONENT =
            new ComponentName("com.android.phone",
                    "com.android.services.telephony.TelephonyConnectionService");

    private final Phone mPhone;

    private PersistableBundle mCarrierConfig;

    /**
     * Constructor
     *
     * @param phone The phone instance
     */
    public MissedIncomingCallSmsFilter(Phone phone) {
        mPhone = phone;

        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            mCarrierConfig = configManager.getConfigForSubId(mPhone.getSubId());
        }
    }

    /**
     * Check if the message is missed incoming call SMS, which is sent from the carrier to notify
     * the user about the missed incoming call earlier.
     *
     * @param pdus SMS pdu binary
     * @param format Either {@link SmsConstants#FORMAT_3GPP} or {@link SmsConstants#FORMAT_3GPP2}
     * @return {@code true} if this is an SMS for notifying the user about missed incoming call.
     */
    public boolean filter(byte[][] pdus, String format) {
        // The missed incoming call SMS must be one page only, and if not we should ignore it.
        if (pdus.length != 1) {
            return false;
        }

        if (mCarrierConfig != null) {
            String[] originators = mCarrierConfig.getStringArray(CarrierConfigManager
                    .KEY_MISSED_INCOMING_CALL_SMS_ORIGINATOR_STRING_ARRAY);
            if (originators != null) {
                SmsMessage message = SmsMessage.createFromPdu(pdus[0], format);
                if (message != null
                        && !TextUtils.isEmpty(message.getOriginatingAddress())
                        && Arrays.asList(originators).contains(message.getOriginatingAddress())) {
                    return processSms(message);
                }
            }
        }
        return false;
    }

    /**
     * Get the Epoch time.
     *
     * @param year Year in string format. If this param is null or empty, a guessed year will be
     * used. Some carriers do not provide this information in the SMS.
     * @param month Month in string format.
     * @param day Day in string format.
     * @param hour Hour in string format.
     * @param minute Minute in string format.
     * @return The Epoch time in milliseconds.
     */
    private long getEpochTime(String year, String month, String day, String hour, String minute) {
        LocalDateTime now = LocalDateTime.now();
        if (TextUtils.isEmpty(year)) {
            // If year is not provided, guess the year from current time.
            year = Integer.toString(now.getYear());
        }

        LocalDateTime time;
        // Check if the guessed year is reasonable. If it's the future, then the year must be
        // the previous year. For example, the missed call's month and day is 12/31, but current
        // date is 1/1/2020, then the year of missed call must be 2019.
        do {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
            time = LocalDateTime.parse(year + month + day + hour + minute, formatter);
            year = Integer.toString(Integer.parseInt(year) - 1);
        } while (time.isAfter(now));

        Instant instant = time.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }

    /**
     * Process the SMS message
     *
     * @param message SMS message
     *
     * @return {@code true} if the SMS message has been processed as a missed incoming call SMS.
     */
    private boolean processSms(@NonNull SmsMessage message) {
        long missedCallTime = 0;
        String callerId = null;

        String[] smsPatterns = mCarrierConfig.getStringArray(CarrierConfigManager
                .KEY_MISSED_INCOMING_CALL_SMS_PATTERN_STRING_ARRAY);
        if (smsPatterns == null || smsPatterns.length == 0) {
            Rlog.w(TAG, "Missed incoming call SMS pattern is not configured!");
            return false;
        }

        for (String smsPattern : smsPatterns) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(smsPattern, Pattern.DOTALL | Pattern.UNIX_LINES);
            } catch (PatternSyntaxException e) {
                Rlog.w(TAG, "Configuration error. Unexpected missed incoming call sms "
                        + "pattern: " + smsPattern + ", e=" + e);
                continue;
            }

            Matcher matcher = pattern.matcher(message.getMessageBody());
            String year = null, month = null, day = null, hour = null, minute = null;
            if (matcher.find()) {
                try {
                    month = matcher.group(SMS_MONTH_TAG);
                    day = matcher.group(SMS_DAY_TAG);
                    hour = matcher.group(SMS_HOUR_TAG);
                    minute = matcher.group(SMS_MINUTE_TAG);
                    if (VDBG) {
                        Rlog.v(TAG, "month=" + month + ", day=" + day + ", hour=" + hour
                                + ", minute=" + minute);
                    }
                } catch (IllegalArgumentException e) {
                    if (VDBG) {
                        Rlog.v(TAG, "One of the critical date field is missing. Using the "
                                + "current time for missed incoming call.");
                    }
                    missedCallTime = System.currentTimeMillis();
                }

                // Year is an optional field.
                try {
                    year = matcher.group(SMS_YEAR_TAG);
                } catch (IllegalArgumentException e) {
                    if (VDBG) Rlog.v(TAG, "Year is missing.");
                }

                try {
                    if (missedCallTime == 0) {
                        missedCallTime = getEpochTime(year, month, day, hour, minute);
                        if (missedCallTime == 0) {
                            Rlog.e(TAG, "Can't get the time. Use the current time.");
                            missedCallTime = System.currentTimeMillis();
                        }
                    }

                    if (VDBG) Rlog.v(TAG, "missedCallTime=" + missedCallTime);
                } catch (Exception e) {
                    Rlog.e(TAG, "Can't get the time for missed incoming call");
                }

                try {
                    callerId = matcher.group(SMS_CALLER_ID_TAG);
                    if (VDBG) Rlog.v(TAG, "caller id=" + callerId);
                } catch (IllegalArgumentException e) {
                    Rlog.d(TAG, "Caller id is not provided or can't be parsed.");
                }
                createMissedIncomingCallEvent(missedCallTime, callerId);
                return true;
            }
        }

        Rlog.d(TAG, "SMS did not match any missed incoming call SMS pattern.");
        return false;
    }

    // Create phone account. The logic is copied from PhoneUtils.makePstnPhoneAccountHandle.
    private static PhoneAccountHandle makePstnPhoneAccountHandle(Phone phone) {
        return new PhoneAccountHandle(PSTN_CONNECTION_SERVICE_COMPONENT,
                String.valueOf(phone.getFullIccSerialNumber()));
    }

    /**
     * Create the missed incoming call through TelecomManager.
     *
     * @param missedCallTime the time of missed incoming call in. This is the EPOCH time in
     * milliseconds.
     * @param callerId The caller id of the missed incoming call.
     */
    private void createMissedIncomingCallEvent(long missedCallTime, @Nullable String callerId) {
        TelecomManager tm = (TelecomManager) mPhone.getContext()
                .getSystemService(Context.TELECOM_SERVICE);

        if (tm != null) {
            Bundle bundle = new Bundle();

            if (callerId != null) {
                final Uri phoneUri = Uri.fromParts(
                        PhoneAccount.SCHEME_TEL, callerId, null);
                bundle.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, phoneUri);
            }

            // Need to use the Epoch time instead of the elapsed time because it's possible
            // the missed incoming call occurred before the phone boots up.
            bundle.putLong(TelecomManager.EXTRA_CALL_CREATED_EPOCH_TIME_MILLIS, missedCallTime);
            tm.addNewIncomingCall(makePstnPhoneAccountHandle(mPhone), bundle);
        }
    }
}
