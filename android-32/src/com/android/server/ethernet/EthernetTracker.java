/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.ethernet;

import static android.net.TestNetworkManager.TEST_TAP_PREFIX;

import android.annotation.Nullable;
import android.content.Context;
import android.net.IEthernetServiceListener;
import android.net.INetd;
import android.net.ITetheredInterfaceCallback;
import android.net.InterfaceConfiguration;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkCapabilities;
import android.net.NetworkStack;
import android.net.StaticIpConfiguration;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.net.util.NetdService;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.net.module.util.NetdUtils;
import com.android.server.net.BaseNetworkObserver;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks Ethernet interfaces and manages interface configurations.
 *
 * <p>Interfaces may have different {@link android.net.NetworkCapabilities}. This mapping is defined
 * in {@code config_ethernet_interfaces}. Notably, some interfaces could be marked as restricted by
 * not specifying {@link android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED} flag.
 * Interfaces could have associated {@link android.net.IpConfiguration}.
 * Ethernet Interfaces may be present at boot time or appear after boot (e.g., for Ethernet adapters
 * connected over USB). This class supports multiple interfaces. When an interface appears on the
 * system (or is present at boot time) this class will start tracking it and bring it up. Only
 * interfaces whose names match the {@code config_ethernet_iface_regex} regular expression are
 * tracked.
 *
 * <p>All public or package private methods must be thread-safe unless stated otherwise.
 */
final class EthernetTracker {
    private static final int INTERFACE_MODE_CLIENT = 1;
    private static final int INTERFACE_MODE_SERVER = 2;

    private final static String TAG = EthernetTracker.class.getSimpleName();
    private final static boolean DBG = EthernetNetworkFactory.DBG;

    private static final String TEST_IFACE_REGEXP = TEST_TAP_PREFIX + "\\d+";

    /**
     * Interface names we track. This is a product-dependent regular expression, plus,
     * if setIncludeTestInterfaces is true, any test interfaces.
     */
    private String mIfaceMatch;
    private boolean mIncludeTestInterfaces = false;

    /** Mapping between {iface name | mac address} -> {NetworkCapabilities} */
    private final ConcurrentHashMap<String, NetworkCapabilities> mNetworkCapabilities =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IpConfiguration> mIpConfigurations =
            new ConcurrentHashMap<>();

    private final Context mContext;
    private final INetworkManagementService mNMService;
    private final INetd mNetd;
    private final Handler mHandler;
    private final EthernetNetworkFactory mFactory;
    private final EthernetConfigStore mConfigStore;

    private final RemoteCallbackList<IEthernetServiceListener> mListeners =
            new RemoteCallbackList<>();
    private final TetheredInterfaceRequestList mTetheredInterfaceRequests =
            new TetheredInterfaceRequestList();

    // Used only on the handler thread
    private String mDefaultInterface;
    private int mDefaultInterfaceMode = INTERFACE_MODE_CLIENT;
    // Tracks whether clients were notified that the tethered interface is available
    private boolean mTetheredInterfaceWasAvailable = false;
    private volatile IpConfiguration mIpConfigForDefaultInterface;

    private class TetheredInterfaceRequestList extends RemoteCallbackList<ITetheredInterfaceCallback> {
        @Override
        public void onCallbackDied(ITetheredInterfaceCallback cb, Object cookie) {
            mHandler.post(EthernetTracker.this::maybeUntetherDefaultInterface);
        }
    }

    EthernetTracker(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;

        // The services we use.
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNMService = INetworkManagementService.Stub.asInterface(b);
        mNetd = Objects.requireNonNull(NetdService.getInstance(), "could not get netd instance");

        // Interface match regex.
        updateIfaceMatchRegexp();

        // Read default Ethernet interface configuration from resources
        final String[] interfaceConfigs = context.getResources().getStringArray(
                com.android.internal.R.array.config_ethernet_interfaces);
        for (String strConfig : interfaceConfigs) {
            parseEthernetConfig(strConfig);
        }

        mConfigStore = new EthernetConfigStore();

        NetworkCapabilities nc = createNetworkCapabilities(true /* clear default capabilities */);
        mFactory = new EthernetNetworkFactory(handler, context, nc);
        mFactory.register();
    }

