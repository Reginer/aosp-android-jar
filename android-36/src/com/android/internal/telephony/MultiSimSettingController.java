/*
 * Copyright 2019 The Android Open Source Project
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

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;
import static android.telephony.SubscriptionManager.PROFILE_CLASS_PROVISIONING;
import static android.telephony.SubscriptionManager.TRANSFER_STATUS_CONVERTED;
import static android.telephony.TelephonyManager.ACTION_PRIMARY_SUBSCRIPTION_LIST_CHANGED;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_ALL;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DATA;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DISMISS;
import static android.telephony.TelephonyManager.EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_NONE;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_NAMES;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_WARNING_TYPE;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_WARNING_TYPE_DUAL_CDMA;
import static android.telephony.TelephonyManager.EXTRA_SIM_COMBINATION_WARNING_TYPE_NONE;
import static android.telephony.TelephonyManager.EXTRA_SUBSCRIPTION_ID;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.data.DataSettingsManager.DataSettingsManagerCallback;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.satellite.SatelliteController;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.util.ArrayUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * This class will make sure below setting rules are coordinated across different subscriptions
 * and phones in multi-SIM case:
 *
 * 1) Grouped subscriptions will have same settings for MOBILE_DATA and DATA_ROAMING.
 * 2) Default settings updated automatically. It may be cleared or inherited within group.
 *    If default subscription A switches to profile B which is in the same group, B will
 *    become the new default.
 * 3) For primary subscriptions, only default data subscription will have MOBILE_DATA on.
 */
