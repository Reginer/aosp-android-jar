/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.net.ipsec.ike.net;

import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.net.ipsec.ike.IkeManager.getIkeLog;
import static android.net.ipsec.ike.IkeSessionParams.ESP_ENCAP_TYPE_NONE;
import static android.net.ipsec.ike.IkeSessionParams.ESP_ENCAP_TYPE_UDP;
import static android.net.ipsec.ike.IkeSessionParams.ESP_IP_VERSION_AUTO;
import static android.net.ipsec.ike.IkeSessionParams.ESP_IP_VERSION_IPV4;
import static android.net.ipsec.ike.IkeSessionParams.ESP_IP_VERSION_IPV6;
import static android.net.ipsec.ike.IkeSessionParams.IKE_NATT_KEEPALIVE_DELAY_SEC_MAX;
import static android.net.ipsec.ike.IkeSessionParams.IKE_NATT_KEEPALIVE_DELAY_SEC_MIN;
import static android.net.ipsec.ike.IkeSessionParams.IKE_OPTION_AUTOMATIC_ADDRESS_FAMILY_SELECTION;
import static android.net.ipsec.ike.IkeSessionParams.IKE_OPTION_AUTOMATIC_NATT_KEEPALIVES;
import static android.net.ipsec.ike.IkeSessionParams.IKE_OPTION_FORCE_PORT_4500;
import static android.net.ipsec.ike.exceptions.IkeException.wrapAsIkeException;

import static com.android.internal.net.ipsec.ike.IkeContext.CONFIG_AUTO_NATT_KEEPALIVES_CELLULAR_TIMEOUT_OVERRIDE_SECONDS;
import static com.android.internal.net.ipsec.ike.utils.IkeAlarm.IkeAlarmConfig;
import static com.android.internal.net.ipsec.ike.utils.IkeAlarmReceiver.ACTION_KEEPALIVE;

import android.annotation.IntDef;
import android.app.PendingIntent;
import android.net.ConnectivityManager;
import android.net.IpSecManager;
import android.net.IpSecManager.ResourceUnavailableException;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.ipsec.ike.IkeSessionConnectionInfo;
import android.net.ipsec.ike.IkeSessionParams;
import android.net.ipsec.ike.exceptions.IkeException;
import android.os.Handler;
import android.os.Message;
import android.system.ErrnoException;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.ipsec.ike.IkeContext;
import com.android.internal.net.ipsec.ike.IkeSocket;
import com.android.internal.net.ipsec.ike.IkeSocketConfig;
import com.android.internal.net.ipsec.ike.IkeUdp4Socket;
import com.android.internal.net.ipsec.ike.IkeUdp6Socket;
import com.android.internal.net.ipsec.ike.IkeUdp6WithEncapPortSocket;
import com.android.internal.net.ipsec.ike.IkeUdpEncapSocket;
import com.android.internal.net.ipsec.ike.SaRecord.IkeSaRecord;
import com.android.internal.net.ipsec.ike.keepalive.IkeNattKeepalive;
import com.android.internal.net.ipsec.ike.keepalive.IkeNattKeepalive.KeepaliveConfig;
import com.android.internal.net.ipsec.ike.message.IkeHeader;
import com.android.internal.net.ipsec.ike.shim.ShimUtils;
import com.android.internal.net.ipsec.ike.utils.IkeAlarm;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * IkeConnectionController manages all connectivity events for an IKE Session
 *
 * <p>IkeConnectionController's responsibilities include:
 *
 * <ul>
 *   <li>Manage IkeSocket for sending and receiving IKE packets
 *   <li>Monitor and handle network and addresses changes
 *   <li>Schedule NAT-T keepalive
 * </ul>
 *
 * An IkeConnectionController should be set up when IKE Session is being established and should be
 * torn down when the IKE Session is terminated.
 */
public class IkeConnectionController implements IkeNetworkUpdater, IkeSocket.Callback {
    private static final String TAG = IkeConnectionController.class.getSimpleName();

    // The maximum number of attempts allowed for a single DNS resolution.
    private static final int MAX_DNS_RESOLUTION_ATTEMPTS = 3;

