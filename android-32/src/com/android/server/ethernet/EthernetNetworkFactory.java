/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.EthernetNetworkSpecifier;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.ip.IIpClient;
import android.net.ip.IpClientCallbacks;
import android.net.ip.IpClientUtil;
import android.net.shared.ProvisioningConfiguration;
import android.net.util.InterfaceParams;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.util.IndentingPrintWriter;

import java.io.FileDescriptor;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link NetworkFactory} that represents Ethernet networks.
 *
 * This class reports a static network score of 70 when it is tracking an interface and that
 * interface's link is up, and a score of 0 otherwise.
 */
public class EthernetNetworkFactory extends NetworkFactory {
    private final static String TAG = EthernetNetworkFactory.class.getSimpleName();
    final static boolean DBG = true;

    private final static int NETWORK_SCORE = 70;
    private static final String NETWORK_TYPE = "Ethernet";

    private final ConcurrentHashMap<String, NetworkInterfaceState> mTrackingInterfaces =
            new ConcurrentHashMap<>();
    private final Handler mHandler;
    private final Context mContext;

    public static class ConfigurationException extends AndroidRuntimeException {
        public ConfigurationException(String msg) {
            super(msg);
        }
    }

    public EthernetNetworkFactory(Handler handler, Context context, NetworkCapabilities filter) {
        super(handler.getLooper(), context, NETWORK_TYPE, filter);

        mHandler = handler;
        mContext = context;

        setScoreFilter(NETWORK_SCORE);
    }

    @Override
    public boolean acceptRequest(NetworkRequest request) {
        if (DBG) {
            Log.d(TAG, "acceptRequest, request: " + request);
        }

        return networkForRequest(request) != null;
    }

    @Override
    protected void needNetworkFor(NetworkRequest networkRequest) {
        NetworkInterfaceState network = networkForRequest(networkRequest);

        if (network == null) {
            Log.e(TAG, "needNetworkFor, failed to get a network for " + networkRequest);
            return;
        }

        if (++network.refCount == 1) {
            network.start();
        }
    }

    @Override
    protected void releaseNetworkFor(NetworkRequest networkRequest) {
        NetworkInterfaceState network = networkForRequest(networkRequest);
        if (network == null) {
            Log.e(TAG, "releaseNetworkFor, failed to get a network for " + networkRequest);
            return;
        }

        if (--network.refCount == 0) {
            network.stop();
        }
    }

    /**
     * Returns an array of available interface names. The array is sorted: unrestricted interfaces
     * goes first, then sorted by name.
     */
    String[] getAvailableInterfaces(boolean includeRestricted) {
        return mTrackingInterfaces.values()
                .stream()
                .filter(iface -> !iface.isRestricted() || includeRestricted)
                .sorted((iface1, iface2) -> {
                    int r = Boolean.compare(iface1.isRestricted(), iface2.isRestricted());
                    return r == 0 ? iface1.name.compareTo(iface2.name) : r;
                })
                .map(iface -> iface.name)
                .toArray(String[]::new);
    }

    void addInterface(String ifaceName, String hwAddress, NetworkCapabilities capabilities,
             IpConfiguration ipConfiguration) {
        if (mTrackingInterfaces.containsKey(ifaceName)) {
            Log.e(TAG, "Interface with name " + ifaceName + " already exists.");
            return;
        }

        if (DBG) {
            Log.d(TAG, "addInterface, iface: " + ifaceName + ", capabilities: " + capabilities);
        }

        NetworkInterfaceState iface = new NetworkInterfaceState(
                ifaceName, hwAddress, mHandler, mContext, capabilities, this);
        iface.setIpConfig(ipConfiguration);
        mTrackingInterfaces.put(ifaceName, iface);

        updateCapabilityFilter();
    }

    private static NetworkCapabilities mixInCapabilities(NetworkCapabilities nc,
            NetworkCapabilities addedNc) {
       final NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder(nc);
       for (int transport : addedNc.getTransportTypes()) builder.addTransportType(transport);
       for (int capability : addedNc.getCapabilities()) builder.addCapability(capability);
       return builder.build();
    }

