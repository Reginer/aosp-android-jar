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
 * EapSimTypeData represents the Type Data for an {@link EapMessage} during an EAP-SIM session.
 */
public class EapSimTypeData extends EapSimAkaTypeData {
    private static final String TAG = EapSimTypeData.class.getSimpleName();

    // EAP-SIM Subtype values defined by IANA
    // https://www.iana.org/assignments/eapsimaka-numbers/eapsimaka-numbers.xhtml
    public static final int EAP_SIM_START = 10;
    public static final int EAP_SIM_CHALLENGE = 11;
    public static final int EAP_SIM_NOTIFICATION = 12;
    public static final int EAP_SIM_REAUTHENTICATION = 13;
    public static final int EAP_SIM_CLIENT_ERROR = 14;

    public static final Map<Integer, String> EAP_SIM_SUBTYPE_STRING = new HashMap<>();
    static {
        EAP_SIM_SUBTYPE_STRING.put(EAP_SIM_START, "Start");
        EAP_SIM_SUBTYPE_STRING.put(EAP_SIM_CHALLENGE, "Challenge");
        EAP_SIM_SUBTYPE_STRING.put(EAP_SIM_NOTIFICATION, "Notification");
        EAP_SIM_SUBTYPE_STRING.put(EAP_SIM_REAUTHENTICATION, "Re-authentication");
        EAP_SIM_SUBTYPE_STRING.put(EAP_SIM_CLIENT_ERROR, "Client-Error");
    }

    private static final Set<Integer> SUPPORTED_SUBTYPES = new HashSet<>();
    static {
        SUPPORTED_SUBTYPES.add(EAP_SIM_START);
        SUPPORTED_SUBTYPES.add(EAP_SIM_CHALLENGE);
        SUPPORTED_SUBTYPES.add(EAP_SIM_NOTIFICATION);
        SUPPORTED_SUBTYPES.add(EAP_SIM_REAUTHENTICATION);
        SUPPORTED_SUBTYPES.add(EAP_SIM_CLIENT_ERROR);
    }

    private static final EapSimTypeDataDecoder sTypeDataDecoder = new EapSimTypeDataDecoder();

    @VisibleForTesting
    public EapSimTypeData(int eapSubType, LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap) {
        super(eapSubType, attributeMap);
    }

    private EapSimTypeData(
            int eapSubType,
            LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap,
            byte[] reservedBytes) {
        super(eapSubType, attributeMap, reservedBytes);
    }

    public EapSimTypeData(int eapSubtype, List<EapSimAkaAttribute> attributes) {
        super(eapSubtype, new LinkedHashMap<>());

        if (!SUPPORTED_SUBTYPES.contains(eapSubtype)) {
            throw new IllegalArgumentException("Invalid subtype for EAP-SIM: " + eapSubtype);
        }

        for (EapSimAkaAttribute attribute : attributes) {
            if (attributeMap.containsKey(attribute.attributeType)) {
                throw new IllegalArgumentException(
                        "Duplicate attribute in attributes: " + attribute.attributeType);
            }
            attributeMap.put(attribute.attributeType, attribute);
        }
    }

    public static EapSimTypeDataDecoder getEapSimTypeDataDecoder() {
        return sTypeDataDecoder;
    }

    /**
     * EapSimTypeDataDecoder will be used for decoding {@link EapSimTypeData} objects.
     */
    public static class EapSimTypeDataDecoder extends EapSimAkaTypeDataDecoder<EapSimTypeData> {
        private static final String TAG = EapSimTypeDataDecoder.class.getSimpleName();
        private static final String EAP_METHOD = "EAP-SIM";

        protected EapSimTypeDataDecoder() {
            super(
                    TAG,
                    EAP_METHOD,
                    SUPPORTED_SUBTYPES,
                    EapSimAttributeFactory.getInstance(),
                    EAP_SIM_SUBTYPE_STRING);
        }

        /**
         * Decodes the given byte-array into a DecodeResult object.
         *
         * @param typeData the byte-encoding of the EapSimTypeData to be parsed
         * @return a DecodeResult object. If the decoding is successful, this will encapsulate an
         *         EapSimTypeData instance representing the data stored in typeData. Otherwise, it
         *         will contain the relevant AtClientErrorCode for the decoding error.
         */
        public DecodeResult<EapSimTypeData> decode(@NonNull byte[] typeData) {
            return super.decode(typeData);
        }

        @Override
        protected EapSimTypeData getInstance(
                int eapSubtype,
                LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap,
                byte[] reservedBytes) {
            return new EapSimTypeData(eapSubtype, attributeMap, reservedBytes);
        }
    }
}
