package com.android.clockwork.connectivity;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;

import java.util.HashSet;

/**
 * Manages the Proxy Network agent.
 *
 * This class should be removed once WearBluetoothService is hooked up to
 * WearConnectivityController/Service.
 */
public class WearProxyNetworkAgent {

    public interface Listener {
        void onProxyConnectionChange(boolean proxyConnected);
    }

    private boolean mIsProxyConnected;
    private HashSet<Listener> mListeners = new HashSet<>();

    public WearProxyNetworkAgent(ConnectivityManager connectivityManager) {
        mIsProxyConnected = fetchProxyConnectedStatus(connectivityManager);

        NetworkRequest networkRequest = new NetworkRequest.Builder().
                addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH).build();
        connectivityManager.requestNetwork(networkRequest, mPersistBtProxyCallback);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public boolean isProxyConnected() {
        return mIsProxyConnected;
    }

    private void notifyProxyConnectionChange(boolean proxyConnected) {
        boolean prevConnected = mIsProxyConnected;
        mIsProxyConnected = proxyConnected;
        if (mIsProxyConnected != prevConnected) {
            for (Listener listener : mListeners) {
                listener.onProxyConnectionChange(mIsProxyConnected);
            }
        }
    }

    /**
     * This callback fulfills two critical roles:
     * 1. Listens for BT Proxy NetworkAgent status and updates mIsProxyConnected as needed.
     *
     * 2. On iOS, ensures that the BT Proxy NetworkAgent is never torn down by ConnectivityService
     *   unless the proxy connection is physically unavailable;  without this NetworkRequest and
     *   Callback, ConnectivityService will tear down any NetworkAgent that is not the default,
     *   and this can happen to Proxy on iOS whenever WiFi is active.
     *
     *   The actual state update is delegated to a handler because when BT disconnects, we allow
     *   a certain delay for a BT reconnect to occur before notifying.
     */
    private final ConnectivityManager.NetworkCallback mPersistBtProxyCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    notifyProxyConnectionChange(true);
                }

                @Override
                public void onLost(Network network) {
                    notifyProxyConnectionChange(false);
                }
            };

    /**
     * Returns whether the device is connected to BT proxy.
     */
    private boolean fetchProxyConnectedStatus(ConnectivityManager connectivityManager) {
        for (Network network : connectivityManager.getAllNetworks()) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo != null
                    && networkInfo.getType() == ConnectivityManager.TYPE_PROXY
                    && networkInfo.isConnected()) {
                return true;
            }
        }
        return false;
    }
}
