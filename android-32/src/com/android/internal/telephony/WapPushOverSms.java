/*
 * Copyright (C) 2008 The Android Open Source Project
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

import static android.os.PowerWhitelistManager.REASON_EVENT_MMS;
import static android.os.PowerWhitelistManager.TEMPORARY_ALLOWLIST_TYPE_FOREGROUND_SERVICE_ALLOWED;

import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerWhitelistManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import com.android.internal.telephony.uicc.IccUtils;
import com.android.telephony.Rlog;

import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduParser;

import java.util.HashMap;
import java.util.List;

/**
 * WAP push handler class.
 *
 * @hide
 */
public class WapPushOverSms implements ServiceConnection {
    private static final String TAG = "WAP PUSH";
    private static final boolean DBG = false;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final Context mContext;
    PowerWhitelistManager mPowerWhitelistManager;

    private String mWapPushManagerPackage;

    /** Assigned from ServiceConnection callback on main threaad. */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private volatile IWapPushManager mWapPushManager;

    private void bindWapPushManagerService(Context context) {
        Intent intent = new Intent(IWapPushManager.class.getName());
        ComponentName comp = resolveSystemService(context.getPackageManager(), intent);
        intent.setComponent(comp);
        if (comp == null || !context.bindService(intent, this, Context.BIND_AUTO_CREATE)) {
            Rlog.e(TAG, "bindService() for wappush manager failed");
        } else {
            synchronized (this) {
                mWapPushManagerPackage = comp.getPackageName();
            }
            if (DBG) Rlog.v(TAG, "bindService() for wappush manager succeeded");
        }
    }

    /**
     * Special function for use by the system to resolve service
     * intents to system apps.  Throws an exception if there are
     * multiple potential matches to the Intent.  Returns null if
     * there are no matches.
     */
    private static @Nullable ComponentName resolveSystemService(@NonNull PackageManager pm,
            @NonNull Intent intent) {
        List<ResolveInfo> results = pm.queryIntentServices(
                intent, PackageManager.MATCH_SYSTEM_ONLY);
        if (results == null) {
            return null;
        }
        ComponentName comp = null;
        for (int i = 0; i < results.size(); i++) {
            ResolveInfo ri = results.get(i);
            ComponentName foundComp = new ComponentName(ri.serviceInfo.applicationInfo.packageName,
                    ri.serviceInfo.name);
            if (comp != null) {
                throw new IllegalStateException("Multiple system services handle " + intent
                    + ": " + comp + ", " + foundComp);
            }
            comp = foundComp;
        }
        return comp;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mWapPushManager = IWapPushManager.Stub.asInterface(service);
        if (DBG) Rlog.v(TAG, "wappush manager connected to " + hashCode());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mWapPushManager = null;
        if (DBG) Rlog.v(TAG, "wappush manager disconnected.");
    }

    public WapPushOverSms(Context context) {
        mContext = context;
        mPowerWhitelistManager = mContext.getSystemService(PowerWhitelistManager.class);

        UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);

