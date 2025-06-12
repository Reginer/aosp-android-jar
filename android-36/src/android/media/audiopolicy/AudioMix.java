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

package android.media.audiopolicy;

import static android.media.AudioSystem.getDeviceName;
import static android.media.AudioSystem.isRemoteSubmixDevice;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioSystem;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * @hide
 */
@SystemApi
public class AudioMix implements Parcelable {

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private @NonNull AudioMixingRule mRule;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private @NonNull AudioFormat mFormat;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mRouteFlags;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mMixType = MIX_TYPE_INVALID;

    private final IBinder mToken;

    // written by AudioPolicy
    int mMixState = MIX_STATE_DISABLED;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int mCallbackFlags;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @NonNull String mDeviceAddress;

    // initialized in constructor, read by AudioPolicyConfig
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    final int mDeviceSystemType; // an AudioSystem.DEVICE_* value, not AudioDeviceInfo.TYPE_*

    // The (virtual) device ID that this AudioMix was registered for. This value is overwritten
    // when registering this AudioMix with an AudioPolicy or attaching this AudioMix to an
    // AudioPolicy to match the AudioPolicy attribution. Does not imply that it only modifies
    // audio routing for this device ID.
    private int mVirtualDeviceId;

    /**
     * All parameters are guaranteed valid through the Builder.
     */
    private AudioMix(@NonNull AudioMixingRule rule, @NonNull AudioFormat format,
            int routeFlags, int callbackFlags,
            int deviceType, @Nullable String deviceAddress, IBinder token,
            int virtualDeviceId) {
        mRule = Objects.requireNonNull(rule);
        mFormat = Objects.requireNonNull(format);
        mRouteFlags = routeFlags;
        mMixType = rule.getTargetMixType();
        mCallbackFlags = callbackFlags;
        mDeviceSystemType = deviceType;
        mDeviceAddress = (deviceAddress == null) ? new String("") : deviceAddress;
        mToken = token;
        mVirtualDeviceId = virtualDeviceId;
    }

    // CALLBACK_FLAG_* values: keep in sync with AudioMix::kCbFlag* values defined
    // in frameworks/av/include/media/AudioPolicy.h
    /** @hide */
    public final static int CALLBACK_FLAG_NOTIFY_ACTIVITY = 0x1;
    // when adding new MIX_FLAG_* flags, add them to this mask of authorized masks:
    private final static int CALLBACK_FLAGS_ALL = CALLBACK_FLAG_NOTIFY_ACTIVITY;

    // ROUTE_FLAG_* values: keep in sync with MIX_ROUTE_FLAG_* values defined
    // in frameworks/av/include/media/AudioPolicy.h
    /**
     * An audio mix behavior where the output of the mix is sent to the original destination of
     * the audio signal, i.e. an output device for an output mix, or a recording for an input mix.
     */
    public static final int ROUTE_FLAG_RENDER    = 0x1;
    /**
     * An audio mix behavior where the output of the mix is rerouted back to the framework and
     * is accessible for injection or capture through the {@link AudioTrack} and {@link AudioRecord}
     * APIs.
     */
    public static final int ROUTE_FLAG_LOOP_BACK = 0x1 << 1;

    /**
     * An audio mix behavior where the targeted audio is played unaffected but a copy is
     * accessible for capture through {@link AudioRecord}.
     *
     * Only capture of playback is supported, not capture of capture.
     * Use concurrent capture instead to capture what is captured by other apps.
     *
     * The captured audio is an approximation of the played audio.
     * Effects and volume are not applied, and track are mixed with different delay then in the HAL.
     * As a result, this API is not suitable for echo cancelling.
     * @hide
     */
    public static final int ROUTE_FLAG_LOOP_BACK_RENDER = ROUTE_FLAG_LOOP_BACK | ROUTE_FLAG_RENDER;

    private static final int ROUTE_FLAG_SUPPORTED = ROUTE_FLAG_RENDER | ROUTE_FLAG_LOOP_BACK;

    // MIX_TYPE_* values to keep in sync with frameworks/av/include/media/AudioPolicy.h
    /**
     * @hide
     * Invalid mix type, default value.
     */
    public static final int MIX_TYPE_INVALID = -1;
    /**
     * @hide
     * Mix type indicating playback streams are mixed.
     */
    public static final int MIX_TYPE_PLAYERS = 0;
    /**
     * @hide
     * Mix type indicating recording streams are mixed.
     */
    public static final int MIX_TYPE_RECORDERS = 1;


