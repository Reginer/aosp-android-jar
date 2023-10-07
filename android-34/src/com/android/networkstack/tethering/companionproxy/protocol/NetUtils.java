package com.android.networkstack.tethering.companionproxy.protocol;

import com.android.networkstack.tethering.companionproxy.util.ReadableByteBuffer;

/**
 * Implements utilities for decoding parts of TCP/UDP/IP headers.
 *
 * @hide
 */
final class NetUtils {
    static void encodeNetworkUnsignedInt16(int value, byte[] dst, final int dstPos) {
        dst[dstPos] = (byte) ((value >> 8) & 0xFF);
        dst[dstPos + 1] = (byte) (value & 0xFF);
    }

    static int decodeNetworkUnsignedInt16(byte[] data, final int pos) {
        return ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
    }

    static int decodeNetworkUnsignedInt16(ReadableByteBuffer data, final int pos) {
        return ((data.peek(pos) & 0xFF) << 8) | (data.peek(pos + 1) & 0xFF);
    }

    private NetUtils() {}
}
