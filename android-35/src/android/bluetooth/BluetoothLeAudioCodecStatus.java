/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.bluetooth;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the codec status (configuration and capability) for a Bluetooth Le Audio source
 * device.
 *
 * @see BluetoothLeAudio
 */
public final class BluetoothLeAudioCodecStatus implements Parcelable {
    /**
     * Extra for the codec configuration intents of the individual profiles.
     *
     * <p>This extra represents the current codec status of the Le Audio profile.
     */
    public static final String EXTRA_LE_AUDIO_CODEC_STATUS =
            "android.bluetooth.extra.LE_AUDIO_CODEC_STATUS";

    private final @Nullable BluetoothLeAudioCodecConfig mInputCodecConfig;
    private final @Nullable BluetoothLeAudioCodecConfig mOutputCodecConfig;
    private final @Nullable List<BluetoothLeAudioCodecConfig> mInputCodecsLocalCapabilities;
    private final @Nullable List<BluetoothLeAudioCodecConfig> mOutputCodecsLocalCapabilities;
    private final @Nullable List<BluetoothLeAudioCodecConfig> mInputCodecsSelectableCapabilities;
    private final @Nullable List<BluetoothLeAudioCodecConfig> mOutputCodecsSelectableCapabilities;

    /**
     * Represents the codec status for a Bluetooth LE Audio source device.
     *
     * @param inputCodecConfig the current input codec configuration.
     * @param outputCodecConfig the current output codec configuration.
     * @param inputCodecsLocalCapabilities the local input codecs capabilities.
     * @param outputCodecsLocalCapabilities the local output codecs capabilities.
     * @param inputCodecsSelectableCapabilities the selectable input codecs capabilities.
     * @param outputCodecsSelectableCapabilities the selectable output codecs capabilities.
     */
    public BluetoothLeAudioCodecStatus(
            @Nullable BluetoothLeAudioCodecConfig inputCodecConfig,
            @Nullable BluetoothLeAudioCodecConfig outputCodecConfig,
            @NonNull List<BluetoothLeAudioCodecConfig> inputCodecsLocalCapabilities,
            @NonNull List<BluetoothLeAudioCodecConfig> outputCodecsLocalCapabilities,
            @NonNull List<BluetoothLeAudioCodecConfig> inputCodecsSelectableCapabilities,
            @NonNull List<BluetoothLeAudioCodecConfig> outputCodecsSelectableCapabilities) {
        mInputCodecConfig = inputCodecConfig;
        mOutputCodecConfig = outputCodecConfig;
        mInputCodecsLocalCapabilities = inputCodecsLocalCapabilities;
        mOutputCodecsLocalCapabilities = outputCodecsLocalCapabilities;
        mInputCodecsSelectableCapabilities = inputCodecsSelectableCapabilities;
        mOutputCodecsSelectableCapabilities = outputCodecsSelectableCapabilities;
    }

