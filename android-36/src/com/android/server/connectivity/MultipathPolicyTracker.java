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

package com.android.server.connectivity;

import static android.net.ConnectivityManager.MULTIPATH_PREFERENCE_HANDOVER;
import static android.net.ConnectivityManager.MULTIPATH_PREFERENCE_RELIABILITY;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicy.WARNING_DISABLED;
import static android.provider.Settings.Global.NETWORK_DEFAULT_DAILY_MULTIPATH_QUOTA_BYTES;

import static com.android.server.net.NetworkPolicyManagerInternal.QUOTA_TYPE_MULTIPATH;
import static com.android.server.net.NetworkPolicyManagerService.OPPORTUNISTIC_QUOTA_UNKNOWN;

import android.app.usage.NetworkStatsManager;
import android.app.usage.NetworkStatsManager.UsageCallback;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkIdentity;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.TelephonyNetworkSpecifier;
import android.net.Uri;
import android.os.BestClock;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DebugUtils;
import android.util.Log;
import android.util.Range;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.net.NetworkPolicyManagerInternal;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages multipath data budgets.
 *
 * Informs the return value of ConnectivityManager#getMultipathPreference() based on:
 * - The user's data plan, as returned by getSubscriptionOpportunisticQuota().
 * - The amount of data usage that occurs on mobile networks while they are not the system default
 *   network (i.e., when the app explicitly selected such networks).
 *
 * Currently, quota is determined on a daily basis, from midnight to midnight local time.
 *
 * @hide
 */
public class MultipathPolicyTracker {
    private static String TAG = MultipathPolicyTracker.class.getSimpleName();

    private static final boolean DBG = false;
    private static final long MIN_THRESHOLD_BYTES = 2 * 1_048_576L; // 2MiB

    // This context is for the current user.
    private final Context mContext;
    // This context is for all users, so register a BroadcastReceiver which can receive intents from
    // all users.
    private final Context mUserAllContext;
    private final Handler mHandler;
    private final Clock mClock;
    private final Dependencies mDeps;
    private final ContentResolver mResolver;
    private final ConfigChangeReceiver mConfigChangeReceiver;

    @VisibleForTesting
    final ContentObserver mSettingsObserver;

    private ConnectivityManager mCM;
    private NetworkPolicyManager mNPM;
    private NetworkStatsManager mStatsManager;

    private NetworkCallback mMobileNetworkCallback;
    private NetworkPolicyManager.Listener mPolicyListener;


    /**
     * Divider to calculate opportunistic quota from user-set data limit or warning: 5% of user-set
     * limit.
     */
    private static final int OPQUOTA_USER_SETTING_DIVIDER = 20;

    public static class Dependencies {
        public Clock getClock() {
            return new BestClock(ZoneOffset.UTC, SystemClock.currentNetworkTimeClock(),
                    Clock.systemUTC());
        }
    }

    public MultipathPolicyTracker(Context ctx, Handler handler) {
        this(ctx, handler, new Dependencies());
    }

    public MultipathPolicyTracker(Context ctx, Handler handler, Dependencies deps) {
        mContext = ctx;
        mUserAllContext = ctx.createContextAsUser(UserHandle.ALL, 0 /* flags */);
        mHandler = handler;
        mClock = deps.getClock();
        mDeps = deps;
        mResolver = mContext.getContentResolver();
        mSettingsObserver = new SettingsObserver(mHandler);
        mConfigChangeReceiver = new ConfigChangeReceiver();
        // Because we are initialized by the ConnectivityService constructor, we can't touch any
        // connectivity APIs. Service initialization is done in start().
    }

    public void start() {
        mCM = mContext.getSystemService(ConnectivityManager.class);
        mNPM = mContext.getSystemService(NetworkPolicyManager.class);
        mStatsManager = mContext.getSystemService(NetworkStatsManager.class);

        registerTrackMobileCallback();
        registerNetworkPolicyListener();
        final Uri defaultSettingUri =
                Settings.Global.getUriFor(NETWORK_DEFAULT_DAILY_MULTIPATH_QUOTA_BYTES);
        mResolver.registerContentObserver(defaultSettingUri, false, mSettingsObserver);

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        mUserAllContext.registerReceiver(
                mConfigChangeReceiver, intentFilter, null /* broadcastPermission */, mHandler);
    }

