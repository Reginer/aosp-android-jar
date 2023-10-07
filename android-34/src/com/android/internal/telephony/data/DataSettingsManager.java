/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.internal.telephony.data;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MobileDataPolicy;
import android.telephony.TelephonyRegistryManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.ApnSetting.ApnType;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;

import com.android.internal.telephony.GlobalSettingsHelper;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.telephony.data.DataConfigManager.DataConfigManagerCallback;
import com.android.internal.telephony.metrics.DeviceTelephonyPropertiesStats;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * DataSettingsManager maintains the data related settings, for example, data enabled settings,
 * data roaming settings, etc...
 */
public class DataSettingsManager extends Handler {
    /** Invalid mobile data policy **/
    private static final int INVALID_MOBILE_DATA_POLICY = -1;

    /** Event for call state changed. */
    private static final int EVENT_CALL_STATE_CHANGED = 2;
    /** Event for subscriptions updated. */
    private static final int EVENT_SUBSCRIPTIONS_CHANGED = 4;
    /** Event for set data enabled for reason. */
    private static final int EVENT_SET_DATA_ENABLED_FOR_REASON = 5;
    /** Event for set data roaming enabled. */
    private static final int EVENT_SET_DATA_ROAMING_ENABLED = 6;
    /** Event for set mobile data policy. */
    private static final int EVENT_SET_MOBILE_DATA_POLICY = 7;

    /** Event for device provisioned changed. */
    private static final int EVENT_PROVISIONED_CHANGED = 9;
    /** Event for provisioning data enabled setting changed. */
    private static final int EVENT_PROVISIONING_DATA_ENABLED_CHANGED = 10;
    /** Event for initializing DataSettingsManager. */
    private static final int EVENT_INITIALIZE = 11;

    private final Phone mPhone;
    private final ContentResolver mResolver;
    private final SettingsObserver mSettingsObserver;
    private final String mLogTag;
    private final LocalLog mLocalLog = new LocalLog(128);
    private Set<Integer> mEnabledMobileDataPolicy = new HashSet<>();
    private int mSubId;

    /** Data config manager */
    private final @NonNull DataConfigManager mDataConfigManager;

    /** Data settings manager callbacks. */
    private final @NonNull Set<DataSettingsManagerCallback> mDataSettingsManagerCallbacks =
            new ArraySet<>();

    /** Mapping of {@link TelephonyManager.DataEnabledReason} to data enabled values. */
    private final Map<Integer, Boolean> mDataEnabledSettings = new ArrayMap<>();

    /**
     * Flag indicating whether data is allowed or not for the device.
     * It can be disabled by user, carrier, policy or thermal.
     */
    private boolean mIsDataEnabled;

    /**
     * Used to indicate that the initial value for mIsDataEnabled was set.
     * Prevent race condition where the initial value might be incorrect.
     */
    private boolean mInitialized = false;

    /**
     * Data settings manager callback. This should be only used by {@link DataNetworkController}.
     */
    public static class DataSettingsManagerCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public DataSettingsManagerCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when overall data enabled state changed.
         *
         * @param enabled {@code true} indicates mobile data is enabled.
         * @param reason {@link TelephonyManager.DataEnabledChangedReason} indicating the reason why
         *               mobile data enabled changed.
         * @param callingPackage The package that changed the data enabled state.
         */
        public void onDataEnabledChanged(boolean enabled,
                @TelephonyManager.DataEnabledChangedReason int reason,
                @NonNull String callingPackage) {}

        /**
         * Called when data enabled override changed.
         *
         * @param enabled {@code true} indicates data enabled override is enabled.
         * @param policy {@link TelephonyManager.MobileDataPolicy} indicating the policy that was
         *               enabled or disabled.
         */
        public void onDataEnabledOverrideChanged(boolean enabled,
                @TelephonyManager.MobileDataPolicy int policy) {}

