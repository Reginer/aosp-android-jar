/*
 * Copyright (C) 2011 The Android Open Source Project
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

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;
import static android.net.NetworkStats.DEFAULT_NETWORK_NO;
import static android.net.NetworkStats.IFACE_ALL;
import static android.net.NetworkStats.METERED_NO;
import static android.net.NetworkStats.ROAMING_NO;
import static android.net.NetworkStats.SET_DEFAULT;
import static android.net.NetworkStats.TAG_NONE;
import static android.net.NetworkStats.UID_ALL;
import static android.net.NetworkStatsHistory.DataStreamUtils.readFullLongArray;
import static android.net.NetworkStatsHistory.DataStreamUtils.readVarLongArray;
import static android.net.NetworkStatsHistory.DataStreamUtils.writeVarLongArray;
import static android.net.NetworkStatsHistory.Entry.UNKNOWN;
import static android.net.NetworkStatsHistory.ParcelUtils.readLongArray;
import static android.net.NetworkStatsHistory.ParcelUtils.writeLongArray;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import static com.android.net.module.util.NetworkStatsUtils.multiplySafeByRational;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.NetworkStatsHistoryBucketProto;
import android.service.NetworkStatsHistoryProto;
import android.util.IndentingPrintWriter;
import android.util.proto.ProtoOutputStream;

import com.android.net.module.util.CollectionUtils;
import com.android.net.module.util.NetworkStatsUtils;

import libcore.util.EmptyArray;

import java.io.CharArrayWriter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/**
 * Collection of historical network statistics, recorded into equally-sized
 * "buckets" in time. Internally it stores data in {@code long} series for more
 * efficient persistence.
 * <p>
 * Each bucket is defined by a {@link #bucketStart} timestamp, and lasts for
 * {@link #bucketDuration}. Internally assumes that {@link #bucketStart} is
 * sorted at all times.
 *
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
public final class NetworkStatsHistory implements Parcelable {
    private static final int VERSION_INIT = 1;
    private static final int VERSION_ADD_PACKETS = 2;
    private static final int VERSION_ADD_ACTIVE = 3;

    /** @hide */
    public static final int FIELD_ACTIVE_TIME = 0x01;
    /** @hide */
    public static final int FIELD_RX_BYTES = 0x02;
    /** @hide */
    public static final int FIELD_RX_PACKETS = 0x04;
    /** @hide */
    public static final int FIELD_TX_BYTES = 0x08;
    /** @hide */
    public static final int FIELD_TX_PACKETS = 0x10;
    /** @hide */
    public static final int FIELD_OPERATIONS = 0x20;
    /** @hide */
    public static final int FIELD_ALL = 0xFFFFFFFF;

    private long bucketDuration;
    private int bucketCount;
    private long[] bucketStart;
    private long[] activeTime;
    private long[] rxBytes;
    private long[] rxPackets;
    private long[] txBytes;
    private long[] txPackets;
    private long[] operations;
    private long totalBytes;

    /** @hide */
    public NetworkStatsHistory(long bucketDuration, long[] bucketStart, long[] activeTime,
            long[] rxBytes, long[] rxPackets, long[] txBytes, long[] txPackets,
            long[] operations, int bucketCount, long totalBytes) {
        this.bucketDuration = bucketDuration;
        this.bucketStart = bucketStart;
        this.activeTime = activeTime;
        this.rxBytes = rxBytes;
        this.rxPackets = rxPackets;
        this.txBytes = txBytes;
        this.txPackets = txPackets;
        this.operations = operations;
        this.bucketCount = bucketCount;
        this.totalBytes = totalBytes;
    }

    /**
     * An instance to represent a single record in a {@link NetworkStatsHistory} object.
     */
    public static final class Entry {
        /** @hide */
        public static final long UNKNOWN = -1;

        /** @hide */
        // TODO: Migrate all callers to get duration from the history object and remove this field.
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public long bucketDuration;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public long bucketStart;
        /** @hide */
        public long activeTime;
        /** @hide */
        @UnsupportedAppUsage
        public long rxBytes;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public long rxPackets;
        /** @hide */
        @UnsupportedAppUsage
        public long txBytes;
        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public long txPackets;
        /** @hide */
        public long operations;
        /** @hide */
        Entry() {}

        /**
         * Construct a {@link Entry} instance to represent a single record in a
         * {@link NetworkStatsHistory} object.
         *
         * @param bucketStart Start of period for this {@link Entry}, in milliseconds since the
         *                    Unix epoch, see {@link java.lang.System#currentTimeMillis}.
         * @param activeTime Active time for this {@link Entry}, in milliseconds.
         * @param rxBytes Number of bytes received for this {@link Entry}. Statistics should
         *                represent the contents of IP packets, including IP headers.
         * @param rxPackets Number of packets received for this {@link Entry}. Statistics should
         *                  represent the contents of IP packets, including IP headers.
         * @param txBytes Number of bytes transmitted for this {@link Entry}. Statistics should
         *                represent the contents of IP packets, including IP headers.
         * @param txPackets Number of bytes transmitted for this {@link Entry}. Statistics should
         *                  represent the contents of IP packets, including IP headers.
         * @param operations count of network operations performed for this {@link Entry}. This can
         *                   be used to derive bytes-per-operation.
         */
        public Entry(long bucketStart, long activeTime, long rxBytes,
                long rxPackets, long txBytes, long txPackets, long operations) {
            this.bucketStart = bucketStart;
            this.activeTime = activeTime;
            this.rxBytes = rxBytes;
            this.rxPackets = rxPackets;
            this.txBytes = txBytes;
            this.txPackets = txPackets;
            this.operations = operations;
        }

        /**
         * Get start timestamp of the bucket's time interval, in milliseconds since the Unix epoch.
         */
        public long getBucketStart() {
            return bucketStart;
        }

        /**
         * Get active time of the bucket's time interval, in milliseconds.
         */
        public long getActiveTime() {
            return activeTime;
        }

        /** Get number of bytes received for this {@link Entry}. */
        public long getRxBytes() {
            return rxBytes;
        }

        /** Get number of packets received for this {@link Entry}. */
        public long getRxPackets() {
            return rxPackets;
        }

        /** Get number of bytes transmitted for this {@link Entry}. */
        public long getTxBytes() {
            return txBytes;
        }

        /** Get number of packets transmitted for this {@link Entry}. */
        public long getTxPackets() {
            return txPackets;
        }

        /** Get count of network operations performed for this {@link Entry}. */
        public long getOperations() {
            return operations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o.getClass() != getClass()) return false;
            Entry entry = (Entry) o;
            return bucketStart == entry.bucketStart
                    && activeTime == entry.activeTime && rxBytes == entry.rxBytes
                    && rxPackets == entry.rxPackets && txBytes == entry.txBytes
                    && txPackets == entry.txPackets && operations == entry.operations;
        }

        @Override
        public int hashCode() {
            return (int) (bucketStart * 2
                    + activeTime * 3
                    + rxBytes * 5
                    + rxPackets * 7
                    + txBytes * 11
                    + txPackets * 13
                    + operations * 17);
        }

        @Override
        public String toString() {
            return "Entry{"
                    + "bucketStart=" + bucketStart
                    + ", activeTime=" + activeTime
                    + ", rxBytes=" + rxBytes
                    + ", rxPackets=" + rxPackets
                    + ", txBytes=" + txBytes
                    + ", txPackets=" + txPackets
                    + ", operations=" + operations
                    + "}";
        }

        /**
         * Add the given {@link Entry} with this instance and return a new {@link Entry}
         * instance as the result.
         *
         * @hide
         */
        @NonNull
        public Entry plus(@NonNull Entry another, long bucketDuration) {
            if (this.bucketStart != another.bucketStart) {
                throw new IllegalArgumentException("bucketStart " + this.bucketStart
                        + " is not equal to " + another.bucketStart);
            }
            return new Entry(this.bucketStart,
                    // Active time should not go over bucket duration.
                    Math.min(this.activeTime + another.activeTime, bucketDuration),
                    this.rxBytes + another.rxBytes,
                    this.rxPackets + another.rxPackets,
                    this.txBytes + another.txBytes,
                    this.txPackets + another.txPackets,
                    this.operations + another.operations);
        }
    }

    /** @hide */
    @UnsupportedAppUsage
    public NetworkStatsHistory(long bucketDuration) {
        this(bucketDuration, 10, FIELD_ALL);
    }

    /** @hide */
    public NetworkStatsHistory(long bucketDuration, int initialSize) {
        this(bucketDuration, initialSize, FIELD_ALL);
    }

    /** @hide */
    public NetworkStatsHistory(long bucketDuration, int initialSize, int fields) {
        this.bucketDuration = bucketDuration;
        bucketStart = new long[initialSize];
        if ((fields & FIELD_ACTIVE_TIME) != 0) activeTime = new long[initialSize];
        if ((fields & FIELD_RX_BYTES) != 0) rxBytes = new long[initialSize];
        if ((fields & FIELD_RX_PACKETS) != 0) rxPackets = new long[initialSize];
        if ((fields & FIELD_TX_BYTES) != 0) txBytes = new long[initialSize];
        if ((fields & FIELD_TX_PACKETS) != 0) txPackets = new long[initialSize];
        if ((fields & FIELD_OPERATIONS) != 0) operations = new long[initialSize];
        bucketCount = 0;
        totalBytes = 0;
    }

    /** @hide */
    public NetworkStatsHistory(NetworkStatsHistory existing, long bucketDuration) {
        this(bucketDuration, existing.estimateResizeBuckets(bucketDuration));
        recordEntireHistory(existing);
    }

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public NetworkStatsHistory(Parcel in) {
        bucketDuration = in.readLong();
        bucketStart = readLongArray(in);
        activeTime = readLongArray(in);
        rxBytes = readLongArray(in);
        rxPackets = readLongArray(in);
        txBytes = readLongArray(in);
        txPackets = readLongArray(in);
        operations = readLongArray(in);
        bucketCount = bucketStart.length;
        totalBytes = in.readLong();
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeLong(bucketDuration);
        writeLongArray(out, bucketStart, bucketCount);
        writeLongArray(out, activeTime, bucketCount);
        writeLongArray(out, rxBytes, bucketCount);
        writeLongArray(out, rxPackets, bucketCount);
        writeLongArray(out, txBytes, bucketCount);
        writeLongArray(out, txPackets, bucketCount);
        writeLongArray(out, operations, bucketCount);
        out.writeLong(totalBytes);
    }

    /** @hide */
    public NetworkStatsHistory(DataInput in) throws IOException {
        final int version = in.readInt();
        switch (version) {
            case VERSION_INIT: {
                bucketDuration = in.readLong();
                bucketStart = readFullLongArray(in);
                rxBytes = readFullLongArray(in);
                rxPackets = new long[bucketStart.length];
                txBytes = readFullLongArray(in);
                txPackets = new long[bucketStart.length];
                operations = new long[bucketStart.length];
                bucketCount = bucketStart.length;
                totalBytes = CollectionUtils.total(rxBytes) + CollectionUtils.total(txBytes);
                break;
            }
            case VERSION_ADD_PACKETS:
            case VERSION_ADD_ACTIVE: {
                bucketDuration = in.readLong();
                bucketStart = readVarLongArray(in);
                activeTime = (version >= VERSION_ADD_ACTIVE) ? readVarLongArray(in)
                        : new long[bucketStart.length];
                rxBytes = readVarLongArray(in);
                rxPackets = readVarLongArray(in);
                txBytes = readVarLongArray(in);
                txPackets = readVarLongArray(in);
                operations = readVarLongArray(in);
                bucketCount = bucketStart.length;
                totalBytes = CollectionUtils.total(rxBytes) + CollectionUtils.total(txBytes);
                break;
            }
            default: {
                throw new ProtocolException("unexpected version: " + version);
            }
        }

        if (bucketStart.length != bucketCount || rxBytes.length != bucketCount
                || rxPackets.length != bucketCount || txBytes.length != bucketCount
                || txPackets.length != bucketCount || operations.length != bucketCount) {
            throw new ProtocolException("Mismatched history lengths");
        }
    }

    /** @hide */
    public void writeToStream(DataOutput out) throws IOException {
        out.writeInt(VERSION_ADD_ACTIVE);
        out.writeLong(bucketDuration);
        writeVarLongArray(out, bucketStart, bucketCount);
        writeVarLongArray(out, activeTime, bucketCount);
        writeVarLongArray(out, rxBytes, bucketCount);
        writeVarLongArray(out, rxPackets, bucketCount);
        writeVarLongArray(out, txBytes, bucketCount);
        writeVarLongArray(out, txPackets, bucketCount);
        writeVarLongArray(out, operations, bucketCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int size() {
        return bucketCount;
    }

    /** @hide */
    public long getBucketDuration() {
        return bucketDuration;
    }

    /** @hide */
    @UnsupportedAppUsage
    public long getStart() {
        if (bucketCount > 0) {
            return bucketStart[0];
        } else {
            return Long.MAX_VALUE;
        }
    }

    /** @hide */
    @UnsupportedAppUsage
    public long getEnd() {
        if (bucketCount > 0) {
            return bucketStart[bucketCount - 1] + bucketDuration;
        } else {
            return Long.MIN_VALUE;
        }
    }

    /**
     * Return total bytes represented by this history.
     * @hide
     */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Return index of bucket that contains or is immediately before the
     * requested time.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int getIndexBefore(long time) {
        int index = Arrays.binarySearch(bucketStart, 0, bucketCount, time);
        if (index < 0) {
            index = (~index) - 1;
        } else {
            index -= 1;
        }
        return NetworkStatsUtils.constrain(index, 0, bucketCount - 1);
    }

    /**
     * Return index of bucket that contains or is immediately after the
     * requested time.
     * @hide
     */
    public int getIndexAfter(long time) {
        int index = Arrays.binarySearch(bucketStart, 0, bucketCount, time);
        if (index < 0) {
            index = ~index;
        } else {
            index += 1;
        }
        return NetworkStatsUtils.constrain(index, 0, bucketCount - 1);
    }

    /**
     * Return specific stats entry.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public Entry getValues(int i, Entry recycle) {
        final Entry entry = recycle != null ? recycle : new Entry();
        entry.bucketStart = bucketStart[i];
        entry.bucketDuration = bucketDuration;
        entry.activeTime = getLong(activeTime, i, UNKNOWN);
        entry.rxBytes = getLong(rxBytes, i, UNKNOWN);
        entry.rxPackets = getLong(rxPackets, i, UNKNOWN);
        entry.txBytes = getLong(txBytes, i, UNKNOWN);
        entry.txPackets = getLong(txPackets, i, UNKNOWN);
        entry.operations = getLong(operations, i, UNKNOWN);
        return entry;
    }

    /**
     * Get List of {@link Entry} of the {@link NetworkStatsHistory} instance.
     *
     * @return
     */
    @NonNull
    public List<Entry> getEntries() {
        // TODO: Return a wrapper that uses this list instead, to prevent the returned result
        //  from being changed.
        final ArrayList<Entry> ret = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            ret.add(getValues(i, null /* recycle */));
        }
        return ret;
    }

    /** @hide */
    public void setValues(int i, Entry entry) {
        // Unwind old values
        if (rxBytes != null) totalBytes -= rxBytes[i];
        if (txBytes != null) totalBytes -= txBytes[i];

        bucketStart[i] = entry.bucketStart;
        setLong(activeTime, i, entry.activeTime);
        setLong(rxBytes, i, entry.rxBytes);
        setLong(rxPackets, i, entry.rxPackets);
        setLong(txBytes, i, entry.txBytes);
        setLong(txPackets, i, entry.txPackets);
        setLong(operations, i, entry.operations);

        // Apply new values
        if (rxBytes != null) totalBytes += rxBytes[i];
        if (txBytes != null) totalBytes += txBytes[i];
    }

    /**
     * Record that data traffic occurred in the given time range. Will
     * distribute across internal buckets, creating new buckets as needed.
     * @hide
     */
    @Deprecated
    public void recordData(long start, long end, long rxBytes, long txBytes) {
        recordData(start, end, new NetworkStats.Entry(
                IFACE_ALL, UID_ALL, SET_DEFAULT, TAG_NONE, METERED_NO, ROAMING_NO,
                DEFAULT_NETWORK_NO, rxBytes, 0L, txBytes, 0L, 0L));
    }

    /**
     * Record that data traffic occurred in the given time range. Will
     * distribute across internal buckets, creating new buckets as needed.
     * @hide
     */
    public void recordData(long start, long end, NetworkStats.Entry entry) {
        long rxBytes = entry.rxBytes;
        long rxPackets = entry.rxPackets;
        long txBytes = entry.txBytes;
        long txPackets = entry.txPackets;
        long operations = entry.operations;

        if (entry.isNegative()) {
            throw new IllegalArgumentException("tried recording negative data");
        }
        if (entry.isEmpty()) {
            return;
        }

        // create any buckets needed by this range
        ensureBuckets(start, end);
        // Return fast if there is still no entry. This would typically happen when the start,
        // end or duration are not valid values, e.g. start > end, negative duration value, etc.
        if (bucketCount == 0) return;

        // distribute data usage into buckets
        long duration = end - start;
        final int startIndex = getIndexAfter(end);
        for (int i = startIndex; i >= 0; i--) {
            final long curStart = bucketStart[i];
            final long curEnd = curStart + bucketDuration;

            // bucket is older than record; we're finished
            if (curEnd < start) break;
            // bucket is newer than record; keep looking
            if (curStart > end) continue;

            final long overlap = Math.min(curEnd, end) - Math.max(curStart, start);
            if (overlap <= 0) continue;

            // integer math each time is faster than floating point
            final long fracRxBytes = multiplySafeByRational(rxBytes, overlap, duration);
            final long fracRxPackets = multiplySafeByRational(rxPackets, overlap, duration);
            final long fracTxBytes = multiplySafeByRational(txBytes, overlap, duration);
            final long fracTxPackets = multiplySafeByRational(txPackets, overlap, duration);
            final long fracOperations = multiplySafeByRational(operations, overlap, duration);


            addLong(activeTime, i, overlap);
            addLong(this.rxBytes, i, fracRxBytes); rxBytes -= fracRxBytes;
            addLong(this.rxPackets, i, fracRxPackets); rxPackets -= fracRxPackets;
            addLong(this.txBytes, i, fracTxBytes); txBytes -= fracTxBytes;
            addLong(this.txPackets, i, fracTxPackets); txPackets -= fracTxPackets;
            addLong(this.operations, i, fracOperations); operations -= fracOperations;

            duration -= overlap;
        }

        totalBytes += entry.rxBytes + entry.txBytes;
    }

    /**
     * Record an entire {@link NetworkStatsHistory} into this history. Usually
     * for combining together stats for external reporting.
     * @hide
     */
    @UnsupportedAppUsage
    public void recordEntireHistory(NetworkStatsHistory input) {
        recordHistory(input, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * Record given {@link NetworkStatsHistory} into this history, copying only
     * buckets that atomically occur in the inclusive time range. Doesn't
     * interpolate across partial buckets.
     * @hide
     */
    public void recordHistory(NetworkStatsHistory input, long start, long end) {
        final NetworkStats.Entry entry = new NetworkStats.Entry(
                IFACE_ALL, UID_ALL, SET_DEFAULT, TAG_NONE, METERED_NO, ROAMING_NO,
                DEFAULT_NETWORK_NO, 0L, 0L, 0L, 0L, 0L);
        for (int i = 0; i < input.bucketCount; i++) {
            final long bucketStart = input.bucketStart[i];
            final long bucketEnd = bucketStart + input.bucketDuration;

            // skip when bucket is outside requested range
            if (bucketStart < start || bucketEnd > end) continue;

            entry.rxBytes = getLong(input.rxBytes, i, 0L);
            entry.rxPackets = getLong(input.rxPackets, i, 0L);
            entry.txBytes = getLong(input.txBytes, i, 0L);
            entry.txPackets = getLong(input.txPackets, i, 0L);
            entry.operations = getLong(input.operations, i, 0L);

            recordData(bucketStart, bucketEnd, entry);
        }
    }

    /**
     * Ensure that buckets exist for given time range, creating as needed.
     */
    private void ensureBuckets(long start, long end) {
        // normalize incoming range to bucket boundaries
        start -= start % bucketDuration;
        end += (bucketDuration - (end % bucketDuration)) % bucketDuration;

        for (long now = start; now < end; now += bucketDuration) {
            // try finding existing bucket
            final int index = Arrays.binarySearch(bucketStart, 0, bucketCount, now);
            if (index < 0) {
                // bucket missing, create and insert
                insertBucket(~index, now);
            }
        }
    }

    /**
     * Insert new bucket at requested index and starting time.
     */
    private void insertBucket(int index, long start) {
        // create more buckets when needed
        if (bucketCount >= bucketStart.length) {
            final int newLength = Math.max(bucketStart.length, 10) * 3 / 2;
            bucketStart = Arrays.copyOf(bucketStart, newLength);
            if (activeTime != null) activeTime = Arrays.copyOf(activeTime, newLength);
            if (rxBytes != null) rxBytes = Arrays.copyOf(rxBytes, newLength);
            if (rxPackets != null) rxPackets = Arrays.copyOf(rxPackets, newLength);
            if (txBytes != null) txBytes = Arrays.copyOf(txBytes, newLength);
            if (txPackets != null) txPackets = Arrays.copyOf(txPackets, newLength);
            if (operations != null) operations = Arrays.copyOf(operations, newLength);
        }

        // create gap when inserting bucket in middle
        if (index < bucketCount) {
            final int dstPos = index + 1;
            final int length = bucketCount - index;

            System.arraycopy(bucketStart, index, bucketStart, dstPos, length);
            if (activeTime != null) System.arraycopy(activeTime, index, activeTime, dstPos, length);
            if (rxBytes != null) System.arraycopy(rxBytes, index, rxBytes, dstPos, length);
            if (rxPackets != null) System.arraycopy(rxPackets, index, rxPackets, dstPos, length);
            if (txBytes != null) System.arraycopy(txBytes, index, txBytes, dstPos, length);
            if (txPackets != null) System.arraycopy(txPackets, index, txPackets, dstPos, length);
            if (operations != null) System.arraycopy(operations, index, operations, dstPos, length);
        }

        bucketStart[index] = start;
        setLong(activeTime, index, 0L);
        setLong(rxBytes, index, 0L);
        setLong(rxPackets, index, 0L);
        setLong(txBytes, index, 0L);
        setLong(txPackets, index, 0L);
        setLong(operations, index, 0L);
        bucketCount++;
    }

    /**
     * Clear all data stored in this object.
     * @hide
     */
    public void clear() {
        bucketStart = EmptyArray.LONG;
        if (activeTime != null) activeTime = EmptyArray.LONG;
        if (rxBytes != null) rxBytes = EmptyArray.LONG;
        if (rxPackets != null) rxPackets = EmptyArray.LONG;
        if (txBytes != null) txBytes = EmptyArray.LONG;
        if (txPackets != null) txPackets = EmptyArray.LONG;
        if (operations != null) operations = EmptyArray.LONG;
        bucketCount = 0;
        totalBytes = 0;
    }

    /**
     * Remove buckets that start older than requested cutoff.
     *
     * This method will remove any bucket that contains any data older than the requested
     * cutoff, even if that same bucket includes some data from after the cutoff.
     *
     * @hide
     */
    public void removeBucketsStartingBefore(final long cutoff) {
        // TODO: Consider use getIndexBefore.
        int i;
        for (i = 0; i < bucketCount; i++) {
            final long curStart = bucketStart[i];

            // This bucket starts after or at the cutoff, so it should be kept.
            if (curStart >= cutoff) break;
        }

        if (i > 0) {
            final int length = bucketStart.length;
            bucketStart = Arrays.copyOfRange(bucketStart, i, length);
            if (activeTime != null) activeTime = Arrays.copyOfRange(activeTime, i, length);
            if (rxBytes != null) rxBytes = Arrays.copyOfRange(rxBytes, i, length);
            if (rxPackets != null) rxPackets = Arrays.copyOfRange(rxPackets, i, length);
            if (txBytes != null) txBytes = Arrays.copyOfRange(txBytes, i, length);
            if (txPackets != null) txPackets = Arrays.copyOfRange(txPackets, i, length);
            if (operations != null) operations = Arrays.copyOfRange(operations, i, length);
            bucketCount -= i;

            totalBytes = 0;
            if (rxBytes != null) totalBytes += CollectionUtils.total(rxBytes);
            if (txBytes != null) totalBytes += CollectionUtils.total(txBytes);
        }
    }

    /**
     * Return interpolated data usage across the requested range. Interpolates
     * across buckets, so values may be rounded slightly.
     *
     * <p>If the active bucket is not completed yet, it returns the proportional value of it
     * based on its duration and the {@code end} param.
     *
     * @param start - start of the range, timestamp in milliseconds since the epoch.
     * @param end - end of the range, timestamp in milliseconds since the epoch.
     * @param recycle - entry instance for performance, could be null.
     * @hide
     */
    @UnsupportedAppUsage
    public Entry getValues(long start, long end, Entry recycle) {
        return getValues(start, end, Long.MAX_VALUE, recycle);
    }

    /**
     * Return interpolated data usage across the requested range. Interpolates
     * across buckets, so values may be rounded slightly.
     *
     * @param start - start of the range, timestamp in milliseconds since the epoch.
     * @param end - end of the range, timestamp in milliseconds since the epoch.
     * @param now - current timestamp in milliseconds since the epoch (wall clock).
     * @param recycle - entry instance for performance, could be null.
     * @hide
     */
    @UnsupportedAppUsage
    public Entry getValues(long start, long end, long now, Entry recycle) {
        final Entry entry = recycle != null ? recycle : new Entry();
        entry.bucketDuration = end - start;
        entry.bucketStart = start;
        entry.activeTime = activeTime != null ? 0 : UNKNOWN;
        entry.rxBytes = rxBytes != null ? 0 : UNKNOWN;
        entry.rxPackets = rxPackets != null ? 0 : UNKNOWN;
        entry.txBytes = txBytes != null ? 0 : UNKNOWN;
        entry.txPackets = txPackets != null ? 0 : UNKNOWN;
        entry.operations = operations != null ? 0 : UNKNOWN;

        // Return fast if there is no entry.
        if (bucketCount == 0) return entry;

        final int startIndex = getIndexAfter(end);
        for (int i = startIndex; i >= 0; i--) {
            final long curStart = bucketStart[i];
            long curEnd = curStart + bucketDuration;

            // bucket is older than request; we're finished
            if (curEnd <= start) break;
            // bucket is newer than request; keep looking
            if (curStart >= end) continue;

            // the active bucket is shorter then a normal completed bucket
            if (curEnd > now) curEnd = now;
            // usually this is simply bucketDuration
            final long bucketSpan = curEnd - curStart;
            // prevent division by zero
            if (bucketSpan <= 0) continue;

            final long overlapEnd = curEnd < end ? curEnd : end;
            final long overlapStart = curStart > start ? curStart : start;
            final long overlap = overlapEnd - overlapStart;
            if (overlap <= 0) continue;

            // integer math each time is faster than floating point
            if (activeTime != null) {
                entry.activeTime += multiplySafeByRational(activeTime[i], overlap, bucketSpan);
            }
            if (rxBytes != null) {
                entry.rxBytes += multiplySafeByRational(rxBytes[i], overlap, bucketSpan);
            }
            if (rxPackets != null) {
                entry.rxPackets += multiplySafeByRational(rxPackets[i], overlap, bucketSpan);
            }
            if (txBytes != null) {
                entry.txBytes += multiplySafeByRational(txBytes[i], overlap, bucketSpan);
            }
            if (txPackets != null) {
                entry.txPackets += multiplySafeByRational(txPackets[i], overlap, bucketSpan);
            }
            if (operations != null) {
                entry.operations += multiplySafeByRational(operations[i], overlap, bucketSpan);
            }
        }
        return entry;
    }

    /**
     * @deprecated only for temporary testing
     * @hide
     */
    @Deprecated
    public void generateRandom(long start, long end, long bytes) {
        final Random r = new Random();

        final float fractionRx = r.nextFloat();
        final long rxBytes = (long) (bytes * fractionRx);
        final long txBytes = (long) (bytes * (1 - fractionRx));

        final long rxPackets = rxBytes / 1024;
        final long txPackets = txBytes / 1024;
        final long operations = rxBytes / 2048;

        generateRandom(start, end, rxBytes, rxPackets, txBytes, txPackets, operations, r);
    }

    /**
     * @deprecated only for temporary testing
     * @hide
     */
    @Deprecated
    public void generateRandom(long start, long end, long rxBytes, long rxPackets, long txBytes,
            long txPackets, long operations, Random r) {
        ensureBuckets(start, end);

        final NetworkStats.Entry entry = new NetworkStats.Entry(
                IFACE_ALL, UID_ALL, SET_DEFAULT, TAG_NONE, METERED_NO, ROAMING_NO,
                DEFAULT_NETWORK_NO, 0L, 0L, 0L, 0L, 0L);
        while (rxBytes > 1024 || rxPackets > 128 || txBytes > 1024 || txPackets > 128
                || operations > 32) {
            final long curStart = randomLong(r, start, end);
            final long curEnd = curStart + randomLong(r, 0, (end - curStart) / 2);

            entry.rxBytes = randomLong(r, 0, rxBytes);
            entry.rxPackets = randomLong(r, 0, rxPackets);
            entry.txBytes = randomLong(r, 0, txBytes);
            entry.txPackets = randomLong(r, 0, txPackets);
            entry.operations = randomLong(r, 0, operations);

            rxBytes -= entry.rxBytes;
            rxPackets -= entry.rxPackets;
            txBytes -= entry.txBytes;
            txPackets -= entry.txPackets;
            operations -= entry.operations;

            recordData(curStart, curEnd, entry);
        }
    }

    /** @hide */
    public static long randomLong(Random r, long start, long end) {
        return (long) (start + (r.nextFloat() * (end - start)));
    }

    /**
     * Quickly determine if this history intersects with given window.
     * @hide
     */
    public boolean intersects(long start, long end) {
        final long dataStart = getStart();
        final long dataEnd = getEnd();
        if (start >= dataStart && start <= dataEnd) return true;
        if (end >= dataStart && end <= dataEnd) return true;
        if (dataStart >= start && dataStart <= end) return true;
        if (dataEnd >= start && dataEnd <= end) return true;
        return false;
    }

    /** @hide */
    public void dump(IndentingPrintWriter pw, boolean fullHistory) {
        pw.print("NetworkStatsHistory: bucketDuration=");
        pw.println(bucketDuration / SECOND_IN_MILLIS);
        pw.increaseIndent();

        final int start = fullHistory ? 0 : Math.max(0, bucketCount - 32);
        if (start > 0) {
            pw.print("(omitting "); pw.print(start); pw.println(" buckets)");
        }

        for (int i = start; i < bucketCount; i++) {
            pw.print("st="); pw.print(bucketStart[i] / SECOND_IN_MILLIS);
            if (rxBytes != null) { pw.print(" rb="); pw.print(rxBytes[i]); }
            if (rxPackets != null) { pw.print(" rp="); pw.print(rxPackets[i]); }
            if (txBytes != null) { pw.print(" tb="); pw.print(txBytes[i]); }
            if (txPackets != null) { pw.print(" tp="); pw.print(txPackets[i]); }
            if (operations != null) { pw.print(" op="); pw.print(operations[i]); }
            pw.println();
        }

        pw.decreaseIndent();
    }

    /** @hide */
    public void dumpCheckin(PrintWriter pw) {
        pw.print("d,");
        pw.print(bucketDuration / SECOND_IN_MILLIS);
        pw.println();

        for (int i = 0; i < bucketCount; i++) {
            pw.print("b,");
            pw.print(bucketStart[i] / SECOND_IN_MILLIS); pw.print(',');
            if (rxBytes != null) { pw.print(rxBytes[i]); } else { pw.print("*"); } pw.print(',');
            if (rxPackets != null) { pw.print(rxPackets[i]); } else { pw.print("*"); } pw.print(',');
            if (txBytes != null) { pw.print(txBytes[i]); } else { pw.print("*"); } pw.print(',');
            if (txPackets != null) { pw.print(txPackets[i]); } else { pw.print("*"); } pw.print(',');
            if (operations != null) { pw.print(operations[i]); } else { pw.print("*"); }
            pw.println();
        }
    }

    /** @hide */
    public void dumpDebug(ProtoOutputStream proto, long tag) {
        final long start = proto.start(tag);

        proto.write(NetworkStatsHistoryProto.BUCKET_DURATION_MS, bucketDuration);

        for (int i = 0; i < bucketCount; i++) {
            final long startBucket = proto.start(NetworkStatsHistoryProto.BUCKETS);

            proto.write(NetworkStatsHistoryBucketProto.BUCKET_START_MS,
                    bucketStart[i]);
            dumpDebug(proto, NetworkStatsHistoryBucketProto.RX_BYTES, rxBytes, i);
            dumpDebug(proto, NetworkStatsHistoryBucketProto.RX_PACKETS, rxPackets, i);
            dumpDebug(proto, NetworkStatsHistoryBucketProto.TX_BYTES, txBytes, i);
            dumpDebug(proto, NetworkStatsHistoryBucketProto.TX_PACKETS, txPackets, i);
            dumpDebug(proto, NetworkStatsHistoryBucketProto.OPERATIONS, operations, i);

            proto.end(startBucket);
        }

        proto.end(start);
    }

    private static void dumpDebug(ProtoOutputStream proto, long tag, long[] array, int index) {
        if (array != null) {
            proto.write(tag, array[index]);
        }
    }

    @Override
    public String toString() {
        final CharArrayWriter writer = new CharArrayWriter();
        dump(new IndentingPrintWriter(writer, "  "), false);
        return writer.toString();
    }

    /**
     * Same as "equals", but not actually called equals as this would affect public API behavior.
     * @hide
     */
    @Nullable
    public boolean isSameAs(NetworkStatsHistory other) {
        return bucketCount == other.bucketCount
                && Arrays.equals(bucketStart, other.bucketStart)
                // Don't check activeTime since it can change on import due to the importer using
                // recordHistory. It's also not exposed by the APIs or present in dumpsys or
                // toString().
                && Arrays.equals(rxBytes, other.rxBytes)
                && Arrays.equals(rxPackets, other.rxPackets)
                && Arrays.equals(txBytes, other.txBytes)
                && Arrays.equals(txPackets, other.txPackets)
                && Arrays.equals(operations, other.operations)
                && totalBytes == other.totalBytes;
    }

    @UnsupportedAppUsage
    public static final @android.annotation.NonNull Creator<NetworkStatsHistory> CREATOR = new Creator<NetworkStatsHistory>() {
        @Override
        public NetworkStatsHistory createFromParcel(Parcel in) {
            return new NetworkStatsHistory(in);
        }

        @Override
        public NetworkStatsHistory[] newArray(int size) {
            return new NetworkStatsHistory[size];
        }
    };

    private static long getLong(long[] array, int i, long value) {
        return array != null ? array[i] : value;
    }

    private static void setLong(long[] array, int i, long value) {
        if (array != null) array[i] = value;
    }

    private static void addLong(long[] array, int i, long value) {
        if (array != null) array[i] += value;
    }

    /** @hide */
    public int estimateResizeBuckets(long newBucketDuration) {
        return (int) (size() * getBucketDuration() / newBucketDuration);
    }

    /**
     * Utility methods for interacting with {@link DataInputStream} and
     * {@link DataOutputStream}, mostly dealing with writing partial arrays.
     * @hide
     */
    public static class DataStreamUtils {
        @Deprecated
        public static long[] readFullLongArray(DataInput in) throws IOException {
            final int size = in.readInt();
            if (size < 0) throw new ProtocolException("negative array size");
            final long[] values = new long[size];
            for (int i = 0; i < values.length; i++) {
                values[i] = in.readLong();
            }
            return values;
        }

        /**
         * Read variable-length {@link Long} using protobuf-style approach.
         */
        public static long readVarLong(DataInput in) throws IOException {
            int shift = 0;
            long result = 0;
            while (shift < 64) {
                byte b = in.readByte();
                result |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0)
                    return result;
                shift += 7;
            }
            throw new ProtocolException("malformed long");
        }

        /**
         * Write variable-length {@link Long} using protobuf-style approach.
         */
        public static void writeVarLong(DataOutput out, long value) throws IOException {
            while (true) {
                if ((value & ~0x7FL) == 0) {
                    out.writeByte((int) value);
                    return;
                } else {
                    out.writeByte(((int) value & 0x7F) | 0x80);
                    value >>>= 7;
                }
            }
        }

        public static long[] readVarLongArray(DataInput in) throws IOException {
            final int size = in.readInt();
            if (size == -1) return null;
            if (size < 0) throw new ProtocolException("negative array size");
            final long[] values = new long[size];
            for (int i = 0; i < values.length; i++) {
                values[i] = readVarLong(in);
            }
            return values;
        }

        public static void writeVarLongArray(DataOutput out, long[] values, int size)
                throws IOException {
            if (values == null) {
                out.writeInt(-1);
                return;
            }
            if (size > values.length) {
                throw new IllegalArgumentException("size larger than length");
            }
            out.writeInt(size);
            for (int i = 0; i < size; i++) {
                writeVarLong(out, values[i]);
            }
        }
    }

    /**
     * Utility methods for interacting with {@link Parcel} structures, mostly
     * dealing with writing partial arrays.
     * @hide
     */
    public static class ParcelUtils {
        public static long[] readLongArray(Parcel in) {
            final int size = in.readInt();
            if (size == -1) return null;
            final long[] values = new long[size];
            for (int i = 0; i < values.length; i++) {
                values[i] = in.readLong();
            }
            return values;
        }

        public static void writeLongArray(Parcel out, long[] values, int size) {
            if (values == null) {
                out.writeInt(-1);
                return;
            }
            if (size > values.length) {
                throw new IllegalArgumentException("size larger than length");
            }
            out.writeInt(size);
            for (int i = 0; i < size; i++) {
                out.writeLong(values[i]);
            }
        }
    }

    /**
     * Builder class for {@link NetworkStatsHistory}.
     */
    public static final class Builder {
        private final TreeMap<Long, Entry> mEntries;
        private final long mBucketDuration;

        /**
         * Creates a new Builder with given bucket duration and initial capacity to construct
         * {@link NetworkStatsHistory} objects.
         *
         * @param bucketDuration Duration of the buckets of the object, in milliseconds.
         * @param initialCapacity Estimated number of records.
         */
        public Builder(long bucketDuration, int initialCapacity) {
            mBucketDuration = bucketDuration;
            // Create a collection that is always sorted and can deduplicate items by the timestamp.
            mEntries = new TreeMap<>();
        }

        /**
         * Add an {@link Entry} into the {@link NetworkStatsHistory} instance. If the timestamp
         * already exists, the given {@link Entry} will be combined into existing entry.
         *
         * @param entry The target {@link Entry} object.
         * @return The builder object.
         */
        @NonNull
        public Builder addEntry(@NonNull Entry entry) {
            final Entry existing = mEntries.get(entry.bucketStart);
            if (existing != null) {
                mEntries.put(entry.bucketStart, existing.plus(entry, mBucketDuration));
            } else {
                mEntries.put(entry.bucketStart, entry);
            }
            return this;
        }

        private static long sum(@NonNull long[] array) {
            long sum = 0L;
            for (long entry : array) {
                sum += entry;
            }
            return sum;
        }

        /**
         * Builds the instance of the {@link NetworkStatsHistory}.
         *
         * @return the built instance of {@link NetworkStatsHistory}.
         */
        @NonNull
        public NetworkStatsHistory build() {
            int size = mEntries.size();
            final long[] bucketStart = new long[size];
            final long[] activeTime = new long[size];
            final long[] rxBytes = new long[size];
            final long[] rxPackets = new long[size];
            final long[] txBytes = new long[size];
            final long[] txPackets = new long[size];
            final long[] operations = new long[size];

            int i = 0;
            for (Entry entry : mEntries.values()) {
                bucketStart[i] = entry.bucketStart;
                activeTime[i] = entry.activeTime;
                rxBytes[i] = entry.rxBytes;
                rxPackets[i] = entry.rxPackets;
                txBytes[i] = entry.txBytes;
                txPackets[i] = entry.txPackets;
                operations[i] = entry.operations;
                i++;
            }

            return new NetworkStatsHistory(mBucketDuration, bucketStart, activeTime,
                    rxBytes, rxPackets, txBytes, txPackets, operations,
                    size, sum(rxBytes) + sum(txBytes));
        }
    }
}
