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
import android.net.IpPrefix;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

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
    public static final int IPV4_FLAG_MF = 0x2000;
    public static final int IPV4_FLAG_DF = 0x4000;
    public static final int IPV4_PROTOCOL_IGMP = 2;
    public static final int IPV4_IGMP_MIN_SIZE = 8;
    public static final int IPV4_IGMP_GROUP_RECORD_SIZE = 8;
    public static final int IPV4_IGMP_TYPE_V1_REPORT = 0x12;
    public static final int IPV4_IGMP_TYPE_V2_JOIN_REPORT = 0x16;
    public static final int IPV4_IGMP_TYPE_V2_LEAVE_REPORT = 0x17;
    public static final int IPV4_IGMP_TYPE_V3_REPORT = 0x22;
    public static final int IPV4_OPTION_TYPE_ROUTER_ALERT = 0x94;
    public static final int IPV4_OPTION_LEN_ROUTER_ALERT = 4;
    // getSockOpt() for v4 MTU
    public static final int IP_MTU = 14;
    public static final Inet4Address IPV4_ADDR_ALL = makeInet4Address(
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff);
    public static final Inet4Address IPV4_ADDR_ANY = makeInet4Address(
            (byte) 0, (byte) 0, (byte) 0, (byte) 0);
    public static final Inet6Address IPV6_ADDR_ANY = makeInet6Address(new byte[]{
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0 });
    public static final Inet4Address IPV4_ADDR_ALL_HOST_MULTICAST =
            (Inet4Address) InetAddresses.parseNumericAddress("224.0.0.1");

    /**
     * CLAT constants
     */
    public static final IpPrefix CLAT_PREFIX = new IpPrefix("192.0.0.0/29");

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
    public static final int IPV6_FRAGMENT_ID_OFFSET = 4;
    public static final int IPV6_MIN_MTU = 1280;
    public static final int IPV6_FRAGMENT_ID_LEN = 4;
    public static final int IPV6_FRAGMENT_HEADER_LEN = 8;
    public static final int RFC7421_PREFIX_LENGTH = 64;
    // getSockOpt() for v6 MTU
    public static final int IPV6_MTU = 24;
    public static final Inet6Address IPV6_ADDR_ALL_NODES_MULTICAST =
            (Inet6Address) InetAddresses.parseNumericAddress("ff02::1");
    public static final Inet6Address IPV6_ADDR_ALL_ROUTERS_MULTICAST =
            (Inet6Address) InetAddresses.parseNumericAddress("ff02::2");
    public static final Inet6Address IPV6_ADDR_ALL_HOSTS_MULTICAST =
            (Inet6Address) InetAddresses.parseNumericAddress("ff02::3");
    public static final Inet6Address IPV6_ADDR_NODE_LOCAL_ALL_NODES_MULTICAST =
             (Inet6Address) InetAddresses.parseNumericAddress("ff01::1");
    public static final int IPPROTO_FRAGMENT = 44;

    /**
     * ICMP constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc792
     */
    public static final int ICMP_CHECKSUM_OFFSET = 2;
    public static final int ICMP_HEADER_LEN = 8;
    /**
     * ICMPv6 constants.
     *
     * See also:
     *     - https://tools.ietf.org/html/rfc4191
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
    public static final int ICMPV6_ND_OPTION_RIO   = 24;
    public static final int ICMPV6_ND_OPTION_RDNSS = 25;
    public static final int ICMPV6_ND_OPTION_PREF64 = 38;

    public static final int ICMPV6_RS_HEADER_LEN = 8;
    public static final int ICMPV6_RA_HEADER_LEN = 16;
    public static final int ICMPV6_NS_HEADER_LEN = 24;
    public static final int ICMPV6_NA_HEADER_LEN = 24;
    public static final int ICMPV6_ND_OPTION_TLLA_LEN = 8;
    public static final int ICMPV6_ND_OPTION_SLLA_LEN = 8;

    public static final int NEIGHBOR_ADVERTISEMENT_FLAG_ROUTER    = 1 << 31;
    public static final int NEIGHBOR_ADVERTISEMENT_FLAG_SOLICITED = 1 << 30;
    public static final int NEIGHBOR_ADVERTISEMENT_FLAG_OVERRIDE  = 1 << 29;

    public static final byte ROUTER_ADVERTISEMENT_FLAG_MANAGED_ADDRESS = (byte) (1 << 7);
    public static final byte ROUTER_ADVERTISEMENT_FLAG_OTHER = (byte) (1 << 6);

    public static final byte PIO_FLAG_ON_LINK = (byte) (1 << 7);
    public static final byte PIO_FLAG_AUTONOMOUS = (byte) (1 << 6);
    public static final byte PIO_FLAG_DHCPV6_PD_PREFERRED = (byte) (1 << 4);

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
    public static final int UDP_SRCPORT_OFFSET = 0;
    public static final int UDP_DSTPORT_OFFSET = 2;
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
    // The maximum length of a DHCP packet that can be constructed.
    public static final int DHCP_MAX_LENGTH = 1500;
    public static final int DHCP_MAX_OPTION_LEN = 255;

    /**
     * DHCPv6 constants.
     *
     * See also:
     *     - https://datatracker.ietf.org/doc/html/rfc8415
     *     - https://www.iana.org/assignments/dhcpv6-parameters/dhcpv6-parameters.xhtml
     */
    public static final int DHCP6_CLIENT_PORT = 546;
    public static final int DHCP6_SERVER_PORT = 547;
    public static final Inet6Address ALL_DHCP_RELAY_AGENTS_AND_SERVERS =
            (Inet6Address) InetAddresses.parseNumericAddress("ff02::1:2");
    public static final int DHCP6_OPTION_IA_ADDR = 5;
    public static final int DHCP6_OPTION_IA_PD = 25;
    public static final int DHCP6_OPTION_IAPREFIX = 26;

    /**
     * DNS constants.
     *
     * See also:
     *     - https://datatracker.ietf.org/doc/html/rfc7858#section-3.1
     */
    public static final short DNS_OVER_TLS_PORT = 853;

    /**
     * Dns query type constants.
     *
     * See also:
     *    - https://datatracker.ietf.org/doc/html/rfc1035#section-3.2.2
     */
    public static final int TYPE_A = 1;
    public static final int TYPE_PTR = 12;
    public static final int TYPE_TXT = 16;
    public static final int TYPE_AAAA = 28;
    public static final int TYPE_SRV = 33;

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

    /**
     * A test URL used to override configuration settings and overlays for the network validation
     * HTTPS URL, when set in {@link android.provider.DeviceConfig} configuration.
     *
     * <p>This URL will be ignored if the host is not "localhost" (it can only be used to test with
     * a local test server), and must not be set in production scenarios (as enforced by CTS tests).
     *
     * <p>{@link #TEST_URL_EXPIRATION_TIME} must also be set to use this setting.
     */
    public static final String TEST_CAPTIVE_PORTAL_HTTPS_URL = "test_captive_portal_https_url";
    /**
     * A test URL used to override configuration settings and overlays for the network validation
     * HTTP URL, when set in {@link android.provider.DeviceConfig} configuration.
     *
     * <p>This URL will be ignored if the host is not "localhost" (it can only be used to test with
     * a local test server), and must not be set in production scenarios (as enforced by CTS tests).
     *
     * <p>{@link #TEST_URL_EXPIRATION_TIME} must also be set to use this setting.
     */
    public static final String TEST_CAPTIVE_PORTAL_HTTP_URL = "test_captive_portal_http_url";
    /**
     * Expiration time of the test URL, in ms, relative to {@link System#currentTimeMillis()}.
     *
     * <p>After this expiration time, test URLs will be ignored. They will also be ignored if
     * the expiration time is more than 10 minutes in the future, to avoid misconfiguration
     * following test runs.
     */
    public static final String TEST_URL_EXPIRATION_TIME = "test_url_expiration_time";

    /**
     * List of IpPrefix that are local network prefixes.
     */
    public static final List<IpPrefix> IPV4_LOCAL_PREFIXES = List.of(
            new IpPrefix("169.254.0.0/16"), // Link Local
            new IpPrefix("100.64.0.0/10"),  // CGNAT
            new IpPrefix("10.0.0.0/8"),     // RFC1918
            new IpPrefix("172.16.0.0/12"),  // RFC1918
            new IpPrefix("192.168.0.0/16")  // RFC1918
    );

    /**
     * List of IpPrefix that are multicast and broadcast prefixes.
     */
    public static final List<IpPrefix> MULTICAST_AND_BROADCAST_PREFIXES = List.of(
            new IpPrefix("224.0.0.0/4"),               // Multicast
            new IpPrefix("ff00::/8"),                  // Multicast
            new IpPrefix("255.255.255.255/32")         // Broadcast
    );

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
