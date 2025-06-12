/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static android.net.connectivity.ConnectivityCompatChanges.EXCLUDED_ROUTES;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.app.compat.CompatChanges;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.modules.utils.build.SdkLevel;
import com.android.net.module.util.CollectionUtils;
import com.android.net.module.util.LinkPropertiesUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Describes the properties of a network link.
 *
 * A link represents a connection to a network.
 * It may have multiple addresses and multiple gateways,
 * multiple dns servers but only one http proxy and one
 * network interface.
 *
 * Note that this is just a holder of data.  Modifying it
 * does not affect live networks.
 *
 */
public final class LinkProperties implements Parcelable {
    // The interface described by the network link.
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private String mIfaceName;
    private final ArrayList<LinkAddress> mLinkAddresses = new ArrayList<>();
    private final ArrayList<InetAddress> mDnses = new ArrayList<>();
    // PCSCF addresses are addresses of SIP proxies that only exist for the IMS core service.
    private final ArrayList<InetAddress> mPcscfs = new ArrayList<InetAddress>();
    private final ArrayList<InetAddress> mValidatedPrivateDnses = new ArrayList<>();
    private boolean mUsePrivateDns;
    private String mPrivateDnsServerName;
    private String mDomains;
    private ArrayList<RouteInfo> mRoutes = new ArrayList<>();
    private Inet4Address mDhcpServerAddress;
    private ProxyInfo mHttpProxy;
    private int mMtu;
    // in the format "rmem_min,rmem_def,rmem_max,wmem_min,wmem_def,wmem_max"
    private String mTcpBufferSizes;
    private IpPrefix mNat64Prefix;
    private boolean mWakeOnLanSupported;
    private Uri mCaptivePortalApiUrl;
    private CaptivePortalData mCaptivePortalData;

    /**
     * Indicates whether parceling should preserve fields that are set based on permissions of
     * the process receiving the {@link LinkProperties}.
     */
    private final transient boolean mParcelSensitiveFields;

    private static final int MIN_MTU    = 68;

    private static final int MIN_MTU_V6 = 1280;

    private static final int MAX_MTU    = 10000;

    private static final int INET6_ADDR_LENGTH = 16;

    // Stores the properties of links that are "stacked" above this link.
    // Indexed by interface name to allow modification and to prevent duplicates being added.
    private Hashtable<String, LinkProperties> mStackedLinks = new Hashtable<>();

    /**
     * @hide
     */
    @UnsupportedAppUsage(implicitMember =
            "values()[Landroid/net/LinkProperties$ProvisioningChange;")
    public enum ProvisioningChange {
        @UnsupportedAppUsage
        STILL_NOT_PROVISIONED,
        @UnsupportedAppUsage
        LOST_PROVISIONING,
        @UnsupportedAppUsage
        GAINED_PROVISIONING,
        @UnsupportedAppUsage
        STILL_PROVISIONED,
    }

    /**
     * Compare the provisioning states of two LinkProperties instances.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static ProvisioningChange compareProvisioning(
            LinkProperties before, LinkProperties after) {
        if (before.isProvisioned() && after.isProvisioned()) {
            // On dual-stack networks, DHCPv4 renewals can occasionally fail.
            // When this happens, IPv6-reachable services continue to function
            // normally but IPv4-only services (naturally) fail.
            //
            // When an application using an IPv4-only service reports a bad
            // network condition to the framework, attempts to re-validate
            // the network succeed (since we support IPv6-only networks) and
            // nothing is changed.
            //
            // For users, this is confusing and unexpected behaviour, and is
            // not necessarily easy to diagnose.  Therefore, we treat changing
            // from a dual-stack network to an IPv6-only network equivalent to
            // a total loss of provisioning.
            //
            // For one such example of this, see b/18867306.
            //
            // Additionally, losing IPv6 provisioning can result in TCP
            // connections getting stuck until timeouts fire and other
            // baffling failures. Therefore, loss of either IPv4 or IPv6 on a
            // previously dual-stack network is deemed a lost of provisioning.
            if ((before.isIpv4Provisioned() && !after.isIpv4Provisioned())
                    || (before.isIpv6Provisioned() && !after.isIpv6Provisioned())) {
                return ProvisioningChange.LOST_PROVISIONING;
            }
            return ProvisioningChange.STILL_PROVISIONED;
        } else if (before.isProvisioned() && !after.isProvisioned()) {
            return ProvisioningChange.LOST_PROVISIONING;
        } else if (!before.isProvisioned() && after.isProvisioned()) {
            return ProvisioningChange.GAINED_PROVISIONING;
        } else {  // !before.isProvisioned() && !after.isProvisioned()
            return ProvisioningChange.STILL_NOT_PROVISIONED;
        }
    }

    /**
     * Constructs a new {@code LinkProperties} with default values.
     */
    public LinkProperties() {
        mParcelSensitiveFields = false;
    }

    /**
     * @hide
     */
    @SystemApi
    public LinkProperties(@Nullable LinkProperties source) {
        this(source, false /* parcelSensitiveFields */);
    }

    /**
     * Create a copy of a {@link LinkProperties} that may preserve fields that were set
     * based on the permissions of the process that originally received it.
     *
     * <p>By default {@link LinkProperties} does not preserve such fields during parceling, as
     * they should not be shared outside of the process that receives them without appropriate
     * checks.
     * @param parcelSensitiveFields Whether the sensitive fields should be kept when parceling
     * @hide
     */
    @SystemApi
    public LinkProperties(@Nullable LinkProperties source, boolean parcelSensitiveFields) {
        mParcelSensitiveFields = parcelSensitiveFields;
        if (source == null) return;
        mIfaceName = source.mIfaceName;
        mLinkAddresses.addAll(source.mLinkAddresses);
        mDnses.addAll(source.mDnses);
        mValidatedPrivateDnses.addAll(source.mValidatedPrivateDnses);
        mUsePrivateDns = source.mUsePrivateDns;
        mPrivateDnsServerName = source.mPrivateDnsServerName;
        mPcscfs.addAll(source.mPcscfs);
        mDomains = source.mDomains;
        mRoutes.addAll(source.mRoutes);
        mHttpProxy = (source.mHttpProxy == null) ? null : new ProxyInfo(source.mHttpProxy);
        for (LinkProperties l: source.mStackedLinks.values()) {
            addStackedLink(l);
        }
        setMtu(source.mMtu);
        setDhcpServerAddress(source.getDhcpServerAddress());
        mTcpBufferSizes = source.mTcpBufferSizes;
        mNat64Prefix = source.mNat64Prefix;
        mWakeOnLanSupported = source.mWakeOnLanSupported;
        mCaptivePortalApiUrl = source.mCaptivePortalApiUrl;
        mCaptivePortalData = source.mCaptivePortalData;
    }

    /**
     * Sets the interface name for this link.  All {@link RouteInfo} already set for this
     * will have their interface changed to match this new value.
     *
     * @param iface The name of the network interface used for this link.
     */
    public void setInterfaceName(@Nullable String iface) {
        mIfaceName = iface;
        ArrayList<RouteInfo> newRoutes = new ArrayList<>(mRoutes.size());
        for (RouteInfo route : mRoutes) {
            newRoutes.add(routeWithInterface(route));
        }
        mRoutes = newRoutes;
    }