    private void updateCapabilityFilter() {
        NetworkCapabilities capabilitiesFilter =
                NetworkCapabilities.Builder.withoutDefaultCapabilities()
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build();

        for (NetworkInterfaceState iface:  mTrackingInterfaces.values()) {
            capabilitiesFilter = mixInCapabilities(capabilitiesFilter, iface.mCapabilities);
        }

        if (DBG) Log.d(TAG, "updateCapabilityFilter: " + capabilitiesFilter);
        setCapabilityFilter(capabilitiesFilter);
    }

    void removeInterface(String interfaceName) {
        NetworkInterfaceState iface = mTrackingInterfaces.remove(interfaceName);
        if (iface != null) {
            iface.stop();
        }

        updateCapabilityFilter();
    }

    /** Returns true if state has been modified */
    boolean updateInterfaceLinkState(String ifaceName, boolean up) {
        if (!mTrackingInterfaces.containsKey(ifaceName)) {
            return false;
        }

        if (DBG) {
            Log.d(TAG, "updateInterfaceLinkState, iface: " + ifaceName + ", up: " + up);
        }

        NetworkInterfaceState iface = mTrackingInterfaces.get(ifaceName);
        return iface.updateLinkState(up);
    }

    boolean hasInterface(String interfacName) {
        return mTrackingInterfaces.containsKey(interfacName);
    }

    void updateIpConfiguration(String iface, IpConfiguration ipConfiguration) {
        NetworkInterfaceState network = mTrackingInterfaces.get(iface);
        if (network != null) {
            network.setIpConfig(ipConfiguration);
        }
    }

    private NetworkInterfaceState networkForRequest(NetworkRequest request) {
        String requestedIface = null;

        NetworkSpecifier specifier = request.getNetworkSpecifier();
        if (specifier instanceof EthernetNetworkSpecifier) {
            requestedIface = ((EthernetNetworkSpecifier) specifier)
                .getInterfaceName();
        }

        NetworkInterfaceState network = null;
        if (!TextUtils.isEmpty(requestedIface)) {
            NetworkInterfaceState n = mTrackingInterfaces.get(requestedIface);
            if (n != null && request.canBeSatisfiedBy(n.mCapabilities)) {
                network = n;
            }
        } else {
            for (NetworkInterfaceState n : mTrackingInterfaces.values()) {
                if (request.canBeSatisfiedBy(n.mCapabilities) && n.mLinkUp) {
                    network = n;
                    break;
                }
            }
        }

        if (DBG) {
            Log.i(TAG, "networkForRequest, request: " + request + ", network: " + network);
        }

        return network;
    }

    private static class NetworkInterfaceState {
        final String name;

        private final String mHwAddress;
        private final NetworkCapabilities mCapabilities;
        private final Handler mHandler;
        private final Context mContext;
        private final NetworkFactory mNetworkFactory;
        private final int mLegacyType;

        private static String sTcpBufferSizes = null;  // Lazy initialized.

        private boolean mLinkUp;
        private LinkProperties mLinkProperties = new LinkProperties();

        private volatile @Nullable IIpClient mIpClient;
        private @Nullable IpClientCallbacksImpl mIpClientCallback;
        private @Nullable NetworkAgent mNetworkAgent;
        private @Nullable IpConfiguration mIpConfig;

        /**
         * An object to contain all transport type information, including base network score and
         * the legacy transport type it maps to (if any)
         */
        private static class TransportInfo {
            final int mLegacyType;
            final int mScore;

            private TransportInfo(int legacyType, int score) {
                mLegacyType = legacyType;
                mScore = score;
            }
        }

