/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.internal.telephony;

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.VoicemailContract;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VisualVoicemailSms;
import android.telephony.VisualVoicemailSmsFilterSettings;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.VisualVoicemailSmsParser.WrappedMessageData;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Filters SMS to {@link android.telephony.VisualVoicemailService}, based on the config from {@link
 * VisualVoicemailSmsFilterSettings}. The SMS is sent to telephony service which will do the actual
 * dispatching.
 */
public class VisualVoicemailSmsFilter {

    /**
     * Interface to convert subIds so the logic can be replaced in tests.
     */
    @VisibleForTesting
    public interface PhoneAccountHandleConverter {

        /**
         * Convert the subId to a {@link PhoneAccountHandle}
         */
        PhoneAccountHandle fromSubId(int subId);
    }

    private static final String TAG = "VvmSmsFilter";

    private static final String TELEPHONY_SERVICE_PACKAGE = "com.android.phone";

    private static final ComponentName PSTN_CONNECTION_SERVICE_COMPONENT =
            new ComponentName("com.android.phone",
                    "com.android.services.telephony.TelephonyConnectionService");

    private static Map<String, List<Pattern>> sPatterns;

    private static final PhoneAccountHandleConverter DEFAULT_PHONE_ACCOUNT_HANDLE_CONVERTER =
            new PhoneAccountHandleConverter() {

                @Override
                public PhoneAccountHandle fromSubId(int subId) {
                    if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                        return null;
                    }
                    int phoneId = SubscriptionManager.getPhoneId(subId);
                    if (phoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
                        return null;
                    }
                    return new PhoneAccountHandle(PSTN_CONNECTION_SERVICE_COMPONENT,
                            Integer.toString(PhoneFactory.getPhone(phoneId).getSubId()));
                }
            };

    private static PhoneAccountHandleConverter sPhoneAccountHandleConverter =
            DEFAULT_PHONE_ACCOUNT_HANDLE_CONVERTER;

    /**
     * Wrapper to combine multiple PDU into an SMS message
     */
    private static class FullMessage {

        public SmsMessage firstMessage;
        public String fullMessageBody;
    }

    /**
     * Attempt to parse the incoming SMS as a visual voicemail SMS. If the parsing succeeded, A
     * {@link VoicemailContract#ACTION_VOICEMAIL_SMS_RECEIVED} intent will be sent to telephony
     * service, and the SMS will be dropped.
     *
     * <p>The accepted format for a visual voicemail SMS is a generalization of the OMTP format:
     *
     * <p>[clientPrefix]:[prefix]:([key]=[value];)*
     *
     * Additionally, if the SMS does not match the format, but matches the regex specified by the
     * carrier in {@link com.android.internal.R.array#config_vvmSmsFilterRegexes}, the SMS will
     * still be dropped and a {@link VoicemailContract#ACTION_VOICEMAIL_SMS_RECEIVED} will be sent.
     *
     * @return true if the SMS has been parsed to be a visual voicemail SMS and should be dropped
     */
    public static boolean filter(Context context, byte[][] pdus, String format, int destPort,
            int subId) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        VisualVoicemailSmsFilterSettings settings;
        settings = telephonyManager.getActiveVisualVoicemailSmsFilterSettings(subId);

        if (settings == null) {
            FullMessage fullMessage = getFullMessage(pdus, format);
            if (fullMessage != null) {
                // This is special case that voice mail SMS received before the filter has been
                // set. To drop the SMS unconditionally.
                if (messageBodyMatchesVvmPattern(context, subId, fullMessage.fullMessageBody)) {
                    Log.e(TAG, "SMS matching VVM format received but the filter not been set yet");
                    return true;
                }
            }
            return false;
        }

        PhoneAccountHandle phoneAccountHandle = sPhoneAccountHandleConverter.fromSubId(subId);

        if (phoneAccountHandle == null) {
            Log.e(TAG, "Unable to convert subId " + subId + " to PhoneAccountHandle");
            return false;
        }

        String clientPrefix = settings.clientPrefix;
        FullMessage fullMessage = getFullMessage(pdus, format);

        if (fullMessage == null) {
            // Carrier WAP push SMS is not recognized by android, which has a ascii PDU.
            // Attempt to parse it.
            Log.i(TAG, "Unparsable SMS received");
            String asciiMessage = parseAsciiPduMessage(pdus);
            WrappedMessageData messageData = VisualVoicemailSmsParser
                    .parseAlternativeFormat(asciiMessage);
            if (messageData == null) {
                Log.i(TAG, "Attempt to parse ascii PDU");
                messageData = VisualVoicemailSmsParser.parse(clientPrefix, asciiMessage);
            }
            if (messageData != null) {
                sendVvmSmsBroadcast(context, settings, phoneAccountHandle, messageData, null);
            }
            // Confidence for what the message actually is is low. Don't remove the message and let
            // system decide. Usually because it is not parsable it will be dropped.
            return false;
        }

        String messageBody = fullMessage.fullMessageBody;
        WrappedMessageData messageData = VisualVoicemailSmsParser
                .parse(clientPrefix, messageBody);
        if (messageData != null) {
            if (settings.destinationPort
                    == VisualVoicemailSmsFilterSettings.DESTINATION_PORT_DATA_SMS) {
                if (destPort == -1) {
                    // Non-data SMS is directed to the port "-1".
                    Log.i(TAG, "SMS matching VVM format received but is not a DATA SMS");
                    return false;
                }
            } else if (settings.destinationPort
                    != VisualVoicemailSmsFilterSettings.DESTINATION_PORT_ANY) {
                if (settings.destinationPort != destPort) {
                    Log.i(TAG, "SMS matching VVM format received but is not directed to port "
                            + settings.destinationPort);
                    return false;
                }
            }

            if (!settings.originatingNumbers.isEmpty()
                    && !isSmsFromNumbers(fullMessage.firstMessage, settings.originatingNumbers)) {
                Log.i(TAG, "SMS matching VVM format received but is not from originating numbers");
                return false;
            }

            sendVvmSmsBroadcast(context, settings, phoneAccountHandle, messageData, null);
            return true;
        }

        if (messageBodyMatchesVvmPattern(context, subId, messageBody)) {
            Log.w(TAG,
                    "SMS matches pattern but has illegal format, still dropping as VVM SMS");
            sendVvmSmsBroadcast(context, settings, phoneAccountHandle, null, messageBody);
            return true;
        }
        return false;
    }

    private static boolean messageBodyMatchesVvmPattern(Context context, int subId,
            String messageBody) {
        buildPatternsMap(context);
        String mccMnc = context.getSystemService(TelephonyManager.class).getSimOperator(subId);

        List<Pattern> patterns = sPatterns.get(mccMnc);
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }

        for (Pattern pattern : patterns) {
            if (pattern.matcher(messageBody).matches()) {
                Log.w(TAG, "Incoming SMS matches pattern " + pattern);
                return true;
            }
        }
        return false;
    }

    /**
     * override how subId is converted to PhoneAccountHandle for tests
     */
    @VisibleForTesting
    public static void setPhoneAccountHandleConverterForTest(
            PhoneAccountHandleConverter converter) {
        if (converter == null) {
            sPhoneAccountHandleConverter = DEFAULT_PHONE_ACCOUNT_HANDLE_CONVERTER;
        } else {
            sPhoneAccountHandleConverter = converter;
        }
    }

    private static void buildPatternsMap(Context context) {
        if (sPatterns != null) {
            return;
        }
        sPatterns = new ArrayMap<>();
        // TODO(twyen): build from CarrierConfig once public API can be updated.
        for (String entry : context.getResources()
                .getStringArray(com.android.internal.R.array.config_vvmSmsFilterRegexes)) {
            String[] mccMncList = entry.split(";")[0].split(",");
            Pattern pattern = Pattern.compile(entry.split(";")[1]);

            for (String mccMnc : mccMncList) {
                if (!sPatterns.containsKey(mccMnc)) {
                    sPatterns.put(mccMnc, new ArrayList<>());
                }
                sPatterns.get(mccMnc).add(pattern);
            }
        }
    }

    private static void sendVvmSmsBroadcast(Context context,
            VisualVoicemailSmsFilterSettings filterSettings, PhoneAccountHandle phoneAccountHandle,
            @Nullable WrappedMessageData messageData, @Nullable String messageBody) {
        Log.i(TAG, "VVM SMS received");
        Intent intent = new Intent(VoicemailContract.ACTION_VOICEMAIL_SMS_RECEIVED);
        VisualVoicemailSms.Builder builder = new VisualVoicemailSms.Builder();
        if (messageData != null) {
            builder.setPrefix(messageData.prefix);
            builder.setFields(messageData.fields);
        }
        if (messageBody != null) {
            builder.setMessageBody(messageBody);
        }
        builder.setPhoneAccountHandle(phoneAccountHandle);
        intent.putExtra(VoicemailContract.EXTRA_VOICEMAIL_SMS, builder.build());
        intent.putExtra(VoicemailContract.EXTRA_TARGET_PACKAGE, filterSettings.packageName);
        intent.setPackage(TELEPHONY_SERVICE_PACKAGE);
        context.sendBroadcast(intent);
    }

    /**
     * @return the message body of the SMS, or {@code null} if it can not be parsed.
     */
    @Nullable
    private static FullMessage getFullMessage(byte[][] pdus, String format) {
        FullMessage result = new FullMessage();
        StringBuilder builder = new StringBuilder();
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        for (byte pdu[] : pdus) {
            SmsMessage message = SmsMessage.createFromPdu(pdu, format);
            if (message == null) {
                // The PDU is not recognized by android
                return null;
            }
            if (result.firstMessage == null) {
                result.firstMessage = message;
            }
            String body = message.getMessageBody();
            if (body == null && message.getUserData() != null) {
                // Attempt to interpret the user data as UTF-8. UTF-8 string over data SMS using
                // 8BIT data coding scheme is our recommended way to send VVM SMS and is used in CTS
                // Tests. The OMTP visual voicemail specification does not specify the SMS type and
                // encoding.
                ByteBuffer byteBuffer = ByteBuffer.wrap(message.getUserData());
                try {
                    body = decoder.decode(byteBuffer).toString();
                } catch (CharacterCodingException e) {
                    // User data is not decode-able as UTF-8. Ignoring.
                    return null;
                }
            }
            if (body != null) {
                builder.append(body);
            }
        }
        result.fullMessageBody = builder.toString();
        return result;
    }

    private static String parseAsciiPduMessage(byte[][] pdus) {
        StringBuilder builder = new StringBuilder();
        for (byte pdu[] : pdus) {
            builder.append(new String(pdu, StandardCharsets.US_ASCII));
        }
        return builder.toString();
    }

    private static boolean isSmsFromNumbers(SmsMessage message, List<String> numbers) {
        if (message == null) {
            Log.e(TAG, "Unable to create SmsMessage from PDU, cannot determine originating number");
            return false;
        }

        for (String number : numbers) {
            if (PhoneNumberUtils.compare(number, message.getOriginatingAddress())) {
                return true;
            }
        }
        return false;
    }
}
