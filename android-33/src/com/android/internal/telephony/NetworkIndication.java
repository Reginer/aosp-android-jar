/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.telephony.TelephonyManager.UNKNOWN_CARRIER_ID;

import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CDMA_PRL_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_CELL_INFO_LIST;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_LCEDATA_RECV;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_NETWORK_SCAN_RESULT;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_NITZ_TIME_RECEIVED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_PHYSICAL_CHANNEL_CONFIG;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESPONSE_NETWORK_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_RESTRICTED_STATE_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SIGNAL_STRENGTH;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_SUPP_SVC_NOTIFICATION;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_VOICE_RADIO_TECH_CHANGED;

import android.annotation.ElapsedRealtimeLong;
import android.hardware.radio.network.IRadioNetworkIndication;
import android.os.AsyncResult;
import android.sysprop.TelephonyProperties;
import android.telephony.AnomalyReporter;
import android.telephony.BarringInfo;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.LinkCapacityEstimate;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhysicalChannelConfig;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.text.TextUtils;

import com.android.internal.telephony.gsm.SuppServiceNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Interface declaring unsolicited radio indications for network APIs.
 */
public class NetworkIndication extends IRadioNetworkIndication.Stub {
    private final RIL mRil;

    public NetworkIndication(RIL ril) {
        mRil = ril;
    }

    /**
     * Indicate that BarringInfo has changed for the current cell and user.
     * @param indicationType Type of radio indication
     * @param cellIdentity the CellIdentity of the Cell
     * @param barringInfos the updated barring information from the current cell, filtered for the
     *        current PLMN and access class / access category.
     */
    public void barringInfoChanged(int indicationType,
            android.hardware.radio.network.CellIdentity cellIdentity,
            android.hardware.radio.network.BarringInfo[] barringInfos) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        if (cellIdentity == null || barringInfos == null) {
            reportAnomaly(UUID.fromString("645b16bb-c930-4c1c-9c5d-568696542e05"),
                    "Invalid barringInfoChanged indication");
            mRil.riljLoge("Invalid barringInfoChanged indication");
            return;
        }

        BarringInfo cbi = new BarringInfo(RILUtils.convertHalCellIdentity(cellIdentity),
                RILUtils.convertHalBarringInfoList(barringInfos));