        /**
         * A map of TRANSPORT_* types to TransportInfo, making scoring and legacy type information
         * available for each type an ethernet interface could propagate.
         *
         * Unfortunately, base scores for the various transports are not yet centrally located.
         * They've been lifted from the corresponding NetworkFactory files in the meantime.
         *
         * Additionally, there are no legacy type equivalents to LOWPAN or WIFI_AWARE. These types
         * are set to TYPE_NONE to match the behavior of their own network factories.
         */
        private static final SparseArray<TransportInfo> sTransports = new SparseArray();
        static {
            // LowpanInterfaceTracker.NETWORK_SCORE
            sTransports.put(NetworkCapabilities.TRANSPORT_LOWPAN,
                    new TransportInfo(ConnectivityManager.TYPE_NONE, 30));
            // WifiAwareDataPathStateManager.NETWORK_FACTORY_SCORE_AVAIL
            sTransports.put(NetworkCapabilities.TRANSPORT_WIFI_AWARE,
                    new TransportInfo(ConnectivityManager.TYPE_NONE, 1));
            // EthernetNetworkFactory.NETWORK_SCORE
            sTransports.put(NetworkCapabilities.TRANSPORT_ETHERNET,
                    new TransportInfo(ConnectivityManager.TYPE_ETHERNET, 70));
            // BluetoothTetheringNetworkFactory.NETWORK_SCORE
            sTransports.put(NetworkCapabilities.TRANSPORT_BLUETOOTH,
                    new TransportInfo(ConnectivityManager.TYPE_BLUETOOTH, 69));
            // WifiNetworkFactory.SCORE_FILTER / NetworkAgent.WIFI_BASE_SCORE
            sTransports.put(NetworkCapabilities.TRANSPORT_WIFI,
                    new TransportInfo(ConnectivityManager.TYPE_WIFI, 60));
            // TelephonyNetworkFactory.TELEPHONY_NETWORK_SCORE
            sTransports.put(NetworkCapabilities.TRANSPORT_CELLULAR,
                    new TransportInfo(ConnectivityManager.TYPE_MOBILE, 50));
        }

        long refCount = 0;

        private class IpClientCallbacksImpl extends IpClientCallbacks {
            private final ConditionVariable mIpClientStartCv = new ConditionVariable(false);
            private final ConditionVariable mIpClientShutdownCv = new ConditionVariable(false);

            @Override
            public void onIpClientCreated(IIpClient ipClient) {
                mIpClient = ipClient;
                mIpClientStartCv.open();
            }

            private void awaitIpClientStart() {
                mIpClientStartCv.block();
            }

            private void awaitIpClientShutdown() {
                mIpClientShutdownCv.block();
            }

            @Override
            public void onProvisioningSuccess(LinkProperties newLp) {
                mHandler.post(() -> onIpLayerStarted(newLp));
            }

            @Override
            public void onProvisioningFailure(LinkProperties newLp) {
                mHandler.post(() -> onIpLayerStopped(newLp));
            }

            @Override
            public void onLinkPropertiesChange(LinkProperties newLp) {
                mHandler.post(() -> updateLinkProperties(newLp));
            }

            @Override
            public void onQuit() {
                mIpClient = null;
                mIpClientShutdownCv.open();
            }
        }

        private static void shutdownIpClient(IIpClient ipClient) {
            try {
                ipClient.shutdown();
            } catch (RemoteException e) {
                Log.e(TAG, "Error stopping IpClient", e);
            }
        }

        NetworkInterfaceState(String ifaceName, String hwAddress, Handler handler, Context context,
                @NonNull NetworkCapabilities capabilities, NetworkFactory networkFactory) {
            name = ifaceName;
            mCapabilities = checkNotNull(capabilities);
            mHandler = handler;
            mContext = context;
            mNetworkFactory = networkFactory;
            int legacyType = ConnectivityManager.TYPE_NONE;
            int[] transportTypes = mCapabilities.getTransportTypes();

            if (transportTypes.length > 0) {
                legacyType = getLegacyType(transportTypes[0]);
            } else {
                // Should never happen as transport is always one of ETHERNET or a valid override
                throw new ConfigurationException("Network Capabilities do not have an associated "
                        + "transport type.");
            }

            mHwAddress = hwAddress;
            mLegacyType = legacyType;
        }

