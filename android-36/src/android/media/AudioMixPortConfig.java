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

package android.media;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;

/**
 * An AudioMixPortConfig describes a possible configuration of an output or input mixer.
 * It is used to specify a sink or source when creating a connection with
 * AudioManager.connectAudioPatch().
 * An AudioMixPortConfig is obtained from AudioMixPort.buildConfig().
 * @hide
 */

public class AudioMixPortConfig extends AudioPortConfig {

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    AudioMixPortConfig(AudioMixPort mixPort, int samplingRate, int channelMask, int format,
                AudioGainConfig gain) {
        super((AudioPort)mixPort, samplingRate, channelMask, format, gain);
    }

    /**
     * Returns the audio mix port this AudioMixPortConfig is issued from.
     */
    public AudioMixPort port() {
        return (AudioMixPort)mPort;
    }
}

