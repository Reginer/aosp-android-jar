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

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.net.ipsec.ike.exceptions.IkeException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
     * A new on-demand liveness check has started. Called when a liveness check begins with a new
     * on-demand task.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi("com.android.ipsec.flags.liveness_check_api")
    int LIVENESS_STATUS_ON_DEMAND_STARTED = 0;

    /**
     * A new on-demand liveness check is running. Called when a liveness check request is already
     * running in an on-demand task.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi("com.android.ipsec.flags.liveness_check_api")
    int LIVENESS_STATUS_ON_DEMAND_ONGOING = 1;

    /**
     * A new on-demand liveness check has started. Called when a liveness check begins in background
     * with joining an existing running task.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi("com.android.ipsec.flags.liveness_check_api")
    int LIVENESS_STATUS_BACKGROUND_STARTED = 2;

    /**
     * A background liveness check is running. Called when a liveness check request is already
     * running in background.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi("com.android.ipsec.flags.liveness_check_api")
    int LIVENESS_STATUS_BACKGROUND_ONGOING = 3;

    /**
     * Success status. Called when the peer's liveness is proven.
     *
     * <p>Note that this status is a result status when the peer is proven as alive, regardless of
     * whether it is started in on-demand or in background.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi("com.android.ipsec.flags.liveness_check_api")
    int LIVENESS_STATUS_SUCCESS = 4;

    /**
     * Failure status. Called when the IKE message retransmission times out.
     *
     * <p>This failure status is called when retransmission timeouts have expired. The IkeSession
     * will be closed immediately by calling {@link IkeSessionCallback#onClosedWithException} with
     * {@link android.net.ipsec.ike.exceptions.IkeTimeoutException} in the {@link
     * IkeException#getCause()}.
     *
     * <p>Note that this status is a result status when the peer is determined as dead alive,
     * regardless of whether it is started in on-demand or in background.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi("com.android.ipsec.flags.liveness_check_api")
    int LIVENESS_STATUS_FAILURE = 5;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        LIVENESS_STATUS_ON_DEMAND_STARTED,
        LIVENESS_STATUS_ON_DEMAND_ONGOING,
        LIVENESS_STATUS_BACKGROUND_STARTED,
        LIVENESS_STATUS_BACKGROUND_ONGOING,
        LIVENESS_STATUS_SUCCESS,
        LIVENESS_STATUS_FAILURE,
    })
    @interface LivenessStatus {}

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

    /**
     * Called when the status changes for the liveness check request.
     *
     * <p>{@link LivenessStatus#LIVENESS_STATUS_ON_DEMAND_STARTED}: This status is called when
     * liveness checking is started with a new on-demand DPD task.
     *
     * <ul>
     *   <li>Note that when a client requests a liveness check, if no tasks are currently running in
     *       the session, a new on-demand DPD task is started and notified of {@link
     *       LivenessStatus#LIVENESS_STATUS_ON_DEMAND_STARTED}.
     *   <li>Note that a new on-demand DPD task uses retransmission timeouts from {@link
     *       IkeSessionParams#getLivenessRetransmissionTimeoutsMillis()}.
     * </ul>
     *
     * <p>{@link LivenessStatus#LIVENESS_STATUS_ON_DEMAND_ONGOING}: This status is called when
     * liveness checking is already running in an on-demand DPD task.
     *
     * <ul>
     *   <li>Note that when a client requests a liveness check, if there is already running in an
     *       on-demand DPD task, {@link LivenessStatus#LIVENESS_STATUS_ON_DEMAND_ONGOING} is
     *       notified.
     * </ul>
     *
     * <p>{@link LivenessStatus#LIVENESS_STATUS_BACKGROUND_STARTED}: This status is called when
     * liveness checking is started with joining an existing running task.
     *
     * <ul>
     *   <li>Note that if there is an existing running task in the session and no liveness check
     *       request is running in the background, the liveness check request will be joined to the
     *       existing running task in the background. Then, while joining, {@link
     *       LivenessStatus#LIVENESS_STATUS_BACKGROUND_STARTED} is notified.
     *   <li>Note that an existing running task uses retransmission timeouts from {@link
     *       IkeSessionParams#getRetransmissionTimeoutsMillis()}.
     * </ul>
     *
     * <p>{@link LivenessStatus#LIVENESS_STATUS_BACKGROUND_ONGOING}: This status is called when
     * liveness checking is already running with joining an existing running task.
     *
     * <ul>
     *   <li>Note that when a client requests a liveness check, if the request is already running in
     *       the background, {@link LivenessStatus#LIVENESS_STATUS_ON_DEMAND_ONGOING} is notified.
     * </ul>
     *
     * <p>{@link LivenessStatus#LIVENESS_STATUS_SUCCESS}: This status is called when the peer's
     * liveness is proven. Regardless of whether the request is running in an on-demand task or
     * running in the background, Success result is reported with this status. Once this status is
     * called, the liveness check request is done and no further status notifications are made until
     * the next {@link IkeSession#requestLivenessCheck}.
     *
     * <ul>
     *   <li>Note that if the peer's aliveness is proven in the on-demand DPD task, {@link
     *       LivenessStatus#LIVENESS_STATUS_SUCCESS} is notified as soon as a valid on-demand DPD
     *       response is received properly.
     *   <li>Note that if the peer's liveness is proven in a background liveness check with joining
     *       an existing running task, it can prove that the peer is alive for a valid incoming
     *       packet of the joined task. In this case, {@link LivenessStatus#LIVENESS_STATUS_SUCCESS}
     *       is notified as well.
     * </ul>
     *
     * <p>{@link LivenessStatus#LIVENESS_STATUS_FAILURE}: This state is called when the peer is
     * determined as dead for a liveness check request. After this status is called, the IkeSession
     * will be closed immediately by calling {@link IkeSessionCallback#onClosedWithException} with
     * {@link android.net.ipsec.ike.exceptions.IkeTimeoutException} in the {@link
     * IkeException#getCause()}. Depending on the type of task for which liveness checking is
     * performed, the failure result is reported as different retransmission timeouts.
     *
     * <ul>
     *   <li>Note that if an on-demand DPD task is running, This task takes retransmission timeouts
     *       from {@link IkeSessionParams#getLivenessRetransmissionTimeoutsMillis}, and after all
     *       timeouts expire, {@link LivenessStatus#LIVENESS_STATUS_FAILURE} is notified and is
     *       followed by closing session.
     *   <li>Note that, if the liveness check request is running in the background in the joined
     *       task, the task takes retransmission timeouts from {@link
     *       IkeSessionParams#getRetransmissionTimeoutsMillis()}, and after all timeouts expire,
     *       {@link LivenessStatus#LIVENESS_STATUS_FAILURE} is notified and is followed by closing
     *       session.
     * </ul>
     *
     * @param status the status of {@link LivenessStatus}
     * @hide
     */
    @SystemApi
    @FlaggedApi("com.android.ipsec.flags.liveness_check_api")
    default void onLivenessStatusChanged(@LivenessStatus int status) {}
}
