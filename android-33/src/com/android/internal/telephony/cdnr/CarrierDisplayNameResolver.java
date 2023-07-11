/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony.cdnr;

import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_CARRIER_API;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_CARRIER_CONFIG;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_CSIM;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_DATA_OPERATOR_SIGNALLING;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_ERI;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_MODEM_CONFIG;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_RUIM;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_SIM;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_USIM;
import static com.android.internal.telephony.cdnr.EfData.EF_SOURCE_VOICE_OPERATOR_SIGNALLING;

import android.annotation.NonNull;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.SparseArray;

import com.android.internal.R;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.cdnr.EfData.EFSource;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRecords.CarrierNameDisplayConditionBitmask;
import com.android.internal.telephony.uicc.IccRecords.OperatorPlmnInfo;
import com.android.internal.telephony.uicc.IccRecords.PlmnNetworkName;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Carrier display name resolver. */
public class CarrierDisplayNameResolver {
    private static final boolean DBG = true;
    private static final String TAG = "CDNR";

    /**
     * Only display SPN in home network, and PLMN network name in roaming network.
     */
    @CarrierNameDisplayConditionBitmask
    private static final int DEFAULT_CARRIER_NAME_DISPLAY_CONDITION_BITMASK = 0;

    private static final CarrierDisplayNameConditionRule DEFAULT_CARRIER_DISPLAY_NAME_RULE =
            new CarrierDisplayNameConditionRule(DEFAULT_CARRIER_NAME_DISPLAY_CONDITION_BITMASK);

    private final SparseArray<EfData> mEf = new SparseArray<>();

    private final LocalLog mLocalLog;
    private final Context mContext;
    private final GsmCdmaPhone mPhone;
    private final CarrierConfigManager mCCManager;

    private CarrierDisplayNameData mCarrierDisplayNameData;

    /**
     * The priority of ef source. Lower index means higher priority.
     */
    private static final List<Integer> EF_SOURCE_PRIORITY =
            Arrays.asList(
                    EF_SOURCE_CARRIER_API,
                    EF_SOURCE_CARRIER_CONFIG,
                    EF_SOURCE_ERI,
                    EF_SOURCE_USIM,
                    EF_SOURCE_SIM,
                    EF_SOURCE_CSIM,
                    EF_SOURCE_RUIM,
                    EF_SOURCE_VOICE_OPERATOR_SIGNALLING,
                    EF_SOURCE_DATA_OPERATOR_SIGNALLING,
                    EF_SOURCE_MODEM_CONFIG);

    public CarrierDisplayNameResolver(GsmCdmaPhone phone) {
        mLocalLog = new LocalLog(32);
        mContext = phone.getContext();
        mPhone = phone;
        mCCManager = (CarrierConfigManager) mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
    }

    /**
     * Update the ef from Ruim. If {@code ruim} is null, the ef records from this source will be
     * removed.
     *
     * @param ruim Ruim records.
     */
    public void updateEfFromRuim(RuimRecords ruim) {
        int key = getSourcePriority(EF_SOURCE_RUIM);
        if (ruim == null) {
            mEf.remove(key);
        } else {
            mEf.put(key, new RuimEfData(ruim));
        }
    }

    /**
     * Update the ef from Usim. If {@code usim} is null, the ef records from this source will be
     * removed.
     *
     * @param usim Usim records.
     */
    public void updateEfFromUsim(SIMRecords usim) {
        int key = getSourcePriority(EF_SOURCE_USIM);
        if (usim == null) {
            mEf.remove(key);
        } else {
            mEf.put(key, new UsimEfData(usim));
        }
    }

    /**
     * Update the ef from carrier config. If {@code config} is null, the ef records from this source
     * will be removed.
     *
     * @param config carrier config.
     */
    public void updateEfFromCarrierConfig(PersistableBundle config) {
        int key = getSourcePriority(EF_SOURCE_CARRIER_CONFIG);
        if (config == null) {
            mEf.remove(key);
        } else {
            mEf.put(key, new CarrierConfigEfData(config));
        }
    }

