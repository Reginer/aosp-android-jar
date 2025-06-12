/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.Executor;

/** @hide */
public final class TcpSocketKeepalive extends SocketKeepalive {

    public TcpSocketKeepalive(@NonNull IConnectivityManager service,
            @NonNull Network network,
            @NonNull ParcelFileDescriptor pfd,
            @NonNull Executor executor,
            @NonNull Callback callback) {
        super(service, network, pfd, executor, callback);
    }

    /**
     * Starts keepalives. {@code mSocket} must be a connected TCP socket.
     *
     * - The application must not write to or read from the socket after calling this method, until
     *   onDataReceived, onStopped, or onError are called. If it does, the keepalive will fail
     *   with {@link #ERROR_SOCKET_NOT_IDLE}, or {@code #ERROR_INVALID_SOCKET} if the socket
     *   experienced an error (as in poll(2) returned POLLERR or POLLHUP); if this happens, the data
     *   received from the socket may be invalid, and the socket can't be recovered.
     * - If the socket has data in the send or receive buffer, then this call will fail with
     *   {@link #ERROR_SOCKET_NOT_IDLE} and can be retried after the data has been processed.
     *   An app could ensure this by using an application-layer protocol to receive acknowledgement
     *   that indicates all data has been delivered to server, e.g. HTTP 200 OK.
     *   Then the app could go into keepalive mode after reading all remaining data within the
     *   acknowledgement.
     */
    @Override
    protected void startImpl(int intervalSec, int flags, Network underpinnedNetwork) {
        if (0 != flags) {
            throw new IllegalArgumentException("Illegal flag value for "
                    + this.getClass().getSimpleName() + " : " + flags);
        }

        if (underpinnedNetwork != null) {
            throw new IllegalArgumentException("Illegal underpinned network for "
                    + this.getClass().getSimpleName() + " : " + underpinnedNetwork);
        }

        mExecutor.execute(() -> {
            try {
                mService.startTcpKeepalive(mNetwork, mPfd, intervalSec, mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Error starting packet keepalive: ", e);
                throw e.rethrowFromSystemServer();
            }
        });
    }

    @Override
    protected void stopImpl() {
        mExecutor.execute(() -> {
            try {
                mService.stopKeepalive(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Error stopping packet keepalive: ", e);
                throw e.rethrowFromSystemServer();
            }
        });
    }
}
