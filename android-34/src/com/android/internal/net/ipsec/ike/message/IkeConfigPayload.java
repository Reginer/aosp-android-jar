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

package com.android.internal.net.ipsec.ike.message;

import android.annotation.IntDef;
import android.net.LinkAddress;
import android.net.ipsec.ike.IkeManager;
import android.net.ipsec.ike.IkeSessionParams;
import android.net.ipsec.ike.IkeSessionParams.ConfigRequestIpv4PcscfServer;
import android.net.ipsec.ike.IkeSessionParams.ConfigRequestIpv6PcscfServer;
import android.net.ipsec.ike.IkeSessionParams.IkeConfigRequest;
import android.net.ipsec.ike.TunnelModeChildSessionParams.ConfigRequestIpv4Address;
import android.net.ipsec.ike.TunnelModeChildSessionParams.ConfigRequestIpv4DhcpServer;
import android.net.ipsec.ike.TunnelModeChildSessionParams.ConfigRequestIpv4DnsServer;
import android.net.ipsec.ike.TunnelModeChildSessionParams.ConfigRequestIpv4Netmask;
import android.net.ipsec.ike.TunnelModeChildSessionParams.ConfigRequestIpv6Address;
import android.net.ipsec.ike.TunnelModeChildSessionParams.ConfigRequestIpv6DnsServer;
import android.net.ipsec.ike.TunnelModeChildSessionParams.TunnelModeChildConfigRequest;
import android.net.ipsec.ike.exceptions.InvalidSyntaxException;
import android.os.PersistableBundle;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.vcn.util.PersistableBundleUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents Configuration payload.
 *
 * <p>Configuration payload is used to exchange configuration information between IKE peers.
 *
 * <p>Configuration type should be consistent with the IKE message direction (e.g. a request Config
 * Payload should be in a request IKE message). IKE library will ignore Config Payload with
 * inconsistent type or with unrecognized type.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.6">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class IkeConfigPayload extends IkePayload {
    private static final int CONFIG_HEADER_RESERVED_LEN = 3;
    private static final int CONFIG_HEADER_LEN = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        CONFIG_ATTR_INTERNAL_IP4_ADDRESS,
        CONFIG_ATTR_INTERNAL_IP4_NETMASK,
        CONFIG_ATTR_INTERNAL_IP4_DNS,
        CONFIG_ATTR_INTERNAL_IP4_DHCP,
        CONFIG_ATTR_APPLICATION_VERSION,
        CONFIG_ATTR_INTERNAL_IP6_ADDRESS,
        CONFIG_ATTR_INTERNAL_IP6_DNS,
        CONFIG_ATTR_INTERNAL_IP4_SUBNET,
        CONFIG_ATTR_SUPPORTED_ATTRIBUTES,
        CONFIG_ATTR_INTERNAL_IP6_SUBNET,
        CONFIG_ATTR_IP4_PCSCF,
        CONFIG_ATTR_IP6_PCSCF
    })
    public @interface ConfigAttr {}

    public static final int CONFIG_ATTR_INTERNAL_IP4_ADDRESS = 1;
    public static final int CONFIG_ATTR_INTERNAL_IP4_NETMASK = 2;
    public static final int CONFIG_ATTR_INTERNAL_IP4_DNS = 3;
    public static final int CONFIG_ATTR_INTERNAL_IP4_DHCP = 6;
    public static final int CONFIG_ATTR_APPLICATION_VERSION = 7;
    public static final int CONFIG_ATTR_INTERNAL_IP6_ADDRESS = 8;
    public static final int CONFIG_ATTR_INTERNAL_IP6_DNS = 10;
    public static final int CONFIG_ATTR_INTERNAL_IP4_SUBNET = 13;
    public static final int CONFIG_ATTR_SUPPORTED_ATTRIBUTES = 14;
    public static final int CONFIG_ATTR_INTERNAL_IP6_SUBNET = 15;
    public static final int CONFIG_ATTR_IP4_PCSCF = 20;
    public static final int CONFIG_ATTR_IP6_PCSCF = 21;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONFIG_TYPE_REQUEST, CONFIG_TYPE_REPLY})
    public @interface ConfigType {}

    // We don't support CONFIG_TYPE_SET and CONFIG_TYPE_ACK
    public static final int CONFIG_TYPE_REQUEST = 1;
    public static final int CONFIG_TYPE_REPLY = 2;

    @ConfigType public final int configType;
    public final List<ConfigAttribute> recognizedAttributeList;

    /** Build an IkeConfigPayload from a decoded inbound IKE packet. */
    IkeConfigPayload(boolean critical, byte[] payloadBody) throws InvalidSyntaxException {
        super(PAYLOAD_TYPE_CP, critical);

        ByteBuffer inputBuffer = ByteBuffer.wrap(payloadBody);
        configType = Byte.toUnsignedInt(inputBuffer.get());
        inputBuffer.get(new byte[CONFIG_HEADER_RESERVED_LEN]);

        recognizedAttributeList = ConfigAttribute.decodeAttributesFrom(inputBuffer);

        // For an inbound Config Payload, IKE library is only able to handle a Config Reply or IKE
        // Session attribute requests in a Config Request. For interoperability, netmask validation
        // will be skipped for Config(Request) and config payloads with unsupported config types.
        if (configType == CONFIG_TYPE_REPLY) {
            validateNetmaskInReply();
        }
    }

    /** Build an IkeConfigPayload instance for an outbound IKE packet. */
    public IkeConfigPayload(boolean isReply, List<ConfigAttribute> attributeList) {
        super(PAYLOAD_TYPE_CP, false);
        this.configType = isReply ? CONFIG_TYPE_REPLY : CONFIG_TYPE_REQUEST;
        this.recognizedAttributeList = attributeList;
    }

    private void validateNetmaskInReply() throws InvalidSyntaxException {
        boolean hasIpv4Address = false;
        int numNetmask = 0;

        for (ConfigAttribute attr : recognizedAttributeList) {
            if (attr.isEmptyValue()) {
                IkeManager.getIkeLog()
                        .d(
                                "IkeConfigPayload",
                                "Found empty attribute in a Config Payload reply "
                                        + attr.attributeType);
            }
            switch (attr.attributeType) {
                case CONFIG_ATTR_INTERNAL_IP4_ADDRESS:
                    if (!attr.isEmptyValue()) hasIpv4Address = true;
                    break;
                case CONFIG_ATTR_INTERNAL_IP4_NETMASK:
                    if (!attr.isEmptyValue()) numNetmask++;
                    break;
                default:
                    continue;
            }
        }

        if (!hasIpv4Address && numNetmask > 0) {
            throw new InvalidSyntaxException(
                    "Found INTERNAL_IP4_NETMASK attribute but no INTERNAL_IP4_ADDRESS attribute");
        }

        if (numNetmask > 1) {
            throw new InvalidSyntaxException("Found more than one INTERNAL_IP4_NETMASK");
        }
    }

    // TODO: Create ConfigAttribute subclasses for each attribute.

    /** This class represents common information of all Configuration Attributes. */
    public abstract static class ConfigAttribute {
        private static final String ENCODED_ATTRIBUTE_BYTES_KEY = "encodedAttribute";

        private static final int ATTRIBUTE_TYPE_MASK = 0x7fff;

        private static final int ATTRIBUTE_HEADER_LEN = 4;
        private static final int IPV4_PREFIX_LEN_MAX = 32;

        protected static final int VALUE_LEN_NOT_INCLUDED = 0;

        protected static final int IPV4_ADDRESS_LEN = 4;
        protected static final int IPV6_ADDRESS_LEN = 16;
        protected static final int PREFIX_LEN_LEN = 1;

        public final int attributeType;

        protected ConfigAttribute(int attributeType) {
            this.attributeType = attributeType;
        }

        protected ConfigAttribute(int attributeType, int len) throws InvalidSyntaxException {
            this(attributeType);

            if (!isLengthValid(len)) {
                throw new InvalidSyntaxException("Invalid configuration length");
            }
        }

        /**
         * Constructs this object by deserializing a PersistableBundle.
         *
         * <p>Constructed ConfigAttributes are guaranteed to be valid, as checked by
         * #decodeAttributesFrom(ByteBuffer)
         */
        public static ConfigAttribute fromPersistableBundle(PersistableBundle in) {
            Objects.requireNonNull(in, "PersistableBundle is null");

            PersistableBundle byteArrayBundle =
                    in.getPersistableBundle(ENCODED_ATTRIBUTE_BYTES_KEY);
            ByteBuffer buffer =
                    ByteBuffer.wrap(PersistableBundleUtils.toByteArray(byteArrayBundle));

            ConfigAttribute attribute;
            try {
                attribute = decodeSingleAttributeFrom(buffer);
            } catch (NegativeArraySizeException
                    | BufferUnderflowException
                    | InvalidSyntaxException e) {
                throw new IllegalArgumentException(
                        "PersistableBundle contains invalid Config request");
            }

            if (buffer.hasRemaining()) {
                throw new IllegalArgumentException(
                        "Unexpected trailing bytes in Config request PersistableBundle");
            }

            return attribute;
        }

        /** Serializes this object to a PersistableBundle */
        public PersistableBundle toPersistableBundle() {
            final PersistableBundle result = new PersistableBundle();

            ByteBuffer buffer = ByteBuffer.allocate(getAttributeLen());
            encodeAttributeToByteBuffer(buffer);

            result.putPersistableBundle(
                    ENCODED_ATTRIBUTE_BYTES_KEY,
                    PersistableBundleUtils.fromByteArray(buffer.array()));
            return result;
        }

        /**
         * Package private method to decode ConfigAttribute list from an inbound packet
         *
         * <p>NegativeArraySizeException and BufferUnderflowException will be caught in {@link
         * IkeMessage}
         */
        static List<ConfigAttribute> decodeAttributesFrom(ByteBuffer inputBuffer)
                throws InvalidSyntaxException {
            List<ConfigAttribute> configList = new LinkedList();

            while (inputBuffer.hasRemaining()) {
                ConfigAttribute attribute = decodeSingleAttributeFrom(inputBuffer);
                if (attribute != null) {
                    configList.add(attribute);
                }
            }

            return configList;
        }

        /**
         * Method to decode a single ConfigAttribute from a ByteBuffer.
         *
         * <p>Caller should be responsible for handling NegativeArraySizeException and
         * BufferUnderflowException.
         */
        private static ConfigAttribute decodeSingleAttributeFrom(ByteBuffer inputBuffer)
                throws InvalidSyntaxException {
            int attributeType = Short.toUnsignedInt(inputBuffer.getShort());
            int length = Short.toUnsignedInt(inputBuffer.getShort());
            byte[] value = new byte[length];
            inputBuffer.get(value);

            switch (attributeType) {
                case CONFIG_ATTR_INTERNAL_IP4_ADDRESS:
                    return new ConfigAttributeIpv4Address(value);
                case CONFIG_ATTR_INTERNAL_IP4_NETMASK:
                    return new ConfigAttributeIpv4Netmask(value);
                case CONFIG_ATTR_INTERNAL_IP4_DNS:
                    return new ConfigAttributeIpv4Dns(value);
                case CONFIG_ATTR_INTERNAL_IP4_DHCP:
                    return new ConfigAttributeIpv4Dhcp(value);
                case CONFIG_ATTR_APPLICATION_VERSION:
                    return new ConfigAttributeAppVersion(value);
                case CONFIG_ATTR_INTERNAL_IP6_ADDRESS:
                    return new ConfigAttributeIpv6Address(value);
                case CONFIG_ATTR_INTERNAL_IP6_DNS:
                    return new ConfigAttributeIpv6Dns(value);
                case CONFIG_ATTR_INTERNAL_IP4_SUBNET:
                    return new ConfigAttributeIpv4Subnet(value);
                case CONFIG_ATTR_INTERNAL_IP6_SUBNET:
                    return new ConfigAttributeIpv6Subnet(value);
                case CONFIG_ATTR_IP4_PCSCF:
                    return new ConfigAttributeIpv4Pcscf(value);
                case CONFIG_ATTR_IP6_PCSCF:
                    return new ConfigAttributeIpv6Pcscf(value);
                default:
                    IkeManager.getIkeLog()
                            .i("IkeConfigPayload", "Unrecognized attribute type: " + attributeType);
                    return null;
            }
        }

        /** Encode attribute to ByteBuffer. */
        public void encodeAttributeToByteBuffer(ByteBuffer buffer) {
            buffer.putShort((short) (attributeType & ATTRIBUTE_TYPE_MASK))
                    .putShort((short) getValueLength());
            encodeValueToByteBuffer(buffer);
        }

        /** Get attribute length. */
        public int getAttributeLen() {
            return ATTRIBUTE_HEADER_LEN + getValueLength();
        }

        /** Returns if this attribute value is empty. */
        public boolean isEmptyValue() {
            return getValueLength() == VALUE_LEN_NOT_INCLUDED;
        }

        @Override
        public int hashCode() {
            return Objects.hash(attributeType);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ConfigAttribute)) {
                return false;
            }

            return attributeType == ((ConfigAttribute) o).attributeType;
        }

        protected static int netmaskToPrefixLen(Inet4Address address) {
            byte[] bytes = address.getAddress();

            int netmaskInt = ByteBuffer.wrap(bytes).getInt();
            int leftmostBitMask = 0x80000000;

            int prefixLen = 0;
            while ((netmaskInt & leftmostBitMask) == leftmostBitMask) {
                prefixLen++;
                netmaskInt <<= 1;
            }

            if (netmaskInt != 0) {
                throw new IllegalArgumentException("Invalid netmask address");
            }

            return prefixLen;
        }

        protected static byte[] prefixToNetmaskBytes(int prefixLen) {
            if (prefixLen > IPV4_PREFIX_LEN_MAX || prefixLen < 0) {
                throw new IllegalArgumentException("Invalid IPv4 prefix length.");
            }

            int netmaskInt = (int) (((long) 0xffffffff) << (IPV4_PREFIX_LEN_MAX - prefixLen));
            byte[] netmask = new byte[IPV4_ADDRESS_LEN];

            ByteBuffer buffer = ByteBuffer.allocate(IPV4_ADDRESS_LEN);
            buffer.putInt(netmaskInt);
            return buffer.array();
        }

        protected abstract void encodeValueToByteBuffer(ByteBuffer buffer);

        protected abstract int getValueLength();

        protected abstract boolean isLengthValid(int length);
    }

    /** This class supports strong typing for IkeConfigRequest(s) */
    public abstract static class IkeConfigAttribute extends ConfigAttribute
            implements IkeConfigRequest {
        protected IkeConfigAttribute(int attributeType) {
            super(attributeType);
        }

        protected IkeConfigAttribute(int attributeType, int len) throws InvalidSyntaxException {
            super(attributeType, len);
        }
    }

    /** This class supports strong typing for TunnelModeChildConfigRequest(s) */
    public abstract static class TunnelModeChildConfigAttribute extends ConfigAttribute
            implements TunnelModeChildConfigRequest {
        protected TunnelModeChildConfigAttribute(int attributeType) {
            super(attributeType);
        }

        protected TunnelModeChildConfigAttribute(int attributeType, int len)
                throws InvalidSyntaxException {
            super(attributeType, len);
        }
    }

    /**
     * This class represents common information of all Tunnel Mode Child Session Configuration
     * Attributes for which the value is one IPv4 address or empty.
     */
    abstract static class TunnelModeChildConfigAttrIpv4AddressBase
            extends TunnelModeChildConfigAttribute implements TunnelModeChildConfigRequest {
        public final Inet4Address address;

        protected TunnelModeChildConfigAttrIpv4AddressBase(
                int attributeType, Inet4Address address) {
            super(attributeType);
            this.address = address;
        }

        protected TunnelModeChildConfigAttrIpv4AddressBase(int attributeType) {
            super(attributeType);
            this.address = null;
        }

        protected TunnelModeChildConfigAttrIpv4AddressBase(int attributeType, byte[] value)
                throws InvalidSyntaxException {
            super(attributeType, value.length);

            if (value.length == VALUE_LEN_NOT_INCLUDED) {
                address = null;
                return;
            }

            try {
                InetAddress netAddress = InetAddress.getByAddress(value);

                if (!(netAddress instanceof Inet4Address)) {
                    throw new InvalidSyntaxException("Invalid IPv4 address.");
                }
                address = (Inet4Address) netAddress;
            } catch (UnknownHostException e) {
                throw new InvalidSyntaxException("Invalid attribute value", e);
            }
        }

        @Override
        protected void encodeValueToByteBuffer(ByteBuffer buffer) {
            if (address == null) return; // No encoding necessary

            buffer.put(address.getAddress());
        }

        @Override
        protected int getValueLength() {
            return address == null ? 0 : IPV4_ADDRESS_LEN;
        }

        @Override
        protected boolean isLengthValid(int length) {
            return length == IPV4_ADDRESS_LEN || length == VALUE_LEN_NOT_INCLUDED;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), address);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof TunnelModeChildConfigAttrIpv4AddressBase)) {
                return false;
            }

            TunnelModeChildConfigAttrIpv4AddressBase other =
                    (TunnelModeChildConfigAttrIpv4AddressBase) o;

            return Objects.equals(address, other.address);
        }
    }

    /**
     * This class represents common information of all IKE Session Configuration Attributes for
     * which the value is one IPv4 address or empty.
     */
    abstract static class IkeConfigAttrIpv4AddressBase extends IkeConfigAttribute
            implements IkeSessionParams.IkeConfigRequest {
        public final Inet4Address address;

        protected IkeConfigAttrIpv4AddressBase(int attributeType, Inet4Address address) {
            super(attributeType);
            this.address = address;
        }

        protected IkeConfigAttrIpv4AddressBase(int attributeType) {
            super(attributeType);
            this.address = null;
        }

        protected IkeConfigAttrIpv4AddressBase(int attributeType, byte[] value)
                throws InvalidSyntaxException {
            super(attributeType, value.length);

            if (value.length == VALUE_LEN_NOT_INCLUDED) {
                address = null;
                return;
            }

            try {
                InetAddress netAddress = InetAddress.getByAddress(value);

                if (!(netAddress instanceof Inet4Address)) {
                    throw new InvalidSyntaxException("Invalid IPv4 address.");
                }
                address = (Inet4Address) netAddress;
            } catch (UnknownHostException e) {
                throw new InvalidSyntaxException("Invalid attribute value", e);
            }
        }

        @Override
        protected void encodeValueToByteBuffer(ByteBuffer buffer) {
            if (address == null) return; // No encoding necessary

            buffer.put(address.getAddress());
        }

        @Override
        protected int getValueLength() {
            return address == null ? 0 : IPV4_ADDRESS_LEN;
        }

        @Override
        protected boolean isLengthValid(int length) {
            return length == IPV4_ADDRESS_LEN || length == VALUE_LEN_NOT_INCLUDED;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), address);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof IkeConfigAttrIpv4AddressBase)) {
                return false;
            }

            IkeConfigAttrIpv4AddressBase other = (IkeConfigAttrIpv4AddressBase) o;

            return Objects.equals(address, other.address);
        }
    }

    /** This class represents Configuration Attribute for IPv4 internal address. */
    public static class ConfigAttributeIpv4Address extends TunnelModeChildConfigAttrIpv4AddressBase
            implements ConfigRequestIpv4Address {
        /** Construct an instance with specified address for an outbound packet. */
        public ConfigAttributeIpv4Address(Inet4Address ipv4Address) {
            super(CONFIG_ATTR_INTERNAL_IP4_ADDRESS, ipv4Address);
        }

        /**
         * Construct an instance without a specified address for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv4Address() {
            super(CONFIG_ATTR_INTERNAL_IP4_ADDRESS);
        }

        /** Construct an instance with a decoded inbound packet. */
        @VisibleForTesting
        ConfigAttributeIpv4Address(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_INTERNAL_IP4_ADDRESS, value);
        }

        @Override
        public Inet4Address getAddress() {
            return address;
        }
    }

    /**
     * This class represents Configuration Attribute for IPv4 netmask.
     *
     * <p>Non-empty values for this attribute in a CFG_REQUEST do not make sense and thus MUST NOT
     * be included
     */
    public static class ConfigAttributeIpv4Netmask extends TunnelModeChildConfigAttrIpv4AddressBase
            implements ConfigRequestIpv4Netmask {
        /**
         * Construct an instance without a specified netmask for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv4Netmask() {
            super(CONFIG_ATTR_INTERNAL_IP4_NETMASK);
        }

        /** Construct an instance with a decoded inbound packet. */
        @VisibleForTesting
        public ConfigAttributeIpv4Netmask(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_INTERNAL_IP4_NETMASK, value);

            if (address == null) return;
            try {
                netmaskToPrefixLen(address);
            } catch (IllegalArgumentException e) {
                throw new InvalidSyntaxException("Invalid attribute value", e);
            }
        }

        /** Convert netmask to prefix length. */
        public int getPrefixLen() {
            return netmaskToPrefixLen(address);
        }
    }

    /** This class represents Configuration Attribute for IPv4 DHCP server. */
    public static class ConfigAttributeIpv4Dhcp extends TunnelModeChildConfigAttrIpv4AddressBase
            implements ConfigRequestIpv4DhcpServer {
        /** Construct an instance with specified DHCP server address for an outbound packet. */
        public ConfigAttributeIpv4Dhcp(Inet4Address ipv4Address) {
            super(CONFIG_ATTR_INTERNAL_IP4_DHCP, ipv4Address);
        }

        /**
         * Construct an instance without a specified DHCP server address for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv4Dhcp() {
            super(CONFIG_ATTR_INTERNAL_IP4_DHCP);
        }

        /** Construct an instance with a decoded inbound packet. */
        @VisibleForTesting
        ConfigAttributeIpv4Dhcp(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_INTERNAL_IP4_DHCP, value);
        }

        public Inet4Address getAddress() {
            return address;
        }
    }

    /**
     * This class represents Configuration Attribute for IPv4 DNS.
     *
     * <p>There is no use case to create a DNS request for a specfic DNS server address. As an IKE
     * client, we will only support building an empty DNS attribute for an outbound IKE packet.
     */
    public static class ConfigAttributeIpv4Dns extends TunnelModeChildConfigAttrIpv4AddressBase
            implements ConfigRequestIpv4DnsServer {
        /** Construct an instance with specified DNS server address for an outbound packet. */
        public ConfigAttributeIpv4Dns(Inet4Address ipv4Address) {
            super(CONFIG_ATTR_INTERNAL_IP4_DNS, ipv4Address);
        }

        /**
         * Construct an instance without a specified DNS server address for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv4Dns() {
            super(CONFIG_ATTR_INTERNAL_IP4_DNS);
        }

        /** Construct an instance with a decoded inbound packet. */
        @VisibleForTesting
        ConfigAttributeIpv4Dns(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_INTERNAL_IP4_DNS, value);
        }

        public Inet4Address getAddress() {
            return address;
        }
    }

    // TODO: b/145454043 Remove constructors for building outbound
    // INTERNAL_IP4_SUBNET/INTERNAL_IP6_SUBNET because they should never be in a config request and
    // IKE library, as an IKE client, will never send them in a config reply either.

    /**
     * This class represents Configuration Attribute for IPv4 subnets.
     *
     * <p>According to RFC 7296, INTERNAL_IP4_SUBNET in configuration requests cannot be used
     * reliably because the meaning is unclear.
     */
    public static class ConfigAttributeIpv4Subnet extends TunnelModeChildConfigAttribute {
        private static final int VALUE_LEN = 2 * IPV4_ADDRESS_LEN;

        public final LinkAddress linkAddress;

        /** Construct an instance with specified subnet for an outbound packet. */
        public ConfigAttributeIpv4Subnet(LinkAddress ipv4LinkAddress) {
            super(CONFIG_ATTR_INTERNAL_IP4_SUBNET);

            if (!ipv4LinkAddress.isIpv4()) {
                throw new IllegalArgumentException("Input LinkAddress is not IPv4");
            }

            this.linkAddress = ipv4LinkAddress;
        }

        /**
         * Construct an instance without a specified subnet for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv4Subnet() {
            super(CONFIG_ATTR_INTERNAL_IP4_SUBNET);
            this.linkAddress = null;
        }

        /** Construct an instance with a decoded inbound packet. */
        @VisibleForTesting
        ConfigAttributeIpv4Subnet(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_INTERNAL_IP4_SUBNET, value.length);

            if (value.length == VALUE_LEN_NOT_INCLUDED) {
                linkAddress = null;
                return;
            }

            try {
                ByteBuffer inputBuffer = ByteBuffer.wrap(value);
                byte[] ipBytes = new byte[IPV4_ADDRESS_LEN];
                inputBuffer.get(ipBytes);
                byte[] netmaskBytes = new byte[IPV4_ADDRESS_LEN];
                inputBuffer.get(netmaskBytes);

                InetAddress address = InetAddress.getByAddress(ipBytes);
                InetAddress netmask = InetAddress.getByAddress(netmaskBytes);
                validateInet4AddressTypeOrThrow(address);
                validateInet4AddressTypeOrThrow(netmask);

                linkAddress = new LinkAddress(address, netmaskToPrefixLen((Inet4Address) netmask));
            } catch (UnknownHostException | IllegalArgumentException e) {
                throw new InvalidSyntaxException("Invalid attribute value", e);
            }
        }

        private void validateInet4AddressTypeOrThrow(InetAddress address) {
            if (!(address instanceof Inet4Address)) {
                throw new IllegalArgumentException("Input InetAddress is not IPv4");
            }
        }

        @Override
        protected void encodeValueToByteBuffer(ByteBuffer buffer) {
            if (linkAddress == null) {
                buffer.put(new byte[VALUE_LEN_NOT_INCLUDED]);
                return;
            }
            byte[] netmaskBytes = prefixToNetmaskBytes(linkAddress.getPrefixLength());
            buffer.put(linkAddress.getAddress().getAddress()).put(netmaskBytes);
        }

        @Override
        protected int getValueLength() {
            return linkAddress == null ? 0 : VALUE_LEN;
        }

        @Override
        protected boolean isLengthValid(int length) {
            return length == VALUE_LEN || length == VALUE_LEN_NOT_INCLUDED;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), linkAddress);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof ConfigAttributeIpv4Subnet)) {
                return false;
            }

            ConfigAttributeIpv4Subnet other = (ConfigAttributeIpv4Subnet) o;

            return Objects.equals(linkAddress, other.linkAddress);
        }
    }

    /** This class represents an IPv4 P_CSCF address attribute */
    public static class ConfigAttributeIpv4Pcscf extends IkeConfigAttrIpv4AddressBase
            implements ConfigRequestIpv4PcscfServer {
        /** Construct an instance with a specified P_CSCF server address for an outbound packet. */
        public ConfigAttributeIpv4Pcscf(Inet4Address ipv4Address) {
            super(CONFIG_ATTR_IP4_PCSCF, ipv4Address);
        }

        /**
         * Construct an instance without a specified P_CSCF server address for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv4Pcscf() {
            super(CONFIG_ATTR_IP4_PCSCF);
        }

        /** Construct an instance with a decoded inbound packet. */
        @VisibleForTesting
        ConfigAttributeIpv4Pcscf(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_IP4_PCSCF, value);
        }

        @Override
        public Inet4Address getAddress() {
            return address;
        }
    }

    /**
     * This class represents common information of all Tunnel Mode Child Session Configuration
     * Attributes for which the value is one IPv6 address or empty.
     */
    abstract static class TunnelModeChildConfigAttrIpv6AddressBase
            extends TunnelModeChildConfigAttribute implements TunnelModeChildConfigRequest {
        public final Inet6Address address;

        protected TunnelModeChildConfigAttrIpv6AddressBase(
                int attributeType, Inet6Address address) {
            super(attributeType);
            this.address = address;
        }

        protected TunnelModeChildConfigAttrIpv6AddressBase(int attributeType) {
            super(attributeType);
            this.address = null;
        }

        protected TunnelModeChildConfigAttrIpv6AddressBase(int attributeType, byte[] value)
                throws InvalidSyntaxException {
            super(attributeType, value.length);

            if (value.length == VALUE_LEN_NOT_INCLUDED) {
                address = null;
                return;
            }

            try {
                InetAddress netAddress = InetAddress.getByAddress(value);

                if (!(netAddress instanceof Inet6Address)) {
                    throw new InvalidSyntaxException("Invalid IPv6 address.");
                }
                address = (Inet6Address) netAddress;
            } catch (UnknownHostException e) {
                throw new InvalidSyntaxException("Invalid attribute value", e);
            }
        }

        @Override
        protected void encodeValueToByteBuffer(ByteBuffer buffer) {
            if (address == null) return; // No encoding necessary

            buffer.put(address.getAddress());
        }

        @Override
        protected int getValueLength() {
            return address == null ? 0 : IPV6_ADDRESS_LEN;
        }

        @Override
        protected boolean isLengthValid(int length) {
            return length == IPV6_ADDRESS_LEN || length == VALUE_LEN_NOT_INCLUDED;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), address);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof TunnelModeChildConfigAttrIpv6AddressBase)) {
                return false;
            }

            TunnelModeChildConfigAttrIpv6AddressBase other =
                    (TunnelModeChildConfigAttrIpv6AddressBase) o;

            return Objects.equals(address, other.address);
        }
    }

    /**
     * This class represents common information of all IKE Session Configuration Attributes for
     * which the value is one IPv6 address or empty.
     */
    abstract static class IkeConfigAttrIpv6AddressBase extends IkeConfigAttribute
            implements IkeConfigRequest {
        public final Inet6Address address;

        protected IkeConfigAttrIpv6AddressBase(int attributeType, Inet6Address address) {
            super(attributeType);
            this.address = address;
        }

        protected IkeConfigAttrIpv6AddressBase(int attributeType) {
            super(attributeType);
            this.address = null;
        }

        protected IkeConfigAttrIpv6AddressBase(int attributeType, byte[] value)
                throws InvalidSyntaxException {
            super(attributeType, value.length);

            if (value.length == VALUE_LEN_NOT_INCLUDED) {
                address = null;
                return;
            }

            try {
                InetAddress netAddress = InetAddress.getByAddress(value);

                if (!(netAddress instanceof Inet6Address)) {
                    throw new InvalidSyntaxException("Invalid IPv6 address.");
                }
                address = (Inet6Address) netAddress;
            } catch (UnknownHostException e) {
                throw new InvalidSyntaxException("Invalid attribute value", e);
            }
        }

        @Override
        protected void encodeValueToByteBuffer(ByteBuffer buffer) {
            if (address == null) return; // No encoding necessary

            buffer.put(address.getAddress());
        }

        @Override
        protected int getValueLength() {
            return address == null ? 0 : IPV6_ADDRESS_LEN;
        }

        @Override
        protected boolean isLengthValid(int length) {
            return length == IPV6_ADDRESS_LEN || length == VALUE_LEN_NOT_INCLUDED;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), address);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof IkeConfigAttrIpv6AddressBase)) {
                return false;
            }

            IkeConfigAttrIpv6AddressBase other = (IkeConfigAttrIpv6AddressBase) o;

            return Objects.equals(address, other.address);
        }
    }

    /**
     * This class represents common information of all Configuration Attributes for which the value
     * is an IPv6 address range.
     *
     * <p>These attributes contains an IPv6 address and a prefix length.
     */
    abstract static class TunnelModeChildConfigAttrIpv6AddrRangeBase
            extends TunnelModeChildConfigAttribute {
        private static final int VALUE_LEN = IPV6_ADDRESS_LEN + PREFIX_LEN_LEN;

        public final LinkAddress linkAddress;

        protected TunnelModeChildConfigAttrIpv6AddrRangeBase(
                int attributeType, LinkAddress ipv6LinkAddress) {
            super(attributeType);

            validateIpv6LinkAddressTypeOrThrow(ipv6LinkAddress);
            linkAddress = ipv6LinkAddress;
        }

        protected TunnelModeChildConfigAttrIpv6AddrRangeBase(int attributeType) {
            super(attributeType);
            linkAddress = null;
        }

        protected TunnelModeChildConfigAttrIpv6AddrRangeBase(int attributeType, byte[] value)
                throws InvalidSyntaxException {
            super(attributeType, value.length);

            if (value.length == VALUE_LEN_NOT_INCLUDED) {
                linkAddress = null;
                return;
            }

            try {
                ByteBuffer inputBuffer = ByteBuffer.wrap(value);
                byte[] ip6AddrBytes = new byte[IPV6_ADDRESS_LEN];
                inputBuffer.get(ip6AddrBytes);
                InetAddress address = InetAddress.getByAddress(ip6AddrBytes);

                int prefixLen = Byte.toUnsignedInt(inputBuffer.get());

                linkAddress = new LinkAddress(address, prefixLen);
                validateIpv6LinkAddressTypeOrThrow(linkAddress);
            } catch (UnknownHostException | IllegalArgumentException e) {
                throw new InvalidSyntaxException("Invalid attribute value", e);
            }
        }

        private void validateIpv6LinkAddressTypeOrThrow(LinkAddress address) {
            if (!address.isIpv6()) {
                throw new IllegalArgumentException("Input LinkAddress is not IPv6");
            }
        }

        @Override
        protected void encodeValueToByteBuffer(ByteBuffer buffer) {
            if (linkAddress == null) {
                buffer.put(new byte[VALUE_LEN_NOT_INCLUDED]);
                return;
            }

            buffer.put(linkAddress.getAddress().getAddress())
                    .put((byte) linkAddress.getPrefixLength());
        }

        @Override
        protected int getValueLength() {
            return linkAddress == null ? VALUE_LEN_NOT_INCLUDED : VALUE_LEN;
        }

        @Override
        protected boolean isLengthValid(int length) {
            return length == VALUE_LEN || length == VALUE_LEN_NOT_INCLUDED;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), linkAddress);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof TunnelModeChildConfigAttrIpv6AddrRangeBase)) {
                return false;
            }

            TunnelModeChildConfigAttrIpv6AddrRangeBase other =
                    (TunnelModeChildConfigAttrIpv6AddrRangeBase) o;

            return Objects.equals(linkAddress, other.linkAddress);
        }
    }

    /** This class represents Configuration Attribute for IPv6 internal addresses. */
    public static class ConfigAttributeIpv6Address
            extends TunnelModeChildConfigAttrIpv6AddrRangeBase implements ConfigRequestIpv6Address {
        /** Construct an instance with specified address for an outbound packet. */
        public ConfigAttributeIpv6Address(LinkAddress ipv6LinkAddress) {
            super(CONFIG_ATTR_INTERNAL_IP6_ADDRESS, ipv6LinkAddress);
        }

        /**
         * Construct an instance without a specified address for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv6Address() {
            super(CONFIG_ATTR_INTERNAL_IP6_ADDRESS);
        }

        /** Construct an instance with a decoded inbound packet. */
        @VisibleForTesting
        ConfigAttributeIpv6Address(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_INTERNAL_IP6_ADDRESS, value);
        }

        @Override
        public Inet6Address getAddress() {
            return linkAddress == null ? null : (Inet6Address) linkAddress.getAddress();
        }

        @Override
        public int getPrefixLength() {
            return linkAddress == null ? 0 : linkAddress.getPrefixLength();
        }
    }

    /**
     * This class represents Configuration Attribute for IPv6 subnets.
     *
     * <p>According to RFC 7296, INTERNAL_IP6_SUBNET in configuration requests cannot be used
     * reliably because the meaning is unclear.
     */
    public static class ConfigAttributeIpv6Subnet
            extends TunnelModeChildConfigAttrIpv6AddrRangeBase {
        /** Construct an instance with specified subnet for an outbound packet. */
        public ConfigAttributeIpv6Subnet(LinkAddress ipv6LinkAddress) {
            super(CONFIG_ATTR_INTERNAL_IP6_SUBNET, ipv6LinkAddress);
        }

        /**
         * Construct an instance without a specified subnet for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv6Subnet() {
            super(CONFIG_ATTR_INTERNAL_IP6_SUBNET);
        }

        /** Construct an instance with a decoded inbound packet. */
        @VisibleForTesting
        ConfigAttributeIpv6Subnet(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_INTERNAL_IP6_SUBNET, value);
        }
    }

    /**
     * This class represents Configuration Attribute for IPv6 DNS.
     *
     * <p>There is no use case to create a DNS request for a specfic DNS server address. As an IKE
     * client, we will only support building an empty DNS attribute for an outbound IKE packet.
     */
    public static class ConfigAttributeIpv6Dns extends TunnelModeChildConfigAttrIpv6AddressBase
            implements ConfigRequestIpv6DnsServer {
        /** Construct an instance with specified DNS server address for an outbound packet. */
        public ConfigAttributeIpv6Dns(Inet6Address ipv6Address) {
            super(CONFIG_ATTR_INTERNAL_IP6_DNS, ipv6Address);
        }

        /**
         * Construct an instance without a specified DNS server address for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv6Dns() {
            super(CONFIG_ATTR_INTERNAL_IP6_DNS);
        }

        protected ConfigAttributeIpv6Dns(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_INTERNAL_IP6_DNS, value);
        }

        public Inet6Address getAddress() {
            return address;
        }
    }

    /** This class represents an IPv6 P_CSCF address attribute */
    public static class ConfigAttributeIpv6Pcscf extends IkeConfigAttrIpv6AddressBase
            implements ConfigRequestIpv6PcscfServer {
        /** Construct an instance with a specified P_CSCF server address for an outbound packet. */
        public ConfigAttributeIpv6Pcscf(Inet6Address ipv6Address) {
            super(CONFIG_ATTR_IP6_PCSCF, ipv6Address);
        }

        /**
         * Construct an instance without a specified P_CSCF server address for an outbound packet.
         *
         * <p>It must be only used in a configuration request.
         */
        public ConfigAttributeIpv6Pcscf() {
            super(CONFIG_ATTR_IP6_PCSCF);
        }

        protected ConfigAttributeIpv6Pcscf(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_IP6_PCSCF, value);
        }

        @Override
        public Inet6Address getAddress() {
            return address;
        }
    }

    /** This class represents an application version attribute */
    public static class ConfigAttributeAppVersion extends ConfigAttribute {
        private static final Charset ASCII = StandardCharsets.US_ASCII;
        private static final String APP_VERSION_NONE = "";

        public final String applicationVersion;

        /**
         * Construct an instance for an outbound packet for requesting remote application version.
         */
        public ConfigAttributeAppVersion() {
            this(APP_VERSION_NONE);
        }

        /** Construct an instance for an outbound packet with local application version. */
        public ConfigAttributeAppVersion(String localAppVersion) {
            super(CONFIG_ATTR_APPLICATION_VERSION);
            applicationVersion = localAppVersion;
        }

        /** Construct an instance from a decoded inbound packet. */
        protected ConfigAttributeAppVersion(byte[] value) throws InvalidSyntaxException {
            super(CONFIG_ATTR_APPLICATION_VERSION);
            applicationVersion = new String(value, ASCII);
        }

        protected void encodeValueToByteBuffer(ByteBuffer buffer) {
            buffer.put(applicationVersion.getBytes(ASCII));
        }

        protected int getValueLength() {
            return applicationVersion.getBytes(ASCII).length;
        }

        @Override
        protected boolean isLengthValid(int length) {
            return length >= 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), applicationVersion);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o) || !(o instanceof ConfigAttributeAppVersion)) {
                return false;
            }

            ConfigAttributeAppVersion other = (ConfigAttributeAppVersion) o;

            return Objects.equals(applicationVersion, other.applicationVersion);
        }
    }

    /**
     * Encode Configuration payload to ByteBUffer.
     *
     * @param nextPayload type of payload that follows this payload.
     * @param byteBuffer destination ByteBuffer that stores encoded payload.
     */
    @Override
    protected void encodeToByteBuffer(@PayloadType int nextPayload, ByteBuffer byteBuffer) {
        encodePayloadHeaderToByteBuffer(nextPayload, getPayloadLength(), byteBuffer);
        byteBuffer.put((byte) configType).put(new byte[CONFIG_HEADER_RESERVED_LEN]);

        for (ConfigAttribute attr : recognizedAttributeList) {
            attr.encodeAttributeToByteBuffer(byteBuffer);
        }
    }

    /**
     * Get entire payload length.
     *
     * @return entire payload length.
     */
    @Override
    protected int getPayloadLength() {
        int len = GENERIC_HEADER_LENGTH + CONFIG_HEADER_LEN;

        for (ConfigAttribute attr : recognizedAttributeList) {
            len += attr.getAttributeLen();
        }

        return len;
    }

    /**
     * Return the payload type as a String.
     *
     * @return the payload type as a String.
     */
    @Override
    public String getTypeString() {
        switch (configType) {
            case CONFIG_TYPE_REQUEST:
                return "CP(Req)";
            case CONFIG_TYPE_REPLY:
                return "CP(Reply)";
            default:
                return "CP(" + configType + ")";
        }
    }
}
