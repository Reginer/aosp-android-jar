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
package android.net;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;
import static android.net.NetworkCapabilities.NET_ENTERPRISE_ID_1;
import static android.net.NetworkRequest.Type.BACKGROUND_REQUEST;
import static android.net.NetworkRequest.Type.LISTEN;
import static android.net.NetworkRequest.Type.LISTEN_FOR_BEST;
import static android.net.NetworkRequest.Type.REQUEST;
import static android.net.NetworkRequest.Type.RESERVATION;
import static android.net.NetworkRequest.Type.TRACK_DEFAULT;
import static android.net.NetworkRequest.Type.TRACK_SYSTEM_DEFAULT;
import static android.net.QosCallback.QosCallbackRegistrationException;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.LongDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityDiagnosticsManager.DataStallReport.DetectionMethod;
import android.net.IpSecManager.UdpEncapsulationSocket;
import android.net.SocketKeepalive.Callback;
import android.net.TetheringManager.StartTetheringCallback;
import android.net.TetheringManager.TetheringEventCallback;
import android.net.TetheringManager.TetheringRequest;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LruCache;
import android.util.Range;
import android.util.SparseIntArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.modules.utils.build.SdkLevel;
import com.android.net.flags.Flags;

import libcore.net.event.NetworkEventDispatcher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Class that answers queries about the state of network connectivity. It also
 * notifies applications when network connectivity changes.
 * <p>
 * The primary responsibilities of this class are to:
 * <ol>
 * <li>Monitor network connections (Wi-Fi, GPRS, UMTS, etc.)</li>
 * <li>Send broadcast intents when network connectivity changes</li>
 * <li>Attempt to "fail over" to another network when connectivity to a network
 * is lost</li>
 * <li>Provide an API that allows applications to query the coarse-grained or fine-grained
 * state of the available networks</li>
 * <li>Provide an API that allows applications to request and select networks for their data
 * traffic</li>
 * </ol>
 */
@SystemService(Context.CONNECTIVITY_SERVICE)
public class ConnectivityManager {
    private static final String TAG = "ConnectivityManager";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    /**
     * A change in network connectivity has occurred. A default connection has either
     * been established or lost. The NetworkInfo for the affected network is
     * sent as an extra; it should be consulted to see what kind of
     * connectivity event occurred.
     * <p/>
     * Apps targeting Android 7.0 (API level 24) and higher do not receive this
     * broadcast if they declare the broadcast receiver in their manifest. Apps
     * will still receive broadcasts if they register their
     * {@link android.content.BroadcastReceiver} with
     * {@link android.content.Context#registerReceiver Context.registerReceiver()}
     * and that context is still valid.
     * <p/>
     * If this is a connection that was the result of failing over from a
     * disconnected network, then the FAILOVER_CONNECTION boolean extra is
     * set to true.
     * <p/>
     * For a loss of connectivity, if the connectivity manager is attempting
     * to connect (or has already connected) to another network, the
     * NetworkInfo for the new network is also passed as an extra. This lets
     * any receivers of the broadcast know that they should not necessarily
     * tell the user that no data traffic will be possible. Instead, the
     * receiver should expect another broadcast soon, indicating either that
     * the failover attempt succeeded (and so there is still overall data
     * connectivity), or that the failover attempt failed, meaning that all
     * connectivity has been lost.
     * <p/>
     * For a disconnect event, the boolean extra EXTRA_NO_CONNECTIVITY
     * is set to {@code true} if there are no connected networks at all.
     * <p />
     * Note that this broadcast is deprecated and generally tries to implement backwards
     * compatibility with older versions of Android. As such, it may not reflect new
     * capabilities of the system, like multiple networks being connected at the same
     * time, the details of newer technology, or changes in tethering state.
     *
     * @deprecated apps should use the more versatile {@link #requestNetwork},
     *             {@link #registerNetworkCallback} or {@link #registerDefaultNetworkCallback}
     *             functions instead for faster and more detailed updates about the network
     *             changes they care about.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @Deprecated
    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    /**
     * The device has connected to a network that has presented a captive
     * portal, which is blocking Internet connectivity. The user was presented
     * with a notification that network sign in is required,
     * and the user invoked the notification's action indicating they
     * desire to sign in to the network. Apps handling this activity should
     * facilitate signing in to the network. This action includes a
     * {@link Network} typed extra called {@link #EXTRA_NETWORK} that represents
     * the network presenting the captive portal; all communication with the
     * captive portal must be done using this {@code Network} object.
     * <p/>
     * This activity includes a {@link CaptivePortal} extra named
     * {@link #EXTRA_CAPTIVE_PORTAL} that can be used to indicate different
     * outcomes of the captive portal sign in to the system:
     * <ul>
     * <li> When the app handling this action believes the user has signed in to
     * the network and the captive portal has been dismissed, the app should
     * call {@link CaptivePortal#reportCaptivePortalDismissed} so the system can
     * reevaluate the network. If reevaluation finds the network no longer
     * subject to a captive portal, the network may become the default active
     * data network.</li>
     * <li> When the app handling this action believes the user explicitly wants
     * to ignore the captive portal and the network, the app should call
     * {@link CaptivePortal#ignoreNetwork}. </li>
     * </ul>
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_CAPTIVE_PORTAL_SIGN_IN = "android.net.conn.CAPTIVE_PORTAL";

    /**
     * The lookup key for a {@link NetworkInfo} object. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     *
     * @deprecated The {@link NetworkInfo} object is deprecated, as many of its properties
     *             can't accurately represent modern network characteristics.
     *             Please obtain information about networks from the {@link NetworkCapabilities}
     *             or {@link LinkProperties} objects instead.
     */
    @Deprecated
    public static final String EXTRA_NETWORK_INFO = "networkInfo";

    /**
     * Network type which triggered a {@link #CONNECTIVITY_ACTION} broadcast.
     *
     * @see android.content.Intent#getIntExtra(String, int)
     * @deprecated The network type is not rich enough to represent the characteristics
     *             of modern networks. Please use {@link NetworkCapabilities} instead,
     *             in particular the transports.
     */
    @Deprecated
    public static final String EXTRA_NETWORK_TYPE = "networkType";

    /**
     * The lookup key for a boolean that indicates whether a connect event
     * is for a network to which the connectivity manager was failing over
     * following a disconnect on another network.
     * Retrieve it with {@link android.content.Intent#getBooleanExtra(String,boolean)}.
     *
     * @deprecated See {@link NetworkInfo}.
     */
    @Deprecated
    public static final String EXTRA_IS_FAILOVER = "isFailover";
    /**
     * The lookup key for a {@link NetworkInfo} object. This is supplied when
     * there is another network that it may be possible to connect to. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     *
     * @deprecated See {@link NetworkInfo}.
     */
    @Deprecated
    public static final String EXTRA_OTHER_NETWORK_INFO = "otherNetwork";
    /**
     * The lookup key for a boolean that indicates whether there is a
     * complete lack of connectivity, i.e., no network is available.
     * Retrieve it with {@link android.content.Intent#getBooleanExtra(String,boolean)}.
     */
    public static final String EXTRA_NO_CONNECTIVITY = "noConnectivity";
    /**
     * The lookup key for a string that indicates why an attempt to connect
     * to a network failed. The string has no particular structure. It is
     * intended to be used in notifications presented to users. Retrieve
     * it with {@link android.content.Intent#getStringExtra(String)}.
     */
    public static final String EXTRA_REASON = "reason";
    /**
     * The lookup key for a string that provides optionally supplied
     * extra information about the network state. The information
     * may be passed up from the lower networking layers, and its
     * meaning may be specific to a particular network type. Retrieve
     * it with {@link android.content.Intent#getStringExtra(String)}.
     *
     * @deprecated See {@link NetworkInfo#getExtraInfo()}.
     */
    @Deprecated
    public static final String EXTRA_EXTRA_INFO = "extraInfo";
    /**
     * The lookup key for an int that provides information about
     * our connection to the internet at large.  0 indicates no connection,
     * 100 indicates a great connection.  Retrieve it with
     * {@link android.content.Intent#getIntExtra(String, int)}.
     * {@hide}
     */
    public static final String EXTRA_INET_CONDITION = "inetCondition";
    /**
     * The lookup key for a {@link CaptivePortal} object included with the
     * {@link #ACTION_CAPTIVE_PORTAL_SIGN_IN} intent.  The {@code CaptivePortal}
     * object can be used to either indicate to the system that the captive
     * portal has been dismissed or that the user does not want to pursue
     * signing in to captive portal.  Retrieve it with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_CAPTIVE_PORTAL = "android.net.extra.CAPTIVE_PORTAL";

    /**
     * Key for passing a URL to the captive portal login activity.
     */
    public static final String EXTRA_CAPTIVE_PORTAL_URL = "android.net.extra.CAPTIVE_PORTAL_URL";

    /**
     * Key for passing a {@link android.net.captiveportal.CaptivePortalProbeSpec} to the captive
     * portal login activity.
     * {@hide}
     */
    @SystemApi
    public static final String EXTRA_CAPTIVE_PORTAL_PROBE_SPEC =
            "android.net.extra.CAPTIVE_PORTAL_PROBE_SPEC";

    /**
     * Key for passing a user agent string to the captive portal login activity.
     * {@hide}
     */
    @SystemApi
    public static final String EXTRA_CAPTIVE_PORTAL_USER_AGENT =
            "android.net.extra.CAPTIVE_PORTAL_USER_AGENT";

    /**
     * Broadcast action to indicate the change of data activity status
     * (idle or active) on a network in a recent period.
     * The network becomes active when data transmission is started, or
     * idle if there is no data transmission for a period of time.
     * {@hide}
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_DATA_ACTIVITY_CHANGE =
            "android.net.conn.DATA_ACTIVITY_CHANGE";
    /**
     * The lookup key for an enum that indicates the network device type on which this data activity
     * change happens.
     * {@hide}
     */
    public static final String EXTRA_DEVICE_TYPE = "deviceType";
    /**
     * The lookup key for a boolean that indicates the device is active or not. {@code true} means
     * it is actively sending or receiving data and {@code false} means it is idle.
     * {@hide}
     */
    public static final String EXTRA_IS_ACTIVE = "isActive";
    /**
     * The lookup key for a long that contains the timestamp (nanos) of the radio state change.
     * {@hide}
     */
    public static final String EXTRA_REALTIME_NS = "tsNanos";

    /**
     * Broadcast Action: The setting for background data usage has changed
     * values. Use {@link #getBackgroundDataSetting()} to get the current value.
     * <p>
     * If an application uses the network in the background, it should listen
     * for this broadcast and stop using the background data if the value is
     * {@code false}.
     * <p>
     *
     * @deprecated As of {@link VERSION_CODES#ICE_CREAM_SANDWICH}, availability
     *             of background data depends on several combined factors, and
     *             this broadcast is no longer sent. Instead, when background
     *             data is unavailable, {@link #getActiveNetworkInfo()} will now
     *             appear disconnected. During first boot after a platform
     *             upgrade, this broadcast will be sent once if
     *             {@link #getBackgroundDataSetting()} was {@code false} before
     *             the upgrade.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @Deprecated
    public static final String ACTION_BACKGROUND_DATA_SETTING_CHANGED =
            "android.net.conn.BACKGROUND_DATA_SETTING_CHANGED";

    /**
     * Broadcast Action: The network connection may not be good
     * uses {@code ConnectivityManager.EXTRA_INET_CONDITION} and
     * {@code ConnectivityManager.EXTRA_NETWORK_INFO} to specify
     * the network and it's condition.
     * @hide
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @UnsupportedAppUsage
    public static final String INET_CONDITION_ACTION =
            "android.net.conn.INET_CONDITION_ACTION";

    /**
     * Broadcast Action: A tetherable connection has come or gone.
     * Uses {@code ConnectivityManager.EXTRA_AVAILABLE_TETHER},
     * {@code ConnectivityManager.EXTRA_ACTIVE_LOCAL_ONLY},
     * {@code ConnectivityManager.EXTRA_ACTIVE_TETHER}, and
     * {@code ConnectivityManager.EXTRA_ERRORED_TETHER} to indicate
     * the current state of tethering.  Each include a list of
     * interface names in that state (may be empty).
     * @hide
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String ACTION_TETHER_STATE_CHANGED =
            TetheringManager.ACTION_TETHER_STATE_CHANGED;

    /**
     * @hide
     * gives a String[] listing all the interfaces configured for
     * tethering and currently available for tethering.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String EXTRA_AVAILABLE_TETHER = TetheringManager.EXTRA_AVAILABLE_TETHER;

    /**
     * @hide
     * gives a String[] listing all the interfaces currently in local-only
     * mode (ie, has DHCPv4+IPv6-ULA support and no packet forwarding)
     */
    public static final String EXTRA_ACTIVE_LOCAL_ONLY = TetheringManager.EXTRA_ACTIVE_LOCAL_ONLY;

    /**
     * @hide
     * gives a String[] listing all the interfaces currently tethered
     * (ie, has DHCPv4 support and packets potentially forwarded/NATed)
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String EXTRA_ACTIVE_TETHER = TetheringManager.EXTRA_ACTIVE_TETHER;

    /**
     * @hide
     * gives a String[] listing all the interfaces we tried to tether and
     * failed.  Use {@link #getLastTetherError} to find the error code
     * for any interfaces listed here.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String EXTRA_ERRORED_TETHER = TetheringManager.EXTRA_ERRORED_TETHER;

    /**
     * Broadcast Action: The captive portal tracker has finished its test.
     * Sent only while running Setup Wizard, in lieu of showing a user
     * notification.
     * @hide
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CAPTIVE_PORTAL_TEST_COMPLETED =
            "android.net.conn.CAPTIVE_PORTAL_TEST_COMPLETED";
    /**
     * The lookup key for a boolean that indicates whether a captive portal was detected.
     * Retrieve it with {@link android.content.Intent#getBooleanExtra(String,boolean)}.
     * @hide
     */
    public static final String EXTRA_IS_CAPTIVE_PORTAL = "captivePortal";

    /**
     * Action used to display a dialog that asks the user whether to connect to a network that is
     * not validated. This intent is used to start the dialog in settings via startActivity.
     *
     * This action includes a {@link Network} typed extra which is called
     * {@link ConnectivityManager#EXTRA_NETWORK} that represents the network which is unvalidated.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final String ACTION_PROMPT_UNVALIDATED = "android.net.action.PROMPT_UNVALIDATED";

    /**
     * Action used to display a dialog that asks the user whether to avoid a network that is no
     * longer validated. This intent is used to start the dialog in settings via startActivity.
     *
     * This action includes a {@link Network} typed extra which is called
     * {@link ConnectivityManager#EXTRA_NETWORK} that represents the network which is no longer
     * validated.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final String ACTION_PROMPT_LOST_VALIDATION =
            "android.net.action.PROMPT_LOST_VALIDATION";

    /**
     * Action used to display a dialog that asks the user whether to stay connected to a network
     * that has not validated. This intent is used to start the dialog in settings via
     * startActivity.
     *
     * This action includes a {@link Network} typed extra which is called
     * {@link ConnectivityManager#EXTRA_NETWORK} that represents the network which has partial
     * connectivity.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final String ACTION_PROMPT_PARTIAL_CONNECTIVITY =
            "android.net.action.PROMPT_PARTIAL_CONNECTIVITY";

    /**
     * Clear DNS Cache Action: This is broadcast when networks have changed and old
     * DNS entries should be cleared.
     * @hide
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String ACTION_CLEAR_DNS_CACHE = "android.net.action.CLEAR_DNS_CACHE";

    /**
     * Invalid tethering type.
     * @see #startTethering(int, boolean, OnStartTetheringCallback)
     * @hide
     */
    public static final int TETHERING_INVALID   = TetheringManager.TETHERING_INVALID;

    /**
     * Wifi tethering type.
     * @see #startTethering(int, boolean, OnStartTetheringCallback)
     * @hide
     */
    @SystemApi
    public static final int TETHERING_WIFI      = 0;

    /**
     * USB tethering type.
     * @see #startTethering(int, boolean, OnStartTetheringCallback)
     * @hide
     */
    @SystemApi
    public static final int TETHERING_USB       = 1;

    /**
     * Bluetooth tethering type.
     * @see #startTethering(int, boolean, OnStartTetheringCallback)
     * @hide
     */
    @SystemApi
    public static final int TETHERING_BLUETOOTH = 2;

    /**
     * Wifi P2p tethering type.
     * Wifi P2p tethering is set through events automatically, and don't
     * need to start from #startTethering(int, boolean, OnStartTetheringCallback).
     * @hide
     */
    public static final int TETHERING_WIFI_P2P = TetheringManager.TETHERING_WIFI_P2P;

    /**
     * Extra used for communicating with the TetherService. Includes the type of tethering to
     * enable if any.
     * @hide
     */
    public static final String EXTRA_ADD_TETHER_TYPE = TetheringConstants.EXTRA_ADD_TETHER_TYPE;

    /**
     * Extra used for communicating with the TetherService. Includes the type of tethering for
     * which to cancel provisioning.
     * @hide
     */
    public static final String EXTRA_REM_TETHER_TYPE = TetheringConstants.EXTRA_REM_TETHER_TYPE;

    /**
     * Extra used for communicating with the TetherService. True to schedule a recheck of tether
     * provisioning.
     * @hide
     */
    public static final String EXTRA_SET_ALARM = TetheringConstants.EXTRA_SET_ALARM;

    /**
     * Tells the TetherService to run a provision check now.
     * @hide
     */
    public static final String EXTRA_RUN_PROVISION = TetheringConstants.EXTRA_RUN_PROVISION;

    /**
     * Extra used for communicating with the TetherService. Contains the {@link ResultReceiver}
     * which will receive provisioning results. Can be left empty.
     * @hide
     */
    public static final String EXTRA_PROVISION_CALLBACK =
            TetheringConstants.EXTRA_PROVISION_CALLBACK;

    /**
     * The absence of a connection type.
     * @hide
     */
    @SystemApi
    public static final int TYPE_NONE        = -1;

    /**
     * A Mobile data connection. Devices may support more than one.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasTransport} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request an
     *         appropriate network. See {@link NetworkCapabilities} for supported transports.
     */
    @Deprecated
    public static final int TYPE_MOBILE      = 0;

    /**
     * A WIFI data connection. Devices may support more than one.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasTransport} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request an
     *         appropriate network. See {@link NetworkCapabilities} for supported transports.
     */
    @Deprecated
    public static final int TYPE_WIFI        = 1;

    /**
     * An MMS-specific Mobile data connection.  This network type may use the
     * same network interface as {@link #TYPE_MOBILE} or it may use a different
     * one.  This is used by applications needing to talk to the carrier's
     * Multimedia Messaging Service servers.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasCapability} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request a network that
     *         provides the {@link NetworkCapabilities#NET_CAPABILITY_MMS} capability.
     */
    @Deprecated
    public static final int TYPE_MOBILE_MMS  = 2;

    /**
     * A SUPL-specific Mobile data connection.  This network type may use the
     * same network interface as {@link #TYPE_MOBILE} or it may use a different
     * one.  This is used by applications needing to talk to the carrier's
     * Secure User Plane Location servers for help locating the device.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasCapability} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request a network that
     *         provides the {@link NetworkCapabilities#NET_CAPABILITY_SUPL} capability.
     */
    @Deprecated
    public static final int TYPE_MOBILE_SUPL = 3;

    /**
     * A DUN-specific Mobile data connection.  This network type may use the
     * same network interface as {@link #TYPE_MOBILE} or it may use a different
     * one.  This is sometimes by the system when setting up an upstream connection
     * for tethering so that the carrier is aware of DUN traffic.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasCapability} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request a network that
     *         provides the {@link NetworkCapabilities#NET_CAPABILITY_DUN} capability.
     */
    @Deprecated
    public static final int TYPE_MOBILE_DUN  = 4;

    /**
     * A High Priority Mobile data connection.  This network type uses the
     * same network interface as {@link #TYPE_MOBILE} but the routing setup
     * is different.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasTransport} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request an
     *         appropriate network. See {@link NetworkCapabilities} for supported transports.
     */
    @Deprecated
    public static final int TYPE_MOBILE_HIPRI = 5;

    /**
     * A WiMAX data connection.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasTransport} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request an
     *         appropriate network. See {@link NetworkCapabilities} for supported transports.
     */
    @Deprecated
    public static final int TYPE_WIMAX       = 6;

    /**
     * A Bluetooth data connection.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasTransport} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request an
     *         appropriate network. See {@link NetworkCapabilities} for supported transports.
     */
    @Deprecated
    public static final int TYPE_BLUETOOTH   = 7;

    /**
     * Fake data connection.  This should not be used on shipping devices.
     * @deprecated This is not used any more.
     */
    @Deprecated
    public static final int TYPE_DUMMY       = 8;

    /**
     * An Ethernet data connection.
     *
     * @deprecated Applications should instead use {@link NetworkCapabilities#hasTransport} or
     *         {@link #requestNetwork(NetworkRequest, NetworkCallback)} to request an
     *         appropriate network. See {@link NetworkCapabilities} for supported transports.
     */
    @Deprecated
    public static final int TYPE_ETHERNET    = 9;

    /**
     * Over the air Administration.
     * @deprecated Use {@link NetworkCapabilities} instead.
     * {@hide}
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 130143562)
    public static final int TYPE_MOBILE_FOTA = 10;

    /**
     * IP Multimedia Subsystem.
     * @deprecated Use {@link NetworkCapabilities#NET_CAPABILITY_IMS} instead.
     * {@hide}
     */
    @Deprecated
    @UnsupportedAppUsage
    public static final int TYPE_MOBILE_IMS  = 11;

    /**
     * Carrier Branded Services.
     * @deprecated Use {@link NetworkCapabilities#NET_CAPABILITY_CBS} instead.
     * {@hide}
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 130143562)
    public static final int TYPE_MOBILE_CBS  = 12;

    /**
     * A Wi-Fi p2p connection. Only requesting processes will have access to
     * the peers connected.
     * @deprecated Use {@link NetworkCapabilities#NET_CAPABILITY_WIFI_P2P} instead.
     * {@hide}
     */
    @Deprecated
    @SystemApi
    public static final int TYPE_WIFI_P2P    = 13;

    /**
     * The network to use for initially attaching to the network
     * @deprecated Use {@link NetworkCapabilities#NET_CAPABILITY_IA} instead.
     * {@hide}
     */
    @Deprecated
    @UnsupportedAppUsage
    public static final int TYPE_MOBILE_IA = 14;

    /**
     * Emergency PDN connection for emergency services.  This
     * may include IMS and MMS in emergency situations.
     * @deprecated Use {@link NetworkCapabilities#NET_CAPABILITY_EIMS} instead.
     * {@hide}
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 130143562)
    public static final int TYPE_MOBILE_EMERGENCY = 15;

    /**
     * The network that uses proxy to achieve connectivity.
     * @deprecated Use {@link NetworkCapabilities} instead.
     * {@hide}
     */
    @Deprecated
    @SystemApi
    public static final int TYPE_PROXY = 16;

    /**
     * A virtual network using one or more native bearers.
     * It may or may not be providing security services.
     * @deprecated Applications should use {@link NetworkCapabilities#TRANSPORT_VPN} instead.
     */
    @Deprecated
    public static final int TYPE_VPN = 17;

    /**
     * A network that is exclusively meant to be used for testing
     *
     * @deprecated Use {@link NetworkCapabilities} instead.
     * @hide
     */
    @Deprecated
    public static final int TYPE_TEST = 18; // TODO: Remove this once NetworkTypes are unused.

