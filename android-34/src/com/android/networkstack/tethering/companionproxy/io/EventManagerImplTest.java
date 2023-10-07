package com.android.networkstack.tethering.companionproxy.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.networkstack.tethering.companionproxy.io.AsyncFile;
import com.android.networkstack.tethering.companionproxy.io.EventManager;
import com.android.networkstack.tethering.companionproxy.io.EventManagerImpl;
import com.android.networkstack.tethering.companionproxy.io.FakeOsAccess;
import com.android.networkstack.tethering.companionproxy.io.FileHandle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class EventManagerImplTest implements EventManagerImpl.Listener {
    private static final String TAG = EventManagerImplTest.class.getSimpleName();

    private FakeOsAccess mOsAccess;
    private EventManagerImpl mEventManager;
    private ArrayList<String> mErrors = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        mOsAccess = new FakeOsAccess("EventManager");
        mEventManager = new EventManagerImpl(mOsAccess, this, "EventManager");
        mEventManager.start();
    }

    @After
    public void tearDown() throws Exception {
        if (!mErrors.isEmpty()) {
            fail("Errors: " + mErrors.stream().collect(Collectors.joining("; ")));
        }

        if (mEventManager != null) {
            mEventManager.shutdown();
        }
    }

    @Test
    public void scheduleAlarm_afterShutdown() throws Exception {
        mEventManager.shutdown();

        AlarmState state = new AlarmState(null);
        assertNull(mEventManager.scheduleAlarm(0, state));
    }

    @Test
    public void scheduleAlarm_sequence() throws Exception {
        AlarmState alarm1 = scheduleAlarm(10);
        AlarmState alarm2 = scheduleAlarm(50);
        AlarmState alarm3 = scheduleAlarm(100);

        assertTrue(alarm3.waitToComplete(1000));
        assertTrue(alarm1.notificationTime != 0);
        assertTrue(alarm2.notificationTime != 0);
        assertTrue(alarm3.notificationTime != 0);
        assertTrue(alarm1.notificationTime <= alarm2.notificationTime);
        assertTrue(alarm2.notificationTime <= alarm3.notificationTime);
    }

    @Test
    public void scheduleAlarm_sequenceMultiThreaded() throws Exception {
        final ArrayList<AlarmState> alarms1 = new ArrayList<>();
        final ArrayList<AlarmState> alarms2 = new ArrayList<>();
        final ArrayList<AlarmState> alarms3 = new ArrayList<>();
        final Object lockObject = new Object();

        for (int i = 0; i < 10; i++) {
            alarms1.add(scheduleAlarm(10, null, lockObject, mEventManager));
            alarms2.add(scheduleAlarm(50, null, lockObject, mEventManager));
            alarms3.add(scheduleAlarm(100, null, lockObject, mEventManager));
        }

        synchronized (lockObject) {
            assertTrue(waitForCondition(lockObject, 1000, () -> {
                boolean allAlarmsCompleted = true;
                for (AlarmState alarm : alarms3) {
                    if (alarm.notificationTime == 0 && !alarm.wasCancelled) {
                        allAlarmsCompleted = false;
                        break;
                    }
                }
                return allAlarmsCompleted;
            }));

            for (int i = 0; i < alarms3.size(); i++) {
                assertTrue(alarms1.get(i).notificationTime != 0);
                assertTrue(alarms2.get(i).notificationTime != 0);
                assertTrue(alarms3.get(i).notificationTime != 0);
                assertTrue(alarms1.get(i).notificationTime <= alarms2.get(i).notificationTime);
                assertTrue(alarms2.get(i).notificationTime <= alarms3.get(i).notificationTime);
            }
        }
    }

    @Test
    public void scheduleAlarm_cancellation() throws Exception {
        AlarmState alarm1 = scheduleAlarm(5000);
        AlarmState alarm2 = scheduleAlarm(100);

        alarm1.alarm.cancel();

        // Alarm should get cancellation callback immediately.
        // Here in test scheduled for 5 seconds, must complete within 1
        assertTrue(alarm1.waitToComplete(1000));
        assertTrue(alarm1.wasCancelled);

        // But the other alarm should still run.
        assertTrue(alarm2.waitToComplete(1000));
        assertTrue(alarm2.notificationTime != 0);
    }

    @Test
    public void registerFile_afterShutdown() throws Exception {
        mEventManager.shutdown();

        try {
            registerFile();
            fail("registerFile() after shutdown should generate an exception");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void registerFile_closeOnShutdown() throws Exception {
        FileState state = registerFile();

        mEventManager.shutdown();

        state.assertManagedFdClosed();
    }

    @Test
    public void registerFile_openAndClose() throws Exception {
        FileState state = registerFile();
        assertTrue(mOsAccess.getFileDescriptorNumber(state.managedFd) > 0);

        state.file.close();

        state.assertManagedFdClosed();
        assertTrue(mOsAccess.getFileDescriptorNumber(state.peerFd) > 0);
    }

    @Test
    public void registerFile_asyncRead() throws Exception {
        final FileState state = registerFile();

        final byte[] sentData = createTestData(0, 1, 1000);
        assertEquals(sentData.length - 2,
            mOsAccess.write(state.peerFd, sentData, 1, sentData.length - 2));

        assertFalse(state.waitForReadEvent(100));

        state.file.enableReadEvents(true);
        assertTrue(state.waitForReadEvent(1000));

        state.hasReadEvent = false;
        executeEventManagerLoop();
        assertTrue(state.waitForReadEvent(1000));

        state.file.enableReadEvents(false);
        executeEventManagerLoop();
        state.hasReadEvent = false;
        executeEventManagerLoop();
        assertFalse(state.waitForReadEvent(100));

        final byte[] receivedData = new byte[sentData.length + 100];
        executeEventManagerLoop(() -> {
            assertEquals(sentData.length - 2,
                state.file.read(receivedData, 1, receivedData.length - 1));
        });

        checkTestBytes(receivedData, 1, sentData.length - 2, 0, 0);

        state.file.close();
        state.assertManagedFdClosed();
    }

    @Test
    public void registerFile_endOfFile() throws Exception {
        final FileState state = registerFile();

        final byte[] sentData = createTestData(0, 1, 1000);
        assertEquals(sentData.length - 2,
            mOsAccess.write(state.peerFd, sentData, 1, sentData.length - 2));
        mOsAccess.close(state.peerFd);

        assertFalse(state.file.reachedEndOfFile());

        final byte[] receivedData = new byte[sentData.length + 100];
        executeEventManagerLoop(() -> {
            assertEquals(sentData.length - 2,
                state.file.read(receivedData, 1, receivedData.length - 1));
        });

        checkTestBytes(receivedData, 1, sentData.length - 2, 0, 0);
        assertFalse(state.file.reachedEndOfFile());

        executeEventManagerLoop(() -> {
            assertEquals(-1,
                state.file.read(receivedData, 1, receivedData.length - 1));
        });

        assertTrue(state.file.reachedEndOfFile());

        state.file.close();
        state.assertManagedFdClosed();
    }

    @Test
    public void registerFile_asyncWrite() throws Exception {
        final FileState state = registerFile();

        assertFalse(state.waitForWriteEvent(100));

        final AtomicInteger totalWrittenCount = new AtomicInteger();
        state.fillWriteBuffers(0, totalWrittenCount);

        state.file.enableWriteEvents(true);
        assertFalse(state.waitForWriteEvent(100));

        final byte[] receivedData = new byte[2000];
        assertEquals(receivedData.length - 1,
            mOsAccess.read(state.peerFd, receivedData, 1, receivedData.length - 1));
        checkTestBytes(receivedData, 1, receivedData.length - 1, 0, 0);

        executeEventManagerLoop();
        assertTrue(state.waitForWriteEvent(1000));

        state.file.enableWriteEvents(false);
        executeEventManagerLoop();
        state.hasWriteEvent = false;
        executeEventManagerLoop();
        assertFalse(state.waitForWriteEvent(100));

        state.file.close();
        state.assertManagedFdClosed();
    }

    @Test
    public void socketpair() throws Exception {
        doTestPipe(mOsAccess.socketpair());
    }

    @Test
    public void pipe() throws Exception {
        doTestPipe(mOsAccess.pipe());
    }

    private void doTestPipe(ParcelFileDescriptor[] fds) throws Exception {
        mOsAccess.setNonBlocking(mOsAccess.getInnerFileDescriptor(fds[0]));
        mOsAccess.setNonBlocking(mOsAccess.getInnerFileDescriptor(fds[1]));
        mOsAccess.write(mOsAccess.getInnerFileDescriptor(fds[1]), new byte[10], 0, 10);
        mOsAccess.read(mOsAccess.getInnerFileDescriptor(fds[0]), new byte[10], 0, 10);
        mOsAccess.close(fds[0]);
        mOsAccess.close(fds[1]);
    }

    @Override
    public void onEventManagerFailure() {
        recordError("onEventManagerFailure");
    }

    private AlarmState scheduleAlarm(long timeout) {
        return scheduleAlarm(timeout, null);
    }

    private AlarmState scheduleAlarm(long timeout, Runnable callback) {
        return scheduleAlarm(timeout, callback, null, mEventManager);
    }

    private AlarmState scheduleAlarm(long timeout, Runnable callback,
            Object lockObject, EventManager eventManager) {
        AlarmState state = new AlarmState(callback, lockObject);
        state.alarm = eventManager.scheduleAlarm(timeout, state);
        assertNotNull(state.alarm);
        return state;
    }

    private FileState registerFile() throws Exception {
        ParcelFileDescriptor[] fds = mOsAccess.socketpair();

        FileState state = new FileState(fds[0], fds[1]);
        state.file = mEventManager.registerFile(
            FileHandle.fromFileDescriptor(state.managedParcelFd), state);
        assertNotNull(state.file);

        mOsAccess.setNonBlocking(state.peerFd);

        return state;
    }

    private interface Callback {
        void run() throws Exception;
    }

    private void executeEventManagerLoop() {
        executeEventManagerLoop(null);
    }

    private void executeEventManagerLoop(final Callback callback) {
        final AtomicReference<Exception> exception = new AtomicReference<>();
        AlarmState state = scheduleAlarm(0, () -> {
            try {
                if (callback != null) {
                    callback.run();
                }
            } catch (Exception e) {
                exception.set(e);
            }
        });

        assertTrue(state.waitToComplete(1000));

        if (exception.get() != null) {
            throw new RuntimeException(exception.get());
        }
    }

    private static byte[] createTestData(int firstValue, int padLen, int dataLen) {
        final byte[] data = new byte[padLen + dataLen];
        for (int i = padLen; i < data.length; i++) {
            data[i] = (byte) (firstValue & 0xFF);
            firstValue++;
        }
        return data;
    }

    private static int checkTestBytes(
            byte[] buffer, int pos, int len, int startValue, int totalRead) {
        for (int i = 0; i < len; i++) {
            byte expectedValue = (byte) (startValue & 0xFF);
            if (expectedValue != buffer[pos + i]) {
                fail("Unexpected byte=" + (((int) buffer[pos + i]) & 0xFF)
                        + ", expected=" + (((int) expectedValue) & 0xFF)
                        + ", pos=" + (totalRead + i));
            }
            startValue = (startValue + 1) % 256;
        }
        return startValue;
    }

    private void recordError(String message) {
        Log.e(TAG, message, new Throwable(message));
        mErrors.add(message);
    }

    private interface Condition {
       boolean test();
    }

    private static boolean waitForCondition(Object obj, long timeout, Condition condition) {
        final long deadline = System.currentTimeMillis() + timeout;
        while (!condition.test()) {
            final long remainingTimeoutMs = deadline - System.currentTimeMillis();
            if (remainingTimeoutMs <= 0) {
                return false;
            }
            try {
                obj.wait(remainingTimeoutMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    private static boolean pollForCondition(long timeout, Condition condition) {
        final long deadline = System.currentTimeMillis() + timeout;
        while (!condition.test()) {
            final long remainingTimeoutMs = deadline - System.currentTimeMillis();
            if (remainingTimeoutMs <= 0) {
                return false;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    private class AlarmState implements EventManager.Alarm.Listener {
        final Runnable callback;
        final Object lockObject;
        volatile EventManager.Alarm alarm;
        volatile long notificationTime;
        volatile boolean wasCancelled;

        AlarmState(Runnable callback) {
            this(callback, null);
        }

        AlarmState(Runnable callback, Object lockObject) {
            this.callback = callback;
            this.lockObject = (lockObject != null ? lockObject : this);
        }

        boolean waitToComplete(long timeout) {
            synchronized (lockObject) {
                return waitForCondition(lockObject, timeout, () -> {
                    return (notificationTime != 0 || wasCancelled);
                });
            }
        }

        @Override
        public void onAlarm(EventManager.Alarm alarm, long elapsedTimeMs) {
            synchronized (lockObject) {
                if (this.alarm != null && alarm != this.alarm) {
                    recordError("onAlarm with a wrong alarm");
                    return;
                }
                if (elapsedTimeMs < 0) {
                    recordError("Notified with negative elapsedTimeMs " + elapsedTimeMs);
                }
                if (notificationTime != 0) {
                    recordError("Second call to onAlarm");
                }
                if (wasCancelled) {
                    recordError("onAlarm after onAlarmCancelled");
                }
                notificationTime = mOsAccess.monotonicTimeMillis();
                if (callback != null) {
                    callback.run();
                }
                lockObject.notifyAll();
            }
        }

        @Override
        public void onAlarmCancelled(EventManager.Alarm alarm) {
            synchronized (lockObject) {
                if (this.alarm != null && alarm != this.alarm) {
                    recordError("onAlarmCancelled with a wrong alarm");
                    return;
                }
                if (notificationTime != 0) {
                    recordError("onAlarmCancelled after onAlarm");
                }
                if (wasCancelled) {
                    recordError("Second call to onAlarmCancelled");
                }
                wasCancelled = true;
                if (callback != null) {
                    callback.run();
                }
                lockObject.notifyAll();
            }
        }
    }

    private class FileState implements AsyncFile.Listener {
        final ParcelFileDescriptor managedParcelFd;
        final ParcelFileDescriptor peerParcelFd;
        final FileDescriptor managedFd;
        final FileDescriptor peerFd;
        volatile AsyncFile file;
        volatile boolean hasCloseEvent;
        volatile boolean hasReadEvent;
        volatile boolean hasWriteEvent;

        FileState(ParcelFileDescriptor managedFd, ParcelFileDescriptor peerFd) {
            this.managedParcelFd = managedFd;
            this.peerParcelFd = peerFd;
            this.managedFd = mOsAccess.getInnerFileDescriptor(managedFd);
            this.peerFd = mOsAccess.getInnerFileDescriptor(peerFd);
        }

        void assertManagedFdClosed() {
            // EventManager may process file close before clearing FileDescriptor.
            pollForCondition(1000, () -> {
                return hasCloseEvent;
            });

            assertEquals(-1, mOsAccess.getFileDescriptorNumber(managedFd));
            assertTrue(hasCloseEvent);
        }

        synchronized boolean waitForReadEvent(long timeout) {
            return waitForCondition(this, timeout, () -> {
                return hasReadEvent;
            });
        }

        synchronized boolean waitForWriteEvent(long timeout) {
            return waitForCondition(this, timeout, () -> {
                return hasWriteEvent;
            });
        }

        void fillWriteBuffers(int firstValue, AtomicInteger totalWrittenCount) {
            executeEventManagerLoop(() -> {
                int currentValue = firstValue;
                while (true) {
                    final byte[] data = createTestData(currentValue, 1, 1000);
                    int writeCount = file.write(data, 1, data.length - 1);
                    if (writeCount == 0) {
                        break;
                    }
                    currentValue += writeCount;
                    totalWrittenCount.addAndGet(writeCount);
                }
            });
        }

        @Override
        public synchronized void onClosed(AsyncFile file) {
            if (file != this.file) {
                recordError("Notified with a wrong file");
                return;
            }
            hasCloseEvent = true;
            notifyAll();
        }

        @Override
        public synchronized void onReadReady(AsyncFile file) {
            if (file != this.file) {
                recordError("Notified with a wrong file");
                return;
            }
            hasReadEvent = true;
            notifyAll();
        }

        @Override
        public synchronized void onWriteReady(AsyncFile file) {
            if (file != this.file) {
                recordError("Notified with a wrong file");
                return;
            }
            hasWriteEvent = true;
            notifyAll();
        }
    }
}
