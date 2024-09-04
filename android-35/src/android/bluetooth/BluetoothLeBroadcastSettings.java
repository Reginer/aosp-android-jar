/*
 * Copyright 2023 The Android Open Source Project
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class contains the broadcast group settings information for this Broadcast Group.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcastSettings implements Parcelable {
    private final boolean mIsPublicBroadcast;
    private final String mBroadcastName;
    private final byte[] mBroadcastCode;
    private final BluetoothLeAudioContentMetadata mPublicBroadcastMetadata;
    private final List<BluetoothLeBroadcastSubgroupSettings> mSubgroupSettings;

    private BluetoothLeBroadcastSettings(
            boolean isPublicBroadcast,
            String broadcastName,
            byte[] broadcastCode,
            BluetoothLeAudioContentMetadata publicBroadcastMetadata,
            List<BluetoothLeBroadcastSubgroupSettings> subgroupSettings) {
        mIsPublicBroadcast = isPublicBroadcast;
        mBroadcastName = broadcastName;
        mBroadcastCode = broadcastCode;
        mPublicBroadcastMetadata = publicBroadcastMetadata;
        mSubgroupSettings = subgroupSettings;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof BluetoothLeBroadcastSettings)) {
            return false;
        }
        final BluetoothLeBroadcastSettings other = (BluetoothLeBroadcastSettings) o;
        return mIsPublicBroadcast == other.isPublicBroadcast()
                && Objects.equals(mBroadcastName, other.getBroadcastName())
                && Arrays.equals(mBroadcastCode, other.getBroadcastCode())
                && Objects.equals(mPublicBroadcastMetadata, other.getPublicBroadcastMetadata())
                && mSubgroupSettings.equals(other.getSubgroupSettings());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mIsPublicBroadcast,
                mBroadcastName,
                Arrays.hashCode(mBroadcastCode),
                mPublicBroadcastMetadata,
                mSubgroupSettings);
    }

    /**
     * Return {@code true} if this Broadcast Group is set to broadcast Public Broadcast Announcement
     * otherwise return {@code false}.
     *
     * @hide
     */
    @SystemApi
    public boolean isPublicBroadcast() {
        return mIsPublicBroadcast;
    }

    /**
     * Return the broadcast code for this Broadcast Group.
     *
     * @return Broadcast name for this Broadcast Group, null if no name provided
     * @hide
     */
    @SystemApi
    @Nullable
    public String getBroadcastName() {
        return mBroadcastName;
    }

    /**
     * Get the Broadcast Code currently set for this broadcast group.
     *
     * <p>Only needed when encryption is enabled
     *
     * <p>As defined in Volume 3, Part C, Section 3.2.6 of Bluetooth Core Specification, Version
     * 5.3, Broadcast Code is used to encrypt a broadcast audio stream.
     *
     * <p>It must be a UTF-8 string that has at least 4 octets and should not exceed 16 octets.
     *
     * @return Broadcast Code currently set for this broadcast group, null if code is not required
     *     or code is currently unknown
     * @hide
     */
    @SystemApi
    @Nullable
    public byte[] getBroadcastCode() {
        return mBroadcastCode;
    }

    /**
     * Get public broadcast metadata for this Broadcast Group.
     *
     * @return public broadcast metadata for this Broadcast Group, null if no public metadata exists
     * @hide
     */
    @SystemApi
    @Nullable
    public BluetoothLeAudioContentMetadata getPublicBroadcastMetadata() {
        return mPublicBroadcastMetadata;
    }

    /**
     * Get available subgroup settings in the broadcast group.
     *
     * @return list of subgroup settings in the broadcast group
     * @hide
     */
    @SystemApi
    @NonNull
    public List<BluetoothLeBroadcastSubgroupSettings> getSubgroupSettings() {
        return mSubgroupSettings;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeBoolean(mIsPublicBroadcast);
        out.writeString(mBroadcastName);
        if (mBroadcastCode != null) {
            out.writeInt(mBroadcastCode.length);
            out.writeByteArray(mBroadcastCode);
        } else {
            // -1 indicates missing broadcast code
            out.writeInt(-1);
        }
        out.writeTypedObject(mPublicBroadcastMetadata, 0);
        out.writeTypedList(mSubgroupSettings);
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeBroadcastSettings} from parcel.
     *
     * @hide
     */
    @SystemApi @NonNull
    public static final Creator<BluetoothLeBroadcastSettings> CREATOR =
            new Creator<>() {
                public @NonNull BluetoothLeBroadcastSettings createFromParcel(@NonNull Parcel in) {
                    Builder builder = new Builder();
                    builder.setPublicBroadcast(in.readBoolean());
                    builder.setBroadcastName(in.readString());
                    final int codeLen = in.readInt();
                    byte[] broadcastCode = null;
                    if (codeLen != -1) {
                        broadcastCode = new byte[codeLen];
                        if (codeLen >= 0) {
                            in.readByteArray(broadcastCode);
                        }
                    }
                    builder.setBroadcastCode(broadcastCode);
                    builder.setPublicBroadcastMetadata(
                            in.readTypedObject(BluetoothLeAudioContentMetadata.CREATOR));
                    final List<BluetoothLeBroadcastSubgroupSettings> subgroupSettings =
                            new ArrayList<>();
                    in.readTypedList(
                            subgroupSettings, BluetoothLeBroadcastSubgroupSettings.CREATOR);
                    for (BluetoothLeBroadcastSubgroupSettings setting : subgroupSettings) {
                        builder.addSubgroupSettings(setting);
                    }
                    return builder.build();
                }

                public @NonNull BluetoothLeBroadcastSettings[] newArray(int size) {
                    return new BluetoothLeBroadcastSettings[size];
                }
            };

    /**
     * Builder for {@link BluetoothLeBroadcastSettings}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private boolean mIsPublicBroadcast = false;
        private String mBroadcastName = null;
        private byte[] mBroadcastCode = null;
        private BluetoothLeAudioContentMetadata mPublicBroadcastMetadata = null;
        private List<BluetoothLeBroadcastSubgroupSettings> mSubgroupSettings = new ArrayList<>();

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
        public Builder(@NonNull BluetoothLeBroadcastSettings original) {
            mIsPublicBroadcast = original.isPublicBroadcast();
            mBroadcastName = original.getBroadcastName();
            mBroadcastCode = original.getBroadcastCode();
            mPublicBroadcastMetadata = original.getPublicBroadcastMetadata();
            mSubgroupSettings = original.getSubgroupSettings();
        }

        /**
         * Set whether the Public Broadcast is on for this broadcast group.
         *
         * @param isPublicBroadcast whether the Public Broadcast is enabled
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setPublicBroadcast(boolean isPublicBroadcast) {
            mIsPublicBroadcast = isPublicBroadcast;
            return this;
        }

        /**
         * Set broadcast name for the broadcast group.
         *
         * <p>As defined in Public Broadcast Profile V1.0, section 5.1. Broadcast_Name AD Type is a
         * UTF-8 encoded string containing a minimum of 4 characters and a maximum of 32
         * human-readable characters.
         *
         * @param broadcastName Broadcast name for this broadcast group, null if no name provided
         * @throws IllegalArgumentException if name is non-null and its length is less than 4
         *     characters or greater than 32 characters
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setBroadcastName(@Nullable String broadcastName) {
            if (broadcastName != null
                    && ((broadcastName.length() > 32) || (broadcastName.length() < 4))) {
                throw new IllegalArgumentException("Invalid broadcast name length");
            }
            mBroadcastName = broadcastName;
            return this;
        }

        /**
         * Set the Broadcast Code currently set for this broadcast group.
         *
         * <p>Only needed when encryption is enabled As defined in Volume 3, Part C, Section 3.2.6
         * of Bluetooth Core Specification, Version 5.3, Broadcast Code is used to encrypt a
         * broadcast audio stream. It must be a UTF-8 string that has at least 4 octets and should
         * not exceed 16 octets.
         *
         * @param broadcastCode Broadcast Code for this broadcast group, null if code is not
         *     required for non-encrypted broadcast
         * @throws IllegalArgumentException if name is non-null and its length is less than 4
         *     characters or greater than 16 characters
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setBroadcastCode(@Nullable byte[] broadcastCode) {
            if (broadcastCode != null
                    && ((broadcastCode.length > 16) || (broadcastCode.length < 4))) {
                throw new IllegalArgumentException("Invalid broadcast code length");
            }
            mBroadcastCode = broadcastCode;
            return this;
        }

        /**
         * Set public broadcast metadata for this Broadcast Group. PBS should include the
         * Program_Info length-type-value (LTV) structure metadata
         *
         * @param publicBroadcastMetadata public broadcast metadata for this Broadcast Group, null
         *     if no public meta data provided
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
         * Add a subgroup settings to the broadcast group.
         *
         * @param subgroupSettings contains subgroup's setting data
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder addSubgroupSettings(
                @NonNull BluetoothLeBroadcastSubgroupSettings subgroupSettings) {
            Objects.requireNonNull(subgroupSettings, "subgroupSettings cannot be null");
            mSubgroupSettings.add(subgroupSettings);
            return this;
        }

        /**
         * Clear subgroup settings list so that one can reset the builder
         *
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder clearSubgroupSettings() {
            mSubgroupSettings.clear();
            return this;
        }

        /**
         * Build {@link BluetoothLeBroadcastSettings}.
         *
         * @return {@link BluetoothLeBroadcastSettings}
         * @throws IllegalArgumentException if the object cannot be built
         * @hide
         */
        @SystemApi
        @NonNull
        public BluetoothLeBroadcastSettings build() {
            if (mSubgroupSettings.isEmpty()) {
                throw new IllegalArgumentException("Must contain at least one subgroup");
            }
            return new BluetoothLeBroadcastSettings(
                    mIsPublicBroadcast,
                    mBroadcastName,
                    mBroadcastCode,
                    mPublicBroadcastMetadata,
                    mSubgroupSettings);
        }
    }
}
