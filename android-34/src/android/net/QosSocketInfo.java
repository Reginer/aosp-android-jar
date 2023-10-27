/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.net;

import static android.system.OsConstants.SOCK_DGRAM;
import static android.system.OsConstants.SOCK_STREAM;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Used in conjunction with
 * {@link ConnectivityManager#registerQosCallback}
 * in order to receive Qos Sessions related to the local address and port of a bound {@link Socket}
 * and/or remote address and port of a connected {@link Socket}.
 *
 * @hide
 */
@SystemApi
public final class QosSocketInfo implements Parcelable {

    @NonNull
    private final Network mNetwork;

    @NonNull
    private final ParcelFileDescriptor mParcelFileDescriptor;

    @NonNull
    private final InetSocketAddress mLocalSocketAddress;

    @Nullable
    private final InetSocketAddress mRemoteSocketAddress;

    private final int mSocketType;

    /**
     * The {@link Network} the socket is on.
     *
     * @return the registered {@link Network}
     */
    @NonNull
    public Network getNetwork() {
        return mNetwork;
    }

    /**
     * The parcel file descriptor wrapped around the socket's file descriptor.
     *
     * @return the parcel file descriptor of the socket
     * @hide
     */
    @NonNull
    public ParcelFileDescriptor getParcelFileDescriptor() {
        return mParcelFileDescriptor;
    }

    /**
     * The local address of the socket passed into {@link QosSocketInfo(Network, Socket)}.
     * The value does not reflect any changes that occur to the socket after it is first set
     * in the constructor.
     *
     * @return the local address of the socket
     */
    @NonNull
    public InetSocketAddress getLocalSocketAddress() {
        return mLocalSocketAddress;
    }

    /**
     * The remote address of the socket passed into {@link QosSocketInfo(Network, Socket)}.
     * The value does not reflect any changes that occur to the socket after it is first set
     * in the constructor.
     *
     * @return the remote address of the socket if socket is connected, null otherwise
     */
    @Nullable
    public InetSocketAddress getRemoteSocketAddress() {
        return mRemoteSocketAddress;
    }

    /**
     * The socket type of the socket passed in when this QosSocketInfo object was constructed.
     *
     * @return the socket type of the socket.
     * @hide
     */
    public int getSocketType()  {
        return mSocketType;
    }

    /**
     * Creates a {@link QosSocketInfo} given a {@link Network} and bound {@link Socket}.  The
     * {@link Socket} must remain bound in order to receive {@link QosSession}s.
     *
     * @param network the network
     * @param socket the bound {@link Socket}
     */
    public QosSocketInfo(@NonNull final Network network, @NonNull final Socket socket)
            throws IOException {
        Objects.requireNonNull(socket, "socket cannot be null");

        mNetwork = Objects.requireNonNull(network, "network cannot be null");
        mParcelFileDescriptor = ParcelFileDescriptor.fromSocket(socket);
        mLocalSocketAddress =
                new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
        mSocketType = SOCK_STREAM;

        if (socket.isConnected()) {
            mRemoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        } else {
            mRemoteSocketAddress = null;
        }
    }

    /**
     * Creates a {@link QosSocketInfo} given a {@link Network} and bound {@link DatagramSocket}. The
     * {@link DatagramSocket} must remain bound in order to receive {@link QosSession}s.
     *
     * @param network the network
     * @param socket the bound {@link DatagramSocket}
     */
    public QosSocketInfo(@NonNull final Network network, @NonNull final DatagramSocket socket)
            throws IOException {
        Objects.requireNonNull(socket, "socket cannot be null");

        mNetwork = Objects.requireNonNull(network, "network cannot be null");
        mParcelFileDescriptor = ParcelFileDescriptor.fromDatagramSocket(socket);
        mLocalSocketAddress =
                new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
        mSocketType = SOCK_DGRAM;

        if (socket.isConnected()) {
            mRemoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        } else {
            mRemoteSocketAddress = null;
        }
    }

    /* Parcelable methods */
    private QosSocketInfo(final Parcel in) {
        mNetwork = Objects.requireNonNull(Network.CREATOR.createFromParcel(in));
        final boolean withFd = in.readBoolean();
        if (withFd) {
            mParcelFileDescriptor = ParcelFileDescriptor.CREATOR.createFromParcel(in);
        } else {
            mParcelFileDescriptor = null;
        }

        mLocalSocketAddress = readSocketAddress(in);
        mRemoteSocketAddress = readSocketAddress(in);

        mSocketType = in.readInt();
    }

    private InetSocketAddress readSocketAddress(final Parcel in) {
        final byte[] addrBytes = in.createByteArray();
        if (addrBytes == null) {
            return null;
        }
        final int port = in.readInt();

        try {
            return new InetSocketAddress(InetAddress.getByAddress(addrBytes), port);
        } catch (final UnknownHostException e) {
            /* This can never happen. UnknownHostException will never be thrown
               since the address provided is numeric and non-null. */
            throw new RuntimeException("UnknownHostException on numeric address", e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        writeToParcelInternal(dest, flags, /*includeFd=*/ true);
    }

    /**
     * Used when sending QosSocketInfo to telephony, which does not need access to the socket FD.
     * @hide
     */
    public void writeToParcelWithoutFd(@NonNull final Parcel dest, final int flags) {
        writeToParcelInternal(dest, flags, /*includeFd=*/ false);
    }

    private void writeToParcelInternal(
            @NonNull final Parcel dest, final int flags, boolean includeFd) {
        mNetwork.writeToParcel(dest, 0);

        if (includeFd) {
            dest.writeBoolean(true);
            mParcelFileDescriptor.writeToParcel(dest, 0);
        } else {
            dest.writeBoolean(false);
        }

        dest.writeByteArray(mLocalSocketAddress.getAddress().getAddress());
        dest.writeInt(mLocalSocketAddress.getPort());

        if (mRemoteSocketAddress == null) {
            dest.writeByteArray(null);
        } else {
            dest.writeByteArray(mRemoteSocketAddress.getAddress().getAddress());
            dest.writeInt(mRemoteSocketAddress.getPort());
        }
        dest.writeInt(mSocketType);
    }

    @NonNull
    public static final Parcelable.Creator<QosSocketInfo> CREATOR =
            new Parcelable.Creator<QosSocketInfo>() {
            @NonNull
            @Override
            public QosSocketInfo createFromParcel(final Parcel in) {
                return new QosSocketInfo(in);
            }

            @NonNull
            @Override
            public QosSocketInfo[] newArray(final int size) {
                return new QosSocketInfo[size];
            }
        };
}
