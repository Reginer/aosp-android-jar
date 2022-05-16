/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.PendingIntent;
import android.content.Context;
import android.net.IpSecManager.UdpEncapsulationSocket;
import android.net.SocketKeepalive.Callback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executor;

import static android.os._Original_Build.VERSION_CODES.BASE;

/**
 * Stub ConnectivityManager for layoutlib
 */
public class ConnectivityManager {
    @Deprecated
    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    public static final String ACTION_CAPTIVE_PORTAL_SIGN_IN = "android.net.conn.CAPTIVE_PORTAL";

    @Deprecated
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    @Deprecated
    public static final String EXTRA_NETWORK_TYPE = "networkType";

    @Deprecated
    public static final String EXTRA_IS_FAILOVER = "isFailover";
    @Deprecated
    public static final String EXTRA_OTHER_NETWORK_INFO = "otherNetwork";
    public static final String EXTRA_NO_CONNECTIVITY = "noConnectivity";
    public static final String EXTRA_REASON = "reason";
    @Deprecated
    public static final String EXTRA_EXTRA_INFO = "extraInfo";
    public static final String EXTRA_INET_CONDITION = "inetCondition";
    public static final String EXTRA_CAPTIVE_PORTAL = "android.net.extra.CAPTIVE_PORTAL";

    public static final String EXTRA_CAPTIVE_PORTAL_URL = "android.net.extra.CAPTIVE_PORTAL_URL";

    public static final String EXTRA_CAPTIVE_PORTAL_PROBE_SPEC =
            "android.net.extra.CAPTIVE_PORTAL_PROBE_SPEC";

    public static final String EXTRA_CAPTIVE_PORTAL_USER_AGENT =
            "android.net.extra.CAPTIVE_PORTAL_USER_AGENT";

    public static final String ACTION_DATA_ACTIVITY_CHANGE =
            "android.net.conn.DATA_ACTIVITY_CHANGE";
    public static final String EXTRA_DEVICE_TYPE = "deviceType";
    public static final String EXTRA_IS_ACTIVE = "isActive";
    public static final String EXTRA_REALTIME_NS = "tsNanos";

    @Deprecated
    public static final String ACTION_BACKGROUND_DATA_SETTING_CHANGED =
            "android.net.conn.BACKGROUND_DATA_SETTING_CHANGED";

    public static final String INET_CONDITION_ACTION =
            "android.net.conn.INET_CONDITION_ACTION";

    public static final String ACTION_TETHER_STATE_CHANGED =
            TetheringManager.ACTION_TETHER_STATE_CHANGED;

    public static final String EXTRA_AVAILABLE_TETHER = TetheringManager.EXTRA_AVAILABLE_TETHER;

    public static final String EXTRA_ACTIVE_LOCAL_ONLY = TetheringManager.EXTRA_ACTIVE_LOCAL_ONLY;

    public static final String EXTRA_ACTIVE_TETHER = TetheringManager.EXTRA_ACTIVE_TETHER;

    public static final String EXTRA_ERRORED_TETHER = TetheringManager.EXTRA_ERRORED_TETHER;

    public static final String ACTION_CAPTIVE_PORTAL_TEST_COMPLETED =
            "android.net.conn.CAPTIVE_PORTAL_TEST_COMPLETED";
    public static final String EXTRA_IS_CAPTIVE_PORTAL = "captivePortal";

    public static final String ACTION_PROMPT_UNVALIDATED = "android.net.conn.PROMPT_UNVALIDATED";

    public static final String ACTION_PROMPT_LOST_VALIDATION =
            "android.net.conn.PROMPT_LOST_VALIDATION";

    public static final String ACTION_PROMPT_PARTIAL_CONNECTIVITY =
            "android.net.conn.PROMPT_PARTIAL_CONNECTIVITY";

    public static final int TETHERING_INVALID   = TetheringManager.TETHERING_INVALID;

