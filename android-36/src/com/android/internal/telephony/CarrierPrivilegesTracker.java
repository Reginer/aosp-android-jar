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

import static android.telephony.CarrierConfigManager.KEY_CARRIER_CERTIFICATE_STRING_ARRAY;
import static android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX;
import static android.telephony.TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS;
import static android.telephony.TelephonyManager.CARRIER_PRIVILEGE_STATUS_NO_ACCESS;
import static android.telephony.TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
import static android.telephony.TelephonyManager.EXTRA_SIM_STATE;
import static android.telephony.TelephonyManager.SIM_STATE_ABSENT;
import static android.telephony.TelephonyManager.SIM_STATE_LOADED;
import static android.telephony.TelephonyManager.SIM_STATE_NOT_READY;
import static android.telephony.TelephonyManager.SIM_STATE_READY;
import static android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;

import android.annotation.ElapsedRealtimeLong;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.carrier.CarrierService;
import android.telephony.Annotation.CarrierPrivilegeStatus;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyRegistryManager;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Pair;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.telephony.util.WorkerThread;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * CarrierPrivilegesTracker will track the Carrier Privileges for a specific {@link Phone}.
 * Registered Telephony entities will receive notifications when the UIDs with these privileges
 * change.
 */
@SuppressLint("MissingPermission")
public class CarrierPrivilegesTracker extends Handler {
    private static final String TAG = CarrierPrivilegesTracker.class.getSimpleName();

    private static final boolean VDBG = false;

    private static final String SHA_1 = "SHA-1";
    private static final String SHA_256 = "SHA-256";

    private static final int PACKAGE_NOT_PRIVILEGED = 0;
    private static final int PACKAGE_PRIVILEGED_FROM_CARRIER_CONFIG = 1;
    private static final int PACKAGE_PRIVILEGED_FROM_SIM = 2;
    private static final int PACKAGE_PRIVILEGED_FROM_CARRIER_SERVICE_TEST_OVERRIDE = 3;

    // TODO(b/232273884): Turn feature on when find solution to handle the inter-carriers switching
    /**
     * Time delay to clear UICC rules after UICC is gone.
     * This introduces the grace period to retain carrier privileges when SIM is removed.
     *
     * This feature is off by default due to the security concern during inter-carriers switching.
     */
    private static final long CLEAR_UICC_RULES_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(0);

    /**
     * PackageManager flags used to query installed packages.
     * Include DISABLED_UNTIL_USED components. This facilitates cases where a carrier app
     * is disabled by default, and some other component wants to enable it when it has
     * gained carrier privileges (as an indication that a matching SIM has been inserted).
     */
    private static final int INSTALLED_PACKAGES_QUERY_FLAGS =
            PackageManager.GET_SIGNING_CERTIFICATES
                    | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
                    | PackageManager.MATCH_HIDDEN_UNTIL_INSTALLED_COMPONENTS;

    /**
     * Action for tracking when the Phone's SIM state changes.
     * arg1: slotId that this Action applies to
     * arg2: simState reported by this Broadcast
     */
    private static final int ACTION_SIM_STATE_UPDATED = 4;

    /**
     * Action for tracking when a package is installed, replaced or changed (exclude the case
     * disabled by user) on the device.
     * obj: String package name that was installed, replaced or changed on the device.
     */
    private static final int ACTION_PACKAGE_ADDED_REPLACED_OR_CHANGED = 5;

    /**
     * Action for tracking when a package is uninstalled or disabled by user on the device.
     * obj: String package name that was installed or disabled by user on the device.
     */
    private static final int ACTION_PACKAGE_REMOVED_OR_DISABLED_BY_USER = 6;

    /**
     * Action used to initialize the state of the Tracker.
     */
    private static final int ACTION_INITIALIZE_TRACKER = 7;

    /**
     * Action to set the test override rule through {@link TelephonyManager#setCarrierTestOverride}.
     * obj: String of the carrierPrivilegeRules from method setCarrierTestOverride.
     */
    private static final int ACTION_SET_TEST_OVERRIDE_RULE = 8;

    /**
     * Action to clear UICC rules.
     */
    private static final int ACTION_CLEAR_UICC_RULES = 9;

    /**
     * Action to handle the case when UiccAccessRules has been loaded.
     */
    private static final int ACTION_UICC_ACCESS_RULES_LOADED = 10;

    /**
     * Action to set the test override rule through {@link
     * TelephonyManager#setCarrierServicePackageOverride}.
     *
     * <p>obj: String of the carrierServicePackage from method setCarrierServicePackageOverride.
     */
    private static final int ACTION_SET_TEST_OVERRIDE_CARRIER_SERVICE_PACKAGE = 11;

    private final Context mContext;
    @NonNull
    private final FeatureFlags mFeatureFlags;
    private final Phone mPhone;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private final CarrierConfigManager mCarrierConfigManager;
    private final TelephonyManager mTelephonyManager;
    private final TelephonyRegistryManager mTelephonyRegistryManager;

    @NonNull private final LocalLog mLocalLog = new LocalLog(64);
    // Stores rules for Carrier Config-loaded rules
    @NonNull private final List<UiccAccessRule> mCarrierConfigRules = new ArrayList<>();
    // Stores rules for SIM-loaded rules.
    @NonNull private final List<UiccAccessRule> mUiccRules = new ArrayList<>();
    // Stores rule from test override (through TelephonyManager#setCarrierTestOverride).
    // - Null list indicates no test override (CC and UICC rules are respected)
    // - Empty list indicates test override to simulate no rules (CC and UICC rules are ignored)
    // - Non-empty list indicates test override with specific rules (CC and UICC rules are ignored)
    @Nullable private List<UiccAccessRule> mTestOverrideRules = null;
    @Nullable private String mTestOverrideCarrierServicePackage = null;
    // Map of PackageName -> Certificate hashes for that Package
    @NonNull private final Map<String, Set<Integer>> mInstalledPackageCertHashes = new ArrayMap<>();
    // Map of PackageName -> UIDs for that Package
    @NonNull private final Map<String, Set<Integer>> mCachedUids = new ArrayMap<>();

