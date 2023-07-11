/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.wifitrackerlib;

import static android.net.wifi.WifiInfo.INVALID_RSSI;

import static androidx.core.util.Preconditions.checkNotNull;

import static com.android.wifitrackerlib.Utils.getNetworkPart;
import static com.android.wifitrackerlib.Utils.getSingleSecurityTypeFromMultipleSecurityTypes;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

import androidx.annotation.AnyThread;
import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Base class for an entry representing a Wi-Fi network in a Wi-Fi picker/settings.
 * Subclasses should override the default methods for their own needs.
 *
 * Clients implementing a Wi-Fi picker/settings should receive WifiEntry objects from classes
 * implementing BaseWifiTracker, and rely on the given API for all user-displayable information and
 * actions on the represented network.
 */
public class WifiEntry {
    /**
     * Security type based on WifiConfiguration.KeyMgmt
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            SECURITY_NONE,
            SECURITY_OWE,
            SECURITY_WEP,
            SECURITY_PSK,
            SECURITY_SAE,
            SECURITY_EAP,
            SECURITY_EAP_SUITE_B,
            SECURITY_EAP_WPA3_ENTERPRISE,
    })

    public @interface Security {}

    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;
    public static final int SECURITY_OWE = 4;
    public static final int SECURITY_SAE = 5;
    public static final int SECURITY_EAP_SUITE_B = 6;
    public static final int SECURITY_EAP_WPA3_ENTERPRISE = 7;

    public static final int NUM_SECURITY_TYPES = 8;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            CONNECTED_STATE_DISCONNECTED,
            CONNECTED_STATE_CONNECTED,
            CONNECTED_STATE_CONNECTING
    })

    public @interface ConnectedState {}

    public static final int CONNECTED_STATE_DISCONNECTED = 0;
    public static final int CONNECTED_STATE_CONNECTING = 1;
    public static final int CONNECTED_STATE_CONNECTED = 2;

    // Wi-Fi signal levels for displaying signal strength.
    public static final int WIFI_LEVEL_MIN = 0;
    public static final int WIFI_LEVEL_MAX = 4;
    public static final int WIFI_LEVEL_UNREACHABLE = -1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            METERED_CHOICE_AUTO,
            METERED_CHOICE_METERED,
            METERED_CHOICE_UNMETERED,
    })

    public @interface MeteredChoice {}

    // User's choice whether to treat a network as metered.
    public static final int METERED_CHOICE_AUTO = 0;
    public static final int METERED_CHOICE_METERED = 1;
    public static final int METERED_CHOICE_UNMETERED = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            PRIVACY_DEVICE_MAC,
            PRIVACY_RANDOMIZED_MAC,
            PRIVACY_UNKNOWN
    })

    public @interface Privacy {}

    public static final int PRIVACY_DEVICE_MAC = 0;
    public static final int PRIVACY_RANDOMIZED_MAC = 1;
    public static final int PRIVACY_UNKNOWN = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            FREQUENCY_2_4_GHZ,
            FREQUENCY_5_GHZ,
            FREQUENCY_6_GHZ,
            FREQUENCY_60_GHZ,
            FREQUENCY_UNKNOWN
    })

    public @interface Frequency {}

    public static final int FREQUENCY_2_4_GHZ = 2_400;
    public static final int FREQUENCY_5_GHZ = 5_000;
    public static final int FREQUENCY_6_GHZ = 6_000;
    public static final int FREQUENCY_60_GHZ = 60_000;
    public static final int FREQUENCY_UNKNOWN = -1;

    /**
     * Min bound on the 2.4 GHz (802.11b/g/n) WLAN channels.
     */
    public static final int MIN_FREQ_24GHZ = 2400;

    /**
     * Max bound on the 2.4 GHz (802.11b/g/n) WLAN channels.
     */
    public static final int MAX_FREQ_24GHZ = 2500;

    /**
     * Min bound on the 5.0 GHz (802.11a/h/j/n/ac) WLAN channels.
     */
    public static final int MIN_FREQ_5GHZ = 4900;

