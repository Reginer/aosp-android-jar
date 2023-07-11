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
 * limitations under the License.
 */

package com.android.internal.telephony.dataconnection;


import static android.telephony.PhoneStateListener.LISTEN_CALL_STATE;
import static android.telephony.PhoneStateListener.LISTEN_NONE;

import android.annotation.IntDef;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telephony.Annotation.CallState;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.util.LocalLog;
import android.util.Pair;

import com.android.internal.telephony.GlobalSettingsHelper;
import com.android.internal.telephony.MultiSimSettingController;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.data.DataEnabledOverride;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The class to hold different data enabled/disabled settings. Also it allows clients to register
 * for overall data enabled setting changed event.
 * @hide
 */
public class DataEnabledSettings {

    private static final String LOG_TAG = "DataEnabledSettings";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"REASON_"},
            value = {
                    REASON_REGISTERED,
                    REASON_INTERNAL_DATA_ENABLED,
                    REASON_USER_DATA_ENABLED,
                    REASON_POLICY_DATA_ENABLED,
                    REASON_DATA_ENABLED_BY_CARRIER,
                    REASON_PROVISIONED_CHANGED,
                    REASON_PROVISIONING_DATA_ENABLED_CHANGED,
                    REASON_OVERRIDE_RULE_CHANGED,
                    REASON_OVERRIDE_CONDITION_CHANGED,
                    REASON_THERMAL_DATA_ENABLED
            })
    public @interface DataEnabledChangedReason {}

    public static final int REASON_REGISTERED = 0;

    public static final int REASON_INTERNAL_DATA_ENABLED = 1;

    public static final int REASON_USER_DATA_ENABLED = 2;

    public static final int REASON_POLICY_DATA_ENABLED = 3;

    public static final int REASON_DATA_ENABLED_BY_CARRIER = 4;

    public static final int REASON_PROVISIONED_CHANGED = 5;

    public static final int REASON_PROVISIONING_DATA_ENABLED_CHANGED = 6;

    public static final int REASON_OVERRIDE_RULE_CHANGED = 7;

    public static final int REASON_OVERRIDE_CONDITION_CHANGED = 8;

    public static final int REASON_THERMAL_DATA_ENABLED = 9;

    /**
     * responds to the setInternalDataEnabled call - used internally to turn off data.
     * For example during emergency calls
     */
    private boolean mInternalDataEnabled = true;

    /**
     * Flag indicating data allowed by network policy manager or not.
     */
    private boolean mPolicyDataEnabled = true;

    /**
     * Indicate if metered APNs are enabled by the carrier. set false to block all the metered APNs
     * from continuously sending requests, which causes undesired network load.
     */
    private boolean mCarrierDataEnabled = true;

    /**
     * Flag indicating data allowed by Thermal service or not.
     */
    private boolean mThermalDataEnabled = true;

    /**
     * Flag indicating whether data is allowed or not for the device. It can be disabled by
     * user, carrier, policy or thermal
     */
    private boolean mIsDataEnabled = false;

    private final Phone mPhone;

    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private ContentResolver mResolver = null;

    private final RegistrantList mOverallDataEnabledChangedRegistrants = new RegistrantList();

    // TODO: Merge this with mOverallDataEnabledChangedRegistrants. In the future, notifying data
    // enabled changed with APN types bitmask
    private final RegistrantList mOverallDataEnabledOverrideChangedRegistrants =
            new RegistrantList();

    private final LocalLog mSettingChangeLocalLog = new LocalLog(32);

    private DataEnabledOverride mDataEnabledOverride;

    private TelephonyManager mTelephonyManager;

    // for msim, user data enabled setting depends on subId.
    private final SubscriptionManager.OnSubscriptionsChangedListener
            mOnSubscriptionsChangeListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    synchronized (this) {
                        if (mSubId != mPhone.getSubId()) {
                            log("onSubscriptionsChanged subId: " + mSubId + " to: "
                                    + mPhone.getSubId());
                            mSubId = mPhone.getSubId();
                            mDataEnabledOverride = getDataEnabledOverride();
                            updatePhoneStateListener();
                            updateDataEnabledAndNotify(REASON_USER_DATA_ENABLED);
                            mPhone.notifyUserMobileDataStateChanged(isUserDataEnabled());
                        }
                    }
                }
            };

    private void updatePhoneStateListener() {
        mTelephonyManager.listen(mPhoneStateListener, LISTEN_NONE);
        if (SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(mSubId);
        }
        mTelephonyManager.listen(mPhoneStateListener, LISTEN_CALL_STATE);
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(@CallState int state, String phoneNumber) {
            updateDataEnabledAndNotify(REASON_OVERRIDE_CONDITION_CHANGED);
        }
    };

    @Override
    public String toString() {
        return "[mInternalDataEnabled=" + mInternalDataEnabled
                + ", isUserDataEnabled=" + isUserDataEnabled()
                + ", isProvisioningDataEnabled=" + isProvisioningDataEnabled()
                + ", mPolicyDataEnabled=" + mPolicyDataEnabled
                + ", mCarrierDataEnabled=" + mCarrierDataEnabled
                + ", mIsDataEnabled=" + mIsDataEnabled
                + ", mThermalDataEnabled=" + mThermalDataEnabled
                + ", " + mDataEnabledOverride
                + "]";
    }

    public DataEnabledSettings(Phone phone) {
        mPhone = phone;
        mResolver = mPhone.getContext().getContentResolver();
        SubscriptionManager subscriptionManager = (SubscriptionManager) mPhone.getContext()
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        subscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        mTelephonyManager = (TelephonyManager) mPhone.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        mDataEnabledOverride = getDataEnabledOverride();
        updateDataEnabled();
    }

    private DataEnabledOverride getDataEnabledOverride() {
        return new DataEnabledOverride(SubscriptionController.getInstance()
                .getDataEnabledOverrideRules(mPhone.getSubId()));
    }

    public synchronized void setInternalDataEnabled(boolean enabled) {
        if (mInternalDataEnabled != enabled) {
            localLog("InternalDataEnabled", enabled);
            mInternalDataEnabled = enabled;
            updateDataEnabledAndNotify(REASON_INTERNAL_DATA_ENABLED);
        }
    }
    public synchronized boolean isInternalDataEnabled() {
        return mInternalDataEnabled;
    }

    private synchronized void setUserDataEnabled(boolean enabled) {
        // By default the change should propagate to the group.
        setUserDataEnabled(enabled, true);
    }

    /**
     * @param notifyMultiSimSettingController if setUserDataEnabled is already from propagating
     *        from MultiSimSettingController, don't notify MultiSimSettingController again.
     *        For example, if sub1 and sub2 are in the same group and user enables data for sub
     *        1, sub 2 will also be enabled but with propagateToGroup = false.
     */
    public synchronized void setUserDataEnabled(boolean enabled,
            boolean notifyMultiSimSettingController) {
        // Can't disable data for stand alone opportunistic subscription.
        if (isStandAloneOpportunistic(mPhone.getSubId(), mPhone.getContext()) && !enabled) return;

        boolean changed = GlobalSettingsHelper.setInt(mPhone.getContext(),
                Settings.Global.MOBILE_DATA, mPhone.getSubId(), (enabled ? 1 : 0));
        if (changed) {
            localLog("UserDataEnabled", enabled);
            mPhone.notifyUserMobileDataStateChanged(enabled);
            updateDataEnabledAndNotify(REASON_USER_DATA_ENABLED);
            if (notifyMultiSimSettingController) {
                MultiSimSettingController.getInstance().notifyUserDataEnabled(
                        mPhone.getSubId(), enabled);
            }
        }
    }

    /**
     * Policy control of data connection with reason
     * @param reason the reason the data enable change is taking place
     * @param enabled True if enabling the data, otherwise disabling.
     */
    public synchronized void setDataEnabled(@TelephonyManager.DataEnabledReason int reason,
            boolean enabled) {
        switch (reason) {
            case TelephonyManager.DATA_ENABLED_REASON_USER:
                setUserDataEnabled(enabled);
                break;
            case TelephonyManager.DATA_ENABLED_REASON_CARRIER:
                setCarrierDataEnabled(enabled);
                break;
            case TelephonyManager.DATA_ENABLED_REASON_POLICY:
                setPolicyDataEnabled(enabled);
                break;
            case TelephonyManager.DATA_ENABLED_REASON_THERMAL:
                setThermalDataEnabled(enabled);
                break;
            default:
                log("Invalid data enable reason " + reason);
                break;
        }
    }

    public synchronized boolean isUserDataEnabled() {
        // User data should always be true for opportunistic subscription.
        if (isStandAloneOpportunistic(mPhone.getSubId(), mPhone.getContext())) return true;

        boolean defaultVal = TelephonyProperties.mobile_data().orElse(true);

        return GlobalSettingsHelper.getBoolean(mPhone.getContext(),
                Settings.Global.MOBILE_DATA, mPhone.getSubId(), defaultVal);
    }

    /**
     * Set whether always allowing MMS data connection.
     *
     * @param alwaysAllow {@code true} if MMS data is always allowed.
     *
     * @return {@code false} if the setting is changed.
     */
    public synchronized boolean setAlwaysAllowMmsData(boolean alwaysAllow) {
        localLog("setAlwaysAllowMmsData", alwaysAllow);
        mDataEnabledOverride.setAlwaysAllowMms(alwaysAllow);
        boolean changed = SubscriptionController.getInstance()
                .setDataEnabledOverrideRules(mPhone.getSubId(), mDataEnabledOverride.getRules());
        if (changed) {
            updateDataEnabledAndNotify(REASON_OVERRIDE_RULE_CHANGED);
            notifyDataEnabledOverrideChanged();
        }

        return changed;
    }

    /**
     * Set allowing mobile data during voice call. This is used for allowing data on the non-default
     * data SIM. When a voice call is placed on the non-default data SIM on DSDS devices, users will
     * not be able to use mobile data. By calling this API, data will be temporarily enabled on the
     * non-default data SIM during the life cycle of the voice call.
     *
     * @param allow {@code true} if allowing using data during voice call, {@code false} if
     * disallowed
     *
     * @return {@code true} if operation is successful. otherwise {@code false}.
     */
    public synchronized boolean setAllowDataDuringVoiceCall(boolean allow) {
        localLog("setAllowDataDuringVoiceCall", allow);
        if (allow == isDataAllowedInVoiceCall()) {
            return true;
        }
        mDataEnabledOverride.setDataAllowedInVoiceCall(allow);

        boolean changed = SubscriptionController.getInstance()
                .setDataEnabledOverrideRules(mPhone.getSubId(), mDataEnabledOverride.getRules());
        if (changed) {
            updateDataEnabledAndNotify(REASON_OVERRIDE_RULE_CHANGED);
            notifyDataEnabledOverrideChanged();
        }

        return changed;
    }

    /**
     * Check if data is allowed during voice call.
     *
     * @return {@code true} if data is allowed during voice call.
     */
    public synchronized boolean isDataAllowedInVoiceCall() {
        return mDataEnabledOverride.isDataAllowedInVoiceCall();
    }

    public synchronized boolean isMmsAlwaysAllowed() {
        return mDataEnabledOverride.isMmsAlwaysAllowed();
    }

    private synchronized void setPolicyDataEnabled(boolean enabled) {
        if (mPolicyDataEnabled != enabled) {
            localLog("PolicyDataEnabled", enabled);
            mPolicyDataEnabled = enabled;
            updateDataEnabledAndNotify(REASON_POLICY_DATA_ENABLED);
        }
    }

    public synchronized boolean isPolicyDataEnabled() {
        return mPolicyDataEnabled;
    }

    private synchronized void setCarrierDataEnabled(boolean enabled) {
        if (mCarrierDataEnabled != enabled) {
            localLog("CarrierDataEnabled", enabled);
            mCarrierDataEnabled = enabled;
            updateDataEnabledAndNotify(REASON_DATA_ENABLED_BY_CARRIER);
        }
    }

    public synchronized boolean isCarrierDataEnabled() {
        return mCarrierDataEnabled;
    }

    private synchronized void setThermalDataEnabled(boolean enabled) {
        if (mThermalDataEnabled != enabled) {
            localLog("ThermalDataEnabled", enabled);
            mThermalDataEnabled = enabled;
            updateDataEnabledAndNotify(REASON_THERMAL_DATA_ENABLED);
        }
    }

    public synchronized boolean isThermalDataEnabled() {
        return mThermalDataEnabled;
    }

    public synchronized void updateProvisionedChanged() {
        updateDataEnabledAndNotify(REASON_PROVISIONED_CHANGED);
    }

    public synchronized void updateProvisioningDataEnabled() {
        updateDataEnabledAndNotify(REASON_PROVISIONING_DATA_ENABLED_CHANGED);
    }

    public synchronized boolean isDataEnabled() {
        return mIsDataEnabled;
    }

    /**
     * Check if data is enabled for a specific reason {@@TelephonyManager.DataEnabledReason}
     *
     * @return {@code true} if the overall data is enabled; {@code false} if not.
     */
    public synchronized boolean isDataEnabledForReason(
            @TelephonyManager.DataEnabledReason int reason) {
        switch (reason) {
            case TelephonyManager.DATA_ENABLED_REASON_USER:
                return isUserDataEnabled();
            case TelephonyManager.DATA_ENABLED_REASON_CARRIER:
                return isCarrierDataEnabled();
            case TelephonyManager.DATA_ENABLED_REASON_POLICY:
                return isPolicyDataEnabled();
            case TelephonyManager.DATA_ENABLED_REASON_THERMAL:
                return isThermalDataEnabled();
            default:
                return false;
        }
    }

    private synchronized void updateDataEnabledAndNotify(int reason) {
        boolean prevDataEnabled = mIsDataEnabled;

        updateDataEnabled();

        if (prevDataEnabled != mIsDataEnabled) {
            notifyDataEnabledChanged(!prevDataEnabled, reason);
        }
    }

    private synchronized void updateDataEnabled() {
        if (isProvisioning()) {
            mIsDataEnabled = isProvisioningDataEnabled();
        } else {
            mIsDataEnabled = mInternalDataEnabled && (isUserDataEnabled() || mDataEnabledOverride
                    .shouldOverrideDataEnabledSettings(mPhone, ApnSetting.TYPE_ALL))
                    && mPolicyDataEnabled && mCarrierDataEnabled && mThermalDataEnabled;
        }
    }

    public boolean isProvisioning() {
        return Settings.Global.getInt(mResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 0;
    }
    /**
     * In provisioning, we might want to have enable mobile data during provisioning. It depends
     * on value of Settings.Global.DEVICE_PROVISIONING_MOBILE_DATA_ENABLED which is set by
     * setupwizard. It only matters if it's in provisioning stage.
     * @return whether we are enabling userData during provisioning stage.
     */
    public boolean isProvisioningDataEnabled() {
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

    public synchronized void setDataRoamingEnabled(boolean enabled) {
        // will trigger handleDataOnRoamingChange() through observer
        boolean changed = GlobalSettingsHelper.setBoolean(mPhone.getContext(),
                Settings.Global.DATA_ROAMING, mPhone.getSubId(), enabled);

        if (changed) {
            localLog("setDataRoamingEnabled", enabled);
            MultiSimSettingController.getInstance().notifyRoamingDataEnabled(mPhone.getSubId(),
                    enabled);
        }
    }

    /**
     * Return current {@link android.provider.Settings.Global#DATA_ROAMING} value.
     */
    public synchronized boolean getDataRoamingEnabled() {
        return GlobalSettingsHelper.getBoolean(mPhone.getContext(),
                Settings.Global.DATA_ROAMING, mPhone.getSubId(), getDefaultDataRoamingEnabled());
    }

    /**
     * get default values for {@link Settings.Global#DATA_ROAMING}
     * return {@code true} if either
     * {@link CarrierConfigManager#KEY_CARRIER_DEFAULT_DATA_ROAMING_ENABLED_BOOL} or
     * system property ro.com.android.dataroaming is set to true. otherwise return {@code false}
     */
    public synchronized boolean getDefaultDataRoamingEnabled() {
        final CarrierConfigManager configMgr = (CarrierConfigManager)
                mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        boolean isDataRoamingEnabled = "true".equalsIgnoreCase(SystemProperties.get(
                "ro.com.android.dataroaming", "false"));
        isDataRoamingEnabled |= configMgr.getConfigForSubId(mPhone.getSubId()).getBoolean(
                CarrierConfigManager.KEY_CARRIER_DEFAULT_DATA_ROAMING_ENABLED_BOOL);
        return isDataRoamingEnabled;
    }

    private void notifyDataEnabledChanged(boolean enabled, int reason) {
        mOverallDataEnabledChangedRegistrants.notifyResult(new Pair<>(enabled, reason));
        mPhone.notifyDataEnabled(enabled, reason);
    }

    public void registerForDataEnabledChanged(Handler h, int what, Object obj) {
        mOverallDataEnabledChangedRegistrants.addUnique(h, what, obj);
        notifyDataEnabledChanged(isDataEnabled(), REASON_REGISTERED);
    }

    public void unregisterForDataEnabledChanged(Handler h) {
        mOverallDataEnabledChangedRegistrants.remove(h);
    }

    private void notifyDataEnabledOverrideChanged() {
        mOverallDataEnabledOverrideChangedRegistrants.notifyRegistrants();
    }

    /**
     * Register for data enabled override changed event.
     *
     * @param h The handler
     * @param what The event
     */
    public void registerForDataEnabledOverrideChanged(Handler h, int what) {
        mOverallDataEnabledOverrideChangedRegistrants.addUnique(h, what, null);
        notifyDataEnabledOverrideChanged();
    }

    /**
     * Unregistered for data enabled override changed event.
     *
     * @param h The handler
     */
    public void unregisterForDataEnabledOverrideChanged(Handler h) {
        mOverallDataEnabledOverrideChangedRegistrants.remove(h);
    }

    private static boolean isStandAloneOpportunistic(int subId, Context context) {
        SubscriptionInfo info = SubscriptionController.getInstance().getActiveSubscriptionInfo(
                subId, context.getOpPackageName(), context.getAttributionTag());
        return (info != null) && info.isOpportunistic() && info.getGroupUuid() == null;
    }

    public synchronized boolean isDataEnabled(int apnType) {
        if (isProvisioning()) {
            return isProvisioningDataEnabled();
        } else {
            boolean userDataEnabled = isUserDataEnabled();
            // Check if we should temporarily enable data in certain conditions.
            boolean isDataEnabledOverridden = mDataEnabledOverride
                    .shouldOverrideDataEnabledSettings(mPhone, apnType);

            return (mInternalDataEnabled && mPolicyDataEnabled && mCarrierDataEnabled
                    && mThermalDataEnabled && (userDataEnabled || isDataEnabledOverridden));
        }
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    private void localLog(String name, boolean value) {
        mSettingChangeLocalLog.log(name + " change to " + value);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println(" DataEnabledSettings=");
        mSettingChangeLocalLog.dump(fd, pw, args);
    }
}
