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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.audiopolicy.AudioProductStrategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class AudioApiProviderTest {

    private static final int FAKE_STRATEGY_MEDIA_ID = 20;
    private static final int FAKE_STRATEGY_OTHER_ID = -20;
    private AudioApiProvider mTestAudioApiProvider;
    @Mock private AudioProductStrategy mAudioProductStrategyMedia;
    @Mock private AudioProductStrategy mAudioProductStrategyOther;
    @Mock private AudioSystemWrapper mAudioSystemWrapper;
    @Mock private AudioManagerWrapper mAudioManagerWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mTestAudioApiProvider = new AudioApiProvider(mAudioSystemWrapper, mAudioManagerWrapper);
        when(mAudioProductStrategyMedia.getAudioAttributesForLegacyStreamType(
                        AudioManager.STREAM_MUSIC))
                .thenReturn((new AudioAttributes.Builder()).build());
        when(mAudioProductStrategyOther.getAudioAttributesForLegacyStreamType(
                        AudioManager.STREAM_MUSIC))
                .thenReturn(null);
        when(mAudioProductStrategyMedia.getId()).thenReturn(FAKE_STRATEGY_MEDIA_ID);
        when(mAudioProductStrategyOther.getId()).thenReturn(FAKE_STRATEGY_OTHER_ID);
    }

    @Test
    public void disableSpeaker_shouldSetRoleForWatchSpeakerAsDisabledForMediaStrategy() {
        ArgumentCaptor<List<AudioDeviceAttributes>> deviceAttributesCaptor =
                ArgumentCaptor.forClass(List.class);
        AudioApiProvider mockAudioApiProvider = spy(mTestAudioApiProvider);
        when(mAudioManagerWrapper.getAudioProductStrategies())
                .thenReturn(List.of(mAudioProductStrategyMedia, mAudioProductStrategyOther));

        mockAudioApiProvider.disableSpeaker();

        verify(mAudioSystemWrapper)
                .setDevicesRoleForStrategy(
                        eq(FAKE_STRATEGY_MEDIA_ID),
                        eq(AudioSystem.DEVICE_ROLE_DISABLED),
                        deviceAttributesCaptor.capture());
        assertThat(deviceAttributesCaptor.getValue().size()).isEqualTo(1);
        assertThat(deviceAttributesCaptor.getValue().get(0).getRole())
                .isEqualTo(AudioSystem.DEVICE_ROLE_DISABLED);
        assertThat(deviceAttributesCaptor.getValue().get(0).getType())
                .isEqualTo(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
    }
}