    /**
     * Max bound on the 5.0 GHz (802.11a/h/j/n/ac) WLAN channels.
     */
    public static final int MAX_FREQ_5GHZ = 5900;

    /**
     * Min bound on the 6.0 GHz (802.11ax) WLAN channels.
     */
    public static final int MIN_FREQ_6GHZ = 5925;

    /**
     * Max bound on the 6.0 GHz (802.11ax) WLAN channels.
     */
    public static final int MAX_FREQ_6GHZ = 7125;

    /**
     * Min bound on the 60 GHz (802.11ad) WLAN channels.
     */
    public static final int MIN_FREQ_60GHZ = 58320;

    /**
     * Max bound on the 60 GHz (802.11ad) WLAN channels.
     */
    public static final int MAX_FREQ_60GHZ = 70200;

    /**
     * Max ScanResult information displayed of Wi-Fi Verbose Logging.
     */
    protected static final int MAX_VERBOSE_LOG_DISPLAY_SCANRESULT_COUNT = 4;

    /**
     * Default comparator for sorting WifiEntries on a Wi-Fi picker list.
     */
    public static Comparator<WifiEntry> WIFI_PICKER_COMPARATOR =
            Comparator.comparing((WifiEntry entry) -> entry.getConnectedState()
                            != CONNECTED_STATE_CONNECTED)
                    .thenComparing((WifiEntry entry) -> !entry.canConnect())
                    .thenComparing((WifiEntry entry) -> !entry.isSubscription())
                    .thenComparing((WifiEntry entry) -> !entry.isSaved())
                    .thenComparing((WifiEntry entry) -> !entry.isSuggestion())
                    .thenComparing((WifiEntry entry) -> -entry.getLevel())
                    .thenComparing((WifiEntry entry) -> entry.getTitle());

    /**
     * Default comparator for sorting WifiEntries by title.
     */
    public static Comparator<WifiEntry> TITLE_COMPARATOR =
            Comparator.comparing((WifiEntry entry) -> entry.getTitle());

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    final boolean mForSavedNetworksPage;

    protected final WifiManager mWifiManager;

    // Callback associated with this WifiEntry. Subclasses should call its methods appropriately.
    private WifiEntryCallback mListener;
    protected final Handler mCallbackHandler;

    protected int mLevel = WIFI_LEVEL_UNREACHABLE;
    protected WifiInfo mWifiInfo;
    protected NetworkInfo mNetworkInfo;
    protected NetworkCapabilities mNetworkCapabilities;
    protected ConnectedInfo mConnectedInfo;

    protected ConnectCallback mConnectCallback;
    protected DisconnectCallback mDisconnectCallback;
    protected ForgetCallback mForgetCallback;

    protected boolean mCalledConnect = false;
    protected boolean mCalledDisconnect = false;

    private boolean mIsValidated;
    protected boolean mIsDefaultNetwork;
    protected boolean mIsLowQuality;

    private Optional<ManageSubscriptionAction> mManageSubscriptionAction = Optional.empty();

    public WifiEntry(@NonNull Handler callbackHandler, @NonNull WifiManager wifiManager,
            boolean forSavedNetworksPage) throws IllegalArgumentException {
        checkNotNull(callbackHandler, "Cannot construct with null handler!");
        checkNotNull(wifiManager, "Cannot construct with null WifiManager!");
        mCallbackHandler = callbackHandler;
        mForSavedNetworksPage = forSavedNetworksPage;
        mWifiManager = wifiManager;
    }

    // Info available for all WifiEntries //

    /** The unique key defining a WifiEntry */
    @NonNull
    public String getKey() {
        return "";
    };

    /** Returns connection state of the network defined by the CONNECTED_STATE constants */
    @ConnectedState
    public synchronized int getConnectedState() {
        if (mNetworkInfo == null) {
            return CONNECTED_STATE_DISCONNECTED;
        }

        switch (mNetworkInfo.getDetailedState()) {
            case SCANNING:
            case CONNECTING:
            case AUTHENTICATING:
            case OBTAINING_IPADDR:
            case VERIFYING_POOR_LINK:
            case CAPTIVE_PORTAL_CHECK:
                return CONNECTED_STATE_CONNECTING;
            case CONNECTED:
                return CONNECTED_STATE_CONNECTED;
            default:
                return CONNECTED_STATE_DISCONNECTED;
        }
    }


