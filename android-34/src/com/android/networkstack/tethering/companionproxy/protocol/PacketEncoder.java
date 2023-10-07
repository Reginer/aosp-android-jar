package com.android.networkstack.tethering.companionproxy.protocol;

import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;
import com.android.networkstack.tethering.companionproxy.io.EventManager;
import com.android.networkstack.tethering.companionproxy.util.ReadableByteBuffer;
import com.google.protobuf.CodedOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Encodes outgoing packets and tracks flow control state.
 *
 * @hide
 */
final class PacketEncoder {
    private static final String TAG = LogUtils.TAG;

    static final int HEADER_TYPE_MASK           = 0x7 << 5;
    static final int HEADER_TYPE_DATA_ACK       = 0x0 << 5;
    static final int HEADER_TYPE_DATA           = 0x1 << 5;
    static final int HEADER_TYPE_DATA_REQ_ACK   = 0x2 << 5;
    static final int HEADER_TYPE_CONTROL        = 0x7 << 5;

    static final int HEADER_CONTROL_MASK = 0x1F;
    static final int HEADER_SEQ_MASK = 0x1F;

    static final int HEADER_CONTROL_RESET            = 0x0;
    static final int HEADER_CONTROL_HANDSHAKE_START  = 0x1;
    static final int HEADER_CONTROL_HANDSHAKE_ACK    = 0x2;
    static final int HEADER_CONTROL_HANDSHAKE_DONE   = 0x3;
    static final int HEADER_CONTROL_NETWORK_CONFIG   = 0x4;
    static final int HEADER_CONTROL_LINK_USAGE_STATS = 0x5;

    static final int CONTROL_PACKET_MAX_PROTO_LENGTH = 200;

    // Window size has to reflect that max sequence number is 31.
    static final int MAX_TRACKED_SEQS = 1 << 5;
    static final int MAX_RX_WINDOW_SIZE = MAX_TRACKED_SEQS - 1;
    static final int MAX_TX_WINDOW_SIZE = MAX_TRACKED_SEQS - 1;
    static final int DEFAULT_MAX_RX_WINDOW_SIZE = 8;
    static final int DEFAULT_MAX_TX_WINDOW_SIZE = 8;

    // Timeout for resending any unacked data.
    static final int DATA_PAYLOAD_RETRANSMIT_TIMEOUT = 4000;
    // Timeout for acking any inbound data.
    static final int DATA_PAYLOAD_ACK_TIMEOUT = 200;

    /** @hide */
    interface Delegate {
        boolean sendToWire(byte[] data, int pos, int len);

        boolean handleRetransmitTimeout(long elapsedTimeMs);

        void onConsumedDataSource();
    }

    private static final class ControlPacket {
        final byte type;
        final byte[] wireData;
        int wireDataLen;
        CodedOutputStream protoOutput;

        ControlPacket(int type) {
            this.type = (byte) ((type | HEADER_TYPE_CONTROL) & 0xFF);
            wireData = new byte[CONTROL_PACKET_MAX_PROTO_LENGTH + 1];
            wireData[0] = this.type;
            protoOutput = CodedOutputStream.newInstance(
                wireData, 1, CONTROL_PACKET_MAX_PROTO_LENGTH);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Packet{type=");
            sb.append(PacketDecoder.getTypeName(type));
            sb.append("/0x");
            sb.append(Integer.toHexString(type & 0xFF).toUpperCase(Locale.US));
            sb.append(",len=");
            sb.append(wireDataLen);
            sb.append('}');
            return sb.toString();
        }
    }

    private final EventManager mEventManager;
    private final Delegate mDelegate;
    private final int mMaxPacketSize;
    private final String mNameForLogging;
    private final ReadableByteBuffer mDataSource;
    private final ArrayList<ControlPacket> mControlPackets = new ArrayList<>();

    private ProtocolConfig mConfig;
    private boolean mHasStoppedDataStream;

    // Inbound data acking:
    private int mLastInboundDataSn;
    private int mInboundUnackedPacketCount;
    private boolean mShouldAckNow;
    private EventManager.Alarm mInboundDataAckAlarm;

    // Outbound fragment tracking:
    private final int[] mOutboundDataPayloadSizes = new int[MAX_TRACKED_SEQS];
    private int mNextOutboundExpectedAckSn;
    private int mNextOutboundFreeSn;
    private EventManager.Alarm mRetransmitAlarm;

