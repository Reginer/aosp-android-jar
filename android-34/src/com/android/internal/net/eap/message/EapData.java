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

import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_AKA;
import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_AKA_PRIME;
import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_MSCHAP_V2;
import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_SIM;
import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_TTLS;

import android.annotation.IntDef;
import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * EapData represents the data-bytes of an EAP-Packet.
 *
 * <p>EapData objects will always have a Type-value and the Type-Data bytes that follow.
 *
 * EapData objects should be parsed from the Type and Type-Data sections of an EAP Packet, as shown
 * below:
 *
 * +-----------------+-----------------+----------------------------------+
 * |   Code (1B)     | Identifier (1B) |           Length (2B)            |
 * +-----------------+-----------------+----------------------------------+
 * |    Type (1B)    |  Type-Data ...
 * +-----------------+-----
 *
 * @see <a href="https://tools.ietf.org/html/rfc3748#section-4">RFC 3748, Extensible Authentication
 * Protocol (EAP)</a>
 */
public class EapData {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        EAP_IDENTITY,
        EAP_NOTIFICATION,
        EAP_NAK,
        EAP_TYPE_SIM,
        EAP_TYPE_AKA,
        EAP_TYPE_MSCHAP_V2,
        EAP_TYPE_AKA_PRIME,
        EAP_TYPE_TTLS
    })
    public @interface EapType {}

    // EAP Type values defined by IANA
    // https://www.iana.org/assignments/eap-numbers/eap-numbers.xhtml
    public static final int EAP_IDENTITY = 1;
    public static final int EAP_NOTIFICATION = 2;
    public static final int EAP_NAK = 3;
    // EAP_MD5_CHALLENGE unsupported, allowable based on RFC 3748, Section 5.4

    public static final Map<Integer, String> EAP_TYPE_STRING = new HashMap<>();
    static {
        EAP_TYPE_STRING.put(EAP_IDENTITY, "Identity");
        EAP_TYPE_STRING.put(EAP_NOTIFICATION, "Notification");
        EAP_TYPE_STRING.put(EAP_NAK, "Nak");
        EAP_TYPE_STRING.put(EAP_TYPE_SIM, "EAP-SIM");
        EAP_TYPE_STRING.put(EAP_TYPE_AKA, "EAP-AKA");
        EAP_TYPE_STRING.put(EAP_TYPE_MSCHAP_V2, "EAP-MSCHAP-V2");
        EAP_TYPE_STRING.put(EAP_TYPE_AKA_PRIME, "EAP-AKA-PRIME");
        EAP_TYPE_STRING.put(EAP_TYPE_TTLS, "EAP-TTLS");
    }

    private static final Set<Integer> SUPPORTED_TYPES = new HashSet<>();
    static {
        SUPPORTED_TYPES.add(EAP_IDENTITY);
        SUPPORTED_TYPES.add(EAP_NOTIFICATION);
        SUPPORTED_TYPES.add(EAP_NAK);

        // supported EAP Method types
        SUPPORTED_TYPES.add(EAP_TYPE_SIM);
        SUPPORTED_TYPES.add(EAP_TYPE_AKA);
        SUPPORTED_TYPES.add(EAP_TYPE_MSCHAP_V2);
        SUPPORTED_TYPES.add(EAP_TYPE_AKA_PRIME);
        SUPPORTED_TYPES.add(EAP_TYPE_TTLS);
    }

    @EapType public final int eapType;
    public final byte[] eapTypeData;

    public static final EapData NOTIFICATION_DATA = new EapData(EAP_NOTIFICATION, new byte[0]);

    /**
     * Constructs a new EapData instance.
     *
     * @param eapType the {@link EapType} for this EapData instance
     * @param eapTypeData the Type-Data for this EapData instance
     * @throws IllegalArgumentException if eapTypeData is null or if
     *         {@link EapData#isSupportedEapType} is false for the given eapType
     */
    public EapData(@EapType int eapType, @NonNull byte[] eapTypeData) {
        this.eapType = eapType;
        this.eapTypeData = eapTypeData;

        validate();
    }

    /**
     * Gets the length of this EapData object.
     *
     * @return int for the number of bytes this EapData object represents
     */
    public int getLength() {
        // length of byte-array + 1 (for 1B type field)
        return eapTypeData.length + 1;
    }

    /**
     * Returns whether this instance is equal to the given Object o.
     *
     * @param o the Object to be tested for equality
     * @return true iff this instance is equal to the given o
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof EapData)) {
            return false;
        }
        EapData eapData = (EapData) o;
        return eapType == eapData.eapType
                && Arrays.equals(eapTypeData, eapData.eapTypeData);
    }

    /**
     * Returns the hashCode value for this instance.
     *
     * @return the hashCode value for this instance
     */
    @Override
    public int hashCode() {
        return Objects.hash(eapType, Arrays.hashCode(eapTypeData));
    }

    /**
     * Puts the byte-encoding for this EapData instance into the given ByteBuffer.
     *
     * @param b the ByteBuffer to write this EapData instance to
     */
    public void encodeToByteBuffer(ByteBuffer b) {
        b.put((byte) eapType);
        b.put(eapTypeData);
    }

    /**
     * Returns whether the given eapType is a supported {@link EapType} value.
     *
     * @param eapType the value to be checked
     * @return true iff the given eapType is a supported EAP Type
     */
    public static boolean isSupportedEapType(int eapType) {
        return SUPPORTED_TYPES.contains(eapType);
    }

    private void validate() {
        if (this.eapTypeData == null) {
            throw new IllegalArgumentException("EapTypeData byte[] must be non-null");
        }
        if (!isSupportedEapType(this.eapType)) {
            throw new IllegalArgumentException("eapType must be be a valid @EapType value");
        }
    }
}