    /**
     * Gets the interface name for this link.  May be {@code null} if not set.
     *
     * @return The interface name set for this link or {@code null}.
     */
    public @Nullable String getInterfaceName() {
        return mIfaceName;
    }

    /**
     * @hide
     */
    @SystemApi
    public @NonNull List<String> getAllInterfaceNames() {
        List<String> interfaceNames = new ArrayList<>(mStackedLinks.size() + 1);
        if (mIfaceName != null) interfaceNames.add(mIfaceName);
        for (LinkProperties stacked: mStackedLinks.values()) {
            interfaceNames.addAll(stacked.getAllInterfaceNames());
        }
        return interfaceNames;
    }

    /**
     * Returns all the addresses on this link.  We often think of a link having a single address,
     * however, particularly with Ipv6 several addresses are typical.  Note that the
     * {@code LinkProperties} actually contains {@link LinkAddress} objects which also include
     * prefix lengths for each address.  This is a simplified utility alternative to
     * {@link LinkProperties#getLinkAddresses}.
     *
     * @return An unmodifiable {@link List} of {@link InetAddress} for this link.
     * @hide
     */
    @SystemApi
    public @NonNull List<InetAddress> getAddresses() {
        final List<InetAddress> addresses = new ArrayList<>();
        for (LinkAddress linkAddress : mLinkAddresses) {
            addresses.add(linkAddress.getAddress());
        }
        return Collections.unmodifiableList(addresses);
    }

    /**
     * Returns all the addresses on this link and all the links stacked above it.
     * @hide
     */
    @UnsupportedAppUsage
    public @NonNull List<InetAddress> getAllAddresses() {
        List<InetAddress> addresses = new ArrayList<>();
        for (LinkAddress linkAddress : mLinkAddresses) {
            addresses.add(linkAddress.getAddress());
        }
        for (LinkProperties stacked: mStackedLinks.values()) {
            addresses.addAll(stacked.getAllAddresses());
        }
        return addresses;
    }

