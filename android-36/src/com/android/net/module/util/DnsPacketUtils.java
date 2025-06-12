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
import android.annotation.Nullable;
import android.net.InetAddresses;
import android.net.ParseException;
import android.text.TextUtils;
import android.util.Patterns;

import com.android.internal.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
        private static final int MAXNAMESIZE = 255;
        private static final int MAXLABELCOUNT = 128;

        private static final DecimalFormat sByteFormat = new DecimalFormat();
        private static final FieldPosition sPos = new FieldPosition(0);

        /**
         * Convert label from {@code byte[]} to {@code String}
         *
         * <p>Follows the same conversion rules of the native code (ns_name.c in libc).
         */
        @VisibleForTesting
        static String labelToString(@NonNull byte[] label) {
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
         * Converts domain name to labels according to RFC 1035.
         *
         * @param name Domain name as String that needs to be converted to labels.
         * @return An encoded byte array that is constructed out of labels,
         *         and ends with zero-length label.
         * @throws ParseException if failed to parse the given domain name or
         *         IOException if failed to output labels.
         */
        public static @NonNull byte[] domainNameToLabels(@NonNull String name) throws
                IOException, ParseException {
            if (name.length() > MAXNAMESIZE) {
                throw new ParseException("Domain name exceeds max length: " + name.length());
            }
            if (!isHostName(name)) {
                throw new ParseException("Failed to parse domain name: " + name);
            }
            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            final String[] labels = name.split("\\.");
            for (final String label : labels) {
                if (label.length() > MAXLABELSIZE) {
                    throw new ParseException("label is too long: " + label);
                }
                buf.write(label.length());
                // Encode as UTF-8 as suggested in RFC 6055 section 3.
                buf.write(label.getBytes(StandardCharsets.UTF_8));
            }
            buf.write(0x00); // end with zero-length label
            return buf.toByteArray();
        }

        /**
         * Check whether the input is a valid hostname based on rfc 1035 section 3.3.
         *
         * @param hostName the target host name.
         * @return true if the input is a valid hostname.
         */
        public static boolean isHostName(@Nullable String hostName) {
            // TODO: Use {@code Patterns.HOST_NAME} if available.
            // Patterns.DOMAIN_NAME accepts host names or IP addresses, so reject
            // IP addresses.
            return hostName != null
                    && Patterns.DOMAIN_NAME.matcher(hostName).matches()
                    && !InetAddresses.isNumericAddress(hostName);
        }

        /**
         * Parses the domain / target name of a DNS record.
         */
        public static String parseName(final ByteBuffer buf, int depth,
                boolean isNameCompressionSupported) throws
                BufferUnderflowException, DnsPacket.ParseException {
            return parseName(buf, depth, MAXLABELCOUNT, isNameCompressionSupported);
        }

        /**
         * Parses the domain / target name of a DNS record.
         *
         * As described in RFC 1035 Section 4.1.3, the NAME field of a DNS Resource Record always
         * supports Name Compression, whereas domain names contained in the RDATA payload of a DNS
         * record may or may not support Name Compression, depending on the record TYPE. Moreover,
         * even if Name Compression is supported, its usage is left to the implementation.
         */
        public static String parseName(final ByteBuffer buf, int depth, int maxLabelCount,
                boolean isNameCompressionSupported) throws
                BufferUnderflowException, DnsPacket.ParseException {
            if (depth > maxLabelCount) {
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
                final String pointed = parseName(buf, depth + 1, maxLabelCount,
                        isNameCompressionSupported);
                buf.position(oldPos);
                return pointed;
            } else {
                final byte[] label = new byte[len];
                buf.get(label);
                final String head = labelToString(label);
                if (head.length() > MAXLABELSIZE) {
                    throw new DnsPacket.ParseException("Parse name fail, invalid label length");
                }
                final String tail = parseName(buf, depth + 1, maxLabelCount,
                        isNameCompressionSupported);
                return TextUtils.isEmpty(tail) ? head : head + "." + tail;
            }
        }

        private DnsRecordParser() {}
    }

    private DnsPacketUtils() {}
}