        void setIpConfig(IpConfiguration ipConfig) {
            if (Objects.equals(this.mIpConfig, ipConfig)) {
                if (DBG) Log.d(TAG, "ipConfig have not changed,so ignore setIpConfig");
                return;
            }
            this.mIpConfig = ipConfig;
            if (mNetworkAgent != null) {
                restart();
            }
        }

        boolean satisfied(NetworkCapabilities requestedCapabilities) {
            return requestedCapabilities.satisfiedByNetworkCapabilities(mCapabilities);
        }

        boolean isRestricted() {
            return !mCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        }

        /**
         * Determines the legacy transport type from a NetworkCapabilities transport type. Defaults
         * to legacy TYPE_NONE if there is no known conversion
         */
        private static int getLegacyType(int transport) {
            TransportInfo transportInfo = sTransports.get(transport, /* if dne */ null);
            if (transportInfo != null) {
                return transportInfo.mLegacyType;
            }
            return ConnectivityManager.TYPE_NONE;
        }

        /**
         * Determines the network score based on the transport associated with the interface.
         * Ethernet interfaces could propagate a transport types forward. Since we can't
         * get more information about the statuses of the interfaces on the other end of the local
         * interface, we'll best-effort assign the score as the base score of the assigned transport
         * when the link is up. When the link is down, the score is set to zero.
         *
         * This function is called with the purpose of assigning and updating the network score of
         * the member NetworkAgent.
         */
        private int getNetworkScore() {
            // never set the network score below 0.
            if (!mLinkUp) {
                return 0;
            }

            int[] transportTypes = mCapabilities.getTransportTypes();
            if (transportTypes.length < 1) {
                Log.w(TAG, "Network interface '" + mLinkProperties.getInterfaceName() + "' has no "
                        + "transport type associated with it. Score set to zero");
                return 0;
            }
            TransportInfo transportInfo = sTransports.get(transportTypes[0], /* if dne */ null);
            if (transportInfo != null) {
                return transportInfo.mScore;
            }
            return 0;
        }

        private void start() {
            if (mIpClient != null) {
                if (DBG) Log.d(TAG, "IpClient already started");
                return;
            }
            if (DBG) {
                Log.d(TAG, String.format("Starting Ethernet IpClient(%s)", name));
            }

            mIpClientCallback = new IpClientCallbacksImpl();
            IpClientUtil.makeIpClient(mContext, name, mIpClientCallback);
            mIpClientCallback.awaitIpClientStart();
            if (sTcpBufferSizes == null) {
                sTcpBufferSizes = mContext.getResources().getString(
                        com.android.internal.R.string.config_ethernet_tcp_buffers);
            }
            provisionIpClient(mIpClient, mIpConfig, sTcpBufferSizes);
        }

        void onIpLayerStarted(LinkProperties linkProperties) {
            if (mNetworkAgent != null) {
                Log.e(TAG, "Already have a NetworkAgent - aborting new request");
                stop();
                return;
            }
            mLinkProperties = linkProperties;

            // Create our NetworkAgent.
            final NetworkAgentConfig config = new NetworkAgentConfig.Builder()
                    .setLegacyType(mLegacyType)
                    .setLegacyTypeName(NETWORK_TYPE)
                    .setLegacyExtraInfo(mHwAddress)
                    .build();
            mNetworkAgent = new NetworkAgent(mContext, mHandler.getLooper(),
                    NETWORK_TYPE, mCapabilities, mLinkProperties,
                    getNetworkScore(), config, mNetworkFactory.getProvider()) {
                public void unwanted() {
                    if (this == mNetworkAgent) {
                        stop();
                    } else if (mNetworkAgent != null) {
                        Log.d(TAG, "Ignoring unwanted as we have a more modern " +
                                "instance");
                    }  // Otherwise, we've already called stop.
                }
            };
            mNetworkAgent.register();
            mNetworkAgent.markConnected();
        }

