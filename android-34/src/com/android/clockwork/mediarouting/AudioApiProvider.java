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

import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.audiopolicy.AudioProductStrategy;

import com.android.internal.annotations.VisibleForTesting;

import java.util.List;

/** A wrapper for {@link AudioSystem} class to ease unit testing of the static methods used. */
class AudioApiProvider {

    private static final AudioApiProvider sInstance =
            new AudioApiProvider(new AudioSystemWrapper(), new AudioManagerWrapper());
    private final AudioSystemWrapper mAudioSystemWrapper;
    private final AudioManagerWrapper mAudioManagerWrapper;

    @VisibleForTesting
    AudioApiProvider(
            AudioSystemWrapper audioSystemWrapper, AudioManagerWrapper audioManagerWrapper) {
        mAudioSystemWrapper = audioSystemWrapper;
        mAudioManagerWrapper = audioManagerWrapper;
    }

    static AudioApiProvider getInstance() {
        return sInstance;
    }

    /**
     * Calls the Audio platform APIs to disable the speaker.
     *
     * @return {@link AudioSystem#SUCCESS} if the speaker is disabled successfully, otherwise
     *     returns {@link AudioSystem#ERROR}.
     */
    int disableSpeaker() {
        AudioProductStrategy mediaStrategy = getAudioStrategyForMedia();
        if (mediaStrategy != null) {
            List<AudioDeviceAttributes> deviceAttributes =
                    List.of(
                            new AudioDeviceAttributes(
                                    AudioDeviceAttributes.ROLE_OUTPUT,
                                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                                    /* address= */ ""));

            return mAudioSystemWrapper.setDevicesRoleForStrategy(
                    mediaStrategy.getId(), AudioSystem.DEVICE_ROLE_DISABLED, deviceAttributes);
        }
        return AudioSystem.ERROR;
    }

    /**
     * Returns the {@link AudioProductStrategy} linked to media if it was found, otherwise returns
     * null.
     */
    private AudioProductStrategy getAudioStrategyForMedia() {
        for (AudioProductStrategy audioProductStrategy :
                mAudioManagerWrapper.getAudioProductStrategies()) {
            if (audioProductStrategy.getAudioAttributesForLegacyStreamType(
                            AudioManager.STREAM_MUSIC)
                    != null) {
                return audioProductStrategy;
            }
        }
        return null;
    }
}
