/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.bluetooth.le;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevice.AddressType;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The {@link AdvertisingSetParameters} provide a way to adjust advertising preferences for each
 * Bluetooth LE advertising set. Use {@link AdvertisingSetParameters.Builder} to create an instance
 * of this class.
 */
public final class AdvertisingSetParameters implements Parcelable {

    /**
     * Advertise on low frequency, around every 1000ms. This is the default and preferred
     * advertising mode as it consumes the least power.
     */
    public static final int INTERVAL_HIGH = 1600;

    /**
     * Advertise on medium frequency, around every 250ms. This is balanced between advertising
     * frequency and power consumption.
     */
    public static final int INTERVAL_MEDIUM = 400;

    /**
     * Perform high frequency, low latency advertising, around every 100ms. This has the highest
     * power consumption and should not be used for continuous background advertising.
     */
    public static final int INTERVAL_LOW = 160;

    /** Minimum value for advertising interval. */
    public static final int INTERVAL_MIN = 160;

    /** Maximum value for advertising interval. */
    public static final int INTERVAL_MAX = 16777215;

    /**
     * Advertise using the lowest transmission (TX) power level. Low transmission power can be used
     * to restrict the visibility range of advertising packets.
     */
    public static final int TX_POWER_ULTRA_LOW = -21;

    /** Advertise using low TX power level. */
    public static final int TX_POWER_LOW = -15;

    /** Advertise using medium TX power level. */
    public static final int TX_POWER_MEDIUM = -7;

    /**
     * Advertise using high TX power level. This corresponds to largest visibility range of the
     * advertising packet.
     */
    public static final int TX_POWER_HIGH = 1;

    /** Minimum value for TX power. */
    public static final int TX_POWER_MIN = -127;

    /** Maximum value for TX power. */
    public static final int TX_POWER_MAX = 1;