    // MIX_STATE_* values to keep in sync with frameworks/av/include/media/AudioPolicy.h
    /**
     * State of a mix before its policy is enabled.
     */
    public static final int MIX_STATE_DISABLED = -1;
    /**
     * State of a mix when there is no audio to mix.
     */
    public static final int MIX_STATE_IDLE = 0;
    /**
     * State of a mix that is actively mixing audio.
     */
    public static final int MIX_STATE_MIXING = 1;

    /** Maximum sampling rate for privileged playback capture*/
    private static final int PRIVILEDGED_CAPTURE_MAX_SAMPLE_RATE = 16000;

    /** Maximum channel number for privileged playback capture*/
    private static final int PRIVILEDGED_CAPTURE_MAX_CHANNEL_NUMBER = 1;

    /** Maximum channel number for privileged playback capture*/
    private static final int PRIVILEDGED_CAPTURE_MAX_BYTES_PER_SAMPLE = 2;

    /**
     * The current mixing state.
     * @return one of {@link #MIX_STATE_DISABLED}, {@link #MIX_STATE_IDLE},
     *          {@link #MIX_STATE_MIXING}.
     */
    public int getMixState() {
        return mMixState;
    }


    /** @hide */
    public int getRouteFlags() {
        return mRouteFlags;
    }

    /** @hide */
    public AudioFormat getFormat() {
        return mFormat;
    }

    /** @hide */
    public AudioMixingRule getRule() {
        return mRule;
    }

    /** @hide */
    public int getMixType() {
        return mMixType;
    }

    void setRegistration(String regId) {
        mDeviceAddress = regId;
    }

    /** @hide */
    public void setAudioMixingRule(@NonNull AudioMixingRule rule) {
        if (mRule.getTargetMixType() != rule.getTargetMixType()) {
            throw new UnsupportedOperationException(
                    "Target mix role of updated rule doesn't match the mix role of the AudioMix");
        }
        mRule = Objects.requireNonNull(rule);
    }

    /** @hide */
    public String getRegistration() {
        return mDeviceAddress;
    }

    /** @hide */
    public boolean isAffectingUsage(int usage) {
        return mRule.isAffectingUsage(usage);
    }

    /**
      * Returns {@code true} if the rule associated with this mix contains a
      * RULE_MATCH_ATTRIBUTE_USAGE criterion for the given usage
      *
      * @hide
      */
    public boolean containsMatchAttributeRuleForUsage(int usage) {
        return mRule.containsMatchAttributeRuleForUsage(usage);
    }

    /** @hide */
    public boolean isRoutedToDevice(int deviceType, @NonNull String deviceAddress) {
        if ((mRouteFlags & ROUTE_FLAG_RENDER) != ROUTE_FLAG_RENDER) {
            return false;
        }
        if (deviceType != mDeviceSystemType) {
            return false;
        }
        if (!deviceAddress.equals(mDeviceAddress)) {
            return false;
        }
        return true;
    }

    /** @return an error string if the format would not allow Privileged playbackCapture
     *          null otherwise
     * @hide */
    public static String canBeUsedForPrivilegedMediaCapture(AudioFormat format) {
        int sampleRate = format.getSampleRate();
        if (sampleRate > PRIVILEDGED_CAPTURE_MAX_SAMPLE_RATE || sampleRate <= 0) {
            return "Privileged audio capture sample rate " + sampleRate
                   + " can not be over " + PRIVILEDGED_CAPTURE_MAX_SAMPLE_RATE + "kHz";
        }
        int channelCount = format.getChannelCount();
        if (channelCount > PRIVILEDGED_CAPTURE_MAX_CHANNEL_NUMBER || channelCount <= 0) {
            return "Privileged audio capture channel count " + channelCount + " can not be over "
                   + PRIVILEDGED_CAPTURE_MAX_CHANNEL_NUMBER;
        }
        int encoding = format.getEncoding();
        if (!format.isPublicEncoding(encoding) || !format.isEncodingLinearPcm(encoding)) {
            return "Privileged audio capture encoding " + encoding + "is not linear";
        }
        if (format.getBytesPerSample(encoding) > PRIVILEDGED_CAPTURE_MAX_BYTES_PER_SAMPLE) {
            return "Privileged audio capture encoding " + encoding + " can not be over "
                   + PRIVILEDGED_CAPTURE_MAX_BYTES_PER_SAMPLE + " bytes per sample";
        }
        return null;
    }

