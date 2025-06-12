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
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.content.res.Resources;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.DeviceConfig;
import android.telephony.Annotation.ApnType;
import android.telephony.Annotation.NetCapability;
import android.telephony.Annotation.NetworkType;
import android.telephony.CarrierConfigManager;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.DataNetworkController.HandoverRule;
import com.android.internal.telephony.data.DataRetryManager.DataHandoverRetryRule;
import com.android.internal.telephony.data.DataRetryManager.DataSetupRetryRule;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DataConfigManager is the source of all data related configuration from carrier config and
 * resource overlay. DataConfigManager is created to reduce the excessive access to the
 * {@link CarrierConfigManager}. All the data config will be loaded once and stored here.
 */
public class DataConfigManager extends Handler {
    /** The default timeout in ms for data network stuck in a transit state. */
    private static final int DEFAULT_NETWORK_TRANSIT_STATE_TIMEOUT_MS = 300000;

    /** Event for carrier config changed. */
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 1;

    /** Event for device config changed. */
    private static final int EVENT_DEVICE_CONFIG_CHANGED = 2;

    /** Indicates the bandwidth estimation source is from the modem. */
    private static final String BANDWIDTH_SOURCE_MODEM_STRING_VALUE = "modem";

    /** Indicates the bandwidth estimation source is from the static carrier config. */
    private static final String BANDWIDTH_SOURCE_CARRIER_CONFIG_STRING_VALUE = "carrier_config";

    /** Indicates the bandwidth estimation source is from {@link LinkBandwidthEstimator}. */
    private static final String BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR_STRING_VALUE =
            "bandwidth_estimator";

    /** Default downlink and uplink bandwidth value in kbps. */
    private static final int DEFAULT_BANDWIDTH = 14;

    /** Network type GPRS. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_GPRS = "GPRS";

    /** Network type EDGE. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_EDGE = "EDGE";

    /** Network type UMTS. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_UMTS = "UMTS";

    /** Network type CDMA. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_CDMA = "CDMA";

    /** Network type 1xRTT. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_1xRTT = "1xRTT";

    /** Network type EvDo Rev 0. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_EVDO_0 = "EvDo_0";

    /** Network type EvDo Rev A. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_EVDO_A = "EvDo_A";

    /** Network type HSDPA. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_HSDPA = "HSDPA";

    /** Network type HSUPA. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_HSUPA = "HSUPA";

    /** Network type HSPA. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_HSPA = "HSPA";

    /** Network type EvDo Rev B. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_EVDO_B = "EvDo_B";

    /** Network type eHRPD. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_EHRPD = "eHRPD";

    /** Network type iDEN. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_IDEN = "iDEN";

    /** Network type LTE. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_LTE = "LTE";

    /** Network type HSPA+. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_HSPAP = "HSPA+";

    /** Network type GSM. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_GSM = "GSM";

    /** Network type IWLAN. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_IWLAN = "IWLAN";

    /** Network type TD_SCDMA. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_TD_SCDMA = "TD_SCDMA";

    /** Network type LTE_CA. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_LTE_CA = "LTE_CA";

    /** Network type NR_NSA. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_NR_NSA = "NR_NSA";

    /** Network type NR_NSA_MMWAVE. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_NR_NSA_MMWAVE = "NR_NSA_MMWAVE";

    /** Network type NR_SA. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_NR_SA = "NR_SA";

    /** Network type NR_SA_MMWAVE. Should not be used outside of DataConfigManager. */
    private static final String DATA_CONFIG_NETWORK_TYPE_NR_SA_MMWAVE = "NR_SA_MMWAVE";

    /**
     * The delay in milliseconds to re-evaluate existing data networks for bootstrap sim data usage
     * limit.
     */
    private static final long REEVALUATE_BOOTSTRAP_SIM_DATA_USAGE_MILLIS =
            TimeUnit.SECONDS.toMillis(60);