    /** @hide */
    @IntDef(
            prefix = "ADDRESS_TYPE_",
            value = {
                ADDRESS_TYPE_DEFAULT,
                ADDRESS_TYPE_PUBLIC,
                ADDRESS_TYPE_RANDOM,
                ADDRESS_TYPE_RANDOM_NON_RESOLVABLE,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AddressTypeStatus {}

    /**
     * Advertise own address type that corresponds privacy settings of the device.
     *
     * @hide
     */
    @SystemApi public static final int ADDRESS_TYPE_DEFAULT = -1;

    /**
     * Advertise own public address type.
     *
     * @hide
     */
    @SystemApi public static final int ADDRESS_TYPE_PUBLIC = 0;

    /**
     * Generate and adverise own resolvable private address.
     *
     * @hide
     */
    @SystemApi public static final int ADDRESS_TYPE_RANDOM = 1;

    /**
     * Generate and advertise on non-resolvable private address.
     *
     * @hide
     */
    @SystemApi public static final int ADDRESS_TYPE_RANDOM_NON_RESOLVABLE = 2;

    private final boolean mIsLegacy;
    private final boolean mIsAnonymous;
    private final boolean mIncludeTxPower;
    private final int mPrimaryPhy;
    private final int mSecondaryPhy;
    private final boolean mConnectable;
    private final boolean mDiscoverable;
    private final boolean mScannable;
    private final int mInterval;
    private final int mTxPowerLevel;
    private final int mOwnAddressType;
    private final boolean mIsDirected;
    private final boolean mIsHighDutyCycle;
    private final String mPeerAddress;
    private final @AddressType int mPeerAddressType;

    private AdvertisingSetParameters(
            boolean connectable,
            boolean discoverable,
            boolean scannable,
            boolean isLegacy,
            boolean isAnonymous,
            boolean includeTxPower,
            int primaryPhy,
            int secondaryPhy,
            int interval,
            int txPowerLevel,
            @AddressTypeStatus int ownAddressType,
            boolean isDirected,
            boolean isHighDutyCycle,
            String peerAddress,
            @AddressType int peerAddressType) {
        mConnectable = connectable;
        mDiscoverable = discoverable;
        mScannable = scannable;
        mIsLegacy = isLegacy;
        mIsAnonymous = isAnonymous;
        mIncludeTxPower = includeTxPower;
        mPrimaryPhy = primaryPhy;
        mSecondaryPhy = secondaryPhy;
        mInterval = interval;
        mTxPowerLevel = txPowerLevel;
        mOwnAddressType = ownAddressType;
        mIsDirected = isDirected;
        mIsHighDutyCycle = isHighDutyCycle;
        mPeerAddress = peerAddress;
        mPeerAddressType = peerAddressType;
    }

    private AdvertisingSetParameters(Parcel in) {
        mConnectable = in.readInt() != 0;
        mScannable = in.readInt() != 0;
        mIsLegacy = in.readInt() != 0;
        mIsAnonymous = in.readInt() != 0;
        mIncludeTxPower = in.readInt() != 0;
        mPrimaryPhy = in.readInt();
        mSecondaryPhy = in.readInt();
        mInterval = in.readInt();
        mTxPowerLevel = in.readInt();
        mOwnAddressType = in.readInt();
        mDiscoverable = in.readInt() != 0;
        mIsDirected = in.readBoolean();
        mIsHighDutyCycle = in.readBoolean();
        mPeerAddress = in.readString();
        mPeerAddressType = in.readInt();
    }

    /** Returns whether the advertisement will be connectable. */
    public boolean isConnectable() {
        return mConnectable;
    }

    /** Returns whether the advertisement will be discoverable. */
    public boolean isDiscoverable() {
        return mDiscoverable;
    }

    /** Returns whether the advertisement will be scannable. */
    public boolean isScannable() {
        return mScannable;
    }

    /** Returns whether the legacy advertisement will be used. */
    public boolean isLegacy() {
        return mIsLegacy;
    }

    /** Returns whether the advertisement will be anonymous. */
    public boolean isAnonymous() {
        return mIsAnonymous;
    }

    /** Returns whether the TX Power will be included. */
    public boolean includeTxPower() {
        return mIncludeTxPower;
    }

    /** Returns the primary advertising phy. */
    public int getPrimaryPhy() {
        return mPrimaryPhy;
    }

    /** Returns the secondary advertising phy. */
    public int getSecondaryPhy() {
        return mSecondaryPhy;
    }

    /** Returns the advertising interval. */
    public int getInterval() {
        return mInterval;
    }

    /** Returns the TX power level for advertising. */
    public int getTxPowerLevel() {
        return mTxPowerLevel;
    }

    /**
     * @return the own address type for advertising
     * @hide
     */
    @SystemApi
    public @AddressTypeStatus int getOwnAddressType() {
        return mOwnAddressType;
    }

    /**
     * @return Whether the advertisement is directed
     * @hide This API is not publicly available as it is mainly intended for accessory devices
     *     running Android to broadcast their availability and which can thus leverage system APIs.
     */
    @FlaggedApi(Flags.FLAG_DIRECTED_ADVERTISING_API)
    @SystemApi
    public boolean isDirected() {
        return mIsDirected;
    }

    /**
     * @return Whether the advertisement is high duty cycle or not
     * @hide This API is not publicly available as it is mainly intended for accessory devices
     *     running Android to broadcast their availability and which can thus leverage system APIs.
     */
    @FlaggedApi(Flags.FLAG_DIRECTED_ADVERTISING_API)
    @SystemApi
    public boolean isHighDutyCycle() {
        return mIsHighDutyCycle;
    }

    /**
     * @return Peer address for directed advertising
     * @hide This API is not publicly available as it is mainly intended for accessory devices
     *     running Android to broadcast their availability and which can thus leverage system APIs.
     */
    @FlaggedApi(Flags.FLAG_DIRECTED_ADVERTISING_API)
    @SystemApi
    public @Nullable String getPeerAddress() {
        return mPeerAddress;
    }

    /**
     * @return Peer address type for directed advertising
     * @hide This API is not publicly available as it is mainly intended for accessory devices
     *     running Android to broadcast their availability and which can thus leverage system APIs.
     */
    @FlaggedApi(Flags.FLAG_DIRECTED_ADVERTISING_API)
    @SystemApi
    public @AddressType int getPeerAddressType() {
        return mPeerAddressType;
    }

    @Override
    public String toString() {
        return "AdvertisingSetParameters [connectable="
                + mConnectable
                + ", discoverable="
                + mDiscoverable
                + ", isLegacy="
                + mIsLegacy
                + ", isAnonymous="
                + mIsAnonymous
                + ", includeTxPower="
                + mIncludeTxPower
                + ", primaryPhy="
                + mPrimaryPhy
                + ", secondaryPhy="
                + mSecondaryPhy
                + ", interval="
                + mInterval
                + ", txPowerLevel="
                + mTxPowerLevel
                + ", ownAddressType="
                + mOwnAddressType
                + ", isDirected="
                + mIsDirected
                + ", isHighDutyCycle="
                + mIsHighDutyCycle
                + ", peerAddress="
                + mPeerAddress
                + ", peerAddressType="
                + mPeerAddressType
                + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mConnectable ? 1 : 0);
        dest.writeInt(mScannable ? 1 : 0);
        dest.writeInt(mIsLegacy ? 1 : 0);
        dest.writeInt(mIsAnonymous ? 1 : 0);
        dest.writeInt(mIncludeTxPower ? 1 : 0);
        dest.writeInt(mPrimaryPhy);
        dest.writeInt(mSecondaryPhy);
        dest.writeInt(mInterval);
        dest.writeInt(mTxPowerLevel);
        dest.writeInt(mOwnAddressType);
        dest.writeInt(mDiscoverable ? 1 : 0);
        dest.writeBoolean(mIsDirected);
        dest.writeBoolean(mIsHighDutyCycle);
        android.bluetooth.BluetoothUtils.writeStringToParcel(dest, mPeerAddress);
        dest.writeInt(mPeerAddressType);
    }

    public static final @android.annotation.NonNull Parcelable.Creator<AdvertisingSetParameters>
            CREATOR =
                    new Creator<AdvertisingSetParameters>() {
                        @Override
                        public AdvertisingSetParameters[] newArray(int size) {
                            return new AdvertisingSetParameters[size];
                        }

                        @Override
                        public AdvertisingSetParameters createFromParcel(Parcel in) {
                            return new AdvertisingSetParameters(in);
                        }
                    };

    /** Builder class for {@link AdvertisingSetParameters}. */
    public static final class Builder {
        private boolean mConnectable = false;
        private boolean mDiscoverable = true;
        private boolean mScannable = false;
        private boolean mIsLegacy = false;
        private boolean mIsAnonymous = false;
        private boolean mIncludeTxPower = false;
        private int mPrimaryPhy = BluetoothDevice.PHY_LE_1M;
        private int mSecondaryPhy = BluetoothDevice.PHY_LE_1M;
        private int mInterval = INTERVAL_LOW;
        private int mTxPowerLevel = TX_POWER_MEDIUM;
        private int mOwnAddressType = ADDRESS_TYPE_DEFAULT;
        private boolean mIsDirected = false;
        private boolean mIsHighDutyCycle = false;
        private String mPeerAddress = null;
        private @AddressType int mPeerAddressType = BluetoothDevice.ADDRESS_TYPE_PUBLIC;

        /**
         * Set whether the advertisement type should be connectable or non-connectable. Legacy
         * advertisements can be both connectable and scannable. Non-legacy advertisements can be
         * only scannable or only connectable.
         *
         * @param connectable Controls whether the advertisement type will be connectable (true) or
         *     non-connectable (false).
         */
        public Builder setConnectable(boolean connectable) {
            mConnectable = connectable;
            return this;
        }

        /**
         * Set whether the advertisement type should be discoverable or non-discoverable. By
         * default, advertisements will be discoverable. Devices connecting to non-discoverable
         * advertisements cannot initiate bonding.
         *
         * @param discoverable Controls whether the advertisement type will be discoverable ({@code
         *     true}) or non-discoverable ({@code false}).
         */
        public @NonNull Builder setDiscoverable(boolean discoverable) {
            mDiscoverable = discoverable;
            return this;
        }

        /**
         * Set whether the advertisement type should be scannable. Legacy advertisements can be both
         * connectable and scannable. Non-legacy advertisements can be only scannable or only
         * connectable.
         *
         * @param scannable Controls whether the advertisement type will be scannable (true) or
         *     non-scannable (false).
         */
        public Builder setScannable(boolean scannable) {
            mScannable = scannable;
            return this;
        }

        /**
         * When set to true, advertising set will advertise 4.x Spec compliant advertisements.
         *
         * @param isLegacy whether legacy advertising mode should be used.
         */
        public Builder setLegacyMode(boolean isLegacy) {
            mIsLegacy = isLegacy;
            return this;
        }

        /**
         * Set whether advertiser address should be omitted from all packets. If this mode is used,
         * periodic advertising can't be enabled for this set.
         *
         * <p>This is used only if legacy mode is not used.
         *
         * @param isAnonymous whether anonymous advertising should be used.
         */
        public Builder setAnonymous(boolean isAnonymous) {
            mIsAnonymous = isAnonymous;
            return this;
        }

        /**
         * Set whether TX power should be included in the extended header.
         *
         * <p>This is used only if legacy mode is not used.
         *
         * @param includeTxPower whether TX power should be included in extended header
         */
        public Builder setIncludeTxPower(boolean includeTxPower) {
            mIncludeTxPower = includeTxPower;
            return this;
        }

        /**
         * Set the primary physical channel used for this advertising set.
         *
         * <p>This is used only if legacy mode is not used.
         *
         * <p>Use {@link BluetoothAdapter#isLeCodedPhySupported} to determine if LE Coded PHY is
         * supported on this device.
         *
         * @param primaryPhy Primary advertising physical channel, can only be {@link
         *     BluetoothDevice#PHY_LE_1M} or {@link BluetoothDevice#PHY_LE_CODED}.
         * @throws IllegalArgumentException If the primaryPhy is invalid.
         */
        public Builder setPrimaryPhy(int primaryPhy) {
            if (primaryPhy != BluetoothDevice.PHY_LE_1M
                    && primaryPhy != BluetoothDevice.PHY_LE_CODED) {
                throw new IllegalArgumentException("bad primaryPhy " + primaryPhy);
            }
            mPrimaryPhy = primaryPhy;
            return this;
        }

        /**
         * Set the secondary physical channel used for this advertising set.
         *
         * <p>This is used only if legacy mode is not used.
         *
         * <p>Use {@link BluetoothAdapter#isLeCodedPhySupported} and {@link
         * BluetoothAdapter#isLe2MPhySupported} to determine if LE Coded PHY or 2M PHY is supported
         * on this device.
         *
         * @param secondaryPhy Secondary advertising physical channel, can only be one of {@link
         *     BluetoothDevice#PHY_LE_1M}, {@link BluetoothDevice#PHY_LE_2M} or {@link
         *     BluetoothDevice#PHY_LE_CODED}.
         * @throws IllegalArgumentException If the secondaryPhy is invalid.
         */
        public Builder setSecondaryPhy(int secondaryPhy) {
            if (secondaryPhy != BluetoothDevice.PHY_LE_1M
                    && secondaryPhy != BluetoothDevice.PHY_LE_2M
                    && secondaryPhy != BluetoothDevice.PHY_LE_CODED) {
                throw new IllegalArgumentException("bad secondaryPhy " + secondaryPhy);
            }
            mSecondaryPhy = secondaryPhy;
            return this;
        }

        /**
         * Set advertising interval.
         *
         * @param interval Bluetooth LE Advertising interval, in 0.625ms unit. Valid range is from
         *     160 (100ms) to 16777215 (10,485.759375 s). Recommended values are: {@link
         *     AdvertisingSetParameters#INTERVAL_LOW}, {@link
         *     AdvertisingSetParameters#INTERVAL_MEDIUM}, or {@link
         *     AdvertisingSetParameters#INTERVAL_HIGH}.
         * @throws IllegalArgumentException If the interval is invalid.
         */
        public Builder setInterval(int interval) {
            if (interval < INTERVAL_MIN || interval > INTERVAL_MAX) {
                throw new IllegalArgumentException("unknown interval " + interval);
            }
            mInterval = interval;
            return this;
        }

        /**
         * Set the transmission power level for the advertising.
         *
         * @param txPowerLevel Transmission power of Bluetooth LE Advertising, in dBm. The valid
         *     range is [-127, 1] Recommended values are: {@link
         *     AdvertisingSetParameters#TX_POWER_ULTRA_LOW}, {@link
         *     AdvertisingSetParameters#TX_POWER_LOW}, {@link
         *     AdvertisingSetParameters#TX_POWER_MEDIUM}, or {@link
         *     AdvertisingSetParameters#TX_POWER_HIGH}.
         * @throws IllegalArgumentException If the {@code txPowerLevel} is invalid.
         */
        public Builder setTxPowerLevel(int txPowerLevel) {
            if (txPowerLevel < TX_POWER_MIN || txPowerLevel > TX_POWER_MAX) {
                throw new IllegalArgumentException("unknown txPowerLevel " + txPowerLevel);
            }
            mTxPowerLevel = txPowerLevel;
            return this;
        }

        /**
         * Set own address type for advertising to control public or privacy mode. If used to set
         * address type anything other than {@link AdvertisingSetParameters#ADDRESS_TYPE_DEFAULT},
         * then it will require BLUETOOTH_PRIVILEGED permission and will be checked at the time of
         * starting advertising.
         *
         * @throws IllegalArgumentException If the {@code ownAddressType} is invalid
         * @hide
         */
        @SystemApi
        public @NonNull Builder setOwnAddressType(@AddressTypeStatus int ownAddressType) {
            if (ownAddressType < AdvertisingSetParameters.ADDRESS_TYPE_DEFAULT
                    || ownAddressType
                            > AdvertisingSetParameters.ADDRESS_TYPE_RANDOM_NON_RESOLVABLE) {
                throw new IllegalArgumentException("unknown address type " + ownAddressType);
            }
            mOwnAddressType = ownAddressType;
            return this;
        }

        /**
         * Set whether the advertising is a directed advertising.
         *
         * @param isDirected Controls whether the advertising is directed or not
         * @hide This API is not publicly available as it is mainly intended for accessory devices
         *     running Android to broadcast their availability and which can thus leverage system
         *     APIs.
         */
        @FlaggedApi(Flags.FLAG_DIRECTED_ADVERTISING_API)
        @SystemApi
        public @NonNull Builder setDirected(boolean isDirected) {
            mIsDirected = isDirected;
            return this;
        }

        /**
         * Set whether the advertising is high duty cycle or not.
         *
         * @param isHighDutyCycle Controls whether the advertising high duty cycle or not
         * @hide This API is not publicly available as it is mainly intended for accessory devices
         *     running Android to broadcast their availability and which can thus leverage system
         *     APIs.
         */
        @FlaggedApi(Flags.FLAG_DIRECTED_ADVERTISING_API)
        @SystemApi
        public @NonNull Builder setHighDutyCycle(boolean isHighDutyCycle) {
            mIsHighDutyCycle = isHighDutyCycle;
            return this;
        }

        /**
         * Set peer address for directed advertising.
         *
         * @param peerAddress peer address for the directed advertising
         * @throws IllegalArgumentException peer address is invalid
         * @hide This API is not publicly available as it is mainly intended for accessory devices
         *     running Android to broadcast their availability and which can thus leverage system
         *     APIs.
         */
        @FlaggedApi(Flags.FLAG_DIRECTED_ADVERTISING_API)
        @SystemApi
        public @NonNull Builder setPeerAddress(@NonNull String peerAddress) {
            requireNonNull(peerAddress);
            if (!BluetoothAdapter.checkBluetoothAddress(peerAddress)) {
                throw new IllegalArgumentException(
                        peerAddress + " is not a valid Bluetooth address");
            }
            mPeerAddress = peerAddress;
            return this;
        }

        /**
         * Set peer address type for directed advertising.
         *
         * @param peerAddressType peer address type for the directed advertising
         * @throws IllegalArgumentException if {@code peerAddressType} is invalid
         * @hide This API is not publicly available as it is mainly intended for accessory devices
         *     running Android to broadcast their availability and which can thus leverage system
         *     APIs.
         */
        @FlaggedApi(Flags.FLAG_DIRECTED_ADVERTISING_API)
        @SystemApi
        public @NonNull Builder setPeerAddressType(@AddressType int peerAddressType) {
            switch (peerAddressType) {
                case BluetoothDevice.ADDRESS_TYPE_PUBLIC:
                case BluetoothDevice.ADDRESS_TYPE_RANDOM:
                    mPeerAddressType = peerAddressType;
                    break;
                case BluetoothDevice.ADDRESS_TYPE_UNKNOWN:
                case BluetoothDevice.ADDRESS_TYPE_ANONYMOUS:
                    throw new IllegalArgumentException(
                            "unsupported peer address type " + peerAddressType);
                default:
                    throw new IllegalArgumentException(
                            "unknown peer address type " + peerAddressType);
            }
            return this;
        }

        /**
         * Build the {@link AdvertisingSetParameters} object.
         *
         * @throws IllegalStateException if invalid combination of parameters is used.
         */
        public AdvertisingSetParameters build() {
            if (mIsLegacy) {
                if (mIsAnonymous) {
                    throw new IllegalArgumentException("Legacy advertising can't be anonymous");
                }

                if (mIsDirected && !mConnectable) {
                    throw new IllegalStateException(
                            "Legacy directed advertising must be connectable");
                }

                if (mIsDirected && mScannable) {
                    throw new IllegalStateException(
                            "Legacy directed advertising can't be scannable");
                }

                if (!mIsDirected && mIsHighDutyCycle) {
                    throw new IllegalStateException(
                            "Non-directed legacy advertising can't be high duty cycle");
                }

                if (!mIsDirected && mConnectable && !mScannable) {
                    throw new IllegalStateException(
                            "Non-directed legacy advertising can't be connectable and"
                                    + " non-scannable");
                }

                if (mIncludeTxPower) {
                    throw new IllegalStateException(
                            "Legacy advertising can't include TX power level in header");
                }
            } else {
                if (mConnectable && mScannable) {
                    throw new IllegalStateException(
                            "Advertising can't be both connectable and scannable");
                }

                if (mIsAnonymous && mConnectable) {
                    throw new IllegalStateException(
                            "Advertising can't be both connectable and anonymous");
                }

                if (mIsHighDutyCycle) {
                    throw new IllegalStateException(
                            "Non-legacy advertising can't be high duty cycle");
                }
            }

            if (mIsDirected && mPeerAddress == null) {
                throw new IllegalStateException(
                        "Peer address should not be null for directed advertising");
            }

            return new AdvertisingSetParameters(
                    mConnectable,
                    mDiscoverable,
                    mScannable,
                    mIsLegacy,
                    mIsAnonymous,
                    mIncludeTxPower,
                    mPrimaryPhy,
                    mSecondaryPhy,
                    mInterval,
                    mTxPowerLevel,
                    mOwnAddressType,
                    mIsDirected,
                    mIsHighDutyCycle,
                    mPeerAddress,
                    mPeerAddressType);
        }
    }
}
