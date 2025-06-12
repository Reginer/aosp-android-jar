/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.net.ipsec.ike;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.os.PersistableBundle;
import android.util.ArraySet;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * IkeIdentification is abstract base class that represents the common information for all types of
 * IKE entity identification.
 *
 * <p>{@link IkeIdentification} is used in IKE authentication.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.5">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public abstract class IkeIdentification {
    // Set of supported ID types.
    private static final Set<Integer> SUPPORTED_ID_TYPES;

    private static final int INDEX_SAN_TYPE = 0;
    private static final int INDEX_SAN_DATA = 1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        ID_TYPE_IPV4_ADDR,
        ID_TYPE_FQDN,
        ID_TYPE_RFC822_ADDR,
        ID_TYPE_IPV6_ADDR,
        ID_TYPE_DER_ASN1_DN,
        ID_TYPE_KEY_ID
    })
    public @interface IdType {}

    /** @hide */
    public static final int ID_TYPE_IPV4_ADDR = 1;
    /** @hide */
    public static final int ID_TYPE_FQDN = 2;
    /** @hide */
    public static final int ID_TYPE_RFC822_ADDR = 3;
    /** @hide */
    public static final int ID_TYPE_IPV6_ADDR = 5;
    /** @hide */
    public static final int ID_TYPE_DER_ASN1_DN = 9;
    /** @hide */
    public static final int ID_TYPE_KEY_ID = 11;

    static {
        SUPPORTED_ID_TYPES = new ArraySet();
        SUPPORTED_ID_TYPES.add(ID_TYPE_IPV4_ADDR);
        SUPPORTED_ID_TYPES.add(ID_TYPE_FQDN);
        SUPPORTED_ID_TYPES.add(ID_TYPE_RFC822_ADDR);
        SUPPORTED_ID_TYPES.add(ID_TYPE_IPV6_ADDR);
        SUPPORTED_ID_TYPES.add(ID_TYPE_DER_ASN1_DN);
        SUPPORTED_ID_TYPES.add(ID_TYPE_KEY_ID);
    }

    /** @hide Subject Alternative Name Type for RFC822 Email Address defined in RFC 5280 */
    protected static final int SAN_TYPE_RFC822_NAME = 1;
    /** @hide Subject Alternative Name Type for DNS Name defined in RFC 5280 */
    protected static final int SAN_TYPE_DNS = 2;
    /** @hide Subject Alternative Name Type for IP Address defined in RFC 5280 */
    protected static final int SAN_TYPE_IP_ADDRESS = 7;

    private static final String ID_TYPE_KEY = "idType";
    /** @hide */
    public final int idType;

    /** @hide */
    protected IkeIdentification(@IdType int type) {
        idType = type;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public static IkeIdentification fromPersistableBundle(@NonNull PersistableBundle in) {
        int idType = in.getInt(ID_TYPE_KEY);
        switch (idType) {
            case ID_TYPE_IPV4_ADDR:
                return IkeIpv4AddrIdentification.fromPersistableBundle(in);
            case ID_TYPE_FQDN:
                return IkeFqdnIdentification.fromPersistableBundle(in);
            case ID_TYPE_RFC822_ADDR:
                return IkeRfc822AddrIdentification.fromPersistableBundle(in);
            case ID_TYPE_IPV6_ADDR:
                return IkeIpv6AddrIdentification.fromPersistableBundle(in);
            case ID_TYPE_DER_ASN1_DN:
                return IkeDerAsn1DnIdentification.fromPersistableBundle(in);
            case ID_TYPE_KEY_ID:
                return IkeKeyIdIdentification.fromPersistableBundle(in);
            default:
                throw new IllegalArgumentException("Invalid ID type: " + idType);
        }
    }

    /**
     * Serializes this object to a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public PersistableBundle toPersistableBundle() {
        final PersistableBundle result = new PersistableBundle();
        result.putInt(ID_TYPE_KEY, idType);
        return result;
    }

    /**
     * Returns ID type as a String
     *
     * @hide
     */
    public abstract String getIdTypeString();

    /**
     * Check if the end certificate's subject DN or SAN matches this identification
     *
     * @hide
     */
    public abstract void validateEndCertIdOrThrow(X509Certificate endCert)
            throws AuthenticationFailedException;

    /**
     * Check if the end certificate SAN matches the identification
     *
     * <p>According to RFC 7296, the received IKE ID that types are FQDN, IPv4/IPv6 Address and
     * RFC822 Address should match the end certificate Subject Alternative Name (SAN).
     *
     * @hide
     */
    protected void validateEndCertSanOrThrow(
            X509Certificate endCert, int expectedSanType, Object expectedSanData)
            throws AuthenticationFailedException {
        try {
            // Each List is one SAN whose first entry is an Integer that represents a SAN type and
            // second entry is a String or a byte array that represents the SAN data
            Collection<List<?>> allSans = endCert.getSubjectAlternativeNames();
            if (allSans == null) {
                throw new AuthenticationFailedException("End certificate does not contain SAN");
            }

            for (List<?> san : allSans) {
                if ((Integer) san.get(INDEX_SAN_TYPE) == expectedSanType) {
                    Object item = san.get(INDEX_SAN_DATA);
                    if (expectedSanData.equals(item)) {
                        return;
                    }
                }
            }
            throw new AuthenticationFailedException(
                    "End certificate SAN and " + getIdTypeString() + " ID mismatched");
        } catch (CertificateParsingException e) {
            throw new AuthenticationFailedException(e);
        }
    }
    /**
     * Return the encoded identification data in a byte array.
     *
     * @return the encoded identification data.
     * @hide
     */
    public abstract byte[] getEncodedIdData();
}
