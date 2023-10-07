package com.android.networkstack.tethering.companionproxy.protocol;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import java.io.IOException;
import java.util.Locale;

/**
 * Decodes packet objects from the given packet data bytes.
 *
 * @hide
 */
final class PacketDecoder {
    /**
     * Represents a single decoded packet.
     * @hide
     */
    static class Packet {
        enum Type { UNDEFINED, CONTROL, DATA }

        Type type;

        int controlPacketType;
        HandshakeData handshakeData;
        NetworkConfig networkConfig;
        LinkUsageStats linkUsageStats;

        int dataSn;
        int dataPayloadPos;
        int dataPayloadLen;
        boolean isRequestingAck;

        boolean dataHasAck;
        int dataAckSn;

        private int totalLen;
        private int headerByte;

        private Packet() {
            clear();
        }

        private void clear() {
            type = Type.UNDEFINED;

            controlPacketType = -1;
            handshakeData = null;
            networkConfig = null;
            linkUsageStats = null;

            dataSn = -1;
            dataPayloadPos = -1;
            dataPayloadLen = -1;
            isRequestingAck = false;

            dataHasAck = false;
            dataAckSn = -1;

            totalLen = 0;
            headerByte = 0;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Packet{type=");
            sb.append(getTypeName(headerByte));
            sb.append(",hdr=0x");
            sb.append(Integer.toHexString(headerByte & 0xFF).toUpperCase(Locale.US));
            sb.append(",len=");
            sb.append(totalLen);

            if (type == Type.DATA) {
                if (dataHasAck) {
                    sb.append(",asn=");
                    sb.append(dataAckSn);
                }
                if (isRequestingAck) {
                    sb.append(",ack_req");
                }
                if (dataPayloadLen > 0) {
                    sb.append(",dsn=");
                    sb.append(dataSn);
                    sb.append(",dataLen=");
                    sb.append(dataPayloadLen);
                }
            }

            sb.append('}');
            return sb.toString();
        }
    }

    private final Packet mCurrentPacket = new Packet();
    private String mStreamErrorMessage;

    PacketDecoder() {}

    /**
     * Returns non-null description if there was an error.
     * Once set, the error state never goes away.
     */
    String getStreamErrorMessage() { return mStreamErrorMessage; }

    /**
     * Decodes one packet from the given byte buffer.
     * Returns the same cached Packet instance on each invocation,
     * the caller should not assume ownership of the returned object.
     */
    Packet readOnePacket(byte[] data, int pos, int len) {
        mCurrentPacket.clear();

        if (mStreamErrorMessage != null || len <= 0) {
            return null;
        }

        mCurrentPacket.totalLen = len;
        mCurrentPacket.headerByte = data[pos++] & 0xFF;
        len--;

        final int headerTypeBits = mCurrentPacket.headerByte & PacketEncoder.HEADER_TYPE_MASK;
        if (headerTypeBits == PacketEncoder.HEADER_TYPE_CONTROL) {
            mCurrentPacket.type = Packet.Type.CONTROL;
            mCurrentPacket.controlPacketType =
                (byte) (mCurrentPacket.headerByte & PacketEncoder.HEADER_CONTROL_MASK);
            return parseControlPacket(data, pos, len);
        }

        if (headerTypeBits == PacketEncoder.HEADER_TYPE_DATA_ACK) {
            mCurrentPacket.dataHasAck = true;
            mCurrentPacket.dataAckSn =
                (byte) (mCurrentPacket.headerByte & PacketEncoder.HEADER_SEQ_MASK);
            if (len == 0) {
                mCurrentPacket.type = Packet.Type.DATA;
                mCurrentPacket.dataPayloadLen = 0;
                return mCurrentPacket;
            }

            len--;
            mCurrentPacket.headerByte = data[pos++] & 0xFF;
            // Intentional fallthrough - ACK may be prepended to a data packet.
        }

        if (headerTypeBits == PacketEncoder.HEADER_TYPE_DATA
                || headerTypeBits == PacketEncoder.HEADER_TYPE_DATA_REQ_ACK) {
            mCurrentPacket.type = Packet.Type.DATA;
            mCurrentPacket.dataSn =
                (byte) (mCurrentPacket.headerByte & PacketEncoder.HEADER_SEQ_MASK);
            mCurrentPacket.isRequestingAck =
                (headerTypeBits == PacketEncoder.HEADER_TYPE_DATA_REQ_ACK);
            mCurrentPacket.dataPayloadPos = pos;
            mCurrentPacket.dataPayloadLen = len;
            return mCurrentPacket;
        }

        return reportError("Unexpected header type");
    }