    /** Returns the display title. This is most commonly the SSID of a network. */
    @NonNull
    public String getTitle() {
        return "";
    }

    /** Returns the display summary, it's a concise summary. */
    @NonNull
    public String getSummary() {
        return getSummary(true /* concise */);
    }

    /** Returns the second summary, it's for additional information of the WifiEntry */
    @NonNull
    public CharSequence getSecondSummary() {
        return "";
    }

    /**
     * Returns the display summary.
     * @param concise Whether to show more information. e.g., verbose logging.
     */
    @NonNull
    public String getSummary(boolean concise) {
        return "";
    };

    /**
     * Returns the signal strength level within [WIFI_LEVEL_MIN, WIFI_LEVEL_MAX].
     * A value of WIFI_LEVEL_UNREACHABLE indicates an out of range network.
     */
    public int getLevel() {
        return mLevel;
    };

    /**
     * Returns whether the level icon for this network should show an X or not.
     */
    public boolean shouldShowXLevelIcon() {
        return getConnectedState() != CONNECTED_STATE_DISCONNECTED
                && (!mIsValidated || !mIsDefaultNetwork) && !canSignIn();
    }

    /**
     * Returns whether this network has validated internet access or not.
     * Note: This does not necessarily mean the network is the default route.
     */
    public boolean hasInternetAccess() {
        return mIsValidated;
    }

    /**
     * Returns whether this network is the default network or not (i.e. this network is the one
     * currently being used to provide internet connection).
     */
    public boolean isDefaultNetwork() {
        return mIsDefaultNetwork;
    }

    /**
     * Returns the SSID of the entry, if applicable. Null otherwise.
     */
    @Nullable
    public String getSsid() {
        return null;
    }

    /**
     * Returns the security type defined by the SECURITY constants
     * DEPRECATED: Use getSecurityTypes() which can return multiple security types.
     */
    // TODO(b/187554920): Remove this and move all clients to getSecurityTypes()
    @Security
    public int getSecurity() {
        switch (getSingleSecurityTypeFromMultipleSecurityTypes(getSecurityTypes())) {
            case WifiInfo.SECURITY_TYPE_OPEN:
                return SECURITY_NONE;
            case WifiInfo.SECURITY_TYPE_OWE:
                return SECURITY_OWE;
            case WifiInfo.SECURITY_TYPE_WEP:
                return SECURITY_WEP;
            case WifiInfo.SECURITY_TYPE_PSK:
                return SECURITY_PSK;
            case WifiInfo.SECURITY_TYPE_SAE:
                return SECURITY_SAE;
            case WifiInfo.SECURITY_TYPE_EAP:
                return SECURITY_EAP;
            case WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE:
                return SECURITY_EAP_WPA3_ENTERPRISE;
            case WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT:
                return SECURITY_EAP_SUITE_B;
            case WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2:
            case WifiInfo.SECURITY_TYPE_PASSPOINT_R3:
                return SECURITY_EAP;
            default:
                return SECURITY_NONE;
        }
    }

    /**
     * Returns security type of the current connection, or the available types for connection
     * in the form of the SECURITY_TYPE_* values in {@link WifiInfo}
     */
    @NonNull
    public List<Integer> getSecurityTypes() {
        return Collections.emptyList();
    }

    /** Returns the MAC address of the connection */
    @Nullable
    public String getMacAddress() {
        return null;
    }

    /**
     * Indicates when a network is metered or the user marked the network as metered.
     */
    public boolean isMetered() {
        return false;
    }

    /**
     * Indicates whether or not an entry is for a saved configuration.
     */
    public boolean isSaved() {
        return false;
    }

    /**
     * Indicates whether or not an entry is for a saved configuration.
     */
    public boolean isSuggestion() {
        return false;
    }

