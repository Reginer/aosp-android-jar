/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.net.ConnectivityManager.FIREWALL_CHAIN_BACKGROUND;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_DOZABLE;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_LOW_POWER_STANDBY;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_METERED_ALLOW;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_METERED_DENY_ADMIN;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_METERED_DENY_USER;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_OEM_DENY_1;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_OEM_DENY_2;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_OEM_DENY_3;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_POWERSAVE;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_RESTRICTED;
import static android.net.ConnectivityManager.FIREWALL_CHAIN_STANDBY;

import android.util.Pair;

import com.android.net.module.util.Struct;

import java.util.Arrays;
import java.util.List;

/**
 * BpfNetMaps related constants that can be shared among modules.
 *
 * @hide
 */
// Note that this class should be put into bootclasspath instead of static libraries.
// Because modules could have different copies of this class if this is statically linked,
// which would be problematic if the definitions in these modules are not synchronized.
public class BpfNetMapsConstants {
    // Prevent this class from being accidental instantiated.
    private BpfNetMapsConstants() {}

    public static final String CONFIGURATION_MAP_PATH =
            "/sys/fs/bpf/netd_shared/map_netd_configuration_map";
    public static final String UID_OWNER_MAP_PATH =
            "/sys/fs/bpf/netd_shared/map_netd_uid_owner_map";
    public static final String UID_PERMISSION_MAP_PATH =
            "/sys/fs/bpf/netd_shared/map_netd_uid_permission_map";
    public static final String COOKIE_TAG_MAP_PATH =
            "/sys/fs/bpf/netd_shared/map_netd_cookie_tag_map";
    public static final String DATA_SAVER_ENABLED_MAP_PATH =
            "/sys/fs/bpf/netd_shared/map_netd_data_saver_enabled_map";
    public static final String INGRESS_DISCARD_MAP_PATH =
            "/sys/fs/bpf/netd_shared/map_netd_ingress_discard_map";
    public static final Struct.S32 UID_RULES_CONFIGURATION_KEY = new Struct.S32(0);
    public static final Struct.S32 CURRENT_STATS_MAP_CONFIGURATION_KEY = new Struct.S32(1);
    public static final Struct.S32 DATA_SAVER_ENABLED_KEY = new Struct.S32(0);

    public static final short DATA_SAVER_DISABLED = 0;
    public static final short DATA_SAVER_ENABLED = 1;

    // LINT.IfChange(match_type)
    public static final long NO_MATCH = 0;
    public static final long HAPPY_BOX_MATCH = (1 << 0);
    public static final long PENALTY_BOX_USER_MATCH = (1 << 1);
    public static final long DOZABLE_MATCH = (1 << 2);
    public static final long STANDBY_MATCH = (1 << 3);
    public static final long POWERSAVE_MATCH = (1 << 4);
    public static final long RESTRICTED_MATCH = (1 << 5);
    public static final long LOW_POWER_STANDBY_MATCH = (1 << 6);
    public static final long IIF_MATCH = (1 << 7);
    public static final long LOCKDOWN_VPN_MATCH = (1 << 8);
    public static final long OEM_DENY_1_MATCH = (1 << 9);
    public static final long OEM_DENY_2_MATCH = (1 << 10);
    public static final long OEM_DENY_3_MATCH = (1 << 11);
    public static final long BACKGROUND_MATCH = (1 << 12);
    public static final long PENALTY_BOX_ADMIN_MATCH = (1 << 13);

    public static final List<Pair<Long, String>> MATCH_LIST = Arrays.asList(
            Pair.create(HAPPY_BOX_MATCH, "HAPPY_BOX_MATCH"),
            Pair.create(PENALTY_BOX_USER_MATCH, "PENALTY_BOX_USER_MATCH"),
            Pair.create(DOZABLE_MATCH, "DOZABLE_MATCH"),
            Pair.create(STANDBY_MATCH, "STANDBY_MATCH"),
            Pair.create(POWERSAVE_MATCH, "POWERSAVE_MATCH"),
            Pair.create(RESTRICTED_MATCH, "RESTRICTED_MATCH"),
            Pair.create(LOW_POWER_STANDBY_MATCH, "LOW_POWER_STANDBY_MATCH"),
            Pair.create(IIF_MATCH, "IIF_MATCH"),
            Pair.create(LOCKDOWN_VPN_MATCH, "LOCKDOWN_VPN_MATCH"),
            Pair.create(OEM_DENY_1_MATCH, "OEM_DENY_1_MATCH"),
            Pair.create(OEM_DENY_2_MATCH, "OEM_DENY_2_MATCH"),
            Pair.create(OEM_DENY_3_MATCH, "OEM_DENY_3_MATCH"),
            Pair.create(BACKGROUND_MATCH, "BACKGROUND_MATCH"),
            Pair.create(PENALTY_BOX_ADMIN_MATCH, "PENALTY_BOX_ADMIN_MATCH")
    );

    /**
     * List of all firewall allow chains that are applied to all networks regardless of meteredness
     * See {@link #METERED_ALLOW_CHAINS} for allow chains that are only applied to metered networks.
     *
     * Allow chains mean the firewall denies all uids by default, uids must be explicitly allowed.
     */
    public static final List<Integer> ALLOW_CHAINS = List.of(
            FIREWALL_CHAIN_DOZABLE,
            FIREWALL_CHAIN_POWERSAVE,
            FIREWALL_CHAIN_RESTRICTED,
            FIREWALL_CHAIN_LOW_POWER_STANDBY,
            FIREWALL_CHAIN_BACKGROUND
    );

    /**
     * List of all firewall deny chains that are applied to all networks regardless of meteredness
     * See {@link #METERED_DENY_CHAINS} for deny chains that are only applied to metered networks.
     *
     * Deny chains mean the firewall allows all uids by default, uids must be explicitly denied.
     */
    public static final List<Integer> DENY_CHAINS = List.of(
            FIREWALL_CHAIN_STANDBY,
            FIREWALL_CHAIN_OEM_DENY_1,
            FIREWALL_CHAIN_OEM_DENY_2,
            FIREWALL_CHAIN_OEM_DENY_3
    );

    /**
     * List of all firewall allow chains that are only applied to metered networks.
     * See {@link #ALLOW_CHAINS} for allow chains that are applied to all networks regardless of
     * meteredness.
     */
    public static final List<Integer> METERED_ALLOW_CHAINS = List.of(
            FIREWALL_CHAIN_METERED_ALLOW
    );

    /**
     * List of all firewall deny chains that are only applied to metered networks.
     * See {@link #DENY_CHAINS} for deny chains that are applied to all networks regardless of
     * meteredness.
     */
    public static final List<Integer> METERED_DENY_CHAINS = List.of(
            FIREWALL_CHAIN_METERED_DENY_USER,
            FIREWALL_CHAIN_METERED_DENY_ADMIN
    );
    // LINT.ThenChange(../../../../bpf_progs/netd.h)
}
