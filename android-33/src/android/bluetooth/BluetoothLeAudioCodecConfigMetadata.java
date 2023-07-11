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
    private static final int AUDIO_CHANNEL_LOCATION_TYPE = 0x03;

    private final long mAudioLocation;
    private final byte[] mRawMetadata;

    private BluetoothLeAudioCodecConfigMetadata(long audioLocation, byte[] rawMetadata) {
        mAudioLocation = audioLocation;
        mRawMetadata = rawMetadata;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o != null && o instanceof BluetoothLeAudioCodecConfigMetadata) {
            final BluetoothLeAudioCodecConfigMetadata oth = (BluetoothLeAudioCodecConfigMetadata) o;
            return mAudioLocation == oth.getAudioLocation()
                && Arrays.equals(mRawMetadata, oth.getRawMetadata());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAudioLocation, mRawMetadata);
        // return mRawMetadata.hashCode();
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
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeAudioCodecConfigMetadata} from
     * parcel.
     * @hide
     */
    @SystemApi
    public static final @NonNull Parcelable.Creator<BluetoothLeAudioCodecConfigMetadata> CREATOR =
            new Parcelable.Creator<BluetoothLeAudioCodecConfigMetadata>() {
                @NonNull
                public BluetoothLeAudioCodecConfigMetadata createFromParcel(@NonNull Parcel in) {
                    long audioLocation = in.readLong();
                    int rawMetadataLen = in.readInt();
                    byte[] rawMetadata;
                    if (rawMetadataLen != -1) {
                        rawMetadata = new byte[rawMetadataLen];
                        in.readByteArray(rawMetadata);
                    } else {
                        rawMetadata = new byte[0];
                    }
                    return new BluetoothLeAudioCodecConfigMetadata(audioLocation, rawMetadata);
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
    public static @NonNull BluetoothLeAudioCodecConfigMetadata fromRawBytes(
            @NonNull byte[] rawBytes) {
        if (rawBytes == null) {
            throw new IllegalArgumentException("Raw bytes cannot be null");
        }
        List<TypeValueEntry> entries = BluetoothUtils.parseLengthTypeValueBytes(rawBytes);
        if (rawBytes.length > 0 && rawBytes[0] > 0 && entries.isEmpty()) {
            throw new IllegalArgumentException("No LTV entries are found from rawBytes of size "
                    + rawBytes.length);
        }
        long audioLocation = 0;
        for (TypeValueEntry entry : entries) {
            if (entry.getType() == AUDIO_CHANNEL_LOCATION_TYPE) {
                byte[] bytes = entry.getValue();
                // Get unsigned uint32_t to long
                audioLocation = ((bytes[0] & 0xFF) <<  0) | ((bytes[1] & 0xFF) <<  8)
                        | ((bytes[2] & 0xFF) << 16) | ((long) (bytes[3] & 0xFF) << 24);
            }
        }
        return new BluetoothLeAudioCodecConfigMetadata(audioLocation, rawBytes);
    }

    /**
     * Builder for {@link BluetoothLeAudioCodecConfigMetadata}.
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private long mAudioLocation = 0;
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
        public @NonNull Builder setAudioLocation(long audioLocation) {
            mAudioLocation = audioLocation;
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
            if (mAudioLocation != 0) {
                entries.removeIf(entry -> entry.getType() == AUDIO_CHANNEL_LOCATION_TYPE);
                entries.add(new TypeValueEntry(AUDIO_CHANNEL_LOCATION_TYPE,
                        ByteBuffer.allocate(Long.BYTES).putLong(mAudioLocation).array()));
            }
            byte[] rawBytes = BluetoothUtils.serializeTypeValue(entries);
            if (rawBytes == null) {
                throw new IllegalArgumentException("Failed to serialize entries to bytes");
            }
            return new BluetoothLeAudioCodecConfigMetadata(mAudioLocation, rawBytes);
        }
    }
}