    public void shutdown() {
        maybeUnregisterTrackMobileCallback();
        unregisterNetworkPolicyListener();
        for (MultipathTracker t : mMultipathTrackers.values()) {
            t.shutdown();
        }
        mMultipathTrackers.clear();
        mResolver.unregisterContentObserver(mSettingsObserver);
        mUserAllContext.unregisterReceiver(mConfigChangeReceiver);
    }

    // Called on an arbitrary binder thread.
    public Integer getMultipathPreference(Network network) {
        if (network == null) {
            return null;
        }
        MultipathTracker t = mMultipathTrackers.get(network);
        if (t != null) {
            return t.getMultipathPreference();
        }
        return null;
    }

    // Track information on mobile networks as they come and go.
    class MultipathTracker {
        final Network network;
        final String subscriberId;
        private final int mSubId;

        private long mQuota;
        /** Current multipath budget. Nonzero iff we have budget. */
        // The budget could be accessed by multiple threads, make it volatile to ensure the callers
        // on a different thread will not see the stale value.
        private volatile long mMultipathBudget;
        private final NetworkTemplate mNetworkTemplate;
        private final UsageCallback mUsageCallback;
        private boolean mUsageCallbackRegistered = false;
        private NetworkCapabilities mNetworkCapabilities;
        private final NetworkStatsManager mStatsManager;

        public MultipathTracker(Network network, NetworkCapabilities nc) {
            this.network = network;
            this.mNetworkCapabilities = new NetworkCapabilities(nc);
            NetworkSpecifier specifier = nc.getNetworkSpecifier();
            if (specifier instanceof TelephonyNetworkSpecifier) {
                mSubId = ((TelephonyNetworkSpecifier) specifier).getSubscriptionId();
            } else {
                throw new IllegalStateException(String.format(
                        "Can't get subId from mobile network %s (%s)",
                        network, nc));
            }

            TelephonyManager tele = mContext.getSystemService(TelephonyManager.class);
            if (tele == null) {
                throw new IllegalStateException(String.format("Missing TelephonyManager"));
            }
            tele = tele.createForSubscriptionId(mSubId);
            if (tele == null) {
                throw new IllegalStateException(String.format(
                        "Can't get TelephonyManager for subId %d", mSubId));
            }

            subscriberId = tele.getSubscriberId();
            if (subscriberId == null) {
                throw new IllegalStateException("Null subscriber Id for subId " + mSubId);
            }
            mNetworkTemplate = new NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE)
                    .setSubscriberIds(Set.of(subscriberId))
                    .setMeteredness(NetworkStats.METERED_YES)
                    .setDefaultNetworkStatus(NetworkStats.DEFAULT_NETWORK_NO)
                    .build();
            mUsageCallback = new UsageCallback() {
                @Override
                public void onThresholdReached(int networkType, String subscriberId) {
                    if (DBG) Log.d(TAG, "onThresholdReached for network " + network);
                    updateMultipathBudget();
                }
            };
            mStatsManager = mContext.getSystemService(NetworkStatsManager.class);
            // Query stats from NetworkStatsService will trigger a poll by default.
            // But since MultipathPolicyTracker listens NPMS events that triggered by
            // stats updated event, and will query stats
            // after the event. A polling -> updated -> query -> polling loop will be introduced
            // if polls on open. Hence, set flag to false to prevent a polling loop.
            mStatsManager.setPollOnOpen(false);

            updateMultipathBudget();
        }

        public void setNetworkCapabilities(NetworkCapabilities nc) {
            mNetworkCapabilities = new NetworkCapabilities(nc);
        }

