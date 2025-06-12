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
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * EapAkaPrimeTypeData represents the Type Data for an {@link EapMessage} during an EAP-AKA'
 * session.
 */
public class EapAkaPrimeTypeData extends EapAkaTypeData {
    private static final EapAkaPrimeTypeDataDecoder sTypeDataDecoder =
            new EapAkaPrimeTypeDataDecoder();

    @VisibleForTesting
    EapAkaPrimeTypeData(int eapSubType, LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap) {
        super(eapSubType, attributeMap);
    }

    private EapAkaPrimeTypeData(
            int eapSubType,
            LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap,
            byte[] reservedBytes) {
        super(eapSubType, attributeMap, reservedBytes);
    }

    /**
     * Creates and returns an EapAkaPrimeTypeData instance with the given subtype and attributes.
     *
     * @param eapSubtype the subtype for the EAP-AKA type data
     * @param attributes the List of EapSimAkaAttributes to be included in this type data
     */
    public EapAkaPrimeTypeData(int eapSubtype, List<EapSimAkaAttribute> attributes) {
        super(eapSubtype, attributes);
    }

    public static EapAkaPrimeTypeDataDecoder getEapAkaPrimeTypeDataDecoder() {
        return sTypeDataDecoder;
    }

    /**
     * EapAkaTypeDataDecoder will be used for decoding {@link EapAkaPrimeTypeData} objects.
     */
    public static class EapAkaPrimeTypeDataDecoder
            extends EapSimAkaTypeDataDecoder<EapAkaTypeData> {
        private static final String TAG = EapAkaPrimeTypeDataDecoder.class.getSimpleName();
        private static final String EAP_METHOD = "EAP-AKA'";

        protected EapAkaPrimeTypeDataDecoder() {
            super(
                    TAG,
                    EAP_METHOD,
                    SUPPORTED_SUBTYPES,
                    EapAkaPrimeAttributeFactory.getInstance(),
                    EAP_AKA_SUBTYPE_STRING);
        }

        /**
         * Decodes the given byte-array into a DecodeResult object.
         *
         * <p>Note that <b>only 1 KDF value</b> is allowed. If multiple AT_KDF attributes are
         * supplied, a {@link DecodeResult} wrapping a {@link AtClientErrorCode#UNABLE_TO_PROCESS}
         * will be returned.
         *
         * @param typeData the byte-encoding of the EapAkaPrimeTypeData to be parsed
         * @return a DecodeResult object. If the decoding is successful, this will encapsulate an
         *     EapAkaPrimeTypeData instance representing the data stored in typeData. Otherwise, it
         *     will contain the relevant AtClientErrorCode for the decoding error.
         */
        public DecodeResult<EapAkaTypeData> decode(@NonNull byte[] typeData) {
            return super.decode(typeData);
        }

        @Override
        protected EapAkaPrimeTypeData getInstance(
                int eapSubtype,
                LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap,
                byte[] reservedBytes) {
            return new EapAkaPrimeTypeData(eapSubtype, attributeMap, reservedBytes);
        }
    }
}