        mRil.mBarringInfoChangedRegistrants.notifyRegistrants(new AsyncResult(null, cbi, null));
    }

    /**
     * Indicates when PRL (preferred roaming list) changes.
     * @param indicationType Type of radio indication
     * @param version PRL version after PRL changes
     */
    public void cdmaPrlChanged(int indicationType, int version) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        int[] response = new int[]{version};

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CDMA_PRL_CHANGED, response);

        mRil.mCdmaPrlChangedRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
    }

    /**
     * Report all of the current cell information known to the radio.
     * @param indicationType Type of radio indication
     * @param records Current cell information
     */
    public void cellInfoList(int indicationType,
            android.hardware.radio.network.CellInfo[] records) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);
        ArrayList<CellInfo> response = RILUtils.convertHalCellInfoList(records);
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_CELL_INFO_LIST, response);
        mRil.mRilCellInfoListRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
    }

    /**
     * Indicates current link capacity estimate.
     * @param indicationType Type of radio indication
     * @param lce LinkCapacityEstimate
     */
    public void currentLinkCapacityEstimate(int indicationType,
            android.hardware.radio.network.LinkCapacityEstimate lce) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        List<LinkCapacityEstimate> response = RILUtils.convertHalLceData(lce);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_LCEDATA_RECV, response);

        if (mRil.mLceInfoRegistrants != null) {
            mRil.mLceInfoRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
        }
    }

    /**
     * Indicates current physical channel configuration.
     * @param indicationType Type of radio indication
     * @param configs Vector of PhysicalChannelConfigs
     */
    public void currentPhysicalChannelConfigs(int indicationType,
            android.hardware.radio.network.PhysicalChannelConfig[] configs) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);
        List<PhysicalChannelConfig> response = new ArrayList<>(configs.length);
        try {
            for (android.hardware.radio.network.PhysicalChannelConfig config : configs) {
                PhysicalChannelConfig.Builder builder = new PhysicalChannelConfig.Builder();
                switch (config.band.getTag()) {
                    case android.hardware.radio.network.PhysicalChannelConfigBand.geranBand:
                        builder.setBand(config.band.getGeranBand());
                        break;
                    case android.hardware.radio.network.PhysicalChannelConfigBand.utranBand:
                        builder.setBand(config.band.getUtranBand());
                        break;
                    case android.hardware.radio.network.PhysicalChannelConfigBand.eutranBand:
                        builder.setBand(config.band.getEutranBand());
                        break;
                    case android.hardware.radio.network.PhysicalChannelConfigBand.ngranBand:
                        builder.setBand(config.band.getNgranBand());
                        break;
                    default:
                        mRil.riljLoge("Unsupported band type " + config.band.getTag());
                }
                response.add(builder.setCellConnectionStatus(
                        RILUtils.convertHalCellConnectionStatus(config.status))
                        .setDownlinkChannelNumber(config.downlinkChannelNumber)
                        .setUplinkChannelNumber(config.uplinkChannelNumber)
                        .setCellBandwidthDownlinkKhz(config.cellBandwidthDownlinkKhz)
                        .setCellBandwidthUplinkKhz(config.cellBandwidthUplinkKhz)
                        .setNetworkType(ServiceState.rilRadioTechnologyToNetworkType(config.rat))
                        .setPhysicalCellId(config.physicalCellId)
                        .setContextIds(config.contextIds)
                        .build());
            }
        } catch (IllegalArgumentException iae) {
            reportAnomaly(UUID.fromString("918f0970-9aa9-4bcd-a28e-e49a83fe77d5"),
                    "RIL reported invalid PCC (AIDL)");
            mRil.riljLoge("Invalid PhysicalChannelConfig " + iae);
            return;
        }
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_PHYSICAL_CHANNEL_CONFIG, response);

        mRil.mPhysicalChannelConfigurationRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    /**
     * Indicates current signal strength of the radio.
     * @param indicationType Type of radio indication
     * @param signalStrength SignalStrength information
     */
    public void currentSignalStrength(int indicationType,
            android.hardware.radio.network.SignalStrength signalStrength) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        SignalStrength ssInitial = RILUtils.convertHalSignalStrength(signalStrength);

        SignalStrength ss = mRil.fixupSignalStrength10(ssInitial);
        // Note this is set to "verbose" because it happens frequently
        if (RIL.RILJ_LOGV) mRil.unsljLogvRet(RIL_UNSOL_SIGNAL_STRENGTH, ss);

        if (mRil.mSignalStrengthRegistrant != null) {
            mRil.mSignalStrengthRegistrant.notifyRegistrant(new AsyncResult(null, ss, null));
        }
    }

    /**
     * Indicates when IMS registration state has changed.
     * @param indicationType Type of radio indication
     */
    public void imsNetworkStateChanged(int indicationType) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED);

        mRil.mImsNetworkStateChangedRegistrants.notifyRegistrants();
    }

    /**
     * Incremental network scan results.
     * @param indicationType Type of radio indication
     * @param result the result of the network scan
     */
    public void networkScanResult(int indicationType,
            android.hardware.radio.network.NetworkScanResult result) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        ArrayList<CellInfo> cellInfos = RILUtils.convertHalCellInfoList(result.networkInfos);
        NetworkScanResult nsr = new NetworkScanResult(result.status, result.error, cellInfos);
        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_NETWORK_SCAN_RESULT, nsr);
        mRil.mRilNetworkScanResultRegistrants.notifyRegistrants(new AsyncResult(null, nsr, null));
    }

    /**
     * Indicates when either voice or data network state changed
     * @param indicationType Type of radio indication
     */
    public void networkStateChanged(int indicationType) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLog(RIL_UNSOL_RESPONSE_NETWORK_STATE_CHANGED);

        mRil.mNetworkStateRegistrants.notifyRegistrants();
    }

    /**
     * Indicates when radio has received a NITZ time message.
     * @param indicationType Type of radio indication
     * @param nitzTime NITZ time string in the form "yy/mm/dd,hh:mm:ss(+/-)tz,dt"
     * @param receivedTimeMs time according to {@link android.os.SystemClock#elapsedRealtime()} when
     *        the RIL sent the NITZ time to the framework
     * @param ageMs time in milliseconds indicating how long NITZ was cached in RIL and modem.
     *        This must track true age and therefore must be calculated using clocks that
     *        include the time spend in sleep / low power states. If it can not be guaranteed,
     *        there must not be any caching done at the modem and should fill in 0 for ageMs
     */
    public void nitzTimeReceived(int indicationType, String nitzTime,
        @ElapsedRealtimeLong long receivedTimeMs, long ageMs) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_NITZ_TIME_RECEIVED, nitzTime);

        // Ignore the NITZ if receivedTimeMs or ageMs is not a valid time.
        // e.g. receivedTimeMs is non-positive, ageMs is negative or greater than receivedTimeMs.
        if ((receivedTimeMs <= 0) || (ageMs < 0) || (ageMs >= receivedTimeMs)) {
            reportAnomaly(UUID.fromString("fc7c56d4-485d-475a-aaff-394203c6cdfc"),
                    "NITZ indication with invalid parameter");

            mRil.riljLoge("NITZ parameter is invalid, ignoring nitzTimeReceived indication. "
                + "receivedTimeMs = " + receivedTimeMs + ", ageMs = " + ageMs);
            return;
        }

        // TODO: Clean this up with a parcelable class for better self-documentation
        Object[] result = new Object[3];
        result[0] = nitzTime;
        result[1] = receivedTimeMs;
        result[2] = ageMs;

        boolean ignoreNitz = TelephonyProperties.ignore_nitz().orElse(false);

        if (ignoreNitz) {
            if (RIL.RILJ_LOGD) mRil.riljLog("ignoring UNSOL_NITZ_TIME_RECEIVED");
        } else {
            if (mRil.mNITZTimeRegistrant != null) {
                mRil.mNITZTimeRegistrant.notifyRegistrant(new AsyncResult(null, result, null));
            }
            // in case NITZ time registrant isn't registered yet, or a new registrant
            // registers later
            mRil.mLastNITZTimeInfo = result;
        }
    }

    /**
     * Indicate that a registration failure has occurred.
     * @param cellIdentity a CellIdentity the CellIdentity of the Cell
     * @param chosenPlmn a 5 or 6 digit alphanumeric string indicating the PLMN on which
     *        registration failed
     * @param domain the domain of the failed procedure: CS, PS, or both
     * @param causeCode the primary failure cause code of the procedure
     * @param additionalCauseCode an additional cause code if applicable
     */
    public void registrationFailed(int indicationType,
            android.hardware.radio.network.CellIdentity cellIdentity, String chosenPlmn,
            @NetworkRegistrationInfo.Domain int domain, int causeCode, int additionalCauseCode) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);
        CellIdentity ci = RILUtils.convertHalCellIdentity(cellIdentity);
        if (ci == null || TextUtils.isEmpty(chosenPlmn)
                || (domain & NetworkRegistrationInfo.DOMAIN_CS_PS) == 0
                || (domain & ~NetworkRegistrationInfo.DOMAIN_CS_PS) != 0
                || causeCode < 0 || additionalCauseCode < 0
                || (causeCode == Integer.MAX_VALUE && additionalCauseCode == Integer.MAX_VALUE)) {
            reportAnomaly(UUID.fromString("f16e5703-6105-4341-9eb3-e68189156eb4"),
                    "Invalid registrationFailed indication");

            mRil.riljLoge("Invalid registrationFailed indication");
            return;
        }

        mRil.mRegistrationFailedRegistrant.notifyRegistrant(
                new AsyncResult(null, new RegistrationFailedEvent(
                        ci, chosenPlmn, domain, causeCode, additionalCauseCode), null));
    }

    /**
     * Indicates a restricted state change (eg, for Domain Specific Access Control).
     * @param indicationType Type of radio indication
     * @param state Bitmask of restricted state as defined by PhoneRestrictedState
     */
    public void restrictedStateChanged(int indicationType, int state) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogvRet(RIL_UNSOL_RESTRICTED_STATE_CHANGED, state);

        if (mRil.mRestrictedStateRegistrant != null) {
            mRil.mRestrictedStateRegistrant.notifyRegistrant(new AsyncResult(null, state, null));
        }
    }

    /**
     * Reports supplementary service related notification from the network.
     * @param indicationType Type of radio indication
     * @param suppSvcNotification SuppSvcNotification
     */
    public void suppSvcNotify(int indicationType,
            android.hardware.radio.network.SuppSvcNotification suppSvcNotification) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        SuppServiceNotification notification = new SuppServiceNotification();
        notification.notificationType = suppSvcNotification.isMT ? 1 : 0;
        notification.code = suppSvcNotification.code;
        notification.index = suppSvcNotification.index;
        notification.type = suppSvcNotification.type;
        notification.number = suppSvcNotification.number;

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_SUPP_SVC_NOTIFICATION, notification);

        if (mRil.mSsnRegistrant != null) {
            mRil.mSsnRegistrant.notifyRegistrant(new AsyncResult(null, notification, null));
        }
    }

    /**
     * Indicates that voice technology has changed. Responds with new rat.
     * @param indicationType Type of radio indication
     * @param rat Current new voice rat
     */
    public void voiceRadioTechChanged(int indicationType, int rat) {
        mRil.processIndication(RIL.NETWORK_SERVICE, indicationType);

        int[] response = new int[] {rat};

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_VOICE_RADIO_TECH_CHANGED, response);

        mRil.mVoiceRadioTechChangedRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    @Override
    public String getInterfaceHash() {
        return IRadioNetworkIndication.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioNetworkIndication.VERSION;
    }

    private void reportAnomaly(UUID uuid, String msg) {
        Phone phone = mRil.mPhoneId == null ? null : PhoneFactory.getPhone(mRil.mPhoneId);
        int carrierId = phone == null ? UNKNOWN_CARRIER_ID : phone.getCarrierId();
        AnomalyReporter.reportAnomaly(uuid, msg, carrierId);
    }
}
