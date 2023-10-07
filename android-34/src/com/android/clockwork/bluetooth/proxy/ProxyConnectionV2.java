package com.android.clockwork.bluetooth.proxy;

import android.annotation.MainThread;
import android.annotation.NonNull;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;
import com.android.networkstack.tethering.companionproxy.io.AndroidOsAccess;
import com.android.networkstack.tethering.companionproxy.io.BufferedFile;
import com.android.networkstack.tethering.companionproxy.io.EventManager;
import com.android.networkstack.tethering.companionproxy.io.EventManagerImpl;
import com.android.networkstack.tethering.companionproxy.io.FileHandle;
import com.android.networkstack.tethering.companionproxy.io.OsAccess;
import com.android.networkstack.tethering.companionproxy.protocol.BtConnectionHandler;
import com.android.networkstack.tethering.companionproxy.protocol.LinkUsageStats;
import com.android.networkstack.tethering.companionproxy.protocol.LogUtils;
import com.android.networkstack.tethering.companionproxy.protocol.NetworkConfig;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages connection to the Phone-based Wearable Device Tethering.
 */
public class ProxyConnectionV2 implements ProxyConnection {
    private static final String TAG = LogUtils.TAG;

    // Maximum time to wait for the dump() call to complete on the EventManager thread.
    private static final int DUMP_WAIT_TIMEOUT = 1000;

    private static final int BT_CONNECTION_SHUTDOWN_TIMEOUT = 10000;

    // The client side first receives a 24-byte-long header.
    private static final int BLUEDROID_HEADER_SIZE = 24;

    private final Listener mListener;

    private final OsAccess mOsAccess = new AndroidOsAccess();
    private final CountDownLatch mHandshakeLatch = new CountDownLatch(1);
    private EventManagerImpl mEventManager;
    private BtConnectionHandler mBtConnection;
    private TunDevice mTunDevice;
    private boolean mStartedToConnect;
    private final String mTunInterfaceName;
    private final String mNameForLogging;

    private static final AtomicInteger sTunIndex = new AtomicInteger();

    public ProxyConnectionV2(Listener listener) {
        mListener = listener;

        final int tunIndex = sTunIndex.getAndIncrement();
        mTunInterfaceName = "teth-tun" + tunIndex;
        mNameForLogging = "PHONE" + tunIndex;
    }

    @Override
    public int connect(ParcelFileDescriptor btFd) {
        if (mStartedToConnect) {
            mOsAccess.close(btFd);
            throw new IllegalStateException();
        }
        mStartedToConnect = true;

        ParcelFileDescriptor tunFd = null;
        int result = CONNECT_RESULT_FAILED;
        try {
            CallbackHandler callbacks = new CallbackHandler();

            tunFd = ParcelFileDescriptor.adoptFd(openInterfaceNative(mTunInterfaceName, true));

            enableInterfaceNative(mTunInterfaceName);

            mEventManager = new EventManagerImpl(mOsAccess, callbacks, TAG);
            mEventManager.start();

            mTunDevice = new TunDevice(tunFd,
                BtConnectionHandler.MAX_IP_PACKET_LENGTH * 4,
                BtConnectionHandler.MAX_IP_PACKET_LENGTH * 4);

            BtConnectionHandler.Params btConnectionParams = new BtConnectionHandler.Params(
                mEventManager, mTunDevice.file.getInboundBuffer(), true, mNameForLogging);

            mBtConnection = BtConnectionHandler.createFromStream(
                btConnectionParams, FileHandle.fromFileDescriptor(btFd), callbacks);

            mBtConnection.start();
            mBtConnection.startHandshake();

            // We consider connection ready only after the handshake.
            result = CONNECT_RESULT_TIMEOUT;
        } catch (Throwable e) {
            Log.e(TAG, logStr("Unexpected exception starting ProxyConnectionV2 : "
                    + e.toString()), e);
        } finally {
            if (result != CONNECT_RESULT_TIMEOUT) {
                disconnect();
                mOsAccess.close(btFd);
                mOsAccess.close(tunFd);
            }
        }

        return result;
    }

    @Override
    public String getInterfaceName() {
        return mTunInterfaceName;
    }

    @Override
    public int getMtu() {
        return BtConnectionHandler.MAX_IP_PACKET_LENGTH;
    }