        // TODO: calculate with proper timezone information
        private long getDailyNonDefaultDataUsage() {
            final ZonedDateTime end =
                    ZonedDateTime.ofInstant(mClock.instant(), ZoneId.systemDefault());
            final ZonedDateTime start = end.truncatedTo(ChronoUnit.DAYS);

            final long bytes = getNetworkTotalBytes(
                    start.toInstant().toEpochMilli(),
                    end.toInstant().toEpochMilli());
            if (DBG) Log.d(TAG, "Non-default data usage: " + bytes);
            return bytes;
        }

        private long getNetworkTotalBytes(long start, long end) {
            try {
                final android.app.usage.NetworkStats.Bucket ret =
                        mStatsManager.querySummaryForDevice(mNetworkTemplate, start, end);
                return ret.getRxBytes() + ret.getTxBytes();
            } catch (RuntimeException e) {
                Log.w(TAG, "Failed to get data usage: " + e);
                return -1;
            }
        }

        private NetworkIdentity getTemplateMatchingNetworkIdentity(NetworkCapabilities nc) {
            return new NetworkIdentity.Builder().setType(ConnectivityManager.TYPE_MOBILE)
                    .setSubscriberId(subscriberId)
                    .setRoaming(!nc.hasCapability(NET_CAPABILITY_NOT_ROAMING))
                    .setMetered(!nc.hasCapability(NET_CAPABILITY_NOT_METERED))
                    .setSubId(mSubId)
                    .build();
        }

        private long getRemainingDailyBudget(long limitBytes,
                Range<ZonedDateTime> cycle) {
            final long start = cycle.getLower().toInstant().toEpochMilli();
            final long end = cycle.getUpper().toInstant().toEpochMilli();
            final long totalBytes = getNetworkTotalBytes(start, end);
            final long remainingBytes = totalBytes == -1 ? 0 : Math.max(0, limitBytes - totalBytes);
            // 1 + ((end - now - 1) / millisInDay with integers is equivalent to:
            // ceil((double)(end - now) / millisInDay)
            final long remainingDays =
                    1 + ((end - mClock.millis() - 1) / TimeUnit.DAYS.toMillis(1));

            return remainingBytes / Math.max(1, remainingDays);
        }

        private long getUserPolicyOpportunisticQuotaBytes() {
            // Keep the most restrictive applicable policy
            long minQuota = Long.MAX_VALUE;
            final NetworkIdentity identity = getTemplateMatchingNetworkIdentity(
                    mNetworkCapabilities);

            final NetworkPolicy[] policies = mNPM.getNetworkPolicies();
            for (NetworkPolicy policy : policies) {
                if (policy.hasCycle() && policy.template.matches(identity)) {
                    final long cycleStart = policy.cycleIterator().next().getLower()
                            .toInstant().toEpochMilli();
                    // Prefer user-defined warning, otherwise use hard limit
                    final long activeWarning = getActiveWarning(policy, cycleStart);
                    final long policyBytes = (activeWarning == WARNING_DISABLED)
                            ? getActiveLimit(policy, cycleStart)
                            : activeWarning;

                    if (policyBytes != LIMIT_DISABLED && policyBytes != WARNING_DISABLED) {
                        final long policyBudget = getRemainingDailyBudget(policyBytes,
                                policy.cycleIterator().next());
                        minQuota = Math.min(minQuota, policyBudget);
                    }
                }
            }

            if (minQuota == Long.MAX_VALUE) {
                return OPPORTUNISTIC_QUOTA_UNKNOWN;
            }

            return minQuota / OPQUOTA_USER_SETTING_DIVIDER;
        }