    public static final int TETHERING_WIFI      = TetheringManager.TETHERING_WIFI;

    public static final int TETHERING_USB       = TetheringManager.TETHERING_USB;

    public static final int TETHERING_BLUETOOTH = TetheringManager.TETHERING_BLUETOOTH;

    public static final int TETHERING_WIFI_P2P = TetheringManager.TETHERING_WIFI_P2P;

    public static final int TYPE_NONE        = -1;

    @Deprecated
    public static final int TYPE_MOBILE      = 0;

    @Deprecated
    public static final int TYPE_WIFI        = 1;

    @Deprecated
    public static final int TYPE_MOBILE_MMS  = 2;

    @Deprecated
    public static final int TYPE_MOBILE_SUPL = 3;

    @Deprecated
    public static final int TYPE_MOBILE_DUN  = 4;

    @Deprecated
    public static final int TYPE_MOBILE_HIPRI = 5;

    @Deprecated
    public static final int TYPE_WIMAX       = 6;

    @Deprecated
    public static final int TYPE_BLUETOOTH   = 7;

    @Deprecated
    public static final int TYPE_DUMMY       = 8;

    @Deprecated
    public static final int TYPE_ETHERNET    = 9;

    @Deprecated
    public static final int TYPE_MOBILE_FOTA = 10;

    @Deprecated
    public static final int TYPE_MOBILE_IMS  = 11;

    @Deprecated
    public static final int TYPE_MOBILE_CBS  = 12;

    @Deprecated
    public static final int TYPE_WIFI_P2P    = 13;

    @Deprecated
    public static final int TYPE_MOBILE_IA = 14;

    @Deprecated
    public static final int TYPE_MOBILE_EMERGENCY = 15;

    @Deprecated
    public static final int TYPE_PROXY = 16;

    @Deprecated
    public static final int TYPE_VPN = 17;

    @Deprecated
    public static final int TYPE_TEST = 18; // TODO: Remove this once NetworkTypes are unused.

    @Deprecated
    public @interface LegacyNetworkType {}

    public static final int MAX_RADIO_TYPE = TYPE_TEST;

    public static final int MAX_NETWORK_TYPE = TYPE_TEST;

    @Deprecated
    public static final int DEFAULT_NETWORK_PREFERENCE = TYPE_WIFI;

    public static final int REQUEST_ID_UNSET = 0;

    public static final int NETID_UNSET = 0;
    public static final String PRIVATE_DNS_MODE_OFF = "off";
    public static final String PRIVATE_DNS_MODE_OPPORTUNISTIC = "opportunistic";
    public static final String PRIVATE_DNS_MODE_PROVIDER_HOSTNAME = "hostname";
    public static final String PRIVATE_DNS_DEFAULT_MODE_FALLBACK = PRIVATE_DNS_MODE_OPPORTUNISTIC;

    private static ConnectivityManager sInstance;

    @Deprecated
    public static boolean isNetworkTypeValid(int networkType) {
        return false;
    }

    @Deprecated
    public static String getNetworkTypeName(int type) {
        return null;
    }

    @Deprecated
    public static boolean isNetworkTypeMobile(int networkType) {
        return false;
    }

    @Deprecated
    public static boolean isNetworkTypeWifi(int networkType) {
        return false;
    }

    @Deprecated
    public void setNetworkPreference(int preference) {
    }

    @Deprecated
    public int getNetworkPreference() {
        return TYPE_NONE;
    }

    @Deprecated
    @Nullable
    public NetworkInfo getActiveNetworkInfo() {
        return null;
    }

    @Nullable
    public Network getActiveNetwork() {
        return null;
    }

    @Nullable
    public Network getActiveNetworkForUid(int uid) {
        return null;
    }

    public Network getActiveNetworkForUid(int uid, boolean ignoreBlocked) {
        return null;
    }

    public boolean isAlwaysOnVpnPackageSupportedForUser(int userId, @Nullable String vpnPackage) {
        return false;
    }

