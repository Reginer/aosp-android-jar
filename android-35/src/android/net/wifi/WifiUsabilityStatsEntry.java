/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.net.wifi;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.Annotation.NetworkType;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class makes a subset of
 * com.android.server.wifi.nano.WifiMetricsProto.WifiUsabilityStatsEntry parcelable.
 *
 * @hide
 */
@SystemApi
public final class WifiUsabilityStatsEntry implements Parcelable {
    private static final String TAG = "WifiUsabilityStatsEntry";

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"LINK_STATE_"}, value = {
            LINK_STATE_UNKNOWN,
            LINK_STATE_NOT_IN_USE,
            LINK_STATE_IN_USE})
    public @interface LinkState {}

    /** @hide */
    public static String getLinkStateString(@LinkState int state) {
        switch (state) {
            case LINK_STATE_NOT_IN_USE:
                return "LINK_STATE_NOT_IN_USE";
            case LINK_STATE_IN_USE:
                return "LINK_STATE_IN_USE";
            default:
                return "LINK_STATE_UNKNOWN";
        }
    }

    /** Chip does not support reporting the state of the link. */
    public static final int LINK_STATE_UNKNOWN = 0;
    /**
     * Link has not been in use since last report. It is placed in power save. All management,
     * control and data frames for the MLO connection are carried over other links. In this state
     * the link will not listen to beacons even in DTIM period and does not perform any
     * GTK/IGTK/BIGTK updates but remains associated.
     */
    public static final int LINK_STATE_NOT_IN_USE = 1;
    /**
     * Link is in use. In presence of traffic, it is set to be power active. When the traffic
     * stops, the link will go into power save mode and will listen for beacons every DTIM period.
     */
    public static final int LINK_STATE_IN_USE = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"PROBE_STATUS_"}, value = {
            PROBE_STATUS_UNKNOWN,
            PROBE_STATUS_NO_PROBE,
            PROBE_STATUS_SUCCESS,
            PROBE_STATUS_FAILURE})
    public @interface ProbeStatus {}

    /** Link probe status is unknown */
    public static final int PROBE_STATUS_UNKNOWN = 0;
    /** Link probe is not triggered */
    public static final int PROBE_STATUS_NO_PROBE = 1;
    /** Link probe is triggered and the result is success */
    public static final int PROBE_STATUS_SUCCESS = 2;
    /** Link probe is triggered and the result is failure */
    public static final int PROBE_STATUS_FAILURE = 3;

    /** Absolute milliseconds from device boot when these stats were sampled */
    private final long mTimeStampMillis;
    /** The RSSI (in dBm) at the sample time */
    private final int mRssi;
    /** Link speed at the sample time in Mbps */
    private final int mLinkSpeedMbps;
    /** The total number of tx success counted from the last radio chip reset */
    private final long mTotalTxSuccess;
    /** The total number of MPDU data packet retries counted from the last radio chip reset */
    private final long mTotalTxRetries;
    /** The total number of tx bad counted from the last radio chip reset */
    private final long mTotalTxBad;
    /** The total number of rx success counted from the last radio chip reset */
    private final long mTotalRxSuccess;
    /** The total time the wifi radio is on in ms counted from the last radio chip reset */
    private final long mTotalRadioOnTimeMillis;
    /** The total time the wifi radio is doing tx in ms counted from the last radio chip reset */
    private final long mTotalRadioTxTimeMillis;
    /** The total time the wifi radio is doing rx in ms counted from the last radio chip reset */
    private final long mTotalRadioRxTimeMillis;
    /** The total time spent on all types of scans in ms counted from the last radio chip reset */
    private final long mTotalScanTimeMillis;
    /** The total time spent on nan scans in ms counted from the last radio chip reset */
    private final long mTotalNanScanTimeMillis;
    /** The total time spent on background scans in ms counted from the last radio chip reset */
    private final long mTotalBackgroundScanTimeMillis;
    /** The total time spent on roam scans in ms counted from the last radio chip reset */
    private final long mTotalRoamScanTimeMillis;
    /** The total time spent on pno scans in ms counted from the last radio chip reset */
    private final long mTotalPnoScanTimeMillis;
    /** The total time spent on hotspot2.0 scans and GAS exchange in ms counted from the last radio
     * chip reset */
    private final long mTotalHotspot2ScanTimeMillis;
    /** The total time CCA is on busy status on the current frequency in ms counted from the last
     * radio chip reset */
    private final long mTotalCcaBusyFreqTimeMillis;
    /** The total radio on time on the current frequency from the last radio chip reset */
    private final long mTotalRadioOnFreqTimeMillis;
    /** The total number of beacons received from the last radio chip reset */
    private final long mTotalBeaconRx;
    /** The status of link probe since last stats update */
    @ProbeStatus private final int mProbeStatusSinceLastUpdate;
    /** The elapsed time of the most recent link probe since last stats update */
    private final int mProbeElapsedTimeSinceLastUpdateMillis;
    /** The MCS rate of the most recent link probe since last stats update */
    private final int mProbeMcsRateSinceLastUpdate;
    /** Rx link speed at the sample time in Mbps */
    private final int mRxLinkSpeedMbps;
    /** @see #getTimeSliceDutyCycleInPercent() */
    private final int mTimeSliceDutyCycleInPercent;

    /** {@hide} */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"WME_ACCESS_CATEGORY_"}, value = {
        WME_ACCESS_CATEGORY_BE,
        WME_ACCESS_CATEGORY_BK,
        WME_ACCESS_CATEGORY_VI,
        WME_ACCESS_CATEGORY_VO})
    public @interface WmeAccessCategory {}

    /**
     * Wireless Multimedia Extensions (WME) Best Effort Access Category, IEEE Std 802.11-2020,
     * Section 9.4.2.28, Table 9-155
     */
    public static final int WME_ACCESS_CATEGORY_BE = 0;
    /**
     * Wireless Multimedia Extensions (WME) Background Access Category, IEEE Std 802.11-2020,
     * Section 9.4.2.28, Table 9-155
     */
    public static final int WME_ACCESS_CATEGORY_BK = 1;
    /**
     * Wireless Multimedia Extensions (WME) Video Access Category, IEEE Std 802.11-2020,
     * Section 9.4.2.28, Table 9-155
     */
    public static final int WME_ACCESS_CATEGORY_VI = 2;
    /**
     * Wireless Multimedia Extensions (WME) Voice Access Category, IEEE Std 802.11-2020,
     * Section 9.4.2.28, Table 9-155
     */
    public static final int WME_ACCESS_CATEGORY_VO = 3;
    /** Number of WME Access Categories */
    public static final int NUM_WME_ACCESS_CATEGORIES = 4;

    /**
     * Data packet contention time statistics.
     */
    public static final class ContentionTimeStats implements Parcelable {
        private long mContentionTimeMinMicros;
        private long mContentionTimeMaxMicros;
        private long mContentionTimeAvgMicros;
        private long mContentionNumSamples;

        /** @hide */
        public ContentionTimeStats() {
        }

        /**
         * Constructor function
         * @param timeMin The minimum data packet contention time
         * @param timeMax The maximum data packet contention time
         * @param timeAvg The average data packet contention time
         * @param numSamples The number of samples used to get the reported contention time
         */
        public ContentionTimeStats(long timeMin, long timeMax, long timeAvg, long numSamples) {
            this.mContentionTimeMinMicros = timeMin;
            this.mContentionTimeMaxMicros = timeMax;
            this.mContentionTimeAvgMicros = timeAvg;
            this.mContentionNumSamples = numSamples;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeLong(mContentionTimeMinMicros);
            dest.writeLong(mContentionTimeMaxMicros);
            dest.writeLong(mContentionTimeAvgMicros);
            dest.writeLong(mContentionNumSamples);
        }

        /** Implement the Parcelable interface */
        public static final @NonNull Creator<ContentionTimeStats> CREATOR =
                new Creator<ContentionTimeStats>() {
            public ContentionTimeStats createFromParcel(Parcel in) {
                ContentionTimeStats stats = new ContentionTimeStats();
                stats.mContentionTimeMinMicros = in.readLong();
                stats.mContentionTimeMaxMicros = in.readLong();
                stats.mContentionTimeAvgMicros = in.readLong();
                stats.mContentionNumSamples = in.readLong();
                return stats;
            }
            public ContentionTimeStats[] newArray(int size) {
                return new ContentionTimeStats[size];
            }
        };

        /** Data packet min contention time in microseconds */
        public long getContentionTimeMinMicros() {
            return mContentionTimeMinMicros;
        }

        /** Data packet max contention time in microseconds */
        public long getContentionTimeMaxMicros() {
            return mContentionTimeMaxMicros;
        }

        /** Data packet average contention time in microseconds */
        public long getContentionTimeAvgMicros() {
            return mContentionTimeAvgMicros;
        }

        /**
         * Number of data packets used for deriving the min, the max, and the average
         * contention time
         */
        public long getContentionNumSamples() {
            return mContentionNumSamples;
        }
    }
    private final ContentionTimeStats[] mContentionTimeStats;

    /** {@hide} */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"WIFI_PREAMBLE_"}, value = {
        WIFI_PREAMBLE_OFDM,
        WIFI_PREAMBLE_CCK,
        WIFI_PREAMBLE_HT,
        WIFI_PREAMBLE_VHT,
        WIFI_PREAMBLE_HE,
        WIFI_PREAMBLE_EHT,
        WIFI_PREAMBLE_INVALID})
    public @interface WifiPreambleType {}

    /** Preamble type for 802.11a/g, IEEE Std 802.11-2020, Section 17 */
    public static final int WIFI_PREAMBLE_OFDM = 0;
    /** Preamble type for 802.11b, IEEE Std 802.11-2020, Section 16 */
    public static final int WIFI_PREAMBLE_CCK = 1;
    /** Preamble type for 802.11n, IEEE Std 802.11-2020, Section 19 */
    public static final int WIFI_PREAMBLE_HT = 2;
    /** Preamble type for 802.11ac, IEEE Std 802.11-2020, Section 21 */
    public static final int WIFI_PREAMBLE_VHT = 3;
    /** Preamble type for 802.11ax, IEEE Std 802.11ax-2021, Section 27 */
    public static final int WIFI_PREAMBLE_HE = 5;
    /** Preamble type for 802.11be, IEEE Std 802.11be-2021, Section 36 */
    public static final int WIFI_PREAMBLE_EHT = 6;
    /** Invalid */
    public static final int WIFI_PREAMBLE_INVALID = -1;

    /** {@hide} */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"WIFI_SPATIAL_STREAMS_"}, value = {
        WIFI_SPATIAL_STREAMS_ONE,
        WIFI_SPATIAL_STREAMS_TWO,
        WIFI_SPATIAL_STREAMS_THREE,
        WIFI_SPATIAL_STREAMS_FOUR,
        WIFI_SPATIAL_STREAMS_INVALID})
    public @interface WifiSpatialStreams {}

    /** Single stream, 1x1 */
    public static final int WIFI_SPATIAL_STREAMS_ONE = 1;
    /** Dual streams, 2x2 */
    public static final int WIFI_SPATIAL_STREAMS_TWO = 2;
    /** Three streams, 3x3 */
    public static final int WIFI_SPATIAL_STREAMS_THREE = 3;
    /** Four streams, 4x4 */
    public static final int WIFI_SPATIAL_STREAMS_FOUR = 4;
    /** Invalid */
    public static final int WIFI_SPATIAL_STREAMS_INVALID = -1;

    /** {@hide} */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"WIFI_BANDWIDTH_"}, value = {
        WIFI_BANDWIDTH_20_MHZ,
        WIFI_BANDWIDTH_40_MHZ,
        WIFI_BANDWIDTH_80_MHZ,
        WIFI_BANDWIDTH_160_MHZ,
        WIFI_BANDWIDTH_320_MHZ,
        WIFI_BANDWIDTH_80P80_MHZ,
        WIFI_BANDWIDTH_5_MHZ,
        WIFI_BANDWIDTH_10_MHZ,
        WIFI_BANDWIDTH_INVALID})
    public @interface WifiChannelBandwidth {}

    /** Channel bandwidth: 20MHz */
    public static final int WIFI_BANDWIDTH_20_MHZ = 0;
    /** Channel bandwidth: 40MHz */
    public static final int WIFI_BANDWIDTH_40_MHZ = 1;
    /** Channel bandwidth: 80MHz */
    public static final int WIFI_BANDWIDTH_80_MHZ = 2;
    /** Channel bandwidth: 160MHz */
    public static final int WIFI_BANDWIDTH_160_MHZ = 3;
    /** Channel bandwidth: 80MHz + 80MHz */
    public static final int WIFI_BANDWIDTH_80P80_MHZ = 4;
    /** Channel bandwidth: 5MHz */
    public static final int WIFI_BANDWIDTH_5_MHZ = 5;
    /** Channel bandwidth: 10MHz */
    public static final int WIFI_BANDWIDTH_10_MHZ = 6;
    /** Channel bandwidth: 320MHz */
    public static final int WIFI_BANDWIDTH_320_MHZ = 7;
    /** Invalid */
    public static final int WIFI_BANDWIDTH_INVALID = -1;

    /**
     * Rate information and statistics: packet counters (tx/rx successful packets, retries, lost)
     * indexed by preamble, bandwidth, number of spatial streams, MCS, and bit rate.
     */
    public static final class RateStats implements Parcelable {
        @WifiPreambleType private int mPreamble;
        @WifiSpatialStreams private int mNss;
        @WifiChannelBandwidth private int mBw;
        private int mRateMcsIdx;
        private int mBitRateInKbps;
        private int mTxMpdu;
        private int mRxMpdu;
        private int mMpduLost;
        private int mRetries;

        /** @hide */
        public RateStats() {
        }

        /**
         * Constructor function.
         * @param preamble Preamble information.
         * @param nss Number of spatial streams.
         * @param bw Bandwidth information.
         * @param rateMcsIdx MCS index. OFDM/CCK rate code would be as per IEEE std in the units of
         *                   0.5Mbps. HT/VHT/HE/EHT: it would be MCS index.
         * @param bitRateInKbps Bitrate in units of 100 Kbps.
         * @param txMpdu Number of successfully transmitted data packets (ACK received).
         * @param rxMpdu Number of received data packets.
         * @param mpduLost Number of data packet losses (no ACK).
         * @param retries Number of data packet retries.
         */
        public RateStats(@WifiPreambleType int preamble, @WifiSpatialStreams int nss,
                @WifiChannelBandwidth int bw, int rateMcsIdx, int bitRateInKbps, int txMpdu,
                int rxMpdu, int mpduLost, int retries) {
            this.mPreamble = preamble;
            this.mNss = nss;
            this.mBw = bw;
            this.mRateMcsIdx = rateMcsIdx;
            this.mBitRateInKbps = bitRateInKbps;
            this.mTxMpdu = txMpdu;
            this.mRxMpdu = rxMpdu;
            this.mMpduLost = mpduLost;
            this.mRetries = retries;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(mPreamble);
            dest.writeInt(mNss);
            dest.writeInt(mBw);
            dest.writeInt(mRateMcsIdx);
            dest.writeInt(mBitRateInKbps);
            dest.writeInt(mTxMpdu);
            dest.writeInt(mRxMpdu);
            dest.writeInt(mMpduLost);
            dest.writeInt(mRetries);
        }

        /** Implement the Parcelable interface */
        public static final @NonNull Creator<RateStats> CREATOR = new Creator<RateStats>() {
            public RateStats createFromParcel(Parcel in) {
                RateStats stats = new RateStats();
                stats.mPreamble = in.readInt();
                stats.mNss = in.readInt();
                stats.mBw = in.readInt();
                stats.mRateMcsIdx = in.readInt();
                stats.mBitRateInKbps = in.readInt();
                stats.mTxMpdu = in.readInt();
                stats.mRxMpdu = in.readInt();
                stats.mMpduLost = in.readInt();
                stats.mRetries = in.readInt();
                return stats;
            }
            public RateStats[] newArray(int size) {
                return new RateStats[size];
            }
        };

        /** Preamble information, see {@link WifiPreambleType} */
        @WifiPreambleType public int getPreamble() {
            return mPreamble;
        }

        /** Number of spatial streams, see {@link WifiSpatialStreams} */
        @WifiSpatialStreams public int getNumberOfSpatialStreams() {
            return mNss;
        }

        /** Bandwidth information, see {@link WifiChannelBandwidth} */
        @WifiChannelBandwidth public int getBandwidthInMhz() {
            return mBw;
        }

        /**
         * MCS index. OFDM/CCK rate code would be as per IEEE std in the units of 0.5Mbps.
         * HT/VHT/HE/EHT: it would be MCS index
         */
        public int getRateMcsIdx() {
            return mRateMcsIdx;
        }

        /** Bitrate in units of a hundred Kbps */
        public int getBitRateInKbps() {
            return mBitRateInKbps;
        }

        /** Number of successfully transmitted data packets (ACK received) */
        public int getTxMpdu() {
            return mTxMpdu;
        }

        /** Number of received data packets */
        public int getRxMpdu() {
            return mRxMpdu;
        }

        /** Number of data packet losses (no ACK) */
        public int getMpduLost() {
            return mMpduLost;
        }

        /** Number of data packet retries */
        public int getRetries() {
            return mRetries;
        }
    }
    private final RateStats[] mRateStats;

    /**
     * Wifi link layer radio stats.
     */
    public static final class RadioStats implements Parcelable {
        /**
         * Invalid radio id.
         * @hide
         */
        public static final int INVALID_RADIO_ID = -1;
        private int mRadioId;
        private long mTotalRadioOnTimeMillis;
        private long mTotalRadioTxTimeMillis;
        private long mTotalRadioRxTimeMillis;
        private long mTotalScanTimeMillis;
        private long mTotalNanScanTimeMillis;
        private long mTotalBackgroundScanTimeMillis;
        private long mTotalRoamScanTimeMillis;
        private long mTotalPnoScanTimeMillis;
        private long mTotalHotspot2ScanTimeMillis;

        /** @hide */
        public RadioStats() {
        }

        /**
         * Constructor function
         * @param radioId Firmware/Hardware implementation specific persistent value for this
         *                device, identifying the radio interface for which the stats are produced.
         * @param onTime The total time the wifi radio is on in ms counted from the last radio
         *               chip reset
         * @param txTime The total time the wifi radio is transmitting in ms counted from the last
         *               radio chip reset
         * @param rxTime The total time the wifi radio is receiving in ms counted from the last
         *               radio chip reset
         * @param onTimeScan The total time spent on all types of scans in ms counted from the
         *                   last radio chip reset
         * @param onTimeNanScan The total time spent on nan scans in ms counted from the last radio
         *                      chip reset
         * @param onTimeBackgroundScan The total time spent on background scans in ms counted from
         *                             the last radio chip reset
         * @param onTimeRoamScan The total time spent on roam scans in ms counted from the last
         *                       radio chip reset
         * @param onTimePnoScan The total time spent on pno scans in ms counted from the last radio
         *                      chip reset
         * @param onTimeHs20Scan The total time spent on hotspot2.0 scans and GAS exchange in ms
         *                       counted from the last radio chip reset
         */
        public RadioStats(int radioId, long onTime, long txTime, long rxTime, long onTimeScan,
                long onTimeNanScan, long onTimeBackgroundScan, long onTimeRoamScan,
                long onTimePnoScan, long onTimeHs20Scan) {
            this.mRadioId = radioId;
            this.mTotalRadioOnTimeMillis = onTime;
            this.mTotalRadioTxTimeMillis = txTime;
            this.mTotalRadioRxTimeMillis = rxTime;
            this.mTotalScanTimeMillis = onTimeScan;
            this.mTotalNanScanTimeMillis = onTimeNanScan;
            this.mTotalBackgroundScanTimeMillis = onTimeBackgroundScan;
            this.mTotalRoamScanTimeMillis = onTimeRoamScan;
            this.mTotalPnoScanTimeMillis = onTimePnoScan;
            this.mTotalHotspot2ScanTimeMillis = onTimeHs20Scan;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(mRadioId);
            dest.writeLong(mTotalRadioOnTimeMillis);
            dest.writeLong(mTotalRadioTxTimeMillis);
            dest.writeLong(mTotalRadioRxTimeMillis);
            dest.writeLong(mTotalScanTimeMillis);
            dest.writeLong(mTotalNanScanTimeMillis);
            dest.writeLong(mTotalBackgroundScanTimeMillis);
            dest.writeLong(mTotalRoamScanTimeMillis);
            dest.writeLong(mTotalPnoScanTimeMillis);
            dest.writeLong(mTotalHotspot2ScanTimeMillis);
        }

        /** Implement the Parcelable interface */
        public static final @NonNull Creator<RadioStats> CREATOR =
                new Creator<RadioStats>() {
                    public RadioStats createFromParcel(Parcel in) {
                        RadioStats stats = new RadioStats();
                        stats.mRadioId = in.readInt();
                        stats.mTotalRadioOnTimeMillis = in.readLong();
                        stats.mTotalRadioTxTimeMillis = in.readLong();
                        stats.mTotalRadioRxTimeMillis = in.readLong();
                        stats.mTotalScanTimeMillis = in.readLong();
                        stats.mTotalNanScanTimeMillis = in.readLong();
                        stats.mTotalBackgroundScanTimeMillis = in.readLong();
                        stats.mTotalRoamScanTimeMillis = in.readLong();
                        stats.mTotalPnoScanTimeMillis = in.readLong();
                        stats.mTotalHotspot2ScanTimeMillis = in.readLong();
                        return stats;
                    }
                    public RadioStats[] newArray(int size) {
                        return new RadioStats[size];
                    }
                };

        /**
         * Firmware/Hardware implementation specific persistent value for this
         * device, identifying the radio interface for which the stats are produced.
         */
        public long getRadioId() {
            return mRadioId;
        }

        /** The total time the wifi radio is on in ms counted from the last radio chip reset */
        public long getTotalRadioOnTimeMillis() {
            return mTotalRadioOnTimeMillis;
        }

        /**
         * The total time the wifi radio is transmitting in ms counted from the last radio chip
         * reset
         */
        public long getTotalRadioTxTimeMillis() {
            return mTotalRadioTxTimeMillis;
        }

        /**
         * The total time the wifi radio is receiving in ms counted from the last radio chip reset
         */
        public long getTotalRadioRxTimeMillis() {
            return mTotalRadioRxTimeMillis;
        }

        /**
         * The total time spent on all types of scans in ms counted from the last radio chip reset
         */
        public long getTotalScanTimeMillis() {
            return mTotalScanTimeMillis;
        }

        /** The total time spent on nan scans in ms counted from the last radio chip reset */
        public long getTotalNanScanTimeMillis() {
            return mTotalNanScanTimeMillis;
        }

        /** The total time spent on background scans in ms counted from the last radio chip reset */
        public long getTotalBackgroundScanTimeMillis() {
            return mTotalBackgroundScanTimeMillis;
        }

        /** The total time spent on roam scans in ms counted from the last radio chip reset */
        public long getTotalRoamScanTimeMillis() {
            return mTotalRoamScanTimeMillis;
        }

        /** The total time spent on pno scans in ms counted from the last radio chip reset */
        public long getTotalPnoScanTimeMillis() {
            return mTotalPnoScanTimeMillis;
        }

        /**
         * The total time spent on hotspot2.0 scans and GAS exchange in ms counted from the
         * last radio chip reset
         */
        public long getTotalHotspot2ScanTimeMillis() {
            return mTotalHotspot2ScanTimeMillis;
        }
    }
    private final RadioStats[] mRadioStats;
    private final int mChannelUtilizationRatio;
    private final boolean mIsThroughputSufficient;
    private final boolean mIsWifiScoringEnabled;
    private final boolean mIsCellularDataAvailable;
    private final @NetworkType int mCellularDataNetworkType;
    private final int mCellularSignalStrengthDbm;
    private final int mCellularSignalStrengthDb;
    private final boolean mIsSameRegisteredCell;

    /**
     * Link specific statistics.
     *
     * @hide
     */
    public static final class LinkStats implements Parcelable {
        /** Link identifier (0 to 14) */
        private final int mLinkId;
        /** Link state as listed {@link LinkState} */
        private final @LinkState int mState;
        /** Radio identifier */
        private final int mRadioId;
        /** The RSSI (in dBm) at the sample time */
        private final int mRssi;
        /** Tx Link speed at the sample time in Mbps */
        private final int mTxLinkSpeedMbps;
        /** Rx link speed at the sample time in Mbps */
        private final int mRxLinkSpeedMbps;
        /** The total number of tx success counted from the last radio chip reset */
        private final long mTotalTxSuccess;
        /** The total number of MPDU data packet retries counted from the last radio chip reset */
        private final long mTotalTxRetries;
        /** The total number of tx bad counted from the last radio chip reset */
        private final long mTotalTxBad;
        /** The total number of rx success counted from the last radio chip reset */
        private final long mTotalRxSuccess;
        /** The total number of beacons received from the last radio chip reset */
        private final long mTotalBeaconRx;
        /** @see #WifiUsabilityStatsEntry#getTimeSliceDutyCycleInPercent() */
        private final int mTimeSliceDutyCycleInPercent;
        /**
         * The total time CCA is on busy status on the link frequency in ms counted from the last
         * radio chip reset
         */
        private final long mTotalCcaBusyFreqTimeMillis;
        /** The total radio on time on the link frequency in ms from the last radio chip reset */
        private final long mTotalRadioOnFreqTimeMillis;
        /** Data packet contention time statistics */
        private final ContentionTimeStats[] mContentionTimeStats;
        /** Rate information and statistics */
        private final RateStats[] mRateStats;

        /** @hide */
        public LinkStats() {
            mLinkId = MloLink.INVALID_MLO_LINK_ID;
            mState = LINK_STATE_UNKNOWN;
            mRssi = WifiInfo.INVALID_RSSI;
            mRadioId = RadioStats.INVALID_RADIO_ID;
            mTxLinkSpeedMbps = WifiInfo.LINK_SPEED_UNKNOWN;
            mRxLinkSpeedMbps = WifiInfo.LINK_SPEED_UNKNOWN;
            mTotalTxSuccess = 0;
            mTotalTxRetries = 0;
            mTotalTxBad = 0;
            mTotalRxSuccess = 0;
            mTotalBeaconRx = 0;
            mTimeSliceDutyCycleInPercent = 0;
            mTotalCcaBusyFreqTimeMillis = 0;
            mTotalRadioOnFreqTimeMillis = 0;
            mContentionTimeStats = null;
            mRateStats = null;
        }

        /** @hide
         * Constructor function
         *
         * @param linkId                      Identifier of the link.
         * @param radioId                     Identifier of the radio on which the link is operating
         *                                    currently.
         * @param rssi                        Link Rssi (in dBm) at sample time.
         * @param txLinkSpeedMpbs             Transmit link speed in Mpbs at sample time.
         * @param rxLinkSpeedMpbs             Receive link speed in Mbps at sample time.
         * @param totalTxSuccess              Total number of Tx success.
         * @param totalTxRetries              Total packet retries.
         * @param totalTxBad                  Total number of bad packets.
         * @param totalRxSuccess              Total number of good receive packets.
         * @param totalBeaconRx               Total number of beacon received.
         * @param timeSliceDutyCycleInPercent Duty cycle of the connection.
         * @param totalCcaBusyFreqTimeMillis  Total CCA is on busy status on the link frequency from
         *                                    the last radio chip reset.
         * @param totalRadioOnFreqTimeMillis  Total Radio on time on the link frequency from the
         *                                    last radio chip reset.
         * @param contentionTimeStats         Data packet contention time statistics.
         * @param rateStats                   Rate information.
         */
        public LinkStats(int linkId, int state, int radioId, int rssi, int txLinkSpeedMpbs,
                int rxLinkSpeedMpbs, long totalTxSuccess, long totalTxRetries, long totalTxBad,
                long totalRxSuccess, long totalBeaconRx, int timeSliceDutyCycleInPercent,
                long totalCcaBusyFreqTimeMillis, long  totalRadioOnFreqTimeMillis,
                ContentionTimeStats[] contentionTimeStats, RateStats[] rateStats) {
            this.mLinkId = linkId;
            this.mState = state;
            this.mRadioId = radioId;
            this.mRssi = rssi;
            this.mTxLinkSpeedMbps = txLinkSpeedMpbs;
            this.mRxLinkSpeedMbps = rxLinkSpeedMpbs;
            this.mTotalTxSuccess = totalTxSuccess;
            this.mTotalTxRetries = totalTxRetries;
            this.mTotalTxBad = totalTxBad;
            this.mTotalRxSuccess = totalRxSuccess;
            this.mTotalBeaconRx = totalBeaconRx;
            this.mTimeSliceDutyCycleInPercent = timeSliceDutyCycleInPercent;
            this.mTotalCcaBusyFreqTimeMillis = totalCcaBusyFreqTimeMillis;
            this.mTotalRadioOnFreqTimeMillis = totalRadioOnFreqTimeMillis;
            this.mContentionTimeStats = contentionTimeStats;
            this.mRateStats = rateStats;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(mLinkId);
            dest.writeInt(mState);
            dest.writeInt(mRadioId);
            dest.writeInt(mRssi);
            dest.writeInt(mTxLinkSpeedMbps);
            dest.writeInt(mRxLinkSpeedMbps);
            dest.writeLong(mTotalTxSuccess);
            dest.writeLong(mTotalTxRetries);
            dest.writeLong(mTotalTxBad);
            dest.writeLong(mTotalRxSuccess);
            dest.writeLong(mTotalBeaconRx);
            dest.writeInt(mTimeSliceDutyCycleInPercent);
            dest.writeLong(mTotalCcaBusyFreqTimeMillis);
            dest.writeLong(mTotalRadioOnFreqTimeMillis);
            dest.writeTypedArray(mContentionTimeStats, flags);
            dest.writeTypedArray(mRateStats, flags);
        }

        /** Implement the Parcelable interface */
        @NonNull
        public static final Creator<LinkStats> CREATOR =
                new Creator<>() {
                    public LinkStats createFromParcel(Parcel in) {
                        return new LinkStats(in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                                in.readInt(), in.readInt(), in.readLong(), in.readLong(),
                                in.readLong(), in.readLong(), in.readLong(), in.readInt(),
                                in.readLong(), in.readLong(),
                                in.createTypedArray(ContentionTimeStats.CREATOR),
                                in.createTypedArray(RateStats.CREATOR));
                    }

                    public LinkStats[] newArray(int size) {
                        return new LinkStats[size];
                    }
                };
    }

    /** Link stats per link */
    private final SparseArray<LinkStats> mLinkStats;

    /** Constructor function {@hide} */
    public WifiUsabilityStatsEntry(long timeStampMillis, int rssi, int linkSpeedMbps,
            long totalTxSuccess, long totalTxRetries, long totalTxBad, long totalRxSuccess,
            long totalRadioOnTimeMillis, long totalRadioTxTimeMillis, long totalRadioRxTimeMillis,
            long totalScanTimeMillis, long totalNanScanTimeMillis,
            long totalBackgroundScanTimeMillis,
            long totalRoamScanTimeMillis, long totalPnoScanTimeMillis,
            long totalHotspot2ScanTimeMillis,
            long totalCcaBusyFreqTimeMillis, long totalRadioOnFreqTimeMillis, long totalBeaconRx,
            @ProbeStatus int probeStatusSinceLastUpdate, int probeElapsedTimeSinceLastUpdateMillis,
            int probeMcsRateSinceLastUpdate, int rxLinkSpeedMbps,
            int timeSliceDutyCycleInPercent, ContentionTimeStats[] contentionTimeStats,
            RateStats[] rateStats, RadioStats[] radiostats, int channelUtilizationRatio,
            boolean isThroughputSufficient, boolean isWifiScoringEnabled,
            boolean isCellularDataAvailable, @NetworkType int cellularDataNetworkType,
            int cellularSignalStrengthDbm, int cellularSignalStrengthDb,
            boolean isSameRegisteredCell, SparseArray<LinkStats> linkStats) {
        mTimeStampMillis = timeStampMillis;
        mRssi = rssi;
        mLinkSpeedMbps = linkSpeedMbps;
        mTotalTxSuccess = totalTxSuccess;
        mTotalTxRetries = totalTxRetries;
        mTotalTxBad = totalTxBad;
        mTotalRxSuccess = totalRxSuccess;
        mTotalRadioOnTimeMillis = totalRadioOnTimeMillis;
        mTotalRadioTxTimeMillis = totalRadioTxTimeMillis;
        mTotalRadioRxTimeMillis = totalRadioRxTimeMillis;
        mTotalScanTimeMillis = totalScanTimeMillis;
        mTotalNanScanTimeMillis = totalNanScanTimeMillis;
        mTotalBackgroundScanTimeMillis = totalBackgroundScanTimeMillis;
        mTotalRoamScanTimeMillis = totalRoamScanTimeMillis;
        mTotalPnoScanTimeMillis = totalPnoScanTimeMillis;
        mTotalHotspot2ScanTimeMillis = totalHotspot2ScanTimeMillis;
        mTotalCcaBusyFreqTimeMillis = totalCcaBusyFreqTimeMillis;
        mTotalRadioOnFreqTimeMillis = totalRadioOnFreqTimeMillis;
        mTotalBeaconRx = totalBeaconRx;
        mProbeStatusSinceLastUpdate = probeStatusSinceLastUpdate;
        mProbeElapsedTimeSinceLastUpdateMillis = probeElapsedTimeSinceLastUpdateMillis;
        mProbeMcsRateSinceLastUpdate = probeMcsRateSinceLastUpdate;
        mRxLinkSpeedMbps = rxLinkSpeedMbps;
        mTimeSliceDutyCycleInPercent = timeSliceDutyCycleInPercent;
        mContentionTimeStats = contentionTimeStats;
        mRateStats = rateStats;
        mRadioStats = radiostats;
        mChannelUtilizationRatio = channelUtilizationRatio;
        mIsThroughputSufficient = isThroughputSufficient;
        mIsWifiScoringEnabled = isWifiScoringEnabled;
        mIsCellularDataAvailable = isCellularDataAvailable;
        mCellularDataNetworkType = cellularDataNetworkType;
        mCellularSignalStrengthDbm = cellularSignalStrengthDbm;
        mCellularSignalStrengthDb = cellularSignalStrengthDb;
        mIsSameRegisteredCell = isSameRegisteredCell;
        mLinkStats = linkStats;
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mTimeStampMillis);
        dest.writeInt(mRssi);
        dest.writeInt(mLinkSpeedMbps);
        dest.writeLong(mTotalTxSuccess);
        dest.writeLong(mTotalTxRetries);
        dest.writeLong(mTotalTxBad);
        dest.writeLong(mTotalRxSuccess);
        dest.writeLong(mTotalRadioOnTimeMillis);
        dest.writeLong(mTotalRadioTxTimeMillis);
        dest.writeLong(mTotalRadioRxTimeMillis);
        dest.writeLong(mTotalScanTimeMillis);
        dest.writeLong(mTotalNanScanTimeMillis);
        dest.writeLong(mTotalBackgroundScanTimeMillis);
        dest.writeLong(mTotalRoamScanTimeMillis);
        dest.writeLong(mTotalPnoScanTimeMillis);
        dest.writeLong(mTotalHotspot2ScanTimeMillis);
        dest.writeLong(mTotalCcaBusyFreqTimeMillis);
        dest.writeLong(mTotalRadioOnFreqTimeMillis);
        dest.writeLong(mTotalBeaconRx);
        dest.writeInt(mProbeStatusSinceLastUpdate);
        dest.writeInt(mProbeElapsedTimeSinceLastUpdateMillis);
        dest.writeInt(mProbeMcsRateSinceLastUpdate);
        dest.writeInt(mRxLinkSpeedMbps);
        dest.writeInt(mTimeSliceDutyCycleInPercent);
        dest.writeTypedArray(mContentionTimeStats, flags);
        dest.writeTypedArray(mRateStats, flags);
        dest.writeTypedArray(mRadioStats, flags);
        dest.writeInt(mChannelUtilizationRatio);
        dest.writeBoolean(mIsThroughputSufficient);
        dest.writeBoolean(mIsWifiScoringEnabled);
        dest.writeBoolean(mIsCellularDataAvailable);
        dest.writeInt(mCellularDataNetworkType);
        dest.writeInt(mCellularSignalStrengthDbm);
        dest.writeInt(mCellularSignalStrengthDb);
        dest.writeBoolean(mIsSameRegisteredCell);
        dest.writeSparseArray(mLinkStats);
    }

    /** Implement the Parcelable interface */
    public static final @android.annotation.NonNull Creator<WifiUsabilityStatsEntry> CREATOR =
            new Creator<WifiUsabilityStatsEntry>() {
        public WifiUsabilityStatsEntry createFromParcel(Parcel in) {
            return new WifiUsabilityStatsEntry(
                    in.readLong(), in.readInt(),
                    in.readInt(), in.readLong(), in.readLong(),
                    in.readLong(), in.readLong(), in.readLong(),
                    in.readLong(), in.readLong(), in.readLong(),
                    in.readLong(), in.readLong(), in.readLong(),
                    in.readLong(), in.readLong(), in.readLong(),
                    in.readLong(), in.readLong(), in.readInt(),
                    in.readInt(), in.readInt(), in.readInt(),
                    in.readInt(), in.createTypedArray(ContentionTimeStats.CREATOR),
                    in.createTypedArray(RateStats.CREATOR),
                    in.createTypedArray(RadioStats.CREATOR),
                    in.readInt(), in.readBoolean(), in.readBoolean(),
                    in.readBoolean(), in.readInt(), in.readInt(),
                    in.readInt(), in.readBoolean(),
                    in.readSparseArray(LinkStats.class.getClassLoader())
            );
        }

        public WifiUsabilityStatsEntry[] newArray(int size) {
            return new WifiUsabilityStatsEntry[size];
        }
    };

    /** Get identifiers of the links if multi link configuration exists.
     *
     * @return An array of link identifiers if exists, otherwise null.
     */
    @Nullable
    public int[] getLinkIds() {
        if (mLinkStats == null) return null;
        int[] result = new int[mLinkStats.size()];
        for (int i = 0; i < mLinkStats.size(); i++) {
            result[i] = mLinkStats.keyAt(i);
        }
        return result;
    }

    /**
     * Get link state. Refer {@link LinkState}.
     *
     * @param linkId Identifier of the link.
     * @return Link state.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public @LinkState int getLinkState(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mState;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /** Absolute milliseconds from device boot when these stats were sampled */
    public long getTimeStampMillis() {
        return mTimeStampMillis;
    }

    /**
     * The RSSI (in dBm) at the sample time. In case of Multi Link Operation (MLO), returned RSSI is
     * the highest of all associated links.
     *
     * @return the RSSI.
     */
    public int getRssi() {
        return mRssi;
    }

    /** The RSSI (in dBm) of the link at the sample time.
     *
     * @param linkId Identifier of the link.
     * @return RSSI in dBm.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public int getRssi(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mRssi;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * Get firmware/hardware implementation specific persistent value for this
     * device, identifying the radio interface for which the stats are produced.
     *
     * @param linkId Identifier of the link.
     * @return Radio identifier.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public long getRadioId(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mRadioId;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /** Link speed at the sample time in Mbps. In case of Multi Link Operation (MLO), returned value
     *  is the current link speed of the associated link with the highest RSSI.
     *
     * @return Link speed in Mpbs or {@link WifiInfo#LINK_SPEED_UNKNOWN} if link speed is unknown.
     */
    public int getLinkSpeedMbps() {
        return mLinkSpeedMbps;
    }

    /**
     * Tx Link speed of the link at the sample time in Mbps.
     *
     * @param linkId Identifier of the link.
     * @return Transmit link speed in Mpbs or {@link WifiInfo#LINK_SPEED_UNKNOWN} if link speed
     * is unknown.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public int getTxLinkSpeedMbps(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mTxLinkSpeedMbps;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * The total number of tx success counted from the last radio chip reset.  In case of Multi
     * Link Operation (MLO), the returned value is the sum of total number of tx success on all
     * associated links.
     *
     * @return total tx success count.
     */
    public long getTotalTxSuccess() {
        return mTotalTxSuccess;
    }

    /**
     * The total number of tx success on a link counted from the last radio chip reset.
     *
     * @param linkId Identifier of the link.
     * @return total tx success count.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public long getTotalTxSuccess(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mTotalTxSuccess;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * The total number of MPDU data packet retries counted from the last radio chip reset. In
     * case of Multi Link Operation (MLO), the returned value is the sum of total number of MPDU
     * data packet retries on all associated links.
     *
     * @return total tx retries count.
     */
    public long getTotalTxRetries() {
        return mTotalTxRetries;
    }

    /**
     * The total number of MPDU data packet retries on a link counted from the last radio chip
     * reset.
     *
     * @param linkId Identifier of the link.
     * @return total tx retries count.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public long getTotalTxRetries(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mTotalTxRetries;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * The total number of tx bad counted from the last radio chip reset. In case of Multi Link
     * Operation (MLO), the returned value is the sum of total number of tx bad on all associated
     * links.
     *
     * @return total tx bad count.
     */
    public long getTotalTxBad() {
        return mTotalTxBad;
    }

    /**
     * The total number of tx bad on a link counted from the last radio chip reset.
     *
     * @param linkId Identifier of the link.
     * @return total tx bad count.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public long getTotalTxBad(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mTotalTxBad;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * The total number of rx success counted from the last radio chip reset. In case of Multi
     * Link Operation (MLO), the returned value is the sum of total number of rx success on all
     * associated links.
     *
     * @return total rx success count.
     */
    public long getTotalRxSuccess() {
        return mTotalRxSuccess;
    }

    /**
     * The total number of rx success on a link counted from the last radio chip reset.
     *
     * @param linkId Identifier of the link.
     * @return total rx success count.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public long getTotalRxSuccess(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mTotalRxSuccess;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /** The total time the wifi radio is on in ms counted from the last radio chip reset */
    public long getTotalRadioOnTimeMillis() {
        return mTotalRadioOnTimeMillis;
    }

    /** The total time the wifi radio is doing tx in ms counted from the last radio chip reset */
    public long getTotalRadioTxTimeMillis() {
        return mTotalRadioTxTimeMillis;
    }

    /** The total time the wifi radio is doing rx in ms counted from the last radio chip reset */
    public long getTotalRadioRxTimeMillis() {
        return mTotalRadioRxTimeMillis;
    }

    /** The total time spent on all types of scans in ms counted from the last radio chip reset */
    public long getTotalScanTimeMillis() {
        return mTotalScanTimeMillis;
    }

    /** The total time spent on nan scans in ms counted from the last radio chip reset */
    public long getTotalNanScanTimeMillis() {
        return mTotalNanScanTimeMillis;
    }

    /** The total time spent on background scans in ms counted from the last radio chip reset */
    public long getTotalBackgroundScanTimeMillis() {
        return mTotalBackgroundScanTimeMillis;
    }

    /** The total time spent on roam scans in ms counted from the last radio chip reset */
    public long getTotalRoamScanTimeMillis() {
        return mTotalRoamScanTimeMillis;
    }

    /** The total time spent on pno scans in ms counted from the last radio chip reset */
    public long getTotalPnoScanTimeMillis() {
        return mTotalPnoScanTimeMillis;
    }

    /** The total time spent on hotspot2.0 scans and GAS exchange in ms counted from the last radio
     * chip reset */
    public long getTotalHotspot2ScanTimeMillis() {
        return mTotalHotspot2ScanTimeMillis;
    }

    /**
     * The total time CCA is on busy status on the current frequency in ms counted from the last
     * radio chip reset.In case of Multi Link Operation (MLO), returned value is the total time
     * CCA is on busy status on the current frequency of the associated link with the highest
     * RSSI.
     */
    public long getTotalCcaBusyFreqTimeMillis() {
        return mTotalCcaBusyFreqTimeMillis;
    }

    /**
     * The total time CCA is on busy status on the link frequency in ms counted from the last radio
     * chip reset.
     *
     * @param linkId Identifier of the link.
     * @return total time CCA is on busy status for the link in ms.
     * @throws NoSuchElementException if linkId is invalid.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public long getTotalCcaBusyFreqTimeMillis(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mTotalCcaBusyFreqTimeMillis;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * The total radio on time on the current frequency from the last radio chip reset. In case of
     * Multi Link Operation (MLO), returned value is the total time radio on time on the current
     * frequency of the associated link with the highest RSSI.
     */
    public long getTotalRadioOnFreqTimeMillis() {
        return mTotalRadioOnFreqTimeMillis;
    }

    /**
     * The total radio on time on the link frequency from the last radio chip reset
     *
     * @param linkId Identifier of the link.
     * @return The total radio on time for the link in ms.
     * @throws NoSuchElementException if linkId is invalid.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public long getTotalRadioOnFreqTimeMillis(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mTotalRadioOnFreqTimeMillis;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * The total number of beacons received from the last radio chip reset. In case of Multi Link
     * Operation (MLO), the returned value is the beacons received on the associated link with
     * the highest RSSI.
     *
     * @return total beacons received.
     */
    public long getTotalBeaconRx() {
        return mTotalBeaconRx;
    }

    /**
     * The total number of beacons received from the last radio chip reset.
     *
     * @param linkId Identifier of the link.
     * @return total beacons received.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public long getTotalBeaconRx(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mTotalBeaconRx;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /** The status of link probe since last stats update */
    @ProbeStatus public int getProbeStatusSinceLastUpdate() {
        return mProbeStatusSinceLastUpdate;
    }

    /** The elapsed time of the most recent link probe since last stats update */
    public int getProbeElapsedTimeSinceLastUpdateMillis() {
        return mProbeElapsedTimeSinceLastUpdateMillis;
    }

    /** The MCS rate of the most recent link probe since last stats update */
    public int getProbeMcsRateSinceLastUpdate() {
        return mProbeMcsRateSinceLastUpdate;
    }

    /**
     * Rx link speed at the sample time in Mbps. In case of Multi Link Operation (MLO), returned
     * value is the receive link speed of the associated link with the highest RSSI.
     *
     * @return Receive link speed in Mbps or {@link WifiInfo#LINK_SPEED_UNKNOWN} if link speed
     * is unknown.
     */
    public int getRxLinkSpeedMbps() {
        return mRxLinkSpeedMbps;
    }

    /**
     * Rx Link speed of the link at the sample time in Mbps.
     *
     * @param linkId Identifier of the link.
     * @return Receive link speed in Mbps or {@link WifiInfo#LINK_SPEED_UNKNOWN} if link speed
     * is unknown.
     * @throws NoSuchElementException if linkId is invalid.
     */
    public int getRxLinkSpeedMbps(int linkId) {
        if (mLinkStats.contains(linkId)) return mLinkStats.get(linkId).mRxLinkSpeedMbps;
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * Duty cycle of the connection.
     * If this connection is being served using time slicing on a radio with one or more interfaces
     * (i.e MCC), then this method returns the duty cycle assigned to this interface in percent.
     * If no concurrency or not using time slicing during concurrency (i.e SCC or DBS), set to 100.
     * In case of Multi Link Operation (MLO), returned value is the duty cycle of the associated
     * link with the highest RSSI.
     *
     * @return duty cycle in percent if known.
     * @throws NoSuchElementException if the duty cycle is unknown (not provided by the HAL).
     */
    public @IntRange(from = 0, to = 100) int getTimeSliceDutyCycleInPercent() {
        if (mTimeSliceDutyCycleInPercent == -1) {
            throw new NoSuchElementException("Unknown value");
        }
        return mTimeSliceDutyCycleInPercent;
    }

    /**
     * Duty cycle of the connection for a link.
     * If this connection is being served using time slicing on a radio with one or more interfaces
     * (i.e MCC) and links, then this method returns the duty cycle assigned to this interface in
     * percent for the link. If no concurrency or not using time slicing during concurrency (i.e SCC
     * or DBS), set to 100.
     *
     * @param linkId Identifier of the link.
     * @return duty cycle in percent if known.
     * @throws NoSuchElementException if the duty cycle is unknown (not provided by the HAL) or
     * linkId is invalid.
     */
    public @IntRange(from = 0, to = 100) int getTimeSliceDutyCycleInPercent(int linkId) {
        if (!mLinkStats.contains(linkId)) {
            throw new NoSuchElementException("linkId is invalid - " + linkId);
        }
        if (mLinkStats.get(linkId).mTimeSliceDutyCycleInPercent == -1) {
            throw new NoSuchElementException("Unknown value");
        }
        return mLinkStats.get(linkId).mTimeSliceDutyCycleInPercent;
    }

    /**
     * Data packet contention time statistics for Access Category. In case of Multi Link
     * Operation (MLO), the returned value is the contention stats of the associated link with the
     * highest RSSI.
     *
     * @param ac The access category, see {@link WmeAccessCategory}.
     * @return The contention time statistics, see {@link ContentionTimeStats}
     */
    @NonNull
    public ContentionTimeStats getContentionTimeStats(@WmeAccessCategory int ac) {
        if (mContentionTimeStats != null
                && mContentionTimeStats.length == NUM_WME_ACCESS_CATEGORIES) {
            return mContentionTimeStats[ac];
        }
        Log.e(TAG, "The ContentionTimeStats is not filled out correctly: "
                + Arrays.toString(mContentionTimeStats));
        return new ContentionTimeStats();
    }

    /**
     * Data packet contention time statistics of a link for Access Category.
     *
     * @param linkId Identifier of the link.
     * @param ac The access category, see {@link WmeAccessCategory}.
     * @return The contention time statistics, see {@link ContentionTimeStats}.
     * @throws NoSuchElementException if linkId is invalid.
     */
    @NonNull
    public ContentionTimeStats getContentionTimeStats(int linkId, @WmeAccessCategory int ac) {
        if (!mLinkStats.contains(linkId)) {
            throw new NoSuchElementException("linkId is invalid - " + linkId);
        }
        ContentionTimeStats[] linkContentionTimeStats = mLinkStats.get(
                linkId).mContentionTimeStats;
        if (linkContentionTimeStats != null
                && linkContentionTimeStats.length == NUM_WME_ACCESS_CATEGORIES) {
            return linkContentionTimeStats[ac];
        }
        Log.e(TAG, "The ContentionTimeStats is not filled out correctly: "
                + Arrays.toString(mContentionTimeStats));
        return new ContentionTimeStats();
    }

    /**
     * Rate information and statistics, which are ordered by preamble, modulation and coding scheme
     * (MCS), and number of spatial streams (NSS). In case of Multi Link Operation (MLO), the
     * returned rate information is that of the associated link with the highest RSSI.
     *
     * @return A list of rate statistics in the form of a list of {@link RateStats} objects.
     *         Depending on the link type, the list is created following the order of:
     *         - HT (IEEE Std 802.11-2020, Section 19): LEGACY rates (1Mbps, ..., 54Mbps),
     *           HT MCS0, ..., MCS15;
     *         - VHT (IEEE Std 802.11-2020, Section 21): LEGACY rates (1Mbps, ..., 54Mbps),
     *           VHT MCS0/NSS1, ..., VHT MCS11/NSS1, VHT MCSO/NSS2, ..., VHT MCS11/NSS2;
     *         - HE (IEEE Std 802.11ax-2020, Section 27): LEGACY rates (1Mbps, ..., 54Mbps),
     *           HE MCS0/NSS1, ..., HE MCS11/NSS1, HE MCSO/NSS2, ..., HE MCS11/NSS2.
     *         - EHT (IEEE std 802.11be-2021, Section 36): Legacy rates (1Mbps, ..., 54Mbps),
     *           EHT MSC0/NSS1, ..., EHT MCS14/NSS1, EHT MCS0/NSS2, ..., EHT MCS14/NSS2.
     */
    @NonNull
    public List<RateStats> getRateStats() {
        if (mRateStats != null) {
            return Arrays.asList(mRateStats);
        }
        return Collections.emptyList();
    }

    /**
     * Rate information and statistics, which are ordered by preamble, modulation and coding scheme
     * (MCS), and number of spatial streams (NSS) for link.
     *
     * @param linkId Identifier of the link.
     * @return A list of rate statistics in the form of a list of {@link RateStats} objects.
     *         Depending on the link type, the list is created following the order of:
     *         - HT (IEEE Std 802.11-2020, Section 19): LEGACY rates (1Mbps, ..., 54Mbps),
     *           HT MCS0, ..., MCS15;
     *         - VHT (IEEE Std 802.11-2020, Section 21): LEGACY rates (1Mbps, ..., 54Mbps),
     *           VHT MCS0/NSS1, ..., VHT MCS11/NSS1, VHT MCSO/NSS2, ..., VHT MCS11/NSS2;
     *         - HE (IEEE Std 802.11ax-2020, Section 27): LEGACY rates (1Mbps, ..., 54Mbps),
     *           HE MCS0/NSS1, ..., HE MCS11/NSS1, HE MCSO/NSS2, ..., HE MCS11/NSS2.
     *         - EHT (IEEE std 802.11be-2021, Section 36): Legacy rates (1Mbps, ..., 54Mbps),
     *           EHT MSC0/NSS1, ..., EHT MCS14/NSS1, EHT MCS0/NSS2, ..., EHT MCS14/NSS2.
     * @throws NoSuchElementException if linkId is invalid.
     */
    @NonNull
    public List<RateStats> getRateStats(int linkId) {
        if (mLinkStats.contains(linkId)) {
            RateStats[] rateStats = mLinkStats.get(linkId).mRateStats;
            if (rateStats != null) return Arrays.asList(rateStats);
            return Collections.emptyList();
        }
        throw new NoSuchElementException("linkId is invalid - " + linkId);
    }

    /**
     * Radio stats from all the radios, see {@link RadioStats#getRadioId()}
     * @return A list of Wifi link layer radio stats, see {@link RadioStats}
     */
    @NonNull
    public List<RadioStats> getWifiLinkLayerRadioStats() {
        if (mRadioStats != null) {
            return Arrays.asList(mRadioStats);
        }
        return Collections.emptyList();
    }

    /**
     * Channel utilization ratio on the current channel.
     *
     * @return The channel utilization ratio (value) in the range of [0, 255], where x corresponds
     *         to (x * 100 / 255)%, or -1 indicating that there is no valid value to return.
     */
    public @IntRange(from = -1, to = 255) int getChannelUtilizationRatio() {
        return mChannelUtilizationRatio;
    }

    /**
     * Indicate whether current link layer (L2) throughput is sufficient.  L2 throughput is
     * sufficient when one of the following conditions is met: 1) L3 throughput is low and L2
     * throughput is above its low threshold; 2) L3 throughput is not low and L2 throughput over L3
     * throughput ratio is above a threshold; 3) L3 throughput is not low and L2 throughput is
     * above its high threshold.
     *
     * @return true if it is sufficient or false if it is insufficient.
     */
    public boolean isThroughputSufficient() {
        return mIsThroughputSufficient;
    }

    /**
     * Indicate whether Wi-Fi scoring is enabled by the user,
     * see {@link WifiManager#setWifiScoringEnabled(boolean)}.
     *
     * @return true if it is enabled.
     */
    public boolean isWifiScoringEnabled() {
        return mIsWifiScoringEnabled;
    }

    /**
     * Indicate whether Cellular data is available.
     *
     * @return true if it is available and false otherwise.
     */
    public boolean isCellularDataAvailable() {
        return mIsCellularDataAvailable;
    }

    /** Cellular data network type currently in use on the device for data transmission */
    @NetworkType public int getCellularDataNetworkType() {
        return mCellularDataNetworkType;
    }

    /**
     * Cellular signal strength in dBm, NR: CsiRsrp, LTE: Rsrp, WCDMA/TDSCDMA: Rscp,
     * CDMA: Rssi, EVDO: Rssi, GSM: Rssi
     */
    public int getCellularSignalStrengthDbm() {
        return mCellularSignalStrengthDbm;
    }

    /**
     * Cellular signal strength in dB, NR: CsiSinr, LTE: Rsrq, WCDMA: EcNo, TDSCDMA: invalid,
     * CDMA: Ecio, EVDO: SNR, GSM: invalid
     */
    public int getCellularSignalStrengthDb() {
        return mCellularSignalStrengthDb;
    }

    /** Whether the primary registered cell of current entry is same as that of previous entry */
    public boolean isSameRegisteredCell() {
        return mIsSameRegisteredCell;
    }
}
