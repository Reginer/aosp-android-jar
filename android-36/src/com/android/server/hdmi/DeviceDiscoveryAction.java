/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.server.hdmi;

import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.util.Slog;

import com.android.internal.util.Preconditions;
import com.android.server.hdmi.HdmiControlService.DevicePollingCallback;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Feature action that handles device discovery sequences.
 * Device discovery is launched when device is woken from "Standby" state
 * or enabled "Control for Hdmi" from disabled state.
 *
 * <p>Device discovery goes through the following steps.
 * <ol>
 *   <li>Poll all non-local devices by sending &lt;Polling Message&gt;
 *   <li>Gather "Physical address" and "device type" of all acknowledged devices
 *   <li>Gather "OSD (display) name" of all acknowledge devices
 *   <li>Gather "Vendor id" of all acknowledge devices
 * </ol>
 * We attempt to get OSD name/vendor ID up to 5 times in case the communication fails.
 */
final class DeviceDiscoveryAction extends HdmiCecFeatureAction {
    private static final String TAG = "DeviceDiscoveryAction";

    // State in which the action is waiting for device polling.
    private static final int STATE_WAITING_FOR_DEVICE_POLLING = 1;
    // State in which the action is waiting for gathering physical address of non-local devices.
    private static final int STATE_WAITING_FOR_PHYSICAL_ADDRESS = 2;
    // State in which the action is waiting for gathering osd name of non-local devices.
    private static final int STATE_WAITING_FOR_OSD_NAME = 3;
    // State in which the action is waiting for gathering vendor id of non-local devices.
    private static final int STATE_WAITING_FOR_VENDOR_ID = 4;
    // State in which the action is waiting for devices to be ready.
    private static final int STATE_WAITING_FOR_DEVICES = 5;
    // State in which the action is waiting for gathering power status of non-local devices.
    private static final int STATE_WAITING_FOR_POWER = 6;

    /**
     * Interface used to report result of device discovery.
     */
    interface DeviceDiscoveryCallback {
        /**
         * Called when device discovery is done.
         *
         * @param deviceInfos a list of all non-local devices. It can be empty list.
         */
        void onDeviceDiscoveryDone(List<HdmiDeviceInfo> deviceInfos);
    }

    // An internal container used to keep track of device information during
    // this action.
    private static final class DeviceInfo {
        private final int mLogicalAddress;

        private int mPhysicalAddress = Constants.INVALID_PHYSICAL_ADDRESS;
        private int mPortId = Constants.INVALID_PORT_ID;
        private int mVendorId = Constants.VENDOR_ID_UNKNOWN;
        private int mPowerStatus = HdmiControlManager.POWER_STATUS_UNKNOWN;
        private String mDisplayName = "";
        private int mDeviceType = HdmiDeviceInfo.DEVICE_INACTIVE;

        private DeviceInfo(int logicalAddress) {
            mLogicalAddress = logicalAddress;
        }

        private HdmiDeviceInfo toHdmiDeviceInfo() {
            return  HdmiDeviceInfo.cecDeviceBuilder()
                    .setLogicalAddress(mLogicalAddress)
                    .setPhysicalAddress(mPhysicalAddress)
                    .setPortId(mPortId)
                    .setVendorId(mVendorId)
                    .setDeviceType(mDeviceType)
                    .setDisplayName(mDisplayName)
                    .setDevicePowerStatus(mPowerStatus)
                    .build();
        }
    }

    private final ArrayList<DeviceInfo> mDevices = new ArrayList<>();
    private final DeviceDiscoveryCallback mCallback;
    private int mProcessedDeviceCount = 0;
    private int mTimeoutRetry = 0;
    private boolean mIsTvDevice = localDevice().mService.isTvDevice();
    private final int mDelayPeriod;

    /**
     * Constructor.
     *
     * @param source an instance of {@link HdmiCecLocalDevice}.
     * @param delay delay action for this period between query Physical Address and polling
     */
    DeviceDiscoveryAction(HdmiCecLocalDevice source, DeviceDiscoveryCallback callback, int delay) {
        super(source);
        mCallback = Objects.requireNonNull(callback);
        mDelayPeriod = delay;
    }

    /**
     * Constructor.
     *
     * @param source an instance of {@link HdmiCecLocalDevice}.
     */
    DeviceDiscoveryAction(HdmiCecLocalDevice source, DeviceDiscoveryCallback callback) {
        this(source, callback, 0);
    }

