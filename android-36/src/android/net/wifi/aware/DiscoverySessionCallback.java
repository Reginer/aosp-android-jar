/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.net.wifi.aware;

import static com.android.ranging.flags.Flags.FLAG_RANGING_RTT_ENABLED;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.net.wifi.rtt.RangingResult;

import java.util.List;

/**
 * Base class for Aware session events callbacks. Should be extended by
 * applications wanting notifications. The callbacks are set when a
 * publish or subscribe session is created using
 * {@link WifiAwareSession#publish(PublishConfig, DiscoverySessionCallback,
 * android.os.Handler)} or
 * {@link WifiAwareSession#subscribe(SubscribeConfig, DiscoverySessionCallback,
 * android.os.Handler)}.
 * <p>
 * A single callback is set at session creation - it cannot be replaced.
 */
public class DiscoverySessionCallback {
    /**
     * Called when a publish operation is started successfully in response to a
     * {@link WifiAwareSession#publish(PublishConfig, DiscoverySessionCallback,
     * android.os.Handler)} operation.
     *
     * @param session The {@link PublishDiscoverySession} used to control the
     *            discovery session.
     */
    public void onPublishStarted(@NonNull PublishDiscoverySession session) {
        /* empty */
    }

    /**
     * Called when a subscribe operation is started successfully in response to a
     * {@link WifiAwareSession#subscribe(SubscribeConfig, DiscoverySessionCallback,
     * android.os.Handler)} operation.
     *
     * @param session The {@link SubscribeDiscoverySession} used to control the
     *            discovery session.
     */
    public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
        /* empty */
    }

    /**
     * Called when a publish or subscribe discovery session configuration update request
     * succeeds. Called in response to
     * {@link PublishDiscoverySession#updatePublish(PublishConfig)} or
     * {@link SubscribeDiscoverySession#updateSubscribe(SubscribeConfig)}.
     */
    public void onSessionConfigUpdated() {
        /* empty */
    }

    /**
     * Called when a publish or subscribe discovery session cannot be created:
     * {@link WifiAwareSession#publish(PublishConfig, DiscoverySessionCallback,
     * android.os.Handler)} or
     * {@link WifiAwareSession#subscribe(SubscribeConfig, DiscoverySessionCallback,
     * android.os.Handler)}, or when a configuration update fails:
     * {@link PublishDiscoverySession#updatePublish(PublishConfig)} or
     * {@link SubscribeDiscoverySession#updateSubscribe(SubscribeConfig)}.
     * <p>
     *     For discovery session updates failure leaves the session running with its previous
     *     configuration - the discovery session is not terminated.
     */
    public void onSessionConfigFailed() {
        /* empty */
    }

    /**
     * Called when a discovery session (publish or subscribe) terminates. Termination may be due
     * to user-request (either directly through {@link DiscoverySession#close()} or
     * application-specified expiration, e.g. {@link PublishConfig.Builder#setTtlSec(int)}
     * or {@link SubscribeConfig.Builder#setTtlSec(int)}).
     */
    public void onSessionTerminated() {
        /* empty */
    }

    /**
     * Called when a discovery session (publish or subscribe) has been suspended successfully.
     * Suspension is triggered by {@link DiscoverySession#suspend()}.
     * @hide
     */
    @SystemApi
    public void onSessionSuspendSucceeded() {
        /* empty */
    }

    /**
     * Called when a discovery session (publish or subscribe) has failed to be suspended
     * successfully. Suspension is triggered by {@link DiscoverySession#suspend()}.
     *
     * @param reason One of:
     *      {@link WifiAwareManager#WIFI_AWARE_SUSPEND_REDUNDANT_REQUEST},
     *      {@link WifiAwareManager#WIFI_AWARE_SUSPEND_INVALID_SESSION},
     *      {@link WifiAwareManager#WIFI_AWARE_SUSPEND_CANNOT_SUSPEND},
     *      {@link WifiAwareManager#WIFI_AWARE_SUSPEND_INTERNAL_ERROR}
     * @hide
     */
    @SystemApi
    public void onSessionSuspendFailed(
            @WifiAwareManager.SessionSuspensionFailedReasonCode int reason) {
        /* empty */
    }

    /**
     * Called when a discovery session (publish or subscribe) has been resumed from suspension
     * successfully. Resumption is triggered by {@link DiscoverySession#resume()}.
     * @hide
     */
    @SystemApi
    public void onSessionResumeSucceeded() {
        /* empty */
    }

    /**
     * Called when a discovery session (publish or subscribe) has failed to be resumed from
     * suspension successfully. Resumption is triggered by {@link DiscoverySession#resume()}.
     *
     * @param reason One of:
     *      {@link WifiAwareManager#WIFI_AWARE_RESUME_REDUNDANT_REQUEST},
     *      {@link WifiAwareManager#WIFI_AWARE_RESUME_INVALID_SESSION},
     *      {@link WifiAwareManager#WIFI_AWARE_RESUME_INTERNAL_ERROR}
     * @hide
     */
    @SystemApi
    public void onSessionResumeFailed(
            @WifiAwareManager.SessionResumptionFailedReasonCode int reason) {
        /* empty */
    }

    /**
     * Called when a subscribe operation results in a service discovery.
     * <p>
     * Note that this method and
     * {@link #onServiceDiscoveredWithinRange(PeerHandle, byte[], List, int)} may be called
     * multiple times per service discovery.
     * <p>
     * Note: This method is superseded by {@link #onServiceDiscovered(ServiceDiscoveryInfo)} which
     * returns more information. Note that both legacy and new callback will be triggered on
     * discovery.
     *
     * @param peerHandle An opaque handle to the peer matching our discovery operation.
     * @param serviceSpecificInfo The service specific information (arbitrary
     *            byte array) provided by the peer as part of its discovery
     *            configuration.
     * @param matchFilter The filter which resulted in this service discovery. For
     * {@link PublishConfig#PUBLISH_TYPE_UNSOLICITED},
     * {@link SubscribeConfig#SUBSCRIBE_TYPE_PASSIVE} discovery sessions this is the publisher's
     *                    match filter. For {@link PublishConfig#PUBLISH_TYPE_SOLICITED},
     *                    {@link SubscribeConfig#SUBSCRIBE_TYPE_ACTIVE} discovery sessions this
     *                    is the subscriber's match filter.
     */
    public void onServiceDiscovered(PeerHandle peerHandle,
            byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
        /* empty */
    }

    /**
     * Called when a subscribe operation results in a service discovery.
     * <p>
     * Note: This method supersedes {@link #onServiceDiscovered(PeerHandle, byte[], List)} and
     * provides additional information - including cipher suite type and security context of the
     * peer. Both the legacy and the new callback will be triggered on discovery.
     *
     * @param info A {@link ServiceDiscoveryInfo} structure containing information on the discovery
     *             session and the discovered peer.
     */
    public void onServiceDiscovered(@NonNull ServiceDiscoveryInfo info) {
        /* empty */
    }

    /**
     * Called when a subscribe operation results in a
     * service discovery. Called when a Subscribe service was configured with a range requirement
     * {@link SubscribeConfig.Builder#setMinDistanceMm(int)} and/or
     * {@link SubscribeConfig.Builder#setMaxDistanceMm(int)} and the Publish service was configured
     * with {@link PublishConfig.Builder#setRangingEnabled(boolean)}.
     * <p>
     * If either Publisher or Subscriber does not enable Ranging, or if Ranging is temporarily
     * disabled by the underlying device, service discovery proceeds without ranging and the
     * {@link #onServiceDiscovered(PeerHandle, byte[], List)} is called.
     * <p>
     * Note that this method and {@link #onServiceDiscovered(PeerHandle, byte[], List)} may be
     * called multiple times per service discovery.
     * <p>
     * Note: This method is superseded by
     * {@link #onServiceDiscoveredWithinRange(ServiceDiscoveryInfo, int)} which returns more
     * information. Note that both legacy and new callback will be triggered on discovery.
     *
     * @param peerHandle An opaque handle to the peer matching our discovery operation.
     * @param serviceSpecificInfo The service specific information (arbitrary
     *            byte array) provided by the peer as part of its discovery
     *            configuration.
     * @param matchFilter The filter which resulted in this service discovery. For
     * {@link PublishConfig#PUBLISH_TYPE_UNSOLICITED},
     * {@link SubscribeConfig#SUBSCRIBE_TYPE_PASSIVE} discovery sessions this is the publisher's
     *                    match filter. For {@link PublishConfig#PUBLISH_TYPE_SOLICITED},
     *                    {@link SubscribeConfig#SUBSCRIBE_TYPE_ACTIVE} discovery sessions this
     *                    is the subscriber's match filter.
     * @param distanceMm The measured distance to the Publisher in mm. Note: the measured distance
     *                   may be negative for very close devices.
     */
    public void onServiceDiscoveredWithinRange(PeerHandle peerHandle,
        byte[] serviceSpecificInfo, List<byte[]> matchFilter, int distanceMm) {
        /* empty */
    }

    /**
     * Called when a subscribe operation results in a
     * service discovery. Called when a Subscribe service was configured with a range requirement
     * {@link SubscribeConfig.Builder#setMinDistanceMm(int)} and/or
     * {@link SubscribeConfig.Builder#setMaxDistanceMm(int)} and the Publish service was configured
     * with {@link PublishConfig.Builder#setRangingEnabled(boolean)}.
     * <p>
     * If either Publisher or Subscriber does not enable Ranging, or if Ranging is temporarily
     * disabled by the underlying device, service discovery proceeds without ranging and the
     * {@link #onServiceDiscovered(PeerHandle, byte[], List)} is called.
     * <p>
     * Note: This method supersedes
     * {@link #onServiceDiscoveredWithinRange(PeerHandle, byte[], List, int)} and provides
     * additional information - including cipher suite type and security context of the peer. Both
     * the legacy and the new callback will be triggered on discovery.
     *
     * @param info A {@link ServiceDiscoveryInfo} which indicate service config of the descovery
     *             sessions.
     * @param distanceMm The measured distance to the Publisher in mm. Note: the measured distance
 *                   may be negative for very close devices.
     */
    public void onServiceDiscoveredWithinRange(@NonNull ServiceDiscoveryInfo info, int distanceMm) {
        /* empty */
    }

    /**
     * Called in response to
     * {@link DiscoverySession#sendMessage(PeerHandle, int, byte[])}
     * when a message is transmitted successfully - i.e. when it was received successfully by the
     * peer (corresponds to an ACK being received).
     * <p>
     * Note that either this callback or
     * {@link DiscoverySessionCallback#onMessageSendFailed(int)} will be
     * received - never both.
     *
     * @param messageId The arbitrary message ID specified when sending the message.
     */
    public void onMessageSendSucceeded(@SuppressWarnings("unused") int messageId) {
        /* empty */
    }

    /**
     * Called when message transmission initiated with
     * {@link DiscoverySession#sendMessage(PeerHandle, int, byte[])} fails. E.g. when no ACK is
     * received from the peer.
     * <p>
     * Note that either this callback or
     * {@link DiscoverySessionCallback#onMessageSendSucceeded(int)} will be received
     * - never both.
     *
     * @param messageId The arbitrary message ID specified when sending the message.
     */
    public void onMessageSendFailed(@SuppressWarnings("unused") int messageId) {
        /* empty */
    }

    /**
     * Called when a message is received from a discovery session peer - in response to the
     * peer's {@link DiscoverySession#sendMessage(PeerHandle, int, byte[])}.
     *
     * @param peerHandle An opaque handle to the peer matching our discovery operation.
     * @param message A byte array containing the message.
     */
    public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
        /* empty */
    }

    /**
     * Called when the discovered service is not available. All further operations on this
     * discovery session will fail. If the service is available again,
     * {@link #onServiceDiscovered(PeerHandle, byte[], List)} or
     * {@link #onServiceDiscoveredWithinRange(PeerHandle, byte[], List, int)} will be called.
     *
     * @param peerHandle An opaque handle to the peer matching our discovery operation.
     * @param reason Discovered service lost reason code. One of
     *               {@link WifiAwareManager#WIFI_AWARE_DISCOVERY_LOST_REASON_PEER_NOT_VISIBLE},
     *               {@link WifiAwareManager#WIFI_AWARE_DISCOVERY_LOST_REASON_UNKNOWN
     */
    public void onServiceLost(@NonNull PeerHandle peerHandle,
            @WifiAwareManager.DiscoveryLostReasonCode int reason) {
        /* empty */
    }

    /**
     * Callback indicating that a pairing request is received from peer.
     *
     * @param peerHandle The peer's handle where the request is from
     * @param requestId The ID of the Aware pairing session
     */
    public void onPairingSetupRequestReceived(@NonNull PeerHandle peerHandle, int requestId) {

    }

    /**
     * Callback indicating that a pairing setup process succeeded.
     *
     * @param peerHandle The pairing peer handle.
     * @param alias      This is the paired device alias set by the caller.
     *                   {@link DiscoverySession#initiatePairingRequest(PeerHandle, String, int, String)}
     *                   or
     *                   {@link DiscoverySession#acceptPairingRequest(int, PeerHandle, String, int, String)}
     */
    public void onPairingSetupSucceeded(@NonNull PeerHandle peerHandle,
            @NonNull String alias) {

    }

    /**
     * Callback indicating that a pairing setup process failed.
     *
     * @param peerHandle The pairing peer handle.
     */
    public void onPairingSetupFailed(@NonNull PeerHandle peerHandle) {

    }

    /**
     * Callback indicating that a pairing verification process succeeded.
     *
     * @param peerHandle The pairing peer handle
     * @param alias      This is the paired device alias set by the caller.
     *                   {@link DiscoverySession#initiatePairingRequest(PeerHandle, String, int, String)}
     *                   or
     *                   {@link DiscoverySession#acceptPairingRequest(int, PeerHandle, String, int, String)}
     */
    public void onPairingVerificationSucceed(@NonNull PeerHandle peerHandle,
            @NonNull String alias){

    }

    /**
     * Callback indicating that a pairing verification process failed.
     *
     * @param peerHandle The pairing peer handle
     */
    public void onPairingVerificationFailed(@NonNull PeerHandle peerHandle) {

    }

    /**
     * Callback indicating that a Bootstrapping method negotiation succeeded.
     * The follow-up out-of-band bootstrapping can start
     *
     * @param peerHandle The bootstrapping peer handle
     * @param method     The bootstrapping method accept by the peer
     */
    public void onBootstrappingSucceeded(@NonNull PeerHandle peerHandle,
            @AwarePairingConfig.BootstrappingMethod int method){

    }

    /**
     * Callback indicating that a Bootstrapping method negotiation failed.
     *
     * @param peerHandle The bootstrapping peer handle
     */
    public void onBootstrappingFailed(@NonNull PeerHandle peerHandle) {

    }

    /**
     * Callback indicating that ranging results have been received.
     *
     * @param rangingResults List of range measurements.
     * @hide
     */
    @FlaggedApi(FLAG_RANGING_RTT_ENABLED)
    @SystemApi
    public void onRangingResultsReceived(@NonNull List<RangingResult> rangingResults) {

    }
}
