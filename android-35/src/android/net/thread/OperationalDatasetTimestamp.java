/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.net.thread;

import static com.android.internal.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Objects;

/**
 * The timestamp of Thread Operational Dataset.
 *
 * @see ActiveOperationalDataset
 * @see PendingOperationalDataset
 * @hide
 */
@FlaggedApi(ThreadNetworkFlags.FLAG_THREAD_ENABLED)
@SystemApi
public final class OperationalDatasetTimestamp {
    /** @hide */
    public static final int LENGTH_TIMESTAMP = Long.BYTES;

    private static final int TICKS_UPPER_BOUND = 0x8000;

    private final long mSeconds;
    private final int mTicks;
    private final boolean mIsAuthoritativeSource;

    /**
     * Creates a new {@link OperationalDatasetTimestamp} object from an {@link Instant}.
     *
     * <p>The {@code seconds} is set to {@code instant.getEpochSecond()}, {@code ticks} is set to
     * {@link instant#getNano()} based on frequency of 32768 Hz, and {@code isAuthoritativeSource}
     * is set to {@code true}.
     *
     * <p>Note that this conversion can lose precision and a value returned by {@link #toInstant}
     * may not equal exactly the {@code instant}.
     *
     * @throws IllegalArgumentException if {@code instant.getEpochSecond()} is larger than {@code
     *     0xffffffffffffL}
     * @see toInstant
     */
    @NonNull
    public static OperationalDatasetTimestamp fromInstant(@NonNull Instant instant) {
        int ticks = getRoundedTicks(instant.getNano());
        long seconds = instant.getEpochSecond() + ticks / TICKS_UPPER_BOUND;
        // the rounded ticks can be 0x8000 if instant.getNano() >= 999984742
        ticks = ticks % TICKS_UPPER_BOUND;
        return new OperationalDatasetTimestamp(seconds, ticks, true /* isAuthoritativeSource */);
    }

    /**
     * Converts this {@link OperationalDatasetTimestamp} object to an {@link Instant}.
     *
     * <p>Note that the return value may not equal exactly the {@code instant} if this object is
     * created with {@link #fromInstant}.
     *
     * @see fromInstant
     */
    @NonNull
    public Instant toInstant() {
        long nanos = Math.round((double) mTicks * 1000000000L / TICKS_UPPER_BOUND);
        return Instant.ofEpochSecond(mSeconds, nanos);
    }

    /**
     * Creates a new {@link OperationalDatasetTimestamp} object from the OperationalDatasetTimestamp
     * TLV value.
     *
     * @hide
     */
    @NonNull
    public static OperationalDatasetTimestamp fromTlvValue(@NonNull byte[] encodedTimestamp) {
        requireNonNull(encodedTimestamp, "encodedTimestamp cannot be null");
        checkArgument(
                encodedTimestamp.length == LENGTH_TIMESTAMP,
                "Invalid Thread OperationalDatasetTimestamp length (length = %d,"
                        + " expectedLength=%d)",
                encodedTimestamp.length,
                LENGTH_TIMESTAMP);
        long longTimestamp = ByteBuffer.wrap(encodedTimestamp).getLong();
        return new OperationalDatasetTimestamp(
                (longTimestamp >> 16) & 0x0000ffffffffffffL,
                (int) ((longTimestamp >> 1) & 0x7fffL),
                (longTimestamp & 0x01) != 0);
    }

    /**
     * Converts this {@link OperationalDatasetTimestamp} object to Thread TLV value.
     *
     * @hide
     */
    @NonNull
    public byte[] toTlvValue() {
        byte[] tlv = new byte[LENGTH_TIMESTAMP];
        ByteBuffer buffer = ByteBuffer.wrap(tlv);
        long encodedValue = (mSeconds << 16) | (mTicks << 1) | (mIsAuthoritativeSource ? 1 : 0);
        buffer.putLong(encodedValue);
        return tlv;
    }

    /**
     * Creates a new {@link OperationalDatasetTimestamp} object.
     *
     * @param seconds the value encodes a Unix Time value. Must be in the range of
     *     0x0-0xffffffffffffL
     * @param ticks the value encodes the fractional Unix Time value in 32.768 kHz resolution. Must
     *     be in the range of 0x0-0x7fff
     * @param isAuthoritativeSource the flag indicates the time was obtained from an authoritative
     *     source: either NTP (Network Time Protocol), GPS (Global Positioning System), cell
     *     network, or other method
     * @throws IllegalArgumentException if the {@code seconds} is not in range of
     *     0x0-0xffffffffffffL or {@code ticks} is not in range of 0x0-0x7fff
     */
    public OperationalDatasetTimestamp(
            @IntRange(from = 0x0, to = 0xffffffffffffL) long seconds,
            @IntRange(from = 0x0, to = 0x7fff) int ticks,
            boolean isAuthoritativeSource) {
        checkArgument(
                seconds >= 0 && seconds <= 0xffffffffffffL,
                "seconds exceeds allowed range (seconds = %d,"
                        + " allowedRange = [0x0, 0xffffffffffffL])",
                seconds);
        checkArgument(
                ticks >= 0 && ticks <= 0x7fff,
                "ticks exceeds allowed ranged (ticks = %d, allowedRange" + " = [0x0, 0x7fff])",
                ticks);
        mSeconds = seconds;
        mTicks = ticks;
        mIsAuthoritativeSource = isAuthoritativeSource;
    }

    /**
     * Returns the rounded ticks converted from the nano seconds.
     *
     * <p>Note that rhe return value can be as large as {@code TICKS_UPPER_BOUND}.
     */
    private static int getRoundedTicks(long nanos) {
        return (int) Math.round((double) nanos * TICKS_UPPER_BOUND / 1000000000L);
    }

    /** Returns the seconds portion of the timestamp. */
    public @IntRange(from = 0x0, to = 0xffffffffffffL) long getSeconds() {
        return mSeconds;
    }

    /** Returns the ticks portion of the timestamp. */
    public @IntRange(from = 0x0, to = 0x7fff) int getTicks() {
        return mTicks;
    }

    /** Returns {@code true} if the timestamp comes from an authoritative source. */
    public boolean isAuthoritativeSource() {
        return mIsAuthoritativeSource;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{seconds=")
                .append(getSeconds())
                .append(", ticks=")
                .append(getTicks())
                .append(", isAuthoritativeSource=")
                .append(isAuthoritativeSource())
                .append(", instant=")
                .append(toInstant())
                .append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof OperationalDatasetTimestamp)) {
            return false;
        } else {
            OperationalDatasetTimestamp otherTimestamp = (OperationalDatasetTimestamp) other;
            return mSeconds == otherTimestamp.mSeconds
                    && mTicks == otherTimestamp.mTicks
                    && mIsAuthoritativeSource == otherTimestamp.mIsAuthoritativeSource;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSeconds, mTicks, mIsAuthoritativeSource);
    }
}