    // Outbound buffering:
    private final byte[] mOutboundBuffer;
    private int mOutboundBufferSize;
    private int mOutboundBufferDataPayloadSize;
    private boolean mOutboundBufferHasAck;

    PacketEncoder(
            EventManager eventManager,
            ReadableByteBuffer dataSource,
            Delegate delegate,
            int maxPacketSize,
            ProtocolConfig config,
            String nameForLogging) {
        mEventManager = eventManager;
        mDataSource = dataSource;
        mMaxPacketSize = maxPacketSize;
        mDelegate = delegate;
        mConfig = config;
        mNameForLogging = nameForLogging;

        mOutboundBuffer = new byte[mMaxPacketSize];
    }

    void stopDataStream() {
        mHasStoppedDataStream = true;
        cancelInboundDataAckAlarm();
        cancelRetransmitAlarm();
    }

    void updateConfig(ProtocolConfig config) {
        mConfig = config;
    }

    /** Returns last data SN sent by the peer. */
    int getLastInboundDataSn() {
        return mLastInboundDataSn;
    }

    private int getOutboundDataPayloadSize(int sn) {
        return mOutboundDataPayloadSizes[sn];
    }

    private void setOutboundDataPayloadSize(int sn, int size) {
        mOutboundDataPayloadSizes[sn] = size;
    }

    private boolean isOutboundDataPayloadAwaitingAck(int sn) {
        return mOutboundDataPayloadSizes[sn] != 0;
    }

    private void clearOutboundDataPayloadSizesUpTo(int endSnExclusive) {
        for (int sn = mNextOutboundExpectedAckSn; sn != endSnExclusive; sn = getNextSn(sn)) {
            setOutboundDataPayloadSize(sn, 0);
        }
    }

    // Returns the number of packets in flight, excluding packets
    // that are pending retransmission.
    private int getNumPacketsInFlight() {
        return calculateSnDistance(mNextOutboundExpectedAckSn, mNextOutboundFreeSn);
    }

    private int getTotalNumBytesAwaitingAckUpTo(int endSnExclusive) {
        int result = 0;
        for (int sn = mNextOutboundExpectedAckSn; sn != endSnExclusive; sn = getNextSn(sn)) {
            result += getOutboundDataPayloadSize(sn);
        }
        return result;
    }

    private int getTotalNumBytesAwaitingAck() {
        return getTotalNumBytesAwaitingAckUpTo(mNextOutboundFreeSn);
    }

    static int getNextSn(int sn) {
        return (sn + 1) % MAX_TRACKED_SEQS;
    }

    static int calculateSnDistance(int startSnInclusive, int endSnExclusive) {
        return (MAX_TRACKED_SEQS + endSnExclusive - startSnInclusive) % MAX_TRACKED_SEQS;
    }

    private static byte makeHeaderByte(int type, int sn) {
        return (byte) ((type | sn) & 0xFF);
    }