    @Override
    public int continueConnect() {
        if (!mStartedToConnect) {
            throw new IllegalStateException();
        }
        if (mEventManager == null) {
            return CONNECT_RESULT_FAILED;
        }

        try {
            if (mHandshakeLatch.await(1000, TimeUnit.MILLISECONDS)) {
                if (mBtConnection != null && mBtConnection.isOpen()) {
                    return CONNECT_RESULT_CONNECTED;
                }

                Log.i(TAG, logStr("Connection marked as closed while waiting to connect"));
                return CONNECT_RESULT_FAILED;
            }
        } catch (InterruptedException e) {
            // ignore
        }

        return CONNECT_RESULT_TIMEOUT;
    }

    private void onBtHandshakeDone() {
        mHandshakeLatch.countDown();
        continueReadingTunData();
    }

    @Override
    public boolean disconnect() {
        mStartedToConnect = true;

        if (mBtConnection != null) {
            BtConnectionHandler btConnection = mBtConnection;
            mBtConnection = null;

            final CountDownLatch latch = new CountDownLatch(1);
            btConnection.shutdown(() -> {
                latch.countDown();
            });

            try {
                latch.await(BT_CONNECTION_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (mTunDevice != null) {
            TunDevice tunDevice = mTunDevice;
            mTunDevice = null;
            tunDevice.file.close();
        }

        if (mEventManager != null) {
            // EventManager auto-closes all of our file descriptors.
            EventManagerImpl eventManager = mEventManager;
            mEventManager = null;
            eventManager.shutdown();
        }

        return false;
    }

    private void onBtConnectionClosed(String reason) {
        mHandshakeLatch.countDown();
        mListener.onDisconnect(-2000, reason);
    }

    private void startShutdownOnInternalError(String message) {
        Log.e(TAG, logStr(message));

        if (mBtConnection != null) {
            mBtConnection.shutdown(null);
        }
    }

    private void onBtSessionStalled(long stallElapsedTimeMs) {
        startShutdownOnInternalError(
            "Closing stalled connection, time=" + stallElapsedTimeMs);
    }

    private void continueReadingTunData() {
        if (mTunDevice != null && mHandshakeLatch.getCount() == 0) {
            mTunDevice.file.continueReading();
        }
    }

    private boolean onBtDataPacket(byte[] buffer, int pos, int len) {
        boolean success = mTunDevice.file.enqueueOutboundData(buffer, pos, len);
        if (!success) {
            // The peer sent us way too much data. It is not a recoverable error,
            // and the connection will be closed, so log a little more here.
            Log.w(TAG, logStr("Unable to buffer IP data for TUN. Buffer free_size="
                    + mTunDevice.file.getOutboundBufferFreeSize()
                    + ", buffered=" + mTunDevice.file.getOutboundBufferSize()
                    + ", payload=" + len));
        }
        return success;
    }

    private void onBtNetworkConfig(NetworkConfig networkConfig) {
        if (networkConfig.links.isEmpty()) {
            Log.i(TAG, logStr("Active network state change type=TYPE_NONE"));
            mListener.onActiveNetworkState(-1, false);  // TYPE_NONE.
            return;
        }

        int networkType;
        final NetworkConfig.LinkInfo link = networkConfig.links.get(0);
        if (link.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            if (link.hasCapability(NetworkCapabilities.NET_CAPABILITY_DUN)) {
                networkType = ConnectivityManager.TYPE_MOBILE_DUN;
            } else if (link.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)) {
                networkType = ConnectivityManager.TYPE_MOBILE_MMS;
            } else if (link.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)) {
                networkType = ConnectivityManager.TYPE_MOBILE_SUPL;
            } else {
                networkType = ConnectivityManager.TYPE_MOBILE;
            }
        } else if (link.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            if (link.hasCapability(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P)) {
                networkType = ConnectivityManager.TYPE_WIFI_P2P;
            } else {
                networkType = ConnectivityManager.TYPE_WIFI;
            }
        } else if (link.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
            networkType = ConnectivityManager.TYPE_BLUETOOTH;
        } else if (link.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            networkType = ConnectivityManager.TYPE_ETHERNET;
        } else {
            networkType = ConnectivityManager.TYPE_MOBILE;
            Log.e(TAG, logStr("Unexpected network transports: "
                    + Long.toHexString(link.transports)));
        }

        final boolean isMetered = !link.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_NOT_METERED);

        Log.i(TAG, logStr("Active network state change type=" + networkType
                + ", is_metered=" + isMetered));

        mListener.onActiveNetworkState(networkType, isMetered);
    }

