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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.media.AudioDeviceInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class WearAudioDeviceCallbackTest {
    private WearAudioDeviceCallback mTestWearAudioDeviceCallback;
    @Mock private AudioApiProvider mAudioApiProvider;
    @Mock AudioDeviceInfo mMicAudioDeviceInfo;
    @Mock AudioDeviceInfo mBtA2dpAudioDeviceInfo;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTestWearAudioDeviceCallback = new WearAudioDeviceCallback(mAudioApiProvider);
        when(mMicAudioDeviceInfo.getType()).thenReturn(AudioDeviceInfo.TYPE_BUILTIN_MIC);
        when(mBtA2dpAudioDeviceInfo.getType()).thenReturn(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
    }

    @Test
    public void onAudioDevicesAdded_callWithValidOutputDevice_shouldInvokeDisableSpeaker() {
        AudioDeviceInfo[] audioDeviceInfos = {mMicAudioDeviceInfo, mBtA2dpAudioDeviceInfo};

        mTestWearAudioDeviceCallback.onAudioDevicesAdded(audioDeviceInfos);

        verify(mAudioApiProvider).disableSpeaker();
    }

    @Test
    public void onAudioDevicesAdded_callWithoutValidOutputDevice_shouldNotInvokeDisableSpeaker() {
        AudioDeviceInfo[] audioDeviceInfos = {mMicAudioDeviceInfo};

        mTestWearAudioDeviceCallback.onAudioDevicesAdded(audioDeviceInfos);

        verify(mAudioApiProvider, never()).disableSpeaker();
    }
}
