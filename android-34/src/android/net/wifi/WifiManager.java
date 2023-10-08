/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.net.wifi;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION;
import static android.Manifest.permission.NEARBY_WIFI_DEVICES;
import static android.Manifest.permission.READ_WIFI_CREDENTIAL;
import static android.Manifest.permission.REQUEST_COMPANION_PROFILE_AUTOMOTIVE_PROJECTION;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.app.ActivityManager;
import android.app.admin.WifiSsidPolicy;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledAfter;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.DhcpInfo;
import android.net.DhcpOption;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkStack;
import android.net.Uri;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.ProvisioningCallback;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.WorkSource;
import android.os.connectivity.WifiActivityEnergyInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.CloseGuard;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.modules.utils.HandlerExecutor;
import com.android.modules.utils.ParceledListSlice;
import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This class provides the primary API for managing all aspects of Wi-Fi
 * connectivity.
 * <p>
 * On releases before {@link android.os.Build.VERSION_CODES#N}, this object
 * should only be obtained from an {@linkplain Context#getApplicationContext()
 * application context}, and not from any other derived context to avoid memory
 * leaks within the calling process.
 * <p>
 * It deals with several categories of items:
 * </p>
 * <ul>
 * <li>The list of configured networks. The list can be viewed and updated, and
 * attributes of individual entries can be modified.</li>
 * <li>The currently active Wi-Fi network, if any. Connectivity can be
 * established or torn down, and dynamic information about the state of the
 * network can be queried.</li>
 * <li>Results of access point scans, containing enough information to make
 * decisions about what access point to connect to.</li>
 * <li>It defines the names of various Intent actions that are broadcast upon
 * any sort of change in Wi-Fi state.
 * </ul>
 * <p>
 * This is the API to use when performing Wi-Fi specific operations. To perform
 * operations that pertain to network connectivity at an abstract level, use
 * {@link android.net.ConnectivityManager}.
 * </p>
 */
@SystemService(Context.WIFI_SERVICE)
public class WifiManager {

    private static final String TAG = "WifiManager";

    /**
     * Local networks should not be modified by B&R since the user may have
     * updated it with the latest configurations.
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.S_V2)
    public static final long NOT_OVERRIDE_EXISTING_NETWORKS_ON_RESTORE = 234793325L;

    // Supplicant error codes:
    /**
     * The error code if there was a problem authenticating.
     * @deprecated This is no longer supported.
     */
    @Deprecated
    public static final int ERROR_AUTHENTICATING = 1;

    /**
     * The reason code if there is no error during authentication.
     * It could also imply that there no authentication in progress,
     * this reason code also serves as a reset value.
     * @deprecated This is no longer supported.
     * @hide
     */
    @Deprecated
    public static final int ERROR_AUTH_FAILURE_NONE = 0;

    /**
     * The reason code if there was a timeout authenticating.
     * @deprecated This is no longer supported.
     * @hide
     */
    @Deprecated
    public static final int ERROR_AUTH_FAILURE_TIMEOUT = 1;

    /**
     * The reason code if there was a wrong password while
     * authenticating.
     * @deprecated This is no longer supported.
     * @hide
     */
    @Deprecated
    public static final int ERROR_AUTH_FAILURE_WRONG_PSWD = 2;

    /**
     * The reason code if there was EAP failure while
     * authenticating.
     * @deprecated This is no longer supported.
     * @hide
     */
    @Deprecated
    public static final int ERROR_AUTH_FAILURE_EAP_FAILURE = 3;

    /** @hide */
    public static final int NETWORK_SUGGESTIONS_MAX_PER_APP_LOW_RAM = 256;

    /** @hide */
    public static final int NETWORK_SUGGESTIONS_MAX_PER_APP_HIGH_RAM = 1024;

    /**
     * Reason code if all of the network suggestions were successfully added or removed.
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_SUCCESS = 0;

    /**
     * Reason code if there was an internal error in the platform while processing the addition or
     * removal of suggestions.
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL = 1;

    /**
     * Reason code if the user has disallowed "android:change_wifi_state" app-ops from the app.
     * @see android.app.AppOpsManager#unsafeCheckOp(String, int, String).
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED = 2;

    /**
     * Reason code if one or more of the network suggestions added already exists in platform's
     * database.
     * Note: this code will not be returned with Android 11 as in-place modification is allowed,
     * please check {@link #addNetworkSuggestions(List)}.
     * @see WifiNetworkSuggestion#equals(Object)
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE = 3;

    /**
     * Reason code if the number of network suggestions provided by the app crosses the max
     * threshold set per app.
     * The framework will reject all suggestions provided by {@link #addNetworkSuggestions(List)} if
     * the total size exceeds the limit.
     * @see #getMaxNumberOfNetworkSuggestionsPerApp()
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP = 4;

    /**
     * Reason code if one or more of the network suggestions removed does not exist in platform's
     * database.
     * The framework won't remove any suggestions if one or more of suggestions provided
     * by {@link #removeNetworkSuggestions(List)} does not exist in database.
     * @see WifiNetworkSuggestion#equals(Object)
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID = 5;

    /**
     * Reason code if one or more of the network suggestions added is not allowed.
     * The framework will reject all suggestions provided by {@link #addNetworkSuggestions(List)}
     * if one or more of them is not allowed.
     * This error may be caused by suggestion is using SIM-based encryption method, but calling app
     * is not carrier privileged.
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED = 6;

    /**
     * Reason code if one or more of the network suggestions added is invalid. Framework will reject
     * all the suggestions in the list.
     * The framework will reject all suggestions provided by {@link #addNetworkSuggestions(List)}
     * if one or more of them is invalid.
     * Please use {@link WifiNetworkSuggestion.Builder} to create network suggestions.
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID = 7;

    /**
     * Reason code if {@link android.os.UserManager#DISALLOW_ADD_WIFI_CONFIG} user restriction
     * is set and calling app is restricted by device admin.
     */
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_RESTRICTED_BY_ADMIN = 8;

    /** @hide */
    @IntDef(prefix = { "STATUS_NETWORK_SUGGESTIONS_" }, value = {
            STATUS_NETWORK_SUGGESTIONS_SUCCESS,
            STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL,
            STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED,
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE,
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP,
            STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID,
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED,
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID,
            STATUS_NETWORK_SUGGESTIONS_ERROR_RESTRICTED_BY_ADMIN,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetworkSuggestionsStatusCode {}

    /**
     * Reason code if suggested network connection attempt failed with an unknown failure.
     */
    public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_UNKNOWN = 0;
    /**
     * Reason code if suggested network connection attempt failed with association failure.
     */
    public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_ASSOCIATION = 1;
    /**
     * Reason code if suggested network connection attempt failed with an authentication failure.
     */
    public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_AUTHENTICATION = 2;
    /**
     * Reason code if suggested network connection attempt failed with an IP provision failure.
     */
    public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_IP_PROVISIONING = 3;

    /** @hide */
    @IntDef(prefix = {"STATUS_SUGGESTION_CONNECTION_FAILURE_"},
            value = {STATUS_SUGGESTION_CONNECTION_FAILURE_UNKNOWN,
                    STATUS_SUGGESTION_CONNECTION_FAILURE_ASSOCIATION,
                    STATUS_SUGGESTION_CONNECTION_FAILURE_AUTHENTICATION,
                    STATUS_SUGGESTION_CONNECTION_FAILURE_IP_PROVISIONING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SuggestionConnectionStatusCode {}

    /**
     * Reason code if local-only network connection attempt failed with an unknown failure.
     */
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_UNKNOWN = 0;
    /**
     * Reason code if local-only network connection attempt failed with association failure.
     */
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_ASSOCIATION = 1;
    /**
     * Reason code if local-only network connection attempt failed with an authentication failure.
     */
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_AUTHENTICATION = 2;
    /**
     * Reason code if local-only network connection attempt failed with an IP provisioning failure.
     */
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_IP_PROVISIONING = 3;
    /**
     * Reason code if local-only network connection attempt failed with AP not in range.
     */
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NOT_FOUND = 4;
    /**
     * Reason code if local-only network connection attempt failed with AP not responding
     */
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NO_RESPONSE = 5;

    /** @hide */
    @IntDef(prefix = {"STATUS_LOCAL_ONLY_CONNECTION_FAILURE_"},
            value = {STATUS_LOCAL_ONLY_CONNECTION_FAILURE_UNKNOWN,
                    STATUS_LOCAL_ONLY_CONNECTION_FAILURE_ASSOCIATION,
                    STATUS_LOCAL_ONLY_CONNECTION_FAILURE_AUTHENTICATION,
                    STATUS_LOCAL_ONLY_CONNECTION_FAILURE_IP_PROVISIONING,
                    STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NOT_FOUND,
                    STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NO_RESPONSE
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LocalOnlyConnectionStatusCode {}

    /**
     * Status code if suggestion approval status is unknown, an App which hasn't made any
     * suggestions will get this code.
     */
    public static final int STATUS_SUGGESTION_APPROVAL_UNKNOWN = 0;

    /**
     * Status code if the calling app is still pending user approval for suggestions.
     */
    public static final int STATUS_SUGGESTION_APPROVAL_PENDING = 1;

    /**
     * Status code if the calling app got the user approval for suggestions.
     */
    public static final int STATUS_SUGGESTION_APPROVAL_APPROVED_BY_USER = 2;

    /**
     * Status code if the calling app suggestions were rejected by the user.
     */
    public static final int STATUS_SUGGESTION_APPROVAL_REJECTED_BY_USER = 3;

    /**
     * Status code if the calling app was approved by virtue of being a carrier privileged app.
     * @see TelephonyManager#hasCarrierPrivileges().
     */
    public static final int STATUS_SUGGESTION_APPROVAL_APPROVED_BY_CARRIER_PRIVILEGE = 4;

    /** @hide */
    @IntDef(prefix = {"STATUS_SUGGESTION_APPROVAL_"},
            value = {STATUS_SUGGESTION_APPROVAL_UNKNOWN,
                    STATUS_SUGGESTION_APPROVAL_PENDING,
                    STATUS_SUGGESTION_APPROVAL_APPROVED_BY_USER,
                    STATUS_SUGGESTION_APPROVAL_REJECTED_BY_USER,
                    STATUS_SUGGESTION_APPROVAL_APPROVED_BY_CARRIER_PRIVILEGE
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SuggestionUserApprovalStatus {}

    /**
     * If one of the removed suggestions is currently connected, that network will be disconnected
     * after a short delay as opposed to immediately (which will be done by
     * {@link #ACTION_REMOVE_SUGGESTION_DISCONNECT}). The {@link ConnectivityManager} may call the
     * {@link NetworkCallback#onLosing(Network, int)} on such networks.
     */
    public static final int ACTION_REMOVE_SUGGESTION_LINGER = 1;

    /**
     * If one of the removed suggestions is currently connected, trigger an immediate disconnect
     * after suggestions removal
     */
    public static final int ACTION_REMOVE_SUGGESTION_DISCONNECT = 2;

    /** @hide */
    @IntDef(prefix = {"ACTION_REMOVE_SUGGESTION_"},
            value = {ACTION_REMOVE_SUGGESTION_LINGER,
                    ACTION_REMOVE_SUGGESTION_DISCONNECT
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionAfterRemovingSuggestion {}

    /**
     * Only available on Android S or later.
     * @hide
     **/
    public static final String EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE =
            "EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE";

    /**
     * Broadcast intent action indicating whether Wi-Fi scanning is currently available.
     * Available extras:
     * - {@link #EXTRA_SCAN_AVAILABLE}
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_WIFI_SCAN_AVAILABILITY_CHANGED =
            "android.net.wifi.action.WIFI_SCAN_AVAILABILITY_CHANGED";

    /**
     * A boolean extra indicating whether scanning is currently available.
     * Sent in the broadcast {@link #ACTION_WIFI_SCAN_AVAILABILITY_CHANGED}.
     * Its value is true if scanning is currently available, false otherwise.
     */
    public static final String EXTRA_SCAN_AVAILABLE = "android.net.wifi.extra.SCAN_AVAILABLE";

    /**
     * Broadcast intent action indicating that the credential of a Wi-Fi network
     * has been changed. One extra provides the ssid of the network. Another
     * extra provides the event type, whether the credential is saved or forgot.
     * @hide
     */
    @SystemApi
    public static final String WIFI_CREDENTIAL_CHANGED_ACTION =
            "android.net.wifi.WIFI_CREDENTIAL_CHANGED";
    /** @hide */
    @SystemApi
    public static final String EXTRA_WIFI_CREDENTIAL_EVENT_TYPE = "et";
    /** @hide */
    @SystemApi
    public static final String EXTRA_WIFI_CREDENTIAL_SSID = "ssid";
    /** @hide */
    @SystemApi
    public static final int WIFI_CREDENTIAL_SAVED = 0;
    /** @hide */
    @SystemApi
    public static final int WIFI_CREDENTIAL_FORGOT = 1;

    /** @hide */
    @SystemApi
    public static final int PASSPOINT_HOME_NETWORK = 0;

    /** @hide */
    @SystemApi
    public static final int PASSPOINT_ROAMING_NETWORK = 1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            API_SCANNING_ENABLED,
            API_WIFI_ENABLED,
            API_SOFT_AP,
            API_TETHERED_HOTSPOT,
            API_AUTOJOIN_GLOBAL,
            API_SET_SCAN_SCHEDULE,
            API_SET_ONE_SHOT_SCREEN_ON_CONNECTIVITY_SCAN_DELAY,
            API_SET_NETWORK_SELECTION_CONFIG,
            API_SET_THIRD_PARTY_APPS_ENABLING_WIFI_CONFIRMATION_DIALOG,
            API_ADD_NETWORK,
            API_UPDATE_NETWORK,
            API_ALLOW_AUTOJOIN,
            API_CONNECT_CONFIG,
            API_CONNECT_NETWORK_ID,
            API_DISABLE_NETWORK,
            API_ENABLE_NETWORK,
            API_FORGET,
            API_SAVE,
            API_START_SCAN,
            API_START_LOCAL_ONLY_HOTSPOT,
            API_P2P_DISCOVER_PEERS,
            API_P2P_DISCOVER_PEERS_ON_SOCIAL_CHANNELS,
            API_P2P_DISCOVER_PEERS_ON_SPECIFIC_FREQUENCY,
            API_P2P_STOP_PEER_DISCOVERY,
            API_P2P_CONNECT,
            API_P2P_CANCEL_CONNECT,
            API_P2P_CREATE_GROUP,
            API_P2P_CREATE_GROUP_P2P_CONFIG,
            API_P2P_REMOVE_GROUP,
            API_P2P_START_LISTENING,
            API_P2P_STOP_LISTENING,
            API_P2P_SET_CHANNELS,
            API_WIFI_SCANNER_START_SCAN,
            API_SET_TDLS_ENABLED,
            API_SET_TDLS_ENABLED_WITH_MAC_ADDRESS
    })
    public @interface ApiType {}

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of {@link WifiScanner#setScanningEnabled(boolean)}
     * @hide
     */
    @SystemApi
    public static final int API_SCANNING_ENABLED = 1;
    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of {@link WifiManager#setWifiEnabled(boolean)} .
     * @hide
     */
    @SystemApi
    public static final int API_WIFI_ENABLED = 2;
    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of {@link WifiManager#startSoftAp(WifiConfiguration)} and
     * {@link WifiManager#stopSoftAp()}.
     * @hide
     */
    @SystemApi
    public static final int API_SOFT_AP = 3;
    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of {@link WifiManager#startTetheredHotspot(SoftApConfiguration)}.
     * @hide
     */
    @SystemApi
    public static final int API_TETHERED_HOTSPOT = 4;
    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of {@link WifiManager#allowAutojoinGlobal(boolean)}.
     * @hide
     */
    @SystemApi
    public static final int API_AUTOJOIN_GLOBAL = 5;
    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of {@link WifiManager#setScreenOnScanSchedule(List)}.
     * @hide
     */
    @SystemApi
    public static final int API_SET_SCAN_SCHEDULE = 6;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of {@link WifiManager#setOneShotScreenOnConnectivityScanDelayMillis(int)}.
     * @hide
     */
    @SystemApi
    public static final int API_SET_ONE_SHOT_SCREEN_ON_CONNECTIVITY_SCAN_DELAY = 7;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#setNetworkSelectionConfig(WifiNetworkSelectionConfig)}
     * @hide
     */
    @SystemApi
    public static final int API_SET_NETWORK_SELECTION_CONFIG = 8;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#setThirdPartyAppEnablingWifiConfirmationDialogEnabled(boolean)}
     * @hide
     */
    @SystemApi
    public static final int API_SET_THIRD_PARTY_APPS_ENABLING_WIFI_CONFIRMATION_DIALOG = 9;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#addNetwork(WifiConfiguration)}
     * @hide
     */
    @SystemApi
    public static final int API_ADD_NETWORK = 10;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#updateNetwork(WifiConfiguration)}
     * @hide
     */
    @SystemApi
    public static final int API_UPDATE_NETWORK = 11;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#allowAutojoin(int, boolean)}
     * @hide
     */
    @SystemApi
    public static final int API_ALLOW_AUTOJOIN = 12;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#connect(WifiConfiguration, ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_CONNECT_CONFIG = 13;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#connect(int, ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_CONNECT_NETWORK_ID = 14;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#disableNetwork(int)}
     * @hide
     */
    @SystemApi
    public static final int API_DISABLE_NETWORK = 15;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#enableNetwork(int, boolean)}
     * @hide
     */
    @SystemApi
    public static final int API_ENABLE_NETWORK = 16;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#forget(int, ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_FORGET = 17;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#save(WifiConfiguration, ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_SAVE = 18;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#startScan()}
     * @hide
     */
    @SystemApi
    public static final int API_START_SCAN = 19;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#startLocalOnlyHotspot(LocalOnlyHotspotCallback, Handler)}
     * @hide
     */
    @SystemApi
    public static final int API_START_LOCAL_ONLY_HOTSPOT = 20;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#discoverPeers(WifiP2pManager.Channel, WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_DISCOVER_PEERS = 21;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#discoverPeersOnSocialChannels(WifiP2pManager.Channel,
     * WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_DISCOVER_PEERS_ON_SOCIAL_CHANNELS = 22;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#discoverPeersOnSpecificFrequency(WifiP2pManager.Channel, int,
     * WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_DISCOVER_PEERS_ON_SPECIFIC_FREQUENCY = 23;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#stopPeerDiscovery(WifiP2pManager.Channel,
     * WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_STOP_PEER_DISCOVERY = 24;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#connect(WifiP2pManager.Channel, WifiP2pConfig,
     * WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_CONNECT = 25;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#cancelConnect(WifiP2pManager.Channel, WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_CANCEL_CONNECT = 26;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#createGroup(WifiP2pManager.Channel, WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_CREATE_GROUP = 27;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#createGroup(WifiP2pManager.Channel, WifiP2pConfig,
     * WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_CREATE_GROUP_P2P_CONFIG = 28;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#removeGroup(WifiP2pManager.Channel, WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_REMOVE_GROUP = 29;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#startListening(WifiP2pManager.Channel, WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_START_LISTENING = 30;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#stopListening(WifiP2pManager.Channel, WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_STOP_LISTENING = 31;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiP2pManager#setWifiP2pChannels(WifiP2pManager.Channel, int, int,
     * WifiP2pManager.ActionListener)}
     * @hide
     */
    @SystemApi
    public static final int API_P2P_SET_CHANNELS = 32;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiScanner#startScan(WifiScanner.ScanSettings, WifiScanner.ScanListener)}
     * @hide
     */
    @SystemApi
    public static final int API_WIFI_SCANNER_START_SCAN = 33;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#setTdlsEnabled(InetAddress, boolean)} and
     * {@link WifiManager#setTdlsEnabled(InetAddress, boolean, Executor, Consumer)}
     * @hide
     */
    @SystemApi
    public static final int API_SET_TDLS_ENABLED = 34;

    /**
     * A constant used in
     * {@link WifiManager#getLastCallerInfoForApi(int, Executor, BiConsumer)}
     * Tracks usage of
     * {@link WifiManager#setTdlsEnabledWithMacAddress(String, boolean)} and
     * {@link WifiManager#setTdlsEnabledWithMacAddress(String, boolean, Executor, Consumer)}
     * @hide
     */
    @SystemApi
    public static final int API_SET_TDLS_ENABLED_WITH_MAC_ADDRESS = 35;

    /**
     * Used internally to keep track of boundary.
     * @hide
     */
    public static final int API_MAX = 36;

    /**
     * Broadcast intent action indicating that a Passpoint provider icon has been received.
     *
     * Included extras:
     * {@link #EXTRA_BSSID_LONG}
     * {@link #EXTRA_FILENAME}
     * {@link #EXTRA_ICON}
     *
     * Receiver Required Permission: android.Manifest.permission.ACCESS_WIFI_STATE
     *
     * <p>Note: The broadcast is only delivered to registered receivers - no manifest registered
     * components will be launched.
     *
     * @hide
     */
    public static final String ACTION_PASSPOINT_ICON = "android.net.wifi.action.PASSPOINT_ICON";
    /**
     * BSSID of an AP in long representation.  The {@link #EXTRA_BSSID} contains BSSID in
     * String representation.
     *
     * Retrieve with {@link android.content.Intent#getLongExtra(String, long)}.
     *
     * @hide
     */
    public static final String EXTRA_BSSID_LONG = "android.net.wifi.extra.BSSID_LONG";
    /**
     * Icon data.
     *
     * Retrieve with {@link android.content.Intent#getParcelableExtra(String)} and cast into
     * {@link android.graphics.drawable.Icon}.
     *
     * @hide
     */
    public static final String EXTRA_ICON = "android.net.wifi.extra.ICON";
    /**
     * Name of a file.
     *
     * Retrieve with {@link android.content.Intent#getStringExtra(String)}.
     *
     * @hide
     */
    public static final String EXTRA_FILENAME = "android.net.wifi.extra.FILENAME";

    /**
     * Broadcast intent action indicating a Passpoint OSU Providers List element has been received.
     *
     * Included extras:
     * {@link #EXTRA_BSSID_LONG}
     * {@link #EXTRA_ANQP_ELEMENT_DATA}
     *
     * Receiver Required Permission: android.Manifest.permission.ACCESS_WIFI_STATE
     *
     * <p>Note: The broadcast is only delivered to registered receivers - no manifest registered
     * components will be launched.
     *
     * @hide
     */
    public static final String ACTION_PASSPOINT_OSU_PROVIDERS_LIST =
            "android.net.wifi.action.PASSPOINT_OSU_PROVIDERS_LIST";
    /**
     * Raw binary data of an ANQP (Access Network Query Protocol) element.
     *
     * Retrieve with {@link android.content.Intent#getByteArrayExtra(String)}.
     *
     * @hide
     */
    public static final String EXTRA_ANQP_ELEMENT_DATA =
            "android.net.wifi.extra.ANQP_ELEMENT_DATA";

    /**
     * Broadcast intent action indicating that a Passpoint Deauth Imminent frame has been received.
     *
     * Included extras:
     * {@link #EXTRA_BSSID_LONG}
     * {@link #EXTRA_ESS}
     * {@link #EXTRA_DELAY}
     * {@link #EXTRA_URL}
     *
     * Receiver Required Permission: android.Manifest.permission.ACCESS_WIFI_STATE
     *
     * <p>Note: The broadcast is only delivered to registered receivers - no manifest registered
     * components will be launched.
     *
     * @hide
     */
    public static final String ACTION_PASSPOINT_DEAUTH_IMMINENT =
            "android.net.wifi.action.PASSPOINT_DEAUTH_IMMINENT";
    /**
     * Flag indicating BSS (Basic Service Set) or ESS (Extended Service Set). This will be set to
     * {@code true} for ESS.
     *
     * Retrieve with {@link android.content.Intent#getBooleanExtra(String, boolean)}.
     *
     * @hide
     */
    public static final String EXTRA_ESS = "android.net.wifi.extra.ESS";
    /**
     * Delay in seconds.
     *
     * Retrieve with {@link android.content.Intent#getIntExtra(String, int)}.
     *
     * @hide
     */
    public static final String EXTRA_DELAY = "android.net.wifi.extra.DELAY";

    /**
     * Broadcast intent action indicating a Passpoint subscription remediation frame has been
     * received.
     *
     * Included extras:
     * {@link #EXTRA_BSSID_LONG}
     * {@link #EXTRA_SUBSCRIPTION_REMEDIATION_METHOD}
     * {@link #EXTRA_URL}
     *
     * Receiver Required Permission: android.Manifest.permission.ACCESS_WIFI_STATE
     *
     * <p>Note: The broadcast is only delivered to registered receivers - no manifest registered
     * components will be launched.
     *
     * @hide
     */
    public static final String ACTION_PASSPOINT_SUBSCRIPTION_REMEDIATION =
            "android.net.wifi.action.PASSPOINT_SUBSCRIPTION_REMEDIATION";
    /**
     * The protocol supported by the subscription remediation server. The possible values are:
     * 0 - OMA DM
     * 1 - SOAP XML SPP
     *
     * Retrieve with {@link android.content.Intent#getIntExtra(String, int)}.
     *
     * @hide
     */
    public static final String EXTRA_SUBSCRIPTION_REMEDIATION_METHOD =
            "android.net.wifi.extra.SUBSCRIPTION_REMEDIATION_METHOD";

    /**
     * Activity Action: Receiver should launch Passpoint OSU (Online Sign Up) view.
     * Included extras:
     *
     * {@link #EXTRA_OSU_NETWORK}: {@link Network} instance associated with OSU AP.
     * {@link #EXTRA_URL}: String representation of a server URL used for OSU process.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PASSPOINT_LAUNCH_OSU_VIEW =
            "android.net.wifi.action.PASSPOINT_LAUNCH_OSU_VIEW";

    /**
     * The lookup key for a {@link android.net.Network} associated with a Passpoint OSU server.
     * Included in the {@link #ACTION_PASSPOINT_LAUNCH_OSU_VIEW} broadcast.
     *
     * Retrieve with {@link android.content.Intent#getParcelableExtra(String)}.
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_OSU_NETWORK = "android.net.wifi.extra.OSU_NETWORK";

    /**
     * String representation of an URL for Passpoint OSU.
     * Included in the {@link #ACTION_PASSPOINT_LAUNCH_OSU_VIEW} broadcast.
     *
     * Retrieve with {@link android.content.Intent#getStringExtra(String)}.
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_URL = "android.net.wifi.extra.URL";

    /**
     * Broadcast intent action indicating that Wi-Fi has been enabled, disabled,
     * enabling, disabling, or unknown. One extra provides this state as an int.
     * Another extra provides the previous state, if available.  No network-related
     * permissions are required to subscribe to this broadcast.
     *
     * <p class="note">This broadcast is not delivered to manifest receivers in
     * applications that target API version 26 or later.
     *
     * @see #EXTRA_WIFI_STATE
     * @see #EXTRA_PREVIOUS_WIFI_STATE
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_STATE_CHANGED_ACTION =
        "android.net.wifi.WIFI_STATE_CHANGED";
    /**
     * The lookup key for an int that indicates whether Wi-Fi is enabled,
     * disabled, enabling, disabling, or unknown.  Retrieve it with
     * {@link android.content.Intent#getIntExtra(String,int)}.
     *
     * @see #WIFI_STATE_DISABLED
     * @see #WIFI_STATE_DISABLING
     * @see #WIFI_STATE_ENABLED
     * @see #WIFI_STATE_ENABLING
     * @see #WIFI_STATE_UNKNOWN
     */
    public static final String EXTRA_WIFI_STATE = "wifi_state";
    /**
     * The previous Wi-Fi state.
     *
     * @see #EXTRA_WIFI_STATE
     */
    public static final String EXTRA_PREVIOUS_WIFI_STATE = "previous_wifi_state";

    /**
     * Wi-Fi is currently being disabled. The state will change to {@link #WIFI_STATE_DISABLED} if
     * it finishes successfully.
     *
     * @see #WIFI_STATE_CHANGED_ACTION
     * @see #getWifiState()
     */
    public static final int WIFI_STATE_DISABLING = 0;
    /**
     * Wi-Fi is disabled.
     *
     * @see #WIFI_STATE_CHANGED_ACTION
     * @see #getWifiState()
     */
    public static final int WIFI_STATE_DISABLED = 1;
    /**
     * Wi-Fi is currently being enabled. The state will change to {@link #WIFI_STATE_ENABLED} if
     * it finishes successfully.
     *
     * @see #WIFI_STATE_CHANGED_ACTION
     * @see #getWifiState()
     */
    public static final int WIFI_STATE_ENABLING = 2;
    /**
     * Wi-Fi is enabled.
     *
     * @see #WIFI_STATE_CHANGED_ACTION
     * @see #getWifiState()
     */
    public static final int WIFI_STATE_ENABLED = 3;
    /**
     * Wi-Fi is in an unknown state. This state will occur when an error happens while enabling
     * or disabling.
     *
     * @see #WIFI_STATE_CHANGED_ACTION
     * @see #getWifiState()
     */
    public static final int WIFI_STATE_UNKNOWN = 4;

    /**
     * Broadcast intent action indicating that Wi-Fi AP has been enabled, disabled,
     * enabling, disabling, or failed.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public static final String WIFI_AP_STATE_CHANGED_ACTION =
        "android.net.wifi.WIFI_AP_STATE_CHANGED";

    /**
     * The lookup key for an int that indicates whether Wi-Fi AP is enabled,
     * disabled, enabling, disabling, or failed.  Retrieve it with
     * {@link android.content.Intent#getIntExtra(String,int)}.
     *
     * @see #WIFI_AP_STATE_DISABLED
     * @see #WIFI_AP_STATE_DISABLING
     * @see #WIFI_AP_STATE_ENABLED
     * @see #WIFI_AP_STATE_ENABLING
     * @see #WIFI_AP_STATE_FAILED
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";

    /**
     * An extra containing the int error code for Soft AP start failure.
     * Can be obtained from the {@link #WIFI_AP_STATE_CHANGED_ACTION} using
     * {@link android.content.Intent#getIntExtra}.
     * This extra will only be attached if {@link #EXTRA_WIFI_AP_STATE} is
     * attached and is equal to {@link #WIFI_AP_STATE_FAILED}.
     *
     * The error code will be one of:
     * {@link #SAP_START_FAILURE_GENERAL},
     * {@link #SAP_START_FAILURE_NO_CHANNEL},
     * {@link #SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}
     * {@link #SAP_START_FAILURE_USER_REJECTED}
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_WIFI_AP_FAILURE_REASON =
            "android.net.wifi.extra.WIFI_AP_FAILURE_REASON";
    /**
     * The previous Wi-Fi state.
     *
     * @see #EXTRA_WIFI_AP_STATE
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = "previous_wifi_state";
    /**
     * The lookup key for a String extra that stores the interface name used for the Soft AP.
     * This extra is included in the broadcast {@link #WIFI_AP_STATE_CHANGED_ACTION}.
     * Retrieve its value with {@link android.content.Intent#getStringExtra(String)}.
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_WIFI_AP_INTERFACE_NAME =
            "android.net.wifi.extra.WIFI_AP_INTERFACE_NAME";
    /**
     * The lookup key for an int extra that stores the intended IP mode for this Soft AP.
     * One of {@link #IFACE_IP_MODE_TETHERED} or {@link #IFACE_IP_MODE_LOCAL_ONLY}.
     * This extra is included in the broadcast {@link #WIFI_AP_STATE_CHANGED_ACTION}.
     * Retrieve its value with {@link android.content.Intent#getIntExtra(String, int)}.
     *
     * @hide
     */
    @SystemApi
    public static final String EXTRA_WIFI_AP_MODE = "android.net.wifi.extra.WIFI_AP_MODE";

    /** @hide */
    @IntDef(flag = false, prefix = { "WIFI_AP_STATE_" }, value = {
        WIFI_AP_STATE_DISABLING,
        WIFI_AP_STATE_DISABLED,
        WIFI_AP_STATE_ENABLING,
        WIFI_AP_STATE_ENABLED,
        WIFI_AP_STATE_FAILED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiApState {}

    /**
     * Wi-Fi AP is currently being disabled. The state will change to
     * {@link #WIFI_AP_STATE_DISABLED} if it finishes successfully.
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiApState()
     *
     * @hide
     */
    @SystemApi
    public static final int WIFI_AP_STATE_DISABLING = 10;
    /**
     * Wi-Fi AP is disabled.
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiState()
     *
     * @hide
     */
    @SystemApi
    public static final int WIFI_AP_STATE_DISABLED = 11;
    /**
     * Wi-Fi AP is currently being enabled. The state will change to
     * {@link #WIFI_AP_STATE_ENABLED} if it finishes successfully.
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiApState()
     *
     * @hide
     */
    @SystemApi
    public static final int WIFI_AP_STATE_ENABLING = 12;
    /**
     * Wi-Fi AP is enabled.
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiApState()
     *
     * @hide
     */
    @SystemApi
    public static final int WIFI_AP_STATE_ENABLED = 13;
    /**
     * Wi-Fi AP is in a failed state. This state will occur when an error occurs during
     * enabling or disabling
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiApState()
     *
     * @hide
     */
    @SystemApi
    public static final int WIFI_AP_STATE_FAILED = 14;

    /** @hide */
    @IntDef(flag = false, prefix = { "SAP_START_FAILURE_" }, value = {
        SAP_START_FAILURE_GENERAL,
        SAP_START_FAILURE_NO_CHANNEL,
        SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION,
        SAP_START_FAILURE_USER_REJECTED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SapStartFailure {}

    /**
     *  All other reasons for AP start failure besides {@link #SAP_START_FAILURE_NO_CHANNEL},
     *  {@link #SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION}, and
     *  {@link #SAP_START_FAILURE_USER_REJECTED}.
     *
     *  @hide
     */
    @SystemApi
    public static final int SAP_START_FAILURE_GENERAL= 0;

    /**
     *  If Wi-Fi AP start failed, this reason code means that no legal channel exists on user
     *  selected band due to regulatory constraints.
     *
     *  @hide
     */
    @SystemApi
    public static final int SAP_START_FAILURE_NO_CHANNEL = 1;

    /**
     *  If Wi-Fi AP start failed, this reason code means that the specified configuration
     *  is not supported by the current HAL version.
     *
     *  @hide
     */
    @SystemApi
    public static final int SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION = 2;

    /**
     *  If Wi-Fi AP start failed, this reason code means that the user was asked for confirmation to
     *  create the AP and the user declined.
     *
     *  @hide
     */
    @SystemApi
    public static final int SAP_START_FAILURE_USER_REJECTED = 3;

    /** @hide */
    @IntDef(flag = false, prefix = { "SAP_CLIENT_BLOCKED_REASON_" }, value = {
        SAP_CLIENT_BLOCK_REASON_CODE_BLOCKED_BY_USER,
        SAP_CLIENT_BLOCK_REASON_CODE_NO_MORE_STAS,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SapClientBlockedReason {}

    /**
     *  If Soft Ap client is blocked, this reason code means that client doesn't exist in the
     *  specified configuration {@link SoftApConfiguration.Builder#setBlockedClientList(List)}
     *  and {@link SoftApConfiguration.Builder#setAllowedClientList(List)}
     *  and the {@link SoftApConfiguration.Builder#setClientControlByUserEnabled(boolean)}
     *  is configured as well.
     *  @hide
     */
    @SystemApi
    public static final int SAP_CLIENT_BLOCK_REASON_CODE_BLOCKED_BY_USER = 0;

    /**
     *  If Soft Ap client is blocked, this reason code means that no more clients can be
     *  associated to this AP since it reached maximum capacity. The maximum capacity is
     *  the minimum of {@link SoftApConfiguration.Builder#setMaxNumberOfClients(int)} and
     *  {@link SoftApCapability#getMaxSupportedClients} which get from
     *  {@link WifiManager.SoftApCallback#onCapabilityChanged(SoftApCapability)}.
     *
     *  @hide
     */
    @SystemApi
    public static final int SAP_CLIENT_BLOCK_REASON_CODE_NO_MORE_STAS = 1;

    /**
     * Client disconnected for unspecified reason. This could for example be because the AP is being
     * shut down.
     * @hide
     */
    public static final int SAP_CLIENT_DISCONNECT_REASON_CODE_UNSPECIFIED = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"IFACE_IP_MODE_"}, value = {
            IFACE_IP_MODE_UNSPECIFIED,
            IFACE_IP_MODE_CONFIGURATION_ERROR,
            IFACE_IP_MODE_TETHERED,
            IFACE_IP_MODE_LOCAL_ONLY})
    public @interface IfaceIpMode {}

    /**
     * Interface IP mode unspecified.
     *
     * @see #updateInterfaceIpState(String, int)
     *
     * @hide
     */
    @SystemApi
    public static final int IFACE_IP_MODE_UNSPECIFIED = -1;

    /**
     * Interface IP mode for configuration error.
     *
     * @see #updateInterfaceIpState(String, int)
     *
     * @hide
     */
    @SystemApi
    public static final int IFACE_IP_MODE_CONFIGURATION_ERROR = 0;

    /**
     * Interface IP mode for tethering.
     *
     * @see #updateInterfaceIpState(String, int)
     *
     * @hide
     */
    @SystemApi
    public static final int IFACE_IP_MODE_TETHERED = 1;

    /**
     * Interface IP mode for Local Only Hotspot.
     *
     * @see #updateInterfaceIpState(String, int)
     *
     * @hide
     */
    @SystemApi
    public static final int IFACE_IP_MODE_LOCAL_ONLY = 2;

    /**
     * Broadcast intent action indicating that the wifi network settings
     * had been reset.
     *
     * Note: This intent is sent as a directed broadcast to each manifest registered receiver.
     * Intent will not be received by dynamically registered receivers.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_CARRIER_PROVISIONING)
    public static final String ACTION_NETWORK_SETTINGS_RESET =
            "android.net.wifi.action.NETWORK_SETTINGS_RESET";

    /**
     * Broadcast intent action indicating that the wifi network profiles provisioned
     * may need refresh.
     *
     * Note: This intent is sent as a directed broadcast to each manifest registered receiver;
     * And restricted to those apps which have the NETWORK_CARRIER_PROVISIONING permission.
     * Intent will not be received by dynamically registered receivers.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_CARRIER_PROVISIONING)
    public static final String ACTION_REFRESH_USER_PROVISIONING =
            "android.net.wifi.action.REFRESH_USER_PROVISIONING";

    /**
     * Broadcast intent action indicating that a connection to the supplicant has
     * been established (and it is now possible
     * to perform Wi-Fi operations) or the connection to the supplicant has been
     * lost. One extra provides the connection state as a boolean, where {@code true}
     * means CONNECTED.
     * @deprecated This is no longer supported.
     * @see #EXTRA_SUPPLICANT_CONNECTED
     */
    @Deprecated
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String SUPPLICANT_CONNECTION_CHANGE_ACTION =
        "android.net.wifi.supplicant.CONNECTION_CHANGE";
    /**
     * The lookup key for a boolean that indicates whether a connection to
     * the supplicant daemon has been gained or lost. {@code true} means
     * a connection now exists.
     * Retrieve it with {@link android.content.Intent#getBooleanExtra(String,boolean)}.
     * @deprecated This is no longer supported.
     */
    @Deprecated
    public static final String EXTRA_SUPPLICANT_CONNECTED = "connected";
    /**
     * Broadcast intent action indicating that the state of Wi-Fi connectivity
     * has changed. An extra provides the new state
     * in the form of a {@link android.net.NetworkInfo} object.  No network-related
     * permissions are required to subscribe to this broadcast.
     *
     * <p class="note">This broadcast is not delivered to manifest receivers in
     * applications that target API version 26 or later.
     * @see #EXTRA_NETWORK_INFO
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String NETWORK_STATE_CHANGED_ACTION = "android.net.wifi.STATE_CHANGE";
    /**
     * The lookup key for a {@link android.net.NetworkInfo} object associated with the
     * Wi-Fi network. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    /**
     * The lookup key for a String giving the BSSID of the access point to which
     * we are connected. No longer used.
     */
    @Deprecated
    public static final String EXTRA_BSSID = "bssid";
    /**
     * The lookup key for a {@link android.net.wifi.WifiInfo} object giving the
     * information about the access point to which we are connected.
     * No longer used.
     */
    @Deprecated
    public static final String EXTRA_WIFI_INFO = "wifiInfo";
    /**
     * Broadcast intent action indicating that the state of establishing a connection to
     * an access point has changed.One extra provides the new
     * {@link SupplicantState}. Note that the supplicant state is Wi-Fi specific, and
     * is not generally the most useful thing to look at if you are just interested in
     * the overall state of connectivity.
     * @see #EXTRA_NEW_STATE
     * @see #EXTRA_SUPPLICANT_ERROR
     * @deprecated This is no longer supported.
     */
    @Deprecated
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String SUPPLICANT_STATE_CHANGED_ACTION =
        "android.net.wifi.supplicant.STATE_CHANGE";
    /**
     * The lookup key for a {@link SupplicantState} describing the new state
     * Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     * @deprecated This is no longer supported.
     */
    @Deprecated
    public static final String EXTRA_NEW_STATE = "newState";

    /**
     * The lookup key for a {@link SupplicantState} describing the supplicant
     * error code if any
     * Retrieve with
     * {@link android.content.Intent#getIntExtra(String, int)}.
     * @see #ERROR_AUTHENTICATING
     * @deprecated This is no longer supported.
     */
    @Deprecated
    public static final String EXTRA_SUPPLICANT_ERROR = "supplicantError";

    /**
     * The lookup key for a {@link SupplicantState} describing the supplicant
     * error reason if any
     * Retrieve with
     * {@link android.content.Intent#getIntExtra(String, int)}.
     * @see #ERROR_AUTH_FAILURE_#REASON_CODE
     * @deprecated This is no longer supported.
     * @hide
     */
    @Deprecated
    public static final String EXTRA_SUPPLICANT_ERROR_REASON = "supplicantErrorReason";

    /**
     * Broadcast intent action indicating that the configured networks changed.
     * This can be as a result of adding/updating/deleting a network.
     * <br />
     * {@link #EXTRA_CHANGE_REASON} contains whether the configuration was added/changed/removed.
     * {@link #EXTRA_WIFI_CONFIGURATION} is never set beginning in
     * {@link android.os.Build.VERSION_CODES#R}.
     * {@link #EXTRA_MULTIPLE_NETWORKS_CHANGED} is set for backwards compatibility reasons, but
     * its value is always true beginning in {@link android.os.Build.VERSION_CODES#R}, even if only
     * a single network changed.
     * <br />
     * The {@link android.Manifest.permission#ACCESS_WIFI_STATE ACCESS_WIFI_STATE} permission is
     * required to receive this broadcast.
     *
     * @hide
     */
    @SystemApi
    public static final String CONFIGURED_NETWORKS_CHANGED_ACTION =
        "android.net.wifi.CONFIGURED_NETWORKS_CHANGE";
    /**
     * The lookup key for a {@link android.net.wifi.WifiConfiguration} object representing
     * the changed Wi-Fi configuration when the {@link #CONFIGURED_NETWORKS_CHANGED_ACTION}
     * broadcast is sent.
     * @deprecated This extra is never set beginning in {@link android.os.Build.VERSION_CODES#R},
     * regardless of the target SDK version. Use {@link #getConfiguredNetworks} to get the full list
     * of configured networks.
     * @hide
     */
    @Deprecated
    @SystemApi
    public static final String EXTRA_WIFI_CONFIGURATION = "wifiConfiguration";
    /**
     * Multiple network configurations have changed.
     * @see #CONFIGURED_NETWORKS_CHANGED_ACTION
     * @deprecated This extra's value is always true beginning in
     * {@link android.os.Build.VERSION_CODES#R}, regardless of the target SDK version.
     * @hide
     */
    @Deprecated
    @SystemApi
    public static final String EXTRA_MULTIPLE_NETWORKS_CHANGED = "multipleChanges";
    /**
     * The lookup key for an integer indicating the reason a Wi-Fi network configuration
     * has changed. One of {@link #CHANGE_REASON_ADDED}, {@link #CHANGE_REASON_REMOVED},
     * {@link #CHANGE_REASON_CONFIG_CHANGE}.
     *
     * @see #CONFIGURED_NETWORKS_CHANGED_ACTION
     * @hide
     */
    @SystemApi
    public static final String EXTRA_CHANGE_REASON = "changeReason";
    /**
     * The configuration is new and was added.
     * @hide
     */
    @SystemApi
    public static final int CHANGE_REASON_ADDED = 0;
    /**
     * The configuration was removed and is no longer present in the system's list of
     * configured networks.
     * @hide
     */
    @SystemApi
    public static final int CHANGE_REASON_REMOVED = 1;
    /**
     * The configuration has changed as a result of explicit action or because the system
     * took an automated action such as disabling a malfunctioning configuration.
     * @hide
     */
    @SystemApi
    public static final int CHANGE_REASON_CONFIG_CHANGE = 2;
    /**
     * An access point scan has completed, and results are available.
     * Call {@link #getScanResults()} to obtain the results.
     * The broadcast intent may contain an extra field with the key {@link #EXTRA_RESULTS_UPDATED}
     * and a {@code boolean} value indicating if the scan was successful.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String SCAN_RESULTS_AVAILABLE_ACTION = "android.net.wifi.SCAN_RESULTS";

    /**
     * Lookup key for a {@code boolean} extra in intent {@link #SCAN_RESULTS_AVAILABLE_ACTION}
     * representing if the scan was successful or not.
     * Scans may fail for multiple reasons, these may include:
     * <ol>
     * <li>An app requested too many scans in a certain period of time.
     * This may lead to additional scan request rejections via "scan throttling" for both
     * foreground and background apps.
     * Note: Apps holding android.Manifest.permission.NETWORK_SETTINGS permission are
     * exempted from scan throttling.
     * </li>
     * <li>The device is idle and scanning is disabled.</li>
     * <li>Wifi hardware reported a scan failure.</li>
     * </ol>
     * @return true scan was successful, results are updated
     * @return false scan was not successful, results haven't been updated since previous scan
     */
    public static final String EXTRA_RESULTS_UPDATED = "resultsUpdated";

    /**
     * A batch of access point scans has been completed and the results areavailable.
     * Call {@link #getBatchedScanResults()} to obtain the results.
     * @deprecated This API is nolonger supported.
     * Use {@link WifiScanner} API
     * @hide
     */
    @Deprecated
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String BATCHED_SCAN_RESULTS_AVAILABLE_ACTION =
            "android.net.wifi.BATCHED_RESULTS";

    /**
     * The RSSI (signal strength) has changed.
     *
     * Receiver Required Permission: android.Manifest.permission.ACCESS_WIFI_STATE
     * @see #EXTRA_NEW_RSSI
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String RSSI_CHANGED_ACTION = "android.net.wifi.RSSI_CHANGED";
    /**
     * The lookup key for an {@code int} giving the new RSSI in dBm.
     */
    public static final String EXTRA_NEW_RSSI = "newRssi";

    /**
     * @see #ACTION_LINK_CONFIGURATION_CHANGED
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String LINK_CONFIGURATION_CHANGED_ACTION =
            "android.net.wifi.LINK_CONFIGURATION_CHANGED";

    /**
     * Broadcast intent action indicating that the link configuration changed on wifi.
     * <br /> No permissions are required to listen to this broadcast.
     * @hide
     */
    @SystemApi
    public static final String ACTION_LINK_CONFIGURATION_CHANGED =
            // should be android.net.wifi.action.LINK_CONFIGURATION_CHANGED, but due to
            // @UnsupportedAppUsage leaving it as android.net.wifi.LINK_CONFIGURATION_CHANGED.
            LINK_CONFIGURATION_CHANGED_ACTION;

    /**
     * The lookup key for a {@link android.net.LinkProperties} object associated with the
     * Wi-Fi network.
     * Included in the {@link #ACTION_LINK_CONFIGURATION_CHANGED} broadcast.
     *
     * Retrieve with {@link android.content.Intent#getParcelableExtra(String)}.
     *
     * @deprecated this extra is no longer populated.
     *
     * @hide
     */
    @Deprecated
    @SystemApi
    public static final String EXTRA_LINK_PROPERTIES = "android.net.wifi.extra.LINK_PROPERTIES";

    /**
     * The lookup key for a {@link android.net.NetworkCapabilities} object associated with the
     * Wi-Fi network. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     * @hide
     */
    public static final String EXTRA_NETWORK_CAPABILITIES = "networkCapabilities";

    /**
     * The network IDs of the configured networks could have changed.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String NETWORK_IDS_CHANGED_ACTION = "android.net.wifi.NETWORK_IDS_CHANGED";

    /**
     * Activity Action: Show a system activity that allows the user to enable
     * scans to be available even with Wi-Fi turned off.
     *
     * <p>Notification of the result of this activity is posted using the
     * {@link android.app.Activity#onActivityResult} callback. The
     * <code>resultCode</code>
     * will be {@link android.app.Activity#RESULT_OK} if scan always mode has
     * been turned on or {@link android.app.Activity#RESULT_CANCELED} if the user
     * has rejected the request or an error has occurred.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE =
            "android.net.wifi.action.REQUEST_SCAN_ALWAYS_AVAILABLE";

    /**
     * Activity Action: Pick a Wi-Fi network to connect to.
     * <p>Input: Nothing.
     * <p>Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PICK_WIFI_NETWORK = "android.net.wifi.PICK_WIFI_NETWORK";

    /**
     * Activity Action: Receiver should show UI to get user approval to enable WiFi.
     * <p>Input: {@link android.content.Intent#EXTRA_PACKAGE_NAME} string extra with
     *           the name of the app requesting the action.
     * <p>Output: Nothing.
     * <p>No permissions are required to send this action.
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_ENABLE = "android.net.wifi.action.REQUEST_ENABLE";

    /**
     * Activity Action: Receiver should show UI to get user approval to disable WiFi.
     * <p>Input: {@link android.content.Intent#EXTRA_PACKAGE_NAME} string extra with
     *           the name of the app requesting the action.
     * <p>Output: Nothing.
     * <p>No permissions are required to send this action.
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_DISABLE = "android.net.wifi.action.REQUEST_DISABLE";

    /**
     * Directed broadcast intent action indicating that the device has connected to one of the
     * network suggestions provided by the app. This will be sent post connection to a network
     * which was created with {@link WifiNetworkSuggestion.Builder#setIsAppInteractionRequired(
     * boolean)}
     * flag set.
     * <p>
     * Note: The broadcast is sent to the app only if it holds
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION} permission.
     *
     * @see #EXTRA_NETWORK_SUGGESTION
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION =
            "android.net.wifi.action.WIFI_NETWORK_SUGGESTION_POST_CONNECTION";
    /**
     * Sent as as a part of {@link #ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION} that holds
     * an instance of {@link WifiNetworkSuggestion} corresponding to the connected network.
     */
    public static final String EXTRA_NETWORK_SUGGESTION =
            "android.net.wifi.extra.NETWORK_SUGGESTION";

    /**
     * Internally used Wi-Fi lock mode representing the case were no locks are held.
     * @hide
     */
    public static final int WIFI_MODE_NO_LOCKS_HELD = 0;

    /**
     * In this Wi-Fi lock mode, Wi-Fi will be kept active,
     * and will behave normally, i.e., it will attempt to automatically
     * establish a connection to a remembered access point that is
     * within range, and will do periodic scans if there are remembered
     * access points but none are in range.
     *
     * @deprecated This API is non-functional and will have no impact.
     */
    @Deprecated
    public static final int WIFI_MODE_FULL = 1;

    /**
     * In this Wi-Fi lock mode, Wi-Fi will be kept active,
     * but the only operation that will be supported is initiation of
     * scans, and the subsequent reporting of scan results. No attempts
     * will be made to automatically connect to remembered access points,
     * nor will periodic scans be automatically performed looking for
     * remembered access points. Scans must be explicitly requested by
     * an application in this mode.
     *
     * @deprecated This API is non-functional and will have no impact.
     */
    @Deprecated
    public static final int WIFI_MODE_SCAN_ONLY = 2;

    /**
     * In this Wi-Fi lock mode, Wi-Fi will not go to power save.
     * This results in operating with low packet latency.
     * The lock is only active when the device is connected to an access point.
     * The lock is active even when the device screen is off or the acquiring application is
     * running in the background.
     * This mode will consume more power and hence should be used only
     * when there is a need for this tradeoff.
     * <p>
     * An example use case is when a voice connection needs to be
     * kept active even after the device screen goes off.
     * Holding a {@link #WIFI_MODE_FULL_HIGH_PERF} lock for the
     * duration of the voice call may improve the call quality.
     * <p>
     * When there is no support from the hardware, the {@link #WIFI_MODE_FULL_HIGH_PERF}
     * lock will have no impact.
     *
     * @deprecated The {@code WIFI_MODE_FULL_HIGH_PERF} is deprecated and is automatically replaced
     * with {@link #WIFI_MODE_FULL_LOW_LATENCY} with all the restrictions documented on that lock.
     * I.e. any request to the {@code WIFI_MODE_FULL_HIGH_PERF} will now obtain a
     * {@link #WIFI_MODE_FULL_LOW_LATENCY} lock instead.
     * Deprecation is due to the impact of {@code WIFI_MODE_FULL_HIGH_PERF} on power dissipation.
     * The {@link #WIFI_MODE_FULL_LOW_LATENCY} provides much of the same desired functionality with
     * less impact on power dissipation.
     */
    @Deprecated
    public static final int WIFI_MODE_FULL_HIGH_PERF = 3;

    /**
     * In this Wi-Fi lock mode, Wi-Fi will operate with a priority to achieve low latency.
     * {@link #WIFI_MODE_FULL_LOW_LATENCY} lock has the following limitations:
     * <ol>
     * <li>The lock is only active when the device is connected to an access point.</li>
     * <li>The lock is only active when the screen is on.</li>
     * <li>The lock is only active when the acquiring app is running in the foreground.</li>
     * </ol>
     * Low latency mode optimizes for reduced packet latency,
     * and as a result other performance measures may suffer when there are trade-offs to make:
     * <ol>
     * <li>Battery life may be reduced.</li>
     * <li>Throughput may be reduced.</li>
     * <li>Frequency of Wi-Fi scanning may be reduced. This may result in: </li>
     * <ul>
     * <li>The device may not roam or switch to the AP with highest signal quality.</li>
     * <li>Location accuracy may be reduced.</li>
     * </ul>
     * </ol>
     * <p>
     * Example use cases are real time gaming or virtual reality applications where
     * low latency is a key factor for user experience.
     * <p>
     * Note: For an app which acquires both {@link #WIFI_MODE_FULL_LOW_LATENCY} and
     * {@link #WIFI_MODE_FULL_HIGH_PERF} locks, {@link #WIFI_MODE_FULL_LOW_LATENCY}
     * lock will be effective when app is running in foreground and screen is on,
     * while the {@link #WIFI_MODE_FULL_HIGH_PERF} lock will take effect otherwise.
     */
    public static final int WIFI_MODE_FULL_LOW_LATENCY = 4;


    /** Anything worse than or equal to this will show 0 bars. */
    @UnsupportedAppUsage
    private static final int MIN_RSSI = -100;

    /** Anything better than or equal to this will show the max bars. */
    @UnsupportedAppUsage
    private static final int MAX_RSSI = -55;

    /**
     * Number of RSSI levels used in the framework to initiate {@link #RSSI_CHANGED_ACTION}
     * broadcast, where each level corresponds to a range of RSSI values.
     * The {@link #RSSI_CHANGED_ACTION} broadcast will only fire if the RSSI
     * change is significant enough to change the RSSI signal level.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final int RSSI_LEVELS = 5;

    //TODO (b/146346676): This needs to be removed, not used in the code.
    /**
     * Auto settings in the driver. The driver could choose to operate on both
     * 2.4 GHz and 5 GHz or make a dynamic decision on selecting the band.
     * @hide
     */
    @UnsupportedAppUsage
    public static final int WIFI_FREQUENCY_BAND_AUTO = 0;

    /**
     * Operation on 5 GHz alone
     * @hide
     */
    @UnsupportedAppUsage
    public static final int WIFI_FREQUENCY_BAND_5GHZ = 1;

    /**
     * Operation on 2.4 GHz alone
     * @hide
     */
    @UnsupportedAppUsage
    public static final int WIFI_FREQUENCY_BAND_2GHZ = 2;

    /** @hide */
    public static final boolean DEFAULT_POOR_NETWORK_AVOIDANCE_ENABLED = false;

    /**
     * Maximum number of active locks we allow.
     * This limit was added to prevent apps from creating a ridiculous number
     * of locks and crashing the system by overflowing the global ref table.
     */
    private static final int MAX_ACTIVE_LOCKS = 50;

    /** Indicates an invalid SSID. */
    public static final String UNKNOWN_SSID = "<unknown ssid>";

    /** @hide */
    public static final MacAddress ALL_ZEROS_MAC_ADDRESS =
            MacAddress.fromString("00:00:00:00:00:00");

    /** @hide */
    @IntDef(flag = false, prefix = { "WIFI_MULTI_INTERNET_MODE_" }, value = {
        WIFI_MULTI_INTERNET_MODE_DISABLED,
        WIFI_MULTI_INTERNET_MODE_DBS_AP,
        WIFI_MULTI_INTERNET_MODE_MULTI_AP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiMultiInternetMode {}

    /**
     * Wi-Fi simultaneous connection to multiple internet-providing Wi-Fi networks (APs) is
     * disabled.
     *
     * @see #getStaConcurrencyForMultiInternetMode()
     *
     */
    public static final int WIFI_MULTI_INTERNET_MODE_DISABLED = 0;
    /**
     * Wi-Fi simultaneous connection to multiple internet-providing Wi-FI networks (APs) is enabled
     * and restricted to a single network on different bands (e.g. a DBS AP).
     *
     * @see #getStaConcurrencyForMultiInternetMode()
     *
     */
    public static final int WIFI_MULTI_INTERNET_MODE_DBS_AP = 1;
    /**
     * Wi-Fi simultaneous connection to multiple internet-providing Wi-Fi networks (APs) is enabled.
     * The device can connect to any networks/APs - it is just restricted to using different bands
     * for individual connections.
     *
     * @see #getStaConcurrencyForMultiInternetMode()
     *
     */
    public static final int WIFI_MULTI_INTERNET_MODE_MULTI_AP = 2;

    /**
     * The bundle key string for the channel frequency in MHz.
     * See {@link #getChannelData(Executor, Consumer)}
     */
    public static final String CHANNEL_DATA_KEY_FREQUENCY_MHZ = "CHANNEL_DATA_KEY_FREQUENCY_MHZ";
    /**
     * The bundle key for the number of APs found on the corresponding channel specified by
     * {@link WifiManager#CHANNEL_DATA_KEY_FREQUENCY_MHZ}.
     * See {@link #getChannelData(Executor, Consumer)}
     */
    public static final String CHANNEL_DATA_KEY_NUM_AP = "CHANNEL_DATA_KEY_NUM_AP";

    /**
     * This policy is being tracked by the Wifi service.
     * Indicates success for {@link #addQosPolicies(List, Executor, Consumer)}.
     * @hide
     */
    @SystemApi
    public static final int QOS_REQUEST_STATUS_TRACKING = 0;

    /**
     * A policy with the same policy ID is already being tracked.
     * @hide
     */
    @SystemApi
    public static final int QOS_REQUEST_STATUS_ALREADY_ACTIVE = 1;

    /**
     * There are insufficient resources to handle this request at this time.
     * @hide
     */
    @SystemApi
    public static final int QOS_REQUEST_STATUS_INSUFFICIENT_RESOURCES = 2;

    /**
     * The parameters in the policy request are invalid.
     * @hide
     */
    @SystemApi
    public static final int QOS_REQUEST_STATUS_INVALID_PARAMETERS = 3;

    /**
     * An unspecified failure occurred while processing this request.
     * @hide
     */
    @SystemApi
    public static final int QOS_REQUEST_STATUS_FAILURE_UNKNOWN = 4;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"QOS_REQUEST_STATUS_"}, value = {
            QOS_REQUEST_STATUS_TRACKING,
            QOS_REQUEST_STATUS_ALREADY_ACTIVE,
            QOS_REQUEST_STATUS_INSUFFICIENT_RESOURCES,
            QOS_REQUEST_STATUS_INVALID_PARAMETERS,
            QOS_REQUEST_STATUS_FAILURE_UNKNOWN})
    public @interface QosRequestStatus {}

    /**
     * Maximum number of policies that can be included in a QoS add/remove request.
     */
    private static final int MAX_POLICIES_PER_QOS_REQUEST = 16;

    /**
     * Get the maximum number of policies that can be included in a request to
     * {@link #addQosPolicies(List, Executor, Consumer)} or {@link #removeQosPolicies(int[])}.
     * @hide
     */
    @SystemApi
    public static int getMaxNumberOfPoliciesPerQosRequest() {
        return MAX_POLICIES_PER_QOS_REQUEST;
    }

    /* Number of currently active WifiLocks and MulticastLocks */
    @UnsupportedAppUsage
    private int mActiveLockCount;

    private Context mContext;
    @UnsupportedAppUsage
    IWifiManager mService;
    private final int mTargetSdkVersion;

    private Looper mLooper;
    private boolean mVerboseLoggingEnabled = false;

    private final Object mLock = new Object(); // lock guarding access to the following vars
    @GuardedBy("mLock")
    private LocalOnlyHotspotCallbackProxy mLOHSCallbackProxy;
    @GuardedBy("mLock")
    private LocalOnlyHotspotObserverProxy mLOHSObserverProxy;

    private static final SparseArray<IOnWifiUsabilityStatsListener>
            sOnWifiUsabilityStatsListenerMap = new SparseArray();
    private static final SparseArray<ISuggestionConnectionStatusListener>
            sSuggestionConnectionStatusListenerMap = new SparseArray();
    private static final SparseArray<ISuggestionUserApprovalStatusListener>
            sSuggestionUserApprovalStatusListenerMap = new SparseArray();
    private static final SparseArray<IWifiVerboseLoggingStatusChangedListener>
            sWifiVerboseLoggingStatusChangedListenerMap = new SparseArray();
    private static final SparseArray<INetworkRequestMatchCallback>
            sNetworkRequestMatchCallbackMap = new SparseArray();
    private static final SparseArray<ITrafficStateCallback>
            sTrafficStateCallbackMap = new SparseArray();
    private static final SparseArray<ISoftApCallback> sSoftApCallbackMap = new SparseArray();
    private static final SparseArray<IOnWifiDriverCountryCodeChangedListener>
            sActiveCountryCodeChangedCallbackMap = new SparseArray();
    private static final SparseArray<ISoftApCallback>
            sLocalOnlyHotspotSoftApCallbackMap = new SparseArray();
    private static final SparseArray<ILocalOnlyConnectionStatusListener>
            sLocalOnlyConnectionStatusListenerMap = new SparseArray();
    private static final SparseArray<IWifiNetworkStateChangedListener>
            sOnWifiNetworkStateChangedListenerMap = new SparseArray<>();
    private static final SparseArray<IWifiLowLatencyLockListener>
            sWifiLowLatencyLockListenerMap = new SparseArray<>();

    /**
     * Multi-link operation (MLO) will allow Wi-Fi devices to operate on multiple links at the same
     * time through a single connection, aiming to support applications that require lower latency,
     * and higher capacity. Chip vendors have algorithms that run on the chip to use available links
     * based on incoming traffic and various inputs. Below is a list of Multi-Link Operation modes
     * that applications can suggest to be accommodated in the algorithm.
     *
     * The default MLO mode is for chip vendors to use algorithms to select the optimum links to
     * operate on, without any guidance from the calling app.
     *
     * @hide
     */
    @SystemApi
    public static final int MLO_MODE_DEFAULT = 0;

    /**
     * Low latency mode for Multi-link operation. In this mode, the chip vendor's algorithm
     * should select MLO links that will achieve low latency.
     *
     * @hide
     */
    @SystemApi
    public static final int MLO_MODE_LOW_LATENCY = 1;

    /**
     * High throughput mode for Multi-link operation. In this mode, the chip vendor's algorithm
     * should select MLO links that will achieve higher throughput.
     *
     * @hide
     */
    @SystemApi
    public static final int MLO_MODE_HIGH_THROUGHPUT = 2;

    /**
     * Low power mode for Multi-link operation. In this mode, the chip vendor's algorithm
     * should select MLO links that will achieve low power.
     *
     * @hide
     */
    @SystemApi
    public static final int MLO_MODE_LOW_POWER = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"MLO_MODE_"}, value = {
            MLO_MODE_DEFAULT,
            MLO_MODE_LOW_LATENCY,
            MLO_MODE_HIGH_THROUGHPUT,
            MLO_MODE_LOW_POWER})
    public @interface MloMode {
    }

    /**
     * Create a new WifiManager instance.
     * Applications will almost always want to use
     * {@link android.content.Context#getSystemService Context.getSystemService} to retrieve
     * the standard {@link android.content.Context#WIFI_SERVICE Context.WIFI_SERVICE}.
     *
     * @param context the application context
     * @param service the Binder interface
     * @param looper the Looper used to deliver callbacks
     * @hide - hide this because it takes in a parameter of type IWifiManager, which
     * is a system private class.
     */
    public WifiManager(@NonNull Context context, @NonNull IWifiManager service,
        @NonNull Looper looper) {
        mContext = context;
        mService = service;
        mLooper = looper;
        mTargetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        updateVerboseLoggingEnabledFromService();
    }

    /**
     * Return a list of all the networks configured for the current foreground
     * user.
     *
     * Not all fields of WifiConfiguration are returned. Only the following
     * fields are filled in:
     * <ul>
     * <li>networkId</li>
     * <li>SSID</li>
     * <li>BSSID</li>
     * <li>priority</li>
     * <li>allowedProtocols</li>
     * <li>allowedKeyManagement</li>
     * <li>allowedAuthAlgorithms</li>
     * <li>allowedPairwiseCiphers</li>
     * <li>allowedGroupCiphers</li>
     * <li>status</li>
     * </ul>
     * @return a list of network configurations in the form of a list
     * of {@link WifiConfiguration} objects.
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return an
     * empty list.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps will have access to the full list.
     * <li>Callers with Carrier privilege will receive a restricted list only containing
     * configurations which they created.
     * </ul>
     */
    @Deprecated
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    public List<WifiConfiguration> getConfiguredNetworks() {
        try {
            ParceledListSlice<WifiConfiguration> parceledList =
                    mService.getConfiguredNetworks(mContext.getOpPackageName(),
                            mContext.getAttributionTag(), false);
            if (parceledList == null) {
                return Collections.emptyList();
            }
            return parceledList.getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return a list of all the networks previously configured by the calling app. Can
     * be called by Device Owner (DO), Profile Owner (PO), Callers with Carrier privilege and
     * system apps.
     *
     * @return a list of network configurations in the form of a list
     * of {@link WifiConfiguration} objects.
     * @throws SecurityException if the caller is not allowed to call this API
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    @NonNull
    public List<WifiConfiguration> getCallerConfiguredNetworks() {
        try {
            ParceledListSlice<WifiConfiguration> parceledList =
                    mService.getConfiguredNetworks(mContext.getOpPackageName(),
                            mContext.getAttributionTag(), true);
            if (parceledList == null) {
                return Collections.emptyList();
            }
            return parceledList.getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }


    /**
     * Applications targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later need to have
     * the following permissions: {@link android.Manifest.permission#NEARBY_WIFI_DEVICES},
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE} and
     * {@link android.Manifest.permission#READ_WIFI_CREDENTIAL}.
     * Applications targeting {@link Build.VERSION_CODES#S} or prior SDK levels need to have the
     * following permissions: {@link android.Manifest.permission#ACCESS_FINE_LOCATION},
     * {@link android.Manifest.permission#CHANGE_WIFI_STATE} and
     * {@link android.Manifest.permission#READ_WIFI_CREDENTIAL}.
     * <p> See {@link #getPrivilegedConnectedNetwork()} to get the WifiConfiguration for only the
     * connected network that's providing internet by default.
     *
     * @hide
     **/
    @SystemApi
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES, ACCESS_WIFI_STATE,
            READ_WIFI_CREDENTIAL},
            conditional = true)
    public List<WifiConfiguration> getPrivilegedConfiguredNetworks() {
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            ParceledListSlice<WifiConfiguration> parceledList =
                    mService.getPrivilegedConfiguredNetworks(mContext.getOpPackageName(),
                            mContext.getAttributionTag(), extras);
            if (parceledList == null) {
                return Collections.emptyList();
            }
            return parceledList.getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets the {@link WifiConfiguration} with credentials of the connected wifi network
     * that's providing internet by default.
     * <p>
     * On {@link android.os.Build.VERSION_CODES#TIRAMISU} or later SDKs, the caller need to have
     * the following permissions: {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation",
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE} and
     * {@link android.Manifest.permission#READ_WIFI_CREDENTIAL}. If the app does not have
     * android:usesPermissionFlags="neverForLocation", then it must also have
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     * <p>
     * On {@link Build.VERSION_CODES#S} or prior SDKs, the caller need to have the
     * following permissions: {@link android.Manifest.permission#ACCESS_FINE_LOCATION},
     * {@link android.Manifest.permission#CHANGE_WIFI_STATE} and
     * {@link android.Manifest.permission#READ_WIFI_CREDENTIAL}.
     *
     * @return The WifiConfiguration representation of the connected wifi network providing
     * internet, or null if wifi is not connected.
     *
     * @throws SecurityException if caller does not have the required permissions
     * @hide
     **/
    @SystemApi
    @RequiresPermission(allOf = {NEARBY_WIFI_DEVICES, ACCESS_WIFI_STATE, READ_WIFI_CREDENTIAL},
            conditional = true)
    @Nullable
    public WifiConfiguration getPrivilegedConnectedNetwork() {
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            return mService.getPrivilegedConnectedNetwork(mContext.getOpPackageName(),
                    mContext.getAttributionTag(), extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of all matching WifiConfigurations of PasspointConfiguration for a given list
     * of ScanResult.
     *
     * An empty list will be returned when no PasspointConfiguration are installed or if no
     * PasspointConfiguration match the ScanResult.
     *
     * @param scanResults a list of scanResult that represents the BSSID
     * @return List that consists of {@link WifiConfiguration} and corresponding scanResults per
     * network type({@link #PASSPOINT_HOME_NETWORK} and {@link #PASSPOINT_ROAMING_NETWORK}).
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    @NonNull
    public List<Pair<WifiConfiguration, Map<Integer, List<ScanResult>>>> getAllMatchingWifiConfigs(
            @NonNull List<ScanResult> scanResults) {
        List<Pair<WifiConfiguration, Map<Integer, List<ScanResult>>>> configs = new ArrayList<>();
        try {
            Map<String, Map<Integer, List<ScanResult>>> results =
                    mService.getAllMatchingPasspointProfilesForScanResults(scanResults);
            if (results.isEmpty()) {
                return configs;
            }
            List<WifiConfiguration> wifiConfigurations =
                    mService.getWifiConfigsForPasspointProfiles(
                            new ArrayList<>(results.keySet()));
            for (WifiConfiguration configuration : wifiConfigurations) {
                Map<Integer, List<ScanResult>> scanResultsPerNetworkType =
                        results.get(configuration.getProfileKey());
                if (scanResultsPerNetworkType != null) {
                    configs.add(Pair.create(configuration, scanResultsPerNetworkType));
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

        return configs;
    }

    /**
     * To be used with setScreenOnScanSchedule.
     * @hide
     */
    @SystemApi
    public static class ScreenOnScanSchedule {
        private final Duration mScanInterval;
        private final int mScanType;

        /**
         * Creates a ScreenOnScanSchedule.
         * @param scanInterval Interval between framework-initiated connectivity scans.
         * @param scanType One of the {@code WifiScanner.SCAN_TYPE_} values.
         */
        public ScreenOnScanSchedule(@NonNull Duration scanInterval,
                @WifiAnnotations.ScanType int scanType) {
            if (scanInterval == null) {
                throw new IllegalArgumentException("scanInterval can't be null");
            }
            mScanInterval = scanInterval;
            mScanType = scanType;
        }

        /**
         * Gets the interval between framework-initiated connectivity scans.
         */
        public @NonNull Duration getScanInterval() {
            return mScanInterval;
        }

        /**
         * Gets the type of scan to be used. One of the {@code WifiScanner.SCAN_TYPE_} values.
         */
        public @WifiAnnotations.ScanType int getScanType() {
            return mScanType;
        }
    }

    /**
     * This API allows a privileged app to customize the wifi framework's network selection logic.
     * To revert to default behavior, call this API with a {@link WifiNetworkSelectionConfig}
     * created from a default {@link WifiNetworkSelectionConfig.Builder}.
     *
     * Use {@link WifiManager#getNetworkSelectionConfig(Executor, Consumer)} to get the current
     * network selection configuration.
     * <P>
     * @param nsConfig an Object representing the network selection configuration being programmed.
     *                 This should be created with a {@link WifiNetworkSelectionConfig.Builder}.
     *
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws IllegalArgumentException if input is invalid.
     * @throws SecurityException if the caller does not have permission.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            MANAGE_WIFI_NETWORK_SELECTION
    })
    @SystemApi
    public void setNetworkSelectionConfig(@NonNull WifiNetworkSelectionConfig nsConfig) {
        try {
            if (nsConfig == null) {
                throw new IllegalArgumentException("nsConfig can not be null");
            }
            mService.setNetworkSelectionConfig(nsConfig);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * This API allows a privileged app to retrieve the {@link WifiNetworkSelectionConfig}
     * currently being used by the network selector.
     *
     * Use {@link WifiManager#setNetworkSelectionConfig(WifiNetworkSelectionConfig)} to set a
     * new network selection configuration.
     * <P>
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return
     *                        {@link WifiNetworkSelectionConfig}
     *
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws SecurityException if the caller does not have permission.
     * @throws NullPointerException if the caller provided invalid inputs.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            MANAGE_WIFI_NETWORK_SELECTION
    })
    @SystemApi
    public void getNetworkSelectionConfig(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<WifiNetworkSelectionConfig> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.getNetworkSelectionConfig(
                    new IWifiNetworkSelectionConfigListener.Stub() {
                        @Override
                        public void onResult(WifiNetworkSelectionConfig value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allows a privileged app to enable/disable whether a confirmation dialog should be displayed
     * when third-party apps attempt to turn on WiFi.
     *
     * Use {@link #isThirdPartyAppEnablingWifiConfirmationDialogEnabled()} to get the
     * currently configured value.
     *
     * Note: Only affects behavior for apps with targetSDK < Q, since third party apps are not
     * allowed to enable wifi on targetSDK >= Q.
     *
     * This overrides the overlay value |config_showConfirmationDialogForThirdPartyAppsEnablingWifi|
     * <P>
     * @param enable true to enable the confirmation dialog, false otherwise
     *
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws SecurityException if the caller does not have permission.
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    @SystemApi
    public void setThirdPartyAppEnablingWifiConfirmationDialogEnabled(boolean enable) {
        try {
            mService.setThirdPartyAppEnablingWifiConfirmationDialogEnabled(enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check whether the wifi configuration indicates that a confirmation dialog should be displayed
     * when third-party apps attempt to turn on WiFi.
     *
     * Use {@link #setThirdPartyAppEnablingWifiConfirmationDialogEnabled(boolean)} to set this
     * value.
     *
     * Note: This setting only affects behavior for apps with targetSDK < Q, since third party apps
     *       are not allowed to enable wifi on targetSDK >= Q.
     *
     * <P>
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws SecurityException if the caller does not have permission.
     * @return true if dialog should be displayed, false otherwise.
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    @SystemApi
    public boolean isThirdPartyAppEnablingWifiConfirmationDialogEnabled() {
        try {
            return mService.isThirdPartyAppEnablingWifiConfirmationDialogEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allows a privileged app to customize the screen-on scan behavior. When a non-null schedule
     * is set via this API, it will always get used instead of the scan schedules defined in the
     * overlay. When a null schedule is set via this API, the wifi subsystem will go back to using
     * the scan schedules defined in the overlay. Also note, the scan schedule will be truncated
     * (rounded down) to the nearest whole second.
     * <p>
     * Example usage:
     * The following call specifies that first scheduled scan should be in 20 seconds using
     * {@link WifiScanner#SCAN_TYPE_HIGH_ACCURACY}, and all
     * scheduled scans later should happen every 40 seconds using
     * {@link WifiScanner#SCAN_TYPE_LOW_POWER}.
     * <pre>
     * List<ScreenOnScanSchedule> schedule = new ArrayList<>();
     * schedule.add(new ScreenOnScanSchedule(Duration.ofSeconds(20),
     *         WifiScanner.SCAN_TYPE_HIGH_ACCURACY));
     * schedule.add(new ScreenOnScanSchedule(Duration.ofSeconds(40),
     *         WifiScanner.SCAN_TYPE_LOW_POWER));
     * wifiManager.setScreenOnScanSchedule(schedule);
     * </pre>
     * @param screenOnScanSchedule defines the screen-on scan schedule and the corresponding
     *                             scan type. Set to null to clear any previously set value.
     *
     * @throws IllegalStateException if input is invalid
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws SecurityException if the caller does not have permission.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            MANAGE_WIFI_NETWORK_SELECTION
    })
    @SystemApi
    public void setScreenOnScanSchedule(@Nullable List<ScreenOnScanSchedule> screenOnScanSchedule) {
        try {
            if (screenOnScanSchedule == null) {
                mService.setScreenOnScanSchedule(null, null);
                return;
            }
            if (screenOnScanSchedule.isEmpty()) {
                throw new IllegalArgumentException("The input should either be null or a non-empty"
                        + " list");
            }
            int[] scanSchedule = new int[screenOnScanSchedule.size()];
            int[] scanType = new int[screenOnScanSchedule.size()];
            for (int i = 0; i < screenOnScanSchedule.size(); i++) {
                scanSchedule[i] = (int) screenOnScanSchedule.get(i).getScanInterval().toSeconds();
                scanType[i] = screenOnScanSchedule.get(i).getScanType();
            }
            mService.setScreenOnScanSchedule(scanSchedule, scanType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * The Wi-Fi framework may trigger connectivity scans in response to the screen turning on for
     * network selection purposes. This API allows a privileged app to set a delay to the next
     * connectivity scan triggered by the Wi-Fi framework in response to the next screen-on event.
     * This gives a window for the privileged app to issue their own custom scans to influence Wi-Fi
     * network selection. The expected usage is the privileged app monitor for the screen turning
     * off, and then call this API if it believes delaying the next screen-on connectivity scan is
     * needed.
     * <p>
     * Note that this API will only delay screen-on connectivity scans once. This API will need to
     * be called again if further screen-on scan delays are needed after it resolves.
     * @param delayMs defines the time in milliseconds to delay the next screen-on connectivity
     *                scan. Setting this to 0 will remove the delay.
     *
     * @throws IllegalStateException if input is invalid
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws SecurityException if the caller does not have permission.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            MANAGE_WIFI_NETWORK_SELECTION
    })
    @SystemApi
    public void setOneShotScreenOnConnectivityScanDelayMillis(@IntRange(from = 0) int delayMs) {
        try {
            mService.setOneShotScreenOnConnectivityScanDelayMillis(delayMs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Retrieve a list of {@link WifiConfiguration} for available {@link WifiNetworkSuggestion}
     * matching the given list of {@link ScanResult}.
     *
     * An available {@link WifiNetworkSuggestion} must satisfy:
     * <ul>
     * <li> Matching one of the {@link ScanResult} from the given list.
     * <li> and {@link WifiNetworkSuggestion.Builder#setIsUserAllowedToManuallyConnect(boolean)} set
     * to true.
     * </ul>
     *
     * @param scanResults a list of scanResult.
     * @return a list of @link WifiConfiguration} for available {@link WifiNetworkSuggestion}
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    @NonNull
    public List<WifiConfiguration> getWifiConfigForMatchedNetworkSuggestionsSharedWithUser(
            @NonNull List<ScanResult> scanResults) {
        try {
            return mService.getWifiConfigForMatchedNetworkSuggestionsSharedWithUser(scanResults);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Specify a set of SSIDs that will not get disabled internally by the Wi-Fi subsystem when
     * connection issues occur. To clear the list, call this API with an empty Set.
     * <p>
     * {@link #getSsidsAllowlist()} can be used to check the SSIDs that have been set.
     * @param ssids - list of WifiSsid that will not get disabled internally
     * @throws SecurityException if the calling app is not a Device Owner (DO), Profile Owner (PO),
     *                           or a privileged app that has one of the permissions required by
     *                           this API.
     * @throws IllegalArgumentException if the input is null.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION}, conditional = true)
    public void setSsidsAllowlist(@NonNull Set<WifiSsid> ssids) {
        if (ssids == null) {
            throw new IllegalArgumentException(TAG + ": ssids can not be null");
        }
        try {
            mService.setSsidsAllowlist(mContext.getOpPackageName(), new ArrayList<>(ssids));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the Set of SSIDs that will not get disabled internally by the Wi-Fi subsystem when
     * connection issues occur.
     * @throws SecurityException if the calling app is not a Device Owner (DO), Profile Owner (PO),
     *                           or a privileged app that has one of the permissions required by
     *                           this API.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION}, conditional = true)
    public @NonNull Set<WifiSsid> getSsidsAllowlist() {
        try {
            return new ArraySet<WifiSsid>(
                    mService.getSsidsAllowlist(mContext.getOpPackageName()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of unique Hotspot 2.0 OSU (Online Sign-Up) providers associated with a given
     * list of ScanResult.
     *
     * An empty list will be returned if no match is found.
     *
     * @param scanResults a list of ScanResult
     * @return Map that consists {@link OsuProvider} and a list of matching {@link ScanResult}
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    @NonNull
    public Map<OsuProvider, List<ScanResult>> getMatchingOsuProviders(
            @Nullable List<ScanResult> scanResults) {
        if (scanResults == null) {
            return new HashMap<>();
        }
        try {
            return mService.getMatchingOsuProviders(scanResults);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the matching Passpoint R2 configurations for given OSU (Online Sign-Up) providers.
     *
     * Given a list of OSU providers, this only returns OSU providers that already have Passpoint R2
     * configurations in the device.
     * An empty map will be returned when there is no matching Passpoint R2 configuration for the
     * given OsuProviders.
     *
     * @param osuProviders a set of {@link OsuProvider}
     * @return Map that consists of {@link OsuProvider} and matching {@link PasspointConfiguration}.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    @NonNull
    public Map<OsuProvider, PasspointConfiguration> getMatchingPasspointConfigsForOsuProviders(
            @NonNull Set<OsuProvider> osuProviders) {
        try {
            return mService.getMatchingPasspointConfigsForOsuProviders(
                    new ArrayList<>(osuProviders));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Add a new network description to the set of configured networks.
     * The {@code networkId} field of the supplied configuration object
     * is ignored.
     * <p/>
     * The new network will be marked DISABLED by default. To enable it,
     * called {@link #enableNetwork}.
     *
     * @param config the set of variables that describe the configuration,
     *            contained in a {@link WifiConfiguration} object.
     *            If the {@link WifiConfiguration} has an Http Proxy set
     *            the calling app must be System, or be provisioned as the Profile or Device Owner.
     * @return the ID of the newly created network description. This is used in
     *         other operations to specified the network to be acted upon.
     *         Returns {@code -1} on failure.
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return
     * {@code -1}.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    public int addNetwork(WifiConfiguration config) {
        if (config == null) {
            return -1;
        }
        config.networkId = -1;
        return addOrUpdateNetwork(config);
    }

    /**
     * This is a new version of {@link #addNetwork(WifiConfiguration)} which returns more detailed
     * failure codes. The usage of this API is limited to Device Owner (DO), Profile Owner (PO),
     * system app, and privileged apps.
     * <p>
     * Add a new network description to the set of configured networks. The {@code networkId}
     * field of the supplied configuration object is ignored. The new network will be marked
     * DISABLED by default. To enable it, call {@link #enableNetwork}.
     * <p>
     * @param config the set of variables that describe the configuration,
     *            contained in a {@link WifiConfiguration} object.
     *            If the {@link WifiConfiguration} has an Http Proxy set
     *            the calling app must be System, or be provisioned as the Profile or Device Owner.
     * @return A {@link AddNetworkResult} Object.
     * @throws SecurityException if the calling app is not a Device Owner (DO),
     *                           Profile Owner (PO), system app, or a privileged app that has one of
     *                           the permissions required by this API.
     * @throws IllegalArgumentException if the input configuration is null or if the
     *            security type in input configuration is not supported.
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_MANAGED_PROVISIONING
            }, conditional = true)
    @NonNull
    public AddNetworkResult addNetworkPrivileged(@NonNull WifiConfiguration config) {
        if (config == null) throw new IllegalArgumentException("config cannot be null");
        if (config.isSecurityType(WifiInfo.SECURITY_TYPE_DPP)
                && !isFeatureSupported(WIFI_FEATURE_DPP_AKM)) {
            throw new IllegalArgumentException("dpp akm is not supported");
        }
        config.networkId = -1;
        try {
            return mService.addOrUpdateNetworkPrivileged(config, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Provides the results of a call to {@link #addNetworkPrivileged(WifiConfiguration)}
     */
    public static final class AddNetworkResult implements Parcelable {
        /**
         * The operation has completed successfully.
         */
        public static final int STATUS_SUCCESS = 0;
        /**
         * The operation has failed due to an unknown reason.
         */
        public static final int STATUS_FAILURE_UNKNOWN = 1;
        /**
         * The calling app does not have permission to call this API.
         */
        public static final int STATUS_NO_PERMISSION = 2;
        /**
         * Generic failure code for adding a passpoint network.
         */
        public static final int STATUS_ADD_PASSPOINT_FAILURE = 3;
        /**
         * Generic failure code for adding a non-passpoint network.
         */
        public static final int STATUS_ADD_WIFI_CONFIG_FAILURE = 4;
        /**
         * The network configuration is invalid.
         */
        public static final int STATUS_INVALID_CONFIGURATION = 5;
        /**
         * The calling app has no permission to modify the configuration.
         */
        public static final int STATUS_NO_PERMISSION_MODIFY_CONFIG = 6;
        /**
         * The calling app has no permission to modify the proxy setting.
         */
        public static final int STATUS_NO_PERMISSION_MODIFY_PROXY_SETTING = 7;
        /**
         * The calling app has no permission to modify the MAC randomization setting.
         */
        public static final int STATUS_NO_PERMISSION_MODIFY_MAC_RANDOMIZATION = 8;
        /**
         * Internal failure in updating network keys..
         */
        public static final int STATUS_FAILURE_UPDATE_NETWORK_KEYS = 9;
        /**
         * The enterprise network is missing either the root CA or domain name.
         */
        public static final int STATUS_INVALID_CONFIGURATION_ENTERPRISE = 10;

        /** @hide */
        @IntDef(prefix = { "STATUS_" }, value = {
                STATUS_SUCCESS,
                STATUS_FAILURE_UNKNOWN,
                STATUS_NO_PERMISSION,
                STATUS_ADD_PASSPOINT_FAILURE,
                STATUS_ADD_WIFI_CONFIG_FAILURE,
                STATUS_INVALID_CONFIGURATION,
                STATUS_NO_PERMISSION_MODIFY_CONFIG,
                STATUS_NO_PERMISSION_MODIFY_PROXY_SETTING,
                STATUS_NO_PERMISSION_MODIFY_MAC_RANDOMIZATION,
                STATUS_FAILURE_UPDATE_NETWORK_KEYS,
                STATUS_INVALID_CONFIGURATION_ENTERPRISE,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface AddNetworkStatusCode {}

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(statusCode);
            dest.writeInt(networkId);
        }

        /** Implement the Parcelable interface */
        public static final @android.annotation.NonNull Creator<AddNetworkResult> CREATOR =
                new Creator<AddNetworkResult>() {
                    public AddNetworkResult createFromParcel(Parcel in) {
                        return new AddNetworkResult(in.readInt(), in.readInt());
                    }

                    public AddNetworkResult[] newArray(int size) {
                        return new AddNetworkResult[size];
                    }
                };

        /**
         * One of the {@code STATUS_} values. If the operation is successful this field
         * will be set to {@code STATUS_SUCCESS}.
         */
        public final @AddNetworkStatusCode int statusCode;
        /**
         * The identifier of the added network, which could be used in other operations. This field
         * will be set to {@code -1} if the operation failed.
         */
        public final int networkId;

        public AddNetworkResult(@AddNetworkStatusCode int statusCode, int networkId) {
            this.statusCode = statusCode;
            this.networkId = networkId;
        }
    }

    /**
     * Update the network description of an existing configured network.
     *
     * @param config the set of variables that describe the configuration,
     *            contained in a {@link WifiConfiguration} object. It may
     *            be sparse, so that only the items that are being changed
     *            are non-<code>null</code>. The {@code networkId} field
     *            must be set to the ID of the existing network being updated.
     *            If the {@link WifiConfiguration} has an Http Proxy set
     *            the calling app must be System, or be provisioned as the Profile or Device Owner.
     * @return Returns the {@code networkId} of the supplied
     *         {@code WifiConfiguration} on success.
     *         <br/>
     *         Returns {@code -1} on failure, including when the {@code networkId}
     *         field of the {@code WifiConfiguration} does not refer to an
     *         existing network.
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return
     * {@code -1}.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    public int updateNetwork(WifiConfiguration config) {
        if (config == null || config.networkId < 0) {
            return -1;
        }
        return addOrUpdateNetwork(config);
    }

    /**
     * Internal method for doing the RPC that creates a new network description
     * or updates an existing one.
     *
     * @param config The possibly sparse object containing the variables that
     *         are to set or updated in the network description.
     * @return the ID of the network on success, {@code -1} on failure.
     */
    private int addOrUpdateNetwork(WifiConfiguration config) {
        Bundle extras = new Bundle();
        if (SdkLevel.isAtLeastS()) {
            extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                    mContext.getAttributionSource());
        }

        try {
            return mService.addOrUpdateNetwork(config, mContext.getOpPackageName(), extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Interface for indicating user selection from the list of networks presented in the
     * {@link NetworkRequestMatchCallback#onMatch(List)}.
     *
     * The platform will implement this callback and pass it along with the
     * {@link NetworkRequestMatchCallback#onUserSelectionCallbackRegistration(
     * NetworkRequestUserSelectionCallback)}. The UI component handling
     * {@link NetworkRequestMatchCallback} will invoke {@link #select(WifiConfiguration)} or
     * {@link #reject()} to return the user's selection back to the platform via this callback.
     * @hide
     */
    @SystemApi
    public interface NetworkRequestUserSelectionCallback {
        /**
         * User selected this network to connect to.
         * @param wifiConfiguration WifiConfiguration object corresponding to the network
         *                          user selected.
         */
        @SuppressLint("CallbackMethodName")
        default void select(@NonNull WifiConfiguration wifiConfiguration) {}

        /**
         * User rejected the app's request.
         */
        @SuppressLint("CallbackMethodName")
        default void reject() {}
    }

    /**
     * Interface for network request callback. Should be implemented by applications and passed when
     * calling {@link #registerNetworkRequestMatchCallback(Executor,
     * WifiManager.NetworkRequestMatchCallback)}.
     *
     * This is meant to be implemented by a UI component to present the user with a list of networks
     * matching the app's request. The user is allowed to pick one of these networks to connect to
     * or reject the request by the app.
     * @hide
     */
    @SystemApi
    public interface NetworkRequestMatchCallback {
        /**
         * Invoked to register a callback to be invoked to convey user selection. The callback
         * object passed in this method is to be invoked by the UI component after the service sends
         * a list of matching scan networks using {@link #onMatch(List)} and user picks a network
         * from that list.
         *
         * @param userSelectionCallback Callback object to send back the user selection.
         */
        default void onUserSelectionCallbackRegistration(
                @NonNull NetworkRequestUserSelectionCallback userSelectionCallback) {}

        /**
         * Invoked when the active network request is aborted, either because
         * <li> The app released the request, OR</li>
         * <li> Request was overridden by a new request</li>
         * This signals the end of processing for the current request and should stop the UI
         * component. No subsequent calls from the UI component will be handled by the platform.
         */
        default void onAbort() {}

        /**
         * Invoked when a network request initiated by an app matches some networks in scan results.
         * This may be invoked multiple times for a single network request as the platform finds new
         * matching networks in scan results.
         *
         * @param scanResults List of {@link ScanResult} objects corresponding to the networks
         *                    matching the request.
         */
        default void onMatch(@NonNull List<ScanResult> scanResults) {}

        /**
         * Invoked on a successful connection with the network that the user selected
         * via {@link NetworkRequestUserSelectionCallback}.
         *
         * @param wifiConfiguration WifiConfiguration object corresponding to the network that the
         *                          user selected.
         */
        default void onUserSelectionConnectSuccess(@NonNull WifiConfiguration wifiConfiguration) {}

        /**
         * Invoked on failure to establish connection with the network that the user selected
         * via {@link NetworkRequestUserSelectionCallback}.
         *
         * @param wifiConfiguration WifiConfiguration object corresponding to the network
         *                          user selected.
         */
        default void onUserSelectionConnectFailure(@NonNull WifiConfiguration wifiConfiguration) {}
    }

    /**
     * Callback proxy for NetworkRequestUserSelectionCallback objects.
     * @hide
     */
    private class NetworkRequestUserSelectionCallbackProxy implements
            NetworkRequestUserSelectionCallback {
        private final INetworkRequestUserSelectionCallback mCallback;

        NetworkRequestUserSelectionCallbackProxy(
                INetworkRequestUserSelectionCallback callback) {
            mCallback = callback;
        }

        @Override
        public void select(@NonNull WifiConfiguration wifiConfiguration) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "NetworkRequestUserSelectionCallbackProxy: select "
                        + "wificonfiguration: " + wifiConfiguration);
            }
            try {
                mCallback.select(wifiConfiguration);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to invoke onSelected", e);
                throw e.rethrowFromSystemServer();
            }
        }

        @Override
        public void reject() {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "NetworkRequestUserSelectionCallbackProxy: reject");
            }
            try {
                mCallback.reject();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to invoke onRejected", e);
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Callback proxy for NetworkRequestMatchCallback objects.
     * @hide
     */
    private class NetworkRequestMatchCallbackProxy extends INetworkRequestMatchCallback.Stub {
        private final Executor mExecutor;
        private final NetworkRequestMatchCallback mCallback;

        NetworkRequestMatchCallbackProxy(Executor executor, NetworkRequestMatchCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onUserSelectionCallbackRegistration(
                INetworkRequestUserSelectionCallback userSelectionCallback) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "NetworkRequestMatchCallbackProxy: "
                        + "onUserSelectionCallbackRegistration callback: " + userSelectionCallback);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onUserSelectionCallbackRegistration(
                        new NetworkRequestUserSelectionCallbackProxy(userSelectionCallback));
            });
        }

        @Override
        public void onAbort() {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "NetworkRequestMatchCallbackProxy: onAbort");
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onAbort();
            });
        }

        @Override
        public void onMatch(List<ScanResult> scanResults) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "NetworkRequestMatchCallbackProxy: onMatch scanResults: "
                        + scanResults);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onMatch(scanResults);
            });
        }

        @Override
        public void onUserSelectionConnectSuccess(WifiConfiguration wifiConfiguration) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "NetworkRequestMatchCallbackProxy: onUserSelectionConnectSuccess "
                        + " wificonfiguration: " + wifiConfiguration);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onUserSelectionConnectSuccess(wifiConfiguration);
            });
        }

        @Override
        public void onUserSelectionConnectFailure(WifiConfiguration wifiConfiguration) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "NetworkRequestMatchCallbackProxy: onUserSelectionConnectFailure"
                        + " wificonfiguration: " + wifiConfiguration);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onUserSelectionConnectFailure(wifiConfiguration);
            });
        }
    }

    /**
     * Registers a callback for NetworkRequest matches. See {@link NetworkRequestMatchCallback}.
     * Caller can unregister a previously registered callback using
     * {@link #unregisterNetworkRequestMatchCallback(NetworkRequestMatchCallback)}
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#NETWORK_SETTINGS} permission. Callers
     * without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     *
     * @param executor The Executor on whose thread to execute the callbacks of the {@code callback}
     *                 object.
     * @param callback Callback for network match events to register.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void registerNetworkRequestMatchCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull NetworkRequestMatchCallback callback) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "registerNetworkRequestMatchCallback: callback=" + callback
                + ", executor=" + executor);

        try {
            synchronized (sNetworkRequestMatchCallbackMap) {
                INetworkRequestMatchCallback.Stub binderCallback =
                        new NetworkRequestMatchCallbackProxy(executor, callback);
                sNetworkRequestMatchCallbackMap.put(System.identityHashCode(callback),
                        binderCallback);
                mService.registerNetworkRequestMatchCallback(binderCallback);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Unregisters a callback for NetworkRequest matches. See {@link NetworkRequestMatchCallback}.
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#NETWORK_SETTINGS} permission. Callers
     * without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     *
     * @param callback Callback for network match events to unregister.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void unregisterNetworkRequestMatchCallback(
            @NonNull NetworkRequestMatchCallback callback) {
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "unregisterNetworkRequestMatchCallback: callback=" + callback);

        try {
            synchronized (sNetworkRequestMatchCallbackMap) {
                int callbackIdentifier = System.identityHashCode(callback);
                if (!sNetworkRequestMatchCallbackMap.contains(callbackIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + callbackIdentifier);
                    return;
                }
                mService.unregisterNetworkRequestMatchCallback(
                        sNetworkRequestMatchCallbackMap.get(callbackIdentifier));
                sNetworkRequestMatchCallbackMap.remove(callbackIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Privileged API to revoke all app state from wifi stack (equivalent to operations that the
     * wifi stack performs to clear state for an app that was uninstalled.
     * This removes:
     * <li> All saved networks or passpoint profiles added by the app </li>
     * <li> All previously approved peer to peer connection to access points initiated by the app
     * using {@link WifiNetworkSpecifier}</li>
     * <li> All network suggestions and approvals provided using {@link WifiNetworkSuggestion}</li>
     * <p>
     * @param targetAppUid UID of the app.
     * @param targetAppPackageName Package name of the app.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void removeAppState(int targetAppUid, @NonNull String targetAppPackageName) {
        try {
            mService.removeAppState(targetAppUid, targetAppPackageName);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Provide a list of network suggestions to the device. See {@link WifiNetworkSuggestion}
     * for a detailed explanation of the parameters.
     * When the device decides to connect to one of the provided network suggestions, platform sends
     * a directed broadcast {@link #ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION} to the app if
     * the network was created with
     * {@link WifiNetworkSuggestion.Builder#setIsAppInteractionRequired(boolean)} flag set and the
     * app holds {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION}
     * permission.
     *<p>
     * NOTE:
     * <ul>
     * <li> These networks are just a suggestion to the platform. The platform will ultimately
     * decide on which network the device connects to. </li>
     * <li> When an app is uninstalled or disabled, all its suggested networks are discarded.
     * If the device is currently connected to a suggested network which is being removed then the
     * device will disconnect from that network.</li>
     * <li> If user reset network settings, all added suggestions will be discarded. Apps can use
     * {@link #getNetworkSuggestions()} to check if their suggestions are in the device.</li>
     * <li> In-place modification of existing suggestions are allowed.</li>
     * <ul>
     * <li> If the provided suggestions include any previously provided suggestions by the app,
     * previous suggestions will be updated.</li>
     * <li>If one of the provided suggestions marks a previously unmetered suggestion as metered and
     * the device is currently connected to that suggested network, then the device will disconnect
     * from that network. The system will immediately re-evaluate all the network candidates
     * and possibly reconnect back to the same suggestion. This disconnect is to make sure that any
     * traffic flowing over unmetered networks isn't accidentally continued over a metered network.
     * </li>
     * <li>
     * On {@link android.os.Build.VERSION_CODES#TIRAMISU} or above If one of the provided
     * suggestions marks a previously trusted suggestion as untrusted and the device is currently
     * connected to that suggested network, then the device will disconnect from that network. The
     * system will immediately re-evaluate all the network candidates. This disconnect is to make
     * sure device will not remain connected to an untrusted network without a related
     * {@link android.net.NetworkRequest}.
     * </li>
     * </ul>
     * </ul>
     *
     * @param networkSuggestions List of network suggestions provided by the app.
     * @return Status code for the operation. One of the STATUS_NETWORK_SUGGESTIONS_ values.
     * @throws SecurityException if the caller is missing required permissions.
     * @see WifiNetworkSuggestion#equals(Object)
     */
    @RequiresPermission(CHANGE_WIFI_STATE)
    public @NetworkSuggestionsStatusCode int addNetworkSuggestions(
            @NonNull List<WifiNetworkSuggestion> networkSuggestions) {
        try {
            return mService.addNetworkSuggestions(
                    networkSuggestions, mContext.getOpPackageName(), mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove some or all of the network suggestions that were previously provided by the app.
     * If one of the suggestions being removed was used to establish connection to the current
     * network, then the device will immediately disconnect from that network. This method is same
     * as {@link #removeNetworkSuggestions(List, int)} with
     * {@link #ACTION_REMOVE_SUGGESTION_DISCONNECT}
     *
     * See {@link WifiNetworkSuggestion} for a detailed explanation of the parameters.
     * See {@link WifiNetworkSuggestion#equals(Object)} for the equivalence evaluation used.
     * <p></
     * Note: Use {@link #removeNetworkSuggestions(List, int)}. An {@code action} of
     * {@link #ACTION_REMOVE_SUGGESTION_DISCONNECT} is equivalent to the current behavior.
     *
     * @param networkSuggestions List of network suggestions to be removed. Pass an empty list
     *                           to remove all the previous suggestions provided by the app.
     * @return Status code for the operation. One of the {@code STATUS_NETWORK_SUGGESTIONS_*}
     * values. Any matching suggestions are removed from the device and will not be considered for
     * any further connection attempts.
     */
    @RequiresPermission(CHANGE_WIFI_STATE)
    public @NetworkSuggestionsStatusCode int removeNetworkSuggestions(
            @NonNull List<WifiNetworkSuggestion> networkSuggestions) {
        return removeNetworkSuggestions(networkSuggestions, ACTION_REMOVE_SUGGESTION_DISCONNECT);
    }

    /**
     * Remove some or all of the network suggestions that were previously provided by the app.
     * If one of the suggestions being removed was used to establish connection to the current
     * network, then the specified action will be executed.
     *
     * See {@link WifiNetworkSuggestion} for a detailed explanation of the parameters.
     * See {@link WifiNetworkSuggestion#equals(Object)} for the equivalence evaluation used.
     *
     * @param networkSuggestions List of network suggestions to be removed. Pass an empty list
     *                           to remove all the previous suggestions provided by the app.
     * @param action Desired action to execute after removing the suggestion. One of
     *               {@code ACTION_REMOVE_SUGGESTION_*}
     * @return Status code for the operation. One of the {@code STATUS_NETWORK_SUGGESTIONS_*}
     * values. Any matching suggestions are removed from the device and will not be considered for
     * further connection attempts.
     */
    @RequiresPermission(CHANGE_WIFI_STATE)
    public @NetworkSuggestionsStatusCode int removeNetworkSuggestions(
            @NonNull List<WifiNetworkSuggestion> networkSuggestions,
            @ActionAfterRemovingSuggestion int action) {
        try {
            return mService.removeNetworkSuggestions(networkSuggestions,
                    mContext.getOpPackageName(), action);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get all network suggestions provided by the calling app.
     * See {@link #addNetworkSuggestions(List)}
     * See {@link #removeNetworkSuggestions(List)}
     * @return a list of {@link WifiNetworkSuggestion}
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public @NonNull List<WifiNetworkSuggestion> getNetworkSuggestions() {
        try {
            return mService.getNetworkSuggestions(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Returns the max number of network suggestions that are allowed per app on the device.
     * @see #addNetworkSuggestions(List)
     * @see #removeNetworkSuggestions(List)
     */
    public int getMaxNumberOfNetworkSuggestionsPerApp() {
        return getMaxNumberOfNetworkSuggestionsPerApp(
                mContext.getSystemService(ActivityManager.class).isLowRamDevice());
    }

    /** @hide */
    public static int getMaxNumberOfNetworkSuggestionsPerApp(boolean isLowRamDevice) {
        return isLowRamDevice
                ? NETWORK_SUGGESTIONS_MAX_PER_APP_LOW_RAM
                : NETWORK_SUGGESTIONS_MAX_PER_APP_HIGH_RAM;
    }

    /**
     * Add or update a Passpoint configuration.  The configuration provides a credential
     * for connecting to Passpoint networks that are operated by the Passpoint
     * service provider specified in the configuration.
     *
     * Each configuration is uniquely identified by a unique key which depends on the contents of
     * the configuration. This allows the caller to install multiple profiles with the same FQDN
     * (Fully qualified domain name). Therefore, in order to update an existing profile, it is
     * first required to remove it using {@link WifiManager#removePasspointConfiguration(String)}.
     * Otherwise, a new profile will be added with both configuration.
     *
     * Deprecated for general app usage - except DO/PO apps.
     * See {@link WifiNetworkSuggestion.Builder#setPasspointConfig(PasspointConfiguration)} to
     * create a passpoint suggestion.
     * See {@link #addNetworkSuggestions(List)}, {@link #removeNetworkSuggestions(List)} for new
     * API to add Wi-Fi networks for consideration when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#R} or above, this API will always fail and throw
     * {@link IllegalArgumentException}.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     *
     * @param config The Passpoint configuration to be added
     * @throws IllegalArgumentException if configuration is invalid or Passpoint is not enabled on
     *                                  the device.
     */
    public void addOrUpdatePasspointConfiguration(PasspointConfiguration config) {
        try {
            if (!mService.addOrUpdatePasspointConfiguration(config, mContext.getOpPackageName())) {
                throw new IllegalArgumentException();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove the Passpoint configuration identified by its FQDN (Fully Qualified Domain Name) added
     * by the caller.
     *
     * @param fqdn The FQDN of the Passpoint configuration added by the caller to be removed
     * @throws IllegalArgumentException if no configuration is associated with the given FQDN or
     *                                  Passpoint is not enabled on the device.
     * @deprecated This will be non-functional in a future release.
     * <br>
     * Requires {@code android.Manifest.permission.NETWORK_SETTINGS} or
     * {@code android.Manifest.permission.NETWORK_CARRIER_PROVISIONING}.
     */
    @Deprecated
    public void removePasspointConfiguration(String fqdn) {
        try {
            if (!mService.removePasspointConfiguration(fqdn, mContext.getOpPackageName())) {
                throw new IllegalArgumentException();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the list of installed Passpoint configurations added by the caller.
     *
     * An empty list will be returned when no configurations are installed.
     *
     * @return A list of {@link PasspointConfiguration} added by the caller
     * @deprecated This will be non-functional in a future release.
     * <br>
     * Requires {@code android.Manifest.permission.NETWORK_SETTINGS} or
     * {@code android.Manifest.permission.NETWORK_SETUP_WIZARD}.
     */
    @Deprecated
    public List<PasspointConfiguration> getPasspointConfigurations() {
        try {
            return mService.getPasspointConfigurations(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Query for a Hotspot 2.0 release 2 OSU icon file. An {@link #ACTION_PASSPOINT_ICON} intent
     * will be broadcasted once the request is completed.  The presence of the intent extra
     * {@link #EXTRA_ICON} will indicate the result of the request.
     * A missing intent extra {@link #EXTRA_ICON} will indicate a failure.
     *
     * @param bssid The BSSID of the AP
     * @param fileName Name of the icon file (remote file) to query from the AP
     *
     * @throws UnsupportedOperationException if Passpoint is not enabled on the device.
     * @hide
     */
    public void queryPasspointIcon(long bssid, String fileName) {
        try {
            mService.queryPasspointIcon(bssid, fileName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Match the currently associated network against the SP matching the given FQDN
     * @param fqdn FQDN of the SP
     * @return ordinal [HomeProvider, RoamingProvider, Incomplete, None, Declined]
     * @hide
     */
    public int matchProviderWithCurrentNetwork(String fqdn) {
        try {
            return mService.matchProviderWithCurrentNetwork(fqdn);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove the specified network from the list of configured networks.
     * This may result in the asynchronous delivery of state change
     * events.
     *
     * Applications are not allowed to remove networks created by other
     * applications.
     *
     * @param netId the ID of the network as returned by {@link #addNetwork} or {@link
     *        #getConfiguredNetworks}.
     * @return {@code true} if the operation succeeded
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return
     * {@code false}.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    public boolean removeNetwork(int netId) {
        try {
            return mService.removeNetwork(netId, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove all configured networks that were not created by the calling app. Can only
     * be called by a Device Owner (DO) app.
     *
     * @return {@code true} if at least one network is removed, {@code false} otherwise
     * @throws SecurityException if the caller is not a Device Owner app
     */
    @RequiresPermission(CHANGE_WIFI_STATE)
    public boolean removeNonCallerConfiguredNetworks() {
        try {
            return mService.removeNonCallerConfiguredNetworks(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
    /**
     * Allow a previously configured network to be associated with. If
     * <code>attemptConnect</code> is true, an attempt to connect to the selected
     * network is initiated. This may result in the asynchronous delivery
     * of state change events.
     * <p>
     * <b>Note:</b> Network communication may not use Wi-Fi even if Wi-Fi is connected;
     * traffic may instead be sent through another network, such as cellular data,
     * Bluetooth tethering, or Ethernet. For example, traffic will never use a
     * Wi-Fi network that does not provide Internet access (e.g. a wireless
     * printer), if another network that does offer Internet access (e.g.
     * cellular data) is available. Applications that need to ensure that their
     * network traffic uses Wi-Fi should use APIs such as
     * {@link Network#bindSocket(java.net.Socket)},
     * {@link Network#openConnection(java.net.URL)}, or
     * {@link ConnectivityManager#bindProcessToNetwork} to do so.
     *
     * Applications are not allowed to enable networks created by other
     * applications.
     *
     * @param netId the ID of the network as returned by {@link #addNetwork} or {@link
     *        #getConfiguredNetworks}.
     * @param attemptConnect The way to select a particular network to connect to is specify
     *        {@code true} for this parameter.
     * @return {@code true} if the operation succeeded
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return
     * {@code false}.
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    public boolean enableNetwork(int netId, boolean attemptConnect) {
        try {
            return mService.enableNetwork(netId, attemptConnect, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Disable a configured network. The specified network will not be
     * a candidate for associating. This may result in the asynchronous
     * delivery of state change events.
     *
     * Applications are not allowed to disable networks created by other
     * applications.
     *
     * @param netId the ID of the network as returned by {@link #addNetwork} or {@link
     *        #getConfiguredNetworks}.
     * @return {@code true} if the operation succeeded
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return
     * {@code false}.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    public boolean disableNetwork(int netId) {
        try {
            return mService.disableNetwork(netId, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Disassociate from the currently active access point. This may result
     * in the asynchronous delivery of state change events.
     * @return {@code true} if the operation succeeded
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return
     * {@code false}.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    public boolean disconnect() {
        try {
            return mService.disconnect(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Reconnect to the currently active access point, if we are currently
     * disconnected. This may result in the asynchronous delivery of state
     * change events.
     * @return {@code true} if the operation succeeded
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return
     * {@code false}.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     */
    @Deprecated
    public boolean reconnect() {
        try {
            return mService.reconnect(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Reconnect to the currently active access point, even if we are already
     * connected. This may result in the asynchronous delivery of state
     * change events.
     * @return {@code true} if the operation succeeded
     *
     * @deprecated
     * a) See {@link WifiNetworkSpecifier.Builder#build()} for new
     * mechanism to trigger connection to a Wi-Fi network.
     * b) See {@link #addNetworkSuggestions(List)},
     * {@link #removeNetworkSuggestions(List)} for new API to add Wi-Fi networks for consideration
     * when auto-connecting to wifi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always return false.
     */
    @Deprecated
    public boolean reassociate() {
        try {
            return mService.reassociate(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check that the supplicant daemon is responding to requests.
     * @return {@code true} if we were able to communicate with the supplicant and
     * it returned the expected response to the PING message.
     * @deprecated Will return the output of {@link #isWifiEnabled()} instead.
     */
    @Deprecated
    public boolean pingSupplicant() {
        return isWifiEnabled();
    }

    /** @hide */
    public static final long WIFI_FEATURE_INFRA            = 1L << 0;  // Basic infrastructure mode
    /** @hide */
    public static final long WIFI_FEATURE_PASSPOINT        = 1L << 2;  // Support for GAS/ANQP
    /** @hide */
    public static final long WIFI_FEATURE_P2P              = 1L << 3;  // Wifi-Direct
    /** @hide */
    public static final long WIFI_FEATURE_MOBILE_HOTSPOT   = 1L << 4;  // Soft AP
    /** @hide */
    public static final long WIFI_FEATURE_SCANNER          = 1L << 5;  // WifiScanner APIs
    /** @hide */
    public static final long WIFI_FEATURE_AWARE            = 1L << 6;  // Wi-Fi Aware networking
    /** @hide */
    public static final long WIFI_FEATURE_D2D_RTT          = 1L << 7;  // Device-to-device RTT
    /** @hide */
    public static final long WIFI_FEATURE_D2AP_RTT         = 1L << 8;  // Device-to-AP RTT
    /** @hide */
    public static final long WIFI_FEATURE_PNO              = 1L << 10;  // Preferred network offload
    /** @hide */
    public static final long WIFI_FEATURE_TDLS             = 1L << 12; // Tunnel directed link setup
    /** @hide */
    public static final long WIFI_FEATURE_TDLS_OFFCHANNEL  = 1L << 13; // TDLS off channel
    /** @hide */
    public static final long WIFI_FEATURE_AP_STA           = 1L << 15; // AP STA Concurrency
    /** @hide */
    public static final long WIFI_FEATURE_LINK_LAYER_STATS = 1L << 16; // Link layer stats
    /** @hide */
    public static final long WIFI_FEATURE_LOGGER           = 1L << 17; // WiFi Logger
    /** @hide */
    public static final long WIFI_FEATURE_RSSI_MONITOR     = 1L << 19; // RSSI Monitor
    /** @hide */
    public static final long WIFI_FEATURE_MKEEP_ALIVE      = 1L << 20; // mkeep_alive
    /** @hide */
    public static final long WIFI_FEATURE_CONFIG_NDO       = 1L << 21; // ND offload
    /** @hide */
    public static final long WIFI_FEATURE_CONTROL_ROAMING  = 1L << 23; // Control firmware roaming
    /** @hide */
    public static final long WIFI_FEATURE_IE_WHITELIST     = 1L << 24; // Probe IE white listing
    /** @hide */
    public static final long WIFI_FEATURE_SCAN_RAND        = 1L << 25; // Random MAC & Probe seq
    /** @hide */
    public static final long WIFI_FEATURE_TX_POWER_LIMIT   = 1L << 26; // Set Tx power limit
    /** @hide */
    public static final long WIFI_FEATURE_WPA3_SAE         = 1L << 27; // WPA3-Personal SAE
    /** @hide */
    public static final long WIFI_FEATURE_WPA3_SUITE_B     = 1L << 28; // WPA3-Enterprise Suite-B
    /** @hide */
    public static final long WIFI_FEATURE_OWE              = 1L << 29; // Enhanced Open
    /** @hide */
    public static final long WIFI_FEATURE_LOW_LATENCY      = 1L << 30; // Low Latency modes
    /** @hide */
    public static final long WIFI_FEATURE_DPP              = 1L << 31; // DPP (Easy-Connect)
    /** @hide */
    public static final long WIFI_FEATURE_P2P_RAND_MAC     = 1L << 32; // Random P2P MAC
    /** @hide */
    public static final long WIFI_FEATURE_CONNECTED_RAND_MAC    = 1L << 33; // Random STA MAC
    /** @hide */
    public static final long WIFI_FEATURE_AP_RAND_MAC      = 1L << 34; // Random AP MAC
    /** @hide */
    public static final long WIFI_FEATURE_MBO              = 1L << 35; // MBO Support
    /** @hide */
    public static final long WIFI_FEATURE_OCE              = 1L << 36; // OCE Support
    /** @hide */
    public static final long WIFI_FEATURE_WAPI             = 1L << 37; // WAPI

    /** @hide */
    public static final long WIFI_FEATURE_FILS_SHA256      = 1L << 38; // FILS-SHA256

    /** @hide */
    public static final long WIFI_FEATURE_FILS_SHA384      = 1L << 39; // FILS-SHA384

    /** @hide */
    public static final long WIFI_FEATURE_SAE_PK           = 1L << 40; // SAE-PK

    /** @hide */
    public static final long WIFI_FEATURE_STA_BRIDGED_AP   = 1L << 41; // STA + Bridged AP

    /** @hide */
    public static final long WIFI_FEATURE_BRIDGED_AP       = 1L << 42; // Bridged AP

    /** @hide */
    public static final long WIFI_FEATURE_INFRA_60G        = 1L << 43; // 60 GHz Band Support

    /**
     * Support for 2 STA's for the local-only (peer to peer) connection + internet connection
     * concurrency.
     * @hide
     */
    public static final long WIFI_FEATURE_ADDITIONAL_STA_LOCAL_ONLY = 1L << 44;

    /**
     * Support for 2 STA's for the make before break concurrency.
     * @hide
     */
    public static final long WIFI_FEATURE_ADDITIONAL_STA_MBB = 1L << 45;

    /**
     * Support for 2 STA's for the restricted connection + internet connection concurrency.
     * @hide
     */
    public static final long WIFI_FEATURE_ADDITIONAL_STA_RESTRICTED = 1L << 46;

    /**
     * DPP (Easy-Connect) Enrollee Responder mode support
     * @hide
     */
    public static final long WIFI_FEATURE_DPP_ENROLLEE_RESPONDER = 1L << 47;

    /**
     * Passpoint Terms and Conditions feature support
     * @hide
     */
    public static final long WIFI_FEATURE_PASSPOINT_TERMS_AND_CONDITIONS = 1L << 48;

     /** @hide */
    public static final long WIFI_FEATURE_SAE_H2E          = 1L << 49; // Hash-to-Element

     /** @hide */
    public static final long WIFI_FEATURE_WFD_R2           = 1L << 50; // Wi-Fi Display R2

    /**
     * RFC 7542 decorated identity support
     * @hide */
    public static final long WIFI_FEATURE_DECORATED_IDENTITY = 1L << 51;

    /**
     * Trust On First Use support for WPA Enterprise network
     * @hide
     */
    public static final long WIFI_FEATURE_TRUST_ON_FIRST_USE = 1L << 52;

    /**
     * Support for 2 STA's multi internet concurrency.
     * @hide
     */
    public static final long WIFI_FEATURE_ADDITIONAL_STA_MULTI_INTERNET = 1L << 53;

    /**
     * Support for DPP (Easy-Connect) AKM.
     * @hide
     */
    public static final long WIFI_FEATURE_DPP_AKM = 1L << 54;

    /**
     * Support for setting TLS minimum version.
     * @hide
     */
    public static final long WIFI_FEATURE_SET_TLS_MINIMUM_VERSION = 1L << 55;

    /**
     * Support for TLS v.13.
     * @hide
     */
    public static final long WIFI_FEATURE_TLS_V1_3 = 1L << 56;

    /**
     * Support for Dual Band Simultaneous (DBS) operation.
     * @hide
     */
    public static final long WIFI_FEATURE_DUAL_BAND_SIMULTANEOUS = 1L << 57;
    /**
     * Support for TID-To-Link Mapping negotiation.
     * @hide
     */
    public static final long WIFI_FEATURE_T2LM_NEGOTIATION = 1L << 58;

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
     * @return true if this adapter supports Passpoint
     * @hide
     */
    public boolean isPasspointSupported() {
        return isFeatureSupported(WIFI_FEATURE_PASSPOINT);
    }

    /**
     * @return true if this adapter supports WifiP2pManager (Wi-Fi Direct)
     */
    public boolean isP2pSupported() {
        return isFeatureSupported(WIFI_FEATURE_P2P);
    }

    /**
     * @return true if this adapter supports portable Wi-Fi hotspot
     * @hide
     */
    @SystemApi
    public boolean isPortableHotspotSupported() {
        return isFeatureSupported(WIFI_FEATURE_MOBILE_HOTSPOT);
    }

    /**
     * @return true if this adapter supports WifiScanner APIs
     * @hide
     */
    @SystemApi
    public boolean isWifiScannerSupported() {
        return isFeatureSupported(WIFI_FEATURE_SCANNER);
    }

    /**
     * @return true if this adapter supports Neighbour Awareness Network APIs
     * @hide
     */
    public boolean isWifiAwareSupported() {
        return isFeatureSupported(WIFI_FEATURE_AWARE);
    }

    /**
     * Query whether or not the device supports Station (STA) + Access point (AP) concurrency.
     *
     * @return true if this device supports STA + AP concurrency, false otherwise.
     */
    public boolean isStaApConcurrencySupported() {
        return isFeatureSupported(WIFI_FEATURE_AP_STA);
    }

    /**
     * Query whether or not the device supports concurrent station (STA) connections for local-only
     * connections using {@link WifiNetworkSpecifier}.
     *
     * @return true if this device supports multiple STA concurrency for this use-case, false
     * otherwise.
     */
    public boolean isStaConcurrencyForLocalOnlyConnectionsSupported() {
        return isFeatureSupported(WIFI_FEATURE_ADDITIONAL_STA_LOCAL_ONLY);
    }

    /**
     * Query whether or not the device supports concurrent station (STA) connections for
     * make-before-break wifi to wifi switching.
     *
     * Note: This is an internal feature which is not available to apps.
     *
     * @return true if this device supports multiple STA concurrency for this use-case, false
     * otherwise.
     */
    public boolean isMakeBeforeBreakWifiSwitchingSupported() {
        return isFeatureSupported(WIFI_FEATURE_ADDITIONAL_STA_MBB);
    }

    /**
     * Query whether or not the device supports concurrent station (STA) connections for multi
     * internet connections.
     *
     * @return true if this device supports multiple STA concurrency for this use-case, false
     * otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public boolean isStaConcurrencyForMultiInternetSupported() {
        return isFeatureSupported(WIFI_FEATURE_ADDITIONAL_STA_MULTI_INTERNET);
    }

    /**
     * Query whether or not the device supports concurrent station (STA) connections for restricted
     * connections using {@link WifiNetworkSuggestion.Builder#setOemPaid(boolean)} /
     * {@link WifiNetworkSuggestion.Builder#setOemPrivate(boolean)}.
     *
     * @return true if this device supports multiple STA concurrency for this use-case, false
     * otherwise.
     * @hide
     */
    @SystemApi
    public boolean isStaConcurrencyForRestrictedConnectionsSupported() {
        return isFeatureSupported(WIFI_FEATURE_ADDITIONAL_STA_RESTRICTED);
    }

    /**
     * @deprecated Please use {@link android.content.pm.PackageManager#hasSystemFeature(String)}
     * with {@link android.content.pm.PackageManager#FEATURE_WIFI_RTT} and
     * {@link android.content.pm.PackageManager#FEATURE_WIFI_AWARE}.
     *
     * @return true if this adapter supports Device-to-device RTT
     * @hide
     */
    @Deprecated
    @SystemApi
    public boolean isDeviceToDeviceRttSupported() {
        return isFeatureSupported(WIFI_FEATURE_D2D_RTT);
    }

    /**
     * @deprecated Please use {@link android.content.pm.PackageManager#hasSystemFeature(String)}
     * with {@link android.content.pm.PackageManager#FEATURE_WIFI_RTT}.
     *
     * @return true if this adapter supports Device-to-AP RTT
     */
    @Deprecated
    public boolean isDeviceToApRttSupported() {
        return isFeatureSupported(WIFI_FEATURE_D2AP_RTT);
    }

    /**
     * @return true if this adapter supports offloaded connectivity scan
     */
    public boolean isPreferredNetworkOffloadSupported() {
        return isFeatureSupported(WIFI_FEATURE_PNO);
    }

    /**
     * @return true if this adapter supports Tunnel Directed Link Setup
     */
    public boolean isTdlsSupported() {
        return isFeatureSupported(WIFI_FEATURE_TDLS);
    }

    /**
     * @return true if this adapter supports Off Channel Tunnel Directed Link Setup
     * @hide
     */
    public boolean isOffChannelTdlsSupported() {
        return isFeatureSupported(WIFI_FEATURE_TDLS_OFFCHANNEL);
    }

    /**
     * @return true if this adapter supports advanced power/performance counters
     */
    public boolean isEnhancedPowerReportingSupported() {
        return isFeatureSupported(WIFI_FEATURE_LINK_LAYER_STATS);
    }

    /**
     * @return true if this device supports connected MAC randomization.
     * @hide
     */
    @SystemApi
    public boolean isConnectedMacRandomizationSupported() {
        return isFeatureSupported(WIFI_FEATURE_CONNECTED_RAND_MAC);
    }

    /**
     * @return true if this device supports AP MAC randomization.
     * @hide
     */
    @SystemApi
    public boolean isApMacRandomizationSupported() {
        return isFeatureSupported(WIFI_FEATURE_AP_RAND_MAC);
    }

    /**
     * Check if the chipset supports 2.4GHz band.
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public boolean is24GHzBandSupported() {
        try {
            return mService.is24GHzBandSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if the chipset supports 5GHz band.
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public boolean is5GHzBandSupported() {
        try {
            return mService.is5GHzBandSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if the chipset supports the 60GHz frequency band.
     *
     * @return {@code true} if supported, {@code false} otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean is60GHzBandSupported() {
        try {
            return mService.is60GHzBandSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if the chipset supports 6GHz band.
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public boolean is6GHzBandSupported() {
        try {
            return mService.is6GHzBandSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if the chipset supports a certain Wi-Fi standard.
     * @param standard the IEEE 802.11 standard to check on.
     *        valid values from {@link ScanResult}'s {@code WIFI_STANDARD_}
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public boolean isWifiStandardSupported(@WifiAnnotations.WifiStandard int standard) {
        try {
            return mService.isWifiStandardSupported(standard);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Query whether or not the device supports concurrency of Station (STA) + multiple access
     * points (AP) (where the APs bridged together).
     *
     * See {@link SoftApConfiguration.Builder#setBands(int[])}
     * or {@link SoftApConfiguration.Builder#setChannels(android.util.SparseIntArray)} to configure
     * bridged AP when the bridged AP supported.
     *
     * @return true if this device supports concurrency of STA + multiple APs which are bridged
     *         together, false otherwise.
     */
    public boolean isStaBridgedApConcurrencySupported() {
        return isFeatureSupported(WIFI_FEATURE_STA_BRIDGED_AP);
    }

    /**
     * Query whether or not the device supports multiple Access point (AP) which are bridged
     * together.
     *
     * See {@link SoftApConfiguration.Builder#setBands(int[])}
     * or {@link SoftApConfiguration.Builder#setChannels(android.util.SparseIntArray)} to configure
     * bridged AP when the bridged AP supported.
     *
     * @return true if this device supports concurrency of multiple AP which bridged together,
     *         false otherwise.
     */
    public boolean isBridgedApConcurrencySupported() {
        return isFeatureSupported(WIFI_FEATURE_BRIDGED_AP);
    }

    /**
     * Interface for Wi-Fi activity energy info listener. Should be implemented by applications and
     * set when calling {@link WifiManager#getWifiActivityEnergyInfoAsync}.
     *
     * @hide
     */
    @SystemApi
    public interface OnWifiActivityEnergyInfoListener {
        /**
         * Called when Wi-Fi activity energy info is available.
         * Note: this listener is triggered at most once for each call to
         * {@link #getWifiActivityEnergyInfoAsync}.
         *
         * @param info the latest {@link WifiActivityEnergyInfo}, or null if unavailable.
         */
        void onWifiActivityEnergyInfo(@Nullable WifiActivityEnergyInfo info);
    }

    private static class OnWifiActivityEnergyInfoProxy
            extends IOnWifiActivityEnergyInfoListener.Stub {
        private final Object mLock = new Object();
        @Nullable @GuardedBy("mLock") private Executor mExecutor;
        @Nullable @GuardedBy("mLock") private OnWifiActivityEnergyInfoListener mListener;

        OnWifiActivityEnergyInfoProxy(Executor executor,
                OnWifiActivityEnergyInfoListener listener) {
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public void onWifiActivityEnergyInfo(WifiActivityEnergyInfo info) {
            Executor executor;
            OnWifiActivityEnergyInfoListener listener;
            synchronized (mLock) {
                if (mExecutor == null || mListener == null) {
                    return;
                }
                executor = mExecutor;
                listener = mListener;
                // null out to allow garbage collection, prevent triggering listener more than once
                mExecutor = null;
                mListener = null;
            }
            Binder.clearCallingIdentity();
            executor.execute(() -> listener.onWifiActivityEnergyInfo(info));
        }
    }

    /**
     * Request to get the current {@link WifiActivityEnergyInfo} asynchronously.
     * Note: This method will return null if {@link #isEnhancedPowerReportingSupported()} returns
     * false.
     *
     * @param executor the executor that the listener will be invoked on
     * @param listener the listener that will receive the {@link WifiActivityEnergyInfo} object
     *                 when it becomes available. The listener will be triggered at most once for
     *                 each call to this method.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void getWifiActivityEnergyInfoAsync(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OnWifiActivityEnergyInfoListener listener) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        try {
            mService.getWifiActivityEnergyInfoAsync(
                    new OnWifiActivityEnergyInfoProxy(executor, listener));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Request a scan for access points. Returns immediately. The availability
     * of the results is made known later by means of an asynchronous event sent
     * on completion of the scan.
     * <p>
     * To initiate a Wi-Fi scan, declare the
     * {@link android.Manifest.permission#CHANGE_WIFI_STATE}
     * permission in the manifest, and perform these steps:
     * </p>
     * <ol style="1">
     * <li>Invoke the following method:
     * {@code ((WifiManager) getSystemService(WIFI_SERVICE)).startScan()}</li>
     * <li>
     * Register a BroadcastReceiver to listen to
     * {@code SCAN_RESULTS_AVAILABLE_ACTION}.</li>
     * <li>When a broadcast is received, call:
     * {@code ((WifiManager) getSystemService(WIFI_SERVICE)).getScanResults()}</li>
     * </ol>
     * @return {@code true} if the operation succeeded, i.e., the scan was initiated.
     * @deprecated The ability for apps to trigger scan requests will be removed in a future
     * release.
     */
    @Deprecated
    public boolean startScan() {
        return startScan(null);
    }

    /** @hide */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.UPDATE_DEVICE_STATS)
    public boolean startScan(WorkSource workSource) {
        try {
            String packageName = mContext.getOpPackageName();
            String attributionTag = mContext.getAttributionTag();
            return mService.startScan(packageName, attributionTag);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * WPS has been deprecated from Client mode operation.
     *
     * @return null
     * @hide
     * @deprecated This API is deprecated
     */
    public String getCurrentNetworkWpsNfcConfigurationToken() {
        return null;
    }

    /**
     * Return dynamic information about the current Wi-Fi connection, if any is active.
     * <p>
     *
     * @return the Wi-Fi information, contained in {@link WifiInfo}.
     *
     * @deprecated Starting with {@link Build.VERSION_CODES#S}, WifiInfo retrieval is moved to
     * {@link ConnectivityManager} API surface. WifiInfo is attached in
     * {@link NetworkCapabilities#getTransportInfo()} which is available via callback in
     * {@link NetworkCallback#onCapabilitiesChanged(Network, NetworkCapabilities)} or on-demand from
     * {@link ConnectivityManager#getNetworkCapabilities(Network)}.
     *
     *</p>
     * Usage example:
     * <pre>
     * final NetworkRequest request =
     *      new NetworkRequest.Builder()
     *      .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
     *      .build();
     * final ConnectivityManager connectivityManager =
     *      context.getSystemService(ConnectivityManager.class);
     * final NetworkCallback networkCallback = new NetworkCallback() {
     *      ...
     *      &#64;Override
     *      void onAvailable(Network network) {}
     *
     *      &#64;Override
     *      void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
     *          WifiInfo wifiInfo = (WifiInfo) networkCapabilities.getTransportInfo();
     *      }
     *      // etc.
     * };
     * connectivityManager.requestNetwork(request, networkCallback); // For request
     * connectivityManager.registerNetworkCallback(request, networkCallback); // For listen
     * </pre>
     * <p>
     * <b>Compatibility Notes:</b>
     * <li>Apps can continue using this API, however newer features
     * such as ability to mask out location sensitive data in WifiInfo will not be supported
     * via this API. </li>
     * <li>On devices supporting concurrent connections (indicated via
     * {@link #isStaConcurrencyForLocalOnlyConnectionsSupported()}, etc) this API will return
     * the details of the internet providing connection (if any) to all apps, except for the apps
     * that triggered the creation of the concurrent connection. For such apps, this API will return
     * the details of the connection they created. e.g. apps using {@link WifiNetworkSpecifier} will
     * trigger a concurrent connection on supported devices and hence this API will provide
     * details of their peer to peer connection (not the internet providing connection). This
     * is to maintain backwards compatibility with behavior on single STA devices.</li>
     * </p>
     */
    @Deprecated
    @RequiresPermission(allOf = {ACCESS_WIFI_STATE, ACCESS_FINE_LOCATION}, conditional = true)
    public WifiInfo getConnectionInfo() {
        try {
            return mService.getConnectionInfo(mContext.getOpPackageName(),
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the results of the latest access point scan.
     * @return the list of access points found in the most recent scan. An app must hold
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION} permission
     * and {@link android.Manifest.permission#ACCESS_WIFI_STATE} permission
     * in order to get valid results.
     */
    @RequiresPermission(allOf = {ACCESS_WIFI_STATE, ACCESS_FINE_LOCATION})
    public List<ScanResult> getScanResults() {
        try {
            return mService.getScanResults(mContext.getOpPackageName(),
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the filtered ScanResults which match the network configurations specified by the
     * {@code networkSuggestionsToMatch}. Suggestions which use {@link WifiConfiguration} use
     * SSID and the security type to match. Suggestions which use {@link PasspointConfigration}
     * use the matching rules of Hotspot 2.0.
     * @param networkSuggestionsToMatch The list of {@link WifiNetworkSuggestion} to match against.
     * These may or may not be suggestions which are installed on the device.
     * @param scanResults The scan results to be filtered. Optional - if not provided(empty list),
     * the Wi-Fi service will use the most recent scan results which the system has.
     * @return The map of {@link WifiNetworkSuggestion} to the list of {@link ScanResult}
     * corresponding to networks which match them.
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    @NonNull
    public Map<WifiNetworkSuggestion, List<ScanResult>> getMatchingScanResults(
            @NonNull List<WifiNetworkSuggestion> networkSuggestionsToMatch,
            @Nullable List<ScanResult> scanResults) {
        if (networkSuggestionsToMatch == null) {
            throw new IllegalArgumentException("networkSuggestions must not be null.");
        }
        try {
            return mService.getMatchingScanResults(
                    networkSuggestionsToMatch, scanResults,
                    mContext.getOpPackageName(), mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set if scanning is always available.
     *
     * If set to {@code true}, apps can issue {@link #startScan} and fetch scan results
     * even when Wi-Fi is turned off.
     *
     * @param isAvailable true to enable, false to disable.
     * @hide
     * @see #isScanAlwaysAvailable()
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void setScanAlwaysAvailable(boolean isAvailable) {
        try {
            mService.setScanAlwaysAvailable(isAvailable, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if scanning is always available.
     *
     * If this return {@code true}, apps can issue {@link #startScan} and fetch scan results
     * even when Wi-Fi is turned off.
     *
     * To change this setting, see {@link #ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE}.
     * @deprecated The ability for apps to trigger scan requests will be removed in a future
     * release.
     */
    @Deprecated
    public boolean isScanAlwaysAvailable() {
        try {
            return mService.isScanAlwaysAvailable();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get channel data such as the number of APs found on each channel from the most recent scan.
     * App requires {@link android.Manifest.permission#NEARBY_WIFI_DEVICES}
     *
     * @param executor        The executor on which callback will be invoked.
     * @param resultsCallback A callback that will return {@code List<Bundle>} containing channel
     *                       data such as the number of APs found on each channel.
     *                       {@link WifiManager#CHANNEL_DATA_KEY_FREQUENCY_MHZ} and
     *                       {@link WifiManager#CHANNEL_DATA_KEY_NUM_AP} are used to get
     *                       the frequency (Mhz) and number of APs.
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws SecurityException             if the caller does not have permission.
     * @throws NullPointerException          if the caller provided invalid inputs.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(NEARBY_WIFI_DEVICES)
    public void getChannelData(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<List<Bundle>> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                    mContext.getAttributionSource());
            mService.getChannelData(new IListListener.Stub() {
                @Override
                public void onResult(List value) {
                    Binder.clearCallingIdentity();
                    executor.execute(() -> {
                        resultsCallback.accept(value);
                    });
                }
            }, mContext.getOpPackageName(), extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Tell the device to persist the current list of configured networks.
     * <p>
     * Note: It is possible for this method to change the network IDs of
     * existing networks. You should assume the network IDs can be different
     * after calling this method.
     *
     * @return {@code false}.
     * @deprecated There is no need to call this method -
     * {@link #addNetwork(WifiConfiguration)}, {@link #updateNetwork(WifiConfiguration)}
     * and {@link #removeNetwork(int)} already persist the configurations automatically.
     */
    @Deprecated
    public boolean saveConfiguration() {
        return false;
    }

    /**
     * Helper class to support driver country code changed listener.
     */
    private static class OnDriverCountryCodeChangedProxy
            extends IOnWifiDriverCountryCodeChangedListener.Stub {

        @NonNull private Executor mExecutor;
        @NonNull private ActiveCountryCodeChangedCallback mCallback;

        OnDriverCountryCodeChangedProxy(@NonNull Executor executor,
                @NonNull ActiveCountryCodeChangedCallback callback) {
            Objects.requireNonNull(executor);
            Objects.requireNonNull(callback);
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onDriverCountryCodeChanged(String countryCode) {
            Log.i(TAG, "OnDriverCountryCodeChangedProxy: receive onDriverCountryCodeChanged: "
                    + countryCode);
            Binder.clearCallingIdentity();
            if (countryCode != null) {
                mExecutor.execute(() -> mCallback.onActiveCountryCodeChanged(countryCode));
            } else {
                mExecutor.execute(() -> mCallback.onCountryCodeInactive());
            }
        }
    }

    /**
     * Interface used to listen the active country code changed event.
     * @hide
     */
    @SystemApi
    public interface ActiveCountryCodeChangedCallback {
        /**
         * Called when the country code used by the Wi-Fi subsystem has changed.
         *
         * @param countryCode An ISO-3166-alpha2 country code which is 2-Character alphanumeric.
         */
        void onActiveCountryCodeChanged(@NonNull String countryCode);

        /**
         * Called when the Wi-Fi subsystem does not have an active country code.
         * This can happen when Wi-Fi is disabled.
         */
        void onCountryCodeInactive();
    }

    /**
     * Add the provided callback for the active country code changed event.
     * Caller will receive either
     * {@link WifiManager.ActiveCountryCodeChangedCallback#onActiveCountryCodeChanged(String)}
     * or {@link WifiManager.ActiveCountryCodeChangedCallback#onCountryCodeInactive()}
     * on registration.
     *
     * Note: When the global location setting is off or the caller does not have runtime location
     * permission, caller will not receive the callback even if caller register callback succeeded.
     *
     *
     * Caller can remove a previously registered callback using
     * {@link WifiManager#unregisterActiveCountryCodeChangedCallback(
     * ActiveCountryCodeChangedCallback)}.
     *
     * <p>
     * Note:
     * The value provided by
     * {@link WifiManager.ActiveCountryCodeChangedCallback#onActiveCountryCodeChanged(String)}
     * may be different from the returned value from {@link WifiManager#getCountryCode()} even if
     * the Wi-Fi subsystem is active. See: {@link WifiManager#getCountryCode()} for details.
     * </p>
     *
     * @param executor The Executor on which to execute the callbacks.
     * @param callback callback for the driver country code changed events.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    public void registerActiveCountryCodeChangedCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull ActiveCountryCodeChangedCallback callback) {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.d(TAG, "registerActiveCountryCodeChangedCallback: callback=" + callback
                    + ", executor=" + executor);
        }
        final int callbackIdentifier = System.identityHashCode(callback);
        synchronized (sActiveCountryCodeChangedCallbackMap) {
            try {
                IOnWifiDriverCountryCodeChangedListener.Stub binderListener =
                        new OnDriverCountryCodeChangedProxy(executor, callback);
                sActiveCountryCodeChangedCallbackMap.put(callbackIdentifier,
                        binderListener);
                mService.registerDriverCountryCodeChangedListener(binderListener,
                        mContext.getOpPackageName(), mContext.getAttributionTag());
            } catch (RemoteException e) {
                sActiveCountryCodeChangedCallbackMap.remove(callbackIdentifier);
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Allow callers to remove a previously registered listener. After calling this method,
     * applications will no longer receive the active country code changed events through that
     * callback.
     *
     * @param callback Callback to remove the active country code changed events.
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SystemApi
    public void unregisterActiveCountryCodeChangedCallback(
            @NonNull ActiveCountryCodeChangedCallback callback) {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        if (callback == null) throw new IllegalArgumentException("Callback cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.d(TAG, "unregisterActiveCountryCodeChangedCallback: callback=" + callback);
        }
        final int callbackIdentifier = System.identityHashCode(callback);
        synchronized (sActiveCountryCodeChangedCallbackMap) {
            try {
                if (!sActiveCountryCodeChangedCallbackMap.contains(callbackIdentifier)) {
                    Log.w(TAG, "Unknown external listener " + callbackIdentifier);
                    return;
                }
                mService.unregisterDriverCountryCodeChangedListener(
                        sActiveCountryCodeChangedCallbackMap.get(callbackIdentifier));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } finally {
                sActiveCountryCodeChangedCallbackMap.remove(callbackIdentifier);
            }
        }
    }

    /**
     * Interface used to listen to changes in current network state.
     * @hide
     */
    @SystemApi
    public interface WifiNetworkStateChangedListener {
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = {"WIFI_ROLE_CLIENT_"}, value = {
                WIFI_ROLE_CLIENT_PRIMARY,
                WIFI_ROLE_CLIENT_SECONDARY_INTERNET,
                WIFI_ROLE_CLIENT_SECONDARY_LOCAL_ONLY
        })
        @interface WifiClientModeRole {}

        /**
         * A client mode role returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * Represents the primary Client Mode Manager which is mostly used for internet, but could
         * also be used for other use-cases such as local only connections.
         **/
        int WIFI_ROLE_CLIENT_PRIMARY = 1;
        /**
         * A client mode role returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * Represents a Client Mode Manager dedicated for the secondary internet use-case.
         **/
        int WIFI_ROLE_CLIENT_SECONDARY_INTERNET = 2;
        /**
         * A client mode role returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * Represents a Client Mode Manager dedicated for the local only connection use-case.
         **/
        int WIFI_ROLE_CLIENT_SECONDARY_LOCAL_ONLY = 3;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = {"WIFI_NETWORK_STATUS_"}, value = {
                WIFI_NETWORK_STATUS_IDLE,
                WIFI_NETWORK_STATUS_SCANNING,
                WIFI_NETWORK_STATUS_CONNECTING,
                WIFI_NETWORK_STATUS_AUTHENTICATING,
                WIFI_NETWORK_STATUS_OBTAINING_IPADDR,
                WIFI_NETWORK_STATUS_CONNECTED,
                WIFI_NETWORK_STATUS_DISCONNECTED,
                WIFI_NETWORK_STATUS_FAILED
        })
        @interface WifiNetworkState {}

        /**
         * A state returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * Supplicant is in uninitialized state.
         **/
        int WIFI_NETWORK_STATUS_IDLE = 1;
        /**
         * A state returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * Supplicant is scanning.
         **/
        int WIFI_NETWORK_STATUS_SCANNING = 2;
        /**
         * A state returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * L2 connection is in progress.
         **/
        int WIFI_NETWORK_STATUS_CONNECTING = 3;
        /**
         * A state returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * L2 connection 4 way handshake.
         **/
        int WIFI_NETWORK_STATUS_AUTHENTICATING = 4;
        /**
         * A state returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * L2 connection complete. Obtaining IP address.
         **/
        int WIFI_NETWORK_STATUS_OBTAINING_IPADDR = 5;
        /**
         * A state returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * L3 connection is complete.
         **/
        int WIFI_NETWORK_STATUS_CONNECTED = 6;
        /**
         * A state returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * Network disconnected.
         **/
        int WIFI_NETWORK_STATUS_DISCONNECTED = 7;
        /**
         * A state returned by {@link #onWifiNetworkStateChanged(int, int)}.
         * A pseudo-state that should normally never be seen.
         **/
        int WIFI_NETWORK_STATUS_FAILED = 8;


        /**
         * Provides network state changes per client mode role.
         * @param cmmRole the role of the wifi client mode manager having the state change.
         *                One of {@link WifiClientModeRole}.
         * @param state the wifi network state specified by one of {@link WifiNetworkState}.
         */
        void onWifiNetworkStateChanged(@WifiClientModeRole int cmmRole,
                @WifiNetworkState int state);
    }

    /**
     * Helper class to support wifi network state changed listener.
     */
    private static class OnWifiNetworkStateChangedProxy
            extends IWifiNetworkStateChangedListener.Stub {

        @NonNull private Executor mExecutor;
        @NonNull private WifiNetworkStateChangedListener mListener;

        OnWifiNetworkStateChangedProxy(@NonNull Executor executor,
                @NonNull WifiNetworkStateChangedListener listener) {
            Objects.requireNonNull(executor);
            Objects.requireNonNull(listener);
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public void onWifiNetworkStateChanged(int cmmRole, int state) {
            Log.i(TAG, "OnWifiNetworkStateChangedProxy: onWifiNetworkStateChanged: "
                    + cmmRole + ", " + state);
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mListener.onWifiNetworkStateChanged(cmmRole, state));
        }
    }

    /**
     * Add a listener to listen to Wi-Fi network state changes on available client mode roles
     * specified in {@link WifiNetworkStateChangedListener.WifiClientModeRole}.
     * When wifi state changes such as connected/disconnect happens, results will be delivered via
     * {@link WifiNetworkStateChangedListener#onWifiNetworkStateChanged(int, int)}.
     *
     * @param executor The Executor on which to execute the callbacks.
     * @param listener listener for the network status updates.
     * @throws SecurityException if the caller is missing required permissions.
     * @throws IllegalArgumentException if incorrect input arguments are provided.
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.NETWORK_SETTINGS)
    public void addWifiNetworkStateChangedListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull WifiNetworkStateChangedListener listener) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.d(TAG, "addWifiNetworkStateChangedListener: listener=" + listener
                    + ", executor=" + executor);
        }
        final int listenerIdentifier = System.identityHashCode(listener);
        synchronized (sOnWifiNetworkStateChangedListenerMap) {
            try {
                IWifiNetworkStateChangedListener.Stub listenerProxy =
                        new OnWifiNetworkStateChangedProxy(executor, listener);
                sOnWifiNetworkStateChangedListenerMap.put(listenerIdentifier,
                        listenerProxy);
                mService.addWifiNetworkStateChangedListener(listenerProxy);
            } catch (RemoteException e) {
                sOnWifiNetworkStateChangedListenerMap.remove(listenerIdentifier);
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Remove a listener added using
     * {@link #addWifiNetworkStateChangedListener(Executor, WifiNetworkStateChangedListener)}.
     * @param listener the listener to be removed.
     * @throws IllegalArgumentException if incorrect input arguments are provided.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public void removeWifiNetworkStateChangedListener(
            @NonNull WifiNetworkStateChangedListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.d(TAG, "removeWifiNetworkStateChangedListener: listener=" + listener);
        }
        final int listenerIdentifier = System.identityHashCode(listener);
        synchronized (sOnWifiNetworkStateChangedListenerMap) {
            try {
                if (!sOnWifiNetworkStateChangedListenerMap.contains(listenerIdentifier)) {
                    Log.w(TAG, "Unknown external listener " + listenerIdentifier);
                    return;
                }
                mService.removeWifiNetworkStateChangedListener(
                        sOnWifiNetworkStateChangedListenerMap.get(listenerIdentifier));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } finally {
                sOnWifiNetworkStateChangedListenerMap.remove(listenerIdentifier);
            }
        }
    }

    /**
     * Get the country code as resolved by the Wi-Fi framework.
     * The Wi-Fi framework uses multiple sources to resolve a country code
     * - in order of priority (high to low):
     * 1. Override country code set by {@link WifiManager#setOverrideCountryCode(String)}
     * and cleared by {@link WifiManager#clearOverrideCountryCode()}. Typically only used
     * for testing.
     * 2. Country code supplied by the telephony module. Typically provided from the
     * current network or from emergency cell information.
     * 3. Country code supplied by the wifi driver module. (802.11d)
     * 4. Default country code set either via {@code ro.boot.wificountrycode}
     * or the {@link WifiManager#setDefaultCountryCode(String)}.
     *
     * <p>
     * Note:
     * This method returns the Country Code value used by the framework - even if not currently
     * used by the Wi-Fi subsystem. I.e. the returned value from this API may be different from the
     * value provided by
     * {@link WifiManager.ActiveCountryCodeChangedCallback#onActiveCountryCodeChanged(String)}.
     * Such a difference may happen when there is an ongoing network connection (STA, AP, Direct,
     * or Aware) and the Wi-Fi subsystem does not support dynamic updates - at that point the
     * framework may defer setting the Country Code to the Wi-Fi subsystem.
     * </p>
     * @return the country code in ISO 3166 alpha-2 (2-letter) upper format,
     * or null if there is no country code configured.
     *
     * @hide
     */
    @Nullable
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    })
    public String getCountryCode() {
        try {
            return mService.getCountryCode(mContext.getOpPackageName(),
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set the override country code - may be used for testing. See the country code resolution
     * order and format in {@link #getCountryCode()}.
     * @param country A 2-Character alphanumeric country code.
     * @see #getCountryCode().
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_COUNTRY_CODE)
    public void setOverrideCountryCode(@NonNull String country) {
        try {
            mService.setOverrideCountryCode(country);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * This clears the override country code which was previously set by
     * {@link WifiManager#setOverrideCountryCode(String)} method.
     * @see #getCountryCode().
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_COUNTRY_CODE)
    public void clearOverrideCountryCode() {
        try {
            mService.clearOverrideCountryCode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
    /**
     * Used to configure the default country code. See {@link #getCountryCode()} for resolution
     * method of the country code.
     * @param country A 2-character alphanumeric country code.
     * @see #getCountryCode().
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_COUNTRY_CODE)
    public void setDefaultCountryCode(@NonNull String country) {
        try {
            mService.setDefaultCountryCode(country);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the DHCP-assigned addresses from the last successful DHCP request,
     * if any.
     *
     * @return the DHCP information
     *
     * @deprecated Use the methods on {@link android.net.LinkProperties} which can be obtained
     * either via {@link NetworkCallback#onLinkPropertiesChanged(Network, LinkProperties)} or
     * {@link ConnectivityManager#getLinkProperties(Network)}.
     *
     * <p>
     * <b>Compatibility Notes:</b>
     * <li>On devices supporting concurrent connections (indicated via
     * {@link #isStaConcurrencyForLocalOnlyConnectionsSupported()}, etc), this API will return
     * the details of the internet providing connection (if any) to all apps, except for the apps
     * that triggered the creation of the concurrent connection. For such apps, this API will return
     * the details of the connection they created. e.g. apps using {@link WifiNetworkSpecifier} will
     * trigger a concurrent connection on supported devices and hence this API will provide
     * details of their peer to peer connection (not the internet providing connection). This
     * is to maintain backwards compatibility with behavior on single STA devices.</li>
     * </p>
     */
    @Deprecated
    public DhcpInfo getDhcpInfo() {
        try {
            return mService.getDhcpInfo(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable or disable Wi-Fi.
     * <p>
     * Applications must have the {@link android.Manifest.permission#CHANGE_WIFI_STATE}
     * permission to toggle wifi.
     *
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @return {@code false} if the request cannot be satisfied; {@code true} indicates that wifi is
     *         either already in the requested state, or in progress toward the requested state.
     * @throws  SecurityException if the caller is missing required permissions.
     *
     * @deprecated Starting with Build.VERSION_CODES#Q, applications are not allowed to
     * enable/disable Wi-Fi.
     * <b>Compatibility Note:</b> For applications targeting
     * {@link android.os.Build.VERSION_CODES#Q} or above, this API will always fail and return
     * {@code false}. If apps are targeting an older SDK ({@link android.os.Build.VERSION_CODES#P}
     * or below), they can continue to use this API.
     * <p>
     * Deprecation Exemptions:
     * <ul>
     * <li>Device Owner (DO), Profile Owner (PO) and system apps.
     * </ul>
     *
     * Starting with {@link android.os.Build.VERSION_CODES#TIRAMISU}, DO/COPE may set
     * a user restriction (DISALLOW_CHANGE_WIFI_STATE) to only allow DO/PO to use this API.
     */
    @Deprecated
    public boolean setWifiEnabled(boolean enabled) {
        try {
            return mService.setWifiEnabled(mContext.getOpPackageName(), enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Abstract callback class for applications to receive updates about the Wi-Fi subsystem
     * restarting. The Wi-Fi subsystem can restart due to internal recovery mechanisms or via user
     * action.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public abstract static class SubsystemRestartTrackingCallback {
        private final SubsystemRestartTrackingCallback.SubsystemRestartCallbackProxy mProxy;

        public SubsystemRestartTrackingCallback() {
            mProxy = new SubsystemRestartTrackingCallback.SubsystemRestartCallbackProxy();
        }

        /*package*/ @NonNull
        SubsystemRestartTrackingCallback.SubsystemRestartCallbackProxy getProxy() {
            return mProxy;
        }

        /**
         * Indicates that the Wi-Fi subsystem is about to restart.
         */
        public abstract void onSubsystemRestarting();

        /**
         * Indicates that the Wi-Fi subsystem has restarted.
         */
        public abstract void onSubsystemRestarted();

        private static class SubsystemRestartCallbackProxy extends ISubsystemRestartCallback.Stub {
            private final Object mLock = new Object();
            @Nullable
            @GuardedBy("mLock")
            private Executor mExecutor;
            @Nullable
            @GuardedBy("mLock")
            private SubsystemRestartTrackingCallback mCallback;

            SubsystemRestartCallbackProxy() {
                mExecutor = null;
                mCallback = null;
            }

            /*package*/ void initProxy(@NonNull Executor executor,
                    @NonNull SubsystemRestartTrackingCallback callback) {
                synchronized (mLock) {
                    mExecutor = executor;
                    mCallback = callback;
                }
            }

            /*package*/ void cleanUpProxy() {
                synchronized (mLock) {
                    mExecutor = null;
                    mCallback = null;
                }
            }

            @Override
            public void onSubsystemRestarting() {
                Executor executor;
                SubsystemRestartTrackingCallback callback;
                synchronized (mLock) {
                    executor = mExecutor;
                    callback = mCallback;
                }
                if (executor == null || callback == null) {
                    return;
                }
                Binder.clearCallingIdentity();
                executor.execute(callback::onSubsystemRestarting);
            }

            @Override
            public void onSubsystemRestarted() {
                Executor executor;
                SubsystemRestartTrackingCallback callback;
                synchronized (mLock) {
                    executor = mExecutor;
                    callback = mCallback;
                }
                if (executor == null || callback == null) {
                    return;
                }
                Binder.clearCallingIdentity();
                executor.execute(callback::onSubsystemRestarted);
            }
        }
    }

    /**
     * Registers a {@link SubsystemRestartTrackingCallback} to listen to Wi-Fi subsystem restarts.
     * The subsystem may restart due to internal recovery mechanisms or via user action.
     *
     * @see #unregisterSubsystemRestartTrackingCallback(SubsystemRestartTrackingCallback)
     *
     * @param executor Executor to execute callback on
     * @param callback {@link SubsystemRestartTrackingCallback} to register
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public void registerSubsystemRestartTrackingCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull SubsystemRestartTrackingCallback callback) {
        if (executor == null) throw new IllegalArgumentException("executor must not be null");
        if (callback == null) throw new IllegalArgumentException("callback must not be null");
        SubsystemRestartTrackingCallback.SubsystemRestartCallbackProxy proxy = callback.getProxy();
        proxy.initProxy(executor, callback);
        try {
            mService.registerSubsystemRestartCallback(proxy);
        } catch (RemoteException e) {
            proxy.cleanUpProxy();
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Unregisters a {@link SubsystemRestartTrackingCallback} registered with
     * {@link #registerSubsystemRestartTrackingCallback(Executor, SubsystemRestartTrackingCallback)}
     *
     * @param callback {@link SubsystemRestartTrackingCallback} to unregister
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public void unregisterSubsystemRestartTrackingCallback(
            @NonNull SubsystemRestartTrackingCallback callback) {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");
        SubsystemRestartTrackingCallback.SubsystemRestartCallbackProxy proxy = callback.getProxy();
        try {
            mService.unregisterSubsystemRestartCallback(proxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            proxy.cleanUpProxy();
        }
    }

    /**
     * Restart the Wi-Fi subsystem.
     *
     * Restarts the Wi-Fi subsystem - effectively disabling it and re-enabling it. All existing
     * Access Point (AP) associations are torn down, all Soft APs are disabled, Wi-Fi Direct and
     * Wi-Fi Aware are disabled.
     *
     * The state of the system after restart is not guaranteed to match its state before the API is
     * called - for instance the device may associate to a different Access Point (AP), and tethered
     * hotspots may or may not be restored.
     *
     * Use the
     * {@link #registerSubsystemRestartTrackingCallback(Executor, SubsystemRestartTrackingCallback)}
     * to track the operation.
     *
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @SystemApi
    @RequiresPermission(android.Manifest.permission.RESTART_WIFI_SUBSYSTEM)
    public void restartWifiSubsystem() {
        try {
            mService.restartWifiSubsystem();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets the Wi-Fi enabled state.
     * @return One of {@link #WIFI_STATE_DISABLED},
     *         {@link #WIFI_STATE_DISABLING}, {@link #WIFI_STATE_ENABLED},
     *         {@link #WIFI_STATE_ENABLING}, {@link #WIFI_STATE_UNKNOWN}
     * @see #isWifiEnabled()
     */
    public int getWifiState() {
        try {
            return mService.getWifiEnabledState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return whether Wi-Fi is enabled or disabled.
     * @return {@code true} if Wi-Fi is enabled
     * @see #getWifiState()
     */
    public boolean isWifiEnabled() {
        return getWifiState() == WIFI_STATE_ENABLED;
    }

    /**
     * Calculates the level of the signal. This should be used any time a signal
     * is being shown.
     *
     * @param rssi The power of the signal measured in RSSI.
     * @param numLevels The number of levels to consider in the calculated level.
     * @return A level of the signal, given in the range of 0 to numLevels-1 (both inclusive).
     * @deprecated Callers should use {@link #calculateSignalLevel(int)} instead to get the
     * signal level using the system default RSSI thresholds, or otherwise compute the RSSI level
     * themselves using their own formula.
     */
    @Deprecated
    public static int calculateSignalLevel(int rssi, int numLevels) {
        if (rssi <= MIN_RSSI) {
            return 0;
        } else if (rssi >= MAX_RSSI) {
            return numLevels - 1;
        } else {
            float inputRange = (MAX_RSSI - MIN_RSSI);
            float outputRange = (numLevels - 1);
            return (int)((float)(rssi - MIN_RSSI) * outputRange / inputRange);
        }
    }

    /**
     * Given a raw RSSI, return the RSSI signal quality rating using the system default RSSI
     * quality rating thresholds.
     * @param rssi a raw RSSI value, in dBm, usually between -55 and -90
     * @return the RSSI signal quality rating, in the range
     * [0, {@link #getMaxSignalLevel()}], where 0 is the lowest (worst signal) RSSI
     * rating and {@link #getMaxSignalLevel()} is the highest (best signal) RSSI rating.
     */
    @IntRange(from = 0)
    public int calculateSignalLevel(int rssi) {
        try {
            return mService.calculateSignalLevel(rssi);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the system default maximum signal level.
     * This is the maximum RSSI level returned by {@link #calculateSignalLevel(int)}.
     */
    @IntRange(from = 0)
    public int getMaxSignalLevel() {
        return calculateSignalLevel(Integer.MAX_VALUE);
    }

    /**
     * Compares two signal strengths.
     *
     * @param rssiA The power of the first signal measured in RSSI.
     * @param rssiB The power of the second signal measured in RSSI.
     * @return Returns <0 if the first signal is weaker than the second signal,
     *         0 if the two signals have the same strength, and >0 if the first
     *         signal is stronger than the second signal.
     */
    public static int compareSignalLevel(int rssiA, int rssiB) {
        return rssiA - rssiB;
    }

    /**
     * Call allowing ConnectivityService to update WifiService with interface mode changes.
     *
     * @param ifaceName String name of the updated interface, or null to represent all interfaces
     * @param mode int representing the new mode, one of:
     *             {@link #IFACE_IP_MODE_TETHERED},
     *             {@link #IFACE_IP_MODE_LOCAL_ONLY},
     *             {@link #IFACE_IP_MODE_CONFIGURATION_ERROR},
     *             {@link #IFACE_IP_MODE_UNSPECIFIED}
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void updateInterfaceIpState(@Nullable String ifaceName, @IfaceIpMode int mode) {
        try {
            mService.updateInterfaceIpState(ifaceName, mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* Wi-Fi/Cellular Coex */

    /**
     * Mandatory coex restriction flag for Wi-Fi Direct.
     *
     * @see #setCoexUnsafeChannels(List, int)
     *
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    public static final int COEX_RESTRICTION_WIFI_DIRECT = 0x1 << 0;

    /**
     * Mandatory coex restriction flag for SoftAP
     *
     * @see #setCoexUnsafeChannels(List, int)
     *
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    public static final int COEX_RESTRICTION_SOFTAP = 0x1 << 1;

    /**
     * Mandatory coex restriction flag for Wi-Fi Aware.
     *
     * @see #setCoexUnsafeChannels(List, int)
     *
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    public static final int COEX_RESTRICTION_WIFI_AWARE = 0x1 << 2;

    /** @hide */
    @RequiresApi(Build.VERSION_CODES.S)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = {"COEX_RESTRICTION_"}, value = {
            COEX_RESTRICTION_WIFI_DIRECT,
            COEX_RESTRICTION_SOFTAP,
            COEX_RESTRICTION_WIFI_AWARE
    })
    public @interface CoexRestriction {}

    /**
     * @return {@code true} if the default coex algorithm is enabled. {@code false} otherwise.
     *
     * @hide
     */
    public boolean isDefaultCoexAlgorithmEnabled() {
        try {
            return mService.isDefaultCoexAlgorithmEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Specify the list of {@link CoexUnsafeChannel} to propagate through the framework for
     * Wi-Fi/Cellular coex channel avoidance if the default algorithm is disabled via overlay
     * (i.e. config_wifiCoexDefaultAlgorithmEnabled = false). Otherwise do nothing.
     *
     * @param unsafeChannels List of {@link CoexUnsafeChannel} to avoid.
     * @param restrictions Bitmap of {@code COEX_RESTRICTION_*} constants specifying the mode
     *                     restrictions on the specified channels. If any restrictions are set,
     *                     then the supplied CoexUnsafeChannels should be completely avoided for
     *                     the specified modes, rather than be avoided with best effort.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_UPDATE_COEX_UNSAFE_CHANNELS)
    @RequiresApi(Build.VERSION_CODES.S)
    public void setCoexUnsafeChannels(
            @NonNull List<CoexUnsafeChannel> unsafeChannels, @CoexRestriction int restrictions) {
        if (unsafeChannels == null) {
            throw new IllegalArgumentException("unsafeChannels must not be null");
        }
        try {
            mService.setCoexUnsafeChannels(unsafeChannels, restrictions);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Registers a CoexCallback to listen on the current CoexUnsafeChannels and restrictions being
     * used for Wi-Fi/cellular coex channel avoidance. The callback method
     * {@link CoexCallback#onCoexUnsafeChannelsChanged(List, int)} will be called immediately after
     * registration to return the current values.
     *
     * @param executor Executor to execute listener callback on
     * @param callback CoexCallback to register
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_ACCESS_COEX_UNSAFE_CHANNELS)
    @RequiresApi(Build.VERSION_CODES.S)
    public void registerCoexCallback(
            @NonNull @CallbackExecutor Executor executor, @NonNull CoexCallback callback) {
        if (executor == null) throw new IllegalArgumentException("executor must not be null");
        if (callback == null) throw new IllegalArgumentException("callback must not be null");
        CoexCallback.CoexCallbackProxy proxy = callback.getProxy();
        proxy.initProxy(executor, callback);
        try {
            mService.registerCoexCallback(proxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Unregisters a CoexCallback from listening on the current CoexUnsafeChannels and restrictions
     * being used for Wi-Fi/cellular coex channel avoidance.
     * @param callback CoexCallback to unregister
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_ACCESS_COEX_UNSAFE_CHANNELS)
    @RequiresApi(Build.VERSION_CODES.S)
    public void unregisterCoexCallback(@NonNull CoexCallback callback) {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");
        CoexCallback.CoexCallbackProxy proxy = callback.getProxy();
        try {
            mService.unregisterCoexCallback(proxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            proxy.cleanUpProxy();
        }
    }

    /**
     * Abstract callback class for applications to receive updates about current CoexUnsafeChannels
     * for Wi-Fi/Cellular coex channel avoidance.
     *
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.S)
    public abstract static class CoexCallback {
        private final CoexCallbackProxy mCoexCallbackProxy;

        public CoexCallback() {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            mCoexCallbackProxy = new CoexCallbackProxy();
        }

        /*package*/ @NonNull
        CoexCallbackProxy getProxy() {
            return mCoexCallbackProxy;
        }

        /**
         * This indicates the current CoexUnsafeChannels and restrictions calculated by the default
         * coex algorithm if config_wifiCoexDefaultAlgorithmEnabled is {@code true}. Otherwise, the
         * values will match the ones supplied to {@link #setCoexUnsafeChannels(List, int)}.
         *
         * @param unsafeChannels List of {@link CoexUnsafeChannel} to avoid.
         * @param restrictions Bitmap of {@code COEX_RESTRICTION_*} constants specifying the mode
         *                     restrictions on the specified channels. If any restrictions are set,
         *                     then the supplied CoexUnsafeChannels should be completely avoided for
         *                     the specified modes, rather than be avoided with best effort.
         */
        public abstract void onCoexUnsafeChannelsChanged(
                @NonNull List<CoexUnsafeChannel> unsafeChannels, @CoexRestriction int restrictions);

        /**
         * Callback proxy for CoexCallback objects.
         */
        private static class CoexCallbackProxy extends ICoexCallback.Stub {
            private final Object mLock = new Object();
            @Nullable @GuardedBy("mLock") private Executor mExecutor;
            @Nullable @GuardedBy("mLock") private CoexCallback mCallback;

            CoexCallbackProxy() {
                mExecutor = null;
                mCallback = null;
            }

            /*package*/ void initProxy(@NonNull Executor executor,
                    @NonNull CoexCallback callback) {
                synchronized (mLock) {
                    mExecutor = executor;
                    mCallback = callback;
                }
            }

            /*package*/ void cleanUpProxy() {
                synchronized (mLock) {
                    mExecutor = null;
                    mCallback = null;
                }
            }

            @Override
            public void onCoexUnsafeChannelsChanged(
                    @NonNull List<CoexUnsafeChannel> unsafeChannels,
                    @CoexRestriction int restrictions) {
                Executor executor;
                CoexCallback callback;
                synchronized (mLock) {
                    executor = mExecutor;
                    callback = mCallback;
                }
                if (executor == null || callback == null) {
                    return;
                }
                Binder.clearCallingIdentity();
                executor.execute(() ->
                        callback.onCoexUnsafeChannelsChanged(unsafeChannels, restrictions));
            }
        }
    }

    /**
     * Start Soft AP (hotspot) mode for tethering purposes with the specified configuration.
     * Note that starting Soft AP mode may disable station mode operation if the device does not
     * support concurrency.
     * @param wifiConfig SSID, security and channel details as part of WifiConfiguration, or null to
     *                   use the persisted Soft AP configuration that was previously set using
     *                   {@link #setWifiApConfiguration(WifiConfiguration)}.
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     *
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public boolean startSoftAp(@Nullable WifiConfiguration wifiConfig) {
        try {
            return mService.startSoftAp(wifiConfig, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Start Soft AP (hotspot) mode for tethering purposes with the specified configuration.
     * Note that starting Soft AP mode may disable station mode operation if the device does not
     * support concurrency.
     *
     * Note: Call {@link WifiManager#validateSoftApConfiguration(SoftApConfiguration)} to avoid
     * unexpected error due to invalid configuration.
     *
     * @param softApConfig A valid SoftApConfiguration specifying the configuration of the SAP, or
     *                     null to use the persisted Soft AP configuration that was previously set
     *                     using {@link WifiManager#setSoftApConfiguration(SoftApConfiguration)}.
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public boolean startTetheredHotspot(@Nullable SoftApConfiguration softApConfig) {
        try {
            return mService.startTetheredHotspot(softApConfig, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }


    /**
     * Stop SoftAp mode.
     * Note that stopping softap mode will restore the previous wifi mode.
     * @return {@code true} if the operation succeeds, {@code false} otherwise
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public boolean stopSoftAp() {
        try {
            return mService.stopSoftAp();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if input configuration is valid.
     *
     * @param config a configuration would like to be checked.
     * @return true if config is valid, otherwise false.
     */
    public boolean validateSoftApConfiguration(@NonNull SoftApConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException(TAG + ": config can not be null");
        }
        try {
            return mService.validateSoftApConfiguration(config);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Request a local only hotspot that an application can use to communicate between co-located
     * devices connected to the created WiFi hotspot.  The network created by this method will not
     * have Internet access.  Each application can make a single request for the hotspot, but
     * multiple applications could be requesting the hotspot at the same time.  When multiple
     * applications have successfully registered concurrently, they will be sharing the underlying
     * hotspot. {@link LocalOnlyHotspotCallback#onStarted(LocalOnlyHotspotReservation)} is called
     * when the hotspot is ready for use by the application.
     * <p>
     * Each application can make a single active call to this method. The {@link
     * LocalOnlyHotspotCallback#onStarted(LocalOnlyHotspotReservation)} callback supplies the
     * requestor with a {@link LocalOnlyHotspotReservation} that contains a
     * {@link SoftApConfiguration} with the SSID, security type and credentials needed to connect
     * to the hotspot.  Communicating this information is up to the application.
     * <p>
     * If the LocalOnlyHotspot cannot be created, the {@link LocalOnlyHotspotCallback#onFailed(int)}
     * method will be called. Example failures include errors bringing up the network or if
     * there is an incompatible operating mode.  For example, if the user is currently using Wifi
     * Tethering to provide an upstream to another device, LocalOnlyHotspot may not start due to
     * an incompatible mode. The possible error codes include:
     * {@link LocalOnlyHotspotCallback#ERROR_NO_CHANNEL},
     * {@link LocalOnlyHotspotCallback#ERROR_GENERIC},
     * {@link LocalOnlyHotspotCallback#ERROR_INCOMPATIBLE_MODE} and
     * {@link LocalOnlyHotspotCallback#ERROR_TETHERING_DISALLOWED}.
     * <p>
     * Internally, requests will be tracked to prevent the hotspot from being torn down while apps
     * are still using it.  The {@link LocalOnlyHotspotReservation} object passed in the  {@link
     * LocalOnlyHotspotCallback#onStarted(LocalOnlyHotspotReservation)} call should be closed when
     * the LocalOnlyHotspot is no longer needed using {@link LocalOnlyHotspotReservation#close()}.
     * Since the hotspot may be shared among multiple applications, removing the final registered
     * application request will trigger the hotspot teardown.  This means that applications should
     * not listen to broadcasts containing wifi state to determine if the hotspot was stopped after
     * they are done using it. Additionally, once {@link LocalOnlyHotspotReservation#close()} is
     * called, applications will not receive callbacks of any kind.
     * <p>
     * Applications should be aware that the user may also stop the LocalOnlyHotspot through the
     * Settings UI; it is not guaranteed to stay up as long as there is a requesting application.
     * The requestors will be notified of this case via
     * {@link LocalOnlyHotspotCallback#onStopped()}.  Other cases may arise where the hotspot is
     * torn down (Emergency mode, etc).  Application developers should be aware that it can stop
     * unexpectedly, but they will receive a notification if they have properly registered.
     * <p>
     * Applications should also be aware that this network will be shared with other applications.
     * Applications are responsible for protecting their data on this network (e.g. TLS).
     * <p>
     * Applications targeting {@link android.os.Build.VERSION_CODES#TIRAMISU} or later need to have
     * the following permissions: {@link android.Manifest.permission#CHANGE_WIFI_STATE} and
     * {@link android.Manifest.permission#NEARBY_WIFI_DEVICES}.
     * Applications targeting {@link Build.VERSION_CODES#S} or prior SDK levels need to have the
     * following permissions: {@link android.Manifest.permission#CHANGE_WIFI_STATE} and
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     * Callers without
     * the permissions will trigger a {@link java.lang.SecurityException}.
     * <p>
     * @param callback LocalOnlyHotspotCallback for the application to receive updates about
     * operating status.
     * @param handler Handler to be used for callbacks.  If the caller passes a null Handler, the
     * main thread will be used.
     */
    @RequiresPermission(allOf = {CHANGE_WIFI_STATE, ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES},
            conditional = true)
    public void startLocalOnlyHotspot(LocalOnlyHotspotCallback callback,
            @Nullable Handler handler) {
        Executor executor = handler == null ? null : new HandlerExecutor(handler);
        startLocalOnlyHotspotInternal(null, executor, callback);
    }

    /**
     * Starts a local-only hotspot with a specific configuration applied. See
     * {@link #startLocalOnlyHotspot(LocalOnlyHotspotCallback, Handler)}.
     *
     * Applications need either {@link android.Manifest.permission#NETWORK_SETUP_WIZARD},
     * {@link android.Manifest.permission#NETWORK_SETTINGS} or
     * {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} to call this method.
     *
     * Since custom configuration settings may be incompatible with each other, the hotspot started
     * through this method cannot coexist with another hotspot created through
     * startLocalOnlyHotspot. If this is attempted, the first hotspot request wins and others
     * receive {@link LocalOnlyHotspotCallback#ERROR_GENERIC} through
     * {@link LocalOnlyHotspotCallback#onFailed}.
     *
     * @param config Custom configuration for the hotspot. See {@link SoftApConfiguration}.
     * @param executor Executor to run callback methods on, or null to use the main thread.
     * @param callback Callback object for updates about hotspot status, or null for no updates.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            NEARBY_WIFI_DEVICES})
    public void startLocalOnlyHotspot(@NonNull SoftApConfiguration config,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable LocalOnlyHotspotCallback callback) {
        Objects.requireNonNull(config);
        startLocalOnlyHotspotInternal(config, executor, callback);
    }

    /**
     * Common implementation of both configurable and non-configurable LOHS.
     *
     * @param config App-specified configuration, or null. When present, additional privileges are
     *               required, and the hotspot cannot be shared with other clients.
     * @param executor Executor to run callback methods on, or null to use the main thread.
     * @param callback Callback object for updates about hotspot status, or null for no updates.
     */
    private void startLocalOnlyHotspotInternal(
            @Nullable SoftApConfiguration config,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable LocalOnlyHotspotCallback callback) {
        if (executor == null) {
            executor = mContext.getMainExecutor();
        }
        synchronized (mLock) {
            LocalOnlyHotspotCallbackProxy proxy =
                    new LocalOnlyHotspotCallbackProxy(this, executor, callback);
            try {
                String packageName = mContext.getOpPackageName();
                String featureId = mContext.getAttributionTag();
                Bundle extras = new Bundle();
                if (SdkLevel.isAtLeastS()) {
                    extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                            mContext.getAttributionSource());
                }
                int returnCode = mService.startLocalOnlyHotspot(proxy, packageName, featureId,
                        config, extras);
                if (returnCode != LocalOnlyHotspotCallback.REQUEST_REGISTERED) {
                    // Send message to the proxy to make sure we call back on the correct thread
                    proxy.onHotspotFailed(returnCode);
                    return;
                }
                mLOHSCallbackProxy = proxy;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Cancels a pending local only hotspot request.  This can be used by the calling application to
     * cancel the existing request if the provided callback has not been triggered.  Calling this
     * method will be equivalent to closing the returned LocalOnlyHotspotReservation, but it is not
     * explicitly required.
     * <p>
     * When cancelling this request, application developers should be aware that there may still be
     * outstanding local only hotspot requests and the hotspot may still start, or continue running.
     * Additionally, if a callback was registered, it will no longer be triggered after calling
     * cancel.
     *
     * @hide
     */
    @UnsupportedAppUsage
    public void cancelLocalOnlyHotspotRequest() {
        synchronized (mLock) {
            stopLocalOnlyHotspot();
        }
    }

    /**
     *  Method used to inform WifiService that the LocalOnlyHotspot is no longer needed.  This
     *  method is used by WifiManager to release LocalOnlyHotspotReservations held by calling
     *  applications and removes the internal tracking for the hotspot request.  When all requesting
     *  applications are finished using the hotspot, it will be stopped and WiFi will return to the
     *  previous operational mode.
     *
     *  This method should not be called by applications.  Instead, they should call the close()
     *  method on their LocalOnlyHotspotReservation.
     */
    private void stopLocalOnlyHotspot() {
        synchronized (mLock) {
            if (mLOHSCallbackProxy == null) {
                // nothing to do, the callback was already cleaned up.
                return;
            }
            mLOHSCallbackProxy = null;
            try {
                mService.stopLocalOnlyHotspot();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Registers a callback for local only hotspot. See {@link SoftApCallback}. Caller will receive
     * the following callbacks on registration:
     * <ul>
     * <li> {@link SoftApCallback#onStateChanged(int, int)}</li>
     * <li> {@link SoftApCallback#onConnectedClientsChanged(List<WifiClient>)}</li>
     * <li> {@link SoftApCallback#onInfoChanged(List<SoftApInfo>)}</li>
     * <li> {@link SoftApCallback#onCapabilityChanged(SoftApCapability)}</li>
     * </ul>
     *
     * Use {@link SoftApCallback#onConnectedClientsChanged(SoftApInfo, List<WifiClient>)} to know
     * if there are any clients connected to a specific bridged instance of this AP
     * (if bridged AP is enabled).
     *
     * Note: Caller will receive the callback
     * {@link SoftApCallback#onConnectedClientsChanged(SoftApInfo, List<WifiClient>)}
     * on registration when there are clients connected to AP.
     *
     * These will be dispatched on registration to provide the caller with the current state
     * (and are not an indication of any current change). Note that receiving an immediate
     * WIFI_AP_STATE_FAILED value for soft AP state indicates that the latest attempt to start
     * soft AP has failed. Caller can unregister a previously registered callback using
     * {@link #unregisterLocalOnlyHotspotSoftApCallback}
     * <p>
     *
     * @param executor The Executor on whose thread to execute the callbacks of the {@code callback}
     *                 object.
     * @param callback Callback for local only hotspot events
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(NEARBY_WIFI_DEVICES)
    public void registerLocalOnlyHotspotSoftApCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull SoftApCallback callback) {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "registerLocalOnlyHotspotSoftApCallback: callback=" + callback + ", executor="
                + executor);
        try {
            synchronized (sLocalOnlyHotspotSoftApCallbackMap) {
                ISoftApCallback.Stub binderCallback = new SoftApCallbackProxy(executor, callback,
                        IFACE_IP_MODE_LOCAL_ONLY);
                sLocalOnlyHotspotSoftApCallbackMap.put(System.identityHashCode(callback),
                        binderCallback);
                Bundle extras = new Bundle();
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
                mService.registerLocalOnlyHotspotSoftApCallback(binderCallback, extras);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow callers to unregister a previously registered callback. After calling this method,
     * applications will no longer receive local only hotspot events.
     *
     * <p>
     *
     * @param callback Callback to unregister for soft AP events
     *
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(NEARBY_WIFI_DEVICES)
    public void unregisterLocalOnlyHotspotSoftApCallback(@NonNull SoftApCallback callback) {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException();
        }
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "unregisterLocalOnlyHotspotSoftApCallback: callback=" + callback);

        try {
            synchronized (sLocalOnlyHotspotSoftApCallbackMap) {
                int callbackIdentifier = System.identityHashCode(callback);
                if (!sLocalOnlyHotspotSoftApCallbackMap.contains(callbackIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + callbackIdentifier);
                    return;
                }
                Bundle extras = new Bundle();
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
                mService.unregisterLocalOnlyHotspotSoftApCallback(
                        sLocalOnlyHotspotSoftApCallbackMap.get(callbackIdentifier), extras);
                sLocalOnlyHotspotSoftApCallbackMap.remove(callbackIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow callers (Settings UI) to watch LocalOnlyHotspot state changes.  Callers will
     * receive a {@link LocalOnlyHotspotSubscription} object as a parameter of the
     * {@link LocalOnlyHotspotObserver#onRegistered(LocalOnlyHotspotSubscription)}. The registered
     * callers will receive the {@link LocalOnlyHotspotObserver#onStarted(SoftApConfiguration)} and
     * {@link LocalOnlyHotspotObserver#onStopped()} callbacks.
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION}
     * permission.  Callers without the permission will trigger a
     * {@link java.lang.SecurityException}.
     * <p>
     * @param observer LocalOnlyHotspotObserver callback.
     * @param handler Handler to use for callbacks
     *
     * @hide
     */
    public void watchLocalOnlyHotspot(LocalOnlyHotspotObserver observer,
            @Nullable Handler handler) {
        Executor executor = handler == null ? mContext.getMainExecutor()
                : new HandlerExecutor(handler);
        synchronized (mLock) {
            mLOHSObserverProxy =
                    new LocalOnlyHotspotObserverProxy(this, executor, observer);
            try {
                mService.startWatchLocalOnlyHotspot(mLOHSObserverProxy);
                mLOHSObserverProxy.registered();
            } catch (RemoteException e) {
                mLOHSObserverProxy = null;
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Allow callers to stop watching LocalOnlyHotspot state changes.  After calling this method,
     * applications will no longer receive callbacks.
     *
     * @hide
     */
    public void unregisterLocalOnlyHotspotObserver() {
        synchronized (mLock) {
            if (mLOHSObserverProxy == null) {
                // nothing to do, the callback was already cleaned up
                return;
            }
            mLOHSObserverProxy = null;
            try {
                mService.stopWatchLocalOnlyHotspot();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Gets the tethered Wi-Fi hotspot enabled state.
     * @return One of {@link #WIFI_AP_STATE_DISABLED},
     *         {@link #WIFI_AP_STATE_DISABLING}, {@link #WIFI_AP_STATE_ENABLED},
     *         {@link #WIFI_AP_STATE_ENABLING}, {@link #WIFI_AP_STATE_FAILED}
     * @see #isWifiApEnabled()
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public int getWifiApState() {
        try {
            return mService.getWifiApEnabledState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return whether tethered Wi-Fi AP is enabled or disabled.
     * @return {@code true} if tethered  Wi-Fi AP is enabled
     * @see #getWifiApState()
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public boolean isWifiApEnabled() {
        return getWifiApState() == WIFI_AP_STATE_ENABLED;
    }

    /**
     * Gets the tethered Wi-Fi AP Configuration.
     * @return AP details in WifiConfiguration
     *
     * Note that AP detail may contain configuration which is cannot be represented
     * by the legacy WifiConfiguration, in such cases a null will be returned.
     *
     * @deprecated This API is deprecated. Use {@link #getSoftApConfiguration()} instead.
     * @hide
     */
    @Nullable
    @SystemApi
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    @Deprecated
    public WifiConfiguration getWifiApConfiguration() {
        try {
            return mService.getWifiApConfiguration();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets the Wi-Fi tethered AP Configuration.
     * @return AP details in {@link SoftApConfiguration}
     *
     * @hide
     */
    @NonNull
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public SoftApConfiguration getSoftApConfiguration() {
        try {
            return mService.getSoftApConfiguration();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets the last configured Wi-Fi tethered AP passphrase.
     *
     * Note: It may be null when there is no passphrase changed since
     * device boot.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultCallback An asynchronous callback that will return the last configured
     *                       Wi-Fi tethered AP passphrase.
     *
     * @throws SecurityException if the caller does not have permission.
     * @throws NullPointerException if the caller provided invalid inputs.
     *
     * @hide
     */
    @Nullable
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void queryLastConfiguredTetheredApPassphraseSinceBoot(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<String> resultCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultCallback, "resultsCallback cannot be null");
        try {
            mService.queryLastConfiguredTetheredApPassphraseSinceBoot(
                    new IStringListener.Stub() {
                        @Override
                        public void onResult(String value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the tethered Wi-Fi AP Configuration.
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     *
     * @deprecated This API is deprecated. Use {@link #setSoftApConfiguration(SoftApConfiguration)}
     * instead.
     * @hide
     */
    @SystemApi
    @RequiresPermission(CHANGE_WIFI_STATE)
    @Deprecated
    public boolean setWifiApConfiguration(WifiConfiguration wifiConfig) {
        try {
            return mService.setWifiApConfiguration(wifiConfig, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the tethered Wi-Fi AP Configuration.
     *
     * If the API is called while the tethered soft AP is enabled, the configuration will apply to
     * the current soft AP if the new configuration only includes
     * {@link SoftApConfiguration.Builder#setMaxNumberOfClients(int)}
     * or {@link SoftApConfiguration.Builder#setShutdownTimeoutMillis(long)}
     * or {@link SoftApConfiguration.Builder#setClientControlByUserEnabled(boolean)}
     * or {@link SoftApConfiguration.Builder#setBlockedClientList(List)}
     * or {@link SoftApConfiguration.Builder#setAllowedClientList(List)}
     * or {@link SoftApConfiguration.Builder#setAutoShutdownEnabled(boolean)}
     * or {@link SoftApConfiguration.Builder#setBridgedModeOpportunisticShutdownEnabled(boolean)}
     *
     * Otherwise, the configuration changes will be applied when the Soft AP is next started
     * (the framework will not stop/start the AP).
     *
     * Note: Call {@link WifiManager#validateSoftApConfiguration(SoftApConfiguration)} to avoid
     * unexpected error due to invalid configuration.
     *
     * @param softApConfig  A valid SoftApConfiguration specifying the configuration of the SAP.
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public boolean setSoftApConfiguration(@NonNull SoftApConfiguration softApConfig) {
        try {
            return mService.setSoftApConfiguration(
                    softApConfig, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable/Disable TDLS on a specific local route.
     *
     * <p>
     * TDLS enables two wireless endpoints to talk to each other directly
     * without going through the access point that is managing the local
     * network. It saves bandwidth and improves quality of the link.
     * </p>
     * <p>
     * This API enables/disables the option of using TDLS. If enabled, the
     * underlying hardware is free to use TDLS or a hop through the access
     * point. If disabled, existing TDLS session is torn down and
     * hardware is restricted to use access point for transferring wireless
     * packets. Default value for all routes is 'disabled', meaning restricted
     * to use access point for transferring packets.
     * </p>
     *
     * @param remoteIPAddress IP address of the endpoint to setup TDLS with
     * @param enable true = setup and false = tear down TDLS
     */
    public void setTdlsEnabled(InetAddress remoteIPAddress, boolean enable) {
        try {
            mService.enableTdls(remoteIPAddress.getHostAddress(), enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable/Disable TDLS on a specific local route.
     *
     * Similar to {@link #setTdlsEnabled(InetAddress, boolean)}, except
     * this version sends the result of the Enable/Disable request.
     *
     * @param remoteIPAddress IP address of the endpoint to setup TDLS with
     * @param enable true = setup and false = tear down TDLS
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return {@code Boolean} indicating
     *                        whether TDLS was successfully enabled or disabled.
     *                        {@code true} for success, {@code false} for failure.
     *
     * @throws NullPointerException if the caller provided invalid inputs.
     */
    public void setTdlsEnabled(@NonNull InetAddress remoteIPAddress, boolean enable,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        Objects.requireNonNull(remoteIPAddress, "remote IP address cannot be null");
        try {
            mService.enableTdlsWithRemoteIpAddress(remoteIPAddress.getHostAddress(), enable,
                    new IBooleanListener.Stub() {
                        @Override
                        public void onResult(boolean value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Similar to {@link #setTdlsEnabled(InetAddress, boolean) }, except
     * this version allows you to specify remote endpoint with a MAC address.
     * @param remoteMacAddress MAC address of the remote endpoint such as 00:00:0c:9f:f2:ab
     * @param enable true = setup and false = tear down TDLS
     */
    public void setTdlsEnabledWithMacAddress(String remoteMacAddress, boolean enable) {
        try {
            mService.enableTdlsWithMacAddress(remoteMacAddress, enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable/Disable TDLS with a specific peer Mac Address.
     *
     * Similar to {@link #setTdlsEnabledWithMacAddress(String, boolean)}, except
     * this version sends the result of the Enable/Disable request.
     *
     * @param remoteMacAddress Mac address of the endpoint to setup TDLS with
     * @param enable true = setup and false = tear down TDLS
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return {@code Boolean} indicating
     *                        whether TDLS was successfully enabled or disabled.
     *                        {@code true} for success, {@code false} for failure.
     *
     * @throws NullPointerException if the caller provided invalid inputs.
     */
    public void setTdlsEnabledWithMacAddress(@NonNull String remoteMacAddress, boolean enable,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        Objects.requireNonNull(remoteMacAddress, "remote Mac address cannot be null");
        try {
            mService.enableTdlsWithRemoteMacAddress(remoteMacAddress, enable,
                    new IBooleanListener.Stub() {
                        @Override
                        public void onResult(boolean value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if a TDLS session can be established at this time via
     * {@link #setTdlsEnabled(InetAddress, boolean)} or
     * {@link #setTdlsEnabledWithMacAddress(String, boolean)} or
     * {@link #setTdlsEnabled(InetAddress, boolean, Executor, Consumer)} or
     * {@link #setTdlsEnabledWithMacAddress(String, boolean, Executor, Consumer)}
     *
     * Internally framework checks the STA connected state, device support for TDLS and
     * the number of TDLS sessions available in driver/firmware.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return {@code Boolean} indicating
     *                        whether a TDLS session can be established at this time.
     *                        {@code true} for available, {@code false} for not available.
     *
     * @throws NullPointerException if the caller provided invalid inputs.
     */
    public void isTdlsOperationCurrentlyAvailable(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.isTdlsOperationCurrentlyAvailable(
                    new IBooleanListener.Stub() {
                        @Override
                        public void onResult(boolean value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the maximum number of concurrent TDLS sessions supported by the device.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return the maximum number of
     *                        concurrent TDLS sessions supported by the device. Returns
     *                        {@code -1} if information is not available,
     *                        e.g. if the driver/firmware doesn't provide this information.
     *
     * @throws NullPointerException if the caller provided invalid inputs.
     * @throws UnsupportedOperationException if the feature is not available.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void getMaxSupportedConcurrentTdlsSessions(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Integer> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.getMaxSupportedConcurrentTdlsSessions(
                    new IIntegerListener.Stub() {
                        @Override
                        public void onResult(int value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the number of currently enabled TDLS sessions.
     *
     * Tracks the number of peers enabled for TDLS session via
     * {@link #setTdlsEnabled(InetAddress, boolean) },
     * {@link #setTdlsEnabledWithMacAddress(String, boolean) },
     * {@link #setTdlsEnabled(InetAddress, boolean, Executor, Consumer) } and
     * {@link #setTdlsEnabledWithMacAddress(String, boolean, Executor, Consumer) }
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return the number of Peer
     *                        Mac addresses configured in the driver for TDLS session.
     *
     * @throws NullPointerException if the caller provided invalid inputs.
     */
    public void getNumberOfEnabledTdlsSessions(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Integer> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.getNumberOfEnabledTdlsSessions(
                    new IIntegerListener.Stub() {
                        @Override
                        public void onResult(int value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ActionListener.FAILURE_INTERNAL_ERROR,
            ActionListener.FAILURE_IN_PROGRESS,
            ActionListener.FAILURE_BUSY,
            ActionListener.FAILURE_INVALID_ARGS,
            ActionListener.FAILURE_NOT_AUTHORIZED})
    public @interface ActionListenerFailureReason {}

    /* WPS specific errors */
    /** WPS overlap detected
     * @deprecated This is deprecated
     */
    public static final int WPS_OVERLAP_ERROR           = 3;
    /** WEP on WPS is prohibited
     * @deprecated This is deprecated
     */
    public static final int WPS_WEP_PROHIBITED          = 4;
    /** TKIP only prohibited
     * @deprecated This is deprecated
     */
    public static final int WPS_TKIP_ONLY_PROHIBITED    = 5;
    /** Authentication failure on WPS
     * @deprecated This is deprecated
     */
    public static final int WPS_AUTH_FAILURE            = 6;
    /** WPS timed out
     * @deprecated This is deprecated
     */
    public static final int WPS_TIMED_OUT               = 7;

    /**
     * Interface for callback invocation on an application action.
     * @hide
     */
    @SystemApi
    public interface ActionListener {
        /**
         * Passed with {@link #onFailure}.
         * Indicates that the operation failed due to an internal error.
         */
        int FAILURE_INTERNAL_ERROR = 0;

        /**
         * Passed with {@link #onFailure}.
         * Indicates that the operation is already in progress.
         */
        int FAILURE_IN_PROGRESS = 1;

        /**
         * Passed with {@link #onFailure}.
         * Indicates that the operation failed because the framework is busy and is unable to
         * service the request.
         */
        int FAILURE_BUSY = 2;

        /**
         * Passed with {@link #onFailure}.
         * Indicates that the operation failed due to invalid inputs.
         */
        int FAILURE_INVALID_ARGS = 3;

        /**
         * Passed with {@link #onFailure}.
         * Indicates that the operation failed due to insufficient user permissions.
         */
        int FAILURE_NOT_AUTHORIZED = 4;

        /**
         * The operation succeeded.
         */
        void onSuccess();
        /**
         * The operation failed.
         * @param reason The reason for failure depends on the operation.
         */
        void onFailure(@ActionListenerFailureReason int reason);
    }

    /** Interface for callback invocation on a start WPS action
     * @deprecated This is deprecated
     */
    public static abstract class WpsCallback {

        /** WPS start succeeded
         * @deprecated This API is deprecated
         */
        public abstract void onStarted(String pin);

        /** WPS operation completed successfully
         * @deprecated This API is deprecated
         */
        public abstract void onSucceeded();

        /**
         * WPS operation failed
         * @param reason The reason for failure could be one of
         * {@link #WPS_TKIP_ONLY_PROHIBITED}, {@link #WPS_OVERLAP_ERROR},
         * {@link #WPS_WEP_PROHIBITED}, {@link #WPS_TIMED_OUT} or {@link #WPS_AUTH_FAILURE}
         * and some generic errors.
         * @deprecated This API is deprecated
         */
        public abstract void onFailed(int reason);
    }

    /**
     * Base class for soft AP callback. Should be extended by applications and set when calling
     * {@link WifiManager#registerSoftApCallback(Executor, SoftApCallback)}.
     *
     * @hide
     */
    @SystemApi
    public interface SoftApCallback {
        /**
         * Called when soft AP state changes.
         *
         * @param state         the new AP state. One of {@link #WIFI_AP_STATE_DISABLED},
         *                      {@link #WIFI_AP_STATE_DISABLING}, {@link #WIFI_AP_STATE_ENABLED},
         *                      {@link #WIFI_AP_STATE_ENABLING}, {@link #WIFI_AP_STATE_FAILED}
         * @param failureReason reason when in failed state. One of
         *                      {@link #SAP_START_FAILURE_GENERAL},
         *                      {@link #SAP_START_FAILURE_NO_CHANNEL},
         *                      {@link #SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION},
         *                      {@link #SAP_START_FAILURE_USER_REJECTED}
         */
        default void onStateChanged(@WifiApState int state, @SapStartFailure int failureReason) {}

        /**
         * Called when the connected clients to soft AP changes.
         *
         * @param clients the currently connected clients
         *
         * @deprecated This API is deprecated.
         * Use {@link #onConnectedClientsChanged(SoftApInfo, List<WifiClient>)} instead.
         */
        @Deprecated
        default void onConnectedClientsChanged(@NonNull List<WifiClient> clients) {}


        /**
         * Called when the connected clients for a soft AP instance change.
         *
         * When the Soft AP is configured in single AP mode, this callback is invoked
         * with the same {@link SoftApInfo} for all connected clients changes.
         * When the Soft AP is configured as multiple Soft AP instances (using
         * {@link SoftApConfiguration.Builder#setBands(int[])} or
         * {@link SoftApConfiguration.Builder#setChannels(android.util.SparseIntArray)}), this
         * callback is invoked with the corresponding {@link SoftApInfo} for the instance in which
         * the connected clients changed.
         *
         * @param info The {@link SoftApInfo} of the AP.
         * @param clients The currently connected clients on the AP instance specified by
         *                {@code info}.
         */
        default void onConnectedClientsChanged(@NonNull SoftApInfo info,
                @NonNull List<WifiClient> clients) {}

        /**
         * Called when the Soft AP information changes.
         *
         * Note: this API remains valid only when the Soft AP is configured as a single AP -
         * not as multiple Soft APs (which are bridged to each other). When multiple Soft APs are
         * configured (using {@link SoftApConfiguration.Builder#setBands(int[])} or
         * {@link SoftApConfiguration.Builder#setChannels(android.util.SparseIntArray)})
         * this callback will not be triggered -  use the
         * {@link #onInfoChanged(List<SoftApInfo>)} callback in that case.
         *
         * @param softApInfo is the Soft AP information. {@link SoftApInfo}
         *
         * @deprecated This API is deprecated. Use {@link #onInfoChanged(List<SoftApInfo>)}
         * instead.
         */
        @Deprecated
        default void onInfoChanged(@NonNull SoftApInfo softApInfo) {
            // Do nothing: can be updated to add SoftApInfo details (e.g. channel) to the UI.
        }

        /**
         * Called when the Soft AP information changes.
         *
         * Returns information on all configured Soft AP instances. The number of the elements in
         * the list depends on Soft AP configuration and state:
         * <ul>
         * <li>An empty list will be returned when the Soft AP is disabled.
         * <li>One information element will be returned in the list when the Soft AP is configured
         *     as a single AP or when a single Soft AP remains active.
         * <li>Two information elements will be returned in the list when the multiple Soft APs are
         *     configured and are active.
         *     (configured using {@link SoftApConfiguration.Builder#setBands(int[])} or
         *     {@link SoftApConfiguration.Builder#setChannels(android.util.SparseIntArray)}).
         * </ul>
         *
         * Note: When multiple Soft AP instances are configured, one of the Soft APs may
         * be shut down independently of the other by the framework. This can happen if no devices
         * are connected to it for some duration. In that case, one information element will be
         * returned.
         *
         * See {@link #isBridgedApConcurrencySupported()} for support info of multiple (bridged) AP.
         *
         * @param softApInfoList is the list of the Soft AP information elements -
         *        {@link SoftApInfo}.
         */
        default void onInfoChanged(@NonNull List<SoftApInfo> softApInfoList) {
            // Do nothing: can be updated to add SoftApInfo details (e.g. channel) to the UI.
        }

        /**
         * Called when capability of Soft AP changes.
         *
         * @param softApCapability is the Soft AP capability. {@link SoftApCapability}
         */
        default void onCapabilityChanged(@NonNull SoftApCapability softApCapability) {
            // Do nothing: can be updated to add SoftApCapability details (e.g. meximum supported
            // client number) to the UI.
        }

        /**
         * Called when client trying to connect but device blocked the client with specific reason.
         *
         * Can be used to ask user to update client to allowed list or blocked list
         * when reason is {@link SAP_CLIENT_BLOCK_REASON_CODE_BLOCKED_BY_USER}, or
         * indicate the block due to maximum supported client number limitation when reason is
         * {@link SAP_CLIENT_BLOCK_REASON_CODE_NO_MORE_STAS}.
         *
         * @param client the currently blocked client.
         * @param blockedReason one of blocked reason from {@link SapClientBlockedReason}
         */
        default void onBlockedClientConnecting(@NonNull WifiClient client,
                @SapClientBlockedReason int blockedReason) {
            // Do nothing: can be used to ask user to update client to allowed list or blocked list.
        }
    }

    /**
     * Callback proxy for SoftApCallback objects.
     *
     * @hide
     */
    private class SoftApCallbackProxy extends ISoftApCallback.Stub {
        private final Executor mExecutor;
        private final SoftApCallback mCallback;
        // Either {@link #IFACE_IP_MODE_TETHERED} or {@link #IFACE_IP_MODE_LOCAL_ONLY}.
        private final int mIpMode;
        private Map<String, List<WifiClient>> mCurrentClients = new HashMap<>();
        private Map<String, SoftApInfo> mCurrentInfos = new HashMap<>();

        private List<WifiClient> getConnectedClientList(Map<String, List<WifiClient>> clientsMap) {
            List<WifiClient> connectedClientList = new ArrayList<>();
            for (List<WifiClient> it : clientsMap.values()) {
                connectedClientList.addAll(it);
            }
            return connectedClientList;
        }

        SoftApCallbackProxy(Executor executor, SoftApCallback callback, int mode) {
            mExecutor = executor;
            mCallback = callback;
            mIpMode = mode;
        }

        @Override
        public void onStateChanged(int state, int failureReason) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode + ", onStateChanged: state="
                        + state + ", failureReason=" + failureReason);
            }

            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onStateChanged(state, failureReason);
            });
        }

        @Override
        public void onConnectedClientsOrInfoChanged(Map<String, SoftApInfo> infos,
                Map<String, List<WifiClient>> clients, boolean isBridged, boolean isRegistration) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode
                        + ", onConnectedClientsOrInfoChanged: clients: "
                        + clients + ", infos: " + infos + ", isBridged is " + isBridged
                        + ", isRegistration is " + isRegistration);
            }

            List<SoftApInfo> changedInfoList = new ArrayList<>(infos.values());
            Map<SoftApInfo, List<WifiClient>> changedInfoClients = new HashMap<>();
            // Some devices may not support infos callback, allow them to support client
            // connection changed callback.
            boolean areClientsChangedWithoutInfosChanged =
                    infos.size() == 0 && getConnectedClientList(clients).size()
                    != getConnectedClientList(mCurrentClients).size();
            boolean isInfoChanged = infos.size() != mCurrentInfos.size();

            if (isRegistration) {
                // Check if there are clients connected, put it to changedInfoClients
                for (SoftApInfo currentInfo : infos.values()) {
                    String instance = currentInfo.getApInstanceIdentifier();
                    if (clients.getOrDefault(instance, Collections.emptyList()).size() > 0) {
                        changedInfoClients.put(currentInfo, clients.get(instance));
                    }
                }
            }

            // Check if old info removed or not (client changed case)
            for (SoftApInfo info : mCurrentInfos.values()) {
                String changedInstance = info.getApInstanceIdentifier();
                List<WifiClient> changedClientList = clients.getOrDefault(
                        changedInstance, Collections.emptyList());
                if (!changedInfoList.contains(info)) {
                    isInfoChanged = true;
                    if (mCurrentClients.getOrDefault(changedInstance,
                              Collections.emptyList()).size() > 0) {
                        SoftApInfo changedInfo = infos.get(changedInstance);
                        if (changedInfo == null || changedInfo.getFrequency() == 0) {
                            Log.d(TAG, "SoftApCallbackProxy on mode " + mIpMode
                                    + ", info changed on client connected instance(AP disabled)");
                            // Send old info with empty client list for shutdown case
                            changedInfoClients.put(info, Collections.emptyList());
                        } else {
                            Log.d(TAG, "SoftApCallbackProxy on mode " + mIpMode
                                    + ", info changed on client connected instance");
                            changedInfoClients.put(changedInfo, changedClientList);
                        }
                    }
                } else {
                    // info doesn't change, check client list
                    if (changedClientList.size()
                            != mCurrentClients
                            .getOrDefault(changedInstance, Collections.emptyList()).size()) {
                        // Here should notify client changed on new info(same as old info)
                        changedInfoClients.put(info, changedClientList);
                    }
                }
            }

            mCurrentClients = clients;
            mCurrentInfos = infos;
            if (!isInfoChanged && changedInfoClients.isEmpty()
                    && !isRegistration && !areClientsChangedWithoutInfosChanged) {
                Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode
                        + ", No changed & Not Registration don't need to notify the client");
                return;
            }
            Binder.clearCallingIdentity();
            // Notify the clients changed first for old info shutdown case
            for (SoftApInfo changedInfo : changedInfoClients.keySet()) {
                Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode
                        + ", send onConnectedClientsChanged, changedInfo is "
                        + changedInfo + " and clients are " + changedInfoClients.get(changedInfo));
                mExecutor.execute(() -> {
                    mCallback.onConnectedClientsChanged(
                            changedInfo, changedInfoClients.get(changedInfo));
                });
            }

            if (isInfoChanged || isRegistration) {
                if (!isBridged) {
                    SoftApInfo newInfo = changedInfoList.isEmpty()
                            ? new SoftApInfo() : changedInfoList.get(0);
                    Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode
                            + ", send InfoChanged, newInfo: " + newInfo);
                    mExecutor.execute(() -> {
                        mCallback.onInfoChanged(newInfo);
                    });
                }
                Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode
                        + ", send InfoChanged, changedInfoList: " + changedInfoList);
                mExecutor.execute(() -> {
                    mCallback.onInfoChanged(changedInfoList);
                });
            }

            if (isRegistration || !changedInfoClients.isEmpty()
                    || areClientsChangedWithoutInfosChanged) {
                Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode
                        + ", send onConnectedClientsChanged(clients): "
                        + getConnectedClientList(clients));
                mExecutor.execute(() -> {
                    mCallback.onConnectedClientsChanged(getConnectedClientList(clients));
                });
            }
        }

        @Override
        public void onCapabilityChanged(SoftApCapability capability) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode
                        + ",  onCapabilityChanged: SoftApCapability = " + capability);
            }

            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onCapabilityChanged(capability);
            });
        }

        @Override
        public void onBlockedClientConnecting(@NonNull WifiClient client, int blockedReason) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "SoftApCallbackProxy on mode " + mIpMode
                        + ", onBlockedClientConnecting: client =" + client
                        + " with reason = " + blockedReason);
            }

            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onBlockedClientConnecting(client, blockedReason);
            });
        }
    }

    /**
     * Registers a callback for Soft AP. See {@link SoftApCallback}. Caller will receive the
     * following callbacks on registration:
     * <ul>
     * <li> {@link SoftApCallback#onStateChanged(int, int)}</li>
     * <li> {@link SoftApCallback#onConnectedClientsChanged(List<WifiClient>)}</li>
     * <li> {@link SoftApCallback#onInfoChanged(SoftApInfo)}</li>
     * <li> {@link SoftApCallback#onInfoChanged(List<SoftApInfo>)}</li>
     * <li> {@link SoftApCallback#onCapabilityChanged(SoftApCapability)}</li>
     * </ul>
     *
     * Use {@link SoftApCallback#onConnectedClientsChanged(SoftApInfo, List<WifiClient>)} to know
     * if there are any clients connected to a specific bridged instance of this AP
     * (if bridged AP is enabled).
     *
     * Note: Caller will receive the callback
     * {@link SoftApCallback#onConnectedClientsChanged(SoftApInfo, List<WifiClient>)}
     * on registration when there are clients connected to AP.
     *
     * These will be dispatched on registration to provide the caller with the current state
     * (and are not an indication of any current change). Note that receiving an immediate
     * WIFI_AP_STATE_FAILED value for soft AP state indicates that the latest attempt to start
     * soft AP has failed. Caller can unregister a previously registered callback using
     * {@link #unregisterSoftApCallback}
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#NETWORK_SETTINGS NETWORK_SETTINGS} permission. Callers
     * without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     *
     * @param executor The Executor on whose thread to execute the callbacks of the {@code callback}
     *                 object.
     * @param callback Callback for soft AP events
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public void registerSoftApCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull SoftApCallback callback) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "registerSoftApCallback: callback=" + callback + ", executor=" + executor);

        try {
            synchronized (sSoftApCallbackMap) {
                ISoftApCallback.Stub binderCallback = new SoftApCallbackProxy(executor, callback,
                        IFACE_IP_MODE_TETHERED);
                sSoftApCallbackMap.put(System.identityHashCode(callback), binderCallback);
                mService.registerSoftApCallback(binderCallback);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow callers to unregister a previously registered callback. After calling this method,
     * applications will no longer receive soft AP events.
     *
     * @param callback Callback to unregister for soft AP events
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public void unregisterSoftApCallback(@NonNull SoftApCallback callback) {
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "unregisterSoftApCallback: callback=" + callback);

        try {
            synchronized (sSoftApCallbackMap) {
                int callbackIdentifier = System.identityHashCode(callback);
                if (!sSoftApCallbackMap.contains(callbackIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + callbackIdentifier);
                    return;
                }
                mService.unregisterSoftApCallback(sSoftApCallbackMap.get(callbackIdentifier));
                sSoftApCallbackMap.remove(callbackIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * LocalOnlyHotspotReservation that contains the {@link SoftApConfiguration} for the active
     * LocalOnlyHotspot request.
     * <p>
     * Applications requesting LocalOnlyHotspot for sharing will receive an instance of the
     * LocalOnlyHotspotReservation in the
     * {@link LocalOnlyHotspotCallback#onStarted(LocalOnlyHotspotReservation)} call.  This
     * reservation contains the relevant {@link SoftApConfiguration}.
     * When an application is done with the LocalOnlyHotspot, they should call {@link
     * LocalOnlyHotspotReservation#close()}.  Once this happens, the application will not receive
     * any further callbacks. If the LocalOnlyHotspot is stopped due to a
     * user triggered mode change, applications will be notified via the {@link
     * LocalOnlyHotspotCallback#onStopped()} callback.
     */
    public class LocalOnlyHotspotReservation implements AutoCloseable {

        private final CloseGuard mCloseGuard = new CloseGuard();
        private final SoftApConfiguration mSoftApConfig;
        private final WifiConfiguration mWifiConfig;
        private boolean mClosed = false;

        /** @hide */
        @VisibleForTesting
        public LocalOnlyHotspotReservation(SoftApConfiguration config) {
            mSoftApConfig = config;
            mWifiConfig = config.toWifiConfiguration();
            mCloseGuard.open("close");
        }

        /**
         * Returns the {@link WifiConfiguration} of the current Local Only Hotspot (LOHS).
         * May be null if hotspot enabled and security type is not
         * {@code WifiConfiguration.KeyMgmt.None} or {@code WifiConfiguration.KeyMgmt.WPA2_PSK}.
         *
         * @deprecated Use {@code WifiManager#getSoftApConfiguration()} to get the
         * LOHS configuration.
         */
        @Deprecated
        @Nullable
        public WifiConfiguration getWifiConfiguration() {
            return mWifiConfig;
        }

        /**
         * Returns the {@link SoftApConfiguration} of the current Local Only Hotspot (LOHS).
         */
        @NonNull
        public SoftApConfiguration getSoftApConfiguration() {
            return mSoftApConfig;
        }

        @Override
        public void close() {
            try {
                synchronized (mLock) {
                    if (!mClosed) {
                        mClosed = true;
                        stopLocalOnlyHotspot();
                        mCloseGuard.close();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop Local Only Hotspot.");
            } finally {
                Reference.reachabilityFence(this);
            }
        }

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
    }

    /**
     * Callback class for applications to receive updates about the LocalOnlyHotspot status.
     */
    public static class LocalOnlyHotspotCallback {
        /** @hide */
        public static final int REQUEST_REGISTERED = 0;

        public static final int ERROR_NO_CHANNEL = 1;
        public static final int ERROR_GENERIC = 2;
        public static final int ERROR_INCOMPATIBLE_MODE = 3;
        public static final int ERROR_TETHERING_DISALLOWED = 4;

        /** LocalOnlyHotspot start succeeded. */
        public void onStarted(LocalOnlyHotspotReservation reservation) {};

        /**
         * LocalOnlyHotspot stopped.
         * <p>
         * The LocalOnlyHotspot can be disabled at any time by the user.  When this happens,
         * applications will be notified that it was stopped. This will not be invoked when an
         * application calls {@link LocalOnlyHotspotReservation#close()}.
         */
        public void onStopped() {};

        /**
         * LocalOnlyHotspot failed to start.
         * <p>
         * Applications can attempt to call
         * {@link WifiManager#startLocalOnlyHotspot(LocalOnlyHotspotCallback, Handler)} again at
         * a later time.
         * <p>
         * @param reason The reason for failure could be one of: {@link
         * #ERROR_TETHERING_DISALLOWED}, {@link #ERROR_INCOMPATIBLE_MODE},
         * {@link #ERROR_NO_CHANNEL}, or {@link #ERROR_GENERIC}.
         */
        public void onFailed(int reason) { };
    }

    /**
     * Callback proxy for LocalOnlyHotspotCallback objects.
     */
    private static class LocalOnlyHotspotCallbackProxy extends ILocalOnlyHotspotCallback.Stub {
        private final WeakReference<WifiManager> mWifiManager;
        private final Executor mExecutor;
        private final LocalOnlyHotspotCallback mCallback;

        /**
         * Constructs a {@link LocalOnlyHotspotCallbackProxy} using the specified executor.  All
         * callbacks will run using the given executor.
         *
         * @param manager WifiManager
         * @param executor Executor for delivering callbacks.
         * @param callback LocalOnlyHotspotCallback to notify the calling application, or null.
         */
        LocalOnlyHotspotCallbackProxy(
                @NonNull WifiManager manager,
                @NonNull @CallbackExecutor Executor executor,
                @Nullable LocalOnlyHotspotCallback callback) {
            mWifiManager = new WeakReference<>(manager);
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onHotspotStarted(SoftApConfiguration config) {
            WifiManager manager = mWifiManager.get();
            if (manager == null) return;

            if (config == null) {
                Log.e(TAG, "LocalOnlyHotspotCallbackProxy: config cannot be null.");
                onHotspotFailed(LocalOnlyHotspotCallback.ERROR_GENERIC);
                return;
            }
            final LocalOnlyHotspotReservation reservation =
                    manager.new LocalOnlyHotspotReservation(config);
            if (mCallback == null) return;
            mExecutor.execute(() -> mCallback.onStarted(reservation));
        }

        @Override
        public void onHotspotStopped() {
            WifiManager manager = mWifiManager.get();
            if (manager == null) return;

            Log.w(TAG, "LocalOnlyHotspotCallbackProxy: hotspot stopped");
            if (mCallback == null) return;
            mExecutor.execute(() -> mCallback.onStopped());
        }

        @Override
        public void onHotspotFailed(int reason) {
            WifiManager manager = mWifiManager.get();
            if (manager == null) return;

            Log.w(TAG, "LocalOnlyHotspotCallbackProxy: failed to start.  reason: "
                    + reason);
            if (mCallback == null) return;
            mExecutor.execute(() -> mCallback.onFailed(reason));
        }
    }

    /**
     * LocalOnlyHotspotSubscription that is an AutoCloseable object for tracking applications
     * watching for LocalOnlyHotspot changes.
     *
     * @hide
     */
    public class LocalOnlyHotspotSubscription implements AutoCloseable {
        private final CloseGuard mCloseGuard = new CloseGuard();

        /** @hide */
        @VisibleForTesting
        public LocalOnlyHotspotSubscription() {
            mCloseGuard.open("close");
        }

        @Override
        public void close() {
            try {
                unregisterLocalOnlyHotspotObserver();
                mCloseGuard.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister LocalOnlyHotspotObserver.");
            } finally {
                Reference.reachabilityFence(this);
            }
        }

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
    }

    /**
     * Class to notify calling applications that watch for changes in LocalOnlyHotspot of updates.
     *
     * @hide
     */
    public static class LocalOnlyHotspotObserver {
        /**
         * Confirm registration for LocalOnlyHotspotChanges by returning a
         * LocalOnlyHotspotSubscription.
         */
        public void onRegistered(LocalOnlyHotspotSubscription subscription) {};

        /**
         * LocalOnlyHotspot started with the supplied config.
         */
        public void onStarted(SoftApConfiguration config) {};

        /**
         * LocalOnlyHotspot stopped.
         */
        public void onStopped() {};
    }

    /**
     * Callback proxy for LocalOnlyHotspotObserver objects.
     */
    private static class LocalOnlyHotspotObserverProxy extends ILocalOnlyHotspotCallback.Stub {
        private final WeakReference<WifiManager> mWifiManager;
        private final Executor mExecutor;
        private final LocalOnlyHotspotObserver mObserver;

        /**
         * Constructs a {@link LocalOnlyHotspotObserverProxy} using the specified looper.
         * All callbacks will be delivered on the thread of the specified looper.
         *
         * @param manager WifiManager
         * @param executor Executor for delivering callbacks
         * @param observer LocalOnlyHotspotObserver to notify the calling application.
         */
        LocalOnlyHotspotObserverProxy(WifiManager manager, Executor executor,
                final LocalOnlyHotspotObserver observer) {
            mWifiManager = new WeakReference<>(manager);
            mExecutor = executor;
            mObserver = observer;
        }

        public void registered() throws RemoteException {
            WifiManager manager = mWifiManager.get();
            if (manager == null) return;

            mExecutor.execute(() ->
                    mObserver.onRegistered(manager.new LocalOnlyHotspotSubscription()));
        }

        @Override
        public void onHotspotStarted(SoftApConfiguration config) {
            WifiManager manager = mWifiManager.get();
            if (manager == null) return;

            if (config == null) {
                Log.e(TAG, "LocalOnlyHotspotObserverProxy: config cannot be null.");
                return;
            }
            mExecutor.execute(() -> mObserver.onStarted(config));
        }

        @Override
        public void onHotspotStopped() {
            WifiManager manager = mWifiManager.get();
            if (manager == null) return;

            mExecutor.execute(() -> mObserver.onStopped());
        }

        @Override
        public void onHotspotFailed(int reason) {
            // do nothing
        }
    }

    /**
     * Callback proxy for ActionListener objects.
     */
    private class ActionListenerProxy extends IActionListener.Stub {
        private final String mActionTag;
        private final Handler mHandler;
        private final ActionListener mCallback;

        ActionListenerProxy(String actionTag, Looper looper, ActionListener callback) {
            mActionTag = actionTag;
            mHandler = new Handler(looper);
            mCallback = callback;
        }

        @Override
        public void onSuccess() {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "ActionListenerProxy:" + mActionTag + ": onSuccess");
            }
            mHandler.post(() -> {
                mCallback.onSuccess();
            });
        }

        @Override
        public void onFailure(@ActionListenerFailureReason int reason) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "ActionListenerProxy:" + mActionTag + ": onFailure=" + reason);
            }
            mHandler.post(() -> {
                mCallback.onFailure(reason);
            });
        }
    }

    private void connectInternal(@Nullable WifiConfiguration config, int networkId,
            @Nullable ActionListener listener) {
        ActionListenerProxy listenerProxy = null;
        if (listener != null) {
            listenerProxy = new ActionListenerProxy("connect", mLooper, listener);
        }
        try {
            mService.connect(config, networkId, listenerProxy, mContext.getOpPackageName());
        } catch (RemoteException e) {
            if (listenerProxy != null) {
                listenerProxy.onFailure(ActionListener.FAILURE_INTERNAL_ERROR);
            }
        } catch (SecurityException e) {
            if (listenerProxy != null) {
                listenerProxy.onFailure(ActionListener.FAILURE_NOT_AUTHORIZED);
            }
        }
    }

    /**
     * Connect to a network with the given configuration. The network also
     * gets added to the list of configured networks for the foreground user.
     *
     * For a new network, this function is used instead of a
     * sequence of addNetwork(), enableNetwork(), and reconnect()
     *
     * @param config the set of variables that describe the configuration,
     *            contained in a {@link WifiConfiguration} object.
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiManager instance needs to be
     * initialized again
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK
    })
    public void connect(@NonNull WifiConfiguration config, @Nullable ActionListener listener) {
        if (config == null) throw new IllegalArgumentException("config cannot be null");
        connectInternal(config, WifiConfiguration.INVALID_NETWORK_ID, listener);
    }

    /**
     * Connect to a network with the given networkId.
     *
     * This function is used instead of a enableNetwork() and reconnect()
     *
     * <li> This API will cause reconnect if the credentials of the current active
     * connection has been changed.</li>
     * <li> This API will cause reconnect if the current active connection is marked metered.</li>
     *
     * @param networkId the ID of the network as returned by {@link #addNetwork} or {@link
     *        getConfiguredNetworks}.
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiManager instance needs to be
     * initialized again
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK
    })
    public void connect(int networkId, @Nullable ActionListener listener) {
        if (networkId < 0) throw new IllegalArgumentException("Network id cannot be negative");
        connectInternal(null, networkId, listener);
    }

    /**
     * Temporarily disable autojoin for all currently visible and provisioned (saved, suggested)
     * wifi networks except merged carrier networks from the provided subscription ID.
     *
     * Disabled networks will get automatically re-enabled when they are out of range for a period
     * of time, or after the maximum disable duration specified in the framework.
     *
     * Calling {@link #stopRestrictingAutoJoinToSubscriptionId()} will immediately re-enable
     * autojoin on all disabled networks.
     *
     * @param subscriptionId the subscription ID of the carrier whose merged wifi networks won't be
     *                       disabled {@link android.telephony.SubscriptionInfo#getSubscriptionId()}
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    @RequiresApi(Build.VERSION_CODES.S)
    public void startRestrictingAutoJoinToSubscriptionId(int subscriptionId) {
        try {
            mService.startRestrictingAutoJoinToSubscriptionId(subscriptionId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Re-enable autojoin for all non carrier merged wifi networks temporarily disconnected by
     * {@link #startRestrictingAutoJoinToSubscriptionId(int)}.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    @RequiresApi(Build.VERSION_CODES.S)
    public void stopRestrictingAutoJoinToSubscriptionId() {
        try {
            mService.stopRestrictingAutoJoinToSubscriptionId();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Save the given network to the list of configured networks for the
     * foreground user. If the network already exists, the configuration
     * is updated. Any new network is enabled by default.
     *
     * For a new network, this function is used instead of a
     * sequence of addNetwork() and enableNetwork().
     *
     * For an existing network, it accomplishes the task of updateNetwork()
     *
     * <li> This API will cause reconnect if the credentials of the current active
     * connection has been changed.</li>
     * <li> This API will cause disconnect if the current active connection is marked metered.</li>
     *
     * @param config the set of variables that describe the configuration,
     *            contained in a {@link WifiConfiguration} object.
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiManager instance needs to be
     * initialized again
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK
    })
    public void save(@NonNull WifiConfiguration config, @Nullable ActionListener listener) {
        if (config == null) throw new IllegalArgumentException("config cannot be null");
        ActionListenerProxy listenerProxy = null;
        if (listener != null) {
            listenerProxy = new ActionListenerProxy("save", mLooper, listener);
        }
        try {
            mService.save(config, listenerProxy, mContext.getOpPackageName());
        } catch (RemoteException e) {
            if (listenerProxy != null) {
                listenerProxy.onFailure(ActionListener.FAILURE_INTERNAL_ERROR);
            }
        } catch (SecurityException e) {
            if (listenerProxy != null) {
                listenerProxy.onFailure(ActionListener.FAILURE_NOT_AUTHORIZED);
            }
        }
    }

    /**
     * Delete the network from the list of configured networks for the
     * foreground user.
     *
     * This function is used instead of a sequence of removeNetwork()
     *
     * @param config the set of variables that describe the configuration,
     *            contained in a {@link WifiConfiguration} object.
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiManager instance needs to be
     * initialized again
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK
    })
    public void forget(int netId, @Nullable ActionListener listener) {
        if (netId < 0) throw new IllegalArgumentException("Network id cannot be negative");
        ActionListenerProxy listenerProxy = null;
        if (listener != null) {
            listenerProxy = new ActionListenerProxy("forget", mLooper, listener);
        }
        try {
            mService.forget(netId, listenerProxy);
        } catch (RemoteException e) {
            if (listenerProxy != null) {
                listenerProxy.onFailure(ActionListener.FAILURE_INTERNAL_ERROR);
            }
        } catch (SecurityException e) {
            if (listenerProxy != null) {
                listenerProxy.onFailure(ActionListener.FAILURE_NOT_AUTHORIZED);
            }
        }
    }

    /**
     * Disable network
     *
     * @param netId is the network Id
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiManager instance needs to be
     * initialized again
     * @deprecated This API is deprecated. Use {@link #disableNetwork(int)} instead.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK
    })
    @Deprecated
    public void disable(int netId, @Nullable ActionListener listener) {
        if (netId < 0) throw new IllegalArgumentException("Network id cannot be negative");
        // Simple wrapper which forwards the call to disableNetwork. This is a temporary
        // implementation until we can remove this API completely.
        boolean status = disableNetwork(netId);
        if (listener != null) {
            if (status) {
                listener.onSuccess();
            } else {
                listener.onFailure(ActionListener.FAILURE_INTERNAL_ERROR);
            }
        }
    }

    /**
     * Control whether the device will automatically search for and connect to Wi-Fi networks -
     * auto-join Wi-Fi networks. Disabling this option will not impact manual connections - i.e.
     * the user will still be able to manually select and connect to a Wi-Fi network. Disabling
     * this option significantly impacts the device connectivity and is a restricted operation
     * (see below for permissions). Note that disabling this operation will also disable
     * connectivity initiated scanning operations.
     * <p>
     * Disabling the auto-join configuration is a temporary operation (with the exception of a
     * DO/PO caller): it will be reset (to enabled) when the device reboots or the user toggles
     * Wi-Fi off/on. When the caller is a DO/PO then toggling Wi-Fi will not reset the
     * configuration. Additionally, if a DO/PO disables auto-join then it cannot be (re)enabled by
     * a non-DO/PO caller.
     *
     * @param allowAutojoin true to allow auto-join, false to disallow auto-join
     *
     * Available for DO/PO apps.
     * Other apps require {@code android.Manifest.permission#NETWORK_SETTINGS} or
     * {@code android.Manifest.permission#MANAGE_WIFI_NETWORK_SELECTION} permission.
     */
    public void allowAutojoinGlobal(boolean allowAutojoin) {
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            mService.allowAutojoinGlobal(allowAutojoin, mContext.getOpPackageName(), extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Query whether or not auto-join global is enabled/disabled
     * @see #allowAutojoinGlobal(boolean)
     *
     * Available for DO/PO apps.
     * Other apps require {@code android.Manifest.permission#NETWORK_SETTINGS} or
     * {@code android.Manifest.permission#MANAGE_WIFI_NETWORK_SELECTION} permission.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return {@code Boolean} indicating
     *                        whether auto-join global is enabled/disabled.
     *
     * @throws SecurityException if the caller does not have permission.
     * @throws NullPointerException if the caller provided invalid inputs.
     */
    public void queryAutojoinGlobal(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.queryAutojoinGlobal(
                    new IBooleanListener.Stub() {
                        @Override
                        public void onResult(boolean value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the user choice for allowing auto-join to a network.
     * The updated choice will be made available through the updated config supplied by the
     * CONFIGURED_NETWORKS_CHANGED broadcast.
     *
     * @param netId the id of the network to allow/disallow auto-join for.
     * @param allowAutojoin true to allow auto-join, false to disallow auto-join
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void allowAutojoin(int netId, boolean allowAutojoin) {
        try {
            mService.allowAutojoin(netId, allowAutojoin);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Configure auto-join settings for a Passpoint profile.
     *
     * @param fqdn the FQDN (fully qualified domain name) of the passpoint profile.
     * @param allowAutojoin true to enable auto-join, false to disable auto-join.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void allowAutojoinPasspoint(@NonNull String fqdn, boolean allowAutojoin) {
        try {
            mService.allowAutojoinPasspoint(fqdn, allowAutojoin);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Configure MAC randomization setting for a Passpoint profile.
     * MAC randomization is enabled by default.
     *
     * @param fqdn the FQDN (fully qualified domain name) of the passpoint profile.
     * @param enable true to enable MAC randomization, false to disable MAC randomization.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void setMacRandomizationSettingPasspointEnabled(@NonNull String fqdn, boolean enable) {
        try {
            mService.setMacRandomizationSettingPasspointEnabled(fqdn, enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the user's choice of metered override for a Passpoint profile.
     *
     * @param fqdn the FQDN (fully qualified domain name) of the passpoint profile.
     * @param meteredOverride One of three values: {@link WifiConfiguration#METERED_OVERRIDE_NONE},
     *                        {@link WifiConfiguration#METERED_OVERRIDE_METERED},
     *                        {@link WifiConfiguration#METERED_OVERRIDE_NOT_METERED}
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void setPasspointMeteredOverride(@NonNull String fqdn,
            @WifiConfiguration.MeteredOverride int meteredOverride) {
        try {
            mService.setPasspointMeteredOverride(fqdn, meteredOverride);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Temporarily disable a network. Should always trigger with user disconnect network.
     *
     * @param network Input can be SSID or FQDN. And caller must ensure that the SSID passed thru
     *                this API matched the WifiConfiguration.SSID rules, and thus be surrounded by
     *                quotes.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK
    })
    public void disableEphemeralNetwork(@NonNull String network) {
        if (TextUtils.isEmpty(network)) {
            throw new IllegalArgumentException("SSID cannot be null or empty!");
        }
        try {
            mService.disableEphemeralNetwork(network, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * WPS suport has been deprecated from Client mode and this method will immediately trigger
     * {@link WpsCallback#onFailed(int)} with a generic error.
     *
     * @param config WPS configuration (does not support {@link WpsInfo#LABEL})
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiManager instance needs to be initialized again
     * @deprecated This API is deprecated
     */
    public void startWps(WpsInfo config, WpsCallback listener) {
        if (listener != null ) {
            listener.onFailed(ActionListener.FAILURE_INTERNAL_ERROR);
        }
    }

    /**
     * WPS support has been deprecated from Client mode and this method will immediately trigger
     * {@link WpsCallback#onFailed(int)} with a generic error.
     *
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiManager instance needs to be initialized again
     * @deprecated This API is deprecated
     */
    public void cancelWps(WpsCallback listener) {
        if (listener != null) {
            listener.onFailed(ActionListener.FAILURE_INTERNAL_ERROR);
        }
    }

    /**
     * Allows an application to keep the Wi-Fi radio awake.
     * Normally the Wi-Fi radio may turn off when the user has not used the device in a while.
     * Acquiring a WifiLock will keep the radio on until the lock is released.  Multiple
     * applications may hold WifiLocks, and the radio will only be allowed to turn off when no
     * WifiLocks are held in any application.
     * <p>
     * Before using a WifiLock, consider carefully if your application requires Wi-Fi access, or
     * could function over a mobile network, if available.  A program that needs to download large
     * files should hold a WifiLock to ensure that the download will complete, but a program whose
     * network usage is occasional or low-bandwidth should not hold a WifiLock to avoid adversely
     * affecting battery life.
     * <p>
     * Note that WifiLocks cannot override the user-level "Wi-Fi Enabled" setting, nor Airplane
     * Mode.  They simply keep the radio from turning off when Wi-Fi is already on but the device
     * is idle.
     * <p>
     * Any application using a WifiLock must request the {@code android.permission.WAKE_LOCK}
     * permission in an {@code <uses-permission>} element of the application's manifest.
     */
    public class WifiLock {
        private String mTag;
        private final IBinder mBinder;
        private int mRefCount;
        int mLockType;
        private boolean mRefCounted;
        private boolean mHeld;
        private WorkSource mWorkSource;

        private WifiLock(int lockType, String tag) {
            mTag = tag;
            mLockType = lockType;
            mBinder = new Binder();
            mRefCount = 0;
            mRefCounted = true;
            mHeld = false;
        }

        /**
         * Locks the Wi-Fi radio on until {@link #release} is called.
         *
         * If this WifiLock is reference-counted, each call to {@code acquire} will increment the
         * reference count, and the radio will remain locked as long as the reference count is
         * above zero.
         *
         * If this WifiLock is not reference-counted, the first call to {@code acquire} will lock
         * the radio, but subsequent calls will be ignored.  Only one call to {@link #release}
         * will be required, regardless of the number of times that {@code acquire} is called.
         */
        public void acquire() {
            synchronized (mBinder) {
                if (mRefCounted ? (++mRefCount == 1) : (!mHeld)) {
                    try {
                        mService.acquireWifiLock(mBinder, mLockType, mTag, mWorkSource);
                        synchronized (WifiManager.this) {
                            if (mActiveLockCount >= MAX_ACTIVE_LOCKS) {
                                mService.releaseWifiLock(mBinder);
                                throw new UnsupportedOperationException(
                                            "Exceeded maximum number of wifi locks");
                            }
                            mActiveLockCount++;
                        }
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                    mHeld = true;
                }
            }
        }

        /**
         * Unlocks the Wi-Fi radio, allowing it to turn off when the device is idle.
         *
         * If this WifiLock is reference-counted, each call to {@code release} will decrement the
         * reference count, and the radio will be unlocked only when the reference count reaches
         * zero.  If the reference count goes below zero (that is, if {@code release} is called
         * a greater number of times than {@link #acquire}), an exception is thrown.
         *
         * If this WifiLock is not reference-counted, the first call to {@code release} (after
         * the radio was locked using {@link #acquire}) will unlock the radio, and subsequent
         * calls will be ignored.
         */
        public void release() {
            synchronized (mBinder) {
                if (mRefCounted ? (--mRefCount == 0) : (mHeld)) {
                    try {
                        mService.releaseWifiLock(mBinder);
                        synchronized (WifiManager.this) {
                            mActiveLockCount--;
                        }
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                    mHeld = false;
                }
                if (mRefCount < 0) {
                    throw new RuntimeException("WifiLock under-locked " + mTag);
                }
            }
        }

        /**
         * Controls whether this is a reference-counted or non-reference-counted WifiLock.
         *
         * Reference-counted WifiLocks keep track of the number of calls to {@link #acquire} and
         * {@link #release}, and only allow the radio to sleep when every call to {@link #acquire}
         * has been balanced with a call to {@link #release}.  Non-reference-counted WifiLocks
         * lock the radio whenever {@link #acquire} is called and it is unlocked, and unlock the
         * radio whenever {@link #release} is called and it is locked.
         *
         * @param refCounted true if this WifiLock should keep a reference count
         */
        public void setReferenceCounted(boolean refCounted) {
            mRefCounted = refCounted;
        }

        /**
         * Checks whether this WifiLock is currently held.
         *
         * @return true if this WifiLock is held, false otherwise
         */
        public boolean isHeld() {
            synchronized (mBinder) {
                return mHeld;
            }
        }

        public void setWorkSource(WorkSource ws) {
            synchronized (mBinder) {
                if (ws != null && ws.isEmpty()) {
                    ws = null;
                }
                boolean changed = true;
                if (ws == null) {
                    mWorkSource = null;
                } else {
                    ws = ws.withoutNames();
                    if (mWorkSource == null) {
                        changed = mWorkSource != null;
                        mWorkSource = new WorkSource(ws);
                    } else {
                        changed = !mWorkSource.equals(ws);
                        if (changed) {
                            mWorkSource.set(ws);
                        }
                    }
                }
                if (changed && mHeld) {
                    try {
                        mService.updateWifiLockWorkSource(mBinder, mWorkSource);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }

        public String toString() {
            String s1, s2, s3;
            synchronized (mBinder) {
                s1 = Integer.toHexString(System.identityHashCode(this));
                s2 = mHeld ? "held; " : "";
                if (mRefCounted) {
                    s3 = "refcounted: refcount = " + mRefCount;
                } else {
                    s3 = "not refcounted";
                }
                return "WifiLock{ " + s1 + "; " + s2 + s3 + " }";
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            synchronized (mBinder) {
                if (mHeld) {
                    try {
                        mService.releaseWifiLock(mBinder);
                        synchronized (WifiManager.this) {
                            mActiveLockCount--;
                        }
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    /**
     * Creates a new WifiLock.
     *
     * @param lockType the type of lock to create. See {@link #WIFI_MODE_FULL_HIGH_PERF}
     * and {@link #WIFI_MODE_FULL_LOW_LATENCY} for descriptions of the types of Wi-Fi locks.
     * @param tag a tag for the WifiLock to identify it in debugging messages.  This string is
     *            never shown to the user under normal conditions, but should be descriptive
     *            enough to identify your application and the specific WifiLock within it, if it
     *            holds multiple WifiLocks.
     *
     * @return a new, unacquired WifiLock with the given tag.
     *
     * @see WifiLock
     */
    public WifiLock createWifiLock(int lockType, String tag) {
        return new WifiLock(lockType, tag);
    }

    /**
     * Interface for low latency lock listener. Should be extended by application and
     * set when calling {@link WifiManager#addWifiLowLatencyLockListener(Executor,
     * WifiLowLatencyLockListener)}.
     *
     * @hide
     */
    public interface WifiLowLatencyLockListener {
        /**
         * Provides low latency mode is activated or not. Triggered when Wi-Fi chip enters into low
         * latency mode.
         *
         * Note: Always called with current state when a new listener gets registered.
         */
        void onActivatedStateChanged(boolean activated);

        /**
         * Provides UIDs (lock owners) of the applications which currently acquired low latency
         * lock. Triggered when an application acquires or releases a lock.
         *
         * Note: Always called with UIDs of the current acquired locks when a new listener gets
         * registered.
         *
         * @param ownerUids An array of UIDs.
         */
        default void onOwnershipChanged(@NonNull int[] ownerUids) {}

        /**
         * Provides UIDs of the applications which acquired the low latency lock and is currently
         * active. See {@link WifiManager#WIFI_MODE_FULL_LOW_LATENCY} for the conditions to be
         * met for low latency lock to be active. Triggered when application acquiring the lock
         * satisfies or does not satisfy low latency conditions when the low latency mode is
         * activated. Also gets triggered when the lock becomes active, immediately after the
         * {@link WifiLowLatencyLockListener#onActivatedStateChanged(boolean)} callback is
         * triggered.
         *
         * Note: Always called with UIDs of the current active locks when a new listener gets
         * registered if the Wi-Fi chip is in low latency mode.
         *
         * @param activeUids An array of UIDs.
         */
        default void onActiveUsersChanged(@NonNull int[] activeUids) {}
    }

    /**
     * Helper class to support wifi low latency lock listener.
     */
    private static class OnWifiLowLatencyLockProxy extends IWifiLowLatencyLockListener.Stub {
        @NonNull
        private Executor mExecutor;
        @NonNull
        private WifiLowLatencyLockListener mListener;

        OnWifiLowLatencyLockProxy(@NonNull Executor executor,
                @NonNull WifiLowLatencyLockListener listener) {
            Objects.requireNonNull(executor);
            Objects.requireNonNull(listener);
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public void onActivatedStateChanged(boolean activated) {
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mListener.onActivatedStateChanged(activated));

        }

        @Override
        public void onOwnershipChanged(@NonNull int[] ownerUids) {
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mListener.onOwnershipChanged(ownerUids));

        }

        @Override
        public void onActiveUsersChanged(@NonNull int[] activeUids) {
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mListener.onActiveUsersChanged(activeUids));
        }
    }

    /**
     * Add a listener for monitoring the low latency lock. The caller can unregister a previously
     * registered listener using {@link WifiManager#removeWifiLowLatencyLockListener(
     * WifiLowLatencyLockListener)}.
     *
     * Applications should have the {@link android.Manifest.permission#NETWORK_SETTINGS} and
     * {@link android.Manifest.permission#MANAGE_WIFI_NETWORK_SELECTION} permission. Callers
     * without the permission will trigger a {@link java.lang.SecurityException}.
     *
     * @param executor The Executor on which to execute the callbacks.
     * @param listener The listener for the latency mode change.
     * @throws IllegalArgumentException if incorrect input arguments are provided.
     * @throws SecurityException if the caller is not allowed to call this API
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(anyOf = {android.Manifest.permission.NETWORK_SETTINGS,
            MANAGE_WIFI_NETWORK_SELECTION})
    public void addWifiLowLatencyLockListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull WifiLowLatencyLockListener listener) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.d(TAG, "addWifiLowLatencyLockListener: listener=" + listener + ", executor="
                    + executor);
        }
        final int listenerIdentifier = System.identityHashCode(listener);
        try {
            synchronized (sWifiLowLatencyLockListenerMap) {
                IWifiLowLatencyLockListener.Stub listenerProxy = new OnWifiLowLatencyLockProxy(
                        executor,
                        listener);
                sWifiLowLatencyLockListenerMap.put(listenerIdentifier, listenerProxy);
                mService.addWifiLowLatencyLockListener(listenerProxy);
            }
        } catch (RemoteException e) {
            sWifiLowLatencyLockListenerMap.remove(listenerIdentifier);
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Removes a listener added using {@link WifiManager#addWifiLowLatencyLockListener(Executor,
     * WifiLowLatencyLockListener)}. After calling this method, applications will no longer
     * receive low latency mode notifications.
     *
     * @param listener the listener to be removed.
     * @throws IllegalArgumentException if incorrect input arguments are provided.
     * @hide
     */
    public void removeWifiLowLatencyLockListener(@NonNull WifiLowLatencyLockListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.d(TAG, "removeWifiLowLatencyLockListener: listener=" + listener);
        }
        final int listenerIdentifier = System.identityHashCode(listener);
        synchronized (sWifiLowLatencyLockListenerMap) {
            try {
                if (!sWifiLowLatencyLockListenerMap.contains(listenerIdentifier)) {
                    Log.w(TAG, "Unknown external listener " + listenerIdentifier);
                    return;
                }
                mService.removeWifiLowLatencyLockListener(
                        sWifiLowLatencyLockListenerMap.get(listenerIdentifier));

            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } finally {
                sWifiLowLatencyLockListenerMap.remove(listenerIdentifier);
            }
        }
    }

    /**
     * Creates a new WifiLock.
     *
     * @param tag a tag for the WifiLock to identify it in debugging messages.  This string is
     *            never shown to the user under normal conditions, but should be descriptive
     *            enough to identify your application and the specific WifiLock within it, if it
     *            holds multiple WifiLocks.
     *
     * @return a new, unacquired WifiLock with the given tag.
     *
     * @see WifiLock
     *
     * @deprecated This API is non-functional.
     */
    @Deprecated
    public WifiLock createWifiLock(String tag) {
        return new WifiLock(WIFI_MODE_FULL, tag);
    }

    /**
     * Create a new MulticastLock
     *
     * @param tag a tag for the MulticastLock to identify it in debugging
     *            messages.  This string is never shown to the user under
     *            normal conditions, but should be descriptive enough to
     *            identify your application and the specific MulticastLock
     *            within it, if it holds multiple MulticastLocks.
     *
     * @return a new, unacquired MulticastLock with the given tag.
     *
     * @see MulticastLock
     */
    public MulticastLock createMulticastLock(String tag) {
        return new MulticastLock(tag);
    }

    /**
     * Allows an application to receive Wifi Multicast packets.
     * Normally the Wifi stack filters out packets not explicitly
     * addressed to this device.  Acquring a MulticastLock will
     * cause the stack to receive packets addressed to multicast
     * addresses.  Processing these extra packets can cause a noticeable
     * battery drain and should be disabled when not needed.
     */
    public class MulticastLock {
        private String mTag;
        private final IBinder mBinder;
        private int mRefCount;
        private boolean mRefCounted;
        private boolean mHeld;

        private MulticastLock(String tag) {
            mTag = tag;
            mBinder = new Binder();
            mRefCount = 0;
            mRefCounted = true;
            mHeld = false;
        }

        /**
         * Locks Wifi Multicast on until {@link #release} is called.
         *
         * If this MulticastLock is reference-counted each call to
         * {@code acquire} will increment the reference count, and the
         * wifi interface will receive multicast packets as long as the
         * reference count is above zero.
         *
         * If this MulticastLock is not reference-counted, the first call to
         * {@code acquire} will turn on the multicast packets, but subsequent
         * calls will be ignored.  Only one call to {@link #release} will
         * be required, regardless of the number of times that {@code acquire}
         * is called.
         *
         * Note that other applications may also lock Wifi Multicast on.
         * Only they can relinquish their lock.
         *
         * Also note that applications cannot leave Multicast locked on.
         * When an app exits or crashes, any Multicast locks will be released.
         */
        public void acquire() {
            synchronized (mBinder) {
                if (mRefCounted ? (++mRefCount == 1) : (!mHeld)) {
                    try {
                        mService.acquireMulticastLock(mBinder, mTag);
                        synchronized (WifiManager.this) {
                            if (mActiveLockCount >= MAX_ACTIVE_LOCKS) {
                                mService.releaseMulticastLock(mTag);
                                throw new UnsupportedOperationException(
                                        "Exceeded maximum number of wifi locks");
                            }
                            mActiveLockCount++;
                        }
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                    mHeld = true;
                }
            }
        }

        /**
         * Unlocks Wifi Multicast, restoring the filter of packets
         * not addressed specifically to this device and saving power.
         *
         * If this MulticastLock is reference-counted, each call to
         * {@code release} will decrement the reference count, and the
         * multicast packets will only stop being received when the reference
         * count reaches zero.  If the reference count goes below zero (that
         * is, if {@code release} is called a greater number of times than
         * {@link #acquire}), an exception is thrown.
         *
         * If this MulticastLock is not reference-counted, the first call to
         * {@code release} (after the radio was multicast locked using
         * {@link #acquire}) will unlock the multicast, and subsequent calls
         * will be ignored.
         *
         * Note that if any other Wifi Multicast Locks are still outstanding
         * this {@code release} call will not have an immediate effect.  Only
         * when all applications have released all their Multicast Locks will
         * the Multicast filter be turned back on.
         *
         * Also note that when an app exits or crashes all of its Multicast
         * Locks will be automatically released.
         */
        public void release() {
            synchronized (mBinder) {
                if (mRefCounted ? (--mRefCount == 0) : (mHeld)) {
                    try {
                        mService.releaseMulticastLock(mTag);
                        synchronized (WifiManager.this) {
                            mActiveLockCount--;
                        }
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                    mHeld = false;
                }
                if (mRefCount < 0) {
                    throw new RuntimeException("MulticastLock under-locked "
                            + mTag);
                }
            }
        }

        /**
         * Controls whether this is a reference-counted or non-reference-
         * counted MulticastLock.
         *
         * Reference-counted MulticastLocks keep track of the number of calls
         * to {@link #acquire} and {@link #release}, and only stop the
         * reception of multicast packets when every call to {@link #acquire}
         * has been balanced with a call to {@link #release}.  Non-reference-
         * counted MulticastLocks allow the reception of multicast packets
         * whenever {@link #acquire} is called and stop accepting multicast
         * packets whenever {@link #release} is called.
         *
         * @param refCounted true if this MulticastLock should keep a reference
         * count
         */
        public void setReferenceCounted(boolean refCounted) {
            mRefCounted = refCounted;
        }

        /**
         * Checks whether this MulticastLock is currently held.
         *
         * @return true if this MulticastLock is held, false otherwise
         */
        public boolean isHeld() {
            synchronized (mBinder) {
                return mHeld;
            }
        }

        public String toString() {
            String s1, s2, s3;
            synchronized (mBinder) {
                s1 = Integer.toHexString(System.identityHashCode(this));
                s2 = mHeld ? "held; " : "";
                if (mRefCounted) {
                    s3 = "refcounted: refcount = " + mRefCount;
                } else {
                    s3 = "not refcounted";
                }
                return "MulticastLock{ " + s1 + "; " + s2 + s3 + " }";
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            setReferenceCounted(false);
            release();
        }
    }

    /**
     * Check multicast filter status.
     *
     * @return true if multicast packets are allowed.
     *
     * @hide pending API council approval
     */
    public boolean isMulticastEnabled() {
        try {
            return mService.isMulticastEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Initialize the multicast filtering to 'on'
     * @hide no intent to publish
     */
    @UnsupportedAppUsage
    public boolean initializeMulticastFiltering() {
        try {
            mService.initializeMulticastFiltering();
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set Wi-Fi verbose logging level from developer settings.
     *
     * @param enable true to enable verbose logging, false to disable.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void setVerboseLoggingEnabled(boolean enable) {
        enableVerboseLogging(enable ? VERBOSE_LOGGING_LEVEL_ENABLED
                : VERBOSE_LOGGING_LEVEL_DISABLED);
    }

    /**
     * Set Wi-Fi verbose logging level from developer settings.
     *
     * @param verbose the verbose logging mode which could be
     * {@link #VERBOSE_LOGGING_LEVEL_DISABLED}, {@link #VERBOSE_LOGGING_LEVEL_ENABLED}, or
     * {@link #VERBOSE_LOGGING_LEVEL_ENABLED_SHOW_KEY}.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void setVerboseLoggingLevel(@VerboseLoggingLevel int verbose) {
        enableVerboseLogging(verbose);
    }

    /** @hide */
    @UnsupportedAppUsage(
            maxTargetSdk = Build.VERSION_CODES.Q,
            publicAlternatives = "Use {@code #setVerboseLoggingEnabled(boolean)} instead."
    )
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void enableVerboseLogging(@VerboseLoggingLevel int verbose) {
        try {
            mService.enableVerboseLogging(verbose);
            mVerboseLoggingEnabled = verbose == VERBOSE_LOGGING_LEVEL_ENABLED;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the persisted Wi-Fi verbose logging level, set by
     * {@link #setVerboseLoggingEnabled(boolean)} or {@link #setVerboseLoggingLevel(int)}.
     * No permissions are required to call this method.
     *
     * @return true to indicate that verbose logging is enabled, false to indicate that verbose
     * logging is disabled.
     *
     * @hide
     */
    @SystemApi
    public boolean isVerboseLoggingEnabled() {
        return getVerboseLoggingLevel() > 0;
    }

    /**
     * Get the persisted Wi-Fi verbose logging level, set by
     * {@link #setVerboseLoggingEnabled(boolean)} or {@link #setVerboseLoggingLevel(int)}.
     * No permissions are required to call this method.
     *
     * @return one of {@link #VERBOSE_LOGGING_LEVEL_DISABLED},
     *         {@link #VERBOSE_LOGGING_LEVEL_ENABLED},
     *         or {@link #VERBOSE_LOGGING_LEVEL_ENABLED_SHOW_KEY}.
     *
     * @hide
     */
    @SystemApi
    public @VerboseLoggingLevel int getVerboseLoggingLevel() {
        try {
            return mService.getVerboseLoggingLevel();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Removes all saved Wi-Fi networks, Passpoint configurations, ephemeral networks, Network
     * Requests, and Network Suggestions.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void factoryReset() {
        try {
            mService.factoryReset(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get {@link Network} object of current wifi network, or null if not connected.
     * @hide
     */
    @Nullable
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    public Network getCurrentNetwork() {
        try {
            return mService.getCurrentNetwork();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Deprecated
     * returns false
     * @hide
     * @deprecated
     */
    public boolean setEnableAutoJoinWhenAssociated(boolean enabled) {
        return false;
    }

    /**
     * Deprecated
     * returns false
     * @hide
     * @deprecated
     */
    public boolean getEnableAutoJoinWhenAssociated() {
        return false;
    }

    /**
     * Returns a byte stream representing the data that needs to be backed up to save the
     * current Wifi state.
     * This Wifi state can be restored by calling {@link #restoreBackupData(byte[])}.
     * @hide
     */
    @NonNull
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public byte[] retrieveBackupData() {
        try {
            return mService.retrieveBackupData();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Restore state from the backed up data.
     * @param data byte stream in the same format produced by {@link #retrieveBackupData()}
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void restoreBackupData(@NonNull byte[] data) {
        try {
            mService.restoreBackupData(data);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a byte stream representing the data that needs to be backed up to save the
     * current soft ap config data.
     *
     * This soft ap config can be restored by calling {@link #restoreSoftApBackupData(byte[])}
     * @hide
     */
    @NonNull
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public byte[] retrieveSoftApBackupData() {
        try {
            return mService.retrieveSoftApBackupData();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns soft ap config from the backed up data or null if data is invalid.
     * @param data byte stream in the same format produced by {@link #retrieveSoftApBackupData()}
     *
     * @hide
     */
    @Nullable
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public SoftApConfiguration restoreSoftApBackupData(@NonNull byte[] data) {
        try {
            return mService.restoreSoftApBackupData(data);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Restore state from the older version of back up data.
     * The old backup data was essentially a backup of wpa_supplicant.conf
     * and ipconfig.txt file.
     * @param supplicantData bytes representing wpa_supplicant.conf
     * @param ipConfigData bytes representing ipconfig.txt
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void restoreSupplicantBackupData(
            @NonNull byte[] supplicantData, @NonNull byte[] ipConfigData) {
        try {
            mService.restoreSupplicantBackupData(supplicantData, ipConfigData);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Start subscription provisioning flow
     *
     * @param provider {@link OsuProvider} to provision with
     * @param executor the Executor on which to run the callback.
     * @param callback {@link ProvisioningCallback} for updates regarding provisioning flow
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    public void startSubscriptionProvisioning(@NonNull OsuProvider provider,
            @NonNull @CallbackExecutor Executor executor, @NonNull ProvisioningCallback callback) {
        // Verify arguments
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        try {
            mService.startSubscriptionProvisioning(provider,
                    new ProvisioningCallbackProxy(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Helper class to support OSU Provisioning callbacks
     */
    private static class ProvisioningCallbackProxy extends IProvisioningCallback.Stub {
        private final Executor mExecutor;
        private final ProvisioningCallback mCallback;

        ProvisioningCallbackProxy(Executor executor, ProvisioningCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onProvisioningStatus(int status) {
            mExecutor.execute(() -> mCallback.onProvisioningStatus(status));
        }

        @Override
        public void onProvisioningFailure(int status) {
            mExecutor.execute(() -> mCallback.onProvisioningFailure(status));
        }

        @Override
        public void onProvisioningComplete() {
            mExecutor.execute(() -> mCallback.onProvisioningComplete());
        }
    }

    /**
     * Interface for Traffic state callback. Should be extended by applications and set when
     * calling {@link #registerTrafficStateCallback(Executor, WifiManager.TrafficStateCallback)}.
     * @hide
     */
    @SystemApi
    public interface TrafficStateCallback {
        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = {"DATA_ACTIVITY_"}, value = {
                DATA_ACTIVITY_NONE,
                DATA_ACTIVITY_IN,
                DATA_ACTIVITY_OUT,
                DATA_ACTIVITY_INOUT})
        @interface DataActivity {}

        // Lowest bit indicates data reception and the second lowest bit indicates data transmitted
        /** No data in or out */
        int DATA_ACTIVITY_NONE         = 0x00;
        /** Data in, no data out */
        int DATA_ACTIVITY_IN           = 0x01;
        /** Data out, no data in */
        int DATA_ACTIVITY_OUT          = 0x02;
        /** Data in and out */
        int DATA_ACTIVITY_INOUT        = 0x03;

        /**
         * Callback invoked to inform clients about the current traffic state.
         *
         * @param state One of the values: {@link #DATA_ACTIVITY_NONE}, {@link #DATA_ACTIVITY_IN},
         * {@link #DATA_ACTIVITY_OUT} & {@link #DATA_ACTIVITY_INOUT}.
         */
        void onStateChanged(@DataActivity int state);
    }

    /**
     * Callback proxy for TrafficStateCallback objects.
     *
     * @hide
     */
    private class TrafficStateCallbackProxy extends ITrafficStateCallback.Stub {
        private final Executor mExecutor;
        private final TrafficStateCallback mCallback;

        TrafficStateCallbackProxy(Executor executor, TrafficStateCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onStateChanged(int state) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "TrafficStateCallbackProxy: onStateChanged state=" + state);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mCallback.onStateChanged(state);
            });
        }
    }

    /**
     * Registers a callback for monitoring traffic state. See {@link TrafficStateCallback}. These
     * callbacks will be invoked periodically by platform to inform clients about the current
     * traffic state. Caller can unregister a previously registered callback using
     * {@link #unregisterTrafficStateCallback(TrafficStateCallback)}
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#NETWORK_SETTINGS NETWORK_SETTINGS} permission. Callers
     * without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     *
     * @param executor The Executor on whose thread to execute the callbacks of the {@code callback}
     *                 object.
     * @param callback Callback for traffic state events
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void registerTrafficStateCallback(@NonNull @CallbackExecutor Executor executor,
                                             @NonNull TrafficStateCallback callback) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "registerTrafficStateCallback: callback=" + callback + ", executor=" + executor);

        try {
            synchronized (sTrafficStateCallbackMap) {
                ITrafficStateCallback.Stub binderCallback = new TrafficStateCallbackProxy(executor,
                        callback);
                sTrafficStateCallbackMap.put(System.identityHashCode(callback), binderCallback);
                mService.registerTrafficStateCallback(binderCallback);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow callers to unregister a previously registered callback. After calling this method,
     * applications will no longer receive traffic state notifications.
     *
     * @param callback Callback to unregister for traffic state events
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void unregisterTrafficStateCallback(@NonNull TrafficStateCallback callback) {
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "unregisterTrafficStateCallback: callback=" + callback);

        try {
            synchronized (sTrafficStateCallbackMap) {
                int callbackIdentifier = System.identityHashCode(callback);
                if (!sTrafficStateCallbackMap.contains(callbackIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + callbackIdentifier);
                    return;
                }
                mService.unregisterTrafficStateCallback(
                        sTrafficStateCallbackMap.get(callbackIdentifier));
                sTrafficStateCallbackMap.remove(callbackIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Helper method to update the local verbose logging flag based on the verbose logging
     * level from wifi service.
     */
    private void updateVerboseLoggingEnabledFromService() {
        mVerboseLoggingEnabled = isVerboseLoggingEnabled();
    }

    /**
     * @return true if this device supports WPA3-Personal SAE
     */
    public boolean isWpa3SaeSupported() {
        return isFeatureSupported(WIFI_FEATURE_WPA3_SAE);
    }

    /**
     * @return true if this device supports WPA3-Enterprise Suite-B-192
     */
    public boolean isWpa3SuiteBSupported() {
        return isFeatureSupported(WIFI_FEATURE_WPA3_SUITE_B);
    }

    /**
     * @return true if this device supports Wi-Fi Enhanced Open (OWE)
     */
    public boolean isEnhancedOpenSupported() {
        return isFeatureSupported(WIFI_FEATURE_OWE);
    }

    /**
     * Wi-Fi Easy Connect (DPP) introduces standardized mechanisms to simplify the provisioning and
     * configuration of Wi-Fi devices.
     * For more details, visit <a href="https://www.wi-fi.org/">https://www.wi-fi.org/</a> and
     * search for "Easy Connect" or "Device Provisioning Protocol specification".
     *
     * @return true if this device supports Wi-Fi Easy-connect (Device Provisioning Protocol)
     */
    public boolean isEasyConnectSupported() {
        return isFeatureSupported(WIFI_FEATURE_DPP);
    }

    /**
     * @return true if this device supports Wi-Fi Easy Connect (DPP) Enrollee Responder mode.
     */
    public boolean isEasyConnectEnrolleeResponderModeSupported() {
        return isFeatureSupported(WIFI_FEATURE_DPP_ENROLLEE_RESPONDER);
    }

    /**
     * @return true if this device supports WAPI.
     */
    public boolean isWapiSupported() {
        return isFeatureSupported(WIFI_FEATURE_WAPI);
    }

    /**
     * @return true if this device supports WPA3 SAE Public Key.
     */
    public boolean isWpa3SaePublicKeySupported() {
        // This feature is not fully implemented in the framework yet.
        // After the feature complete, it returns whether WIFI_FEATURE_SAE_PK
        // is supported or not directly.
        return false;
    }

    /**
     * @return true if this device supports Wi-Fi Passpoint Terms and Conditions feature.
     */
    public boolean isPasspointTermsAndConditionsSupported() {
        return isFeatureSupported(WIFI_FEATURE_PASSPOINT_TERMS_AND_CONDITIONS);
    }

    /**
     * @return true if this device supports WPA3 SAE Hash-to-Element.
     */
    public boolean isWpa3SaeH2eSupported() {
        return isFeatureSupported(WIFI_FEATURE_SAE_H2E);
    }

    /**
     * @return true if this device supports Wi-Fi Display R2.
     */
    public boolean isWifiDisplayR2Supported() {
        return isFeatureSupported(WIFI_FEATURE_WFD_R2);
    }

    /**
     * @return true if this device supports RFC 7542 decorated identity.
     */
    public boolean isDecoratedIdentitySupported() {
        return isFeatureSupported(WIFI_FEATURE_DECORATED_IDENTITY);
    }

    /**
     * @return true if this device supports Trust On First Use (TOFU).
     */
    public boolean isTrustOnFirstUseSupported() {
        return isFeatureSupported(WIFI_FEATURE_TRUST_ON_FIRST_USE);
    }

    /**
     * Wi-Fi Easy Connect DPP AKM enables provisioning and configuration of Wi-Fi devices without
     * the need of using the device PSK passphrase.
     * For more details, visit <a href="https://www.wi-fi.org/">https://www.wi-fi.org/</a> and
     * search for "Easy Connect" or "Device Provisioning Protocol specification".
     *
     * @return true if this device supports Wi-Fi Easy-connect DPP (Device Provisioning Protocol)
     * AKM, false otherwise.
     */
    public boolean isEasyConnectDppAkmSupported() {
        return isFeatureSupported(WIFI_FEATURE_DPP_AKM);
    }

    /**
     * Indicate that whether or not settings required TLS minimum version is supported.
     *
     * If the device doesn't support this capability, the minimum accepted TLS version is 1.0.
     *
     * @return true if this device supports setting TLS minimum version.
     */
    public boolean isTlsMinimumVersionSupported() {
        return isFeatureSupported(WIFI_FEATURE_SET_TLS_MINIMUM_VERSION);
    }

    /**
     * Indicate that whether or not TLS v1.3 is supported.
     *
     * If requested minimum is not supported, it will default to the maximum supported version.
     *
     * @return true if this device supports TLS v1.3.
     */
    public boolean isTlsV13Supported() {
        return isFeatureSupported(WIFI_FEATURE_TLS_V1_3);
    }

    /**
     * @return true if this device supports Dual Band Simultaneous (DBS) operation.
     */
    public boolean isDualBandSimultaneousSupported() {
        return isFeatureSupported(WIFI_FEATURE_DUAL_BAND_SIMULTANEOUS);
    }

    /**
     * @return true if this device supports TID-To-Link Mapping Negotiation.
     */
    public boolean isTidToLinkMappingNegotiationSupported() {
        return isFeatureSupported(WIFI_FEATURE_T2LM_NEGOTIATION);
    }

    /**
     * Gets the factory Wi-Fi MAC addresses.
     * @return Array of String representing Wi-Fi MAC addresses sorted lexically or an empty Array
     * if failed.
     * @hide
     */
    @NonNull
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public String[] getFactoryMacAddresses() {
        try {
            return mService.getFactoryMacAddresses();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"DEVICE_MOBILITY_STATE_"}, value = {
            DEVICE_MOBILITY_STATE_UNKNOWN,
            DEVICE_MOBILITY_STATE_HIGH_MVMT,
            DEVICE_MOBILITY_STATE_LOW_MVMT,
            DEVICE_MOBILITY_STATE_STATIONARY})
    public @interface DeviceMobilityState {}

    /**
     * Unknown device mobility state
     *
     * @see #setDeviceMobilityState(int)
     *
     * @hide
     */
    @SystemApi
    public static final int DEVICE_MOBILITY_STATE_UNKNOWN = 0;

    /**
     * High movement device mobility state.
     * e.g. on a bike, in a motor vehicle
     *
     * @see #setDeviceMobilityState(int)
     *
     * @hide
     */
    @SystemApi
    public static final int DEVICE_MOBILITY_STATE_HIGH_MVMT = 1;

    /**
     * Low movement device mobility state.
     * e.g. walking, running
     *
     * @see #setDeviceMobilityState(int)
     *
     * @hide
     */
    @SystemApi
    public static final int DEVICE_MOBILITY_STATE_LOW_MVMT = 2;

    /**
     * Stationary device mobility state
     *
     * @see #setDeviceMobilityState(int)
     *
     * @hide
     */
    @SystemApi
    public static final int DEVICE_MOBILITY_STATE_STATIONARY = 3;

    /**
     * Updates the device mobility state. Wifi uses this information to adjust the interval between
     * Wifi scans in order to balance power consumption with scan accuracy.
     * The default mobility state when the device boots is {@link #DEVICE_MOBILITY_STATE_UNKNOWN}.
     * This API should be called whenever there is a change in the mobility state.
     * @param state the updated device mobility state
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_SET_DEVICE_MOBILITY_STATE)
    public void setDeviceMobilityState(@DeviceMobilityState int state) {
        try {
            mService.setDeviceMobilityState(state);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* Easy Connect - AKA Device Provisioning Protocol (DPP) */

    /**
     * Easy Connect Network role: Station.
     *
     * @hide
     */
    @SystemApi
    public static final int EASY_CONNECT_NETWORK_ROLE_STA = 0;

    /**
     * Easy Connect Network role: Access Point.
     *
     * @hide
     */
    @SystemApi
    public static final int EASY_CONNECT_NETWORK_ROLE_AP = 1;

    /** @hide */
    @IntDef(prefix = {"EASY_CONNECT_NETWORK_ROLE_"}, value = {
            EASY_CONNECT_NETWORK_ROLE_STA,
            EASY_CONNECT_NETWORK_ROLE_AP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EasyConnectNetworkRole {
    }

    /** Easy Connect Device information maximum allowed length */
    private static final int EASY_CONNECT_DEVICE_INFO_MAXIMUM_LENGTH = 40;

    /**
     * Easy Connect Cryptography Curve name: prime256v1
     *
     * @hide
     */
    @SystemApi
    public static final int EASY_CONNECT_CRYPTOGRAPHY_CURVE_PRIME256V1 = 0;

    /**
     * Easy Connect Cryptography Curve name: secp384r1
     *
     * @hide
     */
    @SystemApi
    public static final int EASY_CONNECT_CRYPTOGRAPHY_CURVE_SECP384R1 = 1;

    /**
     * Easy Connect Cryptography Curve name: secp521r1
     *
     * @hide
     */
    @SystemApi
    public static final int EASY_CONNECT_CRYPTOGRAPHY_CURVE_SECP521R1 = 2;


    /**
     * Easy Connect Cryptography Curve name: brainpoolP256r1
     *
     * @hide
     */
    @SystemApi
    public static final int EASY_CONNECT_CRYPTOGRAPHY_CURVE_BRAINPOOLP256R1 = 3;


    /**
     * Easy Connect Cryptography Curve name: brainpoolP384r1
     *
     * @hide
     */
    @SystemApi
    public static final int EASY_CONNECT_CRYPTOGRAPHY_CURVE_BRAINPOOLP384R1 = 4;


    /**
     * Easy Connect Cryptography Curve name: brainpoolP512r1
     *
     * @hide
     */
    @SystemApi
    public static final int EASY_CONNECT_CRYPTOGRAPHY_CURVE_BRAINPOOLP512R1 = 5;

    /** @hide */
    @IntDef(prefix = {"EASY_CONNECT_CRYPTOGRAPHY_CURVE_"}, value = {
            EASY_CONNECT_CRYPTOGRAPHY_CURVE_PRIME256V1,
            EASY_CONNECT_CRYPTOGRAPHY_CURVE_SECP384R1,
            EASY_CONNECT_CRYPTOGRAPHY_CURVE_SECP521R1,
            EASY_CONNECT_CRYPTOGRAPHY_CURVE_BRAINPOOLP256R1,
            EASY_CONNECT_CRYPTOGRAPHY_CURVE_BRAINPOOLP384R1,
            EASY_CONNECT_CRYPTOGRAPHY_CURVE_BRAINPOOLP512R1,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EasyConnectCryptographyCurve {
    }

    /**
     * Verbose logging mode: DISABLED.
     * @hide
     */
    @SystemApi
    public static final int VERBOSE_LOGGING_LEVEL_DISABLED = 0;

    /**
     * Verbose logging mode: ENABLED.
     * @hide
     */
    @SystemApi
    public static final int VERBOSE_LOGGING_LEVEL_ENABLED = 1;

    /**
     * Verbose logging mode: ENABLED_SHOW_KEY.
     * This mode causes the Wi-Fi password and encryption keys to be output to the logcat.
     * This is security sensitive information useful for debugging.
     * This configuration is enabled for 30 seconds and then falls back to
     * the regular verbose mode (i.e. to {@link VERBOSE_LOGGING_LEVEL_ENABLED}).
     * Show key mode is not persistent, i.e. rebooting the device would fallback to
     * the regular verbose mode.
     * @hide
     */
    @SystemApi
    public static final int VERBOSE_LOGGING_LEVEL_ENABLED_SHOW_KEY = 2;

    /** @hide */
    @IntDef(prefix = {"VERBOSE_LOGGING_LEVEL_"}, value = {
            VERBOSE_LOGGING_LEVEL_DISABLED,
            VERBOSE_LOGGING_LEVEL_ENABLED,
            VERBOSE_LOGGING_LEVEL_ENABLED_SHOW_KEY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface VerboseLoggingLevel {
    }

    /**
     * Start Easy Connect (DPP) in Configurator-Initiator role. The current device will initiate
     * Easy Connect bootstrapping with a peer, and configure the peer with the SSID and password of
     * the specified network using the Easy Connect protocol on an encrypted link.
     *
     * @param enrolleeUri         URI of the Enrollee obtained separately (e.g. QR code scanning)
     * @param selectedNetworkId   Selected network ID to be sent to the peer
     * @param enrolleeNetworkRole The network role of the enrollee
     * @param callback            Callback for status updates
     * @param executor            The Executor on which to run the callback.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    public void startEasyConnectAsConfiguratorInitiator(@NonNull String enrolleeUri,
            int selectedNetworkId, @EasyConnectNetworkRole int enrolleeNetworkRole,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull EasyConnectStatusCallback callback) {
        Binder binder = new Binder();
        try {
            mService.startDppAsConfiguratorInitiator(binder, mContext.getOpPackageName(),
                    enrolleeUri, selectedNetworkId, enrolleeNetworkRole,
                    new EasyConnectCallbackProxy(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Start Easy Connect (DPP) in Enrollee-Initiator role. The current device will initiate Easy
     * Connect bootstrapping with a peer, and receive the SSID and password from the peer
     * configurator.
     *
     * @param configuratorUri URI of the Configurator obtained separately (e.g. QR code scanning)
     * @param callback        Callback for status updates
     * @param executor        The Executor on which to run the callback.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    public void startEasyConnectAsEnrolleeInitiator(@NonNull String configuratorUri,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull EasyConnectStatusCallback callback) {
        Binder binder = new Binder();
        try {
            mService.startDppAsEnrolleeInitiator(binder, configuratorUri,
                    new EasyConnectCallbackProxy(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Start Easy Connect (DPP) in Enrollee-Responder role.
     * The device will:
     * 1. Generate a DPP bootstrap URI and return it using the
     * {@link EasyConnectStatusCallback#onBootstrapUriGenerated(Uri)} method.
     * 2. Start DPP as a Responder, waiting for an Initiator device to start the DPP
     * authentication process.
     * The caller should use the URI provided in step #1, for instance display it as a QR code
     * or communicate it in some other way to the initiator device.
     *
     * @param deviceInfo      Device specific information to add to the DPP URI. This field allows
     *                        the users of the configurators to identify the device.
     *                        Optional - if not provided or in case of an empty string,
     *                        Info field (I:) will be skipped in the generated DPP URI.
     *                        Allowed Range of ASCII characters in deviceInfo - %x20-7E.
     *                        semicolon and space are not allowed. Due to the limitation of maximum
     *                        allowed characters in QR code, framework adds a limit to maximum
     *                        characters in deviceInfo. Users must call
     *                        {@link WifiManager#getEasyConnectMaxAllowedResponderDeviceInfoLength()
     *                        } method to know max allowed length. Violation of these rules will
     *                        result in an exception.
     * @param curve           Elliptic curve cryptography used to generate DPP
     *                        public/private key pair. If application is not interested in a
     *                        specific curve, use specification mandated NIST P-256 elliptic curve,
     *                        {@link WifiManager#EASY_CONNECT_CRYPTOGRAPHY_CURVE_PRIME256V1}.
     * @param callback        Callback for status updates
     * @param executor        The Executor on which to run the callback.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    @RequiresApi(Build.VERSION_CODES.S)
    public void startEasyConnectAsEnrolleeResponder(@Nullable String deviceInfo,
            @EasyConnectCryptographyCurve int curve,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull EasyConnectStatusCallback callback) {
        Binder binder = new Binder();
        try {
            mService.startDppAsEnrolleeResponder(binder, deviceInfo, curve,
                    new EasyConnectCallbackProxy(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Maximum allowed length of Device specific information that can be added to the URI of
     * Easy Connect responder device.
     * @see #startEasyConnectAsEnrolleeResponder(String, int, Executor, EasyConnectStatusCallback)}
     *
     * @hide
     */
    @SystemApi
    public static int getEasyConnectMaxAllowedResponderDeviceInfoLength() {
        return EASY_CONNECT_DEVICE_INFO_MAXIMUM_LENGTH;
    }

    /**
     * Stop or abort a current Easy Connect (DPP) session. This call, once processed, will
     * terminate any ongoing transaction, and clean up all associated resources. Caller should not
     * expect any callbacks once this call is made. However, due to the asynchronous nature of
     * this call, a callback may be fired if it was already pending in the queue.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    public void stopEasyConnectSession() {
        try {
            /* Request lower layers to stop/abort and clear resources */
            mService.stopDppSession();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Helper class to support Easy Connect (DPP) callbacks
     *
     * @hide
     */
    private static class EasyConnectCallbackProxy extends IDppCallback.Stub {
        private final Executor mExecutor;
        private final EasyConnectStatusCallback mEasyConnectStatusCallback;

        EasyConnectCallbackProxy(Executor executor,
                EasyConnectStatusCallback easyConnectStatusCallback) {
            mExecutor = executor;
            mEasyConnectStatusCallback = easyConnectStatusCallback;
        }

        @Override
        public void onSuccessConfigReceived(int newNetworkId) {
            Log.d(TAG, "Easy Connect onSuccessConfigReceived callback");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mEasyConnectStatusCallback.onEnrolleeSuccess(newNetworkId);
            });
        }

        @Override
        public void onSuccess(int status) {
            Log.d(TAG, "Easy Connect onSuccess callback");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mEasyConnectStatusCallback.onConfiguratorSuccess(status);
            });
        }

        @Override
        public void onFailure(int status, String ssid, String channelList,
                int[] operatingClassArray) {
            Log.d(TAG, "Easy Connect onFailure callback");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                SparseArray<int[]> channelListArray = parseDppChannelList(channelList);
                mEasyConnectStatusCallback.onFailure(status, ssid, channelListArray,
                        operatingClassArray);
            });
        }

        @Override
        public void onProgress(int status) {
            Log.d(TAG, "Easy Connect onProgress callback");
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mEasyConnectStatusCallback.onProgress(status);
            });
        }

        @Override
        public void onBootstrapUriGenerated(@NonNull String uri) {
            Log.d(TAG, "Easy Connect onBootstrapUriGenerated callback");
            if (!SdkLevel.isAtLeastS()) {
                Log.e(TAG, "Easy Connect bootstrap URI callback supported only on S+");
                return;
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> {
                mEasyConnectStatusCallback.onBootstrapUriGenerated(Uri.parse(uri));
            });
        }
    }

    /**
     * Interface for Wi-Fi usability statistics listener. Should be implemented by applications and
     * set when calling {@link WifiManager#addOnWifiUsabilityStatsListener(Executor,
     * OnWifiUsabilityStatsListener)}.
     *
     * @hide
     */
    @SystemApi
    public interface OnWifiUsabilityStatsListener {
        /**
         * Called when Wi-Fi usability statistics is updated.
         *
         * @param seqNum The sequence number of statistics, used to derive the timing of updated
         *               Wi-Fi usability statistics, set by framework and incremented by one after
         *               each update.
         * @param isSameBssidAndFreq The flag to indicate whether the BSSID and the frequency of
         *                           network stays the same or not relative to the last update of
         *                           Wi-Fi usability stats.
         * @param stats The updated Wi-Fi usability statistics.
         */
        void onWifiUsabilityStats(int seqNum, boolean isSameBssidAndFreq,
                @NonNull WifiUsabilityStatsEntry stats);
    }

    /**
     * Interface for Wi-Fi verbose logging status listener. Should be implemented by applications
     * and set when calling {@link WifiManager#addWifiVerboseLoggingStatusListener(Executor,
     * WifiVerboseLoggingStatusListener)}.
     *
     * @hide
     */
    @SystemApi
    public interface WifiVerboseLoggingStatusChangedListener {
        /**
         * Called when Wi-Fi verbose logging setting is updated.
         *
         * @param enabled true if verbose logging is enabled, false if verbose logging is disabled.
         */
        void onWifiVerboseLoggingStatusChanged(boolean enabled);
    }

    /**
     * Adds a listener for Wi-Fi usability statistics. See {@link OnWifiUsabilityStatsListener}.
     * Multiple listeners can be added. Callers will be invoked periodically by framework to
     * inform clients about the current Wi-Fi usability statistics. Callers can remove a previously
     * added listener using
     * {@link #removeOnWifiUsabilityStatsListener(OnWifiUsabilityStatsListener)}.
     *
     * @param executor The executor on which callback will be invoked.
     * @param listener Listener for Wifi usability statistics.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_UPDATE_USABILITY_STATS_SCORE)
    public void addOnWifiUsabilityStatsListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull OnWifiUsabilityStatsListener listener) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "addOnWifiUsabilityStatsListener: listener=" + listener);
        }
        try {
            synchronized (sOnWifiUsabilityStatsListenerMap) {
                IOnWifiUsabilityStatsListener.Stub binderCallback =
                        new IOnWifiUsabilityStatsListener.Stub() {
                            @Override
                            public void onWifiUsabilityStats(int seqNum, boolean isSameBssidAndFreq,
                                    WifiUsabilityStatsEntry stats) {
                                if (mVerboseLoggingEnabled) {
                                    Log.v(TAG, "OnWifiUsabilityStatsListener: "
                                            + "onWifiUsabilityStats: seqNum=" + seqNum);
                                }
                                Binder.clearCallingIdentity();
                                executor.execute(() -> listener.onWifiUsabilityStats(
                                        seqNum, isSameBssidAndFreq, stats));
                            }
                        };
                sOnWifiUsabilityStatsListenerMap.put(System.identityHashCode(listener),
                        binderCallback);
                mService.addOnWifiUsabilityStatsListener(binderCallback);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow callers to remove a previously registered listener. After calling this method,
     * applications will no longer receive Wi-Fi usability statistics.
     *
     * @param listener Listener to remove the Wi-Fi usability statistics.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_UPDATE_USABILITY_STATS_SCORE)
    public void removeOnWifiUsabilityStatsListener(@NonNull OnWifiUsabilityStatsListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "removeOnWifiUsabilityStatsListener: listener=" + listener);
        }
        try {
            synchronized (sOnWifiUsabilityStatsListenerMap) {
                int listenerIdentifier = System.identityHashCode(listener);
                if (!sOnWifiUsabilityStatsListenerMap.contains(listenerIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + listenerIdentifier);
                    return;
                }
                mService.removeOnWifiUsabilityStatsListener(
                        sOnWifiUsabilityStatsListenerMap.get(listenerIdentifier));
                sOnWifiUsabilityStatsListenerMap.remove(listenerIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Provide a Wi-Fi usability score information to be recorded (but not acted upon) by the
     * framework. The Wi-Fi usability score is derived from {@link OnWifiUsabilityStatsListener}
     * where a score is matched to Wi-Fi usability statistics using the sequence number. The score
     * is used to quantify whether Wi-Fi is usable in a future time.
     *
     * @param seqNum Sequence number of the Wi-Fi usability score.
     * @param score The Wi-Fi usability score, expected range: [0, 100].
     * @param predictionHorizonSec Prediction horizon of the Wi-Fi usability score in second,
     *                             expected range: [0, 30].
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_UPDATE_USABILITY_STATS_SCORE)
    public void updateWifiUsabilityScore(int seqNum, int score, int predictionHorizonSec) {
        try {
            mService.updateWifiUsabilityScore(seqNum, score, predictionHorizonSec);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Abstract class for scan results callback. Should be extended by applications and set when
     * calling {@link WifiManager#registerScanResultsCallback(Executor, ScanResultsCallback)}.
     */
    public abstract static class ScanResultsCallback {
        private final ScanResultsCallbackProxy mScanResultsCallbackProxy;

        public ScanResultsCallback() {
            mScanResultsCallbackProxy = new ScanResultsCallbackProxy();
        }

        /**
         * Called when new scan results are available.
         * Clients should use {@link WifiManager#getScanResults()} to get the scan results.
         */
        public abstract void onScanResultsAvailable();

        /*package*/ @NonNull ScanResultsCallbackProxy getProxy() {
            return mScanResultsCallbackProxy;
        }

        private static class ScanResultsCallbackProxy extends IScanResultsCallback.Stub {
            private final Object mLock = new Object();
            @Nullable @GuardedBy("mLock") private Executor mExecutor;
            @Nullable @GuardedBy("mLock") private ScanResultsCallback mCallback;

            ScanResultsCallbackProxy() {
                mCallback = null;
                mExecutor = null;
            }

            /*package*/ void initProxy(@NonNull Executor executor,
                    @NonNull ScanResultsCallback callback) {
                synchronized (mLock) {
                    mExecutor = executor;
                    mCallback = callback;
                }
            }

            /*package*/ void cleanUpProxy() {
                synchronized (mLock) {
                    mExecutor = null;
                    mCallback = null;
                }
            }

            @Override
            public void onScanResultsAvailable() {
                ScanResultsCallback callback;
                Executor executor;
                synchronized (mLock) {
                    executor = mExecutor;
                    callback = mCallback;
                }
                if (callback == null || executor == null) {
                    return;
                }
                Binder.clearCallingIdentity();
                executor.execute(callback::onScanResultsAvailable);
            }
        }
    }

    /**
     * Register a callback for Scan Results. See {@link ScanResultsCallback}.
     * Caller will receive the event when scan results are available.
     * Caller should use {@link WifiManager#getScanResults()} requires
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} to get the scan results.
     * Caller can remove a previously registered callback using
     * {@link WifiManager#unregisterScanResultsCallback(ScanResultsCallback)}
     * Same caller can add multiple listeners.
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE} permission. Callers
     * without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     *
     * @param executor The executor to execute the callback of the {@code callback} object.
     * @param callback callback for Scan Results events
     */

    @RequiresPermission(ACCESS_WIFI_STATE)
    public void registerScanResultsCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull ScanResultsCallback callback) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");

        Log.v(TAG, "registerScanResultsCallback: callback=" + callback
                + ", executor=" + executor);
        ScanResultsCallback.ScanResultsCallbackProxy proxy = callback.getProxy();
        proxy.initProxy(executor, callback);
        try {
            mService.registerScanResultsCallback(proxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow callers to unregister a previously registered callback. After calling this method,
     * applications will no longer receive Scan Results events.
     *
     * @param callback callback to unregister for Scan Results events
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void unregisterScanResultsCallback(@NonNull ScanResultsCallback callback) {
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        Log.v(TAG, "unregisterScanResultsCallback: Callback=" + callback);
        ScanResultsCallback.ScanResultsCallbackProxy proxy = callback.getProxy();
        try {
            mService.unregisterScanResultsCallback(proxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } finally {
            proxy.cleanUpProxy();
        }
    }

    /**
     * Interface for suggestion connection status listener.
     * Should be implemented by applications and set when calling
     * {@link WifiManager#addSuggestionConnectionStatusListener(
     * Executor, SuggestionConnectionStatusListener)}.
     */
    public interface SuggestionConnectionStatusListener {

        /**
         * Called when the framework attempted to connect to a suggestion provided by the
         * registering app, but the connection to the suggestion failed.
         * @param wifiNetworkSuggestion The suggestion which failed to connect.
         * @param failureReason the connection failure reason code.
         */
        void onConnectionStatus(
                @NonNull WifiNetworkSuggestion wifiNetworkSuggestion,
                @SuggestionConnectionStatusCode int failureReason);
    }

    private class SuggestionConnectionStatusListenerProxy extends
            ISuggestionConnectionStatusListener.Stub {
        private final Executor mExecutor;
        private final SuggestionConnectionStatusListener mListener;

        SuggestionConnectionStatusListenerProxy(@NonNull Executor executor,
                @NonNull SuggestionConnectionStatusListener listener) {
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public void onConnectionStatus(@NonNull WifiNetworkSuggestion wifiNetworkSuggestion,
                int failureReason) {
            Binder.clearCallingIdentity();
            mExecutor.execute(() ->
                    mListener.onConnectionStatus(wifiNetworkSuggestion, failureReason));
        }

    }

    /**
     * Interface for local-only connection failure listener.
     * Should be implemented by applications and set when calling
     * {@link WifiManager#addLocalOnlyConnectionFailureListener(Executor, LocalOnlyConnectionFailureListener)}
     */
    public interface LocalOnlyConnectionFailureListener {

        /**
         * Called when the framework attempted to connect to a local-only network requested by the
         * registering app, but the connection to the network failed.
         * @param wifiNetworkSpecifier The {@link WifiNetworkSpecifier} which failed to connect.
         * @param failureReason the connection failure reason code.
         */
        void onConnectionFailed(
                @NonNull WifiNetworkSpecifier wifiNetworkSpecifier,
                @LocalOnlyConnectionStatusCode int failureReason);
    }

    private static class LocalOnlyConnectionStatusListenerProxy extends
            ILocalOnlyConnectionStatusListener.Stub {
        private final Executor mExecutor;
        private final LocalOnlyConnectionFailureListener mListener;

        LocalOnlyConnectionStatusListenerProxy(@NonNull Executor executor,
                @NonNull LocalOnlyConnectionFailureListener listener) {
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public void onConnectionStatus(@NonNull WifiNetworkSpecifier networkSpecifier,
                int failureReason) {
            Binder.clearCallingIdentity();
            mExecutor.execute(() ->
                    mListener.onConnectionFailed(networkSpecifier, failureReason));
        }

    }

    /**
     * Add a listener listening to wifi verbose logging changes.
     * See {@link WifiVerboseLoggingStatusChangedListener}.
     * Caller can remove a previously registered listener using
     * {@link WifiManager#removeWifiVerboseLoggingStatusChangedListener(
     * WifiVerboseLoggingStatusChangedListener)}
     * Same caller can add multiple listeners to monitor the event.
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE}.
     * Callers without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     * @param executor The executor to execute the listener of the {@code listener} object.
     * @param listener listener for changes in wifi verbose logging.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void addWifiVerboseLoggingStatusChangedListener(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull WifiVerboseLoggingStatusChangedListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        if (executor == null) throw new IllegalArgumentException("Executor cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "addWifiVerboseLoggingStatusChangedListener listener=" + listener
                    + ", executor=" + executor);
        }
        try {
            synchronized (sWifiVerboseLoggingStatusChangedListenerMap) {
                IWifiVerboseLoggingStatusChangedListener.Stub binderCallback =
                        new IWifiVerboseLoggingStatusChangedListener.Stub() {
                            @Override
                            public void onStatusChanged(boolean enabled) {
                                if (mVerboseLoggingEnabled) {
                                    Log.v(TAG, "WifiVerboseLoggingStatusListener: "
                                            + "onVerboseLoggingStatusChanged: enabled=" + enabled);
                                }
                                Binder.clearCallingIdentity();
                                executor.execute(() -> listener.onWifiVerboseLoggingStatusChanged(
                                        enabled));
                            }
                        };
                sWifiVerboseLoggingStatusChangedListenerMap.put(System.identityHashCode(listener),
                        binderCallback);
                mService.addWifiVerboseLoggingStatusChangedListener(binderCallback);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow callers to remove a previously registered listener.
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE}.
     * Callers without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     * @param listener listener to remove.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void removeWifiVerboseLoggingStatusChangedListener(
            @NonNull WifiVerboseLoggingStatusChangedListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        Log.v(TAG, "removeWifiVerboseLoggingStatusChangedListener: listener=" + listener);
        try {
            synchronized (sWifiVerboseLoggingStatusChangedListenerMap) {
                int listenerIdentifier = System.identityHashCode(listener);
                if (!sWifiVerboseLoggingStatusChangedListenerMap.contains(listenerIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + listenerIdentifier);
                    return;
                }
                mService.removeWifiVerboseLoggingStatusChangedListener(
                        sWifiVerboseLoggingStatusChangedListenerMap.get(listenerIdentifier));
                sWifiVerboseLoggingStatusChangedListenerMap.remove(listenerIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Add a listener for suggestion networks. See {@link SuggestionConnectionStatusListener}.
     * Caller will receive the event when suggested network have connection failure.
     * Caller can remove a previously registered listener using
     * {@link WifiManager#removeSuggestionConnectionStatusListener(
     * SuggestionConnectionStatusListener)}
     * Same caller can add multiple listeners to monitor the event.
     * <p>
     * Applications should have the
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} and
     * {@link android.Manifest.permission#ACCESS_WIFI_STATE} permissions.
     * Callers without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     *
     * @param executor The executor to execute the listener of the {@code listener} object.
     * @param listener listener for suggestion network connection failure.
     */
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    public void addSuggestionConnectionStatusListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull SuggestionConnectionStatusListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        if (executor == null) throw new IllegalArgumentException("Executor cannot be null");
        Log.v(TAG, "addSuggestionConnectionStatusListener listener=" + listener
                + ", executor=" + executor);
        try {
            synchronized (sSuggestionConnectionStatusListenerMap) {
                ISuggestionConnectionStatusListener.Stub binderCallback =
                        new SuggestionConnectionStatusListenerProxy(executor, listener);
                sSuggestionConnectionStatusListenerMap.put(System.identityHashCode(listener),
                        binderCallback);
                mService.registerSuggestionConnectionStatusListener(binderCallback,
                        mContext.getOpPackageName(), mContext.getAttributionTag());
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

    }

    /**
     * Allow callers to remove a previously registered listener. After calling this method,
     * applications will no longer receive suggestion connection events through that listener.
     *
     * @param listener listener to remove.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void removeSuggestionConnectionStatusListener(
            @NonNull SuggestionConnectionStatusListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        Log.v(TAG, "removeSuggestionConnectionStatusListener: listener=" + listener);
        try {
            synchronized (sSuggestionConnectionStatusListenerMap) {
                int listenerIdentifier = System.identityHashCode(listener);
                if (!sSuggestionConnectionStatusListenerMap.contains(listenerIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + listenerIdentifier);
                    return;
                }
                mService.unregisterSuggestionConnectionStatusListener(
                        sSuggestionConnectionStatusListenerMap.get(listenerIdentifier),
                        mContext.getOpPackageName());
                sSuggestionConnectionStatusListenerMap.remove(listenerIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Add a listener for local-only networks. See {@link WifiNetworkSpecifier}.
     * Specify the caller will only get connection failures for networks they requested.
     * Caller can remove a previously registered listener using
     * {@link WifiManager#removeLocalOnlyConnectionFailureListener(LocalOnlyConnectionFailureListener)}
     * Same caller can add multiple listeners to monitor the event.
     * <p>
     * Applications should have the {@link android.Manifest.permission#ACCESS_WIFI_STATE}
     * permissions.
     * Callers without the permission will trigger a {@link java.lang.SecurityException}.
     * <p>
     *
     * @param executor The executor to execute the listener of the {@code listener} object.
     * @param listener listener for local-only network connection failure.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void addLocalOnlyConnectionFailureListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull LocalOnlyConnectionFailureListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        if (executor == null) throw new IllegalArgumentException("Executor cannot be null");
        try {
            synchronized (sLocalOnlyConnectionStatusListenerMap) {
                if (sLocalOnlyConnectionStatusListenerMap
                        .contains(System.identityHashCode(listener))) {
                    Log.w(TAG, "Same listener already registered");
                    return;
                }
                ILocalOnlyConnectionStatusListener.Stub binderCallback =
                        new LocalOnlyConnectionStatusListenerProxy(executor, listener);
                sLocalOnlyConnectionStatusListenerMap.put(System.identityHashCode(listener),
                        binderCallback);
                mService.addLocalOnlyConnectionStatusListener(binderCallback,
                        mContext.getOpPackageName(), mContext.getAttributionTag());
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow callers to remove a previously registered listener. After calling this method,
     * applications will no longer receive local-only connection events through that listener.
     *
     * @param listener listener to remove.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void removeLocalOnlyConnectionFailureListener(
            @NonNull LocalOnlyConnectionFailureListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        try {
            synchronized (sLocalOnlyConnectionStatusListenerMap) {
                int listenerIdentifier = System.identityHashCode(listener);
                if (!sLocalOnlyConnectionStatusListenerMap.contains(listenerIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + listenerIdentifier);
                    return;
                }
                mService.removeLocalOnlyConnectionStatusListener(
                        sLocalOnlyConnectionStatusListenerMap.get(listenerIdentifier),
                        mContext.getOpPackageName());
                sLocalOnlyConnectionStatusListenerMap.remove(listenerIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Parse the list of channels the DPP enrollee reports when it fails to find an AP.
     *
     * @param channelList List of channels in the format defined in the DPP specification.
     * @return A parsed sparse array, where the operating class is the key.
     * @hide
     */
    @VisibleForTesting
    public static SparseArray<int[]> parseDppChannelList(String channelList) {
        SparseArray<int[]> channelListArray = new SparseArray<>();

        if (TextUtils.isEmpty(channelList)) {
            return channelListArray;
        }
        StringTokenizer str = new StringTokenizer(channelList, ",");
        String classStr = null;
        List<Integer> channelsInClass = new ArrayList<>();

        try {
            while (str.hasMoreElements()) {
                String cur = str.nextToken();

                /**
                 * Example for a channel list:
                 *
                 * 81/1,2,3,4,5,6,7,8,9,10,11,115/36,40,44,48,118/52,56,60,64,121/100,104,108,112,
                 * 116,120,124,128,132,136,140,0/144,124/149,153,157,161,125/165
                 *
                 * Detect operating class by the delimiter of '/' and use a string tokenizer with
                 * ',' as a delimiter.
                 */
                int classDelim = cur.indexOf('/');
                if (classDelim != -1) {
                    if (classStr != null) {
                        // Store the last channel array in the sparse array, where the operating
                        // class is the key (as an integer).
                        int[] channelsArray = new int[channelsInClass.size()];
                        for (int i = 0; i < channelsInClass.size(); i++) {
                            channelsArray[i] = channelsInClass.get(i);
                        }
                        channelListArray.append(Integer.parseInt(classStr), channelsArray);
                        channelsInClass = new ArrayList<>();
                    }

                    // Init a new operating class and store the first channel
                    classStr = cur.substring(0, classDelim);
                    String channelStr = cur.substring(classDelim + 1);
                    channelsInClass.add(Integer.parseInt(channelStr));
                } else {
                    if (classStr == null) {
                        // Invalid format
                        Log.e(TAG, "Cannot parse DPP channel list");
                        return new SparseArray<>();
                    }
                    channelsInClass.add(Integer.parseInt(cur));
                }
            }

            // Store the last array
            if (classStr != null) {
                int[] channelsArray = new int[channelsInClass.size()];
                for (int i = 0; i < channelsInClass.size(); i++) {
                    channelsArray[i] = channelsInClass.get(i);
                }
                channelListArray.append(Integer.parseInt(classStr), channelsArray);
            }
            return channelListArray;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Cannot parse DPP channel list");
            return new SparseArray<>();
        }
    }

    /**
     * Callback interface for framework to receive network status updates and trigger of updating
     * {@link WifiUsabilityStatsEntry}.
     *
     * @hide
     */
    @SystemApi
    public interface ScoreUpdateObserver {
        /**
         * Called by applications to indicate network status. For applications targeting
         * {@link android.os.Build.VERSION_CODES#S} or above: The score is not used to take action
         * on network selection but for the purpose of Wifi metric collection only; Network
         * selection is influenced by inputs from
         * {@link ScoreUpdateObserver#notifyStatusUpdate(int, boolean)},
         * {@link ScoreUpdateObserver#requestNudOperation(int, boolean)}, and
         * {@link ScoreUpdateObserver#blocklistCurrentBssid(int)}.
         *
         * @param sessionId The ID to indicate current Wi-Fi network connection obtained from
         *                  {@link WifiConnectedNetworkScorer#onStart(int)}.
         * @param score The score representing link quality of current Wi-Fi network connection.
         *              Populated by connected network scorer in applications..
         */
        void notifyScoreUpdate(int sessionId, int score);

        /**
         * Called by applications to trigger an update of {@link WifiUsabilityStatsEntry}.
         * To receive update applications need to add WifiUsabilityStatsEntry listener. See
         * {@link addOnWifiUsabilityStatsListener(Executor, OnWifiUsabilityStatsListener)}.
         *
         * @param sessionId The ID to indicate current Wi-Fi network connection obtained from
         *                  {@link WifiConnectedNetworkScorer#onStart(int)}.
         */
        void triggerUpdateOfWifiUsabilityStats(int sessionId);

        /**
         * Called by applications to indicate whether current Wi-Fi network is usable or not.
         *
         * @param sessionId The ID to indicate current Wi-Fi network connection obtained from
         *                  {@link WifiConnectedNetworkScorer#onStart(int)}.
         * @param isUsable The boolean representing whether current Wi-Fi network is usable, and it
         *                 may be sent to ConnectivityService and used for setting default network.
         *                 Populated by connected network scorer in applications.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        default void notifyStatusUpdate(int sessionId, boolean isUsable) {}

        /**
         * Called by applications to start a NUD (Neighbor Unreachability Detection) operation. The
         * framework throttles NUD operations to no more frequently than every five seconds
         * (see {@link WifiScoreReport#NUD_THROTTLE_MILLIS}). The framework keeps track of requests
         * and executes them as soon as possible based on the throttling criteria.
         *
         * @param sessionId The ID to indicate current Wi-Fi network connection obtained from
         *                  {@link WifiConnectedNetworkScorer#onStart(int)}.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        default void requestNudOperation(int sessionId) {}

        /**
         * Called by applications to blocklist currently connected BSSID. No blocklisting operation
         * if called after disconnection.
         *
         * @param sessionId The ID to indicate current Wi-Fi network connection obtained from
         *                  {@link WifiConnectedNetworkScorer#onStart(int)}.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        default void blocklistCurrentBssid(int sessionId) {}
    }

    /**
     * Callback proxy for {@link ScoreUpdateObserver} objects.
     *
     * @hide
     */
    private class ScoreUpdateObserverProxy implements ScoreUpdateObserver {
        private final IScoreUpdateObserver mScoreUpdateObserver;

        private ScoreUpdateObserverProxy(IScoreUpdateObserver observer) {
            mScoreUpdateObserver = observer;
        }

        @Override
        public void notifyScoreUpdate(int sessionId, int score) {
            try {
                mScoreUpdateObserver.notifyScoreUpdate(sessionId, score);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override
        public void triggerUpdateOfWifiUsabilityStats(int sessionId) {
            try {
                mScoreUpdateObserver.triggerUpdateOfWifiUsabilityStats(sessionId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override
        public void notifyStatusUpdate(int sessionId, boolean isUsable) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            try {
                mScoreUpdateObserver.notifyStatusUpdate(sessionId, isUsable);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override
        public void requestNudOperation(int sessionId) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            try {
                mScoreUpdateObserver.requestNudOperation(sessionId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override
        public void blocklistCurrentBssid(int sessionId) {
            if (!SdkLevel.isAtLeastS()) {
                throw new UnsupportedOperationException();
            }
            try {
                mScoreUpdateObserver.blocklistCurrentBssid(sessionId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Interface for Wi-Fi connected network scorer. Should be implemented by applications and set
     * when calling
     * {@link WifiManager#setWifiConnectedNetworkScorer(Executor, WifiConnectedNetworkScorer)}.
     *
     * @hide
     */
    @SystemApi
    public interface WifiConnectedNetworkScorer {
        /**
         * Called by framework to indicate the start of a network connection.
         * @param sessionId The ID to indicate current Wi-Fi network connection.
         * @deprecated This API is deprecated.  Please use
         *             {@link WifiConnectedNetworkScorer#onStart(WifiConnectedSessionInfo)}.
         */
        @Deprecated
        default void onStart(int sessionId) {}

        /**
         * Called by framework to indicate the start of a network connection.
         * @param sessionInfo The session information to indicate current Wi-Fi network connection.
         *                    See {@link WifiConnectedSessionInfo}.
         */
        default void onStart(@NonNull WifiConnectedSessionInfo sessionInfo) {
            onStart(sessionInfo.getSessionId());
        }

        /**
         * Called by framework to indicate the end of a network connection.
         * @param sessionId The ID to indicate current Wi-Fi network connection obtained from
         *                  {@link WifiConnectedNetworkScorer#onStart(int)}.
         */
        void onStop(int sessionId);

        /**
         * Framework sets callback for score change events after application sets its scorer.
         * @param observerImpl The instance for {@link WifiManager#ScoreUpdateObserver}. Should be
         * implemented and instantiated by framework.
         */
        void onSetScoreUpdateObserver(@NonNull ScoreUpdateObserver observerImpl);

        /**
         * Called by framework to indicate the user accepted a dialog to switch to a new network.
         * @param sessionId The ID to indicate current Wi-Fi network connection obtained from
         *                  {@link WifiConnectedNetworkScorer#onStart(int)}.
         * @param targetNetworkId Network ID of the target network.
         * @param targetBssid BSSID of the target network.
         */
        default void onNetworkSwitchAccepted(
                int sessionId, int targetNetworkId, @NonNull String targetBssid) {
            // No-op.
        }

        /**
         * Called by framework to indicate the user rejected a dialog to switch to new network.
         * @param sessionId The ID to indicate current Wi-Fi network connection obtained from
         *                  {@link WifiConnectedNetworkScorer#onStart(int)}.
         * @param targetNetworkId Network ID of the target network.
         * @param targetBssid BSSID of the target network.
         */
        default void onNetworkSwitchRejected(
                int sessionId, int targetNetworkId, @NonNull String targetBssid) {
            // No-op.
        }
    }


    /**
     * Callback registered with {@link WifiManager#setExternalPnoScanRequest(List, int[], Executor,
     * PnoScanResultsCallback)}. Returns status and result information on offloaded external PNO
     * requests.
     * @hide
     */
    @SystemApi
    public interface PnoScanResultsCallback {
        /**
         * A status code returned by {@link #onRegisterFailed(int)}.
         * Unknown failure.
         */
        int REGISTER_PNO_CALLBACK_UNKNOWN = 0;

        /**
         * A status code returned by {@link #onRegisterFailed(int)}.
         * A callback has already been registered by the caller.
         */
        int REGISTER_PNO_CALLBACK_ALREADY_REGISTERED = 1;

        /**
         * A status code returned by {@link #onRegisterFailed(int)}.
         * The platform is unable to serve this request because another app has a PNO scan request
         * active.
         */
        int REGISTER_PNO_CALLBACK_RESOURCE_BUSY = 2;

        /**
         * A status code returned by {@link #onRegisterFailed(int)}.
         * PNO scans are not supported on this device.
         */
        int REGISTER_PNO_CALLBACK_PNO_NOT_SUPPORTED = 3;

        /** @hide */
        @IntDef(prefix = { "REGISTER_PNO_CALLBACK_" }, value = {
                REGISTER_PNO_CALLBACK_UNKNOWN,
                REGISTER_PNO_CALLBACK_ALREADY_REGISTERED,
                REGISTER_PNO_CALLBACK_RESOURCE_BUSY,
                REGISTER_PNO_CALLBACK_PNO_NOT_SUPPORTED
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface RegisterFailureReason {}

        /**
         * A status code returned by {@link #onRemoved(int)}.
         * Unknown reason.
         */
        int REMOVE_PNO_CALLBACK_UNKNOWN = 0;

        /**
         * A status code returned by {@link #onRemoved(int)}.
         * This Callback is automatically removed after results ScanResults are delivered.
         */
        int REMOVE_PNO_CALLBACK_RESULTS_DELIVERED = 1;

        /**
         * A status code returned by {@link #onRemoved(int)}.
         * This callback has been unregistered via {@link WifiManager#clearExternalPnoScanRequest()}
         */
        int REMOVE_PNO_CALLBACK_UNREGISTERED = 2;

        /** @hide */
        @IntDef(prefix = { "REMOVE_PNO_CALLBACK_" }, value = {
                REMOVE_PNO_CALLBACK_UNKNOWN,
                REMOVE_PNO_CALLBACK_RESULTS_DELIVERED,
                REMOVE_PNO_CALLBACK_UNREGISTERED
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface RemovalReason {}

        /**
         * Called when PNO scan finds one of the requested SSIDs. This is a one time callback.
         * After results are reported the callback will be automatically unregistered.
         */
        void onScanResultsAvailable(@NonNull List<ScanResult> scanResults);

        /**
         * Called when this callback has been successfully registered.
         */
        void onRegisterSuccess();

        /**
         * Called when this callback failed to register with the failure reason.
         * See {@link RegisterFailureReason} for details.
         */
        void onRegisterFailed(@RegisterFailureReason int reason);

        /**
         * Called when this callback has been unregistered from the Wi-Fi subsystem.
         * See {@link RemovalReason} for details.
         */
        void onRemoved(@RemovalReason int reason);
    }


    private class PnoScanResultsCallbackProxy extends IPnoScanResultsCallback.Stub {
        private Executor mExecutor;
        private PnoScanResultsCallback mCallback;

        PnoScanResultsCallbackProxy(@NonNull Executor executor,
                @NonNull PnoScanResultsCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onScanResultsAvailable(List<ScanResult> scanResults) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "PnoScanResultsCallback: " + "onScanResultsAvailable");
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onScanResultsAvailable(scanResults));
        }

        @Override
        public void onRegisterSuccess() {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "PnoScanResultsCallback: " + "onRegisterSuccess");
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onRegisterSuccess());
        }

        @Override
        public void onRegisterFailed(int reason) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "PnoScanResultsCallback: " + "onRegisterFailed " + reason);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onRegisterFailed(reason));
        }

        @Override
        public void onRemoved(int reason) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "PnoScanResultsCallback: " + "onRemoved");
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onRemoved(reason));
        }
    }

    /**
     * Callback proxy for {@link WifiConnectedNetworkScorer} objects.
     *
     * @hide
     */
    private class WifiConnectedNetworkScorerProxy extends IWifiConnectedNetworkScorer.Stub {
        private Executor mExecutor;
        private WifiConnectedNetworkScorer mScorer;

        WifiConnectedNetworkScorerProxy(Executor executor, WifiConnectedNetworkScorer scorer) {
            mExecutor = executor;
            mScorer = scorer;
        }

        @Override
        public void onStart(@NonNull WifiConnectedSessionInfo sessionInfo) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "WifiConnectedNetworkScorer: " + "onStart: sessionInfo=" + sessionInfo);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mScorer.onStart(sessionInfo));
        }

        @Override
        public void onStop(int sessionId) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "WifiConnectedNetworkScorer: " + "onStop: sessionId=" + sessionId);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mScorer.onStop(sessionId));
        }

        @Override
        public void onSetScoreUpdateObserver(IScoreUpdateObserver observerImpl) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "WifiConnectedNetworkScorer: "
                        + "onSetScoreUpdateObserver: observerImpl=" + observerImpl);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mScorer.onSetScoreUpdateObserver(
                    new ScoreUpdateObserverProxy(observerImpl)));
        }

        @Override
        public void onNetworkSwitchAccepted(
                int sessionId, int targetNetworkId, @NonNull String targetBssid) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "WifiConnectedNetworkScorer: onNetworkSwitchAccepted:"
                        + " sessionId=" + sessionId
                        + " targetNetworkId=" + targetNetworkId
                        + " targetBssid=" + targetBssid);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mScorer.onNetworkSwitchAccepted(
                    sessionId, targetNetworkId, targetBssid));
        }
        @Override
        public void onNetworkSwitchRejected(
                int sessionId, int targetNetworkId, @NonNull String targetBssid) {
            if (mVerboseLoggingEnabled) {
                Log.v(TAG, "WifiConnectedNetworkScorer: onNetworkSwitchRejected:"
                                + " sessionId=" + sessionId
                                + " targetNetworkId=" + targetNetworkId
                                + " targetBssid=" + targetBssid);
            }
            Binder.clearCallingIdentity();
            mExecutor.execute(() -> mScorer.onNetworkSwitchRejected(
                    sessionId, targetNetworkId, targetBssid));
        }
    }

    /**
     * This API allows the caller to program up to 2 SSIDs for PNO scans. PNO scans are offloaded
     * to the Wi-Fi chip when the device is inactive (typically screen-off).
     * If the screen is currently off when this API is called, then a PNO scan including the
     * requested SSIDs will immediately get started. If the screen is on when this API is called,
     * the requested SSIDs will get included for PNO scans the next time the screen turns off.
     * <p>
     * Note, due to PNO being a limited resource, only one external PNO request is supported, and
     * calling this API will fail if an external PNO scan request is already registered by another
     * caller. If the caller that has already registered a callback calls this API again, the new
     * callback will override the previous one.
     * <p>
     * After this API is called, {@link PnoScanResultsCallback#onRegisterSuccess()} will be invoked
     * if the operation is successful, or {@link PnoScanResultsCallback#onRegisterFailed(int)} will
     * be invoked if the operation failed.
     * <p>
     * {@link PnoScanResultsCallback#onRemoved(int)} will be invoked to notify the caller when the
     * external PNO scan request is removed, which will happen when one of the following events
     * happen:
     * </p>
     * <ul>
     * <li>Upon finding any of the requested SSIDs through either a connectivity scan or PNO scan,
     * the matching ScanResults will be returned
     * via {@link PnoScanResultsCallback#onScanResultsAvailable(List)}, and the registered PNO
     * scan request will get automatically removed.</li>
     * <li>The external PNO scan request is removed by a call to
     * {@link #clearExternalPnoScanRequest()}</li>
     * </ul>
     *
     * @param ssids The list of SSIDs to request for PNO scan.
     * @param frequencies Provide as hint a list of up to 10 frequencies to be used for PNO scan.
     *                    Each frequency should be in MHz. For example 2412 and 5180 are valid
     *                    frequencies. {@link WifiInfo#getFrequency()} is a location where this
     *                    information could be obtained. If a null or empty array is provided, the
     *                    Wi-Fi framework will automatically decide the list of frequencies to scan.
     * @param executor The executor on which callback will be invoked.
     * @param callback For the calling application to receive results and status updates.
     *
     * @throws SecurityException if the caller does not have permission.
     * @throws IllegalArgumentException if the caller provided invalid inputs.
     * @throws UnsupportedOperationException if this API is not supported on this SDK version.
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION,
            REQUEST_COMPANION_PROFILE_AUTOMOTIVE_PROJECTION})
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public void setExternalPnoScanRequest(@NonNull List<WifiSsid> ssids,
            @Nullable int[] frequencies, @NonNull @CallbackExecutor Executor executor,
            @NonNull PnoScanResultsCallback callback) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
        try {
            mService.setExternalPnoScanRequest(new Binder(),
                    new PnoScanResultsCallbackProxy(executor, callback),
                    ssids, frequencies == null ? new int[0] : frequencies,
                    mContext.getOpPackageName(), mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Clear the current PNO scan request that's been set by the calling UID. Note, the call will
     * be no-op if the current PNO scan request is set by a different UID.
     *
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public void clearExternalPnoScanRequest() {
        try {
            mService.clearExternalPnoScanRequest();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns information about the last caller of an API.
     *
     * @param apiType The type of API to request information for the last caller.
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return 2 arguments.
     *                        {@code String} the name of the package that performed the last API
     *                        call. {@code Boolean} the value associated with the last API call.
     *
     * @throws SecurityException if the caller does not have permission.
     * @throws IllegalArgumentException if the caller provided invalid inputs.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK})
    public void getLastCallerInfoForApi(@ApiType int apiType,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BiConsumer<String, Boolean> resultsCallback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor can't be null");
        }
        if (resultsCallback == null) {
            throw new IllegalArgumentException("resultsCallback can't be null");
        }
        try {
            mService.getLastCallerInfoForApi(apiType,
                    new ILastCallerListener.Stub() {
                        @Override
                        public void onResult(String packageName, boolean enabled) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(packageName, enabled);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set a callback for Wi-Fi connected network scorer.  See {@link WifiConnectedNetworkScorer}.
     * Only a single scorer can be set. Caller will be invoked periodically by framework to inform
     * client about start and stop of Wi-Fi connection. Caller can clear a previously set scorer
     * using {@link clearWifiConnectedNetworkScorer()}.
     *
     * @param executor The executor on which callback will be invoked.
     * @param scorer Scorer for Wi-Fi network implemented by application.
     * @return true Scorer is set successfully.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_UPDATE_USABILITY_STATS_SCORE)
    public boolean setWifiConnectedNetworkScorer(@NonNull @CallbackExecutor Executor executor,
            @NonNull WifiConnectedNetworkScorer scorer) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        if (scorer == null) throw new IllegalArgumentException("scorer cannot be null");
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "setWifiConnectedNetworkScorer: scorer=" + scorer);
        }
        try {
            return mService.setWifiConnectedNetworkScorer(new Binder(),
                    new WifiConnectedNetworkScorerProxy(executor, scorer));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allow caller to clear a previously set scorer. After calling this method,
     * client will no longer receive information about start and stop of Wi-Fi connection.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.WIFI_UPDATE_USABILITY_STATS_SCORE)
    public void clearWifiConnectedNetworkScorer() {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "clearWifiConnectedNetworkScorer");
        }
        try {
            mService.clearWifiConnectedNetworkScorer();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable/disable wifi scan throttling from 3rd party apps.
     *
     * <p>
     * The throttling limits for apps are described in
     * <a href="Wi-Fi Scan Throttling">
     * https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-throttling</a>
     * </p>
     *
     * @param enable true to allow scan throttling, false to disallow scan throttling.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void setScanThrottleEnabled(boolean enable) {
        try {
            mService.setScanThrottleEnabled(enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the persisted Wi-Fi scan throttle state. Defaults to true, unless changed by the user via
     * Developer options.
     *
     * <p>
     * The throttling limits for apps are described in
     * <a href="Wi-Fi Scan Throttling">
     * https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-throttling</a>
     * </p>
     *
     * @return true to indicate that scan throttling is enabled, false to indicate that scan
     * throttling is disabled.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public boolean isScanThrottleEnabled() {
        try {
            return mService.isScanThrottleEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable/disable wifi auto wakeup feature.
     *
     * <p>
     * The feature is described in
     * <a href="Wi-Fi Turn on automatically">
     * https://source.android.com/devices/tech/connect/wifi-infrastructure
     * #turn_on_wi-fi_automatically
     * </a>
     *
     * @param enable true to enable, false to disable.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void setAutoWakeupEnabled(boolean enable) {
        try {
            mService.setAutoWakeupEnabled(enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the persisted Wi-Fi auto wakeup feature state. Defaults to false, unless changed by the
     * user via Settings.
     *
     * <p>
     * The feature is described in
     * <a href="Wi-Fi Turn on automatically">
     * https://source.android.com/devices/tech/connect/wifi-infrastructure
     * #turn_on_wi-fi_automatically
     * </a>
     *
     * @return true to indicate that wakeup feature is enabled, false to indicate that wakeup
     * feature is disabled.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public boolean isAutoWakeupEnabled() {
        try {
            return mService.isAutoWakeupEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the state of carrier offload on merged or unmerged networks for specified subscription.
     *
     * <p>
     * When a subscription's carrier network offload is disabled, all network suggestions related to
     * this subscription will not be considered for auto join.
     * <p>
     * If calling app want disable all carrier network offload from a specified subscription, should
     * call this API twice to disable both merged and unmerged carrier network suggestions.
     *
     * @param subscriptionId See {@link SubscriptionInfo#getSubscriptionId()}.
     * @param merged True for carrier merged network, false otherwise.
     *               See {@link WifiNetworkSuggestion.Builder#setCarrierMerged(boolean)}
     * @param enabled True for enable carrier network offload, false otherwise.
     * @see #isCarrierNetworkOffloadEnabled(int, boolean)
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD})
    public void setCarrierNetworkOffloadEnabled(int subscriptionId, boolean merged,
            boolean enabled) {
        try {
            mService.setCarrierNetworkOffloadEnabled(subscriptionId, merged, enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the carrier network offload state for merged or unmerged networks for specified
     * subscription.
     * @param subscriptionId subscription ID see {@link SubscriptionInfo#getSubscriptionId()}
     * @param merged True for carrier merged network, false otherwise.
     *               See {@link WifiNetworkSuggestion.Builder#setCarrierMerged(boolean)}
     * @return True to indicate that carrier network offload is enabled, false otherwise.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public boolean isCarrierNetworkOffloadEnabled(int subscriptionId, boolean merged) {
        try {
            return mService.isCarrierNetworkOffloadEnabled(subscriptionId, merged);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Interface for network suggestion user approval status change listener.
     * Should be implemented by applications and registered using
     * {@link #addSuggestionUserApprovalStatusListener(Executor,
     * SuggestionUserApprovalStatusListener)} (
     */
    public interface SuggestionUserApprovalStatusListener {

        /**
         * Called when the user approval status of the App has changed.
         * @param status The current status code for the user approval. One of the
         *               {@code STATUS_SUGGESTION_APPROVAL_} values.
         */
        void onUserApprovalStatusChange(@SuggestionUserApprovalStatus int status);
    }

    private class SuggestionUserApprovalStatusListenerProxy extends
            ISuggestionUserApprovalStatusListener.Stub {
        private final Executor mExecutor;
        private final SuggestionUserApprovalStatusListener mListener;

        SuggestionUserApprovalStatusListenerProxy(@NonNull Executor executor,
                @NonNull SuggestionUserApprovalStatusListener listener) {
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public void onUserApprovalStatusChange(int status) {
            mExecutor.execute(() -> mListener.onUserApprovalStatusChange(status));
        }

    }

    /**
     * Add a listener for Wi-Fi network suggestion user approval status.
     * See {@link SuggestionUserApprovalStatusListener}.
     * Caller will receive a callback immediately after adding a listener and when the user approval
     * status of the caller has changed.
     * Caller can remove a previously registered listener using
     * {@link WifiManager#removeSuggestionUserApprovalStatusListener(
     * SuggestionUserApprovalStatusListener)}
     * A caller can add multiple listeners to monitor the event.
     * @param executor The executor to execute the listener of the {@code listener} object.
     * @param listener listener for suggestion user approval status changes.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void addSuggestionUserApprovalStatusListener(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull SuggestionUserApprovalStatusListener listener) {
        if (listener == null) throw new NullPointerException("Listener cannot be null");
        if (executor == null) throw new NullPointerException("Executor cannot be null");
        Log.v(TAG, "addSuggestionUserApprovalStatusListener listener=" + listener
                + ", executor=" + executor);
        try {
            synchronized (sSuggestionUserApprovalStatusListenerMap) {
                ISuggestionUserApprovalStatusListener.Stub binderCallback =
                        new SuggestionUserApprovalStatusListenerProxy(executor, listener);
                sSuggestionUserApprovalStatusListenerMap.put(System.identityHashCode(listener),
                        binderCallback);
                mService.addSuggestionUserApprovalStatusListener(binderCallback,
                        mContext.getOpPackageName());
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

    }

    /**
     * Allow callers to remove a previously registered listener using
     * {@link #addSuggestionUserApprovalStatusListener(Executor,
     * SuggestionUserApprovalStatusListener)}. After calling this method,
     * applications will no longer receive network suggestion user approval status change through
     * that listener.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    public void removeSuggestionUserApprovalStatusListener(
            @NonNull SuggestionUserApprovalStatusListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        Log.v(TAG, "removeSuggestionUserApprovalStatusListener: listener=" + listener);
        try {
            synchronized (sSuggestionUserApprovalStatusListenerMap) {
                int listenerIdentifier = System.identityHashCode(listener);
                if (!sSuggestionUserApprovalStatusListenerMap.contains(listenerIdentifier)) {
                    Log.w(TAG, "Unknown external callback " + listenerIdentifier);
                    return;
                }
                mService.removeSuggestionUserApprovalStatusListener(
                        sSuggestionUserApprovalStatusListenerMap.get(listenerIdentifier),
                        mContext.getOpPackageName());
                sSuggestionUserApprovalStatusListenerMap.remove(listenerIdentifier);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Indicates the start/end of an emergency scan request being processed by {@link WifiScanner}.
     * The wifi stack should ensure that the wifi chip remains on for the duration of the scan.
     * WifiScanner detects emergency scan requests via
     * {@link WifiScanner.ScanSettings#ignoreLocationSettings} flag.
     *
     * If the wifi stack is off (because location & wifi toggles are off) when this indication is
     * received, the wifi stack will temporarily move to a scan only mode. Since location toggle
     * is off, only scan with
     * {@link WifiScanner.ScanSettings#ignoreLocationSettings} flag set will be
     * allowed to be processed for this duration.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    public void setEmergencyScanRequestInProgress(boolean inProgress) {
        try {
            mService.setEmergencyScanRequestInProgress(inProgress);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable or disable Wi-Fi scoring.  Wi-Fi network status is evaluated by Wi-Fi scoring
     * {@link WifiScoreReport}. This API enables/disables Wi-Fi scoring to take action on network
     * selection.
     *
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @return true The status of Wifi scoring is set successfully.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public boolean setWifiScoringEnabled(boolean enabled) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "setWifiScoringEnabled: " + enabled);
        }
        try {
            return mService.setWifiScoringEnabled(enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Flush Passpoint ANQP cache, and clear pending ANQP requests. Allows the caller to reset the
     * Passpoint ANQP state, if required.
     *
     * Notes:
     * 1. Flushing the ANQP cache may cause delays in discovering and connecting to Passpoint
     * networks.
     * 2. Intended to be used by Device Owner (DO), Profile Owner (PO), Settings and provisioning
     * apps.
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_MANAGED_PROVISIONING,
            android.Manifest.permission.NETWORK_CARRIER_PROVISIONING
            }, conditional = true)
    public void flushPasspointAnqpCache() {
        try {
            mService.flushPasspointAnqpCache(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of {@link WifiAvailableChannel} for the specified band and operational
     * mode(s), that is allowed for the current regulatory domain. An empty list implies that there
     * are no available channels for use.
     *
     * Note: the {@code band} parameter which is specified as a {@code WifiScanner#WIFI_BAND_*}
     * constant is limited to one of the band values specified below. Specifically, if the 5GHz
     * band is included then it must include the DFS channels - an exception will be thrown
     * otherwise. The caller should not make any assumptions about whether DFS channels are allowed.
     * This API will indicate whether DFS channels are allowed for the specified operation mode(s)
     * per device policy.
     *
     * @param band one of the following band constants defined in {@code WifiScanner#WIFI_BAND_*}
     *             constants.
     *             1. {@code WifiScanner#WIFI_BAND_UNSPECIFIED}=0 - no band specified; Looks for the
     *                channels in all the available bands - 2.4 GHz, 5 GHz, 6 GHz and 60 GHz
     *             2. {@code WifiScanner#WIFI_BAND_24_GHZ}=1
     *             3. {@code WifiScanner#WIFI_BAND_5_GHZ_WITH_DFS}=6
     *             4. {@code WifiScanner#WIFI_BAND_BOTH_WITH_DFS}=7
     *             5. {@code WifiScanner#WIFI_BAND_6_GHZ}=8
     *             6. {@code WifiScanner#WIFI_BAND_24_5_WITH_DFS_6_GHZ}=15
     *             7. {@code WifiScanner#WIFI_BAND_60_GHZ}=16
     *             8. {@code WifiScanner#WIFI_BAND_24_5_WITH_DFS_6_60_GHZ}=31
     * @param mode Bitwise OR of {@code WifiAvailableChannel#OP_MODE_*} constants
     *        e.g. {@link WifiAvailableChannel#OP_MODE_WIFI_AWARE}
     * @return a list of {@link WifiAvailableChannel}
     *
     * @throws UnsupportedOperationException - if this API is not supported on this device
     *         or IllegalArgumentException - if the band specified is not one among the list
     *         of bands mentioned above.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @NonNull
    @RequiresPermission(NEARBY_WIFI_DEVICES)
    public List<WifiAvailableChannel> getAllowedChannels(
            int band,
            @WifiAvailableChannel.OpMode int mode) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            return mService.getUsableChannels(band, mode,
                    WifiAvailableChannel.FILTER_REGULATORY, mContext.getOpPackageName(), extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of {@link WifiAvailableChannel} for the specified band and operational
     * mode(s) per the current regulatory domain and device-specific constraints such as concurrency
     * state and interference due to other radios. An empty list implies that there are no available
     * channels for use.
     *
     * Note: the {@code band} parameter which is specified as a {@code WifiScanner#WIFI_BAND_*}
     * constant is limited to one of the band values specified below. Specifically, if the 5GHz
     * band is included then it must include the DFS channels - an exception will be thrown
     * otherwise. The caller should not make any assumptions about whether DFS channels are allowed.
     * This API will indicate whether DFS channels are allowed for the specified operation mode(s)
     * per device policy.
     *
     * @param band one of the following band constants defined in {@code WifiScanner#WIFI_BAND_*}
     *             constants.
     *             1. {@code WifiScanner#WIFI_BAND_UNSPECIFIED}=0 - no band specified; Looks for the
     *                channels in all the available bands - 2.4 GHz, 5 GHz, 6 GHz and 60 GHz
     *             2. {@code WifiScanner#WIFI_BAND_24_GHZ}=1
     *             3. {@code WifiScanner#WIFI_BAND_5_GHZ_WITH_DFS}=6
     *             4. {@code WifiScanner#WIFI_BAND_BOTH_WITH_DFS}=7
     *             5. {@code WifiScanner#WIFI_BAND_6_GHZ}=8
     *             6. {@code WifiScanner#WIFI_BAND_24_5_WITH_DFS_6_GHZ}=15
     *             7. {@code WifiScanner#WIFI_BAND_60_GHZ}=16
     *             8. {@code WifiScanner#WIFI_BAND_24_5_WITH_DFS_6_60_GHZ}=31
     * @param mode Bitwise OR of {@code WifiAvailableChannel#OP_MODE_*} constants
     *        e.g. {@link WifiAvailableChannel#OP_MODE_WIFI_AWARE}
     * @return a list of {@link WifiAvailableChannel}
     *
     * @throws UnsupportedOperationException - if this API is not supported on this device
     *         or IllegalArgumentException - if the band specified is not one among the list
     *         of bands mentioned above.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @NonNull
    @RequiresPermission(NEARBY_WIFI_DEVICES)
    public List<WifiAvailableChannel> getUsableChannels(
            int band,
            @WifiAvailableChannel.OpMode int mode) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            return mService.getUsableChannels(band, mode,
                    WifiAvailableChannel.getUsableFilter(), mContext.getOpPackageName(), extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * If the device supports Wi-Fi Passpoint, the user can explicitly enable or disable it.
     * That status can be queried using this method.
     * @return {@code true} if Wi-Fi Passpoint is enabled
     *
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public boolean isWifiPasspointEnabled() {
        try {
            return mService.isWifiPasspointEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Explicitly enable or disable Wi-Fi Passpoint as a global switch.
     * The global Passpoint enabling/disabling overrides individual configuration
     * enabling/disabling.
     * Passpoint global status can be queried by {@link WifiManager#isWifiPasspointEnabled }.
     *
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    public void setWifiPasspointEnabled(boolean enabled) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "setWifiPasspointEnabled: " + enabled);
        }
        try {
            mService.setWifiPasspointEnabled(enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * The device may support concurrent connections to multiple internet-providing Wi-Fi
     * networks (APs) - that is indicated by
     * {@link WifiManager#isStaConcurrencyForMultiInternetSupported()}.
     * This method indicates whether or not the feature is currently enabled.
     * A value of {@link WifiManager#WIFI_MULTI_INTERNET_MODE_DISABLED} indicates that the feature
     * is disabled, a value of {@link WifiManager#WIFI_MULTI_INTERNET_MODE_DBS_AP} or
     * {@link WifiManager#WIFI_MULTI_INTERNET_MODE_MULTI_AP} indicates that the feature is enabled.
     *
     * The app can register to receive the corresponding Wi-Fi networks using the
     * {@link ConnectivityManager#registerNetworkCallback(NetworkRequest, NetworkCallback)} API with
     * a {@link WifiNetworkSpecifier} configured using the
     * {@link WifiNetworkSpecifier.Builder#setBand(int)} method.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
    public @WifiMultiInternetMode int getStaConcurrencyForMultiInternetMode() {
        try {
            return mService.getStaConcurrencyForMultiInternetMode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if the currently connected network meets the minimum required Wi-Fi security level set.
     * If not, the current network will be disconnected.
     *
     * @throws SecurityException if the caller does not have permission.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SystemApi
    @RequiresPermission(android.Manifest.permission.MANAGE_DEVICE_ADMINS)
    public void notifyMinimumRequiredWifiSecurityLevelChanged(int level) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "notifyMinimumRequiredWifiSecurityLevelChanged");
        }
        try {
            mService.notifyMinimumRequiredWifiSecurityLevelChanged(level);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if the currently connected network meets the Wi-Fi SSID policy set.
     * If not, the current network will be disconnected.
     *
     * @throws SecurityException if the caller does not have permission.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SystemApi
    @RequiresPermission(android.Manifest.permission.MANAGE_DEVICE_ADMINS)
    public void notifyWifiSsidPolicyChanged(@NonNull WifiSsidPolicy policy) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "notifyWifiSsidPolicyChanged");
        }
        try {
            if (policy != null) {
                mService.notifyWifiSsidPolicyChanged(
                        policy.getPolicyType(), new ArrayList<>(policy.getSsids()));
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Configure whether or not concurrent multiple connections to internet-providing Wi-Fi
     * networks (AP) is enabled.
     * Use {@link WifiManager#WIFI_MULTI_INTERNET_MODE_DISABLED} to disable, and either
     * {@link WifiManager#WIFI_MULTI_INTERNET_MODE_DBS_AP} or
     * {@link WifiManager#WIFI_MULTI_INTERNET_MODE_MULTI_AP} to enable in different modes.
     * The {@link WifiManager#getStaConcurrencyForMultiInternetMode() } can be used to retrieve
     * the current mode.
     *
     * @param mode Multi internet mode.
     * @return true when the mode is set successfully, false when failed.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD
    })
    public boolean setStaConcurrencyForMultiInternetMode(@WifiMultiInternetMode int mode) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "setStaConcurrencyForMultiInternetMode: " + mode);
        }
        try {
            return mService.setStaConcurrencyForMultiInternetMode(mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Intent action to launch a dialog from the WifiDialog app.
     * Must include EXTRA_DIALOG_ID, EXTRA_DIALOG_TYPE, and appropriate extras for the dialog type.
     * @hide
     */
    public static final String ACTION_LAUNCH_DIALOG =
            "android.net.wifi.action.LAUNCH_DIALOG";

    /**
     * Intent action to dismiss an existing dialog from the WifiDialog app.
     * Must include EXTRA_DIALOG_ID.
     * @hide
     */
    public static final String ACTION_DISMISS_DIALOG =
            "android.net.wifi.action.DISMISS_DIALOG";

    /**
     * Unknown DialogType.
     * @hide
     */
    public static final int DIALOG_TYPE_UNKNOWN = 0;

    /**
     * DialogType for a simple dialog.
     * @see {@link com.android.server.wifi.WifiDialogManager#createSimpleDialog}
     * @hide
     */
    public static final int DIALOG_TYPE_SIMPLE = 1;

    /**
     * DialogType for a P2P Invitation Sent dialog.
     * @see {@link com.android.server.wifi.WifiDialogManager#createP2pInvitationSentDialog}
     * @hide
     */
    public static final int DIALOG_TYPE_P2P_INVITATION_SENT = 2;

    /**
     * DialogType for a P2P Invitation Received dialog.
     * @see {@link com.android.server.wifi.WifiDialogManager#createP2pInvitationReceivedDialog}
     * @hide
     */
    public static final int DIALOG_TYPE_P2P_INVITATION_RECEIVED = 3;

    /** @hide */
    @IntDef(prefix = { "DIALOG_TYPE_" }, value = {
            DIALOG_TYPE_UNKNOWN,
            DIALOG_TYPE_SIMPLE,
            DIALOG_TYPE_P2P_INVITATION_SENT,
            DIALOG_TYPE_P2P_INVITATION_RECEIVED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogType {}

    /**
     * Dialog positive button was clicked.
     * @hide
     */
    public static final int DIALOG_REPLY_POSITIVE = 0;

    /**
     * Dialog negative button was clicked.
     * @hide
     */
    public static final int DIALOG_REPLY_NEGATIVE = 1;

    /**
     * Dialog neutral button was clicked.
     * @hide
     */
    public static final int DIALOG_REPLY_NEUTRAL = 2;

    /**
     * Dialog was cancelled.
     * @hide
     */
    public static final int DIALOG_REPLY_CANCELLED = 3;

    /**
     * Indication of a reply to a dialog.
     * See {@link WifiManager#replyToSimpleDialog(int, int)}
     * @hide
     */
    @IntDef(prefix = { "DIALOG_TYPE_" }, value = {
            DIALOG_REPLY_POSITIVE,
            DIALOG_REPLY_NEGATIVE,
            DIALOG_REPLY_NEUTRAL,
            DIALOG_REPLY_CANCELLED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogReply {}

    /**
     * Invalid dialog id for dialogs that are not currently active.
     * @hide
     */
    public static final int INVALID_DIALOG_ID = -1;

    /**
     * Extra int indicating the type of dialog to display.
     * @hide
     */
    public static final String EXTRA_DIALOG_TYPE = "android.net.wifi.extra.DIALOG_TYPE";

    /**
     * Extra int indicating the ID of a dialog. The value must not be {@link #INVALID_DIALOG_ID}.
     * @hide
     */
    public static final String EXTRA_DIALOG_ID = "android.net.wifi.extra.DIALOG_ID";

    /**
     * Extra String indicating the title of a simple dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_TITLE = "android.net.wifi.extra.DIALOG_TITLE";

    /**
     * Extra String indicating the message of a simple dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_MESSAGE = "android.net.wifi.extra.DIALOG_MESSAGE";

    /**
     * Extra String indicating the message URL of a simple dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_MESSAGE_URL =
            "android.net.wifi.extra.DIALOG_MESSAGE_URL";

    /**
     * Extra String indicating the start index of a message URL span of a simple dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_MESSAGE_URL_START =
            "android.net.wifi.extra.DIALOG_MESSAGE_URL_START";

    /**
     * Extra String indicating the end index of a message URL span of a simple dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_MESSAGE_URL_END =
            "android.net.wifi.extra.DIALOG_MESSAGE_URL_END";

    /**
     * Extra String indicating the positive button text of a simple dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_POSITIVE_BUTTON_TEXT =
            "android.net.wifi.extra.DIALOG_POSITIVE_BUTTON_TEXT";

    /**
     * Extra String indicating the negative button text of a simple dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_NEGATIVE_BUTTON_TEXT =
            "android.net.wifi.extra.DIALOG_NEGATIVE_BUTTON_TEXT";

    /**
     * Extra String indicating the neutral button text of a simple dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_NEUTRAL_BUTTON_TEXT =
            "android.net.wifi.extra.DIALOG_NEUTRAL_BUTTON_TEXT";

    /**
     * Extra long indicating the timeout in milliseconds of a dialog.
     * @hide
     */
    public static final String EXTRA_DIALOG_TIMEOUT_MS = "android.net.wifi.extra.DIALOG_TIMEOUT_MS";

    /**
     * Extra String indicating a P2P device name for a P2P Invitation Sent/Received dialog.
     * @hide
     */
    public static final String EXTRA_P2P_DEVICE_NAME = "android.net.wifi.extra.P2P_DEVICE_NAME";

    /**
     * Extra boolean indicating that a PIN is requested for a P2P Invitation Received dialog.
     * @hide
     */
    public static final String EXTRA_P2P_PIN_REQUESTED = "android.net.wifi.extra.P2P_PIN_REQUESTED";

    /**
     * Extra String indicating the PIN to be displayed for a P2P Invitation Sent/Received dialog.
     * @hide
     */
    public static final String EXTRA_P2P_DISPLAY_PIN = "android.net.wifi.extra.P2P_DISPLAY_PIN";

    /**
     * Extra boolean indicating ACTION_CLOSE_SYSTEM_DIALOGS should not close the Wi-Fi dialogs.
     * @hide
     */
    public static final String EXTRA_CLOSE_SYSTEM_DIALOGS_EXCEPT_WIFI =
            "android.net.wifi.extra.CLOSE_SYSTEM_DIALOGS_EXCEPT_WIFI";

    /**
     * Returns a set of packages that aren't DO or PO but should be able to manage WiFi networks.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    @NonNull
    public Set<String> getOemPrivilegedWifiAdminPackages() {
        try {
            return new ArraySet<>(mService.getOemPrivilegedWifiAdminPackages());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Method for WifiDialog to notify the framework of a reply to a simple dialog.
     * @param dialogId id of the replying dialog.
     * @param reply reply of the dialog.
     * @hide
     */
    public void replyToSimpleDialog(int dialogId, @DialogReply int reply) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "replyToWifiEnableRequestDialog: dialogId=" + dialogId
                    + " reply=" + reply);
        }
        try {
            mService.replyToSimpleDialog(dialogId, reply);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Method for WifiDialog to notify the framework of a reply to a P2P Invitation Received dialog.
     * @param dialogId id of the replying dialog.
     * @param accepted Whether the invitation was accepted.
     * @param optionalPin PIN of the reply, or {@code null} if none was supplied.
     * @hide
     */
    public void replyToP2pInvitationReceivedDialog(
            int dialogId, boolean accepted, @Nullable String optionalPin) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "replyToP2pInvitationReceivedDialog: "
                    + "dialogId=" + dialogId
                    + ", accepted=" + accepted
                    + ", pin=" + optionalPin);
        }
        try {
            mService.replyToP2pInvitationReceivedDialog(dialogId, accepted, optionalPin);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Specify a list of DHCP options to use for any network whose SSID is specified and which
     * transmits vendor-specific information elements (VSIEs) using the specified Organizationally
     * Unique Identifier (OUI). If the AP transmits VSIEs for multiple specified OUIs then all
     * matching DHCP options will be used. The allowlist for DHCP options in
     * {@link android.net.ip.IpClient} gates whether the DHCP options will actually be used.
     * When DHCP options are used: if the option value {@link android.net.DhcpOption#getValue()}
     * is null, the option type {@link android.net.DhcpOption#getType()} will be put in the
     * Parameter Request List in the DHCP packets; otherwise, the option will be included in the
     * options section in the DHCP packets. Use {@link #removeCustomDhcpOptions(Object, Object)}
     * to remove the specified DHCP options.
     *
     * @param ssid the network SSID.
     * @param oui the 3-byte OUI.
     * @param options the list of {@link android.net.DhcpOption}.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public void addCustomDhcpOptions(@NonNull WifiSsid ssid, @NonNull byte[] oui,
            @NonNull List<DhcpOption> options) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "addCustomDhcpOptions: ssid="
                    + ssid + ", oui=" + Arrays.toString(oui) + ", options=" + options);
        }
        try {
            mService.addCustomDhcpOptions(ssid, oui, options);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove custom DHCP options specified by {@link #addCustomDhcpOptions(Object, Object, List)}.
     *
     * @param ssid the network SSID.
     * @param oui the 3-byte OUI.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.OVERRIDE_WIFI_CONFIG
    })
    public void removeCustomDhcpOptions(@NonNull WifiSsid ssid, @NonNull byte[] oui) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "removeCustomDhcpOptions: ssid=" + ssid + ", oui=" + Arrays.toString(oui));
        }
        try {
            mService.removeCustomDhcpOptions(ssid, oui);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Wi-Fi interface of type STA (station/client Wi-Fi infrastructure device).
     */
    public static final int WIFI_INTERFACE_TYPE_STA = 0;

    /**
     * Wi-Fi interface of type AP (access point Wi-Fi infrastructure device).
     */
    public static final int WIFI_INTERFACE_TYPE_AP = 1;

    /**
     * Wi-Fi interface of type Wi-Fi Aware (aka NAN).
     */
    public static final int WIFI_INTERFACE_TYPE_AWARE = 2;

    /**
     * Wi-Fi interface of type Wi-Fi Direct (aka P2P).
     */
    public static final int WIFI_INTERFACE_TYPE_DIRECT = 3;

    /** @hide */
    @IntDef(prefix = { "WIFI_INTERFACE_TYPE_" }, value = {
            WIFI_INTERFACE_TYPE_STA,
            WIFI_INTERFACE_TYPE_AP,
            WIFI_INTERFACE_TYPE_AWARE,
            WIFI_INTERFACE_TYPE_DIRECT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface WifiInterfaceType {}

    /**
     * Class describing an impact of interface creation - returned by
     * {@link #reportCreateInterfaceImpact(int, boolean, Executor, BiConsumer)}. Due to Wi-Fi
     * concurrency limitations certain interfaces may have to be torn down. Each of these
     * interfaces was requested by a set of applications who could potentially be impacted.
     *
     * This class contain the information for a single interface: the interface type with
     * {@link InterfaceCreationImpact#getInterfaceType()} and the set of impacted packages
     * with {@link InterfaceCreationImpact#getPackages()}.
     */
    public static class InterfaceCreationImpact {
        private final int mInterfaceType;
        private final Set<String> mPackages;

        public InterfaceCreationImpact(@WifiInterfaceType int interfaceType,
                @NonNull Set<String> packages) {
            mInterfaceType = interfaceType;
            mPackages = packages;
        }

        /**
         * @return The interface type which will be torn down to make room for the interface
         * requested in {@link #reportCreateInterfaceImpact(int, boolean, Executor, BiConsumer)}.
         */
        public @WifiInterfaceType int getInterfaceType() {
            return mInterfaceType;
        }

        /**
         * @return The list of potentially impacted packages due to tearing down the interface
         * specified in {@link #getInterfaceType()}.
         */
        public @NonNull Set<String> getPackages() {
            return mPackages;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mInterfaceType, mPackages);
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;
            if (!(that instanceof InterfaceCreationImpact)) return false;
            InterfaceCreationImpact thatInterfaceCreationImpact = (InterfaceCreationImpact) that;

            return this.mInterfaceType == thatInterfaceCreationImpact.mInterfaceType
                    && Objects.equals(this.mPackages, thatInterfaceCreationImpact.mPackages);
        }
    }

    /**
     * Queries the framework to determine whether the specified interface can be created, and if
     * so - what other interfaces would be torn down by the framework to allow this creation if
     * it were requested. The result is returned via the specified {@link BiConsumer} callback
     * which returns two arguments:
     * <li>First argument: a {@code boolean} - indicating whether or not the interface can be
     * created.</li>
     * <li>Second argument: a {@code Set<InterfaceCreationImpact>} - if the interface can be
     * created (first argument is {@code true} then this is the set of interface types which
     * will be removed and the packages which requested them. Possibly an empty set. If the
     * first argument is {@code false}, then an empty set will be returned here.</li>
     * <p>
     * Interfaces, input and output, are specified using the {@code WIFI_INTERFACE_*} constants:
     * {@link #WIFI_INTERFACE_TYPE_STA}, {@link #WIFI_INTERFACE_TYPE_AP},
     * {@link #WIFI_INTERFACE_TYPE_AWARE}, or {@link #WIFI_INTERFACE_TYPE_DIRECT}.
     * <p>
     * This method does not actually create the interface. That operation is handled by the
     * framework when a particular service method is called. E.g. a Wi-Fi Direct interface may be
     * created when various methods of {@link android.net.wifi.p2p.WifiP2pManager} are called,
     * similarly for Wi-Fi Aware and {@link android.net.wifi.aware.WifiAwareManager}.
     * <p>
     * Note: the information returned via this method is the current snapshot of the system. It may
     * change due to actions of the framework or other apps.
     *
     * @param interfaceType The interface type whose possible creation is being queried.
     * @param requireNewInterface Indicates that the query is for a new interface of the specified
     *                             type - an existing interface won't meet the query. Some
     *                             operations (such as Wi-Fi Direct and Wi-Fi Aware are a shared
     *                             resource and so may not need a new interface).
     * @param executor An {@link Executor} on which to return the result.
     * @param resultCallback The asynchronous callback which will return two argument: a
     * {@code boolean} (whether the interface can be created), and a
     * {@code Set<InterfaceCreationImpact>} (a set of {@link InterfaceCreationImpact}:
     *                       interfaces which will be destroyed when the interface is created
     *                       and the packages which requested them and thus may be impacted).
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(allOf = {android.Manifest.permission.MANAGE_WIFI_INTERFACES,
            ACCESS_WIFI_STATE})
    public void reportCreateInterfaceImpact(@WifiInterfaceType int interfaceType,
            boolean requireNewInterface,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull BiConsumer<Boolean, Set<InterfaceCreationImpact>> resultCallback) {
        Objects.requireNonNull(executor, "Non-null executor required");
        Objects.requireNonNull(resultCallback, "Non-null resultCallback required");
        try {
            mService.reportCreateInterfaceImpact(mContext.getOpPackageName(), interfaceType,
                    requireNewInterface, new IInterfaceCreationInfoCallback.Stub() {
                        @Override
                        public void onResults(boolean canCreate, int[] interfacesToDelete,
                                String[] packagesForInterfaces) {
                            Binder.clearCallingIdentity();
                            if ((interfacesToDelete == null && packagesForInterfaces != null)
                                    || (interfacesToDelete != null
                                    && packagesForInterfaces == null) || (canCreate && (
                                    interfacesToDelete == null || interfacesToDelete.length
                                            != packagesForInterfaces.length))) {
                                Log.e(TAG,
                                        "reportImpactToCreateIfaceRequest: Invalid callback "
                                                + "parameters - canCreate="
                                                + canCreate + ", interfacesToDelete="
                                                + Arrays.toString(interfacesToDelete)
                                                + ", worksourcesForInterfaces="
                                                + Arrays.toString(packagesForInterfaces));
                                return;
                            }

                            final Set<InterfaceCreationImpact> finalSet =
                                    (canCreate && interfacesToDelete.length > 0) ? new ArraySet<>()
                                            : Collections.emptySet();
                            if (canCreate) {
                                for (int i = 0; i < interfacesToDelete.length; ++i) {
                                    finalSet.add(
                                            new InterfaceCreationImpact(interfacesToDelete[i],
                                                    packagesForInterfaces[i] == null
                                                            ? Collections.emptySet()
                                                            : new ArraySet<>(
                                                                    packagesForInterfaces[i]
                                                                            .split(","))));
                                }
                            }
                            executor.execute(() -> resultCallback.accept(canCreate, finalSet));
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the max number of channels that is allowed to be set on a
     * {@link WifiNetworkSpecifier}.
     * @see WifiNetworkSpecifier.Builder#setPreferredChannelsFrequenciesMhz(int[])
     *
     * @return The max number of channels can be set on a request.
     */

    public int getMaxNumberOfChannelsPerNetworkSpecifierRequest() {
        try {
            return mService.getMaxNumberOfChannelsPerRequest();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Add a list of new application-initiated QoS policies.
     *
     * Note: Policies are managed using a policy ID, which can be retrieved using
     *       {@link QosPolicyParams#getPolicyId()}. This ID can be used when removing a policy via
     *       {@link #removeQosPolicies(int[])}. The caller is in charge of assigning and managing
     *       the policy IDs for any requested policies.
     *
     * Note: Policies with duplicate IDs are not allowed. To update an existing policy, first
     *       remove it using {@link #removeQosPolicies(int[])}, and then re-add it using this API.
     *
     * Note: All policies in a single request must have the same {@link QosPolicyParams.Direction}.
     *
     * Note: Currently, only the {@link QosPolicyParams#DIRECTION_DOWNLINK} direction is supported.
     *
     * @param policyParamsList List of {@link QosPolicyParams} objects describing the requested
     *                         policies. Must have a maximum length of
     *                         {@link #getMaxNumberOfPoliciesPerQosRequest()}.
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return a list of integer status
     *                        codes from {@link QosRequestStatus}. Result list will be the same
     *                        length as the input list, and each status code will correspond to
     *                        the policy at that index in the input list.
     *
     * @throws SecurityException if caller does not have the required permissions.
     * @throws NullPointerException if the caller provided a null input.
     * @throws UnsupportedOperationException if the feature is not enabled.
     * @throws IllegalArgumentException if the input list is invalid.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            MANAGE_WIFI_NETWORK_SELECTION
    })
    public void addQosPolicies(@NonNull List<QosPolicyParams> policyParamsList,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<List<Integer>> resultsCallback) {
        Objects.requireNonNull(policyParamsList, "policyParamsList cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.addQosPolicies(policyParamsList, new Binder(), mContext.getOpPackageName(),
                    new IListListener.Stub() {
                        @Override
                        public void onResult(List value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove a list of existing application-initiated QoS policies, previously added via
     * {@link #addQosPolicies(List, Executor, Consumer)}.
     *
     * Note: Policies are identified by their policy IDs, which are assigned by the caller. The ID
     *       for a given policy can be retrieved using {@link QosPolicyParams#getPolicyId()}.
     *
     * @param policyIdList List of policy IDs corresponding to the policies to remove. Must have
     *                     a maximum length of {@link #getMaxNumberOfPoliciesPerQosRequest()}.
     * @throws SecurityException if caller does not have the required permissions.
     * @throws NullPointerException if the caller provided a null input.
     * @throws IllegalArgumentException if the input list is invalid.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            MANAGE_WIFI_NETWORK_SELECTION
    })
    public void removeQosPolicies(@NonNull int[] policyIdList) {
        Objects.requireNonNull(policyIdList, "policyIdList cannot be null");
        try {
            mService.removeQosPolicies(policyIdList, mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Remove all application-initiated QoS policies requested by this caller,
     * previously added via {@link #addQosPolicies(List, Executor, Consumer)}.
     *
     * @throws SecurityException if caller does not have the required permissions.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            MANAGE_WIFI_NETWORK_SELECTION
    })
    public void removeAllQosPolicies() {
        try {
            mService.removeAllQosPolicies(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set the link layer stats polling interval, in milliseconds.
     *
     * @param intervalMs a non-negative integer, for the link layer stats polling interval
     *                   in milliseconds.
     *                   To set a fixed interval, use a positive value.
     *                   For automatic handling of the interval, use value 0
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws SecurityException if the caller does not have permission.
     * @throws IllegalArgumentException if input is invalid.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    public void setLinkLayerStatsPollingInterval(@IntRange (from = 0) int intervalMs) {
        try {
            mService.setLinkLayerStatsPollingInterval(intervalMs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the link layer stats polling interval, in milliseconds.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return current
     *                        link layer stats polling interval in milliseconds.
     *
     * @throws UnsupportedOperationException if the API is not supported on this SDK version.
     * @throws SecurityException if the caller does not have permission.
     * @throws NullPointerException if the caller provided invalid inputs.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION)
    public void getLinkLayerStatsPollingInterval(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Integer> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.getLinkLayerStatsPollingInterval(
                    new IIntegerListener.Stub() {
                        @Override
                        public void onResult(int value) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> {
                                resultsCallback.accept(value);
                            });
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * This API allows a privileged application to set Multi-Link Operation mode.
     *
     * Multi-link operation (MLO) will allow Wi-Fi devices to operate on multiple links at the same
     * time through a single connection, aiming to support applications that require lower latency,
     * and higher capacity. Chip vendors have algorithms that run on the chip to use available links
     * based on incoming traffic and various inputs. This API allows system application to give a
     * suggestion to such algorithms on its preference using {@link MloMode}.
     *
     *
     * @param mode Refer {@link MloMode} for supported modes.
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return {@code Boolean} indicating
     *                        whether the MLO mode is successfully set or not.
     * @throws IllegalArgumentException if mode value is not in {@link MloMode}.
     * @throws NullPointerException if the caller provided a null input.
     * @throws SecurityException if caller does not have the required permissions.
     * @throws UnsupportedOperationException if the set operation is not supported on this SDK.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void setMloMode(@MloMode int mode, @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> resultsCallback) {

        if (mode < MLO_MODE_DEFAULT || mode > MLO_MODE_LOW_POWER) {
            throw new IllegalArgumentException("invalid mode: " + mode);
        }
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.setMloMode(mode, new IBooleanListener.Stub() {
                @Override
                public void onResult(boolean value) {
                    Binder.clearCallingIdentity();
                    executor.execute(() -> {
                        resultsCallback.accept(value);
                    });
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * This API allows a privileged application to get Multi-Link Operation mode. Refer
     * {@link WifiManager#setMloMode(int, Executor, Consumer)}  for more details.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return current MLO mode. Returns
     *                        {@link MloMode#MLO_MODE_DEFAULT} if information is not available,
     *                        e.g. if the driver/firmware doesn't provide this information.
     * @throws NullPointerException if the caller provided a null input.
     * @throws SecurityException if caller does not have the required permissions.
     * @throws UnsupportedOperationException if the get operation is not supported on this SDK.
     * @hide
     */
    @SystemApi
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void getMloMode(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Integer> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            mService.getMloMode(new IIntegerListener.Stub() {
                @Override
                public void onResult(int value) {
                    Binder.clearCallingIdentity();
                    executor.execute(() -> {
                        resultsCallback.accept(value);
                    });
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the maximum number of links supported by the chip for MLO association. e.g. if the Wi-Fi
     * chip supports eMLSR (Enhanced Multi-Link Single Radio) and STR (Simultaneous Transmit and
     * Receive) with following capabilities,
     * - Max MLO assoc link count = 3.
     * - Max MLO STR link count   = 2. See
     * {@link WifiManager#getMaxMloStrLinkCount(Executor, Consumer)}
     * One of the possible configuration is - STR (2.4 GHz , eMLSR(5 GHz, 6 GHz)), provided the
     * radio combination of the chip supports it.
     *
     * @param executor        The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return maximum MLO association link
     *                        count supported by the chip or -1 if error or not available.
     * @throws NullPointerException          if the caller provided a null input.
     * @throws SecurityException             if caller does not have the required permissions.
     * @throws UnsupportedOperationException if the get operation is not supported on this SDK.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void getMaxMloAssociationLinkCount(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Integer> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            mService.getMaxMloAssociationLinkCount(new IIntegerListener.Stub() {
                @Override
                public void onResult(int value) {
                    Binder.clearCallingIdentity();
                    executor.execute(() -> {
                        resultsCallback.accept(value);
                    });
                }
            }, extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the maximum number of STR links used in Multi-Link Operation. The maximum number of STR
     * links used for MLO can be different from the number of radios supported by the chip. e.g. if
     * the Wi-Fi chip supports eMLSR (Enhanced Multi-Link Single Radio) and STR (Simultaneous
     * Transmit and Receive) with following capabilities,
     * - Max MLO assoc link count = 3. See
     *   {@link WifiManager#getMaxMloAssociationLinkCount(Executor, Consumer)}.
     * - Max MLO STR link count   = 2.
     * One of the possible configuration is - STR (2.4 GHz, eMLSR(5 GHz, 6 GHz)), provided the radio
     * combination of the chip supports it.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return maximum STR link count
     *                       supported by the chip in MLO mode or -1 if error or not available.
     * @throws NullPointerException if the caller provided a null input.
     * @throws SecurityException if caller does not have the required permissions.
     * @throws UnsupportedOperationException if the get operation is not supported on this SDK
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void getMaxMloStrLinkCount(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Integer> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            mService.getMaxMloStrLinkCount(new IIntegerListener.Stub() {
                @Override
                public void onResult(int value) {
                    Binder.clearCallingIdentity();
                    executor.execute(() -> {
                        resultsCallback.accept(value);
                    });
                }
            }, extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the set of band combinations supported simultaneously by the Wi-Fi Chip.
     *
     * Note: This method returns simultaneous band operation combination and not multichannel
     * concurrent operation (MCC) combination.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultsCallback An asynchronous callback that will return a list of possible
     *                        simultaneous band combinations supported by the chip or empty list if
     *                        not available. Band value is defined in {@link WifiScanner.WifiBand}.
     * @throws NullPointerException if the caller provided a null input.
     * @throws SecurityException if caller does not have the required permissions.
     * @throws UnsupportedOperationException if the get operation is not supported on this SDK.
     * @hide
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void getSupportedSimultaneousBandCombinations(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<List<int[]>> resultsCallback) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(resultsCallback, "resultsCallback cannot be null");
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            mService.getSupportedSimultaneousBandCombinations(new IWifiBandsListener.Stub() {
                @Override
                public void onResult(WifiBands[] supportedBands) {
                    Binder.clearCallingIdentity();
                    List<int[]> bandCombinations = new ArrayList<>();
                    for (WifiBands wifiBands : supportedBands) {
                        bandCombinations.add(wifiBands.bands);
                    }
                    executor.execute(() -> {
                        resultsCallback.accept(bandCombinations);
                    });
                }
            }, extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}