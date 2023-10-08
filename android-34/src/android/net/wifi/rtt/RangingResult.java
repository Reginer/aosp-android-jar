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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiAnnotations.ChannelWidth;
import android.net.wifi.aware.PeerHandle;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
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

    /** @hide */
    public final int mStatus;

    /** @hide */
    public final MacAddress mMac;

    /** @hide */
    public final PeerHandle mPeerHandle;

    /** @hide */
    public final int mDistanceMm;

    /** @hide */
    public final int mDistanceStdDevMm;

    /** @hide */
    public final int mRssi;

    /** @hide */
    public final int mNumAttemptedMeasurements;

    /** @hide */
    public final int mNumSuccessfulMeasurements;

    /** @hide */
    public final byte[] mLci;

    /** @hide */
    public final byte[] mLcr;

    /** @hide */
    public final ResponderLocation mResponderLocation;

    /** @hide */
    public final long mTimestamp;

    /** @hide */
    public final boolean mIs80211mcMeasurement;

    /** @hide */
    public final int mFrequencyMHz;

    /** @hide */
    public final int mPacketBw;

    /** @hide */
    public RangingResult(@RangeResultStatus int status, @NonNull MacAddress mac, int distanceMm,
            int distanceStdDevMm, int rssi, int numAttemptedMeasurements,
            int numSuccessfulMeasurements, byte[] lci, byte[] lcr,
            ResponderLocation responderLocation, long timestamp, boolean is80211McMeasurement,
            int frequencyMHz, int packetBw) {
        mStatus = status;
        mMac = mac;
        mPeerHandle = null;
        mDistanceMm = distanceMm;
        mDistanceStdDevMm = distanceStdDevMm;
        mRssi = rssi;
        mNumAttemptedMeasurements = numAttemptedMeasurements;
        mNumSuccessfulMeasurements = numSuccessfulMeasurements;
        mLci = lci == null ? EMPTY_BYTE_ARRAY : lci;
        mLcr = lcr == null ? EMPTY_BYTE_ARRAY : lcr;
        mResponderLocation = responderLocation;
        mTimestamp = timestamp;
        mIs80211mcMeasurement = is80211McMeasurement;
        mFrequencyMHz = frequencyMHz;
        mPacketBw = packetBw;
    }

    /** @hide */
    public RangingResult(@RangeResultStatus int status, @NonNull MacAddress mac, int distanceMm,
            int distanceStdDevMm, int rssi, int numAttemptedMeasurements,
            int numSuccessfulMeasurements, byte[] lci, byte[] lcr,
            ResponderLocation responderLocation, long timestamp, boolean is80211McMeasurement) {
        mStatus = status;
        mMac = mac;
        mPeerHandle = null;
        mDistanceMm = distanceMm;
        mDistanceStdDevMm = distanceStdDevMm;
        mRssi = rssi;
        mNumAttemptedMeasurements = numAttemptedMeasurements;
        mNumSuccessfulMeasurements = numSuccessfulMeasurements;
        mLci = lci == null ? EMPTY_BYTE_ARRAY : lci;
        mLcr = lcr == null ? EMPTY_BYTE_ARRAY : lcr;
        mResponderLocation = responderLocation;
        mTimestamp = timestamp;
        mIs80211mcMeasurement = is80211McMeasurement;
        mFrequencyMHz = UNSPECIFIED;
        mPacketBw = UNSPECIFIED;
    }

    /** @hide */
    public RangingResult(@RangeResultStatus int status, PeerHandle peerHandle, int distanceMm,
            int distanceStdDevMm, int rssi, int numAttemptedMeasurements,
            int numSuccessfulMeasurements, byte[] lci, byte[] lcr,
            ResponderLocation responderLocation, long timestamp) {
        mStatus = status;
        mMac = null;
        mPeerHandle = peerHandle;
        mDistanceMm = distanceMm;
        mDistanceStdDevMm = distanceStdDevMm;
        mRssi = rssi;
        mNumAttemptedMeasurements = numAttemptedMeasurements;
        mNumSuccessfulMeasurements = numSuccessfulMeasurements;
        mLci = lci == null ? EMPTY_BYTE_ARRAY : lci;
        mLcr = lcr == null ? EMPTY_BYTE_ARRAY : lcr;
        mResponderLocation = responderLocation;
        mTimestamp = timestamp;
        mIs80211mcMeasurement = true;
        mFrequencyMHz = UNSPECIFIED;
        mPacketBw = UNSPECIFIED;
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
     *
     * @hide
     */
    @SystemApi
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
     *
     * @hide
     */
    @SystemApi
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
     * @return The result is true if the IEEE 802.11mc protocol was used (also known as
     * two-sided RTT). If the result is false, a one-side RTT result is provided which does not
     * subtract the turnaround time at the responder.
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
    }

    public static final @android.annotation.NonNull Creator<RangingResult> CREATOR = new Creator<RangingResult>() {
        @Override
        public RangingResult[] newArray(int size) {
            return new RangingResult[size];
        }

        @Override
        public RangingResult createFromParcel(Parcel in) {
            int status = in.readInt();
            boolean macAddressPresent = in.readBoolean();
            MacAddress mac = null;
            if (macAddressPresent) {
                mac = MacAddress.CREATOR.createFromParcel(in);
            }
            boolean peerHandlePresent = in.readBoolean();
            PeerHandle peerHandle = null;
            if (peerHandlePresent) {
                peerHandle = new PeerHandle(in.readInt());
            }
            int distanceMm = in.readInt();
            int distanceStdDevMm = in.readInt();
            int rssi = in.readInt();
            int numAttemptedMeasurements = in.readInt();
            int numSuccessfulMeasurements = in.readInt();
            byte[] lci = in.createByteArray();
            byte[] lcr = in.createByteArray();
            ResponderLocation responderLocation =
                    in.readParcelable(this.getClass().getClassLoader());
            long timestamp = in.readLong();
            boolean isllmcMeasurement = in.readBoolean();
            int frequencyMHz = in.readInt();
            int packetBw = in.readInt();
            if (peerHandlePresent) {
                return new RangingResult(status, peerHandle, distanceMm, distanceStdDevMm, rssi,
                        numAttemptedMeasurements, numSuccessfulMeasurements, lci, lcr,
                        responderLocation, timestamp);
            } else {
                return new RangingResult(status, mac, distanceMm, distanceStdDevMm, rssi,
                        numAttemptedMeasurements, numSuccessfulMeasurements, lci, lcr,
                        responderLocation, timestamp, isllmcMeasurement, frequencyMHz, packetBw);
            }
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
                && mPacketBw == lhs.mPacketBw;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStatus, mMac, mPeerHandle, mDistanceMm, mDistanceStdDevMm, mRssi,
                mNumAttemptedMeasurements, mNumSuccessfulMeasurements, Arrays.hashCode(mLci),
                Arrays.hashCode(mLcr), mResponderLocation, mTimestamp, mIs80211mcMeasurement,
                mFrequencyMHz, mPacketBw);
    }
}
