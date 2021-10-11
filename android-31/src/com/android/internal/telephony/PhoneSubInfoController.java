/*
 * Copyright (C) 2006 The Android Open Source Project
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

import static android.Manifest.permission.MODIFY_PHONE_STATE;
import static android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.os.TelephonyServiceManager.ServiceRegisterer;
import android.os.UserHandle;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyFrameworkInitializer;

import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.telephony.Rlog;

public class PhoneSubInfoController extends IPhoneSubInfo.Stub {
    private static final String TAG = "PhoneSubInfoController";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; // STOPSHIP if true

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final Context mContext;

    public PhoneSubInfoController(Context context) {
        ServiceRegisterer phoneSubServiceRegisterer = TelephonyFrameworkInitializer
                .getTelephonyServiceManager()
                .getPhoneSubServiceRegisterer();
        if (phoneSubServiceRegisterer.get() == null) {
            phoneSubServiceRegisterer.register(this);
        }
        mContext = context;
    }

    @Deprecated
    public String getDeviceId(String callingPackage) {
        return getDeviceIdWithFeature(callingPackage, null);
    }

    public String getDeviceIdWithFeature(String callingPackage, String callingFeatureId) {
        return getDeviceIdForPhone(SubscriptionManager.getPhoneId(getDefaultSubscription()),
                callingPackage, callingFeatureId);
    }

    public String getDeviceIdForPhone(int phoneId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForPhoneIdWithReadDeviceIdentifiersCheck(phoneId, callingPackage,
                callingFeatureId, "getDeviceId", (phone) -> phone.getDeviceId());
    }

    public String getNaiForSubscriber(int subId, String callingPackage, String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadSubscriberIdentifiersCheck(subId, callingPackage,
                callingFeatureId, "getNai", (phone)-> phone.getNai());
    }

    public String getImeiForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadDeviceIdentifiersCheck(subId, callingPackage,
                callingFeatureId, "getImei", (phone) -> phone.getImei());
    }

    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int subId, int keyType,
                                                              String callingPackage) {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId,
                "getCarrierInfoForImsiEncryption",
                (phone)-> phone.getCarrierInfoForImsiEncryption(keyType, true));
    }

    public void setCarrierInfoForImsiEncryption(int subId, String callingPackage,
                                                ImsiEncryptionInfo imsiEncryptionInfo) {
        callPhoneMethodForSubIdWithModifyCheck(subId, callingPackage,
                "setCarrierInfoForImsiEncryption",
                (phone)-> {
                    phone.setCarrierInfoForImsiEncryption(imsiEncryptionInfo);
                    return null;
                });
    }

    /**
     *  Resets the Carrier Keys in the database. This involves 2 steps:
     *  1. Delete the keys from the database.
     *  2. Send an intent to download new Certificates.
     *  @param subId
     *  @param callingPackage
     */
    public void resetCarrierKeysForImsiEncryption(int subId, String callingPackage) {
        callPhoneMethodForSubIdWithModifyCheck(subId, callingPackage,
                "resetCarrierKeysForImsiEncryption",
                (phone)-> {
                    phone.resetCarrierKeysForImsiEncryption();
                    return null;
                });
    }

    public String getDeviceSvn(String callingPackage, String callingFeatureId) {
        return getDeviceSvnUsingSubId(getDefaultSubscription(), callingPackage, callingFeatureId);
    }

    public String getDeviceSvnUsingSubId(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadCheck(subId, callingPackage, callingFeatureId,
                "getDeviceSvn", (phone)-> phone.getDeviceSvn());
    }

    @Deprecated
    public String getSubscriberId(String callingPackage) {
        return getSubscriberIdWithFeature(callingPackage, null);
    }

    public String getSubscriberIdWithFeature(String callingPackage, String callingFeatureId) {
        return getSubscriberIdForSubscriber(getDefaultSubscription(), callingPackage,
                callingFeatureId);
    }

    public String getSubscriberIdForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        String message = "getSubscriberIdForSubscriber";

        enforceCallingPackage(callingPackage, Binder.getCallingUid(), message);

        long identity = Binder.clearCallingIdentity();
        boolean isActive;
        try {
            isActive = SubscriptionController.getInstance().isActiveSubId(subId, callingPackage,
                    callingFeatureId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        if (isActive) {
            return callPhoneMethodForSubIdWithReadSubscriberIdentifiersCheck(subId, callingPackage,
                    callingFeatureId, message, (phone) -> phone.getSubscriberId());
        } else {
            if (!TelephonyPermissions.checkCallingOrSelfReadSubscriberIdentifiers(
                    mContext, subId, callingPackage, callingFeatureId, message)) {
                return null;
            }
            identity = Binder.clearCallingIdentity();
            try {
                return SubscriptionController.getInstance().getImsiPrivileged(subId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    @Deprecated
    public String getIccSerialNumber(String callingPackage) {
        return getIccSerialNumberWithFeature(callingPackage, null);
    }

    /**
     * Retrieves the serial number of the ICC, if applicable.
     */
    public String getIccSerialNumberWithFeature(String callingPackage, String callingFeatureId) {
        return getIccSerialNumberForSubscriber(getDefaultSubscription(), callingPackage,
                callingFeatureId);
    }

    public String getIccSerialNumberForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadSubscriberIdentifiersCheck(subId, callingPackage,
                callingFeatureId, "getIccSerialNumber", (phone) -> phone.getIccSerialNumber());
    }

    public String getLine1Number(String callingPackage, String callingFeatureId) {
        return getLine1NumberForSubscriber(getDefaultSubscription(), callingPackage,
                callingFeatureId);
    }

    // In R and beyond, READ_PHONE_NUMBERS includes READ_PHONE_NUMBERS and READ_SMS only.
    // Prior to R, it also included READ_PHONE_STATE.  Maintain that for compatibility.
    public String getLine1NumberForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadPhoneNumberCheck(
                subId, callingPackage, callingFeatureId, "getLine1Number",
                (phone)-> phone.getLine1Number());
    }

    public String getLine1AlphaTag(String callingPackage, String callingFeatureId) {
        return getLine1AlphaTagForSubscriber(getDefaultSubscription(), callingPackage,
                callingFeatureId);
    }

    public String getLine1AlphaTagForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadCheck(subId, callingPackage, callingFeatureId,
                "getLine1AlphaTag", (phone)-> phone.getLine1AlphaTag());
    }

    public String getMsisdn(String callingPackage, String callingFeatureId) {
        return getMsisdnForSubscriber(getDefaultSubscription(), callingPackage, callingFeatureId);
    }

    // In R and beyond this will require READ_PHONE_NUMBERS.
    // Prior to R it needed READ_PHONE_STATE.  Maintain that for compatibility.
    public String getMsisdnForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadPhoneNumberCheck(
                subId, callingPackage, callingFeatureId, "getMsisdn", (phone)-> phone.getMsisdn());
    }

    public String getVoiceMailNumber(String callingPackage, String callingFeatureId) {
        return getVoiceMailNumberForSubscriber(getDefaultSubscription(), callingPackage,
                callingFeatureId);
    }

    public String getVoiceMailNumberForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadCheck(subId, callingPackage, callingFeatureId,
                "getVoiceMailNumber", (phone)-> {
                    String number = PhoneNumberUtils.extractNetworkPortion(
                            phone.getVoiceMailNumber());
                    if (VDBG) log("VM: getVoiceMailNUmber: " + number);
                    return number;
                });
    }

    public String getVoiceMailAlphaTag(String callingPackage, String callingFeatureId) {
        return getVoiceMailAlphaTagForSubscriber(getDefaultSubscription(), callingPackage,
                callingFeatureId);
    }

    public String getVoiceMailAlphaTagForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadCheck(subId, callingPackage, callingFeatureId,
                "getVoiceMailAlphaTag", (phone)-> phone.getVoiceMailAlphaTag());
    }

    /**
     * get Phone object based on subId.
     **/
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private Phone getPhone(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return null;
        }
        return PhoneFactory.getPhone(phoneId);
    }

    private boolean enforceIccSimChallengeResponsePermission(Context context, int subId,
            String callingPackage, String callingFeatureId, String message) {
        if (TelephonyPermissions.checkCallingOrSelfUseIccAuthWithDeviceIdentifier(context,
                callingPackage, callingFeatureId, message)) {
            return true;
        }
        if (VDBG) log("No USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER permission.");
        enforcePrivilegedPermissionOrCarrierPrivilege(subId, message);
        return true;
    }

    /**
     * Make sure caller has either read privileged phone permission or carrier privilege.
     *
     * @throws SecurityException if the caller does not have the required permission/privilege
     */
    private void enforcePrivilegedPermissionOrCarrierPrivilege(int subId, String message) {
        // TODO(b/73660190): Migrate to
        // TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivileges and delete
        // this helper method.
        int permissionResult = mContext.checkCallingOrSelfPermission(
                READ_PRIVILEGED_PHONE_STATE);
        if (permissionResult == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (VDBG) log("No read privileged phone permission, check carrier privilege next.");
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(mContext, subId, message);
    }

    /**
     * Make sure caller has modify phone state permission.
     */
    private void enforceModifyPermission() {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE,
                "Requires MODIFY_PHONE_STATE");
    }

    /**
     * Make sure the caller is the calling package itself
     *
     * @throws SecurityException if the caller is not the calling package
     */
    private void enforceCallingPackage(String callingPackage, int callingUid, String message) {
        int packageUid = -1;
        PackageManager pm = mContext.createContextAsUser(
                UserHandle.getUserHandleForUid(callingUid), 0).getPackageManager();
        if (pm != null) {
            try {
                packageUid = pm.getPackageUid(callingPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // packageUid is -1
            }
        }
        if (packageUid != callingUid) {
            throw new SecurityException(message + ": Package " + callingPackage
                    + " does not belong to " + callingUid);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int getDefaultSubscription() {
        return  PhoneFactory.getDefaultSubscription();
    }

    /**
    * get the Isim Impi based on subId
    */
    public String getIsimImpi(int subId) {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getIsimImpi",
                (phone) -> {
                    IsimRecords isim = phone.getIsimRecords();
                    if (isim != null) {
                        return isim.getIsimImpi();
                    } else {
                        return null;
                    }
                });
    }

    /**
    * get the Isim Domain based on subId
    */
    public String getIsimDomain(int subId) {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getIsimDomain",
                (phone) -> {
                    IsimRecords isim = phone.getIsimRecords();
                    if (isim != null) {
                        return isim.getIsimDomain();
                    } else {
                        return null;
                    }
                });
    }

    /**
    * get the Isim Impu based on subId
    */
    public String[] getIsimImpu(int subId) {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getIsimImpu",
                (phone) -> {
                    IsimRecords isim = phone.getIsimRecords();
                    if (isim != null) {
                        return isim.getIsimImpu();
                    } else {
                        return null;
                    }
                });
    }

    /**
    * get the Isim Ist based on subId
    */
    public String getIsimIst(int subId) throws RemoteException {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getIsimIst",
                (phone) -> {
                    IsimRecords isim = phone.getIsimRecords();
                    if (isim != null) {
                        return isim.getIsimIst();
                    } else {
                        return null;
                    }
                });
    }

    /**
    * get the Isim Pcscf based on subId
    */
    public String[] getIsimPcscf(int subId) throws RemoteException {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getIsimPcscf",
                (phone) -> {
                    IsimRecords isim = phone.getIsimRecords();
                    if (isim != null) {
                        return isim.getIsimPcscf();
                    } else {
                        return null;
                    }
                });
    }

    @Override
    public String getIccSimChallengeResponse(int subId, int appType, int authType, String data,
            String callingPackage, String callingFeatureId) throws RemoteException {
        CallPhoneMethodHelper<String> toExecute = (phone)-> {
            UiccCard uiccCard = phone.getUiccCard();
            if (uiccCard == null) {
                loge("getIccSimChallengeResponse() UiccCard is null");
                return null;
            }

            UiccCardApplication uiccApp = uiccCard.getApplicationByType(appType);
            if (uiccApp == null) {
                loge("getIccSimChallengeResponse() no app with specified type -- " + appType);
                return null;
            } else {
                loge("getIccSimChallengeResponse() found app " + uiccApp.getAid()
                        + " specified type -- " + appType);
            }

            if (authType != UiccCardApplication.AUTH_CONTEXT_EAP_SIM
                    && authType != UiccCardApplication.AUTH_CONTEXT_EAP_AKA) {
                loge("getIccSimChallengeResponse() unsupported authType: " + authType);
                return null;
            }
            return uiccApp.getIccRecords().getIccSimChallengeResponse(authType, data);
        };

        return callPhoneMethodWithPermissionCheck(subId, callingPackage, callingFeatureId,
                "getIccSimChallengeResponse", toExecute,
                this::enforceIccSimChallengeResponsePermission);
    }

    public String getGroupIdLevel1ForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadCheck(subId, callingPackage, callingFeatureId,
                "getGroupIdLevel1", (phone)-> phone.getGroupIdLevel1());
    }

    /** Below are utility methods that abstracts the flow that many public methods use:
     *  1. Check permission: pass, throw exception, or fails (returns false).
     *  2. clearCallingIdentity.
     *  3. Call a specified phone method and get return value.
     *  4. restoreCallingIdentity and return.
     */
    private interface CallPhoneMethodHelper<T> {
        T callMethod(Phone phone);
    }

    private interface PermissionCheckHelper {
        // Implemented to do whatever permission check it wants.
        // If passes, it should return true.
        // If permission is not granted, throws SecurityException.
        // If permission is revoked by AppOps, return false.
        boolean checkPermission(Context context, int subId, String callingPackage,
                @Nullable String callingFeatureId, String message);
    }

    // Base utility method that others use.
    private <T> T callPhoneMethodWithPermissionCheck(int subId, String callingPackage,
            @Nullable String callingFeatureId, String message,
            CallPhoneMethodHelper<T> callMethodHelper,
            PermissionCheckHelper permissionCheckHelper) {
        if (!permissionCheckHelper.checkPermission(mContext, subId, callingPackage,
                callingFeatureId, message)) {
            return null;
        }

        final long identity = Binder.clearCallingIdentity();
        try {
            Phone phone = getPhone(subId);
            if (phone != null) {
                return callMethodHelper.callMethod(phone);
            } else {
                loge(message + " phone is null for Subscription:" + subId);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private <T> T callPhoneMethodForSubIdWithReadCheck(int subId, String callingPackage,
            @Nullable String callingFeatureId, String message,
            CallPhoneMethodHelper<T> callMethodHelper) {
        return callPhoneMethodWithPermissionCheck(subId, callingPackage, callingFeatureId,
                message, callMethodHelper,
                (aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage)->
                        TelephonyPermissions.checkCallingOrSelfReadPhoneState(
                                aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage));
    }

    private <T> T callPhoneMethodForSubIdWithReadDeviceIdentifiersCheck(int subId,
            String callingPackage, @Nullable String callingFeatureId, String message,
            CallPhoneMethodHelper<T> callMethodHelper) {
        return callPhoneMethodWithPermissionCheck(subId, callingPackage, callingFeatureId,
                message, callMethodHelper,
                (aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage)->
                        TelephonyPermissions.checkCallingOrSelfReadDeviceIdentifiers(
                                aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage));
    }

    private <T> T callPhoneMethodForSubIdWithReadSubscriberIdentifiersCheck(int subId,
            String callingPackage, @Nullable String callingFeatureId, String message,
            CallPhoneMethodHelper<T> callMethodHelper) {
        return callPhoneMethodWithPermissionCheck(subId, callingPackage, callingFeatureId,
                message, callMethodHelper,
                (aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage)->
                        TelephonyPermissions.checkCallingOrSelfReadSubscriberIdentifiers(
                                aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage));
    }

    private <T> T callPhoneMethodForSubIdWithPrivilegedCheck(
            int subId, String message, CallPhoneMethodHelper<T> callMethodHelper) {
        return callPhoneMethodWithPermissionCheck(subId, null, null, message, callMethodHelper,
                (aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage) -> {
                    mContext.enforceCallingOrSelfPermission(READ_PRIVILEGED_PHONE_STATE, message);
                    return true;
                });
    }

    private <T> T callPhoneMethodForSubIdWithModifyCheck(int subId, String callingPackage,
            String message, CallPhoneMethodHelper<T> callMethodHelper) {
        return callPhoneMethodWithPermissionCheck(subId, null, null, message, callMethodHelper,
                (aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage)-> {
                    enforceModifyPermission();
                    return true;
                });
    }

    private <T> T callPhoneMethodForSubIdWithReadPhoneNumberCheck(int subId, String callingPackage,
            @NonNull String callingFeatureId, String message,
            CallPhoneMethodHelper<T> callMethodHelper) {
        return callPhoneMethodWithPermissionCheck(subId, callingPackage, callingFeatureId,
                message, callMethodHelper,
                (aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage) ->
                        TelephonyPermissions.checkCallingOrSelfReadPhoneNumber(
                                aContext, aSubId, aCallingPackage, aCallingFeatureId, aMessage));
    }

    private <T> T callPhoneMethodForPhoneIdWithReadDeviceIdentifiersCheck(int phoneId,
            String callingPackage, @Nullable String callingFeatureId, String message,
            CallPhoneMethodHelper<T> callMethodHelper) {
        // Getting subId before doing permission check.
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            phoneId = 0;
        }
        final Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null) {
            return null;
        }
        if (!TelephonyPermissions.checkCallingOrSelfReadDeviceIdentifiers(mContext,
                phone.getSubId(), callingPackage, callingFeatureId, message)) {
            return null;
        }

        final long identity = Binder.clearCallingIdentity();
        try {
            return callMethodHelper.callMethod(phone);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void loge(String s) {
        Rlog.e(TAG, s);
    }
}
