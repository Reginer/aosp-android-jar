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
 * limitations under the License
 */

package com.android.libraries.tv.tvsystem.media;


import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.content.Context;
import android.media.AudioManager;

/**
 * Provides access to APIs in {@link AudioManager} that are otherwise @hidden.
 *
 * @hide
 */
@SystemApi
public final class TvAudioManager {

    private final AudioManager mAudioManager;

    public TvAudioManager(@NonNull Context context) {
        mAudioManager = context.getSystemService(AudioManager.class);
    }

    /**
     * @hide
     * Sets the volume behavior for an audio output device.
     * @see AudioManager#DEVICE_VOLUME_BEHAVIOR_VARIABLE
     * @see AudioManager#DEVICE_VOLUME_BEHAVIOR_FULL
     * @see AudioManager#DEVICE_VOLUME_BEHAVIOR_FIXED
     * @see AudioManager#DEVICE_VOLUME_BEHAVIOR_ABSOLUTE
     * @see AudioManager#DEVICE_VOLUME_BEHAVIOR_ABSOLUTE_MULTI_MODE
     * @param device the device to be affected
     * @param deviceVolumeBehavior one of the device behaviors
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.MODIFY_AUDIO_ROUTING)
    public void setDeviceVolumeBehavior(@NonNull AudioDeviceAttributes device,
            @AudioManager.DeviceVolumeBehavior int deviceVolumeBehavior) {
        android.media.AudioDeviceAttributes audioDeviceAttributes =
                new android.media.AudioDeviceAttributes(device.getRole(), device.getType(),
                        device.getAddress());
        mAudioManager.setDeviceVolumeBehavior(audioDeviceAttributes, deviceVolumeBehavior);
    }

    /**
     * @hide
     * Returns the volume device behavior for the given audio device
     * @param device the audio device
     * @return the volume behavior for the device
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.MODIFY_AUDIO_ROUTING)
    public @AudioManager.DeviceVolumeBehavior
    int getDeviceVolumeBehavior(@NonNull AudioDeviceAttributes device) {
        android.media.AudioDeviceAttributes audioDeviceAttributes =
                new android.media.AudioDeviceAttributes(device.getRole(), device.getType(),
                        device.getAddress());
        return mAudioManager.getDeviceVolumeBehavior(audioDeviceAttributes);
    }
}
