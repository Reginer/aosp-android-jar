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

package com.android.internal.telephony.metrics;

import android.annotation.Nullable;
import android.os.SystemClock;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkUtils;
import android.telephony.Annotation.NetworkType;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularDataServiceSwitch;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularServiceState;
import com.android.telephony.Rlog;

import java.util.concurrent.atomic.AtomicReference;

/** Tracks service state duration and switch metrics for each phone. */
public class ServiceStateStats {
    private static final String TAG = ServiceStateStats.class.getSimpleName();

    private final AtomicReference<TimestampedServiceState> mLastState =
            new AtomicReference<>(new TimestampedServiceState(null, 0L));
    private final Phone mPhone;
    private final PersistAtomsStorage mStorage;

    public ServiceStateStats(Phone phone) {
        mPhone = phone;
        mStorage = PhoneFactory.getMetricsCollector().getAtomsStorage();
    }

    /** Finalizes the durations of the current service state segment. */
    public void conclude() {
        final long now = getTimeMillis();
        TimestampedServiceState lastState =
                mLastState.getAndUpdate(
                        state -> new TimestampedServiceState(state.mServiceState, now));
        addServiceState(lastState, now);
    }

    /** Updates service state when IMS voice registration changes. */
    public void onImsVoiceRegistrationChanged() {
        final long now = getTimeMillis();
        TimestampedServiceState lastState =
                mLastState.getAndUpdate(
                        state -> {
                            if (state.mServiceState == null) {
                                return new TimestampedServiceState(null, now);
                            }
                            CellularServiceState newServiceState = copyOf(state.mServiceState);
                            newServiceState.voiceRat =
                                    getVoiceRat(mPhone, getServiceStateForPhone(mPhone));
                            return new TimestampedServiceState(newServiceState, now);
                        });
        addServiceState(lastState, now);
    }

    /** Updates the current service state. */
    public void onServiceStateChanged(ServiceState serviceState) {
        final long now = getTimeMillis();
        if (isModemOff(serviceState)) {
            // Finish the duration of last service state and mark modem off
            addServiceState(mLastState.getAndSet(new TimestampedServiceState(null, now)), now);
        } else {
            CellularServiceState newState = new CellularServiceState();
            newState.voiceRat = getVoiceRat(mPhone, serviceState);
            newState.dataRat = getDataRat(serviceState);
            newState.voiceRoamingType = serviceState.getVoiceRoamingType();
            newState.dataRoamingType = serviceState.getDataRoamingType();
            newState.isEndc = isEndc(serviceState);
            newState.simSlotIndex = mPhone.getPhoneId();
            newState.isMultiSim = SimSlotState.isMultiSim();
            newState.carrierId = mPhone.getCarrierId();
            newState.isEmergencyOnly = isEmergencyOnly(serviceState);

            TimestampedServiceState prevState =
                    mLastState.getAndSet(new TimestampedServiceState(newState, now));
            addServiceStateAndSwitch(
                    prevState, now, getDataServiceSwitch(prevState.mServiceState, newState));
        }
    }

    private void addServiceState(TimestampedServiceState prevState, long now) {
        addServiceStateAndSwitch(prevState, now, null);
    }

    private void addServiceStateAndSwitch(
            TimestampedServiceState prevState,
            long now,
            @Nullable CellularDataServiceSwitch serviceSwitch) {
        if (prevState.mServiceState == null) {
            // Skip duration when modem is off
            return;
        }
        if (now >= prevState.mTimestamp) {
            CellularServiceState state = copyOf(prevState.mServiceState);
            state.totalTimeMillis = now - prevState.mTimestamp;
            mStorage.addCellularServiceStateAndCellularDataServiceSwitch(state, serviceSwitch);
        } else {
            Rlog.e(TAG, "addServiceState: durationMillis<0");
        }
    }

    @Nullable
    private CellularDataServiceSwitch getDataServiceSwitch(
            @Nullable CellularServiceState prevState, CellularServiceState nextState) {
        // Record switch only if multi-SIM state and carrier ID are the same and data RAT differs.
        if (prevState != null
                && prevState.isMultiSim == nextState.isMultiSim
                && prevState.carrierId == nextState.carrierId
                && prevState.dataRat != nextState.dataRat) {
            CellularDataServiceSwitch serviceSwitch = new CellularDataServiceSwitch();
            serviceSwitch.ratFrom = prevState.dataRat;
            serviceSwitch.ratTo = nextState.dataRat;
            serviceSwitch.isMultiSim = nextState.isMultiSim;
            serviceSwitch.simSlotIndex = nextState.simSlotIndex;
            serviceSwitch.carrierId = nextState.carrierId;
            serviceSwitch.switchCount = 1;
            return serviceSwitch;
        } else {
            return null;
        }
    }

    /** Returns the service state for the given phone, or {@code null} if it cannot be obtained. */
    @Nullable
    private static ServiceState getServiceStateForPhone(Phone phone) {
        ServiceStateTracker serviceStateTracker = phone.getServiceStateTracker();
        return serviceStateTracker != null ? serviceStateTracker.getServiceState() : null;
    }

    /**
     * Returns the band used from the given phone, or {@code 0} if it is invalid or cannot be
     * determined.
     */
    static int getBand(Phone phone) {
        ServiceState serviceState = getServiceStateForPhone(phone);
        return getBand(serviceState);
    }

