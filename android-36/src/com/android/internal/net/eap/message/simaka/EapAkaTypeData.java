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

import android.annotation.NonNull;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.message.EapMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EapAkaTypeData represents the Type Data for an {@link EapMessage} during an EAP-AKA session.
 */
public class EapAkaTypeData extends EapSimAkaTypeData {
    private static final String TAG = EapAkaTypeData.class.getSimpleName();

    // EAP-AKA Subtype values defined by IANA
    // https://www.iana.org/assignments/eapsimaka-numbers/eapsimaka-numbers.xhtml
    public static final int EAP_AKA_CHALLENGE = 1;
    public static final int EAP_AKA_AUTHENTICATION_REJECT = 2;
    public static final int EAP_AKA_SYNCHRONIZATION_FAILURE = 4;
    public static final int EAP_AKA_IDENTITY = 5;
    public static final int EAP_AKA_NOTIFICATION = 12;
    public static final int EAP_AKA_REAUTHENTICATION = 13;
    public static final int EAP_AKA_CLIENT_ERROR = 14;

    public static final Map<Integer, String> EAP_AKA_SUBTYPE_STRING = new HashMap<>();
    static {
        EAP_AKA_SUBTYPE_STRING.put(EAP_AKA_CHALLENGE, "Challenge");
        EAP_AKA_SUBTYPE_STRING.put(EAP_AKA_AUTHENTICATION_REJECT, "Authentication-Reject");
        EAP_AKA_SUBTYPE_STRING.put(EAP_AKA_SYNCHRONIZATION_FAILURE, "Synchronization-Failure");
        EAP_AKA_SUBTYPE_STRING.put(EAP_AKA_IDENTITY, "Identity");
        EAP_AKA_SUBTYPE_STRING.put(EAP_AKA_NOTIFICATION, "Notification");
        EAP_AKA_SUBTYPE_STRING.put(EAP_AKA_REAUTHENTICATION, "Re-authentication");
        EAP_AKA_SUBTYPE_STRING.put(EAP_AKA_CLIENT_ERROR, "Client-Error");
    }

    protected static final Set<Integer> SUPPORTED_SUBTYPES = new HashSet<>();
    static {
        SUPPORTED_SUBTYPES.add(EAP_AKA_CHALLENGE);
        SUPPORTED_SUBTYPES.add(EAP_AKA_AUTHENTICATION_REJECT);
        SUPPORTED_SUBTYPES.add(EAP_AKA_SYNCHRONIZATION_FAILURE);
        SUPPORTED_SUBTYPES.add(EAP_AKA_IDENTITY);
        SUPPORTED_SUBTYPES.add(EAP_AKA_NOTIFICATION);
        SUPPORTED_SUBTYPES.add(EAP_AKA_REAUTHENTICATION);
        SUPPORTED_SUBTYPES.add(EAP_AKA_CLIENT_ERROR);
    }

    private static final EapAkaTypeDataDecoder sTypeDataDecoder = new EapAkaTypeDataDecoder();

    @VisibleForTesting
    public EapAkaTypeData(int eapSubType, LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap) {
        super(eapSubType, attributeMap);
    }

    protected EapAkaTypeData(
            int eapSubType,
            LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap,
            byte[] reservedBytes) {
        super(eapSubType, attributeMap, reservedBytes);
    }

    /**
     * Creates and returns an EapAkaTypeData instance with the given subtype and attributes.
     *
     * @param eapSubtype the subtype for the EAP-AKA type data
     * @param attributes the List of EapSimAkaAttributes to be included in this type data
     */
    public EapAkaTypeData(int eapSubtype, List<EapSimAkaAttribute> attributes) {
        super(eapSubtype, new LinkedHashMap<>());

        if (!SUPPORTED_SUBTYPES.contains(eapSubtype)) {
            throw new IllegalArgumentException("Invalid subtype for EAP-AKA: " + eapSubtype);
        }

        for (EapSimAkaAttribute attribute : attributes) {
            if (attributeMap.containsKey(attribute.attributeType)) {
                throw new IllegalArgumentException(
                        "Duplicate attribute in attributes: " + attribute.attributeType);
            }
            attributeMap.put(attribute.attributeType, attribute);
        }
    }

    public static EapAkaTypeDataDecoder getEapAkaTypeDataDecoder() {
        return sTypeDataDecoder;
    }

    /**
     * EapAkaTypeDataDecoder will be used for decoding {@link EapAkaTypeData} objects.
     */
    public static class EapAkaTypeDataDecoder extends EapSimAkaTypeDataDecoder<EapAkaTypeData> {
        private static final String TAG = EapAkaTypeDataDecoder.class.getSimpleName();
        private static final String EAP_METHOD = "EAP-AKA";

        protected EapAkaTypeDataDecoder() {
            super(
                    TAG,
                    EAP_METHOD,
                    SUPPORTED_SUBTYPES,
                    EapAkaAttributeFactory.getInstance(),
                    EAP_AKA_SUBTYPE_STRING);
        }

        /**
         * Decodes the given byte-array into a DecodeResult object.
         *
         * @param typeData the byte-encoding of the EapAkaTypeData to be parsed
         * @return a DecodeResult object. If the decoding is successful, this will encapsulate an
         *         EapAkaTypeData instance representing the data stored in typeData. Otherwise, it
         *         will contain the relevant AtClientErrorCode for the decoding error.
         */
        public DecodeResult<EapAkaTypeData> decode(@NonNull byte[] typeData) {
            return super.decode(typeData);
        }

        @Override
        protected EapAkaTypeData getInstance(
                int eapSubtype,
                LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap,
                byte[] reservedBytes) {
            return new EapAkaTypeData(eapSubtype, attributeMap, reservedBytes);
        }
    }
}
