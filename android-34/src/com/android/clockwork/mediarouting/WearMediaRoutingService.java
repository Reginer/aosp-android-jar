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

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.SystemService;


/** Service handling automatic invocation of output switcher. */
public class WearMediaRoutingService extends SystemService {

    private static final String TAG = "WearMediaRoutingService";
    // TODO(b/263136377): replace this with feature flag
    @VisibleForTesting static boolean sIsAutoLaunchOutputSwitcherEnabled = false;
    private final WearAudioPlaybackCallback mWearAudioPlaybackCallback;
    private final WearAudioDeviceCallback mWearAudioDeviceCallback;
    private final WearAudioServerStateCallback mWearAudioServerStateCallback;
    private final AudioManager mAudioManager;
    private final AudioApiProvider mAudioApiProvider;

    @VisibleForTesting
    WearMediaRoutingService(
            Context context,
            WearAudioPlaybackCallback wearAudioPlaybackCallback,
            WearAudioDeviceCallback wearAudioDeviceCallback,
            WearAudioServerStateCallback wearAudioServerStateCallback,
            AudioManager audioManager,
            AudioApiProvider audioApiProvider) {
        super(context);
        mWearAudioPlaybackCallback = wearAudioPlaybackCallback;
        mAudioManager = audioManager;
        mAudioApiProvider = audioApiProvider;
        mWearAudioDeviceCallback = wearAudioDeviceCallback;
        mWearAudioServerStateCallback = wearAudioServerStateCallback;
    }

    public WearMediaRoutingService(Context context) {
        this(
                context,
                new WearAudioPlaybackCallback(context),
                new WearAudioDeviceCallback(AudioApiProvider.getInstance()),
                new WearAudioServerStateCallback(AudioApiProvider.getInstance()),
                context.getSystemService(AudioManager.class),
                AudioApiProvider.getInstance());
    }

    @Override
    public void onStart() {}

    @Override
    public void onBootPhase(int phase) {
        if (phase != SystemService.PHASE_BOOT_COMPLETED) {
            return;
        }

        if (sIsAutoLaunchOutputSwitcherEnabled) {
            Log.d(TAG, "Auto launch of Media Output Switcher is enabled");
            mAudioApiProvider.disableSpeaker();
            mAudioManager.registerAudioPlaybackCallback(
                    mWearAudioPlaybackCallback, /* handler= */ null);
            mAudioManager.registerAudioDeviceCallback(
                    mWearAudioDeviceCallback, /* handler= */ null);
            mAudioManager.setAudioServerStateCallback(
                    getContext().getMainExecutor(), mWearAudioServerStateCallback);
        } else {
            Log.w(TAG, "Auto launch of Media Output Switcher not enabled");
        }
    }
}