    /**
     * Returns the band used from the given service state, or {@code 0} if it is invalid or cannot
     * be determined.
     */
    static int getBand(@Nullable ServiceState serviceState) {
        if (serviceState == null) {
            Rlog.w(TAG, "getBand: serviceState=null");
            return 0; // Band unknown
        }
        int chNumber = serviceState.getChannelNumber();
        int band;
        @NetworkType int rat = getRat(serviceState);
        switch (rat) {
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                band = AccessNetworkUtils.getOperatingBandForArfcn(chNumber);
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                band = AccessNetworkUtils.getOperatingBandForUarfcn(chNumber);
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_LTE_CA:
                band = AccessNetworkUtils.getOperatingBandForEarfcn(chNumber);
                break;
            default:
                Rlog.w(TAG, "getBand: unknown WWAN RAT " + rat);
                band = 0;
                break;
        }
        if (band == AccessNetworkUtils.INVALID_BAND) {
            Rlog.w(TAG, "getBand: band invalid for rat=" + rat + " ch=" + chNumber);
            return 0;
        } else {
            return band;
        }
    }

    private static CellularServiceState copyOf(CellularServiceState state) {
        // MessageNano does not support clone, have to copy manually
        CellularServiceState copy = new CellularServiceState();
        copy.voiceRat = state.voiceRat;
        copy.dataRat = state.dataRat;
        copy.voiceRoamingType = state.voiceRoamingType;
        copy.dataRoamingType = state.dataRoamingType;
        copy.isEndc = state.isEndc;
        copy.simSlotIndex = state.simSlotIndex;
        copy.isMultiSim = state.isMultiSim;
        copy.carrierId = state.carrierId;
        copy.totalTimeMillis = state.totalTimeMillis;
        copy.isEmergencyOnly = state.isEmergencyOnly;
        return copy;
    }

    /**
     * Returns {@code true} if modem radio is turned off (e.g. airplane mode).
     *
     * <p>Currently this is approximated by voice service state being {@code STATE_POWER_OFF}.
     */
    private static boolean isModemOff(ServiceState state) {
        // TODO(b/189335473): we should get this info from phone's radio power state, which is
        // updated separately
        return state.getVoiceRegState() == ServiceState.STATE_POWER_OFF;
    }

    /**
     * Returns the current voice RAT from IMS registration if present, otherwise from the service
     * state.
     */
    static @NetworkType int getVoiceRat(Phone phone, @Nullable ServiceState state) {
        if (state == null) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
        if (imsPhone != null) {
            @NetworkType int imsVoiceRat = imsPhone.getImsStats().getImsVoiceRadioTech();
            if (imsVoiceRat != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                // If IMS is over WWAN but WWAN PS is not in-service, then IMS RAT is invalid
                boolean isImsVoiceRatValid =
                        (imsVoiceRat == TelephonyManager.NETWORK_TYPE_IWLAN
                                || getDataRat(state) != TelephonyManager.NETWORK_TYPE_UNKNOWN);
                return isImsVoiceRatValid ? imsVoiceRat : TelephonyManager.NETWORK_TYPE_UNKNOWN;
            }
        }

        // If WWAN CS is not in-service, we should return NETWORK_TYPE_UNKNOWN
        final NetworkRegistrationInfo wwanRegInfo =
                state.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_CS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        return wwanRegInfo != null && wwanRegInfo.isInService()
                ? wwanRegInfo.getAccessNetworkTechnology()
                : TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    /**
     * Returns RAT used by WWAN.
     *
     * <p>Returns PS WWAN RAT, or CS WWAN RAT if PS WWAN RAT is unavailable.
     */
    private static @NetworkType int getRat(ServiceState state) {
        @NetworkType int rat = getDataRat(state);
        if (rat == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            rat = state.getVoiceNetworkType();
        }
        return rat;
    }

    /** Returns PS (data) RAT used by WWAN. */
    static @NetworkType int getDataRat(ServiceState state) {
        final NetworkRegistrationInfo wwanRegInfo =
                state.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        return wwanRegInfo != null && wwanRegInfo.isInService()
                ? wwanRegInfo.getAccessNetworkTechnology()
                : TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    private static boolean isEmergencyOnly(ServiceState state) {
        NetworkRegistrationInfo regInfo =
                state.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_CS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        return regInfo != null && !regInfo.isInService() && regInfo.isEmergencyEnabled();
    }

    private static boolean isEndc(ServiceState state) {
        if (getDataRat(state) != TelephonyManager.NETWORK_TYPE_LTE) {
            return false;
        }
        int nrState = state.getNrState();
        return nrState == NetworkRegistrationInfo.NR_STATE_CONNECTED
                || nrState == NetworkRegistrationInfo.NR_STATE_NOT_RESTRICTED;
    }

    @VisibleForTesting
    protected long getTimeMillis() {
        return SystemClock.elapsedRealtime();
    }

    private static final class TimestampedServiceState {
        private final CellularServiceState mServiceState;
        private final long mTimestamp; // Start time of the service state segment

        TimestampedServiceState(CellularServiceState serviceState, long timestamp) {
            mServiceState = serviceState;
            mTimestamp = timestamp;
        }
    }
}
