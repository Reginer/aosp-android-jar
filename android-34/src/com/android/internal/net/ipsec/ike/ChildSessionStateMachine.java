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
package com.android.internal.net.ipsec.ike;

import static android.net.ipsec.ike.IkeManager.getIkeLog;
import static android.net.ipsec.ike.SaProposal.DH_GROUP_NONE;
import static android.net.ipsec.ike.exceptions.IkeException.wrapAsIkeException;
import static android.net.ipsec.ike.exceptions.IkeProtocolException.ERROR_TYPE_TEMPORARY_FAILURE;

import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.BUNDLE_KEY_CHILD_REMOTE_SPI;
import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.CMD_ALARM_FIRED;
import static com.android.internal.net.ipsec.ike.message.IkeHeader.EXCHANGE_TYPE_CREATE_CHILD_SA;
import static com.android.internal.net.ipsec.ike.message.IkeHeader.EXCHANGE_TYPE_IKE_AUTH;
import static com.android.internal.net.ipsec.ike.message.IkeHeader.EXCHANGE_TYPE_INFORMATIONAL;
import static com.android.internal.net.ipsec.ike.message.IkeHeader.ExchangeType;
import static com.android.internal.net.ipsec.ike.message.IkeMessage.IKE_EXCHANGE_SUBTYPE_DELETE_CHILD;
import static com.android.internal.net.ipsec.ike.message.IkeMessage.IKE_EXCHANGE_SUBTYPE_REKEY_CHILD;
import static com.android.internal.net.ipsec.ike.message.IkeNotifyPayload.NOTIFY_TYPE_REKEY_SA;
import static com.android.internal.net.ipsec.ike.message.IkeNotifyPayload.NOTIFY_TYPE_USE_TRANSPORT_MODE;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_CP;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_DELETE;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_KE;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_NONCE;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_NOTIFY;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_SA;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_TS_INITIATOR;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PAYLOAD_TYPE_TS_RESPONDER;
import static com.android.internal.net.ipsec.ike.message.IkePayload.PROTOCOL_ID_ESP;
import static com.android.internal.net.ipsec.ike.utils.IkeAlarm.IkeAlarmConfig;
import static com.android.internal.net.ipsec.ike.utils.IkeAlarm.buildIkeAlarmIntent;
import static com.android.internal.net.ipsec.ike.utils.IkeAlarmReceiver.ACTION_DELETE_CHILD;
import static com.android.internal.net.ipsec.ike.utils.IkeAlarmReceiver.ACTION_REKEY_CHILD;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.app.PendingIntent;
import android.net.IpSecManager;
import android.net.IpSecManager.ResourceUnavailableException;
import android.net.IpSecManager.SecurityParameterIndex;
import android.net.IpSecManager.SpiUnavailableException;
import android.net.IpSecManager.UdpEncapsulationSocket;
import android.net.IpSecTransform;
import android.net.ipsec.ike.ChildSaProposal;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.ChildSessionConfiguration;
import android.net.ipsec.ike.ChildSessionParams;
import android.net.ipsec.ike.IkeTrafficSelector;
import android.net.ipsec.ike.SaProposal;
import android.net.ipsec.ike.TunnelModeChildSessionParams;
import android.net.ipsec.ike.exceptions.IkeException;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidKeException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.net.ipsec.ike.exceptions.NoValidProposalChosenException;
import android.net.ipsec.ike.exceptions.TemporaryFailureException;
import android.net.ipsec.ike.exceptions.TsUnacceptableException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.ipsec.ike.IkeLocalRequestScheduler.ChildLocalRequest;
import com.android.internal.net.ipsec.ike.IkeLocalRequestScheduler.LocalRequestFactory;
import com.android.internal.net.ipsec.ike.SaRecord.ChildSaRecord;
import com.android.internal.net.ipsec.ike.SaRecord.SaLifetimeAlarmScheduler;
import com.android.internal.net.ipsec.ike.crypto.IkeCipher;
import com.android.internal.net.ipsec.ike.crypto.IkeMacIntegrity;
import com.android.internal.net.ipsec.ike.crypto.IkeMacPrf;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttribute;
import com.android.internal.net.ipsec.ike.message.IkeDeletePayload;
import com.android.internal.net.ipsec.ike.message.IkeKePayload;
import com.android.internal.net.ipsec.ike.message.IkeMessage.IkeExchangeSubType;
import com.android.internal.net.ipsec.ike.message.IkeNoncePayload;
import com.android.internal.net.ipsec.ike.message.IkeNotifyPayload;
import com.android.internal.net.ipsec.ike.message.IkeNotifyPayload.NotifyType;
import com.android.internal.net.ipsec.ike.message.IkePayload;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.ChildProposal;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.DhGroupTransform;
import com.android.internal.net.ipsec.ike.message.IkeTsPayload;
import com.android.internal.net.ipsec.ike.shim.ShimUtils;
import com.android.internal.net.ipsec.ike.utils.IpSecSpiGenerator;
import com.android.internal.net.ipsec.ike.utils.RandomnessFactory;
import com.android.internal.util.State;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * ChildSessionStateMachine tracks states and manages exchanges of this Child Session.
 *
 * <p>ChildSessionStateMachine has two types of states. One type are states where there is no
 * ongoing procedure affecting Child Session (non-procedure state), including Initial, Idle and
 * Receiving. All other states are "procedure" states which are named as follows:
 *
 * <pre>
 * State Name = [Procedure Type] + [Exchange Initiator] + [Exchange Type].
 * - An IKE procedure consists of one or two IKE exchanges:
 *      Procedure Type = {CreateChild | DeleteChild | Info | RekeyChild | SimulRekeyChild}.
 * - Exchange Initiator indicates whether local or remote peer is the exchange initiator:
 *      Exchange Initiator = {Local | Remote}
 * - Exchange type defines the function of this exchange.
 *      Exchange Type = {Create | Delete}
 * </pre>
 */
public class ChildSessionStateMachine extends AbstractSessionStateMachine {
    private static final String TAG = "ChildSessionStateMachine";

    private static final int SPI_NOT_REGISTERED = 0;

    private static final int CMD_GENERAL_BASE = CMD_PRIVATE_BASE;

    /** Receive request for negotiating first Child SA. */
    private static final int CMD_HANDLE_FIRST_CHILD_EXCHANGE = CMD_GENERAL_BASE + 1;
    /** Receive a request from the remote. */
    private static final int CMD_HANDLE_RECEIVED_REQUEST = CMD_GENERAL_BASE + 2;
    /** Receive a reponse from the remote. */
    private static final int CMD_HANDLE_RECEIVED_RESPONSE = CMD_GENERAL_BASE + 3;

    private static final SparseArray<String> CMD_TO_STR;

    static {
        CMD_TO_STR = new SparseArray<>();
        CMD_TO_STR.put(CMD_HANDLE_FIRST_CHILD_EXCHANGE, "Handle First Child");
        CMD_TO_STR.put(CMD_HANDLE_RECEIVED_REQUEST, "Rcv request");
        CMD_TO_STR.put(CMD_HANDLE_RECEIVED_RESPONSE, "Rcv response");
    }

    private final IkeContext mIkeContext;
    private final int mIkeSessionId;
    private final Handler mIkeHandler;
    private final IpSecManager mIpSecManager;

    /**
     * mIpSecSpiGenerator will be used by all Child SA creations in this Child Session to avoid SPI
     * collision in test mode.
     */
    private final IpSecSpiGenerator mIpSecSpiGenerator;

    private final LocalRequestFactory mLocalRequestFactory = new LocalRequestFactory();

    /** User provided configurations. */
    @VisibleForTesting final ChildSessionParams mChildSessionParams;

    private final ChildSessionCallback mUserCallback;

    /** Callback to notify IKE Session the state changes. */
    private final IChildSessionSmCallback mChildSmCallback;

    // TODO: Also store ChildSessionCallback for notifying users.

    /** Local address assigned on device. */
    @VisibleForTesting InetAddress mLocalAddress;
    /** Remote address configured by users. */
    @VisibleForTesting InetAddress mRemoteAddress;

    /**
     * UDP-Encapsulated socket that allows IPsec traffic to pass through a NAT. Null if UDP
     * encapsulation is not needed.
     */
    @VisibleForTesting @Nullable UdpEncapsulationSocket mUdpEncapSocket;

    /** Crypto parameters. Updated upon initial negotiation or IKE SA rekey. */
    @VisibleForTesting IkeMacPrf mIkePrf;

    @VisibleForTesting byte[] mSkD;

    /**
     * Negotiated IKE DH group
     *
     * <p>First Child SA, and all additional Child SAs that do not have user specified DH group are
     * set up with crypto keys that are implicitly generated by the negotiated IKE DH group. For
     * those Child SAs, incoming rekey requests that match the negotiated IKE DH group should also
     * be acceptable. This for improving the interoperability with other IKE implementations.
     */
    @VisibleForTesting int mIkeDhGroup;

    /** Package private ChildSaProposal that represents the negotiated Child SA proposal. */
    @VisibleForTesting ChildSaProposal mSaProposal;

    /** Negotiated local Traffic Selector. */
    @VisibleForTesting IkeTrafficSelector[] mLocalTs;
    /** Negotiated remote Traffic Selector. */
    @VisibleForTesting IkeTrafficSelector[] mRemoteTs;

    @VisibleForTesting IkeCipher mChildCipher;
    @VisibleForTesting IkeMacIntegrity mChildIntegrity;

    /** Package private */
    @VisibleForTesting ChildSaRecord mCurrentChildSaRecord;
    /** Package private */
    @VisibleForTesting ChildSaRecord mLocalInitNewChildSaRecord;
    /** Package private */
    @VisibleForTesting ChildSaRecord mRemoteInitNewChildSaRecord;

    /** Package private */
    @VisibleForTesting ChildSaRecord mChildSaRecordSurviving;

    @VisibleForTesting final State mKillChildSessionParent = new KillChildSessionParent();

    @VisibleForTesting final State mInitial = new Initial();
    @VisibleForTesting final State mCreateChildLocalCreate = new CreateChildLocalCreate();
    @VisibleForTesting final State mIdle = new Idle();
    @VisibleForTesting final State mIdleWithDeferredRequest = new IdleWithDeferredRequest();
    @VisibleForTesting final State mClosedAndAwaitResponse = new ClosedAndAwaitResponse();
    @VisibleForTesting final State mDeleteChildLocalDelete = new DeleteChildLocalDelete();
    @VisibleForTesting final State mDeleteChildRemoteDelete = new DeleteChildRemoteDelete();
    @VisibleForTesting final State mRekeyChildLocalCreate = new RekeyChildLocalCreate();
    @VisibleForTesting final State mMobikeRekeyChildLocalCreate = new MobikeRekeyChildLocalCreate();
    @VisibleForTesting final State mRekeyChildRemoteCreate = new RekeyChildRemoteCreate();
    @VisibleForTesting final State mRekeyChildLocalDelete = new RekeyChildLocalDelete();
    @VisibleForTesting final State mRekeyChildRemoteDelete = new RekeyChildRemoteDelete();
    @VisibleForTesting boolean mIsFirstChild;

    /**
     * Builds a new uninitialized ChildSessionStateMachine
     *
     * <p>Upon creation, this state machine will await either the handleFirstChildExchange
     * (IKE_AUTH), or the createChildSession (Additional child creation beyond the first child) to
     * be called, both of which must pass keying and SA information.
     *
     * <p>This two-stage initialization is required to allow race-free user interaction with the IKE
     * Session keyed on the child state machine callbacks.
     *
     * <p>Package private
     */
    ChildSessionStateMachine(
            IkeContext ikeContext,
            ChildSessionStateMachine.Config childSmConfig,
            ChildSessionCallback userCallback,
            IChildSessionSmCallback childSmCallback) {
        super(TAG, ikeContext.getLooper(), childSmConfig.userCbExecutor);

        mIkeContext = ikeContext;
        mIkeSessionId = childSmConfig.ikeSessionId;
        mIkeHandler = childSmConfig.ikeHandler;
        mIpSecManager = childSmConfig.ipSecManager;
        mIpSecSpiGenerator = childSmConfig.ipSecSpiGenerator;

        mChildSessionParams = childSmConfig.sessionParams;
        mUserCallback = userCallback;
        mChildSmCallback = childSmCallback;

        addState(mKillChildSessionParent);

        addState(mInitial, mKillChildSessionParent);
        addState(mCreateChildLocalCreate, mKillChildSessionParent);
        addState(mIdle, mKillChildSessionParent);
        addState(mIdleWithDeferredRequest, mKillChildSessionParent);
        addState(mClosedAndAwaitResponse, mKillChildSessionParent);
        addState(mDeleteChildLocalDelete, mKillChildSessionParent);
        addState(mDeleteChildRemoteDelete, mKillChildSessionParent);
        addState(mRekeyChildLocalCreate, mKillChildSessionParent);
        addState(mMobikeRekeyChildLocalCreate, mKillChildSessionParent);
        addState(mRekeyChildRemoteCreate, mKillChildSessionParent);
        addState(mRekeyChildLocalDelete, mKillChildSessionParent);
        addState(mRekeyChildRemoteDelete, mKillChildSessionParent);

        setInitialState(mInitial);
    }

