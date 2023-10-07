package com.android.networkstack.tethering.companionproxy.io;

import android.os.ParcelFileDescriptor;
import android.system.StructPollfd;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;
import com.android.networkstack.tethering.companionproxy.util.Assertions;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements EventManager. Starts its own thread and invokes all callbacks
 * on that thread.
 *
 * @hide
 */
public final class EventManagerImpl implements EventManager {
    // Never block poll indefinitely. We should always wakeup() poll when necessary,
    // however having an absolute maximum adds an extra safety net.
    private static final int MAX_POLL_TIMEOUT = 5000;

    /** @hide */
    public interface Listener {
        void onEventManagerFailure();
    }

    private final OsAccess mOsAccess;
    private final Listener mListener;
    private final String mLogTag;
    private final Object mLock = new Object();
    private final AtomicBoolean mIsStopped = new AtomicBoolean(false);
    private final FdAsyncFile[] mWakeupPipe = new FdAsyncFile[2];
    private final ArrayList<BaseAsyncFileImpl> mFiles = new ArrayList<>();
    private final PriorityQueue<AlarmData> mAlarms = new PriorityQueue<>();
    private final byte[] mWakeupData = new byte[1];
    private final ArrayList<BaseAsyncFileImpl> mTempFileList = new ArrayList<>();
    private final short mPollInMask;
    private final short mPollOutMask;
    private StructPollfd[] mPollSet = new StructPollfd[0];
    private volatile Thread mThread;
    private volatile boolean mHasAlarmCancellations;
    private boolean mHasLocalWakeupCalls;

    public EventManagerImpl(OsAccess osAccess, Listener listener, String logTag) {
        mOsAccess = osAccess;
        mListener = listener;
        mLogTag = logTag;
        mPollInMask = mOsAccess.getPollInMask();
        mPollOutMask = mOsAccess.getPollOutMask();
    }

    /** Starts EventManager internal thread. */
    public void start() throws IOException {
        if (mThread != null || mIsStopped.get()) {
            throw new IllegalStateException("Cannot start EventManager");
        }

        final ParcelFileDescriptor[] wakeupPipe = mOsAccess.pipe();
        try {
            AsyncFile.Listener wakeupListener = new WakeupEventListener();
            mWakeupPipe[0] = new FdAsyncFile(wakeupPipe[0], wakeupListener);
            mWakeupPipe[1] = new FdAsyncFile(wakeupPipe[1], wakeupListener);
        } catch (IOException e) {
            if (wakeupPipe != null) {
                mOsAccess.close(wakeupPipe[0]);
                mOsAccess.close(wakeupPipe[1]);
            }
            throw e;
        }

        mFiles.add(mWakeupPipe[0]);
        mFiles.add(mWakeupPipe[1]);
        mWakeupPipe[0].enableReadEvents(true);

        mThread =
            new Thread(
                () -> {
                    runLoop();
                },
                "CompanionProxyEventManager");

        mThread.start();
    }

    /** Stops EventManager internal thread, and closes all managed file descriptors. */
    public void shutdown() {
        synchronized (mLock) {
            mIsStopped.set(true);
            if (mThread == null || mThread == Thread.currentThread()) {
                return;
            }
        }

        wakeup();

        try {
            Thread thread = mThread;
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Log.w(mLogTag, logStr("Interrupted while joining EventManager thread"));
        }
    }

    private void cleanupOnShutdown() {
        ArrayList<AlarmData> alarms;
        ArrayList<BaseAsyncFileImpl> files;

        synchronized (mLock) {
            mIsStopped.set(true);

            mThread = null;

            mWakeupPipe[0] = null;
            mWakeupPipe[1] = null;

            files = new ArrayList<>(mFiles);
            mFiles.clear();

            alarms = new ArrayList<>(mAlarms);
            mAlarms.clear();
        }

        for (BaseAsyncFileImpl file : files) {
            file.doClose();
        }

        for (AlarmData alarm : alarms) {
            try {
                alarm.callback.onAlarmCancelled(alarm);
            } catch (Throwable e) {
                Log.w(mLogTag, logStr("Unexpected error while notifying close event"), e);
            }
        }
    }

