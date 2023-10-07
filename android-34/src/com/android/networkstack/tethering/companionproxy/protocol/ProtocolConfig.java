package com.android.networkstack.tethering.companionproxy.protocol;

import java.util.Locale;

/**
 * Encapsulates configuration of the current connection with the peer.
 *
 * @hide
 */
final class ProtocolConfig {
    /**
     * Indicates protocol features enabled in the current connection.
     *
     * @hide
     */
    static final class Capabilities {
        private final int mBitmask;

        private static final int SUPPORTED_CAPABILITIES =
            HandshakeData.CAPABILITY_NET_ID
                | HandshakeData.CAPABILITY_TCP4_HEADER_COMPRESSION
                | HandshakeData.CAPABILITY_TCP6_HEADER_COMPRESSION
                | HandshakeData.CAPABILITY_UDP4_HEADER_COMPRESSION
                | HandshakeData.CAPABILITY_UDP6_HEADER_COMPRESSION;

        Capabilities(int bitmask) {
            mBitmask = bitmask & SUPPORTED_CAPABILITIES;
        }

        int getBitmask() {
            return mBitmask;
        }

        boolean hasNetId() {
            return (mBitmask & HandshakeData.CAPABILITY_NET_ID) != 0;
        }

        boolean hasTcp4Compression() {
            return (mBitmask & HandshakeData.CAPABILITY_TCP4_HEADER_COMPRESSION) != 0;
        }

        boolean hasTcp6Compression() {
            return (mBitmask & HandshakeData.CAPABILITY_TCP6_HEADER_COMPRESSION) != 0;
        }

        boolean hasUdp4Compression() {
            return (mBitmask & HandshakeData.CAPABILITY_UDP4_HEADER_COMPRESSION) != 0;
        }

        boolean hasUdp6Compression() {
            return (mBitmask & HandshakeData.CAPABILITY_UDP6_HEADER_COMPRESSION) != 0;
        }

        static Capabilities merge(Capabilities local, Capabilities remote) {
            return new Capabilities(local.getBitmask() & remote.getBitmask());
        }
    }

    /** Protocol version, such as PROTOCOL_VERSION_V1. */
    final int protocolVersion;

    /** Protocol features enabled in the current connection. */
    final Capabilities capabilities;

    /** Maximum unacknowledged packets, which can be received. */
    final int maxRxWindowSize;

    /** Maximum unacknowledged packets, which can be sent. */
    final int maxTxWindowSize;

    ProtocolConfig(int protocolVersion, Capabilities capabilities,
            int maxRxWindowSize, int maxTxWindowSize) {
        this.protocolVersion = protocolVersion;
        this.capabilities = capabilities;
        this.maxRxWindowSize = Math.max(
            Math.min(maxRxWindowSize, PacketEncoder.MAX_RX_WINDOW_SIZE), 2);
        this.maxTxWindowSize = Math.max(
            Math.min(maxTxWindowSize, PacketEncoder.MAX_TX_WINDOW_SIZE), 2);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProtocolConfig{version=");
        sb.append(protocolVersion);
        sb.append(",capabilities=");
        sb.append(Integer.toHexString(capabilities.getBitmask()).toUpperCase(Locale.US));
        sb.append(",maxRxWnd=");
        sb.append(maxRxWindowSize);
        sb.append(",maxTxWnd=");
        sb.append(maxTxWindowSize);
        sb.append('}');
        return sb.toString();
    }
}