    // Configurations provided by an IKE Session for building the Child Session
    static class Config {
        public final int ikeSessionId;
        public final Handler ikeHandler;
        public final ChildSessionParams sessionParams;
        public final IpSecManager ipSecManager;
        public final IpSecSpiGenerator ipSecSpiGenerator;
        public final Executor userCbExecutor;

        Config(
                int ikeSessionId,
                Handler ikeHandler,
                ChildSessionParams sessionParams,
                IpSecManager ipSecManager,
                IpSecSpiGenerator ipSecSpiGenerator,
                Executor userCbExecutor) {
            this.ikeSessionId = ikeSessionId;
            this.ikeHandler = ikeHandler;
            this.sessionParams = sessionParams;
            this.ipSecManager = ipSecManager;
            this.ipSecSpiGenerator = ipSecSpiGenerator;
            this.userCbExecutor = userCbExecutor;
        }
    }

    /**
     * Interface for ChildSessionStateMachine to notify IkeSessionStateMachine of state changes.
     */
    interface IChildSessionSmCallback {
        /** Notify that new Child SA is created. */
        void onChildSaCreated(int remoteSpi, ChildSessionStateMachine childSession);

        /** Notify that a Child SA is deleted. */
        void onChildSaDeleted(int remoteSpi);

        /** Schedule retry for a Create Child Request on the LocalRequestScheduler. */
        void scheduleRetryLocalRequest(ChildLocalRequest futureRequest);

        /** Notify the IKE Session to send out IKE message for this Child Session. */
        void onOutboundPayloadsReady(
                @ExchangeType int exchangeType,
                boolean isResp,
                List<IkePayload> payloadList,
                ChildSessionStateMachine childSession);

        /** Notify that a Child procedure has been finished. */
        void onProcedureFinished(ChildSessionStateMachine childSession);

        /**
         * Notify the IKE Session State Machine that this Child has been fully shut down.
         *
         * <p>This method MUST be called after the user callbacks have been fired, and MUST always
         * be called before the state machine can shut down.
         */
        void onChildSessionClosed(ChildSessionCallback userCallbacks);

        /**
         * Notify that a Child procedure has been finished and the IKE Session should close itself
         * because of a fatal error.
         *
         * <p>Child Session may encounter an IKE Session fatal error in three cases with different
         * handling rules:
         *
         * <pre>
         * - When there is a fatal error in an inbound request, onOutboundPayloadsReady will be
         *   called first to send out an error notification, and then onFatalIkeSessionError() will
         *   be called to locally close the IKE Session.
         * - When there is a fatal error in an inbound response, onOutboundPayloadsReady will be
         *   called first to send out an IKE Delete message, and then onFatalIkeSessionError() will
         *   be called to locally close the IKE Session.
         * - When there is a fatal error notification in an inbound response,
         *   onFatalIkeSessionError() will be called to locally close the IKE Session"
         * </pre>
         */
        void onFatalIkeSessionError(Exception exception);
    }

    /**
     * Receive requesting and responding payloads for negotiating first Child SA.
     *
     * <p>This method is called synchronously from IkeStateMachine. It proxies the synchronous call
     * as an asynchronous job to the ChildStateMachine handler.
     *
     * @param reqPayloads SA negotiation related payloads in IKE_AUTH request.
     * @param respPayloads SA negotiation related payloads in IKE_AUTH response.
     * @param localAddress The local (outer) address of the Child Session.
     * @param remoteAddress The remote (outer) address of the Child Session.
     * @param udpEncapSocket The socket to use for UDP encapsulation, or NULL if no encap needed.
     * @param ikePrf The pseudo-random function to use for key derivation
     * @param ikeDh The negotiated IKE DH group
     * @param skD The key for which to derive new keying information from.
     */
    public void handleFirstChildExchange(
            List<IkePayload> reqPayloads,
            List<IkePayload> respPayloads,
            InetAddress localAddress,
            InetAddress remoteAddress,
            UdpEncapsulationSocket udpEncapSocket,
            IkeMacPrf ikePrf,
            int ikeDh,
            byte[] skD) {

        this.mLocalAddress = localAddress;
        this.mRemoteAddress = remoteAddress;
        this.mUdpEncapSocket = udpEncapSocket;
        this.mIkePrf = ikePrf;
        this.mIkeDhGroup = ikeDh;
        this.mSkD = skD;
        mIsFirstChild = true;

        int spi = registerProvisionalChildAndGetSpi(respPayloads);
        sendMessage(
                CMD_HANDLE_FIRST_CHILD_EXCHANGE,
                new FirstChildNegotiationData(reqPayloads, respPayloads, spi));
    }

    /**
     * Initiate Create Child procedure.
     *
     * <p>This method is called synchronously from IkeStateMachine. It proxies the synchronous call
     * as an asynchronous job to the ChildStateMachine handler.
     *
     * @param localAddress The local (outer) address from which traffic will originate.
     * @param remoteAddress The remote (outer) address to which traffic will be sent.
     * @param udpEncapSocket The socket to use for UDP encapsulation, or NULL if no encap needed.
     * @param ikePrf The pseudo-random function to use for key derivation
     * @param ikeDh The negotiated IKE DH group
     * @param skD The key for which to derive new keying information from.
     */
    public void createChildSession(
            InetAddress localAddress,
            InetAddress remoteAddress,
            UdpEncapsulationSocket udpEncapSocket,
            IkeMacPrf ikePrf,
            int ikeDh,
            byte[] skD) {
        this.mLocalAddress = localAddress;
        this.mRemoteAddress = remoteAddress;
        this.mUdpEncapSocket = udpEncapSocket;
        this.mIkePrf = ikePrf;
        this.mIkeDhGroup = ikeDh;
        this.mSkD = skD;
        mIsFirstChild = false;

        sendMessage(CMD_LOCAL_REQUEST_CREATE_CHILD);
    }

    /**
     * Initiate Delete Child procedure.
     *
     * <p>This method is called synchronously from IkeStateMachine. It proxies the synchronous call
     * as an asynchronous job to the ChildStateMachine handler.
     */
    public void deleteChildSession() {
        sendMessage(CMD_LOCAL_REQUEST_DELETE_CHILD);
    }

    /**
     * Initiate Rekey Child procedure.
     *
     * <p>This method is called synchronously from IkeStateMachine. It proxies the synchronous call
     * as an asynchronous job to the ChildStateMachine handler.
     */
    public void rekeyChildSession() {
        sendMessage(CMD_LOCAL_REQUEST_REKEY_CHILD);
    }

    /**
     * Update IPsec SAs via MOBIKE.
     *
     * <p>This method is called synchronously from IkeStateMachine, and may complete synchronously
     * if kernel MOBIKE can be used. Otherwise, it will fall back to using rekeys to synchronize
     * IPsec state.
     *
     * @param localAddress The local (outer) address from which traffic will originate.
     * @param remoteAddress The remote (outer) address to which traffic will be sent.
     * @param udpEncapSocket The socket to use for UDP encapsulation, or NULL if no encap needed.
     */
    public void performMigration(
            InetAddress localAddress,
            InetAddress remoteAddress,
            UdpEncapsulationSocket udpEncapSocket) {

        final UdpEncapsulationSocket oldEncapSocket = mUdpEncapSocket;

        this.mLocalAddress = localAddress;
        this.mRemoteAddress = remoteAddress;
        this.mUdpEncapSocket = udpEncapSocket;

        if (oldEncapSocket == mUdpEncapSocket
                && ShimUtils.getInstance()
                        .supportsSameSocketKernelMigration(mIkeContext.getContext())) {
            mIpSecManager.startTunnelModeTransformMigration(
                    mCurrentChildSaRecord.getInboundIpSecTransform(),
                    mRemoteAddress,
                    mLocalAddress);
            mIpSecManager.startTunnelModeTransformMigration(
                    mCurrentChildSaRecord.getOutboundIpSecTransform(),
                    mLocalAddress,
                    mRemoteAddress);
            executeUserCallback(() -> {
                mUserCallback.onIpSecTransformsMigrated(
                        mCurrentChildSaRecord.getInboundIpSecTransform(),
                        mCurrentChildSaRecord.getOutboundIpSecTransform());
            });

            mChildSmCallback.onProcedureFinished(ChildSessionStateMachine.this);
        } else {
            performRekeyMigration(localAddress, remoteAddress, udpEncapSocket);
        }
    }

    /**
     * Initiate Rekey Child procedure for MOBIKE (instead of migrating IPsec SAs).
     *
     * <p>This method should only be used as a fallback mode for devices that do not have
     * XFRM_MIGRATE kernel support.
     *
     * <p>This method is called synchronously from IkeStateMachine. It proxies the synchronous call
     * as an asynchronous job to the ChildStateMachine handler.
     *
     * <p>This method works similarly to {@link #rekeyChildSession()} in that it rekeys the Child
     * SAs associated with this state machine. However, the caller is notified of Child SA creation
     * via {@link ChildSessionCallback#onIpSecTransformsMigrated(android.net.IpSecTransform,
     * android.net.IpSecTransform)};
     *
     * @param localAddress The local (outer) address from which traffic will originate.
     * @param remoteAddress The remote (outer) address to which traffic will be sent.
     * @param udpEncapSocket The socket to use for UDP encapsulation, or NULL if no encap needed.
     */
    public void performRekeyMigration(
            InetAddress localAddress,
            InetAddress remoteAddress,
            UdpEncapsulationSocket udpEncapSocket) {

        this.mLocalAddress = localAddress;
        this.mRemoteAddress = remoteAddress;
        this.mUdpEncapSocket = udpEncapSocket;

        sendMessage(CMD_LOCAL_REQUEST_REKEY_CHILD_MOBIKE);
    }

    /**
     * Receive a request
     *
     * <p>This method is called synchronously from IkeStateMachine. It proxies the synchronous call
     * as an asynchronous job to the ChildStateMachine handler.
     *
     * @param exchangeSubtype the exchange subtype of this inbound request.
     * @param exchangeType the exchange type in the request message.
     * @param payloadList the Child-procedure-related payload list in the request message that needs
     *     validation.
     */
    public void receiveRequest(
            @IkeExchangeSubType int exchangeSubtype,
            @ExchangeType int exchangeType,
            List<IkePayload> payloadList) {
        sendMessage(
                CMD_HANDLE_RECEIVED_REQUEST,
                new ReceivedRequest(exchangeSubtype, exchangeType, payloadList));
    }

    /**
     * Receive a response.
     *
     * <p>This method is called synchronously from IkeStateMachine. It proxies the synchronous call
     * as an asynchronous job to the ChildStateMachine handler.
     *
     * @param exchangeType the exchange type in the response message that needs validation.
     * @param payloadList the Child-procedure-related payload list in the response message that
     *     needs validation.
     */
    public void receiveResponse(@ExchangeType int exchangeType, List<IkePayload> payloadList) {
        if (!isAwaitingCreateResp()) {
            sendMessage(
                    CMD_HANDLE_RECEIVED_RESPONSE, new ReceivedResponse(exchangeType, payloadList));
        }

        // If we are waiting for a Create/RekeyCreate response and the received message contains SA
        // payload we need to register for this provisional Child.
        int spi = registerProvisionalChildAndGetSpi(payloadList);
        sendMessage(
                CMD_HANDLE_RECEIVED_RESPONSE,
                new ReceivedCreateResponse(exchangeType, payloadList, spi));
    }

    private boolean isAwaitingCreateResp() {
        return (getCurrentState() == mCreateChildLocalCreate
                || getCurrentState() == mMobikeRekeyChildLocalCreate
                || getCurrentState() == mRekeyChildLocalCreate);
    }

    /**
     * Update SK_d with provided value when IKE SA is rekeyed.
     *
     * <p>It MUST be only called at the end of Rekey IKE procedure, which guarantees this Child
     * Session is not in Create Child or Rekey Child procedure.
     *
     * @param skD the new skD in byte array.
     */
    public void setSkD(byte[] skD) {
        mSkD = skD;
    }