    // Prepares the next packet to send, if any.
    // Fills mOutboundBuffer, mOutboundBufferSize, mOutboundBufferDataPayloadSize,
    // and mOutboundBufferHasAck.
    private void prepareOutboundBuffer() {
        mOutboundBufferSize = 0;
        mOutboundBufferDataPayloadSize = 0;
        mOutboundBufferHasAck = false;

        if (!mControlPackets.isEmpty()) {
            ControlPacket packet = mControlPackets.remove(0);
            System.arraycopy(packet.wireData, 0, mOutboundBuffer, 0, packet.wireDataLen);
            mOutboundBufferSize = packet.wireDataLen;
            return;
        }

        if (mHasStoppedDataStream) {
            return;
        }

        // ACK data before the other side is blocked waiting for an ACK.
        mOutboundBufferHasAck = mShouldAckNow;
        if (mInboundUnackedPacketCount > (mConfig.maxRxWindowSize / 2)) {
            if (LogUtils.debug()) {
                Log.d(TAG, logStr("Acking now: " + mInboundUnackedPacketCount
                        + " unacked packets > window/2"));
            }
            mOutboundBufferHasAck = true;
        }

        int maxPacketSize = mMaxPacketSize;
        if (mOutboundBufferHasAck) {
            mOutboundBuffer[0] = makeHeaderByte(HEADER_TYPE_DATA_ACK, mLastInboundDataSn);
            mOutboundBufferSize++;
            maxPacketSize--;
        }

        // After adding ACK, check for windows available for sending outbound data.
        if (getNumPacketsInFlight() >= mConfig.maxTxWindowSize) {
            return;
        }

        int dataSourceOffset = getTotalNumBytesAwaitingAck();

        int dataSize = getOutboundDataPayloadSize(mNextOutboundFreeSn);
        if (dataSize == 0) {
            // Current SN has no size recorded, so it's not a retransmit.
            // Check how much data is in the data source.
            int dataSourceSize = mDataSource.size();
            if (dataSourceSize < dataSourceOffset) {
                throw new IllegalArgumentException("DataSource contents went down in size");
            }
            dataSize = dataSourceSize - dataSourceOffset;
            if (dataSize == 0) {
                return;  // No new data.
            }
        }

        // Cap the size to the max of what the transport allows - 1 for the header itself.
        dataSize = Math.min(dataSize, maxPacketSize - 1);

        // If the entire buffer is sent out and we're waiting for the peer's ack, request peer
        // to send that ack. Otherwise the sending will get stalled until the peer sends
        // the ack using a timer.
        final int remainingSourceCapacity =
            mDataSource.capacity() - (dataSourceOffset + dataSize);
        final boolean needsAck = remainingSourceCapacity < (mMaxPacketSize / 2);

        mOutboundBuffer[mOutboundBufferSize++] = makeHeaderByte(
            needsAck ? HEADER_TYPE_DATA_REQ_ACK : HEADER_TYPE_DATA, mNextOutboundFreeSn);
        mDataSource.peekBytes(dataSourceOffset, mOutboundBuffer, mOutboundBufferSize, dataSize);
        mOutboundBufferSize += dataSize;
        mOutboundBufferDataPayloadSize = dataSize;
    }

    /** Serializes and sends packets via sendToWire() while they can be accepted. */
    void sendNextPackets() {
        while (true) {
            prepareOutboundBuffer();
            if (mOutboundBufferSize == 0) {
                break;
            }

            if (LogUtils.verbose()) {
                StringBuilder sb = new StringBuilder();
                if ((mOutboundBuffer[0] & HEADER_TYPE_MASK) == HEADER_TYPE_CONTROL) {
                    sb.append(", ctrl=");
                    sb.append(PacketDecoder.getTypeName(mOutboundBuffer[0]));
                } else {
                    int curIdx = 0;
                    if ((mOutboundBuffer[curIdx] & HEADER_TYPE_MASK) == HEADER_TYPE_DATA_ACK) {
                        sb.append(", ack_sn=");
                        sb.append(mOutboundBuffer[curIdx++] & PacketEncoder.HEADER_SEQ_MASK);
                    }
                    if (curIdx < mOutboundBufferSize) {
                        final int headerTypeBits = (mOutboundBuffer[curIdx] & HEADER_TYPE_MASK);
                        if (headerTypeBits == HEADER_TYPE_DATA_REQ_ACK
                                || headerTypeBits == HEADER_TYPE_DATA) {
                            sb.append(", data_sn=");
                            sb.append(mOutboundBuffer[curIdx++] & PacketEncoder.HEADER_SEQ_MASK);
                            sb.append(", data_len=");
                            sb.append(mOutboundBufferDataPayloadSize);
                        }
                        if (headerTypeBits == HEADER_TYPE_DATA_REQ_ACK) {
                            sb.append(", req_ack");
                        }
                    }
                }
                Log.v(TAG, logStr("Sending packet size=" + mOutboundBufferSize + sb));
            }

            if (!mDelegate.sendToWire(mOutboundBuffer, 0, mOutboundBufferSize)) {
                break;
            }

            if (mOutboundBufferHasAck) {
                mInboundUnackedPacketCount = 0;
                mShouldAckNow = false;
                cancelInboundDataAckAlarm();
            }

            if (mOutboundBufferDataPayloadSize > 0) {
                // Record any sent data payload for retransmission.
                int sn = mNextOutboundFreeSn;
                setOutboundDataPayloadSize(sn, mOutboundBufferDataPayloadSize);
                mNextOutboundFreeSn = getNextSn(sn);

                // We are about to send data, prep the retransmit timer if it isn't already set
                if (mRetransmitAlarm == null) {
                    rescheduleRetransmitAlarm();
                }
            }
        }
    }

