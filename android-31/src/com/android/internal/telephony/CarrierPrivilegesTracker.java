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

import static android.telephony.CarrierConfigManager.EXTRA_SLOT_INDEX;
import static android.telephony.CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_CERTIFICATE_STRING_ARRAY;
import static android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;
import static android.telephony.TelephonyManager.EXTRA_SIM_STATE;
import static android.telephony.TelephonyManager.SIM_STATE_ABSENT;
import static android.telephony.TelephonyManager.SIM_STATE_LOADED;
import static android.telephony.TelephonyManager.SIM_STATE_NOT_READY;
import static android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.LocalLog;

import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * CarrierPrivilegesTracker will track the Carrier Privileges for a specific {@link Phone}.
 * Registered Telephony entities will receive notifications when the UIDs with these privileges
 * change.
 */
public class CarrierPrivilegesTracker extends Handler {
    private static final String TAG = CarrierPrivilegesTracker.class.getSimpleName();

    private static final boolean VDBG = false;

    private static final String SHA_1 = "SHA-1";
    private static final String SHA_256 = "SHA-256";

    /**
     * Action to register a Registrant with this Tracker.
     * obj: Registrant that will be notified of Carrier Privileged UID changes.
     */
    private static final int ACTION_REGISTER_LISTENER = 1;

    /**
     * Action to unregister a Registrant with this Tracker.
     * obj: Handler used by the Registrant that will be removed.
     */
    private static final int ACTION_UNREGISTER_LISTENER = 2;

    /**
     * Action for tracking when Carrier Configs are updated.
     * arg1: Subscription Id for the Carrier Configs update being broadcast
     * arg2: Slot Index for the Carrier Configs update being broadcast
     */
    private static final int ACTION_CARRIER_CONFIG_CERTS_UPDATED = 3;

    /**
     * Action for tracking when the Phone's SIM state changes.
     * arg1: slotId that this Action applies to
     * arg2: simState reported by this Broadcast
     */
    private static final int ACTION_SIM_STATE_UPDATED = 4;

    /**
     * Action for tracking when a package is installed or replaced on the device.
     * obj: String package name that was installed or replaced on the device.
     */
    private static final int ACTION_PACKAGE_ADDED_OR_REPLACED = 5;

    /**
     * Action for tracking when a package is uninstalled on the device.
     * obj: String package name that was installed on the device.
     */
    private static final int ACTION_PACKAGE_REMOVED = 6;

    /**
     * Action used to initialize the state of the Tracker.
     */
    private static final int ACTION_INITIALIZE_TRACKER = 7;

    private final Context mContext;
    private final Phone mPhone;
    private final CarrierConfigManager mCarrierConfigManager;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private final TelephonyManager mTelephonyManager;
    private final RegistrantList mRegistrantList;
    private final LocalLog mLocalLog;

    // Stores rules for Carrier Config-loaded rules
    private final List<UiccAccessRule> mCarrierConfigRules;

    // Stores rules for SIM-loaded rules.
    private final List<UiccAccessRule> mUiccRules;

    // Map of PackageName -> Certificate hashes for that Package
    private final Map<String, Set<String>> mInstalledPackageCerts;

    // Map of PackageName -> UIDs for that Package
    private final Map<String, Set<Integer>> mCachedUids;

    // Privileged UIDs must be kept in sorted order for update-checks.
    private int[] mPrivilegedUids;

