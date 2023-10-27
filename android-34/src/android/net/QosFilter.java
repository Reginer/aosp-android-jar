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

import android.annotation.NonNull;
import android.annotation.SystemApi;

import java.net.InetAddress;

/**
 * Provides the related filtering logic to the {@link NetworkAgent} to match {@link QosSession}s
 * to their related {@link QosCallback}.
 *
 * Used by the {@link com.android.server.ConnectivityService} to validate a {@link QosCallback}
 * is still able to receive a {@link QosSession}.
 *
 * @hide
 */
@SystemApi
public abstract class QosFilter {

    /** @hide */
    protected QosFilter() {
        // Ensure that all derived types are known, and known to be properly handled when being
        // passed to and from NetworkAgent.
        // For now the only known derived type is QosSocketFilter.
        if (!(this instanceof QosSocketFilter)) {
            throw new UnsupportedOperationException(
                    "Unsupported QosFilter type: " + this.getClass().getName());
        }
    }

    /**
     * The network used with this filter.
     *
     * @return the registered {@link Network}
     */
    @NonNull
    public abstract Network getNetwork();

    /**
     * Validates that conditions have not changed such that no further {@link QosSession}s should
     * be passed back to the {@link QosCallback} associated to this filter.
     *
     * @return the error code when present, otherwise the filter is valid
     *
     * @hide
     */
    @QosCallbackException.ExceptionType
    public abstract int validate();

    /**
     * Determines whether or not the parameters will be matched with source address and port of this
     * filter.
     *
     * @param address the UE side address included in IP packet filter set of a QoS flow assigned
     *                on {@link Network}.
     * @param startPort the start of UE side port range included in IP packet filter set of a QoS
     *                flow assigned on {@link Network}.
     * @param endPort the end of UE side port range included in IP packet filter set of a QoS flow
     *                assigned on {@link Network}.
     * @return whether the parameters match the UE side address and port of the filter
     */
    public abstract boolean matchesLocalAddress(@NonNull InetAddress address,
            int startPort, int endPort);

    /**
     * Determines whether or not the parameters will be matched with remote address and port of
     * this filter.
     *
     * @param address the remote address included in IP packet filter set of a QoS flow
     *                assigned on {@link Network}.
     * @param startPort the start of remote port range included in IP packet filter set of a
     *                 QoS flow assigned on {@link Network}.
     * @param endPort the end of the remote range included in IP packet filter set of a QoS
     *                flow assigned on {@link Network}.
     * @return whether the parameters match the remote address and port of the filter
     */
    public abstract boolean matchesRemoteAddress(@NonNull InetAddress address,
            int startPort, int endPort);

    /**
     * Determines whether or not the parameter will be matched with this filter.
     *
     * @param protocol the protocol such as TCP or UDP included in IP packet filter set of a QoS
     *                 flow assigned on {@link Network}. Only {@code IPPROTO_TCP} and {@code
     *                 IPPROTO_UDP} currently supported.
     * @return whether the parameters match the socket type of the filter
     */
    // Since this method is added in U, it's required to be default method for binary compatibility
    // with existing @SystemApi.
    // IPPROTO_* are not compile-time constants, so they are not annotated with @IntDef.
    public boolean matchesProtocol(int protocol) {
        return false;
    }
}

