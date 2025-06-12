/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.bluetooth;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

/**
 * A listening Bluetooth socket.
 *
 * <p>The interface for Bluetooth Sockets is similar to that of TCP sockets: {@link java.net.Socket}
 * and {@link java.net.ServerSocket}. On the server side, use a {@link BluetoothServerSocket} to
 * create a listening server socket. When a connection is accepted by the {@link
 * BluetoothServerSocket}, it will return a new {@link BluetoothSocket} to manage the connection. On
 * the client side, use a single {@link BluetoothSocket} to both initiate an outgoing connection and
 * to manage the connection.
 *
 * <p>For Bluetooth BR/EDR, the most common type of socket is RFCOMM, which is the type supported by
 * the Android APIs. RFCOMM is a connection-oriented, streaming transport over Bluetooth BR/EDR. It
 * is also known as the Serial Port Profile (SPP). To create a listening {@link
 * BluetoothServerSocket} that's ready for incoming Bluetooth BR/EDR connections, use {@link
 * BluetoothAdapter#listenUsingRfcommWithServiceRecord
 * BluetoothAdapter.listenUsingRfcommWithServiceRecord()}.
 *
 * <p>For Bluetooth LE, the socket uses LE Connection-oriented Channel (CoC). LE CoC is a
 * connection-oriented, streaming transport over Bluetooth LE and has a credit-based flow control.
 * Correspondingly, use {@link BluetoothAdapter#listenUsingL2capChannel
 * BluetoothAdapter.listenUsingL2capChannel()} to create a listening {@link BluetoothServerSocket}
 * that's ready for incoming Bluetooth LE CoC connections. For LE CoC, you can use {@link #getPsm()}
 * to get the protocol/service multiplexer (PSM) value that the peer needs to use to connect to your
 * socket.
 *
 * <p>After the listening {@link BluetoothServerSocket} is created, call {@link #accept()} to listen
 * for incoming connection requests. This call will block until a connection is established, at
 * which point, it will return a {@link BluetoothSocket} to manage the connection. Once the {@link
 * BluetoothSocket} is acquired, it's a good idea to call {@link #close()} on the {@link
 * BluetoothServerSocket} when it's no longer needed for accepting connections. Closing the {@link
 * BluetoothServerSocket} will <em>not</em> close the returned {@link BluetoothSocket}.
 *
 * <p>{@link BluetoothServerSocket} is thread safe. In particular, {@link #close} will always
 * immediately abort ongoing operations and close the server socket.
 *
 * <p><div class="special reference">
 *
 * <h3>Developer Guides</h3>
 *
 * <p>For more information about using Bluetooth, read the <a
 * href="{@docRoot}guide/topics/connectivity/bluetooth.html">Bluetooth</a> developer guide. </div>
 *
 * @see BluetoothSocket
 */
@SuppressLint("AndroidFrameworkBluetoothPermission")
public final class BluetoothServerSocket implements Closeable {
    private static final String TAG = "BluetoothServerSocket";

    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    @UnsupportedAppUsage(
            publicAlternatives = "Use public {@link BluetoothServerSocket} API " + "instead.")
    /*package*/ final BluetoothSocket mSocket;

    private int mChannel;
    private long mSocketCreationTimeMillis = 0;
    private long mSocketCreationLatencyMillis = 0;

    // BluetoothSocket.getConnectionType() will hide L2CAP_LE.
    // Therefore a new variable need to be maintained here.
    private int mType;

    /**
     * Construct a socket for incoming connections.
     *
     * @param type type of socket
     * @param auth require the remote device to be authenticated
     * @param encrypt require the connection to be encrypted
     * @param port remote port
     * @throws IOException On error, for example Bluetooth not available, or insufficient privileges
     */
    /*package*/ BluetoothServerSocket(int type, boolean auth, boolean encrypt, int port)
            throws IOException {
        mSocketCreationTimeMillis = System.currentTimeMillis();
        mType = type;
        mChannel = port;
        mSocket = new BluetoothSocket(type, auth, encrypt, port, null);
        if (port == BluetoothAdapter.SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
            mSocket.setExcludeSdp(true);
        }
        mSocketCreationLatencyMillis = System.currentTimeMillis() - mSocketCreationTimeMillis;
    }

