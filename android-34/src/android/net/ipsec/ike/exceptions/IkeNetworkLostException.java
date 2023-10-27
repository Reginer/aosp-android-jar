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

package android.net.ipsec.ike.exceptions;

import android.annotation.NonNull;
import android.net.Network;
import android.net.ipsec.ike.IkeSessionCallback;

import java.util.Objects;

/**
 * IkeNetworkLostException is returned to the caller via {@link
 * IkeSessionCallback#onError(IkeException)} if the underlying Network for the {@link
 * android.net.ipsec.ike.IkeSession} was lost with no alternatives.
 *
 * <p>This Exception corresponds to {@link
 * android.net.ConnectivityManager.NetworkCallback#onLost(android.net.Network)} being invoked for
 * the specified underlying Network.
 *
 * <p>When the caller receives this Exception, they must either:
 *
 * <ul>
 *   <li>set a new underlying Network for the corresponding IkeSession (MOBIKE must be enabled and
 *       the IKE Session must have started with a caller-configured Network), or
 *   <li>wait for a new underlying Network to become available (MOBIKE must be enabled and the IKE
 *       Session must be tracking the System default Network), or
 *       <ul>
 *         <li>Note: if the maximum retransmission time is encountered while waiting, the IKE
 *             Session will close. If this occurs, the caller will be notified via {@link
 *             IkeSessionCallback#onClosedWithException(IkeException)}.
 *       </ul>
 *   <li>close the corresponding IkeSession.
 * </ul>
 */
public final class IkeNetworkLostException extends IkeNonProtocolException {
    private final Network mNetwork;

    /** Constructs an IkeNetworkLostException to indicate the specified Network was lost. */
    public IkeNetworkLostException(@NonNull Network network) {
        super();
        Objects.requireNonNull(network, "network is null");

        mNetwork = network;
    }

    /** Returns the IkeSession's underlying Network that was lost. */
    @NonNull
    public Network getNetwork() {
        return mNetwork;
    }
}
