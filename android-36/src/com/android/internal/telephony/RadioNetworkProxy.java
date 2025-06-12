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

import static com.android.internal.telephony.RILConstants.REQUEST_NOT_SUPPORTED;

import android.annotation.NonNull;
import android.os.AsyncResult;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessSpecifier;
import android.telephony.Rlog;
import android.telephony.SignalThresholdInfo;

import com.android.internal.telephony.flags.Flags;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A holder for IRadioNetwork.
 * Use getAidl to get IRadioNetwork and call the AIDL implementations of the HAL APIs.
 */
public class RadioNetworkProxy extends RadioServiceProxy {
    private static final String TAG = "RadioNetworkProxy";
    private volatile android.hardware.radio.network.IRadioNetwork mNetworkProxy = null;

    private static final int INDICATION_FILTERS_ALL_V1_2 =
            android.hardware.radio.V1_5.IndicationFilter.SIGNAL_STRENGTH
                    | android.hardware.radio.V1_5.IndicationFilter.FULL_NETWORK_STATE
                    | android.hardware.radio.V1_5.IndicationFilter.DATA_CALL_DORMANCY_CHANGED
                    | android.hardware.radio.V1_5.IndicationFilter.LINK_CAPACITY_ESTIMATE
                    | android.hardware.radio.V1_5.IndicationFilter.PHYSICAL_CHANNEL_CONFIG;
    private static final int INDICATION_FILTERS_ALL_V1_5 =
            INDICATION_FILTERS_ALL_V1_2
                    | android.hardware.radio.V1_5.IndicationFilter.REGISTRATION_FAILURE
                    | android.hardware.radio.V1_5.IndicationFilter.BARRING_INFO;
    private static final int INDICATION_FILTERS_ALL_AIDL =
            android.hardware.radio.network.IndicationFilter.SIGNAL_STRENGTH
                    | android.hardware.radio.network.IndicationFilter.FULL_NETWORK_STATE
                    | android.hardware.radio.network.IndicationFilter.DATA_CALL_DORMANCY_CHANGED
                    | android.hardware.radio.network.IndicationFilter.LINK_CAPACITY_ESTIMATE
                    | android.hardware.radio.network.IndicationFilter.PHYSICAL_CHANNEL_CONFIG
                    | android.hardware.radio.network.IndicationFilter.REGISTRATION_FAILURE
                    | android.hardware.radio.network.IndicationFilter.BARRING_INFO;

    /**
     * Set IRadioNetwork as the AIDL implementation for RadioServiceProxy
     * @param halVersion Radio HAL version
     * @param network IRadioNetwork implementation
     *
     * @return updated HAL version
     */
    public HalVersion setAidl(HalVersion halVersion,
            android.hardware.radio.network.IRadioNetwork network) {
        HalVersion version = halVersion;
        try {
            version = RIL.getServiceHalVersion(network.getInterfaceVersion());
        } catch (RemoteException e) {
            Rlog.e(TAG, "setAidl: " + e);
        }
        mHalVersion = version;
        mNetworkProxy = network;
        mIsAidl = true;

        Rlog.d(TAG, "AIDL initialized mHalVersion=" + mHalVersion);
        return mHalVersion;
    }

    /**
     * Get the AIDL implementation of RadioNetworkProxy
     * @return IRadioNetwork implementation
     */
    public android.hardware.radio.network.IRadioNetwork getAidl() {
        return mNetworkProxy;
    }

    /**
     * Reset RadioNetworkProxy
     */
    @Override
    public void clear() {
        super.clear();
        mNetworkProxy = null;
    }

    /**
     * Check whether a RadioNetwork implementation exists
     * @return true if there is neither a HIDL nor AIDL implementation
     */
    @Override
    public boolean isEmpty() {
        return mRadioProxy == null && mNetworkProxy == null;
    }

