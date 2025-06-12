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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An audio port is a node of the audio framework or hardware that can be connected to or
 * disconnect from another audio node to create a specific audio routing configuration.
 * Examples of audio ports are an output device (speaker) or an output mix (see AudioMixPort).
 * All attributes that are relevant for applications to make routing selection are described
 * in an AudioPort,  in particular:
 * - possible channel mask configurations.
 * - audio format (PCM 16bit, PCM 24bit...)
 * - gain: a port can be associated with one or more gain controllers (see AudioGain).
 *
 * This object is always created by the framework and read only by applications.
 * A list of all audio port descriptors currently available for applications to control
 * is obtained by AudioManager.listAudioPorts().
 * An application can obtain an AudioPortConfig for a valid configuration of this port
 * by calling AudioPort.buildConfig() and use this configuration
 * to create a connection between audio sinks and sources with AudioManager.connectAudioPatch()
 *
 * @hide
 */
public class AudioPort {
    private static final String TAG = "AudioPort";

    /**
     * For use by the audio framework.
     */
    public static final int ROLE_NONE = 0;
    /**
     * The audio port is a source (produces audio)
     */
    public static final int ROLE_SOURCE = 1;
    /**
     * The audio port is a sink (consumes audio)
     */
    public static final int ROLE_SINK = 2;

    /**
     * audio port type for use by audio framework implementation
     */
    public static final int TYPE_NONE = 0;
    /**
     */
    public static final int TYPE_DEVICE = 1;
    /**
     */
    public static final int TYPE_SUBMIX = 2;
    /**
     */
    public static final int TYPE_SESSION = 3;


    @UnsupportedAppUsage
    AudioHandle mHandle;
    @UnsupportedAppUsage
    protected final int mRole;
    private final String mName;
    private final int[] mSamplingRates;
    private final int[] mChannelMasks;
    private final int[] mChannelIndexMasks;
    private final int[] mFormats;
    private final List<AudioProfile> mProfiles;
    private final List<AudioDescriptor> mDescriptors;
    @UnsupportedAppUsage
    private final AudioGain[] mGains;
    @UnsupportedAppUsage
    private AudioPortConfig mActiveConfig;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    AudioPort(AudioHandle handle, int role, String name,
            int[] samplingRates, int[] channelMasks, int[] channelIndexMasks,
            int[] formats, AudioGain[] gains) {
        mHandle = handle;
        mRole = role;
        mName = name;
        mSamplingRates = samplingRates;
        mChannelMasks = channelMasks;
        mChannelIndexMasks = channelIndexMasks;
        mFormats = formats;
        mGains = gains;
        mProfiles = new ArrayList<>();
        if (mFormats != null) {
            for (int format : mFormats) {
                mProfiles.add(new AudioProfile(
                        format, samplingRates, channelMasks, channelIndexMasks,
                        AudioProfile.AUDIO_ENCAPSULATION_TYPE_NONE));
            }
        }
        mDescriptors = new ArrayList<>();
    }

    AudioPort(AudioHandle handle, int role, String name,
              List<AudioProfile> profiles, AudioGain[] gains,
              List<AudioDescriptor> descriptors) {
        mHandle = handle;
        mRole = role;
        mName = name;
        mProfiles = profiles;
        mDescriptors = descriptors;
        mGains = gains;
        Set<Integer> formats = new HashSet<>();
        Set<Integer> samplingRates = new HashSet<>();
        Set<Integer> channelMasks = new HashSet<>();
        Set<Integer> channelIndexMasks = new HashSet<>();
        for (AudioProfile profile : profiles) {
            formats.add(profile.getFormat());
            samplingRates.addAll(Arrays.stream(profile.getSampleRates()).boxed()
                    .collect(Collectors.toList()));
            channelMasks.addAll(Arrays.stream(profile.getChannelMasks()).boxed()
                    .collect(Collectors.toList()));
            channelIndexMasks.addAll(Arrays.stream(profile.getChannelIndexMasks()).boxed()
                    .collect(Collectors.toList()));
        }
        mSamplingRates = samplingRates.stream().mapToInt(Number::intValue).toArray();
        mChannelMasks = channelMasks.stream().mapToInt(Number::intValue).toArray();
        mChannelIndexMasks = channelIndexMasks.stream().mapToInt(Number::intValue).toArray();
        mFormats = formats.stream().mapToInt(Number::intValue).toArray();
    }

