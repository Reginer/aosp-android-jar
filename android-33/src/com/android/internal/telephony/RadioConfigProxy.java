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

import android.os.HwBinder;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.UiccSlotMapping;

import com.android.telephony.Rlog;

import java.util.List;
import java.util.Set;

/**
 * RadioConfig proxy class that abstracts the underlying RadioConfig service implementation to
 * downstream users.
 */
public class RadioConfigProxy {
    private final HalVersion mRadioHalVersion;
    private final RadioConfigHidlServiceDeathRecipient mRadioConfigHidlServiceDeathRecipient;
    private final RadioConfigAidlServiceDeathRecipient mRadioConfigAidlServiceDeathRecipient;

    private volatile android.hardware.radio.config.V1_0.IRadioConfig mHidlRadioConfigProxy = null;
    private volatile android.hardware.radio.config.IRadioConfig mAidlRadioConfigProxy = null;

    private HalVersion mRadioConfigHalVersion = RadioConfig.RADIO_CONFIG_HAL_VERSION_UNKNOWN;
    private boolean mIsAidl;

    public RadioConfigProxy(RadioConfig radioConfig, HalVersion radioHalVersion) {
        mRadioHalVersion = radioHalVersion;
        mRadioConfigAidlServiceDeathRecipient =
                new RadioConfigAidlServiceDeathRecipient(radioConfig);
        mRadioConfigHidlServiceDeathRecipient =
                new RadioConfigHidlServiceDeathRecipient(radioConfig);
    }

    /**
     * Set IRadioConfig as the HIDL implementation for RadioConfigProxy
     *
     * @param radioConfigHalVersion RadioConfig HAL version
     * @param radioConfig IRadioConfig implementation
     */
    public void setHidl(
            HalVersion radioConfigHalVersion,
            android.hardware.radio.config.V1_0.IRadioConfig radioConfig) {
        mRadioConfigHalVersion = radioConfigHalVersion;
        mHidlRadioConfigProxy = radioConfig;
        mIsAidl = false;
        mRadioConfigHidlServiceDeathRecipient.setService(radioConfig);
    }

    /**
     * Get HIDL IRadioConfig V1_0
     * @return IRadioConfigV1_0
     */
    public android.hardware.radio.config.V1_0.IRadioConfig getHidl10() {
        return mHidlRadioConfigProxy;
    }

    /**
     * Get HIDL IRadioConfig V1_1
     * @return IRadioConfigV1_1
     */
    public android.hardware.radio.config.V1_1.IRadioConfig getHidl11() {
        return (android.hardware.radio.config.V1_1.IRadioConfig) mHidlRadioConfigProxy;
    }

    /**
     * Get HIDL IRadioConfig V1_3
     * @return IRadioConfigV1_3
     */
    public android.hardware.radio.config.V1_3.IRadioConfig getHidl13() {
        return (android.hardware.radio.config.V1_3.IRadioConfig) mHidlRadioConfigProxy;
    }

    /**
     * Set IRadioConfig as the AIDL implementation for RadioConfigProxy
     *
     * @param radioConfigHalVersion RadioConfig HAL version
     * @param radioConfig IRadioConfig implementation
     */
    public void setAidl(
            HalVersion radioConfigHalVersion,
            android.hardware.radio.config.IRadioConfig radioConfig) {
        mRadioConfigHalVersion = radioConfigHalVersion;
        mAidlRadioConfigProxy = radioConfig;
        mIsAidl = true;
        mRadioConfigAidlServiceDeathRecipient.setService(radioConfig.asBinder());
    }

    /**
     * Get the AIDL implementation of RadioConfigProxy
     *
     * @return IRadio implementation
     */
    public android.hardware.radio.config.IRadioConfig getAidl() {
        return mAidlRadioConfigProxy;
    }

    /** Reset RadioConfigProxy */
    public void clear() {
        mRadioConfigHalVersion = RadioConfig.RADIO_CONFIG_HAL_VERSION_UNKNOWN;
        mHidlRadioConfigProxy = null;
        mAidlRadioConfigProxy = null;
        mRadioConfigHidlServiceDeathRecipient.clear();
        mRadioConfigAidlServiceDeathRecipient.clear();
    }

    /**
     * Wrapper for service's linkToDeath()
     */
    public void linkToDeath(long cookie) throws RemoteException {
        if (isAidl()) {
            mRadioConfigAidlServiceDeathRecipient.linkToDeath((int) cookie);
        } else {
            mRadioConfigHidlServiceDeathRecipient.linkToDeath(cookie);
        }
    }

    /**
     * Check whether an implementation exists for this service
     *
     * @return false if there is neither a HIDL nor AIDL implementation
     */
    public boolean isEmpty() {
        return mAidlRadioConfigProxy == null && mHidlRadioConfigProxy == null;
    }

    /**
     * Whether RadioConfigProxy is an AIDL or HIDL implementation
     *
     * @return true if AIDL, false if HIDL
     */
    public boolean isAidl() {
        return mIsAidl;
    }

    /**
     * Return RadioConfig HAL version used by this instance
     * @return RadioConfig HAL Version
     */
    public HalVersion getVersion() {
        return mRadioConfigHalVersion;
    }

