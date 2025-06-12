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

package android.net;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.NetworkUtilsInternal;
import com.android.internal.net.VpnConfig;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * VpnService is a base class for applications to extend and build their
 * own VPN solutions. In general, it creates a virtual network interface,
 * configures addresses and routing rules, and returns a file descriptor
 * to the application. Each read from the descriptor retrieves an outgoing
 * packet which was routed to the interface. Each write to the descriptor
 * injects an incoming packet just like it was received from the interface.
 * The interface is running on Internet Protocol (IP), so packets are
 * always started with IP headers. The application then completes a VPN
 * connection by processing and exchanging packets with the remote server
 * over a tunnel.
 *
 * <p>Letting applications intercept packets raises huge security concerns.
 * A VPN application can easily break the network. Besides, two of them may
 * conflict with each other. The system takes several actions to address
 * these issues. Here are some key points:
 * <ul>
 *   <li>User action is required the first time an application creates a VPN
 *       connection.</li>
 *   <li>There can be only one VPN connection running at the same time. The
 *       existing interface is deactivated when a new one is created.</li>
 *   <li>A system-managed notification is shown during the lifetime of a
 *       VPN connection.</li>
 *   <li>A system-managed dialog gives the information of the current VPN
 *       connection. It also provides a button to disconnect.</li>
 *   <li>The network is restored automatically when the file descriptor is
 *       closed. It also covers the cases when a VPN application is crashed
 *       or killed by the system.</li>
 * </ul>
 *
 * <p>There are two primary methods in this class: {@link #prepare} and
 * {@link Builder#establish}. The former deals with user action and stops
 * the VPN connection created by another application. The latter creates
 * a VPN interface using the parameters supplied to the {@link Builder}.
 * An application must call {@link #prepare} to grant the right to use
 * other methods in this class, and the right can be revoked at any time.
 * Here are the general steps to create a VPN connection:
 * <ol>
 *   <li>When the user presses the button to connect, call {@link #prepare}
 *       and launch the returned intent, if non-null.</li>
 *   <li>When the application becomes prepared, start the service.</li>
 *   <li>Create a tunnel to the remote server and negotiate the network
 *       parameters for the VPN connection.</li>
 *   <li>Supply those parameters to a {@link Builder} and create a VPN
 *       interface by calling {@link Builder#establish}.</li>
 *   <li>Process and exchange packets between the tunnel and the returned
 *       file descriptor.</li>
 *   <li>When {@link #onRevoke} is invoked, close the file descriptor and
 *       shut down the tunnel gracefully.</li>
 * </ol>
 *
 * <p>Services extending this class need to be declared with an appropriate
 * permission and intent filter. Their access must be secured by
 * {@link android.Manifest.permission#BIND_VPN_SERVICE} permission, and
 * their intent filter must match {@link #SERVICE_INTERFACE} action. Here
 * is an example of declaring a VPN service in {@code AndroidManifest.xml}:
 * <pre>
 * &lt;service android:name=".ExampleVpnService"
 *         android:permission="android.permission.BIND_VPN_SERVICE"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="android.net.VpnService"/&gt;
 *     &lt;/intent-filter&gt;
 * &lt;/service&gt;</pre>
 *
 * <p> The Android system starts a VPN in the background by calling
 * {@link android.content.Context#startService startService()}. In Android 8.0
 * (API level 26) and higher, the system places VPN apps on the temporary
 * allowlist for a short period so the app can start in the background. The VPN
 * app must promote itself to the foreground after it's launched or the system
 * will shut down the app.
 *
 * <h3>Developer's guide</h3>
 *
 * <p>To learn more about developing VPN apps, read the
 * <a href="{@docRoot}guide/topics/connectivity/vpn">VPN developer's guide</a>.
 *
 * @see Builder
 */
public class VpnService extends Service {

    /**
     * The action must be matched by the intent filter of this service. It also
     * needs to require {@link android.Manifest.permission#BIND_VPN_SERVICE}
     * permission so that other applications cannot abuse it.
     */
    public static final String SERVICE_INTERFACE = VpnConfig.SERVICE_INTERFACE;

    /**
     * Key for boolean meta-data field indicating whether this VpnService supports always-on mode.
     *
     * <p>For a VPN app targeting {@link android.os.Build.VERSION_CODES#N API 24} or above, Android
     * provides users with the ability to set it as always-on, so that VPN connection is
     * persisted after device reboot and app upgrade. Always-on VPN can also be enabled by device
     * owner and profile owner apps through
     * {@link DevicePolicyManager#setAlwaysOnVpnPackage}.
     *
     * <p>VPN apps not supporting this feature should opt out by adding this meta-data field to the
     * {@code VpnService} component of {@code AndroidManifest.xml}. In case there is more than one
     * {@code VpnService} component defined in {@code AndroidManifest.xml}, opting out any one of
     * them will opt out the entire app. For example,
     * <pre> {@code
     * <service android:name=".ExampleVpnService"
     *         android:permission="android.permission.BIND_VPN_SERVICE">
     *     <intent-filter>
     *         <action android:name="android.net.VpnService"/>
     *     </intent-filter>
     *     <meta-data android:name="android.net.VpnService.SUPPORTS_ALWAYS_ON"
     *             android:value=false/>
     * </service>
     * } </pre>
     *
     * <p>This meta-data field defaults to {@code true} if absent. It will only have effect on
     * {@link android.os.Build.VERSION_CODES#O_MR1} or higher.
     */
    public static final String SERVICE_META_DATA_SUPPORTS_ALWAYS_ON =
            "android.net.VpnService.SUPPORTS_ALWAYS_ON";

    /**
     * Use IVpnManager since those methods are hidden and not available in VpnManager.
     */
    private static IVpnManager getService() {
        return IVpnManager.Stub.asInterface(
                ServiceManager.getService(Context.VPN_MANAGEMENT_SERVICE));
    }

    /**
     * Prepare to establish a VPN connection. This method returns {@code null}
     * if the VPN application is already prepared or if the user has previously
     * consented to the VPN application. Otherwise, it returns an
     * {@link Intent} to a system activity. The application should launch the
     * activity using {@link Activity#startActivityForResult} to get itself
     * prepared. The activity may pop up a dialog to require user action, and
     * the result will come back via its {@link Activity#onActivityResult}.
     * If the result is {@link Activity#RESULT_OK}, the application becomes
     * prepared and is granted to use other methods in this class.
     *
     * <p>Only one application can be granted at the same time. The right
     * is revoked when another application is granted. The application
     * losing the right will be notified via its {@link #onRevoke}. Unless
     * it becomes prepared again, subsequent calls to other methods in this
     * class will fail.
     *
     * <p>The user may disable the VPN at any time while it is activated, in
     * which case this method will return an intent the next time it is
     * executed to obtain the user's consent again.
     *
     * @see #onRevoke
     */
    public static Intent prepare(Context context) {
        try {
            if (getService().prepareVpn(context.getPackageName(), null, context.getUserId())) {
                return null;
            }
        } catch (RemoteException e) {
            // ignore
        }
        return VpnConfig.getIntentForConfirmation();
    }

    /**
     * Version of {@link #prepare(Context)} which does not require user consent.
     *
     * <p>Requires {@link android.Manifest.permission#CONTROL_VPN} and should generally not be
     * used. Only acceptable in situations where user consent has been obtained through other means.
     *
     * <p>Once this is run, future preparations may be done with the standard prepare method as this
     * will authorize the package to prepare the VPN without consent in the future.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.CONTROL_VPN)
    public static void prepareAndAuthorize(Context context) {
        IVpnManager vm = getService();
        String packageName = context.getPackageName();
        try {
            // Only prepare if we're not already prepared.
            int userId = context.getUserId();
            if (!vm.prepareVpn(packageName, null, userId)) {
                vm.prepareVpn(null, packageName, userId);
            }
            vm.setVpnPackageAuthorization(packageName, userId, VpnManager.TYPE_VPN_SERVICE);
        } catch (RemoteException e) {
            // ignore
        }
    }

    /**
     * Protect a socket from VPN connections. After protecting, data sent
     * through this socket will go directly to the underlying network,
     * so its traffic will not be forwarded through the VPN.
     * This method is useful if some connections need to be kept
     * outside of VPN. For example, a VPN tunnel should protect itself if its
     * destination is covered by VPN routes. Otherwise its outgoing packets
     * will be sent back to the VPN interface and cause an infinite loop. This
     * method will fail if the application is not prepared or is revoked.
     *
     * <p class="note">The socket is NOT closed by this method.
     *
     * @return {@code true} on success.
     */
    public boolean protect(int socket) {
        return NetworkUtilsInternal.protectFromVpn(socket);
    }

    /**
     * Convenience method to protect a {@link Socket} from VPN connections.
     *
     * @return {@code true} on success.
     * @see #protect(int)
     */
    public boolean protect(Socket socket) {
        return protect(socket.getFileDescriptor$().getInt$());
    }

    /**
     * Convenience method to protect a {@link DatagramSocket} from VPN
     * connections.
     *
     * @return {@code true} on success.
     * @see #protect(int)
     */
    public boolean protect(DatagramSocket socket) {
        return protect(socket.getFileDescriptor$().getInt$());
    }

    /**
     * Adds a network address to the VPN interface.
     *
     * Both IPv4 and IPv6 addresses are supported. The VPN must already be established. Fails if the
     * address is already in use or cannot be assigned to the interface for any other reason.
     *
     * Adding an address implicitly allows traffic from that address family (i.e., IPv4 or IPv6) to
     * be routed over the VPN. @see Builder#allowFamily
     *
     * @throws IllegalArgumentException if the address is invalid.
     *
     * @param address The IP address (IPv4 or IPv6) to assign to the VPN interface.
     * @param prefixLength The prefix length of the address.
     *
     * @return {@code true} on success.
     * @see Builder#addAddress
     *
     * @hide
     */
    public boolean addAddress(InetAddress address, int prefixLength) {
        check(address, prefixLength);
        try {
            return getService().addVpnAddress(address.getHostAddress(), prefixLength);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Removes a network address from the VPN interface.
     *
     * Both IPv4 and IPv6 addresses are supported. The VPN must already be established. Fails if the
     * address is not assigned to the VPN interface, or if it is the only address assigned (thus
     * cannot be removed), or if the address cannot be removed for any other reason.
     *
     * After removing an address, if there are no addresses, routes or DNS servers of a particular
     * address family (i.e., IPv4 or IPv6) configured on the VPN, that <b>DOES NOT</b> block that
     * family from being routed. In other words, once an address family has been allowed, it stays
     * allowed for the rest of the VPN's session. @see Builder#allowFamily
     *
     * @throws IllegalArgumentException if the address is invalid.
     *
     * @param address The IP address (IPv4 or IPv6) to assign to the VPN interface.
     * @param prefixLength The prefix length of the address.
     *
     * @return {@code true} on success.
     *
     * @hide
     */
    public boolean removeAddress(InetAddress address, int prefixLength) {
        check(address, prefixLength);
        try {
            return getService().removeVpnAddress(address.getHostAddress(), prefixLength);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Sets the underlying networks used by the VPN for its upstream connections.
     *
     * <p>Used by the system to know the actual networks that carry traffic for apps affected by
     * this VPN in order to present this information to the user (e.g., via status bar icons).
     *
     * <p>This method only needs to be called if the VPN has explicitly bound its underlying
     * communications channels &mdash; such as the socket(s) passed to {@link #protect(int)} &mdash;
     * to a {@code Network} using APIs such as {@link Network#bindSocket(Socket)} or
     * {@link Network#bindSocket(DatagramSocket)}. The VPN should call this method every time
     * the set of {@code Network}s it is using changes.
     *
     * <p>{@code networks} is one of the following:
     * <ul>
     * <li><strong>a non-empty array</strong>: an array of one or more {@link Network}s, in
     * decreasing preference order. For example, if this VPN uses both wifi and mobile (cellular)
     * networks to carry app traffic, but prefers or uses wifi more than mobile, wifi should appear
     * first in the array.</li>
     * <li><strong>an empty array</strong>: a zero-element array, meaning that the VPN has no
     * underlying network connection, and thus, app traffic will not be sent or received.</li>
     * <li><strong>null</strong>: (default) signifies that the VPN uses whatever is the system's
     * default network. I.e., it doesn't use the {@code bindSocket} or {@code bindDatagramSocket}
     * APIs mentioned above to send traffic over specific channels.</li>
     * </ul>
     *
     * <p>This call will succeed only if the VPN is currently established. For setting this value
     * when the VPN has not yet been established, see {@link Builder#setUnderlyingNetworks}.
     *
     * @param networks An array of networks the VPN uses to tunnel traffic to/from its servers.
     *
     * @return {@code true} on success.
     */
    public boolean setUnderlyingNetworks(Network[] networks) {
        try {
            return getService().setUnderlyingNetworksForVpn(networks);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns whether the service is running in always-on VPN mode. In this mode the system ensures
     * that the service is always running by restarting it when necessary, e.g. after reboot.
     *
     * @see DevicePolicyManager#setAlwaysOnVpnPackage(ComponentName, String, boolean, Set)
     */
    public final boolean isAlwaysOn() {
        try {
            return getService().isCallerCurrentAlwaysOnVpnApp();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns whether the service is running in always-on VPN lockdown mode. In this mode the
     * system ensures that the service is always running and that the apps aren't allowed to bypass
     * the VPN.
     *
     * @see DevicePolicyManager#setAlwaysOnVpnPackage(ComponentName, String, boolean, Set)
     */
    public final boolean isLockdownEnabled() {
        try {
            return getService().isCallerCurrentAlwaysOnVpnLockdownApp();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Return the communication interface to the service. This method returns
     * {@code null} on {@link Intent}s other than {@link #SERVICE_INTERFACE}
     * action. Applications overriding this method must identify the intent
     * and return the corresponding interface accordingly.
     *
     * @see Service#onBind
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null && SERVICE_INTERFACE.equals(intent.getAction())) {
            return new Callback();
        }
        return null;
    }

    /**
     * Invoked when the application is revoked. At this moment, the VPN
     * interface is already deactivated by the system. The application should
     * close the file descriptor and shut down gracefully. The default
     * implementation of this method is calling {@link Service#stopSelf()}.
     *
     * <p class="note">Calls to this method may not happen on the main thread
     * of the process.
     *
     * @see #prepare
     */
    public void onRevoke() {
        stopSelf();
    }

    /**
     * Use raw Binder instead of AIDL since now there is only one usage.
     */
    private class Callback extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (code == IBinder.LAST_CALL_TRANSACTION) {
                onRevoke();
                return true;
            }
            return false;
        }
    }

    /**
     * Private method to validate address and prefixLength.
     */
    private static void check(InetAddress address, int prefixLength) {
        if (address.isLoopbackAddress()) {
            throw new IllegalArgumentException("Bad address");
        }
        if (address instanceof Inet4Address) {
            if (prefixLength < 0 || prefixLength > 32) {
                throw new IllegalArgumentException("Bad prefixLength");
            }
        } else if (address instanceof Inet6Address) {
            if (prefixLength < 0 || prefixLength > 128) {
                throw new IllegalArgumentException("Bad prefixLength");
            }
        } else {
            throw new IllegalArgumentException("Unsupported family");
        }
    }

    private static void checkNonPrefixBytes(@NonNull InetAddress address, int prefixLength) {
        final IpPrefix prefix = new IpPrefix(address, prefixLength);
        if (!prefix.getAddress().equals(address)) {
            throw new IllegalArgumentException("Bad address");
        }
    }

    /**
     * Helper class to create a VPN interface. This class should be always
     * used within the scope of the outer {@link VpnService}.
     *
     * @see VpnService
     */
    public class Builder {

        private final VpnConfig mConfig = new VpnConfig();
        @UnsupportedAppUsage
        private final List<LinkAddress> mAddresses = new ArrayList<>();
        @UnsupportedAppUsage
        private final List<RouteInfo> mRoutes = new ArrayList<>();

        public Builder() {
            mConfig.user = VpnService.this.getClass().getName();
        }

        /**
         * Set the name of this session. It will be displayed in
         * system-managed dialogs and notifications. This is recommended
         * not required.
         */
        @NonNull
        public Builder setSession(@NonNull String session) {
            mConfig.session = session;
            return this;
        }

        /**
         * Set the {@link PendingIntent} to an activity for users to
         * configure the VPN connection. If it is not set, the button
         * to configure will not be shown in system-managed dialogs.
         */
        @NonNull
        public Builder setConfigureIntent(@NonNull PendingIntent intent) {
            mConfig.configureIntent = intent;
            return this;
        }

        /**
         * Set the maximum transmission unit (MTU) of the VPN interface. If
         * it is not set, the default value in the operating system will be
         * used.
         *
         * @throws IllegalArgumentException if the value is not positive.
         */
        @NonNull
        public Builder setMtu(int mtu) {
            if (mtu <= 0) {
                throw new IllegalArgumentException("Bad mtu");
            }
            mConfig.mtu = mtu;
            return this;
        }

        /**
         * Sets an HTTP proxy for the VPN network.
         * <p class="note">This proxy is only a recommendation and it is possible that some apps
         * will ignore it.
         * <p class="note">PAC proxies are not supported over VPNs.
         * <p class="note">Apps that do use the proxy cannot distinguish between routes handled
         * and not handled by the VPN and will try to access HTTP resources over the proxy
         * regardless of the destination. In practice this means using a proxy with a split
         * tunnel generally won't work as expected, because HTTP accesses on routes not handled by
         * the VPN will not reach as the proxy won't be available outside of the VPN network.
         */
        @NonNull
        public Builder setHttpProxy(@NonNull ProxyInfo proxyInfo) {
            mConfig.proxyInfo = proxyInfo;
            return this;
        }

        /**
         * Add a network address to the VPN interface. Both IPv4 and IPv6
         * addresses are supported. At least one address must be set before
         * calling {@link #establish}.
         *
         * Adding an address implicitly allows traffic from that address family
         * (i.e., IPv4 or IPv6) to be routed over the VPN. @see #allowFamily
         *
         * @throws IllegalArgumentException if the address is invalid.
         */
        @NonNull
        public Builder addAddress(@NonNull InetAddress address, int prefixLength) {
            check(address, prefixLength);

            if (address.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Bad address");
            }
            mAddresses.add(new LinkAddress(address, prefixLength));
            return this;
        }

        /**
         * Convenience method to add a network address to the VPN interface
         * using a numeric address string. See {@link InetAddress} for the
         * definitions of numeric address formats.
         *
         * Adding an address implicitly allows traffic from that address family
         * (i.e., IPv4 or IPv6) to be routed over the VPN. @see #allowFamily
         *
         * @throws IllegalArgumentException if the address is invalid.
         * @see #addAddress(InetAddress, int)
         */
        @NonNull
        public Builder addAddress(@NonNull String address, int prefixLength) {
            return addAddress(InetAddress.parseNumericAddress(address), prefixLength);
        }

        /**
         * Add a network route to the VPN interface. Both IPv4 and IPv6
         * routes are supported.
         *
         * If a route with the same destination is already present, its type will be updated.
         *
         * @throws IllegalArgumentException if the route is invalid.
         */
        @NonNull
        private Builder addRoute(@NonNull IpPrefix prefix, int type) {
            check(prefix.getAddress(), prefix.getPrefixLength());

            final RouteInfo newRoute = new RouteInfo(prefix, /* gateway */
                    null, /* interface */ null, type);

            final int index = findRouteIndexByDestination(newRoute);

            if (index == -1) {
                mRoutes.add(newRoute);
            } else {
                mRoutes.set(index, newRoute);
            }

            return this;
        }

        /**
         * Add a network route to the VPN interface. Both IPv4 and IPv6
         * routes are supported.
         *
         * Adding a route implicitly allows traffic from that address family
         * (i.e., IPv4 or IPv6) to be routed over the VPN. @see #allowFamily
         *
         * Calling this method overrides previous calls to {@link #excludeRoute} for the same
         * destination.
         *
         * If multiple routes match the packet destination, route with the longest prefix takes
         * precedence.
         *
         * @throws IllegalArgumentException if the route is invalid.
         */
        @NonNull
        public Builder addRoute(@NonNull InetAddress address, int prefixLength) {
            checkNonPrefixBytes(address, prefixLength);

            return addRoute(new IpPrefix(address, prefixLength), RouteInfo.RTN_UNICAST);
        }

        /**
         * Add a network route to the VPN interface. Both IPv4 and IPv6
         * routes are supported.
         *
         * Adding a route implicitly allows traffic from that address family
         * (i.e., IPv4 or IPv6) to be routed over the VPN. @see #allowFamily
         *
         * Calling this method overrides previous calls to {@link #excludeRoute} for the same
         * destination.
         *
         * If multiple routes match the packet destination, route with the longest prefix takes
         * precedence.
         *
         * @throws IllegalArgumentException if the route is invalid.
         */
        @NonNull
        public Builder addRoute(@NonNull IpPrefix prefix) {
            return addRoute(prefix, RouteInfo.RTN_UNICAST);
        }

        /**
         * Convenience method to add a network route to the VPN interface
         * using a numeric address string. See {@link InetAddress} for the
         * definitions of numeric address formats.
         *
         * Adding a route implicitly allows traffic from that address family
         * (i.e., IPv4 or IPv6) to be routed over the VPN. @see #allowFamily
         *
         * Calling this method overrides previous calls to {@link #excludeRoute} for the same
         * destination.
         *
         * If multiple routes match the packet destination, route with the longest prefix takes
         * precedence.
         *
         * @throws IllegalArgumentException if the route is invalid.
         * @see #addRoute(InetAddress, int)
         */
        @NonNull
        public Builder addRoute(@NonNull String address, int prefixLength) {
            return addRoute(InetAddress.parseNumericAddress(address), prefixLength);
        }

        /**
         * Exclude a network route from the VPN interface. Both IPv4 and IPv6
         * routes are supported.
         *
         * Calling this method overrides previous calls to {@link #addRoute} for the same
         * destination.
         *
         * If multiple routes match the packet destination, route with the longest prefix takes
         * precedence.
         *
         * @throws IllegalArgumentException if the route is invalid.
         */
        @NonNull
        public Builder excludeRoute(@NonNull IpPrefix prefix) {
            return addRoute(prefix, RouteInfo.RTN_THROW);
        }

        /**
         * Add a DNS server to the VPN connection. Both IPv4 and IPv6
         * addresses are supported. If none is set, the DNS servers of
         * the default network will be used.
         *
         * Adding a server implicitly allows traffic from that address family
         * (i.e., IPv4 or IPv6) to be routed over the VPN. @see #allowFamily
         *
         * @throws IllegalArgumentException if the address is invalid.
         */
        @NonNull
        public Builder addDnsServer(@NonNull InetAddress address) {
            if (address.isLoopbackAddress() || address.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Bad address");
            }
            if (mConfig.dnsServers == null) {
                mConfig.dnsServers = new ArrayList<String>();
            }
            mConfig.dnsServers.add(address.getHostAddress());
            return this;
        }

        /**
         * Convenience method to add a DNS server to the VPN connection
         * using a numeric address string. See {@link InetAddress} for the
         * definitions of numeric address formats.
         *
         * Adding a server implicitly allows traffic from that address family
         * (i.e., IPv4 or IPv6) to be routed over the VPN. @see #allowFamily
         *
         * @throws IllegalArgumentException if the address is invalid.
         * @see #addDnsServer(InetAddress)
         */
        @NonNull
        public Builder addDnsServer(@NonNull String address) {
            return addDnsServer(InetAddress.parseNumericAddress(address));
        }

        /**
         * Add a search domain to the DNS resolver.
         */
        @NonNull
        public Builder addSearchDomain(@NonNull String domain) {
            if (mConfig.searchDomains == null) {
                mConfig.searchDomains = new ArrayList<String>();
            }
            mConfig.searchDomains.add(domain);
            return this;
        }

        /**
         * Allows traffic from the specified address family.
         *
         * By default, if no address, route or DNS server of a specific family (IPv4 or IPv6) is
         * added to this VPN, then all outgoing traffic of that family is blocked. If any address,
         * route or DNS server is added, that family is allowed.
         *
         * This method allows an address family to be unblocked even without adding an address,
         * route or DNS server of that family. Traffic of that family will then typically
         * fall-through to the underlying network if it's supported.
         *
         * {@code family} must be either {@code AF_INET} (for IPv4) or {@code AF_INET6} (for IPv6).
         * {@link IllegalArgumentException} is thrown if it's neither.
         *
         * @param family The address family ({@code AF_INET} or {@code AF_INET6}) to allow.
         *
         * @return this {@link Builder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Builder allowFamily(int family) {
            if (family == AF_INET) {
                mConfig.allowIPv4 = true;
            } else if (family == AF_INET6) {
                mConfig.allowIPv6 = true;
            } else {
                throw new IllegalArgumentException(family + " is neither " + AF_INET + " nor " +
                        AF_INET6);
            }
            return this;
        }

        private void verifyApp(String packageName) throws PackageManager.NameNotFoundException {
            IPackageManager pm = IPackageManager.Stub.asInterface(
                    ServiceManager.getService("package"));
            try {
                pm.getApplicationInfo(packageName, 0, UserHandle.getCallingUserId());
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Adds an application that's allowed to access the VPN connection.
         *
         * If this method is called at least once, only applications added through this method (and
         * no others) are allowed access. Else (if this method is never called), all applications
         * are allowed by default.  If some applications are added, other, un-added applications
         * will use networking as if the VPN wasn't running.
         *
         * A {@link Builder} may have only a set of allowed applications OR a set of disallowed
         * ones, but not both. Calling this method after {@link #addDisallowedApplication} has
         * already been called, or vice versa, will throw an {@link UnsupportedOperationException}.
         *
         * {@code packageName} must be the canonical name of a currently installed application.
         * {@link PackageManager.NameNotFoundException} is thrown if there's no such application.
         *
         * @throws PackageManager.NameNotFoundException If the application isn't installed.
         *
         * @param packageName The full name (e.g.: "com.google.apps.contacts") of an application.
         *
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        @NonNull
        public Builder addAllowedApplication(@NonNull String packageName)
                throws PackageManager.NameNotFoundException {
            if (mConfig.disallowedApplications != null) {
                throw new UnsupportedOperationException("addDisallowedApplication already called");
            }
            verifyApp(packageName);
            if (mConfig.allowedApplications == null) {
                mConfig.allowedApplications = new ArrayList<String>();
            }
            mConfig.allowedApplications.add(packageName);
            return this;
        }

        /**
         * Adds an application that's denied access to the VPN connection.
         *
         * By default, all applications are allowed access, except for those denied through this
         * method.  Denied applications will use networking as if the VPN wasn't running.
         *
         * A {@link Builder} may have only a set of allowed applications OR a set of disallowed
         * ones, but not both. Calling this method after {@link #addAllowedApplication} has already
         * been called, or vice versa, will throw an {@link UnsupportedOperationException}.
         *
         * {@code packageName} must be the canonical name of a currently installed application.
         * {@link PackageManager.NameNotFoundException} is thrown if there's no such application.
         *
         * @throws PackageManager.NameNotFoundException If the application isn't installed.
         *
         * @param packageName The full name (e.g.: "com.google.apps.contacts") of an application.
         *
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        @NonNull
        public Builder addDisallowedApplication(@NonNull String packageName)
                throws PackageManager.NameNotFoundException {
            if (mConfig.allowedApplications != null) {
                throw new UnsupportedOperationException("addAllowedApplication already called");
            }
            verifyApp(packageName);
            if (mConfig.disallowedApplications == null) {
                mConfig.disallowedApplications = new ArrayList<String>();
            }
            mConfig.disallowedApplications.add(packageName);
            return this;
        }

        /**
         * Allows all apps to bypass this VPN connection.
         *
         * By default, all traffic from apps is forwarded through the VPN interface and it is not
         * possible for apps to side-step the VPN. If this method is called, apps may use methods
         * such as {@link ConnectivityManager#bindProcessToNetwork} to instead send/receive
         * directly over the underlying network or any other network they have permissions for.
         *
         * @return this {@link Builder} object to facilitate chaining of method calls.
         */
        @NonNull
        public Builder allowBypass() {
            mConfig.allowBypass = true;
            return this;
        }

        /**
         * Sets the VPN interface's file descriptor to be in blocking/non-blocking mode.
         *
         * By default, the file descriptor returned by {@link #establish} is non-blocking.
         *
         * @param blocking True to put the descriptor into blocking mode; false for non-blocking.
         *
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        @NonNull
        public Builder setBlocking(boolean blocking) {
            mConfig.blocking = blocking;
            return this;
        }

        /**
         * Sets the underlying networks used by the VPN for its upstream connections.
         *
         * @see VpnService#setUnderlyingNetworks
         *
         * @param networks An array of networks the VPN uses to tunnel traffic to/from its servers.
         *
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        @NonNull
        public Builder setUnderlyingNetworks(@Nullable Network[] networks) {
            mConfig.underlyingNetworks = networks != null ? networks.clone() : null;
            return this;
        }

        /**
         * Marks the VPN network as metered. A VPN network is classified as metered when the user is
         * sensitive to heavy data usage due to monetary costs and/or data limitations. In such
         * cases, you should set this to {@code true} so that apps on the system can avoid doing
         * large data transfers. Otherwise, set this to {@code false}. Doing so would cause VPN
         * network to inherit its meteredness from its underlying networks.
         *
         * <p>VPN apps targeting {@link android.os.Build.VERSION_CODES#Q} or above will be
         * considered metered by default.
         *
         * @param isMetered {@code true} if VPN network should be treated as metered regardless of
         *     underlying network meteredness
         * @return this {@link Builder} object to facilitate chaining method calls
         * @see #setUnderlyingNetworks(Network[])
         * @see ConnectivityManager#isActiveNetworkMetered()
         */
        @NonNull
        public Builder setMetered(boolean isMetered) {
            mConfig.isMetered = isMetered;
            return this;
        }

        /**
         * Create a VPN interface using the parameters supplied to this
         * builder. The interface works on IP packets, and a file descriptor
         * is returned for the application to access them. Each read
         * retrieves an outgoing packet which was routed to the interface.
         * Each write injects an incoming packet just like it was received
         * from the interface. The file descriptor is put into non-blocking
         * mode by default to avoid blocking Java threads. To use the file
         * descriptor completely in native space, see
         * {@link ParcelFileDescriptor#detachFd()}. The application MUST
         * close the file descriptor when the VPN connection is terminated.
         * The VPN interface will be removed and the network will be
         * restored by the system automatically.
         *
         * <p>To avoid conflicts, there can be only one active VPN interface
         * at the same time. Usually network parameters are never changed
         * during the lifetime of a VPN connection. It is also common for an
         * application to create a new file descriptor after closing the
         * previous one. However, it is rare but not impossible to have two
         * interfaces while performing a seamless handover. In this case, the
         * old interface will be deactivated when the new one is created
         * successfully. Both file descriptors are valid but now outgoing
         * packets will be routed to the new interface. Therefore, after
         * draining the old file descriptor, the application MUST close it
         * and start using the new file descriptor. If the new interface
         * cannot be created, the existing interface and its file descriptor
         * remain untouched.
         *
         * <p>An exception will be thrown if the interface cannot be created
         * for any reason. However, this method returns {@code null} if the
         * application is not prepared or is revoked. This helps solve
         * possible race conditions between other VPN applications.
         *
         * @return {@link ParcelFileDescriptor} of the VPN interface, or
         *         {@code null} if the application is not prepared.
         * @throws IllegalArgumentException if a parameter is not accepted
         *         by the operating system.
         * @throws IllegalStateException if a parameter cannot be applied
         *         by the operating system.
         * @throws SecurityException if the service is not properly declared
         *         in {@code AndroidManifest.xml}.
         * @see VpnService
         */
        @Nullable
        public ParcelFileDescriptor establish() {
            mConfig.addresses = mAddresses;
            mConfig.routes = mRoutes;

            try {
                return getService().establishVpn(mConfig);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }

        private int findRouteIndexByDestination(RouteInfo route) {
            for (int i = 0; i < mRoutes.size(); i++) {
                if (mRoutes.get(i).getDestination().equals(route.getDestination())) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Method for testing, to observe mRoutes while builder is being used.
         * @hide
         */
        @VisibleForTesting
        public List<RouteInfo> routes() {
            return Collections.unmodifiableList(mRoutes);
        }
    }
}
