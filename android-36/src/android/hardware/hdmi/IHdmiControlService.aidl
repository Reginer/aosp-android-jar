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

package android.hardware.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiPortInfo;
import android.hardware.hdmi.IHdmiCecSettingChangeListener;
import android.hardware.hdmi.IHdmiCecVolumeControlFeatureListener;
import android.hardware.hdmi.IHdmiControlCallback;
import android.hardware.hdmi.IHdmiControlStatusChangeListener;
import android.hardware.hdmi.IHdmiDeviceEventListener;
import android.hardware.hdmi.IHdmiHotplugEventListener;
import android.hardware.hdmi.IHdmiInputChangeListener;
import android.hardware.hdmi.IHdmiMhlVendorCommandListener;
import android.hardware.hdmi.IHdmiRecordListener;
import android.hardware.hdmi.IHdmiSystemAudioModeChangeListener;
import android.hardware.hdmi.IHdmiVendorCommandListener;

import java.util.List;

/**
 * Binder interface that clients running in the application process
 * will use to perform HDMI-CEC features by communicating with other devices
 * on the bus.
 *
 * @hide
 */
interface IHdmiControlService {
    int[] getSupportedTypes();
    HdmiDeviceInfo getActiveSource();
    void oneTouchPlay(IHdmiControlCallback callback);
    void toggleAndFollowTvPower();
    boolean shouldHandleTvPowerKey();
    void queryDisplayStatus(IHdmiControlCallback callback);
    void addHdmiControlStatusChangeListener(IHdmiControlStatusChangeListener listener);
    void removeHdmiControlStatusChangeListener(IHdmiControlStatusChangeListener listener);
    void addHdmiCecVolumeControlFeatureListener(IHdmiCecVolumeControlFeatureListener listener);
    void removeHdmiCecVolumeControlFeatureListener(IHdmiCecVolumeControlFeatureListener listener);
    void addHotplugEventListener(IHdmiHotplugEventListener listener);
    void removeHotplugEventListener(IHdmiHotplugEventListener listener);
    void addDeviceEventListener(IHdmiDeviceEventListener listener);
    void deviceSelect(int deviceId, IHdmiControlCallback callback);
    void portSelect(int portId, IHdmiControlCallback callback);
    void sendKeyEvent(int deviceType, int keyCode, boolean isPressed);
    void sendVolumeKeyEvent(int deviceType, int keyCode, boolean isPressed);
    List<HdmiPortInfo> getPortInfo();
    boolean canChangeSystemAudioMode();
    boolean getSystemAudioMode();
    int getPhysicalAddress();
    void setSystemAudioMode(boolean enabled, IHdmiControlCallback callback);
    void addSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener listener);
    void removeSystemAudioModeChangeListener(IHdmiSystemAudioModeChangeListener listener);
    void setArcMode(boolean enabled);
    void setProhibitMode(boolean enabled);
    void setSystemAudioVolume(int oldIndex, int newIndex, int maxIndex);
    void setSystemAudioMute(boolean mute);
    void setInputChangeListener(IHdmiInputChangeListener listener);
    List<HdmiDeviceInfo> getInputDevices();
    List<HdmiDeviceInfo> getDeviceList();
    void powerOffRemoteDevice(int logicalAddress, int powerStatus);
    void powerOnRemoteDevice(int logicalAddress, int powerStatus);
    void askRemoteDeviceToBecomeActiveSource(int physicalAddress);
    void sendVendorCommand(int deviceType, int targetAddress, in byte[] params,
            boolean hasVendorId);
    void addVendorCommandListener(IHdmiVendorCommandListener listener, int vendorId);
    void sendStandby(int deviceType, int deviceId);
    void setHdmiRecordListener(IHdmiRecordListener callback);
    void startOneTouchRecord(int recorderAddress, in byte[] recordSource);
    void stopOneTouchRecord(int recorderAddress);
    void startTimerRecording(int recorderAddress, int sourceType, in byte[] recordSource);
    void clearTimerRecording(int recorderAddress, int sourceType, in byte[] recordSource);
    void sendMhlVendorCommand(int portId, int offset, int length, in byte[] data);
    void addHdmiMhlVendorCommandListener(IHdmiMhlVendorCommandListener listener);
    void setStandbyMode(boolean isStandbyModeOn);
    void reportAudioStatus(int deviceType, int volume, int maxVolume, boolean isMute);
    void setSystemAudioModeOnForAudioOnlySource();
    boolean setMessageHistorySize(int newSize);
    int getMessageHistorySize();
    void addCecSettingChangeListener(String name, IHdmiCecSettingChangeListener listener);
    void removeCecSettingChangeListener(String name, IHdmiCecSettingChangeListener listener);
    List<String> getUserCecSettings();
    List<String> getAllowedCecSettingStringValues(String name);
    int[] getAllowedCecSettingIntValues(String name);
    String getCecSettingStringValue(String name);
    void setCecSettingStringValue(String name, String value);
    int getCecSettingIntValue(String name);
    void setCecSettingIntValue(String name, int value);
}