    private final BroadcastReceiver mIntentReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action == null) return;

                    switch (action) {
                        case CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED: {
                            Bundle extras = intent.getExtras();
                            int slotIndex = extras.getInt(EXTRA_SLOT_INDEX);
                            int subId =
                                    extras.getInt(
                                            EXTRA_SUBSCRIPTION_INDEX, INVALID_SUBSCRIPTION_ID);
                            sendMessage(
                                    obtainMessage(
                                            ACTION_CARRIER_CONFIG_CERTS_UPDATED,
                                            subId,
                                            slotIndex));
                            break;
                        }
                        case TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED: // fall through
                        case TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED: {
                            Bundle extras = intent.getExtras();
                            int simState = extras.getInt(EXTRA_SIM_STATE, SIM_STATE_UNKNOWN);
                            int slotId =
                                    extras.getInt(PhoneConstants.PHONE_KEY, INVALID_SIM_SLOT_INDEX);

                            if (simState != SIM_STATE_ABSENT
                                    && simState != SIM_STATE_NOT_READY
                                    && simState != SIM_STATE_LOADED) return;

                            sendMessage(obtainMessage(ACTION_SIM_STATE_UPDATED, slotId, simState));
                            break;
                        }
                        case Intent.ACTION_PACKAGE_ADDED: // fall through
                        case Intent.ACTION_PACKAGE_REPLACED: // fall through
                        case Intent.ACTION_PACKAGE_REMOVED: {
                            int what =
                                    (action.equals(Intent.ACTION_PACKAGE_REMOVED))
                                            ? ACTION_PACKAGE_REMOVED
                                            : ACTION_PACKAGE_ADDED_OR_REPLACED;
                            Uri uri = intent.getData();
                            String pkgName = (uri != null) ? uri.getSchemeSpecificPart() : null;
                            if (TextUtils.isEmpty(pkgName)) {
                                Rlog.e(TAG, "Failed to get package from Intent");
                                return;
                            }

                            sendMessage(obtainMessage(what, pkgName));
                            break;
                        }
                    }
                }
            };

    public CarrierPrivilegesTracker(
            @NonNull Looper looper, @NonNull Phone phone, @NonNull Context context) {
        super(looper);
        mContext = context;
        mCarrierConfigManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        mPackageManager = mContext.getPackageManager();
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mPhone = phone;
        mLocalLog = new LocalLog(100);

        IntentFilter certFilter = new IntentFilter();
        certFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        certFilter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
        certFilter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        mContext.registerReceiver(mIntentReceiver, certFilter);

        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);

        // For package-related broadcasts, specify the data scheme for "package" to receive the
        // package name along with the broadcast
        packageFilter.addDataScheme("package");
        mContext.registerReceiver(mIntentReceiver, packageFilter);

        mRegistrantList = new RegistrantList();
        mCarrierConfigRules = new ArrayList<>();
        mUiccRules = new ArrayList<>();
        mInstalledPackageCerts = new ArrayMap<>();
        mCachedUids = new ArrayMap<>();
        mPrivilegedUids = new int[0];

        sendMessage(obtainMessage(ACTION_INITIALIZE_TRACKER));
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case ACTION_REGISTER_LISTENER: {
                handleRegisterListener((Registrant) msg.obj);
                break;
            }
            case ACTION_UNREGISTER_LISTENER: {
                handleUnregisterListener((Handler) msg.obj);
                break;
            }
            case ACTION_CARRIER_CONFIG_CERTS_UPDATED: {
                int subId = msg.arg1;
                int slotIndex = msg.arg2;
                handleCarrierConfigUpdated(subId, slotIndex);
                break;
            }
            case ACTION_SIM_STATE_UPDATED: {
                handleSimStateChanged(msg.arg1, msg.arg2);
                break;
            }
            case ACTION_PACKAGE_ADDED_OR_REPLACED: {
                String pkgName = (String) msg.obj;
                handlePackageAddedOrReplaced(pkgName);
                break;
            }
            case ACTION_PACKAGE_REMOVED: {
                String pkgName = (String) msg.obj;
                handlePackageRemoved(pkgName);
                break;
            }
            case ACTION_INITIALIZE_TRACKER: {
                handleInitializeTracker();
                break;
            }
            default: {
                Rlog.e(TAG, "Received unknown msg type: " + msg.what);
                break;
            }
        }
    }

    private void handleRegisterListener(Registrant registrant) {
        mRegistrantList.add(registrant);
        registrant.notifyResult(mPrivilegedUids);
    }

    private void handleUnregisterListener(Handler handler) {
        mRegistrantList.remove(handler);
    }

    private void handleCarrierConfigUpdated(int subId, int slotIndex) {
        if (slotIndex != mPhone.getPhoneId()) return;

        List<UiccAccessRule> updatedCarrierConfigRules = Collections.EMPTY_LIST;

        // Carrier Config broadcasts with INVALID_SUBSCRIPTION_ID when the SIM is removed. This is
        // an expected event. When this happens, clear the certificates from the previous configs.
        // The rules will be cleared in maybeUpdateRulesAndNotifyRegistrants() below.
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            updatedCarrierConfigRules = getCarrierConfigRules(subId);
        }

        mLocalLog.log("CarrierConfigUpdated:"
                + " subId=" + subId
                + " slotIndex=" + slotIndex
                + " updated CarrierConfig rules=" + updatedCarrierConfigRules);
        maybeUpdateRulesAndNotifyRegistrants(mCarrierConfigRules, updatedCarrierConfigRules);
    }

    private List<UiccAccessRule> getCarrierConfigRules(int subId) {
        PersistableBundle carrierConfigs = mCarrierConfigManager.getConfigForSubId(subId);
        if (!mCarrierConfigManager.isConfigForIdentifiedCarrier(carrierConfigs)) {
            return Collections.EMPTY_LIST;
        }

        String[] carrierConfigRules =
                carrierConfigs.getStringArray(KEY_CARRIER_CERTIFICATE_STRING_ARRAY);
        if (carrierConfigRules == null) {
            return Collections.EMPTY_LIST;
        }
        return Arrays.asList(UiccAccessRule.decodeRulesFromCarrierConfig(carrierConfigRules));
    }

    private void handleSimStateChanged(int slotId, int simState) {
        if (slotId != mPhone.getPhoneId()) return;

        List<UiccAccessRule> updatedUiccRules = Collections.EMPTY_LIST;

        // Only include the UICC rules if the SIM is fully loaded
        if (simState == SIM_STATE_LOADED) {
            updatedUiccRules = getSimRules();
        }

        mLocalLog.log("SIM State Changed:"
                + " slotId=" + slotId
                + " simState=" + simState
                + " updated SIM-loaded rules=" + updatedUiccRules);
        maybeUpdateRulesAndNotifyRegistrants(mUiccRules, updatedUiccRules);
    }

    private List<UiccAccessRule> getSimRules() {
        if (!mTelephonyManager.hasIccCard(mPhone.getPhoneId())) {
            return Collections.EMPTY_LIST;
        }
        UiccCard uiccCard = mPhone.getUiccCard();
        if (uiccCard == null) {
            Rlog.w(
                    TAG,
                    "Null UiccCard, but hasIccCard was true for phoneId " + mPhone.getPhoneId());
            return Collections.EMPTY_LIST;
        }
        UiccProfile uiccProfile = uiccCard.getUiccProfile();
        if (uiccProfile == null) {
            Rlog.w(
                    TAG,
                    "Null UiccProfile, but hasIccCard was true for phoneId " + mPhone.getPhoneId());
            return Collections.EMPTY_LIST;
        }
        return uiccProfile.getCarrierPrivilegeAccessRules();
    }

    private void handlePackageAddedOrReplaced(String pkgName) {
        PackageInfo pkg;
        try {
            pkg = mPackageManager.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES);
        } catch (NameNotFoundException e) {
            Rlog.e(TAG, "Error getting installed package: " + pkgName, e);
            return;
        }

        updateCertsForPackage(pkg);
        mCachedUids.put(pkg.packageName, getUidsForPackage(pkg.packageName));
        mLocalLog.log("Package added/replaced:"
                + " pkg=" + Rlog.pii(TAG, pkgName)
                + " cert hashes=" + mInstalledPackageCerts.get(pkgName));

        maybeUpdatePrivilegedUidsAndNotifyRegistrants();
    }

    private void updateCertsForPackage(PackageInfo pkg) {
        Set<String> certs = new ArraySet<>();
        List<Signature> signatures = UiccAccessRule.getSignatures(pkg);
        for (Signature signature : signatures) {
            byte[] sha1 = UiccAccessRule.getCertHash(signature, SHA_1);
            certs.add(IccUtils.bytesToHexString(sha1).toUpperCase());

            byte[] sha256 = UiccAccessRule.getCertHash(signature, SHA_256);
            certs.add(IccUtils.bytesToHexString(sha256).toUpperCase());
        }

        mInstalledPackageCerts.put(pkg.packageName, certs);
    }

    private void handlePackageRemoved(String pkgName) {
        if (mInstalledPackageCerts.remove(pkgName) == null) {
            Rlog.e(TAG, "Unknown package was uninstalled: " + pkgName);
            return;
        }
        mCachedUids.remove(pkgName);

        mLocalLog.log("Package removed: pkg=" + Rlog.pii(TAG, pkgName));

        maybeUpdatePrivilegedUidsAndNotifyRegistrants();
    }

    private void handleInitializeTracker() {
        // Cache CarrierConfig rules
        mCarrierConfigRules.addAll(getCarrierConfigRules(mPhone.getSubId()));

        // Cache SIM rules
        mUiccRules.addAll(getSimRules());

        // Cache all installed packages and their certs
        int flags =
                PackageManager.MATCH_DISABLED_COMPONENTS
                        | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.GET_SIGNING_CERTIFICATES;
        List<PackageInfo> installedPackages =
                mPackageManager.getInstalledPackagesAsUser(
                        flags, UserHandle.SYSTEM.getIdentifier());
        for (PackageInfo pkg : installedPackages) {
            updateCertsForPackage(pkg);
        }

        // Okay because no registrants exist yet
        maybeUpdatePrivilegedUidsAndNotifyRegistrants();

        String msg = "Initializing state:"
                + " CarrierConfig rules=" + mCarrierConfigRules
                + " SIM-loaded rules=" + mUiccRules;
        if (VDBG) {
            msg += " installed pkgs=" + getObfuscatedPackages();
        }
        mLocalLog.log(msg);
    }

    private String getObfuscatedPackages() {
        StringJoiner obfuscatedPkgs = new StringJoiner(",", "{", "}");
        for (Map.Entry<String, Set<String>> pkg : mInstalledPackageCerts.entrySet()) {
            obfuscatedPkgs.add("pkg(" + Rlog.pii(TAG, pkg.getKey()) + ")=" + pkg.getValue());
        }
        return obfuscatedPkgs.toString();
    }

    private void maybeUpdateRulesAndNotifyRegistrants(
            List<UiccAccessRule> currentRules, List<UiccAccessRule> updatedRules) {
        if (currentRules.equals(updatedRules)) return;

        currentRules.clear();
        currentRules.addAll(updatedRules);

        maybeUpdatePrivilegedUidsAndNotifyRegistrants();
    }

    private void maybeUpdatePrivilegedUidsAndNotifyRegistrants() {
        int[] currentPrivilegedUids = getCurrentPrivilegedUidsForAllUsers();

        // Sort UIDs for the equality check
        Arrays.sort(currentPrivilegedUids);
        if (Arrays.equals(mPrivilegedUids, currentPrivilegedUids)) return;

        mPrivilegedUids = currentPrivilegedUids;
        mRegistrantList.notifyResult(mPrivilegedUids);

        mLocalLog.log("Privileged UIDs changed. New UIDs=" + Arrays.toString(mPrivilegedUids));
    }

    private int[] getCurrentPrivilegedUidsForAllUsers() {
        Set<Integer> privilegedUids = new ArraySet<>();
        for (Map.Entry<String, Set<String>> e : mInstalledPackageCerts.entrySet()) {
            if (isPackagePrivileged(e.getKey(), e.getValue())) {
                privilegedUids.addAll(getUidsForPackage(e.getKey()));
            }
        }

        IntArray result = new IntArray(privilegedUids.size());
        for (int uid : privilegedUids) {
            result.add(uid);
        }
        return result.toArray();
    }

    /**
     * Returns true iff there is an overlap between the provided certificate hashes and the
     * certificate hashes stored in mCarrierConfigRules and mUiccRules.
     */
    private boolean isPackagePrivileged(String pkgName, Set<String> certs) {
        // Double-nested for loops, but each collection should contain at most 2 elements in nearly
        // every case.
        // TODO(b/184382310) find a way to speed this up
        for (String cert : certs) {
            for (UiccAccessRule rule : mCarrierConfigRules) {
                if (rule.matches(cert, pkgName)) {
                    return true;
                }
            }
            for (UiccAccessRule rule : mUiccRules) {
                if (rule.matches(cert, pkgName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<Integer> getUidsForPackage(String pkgName) {
        if (mCachedUids.containsKey(pkgName)) {
            return mCachedUids.get(pkgName);
        }

        Set<Integer> uids = new ArraySet<>();
        List<UserInfo> users = mUserManager.getUsers();
        for (UserInfo user : users) {
            int userId = user.getUserHandle().getIdentifier();
            try {
                uids.add(mPackageManager.getPackageUidAsUser(pkgName, userId));
            } catch (NameNotFoundException exception) {
                // Didn't find package. Continue looking at other packages
                Rlog.e(TAG, "Unable to find uid for package " + pkgName + " and user " + userId);
            }
        }
        mCachedUids.put(pkgName, uids);
        return uids;
    }

    /**
     * Dump the local log buffer and other internal state of CarrierPrivilegesTracker.
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of CarrierPrivilegesTracker");
        pw.println("CarrierPrivilegesTracker - Log Begin ----");
        mLocalLog.dump(fd, pw, args);
        pw.println("CarrierPrivilegesTracker - Log End ----");
        pw.println("CarrierPrivilegesTracker - Privileged UIDs: "
                + Arrays.toString(mPrivilegedUids));
        pw.println("CarrierPrivilegesTracker - SIM-loaded rules: " + mUiccRules);
        pw.println("CarrierPrivilegesTracker - Carrier config rules: " + mCarrierConfigRules);
        if (VDBG) {
            pw.println("CarrierPrivilegesTracker - Obfuscated Pkgs + Certs: "
                    + getObfuscatedPackages());
        }
    }

    /**
     * Registers the given Registrant with this tracker.
     *
     * <p>After being registered, the Registrant will be notified with the current Carrier
     * Privileged UIDs for this Phone.
     */
    public void registerCarrierPrivilegesListener(Handler h, int what, Object obj) {
        sendMessage(obtainMessage(ACTION_REGISTER_LISTENER, new Registrant(h, what, obj)));
    }

    /**
     * Unregisters the given listener with this tracker.
     */
    public void unregisterCarrierPrivilegesListener(Handler handler) {
        sendMessage(obtainMessage(ACTION_UNREGISTER_LISTENER, handler));
    }
}
