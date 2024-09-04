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
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;

/**
 * Data interface for managing a Thread Pending Operational Dataset.
 *
 * <p>The Pending Operational Dataset represents an Operational Dataset which will become Active in
 * a given delay. This is typically used to deploy new network parameters (e.g. Network Key or
 * Channel) to all devices in the network.
 *
 * @see ThreadNetworkController#scheduleMigration
 * @hide
 */
@FlaggedApi(ThreadNetworkFlags.FLAG_THREAD_ENABLED)
@SystemApi
public final class PendingOperationalDataset implements Parcelable {
    // Value defined in Thread spec 8.10.1.16
    private static final int TYPE_PENDING_TIMESTAMP = 51;

    // Values defined in Thread spec 8.10.1.17
    private static final int TYPE_DELAY_TIMER = 52;
    private static final int LENGTH_DELAY_TIMER_BYTES = 4;

    @NonNull
    public static final Creator<PendingOperationalDataset> CREATOR =
            new Creator<>() {
                @Override
                public PendingOperationalDataset createFromParcel(Parcel in) {
                    return PendingOperationalDataset.fromThreadTlvs(in.createByteArray());
                }

                @Override
                public PendingOperationalDataset[] newArray(int size) {
                    return new PendingOperationalDataset[size];
                }
            };

    @NonNull private final ActiveOperationalDataset mActiveOpDataset;
    @NonNull private final OperationalDatasetTimestamp mPendingTimestamp;
    @NonNull private final Duration mDelayTimer;

    /**
     * Creates a new {@link PendingOperationalDataset} object.
     *
     * @param activeOpDataset the included Active Operational Dataset
     * @param pendingTimestamp the Pending Timestamp which represents the version of this Pending
     *     Dataset
     * @param delayTimer the delay after when {@code activeOpDataset} will be committed on this
     *     device; use {@link Duration#ZERO} to tell the system to choose a reasonable value
     *     automatically
     */
    public PendingOperationalDataset(
            @NonNull ActiveOperationalDataset activeOpDataset,
            @NonNull OperationalDatasetTimestamp pendingTimestamp,
            @NonNull Duration delayTimer) {
        requireNonNull(activeOpDataset, "activeOpDataset cannot be null");
        requireNonNull(pendingTimestamp, "pendingTimestamp cannot be null");
        requireNonNull(delayTimer, "delayTimer cannot be null");
        this.mActiveOpDataset = activeOpDataset;
        this.mPendingTimestamp = pendingTimestamp;
        this.mDelayTimer = delayTimer;
    }

    /**
     * Creates a new {@link PendingOperationalDataset} object from a series of Thread TLVs.
     *
     * <p>{@code tlvs} can be obtained from the value of a Thread Pending Operational Dataset TLV
     * (see the <a href="https://www.threadgroup.org/support#specifications">Thread
     * specification</a> for the definition) or the return value of {@link #toThreadTlvs}.
     *
     * @throws IllegalArgumentException if {@code tlvs} is malformed or contains an invalid Thread
     *     TLV
     */
    @NonNull
    public static PendingOperationalDataset fromThreadTlvs(@NonNull byte[] tlvs) {
        requireNonNull(tlvs, "tlvs cannot be null");

        SparseArray<byte[]> newUnknownTlvs = new SparseArray<>();
        OperationalDatasetTimestamp pendingTimestamp = null;
        Duration delayTimer = null;
        ActiveOperationalDataset activeDataset = ActiveOperationalDataset.fromThreadTlvs(tlvs);
        SparseArray<byte[]> unknownTlvs = activeDataset.getUnknownTlvs();
        for (int i = 0; i < unknownTlvs.size(); i++) {
            int key = unknownTlvs.keyAt(i);
            byte[] value = unknownTlvs.valueAt(i);
            switch (key) {
                case TYPE_PENDING_TIMESTAMP:
                    pendingTimestamp = OperationalDatasetTimestamp.fromTlvValue(value);
                    break;
                case TYPE_DELAY_TIMER:
                    checkArgument(
                            value.length == LENGTH_DELAY_TIMER_BYTES,
                            "Invalid delay timer (length = %d, expectedLength = %d)",
                            value.length,
                            LENGTH_DELAY_TIMER_BYTES);
                    int millis = ByteBuffer.wrap(value).getInt();
                    delayTimer = Duration.ofMillis(Integer.toUnsignedLong(millis));
                    break;
                default:
                    newUnknownTlvs.put(key, value);
                    break;
            }
        }

        if (pendingTimestamp == null) {
            throw new IllegalArgumentException("Pending Timestamp is missing");
        }
        if (delayTimer == null) {
            throw new IllegalArgumentException("Delay Timer is missing");
        }

        activeDataset =
                new ActiveOperationalDataset.Builder(activeDataset)
                        .setUnknownTlvs(newUnknownTlvs)
                        .build();
        return new PendingOperationalDataset(activeDataset, pendingTimestamp, delayTimer);
    }

    /** Returns the Active Operational Dataset. */
    @NonNull
    public ActiveOperationalDataset getActiveOperationalDataset() {
        return mActiveOpDataset;
    }

    /** Returns the Pending Timestamp. */
    @NonNull
    public OperationalDatasetTimestamp getPendingTimestamp() {
        return mPendingTimestamp;
    }

    /** Returns the Delay Timer. */
    @NonNull
    public Duration getDelayTimer() {
        return mDelayTimer;
    }

    /**
     * Converts this {@link PendingOperationalDataset} object to a series of Thread TLVs.
     *
     * <p>See the <a href="https://www.threadgroup.org/support#specifications">Thread
     * specification</a> for the definition of the Thread TLV format.
     */
    @NonNull
    public byte[] toThreadTlvs() {
        ByteArrayOutputStream dataset = new ByteArrayOutputStream();

        byte[] activeDatasetBytes = mActiveOpDataset.toThreadTlvs();
        dataset.write(activeDatasetBytes, 0, activeDatasetBytes.length);

        dataset.write(TYPE_PENDING_TIMESTAMP);
        byte[] pendingTimestampBytes = mPendingTimestamp.toTlvValue();
        dataset.write(pendingTimestampBytes.length);
        dataset.write(pendingTimestampBytes, 0, pendingTimestampBytes.length);

        dataset.write(TYPE_DELAY_TIMER);
        byte[] delayTimerBytes = new byte[LENGTH_DELAY_TIMER_BYTES];
        ByteBuffer.wrap(delayTimerBytes).putInt((int) mDelayTimer.toMillis());
        dataset.write(delayTimerBytes.length);
        dataset.write(delayTimerBytes, 0, delayTimerBytes.length);

        return dataset.toByteArray();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof PendingOperationalDataset)) {
            return false;
        } else {
            PendingOperationalDataset otherDataset = (PendingOperationalDataset) other;
            return mActiveOpDataset.equals(otherDataset.mActiveOpDataset)
                    && mPendingTimestamp.equals(otherDataset.mPendingTimestamp)
                    && mDelayTimer.equals(otherDataset.mDelayTimer);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(mActiveOpDataset, mPendingTimestamp, mDelayTimer);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{activeDataset=")
                .append(getActiveOperationalDataset())
                .append(", pendingTimestamp=")
                .append(getPendingTimestamp())
                .append(", delayTimer=")
                .append(getDelayTimer())
                .append("}");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByteArray(toThreadTlvs());
    }
}
