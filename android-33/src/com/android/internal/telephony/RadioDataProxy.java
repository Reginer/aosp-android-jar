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

import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.TrafficDescriptor;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * A holder for IRadioData. Use getHidl to get IRadio 1.0 and call the HIDL implementations or
 * getAidl to get IRadioData and call the AIDL implementations of the HAL APIs.
 */
public class RadioDataProxy extends RadioServiceProxy {
    private static final String TAG = "RadioDataProxy";
    private volatile android.hardware.radio.data.IRadioData mDataProxy = null;

    /**
     * Set IRadioData as the AIDL implementation for RadioServiceProxy
     * @param halVersion Radio HAL version
     * @param data IRadioData implementation
     */
    public void setAidl(HalVersion halVersion, android.hardware.radio.data.IRadioData data) {
        mHalVersion = halVersion;
        mDataProxy = data;
        mIsAidl = true;
        Rlog.d(TAG, "AIDL initialized");
    }

    /**
     * Get the AIDL implementation of RadioDataProxy
     * @return IRadioData implementation
     */
    public android.hardware.radio.data.IRadioData getAidl() {
        return mDataProxy;
    }

    /**
     * Reset RadioDataProxy
     */
    @Override
    public void clear() {
        super.clear();
        mDataProxy = null;
    }

    /**
     * Check whether a RadioData implementation exists
     * @return true if there is neither a HIDL nor AIDL implementation
     */
    @Override
    public boolean isEmpty() {
        return mRadioProxy == null && mDataProxy == null;
    }

