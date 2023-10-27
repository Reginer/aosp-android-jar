/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class contains the subgroup level information as defined in the BASE structure of Basic
 * Audio profile.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcastSubgroup implements Parcelable {
    private final long mCodecId;
    private final BluetoothLeAudioCodecConfigMetadata mCodecSpecificConfig;
    private final BluetoothLeAudioContentMetadata mContentMetadata;
    private final List<BluetoothLeBroadcastChannel> mChannels;

    private BluetoothLeBroadcastSubgroup(long codecId,
            BluetoothLeAudioCodecConfigMetadata codecSpecificConfig,
            BluetoothLeAudioContentMetadata contentMetadata,
            List<BluetoothLeBroadcastChannel> channels) {
        mCodecId = codecId;
        mCodecSpecificConfig = codecSpecificConfig;
        mContentMetadata = contentMetadata;
        mChannels = channels;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof BluetoothLeBroadcastSubgroup)) {
            return false;
        }
        final BluetoothLeBroadcastSubgroup other = (BluetoothLeBroadcastSubgroup) o;
        return mCodecId == other.getCodecId()
                && mCodecSpecificConfig.equals(other.getCodecSpecificConfig())
                && mContentMetadata.equals(other.getContentMetadata())
                && mChannels.equals(other.getChannels());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCodecId, mCodecSpecificConfig, mContentMetadata, mChannels);
    }

    /**
     * Get the codec ID field as defined by the Basic Audio Profile.
     *
     * The codec ID field has 5 octets, with
     * - Octet 0: Coding_Format as defined in Bluetooth Assigned Numbers
     * - Octet 1-2: Company ID as defined in Bluetooth Assigned Numbers
     *              Shall be 0x0000 if octet 0 != 0xFF
     * - Octet 3-4: Vendor-specific codec ID
     *              Shall be 0x0000 if octet 0 != 0xFF
     *
     * @return 5-byte codec ID field in Java long format
     * @hide
     */
    @SystemApi
    public long getCodecId() {
        return mCodecId;
    }

    /**
     * Get codec specific config metadata for this subgroup.
     *
     * @return codec specific config metadata for this subgroup
     * @hide
     */
    @SystemApi
    @NonNull
    public BluetoothLeAudioCodecConfigMetadata getCodecSpecificConfig() {
        return mCodecSpecificConfig;
    }

    /**
     * Get content metadata for this Broadcast Source subgroup.
     *
     * @return content metadata for this Broadcast Source subgroup
     * @hide
     */
    @SystemApi
    public @NonNull BluetoothLeAudioContentMetadata getContentMetadata() {
        return mContentMetadata;
    }

    /**
     * Indicate if Broadcast Sink should have a preferred Broadcast Channel (BIS).
     *
     * Only used by Broadcast Assistant and Sink. Ignored by Broadcast Source
     *
     * @return true if Broadcast Sink has at least one preferred Broadcast Channel (BIS) as
     * indicated by {@link BluetoothLeBroadcastChannel#isSelected()}
     * @hide
     */
    @SystemApi
    public boolean hasChannelPreference() {
        return mChannels.stream().anyMatch(BluetoothLeBroadcastChannel::isSelected);
    }

    /**
     * Get list of Broadcast Channels included in this Broadcast subgroup.
     *
     * Each Broadcast Channel represents a Broadcast Isochronous Stream (BIS)
     *
     * A Broadcast subgroup should contain at least 1 Broadcast Channel
     *
     * @return list of Broadcast Channels included in this Broadcast subgroup
     * @hide
     */
    @SystemApi
    public @NonNull List<BluetoothLeBroadcastChannel> getChannels() {
        return mChannels;
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
        out.writeLong(mCodecId);
        out.writeTypedObject(mCodecSpecificConfig, 0);
        out.writeTypedObject(mContentMetadata, 0);
        out.writeTypedList(mChannels);
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeBroadcastSubgroup} from parcel.
     * @hide
     */
    @SystemApi
    @NonNull
    public static final Creator<BluetoothLeBroadcastSubgroup> CREATOR = new Creator<>() {
        public @NonNull BluetoothLeBroadcastSubgroup createFromParcel(@NonNull Parcel in) {
            Builder builder = new Builder();
            builder.setCodecId(in.readLong());
            builder.setCodecSpecificConfig(in.readTypedObject(
                    BluetoothLeAudioCodecConfigMetadata.CREATOR));
            builder.setContentMetadata(
                    in.readTypedObject(BluetoothLeAudioContentMetadata.CREATOR));
            List<BluetoothLeBroadcastChannel> channels = new ArrayList<>();
            in.readTypedList(channels, BluetoothLeBroadcastChannel.CREATOR);
            for (BluetoothLeBroadcastChannel channel : channels) {
                builder.addChannel(channel);
            }
            return builder.build();
        }

        public @NonNull BluetoothLeBroadcastSubgroup[] newArray(int size) {
            return new BluetoothLeBroadcastSubgroup[size];
        }
    };

    private static final int UNKNOWN_VALUE_PLACEHOLDER = -1;

    /**
     * Builder for {@link BluetoothLeBroadcastSubgroup}.
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private long mCodecId = UNKNOWN_VALUE_PLACEHOLDER;
        private BluetoothLeAudioCodecConfigMetadata mCodecSpecificConfig = null;
        private BluetoothLeAudioContentMetadata mContentMetadata = null;
        private List<BluetoothLeBroadcastChannel> mChannels = new ArrayList<>();

        /**
         * Create an empty constructor.
         *
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
        public Builder(@NonNull BluetoothLeBroadcastSubgroup original) {
            mCodecId = original.getCodecId();
            mCodecSpecificConfig = original.getCodecSpecificConfig();
            mContentMetadata = original.getContentMetadata();
            mChannels = original.getChannels();
        }

        /**
         * Set the codec ID field as defined by the Basic Audio Profile.
         *
         * The codec ID field has 5 octets, with
         * - Octet 0: Coding_Format as defined in Bluetooth Assigned Numbers
         * - Octet 1-2: Company ID as defined in Bluetooth Assigned Numbers
         *              Shall be 0x0000 if octet 0 != 0xFF
         * - Octet 3-4: Vendor-specific codec ID
         *              Shall be 0x0000 if octet 0 != 0xFF
         *
         * @param codecId 5-byte codec ID field in Java long format
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setCodecId(long codecId) {
            mCodecId = codecId;
            return this;
        }

        /**
         * Set codec specific config metadata for this subgroup.
         *
         * @param codecSpecificConfig codec specific config metadata for this subgroup
         * @throws {@link NullPointerException} if codecSpecificConfig is null
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setCodecSpecificConfig(
                @NonNull BluetoothLeAudioCodecConfigMetadata codecSpecificConfig) {
            Objects.requireNonNull(codecSpecificConfig, "codecSpecificConfig cannot be null");
            mCodecSpecificConfig = codecSpecificConfig;
            return this;
        }

        /**
         * Set content metadata for this Broadcast Source subgroup.
         *
         * @param contentMetadata content metadata for this Broadcast Source subgroup
         * @throws NullPointerException if contentMetadata is null
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setContentMetadata(
                @NonNull BluetoothLeAudioContentMetadata contentMetadata) {
            Objects.requireNonNull(contentMetadata, "contentMetadata cannot be null");
            mContentMetadata = contentMetadata;
            return this;
        }

        /**
         * Add a Broadcast Channel to this Broadcast subgroup.
         *
         * Each Broadcast Channel represents a Broadcast Isochronous Stream (BIS)
         *
         * A Broadcast subgroup should contain at least 1 Broadcast Channel
         *
         * @param channel  a Broadcast Channel to be added to this Broadcast subgroup
         * @throws NullPointerException if channel is null
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder addChannel(@NonNull BluetoothLeBroadcastChannel channel) {
            Objects.requireNonNull(channel, "channel cannot be null");
            mChannels.add(channel);
            return this;
        }

        /**
         * Clear channel list so that one can reset the builder after create it from an existing
         * object.
         *
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder clearChannel() {
            mChannels.clear();
            return this;
        }

        /**
         * Build {@link BluetoothLeBroadcastSubgroup}.
         *
         * @return constructed {@link BluetoothLeBroadcastSubgroup}
         * @throws NullPointerException if {@link NonNull} items are null
         * @throws IllegalArgumentException if the object cannot be built
         * @hide
         */
        @SystemApi
        public @NonNull BluetoothLeBroadcastSubgroup build() {
            Objects.requireNonNull(mCodecSpecificConfig, "CodecSpecificConfig is null");
            Objects.requireNonNull(mContentMetadata, "ContentMetadata is null");
            if (mChannels.isEmpty()) {
                throw new IllegalArgumentException("Must have at least one channel");
            }
            return new BluetoothLeBroadcastSubgroup(mCodecId, mCodecSpecificConfig,
                    mContentMetadata, mChannels);
        }
    }
}
