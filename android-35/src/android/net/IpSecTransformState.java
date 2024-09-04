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
package android.net;

import static android.net.IpSecManager.Flags.IPSEC_TRANSFORM_STATE;

import static com.android.internal.annotations.VisibleForTesting.Visibility;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import com.android.internal.annotations.VisibleForTesting;
import com.android.net.module.util.HexDump;

import java.util.Objects;

/**
 * This class represents a snapshot of the state of an IpSecTransform
 *
 * <p>This class provides the current state of an IpSecTransform, enabling link metric analysis by
 * the caller. Use cases include understanding transform usage, such as packet and byte counts, as
 * well as observing out-of-order delivery by checking the bitmap. Additionally, callers can query
 * IpSecTransformStates at two timestamps. By comparing the changes in packet counts and sequence
 * numbers, callers can estimate IPsec data loss in the inbound direction.
 */
@FlaggedApi(IPSEC_TRANSFORM_STATE)
public final class IpSecTransformState implements Parcelable {
    private final long mTimestamp;
    private final long mTxHighestSequenceNumber;
    private final long mRxHighestSequenceNumber;
    private final long mPacketCount;
    private final long mByteCount;
    private final byte[] mReplayBitmap;

    private IpSecTransformState(
            long timestamp,
            long txHighestSequenceNumber,
            long rxHighestSequenceNumber,
            long packetCount,
            long byteCount,
            byte[] replayBitmap) {
        mTimestamp = timestamp;
        mTxHighestSequenceNumber = txHighestSequenceNumber;
        mRxHighestSequenceNumber = rxHighestSequenceNumber;
        mPacketCount = packetCount;
        mByteCount = byteCount;

        Objects.requireNonNull(replayBitmap, "replayBitmap is null");
        mReplayBitmap = replayBitmap.clone();

        validate();
    }

    private void validate() {
        Objects.requireNonNull(mReplayBitmap, "mReplayBitmap is null");
    }

    /**
     * Deserializes a IpSecTransformState from a PersistableBundle.
     *
     * @hide
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public IpSecTransformState(@NonNull Parcel in) {
        Objects.requireNonNull(in, "The input PersistableBundle is null");
        mTimestamp = in.readLong();
        mTxHighestSequenceNumber = in.readLong();
        mRxHighestSequenceNumber = in.readLong();
        mPacketCount = in.readLong();
        mByteCount = in.readLong();
        mReplayBitmap = HexDump.hexStringToByteArray(in.readString());

        validate();
    }

    // Parcelable methods

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeLong(mTimestamp);
        out.writeLong(mTxHighestSequenceNumber);
        out.writeLong(mRxHighestSequenceNumber);
        out.writeLong(mPacketCount);
        out.writeLong(mByteCount);
        out.writeString(HexDump.toHexString(mReplayBitmap));
    }

    @NonNull
    public static final Parcelable.Creator<IpSecTransformState> CREATOR =
            new Parcelable.Creator<IpSecTransformState>() {
                @NonNull
                public IpSecTransformState createFromParcel(Parcel in) {
                    return new IpSecTransformState(in);
                }

                @NonNull
                public IpSecTransformState[] newArray(int size) {
                    return new IpSecTransformState[size];
                }
            };

    /**
     * Retrieve the timestamp (milliseconds) when this state was created, as per {@link
     * SystemClock#elapsedRealtime}
     *
     * @see Builder#setTimestampMillis(long)
     */
    public long getTimestampMillis() {
        return mTimestamp;
    }

    /**
     * Retrieve the highest sequence number sent so far as an unsigned long
     *
     * @see Builder#setTxHighestSequenceNumber(long)
     */
    public long getTxHighestSequenceNumber() {
        return mTxHighestSequenceNumber;
    }

    /**
     * Retrieve the highest sequence number received so far as an unsigned long
     *
     * @see Builder#setRxHighestSequenceNumber(long)
     */
    public long getRxHighestSequenceNumber() {
        return mRxHighestSequenceNumber;
    }

