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

package com.android.net.module.util;

import static android.net.DnsResolver.TYPE_A;
import static android.net.DnsResolver.TYPE_AAAA;

import static com.android.internal.annotations.VisibleForTesting.Visibility.PRIVATE;
import static com.android.net.module.util.DnsPacketUtils.DnsRecordParser.domainNameToLabels;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.net.module.util.DnsPacketUtils.DnsRecordParser;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Defines basic data for DNS protocol based on RFC 1035.
 * Subclasses create the specific format used in DNS packet.
 *
 * @hide
 */
public abstract class DnsPacket {
    /**
     * Type of the canonical name for an alias. Refer to RFC 1035 section 3.2.2.
     */
    // TODO: Define the constant as a public constant in DnsResolver since it can never change.
    private static final int TYPE_CNAME = 5;
    public static final int TYPE_SVCB = 64;

    /**
     * Thrown when parsing packet failed.
     */
    public static class ParseException extends RuntimeException {
        public String reason;
        public ParseException(@NonNull String reason) {
            super(reason);
            this.reason = reason;
        }

        public ParseException(@NonNull String reason, @NonNull Throwable cause) {
            super(reason, cause);
            this.reason = reason;
        }
    }

    /**
     * DNS header for DNS protocol based on RFC 1035 section 4.1.1.
     *
     *                                     1  1  1  1  1  1
     *       0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                      ID                       |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |QR|   Opcode  |AA|TC|RD|RA|   Z    |   RCODE   |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                    QDCOUNT                    |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                    ANCOUNT                    |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                    NSCOUNT                    |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                    ARCOUNT                    |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     */
    public static class DnsHeader {
        private static final String TAG = "DnsHeader";
        private static final int SIZE_IN_BYTES = 12;
        private final int mId;
        private final int mFlags;
        private final int[] mRecordCount;

        /* If this bit in the 'flags' field is set to 0, the DNS message corresponding to this
         * header is a query; otherwise, it is a response.
         */
        private static final int FLAGS_SECTION_QR_BIT = 15;

        /**
         * Create a new DnsHeader from a positioned ByteBuffer.
         *
         * The ByteBuffer must be in network byte order (which is the default).
         * Reads the passed ByteBuffer from its current position and decodes a DNS header.
         * When this constructor returns, the reading position of the ByteBuffer has been
         * advanced to the end of the DNS header record.
         * This is meant to chain with other methods reading a DNS response in sequence.
         */
        @VisibleForTesting
        public DnsHeader(@NonNull ByteBuffer buf) throws BufferUnderflowException {
            Objects.requireNonNull(buf);
            mId = Short.toUnsignedInt(buf.getShort());
            mFlags = Short.toUnsignedInt(buf.getShort());
            mRecordCount = new int[NUM_SECTIONS];
            for (int i = 0; i < NUM_SECTIONS; ++i) {
                mRecordCount[i] = Short.toUnsignedInt(buf.getShort());
            }
        }

        /**
         * Determines if the DNS message corresponding to this header is a response, as defined in
         * RFC 1035 Section 4.1.1.
         */
        public boolean isResponse() {
            return (mFlags & (1 << FLAGS_SECTION_QR_BIT)) != 0;
        }

        /**
         * Create a new DnsHeader from specified parameters.
         *
         * This constructor only builds the question and answer sections. Authority
         * and additional sections are not supported. Useful when synthesizing dns
         * responses from query or reply packets.
         */
        @VisibleForTesting
        public DnsHeader(int id, int flags, int qdcount, int ancount) {
            this.mId = id;
            this.mFlags = flags;
            mRecordCount = new int[NUM_SECTIONS];
            mRecordCount[QDSECTION] = qdcount;
            mRecordCount[ANSECTION] = ancount;
        }

        /**
         * Get record count by type.
         */
        public int getRecordCount(int type) {
            return mRecordCount[type];
        }

        /**
         * Get flags of this instance.
         */
        public int getFlags() {
            return mFlags;
        }

        /**
         * Get id of this instance.
         */
        public int getId() {
            return mId;
        }

        @Override
        public String toString() {
            return "DnsHeader{" + "id=" + mId + ", flags=" + mFlags
                    + ", recordCounts=" + Arrays.toString(mRecordCount) + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o.getClass() != getClass()) return false;
            final DnsHeader other = (DnsHeader) o;
            return mId == other.mId
                    && mFlags == other.mFlags
                    && Arrays.equals(mRecordCount, other.mRecordCount);
        }

        @Override
        public int hashCode() {
            return 31 * mId + 37 * mFlags + Arrays.hashCode(mRecordCount);
        }

