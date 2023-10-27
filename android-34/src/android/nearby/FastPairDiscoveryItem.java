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
import android.nearby.aidl.FastPairDiscoveryItemParcel;

/**
 * Class for FastPairDiscoveryItem and its builder.
 *
 * @hide
 */
public class FastPairDiscoveryItem {

    FastPairDiscoveryItemParcel mMetadataParcel;

    FastPairDiscoveryItem(
            FastPairDiscoveryItemParcel metadataParcel) {
        this.mMetadataParcel = metadataParcel;
    }

    /**
     * Get Id.
     *
     * @hide
     */
    @Nullable
    public String getId() {
        return mMetadataParcel.id;
    }

    /**
     * Get MacAddress.
     *
     * @hide
     */
    @Nullable
    public String getMacAddress() {
        return mMetadataParcel.macAddress;
    }

    /**
     * Get ActionUrl.
     *
     * @hide
     */
    @Nullable
    public String getActionUrl() {
        return mMetadataParcel.actionUrl;
    }

    /**
     * Get DeviceName.
     *
     * @hide
     */
    @Nullable
    public String getDeviceName() {
        return mMetadataParcel.deviceName;
    }

    /**
     * Get Title.
     *
     * @hide
     */
    @Nullable
    public String getTitle() {
        return mMetadataParcel.title;
    }

    /**
     * Get Description.
     *
     * @hide
     */
    @Nullable
    public String getDescription() {
        return mMetadataParcel.description;
    }

    /**
     * Get DisplayUrl.
     *
     * @hide
     */
    @Nullable
    public String getDisplayUrl() {
        return mMetadataParcel.displayUrl;
    }

    /**
     * Get LastObservationTimestampMillis.
     *
     * @hide
     */
    public long getLastObservationTimestampMillis() {
        return mMetadataParcel.lastObservationTimestampMillis;
    }

    /**
     * Get FirstObservationTimestampMillis.
     *
     * @hide
     */
    public long getFirstObservationTimestampMillis() {
        return mMetadataParcel.firstObservationTimestampMillis;
    }

    /**
     * Get State.
     *
     * @hide
     */
    public int getState() {
        return mMetadataParcel.state;
    }

    /**
     * Get ActionUrlType.
     *
     * @hide
     */
    public int getActionUrlType() {
        return mMetadataParcel.actionUrlType;
    }

    /**
     * Get Rssi.
     *
     * @hide
     */
    public int getRssi() {
        return mMetadataParcel.rssi;
    }

    /**
     * Get PendingAppInstallTimestampMillis.
     *
     * @hide
     */
    public long getPendingAppInstallTimestampMillis() {
        return mMetadataParcel.pendingAppInstallTimestampMillis;
    }

    /**
     * Get TxPower.
     *
     * @hide
     */
    public int getTxPower() {
        return mMetadataParcel.txPower;
    }

    /**
     * Get AppName.
     *
     * @hide
     */
    @Nullable
    public String getAppName() {
        return mMetadataParcel.appName;
    }

    /**
     * Get PackageName.
     *
     * @hide
     */
    @Nullable
    public String getPackageName() {
        return mMetadataParcel.packageName;
    }

    /**
     * Get TriggerId.
     *
     * @hide
     */
    @Nullable
    public String getTriggerId() {
        return mMetadataParcel.triggerId;
    }

    /**
     * Get IconPng, which is submitted at device registration time to display on notification. It is
     * a 32-bit PNG with dimensions of 512px by 512px.
     *
     * @return IconPng in 32-bit PNG with dimensions of 512px by 512px.
     * @hide
     */
    @Nullable
    public byte[] getIconPng() {
        return mMetadataParcel.iconPng;
    }

    /**
     * Get IconFifeUrl.
     *
     * @hide
     */
    @Nullable
    public String getIconFfeUrl() {
        return mMetadataParcel.iconFifeUrl;
    }

    /**
     * Get authenticationPublicKeySecp256r1, which is same as AntiSpoof public key, see
     * <a href="https://developers.google.com/nearby/fast-pair/spec#data_format">Data Format</a>.
     *
     * @return 64-byte authenticationPublicKeySecp256r1.
     * @hide
     */
    @Nullable
    public byte[] getAuthenticationPublicKeySecp256r1() {
        return mMetadataParcel.authenticationPublicKeySecp256r1;
    }

    /**
     * Builder used to create FastPairDiscoveryItem.
     *
     * @hide
     */
    public static final class Builder {

        private final FastPairDiscoveryItemParcel mBuilderParcel;

        /**
         * Default constructor of Builder.
         *
         * @hide
         */
        public Builder() {
            mBuilderParcel = new FastPairDiscoveryItemParcel();
        }

        /**
         * Set Id.
         *
         * @param id Unique id.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         *
         * @hide
         */
        @NonNull
        public Builder setId(@Nullable String id) {
            mBuilderParcel.id = id;
            return this;
        }