    void start() {
        mConfigStore.read();

        // Default interface is just the first one we want to track.
        mIpConfigForDefaultInterface = mConfigStore.getIpConfigurationForDefaultInterface();
        final ArrayMap<String, IpConfiguration> configs = mConfigStore.getIpConfigurations();
        for (int i = 0; i < configs.size(); i++) {
            mIpConfigurations.put(configs.keyAt(i), configs.valueAt(i));
        }

        try {
            mNMService.registerObserver(new InterfaceObserver());
        } catch (RemoteException e) {
            Log.e(TAG, "Could not register InterfaceObserver " + e);
        }

        mHandler.post(this::trackAvailableInterfaces);
    }

    void updateIpConfiguration(String iface, IpConfiguration ipConfiguration) {
        if (DBG) {
            Log.i(TAG, "updateIpConfiguration, iface: " + iface + ", cfg: " + ipConfiguration);
        }

        mConfigStore.write(iface, ipConfiguration);
        mIpConfigurations.put(iface, ipConfiguration);

        mHandler.post(() -> mFactory.updateIpConfiguration(iface, ipConfiguration));
    }

    IpConfiguration getIpConfiguration(String iface) {
        return mIpConfigurations.get(iface);
    }

    boolean isTrackingInterface(String iface) {
        return mFactory.hasInterface(iface);
    }

    String[] getInterfaces(boolean includeRestricted) {
        return mFactory.getAvailableInterfaces(includeRestricted);
    }

    /**
     * Returns true if given interface was configured as restricted (doesn't have
     * NET_CAPABILITY_NOT_RESTRICTED) capability. Otherwise, returns false.
     */
    boolean isRestrictedInterface(String iface) {
        final NetworkCapabilities nc = mNetworkCapabilities.get(iface);
        return nc != null && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
    }

    void addListener(IEthernetServiceListener listener, boolean canUseRestrictedNetworks) {
        mListeners.register(listener, new ListenerInfo(canUseRestrictedNetworks));
    }

    void removeListener(IEthernetServiceListener listener) {
        mListeners.unregister(listener);
    }

    public void setIncludeTestInterfaces(boolean include) {
        mHandler.post(() -> {
            mIncludeTestInterfaces = include;
            updateIfaceMatchRegexp();
            mHandler.post(() -> trackAvailableInterfaces());
        });
    }

    public void requestTetheredInterface(ITetheredInterfaceCallback callback) {
        mHandler.post(() -> {
            if (!mTetheredInterfaceRequests.register(callback)) {
                // Remote process has already died
                return;
            }
            if (mDefaultInterfaceMode == INTERFACE_MODE_SERVER) {
                if (mTetheredInterfaceWasAvailable) {
                    notifyTetheredInterfaceAvailable(callback, mDefaultInterface);
                }
                return;
            }

            setDefaultInterfaceMode(INTERFACE_MODE_SERVER);
        });
    }

    public void releaseTetheredInterface(ITetheredInterfaceCallback callback) {
        mHandler.post(() -> {
            mTetheredInterfaceRequests.unregister(callback);
            maybeUntetherDefaultInterface();
        });
    }

    private void notifyTetheredInterfaceAvailable(ITetheredInterfaceCallback cb, String iface) {
        try {
            cb.onAvailable(iface);
        } catch (RemoteException e) {
            Log.e(TAG, "Error sending tethered interface available callback", e);
        }
    }

    private void notifyTetheredInterfaceUnavailable(ITetheredInterfaceCallback cb) {
        try {
            cb.onUnavailable();
        } catch (RemoteException e) {
            Log.e(TAG, "Error sending tethered interface available callback", e);
        }
    }