    /**
     * Construct a socket for incoming connections.
     *
     * @param type type of socket
     * @param auth require the remote device to be authenticated
     * @param encrypt require the connection to be encrypted
     * @param port remote port
     * @param pitm enforce person-in-the-middle protection for authentication.
     * @param min16DigitPin enforce a minimum length of 16 digits for a sec mode 2 connection
     * @throws IOException On error, for example Bluetooth not available, or insufficient privileges
     */
    /*package*/ BluetoothServerSocket(
            int type, boolean auth, boolean encrypt, int port, boolean pitm, boolean min16DigitPin)
            throws IOException {
        mSocketCreationTimeMillis = System.currentTimeMillis();
        mType = type;
        mChannel = port;
        mSocket = new BluetoothSocket(type, auth, encrypt, port, null, pitm, min16DigitPin);
        if (port == BluetoothAdapter.SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
            mSocket.setExcludeSdp(true);
        }
        mSocketCreationLatencyMillis = System.currentTimeMillis() - mSocketCreationTimeMillis;
    }

    /**
     * Construct a socket for incoming connections.
     *
     * @param type type of socket
     * @param auth require the remote device to be authenticated
     * @param encrypt require the connection to be encrypted
     * @param uuid uuid
     * @throws IOException On error, for example Bluetooth not available, or insufficient privileges
     */
    /*package*/ BluetoothServerSocket(int type, boolean auth, boolean encrypt, ParcelUuid uuid)
            throws IOException {
        mSocketCreationTimeMillis = System.currentTimeMillis();
        mType = type;
        mSocket = new BluetoothSocket(type, auth, encrypt, -1, uuid);
        // TODO: This is the same as mChannel = -1 - is this intentional?
        mChannel = mSocket.getPort();
        mSocketCreationLatencyMillis = System.currentTimeMillis() - mSocketCreationTimeMillis;
    }

    /**
     * Construct a socket for incoming connections.
     *
     * @param type type of socket
     * @param auth require the remote device to be authenticated
     * @param encrypt require the connection to be encrypted
     * @param port remote port
     * @param uuid uuid
     * @param pitm enforce person-in-the-middle protection for authentication.
     * @param min16DigitPin enforce a minimum length of 16 digits for a sec mode 2 connection
     * @param dataPath data path used for this socket
     * @param socketName user-friendly name for this socket
     * @param hubId ID of the hub to which the end point belongs
     * @param endpointId ID of the endpoint within the hub that is associated with this socket
     * @param maximumPacketSize The maximum size (in bytes) of a single data packet
     * @throws IOException On error, for example Bluetooth not available, or insufficient privileges
     */
    /*package*/ BluetoothServerSocket(
            int type,
            boolean auth,
            boolean encrypt,
            int port,
            ParcelUuid uuid,
            boolean pitm,
            boolean min16DigitPin,
            int dataPath,
            @NonNull String socketName,
            long hubId,
            long endpointId,
            int maximumPacketSize)
            throws IOException {
        mSocketCreationTimeMillis = System.currentTimeMillis();
        mType = type;
        mChannel = port;
        mSocket =
                new BluetoothSocket(
                        type,
                        auth,
                        encrypt,
                        port,
                        uuid,
                        pitm,
                        min16DigitPin,
                        dataPath,
                        socketName,
                        hubId,
                        endpointId,
                        maximumPacketSize);
        if (port == BluetoothAdapter.SOCKET_CHANNEL_AUTO_STATIC_NO_SDP) {
            mSocket.setExcludeSdp(true);
        }
        mSocketCreationLatencyMillis = System.currentTimeMillis() - mSocketCreationTimeMillis;
    }

    /**
     * Block until a connection is established.
     *
     * <p>Returns a connected {@link BluetoothSocket} on successful connection.
     *
     * <p>Once this call returns, it can be called again to accept subsequent incoming connections.
     *
     * <p>{@link #close} can be used to abort this call from another thread.
     *
     * @return a connected {@link BluetoothSocket}
     * @throws IOException on error, for example this call was aborted, or timeout
     */
    public BluetoothSocket accept() throws IOException {
        return accept(-1);
    }