    @VisibleForTesting public static final int AUTO_KEEPALIVE_DELAY_SEC_WIFI = 15;
    @VisibleForTesting public static final int AUTO_KEEPALIVE_DELAY_SEC_CELL = 150;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        NAT_TRAVERSAL_SUPPORT_NOT_CHECKED,
        NAT_TRAVERSAL_UNSUPPORTED,
        NAT_NOT_DETECTED,
        NAT_DETECTED
    })
    public @interface NatStatus {}

    /** The IKE client has not checked whether the server supports NAT-T */
    public static final int NAT_TRAVERSAL_SUPPORT_NOT_CHECKED = 0;
    /** The IKE server does not support NAT-T */
    public static final int NAT_TRAVERSAL_UNSUPPORTED = 1;
    /** There is no NAT between the IKE client and the server */
    public static final int NAT_NOT_DETECTED = 2;
    /** There is at least a NAT between the IKE client and the server */
    public static final int NAT_DETECTED = 3;

    private final IkeContext mIkeContext;
    private final Config mConfig;
    private final ConnectivityManager mConnectivityManager;
    private final IpSecManager mIpSecManager;
    private final Dependencies mDependencies;
    private final IkeLocalAddressGenerator mIkeLocalAddressGenerator;
    private final Callback mCallback;

    private final boolean mForcePort4500;
    private final boolean mUseCallerConfiguredNetwork;
    private final String mRemoteHostname;
    private final int mDscp = 0;
    private final IkeSessionParams mIkeParams;
    // Must only be touched on the IkeSessionStateMachine thread.
    private IkeAlarmConfig mKeepaliveAlarmConfig;

    private IkeSocket mIkeSocket;

    /** Underlying network for this IKE Session. May change if mobility handling is enabled. */
    private Network mNetwork;

    /** NetworkCapabilities of the underlying network */
    private NetworkCapabilities mNc;

    /**
     * Network callback used to keep IkeConnectionController aware of network changes when mobility
     * handling is enabled.
     */
    private IkeNetworkCallbackBase mNetworkCallback;

    private boolean mMobilityEnabled = false;

    /** Local address assigned on device. */
    private InetAddress mLocalAddress;
    /** Remote address resolved from caller configured hostname. */
    private InetAddress mRemoteAddress;
    /** Available remote addresses that are v4. */
    private final List<Inet4Address> mRemoteAddressesV4 = new ArrayList<>();
    /** Available remote addresses that are v6. */
    private final List<Inet6Address> mRemoteAddressesV6 = new ArrayList<>();

    private final Set<IkeSaRecord> mIkeSaRecords = new HashSet<>();

    @NatStatus private int mNatStatus;

    // Must only be touched on the IkeSessionStateMachine thread.
    @IkeSessionParams.EspIpVersion private int mIpVersion;
    @IkeSessionParams.EspEncapType private int mEncapType;

    //Must only be touched on the IkeSessionStateMachine thread.
    private Network mUnderpinnedNetwork;

    private IkeNattKeepalive mIkeNattKeepalive;

    /** Constructor of IkeConnectionController */
    @VisibleForTesting
    public IkeConnectionController(
            IkeContext ikeContext, Config config, Dependencies dependencies) {
        mIkeContext = ikeContext;
        mConfig = config;
        mConnectivityManager = mIkeContext.getContext().getSystemService(ConnectivityManager.class);
        mIpSecManager = mIkeContext.getContext().getSystemService(IpSecManager.class);
        mDependencies = dependencies;
        mIkeLocalAddressGenerator = dependencies.newIkeLocalAddressGenerator();
        mCallback = config.callback;

        mIkeParams = config.ikeParams;
        mForcePort4500 = config.ikeParams.hasIkeOption(IKE_OPTION_FORCE_PORT_4500);
        mRemoteHostname = config.ikeParams.getServerHostname();
        mUseCallerConfiguredNetwork = config.ikeParams.getConfiguredNetwork() != null;
        mIpVersion = config.ikeParams.getIpVersion();
        mEncapType = config.ikeParams.getEncapType();
        mUnderpinnedNetwork = null;

        if (mUseCallerConfiguredNetwork) {
            mNetwork = config.ikeParams.getConfiguredNetwork();
        } else {
            mNetwork = mConnectivityManager.getActiveNetwork();
            if (mNetwork == null) {
                throw new IllegalStateException("No active default network found");
            }
        }

        getIkeLog().d(TAG, "Set up on Network " + mNetwork);

        mNatStatus = NAT_TRAVERSAL_SUPPORT_NOT_CHECKED;
    }

    /** Constructor of IkeConnectionController */
    public IkeConnectionController(IkeContext ikeContext, Config config) {
        this(ikeContext, config, new Dependencies());
    }

    /** Config includes all configurations to build an IkeConnectionController */
    public static class Config {
        public final IkeSessionParams ikeParams;
        public final int ikeSessionId;
        public final int alarmCmd;
        public final int sendKeepaliveCmd;
        public final Callback callback;

        /** Constructor for IkeConnectionController.Config */
        public Config(
                IkeSessionParams ikeParams,
                int ikeSessionId,
                int alarmCmd,
                int sendKeepaliveCmd,
                Callback callback) {
            this.ikeParams = ikeParams;
            this.ikeSessionId = ikeSessionId;
            this.alarmCmd = alarmCmd;
            this.sendKeepaliveCmd = sendKeepaliveCmd;
            this.callback = callback;
        }
    }

    /** Callback to notify status changes of the connection */
    public interface Callback {
        /** Notify the IkeConnectionController caller the underlying network has changed */
        void onUnderlyingNetworkUpdated();

        /** Notify the IkeConnectionController caller that the underlying network died */
        void onUnderlyingNetworkDied(Network network);

        /** Notify the IkeConnectionController caller of the incoming IKE packet */
        void onIkePacketReceived(IkeHeader ikeHeader, byte[] ikePackets);

        /** Notify the IkeConnectionController caller of the IKE fatal error */
        void onError(IkeException exception);
    }

    /** External dependencies, for injection in tests */
    @VisibleForTesting
    public static class Dependencies {
        /** Gets an IkeLocalAddressGenerator */
        public IkeLocalAddressGenerator newIkeLocalAddressGenerator() {
            return new IkeLocalAddressGenerator();
        }

        /** Builds and starts NATT keepalive */
        public IkeNattKeepalive newIkeNattKeepalive(
                IkeContext ikeContext, KeepaliveConfig keepaliveConfig) throws IOException {
            IkeNattKeepalive keepalive =
                    new IkeNattKeepalive(
                            ikeContext,
                            ikeContext.getContext().getSystemService(ConnectivityManager.class),
                            keepaliveConfig);
            keepalive.start();
            return keepalive;
        }

        /** Builds and returns a new IkeUdp4Socket */
        public IkeUdp4Socket newIkeUdp4Socket(
                IkeSocketConfig sockConfig, IkeSocket.Callback callback, Handler handler)
                throws ErrnoException, IOException {
            return IkeUdp4Socket.getInstance(sockConfig, callback, handler);
        }

        /** Builds and returns a new IkeUdp6Socket */
        public IkeUdp6Socket newIkeUdp6Socket(
                IkeSocketConfig sockConfig, IkeSocket.Callback callback, Handler handler)
                throws ErrnoException, IOException {
            return IkeUdp6Socket.getInstance(sockConfig, callback, handler);
        }

        /** Builds and returns a new IkeUdp6WithEncapPortSocket */
        public IkeUdp6WithEncapPortSocket newIkeUdp6WithEncapPortSocket(
                IkeSocketConfig sockConfig, IkeSocket.Callback callback, Handler handler)
                throws ErrnoException, IOException {
            return IkeUdp6WithEncapPortSocket.getIkeUdpEncapSocket(sockConfig, callback, handler);
        }

        /** Builds and returns a new IkeUdpEncapSocket */
        public IkeUdpEncapSocket newIkeUdpEncapSocket(
                IkeSocketConfig sockConfig,
                IpSecManager ipSecManager,
                IkeSocket.Callback callback,
                Handler handler)
                throws ErrnoException, IOException, ResourceUnavailableException {
            return IkeUdpEncapSocket.getIkeUdpEncapSocket(
                    sockConfig, ipSecManager, callback, handler.getLooper());
        }
    }

    /**
     * Get the keepalive delay from params, transports and device config.
     *
     * If the AUTOMATIC_NATT_KEEPALIVES option is set, look up the transport in the network
     * capabilities ; if Wi-Fi use the fixed delay, if cell use the device property int
     * (or a fixed delay in the absence of the permission to read device properties).
     * For other transports, or if the AUTOMATIC_NATT_KEEPALIVES option is not set, use the
     * delay from the session params.
     *
     * @param ikeContext Context to read the device config, if necessary.
     * @param ikeParams the session params
     * @param nc the capabilities of the underlying network
     * @return the keepalive delay to use, in seconds.
     */
    @VisibleForTesting
    public static int getKeepaliveDelaySec(
            IkeContext ikeContext, IkeSessionParams ikeParams, NetworkCapabilities nc) {
        int keepaliveDelaySeconds = ikeParams.getNattKeepAliveDelaySeconds();

        if (ikeParams.hasIkeOption(IKE_OPTION_AUTOMATIC_NATT_KEEPALIVES)) {
            if (nc.hasTransport(TRANSPORT_WIFI)) {
                // Most of the time, IKE Session will use shorter keepalive timer on WiFi. Thus
                // choose the Wifi timer as a more conservative value when the NetworkCapabilities
                // have both TRANSPORT_WIFI and TRANSPORT_CELLULAR
                final int autoDelaySeconds = AUTO_KEEPALIVE_DELAY_SEC_WIFI;
                keepaliveDelaySeconds = Math.min(keepaliveDelaySeconds, autoDelaySeconds);
            } else if (nc.hasTransport(TRANSPORT_CELLULAR)) {
                final int autoDelaySeconds =
                        ikeContext.getDeviceConfigPropertyInt(
                                CONFIG_AUTO_NATT_KEEPALIVES_CELLULAR_TIMEOUT_OVERRIDE_SECONDS,
                                IKE_NATT_KEEPALIVE_DELAY_SEC_MIN,
                                IKE_NATT_KEEPALIVE_DELAY_SEC_MAX,
                                AUTO_KEEPALIVE_DELAY_SEC_CELL);
                keepaliveDelaySeconds = Math.min(keepaliveDelaySeconds, autoDelaySeconds);
            }
        }

        return keepaliveDelaySeconds;
    }

    private static IkeAlarmConfig buildInitialKeepaliveAlarmConfig(
            Handler handler,
            IkeContext ikeContext,
            Config config,
            IkeSessionParams ikeParams,
            NetworkCapabilities nc) {
        final Message keepaliveMsg = handler.obtainMessage(
                config.alarmCmd /* what */,
                config.ikeSessionId /* arg1 */,
                config.sendKeepaliveCmd /* arg2 */);
        final PendingIntent keepaliveIntent = IkeAlarm.buildIkeAlarmIntent(ikeContext.getContext(),
                ACTION_KEEPALIVE, getIntentIdentifier(config.ikeSessionId), keepaliveMsg);

        return new IkeAlarmConfig(
                ikeContext.getContext(),
                ACTION_KEEPALIVE,
                TimeUnit.SECONDS.toMillis(getKeepaliveDelaySec(ikeContext, ikeParams, nc)),
                keepaliveIntent,
                keepaliveMsg);
    }

    private static String getIntentIdentifier(int ikeSessionId) {
        return TAG + "_" + ikeSessionId;
    }

    /** Update the IKE NATT keepalive */
    private void setupOrUpdateNattKeeaplive(IkeSocket ikeSocket) throws IOException {
        if (!(ikeSocket instanceof IkeUdpEncapSocket)) {
            if (mIkeNattKeepalive != null) {
                mIkeNattKeepalive.stop();
                mIkeNattKeepalive = null;
            }
            return;
        }

        final KeepaliveConfig keepaliveConfig =
                new KeepaliveConfig(
                        (Inet4Address) mLocalAddress,
                        (Inet4Address) mRemoteAddress,
                        ((IkeUdpEncapSocket) ikeSocket).getUdpEncapsulationSocket(),
                        mNetwork,
                        mUnderpinnedNetwork,
                        mKeepaliveAlarmConfig,
                        mIkeParams);

        if (mIkeNattKeepalive != null) {
            mIkeNattKeepalive.restart(keepaliveConfig);
        } else {
            mIkeNattKeepalive = mDependencies.newIkeNattKeepalive(mIkeContext, keepaliveConfig);
        }
    }

    private IkeSocket getIkeSocket(boolean isIpv4, boolean useEncapPort) throws IkeException {
        IkeSocketConfig sockConfig = new IkeSocketConfig(this, mDscp);
        IkeSocket result = null;

        try {
            if (useEncapPort) {
                if (isIpv4) {
                    result = mDependencies.newIkeUdpEncapSocket(
                            sockConfig, mIpSecManager, this, new Handler(mIkeContext.getLooper()));
                } else {
                    result = mDependencies.newIkeUdp6WithEncapPortSocket(
                            sockConfig, this, new Handler(mIkeContext.getLooper()));
                }
            } else {
                if (isIpv4) {
                    result = mDependencies.newIkeUdp4Socket(
                            sockConfig, this, new Handler(mIkeContext.getLooper()));
                } else {
                    result = mDependencies.newIkeUdp6Socket(
                            sockConfig, this, new Handler(mIkeContext.getLooper()));
                }
            }

            if (result == null) {
                throw new IOException("No socket created");
            }

            result.bindToNetwork(mNetwork);
            return result;
        } catch (ErrnoException | IOException | ResourceUnavailableException e) {
            throw wrapAsIkeException(e);
        }
    }

    private void migrateSpiToIkeSocket(long localSpi, IkeSocket oldSocket, IkeSocket newSocket) {
        newSocket.registerIke(localSpi, this);
        oldSocket.unregisterIke(localSpi);
    }

    private void getAndSwitchToIkeSocket(boolean isIpv4, boolean useEncapPort) throws IkeException {
        IkeSocket newSocket = getIkeSocket(isIpv4, useEncapPort);

        try {
            setupOrUpdateNattKeeaplive(newSocket);
        } catch (IOException e) {
            throw wrapAsIkeException(e);
        }

        if (newSocket != mIkeSocket) {
            for (IkeSaRecord saRecord : mIkeSaRecords) {
                migrateSpiToIkeSocket(saRecord.getLocalSpi(), mIkeSocket, newSocket);
            }
            mIkeSocket.releaseReference(this);
            mIkeSocket = newSocket;
        }
    }

    /** Sets up the IkeConnectionController */
    public void setUp() throws IkeException {
        // Make sure all the resources, especially the NetworkCallback, is released before creating
        // new one.
        unregisterResources();

        // This is call is directly from the IkeSessionStateMachine, and thus cannot be
        // accidentally called in a NetworkCallback. See
        // ConnectivityManager.NetworkCallback#onLinkPropertiesChanged() and
        // ConnectivityManager.NetworkCallback#onCapabilitiesChanged() for discussion of
        // mixing callbacks and synchronous polling methods.
        LinkProperties linkProperties = mConnectivityManager.getLinkProperties(mNetwork);
        mNc = mConnectivityManager.getNetworkCapabilities(mNetwork);
        mKeepaliveAlarmConfig = buildInitialKeepaliveAlarmConfig(
                new Handler(mIkeContext.getLooper()), mIkeContext, mConfig, mIkeParams, mNc);
        try {
            if (linkProperties == null || mNc == null) {
                // Throw NPE to preserve the existing behaviour for backward compatibility
                throw wrapAsIkeException(
                        new NullPointerException(
                                "Attempt setup on network "
                                        + mNetwork
                                        + " with null LinkProperties or null NetworkCapabilities"));
            }
            resolveAndSetAvailableRemoteAddresses();
            selectAndSetRemoteAddress(linkProperties);

            int remotePort =
                    mForcePort4500
                            ? IkeSocket.SERVER_PORT_UDP_ENCAPSULATED
                            : IkeSocket.SERVER_PORT_NON_UDP_ENCAPSULATED;
            boolean isIpv4 = mRemoteAddress instanceof Inet4Address;
            mLocalAddress =
                    mIkeLocalAddressGenerator.generateLocalAddress(
                            mNetwork, isIpv4, mRemoteAddress, remotePort);
            mIkeSocket = getIkeSocket(isIpv4, mForcePort4500);

            setupOrUpdateNattKeeaplive(mIkeSocket);
        } catch (IOException | ErrnoException e) {
            throw wrapAsIkeException(e);
        }

        try {
            if (mUseCallerConfiguredNetwork) {
                // Caller configured a specific Network - track it
                // ConnectivityManager does not provide a callback for tracking a specific
                // Network. In order to do so, create a NetworkRequest without any
                // capabilities so it will match all Networks. The NetworkCallback will then
                // filter for the correct (caller-specified) Network.
                NetworkRequest request = new NetworkRequest.Builder().clearCapabilities().build();
                mNetworkCallback =
                        new IkeSpecificNetworkCallback(
                                this, mNetwork, mLocalAddress, linkProperties, mNc);
                mConnectivityManager.registerNetworkCallback(
                        request, mNetworkCallback, new Handler(mIkeContext.getLooper()));
            } else {
                // Caller did not configure a specific Network - track the default
                mNetworkCallback =
                        new IkeDefaultNetworkCallback(
                                this, mNetwork, mLocalAddress, linkProperties, mNc);
                mConnectivityManager.registerDefaultNetworkCallback(
                        mNetworkCallback, new Handler(mIkeContext.getLooper()));
            }
        } catch (RuntimeException e) {
            mNetworkCallback = null;
            throw wrapAsIkeException(e);
        }
    }

    private void unregisterResources() {
        if (mIkeNattKeepalive != null) {
            mIkeNattKeepalive.stop();
            mIkeNattKeepalive = null;
        }

        if (mNetworkCallback != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }

        if (mIkeSocket != null) {
            for (IkeSaRecord saRecord : mIkeSaRecords) {
                mIkeSocket.unregisterIke(saRecord.getLocalSpi());
            }

            mIkeSocket.releaseReference(this);
            mIkeSocket = null;
        }

        mIkeSaRecords.clear();
    }

    /** Tears down the IkeConnectionController */
    public void tearDown() {
        unregisterResources();
    }

    /** Returns the IkeSocket */
    public IkeSocket getIkeSocket() {
        return mIkeSocket;
    }

    /** Returns if the IkeSocket is a UDP encapsulation socket */
    public boolean useUdpEncapSocket() {
        return mIkeSocket instanceof IkeUdpEncapSocket;
    }

    /** Sends out an IKE packet */
    public void sendIkePacket(byte[] ikePacket) {
        mIkeSocket.sendIkePacket(ikePacket, mRemoteAddress);
    }

    /** Registers the local SPI for an IKE SA waiting for the IKE INIT response */
    public void registerIkeSpi(long ikeSpi) {
        mIkeSocket.registerIke(ikeSpi, this);
    }

    /** Unregisters the local SPI for an IKE SA that failed IKE INIT exchange */
    public void unregisterIkeSpi(long ikeSpi) {
        mIkeSocket.unregisterIke(ikeSpi);
    }

    /** Registers a newly created IKE SA */
    public void registerIkeSaRecord(IkeSaRecord saRecord) {
        mIkeSaRecords.add(saRecord);
        mIkeSocket.registerIke(saRecord.getLocalSpi(), this);
    }

    /** Unregisters a deleted IKE SA */
    public void unregisterIkeSaRecord(IkeSaRecord saRecord) {
        mIkeSaRecords.remove(saRecord);
        mIkeSocket.unregisterIke(saRecord.getLocalSpi());
    }

    /** Returns all registered IKE SAs */
    @VisibleForTesting
    public Set<IkeSaRecord> getIkeSaRecords() {
        return Collections.unmodifiableSet(mIkeSaRecords);
    }

    /**
     * Updates the underlying network
     *
     * <p>This call is always from IkeSessionStateMachine for migrating IKE to a caller configured
     * network, or to update the protocol preference or keepalive delay.
     */
    public void onNetworkSetByUser(
            Network network,
            int ipVersion,
            int encapType,
            int keepaliveDelaySeconds)
            throws IkeException {
        if (!mMobilityEnabled) {
            // Program error. IkeSessionStateMachine should never call this method before enabling
            // mobility.
            getIkeLog().wtf(TAG, "Attempt to update network when mobility is disabled");
            return;
        }

        // This is call is directly from the IkeSessionStateMachine, and thus cannot be
        // accidentally called in a NetworkCallback. See
        // ConnectivityManager.NetworkCallback#onLinkPropertiesChanged() and
        // ConnectivityManager.NetworkCallback#onCapabilitiesChanged() for discussion of
        // mixing callbacks and synchronous polling methods.
        final LinkProperties linkProperties = mConnectivityManager.getLinkProperties(network);
        final NetworkCapabilities networkCapabilities =
                mConnectivityManager.getNetworkCapabilities(network);

        if (linkProperties == null || networkCapabilities == null) {
            // Throw NPE to preserve the existing behaviour for backward compatibility
            throw wrapAsIkeException(
                    new NullPointerException(
                            "Attempt migrating to network "
                                    + network
                                    + " with null LinkProperties or null NetworkCapabilities"));

            // TODO(b/224686889): Notify caller of failed mobility attempt and keep this IKE Session
            // alive
        }

        mIpVersion = ipVersion;
        mEncapType = encapType;

        if (keepaliveDelaySeconds == IkeSessionParams.NATT_KEEPALIVE_INTERVAL_AUTO) {
            keepaliveDelaySeconds = getKeepaliveDelaySec(mIkeContext, mIkeParams, mNc);
        }
        final long keepaliveDelayMs = TimeUnit.SECONDS.toMillis(keepaliveDelaySeconds);
        if (keepaliveDelayMs != mKeepaliveAlarmConfig.delayMs) {
            mKeepaliveAlarmConfig = mKeepaliveAlarmConfig.buildCopyWithDelayMs(keepaliveDelayMs);
            restartKeepaliveIfRunning();
        }

        // Switch to monitor a new network. This call is never expected to trigger a callback
        mNetworkCallback.setNetwork(network, linkProperties, networkCapabilities);
        handleUnderlyingNetworkUpdated(
                network, linkProperties, networkCapabilities, false /* skipIfSameNetwork */);
    }

    /** Called when the underpinned network is set by the user */
    public void onUnderpinnedNetworkSetByUser(final Network underpinnedNetwork)
            throws IkeException {
        mUnderpinnedNetwork = underpinnedNetwork;
        restartKeepaliveIfRunning();
    }

    private void restartKeepaliveIfRunning() throws IkeException {
        try {
            setupOrUpdateNattKeeaplive(mIkeSocket);
        } catch (IOException e) {
            throw wrapAsIkeException(e);
        }
    }

    /** Gets the underlying network */
    public Network getNetwork() {
        return mNetwork;
    }

    /** Gets the underpinned network */
    public Network getUnderpinnedNetwork() {
        return mUnderpinnedNetwork;
    }

    /** Check if mobility is enabled */
    public boolean isMobilityEnabled() {
        return mMobilityEnabled;
    }

    /**
     * Sets the local address.
     *
     * <p>This MUST only be called in a test.
     */
    @VisibleForTesting
    public void setLocalAddress(InetAddress address) {
        mLocalAddress = address;
    }

    /** Gets the local address */
    public InetAddress getLocalAddress() {
        return mLocalAddress;
    }

    /**
     * Sets the remote address.
     *
     * <p>This MUST only be called in a test.
     */
    @VisibleForTesting
    public void setRemoteAddress(InetAddress address) {
        mRemoteAddress = address;
        addRemoteAddress(address);
    }

    /**
     * Adds a remote address.
     *
     * <p>This MUST only be called in a test.
     */
    @VisibleForTesting
    public void addRemoteAddress(InetAddress address) {
        if (address instanceof Inet4Address) {
            mRemoteAddressesV4.add((Inet4Address) address);
        } else {
            mRemoteAddressesV6.add((Inet6Address) address);
        }
    }

    /** Gets the remote addresses */
    public InetAddress getRemoteAddress() {
        return mRemoteAddress;
    }

    /** Gets all the IPv4 remote addresses */
    public List<Inet4Address> getAllRemoteIpv4Addresses() {
        return new ArrayList<>(mRemoteAddressesV4);
    }

    /** Gets all the IPv6 remote addresses */
    public List<Inet6Address> getAllRemoteIpv6Addresses() {
        return new ArrayList<>(mRemoteAddressesV6);
    }

    /** Gets the local port */
    public int getLocalPort() {
        try {
            return mIkeSocket.getLocalPort();
        } catch (ErrnoException e) {
            throw new IllegalStateException("Fail to get local port", e);
        }
    }

    /** Gets the remote port */
    public int getRemotePort() {
        return mIkeSocket.getIkeServerPort();
    }

    /** Handles NAT detection result in IKE INIT */
    public void handleNatDetectionResultInIkeInit(boolean isNatDetected, long localSpi)
            throws IkeException {
        if (!isNatDetected) {
            mNatStatus = NAT_NOT_DETECTED;
            return;
        }

        mNatStatus = NAT_DETECTED;
        if (mRemoteAddress instanceof Inet6Address) {
            throw wrapAsIkeException(new UnsupportedOperationException("IPv6 NAT-T not supported"));
        }

        getIkeLog().d(TAG, "Switching to send to remote port 4500 if it's not already");

        IkeSocket newSocket = getIkeSocket(true /* isIpv4 */, true /* useEncapPort */);

        try {
            setupOrUpdateNattKeeaplive(newSocket);
        } catch (IOException e) {
            throw wrapAsIkeException(e);
        }

        if (newSocket != mIkeSocket) {
            migrateSpiToIkeSocket(localSpi, mIkeSocket, newSocket);
            mIkeSocket.releaseReference(this);
            mIkeSocket = newSocket;
        }
    }

    /** Handles NAT detection result in the MOBIKE INFORMATIONAL exchange */
    public void handleNatDetectionResultInMobike(boolean isNatDetected) throws IkeException {
        if (!isNatDetected) {
            mNatStatus = NAT_NOT_DETECTED;
            return;
        }

        mNatStatus = NAT_DETECTED;
        if (mRemoteAddress instanceof Inet6Address) {
            throw wrapAsIkeException(new UnsupportedOperationException("IPv6 NAT-T not supported"));
        }

        getIkeLog().d(TAG, "Switching to send to remote port 4500 if it's not already");
        getAndSwitchToIkeSocket(true /* isIpv4 */, true /* useEncapPort */);
    }

    /**
     * Marks that the server does not support NAT-T
     *
     * <p>This is method should only be called at the first time IKE client sends NAT_DETECTION (in
     * other words the first time IKE client is using IPv4 address since IKE does not support IPv6
     * NAT-T)
     */
    public void markSeverNattUnsupported() {
        mNatStatus = NAT_TRAVERSAL_UNSUPPORTED;
    }

    /**
     * Clears the knowledge of sever's NAT-T support
     *
     * <p>This MUST only be called in a test.
     */
    @VisibleForTesting
    public void resetSeverNattSupport() {
        mNatStatus = NAT_TRAVERSAL_SUPPORT_NOT_CHECKED;
    }

    /** This MUST only be called in a test. */
    @VisibleForTesting
    public void setNatDetected(boolean isNatDetected) {
        if (!isNatDetected) {
            mNatStatus = NAT_NOT_DETECTED;
            return;
        }

        mNatStatus = NAT_DETECTED;
    }

    /** Returns the NAT status */
    @NatStatus
    public int getNatStatus() {
        return mNatStatus;
    }

    /** Returns the IkeNattKeepalive */
    public IkeNattKeepalive getIkeNattKeepalive() {
        return mIkeNattKeepalive;
    }

    /** Fire software keepalive */
    public void fireKeepAlive() {
        // Software keepalive alarm is fired. Ignore the alarm whe NAT-T keepalive is no
        // longer needed (e.g. migrating from IPv4 to IPv6)
        if (mIkeNattKeepalive != null) {
            mIkeNattKeepalive.onAlarmFired();
        }
    }

    private void resolveAndSetAvailableRemoteAddresses() throws IOException {
        // TODO(b/149954916): Do DNS resolution asynchronously
        InetAddress[] allRemoteAddresses = null;

        for (int attempts = 0;
                attempts < MAX_DNS_RESOLUTION_ATTEMPTS
                        && (allRemoteAddresses == null || allRemoteAddresses.length == 0);
                attempts++) {
            try {
                allRemoteAddresses = mNetwork.getAllByName(mRemoteHostname);
            } catch (UnknownHostException e) {
                final boolean willRetry = attempts + 1 < MAX_DNS_RESOLUTION_ATTEMPTS;
                getIkeLog()
                        .d(
                                TAG,
                                "Failed to look up host for attempt "
                                        + (attempts + 1)
                                        + ": "
                                        + mRemoteHostname
                                        + " retrying? "
                                        + willRetry,
                                e);
            }
        }
        if (allRemoteAddresses == null || allRemoteAddresses.length == 0) {
            final String errMsg =
                    "DNS resolution for "
                            + mRemoteHostname
                            + " failed after "
                            + MAX_DNS_RESOLUTION_ATTEMPTS
                            + " attempts";

            throw ShimUtils.getInstance().getDnsFailedException(errMsg);
        }

        getIkeLog()
                .d(
                        TAG,
                        "Resolved addresses for peer: "
                                + Arrays.toString(allRemoteAddresses)
                                + " to replace old addresses: v4="
                                + mRemoteAddressesV4
                                + " v6="
                                + mRemoteAddressesV6);

        mRemoteAddressesV4.clear();
        mRemoteAddressesV6.clear();
        for (InetAddress remoteAddress : allRemoteAddresses) {
            if (remoteAddress instanceof Inet4Address) {
                mRemoteAddressesV4.add((Inet4Address) remoteAddress);
            } else {
                mRemoteAddressesV6.add((Inet6Address) remoteAddress);
            }
        }
    }

    private static boolean hasLocalIpV4Address(LinkProperties linkProperties) {
        for (LinkAddress linkAddress : linkProperties.getAllLinkAddresses()) {
            if (linkAddress.getAddress() instanceof Inet4Address) {
                return true;
            }
        }

        return false;
    }

    /**
     * Set the remote address for the peer.
     *
     * <p>The selection of IP address is as follows:
     *
     * <ul>
     *   <li>If the caller passed in an IP address family, use that address family.
     *   <li>Otherwise, always prefer IPv6 over IPv4.
     * </ul>
     *
     * Otherwise, an IPv4 address will be used.
     */
    @VisibleForTesting
    public void selectAndSetRemoteAddress(LinkProperties linkProperties) throws IOException {
        // TODO(b/175348096): Randomly choose from available addresses when the IP family is
        // decided.
        final boolean canConnectWithIpv4 =
                !mRemoteAddressesV4.isEmpty() && hasLocalIpV4Address(linkProperties);
        final boolean canConnectWithIpv6 =
                !mRemoteAddressesV6.isEmpty() && linkProperties.hasGlobalIpv6Address();

        adjustIpVersionPreference();

        if (isIpVersionRequired(ESP_IP_VERSION_IPV4)) {
            if (!canConnectWithIpv4) {
                throw ShimUtils.getInstance().getDnsFailedException(
                        "IPv4 required but no IPv4 address available");
            }
            mRemoteAddress = mRemoteAddressesV4.get(0);
        } else if (isIpVersionRequired(ESP_IP_VERSION_IPV6)) {
            if (!canConnectWithIpv6) {
                throw ShimUtils.getInstance().getDnsFailedException(
                        "IPv6 required but no global IPv6 address available");
            }
            mRemoteAddress = mRemoteAddressesV6.get(0);
        } else if (isIpV4Preferred(mIkeParams, mNc) && canConnectWithIpv4) {
            mRemoteAddress = mRemoteAddressesV4.get(0);
        } else if (canConnectWithIpv6) {
            mRemoteAddress = mRemoteAddressesV6.get(0);
        } else if (canConnectWithIpv4) {
            mRemoteAddress = mRemoteAddressesV4.get(0);
        } else {
            // For backwards compatibility, synchronously throw IAE instead of triggering callback.
            throw new IllegalArgumentException("No valid IPv4 or IPv6 addresses for peer");
        }
    }

    private void adjustIpVersionPreference() {
        // As ESP isn't supported on v4 and UDP isn't supported on v6, a request for ENCAP_UDP
        // should force v4 and a request for ENCAP_NONE should force v6 when the family is set
        // to auto.
        // TODO : instead of fudging the arguments here, this should actually be taken into
        // account when figuring out whether to send the NAT detection packet.
        int adjustedIpVersion = mIpVersion;
        if (mIpVersion == ESP_IP_VERSION_AUTO) {
            if (mEncapType == ESP_ENCAP_TYPE_NONE) {
                adjustedIpVersion = ESP_IP_VERSION_IPV6;
            } else if (mEncapType == ESP_ENCAP_TYPE_UDP) {
                adjustedIpVersion = ESP_IP_VERSION_IPV4;
            }

            if (adjustedIpVersion != mIpVersion) {
                getIkeLog().i(TAG, "IP version preference is overridden from "
                        + mIpVersion  + " to " + adjustedIpVersion);
                mIpVersion = adjustedIpVersion;
            }
        }
    }

    private boolean isIpVersionRequired(final int ipVersion) {
        return ipVersion == mIpVersion;
    }

    @VisibleForTesting
    public static boolean isIpV4Preferred(IkeSessionParams ikeParams, NetworkCapabilities nc) {
        return ikeParams.getIpVersion() == ESP_IP_VERSION_AUTO
                && ikeParams.hasIkeOption(IKE_OPTION_AUTOMATIC_ADDRESS_FAMILY_SELECTION)
                && nc.hasTransport(TRANSPORT_WIFI);
    }

    /**
     * Enables IkeConnectionController to handle mobility events
     *
     * <p>This method will enable IkeConnectionController to monitor and handle changes of the
     * underlying network and addresses.
     */
    public void enableMobility() throws IkeException {
        mMobilityEnabled = true;

        if (mNatStatus != NAT_TRAVERSAL_UNSUPPORTED
                && mIkeSocket.getIkeServerPort() != IkeSocket.SERVER_PORT_UDP_ENCAPSULATED) {
            getAndSwitchToIkeSocket(
                    mRemoteAddress instanceof Inet4Address, true /* useEncapPort */);
        }
    }

    /** Creates a IkeSessionConnectionInfo */
    public IkeSessionConnectionInfo buildIkeSessionConnectionInfo() {
        return new IkeSessionConnectionInfo(mLocalAddress, mRemoteAddress, mNetwork);
    }

    /**
     * All the calls that are not initiated from the IkeSessionStateMachine MUST be run in this
     * method unless there are mechanisms to guarantee these calls will never crash the process.
     */
    private void executeOrSendFatalError(Runnable r) {
        ShimUtils.getInstance().executeOrSendFatalError(r, mCallback);
    }

    // This method is never expected be called due to the capabilities change of the existing
    // underlying network. Only explicit user requests, network changes, addresses changes or
    // configuration changes (such as the protocol preference) will call into this method.
    private void handleUnderlyingNetworkUpdated(
            Network network,
            LinkProperties linkProperties,
            NetworkCapabilities networkCapabilities,
            boolean skipIfSameNetwork) {
        if (!mMobilityEnabled) {
            getIkeLog().d(TAG, "onUnderlyingNetworkUpdated: Unable to handle network update");
            mCallback.onUnderlyingNetworkDied(mNetwork);

            return;
        }

        Network oldNetwork = mNetwork;
        InetAddress oldLocalAddress = mLocalAddress;
        InetAddress oldRemoteAddress = mRemoteAddress;

        mNetwork = network;
        mNc = networkCapabilities;

        // If the network changes, perform a new DNS lookup to ensure that the correct remote
        // address is used. This ensures that DNS returns addresses for the correct address families
        // (important if using a v4/v6-only network). This also ensures that DNS64 is handled
        // correctly when switching between networks that may have different IPv6 prefixes.
        if (!mNetwork.equals(oldNetwork)) {
            try {
                resolveAndSetAvailableRemoteAddresses();
            } catch (IOException e) {
                mCallback.onError(wrapAsIkeException(e));
                return;
            }
        }

        try {
            selectAndSetRemoteAddress(linkProperties);
        } catch (IOException e) {
            mCallback.onError(wrapAsIkeException(e));
            return;
        }

        boolean isIpv4 = mRemoteAddress instanceof Inet4Address;

        // If it is known that the server supports NAT-T, use port 4500. Otherwise, use port 500.
        boolean nattSupported = mNatStatus != NAT_TRAVERSAL_UNSUPPORTED;
        int serverPort =
                nattSupported
                        ? IkeSocket.SERVER_PORT_UDP_ENCAPSULATED
                        : IkeSocket.SERVER_PORT_NON_UDP_ENCAPSULATED;

        try {
            mLocalAddress =
                    mIkeLocalAddressGenerator.generateLocalAddress(
                            mNetwork, isIpv4, mRemoteAddress, serverPort);

            if (ShimUtils.getInstance().shouldSkipIfSameNetwork(skipIfSameNetwork)
                    && mNetwork.equals(oldNetwork)
                    && mLocalAddress.equals(oldLocalAddress)
                    && mRemoteAddress.equals(oldRemoteAddress)) {
                getIkeLog()
                        .d(
                                TAG,
                                "onUnderlyingNetworkUpdated: None of network, local or remote"
                                    + " address has changed, and the update is skippable. No action"
                                    + " needed here.");
                return;
            }

            if (!mNetwork.equals(oldNetwork)) {
                boolean useEncapPort = mForcePort4500 || nattSupported;
                getAndSwitchToIkeSocket(mLocalAddress instanceof Inet4Address, useEncapPort);
            }

            for (IkeSaRecord record : mIkeSaRecords) {
                record.migrate(mLocalAddress, mRemoteAddress);
            }
        } catch (IkeException | ErrnoException | IOException e) {
            mCallback.onError(wrapAsIkeException(e));
            return;
        }

        mNetworkCallback.setAddress(mLocalAddress);

        mCallback.onUnderlyingNetworkUpdated();
    }

    @Override
    public void onUnderlyingNetworkUpdated(
            Network network,
            LinkProperties linkProperties,
            NetworkCapabilities networkCapabilities) {
        executeOrSendFatalError(
                () -> {
                    handleUnderlyingNetworkUpdated(
                            network,
                            linkProperties,
                            networkCapabilities,
                            true /* skipIfSameNetwork */);
                });
    }

    @Override
    public void onCapabilitiesUpdated(NetworkCapabilities networkCapabilities) {
        executeOrSendFatalError(
                () -> {
                    mNc = networkCapabilities;

                    // No action. There is no known use case to perform mobility or update keepalive
                    // timer when NetworkCapabilities changes.
                });
    }

    @Override
    public void onUnderlyingNetworkDied() {
        executeOrSendFatalError(
                () -> {
                    mCallback.onUnderlyingNetworkDied(mNetwork);
                });
    }

    @Override
    public void onIkePacketReceived(IkeHeader ikeHeader, byte[] ikePackets) {
        executeOrSendFatalError(
                () -> {
                    mCallback.onIkePacketReceived(ikeHeader, ikePackets);
                });
    }
}
