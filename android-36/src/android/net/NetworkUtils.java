/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.net;

import static android.net.ConnectivityManager.NETID_UNSET;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.system.ErrnoException;
import android.util.Log;
import android.util.Pair;

import com.android.net.module.util.Inet4AddressUtils;

import java.io.FileDescriptor;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Native methods for managing network interfaces.
 *
 * {@hide}
 */
public class NetworkUtils {
    static {
        System.loadLibrary("framework-connectivity-jni");
    }

    private static final String TAG = "NetworkUtils";

    /**
     * Attaches a socket filter that drops all of incoming packets.
     * @param fd the socket's {@link FileDescriptor}.
     */
    public static native void attachDropAllBPFFilter(FileDescriptor fd) throws SocketException;

    /**
     * Detaches a socket filter.
     * @param fd the socket's {@link FileDescriptor}.
     */
    public static native void detachBPFFilter(FileDescriptor fd) throws SocketException;

    private static native boolean bindProcessToNetworkHandle(long netHandle);

    /**
     * Binds the current process to the network designated by {@code netId}.  All sockets created
     * in the future (and not explicitly bound via a bound {@link SocketFactory} (see
     * {@link Network#getSocketFactory}) will be bound to this network.  Note that if this
     * {@code Network} ever disconnects all sockets created in this way will cease to work.  This
     * is by design so an application doesn't accidentally use sockets it thinks are still bound to
     * a particular {@code Network}.  Passing NETID_UNSET clears the binding.
     */
    public static boolean bindProcessToNetwork(int netId) {
        return bindProcessToNetworkHandle(new Network(netId).getNetworkHandle());
    }

    private static native long getBoundNetworkHandleForProcess();

    /**
     * Return the netId last passed to {@link #bindProcessToNetwork}, or NETID_UNSET if
     * {@link #unbindProcessToNetwork} has been called since {@link #bindProcessToNetwork}.
     */
    public static int getBoundNetworkForProcess() {
        final long netHandle = getBoundNetworkHandleForProcess();
        return netHandle == 0L ? NETID_UNSET : Network.fromNetworkHandle(netHandle).getNetId();
    }

    /**
     * Binds host resolutions performed by this process to the network designated by {@code netId}.
     * {@link #bindProcessToNetwork} takes precedence over this setting.  Passing NETID_UNSET clears
     * the binding.
     *
     * @deprecated This is strictly for legacy usage to support startUsingNetworkFeature().
     */
    @Deprecated
    public native static boolean bindProcessToNetworkForHostResolution(int netId);

    private static native int bindSocketToNetworkHandle(FileDescriptor fd, long netHandle);

    /**
     * Explicitly binds {@code fd} to the network designated by {@code netId}.  This
     * overrides any binding via {@link #bindProcessToNetwork}.
     * @return 0 on success or negative errno on failure.
     */
    public static int bindSocketToNetwork(FileDescriptor fd, int netId) {
        return bindSocketToNetworkHandle(fd, new Network(netId).getNetworkHandle());
    }

    /**
     * Determine if {@code uid} can access network designated by {@code netId}.
     * @return {@code true} if {@code uid} can access network, {@code false} otherwise.
     */
    public static boolean queryUserAccess(int uid, int netId) {
        // TODO (b/183485986): remove this method
        return false;
    }

    private static native FileDescriptor resNetworkSend(
            long netHandle, byte[] msg, int msglen, int flags) throws ErrnoException;

    /**
     * DNS resolver series jni method.
     * Issue the query {@code msg} on the network designated by {@code netId}.
     * {@code flags} is an additional config to control actual querying behavior.
     * @return a file descriptor to watch for read events
     */
    public static FileDescriptor resNetworkSend(
            int netId, byte[] msg, int msglen, int flags) throws ErrnoException {
        return resNetworkSend(new Network(netId).getNetworkHandle(), msg, msglen, flags);
    }

    private static native FileDescriptor resNetworkQuery(
            long netHandle, String dname, int nsClass, int nsType, int flags) throws ErrnoException;

