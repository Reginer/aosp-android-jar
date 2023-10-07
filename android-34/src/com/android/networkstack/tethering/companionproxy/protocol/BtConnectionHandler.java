package com.android.networkstack.tethering.companionproxy.protocol;

import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.networkstack.tethering.companionproxy.io.EventManager;
import com.android.networkstack.tethering.companionproxy.io.FileHandle;
import com.android.networkstack.tethering.companionproxy.util.ReadableByteBuffer;

import java.io.IOException;

/**
 * Controls BT connection to a peer.
 *
 * @hide
 */
public final class BtConnectionHandler {
    private static final String TAG = LogUtils.TAG;

    /**
     * Receives notifications when new data, output space, or other events are available.
     * @hide
     */
    public interface Listener {
        /** Notifies of a received network config control packet. */
        void onBtNetworkConfig(NetworkConfig networkConfig);

        /** Notifies of a received link usage stats control packet. */
        void onBtLinkUsageStats(LinkUsageStats linkUsageStats);

        /** Notifies of a stalled connection. */
        void onBtSessionStalled(long stallElapsedTimeMs);

        /**
         * Handles the initial part of the stream, which on some systems provides lower-level
         * configuration data.
         *
         * Returns the number of bytes consumed, or zero if the preamble has been fully read.
         */
        int onBtPreambleData(byte[] data, int pos, int len);

        /**
         * Notifies of a received data packet. Note that data packet is simply a chunk of stream,
         * and does not translate to a separate IP packet.
         */
        boolean onBtDataPacket(byte[] buffer, int pos, int len);

        /** Notifies on data being consumed from the user-provided data source. */
        void onConsumedDataSource();

        /** Notifies that the current connection has been closed. */
        void onBtConnectionClosed(String reason);

        /**
         * Notifies the serve side that handshake has started for the current connection.
         *
         * The implementor must respond by calling acceptHandshake() or shutdown().
         */
        void onBtHandshakeStart();

        /** Notifies that handshake has successfully completed for the current connection. */
        void onBtHandshakeDone();
    }

    public static class Params {
        public EventManager eventManager;
        public ReadableByteBuffer dataSource;
        public boolean isClient;
        public String nameForLogging;

        public Params(EventManager eventManager, ReadableByteBuffer dataSource,
                boolean isClient, String nameForLogging) {
            this.eventManager = eventManager;
            this.dataSource = dataSource;
            this.isClient = isClient;
            this.nameForLogging = nameForLogging;
        }
    }

    // Use minimum allowed IPv6 MTU as maximum IP Packet length.
    public static final int MAX_IP_PACKET_LENGTH = 1280;

    static final int MAX_STREAMED_PACKET_SIZE = 512;

    private static final int MAX_BUFFERED_INBOUND_PACKETS = 4;
    private static final int MAX_BUFFERED_OUTBOUND_PACKETS = 4;

    private enum State { NEW, HANDSHAKE, OPEN, CLOSED }

    private static final int HANDSHAKE_ACK_TIMEOUT = 2000;

    public static final int CLOSE_NOTIFICATION_TIMEOUT = 250;

    // Frequency of notifications that the link has stalled.
    static final int STALL_NOTIFICATION_INTERVAL = 12000;

    private final Listener mListener;
    private final EventManager mEventManager;
    private final boolean mIsClient;
    private final String mNameForLogging;
    private final CallbacksHandler mCallbacksHandler = new CallbacksHandler();

    private PacketFile mFile;

    // Connection state variables.
    private volatile State mState = State.NEW;
    private volatile boolean mHasAcceptedHandshake;
    private ProtocolConfig mConfig;
    private final PacketDecoder mDecoder;
    private final PacketEncoder mEncoder;
    private long mHandshakeAckTimeout = HANDSHAKE_ACK_TIMEOUT;
    private EventManager.Alarm mHandshakeTimeoutAlarm;
    private long mCurrentStallElapsedTimeMs;
    private long mLastStallElapsedTimeMs;

    private int mNextExpectedInboundPayloadSn;