    private void cancelInboundDataAckAlarm() {
        if (mInboundDataAckAlarm != null) {
            mInboundDataAckAlarm.cancel();
            mInboundDataAckAlarm = null;
        }
    }

    private void scheduleInboundDataAckAlarm() {
        if (mInboundDataAckAlarm != null) {
            return;
        }

        mInboundDataAckAlarm = mEventManager.scheduleAlarm(DATA_PAYLOAD_ACK_TIMEOUT,
            new EventManager.Alarm.Listener() {
                @Override public void onAlarm(EventManager.Alarm alarm, long elapsedTimeMs) {
                    if (mHasStoppedDataStream) {
                        return;
                    }
                    mShouldAckNow = true;
                    sendNextPackets();
                }

                @Override public void onAlarmCancelled(EventManager.Alarm alarm) {}
            });
    }

    private void cancelRetransmitAlarm() {
        if (mRetransmitAlarm != null) {
            mRetransmitAlarm.cancel();
            mRetransmitAlarm = null;
        }
    }

    private void rescheduleRetransmitAlarm() {
        cancelRetransmitAlarm();

        mRetransmitAlarm = mEventManager.scheduleAlarm(
            DATA_PAYLOAD_RETRANSMIT_TIMEOUT,
            new EventManager.Alarm.Listener() {
                @Override public void onAlarm(EventManager.Alarm alarm, long elapsedTimeMs) {
                    if (mHasStoppedDataStream) {
                        return;
                    }
                    handleRetransmitTimeout(elapsedTimeMs);
                }

                @Override public void onAlarmCancelled(EventManager.Alarm alarm) {}
            });
    }

    /** Records information about inbound ACK SN. */
    void handleInboundAck(int sn) {
        if (!isOutboundDataPayloadAwaitingAck(sn)) {
            if (LogUtils.verbose()) {
                Log.v(TAG, logStr("Ignoring retransmitted ack_sn=" + sn));
            }
            return;
        }

        // We know the bytes are acknowledged so compute and clear the sizes.
        int nextSn = getNextSn(sn);
        int numBytesAcked = getTotalNumBytesAwaitingAckUpTo(nextSn);

        if (LogUtils.verbose()) {
            Log.v(TAG, logStr("Received ack_sn=" + sn + " for " + numBytesAcked
                    + " byte(s), Next expected ack_sn=" + nextSn));
        }

        clearOutboundDataPayloadSizesUpTo(nextSn);

        cancelRetransmitAlarm();

        if (isOutboundDataPayloadAwaitingAck(nextSn)) {
            // Still awaiting for an ack - re-arm our retransmit timer.
            rescheduleRetransmitAlarm();
        }

        mNextOutboundExpectedAckSn = nextSn;

        mDataSource.readBytes(null, 0, numBytesAcked);

        mDelegate.onConsumedDataSource();
    }

    /** Records information about inbound data SN. */
    void recordInboundDataPacket(int sn, boolean isRequestingAck) {
        mLastInboundDataSn = sn;
        mInboundUnackedPacketCount++;

        if (isRequestingAck) {
            mShouldAckNow = true;
        }

        if (LogUtils.verbose()) {
            Log.v(TAG, logStr("Unacked " + mInboundUnackedPacketCount
                    + " packets, last sn=" + mLastInboundDataSn));
        }

        scheduleInboundDataAckAlarm();
    }

    private void handleRetransmitTimeout(long elapsedTimeMs) {
        mRetransmitAlarm = null;

        if (mDelegate.handleRetransmitTimeout(elapsedTimeMs)) {
            return;
        }

        // Retransmit all un-acked data.
        int sn = mNextOutboundExpectedAckSn;
        Log.w(TAG, logStr("Data Ack Timeout: Rolling back from ()" + mNextOutboundFreeSn
                + ", " + mNextOutboundExpectedAckSn + ") to " + sn));
        mNextOutboundFreeSn = sn;
        sendNextPackets();
    }

    /** Enqueues RESET control packet. */
    void addReset() {
        mControlPackets.clear();
        addControlPacket(new ControlPacket(HEADER_CONTROL_RESET));
    }

