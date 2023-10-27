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
import android.nearby.aidl.FastPairDeviceMetadataParcel;

/**
 * Class for the properties of a given type of Fast Pair device, including images and text.
 *
 * @hide
 */
public class FastPairDeviceMetadata {

    FastPairDeviceMetadataParcel mMetadataParcel;

    FastPairDeviceMetadata(
            FastPairDeviceMetadataParcel metadataParcel) {
        this.mMetadataParcel = metadataParcel;
    }

    /**
     * Get ImageUrl, which will be displayed in notification.
     *
     * @hide
     */
    @Nullable
    public String getImageUrl() {
        return mMetadataParcel.imageUrl;
    }

    /**
     * Get IntentUri, which will be launched to install companion app.
     *
     * @hide
     */
    @Nullable
    public String getIntentUri() {
        return mMetadataParcel.intentUri;
    }

    /**
     * Get BLE transmit power, as described in Fast Pair spec, see
     * <a href="https://developers.google.com/nearby/fast-pair/spec#transmit_power">Transmit Power</a>
     *
     * @hide
     */
    public int getBleTxPower() {
        return mMetadataParcel.bleTxPower;
    }

    /**
     * Get Fast Pair Half Sheet trigger distance in meters.
     *
     * @hide
     */
    public float getTriggerDistance() {
        return mMetadataParcel.triggerDistance;
    }

    /**
     * Get Fast Pair device image, which is submitted at device registration time to display on
     * notification. It is a 32-bit PNG with dimensions of 512px by 512px.
     *
     * @return Fast Pair device image in 32-bit PNG with dimensions of 512px by 512px.
     * @hide
     */
    @Nullable
    public byte[] getImage() {
        return mMetadataParcel.image;
    }

    /**
     * Get Fast Pair device type.
     * DEVICE_TYPE_UNSPECIFIED = 0;
     * HEADPHONES = 1;
     * TRUE_WIRELESS_HEADPHONES = 7;
     * @hide
     */
    public int getDeviceType() {
        return mMetadataParcel.deviceType;
    }

    /**
     * Get Fast Pair device name. e.g., "Pixel Buds A-Series".
     *
     * @hide
     */
    @Nullable
    public String getName() {
        return mMetadataParcel.name;
    }

    /**
     * Get true wireless image url for left bud.
     *
     * @hide
     */
    @Nullable
    public String getTrueWirelessImageUrlLeftBud() {
        return mMetadataParcel.trueWirelessImageUrlLeftBud;
    }

    /**
     * Get true wireless image url for right bud.
     *
     * @hide
     */
    @Nullable
    public String getTrueWirelessImageUrlRightBud() {
        return mMetadataParcel.trueWirelessImageUrlRightBud;
    }

    /**
     * Get true wireless image url for case.
     *
     * @hide
     */
    @Nullable
    public String getTrueWirelessImageUrlCase() {
        return mMetadataParcel.trueWirelessImageUrlCase;
    }

    /**
     * Get InitialNotificationDescription, which is a translated string of
     * "Tap to pair. Earbuds will be tied to %s" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getInitialNotificationDescription() {
        return mMetadataParcel.initialNotificationDescription;
    }

    /**
     * Get InitialNotificationDescriptionNoAccount, which is a translated string of
     * "Tap to pair with this device" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getInitialNotificationDescriptionNoAccount() {
        return mMetadataParcel.initialNotificationDescriptionNoAccount;
    }

    /**
     * Get OpenCompanionAppDescription, which is a translated string of
     * "Tap to finish setup" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getOpenCompanionAppDescription() {
        return mMetadataParcel.openCompanionAppDescription;
    }

    /**
     * Get UpdateCompanionAppDescription, which is a translated string of
     * "Tap to update device settings and finish setup" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getUpdateCompanionAppDescription() {
        return mMetadataParcel.updateCompanionAppDescription;
    }

    /**
     * Get DownloadCompanionAppDescription, which is a translated string of
     * "Tap to download device app on Google Play and see all features" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getDownloadCompanionAppDescription() {
        return mMetadataParcel.downloadCompanionAppDescription;
    }

    /**
     * Get UnableToConnectTitle, which is a translated string of
     * "Unable to connect" based on locale.
     */
    @Nullable
    public String getUnableToConnectTitle() {
        return mMetadataParcel.unableToConnectTitle;
    }

