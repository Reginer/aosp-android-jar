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

import android.annotation.SuppressLint;
import android.net.IpPrefix;
import android.util.Log;

import androidx.annotation.NonNull;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * The PREF64 router advertisement option. RFC 8781.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Type      |    Length     |     Scaled Lifetime     | PLC |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * +                                                               +
 * |              Highest 96 bits of the Prefix                    |
 * +                                                               +
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 */
public class StructNdOptPref64 extends NdOption {
    public static final int STRUCT_SIZE = 16;
    public static final int TYPE = 38;
    public static final byte LENGTH = 2;

    private static final String TAG = StructNdOptPref64.class.getSimpleName();

    /**
     * How many seconds the prefix is expected to remain valid.
     * Valid values are from 0 to 65528 in multiples of 8.
     */
    public final int lifetime;
    /** The NAT64 prefix. */
    @NonNull public final IpPrefix prefix;

    static int plcToPrefixLength(int plc) {
        switch (plc) {
            case 0: return 96;
            case 1: return 64;
            case 2: return 56;
            case 3: return 48;
            case 4: return 40;
            case 5: return 32;
            default:
                throw new IllegalArgumentException("Invalid prefix length code " + plc);
        }
    }

    static int prefixLengthToPlc(int prefixLength) {
        switch (prefixLength) {
            case 96: return 0;
            case 64: return 1;
            case 56: return 2;
            case 48: return 3;
            case 40: return 4;
            case 32: return 5;
            default:
                throw new IllegalArgumentException("Invalid prefix length " + prefixLength);
        }
    }

    /**
     * Returns the 2-byte "scaled lifetime and prefix length code" field: 13-bit lifetime, 3-bit PLC
     */
    static short getScaledLifetimePlc(int lifetime, int prefixLengthCode) {
        return (short) ((lifetime & 0xfff8) | (prefixLengthCode & 0x7));
    }

    public StructNdOptPref64(@NonNull IpPrefix prefix, int lifetime) {
        super((byte) TYPE, LENGTH);

        Objects.requireNonNull(prefix, "prefix must not be null");
        if (!(prefix.getAddress() instanceof Inet6Address)) {
            throw new IllegalArgumentException("Must be an IPv6 prefix: " + prefix);
        }
        prefixLengthToPlc(prefix.getPrefixLength());  // Throw if the prefix length is invalid.
        this.prefix = prefix;

        if (lifetime < 0 || lifetime > 0xfff8) {
            throw new IllegalArgumentException("Invalid lifetime " + lifetime);
        }
        this.lifetime = lifetime & 0xfff8;
    }

    @SuppressLint("NewApi")
    private StructNdOptPref64(@NonNull ByteBuffer buf) {
        super(buf.get(), Byte.toUnsignedInt(buf.get()));
        if (type != TYPE) throw new IllegalArgumentException("Invalid type " + type);
        if (length != LENGTH) throw new IllegalArgumentException("Invalid length " + length);

        int scaledLifetimePlc = Short.toUnsignedInt(buf.getShort());
        lifetime = scaledLifetimePlc & 0xfff8;

        byte[] addressBytes = new byte[16];
        buf.get(addressBytes, 0, 12);
        InetAddress addr;
        try {
            addr = InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError("16-byte array not valid InetAddress?");
        }
        prefix = new IpPrefix(addr, plcToPrefixLength(scaledLifetimePlc & 7));
    }

    /**
     * Parses an option from a {@link ByteBuffer}.
     *
     * @param buf The buffer from which to parse the option. The buffer's byte order must be
     *            {@link java.nio.ByteOrder#BIG_ENDIAN}.
     * @return the parsed option, or {@code null} if the option could not be parsed successfully
     *         (for example, if it was truncated, or if the prefix length code was wrong).
     */
    public static StructNdOptPref64 parse(@NonNull ByteBuffer buf) {
        if (buf.remaining() < STRUCT_SIZE) return null;
        try {
            return new StructNdOptPref64(buf);
        } catch (IllegalArgumentException e) {
            // Not great, but better than throwing an exception that might crash the caller.
            // Convention in this package is that null indicates that the option was truncated, so
            // callers must already handle it.
            Log.d(TAG, "Invalid PREF64 option: " + e);
            return null;
        }
    }

    protected void writeToByteBuffer(ByteBuffer buf) {
        super.writeToByteBuffer(buf);
        buf.putShort(getScaledLifetimePlc(lifetime,  prefixLengthToPlc(prefix.getPrefixLength())));
        buf.put(prefix.getRawAddress(), 0, 12);
    }

    /** Outputs the wire format of the option to a new big-endian ByteBuffer. */
    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(STRUCT_SIZE);
        writeToByteBuffer(buf);
        buf.flip();
        return buf;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("NdOptPref64(%s, %d)", prefix, lifetime);
    }
}
