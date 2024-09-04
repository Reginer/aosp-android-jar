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

import static android.net.BpfNetMapsConstants.ALLOW_CHAINS;
import static android.net.BpfNetMapsConstants.BACKGROUND_MATCH;
import static android.net.BpfNetMapsConstants.DATA_SAVER_ENABLED;
import static android.net.BpfNetMapsConstants.DATA_SAVER_ENABLED_KEY;
import static android.net.BpfNetMapsConstants.DENY_CHAINS;
import static android.net.BpfNetMapsConstants.DOZABLE_MATCH;
import static android.net.BpfNetMapsConstants.HAPPY_BOX_MATCH;
import static android.net.BpfNetMapsConstants.LOW_POWER_STANDBY_MATCH;
import static android.net.BpfNetMapsConstants.MATCH_LIST;
import static android.net.BpfNetMapsConstants.METERED_ALLOW_CHAINS;
import static android.net.BpfNetMapsConstants.METERED_DENY_CHAINS;
import static android.net.BpfNetMapsConstants.NO_MATCH;
import static android.net.BpfNetMapsConstants.OEM_DENY_1_MATCH;
import static android.net.BpfNetMapsConstants.OEM_DENY_2_MATCH;
import static android.net.BpfNetMapsConstants.OEM_DENY_3_MATCH;
import static android.net.BpfNetMapsConstants.PENALTY_BOX_ADMIN_MATCH;
import static android.net.BpfNetMapsConstants.PENALTY_BOX_USER_MATCH;
import static android.net.BpfNetMapsConstants.POWERSAVE_MATCH;
import static android.net.BpfNetMapsConstants.RESTRICTED_MATCH;
import static android.net.BpfNetMapsConstants.STANDBY_MATCH;
import static android.net.BpfNetMapsConstants.UID_RULES_CONFIGURATION_KEY;
import static android.net.ConnectivityManager.BLOCKED_METERED_REASON_ADMIN_DISABLED;
import static android.net.ConnectivityManager.BLOCKED_METERED_REASON_DATA_SAVER;
import static android.net.ConnectivityManager.BLOCKED_METERED_REASON_MASK;
import static android.net.ConnectivityManager.BLOCKED_METERED_REASON_USER_RESTRICTED;
import static android.net.ConnectivityManager.BLOCKED_REASON_APP_BACKGROUND;
import static android.net.ConnectivityManager.BLOCKED_REASON_APP_STANDBY;
import static android.net.ConnectivityManager.BLOCKED_REASON_BATTERY_SAVER;
import static android.net.ConnectivityManager.BLOCKED_REASON_DOZE;
import static android.net.ConnectivityManager.BLOCKED_REASON_LOW_POWER_STANDBY;
import static android.net.ConnectivityManager.BLOCKED_REASON_NONE;
import static android.net.ConnectivityManager.BLOCKED_REASON_OEM_DENY;
import static android.net.ConnectivityManager.BLOCKED_REASON_RESTRICTED_MODE;
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
import static android.net.ConnectivityManager.FIREWALL_RULE_ALLOW;
import static android.net.ConnectivityManager.FIREWALL_RULE_DENY;
import static android.system.OsConstants.EINVAL;

import android.os.Build;
import android.os.Process;
import android.os.ServiceSpecificException;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Pair;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.net.module.util.IBpfMap;
import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.S32;
import com.android.net.module.util.Struct.U32;
import com.android.net.module.util.Struct.U8;

import java.util.StringJoiner;

/**
 * The classes and the methods for BpfNetMaps utilization.
 *
 * @hide
 */
