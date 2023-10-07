package com.android.networkstack.tethering.companionproxy.protocol;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

import java.io.IOException;

/**
 * Control message exchanged during handshake.
 *
 * @hide
 */
final class HandshakeData {
    private static final int TAG_VERSION = 1;
    private static final int TAG_CAPABILITIES = 2;
    private static final int TAG_MAX_RX_WINDOWS_SIZE = 3;
    private static final int TAG_MAX_TX_WINDOWS_SIZE = 4;

    static final int PROTOCOL_VERSION_V1 = 1;

    static final int CAPABILITY_NET_ID                  = 0x01;
    static final int CAPABILITY_TCP4_HEADER_COMPRESSION = 0x02;
    static final int CAPABILITY_TCP6_HEADER_COMPRESSION = 0x04;
    static final int CAPABILITY_UDP4_HEADER_COMPRESSION = 0x08;
    static final int CAPABILITY_UDP6_HEADER_COMPRESSION = 0x10;

    /** Protocol version, such as PROTOCOL_VERSION_V1. */
    int version;

    /** Bitmask of CAPABILITY_ values. */
    int capabilities;

    /** Maximum unacknowledged packets, which can be received. */
    int maxRxWindowSize;

    /** Maximum unacknowledged packets, which can be sent. */
    int maxTxWindowSize;

    HandshakeData() {}

    HandshakeData(HandshakeData src) {
        version = src.version;
        capabilities = src.capabilities;
        maxRxWindowSize = src.maxRxWindowSize;
        maxTxWindowSize = src.maxTxWindowSize;
    }

    static HandshakeData parseFrom(CodedInputStream in) throws IOException {
        HandshakeData result = new HandshakeData();
        while (true) {
            switch (WireFormat.getTagFieldNumber(in.readTag())) {
                case 0:
                    return result;
                case TAG_VERSION:
                    result.version = in.readUInt32();
                    break;
                case TAG_CAPABILITIES:
                    result.capabilities = in.readUInt32();
                    break;
                case TAG_MAX_RX_WINDOWS_SIZE:
                    result.maxRxWindowSize = in.readUInt32();
                    break;
                case TAG_MAX_TX_WINDOWS_SIZE:
                    result.maxTxWindowSize = in.readUInt32();
                    break;
                default:
                    in.skipField(in.getLastTag());
                    break;
            }
        }
    }

    void serializeTo(CodedOutputStream out) throws IOException {
        out.writeUInt32(TAG_VERSION, version);
        out.writeUInt32(TAG_CAPABILITIES, capabilities);
        out.writeUInt32(TAG_MAX_RX_WINDOWS_SIZE, maxRxWindowSize);
        out.writeUInt32(TAG_MAX_TX_WINDOWS_SIZE, maxTxWindowSize);
    }
}
