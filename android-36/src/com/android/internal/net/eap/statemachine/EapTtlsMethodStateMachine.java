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

package com.android.internal.net.eap.statemachine;

import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_TTLS;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.crypto.TlsSession.TLS_STATUS_CLOSED;
import static com.android.internal.net.eap.crypto.TlsSession.TLS_STATUS_FAILURE;
import static com.android.internal.net.eap.crypto.TlsSession.TLS_STATUS_SUCCESS;
import static com.android.internal.net.eap.crypto.TlsSession.TLS_STATUS_TUNNEL_ESTABLISHED;
import static com.android.internal.net.eap.message.EapData.EAP_IDENTITY;
import static com.android.internal.net.eap.message.EapData.EAP_NOTIFICATION;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_FAILURE;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_RESPONSE;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_SUCCESS;
import static com.android.internal.net.eap.message.ttls.EapTtlsInboundFragmentationHelper.FRAGMENTATION_STATUS_ACK;
import static com.android.internal.net.eap.message.ttls.EapTtlsInboundFragmentationHelper.FRAGMENTATION_STATUS_ASSEMBLED;
import static com.android.internal.net.eap.message.ttls.EapTtlsInboundFragmentationHelper.FRAGMENTATION_STATUS_INVALID;

import android.annotation.Nullable;
import android.content.Context;
import android.net.eap.EapSessionConfig.EapMethodConfig.EapMethod;
import android.net.eap.EapSessionConfig.EapTtlsConfig;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapFailure;
import com.android.internal.net.eap.EapResult.EapResponse;
import com.android.internal.net.eap.EapResult.EapSuccess;
import com.android.internal.net.eap.crypto.TlsSession;
import com.android.internal.net.eap.crypto.TlsSession.EapTtlsKeyingMaterial;
import com.android.internal.net.eap.crypto.TlsSession.TlsResult;
import com.android.internal.net.eap.crypto.TlsSessionFactory;
import com.android.internal.net.eap.exceptions.EapInvalidRequestException;
import com.android.internal.net.eap.exceptions.EapSilentException;
import com.android.internal.net.eap.exceptions.ttls.EapTtlsHandshakeException;
import com.android.internal.net.eap.exceptions.ttls.EapTtlsParsingException;
import com.android.internal.net.eap.message.EapData;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.eap.message.ttls.EapTtlsAvp;
import com.android.internal.net.eap.message.ttls.EapTtlsAvp.EapTtlsAvpDecoder;
import com.android.internal.net.eap.message.ttls.EapTtlsAvp.EapTtlsAvpDecoder.AvpDecodeResult;
import com.android.internal.net.eap.message.ttls.EapTtlsInboundFragmentationHelper;
import com.android.internal.net.eap.message.ttls.EapTtlsOutboundFragmentationHelper;
import com.android.internal.net.eap.message.ttls.EapTtlsOutboundFragmentationHelper.FragmentationResult;
import com.android.internal.net.eap.message.ttls.EapTtlsTypeData;
import com.android.internal.net.eap.message.ttls.EapTtlsTypeData.EapTtlsAcknowledgement;
import com.android.internal.net.eap.message.ttls.EapTtlsTypeData.EapTtlsTypeDataDecoder;
import com.android.internal.net.eap.message.ttls.EapTtlsTypeData.EapTtlsTypeDataDecoder.DecodeResult;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.net.ssl.SSLException;

/**
 * EapTtlsMethodStateMachine represents the valid paths possible for the EAP-TTLS protocol
 *
 * <p>EAP-TTLS sessions will always follow the path:
 *
 * <p>Created --+--> Handshake --+--> Tunnel (EAP) --+--> Final
 *
 * <p>Note: EAP-TTLS will only be allowed to run once. The inner EAP instance will not be able to
 * select EAP-TTLS. This is handled in the tunnel state when a new EAP session config is created.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5281">RFC 5281, Extensible Authentication Protocol
 *     Tunneled Transport Layer Security Authenticated Protocol Version 0 (EAP-TTLSv0)</a>
 */
public class EapTtlsMethodStateMachine extends EapMethodStateMachine {

