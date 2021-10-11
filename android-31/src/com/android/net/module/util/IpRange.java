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

package com.android.net.module.util;

import static com.android.internal.annotations.VisibleForTesting.Visibility;

import android.annotation.NonNull;
import android.net.IpPrefix;

import com.android.internal.annotations.VisibleForTesting;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * This class represents an IP range, i.e., a contiguous block of IP addresses defined by a starting
 * and ending IP address. These addresses may not be power-of-two aligned.
 *
 * <p>Conversion to prefixes are deterministic, and will always return the same set of {@link
 * IpPrefix}(es). Ordering of IpPrefix instances is not guaranteed.
 *
 * @hide
 */
public final class IpRange {
    private static final int SIGNUM_POSITIVE = 1;

    private final byte[] mStartAddr;
    private final byte[] mEndAddr;

    public IpRange(@NonNull InetAddress startAddr, @NonNull InetAddress endAddr) {
        Objects.requireNonNull(startAddr, "startAddr must not be null");
        Objects.requireNonNull(endAddr, "endAddr must not be null");

        if (!startAddr.getClass().equals(endAddr.getClass())) {
            throw new IllegalArgumentException("Invalid range: Address family mismatch");
        }
        if (addrToBigInteger(startAddr.getAddress()).compareTo(
                addrToBigInteger(endAddr.getAddress())) >= 0) {
            throw new IllegalArgumentException(
                    "Invalid range; start address must be before end address");
        }

        mStartAddr = startAddr.getAddress();
        mEndAddr = endAddr.getAddress();
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public IpRange(@NonNull IpPrefix prefix) {
        Objects.requireNonNull(prefix, "prefix must not be null");

        // Use masked address from IpPrefix to zero out lower order bits.
        mStartAddr = prefix.getRawAddress();

        // Set all non-prefix bits to max.
        mEndAddr = prefix.getRawAddress();
        for (int bitIndex = prefix.getPrefixLength(); bitIndex < 8 * mEndAddr.length; ++bitIndex) {
            mEndAddr[bitIndex / 8] |= (byte) (0x80 >> (bitIndex % 8));
        }
    }

    private static InetAddress getAsInetAddress(byte[] address) {
        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            // Cannot happen. InetAddress.getByAddress can only throw an exception if the byte
            // array is the wrong length, but are always generated from InetAddress(es).
            throw new IllegalArgumentException("Address is invalid");
        }
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public InetAddress getStartAddr() {
        return getAsInetAddress(mStartAddr);
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public InetAddress getEndAddr() {
        return getAsInetAddress(mEndAddr);
    }

    /**
     * Converts this IP range to a list of IpPrefix instances.
     *
     * <p>This method outputs the IpPrefix instances for use in the routing architecture.
     *
     * <p>For example, the range 192.0.2.4 - 192.0.3.1 converts to the following prefixes:
     *
     * <ul>
     *   <li>192.0.2.128/25
     *   <li>192.0.2.64/26
     *   <li>192.0.2.32/27
     *   <li>192.0.2.16/28
     *   <li>192.0.2.8/29
     *   <li>192.0.2.4/30
     *   <li>192.0.3.0/31
     * </ul>
     */
    public List<IpPrefix> asIpPrefixes() {
        final boolean isIpv6 = (mStartAddr.length == 16);
        final List<IpPrefix> result = new ArrayList<>();
        final Queue<IpPrefix> workingSet = new LinkedList<>();

        // Start with the any-address. Inet4/6Address.ANY requires compiling against
        // core-platform-api.
        workingSet.add(new IpPrefix(isIpv6 ? getAsInetAddress(new byte[16]) /* IPv6_ANY */
                : getAsInetAddress(new byte[4]) /* IPv4_ANY */, 0));

        // While items are still in the queue, test and narrow to subsets.
        while (!workingSet.isEmpty()) {
            final IpPrefix workingPrefix = workingSet.poll();
            final IpRange workingRange = new IpRange(workingPrefix);

            // If the other range is contained within, it's part of the output. Do not test subsets,
            // or we will end up with duplicates.
            if (containsRange(workingRange)) {
                result.add(workingPrefix);
                continue;
            }

            // If there is any overlap, split the working range into it's two subsets, and
            // reevaluate those.
            if (overlapsRange(workingRange)) {
                workingSet.addAll(getSubsetPrefixes(workingPrefix));
            }
        }

        return result;
    }

    /**
     * Returns the two prefixes that comprise the given prefix.
     *
     * <p>For example, for the prefix 192.0.2.0/24, this will return the two prefixes that combined
     * make up the current prefix:
     *
     * <ul>
     *   <li>192.0.2.0/25
     *   <li>192.0.2.128/25
     * </ul>
     */
    private static List<IpPrefix> getSubsetPrefixes(IpPrefix prefix) {
        final List<IpPrefix> result = new ArrayList<>();

        final int currentPrefixLen = prefix.getPrefixLength();
        result.add(new IpPrefix(prefix.getAddress(), currentPrefixLen + 1));

        final byte[] other = prefix.getRawAddress();
        other[currentPrefixLen / 8] =
                (byte) (other[currentPrefixLen / 8] ^ (0x80 >> (currentPrefixLen % 8)));
        result.add(new IpPrefix(getAsInetAddress(other), currentPrefixLen + 1));

        return result;
    }

    /**
     * Checks if the other IP range is contained within this one
     *
     * <p>Checks based on byte values. For other to be contained within this IP range, other's
     * starting address must be greater or equal to the current IpRange's starting address, and the
     * other's ending address must be less than or equal to the current IP range's ending address.
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public boolean containsRange(IpRange other) {
        return addrToBigInteger(mStartAddr).compareTo(addrToBigInteger(other.mStartAddr)) <= 0
                && addrToBigInteger(mEndAddr).compareTo(addrToBigInteger(other.mEndAddr)) >= 0;
    }

    /**
     * Checks if the other IP range overlaps with this one
     *
     * <p>Checks based on byte values. For there to be overlap, this IpRange's starting address must
     * be less than the other's ending address, and vice versa.
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public boolean overlapsRange(IpRange other) {
        return addrToBigInteger(mStartAddr).compareTo(addrToBigInteger(other.mEndAddr)) <= 0
                && addrToBigInteger(other.mStartAddr).compareTo(addrToBigInteger(mEndAddr)) <= 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStartAddr, mEndAddr);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IpRange)) {
            return false;
        }

        final IpRange other = (IpRange) obj;
        return Arrays.equals(mStartAddr, other.mStartAddr)
                && Arrays.equals(mEndAddr, other.mEndAddr);
    }

    /** Gets the InetAddress in BigInteger form */
    private static BigInteger addrToBigInteger(byte[] addr) {
        // Since addr.getAddress() returns network byte order (big-endian), it is compatible with
        // the BigInteger constructor (which assumes big-endian).
        return new BigInteger(SIGNUM_POSITIVE, addr);
    }
}
