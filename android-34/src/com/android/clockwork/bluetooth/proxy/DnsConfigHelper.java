package com.android.clockwork.bluetooth.proxy;

import android.util.Log;

import com.google.android.collect.Lists;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Applies common changes to Phone-provided DNS server list.
 */
public class DnsConfigHelper {
    private static final String TAG = WearProxyConstants.LOG_TAG;

    private static final String GOOGLE_DNS1 = "8.8.8.8";
    private static final String GOOGLE_DNS2 = "8.8.4.4";
    // TODO(qyma): This is a temporary solution for resolving DNS problems. We should fix this
    // by resolving DNS on the phone side, see b/35445861.
    private static final String LE_PRIMARY_DNS = "202.106.0.20";

    private DnsConfigHelper() {}

    /**
     * Updates DnsServers used by Proxy to the given list of addresses.
     *
     * See b/189967388 and b/200723737: for now, prefer the first hardcoded DNS server over the
     * synced servers to guarantee DNS-over-TLS support.
     */
    public static List<InetAddress> computeDnsServerList(
            boolean isLocalEdition, List<InetAddress> syncedDnsServers) {
        List<InetAddress> result = Lists.newArrayList();

        List<InetAddress> hardcodedDnsServers = getHardcodedDnsServers(isLocalEdition);
        if (syncedDnsServers.isEmpty()) {
            // Legacy/fallback behavior, using purely hardcoded servers.
            result.addAll(hardcodedDnsServers);
        } else {
            // b/189967388 requires that the first hardcoded DNS server is preferred over the
            // synced DNS servers.  Once this is fixed with a new proxy that properly handles UDP
            // resolution, the hardcoded DNS servers should either be removed or only used as
            // backup / tertiary servers whenever synced DNS servers are available.
            if (!hardcodedDnsServers.isEmpty()) {
                result.add(hardcodedDnsServers.get(0));
            }
            result.addAll(syncedDnsServers);
        }

        // This should never happen, but make sure it's handled anyway.
        if (result.isEmpty()) {
            Log.e(TAG, "No valid DNS servers available!");
        }

        return result;
    }

    /**
     * Hardcoded set of DNS servers used to ensure that the proxy will always have a DNS server
     * to use even if there isn't one set from the phone.
     */
    private static List<InetAddress> getHardcodedDnsServers(boolean isLocalEdition) {
        String[] dnsConfig =
                isLocalEdition ? new String[] {LE_PRIMARY_DNS, GOOGLE_DNS1, GOOGLE_DNS2}
                        : new String[] {GOOGLE_DNS1, GOOGLE_DNS2};
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
}
