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

import android.annotation.FloatRange;
import android.annotation.NonNull;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class containing a GNSS clock timestamp.
 *
 * <p>It represents a measurement of the GNSS receiver's clock.
 */
public final class GnssClock implements Parcelable {
    // The following enumerations must be in sync with the values declared in gps.h

    private static final int HAS_NO_FLAGS = 0;
    private static final int HAS_LEAP_SECOND = (1<<0);
    private static final int HAS_TIME_UNCERTAINTY = (1<<1);
    private static final int HAS_FULL_BIAS = (1<<2);
    private static final int HAS_BIAS = (1<<3);
    private static final int HAS_BIAS_UNCERTAINTY = (1<<4);
    private static final int HAS_DRIFT = (1<<5);
    private static final int HAS_DRIFT_UNCERTAINTY = (1<<6);
    private static final int HAS_ELAPSED_REALTIME_NANOS = (1 << 7);
    private static final int HAS_ELAPSED_REALTIME_UNCERTAINTY_NANOS = (1 << 8);
    private static final int HAS_REFERENCE_CONSTELLATION_TYPE_FOR_ISB = (1 << 9);
    private static final int HAS_REFERENCE_CARRIER_FREQUENCY_FOR_ISB = (1 << 10);
    private static final int HAS_REFERENCE_CODE_TYPE_FOR_ISB = (1 << 11);

    // End enumerations in sync with gps.h

    private int mFlags;
    private int mLeapSecond;
    private long mTimeNanos;
    private double mTimeUncertaintyNanos;
    private long mFullBiasNanos;
    private double mBiasNanos;
    private double mBiasUncertaintyNanos;
    private double mDriftNanosPerSecond;
    private double mDriftUncertaintyNanosPerSecond;
    private int mHardwareClockDiscontinuityCount;
    private long mElapsedRealtimeNanos;
    private double mElapsedRealtimeUncertaintyNanos;
    private int mReferenceConstellationTypeForIsb;
    private double mReferenceCarrierFrequencyHzForIsb;
    private String mReferenceCodeTypeForIsb;

    /**
     * @hide
     */
    @TestApi
    public GnssClock() {
        initialize();
    }

