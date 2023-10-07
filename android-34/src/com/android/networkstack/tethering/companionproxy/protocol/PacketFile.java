package com.android.networkstack.tethering.companionproxy.protocol;

import com.android.internal.util.IndentingPrintWriter;

/**
 * Defines bidirectional file where all transmissions are made as complete packets.
 *
 * Automatically manages all readability and writeability events in EventManager:
 *   - When read buffer has more space - asks EventManager to notify on more data
 *   - When write buffer has more space - asks the user to provide more data
 *   - When underlying file cannot accept more data - registers EventManager callback
 *
 * @hide
 */
interface PacketFile {
    /** @hide */
    enum ErrorCode {
        UNEXPECTED_ERROR,
        IO_ERROR,
        INBOUND_PACKET_TOO_LARGE,
        OUTBOUND_PACKET_TOO_LARGE,
    }

    /**
     * Receives notifications when new data or output space is available.
     *
     * @hide
     */
    interface Listener {
        /**
         * Handles the initial part of the stream, which on some systems provides lower-level
         * configuration data.
         *
         * Returns the number of bytes consumed, or zero if the preamble has been fully read.
         */
        int onPreambleData(byte[] data, int pos, int len);

        /** Handles one extracted packet. */
        void onInboundPacket(byte[] data, int pos, int len);

        /** Notifies on new data being added to the buffer. */
        void onInboundBuffered(int newByteCount, int totalBufferedSize);

        /** Notifies on data being flushed from output buffer. */
        void onOutboundPacketSpace();

        /** Notifies on unrecoverable error in the packet processing. */
        void onPacketFileError(ErrorCode error, String message);
    }

    /** Requests this file to be closed. */
    void close();

    /** Permanently disables reading of this file, and clears all buffered data. */
    void shutdownReading();

    /** Starts or resumes async read operations on this file. */
    void continueReading();

    /** Returns the number of bytes currently buffered as input. */
    int getInboundBufferSize();

    /** Returns the number of bytes currently available for buffering for output. */
    int getOutboundFreeSize();

    /**
     * Queues the given data for output.
     * Throws runtime exception if there is not enough space.
     */
    boolean enqueueOutboundPacket(byte[] data, int pos, int len);

    void dump(IndentingPrintWriter ipw);
}