    @StringDef(prefix = {"DATA_CONFIG_NETWORK_TYPE_"}, value = {
            DATA_CONFIG_NETWORK_TYPE_GPRS,
            DATA_CONFIG_NETWORK_TYPE_EDGE,
            DATA_CONFIG_NETWORK_TYPE_UMTS,
            DATA_CONFIG_NETWORK_TYPE_CDMA,
            DATA_CONFIG_NETWORK_TYPE_1xRTT,
            DATA_CONFIG_NETWORK_TYPE_EVDO_0,
            DATA_CONFIG_NETWORK_TYPE_EVDO_A,
            DATA_CONFIG_NETWORK_TYPE_HSDPA,
            DATA_CONFIG_NETWORK_TYPE_HSUPA,
            DATA_CONFIG_NETWORK_TYPE_HSPA,
            DATA_CONFIG_NETWORK_TYPE_EVDO_B,
            DATA_CONFIG_NETWORK_TYPE_EHRPD,
            DATA_CONFIG_NETWORK_TYPE_IDEN,
            DATA_CONFIG_NETWORK_TYPE_LTE,
            DATA_CONFIG_NETWORK_TYPE_HSPAP,
            DATA_CONFIG_NETWORK_TYPE_GSM,
            DATA_CONFIG_NETWORK_TYPE_IWLAN,
            DATA_CONFIG_NETWORK_TYPE_TD_SCDMA,
            DATA_CONFIG_NETWORK_TYPE_LTE_CA,
            DATA_CONFIG_NETWORK_TYPE_NR_NSA,
            DATA_CONFIG_NETWORK_TYPE_NR_NSA_MMWAVE,
            DATA_CONFIG_NETWORK_TYPE_NR_SA,
            DATA_CONFIG_NETWORK_TYPE_NR_SA_MMWAVE,
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface DataConfigNetworkType {}

    /** Data config update callbacks. */
    @NonNull
    private final Set<DataConfigManagerCallback> mDataConfigManagerCallbacks = new ArraySet<>();

    /** DeviceConfig key of anomaly report threshold for back to back ims release-request. */
    private static final String KEY_ANOMALY_IMS_RELEASE_REQUEST = "anomaly_ims_release_request";
    /** DeviceConfig key of anomaly report threshold for frequent setup data failure. */
    private static final String KEY_ANOMALY_SETUP_DATA_CALL_FAILURE =
            "anomaly_setup_data_call_failure";
    /** DeviceConfig key of anomaly report threshold for frequent network-unwanted call. */
    private static final String KEY_ANOMALY_NETWORK_UNWANTED = "anomaly_network_unwanted";
    /** DeviceConfig key of anomaly report threshold for invalid QNS params. */
    private static final String KEY_ANOMALY_QNS_PARAM = "anomaly_qns_param";
    /** DeviceConfig key of anomaly report threshold for DataNetwork stuck in connecting state. */
    private static final String KEY_ANOMALY_NETWORK_CONNECTING_TIMEOUT =
            "anomaly_network_connecting_timeout";
    /** DeviceConfig key of anomaly report threshold for DataNetwork stuck in disconnecting state.*/
    private static final String KEY_ANOMALY_NETWORK_DISCONNECTING_TIMEOUT =
            "anomaly_network_disconnecting_timeout";
    /** DeviceConfig key of anomaly report threshold for DataNetwork stuck in handover state. */
    private static final String KEY_ANOMALY_NETWORK_HANDOVER_TIMEOUT =
            "anomaly_network_handover_timeout";
    /** DeviceConfig key of anomaly report: True for enabling APN config invalidity detection */
    private static final String KEY_ANOMALY_APN_CONFIG_ENABLED = "anomaly_apn_config_enabled";
    /** Placeholder indicating missing Auto data switch score config, meaning out of service. */
    private static final int OUT_OF_SERVICE_AUTO_DATA_SWITCH_SCORE = 0;
    /** Anomaly report thresholds for frequent setup data call failure. */
    private EventFrequency mSetupDataCallAnomalyReportThreshold;

    /** Anomaly report thresholds for back to back release-request of IMS. */
    private EventFrequency mImsReleaseRequestAnomalyReportThreshold;

    /**
     * Anomaly report thresholds for frequent network unwanted call
     * at {@link TelephonyNetworkAgent#onNetworkUnwanted}
     */
    private EventFrequency mNetworkUnwantedAnomalyReportThreshold;

    /**
     * {@code true} if enabled anomaly detection for param when QNS wants to change preferred
     * network at {@link AccessNetworksManager}.
     */
    private boolean mIsInvalidQnsParamAnomalyReportEnabled;

    /**
     * Timeout in ms before creating an anomaly report for a DataNetwork stuck in
     * {@link DataNetwork.ConnectingState}.
     */
    private int mNetworkConnectingTimeout;

    /**
     * Timeout in ms before creating an anomaly report for a DataNetwork stuck in
     * {@link DataNetwork.DisconnectingState}.
     */
    private int mNetworkDisconnectingTimeout;

    /**
     * Timeout in ms before creating an anomaly report for a DataNetwork stuck in
     * {@link DataNetwork.HandoverState}.
     */
    private int mNetworkHandoverTimeout;

    /**
     * True if enabled anomaly detection for APN config that's read from {@link DataProfileManager}
     */
    private boolean mIsApnConfigAnomalyReportEnabled;

    @NonNull
    private final Phone mPhone;
    @NonNull
    private final String mLogTag;
    @NonNull
    private final FeatureFlags mFeatureFlags;
    @Nullable
    private final CarrierConfigManager mCarrierConfigManager;
    @NonNull
    private PersistableBundle mCarrierConfig = null;
    @NonNull
    private Resources mResources = null;

    /** The network capability priority map */
    @NonNull
    private final Map<Integer, Integer> mNetworkCapabilityPriorityMap = new ConcurrentHashMap<>();
    /** The data setup retry rules */
    @NonNull
    private final List<DataSetupRetryRule> mDataSetupRetryRules = new ArrayList<>();
    /** The data handover retry rules */
    @NonNull
    private final List<DataHandoverRetryRule> mDataHandoverRetryRules = new ArrayList<>();
    /** The metered APN types for home network */
    @NonNull
    @ApnType
    private final Set<Integer> mMeteredApnTypes = new HashSet<>();
    /** The metered APN types for roaming network */
    @NonNull
    @ApnType
    private final Set<Integer> mRoamingMeteredApnTypes = new HashSet<>();
    /** The network types that only support single data networks */
    @NonNull
    @NetworkType
    private final List<Integer> mSingleDataNetworkTypeList = new ArrayList<>();
    @NonNull
    @NetCapability
    private final Set<Integer> mCapabilitiesExemptFromSingleDataList = new HashSet<>();
    /** The network types that support temporarily not metered */
    @NonNull
    @DataConfigNetworkType
    private final Set<String> mUnmeteredNetworkTypes = new HashSet<>();
    /** The network types that support temporarily not metered when roaming */
    @NonNull
    @DataConfigNetworkType
    private final Set<String> mRoamingUnmeteredNetworkTypes = new HashSet<>();
    /** A map of network types to the downlink and uplink bandwidth values for that network type */
    @NonNull
    @DataConfigNetworkType
    private final Map<String, DataNetwork.NetworkBandwidth> mBandwidthMap =
            new ConcurrentHashMap<>();
    /** A map of network types to the TCP buffer sizes for that network type */
    @NonNull
    @DataConfigNetworkType
    private final Map<String, String> mTcpBufferSizeMap = new ConcurrentHashMap<>();
    /** Rules for handover between IWLAN and cellular network. */
    @NonNull
    private final List<HandoverRule> mHandoverRuleList = new ArrayList<>();
    /** {@code True} keep IMS network in case of moving to non VOPS area; {@code false} otherwise.*/
    private boolean mShouldKeepNetworkUpInNonVops = false;
    /** The set of network types that enable VOPS even in non VOPS area. */
    @NonNull
    @CarrierConfigManager.Ims.NetworkType
    private final List<Integer> mEnabledVopsNetworkTypesInNonVops = new ArrayList<>();
    /**
     * A map of network types to the estimated downlink values by signal strength 0 - 4 for that
     * network type
     */
    @NonNull
    @DataConfigNetworkType
    private final Map<String, int[]> mAutoDataSwitchNetworkTypeSignalMap =
            new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     */
    public DataConfigManager(@NonNull Phone phone, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags) {
        super(looper);
        mPhone = phone;
        mFeatureFlags = featureFlags;
        mLogTag = "DCM-" + mPhone.getPhoneId();
        log("DataConfigManager created.");

        mCarrierConfigManager = mPhone.getContext().getSystemService(CarrierConfigManager.class);
        // Callback send msg to handler thread, so callback itself can be executed in binder thread.
        if (mCarrierConfigManager != null) {
            mCarrierConfigManager.registerCarrierConfigChangeListener(Runnable::run,
                    (slotIndex, subId, carrierId, specificCarrierId) -> {
                        if (slotIndex == mPhone.getPhoneId()) {
                            sendEmptyMessage(EVENT_CARRIER_CONFIG_CHANGED);
                        }
                    });
        }

        // Register for device config update
        DeviceConfig.addOnPropertiesChangedListener(
                DeviceConfig.NAMESPACE_TELEPHONY, Runnable::run,
                properties -> {
                    if (TextUtils.equals(DeviceConfig.NAMESPACE_TELEPHONY,
                            properties.getNamespace())) {
                        sendEmptyMessage(EVENT_DEVICE_CONFIG_CHANGED);
                    }
                });

        // Must be called to set mCarrierConfig and mResources to non-null values
        updateCarrierConfig();
        // Must be called to set anomaly report threshold to non-null values
        updateDeviceConfig();
    }

    /**
     * The data config callback.
     */
    public static class DataConfigManagerCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public DataConfigManagerCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /** Callback on carrier config update.*/
        public void onCarrierConfigChanged() {}

        /** Callback on device config update.*/
        public void onDeviceConfigChanged() {}
    }

    /**
     * Register the callback for receiving information from {@link DataConfigManager}.
     *
     * @param callback The callback.
     */
    public void registerCallback(@NonNull DataConfigManagerCallback callback) {
        mDataConfigManagerCallbacks.add(callback);
    }

    /**
     * Unregister the callback.
     *
     * @param callback The previously registered callback.
     */
    public void unregisterCallback(@NonNull DataConfigManagerCallback callback) {
        mDataConfigManagerCallbacks.remove(callback);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_CARRIER_CONFIG_CHANGED:
                log("EVENT_CARRIER_CONFIG_CHANGED");
                updateCarrierConfig();
                mDataConfigManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                        callback::onCarrierConfigChanged));
                break;
            case EVENT_DEVICE_CONFIG_CHANGED:
                log("EVENT_DEVICE_CONFIG_CHANGED");
                updateDeviceConfig();
                mDataConfigManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                        callback::onDeviceConfigChanged));
                break;
            default:
                loge("Unexpected message " + msg.what);
        }
    }

    /** Update local properties from {@link DeviceConfig} */
    private void updateDeviceConfig() {
        DeviceConfig.Properties properties = //read all telephony properties
                DeviceConfig.getProperties(DeviceConfig.NAMESPACE_TELEPHONY);

        mImsReleaseRequestAnomalyReportThreshold = parseSlidingWindowCounterThreshold(
                properties.getString(KEY_ANOMALY_IMS_RELEASE_REQUEST, null), 0, 2);
        mNetworkUnwantedAnomalyReportThreshold = parseSlidingWindowCounterThreshold(
                properties.getString(KEY_ANOMALY_NETWORK_UNWANTED, null), 0, 12);
        mSetupDataCallAnomalyReportThreshold = parseSlidingWindowCounterThreshold(
                properties.getString(KEY_ANOMALY_SETUP_DATA_CALL_FAILURE, null), 0, 12);
        mIsInvalidQnsParamAnomalyReportEnabled = properties.getBoolean(
                KEY_ANOMALY_QNS_PARAM, false);
        mNetworkConnectingTimeout = properties.getInt(
                KEY_ANOMALY_NETWORK_CONNECTING_TIMEOUT, DEFAULT_NETWORK_TRANSIT_STATE_TIMEOUT_MS);
        mNetworkDisconnectingTimeout = properties.getInt(
                KEY_ANOMALY_NETWORK_DISCONNECTING_TIMEOUT,
                DEFAULT_NETWORK_TRANSIT_STATE_TIMEOUT_MS);
        mNetworkHandoverTimeout = properties.getInt(
                KEY_ANOMALY_NETWORK_HANDOVER_TIMEOUT, DEFAULT_NETWORK_TRANSIT_STATE_TIMEOUT_MS);
        mIsApnConfigAnomalyReportEnabled = properties.getBoolean(
                KEY_ANOMALY_APN_CONFIG_ENABLED, false);
    }

    /**
     * @return {@code true} if the configuration is carrier specific. {@code false} if the
     * configuration is the default (i.e. SIM not inserted).
     */
    public boolean isConfigCarrierSpecific() {
        return mCarrierConfig.getBoolean(CarrierConfigManager.KEY_CARRIER_CONFIG_APPLIED_BOOL);
    }

    /**
     * Update the configuration from carrier configs and resources.
     */
    private void updateCarrierConfig() {
        if (mCarrierConfigManager != null) {
            mCarrierConfig = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId());
        }
        if (mCarrierConfig == null) {
            mCarrierConfig = CarrierConfigManager.getDefaultConfig();
        }
        mResources = SubscriptionManager.getResourcesForSubId(mPhone.getContext(),
                mPhone.getSubId());

        updateNetworkCapabilityPriority();
        updateDataRetryRules();
        updateMeteredApnTypes();
        updateSingleDataNetworkTypeAndCapabilityExemption();
        updateVopsConfig();
        updateUnmeteredNetworkTypes();
        updateBandwidths();
        updateTcpBuffers();
        updateHandoverRules();
        updateAutoDataSwitchConfig();

        log("Carrier config updated. Config is " + (isConfigCarrierSpecific() ? "" : "not ")
                + "carrier specific.");
    }

    /**
     * Update the network capability priority from carrier config.
     */
    private void updateNetworkCapabilityPriority() {
        synchronized (this) {
            mNetworkCapabilityPriorityMap.clear();
            String[] capabilityPriorityStrings = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_TELEPHONY_NETWORK_CAPABILITY_PRIORITIES_STRING_ARRAY);
            if (capabilityPriorityStrings != null) {
                for (String capabilityPriorityString : capabilityPriorityStrings) {
                    capabilityPriorityString =
                            capabilityPriorityString.trim().toUpperCase(Locale.ROOT);
                    String[] tokens = capabilityPriorityString.split(":");
                    if (tokens.length != 2) {
                        loge("Invalid config \"" + capabilityPriorityString + "\"");
                        continue;
                    }

                    int netCap = DataUtils.getNetworkCapabilityFromString(tokens[0]);
                    if (netCap < 0) {
                        loge("Invalid config \"" + capabilityPriorityString + "\"");
                        continue;
                    }

                    int priority = Integer.parseInt(tokens[1]);
                    mNetworkCapabilityPriorityMap.put(netCap, priority);
                }
            }
        }
    }

    /**
     * Get the priority of a network capability.
     *
     * @param capability The network capability
     * @return The priority range from 0 ~ 100. 100 is the highest priority.
     */
    public int getNetworkCapabilityPriority(@NetCapability int capability) {
        if (mNetworkCapabilityPriorityMap.containsKey(capability)) {
            return mNetworkCapabilityPriorityMap.get(capability);
        }
        return 0;
    }

    /**
     * Update the data retry rules from the carrier config.
     */
    private void updateDataRetryRules() {
        synchronized (this) {
            mDataSetupRetryRules.clear();
            String[] dataRetryRulesStrings = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_TELEPHONY_DATA_SETUP_RETRY_RULES_STRING_ARRAY);
            if (dataRetryRulesStrings != null) {
                for (String ruleString : dataRetryRulesStrings) {
                    try {
                        mDataSetupRetryRules.add(new DataSetupRetryRule(ruleString));
                    } catch (IllegalArgumentException e) {
                        loge("updateDataRetryRules: " + e.getMessage());
                    }
                }
            }

            mDataHandoverRetryRules.clear();
            dataRetryRulesStrings = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_TELEPHONY_DATA_HANDOVER_RETRY_RULES_STRING_ARRAY);
            if (dataRetryRulesStrings != null) {
                for (String ruleString : dataRetryRulesStrings) {
                    try {
                        mDataHandoverRetryRules.add(new DataHandoverRetryRule(ruleString));
                    } catch (IllegalArgumentException e) {
                        loge("updateDataRetryRules: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * @return The data setup retry rules from carrier config.
     */
    @NonNull
    public List<DataSetupRetryRule> getDataSetupRetryRules() {
        return Collections.unmodifiableList(mDataSetupRetryRules);
    }

    /**
     * @return The data handover retry rules from carrier config.
     */
    @NonNull
    public List<DataHandoverRetryRule> getDataHandoverRetryRules() {
        return Collections.unmodifiableList(mDataHandoverRetryRules);
    }

    /**
     * @return Whether data roaming is enabled by default in carrier config.
     */
    public boolean isDataRoamingEnabledByDefault() {
        return mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_CARRIER_DEFAULT_DATA_ROAMING_ENABLED_BOOL);
    }

    /**
     * Update the home and roaming metered APN types from the carrier config.
     */
    private void updateMeteredApnTypes() {
        synchronized (this) {
            mMeteredApnTypes.clear();
            String[] meteredApnTypes = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_CARRIER_METERED_APN_TYPES_STRINGS);
            if (meteredApnTypes != null) {
                Arrays.stream(meteredApnTypes)
                        .map(ApnSetting::getApnTypeInt)
                        .forEach(mMeteredApnTypes::add);
            }
            mRoamingMeteredApnTypes.clear();
            String[] roamingMeteredApns = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_CARRIER_METERED_ROAMING_APN_TYPES_STRINGS);
            if (roamingMeteredApns != null) {
                Arrays.stream(roamingMeteredApns)
                        .map(ApnSetting::getApnTypeInt)
                        .forEach(mRoamingMeteredApnTypes::add);
            }
        }
    }

    /**
     * Get the metered network capabilities.
     *
     * @param isRoaming {@code true} for roaming scenario.
     *
     * @return The metered network capabilities when connected to a home network.
     */
    @NonNull
    @NetCapability
    public Set<Integer> getMeteredNetworkCapabilities(boolean isRoaming) {
        Set<Integer> meteredApnTypes = isRoaming ? mRoamingMeteredApnTypes : mMeteredApnTypes;
        Set<Integer> meteredCapabilities = meteredApnTypes.stream()
                .map(DataUtils::apnTypeToNetworkCapability)
                .filter(cap -> cap >= 0)
                .collect(Collectors.toSet());

        // Consumer slices are the slices that are allowed to be accessed by regular application to
        // get better performance. They should be metered. This can be turned into configurations in
        // the future.
        meteredCapabilities.add(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH);
        meteredCapabilities.add(NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY);

        return Collections.unmodifiableSet(meteredCapabilities);
    }

    /**
     * @return {@code true} if tethering profile should not be used when the device is roaming.
     */
    public boolean isTetheringProfileDisabledForRoaming() {
        return mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_DISABLE_DUN_APN_WHILE_ROAMING_WITH_PRESET_APN_BOOL);
    }

    /**
     * Check if the network capability metered.
     *
     * @param networkCapability The network capability.
     * @param isRoaming {@code true} for roaming scenario.
     * @return {@code true} if the network capability is metered.
     */
    public boolean isMeteredCapability(@NetCapability int networkCapability, boolean isRoaming) {
        return getMeteredNetworkCapabilities(isRoaming).contains(networkCapability);
    }

    /**
     * Check if the network capabilities are metered. If one of the capabilities is metered, then
     * the capabilities are metered.
     *
     * @param networkCapabilities The network capabilities.
     * @param isRoaming {@code true} for roaming scenario.
     * @return {@code true} if the capabilities are metered.
     */
    public boolean isAnyMeteredCapability(@NonNull @NetCapability int[] networkCapabilities,
            boolean isRoaming) {
        return Arrays.stream(networkCapabilities).boxed()
                .anyMatch(cap -> isMeteredCapability(cap, isRoaming));
    }

    /**
     * @return Whether to use data activity for RRC detection
     */
    public boolean shouldUseDataActivityForRrcDetection() {
        return mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_LTE_ENDC_USING_USER_DATA_FOR_RRC_DETECTION_BOOL);
    }

    /**
     * Update the network types for only single data networks from the carrier config.
     */
    private void updateSingleDataNetworkTypeAndCapabilityExemption() {
        synchronized (this) {
            mSingleDataNetworkTypeList.clear();
            mCapabilitiesExemptFromSingleDataList.clear();
            int[] singleDataNetworkTypeList = mCarrierConfig.getIntArray(
                    CarrierConfigManager.KEY_ONLY_SINGLE_DC_ALLOWED_INT_ARRAY);
            if (singleDataNetworkTypeList != null) {
                Arrays.stream(singleDataNetworkTypeList).forEach(mSingleDataNetworkTypeList::add);
            }

            int[] singleDataCapabilitiesExemptList = mCarrierConfig.getIntArray(
                    CarrierConfigManager.KEY_CAPABILITIES_EXEMPT_FROM_SINGLE_DC_CHECK_INT_ARRAY);
            if (singleDataCapabilitiesExemptList != null) {
                Arrays.stream(singleDataCapabilitiesExemptList)
                        .forEach(mCapabilitiesExemptFromSingleDataList::add);
            }
        }
    }

    /**
     * Update the voice over PS related config from the carrier config.
     */
    private synchronized void updateVopsConfig() {
            mShouldKeepNetworkUpInNonVops = mCarrierConfig.getBoolean(CarrierConfigManager
                    .Ims.KEY_KEEP_PDN_UP_IN_NO_VOPS_BOOL);
            int[] allowedNetworkTypes = mCarrierConfig.getIntArray(
                    CarrierConfigManager.Ims.KEY_IMS_PDN_ENABLED_IN_NO_VOPS_SUPPORT_INT_ARRAY);
            if (allowedNetworkTypes != null) {
                Arrays.stream(allowedNetworkTypes).forEach(mEnabledVopsNetworkTypesInNonVops::add);
            }
    }

    /**
     * @return The list of {@link NetworkType} that only supports single data networks
     */
    @NonNull
    @NetworkType
    public List<Integer> getNetworkTypesOnlySupportSingleDataNetwork() {
        return Collections.unmodifiableList(mSingleDataNetworkTypeList);
    }

    /**
     * @return The list of {@link android.net.NetworkCapabilities.NetCapability} that every of which
     * is exempt from the single PDN check.
     */
    @NonNull
    @NetCapability
    public Set<Integer> getCapabilitiesExemptFromSingleDataNetwork() {
        return Collections.unmodifiableSet(mCapabilitiesExemptFromSingleDataList);
    }

    /**
     * @param regState The modem reported data registration state.
     * @return {@code true} if should keep IMS network in case of moving to non VOPS area.
     */
    public boolean shouldKeepNetworkUpInNonVops(@NetworkRegistrationInfo.RegistrationState
            int regState) {
        return mShouldKeepNetworkUpInNonVops || allowBringUpNetworkInNonVops(regState);
    }

    /**
     * @param regState The modem reported data registration state.
     * @return {@code true} if allow bring up IMS network in case of moving to non VOPS area.
     */
    public boolean allowBringUpNetworkInNonVops(@NetworkRegistrationInfo.RegistrationState
            int regState) {
        int networkType = -1;
        if (regState == NetworkRegistrationInfo.REGISTRATION_STATE_HOME) {
            networkType = CarrierConfigManager.Ims.NETWORK_TYPE_HOME;
        } else if (regState == NetworkRegistrationInfo.REGISTRATION_STATE_ROAMING) {
            networkType = CarrierConfigManager.Ims.NETWORK_TYPE_ROAMING;
        }
        return mEnabledVopsNetworkTypesInNonVops.contains(networkType);
    }

    /** {@code True} requires ping test to pass on the target slot before switching to it.*/
    public boolean isPingTestBeforeAutoDataSwitchRequired() {
        return mResources.getBoolean(com.android.internal.R.bool
                .auto_data_switch_ping_test_before_switch);
    }

    /**
     * @return Whether {@link NetworkCapabilities#NET_CAPABILITY_TEMPORARILY_NOT_METERED}
     * is supported by the carrier.
     */
    public boolean isTempNotMeteredSupportedByCarrier() {
        return mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_NETWORK_TEMP_NOT_METERED_SUPPORTED_BOOL);
    }

    /**
     * Update the network types that are temporarily not metered from the carrier config.
     */
    private void updateUnmeteredNetworkTypes() {
        synchronized (this) {
            mUnmeteredNetworkTypes.clear();
            String[] unmeteredNetworkTypes = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_UNMETERED_NETWORK_TYPES_STRING_ARRAY);
            if (unmeteredNetworkTypes != null) {
                mUnmeteredNetworkTypes.addAll(Arrays.asList(unmeteredNetworkTypes));
            }
            mRoamingUnmeteredNetworkTypes.clear();
            String[] roamingUnmeteredNetworkTypes = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_ROAMING_UNMETERED_NETWORK_TYPES_STRING_ARRAY);
            if (roamingUnmeteredNetworkTypes != null) {
                mRoamingUnmeteredNetworkTypes.addAll(Arrays.asList(roamingUnmeteredNetworkTypes));
            }
        }
    }

    /**
     * Get whether the network type is unmetered from the carrier configs.
     *
     * @param displayInfo The {@link TelephonyDisplayInfo} to check meteredness for.
     * @param serviceState The {@link ServiceState}, used to determine roaming state.
     * @return Whether the carrier considers the given display info unmetered.
     */
    public boolean isNetworkTypeUnmetered(@NonNull TelephonyDisplayInfo displayInfo,
            @NonNull ServiceState serviceState) {
        String dataConfigNetworkType = getDataConfigNetworkType(displayInfo);
        return serviceState.getDataRoaming()
                ? mRoamingUnmeteredNetworkTypes.contains(dataConfigNetworkType)
                : mUnmeteredNetworkTypes.contains(dataConfigNetworkType);
    }

    /**
     * Update the downlink and uplink bandwidth values from the carrier config.
     */
    private void updateBandwidths() {
        synchronized (this) {
            mBandwidthMap.clear();
            String[] bandwidths = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_BANDWIDTH_STRING_ARRAY);
            boolean useLte = mCarrierConfig.getBoolean(CarrierConfigManager
                    .KEY_BANDWIDTH_NR_NSA_USE_LTE_VALUE_FOR_UPLINK_BOOL);
            if (bandwidths != null) {
                for (String bandwidth : bandwidths) {
                    // split1[0] = network type as string
                    // split1[1] = downlink,uplink
                    String[] split1 = bandwidth.split(":");
                    if (split1.length != 2) {
                        loge("Invalid bandwidth: " + bandwidth);
                        continue;
                    }
                    // split2[0] = downlink bandwidth in kbps
                    // split2[1] = uplink bandwidth in kbps
                    String[] split2 = split1[1].split(",");
                    if (split2.length != 2) {
                        loge("Invalid bandwidth values: " + Arrays.toString(split2));
                        continue;
                    }
                    int downlink, uplink;
                    try {
                        downlink = Integer.parseInt(split2[0]);
                        uplink = Integer.parseInt(split2[1]);
                    } catch (NumberFormatException e) {
                        loge("Exception parsing bandwidth values for network type " + split1[0]
                                + ": " + e);
                        continue;
                    }
                    if (useLte && split1[0].startsWith("NR")) {
                        // We can get it directly from mBandwidthMap because LTE is defined before
                        // the NR values in CarrierConfigManager#KEY_BANDWIDTH_STRING_ARRAY.
                        uplink = mBandwidthMap.get(DATA_CONFIG_NETWORK_TYPE_LTE)
                                .uplinkBandwidthKbps;
                    }
                    mBandwidthMap.put(split1[0],
                            new DataNetwork.NetworkBandwidth(downlink, uplink));
                }
            }
        }
    }

    /**
     * Get the bandwidth estimate from the carrier config.
     *
     * @param displayInfo The {@link TelephonyDisplayInfo} to get the bandwidth for.
     * @return The pre-configured bandwidth estimate from carrier config.
     */
    @NonNull
    public DataNetwork.NetworkBandwidth getBandwidthForNetworkType(
            @NonNull TelephonyDisplayInfo displayInfo) {
        DataNetwork.NetworkBandwidth bandwidth = mBandwidthMap.get(
                getDataConfigNetworkType(displayInfo));
        if (bandwidth != null) {
            return bandwidth;
        }
        return new DataNetwork.NetworkBandwidth(DEFAULT_BANDWIDTH, DEFAULT_BANDWIDTH);
    }

    /**
     * @return What kind of traffic is supported on an unrestricted satellite network.
     */
    @CarrierConfigManager.SATELLITE_DATA_SUPPORT_MODE
    public int getSatelliteDataSupportMode() {
        return mCarrierConfig.getInt(CarrierConfigManager.KEY_SATELLITE_DATA_SUPPORT_MODE_INT);
    }

    /**
     * Returns whether the data roaming setting for satellite connection is ignored.
     *
     * @return {@code true} if data roaming setting for satellite connection is ignored,
     * {@code false} otherwise.
     */
    public boolean isIgnoringDataRoamingSettingForSatellite() {
        return mCarrierConfig.getBoolean(
            CarrierConfigManager.KEY_SATELLITE_IGNORE_DATA_ROAMING_SETTING_BOOL);
    }

    /**
     * @return Whether data throttling should be reset when the TAC changes from the carrier config.
     */
    public boolean shouldResetDataThrottlingWhenTacChanges() {
        return mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_UNTHROTTLE_DATA_RETRY_WHEN_TAC_CHANGES_BOOL);
    }

    /**
     * @return The data service package override string from the carrier config.
     */
    public String getDataServicePackageName() {
        return mCarrierConfig.getString(
                CarrierConfigManager.KEY_CARRIER_DATA_SERVICE_WWAN_PACKAGE_OVERRIDE_STRING);
    }

    /**
     * @return The default MTU value in bytes from the carrier config.
     */
    public int getDefaultMtu() {
        return mCarrierConfig.getInt(CarrierConfigManager.KEY_DEFAULT_MTU_INT);
    }

    /**
     * @return the data limit in bytes that can be used for esim bootstrap usage.
     */
    public long getEsimBootStrapMaxDataLimitBytes() {
        return mResources.getInteger(
                com.android.internal.R.integer.config_esim_bootstrap_data_limit_bytes);
    }

    /**
     * @return the interval in millisecond used to re-evaluate bootstrap sim data usage during esim
     * bootstrap activation
     */
    public long getReevaluateBootstrapSimDataUsageMillis() {
        long bootStrapSimDataUsageReevaluateInterval = mResources.getInteger(
                com.android.internal.R.integer.config_reevaluate_bootstrap_sim_data_usage_millis);

        if (bootStrapSimDataUsageReevaluateInterval <= 0) {
            bootStrapSimDataUsageReevaluateInterval = REEVALUATE_BOOTSTRAP_SIM_DATA_USAGE_MILLIS;
        }

        return bootStrapSimDataUsageReevaluateInterval;
    }

    /**
     * Update the TCP buffer sizes from the resource overlays.
     */
    private void updateTcpBuffers() {
        synchronized (this) {
            mTcpBufferSizeMap.clear();
            String[] configs = mResources.getStringArray(
                    com.android.internal.R.array.config_network_type_tcp_buffers);
            if (configs != null) {
                for (String config : configs) {
                    // split[0] = network type as string
                    // split[1] = rmem_min,rmem_def,rmem_max,wmem_min,wmem_def,wmem_max
                    String[] split = config.split(":");
                    if (split.length != 2) {
                        loge("Invalid TCP buffer sizes entry: " + config);
                        continue;
                    }
                    if (split[1].split(",").length != 6) {
                        loge("Invalid TCP buffer sizes for " + split[0] + ": " + split[1]);
                        continue;
                    }
                    mTcpBufferSizeMap.put(split[0], split[1]);
                }
            }
        }
    }

    /**
     * Anomaly report thresholds for frequent setup data call failure.
     * @return EventFrequency to trigger the anomaly report
     */
    @NonNull
    public EventFrequency getAnomalySetupDataCallThreshold() {
        return mSetupDataCallAnomalyReportThreshold;
    }

    /**
     * Anomaly report thresholds for frequent network unwanted call
     * at {@link TelephonyNetworkAgent#onNetworkUnwanted}
     * @return EventFrequency to trigger the anomaly report
     */
    @NonNull
    public EventFrequency getAnomalyNetworkUnwantedThreshold() {
        return mNetworkUnwantedAnomalyReportThreshold;
    }

    /**
     * Anomaly report thresholds for back to back release-request of IMS.
     * @return EventFrequency to trigger the anomaly report
     */
    @NonNull
    public EventFrequency getAnomalyImsReleaseRequestThreshold() {
        return mImsReleaseRequestAnomalyReportThreshold;
    }

    /**
     * @return {@code true} if enabled anomaly report for invalid param when QNS wants to change
     * preferred network at {@link AccessNetworksManager}.
     */
    public boolean isInvalidQnsParamAnomalyReportEnabled() {
        return mIsInvalidQnsParamAnomalyReportEnabled;
    }

    /**
     * @return Timeout in ms before creating an anomaly report for a DataNetwork stuck in
     * {@link DataNetwork.ConnectingState}.
     */
    public int getAnomalyNetworkConnectingTimeoutMs() {
        return mNetworkConnectingTimeout;
    }

    /**
     * @return Timeout in ms before creating an anomaly report for a DataNetwork stuck in
     * {@link DataNetwork.DisconnectingState}.
     */
    public int getAnomalyNetworkDisconnectingTimeoutMs() {
        return mNetworkDisconnectingTimeout;
    }

    /**
     * @return Timeout in ms before creating an anomaly report for a DataNetwork stuck in
     * {@link DataNetwork.HandoverState}.
     */
    public int getNetworkHandoverTimeoutMs() {
        return mNetworkHandoverTimeout;
    }

    /**
     * @return {@code true} if enabled anomaly report for invalid APN config
     * at {@link DataProfileManager}
     */
    public boolean isApnConfigAnomalyReportEnabled() {
        return mIsApnConfigAnomalyReportEnabled;
    }

    /**
     * Update the network type and signal strength score table for auto data switch decisions.
     */
    private void updateAutoDataSwitchConfig() {
        synchronized (this) {
            mAutoDataSwitchNetworkTypeSignalMap.clear();
            final PersistableBundle table = mCarrierConfig.getPersistableBundle(
                    CarrierConfigManager.KEY_AUTO_DATA_SWITCH_RAT_SIGNAL_SCORE_BUNDLE);
            String[] networkTypeKeys = {
                    DATA_CONFIG_NETWORK_TYPE_GPRS,
                    DATA_CONFIG_NETWORK_TYPE_EDGE,
                    DATA_CONFIG_NETWORK_TYPE_UMTS,
                    DATA_CONFIG_NETWORK_TYPE_CDMA,
                    DATA_CONFIG_NETWORK_TYPE_1xRTT,
                    DATA_CONFIG_NETWORK_TYPE_EVDO_0,
                    DATA_CONFIG_NETWORK_TYPE_EVDO_A,
                    DATA_CONFIG_NETWORK_TYPE_HSDPA,
                    DATA_CONFIG_NETWORK_TYPE_HSUPA,
                    DATA_CONFIG_NETWORK_TYPE_HSPA,
                    DATA_CONFIG_NETWORK_TYPE_EVDO_B,
                    DATA_CONFIG_NETWORK_TYPE_EHRPD,
                    DATA_CONFIG_NETWORK_TYPE_IDEN,
                    DATA_CONFIG_NETWORK_TYPE_LTE,
                    DATA_CONFIG_NETWORK_TYPE_LTE_CA,
                    DATA_CONFIG_NETWORK_TYPE_HSPAP,
                    DATA_CONFIG_NETWORK_TYPE_GSM,
                    DATA_CONFIG_NETWORK_TYPE_TD_SCDMA,
                    DATA_CONFIG_NETWORK_TYPE_NR_NSA,
                    DATA_CONFIG_NETWORK_TYPE_NR_NSA_MMWAVE,
                    DATA_CONFIG_NETWORK_TYPE_NR_SA,
                    DATA_CONFIG_NETWORK_TYPE_NR_SA_MMWAVE
            };
            if (table != null) {
                for (String networkType : networkTypeKeys) {
                    int[] scores = table.getIntArray(networkType);
                    if (scores != null
                            && scores.length == SignalStrength.NUM_SIGNAL_STRENGTH_BINS) {
                        for (int i = 0; i < scores.length; i++) {
                            if (scores[i] < 0) {
                                loge("Auto switch score must not < 0 for network type "
                                        + networkType);
                                break;
                            }
                            if (i == scores.length - 1) {
                                mAutoDataSwitchNetworkTypeSignalMap.put(networkType, scores);
                            }
                        }
                    } else {
                        loge("Auto switch score table should specify "
                                + SignalStrength.NUM_SIGNAL_STRENGTH_BINS
                                + " signal strength for network type " + networkType);
                    }
                }
            }
        }
    }

    /**
     * @param displayInfo The displayed network info.
     * @param signalStrength The signal strength.
     * @return Score base on network type and signal strength to inform auto data switch decision.
     * The min score is {@link #OUT_OF_SERVICE_AUTO_DATA_SWITCH_SCORE} indicating missing config.
     */
    public int getAutoDataSwitchScore(@NonNull TelephonyDisplayInfo displayInfo,
            @NonNull SignalStrength signalStrength) {
        int[] scores = mAutoDataSwitchNetworkTypeSignalMap.get(
                getDataConfigNetworkType(displayInfo));
        return scores != null ? scores[signalStrength.getLevel()]
                : OUT_OF_SERVICE_AUTO_DATA_SWITCH_SCORE;
    }

    /**
     * @return The tolerated gap of score for auto data switch decision, larger than which the
     * device will switch to the SIM with higher score. If 0, the device always switch to the higher
     * score SIM. If < 0, the network type and signal strength based auto switch is disabled.
     */
    public int getAutoDataSwitchScoreTolerance() {
        return mResources.getInteger(com.android.internal.R.integer
                .auto_data_switch_score_tolerance);
    }

    /**
     * TODO: remove after V.
     * @return To indicate whether allow using roaming nDDS if user enabled its roaming when the DDS
     * is not usable(OOS or disabled roaming)
     */
    public boolean doesAutoDataSwitchAllowRoaming() {
        return mResources.getBoolean(com.android.internal.R.bool.auto_data_switch_allow_roaming);
    }

    /**
     * @return The maximum number of retries when a validation for switching failed.
     */
    public int getAutoDataSwitchValidationMaxRetry() {
        return mResources.getInteger(com.android.internal.R.integer
                .auto_data_switch_validation_max_retry);
    }

    /**
     * @return Time threshold in ms to define a internet connection status to be stable
     * (e.g. out of service, in service, wifi is the default active network.etc), while -1 indicates
     * auto switch feature disabled.
     */
    public long getAutoDataSwitchAvailabilityStabilityTimeThreshold() {
        return mResources.getInteger(com.android.internal.R.integer
                .auto_data_switch_availability_stability_time_threshold_millis);
    }

    /**
     * @return Time threshold in ms to define a internet connection performance status to be stable
     * (e.g. LTE + 4 signal strength, UMTS + 2 signal strength), while -1 indicates
     * auto switch feature based on RAT/SS is disabled.
     */
    public long getAutoDataSwitchPerformanceStabilityTimeThreshold() {
        return mResources.getInteger(com.android.internal.R.integer
                .auto_data_switch_performance_stability_time_threshold_millis);
    }

    /**
     * Defines the threshold for switching data back to the default SIM when both SIMs are out of
     * service, in milliseconds.
     * If the current data is on the backup SIM and both SIMs remain out of service for this
     * duration, data will be switched back to the default SIM.
     * A value of 0 means an immediate switch. If the value is negative, the threshold defined by
     * {@link #getAutoDataSwitchAvailabilityStabilityTimeThreshold()} will be used instead.
     */
    public long getAutoDataSwitchAvailabilitySwitchbackStabilityTimeThreshold() {
        return mResources.getInteger(com.android.internal.R.integer
                .auto_data_switch_availability_switchback_stability_time_threshold_millis);
    }

    /**
     * Get the TCP config string, used by {@link LinkProperties#setTcpBufferSizes(String)}.
     * The config string will have the following form, with values in bytes:
     * "read_min,read_default,read_max,write_min,write_default,write_max"
     *
     * @param displayInfo The {@link TelephonyDisplayInfo} to get the TCP config string for.
     * @return The TCP configuration string for the given display info or the default value from
     *         {@code config_tcp_buffers} if unavailable.
     */
    @NonNull
    public String getTcpConfigString(@NonNull TelephonyDisplayInfo displayInfo) {
        String config = mTcpBufferSizeMap.get(getDataConfigNetworkType(displayInfo));
        if (TextUtils.isEmpty(config)) {
            config = getDefaultTcpConfigString();
        }
        return config;
    }

    /**
     * @return The fixed TCP buffer size configured based on the device's memory and performance.
     */
    @NonNull
    public String getDefaultTcpConfigString() {
        return mResources.getString(com.android.internal.R.string.config_tcp_buffers);
    }

    /**
     * @return The delay in millisecond for IMS graceful tear down. If IMS/RCS de-registration
     * does not complete within the window, the data network will be torn down after timeout.
     */
    public long getImsDeregistrationDelay() {
        return mResources.getInteger(
                com.android.internal.R.integer.config_delay_for_ims_dereg_millis);
    }

    /**
     * @return {@code true} if PDN should persist when IWLAN data service restarted/crashed.
     * {@code false} will cause all data networks on IWLAN torn down if IWLAN data service crashes.
     */
    public boolean shouldPersistIwlanDataNetworksWhenDataServiceRestarted() {
        return mResources.getBoolean(com.android.internal.R.bool
                .config_wlan_data_service_conn_persistence_on_restart);
    }

    /**
     * @return {@code true} if adopt predefined IWLAN handover policy. If {@code false}, handover is
     * allowed by default.
     */
    public boolean isIwlanHandoverPolicyEnabled() {
        return mResources.getBoolean(com.android.internal.R.bool
                .config_enable_iwlan_handover_policy);
    }

    /**
     * @return {@code true} if tearing down IMS data network should be delayed until the voice call
     * ends.
     */
    public boolean isImsDelayTearDownUntilVoiceCallEndEnabled() {
        return mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_DELAY_IMS_TEAR_DOWN_UNTIL_CALL_END_BOOL);
    }

    /**
     * @return The bandwidth estimation source.
     */
    @DataNetwork.BandwidthEstimationSource
    public int getBandwidthEstimateSource() {
        String source = mResources.getString(
                com.android.internal.R.string.config_bandwidthEstimateSource);
        return switch (source) {
            case BANDWIDTH_SOURCE_MODEM_STRING_VALUE -> DataNetwork.BANDWIDTH_SOURCE_MODEM;
            case BANDWIDTH_SOURCE_CARRIER_CONFIG_STRING_VALUE ->
                    DataNetwork.BANDWIDTH_SOURCE_CARRIER_CONFIG;
            case BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR_STRING_VALUE ->
                    DataNetwork.BANDWIDTH_SOURCE_BANDWIDTH_ESTIMATOR;
            default -> {
                loge("Invalid bandwidth estimation source config: " + source);
                yield DataNetwork.BANDWIDTH_SOURCE_UNKNOWN;
            }
        };
    }

    /**
     * Get the {@link DataConfigNetworkType} based on the given {@link TelephonyDisplayInfo}.
     *
     * @param displayInfo The {@link TelephonyDisplayInfo} used to determine the type.
     * @return The equivalent {@link DataConfigNetworkType}.
     */
    @NonNull
    @DataConfigNetworkType
    private static String getDataConfigNetworkType(@NonNull TelephonyDisplayInfo displayInfo) {
        int networkType = displayInfo.getNetworkType();
        switch (displayInfo.getOverrideNetworkType()) {
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED:
                if (networkType == TelephonyManager.NETWORK_TYPE_NR) {
                    return DATA_CONFIG_NETWORK_TYPE_NR_SA_MMWAVE;
                } else {
                    return DATA_CONFIG_NETWORK_TYPE_NR_NSA_MMWAVE;
                }
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA:
                return DATA_CONFIG_NETWORK_TYPE_NR_NSA;
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO:
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA:
                return DATA_CONFIG_NETWORK_TYPE_LTE_CA;
            case TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE:
            default:
                return networkTypeToDataConfigNetworkType(networkType);
        }
    }

    /**
     * @return The unsupported network capabilities. The unsupported capabilities will be removed
     * from the default network capabilities that {@link TelephonyNetworkProvider} use to inform
     * connectivity service what network capabilities are supported by telephony.
     */
    @NonNull
    @NetCapability
    public Set<Integer> getUnsupportedNetworkCapabilities() {
        return Arrays.stream(mResources.getStringArray(com.android.internal.R.array
                        .config_unsupported_network_capabilities))
                .map(DataUtils::getNetworkCapabilityFromString)
                .collect(Collectors.toSet());
    }

    /** Update handover rules from carrier config. */
    private void updateHandoverRules() {
        synchronized (this) {
            mHandoverRuleList.clear();
            String[] handoverRulesStrings = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_IWLAN_HANDOVER_POLICY_STRING_ARRAY);
            if (handoverRulesStrings != null) {
                for (String ruleString : handoverRulesStrings) {
                    try {
                        mHandoverRuleList.add(new HandoverRule(ruleString, mFeatureFlags));
                    } catch (IllegalArgumentException e) {
                        loge("updateHandoverRules: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Describe an event occurs eventNumOccurrence within a time span timeWindow
     */
    public static class EventFrequency {
        /** The time window in ms within which event occurs. */
        public final long timeWindow;

        /** The number of time the event occurs. */
        public final int eventNumOccurrence;

        /**
         * Constructor
         *
         * @param timeWindow The time window in ms within which event occurs.
         * @param eventNumOccurrence The number of time the event occurs.
         */
        public EventFrequency(long timeWindow, int eventNumOccurrence) {
            this.timeWindow = timeWindow;
            this.eventNumOccurrence = eventNumOccurrence;
        }

        @Override
        public String toString() {
            return String.format("EventFrequency=[timeWindow=%d, eventNumOccurrence=%d]",
                    timeWindow, eventNumOccurrence);
        }
    }

    /**
     * Parse a pair of event throttle thresholds of the form "time window in ms,occurrences"
     * into {@link EventFrequency}
     * @param s String to be parsed in the form of "time window in ms,occurrences"
     * @param defaultTimeWindow The time window to return if parsing failed.
     * @param defaultOccurrences The occurrence to return if parsing failed.
     * @return timeWindow and occurrence wrapped in EventFrequency
     */
    @VisibleForTesting
    public EventFrequency parseSlidingWindowCounterThreshold(String s,
            long defaultTimeWindow, int defaultOccurrences) {
        EventFrequency defaultValue = new EventFrequency(defaultTimeWindow, defaultOccurrences);
        if (TextUtils.isEmpty(s)) return defaultValue;

        final String[] pair = s.split(",");
        if (pair.length != 2) {
            loge("Invalid format: " + s
                    + "Format should be in \"time window in ms,occurrences\". "
                    + "Using default instead.");
            return defaultValue;
        }
        long windowSpan;
        int occurrence;
        try {
            windowSpan = Long.parseLong(pair[0].trim());
        } catch (NumberFormatException e) {
            loge("Exception parsing SlidingWindow window span " + pair[0] + ": " + e);
            return defaultValue;
        }
        try {
            occurrence = Integer.parseInt(pair[1].trim());
        } catch (NumberFormatException e) {
            loge("Exception parsing SlidingWindow occurrence as integer " + pair[1] + ": " + e);
            return defaultValue;
        }
        return new EventFrequency(windowSpan, occurrence);
    }

    /**
     * @return Get rules for handover between IWLAN and cellular networks.
     *
     * @see CarrierConfigManager#KEY_IWLAN_HANDOVER_POLICY_STRING_ARRAY
     */
    @NonNull
    public List<HandoverRule> getHandoverRules() {
        return Collections.unmodifiableList(mHandoverRuleList);
    }

    /**
     * @return Get the delay in milliseconds for re-evaluating unsatisfied network requests.
     */
    public long getRetrySetupAfterDisconnectMillis() {
        return mCarrierConfig.getLong(CarrierConfigManager
                .KEY_CARRIER_DATA_CALL_APN_RETRY_AFTER_DISCONNECT_LONG);
    }

    /**
     * Get the data config network type for the given network type
     *
     * @param networkType The network type
     * @return The equivalent data config network type
     */
    @NonNull
    @DataConfigNetworkType
    private static String networkTypeToDataConfigNetworkType(
            @NetworkType int networkType) {
        return switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS -> DATA_CONFIG_NETWORK_TYPE_GPRS;
            case TelephonyManager.NETWORK_TYPE_EDGE -> DATA_CONFIG_NETWORK_TYPE_EDGE;
            case TelephonyManager.NETWORK_TYPE_UMTS -> DATA_CONFIG_NETWORK_TYPE_UMTS;
            case TelephonyManager.NETWORK_TYPE_HSDPA -> DATA_CONFIG_NETWORK_TYPE_HSDPA;
            case TelephonyManager.NETWORK_TYPE_HSUPA -> DATA_CONFIG_NETWORK_TYPE_HSUPA;
            case TelephonyManager.NETWORK_TYPE_HSPA -> DATA_CONFIG_NETWORK_TYPE_HSPA;
            case TelephonyManager.NETWORK_TYPE_CDMA -> DATA_CONFIG_NETWORK_TYPE_CDMA;
            case TelephonyManager.NETWORK_TYPE_EVDO_0 -> DATA_CONFIG_NETWORK_TYPE_EVDO_0;
            case TelephonyManager.NETWORK_TYPE_EVDO_A -> DATA_CONFIG_NETWORK_TYPE_EVDO_A;
            case TelephonyManager.NETWORK_TYPE_EVDO_B -> DATA_CONFIG_NETWORK_TYPE_EVDO_B;
            case TelephonyManager.NETWORK_TYPE_1xRTT -> DATA_CONFIG_NETWORK_TYPE_1xRTT;
            case TelephonyManager.NETWORK_TYPE_LTE -> DATA_CONFIG_NETWORK_TYPE_LTE;
            case TelephonyManager.NETWORK_TYPE_EHRPD -> DATA_CONFIG_NETWORK_TYPE_EHRPD;
            case TelephonyManager.NETWORK_TYPE_IDEN -> DATA_CONFIG_NETWORK_TYPE_IDEN;
            case TelephonyManager.NETWORK_TYPE_HSPAP -> DATA_CONFIG_NETWORK_TYPE_HSPAP;
            case TelephonyManager.NETWORK_TYPE_GSM -> DATA_CONFIG_NETWORK_TYPE_GSM;
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA -> DATA_CONFIG_NETWORK_TYPE_TD_SCDMA;
            case TelephonyManager.NETWORK_TYPE_IWLAN -> DATA_CONFIG_NETWORK_TYPE_IWLAN;
            case TelephonyManager.NETWORK_TYPE_LTE_CA -> DATA_CONFIG_NETWORK_TYPE_LTE_CA;
            case TelephonyManager.NETWORK_TYPE_NR -> DATA_CONFIG_NETWORK_TYPE_NR_SA;
            default -> "";
        };
    }

    /**
     * @return Get recovery action delay in milliseconds between recovery actions.
     *
     * @see CarrierConfigManager#KEY_DATA_STALL_RECOVERY_TIMERS_LONG_ARRAY
     */
    @NonNull
    public long[] getDataStallRecoveryDelayMillis() {
        return mCarrierConfig.getLongArray(
            CarrierConfigManager.KEY_DATA_STALL_RECOVERY_TIMERS_LONG_ARRAY);
    }

    /**
     * @return Get the data stall recovery should skip boolean array.
     *
     * @see CarrierConfigManager#KEY_DATA_STALL_RECOVERY_SHOULD_SKIP_BOOL_ARRAY
     */
    @NonNull
    public boolean[] getDataStallRecoveryShouldSkipArray() {
        return mCarrierConfig.getBooleanArray(
            CarrierConfigManager.KEY_DATA_STALL_RECOVERY_SHOULD_SKIP_BOOL_ARRAY);
    }

    /**
     * @return The default preferred APN. An empty string if not configured. This is used for the
     * first time boot up where preferred APN is not set.
     */
    @NonNull
    public String getDefaultPreferredApn() {
        return TextUtils.emptyIfNull(mCarrierConfig.getString(
                CarrierConfigManager.KEY_DEFAULT_PREFERRED_APN_NAME_STRING));
    }

    /**
     * @return The PCO id used for determine if data networks are using NR advanced networks. 0
     * indicates this feature is disabled.
     */
    public int getNrAdvancedCapablePcoId() {
        return mCarrierConfig.getInt(CarrierConfigManager.KEY_NR_ADVANCED_CAPABLE_PCO_ID_INT);
    }

    /**
     * @return The allowed APN types for initial attach. The order in the list determines the
     * priority of it being considered as IA APN. Note this should be only used for some exception
     * cases that we need to use "user-added" APN for initial attach. The regular way to configure
     * IA APN is by adding "IA" type to the APN in APN config.
     */
    @NonNull
    @ApnType
    public List<Integer> getAllowedInitialAttachApnTypes() {
        String[] apnTypesArray = mCarrierConfig.getStringArray(
                CarrierConfigManager.KEY_ALLOWED_INITIAL_ATTACH_APN_TYPES_STRING_ARRAY);
        if (apnTypesArray != null) {
            return Arrays.stream(apnTypesArray)
                    .map(ApnSetting::getApnTypesBitmaskFromString)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * @return {@code true} if enhanced IWLAN handover check is enabled. If enabled, telephony
     * frameworks will not perform handover if the target transport is out of service, or VoPS not
     * supported. The network will be torn down on the source transport, and will be
     * re-established on the target transport when condition is allowed for bringing up a new
     * network.
     */
    public boolean isEnhancedIwlanHandoverCheckEnabled() {
        return mResources.getBoolean(
                com.android.internal.R.bool.config_enhanced_iwlan_handover_check);
    }

    /**
     * @return {@code true} if allow sending null data profile to ask modem to clear the initial
     * attach data profile.
     */
    public boolean allowClearInitialAttachDataProfile() {
        return mResources.getBoolean(
                com.android.internal.R.bool.allow_clear_initial_attach_data_profile);
    }

    /**
     * @return Indicating whether the retry timer from setup data call response for data throttling
     * should be honored for emergency network request. By default this is off, meaning emergency
     * network requests will ignore the previous retry timer passed in from setup data call
     * response.
     */
    public boolean shouldHonorRetryTimerForEmergencyNetworkRequest() {
        return mResources.getBoolean(
                com.android.internal.R.bool.config_honor_data_retry_timer_for_emergency_network);
    }

    /**
     * @return The capabilities that network will be forced to mark as cellular transport.
     */
    @NetCapability
    public Set<Integer> getForcedCellularTransportCapabilities() {
        String[] forcedCellularTransportCapabilities = mResources.getStringArray(
                com.android.internal.R.array.config_force_cellular_transport_capabilities);

        return Arrays.stream(forcedCellularTransportCapabilities)
                .map(DataUtils::getNetworkCapabilityFromString)
                .collect(Collectors.toSet());
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
     * Dump the state of DataConfigManager
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(DataConfigManager.class.getSimpleName() + "-" + mPhone.getPhoneId() + ":");
        pw.increaseIndent();
        pw.println("isConfigCarrierSpecific=" + isConfigCarrierSpecific());
        pw.println("Network capability priority:");
        pw.increaseIndent();
        mNetworkCapabilityPriorityMap.forEach((key, value) -> pw.print(
                DataUtils.networkCapabilityToString(key) + ":" + value + " "));
        pw.decreaseIndent();
        pw.println();
        pw.println("Data setup retry rules:");
        pw.increaseIndent();
        mDataSetupRetryRules.forEach(pw::println);
        pw.decreaseIndent();
        pw.println("isIwlanHandoverPolicyEnabled=" + isIwlanHandoverPolicyEnabled());
        pw.println("Data handover retry rules:");
        pw.increaseIndent();
        mDataHandoverRetryRules.forEach(pw::println);
        pw.decreaseIndent();
        pw.println("shouldHonorRetryTimerForEmergencyNetworkRequest="
                + shouldHonorRetryTimerForEmergencyNetworkRequest());
        pw.println("mSetupDataCallAnomalyReport=" + mSetupDataCallAnomalyReportThreshold);
        pw.println("mNetworkUnwantedAnomalyReport=" + mNetworkUnwantedAnomalyReportThreshold);
        pw.println("mImsReleaseRequestAnomalyReport=" + mImsReleaseRequestAnomalyReportThreshold);
        pw.println("mIsInvalidQnsParamAnomalyReportEnabled="
                + mIsInvalidQnsParamAnomalyReportEnabled);
        pw.println("mNetworkConnectingTimeout=" + mNetworkConnectingTimeout);
        pw.println("mNetworkDisconnectingTimeout=" + mNetworkDisconnectingTimeout);
        pw.println("mNetworkHandoverTimeout=" + mNetworkHandoverTimeout);
        pw.println("mIsApnConfigAnomalyReportEnabled=" + mIsApnConfigAnomalyReportEnabled);
        pw.println("Auto data switch:");
        pw.increaseIndent();
        pw.println("getAutoDataSwitchScoreTolerance=" + getAutoDataSwitchScoreTolerance());
        mAutoDataSwitchNetworkTypeSignalMap.forEach((key, value) -> pw.println(key + ":"
                + Arrays.toString(value)));
        pw.println("getAutoDataSwitchAvailabilityStabilityTimeThreshold="
                + getAutoDataSwitchAvailabilityStabilityTimeThreshold());
        pw.println("getAutoDataSwitchPerformanceStabilityTimeThreshold="
                + getAutoDataSwitchPerformanceStabilityTimeThreshold());
        pw.println("getAutoDataSwitchValidationMaxRetry=" + getAutoDataSwitchValidationMaxRetry());
        pw.decreaseIndent();
        pw.println("Metered APN types=" + mMeteredApnTypes.stream()
                .map(ApnSetting::getApnTypeString).collect(Collectors.joining(",")));
        pw.println("Roaming metered APN types=" + mRoamingMeteredApnTypes.stream()
                .map(ApnSetting::getApnTypeString).collect(Collectors.joining(",")));
        pw.println("Single data network types=" + mSingleDataNetworkTypeList.stream()
                .map(TelephonyManager::getNetworkTypeName).collect(Collectors.joining(",")));
        pw.println("Capabilities exempt from single PDN=" + mCapabilitiesExemptFromSingleDataList
                .stream().map(DataUtils::networkCapabilityToString)
                .collect(Collectors.joining(",")));
        pw.println("mShouldKeepNetworkUpInNonVops=" + mShouldKeepNetworkUpInNonVops);
        pw.println("mEnabledVopsNetworkTypesInNonVops=" + mEnabledVopsNetworkTypesInNonVops);
        pw.println("isPingTestBeforeAutoDataSwitchRequired="
                + isPingTestBeforeAutoDataSwitchRequired());
        pw.println("Unmetered network types=" + String.join(",", mUnmeteredNetworkTypes));
        pw.println("Roaming unmetered network types="
                + String.join(",", mRoamingUnmeteredNetworkTypes));
        pw.println("Bandwidths:");
        pw.increaseIndent();
        mBandwidthMap.forEach((key, value) -> pw.println(key + ":" + value));
        pw.decreaseIndent();
        pw.println("shouldUseDataActivityForRrcDetection="
                + shouldUseDataActivityForRrcDetection());
        pw.println("isTempNotMeteredSupportedByCarrier=" + isTempNotMeteredSupportedByCarrier());
        pw.println("shouldResetDataThrottlingWhenTacChanges="
                + shouldResetDataThrottlingWhenTacChanges());
        pw.println("Data service package name=" + getDataServicePackageName());
        pw.println("Default MTU=" + getDefaultMtu());
        pw.println("TCP buffer sizes by RAT:");
        pw.increaseIndent();
        mTcpBufferSizeMap.forEach((key, value) -> pw.println(key + ":" + value));
        pw.decreaseIndent();
        pw.println("Default TCP buffer sizes=" + getDefaultTcpConfigString());
        pw.println("getImsDeregistrationDelay=" + getImsDeregistrationDelay());
        pw.println("shouldPersistIwlanDataNetworksWhenDataServiceRestarted="
                + shouldPersistIwlanDataNetworksWhenDataServiceRestarted());
        pw.println("Bandwidth estimation source=" + mResources.getString(
                com.android.internal.R.string.config_bandwidthEstimateSource));
        pw.println("isImsDelayTearDownUntilVoiceCallEndEnabled="
                + isImsDelayTearDownUntilVoiceCallEndEnabled());
        pw.println("isEnhancedIwlanHandoverCheckEnabled=" + isEnhancedIwlanHandoverCheckEnabled());
        pw.println("isTetheringProfileDisabledForRoaming="
                + isTetheringProfileDisabledForRoaming());
        pw.println("allowClearInitialAttachDataProfile=" + allowClearInitialAttachDataProfile());
        pw.println("forcedCellularTransportCapabilities=" + getForcedCellularTransportCapabilities()
                .stream().map(DataUtils::networkCapabilityToString)
                .collect(Collectors.joining(",")));
        pw.decreaseIndent();
    }
}
