/*
 * Copyright (C) 2018 The Android Open Source Project
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

import java.net.InetAddress;
import java.util.concurrent.Executor;

/** @hide */
public final class NattSocketKeepalive extends SocketKeepalive {
    /** The NAT-T destination port for IPsec */
    public static final int NATT_PORT = 4500;

    @NonNull private final InetAddress mSource;
    @NonNull private final InetAddress mDestination;
    private final int mResourceId;

    public NattSocketKeepalive(@NonNull IConnectivityManager service,
            @NonNull Network network,
            @NonNull ParcelFileDescriptor pfd,
            int resourceId,
            @NonNull InetAddress source,
            @NonNull InetAddress destination,
            @NonNull Executor executor,
            @NonNull Callback callback) {
        super(service, network, pfd, executor, callback);
        mSource = source;
        mDestination = destination;
        mResourceId = resourceId;
    }

    /**
     * Request that keepalive be started with the given {@code intervalSec}.
     *
     * When a VPN is running with the network for this keepalive as its underlying network, the
     * system can monitor the TCP connections on that VPN to determine whether this keepalive is
     * necessary. To enable this behavior, pass {@link SocketKeepalive#FLAG_AUTOMATIC_ON_OFF} into
     * the flags. When this is enabled, the system will disable sending keepalive packets when
     * there are no TCP connections over the VPN(s) running over this network to save battery, and
     * restart sending them as soon as any TCP connection is opened over one of the VPN networks.
     * When no VPN is running on top of this network, this flag has no effect, i.e. the keepalives
     * are always sent with the specified interval.
     *
     * Also {@see SocketKeepalive}.
     *
     * @param intervalSec The target interval in seconds between keepalive packet transmissions.
     *                    The interval should be between 10 seconds and 3600 seconds. Otherwise,
     *                    the supplied {@link Callback} will see a call to
     *                    {@link Callback#onError(int)} with {@link #ERROR_INVALID_INTERVAL}.
     * @param flags Flags to enable/disable available options on this keepalive.
     * @param underpinnedNetwork The underpinned network of this keepalive.
     *
     * @hide
     */
    @Override
    protected void startImpl(int intervalSec, int flags, Network underpinnedNetwork) {
        if (0 != (flags & ~FLAG_AUTOMATIC_ON_OFF)) {
            throw new IllegalArgumentException("Illegal flag value for "
                    + this.getClass().getSimpleName() + " : " + flags);
        }
        final boolean automaticOnOffKeepalives = 0 != (flags & FLAG_AUTOMATIC_ON_OFF);
        mExecutor.execute(() -> {
            try {
                mService.startNattKeepaliveWithFd(mNetwork, mPfd, mResourceId,
                        intervalSec, mCallback, mSource.getHostAddress(),
                        mDestination.getHostAddress(), automaticOnOffKeepalives,
                        underpinnedNetwork);
            } catch (RemoteException e) {
                Log.e(TAG, "Error starting socket keepalive: ", e);
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
                Log.e(TAG, "Error stopping socket keepalive: ", e);
                throw e.rethrowFromSystemServer();
            }
        });
    }
}
