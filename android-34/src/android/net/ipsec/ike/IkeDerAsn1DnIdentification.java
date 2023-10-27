/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.annotation.NonNull;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.os.PersistableBundle;

import com.android.server.vcn.util.PersistableBundleUtils;

import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.security.auth.x500.X500Principal;

/**
 * This class represents an IKE entity ID based on a DER encoded ASN.1 X.500 Distinguished Name.
 *
 * <p>An example might be "CN=ike.test.android.net, O=Android, C=US".
 */
public final class IkeDerAsn1DnIdentification extends IkeIdentification {
    private static final String DER_ASN1_DN_KEY = "derAsn1Dn";
    /** The ASN.1 X.500 Distinguished Name */
    @NonNull public final X500Principal derAsn1Dn;

    /**
     * Construct an instance of IkeDerAsn1DnIdentification from a decoded inbound packet.
     *
     * @param derAsn1Dn the ASN.1 X.500 Distinguished Name that has been DER encoded.
     * @hide
     */
    public IkeDerAsn1DnIdentification(byte[] derAsn1DnBytes) throws AuthenticationFailedException {
        super(ID_TYPE_DER_ASN1_DN);

        Objects.requireNonNull(derAsn1DnBytes, "derAsn1DnBytes not provided");

        try {
            derAsn1Dn = new X500Principal(derAsn1DnBytes);
        } catch (IllegalArgumentException e) {
            // Incorrect form for DN
            throw new AuthenticationFailedException(e);
        }
    }

    /**
     * Construct an instance of IkeDerAsn1DnIdentification with an ASN.1 X.500 Distinguished Name
     *
     * @param derAsn1Dn the ASN.1 X.500 Distinguished Name.
     */
    public IkeDerAsn1DnIdentification(@NonNull X500Principal derAsn1Dn) {
        super(ID_TYPE_DER_ASN1_DN);

        Objects.requireNonNull(derAsn1Dn, "derAsn1Dn not provided");
        this.derAsn1Dn = derAsn1Dn;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public static IkeDerAsn1DnIdentification fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        PersistableBundle dnBundle = in.getPersistableBundle(DER_ASN1_DN_KEY);
        Objects.requireNonNull(dnBundle, "ASN1 DN bundle is null");

        return new IkeDerAsn1DnIdentification(
                new X500Principal(PersistableBundleUtils.toByteArray(dnBundle)));
    }
    /**
     * Serializes this object to a PersistableBundle
     *
     * @hide
     */
    @Override
    @NonNull
    public PersistableBundle toPersistableBundle() {
        final PersistableBundle result = super.toPersistableBundle();
        result.putPersistableBundle(
                DER_ASN1_DN_KEY, PersistableBundleUtils.fromByteArray(derAsn1Dn.getEncoded()));
        return result;
    }

    /** @hide */
    @Override
    public int hashCode() {
        // idType is also hashed to prevent collisions with other IkeAuthentication subtypes
        return Objects.hash(idType, derAsn1Dn);
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IkeDerAsn1DnIdentification)) return false;

        // idType already verified based on class type; no need to check again.
        return derAsn1Dn.equals(((IkeDerAsn1DnIdentification) o).derAsn1Dn);
    }

    /** @hide */
    @Override
    public String getIdTypeString() {
        return "DER ASN.1 DN";
    }

    /** @hide */
    @Override
    public void validateEndCertIdOrThrow(X509Certificate endCert)
            throws AuthenticationFailedException {
        if (!derAsn1Dn.equals(endCert.getSubjectX500Principal())) {
            throw new AuthenticationFailedException(
                    "End cert subject DN and DER ASN1 DN ID mismtached");
        }
    }

    /**
     * Retrieve the byte-representation of the ASN.1 X.500 DN.
     *
     * @return the byte-representation of the ASN.1 X.500 DN.
     * @hide
     */
    @Override
    public byte[] getEncodedIdData() {
        return derAsn1Dn.getEncoded();
    }
}