    private int findLinkAddressIndex(LinkAddress address) {
        for (int i = 0; i < mLinkAddresses.size(); i++) {
            if (mLinkAddresses.get(i).isSameAddressAs(address)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds a {@link LinkAddress} to this {@code LinkProperties} if a {@link LinkAddress} of the
     * same address/prefix does not already exist.  If it does exist it is replaced.
     * @param address The {@code LinkAddress} to add.
     * @return true if {@code address} was added or updated, false otherwise.
     * @hide
     */
    @SystemApi
    public boolean addLinkAddress(@NonNull LinkAddress address) {
        if (address == null) {
            return false;
        }
        int i = findLinkAddressIndex(address);
        if (i < 0) {
            // Address was not present. Add it.
            mLinkAddresses.add(address);
            return true;
        } else if (mLinkAddresses.get(i).equals(address)) {
            // Address was present and has same properties. Do nothing.
            return false;
        } else {
            // Address was present and has different properties. Update it.
            mLinkAddresses.set(i, address);
            return true;
        }
    }

    /**
     * Removes a {@link LinkAddress} from this {@code LinkProperties}.  Specifically, matches
     * and {@link LinkAddress} with the same address and prefix.
     *
     * @param toRemove A {@link LinkAddress} specifying the address to remove.
     * @return true if the address was removed, false if it did not exist.
     * @hide
     */
    @SystemApi
    public boolean removeLinkAddress(@NonNull LinkAddress toRemove) {
        int i = findLinkAddressIndex(toRemove);
        if (i >= 0) {
            mLinkAddresses.remove(i);
            return true;
        }
        return false;
    }

    /**
     * Returns all the {@link LinkAddress} on this link.  Typically a link will have
     * one IPv4 address and one or more IPv6 addresses.
     *
     * @return An unmodifiable {@link List} of {@link LinkAddress} for this link.
     */
    public @NonNull List<LinkAddress> getLinkAddresses() {
        return Collections.unmodifiableList(mLinkAddresses);
    }

    /**
     * Returns all the addresses on this link and all the links stacked above it.
     * @hide
     */
    @SystemApi
    public @NonNull List<LinkAddress> getAllLinkAddresses() {
        List<LinkAddress> addresses = new ArrayList<>(mLinkAddresses);
        for (LinkProperties stacked: mStackedLinks.values()) {
            addresses.addAll(stacked.getAllLinkAddresses());
        }
        return addresses;
    }

    /**
     * Replaces the {@link LinkAddress} in this {@code LinkProperties} with
     * the given {@link Collection} of {@link LinkAddress}.
     *
     * @param addresses The {@link Collection} of {@link LinkAddress} to set in this
     *                  object.
     */
    public void setLinkAddresses(@NonNull Collection<LinkAddress> addresses) {
        mLinkAddresses.clear();
        for (LinkAddress address: addresses) {
            addLinkAddress(address);
        }
    }

    /**
     * Adds the given {@link InetAddress} to the list of DNS servers, if not present.
     *
     * @param dnsServer The {@link InetAddress} to add to the list of DNS servers.
     * @return true if the DNS server was added, false if it was already present.
     * @hide
     */
    @SystemApi
    public boolean addDnsServer(@NonNull InetAddress dnsServer) {
        if (dnsServer != null && !mDnses.contains(dnsServer)) {
            mDnses.add(dnsServer);
            return true;
        }
        return false;
    }

    /**
     * Removes the given {@link InetAddress} from the list of DNS servers.
     *
     * @param dnsServer The {@link InetAddress} to remove from the list of DNS servers.
     * @return true if the DNS server was removed, false if it did not exist.
     * @hide
     */
    @SystemApi
    public boolean removeDnsServer(@NonNull InetAddress dnsServer) {
        return mDnses.remove(dnsServer);
    }

    /**
     * Replaces the DNS servers in this {@code LinkProperties} with
     * the given {@link Collection} of {@link InetAddress} objects.
     *
     * @param dnsServers The {@link Collection} of DNS servers to set in this object.
     */
    public void setDnsServers(@NonNull Collection<InetAddress> dnsServers) {
        mDnses.clear();
        for (InetAddress dnsServer: dnsServers) {
            addDnsServer(dnsServer);
        }
    }

    /**
     * Returns all the {@link InetAddress} for DNS servers on this link.
     *
     * @return An unmodifiable {@link List} of {@link InetAddress} for DNS servers on
     *         this link.
     */
    public @NonNull List<InetAddress> getDnsServers() {
        return Collections.unmodifiableList(mDnses);
    }

    /**
     * Set whether private DNS is currently in use on this network.
     *
     * @param usePrivateDns The private DNS state.
     * @hide
     */
    @SystemApi
    public void setUsePrivateDns(boolean usePrivateDns) {
        mUsePrivateDns = usePrivateDns;
    }

    /**
     * Returns whether private DNS is currently in use on this network. When
     * private DNS is in use, applications must not send unencrypted DNS
     * queries as doing so could reveal private user information. Furthermore,
     * if private DNS is in use and {@link #getPrivateDnsServerName} is not
     * {@code null}, DNS queries must be sent to the specified DNS server.
     *
     * @return {@code true} if private DNS is in use, {@code false} otherwise.
     */
    public boolean isPrivateDnsActive() {
        return mUsePrivateDns;
    }

    /**
     * Set the name of the private DNS server to which private DNS queries
     * should be sent when in strict mode. This value should be {@code null}
     * when private DNS is off or in opportunistic mode.
     *
     * @param privateDnsServerName The private DNS server name.
     * @hide
     */
    @SystemApi
    public void setPrivateDnsServerName(@Nullable String privateDnsServerName) {
        mPrivateDnsServerName = privateDnsServerName;
    }

    /**
     * Set DHCP server address.
     *
     * @param serverAddress the server address to set.
     */
    public void setDhcpServerAddress(@Nullable Inet4Address serverAddress) {
        mDhcpServerAddress = serverAddress;
    }

     /**
     * Get DHCP server address
     *
     * @return The current DHCP server address.
     */
    public @Nullable Inet4Address getDhcpServerAddress() {
        return mDhcpServerAddress;
    }

    /**
     * Returns the private DNS server name that is in use. If not {@code null},
     * private DNS is in strict mode. In this mode, applications should ensure
     * that all DNS queries are encrypted and sent to this hostname and that
     * queries are only sent if the hostname's certificate is valid. If
     * {@code null} and {@link #isPrivateDnsActive} is {@code true}, private
     * DNS is in opportunistic mode, and applications should ensure that DNS
     * queries are encrypted and sent to a DNS server returned by
     * {@link #getDnsServers}. System DNS will handle each of these cases
     * correctly, but applications implementing their own DNS lookups must make
     * sure to follow these requirements.
     *
     * @return The private DNS server name.
     */
    public @Nullable String getPrivateDnsServerName() {
        return mPrivateDnsServerName;
    }

    /**
     * Adds the given {@link InetAddress} to the list of validated private DNS servers,
     * if not present. This is distinct from the server name in that these are actually
     * resolved addresses.
     *
     * @param dnsServer The {@link InetAddress} to add to the list of validated private DNS servers.
     * @return true if the DNS server was added, false if it was already present.
     * @hide
     */
    public boolean addValidatedPrivateDnsServer(@NonNull InetAddress dnsServer) {
        if (dnsServer != null && !mValidatedPrivateDnses.contains(dnsServer)) {
            mValidatedPrivateDnses.add(dnsServer);
            return true;
        }
        return false;
    }

    /**
     * Removes the given {@link InetAddress} from the list of validated private DNS servers.
     *
     * @param dnsServer The {@link InetAddress} to remove from the list of validated private DNS
     *        servers.
     * @return true if the DNS server was removed, false if it did not exist.
     * @hide
     */
    public boolean removeValidatedPrivateDnsServer(@NonNull InetAddress dnsServer) {
        return mValidatedPrivateDnses.remove(dnsServer);
    }

    /**
     * Replaces the validated private DNS servers in this {@code LinkProperties} with
     * the given {@link Collection} of {@link InetAddress} objects.
     *
     * @param dnsServers The {@link Collection} of validated private DNS servers to set in this
     *        object.
     * @hide
     */
    @SystemApi
    public void setValidatedPrivateDnsServers(@NonNull Collection<InetAddress> dnsServers) {
        mValidatedPrivateDnses.clear();
        for (InetAddress dnsServer: dnsServers) {
            addValidatedPrivateDnsServer(dnsServer);
        }
    }

    /**
     * Returns all the {@link InetAddress} for validated private DNS servers on this link.
     * These are resolved from the private DNS server name.
     *
     * @return An unmodifiable {@link List} of {@link InetAddress} for validated private
     *         DNS servers on this link.
     * @hide
     */
    @SystemApi
    public @NonNull List<InetAddress> getValidatedPrivateDnsServers() {
        return Collections.unmodifiableList(mValidatedPrivateDnses);
    }

    /**
     * Adds the given {@link InetAddress} to the list of PCSCF servers, if not present.
     *
     * @param pcscfServer The {@link InetAddress} to add to the list of PCSCF servers.
     * @return true if the PCSCF server was added, false otherwise.
     * @hide
     */
    @SystemApi
    public boolean addPcscfServer(@NonNull InetAddress pcscfServer) {
        if (pcscfServer != null && !mPcscfs.contains(pcscfServer)) {
            mPcscfs.add(pcscfServer);
            return true;
        }
        return false;
    }

    /**
     * Removes the given {@link InetAddress} from the list of PCSCF servers.
     *
     * @param pcscfServer The {@link InetAddress} to remove from the list of PCSCF servers.
     * @return true if the PCSCF server was removed, false otherwise.
     * @hide
     */
    public boolean removePcscfServer(@NonNull InetAddress pcscfServer) {
        return mPcscfs.remove(pcscfServer);
    }

    /**
     * Replaces the PCSCF servers in this {@code LinkProperties} with
     * the given {@link Collection} of {@link InetAddress} objects.
     *
     * @param pcscfServers The {@link Collection} of PCSCF servers to set in this object.
     * @hide
     */
    @SystemApi
    public void setPcscfServers(@NonNull Collection<InetAddress> pcscfServers) {
        mPcscfs.clear();
        for (InetAddress pcscfServer: pcscfServers) {
            addPcscfServer(pcscfServer);
        }
    }

    /**
     * Returns all the {@link InetAddress} for PCSCF servers on this link.
     *
     * @return An unmodifiable {@link List} of {@link InetAddress} for PCSCF servers on
     *         this link.
     * @hide
     */
    @SystemApi
    public @NonNull List<InetAddress> getPcscfServers() {
        return Collections.unmodifiableList(mPcscfs);
    }

    /**
     * Sets the DNS domain search path used on this link.
     *
     * @param domains A {@link String} listing in priority order the comma separated
     *                domains to search when resolving host names on this link.
     */
    public void setDomains(@Nullable String domains) {
        mDomains = domains;
    }

    /**
     * Get the DNS domains search path set for this link. May be {@code null} if not set.
     *
     * @return A {@link String} containing the comma separated domains to search when resolving host
     *         names on this link or {@code null}.
     */
    public @Nullable String getDomains() {
        return mDomains;
    }

    /**
     * Sets the Maximum Transmission Unit size to use on this link.  This should not be used
     * unless the system default (1500) is incorrect.  Values less than 68 or greater than
     * 10000 will be ignored.
     *
     * @param mtu The MTU to use for this link.
     */
    public void setMtu(int mtu) {
        mMtu = mtu;
    }

    /**
     * Gets any non-default MTU size set for this link.  Note that if the default is being used
     * this will return 0.
     *
     * @return The mtu value set for this link.
     */
    public int getMtu() {
        return mMtu;
    }

    /**
     * Sets the tcp buffers sizes to be used when this link is the system default.
     * Should be of the form "rmem_min,rmem_def,rmem_max,wmem_min,wmem_def,wmem_max".
     *
     * @param tcpBufferSizes The tcp buffers sizes to use.
     *
     * @hide
     */
    @SystemApi
    public void setTcpBufferSizes(@Nullable String tcpBufferSizes) {
        mTcpBufferSizes = tcpBufferSizes;
    }

    /**
     * Gets the tcp buffer sizes. May be {@code null} if not set.
     *
     * @return the tcp buffer sizes to use when this link is the system default or {@code null}.
     *
     * @hide
     */
    @SystemApi
    public @Nullable String getTcpBufferSizes() {
        return mTcpBufferSizes;
    }

    private RouteInfo routeWithInterface(RouteInfo route) {
        return new RouteInfo(
            route.getDestination(),
            route.getGateway(),
            mIfaceName,
            route.getType(),
            route.getMtu());
    }

    private int findRouteIndexByRouteKey(RouteInfo route) {
        for (int i = 0; i < mRoutes.size(); i++) {
            if (mRoutes.get(i).getRouteKey().equals(route.getRouteKey())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds a {@link RouteInfo} to this {@code LinkProperties}. If there is a {@link RouteInfo}
     * with the same destination, gateway and interface with different properties
     * (e.g., different MTU), it will be updated. If the {@link RouteInfo} had an
     * interface name set and that differs from the interface set for this
     * {@code LinkProperties} an {@link IllegalArgumentException} will be thrown.
     * The proper course is to add either un-named or properly named {@link RouteInfo}.
     *
     * @param route A {@link RouteInfo} to add to this object.
     * @return {@code true} was added or updated, false otherwise.
     */
    public boolean addRoute(@NonNull RouteInfo route) {
        String routeIface = route.getInterface();
        if (routeIface != null && !routeIface.equals(mIfaceName)) {
            throw new IllegalArgumentException(
                    "Route added with non-matching interface: " + routeIface
                            + " vs. " + mIfaceName);
        }
        route = routeWithInterface(route);

        int i = findRouteIndexByRouteKey(route);
        if (i == -1) {
            // Route was not present. Add it.
            mRoutes.add(route);
            return true;
        } else if (mRoutes.get(i).equals(route)) {
            // Route was present and has same properties. Do nothing.
            return false;
        } else {
            // Route was present and has different properties. Update it.
            mRoutes.set(i, route);
            return true;
        }
    }

    /**
     * Removes a {@link RouteInfo} from this {@code LinkProperties}, if present. The route must
     * specify an interface and the interface must match the interface of this
     * {@code LinkProperties}, or it will not be removed.
     *
     * @param route A {@link RouteInfo} specifying the route to remove.
     * @return {@code true} if the route was removed, {@code false} if it was not present.
     *
     * @hide
     */
    @SystemApi
    public boolean removeRoute(@NonNull RouteInfo route) {
        return Objects.equals(mIfaceName, route.getInterface()) && mRoutes.remove(route);
    }

    /**
     * Returns all the {@link RouteInfo} set on this link.
     *
     * Only unicast routes are returned for apps targeting Android S or below.
     *
     * @return An unmodifiable {@link List} of {@link RouteInfo} for this link.
     */
    public @NonNull List<RouteInfo> getRoutes() {
        // Before T, there's no throw routes because VpnService is not updatable, so no need to
        // filter them out.
        if (CompatChanges.isChangeEnabled(EXCLUDED_ROUTES) || !SdkLevel.isAtLeastT()) {
            return Collections.unmodifiableList(mRoutes);
        } else {
            // Apps that added a throw route themselves (not obtaining LinkProperties from the
            // system) will not see it in getRoutes on T+ if they do not have the compat change
            // enabled (target SDK < T); but this is expected to be rare and typically only affect
            // tests creating LinkProperties themselves (like CTS v12, which is only running on S).
            return Collections.unmodifiableList(getUnicastRoutes());
        }
    }

    /**
     * Returns all the {@link RouteInfo} of type {@link RouteInfo#RTN_UNICAST} set on this link.
     */
    private @NonNull List<RouteInfo> getUnicastRoutes() {
        return CollectionUtils.filter(mRoutes, route -> route.getType() == RouteInfo.RTN_UNICAST);
    }

    /**
     * Make sure this LinkProperties instance contains routes that cover the local subnet
     * of its link addresses. Add any route that is missing.
     * @hide
     */
    public void ensureDirectlyConnectedRoutes() {
        for (LinkAddress addr : mLinkAddresses) {
            addRoute(new RouteInfo(addr, null, mIfaceName));
        }
    }

    /**
     * Returns all the routes on this link and all the links stacked above it.
     *
     * Only unicast routes are returned for apps targeting Android S or below.
     *
     * @hide
     */
    @SystemApi
    public @NonNull List<RouteInfo> getAllRoutes() {
        final List<RouteInfo> routes = new ArrayList<>(getRoutes());
        for (LinkProperties stacked: mStackedLinks.values()) {
            routes.addAll(stacked.getAllRoutes());
        }
        return routes;
    }

    /**
     * Sets the recommended {@link ProxyInfo} to use on this link, or {@code null} for none.
     * Note that Http Proxies are only a hint - the system recommends their use, but it does
     * not enforce it and applications may ignore them.
     *
     * @param proxy A {@link ProxyInfo} defining the HTTP Proxy to use on this link.
     */
    public void setHttpProxy(@Nullable ProxyInfo proxy) {
        mHttpProxy = proxy;
    }

    /**
     * Gets the recommended {@link ProxyInfo} (or {@code null}) set on this link.
     *
     * @return The {@link ProxyInfo} set on this link or {@code null}.
     */
    public @Nullable ProxyInfo getHttpProxy() {
        return mHttpProxy;
    }

    /**
     * Returns the NAT64 prefix in use on this link, if any.
     *
     * @return the NAT64 prefix or {@code null}.
     */
    public @Nullable IpPrefix getNat64Prefix() {
        return mNat64Prefix;
    }

    /**
     * Sets the NAT64 prefix in use on this link.
     *
     * Currently, only 96-bit prefixes (i.e., where the 32-bit IPv4 address is at the end of the
     * 128-bit IPv6 address) are supported or {@code null} for no prefix.
     *
     * @param prefix the NAT64 prefix.
     */
    public void setNat64Prefix(@Nullable IpPrefix prefix) {
        if (prefix != null && prefix.getPrefixLength() != 96) {
            throw new IllegalArgumentException("Only 96-bit prefixes are supported: " + prefix);
        }
        mNat64Prefix = prefix;  // IpPrefix objects are immutable.
    }

    /**
     * Adds a stacked link.
     *
     * If there is already a stacked link with the same interface name as link,
     * that link is replaced with link. Otherwise, link is added to the list
     * of stacked links.
     *
     * @param link The link to add.
     * @return true if the link was stacked, false otherwise.
     * @hide
     */
    @UnsupportedAppUsage
    public boolean addStackedLink(@NonNull LinkProperties link) {
        if (link.getInterfaceName() != null) {
            mStackedLinks.put(link.getInterfaceName(), link);
            return true;
        }
        return false;
    }

    /**
     * Removes a stacked link.
     *
     * If there is a stacked link with the given interface name, it is
     * removed. Otherwise, nothing changes.
     *
     * @param iface The interface name of the link to remove.
     * @return true if the link was removed, false otherwise.
     * @hide
     */
    public boolean removeStackedLink(@NonNull String iface) {
        LinkProperties removed = mStackedLinks.remove(iface);
        return removed != null;
    }

    /**
     * Returns all the links stacked on top of this link.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public @NonNull List<LinkProperties> getStackedLinks() {
        if (mStackedLinks.isEmpty()) {
            return Collections.emptyList();
        }
        final List<LinkProperties> stacked = new ArrayList<>();
        for (LinkProperties link : mStackedLinks.values()) {
            stacked.add(new LinkProperties(link));
        }
        return Collections.unmodifiableList(stacked);
    }

    /**
     * Clears this object to its initial state.
     */
    public void clear() {
        if (mParcelSensitiveFields) {
            throw new UnsupportedOperationException(
                    "Cannot clear LinkProperties when parcelSensitiveFields is set");
        }

        mIfaceName = null;
        mLinkAddresses.clear();
        mDnses.clear();
        mUsePrivateDns = false;
        mPrivateDnsServerName = null;
        mPcscfs.clear();
        mDomains = null;
        mRoutes.clear();
        mHttpProxy = null;
        mStackedLinks.clear();
        mMtu = 0;
        mDhcpServerAddress = null;
        mTcpBufferSizes = null;
        mNat64Prefix = null;
        mWakeOnLanSupported = false;
        mCaptivePortalApiUrl = null;
        mCaptivePortalData = null;
    }

    /**
     * Implement the Parcelable interface
     */
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        // Space as a separator, so no need for spaces at start/end of the individual fragments.
        final StringJoiner resultJoiner = new StringJoiner(" ", "{", "}");

        if (mIfaceName != null) {
            resultJoiner.add("InterfaceName:");
            resultJoiner.add(mIfaceName);
        }

        resultJoiner.add("LinkAddresses: [");
        if (!mLinkAddresses.isEmpty()) {
            resultJoiner.add(TextUtils.join(",", mLinkAddresses));
        }
        resultJoiner.add("]");

        resultJoiner.add("DnsAddresses: [");
        if (!mDnses.isEmpty()) {
            resultJoiner.add(TextUtils.join(",", mDnses));
        }
        resultJoiner.add("]");

        if (mUsePrivateDns) {
            resultJoiner.add("UsePrivateDns: true");
        }

        if (mPrivateDnsServerName != null) {
            resultJoiner.add("PrivateDnsServerName:");
            resultJoiner.add(mPrivateDnsServerName);
        }

        if (!mPcscfs.isEmpty()) {
            resultJoiner.add("PcscfAddresses: [");
            resultJoiner.add(TextUtils.join(",", mPcscfs));
            resultJoiner.add("]");
        }

        if (!mValidatedPrivateDnses.isEmpty()) {
            final StringJoiner validatedPrivateDnsesJoiner =
                    new StringJoiner(",", "ValidatedPrivateDnsAddresses: [", "]");
            for (final InetAddress addr : mValidatedPrivateDnses) {
                validatedPrivateDnsesJoiner.add(addr.getHostAddress());
            }
            resultJoiner.add(validatedPrivateDnsesJoiner.toString());
        }

        resultJoiner.add("Domains:");
        resultJoiner.add(mDomains);

        resultJoiner.add("MTU:");
        resultJoiner.add(Integer.toString(mMtu));

        if (mWakeOnLanSupported) {
            resultJoiner.add("WakeOnLanSupported: true");
        }

        if (mDhcpServerAddress != null) {
            resultJoiner.add("ServerAddress:");
            resultJoiner.add(mDhcpServerAddress.toString());
        }

        if (mCaptivePortalApiUrl != null) {
            resultJoiner.add("CaptivePortalApiUrl: " + mCaptivePortalApiUrl);
        }

        if (mCaptivePortalData != null) {
            resultJoiner.add("CaptivePortalData: " + mCaptivePortalData);
        }

        if (mTcpBufferSizes != null) {
            resultJoiner.add("TcpBufferSizes:");
            resultJoiner.add(mTcpBufferSizes);
        }

        resultJoiner.add("Routes: [");
        if (!mRoutes.isEmpty()) {
            resultJoiner.add(TextUtils.join(",", mRoutes));
        }
        resultJoiner.add("]");

        if (mHttpProxy != null) {
            resultJoiner.add("HttpProxy:");
            resultJoiner.add(mHttpProxy.toString());
        }

        if (mNat64Prefix != null) {
            resultJoiner.add("Nat64Prefix:");
            resultJoiner.add(mNat64Prefix.toString());
        }

        final Collection<LinkProperties> stackedLinksValues = mStackedLinks.values();
        if (!stackedLinksValues.isEmpty()) {
            final StringJoiner stackedLinksJoiner = new StringJoiner(",", "Stacked: [", "]");
            for (final LinkProperties lp : stackedLinksValues) {
                stackedLinksJoiner.add("[ " + lp + " ]");
            }
            resultJoiner.add(stackedLinksJoiner.toString());
        }

        return resultJoiner.toString();
    }

    /**
     * Returns true if this link has an IPv4 address.
     *
     * @return {@code true} if there is an IPv4 address, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean hasIpv4Address() {
        for (LinkAddress address : mLinkAddresses) {
            if (address.getAddress() instanceof Inet4Address) {
                return true;
            }
        }
        return false;
    }

    /**
     * For backward compatibility.
     * This was annotated with @UnsupportedAppUsage in P, so we can't remove the method completely
     * just yet.
     * @return {@code true} if there is an IPv4 address, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    public boolean hasIPv4Address() {
        return hasIpv4Address();
    }

    /**
     * Returns true if this link or any of its stacked interfaces has an IPv4 address.
     *
     * @return {@code true} if there is an IPv4 address, {@code false} otherwise.
     */
    private boolean hasIpv4AddressOnInterface(String iface) {
        // mIfaceName can be null.
        return (Objects.equals(iface, mIfaceName) && hasIpv4Address())
                || (iface != null && mStackedLinks.containsKey(iface)
                        && mStackedLinks.get(iface).hasIpv4Address());
    }

    /**
     * Returns true if this link has a global preferred IPv6 address.
     *
     * @return {@code true} if there is a global preferred IPv6 address, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean hasGlobalIpv6Address() {
        for (LinkAddress address : mLinkAddresses) {
          if (address.getAddress() instanceof Inet6Address && address.isGlobalPreferred()) {
            return true;
          }
        }
        return false;
    }

    /**
     * Returns true if this link has an IPv4 unreachable default route.
     *
     * @return {@code true} if there is an IPv4 unreachable default route, {@code false} otherwise.
     * @hide
     */
    public boolean hasIpv4UnreachableDefaultRoute() {
        for (RouteInfo r : mRoutes) {
            if (r.isIPv4UnreachableDefault()) {
                return true;
            }
        }
        return false;
    }

    /**
     * For backward compatibility.
     * This was annotated with @UnsupportedAppUsage in P, so we can't remove the method completely
     * just yet.
     * @return {@code true} if there is a global preferred IPv6 address, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    public boolean hasGlobalIPv6Address() {
        return hasGlobalIpv6Address();
    }

    /**
     * Returns true if this link has an IPv4 default route.
     *
     * @return {@code true} if there is an IPv4 default route, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean hasIpv4DefaultRoute() {
        for (RouteInfo r : mRoutes) {
            if (r.isIPv4Default()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this link has an IPv6 unreachable default route.
     *
     * @return {@code true} if there is an IPv6 unreachable default route, {@code false} otherwise.
     * @hide
     */
    public boolean hasIpv6UnreachableDefaultRoute() {
        for (RouteInfo r : mRoutes) {
            if (r.isIPv6UnreachableDefault()) {
                return true;
            }
        }
        return false;
    }

    /**
     * For backward compatibility.
     * This was annotated with @UnsupportedAppUsage in P, so we can't remove the method completely
     * just yet.
     * @return {@code true} if there is an IPv4 default route, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    public boolean hasIPv4DefaultRoute() {
        return hasIpv4DefaultRoute();
    }

    /**
     * Returns true if this link has an IPv6 default route.
     *
     * @return {@code true} if there is an IPv6 default route, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean hasIpv6DefaultRoute() {
        for (RouteInfo r : mRoutes) {
            if (r.isIPv6Default()) {
                return true;
            }
        }
        return false;
    }

    /**
     * For backward compatibility.
     * This was annotated with @UnsupportedAppUsage in P, so we can't remove the method completely
     * just yet.
     * @return {@code true} if there is an IPv6 default route, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    public boolean hasIPv6DefaultRoute() {
        return hasIpv6DefaultRoute();
    }

    /**
     * Returns true if this link has an IPv4 DNS server.
     *
     * @return {@code true} if there is an IPv4 DNS server, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean hasIpv4DnsServer() {
        for (InetAddress ia : mDnses) {
            if (ia instanceof Inet4Address) {
                return true;
            }
        }
        return false;
    }

    /**
     * For backward compatibility.
     * This was annotated with @UnsupportedAppUsage in P, so we can't remove the method completely
     * just yet.
     * @return {@code true} if there is an IPv4 DNS server, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    public boolean hasIPv4DnsServer() {
        return hasIpv4DnsServer();
    }

    /**
     * Returns true if this link has an IPv6 DNS server.
     *
     * @return {@code true} if there is an IPv6 DNS server, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean hasIpv6DnsServer() {
        for (InetAddress ia : mDnses) {
            if (ia instanceof Inet6Address) {
                return true;
            }
        }
        return false;
    }

    /**
     * For backward compatibility.
     * This was annotated with @UnsupportedAppUsage in P, so we can't remove the method completely
     * just yet.
     * @return {@code true} if there is an IPv6 DNS server, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    public boolean hasIPv6DnsServer() {
        return hasIpv6DnsServer();
    }

    /**
     * Returns true if this link has an IPv4 PCSCF server.
     *
     * @return {@code true} if there is an IPv4 PCSCF server, {@code false} otherwise.
     * @hide
     */
    public boolean hasIpv4PcscfServer() {
        for (InetAddress ia : mPcscfs) {
          if (ia instanceof Inet4Address) {
            return true;
          }
        }
        return false;
    }

    /**
     * Returns true if this link has an IPv6 PCSCF server.
     *
     * @return {@code true} if there is an IPv6 PCSCF server, {@code false} otherwise.
     * @hide
     */
    public boolean hasIpv6PcscfServer() {
        for (InetAddress ia : mPcscfs) {
          if (ia instanceof Inet6Address) {
            return true;
          }
        }
        return false;
    }

    /**
     * Returns true if this link is provisioned for global IPv4 connectivity.
     * This requires an IP address, default route, and DNS server.
     *
     * @return {@code true} if the link is provisioned, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean isIpv4Provisioned() {
        return (hasIpv4Address()
                && hasIpv4DefaultRoute()
                && hasIpv4DnsServer());
    }

    /**
     * Returns true if this link is provisioned for global IPv6 connectivity.
     * This requires an IP address, default route, and DNS server.
     *
     * @return {@code true} if the link is provisioned, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean isIpv6Provisioned() {
        return (hasGlobalIpv6Address()
                && hasIpv6DefaultRoute()
                && hasIpv6DnsServer());
    }

    /**
     * For backward compatibility.
     * This was annotated with @UnsupportedAppUsage in P, so we can't remove the method completely
     * just yet.
     * @return {@code true} if the link is provisioned, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    public boolean isIPv6Provisioned() {
        return isIpv6Provisioned();
    }


    /**
     * Returns true if this link is provisioned for global connectivity,
     * for at least one Internet Protocol family.
     *
     * @return {@code true} if the link is provisioned, {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean isProvisioned() {
        return (isIpv4Provisioned() || isIpv6Provisioned());
    }

    /**
     * Evaluate whether the {@link InetAddress} is considered reachable.
     *
     * @return {@code true} if the given {@link InetAddress} is considered reachable,
     *         {@code false} otherwise.
     * @hide
     */
    @SystemApi
    public boolean isReachable(@NonNull InetAddress ip) {
        final List<RouteInfo> allRoutes = getAllRoutes();
        // If we don't have a route to this IP address, it's not reachable.
        final RouteInfo bestRoute = RouteInfo.selectBestRoute(allRoutes, ip);
        if (bestRoute == null) {
            return false;
        }

        // TODO: better source address evaluation for destination addresses.

        if (ip instanceof Inet4Address) {
            // For IPv4, it suffices for now to simply have any address.
            return hasIpv4AddressOnInterface(bestRoute.getInterface());
        } else if (ip instanceof Inet6Address) {
            if (ip.isLinkLocalAddress()) {
                // For now, just make sure link-local destinations have
                // scopedIds set, since transmits will generally fail otherwise.
                // TODO: verify it matches the ifindex of one of the interfaces.
                return (((Inet6Address)ip).getScopeId() != 0);
            }  else {
                // For non-link-local destinations check that either the best route
                // is directly connected or that some global preferred address exists.
                // TODO: reconsider all cases (disconnected ULA networks, ...).
                return (!bestRoute.hasGateway() || hasGlobalIpv6Address());
            }
        }

        return false;
    }

    /**
     * Returns true if this link has a throw route.
     *
     * @return {@code true} if there is an exclude route, {@code false} otherwise.
     * @hide
     */
    public boolean hasExcludeRoute() {
        for (RouteInfo r : mRoutes) {
            if (r.getType() == RouteInfo.RTN_THROW) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares this {@code LinkProperties} interface name against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage
    public boolean isIdenticalInterfaceName(@NonNull LinkProperties target) {
        return LinkPropertiesUtils.isIdenticalInterfaceName(target, this);
    }

    /**
     * Compares this {@code LinkProperties} DHCP server address against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalDhcpServerAddress(@NonNull LinkProperties target) {
        return Objects.equals(mDhcpServerAddress, target.mDhcpServerAddress);
    }

    /**
     * Compares this {@code LinkProperties} interface addresses against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage
    public boolean isIdenticalAddresses(@NonNull LinkProperties target) {
        return LinkPropertiesUtils.isIdenticalAddresses(target, this);
    }

    /**
     * Compares this {@code LinkProperties} DNS addresses against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage
    public boolean isIdenticalDnses(@NonNull LinkProperties target) {
        return LinkPropertiesUtils.isIdenticalDnses(target, this);
    }

    /**
     * Compares this {@code LinkProperties} private DNS settings against the
     * target.
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalPrivateDns(@NonNull LinkProperties target) {
        return (isPrivateDnsActive() == target.isPrivateDnsActive()
                && TextUtils.equals(getPrivateDnsServerName(),
                target.getPrivateDnsServerName()));
    }

    /**
     * Compares this {@code LinkProperties} validated private DNS addresses against
     * the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalValidatedPrivateDnses(@NonNull LinkProperties target) {
        Collection<InetAddress> targetDnses = target.getValidatedPrivateDnsServers();
        return (mValidatedPrivateDnses.size() == targetDnses.size())
                ? mValidatedPrivateDnses.containsAll(targetDnses) : false;
    }

    /**
     * Compares this {@code LinkProperties} PCSCF addresses against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalPcscfs(@NonNull LinkProperties target) {
        // Per 3GPP TS 24.229, B.2.2.1 PDP context activation and P-CSCF discovery
        // list order is important, so on U+ compare one by one
        if (SdkLevel.isAtLeastU()) return target.getPcscfServers().equals(mPcscfs);
        // but for safety old behaviour on pre-U:
        Collection<InetAddress> targetPcscfs = target.getPcscfServers();
        return (mPcscfs.size() == targetPcscfs.size()) ?
                    mPcscfs.containsAll(targetPcscfs) : false;
    }

    /**
     * Compares this {@code LinkProperties} Routes against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage
    public boolean isIdenticalRoutes(@NonNull LinkProperties target) {
        return LinkPropertiesUtils.isIdenticalRoutes(target, this);
    }

    /**
     * Compares this {@code LinkProperties} HttpProxy against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    public boolean isIdenticalHttpProxy(@NonNull LinkProperties target) {
        return LinkPropertiesUtils.isIdenticalHttpProxy(target, this);
    }

    /**
     * Compares this {@code LinkProperties} stacked links against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isIdenticalStackedLinks(@NonNull LinkProperties target) {
        if (!mStackedLinks.keySet().equals(target.mStackedLinks.keySet())) {
            return false;
        }
        for (LinkProperties stacked : mStackedLinks.values()) {
            // Hashtable values can never be null.
            String iface = stacked.getInterfaceName();
            if (!stacked.equals(target.mStackedLinks.get(iface))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares this {@code LinkProperties} MTU against the target
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalMtu(@NonNull LinkProperties target) {
        return getMtu() == target.getMtu();
    }

    /**
     * Compares this {@code LinkProperties} Tcp buffer sizes against the target.
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalTcpBufferSizes(@NonNull LinkProperties target) {
        return Objects.equals(mTcpBufferSizes, target.mTcpBufferSizes);
    }

    /**
     * Compares this {@code LinkProperties} NAT64 prefix against the target.
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalNat64Prefix(@NonNull LinkProperties target) {
        return Objects.equals(mNat64Prefix, target.mNat64Prefix);
    }

    /**
     * Compares this {@code LinkProperties} WakeOnLan supported against the target.
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalWakeOnLan(LinkProperties target) {
        return isWakeOnLanSupported() == target.isWakeOnLanSupported();
    }

    /**
     * Compares this {@code LinkProperties}'s CaptivePortalApiUrl against the target.
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalCaptivePortalApiUrl(LinkProperties target) {
        return Objects.equals(mCaptivePortalApiUrl, target.mCaptivePortalApiUrl);
    }

    /**
     * Compares this {@code LinkProperties}'s CaptivePortalData against the target.
     *
     * @param target LinkProperties to compare.
     * @return {@code true} if both are identical, {@code false} otherwise.
     * @hide
     */
    public boolean isIdenticalCaptivePortalData(LinkProperties target) {
        return Objects.equals(mCaptivePortalData, target.mCaptivePortalData);
    }

    /**
     * Set whether the network interface supports WakeOnLAN
     *
     * @param supported WakeOnLAN supported value
     *
     * @hide
     */
    public void setWakeOnLanSupported(boolean supported) {
        mWakeOnLanSupported = supported;
    }

    /**
     * Returns whether the network interface supports WakeOnLAN
     *
     * @return {@code true} if interface supports WakeOnLAN, {@code false} otherwise.
     */
    public boolean isWakeOnLanSupported() {
        return mWakeOnLanSupported;
    }

    /**
     * Set the URL of the captive portal API endpoint to get more information about the network.
     * @hide
     */
    @SystemApi
    public void setCaptivePortalApiUrl(@Nullable Uri url) {
        mCaptivePortalApiUrl = url;
    }

    /**
     * Get the URL of the captive portal API endpoint to get more information about the network.
     *
     * <p>This is null unless the application has
     * {@link android.Manifest.permission.NETWORK_SETTINGS} or
     * {@link NetworkStack#PERMISSION_MAINLINE_NETWORK_STACK} permissions, and the network provided
     * the URL.
     * @hide
     */
    @SystemApi
    @Nullable
    public Uri getCaptivePortalApiUrl() {
        return mCaptivePortalApiUrl;
    }

    /**
     * Set the CaptivePortalData obtained from the captive portal API (RFC7710bis).
     * @hide
     */
    @SystemApi
    public void setCaptivePortalData(@Nullable CaptivePortalData data) {
        mCaptivePortalData = data;
    }

    /**
     * Get the CaptivePortalData obtained from the captive portal API (RFC7710bis).
     *
     * <p>This is null unless the application has
     * {@link android.Manifest.permission.NETWORK_SETTINGS} or
     * {@link NetworkStack#PERMISSION_MAINLINE_NETWORK_STACK} permissions.
     * @hide
     */
    @SystemApi
    @Nullable
    public CaptivePortalData getCaptivePortalData() {
        return mCaptivePortalData;
    }

    /**
     * Compares this {@code LinkProperties} instance against the target
     * LinkProperties in {@code obj}. Two LinkPropertieses are equal if
     * all their fields are equal in values.
     *
     * For collection fields, such as mDnses, containsAll() is used to check
     * if two collections contains the same elements, independent of order.
     * There are two thoughts regarding containsAll()
     * 1. Duplicated elements. eg, (A, B, B) and (A, A, B) are equal.
     * 2. Worst case performance is O(n^2).
     *
     * @param obj the object to be tested for equality.
     * @return {@code true} if both objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof LinkProperties)) return false;

        LinkProperties target = (LinkProperties) obj;
        /*
         * This method does not check that stacked interfaces are equal, because
         * stacked interfaces are not so much a property of the link as a
         * description of connections between links.
         */
        return isIdenticalInterfaceName(target)
                && isIdenticalAddresses(target)
                && isIdenticalDhcpServerAddress(target)
                && isIdenticalDnses(target)
                && isIdenticalPrivateDns(target)
                && isIdenticalValidatedPrivateDnses(target)
                && isIdenticalPcscfs(target)
                && isIdenticalRoutes(target)
                && isIdenticalHttpProxy(target)
                && isIdenticalStackedLinks(target)
                && isIdenticalMtu(target)
                && isIdenticalTcpBufferSizes(target)
                && isIdenticalNat64Prefix(target)
                && isIdenticalWakeOnLan(target)
                && isIdenticalCaptivePortalApiUrl(target)
                && isIdenticalCaptivePortalData(target);
    }

    /**
     * Generate hashcode based on significant fields
     *
     * Equal objects must produce the same hash code, while unequal objects
     * may have the same hash codes.
     */
    @Override
    public int hashCode() {
        return ((null == mIfaceName) ? 0 : mIfaceName.hashCode()
                + mLinkAddresses.size() * 31
                + mDnses.size() * 37
                + mValidatedPrivateDnses.size() * 61
                + ((null == mDomains) ? 0 : mDomains.hashCode())
                + mRoutes.size() * 41
                + ((null == mHttpProxy) ? 0 : mHttpProxy.hashCode())
                + mStackedLinks.hashCode() * 47)
                + mMtu * 51
                + ((null == mTcpBufferSizes) ? 0 : mTcpBufferSizes.hashCode())
                + (mUsePrivateDns ? 57 : 0)
                + ((null == mDhcpServerAddress) ? 0 : mDhcpServerAddress.hashCode())
                + mPcscfs.size() * 67
                + ((null == mPrivateDnsServerName) ? 0 : mPrivateDnsServerName.hashCode())
                + Objects.hash(mNat64Prefix)
                + (mWakeOnLanSupported ? 71 : 0)
                + Objects.hash(mCaptivePortalApiUrl, mCaptivePortalData);
    }

    /**
     * Implement the Parcelable interface.
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getInterfaceName());
        dest.writeInt(mLinkAddresses.size());
        for (LinkAddress linkAddress : mLinkAddresses) {
            dest.writeParcelable(linkAddress, flags);
        }

        writeAddresses(dest, mDnses);
        writeAddresses(dest, mValidatedPrivateDnses);
        dest.writeBoolean(mUsePrivateDns);
        dest.writeString(mPrivateDnsServerName);
        writeAddresses(dest, mPcscfs);
        dest.writeString(mDomains);
        writeAddress(dest, mDhcpServerAddress);
        dest.writeInt(mMtu);
        dest.writeString(mTcpBufferSizes);
        dest.writeInt(mRoutes.size());
        for (RouteInfo route : mRoutes) {
            dest.writeParcelable(route, flags);
        }

        if (mHttpProxy != null) {
            dest.writeByte((byte)1);
            dest.writeParcelable(mHttpProxy, flags);
        } else {
            dest.writeByte((byte)0);
        }
        dest.writeParcelable(mNat64Prefix, 0);

        ArrayList<LinkProperties> stackedLinks = new ArrayList<>(mStackedLinks.values());
        dest.writeList(stackedLinks);

        dest.writeBoolean(mWakeOnLanSupported);
        dest.writeParcelable(mParcelSensitiveFields ? mCaptivePortalApiUrl : null, 0);
        dest.writeParcelable(mParcelSensitiveFields ? mCaptivePortalData : null, 0);
    }

    private static void writeAddresses(@NonNull Parcel dest, @NonNull List<InetAddress> list) {
        dest.writeInt(list.size());
        for (InetAddress d : list) {
            writeAddress(dest, d);
        }
    }

    private static void writeAddress(@NonNull Parcel dest, @Nullable InetAddress addr) {
        byte[] addressBytes = (addr == null ? null : addr.getAddress());
        dest.writeByteArray(addressBytes);
        if (addr instanceof Inet6Address) {
            final Inet6Address v6Addr = (Inet6Address) addr;
            final boolean hasScopeId = v6Addr.getScopeId() != 0;
            dest.writeBoolean(hasScopeId);
            if (hasScopeId) dest.writeInt(v6Addr.getScopeId());
        }
    }

    @Nullable
    private static InetAddress readAddress(@NonNull Parcel p) throws UnknownHostException {
        final byte[] addr = p.createByteArray();
        if (addr == null) return null;

        if (addr.length == INET6_ADDR_LENGTH) {
            final boolean hasScopeId = p.readBoolean();
            final int scopeId = hasScopeId ? p.readInt() : 0;
            return Inet6Address.getByAddress(null /* host */, addr, scopeId);
        }

        return InetAddress.getByAddress(addr);
    }

    /**
     * Implement the Parcelable interface.
     */
    public static final @android.annotation.NonNull Creator<LinkProperties> CREATOR =
        new Creator<LinkProperties>() {
            public LinkProperties createFromParcel(Parcel in) {
                LinkProperties netProp = new LinkProperties();

                String iface = in.readString();
                if (iface != null) {
                    netProp.setInterfaceName(iface);
                }
                int addressCount = in.readInt();
                for (int i = 0; i < addressCount; i++) {
                    netProp.addLinkAddress(in.readParcelable(null));
                }
                addressCount = in.readInt();
                for (int i = 0; i < addressCount; i++) {
                    try {
                        netProp.addDnsServer(readAddress(in));
                    } catch (UnknownHostException e) { }
                }
                addressCount = in.readInt();
                for (int i = 0; i < addressCount; i++) {
                    try {
                        netProp.addValidatedPrivateDnsServer(readAddress(in));
                    } catch (UnknownHostException e) { }
                }
                netProp.setUsePrivateDns(in.readBoolean());
                netProp.setPrivateDnsServerName(in.readString());
                addressCount = in.readInt();
                for (int i = 0; i < addressCount; i++) {
                    try {
                        netProp.addPcscfServer(readAddress(in));
                    } catch (UnknownHostException e) { }
                }
                netProp.setDomains(in.readString());
                try {
                    netProp.setDhcpServerAddress((Inet4Address) InetAddress
                            .getByAddress(in.createByteArray()));
                } catch (UnknownHostException e) { }
                netProp.setMtu(in.readInt());
                netProp.setTcpBufferSizes(in.readString());
                addressCount = in.readInt();
                for (int i = 0; i < addressCount; i++) {
                    netProp.addRoute(in.readParcelable(null));
                }
                if (in.readByte() == 1) {
                    netProp.setHttpProxy(in.readParcelable(null));
                }
                netProp.setNat64Prefix(in.readParcelable(null));
                ArrayList<LinkProperties> stackedLinks = new ArrayList<LinkProperties>();
                in.readList(stackedLinks, LinkProperties.class.getClassLoader());
                for (LinkProperties stackedLink: stackedLinks) {
                    netProp.addStackedLink(stackedLink);
                }
                netProp.setWakeOnLanSupported(in.readBoolean());

                netProp.setCaptivePortalApiUrl(in.readParcelable(null));
                netProp.setCaptivePortalData(in.readParcelable(null));
                return netProp;
            }

            public LinkProperties[] newArray(int size) {
                return new LinkProperties[size];
            }
        };

    /**
     * Check the valid MTU range based on IPv4 or IPv6.
     * @hide
     */
    public static boolean isValidMtu(int mtu, boolean ipv6) {
        if (ipv6) {
            return mtu >= MIN_MTU_V6 && mtu <= MAX_MTU;
        } else {
            return mtu >= MIN_MTU && mtu <= MAX_MTU;
        }
    }
}