    /**
     * Indicates whether or not an entry is for a subscription.
     */
    public boolean isSubscription() {
        return false;
    }

    /**
     * Returns the WifiConfiguration of an entry or null if unavailable. This should be used when
     * information on the WifiConfiguration needs to be modified and saved via
     * {@link WifiManager#save(WifiConfiguration, WifiManager.ActionListener)}.
     */
    @Nullable
    public WifiConfiguration getWifiConfiguration() {
        return null;
    }

    /**
     * Returns the ConnectedInfo object pertaining to an active connection.
     *
     * Returns null if getConnectedState() != CONNECTED_STATE_CONNECTED.
     */
    @Nullable
    public synchronized ConnectedInfo getConnectedInfo() {
        if (getConnectedState() != CONNECTED_STATE_CONNECTED) {
            return null;
        }

        return new ConnectedInfo(mConnectedInfo);
    }

    /**
     * Info associated with the active connection.
     */
    public static class ConnectedInfo {
        @Frequency
        public int frequencyMhz;
        public List<String> dnsServers = new ArrayList<>();
        public int linkSpeedMbps;
        public String ipAddress;
        public List<String> ipv6Addresses = new ArrayList<>();
        public String gateway;
        public String subnetMask;
        public int wifiStandard = ScanResult.WIFI_STANDARD_UNKNOWN;
        public NetworkCapabilities networkCapabilities;

        /**
         * Creates an empty ConnectedInfo
         */
        public ConnectedInfo() {
        }

        /**
         * Creates a ConnectedInfo with all fields copied from an input ConnectedInfo
         */
        public ConnectedInfo(@NonNull ConnectedInfo other) {
            frequencyMhz = other.frequencyMhz;
            dnsServers = new ArrayList<>(dnsServers);
            linkSpeedMbps = other.linkSpeedMbps;
            ipAddress = other.ipAddress;
            ipv6Addresses = new ArrayList<>(other.ipv6Addresses);
            gateway = other.gateway;
            subnetMask = other.subnetMask;
            wifiStandard = other.wifiStandard;
            networkCapabilities = other.networkCapabilities;
        }
    }

    // User actions on a network

    /** Returns whether the entry should show a connect option */
    public boolean canConnect() {
        return false;
    }

    /** Connects to the network */
    public void connect(@Nullable ConnectCallback callback) {
        // Do nothing.
    }

    /** Returns whether the entry should show a disconnect option */
    public boolean canDisconnect() {
        return false;
    }

    /** Disconnects from the network */
    public void disconnect(@Nullable DisconnectCallback callback) {
        // Do nothing.
    }

    /** Returns whether the entry should show a forget option */
    public boolean canForget() {
        return false;
    }

    /** Forgets the network */
    public void forget(@Nullable ForgetCallback callback) {
        // Do nothing.
    }

    /** Returns whether the network can be signed-in to */
    public boolean canSignIn() {
        return false;
    }

    /** Sign-in to the network. For captive portals. */
    public void signIn(@Nullable SignInCallback callback) {
        // Do nothing.
    }

    /** Returns whether the network can be shared via QR code */
    public boolean canShare() {
        return false;
    }

    /** Returns whether the user can use Easy Connect to onboard a device to the network */
    public boolean canEasyConnect() {
        return false;
    }

    // Modifiable settings

    /**
     *  Returns the user's choice whether to treat a network as metered,
     *  defined by the METERED_CHOICE constants
     */
    @MeteredChoice
    public int getMeteredChoice() {
        return METERED_CHOICE_AUTO;
    }

    /** Returns whether the entry should let the user choose the metered treatment of a network */
    public boolean canSetMeteredChoice() {
        return false;
    }

    /**
     * Sets the user's choice for treating a network as metered,
     * defined by the METERED_CHOICE constants
     */
    public void setMeteredChoice(@MeteredChoice int meteredChoice) {
        // Do nothing.
    }

    /** Returns whether the entry should let the user choose the MAC randomization setting */
    public boolean canSetPrivacy() {
        return false;
    }

    /** Returns the MAC randomization setting defined by the PRIVACY constants */
    @Privacy
    public int getPrivacy() {
        return PRIVACY_UNKNOWN;
    }

