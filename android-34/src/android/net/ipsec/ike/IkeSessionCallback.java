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

package android.net.ipsec.ike;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.net.ipsec.ike.exceptions.IkeException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;

/**
 * Callback interface for receiving state changes of an {@link IkeSession}.
 *
 * <p>{@link IkeSessionCallback} MUST be unique to each {@link IkeSession}. It is registered when
 * callers are requesting a new {@link IkeSession}. It is automatically unregistered when an {@link
 * IkeSession} is closed.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296">RFC 7296, Internet Key Exchange Protocol
 *     Version 2 (IKEv2)</a>
 */
// Using interface instead of abstract class to indicate this callback does not have any state or
// implementation.
@SuppressLint("CallbackInterface")
public interface IkeSessionCallback {
    /**
     * Called when the {@link IkeSession} setup succeeds.
     *
     * <p>This method does not indicate the first Child Session has been setup. Caller MUST refer to
     * the corresponding {@link ChildSessionCallback} for the Child Session setup result.
     *
     * @param sessionConfiguration the configuration information of {@link IkeSession} negotiated
     *     during IKE setup.
     */
    void onOpened(@NonNull IkeSessionConfiguration sessionConfiguration);

    /**
     * Called when the {@link IkeSession} is closed.
     *
     * <p>When the closure is caused by a local, fatal error, {@link
     * #onClosedWithException(IkeException)} will be fired instead of this method.
     */
    void onClosed();

    /**
     * Called if {@link IkeSession} setup failed or {@link IkeSession} is closed because of a fatal
     * error.
     *
     * @param exception the detailed error information.
     * @deprecated Implementers should override {@link #onClosedWithException(IkeException)} to
     *     handle fatal {@link IkeException}s instead of using this method.
     * @hide
     */
    @SystemApi
    @Deprecated
    default void onClosedExceptionally(@NonNull IkeException exception) {}

    /**
     * Called if {@link IkeSession} setup failed or {@link IkeSession} is closed because of a fatal
     * error.
     *
     * @param exception the detailed error information.
     */
    default void onClosedWithException(@NonNull IkeException exception) {
        onClosedExceptionally(exception);
    }

    /**
     * Called if a recoverable error is encountered in an established {@link IkeSession}.
     *
     * <p>This method may be triggered by protocol errors such as an INVALID_IKE_SPI or
     * INVALID_MESSAGE_ID.
     *
     * @param exception the detailed error information.
     * @deprecated Implementers should override {@link #onError(IkeException)} to handle {@link
     *     IkeProtocolException}s instead of using this method.
     * @hide
     */
    @SystemApi
    @Deprecated
    default void onError(@NonNull IkeProtocolException exception) {}

    /**
     * Called if a recoverable error is encountered in an established {@link IkeSession}.
     *
     * <p>This method may be triggered by protocol errors such as an INVALID_IKE_SPI, or by
     * non-protocol errors such as the underlying {@link android.net.Network} dying.
     *
     * @param exception the detailed error information.
     */
    default void onError(@NonNull IkeException exception) {
        if (exception instanceof IkeProtocolException) {
            onError((IkeProtocolException) exception);
            return;
        }

        // do nothing for non-protocol errors by default
    }

    /**
     * Called if the IkeSessionConnectionInfo for an established {@link IkeSession} changes.
     *
     * <p>This method will only be called for MOBIKE-enabled Sessions, and only after a Mobility
     * Event occurs. An mobility event can happen in two Network modes:
     *
     * <ul>
     *   <li><b>Caller managed:</b> The caller controls the underlying Network for the IKE Session
     *       at all times. The IKE Session will only change underlying Networks if the caller
     *       initiates it through {@link IkeSession#setNetwork(Network)}. If the caller-specified
     *       Network is lost, they will be notified via {@link
     *       IkeSessionCallback#onError(android.net.ipsec.ike.exceptions.IkeException)} with an
     *       {@link android.net.ipsec.ike.exceptions.IkeNetworkLostException} specifying the Network
     *       that was lost.
     *   <li><b>Platform Default:</b> The IKE Session will always track the application default
     *       Network. The IKE Session will start on the application default Network, and any
     *       subsequent changes to the default Network (after the IKE_AUTH exchange completes) will
     *       cause the IKE Session's underlying Network to change. If the default Network is lost
     *       with no replacements, the caller will be notified via {@link
     *       IkeSessionCallback#onError(android.net.ipsec.ike.exceptions.IkeException)} with an
     *       {@link android.net.ipsec.ike.exceptions.IkeNetworkLostException}. The caller can either
     *       wait until for a new default Network to become available or they may close the Session
     *       manually via {@link IkeSession#close()}. Note that the IKE Session's maximum
     *       retransmissions may expire while waiting for a new default Network, in which case the
     *       Session will automatically close and {@link #onClosedWithException(IkeException)} will
     *       be fired.
     * </ul>
     *
     * <p>There are three types of mobility events:
     *
     * <ul>
     *   <li>The underlying Network changing, or
     *   <li>The local address disappearing from the current (and unchanged) underlying Network, or
     *   <li>The remote address changing.
     * </ul>
     *
     * @param connectionInfo the updated IkeSessionConnectionInfo for the Session.
     * @hide
     */
    @SystemApi
    default void onIkeSessionConnectionInfoChanged(
            @NonNull IkeSessionConnectionInfo connectionInfo) {}
}