    /** Enqueues handshake start control packet. */
    void addHandshakeStart() {
        addControlPacket(createHandshakePacket(HEADER_CONTROL_HANDSHAKE_START));
    }

    /** Enqueues handshake ack control packet. */
    void addHandshakeAck() {
        addControlPacket(createHandshakePacket(HEADER_CONTROL_HANDSHAKE_ACK));
    }

    /** Enqueues handshake done control packet. */
    void addHandshakeDone() {
        addControlPacket(createHandshakePacket(HEADER_CONTROL_HANDSHAKE_DONE));
    }

    private ControlPacket createHandshakePacket(int type) {
        HandshakeData handshake = new HandshakeData();
        handshake.version = mConfig.protocolVersion;
        handshake.capabilities = mConfig.capabilities.getBitmask();
        handshake.maxRxWindowSize = mConfig.maxRxWindowSize;
        handshake.maxTxWindowSize = mConfig.maxTxWindowSize;
        return createHandshakePacket(type, handshake);
    }

    private static ControlPacket createHandshakePacket(int type, HandshakeData handshake) {
        try {
            ControlPacket packet = new ControlPacket(type);
            handshake.serializeTo(packet.protoOutput);
            return packet;
        } catch (IOException e) {
            throw new IllegalArgumentException("Handshake packet too large: " + e.toString());
        }
    }

    /** Enqueues network config control packet. */
    void addNetworkConfig(NetworkConfig networkConfig) {
        ControlPacket packet = new ControlPacket(HEADER_CONTROL_NETWORK_CONFIG);
        try {
            networkConfig.serializeTo(packet.protoOutput);
        } catch (IOException e) {
            throw new IllegalArgumentException("NetworkConfig packet too large: " + e.toString());
        }
        addControlPacket(packet);
    }

    /** Enqueues link usage stats control packet. */
    void addLinkUsageStats(LinkUsageStats linkUsageStats) {
        ControlPacket packet = new ControlPacket(HEADER_CONTROL_LINK_USAGE_STATS);
        try {
            linkUsageStats.serializeTo(packet.protoOutput);
        } catch (IOException e) {
            throw new IllegalArgumentException("LinkUsageStats packet too large: " + e.toString());
        }
        addControlPacket(packet);
    }

    private void addControlPacket(ControlPacket packet) {
        packet.wireDataLen = packet.protoOutput.getTotalBytesWritten() + 1;
        // protoOutput won't be used anymore, clear reference to GC.
        packet.protoOutput = null;

        mControlPackets.add(packet);

        sendNextPackets();
    }

    private String logStr(String message) {
        return "[BtConnection:" + mNameForLogging + "] " + message;
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.print("PacketEncoder [");

        if (!mControlPackets.isEmpty()) {
            ipw.println();
            ipw.print("ControlPacket");
            ipw.increaseIndent();
            for (ControlPacket packet : mControlPackets) {
                ipw.print(PacketDecoder.getTypeName(packet.wireData[0]));
                ipw.print(", len=" + packet.wireDataLen);
                ipw.println();
            }
            ipw.decreaseIndent();
        }

        ipw.println();
        ipw.printPair("DataSource", mDataSource);

        ipw.println();
        ipw.print("Inbound");
        ipw.increaseIndent();
        ipw.printPair("lastDataSn", mLastInboundDataSn);
        ipw.printPair("unackedPacketCount", mInboundUnackedPacketCount);
        ipw.printPair("shouldAckNow", mShouldAckNow);
        ipw.printPair("dataAckAlarm", mInboundDataAckAlarm);
        ipw.decreaseIndent();

        ipw.println();
        ipw.print("Outbound");
        ipw.increaseIndent();
        ipw.printPair("maxPacketSize", mMaxPacketSize);
        ipw.printPair("nextExpectedAckSn", mNextOutboundExpectedAckSn);
        ipw.printPair("nextFreeSn", mNextOutboundFreeSn);
        ipw.printPair("retransmitAlarm", mRetransmitAlarm);
        ipw.print("payloadSizes=");
        for (int size : mOutboundDataPayloadSizes) {
            ipw.print(size);
            ipw.print(',');
        }
        ipw.print(' ');
        ipw.decreaseIndent();

        ipw.println("]");
    }
}