    /**
     * Call IRadioData#allocatePduSessionId
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void allocatePduSessionId(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mDataProxy.allocatePduSessionId(serial);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).allocatePduSessionId(serial);
        }
    }

    /**
     * Call IRadioData#cancelHandover
     * @param serial Serial number of request
     * @param callId Identifier associated with the data call
     * @throws RemoteException
     */
    public void cancelHandover(int serial, int callId) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mDataProxy.cancelHandover(serial, callId);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).cancelHandover(serial, callId);
        }
    }

    /**
     * Call IRadioData#deactivateDataCall
     * @param serial Serial number of request
     * @param cid The connection ID
     * @param reason Data disconnect reason
     * @throws RemoteException
     */
    public void deactivateDataCall(int serial, int cid, int reason) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mDataProxy.deactivateDataCall(serial, cid, reason);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_2)) {
            ((android.hardware.radio.V1_2.IRadio) mRadioProxy).deactivateDataCall_1_2(
                    serial, cid, reason);
        } else {
            mRadioProxy.deactivateDataCall(serial, cid,
                    reason == DataService.REQUEST_REASON_SHUTDOWN);
        }
    }

    /**
     * Call IRadioData#getDataCallList
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getDataCallList(int serial) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mDataProxy.getDataCallList(serial);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getDataCallList_1_6(serial);
        } else {
            mRadioProxy.getDataCallList(serial);
        }
    }

    /**
     * Call IRadioData#getSlicingConfig
     * @param serial Serial number of request
     * @throws RemoteException
     */
    public void getSlicingConfig(int serial) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mDataProxy.getSlicingConfig(serial);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).getSlicingConfig(serial);
        }
    }

    /**
     * Call IRadioData#releasePduSessionId
     * @param serial Serial number of request
     * @param id PDU session ID to release
     * @throws RemoteException
     */
    public void releasePduSessionId(int serial, int id) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mDataProxy.releasePduSessionId(serial, id);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).releasePduSessionId(serial, id);
        }
    }

    /**
     * Call IRadioData#responseAcknowledgement
     * @throws RemoteException
     */
    @Override
    public void responseAcknowledgement() throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mDataProxy.responseAcknowledgement();
        } else {
            mRadioProxy.responseAcknowledgement();
        }
    }

    /**
     * Call IRadioData#setDataAllowed
     * @param serial Serial number of request
     * @param allow Whether to allow or disallow data calls
     * @throws RemoteException
     */
    public void setDataAllowed(int serial, boolean allow) throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mDataProxy.setDataAllowed(serial, allow);
        } else {
            mRadioProxy.setDataAllowed(serial, allow);
        }
    }

    /**
     * Call IRadioData#setDataProfile
     * @param serial Serial number of request
     * @param profiles Array of DataProfiles to set
     * @param isRoaming Whether or not the device is roaming
     * @throws RemoteException
     */
    public void setDataProfile(int serial, DataProfile[] profiles, boolean isRoaming)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            android.hardware.radio.data.DataProfileInfo[] dpis =
                    new android.hardware.radio.data.DataProfileInfo[profiles.length];
            for (int i = 0; i < profiles.length; i++) {
                dpis[i] = RILUtils.convertToHalDataProfile(profiles[i]);
            }
            mDataProxy.setDataProfile(serial, dpis);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ArrayList<android.hardware.radio.V1_5.DataProfileInfo> dpis = new ArrayList<>();
            for (DataProfile dp : profiles) {
                dpis.add(RILUtils.convertToHalDataProfile15(dp));
            }
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).setDataProfile_1_5(serial, dpis);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_4)) {
            ArrayList<android.hardware.radio.V1_4.DataProfileInfo> dpis = new ArrayList<>();
            for (DataProfile dp : profiles) {
                dpis.add(RILUtils.convertToHalDataProfile14(dp));
            }
            ((android.hardware.radio.V1_4.IRadio) mRadioProxy).setDataProfile_1_4(serial, dpis);
        } else {
            ArrayList<android.hardware.radio.V1_0.DataProfileInfo> dpis = new ArrayList<>();
            for (DataProfile dp : profiles) {
                if (dp.isPersistent()) {
                    dpis.add(RILUtils.convertToHalDataProfile10(dp));
                }
            }
            if (!dpis.isEmpty()) {
                mRadioProxy.setDataProfile(serial, dpis, isRoaming);
            }
        }
    }

    /**
     * Call IRadioData#setDataThrottling
     * @param serial Serial number of request
     * @param dataThrottlingAction DataThrottlingAction as defined in DataThrottlingAction.aidl
     * @param completionDurationMillis Window in ms in which the requested throttling action has to
     *                                 be achieved.
     * @throws RemoteException
     */
    public void setDataThrottling(int serial, byte dataThrottlingAction,
            long completionDurationMillis) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mDataProxy.setDataThrottling(serial, dataThrottlingAction, completionDurationMillis);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).setDataThrottling(serial,
                    dataThrottlingAction, completionDurationMillis);
        }
    }

    /**
     * Call IRadioData#setInitialAttachApn
     * @param serial Serial number of request
     * @param dataProfile Data profile containing APN settings
     * @param isRoaming Whether or not the device is roaming
     * @throws RemoteException
     */
    public void setInitialAttachApn(int serial, DataProfile dataProfile, boolean isRoaming)
            throws RemoteException {
        if (isEmpty()) return;
        if (isAidl()) {
            mDataProxy.setInitialAttachApn(serial, RILUtils.convertToHalDataProfile(dataProfile));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).setInitialAttachApn_1_5(serial,
                    RILUtils.convertToHalDataProfile15(dataProfile));
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_4)) {
            ((android.hardware.radio.V1_4.IRadio) mRadioProxy).setInitialAttachApn_1_4(serial,
                    RILUtils.convertToHalDataProfile14(dataProfile));
        } else {
            mRadioProxy.setInitialAttachApn(serial, RILUtils.convertToHalDataProfile10(dataProfile),
                    dataProfile.isPersistent(), isRoaming);
        }
    }

    /**
     * Call IRadioData#setupDataCall
     * @param serial Serial number of request
     * @param phoneId Phone ID of the requestor
     * @param accessNetwork Access network to setup the data call
     * @param dataProfileInfo Data profile info
     * @param isRoaming Whether or not the device is roaming
     * @param roamingAllowed Whether or not data roaming is allowed by the user
     * @param reason Request reason
     * @param linkProperties LinkProperties containing address and DNS info
     * @param pduSessionId The PDU session ID to be used for this data call
     * @param sliceInfo SliceInfo to be used for the data connection when a handover occurs from
     *                  EPDG to 5G
     * @param trafficDescriptor TrafficDescriptor for which the data connection needs to be
     *                          established
     * @param matchAllRuleAllowed Whether or not the default match-all URSP rule for this request
     *                            is allowed
     * @throws RemoteException
     */
    public void setupDataCall(int serial, int phoneId, int accessNetwork,
            DataProfile dataProfileInfo, boolean isRoaming, boolean roamingAllowed, int reason,
            LinkProperties linkProperties, int pduSessionId, NetworkSliceInfo sliceInfo,
            TrafficDescriptor trafficDescriptor, boolean matchAllRuleAllowed)
            throws RemoteException {
        if (isEmpty()) return;
        ArrayList<String> addresses = new ArrayList<>();
        ArrayList<String> dnses = new ArrayList<>();
        String[] dnsesArr = null;
        if (linkProperties != null) {
            for (InetAddress address : linkProperties.getAddresses()) {
                addresses.add(address.getHostAddress());
            }
            dnsesArr = new String[linkProperties.getDnsServers().size()];
            for (int i = 0; i < linkProperties.getDnsServers().size(); i++) {
                dnses.add(linkProperties.getDnsServers().get(i).getHostAddress());
                dnsesArr[i] = linkProperties.getDnsServers().get(i).getHostAddress();
            }
        } else {
            dnsesArr = new String[0];
        }
        if (isAidl()) {
            // Create a new DataProfile to set the TrafficDescriptor
            DataProfile dp = new DataProfile.Builder()
                    .setType(dataProfileInfo.getType())
                    .setPreferred(dataProfileInfo.isPreferred())
                    .setTrafficDescriptor(trafficDescriptor)
                    .setApnSetting(dataProfileInfo.getApnSetting())
                    .build();
            mDataProxy.setupDataCall(serial, accessNetwork, RILUtils.convertToHalDataProfile(dp),
                    roamingAllowed, reason, RILUtils.convertToHalLinkProperties(linkProperties),
                    dnsesArr, pduSessionId, RILUtils.convertToHalSliceInfoAidl(sliceInfo),
                    matchAllRuleAllowed);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).setupDataCall_1_6(serial,
                    accessNetwork, RILUtils.convertToHalDataProfile15(dataProfileInfo),
                    roamingAllowed, reason, RILUtils.convertToHalLinkProperties15(linkProperties),
                    dnses, pduSessionId, RILUtils.convertToHalSliceInfo(sliceInfo),
                    RILUtils.convertToHalTrafficDescriptor(trafficDescriptor),
                    matchAllRuleAllowed);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            ((android.hardware.radio.V1_5.IRadio) mRadioProxy).setupDataCall_1_5(serial,
                    accessNetwork, RILUtils.convertToHalDataProfile15(dataProfileInfo),
                    roamingAllowed, reason, RILUtils.convertToHalLinkProperties15(linkProperties),
                    dnses);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_4)) {
            ((android.hardware.radio.V1_4.IRadio) mRadioProxy).setupDataCall_1_4(serial,
                    accessNetwork, RILUtils.convertToHalDataProfile14(dataProfileInfo),
                    roamingAllowed, reason, addresses, dnses);
        } else if (mHalVersion.greaterOrEqual(RIL.RADIO_HAL_VERSION_1_2)) {
            ((android.hardware.radio.V1_2.IRadio) mRadioProxy).setupDataCall_1_2(serial,
                    accessNetwork, RILUtils.convertToHalDataProfile10(dataProfileInfo),
                    dataProfileInfo.isPersistent(), roamingAllowed, isRoaming, reason, addresses,
                    dnses);
        } else {
            // Getting data RAT here is just a workaround to support the older 1.0 vendor RIL.
            // The new data service interface passes access network type instead of RAT for
            // setup data request. It is impossible to convert access network type back to RAT here,
            // so we directly get the data RAT from phone.
            int dataRat = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
            Phone phone = PhoneFactory.getPhone(phoneId);
            if (phone != null) {
                ServiceState ss = phone.getServiceState();
                if (ss != null) {
                    dataRat = ss.getRilDataRadioTechnology();
                }
            }
            mRadioProxy.setupDataCall(serial, dataRat,
                    RILUtils.convertToHalDataProfile10(dataProfileInfo),
                    dataProfileInfo.isPersistent(), roamingAllowed, isRoaming);
        }
    }

    /**
     * Call IRadioData#startHandover
     * @param serial Serial number of request
     * @param callId Identifier of the data call
     * @throws RemoteException
     */
    public void startHandover(int serial, int callId) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_6)) return;
        if (isAidl()) {
            mDataProxy.startHandover(serial, callId);
        } else {
            ((android.hardware.radio.V1_6.IRadio) mRadioProxy).startHandover(serial, callId);
        }
    }

    /**
     * Call IRadioData#startKeepalive
     * @param serial Serial number of request
     * @param contextId Context ID for the data call
     * @param packetData Keepalive packet data
     * @param intervalMillis Max keepalive interval in ms
     * @param result Result to return in case of invalid arguments
     * @throws RemoteException
     */
    public void startKeepalive(int serial, int contextId, KeepalivePacketData packetData,
            int intervalMillis, Message result) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_1)) return;
        if (isAidl()) {
            android.hardware.radio.data.KeepaliveRequest req =
                    new android.hardware.radio.data.KeepaliveRequest();
            req.cid = contextId;

            if (packetData.getDstAddress() instanceof Inet4Address) {
                req.type = android.hardware.radio.data.KeepaliveRequest.TYPE_NATT_IPV4;
            } else if (packetData.getDstAddress() instanceof Inet6Address) {
                req.type = android.hardware.radio.data.KeepaliveRequest.TYPE_NATT_IPV6;
            } else {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RILConstants.INVALID_ARGUMENTS));
                result.sendToTarget();
                return;
            }

            final InetAddress srcAddress = packetData.getSrcAddress();
            final InetAddress dstAddress = packetData.getDstAddress();
            byte[] sourceAddress = new byte[srcAddress.getAddress().length];
            for (int i = 0; i < sourceAddress.length; i++) {
                sourceAddress[i] = srcAddress.getAddress()[i];
            }
            req.sourceAddress = sourceAddress;
            req.sourcePort = packetData.getSrcPort();
            byte[] destinationAddress = new byte[dstAddress.getAddress().length];
            for (int i = 0; i < destinationAddress.length; i++) {
                destinationAddress[i] = dstAddress.getAddress()[i];
            }
            req.destinationAddress = destinationAddress;
            req.destinationPort = packetData.getDstPort();
            req.maxKeepaliveIntervalMillis = intervalMillis;

            mDataProxy.startKeepalive(serial, req);
        } else {
            android.hardware.radio.V1_1.KeepaliveRequest req =
                    new android.hardware.radio.V1_1.KeepaliveRequest();

            req.cid = contextId;

            if (packetData.getDstAddress() instanceof Inet4Address) {
                req.type = android.hardware.radio.V1_1.KeepaliveType.NATT_IPV4;
            } else if (packetData.getDstAddress() instanceof Inet6Address) {
                req.type = android.hardware.radio.V1_1.KeepaliveType.NATT_IPV6;
            } else {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RILConstants.INVALID_ARGUMENTS));
                result.sendToTarget();
                return;
            }

            final InetAddress srcAddress = packetData.getSrcAddress();
            final InetAddress dstAddress = packetData.getDstAddress();
            RILUtils.appendPrimitiveArrayToArrayList(
                    srcAddress.getAddress(), req.sourceAddress);
            req.sourcePort = packetData.getSrcPort();
            RILUtils.appendPrimitiveArrayToArrayList(
                    dstAddress.getAddress(), req.destinationAddress);
            req.destinationPort = packetData.getDstPort();
            req.maxKeepaliveIntervalMillis = intervalMillis;

            ((android.hardware.radio.V1_1.IRadio) mRadioProxy).startKeepalive(serial, req);
        }
    }

    /**
     * Call IRadioData#stopKeepalive
     * @param serial Serial number of request
     * @param sessionHandle The handle that was provided by startKeepaliveResponse
     * @throws RemoteException
     */
    public void stopKeepalive(int serial, int sessionHandle) throws RemoteException {
        if (isEmpty() || mHalVersion.less(RIL.RADIO_HAL_VERSION_1_1)) return;
        if (isAidl()) {
            mDataProxy.stopKeepalive(serial, sessionHandle);
        } else {
            ((android.hardware.radio.V1_1.IRadio) mRadioProxy).stopKeepalive(serial, sessionHandle);
        }
    }
}
