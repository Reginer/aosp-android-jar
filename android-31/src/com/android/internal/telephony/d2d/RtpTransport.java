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

package com.android.internal.telephony.d2d;

import android.annotation.NonNull;
import android.net.Uri;
import android.os.Handler;
import android.telecom.Log;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.RtpHeaderExtension;
import android.telephony.ims.RtpHeaderExtensionType;
import android.util.ArraySet;

import com.android.internal.telephony.BiMap;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements a transport protocol which makes use of RTP header extensions to communicate between
 * two devices.
 * <p>
 * Two types of device to device communications are supported,
 * {@link #DEVICE_STATE_RTP_HEADER_EXTENSION} messages which communicate attributes such as the
 * device service level and battery, and {@link #CALL_STATE_RTP_HEADER_EXTENSION} which communicates
 * information pertaining to an ongoing call.
 * <p>
 * When the ImsService negotiates the media for a call using SDP it will also potentially negotiate
 * the supported RTP header extension types which both parties of the call agree upon.  When a call
 * first begins if the accepted RTP header extension URIs matches the two well defined URIs we
 * define here, then we can assume the other party supports device to device communication using
 * RTP header extensions.
 * <p>
 * It is, however, possible that network signalling causes a failure to successfully negotiate the
 * RTP header extension types which both sides agree upon.  In this case we will wait for a short
 * period of time to see if we receive an RTP header extension which corresponds to one of the
 * local identifiers we would have expected to negotiation.  If such an RTP header extension is
 * received in a timely manner, we can assume that the other party is capable of using RTP header
 * extensions to communicate even though the RTP extension type URIs did not successfully negotiate.
 * If we do not receive a valid extension in this case, we will consider negotiation for this
 * transport to have failed.
 */
public class RtpTransport implements TransportProtocol, RtpAdapter.Callback {
    /**
     * {@link Uri} identifier for an RTP header extension used to communicate device state during
     * calls.
     */
    public static Uri DEVICE_STATE_RTP_HEADER_EXTENSION =
            Uri.parse("http://develop.android.com/122020/d2dcomm#device-state");

    /**
     * {@link Uri} identifier for an RTP header extension used to communication call state during
     * calls.
     */
    public static Uri CALL_STATE_RTP_HEADER_EXTENSION =
            Uri.parse("http://develop.android.com/122020/d2dcomm#call-state");

    /**
     * Default local identifier for device state RTP header extensions.
     */
    public static int DEVICE_STATE_LOCAL_IDENTIFIER = 10;

    /**
     * Default local identifier for call state RTP header extensions.
     */
    public static int CALL_STATE_LOCAL_IDENTIFIER = 11;

    /**
     * {@link RtpHeaderExtensionType} for device state communication.
     */
    public static RtpHeaderExtensionType DEVICE_STATE_RTP_HEADER_EXTENSION_TYPE =
            new RtpHeaderExtensionType(DEVICE_STATE_LOCAL_IDENTIFIER,
                    DEVICE_STATE_RTP_HEADER_EXTENSION);

    /**
     * {@link RtpHeaderExtensionType} for call state communication.
     */
    public static RtpHeaderExtensionType CALL_STATE_RTP_HEADER_EXTENSION_TYPE =
            new RtpHeaderExtensionType(CALL_STATE_LOCAL_IDENTIFIER,
                    CALL_STATE_RTP_HEADER_EXTENSION);


    /**
     * See {@link #generateRtpHeaderExtension(Communicator.Message)} for more information; indicates
     * the offset of the parameter value in the RTP header extension payload.
     */
    public static final int RTP_PARAMETER_BIT_OFFSET = 4;

    /**
     * RTP header extension bits set for {@link Communicator#MESSAGE_CALL_RADIO_ACCESS_TYPE} msg.
     */
    public static final byte RTP_CALL_STATE_MSG_RADIO_ACCESS_TYPE_BITS = 0b0001;

    /**
     * RTP header extension bits set for {@link Communicator#MESSAGE_CALL_AUDIO_CODEC} msg.
     */
    public static final byte RTP_CALL_STATE_MSG_CODEC_BITS = 0b0010;

    /**
     * RTP header extension bits set for {@link Communicator#MESSAGE_DEVICE_BATTERY_STATE} msg.
     */
    public static final byte RTP_DEVICE_STATE_MSG_BATTERY_BITS = 0b0001;

    /**
     * RTP header extension bits set for {@link Communicator#MESSAGE_DEVICE_NETWORK_COVERAGE} msg.
     */
    public static final byte RTP_DEVICE_STATE_MSG_NETWORK_COVERAGE_BITS = 0b0010;

    /**
     * Provides a mapping between the various {@code Communicator#MESSAGE_*} values and their bit
     * representation in an RTP header extension payload.  Used to translate outgoing message types
     * to an RTP payload and to interpret incoming RTP payloads and translate them back into message
     * types.
     */
    private static final BiMap<Integer, Byte> CALL_STATE_MSG_TYPE_TO_RTP_BITS = new BiMap<>();
    private static final BiMap<Integer, Byte> DEVICE_STATE_MSG_TYPE_TO_RTP_BITS = new BiMap<>();
    static {
        CALL_STATE_MSG_TYPE_TO_RTP_BITS.put(
                Communicator.MESSAGE_CALL_RADIO_ACCESS_TYPE,
                RTP_CALL_STATE_MSG_RADIO_ACCESS_TYPE_BITS);
        CALL_STATE_MSG_TYPE_TO_RTP_BITS.put(
                Communicator.MESSAGE_CALL_AUDIO_CODEC,
                RTP_CALL_STATE_MSG_CODEC_BITS);
        DEVICE_STATE_MSG_TYPE_TO_RTP_BITS.put(
                Communicator.MESSAGE_DEVICE_BATTERY_STATE,
                RTP_DEVICE_STATE_MSG_BATTERY_BITS);
        DEVICE_STATE_MSG_TYPE_TO_RTP_BITS.put(
                Communicator.MESSAGE_DEVICE_NETWORK_COVERAGE,
                RTP_DEVICE_STATE_MSG_NETWORK_COVERAGE_BITS);
    }

    /**
     * RTP header extension bits set for {@link Communicator#MESSAGE_CALL_RADIO_ACCESS_TYPE} data
     * payload.  Corresponds to {@link Communicator#RADIO_ACCESS_TYPE_LTE}.
     */
    public static final byte RTP_RAT_VALUE_LTE_BITS  = 0b0001 << RTP_PARAMETER_BIT_OFFSET;

    /**
     * RTP header extension bits set for {@link Communicator#MESSAGE_CALL_RADIO_ACCESS_TYPE} data
     * payload.  Corresponds to {@link Communicator#RADIO_ACCESS_TYPE_IWLAN}.
     */
    public static final byte RTP_RAT_VALUE_WLAN_BITS = 0b0010 << RTP_PARAMETER_BIT_OFFSET;

    /**
     * RTP header extension bits set for {@link Communicator#MESSAGE_CALL_RADIO_ACCESS_TYPE} data
     * payload.  Corresponds to {@link Communicator#RADIO_ACCESS_TYPE_NR}.
     */
    public static final byte RTP_RAT_VALUE_NR_BITS   = 0b0011 << RTP_PARAMETER_BIT_OFFSET;

    /**
     * Provides a mapping between the various {@code Communicator#RADIO_ACCESS_TYPE_*} values and
     * their bit representation in an RTP header extension payload.
     */
    private static final BiMap<Integer, Byte> RAT_VALUE_TO_RTP_BITS = new BiMap<>();
    static {
        RAT_VALUE_TO_RTP_BITS.put(
                Communicator.RADIO_ACCESS_TYPE_IWLAN, RTP_RAT_VALUE_WLAN_BITS);
        RAT_VALUE_TO_RTP_BITS.put(
                Communicator.RADIO_ACCESS_TYPE_LTE, RTP_RAT_VALUE_LTE_BITS);
        RAT_VALUE_TO_RTP_BITS.put(
                Communicator.RADIO_ACCESS_TYPE_NR, RTP_RAT_VALUE_NR_BITS);
    }

    /**
     * RTP header extension bits set for {@link Communicator#MESSAGE_CALL_AUDIO_CODEC} data
     * payload.  Corresponds to {@link Communicator#AUDIO_CODEC_EVS}.
     */
    public static final byte RTP_CODEC_VALUE_EVS_BITS    = 0b0001 << RTP_PARAMETER_BIT_OFFSET;
    public static final byte RTP_CODEC_VALUE_AMR_WB_BITS = 0b0010 << RTP_PARAMETER_BIT_OFFSET;
    public static final byte RTP_CODEC_VALUE_AMR_NB_BITS = 0b0011 << RTP_PARAMETER_BIT_OFFSET;

    /**
     * Provides a mapping between the various {@code Communicator#AUDIO_CODEC_*} values and
     * their bit representation in an RTP header extension payload.
     */
    private static final BiMap<Integer, Byte> CODEC_VALUE_TO_RTP_BITS = new BiMap<>();
    static {
        CODEC_VALUE_TO_RTP_BITS.put(
                Communicator.AUDIO_CODEC_EVS, RTP_CODEC_VALUE_EVS_BITS);
        CODEC_VALUE_TO_RTP_BITS.put(
                Communicator.AUDIO_CODEC_AMR_WB, RTP_CODEC_VALUE_AMR_WB_BITS);
        CODEC_VALUE_TO_RTP_BITS.put(
                Communicator.AUDIO_CODEC_AMR_NB, RTP_CODEC_VALUE_AMR_NB_BITS);
    }

    public static final byte RTP_BATTERY_STATE_LOW_BITS      = 0b0000 << RTP_PARAMETER_BIT_OFFSET;
    public static final byte RTP_BATTERY_STATE_GOOD_BITS     = 0b0001 << RTP_PARAMETER_BIT_OFFSET;
    public static final byte RTP_BATTERY_STATE_CHARGING_BITS = 0b0011 << RTP_PARAMETER_BIT_OFFSET;

    /**
     * Provides a mapping between the various {@code Communicator#BATTERY_STATE_*} values and
     * their bit representation in an RTP header extension payload.
     */
    private static final BiMap<Integer, Byte> BATTERY_STATE_VALUE_TO_RTP_BITS = new BiMap<>();
    static {
        BATTERY_STATE_VALUE_TO_RTP_BITS.put(
                Communicator.BATTERY_STATE_LOW, RTP_BATTERY_STATE_LOW_BITS);
        BATTERY_STATE_VALUE_TO_RTP_BITS.put(
                Communicator.BATTERY_STATE_GOOD, RTP_BATTERY_STATE_GOOD_BITS);
        BATTERY_STATE_VALUE_TO_RTP_BITS.put(
                Communicator.BATTERY_STATE_CHARGING, RTP_BATTERY_STATE_CHARGING_BITS);
    }

    public static final byte RTP_NETWORK_COVERAGE_POOR_BITS = 0b0000 << RTP_PARAMETER_BIT_OFFSET;
    public static final byte RTP_NETWORK_COVERAGE_GOOD_BITS = 0b0001 << RTP_PARAMETER_BIT_OFFSET;

    /**
     * Provides a mapping between the various {@code Communicator#COVERAGE_*} values and
     * their bit representation in an RTP header extension payload.
     */
    private static final BiMap<Integer, Byte> NETWORK_COVERAGE_VALUE_TO_RTP_BITS = new BiMap<>();
    static {
        NETWORK_COVERAGE_VALUE_TO_RTP_BITS.put(
                Communicator.COVERAGE_POOR, RTP_NETWORK_COVERAGE_POOR_BITS);
        NETWORK_COVERAGE_VALUE_TO_RTP_BITS.put(
                Communicator.COVERAGE_GOOD, RTP_NETWORK_COVERAGE_GOOD_BITS);
    }

    /**
     * Indicates that the transport is not yet ready for use and negotiation has not yet completed.
     */
    public static final int PROTOCOL_STATUS_NEGOTIATION_REQUIRED = 1;

    /**
     * Indicates that the agreed upon RTP header extension types (see
     * {@link RtpAdapter#getAcceptedRtpHeaderExtensions()}) did not specify both the
     * {@link #DEVICE_STATE_RTP_HEADER_EXTENSION} and {@link #CALL_STATE_RTP_HEADER_EXTENSION
     * RTP header extension types.  We are not going to wait a short time to see if we receive an
     * incoming RTP packet with one of the local identifiers we'd normally expect associated with
     * these header extension {@link Uri}s.  If we do receive a valid RTP header extension we will
     * move to the {@link #PROTOCOL_STATUS_NEGOTIATION_COMPLETE} status.  If we do not receive a
     * valid RTP header extension we move to {@link #PROTOCOL_STATUS_NEGOTIATION_FAILED} and report
     * the failure via {@link Callback#onNegotiationFailed(TransportProtocol) }.
     */
    public static final int PROTOCOL_STATUS_NEGOTIATION_WAITING_ON_PACKET = 2;

    /**
     * Indicates we either agreed upon the required header extensions, or received a valid packet
     * despite not having agreed upon required header extensions.  The transport is ready to use at
     * this point.
     */
    public static final int PROTOCOL_STATUS_NEGOTIATION_COMPLETE = 3;

    /**
     * Indicates protocol negotiation failed.
     */
    public static final int PROTOCOL_STATUS_NEGOTIATION_FAILED = 4;

    /**
     * Callback which is used to report back to the {@link Communicator} about the status of
     * protocol negotiation and incoming messages.
     */
    private TransportProtocol.Callback mCallback;

    /**
     * Adapter which abstracts out the details of sending/receiving RTP header extensions.
     */
    private final RtpAdapter mRtpAdapter;

    /**
     * Configuration adapter for timeouts related to protocol setup.
     */
    private final Timeouts.Adapter mTimeoutsAdapter;

    /**
     * Handler for posting future events.
     */
    private final Handler mHandler;

    /**
     * {@code true} if the carrier supports negotiating the RTP header extensions using SDP.
     * If {@code true}, we can expected the
     * {@link ImsCallProfile#getAcceptedRtpHeaderExtensionTypes()} to contain the SDP negotiated RTP
     * header extensions.  If {@code false} we will assume the protocol is negotiated only after
     * receiving an RTP header extension of the expected type.
     */
    private final boolean mIsSdpNegotiationSupported;

    /**
     * Protocol status.
     */
    private int mProtocolStatus = PROTOCOL_STATUS_NEGOTIATION_REQUIRED;

    /**
     * The supported RTP header extension types.
     */
    private ArraySet<RtpHeaderExtensionType> mSupportedRtpHeaderExtensionTypes = new ArraySet<>();

    /**
     * Initializes the {@link RtpTransport}.
     * @param rtpAdapter Adapter for abstract send/receive of RTP header extension data.
     * @param timeoutsAdapter Timeouts adapter for dealing with time based configurations.
     * @param handler Handler for posting future events.
     * @param isSdpNegotiationSupported Indicates whether SDP negotiation
     */
    public RtpTransport(RtpAdapter rtpAdapter, Timeouts.Adapter timeoutsAdapter, Handler handler,
            boolean isSdpNegotiationSupported) {
        mRtpAdapter = rtpAdapter;
        mTimeoutsAdapter = timeoutsAdapter;
        mHandler = handler;
        mIsSdpNegotiationSupported = isSdpNegotiationSupported;
    }

    /**
     * Sets the Callback for this transport.  Used to report back received messages.
     * @param callback The callback.
     */
    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * Begin transport protocol negotiation at the request of the framework.
     *
     * If the {@link #DEVICE_STATE_RTP_HEADER_EXTENSION} and
     * {@link #CALL_STATE_RTP_HEADER_EXTENSION} extension {@link Uri}s are part of the accepted
     * extension types, we consider the negotiation successful.
     *
     * TODO: If they are not in the accepted extensions, we will wait a short period of time to
     * receive a valid RTP header extension we recognize.
     */
    @Override
    public void startNegotiation() {
        Set<RtpHeaderExtensionType> acceptedExtensions =
                mRtpAdapter.getAcceptedRtpHeaderExtensions();
        mSupportedRtpHeaderExtensionTypes.addAll(acceptedExtensions);

        Log.i(this, "startNegotiation: supportedExtensions=%s", mSupportedRtpHeaderExtensionTypes
                .stream()
                .map(e -> e.toString())
                .collect(Collectors.joining(",")));

        if (mIsSdpNegotiationSupported) {
            boolean areExtensionsAvailable = acceptedExtensions.stream().anyMatch(
                    e -> e.getUri().equals(DEVICE_STATE_RTP_HEADER_EXTENSION))
                    && acceptedExtensions.stream().anyMatch(
                    e -> e.getUri().equals(CALL_STATE_RTP_HEADER_EXTENSION));

            if (areExtensionsAvailable) {
                // Headers were negotiated during SDP, so we can assume negotiation is complete and
                // signal to the communicator that we can use this transport.
                mProtocolStatus = PROTOCOL_STATUS_NEGOTIATION_COMPLETE;
                Log.i(this, "startNegotiation: header extensions available, negotiation success");
                notifyProtocolReady();
            } else {
                // Headers failed to be negotiated during SDP.   Assume protocol is not available.
                // TODO: Implement fallback logic where we still try an SDP probe/response.
                mProtocolStatus = PROTOCOL_STATUS_NEGOTIATION_FAILED;
                Log.i(this,
                        "startNegotiation: header extensions not available; negotiation failed");
                notifyProtocolUnavailable();
            }
        } else {
            Log.i(this, "startNegotiation: SDP negotiation not supported; negotiation complete");
            // TODO: This is temporary; we will need to implement a probe/response in this scenario
            // if SDP is not supported.  For now we will just assume the protocol is ready.
            notifyProtocolReady();
        }
    }

    /**
     * Handles sending messages using the RTP transport.  Generates valid {@link RtpHeaderExtension}
     * instances for each message to send.
     * @param messages The messages to send.
     */
    @Override
    public void sendMessages(Set<Communicator.Message> messages) {
        Set<RtpHeaderExtension> toSend = messages.stream().map(m -> generateRtpHeaderExtension(m))
                .collect(Collectors.toSet());
        Log.i(this, "sendMessages: sending=%s", messages);
        mRtpAdapter.sendRtpHeaderExtensions(toSend);
    }

    /**
     * Forces the protocol status to negotiated; for test purposes.
     */
    @Override
    public void forceNegotiated() {
        // If there is no supported RTP header extensions we need to fake it.
        if (mSupportedRtpHeaderExtensionTypes == null
                || mSupportedRtpHeaderExtensionTypes.isEmpty()) {
            mSupportedRtpHeaderExtensionTypes.add(DEVICE_STATE_RTP_HEADER_EXTENSION_TYPE);
            mSupportedRtpHeaderExtensionTypes.add(CALL_STATE_RTP_HEADER_EXTENSION_TYPE);
        }
        mProtocolStatus = PROTOCOL_STATUS_NEGOTIATION_COMPLETE;
    }

    /**
     * Forces the protocol status to un-negotiated; for test purposes.
     */
    @Override
    public void forceNotNegotiated() {
        mProtocolStatus = PROTOCOL_STATUS_NEGOTIATION_REQUIRED;
    }

    /**
     * Called by the platform when RTP header extensions are received and need to be translated to
     * concrete messages.
     * Results in a callback via
     * {@link com.android.internal.telephony.d2d.TransportProtocol.Callback#onMessagesReceived(Set)}
     * to notify when incoming messages are received.
     * @param extensions The received RTP header extensions.
     */
    @Override
    public void onRtpHeaderExtensionsReceived(@NonNull Set<RtpHeaderExtension> extensions) {
        Set<Communicator.Message> messages = extensions.stream().map(e -> extractMessage(e))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (messages.size() == 0) {
            return;
        }
        mCallback.onMessagesReceived(messages);
    }

    /**
     * Given a {@link RtpHeaderExtension} received from the network, parse out an found message.
     * @param extension The RTP header extension to parse.
     * @return The message, or {@code null} if no valid message found.
     */
    private Communicator.Message extractMessage(@NonNull RtpHeaderExtension extension) {
        // First determine the URI to figure out the general classification of the message.
        Optional<Uri> foundUri = mSupportedRtpHeaderExtensionTypes.stream()
                .filter(et -> et.getLocalIdentifier() == extension.getLocalIdentifier())
                .map(et -> et.getUri())
                .findFirst();
        if (!foundUri.isPresent()) {
            Log.w(this, "extractMessage: localIdentifier=%d not supported.",
                    extension.getLocalIdentifier());
            return null;
        }

        if (extension.getExtensionData() == null || extension.getExtensionData().length != 1) {
            Log.w(this, "extractMessage: localIdentifier=%d message with invalid data length.",
                    extension.getLocalIdentifier());
            return null;
        }

        Uri uri = foundUri.get();

        // Extract the bits which are the message type.
        byte messageTypeBits = (byte) (extension.getExtensionData()[0] & 0b1111);
        byte messageValueBits = (byte) (extension.getExtensionData()[0]
                & (0b1111 << RTP_PARAMETER_BIT_OFFSET));

        int messageType;
        int messageValue;
        if (DEVICE_STATE_RTP_HEADER_EXTENSION.equals(uri)) {
            Integer type = DEVICE_STATE_MSG_TYPE_TO_RTP_BITS.getKey(messageTypeBits);
            if (type == null) {
                Log.w(this, "extractMessage: localIdentifier=%d message with invalid type %s.",
                        extension.getLocalIdentifier(), Integer.toBinaryString(messageTypeBits));
                return null;
            }
            messageType = type;
            switch (messageType) {
                case Communicator.MESSAGE_DEVICE_BATTERY_STATE:
                    Integer val = BATTERY_STATE_VALUE_TO_RTP_BITS.getKey(messageValueBits);
                    if (val == null) {
                        Log.w(this, "extractMessage: localIdentifier=%d, battery state msg with "
                                        + "invalid value=%s",
                                extension.getLocalIdentifier(),
                                Integer.toBinaryString(messageValueBits));
                        return null;
                    }
                    messageValue = val;
                    break;
                case Communicator.MESSAGE_DEVICE_NETWORK_COVERAGE:
                    Integer val2 = NETWORK_COVERAGE_VALUE_TO_RTP_BITS.getKey(messageValueBits);
                    if (val2 == null) {
                        Log.w(this, "extractMessage: localIdentifier=%d, network coverage msg with "
                                        + "invalid value=%s",
                                extension.getLocalIdentifier(),
                                Integer.toBinaryString(messageValueBits));
                        return null;
                    }
                    messageValue = val2;
                    break;
                default:
                    Log.w(this, "messageType=%s, value=%s; invalid value",
                            Integer.toBinaryString(messageTypeBits),
                            Integer.toBinaryString(messageValueBits));
                    return null;
            }
        } else if (CALL_STATE_RTP_HEADER_EXTENSION.equals(uri)) {
            Integer typeValue = CALL_STATE_MSG_TYPE_TO_RTP_BITS.getKey(messageTypeBits);
            if (typeValue == null) {
                Log.w(this, "extractMessage: localIdentifier=%d, network coverage msg with "
                                + "invalid type=%s",
                        extension.getLocalIdentifier(),
                        Integer.toBinaryString(messageTypeBits));
                return null;
            }
            messageType = typeValue;
            switch (messageType) {
                case Communicator.MESSAGE_CALL_AUDIO_CODEC:
                    Integer val = CODEC_VALUE_TO_RTP_BITS.getKey(messageValueBits);
                    if (val == null) {
                        Log.w(this, "extractMessage: localIdentifier=%d, audio codec msg with "
                                        + "invalid value=%s",
                                extension.getLocalIdentifier(),
                                Integer.toBinaryString(messageValueBits));
                        return null;
                    }
                    messageValue = val;
                    break;
                case Communicator.MESSAGE_CALL_RADIO_ACCESS_TYPE:
                    Integer val2 = RAT_VALUE_TO_RTP_BITS.getKey(messageValueBits);
                    if (val2 == null) {
                        Log.w(this, "extractMessage: localIdentifier=%d, rat type msg with "
                                        + "invalid value=%s",
                                extension.getLocalIdentifier(),
                                Integer.toBinaryString(messageValueBits));
                        return null;
                    }
                    messageValue = val2;
                    break;
                default:
                    Log.w(this, "messageType=%s, value=%s; invalid value",
                            Integer.toBinaryString(messageTypeBits),
                            Integer.toBinaryString(messageValueBits));
                    return null;
            }
        } else {
            Log.w(this, "invalid uri=%s", uri);
            return null;
        }
        Log.i(this, "extractMessage: messageType=%s, value=%s --> message=%d, value=%d",
                Integer.toBinaryString(messageTypeBits), Integer.toBinaryString(messageValueBits),
                messageType, messageValue);
        return new Communicator.Message(messageType, messageValue);
    }


    /**
     * Generates an {@link RtpHeaderExtension} based on the specified message.
     * Per RFC8285, RTP header extensions have the format:
     * (bit)
     *  ___byte__ __byte__
     *  1234 5678 12345678
     * +----+----+--------+
     * | ID | len|  data  |
     * +----+----+--------+
     * Where ID is the {@link RtpHeaderExtensionType#getLocalIdentifier()}, len indicates the
     * number of data bytes being sent ({@link RtpHeaderExtension#getExtensionData()}.size()),
     * and data is the payload of the RTP header extension.
     * The {@link RtpHeaderExtension} generated here contains the bytes necessary to populate the
     * data field; the "ID" and "len" fields in the send RTP header extension are populated by the
     * IMS service.
     *
     * This method is responsible for generating the data field in the RTP header extension to be
     * sent.
     *
     * Device to device communication assumes the following format for the single byte payload:
     *  12345678
     * +--------+
     * |PPPPVVVV|
     * +--------+
     * Where:
     *   PPPP - 4 bits reserved for indicating the parameter encoded (e.g. it could be an audio
     *   codec ({@link Communicator#MESSAGE_CALL_AUDIO_CODEC})).
     *   VVVV - 4 bits reserved for indicating the value of the parameter encoded (e.g. it could be
     *   the EVS codec ({@link Communicator#AUDIO_CODEC_EVS})).
     *
     * @param message The message to be sent via RTP header extensions.
     * @return An {@link RtpHeaderExtension} representing the message.
     */
    public RtpHeaderExtension generateRtpHeaderExtension(Communicator.Message message) {
        byte[] payload = new byte[1];
        switch (message.getType()) {
            case Communicator.MESSAGE_CALL_AUDIO_CODEC:
                payload[0] |= CALL_STATE_MSG_TYPE_TO_RTP_BITS.getValue(message.getType());
                payload[0] |= CODEC_VALUE_TO_RTP_BITS.getValue(message.getValue());
                return new RtpHeaderExtension(
                        getRtpHeaderExtensionIdentifier(CALL_STATE_RTP_HEADER_EXTENSION),
                        payload);
            case Communicator.MESSAGE_CALL_RADIO_ACCESS_TYPE:
                payload[0] |= CALL_STATE_MSG_TYPE_TO_RTP_BITS.getValue(message.getType());
                payload[0] |= RAT_VALUE_TO_RTP_BITS.getValue(message.getValue());
                return new RtpHeaderExtension(
                        getRtpHeaderExtensionIdentifier(CALL_STATE_RTP_HEADER_EXTENSION),
                        payload);
            case Communicator.MESSAGE_DEVICE_BATTERY_STATE:
                payload[0] |= DEVICE_STATE_MSG_TYPE_TO_RTP_BITS.getValue(message.getType());
                payload[0] |= BATTERY_STATE_VALUE_TO_RTP_BITS.getValue(message.getValue());
                return new RtpHeaderExtension(
                        getRtpHeaderExtensionIdentifier(DEVICE_STATE_RTP_HEADER_EXTENSION),
                        payload);
            case Communicator.MESSAGE_DEVICE_NETWORK_COVERAGE:
                payload[0] |= DEVICE_STATE_MSG_TYPE_TO_RTP_BITS.getValue(message.getType());
                payload[0] |= NETWORK_COVERAGE_VALUE_TO_RTP_BITS.getValue(message.getValue());
                return new RtpHeaderExtension(
                        getRtpHeaderExtensionIdentifier(DEVICE_STATE_RTP_HEADER_EXTENSION),
                        payload);
        }
        return null;
    }

    /**
     * Given a {@link Uri} identifying an RTP header extension, return the associated local
     * identifier.
     * @param requestedUri the requested {@link Uri}.
     * @return the local identifier.
     */
    private int getRtpHeaderExtensionIdentifier(Uri requestedUri) {
        return mSupportedRtpHeaderExtensionTypes.stream()
                .filter(t -> t.getUri().equals(requestedUri))
                .findFirst().get().getLocalIdentifier();
    }

    /**
     * Notifies the {@link Communicator} that the RTP-based protocol is available.}
     */
    private void notifyProtocolReady() {
        if (mCallback != null) {
            mCallback.onNegotiationSuccess(this);
        }
    }

    /**
     * Notifies the {@link Communicator} that the RTP-based protocol is unavailable.
     */
    private void notifyProtocolUnavailable() {
        if (mCallback != null) {
            mCallback.onNegotiationFailed(this);
        }
    }
}