    /**
     * Register provisioning ChildSessionStateMachine in IChildSessionSmCallback
     *
     * <p>This method is for avoiding CHILD_SA_NOT_FOUND error in IkeSessionStateMachine when remote
     * peer sends request for delete/rekey this Child SA before ChildSessionStateMachine sends
     * FirstChildNegotiationData or Create response to itself.
     */
    private int registerProvisionalChildAndGetSpi(List<IkePayload> respPayloads) {
        IkeSaPayload saPayload =
                IkePayload.getPayloadForTypeInProvidedList(
                        PAYLOAD_TYPE_SA, IkeSaPayload.class, respPayloads);

        if (saPayload == null) return SPI_NOT_REGISTERED;

        // IkeSaPayload.Proposal stores SPI in long type so as to be applied to both 8-byte IKE SPI
        // and 4-byte Child SPI. Here we cast the stored SPI to int to represent a Child SPI.
        int remoteGenSpi = (int) (saPayload.proposalList.get(0).spi);
        mChildSmCallback.onChildSaCreated(remoteGenSpi, this);
        return remoteGenSpi;
    }

    private void replyErrorNotification(@NotifyType int notifyType) {
        replyErrorNotification(notifyType, new byte[0]);
    }

    private void replyErrorNotification(@NotifyType int notifyType, byte[] notifyData) {
        List<IkePayload> outPayloads = new ArrayList<>(1);
        IkeNotifyPayload notifyPayload = new IkeNotifyPayload(notifyType, notifyData);
        outPayloads.add(notifyPayload);

        mChildSmCallback.onOutboundPayloadsReady(
                EXCHANGE_TYPE_INFORMATIONAL, true /*isResp*/, outPayloads, this);
    }

    private void sendDeleteIkeRequest() {
        List<IkePayload> outIkePayloads = new ArrayList<>(1);
        outIkePayloads.add(new IkeDeletePayload());

        mChildSmCallback.onOutboundPayloadsReady(
                EXCHANGE_TYPE_INFORMATIONAL,
                false /*isResp*/,
                outIkePayloads,
                ChildSessionStateMachine.this);
    }

    class OnIpSecSaPairCreatedRunnable implements Runnable {
        private final IpSecTransform mOut;
        private final IpSecTransform mIn;

        OnIpSecSaPairCreatedRunnable(ChildSaRecord childSaRecord) {
            mOut = childSaRecord.getOutboundIpSecTransform();
            mIn = childSaRecord.getInboundIpSecTransform();
        }

        @Override
        public void run() {
            mUserCallback.onIpSecTransformCreated(mOut, IpSecManager.DIRECTION_OUT);
            mUserCallback.onIpSecTransformCreated(mIn, IpSecManager.DIRECTION_IN);
        }
    }

    class OnIpSecSaPairDeletedRunnable implements Runnable {
        private final IpSecTransform mOut;
        private final IpSecTransform mIn;

        OnIpSecSaPairDeletedRunnable(ChildSaRecord childSaRecord) {
            mOut = childSaRecord.getOutboundIpSecTransform();
            mIn = childSaRecord.getInboundIpSecTransform();
        }

        @Override
        public void run() {
            mUserCallback.onIpSecTransformDeleted(mOut, IpSecManager.DIRECTION_OUT);
            mUserCallback.onIpSecTransformDeleted(mIn, IpSecManager.DIRECTION_IN);
        }
    }

    /**
     * ReceivedRequest contains exchange subtype and payloads that are extracted from a request
     * message to the current Child procedure.
     */
    private static class ReceivedRequest {
        @IkeExchangeSubType public final int exchangeSubtype;
        @ExchangeType public final int exchangeType;
        public final List<IkePayload> requestPayloads;

        ReceivedRequest(
                @IkeExchangeSubType int eSubtype,
                @ExchangeType int eType,
                List<IkePayload> reqPayloads) {
            exchangeSubtype = eSubtype;
            exchangeType = eType;
            requestPayloads = reqPayloads;
        }
    }

    /**
     * ReceivedResponse contains exchange type and payloads that are extracted from a response
     * message to the current Child procedure.
     */
    private static class ReceivedResponse {
        @ExchangeType public final int exchangeType;
        public final List<IkePayload> responsePayloads;

        ReceivedResponse(@ExchangeType int eType, List<IkePayload> respPayloads) {
            exchangeType = eType;
            responsePayloads = respPayloads;
        }
    }

    private static class ReceivedCreateResponse extends ReceivedResponse {
        public final int registeredSpi;

        ReceivedCreateResponse(@ExchangeType int eType, List<IkePayload> respPayloads, int spi) {
            super(eType, respPayloads);
            registeredSpi = spi;
        }
    }

    /**
     * FirstChildNegotiationData contains payloads for negotiating first Child SA in IKE_AUTH
     * request and IKE_AUTH response and callback to notify IkeSessionStateMachine the SA
     * negotiation result.
     */
    private static class FirstChildNegotiationData extends ReceivedCreateResponse {
        public final List<IkePayload> requestPayloads;

        FirstChildNegotiationData(
                List<IkePayload> reqPayloads, List<IkePayload> respPayloads, int spi) {
            super(EXCHANGE_TYPE_IKE_AUTH, respPayloads, spi);
            requestPayloads = reqPayloads;
        }
    }

    /** Top level state for handling uncaught exceptions for all subclasses. */
    abstract class ExceptionHandler extends ExceptionHandlerBase {
        @Override
        protected void cleanUpAndQuit(RuntimeException e) {
            // TODO: b/140123526 Send a response if exception was caught when processing a request.

            // Clean up all SaRecords.
            closeAllSaRecords(false /*expectSaClosed*/);

            executeUserCallback(
                    () -> {
                        mUserCallback.onClosedWithException(wrapAsIkeException(e));
                    });
            logWtf("Unexpected exception in " + getCurrentStateName(), e);
            quitSessionNow();
        }

        @Override
        protected String getCmdString(int cmd) {
            return CMD_TO_STR.get(cmd);
        }
    }

    /** Called when this StateMachine quits. */
    @Override
    protected void onQuitting() {
        // Clean up all SaRecords.
        closeAllSaRecords(true /*expectSaClosed*/);

        mChildSmCallback.onProcedureFinished(this);
        mChildSmCallback.onChildSessionClosed(mUserCallback);
    }

    private void closeAllSaRecords(boolean expectSaClosed) {
        closeChildSaRecord(mCurrentChildSaRecord, expectSaClosed);
        closeChildSaRecord(mLocalInitNewChildSaRecord, expectSaClosed);
        closeChildSaRecord(mRemoteInitNewChildSaRecord, expectSaClosed);

        mCurrentChildSaRecord = null;
        mLocalInitNewChildSaRecord = null;
        mRemoteInitNewChildSaRecord = null;
    }

    private void closeChildSaRecord(ChildSaRecord childSaRecord, boolean expectSaClosed) {
        if (childSaRecord == null) return;

        OnIpSecSaPairDeletedRunnable delRunnable = new OnIpSecSaPairDeletedRunnable(childSaRecord);
        executeUserCallback(delRunnable);

        mChildSmCallback.onChildSaDeleted(childSaRecord.getRemoteSpi());
        childSaRecord.close();

        if (!expectSaClosed) return;

        logWtf(
                "ChildSaRecord with local SPI: "
                        + childSaRecord.getLocalSpi()
                        + " is not correctly closed.");
    }

    private void handleChildFatalError(Exception error) {
        IkeException ikeException = wrapAsIkeException(error);
        loge("Child Session fatal error", ikeException);

        // Clean up all SaRecords and quit
        closeAllSaRecords(false /*expectSaClosed*/);
        executeUserCallback(
                () -> {
                    mUserCallback.onClosedWithException(ikeException);
                });
        quitSessionNow();
    }

    /**
     * This state handles the request to close Child Session immediately without initiating any
     * exchange.
     *
     * <p>Request for closing Child Session immediately is usually caused by the closing of IKE
     * Session. All states MUST be a child state of KillChildSessionParent to handle the closing
     * request.
     */
    private class KillChildSessionParent extends ExceptionHandler {
        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_KILL_SESSION:
                    closeAllSaRecords(false /*expectSaClosed*/);
                    executeUserCallback(
                            () -> {
                                mUserCallback.onClosed();
                            });
                    quitSessionNow();
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * CreateChildLocalCreateBase represents the common information for a locally-initiated initial
     * Child SA negotiation for setting up this Child Session.
     */
    private abstract class CreateChildLocalCreateBase extends ExceptionHandler {
        protected void validateAndBuildChild(
                List<IkePayload> reqPayloads,
                List<IkePayload> respPayloads,
                @ExchangeType int exchangeType,
                @ExchangeType int expectedExchangeType,
                int registeredSpi) {
            validateAndBuildChild(
                    reqPayloads,
                    respPayloads,
                    registeredSpi,
                    CreateChildSaHelper.validateAndNegotiateInitChild(
                            reqPayloads,
                            respPayloads,
                            exchangeType,
                            expectedExchangeType,
                            mChildSessionParams.isTransportMode(),
                            mIpSecSpiGenerator,
                            mRemoteAddress));
        }

        protected void validateAndBuildChild(
                List<IkePayload> reqPayloads,
                List<IkePayload> respPayloads,
                int registeredSpi,
                CreateChildResult createChildResult) {
            switch (createChildResult.status) {
                case CREATE_STATUS_OK:
                    try {
                        setUpNegotiatedResult(createChildResult);

                        mCurrentChildSaRecord =
                                ChildSaRecord.makeChildSaRecord(
                                        mIkeContext.getContext(),
                                        reqPayloads,
                                        respPayloads,
                                        createChildResult.initSpi,
                                        createChildResult.respSpi,
                                        mLocalAddress,
                                        mRemoteAddress,
                                        mUdpEncapSocket,
                                        mIkePrf,
                                        mChildIntegrity,
                                        mChildCipher,
                                        mSkD,
                                        mChildSessionParams.isTransportMode(),
                                        true /*isLocalInit*/,
                                        buildSaLifetimeAlarmSched(
                                                createChildResult.respSpi.getSpi()));

                        ChildSessionConfiguration sessionConfig =
                                buildChildSessionConfigFromResp(createChildResult, respPayloads);

                        OnIpSecSaPairCreatedRunnable createRunnable =
                                new OnIpSecSaPairCreatedRunnable(mCurrentChildSaRecord);
                        executeUserCallback(
                                () -> {
                                    createRunnable.run();
                                    mUserCallback.onOpened(sessionConfig);
                                });

                        transitionTo(mIdle);
                    } catch (GeneralSecurityException
                            | ResourceUnavailableException
                            | SpiUnavailableException
                            | IOException e) {
                        // #makeChildSaRecord failed.

                        // TODO: Initiate deletion
                        mChildSmCallback.onChildSaDeleted(createChildResult.respSpi.getSpi());
                        handleChildFatalError(e);
                    } finally {
                        // In the successful case the transform in ChildSaRecord has taken ownership
                        // of the SPI (in IpSecService), and will keep it alive.
                        createChildResult.initSpi.close();
                        createChildResult.respSpi.close();
                    }
                    break;
                case CREATE_STATUS_CHILD_ERROR_INVALID_MSG:
                    // TODO: Initiate deletion
                    handleCreationFailAndQuit(registeredSpi, createChildResult.exception);
                    break;
                case CREATE_STATUS_CHILD_ERROR_RCV_NOTIFY:
                    handleCreationFailAndQuit(registeredSpi, createChildResult.exception);
                    break;
                default:
                    cleanUpAndQuit(
                            new IllegalStateException(
                                    "Unrecognized status: " + createChildResult.status));
            }
        }

        private void setUpNegotiatedResult(CreateChildResult createChildResult) {
            // Build crypto tools using negotiated ChildSaProposal. It is ensured by {@link
            // IkeSaPayload#getVerifiedNegotiatedChildProposalPair} that the negotiated
            // ChildSaProposal is valid. The negotiated ChildSaProposal has exactly one encryption
            // algorithm. When it has a combined-mode encryption algorithm, it either does not have
            // integrity algorithm or only has one NONE value integrity algorithm. When the
            // negotiated ChildSaProposal has a normal encryption algorithm, it either does not have
            // integrity algorithm or has one integrity algorithm with any supported value.

            mSaProposal = createChildResult.negotiatedProposal;
            mChildCipher = IkeCipher.create(mSaProposal.getEncryptionTransforms()[0]);
            if (mSaProposal.getIntegrityTransforms().length != 0
                    && mSaProposal.getIntegrityTransforms()[0].id
                            != SaProposal.INTEGRITY_ALGORITHM_NONE) {
                mChildIntegrity = IkeMacIntegrity.create(mSaProposal.getIntegrityTransforms()[0]);
            }

            mLocalTs = createChildResult.initTs;
            mRemoteTs = createChildResult.respTs;
        }

        private ChildSessionConfiguration buildChildSessionConfigFromResp(
                CreateChildResult createChildResult, List<IkePayload> respPayloads) {
            IkeConfigPayload configPayload =
                    IkePayload.getPayloadForTypeInProvidedList(
                            PAYLOAD_TYPE_CP, IkeConfigPayload.class, respPayloads);

            if (mChildSessionParams.isTransportMode()
                    || configPayload == null
                    || configPayload.configType != IkeConfigPayload.CONFIG_TYPE_REPLY) {
                if (configPayload != null) {
                    logw("Unexpected config payload. Config Type: " + configPayload.configType);
                }

                return new ChildSessionConfiguration(
                        Arrays.asList(createChildResult.initTs),
                        Arrays.asList(createChildResult.respTs));
            } else {
                return new ChildSessionConfiguration(
                        Arrays.asList(createChildResult.initTs),
                        Arrays.asList(createChildResult.respTs),
                        configPayload);
            }
        }

