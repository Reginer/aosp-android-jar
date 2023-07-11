package com.android.clockwork.bluetooth.proxy;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.util.Log;

import com.google.android.collect.Lists;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Companion proxy link properties configuration
 *
 * This wrapper around {@link LinkProperties} with some
 * additional wear specific configuration for the bluetooth
 * sysproxy.
 */
public class ProxyLinkProperties {
    private static final String TAG = WearProxyConstants.LOG_TAG;

    private static final int MTU = 1500;
    private static final String INTERFACE_NAME = "lo";
    private static final String LOCAL_ADDRESS = "127.0.0.1";
    private static final String GOOGLE_DNS1 = "8.8.8.8";
    private static final String GOOGLE_DNS2 = "8.8.4.4";
    // TODO(qyma): This is a temporary solution for resolving DNS problems. We should fix this
    // by resolving DNS on the phone side, see b/35445861.
    private static final String LE_PRIMARY_DNS = "202.106.0.20";

    private final LinkProperties mLinkProperties;
    private final boolean mIsLocalEdition;

    public ProxyLinkProperties(final LinkProperties linkProperties, final boolean isLocalEdition) {
        mLinkProperties = linkProperties;
        mLinkProperties.setInterfaceName(INTERFACE_NAME);
        mLinkProperties.setMtu(MTU);
        mIsLocalEdition = isLocalEdition;
        try {
            addLocalRoute();
            setDnsServers(Lists.newArrayList());
        } catch (UnknownHostException e) {
            // Rethrow a checked exception; we can't proceed if adding the local route fails
            throw new RuntimeException("Could not add proxy loopback route", e);
        }
    }

    public LinkProperties getLinkProperties() {
        return mLinkProperties;
    }

    /**
     * Updates DnsServers used by Proxy to the given list of addresses.
     *
     * See b/189967388 and b/200723737: for now, prefer the first hardcoded DNS server over the
     * synced servers to guarantee DNS-over-TLS support.
     */
    public void setDnsServers(List<InetAddress> syncedDnsServers) {
        List<InetAddress> computedDnsList = Lists.newArrayList();
        List<InetAddress> hardcodedDnsServers = getHardcodedDnsServers();

        if (syncedDnsServers.isEmpty()) {
            // Legacy/fallback behavior, using purely hardcoded servers.
            computedDnsList.addAll(hardcodedDnsServers);
        } else {
            // b/189967388 requires that the first hardcoded DNS server is preferred over the
            // synced DNS servers.  Once this is fixed with a new proxy that properly handles UDP
            // resolution, the hardcoded DNS servers should either be removed or only used as
            // backup / tertiary servers whenever synced DNS servers are available.
            if (!hardcodedDnsServers.isEmpty()) {
                computedDnsList.add(hardcodedDnsServers.get(0));
            }
            computedDnsList.addAll(syncedDnsServers);
        }

        // This should never happen, but make sure it's handled anyway.
        if (computedDnsList.isEmpty()) {
            Log.e(TAG, "No valid DNS servers available!");
            return;
        }
        mLinkProperties.setDnsServers(computedDnsList);
    }

    /**
     * Hardcoded set of DNS servers used to ensure that the proxy will always have a DNS server
     * to use even if there isn't one set from the phone.
     */
    private List<InetAddress> getHardcodedDnsServers() {
        String[] dnsConfig =
                mIsLocalEdition ? new String[]{LE_PRIMARY_DNS, GOOGLE_DNS1, GOOGLE_DNS2}
                        : new String[]{GOOGLE_DNS1, GOOGLE_DNS2};
        List<InetAddress> hardcodedServers = Lists.newArrayList();
        for (String dnsName : dnsConfig) {
            try {
                hardcodedServers.add(InetAddress.getByName(dnsName));
            } catch (UnknownHostException e) {
                Log.e(TAG, "Could not resolve hardcoded DNS server: " + dnsName, e);
            }
        }
        return hardcodedServers;
    }

    protected void addLocalRoute() throws UnknownHostException {
        final InetAddress gateway = InetAddress.getByName(LOCAL_ADDRESS);
        mLinkProperties.addRoute(new RouteInfo(
				null, gateway, INTERFACE_NAME, RouteInfo.RTN_UNICAST));
    }
}
