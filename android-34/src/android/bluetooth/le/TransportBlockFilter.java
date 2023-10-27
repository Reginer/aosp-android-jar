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

package android.bluetooth.le;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothAssignedNumbers.OrganizationId;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Wrapper for filter input for Transport Discovery Data Transport Blocks.
 * This class represents the filter for a Transport Block from a Transport Discovery Data
 * advertisement data.
 *
 * @see ScanFilter
 * @hide
 */
@SystemApi
public final class TransportBlockFilter implements Parcelable {

    private final int mOrgId;
    private final int mTdsFlags;
    private final int mTdsFlagsMask;
    private final byte[] mTransportData;
    private final byte[] mTransportDataMask;
    private final byte[] mWifiNanHash;

    /**
     * Length of a Wi-FI NAN hash in bytes/
     * @hide
     */
    @SystemApi
    public static final int WIFI_NAN_HASH_LENGTH_BYTES = 8;

    private TransportBlockFilter(int orgId, int tdsFlags, int tdsFlagsMask,
            @Nullable byte[] transportData, @Nullable byte[] transportDataMask,
            @Nullable byte[] wifiNanHash) {
        if (orgId < 1) {
            throw new IllegalArgumentException("invalid organization id " + orgId);
        }
        if (tdsFlags == -1) {
            throw new IllegalArgumentException("tdsFlag is invalid");
        }
        if (tdsFlagsMask == -1) {
            throw new IllegalArgumentException("tdsFlagsMask is invalid");
        }
        if (orgId == OrganizationId.WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING) {
            if (transportData != null || transportDataMask != null) {
                throw new IllegalArgumentException(
                        "wifiNanHash should be used instead of transportData and/or "
                                + "transportDataMask when orgId is "
                                + "WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING");
            }
            if (wifiNanHash != null && wifiNanHash.length != WIFI_NAN_HASH_LENGTH_BYTES) {
                throw new IllegalArgumentException(
                        "wifiNanHash should be WIFI_NAN_HASH_LENGTH_BYTES long, but the input is "
                                + wifiNanHash.length + " bytes");
            }
        } else {
            if (wifiNanHash != null) {
                throw new IllegalArgumentException("wifiNanHash should not be used when orgId is "
                        + "not WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING");
            }
        }
        mOrgId = orgId;
        mTdsFlags = tdsFlags;
        mTdsFlagsMask = tdsFlagsMask;
        mTransportData = transportData;
        mTransportDataMask = transportDataMask;
        mWifiNanHash = wifiNanHash;
    }

    /**
     * Get Organization ID assigned by Bluetooth SIG. For more details refer to Transport Discovery
     * Service Organization IDs in
     * <a href="https://www.bluetooth.com/specifications/assigned-numbers/">Bluetooth Assigned Numbers</a>
     * @hide
     */
    @SystemApi
    public int getOrgId() {
        return mOrgId;
    }


    /**
     * Get Transport Discovery Service (TDS) flags to filter Transport Discovery Blocks
     *
     * @hide
     */
    @SystemApi
    public int getTdsFlags() {
        return mTdsFlags;
    }

    /**
     * Get masks for filtering Transport Discovery Service (TDS) flags in Transport Discovery Blocks
     *
     * @return a bitmask to select which bits in {@code tdsFlag} to match. 0 means no bit in
     * tdsFlags will be used for matching
     * @hide
     */
    @SystemApi
    public int getTdsFlagsMask() {
        return mTdsFlagsMask;
    }

    /**
     * Get data to filter Transport Discovery Blocks.
     *
     * Cannot be used when {@code orgId} is {@link OrganizationId
     * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
     *
     * @return Data to filter Transport Discovery Blocks, null if not used
     * @hide
     */
    @SystemApi
    @Nullable
    public byte[] getTransportData() {
        return mTransportData;
    }

    /**
     * Get masks for filtering data in Transport Discovery Blocks.
     *
     * Cannot be used when {@code orgId} is
     * {@link OrganizationId#WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
     *
     * @return a byte array with matching length to {@code transportData} to
     * select which bit to use in filter, null is not used
     * @hide
     */
    @SystemApi
    @Nullable
    public byte[] getTransportDataMask() {
        return mTransportDataMask;
    }

    /**
     * Get hashed bloom filter value to filter
     * {@link OrganizationId#WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING} services in Transport
     * Discovery Blocks.
     *
     * Can only be used when {@code orgId} is
     * {@link OrganizationId#WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}.
     *
     * @return 8 octets Wi-Fi NAN defined bloom filter hash, null if not used
     * @hide
     */
    @SystemApi
    @Nullable
    public byte[] getWifiNanHash() {
        return mWifiNanHash;
    }

