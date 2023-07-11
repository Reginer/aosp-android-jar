package com.android.clockwork.bluetooth.proxy;

import android.annotation.AnyThread;
import android.annotation.MainThread;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.android.clockwork.common.DebugAssert;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.net.InetAddress;
import java.util.List;

/**
 * Helper class that interfaces with {@link ConnectivityService} and companion proxy
 * shard network.
 *
 * Manages the network factory and various network agents that communicate with
 * {@link ConnectivityService} to provide network connectivity for users.
 *
 */
public class ProxyServiceHelper {
    private static final String TAG = WearProxyConstants.LOG_TAG;
    public static final int CAPABILITIES_UPSTREAM_BANDWIDTH_KBPS = 1600;
    public static final int CAPABILITIES_DOWNSTREAM_BANDWIDTH_KBPS = 1600;

    @VisibleForTesting final NetworkCapabilities.Builder mCapabilitiesBlder;

    private final ProxyNetworkFactory mProxyNetworkFactory;
    private final ProxyNetworkAgent mProxyNetworkAgent;

    @Nullable private String mCompanionName;

    public ProxyServiceHelper(
            final Context context,
            final NetworkCapabilities.Builder capabilitiesBlder,
            final ProxyLinkProperties proxyLinkProperties) {
        DebugAssert.isMainThread();

        mCapabilitiesBlder = capabilitiesBlder;

        buildCapabilities();

        mProxyNetworkAgent =
            buildProxyNetworkAgent(context, mCapabilitiesBlder.build(), proxyLinkProperties);
        mProxyNetworkFactory = buildProxyNetworkFactory(context, mCapabilitiesBlder.build());

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Created proxy network service manager");
        }
    }

    /**
     * Starts a sysproxy network session for {@link ConnectivityService}.
     *
     * Ensure a network agent exists to advocate for the sysproxy network
     * in the larger scheme of connectivity service.
     *
     * Inform {@link ConnectivityService} that the sysproxy network
     * has connected and ready for validation.
     */
    @MainThread
    public void startNetworkSession(@Nullable final String reason,
            ProxyNetworkAgent.Listener callback) {
        DebugAssert.isMainThread();
        mProxyNetworkAgent.maybeSetUpNetworkAgent(reason, mCompanionName, callback);
        mProxyNetworkAgent.setConnected(reason, mCompanionName);
    }

    /**
     * Stops a sysproxy network session.
     *
     * Inform {@link ConnectivityService} that the sysproxy network has
     * disconnected and should not be used for network traffic.
     *
     * Invalidate the current network agent to ensure futher traffic is no longer
     * possible with this session.
     */
    @MainThread
    public void stopNetworkSession(@Nullable final String reason) {
        DebugAssert.isMainThread();
        mProxyNetworkAgent.setDisconnected(reason, mCompanionName);
        mProxyNetworkAgent.invalidateCurrentNetworkAgent();
    }

    /**
     * Sets metered or unmetered flag for current sysproxy network session.
     *
     * Metered and unmetered networks may be treated differently by
     * {@link ConnectivityService}.  By default sysproxy will be treated
     * with the initial default capabilities from {@link WearConnectivityService}.
     */
    @MainThread
    public void setMetered(final boolean isMetered) {
        DebugAssert.isMainThread();
        if (isMetered) {
            mCapabilitiesBlder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        } else {
            mCapabilitiesBlder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }
        mProxyNetworkAgent.sendCapabilities(mCapabilitiesBlder.build());
    }

    /**
     * Invoking this with a new network score may force a re-evaluation of default network.
     */
    @MainThread
    public void setNetworkScore(final int networkScore) {
        DebugAssert.isMainThread();
        mProxyNetworkFactory.setNetworkScore(networkScore);
        mProxyNetworkAgent.setNetworkScore(networkScore);
    }

    @AnyThread
    public int getNetworkScore() {
        return mProxyNetworkAgent.getNetworkScore();
    }

    /**
     * Updates DNS Servers used by proxy.
     */
    public void setDnsServers(List<InetAddress> dnsServers) {
        mProxyNetworkAgent.setDnsServers(dnsServers);
    }

    @MainThread
    public void setCompanionName(@Nullable final String companionName) {
        DebugAssert.isMainThread();
        mCompanionName = companionName;
    }

    private void buildCapabilities() {
        mCapabilitiesBlder.addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH);

        mCapabilitiesBlder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        mCapabilitiesBlder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        mCapabilitiesBlder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
        mCapabilitiesBlder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
        addNetworkCapabilitiesBandwidth();
    }

    @VisibleForTesting
    void addNetworkCapabilitiesBandwidth() {
        mCapabilitiesBlder.setLinkUpstreamBandwidthKbps(CAPABILITIES_UPSTREAM_BANDWIDTH_KBPS);
        mCapabilitiesBlder.setLinkDownstreamBandwidthKbps(CAPABILITIES_DOWNSTREAM_BANDWIDTH_KBPS);
    }

    @VisibleForTesting
    ProxyNetworkFactory buildProxyNetworkFactory(
            final Context context,
            final NetworkCapabilities capabilities) {
        return new ProxyNetworkFactory(context, capabilities);
    }

    @VisibleForTesting
    ProxyNetworkAgent buildProxyNetworkAgent(
            final Context context,
            final NetworkCapabilities capabilities,
            final ProxyLinkProperties proxyLinkProperties) {
         return new ProxyNetworkAgent(context, capabilities, proxyLinkProperties);
    }

    @AnyThread
    public void dump(@NonNull final IndentingPrintWriter ipw) {
        mProxyNetworkFactory.dump(ipw);
        mProxyNetworkAgent.dump(ipw);
    }
}
