package com.android.clockwork.bluetooth;

import android.annotation.MainThread;
import android.annotation.Nullable;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import com.android.clockwork.bluetooth.DataForwarderThread.Listener;
import com.android.clockwork.common.LogUtil;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

/** Monitors data between sysproxy and the bluetooth processes. */
public class BluetoothSocketMonitor {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;
    private final Handler mainHandler;
    private final Listener listener;
    @Nullable private DataForwarderThread bluetoothToShardForwarder;
    @Nullable private DataForwarderThread shardToBluetoothForwarder;

    interface Listener {
        /**
         * Called on an arbitrary thread when sysproxy writes data to or receives data from the
         * bluetooth socket.
         */
        void onBluetoothData();
    }

    public BluetoothSocketMonitor(Listener listener) {
        this.listener = listener;
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Starts monitoring the data traffic on the given bluetooth file descriptor.
     *
     * <p>The socket monitor takes ownership of the bluetooth file descriptor after this call.
     * Therefore the caller should not use it any more and should use the returned file descriptor
     * instead.
     *
     * <p>NOTE: An instance must be started only once.
     *
     * @param bluetoothFd The file descriptor received from the bluetooth socket connect call.
     * @param listener The listener notified when sysproxy has outgoing bluetooth data.
     * @return The file descriptor the caller should use instead of the bluetooth file descriptor.
     */
    @MainThread
    public ParcelFileDescriptor start(ParcelFileDescriptor bluetoothFd) {
        try {
            // SOCK_SEQPACKET is needed to avoid exceeding the max packet size allowed by the peer.
            // Sysproxy keeps all written packets under the limit. This class keeps the forwarded
            // packets under the limit by maintaining the packet sizes used by sysproxy.
            //
            // Sysproxy does not actually rely on packet boundaries so, using SOCK_STREAM may also
            // seem fine here. However, with that option, multiple sysproxy packets could be read at
            // once and be forwarded as a single packet, which may potentially exceed the max packet
            // size.
            ParcelFileDescriptor[] newFds =
                    ParcelFileDescriptor.createSocketPair(OsConstants.SOCK_SEQPACKET);
            // New file descriptors are used for sysproxy <-> shard communication. ownFd is kept on
            // the proxy shard and peerFd is sent to the sysproxy process.
            ParcelFileDescriptor ownFd = newFds[0];
            ParcelFileDescriptor peerFd = newFds[1];
            LogUtil.logDOrNotUser(TAG, "Started monitoring BT socket");

            // File descriptors are duped to avoid one thread closing the descriptor while the other
            // is using it.
            bluetoothToShardForwarder =
                    new DataForwarderThread(
                            bluetoothFd.dup(),
                            ownFd.dup(),
                            "BtToShard",
                            new BluetoothToShardListener());
            shardToBluetoothForwarder =
                    new DataForwarderThread(
                            ownFd, bluetoothFd, "ShardToBt", new ShardToBluetoothListener());
            bluetoothToShardForwarder.start();
            shardToBluetoothForwarder.start();

            return peerFd;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create socket pair", e);
            return null;
        }
    }

    private class ShardToBluetoothListener implements DataForwarderThread.Listener {
        @Override
        public void onDataRead() {
            listener.onBluetoothData();
        }

        @Override
        public void onThreadFinishedRunning() {
            mainHandler.post(
                    () -> {
                        shardToBluetoothForwarder = null;
                        if (bluetoothToShardForwarder == null) {
                            LogUtil.logDOrNotUser(TAG, "Finished monitoring");
                        } else {
                            bluetoothToShardForwarder.interrupt();
                        }
                    });
        }
    }

    private class BluetoothToShardListener implements DataForwarderThread.Listener {
        @Override
        public void onDataRead() {
            listener.onBluetoothData();
        }

        @Override
        public void onThreadFinishedRunning() {
            mainHandler.post(
                    () -> {
                        bluetoothToShardForwarder = null;
                        if (shardToBluetoothForwarder == null) {
                            LogUtil.logDOrNotUser(TAG, "Finished monitoring");
                        } else {
                            shardToBluetoothForwarder.interrupt();
                        }
                    });
        }
    }
}

/**
 * Thread forwarding data from file descriptor to another. May not preserve message boundaries.
 *
 * <p>Stops running when one of the file descriptors is closed or if any read/write error is
 * received.
 */
class DataForwarderThread extends Thread {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;
    private ParcelFileDescriptor inputFd;
    private ParcelFileDescriptor outputFd;
    private String label;

    interface Listener {
        /** Called when new data is read from the input file descriptor. */
        void onDataRead();
        /** Called right before the thread finishes running. */
        void onThreadFinishedRunning();
    }

    private final Listener listener;

    /**
     * Creates a data forwarder.
     *
     * @param inputFd File descriptor data is read from.
     * @param outputFd File descriptor the data is written to.
     * @param label Label used when logging to identify this forwarder.
     * @param listener Listener that is notified on data forwarding events.
     */
    public DataForwarderThread(
            ParcelFileDescriptor inputFd,
            ParcelFileDescriptor outputFd,
            String label,
            Listener listener) {
        this.inputFd = inputFd;
        this.outputFd = outputFd;
        this.label = label;
        this.listener = listener;
    }

    public void run() {
        Log.d(TAG, label + " - Starting to forward data");
        try {
            transfer();
        } catch (ErrnoException e) {
            Log.e(TAG, label + " - Failed to copy", e);
        } catch (InterruptedIOException e) {
            Log.e(TAG, label + " - IO interrupted", e);
        } finally {
            listener.onThreadFinishedRunning();
        }
    }

    // Large enough to read any L2CAP packet.
    private static final int BUFFER_SIZE = 65535;

    public void transfer() throws InterruptedIOException, ErrnoException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        long totalRead = 0;
        long totalWritten = 0;
        try {
            while (!interrupted()) {
                int readCount = Os.read(inputFd.getFileDescriptor(), buffer);
                if (readCount == 0) {
                    // Zero read is actually ambiguous for SOCK_SEQPACKET sockets. It may either be
                    // an empty packet or EOF. EOF can be assumed here because sysproxy never writes
                    // empty packets.
                    LogUtil.logDOrNotUser(
                            TAG, label + " - Read EOF. Input FD is probably disconnected.");

                    try {
                        // Sends an explicit empty packet as an EOF signal, so that the reading
                        // process closes the file descriptor. As a result, the other thread,
                        // reading from this outputFd also receives EOF and finishes executing.
                        // This is required because interrupting the other thread is not enough to
                        // unblock it from reading.
                        Os.write(outputFd.getFileDescriptor(), new byte[0], 0, 0);
                    } catch (ErrnoException e) {
                        LogUtil.logDOrNotUser(
                                TAG, label + " - Output FD is probably disconnected.");
                    }
                    break;
                } else if (readCount < 0) {
                    Log.e(TAG, label + " - Read error: " + readCount);
                    break;
                }
                totalRead += readCount;
                listener.onDataRead();

                buffer.flip();
                while (!interrupted() && buffer.hasRemaining()) {
                    totalWritten += Os.write(outputFd.getFileDescriptor(), buffer);
                }
                buffer.clear();
            }
        } finally {
            LogUtil.logDOrNotUser(
                    TAG, label + " - Finished. read: " + totalRead + " written: " + totalWritten);
            try {
                inputFd.close();
            } catch (IOException e) {
                Log.e(TAG, label + " - Failed to close inputFd", e);
            }
            try {
                outputFd.close();
            } catch (IOException e) {
                Log.e(TAG, label + " - Failed to close outputFd", e);
            }
        }
    }
}
