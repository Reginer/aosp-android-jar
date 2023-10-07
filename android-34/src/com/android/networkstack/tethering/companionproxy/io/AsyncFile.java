package com.android.networkstack.tethering.companionproxy.io;

import java.io.IOException;

/**
 * Represents an EventManager-managed file with Async IO semantics.
 *
 * Implements level-based Asyn IO semantics. This means that:
 *   - onReadReady() callback would keep happening as long as there's any remaining
 *     data to read, or the user calls enableReadEvents(false)
 *   - onWriteReady() callback would keep happening as long as there's remaining space
 *     to write to, or the user calls enableWriteEvents(false)
 *
 * All operations except close() must be called on the EventManager thread.
 *
 * @hide
 */
public interface AsyncFile {
    /**
     * Receives notifications when file readability or writeability changes.
     * @hide
     */
    public interface Listener {
        /** Invoked after the underlying file has been closed. */
        void onClosed(AsyncFile file);

        /** Invoked while the file has readable data and read notifications are enabled. */
        void onReadReady(AsyncFile file);

        /** Invoked while the file has writeable space and write notifications are enabled. */
        void onWriteReady(AsyncFile file);
    }

    /** Requests this file to be closed. */
    void close();

    /** Enables or disables onReadReady() events. */
    void enableReadEvents(boolean enable);

    /** Enables or disables onWriteReady() events. */
    void enableWriteEvents(boolean enable);

    /** Returns true if the input stream has reached its end, or has been closed. */
    boolean reachedEndOfFile();

    /**
     * Reads available data from the given non-blocking file descriptor.
     *
     * Returns zero if there's no data to read at this moment.
     * Returns -1 if the file has reached its end or the input stream has been closed.
     * Otherwise returns the number of bytes read.
     */
    int read(byte[] buffer, int pos, int len) throws IOException;

    /**
     * Writes data into the given non-blocking file descriptor.
     *
     * Returns zero if there's no buffer space to write to at this moment.
     * Otherwise returns the number of bytes written.
     */
    int write(byte[] buffer, int pos, int len) throws IOException;
}