    @VisibleForTesting public static TlsSessionFactory sTlsSessionFactory = new TlsSessionFactory();
    private static final int DEFAULT_AVP_VENDOR_ID = 0;

    private final Context mContext;
    private final EapTtlsConfig mEapTtlsConfig;
    private final EapTtlsTypeDataDecoder mTypeDataDecoder;
    private final SecureRandom mSecureRandom;

    @VisibleForTesting final EapTtlsInboundFragmentationHelper mInboundFragmentationHelper;
    @VisibleForTesting final EapTtlsOutboundFragmentationHelper mOutboundFragmentationHelper;
    @VisibleForTesting TlsSession mTlsSession;

    public EapTtlsMethodStateMachine(
            Context context,
            EapTtlsConfig eapTtlsConfig,
            SecureRandom secureRandom) {
        this(
                context,
                eapTtlsConfig,
                secureRandom,
                new EapTtlsTypeDataDecoder(),
                new EapTtlsInboundFragmentationHelper(),
                new EapTtlsOutboundFragmentationHelper());
    }

    @VisibleForTesting
    public EapTtlsMethodStateMachine(
            Context context,
            EapTtlsConfig eapTtlsConfig,
            SecureRandom secureRandom,
            EapTtlsTypeDataDecoder typeDataDecoder,
            EapTtlsInboundFragmentationHelper inboundFragmentationHelper,
            EapTtlsOutboundFragmentationHelper outboundFragmentationHelper) {
        mContext = context;
        mEapTtlsConfig = eapTtlsConfig;
        mTypeDataDecoder = typeDataDecoder;
        mSecureRandom = secureRandom;
        mInboundFragmentationHelper = inboundFragmentationHelper;
        mOutboundFragmentationHelper = outboundFragmentationHelper;

        transitionTo(new CreatedState());
    }

    @Override
    @EapMethod
    int getEapMethod() {
        return EAP_TYPE_TTLS;
    }

    @Override
    EapResult handleEapNotification(String tag, EapMessage message) {
        return EapStateMachine.handleNotification(tag, message);
    }

