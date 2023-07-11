/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.net.module.util.netlink;

import static android.system.OsConstants.AF_INET6;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A NetlinkMessage subclass for RTM_NEWNDUSEROPT messages.
 */
public class NduseroptMessage extends NetlinkMessage {
    public static final int STRUCT_SIZE = 16;

    static final int NDUSEROPT_SRCADDR = 1;

    /** The address family. Presumably always AF_INET6. */
    public final byte family;
    /**
     * The total length in bytes of the options that follow this structure.
     * Actually a 16-bit unsigned integer.
     */
    public final int opts_len;
    /** The interface index on which the options were received. */
    public final int ifindex;
    /** The ICMP type of the packet that contained the options. */
    public final byte icmp_type;
    /** The ICMP code of the packet that contained the options. */
    public final byte icmp_code;

    /**
     * ND option that was in this message.
     * Even though the length field is called "opts_len", the kernel only ever sends one option per
     * message. It is unlikely that this will ever change as it would break existing userspace code.
     * But if it does, we can simply update this code, since userspace is typically newer than the
     * kernel.
     */
    @Nullable
    public final NdOption option;

    /** The IP address that sent the packet containing the option. */
    public final InetAddress srcaddr;

    NduseroptMessage(@NonNull StructNlMsgHdr header, @NonNull ByteBuffer buf)
            throws UnknownHostException {
        super(header);

        // The structure itself.
        buf.order(ByteOrder.nativeOrder());  // Restored in the finally clause inside parse().
        final int start = buf.position();
        family = buf.get();
        buf.get();  // Skip 1 byte of padding.
        opts_len = Short.toUnsignedInt(buf.getShort());
        ifindex = buf.getInt();
        icmp_type = buf.get();
        icmp_code = buf.get();
        buf.position(buf.position() + 6);  // Skip 6 bytes of padding.

        // The ND option.
        // Ensure we don't read past opts_len even if the option length is invalid.
        // Note that this check is not really necessary since if the option length is not valid,
        // this struct won't be very useful to the caller.
        //
        // It's safer to pass the slice of original ByteBuffer to just parse the ND option field,
        // although parsing ND option might throw exception or return null, it won't break the
        // original ByteBuffer position.
        buf.order(ByteOrder.BIG_ENDIAN);
        try {
            final ByteBuffer slice = buf.slice();
            slice.limit(opts_len);
            option = NdOption.parse(slice);
        } finally {
            // Advance buffer position according to opts_len in the header. ND option length might
            // be incorrect in the malformed packet.
            int newPosition = start + STRUCT_SIZE + opts_len;
            if (newPosition >= buf.limit()) {
                throw new IllegalArgumentException("ND option extends past end of buffer");
            }
            buf.position(newPosition);
        }

        // The source address attribute.
        StructNlAttr nla = StructNlAttr.parse(buf);
        if (nla == null || nla.nla_type != NDUSEROPT_SRCADDR || nla.nla_value == null) {
            throw new IllegalArgumentException("Invalid source address in ND useropt");
        }
        if (family == AF_INET6) {
            // InetAddress.getByAddress only looks at the ifindex if the address type needs one.
            srcaddr = Inet6Address.getByAddress(null /* hostname */, nla.nla_value, ifindex);
        } else {
            srcaddr = InetAddress.getByAddress(nla.nla_value);
        }
    }

    /**
     * Parses a StructNduseroptmsg from a {@link ByteBuffer}.
     *
     * @param header the netlink message header.
     * @param buf The buffer from which to parse the option. The buffer's byte order must be
     *            {@link java.nio.ByteOrder#BIG_ENDIAN}.
     * @return the parsed option, or {@code null} if the option could not be parsed successfully
     *         (for example, if it was truncated, or if the prefix length code was wrong).
     */
    @Nullable
    public static NduseroptMessage parse(@NonNull StructNlMsgHdr header, @NonNull ByteBuffer buf) {
        if (buf == null || buf.remaining() < STRUCT_SIZE) return null;
        ByteOrder oldOrder = buf.order();
        try {
            return new NduseroptMessage(header, buf);
        } catch (IllegalArgumentException | UnknownHostException | BufferUnderflowException e) {
            // Not great, but better than throwing an exception that might crash the caller.
            // Convention in this package is that null indicates that the option was truncated, so
            // callers must already handle it.
            return null;
        } finally {
            buf.order(oldOrder);
        }
    }

    @Override
    public String toString() {
        return String.format("Nduseroptmsg(%d, %d, %d, %d, %d, %s)",
                family, opts_len, ifindex, Byte.toUnsignedInt(icmp_type),
                Byte.toUnsignedInt(icmp_code), srcaddr.getHostAddress());
    }
}
