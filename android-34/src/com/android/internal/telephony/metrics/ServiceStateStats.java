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
import static android.telephony.TelephonyManager.DATA_CONNECTED;

import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_CS;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_IMS;
import static com.android.internal.telephony.TelephonyStatsLog.VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_UNKNOWN;

import android.annotation.NonNull;
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
import com.android.internal.telephony.data.DataNetwork;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.DataNetworkController.DataNetworkControllerCallback;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularDataServiceSwitch;
import com.android.internal.telephony.nano.PersistAtomsProto.CellularServiceState;
import com.android.telephony.Rlog;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Tracks service state duration and switch metrics for each phone. */
public class ServiceStateStats extends DataNetworkControllerCallback {
    private static final String TAG = ServiceStateStats.class.getSimpleName();

    private final AtomicReference<TimestampedServiceState> mLastState =
            new AtomicReference<>(new TimestampedServiceState(null, 0L));
    private final Phone mPhone;
    private final PersistAtomsStorage mStorage;
    private final DeviceStateHelper mDeviceStateHelper;

    public ServiceStateStats(Phone phone) {
        super(Runnable::run);
        mPhone = phone;
        mStorage = PhoneFactory.getMetricsCollector().getAtomsStorage();
        mDeviceStateHelper = PhoneFactory.getMetricsCollector().getDeviceStateHelper();
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

    /** Registers for internet pdn connected callback. */
    public void registerDataNetworkControllerCallback() {
        mPhone.getDataNetworkController().registerDataNetworkControllerCallback(this);
    }

    /** Updates service state when internet pdn gets connected. */
    public void onInternetDataNetworkConnected(@NonNull List<DataNetwork> internetNetworks) {
        onInternetDataNetworkChanged(true);
    }

    /** Updates service state when internet pdn gets disconnected. */
    public void onInternetDataNetworkDisconnected() {
        onInternetDataNetworkChanged(false);
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
            newState.dataRat = getRat(serviceState, NetworkRegistrationInfo.DOMAIN_PS);
            newState.voiceRoamingType = serviceState.getVoiceRoamingType();
            newState.dataRoamingType = serviceState.getDataRoamingType();
            newState.isEndc = isEndc(serviceState);
            newState.simSlotIndex = mPhone.getPhoneId();
            newState.isMultiSim = SimSlotState.isMultiSim();
            newState.carrierId = mPhone.getCarrierId();
            newState.isEmergencyOnly = isEmergencyOnly(serviceState);
            newState.isInternetPdnUp = isInternetPdnUp(mPhone);
            newState.foldState = mDeviceStateHelper.getFoldState();
            TimestampedServiceState prevState =
                    mLastState.getAndSet(new TimestampedServiceState(newState, now));
            addServiceStateAndSwitch(
                    prevState, now, getDataServiceSwitch(prevState.mServiceState, newState));
        }
    }

    /** Updates the fold state of the device for the current service state. */
    public void onFoldStateChanged(int foldState) {
        final long now = getTimeMillis();
        CellularServiceState lastServiceState = mLastState.get().mServiceState;
        if (lastServiceState == null || lastServiceState.foldState == foldState) {
            // Not need to update the fold state if modem is off or if is the
            // same fold state
            return;
        } else {
            TimestampedServiceState lastState =
                    mLastState.getAndUpdate(
                            state -> {
                                CellularServiceState newServiceState = copyOf(state.mServiceState);
                                newServiceState.foldState = foldState;
                                return new TimestampedServiceState(newServiceState, now);
                            });
            addServiceState(lastState, now);
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
        @NetworkType int rat = getRat(serviceState, NetworkRegistrationInfo.DOMAIN_PS);
        if (rat == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            rat = serviceState.getVoiceNetworkType();
        }
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
            case TelephonyManager.NETWORK_TYPE_NR:
                band = AccessNetworkUtils.getOperatingBandForNrarfcn(chNumber);
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
        copy.isInternetPdnUp = state.isInternetPdnUp;
        copy.foldState = state.foldState;
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
     *
     * <p>If the device is not in service, {@code TelephonyManager.NETWORK_TYPE_UNKNOWN} is returned
     * despite that the device may have emergency service over a certain RAT.
     */
    static @NetworkType int getVoiceRat(Phone phone, @Nullable ServiceState state) {
        return getVoiceRat(phone, state, VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_UNKNOWN);
    }

    /**
     * Returns the current voice RAT according to the bearer.
     *
     * <p>If the device is not in service, {@code TelephonyManager.NETWORK_TYPE_UNKNOWN} is returned
     * despite that the device may have emergency service over a certain RAT.
     */
    @VisibleForTesting public
    static @NetworkType int getVoiceRat(Phone phone, @Nullable ServiceState state, int bearer) {
        if (state == null) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
        if (bearer != VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_CS && imsPhone != null) {
            @NetworkType int imsVoiceRat = imsPhone.getImsStats().getImsVoiceRadioTech();
            if (imsVoiceRat != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                // If IMS is registered over WWAN but WWAN PS is not in service,
                // fallback to WWAN CS RAT
                boolean isImsVoiceRatValid =
                        (imsVoiceRat == TelephonyManager.NETWORK_TYPE_IWLAN
                                || getRat(state, NetworkRegistrationInfo.DOMAIN_PS)
                                        != TelephonyManager.NETWORK_TYPE_UNKNOWN);
                if (isImsVoiceRatValid) {
                    return imsVoiceRat;
                }
            }
        }
        if (bearer == VOICE_CALL_SESSION__BEARER_AT_END__CALL_BEARER_IMS) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        } else {
            return getRat(state, NetworkRegistrationInfo.DOMAIN_CS);
        }
    }

    /** Returns RAT used by WWAN if WWAN is in service. */
    public static @NetworkType int getRat(
            ServiceState state, @NetworkRegistrationInfo.Domain int domain) {
        final NetworkRegistrationInfo wwanRegInfo =
                state.getNetworkRegistrationInfo(
                        domain, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
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
        if (getRat(state, NetworkRegistrationInfo.DOMAIN_PS) != TelephonyManager.NETWORK_TYPE_LTE) {
            return false;
        }
        int nrState = state.getNrState();
        return nrState == NetworkRegistrationInfo.NR_STATE_CONNECTED
                || nrState == NetworkRegistrationInfo.NR_STATE_NOT_RESTRICTED;
    }

    private static boolean isInternetPdnUp(Phone phone) {
        DataNetworkController dataNetworkController = phone.getDataNetworkController();
        if (dataNetworkController != null) {
            return dataNetworkController.getInternetDataNetworkState() == DATA_CONNECTED;
        }
        return false;
    }

    private void onInternetDataNetworkChanged(boolean internetPdnUp) {
        final long now = getTimeMillis();
        TimestampedServiceState lastState =
                mLastState.getAndUpdate(
                        state -> {
                            if (state.mServiceState == null) {
                                return new TimestampedServiceState(null, now);
                            }
                            CellularServiceState newServiceState = copyOf(state.mServiceState);
                            newServiceState.isInternetPdnUp = internetPdnUp;
                            return new TimestampedServiceState(newServiceState, now);
                        });
        addServiceState(lastState, now);
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
