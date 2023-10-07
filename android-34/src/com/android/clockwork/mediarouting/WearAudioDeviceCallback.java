/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.clockwork.mediarouting;

import static android.media.AudioDeviceInfo.TYPE_BLE_BROADCAST;
import static android.media.AudioDeviceInfo.TYPE_BLE_HEADSET;
import static android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER;
import static android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP;
import static android.media.AudioDeviceInfo.TYPE_HEARING_AID;
import static android.media.AudioDeviceInfo.TYPE_LINE_ANALOG;
import static android.media.AudioDeviceInfo.TYPE_LINE_DIGITAL;
import static android.media.AudioDeviceInfo.TYPE_USB_DEVICE;
import static android.media.AudioDeviceInfo.TYPE_USB_HEADSET;
import static android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES;
import static android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET;

import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;

class WearAudioDeviceCallback extends AudioDeviceCallback {

    private final AudioApiProvider mAudioApiProvider;

    WearAudioDeviceCallback(AudioApiProvider audioApiProvider) {
        mAudioApiProvider = audioApiProvider;
    }

    @Override
    public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
        for (AudioDeviceInfo audioDeviceInfo : addedDevices) {
            if (isValidMediaPlaybackDevice(audioDeviceInfo.getType())) {
                mAudioApiProvider.disableSpeaker();
                break;
            }
        }
    }

    private static boolean isValidMediaPlaybackDevice(int type) {
        switch (type) {
            case TYPE_WIRED_HEADSET:
            case TYPE_WIRED_HEADPHONES:
            case TYPE_BLUETOOTH_A2DP:
            case TYPE_USB_DEVICE:
            case TYPE_USB_HEADSET:
            case TYPE_LINE_ANALOG:
            case TYPE_LINE_DIGITAL:
            case TYPE_HEARING_AID:
            case TYPE_BLE_HEADSET:
            case TYPE_BLE_SPEAKER:
            case TYPE_BLE_BROADCAST:
                return true;
            default:
                return false;
        }
    }
}
