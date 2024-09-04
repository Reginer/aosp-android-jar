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

package com.android.internal.net.eap.message;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.EapData.EAP_NAK;
import static com.android.internal.net.eap.message.EapData.NOTIFICATION_DATA;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapResponse;
import com.android.internal.net.eap.exceptions.EapInvalidPacketLengthException;
import com.android.internal.net.eap.exceptions.EapSilentException;
import com.android.internal.net.eap.exceptions.InvalidEapCodeException;
import com.android.internal.net.eap.exceptions.UnsupportedEapTypeException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * EapMessage represents an EAP Message.
 *
 * <p>EapMessages will be of type:
 * <ul>
 *     <li>@{link EAP_CODE_REQUEST}</li>
 *     <li>@{link EAP_CODE_RESPONSE}</li>
 *     <li>@{link EAP_CODE_SUCCESS}</li>
 *     <li>@{link EAP_CODE_FAILURE}</li>
 * </ul>
 *
 * Per RFC 3748 Section 4, EAP-Request and EAP-Response packets should be in the format:
 *
 * +-----------------+-----------------+----------------------------------+
 * |    Code (1B)    | Identifier (1B) |           Length (2B)            |
 * +-----------------+-----------------+----------------------------------+
 * |    Type (1B)    |  Type-Data ...
 * +-----------------+-----
 *
 * EAP-Success and EAP-Failure packets should be in the format:
 *
 * +-----------------+-----------------+----------------------------------+
 * |   Code (1B)     | Identifier (1B) |       Length (2B) = '0004'       |
 * +-----------------+-----------------+----------------------------------+
 *
 * Note that Length includes the EAP Header bytes.
 *
 * @see <a href="https://tools.ietf.org/html/rfc3748#section-4">RFC 3748, Extensible Authentication
 * Protocol (EAP)</a>
 */
