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

package android.permission;

import static android.Manifest.permission_group.CAMERA;
import static android.Manifest.permission_group.LOCATION;
import static android.Manifest.permission_group.MICROPHONE;
import static android.app.AppOpsManager.ATTRIBUTION_CHAIN_ID_NONE;
import static android.app.AppOpsManager.ATTRIBUTION_FLAGS_NONE;
import static android.app.AppOpsManager.ATTRIBUTION_FLAG_ACCESSOR;
import static android.app.AppOpsManager.ATTRIBUTION_FLAG_RECEIVER;
import static android.app.AppOpsManager.ATTRIBUTION_FLAG_TRUSTED;
import static android.app.AppOpsManager.AttributionFlags;
import static android.app.AppOpsManager.OPSTR_CAMERA;
import static android.app.AppOpsManager.OPSTR_COARSE_LOCATION;
import static android.app.AppOpsManager.OPSTR_FINE_LOCATION;
import static android.app.AppOpsManager.OPSTR_PHONE_CALL_CAMERA;
import static android.app.AppOpsManager.OPSTR_PHONE_CALL_MICROPHONE;
import static android.app.AppOpsManager.OPSTR_RECEIVE_AMBIENT_TRIGGER_AUDIO;
import static android.app.AppOpsManager.OPSTR_RECORD_AUDIO;
import static android.app.AppOpsManager.OP_CAMERA;
import static android.app.AppOpsManager.OP_FLAGS_ALL_TRUSTED;
import static android.app.AppOpsManager.OP_RECORD_AUDIO;
import static android.media.AudioSystem.MODE_IN_COMMUNICATION;
import static android.telephony.TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.Attribution;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.icu.text.ListFormatter;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Process;
import android.os.UserHandle;
import android.permission.flags.Flags;
import android.provider.DeviceConfig;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A helper which gets all apps which have used microphone, camera, and possible location
 * permissions within a certain timeframe, as well as possible special attributions, and if the
 * usage is a phone call.
 *
 * @hide
 */