    public boolean setAlwaysOnVpnPackageForUser(int userId, @Nullable String vpnPackage,
            boolean lockdownEnabled, @Nullable List<String> lockdownList) {
        return false;
    }

    public String getAlwaysOnVpnPackageForUser(int userId) {
        return null;
    }

    public boolean isVpnLockdownEnabled(int userId) {
        return false;
    }

    public List<String> getVpnLockdownWhitelist(int userId) {
        return null;
    }

    public NetworkInfo getActiveNetworkInfoForUid(int uid) {
        return null;
    }

    public NetworkInfo getActiveNetworkInfoForUid(int uid, boolean ignoreBlocked) {
        return null;
    }

    @Deprecated
    @Nullable
    public NetworkInfo getNetworkInfo(int networkType) {
        return null;
    }

    @Deprecated
    @Nullable
    public NetworkInfo getNetworkInfo(@Nullable Network network) {
        return null;
    }

    public NetworkInfo getNetworkInfoForUid(Network network, int uid, boolean ignoreBlocked) {
        return null;
    }

    @Deprecated
    @NonNull
    public NetworkInfo[] getAllNetworkInfo() {
        return new NetworkInfo[0];
    }

    @Deprecated
    public Network getNetworkForType(int networkType) {
        return null;
    }

    @NonNull
    public Network[] getAllNetworks() {
        return new Network[0];
    }