    private Packet parseControlPacket(byte[] data, int pos, int len) {
        if (len > PacketEncoder.CONTROL_PACKET_MAX_PROTO_LENGTH) {
            return reportError("ControlPacket too large");
        }

        try {
            CodedInputStream input = CodedInputStream.newInstance(data, pos, len);
            switch (mCurrentPacket.controlPacketType) {
                case PacketEncoder.HEADER_CONTROL_RESET:
                    return parseResetData(input);
                case PacketEncoder.HEADER_CONTROL_HANDSHAKE_START:
                case PacketEncoder.HEADER_CONTROL_HANDSHAKE_ACK:
                case PacketEncoder.HEADER_CONTROL_HANDSHAKE_DONE:
                    mCurrentPacket.handshakeData = HandshakeData.parseFrom(input);
                    return mCurrentPacket;
                case PacketEncoder.HEADER_CONTROL_NETWORK_CONFIG:
                    mCurrentPacket.networkConfig = NetworkConfig.parseFrom(input);
                    return mCurrentPacket;
                case PacketEncoder.HEADER_CONTROL_LINK_USAGE_STATS:
                    mCurrentPacket.linkUsageStats = LinkUsageStats.parseFrom(input);
                    return mCurrentPacket;
                default:
                    return reportError("Unknown ControlPacket type");
            }
        } catch (IOException e) {
            return reportError("Unable to parse ControlPacket: " + e);
        }
    }

    private Packet parseResetData(CodedInputStream in) throws IOException {
        while (true) {
            switch (WireFormat.getTagFieldNumber(in.readTag())) {
                case 0:
                    return mCurrentPacket;
                default:
                    in.skipField(in.getLastTag());
                    break;
            }
        }
    }

    private Packet reportError(String msg) {
        mStreamErrorMessage = "Error decoding: '" + msg + "', " + mCurrentPacket;
        mCurrentPacket.clear();
        return null;
    }

    /** Converts header byte to a debug string. */
    static String getTypeName(int type) {
        if ((type & PacketEncoder.HEADER_TYPE_MASK) == PacketEncoder.HEADER_TYPE_CONTROL) {
            switch (type & PacketEncoder.HEADER_CONTROL_MASK) {
                case PacketEncoder.HEADER_CONTROL_RESET:
                    return "RESET";
                case PacketEncoder.HEADER_CONTROL_HANDSHAKE_START:
                    return "HANDSHAKE_START";
                case PacketEncoder.HEADER_CONTROL_HANDSHAKE_ACK:
                    return "HANDSHAKE_ACK";
                case PacketEncoder.HEADER_CONTROL_HANDSHAKE_DONE:
                    return "HANDSHAKE_DONE";
                case PacketEncoder.HEADER_CONTROL_NETWORK_CONFIG:
                    return "NETWORK_CONFIG";
                case PacketEncoder.HEADER_CONTROL_LINK_USAGE_STATS:
                    return "LINK_USAGE_STATS";
                default:
                    return "0x" + Integer.toHexString(type & 0xFF).toUpperCase(Locale.US);
            }
        }

        switch (type & PacketEncoder.HEADER_TYPE_MASK) {
            case PacketEncoder.HEADER_TYPE_DATA:
                return "DATA";
            case PacketEncoder.HEADER_TYPE_DATA_REQ_ACK:
                return "DATA_REQ_ACK";
            case PacketEncoder.HEADER_TYPE_DATA_ACK:
                return "DATA_ACK";
            default:
                return "0x" + Integer.toHexString(type & 0xFF).toUpperCase(Locale.US);
        }
    }
}