    /** @hide */
    public boolean isForCallRedirection() {
        return mRule.isForCallRedirection();
    }

    /** @hide */
    public boolean matchesVirtualDeviceId(int deviceId) {
        return mVirtualDeviceId == deviceId;
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AudioMix that = (AudioMix) o;
        boolean tokenMatch = android.media.audiopolicy.Flags.audioMixOwnership()
                ? Objects.equals(this.mToken, that.mToken)
                : true;
        return Objects.equals(this.mRouteFlags, that.mRouteFlags)
            && Objects.equals(this.mRule, that.mRule)
            && Objects.equals(this.mMixType, that.mMixType)
            && Objects.equals(this.mFormat, that.mFormat)
            && tokenMatch;
    }

    /** @hide */
    @Override
    public int hashCode() {
        if (android.media.audiopolicy.Flags.audioMixOwnership()) {
            return Objects.hash(mRouteFlags, mRule, mMixType, mFormat, mToken);
        }
        return Objects.hash(mRouteFlags, mRule, mMixType, mFormat);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // write mix route flags
        dest.writeInt(mRouteFlags);
        // write callback flags
        dest.writeInt(mCallbackFlags);
        // write device information
        dest.writeInt(mDeviceSystemType);
        dest.writeString8(mDeviceAddress);
        mFormat.writeToParcel(dest, flags);
        mRule.writeToParcel(dest, flags);
        dest.writeStrongBinder(mToken);
        dest.writeInt(mVirtualDeviceId);
    }

    public static final @NonNull Parcelable.Creator<AudioMix> CREATOR = new Parcelable.Creator<>() {
        /**
         * Rebuilds an AudioMix previously stored with writeToParcel().
         *
         * @param p Parcel object to read the AudioMix from
         * @return a new AudioMix created from the data in the parcel
         */
        public AudioMix createFromParcel(Parcel p) {
            final AudioMix.Builder mixBuilder = new AudioMix.Builder();
            // read mix route flags
            mixBuilder.setRouteFlags(p.readInt());
            // read callback flags
            mixBuilder.setCallbackFlags(p.readInt());
            // read device information
            mixBuilder.setDevice(p.readInt(), p.readString8());
            mixBuilder.setFormat(AudioFormat.CREATOR.createFromParcel(p));
            mixBuilder.setMixingRule(AudioMixingRule.CREATOR.createFromParcel(p));
            mixBuilder.setToken(p.readStrongBinder());
            mixBuilder.setVirtualDeviceId(p.readInt());
            return mixBuilder.build();
        }

        public AudioMix[] newArray(int size) {
            return new AudioMix[size];
        }
    };

    /**
     * Updates the deviceId of the AudioMix to match with the AudioPolicy the mix is registered
     * through.
     * @hide
     */
    public void setVirtualDeviceId(int virtualDeviceId) {
        mVirtualDeviceId = virtualDeviceId;
    }

    /** @hide */
    @IntDef(flag = true,
            value = { ROUTE_FLAG_RENDER, ROUTE_FLAG_LOOP_BACK } )
    @Retention(RetentionPolicy.SOURCE)
    public @interface RouteFlags {}

    /**
     * Builder class for {@link AudioMix} objects
     */
    public static class Builder {
        private AudioMixingRule mRule = null;
        private AudioFormat mFormat = null;
        private int mRouteFlags = 0;
        private int mCallbackFlags = 0;
        private IBinder mToken = null;
        private int mVirtualDeviceId = Context.DEVICE_ID_DEFAULT;
        // an AudioSystem.DEVICE_* value, not AudioDeviceInfo.TYPE_*
        private int mDeviceSystemType = AudioSystem.DEVICE_NONE;
        private String mDeviceAddress = null;

        /**
         * @hide
         * Only used by AudioPolicyConfig, not a public API.
         */
        Builder() { }

        /**
         * Construct an instance for the given {@link AudioMixingRule}.
         * @param rule a non-null {@link AudioMixingRule} instance.
         * @throws IllegalArgumentException
         */
        public Builder(@NonNull AudioMixingRule rule)
                throws IllegalArgumentException {
            if (rule == null) {
                throw new IllegalArgumentException("Illegal null AudioMixingRule argument");
            }
            mRule = rule;
        }