    /**
     * DNS resolver series jni method.
     * Look up the {@code nsClass} {@code nsType} Resource Record (RR) associated
     * with Domain Name {@code dname} on the network designated by {@code netId}.
     * {@code flags} is an additional config to control actual querying behavior.
     * @return a file descriptor to watch for read events
     */
    public static FileDescriptor resNetworkQuery(
            int netId, String dname, int nsClass, int nsType, int flags) throws ErrnoException {
        return resNetworkQuery(new Network(netId).getNetworkHandle(), dname, nsClass, nsType,
                flags);
    }

    /**
     * DNS resolver series jni method.
     * Read a result for the query associated with the {@code fd}.
     * @return DnsResponse containing blob answer and rcode
     */
    public static native DnsResolver.DnsResponse resNetworkResult(FileDescriptor fd)
            throws ErrnoException;

    /**
     * DNS resolver series jni method.
     * Attempts to cancel the in-progress query associated with the {@code fd}.
     */
    public static native void resNetworkCancel(FileDescriptor fd);

    /**
     * DNS resolver series jni method.
     * Attempts to get network which resolver will use if no network is explicitly selected.
     */
    public static native Network getDnsNetwork() throws ErrnoException;

    /**
     * Get the tcp repair window associated with the {@code fd}.
     *
     * @param fd the tcp socket's {@link FileDescriptor}.
     * @return a {@link TcpRepairWindow} object indicates tcp window size.
     */
    public static native TcpRepairWindow getTcpRepairWindow(FileDescriptor fd)
            throws ErrnoException;

    /**
     * @see Inet4AddressUtils#intToInet4AddressHTL(int)
     * @deprecated Use either {@link Inet4AddressUtils#intToInet4AddressHTH(int)}
     *             or {@link Inet4AddressUtils#intToInet4AddressHTL(int)}
     */
    @Deprecated
    @UnsupportedAppUsage
    public static InetAddress intToInetAddress(int hostAddress) {
        return Inet4AddressUtils.intToInet4AddressHTL(hostAddress);
    }

    /**
     * @see Inet4AddressUtils#inet4AddressToIntHTL(Inet4Address)
     * @deprecated Use either {@link Inet4AddressUtils#inet4AddressToIntHTH(Inet4Address)}
     *             or {@link Inet4AddressUtils#inet4AddressToIntHTL(Inet4Address)}
     */
    @Deprecated
    public static int inetAddressToInt(Inet4Address inetAddr)
            throws IllegalArgumentException {
        return Inet4AddressUtils.inet4AddressToIntHTL(inetAddr);
    }

    /**
     * @see Inet4AddressUtils#prefixLengthToV4NetmaskIntHTL(int)
     * @deprecated Use either {@link Inet4AddressUtils#prefixLengthToV4NetmaskIntHTH(int)}
     *             or {@link Inet4AddressUtils#prefixLengthToV4NetmaskIntHTL(int)}
     */
    @Deprecated
    @UnsupportedAppUsage
    public static int prefixLengthToNetmaskInt(int prefixLength)
            throws IllegalArgumentException {
        return Inet4AddressUtils.prefixLengthToV4NetmaskIntHTL(prefixLength);
    }

    /**
     * Convert a IPv4 netmask integer to a prefix length
     * @param netmask as an integer (0xff000000 for a /8 subnet)
     * @return the network prefix length
     */
    public static int netmaskIntToPrefixLength(int netmask) {
        return Integer.bitCount(netmask);
    }

