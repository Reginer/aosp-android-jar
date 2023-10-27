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
import android.net.InetAddresses;
import android.net.ipsec.ike.exceptions.AuthenticationFailedException;
import android.os.PersistableBundle;

import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Objects;

/** IkeIpv6AddrIdentification represents an IKE entity identification based on IPv6 address. */
public class IkeIpv6AddrIdentification extends IkeIdentification {
    private static final String IP_ADDRESS_KEY = "ipv6Address";

    /** The IPv6 address. */
    @NonNull public final Inet6Address ipv6Address;

    /**
     * Construct an instance of IkeIpv6AddrIdentification from a decoded inbound packet.
     *
     * @param ipv6AddrBytes IPv6 address in byte array.
     * @throws AuthenticationFailedException for decoding bytes error.
     * @hide
     */
    public IkeIpv6AddrIdentification(byte[] ipv6AddrBytes) throws AuthenticationFailedException {
        super(ID_TYPE_IPV6_ADDR);
        try {
            ipv6Address = (Inet6Address) (Inet6Address.getByAddress(ipv6AddrBytes));
        } catch (ClassCastException | UnknownHostException e) {
            throw new AuthenticationFailedException(e);
        }
    }

    /**
     * Construct an instance of {@link IkeIpv6AddrIdentification} with an IPv6 address.
     *
     * @param address the IPv6 address.
     */
    public IkeIpv6AddrIdentification(@NonNull Inet6Address address) {
        super(ID_TYPE_IPV6_ADDR);
        ipv6Address = address;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public static IkeIpv6AddrIdentification fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        return new IkeIpv6AddrIdentification(
                (Inet6Address) InetAddresses.parseNumericAddress(in.getString(IP_ADDRESS_KEY)));
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
        result.putString(IP_ADDRESS_KEY, ipv6Address.getHostAddress());
        return result;
    }

    /** @hide */
    @Override
    public int hashCode() {
        // idType is also hashed to prevent collisions with other IkeAuthentication subtypes
        return Objects.hash(idType, ipv6Address);
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IkeIpv6AddrIdentification)) return false;

        // idType already verified based on class type; no need to check again.
        return ipv6Address.equals(((IkeIpv6AddrIdentification) o).ipv6Address);
    }

    /** @hide */
    @Override
    public String getIdTypeString() {
        return "IPv6 Address";
    }

    /** @hide */
    @Override
    public void validateEndCertIdOrThrow(X509Certificate endCert)
            throws AuthenticationFailedException {
        // The corresponding SAN type is IP Address as per RFC 7296
        validateEndCertSanOrThrow(endCert, SAN_TYPE_IP_ADDRESS, ipv6Address.getHostAddress());
    }

    /**
     * Retrieve the byte-representation of the IPv6 address.
     *
     * @return the byte-representation of the IPv6 address.
     * @hide
     */
    @Override
    public byte[] getEncodedIdData() {
        return ipv6Address.getAddress();
    }
}
