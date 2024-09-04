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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation;
import android.telephony.CarrierConfigManager;
import android.telephony.CellInfo;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhysicalChannelConfig;
import android.telephony.ServiceState;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.LinkStatus;
import android.telephony.data.EpsQos;
import android.telephony.data.NrQos;
import android.telephony.data.QosBearerSession;
import android.text.TextUtils;

import com.android.internal.telephony.data.DataNetworkController.DataNetworkControllerCallback;
import com.android.internal.telephony.data.DataUtils;
import com.android.internal.telephony.flags.FeatureFlags;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final String STATE_CONNECTED_RRC_IDLE = "connected_rrc_idle";
    private static final String STATE_CONNECTED = "connected";
    private static final String STATE_NOT_RESTRICTED_RRC_IDLE = "not_restricted_rrc_idle";
    private static final String STATE_NOT_RESTRICTED_RRC_CON = "not_restricted_rrc_con";
    private static final String STATE_RESTRICTED = "restricted";
    private static final String STATE_ANY = "any";
    private static final String STATE_LEGACY = "legacy";
    private static final String[] ALL_STATES = {STATE_CONNECTED_NR_ADVANCED, STATE_CONNECTED,
            STATE_CONNECTED_RRC_IDLE, STATE_NOT_RESTRICTED_RRC_IDLE, STATE_NOT_RESTRICTED_RRC_CON,
            STATE_RESTRICTED, STATE_LEGACY };

    /** Stop all timers and go to current state. */
    public static final int EVENT_UPDATE = 0;
    /** Quit after processing all existing messages. */
    private static final int EVENT_QUIT = 1;
    /** Initialize all events. */
    private static final int EVENT_INITIALIZE = 2;
    /** Event for service state changed (data rat, bandwidth, NR state, NR frequency, etc). */
    private static final int EVENT_SERVICE_STATE_CHANGED = 3;
    /** Event for physical link status changed. */
    private static final int EVENT_PHYSICAL_LINK_STATUS_CHANGED = 4;
    /** Event for physical channel config indications turned on/off. */
    private static final int EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED = 5;
    /** Event for carrier configs changed. */
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 6;
    /** Event for primary timer expired. If a secondary timer exists, it will begin afterwards. */
    private static final int EVENT_PRIMARY_TIMER_EXPIRED = 7;
    /** Event for secondary timer expired. */
    private static final int EVENT_SECONDARY_TIMER_EXPIRED = 8;
    /** Event for radio off or unavailable. */
    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 9;
    /** Event for preferred network mode changed. */
    private static final int EVENT_PREFERRED_NETWORK_MODE_CHANGED = 10;
    /** Event for physical channel configs changed. */
    private static final int EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED = 11;
    /** Event for device idle mode changed, when device goes to deep sleep and pauses all timers. */
    private static final int EVENT_DEVICE_IDLE_MODE_CHANGED = 12;
    /** Event for qos sessions changed. */
    private static final int EVENT_QOS_SESSION_CHANGED = 13;

    private static final String[] sEvents = new String[EVENT_QOS_SESSION_CHANGED + 1];
    static {
        sEvents[EVENT_UPDATE] = "EVENT_UPDATE";
        sEvents[EVENT_QUIT] = "EVENT_QUIT";
        sEvents[EVENT_SERVICE_STATE_CHANGED] = "EVENT_SERVICE_STATE_CHANGED";
        sEvents[EVENT_PHYSICAL_LINK_STATUS_CHANGED] = "EVENT_PHYSICAL_LINK_STATUS_CHANGED";
        sEvents[EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED] =
                "EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED";
        sEvents[EVENT_CARRIER_CONFIG_CHANGED] = "EVENT_CARRIER_CONFIG_CHANGED";
        sEvents[EVENT_PRIMARY_TIMER_EXPIRED] = "EVENT_PRIMARY_TIMER_EXPIRED";
        sEvents[EVENT_SECONDARY_TIMER_EXPIRED] = "EVENT_SECONDARY_TIMER_EXPIRED";
        sEvents[EVENT_RADIO_OFF_OR_UNAVAILABLE] = "EVENT_RADIO_OFF_OR_UNAVAILABLE";
        sEvents[EVENT_PREFERRED_NETWORK_MODE_CHANGED] = "EVENT_PREFERRED_NETWORK_MODE_CHANGED";
        sEvents[EVENT_INITIALIZE] = "EVENT_INITIALIZE";
        sEvents[EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED] = "EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED";
        sEvents[EVENT_DEVICE_IDLE_MODE_CHANGED] = "EVENT_DEVICE_IDLE_MODE_CHANGED";
        sEvents[EVENT_QOS_SESSION_CHANGED] = "EVENT_QOS_SESSION_CHANGED";
    }

    @NonNull private final Phone mPhone;
    @NonNull private final DisplayInfoController mDisplayInfoController;
    @NonNull private final FeatureFlags mFeatureFlags;
    @NonNull private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED:
                    sendMessage(EVENT_DEVICE_IDLE_MODE_CHANGED);
                    break;
            }
        }
    };

    @NonNull private final CarrierConfigManager.CarrierConfigChangeListener
            mCarrierConfigChangeListener =
            new CarrierConfigManager.CarrierConfigChangeListener() {
                @Override
                public void onCarrierConfigChanged(int slotIndex, int subId, int carrierId,
                        int specificCarrierId) {
                    // CarrierConfigChangeListener wouldn't send notification on device unlock
                    if (slotIndex == mPhone.getPhoneId()) {
                        sendMessage(EVENT_CARRIER_CONFIG_CHANGED);
                    }
                }
            };

    @NonNull private final DataNetworkControllerCallback mDataNetworkControllerCallback =
            new DataNetworkControllerCallback(getHandler()::post) {
                @Override
                public void onQosSessionsChanged(
                        @NonNull List<QosBearerSession> qosBearerSessions) {
                    if (!mIsTimerResetEnabledOnVoiceQos) return;
                    sendMessage(obtainMessage(EVENT_QOS_SESSION_CHANGED, qosBearerSessions));
                }

                @Override
                public void onNrAdvancedCapableByPcoChanged(boolean nrAdvancedCapable) {
                    if (mNrAdvancedCapablePcoId <= 0) return;
                    log("mIsNrAdvancedAllowedByPco=" + nrAdvancedCapable);
                    mIsNrAdvancedAllowedByPco = nrAdvancedCapable;
                    sendMessage(EVENT_UPDATE);
                }

                @Override
                public void onPhysicalLinkStatusChanged(@LinkStatus int status) {
                    if (isUsingPhysicalChannelConfigForRrcDetection()) return;
                    sendMessage(obtainMessage(EVENT_PHYSICAL_LINK_STATUS_CHANGED, status));
                }
            };

    @NonNull private Map<String, OverrideTimerRule> mOverrideTimerRules = new HashMap<>();
    @NonNull private String mLteEnhancedPattern = "";
    @Annotation.OverrideNetworkType private int mOverrideNetworkType;
    private boolean mIsPhysicalChannelConfigOn;
    private boolean mIsPrimaryTimerActive;
    private boolean mIsSecondaryTimerActive;
    private long mSecondaryTimerExpireTimestamp;
    private boolean mIsTimerResetEnabledForLegacyStateRrcIdle;
    /** Carrier config to reset timers when mccmnc changes */
    private boolean mIsTimerResetEnabledOnPlmnChanges;
    /** Carrier config to reset timers when QCI(LTE) or 5QI(NR) is 1(conversational voice) */
    private boolean mIsTimerResetEnabledOnVoiceQos;
    private int mLtePlusThresholdBandwidth;
    private int mNrAdvancedThresholdBandwidth;
    private boolean mIncludeLteForNrAdvancedThresholdBandwidth;
    private boolean mRatchetPccFieldsForSameAnchorNrCell;
    @NonNull private final Set<Integer> mAdditionalNrAdvancedBands = new HashSet<>();
    @NonNull private String mPrimaryTimerState;
    @NonNull private String mSecondaryTimerState;
    // TODO(b/316425811 remove the workaround)
    private int mNrAdvancedBandsSecondaryTimer;
    @NonNull private String mPreviousState;
    @LinkStatus private int mPhysicalLinkStatus;
    private boolean mIsPhysicalChannelConfig16Supported;
    private boolean mIsNrAdvancedAllowedByPco = false;
    private int mNrAdvancedCapablePcoId = 0;
    private boolean mIsUsingUserDataForRrcDetection = false;
    private boolean mEnableNrAdvancedWhileRoaming = true;
    private boolean mIsDeviceIdleMode = false;
    private boolean mPrimaryCellChangedWhileIdle = false;

    // Cached copies below to prevent race conditions
    @NonNull private ServiceState mServiceState;
    /** Used to track link status to be DORMANT or ACTIVE */
    @Nullable private List<PhysicalChannelConfig> mPhysicalChannelConfigs;

    // Ratchet physical channel config fields to prevent 5G/5G+ flickering
    @NonNull private Set<Integer> mRatchetedNrBands = new HashSet<>();
    // TODO(b/316425811 remove the workaround)
    private boolean mLastShownNrDueToAdvancedBand = false;
    private int mRatchetedNrBandwidths = 0;
    private int mLastAnchorNrCellId = PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN;
    private boolean mDoesPccListIndicateIdle = false;

    /**
     * NetworkTypeController constructor.
     *
     * @param phone Phone object.
     * @param displayInfoController DisplayInfoController to send override network types to.
     * @param featureFlags FeatureFlags controlling what icon features are enabled.
     */
    public NetworkTypeController(Phone phone, DisplayInfoController displayInfoController,
            FeatureFlags featureFlags) {
        super(TAG, displayInfoController);
        mPhone = phone;
        mFeatureFlags = featureFlags;
        mDisplayInfoController = displayInfoController;
        mOverrideNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE;
        mIsPhysicalChannelConfigOn = true;
        mPrimaryTimerState = "";
        mSecondaryTimerState = "";
        mPreviousState = "";
        DefaultState defaultState = new DefaultState();
        addState(defaultState);
        addState(mLegacyState, defaultState);
        addState(mIdleState, defaultState);
        addState(mLteConnectedState, defaultState);
        addState(mNrIdleState, defaultState);
        addState(mNrConnectedState, defaultState);
        addState(mNrConnectedAdvancedState, defaultState);
        setInitialState(defaultState);
        start();

        mServiceState = mPhone.getServiceStateTracker().getServiceState();
        mPhysicalChannelConfigs = mPhone.getServiceStateTracker().getPhysicalChannelConfigList();

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
     * @return The current data network type, used to create TelephonyDisplayInfo in
     * DisplayInfoController.
     */
    public @Annotation.NetworkType int getDataNetworkType() {
        NetworkRegistrationInfo nri = mServiceState.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        return nri == null ? TelephonyManager.NETWORK_TYPE_UNKNOWN
                : nri.getAccessNetworkTechnology();
    }

    /**
     * @return {@code true} if either the primary or secondary 5G icon timers are active,
     * and {@code false} if neither are.
     */
    public boolean areAnyTimersActive() {
        return mIsPrimaryTimerActive || mIsSecondaryTimerActive;
    }

    private void registerForAllEvents() {
        mPhone.registerForRadioOffOrNotAvailable(getHandler(),
                EVENT_RADIO_OFF_OR_UNAVAILABLE, null);
        mPhone.registerForPreferredNetworkTypeChanged(getHandler(),
                EVENT_PREFERRED_NETWORK_MODE_CHANGED, null);
        mPhone.registerForPhysicalChannelConfig(getHandler(),
                EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED, null);
        mPhone.getServiceStateTracker().registerForServiceStateChanged(getHandler(),
                EVENT_SERVICE_STATE_CHANGED, null);
        mIsPhysicalChannelConfig16Supported = mPhone.getContext().getSystemService(
                TelephonyManager.class).isRadioInterfaceCapabilitySupported(
                TelephonyManager.CAPABILITY_PHYSICAL_CHANNEL_CONFIG_1_6_SUPPORTED);
        mPhone.getDeviceStateMonitor().registerForPhysicalChannelConfigNotifChanged(getHandler(),
                EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED, null);
        mPhone.getDataNetworkController().registerDataNetworkControllerCallback(
                mDataNetworkControllerCallback);
        IntentFilter filter = new IntentFilter();
        filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone);
        CarrierConfigManager ccm = mPhone.getContext().getSystemService(CarrierConfigManager.class);
        ccm.registerCarrierConfigChangeListener(Runnable::run, mCarrierConfigChangeListener);
    }

    private void unRegisterForAllEvents() {
        mPhone.unregisterForRadioOffOrNotAvailable(getHandler());
        mPhone.unregisterForPreferredNetworkTypeChanged(getHandler());
        mPhone.getServiceStateTracker().unregisterForServiceStateChanged(getHandler());
        mPhone.getDeviceStateMonitor().unregisterForPhysicalChannelConfigNotifChanged(getHandler());
        mPhone.getDataNetworkController().unregisterDataNetworkControllerCallback(
                mDataNetworkControllerCallback);
        mPhone.getContext().unregisterReceiver(mIntentReceiver);
        CarrierConfigManager ccm = mPhone.getContext().getSystemService(CarrierConfigManager.class);
        if (mCarrierConfigChangeListener != null) {
            ccm.unregisterCarrierConfigChangeListener(mCarrierConfigChangeListener);
        }
    }

    private void parseCarrierConfigs() {
        PersistableBundle config = CarrierConfigManager.getDefaultConfig();
        CarrierConfigManager configManager =
                mPhone.getContext().getSystemService(CarrierConfigManager.class);
        if (configManager != null) {
            PersistableBundle b = configManager.getConfigForSubId(mPhone.getSubId());
            if (b != null) {
                config = b;
            }
        }
        mLteEnhancedPattern = config.getString(
                CarrierConfigManager.KEY_SHOW_CARRIER_DATA_ICON_PATTERN_STRING);
        mIsTimerResetEnabledForLegacyStateRrcIdle = config.getBoolean(
                CarrierConfigManager.KEY_NR_TIMERS_RESET_IF_NON_ENDC_AND_RRC_IDLE_BOOL);
        mIsTimerResetEnabledOnPlmnChanges = config.getBoolean(
                CarrierConfigManager.KEY_NR_TIMERS_RESET_ON_PLMN_CHANGE_BOOL);
        mIsTimerResetEnabledOnVoiceQos = config.getBoolean(
                CarrierConfigManager.KEY_NR_TIMERS_RESET_ON_VOICE_QOS_BOOL);
        mLtePlusThresholdBandwidth = config.getInt(
                CarrierConfigManager.KEY_LTE_PLUS_THRESHOLD_BANDWIDTH_KHZ_INT);
        mNrAdvancedThresholdBandwidth = config.getInt(
                CarrierConfigManager.KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ_INT);
        mIncludeLteForNrAdvancedThresholdBandwidth = config.getBoolean(
                CarrierConfigManager.KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH_BOOL);
        mRatchetPccFieldsForSameAnchorNrCell = config.getBoolean(
                CarrierConfigManager.KEY_RATCHET_NR_ADVANCED_BANDWIDTH_IF_RRC_IDLE_BOOL);
        mEnableNrAdvancedWhileRoaming = config.getBoolean(
                CarrierConfigManager.KEY_ENABLE_NR_ADVANCED_WHILE_ROAMING_BOOL);
        mAdditionalNrAdvancedBands.clear();
        int[] additionalNrAdvancedBands = config.getIntArray(
                CarrierConfigManager.KEY_ADDITIONAL_NR_ADVANCED_BANDS_INT_ARRAY);
        if (additionalNrAdvancedBands != null) {
            Arrays.stream(additionalNrAdvancedBands).forEach(mAdditionalNrAdvancedBands::add);
        }
        mNrAdvancedCapablePcoId = config.getInt(
                CarrierConfigManager.KEY_NR_ADVANCED_CAPABLE_PCO_ID_INT);
        mIsUsingUserDataForRrcDetection = config.getBoolean(
                CarrierConfigManager.KEY_LTE_ENDC_USING_USER_DATA_FOR_RRC_DETECTION_BOOL);
        mNrAdvancedBandsSecondaryTimer = config.getInt(
                CarrierConfigManager.KEY_NR_ADVANCED_BANDS_SECONDARY_TIMER_SECONDS_INT);
        String nrIconConfiguration = config.getString(
                CarrierConfigManager.KEY_5G_ICON_CONFIGURATION_STRING);
        String overrideTimerRule = config.getString(
                CarrierConfigManager.KEY_5G_ICON_DISPLAY_GRACE_PERIOD_STRING);
        String overrideSecondaryTimerRule = config.getString(
                CarrierConfigManager.KEY_5G_ICON_DISPLAY_SECONDARY_GRACE_PERIOD_STRING);
        createTimerRules(nrIconConfiguration, overrideTimerRule, overrideSecondaryTimerRule);
        updatePhysicalChannelConfigs(
                mPhone.getServiceStateTracker().getPhysicalChannelConfigList());
        if (isUsingPhysicalChannelConfigForRrcDetection()) {
            mPhysicalLinkStatus = getPhysicalLinkStatusFromPhysicalChannelConfig();
        }
    }

    private void createTimerRules(String icons, String timers, String secondaryTimers) {
        Map<String, OverrideTimerRule> tempRules = new HashMap<>();
        if (!TextUtils.isEmpty(icons)) {
            // Format: "STATE:ICON,STATE2:ICON2"
            for (String pair : icons.trim().split(",")) {
                String[] kv = (pair.trim().toLowerCase(Locale.ROOT)).split(":");
                if (kv.length != 2) {
                    if (DBG) loge("Invalid 5G icon configuration, config = " + pair);
                    continue;
                }
                if (!mFeatureFlags.supportNrSaRrcIdle() && kv[0].equals(STATE_CONNECTED_RRC_IDLE)) {
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
                String[] kv = (triple.trim().toLowerCase(Locale.ROOT)).split(",");
                if (kv.length != 3) {
                    if (DBG) loge("Invalid 5G icon timer configuration, config = " + triple);
                    continue;
                }
                if (!mFeatureFlags.supportNrSaRrcIdle() && kv[0].equals(STATE_CONNECTED_RRC_IDLE)) {
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
                        if (!mFeatureFlags.supportNrSaRrcIdle()
                                && state.equals(STATE_CONNECTED_RRC_IDLE)) {
                            continue;
                        }
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
                String[] kv = (triple.trim().toLowerCase(Locale.ROOT)).split(",");
                if (kv.length != 3) {
                    if (DBG) {
                        loge("Invalid 5G icon secondary timer configuration, config = " + triple);
                    }
                    continue;
                }
                if (kv[0].equals(STATE_CONNECTED_RRC_IDLE) && !mFeatureFlags.supportNrSaRrcIdle()) {
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
                        if (state.equals(STATE_CONNECTED_RRC_IDLE)
                                && !mFeatureFlags.supportNrSaRrcIdle()) {
                            continue;
                        }
                        OverrideTimerRule node = tempRules.get(state);
                        node.addSecondaryTimer(kv[1], duration);
                    }
                } else {
                    OverrideTimerRule node = tempRules.get(kv[0]);
                    node.addSecondaryTimer(kv[1], duration);
                }
            }
        }

        // TODO: Remove this workaround to make STATE_CONNECTED_RRC_IDLE backwards compatible with
        //  STATE_CONNECTED once carrier configs are updated.
        if (mFeatureFlags.supportNrSaRrcIdle()) {
            OverrideTimerRule nrRules = tempRules.get(STATE_CONNECTED);
            if (!tempRules.get(STATE_CONNECTED_RRC_IDLE).isDefined() && nrRules.isDefined()) {
                OverrideTimerRule nrIdleRules =
                        new OverrideTimerRule(STATE_CONNECTED_RRC_IDLE, nrRules.mOverrideType);
                for (Map.Entry<String, Integer> entry : nrIdleRules.mPrimaryTimers.entrySet()) {
                    nrIdleRules.addTimer(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, Integer> entry : nrIdleRules.mSecondaryTimers.entrySet()) {
                    nrIdleRules.addSecondaryTimer(entry.getKey(), entry.getValue());
                }
                tempRules.put(STATE_CONNECTED_RRC_IDLE, nrIdleRules);
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
                && mServiceState.getNrState() != NetworkRegistrationInfo.NR_STATE_NONE;
        boolean nrSa = dataNetworkType == TelephonyManager.NETWORK_TYPE_NR;

        // NR display is not accurate when physical channel config notifications are off
        if (mIsPhysicalChannelConfigOn && (nrNsa || nrSa)) {
            // Process NR display network type
            displayNetworkType = getNrDisplayType(nrSa);
            if (displayNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE && !nrSa) {
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
        if (isNrSa) {
            if (isNrAdvanced()) {
                keys.add(STATE_CONNECTED_NR_ADVANCED);
            }
        } else {
            switch (mServiceState.getNrState()) {
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
                || mServiceState.isUsingCarrierAggregation())
                && IntStream.of(mServiceState.getCellBandwidths()).sum()
                > mLtePlusThresholdBandwidth) {
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
        for (String opName : new String[] {mServiceState.getOperatorAlphaLongRaw(),
                mServiceState.getOperatorAlphaShortRaw()}) {
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
            AsyncResult ar;
            if (DBG) log("DefaultState: process " + getEventName(msg.what));
            switch (msg.what) {
                case EVENT_UPDATE:
                    resetAllTimers();
                    transitionToCurrentState();
                    break;
                case EVENT_QUIT:
                    if (DBG) log("Reset timers on state machine quitting.");
                    resetAllTimers();
                    unRegisterForAllEvents();
                    quit();
                    break;
                case EVENT_INITIALIZE:
                    // The reason that we do it here is that the work below requires other modules
                    // (e.g. DataNetworkController, ServiceStateTracker), which are not created
                    // when NetworkTypeController is created.
                    registerForAllEvents();
                    parseCarrierConfigs();
                    break;
                case EVENT_SERVICE_STATE_CHANGED:
                    onServiceStateChanged();
                    transitionToCurrentState();
                    break;
                case EVENT_PHYSICAL_LINK_STATUS_CHANGED:
                    mPhysicalLinkStatus = msg.arg1;
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIG_NOTIF_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    mIsPhysicalChannelConfigOn = (boolean) ar.result;
                    if (DBG) {
                        log("mIsPhysicalChannelConfigOn changed to: " + mIsPhysicalChannelConfigOn);
                    }
                    if (!mIsPhysicalChannelConfigOn) {
                        if (DBG) {
                            log("Reset timers since physical channel config indications are off.");
                        }
                        resetAllTimers();
                    }
                    transitionToCurrentState();
                    break;
                case EVENT_CARRIER_CONFIG_CHANGED:
                    parseCarrierConfigs();
                    if (DBG) log("Reset timers since carrier configurations changed.");
                    resetAllTimers();
                    transitionToCurrentState();
                    break;
                case EVENT_PRIMARY_TIMER_EXPIRED:
                    if (DBG) log("Primary timer expired for state: " + mPrimaryTimerState);
                    transitionWithSecondaryTimerTo((IState) msg.obj);
                    break;
                case EVENT_SECONDARY_TIMER_EXPIRED:
                    if (DBG) log("Secondary timer expired for state: " + mSecondaryTimerState);
                    mIsSecondaryTimerActive = false;
                    mSecondaryTimerExpireTimestamp = 0;
                    mSecondaryTimerState = "";
                    updateTimers();
                    mLastShownNrDueToAdvancedBand = false;
                    updateOverrideNetworkType();
                    break;
                case EVENT_RADIO_OFF_OR_UNAVAILABLE:
                    if (DBG) log("Reset timers since radio is off or unavailable.");
                    resetAllTimers();
                    mRatchetedNrBands.clear();
                    mRatchetedNrBandwidths = 0;
                    mLastAnchorNrCellId = PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN;
                    mDoesPccListIndicateIdle = false;
                    mPhysicalChannelConfigs = null;
                    transitionTo(mLegacyState);
                    break;
                case EVENT_PREFERRED_NETWORK_MODE_CHANGED:
                    if (DBG) log("Reset timers since preferred network mode changed.");
                    resetAllTimers();
                    transitionToCurrentState();
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updatePhysicalChannelConfigs((List<PhysicalChannelConfig>) ar.result);
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkStatus = getPhysicalLinkStatusFromPhysicalChannelConfig();
                    }
                    transitionToCurrentState();
                    break;
                case EVENT_DEVICE_IDLE_MODE_CHANGED:
                    PowerManager pm = mPhone.getContext().getSystemService(PowerManager.class);
                    mIsDeviceIdleMode = pm.isDeviceIdleMode();
                    if (DBG) {
                        log("mIsDeviceIdleMode changed to: " + mIsDeviceIdleMode);
                    }
                    if (mIsDeviceIdleMode) {
                        if (DBG) log("Reset timers since device is in idle mode.");
                        resetAllTimers();
                    }
                    transitionToCurrentState();
                    break;
                case EVENT_QOS_SESSION_CHANGED:
                    List<QosBearerSession> qosBearerSessions = (List<QosBearerSession>) msg.obj;
                    boolean inVoiceCall = false;
                    for (QosBearerSession session : qosBearerSessions) {
                        // TS 23.203 23.501 - 1 means conversational voice
                        if (session.getQos() instanceof EpsQos qos) {
                            inVoiceCall = qos.getQci() == 1;
                        } else if (session.getQos() instanceof NrQos qos) {
                            inVoiceCall = qos.get5Qi() == 1;
                        }
                        if (inVoiceCall) {
                            if (DBG) log("Device in voice call, reset all timers");
                            resetAllTimers();
                            transitionToCurrentState();
                            break;
                        }
                    }
                    break;
                default:
                    throw new RuntimeException("Received invalid event: " + msg.what);
            }
            return HANDLED;
        }
    }

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
        private boolean mIsNrRestricted = false;

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
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onServiceStateChanged();
                    // fallthrough
                case EVENT_UPDATE:
                    int rat = getDataNetworkType();
                    if (rat == TelephonyManager.NETWORK_TYPE_NR || isLte(rat) && isNrConnected()) {
                        if (isNrAdvanced()) {
                            transitionTo(mNrConnectedAdvancedState);
                        } else {
                            transitionTo(isPhysicalLinkActive()
                                    || !mFeatureFlags.supportNrSaRrcIdle()
                                    ? mNrConnectedState : mNrIdleState);
                        }
                    } else if (isLte(rat) && isNrNotRestricted()) {
                        transitionWithTimerTo(isPhysicalLinkActive()
                                ? mLteConnectedState : mIdleState);
                    } else {
                        if (!isLte(rat)) {
                            if (DBG) log("Reset timers since 2G and 3G don't need NR timers.");
                            resetAllTimers();
                        }
                        updateOverrideNetworkType();
                    }
                    mIsNrRestricted = isNrRestricted();
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updatePhysicalChannelConfigs((List<PhysicalChannelConfig>) ar.result);
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkStatus = getPhysicalLinkStatusFromPhysicalChannelConfig();
                        if (mIsTimerResetEnabledForLegacyStateRrcIdle && !isPhysicalLinkActive()) {
                            if (DBG) log("Reset timers since timer reset is enabled for RRC idle.");
                            resetAllTimers();
                            updateOverrideNetworkType();
                        }
                    }
                    break;
                case EVENT_PHYSICAL_LINK_STATUS_CHANGED:
                    mPhysicalLinkStatus = msg.arg1;
                    if (mIsTimerResetEnabledForLegacyStateRrcIdle && !isPhysicalLinkActive()) {
                        if (DBG) log("Reset timers since timer reset is enabled for RRC idle.");
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
            return mIsNrRestricted ? STATE_RESTRICTED : STATE_LEGACY;
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
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onServiceStateChanged();
                    // fallthrough
                case EVENT_UPDATE:
                    int rat = getDataNetworkType();
                    if (rat == TelephonyManager.NETWORK_TYPE_NR
                            || (isLte(rat) && isNrConnected())) {
                        if (isNrAdvanced()) {
                            transitionTo(mNrConnectedAdvancedState);
                        } else {
                            transitionTo(isPhysicalLinkActive()
                                    || !mFeatureFlags.supportNrSaRrcIdle()
                                    ? mNrConnectedState : mNrIdleState);
                        }
                    } else if (!isLte(rat) || !isNrNotRestricted()) {
                        transitionWithTimerTo(mLegacyState);
                    } else {
                        if (isPhysicalLinkActive()) {
                            transitionWithTimerTo(mLteConnectedState);
                        } else {
                            // Update in case the override network type changed
                            updateOverrideNetworkType();
                        }
                    }
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updatePhysicalChannelConfigs((List<PhysicalChannelConfig>) ar.result);
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkStatus = getPhysicalLinkStatusFromPhysicalChannelConfig();
                        if (isPhysicalLinkActive()) {
                            transitionWithTimerTo(mLteConnectedState);
                        } else {
                            log("Reevaluating state due to link status changed.");
                            sendMessage(EVENT_UPDATE);
                        }
                    }
                    break;
                case EVENT_PHYSICAL_LINK_STATUS_CHANGED:
                    mPhysicalLinkStatus = msg.arg1;
                    if (isPhysicalLinkActive()) {
                        transitionWithTimerTo(mLteConnectedState);
                    } else {
                        log("Reevaluating state due to link status changed.");
                        sendMessage(EVENT_UPDATE);
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
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onServiceStateChanged();
                    // fallthrough
                case EVENT_UPDATE:
                    int rat = getDataNetworkType();
                    if (rat == TelephonyManager.NETWORK_TYPE_NR
                            || (isLte(rat) && isNrConnected())) {
                        if (isNrAdvanced()) {
                            transitionTo(mNrConnectedAdvancedState);
                        } else {
                            transitionTo(isPhysicalLinkActive()
                                    || !mFeatureFlags.supportNrSaRrcIdle()
                                    ? mNrConnectedState : mNrIdleState);
                        }
                    } else if (!isLte(rat) || !isNrNotRestricted()) {
                        transitionWithTimerTo(mLegacyState);
                    } else {
                        if (!isPhysicalLinkActive()) {
                            transitionWithTimerTo(mIdleState);
                        } else {
                            // Update in case the override network type changed
                            updateOverrideNetworkType();
                        }
                    }
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updatePhysicalChannelConfigs((List<PhysicalChannelConfig>) ar.result);
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkStatus = getPhysicalLinkStatusFromPhysicalChannelConfig();
                        if (!isPhysicalLinkActive()) {
                            transitionWithTimerTo(mIdleState);
                        } else {
                            log("Reevaluating state due to link status changed.");
                            sendMessage(EVENT_UPDATE);
                        }
                    }
                    break;
                case EVENT_PHYSICAL_LINK_STATUS_CHANGED:
                    mPhysicalLinkStatus = msg.arg1;
                    if (!isPhysicalLinkActive()) {
                        transitionWithTimerTo(mIdleState);
                    } else {
                        log("Reevaluating state due to link status changed.");
                        sendMessage(EVENT_UPDATE);
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
     * Device is connected to 5G NR as the primary or secondary cell but not actively using data.
     */
    private final class NrIdleState extends State {
        @Override
        public void enter() {
            if (DBG) log("Entering NrIdleState");
            updateTimers();
            updateOverrideNetworkType();
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("NrIdleState: process " + getEventName(msg.what));
            updateTimers();
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onServiceStateChanged();
                    // fallthrough
                case EVENT_UPDATE:
                    int rat = getDataNetworkType();
                    if (rat == TelephonyManager.NETWORK_TYPE_NR
                            || (isLte(rat) && isNrConnected())) {
                        if (isNrAdvanced()) {
                            transitionTo(mNrConnectedAdvancedState);
                        } else if (isPhysicalLinkActive()) {
                            transitionWithTimerTo(mNrConnectedState);
                        } else {
                            // Update in case the override network type changed
                            updateOverrideNetworkType();
                        }
                    } else if (isLte(rat) && isNrNotRestricted()) {
                        transitionWithTimerTo(isPhysicalLinkActive()
                                ? mLteConnectedState : mIdleState);
                    } else {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updatePhysicalChannelConfigs((List<PhysicalChannelConfig>) ar.result);
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkStatus = getPhysicalLinkStatusFromPhysicalChannelConfig();
                    }
                    if (isPhysicalLinkActive()) {
                        if (isNrAdvanced()) {
                            transitionTo(mNrConnectedAdvancedState);
                        } else {
                            transitionWithTimerTo(mNrConnectedState);
                        }
                    } else {
                        // Update in case the override network type changed
                        updateOverrideNetworkType();
                    }
                    break;
                case EVENT_PHYSICAL_LINK_STATUS_CHANGED:
                    mPhysicalLinkStatus = msg.arg1;
                    if (isPhysicalLinkActive()) {
                        transitionWithTimerTo(mNrConnectedState);
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
            return mFeatureFlags.supportNrSaRrcIdle() ? STATE_CONNECTED_RRC_IDLE : STATE_CONNECTED;
        }
    }

    private final NrIdleState mNrIdleState = new NrIdleState();

    /**
     * Device is connected to 5G NR as the primary or secondary cell.
     */
    private final class NrConnectedState extends State {
        @Override
        public void enter() {
            if (DBG) log("Entering NrConnectedState");
            updateTimers();
            updateOverrideNetworkType();
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("NrConnectedState: process " + getEventName(msg.what));
            updateTimers();
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onServiceStateChanged();
                    // fallthrough
                case EVENT_UPDATE:
                    int rat = getDataNetworkType();
                    if (rat == TelephonyManager.NETWORK_TYPE_NR
                            || (isLte(rat) && isNrConnected())) {
                        if (isNrAdvanced()) {
                            transitionTo(mNrConnectedAdvancedState);
                        } else if (!isPhysicalLinkActive() && mFeatureFlags.supportNrSaRrcIdle()) {
                            transitionWithTimerTo(mNrIdleState);
                        } else {
                            // Update in case the override network type changed
                            updateOverrideNetworkType();
                        }
                    } else if (isLte(rat) && isNrNotRestricted()) {
                        transitionWithTimerTo(isPhysicalLinkActive()
                                ? mLteConnectedState : mIdleState);
                    } else {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updatePhysicalChannelConfigs((List<PhysicalChannelConfig>) ar.result);
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkStatus = getPhysicalLinkStatusFromPhysicalChannelConfig();
                    }
                    if (!isPhysicalLinkActive() && mFeatureFlags.supportNrSaRrcIdle()) {
                        transitionWithTimerTo(mNrIdleState);
                    } else if (isNrAdvanced()) {
                        transitionTo(mNrConnectedAdvancedState);
                    }
                    break;
                case EVENT_PHYSICAL_LINK_STATUS_CHANGED:
                    mPhysicalLinkStatus = msg.arg1;
                    if (!isPhysicalLinkActive() && mFeatureFlags.supportNrSaRrcIdle()) {
                        transitionWithTimerTo(mNrIdleState);
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
            return STATE_CONNECTED;
        }
    }

    private final NrConnectedState mNrConnectedState = new NrConnectedState();

    /**
     * Device is connected to 5G NR as the primary cell and the data rate is higher than
     * the generic 5G data rate.
     */
    private final class NrConnectedAdvancedState extends State {
        @Override
        public void enter() {
            if (DBG) log("Entering NrConnectedAdvancedState");
            updateTimers();
            updateOverrideNetworkType();
            if (!mIsPrimaryTimerActive && !mIsSecondaryTimerActive) {
                mPreviousState = getName();
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            mLastShownNrDueToAdvancedBand = isAdditionalNrAdvancedBand(mRatchetedNrBands);
            if (DBG) {
                log("NrConnectedAdvancedState: process " + getEventName(msg.what)
                        + ", been using advanced band is " + mLastShownNrDueToAdvancedBand);
            }
            updateTimers();
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onServiceStateChanged();
                    // fallthrough
                case EVENT_UPDATE:
                    int rat = getDataNetworkType();
                    if (rat == TelephonyManager.NETWORK_TYPE_NR
                            || (isLte(rat) && isNrConnected())) {
                        if (isNrAdvanced()) {
                            // Update in case the override network type changed
                            updateOverrideNetworkType();
                        } else {
                            if (rat == TelephonyManager.NETWORK_TYPE_NR && mOverrideNetworkType
                                    != TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED) {
                                // manually override network type after data rat changes since
                                // timer will prevent it from being updated
                                mOverrideNetworkType =
                                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE;
                            }
                            transitionWithTimerTo(isPhysicalLinkActive()
                                    || !mFeatureFlags.supportNrSaRrcIdle()
                                    ? mNrConnectedState : mNrIdleState);
                        }
                    } else if (isLte(rat) && isNrNotRestricted()) {
                        transitionWithTimerTo(isPhysicalLinkActive()
                                ? mLteConnectedState : mIdleState);
                    } else {
                        transitionWithTimerTo(mLegacyState);
                    }
                    break;
                case EVENT_PHYSICAL_CHANNEL_CONFIGS_CHANGED:
                    ar = (AsyncResult) msg.obj;
                    updatePhysicalChannelConfigs((List<PhysicalChannelConfig>) ar.result);
                    if (isUsingPhysicalChannelConfigForRrcDetection()) {
                        mPhysicalLinkStatus = getPhysicalLinkStatusFromPhysicalChannelConfig();
                    }
                    if (!isPhysicalLinkActive() && mFeatureFlags.supportNrSaRrcIdle()) {
                        transitionWithTimerTo(mNrIdleState);
                    } else if (!isNrAdvanced()) {
                        transitionWithTimerTo(mNrConnectedState);
                    }
                    break;
                case EVENT_PHYSICAL_LINK_STATUS_CHANGED:
                    mPhysicalLinkStatus = msg.arg1;
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
            return STATE_CONNECTED_NR_ADVANCED;
        }
    }

    private final NrConnectedAdvancedState mNrConnectedAdvancedState =
            new NrConnectedAdvancedState();

    /** On service state changed. */
    private void onServiceStateChanged() {
        ServiceState ss = mPhone.getServiceStateTracker().getServiceState();
        if (mIsTimerResetEnabledOnPlmnChanges
                && !TextUtils.equals(mServiceState.getOperatorNumeric(), ss.getOperatorNumeric())) {
            log("Reset any timers due to nr_timers_reset_on_plmn_change_bool");
            resetAllTimers();
        }
        mServiceState = ss;
        if (DBG) log("ServiceState updated: " + mServiceState);
    }

    private void updatePhysicalChannelConfigs(List<PhysicalChannelConfig> physicalChannelConfigs) {
        boolean isPccListEmpty = physicalChannelConfigs == null || physicalChannelConfigs.isEmpty();
        if (isPccListEmpty && isUsingPhysicalChannelConfigForRrcDetection()) {
            // Clear mPrimaryCellChangedWhileIdle to allow later potential one-off PCI change.
            // Update link status to be DORMANT, but keep ratcheted bands.
            log("Physical channel configs updated: not updating PCC fields for empty PCC list "
                    + "indicating RRC idle.");
            mPrimaryCellChangedWhileIdle = false;
            mPhysicalChannelConfigs = physicalChannelConfigs;
            mDoesPccListIndicateIdle = true;
            return;
        }

        int anchorNrCellId = PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN;
        int anchorLteCellId = PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN;
        int nrBandwidths = 0;
        Set<Integer> nrBands = new HashSet<>();
        if (physicalChannelConfigs != null) {
            for (PhysicalChannelConfig config : physicalChannelConfigs) {
                if (config.getNetworkType() == TelephonyManager.NETWORK_TYPE_NR) {
                    if (config.getConnectionStatus() == CellInfo.CONNECTION_PRIMARY_SERVING
                            && anchorNrCellId == PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN) {
                        anchorNrCellId = config.getPhysicalCellId();
                    }
                    nrBandwidths += config.getCellBandwidthDownlinkKhz();
                    nrBands.add(config.getBand());
                } else if (config.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
                    if (config.getConnectionStatus() == CellInfo.CONNECTION_PRIMARY_SERVING
                            && anchorLteCellId == PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN) {
                        anchorLteCellId = config.getPhysicalCellId();
                    }
                    if (mIncludeLteForNrAdvancedThresholdBandwidth) {
                        nrBandwidths += config.getCellBandwidthDownlinkKhz();
                    }
                }
            }
        }

        // Update anchor NR cell from anchor LTE cell for NR NSA
        if (anchorNrCellId == PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN
                && anchorLteCellId != PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN) {
            anchorNrCellId = anchorLteCellId;
        }

        if (anchorNrCellId == PhysicalChannelConfig.PHYSICAL_CELL_ID_UNKNOWN) {
            if (!isPccListEmpty) {
                log("Ignoring physical channel config fields without an anchor NR cell, "
                        + "either due to LTE-only configs or an unspecified cell ID.");
            }
            mRatchetedNrBandwidths = 0;
            mRatchetedNrBands.clear();
        } else if (anchorNrCellId == mLastAnchorNrCellId && mRatchetPccFieldsForSameAnchorNrCell) {
            log("Ratchet physical channel config fields since anchor NR cell is the same.");
            mRatchetedNrBandwidths = Math.max(mRatchetedNrBandwidths, nrBandwidths);
            mRatchetedNrBands.addAll(nrBands);
        } else {
            if (mFeatureFlags.supportNrSaRrcIdle() && mDoesPccListIndicateIdle
                    && isUsingPhysicalChannelConfigForRrcDetection()
                    && !mPrimaryCellChangedWhileIdle
                    && !isNrAdvancedForPccFields(nrBandwidths, nrBands)) {
                log("Allow primary cell change once during RRC idle without changing state: "
                        + mLastAnchorNrCellId + " -> " + anchorNrCellId);
                mPrimaryCellChangedWhileIdle = true;
                mLastAnchorNrCellId = anchorNrCellId;
                reduceSecondaryTimerIfNeeded();
                return;
            }
            if (mRatchetPccFieldsForSameAnchorNrCell) {
                log("Not ratcheting physical channel config fields since anchor NR cell changed: "
                        + mLastAnchorNrCellId + " -> " + anchorNrCellId);
            }
            mRatchetedNrBandwidths = nrBandwidths;
            mRatchetedNrBands = nrBands;
        }

        mLastAnchorNrCellId = anchorNrCellId;
        mPhysicalChannelConfigs = physicalChannelConfigs;
        mDoesPccListIndicateIdle = false;
        if (DBG) {
            log("Physical channel configs updated: anchorNrCell=" + mLastAnchorNrCellId
                    + ", nrBandwidths=" + mRatchetedNrBandwidths + ", nrBands=" +  mRatchetedNrBands
                    + ", configs=" + mPhysicalChannelConfigs);
        }
    }

    /**
     * Called when PCI change, specifically during idle state.
     */
    private void reduceSecondaryTimerIfNeeded() {
        if (!mIsSecondaryTimerActive || mNrAdvancedBandsSecondaryTimer <= 0) return;
        // Secondary timer is active, so we must have a valid secondary rule right now.
        OverrideTimerRule secondaryRule = mOverrideTimerRules.get(mPrimaryTimerState);
        if (secondaryRule != null) {
            int secondaryDuration = secondaryRule.getSecondaryTimer(mSecondaryTimerState);
            long durationMillis = secondaryDuration * 1000L;
            if ((mSecondaryTimerExpireTimestamp - SystemClock.uptimeMillis()) > durationMillis) {
                if (DBG) log("Due to PCI change, reduce the secondary timer to " + durationMillis);
                removeMessages(EVENT_SECONDARY_TIMER_EXPIRED);
                sendMessageDelayed(EVENT_SECONDARY_TIMER_EXPIRED, mSecondaryTimerState,
                        durationMillis);
            }
        } else {
            loge("!! Secondary timer is active, but found no rule for " + mPrimaryTimerState);
        }
    }

    private void transitionWithTimerTo(IState destState) {
        String destName = destState.getName();
        if (mIsPrimaryTimerActive) {
            log("Transition without timer from " + getCurrentState().getName() + " to " + destName
                    + " due to existing " + mPrimaryTimerState + " primary timer.");
        } else {
            if (DBG) {
                log("Transition with primary timer from " + mPreviousState + " to " + destName);
            }
            OverrideTimerRule rule = mOverrideTimerRules.get(mPreviousState);
            if (!mIsDeviceIdleMode && rule != null && rule.getTimer(destName) > 0) {
                int duration = rule.getTimer(destName);
                if (DBG) log(duration + "s primary timer started for state: " + mPreviousState);
                mPrimaryTimerState = mPreviousState;
                mPreviousState = getCurrentState().getName();
                mIsPrimaryTimerActive = true;
                sendMessageDelayed(EVENT_PRIMARY_TIMER_EXPIRED, destState, duration * 1000L);
            }
        }
        transitionTo(destState);
    }

    private void transitionWithSecondaryTimerTo(IState destState) {
        String currentName = getCurrentState().getName();
        OverrideTimerRule rule = mOverrideTimerRules.get(mPrimaryTimerState);
        if (DBG) {
            log("Transition with secondary timer from " + currentName + " to "
                    + destState.getName());
        }
        if (!mIsDeviceIdleMode && rule != null && rule.getSecondaryTimer(currentName) > 0) {
            int duration = rule.getSecondaryTimer(currentName);
            if (mLastShownNrDueToAdvancedBand && mNrAdvancedBandsSecondaryTimer > 0) {
                duration = mNrAdvancedBandsSecondaryTimer;
                if (DBG) log("timer adjusted by nr_advanced_bands_secondary_timer_seconds_int");
            }
            if (DBG) log(duration + "s secondary timer started for state: " + currentName);
            mSecondaryTimerState = currentName;
            mPreviousState = currentName;
            mIsSecondaryTimerActive = true;
            long durationMillis = duration * 1000L;
            mSecondaryTimerExpireTimestamp = SystemClock.uptimeMillis() + durationMillis;
            sendMessageDelayed(EVENT_SECONDARY_TIMER_EXPIRED, destState, durationMillis);
        }
        mIsPrimaryTimerActive = false;
        transitionTo(getCurrentState());
    }

    private void transitionToCurrentState() {
        int dataRat = getDataNetworkType();
        IState transitionState;
        if (dataRat == TelephonyManager.NETWORK_TYPE_NR || (isLte(dataRat) && isNrConnected())) {
            if (!isPhysicalLinkActive() && mFeatureFlags.supportNrSaRrcIdle()) {
                transitionState = mNrIdleState;
            } else if (isNrAdvanced()) {
                transitionState = mNrConnectedAdvancedState;
            } else {
                transitionState = mNrConnectedState;
            }
        } else if (isLte(dataRat) && isNrNotRestricted()) {
            if (isPhysicalLinkActive()) {
                transitionState = mLteConnectedState;
            } else {
                transitionState = mIdleState;
            }
        } else {
            transitionState = mLegacyState;
        }
        if (!transitionState.equals(getCurrentState())) {
            mPreviousState = getCurrentState().getName();
            transitionTo(transitionState);
        } else {
            updateOverrideNetworkType();
        }
    }

    private void updateTimers() {
        if ((mPhone.getCachedAllowedNetworkTypesBitmask()
                & TelephonyManager.NETWORK_TYPE_BITMASK_NR) == 0) {
            if (DBG) log("Reset timers since NR is not allowed.");
            resetAllTimers();
            return;
        }

        String currentState = getCurrentState().getName();

        if (mIsPrimaryTimerActive && mPrimaryTimerState.equals(currentState)) {
            // remove primary timer if device goes back to the original state
            if (DBG) {
                log("Remove primary timer since primary timer state ("
                        + mPrimaryTimerState + ") was reestablished.");
            }
            removeMessages(EVENT_PRIMARY_TIMER_EXPIRED);
            mIsPrimaryTimerActive = false;
            mPrimaryTimerState = "";
            transitionToCurrentState();
            return;
        }

        if (mIsSecondaryTimerActive && !mSecondaryTimerState.equals(currentState)) {
            // remove secondary timer if devices is no longer in secondary timer state
            if (DBG) {
                log("Remove secondary timer since current state (" +  currentState
                        + ") is no longer secondary timer state (" + mSecondaryTimerState + ").");
            }
            removeMessages(EVENT_SECONDARY_TIMER_EXPIRED);
            mIsSecondaryTimerActive = false;
            mSecondaryTimerExpireTimestamp = 0;
            mSecondaryTimerState = "";
            transitionToCurrentState();
            return;
        }

        if (mIsPrimaryTimerActive || mIsSecondaryTimerActive) {
            if (currentState.equals(STATE_CONNECTED_NR_ADVANCED)) {
                if (DBG) log("Reset timers since state is NR_ADVANCED.");
                resetAllTimers();
            } else if ((currentState.equals(STATE_CONNECTED)
                    || currentState.equals(STATE_CONNECTED_RRC_IDLE))
                    && !mPrimaryTimerState.equals(STATE_CONNECTED_NR_ADVANCED)
                    && !mSecondaryTimerState.equals(STATE_CONNECTED_NR_ADVANCED)) {
                if (DBG) log("Reset non-NR advanced timers since state is NR connected/idle");
                resetAllTimers();
            } else {
                int rat = getDataNetworkType();
                if (!isLte(rat) && rat != TelephonyManager.NETWORK_TYPE_NR) {
                    if (DBG) log("Reset timers since 2G and 3G don't need NR timers.");
                    resetAllTimers();
                }
            }
        }
    }

    private void resetAllTimers() {
        removeMessages(EVENT_PRIMARY_TIMER_EXPIRED);
        removeMessages(EVENT_SECONDARY_TIMER_EXPIRED);
        mIsPrimaryTimerActive = false;
        mIsSecondaryTimerActive = false;
        mSecondaryTimerExpireTimestamp = 0;
        mPrimaryTimerState = "";
        mSecondaryTimerState = "";

        mLastShownNrDueToAdvancedBand = false;
    }

    /**
     * Private class defining timer rules between states to prevent flickering. These rules are
     * created in {@link #parseCarrierConfigs()} based on various carrier configs.
     */
    private static class OverrideTimerRule {
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

        /**
         * @return Whether timer rules have been defined for this {@link #mState}.
         */
        public boolean isDefined() {
            // TODO: Remove this method added to make STATE_CONNECTED_RRC_IDLE backwards compatible
            //  with STATE_CONNECTED once carrier configs are updated.
            return mOverrideType != TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
                    || !mPrimaryTimers.isEmpty() || !mSecondaryTimers.isEmpty();
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
        return mServiceState.getNrState() == NetworkRegistrationInfo.NR_STATE_CONNECTED;
    }

    private boolean isNrNotRestricted() {
        return mServiceState.getNrState() == NetworkRegistrationInfo.NR_STATE_NOT_RESTRICTED;
    }

    private boolean isNrRestricted() {
        return mServiceState.getNrState() == NetworkRegistrationInfo.NR_STATE_RESTRICTED;
    }

    /**
     * @return {@code true} if the device is in NR advanced mode (i.e. 5G+).
     */
    private boolean isNrAdvanced() {
        return isNrAdvancedForPccFields(mRatchetedNrBandwidths, mRatchetedNrBands);
    }

    private boolean isNrAdvancedForPccFields(int bandwidths, Set<Integer> bands) {
        // Check PCO requirement. For carriers using PCO to indicate whether the data connection is
        // NR advanced capable, mNrAdvancedCapablePcoId should be configured to non-zero.
        if (mNrAdvancedCapablePcoId > 0 && !mIsNrAdvancedAllowedByPco) {
            if (DBG) log("isNrAdvanced: not allowed by PCO for PCO ID " + mNrAdvancedCapablePcoId);
            return false;
        }

        // Check if NR advanced is enabled when the device is roaming. Some carriers disable it
        // while the device is roaming.
        if (mServiceState.getDataRoaming() && !mEnableNrAdvancedWhileRoaming) {
            if (DBG) log("isNrAdvanced: false because NR advanced is unavailable while roaming.");
            return false;
        }

        // Check if meeting minimum bandwidth requirement. For most carriers, there is no minimum
        // bandwidth requirement and mNrAdvancedThresholdBandwidth is 0.
        if (mNrAdvancedThresholdBandwidth > 0 && bandwidths < mNrAdvancedThresholdBandwidth) {
            if (DBG) {
                log("isNrAdvanced: false because bandwidths=" + bandwidths
                        + " does not meet the threshold=" + mNrAdvancedThresholdBandwidth);
            }
            return false;
        }

        // If all above tests passed, then check if the device is using millimeter wave bands or
        // carrier designated bands.
        return isNrMmwave() || isAdditionalNrAdvancedBand(bands);
    }

    private boolean isNrMmwave() {
        return mServiceState.getNrFrequencyRange() == ServiceState.FREQUENCY_RANGE_MMWAVE;
    }

    private boolean isAdditionalNrAdvancedBand(Set<Integer> bands) {
        if (mAdditionalNrAdvancedBands.isEmpty() || bands.isEmpty()) {
            if (DBG && !mAdditionalNrAdvancedBands.isEmpty()) {
                // Only log if mAdditionalNrAdvancedBands is empty to prevent log spam
                log("isAdditionalNrAdvancedBand: false because bands are empty; configs="
                        + mAdditionalNrAdvancedBands + ", bands=" + bands);
            }
            return false;
        }
        Set<Integer> intersection = new HashSet<>(mAdditionalNrAdvancedBands);
        intersection.retainAll(bands);
        return !intersection.isEmpty();
    }

    private boolean isLte(int rat) {
        return rat == TelephonyManager.NETWORK_TYPE_LTE
                || rat == TelephonyManager.NETWORK_TYPE_LTE_CA;
    }

    private boolean isPhysicalLinkActive() {
        return mPhysicalLinkStatus == DataCallResponse.LINK_STATUS_ACTIVE;
    }

    private int getPhysicalLinkStatusFromPhysicalChannelConfig() {
        return (mPhysicalChannelConfigs == null || mPhysicalChannelConfigs.isEmpty()
                || mDoesPccListIndicateIdle)
                ? DataCallResponse.LINK_STATUS_DORMANT : DataCallResponse.LINK_STATUS_ACTIVE;
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
                + ", mIsNrAdvanced=" + isNrAdvanced();
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, " ");
        pw.print("NetworkTypeController: ");
        super.dump(fd, pw, args);
        pw.flush();
        pw.increaseIndent();
        pw.println("mSubId=" + mPhone.getSubId());
        pw.println("supportNrSaRrcIdle=" + mFeatureFlags.supportNrSaRrcIdle());
        pw.println("mOverrideTimerRules=" + mOverrideTimerRules.toString());
        pw.println("mLteEnhancedPattern=" + mLteEnhancedPattern);
        pw.println("mIsPhysicalChannelConfigOn=" + mIsPhysicalChannelConfigOn);
        pw.println("mIsPrimaryTimerActive=" + mIsPrimaryTimerActive);
        pw.println("mIsSecondaryTimerActive=" + mIsSecondaryTimerActive);
        pw.println("mIsTimerResetEnabledForLegacyStateRrcIdle="
                + mIsTimerResetEnabledForLegacyStateRrcIdle);
        pw.println("mLtePlusThresholdBandwidth=" + mLtePlusThresholdBandwidth);
        pw.println("mNrAdvancedThresholdBandwidth=" + mNrAdvancedThresholdBandwidth);
        pw.println("mIncludeLteForNrAdvancedThresholdBandwidth="
                + mIncludeLteForNrAdvancedThresholdBandwidth);
        pw.println("mRatchetPccFieldsForSameAnchorNrCell=" + mRatchetPccFieldsForSameAnchorNrCell);
        pw.println("mRatchetedNrBandwidths=" + mRatchetedNrBandwidths);
        pw.println("mAdditionalNrAdvancedBandsList=" + mAdditionalNrAdvancedBands);
        pw.println("mRatchetedNrBands=" + mRatchetedNrBands);
        pw.println("mLastAnchorNrCellId=" + mLastAnchorNrCellId);
        pw.println("mDoesPccListIndicateIdle=" + mDoesPccListIndicateIdle);
        pw.println("mPrimaryTimerState=" + mPrimaryTimerState);
        pw.println("mSecondaryTimerState=" + mSecondaryTimerState);
        pw.println("mPreviousState=" + mPreviousState);
        pw.println("mPhysicalLinkStatus=" + DataUtils.linkStatusToString(mPhysicalLinkStatus));
        pw.println("mIsPhysicalChannelConfig16Supported=" + mIsPhysicalChannelConfig16Supported);
        pw.println("mIsNrAdvancedAllowedByPco=" + mIsNrAdvancedAllowedByPco);
        pw.println("mNrAdvancedCapablePcoId=" + mNrAdvancedCapablePcoId);
        pw.println("mIsUsingUserDataForRrcDetection=" + mIsUsingUserDataForRrcDetection);
        pw.println("mPrimaryCellChangedWhileIdle=" + mPrimaryCellChangedWhileIdle);
        pw.println("mEnableNrAdvancedWhileRoaming=" + mEnableNrAdvancedWhileRoaming);
        pw.println("mIsDeviceIdleMode=" + mIsDeviceIdleMode);
        pw.decreaseIndent();
        pw.flush();
    }
}