        void updateMultipathBudget() {
            long quota = LocalServices.getService(NetworkPolicyManagerInternal.class)
                    .getSubscriptionOpportunisticQuota(this.network, QUOTA_TYPE_MULTIPATH);
            if (DBG) Log.d(TAG, "Opportunistic quota from data plan: " + quota + " bytes");

            // Fallback to user settings-based quota if not available from phone plan
            if (quota == OPPORTUNISTIC_QUOTA_UNKNOWN) {
                quota = getUserPolicyOpportunisticQuotaBytes();
                if (DBG) Log.d(TAG, "Opportunistic quota from user policy: " + quota + " bytes");
            }

            if (quota == OPPORTUNISTIC_QUOTA_UNKNOWN) {
                quota = getDefaultDailyMultipathQuotaBytes();
                if (DBG) Log.d(TAG, "Setting quota: " + quota + " bytes");
            }

            // TODO: re-register if day changed: budget may have run out but should be refreshed.
            if (haveMultipathBudget() && quota == mQuota) {
                // If there is already a usage callback pending , there's no need to re-register it
                // if the quota hasn't changed. The callback will simply fire as expected when the
                // budget is spent.
                if (DBG) Log.d(TAG, "Quota still " + quota + ", not updating.");
                return;
            }
            mQuota = quota;

            // If we can't get current usage, assume the worst and don't give
            // ourselves any budget to work with.
            final long usage = getDailyNonDefaultDataUsage();
            final long budget = (usage == -1) ? 0 : Math.max(0, quota - usage);

            // Only consider budgets greater than MIN_THRESHOLD_BYTES, otherwise the callback will
            // fire late, after data usage went over budget. Also budget should be 0 if remaining
            // data is close to 0.
            // This is necessary because the usage callback does not accept smaller thresholds.
            // Because it snaps everything to MIN_THRESHOLD_BYTES, the lesser of the two evils is
            // to snap to 0 here.
            // This will only be called if the total quota for the day changed, not if usage changed
            // since last time, so even if this is called very often the budget will not snap to 0
            // as soon as there are less than 2MB left for today.
            if (budget > MIN_THRESHOLD_BYTES) {
                if (DBG) {
                    Log.d(TAG, "Setting callback for " + budget + " bytes on network " + network);
                }
                setMultipathBudget(budget);
            } else {
                clearMultipathBudget();
            }
        }

        public int getMultipathPreference() {
            if (haveMultipathBudget()) {
                return MULTIPATH_PREFERENCE_HANDOVER | MULTIPATH_PREFERENCE_RELIABILITY;
            }
            return 0;
        }

        // For debugging only.
        public long getQuota() {
            return mQuota;
        }

        // For debugging only.
        public long getMultipathBudget() {
            return mMultipathBudget;
        }

        private boolean haveMultipathBudget() {
            return mMultipathBudget > 0;
        }

        // Sets the budget and registers a usage callback for it.
        private void setMultipathBudget(long budget) {
            maybeUnregisterUsageCallback();
            if (DBG) Log.d(TAG, "Registering callback, budget is " + mMultipathBudget);
            mStatsManager.registerUsageCallback(mNetworkTemplate, budget,
                    (command) -> mHandler.post(command), mUsageCallback);
            mUsageCallbackRegistered = true;
            mMultipathBudget = budget;
        }

        private void maybeUnregisterUsageCallback() {
            if (!mUsageCallbackRegistered) return;
            if (DBG) Log.d(TAG, "Unregistering callback, budget was " + mMultipathBudget);
            mStatsManager.unregisterUsageCallback(mUsageCallback);
            mUsageCallbackRegistered = false;
        }

        private void clearMultipathBudget() {
            maybeUnregisterUsageCallback();
            mMultipathBudget = 0;
        }

