package com.android.networkstack.tethering.companionproxy.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.ParcelFileDescriptor;

import com.android.networkstack.tethering.companionproxy.io.EventManager;
import com.android.networkstack.tethering.companionproxy.io.EventManagerImpl;
import com.android.networkstack.tethering.companionproxy.io.FakeOsAccess;
import com.android.networkstack.tethering.companionproxy.io.FileHandle;
import com.android.networkstack.tethering.companionproxy.util.CircularByteBuffer;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class BtConnectionHandlerTestBase implements EventManagerImpl.Listener {
    protected static final String TAG = LogUtils.TAG;

    private static final int DEFAULT_DATA_BUFFER_SIZE = 1280;

    protected FakeOsAccess mOsAccess;
    protected EventManagerImpl mEventManager;
    protected ConnectionState mClient;
    protected ConnectionState mServer;

    private ArrayList<String> mErrors = new ArrayList<>();

    protected final void setUpBaseTest() throws Exception {
        mOsAccess = new FakeOsAccess(TAG);
        mEventManager = new EventManagerImpl(mOsAccess, this, TAG);
        mEventManager.start();
    }

    protected final void tearDownBaseTest() throws Exception {
        if (!mErrors.isEmpty()) {
            fail("Errors: " + mErrors.stream().collect(Collectors.joining("; ")));
        }

        if (mEventManager != null) {
            mEventManager.shutdown();
        }
    }

    protected void shutdownClientAndWait() {
        assertTrue(mClient.shutdownAndWait(1000));
        assertTrue(mClient.waitForClose(0, "Shutdown"));
    }

    protected void startDefaultClientAndServer() throws Exception {
        startDefaultClientAndServer(null);
    }

    protected void startDefaultClientAndServer(ConnectionListener listener)
            throws Exception {
        ParcelFileDescriptor[] fds = mOsAccess.socketpair();

        mClient = new ConnectionState(fds[0], true, listener, DEFAULT_DATA_BUFFER_SIZE);
        mClient.start();

        mServer = new ConnectionState(fds[1], false, listener, DEFAULT_DATA_BUFFER_SIZE);
        mServer.start();
    }

    protected static byte[] createTestData(int firstValue, int padLen, int dataLen) {
        final byte[] data = new byte[padLen + dataLen];
        for (int i = padLen; i < data.length; i++) {
            data[i] = (byte) (firstValue & 0xFF);
            firstValue++;
        }
        return data;
    }

    protected static int checkTestBytes(
            byte[] buffer, int pos, int len, int startValue, int totalRead) {
        for (int i = 0; i < len; i++) {
            byte expectedValue = (byte) (startValue & 0xFF);
            if (expectedValue != buffer[pos + i]) {
                throw new RuntimeException("Unexpected byte=" + (((int) buffer[pos + i]) & 0xFF)
                        + ", expected=" + (((int) expectedValue) & 0xFF)
                        + ", pos=" + (totalRead + i));
            }
            startValue = (startValue + 1) % 256;
        }
        return startValue;
    }

    @Override
    public void onEventManagerFailure() {
        recordError("onEventManagerFailure");
    }

    protected interface Condition {
       boolean test();
    }

    protected static boolean waitForCondition(Object obj, long timeout, Condition condition) {
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

    protected final void recordError(String message) {
        mErrors.add(message);
    }

    protected interface ConnectionListener {
        boolean onBtSessionStalled(ConnectionState connection, long stallElapsedTimeMs);
        boolean onBtDataPacket(ConnectionState connection, byte[] buffer, int pos, int len);
        void onConsumedDataSource(ConnectionState connection);
    }

    protected class ConnectionState implements BtConnectionHandler.Listener {

        final boolean isClient;
        final ConnectionListener listener;
        final CircularByteBuffer dataSource;
        final BtConnectionHandler connection;
        final FileDescriptor innerFd;
        final ArrayList<NetworkConfig> networkConfigList = new ArrayList<>();
        final ArrayList<LinkUsageStats> linkUsageStatsList = new ArrayList<>();
        String connectionClosedReason;
        boolean isHandshakeDone;
        boolean shouldAcceptHandshake = true;

        ConnectionState(ParcelFileDescriptor fd, boolean isClient, ConnectionListener listener,
                int dataSourceCapacity) throws Exception {
            this.isClient = isClient;
            this.listener = listener;

            innerFd = mOsAccess.getInnerFileDescriptor(fd);

            String fileName = (isClient ? "CLIENT" : "SERVER");
            mOsAccess.setFileName(innerFd, fileName);

            dataSource = new CircularByteBuffer(dataSourceCapacity);

            BtConnectionHandler.Params params = new BtConnectionHandler.Params(
                mEventManager, dataSource, isClient, fileName);

            connection = BtConnectionHandler.createFromStream(
                params, FileHandle.fromFileDescriptor(fd), this);
        }

        void start() {
            connection.start();
        }

        void startShutdown(Runnable shutdownCallback) {
            connection.shutdown(shutdownCallback);
        }

        boolean shutdownAndWait(int timeout) {
            final CountDownLatch latch = new CountDownLatch(1);

            connection.shutdown(() -> {
                latch.countDown();
            });

            try {
                return latch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }

        boolean waitForClose(String expectedReason) {
            return waitForClose(1000, expectedReason);
        }

        synchronized boolean waitForClose(int timeout, String expectedReason) {
            boolean success = waitForCondition(this, timeout, () -> {
                return (connectionClosedReason != null);
            });
            if (success && expectedReason != null) {
                assertEquals(expectedReason, connectionClosedReason);
            }
            return success;
        }

        synchronized boolean waitForHandshake() {
            return waitForCondition(this, 1000, () -> {
                return (isHandshakeDone);
            });
        }

        synchronized boolean waitForNetworkConfig() {
            return waitForCondition(this, 1000, () -> {
                return !networkConfigList.isEmpty();
            });
        }

        @Override
        public synchronized void onBtConnectionClosed(String reason) {
            if (reason == null) {
                recordError("Null reason in onBtConnectionClosed()");
                reason = "NULL";
            }
            if (connectionClosedReason != null) {
                recordError("Second onBtConnectionClosed(" + reason
                        + ", vs " + connectionClosedReason + ")");
                return;
            }
            connectionClosedReason = reason;
            notifyAll();
        }

        @Override
        public void onBtHandshakeStart() {
            if (isClient) {
                recordError("onBtHandshakeStart() on client");
                connection.shutdown(null);
                return;
            }

            if (shouldAcceptHandshake) {
                connection.acceptHandshake();
            }
        }

        @Override
        public synchronized void onBtHandshakeDone() {
            isHandshakeDone = true;
            notifyAll();
        }

        @Override
        public int onBtPreambleData(byte[] data, int pos, int len) {
            return 0;
        }

        @Override
        public synchronized boolean onBtDataPacket(byte[] buffer, int pos, int len) {
            if (listener == null) {
                recordError("Cannot work with data reads without listener");
                connection.shutdown(null);
                return false;
            }

            if (len > BtConnectionHandler.MAX_STREAMED_PACKET_SIZE) {
                recordError("onBtDataPacket len of " + len);
                connection.shutdown(null);
                return false;
            }

            return listener.onBtDataPacket(this, buffer, pos, len);
        }

        @Override
        public void onConsumedDataSource() {
            if (listener == null) {
                recordError("Cannot work with data writes without listener");
                connection.shutdown(null);
                return;
            }

            listener.onConsumedDataSource(this);
        }

        @Override
        public void onBtSessionStalled(long stallElapsedTimeMs) {
            if (listener == null || !listener.onBtSessionStalled(this, stallElapsedTimeMs)) {
                recordError("onBtSessionStalled: " + stallElapsedTimeMs);
                connection.shutdown(null);
            }
        }

        @Override
        public void onBtNetworkConfig(NetworkConfig networkConfig) {
            if (isClient) {
                networkConfigList.add(networkConfig);
            } else {
                recordError("onBtNetworkConfig on server side");
            }
        }

        @Override
        public void onBtLinkUsageStats(LinkUsageStats linkUsageStats) {
            if (isClient) {
                linkUsageStatsList.add(linkUsageStats);
            } else {
                recordError("onBtLinkUsageStats on server side");
            }
        }
    }
}
