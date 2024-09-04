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

package com.android.internal.net.eap.message.simaka;

import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_ANY_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_CLIENT_ERROR_CODE;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_COUNTER;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_COUNTER_TOO_SMALL;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_ENCR_DATA;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_FULLAUTH_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_IDENTITY;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_IV;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_MAC;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_NEXT_REAUTH_ID;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_NONCE_S;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_NOTIFICATION;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_PADDING;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_PERMANENT_ID_REQ;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.LENGTH_SCALING;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.SKIPPABLE_ATTRIBUTE_RANGE_START;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaUnsupportedAttributeException;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtAnyIdReq;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtCounter;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtCounterTooSmall;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtEncrData;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtFullauthIdReq;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtIdentity;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtIv;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtMac;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtNextReauthId;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtNonceS;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtNotification;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtPadding;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtPermanentIdReq;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EapSimAkaUnsupportedAttribute;

import java.nio.ByteBuffer;

/**
 * EapSimAkaAttributeFactory is used for creating EapSimAkaAttributes according to their type.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4186">RFC 4186, Extensible Authentication
 * Protocol for Subscriber Identity Modules (EAP-SIM)</a>
 * @see <a href="https://tools.ietf.org/html/rfc4187">RFC 4187, Extensible Authentication
 * Protocol for Authentication and Key Agreement (EAP-AKA)</a>
 * @see <a href="https://www.iana.org/assignments/eap-numbers/eap-numbers.xhtml">EAP SIM/AKA
 * Attributes</a>
 */
public abstract class EapSimAkaAttributeFactory {
    /**
     * Decodes a single EapSimAkaAttribute object from the given ByteBuffer.
     *
     * <p>Decoding logic is based on Attribute definitions in RFC 4186#10 and RFC 4187#10.
     *
     * @param attributeType the attribute type to be decoded
     * @param lengthInBytes the length in bytes of the attribute to be decoded
     * @param byteBuffer The ByteBuffer to parse the current attribute from
     * @return The current EapSimAkaAttribute to be parsed, or EapSimAkaUnsupportedAttribute if the
     *     given attributeType is skippable and unsupported
     * @throws EapSimAkaInvalidAttributeException when a malformatted attribute is attempted to be
     *     decoded
     * @throws EapSimAkaUnsupportedAttributeException when an unsupported, unskippable Attribute is
     *     attempted to be decoded
     */
    EapSimAkaAttribute getAttribute(int attributeType, int lengthInBytes, ByteBuffer byteBuffer)
            throws EapSimAkaInvalidAttributeException, EapSimAkaUnsupportedAttributeException {
        switch (attributeType) {
            // TODO(b/139482157): define optional shared attributes
            case EAP_AT_PERMANENT_ID_REQ:
                return new AtPermanentIdReq(lengthInBytes, byteBuffer);
            case EAP_AT_ANY_ID_REQ:
                return new AtAnyIdReq(lengthInBytes, byteBuffer);
            case EAP_AT_FULLAUTH_ID_REQ:
                return new AtFullauthIdReq(lengthInBytes, byteBuffer);
            case EAP_AT_IDENTITY:
                return new AtIdentity(lengthInBytes, byteBuffer);
            case EAP_AT_PADDING:
                return new AtPadding(lengthInBytes, byteBuffer);
            case EAP_AT_MAC:
                return new AtMac(lengthInBytes, byteBuffer);
            case EAP_AT_COUNTER:
                return new AtCounter(lengthInBytes, byteBuffer);
            case EAP_AT_COUNTER_TOO_SMALL:
                return new AtCounterTooSmall(lengthInBytes, byteBuffer);
            case EAP_AT_NONCE_S:
                return new AtNonceS(lengthInBytes, byteBuffer);
            case EAP_AT_NOTIFICATION:
                return new AtNotification(lengthInBytes, byteBuffer);
            case EAP_AT_CLIENT_ERROR_CODE:
                int errorCode = Short.toUnsignedInt(byteBuffer.getShort());
                return new AtClientErrorCode(lengthInBytes, errorCode);
            case EAP_AT_IV:
                return new AtIv(lengthInBytes, byteBuffer);
            case EAP_AT_ENCR_DATA:
                return new AtEncrData(lengthInBytes, byteBuffer);
            case EAP_AT_NEXT_REAUTH_ID:
                return new AtNextReauthId(lengthInBytes, byteBuffer);

            default:
                if (attributeType >= SKIPPABLE_ATTRIBUTE_RANGE_START) {
                    return new EapSimAkaUnsupportedAttribute(
                            attributeType, lengthInBytes, byteBuffer);
                }

                throw new EapSimAkaUnsupportedAttributeException(
                        "Unexpected EAP Attribute=" + attributeType);
        }
    }

    /**
     * This method exists only for testing.
     *
     * <p>It follows the attributeFactory.getAttribute(ByteBuffer) pattern used by
     * EapSimAttributeFactory and EapAkaAttributeFactory.
     */
    @VisibleForTesting
    public EapSimAkaAttribute getAttribute(ByteBuffer byteBuffer)
            throws EapSimAkaInvalidAttributeException, EapSimAkaUnsupportedAttributeException {
        int attributeType = Byte.toUnsignedInt(byteBuffer.get());

        // Length is given as a multiple of 4x bytes (RFC 4186 Section 8.1)
        int lengthInBytes = Byte.toUnsignedInt(byteBuffer.get()) * LENGTH_SCALING;
        return getAttribute(attributeType, lengthInBytes, byteBuffer);
    }
}
