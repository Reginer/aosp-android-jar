/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.net.wifi.aware;

import static android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION;

import static com.android.ranging.flags.Flags.FLAG_RANGING_RTT_ENABLED;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.net.wifi.OuiKeyedData;
import android.net.wifi.ParcelUtil;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiAnnotations;
import android.net.wifi.WifiScanner;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.ResponderConfig;
import android.net.wifi.util.HexEncoding;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Defines the configuration of an Aware subscribe session. Built using
 * {@link SubscribeConfig.Builder}. Subscribe is done using
 * {@link WifiAwareSession#subscribe(SubscribeConfig, DiscoverySessionCallback,
 * android.os.Handler)} or
 * {@link SubscribeDiscoverySession#updateSubscribe(SubscribeConfig)}.
 */
public final class SubscribeConfig implements Parcelable {
    /** @hide */
    @IntDef({
            SUBSCRIBE_TYPE_PASSIVE, SUBSCRIBE_TYPE_ACTIVE })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SubscribeTypes {
    }

    /**
     * Defines a passive subscribe session - a subscribe session where
     * subscribe packets are not transmitted over-the-air and the device listens
     * and matches to transmitted publish packets. Configuration is done using
     * {@link SubscribeConfig.Builder#setSubscribeType(int)}.
     */
    public static final int SUBSCRIBE_TYPE_PASSIVE = 0;

    /**
     * Defines an active subscribe session - a subscribe session where
     * subscribe packets are transmitted over-the-air. Configuration is done
     * using {@link SubscribeConfig.Builder#setSubscribeType(int)}.
     */
    public static final int SUBSCRIBE_TYPE_ACTIVE = 1;

    private static final int AWARE_BAND_2_DISCOVERY_CHANNEL = 2437;
    private static final int MIN_RTT_BURST_SIZE = RangingRequest.getMinRttBurstSize();
    private static final int MAX_RTT_BURST_SIZE = RangingRequest.getMaxRttBurstSize();

    /**
     * Ranging Interval's are in binary Time Unit (TU). As per IEEE 802.11-1999 1 TU equals
     * 1024 microseconds.
     *
     * @hide
     */
    @IntDef({
            PERIODIC_RANGING_INTERVAL_NONE, PERIODIC_RANGING_INTERVAL_128TU,
            PERIODIC_RANGING_INTERVAL_256TU, PERIODIC_RANGING_INTERVAL_512TU,
            PERIODIC_RANGING_INTERVAL_1024TU, PERIODIC_RANGING_INTERVAL_2048TU,
            PERIODIC_RANGING_INTERVAL_4096TU, PERIODIC_RANGING_INTERVAL_8192TU})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PeriodicRangingInterval {
    }

    /**
     * Ranging is not repeated
     *
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public static final int PERIODIC_RANGING_INTERVAL_NONE = 0;

    /**
     * Ranging interval is 128TU [= (128 * 1024) / 1000 = 131.072 ms]
     *
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public static final int PERIODIC_RANGING_INTERVAL_128TU = 128;

    /**
     * Ranging interval is 256TU [= (256 * 1024) / 1000 = 262.144 ms]
     *
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public static final int PERIODIC_RANGING_INTERVAL_256TU = 256;

    /**
     * Ranging interval is 512TU [= (512 * 1024) / 1000 = 524.288 ms]
     *
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public static final int PERIODIC_RANGING_INTERVAL_512TU = 512;

    /**
     * Ranging interval is 1024TU [= (1024 * 1024) / 1000 = 1048.576 ms]
     *
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public static final int PERIODIC_RANGING_INTERVAL_1024TU = 1024;

    /**
     * Ranging interval is 2048TU [= (2048 * 1024) / 1000 = 2097.152 ms]
     *
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public static final int PERIODIC_RANGING_INTERVAL_2048TU = 2048;

    /**
     * Ranging interval is 4096TU [= (4096 * 1024) / 1000 = 4194.304 ms]
     *
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public static final int PERIODIC_RANGING_INTERVAL_4096TU = 4096;

    /**
     * Ranging interval is 8192TU [= (8192 * 1024) / 1000 = 8388.608 ms]
     *
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public static final int PERIODIC_RANGING_INTERVAL_8192TU = 8192;

    /** @hide */
    public final byte[] mServiceName;

    /** @hide */
    public final byte[] mServiceSpecificInfo;

    /** @hide */
    public final byte[] mMatchFilter;

    /** @hide */
    public final int mSubscribeType;

    /** @hide */
    public final int mTtlSec;

    /** @hide */
    public final boolean mEnableTerminateNotification;

    /** @hide */
    public final boolean mMinDistanceMmSet;

    /** @hide */
    public final int mMinDistanceMm;

    /** @hide */
    public final boolean mMaxDistanceMmSet;

    /** @hide */
    public final int mMaxDistanceMm;

    private final boolean mEnableInstantMode;

    private final int mBand;

    private final AwarePairingConfig mPairingConfig;

    private final boolean mIsSuspendable;
    private final List<OuiKeyedData> mVendorData;

    /** @hide */
    public final int mPeriodicRangingInterval;

    /** @hide */
    public final boolean mPeriodicRangingEnabled;

    /** @hide */
    public final int mRttBurstSize;

    /** @hide */
    public final int mFrequencyMhz;

    /** @hide */
    public final int mCenterFrequency0Mhz;

    /** @hide */
    public final int mCenterFrequency1Mhz;

    /** @hide */
    public final int mPreamble;

    /** @hide */
    public final int mChannelWidth;

    /** @hide */
    public SubscribeConfig(byte[] serviceName, byte[] serviceSpecificInfo, byte[] matchFilter,
            int subscribeType, int ttlSec, boolean enableTerminateNotification,
            boolean minDistanceMmSet, int minDistanceMm, boolean maxDistanceMmSet,
            int maxDistanceMm, boolean enableInstantMode, @WifiScanner.WifiBand int band,
            AwarePairingConfig pairingConfig, boolean isSuspendable,
            @NonNull List<OuiKeyedData> vendorData, int rangingInterval,
            boolean enablePeriodicRanging, int rttBurstSize, int frequencyMhz,
            int centerFrequency0Mhz, int centerFrequency1Mhz, int preamble,
            int channelWidth) {
        mServiceName = serviceName;
        mServiceSpecificInfo = serviceSpecificInfo;
        mMatchFilter = matchFilter;
        mSubscribeType = subscribeType;
        mTtlSec = ttlSec;
        mEnableTerminateNotification = enableTerminateNotification;
        mMinDistanceMm = minDistanceMm;
        mMinDistanceMmSet = minDistanceMmSet;
        mMaxDistanceMm = maxDistanceMm;
        mMaxDistanceMmSet = maxDistanceMmSet;
        mEnableInstantMode = enableInstantMode;
        mBand = band;
        mPairingConfig = pairingConfig;
        mIsSuspendable = isSuspendable;
        mVendorData = vendorData;
        mPeriodicRangingInterval = rangingInterval;
        mPeriodicRangingEnabled = enablePeriodicRanging;
        mRttBurstSize = rttBurstSize;
        mFrequencyMhz = frequencyMhz;
        mCenterFrequency0Mhz = centerFrequency0Mhz;
        mCenterFrequency1Mhz = centerFrequency1Mhz;
        mPreamble = preamble;
        mChannelWidth = channelWidth;
    }

    @Override
    public String toString() {
        return "SubscribeConfig [mServiceName='" + (mServiceName == null ? "<null>"
                : String.valueOf(HexEncoding.encode(mServiceName))) + ", mServiceName.length=" + (
                mServiceName == null ? 0 : mServiceName.length) + ", mServiceSpecificInfo='" + (
                (mServiceSpecificInfo == null) ? "<null>" : String.valueOf(
                        HexEncoding.encode(mServiceSpecificInfo)))
                + ", mServiceSpecificInfo.length=" + (mServiceSpecificInfo == null ? 0
                : mServiceSpecificInfo.length) + ", mMatchFilter="
                + (new TlvBufferUtils.TlvIterable(0, 1, mMatchFilter)).toString()
                + ", mMatchFilter.length=" + (mMatchFilter == null ? 0 : mMatchFilter.length)
                + ", mSubscribeType=" + mSubscribeType + ", mTtlSec=" + mTtlSec
                + ", mEnableTerminateNotification=" + mEnableTerminateNotification
                + ", mMinDistanceMm=" + mMinDistanceMm
                + ", mMinDistanceMmSet=" + mMinDistanceMmSet
                + ", mMaxDistanceMm=" + mMaxDistanceMm
                + ", mMaxDistanceMmSet=" + mMaxDistanceMmSet + "]"
                + ", mEnableInstantMode=" + mEnableInstantMode
                + ", mBand=" + mBand
                + ", mPairingConfig" + mPairingConfig
                + ", mIsSuspendable=" + mIsSuspendable
                + ", mVendorData=" + mVendorData + "]"
                + ", mPeriodicRangingInterval" + mPeriodicRangingInterval
                + ", mPeriodicRangingEnabled" + mPeriodicRangingEnabled
                + ", mRttBurstSize" + mRttBurstSize
                + ", mFrequencyMhz" + mFrequencyMhz
                + ", mCenterFrequency0Mhz" + mCenterFrequency0Mhz
                + ", mCenterFrequency1Mhz" + mCenterFrequency1Mhz
                + ", mPreamble" + mPreamble
                + ", mChannelWidth" + mChannelWidth;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(mServiceName);
        dest.writeByteArray(mServiceSpecificInfo);
        dest.writeByteArray(mMatchFilter);
        dest.writeInt(mSubscribeType);
        dest.writeInt(mTtlSec);
        dest.writeInt(mEnableTerminateNotification ? 1 : 0);
        dest.writeInt(mMinDistanceMm);
        dest.writeInt(mMinDistanceMmSet ? 1 : 0);
        dest.writeInt(mMaxDistanceMm);
        dest.writeInt(mMaxDistanceMmSet ? 1 : 0);
        dest.writeBoolean(mEnableInstantMode);
        dest.writeInt(mBand);
        dest.writeParcelable(mPairingConfig, flags);
        dest.writeBoolean(mIsSuspendable);
        dest.writeList(mVendorData);
        dest.writeInt(mPeriodicRangingInterval);
        dest.writeBoolean(mPeriodicRangingEnabled);
        dest.writeInt(mRttBurstSize);
        dest.writeInt(mFrequencyMhz);
        dest.writeInt(mCenterFrequency0Mhz);
        dest.writeInt(mCenterFrequency1Mhz);
        dest.writeInt(mPreamble);
        dest.writeInt(mChannelWidth);
    }

    @NonNull
    public static final Creator<SubscribeConfig> CREATOR = new Creator<>() {
        @Override
        public SubscribeConfig[] newArray(int size) {
            return new SubscribeConfig[size];
        }

        @Override
        public SubscribeConfig createFromParcel(Parcel in) {
            byte[] serviceName = in.createByteArray();
            byte[] ssi = in.createByteArray();
            byte[] matchFilter = in.createByteArray();
            int subscribeType = in.readInt();
            int ttlSec = in.readInt();
            boolean enableTerminateNotification = in.readInt() != 0;
            int minDistanceMm = in.readInt();
            boolean minDistanceMmSet = in.readInt() != 0;
            int maxDistanceMm = in.readInt();
            boolean maxDistanceMmSet = in.readInt() != 0;
            boolean enableInstantMode = in.readBoolean();
            int band = in.readInt();
            AwarePairingConfig pairingConfig = in.readParcelable(
                    AwarePairingConfig.class.getClassLoader());
            boolean isSuspendable = in.readBoolean();
            List<OuiKeyedData> vendorData = ParcelUtil.readOuiKeyedDataList(in);
            int rangingInterval = in.readInt();
            boolean enablePeriodicRanging = in.readBoolean();
            int burstSize = in.readInt();
            int frequencyMhz = in.readInt();
            int centerFrequency0Mhz = in.readInt();
            int centerFrequency1Mhz = in.readInt();
            int preamble = in.readInt();
            int channelWidth = in.readInt();

            return new SubscribeConfig(serviceName, ssi, matchFilter, subscribeType, ttlSec,
                    enableTerminateNotification, minDistanceMmSet, minDistanceMm, maxDistanceMmSet,
                    maxDistanceMm, enableInstantMode, band, pairingConfig, isSuspendable,
                    vendorData, rangingInterval, enablePeriodicRanging, burstSize,
                    frequencyMhz, centerFrequency0Mhz, centerFrequency1Mhz, preamble,
                    channelWidth);
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SubscribeConfig)) {
            return false;
        }

        SubscribeConfig lhs = (SubscribeConfig) o;

        if (!(Arrays.equals(mServiceName, lhs.mServiceName) && Arrays.equals(
                mServiceSpecificInfo, lhs.mServiceSpecificInfo) && Arrays.equals(mMatchFilter,
                lhs.mMatchFilter) && mSubscribeType == lhs.mSubscribeType && mTtlSec == lhs.mTtlSec
                && mEnableTerminateNotification == lhs.mEnableTerminateNotification
                && mMinDistanceMmSet == lhs.mMinDistanceMmSet
                && mMaxDistanceMmSet == lhs.mMaxDistanceMmSet
                && mEnableInstantMode == lhs.mEnableInstantMode
                && mBand == lhs.mBand
                && mIsSuspendable == lhs.mIsSuspendable
                && Objects.equals(mVendorData, lhs.mVendorData)
                && mPeriodicRangingEnabled == lhs.mPeriodicRangingEnabled
                && mRttBurstSize == lhs.mRttBurstSize
                && mFrequencyMhz == lhs.mFrequencyMhz
                && mCenterFrequency0Mhz == lhs.mCenterFrequency0Mhz
                && mCenterFrequency1Mhz == lhs.mCenterFrequency1Mhz
                && mPreamble == lhs.mPreamble
                && mChannelWidth == lhs.mChannelWidth)) {
            return false;
        }

        if (mMinDistanceMmSet && mMinDistanceMm != lhs.mMinDistanceMm) {
            return false;
        }

        if (mMaxDistanceMmSet && mMaxDistanceMm != lhs.mMaxDistanceMm) {
            return false;
        }

        if (mPeriodicRangingEnabled && mPeriodicRangingInterval != lhs.mPeriodicRangingInterval) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(Arrays.hashCode(mServiceName),
                Arrays.hashCode(mServiceSpecificInfo), Arrays.hashCode(mMatchFilter),
                mSubscribeType, mTtlSec, mEnableTerminateNotification, mMinDistanceMmSet,
                mMaxDistanceMmSet, mEnableInstantMode, mBand,  mIsSuspendable, mVendorData,
                mPeriodicRangingEnabled, mRttBurstSize, mFrequencyMhz, mCenterFrequency0Mhz,
                mCenterFrequency1Mhz, mPreamble, mChannelWidth);

        if (mMinDistanceMmSet) {
            result = Objects.hash(result, mMinDistanceMm);
        }
        if (mMaxDistanceMmSet) {
            result = Objects.hash(result, mMaxDistanceMm);
        }
        if (mPeriodicRangingEnabled) {
            result = Objects.hash(result, mPeriodicRangingInterval);
        }

        return result;
    }

    /**
     * Verifies that the contents of the SubscribeConfig are valid. Otherwise
     * throws an IllegalArgumentException.
     *
     * @hide
     */
    public void assertValid(Characteristics characteristics, boolean rttSupported)
            throws IllegalArgumentException {
        WifiAwareUtils.validateServiceName(mServiceName);

        if (!TlvBufferUtils.isValid(mMatchFilter, 0, 1)) {
            throw new IllegalArgumentException(
                    "Invalid matchFilter configuration - LV fields do not match up to length");
        }
        if (mSubscribeType < SUBSCRIBE_TYPE_PASSIVE || mSubscribeType > SUBSCRIBE_TYPE_ACTIVE) {
            throw new IllegalArgumentException("Invalid subscribeType - " + mSubscribeType);
        }
        if (mTtlSec < 0) {
            throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
        }

        if (characteristics != null) {
            int maxServiceNameLength = characteristics.getMaxServiceNameLength();
            if (maxServiceNameLength != 0 && mServiceName.length > maxServiceNameLength) {
                throw new IllegalArgumentException(
                        "Service name longer than supported by device characteristics");
            }
            int maxServiceSpecificInfoLength = characteristics.getMaxServiceSpecificInfoLength();
            if (maxServiceSpecificInfoLength != 0 && mServiceSpecificInfo != null
                    && mServiceSpecificInfo.length > maxServiceSpecificInfoLength) {
                throw new IllegalArgumentException(
                        "Service specific info longer than supported by device characteristics");
            }
            int maxMatchFilterLength = characteristics.getMaxMatchFilterLength();
            if (maxMatchFilterLength != 0 && mMatchFilter != null
                    && mMatchFilter.length > maxMatchFilterLength) {
                throw new IllegalArgumentException(
                        "Match filter longer than supported by device characteristics");
            }
            if (mEnableInstantMode) {
                if (SdkLevel.isAtLeastT()
                        && characteristics.isInstantCommunicationModeSupported()) {
                    // Valid to use instant communication mode
                } else {
                    throw new IllegalArgumentException("instant mode is not supported");
                }
            }
            if (mIsSuspendable && !characteristics.isSuspensionSupported()) {
                throw new IllegalArgumentException("Aware Suspension is not supported");
            }
            if (mPairingConfig != null && !characteristics.isAwarePairingSupported()) {
                throw new IllegalArgumentException("Aware Pairing is not supported");
            }
        }

        if (mMinDistanceMmSet && mMinDistanceMm < 0) {
            throw new IllegalArgumentException("Minimum distance must be non-negative");
        }
        if (mMaxDistanceMmSet && mMaxDistanceMm < 0) {
            throw new IllegalArgumentException("Maximum distance must be non-negative");
        }
        if (mMinDistanceMmSet && mMaxDistanceMmSet && mMaxDistanceMm <= mMinDistanceMm) {
            throw new IllegalArgumentException(
                    "Maximum distance must be greater than minimum distance");
        }

        if (mPeriodicRangingEnabled && (mMinDistanceMmSet || mMaxDistanceMmSet)) {
            throw new IllegalArgumentException(
                    "Either Periodic Ranging or Min/Max distance is allowed. Not both.");
        }

        if (!rttSupported && (mMinDistanceMmSet || mMaxDistanceMmSet)) {
            throw new IllegalArgumentException("Ranging is not supported");
        }
        if ((!rttSupported || !characteristics.isPeriodicRangingSupported())
                && mPeriodicRangingEnabled) {
            throw new IllegalArgumentException("Periodic ranging is not supported");
        }
        if (mPeriodicRangingEnabled && mPeriodicRangingInterval < 0) {
            throw new IllegalArgumentException("Periodic ranging interval must be non-negative");
        }
        if (mPeriodicRangingEnabled && mRttBurstSize < 0) {
            throw new IllegalArgumentException("Rtt Burst size must be non-negative");
        }
        if (mPeriodicRangingEnabled && mFrequencyMhz < 0) {
            throw new IllegalArgumentException(" Frequency must be non-negative");
        }
        if (mPeriodicRangingEnabled && mCenterFrequency0Mhz < 0) {
            throw new IllegalArgumentException("Center Frequency0 must be non-negative");
        }
        if (mPeriodicRangingEnabled && mCenterFrequency1Mhz < 0) {
            throw new IllegalArgumentException("Center Frequency1 must be non-negative");
        }
        if (mPeriodicRangingEnabled && mPreamble < 0) {
            throw new IllegalArgumentException("Preamble must be non-negative");
        }
        if (mPeriodicRangingEnabled && mChannelWidth < 0) {
            throw new IllegalArgumentException("Channel width must be non-negative");
        }
    }

    /**
     * Check if instant mode is enabled for this subscribe session.
     * @see Builder#setInstantCommunicationModeEnabled(boolean, int)
     * @return true for enabled, false otherwise.
     */
    public boolean isInstantCommunicationModeEnabled() {
        return mEnableInstantMode;
    }

    /**
     * Check if enable instant mode on 5G for this subscribe session
     *
     * @see Builder#setInstantCommunicationModeEnabled(boolean, int)
     * @return If instant communication mode is not enabled will return {@link
     *     ScanResult#WIFI_BAND_24_GHZ} as default.
     */
    @WifiAwareManager.InstantModeBand
    public int getInstantCommunicationBand() {
        return mBand;
    }

    /**
     * Get the Aware Pairing config for this subscribe session
     * @see Builder#setPairingConfig(AwarePairingConfig)
     * @return A {@link AwarePairingConfig} specified in this config.
     */
    @Nullable
    public AwarePairingConfig getPairingConfig() {
        return mPairingConfig;
    }

    /**
     * Check if suspension is supported for this subscribe session.
     * @see Builder#setSuspendable(boolean)
     * @return true for supported, false otherwise.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SystemApi
    public boolean isSuspendable() {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException();
        }
        return mIsSuspendable;
    }

    /**
     * Return the vendor-provided configuration data, if it exists. See also {@link
     * Builder#setVendorData(List)}
     *
     * @return Vendor configuration data, or empty list if it does not exist.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @NonNull
    @SystemApi
    public List<OuiKeyedData> getVendorData() {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        return mVendorData != null ? mVendorData : Collections.emptyList();
    }

    /**
     * Check if periodic range reporting is enabled for subscribe session
     * @see Builder#setPeriodicRangingEnabled(boolean)
     * @return true for enabled, false otherwise.
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public boolean isPeriodicRangingEnabled() {
        return mPeriodicRangingEnabled;
    }

    /**
     * Get periodic range reporting interval for subscribe session
     * @see Builder#setPeriodicRangingInterval(int)
     * @return interval of reporting.
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public @PeriodicRangingInterval int getPeriodicRangingInterval() {
        return mPeriodicRangingInterval;
    }

    /**
     * Get the RTT burst size used to determine the average range.
     * @see Builder#setRttBurstSize(int)
     * @return the RTT burst size.
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public int getRttBurstSize() {
        return mRttBurstSize;
    }

    /**
     * Get the frequency in MHz of the Wi-Fi channel
     * @see Builder#setFrequencyMhz(int)
     * @return frequency in MHz.
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    @IntRange(from = 0)
    public int getFrequencyMhz() {
        return mFrequencyMhz;
    }

    /**
     * Get the center frequency in MHz of the first channel segment
     * @see Builder#setCenterFreq0Mhz(int)
     * @return the center frequency in MHz of the first channel segment.
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    @IntRange(from = 0)
    public int getCenterFreq0Mhz() {
        return mCenterFrequency0Mhz;
    }

    /**
     * Get the center frequency in MHz of the second channel segment (if used)
     * @see Builder#setCenterFreq1Mhz(int)
     * @return the center frequency in MHz of the second channel segment
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    @IntRange(from = 0)
    public int getCenterFreq1Mhz() {
        return mCenterFrequency1Mhz;
    }

    /**
     * Get the preamble type of the channel.
     * @see Builder#setPreamble(int)
     * @return the preamble used for this channel.
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public @WifiAnnotations.PreambleType int getPreamble() {
        return ResponderConfig.translateFromLocalToScanResultPreamble(mPreamble);
    }

    /**
     * Channel bandwidth; one of {@link ScanResult#CHANNEL_WIDTH_20MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_40MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_80MHZ}, {@link ScanResult#CHANNEL_WIDTH_160MHZ},
     * {@link ScanResult #CHANNEL_WIDTH_80MHZ_PLUS_MHZ} or {@link ScanResult#CHANNEL_WIDTH_320MHZ}.
     * @see Builder#setChannelWidth(int)
     * @return the bandwidth repsentation of the Wi-Fi channel
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public @WifiAnnotations.ChannelWidth int getChannelWidth() {
        return ResponderConfig.translateFromLocalToScanResultChannelWidth(mChannelWidth);
    }

    /**
     * Builder used to build {@link SubscribeConfig} objects.
     */
    public static final class Builder {
        private byte[] mServiceName;
        private byte[] mServiceSpecificInfo;
        private byte[] mMatchFilter;
        private int mSubscribeType = SUBSCRIBE_TYPE_PASSIVE;
        private int mTtlSec = 0;
        private boolean mEnableTerminateNotification = true;
        private boolean mMinDistanceMmSet = false;
        private int mMinDistanceMm;
        private boolean mMaxDistanceMmSet = false;
        private int mMaxDistanceMm;
        private boolean mEnableInstantMode;
        private int mBand = WifiScanner.WIFI_BAND_24_GHZ;
        private AwarePairingConfig mPairingConfig;
        private boolean mIsSuspendable = false;
        private @NonNull List<OuiKeyedData> mVendorData = Collections.emptyList();
        private boolean mPeriodicRangingEnabled = false;
        private int mPeriodicRangingInterval = PERIODIC_RANGING_INTERVAL_512TU;
        private int mRttBurstSize = RangingRequest.getDefaultRttBurstSize();
        private int mFrequencyMhz = AWARE_BAND_2_DISCOVERY_CHANNEL;
        private int mCenterFrequency0Mhz = 0;
        private int mCenterFrequency1Mhz = 0;
        private int mPreamble = ResponderConfig.PREAMBLE_HT;
        private int mChannelWidth = ResponderConfig.CHANNEL_WIDTH_20MHZ;

        /**
         * Specify the service name of the subscribe session. The actual on-air
         * value is a 6 byte hashed representation of this string.
         * <p>
         * The Service Name is a UTF-8 encoded string from 1 to 255 bytes in length.
         * The only acceptable single-byte UTF-8 symbols for a Service Name are alphanumeric
         * values (A-Z, a-z, 0-9), the hyphen ('-'), the period ('.') and the underscore ('_'). All
         * valid multi-byte UTF-8 characters are acceptable in a Service Name.
         * <p>
         * Must be called - an empty ServiceName is not valid.
         *
         * @param serviceName The service name for the subscribe session.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setServiceName(@NonNull String serviceName) {
            if (serviceName == null) {
                throw new IllegalArgumentException("Invalid service name - must be non-null");
            }
            mServiceName = serviceName.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        /**
         * Specify service specific information for the subscribe session. This is
         * a free-form byte array available to the application to send
         * additional information as part of the discovery operation - i.e. it
         * will not be used to determine whether a publish/subscribe match
         * occurs.
         * <p>
         *     Optional. Empty by default.
         *
         * @param serviceSpecificInfo A byte-array for the service-specific
         *            information field.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setServiceSpecificInfo(@Nullable byte[] serviceSpecificInfo) {
            mServiceSpecificInfo = serviceSpecificInfo;
            return this;
        }

        /**
         * The match filter for a subscribe session. Used to determine whether a service
         * discovery occurred - in addition to relying on the service name.
         * <p>
         *     Optional. Empty by default.
         *
         * @param matchFilter A list of match filter entries (each of which is an arbitrary byte
         *                    array).
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setMatchFilter(@Nullable List<byte[]> matchFilter) {
            mMatchFilter = new TlvBufferUtils.TlvConstructor(0, 1).allocateAndPut(
                    matchFilter).getArray();
            return this;
        }

        /**
         * Sets the type of the subscribe session: active (subscribe packets are
         * transmitted over-the-air), or passive (no subscribe packets are
         * transmitted, a match is made against a solicited/active publish
         * session whose packets are transmitted over-the-air).
         *
         * @param subscribeType Subscribe session type:
         *            {@link SubscribeConfig#SUBSCRIBE_TYPE_ACTIVE} or
         *            {@link SubscribeConfig#SUBSCRIBE_TYPE_PASSIVE}.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setSubscribeType(@SubscribeTypes int subscribeType) {
            if (subscribeType < SUBSCRIBE_TYPE_PASSIVE || subscribeType > SUBSCRIBE_TYPE_ACTIVE) {
                throw new IllegalArgumentException("Invalid subscribeType - " + subscribeType);
            }
            mSubscribeType = subscribeType;
            return this;
        }

        /**
         * Sets the time interval (in seconds) an active (
         * {@link SubscribeConfig.Builder#setSubscribeType(int)}) subscribe session
         * will be alive - i.e. broadcasting a packet. When the TTL is reached
         * an event will be generated for
         * {@link DiscoverySessionCallback#onSessionTerminated()}.
         * <p>
         *     Optional. 0 by default - indicating the session doesn't terminate on its own.
         *     Session will be terminated when {@link DiscoverySession#close()} is
         *     called.
         *
         * @param ttlSec Lifetime of a subscribe session in seconds.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setTtlSec(int ttlSec) {
            if (ttlSec < 0) {
                throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
            }
            mTtlSec = ttlSec;
            return this;
        }

        /**
         * Configure whether a subscribe terminate notification
         * {@link DiscoverySessionCallback#onSessionTerminated()} is reported
         * back to the callback.
         *
         * @param enable If true the terminate callback will be called when the
         *            subscribe is terminated. Otherwise it will not be called.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setTerminateNotificationEnabled(boolean enable) {
            mEnableTerminateNotification = enable;
            return this;
        }

        /**
         * Configure the minimum distance to a discovered publisher at which to trigger a discovery
         * notification. I.e. discovery will be triggered if we've found a matching publisher
         * (based on the other criteria in this configuration) <b>and</b> the distance to the
         * publisher is larger than the value specified in this API. Can be used in conjunction with
         * {@link #setMaxDistanceMm(int)} to specify a geofence, i.e. discovery with min <=
         * distance <= max.
         * <p>
         * For ranging to be used in discovery it must also be enabled on the publisher using
         * {@link PublishConfig.Builder#setRangingEnabled(boolean)}. However, ranging may
         * not be available or enabled on the publisher or may be temporarily disabled on either
         * subscriber or publisher - in such cases discovery will proceed without ranging.
         * <p>
         * When ranging is enabled and available on both publisher and subscriber and a service
         * is discovered based on geofence constraints the
         * {@link DiscoverySessionCallback#onServiceDiscoveredWithinRange(PeerHandle, byte[], List, int)}
         * is called, otherwise the
         * {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle, byte[], List)}
         * is called.
         * <p>
         * The device must support Wi-Fi RTT for this feature to be used. Feature support is checked
         * as described in {@link android.net.wifi.rtt}.
         *
         * @param minDistanceMm Minimum distance, in mm, to the publisher above which to trigger
         *                      discovery.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setMinDistanceMm(int minDistanceMm) {
            mMinDistanceMm = minDistanceMm;
            mMinDistanceMmSet = true;
            return this;
        }

        /**
         * Configure the maximum distance to a discovered publisher at which to trigger a discovery
         * notification. I.e. discovery will be triggered if we've found a matching publisher
         * (based on the other criteria in this configuration) <b>and</b> the distance to the
         * publisher is smaller than the value specified in this API. Can be used in conjunction
         * with {@link #setMinDistanceMm(int)} to specify a geofence, i.e. discovery with min <=
         * distance <= max.
         * <p>
         * For ranging to be used in discovery it must also be enabled on the publisher using
         * {@link PublishConfig.Builder#setRangingEnabled(boolean)}. However, ranging may
         * not be available or enabled on the publisher or may be temporarily disabled on either
         * subscriber or publisher - in such cases discovery will proceed without ranging.
         * <p>
         * When ranging is enabled and available on both publisher and subscriber and a service
         * is discovered based on geofence constraints the
         * {@link DiscoverySessionCallback#onServiceDiscoveredWithinRange(PeerHandle, byte[], List, int)}
         * is called, otherwise the
         * {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle, byte[], List)}
         * is called.
         * <p>
         * The device must support Wi-Fi RTT for this feature to be used. Feature support is checked
         * as described in {@link android.net.wifi.rtt}.
         *
         * @param maxDistanceMm Maximum distance, in mm, to the publisher below which to trigger
         *                      discovery.
         *
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder setMaxDistanceMm(int maxDistanceMm) {
            mMaxDistanceMm = maxDistanceMm;
            mMaxDistanceMmSet = true;
            return this;
        }

        /**
         * Configure whether to enable and use instant communication for this subscribe session.
         * Instant communication will speed up service discovery and any data-path set up as part of
         * this session. Use {@link Characteristics#isInstantCommunicationModeSupported()} to check
         * if the device supports this feature.
         *
         * <p>Note: due to increased power requirements of this mode - it will only remain enabled
         * for 30 seconds from the time the discovery session is started.
         *
         * @param enabled true for enable instant communication mode, default is false.
         * @param band When setting to {@link ScanResult#WIFI_BAND_5_GHZ}, device will try to enable
         *     instant communication mode on 5Ghz, but may fall back to 2.4Ghz due to regulatory
         *     requirements.
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @NonNull
        public Builder setInstantCommunicationModeEnabled(
                boolean enabled, @WifiAwareManager.InstantModeBand int band) {
            if (!SdkLevel.isAtLeastT()) {
                throw new UnsupportedOperationException();
            }
            if (band != ScanResult.WIFI_BAND_24_GHZ && band != ScanResult.WIFI_BAND_5_GHZ) {
                throw new IllegalArgumentException();
            }
            mBand = band;
            mEnableInstantMode = enabled;
            return this;
        }

        /**
         * Set the {@link AwarePairingConfig} for this subscribe session, the peer can use this info
         * to determine the config of the following bootstrapping, pairing setup/verification
         * request.
         * @see AwarePairingConfig
         * @param config The pairing config set to the peer. Only valid when
         * {@link Characteristics#isAwarePairingSupported()} is true.
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @NonNull public Builder setPairingConfig(@Nullable AwarePairingConfig config) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException();
            }
            mPairingConfig = config;
            return this;
        }

        /**
         * Specify whether to configure the subscribe discovery session to be suspendable. This API
         * doesn't suspend the session, it allows it to be suspended and resumed in the future using
         * {@link DiscoverySession#suspend()} and {@link DiscoverySession#resume()} respectively.
         * <p>
         * Optional. Not suspendable by default.
         * <p>
         * The device must support Wi-Fi Aware suspension for a subscribe session to be
         * suspendable. Feature support check is determined by
         * {@link Characteristics#isSuspensionSupported()}.
         *
         * @param isSuspendable If true, then this subscribe session can be suspended.
         *
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         *
         * @see DiscoverySession#suspend()
         * @see DiscoverySession#resume()
         * @hide
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @RequiresPermission(value = MANAGE_WIFI_NETWORK_SELECTION)
        @SystemApi
        @NonNull
        public Builder setSuspendable(boolean isSuspendable) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException();
            }
            mIsSuspendable = isSuspendable;
            return this;
        }

        /**
         * Set additional vendor-provided configuration data.
         *
         * @param vendorData List of {@link OuiKeyedData} containing the vendor-provided
         *     configuration data. Note that multiple elements with the same OUI are allowed.
         * @return Builder for chaining.
         * @hide
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        @SystemApi
        public Builder setVendorData(@NonNull List<OuiKeyedData> vendorData) {
            if (!SdkLevel.isAtLeastV()) {
                throw new UnsupportedOperationException();
            }
            if (vendorData == null) {
                throw new IllegalArgumentException("setVendorData received a null value");
            }
            mVendorData = vendorData;
            return this;
        }

        /**
         * Configure the interval for Wifi Aware periodic ranging.
         * <p>
         * To get the periodic ranging support use
         * {@link Characteristics#isPeriodicRangingSupported()}
         * When interval is not configured, default interval {@link PERIODIC_RANGING_INTERVAL_512TU}
         * is used.
         * </p>
         *
         * @param interval Ranging interval as described in {@link PeriodicRangingInterval}
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
        @SystemApi
        public Builder setPeriodicRangingInterval(@PeriodicRangingInterval int interval) {
            if (interval != PERIODIC_RANGING_INTERVAL_NONE
                    && interval != PERIODIC_RANGING_INTERVAL_128TU
                    && interval != PERIODIC_RANGING_INTERVAL_256TU
                    && interval != PERIODIC_RANGING_INTERVAL_512TU
                    && interval != PERIODIC_RANGING_INTERVAL_1024TU
                    && interval != PERIODIC_RANGING_INTERVAL_2048TU
                    && interval != PERIODIC_RANGING_INTERVAL_4096TU
                    && interval != PERIODIC_RANGING_INTERVAL_8192TU) {
                throw new IllegalArgumentException("Invalid Ranging interval - " + interval);
            }
            mPeriodicRangingInterval = interval;
            return this;
        }

        /**
         * Enable Wifi Aware periodic ranging.
         * <p>
         * To get the periodic ranging support use
         * {@link Characteristics#isPeriodicRangingSupported()}
         *
         * Wifi aware based periodic ranging allows continuous ranging report based on configured
         * interval through {@link #setPeriodicRangingInterval()}. To stop continuous ranging
         * results, reset the {@link #setPeriodicRangingEnabled()} and reconfigure using updated
         * {@link SubscribeDiscoverySession#updateSubscribe(SubscribeConfig)}
         * </p>
         *
         * @param enable Enable or disable periodic ranging report
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
        @SystemApi
        public Builder setPeriodicRangingEnabled(boolean enable) {
            mPeriodicRangingEnabled = enable;
            return this;
        }

        /**
         * Set the RTT Burst size for the Aware Periodic Ranging.
         * <p>
         * If not set, the default RTT burst size given by
         * {@link RangingRequest#getDefaultRttBurstSize()} is used to determine the default value.
         * If set, the value must be in the range {@link RangingRequest#getMinRttBurstSize()} and
         * {@link RangingRequest#getMaxRttBurstSize()} inclusively, or a
         * {@link java.lang.IllegalArgumentException} will be thrown.
         * </p>
         *
         * @param burstSize The number of FTM packets used to estimate a range
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
        @SystemApi
        public Builder setRttBurstSize(int burstSize) {
            if (burstSize < MIN_RTT_BURST_SIZE || burstSize > MAX_RTT_BURST_SIZE) {
                throw new IllegalArgumentException("RTT burst size out of range.");
            }
            mRttBurstSize = burstSize;
            return this;
        }

        /**
         * Sets the frequency of the channel in MHz.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * Peer/Publisher has changed.
         * </p>
         *
         * @param frequency the frequency of the channel in MHz
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
        @SystemApi
        public Builder setFrequencyMhz(@IntRange(from = 0) int frequency) {
            mFrequencyMhz = frequency;
            return this;
        }

        /**
         * Sets the center frequency in MHz of the first segment of the channel.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * Peer/Publisher has changed.
         * </p>
         *
         * @param centerFreq0 the center frequency in MHz of first channel segment
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
        @SystemApi
        public Builder setCenterFreq0Mhz(@IntRange(from = 0) int centerFreq0) {
            mCenterFrequency0Mhz = centerFreq0;
            return this;
        }

        /**
         * Sets the center frequency in MHz of the second segment of the channel, if used.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * Peer/Publisher has changed.
         * </p>
         *
         * @param centerFreq1 the center frequency in MHz of second channel segment
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
        @SystemApi
        public Builder setCenterFreq1Mhz(@IntRange(from = 0) int centerFreq1) {
            mCenterFrequency1Mhz = centerFreq1;
            return this;
        }

        /**
         * Sets the preamble encoding for the protocol.
         * <p>
         * Note: The preamble is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate based on negotiation of Peer capability or concurrency management.
         * </p>
         *
         * @param preamble the preamble encoding
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
        @SystemApi
        public Builder setPreamble(@WifiAnnotations.PreambleType int preamble) {
            mPreamble = ResponderConfig.translateFromScanResultToLocalPreamble(preamble);
            return this;
        }

        /**
         * Sets the channel bandwidth.
         * <p>
         * Note: The channel bandwidth is used as a hint, and the underlying WiFi subsystem may use
         * it, or select an alternate based on negotiation of Peer capability or concurrency
         * management.
         * </p>
         *
         * @param channelWidth the bandwidth of the channel in MHz
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
        @SystemApi
        public Builder setChannelWidth(@WifiAnnotations.ChannelWidth int channelWidth) {
            mChannelWidth =
                    ResponderConfig.translateFromScanResultToLocalChannelWidth(channelWidth);
            return this;
        }

        /**
         * Build {@link SubscribeConfig} given the current requests made on the
         * builder.
         */
        public SubscribeConfig build() {
            return new SubscribeConfig(mServiceName, mServiceSpecificInfo, mMatchFilter,
                    mSubscribeType, mTtlSec, mEnableTerminateNotification,
                    mMinDistanceMmSet, mMinDistanceMm, mMaxDistanceMmSet, mMaxDistanceMm,
                    mEnableInstantMode, mBand, mPairingConfig, mIsSuspendable, mVendorData,
                    mPeriodicRangingInterval, mPeriodicRangingEnabled, mRttBurstSize,
                    mFrequencyMhz, mCenterFrequency0Mhz, mCenterFrequency1Mhz, mPreamble,
                    mChannelWidth);

        }
    }
}
