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

package com.android.internal.telephony.metrics;

import static com.android.internal.telephony.nano.TelephonyProto.EmergencyNumberInfo;
import static com.android.internal.telephony.nano.TelephonyProto.ImsCapabilities;
import static com.android.internal.telephony.nano.TelephonyProto.ImsConnectionState;
import static com.android.internal.telephony.nano.TelephonyProto.RilDataCall;
import static com.android.internal.telephony.nano.TelephonyProto.SimState;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.CarrierIdMatching;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.CarrierKeyChange;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.ModemRestart;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.NetworkCapabilitiesInfo;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RilDeactivateDataCall;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RilSetupDataCall;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.RilSetupDataCallResponse;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonyServiceState;
import static com.android.internal.telephony.nano.TelephonyProto.TelephonySettings;

import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.SparseArray;

import com.android.internal.telephony.nano.TelephonyProto.ActiveSubscriptionInfo;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.DataSwitch;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.OnDemandDataSwitch;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.Type;

import java.util.Arrays;

public class TelephonyEventBuilder {
    private final TelephonyEvent mEvent = new TelephonyEvent();

    public TelephonyEvent build() {
        return mEvent;
    }

    /** The event is not related to any phone id. */
    public TelephonyEventBuilder() {
        this(-1 /* phoneId */);
    }

    public TelephonyEventBuilder(int phoneId) {
        this(SystemClock.elapsedRealtime(), phoneId);
    }

    public TelephonyEventBuilder(long timestamp, int phoneId) {
        mEvent.timestampMillis = timestamp;
        mEvent.phoneId = phoneId;
    }

    public TelephonyEventBuilder setSettings(TelephonySettings settings) {
        mEvent.type = TelephonyEvent.Type.SETTINGS_CHANGED;
        mEvent.settings = settings;
        return this;
    }

    public TelephonyEventBuilder setServiceState(TelephonyServiceState state) {
        mEvent.type = TelephonyEvent.Type.RIL_SERVICE_STATE_CHANGED;
        mEvent.serviceState = state;
        return this;
    }

    public TelephonyEventBuilder setImsConnectionState(ImsConnectionState state) {
        mEvent.type = TelephonyEvent.Type.IMS_CONNECTION_STATE_CHANGED;
        mEvent.imsConnectionState = state;
        return this;
    }

    public TelephonyEventBuilder setImsCapabilities(ImsCapabilities capabilities) {
        mEvent.type = TelephonyEvent.Type.IMS_CAPABILITIES_CHANGED;
        mEvent.imsCapabilities = capabilities;
        return this;
    }

    public TelephonyEventBuilder setDataStallRecoveryAction(int action) {
        mEvent.type = TelephonyEvent.Type.DATA_STALL_ACTION;
        mEvent.dataStallAction = action;
        return this;
    }

    public TelephonyEventBuilder setSignalStrength(int signalstrength) {
        mEvent.type = TelephonyEvent.Type.SIGNAL_STRENGTH;
        mEvent.signalStrength = signalstrength;
        return this;
    }

    public TelephonyEventBuilder setSetupDataCall(RilSetupDataCall request) {
        mEvent.type = TelephonyEvent.Type.DATA_CALL_SETUP;
        mEvent.setupDataCall = request;
        return this;
    }

    public TelephonyEventBuilder setSetupDataCallResponse(RilSetupDataCallResponse rsp) {
        mEvent.type = TelephonyEvent.Type.DATA_CALL_SETUP_RESPONSE;
        mEvent.setupDataCallResponse = rsp;
        return this;
    }

    public TelephonyEventBuilder setDeactivateDataCall(RilDeactivateDataCall request) {
        mEvent.type = TelephonyEvent.Type.DATA_CALL_DEACTIVATE;
        mEvent.deactivateDataCall = request;
        return this;
    }

    public TelephonyEventBuilder setDeactivateDataCallResponse(int errno) {
        mEvent.type = TelephonyEvent.Type.DATA_CALL_DEACTIVATE_RESPONSE;
        mEvent.error = errno;
        return this;
    }

    public TelephonyEventBuilder setDataCalls(RilDataCall[] dataCalls) {
        mEvent.type = TelephonyEvent.Type.DATA_CALL_LIST_CHANGED;
        mEvent.dataCalls = dataCalls;
        return this;
    }