    /**
     * Update the ef for CDMA eri text. The ef records from this source will be set all of the
     * following situation are satisfied.
     *
     * 1. {@code eriText} is neither empty nor null.
     * 2. Current network is CDMA or CdmaLte
     * 3. ERI is allowed.
     *
     * @param eriText
     */
    public void updateEfForEri(String eriText) {
        PersistableBundle config = getCarrierConfig();
        int key = getSourcePriority(EF_SOURCE_ERI);
        if (!TextUtils.isEmpty(eriText) && (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte())
                && config.getBoolean(CarrierConfigManager.KEY_ALLOW_ERI_BOOL)) {
            mEf.put(key, new EriEfData(eriText));
        } else {
            mEf.remove(key);
        }
    }

    /**
     * Update the ef for brandOverride. If {@code operatorName} is empty or null, the ef records
     * from this source will be removed.
     *
     * @param operatorName operator name from brand override.
     */
    public void updateEfForBrandOverride(String operatorName) {
        int key = getSourcePriority(EF_SOURCE_CARRIER_API);
        if (TextUtils.isEmpty(operatorName)) {
            mEf.remove(key);
        } else {
            mEf.put(key,
                    new BrandOverrideEfData(operatorName, getServiceState().getOperatorNumeric()));
        }
    }

