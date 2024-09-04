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

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Objects;

/** IkeIpv4AddrIdentification represents an IKE entity identification based on IPv4 address. */
public final class IkeIpv4AddrIdentification extends IkeIdentification {
    private static final String IP_ADDRESS_KEY = "ipv4Address";
    /** The IPv4 address. */
    @NonNull public final Inet4Address ipv4Address;

    /**
     * Construct an instance of IkeIpv4AddrIdentification from a decoded inbound packet.
     *
     * @param ipv4AddrBytes IPv4 address in byte array.
     * @throws AuthenticationFailedException for decoding bytes error.
     * @hide
     */
    public IkeIpv4AddrIdentification(byte[] ipv4AddrBytes) throws AuthenticationFailedException {
        super(ID_TYPE_IPV4_ADDR);
        try {
            ipv4Address = (Inet4Address) (Inet4Address.getByAddress(ipv4AddrBytes));
        } catch (ClassCastException | UnknownHostException e) {
            throw new AuthenticationFailedException(e);
        }
    }

    /**
     * Construct an instance of {@link IkeIpv4AddrIdentification} with an IPv4 address.
     *
     * @param address the IPv4 address.
     */
    public IkeIpv4AddrIdentification(@NonNull Inet4Address address) {
        super(ID_TYPE_IPV4_ADDR);
        ipv4Address = address;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public static IkeIpv4AddrIdentification fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        return new IkeIpv4AddrIdentification(
                (Inet4Address) InetAddresses.parseNumericAddress(in.getString(IP_ADDRESS_KEY)));
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
        result.putString(IP_ADDRESS_KEY, ipv4Address.getHostAddress());
        return result;
    }

    /** @hide */
    @Override
    public int hashCode() {
        // idType is also hashed to prevent collisions with other IkeAuthentication subtypes
        return Objects.hash(idType, ipv4Address);
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IkeIpv4AddrIdentification)) return false;

        // idType already verified based on class type; no need to check again.
        return ipv4Address.equals(((IkeIpv4AddrIdentification) o).ipv4Address);
    }

    /** @hide */
    @Override
    public String getIdTypeString() {
        return "IPv4 Address";
    }

    /** @hide */
    @Override
    public void validateEndCertIdOrThrow(X509Certificate endCert)
            throws AuthenticationFailedException {
        // The corresponding SAN type is IP Address as per RFC 7296
        validateEndCertSanOrThrow(endCert, SAN_TYPE_IP_ADDRESS, ipv4Address.getHostAddress());
    }

    /**
     * Retrieve the byte-representation of the IPv4 address.
     *
     * @return the byte-representation of the IPv4 address.
     * @hide
     */
    @Override
    public byte[] getEncodedIdData() {
        return ipv4Address.getAddress();
    }
}
