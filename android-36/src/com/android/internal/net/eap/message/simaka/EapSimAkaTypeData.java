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

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_ATTRIBUTE_STRING;

import android.annotation.NonNull;

import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaUnsupportedAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimInvalidAtRandException;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EapSimAkaUnsupportedAttribute;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * EapSimTypeData represents the Type Data for an {@link EapMessage} during an EAP-SIM session.
 */
public abstract class EapSimAkaTypeData {
    private static final int MIN_LEN_BYTES = 3; // subtype (1B) + reserved bytes (2B)
    private static final int RESERVED_BYTES_LEN = 2; // RFC 4186#8.1, RFC 4187#8.1

    public final int eapSubtype;

    /** Save (but ignore) the 2B reserved section after EAP-Type and EAP-SIM/AKA subtype. */
    final byte[] mReservedBytes;

    // LinkedHashMap used to preserve encoded ordering of attributes. This is necessary for checking
    // the MAC value for the message
    public final LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap;

    protected EapSimAkaTypeData(
            int eapSubType, LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap) {
        this(eapSubType, attributeMap, new byte[RESERVED_BYTES_LEN]);
    }

    public EapSimAkaTypeData(
            int eapSubType,
            LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap,
            byte[] reservedBytes) {
        this.eapSubtype = eapSubType;
        this.attributeMap = attributeMap;

        mReservedBytes = reservedBytes.clone();
    }

    /**
     * Creates and returns the byte-array encoding of this EapSimTypeData instance.
     *
     * @return byte[] representing the byte-encoding of this EapSimTypeData instance.
     */
    public byte[] encode() {
        int lengthInBytes = MIN_LEN_BYTES;
        for (EapSimAkaAttribute attribute : attributeMap.values()) {
            lengthInBytes += attribute.lengthInBytes;
        }

        ByteBuffer output = ByteBuffer.allocate(lengthInBytes);
        output.put((byte) eapSubtype);

        // two reserved bytes (RFC 4186#8.1, RFC 4187#8.1)
        output.put(mReservedBytes);

        for (EapSimAkaAttribute attribute : attributeMap.values()) {
            attribute.encode(output);
        }

        return output.array();
    }

    /**
     * Class for decoding EAP-SIM and EAP-AKA type-datas.
     *
     * @param <T> The EapSimAkaTypeData type that the EapSimAkaTypeDataDecoder will be decoding
     */
    public abstract static class EapSimAkaTypeDataDecoder<T extends EapSimAkaTypeData> {
        private final String mTAG;
        private final String mEapMethod;
        private final Set<Integer> mSupportedSubtypes;
        private final EapSimAkaAttributeFactory mAttributeFactory;
        private final Map<Integer, String> mEapSubtypeStrings;

        EapSimAkaTypeDataDecoder(
                String tag,
                String eapMethod,
                Set<Integer> supportedSubtypes,
                EapSimAkaAttributeFactory eapSimAkaAttributeFactory,
                Map<Integer, String> eapSubtypeStrings) {
            this.mTAG = tag;
            this.mEapMethod = eapMethod;
            this.mSupportedSubtypes = supportedSubtypes;
            this.mAttributeFactory = eapSimAkaAttributeFactory;
            this.mEapSubtypeStrings = eapSubtypeStrings;
        }

        protected DecodeResult<T> decode(@NonNull byte[] typeData) {
            if (typeData == null) {
                LOG.d(mTAG, "Invalid EAP Type-Data");
                return new DecodeResult<>(AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(typeData);
            try {
                int eapSubType = Byte.toUnsignedInt(byteBuffer.get());
                if (!mSupportedSubtypes.contains(eapSubType)) {
                    LOG.d(mTAG, "Invalid EAP Type-Data");
                    return new DecodeResult<>(AtClientErrorCode.UNABLE_TO_PROCESS);
                }

                // next two bytes are reserved (RFC 4186#8.1, RFC 4187#8.1)
                byte[] reservedBytes = new byte[RESERVED_BYTES_LEN];
                byteBuffer.get(reservedBytes);

                // read attributes
                LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap = new LinkedHashMap<>();
                while (byteBuffer.hasRemaining()) {
                    EapSimAkaAttribute attribute = mAttributeFactory.getAttribute(byteBuffer);

                    if (attributeMap.containsKey(attribute.attributeType)) {
                        // Duplicate attributes are not allowed (RFC 4186#6.3.1, RFC 4187#6.3.1)
                        LOG.e(mTAG, "Duplicate attribute in parsed EAP-Message");
                        return new DecodeResult<>(AtClientErrorCode.UNABLE_TO_PROCESS);
                    }

                    if (attribute instanceof EapSimAkaUnsupportedAttribute) {
                        LOG.d(mTAG, "Unsupported EAP attribute during decoding: "
                                + attribute.attributeType);
                    }
                    attributeMap.put(attribute.attributeType, attribute);
                }

                T eapSimAkaTypeData = getInstance(eapSubType, attributeMap, reservedBytes);

                logDecodedEapSimAkaTypeData(eapSimAkaTypeData);

                return new DecodeResult<>(eapSimAkaTypeData);
            } catch (EapSimInvalidAtRandException ex) {
                LOG.e(mTAG, "Invalid AtRand attribute", ex);
                return new DecodeResult<>(AtClientErrorCode.INSUFFICIENT_CHALLENGES);
            } catch (EapSimAkaInvalidAttributeException | BufferUnderflowException ex) {
                LOG.e(mTAG, "Incorrectly formatted attribute", ex);
                return new DecodeResult<>(AtClientErrorCode.UNABLE_TO_PROCESS);
            } catch (EapSimAkaUnsupportedAttributeException ex) {
                LOG.e(mTAG, "Unrecognized, non-skippable attribute encountered", ex);
                return new DecodeResult<>(AtClientErrorCode.UNABLE_TO_PROCESS);
            }
        }

        protected abstract T getInstance(
                int eapSubType,
                LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap,
                byte[] reservedBytes);

        private void logDecodedEapSimAkaTypeData(EapSimAkaTypeData eapSimAkaTypeData) {
            StringBuilder msg = new StringBuilder();
            msg.append("Decoded ");
            msg.append(mEapMethod);
            msg.append(" type data: ");

            msg.append(mEapSubtypeStrings.getOrDefault(eapSimAkaTypeData.eapSubtype, "Unknown"));
            msg.append(" attributes=[ ");
            for (int attributeType : eapSimAkaTypeData.attributeMap.keySet()) {
                msg.append(
                        EAP_ATTRIBUTE_STRING.getOrDefault(attributeType,
                                "Unknown(" + attributeType + ")"));
                msg.append(" ");
            }
            msg.append("]");
            LOG.i(mTAG, msg.toString());
        }
    }

    /**
     * DecodeResult represents the result from calling EapSimTypeDataDecoder.decode(). It will
     * contain either a decoded EapSimTypeData or the relevant AtClientErrorCode.
     *
     * @param <T> The EapSimAkaTypeData type that is wrapped in this DecodeResult
     */
    public static class DecodeResult<T extends EapSimAkaTypeData> {
        public final T eapTypeData;
        public final EapSimAkaAttribute.AtClientErrorCode atClientErrorCode;

        public DecodeResult(T eapTypeData) {
            this.eapTypeData = eapTypeData;
            this.atClientErrorCode = null;
        }

        public DecodeResult(EapSimAkaAttribute.AtClientErrorCode atClientErrorCode) {
            this.atClientErrorCode = atClientErrorCode;
            eapTypeData = null;
        }

        public boolean isSuccessfulDecode() {
            return eapTypeData != null;
        }
    }
}