    /**
     * The created state verifies the start request before transitioning to phase 1 of EAP-TTLS
     * (RFC5281#7.1)
     */
    protected class CreatedState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        @Override
        public EapResult process(EapMessage message) {
            // TODO(b/160781895): Support decoding AVP's pre-tunnel in EAP-TTLS
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult decodeResult =
                    mTypeDataDecoder.decodeEapTtlsRequestPacket(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                LOG.e(mTAG, "Error parsing EAP-TTLS packet type data", decodeResult.eapError.cause);
                return decodeResult.eapError;
            } else if (!decodeResult.eapTypeData.isStart) {
                return new EapError(
                        new EapInvalidRequestException(
                                "Unexpected request received in EAP-TTLS: Received first request"
                                        + " without start bit set."));
            }

            return transitionAndProcess(new HandshakeState(), message);
        }
    }

    /**
     * The handshake (phase 1) state builds the tunnel for tunneled EAP authentication in phase 2
     *
     * <p>As per RFC5281#9.2.1, version negotiation occurs during the first exchange between client
     * and server. In other words, this is an implicit negotiation and is not handled independently.
     * In this case, the version will always be zero because that is the only currently supported
     * version of EAP-TTLS at the time of writing. The initiation of the handshake (RFC5281#7.1) is
     * the first response sent by the client.
     */
    protected class HandshakeState extends CloseableTtlsMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        private static final int DEFAULT_VENDOR_ID = 0;

        /**
         * Processes a message for the handshake state
         *
         * <ol>
         *   <li>Checks for EAP-success, EAP-failure, or EAP notification, returns early if one
         *       needs to be handled
         *   <li>Decodes type data, closes the connection if decoding fails
         *   <li>If outbound data is being fragmented, returns early with the next fragment to be
         *       sent
         *   <li>If inbound data is being reassembled, returns early with an ack etc. If nothing has
         *       returned yet, generates an EAP response for the incoming message
         *   <li>If this is a start request, and the first message in the handshake state, starts
         *       the handshake and returns an EAP-Response. Otherwise, processes the incoming
         *       message in TlsSession, and then sends an EAP-Response.
         *   <li>If the handshake is complete, sends a tunnelled EAP-Response/Identity and
         *       transitions to the tunnel state.
         * </ol>
         */
        @Override
        public EapResult process(EapMessage message) {
            EapResult eapResult = handleEapSuccessFailureNotification(mTAG, message);
            if (eapResult != null) {
                return eapResult;
            }

            DecodeResult decodeResult =
                    mTypeDataDecoder.decodeEapTtlsRequestPacket(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                LOG.e(mTAG, "Error parsing EAP-TTLS packet type data", decodeResult.eapError.cause);
                if (mTlsSession == null) {
                    return decodeResult.eapError;
                }
                return transitionToErroredAndAwaitingClosureState(
                        mTAG, message.eapIdentifier, decodeResult.eapError);
            }

            EapTtlsTypeData eapTtlsRequest = decodeResult.eapTypeData;

            // If the remote is in the midst of sending a fragmented message, ack the fragment and
            // return
            EapResult inboundFragmentAck =
                    handleInboundFragmentation(mTAG, eapTtlsRequest, message.eapIdentifier);
            if (inboundFragmentAck != null) {
                return inboundFragmentAck;
            }

            if (eapTtlsRequest.isStart) {
                if (mTlsSession != null) {
                    return transitionToErroredAndAwaitingClosureState(
                            mTAG,
                            message.eapIdentifier,
                            new EapError(
                                    new EapInvalidRequestException(
                                            "Received a start request when a session is already in"
                                                    + " progress")));
                }

                return startHandshake(message.eapIdentifier);
            }

            EapResult nextOutboundFragment =
                    getNextOutboundFragment(mTAG, eapTtlsRequest, message.eapIdentifier);
            if (nextOutboundFragment != null) {
                // Skip further processing, send remaining outbound fragments
                return nextOutboundFragment;
            }

            TlsResult tlsResult;

            try {
                tlsResult =
                        mTlsSession.processHandshakeData(
                                mInboundFragmentationHelper.getAssembledInboundFragment(),
                                buildEapIdentityResponseAvp(message.eapIdentifier));
            } catch (EapSilentException e) {
                LOG.e(mTAG, "Error building an identity response.", e);
                return transitionToErroredAndAwaitingClosureState(
                        mTAG, message.eapIdentifier, new EapError(e));
            }

            switch (tlsResult.status) {
                case TLS_STATUS_TUNNEL_ESTABLISHED:
                    LOG.d(mTAG, "Tunnel established. Generating a response.");
                    transitionTo(new TunnelState());
                    // fallthrough
                case TLS_STATUS_SUCCESS:
                    return buildEapMessageResponse(mTAG, message.eapIdentifier, tlsResult.data);
                case TLS_STATUS_CLOSED:
                    EapError eapError =
                            new EapError(
                                    new EapTtlsHandshakeException(
                                            "Handshake failed to complete and the"
                                                    + " connection was closed."));
                    // Because the TLS session is already closed, we only transition to
                    // ErroredAndAwaitingClosureState as the tls result has data to return from the
                    // closure
                    transitionTo(new ErroredAndAwaitingClosureState(eapError));
                    return buildEapMessageResponse(mTAG, message.eapIdentifier, tlsResult.data);
                case TLS_STATUS_FAILURE:
                    // Handshake failed and attempts to successfully close the tunnel also failed.
                    // Processing more messages is not possible due to the state of TlsSession so
                    // transition to FinalState.
                    transitionTo(new FinalState());
                    return new EapError(
                            new EapTtlsHandshakeException(
                                    "Handshake failed to complete and may not have been closed"
                                            + " properly."));
                default:
                    return transitionToErroredAndAwaitingClosureState(
                            mTAG,
                            message.eapIdentifier,
                            new EapError(
                                    new IllegalStateException(
                                            "Received an unknown TLS result with code "
                                                    + tlsResult.status)));
            }
        }

        /**
         * Initializes the TlsSession and starts a TLS handshake
         *
         * @param eapIdentifier the eap identifier for the response
         * @return an EAP response containing the ClientHello message, or an EAP error if the TLS
         *     handshake fails to begin
         */
        private EapResult startHandshake(int eapIdentifier) {
            try {
                mTlsSession =
                        sTlsSessionFactory.newInstance(
                                mEapTtlsConfig.getServerCaCert(), mSecureRandom);
            } catch (GeneralSecurityException | IOException e) {
                return new EapError(
                        new EapTtlsHandshakeException(
                                "There was an error creating the TLS Session.", e));
            }

            TlsResult tlsResult = mTlsSession.startHandshake();
            if (tlsResult.status == TLS_STATUS_FAILURE) {
                // Handshake failed and attempts to successfully close the tunnel also failed.
                // Processing more messages is not possible due to the state of TlsSession so
                // transition to FinalState.
                transitionTo(new FinalState());
                return new EapError(new EapTtlsHandshakeException("Failed to start handshake."));
            }

            return buildEapMessageResponse(mTAG, eapIdentifier, tlsResult.data);
        }

        /**
         * Builds an EAP-MESSAGE AVP containing an EAP-Identity response
         *
         * <p>Note that this uses the EAP-Identity in the session config nested within EapTtlsConfig
         * which may be different than the identity in the top-level EapSessionConfig
         *
         * @param eapIdentifier the eap identifier for the response
         * @throws EapSilentException if an error occurs creating the eap message
         */
        @VisibleForTesting
        byte[] buildEapIdentityResponseAvp(int eapIdentifier) throws EapSilentException {
            EapData eapData =
                    new EapData(
                            EAP_IDENTITY,
                            mEapTtlsConfig.getInnerEapSessionConfig().getEapIdentity());
            EapMessage eapMessage = new EapMessage(EAP_CODE_RESPONSE, eapIdentifier, eapData);
            return EapTtlsAvp.getEapMessageAvp(DEFAULT_AVP_VENDOR_ID, eapMessage.encode()).encode();
        }

        /**
         * Handles premature EAP-Success and EAP-Failure messages in the handshake state.
         *
         * <p>In the case of an EAP-Success or EAP-Failure, the TLS session will be closed but an
         * EapError or EAP-Failure will be returned. For an invalid type error, the TLS session will
         * be closed and the state will transition to AwaitingClosure.
         *
         * @param message the EapMessage to be checked for early Success/Failure/Notification
         *     messages
         * @return the EapResult generated from handling the give EapMessage, or null if the message
         *     Type matches that of the current EAP method
         */
        @Nullable
        @Override
        public EapResult handleEapSuccessFailure(EapMessage message) {
            if (message.eapCode == EAP_CODE_SUCCESS) {
                // EAP-SUCCESS is required to be the last EAP message sent during the EAP protocol,
                // so receiving a premature SUCCESS message is an unrecoverable error.
                mTlsSession.closeConnection();
                return new EapError(
                        new EapInvalidRequestException(
                                "Received an EAP-Success in the handshake state"));
            } else if (message.eapCode == EAP_CODE_FAILURE) {
                mTlsSession.closeConnection();
                transitionTo(new FinalState());
                return new EapFailure();
            }

            return null;
        }
    }

    /**
     * The tunnel state (phase 2) tunnels data produced by an inner EAP instance
     *
     * <p>The tunnel state creates an inner EAP instance via a new EAP state machine and handles
     * decryption and encryption of data using the previously established TLS tunnel (RFC5281#7.2)
     */
    protected class TunnelState extends CloseableTtlsMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        @VisibleForTesting EapStateMachine mInnerEapStateMachine;
        @VisibleForTesting EapTtlsAvpDecoder mEapTtlsAvpDecoder = new EapTtlsAvpDecoder();

        public TunnelState() {
            mInnerEapStateMachine =
                    new EapStateMachine(
                            mContext, mEapTtlsConfig.getInnerEapSessionConfig(), mSecureRandom);
        }

        /**
         * Processes a message for the inner tunneled authentication method.
         *
         * <ol>
         *   <li>Checks for EAP-success, EAP-failure, or EAP notification, returns early if one
         *       needs to be handled
         *   <li>Decodes type data, closes the connection if decoding fails
         *   <li>If outbound data is being fragmented, returns early with the next fragment to be
         *       sent
         *   <li>If inbound data is being reassembled, returns early with an ack etc. If nothing has
         *       returned yet, generates an EAP response for the incoming message
         *   <li>Decodes AVP, closes the connection if decoding fails.
         *   <li>Processes data through inner state machine. Encodes response in AVP, encrypts it
         *       and sends EAP-Response.
         * </ol>
         */
        @Override
        public EapResult process(EapMessage message) {
            EapResult eapResult = handleEapSuccessFailureNotification(mTAG, message);
            if (eapResult != null) {
                return eapResult;
            }

            DecodeResult decodeResult =
                    mTypeDataDecoder.decodeEapTtlsRequestPacket(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                LOG.e(mTAG, "Error parsing EAP-TTLS packet type data", decodeResult.eapError.cause);
                return transitionToErroredAndAwaitingClosureState(
                        mTAG, message.eapIdentifier, decodeResult.eapError);
            }

            EapTtlsTypeData eapTtlsRequest = decodeResult.eapTypeData;

            EapResult nextOutboundFragment =
                    getNextOutboundFragment(mTAG, eapTtlsRequest, message.eapIdentifier);
            if (nextOutboundFragment != null) {
                return nextOutboundFragment;
            }

            EapResult inboundFragmentAck =
                    handleInboundFragmentation(mTAG, eapTtlsRequest, message.eapIdentifier);
            if (inboundFragmentAck != null) {
                return inboundFragmentAck;
            }

            TlsResult decryptResult =
                    mTlsSession.processIncomingData(
                            mInboundFragmentationHelper.getAssembledInboundFragment());

            EapResult errorResult = handleTunnelTlsResult(decryptResult, message.eapIdentifier);
            if (errorResult != null) {
                return errorResult;
            }

            AvpDecodeResult avpDecodeResult = mEapTtlsAvpDecoder.decode(decryptResult.data);
            if (!avpDecodeResult.isSuccessfulDecode()) {
                LOG.e(mTAG, "Error parsing EAP-TTLS AVP", avpDecodeResult.eapError.cause);
                return transitionToErroredAndAwaitingClosureState(
                        mTAG, message.eapIdentifier, avpDecodeResult.eapError);
            }

            EapTtlsAvp avp = avpDecodeResult.eapTtlsAvp;
            LOG.d(
                    mTAG,
                    "Incoming AVP has been decrypted and processed. AVP data will be passed to the"
                            + " inner state machine.");

            EapResult innerResult = mInnerEapStateMachine.process(avp.data);

            if (innerResult instanceof EapError) {
                return transitionToErroredAndAwaitingClosureState(
                        mTAG, message.eapIdentifier, (EapError) innerResult);
            } else if (innerResult instanceof EapFailure) {
                LOG.e(mTAG, "Tunneled authentication failed");
                mTlsSession.closeConnection();
                transitionTo(new FinalState());
                return innerResult;
            } else if (innerResult instanceof EapSuccess) {
                Exception invalidSuccess =
                        new EapInvalidRequestException(
                                "Received an unexpected EapSuccess from the inner state machine.");
                transitionToErroredAndAwaitingClosureState(
                        mTAG, message.eapIdentifier, new EapError(invalidSuccess));
            }

            LOG.d(mTAG, "Received EapResponse from innerStateMachine");
            TlsResult encryptResult;

            EapResponse innerResponse = (EapResponse) innerResult;
            EapTtlsAvp outgoingAvp =
                    EapTtlsAvp.getEapMessageAvp(DEFAULT_AVP_VENDOR_ID, innerResponse.packet);
            encryptResult = mTlsSession.processOutgoingData(outgoingAvp.encode());

            errorResult = handleTunnelTlsResult(encryptResult, message.eapIdentifier);
            if (errorResult != null) {
                return errorResult;
            }

            LOG.d(mTAG, "Outbound AVP has been assembled and encrypted. Building EAP Response.");

            return buildEapMessageResponse(mTAG, message.eapIdentifier, encryptResult.data);
        }

        /**
         * Validates the results of an encryption or decryption operation
         *
         * <p>If the result is an error state, the tunnel will be closed and a response or EapError
         * will be returned. Otherwise, null is returned to indicate that processing can continue.
         *
         * @param result a TlsResult encapsulating the results of an encrypt or decrypt operation
         * @param eapIdentifier the eap identifier from the latest message
         * @return an eap response if an error occurs or null if processing can continue
         */
        @Nullable
        EapResult handleTunnelTlsResult(TlsResult result, int eapIdentifier) {
            switch (result.status) {
                case TLS_STATUS_SUCCESS:
                    return null;
                case TLS_STATUS_CLOSED:
                    Exception closeException =
                            new SSLException(
                                    "TLS Session failed to encrypt or decrypt data"
                                            + " and was closed.");
                    // Because the TLS session is already closed, we only transition to
                    // ErroredAndAwaitingClosureState as the tls result has data to return from the
                    // closure
                    transitionTo(new ErroredAndAwaitingClosureState(new EapError(closeException)));
                    return buildEapMessageResponse(mTAG, eapIdentifier, result.data);
                case TLS_STATUS_FAILURE:
                    transitionTo(new FinalState());
                    return new EapError(
                            new SSLException(
                                    "Failed to encrypt or decrypt message. Tunnel could not be"
                                            + " closed properly"));
                default:
                    Exception illegalStateException =
                            new IllegalStateException(
                                    "Received an unexpected TLS result with code " + result.status);
                    return transitionToErroredAndAwaitingClosureState(
                            mTAG, eapIdentifier, new EapError(illegalStateException));
            }
        }

        /**
         * Handles EAP-Success and EAP-Failure messages in the tunnel state
         *
         * <p>Both success/failure messages are passed into the inner state machine for processing.
         *
         * <p>If an EAP-Success is returned by the inner state machine, it is discarded and a new
         * EAP-Success that contains the keying material generated during the TLS negotiation is
         * sent instead.
         *
         * @param message the EapMessage to be checked for Success/Failure
         * @return the EapResult generated from handling the give EapMessage, or null if the message
         *     Type matches that of the current EAP method
         */
        @Nullable
        @Override
        EapResult handleEapSuccessFailure(EapMessage message) {
            if (message.eapCode == EAP_CODE_SUCCESS || message.eapCode == EAP_CODE_FAILURE) {
                EapResult innerResult = mInnerEapStateMachine.process(message.encode());
                if (innerResult instanceof EapSuccess) {
                    EapTtlsKeyingMaterial keyingMaterial = mTlsSession.generateKeyingMaterial();
                    mTlsSession.closeConnection();
                    transitionTo(new FinalState());

                    if (!keyingMaterial.isSuccessful()) {
                        return keyingMaterial.eapError;
                    }

                    return new EapSuccess(keyingMaterial.msk, keyingMaterial.emsk);
                }

                transitionTo(new FinalState());
                mTlsSession.closeConnection();
                return innerResult;
            }

            return null;
        }
    }

    /**
     * The closure state handles closure of the TLS session in EAP-TTLS
     *
     * <p>Note that this state is only entered following an error. If EAP authentication completes
     * successfully or fails, the tunnel is assumed to have implicitly closed.
     */
    protected class ErroredAndAwaitingClosureState extends EapMethodState {
        private final String mTAG = this.getClass().getSimpleName();

        private final EapError mEapError;

        /**
         * Initializes the closure state
         *
         * <p>The errored and awaiting closure state is an error state. If a server responds to a
         * close-notify, the data is processed and the EAP error which encapsulates the initial
         * error that caused the closure is returned
         *
         * @param eapError an EAP error that contains the error that initially caused a close to
         *     occur
         */
        public ErroredAndAwaitingClosureState(EapError eapError) {
            mEapError = eapError;
        }

        @Override
        public EapResult process(EapMessage message) {
            EapResult result = handleEapSuccessFailureNotification(mTAG, message);
            if (result != null) {
                return result;
            }

            DecodeResult decodeResult =
                    mTypeDataDecoder.decodeEapTtlsRequestPacket(message.eapData.eapTypeData);
            if (!decodeResult.isSuccessfulDecode()) {
                LOG.e(mTAG, "Error parsing EAP-TTLS packet type data", decodeResult.eapError.cause);
                return decodeResult.eapError;
            }

            // if the server sent data, we process it and return an EapError.
            // A response is not required and is additionally unlikely as we have already sent the
            // closure-notify
            mTlsSession.processIncomingData(decodeResult.eapTypeData.data);

            return mEapError;
        }
    }

    /**
     * Transitions to the ErroredAndAwaitingClosureState and attempts to close the TLS tunnel
     *
     * @param tag the tag of the calling class
     * @param eapIdentifier the EAP identifier from the most recent EAP request
     * @param eapError the EAP error to return if closure fails
     * @return a closure notify TLS message or an EAP error if one cannot be generated
     */
    @VisibleForTesting
    EapResult transitionToErroredAndAwaitingClosureState(
            String tag, int eapIdentifier, EapError eapError) {
        TlsResult closureResult = mTlsSession.closeConnection();
        if (closureResult.status != TLS_STATUS_CLOSED) {
            LOG.e(tag, "Failed to close the TLS session");
            transitionTo(new FinalState());
            return eapError;
        }

        transitionTo(new ErroredAndAwaitingClosureState(eapError));
        return buildEapMessageResponse(
                tag,
                eapIdentifier,
                EapTtlsTypeData.getEapTtlsTypeData(
                        false /* isFragmented */,
                        false /* start */,
                        0 /* version 0 */,
                        closureResult.data.length,
                        closureResult.data));
    }

    /**
     * Verifies whether outbound fragmentation is in progress and constructs the next fragment if
     * necessary
     *
     * @param tag the tag for the calling class
     * @param eapTtlsRequest the request received from the server
     * @param eapIdentifier the eap identifier from the latest message
     * @return an eap response if the next fragment exists, or null if no fragmentation is in
     *     progress
     */
    @Nullable
    private EapResult getNextOutboundFragment(
            String tag, EapTtlsTypeData eapTtlsRequest, int eapIdentifier) {
        if (eapTtlsRequest.isAcknowledgmentPacket()) {
            if (mOutboundFragmentationHelper.hasRemainingFragments()) {
                FragmentationResult result = mOutboundFragmentationHelper.getNextOutboundFragment();
                return buildEapMessageResponse(
                        tag,
                        eapIdentifier,
                        EapTtlsTypeData.getEapTtlsTypeData(
                                result.hasRemainingFragments,
                                false /* start */,
                                0 /* version 0 */,
                                0 /* messageLength */,
                                result.fragmentedData));
            } else {
                return transitionToErroredAndAwaitingClosureState(
                        tag,
                        eapIdentifier,
                        new EapError(
                                new EapInvalidRequestException(
                                        "Received an ack but no packet was in the process of"
                                                + " being fragmented.")));
            }
        } else if (mOutboundFragmentationHelper.hasRemainingFragments()) {
            return transitionToErroredAndAwaitingClosureState(
                    tag,
                    eapIdentifier,
                    new EapError(
                            new EapInvalidRequestException(
                                    "Received a standard EAP-Request but was expecting an ack to a"
                                            + " fragment.")));
        }

        return null;
    }

    /**
     * Processes incoming data, and if necessary, assembles fragments
     *
     * @param tag the tag for the calling class
     * @param eapTtlsRequest the request received from the server
     * @param eapIdentifier the eap identifier from the latest message
     * @return an acknowledgment if the received data is a fragment, null if data is ready to
     *     process
     */
    @Nullable
    private EapResult handleInboundFragmentation(
            String tag, EapTtlsTypeData eapTtlsRequest, int eapIdentifier) {
        int fragmentationStatus =
                mInboundFragmentationHelper.assembleInboundMessage(eapTtlsRequest);

        switch (fragmentationStatus) {
            case FRAGMENTATION_STATUS_ASSEMBLED:
                return null;
            case FRAGMENTATION_STATUS_ACK:
                LOG.d(tag, "Packet is fragmented. Generating an acknowledgement response.");
                return buildEapMessageResponse(
                        tag, eapIdentifier, EapTtlsAcknowledgement.getEapTtlsAcknowledgement());
            case FRAGMENTATION_STATUS_INVALID:
                return transitionToErroredAndAwaitingClosureState(
                        tag,
                        eapIdentifier,
                        new EapError(
                                new EapTtlsParsingException(
                                        "Fragmentation failure: There was an error decoding the"
                                                + " fragmented request.")));
            default:
                return transitionToErroredAndAwaitingClosureState(
                        tag,
                        eapIdentifier,
                        new EapError(
                                new IllegalStateException(
                                        "Received an unknown fragmentation status when assembling"
                                                + " an inbound fragment: "
                                                + fragmentationStatus)));
        }
    }

    /**
     * Takes outbound data and assembles an EAP-Response.
     *
     * <p>The data will be fragmented if necessary
     *
     * @param tag the tag of the calling class
     * @param eapIdentifier the EAP identifier from the most recent EAP request
     * @param data the data used to build the EAP-TTLS type data
     * @return an EAP result that is either an EAP response or an EAP error
     */
    private EapResult buildEapMessageResponse(String tag, int eapIdentifier, byte[] data) {
        // TODO(b/165668196): Modify outbound fragmentation helper to be per-message in EAP-TTLS
        mOutboundFragmentationHelper.setupOutboundFragmentation(data);
        FragmentationResult result = mOutboundFragmentationHelper.getNextOutboundFragment();

        // As per RFC5281#9.2.2, an unfragmented packet may have the length bit set
        return buildEapMessageResponse(
                tag,
                eapIdentifier,
                EapTtlsTypeData.getEapTtlsTypeData(
                        result.hasRemainingFragments,
                        false /* start */,
                        0 /* version 0 */,
                        data.length,
                        result.fragmentedData));
    }

    /**
     * Takes an already constructed EapTtlsTypeData and builds an EAP-Response
     *
     * @param tag the tag of the calling class
     * @param eapIdentifier the EAP identifier from the most recent EAP request
     * @param eapTtlsTypeData the type data to use in the EAP Response
     * @return an EAP result that is either an EAP response or an EAP error
     */
    private EapResult buildEapMessageResponse(
            String tag, int eapIdentifier, EapTtlsTypeData eapTtlsTypeData) {
        try {
            EapData eapData = new EapData(getEapMethod(), eapTtlsTypeData.encode());
            EapMessage eapMessage = new EapMessage(EAP_CODE_RESPONSE, eapIdentifier, eapData);
            return EapResponse.getEapResponse(eapMessage);
        } catch (EapSilentException ex) {
            LOG.e(tag, "Error building response EapMessage", ex);
            return new EapError(ex);
        }
    }

    /**
     * CloseableTtlsMethodState defines specific behaviour for handling EAP-Messages in EAP-TTLS
     *
     * <p>EAP-TTLS requires specific handling compared to what is defined in {@link EapMethodState}
     * as the tunnel needs to be closed. Furthermore, EAP-Success/EAP-Failure handling differs in
     * the tunnel state as it needs to be processed by the inner authentication method.
     *
     * <p>
     */
    abstract class CloseableTtlsMethodState extends EapMethodState {
        abstract EapResult handleEapSuccessFailure(EapMessage message);

        @Override
        @Nullable
        EapResult handleEapSuccessFailureNotification(String tag, EapMessage message) {
            EapResult eapResult = handleEapSuccessFailure(message);
            if (eapResult != null) {
                return eapResult;
            }

            if (message.eapData.eapType == EAP_NOTIFICATION) {
                return handleEapNotification(tag, message);
            } else if (message.eapData.eapType != EAP_TYPE_TTLS) {
                EapError eapError =
                        new EapError(
                                new EapInvalidRequestException(
                                        "Expected EAP Type "
                                                + getEapMethod()
                                                + ", received "
                                                + message.eapData.eapType));
                return transitionToErroredAndAwaitingClosureState(
                        tag, message.eapIdentifier, eapError);
            }

            return null;
        }
    }
}

