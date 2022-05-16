/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2011-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

import static com.android.internal.telephony.util.TelephonyUtils.checkDumpPermission;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.net.Uri;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.TelephonyServiceManager.ServiceRegisterer;
import android.provider.Telephony.Sms.Intents;
import android.telephony.CarrierConfigManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyFrameworkInitializer;
import android.telephony.TelephonyManager;

import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Implements the ISmsImplBase interface used in the SmsManager API.
 */
public class SmsController extends ISmsImplBase {
    static final String LOG_TAG = "SmsController";

    private final Context mContext;

    protected SmsController(Context context) {
        mContext = context;
        ServiceRegisterer smsServiceRegisterer = TelephonyFrameworkInitializer
                .getTelephonyServiceManager()
                .getSmsServiceRegisterer();
        if (smsServiceRegisterer.get() == null) {
            smsServiceRegisterer.register(this);
        }
    }

    private Phone getPhone(int subId) {
        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
        if (phone == null) {
            phone = PhoneFactory.getDefaultPhone();
        }
        return phone;
    }

    private SmsPermissions getSmsPermissions(int subId) {
        Phone phone = getPhone(subId);

        return new SmsPermissions(phone, mContext,
                (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE));
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean updateMessageOnIccEfForSubscriber(int subId, String callingPackage, int index,
            int status, byte[] pdu) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.updateMessageOnIccEf(callingPackage, index, status, pdu);
        } else {
            Rlog.e(LOG_TAG, "updateMessageOnIccEfForSubscriber iccSmsIntMgr is null"
                    + " for Subscription: " + subId);
            return false;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean copyMessageToIccEfForSubscriber(int subId, String callingPackage, int status,
            byte[] pdu, byte[] smsc) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.copyMessageToIccEf(callingPackage, status, pdu, smsc);
        } else {
            Rlog.e(LOG_TAG, "copyMessageToIccEfForSubscriber iccSmsIntMgr is null"
                    + " for Subscription: " + subId);
            return false;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public List<SmsRawData> getAllMessagesFromIccEfForSubscriber(int subId, String callingPackage) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.getAllMessagesFromIccEf(callingPackage);
        } else {
            Rlog.e(LOG_TAG, "getAllMessagesFromIccEfForSubscriber iccSmsIntMgr is"
                    + " null for Subscription: " + subId);
            return null;
        }
    }

    /**
     * @deprecated Use {@link #sendDataForSubscriber(int, String, String, String, String, int,
     * byte[], PendingIntent, PendingIntent)} instead
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void sendDataForSubscriber(int subId, String callingPackage, String destAddr,
            String scAddr, int destPort, byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        sendDataForSubscriber(subId, callingPackage, null, destAddr, scAddr, destPort, data,
                sentIntent, deliveryIntent);
    }

    @Override
    public void sendDataForSubscriber(int subId, String callingPackage,
            String callingAttributionTag, String destAddr, String scAddr, int destPort, byte[] data,
            PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendData(callingPackage, callingAttributionTag, destAddr, scAddr, destPort,
                    data, sentIntent, deliveryIntent);
        } else {
            Rlog.e(LOG_TAG, "sendDataForSubscriber iccSmsIntMgr is null for"
                    + " Subscription: " + subId);
            // TODO: Use a more specific error code to replace RESULT_ERROR_GENERIC_FAILURE.
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    private void sendDataForSubscriberWithSelfPermissionsInternal(int subId, String callingPackage,
            String callingAttributionTag, String destAddr, String scAddr, int destPort, byte[] data,
            PendingIntent sentIntent, PendingIntent deliveryIntent, boolean isForVvm) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendDataWithSelfPermissions(callingPackage, callingAttributionTag,
                    destAddr, scAddr, destPort, data, sentIntent, deliveryIntent, isForVvm);
        } else {
            Rlog.e(LOG_TAG, "sendText iccSmsIntMgr is null for"
                    + " Subscription: " + subId);
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    private String getCallingPackage() {
        return mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid())[0];
    }

    @Override
    public void sendTextForSubscriber(int subId, String callingPackage,
            String callingAttributionTag, String destAddr, String scAddr, String text,
            PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean persistMessageForNonDefaultSmsApp, long messageId) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        if (!getSmsPermissions(subId).checkCallingCanSendText(persistMessageForNonDefaultSmsApp,
                callingPackage, callingAttributionTag, "Sending SMS message")) {
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
            return;
        }
        long token = Binder.clearCallingIdentity();
        SubscriptionInfo info;
        try {
            info = getSubscriptionInfo(subId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        if (isBluetoothSubscription(info)) {
            sendBluetoothText(info, destAddr, text, sentIntent, deliveryIntent);
        } else {
            sendIccText(subId, callingPackage, destAddr, scAddr, text, sentIntent, deliveryIntent,
                    persistMessageForNonDefaultSmsApp, messageId);
        }
    }

    private boolean isBluetoothSubscription(SubscriptionInfo info) {
        return info != null
                && info.getSubscriptionType() == SubscriptionManager.SUBSCRIPTION_TYPE_REMOTE_SIM;
    }

    private void sendBluetoothText(SubscriptionInfo info, String destAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        BtSmsInterfaceManager btSmsInterfaceManager = new BtSmsInterfaceManager();
        btSmsInterfaceManager.sendText(mContext, destAddr, text, sentIntent, deliveryIntent, info);
    }

    private void sendIccText(int subId, String callingPackage, String destAddr,
            String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean persistMessageForNonDefaultSmsApp, long messageId) {
        Rlog.d(LOG_TAG, "sendTextForSubscriber iccSmsIntMgr"
                + " Subscription: " + subId + " id: " + messageId);
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendText(callingPackage, destAddr, scAddr, text, sentIntent,
                    deliveryIntent, persistMessageForNonDefaultSmsApp, messageId);
        } else {
            Rlog.e(LOG_TAG, "sendTextForSubscriber iccSmsIntMgr is null for"
                    + " Subscription: " + subId + " id: " + messageId);
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    private void sendTextForSubscriberWithSelfPermissionsInternal(int subId, String callingPackage,
            String callingAttributeTag, String destAddr, String scAddr, String text,
            PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessage,
            boolean isForVvm) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendTextWithSelfPermissions(callingPackage, callingAttributeTag, destAddr,
                    scAddr, text, sentIntent, deliveryIntent, persistMessage, isForVvm);
        } else {
            Rlog.e(LOG_TAG, "sendText iccSmsIntMgr is null for"
                    + " Subscription: " + subId);
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    @Override
    public void sendTextForSubscriberWithOptions(int subId, String callingPackage,
            String callingAttributionTag, String destAddr, String scAddr, String parts,
            PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessage,
            int priority, boolean expectMore, int validityPeriod) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendTextWithOptions(callingPackage, callingAttributionTag, destAddr,
                    scAddr, parts, sentIntent, deliveryIntent, persistMessage, priority, expectMore,
                    validityPeriod);
        } else {
            Rlog.e(LOG_TAG, "sendTextWithOptions iccSmsIntMgr is null for"
                    + " Subscription: " + subId);
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    @Override
    public void sendMultipartTextForSubscriber(int subId, String callingPackage,
            String callingAttributionTag, String destAddr, String scAddr, List<String> parts,
            List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents,
            boolean persistMessageForNonDefaultSmsApp, long messageId) {
        // This is different from the checking of other method. It prefers the package name
        // returned by getCallPackage() for backward-compatibility.
        if (getCallingPackage() != null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendMultipartText(callingPackage, callingAttributionTag, destAddr, scAddr,
                    parts, sentIntents, deliveryIntents, persistMessageForNonDefaultSmsApp,
                    messageId);
        } else {
            Rlog.e(LOG_TAG, "sendMultipartTextForSubscriber iccSmsIntMgr is null for"
                    + " Subscription: " + subId + " id: " + messageId);
            sendErrorInPendingIntents(sentIntents, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    @Override
    public void sendMultipartTextForSubscriberWithOptions(int subId, String callingPackage,
            String callingAttributionTag, String destAddr, String scAddr, List<String> parts,
            List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents,
            boolean persistMessage, int priority, boolean expectMore, int validityPeriod) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendMultipartTextWithOptions(callingPackage, callingAttributionTag,
                    destAddr, scAddr, parts, sentIntents, deliveryIntents, persistMessage, priority,
                    expectMore, validityPeriod, 0L /* messageId */);
        } else {
            Rlog.e(LOG_TAG, "sendMultipartTextWithOptions iccSmsIntMgr is null for"
                    + " Subscription: " + subId);
            sendErrorInPendingIntents(sentIntents, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean enableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) {
        return enableCellBroadcastRangeForSubscriber(subId, messageIdentifier, messageIdentifier,
                ranType);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean enableCellBroadcastRangeForSubscriber(int subId, int startMessageId,
            int endMessageId, int ranType) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.enableCellBroadcastRange(startMessageId, endMessageId, ranType);
        } else {
            Rlog.e(LOG_TAG, "enableCellBroadcastRangeForSubscriber iccSmsIntMgr is null for"
                    + " Subscription: " + subId);
        }
        return false;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean disableCellBroadcastForSubscriber(int subId,
            int messageIdentifier, int ranType) {
        return disableCellBroadcastRangeForSubscriber(subId, messageIdentifier, messageIdentifier,
                ranType);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean disableCellBroadcastRangeForSubscriber(int subId, int startMessageId,
            int endMessageId, int ranType) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.disableCellBroadcastRange(startMessageId, endMessageId, ranType);
        } else {
            Rlog.e(LOG_TAG, "disableCellBroadcastRangeForSubscriber iccSmsIntMgr is null for"
                    + " Subscription:" + subId);
        }
        return false;
    }

    @Override
    public int getPremiumSmsPermission(String packageName) {
        return getPremiumSmsPermissionForSubscriber(getPreferredSmsSubscription(), packageName);
    }

    @Override
    public int getPremiumSmsPermissionForSubscriber(int subId, String packageName) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.getPremiumSmsPermission(packageName);
        } else {
            Rlog.e(LOG_TAG, "getPremiumSmsPermissionForSubscriber iccSmsIntMgr is null");
        }
        //TODO Rakesh
        return 0;
    }

    @Override
    public void setPremiumSmsPermission(String packageName, int permission) {
        setPremiumSmsPermissionForSubscriber(getPreferredSmsSubscription(), packageName,
                permission);
    }

    @Override
    public void setPremiumSmsPermissionForSubscriber(int subId, String packageName,
            int permission) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.setPremiumSmsPermission(packageName, permission);
        } else {
            Rlog.e(LOG_TAG, "setPremiumSmsPermissionForSubscriber iccSmsIntMgr is null");
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean isImsSmsSupportedForSubscriber(int subId) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.isImsSmsSupported();
        } else {
            Rlog.e(LOG_TAG, "isImsSmsSupportedForSubscriber iccSmsIntMgr is null");
        }
        return false;
    }

    @Override
    public boolean isSmsSimPickActivityNeeded(int subId) {
        final Context context = mContext.getApplicationContext();
        ActivityManager am = context.getSystemService(ActivityManager.class);
        // Don't show the SMS SIM Pick activity if it is not foreground.
        boolean isCallingProcessForeground = am != null
                && am.getUidImportance(Binder.getCallingUid())
                        == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        if (!isCallingProcessForeground) {
            Rlog.d(LOG_TAG, "isSmsSimPickActivityNeeded: calling process not foreground. "
                    + "Suppressing activity.");
            return false;
        }
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        List<SubscriptionInfo> subInfoList;
        final long identity = Binder.clearCallingIdentity();
        try {
            subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }

        if (subInfoList != null) {
            final int subInfoLength = subInfoList.size();

            for (int i = 0; i < subInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                if (sir != null && sir.getSubscriptionId() == subId) {
                    // The subscription id is valid, sms sim pick activity not needed
                    return false;
                }
            }

            // If reached here and multiple SIMs and subs present, sms sim pick activity is needed
            if (subInfoLength > 1 && telephonyManager.getSimCount() > 1) {
                return true;
            }
        }

        return false;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public String getImsSmsFormatForSubscriber(int subId) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.getImsSmsFormat();
        } else {
            Rlog.e(LOG_TAG, "getImsSmsFormatForSubscriber iccSmsIntMgr is null");
        }
        return null;
    }

    @Override
    public void injectSmsPduForSubscriber(
            int subId, byte[] pdu, String format, PendingIntent receivedIntent) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.injectSmsPdu(pdu, format, receivedIntent);
        } else {
            Rlog.e(LOG_TAG, "injectSmsPduForSubscriber iccSmsIntMgr is null");
            // RESULT_SMS_GENERIC_ERROR is documented for injectSmsPdu
            sendErrorInPendingIntent(receivedIntent, Intents.RESULT_SMS_GENERIC_ERROR);
        }
    }

    /**
     * Get preferred SMS subscription.
     *
     * @return User-defined default SMS subscription. If there is no default, return the active
     * subscription if there is only one active. If no preference can be found, return
     * {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID}.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public int getPreferredSmsSubscription() {
        // If there is a default, choose that one.
        int defaultSubId = SubscriptionController.getInstance().getDefaultSmsSubId();
        if (SubscriptionManager.isValidSubscriptionId(defaultSubId)) {
            return defaultSubId;
        }
        // No default, if there is only one sub active, choose that as the "preferred" sub id.
        long token = Binder.clearCallingIdentity();
        try {
            int[] activeSubs = SubscriptionController.getInstance()
                    .getActiveSubIdList(true /*visibleOnly*/);
            if (activeSubs.length == 1) {
                return activeSubs[0];
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        // No preference can be found.
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    /**
     * Get SMS prompt property enabled or not
     *
     * @return True if SMS prompt is enabled.
     */
    @Override
    public boolean isSMSPromptEnabled() {
        return PhoneFactory.isSMSPromptEnabled();
    }

    @Override
    public void sendStoredText(int subId, String callingPkg, String callingAttributionTag,
            Uri messageUri, String scAddress, PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (!getCallingPackage().equals(callingPkg)) {
            throw new SecurityException("sendStoredText: Package " + callingPkg
                    + "does not belong to " + Binder.getCallingUid());
        }
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendStoredText(callingPkg, callingAttributionTag, messageUri, scAddress,
                    sentIntent, deliveryIntent);
        } else {
            Rlog.e(LOG_TAG, "sendStoredText iccSmsIntMgr is null for subscription: " + subId);
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    @Override
    public void sendStoredMultipartText(int subId, String callingPkg, String callingAttributionTag,
            Uri messageUri, String scAddress, List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (!getCallingPackage().equals(callingPkg)) {
            throw new SecurityException("sendStoredMultipartText: Package " + callingPkg
                    + " does not belong to " + Binder.getCallingUid());
        }
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.sendStoredMultipartText(callingPkg, callingAttributionTag, messageUri,
                    scAddress, sentIntents, deliveryIntents);
        } else {
            Rlog.e(LOG_TAG, "sendStoredMultipartText iccSmsIntMgr is null for subscription: "
                    + subId);
            sendErrorInPendingIntents(sentIntents, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    @Override
    public Bundle getCarrierConfigValuesForSubscriber(int subId) {
        final long identity = Binder.clearCallingIdentity();
        try {
            final CarrierConfigManager configManager =
                    (CarrierConfigManager)
                            mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            return getMmsConfig(configManager.getConfigForSubId(subId));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * Filters a bundle to only contain MMS config variables.
     *
     * This is for use with bundles returned by CarrierConfigManager which contain MMS config and
     * unrelated config. It is assumed that all MMS_CONFIG_* keys are present in the supplied
     * bundle.
     *
     * @param config a Bundle that contains MMS config variables and possibly more.
     * @return a new Bundle that only contains the MMS_CONFIG_* keys defined in SmsManager.
     */
    private static Bundle getMmsConfig(BaseBundle config) {
        Bundle filtered = new Bundle();
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_APPEND_TRANSACTION_ID,
                config.getBoolean(SmsManager.MMS_CONFIG_APPEND_TRANSACTION_ID));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_MMS_ENABLED,
                config.getBoolean(SmsManager.MMS_CONFIG_MMS_ENABLED));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_GROUP_MMS_ENABLED,
                config.getBoolean(SmsManager.MMS_CONFIG_GROUP_MMS_ENABLED));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_NOTIFY_WAP_MMSC_ENABLED,
                config.getBoolean(SmsManager.MMS_CONFIG_NOTIFY_WAP_MMSC_ENABLED));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_ALIAS_ENABLED,
                config.getBoolean(SmsManager.MMS_CONFIG_ALIAS_ENABLED));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_ALLOW_ATTACH_AUDIO,
                config.getBoolean(SmsManager.MMS_CONFIG_ALLOW_ATTACH_AUDIO));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_MULTIPART_SMS_ENABLED,
                config.getBoolean(SmsManager.MMS_CONFIG_MULTIPART_SMS_ENABLED));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_SMS_DELIVERY_REPORT_ENABLED,
                config.getBoolean(SmsManager.MMS_CONFIG_SMS_DELIVERY_REPORT_ENABLED));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION,
                config.getBoolean(SmsManager.MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES,
                config.getBoolean(SmsManager.MMS_CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_MMS_READ_REPORT_ENABLED,
                config.getBoolean(SmsManager.MMS_CONFIG_MMS_READ_REPORT_ENABLED));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED,
                config.getBoolean(SmsManager.MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_CLOSE_CONNECTION,
                config.getBoolean(SmsManager.MMS_CONFIG_CLOSE_CONNECTION));
        filtered.putInt(
                SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE,
                config.getInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE));
        filtered.putInt(
                SmsManager.MMS_CONFIG_MAX_IMAGE_WIDTH,
                config.getInt(SmsManager.MMS_CONFIG_MAX_IMAGE_WIDTH));
        filtered.putInt(
                SmsManager.MMS_CONFIG_MAX_IMAGE_HEIGHT,
                config.getInt(SmsManager.MMS_CONFIG_MAX_IMAGE_HEIGHT));
        filtered.putInt(
                SmsManager.MMS_CONFIG_RECIPIENT_LIMIT,
                config.getInt(SmsManager.MMS_CONFIG_RECIPIENT_LIMIT));
        filtered.putInt(
                SmsManager.MMS_CONFIG_ALIAS_MIN_CHARS,
                config.getInt(SmsManager.MMS_CONFIG_ALIAS_MIN_CHARS));
        filtered.putInt(
                SmsManager.MMS_CONFIG_ALIAS_MAX_CHARS,
                config.getInt(SmsManager.MMS_CONFIG_ALIAS_MAX_CHARS));
        filtered.putInt(
                SmsManager.MMS_CONFIG_SMS_TO_MMS_TEXT_THRESHOLD,
                config.getInt(SmsManager.MMS_CONFIG_SMS_TO_MMS_TEXT_THRESHOLD));
        filtered.putInt(
                SmsManager.MMS_CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD,
                config.getInt(SmsManager.MMS_CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD));
        filtered.putInt(
                SmsManager.MMS_CONFIG_MESSAGE_TEXT_MAX_SIZE,
                config.getInt(SmsManager.MMS_CONFIG_MESSAGE_TEXT_MAX_SIZE));
        filtered.putInt(
                SmsManager.MMS_CONFIG_SUBJECT_MAX_LENGTH,
                config.getInt(SmsManager.MMS_CONFIG_SUBJECT_MAX_LENGTH));
        filtered.putInt(
                SmsManager.MMS_CONFIG_HTTP_SOCKET_TIMEOUT,
                config.getInt(SmsManager.MMS_CONFIG_HTTP_SOCKET_TIMEOUT));
        filtered.putString(
                SmsManager.MMS_CONFIG_UA_PROF_TAG_NAME,
                config.getString(SmsManager.MMS_CONFIG_UA_PROF_TAG_NAME));
        filtered.putString(
                SmsManager.MMS_CONFIG_USER_AGENT,
                config.getString(SmsManager.MMS_CONFIG_USER_AGENT));
        filtered.putString(
                SmsManager.MMS_CONFIG_UA_PROF_URL,
                config.getString(SmsManager.MMS_CONFIG_UA_PROF_URL));
        filtered.putString(
                SmsManager.MMS_CONFIG_HTTP_PARAMS,
                config.getString(SmsManager.MMS_CONFIG_HTTP_PARAMS));
        filtered.putString(
                SmsManager.MMS_CONFIG_EMAIL_GATEWAY_NUMBER,
                config.getString(SmsManager.MMS_CONFIG_EMAIL_GATEWAY_NUMBER));
        filtered.putString(
                SmsManager.MMS_CONFIG_NAI_SUFFIX,
                config.getString(SmsManager.MMS_CONFIG_NAI_SUFFIX));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_SHOW_CELL_BROADCAST_APP_LINKS,
                config.getBoolean(SmsManager.MMS_CONFIG_SHOW_CELL_BROADCAST_APP_LINKS));
        filtered.putBoolean(
                SmsManager.MMS_CONFIG_SUPPORT_HTTP_CHARSET_HEADER,
                config.getBoolean(SmsManager.MMS_CONFIG_SUPPORT_HTTP_CHARSET_HEADER));
        return filtered;
    }

    @Override
    public String createAppSpecificSmsTokenWithPackageInfo(
            int subId, String callingPkg, String prefixes, PendingIntent intent) {
        if (callingPkg == null) {
            callingPkg = getCallingPackage();
        }
        return getPhone(subId).getAppSmsManager().createAppSpecificSmsTokenWithPackageInfo(
                subId, callingPkg, prefixes, intent);
    }

    @Override
    public String createAppSpecificSmsToken(int subId, String callingPkg, PendingIntent intent) {
        if (callingPkg == null) {
            callingPkg = getCallingPackage();
        }
        return getPhone(subId).getAppSmsManager().createAppSpecificSmsToken(callingPkg, intent);
    }

    @Override
    public int checkSmsShortCodeDestination(int subId, String callingPackage,
            String callingFeatureId, String destAddress, String countryIso) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(getPhone(subId).getContext(),
                subId, callingPackage, callingFeatureId, "checkSmsShortCodeDestination")) {
            return SmsManager.SMS_CATEGORY_NOT_SHORT_CODE;
        }
        final long identity = Binder.clearCallingIdentity();
        try {
            return getPhone(subId).mSmsUsageMonitor.checkDestination(destAddress, countryIso);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * Internal API to send visual voicemail related SMS. This is not exposed outside the phone
     * process, and should be called only after verifying that the caller is the default VVM app.
     */
    public void sendVisualVoicemailSmsForSubscriber(String callingPackage,
            String callingAttributionTag, int subId, String number, int port, String text,
            PendingIntent sentIntent) {
        if (port == 0) {
            sendTextForSubscriberWithSelfPermissionsInternal(subId, callingPackage,
                    callingAttributionTag, number, null, text, sentIntent, null, false,
                    true /* isForVvm */);
        } else {
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            sendDataForSubscriberWithSelfPermissionsInternal(subId, callingPackage,
                    callingAttributionTag, number, null, (short) port, data, sentIntent, null,
                    true /* isForVvm */);
        }
    }

    @Override
    public String getSmscAddressFromIccEfForSubscriber(int subId, String callingPackage) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.getSmscAddressFromIccEf(callingPackage);
        } else {
            Rlog.e(LOG_TAG, "getSmscAddressFromIccEfForSubscriber iccSmsIntMgr is null"
                    + " for Subscription: " + subId);
            return null;
        }
    }

    @Override
    public boolean setSmscAddressOnIccEfForSubscriber(
            String smsc, int subId, String callingPackage) {
        if (callingPackage == null) {
            callingPackage = getCallingPackage();
        }
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.setSmscAddressOnIccEf(callingPackage, smsc);
        } else {
            Rlog.e(LOG_TAG, "setSmscAddressOnIccEfForSubscriber iccSmsIntMgr is null"
                    + " for Subscription: " + subId);
            return false;
        }
    }

    /**
     * Triggered by `adb shell dumpsys isms`
     */
    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!checkDumpPermission(mContext, LOG_TAG, pw)) {
            return;
        }

        IndentingPrintWriter indentingPW =
                new IndentingPrintWriter(pw, "    " /* singleIndent */);
        for (Phone phone : PhoneFactory.getPhones()) {
            int subId = phone.getSubId();
            indentingPW.println(String.format("SmsManager for subId = %d:", subId));
            indentingPW.increaseIndent();
            if (getIccSmsInterfaceManager(subId) != null) {
                getIccSmsInterfaceManager(subId).dump(fd, indentingPW, args);
            }
            indentingPW.decreaseIndent();
        }
        indentingPW.flush();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void sendErrorInPendingIntent(@Nullable PendingIntent intent, int errorCode) {
        if (intent != null) {
            try {
                intent.send(errorCode);
            } catch (PendingIntent.CanceledException ex) {
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void sendErrorInPendingIntents(List<PendingIntent> intents, int errorCode) {
        if (intents == null) {
            return;
        }

        for (PendingIntent intent : intents) {
            sendErrorInPendingIntent(intent, errorCode);
        }
    }

    /**
     * Get sms interface manager object based on subscription.
     *
     * @return ICC SMS manager
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private @Nullable IccSmsInterfaceManager getIccSmsInterfaceManager(int subId) {
        return getPhone(subId).getIccSmsInterfaceManager();
    }

    private SubscriptionInfo getSubscriptionInfo(int subId) {
        SubscriptionManager manager = (SubscriptionManager) mContext
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        return manager.getActiveSubscriptionInfo(subId);
    }

    /**
     * Get the capacity count of sms on Icc card.
     */
    @Override
    public int getSmsCapacityOnIccForSubscriber(int subId) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);

        if (iccSmsIntMgr != null ) {
            return iccSmsIntMgr.getSmsCapacityOnIcc(getCallingPackage(), null);
        } else {
            Rlog.e(LOG_TAG, "iccSmsIntMgr is null for " + " subId: " + subId);
            return 0;
        }
    }

    /**
     * Reset all cell broadcast ranges. Previously enabled ranges will become invalid after this.
     *
     * @param subId Subscription index
     * @return {@code true} if succeeded, otherwise {@code false}.
     */
    @Override
    public boolean resetAllCellBroadcastRanges(int subId) {
        IccSmsInterfaceManager iccSmsIntMgr = getIccSmsInterfaceManager(subId);
        if (iccSmsIntMgr != null) {
            iccSmsIntMgr.resetAllCellBroadcastRanges();
            return true;
        } else {
            Rlog.e(LOG_TAG, "iccSmsIntMgr is null for " + " subId: " + subId);
            return false;
        }
    }

    /**
     * Internal API to consistently format the debug log output of the cross-stack message id.
     */
    public static String formatCrossStackMessageId(long id) {
        return "{x-message-id:" + id + "}";
    }
}
