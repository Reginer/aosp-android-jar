/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.nearby;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.nearby.aidl.FastPairAccountKeyDeviceMetadataParcel;

/**
 * Class for metadata of a Fast Pair device associated with an account.
 *
 * @hide
 */
public class FastPairAccountKeyDeviceMetadata {

    FastPairAccountKeyDeviceMetadataParcel mMetadataParcel;

    FastPairAccountKeyDeviceMetadata(FastPairAccountKeyDeviceMetadataParcel metadataParcel) {
        this.mMetadataParcel = metadataParcel;
    }

    /**
     * Get Device Account Key, which uniquely identifies a Fast Pair device associated with an
     * account. AccountKey is 16 bytes: first byte is 0x04. Other 15 bytes are randomly generated.
     *
     * @return 16-byte Account Key.
     * @hide
     */
    @Nullable
    public byte[] getDeviceAccountKey() {
        return mMetadataParcel.deviceAccountKey;
    }

    /**
     * Get a hash value of device's account key and public bluetooth address without revealing the
     * public bluetooth address. Sha256 hash value is 32 bytes.
     *
     * @return 32-byte Sha256 hash value.
     * @hide
     */
    @Nullable
    public byte[] getSha256DeviceAccountKeyPublicAddress() {
        return mMetadataParcel.sha256DeviceAccountKeyPublicAddress;
    }

    /**
     * Get metadata of a Fast Pair device type.
     *
     * @hide
     */
    @Nullable
    public FastPairDeviceMetadata getFastPairDeviceMetadata() {
        if (mMetadataParcel.metadata == null) {
            return null;
        }
        return new FastPairDeviceMetadata(mMetadataParcel.metadata);
    }

    /**
     * Get Fast Pair discovery item, which is tied to both the device type and the account.
     *
     * @hide
     */
    @Nullable
    public FastPairDiscoveryItem getFastPairDiscoveryItem() {
        if (mMetadataParcel.discoveryItem == null) {
            return null;
        }
        return new FastPairDiscoveryItem(mMetadataParcel.discoveryItem);
    }

    /**
     * Builder used to create FastPairAccountKeyDeviceMetadata.
     *
     * @hide
     */
    public static final class Builder {

        private final FastPairAccountKeyDeviceMetadataParcel mBuilderParcel;

        /**
         * Default constructor of Builder.
         *
         * @hide
         */
        public Builder() {
            mBuilderParcel = new FastPairAccountKeyDeviceMetadataParcel();
            mBuilderParcel.deviceAccountKey = null;
            mBuilderParcel.sha256DeviceAccountKeyPublicAddress = null;
            mBuilderParcel.metadata = null;
            mBuilderParcel.discoveryItem = null;
        }

        /**
         * Set Account Key.
         *
         * @param deviceAccountKey Fast Pair device account key, which is 16 bytes: first byte is
         *                         0x04. Next 15 bytes are randomly generated.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setDeviceAccountKey(@Nullable byte[] deviceAccountKey) {
            mBuilderParcel.deviceAccountKey = deviceAccountKey;
            return this;
        }

        /**
         * Set sha256 hash value of account key and public bluetooth address.
         *
         * @param sha256DeviceAccountKeyPublicAddress 32-byte sha256 hash value of account key and
         *                                            public bluetooth address.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setSha256DeviceAccountKeyPublicAddress(
                @Nullable byte[] sha256DeviceAccountKeyPublicAddress) {
            mBuilderParcel.sha256DeviceAccountKeyPublicAddress =
                    sha256DeviceAccountKeyPublicAddress;
            return this;
        }


        /**
         * Set Fast Pair metadata.
         *
         * @param metadata Fast Pair metadata.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setFastPairDeviceMetadata(@Nullable FastPairDeviceMetadata metadata) {
            if (metadata == null) {
                mBuilderParcel.metadata = null;
            } else {
                mBuilderParcel.metadata = metadata.mMetadataParcel;
            }
            return this;
        }

        /**
         * Set Fast Pair discovery item.
         *
         * @param discoveryItem Fast Pair discovery item.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setFastPairDiscoveryItem(@Nullable FastPairDiscoveryItem discoveryItem) {
            if (discoveryItem == null) {
                mBuilderParcel.discoveryItem = null;
            } else {
                mBuilderParcel.discoveryItem = discoveryItem.mMetadataParcel;
            }
            return this;
        }

        /**
         * Build {@link FastPairAccountKeyDeviceMetadata} with the currently set configuration.
         *
         * @hide
         */
        @NonNull
        public FastPairAccountKeyDeviceMetadata build() {
            return new FastPairAccountKeyDeviceMetadata(mBuilderParcel);
        }
    }
}
