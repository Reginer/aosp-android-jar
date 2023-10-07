package com.android.networkstack.tethering.companionproxy.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ProtocolConfigTest {
    @Test
    public void capabilities_bitmask() {
        ProtocolConfig.Capabilities capabilities =
            createCapabilities(HandshakeData.CAPABILITY_TCP4_HEADER_COMPRESSION);
        assertTrue(capabilities.hasTcp4Compression());

        capabilities = createCapabilities(HandshakeData.CAPABILITY_TCP6_HEADER_COMPRESSION);
        assertTrue(capabilities.hasTcp6Compression());

        capabilities = createCapabilities(HandshakeData.CAPABILITY_UDP4_HEADER_COMPRESSION);
        assertTrue(capabilities.hasUdp4Compression());

        capabilities = createCapabilities(HandshakeData.CAPABILITY_UDP6_HEADER_COMPRESSION);
        assertTrue(capabilities.hasUdp6Compression());

        capabilities = createCapabilities(HandshakeData.CAPABILITY_NET_ID);
        assertTrue(capabilities.hasNetId());

        capabilities = new ProtocolConfig.Capabilities(0x40000000);
        assertEquals(0, capabilities.getBitmask());
    }

    @Test
    public void capabilities_merge() {
        ProtocolConfig.Capabilities capabilities1 = createCapabilities(
            HandshakeData.CAPABILITY_TCP4_HEADER_COMPRESSION
                | HandshakeData.CAPABILITY_UDP4_HEADER_COMPRESSION
                | HandshakeData.CAPABILITY_NET_ID);
        assertTrue(capabilities1.hasTcp4Compression());
        assertTrue(capabilities1.hasUdp4Compression());
        assertTrue(capabilities1.hasNetId());

        ProtocolConfig.Capabilities capabilities2 = createCapabilities(
            HandshakeData.CAPABILITY_TCP4_HEADER_COMPRESSION
                | HandshakeData.CAPABILITY_TCP6_HEADER_COMPRESSION
                | HandshakeData.CAPABILITY_NET_ID);
        assertTrue(capabilities2.hasTcp4Compression());
        assertTrue(capabilities2.hasTcp6Compression());
        assertTrue(capabilities2.hasNetId());

        ProtocolConfig.Capabilities capabilities =
            ProtocolConfig.Capabilities.merge(capabilities1, capabilities2);
        assertEquals(
            HandshakeData.CAPABILITY_TCP4_HEADER_COMPRESSION
                | HandshakeData.CAPABILITY_NET_ID,
            capabilities.getBitmask());
    }

    private static ProtocolConfig.Capabilities createCapabilities(int bitmask) {
        ProtocolConfig.Capabilities capabilities = new ProtocolConfig.Capabilities(bitmask);
        assertEquals(bitmask, capabilities.getBitmask());
        return capabilities;
    }

    @Test
    public void protocolConfig() {
        ProtocolConfig config = new ProtocolConfig(
            10, createCapabilities(HandshakeData.CAPABILITY_NET_ID), 3, 5);
        assertEquals(10, config.protocolVersion);
        assertEquals(HandshakeData.CAPABILITY_NET_ID, config.capabilities.getBitmask());
        assertEquals(5, config.maxTxWindowSize);
        assertEquals(5, config.maxTxWindowSize);

        config = new ProtocolConfig(10, createCapabilities(0), 100, 5);
        assertEquals(PacketEncoder.MAX_RX_WINDOW_SIZE, config.maxRxWindowSize);
        assertEquals(5, config.maxTxWindowSize);

        config = new ProtocolConfig(10, createCapabilities(0), 1, 5);
        assertEquals(2, config.maxRxWindowSize);
        assertEquals(5, config.maxTxWindowSize);

        config = new ProtocolConfig(10, createCapabilities(0), 3, 100);
        assertEquals(3, config.maxRxWindowSize);
        assertEquals(PacketEncoder.MAX_TX_WINDOW_SIZE, config.maxTxWindowSize);

        config = new ProtocolConfig(10, createCapabilities(0), 3, 1);
        assertEquals(3, config.maxRxWindowSize);
        assertEquals(2, config.maxTxWindowSize);
    }
}
