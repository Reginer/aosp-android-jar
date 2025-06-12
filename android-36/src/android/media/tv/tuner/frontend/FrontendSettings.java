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
import android.annotation.LongDef;
import android.annotation.SystemApi;
import android.hardware.tv.tuner.FrontendInnerFec;
import android.hardware.tv.tuner.FrontendType;
import android.media.tv.tuner.Tuner;
import android.media.tv.tuner.TunerVersionChecker;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Frontend settings for tune and scan operations.
 *
 * @hide
 */
@SystemApi
public abstract class FrontendSettings {
    /** @hide */
    @IntDef(prefix = "TYPE_",
            value = {TYPE_UNDEFINED, TYPE_ANALOG, TYPE_ATSC, TYPE_ATSC3, TYPE_DVBC, TYPE_DVBS,
                    TYPE_DVBT, TYPE_ISDBS, TYPE_ISDBS3, TYPE_ISDBT, TYPE_DTMB, TYPE_IPTV})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}

    /**
     * Undefined frontend type.
     */
    public static final int TYPE_UNDEFINED = FrontendType.UNDEFINED;
    /**
     * Analog frontend type.
     */
    public static final int TYPE_ANALOG = FrontendType.ANALOG;
    /**
     * Advanced Television Systems Committee (ATSC) frontend type.
     */
    public static final int TYPE_ATSC = FrontendType.ATSC;
    /**
     * Advanced Television Systems Committee 3.0 (ATSC-3) frontend type.
     */
    public static final int TYPE_ATSC3 = FrontendType.ATSC3;
    /**
     * Digital Video Broadcasting-Cable (DVB-C) frontend type.
     */
    public static final int TYPE_DVBC = FrontendType.DVBC;
    /**
     * Digital Video Broadcasting-Satellite (DVB-S) frontend type.
     */
    public static final int TYPE_DVBS = FrontendType.DVBS;
    /**
     * Digital Video Broadcasting-Terrestrial (DVB-T) frontend type.
     */
    public static final int TYPE_DVBT = FrontendType.DVBT;
    /**
     * Integrated Services Digital Broadcasting-Satellite (ISDB-S) frontend type.
     */
    public static final int TYPE_ISDBS = FrontendType.ISDBS;
    /**
     * Integrated Services Digital Broadcasting-Satellite 3 (ISDB-S3) frontend type.
     */
    public static final int TYPE_ISDBS3 = FrontendType.ISDBS3;
    /**
     * Integrated Services Digital Broadcasting-Terrestrial (ISDB-T) frontend type.
     */
    public static final int TYPE_ISDBT = FrontendType.ISDBT;
    /**
     * Digital Terrestrial Multimedia Broadcast standard (DTMB) frontend type.
     */
    public static final int TYPE_DTMB = FrontendType.DTMB;
    /**
     * Internet Protocol (IPTV) frontend type.
     */
    public static final int TYPE_IPTV = FrontendType.IPTV;

    /** @hide */
    @LongDef(prefix = "FEC_",
            value = {FEC_UNDEFINED, FEC_AUTO, FEC_1_2, FEC_1_3, FEC_1_4, FEC_1_5, FEC_2_3, FEC_2_5,
            FEC_2_9, FEC_3_4, FEC_3_5, FEC_4_5, FEC_4_15, FEC_5_6, FEC_5_9, FEC_6_7, FEC_7_8,
            FEC_7_9, FEC_7_15, FEC_8_9, FEC_8_15, FEC_9_10, FEC_9_20, FEC_11_15, FEC_11_20,
            FEC_11_45, FEC_13_18, FEC_13_45, FEC_14_45, FEC_23_36, FEC_25_36, FEC_26_45, FEC_28_45,
            FEC_29_45, FEC_31_45, FEC_32_45, FEC_77_90})
    @Retention(RetentionPolicy.SOURCE)
    public @interface InnerFec {}

    /**
     * FEC not defined.
     */
    public static final long FEC_UNDEFINED = FrontendInnerFec.FEC_UNDEFINED;
    /**
     * hardware is able to detect and set FEC automatically.
     */
    public static final long FEC_AUTO = FrontendInnerFec.AUTO;
    /**
     * 1/2 conv. code rate.
     */
    public static final long FEC_1_2 = FrontendInnerFec.FEC_1_2;
    /**
     * 1/3 conv. code rate.
     */
    public static final long FEC_1_3 = FrontendInnerFec.FEC_1_3;
    /**
     * 1/4 conv. code rate.
     */
    public static final long FEC_1_4 = FrontendInnerFec.FEC_1_4;
    /**
     * 1/5 conv. code rate.
     */
    public static final long FEC_1_5 = FrontendInnerFec.FEC_1_5;
    /**
     * 2/3 conv. code rate.
     */
    public static final long FEC_2_3 = FrontendInnerFec.FEC_2_3;
    /**
     * 2/5 conv. code rate.
     */
    public static final long FEC_2_5 = FrontendInnerFec.FEC_2_5;
    /**
     * 2/9 conv. code rate.
     */
    public static final long FEC_2_9 = FrontendInnerFec.FEC_2_9;
    /**
     * 3/4 conv. code rate.
     */
    public static final long FEC_3_4 = FrontendInnerFec.FEC_3_4;
    /**
     * 3/5 conv. code rate.
     */
    public static final long FEC_3_5 = FrontendInnerFec.FEC_3_5;
    /**
     * 4/5 conv. code rate.
     */
    public static final long FEC_4_5 = FrontendInnerFec.FEC_4_5;
    /**
     * 4/15 conv. code rate.
     */
    public static final long FEC_4_15 = FrontendInnerFec.FEC_4_15;
    /**
     * 5/6 conv. code rate.
     */
    public static final long FEC_5_6 = FrontendInnerFec.FEC_5_6;
    /**
     * 5/9 conv. code rate.
     */
    public static final long FEC_5_9 = FrontendInnerFec.FEC_5_9;
    /**
     * 6/7 conv. code rate.
     */
    public static final long FEC_6_7 = FrontendInnerFec.FEC_6_7;
    /**
     * 7/8 conv. code rate.
     */
    public static final long FEC_7_8 = FrontendInnerFec.FEC_7_8;
    /**
     * 7/9 conv. code rate.
     */
    public static final long FEC_7_9 = FrontendInnerFec.FEC_7_9;
    /**
     * 7/15 conv. code rate.
     */
    public static final long FEC_7_15 = FrontendInnerFec.FEC_7_15;
    /**
     * 8/9 conv. code rate.
     */
    public static final long FEC_8_9 = FrontendInnerFec.FEC_8_9;
    /**
     * 8/15 conv. code rate.
     */
    public static final long FEC_8_15 = FrontendInnerFec.FEC_8_15;
    /**
     * 9/10 conv. code rate.
     */
    public static final long FEC_9_10 = FrontendInnerFec.FEC_9_10;
    /**
     * 9/20 conv. code rate.
     */
    public static final long FEC_9_20 = FrontendInnerFec.FEC_9_20;
    /**
     * 11/15 conv. code rate.
     */
    public static final long FEC_11_15 = FrontendInnerFec.FEC_11_15;
    /**
     * 11/20 conv. code rate.
     */
    public static final long FEC_11_20 = FrontendInnerFec.FEC_11_20;
    /**
     * 11/45 conv. code rate.
     */
    public static final long FEC_11_45 = FrontendInnerFec.FEC_11_45;
    /**
     * 13/18 conv. code rate.
     */
    public static final long FEC_13_18 = FrontendInnerFec.FEC_13_18;
    /**
     * 13/45 conv. code rate.
     */
    public static final long FEC_13_45 = FrontendInnerFec.FEC_13_45;
    /**
     * 14/45 conv. code rate.
     */
    public static final long FEC_14_45 = FrontendInnerFec.FEC_14_45;
    /**
     * 23/36 conv. code rate.
     */
    public static final long FEC_23_36 = FrontendInnerFec.FEC_23_36;
    /**
     * 25/36 conv. code rate.
     */
    public static final long FEC_25_36 = FrontendInnerFec.FEC_25_36;
    /**
     * 26/45 conv. code rate.
     */
    public static final long FEC_26_45 = FrontendInnerFec.FEC_26_45;
    /**
     * 28/45 conv. code rate.
     */
    public static final long FEC_28_45 = FrontendInnerFec.FEC_28_45;
    /**
     * 29/45 conv. code rate.
     */
    public static final long FEC_29_45 = FrontendInnerFec.FEC_29_45;
    /**
     * 31/45 conv. code rate.
     */
    public static final long FEC_31_45 = FrontendInnerFec.FEC_31_45;
    /**
     * 32/45 conv. code rate.
     */
    public static final long FEC_32_45 = FrontendInnerFec.FEC_32_45;
    /**
     * 77/90 conv. code rate.
     */
    public static final long FEC_77_90 = FrontendInnerFec.FEC_77_90;

    /** @hide */
    @IntDef(prefix = "FRONTEND_SPECTRAL_INVERSION_",
            value = {FRONTEND_SPECTRAL_INVERSION_UNDEFINED, FRONTEND_SPECTRAL_INVERSION_NORMAL,
                    FRONTEND_SPECTRAL_INVERSION_INVERTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FrontendSpectralInversion {}

    /**
     * Spectral Inversion Type undefined.
     */
    public static final int FRONTEND_SPECTRAL_INVERSION_UNDEFINED =
            android.hardware.tv.tuner.FrontendSpectralInversion.UNDEFINED;
    /**
     * Normal Spectral Inversion.
     */
    public static final int FRONTEND_SPECTRAL_INVERSION_NORMAL =
            android.hardware.tv.tuner.FrontendSpectralInversion.NORMAL;
    /**
     * Inverted Spectral Inversion.
     */
    public static final int FRONTEND_SPECTRAL_INVERSION_INVERTED =
            android.hardware.tv.tuner.FrontendSpectralInversion.INVERTED;

    private final long mFrequency;
    // End frequency is only supported in Tuner 1.1 or higher.
    private long mEndFrequency = Tuner.INVALID_FRONTEND_SETTING_FREQUENCY;
    // General spectral inversion is only supported in Tuner 1.1 or higher.
    private int mSpectralInversion = FRONTEND_SPECTRAL_INVERSION_UNDEFINED;

    FrontendSettings(long frequency) { mFrequency = frequency; }

    /**
     * Returns the frontend type.
     */
    @Type
    public abstract int getType();

    /**
     * Gets the frequency.
     *
     * @return the frequency in Hz.
     * @deprecated Use {@link #getFrequencyLong()}
     */
    @Deprecated
    public int getFrequency() {
        return (int) getFrequencyLong();
    }

    /**
     * Gets the frequency.
     *
     * @return the frequency in Hz.
     */
    public long getFrequencyLong() {
        return mFrequency;
    }

    /**
     * Get the end frequency.
     *
     * @return the end frequency in Hz.
     * @deprecated Use {@link #getEndFrequencyLong()}
     */
    @Deprecated
    @IntRange(from = 1)
    public int getEndFrequency() {
        return (int) getEndFrequencyLong();
    }

    /**
     * Get the end frequency.
     *
     * @return the end frequency in Hz.
     */
    @IntRange(from = 1)
    public long getEndFrequencyLong() {
        return mEndFrequency;
    }

    /**
     * Get the spectral inversion.
     *
     * @return the value of the spectral inversion.
     */
    @FrontendSpectralInversion
    public int getFrontendSpectralInversion() {
        return mSpectralInversion;
    }

    /**
     * Set Spectral Inversion.
     *
     * <p>This API is only supported by Tuner HAL 1.1 or higher. Unsupported version would cause
     * no-op. Use {@link TunerVersionChecker#getTunerVersion()} to check the version.
     *
     * @param inversion the value to set as the spectral inversion. Default value is {@link
     * #FRONTEND_SPECTRAL_INVERSION_UNDEFINED}.
     */
    public void setSpectralInversion(@FrontendSpectralInversion int inversion) {
        if (TunerVersionChecker.checkHigherOrEqualVersionTo(
                TunerVersionChecker.TUNER_VERSION_1_1, "setSpectralInversion")) {
            mSpectralInversion = inversion;
        }
    }

    /**
     * Set End Frequency. This API is only supported with Tuner HAL 1.1 or higher. Otherwise it
     * would be no-op.
     *
     * <p>This API is only supported by Tuner HAL 1.1 or higher. Unsupported version would cause
     * no-op. Use {@link TunerVersionChecker#getTunerVersion()} to check the version.
     *
     * @param endFrequency the end frequency used during blind scan. The default value is
     * {@link android.media.tv.tuner.Tuner#INVALID_FRONTEND_SETTING_FREQUENCY}.
     * @throws IllegalArgumentException if the {@code endFrequency} is not greater than 0.
     * @deprecated Use {@link #setFrequencyLong(long)}
     */
    @IntRange(from = 1)
    @Deprecated
    public void setEndFrequency(int frequency) {
        setEndFrequencyLong((long) frequency);
    }

    /**
     * Set End Frequency. This API is only supported with Tuner HAL 1.1 or higher. Otherwise it
     * would be no-op.
     *
     * <p>This API is only supported by Tuner HAL 1.1 or higher. Unsupported version would cause
     * no-op. Use {@link TunerVersionChecker#getTunerVersion()} to check the version.
     *
     * @param endFrequency the end frequency used during blind scan. The default value is
     * {@link android.media.tv.tuner.Tuner#INVALID_FRONTEND_SETTING_FREQUENCY}.
     * @throws IllegalArgumentException if the {@code endFrequency} is not greater than 0.
     */
    @IntRange(from = 1)
    public void setEndFrequencyLong(long endFrequency) {
        if (TunerVersionChecker.checkHigherOrEqualVersionTo(
                TunerVersionChecker.TUNER_VERSION_1_1, "setEndFrequency")) {
            if (endFrequency < 1) {
                throw new IllegalArgumentException("endFrequency must be greater than 0");
            }
            mEndFrequency = endFrequency;
        }
    }
}
