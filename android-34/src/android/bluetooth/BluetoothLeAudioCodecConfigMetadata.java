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

import static android.bluetooth.BluetoothLeAudioCodecConfig.FRAME_DURATION_10000;
import static android.bluetooth.BluetoothLeAudioCodecConfig.FRAME_DURATION_7500;
import static android.bluetooth.BluetoothLeAudioCodecConfig.FRAME_DURATION_NONE;
import static android.bluetooth.BluetoothLeAudioCodecConfig.FrameDuration;
import static android.bluetooth.BluetoothLeAudioCodecConfig.SAMPLE_RATE_16000;
import static android.bluetooth.BluetoothLeAudioCodecConfig.SAMPLE_RATE_24000;
import static android.bluetooth.BluetoothLeAudioCodecConfig.SAMPLE_RATE_32000;
import static android.bluetooth.BluetoothLeAudioCodecConfig.SAMPLE_RATE_44100;
import static android.bluetooth.BluetoothLeAudioCodecConfig.SAMPLE_RATE_48000;
import static android.bluetooth.BluetoothLeAudioCodecConfig.SAMPLE_RATE_8000;
import static android.bluetooth.BluetoothLeAudioCodecConfig.SAMPLE_RATE_NONE;
import static android.bluetooth.BluetoothLeAudioCodecConfig.SampleRate;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothUtils.TypeValueEntry;
import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A class representing the codec specific config metadata information defined in the Basic Audio
 * Profile.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeAudioCodecConfigMetadata implements Parcelable {
    private static final int SAMPLING_FREQUENCY_TYPE = 0x01;
    private static final int FRAME_DURATION_TYPE = 0x02;
    private static final int AUDIO_CHANNEL_LOCATION_TYPE = 0x03;
    private static final int OCTETS_PER_FRAME_TYPE = 0x04;

    private final long mAudioLocation;
    private final @SampleRate int mSampleRate;
    private final @FrameDuration int mFrameDuration;
    private final int mOctetsPerFrame;
    private final byte[] mRawMetadata;

    /**
     * Audio codec sampling frequency from metadata.
     */
    private static final int CONFIG_SAMPLING_FREQUENCY_UNKNOWN = 0;
    private static final int CONFIG_SAMPLING_FREQUENCY_8000 = 0x01;
    private static final int CONFIG_SAMPLING_FREQUENCY_16000 = 0x03;
    private static final int CONFIG_SAMPLING_FREQUENCY_24000 = 0x05;
    private static final int CONFIG_SAMPLING_FREQUENCY_32000 = 0x06;
    private static final int CONFIG_SAMPLING_FREQUENCY_44100 = 0x07;
    private static final int CONFIG_SAMPLING_FREQUENCY_48000 = 0x08;

    /**
     * Audio codec config frame duration from metadata.
     */
    private static final int CONFIG_FRAME_DURATION_UNKNOWN = -1;
    private static final int CONFIG_FRAME_DURATION_7500 = 0x00;
    private static final int CONFIG_FRAME_DURATION_10000 = 0x01;

    private BluetoothLeAudioCodecConfigMetadata(long audioLocation,
            @SampleRate int sampleRate, @FrameDuration int frameDuration,
            int octetsPerFrame, byte[] rawMetadata) {
        mAudioLocation = audioLocation;
        mSampleRate = sampleRate;
        mFrameDuration = frameDuration;
        mOctetsPerFrame = octetsPerFrame;
        mRawMetadata = rawMetadata;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o != null && o instanceof BluetoothLeAudioCodecConfigMetadata) {
            final BluetoothLeAudioCodecConfigMetadata oth = (BluetoothLeAudioCodecConfigMetadata) o;
            return mAudioLocation == oth.getAudioLocation()
                    && mSampleRate == oth.getSampleRate()
                    && mFrameDuration == oth.getFrameDuration()
                    && mOctetsPerFrame == oth.getOctetsPerFrame()
                    && Arrays.equals(mRawMetadata, oth.getRawMetadata());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAudioLocation, mSampleRate, mFrameDuration,
                mOctetsPerFrame, Arrays.hashCode(mRawMetadata));
    }

    /**
     * Get the audio location information as defined in the Generic Audio section of Bluetooth
     * Assigned numbers.
     *
     * @return configured audio location, -1 if this metadata does not exist
     * @hide
     */
    @SystemApi
    public long getAudioLocation() {
        return mAudioLocation;
    }

    /**
     * Get the audio sample rate information as defined in the Generic Audio section of
     * Bluetooth Assigned numbers 6.12.4.1 Supported_Sampling_Frequencies.
     *
     * Internally this is converted from Sampling_Frequency values as defined in
     * 6.12.5.1
     *
     * @return configured sample rate from meta data,
     * {@link BluetoothLeAudioCodecConfig#SAMPLE_RATE_NONE}
     * if this metadata does not exist
     * @hide
     */
    @SystemApi
    public @SampleRate int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Get the audio frame duration information as defined in the Generic Audio section of
     * Bluetooth Assigned numbers 6.12.5.2 Frame_Duration.
     *
     * Internally this is converted from Frame_Durations values as defined in
     * 6.12.4.2
     *
     * @return configured frame duration from meta data,
     * {@link BluetoothLeAudioCodecConfig#FRAME_DURATION_NONE}
     * if this metadata does not exist
     * @hide
     */
    @SystemApi
    public @FrameDuration int getFrameDuration() {
        return mFrameDuration;
    }

    /**
     * Get the audio octets per frame information as defined in the Generic Audio section of
     * Bluetooth Assigned numbers.
     *
     * @return configured octets per frame from meta data
     * 0 if this metadata does not exist
     * @hide
     */
    @SystemApi
    public int getOctetsPerFrame() {
        return mOctetsPerFrame;
    }

    /**
     * Get the raw bytes of stream metadata in Bluetooth LTV format.
     *
     * Bluetooth LTV format for stream metadata is defined in the Generic Audio
     * section of <a href="https://www.bluetooth.com/specifications/assigned-numbers/">Bluetooth Assigned Numbers</a>,
     * including metadata that was not covered by the getter methods in this class.
     *
     * @return raw bytes of stream metadata in Bluetooth LTV format
     * @hide
     */
    @SystemApi
    public @NonNull byte[] getRawMetadata() {
        return mRawMetadata;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mAudioLocation);
        if (mRawMetadata != null) {
            out.writeInt(mRawMetadata.length);
            out.writeByteArray(mRawMetadata);
        } else {
            out.writeInt(-1);
        }
        out.writeInt(mSampleRate);
        out.writeInt(mFrameDuration);
        out.writeInt(mOctetsPerFrame);
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeAudioCodecConfigMetadata} from
     * parcel.
     * @hide
     */
    @SystemApi
    @NonNull
    public static final Creator<BluetoothLeAudioCodecConfigMetadata> CREATOR = new Creator<>() {
        public @NonNull BluetoothLeAudioCodecConfigMetadata createFromParcel(@NonNull Parcel in) {
            long audioLocation = in.readLong();
            int rawMetadataLen = in.readInt();
            byte[] rawMetadata;
            if (rawMetadataLen != -1) {
                rawMetadata = new byte[rawMetadataLen];
                in.readByteArray(rawMetadata);
            } else {
                rawMetadata = new byte[0];
            }
            int sampleRate = in.readInt();
            int frameDuration = in.readInt();
            int octetsPerFrame = in.readInt();
            return new BluetoothLeAudioCodecConfigMetadata(audioLocation, sampleRate,
                    frameDuration, octetsPerFrame, rawMetadata);
        }

        public @NonNull BluetoothLeAudioCodecConfigMetadata[] newArray(int size) {
            return new BluetoothLeAudioCodecConfigMetadata[size];
        }
    };

    /**
     * Construct a {@link BluetoothLeAudioCodecConfigMetadata} from raw bytes.
     *
     * The byte array will be parsed and values for each getter will be populated
     *
     * Raw metadata cannot be set using builder in order to maintain raw bytes and getter value
     * consistency
     *
     * @param rawBytes raw bytes of stream metadata in Bluetooth LTV format
     * @return parsed {@link BluetoothLeAudioCodecConfigMetadata} object
     * @throws IllegalArgumentException if <var>rawBytes</var> is null or when the raw bytes cannot
     * be parsed to build the object
     * @hide
     */
    @SystemApi
    @NonNull
    public static BluetoothLeAudioCodecConfigMetadata fromRawBytes(@NonNull byte[] rawBytes) {
        if (rawBytes == null) {
            throw new IllegalArgumentException("Raw bytes cannot be null");
        }
        List<TypeValueEntry> entries = BluetoothUtils.parseLengthTypeValueBytes(rawBytes);
        if (rawBytes.length > 0 && rawBytes[0] > 0 && entries.isEmpty()) {
            throw new IllegalArgumentException("No LTV entries are found from rawBytes of size "
                    + rawBytes.length);
        }
        long audioLocation = 0;
        int samplingFrequency = CONFIG_SAMPLING_FREQUENCY_UNKNOWN;
        int frameDuration = CONFIG_FRAME_DURATION_UNKNOWN;
        int octetsPerFrame = 0;
        for (TypeValueEntry entry : entries) {
            if (entry.getType() == AUDIO_CHANNEL_LOCATION_TYPE) {
                byte[] bytes = entry.getValue();
                // Get unsigned uint32_t to long
                audioLocation = ((bytes[0] & 0xFF) <<  0) | ((bytes[1] & 0xFF) <<  8)
                        | ((bytes[2] & 0xFF) << 16) | ((long) (bytes[3] & 0xFF) << 24);
            } else if (entry.getType() == SAMPLING_FREQUENCY_TYPE) {
                byte[] bytes = entry.getValue();
                // Get one byte for sampling frequency in value
                samplingFrequency = (int) (bytes[0] & 0xFF);
            } else if (entry.getType() == FRAME_DURATION_TYPE) {
                byte[] bytes = entry.getValue();
                // Get one byte for frame duration in value
                frameDuration = (int) (bytes[0] & 0xFF);
            } else if (entry.getType() == OCTETS_PER_FRAME_TYPE) {
                byte[] bytes = entry.getValue();
                // Get two bytes for octets per frame to int
                octetsPerFrame = ((bytes[0] & 0xFF) <<  0) | ((int) (bytes[1] & 0xFF) <<  8);
            }
        }
        return new BluetoothLeAudioCodecConfigMetadata(audioLocation,
                convertToSampleRateBitset(samplingFrequency),
                convertToFrameDurationBitset(frameDuration),
                octetsPerFrame, rawBytes);
    }

    /**
     * Builder for {@link BluetoothLeAudioCodecConfigMetadata}.
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private long mAudioLocation = 0;
        private int mSampleRate = SAMPLE_RATE_NONE;
        private int mFrameDuration = FRAME_DURATION_NONE;
        private int mOctetsPerFrame = 0;
        private byte[] mRawMetadata = null;

        /**
         * Create an empty builder.
         * @hide
         */
        @SystemApi
        public Builder() {}

        /**
         * Create a builder with copies of information from original object.
         *
         * @param original original object
         * @hide
         */
        @SystemApi
        public Builder(@NonNull BluetoothLeAudioCodecConfigMetadata original) {
            mAudioLocation = original.getAudioLocation();
            mSampleRate = original.getSampleRate();
            mFrameDuration = original.getFrameDuration();
            mOctetsPerFrame = original.getOctetsPerFrame();
            mRawMetadata = original.getRawMetadata();
        }

        /**
         * Set the audio location information as defined in the Generic Audio section of Bluetooth
         * Assigned numbers.
         *
         * @param audioLocation configured audio location, -1 if does not exist
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setAudioLocation(long audioLocation) {
            mAudioLocation = audioLocation;
            return this;
        }

        /**
         * Set the audio sample rate information as defined in the Generic Audio section of
         * Bluetooth Assigned 6.12.4.1 Supported_Sampling_Frequencies.
         *
         * Internally this will be converted to Sampling_Frequency values as defined in
         * 6.12.5.1
         *
         * @param sampleRate configured sample rate in meta data
         * @return this builder
         * @throws IllegalArgumentException if sample rate is invalid value
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setSampleRate(@SampleRate int sampleRate) {
            if (sampleRate != SAMPLE_RATE_NONE
                    && sampleRate != SAMPLE_RATE_8000
                    && sampleRate != SAMPLE_RATE_16000
                    && sampleRate != SAMPLE_RATE_24000
                    && sampleRate != SAMPLE_RATE_32000
                    && sampleRate != SAMPLE_RATE_44100
                    && sampleRate != SAMPLE_RATE_48000) {
                throw new IllegalArgumentException("Invalid sample rate "
                        + sampleRate);
            }
            mSampleRate = sampleRate;
            return this;
        }

        /**
         * Set the audio frame duration information as defined in the Generic Audio section of
         * Bluetooth Assigned numbers 6.12.5.2 Frame_Duration.
         *
         * Internally this will be converted to Frame_Durations values as defined in
         * 6.12.4.2
         *
         * @param frameDuration configured frame duration in meta data
         * @return this builder
         * @throws IllegalArgumentException if frameDuration is invalid value
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setFrameDuration(@FrameDuration
                int frameDuration) {
            if (frameDuration != FRAME_DURATION_NONE
                    && frameDuration != FRAME_DURATION_7500
                    && frameDuration != FRAME_DURATION_10000) {
                throw new IllegalArgumentException("Invalid frame duration " + frameDuration);
            }
            mFrameDuration = frameDuration;
            return this;
        }

        /**
         * Set the audio octets per frame information as defined in the Generic Audio section of
         * Bluetooth Assigned numbers.
         *
         * @param octetsPerFrame configured octets per frame in meta data
         * @return this builder
         * @throws IllegalArgumentException if octetsPerFrame is invalid value
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setOctetsPerFrame(int octetsPerFrame) {
            if (octetsPerFrame < 0) {
                throw new IllegalArgumentException("Invalid octetsPerFrame " + octetsPerFrame);
            }
            mOctetsPerFrame = octetsPerFrame;
            return this;
        }

        /**
         * Build {@link BluetoothLeAudioCodecConfigMetadata}.
         *
         * @return constructed {@link BluetoothLeAudioCodecConfigMetadata}
         * @throws IllegalArgumentException if the object cannot be built
         * @hide
         */
        @SystemApi
        public @NonNull BluetoothLeAudioCodecConfigMetadata build() {
            List<TypeValueEntry> entries = new ArrayList<>();
            if (mRawMetadata != null) {
                entries = BluetoothUtils.parseLengthTypeValueBytes(mRawMetadata);
                if (mRawMetadata.length > 0 && mRawMetadata[0] > 0 && entries.isEmpty()) {
                    throw new IllegalArgumentException("No LTV entries are found from rawBytes of"
                            + " size " + mRawMetadata.length + " please check the original object"
                            + " passed to Builder's copy constructor");
                }
            }
            if (mSampleRate != SAMPLE_RATE_NONE) {
                int samplingFrequency = convertToSamplingFrequencyValue(mSampleRate);
                entries.removeIf(entry -> entry.getType() == SAMPLING_FREQUENCY_TYPE);
                entries.add(new TypeValueEntry(SAMPLING_FREQUENCY_TYPE,
                        ByteBuffer.allocate(1)
                        .put((byte) (samplingFrequency & 0xFF)).array()));
            }
            if (mFrameDuration != FRAME_DURATION_NONE) {
                int frameDuration = convertToFrameDurationValue(mFrameDuration);
                entries.removeIf(entry -> entry.getType() == FRAME_DURATION_TYPE);
                entries.add(new TypeValueEntry(FRAME_DURATION_TYPE,
                        ByteBuffer.allocate(1)
                        .put((byte) (frameDuration & 0xFF)).array()));
            }
            if (mAudioLocation != -1) {
                entries.removeIf(entry -> entry.getType() == AUDIO_CHANNEL_LOCATION_TYPE);
                entries.add(new TypeValueEntry(AUDIO_CHANNEL_LOCATION_TYPE,
                        ByteBuffer.allocate(4)
                        .putInt((int) (mAudioLocation & 0xFFFFFFFF)).array()));
            }
            if (mOctetsPerFrame != 0) {
                entries.removeIf(entry -> entry.getType() == OCTETS_PER_FRAME_TYPE);
                entries.add(new TypeValueEntry(OCTETS_PER_FRAME_TYPE,
                        ByteBuffer.allocate(2)
                        .putShort((short) (mOctetsPerFrame & 0xFFFF)).array()));
            }
            byte[] rawBytes = BluetoothUtils.serializeTypeValue(entries);
            if (rawBytes == null) {
                throw new IllegalArgumentException("Failed to serialize entries to bytes");
            }
            return new BluetoothLeAudioCodecConfigMetadata(mAudioLocation, mSampleRate,
                    mFrameDuration, mOctetsPerFrame, rawBytes);
        }
    }

    private static int convertToSampleRateBitset(int samplingFrequencyValue) {
        switch (samplingFrequencyValue) {
            case CONFIG_SAMPLING_FREQUENCY_8000:
                return SAMPLE_RATE_8000;
            case CONFIG_SAMPLING_FREQUENCY_16000:
                return SAMPLE_RATE_16000;
            case CONFIG_SAMPLING_FREQUENCY_24000:
                return SAMPLE_RATE_24000;
            case CONFIG_SAMPLING_FREQUENCY_32000:
                return SAMPLE_RATE_32000;
            case CONFIG_SAMPLING_FREQUENCY_44100:
                return SAMPLE_RATE_44100;
            case CONFIG_SAMPLING_FREQUENCY_48000:
                return SAMPLE_RATE_48000;
            default:
                return SAMPLE_RATE_NONE;
        }
    }

    private static int convertToSamplingFrequencyValue(int sampleRateBitSet) {
        switch (sampleRateBitSet) {
            case SAMPLE_RATE_8000:
                return CONFIG_SAMPLING_FREQUENCY_8000;
            case SAMPLE_RATE_16000:
                return CONFIG_SAMPLING_FREQUENCY_16000;
            case SAMPLE_RATE_24000:
                return CONFIG_SAMPLING_FREQUENCY_24000;
            case SAMPLE_RATE_32000:
                return CONFIG_SAMPLING_FREQUENCY_32000;
            case SAMPLE_RATE_44100:
                return CONFIG_SAMPLING_FREQUENCY_44100;
            case SAMPLE_RATE_48000:
                return CONFIG_SAMPLING_FREQUENCY_48000;
            default:
                return CONFIG_SAMPLING_FREQUENCY_UNKNOWN;
        }
    }

    private static int convertToFrameDurationBitset(int frameDurationValue) {
        switch (frameDurationValue) {
            case CONFIG_FRAME_DURATION_7500:
                return FRAME_DURATION_7500;
            case CONFIG_FRAME_DURATION_10000:
                return FRAME_DURATION_10000;
            default:
                return FRAME_DURATION_NONE;
        }
    }

    private static int convertToFrameDurationValue(int frameDurationBitset) {
        switch (frameDurationBitset) {
            case FRAME_DURATION_7500:
                return CONFIG_FRAME_DURATION_7500;
            case FRAME_DURATION_10000:
                return CONFIG_FRAME_DURATION_10000;
            default:
                return CONFIG_FRAME_DURATION_UNKNOWN;
        }
    }
}