        private void handleCreationFailAndQuit(int registeredSpi, IkeException exception) {
            if (registeredSpi != SPI_NOT_REGISTERED) {
                mChildSmCallback.onChildSaDeleted(registeredSpi);
            }
            handleChildFatalError(exception);
        }
    }

    private String getIntentIdentifier(int remoteSpi) {
        return IkeSessionStateMachine.TAG + "_" + mIkeSessionId + "_" + TAG + "_" + remoteSpi;
    }

    private Message getIntentIkeSmMsg(int localRequestType, int remoteSpi) {
        Bundle spiBundle = new Bundle();
        spiBundle.putInt(BUNDLE_KEY_CHILD_REMOTE_SPI, remoteSpi);

        return mIkeHandler.obtainMessage(
                CMD_ALARM_FIRED, mIkeSessionId, localRequestType, spiBundle);
    }

    private SaLifetimeAlarmScheduler buildSaLifetimeAlarmSched(int remoteSpi) {
        Message deleteMsg = getIntentIkeSmMsg(CMD_LOCAL_REQUEST_DELETE_CHILD, remoteSpi);
        Message rekeyMsg = getIntentIkeSmMsg(CMD_LOCAL_REQUEST_REKEY_CHILD, remoteSpi);

        PendingIntent deleteSaIntent =
                buildIkeAlarmIntent(
                        mIkeContext.getContext(),
                        ACTION_DELETE_CHILD,
                        getIntentIdentifier(remoteSpi),
                        deleteMsg);
        PendingIntent rekeySaIntent =
                buildIkeAlarmIntent(
                        mIkeContext.getContext(),
                        ACTION_REKEY_CHILD,
                        getIntentIdentifier(remoteSpi),
                        rekeyMsg);

        return new SaLifetimeAlarmScheduler(
                new IkeAlarmConfig(
                        mIkeContext.getContext(),
                        ACTION_DELETE_CHILD,
                        mChildSessionParams.getHardLifetimeMsInternal(),
                        deleteSaIntent,
                        deleteMsg),
                new IkeAlarmConfig(
                        mIkeContext.getContext(),
                        ACTION_REKEY_CHILD,
                        mChildSessionParams.getSoftLifetimeMsInternal(),
                        rekeySaIntent,
                        rekeyMsg));
    }

    /** Initial state of ChildSessionStateMachine. */
    class Initial extends CreateChildLocalCreateBase {
        List<IkePayload> mRequestPayloads;

        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_HANDLE_FIRST_CHILD_EXCHANGE:
                    FirstChildNegotiationData childNegotiationData =
                            (FirstChildNegotiationData) message.obj;
                    mRequestPayloads = childNegotiationData.requestPayloads;
                    List<IkePayload> respPayloads = childNegotiationData.responsePayloads;

                    // Negotiate Child SA. The exchangeType has been validated in
                    // IkeSessionStateMachine. Won't validate it again here.
                    validateAndBuildChild(
                            mRequestPayloads,
                            respPayloads,
                            EXCHANGE_TYPE_IKE_AUTH,
                            EXCHANGE_TYPE_IKE_AUTH,
                            childNegotiationData.registeredSpi);

                    return HANDLED;
                case CMD_LOCAL_REQUEST_CREATE_CHILD:
                    transitionTo(mCreateChildLocalCreate);
                    return HANDLED;
                case CMD_LOCAL_REQUEST_DELETE_CHILD:
                    // This may happen when creation has been rescheduled to be after deletion.
                    executeUserCallback(
                            () -> {
                                mUserCallback.onClosed();
                            });
                    quitSessionNow();
                    return HANDLED;
                case CMD_FORCE_TRANSITION:
                    transitionTo((State) message.obj);
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }

        @Override
        public void exitState() {
            CreateChildSaHelper.releaseSpiResources(mRequestPayloads);
        }
    }

    /**
     * CreateChildLocalCreate represents the state where Child Session initiates the Create Child
     * exchange.
     */
    class CreateChildLocalCreate extends CreateChildLocalCreateBase {
        private List<IkePayload> mRequestPayloads;

        @Override
        public void enterState() {
            try {
                mRequestPayloads =
                        CreateChildSaHelper.getInitChildCreateReqPayloads(
                                mIkeContext.getRandomnessFactory(),
                                mIpSecSpiGenerator,
                                mLocalAddress,
                                mChildSessionParams,
                                false /*isFirstChildSa*/);

                final ConfigAttribute[] configAttributes =
                        CreateChildSaHelper.getConfigAttributes(mChildSessionParams);
                if (configAttributes.length > 0) {
                    mRequestPayloads.add(
                            new IkeConfigPayload(
                                    false /*isReply*/, Arrays.asList(configAttributes)));
                }

                mChildSmCallback.onOutboundPayloadsReady(
                        EXCHANGE_TYPE_CREATE_CHILD_SA,
                        false /*isResp*/,
                        mRequestPayloads,
                        ChildSessionStateMachine.this);
            } catch (SpiUnavailableException | ResourceUnavailableException e) {
                // Fail to assign SPI
                handleChildFatalError(e);
            }
        }

        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_HANDLE_RECEIVED_RESPONSE:
                    ReceivedCreateResponse rcvResp = (ReceivedCreateResponse) message.obj;
                    CreateChildResult createChildResult =
                            CreateChildSaHelper.validateAndNegotiateInitChild(
                                    mRequestPayloads,
                                    rcvResp.responsePayloads,
                                    rcvResp.exchangeType,
                                    EXCHANGE_TYPE_CREATE_CHILD_SA,
                                    mChildSessionParams.isTransportMode(),
                                    mIpSecSpiGenerator,
                                    mRemoteAddress);

                    // If the response includes the error notification for TEMPORARY_FAILURE, retry
                    // creating the Child.
                    if (isTemporaryFailure(createChildResult)) {
                        transitionTo(mInitial);

                        mChildSmCallback.scheduleRetryLocalRequest(
                                mLocalRequestFactory.getChildLocalRequest(
                                        CMD_LOCAL_REQUEST_CREATE_CHILD,
                                        mUserCallback,
                                        mChildSessionParams));
                        return HANDLED;
                    }

                    validateAndBuildChild(
                            mRequestPayloads,
                            rcvResp.responsePayloads,
                            rcvResp.registeredSpi,
                            createChildResult);
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }

        @Override
        public void exitState() {
            CreateChildSaHelper.releaseSpiResources(mRequestPayloads);
        }

