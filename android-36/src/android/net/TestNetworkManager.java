/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Class that allows creation and management of per-app, test-only networks
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public class TestNetworkManager {
    /**
     * Prefix for tun interfaces created by this class.
     * @hide
     */
    public static final String TEST_TUN_PREFIX = "testtun";

    /**
     * Prefix for tap interfaces created by this class.
     */
    public static final String TEST_TAP_PREFIX = "testtap";

    /**
     * Prefix for clat interfaces.
     * @hide
     */
    public static final String CLAT_INTERFACE_PREFIX = "v4-";

    @NonNull private static final String TAG = TestNetworkManager.class.getSimpleName();

    @NonNull private final ITestNetworkManager mService;

    private static final boolean TAP = false;
    private static final boolean TUN = true;
    private static final boolean BRING_UP = true;
    private static final boolean CARRIER_UP = true;
    // sets disableIpv6ProvisioningDelay to false.
    private static final boolean USE_IPV6_PROV_DELAY = false;
    private static final LinkAddress[] NO_ADDRS = new LinkAddress[0];

    /** @hide */
    public TestNetworkManager(@NonNull ITestNetworkManager service) {
        mService = Objects.requireNonNull(service, "missing ITestNetworkManager");
    }

    /**
     * Teardown the capability-limited, testing-only network for a given interface
     *
     * @param network The test network that should be torn down
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void teardownTestNetwork(@NonNull Network network) {
        try {
            mService.teardownTestNetwork(network.netId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void setupTestNetwork(
            @NonNull String iface,
            @Nullable LinkProperties lp,
            boolean isMetered,
            @NonNull int[] administratorUids,
            @NonNull IBinder binder) {
        try {
            mService.setupTestNetwork(iface, lp, isMetered, administratorUids, binder);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets up a capability-limited, testing-only network for a given interface
     *
     * @param lp The LinkProperties for the TestNetworkService to use for this test network. Note
     *     that the interface name and link addresses will be overwritten, and the passed-in values
     *     discarded.
     * @param isMetered Whether or not the network should be considered metered.
     * @param binder A binder object guarding the lifecycle of this test network.
     * @hide
     */
    public void setupTestNetwork(
            @NonNull LinkProperties lp, boolean isMetered, @NonNull IBinder binder) {
        Objects.requireNonNull(lp, "Invalid LinkProperties");
        setupTestNetwork(lp.getInterfaceName(), lp, isMetered, new int[0], binder);
    }

    /**
     * Sets up a capability-limited, testing-only network for a given interface
     *
     * @param iface the name of the interface to be used for the Network LinkProperties.
     * @param binder A binder object guarding the lifecycle of this test network.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public void setupTestNetwork(@NonNull String iface, @NonNull IBinder binder) {
        setupTestNetwork(iface, null, true, new int[0], binder);
    }

    /**
     * Sets up a capability-limited, testing-only network for a given interface with the given
     * administrator UIDs.
     *
     * @param iface the name of the interface to be used for the Network LinkProperties.
     * @param administratorUids The administrator UIDs to be used for the test-only network
     * @param binder A binder object guarding the lifecycle of this test network.
     * @hide
     */
    public void setupTestNetwork(
            @NonNull String iface, @NonNull int[] administratorUids, @NonNull IBinder binder) {
        setupTestNetwork(iface, null, true, administratorUids, binder);
    }

    /**
     * Create a tun interface for testing purposes
     *
     * @param linkAddrs an array of LinkAddresses to assign to the TUN interface
     * @return A ParcelFileDescriptor of the underlying TUN interface. Close this to tear down the
     *     TUN interface.
     * @deprecated Use {@link #createTunInterface(Collection)} instead.
     * @hide
     */
    @Deprecated
    @NonNull
    public TestNetworkInterface createTunInterface(@NonNull LinkAddress[] linkAddrs) {
        return createTunInterface(Arrays.asList(linkAddrs));
    }

    /**
     * Create a tun interface for testing purposes
     *
     * @param linkAddrs an array of LinkAddresses to assign to the TUN interface
     * @return A ParcelFileDescriptor of the underlying TUN interface. Close this to tear down the
     *     TUN interface.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @NonNull
    public TestNetworkInterface createTunInterface(@NonNull Collection<LinkAddress> linkAddrs) {
        try {
            final LinkAddress[] arr = new LinkAddress[linkAddrs.size()];
            return mService.createInterface(TUN, CARRIER_UP, BRING_UP, USE_IPV6_PROV_DELAY,
                    linkAddrs.toArray(arr), null /* iface */);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Create a tap interface for testing purposes
     *
     * @return A ParcelFileDescriptor of the underlying TAP interface. Close this to tear down the
     *     TAP interface.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    @NonNull
    public TestNetworkInterface createTapInterface() {
        try {
            return mService.createInterface(TAP, CARRIER_UP, BRING_UP, USE_IPV6_PROV_DELAY,
                    NO_ADDRS, null /* iface */);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Create a tap interface for testing purposes
     *
     * @param linkAddrs an array of LinkAddresses to assign to the TAP interface
     * @return A TestNetworkInterface representing the underlying TAP interface. Close the contained
     *     ParcelFileDescriptor to tear down the TAP interface.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @NonNull
    public TestNetworkInterface createTapInterface(@NonNull LinkAddress[] linkAddrs) {
        try {
            return mService.createInterface(TAP, CARRIER_UP, BRING_UP, USE_IPV6_PROV_DELAY,
                    linkAddrs, null /* iface */);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Create a tap interface for testing purposes
     *
     * @param bringUp whether to bring up the interface before returning it.
     *
     * @return A ParcelFileDescriptor of the underlying TAP interface. Close this to tear down the
     *     TAP interface.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @NonNull
    public TestNetworkInterface createTapInterface(boolean bringUp) {
        try {
            return mService.createInterface(TAP, CARRIER_UP, bringUp, USE_IPV6_PROV_DELAY,
                    NO_ADDRS, null /* iface */);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Create a tap interface with a given interface name for testing purposes
     *
     * @param bringUp whether to bring up the interface before returning it.
     * @param iface interface name to be assigned, so far only interface name which starts with
     *              "v4-testtap" or "v4-testtun" is allowed to be created. If it's null, then use
     *              the default name(e.g. testtap or testtun).
     *
     * @return A ParcelFileDescriptor of the underlying TAP interface. Close this to tear down the
     *     TAP interface.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @NonNull
    public TestNetworkInterface createTapInterface(boolean bringUp, @NonNull String iface) {
        try {
            return mService.createInterface(TAP, CARRIER_UP, bringUp, USE_IPV6_PROV_DELAY,
                    NO_ADDRS, iface);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Create a tap interface with or without carrier for testing purposes.
     *
     * Note: setting carrierUp = false is not supported until kernel version 6.0.
     *
     * @param carrierUp whether the created interface has a carrier or not.
     * @param bringUp whether to bring up the interface before returning it.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @NonNull
    public TestNetworkInterface createTapInterface(boolean carrierUp, boolean bringUp) {
        try {
            return mService.createInterface(TAP, carrierUp, bringUp, USE_IPV6_PROV_DELAY, NO_ADDRS,
                    null /* iface */);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Create a tap interface for testing purposes.
     *
     * Note: setting carrierUp = false is not supported until kernel version 6.0.
     *
     * @param carrierUp whether the created interface has a carrier or not.
     * @param bringUp whether to bring up the interface before returning it.
     * @param disableIpv6ProvisioningDelay whether to disable DAD and RS delay.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @NonNull
    public TestNetworkInterface createTapInterface(boolean carrierUp, boolean bringUp,
            boolean disableIpv6ProvisioningDelay) {
        try {
            return mService.createInterface(TAP, carrierUp, bringUp, disableIpv6ProvisioningDelay,
                    NO_ADDRS, null /* iface */);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Create a tap interface for testing purposes.
     *
     * @param disableIpv6ProvisioningDelay whether to disable DAD and RS delay.
     * @param linkAddrs an array of LinkAddresses to assign to the TAP interface
     * @return A TestNetworkInterface representing the underlying TAP interface. Close the contained
     *     ParcelFileDescriptor to tear down the TAP interface.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    @NonNull
    public TestNetworkInterface createTapInterface(boolean disableIpv6ProvisioningDelay,
            @NonNull LinkAddress[] linkAddrs) {
        try {
            return mService.createInterface(TAP, CARRIER_UP, BRING_UP, disableIpv6ProvisioningDelay,
                    linkAddrs, null /* iface */);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enable / disable carrier on TestNetworkInterface
     *
     * Note: TUNSETCARRIER is not supported until kernel version 5.0.
     *
     * @param iface the interface to configure.
     * @param enabled true to turn carrier on, false to turn carrier off.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_TEST_NETWORKS)
    public void setCarrierEnabled(@NonNull TestNetworkInterface iface, boolean enabled) {
        try {
            mService.setCarrierEnabled(iface, enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