    /**
     * @deprecated Use {@link NetworkCapabilities} instead.
     * @hide
     */
    @Deprecated
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "TYPE_" }, value = {
                TYPE_NONE,
                TYPE_MOBILE,
                TYPE_WIFI,
                TYPE_MOBILE_MMS,
                TYPE_MOBILE_SUPL,
                TYPE_MOBILE_DUN,
                TYPE_MOBILE_HIPRI,
                TYPE_WIMAX,
                TYPE_BLUETOOTH,
                TYPE_DUMMY,
                TYPE_ETHERNET,
                TYPE_MOBILE_FOTA,
                TYPE_MOBILE_IMS,
                TYPE_MOBILE_CBS,
                TYPE_WIFI_P2P,
                TYPE_MOBILE_IA,
                TYPE_MOBILE_EMERGENCY,
                TYPE_PROXY,
                TYPE_VPN,
                TYPE_TEST
    })
    public @interface LegacyNetworkType {}

    // Deprecated constants for return values of startUsingNetworkFeature. They used to live
    // in com.android.internal.telephony.PhoneConstants until they were made inaccessible.
    private static final int DEPRECATED_PHONE_CONSTANT_APN_ALREADY_ACTIVE = 0;
    private static final int DEPRECATED_PHONE_CONSTANT_APN_REQUEST_STARTED = 1;
    private static final int DEPRECATED_PHONE_CONSTANT_APN_REQUEST_FAILED = 3;

    /** {@hide} */
    public static final int MAX_RADIO_TYPE = TYPE_TEST;

    /** {@hide} */
    public static final int MAX_NETWORK_TYPE = TYPE_TEST;

    private static final int MIN_NETWORK_TYPE = TYPE_MOBILE;

    /**
     * If you want to set the default network preference,you can directly
     * change the networkAttributes array in framework's config.xml.
     *
     * @deprecated Since we support so many more networks now, the single
     *             network default network preference can't really express
     *             the hierarchy.  Instead, the default is defined by the
     *             networkAttributes in config.xml.  You can determine
     *             the current value by calling {@link #getNetworkPreference()}
     *             from an App.
     */
    @Deprecated
    public static final int DEFAULT_NETWORK_PREFERENCE = TYPE_WIFI;

    /**
     * @hide
     */
    public static final int REQUEST_ID_UNSET = 0;

    /**
     * Static unique request used as a tombstone for NetworkCallbacks that have been unregistered.
     * This allows to distinguish when unregistering NetworkCallbacks those that were never
     * registered from those that were already unregistered.
     * @hide
     */
    private static final NetworkRequest ALREADY_UNREGISTERED =
            new NetworkRequest.Builder().clearCapabilities().build();

    /**
     * A NetID indicating no Network is selected.
     * Keep in sync with bionic/libc/dns/include/resolv_netid.h
     * @hide
     */
    public static final int NETID_UNSET = 0;

    /**
     * Flag to indicate that an app is not subject to any restrictions that could result in its
     * network access blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_NONE = 0;

    /**
     * Flag to indicate that an app is subject to Battery saver restrictions that would
     * result in its network access being blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_BATTERY_SAVER = 1 << 0;

    /**
     * Flag to indicate that an app is subject to Doze restrictions that would
     * result in its network access being blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_DOZE = 1 << 1;

    /**
     * Flag to indicate that an app is subject to App Standby restrictions that would
     * result in its network access being blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_APP_STANDBY = 1 << 2;

    /**
     * Flag to indicate that an app is subject to Restricted mode restrictions that would
     * result in its network access being blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_RESTRICTED_MODE = 1 << 3;

    /**
     * Flag to indicate that an app is blocked because it is subject to an always-on VPN but the VPN
     * is not currently connected.
     *
     * @see DevicePolicyManager#setAlwaysOnVpnPackage(ComponentName, String, boolean)
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_LOCKDOWN_VPN = 1 << 4;

    /**
     * Flag to indicate that an app is subject to Low Power Standby restrictions that would
     * result in its network access being blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_LOW_POWER_STANDBY = 1 << 5;

    /**
     * Flag to indicate that an app is subject to default background restrictions that would
     * result in its network access being blocked.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_BASIC_BACKGROUND_RESTRICTIONS_ENABLED)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_APP_BACKGROUND = 1 << 6;

    /**
     * Flag to indicate that an app is subject to OEM-specific application restrictions that would
     * result in its network access being blocked.
     *
     * @see #FIREWALL_CHAIN_OEM_DENY_1
     * @see #FIREWALL_CHAIN_OEM_DENY_2
     * @see #FIREWALL_CHAIN_OEM_DENY_3
     * @hide
     */
    @FlaggedApi(Flags.FLAG_BLOCKED_REASON_OEM_DENY_CHAINS)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_OEM_DENY = 1 << 7;

    /**
     * Flag to indicate that an app does not have permission to access the specified network,
     * for example, because it does not have the {@link android.Manifest.permission#INTERNET}
     * permission.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_BLOCKED_REASON_NETWORK_RESTRICTED)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_REASON_NETWORK_RESTRICTED = 1 << 8;

    /**
     * Flag to indicate that an app is subject to Data saver restrictions that would
     * result in its metered network access being blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_METERED_REASON_DATA_SAVER = 1 << 16;

    /**
     * Flag to indicate that an app is subject to user restrictions that would
     * result in its metered network access being blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_METERED_REASON_USER_RESTRICTED = 1 << 17;

    /**
     * Flag to indicate that an app is subject to Device admin restrictions that would
     * result in its metered network access being blocked.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_METERED_REASON_ADMIN_DISABLED = 1 << 18;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = {"BLOCKED_"}, value = {
            BLOCKED_REASON_NONE,
            BLOCKED_REASON_BATTERY_SAVER,
            BLOCKED_REASON_DOZE,
            BLOCKED_REASON_APP_STANDBY,
            BLOCKED_REASON_RESTRICTED_MODE,
            BLOCKED_REASON_LOCKDOWN_VPN,
            BLOCKED_REASON_LOW_POWER_STANDBY,
            BLOCKED_REASON_APP_BACKGROUND,
            BLOCKED_REASON_OEM_DENY,
            BLOCKED_REASON_NETWORK_RESTRICTED,
            BLOCKED_METERED_REASON_DATA_SAVER,
            BLOCKED_METERED_REASON_USER_RESTRICTED,
            BLOCKED_METERED_REASON_ADMIN_DISABLED,
    })
    public @interface BlockedReason {}

    /**
     * Set of blocked reasons that are only applicable on metered networks.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int BLOCKED_METERED_REASON_MASK = 0xffff0000;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 130143562)
    private final IConnectivityManager mService;

    /**
     * Firewall chain for device idle (doze mode).
     * Allowlist of apps that have network access in device idle.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_DOZABLE = 1;

    /**
     * Firewall chain used for app standby.
     * Denylist of apps that do not have network access.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_STANDBY = 2;

    /**
     * Firewall chain used for battery saver.
     * Allowlist of apps that have network access when battery saver is on.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_POWERSAVE = 3;

    /**
     * Firewall chain used for restricted networking mode.
     * Allowlist of apps that have access in restricted networking mode.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_RESTRICTED = 4;

    /**
     * Firewall chain used for low power standby.
     * Allowlist of apps that have access in low power standby.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_LOW_POWER_STANDBY = 5;

    /**
     * Firewall chain used for always-on default background restrictions.
     * Allowlist of apps that have access because either they are in the foreground or they are
     * exempted for specific situations while in the background.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_BASIC_BACKGROUND_RESTRICTIONS_ENABLED)
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_BACKGROUND = 6;

    /**
     * Firewall chain used for OEM-specific application restrictions.
     *
     * Denylist of apps that will not have network access due to OEM-specific restrictions. If an
     * app UID is placed on this chain, and the chain is enabled, the app's packets will be dropped.
     *
     * All the {@code FIREWALL_CHAIN_OEM_DENY_x} chains are equivalent, and each one is
     * independent of the others. The chains can be enabled and disabled independently, and apps can
     * be added and removed from each chain independently.
     *
     * @see #FIREWALL_CHAIN_OEM_DENY_2
     * @see #FIREWALL_CHAIN_OEM_DENY_3
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_OEM_DENY_1 = 7;

    /**
     * Firewall chain used for OEM-specific application restrictions.
     *
     * Denylist of apps that will not have network access due to OEM-specific restrictions. If an
     * app UID is placed on this chain, and the chain is enabled, the app's packets will be dropped.
     *
     * All the {@code FIREWALL_CHAIN_OEM_DENY_x} chains are equivalent, and each one is
     * independent of the others. The chains can be enabled and disabled independently, and apps can
     * be added and removed from each chain independently.
     *
     * @see #FIREWALL_CHAIN_OEM_DENY_1
     * @see #FIREWALL_CHAIN_OEM_DENY_3
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_OEM_DENY_2 = 8;

    /**
     * Firewall chain used for OEM-specific application restrictions.
     *
     * Denylist of apps that will not have network access due to OEM-specific restrictions. If an
     * app UID is placed on this chain, and the chain is enabled, the app's packets will be dropped.
     *
     * All the {@code FIREWALL_CHAIN_OEM_DENY_x} chains are equivalent, and each one is
     * independent of the others. The chains can be enabled and disabled independently, and apps can
     * be added and removed from each chain independently.
     *
     * @see #FIREWALL_CHAIN_OEM_DENY_1
     * @see #FIREWALL_CHAIN_OEM_DENY_2
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_OEM_DENY_3 = 9;

    /**
     * Firewall chain for allow list on metered networks
     *
     * UIDs added to this chain have access to metered networks, unless they're also in one of the
     * denylist, {@link #FIREWALL_CHAIN_METERED_DENY_USER},
     * {@link #FIREWALL_CHAIN_METERED_DENY_ADMIN}
     *
     * Note that this chain is used from a separate bpf program that is triggered by iptables and
     * can not be controlled by {@link ConnectivityManager#setFirewallChainEnabled}.
     *
     * @hide
     */
    // TODO: Merge this chain with data saver and support setFirewallChainEnabled
    @FlaggedApi(Flags.FLAG_METERED_NETWORK_FIREWALL_CHAINS)
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_METERED_ALLOW = 10;

    /**
     * Firewall chain for user-set restrictions on metered networks
     *
     * UIDs added to this chain do not have access to metered networks.
     * UIDs should be added to this chain based on user settings.
     * To restrict metered network based on admin configuration (e.g. enterprise policies),
     * {@link #FIREWALL_CHAIN_METERED_DENY_ADMIN} should be used.
     * This chain corresponds to {@link #BLOCKED_METERED_REASON_USER_RESTRICTED}
     *
     * Note that this chain is used from a separate bpf program that is triggered by iptables and
     * can not be controlled by {@link ConnectivityManager#setFirewallChainEnabled}.
     *
     * @hide
     */
    // TODO: Support setFirewallChainEnabled to control this chain
    @FlaggedApi(Flags.FLAG_METERED_NETWORK_FIREWALL_CHAINS)
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_METERED_DENY_USER = 11;

    /**
     * Firewall chain for admin-set restrictions on metered networks
     *
     * UIDs added to this chain do not have access to metered networks.
     * UIDs should be added to this chain based on admin configuration (e.g. enterprise policies).
     * To restrict metered network based on user settings, {@link #FIREWALL_CHAIN_METERED_DENY_USER}
     * should be used.
     * This chain corresponds to {@link #BLOCKED_METERED_REASON_ADMIN_DISABLED}
     *
     * Note that this chain is used from a separate bpf program that is triggered by iptables and
     * can not be controlled by {@link ConnectivityManager#setFirewallChainEnabled}.
     *
     * @hide
     */
    // TODO: Support setFirewallChainEnabled to control this chain
    @FlaggedApi(Flags.FLAG_METERED_NETWORK_FIREWALL_CHAINS)
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_CHAIN_METERED_DENY_ADMIN = 12;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = false, prefix = "FIREWALL_CHAIN_", value = {
        FIREWALL_CHAIN_DOZABLE,
        FIREWALL_CHAIN_STANDBY,
        FIREWALL_CHAIN_POWERSAVE,
        FIREWALL_CHAIN_RESTRICTED,
        FIREWALL_CHAIN_LOW_POWER_STANDBY,
        FIREWALL_CHAIN_BACKGROUND,
        FIREWALL_CHAIN_OEM_DENY_1,
        FIREWALL_CHAIN_OEM_DENY_2,
        FIREWALL_CHAIN_OEM_DENY_3,
        FIREWALL_CHAIN_METERED_ALLOW,
        FIREWALL_CHAIN_METERED_DENY_USER,
        FIREWALL_CHAIN_METERED_DENY_ADMIN
    })
    public @interface FirewallChain {}

    /**
     * A firewall rule which allows or drops packets depending on existing policy.
     * Used by {@link #setUidFirewallRule(int, int, int)} to follow existing policy to handle
     * specific uid's packets in specific firewall chain.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_RULE_DEFAULT = 0;

    /**
     * A firewall rule which allows packets. Used by {@link #setUidFirewallRule(int, int, int)} to
     * allow specific uid's packets in specific firewall chain.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_RULE_ALLOW = 1;

    /**
     * A firewall rule which drops packets. Used by {@link #setUidFirewallRule(int, int, int)} to
     * drop specific uid's packets in specific firewall chain.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int FIREWALL_RULE_DENY = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = false, prefix = "FIREWALL_RULE_", value = {
        FIREWALL_RULE_DEFAULT,
        FIREWALL_RULE_ALLOW,
        FIREWALL_RULE_DENY
    })
    public @interface FirewallRule {}

    /** @hide */
    public static final long FEATURE_USE_DECLARED_METHODS_FOR_CALLBACKS = 1L;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @LongDef(flag = true, prefix = "FEATURE_", value = {
            FEATURE_USE_DECLARED_METHODS_FOR_CALLBACKS
    })
    public @interface ConnectivityManagerFeature {}

    /**
     * A kludge to facilitate static access where a Context pointer isn't available, like in the
     * case of the static set/getProcessDefaultNetwork methods and from the Network class.
     * TODO: Remove this after deprecating the static methods in favor of non-static methods or
     * methods that take a Context argument.
     */
    private static ConnectivityManager sInstance;

    private final Context mContext;

    @GuardedBy("mTetheringEventCallbacks")
    private TetheringManager mTetheringManager;

    // Cache of the most recently used NetworkCallback classes (not instances) -> method flags.
    // 100 is chosen kind arbitrarily as an unlikely number of different types of NetworkCallback
    // overrides that a process may have, and should generally not be reached (for example, the
    // system server services.jar has been observed with dexdump to have only 16 when this was
    // added, and a very large system services app only had 18).
    // If this number is exceeded, the code will still function correctly, but re-registering
    // using a network callback class that was used before, but 100+ other classes have been used in
    // the meantime, will be a bit slower (as slow as the first registration) because
    // getDeclaredMethodsFlag must re-examine the callback class to determine what methods it
    // overrides.
    private static final LruCache<Class<? extends NetworkCallback>, Integer> sMethodFlagsCache =
            new LruCache<>(100);

    private final Object mEnabledConnectivityManagerFeaturesLock = new Object();
    // mEnabledConnectivityManagerFeatures is lazy-loaded in this ConnectivityManager instance, but
    // fetched from ConnectivityService, where it is loaded in ConnectivityService startup, so it
    // should have consistent values.
    @GuardedBy("sEnabledConnectivityManagerFeaturesLock")
    @ConnectivityManagerFeature
    private Long mEnabledConnectivityManagerFeatures = null;

    private TetheringManager getTetheringManager() {
        synchronized (mTetheringEventCallbacks) {
            if (mTetheringManager == null) {
                mTetheringManager = mContext.getSystemService(TetheringManager.class);
            }
            return mTetheringManager;
        }
    }

    /**
     * Tests if a given integer represents a valid network type.
     * @param networkType the type to be tested
     * @return {@code true} if the type is valid, else {@code false}
     * @deprecated All APIs accepting a network type are deprecated. There should be no need to
     *             validate a network type.
     */
    @Deprecated
    public static boolean isNetworkTypeValid(int networkType) {
        return MIN_NETWORK_TYPE <= networkType && networkType <= MAX_NETWORK_TYPE;
    }

    /**
     * Returns a non-localized string representing a given network type.
     * ONLY used for debugging output.
     * @param type the type needing naming
     * @return a String for the given type, or a string version of the type ("87")
     * if no name is known.
     * @deprecated Types are deprecated. Use {@link NetworkCapabilities} instead.
     * {@hide}
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static String getNetworkTypeName(int type) {
        switch (type) {
          case TYPE_NONE:
                return "NONE";
            case TYPE_MOBILE:
                return "MOBILE";
            case TYPE_WIFI:
                return "WIFI";
            case TYPE_MOBILE_MMS:
                return "MOBILE_MMS";
            case TYPE_MOBILE_SUPL:
                return "MOBILE_SUPL";
            case TYPE_MOBILE_DUN:
                return "MOBILE_DUN";
            case TYPE_MOBILE_HIPRI:
                return "MOBILE_HIPRI";
            case TYPE_WIMAX:
                return "WIMAX";
            case TYPE_BLUETOOTH:
                return "BLUETOOTH";
            case TYPE_DUMMY:
                return "DUMMY";
            case TYPE_ETHERNET:
                return "ETHERNET";
            case TYPE_MOBILE_FOTA:
                return "MOBILE_FOTA";
            case TYPE_MOBILE_IMS:
                return "MOBILE_IMS";
            case TYPE_MOBILE_CBS:
                return "MOBILE_CBS";
            case TYPE_WIFI_P2P:
                return "WIFI_P2P";
            case TYPE_MOBILE_IA:
                return "MOBILE_IA";
            case TYPE_MOBILE_EMERGENCY:
                return "MOBILE_EMERGENCY";
            case TYPE_PROXY:
                return "PROXY";
            case TYPE_VPN:
                return "VPN";
            case TYPE_TEST:
                return "TEST";
            default:
                return Integer.toString(type);
        }
    }

    /**
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public void systemReady() {
        try {
            mService.systemReady();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Checks if a given type uses the cellular data connection.
     * This should be replaced in the future by a network property.
     * @param networkType the type to check
     * @return a boolean - {@code true} if uses cellular network, else {@code false}
     * @deprecated Types are deprecated. Use {@link NetworkCapabilities} instead.
     * {@hide}
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 130143562)
    public static boolean isNetworkTypeMobile(int networkType) {
        switch (networkType) {
            case TYPE_MOBILE:
            case TYPE_MOBILE_MMS:
            case TYPE_MOBILE_SUPL:
            case TYPE_MOBILE_DUN:
            case TYPE_MOBILE_HIPRI:
            case TYPE_MOBILE_FOTA:
            case TYPE_MOBILE_IMS:
            case TYPE_MOBILE_CBS:
            case TYPE_MOBILE_IA:
            case TYPE_MOBILE_EMERGENCY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the given network type is backed by a Wi-Fi radio.
     *
     * @deprecated Types are deprecated. Use {@link NetworkCapabilities} instead.
     * @hide
     */
    @Deprecated
    public static boolean isNetworkTypeWifi(int networkType) {
        switch (networkType) {
            case TYPE_WIFI:
            case TYPE_WIFI_P2P:
                return true;
            default:
                return false;
        }
    }

    /**
     * Preference for {@link ProfileNetworkPreference.Builder#setPreference(int)}.
     * See {@link #setProfileNetworkPreferences(UserHandle, List, Executor, Runnable)}
     * Specify that the traffic for this user should by follow the default rules:
     * applications in the profile designated by the UserHandle behave like any
     * other application and use the system default network as their default
     * network. Compare other PROFILE_NETWORK_PREFERENCE_* settings.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int PROFILE_NETWORK_PREFERENCE_DEFAULT = 0;

    /**
     * Preference for {@link ProfileNetworkPreference.Builder#setPreference(int)}.
     * See {@link #setProfileNetworkPreferences(UserHandle, List, Executor, Runnable)}
     * Specify that the traffic for this user should by default go on a network with
     * {@link NetworkCapabilities#NET_CAPABILITY_ENTERPRISE}, and on the system default network
     * if no such network is available.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int PROFILE_NETWORK_PREFERENCE_ENTERPRISE = 1;

    /**
     * Preference for {@link ProfileNetworkPreference.Builder#setPreference(int)}.
     * See {@link #setProfileNetworkPreferences(UserHandle, List, Executor, Runnable)}
     * Specify that the traffic for this user should by default go on a network with
     * {@link NetworkCapabilities#NET_CAPABILITY_ENTERPRISE} and if no such network is available
     * should not have a default network at all (that is, network accesses that
     * do not specify a network explicitly terminate with an error), even if there
     * is a system default network available to apps outside this preference.
     * The apps can still use a non-enterprise network if they request it explicitly
     * provided that specific network doesn't require any specific permission they
     * do not hold.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int PROFILE_NETWORK_PREFERENCE_ENTERPRISE_NO_FALLBACK = 2;

    /**
     * Preference for {@link ProfileNetworkPreference.Builder#setPreference(int)}.
     * See {@link #setProfileNetworkPreferences(UserHandle, List, Executor, Runnable)}
     * Specify that the traffic for this user should by default go on a network with
     * {@link NetworkCapabilities#NET_CAPABILITY_ENTERPRISE}.
     * If there is no such network, the apps will have no default
     * network at all, even if there are available non-enterprise networks on the
     * device (that is, network accesses that do not specify a network explicitly
     * terminate with an error). Additionally, the designated apps should be
     * blocked from using any non-enterprise network even if they specify it
     * explicitly, unless they hold specific privilege overriding this (see
     * {@link android.Manifest.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS}).
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int PROFILE_NETWORK_PREFERENCE_ENTERPRISE_BLOCKING = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            PROFILE_NETWORK_PREFERENCE_DEFAULT,
            PROFILE_NETWORK_PREFERENCE_ENTERPRISE,
            PROFILE_NETWORK_PREFERENCE_ENTERPRISE_NO_FALLBACK
    })
    public @interface ProfileNetworkPreferencePolicy {
    }

    /**
     * Specifies the preferred network type.  When the device has more
     * than one type available the preferred network type will be used.
     *
     * @param preference the network type to prefer over all others.  It is
     *         unspecified what happens to the old preferred network in the
     *         overall ordering.
     * @deprecated Functionality has been removed as it no longer makes sense,
     *             with many more than two networks - we'd need an array to express
     *             preference.  Instead we use dynamic network properties of
     *             the networks to describe their precedence.
     */
    @Deprecated
    public void setNetworkPreference(int preference) {
    }

    /**
     * Retrieves the current preferred network type.
     *
     * @return an integer representing the preferred network type
     *
     * @deprecated Functionality has been removed as it no longer makes sense,
     *             with many more than two networks - we'd need an array to express
     *             preference.  Instead we use dynamic network properties of
     *             the networks to describe their precedence.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public int getNetworkPreference() {
        return TYPE_NONE;
    }

    /**
     * Returns details about the currently active default data network. When
     * connected, this network is the default route for outgoing connections.
     * You should always check {@link NetworkInfo#isConnected()} before initiating
     * network traffic. This may return {@code null} when there is no default
     * network.
     * Note that if the default network is a VPN, this method will return the
     * NetworkInfo for one of its underlying networks instead, or null if the
     * VPN agent did not specify any. Apps interested in learning about VPNs
     * should use {@link #getNetworkInfo(android.net.Network)} instead.
     *
     * @return a {@link NetworkInfo} object for the current default network
     *        or {@code null} if no default network is currently active
     * @deprecated See {@link NetworkInfo}.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @Nullable
    public NetworkInfo getActiveNetworkInfo() {
        try {
            return mService.getActiveNetworkInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a {@link Network} object corresponding to the currently active
     * default data network.  In the event that the current active default data
     * network disconnects, the returned {@code Network} object will no longer
     * be usable.  This will return {@code null} when there is no default
     * network, or when the default network is blocked.
     *
     * @return a {@link Network} object for the current default network or
     *        {@code null} if no default network is currently active or if
     *        the default network is blocked for the caller
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @Nullable
    public Network getActiveNetwork() {
        try {
            return mService.getActiveNetwork();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a {@link Network} object corresponding to the currently active
     * default data network for a specific UID.  In the event that the default data
     * network disconnects, the returned {@code Network} object will no longer
     * be usable.  This will return {@code null} when there is no default
     * network for the UID.
     *
     * @return a {@link Network} object for the current default network for the
     *         given UID or {@code null} if no default network is currently active
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    @Nullable
    public Network getActiveNetworkForUid(int uid) {
        return getActiveNetworkForUid(uid, false);
    }

    /** {@hide} */
    public Network getActiveNetworkForUid(int uid, boolean ignoreBlocked) {
        try {
            return mService.getActiveNetworkForUid(uid, ignoreBlocked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static UidRange[] getUidRangeArray(@NonNull Collection<Range<Integer>> ranges) {
        Objects.requireNonNull(ranges);
        final UidRange[] rangesArray = new UidRange[ranges.size()];
        int index = 0;
        for (Range<Integer> range : ranges) {
            rangesArray[index++] = new UidRange(range.getLower(), range.getUpper());
        }

        return rangesArray;
    }

    /**
     * Adds or removes a requirement for given UID ranges to use the VPN.
     *
     * If set to {@code true}, informs the system that the UIDs in the specified ranges must not
     * have any connectivity except if a VPN is connected and applies to the UIDs, or if the UIDs
     * otherwise have permission to bypass the VPN (e.g., because they have the
     * {@link android.Manifest.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS} permission, or when
     * using a socket protected by a method such as {@link VpnService#protect(DatagramSocket)}. If
     * set to {@code false}, a previously-added restriction is removed.
     * <p>
     * Each of the UID ranges specified by this method is added and removed as is, and no processing
     * is performed on the ranges to de-duplicate, merge, split, or intersect them. In order to
     * remove a previously-added range, the exact range must be removed as is.
     * <p>
     * The changes are applied asynchronously and may not have been applied by the time the method
     * returns. Apps will be notified about any changes that apply to them via
     * {@link NetworkCallback#onBlockedStatusChanged} callbacks called after the changes take
     * effect.
     * <p>
     * This method will block the specified UIDs from accessing non-VPN networks, but does not
     * affect what the UIDs get as their default network.
     * Compare {@link #setVpnDefaultForUids(String, Collection)}, which declares that the UIDs
     * should only have a VPN as their default network, but does not block them from accessing other
     * networks if they request them explicitly with the {@link Network} API.
     * <p>
     * This method should be called only by the VPN code.
     *
     * @param ranges the UID ranges to restrict
     * @param requireVpn whether the specified UID ranges must use a VPN
     *
     * @hide
     */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETTINGS})
    @SystemApi(client = MODULE_LIBRARIES)
    public void setRequireVpnForUids(boolean requireVpn,
            @NonNull Collection<Range<Integer>> ranges) {
        Objects.requireNonNull(ranges);
        // The Range class is not parcelable. Convert to UidRange, which is what is used internally.
        // This method is not necessarily expected to be used outside the system server, so
        // parceling may not be necessary, but it could be used out-of-process, e.g., by the network
        // stack process, or by tests.
        final UidRange[] rangesArray = getUidRangeArray(ranges);
        try {
            mService.setRequireVpnForUids(requireVpn, rangesArray);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Inform the system that this VPN session should manage the passed UIDs.
     *
     * A VPN with the specified session ID may call this method to inform the system that the UIDs
     * in the specified range are subject to a VPN.
     * When this is called, the system will only choose a VPN for the default network of the UIDs in
     * the specified ranges.
     *
     * This method declares that the UIDs in the range will only have a VPN for their default
     * network, but does not block the UIDs from accessing other networks (permissions allowing) by
     * explicitly requesting it with the {@link Network} API.
     * Compare {@link #setRequireVpnForUids(boolean, Collection)}, which does not affect what
     * network the UIDs get as default, but will block them from accessing non-VPN networks.
     *
     * @param session The VPN session which manages the passed UIDs.
     * @param ranges The uid ranges which will treat VPN as their only default network.
     *
     * @hide
     */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETTINGS})
    @SystemApi(client = MODULE_LIBRARIES)
    public void setVpnDefaultForUids(@NonNull String session,
            @NonNull Collection<Range<Integer>> ranges) {
        Objects.requireNonNull(ranges);
        final UidRange[] rangesArray = getUidRangeArray(ranges);
        try {
            mService.setVpnNetworkPreference(session, rangesArray);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Temporarily set automaticOnOff keeplaive TCP polling alarm timer to 1 second.
     *
     * TODO: Remove this when the TCP polling design is replaced with callback.
     * @param timeMs The time of expiry, with System.currentTimeMillis() base. The value should be
     *               set no more than 5 minutes in the future.
     * @hide
     */
    public void setTestLowTcpPollingTimerForKeepalive(long timeMs) {
        try {
            mService.setTestLowTcpPollingTimerForKeepalive(timeMs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Informs ConnectivityService of whether the legacy lockdown VPN, as implemented by
     * LockdownVpnTracker, is in use. This is deprecated for new devices starting from Android 12
     * but is still supported for backwards compatibility.
     * <p>
     * This type of VPN is assumed always to use the system default network, and must always declare
     * exactly one underlying network, which is the network that was the default when the VPN
     * connected.
     * <p>
     * Calling this method with {@code true} enables legacy behaviour, specifically:
     * <ul>
     *     <li>Any VPN that applies to userId 0 behaves specially with respect to deprecated
     *     {@link #CONNECTIVITY_ACTION} broadcasts. Any such broadcasts will have the state in the
     *     {@link #EXTRA_NETWORK_INFO} replaced by state of the VPN network. Also, any time the VPN
     *     connects, a {@link #CONNECTIVITY_ACTION} broadcast will be sent for the network
     *     underlying the VPN.</li>
     *     <li>Deprecated APIs that return {@link NetworkInfo} objects will have their state
     *     similarly replaced by the VPN network state.</li>
     *     <li>Information on current network interfaces passed to NetworkStatsService will not
     *     include any VPN interfaces.</li>
     * </ul>
     *
     * @param enabled whether legacy lockdown VPN is enabled or disabled
     *
     * @hide
     */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETTINGS})
    @SystemApi(client = MODULE_LIBRARIES)
    public void setLegacyLockdownVpnEnabled(boolean enabled) {
        try {
            mService.setLegacyLockdownVpnEnabled(enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns details about the currently active default data network for a given uid.
     * This is for privileged use only to avoid spying on other apps.
     *
     * @return a {@link NetworkInfo} object for the current default network
     *        for the given uid or {@code null} if no default network is
     *        available for the specified uid.
     *
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public NetworkInfo getActiveNetworkInfoForUid(int uid) {
        return getActiveNetworkInfoForUid(uid, false);
    }

    /** {@hide} */
    public NetworkInfo getActiveNetworkInfoForUid(int uid, boolean ignoreBlocked) {
        try {
            return mService.getActiveNetworkInfoForUid(uid, ignoreBlocked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns connection status information about a particular network type.
     *
     * @param networkType integer specifying which networkType in
     *        which you're interested.
     * @return a {@link NetworkInfo} object for the requested
     *        network type or {@code null} if the type is not
     *        supported by the device. If {@code networkType} is
     *        TYPE_VPN and a VPN is active for the calling app,
     *        then this method will try to return one of the
     *        underlying networks for the VPN or null if the
     *        VPN agent didn't specify any.
     *
     * @deprecated This method does not support multiple connected networks
     *             of the same type. Use {@link #getAllNetworks} and
     *             {@link #getNetworkInfo(android.net.Network)} instead.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @Nullable
    public NetworkInfo getNetworkInfo(int networkType) {
        try {
            return mService.getNetworkInfo(networkType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns connection status information about a particular Network.
     *
     * @param network {@link Network} specifying which network
     *        in which you're interested.
     * @return a {@link NetworkInfo} object for the requested
     *        network or {@code null} if the {@code Network}
     *        is not valid.
     * @deprecated See {@link NetworkInfo}.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @Nullable
    public NetworkInfo getNetworkInfo(@Nullable Network network) {
        return getNetworkInfoForUid(network, Process.myUid(), false);
    }

    /** {@hide} */
    public NetworkInfo getNetworkInfoForUid(Network network, int uid, boolean ignoreBlocked) {
        try {
            return mService.getNetworkInfoForUid(network, uid, ignoreBlocked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns connection status information about all network types supported by the device.
     *
     * @return an array of {@link NetworkInfo} objects.  Check each
     * {@link NetworkInfo#getType} for which type each applies.
     *
     * @deprecated This method does not support multiple connected networks
     *             of the same type. Use {@link #getAllNetworks} and
     *             {@link #getNetworkInfo(android.net.Network)} instead.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @NonNull
    public NetworkInfo[] getAllNetworkInfo() {
        try {
            return mService.getAllNetworkInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return a list of {@link NetworkStateSnapshot}s, one for each network that is currently
     * connected.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETTINGS})
    @NonNull
    public List<NetworkStateSnapshot> getAllNetworkStateSnapshots() {
        try {
            return mService.getAllNetworkStateSnapshots();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the {@link Network} object currently serving a given type, or
     * null if the given type is not connected.
     *
     * @hide
     * @deprecated This method does not support multiple connected networks
     *             of the same type. Use {@link #getAllNetworks} and
     *             {@link #getNetworkInfo(android.net.Network)} instead.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage
    public Network getNetworkForType(int networkType) {
        try {
            return mService.getNetworkForType(networkType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns an array of all {@link Network} currently tracked by the framework.
     *
     * @deprecated This method does not provide any notification of network state changes, forcing
     *             apps to call it repeatedly. This is inefficient and prone to race conditions.
     *             Apps should use methods such as
     *             {@link #registerNetworkCallback(NetworkRequest, NetworkCallback)} instead.
     *             Apps that desire to obtain information about networks that do not apply to them
     *             can use {@link NetworkRequest.Builder#setIncludeOtherUidNetworks}.
     *
     * @return an array of {@link Network} objects.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @NonNull
    @Deprecated
    public Network[] getAllNetworks() {
        try {
            return mService.getAllNetworks();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns an array of {@link NetworkCapabilities} objects, representing
     * the Networks that applications run by the given user will use by default.
     * @hide
     */
    @UnsupportedAppUsage
    public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int userId) {
        try {
            return mService.getDefaultNetworkCapabilitiesForUser(
                    userId, mContext.getOpPackageName(), mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the IP information for the current default network.
     *
     * @return a {@link LinkProperties} object describing the IP info
     *        for the current default network, or {@code null} if there
     *        is no current default network.
     *
     * {@hide}
     * @deprecated please use {@link #getLinkProperties(Network)} on the return
     *             value of {@link #getActiveNetwork()} instead. In particular,
     *             this method will return non-null LinkProperties even if the
     *             app is blocked by policy from using this network.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 109783091)
    public LinkProperties getActiveLinkProperties() {
        try {
            return mService.getActiveLinkProperties();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the IP information for a given network type.
     *
     * @param networkType the network type of interest.
     * @return a {@link LinkProperties} object describing the IP info
     *        for the given networkType, or {@code null} if there is
     *        no current default network.
     *
     * {@hide}
     * @deprecated This method does not support multiple connected networks
     *             of the same type. Use {@link #getAllNetworks},
     *             {@link #getNetworkInfo(android.net.Network)}, and
     *             {@link #getLinkProperties(android.net.Network)} instead.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 130143562)
    public LinkProperties getLinkProperties(int networkType) {
        try {
            return mService.getLinkPropertiesForType(networkType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the {@link LinkProperties} for the given {@link Network}.  This
     * will return {@code null} if the network is unknown.
     *
     * @param network The {@link Network} object identifying the network in question.
     * @return The {@link LinkProperties} for the network, or {@code null}.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @Nullable
    public LinkProperties getLinkProperties(@Nullable Network network) {
        try {
            return mService.getLinkProperties(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Redact {@link LinkProperties} for a given package
     *
     * Returns an instance of the given {@link LinkProperties} appropriately redacted to send to the
     * given package, considering its permissions.
     *
     * @param lp A {@link LinkProperties} which will be redacted.
     * @param uid The target uid.
     * @param packageName The name of the package, for appops logging.
     * @return A redacted {@link LinkProperties} which is appropriate to send to the given uid,
     *         or null if the uid lacks the ACCESS_NETWORK_STATE permission.
     * @hide
     */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETTINGS})
    @SystemApi(client = MODULE_LIBRARIES)
    @Nullable
    public LinkProperties getRedactedLinkPropertiesForPackage(@NonNull LinkProperties lp, int uid,
            @NonNull String packageName) {
        try {
            return mService.getRedactedLinkPropertiesForPackage(
                    lp, uid, packageName, mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the {@link NetworkCapabilities} for the given {@link Network}, or null.
     *
     * This will remove any location sensitive data in the returned {@link NetworkCapabilities}.
     * Some {@link TransportInfo} instances like {@link android.net.wifi.WifiInfo} contain location
     * sensitive information. To retrieve this location sensitive information (subject to
     * the caller's location permissions), use a {@link NetworkCallback} with the
     * {@link NetworkCallback#FLAG_INCLUDE_LOCATION_INFO} flag instead.
     *
     * This method returns {@code null} if the network is unknown or if the |network| argument
     * is null.
     *
     * @param network The {@link Network} object identifying the network in question.
     * @return The {@link NetworkCapabilities} for the network, or {@code null}.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @Nullable
    public NetworkCapabilities getNetworkCapabilities(@Nullable Network network) {
        try {
            return mService.getNetworkCapabilities(
                    network, mContext.getOpPackageName(), mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Redact {@link NetworkCapabilities} for a given package.
     *
     * Returns an instance of {@link NetworkCapabilities} that is appropriately redacted to send
     * to the given package, considering its permissions. If the passed capabilities contain
     * location-sensitive information, they will be redacted to the correct degree for the location
     * permissions of the app (COARSE or FINE), and will blame the UID accordingly for retrieving
     * that level of location. If the UID holds no location permission, the returned object will
     * contain no location-sensitive information and the UID is not blamed.
     *
     * @param nc A {@link NetworkCapabilities} instance which will be redacted.
     * @param uid The target uid.
     * @param packageName The name of the package, for appops logging.
     * @return A redacted {@link NetworkCapabilities} which is appropriate to send to the given uid,
     *         or null if the uid lacks the ACCESS_NETWORK_STATE permission.
     * @hide
     */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETTINGS})
    @SystemApi(client = MODULE_LIBRARIES)
    @Nullable
    public NetworkCapabilities getRedactedNetworkCapabilitiesForPackage(
            @NonNull NetworkCapabilities nc,
            int uid, @NonNull String packageName) {
        try {
            return mService.getRedactedNetworkCapabilitiesForPackage(nc, uid, packageName,
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets a URL that can be used for resolving whether a captive portal is present.
     * 1. This URL should respond with a 204 response to a GET request to indicate no captive
     *    portal is present.
     * 2. This URL must be HTTP as redirect responses are used to find captive portal
     *    sign-in pages. Captive portals cannot respond to HTTPS requests with redirects.
     *
     * The system network validation may be using different strategies to detect captive portals,
     * so this method does not necessarily return a URL used by the system. It only returns a URL
     * that may be relevant for other components trying to detect captive portals.
     *
     * @hide
     * @deprecated This API returns a URL which is not guaranteed to be one of the URLs used by the
     *             system.
     */
    @Deprecated
    @SystemApi
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public String getCaptivePortalServerUrl() {
        try {
            return mService.getCaptivePortalServerUrl();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Tells the underlying networking system that the caller wants to
     * begin using the named feature. The interpretation of {@code feature}
     * is completely up to each networking implementation.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * @param networkType specifies which network the request pertains to
     * @param feature the name of the feature to be used
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is specific to each networking
     * implementation+feature combination, except that the value {@code -1}
     * always indicates failure.
     *
     * @deprecated Deprecated in favor of the cleaner
     *             {@link #requestNetwork(NetworkRequest, NetworkCallback)} API.
     *             In {@link VERSION_CODES#M}, and above, this method is unsupported and will
     *             throw {@code UnsupportedOperationException} if called.
     * @removed
     */
    @Deprecated
    public int startUsingNetworkFeature(int networkType, String feature) {
        checkLegacyRoutingApiAccess();
        NetworkCapabilities netCap = networkCapabilitiesForFeature(networkType, feature);
        if (netCap == null) {
            Log.d(TAG, "Can't satisfy startUsingNetworkFeature for " + networkType + ", " +
                    feature);
            return DEPRECATED_PHONE_CONSTANT_APN_REQUEST_FAILED;
        }

        NetworkRequest request = null;
        synchronized (sLegacyRequests) {
            LegacyRequest l = sLegacyRequests.get(netCap);
            if (l != null) {
                Log.d(TAG, "renewing startUsingNetworkFeature request " + l.networkRequest);
                renewRequestLocked(l);
                if (l.currentNetwork != null) {
                    return DEPRECATED_PHONE_CONSTANT_APN_ALREADY_ACTIVE;
                } else {
                    return DEPRECATED_PHONE_CONSTANT_APN_REQUEST_STARTED;
                }
            }

            request = requestNetworkForFeatureLocked(netCap);
        }
        if (request != null) {
            Log.d(TAG, "starting startUsingNetworkFeature for request " + request);
            return DEPRECATED_PHONE_CONSTANT_APN_REQUEST_STARTED;
        } else {
            Log.d(TAG, " request Failed");
            return DEPRECATED_PHONE_CONSTANT_APN_REQUEST_FAILED;
        }
    }

    /**
     * Tells the underlying networking system that the caller is finished
     * using the named feature. The interpretation of {@code feature}
     * is completely up to each networking implementation.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * @param networkType specifies which network the request pertains to
     * @param feature the name of the feature that is no longer needed
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is specific to each networking
     * implementation+feature combination, except that the value {@code -1}
     * always indicates failure.
     *
     * @deprecated Deprecated in favor of the cleaner
     *             {@link #unregisterNetworkCallback(NetworkCallback)} API.
     *             In {@link VERSION_CODES#M}, and above, this method is unsupported and will
     *             throw {@code UnsupportedOperationException} if called.
     * @removed
     */
    @Deprecated
    public int stopUsingNetworkFeature(int networkType, String feature) {
        checkLegacyRoutingApiAccess();
        NetworkCapabilities netCap = networkCapabilitiesForFeature(networkType, feature);
        if (netCap == null) {
            Log.d(TAG, "Can't satisfy stopUsingNetworkFeature for " + networkType + ", " +
                    feature);
            return -1;
        }

        if (removeRequestForFeature(netCap)) {
            Log.d(TAG, "stopUsingNetworkFeature for " + networkType + ", " + feature);
        }
        return 1;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private NetworkCapabilities networkCapabilitiesForFeature(int networkType, String feature) {
        if (networkType == TYPE_MOBILE) {
            switch (feature) {
                case "enableCBS":
                    return networkCapabilitiesForType(TYPE_MOBILE_CBS);
                case "enableDUN":
                case "enableDUNAlways":
                    return networkCapabilitiesForType(TYPE_MOBILE_DUN);
                case "enableFOTA":
                    return networkCapabilitiesForType(TYPE_MOBILE_FOTA);
                case "enableHIPRI":
                    return networkCapabilitiesForType(TYPE_MOBILE_HIPRI);
                case "enableIMS":
                    return networkCapabilitiesForType(TYPE_MOBILE_IMS);
                case "enableMMS":
                    return networkCapabilitiesForType(TYPE_MOBILE_MMS);
                case "enableSUPL":
                    return networkCapabilitiesForType(TYPE_MOBILE_SUPL);
                default:
                    return null;
            }
        } else if (networkType == TYPE_WIFI && "p2p".equals(feature)) {
            return networkCapabilitiesForType(TYPE_WIFI_P2P);
        }
        return null;
    }

    private int legacyTypeForNetworkCapabilities(NetworkCapabilities netCap) {
        if (netCap == null) return TYPE_NONE;
        if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_CBS)) {
            return TYPE_MOBILE_CBS;
        }
        if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)) {
            return TYPE_MOBILE_IMS;
        }
        if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOTA)) {
            return TYPE_MOBILE_FOTA;
        }
        if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_DUN)) {
            return TYPE_MOBILE_DUN;
        }
        if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)) {
            return TYPE_MOBILE_SUPL;
        }
        if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)) {
            return TYPE_MOBILE_MMS;
        }
        if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return TYPE_MOBILE_HIPRI;
        }
        if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P)) {
            return TYPE_WIFI_P2P;
        }
        return TYPE_NONE;
    }

    private static class LegacyRequest {
        NetworkCapabilities networkCapabilities;
        NetworkRequest networkRequest;
        int expireSequenceNumber;
        Network currentNetwork;
        int delay = -1;

        private void clearDnsBinding() {
            if (currentNetwork != null) {
                currentNetwork = null;
                setProcessDefaultNetworkForHostResolution(null);
            }
        }

        NetworkCallback networkCallback = new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                currentNetwork = network;
                Log.d(TAG, "startUsingNetworkFeature got Network:" + network);
                setProcessDefaultNetworkForHostResolution(network);
            }
            @Override
            public void onLost(Network network) {
                if (network.equals(currentNetwork)) clearDnsBinding();
                Log.d(TAG, "startUsingNetworkFeature lost Network:" + network);
            }
        };
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static final HashMap<NetworkCapabilities, LegacyRequest> sLegacyRequests =
            new HashMap<>();

    private NetworkRequest findRequestForFeature(NetworkCapabilities netCap) {
        synchronized (sLegacyRequests) {
            LegacyRequest l = sLegacyRequests.get(netCap);
            if (l != null) return l.networkRequest;
        }
        return null;
    }

    private void renewRequestLocked(LegacyRequest l) {
        l.expireSequenceNumber++;
        Log.d(TAG, "renewing request to seqNum " + l.expireSequenceNumber);
        sendExpireMsgForFeature(l.networkCapabilities, l.expireSequenceNumber, l.delay);
    }

    private void expireRequest(NetworkCapabilities netCap, int sequenceNum) {
        int ourSeqNum = -1;
        synchronized (sLegacyRequests) {
            LegacyRequest l = sLegacyRequests.get(netCap);
            if (l == null) return;
            ourSeqNum = l.expireSequenceNumber;
            if (l.expireSequenceNumber == sequenceNum) removeRequestForFeature(netCap);
        }
        Log.d(TAG, "expireRequest with " + ourSeqNum + ", " + sequenceNum);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private NetworkRequest requestNetworkForFeatureLocked(NetworkCapabilities netCap) {
        int delay = -1;
        int type = legacyTypeForNetworkCapabilities(netCap);
        try {
            delay = mService.getRestoreDefaultNetworkDelay(type);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        LegacyRequest l = new LegacyRequest();
        l.networkCapabilities = netCap;
        l.delay = delay;
        l.expireSequenceNumber = 0;
        l.networkRequest = sendRequestForNetwork(
                netCap, l.networkCallback, 0, REQUEST, type, getDefaultHandler());
        if (l.networkRequest == null) return null;
        sLegacyRequests.put(netCap, l);
        sendExpireMsgForFeature(netCap, l.expireSequenceNumber, delay);
        return l.networkRequest;
    }

    private void sendExpireMsgForFeature(NetworkCapabilities netCap, int seqNum, int delay) {
        if (delay >= 0) {
            Log.d(TAG, "sending expire msg with seqNum " + seqNum + " and delay " + delay);
            CallbackHandler handler = getDefaultHandler();
            Message msg = handler.obtainMessage(EXPIRE_LEGACY_REQUEST, seqNum, 0, netCap);
            handler.sendMessageDelayed(msg, delay);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean removeRequestForFeature(NetworkCapabilities netCap) {
        final LegacyRequest l;
        synchronized (sLegacyRequests) {
            l = sLegacyRequests.remove(netCap);
        }
        if (l == null) return false;
        unregisterNetworkCallback(l.networkCallback);
        l.clearDnsBinding();
        return true;
    }

    private static final SparseIntArray sLegacyTypeToTransport = new SparseIntArray();
    static {
        sLegacyTypeToTransport.put(TYPE_MOBILE,       NetworkCapabilities.TRANSPORT_CELLULAR);
        sLegacyTypeToTransport.put(TYPE_MOBILE_CBS,   NetworkCapabilities.TRANSPORT_CELLULAR);
        sLegacyTypeToTransport.put(TYPE_MOBILE_DUN,   NetworkCapabilities.TRANSPORT_CELLULAR);
        sLegacyTypeToTransport.put(TYPE_MOBILE_FOTA,  NetworkCapabilities.TRANSPORT_CELLULAR);
        sLegacyTypeToTransport.put(TYPE_MOBILE_HIPRI, NetworkCapabilities.TRANSPORT_CELLULAR);
        sLegacyTypeToTransport.put(TYPE_MOBILE_IMS,   NetworkCapabilities.TRANSPORT_CELLULAR);
        sLegacyTypeToTransport.put(TYPE_MOBILE_MMS,   NetworkCapabilities.TRANSPORT_CELLULAR);
        sLegacyTypeToTransport.put(TYPE_MOBILE_SUPL,  NetworkCapabilities.TRANSPORT_CELLULAR);
        sLegacyTypeToTransport.put(TYPE_WIFI,         NetworkCapabilities.TRANSPORT_WIFI);
        sLegacyTypeToTransport.put(TYPE_WIFI_P2P,     NetworkCapabilities.TRANSPORT_WIFI);
        sLegacyTypeToTransport.put(TYPE_BLUETOOTH,    NetworkCapabilities.TRANSPORT_BLUETOOTH);
        sLegacyTypeToTransport.put(TYPE_ETHERNET,     NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    private static final SparseIntArray sLegacyTypeToCapability = new SparseIntArray();
    static {
        sLegacyTypeToCapability.put(TYPE_MOBILE_CBS,  NetworkCapabilities.NET_CAPABILITY_CBS);
        sLegacyTypeToCapability.put(TYPE_MOBILE_DUN,  NetworkCapabilities.NET_CAPABILITY_DUN);
        sLegacyTypeToCapability.put(TYPE_MOBILE_FOTA, NetworkCapabilities.NET_CAPABILITY_FOTA);
        sLegacyTypeToCapability.put(TYPE_MOBILE_IMS,  NetworkCapabilities.NET_CAPABILITY_IMS);
        sLegacyTypeToCapability.put(TYPE_MOBILE_MMS,  NetworkCapabilities.NET_CAPABILITY_MMS);
        sLegacyTypeToCapability.put(TYPE_MOBILE_SUPL, NetworkCapabilities.NET_CAPABILITY_SUPL);
        sLegacyTypeToCapability.put(TYPE_WIFI_P2P,    NetworkCapabilities.NET_CAPABILITY_WIFI_P2P);
    }

    /**
     * Given a legacy type (TYPE_WIFI, ...) returns a NetworkCapabilities
     * instance suitable for registering a request or callback.  Throws an
     * IllegalArgumentException if no mapping from the legacy type to
     * NetworkCapabilities is known.
     *
     * @deprecated Types are deprecated. Use {@link NetworkCallback} or {@link NetworkRequest}
     *     to find the network instead.
     * @hide
     */
    public static NetworkCapabilities networkCapabilitiesForType(int type) {
        final NetworkCapabilities nc = new NetworkCapabilities();

        // Map from type to transports.
        final int NOT_FOUND = -1;
        final int transport = sLegacyTypeToTransport.get(type, NOT_FOUND);
        if (transport == NOT_FOUND) {
            throw new IllegalArgumentException("unknown legacy type: " + type);
        }
        nc.addTransportType(transport);

        // Map from type to capabilities.
        nc.addCapability(sLegacyTypeToCapability.get(
                type, NetworkCapabilities.NET_CAPABILITY_INTERNET));
        nc.maybeMarkCapabilitiesRestricted();
        return nc;
    }

    /** @hide */
    public static class PacketKeepaliveCallback {
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public PacketKeepaliveCallback() {
        }
        /** The requested keepalive was successfully started. */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public void onStarted() {}
        /** The keepalive was resumed after being paused by the system. */
        public void onResumed() {}
        /** The keepalive was successfully stopped. */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public void onStopped() {}
        /** The keepalive was paused automatically by the system. */
        public void onPaused() {}
        /** An error occurred. */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public void onError(int error) {}
    }

    /**
     * Allows applications to request that the system periodically send specific packets on their
     * behalf, using hardware offload to save battery power.
     *
     * To request that the system send keepalives, call one of the methods that return a
     * {@link ConnectivityManager.PacketKeepalive} object, such as {@link #startNattKeepalive},
     * passing in a non-null callback. If the callback is successfully started, the callback's
     * {@code onStarted} method will be called. If an error occurs, {@code onError} will be called,
     * specifying one of the {@code ERROR_*} constants in this class.
     *
     * To stop an existing keepalive, call {@link PacketKeepalive#stop}. The system will call
     * {@link PacketKeepaliveCallback#onStopped} if the operation was successful or
     * {@link PacketKeepaliveCallback#onError} if an error occurred.
     *
     * @deprecated Use {@link SocketKeepalive} instead.
     *
     * @hide
     */
    public class PacketKeepalive {

        private static final String TAG = "PacketKeepalive";

        /** @hide */
        public static final int SUCCESS = 0;

        /** @hide */
        public static final int NO_KEEPALIVE = -1;

        /** @hide */
        public static final int BINDER_DIED = -10;

        /** The specified {@code Network} is not connected. */
        public static final int ERROR_INVALID_NETWORK = -20;
        /** The specified IP addresses are invalid. For example, the specified source IP address is
          * not configured on the specified {@code Network}. */
        public static final int ERROR_INVALID_IP_ADDRESS = -21;
        /** The requested port is invalid. */
        public static final int ERROR_INVALID_PORT = -22;
        /** The packet length is invalid (e.g., too long). */
        public static final int ERROR_INVALID_LENGTH = -23;
        /** The packet transmission interval is invalid (e.g., too short). */
        public static final int ERROR_INVALID_INTERVAL = -24;

        /** The hardware does not support this request. */
        public static final int ERROR_HARDWARE_UNSUPPORTED = -30;
        /** The hardware returned an error. */
        public static final int ERROR_HARDWARE_ERROR = -31;

        /** The NAT-T destination port for IPsec */
        public static final int NATT_PORT = 4500;

        /** The minimum interval in seconds between keepalive packet transmissions */
        public static final int MIN_INTERVAL = 10;

        private final Network mNetwork;
        private final ISocketKeepaliveCallback mCallback;
        private final ExecutorService mExecutor;

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public void stop() {
            try {
                mExecutor.execute(() -> {
                    try {
                        mService.stopKeepalive(mCallback);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error stopping packet keepalive: ", e);
                        throw e.rethrowFromSystemServer();
                    }
                });
            } catch (RejectedExecutionException e) {
                // The internal executor has already stopped due to previous event.
            }
        }

        private PacketKeepalive(Network network, PacketKeepaliveCallback callback) {
            Objects.requireNonNull(network, "network cannot be null");
            Objects.requireNonNull(callback, "callback cannot be null");
            mNetwork = network;
            mExecutor = Executors.newSingleThreadExecutor();
            mCallback = new ISocketKeepaliveCallback.Stub() {
                @Override
                public void onStarted() {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        mExecutor.execute(() -> {
                            callback.onStarted();
                        });
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                }

                @Override
                public void onResumed() {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        mExecutor.execute(() -> {
                            callback.onResumed();
                        });
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                }

                @Override
                public void onStopped() {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        mExecutor.execute(() -> {
                            callback.onStopped();
                        });
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                    mExecutor.shutdown();
                }

                @Override
                public void onPaused() {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        mExecutor.execute(() -> {
                            callback.onPaused();
                        });
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                    mExecutor.shutdown();
                }

                @Override
                public void onError(int error) {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        mExecutor.execute(() -> {
                            callback.onError(error);
                        });
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                    mExecutor.shutdown();
                }

                @Override
                public void onDataReceived() {
                    // PacketKeepalive is only used for Nat-T keepalive and as such does not invoke
                    // this callback when data is received.
                }
            };
        }
    }

    /**
     * Starts an IPsec NAT-T keepalive packet with the specified parameters.
     *
     * @deprecated Use {@link #createSocketKeepalive} instead.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public PacketKeepalive startNattKeepalive(
            Network network, int intervalSeconds, PacketKeepaliveCallback callback,
            InetAddress srcAddr, int srcPort, InetAddress dstAddr) {
        final PacketKeepalive k = new PacketKeepalive(network, callback);
        try {
            mService.startNattKeepalive(network, intervalSeconds, k.mCallback,
                    srcAddr.getHostAddress(), srcPort, dstAddr.getHostAddress());
        } catch (RemoteException e) {
            Log.e(TAG, "Error starting packet keepalive: ", e);
            throw e.rethrowFromSystemServer();
        }
        return k;
    }

    // Construct an invalid fd.
    private ParcelFileDescriptor createInvalidFd() {
        final int invalidFd = -1;
        return ParcelFileDescriptor.adoptFd(invalidFd);
    }

    /**
     * Request that keepalives be started on a IPsec NAT-T socket.
     *
     * @param network The {@link Network} the socket is on.
     * @param socket The socket that needs to be kept alive.
     * @param source The source address of the {@link UdpEncapsulationSocket}.
     * @param destination The destination address of the {@link UdpEncapsulationSocket}.
     * @param executor The executor on which callback will be invoked. The provided {@link Executor}
     *                 must run callback sequentially, otherwise the order of callbacks cannot be
     *                 guaranteed.
     * @param callback A {@link SocketKeepalive.Callback}. Used for notifications about keepalive
     *        changes. Must be extended by applications that use this API.
     *
     * @return A {@link SocketKeepalive} object that can be used to control the keepalive on the
     *         given socket.
     **/
    public @NonNull SocketKeepalive createSocketKeepalive(@NonNull Network network,
            @NonNull UdpEncapsulationSocket socket,
            @NonNull InetAddress source,
            @NonNull InetAddress destination,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Callback callback) {
        ParcelFileDescriptor dup;
        try {
            // Dup is needed here as the pfd inside the socket is owned by the IpSecService,
            // which cannot be obtained by the app process.
            dup = ParcelFileDescriptor.dup(socket.getFileDescriptor());
        } catch (IOException ignored) {
            // Construct an invalid fd, so that if the user later calls start(), it will fail with
            // ERROR_INVALID_SOCKET.
            dup = createInvalidFd();
        }
        return new NattSocketKeepalive(mService, network, dup, socket.getResourceId(), source,
                destination, executor, callback);
    }

    /**
     * Request that keepalives be started on a IPsec NAT-T socket file descriptor. Directly called
     * by system apps which don't use IpSecService to create {@link UdpEncapsulationSocket}.
     *
     * @param network The {@link Network} the socket is on.
     * @param pfd The {@link ParcelFileDescriptor} that needs to be kept alive. The provided
     *        {@link ParcelFileDescriptor} must be bound to a port and the keepalives will be sent
     *        from that port.
     * @param source The source address of the {@link UdpEncapsulationSocket}.
     * @param destination The destination address of the {@link UdpEncapsulationSocket}. The
     *        keepalive packets will always be sent to port 4500 of the given {@code destination}.
     * @param executor The executor on which callback will be invoked. The provided {@link Executor}
     *                 must run callback sequentially, otherwise the order of callbacks cannot be
     *                 guaranteed.
     * @param callback A {@link SocketKeepalive.Callback}. Used for notifications about keepalive
     *        changes. Must be extended by applications that use this API.
     *
     * @return A {@link SocketKeepalive} object that can be used to control the keepalive on the
     *         given socket.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.PACKET_KEEPALIVE_OFFLOAD)
    public @NonNull SocketKeepalive createNattKeepalive(@NonNull Network network,
            @NonNull ParcelFileDescriptor pfd,
            @NonNull InetAddress source,
            @NonNull InetAddress destination,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Callback callback) {
        ParcelFileDescriptor dup;
        try {
            // TODO: Consider remove unnecessary dup.
            dup = pfd.dup();
        } catch (IOException ignored) {
            // Construct an invalid fd, so that if the user later calls start(), it will fail with
            // ERROR_INVALID_SOCKET.
            dup = createInvalidFd();
        }
        return new NattSocketKeepalive(mService, network, dup,
                -1 /* Unused */, source, destination, executor, callback);
    }

    /**
     * Request that keepalives be started on a TCP socket. The socket must be established.
     *
     * @param network The {@link Network} the socket is on.
     * @param socket The socket that needs to be kept alive.
     * @param executor The executor on which callback will be invoked. This implementation assumes
     *                 the provided {@link Executor} runs the callbacks in sequence with no
     *                 concurrency. Failing this, no guarantee of correctness can be made. It is
     *                 the responsibility of the caller to ensure the executor provides this
     *                 guarantee. A simple way of creating such an executor is with the standard
     *                 tool {@code Executors.newSingleThreadExecutor}.
     * @param callback A {@link SocketKeepalive.Callback}. Used for notifications about keepalive
     *        changes. Must be extended by applications that use this API.
     *
     * @return A {@link SocketKeepalive} object that can be used to control the keepalive on the
     *         given socket.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.PACKET_KEEPALIVE_OFFLOAD)
    public @NonNull SocketKeepalive createSocketKeepalive(@NonNull Network network,
            @NonNull Socket socket,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Callback callback) {
        ParcelFileDescriptor dup;
        try {
            dup = ParcelFileDescriptor.fromSocket(socket);
        } catch (UncheckedIOException ignored) {
            // Construct an invalid fd, so that if the user later calls start(), it will fail with
            // ERROR_INVALID_SOCKET.
            dup = createInvalidFd();
        }
        return new TcpSocketKeepalive(mService, network, dup, executor, callback);
    }

    /**
     * Get the supported keepalive count for each transport configured in resource overlays.
     *
     * @return An array of supported keepalive count for each transport type.
     * @hide
     */
    @RequiresPermission(anyOf = { android.Manifest.permission.NETWORK_SETTINGS,
            // CTS 13 used QUERY_ALL_PACKAGES to get the resource value, which was implemented
            // as below in KeepaliveUtils. Also allow that permission so that KeepaliveUtils can
            // use this method and avoid breaking released CTS. Apps that have this permission
            // can query the resource themselves anyway.
            android.Manifest.permission.QUERY_ALL_PACKAGES })
    public int[] getSupportedKeepalives() {
        try {
            return mService.getSupportedKeepalives();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Ensure that a network route exists to deliver traffic to the specified
     * host via the specified network interface. An attempt to add a route that
     * already exists is ignored, but treated as successful.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * @param networkType the type of the network over which traffic to the specified
     * host is to be routed
     * @param hostAddress the IP address of the host to which the route is desired
     * @return {@code true} on success, {@code false} on failure
     *
     * @deprecated Deprecated in favor of the
     *             {@link #requestNetwork(NetworkRequest, NetworkCallback)},
     *             {@link #bindProcessToNetwork} and {@link Network#getSocketFactory} API.
     *             In {@link VERSION_CODES#M}, and above, this method is unsupported and will
     *             throw {@code UnsupportedOperationException} if called.
     * @removed
     */
    @Deprecated
    public boolean requestRouteToHost(int networkType, int hostAddress) {
        return requestRouteToHostAddress(networkType, NetworkUtils.intToInetAddress(hostAddress));
    }

    /**
     * Ensure that a network route exists to deliver traffic to the specified
     * host via the specified network interface. An attempt to add a route that
     * already exists is ignored, but treated as successful.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * @param networkType the type of the network over which traffic to the specified
     * host is to be routed
     * @param hostAddress the IP address of the host to which the route is desired
     * @return {@code true} on success, {@code false} on failure
     * @hide
     * @deprecated Deprecated in favor of the {@link #requestNetwork} and
     *             {@link #bindProcessToNetwork} API.
     */
    @Deprecated
    @UnsupportedAppUsage
    @SystemApi(client = MODULE_LIBRARIES)
    public boolean requestRouteToHostAddress(int networkType, InetAddress hostAddress) {
        checkLegacyRoutingApiAccess();
        try {
            return mService.requestRouteToHostAddress(networkType, hostAddress.getAddress(),
                    mContext.getOpPackageName(), mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the value of the setting for background data usage. If false,
     * applications should not use the network if the application is not in the
     * foreground. Developers should respect this setting, and check the value
     * of this before performing any background data operations.
     * <p>
     * All applications that have background services that use the network
     * should listen to {@link #ACTION_BACKGROUND_DATA_SETTING_CHANGED}.
     * <p>
     * @deprecated As of {@link VERSION_CODES#ICE_CREAM_SANDWICH}, availability of
     * background data depends on several combined factors, and this method will
     * always return {@code true}. Instead, when background data is unavailable,
     * {@link #getActiveNetworkInfo()} will now appear disconnected.
     *
     * @return Whether background data usage is allowed.
     */
    @Deprecated
    public boolean getBackgroundDataSetting() {
        // assume that background data is allowed; final authority is
        // NetworkInfo which may be blocked.
        return true;
    }

    /**
     * Sets the value of the setting for background data usage.
     *
     * @param allowBackgroundData Whether an application should use data while
     *            it is in the background.
     *
     * @attr ref android.Manifest.permission#CHANGE_BACKGROUND_DATA_SETTING
     * @see #getBackgroundDataSetting()
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage
    public void setBackgroundDataSetting(boolean allowBackgroundData) {
        // ignored
    }

    /**
     * @hide
     * @deprecated Talk to TelephonyManager directly
     */
    @Deprecated
    @UnsupportedAppUsage
    public boolean getMobileDataEnabled() {
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        if (tm != null) {
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            Log.d("ConnectivityManager", "getMobileDataEnabled()+ subId=" + subId);
            boolean retVal = tm.createForSubscriptionId(subId).isDataEnabled();
            Log.d("ConnectivityManager", "getMobileDataEnabled()- subId=" + subId
                    + " retVal=" + retVal);
            return retVal;
        }
        Log.d("ConnectivityManager", "getMobileDataEnabled()- remote exception retVal=false");
        return false;
    }

    /**
     * Callback for use with {@link ConnectivityManager#addDefaultNetworkActiveListener}
     * to find out when the system default network has gone in to a high power state.
     */
    public interface OnNetworkActiveListener {
        /**
         * Called on the main thread of the process to report that the current data network
         * has become active, and it is now a good time to perform any pending network
         * operations.  Note that this listener only tells you when the network becomes
         * active; if at any other time you want to know whether it is active (and thus okay
         * to initiate network traffic), you can retrieve its instantaneous state with
         * {@link ConnectivityManager#isDefaultNetworkActive}.
         */
        void onNetworkActive();
    }

    @GuardedBy("mNetworkActivityListeners")
    private final ArrayMap<OnNetworkActiveListener, INetworkActivityListener>
            mNetworkActivityListeners = new ArrayMap<>();

    /**
     * Start listening to reports when the system's default data network is active, meaning it is
     * a good time to perform network traffic.  Use {@link #isDefaultNetworkActive()}
     * to determine the current state of the system's default network after registering the
     * listener.
     * <p>
     * If the process default network has been set with
     * {@link ConnectivityManager#bindProcessToNetwork} this function will not
     * reflect the process's default, but the system default.
     *
     * @param l The listener to be told when the network is active.
     */
    public void addDefaultNetworkActiveListener(final OnNetworkActiveListener l) {
        final INetworkActivityListener rl = new INetworkActivityListener.Stub() {
            @Override
            public void onNetworkActive() throws RemoteException {
                l.onNetworkActive();
            }
        };

        synchronized (mNetworkActivityListeners) {
            try {
                mService.registerNetworkActivityListener(rl);
                mNetworkActivityListeners.put(l, rl);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Remove network active listener previously registered with
     * {@link #addDefaultNetworkActiveListener}.
     *
     * @param l Previously registered listener.
     */
    public void removeDefaultNetworkActiveListener(@NonNull OnNetworkActiveListener l) {
        synchronized (mNetworkActivityListeners) {
            final INetworkActivityListener rl = mNetworkActivityListeners.get(l);
            if (rl == null) {
                throw new IllegalArgumentException("Listener was not registered.");
            }
            try {
                mService.unregisterNetworkActivityListener(rl);
                mNetworkActivityListeners.remove(l);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Return whether the data network is currently active.  An active network means that
     * it is currently in a high power state for performing data transmission.  On some
     * types of networks, it may be expensive to move and stay in such a state, so it is
     * more power efficient to batch network traffic together when the radio is already in
     * this state.  This method tells you whether right now is currently a good time to
     * initiate network traffic, as the network is already active.
     */
    public boolean isDefaultNetworkActive() {
        try {
            return mService.isDefaultNetworkActive();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * {@hide}
     */
    public ConnectivityManager(Context context, IConnectivityManager service) {
        this(context, service, true /* newStatic */);
    }

    private ConnectivityManager(Context context, IConnectivityManager service, boolean newStatic) {
        mContext = Objects.requireNonNull(context, "missing context");
        mService = Objects.requireNonNull(service, "missing IConnectivityManager");
        // sInstance is accessed without a lock, so it may actually be reassigned several times with
        // different ConnectivityManager, but that's still OK considering its usage.
        if (sInstance == null && newStatic) {
            final Context appContext = mContext.getApplicationContext();
            // Don't create static ConnectivityManager instance again to prevent infinite loop.
            // If the application context is null, we're either in the system process or
            // it's the application context very early in app initialization. In both these
            // cases, the passed-in Context will not be freed, so it's safe to pass it to the
            // service. http://b/27532714 .
            sInstance = new ConnectivityManager(appContext != null ? appContext : context, service,
                    false /* newStatic */);
        }
    }

    /** {@hide} */
    @UnsupportedAppUsage
    public static ConnectivityManager from(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /** @hide */
    public NetworkRequest getDefaultRequest() {
        try {
            // This is not racy as the default request is final in ConnectivityService.
            return mService.getDefaultRequest();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Check if the package is allowed to write settings. This also records that such an access
     * happened.
     *
     * @return {@code true} iff the package is allowed to write settings.
     */
    // TODO: Remove method and replace with direct call once R code is pushed to AOSP
    private static boolean checkAndNoteWriteSettingsOperation(@NonNull Context context, int uid,
            @NonNull String callingPackage, @Nullable String callingAttributionTag,
            boolean throwException) {
        return Settings.checkAndNoteWriteSettingsOperation(context, uid, callingPackage,
                callingAttributionTag, throwException);
    }

    /**
     * @deprecated - use getSystemService. This is a kludge to support static access in certain
     *               situations where a Context pointer is unavailable.
     * @hide
     */
    @Deprecated
    static ConnectivityManager getInstanceOrNull() {
        return sInstance;
    }

    /**
     * @deprecated - use getSystemService. This is a kludge to support static access in certain
     *               situations where a Context pointer is unavailable.
     * @hide
     */
    @Deprecated
    @UnsupportedAppUsage
    private static ConnectivityManager getInstance() {
        if (getInstanceOrNull() == null) {
            throw new IllegalStateException("No ConnectivityManager yet constructed");
        }
        return getInstanceOrNull();
    }

    /**
     * Get the set of tetherable, available interfaces.  This list is limited by
     * device configuration and current interface existence.
     *
     * @return an array of 0 or more Strings of tetherable interface names.
     *
     * @deprecated Use {@link TetheringEventCallback#onTetherableInterfacesChanged(List)} instead.
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage
    @Deprecated
    public String[] getTetherableIfaces() {
        return getTetheringManager().getTetherableIfaces();
    }

    /**
     * Get the set of tethered interfaces.
     *
     * @return an array of 0 or more String of currently tethered interface names.
     *
     * @deprecated Use {@link TetheringEventCallback#onTetherableInterfacesChanged(List)} instead.
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage
    @Deprecated
    public String[] getTetheredIfaces() {
        return getTetheringManager().getTetheredIfaces();
    }

    /**
     * Get the set of interface names which attempted to tether but
     * failed.  Re-attempting to tether may cause them to reset to the Tethered
     * state.  Alternatively, causing the interface to be destroyed and recreated
     * may cause them to reset to the available state.
     * {@link ConnectivityManager#getLastTetherError} can be used to get more
     * information on the cause of the errors.
     *
     * @return an array of 0 or more String indicating the interface names
     *        which failed to tether.
     *
     * @deprecated Use {@link TetheringEventCallback#onError(String, int)} instead.
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage
    @Deprecated
    public String[] getTetheringErroredIfaces() {
        return getTetheringManager().getTetheringErroredIfaces();
    }

    /**
     * Get the set of tethered dhcp ranges.
     *
     * @deprecated This method is not supported.
     * TODO: remove this function when all of clients are removed.
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    @Deprecated
    public String[] getTetheredDhcpRanges() {
        throw new UnsupportedOperationException("getTetheredDhcpRanges is not supported");
    }

    /**
     * Attempt to tether the named interface.  This will set up a dhcp server
     * on the interface, forward and NAT IP packets and forward DNS requests
     * to the best active upstream network interface.  Note that if no upstream
     * IP network interface is available, dhcp will still run and traffic will be
     * allowed between the tethered devices and this device, though upstream net
     * access will of course fail until an upstream network interface becomes
     * active.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * <p>WARNING: New clients should not use this function. The only usages should be in PanService
     * and WifiStateMachine which need direct access. All other clients should use
     * {@link #startTethering} and {@link #stopTethering} which encapsulate proper provisioning
     * logic. On SDK versions after {@link Build.VERSION_CODES.VANILLA_ICE_CREAM}, this will throw
     * an UnsupportedOperationException.</p>
     *
     * @param iface the interface name to tether.
     * @return error a {@code TETHER_ERROR} value indicating success or failure type
     * @deprecated Use {@link TetheringManager#startTethering} instead
     *
     * {@hide}
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Deprecated
    public int tether(String iface) {
        return getTetheringManager().tether(iface);
    }

    /**
     * Stop tethering the named interface.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * <p>WARNING: New clients should not use this function. The only usages should be in PanService
     * and WifiStateMachine which need direct access. All other clients should use
     * {@link #startTethering} and {@link #stopTethering} which encapsulate proper provisioning
     * logic. On SDK versions after {@link Build.VERSION_CODES.VANILLA_ICE_CREAM}, this will throw
     * an UnsupportedOperationException.</p>
     *
     * @param iface the interface name to untether.
     * @return error a {@code TETHER_ERROR} value indicating success or failure type
     *
     * {@hide}
     */
    @UnsupportedAppUsage
    @Deprecated
    public int untether(String iface) {
        return getTetheringManager().untether(iface);
    }

    /**
     * Check if the device allows for tethering.  It may be disabled via
     * {@code ro.tether.denied} system property, Settings.TETHER_SUPPORTED or
     * due to device configuration.
     *
     * <p>If this app does not have permission to use this API, it will always
     * return false rather than throw an exception.</p>
     *
     * <p>If the device has a hotspot provisioning app, the caller is required to hold the
     * {@link android.Manifest.permission.TETHER_PRIVILEGED} permission.</p>
     *
     * <p>Otherwise, this method requires the caller to hold the ability to modify system
     * settings as determined by {@link android.provider.Settings.System#canWrite}.</p>
     *
     * @return a boolean - {@code true} indicating Tethering is supported.
     *
     * @deprecated Use {@link TetheringEventCallback#onTetheringSupported(boolean)} instead.
     * {@hide}
     */
    @SystemApi
    @RequiresPermission(anyOf = {android.Manifest.permission.TETHER_PRIVILEGED,
            android.Manifest.permission.WRITE_SETTINGS})
    public boolean isTetheringSupported() {
        return getTetheringManager().isTetheringSupported();
    }

    /**
     * Callback for use with {@link #startTethering} to find out whether tethering succeeded.
     *
     * @deprecated Use {@link TetheringManager.StartTetheringCallback} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    public static abstract class OnStartTetheringCallback {
        /**
         * Called when tethering has been successfully started.
         */
        public void onTetheringStarted() {}

        /**
         * Called when starting tethering failed.
         */
        public void onTetheringFailed() {}
    }

    /**
     * Convenient overload for
     * {@link #startTethering(int, boolean, OnStartTetheringCallback, Handler)} which passes a null
     * handler to run on the current thread's {@link Looper}.
     *
     * @deprecated Use {@link TetheringManager#startTethering} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void startTethering(int type, boolean showProvisioningUi,
            final OnStartTetheringCallback callback) {
        startTethering(type, showProvisioningUi, callback, null);
    }

    /**
     * Runs tether provisioning for the given type if needed and then starts tethering if
     * the check succeeds. If no carrier provisioning is required for tethering, tethering is
     * enabled immediately. If provisioning fails, tethering will not be enabled. It also
     * schedules tether provisioning re-checks if appropriate.
     *
     * @param type The type of tethering to start. Must be one of
     *         {@link ConnectivityManager.TETHERING_WIFI},
     *         {@link ConnectivityManager.TETHERING_USB}, or
     *         {@link ConnectivityManager.TETHERING_BLUETOOTH}.
     * @param showProvisioningUi a boolean indicating to show the provisioning app UI if there
     *         is one. This should be true the first time this function is called and also any time
     *         the user can see this UI. It gives users information from their carrier about the
     *         check failing and how they can sign up for tethering if possible.
     * @param callback an {@link OnStartTetheringCallback} which will be called to notify the caller
     *         of the result of trying to tether.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     *
     * @deprecated Use {@link TetheringManager#startTethering} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void startTethering(int type, boolean showProvisioningUi,
            final OnStartTetheringCallback callback, Handler handler) {
        Objects.requireNonNull(callback, "OnStartTetheringCallback cannot be null.");

        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                if (handler == null) {
                    command.run();
                } else {
                    handler.post(command);
                }
            }
        };

        final StartTetheringCallback tetheringCallback = new StartTetheringCallback() {
            @Override
            public void onTetheringStarted() {
                callback.onTetheringStarted();
            }

            @Override
            public void onTetheringFailed(final int error) {
                callback.onTetheringFailed();
            }
        };

        final TetheringRequest request = new TetheringRequest.Builder(type)
                .setShouldShowEntitlementUi(showProvisioningUi).build();

        getTetheringManager().startTethering(request, executor, tetheringCallback);
    }

    /**
     * Stops tethering for the given type. Also cancels any provisioning rechecks for that type if
     * applicable.
     *
     * @param type The type of tethering to stop. Must be one of
     *         {@link ConnectivityManager.TETHERING_WIFI},
     *         {@link ConnectivityManager.TETHERING_USB}, or
     *         {@link ConnectivityManager.TETHERING_BLUETOOTH}.
     *
     * @deprecated Use {@link TetheringManager#stopTethering} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void stopTethering(int type) {
        getTetheringManager().stopTethering(type);
    }

    /**
     * Callback for use with {@link registerTetheringEventCallback} to find out tethering
     * upstream status.
     *
     * @deprecated Use {@link TetheringManager#OnTetheringEventCallback} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    public abstract static class OnTetheringEventCallback {

        /**
         * Called when tethering upstream changed. This can be called multiple times and can be
         * called any time.
         *
         * @param network the {@link Network} of tethering upstream. Null means tethering doesn't
         * have any upstream.
         */
        public void onUpstreamChanged(@Nullable Network network) {}
    }

    @GuardedBy("mTetheringEventCallbacks")
    private final ArrayMap<OnTetheringEventCallback, TetheringEventCallback>
            mTetheringEventCallbacks = new ArrayMap<>();

    /**
     * Start listening to tethering change events. Any new added callback will receive the last
     * tethering status right away. If callback is registered when tethering has no upstream or
     * disabled, {@link OnTetheringEventCallback#onUpstreamChanged} will immediately be called
     * with a null argument. The same callback object cannot be registered twice.
     *
     * @param executor the executor on which callback will be invoked.
     * @param callback the callback to be called when tethering has change events.
     *
     * @deprecated Use {@link TetheringManager#registerTetheringEventCallback} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void registerTetheringEventCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull final OnTetheringEventCallback callback) {
        Objects.requireNonNull(callback, "OnTetheringEventCallback cannot be null.");

        final TetheringEventCallback tetherCallback =
                new TetheringEventCallback() {
                    @Override
                    public void onUpstreamChanged(@Nullable Network network) {
                        callback.onUpstreamChanged(network);
                    }
                };

        synchronized (mTetheringEventCallbacks) {
            mTetheringEventCallbacks.put(callback, tetherCallback);
            getTetheringManager().registerTetheringEventCallback(executor, tetherCallback);
        }
    }

    /**
     * Remove tethering event callback previously registered with
     * {@link #registerTetheringEventCallback}.
     *
     * @param callback previously registered callback.
     *
     * @deprecated Use {@link TetheringManager#unregisterTetheringEventCallback} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void unregisterTetheringEventCallback(
            @NonNull final OnTetheringEventCallback callback) {
        Objects.requireNonNull(callback, "The callback must be non-null");
        synchronized (mTetheringEventCallbacks) {
            final TetheringEventCallback tetherCallback =
                    mTetheringEventCallbacks.remove(callback);
            getTetheringManager().unregisterTetheringEventCallback(tetherCallback);
        }
    }


    /**
     * Get the list of regular expressions that define any tetherable
     * USB network interfaces.  If USB tethering is not supported by the
     * device, this list should be empty.
     *
     * @return an array of 0 or more regular expression Strings defining
     *        what interfaces are considered tetherable usb interfaces.
     *
     * @deprecated Use {@link TetheringEventCallback#onTetherableInterfaceRegexpsChanged} instead.
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage
    @Deprecated
    public String[] getTetherableUsbRegexs() {
        return getTetheringManager().getTetherableUsbRegexs();
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * Wifi network interfaces.  If Wifi tethering is not supported by the
     * device, this list should be empty.
     *
     * @return an array of 0 or more regular expression Strings defining
     *        what interfaces are considered tetherable wifi interfaces.
     *
     * @deprecated Use {@link TetheringEventCallback#onTetherableInterfaceRegexpsChanged} instead.
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage
    @Deprecated
    public String[] getTetherableWifiRegexs() {
        return getTetheringManager().getTetherableWifiRegexs();
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * Bluetooth network interfaces.  If Bluetooth tethering is not supported by the
     * device, this list should be empty.
     *
     * @return an array of 0 or more regular expression Strings defining
     *        what interfaces are considered tetherable bluetooth interfaces.
     *
     * @deprecated Use {@link TetheringEventCallback#onTetherableInterfaceRegexpsChanged(
     *TetheringManager.TetheringInterfaceRegexps)} instead.
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage
    @Deprecated
    public String[] getTetherableBluetoothRegexs() {
        return getTetheringManager().getTetherableBluetoothRegexs();
    }

    /**
     * Attempt to both alter the mode of USB and Tethering of USB.  A
     * utility method to deal with some of the complexity of USB - will
     * attempt to switch to Rndis and subsequently tether the resulting
     * interface on {@code true} or turn off tethering and switch off
     * Rndis on {@code false}.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * @param enable a boolean - {@code true} to enable tethering
     * @return error a {@code TETHER_ERROR} value indicating success or failure type
     * @deprecated Use {@link TetheringManager#startTethering} instead
     *
     * {@hide}
     */
    @UnsupportedAppUsage
    @Deprecated
    public int setUsbTethering(boolean enable) {
        return getTetheringManager().setUsbTethering(enable);
    }

    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_NO_ERROR}.
     * {@hide}
     */
    @SystemApi
    @Deprecated
    public static final int TETHER_ERROR_NO_ERROR = 0;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_UNKNOWN_IFACE}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_UNKNOWN_IFACE =
            TetheringManager.TETHER_ERROR_UNKNOWN_IFACE;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_SERVICE_UNAVAIL}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_SERVICE_UNAVAIL =
            TetheringManager.TETHER_ERROR_SERVICE_UNAVAIL;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_UNSUPPORTED}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_UNSUPPORTED = TetheringManager.TETHER_ERROR_UNSUPPORTED;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_UNAVAIL_IFACE}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_UNAVAIL_IFACE =
            TetheringManager.TETHER_ERROR_UNAVAIL_IFACE;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_INTERNAL_ERROR}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_MASTER_ERROR =
            TetheringManager.TETHER_ERROR_INTERNAL_ERROR;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_TETHER_IFACE_ERROR}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_TETHER_IFACE_ERROR =
            TetheringManager.TETHER_ERROR_TETHER_IFACE_ERROR;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_UNTETHER_IFACE_ERROR}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_UNTETHER_IFACE_ERROR =
            TetheringManager.TETHER_ERROR_UNTETHER_IFACE_ERROR;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_ENABLE_FORWARDING_ERROR}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_ENABLE_NAT_ERROR =
            TetheringManager.TETHER_ERROR_ENABLE_FORWARDING_ERROR;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_DISABLE_FORWARDING_ERROR}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_DISABLE_NAT_ERROR =
            TetheringManager.TETHER_ERROR_DISABLE_FORWARDING_ERROR;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_IFACE_CFG_ERROR}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_IFACE_CFG_ERROR =
            TetheringManager.TETHER_ERROR_IFACE_CFG_ERROR;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_PROVISIONING_FAILED}.
     * {@hide}
     */
    @SystemApi
    @Deprecated
    public static final int TETHER_ERROR_PROVISION_FAILED = 11;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_DHCPSERVER_ERROR}.
     * {@hide}
     */
    @Deprecated
    public static final int TETHER_ERROR_DHCPSERVER_ERROR =
            TetheringManager.TETHER_ERROR_DHCPSERVER_ERROR;
    /**
     * @deprecated Use {@link TetheringManager#TETHER_ERROR_ENTITLEMENT_UNKNOWN}.
     * {@hide}
     */
    @SystemApi
    @Deprecated
    public static final int TETHER_ERROR_ENTITLEMENT_UNKONWN = 13;

    /**
     * Get a more detailed error code after a Tethering or Untethering
     * request asynchronously failed.
     *
     * @param iface The name of the interface of interest
     * @return error The error code of the last error tethering or untethering the named
     *               interface
     *
     * @deprecated Use {@link TetheringEventCallback#onError(String, int)} instead.
     * {@hide}
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Deprecated
    public int getLastTetherError(String iface) {
        int error = getTetheringManager().getLastTetherError(iface);
        if (error == TetheringManager.TETHER_ERROR_UNKNOWN_TYPE) {
            // TETHER_ERROR_UNKNOWN_TYPE was introduced with TetheringManager and has never been
            // returned by ConnectivityManager. Convert it to the legacy TETHER_ERROR_UNKNOWN_IFACE
            // instead.
            error = TetheringManager.TETHER_ERROR_UNKNOWN_IFACE;
        }
        return error;
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            TETHER_ERROR_NO_ERROR,
            TETHER_ERROR_PROVISION_FAILED,
            TETHER_ERROR_ENTITLEMENT_UNKONWN,
    })
    public @interface EntitlementResultCode {
    }

    /**
     * Callback for use with {@link #getLatestTetheringEntitlementResult} to find out whether
     * entitlement succeeded.
     *
     * @deprecated Use {@link TetheringManager#OnTetheringEntitlementResultListener} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    public interface OnTetheringEntitlementResultListener  {
        /**
         * Called to notify entitlement result.
         *
         * @param resultCode an int value of entitlement result. It may be one of
         *         {@link #TETHER_ERROR_NO_ERROR},
         *         {@link #TETHER_ERROR_PROVISION_FAILED}, or
         *         {@link #TETHER_ERROR_ENTITLEMENT_UNKONWN}.
         */
        void onTetheringEntitlementResult(@EntitlementResultCode int resultCode);
    }

    /**
     * Get the last value of the entitlement check on this downstream. If the cached value is
     * {@link #TETHER_ERROR_NO_ERROR} or showEntitlementUi argument is false, this just returns the
     * cached value. Otherwise, a UI-based entitlement check will be performed. It is not
     * guaranteed that the UI-based entitlement check will complete in any specific time period
     * and it may in fact never complete. Any successful entitlement check the platform performs for
     * any reason will update the cached value.
     *
     * @param type the downstream type of tethering. Must be one of
     *         {@link #TETHERING_WIFI},
     *         {@link #TETHERING_USB}, or
     *         {@link #TETHERING_BLUETOOTH}.
     * @param showEntitlementUi a boolean indicating whether to run UI-based entitlement check.
     * @param executor the executor on which callback will be invoked.
     * @param listener an {@link OnTetheringEntitlementResultListener} which will be called to
     *         notify the caller of the result of entitlement check. The listener may be called zero
     *         or one time.
     * @deprecated Use {@link TetheringManager#requestLatestTetheringEntitlementResult} instead.
     * {@hide}
     */
    @SystemApi
    @Deprecated
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void getLatestTetheringEntitlementResult(int type, boolean showEntitlementUi,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull final OnTetheringEntitlementResultListener listener) {
        Objects.requireNonNull(listener, "TetheringEntitlementResultListener cannot be null.");
        ResultReceiver wrappedListener = new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                final long token = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> {
                        listener.onTetheringEntitlementResult(resultCode);
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        };

        getTetheringManager().requestLatestTetheringEntitlementResult(type, wrappedListener,
                    showEntitlementUi);
    }

    /**
     * Report network connectivity status.  This is currently used only
     * to alter status bar UI.
     * <p>This method requires the caller to hold the permission
     * {@link android.Manifest.permission#STATUS_BAR}.
     *
     * @param networkType The type of network you want to report on
     * @param percentage The quality of the connection 0 is bad, 100 is good
     * @deprecated Types are deprecated. Use {@link #reportNetworkConnectivity} instead.
     * {@hide}
     */
    public void reportInetCondition(int networkType, int percentage) {
        printStackTrace();
        try {
            mService.reportInetCondition(networkType, percentage);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Report a problem network to the framework.  This provides a hint to the system
     * that there might be connectivity problems on this network and may cause
     * the framework to re-evaluate network connectivity and/or switch to another
     * network.
     *
     * @param network The {@link Network} the application was attempting to use
     *                or {@code null} to indicate the current default network.
     * @deprecated Use {@link #reportNetworkConnectivity} which allows reporting both
     *             working and non-working connectivity.
     */
    @Deprecated
    public void reportBadNetwork(@Nullable Network network) {
        printStackTrace();
        try {
            // One of these will be ignored because it matches system's current state.
            // The other will trigger the necessary reevaluation.
            mService.reportNetworkConnectivity(network, true);
            mService.reportNetworkConnectivity(network, false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Report to the framework whether a network has working connectivity.
     * This provides a hint to the system that a particular network is providing
     * working connectivity or not.  In response the framework may re-evaluate
     * the network's connectivity and might take further action thereafter.
     *
     * @param network The {@link Network} the application was attempting to use
     *                or {@code null} to indicate the current default network.
     * @param hasConnectivity {@code true} if the application was able to successfully access the
     *                        Internet using {@code network} or {@code false} if not.
     */
    public void reportNetworkConnectivity(@Nullable Network network, boolean hasConnectivity) {
        printStackTrace();
        try {
            mService.reportNetworkConnectivity(network, hasConnectivity);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set a network-independent global HTTP proxy.
     *
     * This sets an HTTP proxy that applies to all networks and overrides any network-specific
     * proxy. If set, HTTP libraries that are proxy-aware will use this global proxy when
     * accessing any network, regardless of what the settings for that network are.
     *
     * Note that HTTP proxies are by nature typically network-dependent, and setting a global
     * proxy is likely to break networking on multiple networks. This method is only meant
     * for device policy clients looking to do general internal filtering or similar use cases.
     *
     * @see #getGlobalProxy
     * @see LinkProperties#getHttpProxy
     *
     * @param p A {@link ProxyInfo} object defining the new global HTTP proxy. Calling this
     *          method with a {@code null} value will clear the global HTTP proxy.
     * @hide
     */
    // Used by Device Policy Manager to set the global proxy.
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    public void setGlobalProxy(@Nullable final ProxyInfo p) {
        try {
            mService.setGlobalProxy(p);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Retrieve any network-independent global HTTP proxy.
     *
     * @return {@link ProxyInfo} for the current global HTTP proxy or {@code null}
     *        if no global HTTP proxy is set.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @Nullable
    public ProxyInfo getGlobalProxy() {
        try {
            return mService.getGlobalProxy();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Retrieve the global HTTP proxy, or if no global HTTP proxy is set, a
     * network-specific HTTP proxy.  If {@code network} is null, the
     * network-specific proxy returned is the proxy of the default active
     * network.
     *
     * @return {@link ProxyInfo} for the current global HTTP proxy, or if no
     *         global HTTP proxy is set, {@code ProxyInfo} for {@code network},
     *         or when {@code network} is {@code null},
     *         the {@code ProxyInfo} for the default active network.  Returns
     *         {@code null} when no proxy applies or the caller doesn't have
     *         permission to use {@code network}.
     * @hide
     */
    public ProxyInfo getProxyForNetwork(Network network) {
        try {
            return mService.getProxyForNetwork(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the current default HTTP proxy settings.  If a global proxy is set it will be returned,
     * otherwise if this process is bound to a {@link Network} using
     * {@link #bindProcessToNetwork} then that {@code Network}'s proxy is returned, otherwise
     * the default network's proxy is returned.
     *
     * @return the {@link ProxyInfo} for the current HTTP proxy, or {@code null} if no
     *        HTTP proxy is active.
     */
    @Nullable
    public ProxyInfo getDefaultProxy() {
        return getProxyForNetwork(getBoundNetworkForProcess());
    }

    /**
     * Returns whether the hardware supports the given network type.
     *
     * This doesn't indicate there is coverage or such a network is available, just whether the
     * hardware supports it. For example a GSM phone without a SIM card will return {@code true}
     * for mobile data, but a WiFi only tablet would return {@code false}.
     *
     * @param networkType The network type we'd like to check
     * @return {@code true} if supported, else {@code false}
     * @deprecated Types are deprecated. Use {@link NetworkCapabilities} instead.
     * @hide
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 130143562)
    public boolean isNetworkSupported(int networkType) {
        try {
            return mService.isNetworkSupported(networkType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns if the currently active data network is metered. A network is
     * classified as metered when the user is sensitive to heavy data usage on
     * that connection due to monetary costs, data limitations or
     * battery/performance issues. You should check this before doing large
     * data transfers, and warn the user or delay the operation until another
     * network is available.
     *
     * @return {@code true} if large transfers should be avoided, otherwise
     *        {@code false}.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public boolean isActiveNetworkMetered() {
        try {
            return mService.isActiveNetworkMetered();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set sign in error notification to visible or invisible
     *
     * @hide
     * @deprecated Doesn't properly deal with multiple connected networks of the same type.
     */
    @Deprecated
    public void setProvisioningNotificationVisible(boolean visible, int networkType,
            String action) {
        try {
            mService.setProvisioningNotificationVisible(visible, networkType, action);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set the value for enabling/disabling airplane mode
     *
     * @param enable whether to enable airplane mode or not
     *
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_AIRPLANE_MODE,
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK})
    @SystemApi
    public void setAirplaneMode(boolean enable) {
        try {
            mService.setAirplaneMode(enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Registers the specified {@link NetworkProvider}.
     * Each listener must only be registered once. The listener can be unregistered with
     * {@link #unregisterNetworkProvider}.
     *
     * @param provider the provider to register
     * @return the ID of the provider. This ID must be used by the provider when registering
     *         {@link android.net.NetworkAgent}s.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_FACTORY})
    public int registerNetworkProvider(@NonNull NetworkProvider provider) {
        if (provider.getProviderId() != NetworkProvider.ID_NONE) {
            throw new IllegalStateException("NetworkProviders can only be registered once");
        }

        try {
            int providerId = mService.registerNetworkProvider(provider.getMessenger(),
                    provider.getName());
            provider.setProviderId(providerId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        return provider.getProviderId();
    }

    /**
     * Unregisters the specified NetworkProvider.
     *
     * @param provider the provider to unregister
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_FACTORY})
    public void unregisterNetworkProvider(@NonNull NetworkProvider provider) {
        try {
            mService.unregisterNetworkProvider(provider.getMessenger());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        provider.setProviderId(NetworkProvider.ID_NONE);
    }

    /**
     * Register or update a network offer with ConnectivityService.
     *
     * ConnectivityService keeps track of offers made by the various providers and matches
     * them to networking requests made by apps or the system. A callback identifies an offer
     * uniquely, and later calls with the same callback update the offer. The provider supplies a
     * score and the capabilities of the network it might be able to bring up ; these act as
     * filters used by ConnectivityService to only send those requests that can be fulfilled by the
     * provider.
     *
     * The provider is under no obligation to be able to bring up the network it offers at any
     * given time. Instead, this mechanism is meant to limit requests received by providers
     * to those they actually have a chance to fulfill, as providers don't have a way to compare
     * the quality of the network satisfying a given request to their own offer.
     *
     * An offer can be updated by calling this again with the same callback object. This is
     * similar to calling unofferNetwork and offerNetwork again, but will only update the
     * provider with the changes caused by the changes in the offer.
     *
     * @param provider The provider making this offer.
     * @param score The prospective score of the network.
     * @param caps The prospective capabilities of the network.
     * @param callback The callback to call when this offer is needed or unneeded.
     * @hide exposed via the NetworkProvider class.
     */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_FACTORY})
    public void offerNetwork(@NonNull final int providerId,
            @NonNull final NetworkScore score, @NonNull final NetworkCapabilities caps,
            @NonNull final INetworkOfferCallback callback) {
        try {
            mService.offerNetwork(providerId,
                    Objects.requireNonNull(score, "null score"),
                    Objects.requireNonNull(caps, "null caps"),
                    Objects.requireNonNull(callback, "null callback"));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Withdraw a network offer made with {@link #offerNetwork}.
     *
     * @param callback The callback passed at registration time. This must be the same object
     *                 that was passed to {@link #offerNetwork}
     * @hide exposed via the NetworkProvider class.
     */
    public void unofferNetwork(@NonNull final INetworkOfferCallback callback) {
        try {
            mService.unofferNetwork(Objects.requireNonNull(callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
    /** @hide exposed via the NetworkProvider class. */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_FACTORY})
    public void declareNetworkRequestUnfulfillable(@NonNull NetworkRequest request) {
        try {
            mService.declareNetworkRequestUnfulfillable(request);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     * Register a NetworkAgent with ConnectivityService.
     * @return Network corresponding to NetworkAgent.
     */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_FACTORY})
    public Network registerNetworkAgent(@NonNull INetworkAgent na, @NonNull NetworkInfo ni,
            @NonNull LinkProperties lp, @NonNull NetworkCapabilities nc,
            @NonNull NetworkScore score, @NonNull NetworkAgentConfig config, int providerId) {
        return registerNetworkAgent(na, ni, lp, nc, null /* localNetworkConfig */, score, config,
                providerId);
    }

    /**
     * @hide
     * Register a NetworkAgent with ConnectivityService.
     * @return Network corresponding to NetworkAgent.
     */
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_FACTORY})
    public Network registerNetworkAgent(@NonNull INetworkAgent na, @NonNull NetworkInfo ni,
            @NonNull LinkProperties lp, @NonNull NetworkCapabilities nc,
            @Nullable LocalNetworkConfig localNetworkConfig, @NonNull NetworkScore score,
            @NonNull NetworkAgentConfig config, int providerId) {
        try {
            return mService.registerNetworkAgent(na, ni, lp, nc, score, localNetworkConfig, config,
                    providerId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Base class for {@code NetworkRequest} callbacks. Used for notifications about network
     * changes. Should be extended by applications wanting notifications.
     *
     * A {@code NetworkCallback} is registered by calling
     * {@link #requestNetwork(NetworkRequest, NetworkCallback)},
     * {@link #registerNetworkCallback(NetworkRequest, NetworkCallback)},
     * or {@link #registerDefaultNetworkCallback(NetworkCallback)}. A {@code NetworkCallback} is
     * unregistered by calling {@link #unregisterNetworkCallback(NetworkCallback)}.
     * A {@code NetworkCallback} should be registered at most once at any time.
     * A {@code NetworkCallback} that has been unregistered can be registered again.
     */
    public static class NetworkCallback {
        /**
         * Bitmask of method flags with all flags set.
         * @hide
         */
        public static final int DECLARED_METHODS_ALL = ~0;

        /**
         * Bitmask of method flags with no flag set.
         * @hide
         */
        public static final int DECLARED_METHODS_NONE = 0;

        // Tracks whether an instance was created via reflection without calling the constructor.
        private final boolean mConstructorWasCalled;

        /**
         * Annotation for NetworkCallback methods to verify filtering is configured properly.
         *
         * This is only used in tests to ensure that tests fail when a new callback is added, or
         * callbacks are modified, without updating
         * {@link NetworkCallbackMethodsHolder#NETWORK_CB_METHODS} properly.
         * @hide
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        @VisibleForTesting
        public @interface FilteredCallback {
            /**
             * The NetworkCallback.METHOD_* ID of this method.
             */
            int methodId();

            /**
             * The ConnectivityManager.CALLBACK_* message that this method is directly called by.
             *
             * If this method is not called by any message, this should be
             * {@link #CALLBACK_TRANSITIVE_CALLS_ONLY}.
             */
            int calledByCallbackId();

            /**
             * If this method may call other NetworkCallback methods, an array of methods it calls.
             *
             * Only direct calls (not transitive calls) should be included. The IDs must be
             * NetworkCallback.METHOD_* IDs.
             */
            int[] mayCall() default {};
        }

        /**
         * No flags associated with this callback.
         * @hide
         */
        public static final int FLAG_NONE = 0;

        /**
         * Inclusion of this flag means location-sensitive redaction requests keeping location info.
         *
         * Some objects like {@link NetworkCapabilities} may contain location-sensitive information.
         * Prior to Android 12, this information is always returned to apps holding the appropriate
         * permission, possibly noting that the app has used location.
         * <p>In Android 12 and above, by default the sent objects do not contain any location
         * information, even if the app holds the necessary permissions, and the system does not
         * take note of location usage by the app. Apps can request that location information is
         * included, in which case the system will check location permission and the location
         * toggle state, and take note of location usage by the app if any such information is
         * returned.
         *
         * Use this flag to include any location sensitive data in {@link NetworkCapabilities} sent
         * via {@link #onCapabilitiesChanged(Network, NetworkCapabilities)}.
         * <p>
         * These include:
         * <li> Some transport info instances (retrieved via
         * {@link NetworkCapabilities#getTransportInfo()}) like {@link android.net.wifi.WifiInfo}
         * contain location sensitive information.
         * <li> OwnerUid (retrieved via {@link NetworkCapabilities#getOwnerUid()} is location
         * sensitive for wifi suggestor apps (i.e using
         * {@link android.net.wifi.WifiNetworkSuggestion WifiNetworkSuggestion}).</li>
         * </p>
         * <p>
         * Note:
         * <li> Retrieving this location sensitive information (subject to app's location
         * permissions) will be noted by system. </li>
         * <li> Without this flag any {@link NetworkCapabilities} provided via the callback does
         * not include location sensitive information.
         */
        // Note: Some existing fields which are location sensitive may still be included without
        // this flag if the app targets SDK < S (to maintain backwards compatibility).
        public static final int FLAG_INCLUDE_LOCATION_INFO = 1 << 0;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(flag = true, prefix = "FLAG_", value = {
                FLAG_NONE,
                FLAG_INCLUDE_LOCATION_INFO
        })
        public @interface Flag { }

        /**
         * All the valid flags for error checking.
         */
        private static final int VALID_FLAGS = FLAG_INCLUDE_LOCATION_INFO;

        public NetworkCallback() {
            this(FLAG_NONE);
        }

        public NetworkCallback(@Flag int flags) {
            if ((flags & VALID_FLAGS) != flags) {
                throw new IllegalArgumentException("Invalid flags");
            }
            mFlags = flags;
            mConstructorWasCalled = true;
        }

        /**
         * Called when the framework connects to a new network to evaluate whether it satisfies this
         * request. If evaluation succeeds, this callback may be followed by an {@link #onAvailable}
         * callback. There is no guarantee that this new network will satisfy any requests, or that
         * the network will stay connected for longer than the time necessary to evaluate it.
         * <p>
         * Most applications <b>should not</b> act on this callback, and should instead use
         * {@link #onAvailable}. This callback is intended for use by applications that can assist
         * the framework in properly evaluating the network &mdash; for example, an application that
         * can automatically log in to a captive portal without user intervention.
         *
         * @param network The {@link Network} of the network that is being evaluated.
         *
         * @hide
         */
        @FilteredCallback(methodId = METHOD_ONPRECHECK, calledByCallbackId = CALLBACK_PRECHECK)
        public void onPreCheck(@NonNull Network network) {}
        private static final int METHOD_ONPRECHECK = 1;

        /**
         * Called when the framework connects and has declared a new network ready for use.
         * This callback may be called more than once if the {@link Network} that is
         * satisfying the request changes.
         *
         * @param network The {@link Network} of the satisfying network.
         * @param networkCapabilities The {@link NetworkCapabilities} of the satisfying network.
         * @param linkProperties The {@link LinkProperties} of the satisfying network.
         * @param localInfo The {@link LocalNetworkInfo} of the satisfying network, or null
         *                  if this network is not a local network.
         * @param blocked Whether access to the {@link Network} is blocked due to system policy.
         * @hide
         */
        @FilteredCallback(methodId = METHOD_ONAVAILABLE_5ARGS,
                calledByCallbackId = CALLBACK_AVAILABLE,
                // If this list is modified, ConnectivityService#addAvailableStateUpdateCallbacks
                // needs to be updated too.
                mayCall = { METHOD_ONAVAILABLE_4ARGS,
                        METHOD_ONLOCALNETWORKINFOCHANGED,
                        METHOD_ONBLOCKEDSTATUSCHANGED_INT })
        public final void onAvailable(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities,
                @NonNull LinkProperties linkProperties,
                @Nullable LocalNetworkInfo localInfo,
                @BlockedReason int blocked) {
            // Internally only this method is called when a new network is available, and
            // it calls the callback in the same way and order that older versions used
            // to call so as not to change the behavior.
            onAvailable(network, networkCapabilities, linkProperties, blocked != 0);
            if (null != localInfo) onLocalNetworkInfoChanged(network, localInfo);
            onBlockedStatusChanged(network, blocked);
        }
        private static final int METHOD_ONAVAILABLE_5ARGS = 2;

        /**
         * Legacy variant of onAvailable that takes a boolean blocked reason.
         *
         * This method has never been public API, but it's not final, so there may be apps that
         * implemented it and rely on it being called. Do our best not to break them.
         * Note: such apps will also get a second call to onBlockedStatusChanged immediately after
         * this method is called. There does not seem to be a way to avoid this.
         * TODO: add a compat check to move apps off this method, and eventually stop calling it.
         *
         * @hide
         */
        @FilteredCallback(methodId = METHOD_ONAVAILABLE_4ARGS,
                calledByCallbackId = CALLBACK_TRANSITIVE_CALLS_ONLY,
                // If this list is modified, ConnectivityService#addAvailableStateUpdateCallbacks
                // needs to be updated too.
                mayCall = { METHOD_ONAVAILABLE_1ARG,
                        METHOD_ONNETWORKSUSPENDED,
                        METHOD_ONCAPABILITIESCHANGED,
                        METHOD_ONLINKPROPERTIESCHANGED
                })
        public void onAvailable(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities,
                @NonNull LinkProperties linkProperties,
                boolean blocked) {
            onAvailable(network);
            if (!networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)) {
                onNetworkSuspended(network);
            }
            onCapabilitiesChanged(network, networkCapabilities);
            onLinkPropertiesChanged(network, linkProperties);
            // No call to onBlockedStatusChanged here. That is done by the caller.
        }
        private static final int METHOD_ONAVAILABLE_4ARGS = 3;

        /**
         * Called when the framework connects and has declared a new network ready for use.
         *
         * <p>For callbacks registered with {@link #registerNetworkCallback}, multiple networks may
         * be available at the same time, and onAvailable will be called for each of these as they
         * appear.
         *
         * <p>For callbacks registered with {@link #requestNetwork} and
         * {@link #registerDefaultNetworkCallback}, this means the network passed as an argument
         * is the new best network for this request and is now tracked by this callback ; this
         * callback will no longer receive method calls about other networks that may have been
         * passed to this method previously. The previously-best network may have disconnected, or
         * it may still be around and the newly-best network may simply be better.
         *
         * <p>Starting with {@link android.os.Build.VERSION_CODES#O}, this will always immediately
         * be followed by a call to {@link #onCapabilitiesChanged(Network, NetworkCapabilities)}
         * then by a call to {@link #onLinkPropertiesChanged(Network, LinkProperties)}, and a call
         * to {@link #onBlockedStatusChanged(Network, boolean)}.
         *
         * <p>Do NOT call {@link #getNetworkCapabilities(Network)} or
         * {@link #getLinkProperties(Network)} or other synchronous ConnectivityManager methods in
         * this callback as this is prone to race conditions (there is no guarantee the objects
         * returned by these methods will be current). Instead, wait for a call to
         * {@link #onCapabilitiesChanged(Network, NetworkCapabilities)} and
         * {@link #onLinkPropertiesChanged(Network, LinkProperties)} whose arguments are guaranteed
         * to be well-ordered with respect to other callbacks.
         *
         * @param network The {@link Network} of the satisfying network.
         */
        @FilteredCallback(methodId = METHOD_ONAVAILABLE_1ARG,
                calledByCallbackId = CALLBACK_TRANSITIVE_CALLS_ONLY)
        public void onAvailable(@NonNull Network network) {}
        private static final int METHOD_ONAVAILABLE_1ARG = 4;

        /**
         * Called when the network is about to be lost, typically because there are no outstanding
         * requests left for it. This may be paired with a {@link NetworkCallback#onAvailable} call
         * with the new replacement network for graceful handover. This method is not guaranteed
         * to be called before {@link NetworkCallback#onLost} is called, for example in case a
         * network is suddenly disconnected.
         *
         * <p>Do NOT call {@link #getNetworkCapabilities(Network)} or
         * {@link #getLinkProperties(Network)} or other synchronous ConnectivityManager methods in
         * this callback as this is prone to race conditions ; calling these methods while in a
         * callback may return an outdated or even a null object.
         *
         * @param network The {@link Network} that is about to be lost.
         * @param maxMsToLive The time in milliseconds the system intends to keep the network
         *                    connected for graceful handover; note that the network may still
         *                    suffer a hard loss at any time.
         */
        @FilteredCallback(methodId = METHOD_ONLOSING, calledByCallbackId = CALLBACK_LOSING)
        public void onLosing(@NonNull Network network, int maxMsToLive) {}
        private static final int METHOD_ONLOSING = 5;

        /**
         * Called when a network disconnects or otherwise no longer satisfies this request or
         * callback.
         *
         * <p>If the callback was registered with requestNetwork() or
         * registerDefaultNetworkCallback(), it will only be invoked against the last network
         * returned by onAvailable() when that network is lost and no other network satisfies
         * the criteria of the request.
         *
         * <p>If the callback was registered with registerNetworkCallback() it will be called for
         * each network which no longer satisfies the criteria of the callback.
         *
         * <p>Do NOT call {@link #getNetworkCapabilities(Network)} or
         * {@link #getLinkProperties(Network)} or other synchronous ConnectivityManager methods in
         * this callback as this is prone to race conditions ; calling these methods while in a
         * callback may return an outdated or even a null object.
         *
         * @param network The {@link Network} lost.
         */
        @FilteredCallback(methodId = METHOD_ONLOST, calledByCallbackId = CALLBACK_LOST)
        public void onLost(@NonNull Network network) {}
        private static final int METHOD_ONLOST = 6;

        /**
         * If the callback was registered with one of the {@code requestNetwork} methods, this will
         * be called if no network is found within the timeout specified in {@link
         * #requestNetwork(NetworkRequest, NetworkCallback, int)} call or if the requested network
         * request cannot be fulfilled (whether or not a timeout was specified).
         *
         * If the callback was registered when reserving a network, this method indicates that the
         * reservation is removed. It can be called when the reservation is requested, because the
         * system could not satisfy the reservation, or after the reserved network connects.
         *
         * When this callback is invoked the associated {@link NetworkRequest} will have already
         * been removed and released, as if {@link #unregisterNetworkCallback(NetworkCallback)} had
         * been called.
         */
        @FilteredCallback(methodId = METHOD_ONUNAVAILABLE, calledByCallbackId = CALLBACK_UNAVAIL)
        public void onUnavailable() {}
        private static final int METHOD_ONUNAVAILABLE = 7;

        /**
         * Called when the network corresponding to this request changes capabilities but still
         * satisfies the requested criteria.
         *
         * <p>Starting with {@link android.os.Build.VERSION_CODES#O} this method is guaranteed
         * to be called immediately after {@link #onAvailable}.
         *
         * <p>Do NOT call {@link #getLinkProperties(Network)} or other synchronous
         * ConnectivityManager methods in this callback as this is prone to race conditions :
         * calling these methods while in a callback may return an outdated or even a null object.
         *
         * @param network The {@link Network} whose capabilities have changed.
         * @param networkCapabilities The new {@link NetworkCapabilities} for this
         *                            network.
         */
        @FilteredCallback(methodId = METHOD_ONCAPABILITIESCHANGED,
                calledByCallbackId = CALLBACK_CAP_CHANGED)
        public void onCapabilitiesChanged(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities) {}
        private static final int METHOD_ONCAPABILITIESCHANGED = 8;

        /**
         * Called when the network corresponding to this request changes {@link LinkProperties}.
         *
         * <p>Starting with {@link android.os.Build.VERSION_CODES#O} this method is guaranteed
         * to be called immediately after {@link #onAvailable}.
         *
         * <p>Do NOT call {@link #getNetworkCapabilities(Network)} or other synchronous
         * ConnectivityManager methods in this callback as this is prone to race conditions :
         * calling these methods while in a callback may return an outdated or even a null object.
         *
         * @param network The {@link Network} whose link properties have changed.
         * @param linkProperties The new {@link LinkProperties} for this network.
         */
        @FilteredCallback(methodId = METHOD_ONLINKPROPERTIESCHANGED,
                calledByCallbackId = CALLBACK_IP_CHANGED)
        public void onLinkPropertiesChanged(@NonNull Network network,
                @NonNull LinkProperties linkProperties) {}
        private static final int METHOD_ONLINKPROPERTIESCHANGED = 9;

        /**
         * Called when there is a change in the {@link LocalNetworkInfo} for this network.
         *
         * This is only called for local networks, that is those with the
         * NET_CAPABILITY_LOCAL_NETWORK network capability.
         *
         * @param network the {@link Network} whose local network info has changed.
         * @param localNetworkInfo the new {@link LocalNetworkInfo} for this network.
         * @hide
         */
        @FilteredCallback(methodId = METHOD_ONLOCALNETWORKINFOCHANGED,
                calledByCallbackId = CALLBACK_LOCAL_NETWORK_INFO_CHANGED)
        public void onLocalNetworkInfoChanged(@NonNull Network network,
                @NonNull LocalNetworkInfo localNetworkInfo) {}
        private static final int METHOD_ONLOCALNETWORKINFOCHANGED = 10;

        /**
         * Called when the network the framework connected to for this request suspends data
         * transmission temporarily.
         *
         * <p>This generally means that while the TCP connections are still live temporarily
         * network data fails to transfer. To give a specific example, this is used on cellular
         * networks to mask temporary outages when driving through a tunnel, etc. In general this
         * means read operations on sockets on this network will block once the buffers are
         * drained, and write operations will block once the buffers are full.
         *
         * <p>Do NOT call {@link #getNetworkCapabilities(Network)} or
         * {@link #getLinkProperties(Network)} or other synchronous ConnectivityManager methods in
         * this callback as this is prone to race conditions (there is no guarantee the objects
         * returned by these methods will be current).
         *
         * @hide
         */
        @FilteredCallback(methodId = METHOD_ONNETWORKSUSPENDED,
                calledByCallbackId = CALLBACK_SUSPENDED)
        public void onNetworkSuspended(@NonNull Network network) {}
        private static final int METHOD_ONNETWORKSUSPENDED = 11;

        /**
         * Called when the network the framework connected to for this request
         * returns from a {@link NetworkInfo.State#SUSPENDED} state. This should always be
         * preceded by a matching {@link NetworkCallback#onNetworkSuspended} call.

         * <p>Do NOT call {@link #getNetworkCapabilities(Network)} or
         * {@link #getLinkProperties(Network)} or other synchronous ConnectivityManager methods in
         * this callback as this is prone to race conditions : calling these methods while in a
         * callback may return an outdated or even a null object.
         *
         * @hide
         */
        @FilteredCallback(methodId = METHOD_ONNETWORKRESUMED, calledByCallbackId = CALLBACK_RESUMED)
        public void onNetworkResumed(@NonNull Network network) {}
        private static final int METHOD_ONNETWORKRESUMED = 12;

        /**
         * Called when access to the specified network is blocked or unblocked.
         *
         * <p>Do NOT call {@link #getNetworkCapabilities(Network)} or
         * {@link #getLinkProperties(Network)} or other synchronous ConnectivityManager methods in
         * this callback as this is prone to race conditions : calling these methods while in a
         * callback may return an outdated or even a null object.
         *
         * @param network The {@link Network} whose blocked status has changed.
         * @param blocked The blocked status of this {@link Network}.
         */
        @FilteredCallback(methodId = METHOD_ONBLOCKEDSTATUSCHANGED_BOOL,
                calledByCallbackId = CALLBACK_TRANSITIVE_CALLS_ONLY)
        public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {}
        private static final int METHOD_ONBLOCKEDSTATUSCHANGED_BOOL = 13;

        /**
         * Called when access to the specified network is blocked or unblocked, or the reason for
         * access being blocked changes.
         *
         * If a NetworkCallback object implements this method,
         * {@link #onBlockedStatusChanged(Network, boolean)} will not be called.
         *
         * <p>Do NOT call {@link #getNetworkCapabilities(Network)} or
         * {@link #getLinkProperties(Network)} or other synchronous ConnectivityManager methods in
         * this callback as this is prone to race conditions : calling these methods while in a
         * callback may return an outdated or even a null object.
         *
         * @param network The {@link Network} whose blocked status has changed.
         * @param blocked The blocked status of this {@link Network}.
         * @hide
         */
        @FilteredCallback(methodId = METHOD_ONBLOCKEDSTATUSCHANGED_INT,
                calledByCallbackId = CALLBACK_BLK_CHANGED,
                mayCall = { METHOD_ONBLOCKEDSTATUSCHANGED_BOOL })
        @SystemApi(client = MODULE_LIBRARIES)
        public void onBlockedStatusChanged(@NonNull Network network, @BlockedReason int blocked) {
            onBlockedStatusChanged(network, blocked != 0);
        }
        private static final int METHOD_ONBLOCKEDSTATUSCHANGED_INT = 14;

        /**
         * Called when a network is reserved.
         *
         * The reservation includes the {@link NetworkCapabilities} that uniquely describe the
         * network that was reserved. the caller communicates this information to hardware or
         * software components on or off-device to instruct them to create a network matching this
         * reservation.
         *
         * {@link #onReserved(NetworkCapabilities)} is called at most once and is guaranteed to be
         * called before any other callback unless the reservation is unavailable.
         *
         * Once a reservation is made, the reserved {@link NetworkCapabilities} will not be updated,
         * and the reservation remains in place until the reserved network connects or {@link
         * #onUnavailable} is called.
         *
         * @param networkCapabilities The {@link NetworkCapabilities} of the reservation.
         */
        @FlaggedApi(Flags.FLAG_IPV6_OVER_BLE)
        @FilteredCallback(methodId = METHOD_ONRESERVED, calledByCallbackId = CALLBACK_RESERVED)
        public void onReserved(@NonNull NetworkCapabilities networkCapabilities) {}
        private static final int METHOD_ONRESERVED = 15;

        private NetworkRequest networkRequest;
        private final int mFlags;
    }

    /**
     * Constant error codes used by ConnectivityService to communicate about failures and errors
     * across a Binder boundary.
     * @hide
     */
    public interface Errors {
        int TOO_MANY_REQUESTS = 1;
    }

    /** @hide */
    public static class TooManyRequestsException extends RuntimeException {}

    private static RuntimeException convertServiceException(ServiceSpecificException e) {
        switch (e.errorCode) {
            case Errors.TOO_MANY_REQUESTS:
                return new TooManyRequestsException();
            default:
                Log.w(TAG, "Unknown service error code " + e.errorCode);
                return new RuntimeException(e);
        }
    }

    private static final int CALLBACK_TRANSITIVE_CALLS_ONLY     = 0;
    /** @hide */
    public static final int CALLBACK_PRECHECK                   = 1;
    /** @hide */
    public static final int CALLBACK_AVAILABLE                  = 2;
    /** @hide arg1 = TTL */
    public static final int CALLBACK_LOSING                     = 3;
    /** @hide */
    public static final int CALLBACK_LOST                       = 4;
    /** @hide */
    public static final int CALLBACK_UNAVAIL                    = 5;
    /** @hide */
    public static final int CALLBACK_CAP_CHANGED                = 6;
    /** @hide */
    public static final int CALLBACK_IP_CHANGED                 = 7;
    /** @hide obj = NetworkCapabilities, arg1 = seq number */
    private static final int EXPIRE_LEGACY_REQUEST              = 8;
    /** @hide */
    public static final int CALLBACK_SUSPENDED                  = 9;
    /** @hide */
    public static final int CALLBACK_RESUMED                    = 10;
    /** @hide */
    public static final int CALLBACK_BLK_CHANGED                = 11;
    /** @hide */
    public static final int CALLBACK_LOCAL_NETWORK_INFO_CHANGED = 12;
    /** @hide */
    public static final int CALLBACK_RESERVED                   = 13;
    // When adding new IDs, note CallbackQueue assumes callback IDs are at most 16 bits.


    /** @hide */
    public static String getCallbackName(int whichCallback) {
        switch (whichCallback) {
            case CALLBACK_TRANSITIVE_CALLS_ONLY: return "CALLBACK_TRANSITIVE_CALLS_ONLY";
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
            case CALLBACK_LOCAL_NETWORK_INFO_CHANGED: return "CALLBACK_LOCAL_NETWORK_INFO_CHANGED";
            case CALLBACK_RESERVED:     return "CALLBACK_RESERVED";
            default:
                return Integer.toString(whichCallback);
        }
    }

    /** @hide */
    @VisibleForTesting
    public static class NetworkCallbackMethod {
        @NonNull
        public final String mName;
        @NonNull
        public final Class<?>[] mParameterTypes;
        // Bitmask of CALLBACK_* that may transitively call this method.
        public final int mCallbacksCallingThisMethod;

        public NetworkCallbackMethod(@NonNull String name, @NonNull Class<?>[] parameterTypes,
                int callbacksCallingThisMethod) {
            mName = name;
            mParameterTypes = parameterTypes;
            mCallbacksCallingThisMethod = callbacksCallingThisMethod;
        }
    }

    // Holder class for the list of NetworkCallbackMethod. This ensures the list is only created
    // once on first usage, and not just on ConnectivityManager class initialization.
    /** @hide */
    @VisibleForTesting
    public static class NetworkCallbackMethodsHolder {
        public static final NetworkCallbackMethod[] NETWORK_CB_METHODS =
                new NetworkCallbackMethod[] {
                        method("onReserved", 1 << CALLBACK_RESERVED, NetworkCapabilities.class),
                        method("onPreCheck", 1 << CALLBACK_PRECHECK, Network.class),
                        // Note the final overload of onAvailable is not included, since it cannot
                        // match any overridden method.
                        method("onAvailable", 1 << CALLBACK_AVAILABLE, Network.class),
                        method("onAvailable", 1 << CALLBACK_AVAILABLE,
                                Network.class, NetworkCapabilities.class,
                                LinkProperties.class, boolean.class),
                        method("onLosing", 1 << CALLBACK_LOSING, Network.class, int.class),
                        method("onLost", 1 << CALLBACK_LOST, Network.class),
                        method("onUnavailable", 1 << CALLBACK_UNAVAIL),
                        method("onCapabilitiesChanged",
                                1 << CALLBACK_CAP_CHANGED | 1 << CALLBACK_AVAILABLE,
                                Network.class, NetworkCapabilities.class),
                        method("onLinkPropertiesChanged",
                                1 << CALLBACK_IP_CHANGED | 1 << CALLBACK_AVAILABLE,
                                Network.class, LinkProperties.class),
                        method("onLocalNetworkInfoChanged",
                                1 << CALLBACK_LOCAL_NETWORK_INFO_CHANGED | 1 << CALLBACK_AVAILABLE,
                                Network.class, LocalNetworkInfo.class),
                        method("onNetworkSuspended",
                                1 << CALLBACK_SUSPENDED | 1 << CALLBACK_AVAILABLE, Network.class),
                        method("onNetworkResumed",
                                1 << CALLBACK_RESUMED, Network.class),
                        method("onBlockedStatusChanged",
                                1 << CALLBACK_BLK_CHANGED | 1 << CALLBACK_AVAILABLE,
                                Network.class, boolean.class),
                        method("onBlockedStatusChanged",
                                1 << CALLBACK_BLK_CHANGED | 1 << CALLBACK_AVAILABLE,
                                Network.class, int.class),
                };

        private static NetworkCallbackMethod method(
                String name, int callbacksCallingThisMethod, Class<?>... args) {
            return new NetworkCallbackMethod(name, args, callbacksCallingThisMethod);
        }
    }

    private static class CallbackHandler extends Handler {
        private static final String TAG = "ConnectivityManager.CallbackHandler";
        private static final boolean DBG = false;

        CallbackHandler(Looper looper) {
            super(looper);
        }

        CallbackHandler(Handler handler) {
            this(Objects.requireNonNull(handler, "Handler cannot be null.").getLooper());
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == EXPIRE_LEGACY_REQUEST) {
                // the sInstance can't be null because to send this message a ConnectivityManager
                // instance must have been created prior to creating the thread on which this
                // Handler is running.
                sInstance.expireRequest((NetworkCapabilities) message.obj, message.arg1);
                return;
            }

            final NetworkRequest request = getObject(message, NetworkRequest.class);
            final Network network = getObject(message, Network.class);
            final NetworkCallback callback;
            synchronized (sCallbacks) {
                callback = sCallbacks.get(request);
                if (callback == null) {
                    Log.w(TAG,
                            "callback not found for " + getCallbackName(message.what) + " message");
                    return;
                }
                if (message.what == CALLBACK_UNAVAIL) {
                    sCallbacks.remove(request);
                    callback.networkRequest = ALREADY_UNREGISTERED;
                }
            }
            if (DBG) {
                Log.d(TAG, getCallbackName(message.what) + " for network " + network);
            }

            switch (message.what) {
                case CALLBACK_RESERVED: {
                    final NetworkCapabilities cap = getObject(message, NetworkCapabilities.class);
                    callback.onReserved(cap);
                    break;
                }
                case CALLBACK_PRECHECK: {
                    callback.onPreCheck(network);
                    break;
                }
                case CALLBACK_AVAILABLE: {
                    NetworkCapabilities cap = getObject(message, NetworkCapabilities.class);
                    LinkProperties lp = getObject(message, LinkProperties.class);
                    LocalNetworkInfo lni = getObject(message, LocalNetworkInfo.class);
                    callback.onAvailable(network, cap, lp, lni, message.arg1);
                    break;
                }
                case CALLBACK_LOSING: {
                    callback.onLosing(network, message.arg1);
                    break;
                }
                case CALLBACK_LOST: {
                    callback.onLost(network);
                    break;
                }
                case CALLBACK_UNAVAIL: {
                    callback.onUnavailable();
                    break;
                }
                case CALLBACK_CAP_CHANGED: {
                    NetworkCapabilities cap = getObject(message, NetworkCapabilities.class);
                    callback.onCapabilitiesChanged(network, cap);
                    break;
                }
                case CALLBACK_IP_CHANGED: {
                    LinkProperties lp = getObject(message, LinkProperties.class);
                    callback.onLinkPropertiesChanged(network, lp);
                    break;
                }
                case CALLBACK_LOCAL_NETWORK_INFO_CHANGED: {
                    final LocalNetworkInfo info = getObject(message, LocalNetworkInfo.class);
                    callback.onLocalNetworkInfoChanged(network, info);
                    break;
                }
                case CALLBACK_SUSPENDED: {
                    callback.onNetworkSuspended(network);
                    break;
                }
                case CALLBACK_RESUMED: {
                    callback.onNetworkResumed(network);
                    break;
                }
                case CALLBACK_BLK_CHANGED: {
                    callback.onBlockedStatusChanged(network, message.arg1);
                }
            }
        }

        private <T> T getObject(Message msg, Class<T> c) {
            return (T) msg.getData().getParcelable(c.getSimpleName());
        }
    }

    private CallbackHandler getDefaultHandler() {
        synchronized (sCallbacks) {
            if (sCallbackHandler == null) {
                sCallbackHandler = new CallbackHandler(ConnectivityThread.getInstanceLooper());
            }
            return sCallbackHandler;
        }
    }

    private static final HashMap<NetworkRequest, NetworkCallback> sCallbacks = new HashMap<>();
    private static CallbackHandler sCallbackHandler;

    private NetworkRequest sendRequestForNetwork(int asUid, NetworkCapabilities need,
            NetworkCallback callback, int timeoutMs, NetworkRequest.Type reqType, int legacyType,
            CallbackHandler handler) {
        printStackTrace();
        checkCallbackNotNull(callback);
        if (reqType != TRACK_DEFAULT && reqType != TRACK_SYSTEM_DEFAULT && need == null) {
            throw new IllegalArgumentException("null NetworkCapabilities");
        }


        final boolean useDeclaredMethods = isFeatureEnabled(
                FEATURE_USE_DECLARED_METHODS_FOR_CALLBACKS);
        // Set all bits if the feature is disabled
        int declaredMethodsFlag = useDeclaredMethods
                ? tryGetDeclaredMethodsFlag(callback)
                : NetworkCallback.DECLARED_METHODS_ALL;
        final NetworkRequest request;
        final String callingPackageName = mContext.getOpPackageName();
        try {
            synchronized(sCallbacks) {
                if (callback.networkRequest != null
                        && callback.networkRequest != ALREADY_UNREGISTERED) {
                    // TODO: throw exception instead and enforce 1:1 mapping of callbacks
                    // and requests (http://b/20701525).
                    Log.e(TAG, "NetworkCallback was already registered");
                }
                Messenger messenger = new Messenger(handler);
                Binder binder = new Binder();
                final int callbackFlags = callback.mFlags;
                if (reqType == LISTEN) {
                    request = mService.listenForNetwork(
                            need, messenger, binder, callbackFlags, callingPackageName,
                            mContext.getAttributionTag(), declaredMethodsFlag);
                } else {
                    request = mService.requestNetwork(
                            asUid, need, reqType.ordinal(), messenger, timeoutMs, binder,
                            legacyType, callbackFlags, callingPackageName,
                            mContext.getAttributionTag(), declaredMethodsFlag);
                }
                if (request != null) {
                    sCallbacks.put(request, callback);
                }
                callback.networkRequest = request;
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e) {
            throw convertServiceException(e);
        }
        return request;
    }

    private int tryGetDeclaredMethodsFlag(@NonNull NetworkCallback cb) {
        if (!cb.mConstructorWasCalled) {
            // Do not use the optimization if the callback was created via reflection or mocking,
            // as for example with dexmaker-mockito-inline methods will be instrumented without
            // using subclasses. This does not catch all cases as it is still possible to call the
            // constructor when creating mocks, but by default constructors are not called in that
            // case.
            return NetworkCallback.DECLARED_METHODS_ALL;
        }
        try {
            return getDeclaredMethodsFlag(cb.getClass());
        } catch (LinkageError e) {
            // This may happen if some methods reference inaccessible classes in their arguments
            // (for example b/261807130).
            Log.w(TAG, "Could not get methods from NetworkCallback class", e);
            // Fall through
        } catch (Throwable e) {
            // Log.wtf would be best but this is in app process, so the TerribleFailureHandler may
            // have unknown effects, possibly crashing the app (default behavior on eng builds or
            // if the WTF_IS_FATAL setting is set).
            Log.e(TAG, "Unexpected error while getting methods from NetworkCallback class", e);
            // Fall through
        }
        return NetworkCallback.DECLARED_METHODS_ALL;
    }

    private static int getDeclaredMethodsFlag(@NonNull Class<? extends NetworkCallback> clazz) {
        final Integer cachedFlags = sMethodFlagsCache.get(clazz);
        // As this is not synchronized, it is possible that this method will calculate the
        // flags for a given class multiple times, but that is fine. LruCache itself is thread-safe.
        if (cachedFlags != null) {
            return cachedFlags;
        }

        int flag = 0;
        // This uses getMethods instead of getDeclaredMethods, to make sure that if A overrides B
        // that overrides NetworkCallback, A.getMethods also returns methods declared by B.
        for (Method classMethod : clazz.getMethods()) {
            final Class<?> declaringClass = classMethod.getDeclaringClass();
            if (declaringClass == NetworkCallback.class) {
                // The callback is as defined by NetworkCallback and not overridden
                continue;
            }
            if (declaringClass == Object.class) {
                // Optimization: no need to try to match callbacks for methods declared by Object
                continue;
            }
            flag |= getCallbackIdsCallingThisMethod(classMethod);
        }

        if (flag == 0) {
            // dexmaker-mockito-inline (InlineDexmakerMockMaker), for example for mockito-extended,
            // modifies bytecode of classes in-place to add hooks instead of creating subclasses,
            // which would not be detected. When no method is found, fall back to enabling callbacks
            // for all methods.
            // This will not catch the case where both NetworkCallback bytecode is modified and a
            // subclass of NetworkCallback that has some overridden methods are used. But this kind
            // of bytecode injection is only possible in debuggable processes, with a JVMTI debug
            // agent attached, so it should not cause real issues.
            // There may be legitimate cases where an empty callback is filed with no method
            // overridden, for example requestNetwork(requestForCell, new NetworkCallback()) which
            // would ensure that one cell network stays up. But there is no way to differentiate
            // such NetworkCallbacks from a mock that called the constructor, so this code will
            // register the callback with DECLARED_METHODS_ALL and turn off the optimization in that
            // case. Apps are not expected to do this often anyway since the usefulness is very
            // limited.
            flag = NetworkCallback.DECLARED_METHODS_ALL;
        }
        sMethodFlagsCache.put(clazz, flag);
        return flag;
    }

    /**
     * Find out which of the base methods in NetworkCallback will call this method.
     *
     * For example, in the case of onLinkPropertiesChanged, this will be
     * (1 << CALLBACK_IP_CHANGED) | (1 << CALLBACK_AVAILABLE).
     */
    private static int getCallbackIdsCallingThisMethod(@NonNull Method method) {
        for (NetworkCallbackMethod baseMethod : NetworkCallbackMethodsHolder.NETWORK_CB_METHODS) {
            if (!baseMethod.mName.equals(method.getName())) {
                continue;
            }
            Class<?>[] methodParams = method.getParameterTypes();

            // As per JLS 8.4.8.1., a method m1 must have a subsignature of method m2 to override
            // it. And as per JLS 8.4.2, this means the erasure of the signature of m2 must be the
            // same as the signature of m1. Since type erasure is done at compile time, with
            // reflection the erased types are already observed, so the (erased) parameter types
            // must be equal.
            // So for example a method that is identical to a NetworkCallback method, except with
            // one parameter being a subclass of the parameter in the original method, will never
            // be called since it is not an override (the erasure of the arguments are not the same)
            // Therefore, the method is an override only if methodParams is exactly equal to
            // the base method's parameter types.
            if (Arrays.equals(baseMethod.mParameterTypes, methodParams)) {
                return baseMethod.mCallbacksCallingThisMethod;
            }
        }
        return 0;
    }

    private boolean isFeatureEnabled(@ConnectivityManagerFeature long connectivityManagerFeature) {
        synchronized (mEnabledConnectivityManagerFeaturesLock) {
            if (mEnabledConnectivityManagerFeatures == null) {
                try {
                    mEnabledConnectivityManagerFeatures =
                            mService.getEnabledConnectivityManagerFeatures();
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }
            return (mEnabledConnectivityManagerFeatures & connectivityManagerFeature) != 0;
        }
    }

    private NetworkRequest sendRequestForNetwork(NetworkCapabilities need, NetworkCallback callback,
            int timeoutMs, NetworkRequest.Type reqType, int legacyType, CallbackHandler handler) {
        return sendRequestForNetwork(Process.INVALID_UID, need, callback, timeoutMs, reqType,
                legacyType, handler);
    }

    /**
     * Helper function to request a network with a particular legacy type.
     *
     * This API is only for use in internal system code that requests networks with legacy type and
     * relies on CONNECTIVITY_ACTION broadcasts instead of NetworkCallbacks. New caller should use
     * {@link #requestNetwork(NetworkRequest, NetworkCallback, Handler)} instead.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param timeoutMs The time in milliseconds to attempt looking for a suitable network
     *                  before {@link NetworkCallback#onUnavailable()} is called. The timeout must
     *                  be a positive value (i.e. >0).
     * @param legacyType to specify the network type(#TYPE_*).
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @param networkCallback The {@link NetworkCallback} to be utilized for this request. Note
     *                        the callback must not be shared - it uniquely specifies this request.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK)
    public void requestNetwork(@NonNull NetworkRequest request,
            int timeoutMs, int legacyType, @NonNull Handler handler,
            @NonNull NetworkCallback networkCallback) {
        if (legacyType == TYPE_NONE) {
            throw new IllegalArgumentException("TYPE_NONE is meaningless legacy type");
        }
        CallbackHandler cbHandler = new CallbackHandler(handler);
        NetworkCapabilities nc = request.networkCapabilities;
        sendRequestForNetwork(nc, networkCallback, timeoutMs, REQUEST, legacyType, cbHandler);
    }

    /**
     * Request a network to satisfy a set of {@link NetworkCapabilities}.
     *
     * <p>This method will attempt to find the best network that matches the passed
     * {@link NetworkRequest}, and to bring up one that does if none currently satisfies the
     * criteria. The platform will evaluate which network is the best at its own discretion.
     * Throughput, latency, cost per byte, policy, user preference and other considerations
     * may be factored in the decision of what is considered the best network.
     *
     * <p>As long as this request is outstanding, the platform will try to maintain the best network
     * matching this request, while always attempting to match the request to a better network if
     * possible. If a better match is found, the platform will switch this request to the now-best
     * network and inform the app of the newly best network by invoking
     * {@link NetworkCallback#onAvailable(Network)} on the provided callback. Note that the platform
     * will not try to maintain any other network than the best one currently matching the request:
     * a network not matching any network request may be disconnected at any time.
     *
     * <p>For example, an application could use this method to obtain a connected cellular network
     * even if the device currently has a data connection over Ethernet. This may cause the cellular
     * radio to consume additional power. Or, an application could inform the system that it wants
     * a network supporting sending MMSes and have the system let it know about the currently best
     * MMS-supporting network through the provided {@link NetworkCallback}.
     *
     * <p>The status of the request can be followed by listening to the various callbacks described
     * in {@link NetworkCallback}. The {@link Network} object passed to the callback methods can be
     * used to direct traffic to the network (although accessing some networks may be subject to
     * holding specific permissions). Callers will learn about the specific characteristics of the
     * network through
     * {@link NetworkCallback#onCapabilitiesChanged(Network, NetworkCapabilities)} and
     * {@link NetworkCallback#onLinkPropertiesChanged(Network, LinkProperties)}. The methods of the
     * provided {@link NetworkCallback} will only be invoked due to changes in the best network
     * matching the request at any given time; therefore when a better network matching the request
     * becomes available, the {@link NetworkCallback#onAvailable(Network)} method is called
     * with the new network after which no further updates are given about the previously-best
     * network, unless it becomes the best again at some later time. All callbacks are invoked
     * in order on the same thread, which by default is a thread created by the framework running
     * in the app.
     * See {@link #requestNetwork(NetworkRequest, NetworkCallback, Handler)} to change where the
     * callbacks are invoked.
     *
     * <p>This{@link NetworkRequest} will live until released via
     * {@link #unregisterNetworkCallback(NetworkCallback)} or the calling application exits, at
     * which point the system may let go of the network at any time.
     *
     * <p>A version of this method which takes a timeout is
     * {@link #requestNetwork(NetworkRequest, NetworkCallback, int)}, that an app can use to only
     * wait for a limited amount of time for the network to become unavailable.
     *
     * <p>It is presently unsupported to request a network with mutable
     * {@link NetworkCapabilities} such as
     * {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED} or
     * {@link NetworkCapabilities#NET_CAPABILITY_CAPTIVE_PORTAL}
     * as these {@code NetworkCapabilities} represent states that a particular
     * network may never attain, and whether a network will attain these states
     * is unknown prior to bringing up the network so the framework does not
     * know how to go about satisfying a request with these capabilities.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #registerNetworkCallback} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param networkCallback The {@link NetworkCallback} to be utilized for this request. Note
     *                        the callback must not be shared - it uniquely specifies this request.
     *                        The callback is invoked on the default internal Handler.
     * @throws IllegalArgumentException if {@code request} contains invalid network capabilities.
     * @throws SecurityException if missing the appropriate permissions.
     * @throws RuntimeException if the app already has too many callbacks registered.
     */
    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback) {
        requestNetwork(request, networkCallback, getDefaultHandler());
    }

    /**
     * Request a network to satisfy a set of {@link NetworkCapabilities}.
     *
     * This method behaves identically to {@link #requestNetwork(NetworkRequest, NetworkCallback)}
     * but runs all the callbacks on the passed Handler.
     *
     * <p>This method has the same permission requirements as
     * {@link #requestNetwork(NetworkRequest, NetworkCallback)}, is subject to the same limitations,
     * and throws the same exceptions in the same conditions.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param networkCallback The {@link NetworkCallback} to be utilized for this request. Note
     *                        the callback must not be shared - it uniquely specifies this request.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     */
    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, @NonNull Handler handler) {
        CallbackHandler cbHandler = new CallbackHandler(handler);
        NetworkCapabilities nc = request.networkCapabilities;
        sendRequestForNetwork(nc, networkCallback, 0, REQUEST, TYPE_NONE, cbHandler);
    }

    /**
     * Reserve a network to satisfy a set of {@link NetworkCapabilities}.
     *
     * Some types of networks require the system to generate (i.e. reserve) some set of information
     * before a network can be connected. For such networks, {@link #reserveNetwork} can be used
     * which may lead to a call to {@link NetworkCallback#onReserved(NetworkCapabilities)}
     * containing the {@link NetworkCapabilities} that were reserved.
     *
     * A reservation reserves at most one network. If the network connects, a reservation request
     * behaves similar to a request filed using {@link #requestNetwork}. The provided {@link
     * NetworkCallback} will only be called for the reserved network.
     *
     * If the system determines that the requested reservation can never be fulfilled, {@link
     * NetworkCallback#onUnavailable} is called, the reservation is released by the system, and the
     * provided callback can be reused. Otherwise, the reservation remains in place until the
     * requested network connects. There is no guarantee that the reserved network will ever
     * connect.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @param networkCallback The {@link NetworkCallback} to be utilized for this request. Note
     *                        the callback must not be shared - it uniquely specifies this request.
     */
    // TODO: add executor overloads for all network request methods. Any method that passed an
    // Executor could process the messages on the singleton ConnectivityThread Handler.
    @SuppressLint("ExecutorRegistration")
    @FlaggedApi(Flags.FLAG_IPV6_OVER_BLE)
    public void reserveNetwork(@NonNull NetworkRequest request,
            @NonNull Handler handler,
            @NonNull NetworkCallback networkCallback) {
        final CallbackHandler cbHandler = new CallbackHandler(handler);
        final NetworkCapabilities nc = request.networkCapabilities;
        sendRequestForNetwork(nc, networkCallback, 0, RESERVATION, TYPE_NONE, cbHandler);
    }

    /**
     * Request a network to satisfy a set of {@link NetworkCapabilities}, limited
     * by a timeout.
     *
     * This function behaves identically to the non-timed-out version
     * {@link #requestNetwork(NetworkRequest, NetworkCallback)}, but if a suitable network
     * is not found within the given time (in milliseconds) the
     * {@link NetworkCallback#onUnavailable()} callback is called. The request can still be
     * released normally by calling {@link #unregisterNetworkCallback(NetworkCallback)} but does
     * not have to be released if timed-out (it is automatically released). Unregistering a
     * request that timed out is not an error.
     *
     * <p>Do not use this method to poll for the existence of specific networks (e.g. with a small
     * timeout) - {@link #registerNetworkCallback(NetworkRequest, NetworkCallback)} is provided
     * for that purpose. Calling this method will attempt to bring up the requested network.
     *
     * <p>This method has the same permission requirements as
     * {@link #requestNetwork(NetworkRequest, NetworkCallback)}, is subject to the same limitations,
     * and throws the same exceptions in the same conditions.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param networkCallback The {@link NetworkCallback} to be utilized for this request. Note
     *                        the callback must not be shared - it uniquely specifies this request.
     * @param timeoutMs The time in milliseconds to attempt looking for a suitable network
     *                  before {@link NetworkCallback#onUnavailable()} is called. The timeout must
     *                  be a positive value (i.e. >0).
     */
    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, int timeoutMs) {
        checkTimeout(timeoutMs);
        NetworkCapabilities nc = request.networkCapabilities;
        sendRequestForNetwork(nc, networkCallback, timeoutMs, REQUEST, TYPE_NONE,
                getDefaultHandler());
    }

    /**
     * Request a network to satisfy a set of {@link NetworkCapabilities}, limited
     * by a timeout.
     *
     * This method behaves identically to
     * {@link #requestNetwork(NetworkRequest, NetworkCallback, int)} but runs all the callbacks
     * on the passed Handler.
     *
     * <p>This method has the same permission requirements as
     * {@link #requestNetwork(NetworkRequest, NetworkCallback)}, is subject to the same limitations,
     * and throws the same exceptions in the same conditions.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param networkCallback The {@link NetworkCallback} to be utilized for this request. Note
     *                        the callback must not be shared - it uniquely specifies this request.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @param timeoutMs The time in milliseconds to attempt looking for a suitable network
     *                  before {@link NetworkCallback#onUnavailable} is called.
     */
    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, @NonNull Handler handler, int timeoutMs) {
        checkTimeout(timeoutMs);
        CallbackHandler cbHandler = new CallbackHandler(handler);
        NetworkCapabilities nc = request.networkCapabilities;
        sendRequestForNetwork(nc, networkCallback, timeoutMs, REQUEST, TYPE_NONE, cbHandler);
    }

    /**
     * The lookup key for a {@link Network} object included with the intent after
     * successfully finding a network for the applications request.  Retrieve it with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     * <p>
     * Note that if you intend to invoke {@link Network#openConnection(java.net.URL)}
     * then you must get a ConnectivityManager instance before doing so.
     */
    public static final String EXTRA_NETWORK = "android.net.extra.NETWORK";

    /**
     * The lookup key for a {@link NetworkRequest} object included with the intent after
     * successfully finding a network for the applications request.  Retrieve it with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_NETWORK_REQUEST = "android.net.extra.NETWORK_REQUEST";


    /**
     * Request a network to satisfy a set of {@link NetworkCapabilities}.
     *
     * This function behaves identically to the version that takes a NetworkCallback, but instead
     * of {@link NetworkCallback} a {@link PendingIntent} is used.  This means
     * the request may outlive the calling application and get called back when a suitable
     * network is found.
     * <p>
     * The operation is an Intent broadcast that goes to a broadcast receiver that
     * you registered with {@link Context#registerReceiver} or through the
     * &lt;receiver&gt; tag in an AndroidManifest.xml file
     * <p>
     * The operation Intent is delivered with two extras, a {@link Network} typed
     * extra called {@link #EXTRA_NETWORK} and a {@link NetworkRequest}
     * typed extra called {@link #EXTRA_NETWORK_REQUEST} containing
     * the original requests parameters.  It is important to create a new,
     * {@link NetworkCallback} based request before completing the processing of the
     * Intent to reserve the network or it will be released shortly after the Intent
     * is processed.
     * <p>
     * If there is already a request for this Intent registered (with the equality of
     * two Intents defined by {@link Intent#filterEquals}), then it will be removed and
     * replaced by this one, effectively releasing the previous {@link NetworkRequest}.
     * <p>
     * The request may be released normally by calling
     * {@link #releaseNetworkRequest(android.app.PendingIntent)}.
     * <p>It is presently unsupported to request a network with either
     * {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED} or
     * {@link NetworkCapabilities#NET_CAPABILITY_CAPTIVE_PORTAL}
     * as these {@code NetworkCapabilities} represent states that a particular
     * network may never attain, and whether a network will attain these states
     * is unknown prior to bringing up the network so the framework does not
     * know how to go about satisfying a request with these capabilities.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #registerNetworkCallback} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with {@link #unregisterNetworkCallback(PendingIntent)}
     * or {@link #releaseNetworkRequest(PendingIntent)}.
     *
     * <p>This method requires the caller to hold either the
     * {@link android.Manifest.permission#CHANGE_NETWORK_STATE} permission
     * or the ability to modify system settings as determined by
     * {@link android.provider.Settings.System#canWrite}.</p>
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param operation Action to perform when the network is available (corresponds
     *                  to the {@link NetworkCallback#onAvailable} call.  Typically
     *                  comes from {@link PendingIntent#getBroadcast}. Cannot be null.
     * @throws IllegalArgumentException if {@code request} contains invalid network capabilities.
     * @throws SecurityException if missing the appropriate permissions.
     * @throws RuntimeException if the app already has too many callbacks registered.
     */
    public void requestNetwork(@NonNull NetworkRequest request,
            @NonNull PendingIntent operation) {
        printStackTrace();
        checkPendingIntentNotNull(operation);
        try {
            mService.pendingRequestForNetwork(
                    request.networkCapabilities, operation, mContext.getOpPackageName(),
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e) {
            throw convertServiceException(e);
        }
    }

    /**
     * Removes a request made via {@link #requestNetwork(NetworkRequest, android.app.PendingIntent)}
     * <p>
     * This method has the same behavior as
     * {@link #unregisterNetworkCallback(android.app.PendingIntent)} with respect to
     * releasing network resources and disconnecting.
     *
     * @param operation A PendingIntent equal (as defined by {@link Intent#filterEquals}) to the
     *                  PendingIntent passed to
     *                  {@link #requestNetwork(NetworkRequest, android.app.PendingIntent)} with the
     *                  corresponding NetworkRequest you'd like to remove. Cannot be null.
     */
    public void releaseNetworkRequest(@NonNull PendingIntent operation) {
        printStackTrace();
        checkPendingIntentNotNull(operation);
        try {
            mService.releasePendingNetworkRequest(operation);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void checkPendingIntentNotNull(PendingIntent intent) {
        Objects.requireNonNull(intent, "PendingIntent cannot be null.");
    }

    private static void checkCallbackNotNull(NetworkCallback callback) {
        Objects.requireNonNull(callback, "null NetworkCallback");
    }

    private static void checkTimeout(int timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be strictly positive.");
        }
    }

    /**
     * Registers to receive notifications about all networks which satisfy the given
     * {@link NetworkRequest}.  The callbacks will continue to be called until
     * either the application exits or {@link #unregisterNetworkCallback(NetworkCallback)} is
     * called.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #requestNetwork} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param networkCallback The {@link NetworkCallback} that the system will call as suitable
     *                        networks change state.
     *                        The callback is invoked on the default internal Handler.
     * @throws RuntimeException if the app already has too many callbacks registered.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public void registerNetworkCallback(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback) {
        registerNetworkCallback(request, networkCallback, getDefaultHandler());
    }

    /**
     * Registers to receive notifications about all networks which satisfy the given
     * {@link NetworkRequest}.  The callbacks will continue to be called until
     * either the application exits or {@link #unregisterNetworkCallback(NetworkCallback)} is
     * called.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #requestNetwork} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param networkCallback The {@link NetworkCallback} that the system will call as suitable
     *                        networks change state.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @throws RuntimeException if the app already has too many callbacks registered.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public void registerNetworkCallback(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, @NonNull Handler handler) {
        CallbackHandler cbHandler = new CallbackHandler(handler);
        NetworkCapabilities nc = request.networkCapabilities;
        sendRequestForNetwork(nc, networkCallback, 0, LISTEN, TYPE_NONE, cbHandler);
    }

    /**
     * Registers a PendingIntent to be sent when a network is available which satisfies the given
     * {@link NetworkRequest}.
     *
     * This function behaves identically to the version that takes a NetworkCallback, but instead
     * of {@link NetworkCallback} a {@link PendingIntent} is used.  This means
     * the request may outlive the calling application and get called back when a suitable
     * network is found.
     * <p>
     * The operation is an Intent broadcast that goes to a broadcast receiver that
     * you registered with {@link Context#registerReceiver} or through the
     * &lt;receiver&gt; tag in an AndroidManifest.xml file
     * <p>
     * The operation Intent is delivered with two extras, a {@link Network} typed
     * extra called {@link #EXTRA_NETWORK} and a {@link NetworkRequest}
     * typed extra called {@link #EXTRA_NETWORK_REQUEST} containing
     * the original requests parameters.
     * <p>
     * If there is already a request for this Intent registered (with the equality of
     * two Intents defined by {@link Intent#filterEquals}), then it will be removed and
     * replaced by this one, effectively releasing the previous {@link NetworkRequest}.
     * <p>
     * The request may be released normally by calling
     * {@link #unregisterNetworkCallback(android.app.PendingIntent)}.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #requestNetwork} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with {@link #unregisterNetworkCallback(PendingIntent)}
     * or {@link #releaseNetworkRequest(PendingIntent)}.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param operation Action to perform when the network is available (corresponds
     *                  to the {@link NetworkCallback#onAvailable} call.  Typically
     *                  comes from {@link PendingIntent#getBroadcast}. Cannot be null.
     * @throws RuntimeException if the app already has too many callbacks registered.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public void registerNetworkCallback(@NonNull NetworkRequest request,
            @NonNull PendingIntent operation) {
        printStackTrace();
        checkPendingIntentNotNull(operation);
        try {
            mService.pendingListenForNetwork(
                    request.networkCapabilities, operation, mContext.getOpPackageName(),
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e) {
            throw convertServiceException(e);
        }
    }

    /**
     * Registers to receive notifications about changes in the application's default network. This
     * may be a physical network or a virtual network, such as a VPN that applies to the
     * application. The callbacks will continue to be called until either the application exits or
     * {@link #unregisterNetworkCallback(NetworkCallback)} is called.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #requestNetwork} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     * @param networkCallback The {@link NetworkCallback} that the system will call as the
     *                        application's default network changes.
     *                        The callback is invoked on the default internal Handler.
     * @throws RuntimeException if the app already has too many callbacks registered.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public void registerDefaultNetworkCallback(@NonNull NetworkCallback networkCallback) {
        registerDefaultNetworkCallback(networkCallback, getDefaultHandler());
    }

    /**
     * Registers to receive notifications about changes in the application's default network. This
     * may be a physical network or a virtual network, such as a VPN that applies to the
     * application. The callbacks will continue to be called until either the application exits or
     * {@link #unregisterNetworkCallback(NetworkCallback)} is called.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #requestNetwork} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     * @param networkCallback The {@link NetworkCallback} that the system will call as the
     *                        application's default network changes.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @throws RuntimeException if the app already has too many callbacks registered.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public void registerDefaultNetworkCallback(@NonNull NetworkCallback networkCallback,
            @NonNull Handler handler) {
        registerDefaultNetworkCallbackForUid(Process.INVALID_UID, networkCallback, handler);
    }

    /**
     * Registers to receive notifications about changes in the default network for the specified
     * UID. This may be a physical network or a virtual network, such as a VPN that applies to the
     * UID. The callbacks will continue to be called until either the application exits or
     * {@link #unregisterNetworkCallback(NetworkCallback)} is called.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #requestNetwork} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     * @param uid the UID for which to track default network changes.
     * @param networkCallback The {@link NetworkCallback} that the system will call as the
     *                        UID's default network changes.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @throws RuntimeException if the app already has too many callbacks registered.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @SuppressLint({"ExecutorRegistration", "PairedRegistration"})
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETTINGS})
    public void registerDefaultNetworkCallbackForUid(int uid,
            @NonNull NetworkCallback networkCallback, @NonNull Handler handler) {
        CallbackHandler cbHandler = new CallbackHandler(handler);
        sendRequestForNetwork(uid, null /* need */, networkCallback, 0 /* timeoutMs */,
                TRACK_DEFAULT, TYPE_NONE, cbHandler);
    }

    /**
     * Registers to receive notifications about changes in the system default network. The callbacks
     * will continue to be called until either the application exits or
     * {@link #unregisterNetworkCallback(NetworkCallback)} is called.
     *
     * This method should not be used to determine networking state seen by applications, because in
     * many cases, most or even all application traffic may not use the default network directly,
     * and traffic from different applications may go on different networks by default. As an
     * example, if a VPN is connected, traffic from all applications might be sent through the VPN
     * and not onto the system default network. Applications or system components desiring to do
     * determine network state as seen by applications should use other methods such as
     * {@link #registerDefaultNetworkCallback(NetworkCallback, Handler)}.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #requestNetwork} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     * @param networkCallback The {@link NetworkCallback} that the system will call as the
     *                        system default network changes.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @throws RuntimeException if the app already has too many callbacks registered.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @SuppressLint({"ExecutorRegistration", "PairedRegistration"})
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS})
    public void registerSystemDefaultNetworkCallback(@NonNull NetworkCallback networkCallback,
            @NonNull Handler handler) {
        CallbackHandler cbHandler = new CallbackHandler(handler);
        sendRequestForNetwork(null /* NetworkCapabilities need */, networkCallback, 0,
                TRACK_SYSTEM_DEFAULT, TYPE_NONE, cbHandler);
    }

    /**
     * Registers to receive notifications about the best matching network which satisfy the given
     * {@link NetworkRequest}.  The callbacks will continue to be called until
     * either the application exits or {@link #unregisterNetworkCallback(NetworkCallback)} is
     * called.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * {@link #registerNetworkCallback} and its variants and {@link #requestNetwork} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param networkCallback The {@link NetworkCallback} that the system will call as suitable
     *                        networks change state.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     * @throws RuntimeException if the app already has too many callbacks registered.
     */
    @SuppressLint("ExecutorRegistration")
    public void registerBestMatchingNetworkCallback(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback, @NonNull Handler handler) {
        final NetworkCapabilities nc = request.networkCapabilities;
        final CallbackHandler cbHandler = new CallbackHandler(handler);
        sendRequestForNetwork(nc, networkCallback, 0, LISTEN_FOR_BEST, TYPE_NONE, cbHandler);
    }

    /**
     * Requests bandwidth update for a given {@link Network} and returns whether the update request
     * is accepted by ConnectivityService. Once accepted, ConnectivityService will poll underlying
     * network connection for updated bandwidth information. The caller will be notified via
     * {@link ConnectivityManager.NetworkCallback} if there is an update. Notice that this
     * method assumes that the caller has previously called
     * {@link #registerNetworkCallback(NetworkRequest, NetworkCallback)} to listen for network
     * changes.
     *
     * @param network {@link Network} specifying which network you're interested.
     * @return {@code true} on success, {@code false} if the {@link Network} is no longer valid.
     */
    public boolean requestBandwidthUpdate(@NonNull Network network) {
        try {
            return mService.requestBandwidthUpdate(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Unregisters a {@code NetworkCallback} and possibly releases networks originating from
     * {@link #requestNetwork(NetworkRequest, NetworkCallback)} and
     * {@link #registerNetworkCallback(NetworkRequest, NetworkCallback)} calls.
     * If the given {@code NetworkCallback} had previously been used with {@code #requestNetwork},
     * any networks that the device brought up only to satisfy that request will be disconnected.
     *
     * Notifications that would have triggered that {@code NetworkCallback} will immediately stop
     * triggering it as soon as this call returns.
     *
     * @param networkCallback The {@link NetworkCallback} used when making the request.
     */
    public void unregisterNetworkCallback(@NonNull NetworkCallback networkCallback) {
        printStackTrace();
        checkCallbackNotNull(networkCallback);
        final List<NetworkRequest> reqs = new ArrayList<>();
        // Find all requests associated to this callback and stop callback triggers immediately.
        // Callback is reusable immediately. http://b/20701525, http://b/35921499.
        synchronized (sCallbacks) {
            if (networkCallback.networkRequest == null) {
                throw new IllegalArgumentException("NetworkCallback was not registered");
            }
            if (networkCallback.networkRequest == ALREADY_UNREGISTERED) {
                Log.d(TAG, "NetworkCallback was already unregistered");
                return;
            }
            for (Map.Entry<NetworkRequest, NetworkCallback> e : sCallbacks.entrySet()) {
                if (e.getValue() == networkCallback) {
                    reqs.add(e.getKey());
                }
            }
            // TODO: throw exception if callback was registered more than once (http://b/20701525).
            for (NetworkRequest r : reqs) {
                try {
                    mService.releaseNetworkRequest(r);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
                // Only remove mapping if rpc was successful.
                sCallbacks.remove(r);
            }
            networkCallback.networkRequest = ALREADY_UNREGISTERED;
        }
    }

    /**
     * Unregisters a callback previously registered via
     * {@link #registerNetworkCallback(NetworkRequest, android.app.PendingIntent)}.
     *
     * @param operation A PendingIntent equal (as defined by {@link Intent#filterEquals}) to the
     *                  PendingIntent passed to
     *                  {@link #registerNetworkCallback(NetworkRequest, android.app.PendingIntent)}.
     *                  Cannot be null.
     */
    public void unregisterNetworkCallback(@NonNull PendingIntent operation) {
        releaseNetworkRequest(operation);
    }

    /**
     * Informs the system whether it should switch to {@code network} regardless of whether it is
     * validated or not. If {@code accept} is true, and the network was explicitly selected by the
     * user (e.g., by selecting a Wi-Fi network in the Settings app), then the network will become
     * the system default network regardless of any other network that's currently connected. If
     * {@code always} is true, then the choice is remembered, so that the next time the user
     * connects to this network, the system will switch to it.
     *
     * @param network The network to accept.
     * @param accept Whether to accept the network even if unvalidated.
     * @param always Whether to remember this choice in the future.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK})
    public void setAcceptUnvalidated(@NonNull Network network, boolean accept, boolean always) {
        try {
            mService.setAcceptUnvalidated(network, accept, always);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Informs the system whether it should consider the network as validated even if it only has
     * partial connectivity. If {@code accept} is true, then the network will be considered as
     * validated even if connectivity is only partial. If {@code always} is true, then the choice
     * is remembered, so that the next time the user connects to this network, the system will
     * switch to it.
     *
     * @param network The network to accept.
     * @param accept Whether to consider the network as validated even if it has partial
     *               connectivity.
     * @param always Whether to remember this choice in the future.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK})
    public void setAcceptPartialConnectivity(@NonNull Network network, boolean accept,
            boolean always) {
        try {
            mService.setAcceptPartialConnectivity(network, accept, always);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Informs the system to penalize {@code network}'s score when it becomes unvalidated. This is
     * only meaningful if the system is configured not to penalize such networks, e.g., if the
     * {@code config_networkAvoidBadWifi} configuration variable is set to 0 and the {@code
     * NETWORK_AVOID_BAD_WIFI setting is unset}.
     *
     * @param network The network to accept.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_SETUP_WIZARD,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK})
    public void setAvoidUnvalidated(@NonNull Network network) {
        try {
            mService.setAvoidUnvalidated(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Temporarily allow bad Wi-Fi to override {@code config_networkAvoidBadWifi} configuration.
     *
     * @param timeMs The expired current time. The value should be set within a limited time from
     *               now.
     *
     * @hide
     */
    public void setTestAllowBadWifiUntil(long timeMs) {
        try {
            mService.setTestAllowBadWifiUntil(timeMs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Requests that the system open the captive portal app on the specified network.
     *
     * <p>This is to be used on networks where a captive portal was detected, as per
     * {@link NetworkCapabilities#NET_CAPABILITY_CAPTIVE_PORTAL}.
     *
     * @param network The network to log into.
     *
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void startCaptivePortalApp(@NonNull Network network) {
        try {
            mService.startCaptivePortalApp(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Requests that the system open the captive portal app with the specified extras.
     *
     * <p>This endpoint is exclusively for use by the NetworkStack and is protected by the
     * corresponding permission.
     * @param network Network on which the captive portal was detected.
     * @param appExtras Extras to include in the app start intent.
     * @hide
     */
    @SystemApi
    @RequiresPermission(NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK)
    public void startCaptivePortalApp(@NonNull Network network, @NonNull Bundle appExtras) {
        try {
            mService.startCaptivePortalAppInternal(network, appExtras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Determine whether the device is configured to avoid bad Wi-Fi.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            android.Manifest.permission.NETWORK_STACK})
    public boolean shouldAvoidBadWifi() {
        try {
            return mService.shouldAvoidBadWifi();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * It is acceptable to briefly use multipath data to provide seamless connectivity for
     * time-sensitive user-facing operations when the system default network is temporarily
     * unresponsive. The amount of data should be limited (less than one megabyte for every call to
     * this method), and the operation should be infrequent to ensure that data usage is limited.
     *
     * An example of such an operation might be a time-sensitive foreground activity, such as a
     * voice command, that the user is performing while walking out of range of a Wi-Fi network.
     */
    public static final int MULTIPATH_PREFERENCE_HANDOVER = 1 << 0;

    /**
     * It is acceptable to use small amounts of multipath data on an ongoing basis to provide
     * a backup channel for traffic that is primarily going over another network.
     *
     * An example might be maintaining backup connections to peers or servers for the purpose of
     * fast fallback if the default network is temporarily unresponsive or disconnects. The traffic
     * on backup paths should be negligible compared to the traffic on the main path.
     */
    public static final int MULTIPATH_PREFERENCE_RELIABILITY = 1 << 1;

    /**
     * It is acceptable to use metered data to improve network latency and performance.
     */
    public static final int MULTIPATH_PREFERENCE_PERFORMANCE = 1 << 2;

    /**
     * Return value to use for unmetered networks. On such networks we currently set all the flags
     * to true.
     * @hide
     */
    public static final int MULTIPATH_PREFERENCE_UNMETERED =
            MULTIPATH_PREFERENCE_HANDOVER |
            MULTIPATH_PREFERENCE_RELIABILITY |
            MULTIPATH_PREFERENCE_PERFORMANCE;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {
            MULTIPATH_PREFERENCE_HANDOVER,
            MULTIPATH_PREFERENCE_RELIABILITY,
            MULTIPATH_PREFERENCE_PERFORMANCE,
    })
    public @interface MultipathPreference {
    }

    /**
     * Provides a hint to the calling application on whether it is desirable to use the
     * multinetwork APIs (e.g., {@link Network#openConnection}, {@link Network#bindSocket}, etc.)
     * for multipath data transfer on this network when it is not the system default network.
     * Applications desiring to use multipath network protocols should call this method before
     * each such operation.
     *
     * @param network The network on which the application desires to use multipath data.
     *                If {@code null}, this method will return a preference that will generally
     *                apply to metered networks.
     * @return a bitwise OR of zero or more of the {@code MULTIPATH_PREFERENCE_*} constants.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public @MultipathPreference int getMultipathPreference(@Nullable Network network) {
        try {
            return mService.getMultipathPreference(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Resets all connectivity manager settings back to factory defaults.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK})
    public void factoryReset() {
        try {
            mService.factoryReset();
            getTetheringManager().stopAllTethering();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Binds the current process to {@code network}.  All Sockets created in the future
     * (and not explicitly bound via a bound SocketFactory from
     * {@link Network#getSocketFactory() Network.getSocketFactory()}) will be bound to
     * {@code network}.  All host name resolutions will be limited to {@code network} as well.
     * Note that if {@code network} ever disconnects, all Sockets created in this way will cease to
     * work and all host name resolutions will fail.  This is by design so an application doesn't
     * accidentally use Sockets it thinks are still bound to a particular {@link Network}.
     * To clear binding pass {@code null} for {@code network}.  Using individually bound
     * Sockets created by Network.getSocketFactory().createSocket() and
     * performing network-specific host name resolutions via
     * {@link Network#getAllByName Network.getAllByName} is preferred to calling
     * {@code bindProcessToNetwork}.
     *
     * @param network The {@link Network} to bind the current process to, or {@code null} to clear
     *                the current binding.
     * @return {@code true} on success, {@code false} if the {@link Network} is no longer valid.
     */
    public boolean bindProcessToNetwork(@Nullable Network network) {
        // Forcing callers to call through non-static function ensures ConnectivityManager
        // instantiated.
        return setProcessDefaultNetwork(network);
    }

    /**
     * Binds the current process to {@code network}.  All Sockets created in the future
     * (and not explicitly bound via a bound SocketFactory from
     * {@link Network#getSocketFactory() Network.getSocketFactory()}) will be bound to
     * {@code network}.  All host name resolutions will be limited to {@code network} as well.
     * Note that if {@code network} ever disconnects, all Sockets created in this way will cease to
     * work and all host name resolutions will fail.  This is by design so an application doesn't
     * accidentally use Sockets it thinks are still bound to a particular {@link Network}.
     * To clear binding pass {@code null} for {@code network}.  Using individually bound
     * Sockets created by Network.getSocketFactory().createSocket() and
     * performing network-specific host name resolutions via
     * {@link Network#getAllByName Network.getAllByName} is preferred to calling
     * {@code setProcessDefaultNetwork}.
     *
     * @param network The {@link Network} to bind the current process to, or {@code null} to clear
     *                the current binding.
     * @return {@code true} on success, {@code false} if the {@link Network} is no longer valid.
     * @deprecated This function can throw {@link IllegalStateException}.  Use
     *             {@link #bindProcessToNetwork} instead.  {@code bindProcessToNetwork}
     *             is a direct replacement.
     */
    @Deprecated
    public static boolean setProcessDefaultNetwork(@Nullable Network network) {
        int netId = (network == null) ? NETID_UNSET : network.netId;
        boolean isSameNetId = (netId == NetworkUtils.getBoundNetworkForProcess());

        if (netId != NETID_UNSET) {
            netId = network.getNetIdForResolv();
        }

        if (!NetworkUtils.bindProcessToNetwork(netId)) {
            return false;
        }

        if (!isSameNetId) {
            // Set HTTP proxy system properties to match network.
            // TODO: Deprecate this static method and replace it with a non-static version.
            try {
                Proxy.setHttpProxyConfiguration(getInstance().getDefaultProxy());
            } catch (SecurityException e) {
                // The process doesn't have ACCESS_NETWORK_STATE, so we can't fetch the proxy.
                Log.e(TAG, "Can't set proxy properties", e);
            }
            // Must flush DNS cache as new network may have different DNS resolutions.
            InetAddress.clearDnsCache();
            // Must flush socket pool as idle sockets will be bound to previous network and may
            // cause subsequent fetches to be performed on old network.
            NetworkEventDispatcher.getInstance().dispatchNetworkConfigurationChange();
        }

        return true;
    }

    /**
     * Returns the {@link Network} currently bound to this process via
     * {@link #bindProcessToNetwork}, or {@code null} if no {@link Network} is explicitly bound.
     *
     * @return {@code Network} to which this process is bound, or {@code null}.
     */
    @Nullable
    public Network getBoundNetworkForProcess() {
        // Forcing callers to call through non-static function ensures ConnectivityManager has been
        // instantiated.
        return getProcessDefaultNetwork();
    }

    /**
     * Returns the {@link Network} currently bound to this process via
     * {@link #bindProcessToNetwork}, or {@code null} if no {@link Network} is explicitly bound.
     *
     * @return {@code Network} to which this process is bound, or {@code null}.
     * @deprecated Using this function can lead to other functions throwing
     *             {@link IllegalStateException}.  Use {@link #getBoundNetworkForProcess} instead.
     *             {@code getBoundNetworkForProcess} is a direct replacement.
     */
    @Deprecated
    @Nullable
    public static Network getProcessDefaultNetwork() {
        int netId = NetworkUtils.getBoundNetworkForProcess();
        if (netId == NETID_UNSET) return null;
        return new Network(netId);
    }

    private void unsupportedStartingFrom(int version) {
        if (Process.myUid() == Process.SYSTEM_UID) {
            // The getApplicationInfo() call we make below is not supported in system context. Let
            // the call through here, and rely on the fact that ConnectivityService will refuse to
            // allow the system to use these APIs anyway.
            return;
        }

        if (mContext.getApplicationInfo().targetSdkVersion >= version) {
            throw new UnsupportedOperationException(
                    "This method is not supported in target SDK version " + version + " and above");
        }
    }

    // Checks whether the calling app can use the legacy routing API (startUsingNetworkFeature,
    // stopUsingNetworkFeature, requestRouteToHost), and if not throw UnsupportedOperationException.
    // TODO: convert the existing system users (Tethering, GnssLocationProvider) to the new APIs and
    // remove these exemptions. Note that this check is not secure, and apps can still access these
    // functions by accessing ConnectivityService directly. However, it should be clear that doing
    // so is unsupported and may break in the future. http://b/22728205
    private void checkLegacyRoutingApiAccess() {
        unsupportedStartingFrom(VERSION_CODES.M);
    }

    /**
     * Binds host resolutions performed by this process to {@code network}.
     * {@link #bindProcessToNetwork} takes precedence over this setting.
     *
     * @param network The {@link Network} to bind host resolutions from the current process to, or
     *                {@code null} to clear the current binding.
     * @return {@code true} on success, {@code false} if the {@link Network} is no longer valid.
     * @hide
     * @deprecated This is strictly for legacy usage to support {@link #startUsingNetworkFeature}.
     */
    @Deprecated
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static boolean setProcessDefaultNetworkForHostResolution(Network network) {
        return NetworkUtils.bindProcessToNetworkForHostResolution(
                (network == null) ? NETID_UNSET : network.getNetIdForResolv());
    }

    /**
     * Device is not restricting metered network activity while application is running on
     * background.
     */
    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;

    /**
     * Device is restricting metered network activity while application is running on background,
     * but application is allowed to bypass it.
     * <p>
     * In this state, application should take action to mitigate metered network access.
     * For example, a music streaming application should switch to a low-bandwidth bitrate.
     */
    public static final int RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2;

    /**
     * Device is restricting metered network activity while application is running on background.
     * <p>
     * In this state, application should not try to use the network while running on background,
     * because it would be denied.
     */
    public static final int RESTRICT_BACKGROUND_STATUS_ENABLED = 3;

    /**
     * A change in the background metered network activity restriction has occurred.
     * <p>
     * Applications should call {@link #getRestrictBackgroundStatus()} to check if the restriction
     * applies to them.
     * <p>
     * This is only sent to registered receivers, not manifest receivers.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_RESTRICT_BACKGROUND_CHANGED =
            "android.net.conn.RESTRICT_BACKGROUND_CHANGED";

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = false, value = {
            RESTRICT_BACKGROUND_STATUS_DISABLED,
            RESTRICT_BACKGROUND_STATUS_WHITELISTED,
            RESTRICT_BACKGROUND_STATUS_ENABLED,
    })
    public @interface RestrictBackgroundStatus {
    }

    /**
     * Determines if the calling application is subject to metered network restrictions while
     * running on background.
     *
     * @return {@link #RESTRICT_BACKGROUND_STATUS_DISABLED},
     * {@link #RESTRICT_BACKGROUND_STATUS_ENABLED},
     * or {@link #RESTRICT_BACKGROUND_STATUS_WHITELISTED}
     */
    public @RestrictBackgroundStatus int getRestrictBackgroundStatus() {
        try {
            return mService.getRestrictBackgroundStatusByCaller();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * The network watchlist is a list of domains and IP addresses that are associated with
     * potentially harmful apps. This method returns the SHA-256 of the watchlist config file
     * currently used by the system for validation purposes.
     *
     * @return Hash of network watchlist config file. Null if config does not exist.
     */
    @Nullable
    public byte[] getNetworkWatchlistConfigHash() {
        try {
            return mService.getNetworkWatchlistConfigHash();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get watchlist config hash");
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the {@code uid} of the owner of a network connection.
     *
     * @param protocol The protocol of the connection. Only {@code IPPROTO_TCP} and {@code
     *     IPPROTO_UDP} currently supported.
     * @param local The local {@link InetSocketAddress} of a connection.
     * @param remote The remote {@link InetSocketAddress} of a connection.
     * @return {@code uid} if the connection is found and the app has permission to observe it
     *     (e.g., if it is associated with the calling VPN app's VpnService tunnel) or {@link
     *     android.os.Process#INVALID_UID} if the connection is not found.
     * @throws SecurityException if the caller is not the active VpnService for the current
     *     user.
     * @throws IllegalArgumentException if an unsupported protocol is requested.
     */
    public int getConnectionOwnerUid(
            int protocol, @NonNull InetSocketAddress local, @NonNull InetSocketAddress remote) {
        ConnectionInfo connectionInfo = new ConnectionInfo(protocol, local, remote);
        try {
            return mService.getConnectionOwnerUid(connectionInfo);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void printStackTrace() {
        if (DEBUG) {
            final StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
            final StringBuffer sb = new StringBuffer();
            for (int i = 3; i < callStack.length; i++) {
                final String stackTrace = callStack[i].toString();
                if (stackTrace == null || stackTrace.contains("android.os")) {
                    break;
                }
                sb.append(" [").append(stackTrace).append("]");
            }
            Log.d(TAG, "StackLog:" + sb.toString());
        }
    }

    /** @hide */
    public TestNetworkManager startOrGetTestNetworkManager() {
        final IBinder tnBinder;
        try {
            tnBinder = mService.startOrGetTestNetworkService();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

        return new TestNetworkManager(ITestNetworkManager.Stub.asInterface(tnBinder));
    }

    /** @hide */
    public ConnectivityDiagnosticsManager createDiagnosticsManager() {
        return new ConnectivityDiagnosticsManager(mContext, mService);
    }

    /**
     * Simulates a Data Stall for the specified Network.
     *
     * <p>This method should only be used for tests.
     *
     * <p>The caller must be the owner of the specified Network. This simulates a data stall to
     * have the system behave as if it had happened, but does not actually stall connectivity.
     *
     * @param detectionMethod The detection method used to identify the Data Stall.
     *                        See ConnectivityDiagnosticsManager.DataStallReport.DETECTION_METHOD_*.
     * @param timestampMillis The timestamp at which the stall 'occurred', in milliseconds, as per
     *                        SystemClock.elapsedRealtime.
     * @param network The Network for which a Data Stall is being simluated.
     * @param extras The PersistableBundle of extras included in the Data Stall notification.
     * @throws SecurityException if the caller is not the owner of the given network.
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {android.Manifest.permission.MANAGE_TEST_NETWORKS,
            android.Manifest.permission.NETWORK_STACK})
    public void simulateDataStall(@DetectionMethod int detectionMethod, long timestampMillis,
            @NonNull Network network, @NonNull PersistableBundle extras) {
        try {
            mService.simulateDataStall(detectionMethod, timestampMillis, network, extras);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    @NonNull
    private final List<QosCallbackConnection> mQosCallbackConnections = new ArrayList<>();

    /**
     * Registers a {@link QosSocketInfo} with an associated {@link QosCallback}.  The callback will
     * receive available QoS events related to the {@link Network} and local ip + port
     * specified within socketInfo.
     * <p/>
     * The same {@link QosCallback} must be unregistered before being registered a second time,
     * otherwise {@link QosCallbackRegistrationException} is thrown.
     * <p/>
     * This API does not, in itself, require any permission if called with a network that is not
     * restricted. However, the underlying implementation currently only supports the IMS network,
     * which is always restricted. That means non-preinstalled callers can't possibly find this API
     * useful, because they'd never be called back on networks that they would have access to.
     *
     * @throws SecurityException if {@link QosSocketInfo#getNetwork()} is restricted and the app is
     * missing CONNECTIVITY_USE_RESTRICTED_NETWORKS permission.
     * @throws QosCallback.QosCallbackRegistrationException if qosCallback is already registered.
     * @throws RuntimeException if the app already has too many callbacks registered.
     *
     * Exceptions after the time of registration is passed through
     * {@link QosCallback#onError(QosCallbackException)}.  see: {@link QosCallbackException}.
     *
     * @param socketInfo the socket information used to match QoS events
     * @param executor The executor on which the callback will be invoked. The provided
     *                 {@link Executor} must run callback sequentially, otherwise the order of
     *                 callbacks cannot be guaranteed.onQosCallbackRegistered
     * @param callback receives qos events that satisfy socketInfo
     *
     * @hide
     */
    @SystemApi
    public void registerQosCallback(@NonNull final QosSocketInfo socketInfo,
            @CallbackExecutor @NonNull final Executor executor,
            @NonNull final QosCallback callback) {
        Objects.requireNonNull(socketInfo, "socketInfo must be non-null");
        Objects.requireNonNull(executor, "executor must be non-null");
        Objects.requireNonNull(callback, "callback must be non-null");

        try {
            synchronized (mQosCallbackConnections) {
                if (getQosCallbackConnection(callback) == null) {
                    final QosCallbackConnection connection =
                            new QosCallbackConnection(this, callback, executor);
                    mQosCallbackConnections.add(connection);
                    mService.registerQosSocketCallback(socketInfo, connection);
                } else {
                    Log.e(TAG, "registerQosCallback: Callback already registered");
                    throw new QosCallbackRegistrationException();
                }
            }
        } catch (final RemoteException e) {
            Log.e(TAG, "registerQosCallback: Error while registering ", e);

            // The same unregister method method is called for consistency even though nothing
            // will be sent to the ConnectivityService since the callback was never successfully
            // registered.
            unregisterQosCallback(callback);
            e.rethrowFromSystemServer();
        } catch (final ServiceSpecificException e) {
            Log.e(TAG, "registerQosCallback: Error while registering ", e);
            unregisterQosCallback(callback);
            throw convertServiceException(e);
        }
    }

    /**
     * Unregisters the given {@link QosCallback}.  The {@link QosCallback} will no longer receive
     * events once unregistered and can be registered a second time.
     * <p/>
     * If the {@link QosCallback} does not have an active registration, it is a no-op.
     *
     * @param callback the callback being unregistered
     *
     * @hide
     */
    @SystemApi
    public void unregisterQosCallback(@NonNull final QosCallback callback) {
        Objects.requireNonNull(callback, "The callback must be non-null");
        try {
            synchronized (mQosCallbackConnections) {
                final QosCallbackConnection connection = getQosCallbackConnection(callback);
                if (connection != null) {
                    connection.stopReceivingMessages();
                    mService.unregisterQosCallback(connection);
                    mQosCallbackConnections.remove(connection);
                } else {
                    Log.d(TAG, "unregisterQosCallback: Callback not registered");
                }
            }
        } catch (final RemoteException e) {
            Log.e(TAG, "unregisterQosCallback: Error while unregistering ", e);
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets the connection related to the callback.
     *
     * @param callback the callback to look up
     * @return the related connection
     */
    @Nullable
    private QosCallbackConnection getQosCallbackConnection(final QosCallback callback) {
        for (final QosCallbackConnection connection : mQosCallbackConnections) {
            // Checking by reference here is intentional
            if (connection.getCallback() == callback) {
                return connection;
            }
        }
        return null;
    }

    /**
     * Request a network to satisfy a set of {@link NetworkCapabilities}, but
     * does not cause any networks to retain the NET_CAPABILITY_FOREGROUND capability. This can
     * be used to request that the system provide a network without causing the network to be
     * in the foreground.
     *
     * <p>This method will attempt to find the best network that matches the passed
     * {@link NetworkRequest}, and to bring up one that does if none currently satisfies the
     * criteria. The platform will evaluate which network is the best at its own discretion.
     * Throughput, latency, cost per byte, policy, user preference and other considerations
     * may be factored in the decision of what is considered the best network.
     *
     * <p>As long as this request is outstanding, the platform will try to maintain the best network
     * matching this request, while always attempting to match the request to a better network if
     * possible. If a better match is found, the platform will switch this request to the now-best
     * network and inform the app of the newly best network by invoking
     * {@link NetworkCallback#onAvailable(Network)} on the provided callback. Note that the platform
     * will not try to maintain any other network than the best one currently matching the request:
     * a network not matching any network request may be disconnected at any time.
     *
     * <p>For example, an application could use this method to obtain a connected cellular network
     * even if the device currently has a data connection over Ethernet. This may cause the cellular
     * radio to consume additional power. Or, an application could inform the system that it wants
     * a network supporting sending MMSes and have the system let it know about the currently best
     * MMS-supporting network through the provided {@link NetworkCallback}.
     *
     * <p>The status of the request can be followed by listening to the various callbacks described
     * in {@link NetworkCallback}. The {@link Network} object passed to the callback methods can be
     * used to direct traffic to the network (although accessing some networks may be subject to
     * holding specific permissions). Callers will learn about the specific characteristics of the
     * network through
     * {@link NetworkCallback#onCapabilitiesChanged(Network, NetworkCapabilities)} and
     * {@link NetworkCallback#onLinkPropertiesChanged(Network, LinkProperties)}. The methods of the
     * provided {@link NetworkCallback} will only be invoked due to changes in the best network
     * matching the request at any given time; therefore when a better network matching the request
     * becomes available, the {@link NetworkCallback#onAvailable(Network)} method is called
     * with the new network after which no further updates are given about the previously-best
     * network, unless it becomes the best again at some later time. All callbacks are invoked
     * in order on the same thread, which by default is a thread created by the framework running
     * in the app.
     *
     * <p>This{@link NetworkRequest} will live until released via
     * {@link #unregisterNetworkCallback(NetworkCallback)} or the calling application exits, at
     * which point the system may let go of the network at any time.
     *
     * <p>It is presently unsupported to request a network with mutable
     * {@link NetworkCapabilities} such as
     * {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED} or
     * {@link NetworkCapabilities#NET_CAPABILITY_CAPTIVE_PORTAL}
     * as these {@code NetworkCapabilities} represent states that a particular
     * network may never attain, and whether a network will attain these states
     * is unknown prior to bringing up the network so the framework does not
     * know how to go about satisfying a request with these capabilities.
     *
     * <p>To avoid performance issues due to apps leaking callbacks, the system will limit the
     * number of outstanding requests to 100 per app (identified by their UID), shared with
     * all variants of this method, of {@link #registerNetworkCallback} as well as
     * {@link ConnectivityDiagnosticsManager#registerConnectivityDiagnosticsCallback}.
     * Requesting a network with this method will count toward this limit. If this limit is
     * exceeded, an exception will be thrown. To avoid hitting this issue and to conserve resources,
     * make sure to unregister the callbacks with
     * {@link #unregisterNetworkCallback(NetworkCallback)}.
     *
     * @param request {@link NetworkRequest} describing this request.
     * @param networkCallback The {@link NetworkCallback} to be utilized for this request. Note
     *                        the callback must not be shared - it uniquely specifies this request.
     * @param handler {@link Handler} to specify the thread upon which the callback will be invoked.
     *                If null, the callback is invoked on the default internal Handler.
     * @throws IllegalArgumentException if {@code request} contains invalid network capabilities.
     * @throws SecurityException if missing the appropriate permissions.
     * @throws RuntimeException if the app already has too many callbacks registered.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @SuppressLint("ExecutorRegistration")
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void requestBackgroundNetwork(@NonNull NetworkRequest request,
            @NonNull NetworkCallback networkCallback,
            @SuppressLint("ListenerLast") @NonNull Handler handler) {
        final NetworkCapabilities nc = request.networkCapabilities;
        sendRequestForNetwork(nc, networkCallback, 0, BACKGROUND_REQUEST,
                TYPE_NONE, new CallbackHandler(handler));
    }

    /**
     * Used by automotive devices to set the network preferences used to direct traffic at an
     * application level as per the given OemNetworkPreferences. An example use-case would be an
     * automotive OEM wanting to provide connectivity for applications critical to the usage of a
     * vehicle via a particular network.
     *
     * Calling this will overwrite the existing preference.
     *
     * @param preference {@link OemNetworkPreferences} The application network preference to be set.
     * @param executor the executor on which listener will be invoked.
     * @param listener {@link OnSetOemNetworkPreferenceListener} optional listener used to
     *                  communicate completion of setOemNetworkPreference(). This will only be
     *                  called once upon successful completion of setOemNetworkPreference().
     * @throws IllegalArgumentException if {@code preference} contains invalid preference values.
     * @throws SecurityException if missing the appropriate permissions.
     * @throws UnsupportedOperationException if called on a non-automotive device.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.CONTROL_OEM_PAID_NETWORK_PREFERENCE)
    public void setOemNetworkPreference(@NonNull final OemNetworkPreferences preference,
            @Nullable @CallbackExecutor final Executor executor,
            @Nullable final Runnable listener) {
        Objects.requireNonNull(preference, "OemNetworkPreferences must be non-null");
        if (null != listener) {
            Objects.requireNonNull(executor, "Executor must be non-null");
        }
        final IOnCompleteListener listenerInternal = listener == null ? null :
                new IOnCompleteListener.Stub() {
                    @Override
                    public void onComplete() {
                        executor.execute(listener::run);
                    }
        };

        try {
            mService.setOemNetworkPreference(preference, listenerInternal);
        } catch (RemoteException e) {
            Log.e(TAG, "setOemNetworkPreference() failed for preference: " + preference.toString());
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Request that a user profile is put by default on a network matching a given preference.
     *
     * See the documentation for the individual preferences for a description of the supported
     * behaviors.
     *
     * @param profile the profile concerned.
     * @param preference the preference for this profile.
     * @param executor an executor to execute the listener on. Optional if listener is null.
     * @param listener an optional listener to listen for completion of the operation.
     * @throws IllegalArgumentException if {@code profile} is not a valid user profile.
     * @throws SecurityException if missing the appropriate permissions.
     * @deprecated Use {@link #setProfileNetworkPreferences(UserHandle, List, Executor, Runnable)}
     * instead as it provides a more flexible API with more options.
     * @hide
     */
    // This function is for establishing per-profile default networking and can only be called by
    // the device policy manager, running as the system server. It would make no sense to call it
    // on a context for a user because it does not establish a setting on behalf of a user, rather
    // it establishes a setting for a user on behalf of the DPM.
    @SuppressLint({"UserHandle"})
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    @Deprecated
    public void setProfileNetworkPreference(@NonNull final UserHandle profile,
            @ProfileNetworkPreferencePolicy final int preference,
            @Nullable @CallbackExecutor final Executor executor,
            @Nullable final Runnable listener) {

        ProfileNetworkPreference.Builder preferenceBuilder =
                new ProfileNetworkPreference.Builder();
        preferenceBuilder.setPreference(preference);
        if (preference != PROFILE_NETWORK_PREFERENCE_DEFAULT) {
            preferenceBuilder.setPreferenceEnterpriseId(NET_ENTERPRISE_ID_1);
        }
        setProfileNetworkPreferences(profile,
                List.of(preferenceBuilder.build()), executor, listener);
    }

    /**
     * Set a list of default network selection policies for a user profile.
     *
     * Calling this API with a user handle defines the entire policy for that user handle.
     * It will overwrite any setting previously set for the same user profile,
     * and not affect previously set settings for other handles.
     *
     * Call this API with an empty list to remove settings for this user profile.
     *
     * See {@link ProfileNetworkPreference} for more details on each preference
     * parameter.
     *
     * @param profile the user profile for which the preference is being set.
     * @param profileNetworkPreferences the list of profile network preferences for the
     *        provided profile.
     * @param executor an executor to execute the listener on. Optional if listener is null.
     * @param listener an optional listener to listen for completion of the operation.
     * @throws IllegalArgumentException if {@code profile} is not a valid user profile.
     * @throws SecurityException if missing the appropriate permissions.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(android.Manifest.permission.NETWORK_STACK)
    public void setProfileNetworkPreferences(
            @NonNull final UserHandle profile,
            @NonNull List<ProfileNetworkPreference>  profileNetworkPreferences,
            @Nullable @CallbackExecutor final Executor executor,
            @Nullable final Runnable listener) {
        if (null != listener) {
            Objects.requireNonNull(executor, "Pass a non-null executor, or a null listener");
        }
        final IOnCompleteListener proxy;
        if (null == listener) {
            proxy = null;
        } else {
            proxy = new IOnCompleteListener.Stub() {
                @Override
                public void onComplete() {
                    executor.execute(listener::run);
                }
            };
        }
        try {
            mService.setProfileNetworkPreferences(profile, profileNetworkPreferences, proxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    // The first network ID of IPSec tunnel interface.
    private static final int TUN_INTF_NETID_START = 0xFC00; // 0xFC00 = 64512
    // The network ID range of IPSec tunnel interface.
    private static final int TUN_INTF_NETID_RANGE = 0x0400; // 0x0400 = 1024

    /**
     * Get the network ID range reserved for IPSec tunnel interfaces.
     *
     * @return A Range which indicates the network ID range of IPSec tunnel interface.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @NonNull
    public static Range<Integer> getIpSecNetIdRange() {
        return new Range(TUN_INTF_NETID_START, TUN_INTF_NETID_START + TUN_INTF_NETID_RANGE - 1);
    }

    /**
     * Sets data saver switch.
     *
     * <p>This API configures the bandwidth control, and filling data saver status in BpfMap,
     * which is intended for internal use by the network stack to optimize performance
     * when frequently checking data saver status for multiple uids without doing IPC.
     * It does not directly control the global data saver mode that users manage in settings.
     * To query the comprehensive data saver status for a specific UID, including allowlist
     * considerations, use {@link #getRestrictBackgroundStatus}.
     *
     * @param enable True if enable.
     * @throws IllegalStateException if failed.
     * @hide
     */
    @FlaggedApi(Flags.FLAG_SET_DATA_SAVER_VIA_CM)
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void setDataSaverEnabled(final boolean enable) {
        try {
            mService.setDataSaverEnabled(enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Adds the specified UID to the list of UIds that are allowed to use data on metered networks
     * even when background data is restricted. The deny list takes precedence over the allow list.
     *
     * @param uid uid of target app
     * @throws IllegalStateException if updating allow list failed.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void addUidToMeteredNetworkAllowList(final int uid) {
        try {
            mService.setUidFirewallRule(FIREWALL_CHAIN_METERED_ALLOW, uid, FIREWALL_RULE_ALLOW);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Removes the specified UID from the list of UIDs that are allowed to use background data on
     * metered networks when background data is restricted. The deny list takes precedence over
     * the allow list.
     *
     * @param uid uid of target app
     * @throws IllegalStateException if updating allow list failed.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void removeUidFromMeteredNetworkAllowList(final int uid) {
        try {
            mService.setUidFirewallRule(FIREWALL_CHAIN_METERED_ALLOW, uid, FIREWALL_RULE_DENY);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Adds the specified UID to the list of UIDs that are not allowed to use background data on
     * metered networks. Takes precedence over {@link #addUidToMeteredNetworkAllowList}.
     *
     * On V+, {@link #setUidFirewallRule} should be used with
     * {@link #FIREWALL_CHAIN_METERED_DENY_USER} or {@link #FIREWALL_CHAIN_METERED_DENY_ADMIN}
     * based on the reason so that users can receive {@link #BLOCKED_METERED_REASON_USER_RESTRICTED}
     * or {@link #BLOCKED_METERED_REASON_ADMIN_DISABLED}, respectively.
     * This API always uses {@link #FIREWALL_CHAIN_METERED_DENY_USER}.
     *
     * @param uid uid of target app
     * @throws IllegalStateException if updating deny list failed.
     * @hide
     */
    // TODO(b/332649177): Deprecate this API after V
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void addUidToMeteredNetworkDenyList(final int uid) {
        try {
            mService.setUidFirewallRule(FIREWALL_CHAIN_METERED_DENY_USER, uid, FIREWALL_RULE_DENY);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Removes the specified UID from the list of UIDs that can use background data on metered
     * networks if background data is not restricted. The deny list takes precedence over the
     * allow list.
     *
     * On V+, {@link #setUidFirewallRule} should be used with
     * {@link #FIREWALL_CHAIN_METERED_DENY_USER} or {@link #FIREWALL_CHAIN_METERED_DENY_ADMIN}
     * based on the reason so that users can receive {@link #BLOCKED_METERED_REASON_USER_RESTRICTED}
     * or {@link #BLOCKED_METERED_REASON_ADMIN_DISABLED}, respectively.
     * This API always uses {@link #FIREWALL_CHAIN_METERED_DENY_USER}.
     *
     * @param uid uid of target app
     * @throws IllegalStateException if updating deny list failed.
     * @hide
     */
    // TODO(b/332649177): Deprecate this API after V
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void removeUidFromMeteredNetworkDenyList(final int uid) {
        try {
            mService.setUidFirewallRule(FIREWALL_CHAIN_METERED_DENY_USER, uid, FIREWALL_RULE_ALLOW);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets a firewall rule for the specified UID on the specified chain.
     *
     * @param chain target chain.
     * @param uid uid to allow/deny.
     * @param rule firewall rule to allow/drop packets.
     * @throws IllegalStateException if updating firewall rule failed.
     * @throws IllegalArgumentException if {@code rule} is not a valid rule.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void setUidFirewallRule(@FirewallChain final int chain, final int uid,
            @FirewallRule final int rule) {
        try {
            mService.setUidFirewallRule(chain, uid, rule);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get firewall rule of specified firewall chain on specified uid.
     *
     * @param chain target chain.
     * @param uid   target uid
     * @return either FIREWALL_RULE_ALLOW or FIREWALL_RULE_DENY
     * @throws UnsupportedOperationException if called on pre-T devices.
     * @throws ServiceSpecificException in case of failure, with an error code indicating the
     *                                  cause of the failure.
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public int getUidFirewallRule(@FirewallChain final int chain, final int uid) {
        try {
            return mService.getUidFirewallRule(chain, uid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enables or disables the specified firewall chain.
     *
     * Note that metered firewall chains can not be controlled by this API.
     * See {@link #FIREWALL_CHAIN_METERED_ALLOW}, {@link #FIREWALL_CHAIN_METERED_DENY_USER}, and
     * {@link #FIREWALL_CHAIN_METERED_DENY_ADMIN} for more detail.
     *
     * @param chain target chain.
     * @param enable whether the chain should be enabled.
     * @throws UnsupportedOperationException if called on pre-T devices.
     * @throws IllegalStateException if enabling or disabling the firewall chain failed.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void setFirewallChainEnabled(@FirewallChain final int chain, final boolean enable) {
        try {
            mService.setFirewallChainEnabled(chain, enable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the specified firewall chain's status.
     *
     * @param chain target chain.
     * @return {@code true} if chain is enabled, {@code false} if chain is disabled.
     * @throws UnsupportedOperationException if called on pre-T devices.
     * @throws ServiceSpecificException in case of failure, with an error code indicating the
     *                                  cause of the failure.
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public boolean getFirewallChainEnabled(@FirewallChain final int chain) {
        try {
            return mService.getFirewallChainEnabled(chain);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Replaces the contents of the specified UID-based firewall chain.
     *
     * @param chain target chain to replace.
     * @param uids The list of UIDs to be placed into chain.
     * @throws UnsupportedOperationException if called on pre-T devices.
     * @throws IllegalArgumentException if {@code chain} is not a valid chain.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public void replaceFirewallChain(@FirewallChain final int chain, @NonNull final int[] uids) {
        Objects.requireNonNull(uids);
        try {
            mService.replaceFirewallChain(chain, uids);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return whether the network is blocked for the given uid and metered condition.
     *
     * Similar to {@link NetworkPolicyManager#isUidNetworkingBlocked}, but directly reads the BPF
     * maps and therefore considerably faster. For use by the NetworkStack process only.
     *
     * @param uid The target uid.
     * @param isNetworkMetered Whether the target network is metered.
     *
     * @return True if all networking with the given condition is blocked. Otherwise, false.
     * @throws IllegalStateException if the map cannot be opened.
     * @throws ServiceSpecificException if the read fails.
     * @hide
     */
    // This isn't protected by a standard Android permission since it can't
    // afford to do IPC for performance reasons. Instead, the access control
    // is provided by linux file group permission AID_NET_BW_ACCT and the
    // selinux context fs_bpf_net*.
    // Only the system server process and the network stack have access.
    @FlaggedApi(Flags.FLAG_SUPPORT_IS_UID_NETWORKING_BLOCKED)
    @SystemApi(client = MODULE_LIBRARIES)
    // Note b/326143935 kernel bug can trigger crash on some T device.
    @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresPermission(NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK)
    public boolean isUidNetworkingBlocked(int uid, boolean isNetworkMetered) {
        if (!SdkLevel.isAtLeastU()) {
            throw new IllegalStateException(
                    "isUidNetworkingBlocked is not supported on pre-U devices");
        }
        final NetworkStackBpfNetMaps reader = NetworkStackBpfNetMaps.getInstance();
        // Note that before V, the data saver status in bpf is written by ConnectivityService
        // when receiving {@link #ACTION_RESTRICT_BACKGROUND_CHANGED}. Thus,
        // the status is not synchronized.
        // On V+, the data saver status is set by platform code when enabling/disabling
        // data saver, which is synchronized.
        return reader.isUidNetworkingBlocked(uid, isNetworkMetered);
    }

    /** @hide */
    public IBinder getCompanionDeviceManagerProxyService() {
        try {
            return mService.getCompanionDeviceManagerProxyService();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    @RequiresApi(Build.VERSION_CODES.S)
    public IBinder getRoutingCoordinatorService() {
        try {
            return mService.getRoutingCoordinatorService();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get the specified ConnectivityService feature status. This method is for test code to check
     * whether the feature is enabled or not.
     * Note that tests can not just read DeviceConfig since ConnectivityService reads flag at
     * startup. For example, it's possible that the current flag value is "disable"(-1) but the
     * feature is enabled since the flag value was "enable"(1) when ConnectivityService started up.
     * If the ConnectivityManager needs to check the ConnectivityService feature status for non-test
     * purpose, define feature in {@link ConnectivityManagerFeature} and use
     * {@link #isFeatureEnabled} instead.
     *
     * @param featureFlag  target flag for feature
     * @return {@code true} if the feature is enabled, {@code false} if the feature is disabled.
     * @throws IllegalArgumentException if the flag is invalid
     *
     * @hide
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.NETWORK_SETTINGS,
            android.Manifest.permission.NETWORK_STACK,
            NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK
    })
    public boolean isConnectivityServiceFeatureEnabledForTesting(final String featureFlag) {
        try {
            return mService.isConnectivityServiceFeatureEnabledForTesting(featureFlag);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
