/*
 * Copyright 2019 The Android Open Source Project
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

package android.media.tv.tuner.frontend;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.hardware.tv.tuner.FrontendAtsc3Bandwidth;
import android.hardware.tv.tuner.FrontendAtsc3CodeRate;
import android.hardware.tv.tuner.FrontendAtsc3DemodOutputFormat;
import android.hardware.tv.tuner.FrontendAtsc3Fec;
import android.hardware.tv.tuner.FrontendAtsc3Modulation;
import android.hardware.tv.tuner.FrontendAtsc3TimeInterleaveMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Frontend settings for ATSC-3.
 *
 * @hide
 */
@SystemApi
public class Atsc3FrontendSettings extends FrontendSettings {

    /** @hide */
    @IntDef(prefix = "BANDWIDTH_",
            value = {BANDWIDTH_UNDEFINED, BANDWIDTH_AUTO, BANDWIDTH_BANDWIDTH_6MHZ,
                    BANDWIDTH_BANDWIDTH_7MHZ, BANDWIDTH_BANDWIDTH_8MHZ})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Bandwidth {}

    /**
     * Bandwidth not defined.
     */
    public static final int BANDWIDTH_UNDEFINED = FrontendAtsc3Bandwidth.UNDEFINED;
    /**
     * Hardware is able to detect and set bandwidth automatically
     */
    public static final int BANDWIDTH_AUTO = FrontendAtsc3Bandwidth.AUTO;
    /**
     * 6 MHz bandwidth.
     */
    public static final int BANDWIDTH_BANDWIDTH_6MHZ = FrontendAtsc3Bandwidth.BANDWIDTH_6MHZ;
    /**
     * 7 MHz bandwidth.
     */
    public static final int BANDWIDTH_BANDWIDTH_7MHZ = FrontendAtsc3Bandwidth.BANDWIDTH_7MHZ;
    /**
     * 8 MHz bandwidth.
     */
    public static final int BANDWIDTH_BANDWIDTH_8MHZ = FrontendAtsc3Bandwidth.BANDWIDTH_8MHZ;