        /**
         * Set MacAddress.
         *
         * @param macAddress Fast Pair device rotating mac address.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setMacAddress(@Nullable String macAddress) {
            mBuilderParcel.macAddress = macAddress;
            return this;
        }

        /**
         * Set ActionUrl.
         *
         * @param actionUrl Action Url of Fast Pair device.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setActionUrl(@Nullable String actionUrl) {
            mBuilderParcel.actionUrl = actionUrl;
            return this;
        }

        /**
         * Set DeviceName.
         * @param deviceName Fast Pair device name.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setDeviceName(@Nullable String deviceName) {
            mBuilderParcel.deviceName = deviceName;
            return this;
        }

        /**
         * Set Title.
         *
         * @param title Title of Fast Pair device.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setTitle(@Nullable String title) {
            mBuilderParcel.title = title;
            return this;
        }

        /**
         * Set Description.
         *
         * @param description Description of Fast Pair device.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setDescription(@Nullable String description) {
            mBuilderParcel.description = description;
            return this;
        }

        /**
         * Set DisplayUrl.
         *
         * @param displayUrl Display Url of Fast Pair device.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setDisplayUrl(@Nullable String displayUrl) {
            mBuilderParcel.displayUrl = displayUrl;
            return this;
        }

        /**
         * Set LastObservationTimestampMillis.
         *
         * @param lastObservationTimestampMillis Last observed timestamp of Fast Pair device, keyed
         *                                       by a rotating id.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setLastObservationTimestampMillis(
                long lastObservationTimestampMillis) {
            mBuilderParcel.lastObservationTimestampMillis = lastObservationTimestampMillis;
            return this;
        }

        /**
         * Set FirstObservationTimestampMillis.
         *
         * @param firstObservationTimestampMillis First observed timestamp of Fast Pair device,
         *                                        keyed by a rotating id.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setFirstObservationTimestampMillis(
                long firstObservationTimestampMillis) {
            mBuilderParcel.firstObservationTimestampMillis = firstObservationTimestampMillis;
            return this;
        }

        /**
         * Set State.
         *
         * @param state Item's current state. e.g. if the item is blocked.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setState(int state) {
            mBuilderParcel.state = state;
            return this;
        }

        /**
         * Set ActionUrlType.
         *
         * @param actionUrlType The resolved url type for the action_url.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setActionUrlType(int actionUrlType) {
            mBuilderParcel.actionUrlType = actionUrlType;
            return this;
        }

        /**
         * Set Rssi.
         *
         * @param rssi Beacon's RSSI value.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setRssi(int rssi) {
            mBuilderParcel.rssi = rssi;
            return this;
        }

        /**
         * Set PendingAppInstallTimestampMillis.
         *
         * @param pendingAppInstallTimestampMillis The timestamp when the user is redirected to App
         *                                         Store after clicking on the item.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setPendingAppInstallTimestampMillis(long pendingAppInstallTimestampMillis) {
            mBuilderParcel.pendingAppInstallTimestampMillis = pendingAppInstallTimestampMillis;
            return this;
        }

        /**
         * Set TxPower.
         *
         * @param txPower Beacon's tx power.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setTxPower(int txPower) {
            mBuilderParcel.txPower = txPower;
            return this;
        }

        /**
         * Set AppName.
         *
         * @param appName Human readable name of the app designated to open the uri.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setAppName(@Nullable String appName) {
            mBuilderParcel.appName = appName;
            return this;
        }

        /**
         * Set PackageName.
         *
         * @param packageName Package name of the App that owns this item.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setPackageName(@Nullable String packageName) {
            mBuilderParcel.packageName = packageName;
            return this;
        }

        /**
         * Set TriggerId.
         *
         * @param triggerId TriggerId identifies the trigger/beacon that is attached with a message.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setTriggerId(@Nullable String triggerId) {
            mBuilderParcel.triggerId = triggerId;
            return this;
        }

        /**
         * Set IconPng.
         *
         * @param iconPng Bytes of item icon in PNG format displayed in Discovery item list.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setIconPng(@Nullable byte[] iconPng) {
            mBuilderParcel.iconPng = iconPng;
            return this;
        }

        /**
         * Set IconFifeUrl.
         *
         * @param iconFifeUrl A FIFE URL of the item icon displayed in Discovery item list.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setIconFfeUrl(@Nullable String iconFifeUrl) {
            mBuilderParcel.iconFifeUrl = iconFifeUrl;
            return this;
        }

        /**
         * Set authenticationPublicKeySecp256r1, which is same as AntiSpoof public key, see
         * <a href="https://developers.google.com/nearby/fast-pair/spec#data_format">Data Format</a>
         *
         * @param authenticationPublicKeySecp256r1 64-byte Fast Pair device public key.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setAuthenticationPublicKeySecp256r1(
                @Nullable byte[] authenticationPublicKeySecp256r1) {
            mBuilderParcel.authenticationPublicKeySecp256r1 = authenticationPublicKeySecp256r1;
            return this;
        }

        /**
         * Build {@link FastPairDiscoveryItem} with the currently set configuration.
         *
         * @hide
         */
        @NonNull
        public FastPairDiscoveryItem build() {
            return new FastPairDiscoveryItem(mBuilderParcel);
        }
    }
}
