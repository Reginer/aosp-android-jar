package com.android.clockwork.bluetooth.proxy;


import android.os.SystemClock;

import com.android.clockwork.bluetooth.WearBluetoothSettings;
import com.android.clockwork.common.LogUtil;

/**
 * Manages pinging the companion over GATT by limiting the pings based on configured interval.
 */
public class ProxyPinger {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;
    private final ProxyGattServer mProxyGattServer;
    /**
     * The last time a ping was sent or zero if no ping was sent since last interval reset
     */
    private long mLatestPingTimeMs;
    private long mMinPingIntervalMs;

    public ProxyPinger(ProxyGattServer proxyGattServer) {
        mProxyGattServer = proxyGattServer;
    }

    /**
     * Sets the minimum wait time between two pings.
     */
    public void setMinPingIntervalMs(int minPingIntervalMs) {
        mLatestPingTimeMs = 0;
        mMinPingIntervalMs = minPingIntervalMs;
    }

    /**
     * Pings the companion if sufficient time passed since the last ping.
     */
    public void pingIfNeeded() {
        long currentTimeMs = SystemClock.elapsedRealtime();
        if (mLatestPingTimeMs == 0 || currentTimeMs - mLatestPingTimeMs >= mMinPingIntervalMs) {
            ping();
        }
    }

    /**
     * Pings the companion app over GATT.
     */
    public void ping() {
        boolean sent = mProxyGattServer.sendPing();
        LogUtil.logDOrNotUser(TAG, "Ping sent: " + sent);
        // Update the last ping time even if sending the ping fails to avoid trying to ping too
        // frequently on consistently failures.
        mLatestPingTimeMs = SystemClock.elapsedRealtime();
    }
}