    /**
     * Get UnableToConnectDescription, which is a translated string of
     * "Try manually pairing to the device" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getUnableToConnectDescription() {
        return mMetadataParcel.unableToConnectDescription;
    }

    /**
     * Get InitialPairingDescription, which is a translated string of
     * "%s will appear on devices linked with %s" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getInitialPairingDescription() {
        return mMetadataParcel.initialPairingDescription;
    }

    /**
     * Get ConnectSuccessCompanionAppInstalled, which is a translated string of
     * "Your device is ready to be set up" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getConnectSuccessCompanionAppInstalled() {
        return mMetadataParcel.connectSuccessCompanionAppInstalled;
    }

    /**
     * Get ConnectSuccessCompanionAppNotInstalled, which is a translated string of
     * "Download the device app on Google Play to see all available features" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getConnectSuccessCompanionAppNotInstalled() {
        return mMetadataParcel.connectSuccessCompanionAppNotInstalled;
    }

    /**
     * Get SubsequentPairingDescription, which is a translated string of
     * "Connect %s to this phone" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getSubsequentPairingDescription() {
        return mMetadataParcel.subsequentPairingDescription;
    }

    /**
     * Get RetroactivePairingDescription, which is a translated string of
     * "Save device to %s for faster pairing to your other devices" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getRetroactivePairingDescription() {
        return mMetadataParcel.retroactivePairingDescription;
    }

    /**
     * Get WaitLaunchCompanionAppDescription, which is a translated string of
     * "This will take a few moments" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getWaitLaunchCompanionAppDescription() {
        return mMetadataParcel.waitLaunchCompanionAppDescription;
    }

    /**
     * Get FailConnectGoToSettingsDescription, which is a translated string of
     * "Try manually pairing to the device by going to Settings" based on locale.
     *
     * @hide
     */
    @Nullable
    public String getFailConnectGoToSettingsDescription() {
        return mMetadataParcel.failConnectGoToSettingsDescription;
    }

    /**
     * Builder used to create FastPairDeviceMetadata.
     *
     * @hide
     */
    public static final class Builder {

        private final FastPairDeviceMetadataParcel mBuilderParcel;

        /**
         * Default constructor of Builder.
         *
         * @hide
         */
        public Builder() {
            mBuilderParcel = new FastPairDeviceMetadataParcel();
            mBuilderParcel.imageUrl = null;
            mBuilderParcel.intentUri = null;
            mBuilderParcel.name = null;
            mBuilderParcel.bleTxPower = 0;
            mBuilderParcel.triggerDistance = 0;
            mBuilderParcel.image = null;
            mBuilderParcel.deviceType = 0;  // DEVICE_TYPE_UNSPECIFIED
            mBuilderParcel.trueWirelessImageUrlLeftBud = null;
            mBuilderParcel.trueWirelessImageUrlRightBud = null;
            mBuilderParcel.trueWirelessImageUrlCase = null;
            mBuilderParcel.initialNotificationDescription = null;
            mBuilderParcel.initialNotificationDescriptionNoAccount = null;
            mBuilderParcel.openCompanionAppDescription = null;
            mBuilderParcel.updateCompanionAppDescription = null;
            mBuilderParcel.downloadCompanionAppDescription = null;
            mBuilderParcel.unableToConnectTitle = null;
            mBuilderParcel.unableToConnectDescription = null;
            mBuilderParcel.initialPairingDescription = null;
            mBuilderParcel.connectSuccessCompanionAppInstalled = null;
            mBuilderParcel.connectSuccessCompanionAppNotInstalled = null;
            mBuilderParcel.subsequentPairingDescription = null;
            mBuilderParcel.retroactivePairingDescription = null;
            mBuilderParcel.waitLaunchCompanionAppDescription = null;
            mBuilderParcel.failConnectGoToSettingsDescription = null;
        }

