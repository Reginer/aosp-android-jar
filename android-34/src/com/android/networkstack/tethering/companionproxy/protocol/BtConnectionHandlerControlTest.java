package com.android.networkstack.tethering.companionproxy.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BtConnectionHandlerControlTest extends BtConnectionHandlerTestBase {
    @Before
    public void setUp() throws Exception {
        setUpBaseTest();
    }

    @After
    public void tearDown() throws Exception {
        tearDownBaseTest();
    }

    @Test
    public void shutdown_immediateOnClient() throws Exception {
        startDefaultClientAndServer();

        shutdownClientAndWait();
    }

    @Test
    public void startHandshake_abortedOnClient() throws Exception {
        startDefaultClientAndServer();
        mClient.connection.startHandshake();

        shutdownClientAndWait();

        assertFalse(mClient.isHandshakeDone);
    }

    @Test
    public void startHandshake_success() throws Exception {
        startDefaultClientAndServer();
        mClient.connection.startHandshake();

        assertTrue(mClient.waitForHandshake());
        assertTrue(mServer.waitForHandshake());

        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.links.add(new NetworkConfig.LinkInfo());
        networkConfig.links.get(0).transports = 13;
        mServer.connection.sendNetworkConfig(networkConfig);

        assertTrue(mClient.waitForNetworkConfig());
        assertEquals(1, mClient.networkConfigList.size());
        assertEquals(1, mClient.networkConfigList.get(0).links.size());
        assertEquals(networkConfig.links.get(0).transports,
            mClient.networkConfigList.get(0).links.get(0).transports);

        shutdownClientAndWait();
    }

    @Test
    public void startHandshake_serverFailure() throws Exception {
        startDefaultClientAndServer();

        mServer.connection.setConfigForTest(new ProtocolConfig(
            1234567,
            new ProtocolConfig.Capabilities(0),
            PacketEncoder.DEFAULT_MAX_RX_WINDOW_SIZE,
            PacketEncoder.DEFAULT_MAX_RX_WINDOW_SIZE));

        mClient.connection.startHandshake();

        assertTrue(mClient.waitForClose("Reset by peer"));
        assertTrue(mServer.waitForClose("Bad protocol"));
        assertFalse(mClient.isHandshakeDone);
        assertFalse(mServer.isHandshakeDone);
    }

    @Test
    public void startHandshake_clientTimeout() throws Exception {
        startDefaultClientAndServer();

        mClient.connection.setHandshakeAckTimeoutForTest(0);
        mOsAccess.setOutboundRateLimit(mServer.innerFd, 0);

        mClient.connection.startHandshake();
        mServer.connection.sendNetworkConfig(new NetworkConfig());

        assertTrue(mClient.waitForClose("Timeout"));

        mOsAccess.clearOutboundRateLimit(mServer.innerFd);
        assertTrue(mServer.waitForClose("Reset by peer"));

        assertFalse(mClient.isHandshakeDone);
        assertFalse(mServer.isHandshakeDone);
        assertEquals(0, mClient.networkConfigList.size());
    }

    @Test
    public void startHandshake_serverNetworkTimeout() throws Exception {
        startDefaultClientAndServer();

        mServer.connection.setHandshakeAckTimeoutForTest(10);
        mOsAccess.setInboundRateLimit(mClient.innerFd, 0);

        mClient.connection.startHandshake();
        mServer.connection.sendNetworkConfig(new NetworkConfig());

        assertTrue(mServer.waitForClose("Timeout"));

        mOsAccess.clearInboundRateLimit(mClient.innerFd);
        assertTrue(mClient.waitForClose("Reset by peer"));

        assertTrue(mClient.isHandshakeDone);
        assertFalse(mServer.isHandshakeDone);
        assertEquals(0, mClient.networkConfigList.size());
    }

    @Test
    public void startHandshake_serverAcceptTimeout() throws Exception {
        startDefaultClientAndServer();

        mServer.shouldAcceptHandshake = false;
        mServer.connection.setHandshakeAckTimeoutForTest(10);
        mClient.connection.startHandshake();
        mServer.connection.sendNetworkConfig(new NetworkConfig());

        assertTrue(mServer.waitForClose("Timeout"));
        assertTrue(mClient.waitForClose("Reset by peer"));

        assertFalse(mClient.isHandshakeDone);
        assertFalse(mServer.isHandshakeDone);
        assertEquals(0, mClient.networkConfigList.size());
    }
}
