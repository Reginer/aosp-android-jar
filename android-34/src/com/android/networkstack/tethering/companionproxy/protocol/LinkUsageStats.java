package com.android.networkstack.tethering.companionproxy.protocol;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

import java.io.IOException;

/**
 * Control message with information on uplink usage.
 *
 * The intention is to help companion device make its decisions on power consumption.
 *
 * @hide
 */
public final class LinkUsageStats {
    private static final int TAG_HAS_BUFFERS_ABOVE_THRESHOLD = 1;

    /** Indicates whether the sender has "significant" number of buffered packets. */
    public boolean hasBuffersAboveThreshold;

    public LinkUsageStats() {}

    public LinkUsageStats(LinkUsageStats src) {
        hasBuffersAboveThreshold = src.hasBuffersAboveThreshold;
    }

    static LinkUsageStats parseFrom(CodedInputStream in) throws IOException {
        LinkUsageStats result = new LinkUsageStats();
        while (true) {
            switch (WireFormat.getTagFieldNumber(in.readTag())) {
                case 0:
                    return result;
                case TAG_HAS_BUFFERS_ABOVE_THRESHOLD:
                    result.hasBuffersAboveThreshold = in.readBool();
                    break;
                default:
                    in.skipField(in.getLastTag());
                    break;
            }
        }
    }

    void serializeTo(CodedOutputStream out) throws IOException {
        out.writeBool(TAG_HAS_BUFFERS_ABOVE_THRESHOLD, hasBuffersAboveThreshold);
    }
}