        /**
         * Set ImageUlr.
         *
         * @param imageUrl Image Ulr.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setImageUrl(@Nullable String imageUrl) {
            mBuilderParcel.imageUrl = imageUrl;
            return this;
        }

        /**
         * Set IntentUri.
         *
         * @param intentUri Intent uri.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setIntentUri(@Nullable String intentUri) {
            mBuilderParcel.intentUri = intentUri;
            return this;
        }

        /**
         * Set device name.
         *
         * @param name Device name.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setName(@Nullable String name) {
            mBuilderParcel.name = name;
            return this;
        }

        /**
         * Set ble transmission power.
         *
         * @param bleTxPower Ble transmission power.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setBleTxPower(int bleTxPower) {
            mBuilderParcel.bleTxPower = bleTxPower;
            return this;
        }

        /**
         * Set trigger distance.
         *
         * @param triggerDistance Fast Pair trigger distance.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setTriggerDistance(float triggerDistance) {
            mBuilderParcel.triggerDistance = triggerDistance;
            return this;
        }

        /**
         * Set image.
         *
         * @param image Fast Pair device image, which is submitted at device registration time to
         *              display on notification. It is a 32-bit PNG with dimensions of
         *              512px by 512px.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setImage(@Nullable byte[] image) {
            mBuilderParcel.image = image;
            return this;
        }

        /**
         * Set device type.
         *
         * @param deviceType Fast Pair device type.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setDeviceType(int deviceType) {
            mBuilderParcel.deviceType = deviceType;
            return this;
        }

        /**
         * Set true wireless image url for left bud.
         *
         * @param trueWirelessImageUrlLeftBud True wireless image url for left bud.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setTrueWirelessImageUrlLeftBud(
                @Nullable String trueWirelessImageUrlLeftBud) {
            mBuilderParcel.trueWirelessImageUrlLeftBud = trueWirelessImageUrlLeftBud;
            return this;
        }

        /**
         * Set true wireless image url for right bud.
         *
         * @param trueWirelessImageUrlRightBud True wireless image url for right bud.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setTrueWirelessImageUrlRightBud(
                @Nullable String trueWirelessImageUrlRightBud) {
            mBuilderParcel.trueWirelessImageUrlRightBud = trueWirelessImageUrlRightBud;
            return this;
        }

        /**
         * Set true wireless image url for case.
         *
         * @param trueWirelessImageUrlCase True wireless image url for case.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setTrueWirelessImageUrlCase(@Nullable String trueWirelessImageUrlCase) {
            mBuilderParcel.trueWirelessImageUrlCase = trueWirelessImageUrlCase;
            return this;
        }

        /**
         * Set InitialNotificationDescription.
         *
         * @param initialNotificationDescription Initial notification description.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setInitialNotificationDescription(
                @Nullable String initialNotificationDescription) {
            mBuilderParcel.initialNotificationDescription = initialNotificationDescription;
            return this;
        }

        /**
         * Set InitialNotificationDescriptionNoAccount.
         *
         * @param initialNotificationDescriptionNoAccount Initial notification description when
         *                                                account is not present.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setInitialNotificationDescriptionNoAccount(
                @Nullable String initialNotificationDescriptionNoAccount) {
            mBuilderParcel.initialNotificationDescriptionNoAccount =
                    initialNotificationDescriptionNoAccount;
            return this;
        }

        /**
         * Set OpenCompanionAppDescription.
         *
         * @param openCompanionAppDescription Description for opening companion app.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setOpenCompanionAppDescription(
                @Nullable String openCompanionAppDescription) {
            mBuilderParcel.openCompanionAppDescription = openCompanionAppDescription;
            return this;
        }

        /**
         * Set UpdateCompanionAppDescription.
         *
         * @param updateCompanionAppDescription Description for updating companion app.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setUpdateCompanionAppDescription(
                @Nullable String updateCompanionAppDescription) {
            mBuilderParcel.updateCompanionAppDescription = updateCompanionAppDescription;
            return this;
        }

        /**
         * Set DownloadCompanionAppDescription.
         *
         * @param downloadCompanionAppDescription Description for downloading companion app.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setDownloadCompanionAppDescription(
                @Nullable String downloadCompanionAppDescription) {
            mBuilderParcel.downloadCompanionAppDescription = downloadCompanionAppDescription;
            return this;
        }

        /**
         * Set UnableToConnectTitle.
         *
         * @param unableToConnectTitle Title when Fast Pair device is unable to be connected to.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setUnableToConnectTitle(@Nullable String unableToConnectTitle) {
            mBuilderParcel.unableToConnectTitle = unableToConnectTitle;
            return this;
        }

        /**
         * Set UnableToConnectDescription.
         *
         * @param unableToConnectDescription Description when Fast Pair device is unable to be
         *                                   connected to.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setUnableToConnectDescription(
                @Nullable String unableToConnectDescription) {
            mBuilderParcel.unableToConnectDescription = unableToConnectDescription;
            return this;
        }

        /**
         * Set InitialPairingDescription.
         *
         * @param initialPairingDescription Description for Fast Pair initial pairing.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setInitialPairingDescription(@Nullable String initialPairingDescription) {
            mBuilderParcel.initialPairingDescription = initialPairingDescription;
            return this;
        }

        /**
         * Set ConnectSuccessCompanionAppInstalled.
         *
         * @param connectSuccessCompanionAppInstalled Description that let user open the companion
         *                                            app.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setConnectSuccessCompanionAppInstalled(
                @Nullable String connectSuccessCompanionAppInstalled) {
            mBuilderParcel.connectSuccessCompanionAppInstalled =
                    connectSuccessCompanionAppInstalled;
            return this;
        }

        /**
         * Set ConnectSuccessCompanionAppNotInstalled.
         *
         * @param connectSuccessCompanionAppNotInstalled Description that let user download the
         *                                               companion app.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setConnectSuccessCompanionAppNotInstalled(
                @Nullable String connectSuccessCompanionAppNotInstalled) {
            mBuilderParcel.connectSuccessCompanionAppNotInstalled =
                    connectSuccessCompanionAppNotInstalled;
            return this;
        }

        /**
         * Set SubsequentPairingDescription.
         *
         * @param subsequentPairingDescription Description that reminds user there is a paired
         *                                     device nearby.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setSubsequentPairingDescription(
                @Nullable String subsequentPairingDescription) {
            mBuilderParcel.subsequentPairingDescription = subsequentPairingDescription;
            return this;
        }

        /**
         * Set RetroactivePairingDescription.
         *
         * @param retroactivePairingDescription Description that reminds users opt in their device.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setRetroactivePairingDescription(
                @Nullable String retroactivePairingDescription) {
            mBuilderParcel.retroactivePairingDescription = retroactivePairingDescription;
            return this;
        }

        /**
         * Set WaitLaunchCompanionAppDescription.
         *
         * @param waitLaunchCompanionAppDescription Description that indicates companion app is
         *                                          about to launch.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setWaitLaunchCompanionAppDescription(
                @Nullable String waitLaunchCompanionAppDescription) {
            mBuilderParcel.waitLaunchCompanionAppDescription =
                    waitLaunchCompanionAppDescription;
            return this;
        }

        /**
         * Set FailConnectGoToSettingsDescription.
         *
         * @param failConnectGoToSettingsDescription Description that indicates go to bluetooth
         *                                           settings when connection fail.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setFailConnectGoToSettingsDescription(
                @Nullable String failConnectGoToSettingsDescription) {
            mBuilderParcel.failConnectGoToSettingsDescription =
                    failConnectGoToSettingsDescription;
            return this;
        }

        /**
         * Build {@link FastPairDeviceMetadata} with the currently set configuration.
         *
         * @hide
         */
        @NonNull
        public FastPairDeviceMetadata build() {
            return new FastPairDeviceMetadata(mBuilderParcel);
        }
    }
}
