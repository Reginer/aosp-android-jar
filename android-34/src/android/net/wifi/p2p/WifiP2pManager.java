/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.net.wifi.p2p;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.net.MacAddress;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceResponse;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceResponse;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.CloseGuard;
import android.util.Log;
import android.view.Display;

import androidx.annotation.RequiresApi;

import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Protocol;
import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * This class provides the API for managing Wi-Fi peer-to-peer connectivity. This lets an
 * application discover available peers, setup connection to peers and query for the list of peers.
 * When a p2p connection is formed over wifi, the device continues to maintain the uplink
 * connection over mobile or any other available network for internet connectivity on the device.
 *
 * <p> The API is asynchronous and responses to requests from an application are on listener
 * callbacks provided by the application. The application needs to do an initialization with
 * {@link #initialize} before doing any p2p operation.
 *
 * <p> Most application calls need a {@link ActionListener} instance for receiving callbacks
 * {@link ActionListener#onSuccess} or {@link ActionListener#onFailure}. Action callbacks
 * indicate whether the initiation of the action was a success or a failure.
 * Upon failure, the reason of failure can be one of {@link #ERROR}, {@link #P2P_UNSUPPORTED}
 * or {@link #BUSY}.
 *
 * <p> An application can initiate discovery of peers with {@link #discoverPeers}. An initiated
 * discovery request from an application stays active until the device starts connecting to a peer
 * ,forms a p2p group or there is an explicit {@link #stopPeerDiscovery}.
 * Applications can listen to {@link #WIFI_P2P_DISCOVERY_CHANGED_ACTION} to know if a peer-to-peer
 * discovery is running or stopped. Additionally, {@link #WIFI_P2P_PEERS_CHANGED_ACTION} indicates
 * if the peer list has changed.
 *
 * <p> When an application needs to fetch the current list of peers, it can request the list
 * of peers with {@link #requestPeers}. When the peer list is available
 * {@link PeerListListener#onPeersAvailable} is called with the device list.
 *
 * <p> An application can initiate a connection request to a peer through {@link #connect}. See
 * {@link WifiP2pConfig} for details on setting up the configuration. For communication with legacy
 * Wi-Fi devices that do not support p2p, an app can create a group using {@link #createGroup}
 * which creates an access point whose details can be fetched with {@link #requestGroupInfo}.
 *
 * <p> After a successful group formation through {@link #createGroup} or through {@link #connect},
 * use {@link #requestConnectionInfo} to fetch the connection details. The connection info
 * {@link WifiP2pInfo} contains the address of the group owner
 * {@link WifiP2pInfo#groupOwnerAddress} and a flag {@link WifiP2pInfo#isGroupOwner} to indicate
 * if the current device is a p2p group owner. A p2p client can thus communicate with
 * the p2p group owner through a socket connection. If the current device is the p2p group owner,
 * {@link WifiP2pInfo#groupOwnerAddress} is anonymized unless the caller holds the
 * {@code android.Manifest.permission#LOCAL_MAC_ADDRESS} permission.
 *
 * <p> With peer discovery using {@link  #discoverPeers}, an application discovers the neighboring
 * peers, but has no good way to figure out which peer to establish a connection with. For example,
 * if a game application is interested in finding all the neighboring peers that are also running
 * the same game, it has no way to find out until after the connection is setup. Pre-association
 * service discovery is meant to address this issue of filtering the peers based on the running
 * services.
 *
 * <p>With pre-association service discovery, an application can advertise a service for a
 * application on a peer device prior to a connection setup between the devices.
 * Currently, DNS based service discovery (Bonjour) and Upnp are the higher layer protocols
 * supported. Get Bonjour resources at dns-sd.org and Upnp resources at upnp.org
 * As an example, a video application can discover a Upnp capable media renderer
 * prior to setting up a Wi-fi p2p connection with the device.
 *
 * <p> An application can advertise a Upnp or a Bonjour service with a call to
 * {@link #addLocalService}. After a local service is added,
 * the framework automatically responds to a peer application discovering the service prior
 * to establishing a p2p connection. A call to {@link #removeLocalService} removes a local
 * service and {@link #clearLocalServices} can be used to clear all local services.
 *
 * <p> An application that is looking for peer devices that support certain services
 * can do so with a call to  {@link #discoverServices}. Prior to initiating the discovery,
 * application can add service discovery request with a call to {@link #addServiceRequest},
 * remove a service discovery request with a call to {@link #removeServiceRequest} or clear
 * all requests with a call to {@link #clearServiceRequests}. When no service requests remain,
 * a previously running service discovery will stop.
 *
 * The application is notified of a result of service discovery request through listener callbacks
 * set through {@link #setDnsSdResponseListeners} for Bonjour or
 * {@link #setUpnpServiceResponseListener} for Upnp.
 *
 * <p class="note"><strong>Note:</strong>
 * Registering an application handler with {@link #initialize} requires the permissions
 * {@link android.Manifest.permission#ACCESS_WIFI_STATE} and
 * {@link android.Manifest.permission#CHANGE_WIFI_STATE} to perform any further peer-to-peer
 * operations.
 *
 * {@see WifiP2pConfig}
 * {@see WifiP2pInfo}
 * {@see WifiP2pGroup}
 * {@see WifiP2pDevice}
 * {@see WifiP2pDeviceList}
 * {@see android.net.wifi.WpsInfo}
 */
@SystemService(Context.WIFI_P2P_SERVICE)
public class WifiP2pManager {
    private static final String TAG = "WifiP2pManager";

    /** @hide */
    public static final long FEATURE_SET_VENDOR_ELEMENTS        = 1L << 0;
    /** @hide */
    public static final long FEATURE_FLEXIBLE_DISCOVERY         = 1L << 1;
    /** @hide */
    public static final long FEATURE_GROUP_CLIENT_REMOVAL       = 1L << 2;
    /** @hide */
    public static final long FEATURE_GROUP_OWNER_IPV6_LINK_LOCAL_ADDRESS_PROVIDED = 1L << 3;

    /**
     * Extra for transporting a WifiP2pConfig
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_CONFIG =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_CONFIG";
    /**
     * Extra for transporting a WifiP2pServiceInfo
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_SERVICE_INFO =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_SERVICE_INFO";
    /**
     * Extra for transporting a peer discovery frequency.
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_PEER_DISCOVERY_FREQ =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_PEER_DISCOVERY_FREQ";
    /**
     * Extra for transporting a peer MAC address.
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_PEER_ADDRESS =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_PEER_ADDRESS";
    /**
     * Extra used to indicate that a message is sent from Wifi internally
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_INTERNAL_MESSAGE =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_INTERNAL_MESSAGE";

    /**
     * Used to communicate the Display ID for multi display devices.
     * @hide
     **/
    public static final String EXTRA_PARAM_KEY_DISPLAY_ID =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_DISPLAY_ID";

    /**
     * Extra for transporting a WifiP2pDevice.
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_DEVICE =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_DEVICE";
    /**
     * Extra for transporting a WPS PIN.
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_WPS_PIN =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_WPS_PIN";

    /**
     * Extra for transporting vendor-specific information element list
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_INFORMATION_ELEMENT_LIST =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_INFORMATION_ELEMENT_LIST";

    /**
     * Key for transporting a bundle of extra information.
     * @hide
     */
    public static final String EXTRA_PARAM_KEY_BUNDLE =
            "android.net.wifi.p2p.EXTRA_PARAM_KEY_BUNDLE";

    /**
     * Broadcast intent action to indicate whether Wi-Fi p2p is enabled or disabled. An
     * extra {@link #EXTRA_WIFI_STATE} provides the state information as int.
     *
     * @see #EXTRA_WIFI_STATE
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_P2P_STATE_CHANGED_ACTION =
        "android.net.wifi.p2p.STATE_CHANGED";

    /**
     * The lookup key for an int that indicates whether Wi-Fi p2p is enabled or disabled.
     * Retrieve it with {@link android.content.Intent#getIntExtra(String,int)}.
     *
     * @see #WIFI_P2P_STATE_DISABLED
     * @see #WIFI_P2P_STATE_ENABLED
     */
    public static final String EXTRA_WIFI_STATE = "wifi_p2p_state";

    /** @hide */
    @IntDef({
            WIFI_P2P_STATE_DISABLED,
            WIFI_P2P_STATE_ENABLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiP2pState {
    }

    /**
     * Wi-Fi p2p is disabled.
     *
     * @see #WIFI_P2P_STATE_CHANGED_ACTION
     */
    public static final int WIFI_P2P_STATE_DISABLED = 1;

    /**
     * Wi-Fi p2p is enabled.
     *
     * @see #WIFI_P2P_STATE_CHANGED_ACTION
     */
    public static final int WIFI_P2P_STATE_ENABLED = 2;

    /**
     * Broadcast intent action indicating that the state of Wi-Fi p2p connectivity
     * has changed. One extra {@link #EXTRA_WIFI_P2P_INFO} provides the p2p connection info in
     * the form of a {@link WifiP2pInfo} object. Another extra {@link #EXTRA_NETWORK_INFO} provides
     * the network info in the form of a {@link android.net.NetworkInfo}. A third extra provides
     * the details of the group and may contain a {@code null}.
     *
     * All of these permissions are required to receive this broadcast:
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE} and either
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * {@link android.Manifest.permission#NEARBY_WIFI_DEVICES}
     *
     * @see #EXTRA_WIFI_P2P_INFO
     * @see #EXTRA_NETWORK_INFO
     * @see #EXTRA_WIFI_P2P_GROUP
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_P2P_CONNECTION_CHANGED_ACTION =
        "android.net.wifi.p2p.CONNECTION_STATE_CHANGE";

    /**
     * The lookup key for a {@link android.net.wifi.p2p.WifiP2pInfo} object
     * Retrieve with {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_WIFI_P2P_INFO = "wifiP2pInfo";

    /**
     * The lookup key for a {@link android.net.NetworkInfo} object associated with the
     * p2p network. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_NETWORK_INFO = "networkInfo";

    /**
     * The lookup key for a {@link android.net.wifi.p2p.WifiP2pGroup} object
     * associated with the p2p network. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_WIFI_P2P_GROUP = "p2pGroupInfo";

    /**
     * Broadcast intent action indicating that the available peer list has changed. This
     * can be sent as a result of peers being found, lost or updated.
     *
     * All of these permissions are required to receive this broadcast:
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE} and either
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * {@link android.Manifest.permission#NEARBY_WIFI_DEVICES}
     *
     * <p> An extra {@link #EXTRA_P2P_DEVICE_LIST} provides the full list of
     * current peers. The full list of peers can also be obtained any time with
     * {@link #requestPeers}.
     *
     * @see #EXTRA_P2P_DEVICE_LIST
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_P2P_PEERS_CHANGED_ACTION =
        "android.net.wifi.p2p.PEERS_CHANGED";

     /**
      * The lookup key for a {@link android.net.wifi.p2p.WifiP2pDeviceList} object representing
      * the new peer list when {@link #WIFI_P2P_PEERS_CHANGED_ACTION} broadcast is sent.
      *
      * <p>Retrieve with {@link android.content.Intent#getParcelableExtra(String)}.
      */
    public static final String EXTRA_P2P_DEVICE_LIST = "wifiP2pDeviceList";

    /**
     * Broadcast intent action indicating that peer discovery has either started or stopped.
     * One extra {@link #EXTRA_DISCOVERY_STATE} indicates whether discovery has started
     * or stopped.
     *
     * <p>Note that discovery will be stopped during a connection setup. If the application tries
     * to re-initiate discovery during this time, it can fail.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_P2P_DISCOVERY_CHANGED_ACTION =
        "android.net.wifi.p2p.DISCOVERY_STATE_CHANGE";

    /**
     * The lookup key for an int that indicates whether p2p discovery has started or stopped.
     * Retrieve it with {@link android.content.Intent#getIntExtra(String,int)}.
     *
     * @see #WIFI_P2P_DISCOVERY_STARTED
     * @see #WIFI_P2P_DISCOVERY_STOPPED
     */
    public static final String EXTRA_DISCOVERY_STATE = "discoveryState";

    /** @hide */
    @IntDef({
            WIFI_P2P_DISCOVERY_STOPPED,
            WIFI_P2P_DISCOVERY_STARTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiP2pDiscoveryState {
    }

    /**
     * p2p discovery has stopped
     *
     * @see #WIFI_P2P_DISCOVERY_CHANGED_ACTION
     */
    public static final int WIFI_P2P_DISCOVERY_STOPPED = 1;

    /**
     * p2p discovery has started
     *
     * @see #WIFI_P2P_DISCOVERY_CHANGED_ACTION
     */
    public static final int WIFI_P2P_DISCOVERY_STARTED = 2;

    /**
     * Broadcast intent action indicating that peer listen has either started or stopped.
     * One extra {@link #EXTRA_LISTEN_STATE} indicates whether listen has started or stopped.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_WIFI_P2P_LISTEN_STATE_CHANGED =
            "android.net.wifi.p2p.action.WIFI_P2P_LISTEN_STATE_CHANGED";

    /**
     * The lookup key for an int that indicates whether p2p listen has started or stopped.
     * Retrieve it with {@link android.content.Intent#getIntExtra(String,int)}.
     *
     * @see #WIFI_P2P_LISTEN_STARTED
     * @see #WIFI_P2P_LISTEN_STOPPED
     */
    public static final String EXTRA_LISTEN_STATE = "android.net.wifi.p2p.extra.LISTEN_STATE";

    /** @hide */
    @IntDef({
            WIFI_P2P_LISTEN_STOPPED,
            WIFI_P2P_LISTEN_STARTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiP2pListenState {
    }

    /**
     * p2p listen has stopped
     *
     * @see #ACTION_WIFI_P2P_LISTEN_STATE_CHANGED
     */
    public static final int WIFI_P2P_LISTEN_STOPPED = 1;

    /**
     * p2p listen has started
     *
     * @see #ACTION_WIFI_P2P_LISTEN_STATE_CHANGED
     */
    public static final int WIFI_P2P_LISTEN_STARTED = 2;

    /**
     * Broadcast intent action indicating that this device details have changed.
     *
     * <p> An extra {@link #EXTRA_WIFI_P2P_DEVICE} provides this device details.
     * The valid device details can also be obtained with
     * {@link #requestDeviceInfo(Channel, DeviceInfoListener)} when p2p is enabled.
     * To get information notifications on P2P getting enabled refers
     * {@link #WIFI_P2P_STATE_ENABLED}.
     *
     * <p> The {@link #EXTRA_WIFI_P2P_DEVICE} extra contains an anonymized version of the device's
     * MAC address. Callers holding the {@code android.Manifest.permission#LOCAL_MAC_ADDRESS}
     * permission can use {@link #requestDeviceInfo} to obtain the actual MAC address of this
     * device.
     *
     * All of these permissions are required to receive this broadcast:
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE} and either
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * {@link android.Manifest.permission#NEARBY_WIFI_DEVICES}
     *
     * @see #EXTRA_WIFI_P2P_DEVICE
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_P2P_THIS_DEVICE_CHANGED_ACTION =
        "android.net.wifi.p2p.THIS_DEVICE_CHANGED";

    /**
     * The lookup key for a {@link android.net.wifi.p2p.WifiP2pDevice} object
     * Retrieve with {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_WIFI_P2P_DEVICE = "wifiP2pDevice";

    /**
     * Broadcast intent action indicating that remembered persistent groups have changed.
     *
     * You can <em>not</em> receive this through components declared
     * in manifests, only by explicitly registering for it with
     * {@link android.content.Context#registerReceiver(android.content.BroadcastReceiver,
     * android.content.IntentFilter) Context.registerReceiver()}.
     *
     * @hide
     */
    @SystemApi
    public static final String ACTION_WIFI_P2P_PERSISTENT_GROUPS_CHANGED =
            "android.net.wifi.p2p.action.WIFI_P2P_PERSISTENT_GROUPS_CHANGED";

    /**
     * Broadcast intent action indicating whether or not current connecting
     * request is accepted.
     *
     * The connecting request is initiated by
     * {@link #connect(Channel, WifiP2pConfig, ActionListener)}.
     * <p>The {@link #EXTRA_REQUEST_RESPONSE} extra indicates whether or not current
     * request is accepted or rejected.
     * <p>The {@link #EXTRA_REQUEST_CONFIG} extra indicates the responsed configuration.
     */
    public static final String ACTION_WIFI_P2P_REQUEST_RESPONSE_CHANGED =
            "android.net.wifi.p2p.action.WIFI_P2P_REQUEST_RESPONSE_CHANGED";

    /**
     * The lookup key for the result of a request, true if accepted, false otherwise.
     */
    public static final String EXTRA_REQUEST_RESPONSE =
            "android.net.wifi.p2p.extra.REQUEST_RESPONSE";

    /**
     * The lookup key for the {@link WifiP2pConfig} object of a request.
     */
    public static final String EXTRA_REQUEST_CONFIG =
            "android.net.wifi.p2p.extra.REQUEST_CONFIG";

    /**
     * The lookup key for a handover message returned by the WifiP2pService.
     * @hide
     */
    public static final String EXTRA_HANDOVER_MESSAGE =
            "android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE";

    /**
     * The lookup key for a calling package name from WifiP2pManager
     * @hide
     */
    public static final String CALLING_PACKAGE =
            "android.net.wifi.p2p.CALLING_PACKAGE";

    /**
     * The lookup key for a calling feature id from WifiP2pManager
     * @hide
     */
    public static final String CALLING_FEATURE_ID =
            "android.net.wifi.p2p.CALLING_FEATURE_ID";

    /**
     * The lookup key for a calling package binder from WifiP2pManager
     * @hide
     */
    public static final String CALLING_BINDER =
            "android.net.wifi.p2p.CALLING_BINDER";

    /**
     * Run P2P scan on all channels.
     * @hide
     */
    public static final int WIFI_P2P_SCAN_FULL = 0;

    /**
     * Run P2P scan only on social channels.
     * @hide
     */
    public static final int WIFI_P2P_SCAN_SOCIAL = 1;

    /**
     * Run P2P scan only on a specific channel.
     * @hide
     */
    public static final int WIFI_P2P_SCAN_SINGLE_FREQ = 2;

    /** @hide */
    @IntDef(prefix = {"WIFI_P2P_SCAN_"}, value = {
            WIFI_P2P_SCAN_FULL,
            WIFI_P2P_SCAN_SOCIAL,
            WIFI_P2P_SCAN_SINGLE_FREQ})
    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiP2pScanType {
    }

    /**
     * No channel specified for discover Peers APIs. Let lower layer decide the frequencies to scan
     * based on the WifiP2pScanType.
     * @hide
     */
    public static final int WIFI_P2P_SCAN_FREQ_UNSPECIFIED = 0;

    /**
     * Maximum length in bytes of all vendor specific information elements (IEs) allowed to
     * set during Wi-Fi Direct (P2P) discovery.
     */
    private static final int WIFI_P2P_VENDOR_ELEMENTS_MAXIMUM_LENGTH = 512;

    IWifiP2pManager mService;

    private static final int BASE = Protocol.BASE_WIFI_P2P_MANAGER;

    /** @hide */
    public static final int DISCOVER_PEERS                          = BASE + 1;
    /** @hide */
    public static final int DISCOVER_PEERS_FAILED                   = BASE + 2;
    /** @hide */
    public static final int DISCOVER_PEERS_SUCCEEDED                = BASE + 3;

    /** @hide */
    public static final int STOP_DISCOVERY                          = BASE + 4;
    /** @hide */
    public static final int STOP_DISCOVERY_FAILED                   = BASE + 5;
    /** @hide */
    public static final int STOP_DISCOVERY_SUCCEEDED                = BASE + 6;

    /** @hide */
    public static final int CONNECT                                 = BASE + 7;
    /** @hide */
    public static final int CONNECT_FAILED                          = BASE + 8;
    /** @hide */
    public static final int CONNECT_SUCCEEDED                       = BASE + 9;

    /** @hide */
    public static final int CANCEL_CONNECT                          = BASE + 10;
    /** @hide */
    public static final int CANCEL_CONNECT_FAILED                   = BASE + 11;
    /** @hide */
    public static final int CANCEL_CONNECT_SUCCEEDED                = BASE + 12;

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final int CREATE_GROUP                            = BASE + 13;
    /** @hide */
    public static final int CREATE_GROUP_FAILED                     = BASE + 14;
    /** @hide */
    public static final int CREATE_GROUP_SUCCEEDED                  = BASE + 15;

    /** @hide */
    public static final int REMOVE_GROUP                            = BASE + 16;
    /** @hide */
    public static final int REMOVE_GROUP_FAILED                     = BASE + 17;
    /** @hide */
    public static final int REMOVE_GROUP_SUCCEEDED                  = BASE + 18;

    /** @hide */
    public static final int REQUEST_PEERS                           = BASE + 19;
    /** @hide */
    public static final int RESPONSE_PEERS                          = BASE + 20;

    /** @hide */
    public static final int REQUEST_CONNECTION_INFO                 = BASE + 21;
    /** @hide */
    public static final int RESPONSE_CONNECTION_INFO                = BASE + 22;

    /** @hide */
    public static final int REQUEST_GROUP_INFO                      = BASE + 23;
    /** @hide */
    public static final int RESPONSE_GROUP_INFO                     = BASE + 24;

    /** @hide */
    public static final int ADD_LOCAL_SERVICE                       = BASE + 28;
    /** @hide */
    public static final int ADD_LOCAL_SERVICE_FAILED                = BASE + 29;
    /** @hide */
    public static final int ADD_LOCAL_SERVICE_SUCCEEDED             = BASE + 30;

    /** @hide */
    public static final int REMOVE_LOCAL_SERVICE                    = BASE + 31;
    /** @hide */
    public static final int REMOVE_LOCAL_SERVICE_FAILED             = BASE + 32;
    /** @hide */
    public static final int REMOVE_LOCAL_SERVICE_SUCCEEDED          = BASE + 33;

    /** @hide */
    public static final int CLEAR_LOCAL_SERVICES                    = BASE + 34;
    /** @hide */
    public static final int CLEAR_LOCAL_SERVICES_FAILED             = BASE + 35;
    /** @hide */
    public static final int CLEAR_LOCAL_SERVICES_SUCCEEDED          = BASE + 36;

    /** @hide */
    public static final int ADD_SERVICE_REQUEST                     = BASE + 37;
    /** @hide */
    public static final int ADD_SERVICE_REQUEST_FAILED              = BASE + 38;
    /** @hide */
    public static final int ADD_SERVICE_REQUEST_SUCCEEDED           = BASE + 39;

    /** @hide */
    public static final int REMOVE_SERVICE_REQUEST                  = BASE + 40;
    /** @hide */
    public static final int REMOVE_SERVICE_REQUEST_FAILED           = BASE + 41;
    /** @hide */
    public static final int REMOVE_SERVICE_REQUEST_SUCCEEDED        = BASE + 42;

    /** @hide */
    public static final int CLEAR_SERVICE_REQUESTS                  = BASE + 43;
    /** @hide */
    public static final int CLEAR_SERVICE_REQUESTS_FAILED           = BASE + 44;
    /** @hide */
    public static final int CLEAR_SERVICE_REQUESTS_SUCCEEDED        = BASE + 45;

    /** @hide */
    public static final int DISCOVER_SERVICES                       = BASE + 46;
    /** @hide */
    public static final int DISCOVER_SERVICES_FAILED                = BASE + 47;
    /** @hide */
    public static final int DISCOVER_SERVICES_SUCCEEDED             = BASE + 48;

    /** @hide */
    public static final int PING                                    = BASE + 49;

    /** @hide */
    public static final int RESPONSE_SERVICE                        = BASE + 50;

    /** @hide */
    public static final int SET_DEVICE_NAME                         = BASE + 51;
    /** @hide */
    public static final int SET_DEVICE_NAME_FAILED                  = BASE + 52;
    /** @hide */
    public static final int SET_DEVICE_NAME_SUCCEEDED               = BASE + 53;

    /** @hide */
    public static final int DELETE_PERSISTENT_GROUP                 = BASE + 54;
    /** @hide */
    public static final int DELETE_PERSISTENT_GROUP_FAILED          = BASE + 55;
    /** @hide */
    public static final int DELETE_PERSISTENT_GROUP_SUCCEEDED       = BASE + 56;

    /** @hide */
    public static final int REQUEST_PERSISTENT_GROUP_INFO           = BASE + 57;
    /** @hide */
    public static final int RESPONSE_PERSISTENT_GROUP_INFO          = BASE + 58;

    /** @hide */
    public static final int SET_WFD_INFO                            = BASE + 59;
    /** @hide */
    public static final int SET_WFD_INFO_FAILED                     = BASE + 60;
    /** @hide */
    public static final int SET_WFD_INFO_SUCCEEDED                  = BASE + 61;

    /** @hide */
    public static final int START_WPS                               = BASE + 62;
    /** @hide */
    public static final int START_WPS_FAILED                        = BASE + 63;
    /** @hide */
    public static final int START_WPS_SUCCEEDED                     = BASE + 64;

    /** @hide */
    public static final int START_LISTEN                            = BASE + 65;
    /** @hide */
    public static final int START_LISTEN_FAILED                     = BASE + 66;
    /** @hide */
    public static final int START_LISTEN_SUCCEEDED                  = BASE + 67;

    /** @hide */
    public static final int STOP_LISTEN                             = BASE + 68;
    /** @hide */
    public static final int STOP_LISTEN_FAILED                      = BASE + 69;
    /** @hide */
    public static final int STOP_LISTEN_SUCCEEDED                   = BASE + 70;

    /** @hide */
    public static final int SET_CHANNEL                             = BASE + 71;
    /** @hide */
    public static final int SET_CHANNEL_FAILED                      = BASE + 72;
    /** @hide */
    public static final int SET_CHANNEL_SUCCEEDED                   = BASE + 73;

    /** @hide */
    public static final int GET_HANDOVER_REQUEST                    = BASE + 75;
    /** @hide */
    public static final int GET_HANDOVER_SELECT                     = BASE + 76;
    /** @hide */
    public static final int RESPONSE_GET_HANDOVER_MESSAGE           = BASE + 77;
    /** @hide */
    public static final int INITIATOR_REPORT_NFC_HANDOVER           = BASE + 78;
    /** @hide */
    public static final int RESPONDER_REPORT_NFC_HANDOVER           = BASE + 79;
    /** @hide */
    public static final int REPORT_NFC_HANDOVER_SUCCEEDED           = BASE + 80;
    /** @hide */
    public static final int REPORT_NFC_HANDOVER_FAILED              = BASE + 81;

    /** @hide */
    public static final int FACTORY_RESET                           = BASE + 82;
    /** @hide */
    public static final int FACTORY_RESET_FAILED                    = BASE + 83;
    /** @hide */
    public static final int FACTORY_RESET_SUCCEEDED                 = BASE + 84;

    /** @hide */
    public static final int REQUEST_ONGOING_PEER_CONFIG             = BASE + 85;
    /** @hide */
    public static final int RESPONSE_ONGOING_PEER_CONFIG            = BASE + 86;
    /** @hide */
    public static final int SET_ONGOING_PEER_CONFIG                 = BASE + 87;
    /** @hide */
    public static final int SET_ONGOING_PEER_CONFIG_FAILED          = BASE + 88;
    /** @hide */
    public static final int SET_ONGOING_PEER_CONFIG_SUCCEEDED       = BASE + 89;

    /** @hide */
    public static final int REQUEST_P2P_STATE                       = BASE + 90;
    /** @hide */
    public static final int RESPONSE_P2P_STATE                      = BASE + 91;

    /** @hide */
    public static final int REQUEST_DISCOVERY_STATE                 = BASE + 92;
    /** @hide */
    public static final int RESPONSE_DISCOVERY_STATE                = BASE + 93;

    /** @hide */
    public static final int REQUEST_NETWORK_INFO                    = BASE + 94;
    /** @hide */
    public static final int RESPONSE_NETWORK_INFO                   = BASE + 95;

    /** @hide */
    public static final int UPDATE_CHANNEL_INFO                     = BASE + 96;

    /** @hide */
    public static final int REQUEST_DEVICE_INFO                     = BASE + 97;
    /** @hide */
    public static final int RESPONSE_DEVICE_INFO                    = BASE + 98;

    /** @hide */
    public static final int REMOVE_CLIENT                           = BASE + 99;
    /** @hide */
    public static final int REMOVE_CLIENT_FAILED                    = BASE + 100;
    /** @hide */
    public static final int REMOVE_CLIENT_SUCCEEDED                 = BASE + 101;

    /** @hide */
    public static final int ADD_EXTERNAL_APPROVER                   = BASE + 102;
    /** @hide */
    public static final int EXTERNAL_APPROVER_ATTACH                = BASE + 103;
    /** @hide */
    public static final int EXTERNAL_APPROVER_DETACH                = BASE + 104;
    /** @hide */
    public static final int EXTERNAL_APPROVER_CONNECTION_REQUESTED  = BASE + 105;
    /** @hide */
    public static final int EXTERNAL_APPROVER_PIN_GENERATED         = BASE + 106;

    /** @hide */
    public static final int REMOVE_EXTERNAL_APPROVER                = BASE + 107;
    /** @hide */
    public static final int REMOVE_EXTERNAL_APPROVER_FAILED         = BASE + 108;
    /** @hide */
    public static final int REMOVE_EXTERNAL_APPROVER_SUCCEEDED      = BASE + 109;

    /** @hide */
    public static final int SET_CONNECTION_REQUEST_RESULT           = BASE + 110;
    /** @hide */
    public static final int SET_CONNECTION_REQUEST_RESULT_FAILED    = BASE + 111;
    /** @hide */
    public static final int SET_CONNECTION_REQUEST_RESULT_SUCCEEDED = BASE + 112;

    /** @hide */
    public static final int SET_VENDOR_ELEMENTS                       = BASE + 113;
    /** @hide */
    public static final int SET_VENDOR_ELEMENTS_FAILED                = BASE + 114;
    /** @hide */
    public static final int SET_VENDOR_ELEMENTS_SUCCEEDED             = BASE + 115;

    /** @hide */
    public static final int GET_LISTEN_STATE                          = BASE + 116;
    /** @hide */
    public static final int GET_LISTEN_STATE_FAILED                   = BASE + 117;
    /** @hide */
    public static final int RESPONSE_GET_LISTEN_STATE                 = BASE + 118;

    /**
     * Create a new WifiP2pManager instance. Applications use
     * {@link android.content.Context#getSystemService Context.getSystemService()} to retrieve
     * the standard {@link android.content.Context#WIFI_P2P_SERVICE Context.WIFI_P2P_SERVICE}.
     * @param service the Binder interface
     * @hide - hide this because it takes in a parameter of type IWifiP2pManager, which
     * is a system private class.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    public WifiP2pManager(IWifiP2pManager service) {
        mService = service;
    }

    /**
     * Passed with {@link ActionListener#onFailure}.
     * Indicates that the operation failed due to an internal error.
     */
    public static final int ERROR               = 0;

    /**
     * Passed with {@link ActionListener#onFailure}.
     * Indicates that the operation failed because p2p is unsupported on the device.
     */
    public static final int P2P_UNSUPPORTED     = 1;

    /**
     * Passed with {@link ActionListener#onFailure}.
     * Indicates that the operation failed because the framework is busy and
     * unable to service the request
     */
    public static final int BUSY                = 2;

    /**
     * Passed with {@link ActionListener#onFailure}.
     * Indicates that the {@link #discoverServices} failed because no service
     * requests are added. Use {@link #addServiceRequest} to add a service
     * request.
     */
    public static final int NO_SERVICE_REQUESTS = 3;

    /** Interface for callback invocation when framework channel is lost */
    public interface ChannelListener {
        /**
         * The channel to the framework has been disconnected.
         * Application could try re-initializing using {@link #initialize}
         */
        public void onChannelDisconnected();
    }

    /** Interface for callback invocation on an application action */
    public interface ActionListener {
        /** The operation succeeded */
        public void onSuccess();
        /**
         * The operation failed
         * @param reason The reason for failure could be one of {@link #P2P_UNSUPPORTED},
         * {@link #ERROR} or {@link #BUSY}
         */
        public void onFailure(int reason);
    }

    /** Interface for callback invocation when peer list is available */
    public interface PeerListListener {
        /**
         * The requested peer list is available
         * @param peers List of available peers
         */
        public void onPeersAvailable(WifiP2pDeviceList peers);
    }

    /** Interface for callback invocation when connection info is available */
    public interface ConnectionInfoListener {
        /**
         * The requested connection info is available
         * @param info Wi-Fi p2p connection info
         */
        public void onConnectionInfoAvailable(WifiP2pInfo info);
    }

    /** Interface for callback invocation when group info is available */
    public interface GroupInfoListener {
        /**
         * The requested p2p group info is available
         * @param group Wi-Fi p2p group info
         */
        public void onGroupInfoAvailable(WifiP2pGroup group);
    }

   /**
    * Interface for callback invocation when service discovery response other than
    * Upnp or Bonjour is received
    */
    public interface ServiceResponseListener {

        /**
         * The requested service response is available.
         *
         * @param protocolType protocol type. currently only
         * {@link WifiP2pServiceInfo#SERVICE_TYPE_VENDOR_SPECIFIC}.
         * @param responseData service discovery response data based on the requested
         *  service protocol type. The format depends on the service type.
         * @param srcDevice source device.
         */
        public void onServiceAvailable(int protocolType,
                byte[] responseData, WifiP2pDevice srcDevice);
    }

    /**
     * Interface for callback invocation when Bonjour service discovery response
     * is received
     */
    public interface DnsSdServiceResponseListener {

        /**
         * The requested Bonjour service response is available.
         *
         * <p>This function is invoked when the device with the specified Bonjour
         * registration type returned the instance name.
         * @param instanceName instance name.<br>
         *  e.g) "MyPrinter".
         * @param registrationType <br>
         * e.g) "_ipp._tcp.local."
         * @param srcDevice source device.
         */
        public void onDnsSdServiceAvailable(String instanceName,
                String registrationType, WifiP2pDevice srcDevice);

   }

    /**
     * Interface for callback invocation when Bonjour TXT record is available
     * for a service
     */
   public interface DnsSdTxtRecordListener {
        /**
         * The requested Bonjour service response is available.
         *
         * <p>This function is invoked when the device with the specified full
         * service domain service returned TXT record.
         *
         * @param fullDomainName full domain name. <br>
         * e.g) "MyPrinter._ipp._tcp.local.".
         * @param txtRecordMap TXT record data as a map of key/value pairs
         * @param srcDevice source device.
         */
        public void onDnsSdTxtRecordAvailable(String fullDomainName,
                Map<String, String> txtRecordMap,
                WifiP2pDevice srcDevice);
   }

    /**
     * Interface for callback invocation when upnp service discovery response
     * is received
     * */
    public interface UpnpServiceResponseListener {

        /**
         * The requested upnp service response is available.
         *
         * <p>This function is invoked when the specified device or service is found.
         *
         * @param uniqueServiceNames The list of unique service names.<br>
         * e.g) uuid:6859dede-8574-59ab-9332-123456789012::urn:schemas-upnp-org:device:
         * MediaServer:1
         * @param srcDevice source device.
         */
        public void onUpnpServiceAvailable(List<String> uniqueServiceNames,
                WifiP2pDevice srcDevice);
    }


    /**
     * Interface for callback invocation when stored group info list is available
     *
     * @hide
     */
    @SystemApi
    public interface PersistentGroupInfoListener {
        /**
         * The requested stored p2p group info list is available
         * @param groups Wi-Fi p2p group info list
         */
        void onPersistentGroupInfoAvailable(@NonNull WifiP2pGroupList groups);
    }

    /**
     * Interface for callback invocation when Handover Request or Select Message is available
     * @hide
     */
    public interface HandoverMessageListener {
        public void onHandoverMessageAvailable(String handoverMessage);
    }

    /** Interface for callback invocation when p2p state is available
     *  in response to {@link #requestP2pState}.
     */
    public interface P2pStateListener {
        /**
         * The requested p2p state is available.
         * @param state Wi-Fi p2p state
         *        @see #WIFI_P2P_STATE_DISABLED
         *        @see #WIFI_P2P_STATE_ENABLED
         */
        void onP2pStateAvailable(@WifiP2pState int state);
    }

    /** Interface for callback invocation when p2p state is available
     *  in response to {@link #requestDiscoveryState}.
     */
    public interface DiscoveryStateListener {
        /**
         * The requested p2p discovery state is available.
         * @param state Wi-Fi p2p discovery state
         *        @see #WIFI_P2P_DISCOVERY_STARTED
         *        @see #WIFI_P2P_DISCOVERY_STOPPED
         */
        void onDiscoveryStateAvailable(@WifiP2pDiscoveryState int state);
    }

    /** Interface for callback invocation when p2p state is available
     *  in response to {@link #getListenState}.
     *  @hide
     */
    public interface ListenStateListener {
        /**
         * The requested p2p listen state is available.
         * @param state Wi-Fi p2p listen state
         *        @see #WIFI_P2P_LISTEN_STARTED
         *        @see #WIFI_P2P_LISTEN_STOPPED
         */
        void onListenStateAvailable(@WifiP2pListenState int state);
    }

    /** Interface for callback invocation when {@link android.net.NetworkInfo} is available
     *  in response to {@link #requestNetworkInfo}.
     */
    public interface NetworkInfoListener {
        /**
         * The requested {@link android.net.NetworkInfo} is available
         * @param networkInfo Wi-Fi p2p {@link android.net.NetworkInfo}
         */
        void onNetworkInfoAvailable(@NonNull NetworkInfo networkInfo);
    }

    /**
     * Interface for callback invocation when ongoing peer info is available
     * @hide
     */
    public interface OngoingPeerInfoListener {
        /**
         * The requested ongoing WifiP2pConfig is available
         * @param peerConfig WifiP2pConfig for current connecting session
         */
        void onOngoingPeerAvailable(WifiP2pConfig peerConfig);
    }

    /** Interface for callback invocation when {@link android.net.wifi.p2p.WifiP2pDevice}
     *  is available in response to {@link #requestDeviceInfo(Channel, DeviceInfoListener)}.
     */
    public interface DeviceInfoListener {
        /**
         * The requested {@link android.net.wifi.p2p.WifiP2pDevice} is available.
         * @param wifiP2pDevice Wi-Fi p2p {@link android.net.wifi.p2p.WifiP2pDevice}
         */
        void onDeviceInfoAvailable(@Nullable WifiP2pDevice wifiP2pDevice);
    }

    /**
     * Interface for callback invocation when an incoming request is received.
     *
     * This callback is registered by
     * {@link #addExternalApprover(Channel, MacAddress, ExternalApproverRequestListener)}.
     */
    public interface ExternalApproverRequestListener {
        /**
         * This device received a negotiation request from another peer.
         *
         * Used in {@link #onConnectionRequested(int, WifiP2pConfig, WifiP2pDevice)}.
         */
        int REQUEST_TYPE_NEGOTIATION = 0;
        /**
         * This device received an invitation request from GO to join the group.
         *
         * Used in {@link #onConnectionRequested(int, WifiP2pConfig, WifiP2pDevice)}.
         */
        int REQUEST_TYPE_INVITATION = 1;
        /**
         * This GO device received a request from a peer to join the group.
         *
         * Used in {@link #onConnectionRequested(int, WifiP2pConfig, WifiP2pDevice)}.
         */
        int REQUEST_TYPE_JOIN = 2;
        /** @hide */
        @IntDef(prefix = {"REQUEST_TYPE__"}, value = {
            REQUEST_TYPE_NEGOTIATION,
            REQUEST_TYPE_INVITATION,
            REQUEST_TYPE_JOIN})
        @Retention(RetentionPolicy.SOURCE)
        public @interface RequestType {
        }

        /**
         * Detached by a call to
         * {@link #removeExternalApprover(Channel, MacAddress, ActionListener)}.
         *
         * Used in {@link #onDetached(MacAddress, int)}.
         */
        int APPROVER_DETACH_REASON_REMOVE = 0;
        /**
         * Detached due to a framework failure.
         *
         * Used in {@link #onDetached(MacAddress, int)}.
         */
        int APPROVER_DETACH_REASON_FAILURE = 1;
        /**
         * Detached when a new approver replaces an old one.
         *
         * Used in {@link #onDetached(MacAddress, int)}.
         */
        int APPROVER_DETACH_REASON_REPLACE = 2;
        /**
         * Detached since the {@link WifiP2pManager} channel was closed, e.g.
         * by using {@link Channel#close()} method.
         *
         * Used in {@link #onDetached(MacAddress, int)}.
         */
        int APPROVER_DETACH_REASON_CLOSE = 3;
        /** @hide */
        @IntDef(prefix = {"APPROVER_DETACH_REASON_"}, value = {
            APPROVER_DETACH_REASON_REMOVE,
            APPROVER_DETACH_REASON_FAILURE,
            APPROVER_DETACH_REASON_REPLACE,
            APPROVER_DETACH_REASON_CLOSE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface ApproverDetachReason {
        }

        /**
         * Called when an approver registration via
         * {@link #addExternalApprover(Channel, MacAddress, ExternalApproverRequestListener)}
         * is successful.
         *
         * @param deviceAddress is the peer MAC address used in the registration.
         */
        void onAttached(@NonNull MacAddress deviceAddress);
        /**
         * Called when an approver registration via
         * {@link #addExternalApprover(Channel, MacAddress, ExternalApproverRequestListener)}
         * has failed.
         *
         * @param deviceAddress is the peer MAC address used in the registration.
         * @param reason is the failure reason.
         */
        void onDetached(@NonNull MacAddress deviceAddress, @ApproverDetachReason int reason);
        /**
         * Called when there is an incoming connection request
         * which matches a peer (identified by its {@link MacAddress}) registered by the external
         * approver through
         * {@link #addExternalApprover(Channel, MacAddress, ExternalApproverRequestListener)}.
         * The external approver is expected to follow up with a connection decision using the
         * {@link #setConnectionRequestResult(Channel, MacAddress, int, ActionListener)} with
         * {@link #CONNECTION_REQUEST_ACCEPT}, {@link #CONNECTION_REQUEST_REJECT}, or
         * {@link #CONNECTION_REQUEST_DEFER_TO_SERVICE}.
         *
         * @param requestType is one of {@link #REQUEST_TYPE_NEGOTIATION},
         *        {@link #REQUEST_TYPE_INVITATION}, and {@link #REQUEST_TYPE_JOIN}.
         * @param config is the peer configuration.
         * @param device is the peer information.
         */
        void onConnectionRequested(
                @RequestType int requestType, @NonNull WifiP2pConfig config,
                @NonNull WifiP2pDevice device);
        /**
         * Called when a PIN is generated by the WiFi service.
         *
         * The external approver can display the PIN, exchange the PIN via Out-Of-Band way
         * or ask the wifi service to show the PIN as usual using the
         * {@link #setConnectionRequestResult(Channel, MacAddress, int, ActionListener)}
         * with {@link #CONNECTION_REQUEST_DEFER_SHOW_PIN_TO_SERVICE}.
         *
         * @param deviceAddress is the peer MAC address used in the registration.
         * @param pin is the WPS PIN.
         */
        void onPinGenerated(@NonNull MacAddress deviceAddress, @NonNull String pin);
    }


    /**
     * A channel that connects the application to the Wifi p2p framework.
     * Most p2p operations require a Channel as an argument. An instance of Channel is obtained
     * by doing a call on {@link #initialize}
     */
    public static class Channel implements AutoCloseable {
        /** @hide */
        public Channel(Context context, Looper looper, ChannelListener l, Binder binder,
                WifiP2pManager p2pManager) {
            mAsyncChannel = new AsyncChannel();
            mHandler = new P2pHandler(looper);
            mChannelListener = l;
            mContext = context;
            mBinder = binder;
            mP2pManager = p2pManager;

            mCloseGuard.open("close");
        }
        private final static int INVALID_LISTENER_KEY = 0;
        private final WifiP2pManager mP2pManager;
        private ChannelListener mChannelListener;
        private ServiceResponseListener mServRspListener;
        private DnsSdServiceResponseListener mDnsSdServRspListener;
        private DnsSdTxtRecordListener mDnsSdTxtListener;
        private UpnpServiceResponseListener mUpnpServRspListener;
        private HashMap<Integer, Object> mListenerMap = new HashMap<Integer, Object>();
        private final Object mListenerMapLock = new Object();
        private int mListenerKey = 0;

        private final CloseGuard mCloseGuard = new CloseGuard();

        /**
         * Return the binder object.
         * @hide
         */
        public @NonNull Binder getBinder() {
            return mBinder;
        }

        /**
         * Close the current P2P connection and indicate to the P2P service that connections
         * created by the app can be removed.
         */
        public void close() {
            if (mP2pManager == null) {
                Log.w(TAG, "Channel.close(): Null mP2pManager!?");
            } else {
                try {
                    mP2pManager.mService.close(mBinder);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }

            mAsyncChannel.disconnect();
            mCloseGuard.close();
            Reference.reachabilityFence(this);
        }

        /** @hide */
        @Override
        protected void finalize() throws Throwable {
            try {
                if (mCloseGuard != null) {
                    mCloseGuard.warnIfOpen();
                }

                close();
            } finally {
                super.finalize();
            }
        }

        /* package */ final Binder mBinder;

        @UnsupportedAppUsage
        private AsyncChannel mAsyncChannel;
        private P2pHandler mHandler;
        Context mContext;
        class P2pHandler extends Handler {
            P2pHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                Object listener = null;
                // The listener for an external approver should be
                // removed after detaching from the service.
                switch (message.what) {
                    case EXTERNAL_APPROVER_ATTACH:
                    case EXTERNAL_APPROVER_CONNECTION_REQUESTED:
                    case EXTERNAL_APPROVER_PIN_GENERATED:
                        listener = getListener(message.arg2);
                        break;
                    default:
                        listener = removeListener(message.arg2);
                        break;
                }
                switch (message.what) {
                    case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                        if (mChannelListener != null) {
                            mChannelListener.onChannelDisconnected();
                            mChannelListener = null;
                        }
                        break;
                    /* ActionListeners grouped together */
                    case DISCOVER_PEERS_FAILED:
                    case STOP_DISCOVERY_FAILED:
                    case DISCOVER_SERVICES_FAILED:
                    case CONNECT_FAILED:
                    case CANCEL_CONNECT_FAILED:
                    case CREATE_GROUP_FAILED:
                    case REMOVE_GROUP_FAILED:
                    case ADD_LOCAL_SERVICE_FAILED:
                    case REMOVE_LOCAL_SERVICE_FAILED:
                    case CLEAR_LOCAL_SERVICES_FAILED:
                    case ADD_SERVICE_REQUEST_FAILED:
                    case REMOVE_SERVICE_REQUEST_FAILED:
                    case CLEAR_SERVICE_REQUESTS_FAILED:
                    case SET_DEVICE_NAME_FAILED:
                    case DELETE_PERSISTENT_GROUP_FAILED:
                    case SET_WFD_INFO_FAILED:
                    case START_WPS_FAILED:
                    case START_LISTEN_FAILED:
                    case STOP_LISTEN_FAILED:
                    case GET_LISTEN_STATE_FAILED:
                    case SET_CHANNEL_FAILED:
                    case REPORT_NFC_HANDOVER_FAILED:
                    case FACTORY_RESET_FAILED:
                    case SET_ONGOING_PEER_CONFIG_FAILED:
                    case REMOVE_CLIENT_FAILED:
                    case REMOVE_EXTERNAL_APPROVER_FAILED:
                    case SET_CONNECTION_REQUEST_RESULT_FAILED:
                    case SET_VENDOR_ELEMENTS_FAILED:
                        if (listener != null) {
                            ((ActionListener) listener).onFailure(message.arg1);
                        }
                        break;
                    /* ActionListeners grouped together */
                    case DISCOVER_PEERS_SUCCEEDED:
                    case STOP_DISCOVERY_SUCCEEDED:
                    case DISCOVER_SERVICES_SUCCEEDED:
                    case CONNECT_SUCCEEDED:
                    case CANCEL_CONNECT_SUCCEEDED:
                    case CREATE_GROUP_SUCCEEDED:
                    case REMOVE_GROUP_SUCCEEDED:
                    case ADD_LOCAL_SERVICE_SUCCEEDED:
                    case REMOVE_LOCAL_SERVICE_SUCCEEDED:
                    case CLEAR_LOCAL_SERVICES_SUCCEEDED:
                    case ADD_SERVICE_REQUEST_SUCCEEDED:
                    case REMOVE_SERVICE_REQUEST_SUCCEEDED:
                    case CLEAR_SERVICE_REQUESTS_SUCCEEDED:
                    case SET_DEVICE_NAME_SUCCEEDED:
                    case DELETE_PERSISTENT_GROUP_SUCCEEDED:
                    case SET_WFD_INFO_SUCCEEDED:
                    case START_WPS_SUCCEEDED:
                    case START_LISTEN_SUCCEEDED:
                    case STOP_LISTEN_SUCCEEDED:
                    case SET_CHANNEL_SUCCEEDED:
                    case REPORT_NFC_HANDOVER_SUCCEEDED:
                    case FACTORY_RESET_SUCCEEDED:
                    case SET_ONGOING_PEER_CONFIG_SUCCEEDED:
                    case REMOVE_CLIENT_SUCCEEDED:
                    case REMOVE_EXTERNAL_APPROVER_SUCCEEDED:
                    case SET_CONNECTION_REQUEST_RESULT_SUCCEEDED:
                    case SET_VENDOR_ELEMENTS_SUCCEEDED:
                        if (listener != null) {
                            ((ActionListener) listener).onSuccess();
                        }
                        break;
                    case RESPONSE_PEERS:
                        WifiP2pDeviceList peers = (WifiP2pDeviceList) message.obj;
                        if (listener != null) {
                            ((PeerListListener) listener).onPeersAvailable(peers);
                        }
                        break;
                    case RESPONSE_CONNECTION_INFO:
                        WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) message.obj;
                        if (listener != null) {
                            ((ConnectionInfoListener) listener).onConnectionInfoAvailable(wifiP2pInfo);
                        }
                        break;
                    case RESPONSE_GROUP_INFO:
                        WifiP2pGroup group = (WifiP2pGroup) message.obj;
                        if (listener != null) {
                            ((GroupInfoListener) listener).onGroupInfoAvailable(group);
                        }
                        break;
                    case RESPONSE_SERVICE:
                        WifiP2pServiceResponse resp = (WifiP2pServiceResponse) message.obj;
                        handleServiceResponse(resp);
                        break;
                    case RESPONSE_PERSISTENT_GROUP_INFO:
                        WifiP2pGroupList groups = (WifiP2pGroupList) message.obj;
                        if (listener != null) {
                            ((PersistentGroupInfoListener) listener).
                                onPersistentGroupInfoAvailable(groups);
                        }
                        break;
                    case RESPONSE_GET_HANDOVER_MESSAGE:
                        Bundle handoverBundle = (Bundle) message.obj;
                        if (listener != null) {
                            String handoverMessage = handoverBundle != null
                                    ? handoverBundle.getString(EXTRA_HANDOVER_MESSAGE)
                                    : null;
                            ((HandoverMessageListener) listener)
                                    .onHandoverMessageAvailable(handoverMessage);
                        }
                        break;
                    case RESPONSE_ONGOING_PEER_CONFIG:
                        WifiP2pConfig peerConfig = (WifiP2pConfig) message.obj;
                        if (listener != null) {
                            ((OngoingPeerInfoListener) listener)
                                    .onOngoingPeerAvailable(peerConfig);
                        }
                        break;
                    case RESPONSE_P2P_STATE:
                        if (listener != null) {
                            ((P2pStateListener) listener)
                                    .onP2pStateAvailable(message.arg1);
                        }
                        break;
                    case RESPONSE_DISCOVERY_STATE:
                        if (listener != null) {
                            ((DiscoveryStateListener) listener)
                                    .onDiscoveryStateAvailable(message.arg1);
                        }
                        break;
                    case RESPONSE_GET_LISTEN_STATE:
                        if (listener != null) {
                            ((ListenStateListener) listener)
                                    .onListenStateAvailable(message.arg1);
                        }
                        break;
                    case RESPONSE_NETWORK_INFO:
                        if (listener != null) {
                            ((NetworkInfoListener) listener)
                                    .onNetworkInfoAvailable((NetworkInfo) message.obj);
                        }
                        break;
                    case RESPONSE_DEVICE_INFO:
                        if (listener != null) {
                            ((DeviceInfoListener) listener)
                                    .onDeviceInfoAvailable((WifiP2pDevice) message.obj);
                        }
                        break;
                    case EXTERNAL_APPROVER_ATTACH:
                        if (listener != null) {
                            ((ExternalApproverRequestListener) listener)
                                    .onAttached((MacAddress) message.obj);
                        }
                        break;
                    case EXTERNAL_APPROVER_DETACH:
                        if (listener != null) {
                            ((ExternalApproverRequestListener) listener)
                                    .onDetached((MacAddress) message.obj, message.arg1);
                        }
                        break;
                    case EXTERNAL_APPROVER_CONNECTION_REQUESTED:
                        if (listener != null) {
                            int requestType = message.arg1;
                            Bundle bundle = (Bundle) message.obj;
                            WifiP2pDevice device = bundle.getParcelable(EXTRA_PARAM_KEY_DEVICE);
                            WifiP2pConfig config = bundle.getParcelable(EXTRA_PARAM_KEY_CONFIG);
                            ((ExternalApproverRequestListener) listener)
                                    .onConnectionRequested(requestType, config, device);
                        }
                        break;
                    case EXTERNAL_APPROVER_PIN_GENERATED:
                        if (listener != null) {
                            Bundle bundle = (Bundle) message.obj;
                            MacAddress deviceAddress = bundle.getParcelable(
                                    EXTRA_PARAM_KEY_PEER_ADDRESS);
                            String pin = bundle.getString(EXTRA_PARAM_KEY_WPS_PIN);
                            ((ExternalApproverRequestListener) listener)
                                    .onPinGenerated(deviceAddress, pin);
                        }
                        break;
                    default:
                        Log.d(TAG, "Ignored " + message);
                        break;
                }
            }
        }

        private void handleServiceResponse(WifiP2pServiceResponse resp) {
            if (resp instanceof WifiP2pDnsSdServiceResponse) {
                handleDnsSdServiceResponse((WifiP2pDnsSdServiceResponse)resp);
            } else if (resp instanceof WifiP2pUpnpServiceResponse) {
                if (mUpnpServRspListener != null) {
                    handleUpnpServiceResponse((WifiP2pUpnpServiceResponse)resp);
                }
            } else {
                if (mServRspListener != null) {
                    mServRspListener.onServiceAvailable(resp.getServiceType(),
                            resp.getRawData(), resp.getSrcDevice());
                }
            }
        }

        private void handleUpnpServiceResponse(WifiP2pUpnpServiceResponse resp) {
            mUpnpServRspListener.onUpnpServiceAvailable(resp.getUniqueServiceNames(),
                    resp.getSrcDevice());
        }

        private void handleDnsSdServiceResponse(WifiP2pDnsSdServiceResponse resp) {
            if (resp.getDnsType() == WifiP2pDnsSdServiceInfo.DNS_TYPE_PTR) {
                if (mDnsSdServRspListener != null) {
                    mDnsSdServRspListener.onDnsSdServiceAvailable(
                            resp.getInstanceName(),
                            resp.getDnsQueryName(),
                            resp.getSrcDevice());
                }
            } else if (resp.getDnsType() == WifiP2pDnsSdServiceInfo.DNS_TYPE_TXT) {
                if (mDnsSdTxtListener != null) {
                    mDnsSdTxtListener.onDnsSdTxtRecordAvailable(
                            resp.getDnsQueryName(),
                            resp.getTxtRecord(),
                            resp.getSrcDevice());
                }
            } else {
                Log.e(TAG, "Unhandled resp " + resp);
            }
        }

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private int putListener(Object listener) {
            if (listener == null) return INVALID_LISTENER_KEY;
            int key;
            synchronized (mListenerMapLock) {
                do {
                    key = mListenerKey++;
                } while (key == INVALID_LISTENER_KEY);
                mListenerMap.put(key, listener);
            }
            return key;
        }

        private Object getListener(int key) {
            if (key == INVALID_LISTENER_KEY) return null;
            synchronized (mListenerMapLock) {
                return mListenerMap.get(key);
            }
        }

        private Object removeListener(int key) {
            if (key == INVALID_LISTENER_KEY) return null;
            synchronized (mListenerMapLock) {
                return mListenerMap.remove(key);
            }
        }
    }

    private static void checkChannel(Channel c) {
        if (c == null) throw new IllegalArgumentException("Channel needs to be initialized");
    }

    private static void checkServiceInfo(WifiP2pServiceInfo info) {
        if (info == null) throw new IllegalArgumentException("service info is null");
    }

    private static void checkServiceRequest(WifiP2pServiceRequest req) {
        if (req == null) throw new IllegalArgumentException("service request is null");
    }

    private void checkP2pConfig(WifiP2pConfig c) {
        if (c == null) throw new IllegalArgumentException("config cannot be null");
        if (TextUtils.isEmpty(c.deviceAddress)) {
            throw new IllegalArgumentException("deviceAddress cannot be empty");
        }
    }

    /**
     * Registers the application with the Wi-Fi framework. This function
     * must be the first to be called before any p2p operations are performed.
     *
     * @param srcContext is the context of the source
     * @param srcLooper is the Looper on which the callbacks are receivied
     * @param listener for callback at loss of framework communication. Can be null.
     * @return Channel instance that is necessary for performing any further p2p operations
     */
    public Channel initialize(Context srcContext, Looper srcLooper, ChannelListener listener) {
        Binder binder = new Binder();
        Bundle extras = prepareExtrasBundleWithAttributionSource(srcContext);
        int displayId = Display.DEFAULT_DISPLAY;
        try {
            Display display = srcContext.getDisplay();
            if (display != null) {
                displayId = display.getDisplayId();
            }
        } catch (UnsupportedOperationException e) {
            // an acceptable (per API definition) result of getDisplay - implying there's no display
            // associated with the context
        }
        extras.putInt(EXTRA_PARAM_KEY_DISPLAY_ID, displayId);
        Channel channel = initializeChannel(srcContext, srcLooper, listener,
                getMessenger(binder, srcContext.getOpPackageName(), extras), binder);
        return channel;
    }

    /**
     * Registers the application with the Wi-Fi framework. Enables system-only functionality.
     * @hide
     */
    public Channel initializeInternal(Context srcContext, Looper srcLooper,
                                      ChannelListener listener) {
        return initializeChannel(srcContext, srcLooper, listener, getP2pStateMachineMessenger(),
                null);
    }

    private Message prepareMessage(int what, int arg1, int arg2, Bundle extras, Context context) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = maybeGetAttributionSource(context);
        msg.getData().putBundle(EXTRA_PARAM_KEY_BUNDLE, extras);
        return msg;
    }

    private Bundle prepareExtrasBundle(Channel c) {
        Bundle b = new Bundle();
        b.putBinder(CALLING_BINDER, c.getBinder());
        return b;
    }

    /**
     * Note, this should only be used for Binder calls.
     * Unparcelling an AttributionSource will throw an exception when done outside of a Binder
     * transaction. So don't use this with AsyncChannel since it will throw exception when
     * unparcelling.
     */
    private Bundle prepareExtrasBundleWithAttributionSource(Context context) {
        Bundle bundle = new Bundle();
        if (SdkLevel.isAtLeastS()) {
            bundle.putParcelable(WifiManager.EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                    context.getAttributionSource());
        }
        return bundle;
    }

    private Object maybeGetAttributionSource(Context context) {
        return SdkLevel.isAtLeastS() ? context.getAttributionSource() : null;
    }

    private Channel initializeChannel(Context srcContext, Looper srcLooper,
            ChannelListener listener, Messenger messenger, Binder binder) {
        if (messenger == null) return null;

        Channel c = new Channel(srcContext, srcLooper, listener, binder, this);
        if (c.mAsyncChannel.connectSync(srcContext, c.mHandler, messenger)
                == AsyncChannel.STATUS_SUCCESSFUL) {
            Bundle bundle = new Bundle();
            bundle.putString(CALLING_PACKAGE, c.mContext.getOpPackageName());
            bundle.putString(CALLING_FEATURE_ID, c.mContext.getAttributionTag());
            bundle.putBinder(CALLING_BINDER, binder);
            Message msg = prepareMessage(UPDATE_CHANNEL_INFO, 0, c.putListener(null),
                    bundle, c.mContext);
            c.mAsyncChannel.sendMessage(msg);
            return c;
        } else {
            c.close();
            return null;
        }
    }

    /**
     * Initiate peer discovery. A discovery process involves scanning for available Wi-Fi peers
     * for the purpose of establishing a connection.
     *
     * <p> The function call immediately returns after sending a discovery request
     * to the framework. The application is notified of a success or failure to initiate
     * discovery through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> The discovery remains active until a connection is initiated or
     * a p2p group is formed. Register for {@link #WIFI_P2P_PEERS_CHANGED_ACTION} intent to
     * determine when the framework notifies of a change as peers are discovered.
     *
     * <p> Upon receiving a {@link #WIFI_P2P_PEERS_CHANGED_ACTION} intent, an application
     * can request the list of peers using {@link #requestPeers}.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void discoverPeers(Channel channel, ActionListener listener) {
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        channel.mAsyncChannel.sendMessage(prepareMessage(DISCOVER_PEERS, WIFI_P2P_SCAN_FULL,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Scan only the social channels.
     *
     * A discovery process involves scanning for available Wi-Fi peers
     * for the purpose of establishing a connection.
     *
     * <p> The function call immediately returns after sending a discovery request
     * to the framework. The application is notified of a success or failure to initiate
     * discovery through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> The discovery remains active until a connection is initiated or
     * a p2p group is formed. Register for {@link #WIFI_P2P_PEERS_CHANGED_ACTION} intent to
     * determine when the framework notifies of a change as peers are discovered.
     *
     * <p> Upon receiving a {@link #WIFI_P2P_PEERS_CHANGED_ACTION} intent, an application
     * can request the list of peers using {@link #requestPeers}.
     * <p>
     * The application must have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     * <p>
     * Use {@link #isChannelConstrainedDiscoverySupported()} to determine whether the device
     * supports this feature. If {@link #isChannelConstrainedDiscoverySupported()} return
     * {@code false} then this method will throw {@link UnsupportedOperationException}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void discoverPeersOnSocialChannels(@NonNull Channel channel,
            @Nullable ActionListener listener) {
        if (!isChannelConstrainedDiscoverySupported()) {
            throw new UnsupportedOperationException();
        }
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        channel.mAsyncChannel.sendMessage(prepareMessage(DISCOVER_PEERS, WIFI_P2P_SCAN_SOCIAL,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Scan only a single channel specified by frequency.
     *
     * A discovery process involves scanning for available Wi-Fi peers
     * for the purpose of establishing a connection.
     *
     * <p> The function call immediately returns after sending a discovery request
     * to the framework. The application is notified of a success or failure to initiate
     * discovery through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> The discovery remains active until a connection is initiated or
     * a p2p group is formed. Register for {@link #WIFI_P2P_PEERS_CHANGED_ACTION} intent to
     * determine when the framework notifies of a change as peers are discovered.
     *
     * <p> Upon receiving a {@link #WIFI_P2P_PEERS_CHANGED_ACTION} intent, an application
     * can request the list of peers using {@link #requestPeers}.
     * <p>
     * The application must have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     * <p>
     * Use {@link #isChannelConstrainedDiscoverySupported()} to determine whether the device
     * supports this feature. If {@link #isChannelConstrainedDiscoverySupported()} return
     * {@code false} then this method will throw {@link UnsupportedOperationException}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param frequencyMhz is the frequency of the channel to use for peer discovery.
     * @param listener for callbacks on success or failure.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void discoverPeersOnSpecificFrequency(
            @NonNull Channel channel, int frequencyMhz, @Nullable ActionListener listener) {
        if (!isChannelConstrainedDiscoverySupported()) {
            throw new UnsupportedOperationException();
        }
        checkChannel(channel);
        if (frequencyMhz <= 0) {
            throw new IllegalArgumentException("This frequency must be a positive value.");
        }
        Bundle extras = prepareExtrasBundle(channel);
        extras.putInt(EXTRA_PARAM_KEY_PEER_DISCOVERY_FREQ, frequencyMhz);
        channel.mAsyncChannel.sendMessage(prepareMessage(DISCOVER_PEERS, WIFI_P2P_SCAN_SINGLE_FREQ,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Stop an ongoing peer discovery
     *
     * <p> The function call immediately returns after sending a stop request
     * to the framework. The application is notified of a success or failure to initiate
     * stop through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> If P2P Group is in the process of being created, this call will fail (report failure via
     * {@code listener}. The applicantion should listen to
     * {@link #WIFI_P2P_CONNECTION_CHANGED_ACTION} to ensure the state is not
     * {@link android.net.NetworkInfo.State#CONNECTING} and repeat calling when the state changes.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void stopPeerDiscovery(Channel channel, ActionListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(STOP_DISCOVERY, 0, channel.putListener(listener));
    }

    /**
     * Start a p2p connection to a device with the specified configuration.
     *
     * <p> The function call immediately returns after sending a connection request
     * to the framework. The application is notified of a success or failure to initiate
     * connect through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> An app should use {@link WifiP2pConfig.Builder} to build the configuration
     * for this API, ex. call {@link WifiP2pConfig.Builder#setDeviceAddress(MacAddress)}
     * to set the peer MAC address and {@link WifiP2pConfig.Builder#enablePersistentMode(boolean)}
     * to configure the persistent mode.
     *
     * <p> Register for {@link #WIFI_P2P_CONNECTION_CHANGED_ACTION} intent to
     * determine when the framework notifies of a change in connectivity.
     *
     * <p> If the current device is not part of a p2p group, a connect request initiates
     * a group negotiation with the peer.
     *
     * <p> If the current device is part of an existing p2p group or has created
     * a p2p group with {@link #createGroup}, an invitation to join the group is sent to
     * the peer device.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param config options as described in {@link WifiP2pConfig} class
     * @param listener for callbacks on success or failure. Can be null.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void connect(Channel channel, WifiP2pConfig config, ActionListener listener) {
        checkChannel(channel);
        checkP2pConfig(config);
        Bundle extras = prepareExtrasBundle(channel);
        extras.putParcelable(EXTRA_PARAM_KEY_CONFIG, config);
        channel.mAsyncChannel.sendMessage(prepareMessage(CONNECT, 0, channel.putListener(listener),
                extras, channel.mContext));
    }

    /**
     * Cancel any ongoing p2p group negotiation
     *
     * <p> The function call immediately returns after sending a connection cancellation request
     * to the framework. The application is notified of a success or failure to initiate
     * cancellation through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void cancelConnect(Channel channel, ActionListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(CANCEL_CONNECT, 0, channel.putListener(listener));
    }

    /**
     * Create a p2p group with the current device as the group owner. This essentially creates
     * an access point that can accept connections from legacy clients as well as other p2p
     * devices.
     *
     * <p class="note"><strong>Note:</strong>
     * This function would normally not be used unless the current device needs
     * to form a p2p connection with a legacy client
     *
     * <p> The function call immediately returns after sending a group creation request
     * to the framework. The application is notified of a success or failure to initiate
     * group creation through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> Application can request for the group details with {@link #requestGroupInfo}.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void createGroup(Channel channel, ActionListener listener) {
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        channel.mAsyncChannel.sendMessage(prepareMessage(CREATE_GROUP,
                WifiP2pGroup.NETWORK_ID_PERSISTENT, channel.putListener(listener), extras,
                channel.mContext));
    }

    /**
     * Create a p2p group with the current device as the group owner. This essentially creates
     * an access point that can accept connections from legacy clients as well as other p2p
     * devices.
     *
     * <p> An app should use {@link WifiP2pConfig.Builder} to build the configuration
     * for a group.
     *
     * <p class="note"><strong>Note:</strong>
     * This function would normally not be used unless the current device needs
     * to form a p2p group as a Group Owner and allow peers to join it as either
     * Group Clients or legacy Wi-Fi STAs.
     *
     * <p> The function call immediately returns after sending a group creation request
     * to the framework. The application is notified of a success or failure to initiate
     * group creation through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> Application can request for the group details with {@link #requestGroupInfo}.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}.
     * @param config the configuration of a p2p group.
     * @param listener for callbacks on success or failure. Can be null.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void createGroup(@NonNull Channel channel,
            @Nullable WifiP2pConfig config,
            @Nullable ActionListener listener) {
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        extras.putParcelable(EXTRA_PARAM_KEY_CONFIG, config);
        channel.mAsyncChannel.sendMessage(prepareMessage(CREATE_GROUP, 0,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Remove the current p2p group.
     *
     * <p> The function call immediately returns after sending a group removal request
     * to the framework. The application is notified of a success or failure to initiate
     * group removal through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void removeGroup(Channel channel, ActionListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(REMOVE_GROUP, 0, channel.putListener(listener));
    }

    /**
     * Force p2p to enter listen state.
     *
     * When this API is called, this device will periodically enter LISTENING state until
     * {@link #stopListening(Channel, ActionListener)} or
     * {@link #stopPeerDiscovery(Channel, ActionListener)} are called.
     * While in LISTENING state, this device will dwell at its social channel and respond
     * to probe requests from other Wi-Fi Direct peers.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     * @param channel is the channel created at
     *    {@link #initialize(Context, Looper, ChannelListener)}
     * @param listener for callbacks on success or failure.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void startListening(@NonNull Channel channel, @Nullable ActionListener listener) {
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        channel.mAsyncChannel.sendMessage(prepareMessage(START_LISTEN, 0,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Force p2p to exit listen state.
     *
     * When this API is called, this device will stop entering LISTENING state periodically
     * which is triggered by {@link #startListening(Channel, ActionListener)}.
     * If there are running peer discovery which is triggered by
     * {@link #discoverPeers(Channel, ActionListener)} or running service discovery which is
     * triggered by {@link #discoverServices(Channel, ActionListener)}, they will be stopped
     * as well.
     *
     * @param channel is the channel created at
     *    {@link #initialize(Context, Looper, ChannelListener)}
     * @param listener for callbacks on success or failure.
     */
    public void stopListening(@NonNull Channel channel, @Nullable ActionListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(STOP_LISTEN, 0, channel.putListener(listener));
    }

    /**
     * Set P2P listening and operating channel.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listeningChannel the listening channel's Wifi channel number. e.g. 1, 6, 11.
     * @param operatingChannel the operating channel's Wifi channel number. e.g. 1, 6, 11.
     * @param listener for callbacks on success or failure. Can be null.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public void setWifiP2pChannels(@NonNull Channel channel, int listeningChannel,
            int operatingChannel, @Nullable ActionListener listener) {
        checkChannel(channel);
        Bundle p2pChannels = new Bundle();
        p2pChannels.putInt("lc", listeningChannel);
        p2pChannels.putInt("oc", operatingChannel);
        channel.mAsyncChannel.sendMessage(
                SET_CHANNEL, 0, channel.putListener(listener), p2pChannels);
    }

    /**
     * Start a Wi-Fi Protected Setup (WPS) session.
     *
     * <p> The function call immediately returns after sending a request to start a
     * WPS session. Currently, this is only valid if the current device is running
     * as a group owner to allow any new clients to join the group. The application
     * is notified of a success or failure to initiate WPS through listener callbacks
     * {@link ActionListener#onSuccess} or {@link ActionListener#onFailure}.
     * @hide
     */
    @UnsupportedAppUsage(trackingBug = 185141982)
    public void startWps(Channel channel, WpsInfo wps, ActionListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(START_WPS, 0, channel.putListener(listener), wps);
    }

    /**
     * Register a local service for service discovery. If a local service is registered,
     * the framework automatically responds to a service discovery request from a peer.
     *
     * <p> The function call immediately returns after sending a request to add a local
     * service to the framework. The application is notified of a success or failure to
     * add service through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p>The service information is set through {@link WifiP2pServiceInfo}.<br>
     * or its subclass calls  {@link WifiP2pUpnpServiceInfo#newInstance} or
     *  {@link WifiP2pDnsSdServiceInfo#newInstance} for a Upnp or Bonjour service
     * respectively
     *
     * <p>The service information can be cleared with calls to
     *  {@link #removeLocalService} or {@link #clearLocalServices}.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param servInfo is a local service information.
     * @param listener for callbacks on success or failure. Can be null.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void addLocalService(Channel channel, WifiP2pServiceInfo servInfo,
            ActionListener listener) {
        checkChannel(channel);
        checkServiceInfo(servInfo);
        Bundle extras = prepareExtrasBundle(channel);
        extras.putParcelable(EXTRA_PARAM_KEY_SERVICE_INFO, servInfo);
        channel.mAsyncChannel.sendMessage(prepareMessage(ADD_LOCAL_SERVICE, 0,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Remove a registered local service added with {@link #addLocalService}
     *
     * <p> The function call immediately returns after sending a request to remove a
     * local service to the framework. The application is notified of a success or failure to
     * add service through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param servInfo is the local service information.
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void removeLocalService(Channel channel, WifiP2pServiceInfo servInfo,
            ActionListener listener) {
        checkChannel(channel);
        checkServiceInfo(servInfo);
        channel.mAsyncChannel.sendMessage(
                REMOVE_LOCAL_SERVICE, 0, channel.putListener(listener), servInfo);
    }

    /**
     * Clear all registered local services of service discovery.
     *
     * <p> The function call immediately returns after sending a request to clear all
     * local services to the framework. The application is notified of a success or failure to
     * add service through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void clearLocalServices(Channel channel, ActionListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(CLEAR_LOCAL_SERVICES, 0, channel.putListener(listener));
    }

    /**
     * Register a callback to be invoked on receiving service discovery response.
     * Used only for vendor specific protocol right now. For Bonjour or Upnp, use
     * {@link #setDnsSdResponseListeners} or {@link #setUpnpServiceResponseListener}
     * respectively.
     *
     * <p> see {@link #discoverServices} for the detail.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on receiving service discovery response.
     */
    public void setServiceResponseListener(Channel channel,
            ServiceResponseListener listener) {
        checkChannel(channel);
        channel.mServRspListener = listener;
    }

    /**
     * Register a callback to be invoked on receiving Bonjour service discovery
     * response.
     *
     * <p> see {@link #discoverServices} for the detail.
     *
     * @param channel
     * @param servListener is for listening to a Bonjour service response
     * @param txtListener is for listening to a Bonjour TXT record response
     */
    public void setDnsSdResponseListeners(Channel channel,
            DnsSdServiceResponseListener servListener, DnsSdTxtRecordListener txtListener) {
        checkChannel(channel);
        channel.mDnsSdServRspListener = servListener;
        channel.mDnsSdTxtListener = txtListener;
    }

    /**
     * Register a callback to be invoked on receiving upnp service discovery
     * response.
     *
     * <p> see {@link #discoverServices} for the detail.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on receiving service discovery response.
     */
    public void setUpnpServiceResponseListener(Channel channel,
            UpnpServiceResponseListener listener) {
        checkChannel(channel);
        channel.mUpnpServRspListener = listener;
    }

    /**
     * Initiate service discovery. A discovery process involves scanning for
     * requested services for the purpose of establishing a connection to a peer
     * that supports an available service.
     *
     * <p> The function call immediately returns after sending a request to start service
     * discovery to the framework. The application is notified of a success or failure to initiate
     * discovery through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> The services to be discovered are specified with calls to {@link #addServiceRequest}.
     *
     * <p>The application is notified of the response against the service discovery request
     * through listener callbacks registered by {@link #setServiceResponseListener} or
     * {@link #setDnsSdResponseListeners}, or {@link #setUpnpServiceResponseListener}.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void discoverServices(Channel channel, ActionListener listener) {
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        channel.mAsyncChannel.sendMessage(prepareMessage(DISCOVER_SERVICES, 0,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Add a service discovery request.
     *
     * <p> The function call immediately returns after sending a request to add service
     * discovery request to the framework. The application is notified of a success or failure to
     * add service through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p>After service discovery request is added, you can initiate service discovery by
     * {@link #discoverServices}.
     *
     * <p>The added service requests can be cleared with calls to
     * {@link #removeServiceRequest(Channel, WifiP2pServiceRequest, ActionListener)} or
     * {@link #clearServiceRequests(Channel, ActionListener)}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param req is the service discovery request.
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void addServiceRequest(Channel channel,
            WifiP2pServiceRequest req, ActionListener listener) {
        checkChannel(channel);
        checkServiceRequest(req);
        channel.mAsyncChannel.sendMessage(ADD_SERVICE_REQUEST, 0,
                channel.putListener(listener), req);
    }

    /**
     * Remove a specified service discovery request added with {@link #addServiceRequest}
     *
     * <p> The function call immediately returns after sending a request to remove service
     * discovery request to the framework. The application is notified of a success or failure to
     * add service through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param req is the service discovery request.
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void removeServiceRequest(Channel channel, WifiP2pServiceRequest req,
            ActionListener listener) {
        checkChannel(channel);
        checkServiceRequest(req);
        channel.mAsyncChannel.sendMessage(REMOVE_SERVICE_REQUEST, 0,
                channel.putListener(listener), req);
    }

    /**
     * Clear all registered service discovery requests.
     *
     * <p> The function call immediately returns after sending a request to clear all
     * service discovery requests to the framework. The application is notified of a success
     * or failure to add service through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void clearServiceRequests(Channel channel, ActionListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(CLEAR_SERVICE_REQUESTS,
                0, channel.putListener(listener));
    }

    /**
     * Request the current list of peers.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callback when peer list is available. Can be null.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void requestPeers(Channel channel, PeerListListener listener) {
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        channel.mAsyncChannel.sendMessage(prepareMessage(REQUEST_PEERS, 0,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Request device connection info.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callback when connection info is available. Can be null.
     */
    public void requestConnectionInfo(Channel channel, ConnectionInfoListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(
                REQUEST_CONNECTION_INFO, 0, channel.putListener(listener));
    }

    /**
     * Request p2p group info.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callback when group info is available. Can be null.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void requestGroupInfo(Channel channel, GroupInfoListener listener) {
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        channel.mAsyncChannel.sendMessage(prepareMessage(REQUEST_GROUP_INFO, 0,
                channel.putListener(listener), extras, channel.mContext));
    }

    /**
     * Set p2p device name.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callback when group info is available. Can be null.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public void setDeviceName(@NonNull Channel channel, @NonNull String devName,
            @Nullable ActionListener listener) {
        checkChannel(channel);
        WifiP2pDevice d = new WifiP2pDevice();
        d.deviceName = devName;
        channel.mAsyncChannel.sendMessage(SET_DEVICE_NAME, 0, channel.putListener(listener), d);
    }

    /**
     * Set Wifi Display information.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param wfdInfo the Wifi Display information to set
     * @param listener for callbacks on success or failure. Can be null.
     */
    @RequiresPermission(android.Manifest.permission.CONFIGURE_WIFI_DISPLAY)
    public void setWfdInfo(@NonNull Channel channel, @NonNull WifiP2pWfdInfo wfdInfo,
            @Nullable ActionListener listener) {
        setWFDInfo(channel, wfdInfo, listener);
    }

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @RequiresPermission(android.Manifest.permission.CONFIGURE_WIFI_DISPLAY)
    public void setWFDInfo(@NonNull Channel channel, @NonNull WifiP2pWfdInfo wfdInfo,
            @Nullable ActionListener listener) {
        checkChannel(channel);
        try {
            mService.checkConfigureWifiDisplayPermission();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
        channel.mAsyncChannel.sendMessage(SET_WFD_INFO, 0, channel.putListener(listener), wfdInfo);
    }

    /**
     * Remove the client with the MAC address from the group.
     *
     * <p> The function call immediately returns after sending a client removal request
     * to the framework. The application is notified of a success or failure to initiate
     * client removal through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> The callbacks are triggered on the thread specified when initializing the
     * {@code channel}, see {@link #initialize}.
     * <p>
     * Use {@link #isGroupClientRemovalSupported()} to determine whether the device supports
     * this feature. If {@link #isGroupClientRemovalSupported()} return {@code false} then this
     * method will throw {@link UnsupportedOperationException}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param peerAddress MAC address of the client.
     * @param listener for callbacks on success or failure. Can be null.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public void removeClient(@NonNull Channel channel, @NonNull MacAddress peerAddress,
            @Nullable ActionListener listener) {
        if (!isGroupClientRemovalSupported()) {
            throw new UnsupportedOperationException();
        }
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(
                REMOVE_CLIENT, 0, channel.putListener(listener), peerAddress);
    }


    /**
     * Delete a stored persistent group from the system settings.
     *
     * <p> The function call immediately returns after sending a persistent group removal request
     * to the framework. The application is notified of a success or failure to initiate
     * group removal through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p>The persistent p2p group list stored in the system can be obtained by
     * {@link #requestPersistentGroupInfo(Channel, PersistentGroupInfoListener)} and
     *  a network id can be obtained by {@link WifiP2pGroup#getNetworkId()}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param netId the network id of the p2p group.
     * @param listener for callbacks on success or failure. Can be null.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public void deletePersistentGroup(@NonNull Channel channel, int netId,
            @Nullable ActionListener listener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(
                DELETE_PERSISTENT_GROUP, netId, channel.putListener(listener));
    }

    /**
     * Request a list of all the persistent p2p groups stored in system.
     *
     * <p>The caller must have one of {@link android.Manifest.permission.NETWORK_SETTINGS},
     * {@link android.Manifest.permission.NETWORK_STACK}, and
     * {@link android.Manifest.permission.READ_WIFI_CREDENTIAL}.
     *
     * <p>If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later,
     * the application must have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param channel is the channel created at {@link #initialize}
     * @param listener for callback when persistent group info list is available. Can be null.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.READ_WIFI_CREDENTIAL,
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION}, conditional = true)
    public void requestPersistentGroupInfo(@NonNull Channel channel,
            @Nullable PersistentGroupInfoListener listener) {
        checkChannel(channel);
        Bundle extras = prepareExtrasBundle(channel);
        channel.mAsyncChannel.sendMessage(prepareMessage(REQUEST_PERSISTENT_GROUP_INFO, 0,
                channel.putListener(listener), extras, channel.mContext));
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"MIRACAST_"}, value = {
            MIRACAST_DISABLED,
            MIRACAST_SOURCE,
            MIRACAST_SINK})
    public @interface MiracastMode {}

    /**
     * Miracast is disabled.
     * @hide
     */
    @SystemApi
    public static final int MIRACAST_DISABLED = 0;
    /**
     * Device acts as a Miracast source.
     * @hide
     */
    @SystemApi
    public static final int MIRACAST_SOURCE   = 1;
    /**
     * Device acts as a Miracast sink.
     * @hide
     */
    @SystemApi
    public static final int MIRACAST_SINK     = 2;

    /**
     * Accept the incoming request.
     *
     * Used in {@link #setConnectionRequestResult(Channel, MacAddress, int, ActionListener)}.
     */
    public static final int CONNECTION_REQUEST_ACCEPT = 0;
    /**
     * Reject the incoming request.
     *
     * Used in {@link #setConnectionRequestResult(Channel, MacAddress, int, ActionListener)}.
     */
    public static final int CONNECTION_REQUEST_REJECT = 1;
    /**
     * Defer the decision back to the Wi-Fi service (which will display a dialog to the user).
     *
     * Used in {@link #setConnectionRequestResult(Channel, MacAddress, int, ActionListener)}.
     */
    public static final int CONNECTION_REQUEST_DEFER_TO_SERVICE = 2;
    /**
     * Defer the PIN display to the Wi-Fi service (which will display a dialog to the user).
     *
     * Used in {@link #setConnectionRequestResult(Channel, MacAddress, int, ActionListener)}.
     */
    public static final int CONNECTION_REQUEST_DEFER_SHOW_PIN_TO_SERVICE = 3;
    /** @hide */
    @IntDef(prefix = {"CONNECTION_REQUEST_"}, value = {
        CONNECTION_REQUEST_ACCEPT,
        CONNECTION_REQUEST_REJECT,
        CONNECTION_REQUEST_DEFER_TO_SERVICE,
        CONNECTION_REQUEST_DEFER_SHOW_PIN_TO_SERVICE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionRequestResponse {
    }

    /**
     * This is used to provide information to drivers to optimize performance depending
     * on the current mode of operation.
     * {@link #MIRACAST_DISABLED} - disabled
     * {@link #MIRACAST_SOURCE} - source operation
     * {@link #MIRACAST_SINK} - sink operation
     *
     * As an example, the driver could reduce the channel dwell time during scanning
     * when acting as a source or sink to minimize impact on Miracast.
     *
     * @param mode mode of operation. One of {@link #MIRACAST_DISABLED}, {@link #MIRACAST_SOURCE},
     * or {@link #MIRACAST_SINK}
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.CONFIGURE_WIFI_DISPLAY)
    public void setMiracastMode(@MiracastMode int mode) {
        try {
            mService.setMiracastMode(mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private Messenger getMessenger(@NonNull Binder binder, @Nullable String packageName,
            @NonNull Bundle extras) {
        try {
            return mService.getMessenger(binder, packageName, extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get a reference to P2pStateMachine handler. This is used to establish
     * a priveleged AsyncChannel communication with WifiP2pService.
     *
     * @return Messenger pointing to the WifiP2pService handler
     * @hide
     */
    public Messenger getP2pStateMachineMessenger() {
        try {
            return mService.getP2pStateMachineMessenger();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private long getSupportedFeatures() {
        try {
            return mService.getSupportedFeatures();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean isFeatureSupported(long feature) {
        return (getSupportedFeatures() & feature) == feature;
    }

    /**
     * Check if this device supports setting vendor elements.
     *
     * Gates whether the
     * {@link #setVendorElements(Channel, List, ActionListener)}
     * method is functional on this device.
     *
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public boolean isSetVendorElementsSupported() {
        return isFeatureSupported(FEATURE_SET_VENDOR_ELEMENTS);
    }

    /**
     * Check if this device supports discovery limited to a specific frequency or
     * the social channels.
     *
     * Gates whether
     * {@link #discoverPeersOnSpecificFrequency(Channel, int, ActionListener)} and
     * {@link #discoverPeersOnSocialChannels(Channel, ActionListener)}
     * methods are functional on this device.
     *
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public boolean isChannelConstrainedDiscoverySupported() {
        return isFeatureSupported(FEATURE_FLEXIBLE_DISCOVERY);
    }

    /**
     * Check if this device supports removing clients from a group.
     *
     * Gates whether the
     * {@link #removeClient(Channel, MacAddress, ActionListener)}
     * method is functional on this device.
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public boolean isGroupClientRemovalSupported() {
        return isFeatureSupported(FEATURE_GROUP_CLIENT_REMOVAL);
    }

    /**
     * Checks whether this device, while being a group client, can discover and deliver the group
     * owner's IPv6 link-local address.
     *
     * <p>If this method returns {@code true} and
     * {@link #connect(Channel, WifiP2pConfig, ActionListener)} method is called with
     * {@link WifiP2pConfig} having
     * {@link WifiP2pConfig#GROUP_CLIENT_IP_PROVISIONING_MODE_IPV6_LINK_LOCAL} as the group client
     * IP provisioning mode, then the group owner's IPv6 link-local address will be delivered in the
     * group client via {@link #WIFI_P2P_CONNECTION_CHANGED_ACTION} broadcast intent (i.e, group
     * owner address in {@link #EXTRA_WIFI_P2P_INFO}).
     * If this method returns {@code false}, then IPv6 link-local addresses can still be used, but
     * it is the responsibility of the caller to discover that address in other ways, e.g. using
     * out-of-band communication.
     *
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public boolean isGroupOwnerIPv6LinkLocalAddressProvided() {
        return SdkLevel.isAtLeastT()
                && isFeatureSupported(FEATURE_GROUP_OWNER_IPV6_LINK_LOCAL_ADDRESS_PROVIDED);
    }

    /**
     * Get a handover request message for use in WFA NFC Handover transfer.
     * @hide
     */
    public void getNfcHandoverRequest(Channel c, HandoverMessageListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(GET_HANDOVER_REQUEST, 0, c.putListener(listener));
    }


    /**
     * Get a handover select message for use in WFA NFC Handover transfer.
     * @hide
     */
    public void getNfcHandoverSelect(Channel c, HandoverMessageListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(GET_HANDOVER_SELECT, 0, c.putListener(listener));
    }

    /**
     * @hide
     */
    public void initiatorReportNfcHandover(Channel c, String handoverSelect,
                                              ActionListener listener) {
        checkChannel(c);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_HANDOVER_MESSAGE, handoverSelect);
        c.mAsyncChannel.sendMessage(INITIATOR_REPORT_NFC_HANDOVER, 0,
                c.putListener(listener), bundle);
    }


    /**
     * @hide
     */
    public void responderReportNfcHandover(Channel c, String handoverRequest,
                                              ActionListener listener) {
        checkChannel(c);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_HANDOVER_MESSAGE, handoverRequest);
        c.mAsyncChannel.sendMessage(RESPONDER_REPORT_NFC_HANDOVER, 0,
                c.putListener(listener), bundle);
    }

    /**
     * Removes all saved p2p groups.
     *
     * @param c is the channel created at {@link #initialize}.
     * @param listener for callback on success or failure. Can be null.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void factoryReset(@NonNull Channel c, @Nullable ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(FACTORY_RESET, 0, c.putListener(listener));
    }

    /**
     * Request saved WifiP2pConfig which used for an ongoing peer connection
     *
     * @param c is the channel created at {@link #initialize}
     * @param listener for callback when ongoing peer config updated. Can't be null.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    public void requestOngoingPeerConfig(@NonNull Channel c,
            @NonNull OngoingPeerInfoListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(REQUEST_ONGOING_PEER_CONFIG,
                Binder.getCallingUid(), c.putListener(listener));
    }

     /**
     * Set saved WifiP2pConfig which used for an ongoing peer connection
     *
     * @param c is the channel created at {@link #initialize}
     * @param config used for change an ongoing peer connection
     * @param listener for callback when ongoing peer config updated. Can be null.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    public void setOngoingPeerConfig(@NonNull Channel c, @NonNull WifiP2pConfig config,
            @Nullable ActionListener listener) {
        checkChannel(c);
        checkP2pConfig(config);
        c.mAsyncChannel.sendMessage(SET_ONGOING_PEER_CONFIG, 0,
                c.putListener(listener), config);
    }

    /**
     * Request p2p enabled state.
     *
     * <p> This state indicates whether Wi-Fi p2p is enabled or disabled.
     * The valid value is one of {@link #WIFI_P2P_STATE_DISABLED} or
     * {@link #WIFI_P2P_STATE_ENABLED}. The state is returned using the
     * {@link P2pStateListener} listener.
     *
     * <p> This state is also included in the {@link #WIFI_P2P_STATE_CHANGED_ACTION}
     * broadcast event with extra {@link #EXTRA_WIFI_STATE}.
     *
     * @param c is the channel created at {@link #initialize}.
     * @param listener for callback when p2p state is available.
     */
    public void requestP2pState(@NonNull Channel c,
            @NonNull P2pStateListener listener) {
        checkChannel(c);
        if (listener == null) throw new IllegalArgumentException("This listener cannot be null.");
        c.mAsyncChannel.sendMessage(REQUEST_P2P_STATE, 0, c.putListener(listener));
    }

    /**
     * Request p2p discovery state.
     *
     * <p> This state indicates whether p2p discovery has started or stopped.
     * The valid value is one of {@link #WIFI_P2P_DISCOVERY_STARTED} or
     * {@link #WIFI_P2P_DISCOVERY_STOPPED}. The state is returned using the
     * {@link DiscoveryStateListener} listener.
     *
     * <p> This state is also included in the {@link #WIFI_P2P_DISCOVERY_CHANGED_ACTION}
     * broadcast event with extra {@link #EXTRA_DISCOVERY_STATE}.
     *
     * @param c is the channel created at {@link #initialize}.
     * @param listener for callback when discovery state is available.
     */
    public void requestDiscoveryState(@NonNull Channel c,
            @NonNull DiscoveryStateListener listener) {
        checkChannel(c);
        if (listener == null) throw new IllegalArgumentException("This listener cannot be null.");
        c.mAsyncChannel.sendMessage(REQUEST_DISCOVERY_STATE, 0, c.putListener(listener));
    }

    /**
     * Get p2p listen state.
     *
     * <p> This state indicates whether p2p listen has started or stopped.
     * The valid value is one of {@link #WIFI_P2P_LISTEN_STOPPED} or
     * {@link #WIFI_P2P_LISTEN_STARTED}.
     *
     * <p> This state is also included in the {@link #ACTION_WIFI_P2P_LISTEN_STATE_CHANGED}
     * broadcast event with extra {@link #EXTRA_LISTEN_STATE}.
     *
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param c               It is the channel created at {@link #initialize}.
     * @param executor        The executor on which callback will be invoked.
     * @param resultsCallback A callback that will return listen state
     *                        {@link #WIFI_P2P_LISTEN_STOPPED} or {@link #WIFI_P2P_LISTEN_STARTED}
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void getListenState(@NonNull Channel c, @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Integer> resultsCallback) {
        Objects.requireNonNull(c, "channel cannot be null and needs to be initialized)");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        Bundle extras = prepareExtrasBundle(c);
        c.mAsyncChannel.sendMessage(prepareMessage(GET_LISTEN_STATE, 0,
                c.putListener(new ListenStateListener() {
                    @Override
                    public void onListenStateAvailable(int state) {
                        Binder.clearCallingIdentity();
                        executor.execute(() -> {
                            resultsCallback.accept(state);
                        });
                    }
                }), extras, c.mContext));
    }

    /**
     * Request network info.
     *
     * <p> This method provides the network info in the form of a {@link android.net.NetworkInfo}.
     * {@link android.net.NetworkInfo#isAvailable()} indicates the p2p availability and
     * {@link android.net.NetworkInfo#getDetailedState()} reports the current fine-grained state
     * of the network. This {@link android.net.NetworkInfo} is returned using the
     * {@link NetworkInfoListener} listener.
     *
     * <p> This information is also included in the {@link #WIFI_P2P_CONNECTION_CHANGED_ACTION}
     * broadcast event with extra {@link #EXTRA_NETWORK_INFO}.
     *
     * @param c is the channel created at {@link #initialize}.
     * @param listener for callback when network info is available.
     */
    public void requestNetworkInfo(@NonNull Channel c,
            @NonNull NetworkInfoListener listener) {
        checkChannel(c);
        if (listener == null) throw new IllegalArgumentException("This listener cannot be null.");
        c.mAsyncChannel.sendMessage(REQUEST_NETWORK_INFO, 0, c.putListener(listener));
    }

    /**
     * Request Device Info
     *
     * <p> This method provides the device info
     * in the form of a {@link android.net.wifi.p2p.WifiP2pDevice}.
     * Valid {@link android.net.wifi.p2p.WifiP2pDevice} is returned when p2p is enabled.
     * To get information notifications on P2P getting enabled refers
     * {@link #WIFI_P2P_STATE_ENABLED}.
     *
     * <p> This {@link android.net.wifi.p2p.WifiP2pDevice} is returned using the
     * {@link DeviceInfoListener} listener.
     *
     * <p> {@link android.net.wifi.p2p.WifiP2pDevice#deviceAddress} is only available if the caller
     * holds the {@code android.Manifest.permission#LOCAL_MAC_ADDRESS} permission, and holds the
     * anonymized MAC address (02:00:00:00:00:00) otherwise.
     *
     * <p> This information is also included in the {@link #WIFI_P2P_THIS_DEVICE_CHANGED_ACTION}
     * broadcast event with extra {@link #EXTRA_WIFI_P2P_DEVICE}.
     * <p>
     * If targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later, the application must
     * have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * If targeting an earlier release than {@link android.os.Build.VERSION_CODES#TIRAMISU}, the
     * application must have {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param c is the channel created at {@link #initialize(Context, Looper, ChannelListener)}.
     * @param listener for callback when network info is available.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION
            }, conditional = true)
    public void requestDeviceInfo(@NonNull Channel c, @NonNull DeviceInfoListener listener) {
        checkChannel(c);
        if (listener == null) throw new IllegalArgumentException("This listener cannot be null.");

        Bundle extras = prepareExtrasBundle(c);
        c.mAsyncChannel.sendMessage(prepareMessage(REQUEST_DEVICE_INFO, 0,
                c.putListener(listener), extras, c.mContext));
    }

    /**
     * Set the external approver for a specific peer.
     *
     * This API associates a specific peer with an approver. When an incoming request is received
     * from a peer, an authorization request is routed to the attached approver. The approver then
     * calls {@link #setConnectionRequestResult(Channel, MacAddress, int, ActionListener)} to send
     * the result to the WiFi service. A specific peer (identified by its {@code MacAddress}) can
     * only be attached to a single approver. The previous approver will be detached once a new
     * approver is attached. The approver will also be detached automatically when the channel is
     * closed.
     * <p>
     * When an approver is attached, {@link ExternalApproverRequestListener#onAttached(MacAddress)}
     * is called. When an approver is detached,
     * {@link ExternalApproverRequestListener#onDetached(MacAddress, int)} is called.
     * When an incoming request is received,
     * {@link ExternalApproverRequestListener#onConnectionRequested(int, WifiP2pConfig, WifiP2pDevice)}
     * is called. When a WPS PIN is generated,
     * {@link ExternalApproverRequestListener#onPinGenerated(MacAddress, String)} is called.
     * <p>
     * The application must have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param c is the channel created at {@link #initialize(Context, Looper, ChannelListener)}.
     * @param deviceAddress the peer which is bound to the external approver.
     * @param listener for callback when the framework needs to notify the external approver.
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    public void addExternalApprover(@NonNull Channel c, @NonNull MacAddress deviceAddress,
            @NonNull ExternalApproverRequestListener listener) {
        checkChannel(c);
        if (listener == null) throw new IllegalArgumentException("This listener cannot be null.");
        if (null == deviceAddress) {
            throw new IllegalArgumentException("deviceAddress cannot be empty");
        }

        Bundle extras = prepareExtrasBundle(c);
        extras.putParcelable(EXTRA_PARAM_KEY_PEER_ADDRESS, deviceAddress);
        c.mAsyncChannel.sendMessage(prepareMessage(ADD_EXTERNAL_APPROVER, 0,
                c.putListener(listener), extras, c.mContext));
    }

    /**
     * Remove the external approver for a specific peer.
     *
     * The application must have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param c is the channel created at {@link #initialize(Context, Looper, ChannelListener)}.
     * @param deviceAddress the peer which is bound to the external approver.
     * @param listener for callback on success or failure.
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    public void removeExternalApprover(@NonNull Channel c, @NonNull MacAddress deviceAddress,
            @Nullable ActionListener listener) {
        checkChannel(c);
        if (null == deviceAddress) {
            throw new IllegalArgumentException("deviceAddress cannot be empty");
        }

        Bundle extras = prepareExtrasBundle(c);
        extras.putParcelable(EXTRA_PARAM_KEY_PEER_ADDRESS, deviceAddress);
        c.mAsyncChannel.sendMessage(prepareMessage(REMOVE_EXTERNAL_APPROVER, 0,
                c.putListener(listener), extras, c.mContext));
    }

    /**
     * Set the result for the incoming request from a specific peer.
     *
     * The application must have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param c is the channel created at {@link #initialize(Context, Looper, ChannelListener)}.
     * @param deviceAddress the peer which is bound to the external approver.
     * @param result the response for the incoming request.
     * @param listener for callback on success or failure.
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    public void setConnectionRequestResult(@NonNull Channel c, @NonNull MacAddress deviceAddress,
            @ConnectionRequestResponse int result, @Nullable ActionListener listener) {
        checkChannel(c);
        if (null == deviceAddress) {
            throw new IllegalArgumentException("deviceAddress cannot be empty");
        }

        Bundle extras = prepareExtrasBundle(c);
        extras.putParcelable(EXTRA_PARAM_KEY_PEER_ADDRESS, deviceAddress);
        c.mAsyncChannel.sendMessage(prepareMessage(SET_CONNECTION_REQUEST_RESULT,
                result, c.putListener(listener), extras, c.mContext));
    }

    /**
     * Set the result with PIN for the incoming request from a specific peer.
     *
     * The application must have {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation". If the application does not declare
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param c is the channel created at {@link #initialize(Context, Looper, ChannelListener)}.
     * @param deviceAddress the peer which is bound to the external approver.
     * @param result the response for the incoming request.
     * @param pin the PIN for the incoming request.
     * @param listener for callback on success or failure.
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    public void setConnectionRequestResult(@NonNull Channel c, @NonNull MacAddress deviceAddress,
            @ConnectionRequestResponse int result, @Nullable String pin,
            @Nullable ActionListener listener) {
        checkChannel(c);
        if (null == deviceAddress) {
            throw new IllegalArgumentException("deviceAddress cannot be empty");
        }
        if (result == CONNECTION_REQUEST_ACCEPT && TextUtils.isEmpty(pin)) {
            throw new IllegalArgumentException("PIN cannot be empty for accepting a request");
        }

        Bundle extras = prepareExtrasBundle(c);
        extras.putParcelable(EXTRA_PARAM_KEY_PEER_ADDRESS, deviceAddress);
        extras.putString(EXTRA_PARAM_KEY_WPS_PIN, pin);
        c.mAsyncChannel.sendMessage(prepareMessage(SET_CONNECTION_REQUEST_RESULT,
                result, c.putListener(listener), extras, c.mContext));
    }

    /**
     * Set/Clear vendor specific information elements (VSIEs) to be published during
     * Wi-Fi Direct (P2P) discovery.
     *
     * Once {@link Channel#close()} is called, the vendor information elements will be cleared from
     * framework. The information element format is defined in the IEEE 802.11-2016 spec
     * Table 9-77.
     * <p>
     * To clear the previously set vendor elements, call this API with an empty List.
     * <p>
     * The maximum accumulated length of all VSIEs must be before the limit specified by
     * {@link #getP2pMaxAllowedVendorElementsLengthBytes()}.
     * <p>
     * To publish vendor elements, this API should be called before peer discovery API, ex.
     * {@link #discoverPeers(Channel, ActionListener)}.
     * <p>
     * Use {@link #isSetVendorElementsSupported()} to determine whether the device supports
     * this feature. If {@link #isSetVendorElementsSupported()} return {@code false} then
     * this method will throw {@link UnsupportedOperationException}.
     *
     * @param c is the channel created at {@link #initialize(Context, Looper, ChannelListener)}.
     * @param vendorElements application information as vendor-specific information elements.
     * @param listener for callback when network info is available.
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
            })
    public void setVendorElements(@NonNull Channel c,
            @NonNull List<ScanResult.InformationElement> vendorElements,
            @Nullable ActionListener listener) {
        if (!isSetVendorElementsSupported()) {
            throw new UnsupportedOperationException();
        }
        checkChannel(c);
        int totalBytes = 0;
        for (ScanResult.InformationElement e : vendorElements) {
            if (e.id != ScanResult.InformationElement.EID_VSA) {
                throw new IllegalArgumentException("received InformationElement which is not "
                        + "a Vendor Specific IE (VSIE). VSIEs have an ID = 221.");
            }
            // Length field is 1 byte.
            if (e.bytes == null || e.bytes.length > 0xff) {
                throw new IllegalArgumentException("received InformationElement whose payload "
                        + "size is 0 or greater than 255.");
            }
            // The total bytes of an IE is EID (1 byte) + length (1 byte) + payload length.
            totalBytes += 2 + e.bytes.length;
            if (totalBytes > WIFI_P2P_VENDOR_ELEMENTS_MAXIMUM_LENGTH) {
                throw new IllegalArgumentException("received InformationElement whose total "
                        + "size is greater than " + WIFI_P2P_VENDOR_ELEMENTS_MAXIMUM_LENGTH + ".");
            }
        }
        Bundle extras = prepareExtrasBundle(c);
        extras.putParcelableArrayList(EXTRA_PARAM_KEY_INFORMATION_ELEMENT_LIST,
                new ArrayList<>(vendorElements));
        c.mAsyncChannel.sendMessage(prepareMessage(SET_VENDOR_ELEMENTS, 0,
                c.putListener(listener), extras, c.mContext));
    }

    /**
     * Return the maximum total length (in bytes) of all Vendor specific information
     * elements (VSIEs) which can be set using the
     * {@link #setVendorElements(Channel, List, ActionListener)}.
     *
     * The length is calculated adding the payload length + 2 bytes for each VSIE
     * (2 bytes: 1 byte for type and 1 byte for length).
     */
    public static int getP2pMaxAllowedVendorElementsLengthBytes() {
        return WIFI_P2P_VENDOR_ELEMENTS_MAXIMUM_LENGTH;
    }
}
