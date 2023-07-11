package com.android.clockwork.bluetooth.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkAgentHelper;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.RemoteException;

import com.android.internal.util.IndentingPrintWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Test for {@link ProxyNetworkAgent} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowNetworkInfo.class, ShadowConnectivityManager.class})
public class ProxyNetworkAgentTest {

    private static final int NETWORK_SCORE = 123;
    private static final String COMPANION_NAME = "Companion Name";
    private static final String REASON = "Reason";

    @Mock IndentingPrintWriter mockIndentingPrintWriter;
    @Mock NetworkAgent mockNetworkAgent;
    @Mock NetworkCapabilities mockCapabilities;
    @Mock NetworkInfo mockNetworkInfo;

    private ProxyNetworkAgent mProxyNetworkAgent;

    private static class NonLocalProxyLinkProperties extends ProxyLinkProperties {

        NonLocalProxyLinkProperties(LinkProperties linkProperties, boolean isLeEdition) {
            super(linkProperties, isLeEdition);
        }

        @Override
        protected void addLocalRoute() {
            // Do nothing here, to avoid trying to use members unavailable to Robolectric.
        }
    }

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);

        mProxyNetworkAgent = new ProxyNetworkAgent(
                RuntimeEnvironment.application,
                new NetworkCapabilities(),
                new NonLocalProxyLinkProperties(new LinkProperties(), false));
    }

    @Test
    public void testSetUpNetworkAgent_NoAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = null;

        mProxyNetworkAgent.setUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
    }

    @Test
    public void testSetUpNetworkAgent_ExistingAgentReUse() {
        setupNetworkAgent();

        mProxyNetworkAgent.setUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(2, mProxyNetworkAgent.mNetworkAgents.size());
        assertNotEquals(mockNetworkAgent, mProxyNetworkAgent.mCurrentNetworkAgent);
    }

    @Test
    public void testSetUpNetworkAgent_ExistingAgentForceNew() {
        setupNetworkAgent();

        mProxyNetworkAgent.setUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(2, mProxyNetworkAgent.mNetworkAgents.size());
        assertNotEquals(mockNetworkAgent, mProxyNetworkAgent.mCurrentNetworkAgent);
    }

    @Test
    public void testMaybeSetUpNetworkAgent_NoAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = null;

        mProxyNetworkAgent.maybeSetUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
    }

    @Test
    public void testMaybeSetUpNetworkAgent_ExistingAgentReUse() {
        setupNetworkAgent();

        mProxyNetworkAgent.maybeSetUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
        assertEquals(mockNetworkAgent, mProxyNetworkAgent.mCurrentNetworkAgent);
    }

    @Test
    public void testMaybeSetUpNetworkAgent_ExistingAgentForceNew() {
        setupNetworkAgent();

        mProxyNetworkAgent.maybeSetUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
        assertEquals(mockNetworkAgent, mProxyNetworkAgent.mCurrentNetworkAgent);
    }

    @Test
    public void testTearDownNetworkAgent_NoAgentForceNew() {
        mProxyNetworkAgent.mCurrentNetworkAgent = null;

        mProxyNetworkAgent.setUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
        assertNotNull(mProxyNetworkAgent.mCurrentNetworkAgent);

        NetworkAgentHelper.callUnwanted(mProxyNetworkAgent.mCurrentNetworkAgent);

        assertTrue(mProxyNetworkAgent.mNetworkAgents.isEmpty());
        assertNull(mProxyNetworkAgent.mCurrentNetworkAgent);
    }

    @Test
    public void testTearDownNetworkAgent_ExistingAgentForceNew() {
        mProxyNetworkAgent.setUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
        assertNotNull(mProxyNetworkAgent.mCurrentNetworkAgent);

        NetworkAgent unwantedAgent = mProxyNetworkAgent.mCurrentNetworkAgent;

        mProxyNetworkAgent.setUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(2, mProxyNetworkAgent.mNetworkAgents.size());

        NetworkAgentHelper.callUnwanted(unwantedAgent);

        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
    }

    @Test
    public void testTearDownNetworkAgent_ExistingAgentForceNewButMissingFromHash() {
        mProxyNetworkAgent.setUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
        assertNotNull(mProxyNetworkAgent.mCurrentNetworkAgent);

        NetworkAgent unwantedAgent = mProxyNetworkAgent.mCurrentNetworkAgent;

        mProxyNetworkAgent.setUpNetworkAgent(REASON, COMPANION_NAME, null);
        assertEquals(2, mProxyNetworkAgent.mNetworkAgents.size());

        // Secretly poison the hash here
        mProxyNetworkAgent.mNetworkAgents.remove(unwantedAgent);

        NetworkAgentHelper.callUnwanted(unwantedAgent);

        assertEquals(1, mProxyNetworkAgent.mNetworkAgents.size());
    }

    @Test
    public void testSetConnected_NoAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = null;
        mProxyNetworkAgent.setConnected(REASON, COMPANION_NAME);

        verify(mockNetworkInfo, never()).setDetailedState(any(), anyString(), anyString());
        assertTrue(mProxyNetworkAgent.mNetworkAgents.isEmpty());
        verify(mockNetworkAgent, never()).markConnected();
    }

    @Test
    public void testSetConnected_ExistingAgent() {
        setupNetworkAgent();

        mProxyNetworkAgent.setConnected(REASON, COMPANION_NAME);

        verify(mockNetworkAgent).markConnected();
    }

    @Test
    public void testSendCapabilities_NoAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = null;
        mProxyNetworkAgent.sendCapabilities(mockCapabilities);
        verify(mockNetworkAgent, never()).sendNetworkCapabilities(mockCapabilities);
    }

    @Test
    public void testSendCapabilities_ExistingAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = mockNetworkAgent;
        mProxyNetworkAgent.sendCapabilities(mockCapabilities);
        verify(mockNetworkAgent).sendNetworkCapabilities(mockCapabilities);
    }

    @Test
    public void testSendNetworkScore_NoAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = null;
        mProxyNetworkAgent.setNetworkScore(NETWORK_SCORE);

        verify(mockNetworkAgent, never()).sendNetworkScore(NETWORK_SCORE);
    }

    @Test
    public void testSendNetworkScore_ExistingAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = mockNetworkAgent;
        mProxyNetworkAgent.setNetworkScore(NETWORK_SCORE);

        verify(mockNetworkAgent).sendNetworkScore(NETWORK_SCORE);
    }


    @Test
    public void testDump_NoAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = null;
        mProxyNetworkAgent.dump(mockIndentingPrintWriter);
        verify(mockIndentingPrintWriter).printPair(anyString(), anyString());
    }

    @Test
    public void testDump_ExistingAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = mockNetworkAgent;
        mProxyNetworkAgent.dump(mockIndentingPrintWriter);
        verify(mockIndentingPrintWriter, atLeast(1)).printPair(anyString(), anyInt());
    }

    private void setupNetworkAgent() {
        mProxyNetworkAgent.mCurrentNetworkAgent = mockNetworkAgent;
        mProxyNetworkAgent.mNetworkAgents.put(mockNetworkAgent, mockNetworkInfo);
    }

}
