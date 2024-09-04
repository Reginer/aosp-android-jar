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

import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_NONCE_MT;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_RAND;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_SELECTED_VERSION;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_VERSION_LIST;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.LENGTH_SCALING;

import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaUnsupportedAttributeException;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtNonceMt;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtRandSim;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtSelectedVersion;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtVersionList;

import java.nio.ByteBuffer;

/**
 * EapSimAttributeFactory is used for creating EAP-SIM attributes according to their type.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4186">RFC 4186, Extensible Authentication
 * Protocol for Subscriber Identity Modules (EAP-SIM)</a>
 * @see <a href="https://www.iana.org/assignments/eap-numbers/eap-numbers.xhtml">EAP SIM/AKA
 * Attributes</a>
 */
public class EapSimAttributeFactory extends EapSimAkaAttributeFactory {
    private static EapSimAttributeFactory sInstance = new EapSimAttributeFactory();

    private EapSimAttributeFactory() {
    }

    public static EapSimAttributeFactory getInstance() {
        return sInstance;
    }

    /**
     * Decodes a single EapSimAkaAttribute object from the given ByteBuffer.
     *
     * <p>Decoding logic is based on Attribute definitions in RFC 4186#10.
     *
     * @param byteBuffer The ByteBuffer to parse the current attribute from
     * @return The current EapSimAkaAttribute to be parsed, or EapSimAkaUnsupportedAttribute if the
     *         given attributeType is skippable and unsupported
     * @throws EapSimAkaInvalidAttributeException when a malformatted attribute is attempted to be
     *         decoded
     * @throws EapSimAkaUnsupportedAttributeException when an unsupported, unskippable Attribute is
     *         attempted to be decoded
     */
    public EapSimAkaAttribute getAttribute(ByteBuffer byteBuffer) throws
            EapSimAkaInvalidAttributeException, EapSimAkaUnsupportedAttributeException {
        int attributeType = Byte.toUnsignedInt(byteBuffer.get());

        // Length is given as a multiple of 4x bytes (RFC 4186 Section 8.1)
        int lengthInBytes = Byte.toUnsignedInt(byteBuffer.get()) * LENGTH_SCALING;

        switch (attributeType) {
            // TODO(b/134670528): add case statements for all EAP-SIM attributes
            case EAP_AT_VERSION_LIST:
                return new AtVersionList(lengthInBytes, byteBuffer);
            case EAP_AT_SELECTED_VERSION:
                int selectedVersion = Short.toUnsignedInt(byteBuffer.getShort());
                return new AtSelectedVersion(lengthInBytes, selectedVersion);
            case EAP_AT_NONCE_MT:
                return new AtNonceMt(lengthInBytes, byteBuffer);
            case EAP_AT_RAND:
                return new AtRandSim(lengthInBytes, byteBuffer);
            default:
                return super.getAttribute(attributeType, lengthInBytes, byteBuffer);
        }
    }
}
