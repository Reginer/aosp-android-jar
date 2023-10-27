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
import android.nearby.aidl.FastPairAntispoofKeyDeviceMetadataParcel;

/**
 * Class for a type of registered Fast Pair device keyed by modelID, or antispoofKey.
 *
 * @hide
 */
public class FastPairAntispoofKeyDeviceMetadata {

    FastPairAntispoofKeyDeviceMetadataParcel mMetadataParcel;
    FastPairAntispoofKeyDeviceMetadata(
            FastPairAntispoofKeyDeviceMetadataParcel metadataParcel) {
        this.mMetadataParcel = metadataParcel;
    }

    /**
     * Get Antispoof public key.
     *
     * @hide
     */
    @Nullable
    public byte[] getAntispoofPublicKey() {
        return this.mMetadataParcel.antispoofPublicKey;
    }

    /**
     * Get metadata of a Fast Pair device type.
     *
     * @hide
     */
    @Nullable
    public FastPairDeviceMetadata getFastPairDeviceMetadata() {
        if (this.mMetadataParcel.deviceMetadata == null) {
            return null;
        }
        return new FastPairDeviceMetadata(this.mMetadataParcel.deviceMetadata);
    }

    /**
     * Builder used to create FastPairAntispoofkeyDeviceMetadata.
     *
     * @hide
     */
    public static final class Builder {

        private final FastPairAntispoofKeyDeviceMetadataParcel mBuilderParcel;

        /**
         * Default constructor of Builder.
         *
         * @hide
         */
        public Builder() {
            mBuilderParcel = new FastPairAntispoofKeyDeviceMetadataParcel();
            mBuilderParcel.antispoofPublicKey = null;
            mBuilderParcel.deviceMetadata = null;
        }

        /**
         * Set AntiSpoof public key, which uniquely identify a Fast Pair device type.
         *
         * @param antispoofPublicKey is 64 bytes, see <a href="https://developers.google.com/nearby/fast-pair/spec#data_format">Data Format</a>.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setAntispoofPublicKey(@Nullable byte[] antispoofPublicKey) {
            mBuilderParcel.antispoofPublicKey = antispoofPublicKey;
            return this;
        }

        /**
         * Set Fast Pair metadata, which is the property of a Fast Pair device type, including
         * device images and strings.
         *
         * @param metadata Fast Pair device meta data.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setFastPairDeviceMetadata(@Nullable FastPairDeviceMetadata metadata) {
            if (metadata != null) {
                mBuilderParcel.deviceMetadata = metadata.mMetadataParcel;
            } else {
                mBuilderParcel.deviceMetadata = null;
            }
            return this;
        }

        /**
         * Build {@link FastPairAntispoofKeyDeviceMetadata} with the currently set configuration.
         *
         * @hide
         */
        @NonNull
        public FastPairAntispoofKeyDeviceMetadata build() {
            return new FastPairAntispoofKeyDeviceMetadata(mBuilderParcel);
        }
    }
}
