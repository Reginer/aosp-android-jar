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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.List;

class WearAudioPlaybackCallback extends AudioManager.AudioPlaybackCallback {

    private static final String TAG = "WearAudioPlaybackCallback";

    @VisibleForTesting
    static final String ACTION_OUTPUT_SWITCHER =
            "com.google.android.wearable.action.ACTION_OUTPUT_SWITCHER";

    @VisibleForTesting
    static final List<Integer> EXCLUDED_CONTENT_TYPES =
            List.of(AudioAttributes.CONTENT_TYPE_SONIFICATION);

    @VisibleForTesting static final String EXTRA_MEDIA_APP_PACKAGE_NAME = "package_name";

    private final Context mContext;

    WearAudioPlaybackCallback(Context context) {
        mContext = context;
    }

    @Override
    public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> activeConfigs) {
        Log.v(TAG, "Current active configurations: " + activeConfigs);
        for (AudioPlaybackConfiguration activeConfiguration : activeConfigs) {
            // Invoke Output switcher to allow user to select valid output if Media is playing on an
            // invalid Audio output.
            if (isMediaPlaying(activeConfiguration)
                    && isInvalidAudioDeviceOutput(activeConfiguration.getAudioDeviceInfo())) {
                Log.d(TAG, "Launching Wear Media Output switcher");
                String mediaAppPackageName =
                        mContext.getPackageManager()
                                .getNameForUid(activeConfiguration.getClientUid());
                startWearMediaOutputSwitcher(mediaAppPackageName);
                break;
            }
        }
    }

    private static boolean isInvalidAudioDeviceOutput(AudioDeviceInfo audioDeviceInfo) {
        return audioDeviceInfo != null && audioDeviceInfo.getType() == AudioDeviceInfo.TYPE_BUS;
    }

    private static boolean isMediaPlaying(AudioPlaybackConfiguration configuration) {
        AudioAttributes audioAttributes = configuration.getAudioAttributes();
        return audioAttributes.getUsage() == AudioAttributes.USAGE_MEDIA
                && !EXCLUDED_CONTENT_TYPES.contains(audioAttributes.getContentType())
                && configuration.getPlayerState()
                        == AudioPlaybackConfiguration.PLAYER_STATE_STARTED;
    }

    private void startWearMediaOutputSwitcher(String mediaAppPackageName) {
        Intent intent = new Intent(ACTION_OUTPUT_SWITCHER);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_MEDIA_APP_PACKAGE_NAME, mediaAppPackageName);

        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> resolveInfos =
                packageManager.queryIntentActivities(intent, /* flags= */ 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null || activityInfo.applicationInfo == null) {
                continue;
            }
            ApplicationInfo appInfo = activityInfo.applicationInfo;
            // Start application with required Intent action only if it is a system app.
            if (((ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
                            & appInfo.flags)
                    != 0) {
                ComponentName componentName =
                        new ComponentName(activityInfo.packageName, activityInfo.name);
                intent.setComponent(componentName);
                mContext.startActivity(intent);
            }
        }
    }
}
