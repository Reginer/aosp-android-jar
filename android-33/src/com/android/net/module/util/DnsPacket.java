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

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.net.module.util.DnsPacketUtils.DnsRecordParser;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines basic data for DNS protocol based on RFC 1035.
 * Subclasses create the specific format used in DNS packet.
 *
 * @hide
 */
public abstract class DnsPacket {
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
     * DNS header for DNS protocol based on RFC 1035.
     */
    public class DnsHeader {
        private static final String TAG = "DnsHeader";
        public final int id;
        public final int flags;
        public final int rcode;
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
        DnsHeader(@NonNull ByteBuffer buf) throws BufferUnderflowException {
            id = Short.toUnsignedInt(buf.getShort());
            flags = Short.toUnsignedInt(buf.getShort());
            rcode = flags & 0xF;
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
            return (flags & (1 << FLAGS_SECTION_QR_BIT)) != 0;
        }

        /**
         * Get record count by type.
         */
        public int getRecordCount(int type) {
            return mRecordCount[type];
        }
    }

    /**
     * Superclass for DNS questions and DNS resource records.
     *
     * DNS questions (No TTL/RDATA)
     * DNS resource records (With TTL/RDATA)
     */
    public class DnsRecord {
        private static final int MAXNAMESIZE = 255;
        public static final int NAME_NORMAL = 0;
        public static final int NAME_COMPRESSION = 0xC0;

        private static final String TAG = "DnsRecord";

        public final String dName;
        public final int nsType;
        public final int nsClass;
        public final long ttl;
        private final byte[] mRdata;

        /**
         * Create a new DnsRecord from a positioned ByteBuffer.
         *
         * Reads the passed ByteBuffer from its current position and decodes a DNS record.
         * When this constructor returns, the reading position of the ByteBuffer has been
         * advanced to the end of the DNS header record.
         * This is meant to chain with other methods reading a DNS response in sequence.
         *
         * @param buf ByteBuffer input of record, must be in network byte order
         *         (which is the default).
         */
        DnsRecord(int recordType, @NonNull ByteBuffer buf)
                throws BufferUnderflowException, ParseException {
            dName = DnsRecordParser.parseName(buf, 0 /* Parse depth */,
                    /* isNameCompressionSupported= */ true);
            if (dName.length() > MAXNAMESIZE) {
                throw new ParseException(
                        "Parse name fail, name size is too long: " + dName.length());
            }
            nsType = Short.toUnsignedInt(buf.getShort());
            nsClass = Short.toUnsignedInt(buf.getShort());

            if (recordType != QDSECTION) {
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
         * Get a copy of rdata.
         */
        @Nullable
        public byte[] getRR() {
            return (mRdata == null) ? null : mRdata.clone();
        }

    }

    public static final int QDSECTION = 0;
    public static final int ANSECTION = 1;
    public static final int NSSECTION = 2;
    public static final int ARSECTION = 3;
    private static final int NUM_SECTIONS = ARSECTION + 1;

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
            if (count > 0) {
                mRecords[i] = new ArrayList(count);
            }
            for (int j = 0; j < count; ++j) {
                try {
                    mRecords[i].add(new DnsRecord(i, buffer));
                } catch (BufferUnderflowException e) {
                    throw new ParseException("Parse record fail", e);
                }
            }
        }
    }
}