        private boolean isTemporaryFailure(CreateChildResult createChildResult) {
            if (createChildResult.status != CREATE_STATUS_CHILD_ERROR_RCV_NOTIFY) {
                return false;
            }
            return createChildResult.exception instanceof TemporaryFailureException;
        }
    }

    /**
     * Idle represents a state when there is no ongoing IKE exchange affecting established Child SA.
     */
    class Idle extends ExceptionHandler {
        @Override
        public void enterState() {
            maybeNotifyIkeSessionStateMachine();
        }

        protected void maybeNotifyIkeSessionStateMachine() {
            mChildSmCallback.onProcedureFinished(ChildSessionStateMachine.this);
        }

        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_LOCAL_REQUEST_DELETE_CHILD:
                    transitionTo(mDeleteChildLocalDelete);
                    return HANDLED;
                case CMD_LOCAL_REQUEST_REKEY_CHILD:
                    transitionTo(mRekeyChildLocalCreate);
                    return HANDLED;
                case CMD_LOCAL_REQUEST_REKEY_CHILD_MOBIKE:
                    transitionTo(mMobikeRekeyChildLocalCreate);
                    return HANDLED;
                case CMD_HANDLE_RECEIVED_REQUEST:
                    ReceivedRequest req = (ReceivedRequest) message.obj;
                    switch (req.exchangeSubtype) {
                        case IKE_EXCHANGE_SUBTYPE_DELETE_CHILD:
                            deferMessage(message);
                            transitionTo(mDeleteChildRemoteDelete);
                            return HANDLED;
                        case IKE_EXCHANGE_SUBTYPE_REKEY_CHILD:
                            deferMessage(message);
                            transitionTo(mRekeyChildRemoteCreate);
                            return HANDLED;
                        default:
                            return NOT_HANDLED;
                    }
                case CMD_FORCE_TRANSITION: // Testing command
                    transitionTo((State) message.obj);
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * This class is for handling the case when the previous procedure was finished by a new request
     *
     * <p>This state is the destination state when Child Session receives a new procedure request in
     * Rekey Delete. When entering this state, Child Session will process the deferred request as
     * Idle state does but will not notify IKE Session that Child Session has finished all the
     * procedures. It prevents IKE Session from going back to Idle state when its Child Session is
     * still busy.
     */
    class IdleWithDeferredRequest extends Idle {
        @Override
        public void maybeNotifyIkeSessionStateMachine() {
            // Do not notify IkeSessionStateMachine because Child Session needs to process the
            // deferred request and start a new procedure
        }
    }

    /**
     * This class represents the state that Child Session was closed by the remote while waiting for
     * a response.
     *
     * <p>This state is the destination state when Child Session receives a Delete request while
     * waitng for a Rekey Create response. When that happens, Child Session should close all IPsec
     * SAs and notify the user immediately to prevent security risk. Child Session also needs to
     * continue waiting for the response and keep its parent IKE Session retransmitting the request,
     * as required by the IKE spec.
     */
    private class ClosedAndAwaitResponse extends ExceptionHandler {
        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_HANDLE_RECEIVED_RESPONSE:
                    // Do not need to verify the response since the Child Session is already closed
                    quitSessionNow();
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * DeleteResponderBase represents all states after Child Session is established
     *
     * <p>All post-init states share common functionality of being able to respond to Delete Child
     * requests.
     */
    private abstract class DeleteResponderBase extends ExceptionHandler {
        /**
         * Check if the payload list has a Delete Payload that includes the remote SPI of the input
         * ChildSaRecord.
         */
        protected boolean hasRemoteChildSpiForDelete(
                List<IkePayload> payloads, ChildSaRecord expectedRecord) {
            List<IkeDeletePayload> delPayloads =
                    IkePayload.getPayloadListForTypeInProvidedList(
                            PAYLOAD_TYPE_DELETE, IkeDeletePayload.class, payloads);

            for (IkeDeletePayload delPayload : delPayloads) {
                for (int spi : delPayload.spisToDelete) {
                    if (spi == expectedRecord.getRemoteSpi()) return true;
                }
            }
            return false;
        }

        /**
         * Build and send payload list that has a Delete Payload that includes the local SPI of the
         * input ChildSaRecord.
         */
        protected void sendDeleteChild(ChildSaRecord childSaRecord, boolean isResp) {
            List<IkePayload> outIkePayloads = new ArrayList<>(1);
            outIkePayloads.add(new IkeDeletePayload(new int[] {childSaRecord.getLocalSpi()}));

            mChildSmCallback.onOutboundPayloadsReady(
                    EXCHANGE_TYPE_INFORMATIONAL,
                    isResp,
                    outIkePayloads,
                    ChildSessionStateMachine.this);
        }

        /**
         * Helper method for responding to a session deletion request
         *
         * <p>Note that this method expects that the session is keyed on the mCurrentChildSaRecord
         * and closing this Child SA indicates that the remote wishes to end the session as a whole.
         * As such, this should not be used in rekey cases where there is any ambiguity as to which
         * Child SA the session is reliant upon.
         *
         * <p>Note that this method will also quit the state machine
         */
        protected void handleDeleteSessionRequest(List<IkePayload> payloads) {
            if (!hasRemoteChildSpiForDelete(payloads, mCurrentChildSaRecord)) {
                cleanUpAndQuit(
                        new IllegalStateException(
                                "Found no remote SPI for mCurrentChildSaRecord in a Delete Child"
                                        + " request."));
            } else {
                sendDeleteChild(mCurrentChildSaRecord, true /*isResp*/);
                closeSessionAndNotifyUser(true /* quitStateMachine */);
            }
        }

        protected void closeSessionAndNotifyUser(boolean quitStateMachine) {
            OnIpSecSaPairDeletedRunnable delRunnable =
                    new OnIpSecSaPairDeletedRunnable(mCurrentChildSaRecord);
            executeUserCallback(
                    () -> {
                        delRunnable.run();
                        mUserCallback.onClosed();
                    });

            mChildSmCallback.onChildSaDeleted(mCurrentChildSaRecord.getRemoteSpi());
            mCurrentChildSaRecord.close();
            mCurrentChildSaRecord = null;

            if (quitStateMachine) {
                quitSessionNow();
            }
        }
    }

    /**
     * DeleteBase abstracts deletion handling for all states initiating and responding to a Delete
     * Child exchange
     *
     * <p>All subclasses of this state share common functionality that a deletion request is sent,
     * and the response is received.
     */
    private abstract class DeleteBase extends DeleteResponderBase {
        /** Validate payload types in Delete Child response. */
        protected void validateDeleteRespPayloadAndExchangeType(
                List<IkePayload> respPayloads, @ExchangeType int exchangeType)
                throws IkeProtocolException {

            if (exchangeType != EXCHANGE_TYPE_INFORMATIONAL) {
                throw new InvalidSyntaxException(
                        "Unexpected exchange type in Delete Child response: " + exchangeType);
            }

            for (IkePayload payload : respPayloads) {
                handlePayload:
                switch (payload.payloadType) {
                    case PAYLOAD_TYPE_DELETE:
                        // A Delete Payload is only required when it is not simultaneous deletion.
                        // Included Child SPIs are verified in the subclass to make sure the remote
                        // side is deleting the right SAs.
                        break handlePayload;
                    case PAYLOAD_TYPE_NOTIFY:
                        IkeNotifyPayload notify = (IkeNotifyPayload) payload;
                        if (!notify.isErrorNotify()) {
                            logw(
                                    "Unexpected or unknown status notification in Delete Child"
                                            + " response: "
                                            + notify.notifyType);
                            break handlePayload;
                        }

                        throw notify.validateAndBuildIkeException();
                    default:
                        logw(
                                "Unexpected payload type in Delete Child response: "
                                        + payload.payloadType);
                }
            }
        }
    }

    /**
     * DeleteChildLocalDelete represents the state where Child Session initiates the Delete Child
     * exchange.
     */
    class DeleteChildLocalDelete extends DeleteBase {
        private boolean mSimulDeleteDetected = false;

        @Override
        public void enterState() {
            mSimulDeleteDetected = false;
            sendDeleteChild(mCurrentChildSaRecord, false /*isResp*/);
        }

        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_HANDLE_RECEIVED_RESPONSE:
                    try {
                        ReceivedResponse resp = (ReceivedResponse) message.obj;
                        validateDeleteRespPayloadAndExchangeType(
                                resp.responsePayloads, resp.exchangeType);

                        boolean currentSaSpiFound =
                                hasRemoteChildSpiForDelete(
                                        resp.responsePayloads, mCurrentChildSaRecord);
                        if (!currentSaSpiFound && !mSimulDeleteDetected) {
                            throw new InvalidSyntaxException(
                                    "Found no remote SPI in received Delete response.");
                        } else if (currentSaSpiFound && mSimulDeleteDetected) {
                            // As required by the RFC 7296, in simultaneous delete case, the remote
                            // side MUST NOT include SPI of mCurrentChildSaRecord. However, to
                            // provide better interoperatibility, IKE library will keep IKE Session
                            // alive and continue the deleting process.
                            logw(
                                    "Found remote SPI in the Delete response in a simultaneous"
                                            + " deletion case");
                        }

                        closeSessionAndNotifyUser(true /* quitStateMachine */);
                    } catch (IkeProtocolException e) {
                        // Shut down Child Session and notify users the error.
                        handleChildFatalError(e);
                    }
                    return HANDLED;
                case CMD_HANDLE_RECEIVED_REQUEST:
                    ReceivedRequest req = (ReceivedRequest) message.obj;
                    switch (req.exchangeSubtype) {
                        case IKE_EXCHANGE_SUBTYPE_DELETE_CHILD:
                            // It has been verified in IkeSessionStateMachine that the incoming
                            // request can be ONLY for mCurrentChildSaRecord at this point.
                            if (!hasRemoteChildSpiForDelete(
                                    req.requestPayloads, mCurrentChildSaRecord)) {
                                // Program error
                                cleanUpAndQuit(
                                        new IllegalStateException(
                                                "Found no remote SPI for mCurrentChildSaRecord in"
                                                        + " a Delete request"));

                            } else {
                                mChildSmCallback.onOutboundPayloadsReady(
                                        EXCHANGE_TYPE_INFORMATIONAL,
                                        true /*isResp*/,
                                        new LinkedList<>(),
                                        ChildSessionStateMachine.this);
                                mSimulDeleteDetected = true;
                            }
                            return HANDLED;
                        case IKE_EXCHANGE_SUBTYPE_REKEY_CHILD:
                            replyErrorNotification(ERROR_TYPE_TEMPORARY_FAILURE);
                            return HANDLED;
                        default:
                            cleanUpAndQuit(
                                    new IllegalStateException(
                                            "Invalid exchange subtype for Child Session: "
                                                    + req.exchangeSubtype));
                            return HANDLED;
                    }
                default:
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * DeleteChildRemoteDelete represents the state where Child Session receives the Delete Child
     * request.
     */
    class DeleteChildRemoteDelete extends DeleteResponderBase {
        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_HANDLE_RECEIVED_REQUEST:
                    ReceivedRequest req = (ReceivedRequest) message.obj;
                    if (req.exchangeSubtype == IKE_EXCHANGE_SUBTYPE_DELETE_CHILD) {
                        handleDeleteSessionRequest(req.requestPayloads);
                        return HANDLED;
                    }
                    return NOT_HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * RekeyChildLocalCreate represents the state where Child Session initiates the Rekey Child
     * exchange.
     *
     * <p>As indicated in RFC 7296 section 2.8, "when rekeying, the new Child SA SHOULD NOT have
     * different Traffic Selectors and algorithms than the old one."
     */
    class RekeyChildLocalCreate extends DeleteResponderBase {
        private List<IkePayload> mRequestPayloads;

        @Override
        public void enterState() {
            try {
                ChildSaProposal saProposal = mSaProposal;
                if (mIsFirstChild) {
                    saProposal = addDhGroupsFromChildSessionParamsIfAbsent();
                }

                // Build request with negotiated proposal and TS.
                mRequestPayloads =
                        CreateChildSaHelper.getRekeyChildCreateReqPayloads(
                                mIkeContext.getRandomnessFactory(),
                                mIpSecSpiGenerator,
                                mLocalAddress,
                                saProposal,
                                mLocalTs,
                                mRemoteTs,
                                mCurrentChildSaRecord.getLocalSpi(),
                                mChildSessionParams.isTransportMode());
                mChildSmCallback.onOutboundPayloadsReady(
                        EXCHANGE_TYPE_CREATE_CHILD_SA,
                        false /*isResp*/,
                        mRequestPayloads,
                        ChildSessionStateMachine.this);
            } catch (SpiUnavailableException | ResourceUnavailableException e) {
                loge("Fail to assign Child SPI. Schedule a retry for rekey Child");
                mCurrentChildSaRecord.rescheduleRekey(RETRY_INTERVAL_MS);
                transitionTo(mIdle);
            }
        }

        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_HANDLE_RECEIVED_REQUEST:
                    ReceivedRequest req = (ReceivedRequest) message.obj;

                    if (req.exchangeSubtype == IKE_EXCHANGE_SUBTYPE_DELETE_CHILD) {
                        // Handle Delete request, notify users and do state transition to continue
                        // waiting for the response
                        sendDeleteChild(mCurrentChildSaRecord, true /*isResp*/);
                        closeSessionAndNotifyUser(false /* quitStateMachine */);
                        transitionTo(mClosedAndAwaitResponse);
                    } else {
                        replyErrorNotification(ERROR_TYPE_TEMPORARY_FAILURE);
                    }
                    return HANDLED;
                case CMD_HANDLE_RECEIVED_RESPONSE:
                    ReceivedCreateResponse resp = (ReceivedCreateResponse) message.obj;
                    CreateChildResult createChildResult =
                            CreateChildSaHelper.validateAndNegotiateRekeyChildResp(
                                    mRequestPayloads,
                                    resp.responsePayloads,
                                    resp.exchangeType,
                                    EXCHANGE_TYPE_CREATE_CHILD_SA,
                                    mChildSessionParams.isTransportMode(),
                                    mCurrentChildSaRecord,
                                    mIpSecSpiGenerator,
                                    mRemoteAddress);

                    switch (createChildResult.status) {
                        case CREATE_STATUS_OK:
                            try {
                                // Do not need to update TS because they are not changed.
                                mSaProposal = createChildResult.negotiatedProposal;

                                mLocalInitNewChildSaRecord =
                                        ChildSaRecord.makeChildSaRecord(
                                                mIkeContext.getContext(),
                                                mRequestPayloads,
                                                resp.responsePayloads,
                                                createChildResult.initSpi,
                                                createChildResult.respSpi,
                                                mLocalAddress,
                                                mRemoteAddress,
                                                mUdpEncapSocket,
                                                mIkePrf,
                                                mChildIntegrity,
                                                mChildCipher,
                                                mSkD,
                                                mChildSessionParams.isTransportMode(),
                                                true /*isLocalInit*/,
                                                buildSaLifetimeAlarmSched(
                                                        createChildResult.respSpi.getSpi()));

                                notifyCallerForLocalChildSaRekey();

                                transitionTo(mRekeyChildLocalDelete);
                            } catch (GeneralSecurityException
                                    | ResourceUnavailableException
                                    | SpiUnavailableException
                                    | IOException e) {
                                // #makeChildSaRecord failed
                                handleProcessRespOrSaCreationFailAndQuit(resp.registeredSpi, e);
                            } finally {
                                // In the successful case the transform in ChildSaRecord has taken
                                // ownership of the SPI (in IpSecService), and will keep it alive.
                                createChildResult.initSpi.close();
                                createChildResult.respSpi.close();
                            }
                            break;
                        case CREATE_STATUS_CHILD_ERROR_INVALID_MSG:
                            handleProcessRespOrSaCreationFailAndQuit(
                                    resp.registeredSpi, createChildResult.exception);
                            break;
                        case CREATE_STATUS_CHILD_ERROR_RCV_NOTIFY:
                            handleErrorNotify(createChildResult.exception);
                            transitionTo(mIdle);
                            break;
                        default:
                            cleanUpAndQuit(
                                    new IllegalStateException(
                                            "Unrecognized status: " + createChildResult.status));
                    }
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }

        protected void notifyCallerForLocalChildSaRekey() {
            OnIpSecSaPairCreatedRunnable createRunnable =
                    new OnIpSecSaPairCreatedRunnable(mLocalInitNewChildSaRecord);
            executeUserCallback(createRunnable);
        }

        protected void handleProcessRespOrSaCreationFailAndQuit(
                int registeredSpi, Exception exception) {
            // We don't retry rekey if failure was caused by invalid response or SA creation error.
            // Reason is there is no way to notify the remote side the old SA is still alive but the
            // new one has failed. Sending delete request for new SA indicates the rekey has
            // finished and the new SA has died.

            // TODO: Initiate deletion on newly created SA
            if (registeredSpi != SPI_NOT_REGISTERED) {
                mChildSmCallback.onChildSaDeleted(registeredSpi);
            }
            handleChildFatalError(exception);
        }

        protected void handleErrorNotify(Exception exception) {
            loge("Received error notification for rekey Child. Schedule a retry");
            mCurrentChildSaRecord.rescheduleRekey(RETRY_INTERVAL_MS);
        }

        @Override
        public void exitState() {
            CreateChildSaHelper.releaseSpiResources(mRequestPayloads);
        }
    }

    /**
     * MobikeRekeyChildLocalCreate represents the state where Child Session initiates the Rekey
     * Child exchange for MOBIKE-enabled IKE Sessions.
     *
     * <p>MobikeRekeyChildLocalCreate behaves similarly to RekeyChildLocalCreate except:
     *
     * <ul>
     *   <li>It notifies the caller of Child SA creation via {@link
     *       ChildSessionCallback#onIpSecTransformsMigrated(android.net.IpSecTransform,
     *       android.net.IpSecTransform)}. .
     *   <li>It tears down IKE SA immediately if any error occurs. In this case the origin Child SA
     *       has lost connectivity due to the changed IP addresses, and renewing the Child SA by
     *       MOBIKE rekey has failed. IKE module will handle this failure by tearing down the IKE
     *       Session and notifying the caller. In this way, the caller can immediately re-establish
     *       the IKE session if needed.
     * </ul>
     *
     * <p>As indicated in RFC 7296 section 2.8, "when rekeying, the new Child SA SHOULD NOT have
     * different Traffic Selectors and algorithms than the old one."
     */
    class MobikeRekeyChildLocalCreate extends RekeyChildLocalCreate {
        @Override
        protected void notifyCallerForLocalChildSaRekey() {
            IpSecTransform inTransform = mLocalInitNewChildSaRecord.getInboundIpSecTransform();
            IpSecTransform outTransform = mLocalInitNewChildSaRecord.getOutboundIpSecTransform();
            executeUserCallback(
                    () -> mUserCallback.onIpSecTransformsMigrated(inTransform, outTransform));
        }

        @Override
        protected void handleProcessRespOrSaCreationFailAndQuit(
                int registeredSpi, Exception exception) {
            sendDeleteIkeRequest();
            mChildSmCallback.onFatalIkeSessionError(exception);
        }

        @Override
        protected void handleErrorNotify(Exception exception) {
            loge("Received error notification for rekey Child. Tear down IKE SA");
            sendDeleteIkeRequest();
            mChildSmCallback.onFatalIkeSessionError(exception);
        }
    }

    private ChildSaProposal addDhGroupsFromChildSessionParamsIfAbsent() {
        // DH groups are excluded for the first child. Add dh groups from child session params in
        // this case.
        if (mSaProposal.getDhGroups().size() != 0) {
            return mSaProposal;
        }

        Set<DhGroupTransform> dhGroupSet = new LinkedHashSet<>();
        for (SaProposal saProposal : mChildSessionParams.getSaProposals()) {
            if (!mSaProposal.isNegotiatedFromExceptDhGroup(saProposal)) continue;
            dhGroupSet.addAll(Arrays.asList(saProposal.getDhGroupTransforms()));
        }

        DhGroupTransform[] dhGroups = new DhGroupTransform[dhGroupSet.size()];
        dhGroupSet.toArray(dhGroups);

        return new ChildSaProposal(
                mSaProposal.getEncryptionTransforms(),
                mSaProposal.getIntegrityTransforms(),
                dhGroups,
                mSaProposal.getEsnTransforms());
    }

    /**
     * RekeyChildRemoteCreate represents the state where Child Session receives a Rekey Child
     * request.
     *
     * <p>As indicated in RFC 7296 section 2.8, "when rekeying, the new Child SA SHOULD NOT have
     * different Traffic Selectors and algorithms than the old one."
     *
     * <p>Errors in this exchange with no specific protocol error code will all be classified to use
     * NO_PROPOSAL_CHOSEN. The reason that we don't use NO_ADDITIONAL_SAS is because it indicates
     * "responder is unwilling to accept any more Child SAs on this IKE SA.", according to RFC 7296.
     * Sending this error may mislead the remote peer.
     */
    class RekeyChildRemoteCreate extends ExceptionHandler {
        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_HANDLE_RECEIVED_REQUEST:
                    ReceivedRequest req = (ReceivedRequest) message.obj;

                    if (req.exchangeSubtype == IKE_EXCHANGE_SUBTYPE_REKEY_CHILD) {
                        handleCreateChildRequest(req);
                        return HANDLED;
                    }

                    return NOT_HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }

        private void handleCreateChildRequest(ReceivedRequest req) {
            List<IkePayload> reqPayloads = null;
            List<IkePayload> respPayloads = null;
            try {
                reqPayloads = req.requestPayloads;

                // Build a rekey response payload list with our previously selected proposal,
                // against which we will validate the received request. It is guaranteed in
                // IkeSessionStateMachine#getIkeExchangeSubType that a SA Payload is included in the
                // inbound request payload list.
                IkeSaPayload reqSaPayload =
                        IkePayload.getPayloadForTypeInProvidedList(
                                PAYLOAD_TYPE_SA, IkeSaPayload.class, reqPayloads);

                IkeKePayload reqKePayload =
                        IkePayload.getPayloadForTypeInProvidedList(
                                PAYLOAD_TYPE_KE, IkeKePayload.class, reqPayloads);

                ChildSaProposal saProposal = mSaProposal;
                if (reqKePayload != null) {
                    saProposal =
                            reqSaPayload.getNegotiatedChildProposalWithDh(
                                    mSaProposal,
                                    mChildSessionParams.getChildSaProposals(),
                                    reqKePayload.dhGroup,
                                    mIkeDhGroup);
                }


                byte respProposalNumber = reqSaPayload.getNegotiatedProposalNumber(saProposal);

                respPayloads =
                        CreateChildSaHelper.getRekeyChildCreateRespPayloads(
                                mIkeContext.getRandomnessFactory(),
                                mIpSecSpiGenerator,
                                mLocalAddress,
                                respProposalNumber,
                                saProposal,
                                mLocalTs,
                                mRemoteTs,
                                mCurrentChildSaRecord.getLocalSpi(),
                                mChildSessionParams.isTransportMode());
            } catch (NoValidProposalChosenException | InvalidKeException e) {
                handleCreationFailureAndBackToIdle(e);
                return;
            } catch (SpiUnavailableException | ResourceUnavailableException e) {
                handleCreationFailureAndBackToIdle(
                        new NoValidProposalChosenException("Fail to assign inbound SPI", e));
                return;
            }

            CreateChildResult createChildResult =
                    CreateChildSaHelper.validateAndNegotiateRekeyChildRequest(
                            reqPayloads,
                            respPayloads,
                            req.exchangeType /*exchangeType*/,
                            EXCHANGE_TYPE_CREATE_CHILD_SA /*expectedExchangeType*/,
                            mChildSessionParams.isTransportMode(),
                            mIpSecSpiGenerator,
                            mRemoteAddress);

            switch (createChildResult.status) {
                case CREATE_STATUS_OK:
                    try {
                        // Do not need to update TS because they are not changed.
                        mSaProposal = createChildResult.negotiatedProposal;

                        mRemoteInitNewChildSaRecord =
                                ChildSaRecord.makeChildSaRecord(
                                        mIkeContext.getContext(),
                                        reqPayloads,
                                        respPayloads,
                                        createChildResult.initSpi,
                                        createChildResult.respSpi,
                                        mLocalAddress,
                                        mRemoteAddress,
                                        mUdpEncapSocket,
                                        mIkePrf,
                                        mChildIntegrity,
                                        mChildCipher,
                                        mSkD,
                                        mChildSessionParams.isTransportMode(),
                                        false /*isLocalInit*/,
                                        buildSaLifetimeAlarmSched(
                                                createChildResult.initSpi.getSpi()));

                        mChildSmCallback.onChildSaCreated(
                                mRemoteInitNewChildSaRecord.getRemoteSpi(),
                                ChildSessionStateMachine.this);

                        // To avoid traffic loss, outbound transform should only be applied once
                        // the remote has (implicitly) acknowledged our response via the
                        // delete-old-SA request. This will be performed in the finishRekey()
                        // method.
                        IpSecTransform inTransform =
                                mRemoteInitNewChildSaRecord.getInboundIpSecTransform();
                        executeUserCallback(
                                () -> {
                                    mUserCallback.onIpSecTransformCreated(
                                            inTransform, IpSecManager.DIRECTION_IN);
                                });

                        mChildSmCallback.onOutboundPayloadsReady(
                                EXCHANGE_TYPE_CREATE_CHILD_SA,
                                true /*isResp*/,
                                respPayloads,
                                ChildSessionStateMachine.this);

                        transitionTo(mRekeyChildRemoteDelete);
                    } catch (GeneralSecurityException
                            | ResourceUnavailableException
                            | SpiUnavailableException
                            | IOException e) {
                        // #makeChildSaRecord failed.
                        handleCreationFailureAndBackToIdle(
                                new NoValidProposalChosenException(
                                        "Error in Child SA creation", e));
                    } finally {
                        // In the successful case the transform in ChildSaRecord has taken ownership
                        // of the SPI (in IpSecService), and will keep it alive.
                        createChildResult.initSpi.close();
                        createChildResult.respSpi.close();
                    }
                    break;
                case CREATE_STATUS_CHILD_ERROR_INVALID_MSG:
                    IkeException error = createChildResult.exception;
                    if (error instanceof IkeProtocolException) {
                        handleCreationFailureAndBackToIdle((IkeProtocolException) error);
                    } else {
                        handleCreationFailureAndBackToIdle(
                                new NoValidProposalChosenException(
                                        "Error in validating Create Child request", error));
                    }
                    break;
                case CREATE_STATUS_CHILD_ERROR_RCV_NOTIFY:
                    cleanUpAndQuit(
                            new IllegalStateException(
                                    "Unexpected processing status in Create Child request: "
                                            + createChildResult.status));
                    break;
                default:
                    cleanUpAndQuit(
                            new IllegalStateException(
                                    "Unrecognized status: " + createChildResult.status));
            }
        }

        private void handleCreationFailureAndBackToIdle(IkeProtocolException e) {
            loge("Received invalid Rekey Child request. Reject with error notification", e);

            ArrayList<IkePayload> payloads = new ArrayList<>(1);
            payloads.add(e.buildNotifyPayload());
            mChildSmCallback.onOutboundPayloadsReady(
                    EXCHANGE_TYPE_CREATE_CHILD_SA,
                    true /*isResp*/,
                    payloads,
                    ChildSessionStateMachine.this);

            transitionTo(mIdle);
        }
    }

    /**
     * RekeyChildDeleteBase represents common behaviours of deleting stage during rekeying Child SA.
     */
    abstract class RekeyChildDeleteBase extends DeleteBase {
        @Override
        public boolean processStateMessage(Message message) {
            switch (message.what) {
                case CMD_HANDLE_RECEIVED_REQUEST:
                    try {
                        if (isOnNewSa((ReceivedRequest) message.obj)) {
                            finishRekey();
                            deferMessage(message);
                            transitionTo(mIdleWithDeferredRequest);
                            return HANDLED;
                        }
                        return NOT_HANDLED;
                    } catch (IllegalStateException e) {
                        cleanUpAndQuit(e);
                        return HANDLED;
                    }
                default:
                    return NOT_HANDLED;
            }
        }

        private boolean isOnNewSa(ReceivedRequest req) {
            switch (req.exchangeSubtype) {
                case IKE_EXCHANGE_SUBTYPE_DELETE_CHILD:
                    return hasRemoteChildSpiForDelete(req.requestPayloads, mChildSaRecordSurviving);
                case IKE_EXCHANGE_SUBTYPE_REKEY_CHILD:
                    return CreateChildSaHelper.hasRemoteChildSpiForRekey(
                            req.requestPayloads, mChildSaRecordSurviving);
                default:
                    throw new IllegalStateException(
                            "Invalid exchange subtype for Child Session: " + req.exchangeSubtype);
            }
        }

        // Rekey timer for old SA will be cancelled as part of the closing of the SA.
        protected void finishRekey() {
            OnIpSecSaPairDeletedRunnable delRunnable =
                    new OnIpSecSaPairDeletedRunnable(mCurrentChildSaRecord);
            executeUserCallback(delRunnable);

            mChildSmCallback.onChildSaDeleted(mCurrentChildSaRecord.getRemoteSpi());
            mCurrentChildSaRecord.close();

            mCurrentChildSaRecord = mChildSaRecordSurviving;

            mLocalInitNewChildSaRecord = null;
            mRemoteInitNewChildSaRecord = null;
            mChildSaRecordSurviving = null;
        }
    }

    /**
     * RekeyChildLocalDelete represents the deleting stage of a locally-initiated Rekey Child
     * procedure.
     */
    class RekeyChildLocalDelete extends RekeyChildDeleteBase {
        private boolean mSimulDeleteDetected;

        @Override
        public void enterState() {
            mSimulDeleteDetected = false;
            mChildSaRecordSurviving = mLocalInitNewChildSaRecord;
            sendDeleteChild(mCurrentChildSaRecord, false /*isResp*/);
        }

        @Override
        public boolean processStateMessage(Message message) {
            if (super.processStateMessage(message) == HANDLED) {
                return HANDLED;
            }

            switch (message.what) {
                case CMD_HANDLE_RECEIVED_REQUEST:
                    ReceivedRequest req = (ReceivedRequest) message.obj;

                    if (req.exchangeSubtype == IKE_EXCHANGE_SUBTYPE_DELETE_CHILD) {
                        // Reply with empty message during simultaneous deleting and keep waiting
                        // for Delete response.
                        mChildSmCallback.onOutboundPayloadsReady(
                                EXCHANGE_TYPE_INFORMATIONAL,
                                true /*isResp*/,
                                new ArrayList<>(),
                                ChildSessionStateMachine.this);
                        mSimulDeleteDetected = true;
                    } else {
                        replyErrorNotification(ERROR_TYPE_TEMPORARY_FAILURE);
                    }
                    return HANDLED;
                case CMD_HANDLE_RECEIVED_RESPONSE:
                    try {
                        ReceivedResponse resp = (ReceivedResponse) message.obj;
                        validateDeleteRespPayloadAndExchangeType(
                                resp.responsePayloads, resp.exchangeType);

                        boolean currentSaSpiFound =
                                hasRemoteChildSpiForDelete(
                                        resp.responsePayloads, mCurrentChildSaRecord);
                        if (!mSimulDeleteDetected && !currentSaSpiFound) {
                            loge(
                                    "Found no remote SPI for current SA in received Delete"
                                        + " response. Shutting down old SA and finishing rekey.");
                        }
                    } catch (IkeProtocolException e) {
                        loge(
                                "Received Delete response with invalid syntax or error"
                                    + " notifications. Shutting down old SA and finishing rekey.",
                                e);
                    }
                    finishRekey();
                    transitionTo(mIdle);
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }
    }

    /**
     * RekeyChildRemoteDelete represents the deleting stage of a remotely-initiated Rekey Child
     * procedure.
     */
    class RekeyChildRemoteDelete extends RekeyChildDeleteBase {
        @Override
        public void enterState() {
            mChildSaRecordSurviving = mRemoteInitNewChildSaRecord;
            sendMessageDelayed(TIMEOUT_REKEY_REMOTE_DELETE, REKEY_DELETE_TIMEOUT_MS);
        }

        @Override
        public boolean processStateMessage(Message message) {
            if (super.processStateMessage(message) == HANDLED) {
                return HANDLED;
            }

            switch (message.what) {
                case CMD_HANDLE_RECEIVED_REQUEST:
                    ReceivedRequest req = (ReceivedRequest) message.obj;

                    if (req.exchangeSubtype == IKE_EXCHANGE_SUBTYPE_DELETE_CHILD) {
                        handleDeleteRequest(req.requestPayloads);

                    } else {
                        replyErrorNotification(ERROR_TYPE_TEMPORARY_FAILURE);
                    }
                    return HANDLED;
                case TIMEOUT_REKEY_REMOTE_DELETE:
                    // Receiving this signal means the remote side has received the outbound
                    // Rekey-Create response since no retransmissions were received during the
                    // waiting time. IKE library will assume the remote side has set up the new
                    // Child SA and finish the rekey procedure. Users should be warned there is
                    // a risk that the remote side failed to set up the new Child SA and all
                    // outbound IPsec traffic protected by new Child SA will be dropped.

                    // TODO:Consider finishing rekey procedure if the IKE Session receives a new
                    // request. Since window size is one, receiving a new request indicates the
                    // remote side has received the outbound Rekey-Create response

                    finishRekey();
                    transitionTo(mIdle);
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }

        private void handleDeleteRequest(List<IkePayload> payloads) {
            if (!hasRemoteChildSpiForDelete(payloads, mCurrentChildSaRecord)) {
                // Request received on incorrect SA
                cleanUpAndQuit(
                        new IllegalStateException(
                                "Found no remote SPI for current SA in received Delete"
                                        + " response."));
            } else {
                sendDeleteChild(mCurrentChildSaRecord, true /*isResp*/);
                finishRekey();
                transitionTo(mIdle);
            }
        }

        @Override
        protected void finishRekey() {
            IpSecTransform outTransform = mRemoteInitNewChildSaRecord.getOutboundIpSecTransform();
            executeUserCallback(
                    () -> {
                        mUserCallback.onIpSecTransformCreated(
                                outTransform, IpSecManager.DIRECTION_OUT);
                    });

            super.finishRekey();
        }

        @Override
        public void exitState() {
            removeMessages(TIMEOUT_REKEY_REMOTE_DELETE);
        }
    }

    /**
     * Package private helper class to generate IKE SA creation payloads, in both request and
     * response directions.
     */
    static class CreateChildSaHelper {
        /** Create payload list for creating the initial Child SA for this Child Session. */
        public static List<IkePayload> getInitChildCreateReqPayloads(
                RandomnessFactory randomFactory,
                IpSecSpiGenerator ipSecSpiGenerator,
                InetAddress localAddress,
                ChildSessionParams childSessionParams,
                boolean isFirstChildSa)
                throws SpiUnavailableException, ResourceUnavailableException {

            ChildSaProposal[] saProposals = childSessionParams.getSaProposalsInternal();

            if (isFirstChildSa) {
                for (int i = 0; i < saProposals.length; i++) {
                    saProposals[i] =
                            childSessionParams.getSaProposalsInternal()[i]
                                    .getCopyWithoutDhTransform();
                }
            }

            List<IkePayload> payloadList =
                    getChildCreatePayloads(
                            IkeSaPayload.createChildSaRequestPayload(
                                    saProposals, ipSecSpiGenerator, localAddress),
                            childSessionParams.getInboundTrafficSelectorsInternal(),
                            childSessionParams.getOutboundTrafficSelectorsInternal(),
                            childSessionParams.isTransportMode(),
                            isFirstChildSa,
                            randomFactory);

            return payloadList;
        }

        public static ConfigAttribute[] getConfigAttributes(ChildSessionParams params) {
            if (!params.isTransportMode()) {
                return ((TunnelModeChildSessionParams) params).getConfigurationAttributesInternal();
            }
            return new ConfigAttribute[0];
        }

        /** Create payload list as a rekey Child Session request. */
        public static List<IkePayload> getRekeyChildCreateReqPayloads(
                RandomnessFactory randomFactory,
                IpSecSpiGenerator ipSecSpiGenerator,
                InetAddress localAddress,
                ChildSaProposal currentProposal,
                IkeTrafficSelector[] currentLocalTs,
                IkeTrafficSelector[] currentRemoteTs,
                int localSpi,
                boolean isTransport)
                throws SpiUnavailableException, ResourceUnavailableException {
            List<IkePayload> payloads =
                    getChildCreatePayloads(
                            IkeSaPayload.createChildSaRequestPayload(
                                    new ChildSaProposal[] {currentProposal},
                                    ipSecSpiGenerator,
                                    localAddress),
                            currentLocalTs,
                            currentRemoteTs,
                            isTransport,
                            false /*isFirstChildSa*/,
                            randomFactory);

            payloads.add(
                    new IkeNotifyPayload(
                            PROTOCOL_ID_ESP, localSpi, NOTIFY_TYPE_REKEY_SA, new byte[0]));
            return payloads;
        }

        /** Create payload list as a rekey Child Session response. */
        public static List<IkePayload> getRekeyChildCreateRespPayloads(
                RandomnessFactory randomFactory,
                IpSecSpiGenerator ipSecSpiGenerator,
                InetAddress localAddress,
                byte proposalNumber,
                ChildSaProposal currentProposal,
                IkeTrafficSelector[] currentLocalTs,
                IkeTrafficSelector[] currentRemoteTs,
                int localSpi,
                boolean isTransport)
                throws SpiUnavailableException, ResourceUnavailableException {
            List<IkePayload> payloads =
                    getChildCreatePayloads(
                            IkeSaPayload.createChildSaResponsePayload(
                                    proposalNumber,
                                    currentProposal,
                                    ipSecSpiGenerator,
                                    localAddress),
                            currentRemoteTs /*initTs*/,
                            currentLocalTs /*respTs*/,
                            isTransport,
                            false /*isFirstChildSa*/,
                            randomFactory);

            payloads.add(
                    new IkeNotifyPayload(
                            PROTOCOL_ID_ESP, localSpi, NOTIFY_TYPE_REKEY_SA, new byte[0]));
            return payloads;
        }

        /** Create payload list for creating a new Child SA. */
        private static List<IkePayload> getChildCreatePayloads(
                IkeSaPayload saPayload,
                IkeTrafficSelector[] initTs,
                IkeTrafficSelector[] respTs,
                boolean isTransport,
                boolean isFirstChildSa,
                RandomnessFactory randomFactory)
                throws ResourceUnavailableException {
            List<IkePayload> payloadList = new ArrayList<>(5);

            payloadList.add(saPayload);
            payloadList.add(new IkeTsPayload(true /*isInitiator*/, initTs));
            payloadList.add(new IkeTsPayload(false /*isInitiator*/, respTs));

            if (!isFirstChildSa) {
                payloadList.add(new IkeNoncePayload(randomFactory));
            }

            DhGroupTransform[] dhGroups =
                    ((ChildProposal) saPayload.proposalList.get(0))
                            .saProposal.getDhGroupTransforms();
            if (dhGroups.length != 0 && dhGroups[0].id != DH_GROUP_NONE) {
                payloadList.add(
                        IkeKePayload.createOutboundKePayload(dhGroups[0].id, randomFactory));
            }

            if (isTransport) payloadList.add(new IkeNotifyPayload(NOTIFY_TYPE_USE_TRANSPORT_MODE));

            return payloadList;
        }

        /**
         * Validate the received response of initial Create Child SA exchange and return the
         * negotiation result.
         */
        public static CreateChildResult validateAndNegotiateInitChild(
                List<IkePayload> reqPayloads,
                List<IkePayload> respPayloads,
                @ExchangeType int exchangeType,
                @ExchangeType int expectedExchangeType,
                boolean expectTransport,
                IpSecSpiGenerator ipSecSpiGenerator,
                InetAddress remoteAddress) {

            return validateAndNegotiateChild(
                    reqPayloads,
                    respPayloads,
                    exchangeType,
                    expectedExchangeType,
                    true /*isLocalInit*/,
                    expectTransport,
                    ipSecSpiGenerator,
                    remoteAddress);
        }

        /**
         * Validate the received rekey-create request against locally built response (based on
         * previously negotiated Child SA) and return the negotiation result.
         */
        public static CreateChildResult validateAndNegotiateRekeyChildRequest(
                List<IkePayload> reqPayloads,
                List<IkePayload> respPayloads,
                @ExchangeType int exchangeType,
                @ExchangeType int expectedExchangeType,
                boolean expectTransport,
                IpSecSpiGenerator ipSecSpiGenerator,
                InetAddress remoteAddress) {

            // It is guaranteed that a Rekey-Notify Payload with remote SPI of current Child SA is
            // included in the reqPayloads. So we won't validate it again here.
            return validateAndNegotiateChild(
                    reqPayloads,
                    respPayloads,
                    exchangeType,
                    expectedExchangeType,
                    false /*isLocalInit*/,
                    expectTransport,
                    ipSecSpiGenerator,
                    remoteAddress);
        }

        /**
         * Validate the received rekey-create response against locally built request and previously
         * negotiated Child SA, and return the negotiation result.
         */
        public static CreateChildResult validateAndNegotiateRekeyChildResp(
                List<IkePayload> reqPayloads,
                List<IkePayload> respPayloads,
                @ExchangeType int exchangeType,
                @ExchangeType int expectedExchangeType,
                boolean expectTransport,
                ChildSaRecord expectedChildRecord,
                IpSecSpiGenerator ipSecSpiGenerator,
                InetAddress remoteAddress) {
            // Validate rest of payloads and negotiate Child SA.
            CreateChildResult childResult =
                    validateAndNegotiateChild(
                            reqPayloads,
                            respPayloads,
                            exchangeType,
                            expectedExchangeType,
                            true /*isLocalInit*/,
                            expectTransport,
                            ipSecSpiGenerator,
                            remoteAddress);

            // TODO: Validate new Child SA does not have different Traffic Selectors

            return childResult;
        }

        /**
         * Check if SPI of Child SA that is expected to be rekeyed is included in the provided
         * payload list.
         */
        public static boolean hasRemoteChildSpiForRekey(
                List<IkePayload> payloads, ChildSaRecord expectedRecord) {
            List<IkeNotifyPayload> notifyPayloads =
                    IkePayload.getPayloadListForTypeInProvidedList(
                            IkePayload.PAYLOAD_TYPE_NOTIFY, IkeNotifyPayload.class, payloads);

            boolean hasExpectedRekeyNotify = false;
            for (IkeNotifyPayload notifyPayload : notifyPayloads) {
                if (notifyPayload.notifyType == NOTIFY_TYPE_REKEY_SA
                        && notifyPayload.spi == expectedRecord.getRemoteSpi()) {
                    hasExpectedRekeyNotify = true;
                    break;
                }
            }

            return hasExpectedRekeyNotify;
        }

        public static void releaseSpiResources(List<IkePayload> reqPayloads) {
            if (reqPayloads == null) {
                return;
            }

            IkeSaPayload saPayload =
                    IkePayload.getPayloadForTypeInProvidedList(
                            IkePayload.PAYLOAD_TYPE_SA, IkeSaPayload.class, reqPayloads);
            if (saPayload != null) {
                saPayload.releaseChildSpiResourcesIfExists();
            }
        }

        /** Validate the received payload list and negotiate Child SA. */
        private static CreateChildResult validateAndNegotiateChild(
                List<IkePayload> reqPayloads,
                List<IkePayload> respPayloads,
                @ExchangeType int exchangeType,
                @ExchangeType int expectedExchangeType,
                boolean isLocalInit,
                boolean expectTransport,
                IpSecSpiGenerator ipSecSpiGenerator,
                InetAddress remoteAddress) {
            List<IkePayload> inboundPayloads = isLocalInit ? respPayloads : reqPayloads;

            try {
                validatePayloadAndExchangeType(
                        inboundPayloads,
                        isLocalInit /*isResp*/,
                        exchangeType,
                        expectedExchangeType);
            } catch (InvalidSyntaxException e) {
                return new CreateChildResult(CREATE_STATUS_CHILD_ERROR_INVALID_MSG, e);
            }

            List<IkeNotifyPayload> notifyPayloads =
                    IkePayload.getPayloadListForTypeInProvidedList(
                            IkePayload.PAYLOAD_TYPE_NOTIFY,
                            IkeNotifyPayload.class,
                            inboundPayloads);

            boolean hasTransportNotify = false;
            for (IkeNotifyPayload notify : notifyPayloads) {
                if (notify.isErrorNotify()) {
                    try {
                        IkeProtocolException exception = notify.validateAndBuildIkeException();
                        if (isLocalInit) {
                            return new CreateChildResult(
                                    CREATE_STATUS_CHILD_ERROR_RCV_NOTIFY, exception);
                        } else {
                            logw("Received unexpected error notification: " + notify.notifyType);
                        }
                    } catch (InvalidSyntaxException e) {
                        return new CreateChildResult(CREATE_STATUS_CHILD_ERROR_INVALID_MSG, e);
                    }
                }

                switch (notify.notifyType) {
                    case IkeNotifyPayload.NOTIFY_TYPE_ADDITIONAL_TS_POSSIBLE:
                        // TODO: Store it as part of negotiation results that can be retrieved
                        // by users.
                        break;
                    case IkeNotifyPayload.NOTIFY_TYPE_IPCOMP_SUPPORTED:
                        // Ignore
                        break;
                    case IkeNotifyPayload.NOTIFY_TYPE_USE_TRANSPORT_MODE:
                        hasTransportNotify = true;
                        break;
                    case IkeNotifyPayload.NOTIFY_TYPE_ESP_TFC_PADDING_NOT_SUPPORTED:
                        // Ignore
                        break;
                    case IkeNotifyPayload.NOTIFY_TYPE_REKEY_SA:
                        // Handled in Rekey State. Ignore here.
                        break;
                    default:
                        // Unknown and unexpected status notifications are ignored as per RFC7296.
                        logw(
                                "Received unknown or unexpected status notifications with notify"
                                        + " type: "
                                        + notify.notifyType);
                }
            }

            Pair<ChildProposal, ChildProposal> childProposalPair = null;
            try {
                IkeSaPayload reqSaPayload =
                        IkePayload.getPayloadForTypeInProvidedList(
                                IkePayload.PAYLOAD_TYPE_SA, IkeSaPayload.class, reqPayloads);
                IkeSaPayload respSaPayload =
                        IkePayload.getPayloadForTypeInProvidedList(
                                IkePayload.PAYLOAD_TYPE_SA, IkeSaPayload.class, respPayloads);

                // This method either throws exception or returns non-null pair that contains two
                // valid {@link ChildProposal} both with a {@link SecurityParameterIndex} allocated
                // inside.
                childProposalPair =
                        IkeSaPayload.getVerifiedNegotiatedChildProposalPair(
                                reqSaPayload, respSaPayload, ipSecSpiGenerator, remoteAddress);
                ChildSaProposal saProposal = childProposalPair.second.saProposal;

                validateKePayloads(inboundPayloads, isLocalInit /*isResp*/, saProposal);

                if (expectTransport != hasTransportNotify) {
                    throw new NoValidProposalChosenException(
                            "Failed the negotiation on Child SA mode (conflicting modes chosen).");
                }

                Pair<IkeTrafficSelector[], IkeTrafficSelector[]> tsPair =
                        validateAndGetNegotiatedTsPair(reqPayloads, respPayloads);

                return new CreateChildResult(
                        childProposalPair.first.getChildSpiResource(),
                        childProposalPair.second.getChildSpiResource(),
                        saProposal,
                        tsPair.first,
                        tsPair.second);
            } catch (IkeProtocolException
                    | ResourceUnavailableException
                    | SpiUnavailableException e) {
                if (childProposalPair != null) {
                    childProposalPair.first.getChildSpiResource().close();
                    childProposalPair.second.getChildSpiResource().close();
                }

                if (e instanceof InvalidSyntaxException) {
                    return new CreateChildResult(
                            CREATE_STATUS_CHILD_ERROR_INVALID_MSG, (InvalidSyntaxException) e);
                } else if (e instanceof IkeProtocolException) {
                    return new CreateChildResult(
                            CREATE_STATUS_CHILD_ERROR_INVALID_MSG,
                            new InvalidSyntaxException(
                                    "Processing error in received Create Child response", e));
                } else {
                    return new CreateChildResult(
                            CREATE_STATUS_CHILD_ERROR_INVALID_MSG, wrapAsIkeException(e));
                }
            }
        }

        // Validate syntax to make sure all necessary payloads exist and exchange type is correct.
        private static void validatePayloadAndExchangeType(
                List<IkePayload> inboundPayloads,
                boolean isResp,
                @ExchangeType int exchangeType,
                @ExchangeType int expectedExchangeType)
                throws InvalidSyntaxException {
            boolean hasSaPayload = false;
            boolean hasKePayload = false;
            boolean hasNoncePayload = false;
            boolean hasTsInitPayload = false;
            boolean hasTsRespPayload = false;
            boolean hasErrorNotify = false;

            for (IkePayload payload : inboundPayloads) {
                switch (payload.payloadType) {
                    case PAYLOAD_TYPE_SA:
                        hasSaPayload = true;
                        break;
                    case PAYLOAD_TYPE_KE:
                        // Could not decide if KE Payload MUST or MUST NOT be included until SA
                        // negotiation is done.
                        hasKePayload = true;
                        break;
                    case PAYLOAD_TYPE_NONCE:
                        hasNoncePayload = true;
                        break;
                    case PAYLOAD_TYPE_TS_INITIATOR:
                        hasTsInitPayload = true;
                        break;
                    case PAYLOAD_TYPE_TS_RESPONDER:
                        hasTsRespPayload = true;
                        break;
                    case PAYLOAD_TYPE_NOTIFY:
                        if (((IkeNotifyPayload) payload).isErrorNotify()) hasErrorNotify = true;
                        // Do not have enough context to handle all notifications. Handle them
                        // together in higher layer.
                        break;
                    case PAYLOAD_TYPE_CP:
                        // Handled in child creation state. Note Child Session can only handle
                        // Config Payload in initial creation and can only handle a Config Reply.
                        // For interoperability, Config Payloads received in rekey creation
                        // or with other config types will be ignored.
                        break;
                    default:
                        logw(
                                "Received unexpected payload in Create Child SA message. Payload"
                                        + " type: "
                                        + payload.payloadType);
                }
            }

            // Do not need to check exchange type of a request because it has been already verified
            // in IkeSessionStateMachine
            if (isResp
                    && exchangeType != expectedExchangeType
                    && exchangeType != EXCHANGE_TYPE_INFORMATIONAL) {
                throw new InvalidSyntaxException("Received invalid exchange type: " + exchangeType);
            }

            if (exchangeType == EXCHANGE_TYPE_INFORMATIONAL
                    && (hasSaPayload
                            || hasKePayload
                            || hasNoncePayload
                            || hasTsInitPayload
                            || hasTsRespPayload)) {
                logw(
                        "Unexpected payload found in an INFORMATIONAL message: SA, KE, Nonce,"
                                + " TS-Initiator or TS-Responder");
            }

            if (isResp
                    && !hasErrorNotify
                    && (!hasSaPayload
                            || !hasNoncePayload
                            || !hasTsInitPayload
                            || !hasTsRespPayload)) {
                throw new InvalidSyntaxException(
                        "SA, Nonce, TS-Initiator or TS-Responder missing.");
            }
        }

        private static Pair<IkeTrafficSelector[], IkeTrafficSelector[]>
                validateAndGetNegotiatedTsPair(
                        List<IkePayload> reqPayloads, List<IkePayload> respPayloads)
                        throws TsUnacceptableException {
            IkeTrafficSelector[] initTs =
                    validateAndGetNegotiatedTs(reqPayloads, respPayloads, true /*isInitTs*/);
            IkeTrafficSelector[] respTs =
                    validateAndGetNegotiatedTs(reqPayloads, respPayloads, false /*isInitTs*/);

            return new Pair<IkeTrafficSelector[], IkeTrafficSelector[]>(initTs, respTs);
        }

        private static IkeTrafficSelector[] validateAndGetNegotiatedTs(
                List<IkePayload> reqPayloads, List<IkePayload> respPayloads, boolean isInitTs)
                throws TsUnacceptableException {
            int tsType = isInitTs ? PAYLOAD_TYPE_TS_INITIATOR : PAYLOAD_TYPE_TS_RESPONDER;
            IkeTsPayload reqPayload =
                    IkePayload.getPayloadForTypeInProvidedList(
                            tsType, IkeTsPayload.class, reqPayloads);
            IkeTsPayload respPayload =
                    IkePayload.getPayloadForTypeInProvidedList(
                            tsType, IkeTsPayload.class, respPayloads);

            if (!reqPayload.contains(respPayload)) {
                throw new TsUnacceptableException();
            }

            // It is guaranteed by decoding inbound TS Payload and constructing outbound TS Payload
            // that each TS Payload has at least one IkeTrafficSelector.
            return respPayload.trafficSelectors;
        }

        @VisibleForTesting
        static void validateKePayloads(
                List<IkePayload> inboundPayloads,
                boolean isResp,
                ChildSaProposal negotiatedProposal)
                throws IkeProtocolException {
            DhGroupTransform[] dhTransforms = negotiatedProposal.getDhGroupTransforms();

            if (dhTransforms.length > 1) {
                throw new IllegalArgumentException(
                        "Found multiple DH Group Transforms in the negotiated SA proposal");
            }
            boolean expectKePayload =
                    dhTransforms.length == 1 && dhTransforms[0].id != DH_GROUP_NONE;

            IkeKePayload kePayload =
                    IkePayload.getPayloadForTypeInProvidedList(
                            PAYLOAD_TYPE_KE, IkeKePayload.class, inboundPayloads);

            if (expectKePayload && (kePayload == null || dhTransforms[0].id != kePayload.dhGroup)) {
                if (isResp) {
                    throw new InvalidSyntaxException(
                            "KE Payload missing or has mismatched DH Group with the negotiated"
                                    + " proposal.");
                } else {
                    throw new InvalidKeException(dhTransforms[0].id);
                }

            } else if (!expectKePayload && kePayload != null && isResp) {
                // It is valid when the remote request proposed multiple DH Groups with a KE
                // payload, and the responder chose DH_GROUP_NONE.
                throw new InvalidSyntaxException("Received unexpected KE Payload.");
            }
        }

        private static void logw(String s) {
            getIkeLog().w(TAG, s);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        CREATE_STATUS_OK,
        CREATE_STATUS_CHILD_ERROR_INVALID_MSG,
        CREATE_STATUS_CHILD_ERROR_RCV_NOTIFY
    })
    @interface CreateStatus {}

    /** The Child SA negotiation succeeds. */
    private static final int CREATE_STATUS_OK = 0;
    /** The inbound message is invalid in Child negotiation but is non-fatal for IKE Session. */
    private static final int CREATE_STATUS_CHILD_ERROR_INVALID_MSG = 1;
    /** The inbound message includes error notification that failed the Child negotiation. */
    private static final int CREATE_STATUS_CHILD_ERROR_RCV_NOTIFY = 2;

    private static class CreateChildResult {
        @CreateStatus public final int status;
        public final SecurityParameterIndex initSpi;
        public final SecurityParameterIndex respSpi;
        public final ChildSaProposal negotiatedProposal;
        public final IkeTrafficSelector[] initTs;
        public final IkeTrafficSelector[] respTs;
        public final IkeException exception;

        private CreateChildResult(
                @CreateStatus int status,
                SecurityParameterIndex initSpi,
                SecurityParameterIndex respSpi,
                ChildSaProposal negotiatedProposal,
                IkeTrafficSelector[] initTs,
                IkeTrafficSelector[] respTs,
                IkeException exception) {
            this.status = status;
            this.initSpi = initSpi;
            this.respSpi = respSpi;
            this.negotiatedProposal = negotiatedProposal;
            this.initTs = initTs;
            this.respTs = respTs;
            this.exception = exception;
        }

        /* Construct a CreateChildResult instance for a successful case. */
        CreateChildResult(
                SecurityParameterIndex initSpi,
                SecurityParameterIndex respSpi,
                ChildSaProposal negotiatedProposal,
                IkeTrafficSelector[] initTs,
                IkeTrafficSelector[] respTs) {
            this(
                    CREATE_STATUS_OK,
                    initSpi,
                    respSpi,
                    negotiatedProposal,
                    initTs,
                    respTs,
                    null /*exception*/);
        }

        /** Construct a CreateChildResult instance for an error case. */
        CreateChildResult(@CreateStatus int status, IkeException exception) {
            this(
                    status,
                    null /*initSpi*/,
                    null /*respSpi*/,
                    null /*negotiatedProposal*/,
                    null /*initTs*/,
                    null /*respTs*/,
                    exception);
        }
    }
}