    /**
     * Call IRadioNetwork#getAllowedNetworkTypesBitmap
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getAllowedNetworkTypesBitmap(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getAllowedNetworkTypesBitmap(serial);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getAllowedNetworkTypesBitmap(serial);
        } else {
            mRadioProxy.getPreferredNetworkTypeBitmap(serial);
        }
    }

    /**
     * Call IRadioNetwork#getAvailableBandModes
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getAvailableBandModes(int serial) throws RemoteException {
        if (Flags.cleanupCdma()) return;
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getAvailableBandModes(serial);
        } else {
            mRadioProxy.getAvailableBandModes(serial);
        }
    }

    /**
     * Call IRadioNetwork#getAvailableNetworks
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getAvailableNetworks(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getAvailableNetworks(serial);
        } else {
            mRadioProxy.getAvailableNetworks(serial);
        }
    }

    /**
     * Call IRadioNetwork#getBarringInfo
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getBarringInfo(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_5)) return;
        if (isAidl()) {
            mNetworkProxy.getBarringInfo(serial);
        } else {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).getBarringInfo(serial);
        }
    }

    /**
     * Call IRadioNetwork#getCdmaRoamingPreference
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getCdmaRoamingPreference(int serial) throws RemoteException {
        if (Flags.cleanupCdma()) return;
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getCdmaRoamingPreference(serial);
        } else {
            mRadioProxy.getCdmaRoamingPreference(serial);
        }
    }

    /**
     * Call IRadioNetwork#getCellInfoList
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getCellInfoList(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getCellInfoList(serial);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getCellInfoList_1_6(serial);
        } else {
            mRadioProxy.getCellInfoList(serial);
        }
    }

    /**
     * Call IRadioNetwork#getDataRegistrationState
     * @param serial Serial number of request
     * @param overrideHalVersion Radio HAL fallback compatibility override
     * @throws RemoteException
     */
    public void getDataRegistrationState(int serial, HalVersion overrideHalVersion)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getDataRegistrationState(serial);
        } else if ((overrideHalVersion == null
                || overrideHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6))
                && mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getDataRegistrationState_1_6(serial);
        } else if ((overrideHalVersion == null
                || overrideHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5))
                && mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).getDataRegistrationState_1_5(serial);
        } else {
            mRadioProxy.getDataRegistrationState(serial);
        }
    }

    /**
     * Call IRadioNetwork#getImsRegistrationState
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getImsRegistrationState(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getImsRegistrationState(serial);
        } else {
            mRadioProxy.getImsRegistrationState(serial);
        }
    }

    /**
     * Call IRadioNetwork#getNetworkSelectionMode
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getNetworkSelectionMode(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getNetworkSelectionMode(serial);
        } else {
            mRadioProxy.getNetworkSelectionMode(serial);
        }
    }

    /**
     * Call IRadioNetwork#getOperator
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getOperator(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getOperator(serial);
        } else {
            mRadioProxy.getOperator(serial);
        }
    }

    /**
     * Call IRadioNetwork#getSignalStrength
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getSignalStrength(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getSignalStrength(serial);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getSignalStrength_1_6(serial);
        } else {
            mRadioProxy.getSignalStrength_1_4(serial);
        }
    }

    /**
     * Call IRadioNetwork#getSystemSelectionChannels
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getSystemSelectionChannels(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mNetworkProxy.getSystemSelectionChannels(serial);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getSystemSelectionChannels(serial);
        }
    }

    /**
     * Call IRadioNetwork#getVoiceRadioTechnology
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getVoiceRadioTechnology(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getVoiceRadioTechnology(serial);
        } else {
            mRadioProxy.getVoiceRadioTechnology(serial);
        }
    }

    /**
     * Call IRadioNetwork#getVoiceRegistrationState
     * @param serial Serial number of request
     * @param overrideHalVersion Radio HAL fallback compatibility override
     * @throws RemoteException
     */
    public void getVoiceRegistrationState(int serial, HalVersion overrideHalVersion)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getVoiceRegistrationState(serial);
        } else if ((overrideHalVersion == null
                || overrideHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6))
                && mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy)
                    .getVoiceRegistrationState_1_6(serial);
        } else if ((overrideHalVersion == null
                || overrideHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5))
                && mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy)
                    .getVoiceRegistrationState_1_5(serial);
        } else {
            mRadioProxy.getVoiceRegistrationState(serial);
        }
    }

    /**
     * Call IRadioNetwork#isNrDualConnectivityEnabled
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void isNrDualConnectivityEnabled(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mNetworkProxy.isNrDualConnectivityEnabled(serial);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).isNrDualConnectivityEnabled(serial);
        }
    }

    /**
     * Call IRadioNetwork#responseAcknowledgement
     * @throws RemoteException
     */
    @Override
    public void responseAcknowledgement() throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.responseAcknowledgement();
        } else {
            mRadioProxy.responseAcknowledgement();
        }
    }

    /**
     * Call IRadioNetwork#setAllowedNetworkTypesBitmap
     * @param serial Serial number of request
     * @param networkTypeBitmask Network type bitmask to set
     * @throws RemoteException
     */
    public void setAllowedNetworkTypesBitmap(int serial, int networkTypeBitmask)
            throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mNetworkProxy.setAllowedNetworkTypesBitmap(serial,
                    RILUtils.convertToHalRadioAccessFamilyAidl(networkTypeBitmask));
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).setAllowedNetworkTypesBitmap(
                    serial, RILUtils.convertToHalRadioAccessFamily(networkTypeBitmask));
        }
    }

    /**
     * Call IRadioNetwork#setPreferredNetworkTypeBitmap
     * @param serial Serial number of request
     * @param networkTypesBitmask Preferred network types bitmask to set
     * @throws RemoteException
     */
    public void setPreferredNetworkTypeBitmap(int serial, int networkTypesBitmask)
            throws RemoteException {
        if (isEmpty() || mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) return;
        mRadioProxy.setPreferredNetworkTypeBitmap(serial,
                RILUtils.convertToHalRadioAccessFamily(networkTypesBitmask));
    }

    /**
     * Call IRadioNetwork#setBandMode
     * @param serial Serial number of request
     * @param bandMode One of BM_*_BAND
     * @throws RemoteException
     */
    public void setBandMode(int serial, int bandMode) throws RemoteException {
        if (Flags.cleanupCdma()) return;
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setBandMode(serial, bandMode);
        } else {
            mRadioProxy.setBandMode(serial, bandMode);
        }
    }

    /**
     * Call IRadioNetwork#setBarringPassword
     * @param serial Serial number of request
     * @param facility Facility string code
     * @param oldPassword Old password
     * @param newPassword New password
     * @throws RemoteException
     */
    public void setBarringPassword(int serial, String facility, String oldPassword,
            String newPassword) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setBarringPassword(serial, facility, oldPassword, newPassword);
        } else {
            mRadioProxy.setBarringPassword(serial, facility, oldPassword, newPassword);
        }
    }

    /**
     * Call IRadioNetwork#setCdmaRoamingPreference
     * @param serial Serial number of request
     * @param cdmaRoamingType One of CDMA_RM_*
     * @throws RemoteException
     */
    public void setCdmaRoamingPreference(int serial, int cdmaRoamingType) throws RemoteException {
        if (Flags.cleanupCdma()) return;
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setCdmaRoamingPreference(serial, cdmaRoamingType);
        } else {
            mRadioProxy.setCdmaRoamingPreference(serial, cdmaRoamingType);
        }
    }

    /**
     * Call IRadioNetwork#setCellInfoListRate
     * @param serial Serial number of request
     * @param rate Minimum time in milliseconds to indicate time between unsolicited cellInfoList()
     * @throws RemoteException
     */
    public void setCellInfoListRate(int serial, int rate) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setCellInfoListRate(serial, rate);
        } else {
            mRadioProxy.setCellInfoListRate(serial, rate);
        }
    }

    /**
     * Call IRadioNetwork#setIndicationFilter
     * @param serial Serial number of request
     * @param filter Unsolicited response filter
     * @throws RemoteException
     */
    public void setIndicationFilter(int serial, int filter) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setIndicationFilter(serial, filter & INDICATION_FILTERS_ALL_AIDL);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).setIndicationFilter_1_5(serial,
                    filter & INDICATION_FILTERS_ALL_V1_5);
        } else {
            mRadioProxy.setIndicationFilter_1_2(serial, filter & INDICATION_FILTERS_ALL_V1_2);
        }
    }

    /**
     * Call IRadioNetwork#setLinkCapacityReportingCriteria
     * @param serial Serial number of request
     * @param hysteresisMs A hysteresis time in milliseconds. A value of 0 disables hysteresis.
     * @param hysteresisDlKbps An interval in kbps defining the required magnitude change between DL
     *                         reports. A value of 0 disables hysteresis
     * @param hysteresisUlKbps An interval in kbps defining the required magnitude change between UL
     *                         reports. A value of 0 disables hysteresis
     * @param thresholdsDlKbps An array of trigger thresholds in kbps for DL reports. A size of 0
     *                         disables thresholds
     * @param thresholdsUlKbps An array of trigger thresholds in kbps for UL reports. A size of 0
     *                         disables thresholds
     * @param ran RadioAccessNetwork for which to apply criteria
     * @throws RemoteException
     */
    public void setLinkCapacityReportingCriteria(int serial, int hysteresisMs, int hysteresisDlKbps,
            int hysteresisUlKbps, int[] thresholdsDlKbps, int[] thresholdsUlKbps, int ran)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setLinkCapacityReportingCriteria(serial, hysteresisMs, hysteresisDlKbps,
                    hysteresisUlKbps, thresholdsDlKbps, thresholdsUlKbps,
                    RILUtils.convertToHalAccessNetworkAidl(ran));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).setLinkCapacityReportingCriteria_1_5(
                    serial, hysteresisMs, hysteresisDlKbps, hysteresisUlKbps,
                    RILUtils.primitiveArrayToArrayList(thresholdsDlKbps),
                    RILUtils.primitiveArrayToArrayList(thresholdsUlKbps),
                    RILUtils.convertToHalAccessNetwork(ran));
        } else {
            if (ran == AccessNetworkConstants.AccessNetworkType.NGRAN) {
                throw new RuntimeException("NGRAN unsupported on IRadio version: " + mHalVersion);
            }
            mRadioProxy.setLinkCapacityReportingCriteria(
                    serial, hysteresisMs, hysteresisDlKbps, hysteresisUlKbps,
                    RILUtils.primitiveArrayToArrayList(thresholdsDlKbps),
                    RILUtils.primitiveArrayToArrayList(thresholdsUlKbps),
                    RILUtils.convertToHalAccessNetwork(ran));
        }
    }

    /**
     * Call IRadioNetwork#setLocationUpdates
     * @param serial Serial number of request
     * @param enable Whether to enable or disable network state change notifications when location
     *               information (lac and/or cid) has changed
     * @throws RemoteException
     */
    public void setLocationUpdates(int serial, boolean enable) throws RemoteException {
        if (Flags.cleanupCdma()) return;
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setLocationUpdates(serial, enable);
        } else {
            mRadioProxy.setLocationUpdates(serial, enable);
        }
    }

    /**
     * Call IRadioNetwork#setNetworkSelectionModeAutomatic
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void setNetworkSelectionModeAutomatic(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setNetworkSelectionModeAutomatic(serial);
        } else {
            mRadioProxy.setNetworkSelectionModeAutomatic(serial);
        }
    }

    /**
     * Call IRadioNetwork#setNetworkSelectionModeManual
     * @param serial Serial number of request
     * @param operatorNumeric PLMN ID of the network to select
     * @param ran Radio access network type
     * @throws RemoteException
     */
    public void setNetworkSelectionModeManual(int serial, String operatorNumeric, int ran)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setNetworkSelectionModeManual(serial, operatorNumeric,
                    RILUtils.convertToHalAccessNetworkAidl(ran));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).setNetworkSelectionModeManual_1_5(
                    serial, operatorNumeric, RILUtils.convertToHalRadioAccessNetworks(ran));
        } else {
            mRadioProxy.setNetworkSelectionModeManual(serial, operatorNumeric);
        }
    }

    /**
     * Call IRadioNetwork#setNrDualConnectivityState
     * @param serial Serial number of request
     * @param nrDualConnectivityState Expected NR dual connectivity state
     * @throws RemoteException
     */
    public void setNrDualConnectivityState(int serial, byte nrDualConnectivityState)
            throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mNetworkProxy.setNrDualConnectivityState(serial, nrDualConnectivityState);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).setNrDualConnectivityState(
                    serial, nrDualConnectivityState);
        }
    }

    /**
     * Call IRadioNetwork#setSignalStrengthReportingCriteria
     * @param serial Serial number of request
     * @param signalThresholdInfos a list of {@link SignalThresholdInfo} to set with.
     * @throws RemoteException
     */
    public void setSignalStrengthReportingCriteria(int serial,
            @NonNull List<SignalThresholdInfo> signalThresholdInfos) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.network.SignalThresholdInfo[] halSignalThresholdsInfos =
            new android.hardware.radio.network.SignalThresholdInfo[signalThresholdInfos.size()];
            for (int i = 0; i < signalThresholdInfos.size(); i++) {
                halSignalThresholdsInfos[i] = RILUtils.convertToHalSignalThresholdInfoAidl(
                        signalThresholdInfos.get(i));
            }
            mNetworkProxy.setSignalStrengthReportingCriteria(serial, halSignalThresholdsInfos);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            for (SignalThresholdInfo signalThresholdInfo : signalThresholdInfos) {
                ((android.hardware.radio.V1_5.IRadio) mRadioProxy)
                        .setSignalStrengthReportingCriteria_1_5(serial,
                                RILUtils.convertToHalSignalThresholdInfo(signalThresholdInfo),
                                RILUtils.convertToHalAccessNetwork(
                                        signalThresholdInfo.getRadioAccessNetworkType()));
            }
        } else {
            for (SignalThresholdInfo signalThresholdInfo : signalThresholdInfos) {
                mRadioProxy.setSignalStrengthReportingCriteria(serial,
                        signalThresholdInfo.getHysteresisMs(),
                        signalThresholdInfo.getHysteresisDb(),
                        RILUtils.primitiveArrayToArrayList(signalThresholdInfo.getThresholds()),
                        RILUtils.convertToHalAccessNetwork(
                                signalThresholdInfo.getRadioAccessNetworkType()));
            }
        }
    }

    /**
     * Call IRadioNetwork#setSuppServiceNotifications
     * @param serial Serial number of request
     * @param enable True to enable notifications, false to disable
     * @throws RemoteException
     */
    public void setSuppServiceNotifications(int serial, boolean enable) throws RemoteException {
        if (Flags.cleanupCdma()) return;
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setSuppServiceNotifications(serial, enable);
        } else {
            mRadioProxy.setSuppServiceNotifications(serial, enable);
        }
    }

    /**
     * Call IRadioNetwork#setSystemSelectionChannels
     * @param serial Serial number of request
     * @param specifiers Which bands to scan
     * @throws RemoteException
     */
    public void setSystemSelectionChannels(int serial, List<RadioAccessSpecifier> specifiers)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setSystemSelectionChannels(serial, !specifiers.isEmpty(),
                    specifiers.stream().map(RILUtils::convertToHalRadioAccessSpecifierAidl)
                            .toArray(android.hardware.radio.network.RadioAccessSpecifier[]::new));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).setSystemSelectionChannels_1_5(
                    serial, !specifiers.isEmpty(), specifiers.stream()
                            .map(RILUtils::convertToHalRadioAccessSpecifier15)
                            .collect(Collectors.toCollection(ArrayList::new)));
        } else {
            mRadioProxy.setSystemSelectionChannels(serial, !specifiers.isEmpty(),
                    specifiers.stream()
                            .map(RILUtils::convertToHalRadioAccessSpecifier11)
                            .collect(Collectors.toCollection(ArrayList::new)));
        }
    }

    /**
     * Call IRadioNetwork#startNetworkScan
     * @param serial Serial number of request
     * @param request Defines the radio networks/bands/channels which need to be scanned
     * @param overrideHalVersion Radio HAL fallback compatibility override
     * @throws RemoteException
     */
    public void startNetworkScan(int serial, NetworkScanRequest request,
            HalVersion overrideHalVersion, Message result) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.network.NetworkScanRequest halRequest =
                    new android.hardware.radio.network.NetworkScanRequest();
            halRequest.type = request.getScanType();
            halRequest.interval = request.getSearchPeriodicity();
            halRequest.maxSearchTime = request.getMaxSearchTime();
            halRequest.incrementalResultsPeriodicity = request.getIncrementalResultsPeriodicity();
            halRequest.incrementalResults = request.getIncrementalResults();
            halRequest.mccMncs = request.getPlmns().stream().toArray(String[]::new);
            ArrayList<android.hardware.radio.network.RadioAccessSpecifier> specifiers =
                    new ArrayList<>();
            for (RadioAccessSpecifier ras : request.getSpecifiers()) {
                android.hardware.radio.network.RadioAccessSpecifier rasInHalFormat =
                        RILUtils.convertToHalRadioAccessSpecifierAidl(ras);
                if (rasInHalFormat == null) {
                    AsyncResult.forMessage(result, null,
                            CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                    return;
                }
                specifiers.add(rasInHalFormat);
            }
            halRequest.specifiers = specifiers.stream().toArray(
                    android.hardware.radio.network.RadioAccessSpecifier[]::new);
            mNetworkProxy.startNetworkScan(serial, halRequest);
        } else if ((overrideHalVersion == null
                || overrideHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5))
                && mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            android.hardware.radio.V1_5.NetworkScanRequest halRequest =
                    new android.hardware.radio.V1_5.NetworkScanRequest();
            halRequest.type = request.getScanType();
            halRequest.interval = request.getSearchPeriodicity();
            halRequest.maxSearchTime = request.getMaxSearchTime();
            halRequest.incrementalResultsPeriodicity = request.getIncrementalResultsPeriodicity();
            halRequest.incrementalResults = request.getIncrementalResults();
            halRequest.mccMncs.addAll(request.getPlmns());
            for (RadioAccessSpecifier ras : request.getSpecifiers()) {
                android.hardware.radio.V1_5.RadioAccessSpecifier rasInHalFormat =
                        RILUtils.convertToHalRadioAccessSpecifier15(ras);
                if (rasInHalFormat == null) {
                    AsyncResult.forMessage(result, null,
                            CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                    return;
                }
                halRequest.specifiers.add(rasInHalFormat);
            }
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).startNetworkScan_1_5(
                    serial, halRequest);
        } else {
            android.hardware.radio.V1_2.NetworkScanRequest halRequest =
                    new android.hardware.radio.V1_2.NetworkScanRequest();
            halRequest.type = request.getScanType();
            halRequest.interval = request.getSearchPeriodicity();
            halRequest.maxSearchTime = request.getMaxSearchTime();
            halRequest.incrementalResultsPeriodicity = request.getIncrementalResultsPeriodicity();
            halRequest.incrementalResults = request.getIncrementalResults();
            halRequest.mccMncs.addAll(request.getPlmns());

            for (RadioAccessSpecifier ras : request.getSpecifiers()) {
                android.hardware.radio.V1_1.RadioAccessSpecifier rasInHalFormat =
                        RILUtils.convertToHalRadioAccessSpecifier11(ras);
                if (rasInHalFormat == null) {
                    AsyncResult.forMessage(result, null,
                            CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                    return;
                }
                halRequest.specifiers.add(rasInHalFormat);
            }
            mRadioProxy.startNetworkScan_1_4(serial, halRequest);
        }
    }

    /**
     * Call IRadioNetwork#stopNetworkScan
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void stopNetworkScan(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.stopNetworkScan(serial);
        } else {
            mRadioProxy.stopNetworkScan(serial);
        }
    }

    /**
     * Call IRadioNetwork#supplyNetworkDepersonalization
     * @param serial Serial number of request
     * @param netPin Network depersonalization code
     * @throws RemoteException
     */
    public void supplyNetworkDepersonalization(int serial, String netPin) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.supplyNetworkDepersonalization(serial, netPin);
        } else {
            mRadioProxy.supplyNetworkDepersonalization(serial, netPin);
        }
    }

    /**
     * Call IRadioNetwork#getUsageSetting()
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getUsageSetting(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.getUsageSetting(serial);
        }
        // Only supported on AIDL.
    }

    /**
     * Call IRadioNetwork#setUsageSetting()
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void setUsageSetting(int serial,
            /* TelephonyManager.UsageSetting */ int usageSetting) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setUsageSetting(serial, usageSetting);
        }
        // Only supported on AIDL.
    }

    /**
     * Set the Emergency Mode
     *
     * @param serial Serial number of the request.
     * @param emcModeType Defines the radio emergency mode type.
     * @throws RemoteException
     */
    public void setEmergencyMode(int serial, int emcModeType) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setEmergencyMode(serial, emcModeType);
        }
        // Only supported on AIDL.
    }

    /**
     * Triggers an Emergency network scan.
     *
     * @param serial Serial number of the request.
     * @param scanRequest Contains the preferred networks and type of service to be scanned.
     * @throws RemoteException
     */
    public void triggerEmergencyNetworkScan(int serial,
            android.hardware.radio.network.EmergencyNetworkScanTrigger scanRequest)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.triggerEmergencyNetworkScan(serial, scanRequest);
        }
        // Only supported on AIDL.
    }

    /**
     * Cancels ongoing Emergency network scan
     *
     * @param serial Serial number of the request.
     * @param resetScan Indicates how the next {@link #triggerEmergencyNetworkScan} should work.
     *        If {@code true}, then the modem shall start the new scan from the beginning,
     *        otherwise the modem shall resume from the last search.
     *
     * @throws RemoteException
     */
    public void cancelEmergencyNetworkScan(int serial, boolean resetScan) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.cancelEmergencyNetworkScan(serial, resetScan);
        }
        // Only supported on AIDL.
    }

    /**
     * Exits ongoing Emergency Mode
     *
     * @param serial Serial number of the request.
     * @throws RemoteException
     */
    public void exitEmergencyMode(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.exitEmergencyMode(serial);
        }
        // Only supported on AIDL.
    }

    /**
     * Set if null ciphering / null integrity is permitted.
     *
     * @param serial Serial number of the request.
     * @param enabled true if null modes are allowed, false otherwise
     * @throws RemoteException
     */
    public void setNullCipherAndIntegrityEnabled(int serial,
            boolean enabled) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setNullCipherAndIntegrityEnabled(serial, enabled);
        }
        // Only supported on AIDL.
    }

    /**
     * Get if null ciphering / null integrity is permitted.
     * @param serial Serial number of the request.
     * @throws RemoteException
     *
     */
    public void isNullCipherAndIntegrityEnabled(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.isNullCipherAndIntegrityEnabled(serial);
        }
        // Only supported on AIDL.
    }

    /**
     * Checks whether N1 mode is enabled.
     *
     * @param serial Serial number of the request.
     * @throws RemoteException
     */
    public void isN1ModeEnabled(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.isN1ModeEnabled(serial);
        }
        // Only supported on AIDL.
    }

    /**
     * Enables or disables N1 mode.
     *
     * @param serial Serial number of request.
     * @param enable Indicates whether to enable N1 mode or not.
     * @throws RemoteException
     */
    public void setN1ModeEnabled(int serial, boolean enable) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setN1ModeEnabled(serial, enable);
        }
        // Only supported on AIDL.
    }

    /**
     * Enables or disables cellular identifier disclosure transparency.
     *
     * @param serial Serial number of request.
     * @param enable Indicates whether to enable disclosure transparency or not.
     */
    public void setCellularIdentifierTransparencyEnabled(int serial, boolean enable)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setCellularIdentifierTransparencyEnabled(serial, enable);
        }
        // Only supported on AIDL.
    }

    /**
     * Checks whether cellular identifier transparency disclosure is enabled.
     *
     * @param serial Serial number of request.
     */
    public void isCellularIdentifierTransparencyEnabled(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.isCellularIdentifierTransparencyEnabled(serial);
        }
        // Only supported on AIDL.
    }

    /**
     * Checks security algorithm update reports are enabled.
     *
     * @param serial Serial number of the request.
     * @throws RemoteException
     */
    public void isSecurityAlgorithmsUpdatedEnabled(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.isSecurityAlgorithmsUpdatedEnabled(serial);
        }
        // Only supported on AIDL.
    }

    /**
     * Enables or disables security algorithm update reports.
     *
     * @param serial Serial number of request.
     * @param enable Indicates whether to enable or disable security algorithm update reports.
     * @throws RemoteException
     */
    public void setSecurityAlgorithmsUpdatedEnabled(int serial,
            boolean enable) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setSecurityAlgorithmsUpdatedEnabled(serial, enable);
        }
        // Only supported on AIDL.
    }

   /**
     * Set the non-terrestrial PLMN with lower priority than terrestrial networks.
     *
     * @param serial Serial number of request.
     * @param carrierPlmnList The list of roaming PLMN used for connecting to satellite networks
     *                        supported by user subscription.
     * @param allSatellitePlmnList Modem should use the allSatellitePlmnList to identify satellite
     *                             PLMNs that are not supported by the carrier and make sure not to
     *                             attach to them.
     */
    public void setSatellitePlmn(int serial, List<String> carrierPlmnList,
            List<String> allSatellitePlmnList) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            String[] carrierPlmnArray = carrierPlmnList.toArray(new String[0]);
            String[] allSatellitePlmnArray = allSatellitePlmnList.toArray(new String[0]);
            mNetworkProxy.setSatellitePlmn(serial, carrierPlmnArray, allSatellitePlmnArray);
        }
        // Only supported on AIDL.
    }

    /**
     * Enable or disable satellite in the cellular modem associated with a carrier.
     *
     * @param serial Serial number of request.
     * @param satelliteEnabled {@code true} to enable satellite, {@code false} to disable satellite.
     */
    public void setSatelliteEnabledForCarrier(
            int serial, boolean satelliteEnabled) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.setSatelliteEnabledForCarrier(serial, satelliteEnabled);
        }
        // Only supported on AIDL.
    }

    /**
     * Check whether satellite is enabled in the cellular modem associated with a carrier.
     *
     * @param serial Serial number of request.
     */
    public void isSatelliteEnabledForCarrier(int serial)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mNetworkProxy.isSatelliteEnabledForCarrier(serial);
        }
        // Only supported on AIDL.
    }
}
