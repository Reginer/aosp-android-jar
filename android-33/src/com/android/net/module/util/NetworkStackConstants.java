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

import android.net.InetAddresses;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Network constants used by the network stack.
 * @hide
 */
public final class NetworkStackConstants {

    /**
     * Ethernet constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc894
     *     - https://tools.ietf.org/html/rfc2464
     *     - https://tools.ietf.org/html/rfc7042
     *     - http://www.iana.org/assignments/ethernet-numbers/ethernet-numbers.xhtml
     *     - http://www.iana.org/assignments/ieee-802-numbers/ieee-802-numbers.xhtml
     */
    public static final int ETHER_DST_ADDR_OFFSET = 0;
    public static final int ETHER_SRC_ADDR_OFFSET = 6;
    public static final int ETHER_ADDR_LEN = 6;
    public static final int ETHER_TYPE_OFFSET = 12;
    public static final int ETHER_TYPE_LENGTH = 2;
    public static final int ETHER_TYPE_ARP  = 0x0806;
    public static final int ETHER_TYPE_IPV4 = 0x0800;
    public static final int ETHER_TYPE_IPV6 = 0x86dd;
    public static final int ETHER_HEADER_LEN = 14;
    public static final int ETHER_MTU = 1500;
    public static final byte[] ETHER_BROADCAST = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff,
    };

    /**
     * ARP constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc826
     *     - http://www.iana.org/assignments/arp-parameters/arp-parameters.xhtml
     */
    public static final int ARP_PAYLOAD_LEN = 28;  // For Ethernet+IPv4.
    public static final int ARP_ETHER_IPV4_LEN = ARP_PAYLOAD_LEN + ETHER_HEADER_LEN;
    public static final int ARP_REQUEST = 1;
    public static final int ARP_REPLY   = 2;
    public static final int ARP_HWTYPE_RESERVED_LO = 0;
    public static final int ARP_HWTYPE_ETHER       = 1;
    public static final int ARP_HWTYPE_RESERVED_HI = 0xffff;

    /**
     * IPv4 Address Conflict Detection constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc5227
     */
    public static final int IPV4_CONFLICT_PROBE_NUM = 3;
    public static final int IPV4_CONFLICT_ANNOUNCE_NUM = 2;

    /**
     * IPv4 constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc791
     */
    public static final int IPV4_ADDR_BITS = 32;
    public static final int IPV4_MIN_MTU = 68;
    public static final int IPV4_MAX_MTU = 65_535;
    public static final int IPV4_HEADER_MIN_LEN = 20;
    public static final int IPV4_IHL_MASK = 0xf;
    public static final int IPV4_LENGTH_OFFSET = 2;
    public static final int IPV4_FLAGS_OFFSET = 6;
    public static final int IPV4_FRAGMENT_MASK = 0x1fff;
    public static final int IPV4_PROTOCOL_OFFSET = 9;
    public static final int IPV4_CHECKSUM_OFFSET = 10;
    public static final int IPV4_SRC_ADDR_OFFSET = 12;
    public static final int IPV4_DST_ADDR_OFFSET = 16;
    public static final int IPV4_ADDR_LEN = 4;
    public static final Inet4Address IPV4_ADDR_ALL = makeInet4Address(
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff);
    public static final Inet4Address IPV4_ADDR_ANY = makeInet4Address(
            (byte) 0, (byte) 0, (byte) 0, (byte) 0);
    public static final Inet6Address IPV6_ADDR_ANY = makeInet6Address(new byte[]{
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0 });
    /**
     * IPv6 constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc2460
     */
    public static final int IPV6_ADDR_LEN = 16;
    public static final int IPV6_HEADER_LEN = 40;
    public static final int IPV6_LEN_OFFSET = 4;
    public static final int IPV6_PROTOCOL_OFFSET = 6;
    public static final int IPV6_SRC_ADDR_OFFSET = 8;
    public static final int IPV6_DST_ADDR_OFFSET = 24;
    public static final int IPV6_MIN_MTU = 1280;
    public static final Inet6Address IPV6_ADDR_ALL_NODES_MULTICAST =
            (Inet6Address) InetAddresses.parseNumericAddress("ff02::1");
    public static final Inet6Address IPV6_ADDR_ALL_ROUTERS_MULTICAST =
            (Inet6Address) InetAddresses.parseNumericAddress("ff02::2");
    public static final Inet6Address IPV6_ADDR_ALL_HOSTS_MULTICAST =
            (Inet6Address) InetAddresses.parseNumericAddress("ff02::3");

    /**
     * ICMPv6 constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc4443
     *     - https://tools.ietf.org/html/rfc4861
     */
    public static final int ICMPV6_HEADER_MIN_LEN = 4;
    public static final int ICMPV6_CHECKSUM_OFFSET = 2;
    public static final int ICMPV6_ECHO_REPLY_TYPE = 129;
    public static final int ICMPV6_ECHO_REQUEST_TYPE = 128;
    public static final int ICMPV6_ROUTER_SOLICITATION    = 133;
    public static final int ICMPV6_ROUTER_ADVERTISEMENT   = 134;
    public static final int ICMPV6_NEIGHBOR_SOLICITATION  = 135;
    public static final int ICMPV6_NEIGHBOR_ADVERTISEMENT = 136;
    public static final int ICMPV6_ND_OPTION_MIN_LENGTH = 8;
    public static final int ICMPV6_ND_OPTION_LENGTH_SCALING_FACTOR = 8;
    public static final int ICMPV6_ND_OPTION_SLLA  = 1;
    public static final int ICMPV6_ND_OPTION_TLLA  = 2;
    public static final int ICMPV6_ND_OPTION_PIO   = 3;
    public static final int ICMPV6_ND_OPTION_MTU   = 5;
    public static final int ICMPV6_ND_OPTION_RDNSS = 25;
    public static final int ICMPV6_ND_OPTION_PREF64 = 38;

    public static final int ICMPV6_RS_HEADER_LEN = 8;
    public static final int ICMPV6_RA_HEADER_LEN = 16;
    public static final int ICMPV6_NS_HEADER_LEN = 24;
    public static final int ICMPV6_NA_HEADER_LEN = 24;

    public static final int NEIGHBOR_ADVERTISEMENT_FLAG_ROUTER    = 1 << 31;
    public static final int NEIGHBOR_ADVERTISEMENT_FLAG_SOLICITED = 1 << 30;
    public static final int NEIGHBOR_ADVERTISEMENT_FLAG_OVERRIDE  = 1 << 29;

    public static final byte ROUTER_ADVERTISEMENT_FLAG_MANAGED_ADDRESS = (byte) (1 << 7);
    public static final byte ROUTER_ADVERTISEMENT_FLAG_OTHER = (byte) (1 << 6);

    public static final byte PIO_FLAG_ON_LINK = (byte) (1 << 7);
    public static final byte PIO_FLAG_AUTONOMOUS = (byte) (1 << 6);

    /**
     * TCP constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc793
     */
    public static final int TCP_HEADER_MIN_LEN = 20;
    public static final int TCP_CHECKSUM_OFFSET = 16;
    public static final byte TCPHDR_FIN = (byte) (1 << 0);
    public static final byte TCPHDR_SYN = (byte) (1 << 1);
    public static final byte TCPHDR_RST = (byte) (1 << 2);
    public static final byte TCPHDR_PSH = (byte) (1 << 3);
    public static final byte TCPHDR_ACK = (byte) (1 << 4);
    public static final byte TCPHDR_URG = (byte) (1 << 5);

    /**
     * UDP constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc768
     */
    public static final int UDP_HEADER_LEN = 8;
    public static final int UDP_LENGTH_OFFSET = 4;
    public static final int UDP_CHECKSUM_OFFSET = 6;

    /**
     * DHCP constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc2131
     */
    public static final int INFINITE_LEASE = 0xffffffff;
    public static final int DHCP4_CLIENT_PORT = 68;

    /**
     * IEEE802.11 standard constants.
     *
     * See also:
     *     - https://ieeexplore.ieee.org/document/7786995
     */
    public static final int VENDOR_SPECIFIC_IE_ID = 0xdd;


    /**
     * TrafficStats constants.
     */
    // These tags are used by the network stack to do traffic for its own purposes. Traffic
    // tagged with these will be counted toward the network stack and must stay inside the
    // range defined by
    // {@link android.net.TrafficStats#TAG_NETWORK_STACK_RANGE_START} and
    // {@link android.net.TrafficStats#TAG_NETWORK_STACK_RANGE_END}.
    public static final int TAG_SYSTEM_DHCP = 0xFFFFFE01;
    public static final int TAG_SYSTEM_NEIGHBOR = 0xFFFFFE02;
    public static final int TAG_SYSTEM_DHCP_SERVER = 0xFFFFFE03;

    // These tags are used by the network stack to do traffic on behalf of apps. Traffic
    // tagged with these will be counted toward the app on behalf of which the network
    // stack is doing this traffic. These values must stay inside the range defined by
    // {@link android.net.TrafficStats#TAG_NETWORK_STACK_IMPERSONATION_RANGE_START} and
    // {@link android.net.TrafficStats#TAG_NETWORK_STACK_IMPERSONATION_RANGE_END}.
    public static final int TAG_SYSTEM_PROBE = 0xFFFFFF81;
    public static final int TAG_SYSTEM_DNS = 0xFFFFFF82;

    // TODO: Move to Inet4AddressUtils
    // See aosp/1455936: NetworkStackConstants can't depend on it as it causes jarjar-related issues
    // for users of both the net-utils-device-common and net-utils-framework-common libraries.
    // Jarjar rule management needs to be simplified for that: b/170445871

    /**
     * Make an Inet4Address from 4 bytes in network byte order.
     */
    private static Inet4Address makeInet4Address(byte b1, byte b2, byte b3, byte b4) {
        try {
            return (Inet4Address) InetAddress.getByAddress(new byte[] { b1, b2, b3, b4 });
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("addr must be 4 bytes: this should never happen");
        }
    }

    /**
     * Make an Inet6Address from 16 bytes in network byte order.
     */
    private static Inet6Address makeInet6Address(byte[] bytes) {
        try {
            return (Inet6Address) InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("addr must be 16 bytes: this should never happen");
        }
    }
    private NetworkStackConstants() {
        throw new UnsupportedOperationException("This class is not to be instantiated");
    }
}