        void onIpLayerStopped(LinkProperties linkProperties) {
            // This cannot happen due to provisioning timeout, because our timeout is 0. It can only
            // happen if we're provisioned and we lose provisioning.
            stop();
            // If the interface has disappeared provisioning will fail over and over again, so
            // there is no point in starting again
            if (null != InterfaceParams.getByName(name)) {
                start();
            }
        }

        void updateLinkProperties(LinkProperties linkProperties) {
            mLinkProperties = linkProperties;
            if (mNetworkAgent != null) {
                mNetworkAgent.sendLinkProperties(linkProperties);
            }
        }

        /** Returns true if state has been modified */
        boolean updateLinkState(boolean up) {
            if (mLinkUp == up) return false;
            mLinkUp = up;

            stop();
            if (up) {
                start();
            }

            return true;
        }

        void stop() {
            // Invalidate all previous start requests
            if (mIpClient != null) {
                shutdownIpClient(mIpClient);
                mIpClientCallback.awaitIpClientShutdown();
                mIpClient = null;
            }
            mIpClientCallback = null;

            if (mNetworkAgent != null) {
                mNetworkAgent.unregister();
                mNetworkAgent = null;
            }
            mLinkProperties.clear();
        }

        private void updateAgent() {
            if (mNetworkAgent == null) return;
            if (DBG) {
                Log.i(TAG, "Updating mNetworkAgent with: " +
                        mCapabilities + ", " +
                        mLinkProperties);
            }
            mNetworkAgent.sendNetworkCapabilities(mCapabilities);
            mNetworkAgent.sendLinkProperties(mLinkProperties);

            // As a note, getNetworkScore() is fairly expensive to calculate. This is fine for now
            // since the agent isn't updated frequently. Consider caching the score in the future if
            // agent updating is required more often
            mNetworkAgent.sendNetworkScore(getNetworkScore());
        }

        private static void provisionIpClient(IIpClient ipClient, IpConfiguration config,
                String tcpBufferSizes) {
            if (config.getProxySettings() == ProxySettings.STATIC ||
                    config.getProxySettings() == ProxySettings.PAC) {
                try {
                    ipClient.setHttpProxy(config.getHttpProxy());
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }

            if (!TextUtils.isEmpty(tcpBufferSizes)) {
                try {
                    ipClient.setTcpBufferSizes(tcpBufferSizes);
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }

            final ProvisioningConfiguration provisioningConfiguration;
            if (config.getIpAssignment() == IpAssignment.STATIC) {
                provisioningConfiguration = new ProvisioningConfiguration.Builder()
                        .withStaticConfiguration(config.getStaticIpConfiguration())
                        .build();
            } else {
                provisioningConfiguration = new ProvisioningConfiguration.Builder()
                        .withProvisioningTimeoutMs(0)
                        .build();
            }

            try {
                ipClient.startProvisioning(provisioningConfiguration.toStableParcelable());
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }

        void restart(){
            if (DBG) Log.d(TAG, "reconnecting Etherent");
            stop();
            start();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{ "
                    + "refCount: " + refCount + ", "
                    + "iface: " + name + ", "
                    + "up: " + mLinkUp + ", "
                    + "hwAddress: " + mHwAddress + ", "
                    + "networkCapabilities: " + mCapabilities + ", "
                    + "networkAgent: " + mNetworkAgent + ", "
                    + "score: " + getNetworkScore() + ", "
                    + "ipClient: " + mIpClient + ","
                    + "linkProperties: " + mLinkProperties
                    + "}";
        }
    }

    void dump(FileDescriptor fd, IndentingPrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println(getClass().getSimpleName());
        pw.println("Tracking interfaces:");
        pw.increaseIndent();
        for (String iface: mTrackingInterfaces.keySet()) {
            NetworkInterfaceState ifaceState = mTrackingInterfaces.get(iface);
            pw.println(iface + ":" + ifaceState);
            pw.increaseIndent();
            final IIpClient ipClient = ifaceState.mIpClient;
            if (ipClient != null) {
                IpClientUtil.dumpIpClient(ipClient, fd, pw, args);
            } else {
                pw.println("IpClient is null");
            }
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }
}
