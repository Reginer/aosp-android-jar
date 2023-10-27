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

package android.net.ipsec.ike;

import android.annotation.NonNull;
import android.net.Network;

import java.net.InetAddress;
import java.util.Objects;

/**
 * IkeSessionConnectionInfo represents the connection information of an {@link IkeSession}.
 *
 * <p>Connection information includes IP addresses of both the IKE client and server and the network
 * being used.
 */
public final class IkeSessionConnectionInfo {
    private final InetAddress mLocalAddress;
    private final InetAddress mRemoteAddress;
    private final Network mNetwork;

    /**
     * Construct an instance of {@link IkeSessionConnectionInfo}.
     *
     * <p>Except for testing, IKE library users normally do not instantiate {@link
     * IkeSessionConnectionInfo} themselves but instead get a reference via {@link
     * IkeSessionConfiguration} or {@link IkeSessionCallback}
     */
    public IkeSessionConnectionInfo(
            @NonNull InetAddress localAddress,
            @NonNull InetAddress remoteAddress,
            @NonNull Network network) {
        Objects.requireNonNull(localAddress, "localAddress not provided");
        Objects.requireNonNull(remoteAddress, "remoteAddress not provided");
        Objects.requireNonNull(network, "network not provided");

        mLocalAddress = localAddress;
        mRemoteAddress = remoteAddress;
        mNetwork = network;
    }

    /**
     * Returns the local IP address for the underlying {@link Network} being used.
     *
     * @return the local IP address.
     */
    @NonNull
    public InetAddress getLocalAddress() {
        return mLocalAddress;
    }

    /**
     * Returns the remote IP address for the underlying {@link Network} being used.
     *
     * @return the remote IP address.
     */
    @NonNull
    public InetAddress getRemoteAddress() {
        return mRemoteAddress;
    }

    /**
     * Returns the underlying {@link Network} being used.
     *
     * @return the underlying {@link Network} that carries all IKE traffic.
     */
    @NonNull
    public Network getNetwork() {
        return mNetwork;
    }
}