    private PacketDecoder.Packet mCurrentPacket;

    public static BtConnectionHandler createFromStream(
            Params params, FileHandle fileHandle, Listener listener) throws IOException {
        BtConnectionHandler result = new BtConnectionHandler(
            params, listener, MAX_STREAMED_PACKET_SIZE);

        result.mFile = new StreamingPacketFile(
            params.eventManager,
            fileHandle,
            result.mCallbacksHandler,
            MAX_STREAMED_PACKET_SIZE,
            MAX_BUFFERED_INBOUND_PACKETS,
            MAX_BUFFERED_OUTBOUND_PACKETS);

        return result;
    }

    private BtConnectionHandler(
            Params params,
            Listener listener,
            int maxPacketSize) {
        mEventManager = params.eventManager;
        mListener = listener;
        mIsClient = params.isClient;
        mNameForLogging = params.nameForLogging;

        mConfig = new ProtocolConfig(
            HandshakeData.PROTOCOL_VERSION_V1,
            new ProtocolConfig.Capabilities(0),
            PacketEncoder.DEFAULT_MAX_RX_WINDOW_SIZE,
            PacketEncoder.DEFAULT_MAX_RX_WINDOW_SIZE);

        mDecoder = new PacketDecoder();

        mEncoder = new PacketEncoder(mEventManager, params.dataSource, mCallbacksHandler,
            maxPacketSize, mConfig, mNameForLogging);
    }

    /** Starts operation of this connection. */
    public void start() {
        mFile.continueReading();
    }

    /** Returns true if the handshake has completed and the connection is operational. */
    public boolean isOpen() {
        return mState == State.OPEN;
    }

    @VisibleForTesting
    void setConfigForTest(ProtocolConfig config) {
        mConfig = config;
    }

    private void onInboundPacket(byte[] data, int pos, int len) {
        mEventManager.assertInThread();
        if (mState == State.CLOSED) {
            if (LogUtils.verbose()) {
                Log.v(TAG, logStr("Ignoring packet: len=" + len + ", hdr="
                        + PacketDecoder.getTypeName(len > 0 ? data[pos] : 0)));
            }
            return;
        }

        PacketDecoder.Packet packet = mDecoder.readOnePacket(data, pos, len);
        if (mDecoder.getStreamErrorMessage() != null) {
            reportErrorAndReset("Stream error",
                "Decode error " + mDecoder.getStreamErrorMessage());
            return;
        }

        if (packet == null) {
            return;  // The packet is ignored by the decoder.
        }

        if (LogUtils.verbose()) {
            Log.v(TAG, logStr(
                "Parsed " + packet + ", buffered=" + mFile.getInboundBufferSize()));
        }

        mCurrentPacket = packet;
        try {
            processPacket(packet, data);
        } catch (Throwable e) {
            reportErrorAndReset("Unexpected error",
                "Exception processing packet: " + e.toString());
        }

        mCurrentPacket = null;

        mEncoder.sendNextPackets();
    }

    private void processPacket(PacketDecoder.Packet packet, byte[] rawPacketData) {
        if (packet.type == PacketDecoder.Packet.Type.CONTROL) {
            handleControlPacket(packet);
            return;
        }

        if (packet.type != PacketDecoder.Packet.Type.DATA) {
            reportErrorAndReset("Bad packet",
                "Unsupported packet type " + packet.type);
            return;
        }

        mLastStallElapsedTimeMs = 0;
        mCurrentStallElapsedTimeMs = 0;

        if (packet.dataHasAck) {
            mEncoder.handleInboundAck(packet.dataAckSn);
        }

        if (packet.dataPayloadLen > 0) {
            handleDataPacket(packet, rawPacketData);
        }
    }

