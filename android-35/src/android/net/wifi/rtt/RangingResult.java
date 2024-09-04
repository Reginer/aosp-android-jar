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

package android.net.wifi.rtt;

import android.annotation.ElapsedRealtimeLong;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.wifi.OuiKeyedData;
import android.net.wifi.ParcelUtil;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiAnnotations.ChannelWidth;
import android.net.wifi.aware.PeerHandle;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ranging result for a request started by
 * {@link WifiRttManager#startRanging(RangingRequest, java.util.concurrent.Executor, RangingResultCallback)}.
 * Results are returned in {@link RangingResultCallback#onRangingResults(List)}.
 * <p>
 * A ranging result is the distance measurement result for a single device specified in the
 * {@link RangingRequest}.
 */
public final class RangingResult implements Parcelable {
    private static final String TAG = "RangingResult";
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /** @hide */
    @IntDef({STATUS_SUCCESS, STATUS_FAIL, STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RangeResultStatus {
    }

    /**
     * Individual range request status, {@link #getStatus()}. Indicates ranging operation was
     * successful and distance value is valid.
     */
    public static final int STATUS_SUCCESS = 0;

    /**
     * Individual range request status, {@link #getStatus()}. Indicates ranging operation failed
     * and the distance value is invalid.
     */
    public static final int STATUS_FAIL = 1;

    /**
     * Individual range request status, {@link #getStatus()}. Indicates that the ranging operation
     * failed because the specified peer does not support IEEE 802.11mc RTT operations. Support by
     * an Access Point can be confirmed using
     * {@link android.net.wifi.ScanResult#is80211mcResponder()}.
     * <p>
     * On such a failure, the individual result fields of {@link RangingResult} such as
     * {@link RangingResult#getDistanceMm()} are invalid.
     */
    public static final int STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC = 2;

    /**
     * The unspecified value.
     */
    public static final int UNSPECIFIED = -1;

    private final @RangeResultStatus int mStatus;
    private final MacAddress mMac;
    private final PeerHandle mPeerHandle;
    private final int mDistanceMm;
    private final int mDistanceStdDevMm;
    private final int mRssi;
    private final int mNumAttemptedMeasurements;
    private final int mNumSuccessfulMeasurements;
    private final byte[] mLci;
    private final byte[] mLcr;
    private final ResponderLocation mResponderLocation;
    private final long mTimestamp;
    private final boolean mIs80211mcMeasurement;
    private final int mFrequencyMHz;
    private final int mPacketBw;
    private final boolean mIs80211azNtbMeasurement;
    private final long mNtbMinMeasurementTime;
    private final long mNtbMaxMeasurementTime;
    private final int mI2rTxLtfRepetitions;
    private final int mR2iTxLtfRepetitions;
    private final int mNumTxSpatialStreams;
    private final int mNumRxSpatialStreams;
    private List<OuiKeyedData> mVendorData;

    /**
     * Builder class used to construct {@link RangingResult} objects.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final class Builder {
        private @RangeResultStatus int mStatus = STATUS_FAIL;
        private MacAddress mMac = null;
        private PeerHandle mPeerHandle = null;
        private int mDistanceMm = 0;
        private int mDistanceStdDevMm = 0;
        private int mRssi = -127;
        private int mNumAttemptedMeasurements = 0;
        private int mNumSuccessfulMeasurements = 0;
        private byte[] mLci = null;
        private  byte[] mLcr = null;
        private ResponderLocation mResponderLocation = null;
        private long mTimestamp = 0;
        private boolean mIs80211mcMeasurement = false;
        private int mFrequencyMHz = UNSPECIFIED;
        private int mPacketBw = UNSPECIFIED;
        private boolean mIs80211azNtbMeasurement = false;
        private long mNtbMinMeasurementTime = UNSPECIFIED;
        private long mNtbMaxMeasurementTime = UNSPECIFIED;
        private int mI2rTxLtfRepetitions = UNSPECIFIED;
        private int mR2iTxLtfRepetitions = UNSPECIFIED;
        private int mNumTxSpatialStreams = UNSPECIFIED;
        private int mNumRxSpatialStreams = UNSPECIFIED;
        private List<OuiKeyedData> mVendorData = Collections.emptyList();

        /**
         * Sets the Range result status.
         *
         * @param status Ranging result status, if not set defaults to
         *               {@link #STATUS_FAIL}.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setStatus(@RangeResultStatus int status) {
            mStatus = status;
            return this;
        }

        /**
         * Sets the MAC address of the ranging result.
         *
         * @param macAddress Mac address, if not defaults to null.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setMacAddress(@Nullable MacAddress macAddress) {
            mMac = macAddress;
            return this;
        }


        /**
         * Sets the peer handle. Applicable only for NAN Ranging.
         *
         * @param peerHandle Opaque object used to represent a Wi-Fi Aware peer. If not set,
         *                   defaults to null.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setPeerHandle(@Nullable PeerHandle peerHandle) {
            mPeerHandle = peerHandle;
            return this;
        }

        /**
         * Sets the distance in millimeter.
         *
         * @param distanceMm distance. If not set, defaults to 0.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setDistanceMm(int distanceMm) {
            mDistanceMm = distanceMm;
            return this;
        }

        /**
         * Sets the standard deviation of the distance in millimeter.
         *
         * @param distanceStdDevMm Standard deviation of the distance measurement. If not set
         *                         defaults to 0.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setDistanceStdDevMm(int distanceStdDevMm) {
            mDistanceStdDevMm = distanceStdDevMm;
            return this;
        }

        /**
         * Sets the average RSSI.
         *
         * @param rssi Average RSSI. If not set, defaults to -127.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setRssi(int rssi) {
            mRssi = rssi;
            return this;
        }

        /**
         * Sets the total number of RTT measurements attempted.
         *
         * @param numAttemptedMeasurements Number of attempted measurements. If not set, default
         *                                 to 0.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setNumAttemptedMeasurements(int numAttemptedMeasurements) {
            mNumAttemptedMeasurements = numAttemptedMeasurements;
            return this;
        }

        /**
         * Sets the total number of successful RTT measurements.
         *
         * @param numSuccessfulMeasurements Number of successful measurements. If not set, default
         *                                 to 0.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setNumSuccessfulMeasurements(int numSuccessfulMeasurements) {
            mNumSuccessfulMeasurements = numSuccessfulMeasurements;
            return this;
        }

        /**
         * Sets the Location Configuration Information (LCI).
         *
         * LCI provides data about the access point's (AP) physical location, such as its
         * latitude, longitude, and altitude. The format is specified in the IEEE 802.11-2016
         * specifications, section 9.4.2.22.10.
         *
         * @param lci Location configuration information. If not set, defaults to null.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setLci(@Nullable byte[] lci) {
            mLci = lci;
            return this;
        }

        /**
         * Sets the Location Civic Report (LCR).
         *
         * LCR provides additional details about the AP's location in a human-readable format,
         * such as the street address, building name, or floor number. This can be helpful for
         * users to understand the context of their location within a building or complex.
         *
         * The format is
         * specified in the IEEE 802.11-2016 specifications, section 9.4.2.22.13.
         *
         * @param lcr Location civic report. If not set, defaults to null.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setLcr(@Nullable byte[] lcr) {
            mLcr = lcr;
            return this;
        }

        /**
         * Sets Responder Location.
         *
         * ResponderLocation is both a Location Configuration Information (LCI) decoder and a
         * Location Civic Report (LCR) decoder for information received from a Wi-Fi Access Point
         * (AP) during Wi-Fi RTT ranging process.
         *
         * @param responderLocation Responder location. If not set, defaults to null.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setUnverifiedResponderLocation(
                @Nullable ResponderLocation responderLocation) {
            mResponderLocation = responderLocation;
            return this;
        }

        /**
         * Sets the time stamp at which the ranging operation was performed.
         *
         * The timestamp is in milliseconds since boot, including time spent in sleep,
         * corresponding to values provided by {@link android.os.SystemClock#elapsedRealtime()}.
         *
         * @param timestamp time stamp in milliseconds. If not set, default to 0.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setRangingTimestampMillis(@ElapsedRealtimeLong long timestamp) {
            mTimestamp = timestamp;
            return this;
        }


        /**
         * Sets whether the ranging measurement was performed using IEEE 802.11mc ranging method.
         * If {@link #set80211mcMeasurement(boolean)} is set as false and
         * {@link #set80211azNtbMeasurement(boolean)} is also set as false, ranging measurement was
         * performed using one-side RTT. If not set, default to false.
         *
         * @param is80211mcMeasurement true for IEEE 802.11mc measure, otherwise false.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder set80211mcMeasurement(boolean is80211mcMeasurement) {
            mIs80211mcMeasurement = is80211mcMeasurement;
            return this;
        }

        /**
         * Sets the center frequency of the primary 20 MHz frequency (in MHz) of the channel over
         * which the measurement frames are sent. If not set, default to
         * {@link RangingResult#UNSPECIFIED}
         *
         * @param frequencyMHz Frequency.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setMeasurementChannelFrequencyMHz(int frequencyMHz) {
            mFrequencyMHz = frequencyMHz;
            return this;
        }

        /**
         * Sets the bandwidth used to transmit the RTT measurement frame. If not set, default to
         * {@link RangingResult#UNSPECIFIED}.
         *
         * @param measurementBandwidth Measurement bandwidth.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setMeasurementBandwidth(@ChannelWidth int measurementBandwidth) {
            mPacketBw = measurementBandwidth;
            return this;
        }

        /**
         * Sets whether the ranging measurement was performed using IEEE 802.11az non-trigger
         * ranging method. If {@link #set80211azNtbMeasurement(boolean)} is set as false and
         * {@link #set80211mcMeasurement(boolean)} is also set as false, ranging measurement was
         * performed using one-side RTT. If not set defaults to false.
         *
         * @param is80211azNtbMeasurement true for IEEE 802.11az non-trigger based measurement,
         *                                otherwise false.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder set80211azNtbMeasurement(boolean is80211azNtbMeasurement) {
            mIs80211azNtbMeasurement = is80211azNtbMeasurement;
            return this;
        }

        /**
         * Sets minimum time between measurements in microseconds for IEEE 802.11az non-trigger
         * based ranging.  If not set, defaults to {@link RangingResult#UNSPECIFIED}.
         *
         * @param ntbMinMeasurementTime non-trigger based ranging minimum measurement time.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setMinTimeBetweenNtbMeasurementsMicros(long ntbMinMeasurementTime) {
            mNtbMinMeasurementTime = ntbMinMeasurementTime;
            return this;
        }

        /**
         * Sets maximum time between measurements in microseconds for IEEE 802.11az non-trigger
         * based ranging. If not set, defaults to {@link RangingResult#UNSPECIFIED}.
         *
         * @param ntbMaxMeasurementTime non-trigger based ranging maximum measurement time.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setMaxTimeBetweenNtbMeasurementsMicros(long ntbMaxMeasurementTime) {
            mNtbMaxMeasurementTime = ntbMaxMeasurementTime;
            return this;
        }

        /**
         * Sets LTF repetitions that the initiator station used in the preamble. If not set,
         * defaults to {@link RangingResult#UNSPECIFIED}.
         *
         * @param i2rTxLtfRepetitions LFT repetition count.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder set80211azInitiatorTxLtfRepetitionsCount(int i2rTxLtfRepetitions) {
            mI2rTxLtfRepetitions = i2rTxLtfRepetitions;
            return this;
        }

        /**
         * Sets LTF repetitions that the responder station used in the preamble. If not set,
         * defaults to {@link RangingResult#UNSPECIFIED}.
         *
         * @param r2iTxLtfRepetitions LFT repetition count.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder set80211azResponderTxLtfRepetitionsCount(int r2iTxLtfRepetitions) {
            mR2iTxLtfRepetitions = r2iTxLtfRepetitions;
            return this;
        }

        /**
         * Sets number of transmit spatial streams that the initiator station used for the
         * ranging result. If not set, defaults to {@link RangingResult#UNSPECIFIED}.
         *
         * @param numTxSpatialStreams Number of transmit spatial streams.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder set80211azNumberOfTxSpatialStreams(int numTxSpatialStreams) {
            mNumTxSpatialStreams = numTxSpatialStreams;
            return this;
        }

        /**
         * Sets number of receive spatial streams that the initiator station used for the ranging
         * result. If not set, defaults to {@link RangingResult#UNSPECIFIED}.
         *
         * @param numRxSpatialStreams Number of receive spatial streams.
         * @return The builder to facilitate chaining.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder set80211azNumberOfRxSpatialStreams(int numRxSpatialStreams) {
            mNumRxSpatialStreams = numRxSpatialStreams;
            return this;
        }

        /**
         * Set additional vendor-provided configuration data.
         *
         * @param vendorData List of {@link android.net.wifi.OuiKeyedData} containing the
         *                   vendor-provided configuration data. Note that multiple elements with
         *                   the same OUI are allowed.
         * @hide
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @SystemApi
        @NonNull
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
         * Build {@link RangingResult}
         * @return an instance of {@link RangingResult}
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public RangingResult build() {
            if (mMac == null && mPeerHandle == null) {
                throw new IllegalArgumentException("Either MAC address or Peer handle is needed");
            }
            if (mIs80211azNtbMeasurement && mIs80211mcMeasurement) {
                throw new IllegalArgumentException(
                        "A ranging result cannot use both IEEE 802.11mc and IEEE 802.11az "
                                + "measurements simultaneously");
            }
            return new RangingResult(this);
        }
    }

    /** @hide */
    private RangingResult(Builder builder) {
        mStatus = builder.mStatus;
        mMac = builder.mMac;
        mPeerHandle = builder.mPeerHandle;
        mDistanceMm = builder.mDistanceMm;
        mDistanceStdDevMm = builder.mDistanceStdDevMm;
        mRssi = builder.mRssi;
        mNumAttemptedMeasurements = builder.mNumAttemptedMeasurements;
        mNumSuccessfulMeasurements = builder.mNumSuccessfulMeasurements;
        mLci = (builder.mLci == null) ? EMPTY_BYTE_ARRAY : builder.mLci;
        mLcr = (builder.mLcr == null) ? EMPTY_BYTE_ARRAY : builder.mLcr;
        mResponderLocation = builder.mResponderLocation;
        mTimestamp = builder.mTimestamp;
        mIs80211mcMeasurement = builder.mIs80211mcMeasurement;
        mFrequencyMHz = builder.mFrequencyMHz;
        mPacketBw = builder.mPacketBw;
        mIs80211azNtbMeasurement = builder.mIs80211azNtbMeasurement;
        mNtbMinMeasurementTime = builder.mNtbMinMeasurementTime;
        mNtbMaxMeasurementTime = builder.mNtbMaxMeasurementTime;
        mI2rTxLtfRepetitions = builder.mI2rTxLtfRepetitions;
        mR2iTxLtfRepetitions = builder.mR2iTxLtfRepetitions;
        mNumRxSpatialStreams = builder.mNumRxSpatialStreams;
        mNumTxSpatialStreams = builder.mNumTxSpatialStreams;
        mVendorData = builder.mVendorData;
    }

    /**
     * @return The status of ranging measurement: {@link #STATUS_SUCCESS} in case of success, and
     * {@link #STATUS_FAIL} in case of failure.
     */
    @RangeResultStatus
    public int getStatus() {
        return mStatus;
    }

    /**
     * @return The MAC address of the device whose range measurement was requested. Will correspond
     * to the MAC address of the device in the {@link RangingRequest}.
     * <p>
     * Will return a {@code null} for results corresponding to requests issued using a {@code
     * PeerHandle}, i.e. using the {@link RangingRequest.Builder#addWifiAwarePeer(PeerHandle)} API.
     */
    @Nullable
    public MacAddress getMacAddress() {
        return mMac;
    }

    /**
     * @return The PeerHandle of the device whose reange measurement was requested. Will correspond
     * to the PeerHandle of the devices requested using
     * {@link RangingRequest.Builder#addWifiAwarePeer(PeerHandle)}.
     * <p>
     * Will return a {@code null} for results corresponding to requests issued using a MAC address.
     */
    @Nullable public PeerHandle getPeerHandle() {
        return mPeerHandle;
    }

    /**
     * @return The distance (in mm) to the device specified by {@link #getMacAddress()} or
     * {@link #getPeerHandle()}.
     * <p>
     * Note: the measured distance may be negative for very close devices.
     * <p>
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception.
     */
    public int getDistanceMm() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getDistanceMm(): invoked on an invalid result: getStatus()=" + mStatus);
        }
        return mDistanceMm;
    }

    /**
     * @return The standard deviation of the measured distance (in mm) to the device specified by
     * {@link #getMacAddress()} or {@link #getPeerHandle()}. The standard deviation is calculated
     * over the measurements executed in a single RTT burst. The number of measurements is returned
     * by {@link #getNumSuccessfulMeasurements()} - 0 successful measurements indicate that the
     * standard deviation is not valid (a valid standard deviation requires at least 2 data points).
     * <p>
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception.
     */
    public int getDistanceStdDevMm() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getDistanceStdDevMm(): invoked on an invalid result: getStatus()=" + mStatus);
        }
        return mDistanceStdDevMm;
    }

    /**
     * @return The average RSSI, in units of dBm, observed during the RTT measurement.
     * <p>
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception.
     */
    public int getRssi() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getRssi(): invoked on an invalid result: getStatus()=" + mStatus);
        }
        return mRssi;
    }

    /**
     * @return The number of attempted measurements used in the RTT exchange resulting in this set
     * of results. The number of successful measurements is returned by
     * {@link #getNumSuccessfulMeasurements()} which at most, if there are no errors, will be 1
     * less than the number of attempted measurements.
     * <p>
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception. If the value is 0, it should be interpreted as no information available, which may
     * occur for one-sided RTT measurements. Instead {@link RangingRequest#getRttBurstSize()}
     * should be used instead.
     */
    public int getNumAttemptedMeasurements() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getNumAttemptedMeasurements(): invoked on an invalid result: getStatus()="
                            + mStatus);
        }
        return mNumAttemptedMeasurements;
    }

    /**
     * @return The number of successful measurements used to calculate the distance and standard
     * deviation. If the number of successful measurements if 1 then then standard deviation,
     * returned by {@link #getDistanceStdDevMm()}, is not valid (a 0 is returned for the standard
     * deviation).
     * <p>
     * The total number of measurement attempts is returned by
     * {@link #getNumAttemptedMeasurements()}. The number of successful measurements will be at
     * most 1 less then the number of attempted measurements.
     * <p>
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception.
     */
    public int getNumSuccessfulMeasurements() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getNumSuccessfulMeasurements(): invoked on an invalid result: getStatus()="
                            + mStatus);
        }
        return mNumSuccessfulMeasurements;
    }

    /**
     * @return The unverified responder location represented as {@link ResponderLocation} which
     * captures location information the responder is programmed to broadcast. The responder
     * location is referred to as unverified, because we are relying on the device/site
     * administrator to correctly configure its location data.
     * <p>
     * Will return a {@code null} when the location information cannot be parsed.
     * <p>
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception.
     */
    @Nullable
    public ResponderLocation getUnverifiedResponderLocation() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getUnverifiedResponderLocation(): invoked on an invalid result: getStatus()="
                            + mStatus);
        }
        return mResponderLocation;
    }

    /**
     * @return The Location Configuration Information (LCI) as self-reported by the peer. The format
     * is specified in the IEEE 802.11-2016 specifications, section 9.4.2.22.10.
     * <p>
     * Note: the information is NOT validated - use with caution. Consider validating it with
     * other sources of information before using it.
     */
    @SuppressLint("UnflaggedApi") // Flagging API promotion from @SystemApi to public not supported
    @NonNull
    public byte[] getLci() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getLci(): invoked on an invalid result: getStatus()=" + mStatus);
        }
        return mLci;
    }

    /**
     * @return The Location Civic report (LCR) as self-reported by the peer. The format
     * is specified in the IEEE 802.11-2016 specifications, section 9.4.2.22.13.
     * <p>
     * Note: the information is NOT validated - use with caution. Consider validating it with
     * other sources of information before using it.
     */
    @SuppressLint("UnflaggedApi") // Flagging API promotion from @SystemApi to public not supported
    @NonNull
    public byte[] getLcr() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getReportedLocationCivic(): invoked on an invalid result: getStatus()="
                            + mStatus);
        }
        return mLcr;
    }

    /**
     * @return The timestamp at which the ranging operation was performed. The timestamp is in
     * milliseconds since boot, including time spent in sleep, corresponding to values provided by
     * {@link android.os.SystemClock#elapsedRealtime()}.
     * <p>
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception.
     */
    public long getRangingTimestampMillis() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getRangingTimestampMillis(): invoked on an invalid result: getStatus()="
                            + mStatus);
        }
        return mTimestamp;
    }

    /**
     * @return The result is true if the IEEE 802.11mc protocol was used. If the result is false,
     * and {@link #is80211azNtbMeasurement()} is also false a one-side RTT result is provided
     * which does not subtract the turnaround time at the responder.
     * <p>
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception.
     */
    public boolean is80211mcMeasurement() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "is80211mcMeasurementResult(): invoked on an invalid result: getStatus()="
                            + mStatus);
        }
        return mIs80211mcMeasurement;
    }

    /**
     * @return The result is true if the IEEE 802.11az non-trigger based protocol was used. If the
     * result is false, and {@link #is80211mcMeasurement()} is also false a one-side RTT result
     * is provided which does not subtract the turnaround time at the responder.
     * <p>.
     * Only valid if {@link #getStatus()} returns {@link #STATUS_SUCCESS}, otherwise will throw an
     * exception.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public boolean is80211azNtbMeasurement() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "is80211azNtbMeasurement(): invoked on an invalid result: getStatus()="
                            + mStatus);
        }
        return mIs80211azNtbMeasurement;
    }

    /**
     * Gets minimum time between measurements in microseconds for IEEE 802.11az non-trigger based
     * ranging.
     *
     * The next 11az ranging measurement request must be invoked after the minimum time from the
     * last measurement time {@link #getRangingTimestampMillis()} for the peer. Otherwise, cached
     * ranging result will be returned for the peer.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public long getMinTimeBetweenNtbMeasurementsMicros() {
        return mNtbMinMeasurementTime;
    }

    /**
     * Gets maximum time between measurements in microseconds for IEEE 802.11az non-trigger based
     * ranging.
     *
     * The next 11az ranging request needs to be invoked before the maximum time from the last
     * measurement time {@link #getRangingTimestampMillis()}. Otherwise, the non-trigger based
     * ranging session will be terminated and a new ranging negotiation will happen with
     * the responding station.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public long getMaxTimeBetweenNtbMeasurementsMicros() {
        return mNtbMaxMeasurementTime;
    }

    /**
     * Gets LTF repetitions that the responder station (RSTA) used in the preamble of the
     * responder to initiator (I2R) null data PPDU (NDP) for this result.
     *
     * LTF repetitions is the multiple transmissions of HE-LTF symbols in an HE ranging NDP. An
     * HE-LTF repetition value of 1 indicates no repetitions.
     *
     * @return LTF repetitions count
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int get80211azResponderTxLtfRepetitionsCount() {
        return mR2iTxLtfRepetitions;
    }

    /**
     * Gets LTF repetitions that the initiator station (ISTA) used in the preamble of the
     * initiator to responder (I2R) null data PPDU (NDP) for this result.
     *
     * LTF repetitions is the multiple transmissions of HE-LTF symbols in an HE ranging NDP. An
     * HE-LTF repetition value of 1 indicates no repetitions.
     *
     * @return LTF repetitions count
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int get80211azInitiatorTxLtfRepetitionsCount() {
        return mI2rTxLtfRepetitions;
    }

    /**
     * Gets number of transmit spatial streams that the initiator station (ISTA) used for the
     * initiator to responder (I2R) null data PPDU (NDP) for this result.
     *
     * @return Number of spatial streams
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int get80211azNumberOfTxSpatialStreams() {
        return mNumTxSpatialStreams;
    }

    /**
     * Gets number of receive spatial streams that the initiator station (ISTA) used for the
     * initiator to responder (I2R) null data PPDU (NDP) for this result.
     *
     * @return Number of spatial streams
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int get80211azNumberOfRxSpatialStreams() {
        return mNumRxSpatialStreams;
    }

    /**
     * The center frequency of the primary 20 MHz frequency (in MHz) of the channel over
     * which the measurement frames are sent.
     * @return center frequency in Mhz of the channel if available, otherwise {@link #UNSPECIFIED}
     * is returned.
     * <p>
     * @throws IllegalStateException if {@link #getStatus()} does not return
     * {@link #STATUS_SUCCESS}.
     */
    public int getMeasurementChannelFrequencyMHz() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getMeasurementChannelFrequencyMHz():"
                    + " invoked on an invalid result: getStatus()= " + mStatus);
        }
        return mFrequencyMHz;
    }

    /**
     * The bandwidth used to transmit the RTT measurement frame.
     * @return one of {@link ScanResult#CHANNEL_WIDTH_20MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_40MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_80MHZ}, {@link ScanResult#CHANNEL_WIDTH_160MHZ},
     * {@link ScanResult #CHANNEL_WIDTH_80MHZ_PLUS_MHZ} or {@link ScanResult #CHANNEL_WIDTH_320MHZ}
     * if available, otherwise {@link #UNSPECIFIED} is returned.
     * <p>
     * @throws IllegalStateException if {@link #getStatus()} does not return
     * {@link #STATUS_SUCCESS}.
     */
    public @ChannelWidth int getMeasurementBandwidth() {
        if (mStatus != STATUS_SUCCESS) {
            throw new IllegalStateException(
                    "getMeasurementBandwidth(): invoked on an invalid result: getStatus()="
                            + mStatus);
        }
        return mPacketBw;
    }

    /**
     * Get the vendor-provided configuration data, if it exists.
     *
     * @return Vendor configuration data, or empty list if it does not exist.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @SystemApi
    @NonNull
    public List<OuiKeyedData> getVendorData() {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        return mVendorData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mStatus);
        if (mMac == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            mMac.writeToParcel(dest, flags);
        }
        if (mPeerHandle == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            dest.writeInt(mPeerHandle.peerId);
        }
        dest.writeInt(mDistanceMm);
        dest.writeInt(mDistanceStdDevMm);
        dest.writeInt(mRssi);
        dest.writeInt(mNumAttemptedMeasurements);
        dest.writeInt(mNumSuccessfulMeasurements);
        dest.writeByteArray(mLci);
        dest.writeByteArray(mLcr);
        dest.writeParcelable(mResponderLocation, flags);
        dest.writeLong(mTimestamp);
        dest.writeBoolean(mIs80211mcMeasurement);
        dest.writeInt(mFrequencyMHz);
        dest.writeInt(mPacketBw);
        dest.writeBoolean(mIs80211azNtbMeasurement);
        dest.writeLong(mNtbMinMeasurementTime);
        dest.writeLong(mNtbMaxMeasurementTime);
        dest.writeInt(mI2rTxLtfRepetitions);
        dest.writeInt(mR2iTxLtfRepetitions);
        dest.writeInt(mNumTxSpatialStreams);
        dest.writeInt(mNumRxSpatialStreams);
        if (SdkLevel.isAtLeastV()) {
            dest.writeList(mVendorData);
        }
    }

    public static final @android.annotation.NonNull Creator<RangingResult> CREATOR =
            new Creator<RangingResult>() {
                @Override
                public RangingResult[] newArray(int size) {
                    return new RangingResult[size];
                }

                @Override
                public RangingResult createFromParcel(Parcel in) {
                    RangingResult.Builder builder = new Builder()
                            .setStatus(in.readInt())
                            .setMacAddress(
                                    in.readBoolean() ? MacAddress.CREATOR.createFromParcel(in)
                                            : null)
                            .setPeerHandle(in.readBoolean() ? new PeerHandle(in.readInt()) : null)
                            .setDistanceMm(in.readInt())
                            .setDistanceStdDevMm(in.readInt())
                            .setRssi(in.readInt())
                            .setNumAttemptedMeasurements(in.readInt())
                            .setNumSuccessfulMeasurements(in.readInt())
                            .setLci(in.createByteArray())
                            .setLcr(in.createByteArray())
                            .setUnverifiedResponderLocation(
                                    in.readParcelable(this.getClass().getClassLoader()))
                            .setRangingTimestampMillis(in.readLong())
                            .set80211mcMeasurement(in.readBoolean())
                            .setMeasurementChannelFrequencyMHz(in.readInt())
                            .setMeasurementBandwidth(in.readInt())
                            .set80211azNtbMeasurement(in.readBoolean())
                            .setMinTimeBetweenNtbMeasurementsMicros(in.readLong())
                            .setMaxTimeBetweenNtbMeasurementsMicros(in.readLong())
                            .set80211azInitiatorTxLtfRepetitionsCount(in.readInt())
                            .set80211azResponderTxLtfRepetitionsCount(in.readInt())
                            .set80211azNumberOfTxSpatialStreams(in.readInt())
                            .set80211azNumberOfRxSpatialStreams(in.readInt());
                    if (SdkLevel.isAtLeastV()) {
                        builder.setVendorData(ParcelUtil.readOuiKeyedDataList(in));
                    }
                    return builder.build();
                }
            };

    /** @hide */
    @Override
    public String toString() {
        return new StringBuilder("RangingResult: [status=").append(mStatus)
                .append(", mac=").append(mMac)
                .append(", peerHandle=").append(
                        mPeerHandle == null ? "<null>" : mPeerHandle.peerId)
                .append(", distanceMm=").append(mDistanceMm)
                .append(", distanceStdDevMm=").append(mDistanceStdDevMm)
                .append(", rssi=").append(mRssi)
                .append(", numAttemptedMeasurements=").append(mNumAttemptedMeasurements)
                .append(", numSuccessfulMeasurements=").append(mNumSuccessfulMeasurements)
                .append(", lci=").append(Arrays.toString(mLci))
                .append(", lcr=").append(Arrays.toString(mLcr))
                .append(", responderLocation=").append(mResponderLocation)
                .append(", timestamp=").append(mTimestamp).append(", is80211mcMeasurement=")
                .append(mIs80211mcMeasurement)
                .append(", frequencyMHz=").append(mFrequencyMHz)
                .append(", packetBw=").append(mPacketBw)
                .append("is80211azNtbMeasurement").append(mIs80211azNtbMeasurement)
                .append("ntbMinMeasurementTime").append(mNtbMinMeasurementTime)
                .append("ntbMaxMeasurementTime").append(mNtbMaxMeasurementTime)
                .append("i2rTxLtfRepetitions").append(mI2rTxLtfRepetitions)
                .append("r2iTxLtfRepetitions").append(mR2iTxLtfRepetitions)
                .append("txSpatialStreams").append(mNumTxSpatialStreams)
                .append("rxSpatialStreams").append(mNumRxSpatialStreams)
                .append(", vendorData=").append(mVendorData)
                .append("]").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof RangingResult)) {
            return false;
        }

        RangingResult lhs = (RangingResult) o;

        return mStatus == lhs.mStatus && Objects.equals(mMac, lhs.mMac) && Objects.equals(
                mPeerHandle, lhs.mPeerHandle) && mDistanceMm == lhs.mDistanceMm
                && mDistanceStdDevMm == lhs.mDistanceStdDevMm && mRssi == lhs.mRssi
                && mNumAttemptedMeasurements == lhs.mNumAttemptedMeasurements
                && mNumSuccessfulMeasurements == lhs.mNumSuccessfulMeasurements
                && Arrays.equals(mLci, lhs.mLci) && Arrays.equals(mLcr, lhs.mLcr)
                && mTimestamp == lhs.mTimestamp
                && mIs80211mcMeasurement == lhs.mIs80211mcMeasurement
                && Objects.equals(mResponderLocation, lhs.mResponderLocation)
                && mFrequencyMHz == lhs.mFrequencyMHz
                && mPacketBw == lhs.mPacketBw
                && mIs80211azNtbMeasurement == lhs.mIs80211azNtbMeasurement
                && mNtbMinMeasurementTime == lhs.mNtbMinMeasurementTime
                && mNtbMaxMeasurementTime == lhs.mNtbMaxMeasurementTime
                && mI2rTxLtfRepetitions == lhs.mI2rTxLtfRepetitions
                && mR2iTxLtfRepetitions == lhs.mR2iTxLtfRepetitions
                && mNumTxSpatialStreams == lhs.mNumTxSpatialStreams
                && mNumRxSpatialStreams == lhs.mNumRxSpatialStreams
                && Objects.equals(mVendorData, lhs.mVendorData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStatus, mMac, mPeerHandle, mDistanceMm, mDistanceStdDevMm, mRssi,
                mNumAttemptedMeasurements, mNumSuccessfulMeasurements, Arrays.hashCode(mLci),
                Arrays.hashCode(mLcr), mResponderLocation, mTimestamp, mIs80211mcMeasurement,
                mFrequencyMHz, mPacketBw, mIs80211azNtbMeasurement, mNtbMinMeasurementTime,
                mNtbMaxMeasurementTime, mI2rTxLtfRepetitions, mR2iTxLtfRepetitions,
                mNumTxSpatialStreams, mR2iTxLtfRepetitions, mVendorData);
    }
}