    /**
     * Convert an IPv4 netmask to a prefix length, checking that the netmask is contiguous.
     * @param netmask as a {@code Inet4Address}.
     * @return the network prefix length
     * @throws IllegalArgumentException the specified netmask was not contiguous.
     * @hide
     * @deprecated use {@link Inet4AddressUtils#netmaskToPrefixLength(Inet4Address)}
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Deprecated
    public static int netmaskToPrefixLength(Inet4Address netmask) {
        // This is only here because some apps seem to be using it (@UnsupportedAppUsage).
        return Inet4AddressUtils.netmaskToPrefixLength(netmask);
    }


    /**
     * Create an InetAddress from a string where the string must be a standard
     * representation of a V4 or V6 address.  Avoids doing a DNS lookup on failure
     * but it will throw an IllegalArgumentException in that case.
     * @param addrString
     * @return the InetAddress
     * @hide
     * @deprecated Use {@link InetAddresses#parseNumericAddress(String)}, if possible.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    @Deprecated
    public static InetAddress numericToInetAddress(String addrString)
            throws IllegalArgumentException {
        return InetAddresses.parseNumericAddress(addrString);
    }

    /**
     * Returns the implicit netmask of an IPv4 address, as was the custom before 1993.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static int getImplicitNetmask(Inet4Address address) {
        // Only here because it seems to be used by apps
        return Inet4AddressUtils.getImplicitNetmask(address);
    }

    /**
     * Utility method to parse strings such as "192.0.2.5/24" or "2001:db8::cafe:d00d/64".
     * @hide
     */
    public static Pair<InetAddress, Integer> parseIpAndMask(String ipAndMaskString) {
        InetAddress address = null;
        int prefixLength = -1;
        try {
            String[] pieces = ipAndMaskString.split("/", 2);
            prefixLength = Integer.parseInt(pieces[1]);
            address = InetAddresses.parseNumericAddress(pieces[0]);
        } catch (NullPointerException e) {            // Null string.
        } catch (ArrayIndexOutOfBoundsException e) {  // No prefix length.
        } catch (NumberFormatException e) {           // Non-numeric prefix.
        } catch (IllegalArgumentException e) {        // Invalid IP address.
        }

        if (address == null || prefixLength == -1) {
            throw new IllegalArgumentException("Invalid IP address and mask " + ipAndMaskString);
        }

        return new Pair<InetAddress, Integer>(address, prefixLength);
    }

    /**
     * Utility method to parse strings such as "192.0.2.5/24" or "2001:db8::cafe:d00d/64".
     * @hide
     *
     * @deprecated This method is used only for IpPrefix and LinkAddress. Since Android S, use
     *             {@link #parseIpAndMask(String)}, if possible.
     */
    @Deprecated
    public static Pair<InetAddress, Integer> legacyParseIpAndMask(String ipAndMaskString) {
        InetAddress address = null;
        int prefixLength = -1;
        try {
            String[] pieces = ipAndMaskString.split("/", 2);
            prefixLength = Integer.parseInt(pieces[1]);
            if (pieces[0] == null || pieces[0].isEmpty()) {
                final byte[] bytes = new byte[16];
                bytes[15] = 1;
                return new Pair<InetAddress, Integer>(Inet6Address.getByAddress(
                        "ip6-localhost"/* host */, bytes, 0 /* scope_id */), prefixLength);
            }

            if (pieces[0].startsWith("[")
                    && pieces[0].endsWith("]")
                    && pieces[0].indexOf(':') != -1) {
                pieces[0] = pieces[0].substring(1, pieces[0].length() - 1);
            }
            address = InetAddresses.parseNumericAddress(pieces[0]);
        } catch (NullPointerException e) {            // Null string.
        } catch (ArrayIndexOutOfBoundsException e) {  // No prefix length.
        } catch (NumberFormatException e) {           // Non-numeric prefix.
        } catch (IllegalArgumentException e) {        // Invalid IP address.
        } catch (UnknownHostException e) {            // IP address length is illegal
        }

        if (address == null || prefixLength == -1) {
            throw new IllegalArgumentException("Invalid IP address and mask " + ipAndMaskString);
        }

        return new Pair<InetAddress, Integer>(address, prefixLength);
    }