    public TelephonyEventBuilder setNITZ(long timestamp) {
        mEvent.type = TelephonyEvent.Type.NITZ_TIME;
        mEvent.nitzTimestampMillis = timestamp;
        return this;
    }

    public TelephonyEventBuilder setModemRestart(ModemRestart modemRestart) {
        mEvent.type = TelephonyEvent.Type.MODEM_RESTART;
        mEvent.modemRestart = modemRestart;
        return this;
    }

    /**
     * Set and build Carrier Id Matching event
     */
    public TelephonyEventBuilder setCarrierIdMatching(CarrierIdMatching carrierIdMatching) {
        mEvent.type = TelephonyEvent.Type.CARRIER_ID_MATCHING;
        mEvent.carrierIdMatching = carrierIdMatching;
        return this;
    }

    /**
     * Set and build EMERGENCY_NUMBER_REPORT event
     */
    public TelephonyEventBuilder setUpdatedEmergencyNumber(
            EmergencyNumberInfo emergencyNumberInfo,
            int emergencyNumberDbVersion) {
        mEvent.type = TelephonyEvent.Type.EMERGENCY_NUMBER_REPORT;
        mEvent.updatedEmergencyNumber = emergencyNumberInfo;
        mEvent.emergencyNumberDatabaseVersion = emergencyNumberDbVersion;
        return this;
    }

    public TelephonyEventBuilder setCarrierKeyChange(CarrierKeyChange carrierKeyChange) {
        mEvent.type = TelephonyEvent.Type.CARRIER_KEY_CHANGED;
        mEvent.carrierKeyChange = carrierKeyChange;
        return this;
    }

    /** Set and build SIM state change event. */
    public TelephonyEventBuilder setSimStateChange(SparseArray<Integer> simStates) {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        mEvent.simState = new int[phoneCount];
        Arrays.fill(mEvent.simState, SimState.SIM_STATE_UNKNOWN);
        mEvent.type = Type.SIM_STATE_CHANGED;
        for (int i = 0; i < simStates.size(); i++) {
            int key = simStates.keyAt(i);
            if (0 <= key && key < phoneCount) {
                mEvent.simState[key] = simStates.get(key);
            }
        }
        return this;
    }

    /** Set and build subscription info change event. */
    public TelephonyEventBuilder setActiveSubscriptionInfoChange(ActiveSubscriptionInfo info) {
        mEvent.type = Type.ACTIVE_SUBSCRIPTION_INFO_CHANGED;
        mEvent.activeSubscriptionInfo = info;
        return this;
    }

    /** Set and build enabled modem bitmap change event. */
    public TelephonyEventBuilder setEnabledModemBitmap(int enabledModemBitmap) {
        mEvent.type = Type.ENABLED_MODEM_CHANGED;
        mEvent.enabledModemBitmap = enabledModemBitmap;
        return this;
    }

    /** Set and build data switch event. */
    public TelephonyEventBuilder setDataSwitch(DataSwitch dataSwitch) {
        mEvent.type = TelephonyEvent.Type.DATA_SWITCH;
        mEvent.dataSwitch = dataSwitch;
        return this;
    }

    /** Set and build network validation event. */
    public TelephonyEventBuilder setNetworkValidate(int networkValidationState) {
        mEvent.type = TelephonyEvent.Type.NETWORK_VALIDATE;
        mEvent.networkValidationState = networkValidationState;
        return this;
    }

    /** Set and build on demand data switch event. */
    public TelephonyEventBuilder setOnDemandDataSwitch(OnDemandDataSwitch onDemandDataSwitch) {
        mEvent.type = TelephonyEvent.Type.ON_DEMAND_DATA_SWITCH;
        mEvent.onDemandDataSwitch = onDemandDataSwitch;
        return this;
    }

    /** Set and build network capabilities changed event. */
    public TelephonyEventBuilder setNetworkCapabilities(
            NetworkCapabilitiesInfo networkCapabilities) {
        mEvent.type = TelephonyEvent.Type.NETWORK_CAPABILITIES_CHANGED;
        mEvent.networkCapabilities = networkCapabilities;
        return this;
    }

    /** Set radio state. */
    public TelephonyEventBuilder setRadioState(int radioState) {
        mEvent.type = TelephonyEvent.Type.RADIO_STATE_CHANGED;
        mEvent.radioState = radioState;
        return this;
    }
}