    @Override
    boolean start() {
        mDevices.clear();
        mState = STATE_WAITING_FOR_DEVICE_POLLING;

        pollDevices(new DevicePollingCallback() {
            @Override
            public void onPollingFinished(List<Integer> ackedAddress) {
                if (ackedAddress.isEmpty()) {
                    Slog.v(TAG, "No device is detected.");
                    wrapUpAndFinish();
                    return;
                }
                // Check if the action was finished before the callback was called.
                // See {@link HdmiCecFeatureAction#finish}.
                if (mState == STATE_NONE) {
                    Slog.v(TAG, "Action was already finished before the callback was called.");
                    wrapUpAndFinish();
                    return;
                }
                Slog.v(TAG, "Device detected: " + ackedAddress);
                allocateDevices(ackedAddress);
                if (mDelayPeriod > 0) {
                    startToDelayAction();
                } else {
                    startPhysicalAddressStage();
                }
            }
        }, Constants.POLL_ITERATION_REVERSE_ORDER
            | Constants.POLL_STRATEGY_REMOTES_DEVICES, HdmiConfig.DEVICE_POLLING_RETRY);
        return true;
    }

    private void allocateDevices(List<Integer> addresses) {
        for (Integer i : addresses) {
            DeviceInfo info = new DeviceInfo(i);
            mDevices.add(info);
        }
    }

    private void startToDelayAction() {
        Slog.v(TAG, "Waiting for connected devices to be ready");
        mState = STATE_WAITING_FOR_DEVICES;

        checkAndProceedStage();
    }

    private void startPhysicalAddressStage() {
        Slog.v(TAG, "Start [Physical Address Stage]:" + mDevices.size());
        mProcessedDeviceCount = 0;
        mState = STATE_WAITING_FOR_PHYSICAL_ADDRESS;

        checkAndProceedStage();
    }

    private boolean verifyValidLogicalAddress(int address) {
        return address >= Constants.ADDR_TV && address < Constants.ADDR_UNREGISTERED;
    }

    private void queryPhysicalAddress(int address) {
        if (!verifyValidLogicalAddress(address)) {
            checkAndProceedStage();
            return;
        }

        mActionTimer.clearTimerMessage();

        // Check cache first and send request if not exist.
        if (mayProcessMessageIfCached(address, Constants.MESSAGE_REPORT_PHYSICAL_ADDRESS)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGivePhysicalAddress(getSourceAddress(), address));
        addTimer(mState, HdmiConfig.TIMEOUT_MS);
    }

    private void delayActionWithTimePeriod(int timeDelay) {
        mActionTimer.clearTimerMessage();
        addTimer(mState, timeDelay);
    }

    private void startOsdNameStage() {
        Slog.v(TAG, "Start [Osd Name Stage]:" + mDevices.size());
        mProcessedDeviceCount = 0;
        mState = STATE_WAITING_FOR_OSD_NAME;

        checkAndProceedStage();
    }

    private void queryOsdName(int address) {
        if (!verifyValidLogicalAddress(address)) {
            checkAndProceedStage();
            return;
        }

        mActionTimer.clearTimerMessage();

        if (mayProcessMessageIfCached(address, Constants.MESSAGE_SET_OSD_NAME)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGiveOsdNameCommand(getSourceAddress(), address));
        addTimer(mState, HdmiConfig.TIMEOUT_MS);
    }

    private void startVendorIdStage() {
        Slog.v(TAG, "Start [Vendor Id Stage]:" + mDevices.size());

        mProcessedDeviceCount = 0;
        mState = STATE_WAITING_FOR_VENDOR_ID;

        checkAndProceedStage();
    }

    private void queryVendorId(int address) {
        if (!verifyValidLogicalAddress(address)) {
            checkAndProceedStage();
            return;
        }

        mActionTimer.clearTimerMessage();

        if (mayProcessMessageIfCached(address, Constants.MESSAGE_DEVICE_VENDOR_ID)) {
            return;
        }
        sendCommand(
                HdmiCecMessageBuilder.buildGiveDeviceVendorIdCommand(getSourceAddress(), address));
        addTimer(mState, HdmiConfig.TIMEOUT_MS);
    }

    private void startPowerStatusStage() {
        Slog.v(TAG, "Start [Power Status Stage]:" + mDevices.size());
        mProcessedDeviceCount = 0;
        mState = STATE_WAITING_FOR_POWER;

        checkAndProceedStage();
    }

