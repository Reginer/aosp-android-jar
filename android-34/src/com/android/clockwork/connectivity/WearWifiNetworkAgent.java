package com.android.clockwork.connectivity;

import static android.net.ConnectivityManager.NetworkCallback;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import com.android.clockwork.common.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors Wifi network connectivity.
 *
 * <p>Tracks if there is a wifi network providing connectivity. It provides listener update whenever
 * some wifi network becomes available or when all wifi networks are lost.
 */
final class WearWifiNetworkAgent {
    static final String TAG = "WearWifiNetworkAgent";

    /** Status of wifi network connectivty. */
    public enum WifiConnectivityStatus {
        STATUS_WIFI_UNAVAILABLE,
        STATUS_WIFI_CONNECTED,
    }

    /**
     * Interface to notify about wifi connectivity changes.
     */
    public interface Listener {
        void onWifiConnectionChange(WifiConnectivityStatus status);
    }

    private final Listener mListener;
    private final List<Network> mNetworks = new ArrayList<>();
    private WifiConnectivityStatus mStatus = WifiConnectivityStatus.STATUS_WIFI_UNAVAILABLE;
    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            mNetworks.remove(network);
            mNetworks.add(network);
            mStatus = WifiConnectivityStatus.STATUS_WIFI_CONNECTED;
            mListener.onWifiConnectionChange(mStatus);
            LogUtil.logD(TAG, "wifi network available " + network);
        }

        @Override
        public void onLost(Network network) {
            if (mStatus == WifiConnectivityStatus.STATUS_WIFI_UNAVAILABLE) {
                return;
            }

            LogUtil.logD(TAG, "wifi network lost " + network);

            mNetworks.remove(network);
            if (mNetworks.isEmpty()) {
                mStatus = WifiConnectivityStatus.STATUS_WIFI_UNAVAILABLE;
                mListener.onWifiConnectionChange(mStatus);
            }
        }
    };

    WearWifiNetworkAgent(Listener listener) {
        mListener = listener;
    }

    /** Register the agent with connectivity manager. */
    public void register(Context context) {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        ConnectivityManager connectivityManager =
                context.getSystemService(ConnectivityManager.class);
        connectivityManager.registerNetworkCallback(networkRequest, mNetworkCallback);
    }
}