    /**
     * Block until a connection is established, with timeout.
     *
     * <p>Returns a connected {@link BluetoothSocket} on successful connection.
     *
     * <p>Once this call returns, it can be called again to accept subsequent incoming connections.
     *
     * <p>{@link #close} can be used to abort this call from another thread.
     *
     * @return a connected {@link BluetoothSocket}
     * @throws IOException on error, for example this call was aborted, or timeout
     */
    public BluetoothSocket accept(int timeout) throws IOException {
        long socketConnectionTime = System.currentTimeMillis();
        BluetoothSocket acceptedSocket = null;
        try {
            acceptedSocket = mSocket.accept(timeout);
            SocketMetrics.logSocketAccept(
                    acceptedSocket,
                    mSocket,
                    mType,
                    mChannel,
                    timeout,
                    SocketMetrics.RESULT_L2CAP_CONN_SUCCESS,
                    mSocketCreationTimeMillis,
                    mSocketCreationLatencyMillis,
                    socketConnectionTime);
            return acceptedSocket;
        } catch (IOException e) {
            SocketMetrics.logSocketAccept(
                    acceptedSocket,
                    mSocket,
                    mType,
                    mChannel,
                    timeout,
                    SocketMetrics.RESULT_L2CAP_CONN_SERVER_FAILURE,
                    mSocketCreationTimeMillis,
                    mSocketCreationLatencyMillis,
                    socketConnectionTime);
            throw e;
        }
    }

    /**
     * Immediately close this socket, and release all associated resources.
     *
     * <p>Causes blocked calls on this socket in other threads to immediately throw an IOException.
     *
     * <p>Closing the {@link BluetoothServerSocket} will <em>not</em> close any {@link
     * BluetoothSocket} received from {@link #accept()}.
     */
    public void close() throws IOException {
        if (DBG) Log.d(TAG, "BluetoothServerSocket:close() called. mChannel=" + mChannel);
        mSocket.close();
    }

    /*package*/ void setServiceName(String serviceName) {
        mSocket.setServiceName(serviceName);
    }

    /**
     * Returns the channel on which this socket is bound.
     *
     * @hide
     */
    public int getChannel() {
        return mChannel;
    }

    /**
     * Returns the assigned dynamic protocol/service multiplexer (PSM) value for the listening L2CAP
     * Connection-oriented Channel (CoC) server socket. This server socket must be returned by the
     * {@link BluetoothAdapter#listenUsingL2capChannel()} or {@link
     * BluetoothAdapter#listenUsingInsecureL2capChannel()}. The returned value is undefined if this
     * method is called on non-L2CAP server sockets.
     *
     * @return the assigned PSM or LE_PSM value depending on transport
     */
    public int getPsm() {
        return mChannel;
    }

    /**
     * Sets the channel on which future sockets are bound. Currently used only when a channel is
     * auto generated.
     */
    /*package*/ void setChannel(int newChannel) {
        /* TODO: From a design/architecture perspective this is wrong.
         *       The bind operation should be conducted through this class
         *       and the resulting port should be kept in mChannel, and
         *       not set from BluetoothAdapter. */
        if (mSocket != null) {
            if (mSocket.getPort() != newChannel) {
                Log.w(
                        TAG,
                        "The port set is different that the underlying port. mSocket.getPort(): "
                                + mSocket.getPort()
                                + " requested newChannel: "
                                + newChannel);
            }
        }
        mChannel = newChannel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServerSocket: Type: ");
        switch (mSocket.getConnectionType()) {
            case BluetoothSocket.TYPE_RFCOMM:
                {
                    sb.append("TYPE_RFCOMM");
                    break;
                }
            case BluetoothSocket.TYPE_L2CAP:
                {
                    sb.append("TYPE_L2CAP");
                    break;
                }
            case BluetoothSocket.TYPE_L2CAP_LE:
                {
                    sb.append("TYPE_L2CAP_LE");
                    break;
                }
            case BluetoothSocket.TYPE_SCO:
                {
                    sb.append("TYPE_SCO");
                    break;
                }
        }
        sb.append(" Channel: ").append(mChannel);
        return sb.toString();
    }
}
