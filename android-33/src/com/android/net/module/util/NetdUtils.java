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

package com.android.net.module.util;

import static android.net.INetd.IF_STATE_DOWN;
import static android.net.INetd.IF_STATE_UP;
import static android.net.RouteInfo.RTN_THROW;
import static android.net.RouteInfo.RTN_UNICAST;
import static android.net.RouteInfo.RTN_UNREACHABLE;
import static android.system.OsConstants.EBUSY;

import android.annotation.SuppressLint;
import android.net.INetd;
import android.net.InterfaceConfigurationParcel;
import android.net.IpPrefix;
import android.net.RouteInfo;
import android.net.TetherConfigParcel;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collection of utilities for netd.
 */
public class NetdUtils {
    private static final String TAG = NetdUtils.class.getSimpleName();

    /** Used to modify the specified route. */
    public enum ModifyOperation {
        ADD,
        REMOVE,
    }

    /**
     * Get InterfaceConfigurationParcel from netd.
     */
    public static InterfaceConfigurationParcel getInterfaceConfigParcel(@NonNull INetd netd,
            @NonNull String iface) {
        try {
            return netd.interfaceGetCfg(iface);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void validateFlag(String flag) {
        if (flag.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("flag contains space: " + flag);
        }
    }

    /**
     * Check whether the InterfaceConfigurationParcel contains the target flag or not.
     *
     * @param config The InterfaceConfigurationParcel instance.
     * @param flag Target flag string to be checked.
     */
    public static boolean hasFlag(@NonNull final InterfaceConfigurationParcel config,
            @NonNull final String flag) {
        validateFlag(flag);
        final Set<String> flagList = new HashSet<String>(Arrays.asList(config.flags));
        return flagList.contains(flag);
    }

    @VisibleForTesting
    protected static String[] removeAndAddFlags(@NonNull String[] flags, @NonNull String remove,
            @NonNull String add) {
        final ArrayList<String> result = new ArrayList<>();
        try {
            // Validate the add flag first, so that the for-loop can be ignore once the format of
            // add flag is invalid.
            validateFlag(add);
            for (String flag : flags) {
                // Simply ignore both of remove and add flags first, then add the add flag after
                // exiting the loop to prevent adding the duplicate flag.
                if (remove.equals(flag) || add.equals(flag)) continue;
                result.add(flag);
            }
            result.add(add);
            return result.toArray(new String[result.size()]);
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("Invalid InterfaceConfigurationParcel", iae);
        }
    }

    /**
     * Set interface configuration to netd by passing InterfaceConfigurationParcel.
     */
    public static void setInterfaceConfig(INetd netd, InterfaceConfigurationParcel configParcel) {
        try {
            netd.interfaceSetCfg(configParcel);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set the given interface up.
     */
    public static void setInterfaceUp(INetd netd, String iface) {
        final InterfaceConfigurationParcel configParcel = getInterfaceConfigParcel(netd, iface);
        configParcel.flags = removeAndAddFlags(configParcel.flags, IF_STATE_DOWN /* remove */,
                IF_STATE_UP /* add */);
        setInterfaceConfig(netd, configParcel);
    }

    /**
     * Set the given interface down.
     */
    public static void setInterfaceDown(INetd netd, String iface) {
        final InterfaceConfigurationParcel configParcel = getInterfaceConfigParcel(netd, iface);
        configParcel.flags = removeAndAddFlags(configParcel.flags, IF_STATE_UP /* remove */,
                IF_STATE_DOWN /* add */);
        setInterfaceConfig(netd, configParcel);
    }

    /** Start tethering. */
    public static void tetherStart(final INetd netd, final boolean usingLegacyDnsProxy,
            final String[] dhcpRange) throws RemoteException, ServiceSpecificException {
        final TetherConfigParcel config = new TetherConfigParcel();
        config.usingLegacyDnsProxy = usingLegacyDnsProxy;
        config.dhcpRanges = dhcpRange;
        netd.tetherStartWithConfiguration(config);
    }

    /** Setup interface for tethering. */
    public static void tetherInterface(final INetd netd, final String iface, final IpPrefix dest)
            throws RemoteException, ServiceSpecificException {
        tetherInterface(netd, iface, dest, 20 /* maxAttempts */, 50 /* pollingIntervalMs */);
    }

    /** Setup interface with configurable retries for tethering. */
    public static void tetherInterface(final INetd netd, final String iface, final IpPrefix dest,
            int maxAttempts, int pollingIntervalMs)
            throws RemoteException, ServiceSpecificException {
        netd.tetherInterfaceAdd(iface);
        networkAddInterface(netd, iface, maxAttempts, pollingIntervalMs);
        List<RouteInfo> routes = new ArrayList<>();
        routes.add(new RouteInfo(dest, null, iface, RTN_UNICAST));
        addRoutesToLocalNetwork(netd, iface, routes);
    }

    /**
     * Retry Netd#networkAddInterface for EBUSY error code.
     * If the same interface (e.g., wlan0) is in client mode and then switches to tethered mode.
     * There can be a race where puts the interface into the local network but interface is still
     * in use in netd because the ConnectivityService thread hasn't processed the disconnect yet.
     * See b/158269544 for detail.
     */
    private static void networkAddInterface(final INetd netd, final String iface,
            int maxAttempts, int pollingIntervalMs)
            throws ServiceSpecificException, RemoteException {
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                netd.networkAddInterface(INetd.LOCAL_NET_ID, iface);
                return;
            } catch (ServiceSpecificException e) {
                if (e.errorCode == EBUSY && i < maxAttempts) {
                    SystemClock.sleep(pollingIntervalMs);
                    continue;
                }

                Log.e(TAG, "Retry Netd#networkAddInterface failure: " + e);
                throw e;
            }
        }
    }

    /** Reset interface for tethering. */
    public static void untetherInterface(final INetd netd, String iface)
            throws RemoteException, ServiceSpecificException {
        try {
            netd.tetherInterfaceRemove(iface);
        } finally {
            netd.networkRemoveInterface(INetd.LOCAL_NET_ID, iface);
        }
    }

    /** Add |routes| to local network. */
    public static void addRoutesToLocalNetwork(final INetd netd, final String iface,
            final List<RouteInfo> routes) {

        for (RouteInfo route : routes) {
            if (!route.isDefaultRoute()) {
                modifyRoute(netd, ModifyOperation.ADD, INetd.LOCAL_NET_ID, route);
            }
        }

        // IPv6 link local should be activated always.
        modifyRoute(netd, ModifyOperation.ADD, INetd.LOCAL_NET_ID,
                new RouteInfo(new IpPrefix("fe80::/64"), null, iface, RTN_UNICAST));
    }

    /** Remove routes from local network. */
    public static int removeRoutesFromLocalNetwork(final INetd netd, final List<RouteInfo> routes) {
        int failures = 0;

        for (RouteInfo route : routes) {
            try {
                modifyRoute(netd, ModifyOperation.REMOVE, INetd.LOCAL_NET_ID, route);
            } catch (IllegalStateException e) {
                failures++;
            }
        }

        return failures;
    }

    @SuppressLint("NewApi")
    private static String findNextHop(final RouteInfo route) {
        final String nextHop;
        switch (route.getType()) {
            case RTN_UNICAST:
                if (route.hasGateway()) {
                    nextHop = route.getGateway().getHostAddress();
                } else {
                    nextHop = INetd.NEXTHOP_NONE;
                }
                break;
            case RTN_UNREACHABLE:
                nextHop = INetd.NEXTHOP_UNREACHABLE;
                break;
            case RTN_THROW:
                nextHop = INetd.NEXTHOP_THROW;
                break;
            default:
                nextHop = INetd.NEXTHOP_NONE;
                break;
        }
        return nextHop;
    }

    /** Add or remove |route|. */
    public static void modifyRoute(final INetd netd, final ModifyOperation op, final int netId,
            final RouteInfo route) {
        final String ifName = route.getInterface();
        final String dst = route.getDestination().toString();
        final String nextHop = findNextHop(route);

        try {
            switch(op) {
                case ADD:
                    netd.networkAddRoute(netId, ifName, dst, nextHop);
                    break;
                case REMOVE:
                    netd.networkRemoveRoute(netId, ifName, dst, nextHop);
                    break;
                default:
                    throw new IllegalStateException("Unsupported modify operation:" + op);
            }
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }
}