    public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int userId) {
        return null;
    }

    public LinkProperties getActiveLinkProperties() {
        return null;
    }

    @Deprecated
    public LinkProperties getLinkProperties(int networkType) {
        return null;
    }

    @Nullable
    public LinkProperties getLinkProperties(@Nullable Network network) {
        return null;
    }

    @Nullable
    public NetworkCapabilities getNetworkCapabilities(@Nullable Network network) {
        return null;
    }

    @Deprecated
    public String getCaptivePortalServerUrl() {
        return null;
    }

    @Deprecated
    public int startUsingNetworkFeature(int networkType, String feature) {
        return -1;
    }

    @Deprecated
    public int stopUsingNetworkFeature(int networkType, String feature) {
        return -1;
    }

    public static NetworkCapabilities networkCapabilitiesForType(int type) {
        return null;
    }

    public static class PacketKeepaliveCallback {
        public PacketKeepaliveCallback() {
        }
        public void onStarted() {}
        public void onStopped() {}
        public void onError(int error) {}
    }

    public class PacketKeepalive {
        public static final int SUCCESS = 0;

        public static final int NO_KEEPALIVE = -1;

        public static final int BINDER_DIED = -10;

        public static final int ERROR_INVALID_NETWORK = -20;
        public static final int ERROR_INVALID_IP_ADDRESS = -21;
        public static final int ERROR_INVALID_PORT = -22;
        public static final int ERROR_INVALID_LENGTH = -23;
        public static final int ERROR_INVALID_INTERVAL = -24;

        public static final int ERROR_HARDWARE_UNSUPPORTED = -30;
        public static final int ERROR_HARDWARE_ERROR = -31;

        public static final int NATT_PORT = 4500;

        public static final int MIN_INTERVAL = 10;

        public void stop() {}
    }

    public PacketKeepalive startNattKeepalive(
            Network network, int intervalSeconds, PacketKeepaliveCallback callback,
            InetAddress srcAddr, int srcPort, InetAddress dstAddr) {
        return null;
    }

    public @NonNull SocketKeepalive createSocketKeepalive(@NonNull Network network,
            @NonNull UdpEncapsulationSocket socket,
            @NonNull InetAddress source,
            @NonNull InetAddress destination,
            @NonNull Executor executor,
            @NonNull Callback callback) {
        throw new UnsupportedOperationException(
                "createSocketKeepalive is not supported in layoutlib");
    }

    public @NonNull SocketKeepalive createNattKeepalive(@NonNull Network network,
            @NonNull ParcelFileDescriptor pfd,
            @NonNull InetAddress source,
            @NonNull InetAddress destination,
            @NonNull Executor executor,
            @NonNull Callback callback) {
        throw new UnsupportedOperationException(
                "createSocketKeepalive is not supported in layoutlib");
    }

    public @NonNull SocketKeepalive createSocketKeepalive(@NonNull Network network,
            @NonNull Socket socket,
            @NonNull Executor executor,
            @NonNull Callback callback) {
        throw new UnsupportedOperationException(
                "createSocketKeepalive is not supported in layoutlib");
    }

    @Deprecated
    public boolean requestRouteToHost(int networkType, int hostAddress) {
        return false;
    }

    @Deprecated
    public boolean requestRouteToHostAddress(int networkType, InetAddress hostAddress) {
        return false;
    }

    @Deprecated
    public boolean getBackgroundDataSetting() {
        return false;
    }

    @Deprecated
    public void setBackgroundDataSetting(boolean allowBackgroundData) {}

    @Deprecated
    public boolean getMobileDataEnabled() {
        return false;
    }

    public interface OnNetworkActiveListener {
        void onNetworkActive();
    }

    public void addDefaultNetworkActiveListener(final OnNetworkActiveListener l) {}

    public void removeDefaultNetworkActiveListener(@NonNull OnNetworkActiveListener l) {}

    public boolean isDefaultNetworkActive() {
        return false;
    }

    public ConnectivityManager(Context context, IConnectivityManager service) {
        sInstance = this;
    }

    public static ConnectivityManager from(Context context) {
        return new ConnectivityManager(context, null);
    }

    public NetworkRequest getDefaultRequest() {
        return null;
    }

    public static final void enforceChangePermission(Context context) {}

    @Deprecated
    static ConnectivityManager getInstanceOrNull() {
        return sInstance;
    }

    @Deprecated
    private static ConnectivityManager getInstance() {
        if (getInstanceOrNull() == null) {
            throw new IllegalStateException("No ConnectivityManager yet constructed");
        }
        return getInstanceOrNull();
    }

    @Deprecated
    public String[] getTetherableIfaces() {
        return null;
    }

    @Deprecated
    public String[] getTetheredIfaces() {
        return null;
    }

    @Deprecated
    public String[] getTetheringErroredIfaces() {
        return null;
    }

    @Deprecated
    public String[] getTetheredDhcpRanges() {
        throw new UnsupportedOperationException("getTetheredDhcpRanges is not supported");
    }

    @Deprecated
    public int tether(String iface) {
        return 0;
    }

    @Deprecated
    public int untether(String iface) {
        return 0;
    }

    public boolean isTetheringSupported() {
        return false;
    }

    @Deprecated
    public static abstract class OnStartTetheringCallback {
        public void onTetheringStarted() {}

        public void onTetheringFailed() {}
    }

    @Deprecated
    public void startTethering(int type, boolean showProvisioningUi,
            final OnStartTetheringCallback callback) {}

    @Deprecated
    public void startTethering(int type, boolean showProvisioningUi,
            final OnStartTetheringCallback callback, Handler handler) {}

    @Deprecated
    public void stopTethering(int type) {}

    @Deprecated
    public abstract static class OnTetheringEventCallback {

        public void onUpstreamChanged(@Nullable Network network) {}
    }

    @Deprecated
    public void registerTetheringEventCallback(
            @NonNull Executor executor,
            @NonNull final OnTetheringEventCallback callback) {}

    @Deprecated
    public void unregisterTetheringEventCallback(
            @NonNull final OnTetheringEventCallback callback) {}


    @Deprecated
    public String[] getTetherableUsbRegexs() {
        return null;
    }

    @Deprecated
    public String[] getTetherableWifiRegexs() {
        return null;
    }

    @Deprecated
    public String[] getTetherableBluetoothRegexs() {
        return null;
    }

    @Deprecated
    public int setUsbTethering(boolean enable) {
        return 0;
    }

    @Deprecated
    public static final int TETHER_ERROR_NO_ERROR = TetheringManager.TETHER_ERROR_NO_ERROR;
    @Deprecated
    public static final int TETHER_ERROR_UNKNOWN_IFACE =
            TetheringManager.TETHER_ERROR_UNKNOWN_IFACE;
    @Deprecated
    public static final int TETHER_ERROR_SERVICE_UNAVAIL =
            TetheringManager.TETHER_ERROR_SERVICE_UNAVAIL;
    @Deprecated
    public static final int TETHER_ERROR_UNSUPPORTED = TetheringManager.TETHER_ERROR_UNSUPPORTED;
    @Deprecated
    public static final int TETHER_ERROR_UNAVAIL_IFACE =
            TetheringManager.TETHER_ERROR_UNAVAIL_IFACE;
    @Deprecated
    public static final int TETHER_ERROR_MASTER_ERROR =
            TetheringManager.TETHER_ERROR_INTERNAL_ERROR;
    @Deprecated
    public static final int TETHER_ERROR_TETHER_IFACE_ERROR =
            TetheringManager.TETHER_ERROR_TETHER_IFACE_ERROR;
    @Deprecated
    public static final int TETHER_ERROR_UNTETHER_IFACE_ERROR =
            TetheringManager.TETHER_ERROR_UNTETHER_IFACE_ERROR;
    @Deprecated
    public static final int TETHER_ERROR_ENABLE_NAT_ERROR =
            TetheringManager.TETHER_ERROR_ENABLE_FORWARDING_ERROR;
    @Deprecated
    public static final int TETHER_ERROR_DISABLE_NAT_ERROR =
            TetheringManager.TETHER_ERROR_DISABLE_FORWARDING_ERROR;
    @Deprecated
    public static final int TETHER_ERROR_IFACE_CFG_ERROR =
            TetheringManager.TETHER_ERROR_IFACE_CFG_ERROR;
    @Deprecated
    public static final int TETHER_ERROR_PROVISION_FAILED =
            TetheringManager.TETHER_ERROR_PROVISIONING_FAILED;
    @Deprecated
    public static final int TETHER_ERROR_DHCPSERVER_ERROR =
            TetheringManager.TETHER_ERROR_DHCPSERVER_ERROR;
    @Deprecated
    public static final int TETHER_ERROR_ENTITLEMENT_UNKONWN =
            TetheringManager.TETHER_ERROR_ENTITLEMENT_UNKNOWN;

    @Deprecated
    public int getLastTetherError(String iface) {
        return 0;
    }

    public @interface EntitlementResultCode {
    }

    @Deprecated
    public interface OnTetheringEntitlementResultListener  {
        void onTetheringEntitlementResult(@EntitlementResultCode int resultCode);
    }

    @Deprecated
    public void getLatestTetheringEntitlementResult(int type, boolean showEntitlementUi,
            @NonNull Executor executor,
            @NonNull final OnTetheringEntitlementResultListener listener) {}

    public void reportInetCondition(int networkType, int percentage) {}

    @Deprecated
    public void reportBadNetwork(@Nullable Network network) {}

    public void reportNetworkConnectivity(@Nullable Network network, boolean hasConnectivity) {}

    public void setGlobalProxy(ProxyInfo p) {}

    public ProxyInfo getGlobalProxy() {
        return null;
    }

    public ProxyInfo getProxyForNetwork(Network network) {
        return null;
    }

    @Nullable
    public ProxyInfo getDefaultProxy() {
        return null;
    }

    @Deprecated
    public boolean isNetworkSupported(int networkType) {
        return false;
    }

    public boolean isActiveNetworkMetered() {
        return false;
    }

    public boolean updateLockdownVpn() {
        return false;
    }

    public int checkMobileProvisioning(int suggestedTimeOutMs) {
        return 0;
    }

    public String getMobileProvisioningUrl() {
        return null;
    }

    @Deprecated
    public void setProvisioningNotificationVisible(boolean visible, int networkType,
            String action) {}

    public void setAirplaneMode(boolean enable) {}

    public int registerNetworkFactory(Messenger messenger, String name) {
        return 0;
    }

    public void unregisterNetworkFactory(Messenger messenger) {}

    public int registerNetworkProvider(@NonNull NetworkProvider provider) {
        return 0;
    }

    public void unregisterNetworkProvider(@NonNull NetworkProvider provider) {}

    public void declareNetworkRequestUnfulfillable(@NonNull NetworkRequest request) {}

    public Network registerNetworkAgent(Messenger messenger, NetworkInfo ni, LinkProperties lp,
            NetworkCapabilities nc, int score, NetworkAgentConfig config) {
        return null;
    }

    public Network registerNetworkAgent(Messenger messenger, NetworkInfo ni, LinkProperties lp,
            NetworkCapabilities nc, int score, NetworkAgentConfig config, int providerId) {
        return null;
    }

    public static class NetworkCallback {
        public void onPreCheck(@NonNull Network network) {}

        public void onAvailable(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities,
                @NonNull LinkProperties linkProperties, boolean blocked) {}

        public void onAvailable(@NonNull Network network) {}

        public void onLosing(@NonNull Network network, int maxMsToLive) {}

        public void onLost(@NonNull Network network) {}

        public void onUnavailable() {}

        public void onCapabilitiesChanged(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities) {}

        public void onLinkPropertiesChanged(@NonNull Network network,
                @NonNull LinkProperties linkProperties) {}

        public void onNetworkSuspended(@NonNull Network network) {}

        public void onNetworkResumed(@NonNull Network network) {}

        public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {}
    }

    public interface Errors {
        int TOO_MANY_REQUESTS = 1;
    }

    public static class TooManyRequestsException extends RuntimeException {}

    public static final int CALLBACK_PRECHECK            = BASE + 1;
    public static final int CALLBACK_AVAILABLE           = BASE + 2;
    public static final int CALLBACK_LOSING              = BASE + 3;
    public static final int CALLBACK_LOST                = BASE + 4;
    public static final int CALLBACK_UNAVAIL             = BASE + 5;
    public static final int CALLBACK_CAP_CHANGED         = BASE + 6;
    public static final int CALLBACK_IP_CHANGED          = BASE + 7;
    private static final int EXPIRE_LEGACY_REQUEST       = BASE + 8;
    public static final int CALLBACK_SUSPENDED           = BASE + 9;
    public static final int CALLBACK_RESUMED             = BASE + 10;
    public static final int CALLBACK_BLK_CHANGED         = BASE + 11;

    public static String getCallbackName(int whichCallback) {
        switch (whichCallback) {
            case CALLBACK_PRECHECK:     return "CALLBACK_PRECHECK";
            case CALLBACK_AVAILABLE:    return "CALLBACK_AVAILABLE";
            case CALLBACK_LOSING:       return "CALLBACK_LOSING";
            case CALLBACK_LOST:         return "CALLBACK_LOST";
            case CALLBACK_UNAVAIL:      return "CALLBACK_UNAVAIL";
            case CALLBACK_CAP_CHANGED:  return "CALLBACK_CAP_CHANGED";
            case CALLBACK_IP_CHANGED:   return "CALLBACK_IP_CHANGED";
            case EXPIRE_LEGACY_REQUEST: return "EXPIRE_LEGACY_REQUEST";
            case CALLBACK_SUSPENDED:    return "CALLBACK_SUSPENDED";
            case CALLBACK_RESUMED:      return "CALLBACK_RESUMED";
            case CALLBACK_BLK_CHANGED:  return "CALLBACK_BLK_CHANGED";
            default:
                return Integer.toString(whichCallback);
        }
    }

    public void requestNetwork(@NonNull NetworkRequest request,
            int timeoutMs, int legacyType, @NonNull Handler handler,
            @NonNull NetworkCallback networkCallback) {}

    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback) {}

    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, @NonNull Handler handler) {}

    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, int timeoutMs) {}

    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, @NonNull Handler handler, int timeoutMs) {}

    public static final String EXTRA_NETWORK = "android.net.extra.NETWORK";

    public static final String EXTRA_NETWORK_REQUEST = "android.net.extra.NETWORK_REQUEST";


    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull PendingIntent operation) {}

    public void releaseNetworkRequest(@NonNull PendingIntent operation) {}

    public void registerNetworkCallback(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback) {}

    public void registerNetworkCallback(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, @NonNull Handler handler) {}

    public void registerNetworkCallback(@NonNull NetworkRequest request,
            @NonNull PendingIntent operation) {}

    public void registerDefaultNetworkCallback(@NonNull NetworkCallback networkCallback) {}

    public void registerDefaultNetworkCallback(@NonNull NetworkCallback networkCallback,
            @NonNull Handler handler) {}

    public boolean requestBandwidthUpdate(@NonNull Network network) {
        return false;
    }

    public void unregisterNetworkCallback(@NonNull NetworkCallback networkCallback) {}

    public void unregisterNetworkCallback(@NonNull PendingIntent operation) {}

    public void setAcceptUnvalidated(Network network, boolean accept, boolean always) {}

    public void setAcceptPartialConnectivity(Network network, boolean accept, boolean always) {}

    public void setAvoidUnvalidated(Network network) {}

    public void startCaptivePortalApp(Network network) {}

    public void startCaptivePortalApp(@NonNull Network network, @NonNull Bundle appExtras) {}

    public boolean shouldAvoidBadWifi() {
        return false;
    }

    public static final int MULTIPATH_PREFERENCE_HANDOVER = 1 << 0;

    public static final int MULTIPATH_PREFERENCE_RELIABILITY = 1 << 1;

    public static final int MULTIPATH_PREFERENCE_PERFORMANCE = 1 << 2;

    public static final int MULTIPATH_PREFERENCE_UNMETERED =
            MULTIPATH_PREFERENCE_HANDOVER |
                    MULTIPATH_PREFERENCE_RELIABILITY |
                    MULTIPATH_PREFERENCE_PERFORMANCE;

    public @interface MultipathPreference {
    }

    public @MultipathPreference int getMultipathPreference(@Nullable Network network) {
        return 0;
    }

    public void factoryReset() {}

    public boolean bindProcessToNetwork(@Nullable Network network) {
        return false;
    }

    public static boolean setProcessDefaultNetwork(@Nullable Network network) {
        return false;
    }

    @Nullable
    public Network getBoundNetworkForProcess() {
        return null;
    }

    @Deprecated
    @Nullable
    public static Network getProcessDefaultNetwork() {
        return null;
    }

    @Deprecated
    public static boolean setProcessDefaultNetworkForHostResolution(Network network) {
        return false;
    }

    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;

    public static final int RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2;

    public static final int RESTRICT_BACKGROUND_STATUS_ENABLED = 3;

    public static final String ACTION_RESTRICT_BACKGROUND_CHANGED =
            "android.net.conn.RESTRICT_BACKGROUND_CHANGED";

    public @interface RestrictBackgroundStatus {
    }

    public @RestrictBackgroundStatus int getRestrictBackgroundStatus() {
        return 0;
    }

    @Nullable
    public byte[] getNetworkWatchlistConfigHash() {
        return null;
    }

    public int getConnectionOwnerUid(
            int protocol, @NonNull InetSocketAddress local, @NonNull InetSocketAddress remote) {
        return 0;
    }

    public void simulateDataStall(int detectionMethod, long timestampMillis,
            @NonNull Network network, @NonNull PersistableBundle extras) {}
}

