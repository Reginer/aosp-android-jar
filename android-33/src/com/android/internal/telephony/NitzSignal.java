/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.internal.telephony;

import android.annotation.DurationMillisLong;
import android.annotation.ElapsedRealtimeLong;
import android.annotation.NonNull;
import android.os.TimestampedValue;

import java.time.Duration;
import java.util.Objects;

/** NITZ information and associated metadata. */
public final class NitzSignal {

    @ElapsedRealtimeLong private final long mReceiptElapsedMillis;
    @NonNull private final NitzData mNitzData;
    @DurationMillisLong private final long mAgeMillis;

    /**
     * @param receiptElapsedMillis the time according to {@link
     *     android.os.SystemClock#elapsedRealtime()} when the NITZ signal was first received by
     *     the platform code
     * @param nitzData the NITZ data
     * @param ageMillis the age of the NITZ when it was passed to the platform, e.g. if it was
     *     cached by the modem for a period of time. Must not be negative.
     */
    public NitzSignal(
            @ElapsedRealtimeLong long receiptElapsedMillis,
            @NonNull NitzData nitzData,
            long ageMillis) {
        mReceiptElapsedMillis = receiptElapsedMillis;
        mNitzData = Objects.requireNonNull(nitzData);
        if (ageMillis < 0) {
            throw new IllegalArgumentException("ageMillis < 0");
        }
        mAgeMillis = ageMillis;
    }

    /**
     * Returns the time according to {@link android.os.SystemClock#elapsedRealtime()} when the NITZ
     * signal was first received by the platform code.
     */
    @ElapsedRealtimeLong
    public long getReceiptElapsedRealtimeMillis() {
        return mReceiptElapsedMillis;
    }

    /**
     * Returns the NITZ data.
     */
    @NonNull
    public NitzData getNitzData() {
        return mNitzData;
    }

    /**
     * Returns the age of the NITZ when it was passed to the platform, e.g. if it was cached by the
     * modem for a period of time. Must not be negative.
     */
    @DurationMillisLong
    public long getAgeMillis() {
        return mAgeMillis;
    }

    /**
     * Returns a derived property of {@code receiptElapsedMillis - ageMillis}, i.e. the time
     * according to the elapsed realtime clock when the NITZ signal was actually received by this
     * device taking into time it was cached by layers before the RIL.
     */
    @ElapsedRealtimeLong
    public long getAgeAdjustedElapsedRealtimeMillis() {
        return mReceiptElapsedMillis - mAgeMillis;
    }

    /**
     * Creates a {@link android.os.TimestampedValue} containing the UTC time as the number of
     * milliseconds since the start of the Unix epoch. The reference time is the time according to
     * the elapsed realtime clock when that would have been the time, accounting for receipt time
     * and age.
     */
    public TimestampedValue<Long> createTimeSignal() {
        return new TimestampedValue<>(
                getAgeAdjustedElapsedRealtimeMillis(),
                getNitzData().getCurrentTimeInMillis());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NitzSignal that = (NitzSignal) o;
        return mReceiptElapsedMillis == that.mReceiptElapsedMillis
                && mAgeMillis == that.mAgeMillis
                && mNitzData.equals(that.mNitzData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mReceiptElapsedMillis, mNitzData, mAgeMillis);
    }

    @Override
    public String toString() {
        return "NitzSignal{"
                + "mReceiptElapsedMillis=" + Duration.ofMillis(mReceiptElapsedMillis)
                + ", mNitzData=" + mNitzData
                + ", mAgeMillis=" + mAgeMillis
                + '}';
    }
}
