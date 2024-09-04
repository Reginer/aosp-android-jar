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

import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_KDF;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_KDF_INPUT;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.LENGTH_SCALING;

import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaUnsupportedAttributeException;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtKdf;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtKdfInput;

import java.nio.ByteBuffer;

/**
 * EapAkaPrimeAttributeFactory is used for creating EAP-AKA' attributes according to their type.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5448">RFC 5448, Improved Extensible Authentication
 *     Protocol Method for 3rd Generation Authentication and Key Agreement (EAP-AKA')</a>
 * @see <a href="https://www.iana.org/assignments/eap-numbers/eap-numbers.xhtml">EAP SIM/AKA
 *     Attributes</a>
 */
public class EapAkaPrimeAttributeFactory extends EapAkaAttributeFactory {
    private static EapAkaPrimeAttributeFactory sInstance = new EapAkaPrimeAttributeFactory();

    private EapAkaPrimeAttributeFactory() {}

    public static EapAkaPrimeAttributeFactory getInstance() {
        return sInstance;
    }

    /**
     * Decodes a single EapSimAkaAttribute object from the given ByteBuffer.
     *
     * <p>Decoding logic is based on Attribute definitions in RFC 5448#10.
     *
     * @param byteBuffer The ByteBuffer to parse the current attribute from
     * @return The current EapSimAkaAttribute to be parsed, or EapSimAkaUnsupportedAttribute if the
     *     given attributeType is skippable and unsupported
     * @throws EapSimAkaInvalidAttributeException when a malformatted attribute is attempted to be
     *     decoded
     * @throws EapSimAkaUnsupportedAttributeException when an unsupported, unskippable Attribute is
     *     attempted to be decoded
     */
    @Override
    public EapSimAkaAttribute getAttribute(ByteBuffer byteBuffer)
            throws EapSimAkaInvalidAttributeException, EapSimAkaUnsupportedAttributeException {
        int attributeType = Byte.toUnsignedInt(byteBuffer.get());

        // Length is given as a multiple of 4x bytes (RFC 4187#8.1)
        int lengthInBytes = Byte.toUnsignedInt(byteBuffer.get()) * LENGTH_SCALING;

        switch (attributeType) {
            case EAP_AT_KDF_INPUT:
                return new AtKdfInput(lengthInBytes, byteBuffer);
            case EAP_AT_KDF:
                return new AtKdf(lengthInBytes, byteBuffer);
            default:
                return super.getAttribute(attributeType, lengthInBytes, byteBuffer);
        }
    }
}