    /**
     * Retrieve the number of packets processed so far as an unsigned long.
     *
     * <p>The packet count direction (inbound or outbound) aligns with the direction in which the
     * IpSecTransform is applied to.
     *
     * @see Builder#setPacketCount(long)
     */
    public long getPacketCount() {
        return mPacketCount;
    }

    /**
     * Retrieve the number of bytes processed so far as an unsigned long
     *
     * <p>The byte count direction (inbound or outbound) aligns with the direction in which the
     * IpSecTransform is applied to.
     *
     * @see Builder#setByteCount(long)
     */
    public long getByteCount() {
        return mByteCount;
    }

    /**
     * Retrieve the replay bitmap
     *
     * <p>This bitmap represents a replay window, allowing the caller to observe out-of-order
     * delivery. The last bit represents the highest sequence number received so far and bits for
     * the received packets will be marked as true.
     *
     * <p>The size of a replay bitmap will never change over the lifetime of an IpSecTransform
     *
     * <p>The replay bitmap is solely useful for inbound IpSecTransforms. For outbound
     * IpSecTransforms, all bits will be unchecked.
     *
     * @see Builder#setReplayBitmap(byte[])
     */
    @NonNull
    public byte[] getReplayBitmap() {
        return mReplayBitmap.clone();
    }

    /**
     * Builder class for testing purposes
     *
     * <p>Except for testing, IPsec callers normally do not instantiate {@link IpSecTransformState}
     * themselves but instead get a reference via {@link IpSecTransformState}
     */
    @FlaggedApi(IPSEC_TRANSFORM_STATE)
    public static final class Builder {
        private long mTimestamp;
        private long mTxHighestSequenceNumber;
        private long mRxHighestSequenceNumber;
        private long mPacketCount;
        private long mByteCount;
        private byte[] mReplayBitmap;

        public Builder() {
            mTimestamp = SystemClock.elapsedRealtime();
        }

        /**
         * Set the timestamp (milliseconds) when this state was created
         *
         * @see IpSecTransformState#getTimestampMillis()
         */
        @NonNull
        public Builder setTimestampMillis(long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        /**
         * Set the highest sequence number sent so far as an unsigned long
         *
         * @see IpSecTransformState#getTxHighestSequenceNumber()
         */
        @NonNull
        public Builder setTxHighestSequenceNumber(long seqNum) {
            mTxHighestSequenceNumber = seqNum;
            return this;
        }

        /**
         * Set the highest sequence number received so far as an unsigned long
         *
         * @see IpSecTransformState#getRxHighestSequenceNumber()
         */
        @NonNull
        public Builder setRxHighestSequenceNumber(long seqNum) {
            mRxHighestSequenceNumber = seqNum;
            return this;
        }

        /**
         * Set the number of packets processed so far as an unsigned long
         *
         * @see IpSecTransformState#getPacketCount()
         */
        @NonNull
        public Builder setPacketCount(long packetCount) {
            mPacketCount = packetCount;
            return this;
        }

        /**
         * Set the number of bytes processed so far as an unsigned long
         *
         * @see IpSecTransformState#getByteCount()
         */
        @NonNull
        public Builder setByteCount(long byteCount) {
            mByteCount = byteCount;
            return this;
        }

        /**
         * Set the replay bitmap
         *
         * @see IpSecTransformState#getReplayBitmap()
         */
        @NonNull
        public Builder setReplayBitmap(@NonNull byte[] bitMap) {
            mReplayBitmap = bitMap.clone();
            return this;
        }

        /**
         * Build and validate the IpSecTransformState
         *
         * @return an immutable IpSecTransformState instance
         */
        @NonNull
        public IpSecTransformState build() {
            return new IpSecTransformState(
                    mTimestamp,
                    mTxHighestSequenceNumber,
                    mRxHighestSequenceNumber,
                    mPacketCount,
                    mByteCount,
                    mReplayBitmap);
        }
    }
}
