package com.android.networkstack.tethering.companionproxy.protocol;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.android.networkstack.tethering.companionproxy.io.EventManager;
import com.android.networkstack.tethering.companionproxy.io.RateLimiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class BtConnectionHandlerDataflowTest extends BtConnectionHandlerTestBase {
    @Before
    public void setUp() throws Exception {
        setUpBaseTest();
    }

    @After
    public void tearDown() throws Exception {
        tearDownBaseTest();
    }

    @Test
    public void dataflow() throws Exception {
        final TestState state = new TestState(2000, Integer.MAX_VALUE, 1000 * 1000);

        startDefaultClientAndServer(state);
        mClient.connection.startHandshake();

        assertTrue(mClient.waitForHandshake());
        assertTrue(mServer.waitForHandshake());

        state.scheduleRun(0);

        state.waitForCompletion(20000);

        mOsAccess.clearInboundRateLimit(mServer.innerFd);
        mOsAccess.clearOutboundRateLimit(mServer.innerFd);

        final int minExpectedBytes = 10 * 1000;
        if (state.totalBytesSent.get() < minExpectedBytes
                || state.totalBytesReceived.get() < minExpectedBytes) {
            fail("Too few bytes transferred: written=" + state.totalBytesSent.get()
                    + ", read=" + state.totalBytesReceived.get());
        }

        shutdownClientAndWait();
    }

    private abstract class BaseTestState {
        final long testDeadline;
        final Semaphore exitSemaphore = new Semaphore(0);
        final AtomicReference<Exception> exception = new AtomicReference<>();

        BaseTestState(long duration) {
            testDeadline = System.currentTimeMillis() + duration;
        }

        void waitForCompletion(int timeout) {
            try {
                assertTrue(exitSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                if (exception.get() == null) {
                    exception.set(e);
                }
            }

            if (exception.get() != null) {
                throw new RuntimeException(exception.get());
            }
        }

        void scheduleRun(int timeout) {
            mEventManager.scheduleAlarm(timeout, new EventManager.Alarm.Listener() {
                @Override public void onAlarm(EventManager.Alarm alarm, long elapsedTimeMs) {
                    run();
                }

                @Override public void onAlarmCancelled(EventManager.Alarm alarm) {}
            });
        }

        private void run() {
            if (exception.get() != null) {
                return;
            }

            if (System.currentTimeMillis() >= testDeadline) {
                exitSemaphore.release();
                return;
            }

            try {
                doRunOnce();
            } catch (Exception e) {
                setExceptionAndExitTest(e);
            }
        }

        protected final void setExceptionAndExitTest(Exception e) {
            exception.set(e);
            exitSemaphore.release();
        }

        protected abstract void doRunOnce() throws Exception;
    }

    private class TestState extends BaseTestState implements ConnectionListener {
        final RateLimiter srcLimiter;
        final RateLimiter dstLimiter;
        final AtomicInteger totalBytesSent = new AtomicInteger();
        final AtomicInteger totalBytesReceived = new AtomicInteger();

        TestState(long duration, int srcBytesPerSecond, int dstBytesPerSecond) {
            super(duration);
            srcLimiter = new RateLimiter(mOsAccess, srcBytesPerSecond);
            dstLimiter = new RateLimiter(mOsAccess, dstBytesPerSecond);
        }

        private void tryWriteData() throws Exception {
            final int originalSize = mClient.dataSource.freeSize();
            final int allowedSize = srcLimiter.limit(originalSize);
            if (allowedSize > 0) {
                final byte[] data = createTestData(totalBytesSent.get(), 0, allowedSize);
                mClient.dataSource.writeBytes(data, 0, allowedSize);
                totalBytesSent.addAndGet(allowedSize);
            }
        }

        @Override
        public void onConsumedDataSource(ConnectionState connection) {
            try {
                // There may be space in the buffer - try to write more.
                tryWriteData();
            } catch (Exception e) {
                setExceptionAndExitTest(e);
            }
        }

        @Override
        public boolean onBtDataPacket(
                ConnectionState connection, byte[] buffer, int pos, int len) {
            if (!dstLimiter.acceptAllOrNone(len)) {
                return false;
            }
            checkTestBytes(buffer, pos, len, totalBytesReceived.get(), totalBytesReceived.get());
            totalBytesReceived.addAndGet(len);
            return true;
        }

        @Override
        protected void doRunOnce() throws Exception {
            tryWriteData();
            mClient.connection.maybeSendDataPackets();

            scheduleRun(1000);
        }

        @Override
        public boolean onBtSessionStalled(ConnectionState connection, long stallElapsedTimeMs) {
            recordError("onBtSessionStalled: " + stallElapsedTimeMs);
            connection.startShutdown(null);
            return true;
        }
    }
}