public class MultiSimSettingController extends Handler {
    private static final String LOG_TAG = "MultiSimSettingController";
    private static final boolean DBG = true;
    private static final int EVENT_USER_DATA_ENABLED                 = 1;
    private static final int EVENT_ROAMING_DATA_ENABLED              = 2;
    private static final int EVENT_ALL_SUBSCRIPTIONS_LOADED          = 3;
    private static final int EVENT_SUBSCRIPTION_INFO_CHANGED         = 4;
    private static final int EVENT_SUBSCRIPTION_GROUP_CHANGED        = 5;
    private static final int EVENT_DEFAULT_DATA_SUBSCRIPTION_CHANGED = 6;
    @VisibleForTesting
    public static final int EVENT_MULTI_SIM_CONFIG_CHANGED          = 8;
    @VisibleForTesting
    public static final int EVENT_RADIO_STATE_CHANGED                = 9;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"PRIMARY_SUB_"},
            value = {
                    PRIMARY_SUB_NO_CHANGE,
                    PRIMARY_SUB_ADDED,
                    PRIMARY_SUB_REMOVED,
                    PRIMARY_SUB_SWAPPED,
                    PRIMARY_SUB_SWAPPED_IN_GROUP,
                    PRIMARY_SUB_MARKED_OPPT,
                    PRIMARY_SUB_INITIALIZED,
                    PRIMARY_SUB_REMOVED_IN_GROUP
    })
    private @interface PrimarySubChangeType {}

    // Primary subscription not change.
    private static final int PRIMARY_SUB_NO_CHANGE              = 0;
    // One or more primary subscriptions are activated.
    private static final int PRIMARY_SUB_ADDED                  = 1;
    // One or more primary subscriptions are deactivated.
    private static final int PRIMARY_SUB_REMOVED                = 2;
    // One or more primary subscriptions are swapped.
    private static final int PRIMARY_SUB_SWAPPED                = 3;
    // One or more primary subscriptions are swapped but within same sub group.
    private static final int PRIMARY_SUB_SWAPPED_IN_GROUP       = 4;
    // One or more primary subscriptions are marked as opportunistic.
    private static final int PRIMARY_SUB_MARKED_OPPT            = 5;
    // Subscription information is initially loaded.
    private static final int PRIMARY_SUB_INITIALIZED            = 6;
    // One or more primary subscriptions are deactivated but within the same group as another active
    // sub.
    private static final int PRIMARY_SUB_REMOVED_IN_GROUP       = 7;

    protected final Context mContext;
    private final SubscriptionManagerService mSubscriptionManagerService;
    private final @NonNull FeatureFlags mFeatureFlags;

    // Keep a record of active primary (non-opportunistic) subscription list.
    @NonNull private List<Integer> mPrimarySubList = new ArrayList<>();

    /** The singleton instance. */
    protected static MultiSimSettingController sInstance = null;

    // This will be set true when handling EVENT_ALL_SUBSCRIPTIONS_LOADED.
    private boolean mSubInfoInitialized = false;

    // mInitialHandling is to make sure we don't always ask user to re-select data SIM after reboot.
    // After boot-up when things are firstly initialized (mSubInfoInitialized is changed to true
    // and carrier configs are all loaded), we do a reEvaluateAll(). In the first reEvaluateAll(),
    // mInitialHandling will be true and we won't pop up SIM select dialog.
    private boolean mInitialHandling = true;

    // Keep a record of which subIds have carrier config loaded. Length of the array is phone count.
    // The index is phoneId, and value is subId. For example:
    // If carrier config of subId 2 is loaded on phone 0,mCarrierConfigLoadedSubIds[0] = 2.
    // Then if subId 2 is deactivated from phone 0, the value becomes INVALID,
    // mCarrierConfigLoadedSubIds[0] = INVALID_SUBSCRIPTION_ID.
    private int[] mCarrierConfigLoadedSubIds;

    // It indicates whether "Ask every time" option for default SMS subscription is supported by the
    // device.
    private final boolean mIsAskEverytimeSupportedForSms;

    // The number of existing DataSettingsControllerCallback
    private int mCallbacksCount;
    /** The number of active modem count. */
    private int mActiveModemCount;

    private boolean mNeedSetDefaultVoice;
    private boolean mNeedSetDefaultSms;
    private boolean mNeedSetDefaultData;
    private int mConvertedPsimSubId;

    private static final String SETTING_USER_PREF_DATA_SUB = "user_preferred_data_sub";

    private static class DataSettingsControllerCallback extends DataSettingsManagerCallback {
        private final Phone mPhone;

        DataSettingsControllerCallback(@NonNull Phone phone,
                @NonNull @CallbackExecutor Executor executor) {
            super(executor);
            mPhone = phone;
        }

        @Override
        public void onUserDataEnabledChanged(boolean enabled, @NonNull String callingPackage) {
            int subId = mPhone.getSubId();
            // only notifyUserDataEnabled if the change is called from external to avoid
            // setUserDataEnabledForGroup infinite loop
            if (SubscriptionManager.isValidSubscriptionId(subId)
                    && !getInstance().mContext.getOpPackageName().equals(callingPackage)) {
                getInstance().notifyUserDataEnabled(subId, enabled);
            }
        }

        @Override
        public void onDataRoamingEnabledChanged(boolean enabled) {
            int subId = mPhone.getSubId();
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                getInstance().notifyRoamingDataEnabled(mPhone.getSubId(), enabled);
            }
        }
    }

    /**
     * Return the singleton or create one if not existed.
     */
    public static MultiSimSettingController getInstance() {
        synchronized (MultiSimSettingController.class) {
            if (sInstance == null) {
                Log.wtf(LOG_TAG, "getInstance null");
            }

            return sInstance;
        }
    }

    /**
     * Init instance of MultiSimSettingController.
     */
    public static MultiSimSettingController init(Context context,
            @NonNull FeatureFlags featureFlags) {
        synchronized (MultiSimSettingController.class) {
            if (sInstance == null) {
                sInstance = new MultiSimSettingController(context, featureFlags);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    @VisibleForTesting
    public MultiSimSettingController(Context context, @NonNull FeatureFlags featureFlags) {
        mContext = context;
        mSubscriptionManagerService = SubscriptionManagerService.getInstance();
        mFeatureFlags = featureFlags;

        // Initialize mCarrierConfigLoadedSubIds and register to listen to carrier config change.
        TelephonyManager telephonyManager = ((TelephonyManager) mContext.getSystemService(
                TelephonyManager.class));
        final int phoneCount = telephonyManager.getSupportedModemCount();
        mCarrierConfigLoadedSubIds = new int[phoneCount];
        Arrays.fill(mCarrierConfigLoadedSubIds, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mActiveModemCount = telephonyManager.getActiveModemCount();

        PhoneConfigurationManager.registerForMultiSimConfigChange(
                this, EVENT_MULTI_SIM_CONFIG_CHANGED, null);

        mIsAskEverytimeSupportedForSms = mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_sms_ask_every_time_support);

        CarrierConfigManager ccm = mContext.getSystemService(CarrierConfigManager.class);
        // Listener callback is executed on handler thread to directly handle config change
        if (ccm != null) {
            ccm.registerCarrierConfigChangeListener(this::post,
                    (slotIndex, subId, carrierId, specificCarrierId) ->
                            onCarrierConfigChanged(slotIndex, subId));
        }

        mConvertedPsimSubId = getConvertedPsimSubscriptionId();
    }

    private boolean hasCalling() {
        if (!TelephonyCapabilities.minimalTelephonyCdmCheck(mFeatureFlags)) return true;
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_CALLING);
    }

    private boolean hasData() {
        if (!TelephonyCapabilities.minimalTelephonyCdmCheck(mFeatureFlags)) return true;
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_DATA);
    }

    private boolean hasMessaging() {
        if (!TelephonyCapabilities.minimalTelephonyCdmCheck(mFeatureFlags)) return true;
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_MESSAGING);
    }

    /**
     * Notify MOBILE_DATA of a subscription is changed.
     */
    public void notifyUserDataEnabled(int subId, boolean enable) {
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            obtainMessage(EVENT_USER_DATA_ENABLED, subId, enable ? 1 : 0).sendToTarget();
        }
    }

    /**
     * Notify DATA_ROAMING of a subscription is changed.
     */
    public void notifyRoamingDataEnabled(int subId, boolean enable) {
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            obtainMessage(EVENT_ROAMING_DATA_ENABLED, subId, enable ? 1 : 0).sendToTarget();
        }
    }

    /**
     * Notify that, for the first time after boot, SIMs are initialized.
     * Should only be triggered once.
     */
    public void notifyAllSubscriptionLoaded() {
        obtainMessage(EVENT_ALL_SUBSCRIPTIONS_LOADED).sendToTarget();
    }

    /**
     * Notify subscription info change.
     */
    public void notifySubscriptionInfoChanged() {
        log("notifySubscriptionInfoChanged");
        obtainMessage(EVENT_SUBSCRIPTION_INFO_CHANGED).sendToTarget();
    }

    /**
     * Notify subscription group information change.
     */
    public void notifySubscriptionGroupChanged(ParcelUuid groupUuid) {
        obtainMessage(EVENT_SUBSCRIPTION_GROUP_CHANGED, groupUuid).sendToTarget();
    }

    /**
     * Notify default data subscription change.
     */
    public void notifyDefaultDataSubChanged() {
        obtainMessage(EVENT_DEFAULT_DATA_SUBSCRIPTION_CHANGED).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_USER_DATA_ENABLED: {
                int subId = msg.arg1;
                boolean enable = msg.arg2 != 0;
                onUserDataEnabled(subId, enable, true);
                break;
            }
            case EVENT_ROAMING_DATA_ENABLED: {
                int subId = msg.arg1;
                boolean enable = msg.arg2 != 0;
                onRoamingDataEnabled(subId, enable);
                break;
            }
            case EVENT_ALL_SUBSCRIPTIONS_LOADED:
                onAllSubscriptionsLoaded();
                break;
            case EVENT_SUBSCRIPTION_INFO_CHANGED:
                onSubscriptionsChanged();
                break;
            case EVENT_SUBSCRIPTION_GROUP_CHANGED:
                ParcelUuid groupUuid = (ParcelUuid) msg.obj;
                onSubscriptionGroupChanged(groupUuid);
                break;
            case EVENT_DEFAULT_DATA_SUBSCRIPTION_CHANGED:
                onDefaultDataSettingChanged();
                break;
            case EVENT_MULTI_SIM_CONFIG_CHANGED:
                int activeModems = (int) ((AsyncResult) msg.obj).result;
                onMultiSimConfigChanged(activeModems);
                break;
            case EVENT_RADIO_STATE_CHANGED:
                for (int phoneId = 0; phoneId < mActiveModemCount; phoneId++) {
                    Phone phone = PhoneFactory.getPhone(phoneId);
                    if (phone != null && phone.mCi.getRadioState()
                            == TelephonyManager.RADIO_POWER_UNAVAILABLE) {
                        if (DBG) {
                            log("Radio unavailable on phone " + phoneId
                                    + ", clearing sub info initialized flag");
                        }
                        mSubInfoInitialized = false;
                        break;
                    }
                }
                break;
        }
    }

    /**
     * Make sure MOBILE_DATA of subscriptions in same group are synced.
     *
     * If user is enabling a non-default non-opportunistic subscription, make it default
     * data subscription.
     */
    private void onUserDataEnabled(int subId, boolean enable, boolean setDefaultData) {
        if (DBG) log("[onUserDataEnabled] subId=" + subId + " enable=" + enable +
        " setDefaultData=" + setDefaultData);
        // Make sure MOBILE_DATA of subscriptions in same group are synced.
        setUserDataEnabledForGroup(subId, enable);

        SubscriptionInfo subInfo = mSubscriptionManagerService.getSubscriptionInfo(subId);
        int defaultDataSubId = mSubscriptionManagerService.getDefaultDataSubId();

        // If user is enabling a non-default non-opportunistic subscription, make it default.
        if (defaultDataSubId != subId && subInfo != null && !subInfo.isOpportunistic() && enable
                && subInfo.isActive() && setDefaultData) {
            android.provider.Settings.Global.putInt(mContext.getContentResolver(),
                    SETTING_USER_PREF_DATA_SUB, subId);
            mSubscriptionManagerService.setDefaultDataSubId(subId);
        }
    }

    /**
     * Make sure DATA_ROAMING of subscriptions in same group are synced.
     */
    private void onRoamingDataEnabled(int subId, boolean enable) {
        if (DBG) log("onRoamingDataEnabled");
        setRoamingDataEnabledForGroup(subId, enable);

        // Also inform SubscriptionManagerService as it keeps another copy of user setting.
        mSubscriptionManagerService.setDataRoaming(enable ? 1 : 0, subId);
    }

    /**
     * Upon initialization or radio available, update defaults and mobile data enabling.
     * Should only be triggered once.
     */
    private void onAllSubscriptionsLoaded() {
        if (DBG) log("onAllSubscriptionsLoaded: mSubInfoInitialized=" + mSubInfoInitialized);
        if (!mSubInfoInitialized) {
            mSubInfoInitialized = true;
            mConvertedPsimSubId = getConvertedPsimSubscriptionId();
            for (Phone phone : PhoneFactory.getPhones()) {
                phone.mCi.registerForRadioStateChanged(this, EVENT_RADIO_STATE_CHANGED, null);
            }
            reEvaluateAll();
        }
        registerDataSettingsControllerCallbackAsNeeded();
    }

    /**
     * Make sure default values are cleaned or updated.
     *
     * Make sure non-default non-opportunistic subscriptions has data off.
     */
    private void onSubscriptionsChanged() {
        if (DBG) log("onSubscriptionsChanged");
        reEvaluateAll();
    }

    /**
     * This method is called when a phone object is removed (for example when going from multi-sim
     * to single-sim).
     * NOTE: This method does not post a message to self, instead it calls reEvaluateAll() directly.
     * so it should only be called from the main thread. The reason is to update defaults asap
     * after multi_sim_config property has been updated (see b/163582235).
     */
    public void onPhoneRemoved() {
        if (DBG) log("onPhoneRemoved");
        if (Looper.myLooper() != this.getLooper()) {
            throw new RuntimeException("This method must be called from the same looper as "
                    + "MultiSimSettingController.");
        }
        reEvaluateAll();
    }

    private void onCarrierConfigChanged(int phoneId, int subId) {
        log("onCarrierConfigChanged phoneId " + phoneId + " subId " + subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("Carrier config change with invalid phoneId " + phoneId);
            return;
        }

        CarrierConfigManager cm;
        if (!SubscriptionManager.isValidSubscriptionId(subId) // record SIM absent.
                || ((cm = mContext.getSystemService(CarrierConfigManager.class)) != null
                && CarrierConfigManager.isConfigForIdentifiedCarrier(
                        cm.getConfigForSubId(subId)))) {
            mCarrierConfigLoadedSubIds[phoneId] = subId;
            reEvaluateAll();
        }
    }

    /**
     * Check whether carrier config loaded for all subs
     */
    @VisibleForTesting
    public boolean isCarrierConfigLoadedForAllSub() {
        int[] activeSubIds = mSubscriptionManagerService.getActiveSubIdList(false);
        for (int activeSubId : activeSubIds) {
            boolean isLoaded = false;
            for (int configLoadedSub : mCarrierConfigLoadedSubIds) {
                if (configLoadedSub == activeSubId) {
                    isLoaded = true;
                    break;
                }
            }
            if (!isLoaded) {
                if (DBG) log("Carrier config subId " + activeSubId + " is not loaded.");
                return false;
            }
        }

        return true;
    }

    private void onMultiSimConfigChanged(int activeModems) {
        mActiveModemCount = activeModems;
        log("onMultiSimConfigChanged: current ActiveModemCount=" + mActiveModemCount);
        // Clear mCarrierConfigLoadedSubIds. Other actions will responds to active
        // subscription change.
        for (int phoneId = activeModems; phoneId < mCarrierConfigLoadedSubIds.length; phoneId++) {
            mCarrierConfigLoadedSubIds[phoneId] = INVALID_SUBSCRIPTION_ID;
        }
        for (Phone phone : PhoneFactory.getPhones()) {
            phone.mCi.registerForRadioStateChanged(this, EVENT_RADIO_STATE_CHANGED, null);
        }
        registerDataSettingsControllerCallbackAsNeeded();
    }

    /**
     * Wait for subInfo initialization (after boot up or radio unavailable) and carrier config load
     * for all active subscriptions before re-evaluate multi SIM settings.
     */
    private boolean isReadyToReevaluate() {
        boolean carrierConfigsLoaded = isCarrierConfigLoadedForAllSub();
        SatelliteController satelliteController = SatelliteController.getInstance();
        boolean isSatelliteEnabledOrBeingEnabled = false;
        if (satelliteController != null) {
            isSatelliteEnabledOrBeingEnabled =
                    satelliteController.isSatelliteEnabledOrBeingEnabled();
        }

        if (DBG) {
            log("isReadyToReevaluate: subInfoInitialized=" + mSubInfoInitialized
                    + ", carrierConfigsLoaded=" + carrierConfigsLoaded
                    + ", satelliteEnabledOrBeingEnabled=" + isSatelliteEnabledOrBeingEnabled);
        }
        return mSubInfoInitialized && carrierConfigsLoaded
                && !isSatelliteEnabledOrBeingEnabled;
    }

    private void reEvaluateAll() {
        if (!isReadyToReevaluate()) return;
        updateDefaults();
        disableDataForNonDefaultNonOpportunisticSubscriptions();
        deactivateGroupedOpportunisticSubscriptionIfNeeded();
    }

    /**
     * Make sure non-default non-opportunistic subscriptions has data disabled.
     */
    private void onDefaultDataSettingChanged() {
        if (DBG) log("onDefaultDataSettingChanged");
        disableDataForNonDefaultNonOpportunisticSubscriptions();
    }

    /**
     * When a subscription group is created or new subscriptions are added in the group, make
     * sure the settings among them are synced.
     * TODO: b/130258159 have a separate database table for grouped subscriptions so we don't
     * manually sync each setting.
     */
    private void onSubscriptionGroupChanged(ParcelUuid groupUuid) {
        if (DBG) log("onSubscriptionGroupChanged");

        List<SubscriptionInfo> infoList = mSubscriptionManagerService.getSubscriptionsInGroup(
                groupUuid, mContext.getOpPackageName(), mContext.getAttributionTag());
        if (infoList == null || infoList.isEmpty()) return;

        // Get a reference subscription to copy settings from.
        // TODO: the reference sub should be passed in from external caller.
        int refSubId = infoList.get(0).getSubscriptionId();
        for (SubscriptionInfo info : infoList) {
            int subId = info.getSubscriptionId();
            if (info.isActive() && !info.isOpportunistic()) {
                refSubId = subId;
                break;
            }
        }

        if (DBG) log("refSubId is " + refSubId);

        boolean enable = false;
        try {
            enable = GlobalSettingsHelper.getBoolean(
                    mContext, Settings.Global.MOBILE_DATA, refSubId);
        } catch (SettingNotFoundException exception) {
            //pass invalid refSubId to fetch the single-sim setting
            enable = GlobalSettingsHelper.getBoolean(
                    mContext, Settings.Global.MOBILE_DATA, INVALID_SUBSCRIPTION_ID, enable);
        }
        boolean setDefaultData = true;
        List<SubscriptionInfo> activeSubList = mSubscriptionManagerService
                .getActiveSubscriptionInfoList(mContext.getOpPackageName(),
                        mContext.getAttributionTag(), true/*isForAllProfile*/);
        for (SubscriptionInfo activeInfo : activeSubList) {
            if (!(groupUuid.equals(activeInfo.getGroupUuid()))) {
                // Do not set refSubId as defaultDataSubId if there are other active
                // subscriptions which does not belong to this groupUuid
                setDefaultData = false;
                break;
            }
        }
        onUserDataEnabled(refSubId, enable, setDefaultData);

        enable = false;
        try {
            enable = GlobalSettingsHelper.getBoolean(
                    mContext, Settings.Global.DATA_ROAMING, refSubId);
            onRoamingDataEnabled(refSubId, enable);
        } catch (SettingNotFoundException exception) {
            //pass invalid refSubId to fetch the single-sim setting
            enable = GlobalSettingsHelper.getBoolean(
                    mContext, Settings.Global.DATA_ROAMING, INVALID_SUBSCRIPTION_ID, enable);
            onRoamingDataEnabled(refSubId, enable);
        }

        mSubscriptionManagerService.syncGroupedSetting(refSubId);
    }

    /**
     * Automatically update default settings (data / voice / sms).
     *
     * Opportunistic subscriptions can't be default data / voice / sms subscription.
     *
     * 1) If the default subscription is still active, keep it unchanged.
     * 2) Or if there's another active primary subscription that's in the same group,
     *    make it the new default value.
     * 3) Or if there's only one active primary subscription, automatically set default
     *    data subscription on it. Because default data in Android Q is an internal value,
     *    not a user settable value anymore.
     * 4) If non above is met, clear the default value to INVALID.
     *
     */
    protected void updateDefaults() {
        if (DBG) log("updateDefaults");
        if (!isReadyToReevaluate()) return;

        List<SubscriptionInfo> activeSubInfos = mSubscriptionManagerService
                .getActiveSubscriptionInfoList(mContext.getOpPackageName(),
                        mContext.getAttributionTag(), true/*isForAllProfile*/);

        if (ArrayUtils.isEmpty(activeSubInfos)) {
            mPrimarySubList.clear();
            if (DBG) log("updateDefaults: No active sub. Setting default to INVALID sub.");
            mSubscriptionManagerService.setDefaultDataSubId(
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mSubscriptionManagerService.setDefaultVoiceSubId(
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mSubscriptionManagerService.setDefaultSmsSubId(
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            return;
        }

        int change = updatePrimarySubListAndGetChangeType(activeSubInfos);
        if (DBG) log("updateDefaultValues: change: " + change);
        if (change == PRIMARY_SUB_NO_CHANGE) return;

        // If there's only one primary subscription active, we trigger mobile data
        // dialog if and only if there were multiple primary SIM cards and one is removed.
        // Otherwise, if user just inserted their first SIM, or there's one primary and one
        // opportunistic subscription active (activeSubInfos.size() > 1), we automatically
        // set the primary to be default SIM and return.
        boolean conditionForOnePrimarySim =
                mFeatureFlags.resetPrimarySimDefaultValues() ? mPrimarySubList.size() == 1
                        : mPrimarySubList.size() == 1
                        && (change != PRIMARY_SUB_REMOVED || mActiveModemCount == 1);
        if (conditionForOnePrimarySim) {
            int subId = mPrimarySubList.get(0);
            if (DBG) log("updateDefaultValues: to only primary sub " + subId);
            if (hasData()) mSubscriptionManagerService.setDefaultDataSubId(subId);
            if (hasCalling()) mSubscriptionManagerService.setDefaultVoiceSubId(subId);
            if (hasMessaging()) mSubscriptionManagerService.setDefaultSmsSubId(subId);
            if (!mSubscriptionManagerService.isEsimBootStrapProvisioningActivated()) {
                // Determines the appropriate notification type
                // Preconditions:
                // - There is only one active primary subscription.
                // - The eSIM bootstrap is NOT activated.
                // Behavior:
                // - If the primary subscription is not deactivated OR the device is in single SIM
                //   mode, send a notification to dismiss the SIM dialog.
                // - Otherwise, send a notification to trigger the preferred SIM/data pick dialog.
                @TelephonyManager.DefaultSubscriptionSelectType
                int type = (change != PRIMARY_SUB_REMOVED || mActiveModemCount == 1)
                        ? EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DISMISS
                        : EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_ALL;
                sendDefaultSubConfirmedNotification(type, subId);
            }
            return;
        }

        if (DBG) log("updateDefaultValues: records: " + mPrimarySubList);

        boolean dataSelected = false;
        boolean voiceSelected = false;
        boolean smsSelected = false;

        if (hasData()) {
            // Update default data subscription.
            if (DBG) log("updateDefaultValues: Update default data subscription");
            dataSelected = updateDefaultValue(mPrimarySubList,
                    mSubscriptionManagerService.getDefaultDataSubId(),
                    mSubscriptionManagerService::setDefaultDataSubId);
        }

        if (hasCalling()) {
            // Update default voice subscription.
            if (DBG) log("updateDefaultValues: Update default voice subscription");
            voiceSelected = updateDefaultValue(mPrimarySubList,
                    mSubscriptionManagerService.getDefaultVoiceSubId(),
                    mSubscriptionManagerService::setDefaultVoiceSubId);
        }

        if (hasMessaging()) {
            // Update default sms subscription.
            if (DBG) log("updateDefaultValues: Update default sms subscription");
            smsSelected = updateDefaultValue(mPrimarySubList,
                    mSubscriptionManagerService.getDefaultSmsSubId(),
                    mSubscriptionManagerService::setDefaultSmsSubId,
                    mIsAskEverytimeSupportedForSms);
        }

        boolean autoFallbackEnabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_data_sms_auto_fallback);

        // Based on config config_voice_data_sms_auto_fallback value choose voice/data/sms
        // preference auto selection logic or display notification for end used to
        // select voice/data/SMS preferences.
        if (!autoFallbackEnabled) {
            // Hide the dialog for preferred SIM/data pick if the primary subscription change is
            // due to the pSIM conversion.
            if (!setDefaultForPsimConversionChanged(change, dataSelected, voiceSelected,
                    smsSelected)) {
                sendSubChangeNotificationIfNeeded(change, dataSelected, voiceSelected, smsSelected);
            }
        } else {
            updateUserPreferences(mPrimarySubList, dataSelected, voiceSelected, smsSelected);
        }
    }

    @PrimarySubChangeType
    private int updatePrimarySubListAndGetChangeType(List<SubscriptionInfo> activeSubList) {
        // Update mPrimarySubList. Opportunistic subscriptions can't be default
        // data / voice / sms subscription.
        List<Integer> prevPrimarySubList = mPrimarySubList;
        mPrimarySubList = activeSubList.stream()
                .filter(info -> !info.isOpportunistic())
                .filter(info -> info.getProfileClass() != PROFILE_CLASS_PROVISIONING)
                .map(info -> info.getSubscriptionId())
                .collect(Collectors.toList());

        if (mInitialHandling) {
            mInitialHandling = false;
            return PRIMARY_SUB_INITIALIZED;
        }
        if (mPrimarySubList.equals(prevPrimarySubList)) return PRIMARY_SUB_NO_CHANGE;
        if (mPrimarySubList.size() > prevPrimarySubList.size()) return PRIMARY_SUB_ADDED;

        if (mPrimarySubList.size() == prevPrimarySubList.size()) {
            // We need to differentiate PRIMARY_SUB_SWAPPED and PRIMARY_SUB_SWAPPED_IN_GROUP:
            // For SWAPPED_IN_GROUP, we never pop up dialog to ask data sub selection again.
            for (int subId : mPrimarySubList) {
                boolean swappedInSameGroup = false;
                for (int prevSubId : prevPrimarySubList) {
                    if (areSubscriptionsInSameGroup(subId, prevSubId)) {
                        swappedInSameGroup = true;
                        break;
                    }
                }
                if (!swappedInSameGroup) return PRIMARY_SUB_SWAPPED;
            }
            return PRIMARY_SUB_SWAPPED_IN_GROUP;
        } else /* mPrimarySubList.size() < prevPrimarySubList.size() */ {
            // We need to differentiate whether the missing subscription is removed or marked as
            // opportunistic. Usually only one subscription may change at a time, But to be safe, if
            // any previous primary subscription becomes inactive, we consider it
            for (int subId : prevPrimarySubList) {
                if (mPrimarySubList.contains(subId)) continue;
                SubscriptionInfo subInfo = mSubscriptionManagerService.getSubscriptionInfo(subId);

                if (subInfo == null || !subInfo.isActive()) {
                    for (int currentSubId : mPrimarySubList) {
                        if (areSubscriptionsInSameGroup(currentSubId, subId)) {
                            return PRIMARY_SUB_REMOVED_IN_GROUP;
                        }
                    }
                    return PRIMARY_SUB_REMOVED;
                }
                if (!subInfo.isOpportunistic()) {
                    // Should never happen.
                    loge("[updatePrimarySubListAndGetChangeType]: missing active primary "
                            + "subId " + subId);
                }
            }
            return PRIMARY_SUB_MARKED_OPPT;
        }
    }

    private void sendDefaultSubConfirmedNotification(
            @TelephonyManager.DefaultSubscriptionSelectType int type, int defaultSubId) {
        Intent intent = new Intent();
        intent.setAction(ACTION_PRIMARY_SUBSCRIPTION_LIST_CHANGED);
        intent.setClassName("com.android.settings",
                "com.android.settings.sim.SimSelectNotification");

        intent.putExtra(EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE, type);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, defaultSubId);

        if (mFeatureFlags.hsumBroadcast()) {
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            mContext.sendBroadcast(intent);
        }
    }

    private void sendSubChangeNotificationIfNeeded(int change, boolean dataSelected,
            boolean voiceSelected, boolean smsSelected) {

        if (mSubscriptionManagerService.isEsimBootStrapProvisioningActivated()) {
            log("esim bootstrap activation in progress, skip notification");
            return;
        }

        @TelephonyManager.DefaultSubscriptionSelectType
        int simSelectDialogType = getSimSelectDialogType(
                change, dataSelected, voiceSelected, smsSelected);
        log("sendSubChangeNotificationIfNeeded: simSelectDialogType=" + simSelectDialogType);
        SimCombinationWarningParams simCombinationParams = getSimCombinationWarningParams(change);

        if (simSelectDialogType != EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_NONE
                || simCombinationParams.mWarningType != EXTRA_SIM_COMBINATION_WARNING_TYPE_NONE) {
            log("[sendSubChangeNotificationIfNeeded] showing dialog type "
                    + simSelectDialogType);
            log("[sendSubChangeNotificationIfNeeded] showing sim warning "
                    + simCombinationParams.mWarningType);
            Intent intent = new Intent();
            intent.setAction(ACTION_PRIMARY_SUBSCRIPTION_LIST_CHANGED);
            intent.setClassName("com.android.settings",
                    "com.android.settings.sim.SimSelectNotification");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.putExtra(EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE, simSelectDialogType);
            if (simSelectDialogType == EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_ALL) {
                intent.putExtra(EXTRA_SUBSCRIPTION_ID, mPrimarySubList.get(0));
            }

            intent.putExtra(EXTRA_SIM_COMBINATION_WARNING_TYPE, simCombinationParams.mWarningType);
            if (simCombinationParams.mWarningType == EXTRA_SIM_COMBINATION_WARNING_TYPE_DUAL_CDMA) {
                intent.putExtra(EXTRA_SIM_COMBINATION_NAMES, simCombinationParams.mSimNames);
            }
            if (mFeatureFlags.hsumBroadcast()) {
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            } else {
                mContext.sendBroadcast(intent);
            }
        }
    }

    /**
     * Check that the primary subscription has changed due to the pSIM conversion.
     * @param change Whether to update the mPrimarySubList.
     * @param dataSelected Whether the default data subscription is updated
     * @param voiceSelected Whether the default voice subscription is updated
     * @param smsSelected Whether the default sms subscription is updated
     * @return {@code true} if the primary subscription has changed due to the pSIM conversion,
     * {@code false} otherwise.
     */
    private boolean setDefaultForPsimConversionChanged(int change, boolean dataSelected,
            boolean voiceSelected, boolean smsSelected) {
        if (!mFeatureFlags.supportPsimToEsimConversion()) {
            log("pSIM to eSIM conversion is not supported");
            return false;
        }
        if (mSubscriptionManagerService.isEsimBootStrapProvisioningActivated()) {
            log("esim bootstrap activation in progress, skip notification");
            return false;
        }

        @TelephonyManager.DefaultSubscriptionSelectType
        int simSelectDialogType = getSimSelectDialogType(
                change, dataSelected, voiceSelected, smsSelected);
        SimCombinationWarningParams simCombinationParams = getSimCombinationWarningParams(change);
        log("[setDefaultForPsimConversionChanged]showing dialog type:" + simSelectDialogType);
        if (simSelectDialogType != EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_NONE
                || simCombinationParams.mWarningType != EXTRA_SIM_COMBINATION_WARNING_TYPE_NONE) {
            log("[setDefaultForPsimConversionChanged]Converted pSIM:" + mConvertedPsimSubId);
            int subId = getConvertedPsimSubscriptionId();
            if (subId != INVALID_SUBSCRIPTION_ID && subId != mConvertedPsimSubId) {
                // If a primary subscription is removed and only one is left active, ask user
                // for preferred sub selection if any default setting is not set.
                if (simSelectDialogType == EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_ALL) {
                    // check if pSIM's preference is voice.
                    if (mSubscriptionManagerService.getDefaultVoiceSubId()
                            == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        mNeedSetDefaultVoice = true;
                    }
                    // check if pSIM's preference is sms.
                    if (mSubscriptionManagerService.getDefaultSmsSubId()
                            == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        mNeedSetDefaultSms = true;
                    }
                    // check if pSIM's preference is data.
                    if (mSubscriptionManagerService.getDefaultDataSubId()
                            == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        mNeedSetDefaultData = true;
                    }
                    log("select type all, set preferred SIM :" + mPrimarySubList.get(0));
                    mSubscriptionManagerService.setDefaultVoiceSubId(mPrimarySubList.get(0));
                    mSubscriptionManagerService.setDefaultSmsSubId(mPrimarySubList.get(0));
                    mSubscriptionManagerService.setDefaultDataSubId(mPrimarySubList.get(0));
                    return true;
                } else if (simSelectDialogType == EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DATA) {
                    // If another primary subscription is added or default data is not selected, ask
                    // user to select default for data as it's most important.
                    int newSubId = mPrimarySubList.get(0);
                    log("need to set voice:" + mNeedSetDefaultVoice
                            + ", sms:" + mNeedSetDefaultSms
                            + ", data:" + mNeedSetDefaultData);
                    // if the converted pSIM's preference is voice, set the default
                    // setting for the changed primary subscription to voice.
                    if (mNeedSetDefaultVoice) {
                        log("set preferred call, subId:" + newSubId);
                        mSubscriptionManagerService.setDefaultVoiceSubId(newSubId);
                        mNeedSetDefaultVoice = false;
                    }
                    // if the converted pSIM's preference is sms, set the default
                    // setting for the changed primary subscription to sms.
                    if (mNeedSetDefaultSms) {
                        log("set preferred sms, subId:" + newSubId);
                        mSubscriptionManagerService.setDefaultSmsSubId(newSubId);
                        mNeedSetDefaultSms = false;
                    }
                    // if the converted pSIM's preference is data, set the default
                    // setting for the changed primary subscription to data.
                    if (mNeedSetDefaultData) {
                        log("set preferred data, subId:" + newSubId);
                        mSubscriptionManagerService.setDefaultDataSubId(newSubId);
                        mNeedSetDefaultData = false;
                    }
                    mConvertedPsimSubId = subId;
                    log("set converted pSIM subId:" + mConvertedPsimSubId);
                    return true;
                }
            }
        }
        return false;
    }

    private int getConvertedPsimSubscriptionId() {
        // Check to see if any subscription has been converted due to the pSIM conversion.
        // When the primary subscription is changed, if it is the same subscription as
        // the previously converted subscription, it is not due to the pSIM conversion.
        // So the dialog for preferred SIM/data pick should show.
        // TODO(b/332261793): On Android W, we need to add CONVERTING status.
        //  The CONVERTING status allows us to determine if pSIM is in the process of converting,
        //  so we don't need to check for information about previously converted subscriptions.
        int convertedSubId = INVALID_SUBSCRIPTION_ID;
        if (mFeatureFlags.supportPsimToEsimConversion()) {
            List<SubscriptionInfo> infos =
                    mSubscriptionManagerService.getAvailableSubscriptionInfoList(
                            mContext.getOpPackageName(), mContext.getAttributionTag());
            for (SubscriptionInfo info : infos) {
                if (!info.isEmbedded() && info.getTransferStatus() == TRANSFER_STATUS_CONVERTED) {
                    convertedSubId = info.getSubscriptionId();
                }
            }
        }
        log("getConvertedPsimSubscriptionId: convertedSubId=" + convertedSubId);
        return convertedSubId;
    }

    private int getSimSelectDialogType(int change, boolean dataSelected,
            boolean voiceSelected, boolean smsSelected) {
        int dialogType = EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_NONE;

        // Do not show preference selection dialog during SuW as there is fullscreen activity to
        // choose preference.
        if (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) == 0) {
            return dialogType;
        }
        // If a primary subscription is removed and only one is left active, ask user
        // for preferred sub selection if any default setting is not set.
        // If another primary subscription is added or default data is not selected, ask
        // user to select default for data as it's most important.
        if (mPrimarySubList.size() == 1 && change == PRIMARY_SUB_REMOVED
                && (!dataSelected || !smsSelected || !voiceSelected)) {
            dialogType = EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_ALL;
        } else if (mPrimarySubList.size() > 1 && (isUserVisibleChange(change)
                || (change == PRIMARY_SUB_INITIALIZED && !dataSelected))) {
            // If change is SWAPPED_IN_GROUP or MARKED_OPPT, don't ask user again.
            dialogType = EXTRA_DEFAULT_SUBSCRIPTION_SELECT_TYPE_DATA;
        }

        return dialogType;
    }

    private class SimCombinationWarningParams {
        @TelephonyManager.SimCombinationWarningType
        int mWarningType = EXTRA_SIM_COMBINATION_WARNING_TYPE_NONE;
        String mSimNames;
    }

    private SimCombinationWarningParams getSimCombinationWarningParams(int change) {
        SimCombinationWarningParams params = new SimCombinationWarningParams();
        // If it's single SIM active, no SIM combination warning is needed.
        if (mPrimarySubList.size() <= 1) return params;
        // If it's no primary SIM change or it's not user visible change
        // (initialized or swapped in a group), no SIM combination warning is needed.
        if (!isUserVisibleChange(change)) return params;

        List<String> simNames = new ArrayList<>();
        int cdmaPhoneCount = 0;
        for (int subId : mPrimarySubList) {
            Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
            // If a dual CDMA SIM combination warning is needed.
            if (phone != null && phone.isCdmaSubscriptionAppPresent()) {
                cdmaPhoneCount++;
                String simName = null;
                SubscriptionInfoInternal subInfo = mSubscriptionManagerService
                        .getSubscriptionInfoInternal(subId);
                if (subInfo != null) {
                    simName = subInfo.getDisplayName();
                }
                if (TextUtils.isEmpty(simName)) {
                    // Fall back to carrier name.
                    simName = phone.getCarrierName();
                }
                simNames.add(simName);
            }
        }

        if (cdmaPhoneCount > 1) {
            params.mWarningType = EXTRA_SIM_COMBINATION_WARNING_TYPE_DUAL_CDMA;
            params.mSimNames = String.join(" & ", simNames);
        }

        return params;
    }

    private boolean isUserVisibleChange(int change) {
        return (change == PRIMARY_SUB_ADDED || change == PRIMARY_SUB_REMOVED
                || change == PRIMARY_SUB_SWAPPED);
    }

    protected void disableDataForNonDefaultNonOpportunisticSubscriptions() {
        if (!isReadyToReevaluate()) return;

        int defaultDataSub = mSubscriptionManagerService.getDefaultDataSubId();

        for (Phone phone : PhoneFactory.getPhones()) {
            SubscriptionInfoInternal subInfo = mSubscriptionManagerService
                    .getSubscriptionInfoInternal(phone.getSubId());
            boolean isOpportunistic = subInfo != null && subInfo.isOpportunistic();
            if (phone.getSubId() != defaultDataSub
                    && SubscriptionManager.isValidSubscriptionId(phone.getSubId())
                    && !isOpportunistic
                    && phone.isUserDataEnabled()
                    && !areSubscriptionsInSameGroup(defaultDataSub, phone.getSubId())) {
                log("setting data to false on " + phone.getSubId());
                phone.getDataSettingsManager().setDataEnabled(
                        TelephonyManager.DATA_ENABLED_REASON_USER, false,
                        mContext.getOpPackageName());
            }
        }
    }

    private boolean areSubscriptionsInSameGroup(int subId1, int subId2) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId1)
                || !SubscriptionManager.isUsableSubscriptionId(subId2)) return false;
        if (subId1 == subId2) return true;

        SubscriptionInfoInternal subInfo1 =
                mSubscriptionManagerService.getSubscriptionInfoInternal(subId1);
        SubscriptionInfoInternal subInfo2 =
                mSubscriptionManagerService.getSubscriptionInfoInternal(subId2);
        return subInfo1 != null && subInfo2 != null
                && !TextUtils.isEmpty(subInfo1.getGroupUuid())
                && subInfo1.getGroupUuid().equals(subInfo2.getGroupUuid());
    }

    /**
     * Make sure MOBILE_DATA of subscriptions in the same group with the subId
     * are synced.
     */
    protected void setUserDataEnabledForGroup(int subId, boolean enable) {
        log("setUserDataEnabledForGroup subId " + subId + " enable " + enable);
        List<SubscriptionInfo> infoList = null;
        SubscriptionInfoInternal subInfo = mSubscriptionManagerService
                .getSubscriptionInfoInternal(subId);
        if (subInfo != null && !subInfo.getGroupUuid().isEmpty()) {
            infoList = mSubscriptionManagerService.getSubscriptionsInGroup(
                    ParcelUuid.fromString(subInfo.getGroupUuid()), mContext.getOpPackageName(),
                    mContext.getAttributionTag());
        }

        if (infoList == null) return;

        for (SubscriptionInfo info : infoList) {
            int currentSubId = info.getSubscriptionId();
            // TODO: simplify when setUserDataEnabled becomes singleton
            if (info.isActive()) {
                // For active subscription, call setUserDataEnabled through DataSettingsManager.
                Phone phone = PhoneFactory.getPhone(mSubscriptionManagerService
                        .getPhoneId(currentSubId));
                // If enable is true and it's not opportunistic subscription, we don't enable it,
                // as there can't be two
                if (phone != null) {
                    phone.getDataSettingsManager().setDataEnabled(
                            TelephonyManager.DATA_ENABLED_REASON_USER, enable,
                            mContext.getOpPackageName());
                }
            } else {
                // For inactive subscription, directly write into global settings.
                GlobalSettingsHelper.setBoolean(
                        mContext, Settings.Global.MOBILE_DATA, currentSubId, enable);
            }
        }
    }

    /**
     * Make sure DATA_ROAMING of subscriptions in the same group with the subId
     * are synced.
     */
    private void setRoamingDataEnabledForGroup(int subId, boolean enable) {
        SubscriptionInfoInternal subInfo = mSubscriptionManagerService
                .getSubscriptionInfoInternal(subId);
        if (subInfo == null || subInfo.getGroupUuid().isEmpty()) return;
        List<SubscriptionInfo> infoList = SubscriptionManagerService.getInstance()
                .getSubscriptionsInGroup(ParcelUuid.fromString(subInfo.getGroupUuid()),
                        mContext.getOpPackageName(), mContext.getAttributionTag());
        if (infoList == null) return;

        for (SubscriptionInfo info : infoList) {
            // For inactive subscription, directly write into global settings.
            GlobalSettingsHelper.setBoolean(
                    mContext, Settings.Global.DATA_ROAMING, info.getSubscriptionId(), enable);
        }
    }

    private interface UpdateDefaultAction {
        void update(int newValue);
    }

    // Returns whether the new default value is valid.
    private boolean updateDefaultValue(List<Integer> primarySubList, int oldValue,
            UpdateDefaultAction action) {
        return updateDefaultValue(primarySubList, oldValue, action, true);
    }

    private boolean updateDefaultValue(List<Integer> primarySubList, int oldValue,
            UpdateDefaultAction action, boolean allowInvalidSubId) {
        int newValue = INVALID_SUBSCRIPTION_ID;

        if (primarySubList.size() > 0) {
            for (int subId : primarySubList) {
                if (DBG) log("[updateDefaultValue] Record.id: " + subId);
                // 1) If the old subId is still active, or there's another active primary
                // subscription that is in the same group, that should become the new default
                // subscription.
                // 2) If the old subId is INVALID_SUBSCRIPTION_ID and allowInvalidSubId is false,
                // first active subscription is used for new default always.
                if (areSubscriptionsInSameGroup(subId, oldValue)
                        || (!allowInvalidSubId && oldValue == INVALID_SUBSCRIPTION_ID)) {
                    newValue = subId;
                    log("[updateDefaultValue] updates to subId=" + newValue);
                    break;
                }
            }
        }

        if (oldValue != newValue) {
            if (DBG) log("[updateDefaultValue: subId] from " + oldValue + " to " + newValue);
            action.update(newValue);
        }

        return SubscriptionManager.isValidSubscriptionId(newValue);
    }

    // When a primary and its grouped opportunistic subscriptions were active, and the primary
    // subscription gets deactivated or removed, we need to automatically disable the grouped
    // opportunistic subscription, which will be marked isGroupDisabled as true by SubController.
    private void deactivateGroupedOpportunisticSubscriptionIfNeeded() {
        List<SubscriptionInfo> opptSubList = mSubscriptionManagerService.getAllSubInfoList(
                mContext.getOpPackageName(), mContext.getAttributionTag()).stream()
                .filter(SubscriptionInfo::isOpportunistic)
                .collect(Collectors.toList());

        if (ArrayUtils.isEmpty(opptSubList)) return;

        for (SubscriptionInfo info : opptSubList) {
            if (info.isGroupDisabled() && info.isActive()) {
                log("deactivateGroupedOpportunisticSubscriptionIfNeeded: "
                        + "Deactivating grouped opportunistic subscription "
                        + info.getSubscriptionId());
                deactivateSubscription(info);
            }
        }
    }

    private void deactivateSubscription(SubscriptionInfo info) {
        // TODO: b/133379187 have a way to deactivate pSIM.
        if (info.isEmbedded()) {
            log("[deactivateSubscription] eSIM profile " + info.getSubscriptionId());
            EuiccManager euiccManager = (EuiccManager)
                    mContext.getSystemService(Context.EUICC_SERVICE);
            euiccManager.switchToSubscription(SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                    info.getPortIndex(), PendingIntent.getService(
                            mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE));
        }
    }

    // Voice/Data/SMS preferences would be auto selected without any user
    // confirmation in following scenarios,
    // 1. When device powered-up with only one SIM Inserted or while two SIM cards
    // present if one SIM is removed(or turned OFF) the reaiming SIM would be
    // selected as preferred voice/data/sms SIM.
    // 2. When device powered-up with two SIM cards or if two SIM cards
    // present on device with new SIM insert(or SIM turn ON) the first inserted SIM
    // would be selected as preferred voice/data/sms SIM.
    private void updateUserPreferences(List<Integer> primarySubList, boolean dataSelected,
            boolean voiceSelected, boolean smsSelected) {
        // In Single SIM case or if there are no activated subs available, no need to update.
        // EXIT.
        if ((primarySubList.isEmpty()) || (mSubscriptionManagerService
                .getActiveSubInfoCountMax() == 1)) {
            return;
        }

        if (!isRadioAvailableOnAllSubs()) {
            log("Radio is in Invalid state, Ignore Updating User Preference!!!");
            return;
        }
        final int defaultDataSubId = mSubscriptionManagerService.getDefaultDataSubId();

        if (DBG) {
            log("updateUserPreferences:  dds = " + defaultDataSubId + " voice = "
                    + mSubscriptionManagerService.getDefaultVoiceSubId()
                    + " sms = " + mSubscriptionManagerService.getDefaultSmsSubId());
        }

        int autoDefaultSubId = primarySubList.get(0);

        if (hasMessaging() && (primarySubList.size() == 1) && !smsSelected) {
            mSubscriptionManagerService.setDefaultSmsSubId(autoDefaultSubId);
        }

        if (hasCalling() && (primarySubList.size() == 1) && !voiceSelected) {
            mSubscriptionManagerService.setDefaultVoiceSubId(autoDefaultSubId);
        }

        int userPrefDataSubId = getUserPrefDataSubIdFromDB();

        log("User pref subId = " + userPrefDataSubId + " current dds " + defaultDataSubId
                + " next active subId " + autoDefaultSubId);

        if (hasData()) {
            // If earlier user selected DDS is now available, set that as DDS subId.
            if (primarySubList.contains(userPrefDataSubId)
                    && SubscriptionManager.isValidSubscriptionId(userPrefDataSubId)
                    && (defaultDataSubId != userPrefDataSubId)) {
                mSubscriptionManagerService.setDefaultDataSubId(userPrefDataSubId);
            } else if (!dataSelected) {
                mSubscriptionManagerService.setDefaultDataSubId(autoDefaultSubId);
            }
        }

        if (DBG) {
            log("updateUserPreferences: after dds = "
                    + mSubscriptionManagerService.getDefaultDataSubId() + " voice = "
                    + mSubscriptionManagerService.getDefaultVoiceSubId() + " sms = "
                    + mSubscriptionManagerService.getDefaultSmsSubId());
        }
    }

    private int getUserPrefDataSubIdFromDB() {
        return android.provider.Settings.Global.getInt(mContext.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    private boolean isRadioAvailableOnAllSubs() {
        for (int phoneId = 0; phoneId < mActiveModemCount; phoneId++) {
            Phone phone = PhoneFactory.getPhone(phoneId);
            if (phone != null
                    && (phone.mCi.getRadioState() == TelephonyManager.RADIO_POWER_UNAVAILABLE
                    || phone.isShuttingDown())) {
                return false;
            }
        }
        return true;
    }

    private void registerDataSettingsControllerCallbackAsNeeded() {
        // Only register callbacks for new phone instance as PhoneFactory does not remove
        // existing phone instance.
        Phone[] phones = PhoneFactory.getPhones();
        for (int i = mCallbacksCount; i < phones.length; i++) {
            phones[i].getDataSettingsManager().registerCallback(
                    new DataSettingsControllerCallback(phones[i], this::post));
        }
        mCallbacksCount = phones.length;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }
}