    /**
     * Set the response functions for RadioConfig instance
     * @param radioConfig main RadioConfig instance
     * @throws RemoteException
     */
    public void setResponseFunctions(RadioConfig radioConfig) throws RemoteException {
        if (isEmpty()) return;

        if (isAidl()) {
            mAidlRadioConfigProxy.setResponseFunctions(
                    new RadioConfigResponseAidl(radioConfig, mRadioHalVersion),
                    new RadioConfigIndicationAidl(radioConfig));
        } else {
            mHidlRadioConfigProxy.setResponseFunctions(
                    new RadioConfigResponseHidl(radioConfig, mRadioHalVersion),
                    new RadioConfigIndicationHidl(radioConfig));
        }
    }

    /**
     * Get capabilities based off of the radio hal version and feature set configurations
     * @return Set string capabilities
     */
    public Set<String> getFullCapabilitySet() {
        return RILUtils.getCaps(mRadioHalVersion, false);
    }

    /**
     * Wrapper function for IRadioConfig.getSimSlotsStatus().
     */
    public void getSimSlotStatus(int serial) throws RemoteException {
        if (isAidl()) {
            getAidl().getSimSlotsStatus(serial);
        } else {
            getHidl10().getSimSlotsStatus(serial);
        }
    }

    /**
     * Wrapper function for IRadioConfig.setPreferredDataModem(int modemId).
     */
    public void setPreferredDataModem(int serial, int modemId) throws RemoteException {
        if (isAidl()) {
            getAidl().setPreferredDataModem(serial, (byte) modemId);
        } else {
            getHidl11().setPreferredDataModem(serial, (byte) modemId);
        }
    }

    /**
     * Wrapper function for IRadioConfig.getPhoneCapability().
     */
    public void getPhoneCapability(int serial) throws RemoteException {
        if (isAidl()) {
            getAidl().getPhoneCapability(serial);
        } else {
            getHidl11().getPhoneCapability(serial);
        }
    }

    /**
     * Wrapper function for IRadioConfig.setSimSlotsMapping(int32_t serial,
     * vec<SlotPortMapping> portMap).
     */
    public void setSimSlotsMapping(int serial, List<UiccSlotMapping> slotMapping)
            throws RemoteException {
        if (isAidl()) {
            getAidl().setSimSlotsMapping(serial, RILUtils.convertSimSlotsMapping(slotMapping));
        } else {
            getHidl10().setSimSlotsMapping(serial,
                    RILUtils.convertSlotMappingToList(slotMapping));
        }
    }

    /**
     * Wrapper function for using IRadioConfig.setNumOfLiveModems(int32_t serial,
     * byte numOfLiveModems) to switch between single-sim and multi-sim.
     */
    public void setNumOfLiveModems(int serial, int numOfLiveModems) throws RemoteException {
        if (isAidl()) {
            getAidl().setNumOfLiveModems(serial, (byte) numOfLiveModems);
        } else {
            android.hardware.radio.config.V1_1.ModemsConfig modemsConfig =
                    new android.hardware.radio.config.V1_1.ModemsConfig();
            modemsConfig.numOfLiveModems = (byte) numOfLiveModems;
            getHidl11().setModemsConfig(serial, modemsConfig);
        }
    }

    /**
     * Gets the hal capabilities from the device.
     */
    public void getHalDeviceCapabilities(int serial) throws RemoteException {
        if (isAidl()) {
            getAidl().getHalDeviceCapabilities(serial);
        } else {
            getHidl13().getHalDeviceCapabilities(serial);
        }
    }

    /**
     * Death Recipient for HIDL binder (if any) of RadioConfig.
     */
    private static class RadioConfigHidlServiceDeathRecipient implements HwBinder.DeathRecipient {
        private static final String TAG = "RadioConfigHidlSDR";

        private final RadioConfig mRadioConfig;
        private android.hardware.radio.config.V1_0.IRadioConfig mService;

        RadioConfigHidlServiceDeathRecipient(RadioConfig radioConfig) {
            mRadioConfig = radioConfig;
        }

        public void setService(android.hardware.radio.config.V1_0.IRadioConfig service) {
            mService = service;
        }

        public void linkToDeath(long cookie) throws RemoteException {
            mService.linkToDeath(this, cookie);
        }

        public void clear() {
            mService = null;
        }

        @Override
        public void serviceDied(long cookie) {
            // Deal with service going away
            Rlog.e(TAG, "serviceDied");
            mRadioConfig.sendMessage(
                    mRadioConfig.obtainMessage(RadioConfig.EVENT_HIDL_SERVICE_DEAD, cookie));
        }
    }

    /**
     * DeathRecipient for AIDL binder service (if any) of RadioConfig
     */
    private static class RadioConfigAidlServiceDeathRecipient implements IBinder.DeathRecipient {
        private static final String TAG = "RadioConfigAidlSDR";

        private final RadioConfig mRadioConfig;

        private IBinder mService;

        RadioConfigAidlServiceDeathRecipient(RadioConfig radioConfig) {
            mRadioConfig = radioConfig;
        }

        public void setService(IBinder service) {
            mService = service;
        }

        public void linkToDeath(int cookie) throws RemoteException {
            mService.linkToDeath(this, cookie);
        }

        public void clear() {
            mService = null;
        }

        /**
         * Unlink from RadioConfig if any.
         */
        public synchronized void unlinkToDeath() {
            if (mService != null) {
                mService.unlinkToDeath(this, 0);
                mService = null;
            }
        }

        @Override
        public void binderDied() {
            Rlog.e(TAG, "service died.");
            unlinkToDeath();
            mRadioConfig.sendMessage(
                    mRadioConfig.obtainMessage(RadioConfig.EVENT_AIDL_SERVICE_DEAD));
        }
    }
}
