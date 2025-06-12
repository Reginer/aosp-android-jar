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
import static android.telephony.TelephonyManager.ENABLE_FEATURE_MAPPING;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.app.compat.CompatChanges;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.TelephonyServiceManager.ServiceRegisterer;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyFrameworkInitializer;
import android.text.TextUtils;
import android.util.EventLog;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.flags.FeatureFlagsImpl;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PhoneSubInfoController extends IPhoneSubInfo.Stub {
    private static final String TAG = "PhoneSubInfoController";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; // STOPSHIP if true

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final Context mContext;
    private AppOpsManager mAppOps;
    private FeatureFlags mFeatureFlags;
    private PackageManager mPackageManager;
    private final int mVendorApiLevel;

    public PhoneSubInfoController(Context context) {
        this(context, new FeatureFlagsImpl());
    }

    public PhoneSubInfoController(Context context, FeatureFlags featureFlags) {
        ServiceRegisterer phoneSubServiceRegisterer = TelephonyFrameworkInitializer
                .getTelephonyServiceManager()
                .getPhoneSubServiceRegisterer();
        if (phoneSubServiceRegisterer.get() == null) {
            phoneSubServiceRegisterer.register(this);
        }
        mAppOps = context.getSystemService(AppOpsManager.class);
        mContext = context;
        mPackageManager = context.getPackageManager();
        mFeatureFlags = featureFlags;
        mVendorApiLevel = SystemProperties.getInt(
                "ro.vendor.api_level", Build.VERSION.DEVICE_INITIAL_SDK_INT);
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
        enforceCallingPackageUidMatched(callingPackage);
        return callPhoneMethodForPhoneIdWithReadDeviceIdentifiersCheck(phoneId, callingPackage,
                callingFeatureId, "getDeviceId", (phone) -> phone.getDeviceId());
    }

    public String getNaiForSubscriber(int subId, String callingPackage, String callingFeatureId) {
        return callPhoneMethodForSubIdWithReadSubscriberIdentifiersCheck(subId, callingPackage,
                callingFeatureId, "getNai", (phone)-> {
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION,
                            "getNaiForSubscriber");

                    return phone.getNai();
                });
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
                (phone)-> {
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION,
                            "getCarrierInfoForImsiEncryption");

                    return phone.getCarrierInfoForImsiEncryption(keyType, true);
                });
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
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION,
                            "resetCarrierKeysForImsiEncryption");

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
        mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);

        long identity = Binder.clearCallingIdentity();
        boolean isActive;
        try {
            isActive = SubscriptionManagerService.getInstance().isActiveSubId(subId,
                    callingPackage, callingFeatureId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        if (isActive) {
            return callPhoneMethodForSubIdWithReadSubscriberIdentifiersCheck(subId, callingPackage,
                    callingFeatureId, message, (phone) -> {
                        enforceTelephonyFeatureWithException(callingPackage,
                                PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION,
                                "getSubscriberIdForSubscriber");

                        return phone.getSubscriberId();
                    });
        } else {
            if (!TelephonyPermissions.checkCallingOrSelfReadSubscriberIdentifiers(
                    mContext, subId, callingPackage, callingFeatureId, message)) {
                return null;
            }

            enforceTelephonyFeatureWithException(callingPackage,
                    PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION, "getSubscriberIdForSubscriber");

            identity = Binder.clearCallingIdentity();
            try {
                SubscriptionInfoInternal subInfo = SubscriptionManagerService.getInstance()
                        .getSubscriptionInfoInternal(subId);
                if (subInfo != null && !TextUtils.isEmpty(subInfo.getImsi())) {
                    return subInfo.getImsi();
                }
                return null;
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
                callingFeatureId, "getIccSerialNumber", (phone) -> {
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION,
                            "getIccSerialNumberForSubscriber");

                    return phone.getIccSerialNumber();
                });
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
                (phone)-> {
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION,
                            "getLine1NumberForSubscriber");

                    return phone.getLine1Number();
                });
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
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_CALLING,
                            "getVoiceMailNumberForSubscriber");

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
                "getVoiceMailAlphaTag", (phone)-> {
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_CALLING,
                            "getVoiceMailAlphaTagForSubscriber");

                    return phone.getVoiceMailAlphaTag();
                });
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

    private void enforceCallingPackageUidMatched(String callingPackage) {
        try {
            mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
        } catch (SecurityException se) {
            EventLog.writeEvent(0x534e4554, "188677422", Binder.getCallingUid());
            throw se;
        }
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
     * Fetches the IMS private user identity (EF_IMPI) based on subscriptionId.
     *
     * @param subId subscriptionId
     * @return IMPI (IMS private user identity) of type string.
     * @throws IllegalArgumentException if the subscriptionId is not valid
     * @throws IllegalStateException in case the ISIM hasn’t been loaded.
     * @throws SecurityException if the caller does not have the required permission
     */
    public String getImsPrivateUserIdentity(int subId, String callingPackage,
            String callingFeatureId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            throw new IllegalArgumentException("Invalid SubscriptionID  = " + subId);
        }
        if (!TelephonyPermissions.checkCallingOrSelfUseIccAuthWithDeviceIdentifier(mContext,
                callingPackage, callingFeatureId, "getImsPrivateUserIdentity")) {
            throw (new SecurityException("No permissions to the caller"));
        }
        Phone phone = getPhone(subId);
        assert phone != null;
        IsimRecords isim = phone.getIsimRecords();
        if (isim != null) {
            return isim.getIsimImpi();
        } else {
            throw new IllegalStateException("ISIM is not loaded");
        }
    }

    /**
    * get the Isim Domain based on subId
    */
    public String getIsimDomain(int subId) {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getIsimDomain",
                (phone) -> {
                    enforceTelephonyFeatureWithException(getCurrentPackageName(),
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION, "getIsimDomain");

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
     * Fetches the ISIM public user identities (EF_IMPU) from UICC based on subId
     *
     * @param subId subscriptionId
     * @param callingPackage package name of the caller
     * @return List of public user identities of type android.net.Uri or empty list  if
     * EF_IMPU is not available.
     * @throws IllegalArgumentException if the subscriptionId is not valid
     * @throws IllegalStateException in case the ISIM hasn’t been loaded.
     * @throws SecurityException if the caller does not have the required permission
     */
    public List<Uri> getImsPublicUserIdentities(int subId, String callingPackage) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            throw new IllegalArgumentException("Invalid subscription: " + subId);
        }

        TelephonyPermissions
                .enforceCallingOrSelfReadPrivilegedPhoneStatePermissionOrCarrierPrivilege(
                        mContext, subId, "getImsPublicUserIdentities");
        enforceTelephonyFeatureWithException(callingPackage,
                PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION, "getImsPublicUserIdentities");

        Phone phone = getPhone(subId);
        assert phone != null;
        IsimRecords isimRecords = phone.getIsimRecords();
        if (isimRecords != null) {
            String[] impus = isimRecords.getIsimImpu();
            List<Uri> impuList = new ArrayList<>();
            for (String impu : impus) {
                if (impu != null && impu.trim().length() > 0) {
                    impuList.add(Uri.parse(impu));
                }
            }
            return impuList;
        }
        throw new IllegalStateException("ISIM is not loaded");
    }

    /**
    * get the Isim Ist based on subId
    */
    public String getIsimIst(int subId) throws RemoteException {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getIsimIst",
                (phone) -> {
                    enforceTelephonyFeatureWithException(getCurrentPackageName(),
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION, "getIsimIst");

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

    /**
     * Fetches the IMS Proxy Call Session Control Function(P-CSCF) based on the subscription.
     *
     * @param subId subscriptionId
     * @param callingPackage package name of the caller
     * @return List of IMS Proxy Call Session Control Function strings.
     * @throws IllegalArgumentException if the subscriptionId is not valid
     * @throws IllegalStateException in case the ISIM hasn’t been loaded.
     * @throws SecurityException if the caller does not have the required permission
     */
    public List<String> getImsPcscfAddresses(int subId, String callingPackage) {
        if (!mFeatureFlags.supportIsimRecord()) {
            return new ArrayList<>();
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            throw new IllegalArgumentException("Invalid subscription: " + subId);
        }

        TelephonyPermissions
                .enforceCallingOrSelfReadPrivilegedPhoneStatePermissionOrCarrierPrivilege(
                        mContext, subId, "getImsPcscfAddresses");
        enforceTelephonyFeatureWithException(callingPackage,
                PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION, "getImsPcscfAddresses");

        Phone phone = getPhone(subId);
        assert phone != null;
        IsimRecords isimRecords = phone.getIsimRecords();
        if (isimRecords != null) {
            String[] pcscfs = isimRecords.getIsimPcscf();
            List<String> pcscfList = Arrays.stream(pcscfs)
                    .filter(u -> u != null)
                    .map(u -> u.trim())
                    .filter(u -> u.length() > 0)
                    .collect(Collectors.toList());
            return pcscfList;
        }
        throw new IllegalStateException("ISIM is not loaded");
    }

    /**
     * Returns the USIM service table that fetched from EFUST elementary field that are loaded
     * based on the appType.
     */
    public String getSimServiceTable(int subId, int appType) throws RemoteException {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getSimServiceTable",
                (phone) -> {
                    enforceTelephonyFeatureWithException(getCurrentPackageName(),
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION, "getSimServiceTable");

                    UiccPort uiccPort = phone.getUiccPort();
                    if (uiccPort == null || uiccPort.getUiccProfile() == null) {
                        loge("getSimServiceTable(): uiccPort or uiccProfile is null");
                        return null;
                    }
                    UiccCardApplication uiccApp = uiccPort.getUiccProfile().getApplicationByType(
                            appType);
                    if (uiccApp == null) {
                        loge("getSimServiceTable(): no app with specified apptype="
                                + appType);
                        return null;
                    }
                    return ((SIMRecords)uiccApp.getIccRecords()).getSimServiceTable();
                });
    }

    @Override
    public String getIccSimChallengeResponse(int subId, int appType, int authType, String data,
            String callingPackage, String callingFeatureId) throws RemoteException {
        CallPhoneMethodHelper<String> toExecute = (phone)-> {
            enforceTelephonyFeatureWithException(callingPackage,
                    PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION, "getIccSimChallengeResponse");

            UiccPort uiccPort = phone.getUiccPort();
            if (uiccPort == null) {
                loge("getIccSimChallengeResponse() uiccPort is null");
                return null;
            }

            UiccCardApplication uiccApp = uiccPort.getApplicationByType(appType);
            if (uiccApp == null) {
                loge("getIccSimChallengeResponse() no app with specified type -- " + appType);
                return null;
            } else {
                loge("getIccSimChallengeResponse() found app " + uiccApp.getAid()
                        + " specified type -- " + appType);
            }

            if (authType != UiccCardApplication.AUTH_CONTEXT_EAP_SIM
                    && authType != UiccCardApplication.AUTH_CONTEXT_EAP_AKA
                    && authType != UiccCardApplication.AUTH_CONTEXT_GBA_BOOTSTRAP
                    && authType != UiccCardApplication.AUTHTYPE_GBA_NAF_KEY_EXTERNAL) {
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
                "getGroupIdLevel1", (phone)-> {
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION,
                            "getGroupIdLevel1ForSubscriber");

                    return phone.getGroupIdLevel1();
                });
    }

    /**
     * Return GroupIdLevel2 for the subscriber
     */
    public String getGroupIdLevel2ForSubscriber(int subId, String callingPackage,
            String callingFeatureId) {
        return callPhoneMethodForSubIdWithPrivilegedCheck(subId,
                "getGroupIdLevel2", (phone)-> {
                    enforceTelephonyFeatureWithException(callingPackage,
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION,
                            "getGroupIdLevel2ForSubscriber");
                    return phone.getGroupIdLevel2();
                });
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
                if (VDBG) loge(message + " phone is null for Subscription:" + subId);
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

    /**
     * Returns SIP URI or tel URI of the Public Service Identity of the SM-SC fetched from
     * EF_PSISMSC elementary field as defined in Section 4.5.9 (3GPP TS 31.102).
     * @throws IllegalStateException in case if phone or UiccApplication is not available.
     */
    public Uri getSmscIdentity(int subId, int appType) throws RemoteException {
        Uri smscIdentityUri = callPhoneMethodForSubIdWithPrivilegedCheck(subId, "getSmscIdentity",
                (phone) -> {
                    enforceTelephonyFeatureWithException(getCurrentPackageName(),
                            PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION, "getSmscIdentity");

                    try {
                        String smscIdentity = null;
                        UiccPort uiccPort = phone.getUiccPort();
                        UiccCardApplication uiccApp =
                                uiccPort.getUiccProfile().getApplicationByType(
                                        appType);
                        smscIdentity = (uiccApp != null) ? uiccApp.getIccRecords().getSmscIdentity()
                                : null;
                        if (TextUtils.isEmpty(smscIdentity)) {
                            return Uri.EMPTY;
                        }
                        return Uri.parse(smscIdentity);
                    } catch (NullPointerException ex) {
                        Rlog.e(TAG, "getSmscIdentity(): Exception = " + ex);
                        return null;
                    }
                });
        if (smscIdentityUri == null) {
            throw new IllegalStateException("Telephony service error");
        }
        return smscIdentityUri;
    }

    /**
     * Get the current calling package name.
     * @return the current calling package name
     */
    @Nullable
    private String getCurrentPackageName() {
        if (mFeatureFlags.hsumPackageManager()) {
            PackageManager pm = mContext.createContextAsUser(Binder.getCallingUserHandle(), 0)
                    .getPackageManager();
            if (pm == null) return null;
            String[] callingPackageNames = pm.getPackagesForUid(Binder.getCallingUid());
            return (callingPackageNames == null) ? null : callingPackageNames[0];
        }
        if (mPackageManager == null) return null;
        String[] callingPackageNames = mPackageManager.getPackagesForUid(Binder.getCallingUid());
        return (callingPackageNames == null) ? null : callingPackageNames[0];
    }

    /**
     * Make sure the device has required telephony feature
     *
     * @throws UnsupportedOperationException if the device does not have required telephony feature
     */
    private void enforceTelephonyFeatureWithException(@Nullable String callingPackage,
            @NonNull String telephonyFeature, @NonNull String methodName) {
        if (callingPackage == null || mPackageManager == null) {
            return;
        }

        if (!mFeatureFlags.enforceTelephonyFeatureMappingForPublicApis()
                || !CompatChanges.isChangeEnabled(ENABLE_FEATURE_MAPPING, callingPackage,
                Binder.getCallingUserHandle())
                || mVendorApiLevel < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Skip to check associated telephony feature,
            // if compatibility change is not enabled for the current process or
            // the SDK version of vendor partition is less than Android V.
            return;
        }

        if (!mPackageManager.hasSystemFeature(telephonyFeature)) {
            throw new UnsupportedOperationException(
                    methodName + " is unsupported without " + telephonyFeature);
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
