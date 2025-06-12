/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package android.location;

import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_AUTOMATIC_GAIN_CONTROL;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_CARRIER_CYCLES;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_CARRIER_FREQUENCY;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_CARRIER_PHASE;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_CARRIER_PHASE_UNCERTAINTY;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_FULL_ISB;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_FULL_ISB_UNCERTAINTY;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_SATELLITE_ISB;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_SATELLITE_ISB_UNCERTAINTY;
import static android.hardware.gnss.V2_1.IGnssMeasurementCallback.GnssMeasurementFlags.HAS_SNR;

import android.annotation.FloatRange;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * A class representing a GNSS satellite measurement, containing raw and computed information.
 */
public final class GnssMeasurement implements Parcelable {
    private int mFlags;
    private int mSvid;
    private int mConstellationType;
    private double mTimeOffsetNanos;
    private int mState;
    private long mReceivedSvTimeNanos;
    private long mReceivedSvTimeUncertaintyNanos;
    private double mCn0DbHz;
    private double mBasebandCn0DbHz;
    private double mPseudorangeRateMetersPerSecond;
    private double mPseudorangeRateUncertaintyMetersPerSecond;
    private int mAccumulatedDeltaRangeState;
    private double mAccumulatedDeltaRangeMeters;
    private double mAccumulatedDeltaRangeUncertaintyMeters;
    private float mCarrierFrequencyHz;
    private long mCarrierCycles;
    private double mCarrierPhase;
    private double mCarrierPhaseUncertainty;
    private int mMultipathIndicator;
    private double mSnrInDb;
    private double mAutomaticGainControlLevelInDb;
    @NonNull private String mCodeType;
    private double mFullInterSignalBiasNanos;
    private double mFullInterSignalBiasUncertaintyNanos;
    private double mSatelliteInterSignalBiasNanos;
    private double mSatelliteInterSignalBiasUncertaintyNanos;
    @Nullable private SatellitePvt mSatellitePvt;
    @Nullable private Collection<CorrelationVector> mReadOnlyCorrelationVectors;

    // The following enumerations must be in sync with the values declared in GNSS HAL.

    private static final int HAS_NO_FLAGS = 0;
    private static final int HAS_CODE_TYPE = (1 << 14);
    private static final int HAS_BASEBAND_CN0 = (1 << 15);
    private static final int HAS_SATELLITE_PVT = (1 << 20);
    private static final int HAS_CORRELATION_VECTOR = (1 << 21);

    /**
     * The status of the multipath indicator.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MULTIPATH_INDICATOR_UNKNOWN, MULTIPATH_INDICATOR_DETECTED,
            MULTIPATH_INDICATOR_NOT_DETECTED})
    public @interface MultipathIndicator {}

    /**
     * The indicator is not available or the presence or absence of multipath is unknown.
     */
    public static final int MULTIPATH_INDICATOR_UNKNOWN = 0;

    /**
     * The measurement shows signs of multi-path.
     */
    public static final int MULTIPATH_INDICATOR_DETECTED = 1;

    /**
     * The measurement shows no signs of multi-path.
     */
    public static final int MULTIPATH_INDICATOR_NOT_DETECTED = 2;