    /**
     * Check if a scan result matches this transport block filter.
     *
     * @param scanResult scan result to match
     * @return true if matches
     * @hide
     */
    boolean matches(ScanResult scanResult) {
        ScanRecord scanRecord = scanResult.getScanRecord();
        // Transport Discovery data match
        TransportDiscoveryData transportDiscoveryData = scanRecord.getTransportDiscoveryData();

        if ((transportDiscoveryData != null)) {
            for (TransportBlock transportBlock : transportDiscoveryData.getTransportBlocks()) {
                int orgId = transportBlock.getOrgId();
                int tdsFlags =  transportBlock.getTdsFlags();
                int transportDataLength = transportBlock.getTransportDataLength();
                byte[] transportData = transportBlock.getTransportData();

                if (mOrgId != orgId) {
                    continue;
                }
                if ((mTdsFlags & mTdsFlagsMask) != (tdsFlags & mTdsFlagsMask)) {
                    continue;
                }
                if ((mOrgId != OrganizationId.WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING)
                        && (mTransportData != null) && (mTransportDataMask != null)) {
                    if (transportDataLength != 0) {
                        if (!ScanFilter.matchesPartialData(
                                mTransportData, mTransportDataMask, transportData)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
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
    @SystemApi
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mOrgId);
        dest.writeInt(mTdsFlags);
        dest.writeInt(mTdsFlagsMask);
        dest.writeInt(mTransportData == null ? 0 : 1);
        if (mTransportData != null) {
            dest.writeInt(mTransportData.length);
            dest.writeByteArray(mTransportData);
            dest.writeInt(mTransportDataMask == null ? 0 : 1);
            if (mTransportDataMask != null) {
                dest.writeInt(mTransportDataMask.length);
                dest.writeByteArray(mTransportDataMask);
            }
        }
        dest.writeInt(mWifiNanHash == null ? 0 : 1);
        if (mWifiNanHash != null) {
            dest.writeInt(mWifiNanHash.length);
            dest.writeByteArray(mWifiNanHash);
        }
    }

    /**
     * Get a human-readable string for this object.
     */
    @Override
    public String toString() {
        return "TransportBlockFilter [mOrgId=" + mOrgId + ", mTdsFlags=" + mTdsFlags
                + ", mTdsFlagsMask=" + mTdsFlagsMask + ", mTransportData="
                + Arrays.toString(mTransportData) + ", mTransportDataMask="
                + Arrays.toString(mTransportDataMask) + ", mWifiNanHash="
                + Arrays.toString(mWifiNanHash) + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mOrgId, mTdsFlags, mTdsFlagsMask, Arrays.hashCode(mTransportData),
                Arrays.hashCode(mTransportDataMask), Arrays.hashCode(mWifiNanHash));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TransportBlockFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final TransportBlockFilter other = (TransportBlockFilter) obj;
        return mOrgId == other.getOrgId()
                && mTdsFlags == other.getTdsFlags()
                && mTdsFlagsMask == other.getTdsFlagsMask()
                && Arrays.equals(mTransportData, other.getTransportData())
                && Arrays.equals(mTransportDataMask, other.getTransportDataMask())
                && Arrays.equals(mWifiNanHash, other.getWifiNanHash());
    }

    /**
     * Creator for {@link TransportBlockFilter} so that we can create it from {@link Parcel}.
     * @hide
     */
    @SystemApi
    @NonNull
    public static final Creator<TransportBlockFilter> CREATOR = new Creator<>() {
        @Override
        public TransportBlockFilter createFromParcel(Parcel source) {
            final int orgId = source.readInt();
            Builder builder = new Builder(orgId);
            builder.setTdsFlags(source.readInt(), source.readInt());
            if (source.readInt() == 1) {
                int transportDataLength = source.readInt();
                byte[] transportData = new byte[transportDataLength];
                source.readByteArray(transportData);
                byte[] transportDataMask = null;
                if (source.readInt() == 1) {
                    int transportDataMaskLength = source.readInt();
                    transportDataMask = new byte[transportDataMaskLength];
                    source.readByteArray(transportDataMask);
                }
                builder.setTransportData(transportData, transportDataMask);
            }
            if (source.readInt() == 1) {
                int wifiNanHashLength = source.readInt();
                byte[] wifiNanHash = new byte[wifiNanHashLength];
                source.readByteArray(wifiNanHash);
                builder.setWifiNanHash(wifiNanHash);
            }
            return builder.build();
        }

        @Override
        public TransportBlockFilter[] newArray(int size) {
            return new TransportBlockFilter[0];
        }
    };

    /**
     * Builder class for {@link TransportBlockFilter}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {

        private final int mOrgId;
        private int mTdsFlags = 0;
        private int mTdsFlagsMask = 0;
        private byte[] mTransportData = null;
        private byte[] mTransportDataMask = null;
        private byte[] mWifiNanHash = null;

        /**
         * Builder for {@link TransportBlockFilter}.
         *
         * @param orgId Organization ID assigned by Bluetooth SIG. For more details refer to
         * Transport Discovery Service Organization IDs in
         * <a href="https://www.bluetooth.com/specifications/assigned-numbers/">Bluetooth Assigned Numbers</a>.
         * @throws IllegalArgumentException If the {@code orgId} is invalid
         * @see OrganizationId
         * @hide
         */
        @SystemApi
        public Builder(int orgId) {
            if (orgId < 1) {
                throw new IllegalArgumentException("invalid organization id " + orgId);
            }
            mOrgId = orgId;
        }

        /**
         * Set Transport Discovery Service (TDS) flags to filter Transport Discovery Blocks.
         *
         * @param tdsFlags 1 octet value that represents the role of the device and information
         * about its state and supported features. Negative values are invalid for this argument.
         * Default to 0. See Transport Discovery Service specification for more details.
         * @param tdsFlagsMask a bitmask to select which bits in {@code tdsFlag}
         * to match. Default to 0, meaning no flag match required. Negative values are invalid for
         * this argument.
         * @throws IllegalArgumentException if either {@code tdsFlags} or {@code tdsFlagsMask} is
         * invalid.
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setTdsFlags(int tdsFlags, int tdsFlagsMask) {
            if (tdsFlags < 0) {
                throw new IllegalArgumentException("tdsFlag is invalid");
            }
            if (tdsFlagsMask < 0) {
                throw new IllegalArgumentException("tdsFlagsMask is invalid");
            }
            mTdsFlags = tdsFlags;
            mTdsFlagsMask = tdsFlagsMask;
            return this;
        }

        /**
         * Set data to filter Transport Discovery Blocks.
         *
         * Cannot be used when {@code orgId} is
         * {@link OrganizationId#WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
         *
         * @param transportData must be valid value for the particular {@code orgId}. See
         * Transport Discovery Service specification for more details.
         * @param transportDataMask a byte array with matching length to {@code transportData} to
         * select which bit to use in filter.
         * @throws IllegalArgumentException when {@code orgId} is
         * {@link OrganizationId#WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
         * @throws NullPointerException if {@code transportData} or {@code transportDataMask} is
         * {@code null}
         * @throws IllegalArgumentException if {@code transportData} or {@code transportDataMask} is
         * empty
         * @throws IllegalArgumentException if length of {@code transportData} and
         * {@code transportDataMask} do not match
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setTransportData(@NonNull byte[] transportData,
                @NonNull byte[] transportDataMask) {
            if (mOrgId == OrganizationId.WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING) {
                throw new IllegalArgumentException(
                        "setWifiNanHash() should be used instead of setTransportData() when orgId "
                                + "is WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING");
            }
            Objects.requireNonNull(transportData);
            Objects.requireNonNull(transportDataMask);
            if (transportData.length == 0) {
                throw new IllegalArgumentException("transportData is empty");
            }
            if (transportDataMask.length == 0) {
                throw new IllegalArgumentException("transportDataMask is empty");
            }
            if (transportData.length != transportDataMask.length) {
                throw new IllegalArgumentException(
                        "Length of transportData and transportDataMask do not match");
            }
            mTransportData = transportData;
            mTransportDataMask = transportDataMask;
            return this;
        }

        /**
         * Set hashed bloom filter value to filter {@link OrganizationId
         * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING} services in Transport Discovery Blocks.
         *
         * Can only be used when {@code orgId} is {@link OrganizationId
         * #WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}.
         *
         * Cannot be used together with {@link #setTransportData(byte[], byte[])}
         *
         * @param wifiNanHash 8 octets Wi-Fi NAN defined bloom filter hash
         * @throws IllegalArgumentException when {@code orgId} is not
         * {@link OrganizationId#WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING}
         * @throws IllegalArgumentException when {@code wifiNanHash} is not
         * {@link TransportBlockFilter#WIFI_NAN_HASH_LENGTH_BYTES} long
         * @throws NullPointerException when {@code wifiNanHash} is null
         * @return this builder
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder setWifiNanHash(@NonNull byte[] wifiNanHash) {
            if (mOrgId != OrganizationId.WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING) {
                throw new IllegalArgumentException("setWifiNanHash() can only be used when orgId is"
                        + " WIFI_ALLIANCE_NEIGHBOR_AWARENESS_NETWORKING");
            }
            Objects.requireNonNull(wifiNanHash);
            if (wifiNanHash.length != WIFI_NAN_HASH_LENGTH_BYTES) {
                throw new IllegalArgumentException("Wi-Fi NAN hash must be 8 octets long");
            }
            mWifiNanHash = wifiNanHash;
            return this;
        }

        /**
         * Build {@link TransportBlockFilter}.
         *
         * @return {@link TransportBlockFilter}
         * @throws IllegalStateException if the filter cannot be built
         * @hide
         */
        @SystemApi
        @NonNull
        public TransportBlockFilter build() {
            return new TransportBlockFilter(mOrgId, mTdsFlags, mTdsFlagsMask, mTransportData,
                    mTransportDataMask, mWifiNanHash);
        }
    }
}
