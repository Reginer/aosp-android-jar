package com.android.networkstack.tethering.companionproxy.bt;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

import java.io.IOException;

/**
 * Proxy configuration options sent from the listening device.
 *
 * NOTE: When adding new fields ensure the max binary encoded size of the proto
 * message remains less than or equal to 20 bytes. Sending a larger message
 * requires segmentation by the MTU size but this is not implemented for the
 * sake of simplicity.
 *
 * @hide
 */
final class ProxyConfig {
    private static final int TAG_PSM_VALUE = 1;
    private static final int TAG_CHANNEL_CHANGE_ID = 2;

    /** L2CAP PSM value. */
    int psmValue;

    /**
     * Identifies the proxy L2CAP channel change event.
     *
     * When the iOS companion publishes a new L2CAP channel for the proxy, it
     * updates this ID to indicate that the watch should reconnect even if the
     * PSM value of the new channel is the same.
     */
    int channelChangeId;

    ProxyConfig() {}

    ProxyConfig(ProxyConfig src) {
        psmValue = src.psmValue;
        channelChangeId = src.channelChangeId;
    }

    static ProxyConfig parseFrom(CodedInputStream in) throws IOException {
        ProxyConfig result = new ProxyConfig();
        while (true) {
            switch (WireFormat.getTagFieldNumber(in.readTag())) {
                case 0:
                    return result;
                case TAG_PSM_VALUE:
                    result.psmValue = in.readUInt32();
                    break;
                case TAG_CHANNEL_CHANGE_ID:
                    result.channelChangeId = in.readUInt32();
                    break;
                default:
                    in.skipField(in.getLastTag());
                    break;
            }
        }
    }

    void serializeTo(CodedOutputStream out) throws IOException {
        out.writeUInt32(TAG_PSM_VALUE, psmValue);
        out.writeUInt32(TAG_CHANNEL_CHANGE_ID, channelChangeId);
    }
}