    private void maybeUntetherDefaultInterface() {
        if (mTetheredInterfaceRequests.getRegisteredCallbackCount() > 0) return;
        if (mDefaultInterfaceMode == INTERFACE_MODE_CLIENT) return;
        setDefaultInterfaceMode(INTERFACE_MODE_CLIENT);
    }

    private void setDefaultInterfaceMode(int mode) {
        Log.d(TAG, "Setting default interface mode to " + mode);
        mDefaultInterfaceMode = mode;
        if (mDefaultInterface != null) {
            removeInterface(mDefaultInterface);
            addInterface(mDefaultInterface);
        }
    }

    private int getInterfaceMode(final String iface) {
        if (iface.equals(mDefaultInterface)) {
            return mDefaultInterfaceMode;
        }
        return INTERFACE_MODE_CLIENT;
    }

    private void removeInterface(String iface) {
        mFactory.removeInterface(iface);
        maybeUpdateServerModeInterfaceState(iface, false);
    }

    private void stopTrackingInterface(String iface) {
        removeInterface(iface);
        if (iface.equals(mDefaultInterface)) {
            mDefaultInterface = null;
        }
    }

    private void addInterface(String iface) {
        InterfaceConfiguration config = null;
        // Bring up the interface so we get link status indications.
        try {
            NetworkStack.checkNetworkStackPermission(mContext);
            NetdUtils.setInterfaceUp(mNetd, iface);
            config = mNMService.getInterfaceConfig(iface);
        } catch (RemoteException | IllegalStateException e) {
            // Either the system is crashing or the interface has disappeared. Just ignore the
            // error; we haven't modified any state because we only do that if our calls succeed.
            Log.e(TAG, "Error upping interface " + iface, e);
        }

        if (config == null) {
            Log.e(TAG, "Null interface config for " + iface + ". Bailing out.");
            return;
        }

        final String hwAddress = config.getHardwareAddress();

        NetworkCapabilities nc = mNetworkCapabilities.get(iface);
        if (nc == null) {
            // Try to resolve using mac address
            nc = mNetworkCapabilities.get(hwAddress);
            if (nc == null) {
                final boolean isTestIface = iface.matches(TEST_IFACE_REGEXP);
                nc = createDefaultNetworkCapabilities(isTestIface);
            }
        }

        final int mode = getInterfaceMode(iface);
        if (mode == INTERFACE_MODE_CLIENT) {
            IpConfiguration ipConfiguration = mIpConfigurations.get(iface);
            if (ipConfiguration == null) {
                ipConfiguration = createDefaultIpConfiguration();
            }

            Log.d(TAG, "Tracking interface in client mode: " + iface);
            mFactory.addInterface(iface, hwAddress, nc, ipConfiguration);
        } else {
            maybeUpdateServerModeInterfaceState(iface, true);
        }

        // Note: if the interface already has link (e.g., if we crashed and got
        // restarted while it was running), we need to fake a link up notification so we
        // start configuring it.
        if (config.hasFlag("running")) {
            updateInterfaceState(iface, true);
        }
    }