    /** Sets the user's choice for MAC randomization defined by the PRIVACY constants */
    public void setPrivacy(@Privacy int privacy) {
        // Do nothing.
    }

    /** Returns whether the network has auto-join enabled */
    public boolean isAutoJoinEnabled() {
        return false;
    }

    /** Returns whether the user can enable/disable auto-join */
    public boolean canSetAutoJoinEnabled() {
        return false;
    }

    /** Sets whether a network will be auto-joined or not */
    public void setAutoJoinEnabled(boolean enabled) {
        // Do nothing.
    }

    /** Returns the string displayed for @Security */
    public String getSecurityString(boolean concise) {
        return "";
    }

    /** Returns the string displayed for the Wi-Fi standard */
    public String getStandardString() {
        return "";
    }

    /** Returns whether subscription of the entry is expired */
    public boolean isExpired() {
        return false;
    }


    /** Returns whether a user can manage their subscription through this WifiEntry */
    public boolean canManageSubscription() {
        return mManageSubscriptionAction.isPresent();
    };

    /**
     * Return the URI string value of help, if it is not null, WifiPicker may show
     * help icon and route the user to help page specified by the URI string.
     * see {@link Intent#parseUri}
     */
    @Nullable
    public String getHelpUriString() {
        return null;
    }

    /** Allows the user to manage their subscription via an external flow */
    public void manageSubscription() {
        mManageSubscriptionAction.ifPresent(ManageSubscriptionAction::onExecute);
    };

    /** Set the action to be called on calling WifiEntry#manageSubscription. */
    public void setManageSubscriptionAction(
            @NonNull ManageSubscriptionAction manageSubscriptionAction) {
        // only notify update on 1st time
        boolean notify = !mManageSubscriptionAction.isPresent();

        mManageSubscriptionAction = Optional.of(manageSubscriptionAction);
        if (notify) {
            notifyOnUpdated();
        }
    }

    /** Returns the ScanResult information of a WifiEntry */
    @NonNull
    protected String getScanResultDescription() {
        return "";
    }

    /** Returns the network selection information of a WifiEntry */
    @NonNull
    String getNetworkSelectionDescription() {
        return "";
    }

    /** Returns the network capability information of a WifiEntry */
    @NonNull
    String getNetworkCapabilityDescription() {
        final StringBuilder sb = new StringBuilder();
        if (getConnectedState() == CONNECTED_STATE_CONNECTED) {
            sb.append("isValidated:")
                    .append(mIsValidated)
                    .append(", isDefaultNetwork:")
                    .append(mIsDefaultNetwork)
                    .append(", isLowQuality:")
                    .append(mIsLowQuality);
        }
        return sb.toString();
    }

    /**
     * In Wi-Fi picker, when users click a saved network, it will connect to the Wi-Fi network.
     * However, for some special cases, Wi-Fi picker should show Wi-Fi editor UI for users to edit
     * security or password before connecting. Or users will always get connection fail results.
     */
    public boolean shouldEditBeforeConnect() {
        return false;
    }

    /**
     * Sets the callback listener for WifiEntryCallback methods.
     * Subsequent calls will overwrite the previous listener.
     */
    public synchronized void setListener(WifiEntryCallback listener) {
        mListener = listener;
    }

    /**
     * Listener for changes to the state of the WifiEntry.
     * This callback will be invoked on the main thread.
     */
    public interface WifiEntryCallback {
        /**
         * Indicates the state of the WifiEntry has changed and clients may retrieve updates through
         * the WifiEntry getter methods.
         */
        @MainThread
        void onUpdated();
    }

    @AnyThread
    protected void notifyOnUpdated() {
        if (mListener != null) {
            mCallbackHandler.post(() -> {
                final WifiEntryCallback listener = mListener;
                if (listener != null) {
                    listener.onUpdated();
                }
            });
        }
    }

