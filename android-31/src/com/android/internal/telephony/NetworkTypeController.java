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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation;
import android.telephony.CarrierConfigManager;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PcoData;
import android.telephony.PhysicalChannelConfig;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;

import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.dataconnection.DcController;
import com.android.internal.telephony.dataconnection.DcController.PhysicalLinkState;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.internal.util.IState;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * The NetworkTypeController evaluates the override network type of {@link TelephonyDisplayInfo}
 * and sends it to {@link DisplayInfoController}. The override network type can replace the signal
 * icon displayed on the status bar. It is affected by changes in data RAT, NR state, NR frequency,
 * data activity, physical channel config, and carrier configurations. Based on carrier configs,
 * NetworkTypeController also allows timers between various 5G states to prevent flickering.
 */
public class NetworkTypeController extends StateMachine {
    private static final boolean DBG = true;
    private static final String TAG = "NetworkTypeController";
    private static final String ICON_5G = "5g";
    private static final String ICON_5G_PLUS = "5g_plus";
    private static final String STATE_CONNECTED_NR_ADVANCED = "connected_mmwave";
    private static final String STATE_CONNECTED = "connected";
    private static final String STATE_NOT_RESTRICTED_RRC_IDLE = "not_restricted_rrc_idle";
    private static final String STATE_NOT_RESTRICTED_RRC_CON = "not_restricted_rrc_con";
    private static final String STATE_RESTRICTED = "restricted";
    private static final String STATE_ANY = "any";
    private static final String STATE_LEGACY = "legacy";
    private static final String[] ALL_STATES = {STATE_CONNECTED_NR_ADVANCED, STATE_CONNECTED,
            STATE_NOT_RESTRICTED_RRC_IDLE, STATE_NOT_RESTRICTED_RRC_CON, STATE_RESTRICTED,
            STATE_LEGACY };

    /** Stop all timers and go to current state. */
    public static final int EVENT_UPDATE = 0;
    /** Quit after processing all existing messages. */
    public static final int EVENT_QUIT = 1;
    private static final int EVENT_DATA_RAT_CHANGED = 2;
    private static final int EVENT_NR_STATE_CHANGED = 3;
    private static final int EVENT_NR_FREQUENCY_CHANGED = 4;
    private static final int EVENT_PHYSICAL_LINK_STATE_CHANGED = 5;
    private static final int EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED = 6;
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 7;
    private static final int EVENT_PRIMARY_TIMER_EXPIRED = 8;
    private static final int EVENT_SECONDARY_TIMER_EXPIRED = 9;
    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 10;
    private static final int EVENT_PREFERRED_NETWORK_MODE_CHANGED = 11;
    private static final int EVENT_INITIALIZE = 12;
    private static final int EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED = 13;
    private static final int EVENT_PCO_DATA_CHANGED = 14;

    private static final String[] sEvents = new String[EVENT_PCO_DATA_CHANGED + 1];
    static {
        sEvents[EVENT_UPDATE] = "EVENT_UPDATE";
        sEvents[EVENT_QUIT] = "EVENT_QUIT";
        sEvents[EVENT_DATA_RAT_CHANGED] = "EVENT_DATA_RAT_CHANGED";
        sEvents[EVENT_NR_STATE_CHANGED] = "EVENT_NR_STATE_CHANGED";
        sEvents[EVENT_NR_FREQUENCY_CHANGED] = "EVENT_NR_FREQUENCY_CHANGED";
        sEvents[EVENT_PHYSICAL_LINK_STATE_CHANGED] = "EVENT_PHYSICAL_LINK_STATE_CHANGED";
        sEvents[EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED] =
                "EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED";
        sEvents[EVENT_CARRIER_CONFIG_CHANGED] = "EVENT_CARRIER_CONFIG_CHANGED";
        sEvents[EVENT_PRIMARY_TIMER_EXPIRED] = "EVENT_PRIMARY_TIMER_EXPIRED";
        sEvents[EVENT_SECONDARY_TIMER_EXPIRED] = "EVENT_SECONDARY_TIMER_EXPIRED";
        sEvents[EVENT_RADIO_OFF_OR_UNAVAILABLE] = "EVENT_RADIO_OFF_OR_UNAVAILABLE";
        sEvents[EVENT_PREFERRED_NETWORK_MODE_CHANGED] = "EVENT_PREFERRED_NETWORK_MODE_CHANGED";
        sEvents[EVENT_INITIALIZE] = "EVENT_INITIALIZE";
        sEvents[EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED] = "EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED";
        sEvents[EVENT_PCO_DATA_CHANGED] = "EVENT_PCO_DATA_CHANGED";
    }

