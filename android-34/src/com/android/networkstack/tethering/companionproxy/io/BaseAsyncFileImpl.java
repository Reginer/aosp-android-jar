package com.android.networkstack.tethering.companionproxy.io;

import android.system.ErrnoException;
import android.system.StructPollfd;
import android.util.Log;

import java.io.IOException;

/**
 * Base class for all AsyncFile implementations.
 *
 * @hide
 */
abstract class BaseAsyncFileImpl implements AsyncFile {
    private final Listener mListener;
    private final String mLogTag;
    private volatile boolean mWantsClose;
    private volatile boolean mWantsReadEvents;
    private volatile boolean mWantsWriteEvents;
    private boolean mHasNotifiedClosed;
    private volatile boolean mEndOfFile;

    BaseAsyncFileImpl(Listener listener, String logTag) {
        mListener = listener;
        mLogTag = logTag;
    }

    @Override
    public final void close() {
        mWantsClose = true;
        wakeup();
    }

    final boolean wantsClose() {
        return mWantsClose;
    }

    final void doClose() {
        mWantsClose = true;

        closeInternal();

        if (!mHasNotifiedClosed) {
            mHasNotifiedClosed = true;
            try {
                mListener.onClosed(this);
            } catch (Throwable e) {
                Log.w(mLogTag, logStr("Unexpected error while notifying close event"), e);
            }
        }
    }

    @Override
    public final void enableReadEvents(boolean enable) {
        if (enable != mWantsReadEvents) {
            mWantsReadEvents = enable;
            wakeup();
        }
    }

    @Override
    public final void enableWriteEvents(boolean enable) {
        if (enable != mWantsWriteEvents) {
            mWantsWriteEvents = enable;
            wakeup();
        }
    }

    protected final boolean wantsReadEvents() {
        return mWantsReadEvents;
    }

    protected final boolean wantsWriteEvents() {
        return mWantsWriteEvents;
    }

    protected final void callEventListener(boolean hasReadEvent, boolean hasWriteEvent) {
        // Handle writes before reads, because read events may have side effects which
        // affect the state of the sockets being polled, including deleting them entirely.
        // Write events have no side effects, so they should be handled first.
        if (hasWriteEvent && mWantsWriteEvents && !mWantsClose) {
            try {
                mListener.onWriteReady(this);
            } catch (Throwable e) {
                Log.w(mLogTag, logStr("Unexpected error notifying write event, closing file"), e);
                close();
            }
        }

        if (hasReadEvent && mWantsReadEvents && !mWantsClose) {
            try {
                mListener.onReadReady(this);
            } catch (Throwable e) {
                Log.w(mLogTag, logStr("Unexpected error notifying read event, closing file"), e);
                close();
            }
        }
    }

    @Override
    public final boolean reachedEndOfFile() {
        return mEndOfFile;
    }

    @Override
    public final int read(byte[] buffer, int pos, int len) throws IOException {
        assertInThread();
        int result = readInternal(buffer, pos, len);
        if (result < 0) {
            mEndOfFile = true;
        }
        return result;
    }

    @Override
    public final int write(byte[] buffer, int pos, int len) throws IOException {
        assertInThread();
        return writeInternal(buffer, pos, len);
    }

    protected abstract void closeInternal();
    protected abstract void applyEventRequests();
    protected abstract StructPollfd getStructPollfd();
    protected abstract void notifyEvents();
    protected abstract int readInternal(byte[] buffer, int pos, int len) throws IOException;
    protected abstract int writeInternal(byte[] buffer, int pos, int len) throws IOException;

    protected abstract void wakeup();
    protected abstract void assertInThread();

    protected final String logStr(String message) {
        return "[EventManager] " + message;
    }

    protected final void toString(StringBuilder sb) {
        assertInThread();
        sb.append(",eof=");
        sb.append(mEndOfFile);
        sb.append(",reqClose=");
        sb.append(mWantsClose);
        sb.append(",notifiedClosed=");
        sb.append(mHasNotifiedClosed);
        sb.append(",reqRead=");
        sb.append(mWantsReadEvents);
        sb.append(",reqWrite=");
        sb.append(mWantsWriteEvents);
    }
}