    AudioHandle handle() {
        return mHandle;
    }

    /**
     * Get the system unique device ID.
     */
    @UnsupportedAppUsage
    public int id() {
        return mHandle.id();
    }


    /**
     * Get the audio port role
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int role() {
        return mRole;
    }

    /**
     * Get the human-readable name of this port. Perhaps an internal
     * designation or an physical device.
     */
    public String name() {
        return mName;
    }

    /**
     * Get the list of supported sampling rates
     * Empty array if sampling rate is not relevant for this audio port
     */
    public int[] samplingRates() {
        return mSamplingRates;
    }

    /**
     * Get the list of supported channel mask configurations
     * (e.g AudioFormat.CHANNEL_OUT_STEREO)
     * Empty array if channel mask is not relevant for this audio port
     */
    public int[] channelMasks() {
        return mChannelMasks;
    }

    /**
     * Get the list of supported channel index mask configurations
     * (e.g 0x0003 means 2 channel, 0x000F means 4 channel....)
     * Empty array if channel index mask is not relevant for this audio port
     */
    public int[] channelIndexMasks() {
        return mChannelIndexMasks;
    }

    /**
     * Get the list of supported audio format configurations
     * (e.g AudioFormat.ENCODING_PCM_16BIT)
     * Empty array if format is not relevant for this audio port
     */
    public int[] formats() {
        return mFormats;
    }

    /**
     * Get the list of supported audio profiles
     */
    public List<AudioProfile> profiles() {
        return mProfiles;
    }

    /**
     * Get the list of audio descriptor
     */
    public List<AudioDescriptor> audioDescriptors() {
        return mDescriptors;
    }

    /**
     * Get the list of gain descriptors
     * Empty array if this port does not have gain control
     */
    public AudioGain[] gains() {
        return mGains;
    }

    /**
     * Get the gain descriptor at a given index
     */
    AudioGain gain(int index) {
        if (index < 0 || index >= mGains.length) {
            return null;
        }
        return mGains[index];
    }

    /**
     * Build a specific configuration of this audio port for use by methods
     * like AudioManager.connectAudioPatch().
     * @param samplingRate
     * @param channelMask The desired channel mask. AudioFormat.CHANNEL_OUT_DEFAULT if no change
     * from active configuration requested.
     * @param format The desired audio format. AudioFormat.ENCODING_DEFAULT if no change
     * from active configuration requested.
     * @param gain The desired gain. null if no gain changed requested.
     */
    public AudioPortConfig buildConfig(int samplingRate, int channelMask, int format,
                                        AudioGainConfig gain) {
        return new AudioPortConfig(this, samplingRate, channelMask, format, gain);
    }

    /**
     * Get currently active configuration of this audio port.
     */
    public AudioPortConfig activeConfig() {
        return mActiveConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AudioPort)) {
            return false;
        }
        AudioPort ap = (AudioPort)o;
        return mHandle.equals(ap.handle());
    }

    @Override
    public int hashCode() {
        return mHandle.hashCode();
    }

    @Override
    public String toString() {
        String role = Integer.toString(mRole);
        switch (mRole) {
            case ROLE_NONE:
                role = "NONE";
                break;
            case ROLE_SOURCE:
                role = "SOURCE";
                break;
            case ROLE_SINK:
                role = "SINK";
                break;
        }
        return "{mHandle: " + mHandle
                + ", mRole: " + role
                + "}";
    }
}