    private void queryPowerStatus(int address) {
        if (!verifyValidLogicalAddress(address)) {
            checkAndProceedStage();
            return;
        }

        mActionTimer.clearTimerMessage();

        if (mayProcessMessageIfCached(address, Constants.MESSAGE_REPORT_POWER_STATUS)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), address));
        addTimer(mState, HdmiConfig.TIMEOUT_MS);
    }

    private boolean mayProcessMessageIfCached(int address, int opcode) {
        HdmiCecMessage message = getCecMessageCache().getMessage(address, opcode);
        if (message != null) {
            processCommand(message);
            return true;
        }
        return false;
    }

    @Override
    boolean processCommand(HdmiCecMessage cmd) {
        switch (mState) {
            case STATE_WAITING_FOR_PHYSICAL_ADDRESS:
                if (cmd.getOpcode() == Constants.MESSAGE_REPORT_PHYSICAL_ADDRESS) {
                    handleReportPhysicalAddress(cmd);
                    return true;
                }
                return false;
            case STATE_WAITING_FOR_OSD_NAME:
                if (cmd.getOpcode() == Constants.MESSAGE_SET_OSD_NAME) {
                    handleSetOsdName(cmd);
                    return true;
                } else if ((cmd.getOpcode() == Constants.MESSAGE_FEATURE_ABORT) &&
                        ((cmd.getParams()[0] & 0xFF) == Constants.MESSAGE_GIVE_OSD_NAME)) {
                    handleSetOsdName(cmd);
                    return true;
                }
                return false;
            case STATE_WAITING_FOR_VENDOR_ID:
                if (cmd.getOpcode() == Constants.MESSAGE_DEVICE_VENDOR_ID) {
                    handleVendorId(cmd);
                    return true;
                } else if ((cmd.getOpcode() == Constants.MESSAGE_FEATURE_ABORT) &&
                        ((cmd.getParams()[0] & 0xFF) == Constants.MESSAGE_GIVE_DEVICE_VENDOR_ID)) {
                    handleVendorId(cmd);
                    return true;
                }
                return false;
            case STATE_WAITING_FOR_POWER:
                if (cmd.getOpcode() == Constants.MESSAGE_REPORT_POWER_STATUS) {
                    handleReportPowerStatus(cmd);
                    return true;
                } else if ((cmd.getOpcode() == Constants.MESSAGE_FEATURE_ABORT)
                        && ((cmd.getParams()[0] & 0xFF) == Constants.MESSAGE_REPORT_POWER_STATUS)) {
                    handleReportPowerStatus(cmd);
                    return true;
                }
                return false;
            case STATE_WAITING_FOR_DEVICE_POLLING:
                // Fall through.
            default:
                return false;
        }
    }

    private void handleReportPhysicalAddress(HdmiCecMessage cmd) {
        Preconditions.checkState(mProcessedDeviceCount < mDevices.size());

        DeviceInfo current = mDevices.get(mProcessedDeviceCount);
        if (current.mLogicalAddress != cmd.getSource()) {
            Slog.w(TAG, "Unmatched address[expected:" + current.mLogicalAddress + ", actual:" +
                    cmd.getSource());
            return;
        }

        byte params[] = cmd.getParams();
        current.mPhysicalAddress = HdmiUtils.twoBytesToInt(params);
        current.mPortId = getPortId(current.mPhysicalAddress);
        current.mDeviceType = params[2] & 0xFF;
        // Keep display name empty. TIF fallbacks to the service label provided by the package mg.
        current.mDisplayName = "";

        // This is to manager CEC device separately in case they don't have address.
        if (mIsTvDevice) {
            localDevice().mService.getHdmiCecNetwork().updateCecSwitchInfo(current.mLogicalAddress,
                    current.mDeviceType,
                    current.mPhysicalAddress);
        }
        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private int getPortId(int physicalAddress) {
        return mIsTvDevice ? tv().getPortId(physicalAddress)
            : source().getPortId(physicalAddress);
    }

    private void handleSetOsdName(HdmiCecMessage cmd) {
        Preconditions.checkState(mProcessedDeviceCount < mDevices.size());

        DeviceInfo current = mDevices.get(mProcessedDeviceCount);
        if (current.mLogicalAddress != cmd.getSource()) {
            Slog.w(TAG, "Unmatched address[expected:" + current.mLogicalAddress + ", actual:" +
                    cmd.getSource());
            return;
        }

        String displayName = "";
        try {
            if (cmd.getOpcode() != Constants.MESSAGE_FEATURE_ABORT) {
                displayName = new String(cmd.getParams(), "US-ASCII");
            }
        } catch (UnsupportedEncodingException e) {
            Slog.w(TAG, "Failed to decode display name: " + cmd.toString());
        }
        current.mDisplayName = displayName;
        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private void handleVendorId(HdmiCecMessage cmd) {
        Preconditions.checkState(mProcessedDeviceCount < mDevices.size());

        DeviceInfo current = mDevices.get(mProcessedDeviceCount);
        if (current.mLogicalAddress != cmd.getSource()) {
            Slog.w(TAG, "Unmatched address[expected:" + current.mLogicalAddress + ", actual:" +
                    cmd.getSource());
            return;
        }

        if (cmd.getOpcode() != Constants.MESSAGE_FEATURE_ABORT) {
            byte[] params = cmd.getParams();
            int vendorId = HdmiUtils.threeBytesToInt(params);
            current.mVendorId = vendorId;
        }

        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private void handleReportPowerStatus(HdmiCecMessage cmd) {
        Preconditions.checkState(mProcessedDeviceCount < mDevices.size());

        DeviceInfo current = mDevices.get(mProcessedDeviceCount);
        if (current.mLogicalAddress != cmd.getSource()) {
            Slog.w(TAG, "Unmatched address[expected:" + current.mLogicalAddress + ", actual:"
                    + cmd.getSource());
            return;
        }

        if (cmd.getOpcode() != Constants.MESSAGE_FEATURE_ABORT) {
            byte[] params = cmd.getParams();
            int powerStatus = params[0] & 0xFF;
            current.mPowerStatus = powerStatus;
        }

        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private void increaseProcessedDeviceCount() {
        mProcessedDeviceCount++;
        mTimeoutRetry = 0;
    }

    private void removeDevice(int index) {
        mDevices.remove(index);
    }

    private void wrapUpAndFinish() {
        Slog.v(TAG, "---------Wrap up Device Discovery:[" + mDevices.size() + "]---------");
        ArrayList<HdmiDeviceInfo> result = new ArrayList<>();
        for (DeviceInfo info : mDevices) {
            HdmiDeviceInfo cecDeviceInfo = info.toHdmiDeviceInfo();
            Slog.v(TAG, " DeviceInfo: " + cecDeviceInfo);
            result.add(cecDeviceInfo);
        }
        Slog.v(TAG, "--------------------------------------------");
        mCallback.onDeviceDiscoveryDone(result);
        finish();
        // Process any commands buffered while device discovery action was in progress.
        if (mIsTvDevice) {
            tv().processAllDelayedMessages();
        }
    }

    private void checkAndProceedStage() {
        if (mDevices.isEmpty()) {
            wrapUpAndFinish();
            return;
        }
        // If finished current stage, move on to next stage.
        if (mProcessedDeviceCount == mDevices.size()) {
            mProcessedDeviceCount = 0;
            switch (mState) {
                case STATE_WAITING_FOR_PHYSICAL_ADDRESS:
                    startOsdNameStage();
                    return;
                case STATE_WAITING_FOR_OSD_NAME:
                    startVendorIdStage();
                    return;
                case STATE_WAITING_FOR_VENDOR_ID:
                    startPowerStatusStage();
                    return;
                case STATE_WAITING_FOR_POWER:
                    wrapUpAndFinish();
                    return;
                default:
                    return;
            }
        } else {
            sendQueryCommand();
        }
    }

    private void sendQueryCommand() {
        int address = mDevices.get(mProcessedDeviceCount).mLogicalAddress;
        switch (mState) {
            case STATE_WAITING_FOR_DEVICES:
                delayActionWithTimePeriod(mDelayPeriod);
                return;
            case STATE_WAITING_FOR_PHYSICAL_ADDRESS:
                queryPhysicalAddress(address);
                return;
            case STATE_WAITING_FOR_OSD_NAME:
                queryOsdName(address);
                return;
            case STATE_WAITING_FOR_VENDOR_ID:
                queryVendorId(address);
                return;
            case STATE_WAITING_FOR_POWER:
                queryPowerStatus(address);
                return;
            default:
                return;
        }
    }

    @Override
    void handleTimerEvent(int state) {
        if (mState == STATE_NONE || mState != state) {
            return;
        }

        if (mState == STATE_WAITING_FOR_DEVICES) {
            startPhysicalAddressStage();
            return;
        }
        if (++mTimeoutRetry < HdmiConfig.TIMEOUT_RETRY) {
            sendQueryCommand();
            return;
        }
        mTimeoutRetry = 0;
        Slog.v(TAG, "Timeout[State=" + mState + ", Processed=" + mProcessedDeviceCount);
        if (mState != STATE_WAITING_FOR_POWER && mState != STATE_WAITING_FOR_OSD_NAME) {
            // We don't need to remove the device info if the power status is unknown.
            // Some device does not have preferred OSD name and does not respond to Give OSD name.
            // Like LG TV. We can give it default device name and not remove it.
            removeDevice(mProcessedDeviceCount);
        } else {
            increaseProcessedDeviceCount();
        }
        checkAndProceedStage();
    }
}