    /**
     * Listener for changes to the state of the WifiEntry.
     * This callback will be invoked on the main thread.
     */
    public interface ConnectCallback {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                CONNECT_STATUS_SUCCESS,
                CONNECT_STATUS_FAILURE_NO_CONFIG,
                CONNECT_STATUS_FAILURE_UNKNOWN,
                CONNECT_STATUS_FAILURE_SIM_ABSENT
        })

        public @interface ConnectStatus {}

        int CONNECT_STATUS_SUCCESS = 0;
        int CONNECT_STATUS_FAILURE_NO_CONFIG = 1;
        int CONNECT_STATUS_FAILURE_UNKNOWN = 2;
        int CONNECT_STATUS_FAILURE_SIM_ABSENT = 3;

        /**
         * Result of the connect request indicated by the CONNECT_STATUS constants.
         */
        @MainThread
        void onConnectResult(@ConnectStatus int status);
    }

    /**
     * Listener for changes to the state of the WifiEntry.
     * This callback will be invoked on the main thread.
     */
    public interface DisconnectCallback {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                DISCONNECT_STATUS_SUCCESS,
                DISCONNECT_STATUS_FAILURE_UNKNOWN
        })

        public @interface DisconnectStatus {}

        int DISCONNECT_STATUS_SUCCESS = 0;
        int DISCONNECT_STATUS_FAILURE_UNKNOWN = 1;
        /**
         * Result of the disconnect request indicated by the DISCONNECT_STATUS constants.
         */
        @MainThread
        void onDisconnectResult(@DisconnectStatus int status);
    }

    /**
     * Listener for changes to the state of the WifiEntry.
     * This callback will be invoked on the main thread.
     */
    public interface ForgetCallback {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                FORGET_STATUS_SUCCESS,
                FORGET_STATUS_FAILURE_UNKNOWN
        })

        public @interface ForgetStatus {}

        int FORGET_STATUS_SUCCESS = 0;
        int FORGET_STATUS_FAILURE_UNKNOWN = 1;

        /**
         * Result of the forget request indicated by the FORGET_STATUS constants.
         */
        @MainThread
        void onForgetResult(@ForgetStatus int status);
    }

    /**
     * Listener for changes to the state of the WifiEntry.
     * This callback will be invoked on the main thread.
     */
    public interface SignInCallback {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                SIGNIN_STATUS_SUCCESS,
                SIGNIN_STATUS_FAILURE_UNKNOWN
        })

        public @interface SignInStatus {}

        int SIGNIN_STATUS_SUCCESS = 0;
        int SIGNIN_STATUS_FAILURE_UNKNOWN = 1;

        /**
         * Result of the sign-in request indicated by the SIGNIN_STATUS constants.
         */
        @MainThread
        void onSignInResult(@SignInStatus int status);
    }

    /**
     * Returns whether or not the supplied WifiInfo and NetworkInfo represent this WifiEntry
     */
    protected boolean connectionInfoMatches(@NonNull WifiInfo wifiInfo,
            @NonNull NetworkInfo networkInfo) {
        return false;
    }

    /**
     * Updates information regarding the current network connection. If the supplied WifiInfo and
     * NetworkInfo do not match this WifiEntry, then the WifiEntry will update to be
     * unconnected.
     */
    @WorkerThread
    synchronized void updateConnectionInfo(
            @Nullable WifiInfo wifiInfo, @Nullable NetworkInfo networkInfo) {
        if (wifiInfo != null && networkInfo != null
                && connectionInfoMatches(wifiInfo, networkInfo)) {
            // Connection info matches, so the WifiInfo/NetworkInfo represent this network and
            // the network is currently connecting or connected.
            mWifiInfo = wifiInfo;
            mNetworkInfo = networkInfo;
            final int wifiInfoRssi = wifiInfo.getRssi();
            if (wifiInfoRssi != INVALID_RSSI) {
                mLevel = mWifiManager.calculateSignalLevel(wifiInfoRssi);
            }
            if (getConnectedState() == CONNECTED_STATE_CONNECTED) {
                if (mCalledConnect) {
                    mCalledConnect = false;
                    mCallbackHandler.post(() -> {
                        final ConnectCallback connectCallback = mConnectCallback;
                        if (connectCallback != null) {
                            connectCallback.onConnectResult(
                                    ConnectCallback.CONNECT_STATUS_SUCCESS);
                        }
                    });
                }

                if (mConnectedInfo == null) {
                    mConnectedInfo = new ConnectedInfo();
                }
                mConnectedInfo.frequencyMhz = wifiInfo.getFrequency();
                mConnectedInfo.linkSpeedMbps = wifiInfo.getLinkSpeed();
                mConnectedInfo.wifiStandard = wifiInfo.getWifiStandard();
            }
        } else { // Connection info doesn't matched, so this network is disconnected
            mWifiInfo = null;
            mNetworkInfo = null;
            mNetworkCapabilities = null;
            mConnectedInfo = null;
            mIsValidated = false;
            mIsDefaultNetwork = false;
            mIsLowQuality = false;
            if (mCalledDisconnect) {
                mCalledDisconnect = false;
                mCallbackHandler.post(() -> {
                    final DisconnectCallback disconnectCallback = mDisconnectCallback;
                    if (disconnectCallback != null) {
                        disconnectCallback.onDisconnectResult(
                                DisconnectCallback.DISCONNECT_STATUS_SUCCESS);
                    }
                });
            }
        }
        updateSecurityTypes();
        notifyOnUpdated();
    }

    // Called to indicate the security types should be updated to match new information about the
    // network.
    protected void updateSecurityTypes() {
        // Do nothing;
    }

    // Method for WifiTracker to update the link properties, which is valid for all WifiEntry types.
    @WorkerThread
    synchronized void updateLinkProperties(@Nullable LinkProperties linkProperties) {
        if (linkProperties == null || getConnectedState() != CONNECTED_STATE_CONNECTED) {
            mConnectedInfo = null;
            notifyOnUpdated();
            return;
        }

        if (mConnectedInfo == null) {
            mConnectedInfo = new ConnectedInfo();
        }
        // Find IPv4 and IPv6 addresses, and subnet mask
        List<String> ipv6Addresses = new ArrayList<>();
        for (LinkAddress addr : linkProperties.getLinkAddresses()) {
            if (addr.getAddress() instanceof Inet4Address) {
                mConnectedInfo.ipAddress = addr.getAddress().getHostAddress();
                try {
                    InetAddress all = InetAddress.getByAddress(
                            new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255});
                    mConnectedInfo.subnetMask = getNetworkPart(
                            all, addr.getPrefixLength()).getHostAddress();
                } catch (UnknownHostException e) {
                    // Leave subnet null;
                }
            } else if (addr.getAddress() instanceof Inet6Address) {
                ipv6Addresses.add(addr.getAddress().getHostAddress());
            }
        }
        mConnectedInfo.ipv6Addresses = ipv6Addresses;

        // Find IPv4 default gateway.
        for (RouteInfo routeInfo : linkProperties.getRoutes()) {
            if (routeInfo.isDefaultRoute() && routeInfo.getDestination().getAddress()
                    instanceof Inet4Address && routeInfo.hasGateway()) {
                mConnectedInfo.gateway = routeInfo.getGateway().getHostAddress();
                break;
            }
        }

        // Find DNS servers
        mConnectedInfo.dnsServers = linkProperties.getDnsServers().stream()
                .map(InetAddress::getHostAddress).collect(Collectors.toList());

        notifyOnUpdated();
    }

    @WorkerThread
    synchronized void setIsDefaultNetwork(boolean isDefaultNetwork) {
        mIsDefaultNetwork = isDefaultNetwork;
        notifyOnUpdated();
    }

    @WorkerThread
    synchronized void setIsLowQuality(boolean isLowQuality) {
        mIsLowQuality = isLowQuality;
    }

    // Method for WifiTracker to update a connected WifiEntry's network capabilities.
    @WorkerThread
    synchronized void updateNetworkCapabilities(@Nullable NetworkCapabilities capabilities) {
        mNetworkCapabilities = capabilities;
        if (mConnectedInfo == null) {
            return;
        }
        mConnectedInfo.networkCapabilities = mNetworkCapabilities;
        mIsValidated = mNetworkCapabilities != null
                && mNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        notifyOnUpdated();
    }

    synchronized String getWifiInfoDescription() {
        final StringJoiner sj = new StringJoiner(" ");
        if (getConnectedState() == CONNECTED_STATE_CONNECTED && mWifiInfo != null) {
            sj.add("f = " + mWifiInfo.getFrequency());
            final String bssid = mWifiInfo.getBSSID();
            if (bssid != null) {
                sj.add(bssid);
            }
            sj.add("standard = " + getStandardString());
            sj.add("rssi = " + mWifiInfo.getRssi());
            sj.add("score = " + mWifiInfo.getScore());
            sj.add(String.format(" tx=%.1f,", mWifiInfo.getSuccessfulTxPacketsPerSecond()));
            sj.add(String.format("%.1f,", mWifiInfo.getRetriedTxPacketsPerSecond()));
            sj.add(String.format("%.1f ", mWifiInfo.getLostTxPacketsPerSecond()));
            sj.add(String.format("rx=%.1f", mWifiInfo.getSuccessfulRxPacketsPerSecond()));
        }
        return sj.toString();
    }

    protected class ConnectActionListener implements WifiManager.ActionListener {
        @Override
        public void onSuccess() {
            synchronized (WifiEntry.this) {
                mCalledConnect = true;
            }
            // If we aren't connected to the network after 10 seconds, trigger the failure callback
            mCallbackHandler.postDelayed(() -> {
                final ConnectCallback connectCallback = mConnectCallback;
                if (connectCallback != null && mCalledConnect
                        && getConnectedState() == CONNECTED_STATE_DISCONNECTED) {
                    connectCallback.onConnectResult(
                            ConnectCallback.CONNECT_STATUS_FAILURE_UNKNOWN);
                    mCalledConnect = false;
                }
            }, 10_000 /* delayMillis */);
        }

        @Override
        public void onFailure(int i) {
            mCallbackHandler.post(() -> {
                final ConnectCallback connectCallback = mConnectCallback;
                if (connectCallback != null) {
                    connectCallback.onConnectResult(
                            ConnectCallback.CONNECT_STATUS_FAILURE_UNKNOWN);
                }
            });
        }
    }

    protected class ForgetActionListener implements WifiManager.ActionListener {
        @Override
        public void onSuccess() {
            mCallbackHandler.post(() -> {
                final ForgetCallback forgetCallback = mForgetCallback;
                if (forgetCallback != null) {
                    forgetCallback.onForgetResult(ForgetCallback.FORGET_STATUS_SUCCESS);
                }
            });
        }

        @Override
        public void onFailure(int i) {
            mCallbackHandler.post(() -> {
                final ForgetCallback forgetCallback = mForgetCallback;
                if (forgetCallback != null) {
                    forgetCallback.onForgetResult(ForgetCallback.FORGET_STATUS_FAILURE_UNKNOWN);
                }
            });
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof WifiEntry)) return false;
        return getKey().equals(((WifiEntry) other).getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(getKey())
                .append(",title:")
                .append(getTitle())
                .append(",summary:")
                .append(getSummary())
                .append(",isSaved:")
                .append(isSaved())
                .append(",isSubscription:")
                .append(isSubscription())
                .append(",isSuggestion:")
                .append(isSuggestion())
                .append(",level:")
                .append(getLevel())
                .append(shouldShowXLevelIcon() ? "X" : "")
                .append(",security:")
                .append(getSecurityTypes())
                .append(",connected:")
                .append(getConnectedState() == CONNECTED_STATE_CONNECTED ? "true" : "false")
                .append(",connectedInfo:")
                .append(getConnectedInfo())
                .append(",isValidated:")
                .append(mIsValidated)
                .append(",isDefaultNetwork:")
                .append(mIsDefaultNetwork)
                .toString();
    }

    /**
     * The action used to execute the calling of WifiEntry#manageSubscription.
     */
    public interface ManageSubscriptionAction {
        /**
         * Execute the action of managing subscription.
         */
        void onExecute();
    }
}