    // This should be used to guard critical section either with
    // mPrivilegedPackageInfoLock.readLock() or mPrivilegedPackageInfoLock.writeLock(), but never
    // with the mPrivilegedPackageInfoLock object itself.
    @NonNull private final ReadWriteLock mPrivilegedPackageInfoLock = new ReentrantReadWriteLock();
    // Package names and UIDs of apps that currently hold carrier privileges.
    @GuardedBy(anyOf = {"mPrivilegedPackageInfoLock.readLock()",
            "mPrivilegedPackageInfoLock.writeLock()"})
    @NonNull private PrivilegedPackageInfo mPrivilegedPackageInfo = new PrivilegedPackageInfo();

    // Uptime in millis on when the NEXT clear-up of UiccRules are scheduled
    @ElapsedRealtimeLong
    private long mClearUiccRulesUptimeMillis = CLEAR_UICC_RULE_NOT_SCHEDULED;
    // Constant indicates no schedule to clear UiccRules
    private static final long CLEAR_UICC_RULE_NOT_SCHEDULED = -1;

    // Indicates SIM has reached SIM_STATE_READY but not SIM_STATE_LOADED yet. During this transient
    // state, all the information previously loaded from SIM may be updated soon later and thus
    // unreliable. For security's concern, any carrier privileges check should return
    // CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED (instead of neither HAS_ACCESS nor NO_ACCESS) until
    // SIM becomes LOADED again, or grace period specified by CLEAR_UICC_RULES_DELAY_MILLIS expires.
    @GuardedBy(anyOf = {"mPrivilegedPackageInfoLock.readLock()",
            "mPrivilegedPackageInfoLock.writeLock()"})
    private boolean mSimIsReadyButNotLoaded = false;

    private volatile Handler mCurrentHandler;

    /** Small snapshot to hold package names and UIDs of privileged packages. */
    private static final class PrivilegedPackageInfo {
        @NonNull final Set<String> mPackageNames;
        @NonNull final Set<Integer> mUids;
        // The carrier service (packageName, UID) pair
        @NonNull final Pair<String, Integer> mCarrierService;

        PrivilegedPackageInfo() {
            mPackageNames = Collections.emptySet();
            mUids = Collections.emptySet();
            mCarrierService = new Pair<>(null, Process.INVALID_UID);
        }

        PrivilegedPackageInfo(@NonNull Set<String> packageNames, @NonNull Set<Integer> uids,
                @NonNull Pair<String, Integer> carrierService) {
            mPackageNames = packageNames;
            mUids = uids;
            mCarrierService = carrierService;
        }