    private void handleControlPacket(PacketDecoder.Packet packet) {
        switch (packet.controlPacketType) {
            case PacketEncoder.HEADER_CONTROL_RESET:
                handleResetPacket();
                break;
            case PacketEncoder.HEADER_CONTROL_HANDSHAKE_START:
                handleHandshakeStart(packet.handshakeData);
                break;
            case PacketEncoder.HEADER_CONTROL_HANDSHAKE_ACK:
                handleHandshakeAck(packet.handshakeData);
                break;
            case PacketEncoder.HEADER_CONTROL_HANDSHAKE_DONE:
                handleHandshakeDone(packet.handshakeData);
                break;
            case PacketEncoder.HEADER_CONTROL_NETWORK_CONFIG:
                handleNetworkConfig(packet.networkConfig);
                break;
            case PacketEncoder.HEADER_CONTROL_LINK_USAGE_STATS:
                handleLinkUsageStats(packet.linkUsageStats);
                break;
            default:
                reportErrorAndReset("Bad packet",
                    "Unexpected control packet: " + packet.controlPacketType);
                break;
        }
    }

    private void onOutboundPacketSpace() {
        mEventManager.assertInThread();
        mEncoder.sendNextPackets();
    }

    private boolean sendToWire(byte[] data, int pos, int len) {
        mEventManager.assertInThread();
        boolean success = mFile.enqueueOutboundPacket(data, pos, len);
        if (!success) {
            reportErrorAndReset("Buffer overflow", "Failed to send raw data over transport");
        }
        return success;
    }

    ///////////////////////////////////////////////////////////////////////////
    // RESET
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Begins asynchronous shutdown of this connection.
     */
    public void shutdown(Runnable shutdownCallback) {
        mEventManager.execute(() -> {
            closeAndSendResetPacket("Shutdown", shutdownCallback);
        });
    }

    private void closeAndSendResetPacket(String reason, Runnable shutdownCallback) {
        mEventManager.assertInThread();

        if (mState == State.CLOSED) {
            if (shutdownCallback != null) {
                shutdownCallback.run();
            }
            return;
        }

        if (mState == State.NEW) {
            Log.i(TAG, logStr("Closing a new connection due to " + reason));
            setClosedState(false, 0, reason, shutdownCallback);
            return;
        }

        Log.i(TAG, logStr("Sending reset packet due to " + reason));

        setClosedState(true, CLOSE_NOTIFICATION_TIMEOUT, reason, shutdownCallback);
    }

    private void setClosedState(boolean sendReset, int callbackTimeoutMs, String reason,
            Runnable shutdownCallback) {
        mState = State.CLOSED;
        mFile.shutdownReading();
        mEncoder.stopDataStream();

        if (sendReset) {
            mEncoder.addReset();
        }

        mEventManager.scheduleAlarm(callbackTimeoutMs, new EventManager.Alarm.Listener() {
            @Override public void onAlarm(EventManager.Alarm alarm, long elapsedTimeMs) {
                if (shutdownCallback != null) {
                    // Unblock possible latches latch first, to avoid deadlocks.
                    shutdownCallback.run();
                }
                mListener.onBtConnectionClosed(reason);
            }

            @Override public void onAlarmCancelled(EventManager.Alarm alarm) {
            }
        });
    }

    private void reportErrorAndReset(String reason, String description) {
        mEventManager.assertInThread();

        if (mState == State.CLOSED) {
            return;
        }

        String packetInfo = (mCurrentPacket != null ? mCurrentPacket.toString() : "NONE");
        Log.w(TAG, logStr(description + ". Packet=" + packetInfo + ". Closing connection"));

        closeAndSendResetPacket(reason, null);
    }

    private void handleResetPacket() {
        Log.i(TAG, logStr("Received reset packet from peer"));
        setClosedState(false, 0, "Reset by peer", null);
    }