public class PermissionUsageHelper implements AppOpsManager.OnOpActiveChangedListener,
        AppOpsManager.OnOpStartedListener {

    private static final String LOG_TAG = PermissionUsageHelper.class.getName();

    /**
     * Whether to show the mic and camera icons.
     */
    private static final String PROPERTY_CAMERA_MIC_ICONS_ENABLED = "camera_mic_icons_enabled";

    /**
     * Whether to show the location indicators.
     */
    private static final String PROPERTY_LOCATION_INDICATORS_ENABLED =
            "location_indicators_enabled";

    /**
     * How long after an access to show it as "recent"
     */
    private static final String RECENT_ACCESS_TIME_MS = "recent_access_time_ms";

    /**
     * How long after an access to show it as "running"
     */
    private static final String RUNNING_ACCESS_TIME_MS = "running_access_time_ms";

    private static final String SYSTEM_PKG = "android";

    private static final long DEFAULT_RUNNING_TIME_MS = 5000L;
    private static final long DEFAULT_RECENT_TIME_MS = 15000L;

    private static boolean shouldShowIndicators() {
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                PROPERTY_CAMERA_MIC_ICONS_ENABLED, true);
    }

    private static boolean shouldShowLocationIndicator() {
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                PROPERTY_LOCATION_INDICATORS_ENABLED, false);
    }

    private static long getRecentThreshold(Long now) {
        return now - DeviceConfig.getLong(DeviceConfig.NAMESPACE_PRIVACY,
                RECENT_ACCESS_TIME_MS, DEFAULT_RECENT_TIME_MS);
    }

    private static long getRunningThreshold(Long now) {
        return now - DeviceConfig.getLong(DeviceConfig.NAMESPACE_PRIVACY,
                RUNNING_ACCESS_TIME_MS, DEFAULT_RUNNING_TIME_MS);
    }

    private static final List<String> LOCATION_OPS = List.of(
            OPSTR_COARSE_LOCATION,
            OPSTR_FINE_LOCATION
    );

    private static final List<String> MIC_OPS = List.of(
            OPSTR_PHONE_CALL_MICROPHONE,
            OPSTR_RECEIVE_AMBIENT_TRIGGER_AUDIO,
            OPSTR_RECORD_AUDIO
    );

    private static final List<String> CAMERA_OPS = List.of(
            OPSTR_PHONE_CALL_CAMERA,
            OPSTR_CAMERA
    );

    private static @NonNull String getGroupForOp(String op) {
        switch (op) {
            case OPSTR_RECEIVE_AMBIENT_TRIGGER_AUDIO:
            case OPSTR_RECORD_AUDIO:
                return MICROPHONE;
            case OPSTR_CAMERA:
                return CAMERA;
            case OPSTR_PHONE_CALL_MICROPHONE:
            case OPSTR_PHONE_CALL_CAMERA:
                return op;
            case OPSTR_COARSE_LOCATION:
            case OPSTR_FINE_LOCATION:
                return LOCATION;
            default:
                throw new IllegalArgumentException("Unknown app op: " + op);
        }
    }

    private Context mContext;
    private ArrayMap<UserHandle, Context> mUserContexts;
    private PackageManager mPkgManager;
    private AppOpsManager mAppOpsManager;
    private VirtualDeviceManager mVirtualDeviceManager;
    @GuardedBy("mAttributionChains")
    private final ArrayMap<Integer, ArrayList<AccessChainLink>> mAttributionChains =
            new ArrayMap<>();

    /**
     * Constructor for PermissionUsageHelper
     *
     * @param context The context from which to derive the package information
     */
    public PermissionUsageHelper(@NonNull Context context) {
        mContext = context;
        mPkgManager = context.getPackageManager();
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mVirtualDeviceManager = context.getSystemService(VirtualDeviceManager.class);
        mUserContexts = new ArrayMap<>();
        mUserContexts.put(Process.myUserHandle(), mContext);
        // TODO ntmyren: make this listen for flag enable/disable changes
        String[] opStrs = {OPSTR_CAMERA, OPSTR_RECORD_AUDIO};
        mAppOpsManager.startWatchingActive(opStrs, context.getMainExecutor(), this);
        int[] ops = {OP_CAMERA, OP_RECORD_AUDIO};
        mAppOpsManager.startWatchingStarted(ops, this);
    }

    private Context getUserContext(UserHandle user) {
        if (!(mUserContexts.containsKey(user))) {
            mUserContexts.put(user, mContext.createContextAsUser(user, 0));
        }
        return mUserContexts.get(user);
    }

    public void tearDown() {
        mAppOpsManager.stopWatchingActive(this);
        mAppOpsManager.stopWatchingStarted(this);
    }

    @Override
    public void onOpActiveChanged(@NonNull String op, int uid, @NonNull String packageName,
            boolean active) {
        // not part of an attribution chain. Do nothing
    }

    @Override
    public void onOpActiveChanged(@NonNull String op, int uid, @NonNull String packageName,
            @Nullable String attributionTag, boolean active, @AttributionFlags int attributionFlags,
            int attributionChainId) {
        if (active) {
            // Started callback handles these
            return;
        }

        // if any link in the chain is finished, remove the chain. Then, find any other chains that
        // contain this op/package/uid/tag combination, and remove them, as well.
        // TODO ntmyren: be smarter about this
        synchronized (mAttributionChains) {
            mAttributionChains.remove(attributionChainId);
            int numChains = mAttributionChains.size();
            ArrayList<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < numChains; i++) {
                int chainId = mAttributionChains.keyAt(i);
                ArrayList<AccessChainLink> chain = mAttributionChains.valueAt(i);
                int chainSize = chain.size();
                for (int j = 0; j < chainSize; j++) {
                    AccessChainLink link = chain.get(j);
                    if (link.packageAndOpEquals(op, packageName, attributionTag, uid)) {
                        toRemove.add(chainId);
                        break;
                    }
                }
            }
            mAttributionChains.removeAll(toRemove);
        }
    }

    @Override
    public void onOpStarted(int op, int uid, String packageName, String attributionTag,
            @AppOpsManager.OpFlags int flags, @AppOpsManager.Mode int result) {
        // not part of an attribution chain. Do nothing
    }

    @Override
    public void onOpStarted(int op, int uid, String packageName, String attributionTag,
            @AppOpsManager.OpFlags int flags, @AppOpsManager.Mode int result,
            @StartedType int startedType, @AttributionFlags int attributionFlags,
            int attributionChainId) {
        if (startedType == START_TYPE_FAILED || attributionChainId == ATTRIBUTION_CHAIN_ID_NONE
                || attributionFlags == ATTRIBUTION_FLAGS_NONE
                || (attributionFlags & ATTRIBUTION_FLAG_TRUSTED) == 0) {
            // If this is not a successful start, or it is not a chain, or it is untrusted, return
            return;
        }
        synchronized (mAttributionChains) {
            addLinkToChainIfNotPresentLocked(AppOpsManager.opToPublicName(op), packageName, uid,
                    attributionTag, attributionFlags, attributionChainId);
        }
    }

    private void addLinkToChainIfNotPresentLocked(String op, String packageName, int uid,
            String attributionTag, int attributionFlags, int attributionChainId) {

        ArrayList<AccessChainLink> currentChain = mAttributionChains.computeIfAbsent(
                attributionChainId, k -> new ArrayList<>());
        AccessChainLink link = new AccessChainLink(op, packageName, attributionTag, uid,
                attributionFlags);

        if (currentChain.contains(link)) {
            return;
        }

        int currSize = currentChain.size();
        if (currSize == 0 || link.isEnd() || !currentChain.get(currSize - 1).isEnd()) {
            // if the list is empty, this link is the end, or the last link in the current chain
            // isn't the end, add it to the end
            currentChain.add(link);
        } else if (link.isStart()) {
            currentChain.add(0, link);
        } else if (currentChain.get(currentChain.size() - 1).isEnd()) {
            // we already have the end, and this is a mid node, so insert before the end
            currentChain.add(currSize - 1, link);
        }
    }

    /**
     * Return Op usage for CAMERA, LOCATION AND MICROPHONE for all packages for a device.
     * The returned data is to power privacy indicator.
     */
    public @NonNull List<PermissionGroupUsage> getOpUsageDataByDevice(
            boolean includeMicrophoneUsage, String deviceId) {
        List<PermissionGroupUsage> usages = new ArrayList<>();

        if (!shouldShowIndicators()) {
            return usages;
        }

        List<String> ops = new ArrayList<>(CAMERA_OPS);
        if (shouldShowLocationIndicator()) {
            ops.addAll(LOCATION_OPS);
        }
        if (includeMicrophoneUsage) {
            ops.addAll(MIC_OPS);
        }

        Map<String, List<OpUsage>> rawUsages = getOpUsagesByDevice(ops, deviceId);

        ArrayList<String> usedPermGroups = new ArrayList<>(rawUsages.keySet());

        // If we have a phone call, and a carrier privileged app using microphone, hide the
        // phone call.
        AudioManager audioManager = mContext.getSystemService(AudioManager.class);
        boolean hasPhoneCall = usedPermGroups.contains(OPSTR_PHONE_CALL_CAMERA)
                || usedPermGroups.contains(OPSTR_PHONE_CALL_MICROPHONE);
        if (hasPhoneCall && usedPermGroups.contains(MICROPHONE) && audioManager.getMode()
                == MODE_IN_COMMUNICATION) {
            TelephonyManager telephonyManager =
                    mContext.getSystemService(TelephonyManager.class);
            List<OpUsage> permUsages = rawUsages.get(MICROPHONE);
            for (int usageNum = 0; usageNum < permUsages.size(); usageNum++) {
                if (telephonyManager.checkCarrierPrivilegesForPackage(
                        permUsages.get(usageNum).packageName)
                        == CARRIER_PRIVILEGE_STATUS_HAS_ACCESS) {
                    usedPermGroups.remove(OPSTR_PHONE_CALL_CAMERA);
                    usedPermGroups.remove(OPSTR_PHONE_CALL_MICROPHONE);
                }
            }
        }

        // map of package name -> map of attribution tag -> attribution labels
        ArrayMap<String, Map<String, String>> subAttributionLabelsMap = new ArrayMap<>();

        for (int permGroupNum = 0; permGroupNum < usedPermGroups.size(); permGroupNum++) {
            boolean isPhone = false;
            String permGroup = usedPermGroups.get(permGroupNum);

            ArrayMap<OpUsage, CharSequence> usagesWithLabels =
                    getUniqueUsagesWithLabels(permGroup, rawUsages.get(permGroup));

            updateSubattributionLabelsMap(rawUsages.get(permGroup), subAttributionLabelsMap);

            if (permGroup.equals(OPSTR_PHONE_CALL_MICROPHONE)) {
                isPhone = true;
                permGroup = MICROPHONE;
            } else if (permGroup.equals(OPSTR_PHONE_CALL_CAMERA)) {
                isPhone = true;
                permGroup = CAMERA;
            }

            for (int usageNum = 0; usageNum < usagesWithLabels.size(); usageNum++) {
                OpUsage usage = usagesWithLabels.keyAt(usageNum);
                String attributionLabel = subAttributionLabelsMap.getOrDefault(usage.packageName,
                        new ArrayMap<>()).getOrDefault(usage.attributionTag, null);
                usages.add(
                        new PermissionGroupUsage(usage.packageName, usage.uid, usage.lastAccessTime,
                                permGroup,
                                usage.isRunning, isPhone, usage.attributionTag, attributionLabel,
                                usagesWithLabels.valueAt(usageNum), deviceId));
            }
        }

        return usages;
    }

    /**
     * Return Op usage for CAMERA, LOCATION AND MICROPHONE for all packages and all connected
     * devices.
     * The returned data is to power privacy indicator.
     */
    public @NonNull List<PermissionGroupUsage> getOpUsageDataForAllDevices(
            boolean includeMicrophoneUsage) {
        List<PermissionGroupUsage> allUsages = new ArrayList<>();

        if (mVirtualDeviceManager != null) {
            List<VirtualDevice> virtualDevices = mVirtualDeviceManager.getVirtualDevices();
            ArraySet<String> persistentDeviceIds = new ArraySet<>();

            for (int num = 0; num < virtualDevices.size(); num++) {
                persistentDeviceIds.add(virtualDevices.get(num).getPersistentDeviceId());
            }
            persistentDeviceIds.add(VirtualDeviceManager.PERSISTENT_DEVICE_ID_DEFAULT);

            for (int index = 0; index < persistentDeviceIds.size(); index++) {
                allUsages.addAll(
                        getOpUsageDataByDevice(includeMicrophoneUsage,
                                persistentDeviceIds.valueAt(index)));
            }
        }
        return allUsages;
    }


    private void updateSubattributionLabelsMap(List<OpUsage> usages,
            ArrayMap<String, Map<String, String>> subAttributionLabelsMap) {
        if (usages == null || usages.isEmpty()) {
            return;
        }
        for (OpUsage usage : usages) {
            if (usage.attributionTag != null && !subAttributionLabelsMap.containsKey(
                    usage.packageName)) {
                subAttributionLabelsMap.put(usage.packageName,
                        getSubattributionLabelsForPackage(usage.packageName, usage.uid));
            }
        }
    }

    /**
     * Query attribution labels for a package
     *
     * @param packageName
     * @param uid
     * @return map of attribution tag -> attribution labels for a package
     */
    private ArrayMap<String, String> getSubattributionLabelsForPackage(String packageName,
            int uid) {
        ArrayMap<String, String> attributionLabelMap = new ArrayMap<>();
        UserHandle user = UserHandle.getUserHandleForUid(uid);
        try {
            if (!isSubattributionSupported(packageName, uid)) {
                return attributionLabelMap;
            }
            Context userContext = getUserContext(user);
            PackageInfo packageInfo = userContext.getPackageManager().getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(
                            PackageManager.GET_PERMISSIONS | PackageManager.GET_ATTRIBUTIONS_LONG));
            Context pkgContext = userContext.createPackageContext(packageInfo.packageName, 0);
            for (Attribution attribution : packageInfo.attributions) {
                try {
                    String resourceForLabel = pkgContext.getString(attribution.getLabel());
                    attributionLabelMap.put(attribution.getTag(), resourceForLabel);
                } catch (Resources.NotFoundException e) {
                    // Shouldn't happen, do nothing
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Did not find the package, do nothing
        }
        return attributionLabelMap;
    }

    /**
     * Returns true if the app satisfies subattribution policies and supports it
     */
    private boolean isSubattributionSupported(String packageName, int uid) {
        try {
            if (!isLocationProvider(packageName)) {
                return false;
            }
            PackageManager userPkgManager =
                    getUserContext(UserHandle.getUserHandleForUid(uid)).getPackageManager();
            ApplicationInfo appInfo = userPkgManager.getApplicationInfoAsUser(packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                    UserHandle.getUserId(uid));
            if (appInfo != null) {
                return appInfo.areAttributionsUserVisible();
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * @param packageName
     * @return If the package is location provider
     */
    private boolean isLocationProvider(String packageName) {
        return Objects.requireNonNull(
                mContext.getSystemService(LocationManager.class)).isProviderPackage(packageName);
    }

    /**
     * Get the raw usages from the system, and then parse out the ones that are not recent enough,
     * determine which permission group each belongs in, and removes duplicates (if the same app
     * uses multiple permissions of the same group). Stores the package name, attribution tag, user,
     * running/recent info, if the usage is a phone call, per permission group.
     *
     * @param opNames a list of op names to get usage for
     * @param deviceId which device to get op usage for
     * @return A map of permission group -> list of usages that are recent or running
     */
    private Map<String, List<OpUsage>> getOpUsagesByDevice(List<String> opNames, String deviceId) {
        List<AppOpsManager.PackageOps> ops;
        try {
            if (Flags.deviceAwarePermissionApisEnabled()) {
                ops = mAppOpsManager.getPackagesForOps(opNames.toArray(new String[opNames.size()]),
                        deviceId);
            } else if (!Objects.equals(deviceId,
                    VirtualDeviceManager.PERSISTENT_DEVICE_ID_DEFAULT)) {
                Slog.w(LOG_TAG,
                        "device_aware_permission_apis_enabled flag not enabled when deviceId is "
                                + "not default");
                return Collections.emptyMap();
            } else {
                ops = mAppOpsManager.getPackagesForOps(opNames.toArray(new String[opNames.size()]));
            }
        } catch (NullPointerException e) {
            // older builds might not support all the app-ops requested
            return Collections.emptyMap();
        }

        long now = System.currentTimeMillis();
        long recentThreshold = getRecentThreshold(now);
        long runningThreshold = getRunningThreshold(now);
        int opFlags = OP_FLAGS_ALL_TRUSTED;
        Map<String, Map<Integer, OpUsage>> usages = new ArrayMap<>();

        int numPkgOps = ops.size();
        for (int pkgOpNum = 0; pkgOpNum < numPkgOps; pkgOpNum++) {
            AppOpsManager.PackageOps pkgOps = ops.get(pkgOpNum);
            int uid = pkgOps.getUid();
            UserHandle user = UserHandle.getUserHandleForUid(uid);
            String packageName = pkgOps.getPackageName();

            int numOpEntries = pkgOps.getOps().size();
            for (int opEntryNum = 0; opEntryNum < numOpEntries; opEntryNum++) {
                AppOpsManager.OpEntry opEntry = pkgOps.getOps().get(opEntryNum);
                String op = opEntry.getOpStr();
                List<String> attributionTags =
                        new ArrayList<>(opEntry.getAttributedOpEntries().keySet());


                int numAttrEntries = opEntry.getAttributedOpEntries().size();
                for (int attrOpEntryNum = 0; attrOpEntryNum < numAttrEntries; attrOpEntryNum++) {
                    String attributionTag = attributionTags.get(attrOpEntryNum);
                    AppOpsManager.AttributedOpEntry attrOpEntry =
                            opEntry.getAttributedOpEntries().get(attributionTag);

                    long lastAccessTime = attrOpEntry.getLastAccessTime(opFlags);
                    if (attrOpEntry.isRunning()) {
                        lastAccessTime = now;
                    }

                    if (lastAccessTime < recentThreshold && !attrOpEntry.isRunning()) {
                        continue;
                    }

                    boolean isRunning = attrOpEntry.isRunning()
                            || lastAccessTime >= runningThreshold;

                    OpUsage proxyUsage = null;
                    AppOpsManager.OpEventProxyInfo proxy = attrOpEntry.getLastProxyInfo(opFlags);
                    if (proxy != null && proxy.getPackageName() != null) {
                        proxyUsage = new OpUsage(proxy.getPackageName(), proxy.getAttributionTag(),
                                op, proxy.getUid(), lastAccessTime, isRunning, null);
                    }

                    String permGroupName = getGroupForOp(op);
                    OpUsage usage = new OpUsage(packageName, attributionTag, op, uid,
                            lastAccessTime, isRunning, proxyUsage);

                    Integer packageAttr = usage.getPackageIdHash();
                    if (!usages.containsKey(permGroupName)) {
                        ArrayMap<Integer, OpUsage> map = new ArrayMap<>();
                        map.put(packageAttr, usage);
                        usages.put(permGroupName, map);
                    } else {
                        Map<Integer, OpUsage> permGroupUsages =
                                usages.get(permGroupName);
                        if (!permGroupUsages.containsKey(packageAttr)) {
                            permGroupUsages.put(packageAttr, usage);
                        } else if (usage.lastAccessTime
                                > permGroupUsages.get(packageAttr).lastAccessTime) {
                            permGroupUsages.put(packageAttr, usage);
                        }
                    }
                }
            }
        }

        Map<String, List<OpUsage>> flattenedUsages = new ArrayMap<>();
        List<String> permGroups = new ArrayList<>(usages.keySet());
        for (int i = 0; i < permGroups.size(); i++) {
            String permGroupName = permGroups.get(i);
            flattenedUsages.put(permGroupName, new ArrayList<>(usages.get(permGroupName).values()));
        }
        return flattenedUsages;
    }

    private CharSequence formatLabelList(List<CharSequence> labels) {
        return ListFormatter.getInstance().format(labels);
    }

    private ArrayMap<OpUsage, CharSequence> getUniqueUsagesWithLabels(String permGroup,
            List<OpUsage> usages) {
        ArrayMap<OpUsage, CharSequence> usagesAndLabels = new ArrayMap<>();

        if (usages == null || usages.isEmpty()) {
            return usagesAndLabels;
        }

        ArrayMap<Integer, OpUsage> allUsages = new ArrayMap<>();
        // map of packageName and uid hash -> most recent non-proxy-related usage for that uid.
        ArrayMap<Integer, OpUsage> mostRecentUsages = new ArrayMap<>();
        // set of all packages involved in a proxy usage
        ArraySet<Integer> proxyPackages = new ArraySet<>();
        // map of usage -> list of proxy app labels
        ArrayMap<OpUsage, ArrayList<CharSequence>> proxyLabels = new ArrayMap<>();
        // map of usage.proxy hash -> usage hash, telling us if a usage is a proxy
        ArrayMap<Integer, OpUsage> proxies = new ArrayMap<>();

        for (int i = 0; i < usages.size(); i++) {
            OpUsage usage = usages.get(i);
            allUsages.put(usage.getPackageIdHash(), usage);
            if (usage.proxy != null) {
                proxies.put(usage.proxy.getPackageIdHash(), usage);
            }
        }

        // find all possible end points for chains, and find the most recent of the rest of the uses
        for (int usageNum = 0; usageNum < usages.size(); usageNum++) {
            OpUsage usage = usages.get(usageNum);
            if (usage == null) {
                continue;
            }

            int usageAttr = usage.getPackageIdHash();
            // If this usage has a proxy, but is not a proxy, it is the end of a chain.
            // TODO remove once camera converted
            if (!proxies.containsKey(usageAttr) && usage.proxy != null
                    && !MICROPHONE.equals(permGroup)) {
                proxyLabels.put(usage, new ArrayList<>());
                proxyPackages.add(usage.getPackageIdHash());
            }
            // If this usage is not by the system, and is more recent than the next-most recent
            // for it's uid and package name, save it.
            int usageId = usage.getPackageIdHash();
            OpUsage lastMostRecent = mostRecentUsages.get(usageId);
            if (shouldShowPackage(usage.packageName) && (lastMostRecent == null
                    || usage.lastAccessTime > lastMostRecent.lastAccessTime)) {
                mostRecentUsages.put(usageId, usage);
            }
        }

        // get all the proxy labels
        for (int numStart = 0; numStart < proxyLabels.size(); numStart++) {
            OpUsage start = proxyLabels.keyAt(numStart);
            // Remove any non-proxy usage for the starting package
            mostRecentUsages.remove(start.getPackageIdHash());
            OpUsage currentUsage = proxyLabels.keyAt(numStart);
            ArrayList<CharSequence> proxyLabelList = proxyLabels.get(currentUsage);
            if (currentUsage == null || proxyLabelList == null) {
                continue;
            }
            int iterNum = 0;
            int maxUsages = allUsages.size();
            while (currentUsage.proxy != null) {

                if (allUsages.containsKey(currentUsage.proxy.getPackageIdHash())) {
                    currentUsage = allUsages.get(currentUsage.proxy.getPackageIdHash());
                } else {
                    // We are missing the proxy usage. This may be because it's a one-step trusted
                    // proxy. Check if we should show the proxy label, and show it, if so.
                    OpUsage proxy = currentUsage.proxy;
                    if (shouldShowPackage(proxy.packageName)) {
                        currentUsage = proxy;
                        // We've effectively added one usage, so increment the max number of usages
                        maxUsages++;
                    } else {
                        break;
                    }
                }

                if (currentUsage == null || iterNum == maxUsages
                        || currentUsage.getPackageIdHash() == start.getPackageIdHash()) {
                    // We have an invalid state, or a cycle, so break
                    break;
                }

                proxyPackages.add(currentUsage.getPackageIdHash());
                // Don't add an app label for the main app, or the system app
                if (!currentUsage.packageName.equals(start.packageName)
                        && shouldShowPackage(currentUsage.packageName)) {
                    try {
                        PackageManager userPkgManager =
                                getUserContext(currentUsage.getUser()).getPackageManager();
                        ApplicationInfo appInfo = userPkgManager.getApplicationInfo(
                                currentUsage.packageName, 0);
                        CharSequence appLabel = appInfo.loadLabel(userPkgManager);
                        // If we don't already have the app label add it
                        if (!proxyLabelList.contains(appLabel)) {
                            proxyLabelList.add(appLabel);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // Ignore
                    }
                }
                iterNum++;
            }

            // TODO ntmyren: remove this proxy logic once camera is converted to AttributionSource
            // For now: don't add mic proxy usages
            if (!MICROPHONE.equals(permGroup)) {
                usagesAndLabels.put(start,
                        proxyLabelList.isEmpty() ? null : formatLabelList(proxyLabelList));
            }
        }

        synchronized (mAttributionChains) {
            for (int i = 0; i < mAttributionChains.size(); i++) {
                List<AccessChainLink> usageList = mAttributionChains.valueAt(i);
                int lastVisible = usageList.size() - 1;
                // TODO ntmyren: remove this mic code once camera is converted to AttributionSource
                // if the list is empty or incomplete, do not show it.
                if (usageList.isEmpty() || !usageList.get(lastVisible).isEnd()
                        || !usageList.get(0).isStart()
                        || !permGroup.equals(getGroupForOp(usageList.get(0).usage.op))
                        || !MICROPHONE.equals(permGroup)) {
                    continue;
                }

                //TODO ntmyren: remove once camera etc. etc.
                for (AccessChainLink link : usageList) {
                    proxyPackages.add(link.usage.getPackageIdHash());
                }

                AccessChainLink start = usageList.get(0);
                AccessChainLink lastVisibleLink = usageList.get(lastVisible);
                while (lastVisible > 0 && !shouldShowPackage(lastVisibleLink.usage.packageName)) {
                    lastVisible--;
                    lastVisibleLink = usageList.get(lastVisible);
                }
                String proxyLabel = null;
                if (!lastVisibleLink.usage.packageName.equals(start.usage.packageName)) {
                    try {
                        PackageManager userPkgManager =
                                getUserContext(lastVisibleLink.usage.getUser()).getPackageManager();
                        ApplicationInfo appInfo = userPkgManager.getApplicationInfo(
                                lastVisibleLink.usage.packageName, 0);
                        proxyLabel = appInfo.loadLabel(userPkgManager).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        // do nothing
                    }
                }
                usagesAndLabels.put(start.usage, proxyLabel);
            }
        }

        for (int packageHash : mostRecentUsages.keySet()) {
            if (!proxyPackages.contains(packageHash)) {
                usagesAndLabels.put(mostRecentUsages.get(packageHash), null);
            }
        }

        return usagesAndLabels;
    }

    private boolean shouldShowPackage(String packageName) {
        return PermissionManager.shouldShowPackageForIndicatorCached(mContext, packageName);
    }

    /**
     * Represents the usage of an App op by a particular package and attribution
     */
    private static class OpUsage {

        public final String packageName;
        public final String attributionTag;
        public final String op;
        public final int uid;
        public final long lastAccessTime;
        public final OpUsage proxy;
        public final boolean isRunning;

        OpUsage(String packageName, String attributionTag, String op, int uid, long lastAccessTime,
                boolean isRunning, OpUsage proxy) {
            this.packageName = packageName;
            this.attributionTag = attributionTag;
            this.op = op;
            this.uid = uid;
            this.lastAccessTime = lastAccessTime;
            this.isRunning = isRunning;
            this.proxy = proxy;
        }

        public UserHandle getUser() {
            return UserHandle.getUserHandleForUid(uid);
        }

        public int getPackageIdHash() {
            return Objects.hash(packageName, uid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, attributionTag, op, uid, lastAccessTime, isRunning);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OpUsage)) {
                return false;
            }
            OpUsage other = (OpUsage) obj;
            return Objects.equals(packageName, other.packageName) && Objects.equals(attributionTag,
                    other.attributionTag) && Objects.equals(op, other.op) && uid == other.uid
                    && lastAccessTime == other.lastAccessTime && isRunning == other.isRunning;
        }
    }

    private static class AccessChainLink {
        public final OpUsage usage;
        public final @AttributionFlags int flags;

        AccessChainLink(String op, String packageName, String attributionTag, int uid,
                int flags) {
            this.usage = new OpUsage(packageName, attributionTag, op, uid,
                    System.currentTimeMillis(), true, null);
            this.flags = flags;
        }

        public boolean isEnd() {
            return (flags & ATTRIBUTION_FLAG_ACCESSOR) != 0;
        }

        public boolean isStart() {
            return (flags & ATTRIBUTION_FLAG_RECEIVER) != 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof AccessChainLink)) {
                return false;
            }
            AccessChainLink other = (AccessChainLink) obj;
            return other.flags == flags && packageAndOpEquals(other.usage.op,
                    other.usage.packageName, other.usage.attributionTag, other.usage.uid);
        }

        public boolean packageAndOpEquals(String op, String packageName, String attributionTag,
                int uid) {
            return Objects.equals(op, usage.op) && Objects.equals(packageName, usage.packageName)
                    && Objects.equals(attributionTag, usage.attributionTag) && uid == usage.uid;
        }
    }
}
