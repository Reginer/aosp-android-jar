package com.android.clockwork.bluetooth.proxy;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Messenger;
import org.robolectric.annotation.Implements;

/**
 * Shadow of {@link ConnectivityManager}.
 *
 * Extended to stub out capability to register network agents
 */
@Implements(ConnectivityManager.class)
public class ShadowConnectivityManager extends org.robolectric.shadows.ShadowConnectivityManager {
    private static int ID;
    public int registerNetworkAgent(Messenger messenger, NetworkInfo ni, LinkProperties lp,
            NetworkCapabilities nc, int score, NetworkAgentConfig config) {
        return ++ID;
    }
}
