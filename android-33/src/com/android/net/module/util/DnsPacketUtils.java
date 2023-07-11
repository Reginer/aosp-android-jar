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

import static com.android.net.module.util.DnsPacket.DnsRecord.NAME_COMPRESSION;
import static com.android.net.module.util.DnsPacket.DnsRecord.NAME_NORMAL;

import android.annotation.NonNull;
import android.text.TextUtils;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.FieldPosition;

/**
 * Utilities for decoding the contents of a DnsPacket.
 *
 * @hide
 */
public final class DnsPacketUtils {
    /**
     * Reads the passed ByteBuffer from its current position and decodes a DNS record.
     */
    public static class DnsRecordParser {
        private static final int MAXLABELSIZE = 63;
        private static final int MAXLABELCOUNT = 128;

        private static final DecimalFormat sByteFormat = new DecimalFormat();
        private static final FieldPosition sPos = new FieldPosition(0);

        /**
         * Convert label from {@code byte[]} to {@code String}
         *
         * <p>Follows the same conversion rules of the native code (ns_name.c in libc).
         */
        private static String labelToString(@NonNull byte[] label) {
            final StringBuffer sb = new StringBuffer();

            for (int i = 0; i < label.length; ++i) {
                int b = Byte.toUnsignedInt(label[i]);
                // Control characters and non-ASCII characters.
                if (b <= 0x20 || b >= 0x7f) {
                    // Append the byte as an escaped decimal number, e.g., "\19" for 0x13.
                    sb.append('\\');
                    sByteFormat.format(b, sb, sPos);
                } else if (b == '"' || b == '.' || b == ';' || b == '\\' || b == '(' || b == ')'
                        || b == '@' || b == '$') {
                    // Append the byte as an escaped character, e.g., "\:" for 0x3a.
                    sb.append('\\');
                    sb.append((char) b);
                } else {
                    // Append the byte as a character, e.g., "a" for 0x61.
                    sb.append((char) b);
                }
            }
            return sb.toString();
        }

        /**
         * Parses the domain / target name of a DNS record.
         *
         * As described in RFC 1035 Section 4.1.3, the NAME field of a DNS Resource Record always
         * supports Name Compression, whereas domain names contained in the RDATA payload of a DNS
         * record may or may not support Name Compression, depending on the record TYPE. Moreover,
         * even if Name Compression is supported, its usage is left to the implementation.
         */
        public static String parseName(ByteBuffer buf, int depth,
                boolean isNameCompressionSupported) throws
                BufferUnderflowException, DnsPacket.ParseException {
            if (depth > MAXLABELCOUNT) {
                throw new DnsPacket.ParseException("Failed to parse name, too many labels");
            }
            final int len = Byte.toUnsignedInt(buf.get());
            final int mask = len & NAME_COMPRESSION;
            if (0 == len) {
                return "";
            } else if (mask != NAME_NORMAL && mask != NAME_COMPRESSION
                    || (!isNameCompressionSupported && mask == NAME_COMPRESSION)) {
                throw new DnsPacket.ParseException("Parse name fail, bad label type: " + mask);
            } else if (mask == NAME_COMPRESSION) {
                // Name compression based on RFC 1035 - 4.1.4 Message compression
                final int offset = ((len & ~NAME_COMPRESSION) << 8) + Byte.toUnsignedInt(buf.get());
                final int oldPos = buf.position();
                if (offset >= oldPos - 2) {
                    throw new DnsPacket.ParseException(
                            "Parse compression name fail, invalid compression");
                }
                buf.position(offset);
                final String pointed = parseName(buf, depth + 1, isNameCompressionSupported);
                buf.position(oldPos);
                return pointed;
            } else {
                final byte[] label = new byte[len];
                buf.get(label);
                final String head = labelToString(label);
                if (head.length() > MAXLABELSIZE) {
                    throw new DnsPacket.ParseException("Parse name fail, invalid label length");
                }
                final String tail = parseName(buf, depth + 1, isNameCompressionSupported);
                return TextUtils.isEmpty(tail) ? head : head + "." + tail;
            }
        }

        private DnsRecordParser() {}
    }

    private DnsPacketUtils() {}
}