    @Override
    public void assertInThread() {
        Thread thread = mThread;
        if (!Assertions.IS_USER_BUILD && thread != null && thread != Thread.currentThread()) {
            throw new IllegalArgumentException("Not in EventManager thread: "
                + Thread.currentThread().getName());
        }
    }

    @Override
    public AsyncFile registerFile(FileHandle fileHandle, AsyncFile.Listener listener)
            throws IOException {
        BaseAsyncFileImpl file;
        synchronized (mLock) {
            if (mThread == null || mIsStopped.get()) {
                throw new IOException("EventManager has not started or has been shut down");
            }

            file = createAsyncFileImpl(fileHandle, listener);

            mFiles.add(file);
        }

        wakeup();
        return file;
    }

    private BaseAsyncFileImpl createAsyncFileImpl(FileHandle fileHandle,
            AsyncFile.Listener listener) throws IOException {
        ParcelFileDescriptor fd = fileHandle.getFileDescriptor();
        if (fd != null) {
            try {
                return new FdAsyncFile(fd, listener);
            } catch (IOException e) {
                mOsAccess.close(fd);
                throw e;
            }
        }

        Closeable closeable = fileHandle.getCloseable();
        if (closeable != null) {
            closeFile(closeable);
            throw new IOException("Blocking sockets not supported");
        }

        throw new IOException("Invalid FileHandle");
    }

