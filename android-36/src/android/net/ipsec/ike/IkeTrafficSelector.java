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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.net.InetAddresses;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.os.PersistableBundle;
import android.util.ArraySet;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * IkeTrafficSelector represents a Traffic Selector of a Child Session.
 *
 * <p>Traffic Selectors specify addresses that are acceptable within the IPsec SA.
 *
 * <p>Callers can propose {@link IkeTrafficSelector}s when building a {@link ChildSessionParams} and
 * receive the negotiated {@link IkeTrafficSelector}s via a {@link ChildSessionConfiguration}.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.13">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeTrafficSelector {

    // IpProtocolId consists of standard IP Protocol IDs.
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({IP_PROTOCOL_ID_UNSPEC, IP_PROTOCOL_ID_ICMP, IP_PROTOCOL_ID_TCP, IP_PROTOCOL_ID_UDP})
    public @interface IpProtocolId {}

    // Zero value is re-defined by IKE to indicate that all IP protocols are acceptable.
    /** @hide */
    @VisibleForTesting static final int IP_PROTOCOL_ID_UNSPEC = 0;
    /** @hide */
    @VisibleForTesting static final int IP_PROTOCOL_ID_ICMP = 1;
    /** @hide */
    @VisibleForTesting static final int IP_PROTOCOL_ID_TCP = 6;
    /** @hide */
    @VisibleForTesting static final int IP_PROTOCOL_ID_UDP = 17;

    private static final ArraySet<Integer> IP_PROTOCOL_ID_SET = new ArraySet<>();

    static {
        IP_PROTOCOL_ID_SET.add(IP_PROTOCOL_ID_UNSPEC);
        IP_PROTOCOL_ID_SET.add(IP_PROTOCOL_ID_ICMP);
        IP_PROTOCOL_ID_SET.add(IP_PROTOCOL_ID_TCP);
        IP_PROTOCOL_ID_SET.add(IP_PROTOCOL_ID_UDP);
    }

    /**
     * TrafficSelectorType consists of IKE standard Traffic Selector Types.
     *
     * @see <a
     *     href="https://www.iana.org/assignments/ikev2-parameters/ikev2-parameters.xhtml">Internet
     *     Key Exchange Version 2 (IKEv2) Parameters</a>
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE, TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE})
    public @interface TrafficSelectorType {}

    /** @hide */
    public static final int TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE = 7;
    /** @hide */
    public static final int TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE = 8;

    /** @hide */
    public static final int PORT_NUMBER_MIN = 0;
    /** @hide */
    public static final int PORT_NUMBER_MAX = 65535;

    // TODO: Consider defining these constants in a central place in Connectivity.
    private static final int IPV4_ADDR_LEN = 4;
    private static final int IPV6_ADDR_LEN = 16;

    @VisibleForTesting static final int TRAFFIC_SELECTOR_IPV4_LEN = 16;
    @VisibleForTesting static final int TRAFFIC_SELECTOR_IPV6_LEN = 40;

    private static final String START_PORT_KEY = "startPort";
    private static final String END_PORT_KEY = "endPort";
    private static final String START_ADDRESS_KEY = "startingAddress";
    private static final String END_ADDRESS_KEY = "endingAddress";

    /** @hide */
    public final int tsType;
    /** @hide */
    public final int ipProtocolId;
    /** @hide */
    public final int selectorLength;
    /** The smallest port number allowed by this Traffic Selector. Informational only. */
    public final int startPort;
    /** The largest port number allowed by this Traffic Selector. Informational only. */
    public final int endPort;
    /** The smallest address included in this Traffic Selector. */
    @NonNull public final InetAddress startingAddress;
    /** The largest address included in this Traffic Selector. */
    @NonNull public final InetAddress endingAddress;

    private IkeTrafficSelector(
            int tsType,
            int ipProtocolId,
            int selectorLength,
            int startPort,
            int endPort,
            InetAddress startingAddress,
            InetAddress endingAddress) {
        this.tsType = tsType;
        this.ipProtocolId = ipProtocolId;
        this.selectorLength = selectorLength;

        // Ports & Addresses previously validated in decodeIkeTrafficSelectors()
        this.startPort = startPort;
        this.endPort = endPort;
        this.startingAddress = startingAddress;
        this.endingAddress = endingAddress;
    }

    /**
     * Construct an instance of {@link IkeTrafficSelector} for negotiating a Child Session.
     *
     * <p>Android platform does not support port-based routing. The port range negotiation is only
     * informational.
     *
     * @param startPort the smallest port number allowed by this Traffic Selector.
     * @param endPort the largest port number allowed by this Traffic Selector.
     * @param startingAddress the smallest address included in this Traffic Selector.
     * @param endingAddress the largest address included in this Traffic Selector.
     */
    public IkeTrafficSelector(
            int startPort,
            int endPort,
            @NonNull InetAddress startingAddress,
            @NonNull InetAddress endingAddress) {
        this(getTsType(startingAddress), startPort, endPort, startingAddress, endingAddress);
    }

    private static int getTsType(InetAddress address) {
        if (address instanceof Inet4Address) {
            return TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE;
        }
        return TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE;
    }

    /**
     * Construct an instance of IkeTrafficSelector for building an outbound IKE message.
     *
     * @param tsType the Traffic Selector type.
     * @param startPort the smallest port number allowed by this Traffic Selector.
     * @param endPort the largest port number allowed by this Traffic Selector.
     * @param startingAddress the smallest address included in this Traffic Selector.
     * @param endingAddress the largest address included in this Traffic Selector.
     * @hide
     */
    public IkeTrafficSelector(
            @TrafficSelectorType int tsType,
            int startPort,
            int endPort,
            InetAddress startingAddress,
            InetAddress endingAddress) {

        this.tsType = tsType;
        this.ipProtocolId = IP_PROTOCOL_ID_UNSPEC;

        switch (tsType) {
            case TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE:
                this.selectorLength = TRAFFIC_SELECTOR_IPV4_LEN;

                if (!(startingAddress instanceof Inet4Address)
                        || !(endingAddress instanceof Inet4Address)) {
                    throw new IllegalArgumentException(
                            "Invalid address range: TS_IPV4_ADDR_RANGE requires IPv4 addresses.");
                }

                break;
            case TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE:
                this.selectorLength = TRAFFIC_SELECTOR_IPV6_LEN;

                if (!(startingAddress instanceof Inet6Address)
                        || !(endingAddress instanceof Inet6Address)) {
                    throw new IllegalArgumentException(
                            "Invalid address range: TS_IPV6_ADDR_RANGE requires IPv6 addresses.");
                }
                break;
            default:
                throw new IllegalArgumentException("Unrecognized Traffic Selector type.");
        }

        if (compareInetAddressTo(startingAddress, endingAddress) > 0) {
            throw new IllegalArgumentException("Received invalid address range.");
        }

        if (!isPortRangeValid(startPort, endPort)) {
            throw new IllegalArgumentException(
                    "Invalid port range. startPort: " + startPort + " endPort: " + endPort);
        }

        this.startPort = startPort;
        this.endPort = endPort;
        this.startingAddress = startingAddress;
        this.endingAddress = endingAddress;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public static IkeTrafficSelector fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle not provided");

        int startPort = in.getInt(START_PORT_KEY);
        int endPort = in.getInt(END_PORT_KEY);

        InetAddress startingAddress =
                InetAddresses.parseNumericAddress(in.getString(START_ADDRESS_KEY));
        Objects.requireNonNull(in, "startAddress not provided");
        InetAddress endingAddress =
                InetAddresses.parseNumericAddress(in.getString(END_ADDRESS_KEY));
        Objects.requireNonNull(in, "endAddress not provided");

        return new IkeTrafficSelector(startPort, endPort, startingAddress, endingAddress);
    }

    /**
     * Serializes this object to a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public PersistableBundle toPersistableBundle() {
        final PersistableBundle result = new PersistableBundle();

        result.putInt(START_PORT_KEY, startPort);
        result.putInt(END_PORT_KEY, endPort);
        result.putString(START_ADDRESS_KEY, startingAddress.getHostAddress());
        result.putString(END_ADDRESS_KEY, endingAddress.getHostAddress());

        return result;
    }

    /**
     * Decode IkeTrafficSelectors from inbound Traffic Selector Payload.
     *
     * <p>This method is only called by IkeTsPayload when decoding inbound IKE message.
     *
     * @param numTs number or Traffic Selectors
     * @param tsBytes encoded byte array of Traffic Selectors
     * @return an array of decoded IkeTrafficSelectors
     * @throws InvalidSyntaxException if received bytes are malformed.
     * @hide
     */
    public static IkeTrafficSelector[] decodeIkeTrafficSelectors(int numTs, byte[] tsBytes)
            throws InvalidSyntaxException {
        IkeTrafficSelector[] tsArray = new IkeTrafficSelector[numTs];
        ByteBuffer inputBuffer = ByteBuffer.wrap(tsBytes);

        try {
            for (int i = 0; i < numTs; i++) {
                int tsType = Byte.toUnsignedInt(inputBuffer.get());
                switch (tsType) {
                    case TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE:
                        tsArray[i] = decodeTrafficSelector(inputBuffer,
                                TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE);
                        break;
                    case TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE:
                        tsArray[i] = decodeTrafficSelector(inputBuffer,
                                TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE);
                        break;
                    default:
                        throw new InvalidSyntaxException(
                                "Invalid Traffic Selector type: " + tsType);
                }
            }
        } catch (BufferOverflowException e) {
            // Throw exception if any Traffic Selector has invalid length.
            throw new InvalidSyntaxException(e);
        }

        if (inputBuffer.remaining() != 0) {
            throw new InvalidSyntaxException(
                    "Unexpected trailing characters of Traffic Selectors.");
        }

        return tsArray;
    }

    // Decode Traffic Selector from a ByteBuffer. A BufferOverflowException will be thrown and
    // caught by method caller if operation reaches the input ByteBuffer's limit.
    private static IkeTrafficSelector decodeTrafficSelector(ByteBuffer inputBuffer, int tsType)
            throws InvalidSyntaxException {
        // Decode and validate IP Protocol ID
        int ipProtocolId = Byte.toUnsignedInt(inputBuffer.get());
        if (!IP_PROTOCOL_ID_SET.contains(ipProtocolId)) {
            throw new InvalidSyntaxException("Invalid IP Protocol ID.");
        }

        // Decode and validate Selector Length
        boolean isTsIpv4 = tsType == TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE;
        int expectedTsLen = isTsIpv4 ? TRAFFIC_SELECTOR_IPV4_LEN : TRAFFIC_SELECTOR_IPV6_LEN;
        int tsLength = Short.toUnsignedInt(inputBuffer.getShort());
        if (expectedTsLen != tsLength) {
            throw new InvalidSyntaxException("Invalid Traffic Selector Length.");
        }

        // Decode and validate ports
        int startPort = Short.toUnsignedInt(inputBuffer.getShort());
        int endPort = Short.toUnsignedInt(inputBuffer.getShort());
        if (!isPortRangeValid(startPort, endPort)) {
            throw new InvalidSyntaxException(
                    "Received invalid port range. startPort: "
                            + startPort
                            + " endPort: "
                            + endPort);
        }

        // Decode and validate IP addresses
        int expectedAddrLen = isTsIpv4 ? IPV4_ADDR_LEN : IPV6_ADDR_LEN;
        byte[] startAddressBytes = new byte[expectedAddrLen];
        byte[] endAddressBytes = new byte[expectedAddrLen];
        inputBuffer.get(startAddressBytes);
        inputBuffer.get(endAddressBytes);
        try {
            InetAddress startAddress = InetAddress.getByAddress(startAddressBytes);
            InetAddress endAddress = InetAddress.getByAddress(endAddressBytes);

            boolean isStartAddrIpv4 = startAddress instanceof Inet4Address;
            boolean isEndAddrIpv4 = endAddress instanceof Inet4Address;
            if (isTsIpv4 != isStartAddrIpv4 || isTsIpv4 != isEndAddrIpv4) {
                throw new InvalidSyntaxException("Invalid IP address family");
            }

            // Validate address range.
            if (compareInetAddressTo(startAddress, endAddress) > 0) {
                throw new InvalidSyntaxException("Received invalid IP address range.");
            }

            return new IkeTrafficSelector(
                    tsType,
                    ipProtocolId,
                    expectedTsLen,
                    startPort,
                    endPort,
                    startAddress,
                    endAddress);
        } catch (ClassCastException | UnknownHostException | IllegalArgumentException e) {
            throw new InvalidSyntaxException(e);
        }
    }

    // Validate port range.
    private static boolean isPortRangeValid(int startPort, int endPort) {
        return (startPort >= PORT_NUMBER_MIN
                && startPort <= PORT_NUMBER_MAX
                && endPort >= PORT_NUMBER_MIN
                && endPort <= PORT_NUMBER_MAX
                && startPort <= endPort);
    }

    // Compare two InetAddresses. Return -1 if the first input is smaller; 1 if the second input is
    // smaller; 0 if two addresses are equal.
    // TODO: Consider moving it to the platform code in the future./
    private static int compareInetAddressTo(InetAddress leftAddress, InetAddress rightAddress) {
        byte[] leftAddrBytes = leftAddress.getAddress();
        byte[] rightAddrBytes = rightAddress.getAddress();

        if (leftAddrBytes.length != rightAddrBytes.length) {
            throw new IllegalArgumentException("Two addresses are different types.");
        }

        for (int i = 0; i < leftAddrBytes.length; i++) {
            int unsignedByteLeft = Byte.toUnsignedInt(leftAddrBytes[i]);
            int unsignedByteRight = Byte.toUnsignedInt(rightAddrBytes[i]);

            int result = Integer.compare(unsignedByteLeft, unsignedByteRight);
            if (result != 0) return result;
        }
        return 0;
    }

    /**
     * Check if the input IkeTrafficSelector is a subset of this instance.
     *
     * @param ts the provided IkeTrafficSelector to check.
     * @return true if the input IkeTrafficSelector is a subset of this instance, otherwise false.
     * @hide
     */
    public boolean contains(IkeTrafficSelector ts) {
        if (tsType == ts.tsType
                && ipProtocolId == ts.ipProtocolId
                && startPort <= ts.startPort
                && endPort >= ts.endPort
                && compareInetAddressTo(startingAddress, ts.startingAddress) <= 0
                && compareInetAddressTo(endingAddress, ts.endingAddress) >= 0) {
            return true;
        }
        return false;
    }

    /** @hide */
    @Override
    public int hashCode() {
        return Objects.hash(
                tsType,
                ipProtocolId,
                selectorLength,
                startPort,
                endPort,
                startingAddress,
                endingAddress);
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IkeTrafficSelector)) return false;

        IkeTrafficSelector other = (IkeTrafficSelector) o;

        if (tsType != other.tsType
                || ipProtocolId != other.ipProtocolId
                || startPort != other.startPort
                || endPort != other.endPort) {
            return false;
        }

        switch (tsType) {
            case TRAFFIC_SELECTOR_TYPE_IPV4_ADDR_RANGE:
                return (((Inet4Address) startingAddress)
                                .equals((Inet4Address) other.startingAddress)
                        && ((Inet4Address) endingAddress)
                                .equals((Inet4Address) other.endingAddress));
            case TRAFFIC_SELECTOR_TYPE_IPV6_ADDR_RANGE:
                return (((Inet6Address) startingAddress)
                                .equals((Inet6Address) other.startingAddress)
                        && ((Inet6Address) endingAddress)
                                .equals((Inet6Address) other.endingAddress));
            default:
                throw new UnsupportedOperationException("Unrecognized TS type");
        }
    }

    /**
     * Encode traffic selector to ByteBuffer.
     *
     * <p>This method will be only called by IkeTsPayload for building an outbound IKE message.
     *
     * @param byteBuffer destination ByteBuffer that stores encoded traffic selector.
     * @hide
     */
    public void encodeToByteBuffer(ByteBuffer byteBuffer) {
        byteBuffer
                .put((byte) tsType)
                .put((byte) ipProtocolId)
                .putShort((short) selectorLength)
                .putShort((short) startPort)
                .putShort((short) endPort)
                .put(startingAddress.getAddress())
                .put(endingAddress.getAddress());
    }
}