    /**
     * Convert a 32 char hex string into a Inet6Address.
     * throws a runtime exception if the string isn't 32 chars, isn't hex or can't be
     * made into an Inet6Address
     * @param addrHexString a 32 character hex string representing an IPv6 addr
     * @return addr an InetAddress representation for the string
     */
    public static InetAddress hexToInet6Address(String addrHexString)
            throws IllegalArgumentException {
        try {
            return numericToInetAddress(String.format(Locale.US, "%s:%s:%s:%s:%s:%s:%s:%s",
                    addrHexString.substring(0,4),   addrHexString.substring(4,8),
                    addrHexString.substring(8,12),  addrHexString.substring(12,16),
                    addrHexString.substring(16,20), addrHexString.substring(20,24),
                    addrHexString.substring(24,28), addrHexString.substring(28,32)));
        } catch (Exception e) {
            Log.e("NetworkUtils", "error in hexToInet6Address(" + addrHexString + "): " + e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Trim leading zeros from IPv4 address strings
     * Our base libraries will interpret that as octel..
     * Must leave non v4 addresses and host names alone.
     * For example, 192.168.000.010 -> 192.168.0.10
     * TODO - fix base libraries and remove this function
     * @param addr a string representing an ip addr
     * @return a string propertly trimmed
     */
    @UnsupportedAppUsage
    public static String trimV4AddrZeros(String addr) {
        return Inet4AddressUtils.trimAddressZeros(addr);
    }

    /**
     * Returns a prefix set without overlaps.
     *
     * This expects the src set to be sorted from shorter to longer. Results are undefined
     * failing this condition. The returned prefix set is sorted in the same order as the
     * passed set, with the same comparator.
     */
    private static TreeSet<IpPrefix> deduplicatePrefixSet(final TreeSet<IpPrefix> src) {
        final TreeSet<IpPrefix> dst = new TreeSet<>(src.comparator());
        // Prefixes match addresses that share their upper part up to their length, therefore
        // the only kind of possible overlap in two prefixes is strict inclusion of the longer
        // (more restrictive) in the shorter (including equivalence if they have the same
        // length).
        // Because prefixes in the src set are sorted from shorter to longer, deduplicating
        // is done by simply iterating in order, and not adding any longer prefix that is
        // already covered by a shorter one.
        newPrefixes:
        for (IpPrefix newPrefix : src) {
            for (IpPrefix existingPrefix : dst) {
                if (existingPrefix.containsPrefix(newPrefix)) {
                    continue newPrefixes;
                }
            }
            dst.add(newPrefix);
        }
        return dst;
    }

    /**
     * Returns how many IPv4 addresses match any of the prefixes in the passed ordered set.
     *
     * Obviously this returns an integral value between 0 and 2**32.
     * The behavior is undefined if any of the prefixes is not an IPv4 prefix or if the
     * set is not ordered smallest prefix to longer prefix.
     *
     * @param prefixes the set of prefixes, ordered by length
     */
    public static long routedIPv4AddressCount(final TreeSet<IpPrefix> prefixes) {
        long routedIPCount = 0;
        for (final IpPrefix prefix : deduplicatePrefixSet(prefixes)) {
            if (!prefix.isIPv4()) {
                Log.wtf(TAG, "Non-IPv4 prefix in routedIPv4AddressCount");
            }
            int rank = 32 - prefix.getPrefixLength();
            routedIPCount += 1L << rank;
        }
        return routedIPCount;
    }

    /**
     * Returns how many IPv6 addresses match any of the prefixes in the passed ordered set.
     *
     * This returns a BigInteger between 0 and 2**128.
     * The behavior is undefined if any of the prefixes is not an IPv6 prefix or if the
     * set is not ordered smallest prefix to longer prefix.
     */
    public static BigInteger routedIPv6AddressCount(final TreeSet<IpPrefix> prefixes) {
        BigInteger routedIPCount = BigInteger.ZERO;
        for (final IpPrefix prefix : deduplicatePrefixSet(prefixes)) {
            if (!prefix.isIPv6()) {
                Log.wtf(TAG, "Non-IPv6 prefix in routedIPv6AddressCount");
            }
            int rank = 128 - prefix.getPrefixLength();
            routedIPCount = routedIPCount.add(BigInteger.ONE.shiftLeft(rank));
        }
        return routedIPCount;
    }

    /**
     * Sets a socket option with byte array
     *
     * @param fd The socket file descriptor
     * @param level The level at which the option is defined
     * @param option The socket option for which the value is to be set
     * @param value The option value to be set in byte array
     * @throws ErrnoException if setsockopt fails
     */
    public static native void setsockoptBytes(FileDescriptor fd, int level, int option,
            byte[] value) throws ErrnoException;

    /** Returns whether the Linux Kernel is 64 bit */
    public static native boolean isKernel64Bit();

    /** Returns whether the Linux Kernel is x86 */
    public static native boolean isKernelX86();
}