// Note that this class should be put into bootclasspath instead of static libraries.
// Because modules could have different copies of this class if this is statically linked,
// which would be problematic if the definitions in these modules are not synchronized.
// Note that NetworkStack can not use this before U due to b/326143935
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class BpfNetMapsUtils {
    // Bitmaps for calculating whether a given uid is blocked by firewall chains.
    private static final long sMaskDropIfSet;
    private static final long sMaskDropIfUnset;

    static {
        long maskDropIfSet = 0L;
        long maskDropIfUnset = 0L;

        for (int chain : BpfNetMapsConstants.ALLOW_CHAINS) {
            final long match = getMatchByFirewallChain(chain);
            maskDropIfUnset |= match;
        }
        for (int chain : BpfNetMapsConstants.DENY_CHAINS) {
            final long match = getMatchByFirewallChain(chain);
            maskDropIfSet |= match;
        }
        sMaskDropIfSet = maskDropIfSet;
        sMaskDropIfUnset = maskDropIfUnset;
    }

    // Prevent this class from being accidental instantiated.
    private BpfNetMapsUtils() {}

    /**
     * Get corresponding match from firewall chain.
     */
    public static long getMatchByFirewallChain(final int chain) {
        switch (chain) {
            case FIREWALL_CHAIN_DOZABLE:
                return DOZABLE_MATCH;
            case FIREWALL_CHAIN_STANDBY:
                return STANDBY_MATCH;
            case FIREWALL_CHAIN_POWERSAVE:
                return POWERSAVE_MATCH;
            case FIREWALL_CHAIN_RESTRICTED:
                return RESTRICTED_MATCH;
            case FIREWALL_CHAIN_BACKGROUND:
                return BACKGROUND_MATCH;
            case FIREWALL_CHAIN_LOW_POWER_STANDBY:
                return LOW_POWER_STANDBY_MATCH;
            case FIREWALL_CHAIN_OEM_DENY_1:
                return OEM_DENY_1_MATCH;
            case FIREWALL_CHAIN_OEM_DENY_2:
                return OEM_DENY_2_MATCH;
            case FIREWALL_CHAIN_OEM_DENY_3:
                return OEM_DENY_3_MATCH;
            case FIREWALL_CHAIN_METERED_ALLOW:
                return HAPPY_BOX_MATCH;
            case FIREWALL_CHAIN_METERED_DENY_USER:
                return PENALTY_BOX_USER_MATCH;
            case FIREWALL_CHAIN_METERED_DENY_ADMIN:
                return PENALTY_BOX_ADMIN_MATCH;
            default:
                throw new ServiceSpecificException(EINVAL, "Invalid firewall chain: " + chain);
        }
    }

    /**
     * Get whether the chain is an allow-list or a deny-list.
     *
     * ALLOWLIST means the firewall denies all by default, uids must be explicitly allowed
     * DENYLIST means the firewall allows all by default, uids must be explicitly denied
     */
    public static boolean isFirewallAllowList(final int chain) {
        if (ALLOW_CHAINS.contains(chain) || METERED_ALLOW_CHAINS.contains(chain)) {
            return true;
        } else if (DENY_CHAINS.contains(chain) || METERED_DENY_CHAINS.contains(chain)) {
            return false;
        }
        throw new ServiceSpecificException(EINVAL, "Invalid firewall chain: " + chain);
    }

    /**
     * Get match string representation from the given match bitmap.
     */
    public static String matchToString(long matchMask) {
        if (matchMask == NO_MATCH) {
            return "NO_MATCH";
        }

        final StringJoiner sj = new StringJoiner(" ");
        for (final Pair<Long, String> match : MATCH_LIST) {
            final long matchFlag = match.first;
            final String matchName = match.second;
            if ((matchMask & matchFlag) != 0) {
                sj.add(matchName);
                matchMask &= ~matchFlag;
            }
        }
        if (matchMask != 0) {
            sj.add("UNKNOWN_MATCH(" + matchMask + ")");
        }
        return sj.toString();
    }

    /**
     * Throw UnsupportedOperationException if SdkLevel is before T.
     */
    public static void throwIfPreT(final String msg) {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException(msg);
        }
    }

    /**
     * Get the specified firewall chain's status.
     *
     * @param configurationMap target configurationMap
     * @param chain target chain
     * @return {@code true} if chain is enabled, {@code false} if chain is not enabled.
     * @throws UnsupportedOperationException if called on pre-T devices.
     * @throws ServiceSpecificException in case of failure, with an error code indicating the
     *                                  cause of the failure.
     */
    public static boolean isChainEnabled(
            final IBpfMap<S32, U32> configurationMap, final int chain) {
        throwIfPreT("isChainEnabled is not available on pre-T devices");

        final long match = getMatchByFirewallChain(chain);
        try {
            final U32 config = configurationMap.getValue(UID_RULES_CONFIGURATION_KEY);
            return (config.val & match) != 0;
        } catch (ErrnoException e) {
            throw new ServiceSpecificException(e.errno,
                    "Unable to get firewall chain status: " + Os.strerror(e.errno));
        }
    }

    /**
     * Get firewall rule of specified firewall chain on specified uid.
     *
     * @param uidOwnerMap target uidOwnerMap.
     * @param chain target chain.
     * @param uid target uid.
     * @return either FIREWALL_RULE_ALLOW or FIREWALL_RULE_DENY
     * @throws UnsupportedOperationException if called on pre-T devices.
     * @throws ServiceSpecificException      in case of failure, with an error code indicating the
     *                                       cause of the failure.
     */
    public static int getUidRule(final IBpfMap<S32, UidOwnerValue> uidOwnerMap,
            final int chain, final int uid) {
        throwIfPreT("getUidRule is not available on pre-T devices");

        final long match = getMatchByFirewallChain(chain);
        final boolean isAllowList = isFirewallAllowList(chain);
        try {
            final UidOwnerValue uidMatch = uidOwnerMap.getValue(new S32(uid));
            final boolean isMatchEnabled = uidMatch != null && (uidMatch.rule & match) != 0;
            return isMatchEnabled == isAllowList ? FIREWALL_RULE_ALLOW : FIREWALL_RULE_DENY;
        } catch (ErrnoException e) {
            throw new ServiceSpecificException(e.errno,
                    "Unable to get uid rule status: " + Os.strerror(e.errno));
        }
    }

    /**
     * Get blocked reasons for specified uid
     *
     * @param uid Target Uid
     * @return Reasons of network access blocking for an UID
     */
    public static int getUidNetworkingBlockedReasons(final int uid,
            IBpfMap<S32, U32> configurationMap,
            IBpfMap<S32, UidOwnerValue> uidOwnerMap,
            IBpfMap<S32, U8> dataSaverEnabledMap
    ) {
        final long uidRuleConfig;
        final long uidMatch;
        try {
            uidRuleConfig = configurationMap.getValue(UID_RULES_CONFIGURATION_KEY).val;
            final UidOwnerValue value = uidOwnerMap.getValue(new Struct.S32(uid));
            uidMatch = (value != null) ? value.rule : 0L;
        } catch (ErrnoException e) {
            throw new ServiceSpecificException(e.errno,
                    "Unable to get firewall chain status: " + Os.strerror(e.errno));
        }
        final long blockingMatches = (uidRuleConfig & ~uidMatch & sMaskDropIfUnset)
                | (uidRuleConfig & uidMatch & sMaskDropIfSet);

        int blockedReasons = BLOCKED_REASON_NONE;
        if ((blockingMatches & POWERSAVE_MATCH) != 0) {
            blockedReasons |= BLOCKED_REASON_BATTERY_SAVER;
        }
        if ((blockingMatches & DOZABLE_MATCH) != 0) {
            blockedReasons |= BLOCKED_REASON_DOZE;
        }
        if ((blockingMatches & STANDBY_MATCH) != 0) {
            blockedReasons |= BLOCKED_REASON_APP_STANDBY;
        }
        if ((blockingMatches & RESTRICTED_MATCH) != 0) {
            blockedReasons |= BLOCKED_REASON_RESTRICTED_MODE;
        }
        if ((blockingMatches & LOW_POWER_STANDBY_MATCH) != 0) {
            blockedReasons |= BLOCKED_REASON_LOW_POWER_STANDBY;
        }
        if ((blockingMatches & BACKGROUND_MATCH) != 0) {
            blockedReasons |= BLOCKED_REASON_APP_BACKGROUND;
        }
        if ((blockingMatches & (OEM_DENY_1_MATCH | OEM_DENY_2_MATCH | OEM_DENY_3_MATCH)) != 0) {
            blockedReasons |= BLOCKED_REASON_OEM_DENY;
        }

        // Metered chains are not enabled by configuration map currently.
        if ((uidMatch & PENALTY_BOX_USER_MATCH) != 0) {
            blockedReasons |= BLOCKED_METERED_REASON_USER_RESTRICTED;
        }
        if ((uidMatch & PENALTY_BOX_ADMIN_MATCH) != 0) {
            blockedReasons |= BLOCKED_METERED_REASON_ADMIN_DISABLED;
        }
        if ((uidMatch & HAPPY_BOX_MATCH) == 0 && getDataSaverEnabled(dataSaverEnabledMap)) {
            blockedReasons |= BLOCKED_METERED_REASON_DATA_SAVER;
        }

        return blockedReasons;
    }

    /**
     * Return whether the network is blocked by firewall chains for the given uid.
     *
     * Note that {@link #getDataSaverEnabled(IBpfMap)} has a latency before V.
     *
     * @param uid The target uid.
     * @param isNetworkMetered Whether the target network is metered.
     *
     * @return True if the network is blocked. Otherwise, false.
     * @throws ServiceSpecificException if the read fails.
     *
     * @hide
     */
    public static boolean isUidNetworkingBlocked(final int uid, boolean isNetworkMetered,
            IBpfMap<S32, U32> configurationMap,
            IBpfMap<S32, UidOwnerValue> uidOwnerMap,
            IBpfMap<S32, U8> dataSaverEnabledMap
    ) {
        throwIfPreT("isUidBlockedByFirewallChains is not available on pre-T devices");

        // System uid is not blocked by firewall chains, see bpf_progs/netd.c
        // TODO: use UserHandle.isCore() once it is accessible
        if (uid < Process.FIRST_APPLICATION_UID) {
            return false;
        }

        final int blockedReasons = getUidNetworkingBlockedReasons(
                uid,
                configurationMap,
                uidOwnerMap,
                dataSaverEnabledMap);
        if (isNetworkMetered) {
            return blockedReasons != BLOCKED_REASON_NONE;
        } else {
            return (blockedReasons & ~BLOCKED_METERED_REASON_MASK) != BLOCKED_REASON_NONE;
        }
    }

    /**
     * Get Data Saver enabled or disabled
     *
     * Note that before V, the data saver status in bpf is written by ConnectivityService
     * when receiving {@link ConnectivityManager#ACTION_RESTRICT_BACKGROUND_CHANGED}. Thus,
     * the status is not synchronized.
     * On V+, the data saver status is set by platform code when enabling/disabling
     * data saver, which is synchronized.
     *
     * @return whether Data Saver is enabled or disabled.
     * @throws ServiceSpecificException in case of failure, with an error code indicating the
     *                                  cause of the failure.
     */
    public static boolean getDataSaverEnabled(IBpfMap<S32, U8> dataSaverEnabledMap) {
        throwIfPreT("getDataSaverEnabled is not available on pre-T devices");

        try {
            return dataSaverEnabledMap.getValue(DATA_SAVER_ENABLED_KEY).val == DATA_SAVER_ENABLED;
        } catch (ErrnoException e) {
            throw new ServiceSpecificException(e.errno, "Unable to get data saver: "
                    + Os.strerror(e.errno));
        }
    }
}
