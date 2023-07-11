package com.android.clockwork.connectivity;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WearProxyNetworkAgentTest {

    @Mock ConnectivityManager mockConnectivityMgr;
    @Mock Network mockProxyNetwork;
    @Mock NetworkInfo mockProxyNetworkInfo;
    @Mock WearProxyNetworkAgent.Listener mockListener;

    WearProxyNetworkAgent mAgent;
    ConnectivityManager.NetworkCallback mNetworkCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockConnectivityMgr.getAllNetworks()).thenReturn(new Network[] {mockProxyNetwork});
        when(mockConnectivityMgr.getNetworkInfo(mockProxyNetwork)).thenReturn(mockProxyNetworkInfo);

        // default to proxy connected; individual tests may override this
        when(mockProxyNetworkInfo.isConnected()).thenReturn(true);
        when(mockProxyNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_PROXY);
        mAgent = new WearProxyNetworkAgent(mockConnectivityMgr);
        mAgent.addListener(mockListener);

        ArgumentCaptor<ConnectivityManager.NetworkCallback> networkCallbackCaptor =
                ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(mockConnectivityMgr).requestNetwork(
                any(NetworkRequest.class), networkCallbackCaptor.capture());
        mNetworkCallback = networkCallbackCaptor.getValue();
    }

    @Test
    public void testNotifyProxyChanges() {
        // the default setup starts off as connected
        Assert.assertTrue(mAgent.isProxyConnected());

        mNetworkCallback.onLost(mockProxyNetwork);
        verify(mockListener).onProxyConnectionChange(false);
        Assert.assertFalse(mAgent.isProxyConnected());

        reset(mockListener);
        mNetworkCallback.onAvailable(mockProxyNetwork);
        verify(mockListener).onProxyConnectionChange(true);
        Assert.assertTrue(mAgent.isProxyConnected());
    }

    @Test
    public void testConstructorWithDifferentTransportConnected() {
        when(mockProxyNetworkInfo.isConnected()).thenReturn(true);
        when(mockProxyNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
        WearProxyNetworkAgent agent = new WearProxyNetworkAgent(mockConnectivityMgr);
        Assert.assertFalse(agent.isProxyConnected());
    }

    @Test
    public void testConstructorWithProxyDisconnected() {
        when(mockProxyNetworkInfo.isConnected()).thenReturn(false);
        when(mockProxyNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_PROXY);
        WearProxyNetworkAgent agent = new WearProxyNetworkAgent(mockConnectivityMgr);
        Assert.assertFalse(agent.isProxyConnected());
    }

    @Test
    public void testConstructorWithNoNetworks() {
        when(mockConnectivityMgr.getAllNetworks()).thenReturn(new Network[0]);
        when(mockConnectivityMgr.getNetworkInfo(mockProxyNetwork)).thenReturn(null);
        WearProxyNetworkAgent agent = new WearProxyNetworkAgent(mockConnectivityMgr);
        Assert.assertFalse(agent.isProxyConnected());
    }
}