        void shutdown() {
            clearMultipathBudget();
        }
    }

    private static long getActiveWarning(NetworkPolicy policy, long cycleStart) {
        return policy.lastWarningSnooze < cycleStart
                ? policy.warningBytes
                : WARNING_DISABLED;
    }

    private static long getActiveLimit(NetworkPolicy policy, long cycleStart) {
        return policy.lastLimitSnooze < cycleStart
                ? policy.limitBytes
                : LIMIT_DISABLED;
    }

    // Only ever updated on the handler thread. Accessed from other binder threads to retrieve
    // the tracker for a specific network.
    private final ConcurrentHashMap <Network, MultipathTracker> mMultipathTrackers =
            new ConcurrentHashMap<>();

    private long getDefaultDailyMultipathQuotaBytes() {
        final String setting = Settings.Global.getString(mContext.getContentResolver(),
                NETWORK_DEFAULT_DAILY_MULTIPATH_QUOTA_BYTES);
        if (setting != null) {
            try {
                return Long.parseLong(setting);
            } catch(NumberFormatException e) {
                // fall through
            }
        }

        return mContext.getResources().getInteger(
                R.integer.config_networkDefaultDailyMultipathQuotaBytes);
    }

    // TODO: this races with app code that might respond to onAvailable() by immediately calling
    // getMultipathPreference. Fix this by adding to ConnectivityService the ability to directly
    // invoke NetworkCallbacks on tightly-coupled classes such as this one which run on its
    // handler thread.
    private void registerTrackMobileCallback() {
        final NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_INTERNET)
                .addTransportType(TRANSPORT_CELLULAR)
                .build();
        mMobileNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                MultipathTracker existing = mMultipathTrackers.get(network);
                if (existing != null) {
                    existing.setNetworkCapabilities(nc);
                    existing.updateMultipathBudget();
                    return;
                }

                try {
                    mMultipathTrackers.put(network, new MultipathTracker(network, nc));
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Can't track mobile network " + network + ": " + e.getMessage());
                }
                if (DBG) Log.d(TAG, "Tracking mobile network " + network);
            }

            @Override
            public void onLost(Network network) {
                MultipathTracker existing = mMultipathTrackers.get(network);
                if (existing != null) {
                    existing.shutdown();
                    mMultipathTrackers.remove(network);
                }
                if (DBG) Log.d(TAG, "No longer tracking mobile network " + network);
            }
        };

        mCM.registerNetworkCallback(request, mMobileNetworkCallback, mHandler);
    }

    /**
     * Update multipath budgets for all trackers. To be called on the mHandler thread.
     */
    private void updateAllMultipathBudgets() {
        for (MultipathTracker t : mMultipathTrackers.values()) {
            t.updateMultipathBudget();
        }
    }

    private void maybeUnregisterTrackMobileCallback() {
        if (mMobileNetworkCallback != null) {
            mCM.unregisterNetworkCallback(mMobileNetworkCallback);
        }
        mMobileNetworkCallback = null;
    }

    private void registerNetworkPolicyListener() {
        mPolicyListener = new NetworkPolicyManager.Listener() {
            @Override
            public void onMeteredIfacesChanged(String[] meteredIfaces) {
                // Dispatched every time opportunistic quota is recalculated.
                mHandler.post(() -> updateAllMultipathBudgets());
            }
        };
        mNPM.registerListener(mPolicyListener);
    }

    private void unregisterNetworkPolicyListener() {
        mNPM.unregisterListener(mPolicyListener);
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.wtf(TAG, "Should never be reached.");
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (!Settings.Global.getUriFor(NETWORK_DEFAULT_DAILY_MULTIPATH_QUOTA_BYTES)
                    .equals(uri)) {
                Log.wtf(TAG, "Unexpected settings observation: " + uri);
            }
            if (DBG) Log.d(TAG, "Settings change: updating budgets.");
            updateAllMultipathBudgets();
        }
    }

    private final class ConfigChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DBG) Log.d(TAG, "Configuration change: updating budgets.");
            updateAllMultipathBudgets();
        }
    }

    public void dump(IndentingPrintWriter pw) {
        // Do not use in production. Access to class data is only safe on the handler thrad.
        pw.println("MultipathPolicyTracker:");
        pw.increaseIndent();
        for (MultipathTracker t : mMultipathTrackers.values()) {
            pw.println(String.format("Network %s: quota %d, budget %d. Preference: %s",
                    t.network, t.getQuota(), t.getMultipathBudget(),
                    DebugUtils.flagsToString(ConnectivityManager.class, "MULTIPATH_PREFERENCE_",
                            t.getMultipathPreference())));
        }
        pw.decreaseIndent();
    }
}