        bindWapPushManagerService(mContext);
    }

    public void dispose() {
        if (mWapPushManager != null) {
            if (DBG) Rlog.v(TAG, "dispose: unbind wappush manager");
            mContext.unbindService(this);
        } else {
            Rlog.e(TAG, "dispose: not bound to a wappush manager");
        }
    }

    /**
     * Decodes the wap push pdu. The decoded result is wrapped inside the {@link DecodedResult}
     * object. The caller of this method should check {@link DecodedResult#statusCode} for the
     * decoding status. It  can have the following values.
     *
     * Activity.RESULT_OK - the wap push pdu is successfully decoded and should be further processed
     * Intents.RESULT_SMS_HANDLED - the wap push pdu should be ignored.
     * Intents.RESULT_SMS_GENERIC_ERROR - the pdu is invalid.
     */
    private DecodedResult decodeWapPdu(byte[] pdu, InboundSmsHandler handler) {
        DecodedResult result = new DecodedResult();
        if (DBG) Rlog.d(TAG, "Rx: " + IccUtils.bytesToHexString(pdu));

        try {
            int index = 0;
            int transactionId = pdu[index++] & 0xFF;
            int pduType = pdu[index++] & 0xFF;

            // Should we "abort" if no subId for now just no supplying extra param below
            int phoneId = handler.getPhone().getPhoneId();

            if ((pduType != WspTypeDecoder.PDU_TYPE_PUSH) &&
                    (pduType != WspTypeDecoder.PDU_TYPE_CONFIRMED_PUSH)) {
                index = mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_valid_wappush_index);
                if (index != -1) {
                    transactionId = pdu[index++] & 0xff;
                    pduType = pdu[index++] & 0xff;
                    if (DBG)
                        Rlog.d(TAG, "index = " + index + " PDU Type = " + pduType +
                                " transactionID = " + transactionId);

                    // recheck wap push pduType
                    if ((pduType != WspTypeDecoder.PDU_TYPE_PUSH)
                            && (pduType != WspTypeDecoder.PDU_TYPE_CONFIRMED_PUSH)) {
                        if (DBG) Rlog.w(TAG, "Received non-PUSH WAP PDU. Type = " + pduType);
                        result.statusCode = Intents.RESULT_SMS_HANDLED;
                        return result;
                    }
                } else {
                    if (DBG) Rlog.w(TAG, "Received non-PUSH WAP PDU. Type = " + pduType);
                    result.statusCode = Intents.RESULT_SMS_HANDLED;
                    return result;
                }
            }
            WspTypeDecoder pduDecoder =
                    TelephonyComponentFactory.getInstance().inject(WspTypeDecoder.class.getName())
                            .makeWspTypeDecoder(pdu);

            /**
             * Parse HeaderLen(unsigned integer).
             * From wap-230-wsp-20010705-a section 8.1.2
             * The maximum size of a uintvar is 32 bits.
             * So it will be encoded in no more than 5 octets.
             */
            if (pduDecoder.decodeUintvarInteger(index) == false) {
                if (DBG) Rlog.w(TAG, "Received PDU. Header Length error.");
                result.statusCode = Intents.RESULT_SMS_GENERIC_ERROR;
                return result;
            }
            int headerLength = (int) pduDecoder.getValue32();
            index += pduDecoder.getDecodedDataLength();

            int headerStartIndex = index;

            /**
             * Parse Content-Type.
             * From wap-230-wsp-20010705-a section 8.4.2.24
             *
             * Content-type-value = Constrained-media | Content-general-form
             * Content-general-form = Value-length Media-type
             * Media-type = (Well-known-media | Extension-Media) *(Parameter)
             * Value-length = Short-length | (Length-quote Length)
             * Short-length = <Any octet 0-30>   (octet <= WAP_PDU_SHORT_LENGTH_MAX)
             * Length-quote = <Octet 31>         (WAP_PDU_LENGTH_QUOTE)
             * Length = Uintvar-integer
             */
            if (pduDecoder.decodeContentType(index) == false) {
                if (DBG) Rlog.w(TAG, "Received PDU. Header Content-Type error.");
                result.statusCode = Intents.RESULT_SMS_GENERIC_ERROR;
                return result;
            }

            String mimeType = pduDecoder.getValueString();
            long binaryContentType = pduDecoder.getValue32();
            index += pduDecoder.getDecodedDataLength();

            byte[] header = new byte[headerLength];
            System.arraycopy(pdu, headerStartIndex, header, 0, header.length);

            byte[] intentData;

            if (mimeType != null && mimeType.equals(WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO)) {
                intentData = pdu;
            } else {
                int dataIndex = headerStartIndex + headerLength;
                intentData = new byte[pdu.length - dataIndex];
                System.arraycopy(pdu, dataIndex, intentData, 0, intentData.length);
            }

            int[] subIds = SubscriptionManager.getSubId(phoneId);
            int subId = (subIds != null) && (subIds.length > 0) ? subIds[0]
                    : SmsManager.getDefaultSmsSubscriptionId();

            // Continue if PDU parsing fails: the default messaging app may successfully parse the
            // same PDU.
            GenericPdu parsedPdu = null;
            try {
                parsedPdu = new PduParser(intentData, shouldParseContentDisposition(subId)).parse();
            } catch (Exception e) {
                Rlog.e(TAG, "Unable to parse PDU: " + e.toString());
            }

            if (parsedPdu != null && parsedPdu.getMessageType() == MESSAGE_TYPE_NOTIFICATION_IND) {
                final NotificationInd nInd = (NotificationInd) parsedPdu;
                if (nInd.getFrom() != null
                        && BlockChecker.isBlocked(mContext, nInd.getFrom().getString(), null)) {
                    result.statusCode = Intents.RESULT_SMS_HANDLED;
                    return result;
                }
            }

            /**
             * Seek for application ID field in WSP header.
             * If application ID is found, WapPushManager substitute the message
             * processing. Since WapPushManager is optional module, if WapPushManager
             * is not found, legacy message processing will be continued.
             */
            if (pduDecoder.seekXWapApplicationId(index, index + headerLength - 1)) {
                index = (int) pduDecoder.getValue32();
                pduDecoder.decodeXWapApplicationId(index);
                String wapAppId = pduDecoder.getValueString();
                if (wapAppId == null) {
                    wapAppId = Integer.toString((int) pduDecoder.getValue32());
                }
                result.wapAppId = wapAppId;
                String contentType = ((mimeType == null) ?
                        Long.toString(binaryContentType) : mimeType);
                result.contentType = contentType;
                if (DBG) Rlog.v(TAG, "appid found: " + wapAppId + ":" + contentType);
            }

            result.subId = subId;
            result.phoneId = phoneId;
            result.parsedPdu = parsedPdu;
            result.mimeType = mimeType;
            result.transactionId = transactionId;
            result.pduType = pduType;
            result.header = header;
            result.intentData = intentData;
            result.contentTypeParameters = pduDecoder.getContentParameters();
            result.statusCode = Activity.RESULT_OK;
        } catch (ArrayIndexOutOfBoundsException aie) {
            // 0-byte WAP PDU or other unexpected WAP PDU contents can easily throw this;
            // log exception string without stack trace and return false.
            Rlog.e(TAG, "ignoring dispatchWapPdu() array index exception: " + aie);
            result.statusCode = Intents.RESULT_SMS_GENERIC_ERROR;
        }
        return result;
    }

    /**
     * Dispatches inbound messages that are in the WAP PDU format. See
     * wap-230-wsp-20010705-a section 8 for details on the WAP PDU format.
     *
     * @param pdu The WAP PDU, made up of one or more SMS PDUs
     * @param address The originating address
     * @return a result code from {@link android.provider.Telephony.Sms.Intents}, or
     *         {@link Activity#RESULT_OK} if the message has been broadcast
     *         to applications
     */
    public int dispatchWapPdu(byte[] pdu, InboundSmsHandler.SmsBroadcastReceiver receiver,
            InboundSmsHandler handler, String address, int subId, long messageId) {
        DecodedResult result = decodeWapPdu(pdu, handler);
        if (result.statusCode != Activity.RESULT_OK) {
            return result.statusCode;
        }

        /**
         * If the pdu has application ID, WapPushManager substitute the message
         * processing. Since WapPushManager is optional module, if WapPushManager
         * is not found, legacy message processing will be continued.
         */
        if (result.wapAppId != null) {
            try {
                boolean processFurther = true;
                IWapPushManager wapPushMan = mWapPushManager;

                if (wapPushMan == null) {
                    if (DBG) Rlog.w(TAG, "wap push manager not found!");
                } else {
                    synchronized (this) {
                        mPowerWhitelistManager.whitelistAppTemporarilyForEvent(
                                mWapPushManagerPackage, PowerWhitelistManager.EVENT_MMS,
                                REASON_EVENT_MMS, "mms-mgr");
                    }

                    Intent intent = new Intent();
                    intent.putExtra("transactionId", result.transactionId);
                    intent.putExtra("pduType", result.pduType);
                    intent.putExtra("header", result.header);
                    intent.putExtra("data", result.intentData);
                    intent.putExtra("contentTypeParameters", result.contentTypeParameters);
                    SubscriptionManager.putPhoneIdAndSubIdExtra(intent, result.phoneId);
                    if (!TextUtils.isEmpty(address)) {
                        intent.putExtra("address", address);
                    }

                    int procRet = wapPushMan.processMessage(
                        result.wapAppId, result.contentType, intent);
                    if (DBG) Rlog.v(TAG, "procRet:" + procRet);
                    if ((procRet & WapPushManagerParams.MESSAGE_HANDLED) > 0
                            && (procRet & WapPushManagerParams.FURTHER_PROCESSING) == 0) {
                        processFurther = false;
                    }
                }
                if (!processFurther) {
                    return Intents.RESULT_SMS_HANDLED;
                }
            } catch (RemoteException e) {
                if (DBG) Rlog.w(TAG, "remote func failed...");
            }
        }
        if (DBG) Rlog.v(TAG, "fall back to existing handler");

        if (result.mimeType == null) {
            if (DBG) Rlog.w(TAG, "Header Content-Type error.");
            return Intents.RESULT_SMS_GENERIC_ERROR;
        }

        Intent intent = new Intent(Intents.WAP_PUSH_DELIVER_ACTION);
        intent.setType(result.mimeType);
        intent.putExtra("transactionId", result.transactionId);
        intent.putExtra("pduType", result.pduType);
        intent.putExtra("header", result.header);
        intent.putExtra("data", result.intentData);
        intent.putExtra("contentTypeParameters", result.contentTypeParameters);
        if (!TextUtils.isEmpty(address)) {
            intent.putExtra("address", address);
        }
        if (messageId != 0L) {
            intent.putExtra("messageId", messageId);
        }

        // Direct the intent to only the default MMS app. If we can't find a default MMS app
        // then sent it to all broadcast receivers.
        ComponentName componentName = SmsApplication.getDefaultMmsApplication(mContext, true);
        Bundle options = null;
        if (componentName != null) {
            // Deliver MMS message only to this receiver
            intent.setComponent(componentName);
            if (DBG) Rlog.v(TAG, "Delivering MMS to: " + componentName.getPackageName() +
                    " " + componentName.getClassName());
            long duration = mPowerWhitelistManager.whitelistAppTemporarilyForEvent(
                    componentName.getPackageName(), PowerWhitelistManager.EVENT_MMS,
                    REASON_EVENT_MMS, "mms-app");
            BroadcastOptions bopts = BroadcastOptions.makeBasic();
            bopts.setTemporaryAppAllowlist(duration,
                    TEMPORARY_ALLOWLIST_TYPE_FOREGROUND_SERVICE_ALLOWED,
                    REASON_EVENT_MMS,
                    "");
            options = bopts.toBundle();
        }

        handler.dispatchIntent(intent, getPermissionForType(result.mimeType),
                getAppOpsStringPermissionForIntent(result.mimeType), options, receiver,
                UserHandle.SYSTEM, subId);
        return Activity.RESULT_OK;
    }

    /**
     * Check whether the pdu is a MMS WAP push pdu that should be dispatched to the SMS app.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isWapPushForMms(byte[] pdu, InboundSmsHandler handler) {
        DecodedResult result = decodeWapPdu(pdu, handler);
        return result.statusCode == Activity.RESULT_OK
            && WspTypeDecoder.CONTENT_TYPE_B_MMS.equals(result.mimeType);
    }

    private static boolean shouldParseContentDisposition(int subId) {
        return SmsManager
                .getSmsManagerForSubscriptionId(subId)
                .getCarrierConfigValues()
                .getBoolean(SmsManager.MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION, true);
    }

    public static String getPermissionForType(String mimeType) {
        String permission;
        if (WspTypeDecoder.CONTENT_TYPE_B_MMS.equals(mimeType)) {
            permission = android.Manifest.permission.RECEIVE_MMS;
        } else {
            permission = android.Manifest.permission.RECEIVE_WAP_PUSH;
        }
        return permission;
    }

    /**
     * Return a appOps String for the given MIME type.
     * @param mimeType MIME type of the Intent
     * @return The appOps String
     */
    public static String getAppOpsStringPermissionForIntent(String mimeType) {
        String appOp;
        if (WspTypeDecoder.CONTENT_TYPE_B_MMS.equals(mimeType)) {
            appOp = AppOpsManager.OPSTR_RECEIVE_MMS;
        } else {
            appOp = AppOpsManager.OPSTR_RECEIVE_WAP_PUSH;
        }
        return appOp;
    }

    /**
     * Place holder for decoded Wap pdu data.
     */
    private final class DecodedResult {
        String mimeType;
        String contentType;
        int transactionId;
        int pduType;
        int phoneId;
        int subId;
        byte[] header;
        String wapAppId;
        byte[] intentData;
        HashMap<String, String> contentTypeParameters;
        GenericPdu parsedPdu;
        int statusCode;
    }
}
