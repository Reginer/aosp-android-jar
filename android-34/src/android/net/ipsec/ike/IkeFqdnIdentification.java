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

package android.net.ipsec.ike;

import android.annotation.NonNull;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.os.PersistableBundle;

import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * IkeFqdnIdentification represents an IKE entity identification based on a fully-qualified domain
 * name (FQDN). An example might be ike.android.com
 */
public class IkeFqdnIdentification extends IkeIdentification {
    private static final Charset ASCII = Charset.forName("US-ASCII");

    private static final String FQDN_KEY = "fqdn";

    /** The fully-qualified domain name(FQDN). */
    @NonNull public final String fqdn;

    /**
     * Construct an instance of IkeFqdnIdentification from a decoded inbound packet.
     *
     * @param fqdnBytes FQDN in byte array.
     * @hide
     */
    public IkeFqdnIdentification(byte[] fqdnBytes) {
        super(ID_TYPE_FQDN);
        fqdn = new String(fqdnBytes, ASCII);
    }

    /**
     * Construct an instance of {@link IkeFqdnIdentification} with a fully-qualified domain name.
     *
     * @param fqdn the fully-qualified domain name (FQDN).  Must contain only US-ASCII characters,
     * otherwise an IllegalArugmentException will be thrown.
     */
    public IkeFqdnIdentification(@NonNull String fqdn) {
        super(ID_TYPE_FQDN);
        if (!ASCII.newEncoder().canEncode(fqdn)) {
            throw new IllegalArgumentException("Non US-ASCII character set used");
        }

        this.fqdn = fqdn;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public static IkeFqdnIdentification fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        return new IkeFqdnIdentification(in.getString(FQDN_KEY));
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
        result.putString(FQDN_KEY, fqdn);
        return result;
    }

    /** @hide */
    @Override
    public int hashCode() {
        // idType is also hashed to prevent collisions with other IkeAuthentication subtypes
        return Objects.hash(idType, fqdn);
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IkeFqdnIdentification)) return false;

        // idType already verified based on class type; no need to check again.
        return fqdn.equals(((IkeFqdnIdentification) o).fqdn);
    }

    /** @hide */
    @Override
    public String getIdTypeString() {
        return "FQDN";
    }

    /** @hide */
    @Override
    public void validateEndCertIdOrThrow(X509Certificate endCert)
            throws AuthenticationFailedException {
        // The corresponding SAN type is DNS Name as per RFC 7296
        validateEndCertSanOrThrow(endCert, SAN_TYPE_DNS, fqdn);
    }

    /**
     * Retrieve the byte-representation of the FQDN.
     *
     * @return the byte-representation of the FQDN.
     * @hide
     */
    @Override
    public byte[] getEncodedIdData() {
        return fqdn.getBytes(ASCII);
    }
}