    /** Get the resolved carrier display name. */
    public CarrierDisplayNameData getCarrierDisplayNameData() {
        resolveCarrierDisplayName();
        return mCarrierDisplayNameData;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mEf.size(); i++) {
            EfData p = mEf.valueAt(i);
            sb.append("{spnDisplayCondition = "
                    + p.getServiceProviderNameDisplayCondition(isRoaming())
                    + ", spn = " + p.getServiceProviderName()
                    + ", spdiList = " + p.getServiceProviderDisplayInformation()
                    + ", pnnList = " + p.getPlmnNetworkNameList()
                    + ", oplList = " + p.getOperatorPlmnList()
                    + ", ehplmn = " + p.getEhplmnList()
                    + "}, ");
        }
        sb.append(", roamingFromSS = " + getServiceState().getRoaming());
        sb.append(", registeredPLMN = " + getServiceState().getOperatorNumeric());
        return sb.toString();
    }

    /**
     * Dumps information for carrier display name resolver.
     * @param pw information printer.
     */
    public void dump(IndentingPrintWriter pw) {
        pw.println("CDNR:");
        pw.increaseIndent();
        pw.println("fields = " + toString());
        pw.println("carrierDisplayNameData = " + mCarrierDisplayNameData);
        pw.decreaseIndent();

        pw.println("CDNR local log:");
        pw.increaseIndent();
        mLocalLog.dump(pw);
        pw.decreaseIndent();
    }

    @NonNull
    private PersistableBundle getCarrierConfig() {
        PersistableBundle config = mCCManager.getConfigForSubId(mPhone.getSubId());
        if (config == null) config = CarrierConfigManager.getDefaultConfig();
        return config;
    }

    @NonNull
    private CarrierDisplayNameConditionRule getDisplayRule() {
        boolean isRoaming = isRoaming();
        for (int i = 0; i < mEf.size(); i++) {
            if (mEf.valueAt(i).getServiceProviderNameDisplayCondition(isRoaming)
                    != IccRecords.INVALID_CARRIER_NAME_DISPLAY_CONDITION_BITMASK) {
                return new CarrierDisplayNameConditionRule(
                        mEf.valueAt(i).getServiceProviderNameDisplayCondition(isRoaming));
            }
        }
        return DEFAULT_CARRIER_DISPLAY_NAME_RULE;
    }

    @NonNull
    private List<String> getEfSpdi() {
        for (int i = 0; i < mEf.size(); i++) {
            if (mEf.valueAt(i).getServiceProviderDisplayInformation() != null) {
                return mEf.valueAt(i).getServiceProviderDisplayInformation();
            }
        }
        return Collections.EMPTY_LIST;
    }

    @NonNull
    private String getEfSpn() {
        for (int i = 0; i < mEf.size(); i++) {
            if (!TextUtils.isEmpty(mEf.valueAt(i).getServiceProviderName())) {
                return mEf.valueAt(i).getServiceProviderName();
            }
        }
        return "";
    }

    @NonNull
    private List<OperatorPlmnInfo> getEfOpl() {
        for (int i = 0; i < mEf.size(); i++) {
            if (mEf.valueAt(i).getOperatorPlmnList() != null) {
                return mEf.valueAt(i).getOperatorPlmnList();
            }
        }
        return Collections.EMPTY_LIST;
    }

    @NonNull
    private List<PlmnNetworkName> getEfPnn() {
        for (int i = 0; i < mEf.size(); i++) {
            if (mEf.valueAt(i).getPlmnNetworkNameList() != null) {
                return mEf.valueAt(i).getPlmnNetworkNameList();
            }
        }
        return Collections.EMPTY_LIST;
    }

    private boolean isRoaming() {
        // Currently use the roaming state from ServiceState.
        // EF_SPDI is only used when determine the service provider name and PLMN network name
        // display condition rule.
        // All the PLMNs will be considered HOME PLMNs if there is a brand override.
        return getServiceState().getRoaming()
                && !getEfSpdi().contains(getServiceState().getOperatorNumeric());
    }

    private CarrierDisplayNameData getCarrierDisplayNameFromEf() {
        CarrierDisplayNameConditionRule displayRule = getDisplayRule();

        String registeredPlmnName = getServiceState().getOperatorAlpha();
        String registeredPlmnNumeric = getServiceState().getOperatorNumeric();

        String spn = getEfSpn();

        // Resolve the PLMN network name
        List<OperatorPlmnInfo> efOpl = getEfOpl();
        List<PlmnNetworkName> efPnn = getEfPnn();

        String plmn = null;
        if (isRoaming()) {
            plmn = registeredPlmnName;
        } else {
            if (efOpl.isEmpty()) {
                // If the EF_OPL is not present, then the first record in EF_PNN is used for the
                // default network name when registered in the HPLMN or an EHPLMN(if the EHPLMN
                // list is present).
                plmn = efPnn.isEmpty() ? "" : getPlmnNetworkName(efPnn.get(0));
            } else {
                // TODO: Check the TAC/LAC & registered PLMN numeric in OPL list to determine which
                // PLMN name should be used to override the current one.
            }
        }

        // If no PLMN override is present, then the PLMN should be displayed:
        // - operator alpha if it's not empty.
        // - operator numeric.
        if (TextUtils.isEmpty(plmn)) {
            plmn = TextUtils.isEmpty(registeredPlmnName) ? registeredPlmnNumeric
                    : registeredPlmnName;
        }

        boolean showSpn = displayRule.shouldShowSpn(spn);
        boolean showPlmn = TextUtils.isEmpty(spn) || displayRule.shouldShowPlmn(plmn);

        return new CarrierDisplayNameData.Builder()
                .setSpn(spn)
                .setShowSpn(showSpn)
                .setPlmn(plmn)
                .setShowPlmn(showPlmn)
                .build();
    }

    private CarrierDisplayNameData getCarrierDisplayNameFromWifiCallingOverride(
            CarrierDisplayNameData rawCarrierDisplayNameData) {
        PersistableBundle config = getCarrierConfig();
        boolean useRootLocale = config.getBoolean(CarrierConfigManager.KEY_WFC_SPN_USE_ROOT_LOCALE);
        Context displayNameContext = mContext;
        if (useRootLocale) {
            Configuration displayNameConfig = mContext.getResources().getConfiguration();
            displayNameConfig.setLocale(Locale.ROOT);
            // Create a new Context for this temporary change
            displayNameContext = mContext.createConfigurationContext(displayNameConfig);
        }
        Resources r = displayNameContext.getResources();
        String[] wfcSpnFormats = r.getStringArray(com.android.internal.R.array.wfcSpnFormats);
        WfcCarrierNameFormatter wfcFormatter = new WfcCarrierNameFormatter(config, wfcSpnFormats,
                getServiceState().getState() == ServiceState.STATE_POWER_OFF);

        // Override the spn, data spn, plmn by wifi-calling
        String wfcSpn = wfcFormatter.formatVoiceName(rawCarrierDisplayNameData.getSpn());
        String wfcDataSpn = wfcFormatter.formatDataName(rawCarrierDisplayNameData.getSpn());
        List<PlmnNetworkName> efPnn = getEfPnn();
        String plmn = efPnn.isEmpty() ? "" : getPlmnNetworkName(efPnn.get(0));
        String wfcPlmn = wfcFormatter.formatVoiceName(
                TextUtils.isEmpty(plmn) ? rawCarrierDisplayNameData.getPlmn() : plmn);

        CarrierDisplayNameData result = rawCarrierDisplayNameData;
        if (!TextUtils.isEmpty(wfcSpn) && !TextUtils.isEmpty(wfcDataSpn)) {
            result = new CarrierDisplayNameData.Builder()
                    .setSpn(wfcSpn)
                    .setDataSpn(wfcDataSpn)
                    .setShowSpn(true)
                    .build();
        } else if (!TextUtils.isEmpty(wfcPlmn)) {
            result = new CarrierDisplayNameData.Builder()
                    .setPlmn(wfcPlmn)
                    .setShowPlmn(true)
                    .build();
        }
        return result;
    }

    private CarrierDisplayNameData getCarrierDisplayNameFromCrossSimCallingOverride(
            CarrierDisplayNameData rawCarrierDisplayNameData) {
        PersistableBundle config = getCarrierConfig();
        int crossSimSpnFormatIdx =
                config.getInt(CarrierConfigManager.KEY_CROSS_SIM_SPN_FORMAT_INT);
        boolean useRootLocale =
                config.getBoolean(CarrierConfigManager.KEY_WFC_SPN_USE_ROOT_LOCALE);

        String[] crossSimSpnFormats = SubscriptionManager.getResourcesForSubId(
                mPhone.getContext(),
                mPhone.getSubId(), useRootLocale)
                .getStringArray(R.array.crossSimSpnFormats);

        if (crossSimSpnFormatIdx < 0 || crossSimSpnFormatIdx >= crossSimSpnFormats.length) {
            Rlog.e(TAG, "updateSpnDisplay: KEY_CROSS_SIM_SPN_FORMAT_INT out of bounds: "
                    + crossSimSpnFormatIdx);
            crossSimSpnFormatIdx = 0;
        }
        String crossSimSpnFormat = crossSimSpnFormats[crossSimSpnFormatIdx];
        // Override the spn, data spn, plmn by Cross-SIM Calling
        List<PlmnNetworkName> efPnn = getEfPnn();
        String plmn = efPnn.isEmpty() ? "" : getPlmnNetworkName(efPnn.get(0));
        CarrierDisplayNameData result = rawCarrierDisplayNameData;
        String rawSpn = rawCarrierDisplayNameData.getSpn();
        String rawPlmn = TextUtils.isEmpty(plmn) ? rawCarrierDisplayNameData.getPlmn() : plmn;
        String crossSimSpn = String.format(crossSimSpnFormat, rawSpn);
        String crossSimPlmn = String.format(crossSimSpnFormat, plmn);
        if (!TextUtils.isEmpty(rawSpn) && !TextUtils.isEmpty(crossSimSpn)) {
            result = new CarrierDisplayNameData.Builder()
                    .setSpn(crossSimSpn)
                    .setDataSpn(crossSimSpn)
                    .setShowSpn(true)
                    .build();
        } else if (!TextUtils.isEmpty(rawPlmn) && !TextUtils.isEmpty(crossSimPlmn)) {
            result = new CarrierDisplayNameData.Builder()
                    .setPlmn(crossSimPlmn)
                    .setShowPlmn(true)
                    .build();
        }
        return result;
    }

    /**
     * Override the given carrier display name data {@code data} by out of service rule.
     * @param data the carrier display name data need to be overridden.
     * @return overridden carrier display name data.
     */
    private CarrierDisplayNameData getOutOfServiceDisplayName(CarrierDisplayNameData data) {
        // Out of service/Power off/Emergency Only override
        // 1) In flight mode (service state is ServiceState.STATE_POWER_OFF).
        //    showPlmn = true
        //    Only show null as PLMN
        //
        // 2) Service state is ServiceState.STATE_OUT_OF_SERVICE but emergency call is not allowed.
        //    showPlmn = true
        //    Only show "No Service" as PLMN
        //
        // 3) Out of service but emergency call is allowed.
        //    showPlmn = true
        //    Only show "Emergency call only" as PLMN
        String plmn = null;
        boolean isSimReady = mPhone.getUiccCardApplication() != null
                && mPhone.getUiccCardApplication().getState() == AppState.APPSTATE_READY;
        boolean forceDisplayNoService =
                mPhone.getServiceStateTracker().shouldForceDisplayNoService() && !isSimReady;
        ServiceState ss = getServiceState();
        if (ss.getState() == ServiceState.STATE_POWER_OFF && !forceDisplayNoService
                && !Phone.isEmergencyCallOnly()) {
            plmn = null;
        } else if (forceDisplayNoService || !Phone.isEmergencyCallOnly()) {
            plmn = mContext.getResources().getString(
                    com.android.internal.R.string.lockscreen_carrier_default);
        } else {
            plmn = mContext.getResources().getString(
                    com.android.internal.R.string.emergency_calls_only);
        }
        return new CarrierDisplayNameData.Builder()
                .setSpn(data.getSpn())
                .setDataSpn(data.getDataSpn())
                .setShowSpn(data.shouldShowSpn())
                .setPlmn(plmn)
                .setShowPlmn(true)
                .build();
    }

    private void resolveCarrierDisplayName() {
        CarrierDisplayNameData data = getCarrierDisplayNameFromEf();
        if (DBG) Rlog.d(TAG, "CarrierName from EF: " + data);
        if ((mPhone.getImsPhone() != null) && (mPhone.getImsPhone().getImsRegistrationTech()
                == ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM)) {
            data = getCarrierDisplayNameFromCrossSimCallingOverride(data);
            if (DBG) {
                Rlog.d(TAG, "CarrierName override by Cross-SIM Calling " + data);
            }
        } else if (mPhone.getServiceStateTracker().getCombinedRegState(getServiceState())
                == ServiceState.STATE_IN_SERVICE) {
            if (mPhone.isWifiCallingEnabled()) {
                data = getCarrierDisplayNameFromWifiCallingOverride(data);
                if (DBG) {
                    Rlog.d(TAG, "CarrierName override by wifi-calling " + data);
                }
            } else if (getServiceState().getState() == ServiceState.STATE_POWER_OFF) {
                // data in service due to IWLAN but APM on and WFC not available
                data = getOutOfServiceDisplayName(data);
                if (DBG) Rlog.d(TAG, "Out of service carrierName (APM) " + data);
            }
        } else {
            data = getOutOfServiceDisplayName(data);
            if (DBG) Rlog.d(TAG, "Out of service carrierName " + data);
        }

        if (!Objects.equals(mCarrierDisplayNameData, data)) {
            mLocalLog.log(String.format("ResolveCarrierDisplayName: %s", data.toString()));
        }

        mCarrierDisplayNameData = data;
    }

    /**
     * Get the PLMN network name from the {@link PlmnNetworkName} object.
     * @param name the {@link PlmnNetworkName} object may contain the full and short version of PLMN
     * network name.
     * @return full/short version PLMN network name if one of those is existed, otherwise return an
     * empty string.
     */
    private static String getPlmnNetworkName(PlmnNetworkName name) {
        if (name == null) return "";
        if (!TextUtils.isEmpty(name.fullName)) return name.fullName;
        if (!TextUtils.isEmpty(name.shortName)) return name.shortName;
        return "";
    }

    /**
     * Get the priority of the source of ef object. If {@code source} is not in the priority list,
     * return {@link Integer#MAX_VALUE}.
     * @param source source of ef object.
     * @return the priority of the source of ef object.
     */
    private static int getSourcePriority(@EFSource int source) {
        int priority = EF_SOURCE_PRIORITY.indexOf(source);
        if (priority == -1) priority = Integer.MAX_VALUE;
        return priority;
    }

    private static final class CarrierDisplayNameConditionRule {
        private int mDisplayConditionBitmask;

        CarrierDisplayNameConditionRule(int carrierDisplayConditionBitmask) {
            mDisplayConditionBitmask = carrierDisplayConditionBitmask;
        }

        boolean shouldShowSpn(String spn) {
            //Check if show SPN is required.
            Boolean showSpn = ((mDisplayConditionBitmask
                    & IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN)
                    == IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN);

            return !TextUtils.isEmpty(spn) && showSpn;
        }

        boolean shouldShowPlmn(String plmn) {
            // Check if show PLMN is required.
            Boolean showPlmn = ((mDisplayConditionBitmask
                    & IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN)
                    == IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN);

            return !TextUtils.isEmpty(plmn) && showPlmn;
        }

        @Override
        public String toString() {
            return String.format("{ SPN_bit = %d, PLMN_bit = %d }",
                    mDisplayConditionBitmask
                            & IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN,
                    mDisplayConditionBitmask
                            & IccRecords.CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN);
        }
    }

    private ServiceState getServiceState() {
        return mPhone.getServiceStateTracker().getServiceState();
    }

    /**
     * WiFi-Calling formatter for carrier name.
     */
    private static final class WfcCarrierNameFormatter {
        final String mVoiceFormat;
        final String mDataFormat;

        WfcCarrierNameFormatter(@NonNull PersistableBundle config,
                @NonNull String[] wfcFormats, boolean inFlightMode) {
            int voiceIdx = config.getInt(CarrierConfigManager.KEY_WFC_SPN_FORMAT_IDX_INT);
            int dataIdx = config.getInt(CarrierConfigManager.KEY_WFC_DATA_SPN_FORMAT_IDX_INT);
            int flightModeIdx = config.getInt(
                    CarrierConfigManager.KEY_WFC_FLIGHT_MODE_SPN_FORMAT_IDX_INT);

            if (voiceIdx < 0 || voiceIdx >= wfcFormats.length) {
                Rlog.e(TAG, "updateSpnDisplay: KEY_WFC_SPN_FORMAT_IDX_INT out of bounds: "
                        + voiceIdx);
                voiceIdx = 0;
            }

            if (dataIdx < 0 || dataIdx >= wfcFormats.length) {
                Rlog.e(TAG, "updateSpnDisplay: KEY_WFC_DATA_SPN_FORMAT_IDX_INT out of bounds: "
                        + dataIdx);
                dataIdx = 0;
            }

            if (flightModeIdx < 0 || flightModeIdx >= wfcFormats.length) {
                // KEY_WFC_FLIGHT_MODE_SPN_FORMAT_IDX_INT out of bounds. Use the value from
                // voiceIdx.
                flightModeIdx = voiceIdx;
            }

            // flight mode
            if (inFlightMode) {
                voiceIdx = flightModeIdx;
            }

            mVoiceFormat = voiceIdx != -1 ? wfcFormats[voiceIdx] : "";
            mDataFormat = dataIdx != -1 ? wfcFormats[dataIdx] : "";
        }

        /**
         * Format the given {@code name} using wifi-calling voice name formatter.
         * @param name the string need to be formatted.
         * @return formatted string if {@code name} is not empty, otherwise return {@code name}.
         */
        public String formatVoiceName(String name) {
            if (TextUtils.isEmpty(name)) return name;
            return String.format(mVoiceFormat, name.trim());
        }

        /**
         * Format the given {@code name} using wifi-calling data name formatter.
         * @param name the string need to be formatted.
         * @return formatted string if {@code name} is not empty, otherwise return {@code name}.
         */
        public String formatDataName(String name) {
            if (TextUtils.isEmpty(name)) return name;
            return String.format(mDataFormat, name.trim());
        }
    }
}