    private void updateInterfaceState(String iface, boolean up) {
        final int mode = getInterfaceMode(iface);
        final boolean factoryLinkStateUpdated = (mode == INTERFACE_MODE_CLIENT)
                && mFactory.updateInterfaceLinkState(iface, up);

        if (factoryLinkStateUpdated) {
            boolean restricted = isRestrictedInterface(iface);
            int n = mListeners.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    if (restricted) {
                        ListenerInfo listenerInfo = (ListenerInfo) mListeners.getBroadcastCookie(i);
                        if (!listenerInfo.canUseRestrictedNetworks) {
                            continue;
                        }
                    }
                    mListeners.getBroadcastItem(i).onAvailabilityChanged(iface, up);
                } catch (RemoteException e) {
                    // Do nothing here.
                }
            }
            mListeners.finishBroadcast();
        }
    }

    private void maybeUpdateServerModeInterfaceState(String iface, boolean available) {
        if (available == mTetheredInterfaceWasAvailable || !iface.equals(mDefaultInterface)) return;

        Log.d(TAG, (available ? "Tracking" : "No longer tracking")
                + " interface in server mode: " + iface);

        final int pendingCbs = mTetheredInterfaceRequests.beginBroadcast();
        for (int i = 0; i < pendingCbs; i++) {
            ITetheredInterfaceCallback item = mTetheredInterfaceRequests.getBroadcastItem(i);
            if (available) {
                notifyTetheredInterfaceAvailable(item, iface);
            } else {
                notifyTetheredInterfaceUnavailable(item);
            }
        }
        mTetheredInterfaceRequests.finishBroadcast();
        mTetheredInterfaceWasAvailable = available;
    }

    private void maybeTrackInterface(String iface) {
        if (!iface.matches(mIfaceMatch)) {
            return;
        }

        // If we don't already track this interface, and if this interface matches
        // our regex, start tracking it.
        if (mFactory.hasInterface(iface) || iface.equals(mDefaultInterface)) {
            if (DBG) Log.w(TAG, "Ignoring already-tracked interface " + iface);
            return;
        }
        if (DBG) Log.i(TAG, "maybeTrackInterface: " + iface);

        // TODO: avoid making an interface default if it has configured NetworkCapabilities.
        if (mDefaultInterface == null) {
            mDefaultInterface = iface;
        }

        if (mIpConfigForDefaultInterface != null) {
            updateIpConfiguration(iface, mIpConfigForDefaultInterface);
            mIpConfigForDefaultInterface = null;
        }

        addInterface(iface);
    }

    private void trackAvailableInterfaces() {
        try {
            final String[] ifaces = mNMService.listInterfaces();
            for (String iface : ifaces) {
                maybeTrackInterface(iface);
            }
        } catch (RemoteException | IllegalStateException e) {
            Log.e(TAG, "Could not get list of interfaces " + e);
        }
    }


    private class InterfaceObserver extends BaseNetworkObserver {

        @Override
        public void interfaceLinkStateChanged(String iface, boolean up) {
            if (DBG) {
                Log.i(TAG, "interfaceLinkStateChanged, iface: " + iface + ", up: " + up);
            }
            mHandler.post(() -> updateInterfaceState(iface, up));
        }

        @Override
        public void interfaceAdded(String iface) {
            mHandler.post(() -> maybeTrackInterface(iface));
        }

        @Override
        public void interfaceRemoved(String iface) {
            mHandler.post(() -> stopTrackingInterface(iface));
        }
    }

    private static class ListenerInfo {

        boolean canUseRestrictedNetworks = false;

        ListenerInfo(boolean canUseRestrictedNetworks) {
            this.canUseRestrictedNetworks = canUseRestrictedNetworks;
        }
    }

    /**
     * Parses an Ethernet interface configuration
     *
     * @param configString represents an Ethernet configuration in the following format: {@code
     * <interface name|mac address>;[Network Capabilities];[IP config];[Override Transport]}
     */
    private void parseEthernetConfig(String configString) {
        String[] tokens = configString.split(";", /* limit of tokens */ 4);
        String name = tokens[0];
        String capabilities = tokens.length > 1 ? tokens[1] : null;
        String transport = tokens.length > 3 ? tokens[3] : null;
        NetworkCapabilities nc = createNetworkCapabilities(
                !TextUtils.isEmpty(capabilities)  /* clear default capabilities */, capabilities,
                transport).build();
        mNetworkCapabilities.put(name, nc);

        if (tokens.length > 2 && !TextUtils.isEmpty(tokens[2])) {
            IpConfiguration ipConfig = parseStaticIpConfiguration(tokens[2]);
            mIpConfigurations.put(name, ipConfig);
        }
    }

    private static NetworkCapabilities createDefaultNetworkCapabilities(boolean isTestIface) {
        NetworkCapabilities.Builder builder = createNetworkCapabilities(
                false /* clear default capabilities */, null, null)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED);

        if (isTestIface) {
                builder.addTransportType(NetworkCapabilities.TRANSPORT_TEST);
        } else {
                builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }

        return builder.build();
    }

    private static NetworkCapabilities createNetworkCapabilities(boolean clearDefaultCapabilities) {
        return createNetworkCapabilities(clearDefaultCapabilities, null, null).build();
    }

    /**
     * Parses a static list of network capabilities
     *
     * @param clearDefaultCapabilities Indicates whether or not to clear any default capabilities
     * @param commaSeparatedCapabilities A comma separated string list of integer encoded
     *                                   NetworkCapability.NET_CAPABILITY_* values
     * @param overrideTransport A string representing a single integer encoded override transport
     *                          type. Must be one of the NetworkCapability.TRANSPORT_*
     *                          values. TRANSPORT_VPN is not supported. Errors with input
     *                          will cause the override to be ignored.
     */
    @VisibleForTesting
    static NetworkCapabilities.Builder createNetworkCapabilities(
            boolean clearDefaultCapabilities, @Nullable String commaSeparatedCapabilities,
            @Nullable String overrideTransport) {

        final NetworkCapabilities.Builder builder = clearDefaultCapabilities
                ? NetworkCapabilities.Builder.withoutDefaultCapabilities()
                : new NetworkCapabilities.Builder();

        // Determine the transport type. If someone has tried to define an override transport then
        // attempt to add it. Since we can only have one override, all errors with it will
        // gracefully default back to TRANSPORT_ETHERNET and warn the user. VPN is not allowed as an
        // override type. Wifi Aware and LoWPAN are currently unsupported as well.
        int transport = NetworkCapabilities.TRANSPORT_ETHERNET;
        if (!TextUtils.isEmpty(overrideTransport)) {
            try {
                int parsedTransport = Integer.valueOf(overrideTransport);
                if (parsedTransport == NetworkCapabilities.TRANSPORT_VPN
                        || parsedTransport == NetworkCapabilities.TRANSPORT_WIFI_AWARE
                        || parsedTransport == NetworkCapabilities.TRANSPORT_LOWPAN) {
                    Log.e(TAG, "Override transport '" + parsedTransport + "' is not supported. "
                            + "Defaulting to TRANSPORT_ETHERNET");
                } else {
                    transport = parsedTransport;
                }
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Override transport type '" + overrideTransport + "' "
                        + "could not be parsed. Defaulting to TRANSPORT_ETHERNET");
            }
        }

        // Apply the transport. If the user supplied a valid number that is not a valid transport
        // then adding will throw an exception. Default back to TRANSPORT_ETHERNET if that happens
        try {
            builder.addTransportType(transport);
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, transport + " is not a valid NetworkCapability.TRANSPORT_* value. "
                    + "Defaulting to TRANSPORT_ETHERNET");
            builder.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        }

        builder.setLinkUpstreamBandwidthKbps(100 * 1000);
        builder.setLinkDownstreamBandwidthKbps(100 * 1000);

        if (!TextUtils.isEmpty(commaSeparatedCapabilities)) {
            for (String strNetworkCapability : commaSeparatedCapabilities.split(",")) {
                if (!TextUtils.isEmpty(strNetworkCapability)) {
                    try {
                        builder.addCapability(Integer.valueOf(strNetworkCapability));
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "Capability '" + strNetworkCapability + "' could not be parsed");
                    } catch (IllegalArgumentException iae) {
                        Log.e(TAG, strNetworkCapability + " is not a valid "
                                + "NetworkCapability.NET_CAPABILITY_* value");
                    }
                }
            }
        }
        // Ethernet networks have no way to update the following capabilities, so they always
        // have them.
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);

        return builder;
    }

    /**
     * Parses static IP configuration.
     *
     * @param staticIpConfig represents static IP configuration in the following format: {@code
     * ip=<ip-address/mask> gateway=<ip-address> dns=<comma-sep-ip-addresses>
     *     domains=<comma-sep-domains>}
     */
    @VisibleForTesting
    static IpConfiguration parseStaticIpConfiguration(String staticIpConfig) {
        final StaticIpConfiguration.Builder staticIpConfigBuilder =
                new StaticIpConfiguration.Builder();

        for (String keyValueAsString : staticIpConfig.trim().split(" ")) {
            if (TextUtils.isEmpty(keyValueAsString)) continue;

            String[] pair = keyValueAsString.split("=");
            if (pair.length != 2) {
                throw new IllegalArgumentException("Unexpected token: " + keyValueAsString
                        + " in " + staticIpConfig);
            }

            String key = pair[0];
            String value = pair[1];

            switch (key) {
                case "ip":
                    staticIpConfigBuilder.setIpAddress(new LinkAddress(value));
                    break;
                case "domains":
                    staticIpConfigBuilder.setDomains(value);
                    break;
                case "gateway":
                    staticIpConfigBuilder.setGateway(InetAddress.parseNumericAddress(value));
                    break;
                case "dns": {
                    ArrayList<InetAddress> dnsAddresses = new ArrayList<>();
                    for (String address: value.split(",")) {
                        dnsAddresses.add(InetAddress.parseNumericAddress(address));
                    }
                    staticIpConfigBuilder.setDnsServers(dnsAddresses);
                    break;
                }
                default : {
                    throw new IllegalArgumentException("Unexpected key: " + key
                            + " in " + staticIpConfig);
                }
            }
        }
        final IpConfiguration ret = new IpConfiguration();
        ret.setIpAssignment(IpAssignment.STATIC);
        ret.setProxySettings(ProxySettings.NONE);
        ret.setStaticIpConfiguration(staticIpConfigBuilder.build());
        return ret;
    }

    private static IpConfiguration createDefaultIpConfiguration() {
        final IpConfiguration ret = new IpConfiguration();
        ret.setIpAssignment(IpAssignment.DHCP);
        ret.setProxySettings(ProxySettings.NONE);
        return ret;
    }

    private void updateIfaceMatchRegexp() {
        final String match = mContext.getResources().getString(
                com.android.internal.R.string.config_ethernet_iface_regex);
        mIfaceMatch = mIncludeTestInterfaces
                ? "(" + match + "|" + TEST_IFACE_REGEXP + ")"
                : match;
        Log.d(TAG, "Interface match regexp set to '" + mIfaceMatch + "'");
    }

    private void postAndWaitForRunnable(Runnable r) {
        mHandler.runWithScissors(r, 2000L /* timeout */);
    }

    void dump(FileDescriptor fd, IndentingPrintWriter pw, String[] args) {
        postAndWaitForRunnable(() -> {
            pw.println(getClass().getSimpleName());
            pw.println("Ethernet interface name filter: " + mIfaceMatch);
            pw.println("Default interface: " + mDefaultInterface);
            pw.println("Default interface mode: " + mDefaultInterfaceMode);
            pw.println("Tethered interface requests: "
                    + mTetheredInterfaceRequests.getRegisteredCallbackCount());
            pw.println("Listeners: " + mListeners.getRegisteredCallbackCount());
            pw.println("IP Configurations:");
            pw.increaseIndent();
            for (String iface : mIpConfigurations.keySet()) {
                pw.println(iface + ": " + mIpConfigurations.get(iface));
            }
            pw.decreaseIndent();
            pw.println();

            pw.println("Network Capabilities:");
            pw.increaseIndent();
            for (String iface : mNetworkCapabilities.keySet()) {
                pw.println(iface + ": " + mNetworkCapabilities.get(iface));
            }
            pw.decreaseIndent();
            pw.println();

            mFactory.dump(fd, pw, args);
        });
    }
}