public class EapMessage {
    private static final String TAG = EapMessage.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            EAP_CODE_REQUEST,
            EAP_CODE_RESPONSE,
            EAP_CODE_SUCCESS,
            EAP_CODE_FAILURE
    })
    public @interface EapCode {}

    public static final int EAP_CODE_REQUEST = 1;
    public static final int EAP_CODE_RESPONSE = 2;
    public static final int EAP_CODE_SUCCESS = 3;
    public static final int EAP_CODE_FAILURE = 4;

    public static final Map<Integer, String> EAP_CODE_STRING = new HashMap<>();
    static {
        EAP_CODE_STRING.put(EAP_CODE_REQUEST, "REQUEST");
        EAP_CODE_STRING.put(EAP_CODE_RESPONSE, "RESPONSE");
        EAP_CODE_STRING.put(EAP_CODE_SUCCESS, "SUCCESS");
        EAP_CODE_STRING.put(EAP_CODE_FAILURE, "FAILURE");
    }

    public static final int EAP_HEADER_LENGTH = 4;

    @EapCode public final int eapCode;
    public final int eapIdentifier;
    public final int eapLength;
    public final EapData eapData;

    public EapMessage(@EapCode int eapCode, int eapIdentifier, @Nullable EapData eapData)
            throws EapSilentException {
        this.eapCode = eapCode;
        this.eapIdentifier = eapIdentifier;
        this.eapLength = EAP_HEADER_LENGTH + ((eapData == null) ? 0 : eapData.getLength());
        this.eapData = eapData;

        validate();
    }

    /**
     * Decodes and returns an EapMessage from the given byte array.
     *
     * @param packet byte array containing a byte-encoded EapMessage
     * @return the EapMessage instance representing the given {@param packet}
     * @throws EapSilentException for decoding errors that must be discarded silently
     */
    public static EapMessage decode(@NonNull byte[] packet) throws EapSilentException {
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        int eapCode;
        int eapIdentifier;
        int eapLength;
        EapData eapData;
        try {
            eapCode = Byte.toUnsignedInt(buffer.get());
            eapIdentifier = Byte.toUnsignedInt(buffer.get());
            eapLength = Short.toUnsignedInt(buffer.getShort());

            if (eapCode == EAP_CODE_REQUEST || eapCode == EAP_CODE_RESPONSE) {
                int eapType = Byte.toUnsignedInt(buffer.get());
                if (!EapData.isSupportedEapType(eapType)) {
                    LOG.e(TAG, "Decoding EAP packet with unsupported EAP-Type: " + eapType);
                    throw new UnsupportedEapTypeException(eapIdentifier,
                            "Unsupported eapType=" + eapType);
                }

                // Length of data to go into EapData.eapTypeData =
                //      eapLength - EAP_HEADER_LENGTH - 1B (eapType)
                int eapDataLengthRemaining = Math.max(0, eapLength - EAP_HEADER_LENGTH - 1);
                byte[] eapDataBytes =
                        new byte[Math.min(eapDataLengthRemaining, buffer.remaining())];

                buffer.get(eapDataBytes);
                eapData = new EapData(eapType, eapDataBytes);
            } else {
                eapData = null;
            }
        } catch (BufferUnderflowException ex) {
            String msg = "EAP packet is missing required values";
            LOG.e(TAG, msg, ex);
            throw new EapInvalidPacketLengthException(msg, ex);
        }

        int eapDataLength = (eapData == null) ? 0 : eapData.getLength();
        if (eapLength > EAP_HEADER_LENGTH + eapDataLength) {
            String msg = "Packet is shorter than specified length";
            LOG.e(TAG, msg);
            throw new EapInvalidPacketLengthException(msg);
        }

        return new EapMessage(eapCode, eapIdentifier, eapData);
    }

    /**
     * Converts this EapMessage instance to its byte-encoded representation.
     *
     * @return byte[] representing the byte-encoded EapMessage
     */
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(eapLength);
        byteBuffer.put((byte) eapCode);
        byteBuffer.put((byte) eapIdentifier);
        byteBuffer.putShort((short) eapLength);

        if (eapData != null) {
            eapData.encodeToByteBuffer(byteBuffer);
        }

        return byteBuffer.array();
    }

    /**
     * Creates and returns an EAP-Response/Notification message for the given EAP Identifier wrapped
     * in an EapResponse object.
     *
     * @param eapIdentifier the identifier for the message being responded to
     * @return an EapResponse object containing an EAP-Response/Notification message with an
     *         identifier matching the given identifier, or an EapError if an exception was thrown
     */
    public static EapResult getNotificationResponse(int eapIdentifier) {
        try {
            return EapResponse.getEapResponse(
                    new EapMessage(EAP_CODE_RESPONSE, eapIdentifier, NOTIFICATION_DATA));
        } catch (EapSilentException ex) {
            // this should never happen - the only variable value is the identifier
            LOG.wtf(TAG, "Failed to create Notification Response for message with identifier="
                    + eapIdentifier);
            return new EapError(ex);
        }
    }

    /**
     * Creates and returns an EAP-Response/Nak message for the given EAP Identifier wrapped in an
     * EapResponse object.
     *
     * @param eapIdentifier the identifier for the message being responded to
     * @param supportedEapTypes Collection of EAP Method types supported by the EAP Session
     * @return an EapResponse object containing an EAP-Response/Nak message with an identifier
     *         matching the given identifier, or an EapError if an exception was thrown
     */
    public static EapResult getNakResponse(
            int eapIdentifier,
            Collection<Integer> supportedEapTypes) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(supportedEapTypes.size());
            for (int eapMethodType : supportedEapTypes) {
                buffer.put((byte) eapMethodType);
            }
            EapData nakData = new EapData(EAP_NAK, buffer.array());

            return EapResponse.getEapResponse(
                    new EapMessage(EAP_CODE_RESPONSE, eapIdentifier, nakData));
        } catch (EapSilentException ex) {
            // this should never happen - the only variable value is the identifier
            LOG.wtf(TAG,  "Failed to create Nak for message with identifier="
                    + eapIdentifier);
            return new EapError(ex);
        }
    }

    private void validate() throws EapSilentException {
        if (eapCode != EAP_CODE_REQUEST
                && eapCode != EAP_CODE_RESPONSE
                && eapCode != EAP_CODE_SUCCESS
                && eapCode != EAP_CODE_FAILURE) {
            LOG.e(TAG, "Invalid EAP Code: " + eapCode);
            throw new InvalidEapCodeException(eapCode);
        }

        if ((eapCode == EAP_CODE_SUCCESS || eapCode == EAP_CODE_FAILURE)
                && eapLength != EAP_HEADER_LENGTH) {
            LOG.e(TAG, "Invalid length for EAP-Success/EAP-Failure. Length: " + eapLength);
            throw new EapInvalidPacketLengthException(
                    "EAP Success/Failure packets must be length 4");
        }

        if ((eapCode == EAP_CODE_REQUEST || eapCode == EAP_CODE_RESPONSE) && eapData == null) {
            LOG.e(TAG, "No Type value included for EAP-Request/EAP-Response");
            throw new EapInvalidPacketLengthException(
                    "EAP Request/Response packets must include a Type value");
        }
    }
}