    private static void closeFile(Closeable file) {
        try {
            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public Alarm scheduleAlarm(long timeout, Alarm.Listener callback) {
        AlarmData alarm = new AlarmData(callback, mOsAccess.monotonicTimeMillis(), timeout);

        synchronized (mLock) {
            if (mThread == null || mIsStopped.get()) {
                return null;
            }

            mAlarms.add(alarm);
        }

        wakeup();
        return alarm;
    }

    @Override
    public void execute(Runnable callback) {
        scheduleAlarm(0, new Alarm.Listener() {
            @Override
            public void onAlarm(Alarm alarm, long elapsedTimeMs) {
                callback.run();
            }

            @Override
            public void onAlarmCancelled(Alarm alarm) {}
        });
    }

    private void wakeup() {
        if (Thread.currentThread() == mThread) {
            mHasLocalWakeupCalls = true;
            return;
        }

        synchronized (mLock) {
            if (mThread != null && mWakeupPipe[1] != null) {
                try {
                    mWakeupPipe[1].writeInternal(mWakeupData, 0, mWakeupData.length);
                } catch (IOException e) {
                    Log.w(mLogTag, logStr("Failed to write to wakeup pipe: " + e.toString()));
                }
            }
        }
    }

    private void runLoop() {
        if (Log.isLoggable(mLogTag, Log.VERBOSE)) {
            Log.v(mLogTag, logStr("Starting EventManager"));
        }

        try {
            while (!mIsStopped.get()) {
                processExpiredAlarms();
                if (mIsStopped.get()) {
                    break;
                }

                processReceivedEvents();
                if (mIsStopped.get()) {
                    break;
                }

                updatePollFds();

                long pollTimeout = MAX_POLL_TIMEOUT;

                synchronized (mLock) {
                    if (mHasLocalWakeupCalls) {
                        pollTimeout = 0;
                        mHasLocalWakeupCalls = false;
                    } else if (!mAlarms.isEmpty()) {
                        long now = mOsAccess.monotonicTimeMillis();
                        long nextAlarmDeadline = mAlarms.peek().expirationTimeMs;
                        if (nextAlarmDeadline < now) {
                            nextAlarmDeadline = now;
                        }
                        pollTimeout = Math.min(pollTimeout, nextAlarmDeadline - now);
                    }
                }

                mOsAccess.poll(mPollSet, (int) pollTimeout);
            }
        } catch (Throwable e) {
            Log.w(mLogTag, logStr("Unexpected error while trying to poll"), e);
        } finally {
            if (Log.isLoggable(mLogTag, Log.VERBOSE)) {
                Log.v(mLogTag, logStr("Exiting EventManager"));
            }
            if (!mIsStopped.get()) {
                Log.e(mLogTag, logStr("Exiting EventManager with an open connection"));
                mListener.onEventManagerFailure();
            }
        }

        cleanupOnShutdown();
    }

    private void processExpiredAlarms() {
        ArrayList<AlarmData> alarms = null;
        synchronized (mLock) {
            if (mHasAlarmCancellations) {
                mHasAlarmCancellations = false;

                Iterator<AlarmData> it = mAlarms.iterator();
                while (it.hasNext()) {
                    AlarmData alarm = it.next();
                    if (alarm.isCancelled) {
                        if (alarms == null) {
                            alarms = new ArrayList<>();
                        }
                        alarms.add(alarm);
                        it.remove();
                    }
                }
            }

            long now = mOsAccess.monotonicTimeMillis();
            while (!mAlarms.isEmpty() && !mIsStopped.get()) {
                if (mAlarms.peek().expirationTimeMs > now) {
                    break;
                }

                if (alarms == null) {
                    alarms = new ArrayList<>();
                }

                alarms.add(mAlarms.poll());
            }
        }

        if (alarms == null) {
            return;
        }

        for (AlarmData alarm : alarms) {
            if (alarm.isCancelled) {
                try {
                    alarm.callback.onAlarmCancelled(alarm);
                } catch (Throwable e) {
                    Log.w(mLogTag, logStr("Unexpected error while cancelling alarm"), e);
                }
                continue;
            }

            try {
                long elapsedTimeMs = mOsAccess.monotonicTimeMillis() - alarm.creationTimeMs;
                alarm.callback.onAlarm(alarm, elapsedTimeMs);
            } catch (Throwable e) {
                Log.w(mLogTag, logStr("Unexpected error while executing alarm"), e);
            }
        }
    }

    private void removeClosedFiles() {
        int idx = 0;
        while (idx < mFiles.size()) {
            BaseAsyncFileImpl file = mFiles.get(idx);
            if (file.wantsClose()) {
                mFiles.remove(idx);
                file.doClose();
            } else {
                idx++;
            }
        }
    }

    private void processReceivedEvents() {
        synchronized (mLock) {
            removeClosedFiles();

            mTempFileList.clear();
            for (int i = 0; i < mFiles.size(); i++) {
                mTempFileList.add(mFiles.get(i));
            }
        }

        for (int i = 0; i < mTempFileList.size(); i++) {
            if (!mIsStopped.get()) {
                mTempFileList.get(i).notifyEvents();
            }
        }
    }

    private void updatePollFds() {
        synchronized (mLock) {
            // Perform all requested close() operations.
            removeClosedFiles();

            // Perform all requested event registration operations.
            int pollSetSize = 0;
            for (int i = 0; i < mFiles.size(); i++) {
                BaseAsyncFileImpl file = mFiles.get(i);
                file.applyEventRequests();
                if (file.getStructPollfd() != null) {
                    pollSetSize++;
                }
            }

            // Re-compose the mPollSet.
            if (pollSetSize != mPollSet.length) {
                mPollSet = new StructPollfd[pollSetSize];
            }

            int idx = 0;
            for (int i = 0; i < mFiles.size(); i++) {
                BaseAsyncFileImpl file = mFiles.get(i);
                StructPollfd structPollFd = file.getStructPollfd();
                if (structPollFd != null) {
                    mPollSet[idx++] = structPollFd;
                }
            }
        }
    }

    private String logStr(String message) {
        return "[EventManager] " + message;
    }

    public void dump(IndentingPrintWriter ipw) {
        assertInThread();

        synchronized (mLock) {
            ipw.print("EventManager [");
            ipw.printPair("stopped", mIsStopped);
            ipw.printPair("thread", mThread != null ? mThread.isAlive() : false);

            ipw.println();
            ipw.print("Files");
            ipw.increaseIndent();
            for (BaseAsyncFileImpl file : mFiles) {
                ipw.println(file);
            }
            ipw.decreaseIndent();

            ipw.println();
            ipw.print("Alarms");
            ipw.increaseIndent();
            for (AlarmData alarm : mAlarms) {
                ipw.println(alarm);
            }
            ipw.decreaseIndent();

            ipw.println("]");
         }
    }

    private class FdAsyncFile extends BaseAsyncFileImpl {
        private final ParcelFileDescriptor mParcelFd;
        private final FileDescriptor mRawFd;
        private final StructPollfd mStructPollFd = new StructPollfd();

        FdAsyncFile(ParcelFileDescriptor fd, AsyncFile.Listener listener) throws IOException {
            super(listener, mLogTag);

            if (fd == null) {
                throw new NullPointerException();
            }

            mParcelFd = fd;
            mRawFd = mOsAccess.getInnerFileDescriptor(mParcelFd);
            mStructPollFd.fd = mRawFd;
            mStructPollFd.userData = this;

            mOsAccess.setNonBlocking(mRawFd);
        }

        @Override
        protected void closeInternal() {
            mStructPollFd.events = 0;
            mStructPollFd.revents = 0;
            mOsAccess.close(mParcelFd);
        }

        @Override
        protected void applyEventRequests() {
            // Runs in the EventManager thread.
            mStructPollFd.revents = 0;
            mStructPollFd.events = (short) (
                (wantsReadEvents() ? mPollInMask : 0) |
                (wantsWriteEvents() ? mPollOutMask : 0));
        }

        @Override
        protected StructPollfd getStructPollfd() {
            return (mStructPollFd.events != 0 ? mStructPollFd : null);
        }

        @Override
        protected void notifyEvents() {
            // Runs in the EventManager thread.
            final boolean hasReadEvent =
                (mStructPollFd.revents & mPollInMask) == mPollInMask;
            final boolean hasWriteEvent =
                (mStructPollFd.revents & mPollOutMask) == mPollOutMask;
            mStructPollFd.revents = 0;

            callEventListener(hasReadEvent, hasWriteEvent);
        }

        @Override
        protected int readInternal(byte[] buffer, int pos, int len) throws IOException {
            return mOsAccess.read(mRawFd, buffer, pos, len);
        }

        @Override
        protected int writeInternal(byte[] buffer, int pos, int len) throws IOException {
            return mOsAccess.write(mRawFd, buffer, pos, len);
        }

        @Override
        protected void wakeup() {
            EventManagerImpl.this.wakeup();
        }

        @Override
        protected void assertInThread() {
            EventManagerImpl.this.assertInThread();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AsyncFile{fd=");
            sb.append(mOsAccess.getFileDebugName(mParcelFd));
            toString(sb);
            sb.append(",events=");
            sb.append(mStructPollFd.events);
            sb.append(",revents=");
            sb.append(mStructPollFd.revents);
            sb.append('}');
            return sb.toString();
        }
    }

    private static class WakeupEventListener implements AsyncFile.Listener {
        private final byte[] mDiscardBuffer = new byte[12];

        @Override
        public void onClosed(AsyncFile file) {}

        @Override
        public void onReadReady(AsyncFile file) {
            try {
                while (file.read(mDiscardBuffer, 0, mDiscardBuffer.length) > 0) {
                    // keep reading
                }
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public void onWriteReady(AsyncFile file) {}
    };

    private class AlarmData implements Alarm, Comparable<AlarmData> {
        final Alarm.Listener callback;
        final long creationTimeMs;
        final long expirationTimeMs;
        volatile boolean isCancelled;

        AlarmData(Alarm.Listener callback, long creationTimeMs, long durationMs) {
            this.callback = callback;
            this.creationTimeMs = creationTimeMs;
            this.expirationTimeMs = creationTimeMs + durationMs;
        }

        @Override
        public void cancel() {
            isCancelled = true;
            mHasAlarmCancellations = true;
            wakeup();
        }

        @Override
        public int compareTo(AlarmData other) {
            long distance = expirationTimeMs - other.expirationTimeMs;
            return (distance > 0 ? 1 : (distance < 0) ? -1 : 0);
        }

        @Override
        public String toString() {
            assertInThread();
            StringBuilder sb = new StringBuilder();
            sb.append("Alarm{cancelled=");
            sb.append(isCancelled);
            sb.append(",duration=");
            sb.append(expirationTimeMs - creationTimeMs);
            sb.append(",remaining=");
            sb.append(expirationTimeMs - mOsAccess.monotonicTimeMillis());
            sb.append('}');
            return sb.toString();
        }
    }
}