        @Override
        public String toString() {
            return "{packageNames="
                    + getObfuscatedPackages(mPackageNames, pkg -> Rlog.pii(TAG, pkg))
                    + ", uids="
                    + mUids
                    + ", carrierServicePackageName="
                    + Rlog.pii(TAG, mCarrierService.first)
                    + ", carrierServiceUid="
                    + mCarrierService.second
                    + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PrivilegedPackageInfo)) {
                return false;
            }
            PrivilegedPackageInfo other = (PrivilegedPackageInfo) o;
            return mPackageNames.equals(other.mPackageNames) && mUids.equals(other.mUids)
                    && mCarrierService.equals(other.mCarrierService);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mPackageNames, mUids, mCarrierService);
        }
    }

    private final BroadcastReceiver mIntentReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action == null) return;

                    switch (action) {
                        case TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED: // fall through
                        case TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED: {
                            Bundle extras = intent.getExtras();
                            int simState = extras.getInt(EXTRA_SIM_STATE, SIM_STATE_UNKNOWN);
                            int slotId =
                                    extras.getInt(PhoneConstants.PHONE_KEY, INVALID_SIM_SLOT_INDEX);

                            if (simState != SIM_STATE_ABSENT
                                    && simState != SIM_STATE_NOT_READY
                                    && simState != SIM_STATE_READY
                                    && simState != SIM_STATE_LOADED) {
                                return;
                            }

                            mCurrentHandler.sendMessage(
                                    mCurrentHandler.obtainMessage(
                                            ACTION_SIM_STATE_UPDATED, slotId, simState));
                            break;
                        }
                        case Intent.ACTION_PACKAGE_ADDED: // fall through
                        case Intent.ACTION_PACKAGE_REPLACED: // fall through
                        case Intent.ACTION_PACKAGE_REMOVED: // fall through
                        case Intent.ACTION_PACKAGE_CHANGED: {
                            Uri uri = intent.getData();
                            String pkgName = (uri != null) ? uri.getSchemeSpecificPart() : null;
                            if (TextUtils.isEmpty(pkgName)) {
                                Rlog.e(TAG, "Failed to get package from Intent");
                                return;
                            }

                            boolean removed = action.equals(Intent.ACTION_PACKAGE_REMOVED);
                            boolean disabledByUser = false;
                            boolean notExist = false;
                            try {
                                disabledByUser = action.equals(Intent.ACTION_PACKAGE_CHANGED)
                                        && getApplicationEnabledSetting(pkgName)
                                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
                            } catch (IllegalArgumentException iae) {
                                // Very rare case when package changed race with package removed
                                Rlog.w(TAG, "Package does not exist: " + pkgName);
                                notExist = true;
                            }
                            // When a package is explicitly disabled by the user or does not exist,
                            // treat them as if it was removed: clear it from the cache
                            int what = (removed || disabledByUser || notExist)
                                    ? ACTION_PACKAGE_REMOVED_OR_DISABLED_BY_USER
                                    : ACTION_PACKAGE_ADDED_REPLACED_OR_CHANGED;

                            mCurrentHandler.sendMessage(
                                    mCurrentHandler.obtainMessage(what, pkgName));
                            break;
                        }
                    }
                }
            };

    public CarrierPrivilegesTracker(
            @NonNull Looper looper, @NonNull Phone phone,
            @NonNull Context context, @NonNull FeatureFlags featureFlags) {
        super(looper);
        mPhone = phone;
        mContext = context;
        mFeatureFlags = featureFlags;
        mPackageManager = mContext.getPackageManager();
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mCarrierConfigManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        // Callback is executed in handler thread and directly handles carrier config update
        if (mCarrierConfigManager != null) {
            mCarrierConfigManager.registerCarrierConfigChangeListener(this::post,
                    (slotIndex, subId, carrierId, specificCarrierId) -> handleCarrierConfigUpdated(
                            subId, slotIndex));
        }
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyRegistryManager =
                (TelephonyRegistryManager)
                        mContext.getSystemService(Context.TELEPHONY_REGISTRY_SERVICE);

        if (mFeatureFlags.asyncInitCarrierPrivilegesTracker()) {
            final Object localLock = new Object();
            if (mFeatureFlags.threadShred()) {
                mCurrentHandler = new Handler(WorkerThread.get().getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch(msg.what) {
                            case ACTION_INITIALIZE_TRACKER:
                                handleInitializeTracker();
                                if (!hasMessagesOrCallbacks()) {
                                    mCurrentHandler = CarrierPrivilegesTracker.this;
                                }
                                break;
                            default:
                                Message m = CarrierPrivilegesTracker.this.obtainMessage();
                                m.copyFrom(msg);
                                m.sendToTarget();
                                if (!hasMessagesOrCallbacks()) {
                                    mCurrentHandler = CarrierPrivilegesTracker.this;
                                }
                                break;
                        }
                    }
                };
            } else {
                HandlerThread initializerThread =
                        new HandlerThread("CarrierPrivilegesTracker Initializer") {
                            @Override
                            protected void onLooperPrepared() {
                                synchronized (localLock) {
                                    localLock.notifyAll();
                                }
                            }
                        };
                synchronized (localLock) {
                    initializerThread.start();
                    while (true) {
                        try {
                            localLock.wait();
                            break;
                        } catch (InterruptedException ie) {
                        }
                    }
                }
                mCurrentHandler = new Handler(initializerThread.getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch(msg.what) {
                            case ACTION_INITIALIZE_TRACKER:
                                handleInitializeTracker();
                                if (!hasMessagesOrCallbacks()) {
                                    mCurrentHandler = CarrierPrivilegesTracker.this;
                                    initializerThread.quitSafely();
                                }
                                break;
                            default:
                                Message m = CarrierPrivilegesTracker.this.obtainMessage();
                                m.copyFrom(msg);
                                m.sendToTarget();
                                if (!hasMessagesOrCallbacks()) {
                                    mCurrentHandler = CarrierPrivilegesTracker.this;
                                    initializerThread.quitSafely();
                                }
                                break;
                        }
                    }
                };
            }
        } else {
            mCurrentHandler = this;
        }

        mCurrentHandler.sendMessage(obtainMessage(ACTION_INITIALIZE_TRACKER));

        IntentFilter certFilter = new IntentFilter();
        certFilter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
        certFilter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        if (mFeatureFlags.supportCarrierServicesForHsum()) {
            mContext.registerReceiverAsUser(
                    mIntentReceiver, UserHandle.of(ActivityManager.getCurrentUser()), certFilter,
                    /* broadcastPermission= */ null, /* scheduler= */ null);
        } else {
            mContext.registerReceiver(mIntentReceiver, certFilter);
        }

        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);

        // For package-related broadcasts, specify the data scheme for "package" to receive the
        // package name along with the broadcast
        packageFilter.addDataScheme("package");
        if (mFeatureFlags.supportCarrierServicesForHsum()) {
            mContext.registerReceiverAsUser(
                    mIntentReceiver, UserHandle.of(ActivityManager.getCurrentUser()), packageFilter,
                    /* broadcastPermission= */ null, /* scheduler= */ null);
        } else {
            mContext.registerReceiver(mIntentReceiver, packageFilter);
        }

    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case ACTION_SIM_STATE_UPDATED: {
                handleSimStateChanged(msg.arg1, msg.arg2);
                break;
            }
            case ACTION_PACKAGE_ADDED_REPLACED_OR_CHANGED: {
                String pkgName = (String) msg.obj;
                handlePackageAddedReplacedOrChanged(pkgName);
                break;
            }
            case ACTION_PACKAGE_REMOVED_OR_DISABLED_BY_USER: {
                String pkgName = (String) msg.obj;
                handlePackageRemovedOrDisabledByUser(pkgName);
                break;
            }
            case ACTION_INITIALIZE_TRACKER: {
                handleInitializeTracker();
                break;
            }
            case ACTION_SET_TEST_OVERRIDE_RULE: {
                String carrierPrivilegeRules = (String) msg.obj;
                handleSetTestOverrideRules(carrierPrivilegeRules);
                break;
            }
            case ACTION_CLEAR_UICC_RULES: {
                handleClearUiccRules();
                break;
            }
            case ACTION_UICC_ACCESS_RULES_LOADED: {
                handleUiccAccessRulesLoaded();
                break;
            }
            case ACTION_SET_TEST_OVERRIDE_CARRIER_SERVICE_PACKAGE: {
                String carrierServicePackage = (String) msg.obj;
                handleSetTestOverrideCarrierServicePackage(carrierServicePackage);
                break;
            }
            default: {
                Rlog.e(TAG, "Received unknown msg type: " + msg.what);
                break;
            }
        }
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

    @NonNull
    private List<UiccAccessRule> getCarrierConfigRules(int subId) {
        PersistableBundle carrierConfigs =
                CarrierConfigManager.getCarrierConfigSubset(
                        mContext, subId, KEY_CARRIER_CERTIFICATE_STRING_ARRAY);
        // CarrierConfigManager#isConfigForIdentifiedCarrier can handle null or empty bundle
        if (mCarrierConfigManager == null
                || !mCarrierConfigManager.isConfigForIdentifiedCarrier(carrierConfigs)) {
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

        mPrivilegedPackageInfoLock.writeLock().lock();
        try {
            mSimIsReadyButNotLoaded = simState == SIM_STATE_READY;
        } finally {
            mPrivilegedPackageInfoLock.writeLock().unlock();
        }

        // Only include the UICC rules if the SIM is fully loaded
        if (simState == SIM_STATE_LOADED) {
            mLocalLog.log("SIM fully loaded, handleUiccAccessRulesLoaded.");
            handleUiccAccessRulesLoaded();
        } else {
            if (!mUiccRules.isEmpty()
                    && mClearUiccRulesUptimeMillis == CLEAR_UICC_RULE_NOT_SCHEDULED) {
                mClearUiccRulesUptimeMillis =
                        SystemClock.uptimeMillis() + CLEAR_UICC_RULES_DELAY_MILLIS;
                mCurrentHandler.sendMessageAtTime(obtainMessage(ACTION_CLEAR_UICC_RULES),
                        mClearUiccRulesUptimeMillis);
                mLocalLog.log("SIM is gone, simState=" + TelephonyManager.simStateToString(simState)
                        + ". Delay " + TimeUnit.MILLISECONDS.toSeconds(
                        CLEAR_UICC_RULES_DELAY_MILLIS) + " seconds to clear UICC rules.");
            } else {
                mLocalLog.log(
                        "Ignore SIM gone event while UiccRules is empty or waiting to be emptied.");
            }
        }
    }

    private void handleUiccAccessRulesLoaded() {
        mClearUiccRulesUptimeMillis = CLEAR_UICC_RULE_NOT_SCHEDULED;
        removeMessages(ACTION_CLEAR_UICC_RULES);

        List<UiccAccessRule> updatedUiccRules = getSimRules();
        mLocalLog.log("UiccAccessRules loaded:"
                + " updated SIM-loaded rules=" + updatedUiccRules);
        maybeUpdateRulesAndNotifyRegistrants(mUiccRules, updatedUiccRules);
    }

    /** Called when UiccAccessRules has been loaded */
    public void onUiccAccessRulesLoaded() {
        sendEmptyMessage(ACTION_UICC_ACCESS_RULES_LOADED);
    }

    private void handleClearUiccRules() {
        mClearUiccRulesUptimeMillis = CLEAR_UICC_RULE_NOT_SCHEDULED;
        removeMessages(ACTION_CLEAR_UICC_RULES);
        maybeUpdateRulesAndNotifyRegistrants(mUiccRules, Collections.EMPTY_LIST);
    }

    @NonNull
    private List<UiccAccessRule> getSimRules() {
        if (!mTelephonyManager.hasIccCard(mPhone.getPhoneId())) {
            return Collections.EMPTY_LIST;
        }

        UiccPort uiccPort = mPhone.getUiccPort();
        if (uiccPort == null) {
            Rlog.w(
                    TAG,
                    "Null UiccPort, but hasIccCard was present for phoneId " + mPhone.getPhoneId());
            return Collections.EMPTY_LIST;
        }

        UiccProfile uiccProfile = uiccPort.getUiccProfile();
        if (uiccProfile == null) {
            Rlog.w(
                    TAG,
                    "Null UiccProfile, but hasIccCard was true for phoneId " + mPhone.getPhoneId());
            return Collections.EMPTY_LIST;
        }
        return uiccProfile.getCarrierPrivilegeAccessRules();
    }

    private PackageInfo getPackageInfoForPackage(@Nullable String pkgName) {
        if (pkgName == null) return null;

        PackageInfo pkg;
        try {
            return mFeatureFlags.supportCarrierServicesForHsum()
                        ? mPackageManager.getPackageInfoAsUser(
                                pkgName,
                                INSTALLED_PACKAGES_QUERY_FLAGS,
                                ActivityManager.getCurrentUser())
                        : mPackageManager.getPackageInfo(
                                pkgName, INSTALLED_PACKAGES_QUERY_FLAGS);
        } catch (NameNotFoundException e) {
            Rlog.e(TAG, "Error getting installed package: " + pkgName, e);
            return null;
        }
    }

    private void handlePackageAddedReplacedOrChanged(@Nullable String pkgName) {
        PackageInfo pkg = getPackageInfoForPackage(pkgName);
        if (pkg == null) return;

        updateCertHashHashesForPackage(pkg);
        // Invalidate cache because this may be a package already on the device but getting
        // installed for a user it wasn't installed in before, which means there will be an
        // additional UID.
        getUidsForPackage(pkg.packageName, /* invalidateCache= */ true);
        if (VDBG) {
            Rlog.d(TAG, "Package added/replaced/changed:"
                    + " pkg=" + Rlog.pii(TAG, pkgName)
                    + " cert hashes=" + mInstalledPackageCertHashes.get(pkgName));
        }

        maybeUpdatePrivilegedPackagesAndNotifyRegistrants();
    }

    private void updateCertHashHashesForPackage(@NonNull PackageInfo pkg) {
        Set<Integer> certs = new ArraySet<>(2);
        List<Signature> signatures = UiccAccessRule.getSignatures(pkg);
        for (Signature signature : signatures) {
            byte[] sha1 = UiccAccessRule.getCertHash(signature, SHA_1);
            certs.add(UiccAccessRule.getCertificateHashHashCode(sha1));

            byte[] sha256 = UiccAccessRule.getCertHash(signature, SHA_256);
            certs.add(UiccAccessRule.getCertificateHashHashCode(sha256));
        }

        mInstalledPackageCertHashes.put(pkg.packageName, certs);
    }

    private Set<byte[]> getCertsForPackage(@NonNull String pkgName) {
        PackageInfo pkg = getPackageInfoForPackage(pkgName);
        if (pkg == null) return Collections.emptySet();

        List<Signature> signatures = UiccAccessRule.getSignatures(pkg);

        ArraySet<byte[]> certs = new ArraySet<>(2);
        for (Signature signature : signatures) {
            certs.add(UiccAccessRule.getCertHash(signature, SHA_1));
            certs.add(UiccAccessRule.getCertHash(signature, SHA_256));
        }

        return certs;
    }

    private void handlePackageRemovedOrDisabledByUser(@Nullable String pkgName) {
        if (pkgName == null) return;

        if (mInstalledPackageCertHashes.remove(pkgName) == null
                || mCachedUids.remove(pkgName) == null) {
            Rlog.e(TAG, "Unknown package was uninstalled or disabled by user: " + pkgName);
            return;
        }

        if (VDBG) {
            Rlog.d(TAG, "Package removed or disabled by user: pkg=" + Rlog.pii(TAG, pkgName));
        }

        maybeUpdatePrivilegedPackagesAndNotifyRegistrants();
    }

    private void handleInitializeTracker() {
        // Cache CarrierConfig rules
        mCarrierConfigRules.addAll(getCarrierConfigRules(mPhone.getSubId()));

        // Cache SIM rules
        mUiccRules.addAll(getSimRules());

        // Cache all installed packages and their certs
        refreshInstalledPackageCache();

        // Okay because no registrants exist yet
        maybeUpdatePrivilegedPackagesAndNotifyRegistrants();

        String msg = "Initializing state:"
                + " CarrierConfig rules=" + mCarrierConfigRules
                + " SIM-loaded rules=" + mUiccRules;
        if (VDBG) {
            msg +=
                    " installed pkgs="
                            + getObfuscatedPackages(
                                    mInstalledPackageCertHashes.entrySet(),
                                    e -> "pkg(" + Rlog.pii(TAG, e.getKey()) + ")=" + e.getValue());
        }
        mLocalLog.log(msg);
    }

    private void refreshInstalledPackageCache() {
        List<PackageInfo> installedPackages =
                mPackageManager.getInstalledPackagesAsUser(
                        INSTALLED_PACKAGES_QUERY_FLAGS,
                        mFeatureFlags.supportCarrierServicesForHsum()
                                ? ActivityManager.getCurrentUser()
                                : UserHandle.SYSTEM.getIdentifier());
        for (PackageInfo pkg : installedPackages) {
            updateCertHashHashesForPackage(pkg);
            // This may be unnecessary before initialization, but invalidate the cache all the time
            // just in case to ensure consistency.
            getUidsForPackage(pkg.packageName, /* invalidateCache= */ true);
        }
    }

    @NonNull
    private static <T> String getObfuscatedPackages(
            @NonNull Collection<T> packageNames, @NonNull Function<T, String> obfuscator) {
        StringJoiner obfuscated = new StringJoiner(", ", "{", "}");
        for (T packageName : packageNames) {
            obfuscated.add(obfuscator.apply(packageName));
        }
        return obfuscated.toString();
    }

    private void maybeUpdateRulesAndNotifyRegistrants(@NonNull List<UiccAccessRule> currentRules,
            @NonNull List<UiccAccessRule> updatedRules) {
        if (currentRules.equals(updatedRules)) return;

        currentRules.clear();
        currentRules.addAll(updatedRules);

        maybeUpdatePrivilegedPackagesAndNotifyRegistrants();
    }

    private void maybeUpdatePrivilegedPackagesAndNotifyRegistrants() {
        PrivilegedPackageInfo currentPrivilegedPackageInfo =
                getCurrentPrivilegedPackagesForAllUsers();

        boolean carrierPrivilegesPackageNamesChanged;
        boolean carrierPrivilegesUidsChanged;
        boolean carrierServiceChanged;

        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            if (mPrivilegedPackageInfo.equals(currentPrivilegedPackageInfo)) return;

            mLocalLog.log("Privileged packages info changed. New state = "
                    + currentPrivilegedPackageInfo);

            carrierPrivilegesPackageNamesChanged =
                    !currentPrivilegedPackageInfo.mPackageNames.equals(
                            mPrivilegedPackageInfo.mPackageNames);
            carrierPrivilegesUidsChanged =
                    !currentPrivilegedPackageInfo.mUids.equals(mPrivilegedPackageInfo.mUids);
            carrierServiceChanged = !currentPrivilegedPackageInfo.mCarrierService.equals(
                    mPrivilegedPackageInfo.mCarrierService);
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }

        mPrivilegedPackageInfoLock.writeLock().lock();
        try {
            mPrivilegedPackageInfo = currentPrivilegedPackageInfo;
        } finally {
            mPrivilegedPackageInfoLock.writeLock().unlock();
        }

        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            if (carrierPrivilegesPackageNamesChanged || carrierPrivilegesUidsChanged) {
                mTelephonyRegistryManager.notifyCarrierPrivilegesChanged(
                        mPhone.getPhoneId(),
                        Collections.unmodifiableSet(mPrivilegedPackageInfo.mPackageNames),
                        Collections.unmodifiableSet(mPrivilegedPackageInfo.mUids));
            }

            if (carrierServiceChanged) {
                mTelephonyRegistryManager.notifyCarrierServiceChanged(mPhone.getPhoneId(),
                        mPrivilegedPackageInfo.mCarrierService.first,
                        mPrivilegedPackageInfo.mCarrierService.second);
            }
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }

        // Update set of enabled carrier apps now that the privilege rules may have changed.
        ActivityManager am = mContext.getSystemService(ActivityManager.class);
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(),
                mTelephonyManager, am.getCurrentUser(), mContext);
    }

    @NonNull
    private PrivilegedPackageInfo getCurrentPrivilegedPackagesForAllUsers() {
        Set<String> carrierServiceEligiblePackages = new ArraySet<>();
        Set<String> privilegedPackageNames = new ArraySet<>();
        Set<Integer> privilegedUids = new ArraySet<>();
        for (Map.Entry<String, Set<Integer>> e : mInstalledPackageCertHashes.entrySet()) {
            if (!isPackageMaybePrivileged(e.getKey(), e.getValue())) continue;

            Set<byte[]> fullCerts = getCertsForPackage(e.getKey());

            final int priv = getPackagePrivilegedStatus(e.getKey(), fullCerts);
            switch (priv) {
                case PACKAGE_PRIVILEGED_FROM_SIM:
                case PACKAGE_PRIVILEGED_FROM_CARRIER_SERVICE_TEST_OVERRIDE: // fallthrough
                    carrierServiceEligiblePackages.add(e.getKey());
                    // fallthrough
                case PACKAGE_PRIVILEGED_FROM_CARRIER_CONFIG:
                    privilegedPackageNames.add(e.getKey());
                    privilegedUids.addAll(
                            getUidsForPackage(e.getKey(), /* invalidateCache= */ false));
            }
        }

        return new PrivilegedPackageInfo(
                privilegedPackageNames,
                privilegedUids,
                getCarrierService(carrierServiceEligiblePackages));
    }

    private boolean isPackageMaybePrivileged(
            @NonNull String pkgName, @NonNull Set<Integer> hashHashes) {
        for (Integer hashHash : hashHashes) {
            // Non-null (whether empty or not) test override rule will ignore the UICC and CC rules
            if (mTestOverrideRules != null) {
                for (UiccAccessRule rule : mTestOverrideRules) {
                    if (rule.hasMatchingCertificateHashHashAndPackageName(hashHash, pkgName)) {
                        return true;
                    }
                }
            } else {
                for (UiccAccessRule rule : mUiccRules) {
                    if (rule.hasMatchingCertificateHashHashAndPackageName(hashHash, pkgName)) {
                        return true;
                    }
                }
                for (UiccAccessRule rule : mCarrierConfigRules) {
                    if (rule.hasMatchingCertificateHashHashAndPackageName(hashHash, pkgName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the privilege status of the provided package.
     *
     * <p>Returned privilege status depends on whether a package matches the certificates from
     * carrier config, from test overrides or from certificates stored on the SIM.
     */
    private int getPackagePrivilegedStatus(@NonNull String pkgName, @NonNull Set<byte[]> certs) {
        // Double-nested for loops, but each collection should contain at most 2 elements in nearly
        // every case.
        // TODO(b/184382310) find a way to speed this up
        for (byte[] cert : certs) {
            // Non-null (whether empty or not) test override rule will ignore the UICC and CC rules
            if (mTestOverrideRules != null) {
                for (UiccAccessRule rule : mTestOverrideRules) {
                    if (rule.hasMatchingCertificateHashAndPackageName(cert, pkgName)) {
                        return PACKAGE_PRIVILEGED_FROM_SIM;
                    }
                }
            } else {
                for (UiccAccessRule rule : mUiccRules) {
                    if (rule.hasMatchingCertificateHashAndPackageName(cert, pkgName)) {
                        return PACKAGE_PRIVILEGED_FROM_SIM;
                    }
                }
                for (UiccAccessRule rule : mCarrierConfigRules) {
                    if (rule.hasMatchingCertificateHashAndPackageName(cert, pkgName)) {
                        return pkgName.equals(mTestOverrideCarrierServicePackage)
                                ? PACKAGE_PRIVILEGED_FROM_CARRIER_SERVICE_TEST_OVERRIDE
                                : PACKAGE_PRIVILEGED_FROM_CARRIER_CONFIG;
                    }
                }
            }
        }
        return PACKAGE_NOT_PRIVILEGED;
    }

    @NonNull
    private Set<Integer> getUidsForPackage(@NonNull String pkgName, boolean invalidateCache) {
        if (invalidateCache) {
            mCachedUids.remove(pkgName);
        }
        if (mCachedUids.containsKey(pkgName)) {
            return mCachedUids.get(pkgName);
        }

        Set<Integer> uids = new ArraySet<>(1);
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

    private int getPackageUid(@Nullable String pkgName) {
        int uid = Process.INVALID_UID;
        try {
            uid = mFeatureFlags.supportCarrierServicesForHsum()
                    ? mPackageManager.getPackageUidAsUser(pkgName, ActivityManager.getCurrentUser())
                    : mPackageManager.getPackageUid(pkgName, /* flags= */0);
        } catch (NameNotFoundException e) {
            Rlog.e(TAG, "Unable to find uid for package " + pkgName);
        }
        return uid;
    }

    /**
     * Dump the local log buffer and other internal state of CarrierPrivilegesTracker.
     */
    public void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter pw, @NonNull String[] args) {
        pw.println("CarrierPrivilegesTracker - phoneId: " + mPhone.getPhoneId());
        pw.println("CarrierPrivilegesTracker - Log Begin ----");
        mLocalLog.dump(fd, pw, args);
        pw.println("CarrierPrivilegesTracker - Log End ----");
        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            pw.println(
                    "CarrierPrivilegesTracker - Privileged package info: "
                            + mPrivilegedPackageInfo);
            pw.println("mSimIsReadyButNotLoaded: " + mSimIsReadyButNotLoaded);
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }
        pw.println("CarrierPrivilegesTracker - Test-override rules: " + mTestOverrideRules);
        pw.println("CarrierPrivilegesTracker - SIM-loaded rules: " + mUiccRules);
        pw.println("CarrierPrivilegesTracker - Carrier config rules: " + mCarrierConfigRules);
        if (VDBG) {
            pw.println(
                    "CarrierPrivilegesTracker - Obfuscated Pkgs + Certs: "
                            + getObfuscatedPackages(
                                    mInstalledPackageCertHashes.entrySet(),
                                    e -> "pkg(" + Rlog.pii(TAG, e.getKey()) + ")=" + e.getValue()));
        }
        pw.println("mClearUiccRulesUptimeMillis: " + mClearUiccRulesUptimeMillis);
    }

    /**
     * Set test carrier privilege rules which will override the actual rules on both Carrier Config
     * and SIM.
     *
     * <p>{@code carrierPrivilegeRules} can be null, in which case the rules on the Carrier Config
     * and SIM will be used and any previous overrides will be cleared.
     *
     * @see TelephonyManager#setCarrierTestOverride
     */
    public void setTestOverrideCarrierPrivilegeRules(@Nullable String carrierPrivilegeRules) {
        mCurrentHandler.sendMessage(
                obtainMessage(ACTION_SET_TEST_OVERRIDE_RULE, carrierPrivilegeRules));
    }

    /**
     * Override the carrier provisioning package, if it exists.
     *
     * <p>This API is to be used ONLY for testing, and requires the provided package to be carrier
     * privileged. While this override is set, ONLY the specified package will be considered
     * eligible to be bound as the carrier provisioning package, and any existing bindings will be
     * terminated.
     *
     * @param carrierServicePackage the package to be used as the overridden carrier service
     *     package, or {@code null} to reset override
     * @see TelephonyManager#setCarrierServicePackageOverride
     */
    public void setTestOverrideCarrierServicePackage(@Nullable String carrierServicePackage) {
        mCurrentHandler.sendMessage(obtainMessage(
                ACTION_SET_TEST_OVERRIDE_CARRIER_SERVICE_PACKAGE, carrierServicePackage));
    }

    private void handleSetTestOverrideCarrierServicePackage(
            @Nullable String carrierServicePackage) {
        mTestOverrideCarrierServicePackage = carrierServicePackage;
        refreshInstalledPackageCache();
        maybeUpdatePrivilegedPackagesAndNotifyRegistrants();
    }

    private void handleSetTestOverrideRules(@Nullable String carrierPrivilegeRules) {
        if (carrierPrivilegeRules == null) {
            mTestOverrideRules = null;
        } else if (carrierPrivilegeRules.isEmpty()) {
            mTestOverrideRules = Collections.emptyList();
        } else {
            mTestOverrideRules = Arrays.asList(UiccAccessRule.decodeRulesFromCarrierConfig(
                    new String[]{carrierPrivilegeRules}));
            // TODO(b/215239409): remove the additional cache refresh for test override cases.
            // Test override doesn't respect if the package for the specified cert has been removed
            // or hidden since initialization. Refresh the cache again to get the pkg/uid with the
            // best effort.
            refreshInstalledPackageCache();
        }
        maybeUpdatePrivilegedPackagesAndNotifyRegistrants();
    }

    /** Backing of {@link TelephonyManager#checkCarrierPrivilegesForPackage}. */
    public @CarrierPrivilegeStatus int getCarrierPrivilegeStatusForPackage(
            @Nullable String packageName) {
        if (packageName == null) return TelephonyManager.CARRIER_PRIVILEGE_STATUS_NO_ACCESS;

        // TODO(b/205736323) consider if/how we want to account for the RULES_NOT_LOADED and
        // ERROR_LOADING_RULES constants. Technically those will never be returned today since those
        // results are only from the SIM rules, but the CC rules' result (which never has these
        // errors) always supersede them unless something goes super wrong when getting CC.
        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            if (mSimIsReadyButNotLoaded) {
                return CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
            } else if (mPrivilegedPackageInfo.mPackageNames.contains(packageName)) {
                return CARRIER_PRIVILEGE_STATUS_HAS_ACCESS;
            } else {
                return CARRIER_PRIVILEGE_STATUS_NO_ACCESS;
            }
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }
    }

    /** Backing of {@link TelephonyManager#getPackagesWithCarrierPrivileges}. */
    @NonNull
    public Set<String> getPackagesWithCarrierPrivileges() {
        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            return mSimIsReadyButNotLoaded ? Collections.emptySet() :
                    Collections.unmodifiableSet(mPrivilegedPackageInfo.mPackageNames);
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }
    }

    /**
     * Backing of {@link TelephonyManager#hasCarrierPrivileges} and {@link
     * TelephonyManager#getCarrierPrivilegeStatus(int)}.
     */
    public @CarrierPrivilegeStatus int getCarrierPrivilegeStatusForUid(int uid) {
        // TODO(b/205736323) consider if/how we want to account for the RULES_NOT_LOADED and
        // ERROR_LOADING_RULES constants. Technically those will never be returned today since those
        // results are only from the SIM rules, but the CC rules' result (which never has these
        // errors) always supersede them unless something goes super wrong when getting CC.
        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            if (mSimIsReadyButNotLoaded) {
                return CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
            } else if (mPrivilegedPackageInfo.mUids.contains(uid)) {
                return CARRIER_PRIVILEGE_STATUS_HAS_ACCESS;
            } else {
                return CARRIER_PRIVILEGE_STATUS_NO_ACCESS;
            }
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }
    }

    /**
     * Backing of {@link TelephonyManager#getCarrierServicePackageName()} and
     * {@link TelephonyManager#getCarrierServicePackageNameForLogicalSlot(int)}
     */
    @Nullable
    public String getCarrierServicePackageName() {
        // Return the cached one if present, it is fast and safe (no IPC call to PackageManager)
        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            // If SIM is READY but not LOADED, neither the cache nor the queries below are reliable,
            // we should return null for this transient state for security/privacy's concern.
            if (mSimIsReadyButNotLoaded) return null;

            return mPrivilegedPackageInfo.mCarrierService.first;
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }
        // Do NOT query package manager, mPrivilegedPackageInfo.mCarrierService has maintained the
        // latest CarrierService info. Querying PM will not get better result.
    }

    /**
     * @return The UID of carrier service package. {@link Process#INVALID_UID} if not found.
     */
    public int getCarrierServicePackageUid() {
        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            if (mSimIsReadyButNotLoaded) return Process.INVALID_UID;

            return mPrivilegedPackageInfo.mCarrierService.second;
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }
    }

    /**
     * Backing of {@link TelephonyManager#getCarrierPackageNamesForIntent} and {@link
     * TelephonyManager#getCarrierPackageNamesForIntentAndPhone}.
     */
    @NonNull
    public List<String> getCarrierPackageNamesForIntent(@NonNull Intent intent) {
        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            if (mSimIsReadyButNotLoaded) return Collections.emptyList();
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }

        // Do the PackageManager queries before we take the lock, as these are the longest-running
        // pieces of this method and don't depend on the set of carrier apps.
        List<ResolveInfo> resolveInfos = new ArrayList<>();
        if (mFeatureFlags.supportCarrierServicesForHsum()) {
            resolveInfos.addAll(
                    mPackageManager.queryBroadcastReceiversAsUser(
                            intent, /* flags= */ 0, ActivityManager.getCurrentUser()));
            resolveInfos.addAll(
                    mPackageManager.queryIntentActivitiesAsUser(
                            intent, /* flags= */ 0, ActivityManager.getCurrentUser()));
            resolveInfos.addAll(
                    mPackageManager.queryIntentServicesAsUser(
                            intent, /* flags= */ 0, ActivityManager.getCurrentUser()));
            resolveInfos.addAll(
                    mPackageManager.queryIntentContentProvidersAsUser(
                            intent, /* flags= */ 0, ActivityManager.getCurrentUser()));
        } else {
            resolveInfos.addAll(mPackageManager.queryBroadcastReceivers(intent, 0));
            resolveInfos.addAll(mPackageManager.queryIntentActivities(intent, 0));
            resolveInfos.addAll(mPackageManager.queryIntentServices(intent, 0));
            resolveInfos.addAll(mPackageManager.queryIntentContentProviders(intent, 0));
        }

        // Now actually check which of the resolved packages have carrier privileges.
        mPrivilegedPackageInfoLock.readLock().lock();
        try {
            // Check mSimIsReadyButNotLoaded again here since the PackageManager queries above are
            // pretty time-consuming, mSimIsReadyButNotLoaded state may change since last check
            if (mSimIsReadyButNotLoaded) return Collections.emptyList();

            Set<String> packageNames = new ArraySet<>(); // For deduping purposes
            for (ResolveInfo resolveInfo : resolveInfos) {
                String packageName = getPackageName(resolveInfo);
                if (packageName != null && CARRIER_PRIVILEGE_STATUS_HAS_ACCESS
                        == getCarrierPrivilegeStatusForPackage(packageName)) {
                    packageNames.add(packageName);
                }
            }
            return new ArrayList<>(packageNames);
        } finally {
            mPrivilegedPackageInfoLock.readLock().unlock();
        }
    }

    @Nullable
    private static String getPackageName(@NonNull ResolveInfo resolveInfo) {
        // Note: activityInfo covers both activities + broadcast receivers
        if (resolveInfo.activityInfo != null) return resolveInfo.activityInfo.packageName;
        if (resolveInfo.serviceInfo != null) return resolveInfo.serviceInfo.packageName;
        if (resolveInfo.providerInfo != null) return resolveInfo.providerInfo.packageName;
        return null;
    }

    @NonNull
    private Pair<String, Integer> getCarrierService(@NonNull Set<String> simPrivilegedPackages) {
        List<ResolveInfo> carrierServiceResolveInfos =
                mFeatureFlags.supportCarrierServicesForHsum()
                        ? mPackageManager.queryIntentServicesAsUser(
                                new Intent(CarrierService.CARRIER_SERVICE_INTERFACE),
                                /* flags= */ 0,
                                ActivityManager.getCurrentUser())
                        : mPackageManager.queryIntentServices(
                                new Intent(CarrierService.CARRIER_SERVICE_INTERFACE),
                                /* flags= */ 0);
        String carrierServicePackageName = null;
        for (ResolveInfo resolveInfo : carrierServiceResolveInfos) {
            String packageName = getPackageName(resolveInfo);
            if (mTestOverrideCarrierServicePackage != null
                    && !mTestOverrideCarrierServicePackage.equals(packageName)) {
                continue;
            }

            if (simPrivilegedPackages.contains(packageName)) {
                carrierServicePackageName = packageName;
                break;
            }
        }
        return carrierServicePackageName == null
                ? new Pair<>(null, Process.INVALID_UID)
                : new Pair<>(carrierServicePackageName, getPackageUid(carrierServicePackageName));
    }

    private @PackageManager.EnabledState int getApplicationEnabledSetting(
            @NonNull String packageName) {
        if (mFeatureFlags.supportCarrierServicesForHsum()) {
            return mContext.createContextAsUser(
                            UserHandle.of(ActivityManager.getCurrentUser()), /* flags= */ 0)
                    .getPackageManager()
                    .getApplicationEnabledSetting(packageName);
        } else {
            return mPackageManager.getApplicationEnabledSetting(packageName);
        }
    }
}
