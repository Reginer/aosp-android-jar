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

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a Broadcast Source group and the associated information that is needed
 * by Broadcast Audio Scan Service (BASS) to set up a Broadcast Sink.
 *
 * <p>For example, an LE Audio Broadcast Sink can use the information contained within an instance
 * of this class to synchronize with an LE Audio Broadcast group in order to listen to audio from
 * Broadcast subgroup using one or more Broadcast Channels.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcastMetadata implements Parcelable {
    // Information needed for adding broadcast Source

    // Optional: Identity address type
    private final @BluetoothDevice.AddressType int mSourceAddressType;
    // Optional: Must use identity address
    private final BluetoothDevice mSourceDevice;
    private final int mSourceAdvertisingSid;
    private final int mBroadcastId;
    private final int mPaSyncInterval;
    private final boolean mIsEncrypted;
    private final boolean mIsPublicBroadcast;
    private final String mBroadcastName;
    private final byte[] mBroadcastCode;
    private final BluetoothLeAudioContentMetadata mPublicBroadcastMetadata;
    private final @AudioConfigQuality int mAudioConfigQuality;

    /**
     * Audio configuration quality for this Broadcast Group.
     * This quality bitmap is used for presenting the audio stream quality for this BIG,
     * either public broadcast or non-public broadcast
     * Bit0 indicates at least one broadcast Audio Stream configuration is standard quality
     * Bit1 indicates at least one broadcast Audio Stream configuration is high quality
     *
     * @hide
     */
    @IntDef(flag = true, prefix = "AUDIO_CONFIG_QUALITY_",
            value = {
            AUDIO_CONFIG_QUALITY_NONE,
            AUDIO_CONFIG_QUALITY_STANDARD,
            AUDIO_CONFIG_QUALITY_HIGH,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioConfigQuality {}

    /**
     * Audio config quality is none, default value used for audio config quality.
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_CONFIG_QUALITY_NONE = 0;

    /**
     * Audio config quality is standard.
     * This indicates the BIG shall include at least one broadcast Audio Stream
     * configuration defined as Mandatory for a Broadcast Sink in
     * Basic Audio Profile, Version 1 or later, table 6.4
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_CONFIG_QUALITY_STANDARD = 0x1 << 0;

    /**
     * Audio config quality is standard.
     * This indicates the BIG shall include at least one broadcast Audio Stream
     * configuration setting listed in
     * Public Broadcast Profile, Version 1 or later, table 4.2
     *
     * @hide
     */
    @SystemApi
    public static final int AUDIO_CONFIG_QUALITY_HIGH = 0x1 << 1;

    // BASE structure

    // See Section 7 for description. Range: 0x000000 – 0xFFFFFF Units: μs
    //All other values: RFU
    private final int mPresentationDelayMicros;
    // Number of subgroups used to group BISes present in the BIG
    //Shall be at least 1, as defined by Rule 1
    // Sub group info numSubGroup = mSubGroups.length
    private final List<BluetoothLeBroadcastSubgroup> mSubgroups;

    private BluetoothLeBroadcastMetadata(int sourceAddressType,
            BluetoothDevice sourceDevice, int sourceAdvertisingSid, int broadcastId,
            int paSyncInterval, boolean isEncrypted, boolean isPublicBroadcast,
            String broadcastName, byte[] broadcastCode, int presentationDelay,
            @AudioConfigQuality int audioConfigQuality,
            BluetoothLeAudioContentMetadata publicBroadcastMetadata,
            List<BluetoothLeBroadcastSubgroup> subgroups) {
        mSourceAddressType = sourceAddressType;
        mSourceDevice = sourceDevice;
        mSourceAdvertisingSid = sourceAdvertisingSid;
        mBroadcastId = broadcastId;
        mPaSyncInterval = paSyncInterval;
        mIsEncrypted = isEncrypted;
        mIsPublicBroadcast = isPublicBroadcast;
        mBroadcastName = broadcastName;
        mBroadcastCode = broadcastCode;
        mPresentationDelayMicros = presentationDelay;
        mAudioConfigQuality = audioConfigQuality;
        mPublicBroadcastMetadata = publicBroadcastMetadata;
        mSubgroups = subgroups;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof BluetoothLeBroadcastMetadata)) {
            return false;
        }
        final BluetoothLeBroadcastMetadata other = (BluetoothLeBroadcastMetadata) o;
        return mSourceAddressType == other.getSourceAddressType()
                && mSourceDevice.equals(other.getSourceDevice())
                && mSourceAdvertisingSid == other.getSourceAdvertisingSid()
                && mBroadcastId == other.getBroadcastId()
                && mPaSyncInterval == other.getPaSyncInterval()
                && mIsEncrypted == other.isEncrypted()
                && mIsPublicBroadcast == other.isPublicBroadcast()
                && Objects.equals(mBroadcastName, other.getBroadcastName())
                && Arrays.equals(mBroadcastCode, other.getBroadcastCode())
                && mPresentationDelayMicros == other.getPresentationDelayMicros()
                && mAudioConfigQuality == other.getAudioConfigQuality()
                && Objects.equals(mPublicBroadcastMetadata, other.getPublicBroadcastMetadata())
                && mSubgroups.equals(other.getSubgroups());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSourceAddressType, mSourceDevice, mSourceAdvertisingSid,
                mBroadcastId, mPaSyncInterval, mIsEncrypted, mIsPublicBroadcast,
                mBroadcastName, Arrays.hashCode(mBroadcastCode),
                mPresentationDelayMicros, mAudioConfigQuality, mPublicBroadcastMetadata,
                mSubgroups);
    }

    /**
     * Get the address type of the Broadcast Source.
     *
     * Can be either {@link BluetoothDevice#ADDRESS_TYPE_PUBLIC},
     * {@link BluetoothDevice#ADDRESS_TYPE_RANDOM}
     *
     * @return address type of the Broadcast Source
     * @hide
     */
    @SystemApi
    public @BluetoothDevice.AddressType int getSourceAddressType() {
        return mSourceAddressType;
    }

    /**
     * Get the MAC address of the Broadcast Source, which can be Public Device Address,
     * Random Device Address, Public Identity Address or Random (static) Identity Address.
     *
     * @return MAC address of the Broadcast Source
     * @hide
     */
    @SystemApi
    public @NonNull BluetoothDevice getSourceDevice() {
        return mSourceDevice;
    }

    /**
     * Get Advertising_SID subfield of the ADI field of the AUX_ADV_IND PDU or the
     * LL_PERIODIC_SYNC_IND containing the SyncInfo that points to the PA transmitted by the
     * Broadcast Source.
     *
     * @return 1-byte long Advertising_SID of the Broadcast Source
     * @hide
     */
    @SystemApi
    public int getSourceAdvertisingSid() {
        return mSourceAdvertisingSid;
    }

    /**
     * Broadcast_ID of the Broadcast Source.
     *
     * @return 3-byte long Broadcast_ID of the Broadcast Source
     * @hide
     */
    @SystemApi
    public int getBroadcastId() {
        return mBroadcastId;
    }

    /**
     * Indicated that Periodic Advertising Sync interval is unknown.
     * @hide
     */
    @SystemApi
    public static final int PA_SYNC_INTERVAL_UNKNOWN = 0xFFFF;

    /**
     * Get Periodic Advertising Sync interval of the broadcast Source.
     *
     * @return Periodic Advertising Sync interval of the broadcast Source,
     * {@link #PA_SYNC_INTERVAL_UNKNOWN} if unknown
     * @hide
     */
    @SystemApi
    public int getPaSyncInterval() {
        return mPaSyncInterval;
    }

    /**
     * Return true if the Broadcast Source is encrypted.
     *
     * @return true if the Broadcast Source is encrypted
     * @hide
     */
    @SystemApi
    public boolean isEncrypted() {
        return mIsEncrypted;
    }

    /**
     * Return {@code true} if this Broadcast Group is broadcasting Public Broadcast Announcement
     * otherwise return {@code false}.
     *
     * @hide
     */
    @SystemApi
    public boolean isPublicBroadcast() {
        return mIsPublicBroadcast;
    }

    /**
     * Get the broadcast name for this Broadcast Group as UTF-8 format.
     *
     * @return broadcast name or null for this Broadcast Group
     * @hide
     */
    @SystemApi
    public @Nullable String getBroadcastName() {
        return mBroadcastName;
    }

    /**
     * Get the Broadcast Code currently set for this Broadcast Source.
     *
     * Only needed when encryption is enabled
     *
     * <p>As defined in Volume 3, Part C, Section 3.2.6 of Bluetooth Core Specification, Version
     * 5.3, Broadcast Code is used to encrypt a broadcast audio stream.
     * <p>It must be a UTF-8 string that has at least 4 octets and should not exceed 16 octets.
     *
     * @return Broadcast Code currently set for this Broadcast Source,
     * {@code null} if code is not required
     *         or code is currently unknown
     * @hide
     */
    @SystemApi
    public @Nullable byte[] getBroadcastCode() {
        return mBroadcastCode;
    }

    /**
     * Get the overall presentation delay in microseconds of this Broadcast Source.
     *
     * Presentation delay is defined in Section 7 of the Basic Audio Profile.
     *
     * @return presentation delay of this Broadcast Source in microseconds
     * @hide
     */
    @SystemApi
    public @IntRange(from = 0, to = 0xFFFFFF) int getPresentationDelayMicros() {
        return mPresentationDelayMicros;
    }

    /**
     * Get broadcast audio config quality for this Broadcast Group.
     *
     * @return Broadcast audio config quality for this Broadcast Group
     * @hide
     */
    @SystemApi
    public @AudioConfigQuality int getAudioConfigQuality() {
        return mAudioConfigQuality;
    }

    /**
     * Get public broadcast metadata for this Broadcast Group.
     *
     * @return public broadcast metadata for this Broadcast Group,
     * {@code null} if no public metadata exists
     * @hide
     */
    @SystemApi
    public @Nullable BluetoothLeAudioContentMetadata getPublicBroadcastMetadata() {
        return mPublicBroadcastMetadata;
    }

    /**
     * Get available subgroups in this broadcast source.
     *
     * @return list of subgroups in this broadcast source, which should contain at least one
     *         subgroup for each Broadcast Source
     * @hide
     */
    @SystemApi
    public @NonNull List<BluetoothLeBroadcastSubgroup> getSubgroups() {
        return mSubgroups;
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
        out.writeInt(mSourceAddressType);
        if (mSourceDevice != null) {
            out.writeInt(1);
            out.writeTypedObject(mSourceDevice, 0);
        } else {
            // zero indicates missing mSourceDevice
            out.writeInt(0);
        }
        out.writeInt(mSourceAdvertisingSid);
        out.writeInt(mBroadcastId);
        out.writeInt(mPaSyncInterval);
        out.writeBoolean(mIsEncrypted);
        if (mBroadcastCode != null) {
            out.writeInt(mBroadcastCode.length);
            out.writeByteArray(mBroadcastCode);
        } else {
            // -1 indicates missing broadcast code
            out.writeInt(-1);
        }
        out.writeInt(mPresentationDelayMicros);
        out.writeTypedList(mSubgroups);
        out.writeBoolean(mIsPublicBroadcast);
        out.writeString(mBroadcastName);
        out.writeInt(mAudioConfigQuality);
        out.writeTypedObject(mPublicBroadcastMetadata, 0);
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeBroadcastMetadata} from parcel.
     *
     * @hide
     */
    @SystemApi
    @NonNull
    public static final Creator<BluetoothLeBroadcastMetadata> CREATOR = new Creator<>() {
        public @NonNull BluetoothLeBroadcastMetadata createFromParcel(@NonNull Parcel in) {
            Builder builder = new Builder();
            final int sourceAddressType = in.readInt();
            final int deviceExist = in.readInt();
            BluetoothDevice sourceDevice = null;
            if (deviceExist == 1) {
                sourceDevice = in.readTypedObject(BluetoothDevice.CREATOR);
            }
            builder.setSourceDevice(sourceDevice, sourceAddressType);
            builder.setSourceAdvertisingSid(in.readInt());
            builder.setBroadcastId(in.readInt());
            builder.setPaSyncInterval(in.readInt());
            builder.setEncrypted(in.readBoolean());
            final int codeLen = in.readInt();
            byte[] broadcastCode = null;
            if (codeLen != -1) {
                broadcastCode = new byte[codeLen];
                if (codeLen > 0) {
                    in.readByteArray(broadcastCode);
                }
            }
            builder.setBroadcastCode(broadcastCode);
            builder.setPresentationDelayMicros(in.readInt());
            final List<BluetoothLeBroadcastSubgroup> subgroups = new ArrayList<>();
            in.readTypedList(subgroups, BluetoothLeBroadcastSubgroup.CREATOR);
            for (BluetoothLeBroadcastSubgroup subgroup : subgroups) {
                builder.addSubgroup(subgroup);
            }
            builder.setPublicBroadcast(in.readBoolean());
            builder.setBroadcastName(in.readString());
            builder.setAudioConfigQuality(in.readInt());
            builder.setPublicBroadcastMetadata(
                    in.readTypedObject(BluetoothLeAudioContentMetadata.CREATOR));
            return builder.build();
        }

        public @NonNull BluetoothLeBroadcastMetadata[] newArray(int size) {
            return new BluetoothLeBroadcastMetadata[size];
        }
    };

    private static final int UNKNOWN_VALUE_PLACEHOLDER = -1;

    /**
     * Builder for {@link BluetoothLeBroadcastMetadata}.
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private @BluetoothDevice.AddressType int mSourceAddressType =
                BluetoothDevice.ADDRESS_TYPE_UNKNOWN;
        private BluetoothDevice mSourceDevice = null;
        private int mSourceAdvertisingSid = UNKNOWN_VALUE_PLACEHOLDER;
        private int mBroadcastId = UNKNOWN_VALUE_PLACEHOLDER;
        private int mPaSyncInterval = UNKNOWN_VALUE_PLACEHOLDER;
        private boolean mIsEncrypted = false;
        private boolean mIsPublicBroadcast = false;
        private String mBroadcastName = null;
        private byte[] mBroadcastCode = null;
        private int mPresentationDelayMicros = UNKNOWN_VALUE_PLACEHOLDER;
        private @AudioConfigQuality int mAudioConfigQuality = AUDIO_CONFIG_QUALITY_NONE;
        private BluetoothLeAudioContentMetadata mPublicBroadcastMetadata = null;
        private List<BluetoothLeBroadcastSubgroup> mSubgroups = new ArrayList<>();

        /**
         * Create an empty builder.
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
        public Builder(@NonNull BluetoothLeBroadcastMetadata original) {
            mSourceAddressType = original.getSourceAddressType();
            mSourceDevice = original.getSourceDevice();
            mSourceAdvertisingSid = original.getSourceAdvertisingSid();
            mBroadcastId = original.getBroadcastId();
            mPaSyncInterval = original.getPaSyncInterval();
            mIsEncrypted = original.isEncrypted();
            mIsPublicBroadcast = original.isPublicBroadcast();
            mBroadcastName = original.getBroadcastName();
            mBroadcastCode = original.getBroadcastCode();
            mPresentationDelayMicros = original.getPresentationDelayMicros();
            mAudioConfigQuality = original.getAudioConfigQuality();
            mPublicBroadcastMetadata = original.getPublicBroadcastMetadata();
            mSubgroups = original.getSubgroups();
        }


        /**
         * Set the address type and MAC address of the Broadcast Source.
         *
         * Address type can be either {@link BluetoothDevice#ADDRESS_TYPE_PUBLIC},
         * {@link BluetoothDevice#ADDRESS_TYPE_RANDOM}
         *
         * MAC address can be Public Device Address, Random Device Address, Public Identity Address
         * or Random (static) Identity Address
         *
         * @param sourceDevice source advertiser address
         * @param sourceAddressType source advertiser address type
         * @throws IllegalArgumentException if sourceAddressType is invalid
         * @throws NullPointerException if sourceDevice is null
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setSourceDevice(@NonNull BluetoothDevice sourceDevice,
                @BluetoothDevice.AddressType int sourceAddressType) {
            if (sourceAddressType == BluetoothDevice.ADDRESS_TYPE_UNKNOWN) {
                throw new IllegalArgumentException(
                        "sourceAddressType cannot be ADDRESS_TYPE_UNKNOWN");
            }
            if (sourceAddressType != BluetoothDevice.ADDRESS_TYPE_RANDOM
                    && sourceAddressType != BluetoothDevice.ADDRESS_TYPE_PUBLIC) {
                throw new IllegalArgumentException("sourceAddressType " + sourceAddressType
                        + " is invalid");
            }
            Objects.requireNonNull(sourceDevice, "sourceDevice cannot be null");
            mSourceAddressType = sourceAddressType;
            mSourceDevice = sourceDevice;
            return this;
        }

        /**
         * Set Advertising_SID that is a subfield of the ADI field of the AUX_ADV_IND PDU or the
         * LL_PERIODIC_SYNC_IND containing the SyncInfo that points to the PA transmitted by the
         * Broadcast Source.
         *
         * @param sourceAdvertisingSid 1-byte long Advertising_SID of the Broadcast Source
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setSourceAdvertisingSid(int sourceAdvertisingSid) {
            mSourceAdvertisingSid = sourceAdvertisingSid;
            return this;
        }

        /**
         * Set the Broadcast_ID of the Broadcast Source.
         *
         * @param broadcastId 3-byte long Broadcast_ID of the Broadcast Source
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setBroadcastId(int broadcastId) {
            mBroadcastId = broadcastId;
            return this;
        }

        /**
         * Set Periodic Advertising Sync interval of the broadcast Source.
         *
         * @param paSyncInterval Periodic Advertising Sync interval of the broadcast Source,
         *                      {@link #PA_SYNC_INTERVAL_UNKNOWN} if unknown
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setPaSyncInterval(int paSyncInterval) {
            mPaSyncInterval = paSyncInterval;
            return this;
        }

        /**
         * Set whether the Broadcast Source should be encrypted.
         *
         * When setting up a Broadcast Source, if <var>isEncrypted</var> is true while
         * <var>broadcastCode</var> is null, the implementation will automatically generate
         * a Broadcast Code
         *
         * @param isEncrypted whether the Broadcast Source is encrypted
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setEncrypted(boolean isEncrypted) {
            mIsEncrypted = isEncrypted;
            return this;
        }

        /**
         * Set whether this Broadcast Group is broadcasting Public Broadcast Announcement.
         *
         * @param isPublicBroadcast whether this Broadcast Group is broadcasting
         *                          Public Broadcast Announcement
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setPublicBroadcast(boolean isPublicBroadcast) {
            mIsPublicBroadcast = isPublicBroadcast;
            return this;
        }

        /**
         * Set broadcast name for this Broadcast Group.
         *
         * @param broadcastName Broadcast name for this Broadcast Group,
         * {@code null} if no name provided
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setBroadcastName(@Nullable String broadcastName) {
            mBroadcastName = broadcastName;
            return this;
        }

        /**
         * Set the Broadcast Code currently set for this broadcast group.
         *
         * Only needed when encryption is enabled
         *
         * <p>As defined in Volume 3, Part C, Section 3.2.6 of Bluetooth Core Specification, Version
         * 5.3, Broadcast Code is used to encrypt a broadcast audio stream.
         * <p>It must be a UTF-8 string that has at least 4 octets and should not exceed 16 octets.
         *
         * @param broadcastCode Broadcast Code for this Broadcast Source,
         * {@code null} if code is not required
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setBroadcastCode(@Nullable byte[] broadcastCode) {
            mBroadcastCode = broadcastCode;
            return this;
        }

        /**
         * Set the overall presentation delay in microseconds of this Broadcast Source.
         *
         * Presentation delay is defined in Section 7 of the Basic Audio Profile.
         *
         * @param presentationDelayMicros presentation delay of this Broadcast Source in
         *                                microseconds
         * @throws IllegalArgumentException if presentationDelayMicros does not fall in
         *                                  [0, 0xFFFFFF]
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setPresentationDelayMicros(
                @IntRange(from = 0, to = 0xFFFFFF) int presentationDelayMicros) {
            if (presentationDelayMicros < 0 || presentationDelayMicros >= 0xFFFFFF) {
                throw new IllegalArgumentException("presentationDelayMicros "
                        + presentationDelayMicros + " does not fall in [0, 0xFFFFFF]");
            }
            mPresentationDelayMicros = presentationDelayMicros;
            return this;
        }

        /**
         * Set broadcast audio config quality for this Broadcast Group.
         *
         * @param  audioConfigQuality broadcast audio config quality for this Broadcast Group
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setAudioConfigQuality(@AudioConfigQuality int audioConfigQuality) {
            mAudioConfigQuality = audioConfigQuality;
            return this;
        }

        /**
         * Set public broadcast metadata for this Broadcast Group.
         * PBS should include the Program_Info length-type-value (LTV) structure metadata
         *
         * @param  publicBroadcastMetadata public broadcast metadata for this Broadcast Group,
                                           {@code null} if no public meta data provided
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setPublicBroadcastMetadata(
                @Nullable BluetoothLeAudioContentMetadata publicBroadcastMetadata) {
            mPublicBroadcastMetadata = publicBroadcastMetadata;
            return this;
        }

        /**
         * Add a subgroup to this broadcast source.
         *
         * @param subgroup {@link BluetoothLeBroadcastSubgroup} that contains a subgroup's metadata
         * @throws NullPointerException if subgroup is null
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder addSubgroup(@NonNull BluetoothLeBroadcastSubgroup subgroup) {
            Objects.requireNonNull(subgroup, "subgroup cannot be null");
            mSubgroups.add(subgroup);
            return this;
        }

        /**
         * Clear subgroup list so that one can reset the builder after create it from an existing
         * object.
         *
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder clearSubgroup() {
            mSubgroups.clear();
            return this;
        }

        /**
         * Build {@link BluetoothLeBroadcastMetadata}.
         *
         * @return {@link BluetoothLeBroadcastMetadata}
         * @throws IllegalArgumentException if the object cannot be built
         * @throws NullPointerException if {@link NonNull} items are null
         * @hide
         */
        @SystemApi
        public @NonNull BluetoothLeBroadcastMetadata build() {
            if (mSourceAddressType == BluetoothDevice.ADDRESS_TYPE_UNKNOWN) {
                throw new IllegalArgumentException("SourceAddressTyp cannot be unknown");
            }
            if (mSourceAddressType != BluetoothDevice.ADDRESS_TYPE_RANDOM
                    && mSourceAddressType != BluetoothDevice.ADDRESS_TYPE_PUBLIC) {
                throw new IllegalArgumentException("sourceAddressType " + mSourceAddressType
                        + " is invalid");
            }
            Objects.requireNonNull(mSourceDevice, "mSourceDevice cannot be null");
            if (mSubgroups.isEmpty()) {
                throw new IllegalArgumentException("Must contain at least one subgroup");
            }
            return new BluetoothLeBroadcastMetadata(mSourceAddressType, mSourceDevice,
                    mSourceAdvertisingSid, mBroadcastId, mPaSyncInterval, mIsEncrypted,
                    mIsPublicBroadcast, mBroadcastName, mBroadcastCode,
                    mPresentationDelayMicros, mAudioConfigQuality,
                    mPublicBroadcastMetadata, mSubgroups);
        }
    }
}
