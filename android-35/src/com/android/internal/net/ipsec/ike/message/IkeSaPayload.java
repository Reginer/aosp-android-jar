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

package com.android.internal.net.ipsec.ike.message;

import static android.net.ipsec.ike.IkeManager.getIkeLog;
import static android.net.ipsec.ike.SaProposal.DhGroup;
import static android.net.ipsec.ike.SaProposal.EncryptionAlgorithm;
import static android.net.ipsec.ike.SaProposal.IntegrityAlgorithm;
import static android.net.ipsec.ike.SaProposal.PseudorandomFunction;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.net.IpSecManager.ResourceUnavailableException;
import android.net.IpSecManager.SecurityParameterIndex;
import android.net.IpSecManager.SpiUnavailableException;
import android.net.ipsec.ike.ChildSaProposal;
import android.net.ipsec.ike.IkeSaProposal;
import android.net.ipsec.ike.SaProposal;
import android.net.ipsec.ike.exceptions.IkeProtocolException;
import android.net.ipsec.ike.exceptions.InvalidKeException;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.net.ipsec.ike.exceptions.NoValidProposalChosenException;
import android.os.PersistableBundle;
import android.util.ArraySet;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.ipsec.ike.utils.IkeSecurityParameterIndex;
import com.android.internal.net.ipsec.ike.utils.IkeSpiGenerator;
import com.android.internal.net.ipsec.ike.utils.IpSecSpiGenerator;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * IkeSaPayload represents a Security Association payload. It contains one or more {@link Proposal}.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeSaPayload extends IkePayload {
    private static final String TAG = "IkeSaPayload";

    public final boolean isSaResponse;
    public final List<Proposal> proposalList;
    /**
     * Construct an instance of IkeSaPayload for decoding an inbound packet.
     *
     * @param critical indicates if this payload is critical. Ignored in supported payload as
     *     instructed by the RFC 7296.
     * @param isResp indicates if this payload is in a response message.
     * @param payloadBody the encoded payload body in byte array.
     */
    IkeSaPayload(boolean critical, boolean isResp, byte[] payloadBody) throws IkeProtocolException {
        super(IkePayload.PAYLOAD_TYPE_SA, critical);

        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);
        proposalList = new LinkedList<>();
        while (inputBuffer.hasRemaining()) {
            Proposal proposal = Proposal.readFrom(inputBuffer);
            proposalList.add(proposal);
        }

        if (proposalList.isEmpty()) {
            throw new InvalidSyntaxException("Found no SA Proposal in this SA Payload.");
        }

        // An SA response must have exactly one SA proposal.
        if (isResp && proposalList.size() != 1) {
            throw new InvalidSyntaxException(
                    "Expected only one negotiated proposal from SA response: "
                            + "Multiple negotiated proposals found.");
        }
        isSaResponse = isResp;

        boolean firstIsIkeProposal = (proposalList.get(0).protocolId == PROTOCOL_ID_IKE);
        for (int i = 1; i < proposalList.size(); i++) {
            boolean isIkeProposal = (proposalList.get(i).protocolId == PROTOCOL_ID_IKE);
            if (firstIsIkeProposal != isIkeProposal) {
                getIkeLog()
                        .w(TAG, "Found both IKE proposals and Child proposals in this SA Payload.");
                break;
            }
        }

        getIkeLog().d(TAG, "Receive " + toString());
    }

    /** Package private constructor for building a request for IKE SA initial creation or rekey */
    @VisibleForTesting
    IkeSaPayload(
            boolean isResp,
            byte spiSize,
            IkeSaProposal[] saProposals,
            IkeSpiGenerator ikeSpiGenerator,
            InetAddress localAddress)
            throws IOException {
        this(isResp, spiSize, localAddress);

        if (saProposals.length < 1 || isResp && (saProposals.length > 1)) {
            throw new IllegalArgumentException("Invalid SA payload.");
        }

        for (int i = 0; i < saProposals.length; i++) {
            // Proposal number must start from 1.
            proposalList.add(
                    IkeProposal.createIkeProposal(
                            (byte) (i + 1) /* number */,
                            spiSize,
                            saProposals[i],
                            ikeSpiGenerator,
                            localAddress));
        }

        getIkeLog().d(TAG, "Generate " + toString());
    }

    /** Package private constructor for building an response SA Payload for IKE SA rekeys. */
    @VisibleForTesting
    IkeSaPayload(
            boolean isResp,
            byte spiSize,
            byte proposalNumber,
            IkeSaProposal saProposal,
            IkeSpiGenerator ikeSpiGenerator,
            InetAddress localAddress)
            throws IOException {
        this(isResp, spiSize, localAddress);

        proposalList.add(
                IkeProposal.createIkeProposal(
                        proposalNumber /* number */,
                        spiSize,
                        saProposal,
                        ikeSpiGenerator,
                        localAddress));

        getIkeLog().d(TAG, "Generate " + toString());
    }

    private IkeSaPayload(boolean isResp, byte spiSize, InetAddress localAddress)
            throws IOException {
        super(IkePayload.PAYLOAD_TYPE_SA, false);

        // TODO: Check that proposals.length <= 255 in IkeSessionParams and ChildSessionParams
        isSaResponse = isResp;

        // TODO: Allocate IKE SPI and pass to IkeProposal.createIkeProposal()

        // ProposalList populated in other constructors
        proposalList = new ArrayList<Proposal>();
    }

    /**
     * Package private constructor for building an outbound request SA Payload for Child SA
     * negotiation.
     */
    @VisibleForTesting
    IkeSaPayload(
            ChildSaProposal[] saProposals,
            IpSecSpiGenerator ipSecSpiGenerator,
            InetAddress localAddress)
            throws SpiUnavailableException, ResourceUnavailableException {
        this(false /* isResp */, ipSecSpiGenerator, localAddress);

        if (saProposals.length < 1) {
            throw new IllegalArgumentException("Invalid SA payload.");
        }

        // TODO: Check that saProposals.length <= 255 in IkeSessionParams and ChildSessionParams

        for (int i = 0; i < saProposals.length; i++) {
            // Proposal number must start from 1.
            proposalList.add(
                    ChildProposal.createChildProposal(
                            (byte) (i + 1) /* number */,
                            saProposals[i],
                            ipSecSpiGenerator,
                            localAddress));
        }

        getIkeLog().d(TAG, "Generate " + toString());
    }

    /**
     * Package private constructor for building an outbound response SA Payload for Child SA
     * negotiation.
     */
    @VisibleForTesting
    IkeSaPayload(
            byte proposalNumber,
            ChildSaProposal saProposal,
            IpSecSpiGenerator ipSecSpiGenerator,
            InetAddress localAddress)
            throws SpiUnavailableException, ResourceUnavailableException {
        this(true /* isResp */, ipSecSpiGenerator, localAddress);

        proposalList.add(
                ChildProposal.createChildProposal(
                        proposalNumber /* number */, saProposal, ipSecSpiGenerator, localAddress));

        getIkeLog().d(TAG, "Generate " + toString());
    }

    /** Constructor for building an outbound SA Payload for Child SA negotiation. */
    private IkeSaPayload(
            boolean isResp, IpSecSpiGenerator ipSecSpiGenerator, InetAddress localAddress) {
        super(IkePayload.PAYLOAD_TYPE_SA, false);

        isSaResponse = isResp;

        // TODO: Allocate Child SPI and pass to ChildProposal.createChildProposal()

        // ProposalList populated in other constructors
        proposalList = new ArrayList<Proposal>();
    }

    /**
     * Construct an instance of IkeSaPayload for building an outbound IKE initial setup request.
     *
     * <p>According to RFC 7296, for an initial IKE SA negotiation, no SPI is included in SA
     * Proposal. IKE library, as a client, only supports requesting this initial negotiation.
     *
     * @param saProposals the array of all SA Proposals.
     */
    public static IkeSaPayload createInitialIkeSaPayload(IkeSaProposal[] saProposals)
            throws IOException {
        return new IkeSaPayload(
                false /* isResp */,
                SPI_LEN_NOT_INCLUDED,
                saProposals,
                null /* ikeSpiGenerator unused */,
                null /* localAddress unused */);
    }

    /**
     * Construct an instance of IkeSaPayload for building an outbound request for Rekey IKE.
     *
     * @param saProposals the array of all IKE SA Proposals.
     * @param ikeSpiGenerator the IKE SPI generator.
     * @param localAddress the local address assigned on-device.
     */
    public static IkeSaPayload createRekeyIkeSaRequestPayload(
            IkeSaProposal[] saProposals, IkeSpiGenerator ikeSpiGenerator, InetAddress localAddress)
            throws IOException {
        return new IkeSaPayload(
                false /* isResp */, SPI_LEN_IKE, saProposals, ikeSpiGenerator, localAddress);
    }

    /**
     * Construct an instance of IkeSaPayload for building an outbound response for Rekey IKE.
     *
     * @param respProposalNumber the selected proposal's number.
     * @param saProposal the expected selected IKE SA Proposal.
     * @param ikeSpiGenerator the IKE SPI generator.
     * @param localAddress the local address assigned on-device.
     */
    public static IkeSaPayload createRekeyIkeSaResponsePayload(
            byte respProposalNumber,
            IkeSaProposal saProposal,
            IkeSpiGenerator ikeSpiGenerator,
            InetAddress localAddress)
            throws IOException {
        return new IkeSaPayload(
                true /* isResp */,
                SPI_LEN_IKE,
                respProposalNumber,
                saProposal,
                ikeSpiGenerator,
                localAddress);
    }

    /**
     * Construct an instance of IkeSaPayload for building an outbound request for Child SA
     * negotiation.
     *
     * @param saProposals the array of all Child SA Proposals.
     * @param ipSecSpiGenerator the IPsec SPI generator.
     * @param localAddress the local address assigned on-device.
     * @throws ResourceUnavailableException if too many SPIs are currently allocated for this user.
     */
    public static IkeSaPayload createChildSaRequestPayload(
            ChildSaProposal[] saProposals,
            IpSecSpiGenerator ipSecSpiGenerator,
            InetAddress localAddress)
            throws SpiUnavailableException, ResourceUnavailableException {

        return new IkeSaPayload(saProposals, ipSecSpiGenerator, localAddress);
    }

    /**
     * Construct an instance of IkeSaPayload for building an outbound response for Child SA
     * negotiation.
     *
     * @param respProposalNumber the selected proposal's number.
     * @param saProposal the expected selected Child SA Proposal.
     * @param ipSecSpiGenerator the IPsec SPI generator.
     * @param localAddress the local address assigned on-device.
     */
    public static IkeSaPayload createChildSaResponsePayload(
            byte respProposalNumber,
            ChildSaProposal saProposal,
            IpSecSpiGenerator ipSecSpiGenerator,
            InetAddress localAddress)
            throws SpiUnavailableException, ResourceUnavailableException {
        return new IkeSaPayload(respProposalNumber, saProposal, ipSecSpiGenerator, localAddress);
    }

    /**
     * Finds the proposal in this (request) payload that matches the response proposal.
     *
     * @param respProposal the Proposal to match against.
     * @return the byte-value proposal number of the selected proposal
     * @throws NoValidProposalChosenException if no matching proposal was found.
     */
    public byte getNegotiatedProposalNumber(SaProposal respProposal)
            throws NoValidProposalChosenException {
        for (int i = 0; i < proposalList.size(); i++) {
            Proposal reqProposal = proposalList.get(i);
            if (respProposal.isNegotiatedFrom(reqProposal.getSaProposal())
                    && reqProposal.getSaProposal().getProtocolId()
                            == respProposal.getProtocolId()) {
                return reqProposal.number;
            }
        }
        throw new NoValidProposalChosenException("No remotely proposed protocol acceptable");
    }

    /**
     * Finds or builds the negotiated Child proposal when there is a key exchange.
     *
     * <p>This method will be used in Remote Rekey Child. For better interoperability, IKE library
     * allows the server to set up new Child SA with a different DH group if (1) caller has
     * configured that DH group in the Child SA Proposal, or (2) that DH group is the DH group
     * negotiated as part of IKE Session.
     *
     * @param currentProposal the current negotiated Child SA Proposal
     * @param callerConfiguredProposals all caller configured Child SA Proposals
     * @param reqKePayloadDh the DH group in the request KE payload
     * @param ikeDh the DH group negotiated as part of IKE Session
     * @return the negotiated Child SA Proposal
     * @throws NoValidProposalChosenException when there is no acceptable proposal in the SA payload
     * @throws InvalidKeException when the request KE payload has a mismatched DH group
     */
    public ChildSaProposal getNegotiatedChildProposalWithDh(
            ChildSaProposal currentProposal,
            List<ChildSaProposal> callerConfiguredProposals,
            int reqKePayloadDh,
            int ikeDh)
            throws NoValidProposalChosenException, InvalidKeException {

        List<ChildSaProposal> proposalCandidates = new ArrayList<>();
        for (ChildSaProposal callerProposal : callerConfiguredProposals) {
            // Check if current proposal can be negotiated from the callerProposal.
            if (!currentProposal.isNegotiatedFromExceptDhGroup(callerProposal)) {
                continue;
            }

            // Check if current proposal can be negotiated from the Rekey Child request.
            // Try all DH groups in this caller configured proposal and see if current
            // proposal + the DH group can be negotiated from the Rekey request. For
            // better interoperability, if caller does not configure any DH group for
            // this proposal, try DH group negotiated as part of IKE Session. Some
            // implementation will request using the IKE DH group when rekeying the
            // Child SA which is built during IKE Auth
            if (callerProposal.getDhGroups().isEmpty()) {
                callerProposal = callerProposal.getCopyWithAdditionalDhTransform(ikeDh);
            }

            for (int callerDh : callerProposal.getDhGroups()) {
                ChildSaProposal negotiatedProposal =
                        currentProposal.getCopyWithAdditionalDhTransform(callerDh);
                try {
                    getNegotiatedProposalNumber(negotiatedProposal);
                    proposalCandidates.add(negotiatedProposal);
                } catch (NoValidProposalChosenException e) {
                    continue;
                }
            }
        }

        // Check if any negotiated proposal match reqKePayloadDh
        if (proposalCandidates.isEmpty()) {
            throw new NoValidProposalChosenException("No acceptable SA proposal in the request");
        } else {
            for (ChildSaProposal negotiatedProposal : proposalCandidates) {
                if (reqKePayloadDh == negotiatedProposal.getDhGroups().get(0)) {
                    return negotiatedProposal;
                }
            }
            throw new InvalidKeException(proposalCandidates.get(0).getDhGroups().get(0));
        }
    }

    /**
     * Validate the IKE SA Payload pair (request/response) and return the IKE SA negotiation result.
     *
     * <p>Caller is able to extract the negotiated IKE SA Proposal from the response Proposal and
     * the IKE SPI pair generated by both sides.
     *
     * <p>In a locally-initiated case all IKE SA proposals (from users in initial creation or from
     * previously negotiated proposal in rekey creation) in the locally generated reqSaPayload have
     * been validated during building and are unmodified. All Transform combinations in these SA
     * proposals are valid for IKE SA negotiation. It means each IKE SA request proposal MUST have
     * Encryption algorithms, DH group configurations and PRFs. Integrity algorithms can only be
     * omitted when AEAD is used.
     *
     * <p>In a remotely-initiated case the locally generated respSaPayload has exactly one SA
     * proposal. It is validated during building and are unmodified. This proposal has a valid
     * Transform combination for an IKE SA and has at most one value for each Transform type.
     *
     * <p>The response IKE SA proposal is validated against one of the request IKE SA proposals. It
     * is guaranteed that for each Transform type that the request proposal has provided options,
     * the response proposal has exact one Transform value.
     *
     * @param reqSaPayload the request payload.
     * @param respSaPayload the response payload.
     * @param remoteAddress the address of the remote IKE peer.
     * @return the Pair of selected IkeProposal in request and the IkeProposal in response.
     * @throws NoValidProposalChosenException if the response SA Payload cannot be negotiated from
     *     the request SA Payload.
     */
    public static Pair<IkeProposal, IkeProposal> getVerifiedNegotiatedIkeProposalPair(
            IkeSaPayload reqSaPayload,
            IkeSaPayload respSaPayload,
            IkeSpiGenerator ikeSpiGenerator,
            InetAddress remoteAddress)
            throws NoValidProposalChosenException, IOException {
        Pair<Proposal, Proposal> proposalPair =
                getVerifiedNegotiatedProposalPair(reqSaPayload, respSaPayload);
        IkeProposal reqProposal = (IkeProposal) proposalPair.first;
        IkeProposal respProposal = (IkeProposal) proposalPair.second;

        try {
            // Allocate initiator's inbound SPI as needed for remotely initiated IKE SA creation
            if (reqProposal.spiSize != SPI_NOT_INCLUDED
                    && reqProposal.getIkeSpiResource() == null) {
                reqProposal.allocateResourceForRemoteIkeSpi(ikeSpiGenerator, remoteAddress);
            }
            // Allocate responder's inbound SPI as needed for locally initiated IKE SA creation
            if (respProposal.spiSize != SPI_NOT_INCLUDED
                    && respProposal.getIkeSpiResource() == null) {
                respProposal.allocateResourceForRemoteIkeSpi(ikeSpiGenerator, remoteAddress);
            }

            return new Pair(reqProposal, respProposal);
        } catch (Exception e) {
            reqProposal.releaseSpiResourceIfExists();
            respProposal.releaseSpiResourceIfExists();
            throw e;
        }
    }

    /**
     * Validate the SA Payload pair (request/response) and return the Child SA negotiation result.
     *
     * <p>Caller is able to extract the negotiated SA Proposal from the response Proposal and the
     * IPsec SPI pair generated by both sides.
     *
     * <p>In a locally-initiated case all Child SA proposals (from users in initial creation or from
     * previously negotiated proposal in rekey creation) in the locally generated reqSaPayload have
     * been validated during building and are unmodified. All Transform combinations in these SA
     * proposals are valid for Child SA negotiation. It means each request SA proposal MUST have
     * Encryption algorithms and ESN configurations.
     *
     * <p>In a remotely-initiated case the locally generated respSapayload has exactly one SA
     * proposal. It is validated during building and are unmodified. This proposal has a valid
     * Transform combination for an Child SA and has at most one value for each Transform type.
     *
     * <p>The response Child SA proposal is validated against one of the request SA proposals. It is
     * guaranteed that for each Transform type that the request proposal has provided options, the
     * response proposal has exact one Transform value.
     *
     * @param reqSaPayload the request payload.
     * @param respSaPayload the response payload.
     * @param ipSecSpiGenerator the SPI generator to allocate SPI resource for the Proposal in this
     *     inbound SA Payload.
     * @param remoteAddress the address of the remote IKE peer.
     * @return the Pair of selected ChildProposal in the locally generated request and the
     *     ChildProposal in this response.
     * @throws NoValidProposalChosenException if the response SA Payload cannot be negotiated from
     *     the request SA Payload.
     * @throws ResourceUnavailableException if too many SPIs are currently allocated for this user.
     * @throws SpiUnavailableException if the remotely generated SPI is in use.
     */
    public static Pair<ChildProposal, ChildProposal> getVerifiedNegotiatedChildProposalPair(
            IkeSaPayload reqSaPayload,
            IkeSaPayload respSaPayload,
            IpSecSpiGenerator ipSecSpiGenerator,
            InetAddress remoteAddress)
            throws NoValidProposalChosenException, ResourceUnavailableException,
                    SpiUnavailableException {
        Pair<Proposal, Proposal> proposalPair =
                getVerifiedNegotiatedProposalPair(reqSaPayload, respSaPayload);
        ChildProposal reqProposal = (ChildProposal) proposalPair.first;
        ChildProposal respProposal = (ChildProposal) proposalPair.second;

        try {
            // Allocate initiator's inbound SPI as needed for remotely initiated Child SA creation
            if (reqProposal.getChildSpiResource() == null) {
                reqProposal.allocateResourceForRemoteChildSpi(ipSecSpiGenerator, remoteAddress);
            }
            // Allocate responder's inbound SPI as needed for locally initiated Child SA creation
            if (respProposal.getChildSpiResource() == null) {
                respProposal.allocateResourceForRemoteChildSpi(ipSecSpiGenerator, remoteAddress);
            }

            return new Pair(reqProposal, respProposal);
        } catch (Exception e) {
            reqProposal.releaseSpiResourceIfExists();
            respProposal.releaseSpiResourceIfExists();
            throw e;
        }
    }

    private static Pair<Proposal, Proposal> getVerifiedNegotiatedProposalPair(
            IkeSaPayload reqSaPayload, IkeSaPayload respSaPayload)
            throws NoValidProposalChosenException {
        try {
            // If negotiated proposal has an unrecognized Transform, throw an exception.
            Proposal respProposal = respSaPayload.proposalList.get(0);
            if (respProposal.hasUnrecognizedTransform) {
                throw new NoValidProposalChosenException(
                        "Negotiated proposal has unrecognized Transform.");
            }

            // In SA request payload, the first proposal MUST be 1, and subsequent proposals MUST be
            // one more than the previous proposal. In SA response payload, the negotiated proposal
            // number MUST match the selected proposal number in SA request Payload.
            int negotiatedProposalNum = respProposal.number;
            List<Proposal> reqProposalList = reqSaPayload.proposalList;
            if (negotiatedProposalNum < 1 || negotiatedProposalNum > reqProposalList.size()) {
                throw new NoValidProposalChosenException(
                        "Negotiated proposal has invalid proposal number.");
            }

            Proposal reqProposal = reqProposalList.get(negotiatedProposalNum - 1);
            if (!respProposal.isNegotiatedFrom(reqProposal)) {
                throw new NoValidProposalChosenException("Invalid negotiated proposal.");
            }

            // In a locally-initiated creation, release locally generated SPIs in unselected request
            // Proposals. In remotely-initiated SA creation, unused proposals do not have SPIs, and
            // will silently succeed.
            for (Proposal p : reqProposalList) {
                if (reqProposal != p) p.releaseSpiResourceIfExists();
            }

            return new Pair<Proposal, Proposal>(reqProposal, respProposal);
        } catch (Exception e) {
            // In a locally-initiated case, release all locally generated SPIs in the SA request
            // payload.
            for (Proposal p : reqSaPayload.proposalList) p.releaseSpiResourceIfExists();
            throw e;
        }
    }

    @VisibleForTesting
    interface TransformDecoder {
        Transform[] decodeTransforms(int count, ByteBuffer inputBuffer) throws IkeProtocolException;
    }

    /**
     * Release IPsec SPI resources in the outbound Create Child request
     *
     * <p>This method is usually called when an IKE library fails to receive a Create Child response
     * before it is terminated. It is also safe to call after the Create Child exchange has
     * succeeded because the newly created IpSecTransform pair will hold the IPsec SPI resource.
     */
    public void releaseChildSpiResourcesIfExists() {
        for (Proposal proposal : proposalList) {
            if (proposal instanceof ChildProposal) {
                proposal.releaseSpiResourceIfExists();
            }
        }
    }

    /**
     * This class represents the common information of an IKE Proposal and a Child Proposal.
     *
     * <p>Proposal represents a set contains cryptographic algorithms and key generating materials.
     * It contains multiple {@link Transform}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.1">RFC 7296, Internet Key
     *     Exchange Protocol Version 2 (IKEv2)</a>
     *     <p>Proposals with an unrecognized Protocol ID, containing an unrecognized Transform Type
     *     or lacking a necessary Transform Type shall be ignored when processing a received SA
     *     Payload.
     */
    public abstract static class Proposal {
        private static final byte LAST_PROPOSAL = 0;
        private static final byte NOT_LAST_PROPOSAL = 2;

        private static final int PROPOSAL_RESERVED_FIELD_LEN = 1;
        private static final int PROPOSAL_HEADER_LEN = 8;

        private static TransformDecoder sTransformDecoder = new TransformDecoderImpl();

        public final byte number;
        /** All supported protocol will fall into {@link ProtocolId} */
        public final int protocolId;

        public final byte spiSize;
        public final long spi;

        public final boolean hasUnrecognizedTransform;

        @VisibleForTesting
        Proposal(
                byte number,
                int protocolId,
                byte spiSize,
                long spi,
                boolean hasUnrecognizedTransform) {
            this.number = number;
            this.protocolId = protocolId;
            this.spiSize = spiSize;
            this.spi = spi;
            this.hasUnrecognizedTransform = hasUnrecognizedTransform;
        }

        @VisibleForTesting
        static Proposal readFrom(ByteBuffer inputBuffer) throws IkeProtocolException {
            byte isLast = inputBuffer.get();
            if (isLast != LAST_PROPOSAL && isLast != NOT_LAST_PROPOSAL) {
                throw new InvalidSyntaxException(
                        "Invalid value of Last Proposal Substructure: " + isLast);
            }
            // Skip RESERVED byte
            inputBuffer.get(new byte[PROPOSAL_RESERVED_FIELD_LEN]);

            int length = Short.toUnsignedInt(inputBuffer.getShort());
            byte number = inputBuffer.get();
            int protocolId = Byte.toUnsignedInt(inputBuffer.get());

            byte spiSize = inputBuffer.get();
            int transformCount = Byte.toUnsignedInt(inputBuffer.get());

            // TODO: Add check: spiSize must be 0 in initial IKE SA negotiation
            // spiSize should be either 8 for IKE or 4 for IPsec.
            long spi = SPI_NOT_INCLUDED;
            switch (spiSize) {
                case SPI_LEN_NOT_INCLUDED:
                    // No SPI attached for IKE initial exchange.
                    break;
                case SPI_LEN_IPSEC:
                    spi = Integer.toUnsignedLong(inputBuffer.getInt());
                    break;
                case SPI_LEN_IKE:
                    spi = inputBuffer.getLong();
                    break;
                default:
                    throw new InvalidSyntaxException(
                            "Invalid value of spiSize in Proposal Substructure: " + spiSize);
            }

            Transform[] transformArray =
                    sTransformDecoder.decodeTransforms(transformCount, inputBuffer);
            // TODO: Validate that sum of all Transforms' lengths plus Proposal header length equals
            // to Proposal's length.

            List<EncryptionTransform> encryptAlgoList = new LinkedList<>();
            List<PrfTransform> prfList = new LinkedList<>();
            List<IntegrityTransform> integAlgoList = new LinkedList<>();
            List<DhGroupTransform> dhGroupList = new LinkedList<>();
            List<EsnTransform> esnList = new LinkedList<>();

            boolean hasUnrecognizedTransform = false;

            for (Transform transform : transformArray) {
                switch (transform.type) {
                    case Transform.TRANSFORM_TYPE_ENCR:
                        encryptAlgoList.add((EncryptionTransform) transform);
                        break;
                    case Transform.TRANSFORM_TYPE_PRF:
                        prfList.add((PrfTransform) transform);
                        break;
                    case Transform.TRANSFORM_TYPE_INTEG:
                        integAlgoList.add((IntegrityTransform) transform);
                        break;
                    case Transform.TRANSFORM_TYPE_DH:
                        dhGroupList.add((DhGroupTransform) transform);
                        break;
                    case Transform.TRANSFORM_TYPE_ESN:
                        esnList.add((EsnTransform) transform);
                        break;
                    default:
                        hasUnrecognizedTransform = true;
                }
            }

            if (protocolId == PROTOCOL_ID_IKE) {
                IkeSaProposal saProposal =
                        new IkeSaProposal(
                                encryptAlgoList.toArray(
                                        new EncryptionTransform[encryptAlgoList.size()]),
                                prfList.toArray(new PrfTransform[prfList.size()]),
                                integAlgoList.toArray(new IntegrityTransform[integAlgoList.size()]),
                                dhGroupList.toArray(new DhGroupTransform[dhGroupList.size()]));
                return new IkeProposal(number, spiSize, spi, saProposal, hasUnrecognizedTransform);
            } else {
                ChildSaProposal saProposal =
                        new ChildSaProposal(
                                encryptAlgoList.toArray(
                                        new EncryptionTransform[encryptAlgoList.size()]),
                                integAlgoList.toArray(new IntegrityTransform[integAlgoList.size()]),
                                dhGroupList.toArray(new DhGroupTransform[dhGroupList.size()]),
                                esnList.toArray(new EsnTransform[esnList.size()]));
                return new ChildProposal(number, spi, saProposal, hasUnrecognizedTransform);
            }
        }

        private static class TransformDecoderImpl implements TransformDecoder {
            @Override
            public Transform[] decodeTransforms(int count, ByteBuffer inputBuffer)
                    throws IkeProtocolException {
                Transform[] transformArray = new Transform[count];
                for (int i = 0; i < count; i++) {
                    Transform transform = Transform.readFrom(inputBuffer);
                    transformArray[i] = transform;
                }
                return transformArray;
            }
        }

        /** Package private method to set TransformDecoder for testing purposes */
        @VisibleForTesting
        static void setTransformDecoder(TransformDecoder decoder) {
            sTransformDecoder = decoder;
        }

        /** Package private method to reset TransformDecoder */
        @VisibleForTesting
        static void resetTransformDecoder() {
            sTransformDecoder = new TransformDecoderImpl();
        }

        /** Package private */
        boolean isNegotiatedFrom(Proposal reqProposal) {
            if (protocolId != reqProposal.protocolId || number != reqProposal.number) {
                return false;
            }
            return getSaProposal().isNegotiatedFrom(reqProposal.getSaProposal());
        }

        protected void encodeToByteBuffer(boolean isLast, ByteBuffer byteBuffer) {
            Transform[] allTransforms = getSaProposal().getAllTransforms();
            byte isLastIndicator = isLast ? LAST_PROPOSAL : NOT_LAST_PROPOSAL;

            byteBuffer
                    .put(isLastIndicator)
                    .put(new byte[PROPOSAL_RESERVED_FIELD_LEN])
                    .putShort((short) getProposalLength())
                    .put(number)
                    .put((byte) protocolId)
                    .put(spiSize)
                    .put((byte) allTransforms.length);

            switch (spiSize) {
                case SPI_LEN_NOT_INCLUDED:
                    // No SPI attached for IKE initial exchange.
                    break;
                case SPI_LEN_IPSEC:
                    byteBuffer.putInt((int) spi);
                    break;
                case SPI_LEN_IKE:
                    byteBuffer.putLong((long) spi);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid value of spiSize in Proposal Substructure: " + spiSize);
            }

            // Encode all Transform.
            for (int i = 0; i < allTransforms.length; i++) {
                // The last transform has the isLast flag set to true.
                allTransforms[i].encodeToByteBuffer(i == allTransforms.length - 1, byteBuffer);
            }
        }

        protected int getProposalLength() {
            int len = PROPOSAL_HEADER_LEN + spiSize;

            Transform[] allTransforms = getSaProposal().getAllTransforms();
            for (Transform t : allTransforms) len += t.getTransformLength();
            return len;
        }

        @Override
        @NonNull
        public String toString() {
            return "Proposal(" + number + ") " + getSaProposal().toString();
        }

        /** Package private method for releasing SPI resource in this unselected Proposal. */
        abstract void releaseSpiResourceIfExists();

        /** Package private method for getting SaProposal */
        abstract SaProposal getSaProposal();
    }

    /** This class represents a Proposal for IKE SA negotiation. */
    public static final class IkeProposal extends Proposal {
        private IkeSecurityParameterIndex mIkeSpiResource;

        public final IkeSaProposal saProposal;

        /**
         * Construct IkeProposal from a decoded inbound message for IKE negotiation.
         *
         * <p>Package private
         */
        IkeProposal(
                byte number,
                byte spiSize,
                long spi,
                IkeSaProposal saProposal,
                boolean hasUnrecognizedTransform) {
            super(number, PROTOCOL_ID_IKE, spiSize, spi, hasUnrecognizedTransform);
            this.saProposal = saProposal;
        }

        /** Construct IkeProposal for an outbound message for IKE negotiation. */
        private IkeProposal(
                byte number,
                byte spiSize,
                IkeSecurityParameterIndex ikeSpiResource,
                IkeSaProposal saProposal) {
            super(
                    number,
                    PROTOCOL_ID_IKE,
                    spiSize,
                    ikeSpiResource == null ? SPI_NOT_INCLUDED : ikeSpiResource.getSpi(),
                    false /* hasUnrecognizedTransform */);
            mIkeSpiResource = ikeSpiResource;
            this.saProposal = saProposal;
        }

        /**
         * Construct IkeProposal for an outbound message for IKE negotiation.
         *
         * <p>Package private
         */
        @VisibleForTesting
        static IkeProposal createIkeProposal(
                byte number,
                byte spiSize,
                IkeSaProposal saProposal,
                IkeSpiGenerator ikeSpiGenerator,
                InetAddress localAddress)
                throws IOException {
            // IKE_INIT uses SPI_LEN_NOT_INCLUDED, while rekeys use SPI_LEN_IKE
            IkeSecurityParameterIndex spiResource =
                    (spiSize == SPI_LEN_NOT_INCLUDED
                            ? null
                            : ikeSpiGenerator.allocateSpi(localAddress));
            return new IkeProposal(number, spiSize, spiResource, saProposal);
        }

        /** Package private method for releasing SPI resource in this unselected Proposal. */
        void releaseSpiResourceIfExists() {
            // mIkeSpiResource is null when doing IKE initial exchanges.
            if (mIkeSpiResource == null) return;
            mIkeSpiResource.close();
            mIkeSpiResource = null;
        }

        /**
         * Package private method for allocating SPI resource for a validated remotely generated IKE
         * SA proposal.
         */
        void allocateResourceForRemoteIkeSpi(
                IkeSpiGenerator ikeSpiGenerator, InetAddress remoteAddress) throws IOException {
            mIkeSpiResource = ikeSpiGenerator.allocateSpi(remoteAddress, spi);
        }

        @Override
        public SaProposal getSaProposal() {
            return saProposal;
        }

        /**
         * Get the IKE SPI resource.
         *
         * @return the IKE SPI resource or null for IKE initial exchanges.
         */
        public IkeSecurityParameterIndex getIkeSpiResource() {
            return mIkeSpiResource;
        }
    }

    /** This class represents a Proposal for Child SA negotiation. */
    public static final class ChildProposal extends Proposal {
        private SecurityParameterIndex mChildSpiResource;

        public final ChildSaProposal saProposal;

        /**
         * Construct ChildProposal from a decoded inbound message for Child SA negotiation.
         *
         * <p>Package private
         */
        ChildProposal(
                byte number,
                long spi,
                ChildSaProposal saProposal,
                boolean hasUnrecognizedTransform) {
            super(
                    number,
                    PROTOCOL_ID_ESP,
                    SPI_LEN_IPSEC,
                    spi,
                    hasUnrecognizedTransform);
            this.saProposal = saProposal;
        }

        /** Construct ChildProposal for an outbound message for Child SA negotiation. */
        private ChildProposal(
                byte number, SecurityParameterIndex childSpiResource, ChildSaProposal saProposal) {
            super(
                    number,
                    PROTOCOL_ID_ESP,
                    SPI_LEN_IPSEC,
                    (long) childSpiResource.getSpi(),
                    false /* hasUnrecognizedTransform */);
            mChildSpiResource = childSpiResource;
            this.saProposal = saProposal;
        }

        /**
         * Construct ChildProposal for an outbound message for Child SA negotiation.
         *
         * <p>Package private
         */
        @VisibleForTesting
        static ChildProposal createChildProposal(
                byte number,
                ChildSaProposal saProposal,
                IpSecSpiGenerator ipSecSpiGenerator,
                InetAddress localAddress)
                throws SpiUnavailableException, ResourceUnavailableException {
            return new ChildProposal(
                    number, ipSecSpiGenerator.allocateSpi(localAddress), saProposal);
        }

        /** Package private method for releasing SPI resource in this unselected Proposal. */
        void releaseSpiResourceIfExists() {
            if (mChildSpiResource ==  null) return;

            mChildSpiResource.close();
            mChildSpiResource = null;
        }

        /**
         * Package private method for allocating SPI resource for a validated remotely generated
         * Child SA proposal.
         */
        void allocateResourceForRemoteChildSpi(
                IpSecSpiGenerator ipSecSpiGenerator, InetAddress remoteAddress)
                throws ResourceUnavailableException, SpiUnavailableException {
            mChildSpiResource = ipSecSpiGenerator.allocateSpi(remoteAddress, (int) spi);
        }

        @Override
        public SaProposal getSaProposal() {
            return saProposal;
        }

        /**
         * Get the IPsec SPI resource.
         *
         * @return the IPsec SPI resource.
         */
        public SecurityParameterIndex getChildSpiResource() {
            return mChildSpiResource;
        }
    }

    @VisibleForTesting
    interface AttributeDecoder {
        List<Attribute> decodeAttributes(int length, ByteBuffer inputBuffer)
                throws IkeProtocolException;
    }

    /**
     * Transform is an abstract base class that represents the common information for all Transform
     * types. It may contain one or more {@link Attribute}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key
     *     Exchange Protocol Version 2 (IKEv2)</a>
     *     <p>Transforms with unrecognized Transform ID or containing unrecognized Attribute Type
     *     shall be ignored when processing received SA payload.
     */
    public abstract static class Transform {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
            TRANSFORM_TYPE_ENCR,
            TRANSFORM_TYPE_PRF,
            TRANSFORM_TYPE_INTEG,
            TRANSFORM_TYPE_DH,
            TRANSFORM_TYPE_ESN
        })
        public @interface TransformType {}

        public static final int TRANSFORM_TYPE_ENCR = 1;
        public static final int TRANSFORM_TYPE_PRF = 2;
        public static final int TRANSFORM_TYPE_INTEG = 3;
        public static final int TRANSFORM_TYPE_DH = 4;
        public static final int TRANSFORM_TYPE_ESN = 5;

        private static final byte LAST_TRANSFORM = 0;
        private static final byte NOT_LAST_TRANSFORM = 3;

        // Length of reserved field of a Transform.
        private static final int TRANSFORM_RESERVED_FIELD_LEN = 1;

        // Length of the Transform that with no Attribute.
        protected static final int BASIC_TRANSFORM_LEN = 8;

        // TODO: Add constants for supported algorithms

        private static AttributeDecoder sAttributeDecoder = new AttributeDecoderImpl();

        // Only supported type falls into {@link TransformType}
        public final int type;
        public final int id;
        public final boolean isSupported;

        /** Construct an instance of Transform for building an outbound packet. */
        protected Transform(int type, int id) {
            this.type = type;
            this.id = id;
            if (!isSupportedTransformId(id)) {
                throw new IllegalArgumentException(
                        "Unsupported " + getTransformTypeString() + " Algorithm ID: " + id);
            }
            this.isSupported = true;
        }

        /** Construct an instance of Transform for decoding an inbound packet. */
        protected Transform(int type, int id, List<Attribute> attributeList) {
            this.type = type;
            this.id = id;
            this.isSupported =
                    isSupportedTransformId(id) && !hasUnrecognizedAttribute(attributeList);
        }

        @VisibleForTesting
        static Transform readFrom(ByteBuffer inputBuffer) throws IkeProtocolException {
            byte isLast = inputBuffer.get();
            if (isLast != LAST_TRANSFORM && isLast != NOT_LAST_TRANSFORM) {
                throw new InvalidSyntaxException(
                        "Invalid value of Last Transform Substructure: " + isLast);
            }

            // Skip RESERVED byte
            inputBuffer.get(new byte[TRANSFORM_RESERVED_FIELD_LEN]);

            int length = Short.toUnsignedInt(inputBuffer.getShort());
            int type = Byte.toUnsignedInt(inputBuffer.get());

            // Skip RESERVED byte
            inputBuffer.get(new byte[TRANSFORM_RESERVED_FIELD_LEN]);

            int id = Short.toUnsignedInt(inputBuffer.getShort());

            // Decode attributes
            List<Attribute> attributeList = sAttributeDecoder.decodeAttributes(length, inputBuffer);

            validateAttributeUniqueness(attributeList);

            switch (type) {
                case TRANSFORM_TYPE_ENCR:
                    return new EncryptionTransform(id, attributeList);
                case TRANSFORM_TYPE_PRF:
                    return new PrfTransform(id, attributeList);
                case TRANSFORM_TYPE_INTEG:
                    return new IntegrityTransform(id, attributeList);
                case TRANSFORM_TYPE_DH:
                    return new DhGroupTransform(id, attributeList);
                case TRANSFORM_TYPE_ESN:
                    return new EsnTransform(id, attributeList);
                default:
                    return new UnrecognizedTransform(type, id, attributeList);
            }
        }

        private static class AttributeDecoderImpl implements AttributeDecoder {
            @Override
            public List<Attribute> decodeAttributes(int length, ByteBuffer inputBuffer)
                    throws IkeProtocolException {
                List<Attribute> list = new LinkedList<>();
                int parsedLength = BASIC_TRANSFORM_LEN;
                while (parsedLength < length) {
                    Pair<Attribute, Integer> pair = Attribute.readFrom(inputBuffer);
                    parsedLength += pair.second; // Increase parsedLength by the Atrribute length
                    list.add(pair.first);
                }
                // TODO: Validate that parsedLength equals to length.
                return list;
            }
        }

        /** Package private method to set AttributeDecoder for testing purpose */
        @VisibleForTesting
        static void setAttributeDecoder(AttributeDecoder decoder) {
            sAttributeDecoder = decoder;
        }

        /** Package private method to reset AttributeDecoder */
        @VisibleForTesting
        static void resetAttributeDecoder() {
            sAttributeDecoder = new AttributeDecoderImpl();
        }

        // Throw InvalidSyntaxException if there are multiple Attributes of the same type
        private static void validateAttributeUniqueness(List<Attribute> attributeList)
                throws IkeProtocolException {
            Set<Integer> foundTypes = new ArraySet<>();
            for (Attribute attr : attributeList) {
                if (!foundTypes.add(attr.type)) {
                    throw new InvalidSyntaxException(
                            "There are multiple Attributes of the same type. ");
                }
            }
        }

        // Check if there is Attribute with unrecognized type.
        protected abstract boolean hasUnrecognizedAttribute(List<Attribute> attributeList);

        // Check if this Transform ID is supported.
        protected abstract boolean isSupportedTransformId(int id);

        // Encode Transform to a ByteBuffer.
        protected abstract void encodeToByteBuffer(boolean isLast, ByteBuffer byteBuffer);

        // Get entire Transform length.
        protected abstract int getTransformLength();

        protected void encodeBasicTransformToByteBuffer(boolean isLast, ByteBuffer byteBuffer) {
            byte isLastIndicator = isLast ? LAST_TRANSFORM : NOT_LAST_TRANSFORM;
            byteBuffer
                    .put(isLastIndicator)
                    .put(new byte[TRANSFORM_RESERVED_FIELD_LEN])
                    .putShort((short) getTransformLength())
                    .put((byte) type)
                    .put(new byte[TRANSFORM_RESERVED_FIELD_LEN])
                    .putShort((short) id);
        }

        /**
         * Get Tranform Type as a String.
         *
         * @return Tranform Type as a String.
         */
        public abstract String getTransformTypeString();

        // TODO: Add abstract getTransformIdString() to return specific algorithm/dhGroup name
    }

    /**
     * EncryptionTransform represents an encryption algorithm. It may contain an Atrribute
     * specifying the key length.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key
     *     Exchange Protocol Version 2 (IKEv2)</a>
     */
    public static final class EncryptionTransform extends Transform {
        public static final int KEY_LEN_UNSPECIFIED = 0;

        private static final String ID_KEY = "id";
        private static final String SPECIFIED_KEY_LEN_KEY = "mSpecifiedKeyLength";

        // When using encryption algorithm with variable-length keys, mSpecifiedKeyLength MUST be
        // set and a KeyLengthAttribute MUST be attached. Otherwise, mSpecifiedKeyLength MUST NOT be
        // set and KeyLengthAttribute MUST NOT be attached.
        private final int mSpecifiedKeyLength;

        /**
         * Contruct an instance of EncryptionTransform with fixed key length for building an
         * outbound packet.
         *
         * @param id the IKE standard Transform ID.
         */
        public EncryptionTransform(@EncryptionAlgorithm int id) {
            this(id, KEY_LEN_UNSPECIFIED);
        }

        /**
         * Contruct an instance of EncryptionTransform with variable key length for building an
         * outbound packet.
         *
         * @param id the IKE standard Transform ID.
         * @param specifiedKeyLength the specified key length of this encryption algorithm.
         */
        public EncryptionTransform(@EncryptionAlgorithm int id, int specifiedKeyLength) {
            super(Transform.TRANSFORM_TYPE_ENCR, id);

            mSpecifiedKeyLength = specifiedKeyLength;
            try {
                validateKeyLength();
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /** Constructs this object by deserializing a PersistableBundle */
        public static EncryptionTransform fromPersistableBundle(@NonNull PersistableBundle in) {
            Objects.requireNonNull(in, "PersistableBundle is null");
            return new EncryptionTransform(in.getInt(ID_KEY), in.getInt(SPECIFIED_KEY_LEN_KEY));
        }

        /** Serializes this object to a PersistableBundle */
        public PersistableBundle toPersistableBundle() {
            final PersistableBundle result = new PersistableBundle();
            result.putInt(ID_KEY, id);
            result.putInt(SPECIFIED_KEY_LEN_KEY, mSpecifiedKeyLength);

            return result;
        }

        /**
         * Contruct an instance of EncryptionTransform for decoding an inbound packet.
         *
         * @param id the IKE standard Transform ID.
         * @param attributeList the decoded list of Attribute.
         * @throws InvalidSyntaxException for syntax error.
         */
        protected EncryptionTransform(int id, List<Attribute> attributeList)
                throws InvalidSyntaxException {
            super(Transform.TRANSFORM_TYPE_ENCR, id, attributeList);
            if (!isSupported) {
                mSpecifiedKeyLength = KEY_LEN_UNSPECIFIED;
            } else {
                if (attributeList.size() == 0) {
                    mSpecifiedKeyLength = KEY_LEN_UNSPECIFIED;
                } else {
                    KeyLengthAttribute attr = getKeyLengthAttribute(attributeList);
                    mSpecifiedKeyLength = attr.keyLength;
                }
                validateKeyLength();
            }
        }

        /**
         * Get the specified key length.
         *
         * @return the specified key length.
         */
        public int getSpecifiedKeyLength() {
            return mSpecifiedKeyLength;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id, mSpecifiedKeyLength);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EncryptionTransform)) return false;

            EncryptionTransform other = (EncryptionTransform) o;
            return (type == other.type
                    && id == other.id
                    && mSpecifiedKeyLength == other.mSpecifiedKeyLength);
        }

        @Override
        protected boolean isSupportedTransformId(int id) {
            return IkeSaProposal.getSupportedEncryptionAlgorithms().contains(id)
                    || ChildSaProposal.getSupportedEncryptionAlgorithms().contains(id);
        }

        @Override
        protected boolean hasUnrecognizedAttribute(List<Attribute> attributeList) {
            for (Attribute attr : attributeList) {
                if (attr instanceof UnrecognizedAttribute) {
                    return true;
                }
            }
            return false;
        }

        private KeyLengthAttribute getKeyLengthAttribute(List<Attribute> attributeList) {
            for (Attribute attr : attributeList) {
                if (attr.type == Attribute.ATTRIBUTE_TYPE_KEY_LENGTH) {
                    return (KeyLengthAttribute) attr;
                }
            }
            throw new IllegalArgumentException("Cannot find Attribute with Key Length type");
        }

        private void validateKeyLength() throws InvalidSyntaxException {
            switch (id) {
                case SaProposal.ENCRYPTION_ALGORITHM_3DES:
                    /* fall through */
                case SaProposal.ENCRYPTION_ALGORITHM_CHACHA20_POLY1305:
                    if (mSpecifiedKeyLength != KEY_LEN_UNSPECIFIED) {
                        throw new InvalidSyntaxException(
                                "Must not set Key Length value for this "
                                        + getTransformTypeString()
                                        + " Algorithm ID: "
                                        + id);
                    }
                    return;
                case SaProposal.ENCRYPTION_ALGORITHM_AES_CBC:
                    /* fall through */
                case SaProposal.ENCRYPTION_ALGORITHM_AES_CTR:
                    /* fall through */
                case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8:
                    /* fall through */
                case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_12:
                    /* fall through */
                case SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_16:
                    if (mSpecifiedKeyLength == KEY_LEN_UNSPECIFIED) {
                        throw new InvalidSyntaxException(
                                "Must set Key Length value for this "
                                        + getTransformTypeString()
                                        + " Algorithm ID: "
                                        + id);
                    }
                    if (mSpecifiedKeyLength != SaProposal.KEY_LEN_AES_128
                            && mSpecifiedKeyLength != SaProposal.KEY_LEN_AES_192
                            && mSpecifiedKeyLength != SaProposal.KEY_LEN_AES_256) {
                        throw new InvalidSyntaxException(
                                "Invalid key length for this "
                                        + getTransformTypeString()
                                        + " Algorithm ID: "
                                        + id);
                    }
                    return;
                default:
                    // Won't hit here.
                    throw new IllegalArgumentException(
                            "Unrecognized Encryption Algorithm ID: " + id);
            }
        }

        @Override
        protected void encodeToByteBuffer(boolean isLast, ByteBuffer byteBuffer) {
            encodeBasicTransformToByteBuffer(isLast, byteBuffer);

            if (mSpecifiedKeyLength != KEY_LEN_UNSPECIFIED) {
                new KeyLengthAttribute(mSpecifiedKeyLength).encodeToByteBuffer(byteBuffer);
            }
        }

        @Override
        protected int getTransformLength() {
            int len = BASIC_TRANSFORM_LEN;

            if (mSpecifiedKeyLength != KEY_LEN_UNSPECIFIED) {
                len += new KeyLengthAttribute(mSpecifiedKeyLength).getAttributeLength();
            }

            return len;
        }

        @Override
        public String getTransformTypeString() {
            return "Encryption Algorithm";
        }

        @Override
        @NonNull
        public String toString() {
            if (isSupported) {
                return SaProposal.getEncryptionAlgorithmString(id)
                        + "("
                        + getSpecifiedKeyLength()
                        + ")";
            } else {
                return "ENCR(" + id + ")";
            }
        }
    }

    /**
     * PrfTransform represents an pseudorandom function.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key
     *     Exchange Protocol Version 2 (IKEv2)</a>
     */
    public static final class PrfTransform extends Transform {
        /**
         * Contruct an instance of PrfTransform for building an outbound packet.
         *
         * @param id the IKE standard Transform ID.
         */
        public PrfTransform(@PseudorandomFunction int id) {
            super(Transform.TRANSFORM_TYPE_PRF, id);
        }

        /**
         * Contruct an instance of PrfTransform for decoding an inbound packet.
         *
         * @param id the IKE standard Transform ID.
         * @param attributeList the decoded list of Attribute.
         * @throws InvalidSyntaxException for syntax error.
         */
        protected PrfTransform(int id, List<Attribute> attributeList)
                throws InvalidSyntaxException {
            super(Transform.TRANSFORM_TYPE_PRF, id, attributeList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PrfTransform)) return false;

            PrfTransform other = (PrfTransform) o;
            return (type == other.type && id == other.id);
        }

        @Override
        protected boolean isSupportedTransformId(int id) {
            return IkeSaProposal.getSupportedPseudorandomFunctions().contains(id);
        }

        @Override
        protected boolean hasUnrecognizedAttribute(List<Attribute> attributeList) {
            return !attributeList.isEmpty();
        }

        @Override
        protected void encodeToByteBuffer(boolean isLast, ByteBuffer byteBuffer) {
            encodeBasicTransformToByteBuffer(isLast, byteBuffer);
        }

        @Override
        protected int getTransformLength() {
            return BASIC_TRANSFORM_LEN;
        }

        @Override
        public String getTransformTypeString() {
            return "Pseudorandom Function";
        }

        @Override
        @NonNull
        public String toString() {
            if (isSupported) {
                return SaProposal.getPseudorandomFunctionString(id);
            } else {
                return "PRF(" + id + ")";
            }
        }
    }

    /**
     * IntegrityTransform represents an integrity algorithm.
     *
     * <p>Proposing integrity algorithm for ESP SA is optional. Omitting the IntegrityTransform is
     * equivalent to including it with a value of NONE. When multiple integrity algorithms are
     * provided, choosing any of them are acceptable.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key
     *     Exchange Protocol Version 2 (IKEv2)</a>
     */
    public static final class IntegrityTransform extends Transform {
        /**
         * Contruct an instance of IntegrityTransform for building an outbound packet.
         *
         * @param id the IKE standard Transform ID.
         */
        public IntegrityTransform(@IntegrityAlgorithm int id) {
            super(Transform.TRANSFORM_TYPE_INTEG, id);
        }

        /**
         * Contruct an instance of IntegrityTransform for decoding an inbound packet.
         *
         * @param id the IKE standard Transform ID.
         * @param attributeList the decoded list of Attribute.
         * @throws InvalidSyntaxException for syntax error.
         */
        protected IntegrityTransform(int id, List<Attribute> attributeList)
                throws InvalidSyntaxException {
            super(Transform.TRANSFORM_TYPE_INTEG, id, attributeList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IntegrityTransform)) return false;

            IntegrityTransform other = (IntegrityTransform) o;
            return (type == other.type && id == other.id);
        }

        @Override
        protected boolean isSupportedTransformId(int id) {
            return IkeSaProposal.getSupportedIntegrityAlgorithms().contains(id)
                    || ChildSaProposal.getSupportedIntegrityAlgorithms().contains(id);
        }

        @Override
        protected boolean hasUnrecognizedAttribute(List<Attribute> attributeList) {
            return !attributeList.isEmpty();
        }

        @Override
        protected void encodeToByteBuffer(boolean isLast, ByteBuffer byteBuffer) {
            encodeBasicTransformToByteBuffer(isLast, byteBuffer);
        }

        @Override
        protected int getTransformLength() {
            return BASIC_TRANSFORM_LEN;
        }

        @Override
        public String getTransformTypeString() {
            return "Integrity Algorithm";
        }

        @Override
        @NonNull
        public String toString() {
            if (isSupported) {
                return SaProposal.getIntegrityAlgorithmString(id);
            } else {
                return "AUTH(" + id + ")";
            }
        }
    }

    /**
     * DhGroupTransform represents a Diffie-Hellman Group
     *
     * <p>Proposing DH group for non-first Child SA is optional. Omitting the DhGroupTransform is
     * equivalent to including it with a value of NONE. When multiple DH groups are provided,
     * choosing any of them are acceptable.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key
     *     Exchange Protocol Version 2 (IKEv2)</a>
     */
    public static final class DhGroupTransform extends Transform {
        /**
         * Contruct an instance of DhGroupTransform for building an outbound packet.
         *
         * @param id the IKE standard Transform ID.
         */
        public DhGroupTransform(@DhGroup int id) {
            super(Transform.TRANSFORM_TYPE_DH, id);
        }

        /**
         * Contruct an instance of DhGroupTransform for decoding an inbound packet.
         *
         * @param id the IKE standard Transform ID.
         * @param attributeList the decoded list of Attribute.
         * @throws InvalidSyntaxException for syntax error.
         */
        protected DhGroupTransform(int id, List<Attribute> attributeList)
                throws InvalidSyntaxException {
            super(Transform.TRANSFORM_TYPE_DH, id, attributeList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DhGroupTransform)) return false;

            DhGroupTransform other = (DhGroupTransform) o;
            return (type == other.type && id == other.id);
        }

        @Override
        protected boolean isSupportedTransformId(int id) {
            return SaProposal.getSupportedDhGroups().contains(id);
        }

        @Override
        protected boolean hasUnrecognizedAttribute(List<Attribute> attributeList) {
            return !attributeList.isEmpty();
        }

        @Override
        protected void encodeToByteBuffer(boolean isLast, ByteBuffer byteBuffer) {
            encodeBasicTransformToByteBuffer(isLast, byteBuffer);
        }

        @Override
        protected int getTransformLength() {
            return BASIC_TRANSFORM_LEN;
        }

        @Override
        public String getTransformTypeString() {
            return "Diffie-Hellman Group";
        }

        @Override
        @NonNull
        public String toString() {
            if (isSupported) {
                return SaProposal.getDhGroupString(id);
            } else {
                return "DH(" + id + ")";
            }
        }
    }

    /**
     * EsnTransform represents ESN policy that indicates if IPsec SA uses tranditional 32-bit
     * sequence numbers or extended(64-bit) sequence numbers.
     *
     * <p>Currently IKE library only supports negotiating IPsec SA that do not use extended sequence
     * numbers. The Transform ID of EsnTransform in outbound packets is not user configurable.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.2">RFC 7296, Internet Key
     *     Exchange Protocol Version 2 (IKEv2)</a>
     */
    public static final class EsnTransform extends Transform {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({ESN_POLICY_NO_EXTENDED, ESN_POLICY_EXTENDED})
        public @interface EsnPolicy {}

        public static final int ESN_POLICY_NO_EXTENDED = 0;
        public static final int ESN_POLICY_EXTENDED = 1;

        /**
         * Construct an instance of EsnTransform indicates using no-extended sequence numbers for
         * building an outbound packet.
         */
        public EsnTransform() {
            super(Transform.TRANSFORM_TYPE_ESN, ESN_POLICY_NO_EXTENDED);
        }

        /**
         * Contruct an instance of EsnTransform for decoding an inbound packet.
         *
         * @param id the IKE standard Transform ID.
         * @param attributeList the decoded list of Attribute.
         * @throws InvalidSyntaxException for syntax error.
         */
        protected EsnTransform(int id, List<Attribute> attributeList)
                throws InvalidSyntaxException {
            super(Transform.TRANSFORM_TYPE_ESN, id, attributeList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EsnTransform)) return false;

            EsnTransform other = (EsnTransform) o;
            return (type == other.type && id == other.id);
        }

        @Override
        protected boolean isSupportedTransformId(int id) {
            return (id == ESN_POLICY_NO_EXTENDED || id == ESN_POLICY_EXTENDED);
        }

        @Override
        protected boolean hasUnrecognizedAttribute(List<Attribute> attributeList) {
            return !attributeList.isEmpty();
        }

        @Override
        protected void encodeToByteBuffer(boolean isLast, ByteBuffer byteBuffer) {
            encodeBasicTransformToByteBuffer(isLast, byteBuffer);
        }

        @Override
        protected int getTransformLength() {
            return BASIC_TRANSFORM_LEN;
        }

        @Override
        public String getTransformTypeString() {
            return "Extended Sequence Numbers";
        }

        @Override
        @NonNull
        public String toString() {
            if (id == ESN_POLICY_NO_EXTENDED) {
                return "ESN_No_Extended";
            }
            return "ESN_Extended";
        }
    }

    /**
     * UnrecognizedTransform represents a Transform with unrecognized Transform Type.
     *
     * <p>Proposals containing an UnrecognizedTransform should be ignored.
     */
    protected static final class UnrecognizedTransform extends Transform {
        protected UnrecognizedTransform(int type, int id, List<Attribute> attributeList) {
            super(type, id, attributeList);
        }

        @Override
        protected boolean isSupportedTransformId(int id) {
            return false;
        }

        @Override
        protected boolean hasUnrecognizedAttribute(List<Attribute> attributeList) {
            return !attributeList.isEmpty();
        }

        @Override
        protected void encodeToByteBuffer(boolean isLast, ByteBuffer byteBuffer) {
            throw new UnsupportedOperationException(
                    "It is not supported to encode a Transform with" + getTransformTypeString());
        }

        @Override
        protected int getTransformLength() {
            throw new UnsupportedOperationException(
                    "It is not supported to get length of a Transform with "
                            + getTransformTypeString());
        }

        /**
         * Return Tranform Type of Unrecognized Transform as a String.
         *
         * @return Tranform Type of Unrecognized Transform as a String.
         */
        @Override
        public String getTransformTypeString() {
            return "Unrecognized Transform Type.";
        }
    }

    /**
     * Attribute is an abtract base class for completing the specification of some {@link
     * Transform}.
     *
     * <p>Attribute is either in Type/Value format or Type/Length/Value format. For TV format,
     * Attribute length is always 4 bytes containing value for 2 bytes. While for TLV format,
     * Attribute length is determined by length field.
     *
     * <p>Currently only Key Length type is supported
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3.5">RFC 7296, Internet Key
     *     Exchange Protocol Version 2 (IKEv2)</a>
     */
    public abstract static class Attribute {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({ATTRIBUTE_TYPE_KEY_LENGTH})
        public @interface AttributeType {}

        // Support only one Attribute type: Key Length. Should use Type/Value format.
        public static final int ATTRIBUTE_TYPE_KEY_LENGTH = 14;

        // Mask to extract the left most AF bit to indicate Attribute Format.
        private static final int ATTRIBUTE_FORMAT_MASK = 0x8000;
        // Mask to extract 15 bits after the AF bit to indicate Attribute Type.
        private static final int ATTRIBUTE_TYPE_MASK = 0x7fff;

        // Package private mask to indicate that Type-Value (TV) Attribute Format is used.
        static final int ATTRIBUTE_FORMAT_TV = ATTRIBUTE_FORMAT_MASK;

        // Package private
        static final int TV_ATTRIBUTE_VALUE_LEN = 2;
        static final int TV_ATTRIBUTE_TOTAL_LEN = 4;
        static final int TVL_ATTRIBUTE_HEADER_LEN = TV_ATTRIBUTE_TOTAL_LEN;

        // Only Key Length type belongs to AttributeType
        public final int type;

        /** Construct an instance of an Attribute when decoding message. */
        protected Attribute(int type) {
            this.type = type;
        }

        @VisibleForTesting
        static Pair<Attribute, Integer> readFrom(ByteBuffer inputBuffer)
                throws IkeProtocolException {
            short formatAndType = inputBuffer.getShort();
            int format = formatAndType & ATTRIBUTE_FORMAT_MASK;
            int type = formatAndType & ATTRIBUTE_TYPE_MASK;

            int length = 0;
            byte[] value = new byte[0];
            if (format == ATTRIBUTE_FORMAT_TV) {
                // Type/Value format
                length = TV_ATTRIBUTE_TOTAL_LEN;
                value = new byte[TV_ATTRIBUTE_VALUE_LEN];
            } else {
                // Type/Length/Value format
                if (type == ATTRIBUTE_TYPE_KEY_LENGTH) {
                    throw new InvalidSyntaxException("Wrong format in Transform Attribute");
                }

                length = Short.toUnsignedInt(inputBuffer.getShort());
                int valueLen = length - TVL_ATTRIBUTE_HEADER_LEN;
                // IkeMessage will catch exception if valueLen is negative.
                value = new byte[valueLen];
            }

            inputBuffer.get(value);

            switch (type) {
                case ATTRIBUTE_TYPE_KEY_LENGTH:
                    return new Pair(new KeyLengthAttribute(value), length);
                default:
                    return new Pair(new UnrecognizedAttribute(type, value), length);
            }
        }

        // Encode Attribute to a ByteBuffer.
        protected abstract void encodeToByteBuffer(ByteBuffer byteBuffer);

        // Get entire Attribute length.
        protected abstract int getAttributeLength();
    }

    /** KeyLengthAttribute represents a Key Length type Attribute */
    public static final class KeyLengthAttribute extends Attribute {
        public final int keyLength;

        protected KeyLengthAttribute(byte[] value) {
            this(Short.toUnsignedInt(ByteBuffer.wrap(value).getShort()));
        }

        protected KeyLengthAttribute(int keyLength) {
            super(ATTRIBUTE_TYPE_KEY_LENGTH);
            this.keyLength = keyLength;
        }

        @Override
        protected void encodeToByteBuffer(ByteBuffer byteBuffer) {
            byteBuffer
                    .putShort((short) (ATTRIBUTE_FORMAT_TV | ATTRIBUTE_TYPE_KEY_LENGTH))
                    .putShort((short) keyLength);
        }

        @Override
        protected int getAttributeLength() {
            return TV_ATTRIBUTE_TOTAL_LEN;
        }
    }

    /**
     * UnrecognizedAttribute represents a Attribute with unrecoginzed Attribute Type.
     *
     * <p>Transforms containing UnrecognizedAttribute should be ignored.
     */
    protected static final class UnrecognizedAttribute extends Attribute {
        protected UnrecognizedAttribute(int type, byte[] value) {
            super(type);
        }

        @Override
        protected void encodeToByteBuffer(ByteBuffer byteBuffer) {
            throw new UnsupportedOperationException(
                    "It is not supported to encode an unrecognized Attribute.");
        }

        @Override
        protected int getAttributeLength() {
            throw new UnsupportedOperationException(
                    "It is not supported to get length of an unrecognized Attribute.");
        }
    }

    /**
     * Encode SA payload to ByteBUffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);

        for (int i = 0; i < proposalList.size(); i++) {
            // The last proposal has the isLast flag set to true.
            proposalList.get(i).encodeToByteBuffer(i == proposalList.size() - 1, byteBuffer);
        }
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        int len = GENERIC_HEADER_LENGTH;

        for (Proposal p : proposalList) len += p.getProposalLength();

        return len;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        return "SA";
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isSaResponse) {
            sb.append("SA Response: ");
        } else {
            sb.append("SA Request: ");
        }

        int len = proposalList.size();
        for (int i = 0; i < len; i++) {
            sb.append(proposalList.get(i).toString());
            if (i < len - 1) sb.append(", ");
        }

        return sb.toString();
    }
}