    /** @hide */
    @IntDef(prefix = "MODULATION_",
            value = {MODULATION_UNDEFINED, MODULATION_AUTO,
                    MODULATION_MOD_QPSK, MODULATION_MOD_16QAM,
                    MODULATION_MOD_64QAM, MODULATION_MOD_256QAM,
                    MODULATION_MOD_1024QAM, MODULATION_MOD_4096QAM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Modulation {}

    /**
     * Modulation undefined.
     */
    public static final int MODULATION_UNDEFINED = FrontendAtsc3Modulation.UNDEFINED;
    /**
     * Hardware is able to detect and set modulation automatically.
     */
    public static final int MODULATION_AUTO = FrontendAtsc3Modulation.AUTO;
    /**
     * QPSK modulation.
     */
    public static final int MODULATION_MOD_QPSK = FrontendAtsc3Modulation.MOD_QPSK;
    /**
     * 16QAM modulation.
     */
    public static final int MODULATION_MOD_16QAM = FrontendAtsc3Modulation.MOD_16QAM;
    /**
     * 64QAM modulation.
     */
    public static final int MODULATION_MOD_64QAM = FrontendAtsc3Modulation.MOD_64QAM;
    /**
     * 256QAM modulation.
     */
    public static final int MODULATION_MOD_256QAM = FrontendAtsc3Modulation.MOD_256QAM;
    /**
     * 1024QAM modulation.
     */
    public static final int MODULATION_MOD_1024QAM = FrontendAtsc3Modulation.MOD_1024QAM;
    /**
     * 4096QAM modulation.
     */
    public static final int MODULATION_MOD_4096QAM = FrontendAtsc3Modulation.MOD_4096QAM;


    /** @hide */
    @IntDef(prefix = "TIME_INTERLEAVE_MODE_",
            value = {TIME_INTERLEAVE_MODE_UNDEFINED, TIME_INTERLEAVE_MODE_AUTO,
                    TIME_INTERLEAVE_MODE_CTI, TIME_INTERLEAVE_MODE_HTI})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TimeInterleaveMode {}

    /**
     * Time interleave mode undefined.
     */
    public static final int TIME_INTERLEAVE_MODE_UNDEFINED =
            FrontendAtsc3TimeInterleaveMode.UNDEFINED;
    /**
     * Hardware is able to detect and set Time Interleave Mode automatically.
     */
    public static final int TIME_INTERLEAVE_MODE_AUTO = FrontendAtsc3TimeInterleaveMode.AUTO;
    /**
     * CTI Time Interleave Mode.
     */
    public static final int TIME_INTERLEAVE_MODE_CTI = FrontendAtsc3TimeInterleaveMode.CTI;
    /**
     * HTI Time Interleave Mode.
     */
    public static final int TIME_INTERLEAVE_MODE_HTI = FrontendAtsc3TimeInterleaveMode.HTI;


    /** @hide */
    @IntDef(prefix = "CODERATE_",
            value = {CODERATE_UNDEFINED, CODERATE_AUTO, CODERATE_2_15, CODERATE_3_15, CODERATE_4_15,
                    CODERATE_5_15, CODERATE_6_15, CODERATE_7_15, CODERATE_8_15, CODERATE_9_15,
                    CODERATE_10_15, CODERATE_11_15, CODERATE_12_15, CODERATE_13_15})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CodeRate {}

    /**
     * Code rate undefined.
     */
    public static final int CODERATE_UNDEFINED = FrontendAtsc3CodeRate.UNDEFINED;
    /**
     * Hardware is able to detect and set code rate automatically
     */
    public static final int CODERATE_AUTO = FrontendAtsc3CodeRate.AUTO;
    /**
     * 2/15 code rate.
     */
    public static final int CODERATE_2_15 = FrontendAtsc3CodeRate.CODERATE_2_15;
    /**
     * 3/15 code rate.
     */
    public static final int CODERATE_3_15 = FrontendAtsc3CodeRate.CODERATE_3_15;
    /**
     * 4/15 code rate.
     */
    public static final int CODERATE_4_15 = FrontendAtsc3CodeRate.CODERATE_4_15;
    /**
     * 5/15 code rate.
     */
    public static final int CODERATE_5_15 = FrontendAtsc3CodeRate.CODERATE_5_15;
    /**
     * 6/15 code rate.
     */
    public static final int CODERATE_6_15 = FrontendAtsc3CodeRate.CODERATE_6_15;
    /**
     * 7/15 code rate.
     */
    public static final int CODERATE_7_15 = FrontendAtsc3CodeRate.CODERATE_7_15;
    /**
     * 8/15 code rate.
     */
    public static final int CODERATE_8_15 = FrontendAtsc3CodeRate.CODERATE_8_15;
    /**
     * 9/15 code rate.
     */
    public static final int CODERATE_9_15 = FrontendAtsc3CodeRate.CODERATE_9_15;
    /**
     * 10/15 code rate.
     */
    public static final int CODERATE_10_15 = FrontendAtsc3CodeRate.CODERATE_10_15;
    /**
     * 11/15 code rate.
     */
    public static final int CODERATE_11_15 = FrontendAtsc3CodeRate.CODERATE_11_15;
    /**
     * 12/15 code rate.
     */
    public static final int CODERATE_12_15 = FrontendAtsc3CodeRate.CODERATE_12_15;
    /**
     * 13/15 code rate.
     */
    public static final int CODERATE_13_15 = FrontendAtsc3CodeRate.CODERATE_13_15;


    /** @hide */
    @IntDef(prefix = "FEC_",
            value = {FEC_UNDEFINED, FEC_AUTO, FEC_BCH_LDPC_16K, FEC_BCH_LDPC_64K, FEC_CRC_LDPC_16K,
                    FEC_CRC_LDPC_64K, FEC_LDPC_16K, FEC_LDPC_64K})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Fec {}

    /**
     * Forward Error Correction undefined.
     */
    public static final int FEC_UNDEFINED = FrontendAtsc3Fec.UNDEFINED;
    /**
     * Hardware is able to detect and set FEC automatically
     */
    public static final int FEC_AUTO = FrontendAtsc3Fec.AUTO;
    /**
     * BCH LDPC 16K Forward Error Correction
     */
    public static final int FEC_BCH_LDPC_16K = FrontendAtsc3Fec.BCH_LDPC_16K;
    /**
     * BCH LDPC 64K Forward Error Correction
     */
    public static final int FEC_BCH_LDPC_64K = FrontendAtsc3Fec.BCH_LDPC_64K;
    /**
     * CRC LDPC 16K Forward Error Correction
     */
    public static final int FEC_CRC_LDPC_16K = FrontendAtsc3Fec.CRC_LDPC_16K;
    /**
     * CRC LDPC 64K Forward Error Correction
     */
    public static final int FEC_CRC_LDPC_64K = FrontendAtsc3Fec.CRC_LDPC_64K;
    /**
     * LDPC 16K Forward Error Correction
     */
    public static final int FEC_LDPC_16K = FrontendAtsc3Fec.LDPC_16K;
    /**
     * LDPC 64K Forward Error Correction
     */
    public static final int FEC_LDPC_64K = FrontendAtsc3Fec.LDPC_64K;


    /** @hide */
    @IntDef(prefix = "DEMOD_OUTPUT_FORMAT_",
            value = {DEMOD_OUTPUT_FORMAT_UNDEFINED, DEMOD_OUTPUT_FORMAT_ATSC3_LINKLAYER_PACKET,
                    DEMOD_OUTPUT_FORMAT_BASEBAND_PACKET})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DemodOutputFormat {}

    /**
     * Demod output format undefined.
     */
    public static final int DEMOD_OUTPUT_FORMAT_UNDEFINED =
            FrontendAtsc3DemodOutputFormat.UNDEFINED;
    /**
     * ALP format. Typically used in US region.
     */
    public static final int DEMOD_OUTPUT_FORMAT_ATSC3_LINKLAYER_PACKET =
            FrontendAtsc3DemodOutputFormat.ATSC3_LINKLAYER_PACKET;
    /**
     * BaseBand packet format. Typically used in Korea region.
     */
    public static final int DEMOD_OUTPUT_FORMAT_BASEBAND_PACKET =
            FrontendAtsc3DemodOutputFormat.BASEBAND_PACKET;

    private final int mBandwidth;
    private final int mDemodOutputFormat;
    private final Atsc3PlpSettings[] mPlpSettings;

    private Atsc3FrontendSettings(
            long frequency, int bandwidth, int demodOutputFormat, Atsc3PlpSettings[] plpSettings) {
        super(frequency);
        mBandwidth = bandwidth;
        mDemodOutputFormat = demodOutputFormat;
        mPlpSettings = plpSettings;
    }

    /**
     * Gets bandwidth.
     */
    @Bandwidth
    public int getBandwidth() {
        return mBandwidth;
    }
    /**
     * Gets Demod Output Format.
     */
    @DemodOutputFormat
    public int getDemodOutputFormat() {
        return mDemodOutputFormat;
    }
    /**
     * Gets PLP Settings.
     */
    @NonNull
    public Atsc3PlpSettings[] getPlpSettings() {
        return mPlpSettings;
    }

    /**
     * Creates a builder for {@link Atsc3FrontendSettings}.
     *
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Atsc3FrontendSettings}.
     */
    public static class Builder {
        private long mFrequency = 0;
        private int mBandwidth = BANDWIDTH_UNDEFINED;
        private int mDemodOutputFormat = DEMOD_OUTPUT_FORMAT_UNDEFINED;
        private Atsc3PlpSettings[] mPlpSettings = {};

        private Builder() {
        }

        /**
         * Sets frequency in Hz.
         *
         * <p>Default value is 0.
         * @deprecated Use {@link #setFrequencyLong(long)}
         */
        @NonNull
        @IntRange(from = 1)
        @Deprecated
        public Builder setFrequency(int frequency) {
            return setFrequencyLong((long) frequency);
        }

        /**
         * Sets frequency in Hz.
         *
         * <p>Default value is 0.
         */
        @NonNull
        @IntRange(from = 1)
        public Builder setFrequencyLong(long frequency) {
            mFrequency = frequency;
            return this;
        }

        /**
         * Sets bandwidth.
         *
         * <p>Default value is {@link #BANDWIDTH_UNDEFINED}.
         */
        @NonNull
        public Builder setBandwidth(int bandwidth) {
            mBandwidth = bandwidth;
            return this;
        }
        /**
         * Sets Demod Output Format.
         *
         * <p>Default value is {@link #DEMOD_OUTPUT_FORMAT_UNDEFINED}.
         */
        @NonNull
        public Builder setDemodOutputFormat(@DemodOutputFormat int demodOutputFormat) {
            mDemodOutputFormat = demodOutputFormat;
            return this;
        }
        /**
         * Sets PLP Settings.
         *
         * <p>Default value an empty array.
         */
        @NonNull
        public Builder setPlpSettings(@NonNull Atsc3PlpSettings[] plpSettings) {
            mPlpSettings = plpSettings;
            return this;
        }

        /**
         * Builds a {@link Atsc3FrontendSettings} object.
         */
        @NonNull
        public Atsc3FrontendSettings build() {
            return new Atsc3FrontendSettings(mFrequency, mBandwidth, mDemodOutputFormat,
                    mPlpSettings);
        }
    }

    @Override
    public int getType() {
        return FrontendSettings.TYPE_ATSC3;
    }
}