    private final Phone mPhone;
    private final DisplayInfoController mDisplayInfoController;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)
                    && intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                    SubscriptionManager.INVALID_PHONE_INDEX) == mPhone.getPhoneId()
                    && !intent.getBooleanExtra(CarrierConfigManager.EXTRA_REBROADCAST_ON_UNLOCK,
                    false)) {
                sendMessage(EVENT_CARRIER_CONFIG_CHANGED);
            }
        }
    };

    private Map<String, OverrideTimerRule> mOverrideTimerRules = new HashMap<>();
    private String mLteEnhancedPattern = "";
    private int mOverrideNetworkType;
    private boolean mIsPhysicalChannelConfigOn;
    private boolean mIsPrimaryTimerActive;
    private boolean mIsSecondaryTimerActive;
    private boolean mIsTimerResetEnabledForLegacyStateRRCIdle;
    private int mLtePlusThresholdBandwidth;
    private int[] mAdditionalNrAdvancedBandsList;
    private String mPrimaryTimerState;
    private String mSecondaryTimerState;
    private String mPreviousState;
    private @PhysicalLinkState int mPhysicalLinkState;
    private boolean mIsPhysicalChannelConfig16Supported;
    private Boolean mIsNrAdvancedAllowedByPco = false;
    private int mNrAdvancedCapablePcoId = 0;
    private boolean mIsUsingUserDataForRrcDetection = false;

    /**
     * NetworkTypeController constructor.
     *
     * @param phone Phone object.
     * @param displayInfoController DisplayInfoController to send override network types to.
     */
    public NetworkTypeController(Phone phone, DisplayInfoController displayInfoController) {
        super(TAG, displayInfoController);
        mPhone = phone;
        mDisplayInfoController = displayInfoController;
        mOverrideNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE;
        mIsPhysicalChannelConfigOn = true;
        addState(mDefaultState);
        addState(mLegacyState, mDefaultState);
        addState(mIdleState, mDefaultState);
        addState(mLteConnectedState, mDefaultState);
        addState(mNrConnectedState, mDefaultState);
        setInitialState(mDefaultState);
        start();
        sendMessage(EVENT_INITIALIZE);
    }

    /**
     * @return The current override network type, used to create TelephonyDisplayInfo in
     * DisplayInfoController.
     */
    public @Annotation.OverrideNetworkType int getOverrideNetworkType() {
        return mOverrideNetworkType;
    }

    /**
     * @return True if either the primary or secondary 5G hysteresis timer is active,
     * and false if neither are.
     */
    public boolean is5GHysteresisActive() {
        return mIsPrimaryTimerActive || mIsSecondaryTimerActive;
    }

    private void registerForAllEvents() {
        mPhone.registerForRadioOffOrNotAvailable(getHandler(),
                EVENT_RADIO_OFF_OR_UNAVAILABLE, null);
        mPhone.registerForPreferredNetworkTypeChanged(getHandler(),
                EVENT_PREFERRED_NETWORK_MODE_CHANGED, null);
        mPhone.registerForPhysicalChannelConfig(getHandler(),
                EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED, null);
        mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN, getHandler(),
                EVENT_DATA_RAT_CHANGED, null);
        mIsPhysicalChannelConfig16Supported = mPhone.getContext().getSystemService(
                TelephonyManager.class).isRadioInterfaceCapabilitySupported(
                TelephonyManager.CAPABILITY_PHYSICAL_CHANNEL_CONFIG_1_6_SUPPORTED);
        if (!mIsPhysicalChannelConfig16Supported) {
            mPhone.getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                    .registerForPhysicalLinkStateChanged(getHandler(),
                            EVENT_PHYSICAL_LINK_STATE_CHANGED);
        }
        mPhone.getServiceStateTracker().registerForNrStateChanged(getHandler(),
                EVENT_NR_STATE_CHANGED, null);
        mPhone.getServiceStateTracker().registerForNrFrequencyChanged(getHandler(),
                EVENT_NR_FREQUENCY_CHANGED, null);
        mPhone.getDeviceStateMonitor().registerForPhysicalChannelConfigNotifChanged(getHandler(),
                EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone);
        mPhone.mCi.registerForPcoData(getHandler(), EVENT_PCO_DATA_CHANGED, null);
    }

    private void unRegisterForAllEvents() {
        mPhone.unregisterForRadioOffOrNotAvailable(getHandler());
        mPhone.unregisterForPreferredNetworkTypeChanged(getHandler());
        mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN, getHandler());
        mPhone.getServiceStateTracker().unregisterForNrStateChanged(getHandler());
        mPhone.getServiceStateTracker().unregisterForNrFrequencyChanged(getHandler());
        mPhone.getDeviceStateMonitor().unregisterForPhysicalChannelConfigNotifChanged(getHandler());
        mPhone.getContext().unregisterReceiver(mIntentReceiver);
        mPhone.mCi.unregisterForPcoData(getHandler());
    }

    private void parseCarrierConfigs() {
        String nrIconConfiguration = CarrierConfigManager.getDefaultConfig().getString(
                CarrierConfigManager.KEY_5G_ICON_CONFIGURATION_STRING);
        String overrideTimerRule = CarrierConfigManager.getDefaultConfig().getString(
                CarrierConfigManager.KEY_5G_ICON_DISPLAY_GRACE_PERIOD_STRING);
        String overrideSecondaryTimerRule = CarrierConfigManager.getDefaultConfig().getString(
                CarrierConfigManager.KEY_5G_ICON_DISPLAY_SECONDARY_GRACE_PERIOD_STRING);
        mLteEnhancedPattern = CarrierConfigManager.getDefaultConfig().getString(
                CarrierConfigManager.KEY_SHOW_CARRIER_DATA_ICON_PATTERN_STRING);
        mIsTimerResetEnabledForLegacyStateRRCIdle =
                CarrierConfigManager.getDefaultConfig().getBoolean(
                        CarrierConfigManager.KEY_NR_TIMERS_RESET_IF_NON_ENDC_AND_RRC_IDLE_BOOL);
        mLtePlusThresholdBandwidth = CarrierConfigManager.getDefaultConfig().getInt(
                CarrierConfigManager.KEY_LTE_PLUS_THRESHOLD_BANDWIDTH_KHZ_INT);

        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle b = configManager.getConfigForSubId(mPhone.getSubId());
            if (b != null) {
                if (b.getString(CarrierConfigManager.KEY_5G_ICON_CONFIGURATION_STRING) != null) {
                    nrIconConfiguration = b.getString(
                            CarrierConfigManager.KEY_5G_ICON_CONFIGURATION_STRING);
                }
                if (b.getString(CarrierConfigManager
                        .KEY_5G_ICON_DISPLAY_GRACE_PERIOD_STRING) != null) {
                    overrideTimerRule = b.getString(
                            CarrierConfigManager.KEY_5G_ICON_DISPLAY_GRACE_PERIOD_STRING);
                }
                if (b.getString(CarrierConfigManager
                        .KEY_5G_ICON_DISPLAY_SECONDARY_GRACE_PERIOD_STRING) != null) {
                    overrideSecondaryTimerRule = b.getString(
                            CarrierConfigManager.KEY_5G_ICON_DISPLAY_SECONDARY_GRACE_PERIOD_STRING);
                }
                if (b.getString(CarrierConfigManager
                        .KEY_SHOW_CARRIER_DATA_ICON_PATTERN_STRING) != null) {
                    mLteEnhancedPattern = b.getString(
                            CarrierConfigManager.KEY_SHOW_CARRIER_DATA_ICON_PATTERN_STRING);
                }
                mIsTimerResetEnabledForLegacyStateRRCIdle = b.getBoolean(
                        CarrierConfigManager.KEY_NR_TIMERS_RESET_IF_NON_ENDC_AND_RRC_IDLE_BOOL);
                mLtePlusThresholdBandwidth = b.getInt(
                        CarrierConfigManager.KEY_LTE_PLUS_THRESHOLD_BANDWIDTH_KHZ_INT,
                        mLtePlusThresholdBandwidth);
                mAdditionalNrAdvancedBandsList = b.getIntArray(
                        CarrierConfigManager.KEY_ADDITIONAL_NR_ADVANCED_BANDS_INT_ARRAY);
                mNrAdvancedCapablePcoId = b.getInt(
                        CarrierConfigManager.KEY_NR_ADVANCED_CAPABLE_PCO_ID_INT);
                mIsUsingUserDataForRrcDetection = b.getBoolean(
                        CarrierConfigManager.KEY_LTE_ENDC_USING_USER_DATA_FOR_RRC_DETECTION_BOOL);
                if (mIsPhysicalChannelConfig16Supported && mIsUsingUserDataForRrcDetection) {
                    mPhone.getDcTracker(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                            .registerForPhysicalLinkStateChanged(getHandler(),
                                    EVENT_PHYSICAL_LINK_STATE_CHANGED);
                }
            }
        }
        createTimerRules(nrIconConfiguration, overrideTimerRule, overrideSecondaryTimerRule);
    }

    private void createTimerRules(String icons, String timers, String secondaryTimers) {
        Map<String, OverrideTimerRule> tempRules = new HashMap<>();
        if (!TextUtils.isEmpty(icons)) {
            // Format: "STATE:ICON,STATE2:ICON2"
            for (String pair : icons.trim().split(",")) {
                String[] kv = (pair.trim().toLowerCase()).split(":");
                if (kv.length != 2) {
                    if (DBG) loge("Invalid 5G icon configuration, config = " + pair);
                    continue;
                }
                int icon = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE;
                if (kv[1].equals(ICON_5G)) {
                    icon = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA;
                } else if (kv[1].equals(ICON_5G_PLUS)) {
                    icon = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED;
                } else {
                    if (DBG) loge("Invalid 5G icon = " + kv[1]);
                }
                tempRules.put(kv[0], new OverrideTimerRule(kv[0], icon));
            }
        }
        // Ensure all states have an associated OverrideTimerRule and icon
        for (String state : ALL_STATES) {
            if (!tempRules.containsKey(state)) {
                tempRules.put(state, new OverrideTimerRule(
                        state, TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE));
            }
        }

        if (!TextUtils.isEmpty(timers)) {
            // Format: "FROM_STATE,TO_STATE,DURATION;FROM_STATE_2,TO_STATE_2,DURATION_2"
            for (String triple : timers.trim().split(";")) {
                String[] kv = (triple.trim().toLowerCase()).split(",");
                if (kv.length != 3) {
                    if (DBG) loge("Invalid 5G icon timer configuration, config = " + triple);
                    continue;
                }
                int duration;
                try {
                    duration = Integer.parseInt(kv[2]);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (kv[0].equals(STATE_ANY)) {
                    for (String state : ALL_STATES) {
                        OverrideTimerRule node = tempRules.get(state);
                        node.addTimer(kv[1], duration);
                    }
                } else {
                    OverrideTimerRule node = tempRules.get(kv[0]);
                    node.addTimer(kv[1], duration);
                }
            }
        }

        if (!TextUtils.isEmpty(secondaryTimers)) {
            // Format: "PRIMARY_STATE,TO_STATE,DURATION;PRIMARY_STATE_2,TO_STATE_2,DURATION_2"
            for (String triple : secondaryTimers.trim().split(";")) {
                String[] kv = (triple.trim().toLowerCase()).split(",");
                if (kv.length != 3) {
                    if (DBG) {
                        loge("Invalid 5G icon secondary timer configuration, config = " + triple);
                    }
                    continue;
                }
                int duration;
                try {
                    duration = Integer.parseInt(kv[2]);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (kv[0].equals(STATE_ANY)) {
                    for (String state : ALL_STATES) {
                        OverrideTimerRule node = tempRules.get(state);
                        node.addSecondaryTimer(kv[1], duration);
                    }
                } else {
                    OverrideTimerRule node = tempRules.get(kv[0]);
                    node.addSecondaryTimer(kv[1], duration);
                }
            }
        }

        mOverrideTimerRules = tempRules;
        if (DBG) log("mOverrideTimerRules: " + mOverrideTimerRules);
    }

    private void updateOverrideNetworkType() {
        if (mIsPrimaryTimerActive || mIsSecondaryTimerActive) {
            if (DBG) log("Skip updating override network type since timer is active.");
            return;
        }
        mOverrideNetworkType = getCurrentOverrideNetworkType();
        mDisplayInfoController.updateTelephonyDisplayInfo();
    }

    private @Annotation.OverrideNetworkType int getCurrentOverrideNetworkType() {
        int displayNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE;
        int dataNetworkType = getDataNetworkType();
        boolean nrNsa = isLte(dataNetworkType)
                && mPhone.getServiceState().getNrState() != NetworkRegistrationInfo.NR_STATE_NONE;
        boolean nrSa = dataNetworkType == TelephonyManager.NETWORK_TYPE_NR;

        // NR display is not accurate when physical channel config notifications are off
        if (mIsPhysicalChannelConfigOn && (nrNsa || nrSa)) {
            // Process NR display network type
            displayNetworkType = getNrDisplayType(nrSa);
            if (displayNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE) {
                // Use LTE values if 5G values aren't defined
                displayNetworkType = getLteDisplayType();
            }
        } else if (isLte(dataNetworkType)) {
            // Process LTE display network type
            displayNetworkType = getLteDisplayType();
        }
        return displayNetworkType;
    }

    private @Annotation.OverrideNetworkType int getNrDisplayType(boolean isNrSa) {
        // Don't show 5G icon if preferred network type does not include 5G
        if ((mPhone.getCachedAllowedNetworkTypesBitmask()
                & TelephonyManager.NETWORK_TYPE_BITMASK_NR) == 0) {
            return TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE;
        }
        // Icon display keys in order of priority
        List<String> keys = new ArrayList<>();
        if (isNrSa && isNrAdvanced()) {
            keys.add(STATE_CONNECTED_NR_ADVANCED);
        } else {
            switch (mPhone.getServiceState().getNrState()) {
                case NetworkRegistrationInfo.NR_STATE_CONNECTED:
                    if (isNrAdvanced()) {
                        keys.add(STATE_CONNECTED_NR_ADVANCED);
                    }
                    keys.add(STATE_CONNECTED);
                    break;
                case NetworkRegistrationInfo.NR_STATE_NOT_RESTRICTED:
                    keys.add(isPhysicalLinkActive() ? STATE_NOT_RESTRICTED_RRC_CON
                            : STATE_NOT_RESTRICTED_RRC_IDLE);
                    break;
                case NetworkRegistrationInfo.NR_STATE_RESTRICTED:
                    keys.add(STATE_RESTRICTED);
                    break;
            }
        }

        for (String key : keys) {
            OverrideTimerRule rule = mOverrideTimerRules.get(key);
            if (rule != null && rule.mOverrideType
                    != TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE) {
                return rule.mOverrideType;
            }
        }
        return TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE;
    }

    private @Annotation.OverrideNetworkType int getLteDisplayType() {
        int value = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE;
        if ((getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE_CA
                || mPhone.getServiceState().isUsingCarrierAggregation())
                && (IntStream.of(mPhone.getServiceState().getCellBandwidths()).sum()
                        > mLtePlusThresholdBandwidth)) {
            value = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA;
        }
        if (isLteEnhancedAvailable()) {
            value = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO;
        }
        return value;
    }

    private boolean isLteEnhancedAvailable() {
        if (TextUtils.isEmpty(mLteEnhancedPattern)) {
            return false;
        }
        Pattern stringPattern = Pattern.compile(mLteEnhancedPattern);
        for (String opName : new String[] {mPhone.getServiceState().getOperatorAlphaLongRaw(),
                mPhone.getServiceState().getOperatorAlphaShortRaw()}) {
            if (!TextUtils.isEmpty(opName)) {
                Matcher matcher = stringPattern.matcher(opName);
                if (matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The parent state for all other states.
     */
    private final class DefaultState extends State {
        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("DefaultState: process " + getEventName(msg.what));
            switch (msg.what) {
                case EVENT_UPDATE:
                case EVENT_PREFERRED_NETWORK_MODE_CHANGED:
                    resetAllTimers();
                    transitionToCurrentState();
                    break;
                case EVENT_QUIT:
                    resetAllTimers();
                    unRegisterForAllEvents();
                    quit();
                    break;
                case EVENT_INITIALIZE:
                    // The reason that we do it here is because some of the works below requires
                    // other modules (e.g. DcTracker, ServiceStateTracker), which is not created
                    // yet when NetworkTypeController is created.
                    registerForAllEvents();
                    parseCarrierConfigs();
                    break;
                case EVENT_DATA_RAT_CHANGED:
                case EVENT_NR_STATE_CHANGED:
                case EVENT_NR_FREQUENCY_CHANGED:
                case EVENT_PCO_DATA_CHANGED:
                    // ignored
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED:
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkState = getPhysicalLinkStateFromPhysicalChannelConfig();
                    }
                    break;
                case EVENT_PHYSICAL_LINK_STATE_CHANGED:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    mPhysicalLinkState = (int) ar.result;
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED:
                    AsyncResult result = (AsyncResult) msg.obj;
                    mIsPhysicalChannelConfigOn = (boolean) result.result;
                    if (DBG) {
                        log("mIsPhysicalChannelConfigOn changed to: " + mIsPhysicalChannelConfigOn);
                    }
                    if (!mIsPhysicalChannelConfigOn) {
                        resetAllTimers();
                    }
                    transitionToCurrentState();
                    break;
                case EVENT_CARRIER_CONFIG_CHANGED:
                    parseCarrierConfigs();
                    resetAllTimers();
                    transitionToCurrentState();
                    break;
                case EVENT_PRIMARY_TIMER_EXPIRED:
                    transitionWithSecondaryTimerTo((IState) msg.obj);
                    break;
                case EVENT_SECONDARY_TIMER_EXPIRED:
                    mIsSecondaryTimerActive = false;
                    mSecondaryTimerState = "";
                    updateTimers();
                    updateOverrideNetworkType();
                    break;
                case EVENT_RADIO_OFF_OR_UNAVAILABLE:
                    resetAllTimers();
                    transitionTo(mLegacyState);
                    break;
                default:
                    throw new RuntimeException("Received invalid event: " + msg.what);
            }
            return HANDLED;
        }
    }

    private final DefaultState mDefaultState = new DefaultState();

    /**
     * Device does not have NR available, due to any of the below reasons:
     * <ul>
     *   <li> LTE cell does not support EN-DC
     *   <li> LTE cell supports EN-DC, but the use of NR is restricted
     *   <li> Data network type is not LTE, NR NSA, or NR SA
     * </ul>
     * This is the initial state.
     */
    private final class LegacyState extends State {
        private Boolean mIsNrRestricted = false;

        @Override
        public void enter() {
            if (DBG) log("Entering LegacyState");
            updateTimers();
            updateOverrideNetworkType();
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mIsNrRestricted = isNrRestricted();
                mPreviousState = getName();
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("LegacyState: process " + getEventName(msg.what));
            updateTimers();
            int rat = getDataNetworkType();
            switch (msg.what) {
                case EVENT_DATA_RAT_CHANGED:
                    if (rat == TelephonyManager.NETWORK_TYPE_NR || isLte(rat) && isNrConnected()) {
                        transitionTo(mNrConnectedState);
                    } else if (isLte(rat) && isNrNotRestricted()) {
                        transitionWithTimerTo(isPhysicalLinkActive()
                                ? mLteConnectedState : mIdleState);
                    } else {
                        if (!isLte(rat)) {
                            // Rat is 3G or 2G, and it doesn't need NR timer.
                            resetAllTimers();
                        }
                        updateOverrideNetworkType();
                    }
                    mIsNrRestricted = isNrRestricted();
                    break;
                case EVENT_NR_STATE_CHANGED:
                    if (isNrConnected()) {
                        transitionTo(mNrConnectedState);
                    } else if (isLte(rat) && isNrNotRestricted()) {
                        transitionWithTimerTo(isPhysicalLinkActive()
                                ? mLteConnectedState : mIdleState);
                    } else if (isLte(rat) && isNrRestricted()) {
                        updateOverrideNetworkType();
                    }
                    mIsNrRestricted = isNrRestricted();
                    break;
                case EVENT_NR_FREQUENCY_CHANGED:
                    // ignored
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED:
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkState = getPhysicalLinkStateFromPhysicalChannelConfig();
                        if (mIsTimerResetEnabledForLegacyStateRRCIdle && !isPhysicalLinkActive()) {
                            resetAllTimers();
                        }
                    }
                    // Update in case of LTE/LTE+ switch
                    updateOverrideNetworkType();
                    break;
                case EVENT_PHYSICAL_LINK_STATE_CHANGED:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    mPhysicalLinkState = (int) ar.result;
                    if (mIsTimerResetEnabledForLegacyStateRRCIdle && !isPhysicalLinkActive()) {
                        resetAllTimers();
                        updateOverrideNetworkType();
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
            return HANDLED;
        }

        @Override
        public String getName() {
            return mIsNrRestricted  ? STATE_RESTRICTED : STATE_LEGACY;
        }
    }

    private final LegacyState mLegacyState = new LegacyState();

    /**
     * Device does not have any physical connection with the cell (RRC idle).
     */
    private final class IdleState extends State {
        @Override
        public void enter() {
            if (DBG) log("Entering IdleState");
            updateTimers();
            updateOverrideNetworkType();
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("IdleState: process " + getEventName(msg.what));
            updateTimers();
            switch (msg.what) {
                case EVENT_DATA_RAT_CHANGED:
                    int rat = getDataNetworkType();
                    if (rat == TelephonyManager.NETWORK_TYPE_NR) {
                        transitionTo(mNrConnectedState);
                    } else if (!isLte(rat) || !isNrNotRestricted()) {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_NR_STATE_CHANGED:
                    if (isNrConnected()) {
                        transitionTo(mNrConnectedState);
                    } else if (!isNrNotRestricted()) {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_NR_FREQUENCY_CHANGED:
                    // ignore
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED:
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkState = getPhysicalLinkStateFromPhysicalChannelConfig();
                        if (isNrNotRestricted()) {
                            // NOT_RESTRICTED_RRC_IDLE -> NOT_RESTRICTED_RRC_CON
                            if (isPhysicalLinkActive()) {
                                transitionWithTimerTo(mLteConnectedState);
                                break;
                            }
                        } else {
                            log("NR state changed. Sending EVENT_NR_STATE_CHANGED");
                            sendMessage(EVENT_NR_STATE_CHANGED);
                        }
                    }
                    // Update in case of LTE/LTE+ switch
                    updateOverrideNetworkType();
                    break;
                case EVENT_PHYSICAL_LINK_STATE_CHANGED:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    mPhysicalLinkState = (int) ar.result;
                    if (isNrNotRestricted()) {
                        // NOT_RESTRICTED_RRC_IDLE -> NOT_RESTRICTED_RRC_CON
                        if (isPhysicalLinkActive()) {
                            transitionWithTimerTo(mLteConnectedState);
                        }
                    } else {
                        log("NR state changed. Sending EVENT_NR_STATE_CHANGED");
                        sendMessage(EVENT_NR_STATE_CHANGED);
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
            return HANDLED;
        }

        @Override
        public String getName() {
            return STATE_NOT_RESTRICTED_RRC_IDLE;
        }
    }

    private final IdleState mIdleState = new IdleState();

    /**
     * Device is connected to LTE as the primary cell (RRC connected).
     */
    private final class LteConnectedState extends State {
        @Override
        public void enter() {
            if (DBG) log("Entering LteConnectedState");
            updateTimers();
            updateOverrideNetworkType();
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("LteConnectedState: process " + getEventName(msg.what));
            updateTimers();
            switch (msg.what) {
                case EVENT_DATA_RAT_CHANGED:
                    int rat = getDataNetworkType();
                    if (rat == TelephonyManager.NETWORK_TYPE_NR) {
                        transitionTo(mNrConnectedState);
                    } else if (!isLte(rat) || !isNrNotRestricted()) {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_NR_STATE_CHANGED:
                    if (isNrConnected()) {
                        transitionTo(mNrConnectedState);
                    } else if (!isNrNotRestricted()) {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_NR_FREQUENCY_CHANGED:
                    // ignore
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED:
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkState = getPhysicalLinkStateFromPhysicalChannelConfig();
                        if (isNrNotRestricted()) {
                            // NOT_RESTRICTED_RRC_CON -> NOT_RESTRICTED_RRC_IDLE
                            if (!isPhysicalLinkActive()) {
                                transitionWithTimerTo(mIdleState);
                                break;
                            }
                        } else {
                            log("NR state changed. Sending EVENT_NR_STATE_CHANGED");
                            sendMessage(EVENT_NR_STATE_CHANGED);
                        }
                    }
                    // Update in case of LTE/LTE+ switch
                    updateOverrideNetworkType();
                    break;
                case EVENT_PHYSICAL_LINK_STATE_CHANGED:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    mPhysicalLinkState = (int) ar.result;
                    if (isNrNotRestricted()) {
                        // NOT_RESTRICTED_RRC_CON -> NOT_RESTRICTED_RRC_IDLE
                        if (!isPhysicalLinkActive()) {
                            transitionWithTimerTo(mIdleState);
                        }
                    } else {
                        log("NR state changed. Sending EVENT_NR_STATE_CHANGED");
                        sendMessage(EVENT_NR_STATE_CHANGED);
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
            return HANDLED;
        }

        @Override
        public String getName() {
            return STATE_NOT_RESTRICTED_RRC_CON;
        }
    }

    private final LteConnectedState mLteConnectedState = new LteConnectedState();

    /**
     * Device is connected to 5G NR as the secondary cell.
     */
    private final class NrConnectedState extends State {
        private Boolean mIsNrAdvanced = false;

        @Override
        public void enter() {
            if (DBG) log("Entering NrConnectedState");
            updateTimers();
            updateOverrideNetworkType();
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mIsNrAdvanced = isNrAdvanced();
                mPreviousState = getName();
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("NrConnectedState: process " + getEventName(msg.what));
            updateTimers();
            int rat = getDataNetworkType();
            switch (msg.what) {
                case EVENT_DATA_RAT_CHANGED:
                    if (rat == TelephonyManager.NETWORK_TYPE_NR || isLte(rat) && isNrConnected()) {
                        updateOverrideNetworkType();
                    } else if (isLte(rat) && isNrNotRestricted()) {
                        transitionWithTimerTo(isPhysicalLinkActive()
                                ? mLteConnectedState : mIdleState);
                    } else {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_NR_STATE_CHANGED:
                    if (isLte(rat) && isNrNotRestricted()) {
                        transitionWithTimerTo(isPhysicalLinkActive()
                                ? mLteConnectedState : mIdleState);
                    } else if (rat != TelephonyManager.NETWORK_TYPE_NR && !isNrConnected()) {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_PCO_DATA_CHANGED:
                    handlePcoData((AsyncResult) msg.obj);
                    break;
                case EVENT_NR_FREQUENCY_CHANGED:
                case EVENT_PHYSICAL_CHANNEL_CONFIG_CHANGED:
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkState = getPhysicalLinkStateFromPhysicalChannelConfig();
                    }
                    updateNrAdvancedState();
                    break;
                case EVENT_PHYSICAL_LINK_STATE_CHANGED:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    mPhysicalLinkState = (int) ar.result;
                    if (!isNrConnected()) {
                        log("NR state changed. Sending EVENT_NR_STATE_CHANGED");
                        sendMessage(EVENT_NR_STATE_CHANGED);
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
            return HANDLED;
        }

        @Override
        public String getName() {
            return mIsNrAdvanced ? STATE_CONNECTED_NR_ADVANCED : STATE_CONNECTED;
        }

        private void updateNrAdvancedState() {
            if (!isNrConnected()) {
                log("NR state changed. Sending EVENT_NR_STATE_CHANGED");
                sendMessage(EVENT_NR_STATE_CHANGED);
                return;
            }
            if (!isNrAdvanced()) {
                // STATE_CONNECTED_NR_ADVANCED -> STATE_CONNECTED
                transitionWithTimerTo(mNrConnectedState);
            } else {
                // STATE_CONNECTED -> STATE_CONNECTED_NR_ADVANCED
                transitionTo(mNrConnectedState);
            }
            mIsNrAdvanced = isNrAdvanced();
        }

        private void handlePcoData(AsyncResult ar) {
            if (ar.exception != null) {
                loge("PCO_DATA exception: " + ar.exception);
                return;
            }
            PcoData pcodata = (PcoData) ar.result;
            if (pcodata == null) {
                return;
            }
            log("EVENT_PCO_DATA_CHANGED: pco data: " + pcodata);
            DcTracker dcTracker = mPhone.getDcTracker(
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            DataConnection dc =
                    dcTracker != null ? dcTracker.getDataConnectionByContextId(pcodata.cid) : null;
            ApnSetting apnSettings = dc != null ? dc.getApnSetting() : null;
            if (apnSettings != null && apnSettings.canHandleType(ApnSetting.TYPE_DEFAULT)
                    && mNrAdvancedCapablePcoId > 0
                    && pcodata.pcoId == mNrAdvancedCapablePcoId
            ) {
                log("EVENT_PCO_DATA_CHANGED: Nr Advanced is allowed by PCO.");
                mIsNrAdvancedAllowedByPco = pcodata.contents[0] == 1;
                updateNrAdvancedState();
            }
        }
    }

    private final NrConnectedState mNrConnectedState = new NrConnectedState();

    private void transitionWithTimerTo(IState destState) {
        String destName = destState.getName();
        OverrideTimerRule rule = mOverrideTimerRules.get(mPreviousState);
        if (rule != null && rule.getTimer(destName) > 0) {
            if (DBG) log("Primary timer started for state: " + mPreviousState);
            mPrimaryTimerState = mPreviousState;
            mPreviousState = getCurrentState().getName();
            mIsPrimaryTimerActive = true;
            sendMessageDelayed(EVENT_PRIMARY_TIMER_EXPIRED, destState,
                    rule.getTimer(destName) * 1000);
        }
        transitionTo(destState);
    }

    private void transitionWithSecondaryTimerTo(IState destState) {
        String currentName = getCurrentState().getName();
        OverrideTimerRule rule = mOverrideTimerRules.get(mPrimaryTimerState);
        if (rule != null && rule.getSecondaryTimer(currentName) > 0) {
            if (DBG) log("Secondary timer started for state: " + currentName);
            mSecondaryTimerState = currentName;
            mPreviousState = currentName;
            mIsSecondaryTimerActive = true;
            sendMessageDelayed(EVENT_SECONDARY_TIMER_EXPIRED, destState,
                    rule.getSecondaryTimer(currentName) * 1000);
        }
        mIsPrimaryTimerActive = false;
        transitionTo(getCurrentState());
    }

    private void transitionToCurrentState() {
        int dataRat = getDataNetworkType();
        IState transitionState;
        if (dataRat == TelephonyManager.NETWORK_TYPE_NR || isNrConnected()) {
            transitionState = mNrConnectedState;
            mPreviousState = isNrAdvanced() ? STATE_CONNECTED_NR_ADVANCED : STATE_CONNECTED;
        } else if (isLte(dataRat) && isNrNotRestricted()) {
            if (isPhysicalLinkActive()) {
                transitionState = mLteConnectedState;
                mPreviousState = STATE_NOT_RESTRICTED_RRC_CON;
            } else {
                transitionState = mIdleState;
                mPreviousState = STATE_NOT_RESTRICTED_RRC_IDLE;
            }
        } else {
            transitionState = mLegacyState;
            mPreviousState = isNrRestricted() ? STATE_RESTRICTED : STATE_LEGACY;
        }
        if (!transitionState.equals(getCurrentState())) {
            transitionTo(transitionState);
        } else {
            updateOverrideNetworkType();
        }
    }

    private void updateTimers() {
        String currentState = getCurrentState().getName();

        if (mIsPrimaryTimerActive && getOverrideNetworkType() == getCurrentOverrideNetworkType()) {
            // remove primary timer if device goes back to the original icon
            if (DBG) {
                log("Remove primary timer since icon of primary state and current icon equal: "
                        + mPrimaryTimerState);
            }
            removeMessages(EVENT_PRIMARY_TIMER_EXPIRED);
            mIsPrimaryTimerActive = false;
            mPrimaryTimerState = "";
        }

        if (mIsSecondaryTimerActive && !mSecondaryTimerState.equals(currentState)) {
            // remove secondary timer if devices is no longer in secondary timer state
            if (DBG) {
                log("Remove secondary timer since current state (" +  currentState
                        + ") is no longer secondary timer state (" + mSecondaryTimerState + ").");
            }
            removeMessages(EVENT_SECONDARY_TIMER_EXPIRED);
            mIsSecondaryTimerActive = false;
            mSecondaryTimerState = "";
        }

        if (currentState.equals(STATE_CONNECTED_NR_ADVANCED)) {
            resetAllTimers();
        }

        int rat = getDataNetworkType();
        if (!isLte(rat) && rat != TelephonyManager.NETWORK_TYPE_NR) {
            // Rat is 3G or 2G, and it doesn't need NR timer.
            resetAllTimers();
        }
    }

    private void resetAllTimers() {
        if (DBG) {
            log("Remove all timers");
        }
        removeMessages(EVENT_PRIMARY_TIMER_EXPIRED);
        removeMessages(EVENT_SECONDARY_TIMER_EXPIRED);
        mIsPrimaryTimerActive = false;
        mIsSecondaryTimerActive = false;
        mPrimaryTimerState = "";
        mSecondaryTimerState = "";
    }

    /**
     * Private class defining timer rules between states to prevent flickering. These rules are
     * created in {@link #parseCarrierConfigs()} based on various carrier configs.
     */
    private class OverrideTimerRule {
        /** The 5G state this timer rule applies for. See {@link #ALL_STATES}. */
        final String mState;

        /**
         * The override network type associated with this 5G state. This is the icon that will be
         * displayed on the status bar. An override type of NONE will display the LTE value instead.
         */
        final int mOverrideType;

        /**
         * A map of destination states and associated timers. If the 5G state changes from mState
         * to the destination state, keep the override type until either the primary timer expires
         * or mState is regained.
         */
        final Map<String, Integer> mPrimaryTimers;

        /**
         * A map of secondary states and associated timers. After the primary timer expires, keep
         * the override type until either the secondary timer expires or the device is no longer in
         * the secondary state.
         */
        final Map<String, Integer> mSecondaryTimers;

        OverrideTimerRule(String state, int overrideType) {
            mState = state;
            mOverrideType = overrideType;
            mPrimaryTimers = new HashMap<>();
            mSecondaryTimers = new HashMap<>();
        }

        /**
         * Add a primary timer.
         * @param destination Transitions from mState to the destination state.
         * @param duration How long to keep the override type after transition to destination state.
         */
        public void addTimer(String destination, int duration) {
            mPrimaryTimers.put(destination, duration);
        }

        /**
         * Add a secondary timer
         * @param secondaryState Stays in secondaryState after primary timer expires.
         * @param duration How long to keep the override type while in secondaryState.
         */
        public void addSecondaryTimer(String secondaryState, int duration) {
            mSecondaryTimers.put(secondaryState, duration);
        }

        /**
         * @return Primary timer duration from mState to destination state, or 0 if not defined.
         */
        public int getTimer(String destination) {
            Integer timer = mPrimaryTimers.get(destination);
            timer = timer == null ? mPrimaryTimers.get(STATE_ANY) : timer;
            return timer == null ? 0 : timer;
        }

        /**
         * @return Secondary timer duration for secondaryState, or 0 if not defined.
         */
        public int getSecondaryTimer(String secondaryState) {
            Integer secondaryTimer = mSecondaryTimers.get(secondaryState);
            secondaryTimer = secondaryTimer == null
                    ? mSecondaryTimers.get(STATE_ANY) : secondaryTimer;
            return secondaryTimer == null ? 0 : secondaryTimer;
        }

        @Override
        public String toString() {
            return "{mState=" + mState
                    + ", mOverrideType="
                    + TelephonyDisplayInfo.overrideNetworkTypeToString(mOverrideType)
                    + ", mPrimaryTimers=" + mPrimaryTimers
                    + ", mSecondaryTimers=" + mSecondaryTimers + "}";
        }
    }

    private boolean isNrConnected() {
        return mPhone.getServiceState().getNrState() == NetworkRegistrationInfo.NR_STATE_CONNECTED;
    }

    private boolean isNrNotRestricted() {
        return mPhone.getServiceState().getNrState()
                == NetworkRegistrationInfo.NR_STATE_NOT_RESTRICTED;
    }

    private boolean isNrRestricted() {
        return mPhone.getServiceState().getNrState()
                == NetworkRegistrationInfo.NR_STATE_RESTRICTED;
    }

    private boolean isNrAdvanced() {
        return isNrAdvancedCapable() && (isNrMmwave() || isAdditionalNrAdvancedBand());
    }

    private boolean isNrMmwave() {
        return mPhone.getServiceState().getNrFrequencyRange()
                == ServiceState.FREQUENCY_RANGE_MMWAVE;
    }

    private boolean isAdditionalNrAdvancedBand() {
        List<PhysicalChannelConfig> physicalChannelConfigList =
                mPhone.getServiceStateTracker().getPhysicalChannelConfigList();
        if (ArrayUtils.isEmpty(mAdditionalNrAdvancedBandsList)
                || physicalChannelConfigList == null) {
            return false;
        }
        for (PhysicalChannelConfig item : physicalChannelConfigList) {
            if (item.getNetworkType() == TelephonyManager.NETWORK_TYPE_NR
                    && ArrayUtils.contains(mAdditionalNrAdvancedBandsList, item.getBand())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNrAdvancedCapable() {
        if (mNrAdvancedCapablePcoId > 0) {
            return mIsNrAdvancedAllowedByPco;
        }
        return true;
    }

    private boolean isLte(int rat) {
        return rat == TelephonyManager.NETWORK_TYPE_LTE
                || rat == TelephonyManager.NETWORK_TYPE_LTE_CA;
    }

    private boolean isPhysicalLinkActive() {
        return mPhysicalLinkState == DcController.PHYSICAL_LINK_ACTIVE;
    }

    private int getPhysicalLinkStateFromPhysicalChannelConfig() {
        List<PhysicalChannelConfig> physicalChannelConfigList =
                mPhone.getServiceStateTracker().getPhysicalChannelConfigList();
        return (physicalChannelConfigList == null || physicalChannelConfigList.isEmpty())
                ? DcController.PHYSICAL_LINK_NOT_ACTIVE : DcController.PHYSICAL_LINK_ACTIVE;
    }

    private int getDataNetworkType() {
        NetworkRegistrationInfo nri =  mPhone.getServiceState().getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        return nri == null ? TelephonyManager.NETWORK_TYPE_UNKNOWN
                : nri.getAccessNetworkTechnology();
    }

    private String getEventName(int event) {
        try {
            return sEvents[event];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "EVENT_NOT_DEFINED";
        }
    }

    private boolean isUsingPhysicalChannelConfigForRrcDetection() {
        return mIsPhysicalChannelConfig16Supported && !mIsUsingUserDataForRrcDetection;
    }

    protected void log(String s) {
        Rlog.d(TAG, "[" + mPhone.getPhoneId() + "] " + s);
    }

    protected void loge(String s) {
        Rlog.e(TAG, "[" + mPhone.getPhoneId() + "] " + s);
    }

    @Override
    public String toString() {
        return "mOverrideTimerRules=" + mOverrideTimerRules.toString()
                + ", mLteEnhancedPattern=" + mLteEnhancedPattern
                + ", mIsPhysicalChannelConfigOn=" + mIsPhysicalChannelConfigOn
                + ", mIsPrimaryTimerActive=" + mIsPrimaryTimerActive
                + ", mIsSecondaryTimerActive=" + mIsSecondaryTimerActive
                + ", mPrimaryTimerState=" + mPrimaryTimerState
                + ", mSecondaryTimerState=" + mSecondaryTimerState
                + ", mPreviousState=" + mPreviousState
                + ", misNrAdvanced=" + isNrAdvanced();
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, " ");
        pw.print("NetworkTypeController: ");
        super.dump(fd, pw, args);
        pw.flush();
        pw.increaseIndent();
        pw.println("mSubId=" + mPhone.getSubId());
        pw.println("mOverrideTimerRules=" + mOverrideTimerRules.toString());
        pw.println("mLteEnhancedPattern=" + mLteEnhancedPattern);
        pw.println("mIsPhysicalChannelConfigOn=" + mIsPhysicalChannelConfigOn);
        pw.println("mIsPrimaryTimerActive=" + mIsPrimaryTimerActive);
        pw.println("mIsSecondaryTimerActive=" + mIsSecondaryTimerActive);
        pw.println("mIsTimerRestEnabledForLegacyStateRRCIdle="
                + mIsTimerResetEnabledForLegacyStateRRCIdle);
        pw.println("mLtePlusThresholdBandwidth=" + mLtePlusThresholdBandwidth);
        pw.println("mPrimaryTimerState=" + mPrimaryTimerState);
        pw.println("mSecondaryTimerState=" + mSecondaryTimerState);
        pw.println("mPreviousState=" + mPreviousState);
        pw.println("mPhysicalLinkState=" + mPhysicalLinkState);
        pw.println("mAdditionalNrAdvancedBandsList="
                + Arrays.toString(mAdditionalNrAdvancedBandsList));
        pw.println("mNrAdvancedCapablePcoId=" + mNrAdvancedCapablePcoId);
        pw.decreaseIndent();
        pw.flush();
    }
}