    private BluetoothLeAudioCodecStatus(Parcel in) {
        mInputCodecConfig = in.readTypedObject(BluetoothLeAudioCodecConfig.CREATOR);
        mOutputCodecConfig = in.readTypedObject(BluetoothLeAudioCodecConfig.CREATOR);
        mInputCodecsLocalCapabilities =
                in.createTypedArrayList(BluetoothLeAudioCodecConfig.CREATOR);
        mOutputCodecsLocalCapabilities =
                in.createTypedArrayList(BluetoothLeAudioCodecConfig.CREATOR);
        mInputCodecsSelectableCapabilities =
                in.createTypedArrayList(BluetoothLeAudioCodecConfig.CREATOR);
        mOutputCodecsSelectableCapabilities =
                in.createTypedArrayList(BluetoothLeAudioCodecConfig.CREATOR);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof BluetoothLeAudioCodecStatus) {
            BluetoothLeAudioCodecStatus other = (BluetoothLeAudioCodecStatus) o;
            return (Objects.equals(other.mInputCodecConfig, mInputCodecConfig)
                    && Objects.equals(other.mOutputCodecConfig, mOutputCodecConfig)
                    && sameCapabilities(
                            other.mInputCodecsLocalCapabilities, mInputCodecsLocalCapabilities)
                    && sameCapabilities(
                            other.mOutputCodecsLocalCapabilities, mOutputCodecsLocalCapabilities)
                    && sameCapabilities(
                            other.mInputCodecsSelectableCapabilities,
                            mInputCodecsSelectableCapabilities)
                    && sameCapabilities(
                            other.mOutputCodecsSelectableCapabilities,
                            mOutputCodecsSelectableCapabilities));
        }
        return false;
    }

    /**
     * Checks whether two lists of capabilities contain same capabilities. The order of the
     * capabilities in each list is ignored.
     *
     * @param c1 the first list of capabilities to compare
     * @param c2 the second list of capabilities to compare
     * @return {@code true} if both lists contain same capabilities
     */
    private static boolean sameCapabilities(
            @Nullable List<BluetoothLeAudioCodecConfig> c1,
            @Nullable List<BluetoothLeAudioCodecConfig> c2) {
        if (c1 == null) {
            return (c2 == null);
        }
        if (c2 == null) {
            return false;
        }
        if (c1.size() != c2.size()) {
            return false;
        }
        return c1.containsAll(c2);
    }

    private boolean isCodecConfigSelectable(
            BluetoothLeAudioCodecConfig codecConfig, BluetoothLeAudioCodecConfig selectableConfig) {
        if (codecConfig.getCodecType() != selectableConfig.getCodecType()) {
            return false;
        }
        if ((codecConfig.getFrameDuration() != BluetoothLeAudioCodecConfig.FRAME_DURATION_NONE)
                && ((codecConfig.getFrameDuration() & selectableConfig.getFrameDuration()) == 0)) {
            return false;
        }
        if ((codecConfig.getChannelCount() != BluetoothLeAudioCodecConfig.CHANNEL_COUNT_NONE)
                && ((codecConfig.getChannelCount() & selectableConfig.getChannelCount()) == 0)) {
            return false;
        }
        if ((codecConfig.getSampleRate() != BluetoothLeAudioCodecConfig.SAMPLE_RATE_NONE)
                && ((codecConfig.getSampleRate() & selectableConfig.getSampleRate()) == 0)) {
            return false;
        }
        if ((codecConfig.getBitsPerSample() != BluetoothLeAudioCodecConfig.BITS_PER_SAMPLE_NONE)
                && ((codecConfig.getBitsPerSample() & selectableConfig.getBitsPerSample()) == 0)) {
            return false;
        }
        if ((codecConfig.getOctetsPerFrame() != 0)
                && ((codecConfig.getOctetsPerFrame() < selectableConfig.getMinOctetsPerFrame())
                        || (codecConfig.getOctetsPerFrame()
                                > selectableConfig.getMaxOctetsPerFrame()))) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether the Input codec config matches the selectable capabilities. Any parameters of
     * the codec config with NONE value will be considered a wildcard matching.
     *
     * @param codecConfig the codec config to compare against
     * @return {@code true} if the codec config matches, {@code false} otherwise
     */
    public boolean isInputCodecConfigSelectable(@Nullable BluetoothLeAudioCodecConfig codecConfig) {
        if (codecConfig == null) {
            return false;
        }
        for (BluetoothLeAudioCodecConfig selectableConfig : mInputCodecsSelectableCapabilities) {
            if (isCodecConfigSelectable(codecConfig, selectableConfig)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the Output codec config matches the selectable capabilities. Any parameters of
     * the codec config with NONE value will be considered a wildcard matching.
     *
     * @param codecConfig the codec config to compare against
     * @return {@code true} if the codec config matches, {@code false} otherwise
     */
    public boolean isOutputCodecConfigSelectable(
            @Nullable BluetoothLeAudioCodecConfig codecConfig) {
        if (codecConfig == null) {
            return false;
        }
        for (BluetoothLeAudioCodecConfig selectableConfig : mOutputCodecsSelectableCapabilities) {
            if (isCodecConfigSelectable(codecConfig, selectableConfig)) {
                return true;
            }
        }
        return false;
    }

    /** Returns a hash based on the codec config and local capabilities. */
    @Override
    public int hashCode() {
        return Objects.hash(
                mInputCodecConfig,
                mOutputCodecConfig,
                mInputCodecsLocalCapabilities,
                mOutputCodecsLocalCapabilities,
                mInputCodecsSelectableCapabilities,
                mOutputCodecsSelectableCapabilities);
    }

    /**
     * Returns a {@link String} that describes each BluetoothLeAudioCodecStatus parameter current
     * value.
     */
    @Override
    public String toString() {
        return "{mInputCodecConfig:"
                + mInputCodecConfig
                + ",mOutputCodecConfig:"
                + mOutputCodecConfig
                + ",mInputCodecsLocalCapabilities:"
                + mInputCodecsLocalCapabilities
                + ",mOutputCodecsLocalCapabilities:"
                + mOutputCodecsLocalCapabilities
                + ",mInputCodecsSelectableCapabilities:"
                + mInputCodecsSelectableCapabilities
                + ",mOutputCodecsSelectableCapabilities:"
                + mOutputCodecsSelectableCapabilities
                + "}";
    }

    /**
     * @return 0
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /** {@link Parcelable.Creator} interface implementation. */
    public static final @android.annotation.NonNull Parcelable.Creator<BluetoothLeAudioCodecStatus>
            CREATOR =
                    new Parcelable.Creator<BluetoothLeAudioCodecStatus>() {
                        public BluetoothLeAudioCodecStatus createFromParcel(Parcel in) {
                            return new BluetoothLeAudioCodecStatus(in);
                        }

                        public BluetoothLeAudioCodecStatus[] newArray(int size) {
                            return new BluetoothLeAudioCodecStatus[size];
                        }
                    };

    /**
     * Flattens the object to a parcel.
     *
     * @param out The Parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeTypedObject(mInputCodecConfig, flags);
        out.writeTypedObject(mOutputCodecConfig, flags);
        out.writeTypedList(mInputCodecsLocalCapabilities);
        out.writeTypedList(mOutputCodecsLocalCapabilities);
        out.writeTypedList(mInputCodecsSelectableCapabilities);
        out.writeTypedList(mOutputCodecsSelectableCapabilities);
    }

    /**
     * Returns the current Input codec configuration.
     *
     * @return The current input codec config.
     */
    public @Nullable BluetoothLeAudioCodecConfig getInputCodecConfig() {
        return mInputCodecConfig;
    }

    /**
     * Returns the current Output codec configuration.
     *
     * @return The current output codec config.
     */
    public @Nullable BluetoothLeAudioCodecConfig getOutputCodecConfig() {
        return mOutputCodecConfig;
    }

    /**
     * Returns the input codecs local capabilities.
     *
     * @return The list of codec config that supported by the local system.
     */
    public @NonNull List<BluetoothLeAudioCodecConfig> getInputCodecLocalCapabilities() {
        return (mInputCodecsLocalCapabilities == null)
                ? Collections.emptyList()
                : mInputCodecsLocalCapabilities;
    }

    /**
     * Returns the output codecs local capabilities.
     *
     * @return The list of codec config that supported by the local system.
     */
    public @NonNull List<BluetoothLeAudioCodecConfig> getOutputCodecLocalCapabilities() {
        return (mOutputCodecsLocalCapabilities == null)
                ? Collections.emptyList()
                : mOutputCodecsLocalCapabilities;
    }

    /**
     * Returns the Input codecs selectable capabilities.
     *
     * @return The list of codec config that supported by both of the local system and remote
     *     devices.
     */
    public @NonNull List<BluetoothLeAudioCodecConfig> getInputCodecSelectableCapabilities() {
        return (mInputCodecsSelectableCapabilities == null)
                ? Collections.emptyList()
                : mInputCodecsSelectableCapabilities;
    }

    /**
     * Returns the Output codecs selectable capabilities.
     *
     * @return The list of codec config that supported by both of the local system and remote
     *     devices.
     */
    public @NonNull List<BluetoothLeAudioCodecConfig> getOutputCodecSelectableCapabilities() {
        return (mOutputCodecsSelectableCapabilities == null)
                ? Collections.emptyList()
                : mOutputCodecsSelectableCapabilities;
    }
}