        /**
         * @hide
         * Only used by AudioPolicyConfig, not a public API.
         * @param rule
         * @return the same Builder instance.
         * @throws IllegalArgumentException
         */
        Builder setMixingRule(@NonNull AudioMixingRule rule)
                throws IllegalArgumentException {
            if (rule == null) {
                throw new IllegalArgumentException("Illegal null AudioMixingRule argument");
            }
            mRule = rule;
            return this;
        }

        /**
         * @hide
         * Only used by AudioMix internally.
         */
        Builder setToken(IBinder token) {
            mToken = token;
            return this;
        }

        /**
         * @hide
         * Only used by AudioMix internally.
         */
        Builder setVirtualDeviceId(int virtualDeviceId) {
            mVirtualDeviceId = virtualDeviceId;
            return this;
        }

        /**
         * @hide
         * Only used by AudioPolicyConfig, not a public API.
         * @param callbackFlags which callbacks are called from native
         * @return the same Builder instance.
         * @throws IllegalArgumentException
         */
        Builder setCallbackFlags(int flags) throws IllegalArgumentException {
            if ((flags != 0) && ((flags & CALLBACK_FLAGS_ALL) == 0)) {
                throw new IllegalArgumentException("Illegal callback flags 0x"
                        + Integer.toHexString(flags).toUpperCase());
            }
            mCallbackFlags = flags;
            return this;
        }

        /**
         * @hide
         * Only used by AudioPolicyConfig, not a public API.
         * @param deviceType an AudioSystem.DEVICE_* value, not AudioDeviceInfo.TYPE_*
         * @param address
         * @return the same Builder instance.
         */
        @VisibleForTesting
        public Builder setDevice(int deviceType, String address) {
            mDeviceSystemType = deviceType;
            mDeviceAddress = address;
            return this;
        }

        /**
         * Sets the {@link AudioFormat} for the mix.
         * @param format a non-null {@link AudioFormat} instance.
         * @return the same Builder instance.
         * @throws IllegalArgumentException
         */
        public Builder setFormat(@NonNull AudioFormat format)
                throws IllegalArgumentException {
            if (format == null) {
                throw new IllegalArgumentException("Illegal null AudioFormat argument");
            }
            mFormat = format;
            return this;
        }

        /**
         * Sets the routing behavior for the mix. If not set, routing behavior will default to
         * {@link AudioMix#ROUTE_FLAG_LOOP_BACK}.
         * @param routeFlags one of {@link AudioMix#ROUTE_FLAG_LOOP_BACK},
         *     {@link AudioMix#ROUTE_FLAG_RENDER}
         * @return the same Builder instance.
         * @throws IllegalArgumentException
         */
        public Builder setRouteFlags(@RouteFlags int routeFlags)
                throws IllegalArgumentException {
            if (routeFlags == 0) {
                throw new IllegalArgumentException("Illegal empty route flags");
            }
            if ((routeFlags & ROUTE_FLAG_SUPPORTED) == 0) {
                throw new IllegalArgumentException("Invalid route flags 0x"
                        + Integer.toHexString(routeFlags) + "when configuring an AudioMix");
            }
            if ((routeFlags & ~ROUTE_FLAG_SUPPORTED) != 0) {
                throw new IllegalArgumentException("Unknown route flags 0x"
                        + Integer.toHexString(routeFlags) + "when configuring an AudioMix");
            }
            mRouteFlags = routeFlags;
            return this;
        }

        /**
         * Sets the audio device used for playback. Cannot be used in the context of an audio
         * policy used to inject audio to be recorded, or in a mix whose route flags doesn't
         * specify {@link AudioMix#ROUTE_FLAG_RENDER}.
         * @param device a non-null AudioDeviceInfo describing the audio device to play the output
         *     of this mix.
         * @return the same Builder instance
         * @throws IllegalArgumentException
         */
        public Builder setDevice(@NonNull AudioDeviceInfo device) throws IllegalArgumentException {
            if (device == null) {
                throw new IllegalArgumentException("Illegal null AudioDeviceInfo argument");
            }
            if (!device.isSink()) {
                throw new IllegalArgumentException("Unsupported device type on mix, not a sink");
            }
            mDeviceSystemType = AudioDeviceInfo.convertDeviceTypeToInternalDevice(device.getType());
            mDeviceAddress = device.getAddress();
            return this;
        }