    private boolean handleRetransmitTimeout(long elapsedTimeMs) {
        // Handle both Handshake ACK and Retransmit timeouts.

        mEventManager.assertInThread();

        mCurrentStallElapsedTimeMs += elapsedTimeMs;
        if (mCurrentStallElapsedTimeMs < mLastStallElapsedTimeMs) {
            // Deal with possible wraparound, just in case.
            mLastStallElapsedTimeMs = 0;
            mCurrentStallElapsedTimeMs = elapsedTimeMs;
        }

        // mCurrentStallElapsedTimeMs is a total sum of retransmit timeouts.
        // Once that total jumps over the predefined interval - notify the user
        // and remember when it was notified.
        if (mCurrentStallElapsedTimeMs - mLastStallElapsedTimeMs > STALL_NOTIFICATION_INTERVAL) {
            mLastStallElapsedTimeMs = mCurrentStallElapsedTimeMs;
            mListener.onBtSessionStalled(mLastStallElapsedTimeMs);
        }

        if (mState == State.OPEN) {
            return false;  // Normal retransmit timeout, not handled here.
        }

        if (mState == State.HANDSHAKE) {
            reportErrorAndReset("Timeout",
                "Timed out waiting for handshake to complete, resetting");
            return true;
        }

        // Ignore timeouts in NEW and CLOSED states.
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // HANDSHAKE
    ///////////////////////////////////////////////////////////////////////////

    /** Begins handshake process on the client side. */
    public void startHandshake() {
        if (!mIsClient) {
            throw new IllegalArgumentException("Cannot start handshake from server");
        }

        mEventManager.execute(() -> {
            startHandshakeInternal();
        });
    }

    private void startHandshakeInternal() {
        if (!mIsClient || mState != State.NEW) {
            return;
        }

        mState = State.HANDSHAKE;
        scheduleHandshakeTimeout();

        Log.i(TAG, logStr("Starting handshake: " + getHandshakeConfigForLog(null)));

        mEncoder.addHandshakeStart();
    }

    private void handleHandshakeStart(HandshakeData handshakeIn) {
        if (mIsClient || mState != State.NEW) {
            reportErrorAndReset("Unexpected error", "Unexpected handshake start");
            return;
        }

        mState = State.HANDSHAKE;

        if (!updateLinkProperties(handshakeIn)) {
            return;
        }

        scheduleHandshakeTimeout();

        Log.i(TAG, logStr("Received handshake: " + getHandshakeConfigForLog(handshakeIn)));

        mListener.onBtHandshakeStart();
    }

    /** Accepts handshake on the server side. */
    public void acceptHandshake() {
        if (mIsClient) {
            throw new IllegalArgumentException("Cannot accept handshake on client");
        }
        if (mState != State.HANDSHAKE || mHasAcceptedHandshake) {
            return;
        }
        mHasAcceptedHandshake = true;

        mEventManager.execute(() -> {
            Log.i(TAG, logStr("Accepting handshake"));
            mEncoder.addHandshakeAck();
        });
    }

    @VisibleForTesting
    void setHandshakeAckTimeoutForTest(long timeout) {
        mHandshakeAckTimeout = timeout;
    }

    private void cancelHandshakeTimeout() {
        if (mHandshakeTimeoutAlarm != null) {
            mHandshakeTimeoutAlarm.cancel();
            mHandshakeTimeoutAlarm = null;
        }
    }

    private void scheduleHandshakeTimeout() {
        cancelHandshakeTimeout();

        mHandshakeTimeoutAlarm = mEventManager.scheduleAlarm(mHandshakeAckTimeout,
            new EventManager.Alarm.Listener() {
                @Override public void onAlarm(EventManager.Alarm alarm, long elapsedTimeMs) {
                    handleRetransmitTimeout(elapsedTimeMs);
                }

                @Override public void onAlarmCancelled(EventManager.Alarm alarm) {}
            });
    }

    private void handleHandshakeAck(HandshakeData handshakeIn) {
        if (!mIsClient || mState != State.HANDSHAKE) {
            reportErrorAndReset("Unexpected error", "Unexpected handshake ack");
            return;
        }

        if (!updateLinkProperties(handshakeIn)) {
            return;
        }

        mState = State.OPEN;
        cancelHandshakeTimeout();

        Log.i(TAG, logStr("Completing handshake: " + getHandshakeConfigForLog(handshakeIn)));

        mEncoder.addHandshakeDone();

        mListener.onBtHandshakeDone();
    }

    private void handleHandshakeDone(HandshakeData handshakeIn) {
        if (mIsClient || mState != State.HANDSHAKE) {
            reportErrorAndReset("Unexpected error", "Unexpected handshake done");
            return;
        }

        if (!updateLinkProperties(handshakeIn)) {
            return;
        }

        mState = State.OPEN;
        cancelHandshakeTimeout();

        Log.i(TAG, logStr("Connection open: " + getHandshakeConfigForLog(handshakeIn)));

        mListener.onBtHandshakeDone();
    }

    private boolean updateLinkProperties(HandshakeData handshake) {
        if (handshake.version != mConfig.protocolVersion) {
            reportErrorAndReset("Bad protocol",
                "Unsupported peer protocol version " + handshake.version);
            return false;
        }

        mConfig = new ProtocolConfig(
                mConfig.protocolVersion,
                ProtocolConfig.Capabilities.merge(
                    mConfig.capabilities,
                    new ProtocolConfig.Capabilities(handshake.capabilities)),
                Math.min(mConfig.maxRxWindowSize, handshake.maxTxWindowSize),
                Math.min(mConfig.maxTxWindowSize, handshake.maxRxWindowSize));

        mEncoder.updateConfig(mConfig);

        mEncoder.sendNextPackets();
        return true;
    }

    private String getHandshakeConfigForLog(HandshakeData handshakeIn) {
        StringBuilder sb = new StringBuilder();
        sb.append("local=");
        sb.append(mConfig);

        if (handshakeIn != null) {
            ProtocolConfig other = new ProtocolConfig(
                    handshakeIn.version,
                    new ProtocolConfig.Capabilities(handshakeIn.capabilities),
                    handshakeIn.maxRxWindowSize,
                    handshakeIn.maxTxWindowSize);
            sb.append(", remote=");
            sb.append(other);
        }

        return sb.toString();
    }

    ///////////////////////////////////////////////////////////////////////////
    // OTHER CONTROL
    ///////////////////////////////////////////////////////////////////////////

    /** Sends network config control packet from the server side. */
    public void sendNetworkConfig(NetworkConfig networkConfig) {
        if (mIsClient || mState != State.OPEN) {
            return;
        }

        mEventManager.execute(() -> {
            mEncoder.addNetworkConfig(networkConfig);
        });
    }

    private void handleNetworkConfig(NetworkConfig networkConfig) {
        if (!mIsClient || mState != State.OPEN) {
            return;  // Just ignore since these would be resent after next handshake.
        }

        mEventManager.execute(() -> {
            mListener.onBtNetworkConfig(networkConfig);
        });
    }

    private void handleLinkUsageStats(LinkUsageStats linkUsageStats) {
        if (!mIsClient || mState != State.OPEN) {
            return;  // Just ignore since these are harmless.
        }

        mEventManager.execute(() -> {
            mListener.onBtLinkUsageStats(linkUsageStats);
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // DATA PACKETS
    ///////////////////////////////////////////////////////////////////////////

    /** Requests this connection to attempt to send out more packets. */
    public void maybeSendDataPackets() {
        mEventManager.execute(() -> {
            mEncoder.sendNextPackets();
        });
    }

    private void handleDataPacket(PacketDecoder.Packet packet, byte[] rawPacketData) {
        int snForAck = packet.dataSn;
        if (packet.dataSn == mNextExpectedInboundPayloadSn) {
            // That's the SN we expected, grab the underlying data.
            if (!mListener.onBtDataPacket(rawPacketData,
                    packet.dataPayloadPos, packet.dataPayloadLen)) {
                return;  // Not enough space, should be logged by the callee.
            }

            mNextExpectedInboundPayloadSn = PacketEncoder.getNextSn(packet.dataSn);

            if (LogUtils.verbose()) {
                Log.v(TAG, logStr("Received " + packet.dataPayloadLen + " hdr=0x"
                        + Integer.toHexString(packet.dataPayloadLen > 0
                            ? rawPacketData[packet.dataPayloadPos] & 0xFF : 0)
                        + ". sn=" + packet.dataSn + ", Next expected sn="
                        + mNextExpectedInboundPayloadSn));
            }
        } else {
            // Not what we expected, check if it's a resent packet we already received and acked.
            int snDistance = PacketEncoder.calculateSnDistance(
                packet.dataSn, mNextExpectedInboundPayloadSn);
            if (snDistance >= mConfig.maxRxWindowSize) {
                // Not a restransmission, ignore. Should we reset if too far in the future?
                Log.w(TAG, logStr("Received sn=" + packet.dataSn + " != Expected sn="
                        + mNextExpectedInboundPayloadSn));
                return;
            }

            // The ack for this will be sent later (either when the ack timer expires,
            // or with our outgoing data).
            snForAck = mEncoder.getLastInboundDataSn();
            Log.w(TAG, logStr("Received previously received sn=" + packet.dataSn
                    + " != Expected sn=" + mNextExpectedInboundPayloadSn
                    + ", Re-acking with last received sn=" + snForAck));
        }

        mEncoder.recordInboundDataPacket(snForAck, packet.isRequestingAck);

        // Flush data/acks if necessary.
        mEncoder.sendNextPackets();
    }

    private String logStr(String message) {
        return "[BtConnection:" + mNameForLogging + "] " + message;
    }

    public void dump(IndentingPrintWriter ipw) {
        mEventManager.assertInThread();

        ipw.print("BtConnectionHandler [");
        ipw.printPair("state", mState);
        ipw.printPair("client", mIsClient);
        ipw.printPair("name", mNameForLogging);

        ipw.println();
        ipw.printPair("protocolConfig", mConfig);

        ipw.println();
        ipw.increaseIndent();
        mFile.dump(ipw);
        ipw.decreaseIndent();

        if (mDecoder.getStreamErrorMessage() != null) {
            ipw.println();
            ipw.printPair("decoderError", mDecoder.getStreamErrorMessage());
        }

        ipw.println();
        ipw.increaseIndent();
        mEncoder.dump(ipw);
        ipw.decreaseIndent();

        ipw.printPair("nextExpectedInboundPayloadSn", mNextExpectedInboundPayloadSn);

        if (mHandshakeTimeoutAlarm != null) {
            ipw.println();
            ipw.printPair("handshakeTimeoutAlarm", mHandshakeTimeoutAlarm);
        }

        ipw.println();
        ipw.printPair("currentStallElapsedTimeMs", mCurrentStallElapsedTimeMs);
        ipw.printPair("lastStallElapsedTimeMs", mLastStallElapsedTimeMs);

        ipw.println("]");
    }

    private final class CallbacksHandler implements PacketFile.Listener, PacketEncoder.Delegate {
        @Override
        public int onPreambleData(byte[] data, int pos, int len) {
            if (mState == State.CLOSED) {
                return 0;
            }
            return mListener.onBtPreambleData(data, pos, len);
        }

        @Override
        public void onInboundPacket(byte[] data, int pos, int len) {
            BtConnectionHandler.this.onInboundPacket(data, pos, len);
        }

        @Override
        public void onInboundBuffered(int newByteCount, int totalBufferedSize) {
            if (LogUtils.verbose()) {
                Log.v(TAG, logStr("Buffered " + newByteCount
                        + " new bytes, total=" + totalBufferedSize));
            }
        }

        @Override
        public void onOutboundPacketSpace() {
            BtConnectionHandler.this.onOutboundPacketSpace();
        }

        @Override
        public void onPacketFileError(PacketFile.ErrorCode error, String message) {
            reportErrorAndReset("Packet file error",
                "Packet file error " + error + ": " + message);
        }

        @Override
        public boolean sendToWire(byte[] data, int pos, int len) {
            return BtConnectionHandler.this.sendToWire(data, pos, len);
        }

        @Override
        public boolean handleRetransmitTimeout(long elapsedTimeMs) {
            return BtConnectionHandler.this.handleRetransmitTimeout(elapsedTimeMs);
        }

        @Override
        public void onConsumedDataSource() {
            mListener.onConsumedDataSource();
        }
    }
}