        /**
         * Called when data roaming enabled state changed.
         *
         * @param enabled {@code true} indicates data roaming is enabled.
         */
        public void onDataRoamingEnabledChanged(boolean enabled) {}
    }

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param callback Data settings manager callback.
     */
    public DataSettingsManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController, @NonNull Looper looper,
            @NonNull DataSettingsManagerCallback callback) {
        super(looper);
        mPhone = phone;
        mLogTag = "DSMGR-" + mPhone.getPhoneId();
        log("DataSettingsManager created.");
        mSubId = mPhone.getSubId();
        mResolver = mPhone.getContext().getContentResolver();
        registerCallback(callback);
        mDataConfigManager = dataNetworkController.getDataConfigManager();
        refreshEnabledMobileDataPolicy();
        mSettingsObserver = new SettingsObserver(mPhone.getContext(), this);
        mDataEnabledSettings.put(TelephonyManager.DATA_ENABLED_REASON_POLICY, true);
        mDataEnabledSettings.put(TelephonyManager.DATA_ENABLED_REASON_CARRIER, true);
        mDataEnabledSettings.put(TelephonyManager.DATA_ENABLED_REASON_THERMAL, true);

        // Instead of calling onInitialize directly from the constructor, send the event.
        // The reason is that getImsPhone is null when we are still in the constructor here.
        sendEmptyMessage(EVENT_INITIALIZE);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_CALL_STATE_CHANGED: {
                updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_OVERRIDE);
                break;
            }
            case EVENT_SUBSCRIPTIONS_CHANGED: {
                mSubId = (int) msg.obj;
                refreshEnabledMobileDataPolicy();
                updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_USER);
                mPhone.notifyUserMobileDataStateChanged(isUserDataEnabled());
                break;
            }
            case EVENT_SET_DATA_ENABLED_FOR_REASON: {
                String callingPackage = (String) msg.obj;
                boolean enabled = msg.arg2 == 1;
                switch (msg.arg1) {
                    case TelephonyManager.DATA_ENABLED_REASON_USER:
                        setUserDataEnabled(enabled, callingPackage);
                        break;
                    case TelephonyManager.DATA_ENABLED_REASON_CARRIER:
                        setCarrierDataEnabled(enabled, callingPackage);
                        break;
                    case TelephonyManager.DATA_ENABLED_REASON_POLICY:
                        setPolicyDataEnabled(enabled, callingPackage);
                        break;
                    case TelephonyManager.DATA_ENABLED_REASON_THERMAL:
                        setThermalDataEnabled(enabled, callingPackage);
                        break;
                    default:
                        log("Cannot set data enabled for reason: "
                                + dataEnabledChangedReasonToString(msg.arg1));
                        break;
                }
                break;
            }
            case EVENT_SET_DATA_ROAMING_ENABLED: {
                boolean enabled = (boolean) msg.obj;
                setDataRoamingEnabledInternal(enabled);
                setDataRoamingFromUserAction();
                break;
            }
            case EVENT_SET_MOBILE_DATA_POLICY: {
                int mobileDataPolicy = msg.arg1;
                boolean enable = msg.arg2 == 1;
                onSetMobileDataPolicy(mobileDataPolicy, enable);
                break;
            }
            case EVENT_PROVISIONED_CHANGED:
            case EVENT_PROVISIONING_DATA_ENABLED_CHANGED: {
                updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_UNKNOWN);
                break;
            }
            case EVENT_INITIALIZE: {
                onInitialize();
                break;
            }
            default:
                loge("Unknown msg.what: " + msg.what);
        }
    }

    /**
     * Called when needed to register for all events that data network controller is interested.
     */
    private void onInitialize() {
        mDataConfigManager.registerCallback(new DataConfigManagerCallback(this::post) {
            @Override
            public void onCarrierConfigChanged() {
                if (mDataConfigManager.isConfigCarrierSpecific()) {
                    setDefaultDataRoamingEnabled();
                }
            }
        });
        mSettingsObserver.observe(Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED),
                EVENT_PROVISIONED_CHANGED);
        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONING_MOBILE_DATA_ENABLED),
                EVENT_PROVISIONING_DATA_ENABLED_CHANGED);
        mPhone.getCallTracker().registerForVoiceCallStarted(this, EVENT_CALL_STATE_CHANGED, null);
        mPhone.getCallTracker().registerForVoiceCallEnded(this, EVENT_CALL_STATE_CHANGED, null);
        if (mPhone.getImsPhone() != null) {
            mPhone.getImsPhone().getCallTracker().registerForVoiceCallStarted(
                    this, EVENT_CALL_STATE_CHANGED, null);
            mPhone.getImsPhone().getCallTracker().registerForVoiceCallEnded(
                    this, EVENT_CALL_STATE_CHANGED, null);
        }
        mPhone.getContext().getSystemService(TelephonyRegistryManager.class)
                .addOnSubscriptionsChangedListener(new OnSubscriptionsChangedListener() {
                    @Override
                    public void onSubscriptionsChanged() {
                        if (mSubId != mPhone.getSubId()) {
                            log("onSubscriptionsChanged: " + mSubId + " to " + mPhone.getSubId());
                            obtainMessage(EVENT_SUBSCRIPTIONS_CHANGED, mPhone.getSubId())
                                    .sendToTarget();
                        }
                    }
                }, this::post);
        updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_UNKNOWN);
    }

    /**
     * Enable or disable data for a specific {@link TelephonyManager.DataEnabledReason}.
     * @param reason The reason the data enabled change is taking place.
     * @param enabled {@code true} to enable data for the given reason and {@code false} to disable.
     * @param callingPackage The package that changed the data enabled state.
     */
    public void setDataEnabled(@TelephonyManager.DataEnabledReason int reason, boolean enabled,
            String callingPackage) {
        obtainMessage(EVENT_SET_DATA_ENABLED_FOR_REASON, reason, enabled ? 1 : 0, callingPackage)
                .sendToTarget();
    }

    /**
     * Check whether the data is enabled for a specific {@link TelephonyManager.DataEnabledReason}.
     * @return {@code true} if data is enabled for the given reason and {@code false} otherwise.
     */
    public boolean isDataEnabledForReason(@TelephonyManager.DataEnabledReason int reason) {
        if (reason == TelephonyManager.DATA_ENABLED_REASON_USER) {
            return isUserDataEnabled();
        } else {
            return mDataEnabledSettings.get(reason);
        }
    }

    private void updateDataEnabledAndNotify(@TelephonyManager.DataEnabledChangedReason int reason) {
        updateDataEnabledAndNotify(reason, mPhone.getContext().getOpPackageName());
    }

    private void updateDataEnabledAndNotify(@TelephonyManager.DataEnabledChangedReason int reason,
            @NonNull String callingPackage) {
        boolean prevDataEnabled = mIsDataEnabled;
        mIsDataEnabled = isDataEnabled(ApnSetting.TYPE_ALL);
        log("mIsDataEnabled=" + mIsDataEnabled + ", prevDataEnabled=" + prevDataEnabled);
        if (!mInitialized || prevDataEnabled != mIsDataEnabled) {
            if (!mInitialized) mInitialized = true;
            notifyDataEnabledChanged(mIsDataEnabled, reason, callingPackage);
        }
    }

    /**
     * Check whether the user data is enabled when the device is in the provisioning stage.
     * In provisioning, we might want to enable mobile data depending on the value of
     * Settings.Global.DEVICE_PROVISIONING_MOBILE_DATA_ENABLED, which is set by setupwizard.
     * @return {@code true} if user data is enabled when provisioning and {@code false} otherwise.
     */
    private boolean isProvisioningDataEnabled() {
        final String prov_property = SystemProperties.get("ro.com.android.prov_mobiledata",
                "false");
        boolean retVal = "true".equalsIgnoreCase(prov_property);

        final int prov_mobile_data = Settings.Global.getInt(mResolver,
                Settings.Global.DEVICE_PROVISIONING_MOBILE_DATA_ENABLED,
                retVal ? 1 : 0);
        retVal = prov_mobile_data != 0;
        log("getDataEnabled during provisioning retVal=" + retVal + " - (" + prov_property
                + ", " + prov_mobile_data + ")");

        return retVal;
    }

    /**
     * Check whether the overall data is enabled for the device. Note that this value will only
     * be accurate if {@link #isDataInitialized} is {@code true}.
     * @return {@code true} if the overall data is enabled and {@code false} otherwise.
     */
    public boolean isDataEnabled() {
        return mIsDataEnabled;
    }

    /**
     * Check whether data enabled value has been initialized. If this is {@code false}, then
     * {@link #isDataEnabled} is not guaranteed to be accurate. Once data is initialized,
     * {@link DataSettingsManagerCallback#onDataEnabledChanged} will be invoked with reason
     * {@link TelephonyManager#DATA_ENABLED_REASON_UNKNOWN}.
     * @return {@code true} if the data enabled value is initialized and {@code false} otherwise.
     */
    public boolean isDataInitialized() {
        // TODO: Create a new DATA_ENABLED_REASON_INITIALIZED for initial value broadcast
        return mInitialized;
    }

    /**
     * Check whether the overall data is enabled for the device for the given APN type.
     * @param apnType A single APN type to check data enabled for.
     * @return {@code true} if the overall data is enabled for the APN and {@code false} otherwise.
     */
    public boolean isDataEnabled(@ApnType int apnType) {
        if (Settings.Global.getInt(mResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 0) {
            return isProvisioningDataEnabled();
        } else {
            boolean userDataEnabled = isUserDataEnabled();
            // Check if we should temporarily enable data based on mobile data policy.
            boolean isDataEnabledOverridden = isDataEnabledOverriddenForApn(apnType);


            return ((userDataEnabled || isDataEnabledOverridden)
                    && mDataEnabledSettings.get(TelephonyManager.DATA_ENABLED_REASON_POLICY)
                    && mDataEnabledSettings.get(TelephonyManager.DATA_ENABLED_REASON_CARRIER)
                    && mDataEnabledSettings.get(TelephonyManager.DATA_ENABLED_REASON_THERMAL));
        }
    }

    private boolean isStandAloneOpportunistic(int subId) {
        SubscriptionInfoInternal subInfo = SubscriptionManagerService.getInstance()
                .getSubscriptionInfoInternal(subId);
        return subInfo != null && subInfo.isOpportunistic()
                && TextUtils.isEmpty(subInfo.getGroupUuid());
    }

    /**
     * Enable or disable user data.
     * @param enabled {@code true} to enable user data and {@code false} to disable.
     * @param callingPackage The package that changed the data enabled state.
     */
    private void setUserDataEnabled(boolean enabled, String callingPackage) {
        // Can't disable data for stand alone opportunistic subscription.
        if (isStandAloneOpportunistic(mSubId) && !enabled) return;
        boolean changed = GlobalSettingsHelper.setInt(mPhone.getContext(),
                Settings.Global.MOBILE_DATA, mSubId, (enabled ? 1 : 0));
        log("Set user data enabled to " + enabled + ", changed=" + changed + ", callingPackage="
                + callingPackage);
        if (changed) {
            logl("UserDataEnabled changed to " + enabled);
            mPhone.notifyUserMobileDataStateChanged(enabled);
            updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_USER, callingPackage);
        }
    }

    /**
     * Check whether user data is enabled for the device.
     * @return {@code true} if user data is enabled and {@code false} otherwise.
     */
    private boolean isUserDataEnabled() {
        if (Settings.Global.getInt(mResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 0) {
            return isProvisioningDataEnabled();
        }

        // User data should always be true for opportunistic subscription.
        if (isStandAloneOpportunistic(mSubId)) return true;

        boolean defaultVal = TelephonyProperties.mobile_data().orElse(true);

        return GlobalSettingsHelper.getBoolean(mPhone.getContext(),
                Settings.Global.MOBILE_DATA, mSubId, defaultVal);
    }

    /**
     * Enable or disable policy data.
     * @param enabled {@code true} to enable policy data and {@code false} to disable.
     * @param callingPackage The package that changed the data enabled state.
     */
    private void setPolicyDataEnabled(boolean enabled, String callingPackage) {
        if (mDataEnabledSettings.get(TelephonyManager.DATA_ENABLED_REASON_POLICY) != enabled) {
            logl("PolicyDataEnabled changed to " + enabled + ", callingPackage=" + callingPackage);
            mDataEnabledSettings.put(TelephonyManager.DATA_ENABLED_REASON_POLICY, enabled);
            updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_POLICY, callingPackage);
        }
    }

    /**
     * Enable or disable carrier data.
     * @param enabled {@code true} to enable carrier data and {@code false} to disable.
     * @param callingPackage The package that changed the data enabled state.
     */
    private void setCarrierDataEnabled(boolean enabled, String callingPackage) {
        if (mDataEnabledSettings.get(TelephonyManager.DATA_ENABLED_REASON_CARRIER) != enabled) {
            logl("CarrierDataEnabled changed to " + enabled + ", callingPackage=" + callingPackage);
            mDataEnabledSettings.put(TelephonyManager.DATA_ENABLED_REASON_CARRIER, enabled);
            updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_CARRIER,
                    callingPackage);
        }
    }

    /**
     * Enable or disable thermal data.
     * @param enabled {@code true} to enable thermal data and {@code false} to disable.
     * @param callingPackage The package that changed the data enabled state.
     */
    private void setThermalDataEnabled(boolean enabled, String callingPackage) {
        if (mDataEnabledSettings.get(TelephonyManager.DATA_ENABLED_REASON_THERMAL) != enabled) {
            logl("ThermalDataEnabled changed to " + enabled + ", callingPackage=" + callingPackage);
            mDataEnabledSettings.put(TelephonyManager.DATA_ENABLED_REASON_THERMAL, enabled);
            updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_THERMAL,
                    callingPackage);
        }
    }

    /**
     * Enable or disable data roaming from user settings.
     * @param enabled {@code true} to enable data roaming and {@code false} to disable.
     */
    public void setDataRoamingEnabled(boolean enabled) {
        obtainMessage(EVENT_SET_DATA_ROAMING_ENABLED, enabled).sendToTarget();
    }

    /**
     * Enable or disable data roaming.
     * @param enabled {@code true} to enable data roaming and {@code false} to disable.
     */
    private void setDataRoamingEnabledInternal(boolean enabled) {
        // Will trigger handleDataOnRoamingChange() through observer
        boolean changed = GlobalSettingsHelper.setBoolean(mPhone.getContext(),
                Settings.Global.DATA_ROAMING, mSubId, enabled);
        if (changed) {
            logl("DataRoamingEnabled changed to " + enabled);
            mDataSettingsManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    () -> callback.onDataRoamingEnabledChanged(enabled)));
        }
    }

    /**
     * Check whether data roaming is enabled for the device based on the current
     * {@link Settings.Global#DATA_ROAMING} value.
     * @return {@code true} if data roaming is enabled and {@code false} otherwise.
     */
    public boolean isDataRoamingEnabled() {
        return GlobalSettingsHelper.getBoolean(mPhone.getContext(),
                Settings.Global.DATA_ROAMING, mSubId, isDefaultDataRoamingEnabled());
    }

    /**
     * Check whether data roaming is enabled by default.
     * This is true if {@link CarrierConfigManager#KEY_CARRIER_DEFAULT_DATA_ROAMING_ENABLED_BOOL}
     * or the system property "ro.com.android.dataroaming" are true.
     * @return {@code true} if data roaming is enabled by default and {@code false} otherwise.
     */
    public boolean isDefaultDataRoamingEnabled() {
        return "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.dataroaming", "false"))
                || mPhone.getDataNetworkController().getDataConfigManager()
                        .isDataRoamingEnabledByDefault();
    }

    /**
     * Set default value for {@link android.provider.Settings.Global#DATA_ROAMING} if the user
     * has not manually set the value. The default value is {@link #isDefaultDataRoamingEnabled()}.
     */
    public void setDefaultDataRoamingEnabled() {
        // If the user has not manually set the value, use the default value.
        if (!isDataRoamingFromUserAction()) {
            setDataRoamingEnabledInternal(isDefaultDataRoamingEnabled());
        }
    }

    /**
     * Get whether the user has manually enabled or disabled data roaming from settings for the
     * current subscription.
     * @return {@code true} if the user has manually enabled data roaming for the current
     *         subscription and {@code false} if they have not.
     */
    private boolean isDataRoamingFromUserAction() {
        String key = Phone.DATA_ROAMING_IS_USER_SETTING_KEY + mPhone.getSubId();
        final SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(mPhone.getContext());

        // Set the default roaming from user action value if the preference doesn't exist
        if (!sp.contains(key)) {
            if (sp.contains(Phone.DATA_ROAMING_IS_USER_SETTING_KEY)) {
                log("Reusing previous roaming from user action value for backwards compatibility.");
                sp.edit().putBoolean(key, true).commit();
            } else {
                log("Clearing roaming from user action value for new or upgrading devices.");
                sp.edit().putBoolean(key, false).commit();
            }
        }

        boolean isUserSetting = sp.getBoolean(key, true);
        log("isDataRoamingFromUserAction: key=" + key + ", isUserSetting=" + isUserSetting);
        return isUserSetting;
    }

    /**
     * Indicate that the user has manually enabled or disabled the data roaming value from settings.
     * If the user has not manually set the data roaming value, the default value from
     * {@link #isDefaultDataRoamingEnabled()} will continue to be used.
     */
    private void setDataRoamingFromUserAction() {
        String key = Phone.DATA_ROAMING_IS_USER_SETTING_KEY + mPhone.getSubId();
        log("setDataRoamingFromUserAction: key=" + key);
        final SharedPreferences.Editor sp =
                PreferenceManager.getDefaultSharedPreferences(mPhone.getContext()).edit();
        sp.putBoolean(key, true).commit();
    }

    /** Refresh the enabled mobile data policies from Telephony database */
    private void refreshEnabledMobileDataPolicy() {
        SubscriptionInfoInternal subInfo = SubscriptionManagerService.getInstance()
                .getSubscriptionInfoInternal(mSubId);
        if (subInfo != null) {
            mEnabledMobileDataPolicy = getMobileDataPolicyEnabled(
                    subInfo.getEnabledMobileDataPolicies());
        }
    }

    /**
     * @return {@code true} If the mobile data policy is enabled
     */
    public boolean isMobileDataPolicyEnabled(@MobileDataPolicy int mobileDataPolicy) {
        return mEnabledMobileDataPolicy.contains(mobileDataPolicy);
    }

    /**
     * Set mobile data policy enabled status
     * @param mobileDataPolicy The mobile data policy to set
     * @param enable {@code true} to enable the policy; {@code false} to disable.
     */
    public void setMobileDataPolicy(@MobileDataPolicy int mobileDataPolicy, boolean enable) {
        obtainMessage(EVENT_SET_MOBILE_DATA_POLICY, mobileDataPolicy, enable ? 1 : 0)
                .sendToTarget();
    }

    /**
     * Store data mobile policy to Telephony database.
     *
     * @param mobileDataPolicy The mobile data policy that overrides user data enabled setting.
     * @param enable {@code true} to enable the policy; {@code false} to remove the policy.
     */
    private void onSetMobileDataPolicy(@MobileDataPolicy int mobileDataPolicy, boolean enable) {
        if (enable == isMobileDataPolicyEnabled(mobileDataPolicy)) {
            return;
        }
        metricsRecordSetMobileDataPolicy(mobileDataPolicy);

        if (enable) {
            mEnabledMobileDataPolicy.add(mobileDataPolicy);
        } else {
            mEnabledMobileDataPolicy.remove(mobileDataPolicy);
        }

        String enabledMobileDataPolicies = mEnabledMobileDataPolicy.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
        SubscriptionManagerService.getInstance().setEnabledMobileDataPolicies(mSubId,
                enabledMobileDataPolicies);
        logl(TelephonyUtils.mobileDataPolicyToString(mobileDataPolicy) + " changed to "
                + enable);
        updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_OVERRIDE);
        notifyDataEnabledOverrideChanged(enable, mobileDataPolicy);
    }

    /**
     * Record the number of times a mobile data policy is toggled to metrics.
     * @param mobileDataPolicy The mobile data policy that's toggled
     */
    private void metricsRecordSetMobileDataPolicy(@MobileDataPolicy int mobileDataPolicy) {
        if (mobileDataPolicy == TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH) {
            DeviceTelephonyPropertiesStats.recordAutoDataSwitchFeatureToggle();
        }
    }

    /**
     * Check whether data stall recovery on bad network is enabled.
     * @return {@code true} if data stall recovery is enabled and {@code false} otherwise.
     */
    public boolean isRecoveryOnBadNetworkEnabled() {
        return Settings.Global.getInt(mResolver,
                Settings.Global.DATA_STALL_RECOVERY_ON_BAD_NETWORK, 1) == 1;
    }

    private void notifyDataEnabledChanged(boolean enabled,
            @TelephonyManager.DataEnabledChangedReason int reason, @NonNull String callingPackage) {
        logl("notifyDataEnabledChanged: enabled=" + enabled + ", reason="
                + dataEnabledChangedReasonToString(reason) + ", callingPackage=" + callingPackage);
        mDataSettingsManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onDataEnabledChanged(enabled, reason, callingPackage)));
        mPhone.notifyDataEnabled(enabled, reason);
    }

    private void notifyDataEnabledOverrideChanged(boolean enabled,
            @TelephonyManager.MobileDataPolicy int policy) {
        logl("notifyDataEnabledOverrideChanged: enabled=" + enabled);
        mDataSettingsManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onDataEnabledOverrideChanged(enabled, policy)));
    }

    /**
     * Return the parsed mobile data policies.
     *
     * @param policies New mobile data policies in String format.
     * @return A Set of parsed mobile data policies.
     */
    public @NonNull @MobileDataPolicy Set<Integer> getMobileDataPolicyEnabled(
            @NonNull String policies) {
        Set<Integer> mobileDataPolicies = new HashSet<>();
        String[] rulesString = policies.trim().split("\\s*,\\s*");
        for (String rule : rulesString) {
            if (!TextUtils.isEmpty(rule)) {
                int parsedDataPolicy = parsePolicyFrom(rule);
                if (parsedDataPolicy != INVALID_MOBILE_DATA_POLICY) {
                    mobileDataPolicies.add(parsedDataPolicy);
                }
            }
        }
        return mobileDataPolicies;
    }

    /**
     * Parse a mobile data policy retrieved from Telephony db.
     * If the policy is in legacy format, convert it into the corresponding mobile data policy.
     *
     * @param policy Mobile data policy to be parsed from.
     * @return Parsed mobile data policy. {@link #INVALID_MOBILE_DATA_POLICY} if string can't be
     * parsed into a mobile data policy.
     */
    private @MobileDataPolicy int parsePolicyFrom(@NonNull String policy) {
        int dataPolicy;
        try {
            // parse as new override policy
            dataPolicy = Integer.parseInt(policy);
        } catch (NumberFormatException e) {
            dataPolicy = INVALID_MOBILE_DATA_POLICY;
            loge("parsePolicyFrom: invalid mobile data policy format: "  + policy);
        }
        return dataPolicy;
    }

    /**
     * Check if data enabled is temporarily overridden in certain conditions.
     *
     * @param apnType The APN type to check.
     * @return {@code true} if data enabled should be overridden.
     */
    private boolean isDataEnabledOverriddenForApn(@ApnType int apnType) {
        boolean overridden = false;

        // mobile data policy : MMS always allowed
        if (isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED)) {
            overridden = apnType == ApnSetting.TYPE_MMS;
        }

        boolean isNonDds = mPhone.getSubId() != SubscriptionManagerService.getInstance()
                .getDefaultDataSubId();

        // mobile data policy : data during call
        if (isMobileDataPolicyEnabled(TelephonyManager
                .MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL)) {
            overridden = overridden || isNonDds && mPhone.getState() != PhoneConstants.State.IDLE;
        }

        // mobile data policy : auto data switch
        if (isMobileDataPolicyEnabled(TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)) {
            // check user enabled data on the default data phone
            Phone defaultDataPhone = PhoneFactory.getPhone(SubscriptionManagerService.getInstance()
                    .getPhoneId(SubscriptionManagerService.getInstance()
                            .getDefaultDataSubId()));
            if (defaultDataPhone == null) {
                loge("isDataEnabledOverriddenForApn: unexpected defaultDataPhone is null");
            } else {
                overridden = overridden || isNonDds && defaultDataPhone.isUserDataEnabled();
            }
        }
        return overridden;
    }

    /**
     * Register the callback for receiving information from {@link DataSettingsManager}.
     *
     * @param callback The callback.
     */
    public void registerCallback(@NonNull DataSettingsManagerCallback callback) {
        mDataSettingsManagerCallbacks.add(callback);
    }

    /**
     * Unregister the callback for receiving information from {@link DataSettingsManager}.
     *
     * @param callback The callback.
     */
    public void unregisterCallback(@NonNull DataSettingsManagerCallback callback) {
        mDataSettingsManagerCallbacks.remove(callback);
    }

    private static String dataEnabledChangedReasonToString(
            @TelephonyManager.DataEnabledChangedReason int reason) {
        switch (reason) {
            case TelephonyManager.DATA_ENABLED_REASON_USER:
                return "USER";
            case TelephonyManager.DATA_ENABLED_REASON_POLICY:
                return "POLICY";
            case TelephonyManager.DATA_ENABLED_REASON_CARRIER:
                return "CARRIER";
            case TelephonyManager.DATA_ENABLED_REASON_THERMAL:
                return "THERMAL";
            case TelephonyManager.DATA_ENABLED_REASON_OVERRIDE:
                return "OVERRIDE";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return "[isUserDataEnabled=" + isUserDataEnabled()
                + ", isProvisioningDataEnabled=" + isProvisioningDataEnabled()
                + ", mIsDataEnabled=" + mIsDataEnabled
                + ", mDataEnabledSettings=" + mDataEnabledSettings
                + ", mEnabledMobileDataPolicy=" + mEnabledMobileDataPolicy.stream()
                .map(TelephonyUtils::mobileDataPolicyToString).collect(Collectors.joining(","))
                + "]";
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }

    /**
     * Log debug messages and also log into the local log.
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of DataSettingsManager
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(DataSettingsManager.class.getSimpleName() + "-" + mPhone.getPhoneId() + ":");
        pw.increaseIndent();
        pw.println("mIsDataEnabled=" + mIsDataEnabled);
        pw.println("isDataEnabled(internet)=" + isDataEnabled(ApnSetting.TYPE_DEFAULT));
        pw.println("isDataEnabled(mms)=" + isDataEnabled(ApnSetting.TYPE_MMS));
        pw.println("isUserDataEnabled=" + isUserDataEnabled());
        pw.println("isDataRoamingEnabled=" + isDataRoamingEnabled());
        pw.println("isDefaultDataRoamingEnabled=" + isDefaultDataRoamingEnabled());
        pw.println("isDataRoamingFromUserAction=" + isDataRoamingFromUserAction());
        pw.println("device_provisioned=" + Settings.Global.getInt(
                mResolver, Settings.Global.DEVICE_PROVISIONED, 0));
        pw.println("isProvisioningDataEnabled=" + isProvisioningDataEnabled());
        pw.println("data_stall_recovery_on_bad_network=" + Settings.Global.getInt(
                mResolver, Settings.Global.DATA_STALL_RECOVERY_ON_BAD_NETWORK, 1));
        pw.println("mDataEnabledSettings=" + mDataEnabledSettings.entrySet().stream()
                .map(entry ->
                        dataEnabledChangedReasonToString(entry.getKey()) + "=" + entry.getValue())
                .collect(Collectors.joining(", ")));
        pw.println("mEnabledMobileDataPolicy=" + mEnabledMobileDataPolicy.stream()
                .map(TelephonyUtils::mobileDataPolicyToString).collect(Collectors.joining(",")));
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}