        /**
         * Combines all of the settings and return a new {@link AudioMix} object.
         * @return a new {@link AudioMix} object
         * @throws IllegalArgumentException if no {@link AudioMixingRule} has been set.
         */
        public AudioMix build() throws IllegalArgumentException {
            if (mRule == null) {
                throw new IllegalArgumentException("Illegal null AudioMixingRule");
            }
            if (mRouteFlags == 0) {
                // no route flags set, use default as described in Builder.setRouteFlags(int)
                mRouteFlags = ROUTE_FLAG_LOOP_BACK;
            }
            if (mFormat == null) {
                // FIXME Can we eliminate this?  Will AudioMix work with an unspecified sample rate?
                int rate = AudioSystem.getPrimaryOutputSamplingRate();
                if (rate <= 0) {
                    rate = 44100;
                }
                mFormat = new AudioFormat.Builder().setSampleRate(rate).build();
            } else {
                // Ensure that 'mFormat' uses an output channel mask. Using an input channel
                // mask was not made 'illegal' initially, however the framework code
                // assumes usage in AudioMixes of output channel masks only (b/194910301).
                if ((mFormat.getPropertySetMask()
                                & AudioFormat.AUDIO_FORMAT_HAS_PROPERTY_CHANNEL_MASK) != 0) {
                    if (mFormat.getChannelCount() == 1
                            && mFormat.getChannelMask() == AudioFormat.CHANNEL_IN_MONO) {
                        mFormat = new AudioFormat.Builder(mFormat).setChannelMask(
                                AudioFormat.CHANNEL_OUT_MONO).build();
                    }
                    // CHANNEL_IN_STEREO == CHANNEL_OUT_STEREO so no need to correct.
                    // CHANNEL_IN_FRONT_BACK is hidden, should not appear.
                }
            }

            if ((mRouteFlags & ROUTE_FLAG_LOOP_BACK) == ROUTE_FLAG_LOOP_BACK) {
                if (mDeviceSystemType == AudioSystem.DEVICE_NONE) {
                    // If there was no device type explicitly set, configure it based on mix type.
                    mDeviceSystemType = getLoopbackDeviceSystemTypeForAudioMixingRule(mRule);
                } else if (!isRemoteSubmixDevice(mDeviceSystemType)) {
                    // Loopback mode only supports remote submix devices.
                    throw new IllegalArgumentException("Device " + getDeviceName(mDeviceSystemType)
                            + "is not supported for loopback mix.");
                }
            }

            if ((mRouteFlags & ROUTE_FLAG_RENDER) == ROUTE_FLAG_RENDER) {
                if (mDeviceSystemType == AudioSystem.DEVICE_NONE) {
                    throw new IllegalArgumentException(
                            "Can't have flag ROUTE_FLAG_RENDER without an audio device");
                }

                if (AudioSystem.DEVICE_IN_ALL_SET.contains(mDeviceSystemType)) {
                    throw new IllegalArgumentException(
                            "Input device is not supported with ROUTE_FLAG_RENDER");
                }

                if (mRule.getTargetMixType() == MIX_TYPE_RECORDERS) {
                    throw new IllegalArgumentException(
                            "ROUTE_FLAG_RENDER/ROUTE_FLAG_LOOP_BACK_RENDER is not supported for "
                                    + "non-playback mix rule");
                }
            }

            if (mRule.allowPrivilegedMediaPlaybackCapture()) {
                String error = AudioMix.canBeUsedForPrivilegedMediaCapture(mFormat);
                if (error != null) {
                    throw new IllegalArgumentException(error);
                }
            }

            if (mToken == null) {
                mToken = new Binder();
            }

            return new AudioMix(mRule, mFormat, mRouteFlags, mCallbackFlags, mDeviceSystemType,
                    mDeviceAddress, mToken, mVirtualDeviceId);
        }

        private int getLoopbackDeviceSystemTypeForAudioMixingRule(AudioMixingRule rule) {
            switch (mRule.getTargetMixType()) {
                case MIX_TYPE_PLAYERS:
                    return AudioSystem.DEVICE_OUT_REMOTE_SUBMIX;
                case MIX_TYPE_RECORDERS:
                    return AudioSystem.DEVICE_IN_REMOTE_SUBMIX;
                default:
                    throw new IllegalArgumentException(
                            "Unknown mixing rule type - 0x" + Integer.toHexString(
                                    rule.getTargetMixType()));
            }
        }
    }
}
