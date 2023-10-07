package com.android.networkstack.tethering.companionproxy.protocol;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Describes active uplinks of the tethering device.
 *
 * @hide
 */
public final class NetworkConfig {
    private static final int TAG_LINKS = 1;

    /**
     * Describes one active uplink of the tethering device.
     *
     * @hide
     */
    public static final class LinkInfo {
        private static final int TAG_NET_ID = 1;
        private static final int TAG_TRANSPORTS = 2;
        private static final int TAG_CAPABILITIES = 3;

        public int netId;
        public long transports;
        public long capabilities;

        public LinkInfo() {}

        public LinkInfo(LinkInfo src) {
            netId = src.netId;
            transports = src.transports;
            capabilities = src.capabilities;
        }

        /** Checks whether a given transport is available. */
        public boolean hasTransport(int transport) {
            return (transports & (1L << transport)) != 0;
        }

        /** Adds a given transport to the list. */
        public void addTransport(int transport) {
            transports |= (1L << transport);
        }

        /** Checks whether a given capability is available. */
        public boolean hasCapability(int capability) {
            return (capabilities & (1L << capability)) != 0;
        }

        /** Adds a given capability to the list. */
        public void addCapability(int capability) {
            capabilities |= (1L << capability);
        }

        static LinkInfo parseFrom(CodedInputStream in) throws IOException {
            LinkInfo result = new LinkInfo();
            while (true) {
                switch (WireFormat.getTagFieldNumber(in.readTag())) {
                    case 0:
                        return result;
                    case TAG_NET_ID:
                        result.netId = in.readUInt32();
                        break;
                    case TAG_TRANSPORTS:
                        result.transports = in.readUInt64();
                        break;
                    case TAG_CAPABILITIES:
                        result.capabilities = in.readUInt64();
                        break;
                    default:
                        in.skipField(in.getLastTag());
                        break;
                }
            }
        }

        void serializeTo(CodedOutputStream out) throws IOException {
            out.writeUInt32(TAG_NET_ID, netId);
            out.writeUInt64(TAG_TRANSPORTS, transports);
            out.writeUInt64(TAG_CAPABILITIES, capabilities);
        }

        private int computeSerializedSize() {
            return CodedOutputStream.computeUInt32Size(TAG_NET_ID, netId)
                + CodedOutputStream.computeUInt64Size(TAG_TRANSPORTS, transports)
                + CodedOutputStream.computeUInt64Size(TAG_CAPABILITIES, capabilities);
        }
    }

    /** Contains the list of uplinks. */
    public final ArrayList<LinkInfo> links = new ArrayList<>();

    public NetworkConfig() {}

    public NetworkConfig(NetworkConfig src) {
        for (LinkInfo linkInfo : src.links) {
            links.add(new LinkInfo(linkInfo));
        }
    }

    void serializeTo(CodedOutputStream out) throws IOException {
        for (LinkInfo link : links) {
            out.writeTag(TAG_LINKS, WireFormat.WIRETYPE_LENGTH_DELIMITED);
            out.writeUInt32NoTag(link.computeSerializedSize());
            link.serializeTo(out);
        }
    }

    static NetworkConfig parseFrom(CodedInputStream in) throws IOException {
        NetworkConfig result = new NetworkConfig();
        while (true) {
            switch (WireFormat.getTagFieldNumber(in.readTag())) {
                case 0:
                    return result;
                case TAG_LINKS: {
                    final int length = in.readUInt32();
                    final int oldLimit = in.pushLimit(length);
                    result.links.add(LinkInfo.parseFrom(in));
                    in.popLimit(oldLimit);
                    break;
                }
                default:
                    in.skipField(in.getLastTag());
                    break;
            }
        }
    }
}
