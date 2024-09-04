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
 * IkeRfc822AddrIdentification represents an IKE entity identification based on a fully-qualified
 * RFC 822 email address ID (e.g. ike@android.com).
 */
public final class IkeRfc822AddrIdentification extends IkeIdentification {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String RFC822_NAME_KEY = "rfc822Name";

    /** The fully-qualified RFC 822 email address. */
    @NonNull public final String rfc822Name;

    /**
     * Construct an instance of IkeRfc822AddrIdentification from a decoded inbound packet.
     *
     * @param rfc822NameBytes fully-qualified RFC 822 email address in byte array.
     * @hide
     */
    public IkeRfc822AddrIdentification(byte[] rfc822NameBytes) {
        super(ID_TYPE_RFC822_ADDR);
        rfc822Name = new String(rfc822NameBytes, UTF8);
    }

    /**
     * Construct an instance of {@link IkeRfc822AddrIdentification} with a fully-qualified RFC 822
     * email address.
     *
     * @param rfc822Name the fully-qualified RFC 822 email address.
     */
    public IkeRfc822AddrIdentification(@NonNull String rfc822Name) {
        super(ID_TYPE_RFC822_ADDR);
        this.rfc822Name = rfc822Name;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public static IkeRfc822AddrIdentification fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        return new IkeRfc822AddrIdentification(in.getString(RFC822_NAME_KEY));
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
        result.putString(RFC822_NAME_KEY, rfc822Name);
        return result;
    }

    /** @hide */
    @Override
    public int hashCode() {
        // idType is also hashed to prevent collisions with other IkeAuthentication subtypes
        return Objects.hash(idType, rfc822Name);
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IkeRfc822AddrIdentification)) return false;

        // idType already verified based on class type; no need to check again.
        return rfc822Name.equals(((IkeRfc822AddrIdentification) o).rfc822Name);
    }

    /** @hide */
    @Override
    public String getIdTypeString() {
        return "RFC822 Address";
    }

    /** @hide */
    @Override
    public void validateEndCertIdOrThrow(X509Certificate endCert)
            throws AuthenticationFailedException {
        // The corresponding SAN type is RFC822 Name as per RFC 7296
        validateEndCertSanOrThrow(endCert, SAN_TYPE_RFC822_NAME, rfc822Name);
    }

    /**
     * Retrieve the byte-representation of the the RFC 822 email address.
     *
     * @return the byte-representation of the RFC 822 email address.
     * @hide
     */
    @Override
    public byte[] getEncodedIdData() {
        return rfc822Name.getBytes(UTF8);
    }
}