    /**
     * Sets all contents to the values stored in the provided object.
     * @hide
     */
    @TestApi
    public void set(GnssClock clock) {
        mFlags = clock.mFlags;
        mLeapSecond = clock.mLeapSecond;
        mTimeNanos = clock.mTimeNanos;
        mTimeUncertaintyNanos = clock.mTimeUncertaintyNanos;
        mFullBiasNanos = clock.mFullBiasNanos;
        mBiasNanos = clock.mBiasNanos;
        mBiasUncertaintyNanos = clock.mBiasUncertaintyNanos;
        mDriftNanosPerSecond = clock.mDriftNanosPerSecond;
        mDriftUncertaintyNanosPerSecond = clock.mDriftUncertaintyNanosPerSecond;
        mHardwareClockDiscontinuityCount = clock.mHardwareClockDiscontinuityCount;
        mElapsedRealtimeNanos = clock.mElapsedRealtimeNanos;
        mElapsedRealtimeUncertaintyNanos = clock.mElapsedRealtimeUncertaintyNanos;
        mReferenceConstellationTypeForIsb = clock.mReferenceConstellationTypeForIsb;
        mReferenceCarrierFrequencyHzForIsb = clock.mReferenceCarrierFrequencyHzForIsb;
        mReferenceCodeTypeForIsb = clock.mReferenceCodeTypeForIsb;
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
     * Returns {@code true} if {@link #getLeapSecond()} is available, {@code false} otherwise.
     */
    public boolean hasLeapSecond() {
        return isFlagSet(HAS_LEAP_SECOND);
    }

    /**
     * Gets the leap second associated with the clock's time.
     *
     * <p>The sign of the value is defined by the following equation:
     * <pre>
     *     UtcTimeNanos = TimeNanos - (FullBiasNanos + BiasNanos) - LeapSecond * 1,000,000,000</pre>
     *
     * <p>The value is only available if {@link #hasLeapSecond()} is {@code true}.
     */
    public int getLeapSecond() {
        return mLeapSecond;
    }

    /**
     * Sets the leap second associated with the clock's time.
     * @hide
     */
    @TestApi
    public void setLeapSecond(int leapSecond) {
        setFlag(HAS_LEAP_SECOND);
        mLeapSecond = leapSecond;
    }

    /**
     * Resets the leap second associated with the clock's time.
     * @hide
     */
    @TestApi
    public void resetLeapSecond() {
        resetFlag(HAS_LEAP_SECOND);
        mLeapSecond = Integer.MIN_VALUE;
    }

    /**
     * Gets the GNSS receiver internal hardware clock value in nanoseconds.
     *
     * <p>This value is expected to be monotonically increasing while the hardware clock remains
     * powered on. For the case of a hardware clock that is not continuously on, see the
     * {@link #getHardwareClockDiscontinuityCount} field. The GPS time can be derived by subtracting
     * the sum of {@link #getFullBiasNanos()} and {@link #getBiasNanos()} (when they are available)
     * from this value. Sub-nanosecond accuracy can be provided by means of {@link #getBiasNanos()}.
     *
     * <p>The error estimate for this value (if applicable) is {@link #getTimeUncertaintyNanos()}.
     */
    public long getTimeNanos() {
        return mTimeNanos;
    }

    /**
     * Sets the GNSS receiver internal clock in nanoseconds.
     * @hide
     */
    @TestApi
    public void setTimeNanos(long timeNanos) {
        mTimeNanos = timeNanos;
    }

    /**
     * Returns {@code true} if {@link #getTimeUncertaintyNanos()} is available, {@code false}
     * otherwise.
     */
    public boolean hasTimeUncertaintyNanos() {
        return isFlagSet(HAS_TIME_UNCERTAINTY);
    }

    /**
     * Gets the clock's time Uncertainty (1-Sigma) in nanoseconds.
     *
     * <p>The uncertainty is represented as an absolute (single sided) value.
     *
     * <p>The value is only available if {@link #hasTimeUncertaintyNanos()} is {@code true}.
     *
     * <p>This value is often effectively zero (it is the reference clock by which all other times
     * and time uncertainties are measured), and thus this field may often be 0, or not provided.
     */
    @FloatRange(from = 0.0f)
    public double getTimeUncertaintyNanos() {
        return mTimeUncertaintyNanos;
    }

    /**
     * Sets the clock's Time Uncertainty (1-Sigma) in nanoseconds.
     * @hide
     */
    @TestApi
    public void setTimeUncertaintyNanos(@FloatRange(from = 0.0f) double timeUncertaintyNanos) {
        setFlag(HAS_TIME_UNCERTAINTY);
        mTimeUncertaintyNanos = timeUncertaintyNanos;
    }

    /**
     * Resets the clock's Time Uncertainty (1-Sigma) in nanoseconds.
     * @hide
     */
    @TestApi
    public void resetTimeUncertaintyNanos() {
        resetFlag(HAS_TIME_UNCERTAINTY);
    }

    /**
     * Returns {@code true} if {@link #getFullBiasNanos()} is available, {@code false} otherwise.
     */
    public boolean hasFullBiasNanos() {
        return isFlagSet(HAS_FULL_BIAS);
    }

    /**
     * Gets the difference between hardware clock ({@link #getTimeNanos()}) inside GPS receiver and
     * the true GPS time since 0000Z, January 6, 1980, in nanoseconds.
     *
     * <p>This value is available if the receiver has estimated GPS time. If the computed time is
     * for a non-GPS constellation, the time offset of that constellation to GPS has to be applied
     * to fill this value. The value is only available if {@link #hasFullBiasNanos()} is
     * {@code true}.
     *
     * <p>The error estimate for the sum of this field and {@link #getBiasNanos} is
     * {@link #getBiasUncertaintyNanos()}.
     *
     * <p>The sign of the value is defined by the following equation:
     *
     * <pre>
     *     local estimate of GPS time = TimeNanos - (FullBiasNanos + BiasNanos)</pre>
     */
    public long getFullBiasNanos() {
        return mFullBiasNanos;
    }

    /**
     * Sets the full bias in nanoseconds.
     * @hide
     */
    @TestApi
    public void setFullBiasNanos(long value) {
        setFlag(HAS_FULL_BIAS);
        mFullBiasNanos = value;
    }

    /**
     * Resets the full bias in nanoseconds.
     * @hide
     */
    @TestApi
    public void resetFullBiasNanos() {
        resetFlag(HAS_FULL_BIAS);
        mFullBiasNanos = Long.MIN_VALUE;
    }

    /**
     * Returns {@code true} if {@link #getBiasNanos()} is available, {@code false} otherwise.
     */
    public boolean hasBiasNanos() {
        return isFlagSet(HAS_BIAS);
    }

    /**
     * Gets the clock's sub-nanosecond bias.
     *
     * <p>See the description of how this field is part of converting from hardware clock time, to
     * GPS time, in {@link #getFullBiasNanos()}.
     *
     * <p>The error estimate for the sum of this field and {@link #getFullBiasNanos} is
     * {@link #getBiasUncertaintyNanos()}.
     *
     * <p>The value is only available if {@link #hasBiasNanos()} is {@code true}.
     */
    public double getBiasNanos() {
        return mBiasNanos;
    }

    /**
     * Sets the sub-nanosecond bias.
     * @hide
     */
    @TestApi
    public void setBiasNanos(double biasNanos) {
        setFlag(HAS_BIAS);
        mBiasNanos = biasNanos;
    }

    /**
     * Resets the clock's Bias in nanoseconds.
     * @hide
     */
    @TestApi
    public void resetBiasNanos() {
        resetFlag(HAS_BIAS);
    }

    /**
     * Returns {@code true} if {@link #getBiasUncertaintyNanos()} is available, {@code false}
     * otherwise.
     */
    public boolean hasBiasUncertaintyNanos() {
        return isFlagSet(HAS_BIAS_UNCERTAINTY);
    }

    /**
     * Gets the clock's Bias Uncertainty (1-Sigma) in nanoseconds.
     *
     * <p>See the description of how this field provides the error estimate in the conversion from
     * hardware clock time, to GPS time, in {@link #getFullBiasNanos()}.
     *
     * <p>The value is only available if {@link #hasBiasUncertaintyNanos()} is {@code true}.
     */
    @FloatRange(from = 0.0f)
    public double getBiasUncertaintyNanos() {
        return mBiasUncertaintyNanos;
    }

    /**
     * Sets the clock's Bias Uncertainty (1-Sigma) in nanoseconds.
     * @hide
     */
    @TestApi
    public void setBiasUncertaintyNanos(@FloatRange(from = 0.0f) double biasUncertaintyNanos) {
        setFlag(HAS_BIAS_UNCERTAINTY);
        mBiasUncertaintyNanos = biasUncertaintyNanos;
    }

    /**
     * Resets the clock's Bias Uncertainty (1-Sigma) in nanoseconds.
     * @hide
     */
    @TestApi
    public void resetBiasUncertaintyNanos() {
        resetFlag(HAS_BIAS_UNCERTAINTY);
    }

    /**
     * Returns {@code true} if {@link #getDriftNanosPerSecond()} is available, {@code false}
     * otherwise.
     */
    public boolean hasDriftNanosPerSecond() {
        return isFlagSet(HAS_DRIFT);
    }

    /**
     * Gets the clock's Drift in nanoseconds per second.
     *
     * <p>This value is the instantaneous time-derivative of the value provided by
     * {@link #getBiasNanos()}.
     *
     * <p>A positive value indicates that the frequency is higher than the nominal (e.g. GPS master
     * clock) frequency. The error estimate for this reported drift is
     * {@link #getDriftUncertaintyNanosPerSecond()}.
     *
     * <p>The value is only available if {@link #hasDriftNanosPerSecond()} is {@code true}.
     */
    public double getDriftNanosPerSecond() {
        return mDriftNanosPerSecond;
    }

    /**
     * Sets the clock's Drift in nanoseconds per second.
     * @hide
     */
    @TestApi
    public void setDriftNanosPerSecond(double driftNanosPerSecond) {
        setFlag(HAS_DRIFT);
        mDriftNanosPerSecond = driftNanosPerSecond;
    }

    /**
     * Resets the clock's Drift in nanoseconds per second.
     * @hide
     */
    @TestApi
    public void resetDriftNanosPerSecond() {
        resetFlag(HAS_DRIFT);
    }

    /**
     * Returns {@code true} if {@link #getDriftUncertaintyNanosPerSecond()} is available,
     * {@code false} otherwise.
     */
    public boolean hasDriftUncertaintyNanosPerSecond() {
        return isFlagSet(HAS_DRIFT_UNCERTAINTY);
    }

    /**
     * Gets the clock's Drift Uncertainty (1-Sigma) in nanoseconds per second.
     *
     * <p>The value is only available if {@link #hasDriftUncertaintyNanosPerSecond()} is
     * {@code true}.
     */
    @FloatRange(from = 0.0f)
    public double getDriftUncertaintyNanosPerSecond() {
        return mDriftUncertaintyNanosPerSecond;
    }

    /**
     * Sets the clock's Drift Uncertainty (1-Sigma) in nanoseconds per second.
     * @hide
     */
    @TestApi
    public void setDriftUncertaintyNanosPerSecond(
            @FloatRange(from = 0.0f) double driftUncertaintyNanosPerSecond) {
        setFlag(HAS_DRIFT_UNCERTAINTY);
        mDriftUncertaintyNanosPerSecond = driftUncertaintyNanosPerSecond;
    }

    /**
     * Resets the clock's Drift Uncertainty (1-Sigma) in nanoseconds per second.
     * @hide
     */
    @TestApi
    public void resetDriftUncertaintyNanosPerSecond() {
        resetFlag(HAS_DRIFT_UNCERTAINTY);
    }

    /**
     * Returns {@code true} if {@link #getElapsedRealtimeNanos()} is available, {@code false}
     * otherwise.
     */
    public boolean hasElapsedRealtimeNanos() {
        return isFlagSet(HAS_ELAPSED_REALTIME_NANOS);
    }

    /**
     * Returns the elapsed real-time of this clock since system boot, in nanoseconds.
     *
     * <p>The value is only available if {@link #hasElapsedRealtimeNanos()} is
     * {@code true}.
     */
    public long getElapsedRealtimeNanos() {
        return mElapsedRealtimeNanos;
    }

    /**
     * Sets the elapsed real-time of this clock since system boot, in nanoseconds.
     * @hide
     */
    @TestApi
    public void setElapsedRealtimeNanos(long elapsedRealtimeNanos) {
        setFlag(HAS_ELAPSED_REALTIME_NANOS);
        mElapsedRealtimeNanos = elapsedRealtimeNanos;
    }

    /**
     * Resets the elapsed real-time of this clock since system boot, in nanoseconds.
     * @hide
     */
    @TestApi
    public void resetElapsedRealtimeNanos() {
        resetFlag(HAS_ELAPSED_REALTIME_NANOS);
        mElapsedRealtimeNanos = 0;
    }

    /**
     * Returns {@code true} if {@link #getElapsedRealtimeUncertaintyNanos()} is available, {@code
     * false} otherwise.
     */
    public boolean hasElapsedRealtimeUncertaintyNanos() {
        return isFlagSet(HAS_ELAPSED_REALTIME_UNCERTAINTY_NANOS);
    }

    /**
     * Gets the estimate of the relative precision of the alignment of the
     * {@link #getElapsedRealtimeNanos()} timestamp, with the reported measurements in
     * nanoseconds (68% confidence).
     *
     * <p>The value is only available if {@link #hasElapsedRealtimeUncertaintyNanos()} is
     * {@code true}.
     */
    @FloatRange(from = 0.0f)
    public double getElapsedRealtimeUncertaintyNanos() {
        return mElapsedRealtimeUncertaintyNanos;
    }

    /**
     * Sets the estimate of the relative precision of the alignment of the
     * {@link #getElapsedRealtimeNanos()} timestamp, with the reported measurements in
     * nanoseconds (68% confidence).
     * @hide
     */
    @TestApi
    public void setElapsedRealtimeUncertaintyNanos(
            @FloatRange(from = 0.0f) double elapsedRealtimeUncertaintyNanos) {
        setFlag(HAS_ELAPSED_REALTIME_UNCERTAINTY_NANOS);
        mElapsedRealtimeUncertaintyNanos = elapsedRealtimeUncertaintyNanos;
    }

    /**
     * Resets the estimate of the relative precision of the alignment of the
     * {@link #getElapsedRealtimeNanos()} timestamp, with the reported measurements in
     * nanoseconds (68% confidence).
     * @hide
     */
    @TestApi
    public void resetElapsedRealtimeUncertaintyNanos() {
        resetFlag(HAS_ELAPSED_REALTIME_UNCERTAINTY_NANOS);
    }

    /**
     * Returns {@code true} if {@link #getReferenceConstellationTypeForIsb()} is available,
     * {@code false} otherwise.
     */
    public boolean hasReferenceConstellationTypeForIsb() {
        return isFlagSet(HAS_REFERENCE_CONSTELLATION_TYPE_FOR_ISB);
    }

    /**
     * Returns the reference constellation type for inter-signal bias.
     *
     * <p>The value is only available if {@link #hasReferenceConstellationTypeForIsb()} is
     * {@code true}.
     *
     * <p>The return value is one of those constants with {@code CONSTELLATION_} prefix in
     * {@link GnssStatus}.
     */
    @GnssStatus.ConstellationType
    public int getReferenceConstellationTypeForIsb() {
        return mReferenceConstellationTypeForIsb;
    }

    /**
     * Sets the reference constellation type for inter-signal bias.
     * @hide
     */
    @TestApi
    public void setReferenceConstellationTypeForIsb(@GnssStatus.ConstellationType int value) {
        setFlag(HAS_REFERENCE_CONSTELLATION_TYPE_FOR_ISB);
        mReferenceConstellationTypeForIsb = value;
    }

    /**
     * Resets the reference constellation type for inter-signal bias.
     * @hide
     */
    @TestApi
    public void resetReferenceConstellationTypeForIsb() {
        resetFlag(HAS_REFERENCE_CONSTELLATION_TYPE_FOR_ISB);
        mReferenceConstellationTypeForIsb = GnssStatus.CONSTELLATION_UNKNOWN;
    }

    /**
     * Returns {@code true} if {@link #getReferenceCarrierFrequencyHzForIsb()} is available, {@code
     * false} otherwise.
     */
    public boolean hasReferenceCarrierFrequencyHzForIsb() {
        return isFlagSet(HAS_REFERENCE_CARRIER_FREQUENCY_FOR_ISB);
    }

    /**
     * Returns the reference carrier frequency in Hz for inter-signal bias.
     *
     * <p>The value is only available if {@link #hasReferenceCarrierFrequencyHzForIsb()} is
     * {@code true}.
     */
    @FloatRange(from = 0.0)
    public double getReferenceCarrierFrequencyHzForIsb() {
        return mReferenceCarrierFrequencyHzForIsb;
    }

    /**
     * Sets the reference carrier frequency in Hz for inter-signal bias.
     * @hide
     */
    @TestApi
    public void setReferenceCarrierFrequencyHzForIsb(@FloatRange(from = 0.0) double value) {
        setFlag(HAS_REFERENCE_CARRIER_FREQUENCY_FOR_ISB);
        mReferenceCarrierFrequencyHzForIsb = value;
    }

    /**
     * Resets the reference carrier frequency in Hz for inter-signal bias.
     * @hide
     */
    @TestApi
    public void resetReferenceCarrierFrequencyHzForIsb() {
        resetFlag(HAS_REFERENCE_CARRIER_FREQUENCY_FOR_ISB);
    }

    /**
     * Returns {@code true} if {@link #getReferenceCodeTypeForIsb()} is available, {@code
     * false} otherwise.
     */
    public boolean hasReferenceCodeTypeForIsb() {
        return isFlagSet(HAS_REFERENCE_CODE_TYPE_FOR_ISB);
    }

    /**
     * Returns the reference code type for inter-signal bias.
     *
     * <p>The value is only available if {@link #hasReferenceCodeTypeForIsb()} is
     * {@code true}.
     *
     * <p>The return value is one of those constants defined in
     * {@link GnssMeasurement#getCodeType()}.
     */
    @NonNull
    public String getReferenceCodeTypeForIsb() {
        return mReferenceCodeTypeForIsb;
    }

    /**
     * Sets the reference code type for inter-signal bias.
     * @hide
     */
    @TestApi
    public void setReferenceCodeTypeForIsb(@NonNull String codeType) {
        setFlag(HAS_REFERENCE_CODE_TYPE_FOR_ISB);
        mReferenceCodeTypeForIsb = codeType;
    }

    /**
     * Resets the reference code type for inter-signal bias.
     * @hide
     */
    @TestApi
    public void resetReferenceCodeTypeForIsb() {
        resetFlag(HAS_REFERENCE_CODE_TYPE_FOR_ISB);
        mReferenceCodeTypeForIsb = "UNKNOWN";
    }

    /**
     * Gets count of hardware clock discontinuities.
     *
     * <p>When this value stays the same, vs. a value in a previously reported {@link GnssClock}, it
     * can be safely assumed that the {@code TimeNanos} value has been derived from a clock that has
     * been running continuously - e.g. a single continuously powered crystal oscillator, and thus
     * the {@code (FullBiasNanos + BiasNanos)} offset can be modelled with traditional clock bias
     * &amp; drift models.
     *
     * <p>Each time this value changes, vs. the value in a previously reported {@link GnssClock},
     * that suggests the hardware clock may have experienced a discontinuity (e.g. a power cycle or
     * other anomaly), so that any assumptions about modelling a smoothly changing
     * {@code (FullBiasNanos + BiasNanos)} offset, and a smoothly growing {@code (TimeNanos)}
     * between this and the previously reported {@code GnssClock}, should be reset.
     */
    public int getHardwareClockDiscontinuityCount() {
        return mHardwareClockDiscontinuityCount;
    }

    /**
     * Sets count of last hardware clock discontinuity.
     * @hide
     */
    @TestApi
    public void setHardwareClockDiscontinuityCount(int value) {
        mHardwareClockDiscontinuityCount = value;
    }

    public static final @android.annotation.NonNull Creator<GnssClock> CREATOR = new Creator<GnssClock>() {
        @Override
        public GnssClock createFromParcel(Parcel parcel) {
            GnssClock gpsClock = new GnssClock();

            gpsClock.mFlags = parcel.readInt();
            gpsClock.mLeapSecond = parcel.readInt();
            gpsClock.mTimeNanos = parcel.readLong();
            gpsClock.mTimeUncertaintyNanos = parcel.readDouble();
            gpsClock.mFullBiasNanos = parcel.readLong();
            gpsClock.mBiasNanos = parcel.readDouble();
            gpsClock.mBiasUncertaintyNanos = parcel.readDouble();
            gpsClock.mDriftNanosPerSecond = parcel.readDouble();
            gpsClock.mDriftUncertaintyNanosPerSecond = parcel.readDouble();
            gpsClock.mHardwareClockDiscontinuityCount = parcel.readInt();
            gpsClock.mElapsedRealtimeNanos = parcel.readLong();
            gpsClock.mElapsedRealtimeUncertaintyNanos = parcel.readDouble();
            gpsClock.mReferenceConstellationTypeForIsb = parcel.readInt();
            gpsClock.mReferenceCarrierFrequencyHzForIsb = parcel.readDouble();
            gpsClock.mReferenceCodeTypeForIsb = parcel.readString();

            return gpsClock;
        }

        @Override
        public GnssClock[] newArray(int size) {
            return new GnssClock[size];
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mFlags);
        parcel.writeInt(mLeapSecond);
        parcel.writeLong(mTimeNanos);
        parcel.writeDouble(mTimeUncertaintyNanos);
        parcel.writeLong(mFullBiasNanos);
        parcel.writeDouble(mBiasNanos);
        parcel.writeDouble(mBiasUncertaintyNanos);
        parcel.writeDouble(mDriftNanosPerSecond);
        parcel.writeDouble(mDriftUncertaintyNanosPerSecond);
        parcel.writeInt(mHardwareClockDiscontinuityCount);
        parcel.writeLong(mElapsedRealtimeNanos);
        parcel.writeDouble(mElapsedRealtimeUncertaintyNanos);
        parcel.writeInt(mReferenceConstellationTypeForIsb);
        parcel.writeDouble(mReferenceCarrierFrequencyHzForIsb);
        parcel.writeString(mReferenceCodeTypeForIsb);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        final String format = "   %-15s = %s\n";
        final String formatWithUncertainty = "   %-15s = %-25s   %-26s = %s\n";
        StringBuilder builder = new StringBuilder("GnssClock:\n");

        if (hasLeapSecond()) {
            builder.append(String.format(format, "LeapSecond", mLeapSecond));
        }

        builder.append(String.format(
                formatWithUncertainty,
                "TimeNanos",
                mTimeNanos,
                "TimeUncertaintyNanos",
                hasTimeUncertaintyNanos() ? mTimeUncertaintyNanos : null));

        if (hasFullBiasNanos()) {
            builder.append(String.format(format, "FullBiasNanos", mFullBiasNanos));
        }

        if (hasBiasNanos() || hasBiasUncertaintyNanos()) {
            builder.append(String.format(
                    formatWithUncertainty,
                    "BiasNanos",
                    hasBiasNanos() ? mBiasNanos : null,
                    "BiasUncertaintyNanos",
                    hasBiasUncertaintyNanos() ? mBiasUncertaintyNanos : null));
        }

        if (hasDriftNanosPerSecond() || hasDriftUncertaintyNanosPerSecond()) {
            builder.append(String.format(
                    formatWithUncertainty,
                    "DriftNanosPerSecond",
                    hasDriftNanosPerSecond() ? mDriftNanosPerSecond : null,
                    "DriftUncertaintyNanosPerSecond",
                    hasDriftUncertaintyNanosPerSecond() ? mDriftUncertaintyNanosPerSecond : null));
        }

        builder.append(String.format(
                format,
                "HardwareClockDiscontinuityCount",
                mHardwareClockDiscontinuityCount));

        if (hasElapsedRealtimeNanos() || hasElapsedRealtimeUncertaintyNanos()) {
            builder.append(String.format(
                    formatWithUncertainty,
                    "ElapsedRealtimeNanos",
                    hasElapsedRealtimeNanos() ? mElapsedRealtimeNanos : null,
                    "ElapsedRealtimeUncertaintyNanos",
                    hasElapsedRealtimeUncertaintyNanos() ? mElapsedRealtimeUncertaintyNanos
                            : null));
        }

        if (hasReferenceConstellationTypeForIsb()) {
            builder.append(String.format(format, "ReferenceConstellationTypeForIsb",
                    mReferenceConstellationTypeForIsb));
        }

        if (hasReferenceCarrierFrequencyHzForIsb()) {
            builder.append(String.format(format, "ReferenceCarrierFrequencyHzForIsb",
                    mReferenceCarrierFrequencyHzForIsb));
        }

        if (hasReferenceCodeTypeForIsb()) {
            builder.append(
                    String.format(format, "ReferenceCodeTypeForIsb", mReferenceCodeTypeForIsb));
        }

        return builder.toString();
    }

    private void initialize() {
        mFlags = HAS_NO_FLAGS;
        resetLeapSecond();
        setTimeNanos(Long.MIN_VALUE);
        resetTimeUncertaintyNanos();
        resetFullBiasNanos();
        resetBiasNanos();
        resetBiasUncertaintyNanos();
        resetDriftNanosPerSecond();
        resetDriftUncertaintyNanosPerSecond();
        setHardwareClockDiscontinuityCount(Integer.MIN_VALUE);
        resetElapsedRealtimeNanos();
        resetElapsedRealtimeUncertaintyNanos();
        resetReferenceConstellationTypeForIsb();
        resetReferenceCarrierFrequencyHzForIsb();
        resetReferenceCodeTypeForIsb();
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