    @Override
    public void dump(IndentingPrintWriter ipw) {
        ipw.print("ProxyConnectionV2 [");
        ipw.printPair("name", mNameForLogging);
        ipw.printPair("active", (mEventManager != null));
        ipw.printPair("iface", mTunInterfaceName);

        if (mEventManager == null) {
            ipw.println("]");
            return;
        }

        final Semaphore semaphore = new Semaphore(0);
        mEventManager.scheduleAlarm(0, new EventManager.Alarm.Listener() {
            @Override public void onAlarm(EventManager.Alarm alarm, long elapsedTimeMs) {
                ipw.increaseIndent();
                if (mBtConnection != null) {
                    mBtConnection.dump(ipw);
                }
                if (mTunDevice != null) {
                    mTunDevice.dump(ipw);
                }
                mEventManager.dump(ipw);
                ipw.decreaseIndent();
                semaphore.release();
            }

            @Override public void onAlarmCancelled(EventManager.Alarm alarm) {}
        });

        try {
            if (!semaphore.tryAcquire(DUMP_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                ipw.printPair("error", "dump_timeout");
            }
        } catch (InterruptedException e) {
            // ignore
        }

        ipw.println("]");
    }

    private String logStr(String message) {
        return "[ProxyConnectionV2:" + mNameForLogging + "] " + message;
    }

    private static native void classInitNative();
    private static native int openInterfaceNative(String iface, boolean isTun);
    private static native void enableInterfaceNative(String iface);

    static {
        try {
            System.loadLibrary("wear-bluetooth-proxyv2-jni");
            classInitNative();
        } catch (UnsatisfiedLinkError e) {
            // Invoked during testing
            Log.e(TAG, "Unable to load wear ProxyConnectionV2 jni native libraries");
        }
    }

    private class CallbackHandler
            implements EventManagerImpl.Listener, BtConnectionHandler.Listener {
        private int mRemainingPreambleSize = BLUEDROID_HEADER_SIZE;

        @Override
        public void onEventManagerFailure() {
            startShutdownOnInternalError("Closing after EventManager failure");
        }

        @Override
        public void onBtConnectionClosed(String reason) {
            ProxyConnectionV2.this.onBtConnectionClosed(reason);
        }

        @Override
        public void onBtHandshakeStart() {}

        @Override
        public void onBtHandshakeDone() {
            ProxyConnectionV2.this.onBtHandshakeDone();
        }

        @Override
        public void onBtSessionStalled(long stallElapsedTimeMs) {
            ProxyConnectionV2.this.onBtSessionStalled(stallElapsedTimeMs);
        }

        @Override
        public int onBtPreambleData(byte[] data, int pos, int len) {
            final int consumedCount = Math.min(mRemainingPreambleSize, len);
            mRemainingPreambleSize -= consumedCount;
            return consumedCount;
        }

        @Override
        public boolean onBtDataPacket(byte[] buffer, int pos, int len) {
            return ProxyConnectionV2.this.onBtDataPacket(buffer, pos, len);
        }

        @Override
        public void onConsumedDataSource() {
            ProxyConnectionV2.this.continueReadingTunData();
        }

        @Override
        public void onBtNetworkConfig(NetworkConfig networkConfig) {
            ProxyConnectionV2.this.onBtNetworkConfig(networkConfig);
        }

        @Override
        public void onBtLinkUsageStats(LinkUsageStats linkUsageStats) {
            // not yet implemented
        }
    }

    private class TunDevice implements BufferedFile.Listener {
        final BufferedFile file;

        TunDevice(ParcelFileDescriptor fd, int inboundBufferSize, int outboundBufferSize)
                throws IOException {
            file = BufferedFile.create(mEventManager, FileHandle.fromFileDescriptor(fd),
                this, inboundBufferSize, outboundBufferSize);
        }

        @Override
        public void onBufferedFileInboundData(int readByteCount) {
            if (LogUtils.verbose()) {
                Log.v(TAG, logStr("TUN bytes received: " + readByteCount
                        + ", buffered=" + file.getInboundBuffer().size()));
            }

            mBtConnection.maybeSendDataPackets();
        }

        @Override
        public void onBufferedFileClosed() {}

        @Override
        public void onBufferedFileOutboundSpace() {
            // Nothing to do - data is not cached locally by the protocol code.
        }

        @Override
        public void onBufferedFileIoError(String message) {
            startShutdownOnInternalError("Closing after TUN access failure: " + message);
        }

        void dump(IndentingPrintWriter ipw) {
            ipw.print("TunDevice [");
            ipw.increaseIndent();
            file.dump(ipw);
            ipw.decreaseIndent();
            ipw.println("]");
        }
    }
}