        /**
         * Get DnsHeader as byte array.
         */
        @NonNull
        public byte[] getBytes() {
            // TODO: if this is called often, optimize the ByteBuffer out and write to the
            //  array directly.
            final ByteBuffer buf = ByteBuffer.allocate(SIZE_IN_BYTES);
            buf.putShort((short) mId);
            buf.putShort((short) mFlags);
            for (int i = 0; i < NUM_SECTIONS; ++i) {
                buf.putShort((short) mRecordCount[i]);
            }
            return buf.array();
        }
    }

    /**
     * Superclass for DNS questions and DNS resource records.
     *
     * DNS questions (No TTL/RDLENGTH/RDATA) based on RFC 1035 section 4.1.2.
     *                                     1  1  1  1  1  1
     *       0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                                               |
     *     /                     QNAME                     /
     *     /                                               /
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                     QTYPE                     |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                     QCLASS                    |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *
     * DNS resource records (With TTL/RDLENGTH/RDATA) based on RFC 1035 section 4.1.3.
     *                                     1  1  1  1  1  1
     *       0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                                               |
     *     /                                               /
     *     /                      NAME                     /
     *     |                                               |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                      TYPE                     |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                     CLASS                     |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                      TTL                      |
     *     |                                               |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *     |                   RDLENGTH                    |
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
     *     /                     RDATA                     /
     *     /                                               /
     *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     *
     * Note that this class is meant to be used by composition and not inheritance, and
     * that classes implementing more specific DNS records should call #parse.
     */
    // TODO: Make DnsResourceRecord and DnsQuestion subclasses of DnsRecord.
    public static class DnsRecord {
        // Refer to RFC 1035 section 2.3.4 for MAXNAMESIZE.
        // NAME_NORMAL and NAME_COMPRESSION are used for checking name compression,
        // refer to rfc 1035 section 4.1.4.
        public static final int MAXNAMESIZE = 255;
        public static final int NAME_NORMAL = 0;
        public static final int NAME_COMPRESSION = 0xC0;

        private static final String TAG = "DnsRecord";

        public final String dName;
        public final int nsType;
        public final int nsClass;
        public final long ttl;
        private final byte[] mRdata;
        /**
         * Type of this DNS record.
         */
        @RecordType
        public final int rType;

        /**
         * Create a new DnsRecord from a positioned ByteBuffer.
         *
         * Reads the passed ByteBuffer from its current position and decodes a DNS record.
         * When this constructor returns, the reading position of the ByteBuffer has been
         * advanced to the end of the DNS resource record.
         * This is meant to chain with other methods reading a DNS response in sequence.
         *
         * @param rType Type of the record.
         * @param buf ByteBuffer input of record, must be in network byte order
         *         (which is the default).
         */
        protected DnsRecord(@RecordType int rType, @NonNull ByteBuffer buf)
                throws BufferUnderflowException, ParseException {
            Objects.requireNonNull(buf);
            this.rType = rType;
            dName = DnsRecordParser.parseName(buf, 0 /* Parse depth */,
                    true /* isNameCompressionSupported */);
            if (dName.length() > MAXNAMESIZE) {
                throw new ParseException(
                        "Parse name fail, name size is too long: " + dName.length());
            }
            nsType = Short.toUnsignedInt(buf.getShort());
            nsClass = Short.toUnsignedInt(buf.getShort());

            if (rType != QDSECTION) {
                ttl = Integer.toUnsignedLong(buf.getInt());
                final int length = Short.toUnsignedInt(buf.getShort());
                mRdata = new byte[length];
                buf.get(mRdata);
            } else {
                ttl = 0;
                mRdata = null;
            }
        }

        /**
         * Create a new DnsRecord or subclass of DnsRecord instance from a positioned ByteBuffer.
         *
         * Peek the nsType, sending the buffer to corresponding DnsRecord subclass constructors
         * to allow constructing the corresponding object.
         */
        @VisibleForTesting(visibility = PRIVATE)
        public static DnsRecord parse(@RecordType int rType, @NonNull ByteBuffer buf)
                throws BufferUnderflowException, ParseException {
            Objects.requireNonNull(buf);
            final int oldPos = buf.position();
            // Parsed name not used, just for jumping to nsType position.
            DnsRecordParser.parseName(buf, 0 /* Parse depth */,
                    true /* isNameCompressionSupported */);
            // Peek the nsType.
            final int nsType = Short.toUnsignedInt(buf.getShort());
            buf.position(oldPos);
            // Return a DnsRecord instance by default for backward compatibility, this is useful
            // when a partner supports new type of DnsRecord but does not inherit DnsRecord.
            switch (nsType) {
                case TYPE_SVCB:
                    return new DnsSvcbRecord(rType, buf);
                default:
                    return new DnsRecord(rType, buf);
            }
        }

        /**
         * Make an A or AAAA record based on the specified parameters.
         *
         * @param rType Type of the record, can be {@link #ANSECTION}, {@link #ARSECTION}
         *              or {@link #NSSECTION}.
         * @param dName Domain name of the record.
         * @param nsClass Class of the record. See RFC 1035 section 3.2.4.
         * @param ttl time interval (in seconds) that the resource record may be
         *            cached before it should be discarded. Zero values are
         *            interpreted to mean that the RR can only be used for the
         *            transaction in progress, and should not be cached.
         * @param address Instance of {@link InetAddress}
         * @return A record if the {@code address} is an IPv4 address, or AAAA record if the
         *         {@code address} is an IPv6 address.
         */
        public static DnsRecord makeAOrAAAARecord(int rType, @NonNull String dName,
                int nsClass, long ttl, @NonNull InetAddress address) throws IOException {
            final int nsType = (address.getAddress().length == 4) ? TYPE_A : TYPE_AAAA;
            return new DnsRecord(rType, dName, nsType, nsClass, ttl, address, null /* rDataStr */);
        }

        /**
         * Make an CNAME record based on the specified parameters.
         *
         * @param rType Type of the record, can be {@link #ANSECTION}, {@link #ARSECTION}
         *              or {@link #NSSECTION}.
         * @param dName Domain name of the record.
         * @param nsClass Class of the record. See RFC 1035 section 3.2.4.
         * @param ttl time interval (in seconds) that the resource record may be
         *            cached before it should be discarded. Zero values are
         *            interpreted to mean that the RR can only be used for the
         *            transaction in progress, and should not be cached.
         * @param domainName Canonical name of the {@code dName}.
         * @return A record if the {@code address} is an IPv4 address, or AAAA record if the
         *         {@code address} is an IPv6 address.
         */
        public static DnsRecord makeCNameRecord(int rType, @NonNull String dName, int nsClass,
                long ttl, @NonNull String domainName) throws IOException {
            return new DnsRecord(rType, dName, TYPE_CNAME, nsClass, ttl, null /* address */,
                    domainName);
        }

        /**
         * Make a DNS question based on the specified parameters.
         */
        public static DnsRecord makeQuestion(@NonNull String dName, int nsType, int nsClass) {
            return new DnsRecord(dName, nsType, nsClass);
        }

        private static String requireHostName(@NonNull String name) {
            if (!DnsRecordParser.isHostName(name)) {
                throw new IllegalArgumentException("Expected domain name but got " + name);
            }
            return name;
        }

        /**
         * Create a new query DnsRecord from specified parameters, useful when synthesizing
         * dns response.
         */
        private DnsRecord(@NonNull String dName, int nsType, int nsClass) {
            this.rType = QDSECTION;
            this.dName = requireHostName(dName);
            this.nsType = nsType;
            this.nsClass = nsClass;
            mRdata = null;
            this.ttl = 0;
        }

        /**
         * Create a new CNAME/A/AAAA DnsRecord from specified parameters.
         *
         * @param address The address only used when synthesizing A or AAAA record.
         * @param rDataStr The alias of the domain, only used when synthesizing CNAME record.
         */
        private DnsRecord(@RecordType int rType, @NonNull String dName, int nsType, int nsClass,
                long ttl, @Nullable InetAddress address, @Nullable String rDataStr)
                throws IOException {
            this.rType = rType;
            this.dName = requireHostName(dName);
            this.nsType = nsType;
            this.nsClass = nsClass;
            if (rType < 0 || rType >= NUM_SECTIONS || rType == QDSECTION) {
                throw new IllegalArgumentException("Unexpected record type: " + rType);
            }
            mRdata = nsType == TYPE_CNAME ? domainNameToLabels(rDataStr) : address.getAddress();
            this.ttl = ttl;
        }

        /**
         * Get a copy of rdata.
         */
        @Nullable
        public byte[] getRR() {
            return (mRdata == null) ? null : mRdata.clone();
        }

        /**
         * Get DnsRecord as byte array.
         */
        @NonNull
        public byte[] getBytes() throws IOException {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos);
            dos.write(domainNameToLabels(dName));
            dos.writeShort(nsType);
            dos.writeShort(nsClass);
            if (rType != QDSECTION) {
                dos.writeInt((int) ttl);
                if (mRdata == null) {
                    dos.writeShort(0);
                } else {
                    dos.writeShort(mRdata.length);
                    dos.write(mRdata);
                }
            }
            return baos.toByteArray();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o.getClass() != getClass()) return false;
            final DnsRecord other = (DnsRecord) o;
            return rType == other.rType
                    && nsType == other.nsType
                    && nsClass == other.nsClass
                    && ttl == other.ttl
                    && TextUtils.equals(dName, other.dName)
                    && Arrays.equals(mRdata, other.mRdata);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(dName)
                    + 37 * ((int) (ttl & 0xFFFFFFFF))
                    + 41 * ((int) (ttl >> 32))
                    + 43 * nsType
                    + 47 * nsClass
                    + 53 * rType
                    + Arrays.hashCode(mRdata);
        }

        @Override
        public String toString() {
            return "DnsRecord{"
                    + "rType=" + rType
                    + ", dName='" + dName + '\''
                    + ", nsType=" + nsType
                    + ", nsClass=" + nsClass
                    + ", ttl=" + ttl
                    + ", mRdata=" + Arrays.toString(mRdata)
                    + '}';
        }
    }

    /**
     * Header section types, refer to RFC 1035 section 4.1.1.
     */
    public static final int QDSECTION = 0;
    public static final int ANSECTION = 1;
    public static final int NSSECTION = 2;
    public static final int ARSECTION = 3;
    @VisibleForTesting(visibility = PRIVATE)
    static final int NUM_SECTIONS = ARSECTION + 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            QDSECTION,
            ANSECTION,
            NSSECTION,
            ARSECTION,
    })
    public @interface RecordType {}


    private static final String TAG = DnsPacket.class.getSimpleName();

    protected final DnsHeader mHeader;
    protected final List<DnsRecord>[] mRecords;

    protected DnsPacket(@NonNull byte[] data) throws ParseException {
        if (null == data) {
            throw new ParseException("Parse header failed, null input data");
        }

        final ByteBuffer buffer;
        try {
            buffer = ByteBuffer.wrap(data);
            mHeader = new DnsHeader(buffer);
        } catch (BufferUnderflowException e) {
            throw new ParseException("Parse Header fail, bad input data", e);
        }

        mRecords = new ArrayList[NUM_SECTIONS];

        for (int i = 0; i < NUM_SECTIONS; ++i) {
            final int count = mHeader.getRecordCount(i);
            mRecords[i] = new ArrayList(count);
            for (int j = 0; j < count; ++j) {
                try {
                    mRecords[i].add(DnsRecord.parse(i, buffer));
                } catch (BufferUnderflowException e) {
                    throw new ParseException("Parse record fail", e);
                }
            }
        }
    }

    /**
     * Create a new {@link #DnsPacket} from specified parameters.
     *
     * Note that authority records section and additional records section is not supported.
     */
    protected DnsPacket(@NonNull DnsHeader header, @NonNull List<DnsRecord> qd,
            @NonNull List<DnsRecord> an) {
        mHeader = Objects.requireNonNull(header);
        mRecords = new List[NUM_SECTIONS];
        mRecords[QDSECTION] = Collections.unmodifiableList(new ArrayList<>(qd));
        mRecords[ANSECTION] = Collections.unmodifiableList(new ArrayList<>(an));
        mRecords[NSSECTION] = new ArrayList<>();
        mRecords[ARSECTION] = new ArrayList<>();
        for (int i = 0; i < NUM_SECTIONS; i++) {
            if (mHeader.mRecordCount[i] != mRecords[i].size()) {
                throw new IllegalArgumentException("Record count mismatch: expected "
                        + mHeader.mRecordCount[i] + " but was " + mRecords[i]);
            }
        }
    }

    /**
     * Get DnsPacket as byte array.
     */
    public @NonNull byte[] getBytes() throws IOException {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(mHeader.getBytes());

        for (int i = 0; i < NUM_SECTIONS; ++i) {
            for (final DnsRecord record : mRecords[i]) {
                buf.write(record.getBytes());
            }
        }
        return buf.toByteArray();
    }

    @Override
    public String toString() {
        return "DnsPacket{" + "header=" + mHeader + ", records='" + Arrays.toString(mRecords) + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o.getClass() != getClass()) return false;
        final DnsPacket other = (DnsPacket) o;
        return Objects.equals(mHeader, other.mHeader)
                && Arrays.deepEquals(mRecords, other.mRecords);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mHeader);
        result = 31 * result + Arrays.hashCode(mRecords);
        return result;
    }
}
