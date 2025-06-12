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

package android.nearby;

import android.annotation.IntDef;
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
 * Represents a Presence device from nearby scans.
 *
 * @hide
 */
@SystemApi
public final class PresenceDevice extends NearbyDevice implements Parcelable {

    /** The type of presence device. */
    /** @hide **/
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DeviceType.UNKNOWN,
            DeviceType.PHONE,
            DeviceType.TABLET,
            DeviceType.DISPLAY,
            DeviceType.LAPTOP,
            DeviceType.TV,
            DeviceType.WATCH,
    })
    public @interface DeviceType {
        /** The type of the device is unknown. */
        int UNKNOWN = 0;
        /** The device is a phone. */
        int PHONE = 1;
        /** The device is a tablet. */
        int TABLET = 2;
        /** The device is a display. */
        int DISPLAY = 3;
        /** The device is a laptop. */
        int LAPTOP = 4;
        /** The device is a TV. */
        int TV = 5;
        /** The device is a watch. */
        int WATCH = 6;
    }

    private final String mDeviceId;
    private final byte[] mSalt;
    private final byte[] mSecretId;
    private final byte[] mEncryptedIdentity;
    private final int mDeviceType;
    private final String mDeviceImageUrl;
    private final long mDiscoveryTimestampMillis;
    private final List<DataElement> mExtendedProperties;

    /**
     * The id of the device.
     *
     * <p>This id is not a hardware id. It may rotate based on the remote device's broadcasts.
     */
    @NonNull
    public String getDeviceId() {
        return mDeviceId;
    }

    /**
     * Returns the salt used when presence device is discovered.
     */
    @NonNull
    public byte[] getSalt() {
        return mSalt;
    }

    /**
     * Returns the secret used when presence device is discovered.
     */
    @NonNull
    public byte[] getSecretId() {
        return mSecretId;
    }

    /**
     * Returns the encrypted identity used when presence device is discovered.
     */
    @NonNull
    public byte[] getEncryptedIdentity() {
        return mEncryptedIdentity;
    }

    /** The type of the device. */
    @DeviceType
    public int getDeviceType() {
        return mDeviceType;
    }

    /** An image URL representing the device. */
    @Nullable
    public String getDeviceImageUrl() {
        return mDeviceImageUrl;
    }

    /** The timestamp (since boot) when the device is discovered. */
    public long getDiscoveryTimestampMillis() {
        return mDiscoveryTimestampMillis;
    }

    /**
     * The extended properties of the device.
     */
    @NonNull
    public List<DataElement> getExtendedProperties() {
        return mExtendedProperties;
    }

    /**
     * This can only be hidden because this is the System API,
     * which cannot be changed in T timeline.
     *
     * @hide
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof PresenceDevice) {
            PresenceDevice otherDevice = (PresenceDevice) other;
            if (super.equals(otherDevice)) {
                return Arrays.equals(mSalt, otherDevice.mSalt)
                        && Arrays.equals(mSecretId, otherDevice.mSecretId)
                        && Arrays.equals(mEncryptedIdentity, otherDevice.mEncryptedIdentity)
                        && Objects.equals(mDeviceId, otherDevice.mDeviceId)
                        && mDeviceType == otherDevice.mDeviceType
                        && Objects.equals(mDeviceImageUrl, otherDevice.mDeviceImageUrl)
                        && mDiscoveryTimestampMillis == otherDevice.mDiscoveryTimestampMillis
                        && Objects.equals(mExtendedProperties, otherDevice.mExtendedProperties);
            }
        }
        return false;
    }

    /**
     * This can only be hidden because this is the System API,
     * which cannot be changed in T timeline.
     *
     * @hide
     *
     * @return The unique hash value of the {@link PresenceDevice}
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                getMediums(),
                getRssi(),
                Arrays.hashCode(mSalt),
                Arrays.hashCode(mSecretId),
                Arrays.hashCode(mEncryptedIdentity),
                mDeviceId,
                mDeviceType,
                mDeviceImageUrl,
                mDiscoveryTimestampMillis,
                mExtendedProperties);
    }

    private PresenceDevice(String deviceName, List<Integer> mMediums, int rssi, String deviceId,
            byte[] salt, byte[] secretId, byte[] encryptedIdentity, int deviceType,
            String deviceImageUrl, long discoveryTimestampMillis,
            List<DataElement> extendedProperties) {
        super(deviceName, mMediums, rssi);
        mDeviceId = deviceId;
        mSalt = salt;
        mSecretId = secretId;
        mEncryptedIdentity = encryptedIdentity;
        mDeviceType = deviceType;
        mDeviceImageUrl = deviceImageUrl;
        mDiscoveryTimestampMillis = discoveryTimestampMillis;
        mExtendedProperties = extendedProperties;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        String name = getName();
        dest.writeInt(name == null ? 0 : 1);
        if (name != null) {
            dest.writeString(name);
        }
        List<Integer> mediums = getMediums();
        dest.writeInt(mediums.size());
        for (int medium : mediums) {
            dest.writeInt(medium);
        }
        dest.writeInt(getRssi());
        dest.writeInt(mSalt.length);
        dest.writeByteArray(mSalt);
        dest.writeInt(mSecretId.length);
        dest.writeByteArray(mSecretId);
        dest.writeInt(mEncryptedIdentity.length);
        dest.writeByteArray(mEncryptedIdentity);
        dest.writeString(mDeviceId);
        dest.writeInt(mDeviceType);
        dest.writeInt(mDeviceImageUrl == null ? 0 : 1);
        if (mDeviceImageUrl != null) {
            dest.writeString(mDeviceImageUrl);
        }
        dest.writeLong(mDiscoveryTimestampMillis);
        dest.writeInt(mExtendedProperties.size());
        for (DataElement dataElement : mExtendedProperties) {
            dest.writeParcelable(dataElement, 0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<PresenceDevice> CREATOR = new Creator<PresenceDevice>() {
        @Override
        public PresenceDevice createFromParcel(Parcel in) {
            String name = null;
            if (in.readInt() == 1) {
                name = in.readString();
            }
            int size = in.readInt();
            List<Integer> mediums = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                mediums.add(in.readInt());
            }
            int rssi = in.readInt();
            byte[] salt = new byte[in.readInt()];
            in.readByteArray(salt);
            byte[] secretId = new byte[in.readInt()];
            in.readByteArray(secretId);
            byte[] encryptedIdentity = new byte[in.readInt()];
            in.readByteArray(encryptedIdentity);
            String deviceId = in.readString();
            int deviceType = in.readInt();
            String deviceImageUrl = null;
            if (in.readInt() == 1) {
                deviceImageUrl = in.readString();
            }
            long discoveryTimeMillis = in.readLong();
            int dataElementSize = in.readInt();
            List<DataElement> dataElements = new ArrayList<>();
            for (int i = 0; i < dataElementSize; i++) {
                dataElements.add(
                        in.readParcelable(DataElement.class.getClassLoader(), DataElement.class));
            }
            Builder builder = new Builder(deviceId, salt, secretId, encryptedIdentity)
                    .setName(name)
                    .setRssi(rssi)
                    .setDeviceType(deviceType)
                    .setDeviceImageUrl(deviceImageUrl)
                    .setDiscoveryTimestampMillis(discoveryTimeMillis);
            for (int i = 0; i < mediums.size(); i++) {
                builder.addMedium(mediums.get(i));
            }
            for (int i = 0; i < dataElements.size(); i++) {
                builder.addExtendedProperty(dataElements.get(i));
            }
            return builder.build();
        }

        @Override
        public PresenceDevice[] newArray(int size) {
            return new PresenceDevice[size];
        }
    };

    /**
     * Builder class for {@link PresenceDevice}.
     */
    public static final class Builder {

        private final List<DataElement> mExtendedProperties;
        private final List<Integer> mMediums;
        private final String mDeviceId;
        private final byte[] mSalt;
        private final byte[] mSecretId;
        private final byte[] mEncryptedIdentity;

        private String mName;
        private int mRssi;
        private int mDeviceType;
        private String mDeviceImageUrl;
        private long mDiscoveryTimestampMillis;

        /**
         * Constructs a {@link Builder}.
         *
         * @param deviceId the identifier on the discovered Presence device
         * @param salt a random salt used in the beacon from the Presence device.
         * @param secretId a secret identifier used in the beacon from the Presence device.
         * @param encryptedIdentity the identity associated with the Presence device.
         */
        public Builder(@NonNull String deviceId, @NonNull byte[] salt, @NonNull byte[] secretId,
                @NonNull byte[] encryptedIdentity) {
            Objects.requireNonNull(deviceId);
            Objects.requireNonNull(salt);
            Objects.requireNonNull(secretId);
            Objects.requireNonNull(encryptedIdentity);

            mDeviceId = deviceId;
            mSalt = salt;
            mSecretId = secretId;
            mEncryptedIdentity = encryptedIdentity;
            mMediums = new ArrayList<>();
            mExtendedProperties = new ArrayList<>();
            mRssi = -127;
        }

        /**
         * Sets the name of the Presence device.
         *
         * @param name Name of the Presence. Can be {@code null} if there is no name.
         */
        @NonNull
        public Builder setName(@Nullable String name) {
            mName = name;
            return this;
        }

        /**
         * Adds the medium over which the Presence device is discovered.
         *
         * @param medium The {@link Medium} over which the device is discovered.
         */
        @NonNull
        public Builder addMedium(@Medium int medium) {
            mMediums.add(medium);
            return this;
        }

        /**
         * Sets the RSSI on the discovered Presence device.
         *
         * @param rssi The received signal strength in dBm.
         */
        @NonNull
        public Builder setRssi(int rssi) {
            mRssi = rssi;
            return this;
        }

        /**
         * Sets the type of discovered Presence device.
         *
         * @param deviceType Type of the Presence device.
         */
        @NonNull
        public Builder setDeviceType(@DeviceType int deviceType) {
            mDeviceType = deviceType;
            return this;
        }

        /**
         * Sets the image url of the discovered Presence device.
         *
         * @param deviceImageUrl Url of the image for the Presence device.
         */
        @NonNull
        public Builder setDeviceImageUrl(@Nullable String deviceImageUrl) {
            mDeviceImageUrl = deviceImageUrl;
            return this;
        }

        /**
         * Sets discovery timestamp, the clock is based on elapsed time.
         *
         * @param discoveryTimestampMillis Timestamp when the presence device is discovered.
         */
        @NonNull
        public Builder setDiscoveryTimestampMillis(long discoveryTimestampMillis) {
            mDiscoveryTimestampMillis = discoveryTimestampMillis;
            return this;
        }

        /**
         * Adds an extended property of the discovered presence device.
         *
         * @param dataElement Data element of the extended property.
         */
        @NonNull
        public Builder addExtendedProperty(@NonNull DataElement dataElement) {
            Objects.requireNonNull(dataElement);
            mExtendedProperties.add(dataElement);
            return this;
        }

        /**
         * Builds a Presence device.
         */
        @NonNull
        public PresenceDevice build() {
            return new PresenceDevice(mName, mMediums, mRssi, mDeviceId,
                    mSalt, mSecretId, mEncryptedIdentity,
                    mDeviceType,
                    mDeviceImageUrl,
                    mDiscoveryTimestampMillis, mExtendedProperties);
        }
    }
}