    /**
     * GNSS measurement tracking loop state
     * @hide
     */
    @IntDef(flag = true, prefix = { "STATE_" }, value = {
            STATE_CODE_LOCK, STATE_BIT_SYNC, STATE_SUBFRAME_SYNC,
            STATE_TOW_DECODED, STATE_MSEC_AMBIGUOUS, STATE_SYMBOL_SYNC, STATE_GLO_STRING_SYNC,
            STATE_GLO_TOD_DECODED, STATE_BDS_D2_BIT_SYNC, STATE_BDS_D2_SUBFRAME_SYNC,
            STATE_GAL_E1BC_CODE_LOCK, STATE_GAL_E1C_2ND_CODE_LOCK, STATE_GAL_E1B_PAGE_SYNC,
            STATE_SBAS_SYNC, STATE_TOW_KNOWN, STATE_GLO_TOD_KNOWN, STATE_2ND_CODE_LOCK
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}

    /** This GNSS measurement's tracking state is invalid or unknown. */
    public static final int STATE_UNKNOWN = 0;
    /** This GNSS measurement's tracking state has code lock. */
    public static final int STATE_CODE_LOCK = (1<<0);
    /** This GNSS measurement's tracking state has bit sync. */
    public static final int STATE_BIT_SYNC = (1<<1);
    /** This GNSS measurement's tracking state has sub-frame sync. */
    public static final int STATE_SUBFRAME_SYNC = (1<<2);
    /** This GNSS measurement's tracking state has time-of-week decoded. */
    public static final int STATE_TOW_DECODED = (1<<3);
    /** This GNSS measurement's tracking state contains millisecond ambiguity. */
    public static final int STATE_MSEC_AMBIGUOUS = (1<<4);
    /** This GNSS measurement's tracking state has symbol sync. */
    public static final int STATE_SYMBOL_SYNC = (1<<5);
    /** This Glonass measurement's tracking state has string sync. */
    public static final int STATE_GLO_STRING_SYNC = (1<<6);
    /** This Glonass measurement's tracking state has time-of-day decoded. */
    public static final int STATE_GLO_TOD_DECODED = (1<<7);
    /** This Beidou measurement's tracking state has D2 bit sync. */
    public static final int STATE_BDS_D2_BIT_SYNC = (1<<8);
    /** This Beidou measurement's tracking state has D2 sub-frame sync. */
    public static final int STATE_BDS_D2_SUBFRAME_SYNC = (1<<9);
    /** This Galileo measurement's tracking state has E1B/C code lock. */
    public static final int STATE_GAL_E1BC_CODE_LOCK = (1<<10);
    /** This Galileo measurement's tracking state has E1C secondary code lock. */
    public static final int STATE_GAL_E1C_2ND_CODE_LOCK = (1<<11);
    /** This Galileo measurement's tracking state has E1B page sync. */
    public static final int STATE_GAL_E1B_PAGE_SYNC = (1<<12);
    /** This SBAS measurement's tracking state has whole second level sync. */
    public static final int STATE_SBAS_SYNC = (1<<13);
    /**
     * This GNSS measurement's tracking state has time-of-week known, possibly not decoded
     * over the air but has been determined from other sources. If TOW decoded is set then TOW Known
     * will also be set.
     */
    public static final int STATE_TOW_KNOWN = (1<<14);
    /**
     * This Glonass measurement's tracking state has time-of-day known, possibly not decoded
     * over the air but has been determined from other sources. If TOD decoded is set then TOD Known
     * will also be set.
     */
    public static final int STATE_GLO_TOD_KNOWN = (1<<15);

    /** This GNSS measurement's tracking state has secondary code lock. */
    public static final int STATE_2ND_CODE_LOCK  = (1 << 16);

    /**
     * All the GNSS receiver state flags, for bit masking purposes (not a sensible state for any
     * individual measurement.)
     */
    private static final int STATE_ALL = 0x3fff;  // 2 bits + 4 bits + 4 bits + 4 bits = 14 bits

    /**
     * GNSS measurement accumulated delta range state
     * @hide
     */
    @IntDef(flag = true, prefix = { "ADR_STATE_" }, value = {
            ADR_STATE_UNKNOWN, ADR_STATE_VALID, ADR_STATE_RESET, ADR_STATE_CYCLE_SLIP,
            ADR_STATE_HALF_CYCLE_RESOLVED, ADR_STATE_HALF_CYCLE_REPORTED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdrState {}

    /**
     * The state of the value {@link #getAccumulatedDeltaRangeMeters()} is invalid or unknown.
     */
    public static final int ADR_STATE_UNKNOWN = 0;

    /**
     * The state of the {@link #getAccumulatedDeltaRangeMeters()} is valid.
     */
    public static final int ADR_STATE_VALID = (1<<0);

    /**
     * The state of the {@link #getAccumulatedDeltaRangeMeters()} has detected a reset.
     */
    public static final int ADR_STATE_RESET = (1<<1);

    /**
     * The state of the {@link #getAccumulatedDeltaRangeMeters()} has a cycle slip detected.
     */
    public static final int ADR_STATE_CYCLE_SLIP = (1<<2);

    /**
     * Reports whether the value {@link #getAccumulatedDeltaRangeMeters()} has resolved the half
     * cycle ambiguity.
     *
     * <p> When this bit is set, the {@link #getAccumulatedDeltaRangeMeters()} corresponds to the
     * carrier phase measurement plus an accumulated integer number of carrier full cycles.
     *
     * <p> When this bit is unset, the {@link #getAccumulatedDeltaRangeMeters()} corresponds to the
     * carrier phase measurement plus an accumulated integer number of carrier half cycles.
     *
     * <p> For signals that have databits, the carrier phase tracking loops typically use a costas
     * loop discriminator.  This type of tracking loop introduces a half-cycle ambiguity that is
     * resolved by searching through the received data for known patterns of databits (e.g. GPS uses
     * the TLM word) which then determines the polarity of the incoming data and resolves the
     * half-cycle ambiguity.
     *
     * <p>Before the half-cycle ambiguity has been resolved it is possible that the ADR_STATE_VALID
     * flag is set:
     *
     * <ul>
     *   <li> In cases where ADR_STATE_HALF_CYCLE_REPORTED is not set, the
     *   ADR_STATE_HALF_CYCLE_RESOLVED flag will not be available. Here, a half wave length will be
     *   added to the returned accumulated delta range uncertainty to indicate the half cycle
     *   ambiguity.
     *   <li> In cases where ADR_STATE_HALF_CYCLE_REPORTED is set, half cycle ambiguity will be
     *   indicated via both the ADR_STATE_HALF_CYCLE_RESOLVED flag and as well a half wave length
     *   added to the returned accumulated delta range uncertainty.
     * </ul>
     */
    public static final int ADR_STATE_HALF_CYCLE_RESOLVED = (1<<3);

    /**
     * Reports whether the flag {@link #ADR_STATE_HALF_CYCLE_RESOLVED} has been reported by the
     * GNSS hardware.
     *
     * <p> When this bit is set, the value of {@link #getAccumulatedDeltaRangeUncertaintyMeters()}
     * can be low (centimeter level) whether or not the half cycle ambiguity is resolved.
     *
     * <p> When this bit is unset, the value of {@link #getAccumulatedDeltaRangeUncertaintyMeters()}
     * is larger, to cover the potential error due to half cycle ambiguity being unresolved.
     */
    public static final int ADR_STATE_HALF_CYCLE_REPORTED = (1<<4);

    /**
     * All the 'Accumulated Delta Range' flags.
     * @hide
     */
    @TestApi
    public static final int ADR_STATE_ALL =
            ADR_STATE_VALID | ADR_STATE_RESET | ADR_STATE_CYCLE_SLIP |
            ADR_STATE_HALF_CYCLE_RESOLVED | ADR_STATE_HALF_CYCLE_REPORTED;

    // End enumerations in sync with gps.h

    /**
     * @hide
     */
    @TestApi
    public GnssMeasurement() {
        initialize();
    }

    /**
     * Sets all contents to the values stored in the provided object.
     * @hide
     */
    @TestApi
    public void set(GnssMeasurement measurement) {
        mFlags = measurement.mFlags;
        mSvid = measurement.mSvid;
        mConstellationType = measurement.mConstellationType;
        mTimeOffsetNanos = measurement.mTimeOffsetNanos;
        mState = measurement.mState;
        mReceivedSvTimeNanos = measurement.mReceivedSvTimeNanos;
        mReceivedSvTimeUncertaintyNanos = measurement.mReceivedSvTimeUncertaintyNanos;
        mCn0DbHz = measurement.mCn0DbHz;
        mBasebandCn0DbHz = measurement.mBasebandCn0DbHz;
        mPseudorangeRateMetersPerSecond = measurement.mPseudorangeRateMetersPerSecond;
        mPseudorangeRateUncertaintyMetersPerSecond =
                measurement.mPseudorangeRateUncertaintyMetersPerSecond;
        mAccumulatedDeltaRangeState = measurement.mAccumulatedDeltaRangeState;
        mAccumulatedDeltaRangeMeters = measurement.mAccumulatedDeltaRangeMeters;
        mAccumulatedDeltaRangeUncertaintyMeters =
                measurement.mAccumulatedDeltaRangeUncertaintyMeters;
        mCarrierFrequencyHz = measurement.mCarrierFrequencyHz;
        mCarrierCycles = measurement.mCarrierCycles;
        mCarrierPhase = measurement.mCarrierPhase;
        mCarrierPhaseUncertainty = measurement.mCarrierPhaseUncertainty;
        mMultipathIndicator = measurement.mMultipathIndicator;
        mSnrInDb = measurement.mSnrInDb;
        mAutomaticGainControlLevelInDb = measurement.mAutomaticGainControlLevelInDb;
        mCodeType = measurement.mCodeType;
        mFullInterSignalBiasNanos = measurement.mFullInterSignalBiasNanos;
        mFullInterSignalBiasUncertaintyNanos =
                measurement.mFullInterSignalBiasUncertaintyNanos;
        mSatelliteInterSignalBiasNanos = measurement.mSatelliteInterSignalBiasNanos;
        mSatelliteInterSignalBiasUncertaintyNanos =
                measurement.mSatelliteInterSignalBiasUncertaintyNanos;
        mSatellitePvt = measurement.mSatellitePvt;
        mReadOnlyCorrelationVectors = measurement.mReadOnlyCorrelationVectors;
    }

    /**
     * Resets all the contents to its original state.
     * @hide
     */
    @TestApi
    public void reset() {
        initialize();
    }

    /**
     * Gets the satellite ID.
     *
     * <p>Interpretation depends on {@link #getConstellationType()}.
     * See {@link GnssStatus#getSvid(int)}.
     */
    public int getSvid() {
        return mSvid;
    }

    /**
     * Sets the Satellite ID.
     * @hide
     */
    @TestApi
    public void setSvid(int value) {
        mSvid = value;
    }

    /**
     * Gets the constellation type.
     *
     * <p>The return value is one of those constants with {@code CONSTELLATION_} prefix in
     * {@link GnssStatus}.
     */
    @GnssStatus.ConstellationType
    public int getConstellationType() {
        return mConstellationType;
    }

    /**
     * Sets the constellation type.
     * @hide
     */
    @TestApi
    public void setConstellationType(@GnssStatus.ConstellationType int value) {
        mConstellationType = value;
    }

    /**
     * Gets the time offset at which the measurement was taken in nanoseconds.
     *
     * <p>The reference receiver's time from which this is offset is specified by
     * {@link GnssClock#getTimeNanos()}.
     *
     * <p>The sign of this value is given by the following equation:
     * <pre>
     *      measurement time = TimeNanos + TimeOffsetNanos</pre>
     *
     * <p>The value provides an individual time-stamp for the measurement, and allows sub-nanosecond
     * accuracy.
     */
    public double getTimeOffsetNanos() {
        return mTimeOffsetNanos;
    }

    /**
     * Sets the time offset at which the measurement was taken in nanoseconds.
     * @hide
     */
    @TestApi
    public void setTimeOffsetNanos(double value) {
        mTimeOffsetNanos = value;
    }

    /**
     * Gets per-satellite-signal sync state.
     *
     * <p>It represents the current sync state for the associated satellite signal.
     *
     * <p>This value helps interpret {@link #getReceivedSvTimeNanos()}.
     */
    @State
    public int getState() {
        return mState;
    }

    /**
     * Sets the sync state.
     * @hide
     */
    @TestApi
    public void setState(@State int value) {
        mState = value;
    }

    /**
     * Gets a string representation of the 'sync state'.
     *
     * <p>For internal and logging use only.
     */
    private String getStateString() {
        if (mState == STATE_UNKNOWN) {
            return "Unknown";
        }

        StringBuilder builder = new StringBuilder();
        if ((mState & STATE_CODE_LOCK) != 0) {
            builder.append("CodeLock|");
        }
        if ((mState & STATE_BIT_SYNC) != 0) {
            builder.append("BitSync|");
        }
        if ((mState & STATE_SUBFRAME_SYNC) != 0) {
            builder.append("SubframeSync|");
        }
        if ((mState & STATE_TOW_DECODED) != 0) {
            builder.append("TowDecoded|");
        }
        if ((mState & STATE_TOW_KNOWN) != 0) {
          builder.append("TowKnown|");
        }
        if ((mState & STATE_MSEC_AMBIGUOUS) != 0) {
            builder.append("MsecAmbiguous|");
        }
        if ((mState & STATE_SYMBOL_SYNC) != 0) {
            builder.append("SymbolSync|");
        }
        if ((mState & STATE_GLO_STRING_SYNC) != 0) {
            builder.append("GloStringSync|");
        }
        if ((mState & STATE_GLO_TOD_DECODED) != 0) {
            builder.append("GloTodDecoded|");
        }
        if ((mState & STATE_GLO_TOD_KNOWN) != 0) {
          builder.append("GloTodKnown|");
        }
        if ((mState & STATE_BDS_D2_BIT_SYNC) != 0) {
            builder.append("BdsD2BitSync|");
        }
        if ((mState & STATE_BDS_D2_SUBFRAME_SYNC) != 0) {
            builder.append("BdsD2SubframeSync|");
        }
        if ((mState & STATE_GAL_E1BC_CODE_LOCK) != 0) {
            builder.append("GalE1bcCodeLock|");
        }
        if ((mState & STATE_GAL_E1C_2ND_CODE_LOCK) != 0) {
            builder.append("E1c2ndCodeLock|");
        }
        if ((mState & STATE_GAL_E1B_PAGE_SYNC) != 0) {
            builder.append("GalE1bPageSync|");
        }
        if ((mState & STATE_SBAS_SYNC) != 0) {
            builder.append("SbasSync|");
        }
        if ((mState & STATE_2ND_CODE_LOCK) != 0) {
            builder.append("2ndCodeLock|");
        }

        int remainingStates = mState & ~STATE_ALL;
        if (remainingStates > 0) {
            builder.append("Other(");
            builder.append(Integer.toBinaryString(remainingStates));
            builder.append(")|");
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    /**
     * Gets the received GNSS satellite time, at the measurement time, in nanoseconds.
     *
     * <p>The received satellite time is relative to the beginning of the system week for all
     * constellations except for Glonass where it is relative to the beginning of the Glonass
     * system day.
     *
     * <p>The table below indicates the valid range of the received GNSS satellite time. These
     * ranges depend on the constellation and code being tracked and the state of the tracking
     * algorithms given by the {@link #getState} method. The minimum value of this field is zero.
     * The maximum value of this field is determined by looking across all of the state flags
     * that are set, for the given constellation and code type, and finding the the maximum value
     * in this table.
     *
     * <p>For example, for GPS L1 C/A, if STATE_TOW_KNOWN is set, this field can be any value from 0
     * to 1 week (in nanoseconds), and for GAL E1B code, if only STATE_GAL_E1BC_CODE_LOCK is set,
     * then this field can be any value from 0 to 4 milliseconds (in nanoseconds.)
     *
     * <table border="1">
     *   <thead>
     *     <tr>
     *       <td />
     *       <td colspan="4"><strong>GPS/QZSS</strong></td>
     *       <td><strong>GLNS</strong></td>
     *       <td colspan="4"><strong>BDS</strong></td>
     *       <td colspan="3"><strong>GAL</strong></td>
     *       <td><strong>SBAS</strong></td>
     *       <td><strong>NavIC</strong></td>
     *     </tr>
     *     <tr>
     *       <td><strong>State Flag</strong></td>
     *       <td><strong>L1 C/A</strong></td>
     *       <td><strong>L1 C(P)</strong></td>
     *       <td><strong>L5I</strong></td>
     *       <td><strong>L5Q</strong></td>
     *       <td><strong>L1OF</strong></td>
     *       <td><strong>B1I (D1)</strong></td>
     *       <td><strong>B1I (D2)</strong></td>
     *       <td><strong>B1C (P)</strong></td>
     *       <td><strong>B2AQ </strong></td>
     *       <td><strong>E1B</strong></td>
     *       <td><strong>E1C</strong></td>
     *       <td><strong>E5AQ</strong></td>
     *       <td><strong>L1 C/A</strong></td>
     *       <td><strong>L5C</strong></td>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *       <td>
     *         <strong>STATE_UNKNOWN</strong>
     *       </td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *       <td>0</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_CODE_LOCK</strong>
     *       </td>
     *       <td>1 ms</td>
     *       <td>10 ms</td>
     *       <td>1 ms</td>
     *       <td>1 ms</td>
     *       <td>1 ms</td>
     *       <td>1 ms</td>
     *       <td>1 ms</td>
     *       <td>10 ms</td>
     *       <td>1 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>1 ms</td>
     *       <td>1 ms</td>
     *       <td>1 ms</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_SYMBOL_SYNC</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>10 ms</td>
     *       <td>-</td>
     *       <td>10 ms</td>
     *       <td>-</td>
     *       <td>2 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>2 ms</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_BIT_SYNC</strong>
     *       </td>
     *       <td>20 ms</td>
     *       <td>-</td>
     *       <td>20 ms</td>
     *       <td>-</td>
     *       <td>20 ms</td>
     *       <td>20 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>8 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>4 ms</td>
     *       <td>20 ms</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_SUBFRAME_SYNC</strong>
     *       </td>
     *       <td>6 s</td>
     *       <td>-</td>
     *       <td>6 s</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>6 s</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>100 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>100 ms</td>
     *       <td>-</td>
     *       <td>6 s</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_TOW_DECODED</strong>
     *       </td>
     *       <td>1 week</td>
     *       <td>-</td>
     *       <td>1 week</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>-</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_TOW_KNOWN</strong>
     *       </td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>-</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *       <td>1 week</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_GLO_STRING_SYNC</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>2 s</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_GLO_TOD_DECODED</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>1 day</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_GLO_TOD_KNOWN</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>1 day</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_BDS_D2_BIT_SYNC</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>2 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_BDS_D2_SUBFRAME_SYNC</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>600 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_GAL_E1BC_CODE_LOCK</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>4 ms</td>
     *       <td>4 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_GAL_E1C_2ND_CODE_LOCK</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>100 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_2ND_CODE_LOCK</strong>
     *       </td>
     *       <td>-</td>
     *       <td>18000 ms</td>
     *       <td>10 ms</td>
     *       <td>20 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>18000 ms</td>
     *       <td>100 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>100 ms</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_GAL_E1B_PAGE_SYNC</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>2 s</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *     </tr>
     *     <tr>
     *       <td>
     *         <strong>STATE_SBAS_SYNC</strong>
     *       </td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>-</td>
     *       <td>1 s</td>
     *       <td>-</td>
     *     </tr>
     *   </tbody>
     * </table>
     *
     * <p>Note: TOW Known refers to the case where TOW is possibly not decoded over the air but has
     * been determined from other sources. If TOW decoded is set then TOW Known must also be set.
     *
     * <p>Note well: if there is any ambiguity in integer millisecond, STATE_MSEC_AMBIGUOUS must be
     * set accordingly, in the 'state' field. This value must be populated, unless the 'state' ==
     * STATE_UNKNOWN.
     *
     * <p>Note on optional flags:
     * <ul>
     *     <li> For L1 C/A and B1I, STATE_SYMBOL_SYNC is optional since the symbol length is the
     *     same as the bit length.
     *     <li> For L5Q and E5aQ, STATE_BIT_SYNC and STATE_SYMBOL_SYNC are optional since they are
     *     implied by STATE_CODE_LOCK.
     *     <li> STATE_2ND_CODE_LOCK for L5I is optional since it is implied by STATE_SYMBOL_SYNC.
     *     <li> STATE_2ND_CODE_LOCK for E1C is optional since it is implied by
     *     STATE_GAL_E1C_2ND_CODE_LOCK.
     *     <li> For E1B and E1C, STATE_SYMBOL_SYNC is optional, because it is implied by
     *     STATE_GAL_E1BC_CODE_LOCK.
     * </ul>
     */
    public long getReceivedSvTimeNanos() {
        return mReceivedSvTimeNanos;
    }

    /**
     * Sets the received GNSS time in nanoseconds.
     * @hide
     */
    @TestApi
    public void setReceivedSvTimeNanos(long value) {
        mReceivedSvTimeNanos = value;
    }

    /**
     * Gets the error estimate (1-sigma) for the received GNSS time, in nanoseconds.
     */
    public long getReceivedSvTimeUncertaintyNanos() {
        return mReceivedSvTimeUncertaintyNanos;
    }

    /**
     * Sets the received GNSS time uncertainty (1-Sigma) in nanoseconds.
     * @hide
     */
    @TestApi
    public void setReceivedSvTimeUncertaintyNanos(long value) {
        mReceivedSvTimeUncertaintyNanos = value;
    }

    /**
     * Gets the Carrier-to-noise density in dB-Hz.
     *
     * <p>Typical range: 10-50 dB-Hz. The range of possible C/N0 values is 0-63 dB-Hz to handle
     * some edge cases.
     *
     * <p>The value contains the measured C/N0 for the signal at the antenna input.
     */
    @FloatRange(from = 0, to = 63)
    public double getCn0DbHz() {
        return mCn0DbHz;
    }

    /**
     * Sets the carrier-to-noise density in dB-Hz.
     * @hide
     */
    @TestApi
    public void setCn0DbHz(double value) {
        mCn0DbHz = value;
    }

    /**
     * Returns {@code true} if {@link #getBasebandCn0DbHz()} is available, {@code false} otherwise.
     */
    public boolean hasBasebandCn0DbHz() {
        return isFlagSet(HAS_BASEBAND_CN0);
    }

    /**
     * Gets the baseband carrier-to-noise density in dB-Hz.
     *
     * <p>Typical range: 10-50 dB-Hz. The range of possible baseband C/N0 values is 0-63 dB-Hz to
     * handle some edge cases.
     *
     * <p>The value contains the measured C/N0 for the signal at the baseband. This is typically
     * a few dB weaker than the value estimated for C/N0 at the antenna port, which is reported
     * in {@link #getCn0DbHz()}.
     */
    @FloatRange(from = 0, to = 63)
    public double getBasebandCn0DbHz() {
        return mBasebandCn0DbHz;
    }

    /**
     * Sets the baseband carrier-to-noise density in dB-Hz.
     *
     * @hide
     */
    @TestApi
    public void setBasebandCn0DbHz(double value) {
        setFlag(HAS_BASEBAND_CN0);
        mBasebandCn0DbHz = value;
    }

    /**
     * Resets the baseband carrier-to-noise density in dB-Hz.
     *
     * @hide
     */
    @TestApi
    public void resetBasebandCn0DbHz() {
        resetFlag(HAS_BASEBAND_CN0);
    }

    /**
     * Gets the Pseudorange rate at the timestamp in m/s.
     *
     * <p>The error estimate for this value is
     * {@link #getPseudorangeRateUncertaintyMetersPerSecond()}.
     *
     * <p>The value is uncorrected, i.e. corrections for receiver and satellite clock frequency
     * errors are not included.
     *
     * <p>A positive 'uncorrected' value indicates that the SV is moving away from the receiver. The
     * sign of the 'uncorrected' 'pseudorange rate' and its relation to the sign of 'doppler shift'
     * is given by the equation:
     *
     * <pre>
     *      pseudorange rate = -k * doppler shift   (where k is a constant)</pre>
     */
    public double getPseudorangeRateMetersPerSecond() {
        return mPseudorangeRateMetersPerSecond;
    }

    /**
     * Sets the pseudorange rate at the timestamp in m/s.
     * @hide
     */
    @TestApi
    public void setPseudorangeRateMetersPerSecond(double value) {
        mPseudorangeRateMetersPerSecond = value;
    }

    /**
     * Gets the pseudorange's rate uncertainty (1-Sigma) in m/s.
     *
     * <p>The uncertainty is represented as an absolute (single sided) value.
     */
    public double getPseudorangeRateUncertaintyMetersPerSecond() {
        return mPseudorangeRateUncertaintyMetersPerSecond;
    }

    /**
     * Sets the pseudorange's rate uncertainty (1-Sigma) in m/s.
     * @hide
     */
    @TestApi
    public void setPseudorangeRateUncertaintyMetersPerSecond(double value) {
        mPseudorangeRateUncertaintyMetersPerSecond = value;
    }

    /**
     * Gets 'Accumulated Delta Range' state.
     *
     * <p>This indicates the state of the {@link #getAccumulatedDeltaRangeMeters()} measurement. See
     * the table below for a detailed interpretation of each state.
     *
     * <table border="1">
     * <thead>
     * <tr>
     * <th>ADR_STATE</th>
     * <th>Time of relevance</th>
     * <th>Interpretation</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>UNKNOWN</td>
     * <td>ADR(t)</td>
     * <td>No valid carrier phase information is available at time t.</td>
     * </tr>
     * <tr>
     * <td>VALID</td>
     * <td>ADR(t)</td>
     * <td>Valid carrier phase information is available at time t. This indicates that this
     * measurement can be used as a reference for future measurements. However, to compare it to
     * previous measurements to compute delta range, other bits should be checked. Specifically,
     * it can be used for delta range computation if it is valid and has no reset or cycle  slip at
     * this epoch i.e. if VALID_BIT == 1 && CYCLE_SLIP_BIT == 0 && RESET_BIT == 0.</td>
     * </tr>
     * <tr>
     * <td>RESET</td>
     * <td>ADR(t) - ADR(t-1)</td>
     * <td>Carrier phase accumulation has been restarted between current time t and previous time
     * t-1. This indicates that this measurement can be used as a reference for future measurements,
     * but it should not be compared to previous measurements to compute delta range.</td>
     * </tr>
     * <tr>
     * <td>CYCLE_SLIP</td>
     * <td>ADR(t) - ADR(t-1)</td>
     * <td>Cycle slip(s) have been detected between the current time t and previous time t-1. This
     * indicates that this measurement can be used as a reference for future measurements. Clients
     * can use a measurement with a cycle slip to compute delta range against previous measurements
     * at their own risk.</td>
     * </tr>
     * <tr>
     * <td>HALF_CYCLE_RESOLVED</td>
     * <td>ADR(t)</td>
     * <td>Half cycle ambiguity is resolved at time t.</td>
     * </tr>
     * <tr>
     * <td>HALF_CYCLE_REPORTED</td>
     * <td>ADR(t)</td>
     * <td>Half cycle ambiguity is reported at time t.</td>
     * </tr>
     * </tbody>
     * </table>
     */
    @AdrState
    public int getAccumulatedDeltaRangeState() {
        return mAccumulatedDeltaRangeState;
    }

    /**
     * Sets the 'Accumulated Delta Range' state.
     * @hide
     */
    @TestApi
    public void setAccumulatedDeltaRangeState(@AdrState int value) {
        mAccumulatedDeltaRangeState = value;
    }

    /**
     * Gets a string representation of the 'Accumulated Delta Range state'.
     *
     * <p>For internal and logging use only.
     */
    private String getAccumulatedDeltaRangeStateString() {
        if (mAccumulatedDeltaRangeState == ADR_STATE_UNKNOWN) {
            return "Unknown";
        }
        StringBuilder builder = new StringBuilder();
        if ((mAccumulatedDeltaRangeState & ADR_STATE_VALID) == ADR_STATE_VALID) {
            builder.append("Valid|");
        }
        if ((mAccumulatedDeltaRangeState & ADR_STATE_RESET) == ADR_STATE_RESET) {
            builder.append("Reset|");
        }
        if ((mAccumulatedDeltaRangeState & ADR_STATE_CYCLE_SLIP) == ADR_STATE_CYCLE_SLIP) {
            builder.append("CycleSlip|");
        }
        if ((mAccumulatedDeltaRangeState & ADR_STATE_HALF_CYCLE_RESOLVED) ==
                ADR_STATE_HALF_CYCLE_RESOLVED) {
            builder.append("HalfCycleResolved|");
        }
        if ((mAccumulatedDeltaRangeState & ADR_STATE_HALF_CYCLE_REPORTED)
                == ADR_STATE_HALF_CYCLE_REPORTED) {
            builder.append("HalfCycleReported|");
        }
        int remainingStates = mAccumulatedDeltaRangeState & ~ADR_STATE_ALL;
        if (remainingStates > 0) {
            builder.append("Other(");
            builder.append(Integer.toBinaryString(remainingStates));
            builder.append(")|");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    /**
     * Gets the accumulated delta range since the last channel reset, in meters.
     *
     * <p>The error estimate for this value is {@link #getAccumulatedDeltaRangeUncertaintyMeters()}.
     *
     * <p>The availability of the value is represented by {@link #getAccumulatedDeltaRangeState()}.
     *
     * <p>A positive value indicates that the SV is moving away from the receiver.
     * The sign of {@link #getAccumulatedDeltaRangeMeters()} and its relation to the sign of
     * {@link #getCarrierPhase()} is given by the equation:
     *
     * <pre>
     *          accumulated delta range = -k * carrier phase    (where k is a constant)</pre>
     *
     * <p>Similar to the concept of an RTCM "Phaserange", when the accumulated delta range is
     * initially chosen, and whenever it is reset, it will retain the integer nature
     * of the relative carrier phase offset between satellites observed by this receiver, such that
     * the double difference of this value between receivers and satellites may be used, together
     * with integer ambiguity resolution, to determine highly precise relative location between
     * receivers.
     *
     * <p>The alignment of the phase measurement will not be adjusted by the receiver so the
     * in-phase and quadrature phase components will have a quarter cycle offset as they do when
     * transmitted from the satellites. If the measurement is from a combination of the in-phase
     * and quadrature phase components, then the alignment of the phase measurement will be aligned
     * to the in-phase component.
     */
    public double getAccumulatedDeltaRangeMeters() {
        return mAccumulatedDeltaRangeMeters;
    }

    /**
     * Sets the accumulated delta range in meters.
     * @hide
     */
    @TestApi
    public void setAccumulatedDeltaRangeMeters(double value) {
        mAccumulatedDeltaRangeMeters = value;
    }

    /**
     * Gets the accumulated delta range's uncertainty (1-Sigma) in meters.
     *
     * <p>The uncertainty is represented as an absolute (single sided) value.
     *
     * <p>The status of the value is represented by {@link #getAccumulatedDeltaRangeState()}.
     */
    public double getAccumulatedDeltaRangeUncertaintyMeters() {
        return mAccumulatedDeltaRangeUncertaintyMeters;
    }

    /**
     * Sets the accumulated delta range's uncertainty (1-sigma) in meters.
     *
     * <p>The status of the value is represented by {@link #getAccumulatedDeltaRangeState()}.
     *
     * @hide
     */
    @TestApi
    public void setAccumulatedDeltaRangeUncertaintyMeters(double value) {
        mAccumulatedDeltaRangeUncertaintyMeters = value;
    }

    /**
     * Returns {@code true} if {@link #getCarrierFrequencyHz()} is available, {@code false}
     * otherwise.
     */
    public boolean hasCarrierFrequencyHz() {
        return isFlagSet(HAS_CARRIER_FREQUENCY);
    }

    /**
     * Gets the carrier frequency of the tracked signal.
     *
     * <p>For example it can be the GPS central frequency for L1 = 1575.45 MHz, or L2 = 1227.60 MHz,
     * L5 = 1176.45 MHz, varying GLO channels, etc.
     *
     * <p>The value is only available if {@link #hasCarrierFrequencyHz()} is {@code true}.
     *
     * @return the carrier frequency of the signal tracked in Hz.
     */
    public float getCarrierFrequencyHz() {
        return mCarrierFrequencyHz;
    }

    /**
     * Sets the Carrier frequency in Hz.
     * @hide
     */
    @TestApi
    public void setCarrierFrequencyHz(float carrierFrequencyHz) {
        setFlag(HAS_CARRIER_FREQUENCY);
        mCarrierFrequencyHz = carrierFrequencyHz;
    }

    /**
     * Resets the Carrier frequency in Hz.
     * @hide
     */
    @TestApi
    public void resetCarrierFrequencyHz() {
        resetFlag(HAS_CARRIER_FREQUENCY);
        mCarrierFrequencyHz = Float.NaN;
    }

    /**
     * Returns {@code true} if {@link #getCarrierCycles()} is available, {@code false} otherwise.
     * 
     * @deprecated use {@link #getAccumulatedDeltaRangeState()} instead.
     */
    @Deprecated
    public boolean hasCarrierCycles() {
        return isFlagSet(HAS_CARRIER_CYCLES);
    }

    /**
     * The number of full carrier cycles between the satellite and the receiver.
     *
     * <p>The reference frequency is given by the value of {@link #getCarrierFrequencyHz()}.
     *
     * <p>The value is only available if {@link #hasCarrierCycles()} is {@code true}.
     *
     * @deprecated use {@link #getAccumulatedDeltaRangeMeters()} instead.
     */
    @Deprecated
    public long getCarrierCycles() {
        return mCarrierCycles;
    }

    /**
     * Sets the number of full carrier cycles between the satellite and the receiver.
     *
     * @deprecated use {@link #setAccumulatedDeltaRangeMeters(double)}
     * and {@link #setAccumulatedDeltaRangeState(int)} instead.
     * 
     * @hide
     */
    @TestApi
    @Deprecated
    public void setCarrierCycles(long value) {
        setFlag(HAS_CARRIER_CYCLES);
        mCarrierCycles = value;
    }

    /**
     * Resets the number of full carrier cycles between the satellite and the receiver.
     * 
     * @deprecated use {@link #setAccumulatedDeltaRangeMeters(double)}
     * and {@link #setAccumulatedDeltaRangeState(int)} instead.
     * @hide
     */
    @TestApi
    @Deprecated
    public void resetCarrierCycles() {
        resetFlag(HAS_CARRIER_CYCLES);
        mCarrierCycles = Long.MIN_VALUE;
    }

    /**
     * Returns {@code true} if {@link #getCarrierPhase()} is available, {@code false} otherwise.
     * 
     * @deprecated use {@link #getAccumulatedDeltaRangeState()} instead.
     */
    @Deprecated
    public boolean hasCarrierPhase() {
        return isFlagSet(HAS_CARRIER_PHASE);
    }

    /**
     * Gets the RF phase detected by the receiver.
     *
     * <p>Range: [0.0, 1.0].
     *
     * <p>This is the fractional part of the complete carrier phase measurement.
     *
     * <p>The reference frequency is given by the value of {@link #getCarrierFrequencyHz()}.
     *
     * <p>The error estimate for this value is {@link #getCarrierPhaseUncertainty()}.
     *
     * <p>The value is only available if {@link #hasCarrierPhase()} is {@code true}.
     *
     * @deprecated use {@link #getAccumulatedDeltaRangeMeters()} instead.
     */
    @Deprecated
    public double getCarrierPhase() {
        return mCarrierPhase;
    }

    /**
     * Sets the RF phase detected by the receiver.
     * 
     * @deprecated use {@link #setAccumulatedDeltaRangeMeters(double)}
     * and {@link #setAccumulatedDeltaRangeState(int)} instead.
     * 
     * @hide
     */
    @TestApi
    @Deprecated
    public void setCarrierPhase(double value) {
        setFlag(HAS_CARRIER_PHASE);
        mCarrierPhase = value;
    }

    /**
     * Resets the RF phase detected by the receiver.
     * 
     * @deprecated use {@link #setAccumulatedDeltaRangeMeters(double)}
     * and {@link #setAccumulatedDeltaRangeState(int)} instead.
     * 
     * @hide
     */
    @TestApi
    @Deprecated
    public void resetCarrierPhase() {
        resetFlag(HAS_CARRIER_PHASE);
    }

    /**
     * Returns {@code true} if {@link #getCarrierPhaseUncertainty()} is available, {@code false}
     * otherwise.
     * 
     * @deprecated use {@link #getAccumulatedDeltaRangeState()} instead.
     */
    @Deprecated
    public boolean hasCarrierPhaseUncertainty() {
        return isFlagSet(HAS_CARRIER_PHASE_UNCERTAINTY);
    }

    /**
     * Gets the carrier-phase's uncertainty (1-Sigma).
     *
     * <p>The uncertainty is represented as an absolute (single sided) value.
     *
     * <p>The value is only available if {@link #hasCarrierPhaseUncertainty()} is {@code true}.
     *
     * @deprecated use {@link #getAccumulatedDeltaRangeUncertaintyMeters()} instead.
     */
    @Deprecated
    public double getCarrierPhaseUncertainty() {
        return mCarrierPhaseUncertainty;
    }

    /**
     * Sets the Carrier-phase's uncertainty (1-Sigma) in cycles.
     * 
     * @deprecated use {@link #setAccumulatedDeltaRangeUncertaintyMeters(double)}
     * and {@link #setAccumulatedDeltaRangeState(int)} instead.
     * 
     * @hide
     */
    @TestApi
    @Deprecated
    public void setCarrierPhaseUncertainty(double value) {
        setFlag(HAS_CARRIER_PHASE_UNCERTAINTY);
        mCarrierPhaseUncertainty = value;
    }

    /**
     * Resets the Carrier-phase's uncertainty (1-Sigma) in cycles.
     * 
     * @deprecated use {@link #setAccumulatedDeltaRangeUncertaintyMeters(double)}
     * and {@link #setAccumulatedDeltaRangeState(int)} instead.
     * 
     * @hide
     */
    @TestApi
    @Deprecated
    public void resetCarrierPhaseUncertainty() {
        resetFlag(HAS_CARRIER_PHASE_UNCERTAINTY);
    }

    /**
     * Gets a value indicating the 'multipath' state of the event.
     */
    @MultipathIndicator
    public int getMultipathIndicator() {
        return mMultipathIndicator;
    }

    /**
     * Sets the 'multi-path' indicator.
     * @hide
     */
    @TestApi
    public void setMultipathIndicator(@MultipathIndicator int value) {
        mMultipathIndicator = value;
    }

    /**
     * Gets a string representation of the 'multi-path indicator'.
     *
     * <p>For internal and logging use only.
     */
    private String getMultipathIndicatorString() {
        switch (mMultipathIndicator) {
            case MULTIPATH_INDICATOR_UNKNOWN:
                return "Unknown";
            case MULTIPATH_INDICATOR_DETECTED:
                return "Detected";
            case MULTIPATH_INDICATOR_NOT_DETECTED:
                return "NotDetected";
            default:
                return "<Invalid: " + mMultipathIndicator + ">";
        }
    }

    /**
     * Returns {@code true} if {@link #getSnrInDb()} is available, {@code false} otherwise.
     */
    public boolean hasSnrInDb() {
        return isFlagSet(HAS_SNR);
    }

    /**
     * Gets the (post-correlation & integration) Signal-to-Noise ratio (SNR) in dB.
     *
     * <p>The value is only available if {@link #hasSnrInDb()} is {@code true}.
     */
    public double getSnrInDb() {
        return mSnrInDb;
    }

    /**
     * Sets the Signal-to-noise ratio (SNR) in dB.
     * @hide
     */
    @TestApi
    public void setSnrInDb(double snrInDb) {
        setFlag(HAS_SNR);
        mSnrInDb = snrInDb;
    }

    /**
     * Resets the Signal-to-noise ratio (SNR) in dB.
     * @hide
     */
    @TestApi
    public void resetSnrInDb() {
        resetFlag(HAS_SNR);
    }

    /**
     * Returns {@code true} if {@link #getAutomaticGainControlLevelDb()} is available,
     * {@code false} otherwise.
     *
     * @deprecated Use {@link GnssMeasurementsEvent#getGnssAutomaticGainControls()} instead.
     */
    @Deprecated
    public boolean hasAutomaticGainControlLevelDb() {
        return isFlagSet(HAS_AUTOMATIC_GAIN_CONTROL);
    }

    /**
     * Gets the Automatic Gain Control level in dB.
     *
     * <p> AGC acts as a variable gain amplifier adjusting the power of the incoming signal. The AGC
     * level may be used to indicate potential interference. Higher gain (and/or lower input power)
     * shall be output as a positive number. Hence in cases of strong jamming, in the band of this
     * signal, this value will go more negative. This value must be consistent given the same level
     * of the incoming signal power.
     *
     * <p> Note: Different hardware designs (e.g. antenna, pre-amplification, or other RF HW
     * components) may also affect the typical output of of this value on any given hardware design
     * in an open sky test - the important aspect of this output is that changes in this value are
     * indicative of changes on input signal power in the frequency band for this measurement.
     *
     * <p> The value is only available if {@link #hasAutomaticGainControlLevelDb()} is {@code true}
     *
     * @deprecated Use {@link GnssMeasurementsEvent#getGnssAutomaticGainControls()} instead.
     */
    @Deprecated
    public double getAutomaticGainControlLevelDb() {
        return mAutomaticGainControlLevelInDb;
    }

    /**
     * Sets the Automatic Gain Control level in dB.
     * @hide
     * @deprecated Use {@link GnssMeasurementsEvent.Builder#setGnssAutomaticGainControls()} instead.
     */
    @Deprecated
    @TestApi
    public void setAutomaticGainControlLevelInDb(double agcLevelDb) {
        setFlag(HAS_AUTOMATIC_GAIN_CONTROL);
        mAutomaticGainControlLevelInDb = agcLevelDb;
    }

    /**
     * Resets the Automatic Gain Control level.
     * @hide
     */
    @TestApi
    public void resetAutomaticGainControlLevel() {
        resetFlag(HAS_AUTOMATIC_GAIN_CONTROL);
    }

    /**
     * Returns {@code true} if {@link #getCodeType()} is available,
     * {@code false} otherwise.
     */
    public boolean hasCodeType() {
        return isFlagSet(HAS_CODE_TYPE);
    }

    /**
     * Gets the GNSS measurement's code type.
     *
     * <p>Similar to the Attribute field described in RINEX 4.00, e.g., in Tables 9-16 (see
     * https://igs.org/wg/rinex/#documents-formats).
     *
     * <p>Returns "A" for GALILEO E1A, GALILEO E6A, NavIC L5A SPS, NavIC SA SPS, GLONASS G1a L1OCd,
     * GLONASS G2a L2CSI.
     *
     * <p>Returns "B" for GALILEO E1B, GALILEO E6B, NavIC L5B RS (D), NavIC SB RS (D), GLONASS G1a
     * L1OCp, GLONASS G2a L2OCp, QZSS L1Sb.
     *
     * <p>Returns "C" for GPS L1 C/A, GPS L2 C/A, GLONASS G1 C/A, GLONASS G2 C/A, GALILEO E1C,
     * GALILEO E6C, SBAS L1 C/A, QZSS L1 C/A, NavIC L5C RS (P), NavIC SC RS (P).
     *
     * <p>Returns "D" for GPS L2 (L1(C/A) + (P2-P1) (semi-codeless)), QZSS L5S(I), BDS B1C Data,
     * BDS B2a Data, BDS B2b Data, BDS B2 (B2a+B2b) Data, BDS B3a Data, NavIC L1 Data.
     *
     * <p>Returns “E” for QZSS L1 C/B, QZSS L6E.
     *
     * <p>Returns "I" for GPS L5 I, GLONASS G3 I, GALILEO E5a I, GALILEO E5b I, GALILEO E5a+b I,
     * SBAS L5 I, QZSS L5 I, BDS B1 I, BDS B2 I, BDS B3 I.
     *
     * <p>Returns "L" for GPS L1C (P), GPS L2C (L), QZSS L1C (P), QZSS L2C (L), QZSS L6P, BDS
     * B1a Pilot.
     *
     * <p>Returns "M" for GPS L1M, GPS L2M.
     *
     * <p>Returns "N" for GPS L1 codeless, GPS L2 codeless.
     *
     * <p>Returns "P" for GPS L1P, GPS L2P, GLONASS G1P, GLONASS G2P, BDS B1C Pilot, BDS B2a Pilot,
     * BDS B2b Pilot, BDS B2 (B2a+B2b) Pilot, BDS B3a Pilot, QZSS L5S(Q), NavIC L1 Pilot.
     *
     * <p>Returns "Q" for GPS L5 Q, GLONASS G3 Q, GALILEO E5a Q, GALILEO E5b Q, GALILEO E5a+b Q,
     * SBAS L5 Q, QZSS L5 Q, BDS B1 Q, BDS B2 Q, BDS B3 Q.
     *
     * <p>Returns "S" for GPS L1C (D), GPS L2C (M), QZSS L1C (D), QZSS L2C (M), QZSS L6D, BDS B1a
     * Data.
     *
     * <p>Returns "W" for GPS L1 Z-tracking, GPS L2 Z-tracking.
     *
     * <p>Returns "X" for GPS L1C (D+P), GPS L2C (M+L), GPS L5 (I+Q), GLONASS G1a L1OCd+L1OCp,
     * GLONASS G2a L2CSI+L2OCp, GLONASS G3 (I+Q), GALILEO E1 (B+C), GALILEO E5a (I+Q), GALILEO
     * E5b (I+Q), GALILEO E5a+b (I+Q), GALILEO E6 (B+C), SBAS L5 (I+Q), QZSS L1C (D+P), QZSS L2C
     * (M+L), QZSS L5 (I+Q), QZSS L6 (D+P), BDS B1 (I+Q), BDS B1C Data+Pilot, BDS B2a Data+Pilot,
     * BDS B2 (I+Q), BDS B2 (B2a+B2b) Data+Pilot, BDS B3 (I+Q), NavIC L5 (B+C), NavIC S (B+C),
     * NavIC L1 Data+Pilot.
     *
     * <p>Returns "Y" for GPS L1Y, GPS L2Y.
     *
     * <p>Returns "Z" for GALILEO E1 (A+B+C), GALILEO E6 (A+B+C), QZSS L1S/L1-SAIF, QZSS L5S (I+Q),
     * QZSS L6(D+E), BDS B1A Data+Pilot, BDS B2b Data+Pilot, BDS B3a Data+Pilot.
     *
     * <p>Returns "UNKNOWN" if the GNSS Measurement's code type is unknown.
     *
     * <p>The code type is used to specify the observation descriptor defined in GNSS Observation
     * Data File Header Section Description in the RINEX standard (Version 4.00). In cases where
     * the code type does not align with the above listed values, the code type from the most
     * recent version of RINEX should be used. For example, if a code type "G" is added, this
     * string shall be set to "G".
     */
    @NonNull
    public String getCodeType() {
        return mCodeType;
    }

    /**
     * Sets the GNSS measurement's code type.
     *
     * @hide
     */
    @TestApi
    public void setCodeType(@NonNull String codeType) {
        setFlag(HAS_CODE_TYPE);
        mCodeType = codeType;
    }

    /**
     * Resets the GNSS measurement's code type.
     *
     * @hide
     */
    @TestApi
    public void resetCodeType() {
        resetFlag(HAS_CODE_TYPE);
        mCodeType = "UNKNOWN";
    }

    /**
     * Returns {@code true} if {@link #getFullInterSignalBiasNanos()} is available,
     * {@code false} otherwise.
     */
    public boolean hasFullInterSignalBiasNanos() {
        return isFlagSet(HAS_FULL_ISB);
    }

    /**
     * Gets the GNSS measurement's inter-signal bias in nanoseconds with sub-nanosecond accuracy.
     *
     * <p>This value is the sum of the estimated receiver-side and the space-segment-side
     * inter-system bias, inter-frequency bias and inter-code bias, including:
     *
     * <ul>
     * <li>Receiver inter-constellation bias (with respect to the constellation in
     * {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * <li>Receiver inter-frequency bias (with respect to the carrier frequency in
     * {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * <li>Receiver inter-code bias (with respect to the code type in
     * {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * <li>Master clock bias (e.g., GPS-GAL Time Offset (GGTO), GPS-UTC Time Offset (TauGps),
     * BDS-GLO Time Offset (BGTO))(with respect to the constellation in
     * {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * <li>Group delay (e.g., Total Group Delay (TGD))</li>
     * <li>Satellite inter-frequency bias (GLO only) (with respect to the carrier frequency in
     * {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * <li>Satellite inter-code bias (e.g., Differential Code Bias (DCB)) (with respect to the code
     * type in {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * </ul>
     *
     * <p>If a component of the above is already compensated in the provided
     * {@link GnssMeasurement#getReceivedSvTimeNanos()}, then it must not be included in the
     * reported full ISB.
     *
     * <p>The value does not include the inter-frequency Ionospheric bias.
     *
     * <p>The sign of the value is defined by the following equation:
     * <pre>
     *     corrected pseudorange = raw pseudorange - FullInterSignalBiasNanos</pre>
     *
     * <p>The value is only available if {@link #hasFullInterSignalBiasNanos()} is {@code true}.
     */
    public double getFullInterSignalBiasNanos() {
        return mFullInterSignalBiasNanos;
    }

    /**
     * Sets the GNSS measurement's inter-signal bias in nanoseconds.
     *
     * @hide
     */
    @TestApi
    public void setFullInterSignalBiasNanos(double fullInterSignalBiasNanos) {
        setFlag(HAS_FULL_ISB);
        mFullInterSignalBiasNanos = fullInterSignalBiasNanos;
    }

    /**
     * Resets the GNSS measurement's inter-signal bias in nanoseconds.
     *
     * @hide
     */
    @TestApi
    public void resetFullInterSignalBiasNanos() {
        resetFlag(HAS_FULL_ISB);
    }

    /**
     * Returns {@code true} if {@link #getFullInterSignalBiasUncertaintyNanos()} is available,
     * {@code false} otherwise.
     */
    public boolean hasFullInterSignalBiasUncertaintyNanos() {
        return isFlagSet(HAS_FULL_ISB_UNCERTAINTY);
    }

    /**
     * Gets the GNSS measurement's inter-signal bias uncertainty (1 sigma) in
     * nanoseconds with sub-nanosecond accuracy.
     *
     * <p>The value is only available if {@link #hasFullInterSignalBiasUncertaintyNanos()} is
     * {@code true}.
     */
    @FloatRange(from = 0.0)
    public double getFullInterSignalBiasUncertaintyNanos() {
        return mFullInterSignalBiasUncertaintyNanos;
    }

    /**
     * Sets the GNSS measurement's inter-signal bias uncertainty (1 sigma) in nanoseconds.
     *
     * @hide
     */
    @TestApi
    public void setFullInterSignalBiasUncertaintyNanos(@FloatRange(from = 0.0)
            double fullInterSignalBiasUncertaintyNanos) {
        setFlag(HAS_FULL_ISB_UNCERTAINTY);
        mFullInterSignalBiasUncertaintyNanos = fullInterSignalBiasUncertaintyNanos;
    }

    /**
     * Resets the GNSS measurement's inter-signal bias uncertainty (1 sigma) in
     * nanoseconds.
     *
     * @hide
     */
    @TestApi
    public void resetFullInterSignalBiasUncertaintyNanos() {
        resetFlag(HAS_FULL_ISB_UNCERTAINTY);
    }

    /**
     * Returns {@code true} if {@link #getSatelliteInterSignalBiasNanos()} is available,
     * {@code false} otherwise.
     */
    public boolean hasSatelliteInterSignalBiasNanos() {
        return isFlagSet(HAS_SATELLITE_ISB);
    }

    /**
     * Gets the GNSS measurement's satellite inter-signal bias in nanoseconds with sub-nanosecond
     * accuracy.
     *
     * <p>This value is the space-segment-side inter-system bias, inter-frequency bias and
     * inter-code bias, including:
     *
     * <ul>
     * <li>Master clock bias (e.g., GPS-GAL Time Offset (GGTO), GPS-UTC Time Offset (TauGps),
     * BDS-GLO Time Offset (BGTO))(with respect to the constellation in
     * {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * <li>Group delay (e.g., Total Group Delay (TGD))</li>
     * <li>Satellite inter-frequency bias (GLO only) (with respect to the carrier frequency in
     * {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * <li>Satellite inter-code bias (e.g., Differential Code Bias (DCB)) (with respect to the code
     * type in {@link GnssClock#getReferenceConstellationTypeForIsb())</li>
     * </ul>
     *
     * <p>The sign of the value is defined by the following equation:
     * <pre>
     *     corrected pseudorange = raw pseudorange - SatelliteInterSignalBiasNanos</pre>
     *
     * <p>The value is only available if {@link #hasSatelliteInterSignalBiasNanos()} is {@code
     * true}.
     */
    public double getSatelliteInterSignalBiasNanos() {
        return mSatelliteInterSignalBiasNanos;
    }

    /**
     * Sets the GNSS measurement's satellite inter-signal bias in nanoseconds.
     *
     * @hide
     */
    @TestApi
    public void setSatelliteInterSignalBiasNanos(double satelliteInterSignalBiasNanos) {
        setFlag(HAS_SATELLITE_ISB);
        mSatelliteInterSignalBiasNanos = satelliteInterSignalBiasNanos;
    }

    /**
     * Resets the GNSS measurement's satellite inter-signal bias in nanoseconds.
     *
     * @hide
     */
    @TestApi
    public void resetSatelliteInterSignalBiasNanos() {
        resetFlag(HAS_SATELLITE_ISB);
    }

    /**
     * Returns {@code true} if {@link #getSatelliteInterSignalBiasUncertaintyNanos()} is available,
     * {@code false} otherwise.
     */
    public boolean hasSatelliteInterSignalBiasUncertaintyNanos() {
        return isFlagSet(HAS_SATELLITE_ISB_UNCERTAINTY);
    }

    /**
     * Gets the GNSS measurement's satellite inter-signal bias uncertainty (1 sigma) in
     * nanoseconds with sub-nanosecond accuracy.
     *
     * <p>The value is only available if {@link #hasSatelliteInterSignalBiasUncertaintyNanos()} is
     * {@code true}.
     */
    @FloatRange(from = 0.0)
    public double getSatelliteInterSignalBiasUncertaintyNanos() {
        return mSatelliteInterSignalBiasUncertaintyNanos;
    }

    /**
     * Sets the GNSS measurement's satellite inter-signal bias uncertainty (1 sigma) in nanoseconds.
     *
     * @hide
     */
    @TestApi
    public void setSatelliteInterSignalBiasUncertaintyNanos(@FloatRange(from = 0.0)
            double satelliteInterSignalBiasUncertaintyNanos) {
        setFlag(HAS_SATELLITE_ISB_UNCERTAINTY);
        mSatelliteInterSignalBiasUncertaintyNanos = satelliteInterSignalBiasUncertaintyNanos;
    }

    /**
     * Resets the GNSS measurement's satellite inter-signal bias uncertainty (1 sigma) in
     * nanoseconds.
     *
     * @hide
     */
    @TestApi
    public void resetSatelliteInterSignalBiasUncertaintyNanos() {
        resetFlag(HAS_SATELLITE_ISB_UNCERTAINTY);
    }

    /**
     * Returns {@code true} if {@link #getSatellitePvt()} is available,
     * {@code false} otherwise.
     *
     * @hide
     */
    @SystemApi
    public boolean hasSatellitePvt() {
        return isFlagSet(HAS_SATELLITE_PVT);
    }

    /**
     * Gets the Satellite PVT data.
     *
     * <p>The value is only available if {@link #hasSatellitePvt()} is
     * {@code true}.
     *
     * @hide
     */
    @Nullable
    @SystemApi
    public SatellitePvt getSatellitePvt() {
        return mSatellitePvt;
    }

    /**
     * Sets the Satellite PVT.
     *
     * @hide
     */
    @TestApi
    public void setSatellitePvt(@Nullable SatellitePvt satellitePvt) {
        if (satellitePvt == null) {
            resetSatellitePvt();
        } else {
            setFlag(HAS_SATELLITE_PVT);
            mSatellitePvt = satellitePvt;
        }
    }

    /**
     * Resets the Satellite PVT.
     *
     * @hide
     */
    @TestApi
    public void resetSatellitePvt() {
        resetFlag(HAS_SATELLITE_PVT);
    }

    /**
     * Returns {@code true} if {@link #getCorrelationVectors()} is available,
     * {@code false} otherwise.
     *
     * @hide
     */
    @SystemApi
    public boolean hasCorrelationVectors() {
        return isFlagSet(HAS_CORRELATION_VECTOR);
    }

    /**
     * Gets read-only collection of CorrelationVector with each CorrelationVector corresponding to a
     * frequency offset.
     *
     * <p>To represent correlation values over a 2D spaces (delay and frequency), a
     * CorrelationVector is required per frequency offset, and each CorrelationVector contains
     * correlation values at equally spaced spatial offsets.
     *
     * @hide
     */
    @Nullable
    @SystemApi
    @SuppressLint("NullableCollection")
    public Collection<CorrelationVector> getCorrelationVectors() {
        return mReadOnlyCorrelationVectors;
    }

    /**
     * Sets the CorrelationVectors.
     *
     * @hide
     */
    @TestApi
    public void setCorrelationVectors(
            @SuppressLint("NullableCollection")
            @Nullable Collection<CorrelationVector> correlationVectors) {
        if (correlationVectors == null || correlationVectors.isEmpty()) {
            resetCorrelationVectors();
        } else {
            setFlag(HAS_CORRELATION_VECTOR);
            mReadOnlyCorrelationVectors = Collections.unmodifiableCollection(correlationVectors);
        }
    }

    /**
     * Resets the CorrelationVectors.
     *
     * @hide
     */
    @TestApi
    public void resetCorrelationVectors() {
        resetFlag(HAS_CORRELATION_VECTOR);
        mReadOnlyCorrelationVectors = null;
    }

    public static final @NonNull Creator<GnssMeasurement> CREATOR = new Creator<GnssMeasurement>() {
        @Override
        public GnssMeasurement createFromParcel(Parcel parcel) {
            GnssMeasurement gnssMeasurement = new GnssMeasurement();

            gnssMeasurement.mFlags = parcel.readInt();
            gnssMeasurement.mSvid = parcel.readInt();
            gnssMeasurement.mConstellationType = parcel.readInt();
            gnssMeasurement.mTimeOffsetNanos = parcel.readDouble();
            gnssMeasurement.mState = parcel.readInt();
            gnssMeasurement.mReceivedSvTimeNanos = parcel.readLong();
            gnssMeasurement.mReceivedSvTimeUncertaintyNanos = parcel.readLong();
            gnssMeasurement.mCn0DbHz = parcel.readDouble();
            gnssMeasurement.mPseudorangeRateMetersPerSecond = parcel.readDouble();
            gnssMeasurement.mPseudorangeRateUncertaintyMetersPerSecond = parcel.readDouble();
            gnssMeasurement.mAccumulatedDeltaRangeState = parcel.readInt();
            gnssMeasurement.mAccumulatedDeltaRangeMeters = parcel.readDouble();
            gnssMeasurement.mAccumulatedDeltaRangeUncertaintyMeters = parcel.readDouble();
            gnssMeasurement.mCarrierFrequencyHz = parcel.readFloat();
            gnssMeasurement.mCarrierCycles = parcel.readLong();
            gnssMeasurement.mCarrierPhase = parcel.readDouble();
            gnssMeasurement.mCarrierPhaseUncertainty = parcel.readDouble();
            gnssMeasurement.mMultipathIndicator = parcel.readInt();
            gnssMeasurement.mSnrInDb = parcel.readDouble();
            gnssMeasurement.mAutomaticGainControlLevelInDb = parcel.readDouble();
            gnssMeasurement.mCodeType = parcel.readString();
            gnssMeasurement.mBasebandCn0DbHz = parcel.readDouble();
            gnssMeasurement.mFullInterSignalBiasNanos = parcel.readDouble();
            gnssMeasurement.mFullInterSignalBiasUncertaintyNanos = parcel.readDouble();
            gnssMeasurement.mSatelliteInterSignalBiasNanos = parcel.readDouble();
            gnssMeasurement.mSatelliteInterSignalBiasUncertaintyNanos = parcel.readDouble();
            if (gnssMeasurement.hasSatellitePvt()) {
                ClassLoader classLoader = getClass().getClassLoader();
                gnssMeasurement.mSatellitePvt = parcel.readParcelable(classLoader, android.location.SatellitePvt.class);
            }
            if (gnssMeasurement.hasCorrelationVectors()) {
                CorrelationVector[] correlationVectorsArray =
                        new CorrelationVector[parcel.readInt()];
                parcel.readTypedArray(correlationVectorsArray, CorrelationVector.CREATOR);
                Collection<CorrelationVector> corrVecCollection =
                        Arrays.asList(correlationVectorsArray);
                gnssMeasurement.mReadOnlyCorrelationVectors =
                        Collections.unmodifiableCollection(corrVecCollection);
            }
            return gnssMeasurement;
        }

        @Override
        public GnssMeasurement[] newArray(int i) {
            return new GnssMeasurement[i];
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mFlags);
        parcel.writeInt(mSvid);
        parcel.writeInt(mConstellationType);
        parcel.writeDouble(mTimeOffsetNanos);
        parcel.writeInt(mState);
        parcel.writeLong(mReceivedSvTimeNanos);
        parcel.writeLong(mReceivedSvTimeUncertaintyNanos);
        parcel.writeDouble(mCn0DbHz);
        parcel.writeDouble(mPseudorangeRateMetersPerSecond);
        parcel.writeDouble(mPseudorangeRateUncertaintyMetersPerSecond);
        parcel.writeInt(mAccumulatedDeltaRangeState);
        parcel.writeDouble(mAccumulatedDeltaRangeMeters);
        parcel.writeDouble(mAccumulatedDeltaRangeUncertaintyMeters);
        parcel.writeFloat(mCarrierFrequencyHz);
        parcel.writeLong(mCarrierCycles);
        parcel.writeDouble(mCarrierPhase);
        parcel.writeDouble(mCarrierPhaseUncertainty);
        parcel.writeInt(mMultipathIndicator);
        parcel.writeDouble(mSnrInDb);
        parcel.writeDouble(mAutomaticGainControlLevelInDb);
        parcel.writeString(mCodeType);
        parcel.writeDouble(mBasebandCn0DbHz);
        parcel.writeDouble(mFullInterSignalBiasNanos);
        parcel.writeDouble(mFullInterSignalBiasUncertaintyNanos);
        parcel.writeDouble(mSatelliteInterSignalBiasNanos);
        parcel.writeDouble(mSatelliteInterSignalBiasUncertaintyNanos);
        if (hasSatellitePvt()) {
            parcel.writeParcelable(mSatellitePvt, flags);
        }
        if (hasCorrelationVectors()) {
            int correlationVectorCount = mReadOnlyCorrelationVectors.size();
            CorrelationVector[] correlationVectorArray =
                mReadOnlyCorrelationVectors.toArray(new CorrelationVector[correlationVectorCount]);
            parcel.writeInt(correlationVectorArray.length);
            parcel.writeTypedArray(correlationVectorArray, flags);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        final String format = "   %-29s = %s\n";
        final String formatWithUncertainty = "   %-29s = %-25s   %-40s = %s\n";
        StringBuilder builder = new StringBuilder("GnssMeasurement:\n");

        builder.append(String.format(format, "Svid", mSvid));
        builder.append(String.format(format, "ConstellationType", mConstellationType));
        builder.append(String.format(format, "TimeOffsetNanos", mTimeOffsetNanos));

        builder.append(String.format(format, "State", getStateString()));

        builder.append(String.format(
                formatWithUncertainty,
                "ReceivedSvTimeNanos",
                mReceivedSvTimeNanos,
                "ReceivedSvTimeUncertaintyNanos",
                mReceivedSvTimeUncertaintyNanos));

        builder.append(String.format(format, "Cn0DbHz", mCn0DbHz));

        if (hasBasebandCn0DbHz()) {
            builder.append(String.format(format, "BasebandCn0DbHz", mBasebandCn0DbHz));
        }

        builder.append(String.format(
                formatWithUncertainty,
                "PseudorangeRateMetersPerSecond",
                mPseudorangeRateMetersPerSecond,
                "PseudorangeRateUncertaintyMetersPerSecond",
                mPseudorangeRateUncertaintyMetersPerSecond));

        builder.append(String.format(
                format,
                "AccumulatedDeltaRangeState",
                getAccumulatedDeltaRangeStateString()));

        builder.append(String.format(
                formatWithUncertainty,
                "AccumulatedDeltaRangeMeters",
                mAccumulatedDeltaRangeMeters,
                "AccumulatedDeltaRangeUncertaintyMeters",
                mAccumulatedDeltaRangeUncertaintyMeters));

        if (hasCarrierFrequencyHz()) {
            builder.append(String.format(format, "CarrierFrequencyHz", mCarrierFrequencyHz));
        }

        if (hasCarrierCycles()) {
            builder.append(String.format(format, "CarrierCycles", mCarrierCycles));
        }

        if (hasCarrierPhase() || hasCarrierPhaseUncertainty()) {
            builder.append(String.format(
                    formatWithUncertainty,
                    "CarrierPhase",
                    hasCarrierPhase() ? mCarrierPhase : null,
                    "CarrierPhaseUncertainty",
                    hasCarrierPhaseUncertainty() ? mCarrierPhaseUncertainty : null));
        }

        builder.append(String.format(format, "MultipathIndicator", getMultipathIndicatorString()));

        if (hasSnrInDb()) {
            builder.append(String.format(format, "SnrInDb", mSnrInDb));
        }

        if (hasAutomaticGainControlLevelDb()) {
            builder.append(String.format(format, "AgcLevelDb", mAutomaticGainControlLevelInDb));
        }

        if (hasCodeType()) {
            builder.append(String.format(format, "CodeType", mCodeType));
        }

        if (hasFullInterSignalBiasNanos() || hasFullInterSignalBiasUncertaintyNanos()) {
            builder.append(String.format(
                    formatWithUncertainty,
                    "InterSignalBiasNs",
                    hasFullInterSignalBiasNanos() ? mFullInterSignalBiasNanos : null,
                    "InterSignalBiasUncertaintyNs",
                    hasFullInterSignalBiasUncertaintyNanos()
                            ? mFullInterSignalBiasUncertaintyNanos : null));
        }

        if (hasSatelliteInterSignalBiasNanos() || hasSatelliteInterSignalBiasUncertaintyNanos()) {
            builder.append(String.format(
                    formatWithUncertainty,
                    "SatelliteInterSignalBiasNs",
                    hasSatelliteInterSignalBiasNanos() ? mSatelliteInterSignalBiasNanos : null,
                    "SatelliteInterSignalBiasUncertaintyNs",
                    hasSatelliteInterSignalBiasUncertaintyNanos()
                            ? mSatelliteInterSignalBiasUncertaintyNanos
                            : null));
        }

        if (hasSatellitePvt()) {
            builder.append(mSatellitePvt.toString());
        }

        if (hasCorrelationVectors()) {
            for (CorrelationVector correlationVector : mReadOnlyCorrelationVectors) {
                builder.append(correlationVector.toString());
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private void initialize() {
        mFlags = HAS_NO_FLAGS;
        setSvid(0);
        setTimeOffsetNanos(Long.MIN_VALUE);
        setState(STATE_UNKNOWN);
        setReceivedSvTimeNanos(Long.MIN_VALUE);
        setReceivedSvTimeUncertaintyNanos(Long.MAX_VALUE);
        setCn0DbHz(Double.MIN_VALUE);
        setPseudorangeRateMetersPerSecond(Double.MIN_VALUE);
        setPseudorangeRateUncertaintyMetersPerSecond(Double.MIN_VALUE);
        setAccumulatedDeltaRangeState(ADR_STATE_UNKNOWN);
        setAccumulatedDeltaRangeMeters(Double.MIN_VALUE);
        setAccumulatedDeltaRangeUncertaintyMeters(Double.MIN_VALUE);
        resetCarrierFrequencyHz();
        resetCarrierCycles();
        resetCarrierPhase();
        resetCarrierPhaseUncertainty();
        setMultipathIndicator(MULTIPATH_INDICATOR_UNKNOWN);
        resetSnrInDb();
        resetAutomaticGainControlLevel();
        resetCodeType();
        resetBasebandCn0DbHz();
        resetFullInterSignalBiasNanos();
        resetFullInterSignalBiasUncertaintyNanos();
        resetSatelliteInterSignalBiasNanos();
        resetSatelliteInterSignalBiasUncertaintyNanos();
        resetSatellitePvt();
        resetCorrelationVectors();
    }

    private void setFlag(int flag) {
        mFlags |= flag;
    }

    private void resetFlag(int flag) {
        mFlags &= ~flag;
    }

    private boolean isFlagSet(int flag) {
        return (mFlags & flag) == flag;
    }
}
