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

package com.android.server.pm.verify.domain;

import static android.content.IntentFilter.WILDCARD;

import static com.android.server.pm.verify.domain.DomainVerificationUtils.isValidDomain;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import android.Manifest;
import android.annotation.CheckResult;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.UserIdInt;
import android.compat.annotation.ChangeId;
import android.content.Context;
import android.content.Intent;
import android.content.UriRelativeFilterGroup;
import android.content.UriRelativeFilterGroupParcel;
import android.content.pm.Flags;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.verify.domain.DomainOwner;
import android.content.pm.verify.domain.DomainSet;
import android.content.pm.verify.domain.DomainVerificationInfo;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationState;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.content.pm.verify.domain.IDomainVerificationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.pm.pkg.component.ParsedActivity;
import com.android.internal.util.CollectionUtils;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.Computer;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.PackageUserStateUtils;
import com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState;
import com.android.server.pm.verify.domain.models.DomainVerificationPkgState;
import com.android.server.pm.verify.domain.models.DomainVerificationStateMap;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxy;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxyUnavailable;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@SuppressLint("MissingPermission")
public class DomainVerificationService extends SystemService
        implements DomainVerificationManagerInternal, DomainVerificationShell.Callback {

    private static final String TAG = "DomainVerificationService";

    public static final boolean DEBUG_APPROVAL = DomainVerificationDebug.DEBUG_APPROVAL;

    /**
     * The new user preference API for verifying domains marked autoVerify=true in
     * AndroidManifest.xml intent filters is not yet implemented in the current platform preview.
     * This is anticipated to ship before S releases.
     *
     * For now, it is possible to preview the new user preference changes by enabling this
     * ChangeId and using the <code>adb shell pm set-app-links-user-selection</code> and similar
     * commands.
     */
    @ChangeId
    private static final long SETTINGS_API_V2 = 178111421;

    /**
     * States that are currently alive and attached to a package. Entries are exclusive with the
     * state stored in {@link DomainVerificationSettings}, as any pending/restored state should be
     * immediately attached once its available.
     * <p>
     * Generally this should be not accessed directly. Prefer calling {@link
     * #getAndValidateAttachedLocked(UUID, Set, boolean, int, Integer, Computer)}.
     *
     * @see #getAndValidateAttachedLocked(UUID, Set, boolean, int, Integer, Computer)
     **/
    @GuardedBy("mLock")
    @NonNull
    private final DomainVerificationStateMap<DomainVerificationPkgState> mAttachedPkgStates =
            new DomainVerificationStateMap<>();

    /**
     * Lock for all state reads/writes.
     */
    private final Object mLock = new Object();

    @NonNull
    private Connection mConnection;

    @NonNull
    private final SystemConfig mSystemConfig;

    @NonNull
    private final PlatformCompat mPlatformCompat;

    @NonNull
    private final DomainVerificationCollector mCollector;

    @NonNull
    private final DomainVerificationSettings mSettings;

    @NonNull
    private final DomainVerificationEnforcer mEnforcer;

    @NonNull
    private final DomainVerificationDebug mDebug;

    @NonNull
    private final DomainVerificationShell mShell;

    @NonNull
    private final DomainVerificationLegacySettings mLegacySettings;

    @NonNull
    private final IDomainVerificationManager.Stub mStub = new DomainVerificationManagerStub(this);

    @NonNull
    private DomainVerificationProxy mProxy = new DomainVerificationProxyUnavailable();

    private boolean mCanSendBroadcasts;

    public DomainVerificationService(@NonNull Context context, @NonNull SystemConfig systemConfig,
            @NonNull PlatformCompat platformCompat) {
        super(context);
        mSystemConfig = systemConfig;
        mPlatformCompat = platformCompat;
        mCollector = new DomainVerificationCollector(platformCompat, systemConfig);
        mSettings = new DomainVerificationSettings(mCollector);
        mEnforcer = new DomainVerificationEnforcer(context);
        mDebug = new DomainVerificationDebug(mCollector);
        mShell = new DomainVerificationShell(this);
        mLegacySettings = new DomainVerificationLegacySettings();
    }

    @Override
    public void onStart() {
        publishBinderService(Context.DOMAIN_VERIFICATION_SERVICE, mStub);
    }

    @Override
    public void setConnection(@NonNull Connection connection) {
        mConnection = connection;
        mEnforcer.setCallback(mConnection);
    }

    @NonNull
    @Override
    public DomainVerificationProxy getProxy() {
        return mProxy;
    }

    @Override
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (!hasRealVerifier()) {
            return;
        }

        switch (phase) {
            case PHASE_ACTIVITY_MANAGER_READY:
                mCanSendBroadcasts = true;
                break;
            case PHASE_BOOT_COMPLETED:
                verifyPackages(null, false);
                break;
        }
    }

    @Override
    public void onUserUnlocked(@NonNull TargetUser user) {
        super.onUserUnlocked(user);

        // Package verification is sent at both boot and user unlock. The latter will allow v1
        // verification agents to respond to the request, since they will not be directBootAware.
        // However, ideally v2 implementations are boot aware and can handle the initial boot
        // broadcast, to start verifying packages as soon as possible. It's possible this causes
        // unnecessary duplication at device start up, but the implementation is responsible for
        // de-duplicating.
        // TODO: This can be improved by checking if the broadcast was received by the
        //  verification agent in the initial boot broadcast
        verifyPackages(null, false);
    }

    @Override
    public void setProxy(@NonNull DomainVerificationProxy proxy) {
        mProxy = proxy;
    }

    /**
     * Update the URI relative filter groups for a package's verified domains. All previously
     * existing groups will be cleared before the new groups will be applied.
     */
    @RequiresPermission(Manifest.permission.DOMAIN_VERIFICATION_AGENT)
    public void setUriRelativeFilterGroups(@NonNull String packageName,
            @NonNull Bundle bundle)
            throws NameNotFoundException {
        getContext().enforceCallingOrSelfPermission(
                android.Manifest.permission.DOMAIN_VERIFICATION_AGENT,
                "Caller " + mConnection.getCallingUid() + " does not hold "
                        + android.Manifest.permission.DOMAIN_VERIFICATION_AGENT);
        if (bundle.isEmpty()) {
            return;
        }
        synchronized (mLock) {
            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }
            Map<String, List<UriRelativeFilterGroup>> domainToGroupsMap =
                    pkgState.getUriRelativeFilterGroupMap();
            for (String domain : bundle.keySet()) {
                if (!isValidDomain(domain)) {
                    continue;
                }
                ArrayList<UriRelativeFilterGroupParcel> parcels =
                        bundle.getParcelableArrayList(domain, UriRelativeFilterGroupParcel.class);
                List<UriRelativeFilterGroup> groups =
                        UriRelativeFilterGroup.parcelsToGroups(parcels);
                if (groups == null || groups.isEmpty()) {
                    domainToGroupsMap.remove(domain);
                } else {
                    domainToGroupsMap.put(domain, groups);
                }
            }
        }
    }

    /**
     * Retrieve the current URI relative filter groups for a package's verified domain.
     */
    @NonNull
    public Bundle getUriRelativeFilterGroups(@NonNull String packageName,
            @NonNull List<String> domains) {
        Bundle bundle = new Bundle();
        synchronized (mLock) {
            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState != null) {
                Map<String, List<UriRelativeFilterGroup>> map =
                        pkgState.getUriRelativeFilterGroupMap();
                for (int i = 0; i < domains.size(); i++) {
                    if (map.containsKey(domains.get(i))) {
                        List<UriRelativeFilterGroup> groups = map.get(domains.get(i));
                        bundle.putParcelableList(domains.get(i),
                                UriRelativeFilterGroup.groupsToParcels(groups));
                    }
                }
            }
        }
        return bundle;
    }

    @NonNull
    private List<UriRelativeFilterGroup> getUriRelativeFilterGroups(@NonNull String packageName,
            @NonNull String domain) {
        List<UriRelativeFilterGroup> groups;
        synchronized (mLock) {
            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState != null) {
                Map<String, List<UriRelativeFilterGroup>> groupMap =
                        pkgState.getUriRelativeFilterGroupMap();
                groups = groupMap.get(domain);
                if (groups != null) {
                    return groups;
                }
                int first = domain.indexOf(".");
                int second = domain.indexOf('.', first + 1);
                while (first > 0 && second > 0) {
                    groups = groupMap.get(WILDCARD + domain.substring(first));
                    if (groups != null) {
                        return groups;
                    }
                    first = second;
                    second = domain.indexOf('.', second + 1);
                }
            }
        }
        return Collections.emptyList();
    }

    @NonNull
    public List<String> queryValidVerificationPackageNames() {
        mEnforcer.assertApprovedVerifier(mConnection.getCallingUid(), mProxy);
        List<String> packageNames = new ArrayList<>();
        synchronized (mLock) {
            int size = mAttachedPkgStates.size();
            for (int index = 0; index < size; index++) {
                DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(index);
                if (pkgState.isHasAutoVerifyDomains()) {
                    packageNames.add(pkgState.getPackageName());
                }
            }
        }
        return packageNames;
    }

    @Nullable
    @Override
    public UUID getDomainVerificationInfoId(@NonNull String packageName) {
        synchronized (mLock) {
            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState != null) {
                return pkgState.getId();
            } else {
                return null;
            }
        }
    }

    @Nullable
    @Override
    public DomainVerificationInfo getDomainVerificationInfo(@NonNull String packageName)
            throws NameNotFoundException {
        mEnforcer.assertApprovedQuerent(mConnection.getCallingUid(), mProxy);
        final Computer snapshot = mConnection.snapshot();
        synchronized (mLock) {
            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
            if (pkg == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }

            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                return null;
            }

            ArrayMap<String, Integer> hostToStateMap = new ArrayMap<>(pkgState.getStateMap());

            // TODO(b/159952358): Should the domain list be cached?
            ArraySet<String> domains = mCollector.collectValidAutoVerifyDomains(pkg);
            if (domains.isEmpty()) {
                return null;
            }

            int size = domains.size();
            for (int index = 0; index < size; index++) {
                hostToStateMap.putIfAbsent(domains.valueAt(index),
                        DomainVerificationState.STATE_NO_RESPONSE);
            }

            final int mapSize = hostToStateMap.size();
            for (int index = 0; index < mapSize; index++) {
                int internalValue = hostToStateMap.valueAt(index);
                int publicValue = DomainVerificationState.convertToInfoState(internalValue);
                hostToStateMap.setValueAt(index, publicValue);
            }

            // TODO(b/159952358): Do not return if no values are editable (all ignored states)?
            return new DomainVerificationInfo(pkgState.getId(), packageName, hostToStateMap);
        }
    }

    @DomainVerificationManager.Error
    public int setDomainVerificationStatus(@NonNull UUID domainSetId, @NonNull Set<String> domains,
            int state) throws NameNotFoundException {
        if (state < DomainVerificationState.STATE_FIRST_VERIFIER_DEFINED) {
            if (state != DomainVerificationState.STATE_SUCCESS) {
                throw new IllegalArgumentException(
                        "Caller is not allowed to set state code " + state);
            }
        }

        return setDomainVerificationStatusInternal(mConnection.getCallingUid(), domainSetId,
                domains, state);
    }

    @DomainVerificationManager.Error
    @Override
    public int setDomainVerificationStatusInternal(int callingUid, @NonNull UUID domainSetId,
            @NonNull Set<String> domains, int state)
            throws NameNotFoundException {
        mEnforcer.assertApprovedVerifier(callingUid, mProxy);
        final Computer snapshot = mConnection.snapshot();
        synchronized (mLock) {
            List<String> newlyVerifiedDomains = new ArrayList<>();

            GetAttachedResult result = getAndValidateAttachedLocked(domainSetId, domains,
                    true /* forAutoVerify */, callingUid, null /* userId */, snapshot);
            if (result.isError()) {
                return result.getErrorCode();
            }

            DomainVerificationPkgState pkgState = result.getPkgState();
            ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
            for (String domain : domains) {
                Integer previousState = stateMap.get(domain);
                // Skip if the state hasn't changed or can't be changed
                if (previousState != null) {
                    if (previousState == state
                            || !DomainVerificationState.isModifiable(previousState)) {
                        continue;
                    }
                }

                if (DomainVerificationState.isVerified(state)) {
                    if (previousState == null
                            || !DomainVerificationState.isVerified(previousState)) {
                        newlyVerifiedDomains.add(domain);
                    }
                }

                stateMap.put(domain, state);
            }

            // For newly verified domains, revoke their user states across other packages
            int size = newlyVerifiedDomains.size();
            for (int index = 0; index < size; index++) {
                removeUserStatesForDomain(pkgState, newlyVerifiedDomains.get(index));
            }
        }

        mConnection.scheduleWriteSettings();
        return DomainVerificationManager.STATUS_OK;
    }

    @Override
    public void setDomainVerificationStatusInternal(@Nullable String packageName, int state,
            @Nullable final ArraySet<String> domains) throws NameNotFoundException {
        mEnforcer.assertInternal(mConnection.getCallingUid());

        switch (state) {
            case DomainVerificationState.STATE_NO_RESPONSE:
            case DomainVerificationState.STATE_SUCCESS:
            case DomainVerificationState.STATE_APPROVED:
            case DomainVerificationState.STATE_DENIED:
                break;
            default:
                throw new IllegalArgumentException(
                        "State must be one of NO_RESPONSE, SUCCESS, APPROVED, or DENIED");
        }

        if (packageName == null) {
            final Computer snapshot = mConnection.snapshot();
            synchronized (mLock) {
                ArraySet<String> validDomains = new ArraySet<>();

                int size = mAttachedPkgStates.size();
                for (int index = 0; index < size; index++) {
                    DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(index);
                    String pkgName = pkgState.getPackageName();
                    PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
                    if (pkgSetting == null || pkgSetting.getPkg() == null) {
                        continue;
                    }

                    AndroidPackage pkg = pkgSetting.getPkg();

                    validDomains.clear();

                    ArraySet<String> autoVerifyDomains =
                            mCollector.collectValidAutoVerifyDomains(pkg);
                    if (domains == null) {
                        validDomains.addAll(autoVerifyDomains);
                    } else {
                        validDomains.addAll(domains);
                        validDomains.retainAll(autoVerifyDomains);
                    }

                    setDomainVerificationStatusInternal(pkgState, state, validDomains);
                }
            }
        } else {
            final Computer snapshot = mConnection.snapshot();
            synchronized (mLock) {
                DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
                if (pkgState == null) {
                    throw DomainVerificationUtils.throwPackageUnavailable(packageName);
                }

                PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
                if (pkgSetting == null || pkgSetting.getPkg() == null) {
                    throw DomainVerificationUtils.throwPackageUnavailable(packageName);
                }

                AndroidPackage pkg = pkgSetting.getPkg();
                final ArraySet<String> validDomains;
                if (domains == null) {
                    validDomains = mCollector.collectValidAutoVerifyDomains(pkg);
                } else {
                    validDomains = domains;
                    validDomains.retainAll(mCollector.collectValidAutoVerifyDomains(pkg));
                }

                ArraySet<String> newlyVerifiedDomains = null;
                if (DomainVerificationState.isVerified(state)) {
                    newlyVerifiedDomains = new ArraySet<>();
                    ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
                    int domainsSize = validDomains.size();
                    for (int domainIndex = 0; domainIndex < domainsSize; domainIndex++) {
                        String domain = validDomains.valueAt(domainIndex);
                        Integer oldState = stateMap.get(domain);
                        // Only remove if not previously verified
                        if (oldState == null || !DomainVerificationState.isVerified(oldState)) {
                            newlyVerifiedDomains.add(domain);
                        }
                    }
                }

                setDomainVerificationStatusInternal(pkgState, state, validDomains);

                // Mirror SystemApi behavior of revoking user selection for approved domains.
                // This is done in a second pass so that the previous state can be compared before
                // the previous method overwrites it with the new state.
                if (newlyVerifiedDomains != null) {
                    int domainsSize = newlyVerifiedDomains.size();
                    for (int domainIndex = 0; domainIndex < domainsSize; domainIndex++) {
                        String domain = newlyVerifiedDomains.valueAt(domainIndex);
                        removeUserStatesForDomain(pkgState, domain);
                    }
                }
            }
        }

        mConnection.scheduleWriteSettings();
    }

    private void setDomainVerificationStatusInternal(@NonNull DomainVerificationPkgState pkgState,
            int state, @NonNull ArraySet<String> validDomains) {
        ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
        int size = validDomains.size();
        for (int index = 0; index < size; index++) {
            stateMap.put(validDomains.valueAt(index), state);
        }
    }

    private void removeUserStatesForDomain(@NonNull DomainVerificationPkgState owningPkgState,
            @NonNull String domain) {
        SparseArray<DomainVerificationInternalUserState> owningUserStates =
                owningPkgState.getUserStates();
        synchronized (mLock) {
            final int size = mAttachedPkgStates.size();
            for (int index = 0; index < size; index++) {
                DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(index);
                SparseArray<DomainVerificationInternalUserState> array = pkgState.getUserStates();
                int arraySize = array.size();
                for (int arrayIndex = 0; arrayIndex < arraySize; arrayIndex++) {
                    int userId = array.keyAt(arrayIndex);
                    DomainVerificationInternalUserState owningUserState =
                            owningUserStates.get(userId);
                    if (owningUserState != null && !owningUserState.isLinkHandlingAllowed()) {
                        // Skip users where the owning package has their link handling disabled,
                        // since revoking those users would lead to no apps being able to handle
                        // the domain.
                        continue;
                    }
                    array.valueAt(arrayIndex).removeHost(domain);
                }
            }
        }
    }

    public void setDomainVerificationLinkHandlingAllowed(@NonNull String packageName,
            boolean allowed, @UserIdInt int userId) throws NameNotFoundException {
        if (!mEnforcer.assertApprovedUserSelector(mConnection.getCallingUid(),
                mConnection.getCallingUserId(), packageName, userId)) {
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }
        synchronized (mLock) {
            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }

            pkgState.getOrCreateUserState(userId)
                    .setLinkHandlingAllowed(allowed);
        }

        mConnection.scheduleWriteSettings();
    }

    @Override
    public void setDomainVerificationLinkHandlingAllowedInternal(@Nullable String packageName,
            boolean allowed, @UserIdInt int userId) throws NameNotFoundException {
        mEnforcer.assertInternal(mConnection.getCallingUid());
        if (packageName == null) {
            synchronized (mLock) {
                int pkgStateSize = mAttachedPkgStates.size();
                for (int pkgStateIndex = 0; pkgStateIndex < pkgStateSize; pkgStateIndex++) {
                    DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(pkgStateIndex);
                    if (userId == UserHandle.USER_ALL) {
                        for (int aUserId : mConnection.getAllUserIds()) {
                            pkgState.getOrCreateUserState(aUserId)
                                    .setLinkHandlingAllowed(allowed);
                        }
                    } else {
                        pkgState.getOrCreateUserState(userId)
                                .setLinkHandlingAllowed(allowed);
                    }
                }
            }
        } else {
            synchronized (mLock) {
                DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
                if (pkgState == null) {
                    throw DomainVerificationUtils.throwPackageUnavailable(packageName);
                }

                if (userId == UserHandle.USER_ALL) {
                    for (int aUserId : mConnection.getAllUserIds()) {
                        pkgState.getOrCreateUserState(aUserId)
                                .setLinkHandlingAllowed(allowed);
                    }
                } else {
                    pkgState.getOrCreateUserState(userId)
                            .setLinkHandlingAllowed(allowed);
                }
            }
        }

        mConnection.scheduleWriteSettings();
    }

    @CheckResult
    @DomainVerificationManager.Error
    public int setDomainVerificationUserSelection(@NonNull UUID domainSetId,
            @NonNull Set<String> domains, boolean enabled, @UserIdInt int userId)
            throws NameNotFoundException {
        final int callingUid = mConnection.getCallingUid();
        // Pass null for package name here and do the app visibility enforcement inside
        // getAndValidateAttachedLocked instead, since this has to fail with the same invalid
        // ID reason if the target app is invisible
        if (!mEnforcer.assertApprovedUserSelector(callingUid, mConnection.getCallingUserId(),
                null /* packageName */, userId)) {
            return DomainVerificationManager.ERROR_DOMAIN_SET_ID_INVALID;
        }

        final Computer snapshot = mConnection.snapshot();
        synchronized (mLock) {
            GetAttachedResult result = getAndValidateAttachedLocked(domainSetId, domains,
                    false /* forAutoVerify */, callingUid, userId, snapshot);
            if (result.isError()) {
                return result.getErrorCode();
            }

            DomainVerificationPkgState pkgState = result.getPkgState();
            DomainVerificationInternalUserState userState = pkgState.getOrCreateUserState(
                    userId);

            // Disable other packages if approving this one. Note that this check is only done
            // for enabling. This allows an escape hatch in case multiple packages somehow get
            // selected. They can be disabled without blocking in a circular dependency.
            if (enabled) {
                int statusCode = revokeOtherUserSelectionsLocked(userState, userId, domains,
                        snapshot);
                if (statusCode != DomainVerificationManager.STATUS_OK) {
                    return statusCode;
                }
            }

            if (enabled) {
                userState.addHosts(domains);
            } else {
                userState.removeHosts(domains);
            }
        }

        mConnection.scheduleWriteSettings();
        return DomainVerificationManager.STATUS_OK;
    }

    @Override
    public void setDomainVerificationUserSelectionInternal(@UserIdInt int userId,
            @NonNull String packageName, boolean enabled, @Nullable ArraySet<String> domains)
            throws NameNotFoundException {
        mEnforcer.assertInternal(mConnection.getCallingUid());
        final Computer snapshot = mConnection.snapshot();
        synchronized (mLock) {
            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }

            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
            if (pkg == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }

            Set<String> validDomains =
                    domains == null ? mCollector.collectAllWebDomains(pkg) : domains;

            validDomains.retainAll(mCollector.collectAllWebDomains(pkg));

            if (userId == UserHandle.USER_ALL) {
                for (int aUserId : mConnection.getAllUserIds()) {
                    DomainVerificationInternalUserState userState =
                            pkgState.getOrCreateUserState(aUserId);
                    revokeOtherUserSelectionsLocked(userState, aUserId, validDomains,
                            snapshot);
                    if (enabled) {
                        userState.addHosts(validDomains);
                    } else {
                        userState.removeHosts(validDomains);
                    }
                }
            } else {
                DomainVerificationInternalUserState userState =
                        pkgState.getOrCreateUserState(userId);
                revokeOtherUserSelectionsLocked(userState, userId, validDomains, snapshot);
                if (enabled) {
                    userState.addHosts(validDomains);
                } else {
                    userState.removeHosts(validDomains);
                }
            }
        }

        mConnection.scheduleWriteSettings();
    }

    @GuardedBy("mLock")
    private int revokeOtherUserSelectionsLocked(
            @NonNull DomainVerificationInternalUserState userState, @UserIdInt int userId,
            @NonNull Set<String> domains, @NonNull Computer snapshot) {
        // Cache the approved packages from the 1st pass because the search is expensive
        ArrayMap<String, List<String>> domainToApprovedPackages = new ArrayMap<>();

        for (String domain : domains) {
            if (userState.getEnabledHosts().contains(domain)) {
                continue;
            }

            Pair<List<String>, Integer> packagesToLevel = getApprovedPackagesLocked(domain,
                    userId, APPROVAL_LEVEL_NONE + 1, snapshot);
            int highestApproval = packagesToLevel.second;
            if (highestApproval > APPROVAL_LEVEL_SELECTION) {
                return DomainVerificationManager.ERROR_UNABLE_TO_APPROVE;
            }

            domainToApprovedPackages.put(domain, packagesToLevel.first);
        }

        // The removal for other packages must be done in a 2nd pass after it's determined
        // that no higher priority owners exist for all of the domains in the set.
        int mapSize = domainToApprovedPackages.size();
        for (int mapIndex = 0; mapIndex < mapSize; mapIndex++) {
            String domain = domainToApprovedPackages.keyAt(mapIndex);
            List<String> approvedPackages = domainToApprovedPackages.valueAt(mapIndex);
            int approvedSize = approvedPackages.size();
            for (int approvedIndex = 0; approvedIndex < approvedSize; approvedIndex++) {
                String approvedPackage = approvedPackages.get(approvedIndex);
                DomainVerificationPkgState approvedPkgState =
                        mAttachedPkgStates.get(approvedPackage);
                if (approvedPkgState == null) {
                    continue;
                }

                DomainVerificationInternalUserState approvedUserState =
                        approvedPkgState.getUserState(userId);
                if (approvedUserState == null) {
                    continue;
                }

                approvedUserState.removeHost(domain);
            }
        }

        return DomainVerificationManager.STATUS_OK;
    }

    @Nullable
    @Override
    public DomainVerificationUserState getDomainVerificationUserState(
            @NonNull String packageName, @UserIdInt int userId) throws NameNotFoundException {
        if (!mEnforcer.assertApprovedUserStateQuerent(mConnection.getCallingUid(),
                mConnection.getCallingUserId(), packageName, userId)) {
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }

        final Computer snapshot = mConnection.snapshot();
        synchronized (mLock) {
            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
            if (pkg == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }

            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                return null;
            }

            ArraySet<String> webDomains = mCollector.collectAllWebDomains(pkg);
            int webDomainsSize = webDomains.size();

            Map<String, Integer> domains = new ArrayMap<>(webDomainsSize);
            ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
            DomainVerificationInternalUserState userState = pkgState.getUserState(userId);
            Set<String> enabledHosts =
                    userState == null ? emptySet() : userState.getEnabledHosts();

            for (int index = 0; index < webDomainsSize; index++) {
                String host = webDomains.valueAt(index);
                Integer state = stateMap.get(host);

                int domainState;
                if (state != null && DomainVerificationState.isVerified(state)) {
                    domainState = DomainVerificationUserState.DOMAIN_STATE_VERIFIED;
                } else if (enabledHosts.contains(host)) {
                    domainState = DomainVerificationUserState.DOMAIN_STATE_SELECTED;
                } else {
                    domainState = DomainVerificationUserState.DOMAIN_STATE_NONE;
                }

                domains.put(host, domainState);
            }

            boolean linkHandlingAllowed =
                    userState == null || userState.isLinkHandlingAllowed();

            return new DomainVerificationUserState(pkgState.getId(), packageName,
                    UserHandle.of(userId), linkHandlingAllowed, domains);
        }
    }

    @NonNull
    public List<DomainOwner> getOwnersForDomain(@NonNull String domain, @UserIdInt int userId) {
        Objects.requireNonNull(domain);
        mEnforcer.assertOwnerQuerent(mConnection.getCallingUid(), mConnection.getCallingUserId(),
                userId);

        final Computer snapshot = mConnection.snapshot();
        SparseArray<List<String>> levelToPackages = getOwnersForDomainInternal(domain, false,
                userId, snapshot);
        if (levelToPackages.size() == 0) {
            return emptyList();
        }

        List<DomainOwner> owners = new ArrayList<>();
        int size = levelToPackages.size();
        for (int index = 0; index < size; index++) {
            int level = levelToPackages.keyAt(index);
            boolean overrideable = level <= APPROVAL_LEVEL_SELECTION;
            List<String> packages = levelToPackages.valueAt(index);
            int packagesSize = packages.size();
            for (int packageIndex = 0; packageIndex < packagesSize; packageIndex++) {
                owners.add(new DomainOwner(packages.get(packageIndex), overrideable));
            }
        }

        return owners;
    }

    /**
     * @param includeNegative See
     * {@link #approvalLevelForDomain(PackageStateInternal, String, boolean, int, boolean, Object)}.
     * @return Mapping of approval level to packages; packages are sorted by firstInstallTime. Null
     * if no owners were found.
     */
    @NonNull
    private SparseArray<List<String>> getOwnersForDomainInternal(@NonNull String domain,
            boolean includeNegative, @UserIdInt int userId, @NonNull Computer snapshot) {
        SparseArray<List<String>> levelToPackages = new SparseArray<>();
        // First, collect the raw approval level values
        synchronized (mLock) {
            final int size = mAttachedPkgStates.size();
            for (int index = 0; index < size; index++) {
                DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(index);
                String packageName = pkgState.getPackageName();
                PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
                if (pkgSetting == null) {
                    continue;
                }

                int level = approvalLevelForDomain(pkgSetting, domain, includeNegative, userId,
                        DEBUG_APPROVAL, domain);
                if (!includeNegative && level <= APPROVAL_LEVEL_NONE) {
                    continue;
                }
                List<String> list = levelToPackages.get(level);
                if (list == null) {
                    list = new ArrayList<>();
                    levelToPackages.put(level, list);
                }
                list.add(packageName);
            }
        }

        final int size = levelToPackages.size();
        if (size == 0) {
            return levelToPackages;
        }

        // Then sort them ascending by first installed time, with package name as tie breaker
        for (int index = 0; index < size; index++) {
            levelToPackages.valueAt(index).sort((first, second) -> {
                PackageStateInternal firstPkgSetting = snapshot.getPackageStateInternal(first);
                PackageStateInternal secondPkgSetting = snapshot.getPackageStateInternal(second);

                long firstInstallTime =
                        firstPkgSetting == null ? -1L : firstPkgSetting.getUserStateOrDefault(
                                userId).getFirstInstallTimeMillis();
                long secondInstallTime =
                        secondPkgSetting == null ? -1L : secondPkgSetting.getUserStateOrDefault(
                                userId).getFirstInstallTimeMillis();

                if (firstInstallTime != secondInstallTime) {
                    return (int) (firstInstallTime - secondInstallTime);
                }

                return first.compareToIgnoreCase(second);
            });
        }

        return levelToPackages;
    }

    @NonNull
    @Override
    public UUID generateNewId() {
        // TODO(b/159952358): Domain set ID collisions
        return UUID.randomUUID();
    }

    @Override
    public void migrateState(@NonNull PackageStateInternal oldPkgSetting,
            @NonNull PackageStateInternal newPkgSetting, @Nullable DomainSet preVerifiedDomains) {
        String pkgName = newPkgSetting.getPackageName();
        boolean sendBroadcast;

        synchronized (mLock) {
            UUID oldDomainSetId = oldPkgSetting.getDomainSetId();
            UUID newDomainSetId = newPkgSetting.getDomainSetId();
            DomainVerificationPkgState oldPkgState = mAttachedPkgStates.remove(oldDomainSetId);

            AndroidPackage oldPkg = oldPkgSetting.getPkg();
            AndroidPackage newPkg = newPkgSetting.getPkg();

            ArrayMap<String, Integer> newStateMap = new ArrayMap<>();
            SparseArray<DomainVerificationInternalUserState> newUserStates = new SparseArray<>();

            if (oldPkgState == null || oldPkg == null || newPkg == null) {
                // Should be impossible, but to be safe, continue with a new blank state instead
                Slog.wtf(TAG, "Invalid state nullability old state = " + oldPkgState
                        + ", old pkgSetting = " + oldPkgSetting
                        + ", new pkgSetting = " + newPkgSetting
                        + ", old pkg = " + oldPkg
                        + ", new pkg = " + newPkg, new Exception());

                DomainVerificationPkgState newPkgState = new DomainVerificationPkgState(
                        pkgName, newDomainSetId, true, newStateMap, newUserStates,
                        null /* signature */);
                mAttachedPkgStates.put(pkgName, newDomainSetId, newPkgState);
                return;
            }

            ArrayMap<String, Integer> oldStateMap = oldPkgState.getStateMap();
            ArrayMap<String, List<UriRelativeFilterGroup>> oldGroups =
                    oldPkgState.getUriRelativeFilterGroupMap();
            ArraySet<String> newAutoVerifyDomains =
                    mCollector.collectValidAutoVerifyDomains(newPkg);
            int newDomainsSize = newAutoVerifyDomains.size();

            for (int newDomainsIndex = 0; newDomainsIndex < newDomainsSize; newDomainsIndex++) {
                String domain = newAutoVerifyDomains.valueAt(newDomainsIndex);
                Integer oldStateInteger = oldStateMap.get(domain);
                if (oldStateInteger != null) {
                    int oldState = oldStateInteger;
                    // If the following case fails, the state code is left unset
                    // (STATE_NO_RESPONSE) to signal to the verification agent that any existing
                    // error has been cleared and the domain should be re-attempted. This makes
                    // update of a package a signal to re-verify.
                    if (DomainVerificationState.shouldMigrate(oldState)) {
                        newStateMap.put(domain, oldState);
                    }
                }
            }

            SparseArray<DomainVerificationInternalUserState> oldUserStates =
                    oldPkgState.getUserStates();
            int oldUserStatesSize = oldUserStates.size();
            if (oldUserStatesSize > 0) {
                ArraySet<String> newWebDomains = mCollector.collectAllWebDomains(newPkg);
                for (int oldUserStatesIndex = 0; oldUserStatesIndex < oldUserStatesSize;
                        oldUserStatesIndex++) {
                    int userId = oldUserStates.keyAt(oldUserStatesIndex);
                    DomainVerificationInternalUserState oldUserState = oldUserStates.valueAt(
                            oldUserStatesIndex);
                    ArraySet<String> oldEnabledHosts = oldUserState.getEnabledHosts();
                    ArraySet<String> newEnabledHosts = new ArraySet<>(oldEnabledHosts);
                    newEnabledHosts.retainAll(newWebDomains);
                    DomainVerificationInternalUserState newUserState =
                            new DomainVerificationInternalUserState(userId, newEnabledHosts,
                                    oldUserState.isLinkHandlingAllowed());
                    newUserStates.put(userId, newUserState);
                }
            }

            boolean hasAutoVerifyDomains = newDomainsSize > 0;
            boolean needsBroadcast =
                    applyImmutableState(newPkgSetting, newStateMap, newAutoVerifyDomains);

            sendBroadcast = hasAutoVerifyDomains && needsBroadcast;

            // Apply pre-verified states as the last step of migration
            applyPreVerifiedState(newStateMap, newAutoVerifyDomains, preVerifiedDomains);

            mAttachedPkgStates.put(pkgName, newDomainSetId, new DomainVerificationPkgState(
                    pkgName, newDomainSetId, hasAutoVerifyDomains, newStateMap, newUserStates,
                    null /* signature */, oldGroups));
        }

        if (sendBroadcast) {
            sendBroadcast(pkgName);
        }
    }

    // TODO(b/159952358): Handle valid domainSetIds for PackageStateInternals with no AndroidPackage
    @Override
    public void addPackage(@NonNull PackageStateInternal newPkgSetting,
                           @Nullable DomainSet preVerifiedDomains) {
        // TODO(b/159952358): Optimize packages without any domains. Those wouldn't have to be in
        //  the state map, but it would require handling the "migration" case where an app either
        //  gains or loses all domains.

        UUID domainSetId = newPkgSetting.getDomainSetId();
        String pkgName = newPkgSetting.getPackageName();

        boolean sendBroadcast = true;

        DomainVerificationPkgState pkgState;
        pkgState = mSettings.removePendingState(pkgName);
        if (pkgState != null) {
            // Don't send when attaching from pending read, which is usually boot scan. Re-send on
            // boot is handled in a separate method once all packages are added.
            sendBroadcast = false;
        } else {
            pkgState = mSettings.removeRestoredState(pkgName);
            if (pkgState != null && !Objects.equals(pkgState.getBackupSignatureHash(),
                    PackageUtils.computeSignaturesSha256Digest(
                            newPkgSetting.getSigningDetails().getSignatures()))) {
                // If restoring and the signatures don't match, drop the state
                pkgState = null;
            }
        }

        AndroidPackage pkg = newPkgSetting.getPkg();
        ArraySet<String> autoVerifyDomains = mCollector.collectValidAutoVerifyDomains(pkg);
        boolean hasAutoVerifyDomains = !autoVerifyDomains.isEmpty();
        boolean isPendingOrRestored = pkgState != null;
        if (isPendingOrRestored) {
            pkgState = new DomainVerificationPkgState(pkgState, domainSetId, hasAutoVerifyDomains);
            pkgState.getStateMap().retainAll(autoVerifyDomains);

            Set<String> webDomains = mCollector.collectAllWebDomains(pkg);
            SparseArray<DomainVerificationInternalUserState> userStates = pkgState.getUserStates();
            int size = userStates.size();
            for (int index = 0; index < size; index++) {
                userStates.valueAt(index).retainHosts(webDomains);
            }
        } else {
            pkgState = new DomainVerificationPkgState(pkgName, domainSetId, hasAutoVerifyDomains);
        }

        boolean needsBroadcast = applyImmutableState(newPkgSetting, pkgState.getStateMap(),
                autoVerifyDomains);
        if (needsBroadcast && !isPendingOrRestored) {
            // TODO(b/159952358): Test this behavior
            // Attempt to preserve user experience by automatically verifying all domains from
            // legacy state if they were previously approved, or by automatically enabling all
            // hosts through user selection if legacy state indicates a user previously made the
            // choice in settings to allow supported links. The domain verification agent should
            // re-verify these links (set to STATE_MIGRATED) at the next possible opportunity,
            // and disable them if appropriate.
            ArraySet<String> webDomains = null;

            SparseIntArray legacyUserStates = mLegacySettings.getUserStates(pkgName);
            int userStateSize = legacyUserStates == null ? 0 : legacyUserStates.size();
            for (int index = 0; index < userStateSize; index++) {
                int userId = legacyUserStates.keyAt(index);
                int legacyStatus = legacyUserStates.valueAt(index);
                if (legacyStatus
                        == PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS) {
                    if (webDomains == null) {
                        webDomains = mCollector.collectAllWebDomains(pkg);
                    }

                    pkgState.getOrCreateUserState(userId).addHosts(webDomains);
                }
            }

            IntentFilterVerificationInfo legacyInfo = mLegacySettings.remove(pkgName);
            if (legacyInfo != null
                    && legacyInfo.getStatus()
                    == PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS) {
                ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
                int domainsSize = autoVerifyDomains.size();
                for (int index = 0; index < domainsSize; index++) {
                    stateMap.put(autoVerifyDomains.valueAt(index),
                            DomainVerificationState.STATE_MIGRATED);
                }
            }

            // Apply pre-verified states before sending out broadcast
            applyPreVerifiedState(pkgState.getStateMap(), autoVerifyDomains, preVerifiedDomains);
        }

        synchronized (mLock) {
            mAttachedPkgStates.put(pkgName, domainSetId, pkgState);
        }

        if (sendBroadcast && hasAutoVerifyDomains) {
            sendBroadcast(pkgName);
        }
    }

    private void applyPreVerifiedState(ArrayMap<String, Integer> stateMap,
                                       ArraySet<String> autoVerifyDomains,
                                       DomainSet preVerifiedDomains) {
        // If any pre-verified domains are provided, treating them as verified as well. This
        // allows the app to be opened immediately by the corresponding app links, but the
        // pre-verified state can still be overwritten by the domain verification agent in the
        // future.
        if (preVerifiedDomains != null && !autoVerifyDomains.isEmpty()) {
            for (String preVerifiedDomain : preVerifiedDomains.getDomains()) {
                if (autoVerifyDomains.contains(preVerifiedDomain)
                        && !stateMap.containsKey(preVerifiedDomain)) {
                    // Only set the pre-verified state if there's no existing state
                    stateMap.put(preVerifiedDomain, DomainVerificationState.STATE_PRE_VERIFIED);
                    if (DEBUG_APPROVAL) {
                        Slog.d(TAG, "Inserted pre-verified domain: " + preVerifiedDomain);
                    }
                }
            }
        }
    }

    /**
     * Applies any immutable state as the final step when adding or migrating state. Currently only
     * applies {@link SystemConfig#getLinkedApps()}, which approves all domains for a system app.
     *
     * @return whether or not a broadcast is necessary for this package
     */
    private boolean applyImmutableState(@NonNull PackageStateInternal pkgSetting,
            @NonNull ArrayMap<String, Integer> stateMap,
            @NonNull ArraySet<String> autoVerifyDomains) {
        if (pkgSetting.isSystem()
                && mSystemConfig.getLinkedApps().contains(pkgSetting.getPackageName())) {
            int domainsSize = autoVerifyDomains.size();
            for (int index = 0; index < domainsSize; index++) {
                stateMap.put(autoVerifyDomains.valueAt(index),
                        DomainVerificationState.STATE_SYS_CONFIG);
            }
            return false;
        } else {
            int size = stateMap.size();
            for (int index = size - 1; index >= 0; index--) {
                Integer state = stateMap.valueAt(index);
                // If no longer marked in SysConfig, demote any previous SysConfig state
                if (state == DomainVerificationState.STATE_SYS_CONFIG) {
                    stateMap.removeAt(index);
                }
            }

            return true;
        }
    }

    @Override
    public void writeSettings(Computer snapshot, @NonNull TypedXmlSerializer serializer,
            boolean includeSignatures, @UserIdInt int userId) throws IOException {
        synchronized (mLock) {
            Function<String, String> pkgNameToSignature = null;
            if (includeSignatures) {
                pkgNameToSignature = pkgName -> {
                    PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
                    if (pkgSetting == null) {
                        // If querying for a user restored package that isn't installed on the
                        // device yet, there will be no signature to write out. In that case,
                        // it's expected that this returns null and it falls back to the
                        // restored state's stored signature if it exists.
                        return null;
                    }

                    return PackageUtils.computeSignaturesSha256Digest(
                            pkgSetting.getSigningDetails().getSignatures());
                };
            }

            mSettings.writeSettings(serializer, mAttachedPkgStates, userId, pkgNameToSignature);
        }

        mLegacySettings.writeSettings(serializer);
    }

    @Override
    public void readSettings(@NonNull Computer snapshot, @NonNull TypedXmlPullParser parser)
            throws IOException, XmlPullParserException {
        synchronized (mLock) {
            mSettings.readSettings(parser, mAttachedPkgStates, snapshot);
        }
    }

    @Override
    public void readLegacySettings(@NonNull TypedXmlPullParser parser)
            throws IOException, XmlPullParserException {
        mLegacySettings.readSettings(parser);
    }

    @Override
    public void restoreSettings(Computer snapshot, @NonNull TypedXmlPullParser parser)
            throws IOException, XmlPullParserException {
        synchronized (mLock) {
            mSettings.restoreSettings(parser, mAttachedPkgStates, snapshot);
        }
    }

    @Override
    public void addLegacySetting(@NonNull String packageName,
            @NonNull IntentFilterVerificationInfo info) {
        mLegacySettings.add(packageName, info);
    }

    @Override
    public boolean setLegacyUserState(@NonNull String packageName, @UserIdInt int userId,
            int state) {
        if (!mEnforcer.callerIsLegacyUserSelector(mConnection.getCallingUid(),
                mConnection.getCallingUserId(), packageName, userId)) {
            return false;
        }
        mLegacySettings.add(packageName, userId, state);
        mConnection.scheduleWriteSettings();
        return true;
    }

    @Override
    public int getLegacyState(@NonNull String packageName, @UserIdInt int userId) {
        if (!mEnforcer.callerIsLegacyUserQuerent(mConnection.getCallingUid(),
                mConnection.getCallingUserId(), packageName, userId)) {
            return PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED;
        }
        return mLegacySettings.getUserState(packageName, userId);
    }

    @Override
    public void clearPackage(@NonNull String packageName) {
        synchronized (mLock) {
            mAttachedPkgStates.remove(packageName);
            mSettings.removePackage(packageName);
        }

        mConnection.scheduleWriteSettings();
    }

    @Override
    public void clearPackageForUser(@NonNull String packageName, @UserIdInt int userId) {
        synchronized (mLock) {
            final DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState != null) {
                pkgState.removeUser(userId);
            }

            mSettings.removePackageForUser(packageName, userId);
        }

        mConnection.scheduleWriteSettings();
    }

    @Override
    public void clearUser(@UserIdInt int userId) {
        synchronized (mLock) {
            int attachedSize = mAttachedPkgStates.size();
            for (int index = 0; index < attachedSize; index++) {
                mAttachedPkgStates.valueAt(index).removeUser(userId);
            }

            mSettings.removeUser(userId);
        }

        mConnection.scheduleWriteSettings();
    }

    @Override
    public boolean runMessage(int messageCode, Object object) {
        return mProxy.runMessage(messageCode, object);
    }

    @Override
    public void printState(@NonNull IndentingPrintWriter writer, @Nullable String packageName,
            @Nullable Integer userId) throws NameNotFoundException {
        printState(mConnection.snapshot(), writer, packageName, userId);
    }

    @Override
    public void printState(@NonNull Computer snapshot, @NonNull IndentingPrintWriter writer,
            @Nullable String packageName, @Nullable @UserIdInt Integer userId)
            throws NameNotFoundException {
        mEnforcer.assertApprovedQuerent(mConnection.getCallingUid(), mProxy);
        synchronized (mLock) {
            mDebug.printState(writer, packageName, userId, snapshot, mAttachedPkgStates);
        }
    }

    @Override
    public void printOwnersForPackage(@NonNull IndentingPrintWriter writer,
            @Nullable String packageName, @Nullable @UserIdInt Integer userId)
            throws NameNotFoundException {
        mEnforcer.assertApprovedQuerent(mConnection.getCallingUid(), mProxy);
        final Computer snapshot = mConnection.snapshot();
        synchronized (mLock) {
            if (packageName == null) {
                int size = mAttachedPkgStates.size();
                for (int index = 0; index < size; index++) {
                    try {
                        printOwnersForPackage(writer,
                                mAttachedPkgStates.valueAt(index).getPackageName(), userId,
                                snapshot);
                    } catch (NameNotFoundException ignored) {
                        // When iterating packages, if one doesn't exist somehow, ignore
                    }
                }
            } else {
                printOwnersForPackage(writer, packageName, userId, snapshot);
            }
        }
    }

    private void printOwnersForPackage(@NonNull IndentingPrintWriter writer,
            @NonNull String packageName, @Nullable @UserIdInt Integer userId,
            @NonNull Computer snapshot)
            throws NameNotFoundException {
        PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
        AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
        if (pkg == null) {
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }

        ArraySet<String> domains = mCollector.collectAllWebDomains(pkg);
        int size = domains.size();
        if (size == 0) {
            return;
        }

        writer.println(packageName + ":");
        writer.increaseIndent();

        for (int index = 0; index < size; index++) {
            printOwnersForDomain(writer, domains.valueAt(index), userId, snapshot);
        }

        writer.decreaseIndent();
    }

    @Override
    public void printOwnersForDomains(@NonNull IndentingPrintWriter writer,
            @NonNull List<String> domains, @Nullable @UserIdInt Integer userId) {
        mEnforcer.assertApprovedQuerent(mConnection.getCallingUid(), mProxy);
        final Computer snapshot = mConnection.snapshot();
        synchronized (mLock) {
            int size = domains.size();
            for (int index = 0; index < size; index++) {
                printOwnersForDomain(writer, domains.get(index), userId, snapshot);
            }
        }
    }

    private void printOwnersForDomain(@NonNull IndentingPrintWriter writer, @NonNull String domain,
            @Nullable @UserIdInt Integer userId, @NonNull Computer snapshot) {
        SparseArray<SparseArray<List<String>>> userIdToApprovalLevelToOwners =
                new SparseArray<>();

        if (userId == null || userId == UserHandle.USER_ALL) {
            for (int aUserId : mConnection.getAllUserIds()) {
                userIdToApprovalLevelToOwners.put(aUserId,
                        getOwnersForDomainInternal(domain, true, aUserId, snapshot));
            }
        } else {
            userIdToApprovalLevelToOwners.put(userId,
                    getOwnersForDomainInternal(domain, true, userId, snapshot));
        }

        mDebug.printOwners(writer, domain, userIdToApprovalLevelToOwners);
    }

    @NonNull
    @Override
    public DomainVerificationShell getShell() {
        return mShell;
    }

    @NonNull
    @Override
    public DomainVerificationCollector getCollector() {
        return mCollector;
    }

    private void sendBroadcast(@NonNull String packageName) {
        sendBroadcast(Collections.singleton(packageName));
    }

    private void sendBroadcast(@NonNull Set<String> packageNames) {
        if (!mCanSendBroadcasts) {
            // If the system cannot send broadcasts, it's probably still in boot, so dropping this
            // request should be fine. The verification agent should re-scan packages once boot
            // completes.
            return;
        }

        mProxy.sendBroadcastForPackages(packageNames);
    }

    private boolean hasRealVerifier() {
        return !(mProxy instanceof DomainVerificationProxyUnavailable);
    }

    /**
     * Validates parameters provided by an external caller. Checks that an ID is still live and that
     * any provided domains are valid. Should be called at the beginning of each API that takes in a
     * {@link UUID} domain set ID.
     *
     * @param userIdForFilter which user to filter app access to, or null if the caller has already
     *                        validated package visibility
     */
    @CheckResult
    @GuardedBy("mLock")
    private GetAttachedResult getAndValidateAttachedLocked(@NonNull UUID domainSetId,
            @NonNull Set<String> domains, boolean forAutoVerify, int callingUid,
            @Nullable Integer userIdForFilter, @NonNull Computer snapshot)
            throws NameNotFoundException {
        if (domainSetId == null) {
            throw new IllegalArgumentException("domainSetId cannot be null");
        }

        DomainVerificationPkgState pkgState = mAttachedPkgStates.get(domainSetId);
        if (pkgState == null) {
            return GetAttachedResult.error(DomainVerificationManager.ERROR_DOMAIN_SET_ID_INVALID);
        }

        String pkgName = pkgState.getPackageName();

        if (userIdForFilter != null
                && mConnection.filterAppAccess(pkgName, callingUid, userIdForFilter)) {
            return GetAttachedResult.error(DomainVerificationManager.ERROR_DOMAIN_SET_ID_INVALID);
        }

        PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
        if (pkgSetting == null || pkgSetting.getPkg() == null) {
            throw DomainVerificationUtils.throwPackageUnavailable(pkgName);
        }

        if (CollectionUtils.isEmpty(domains)) {
            throw new IllegalArgumentException("Provided domain set cannot be empty");
        }

        AndroidPackage pkg = pkgSetting.getPkg();
        ArraySet<String> declaredDomains = forAutoVerify
                ? mCollector.collectValidAutoVerifyDomains(pkg)
                : mCollector.collectAllWebDomains(pkg);

        if (domains.retainAll(declaredDomains)) {
            return GetAttachedResult.error(DomainVerificationManager.ERROR_UNKNOWN_DOMAIN);
        }

        return GetAttachedResult.success(pkgState);
    }

    @Override
    public void verifyPackages(@Nullable List<String> packageNames, boolean reVerify) {
        mEnforcer.assertInternal(mConnection.getCallingUid());
        Set<String> packagesToBroadcast = new ArraySet<>();

        if (packageNames == null) {
            synchronized (mLock) {
                int pkgStatesSize = mAttachedPkgStates.size();
                for (int pkgStateIndex = 0; pkgStateIndex < pkgStatesSize; pkgStateIndex++) {
                    DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(pkgStateIndex);
                    addIfShouldBroadcastLocked(packagesToBroadcast, pkgState, reVerify);
                }
            }
        } else {
            synchronized (mLock) {
                int size = packageNames.size();
                for (int index = 0; index < size; index++) {
                    String packageName = packageNames.get(index);
                    DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
                    if (pkgState != null) {
                        addIfShouldBroadcastLocked(packagesToBroadcast, pkgState, reVerify);
                    }
                }
            }
        }

        if (!packagesToBroadcast.isEmpty()) {
            sendBroadcast(packagesToBroadcast);
        }
    }

    @GuardedBy("mLock")
    private void addIfShouldBroadcastLocked(@NonNull Collection<String> packageNames,
            @NonNull DomainVerificationPkgState pkgState, boolean reVerify) {
        if ((reVerify && pkgState.isHasAutoVerifyDomains()) || shouldReBroadcastPackage(pkgState)) {
            packageNames.add(pkgState.getPackageName());
        }
    }

    /**
     * Determine whether or not a broadcast should be sent at boot for the given {@param pkgState}.
     * Sends only if the only states recorded are default as decided by {@link
     * DomainVerificationState#isDefault(int)}.
     *
     * If any other state is set, it's assumed that the domain verification agent is aware of the
     * package and has already scheduled future verification requests.
     */
    private boolean shouldReBroadcastPackage(DomainVerificationPkgState pkgState) {
        if (!pkgState.isHasAutoVerifyDomains()) {
            return false;
        }

        ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
        int statesSize = stateMap.size();
        for (int stateIndex = 0; stateIndex < statesSize; stateIndex++) {
            Integer state = stateMap.valueAt(stateIndex);
            if (!DomainVerificationState.isDefault(state)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void clearDomainVerificationState(@Nullable List<String> packageNames) {
        mEnforcer.assertInternal(mConnection.getCallingUid());
        final Computer snapshot = mConnection.snapshot();
        synchronized (mLock) {
            if (packageNames == null) {
                int size = mAttachedPkgStates.size();
                for (int index = 0; index < size; index++) {
                    DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(index);
                    String pkgName = pkgState.getPackageName();
                    PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
                    if (pkgSetting == null || pkgSetting.getPkg() == null) {
                        continue;
                    }
                    resetDomainState(pkgState.getStateMap(), pkgSetting);
                }
            } else {
                int size = packageNames.size();
                for (int index = 0; index < size; index++) {
                    String pkgName = packageNames.get(index);
                    DomainVerificationPkgState pkgState = mAttachedPkgStates.get(pkgName);
                    PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
                    if (pkgSetting == null || pkgSetting.getPkg() == null) {
                        continue;
                    }
                    resetDomainState(pkgState.getStateMap(), pkgSetting);
                }
            }
        }

        mConnection.scheduleWriteSettings();
    }

    /**
     * Reset states that are mutable by the domain verification agent.
     */
    private void resetDomainState(@NonNull ArrayMap<String, Integer> stateMap,
            @NonNull PackageStateInternal pkgSetting) {
        int size = stateMap.size();
        for (int index = size - 1; index >= 0; index--) {
            Integer state = stateMap.valueAt(index);
            boolean reset;
            switch (state) {
                case DomainVerificationState.STATE_SUCCESS:
                case DomainVerificationState.STATE_RESTORED:
                    reset = true;
                    break;
                default:
                    reset = state >= DomainVerificationState.STATE_FIRST_VERIFIER_DEFINED;
                    break;
            }

            if (reset) {
                stateMap.removeAt(index);
            }
        }

        applyImmutableState(pkgSetting, stateMap,
                mCollector.collectValidAutoVerifyDomains(pkgSetting.getPkg()));
    }

    @Override
    public void clearUserStates(@Nullable List<String> packageNames, @UserIdInt int userId) {
        mEnforcer.assertInternal(mConnection.getCallingUid());
        synchronized (mLock) {
            if (packageNames == null) {
                int size = mAttachedPkgStates.size();
                for (int index = 0; index < size; index++) {
                    DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(index);
                    if (userId == UserHandle.USER_ALL) {
                        pkgState.removeAllUsers();
                    } else {
                        pkgState.removeUser(userId);
                    }
                }
            } else {
                int size = packageNames.size();
                for (int index = 0; index < size; index++) {
                    String pkgName = packageNames.get(index);
                    DomainVerificationPkgState pkgState = mAttachedPkgStates.get(pkgName);
                    if (userId == UserHandle.USER_ALL) {
                        pkgState.removeAllUsers();
                    } else {
                        pkgState.removeUser(userId);
                    }
                }
            }
        }

        mConnection.scheduleWriteSettings();
    }

    /**
     * {@inheritDoc}
     *
     * Resolving an Intent to an approved app happens in stages:
     * <ol>
     *     <li>Find all non-zero approved packages for the {@link Intent}'s domain</li>
     *     <li>Filter to packages with the highest approval level, see {@link ApprovalLevel}</li>
     *     <li>Filter out {@link ResolveInfo}s that don't match that approved packages</li>
     *     <li>Take the approved packages with the latest install time</li>
     *     <li>Take the ResolveInfo representing the Activity declared last in the manifest</li>
     *     <li>Return remaining results if any exist</li>
     * </ol>
     */
    @NonNull
    @Override
    public Pair<List<ResolveInfo>, Integer> filterToApprovedApp(@NonNull Intent intent,
            @NonNull List<ResolveInfo> infos, @UserIdInt int userId,
            @NonNull Function<String, PackageStateInternal> pkgSettingFunction) {
        // Collect valid infos
        ArrayMap<ResolveInfo, Integer> infoApprovals = new ArrayMap<>();
        int infosSize = infos.size();
        for (int index = 0; index < infosSize; index++) {
            final ResolveInfo info = infos.get(index);
            // Only collect for intent filters that can auto resolve
            if (info.isAutoResolutionAllowed()) {
                infoApprovals.put(info, null);
            }
        }

        // Find all approval levels
        int highestApproval = fillMapWithApprovalLevels(infoApprovals, intent.getData(), userId,
                pkgSettingFunction);
        if (highestApproval <= APPROVAL_LEVEL_NONE) {
            return Pair.create(emptyList(), highestApproval);
        }

        // Filter to highest, non-zero infos
        for (int index = infoApprovals.size() - 1; index >= 0; index--) {
            if (infoApprovals.valueAt(index) != highestApproval) {
                infoApprovals.removeAt(index);
            }
        }

        if (highestApproval != APPROVAL_LEVEL_LEGACY_ASK) {
            // To maintain legacy behavior while the Settings API is not implemented,
            // show the chooser if all approved apps are marked ask, skipping the
            // last app, last declaration filtering.
            filterToLastFirstInstalled(infoApprovals, pkgSettingFunction);
        }

        // Easier to transform into list as the filterToLastDeclared method
        // requires swapping indexes, which doesn't work with ArrayMap keys
        final int size = infoApprovals.size();
        List<ResolveInfo> finalList = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            finalList.add(infoApprovals.keyAt(index));
        }

        // If legacy ask, skip the last declaration filtering
        if (highestApproval != APPROVAL_LEVEL_LEGACY_ASK) {
            // Find the last declared ResolveInfo per package
            filterToLastDeclared(finalList, pkgSettingFunction);
        }

        return Pair.create(finalList, highestApproval);
    }

    private boolean matchUriRelativeFilterGroups(Uri uri, String pkgName) {
        if (uri.getHost() == null) {
            return false;
        }
        List<UriRelativeFilterGroup> groups = getUriRelativeFilterGroups(pkgName, uri.getHost());
        if (groups.isEmpty()) {
            return true;
        }
        return UriRelativeFilterGroup.matchGroupsToUri(groups, uri);
    }

    /**
     * @return highest approval level found
     */
    @ApprovalLevel
    private int fillMapWithApprovalLevels(@NonNull ArrayMap<ResolveInfo, Integer> inputMap,
            @NonNull Uri uri, @UserIdInt int userId,
            @NonNull Function<String, PackageStateInternal> pkgSettingFunction) {
        int highestApproval = APPROVAL_LEVEL_NONE;
        int size = inputMap.size();
        for (int index = 0; index < size; index++) {
            if (inputMap.valueAt(index) != null) {
                // Already filled by previous iteration
                continue;
            }

            ResolveInfo info = inputMap.keyAt(index);
            final String packageName = info.getComponentInfo().packageName;
            PackageStateInternal pkgSetting = pkgSettingFunction.apply(packageName);
            if (pkgSetting == null || (Flags.relativeReferenceIntentFilters()
                    && !matchUriRelativeFilterGroups(uri, packageName))) {
                fillInfoMapForSamePackage(inputMap, packageName, APPROVAL_LEVEL_NONE);
                continue;
            }
            int approval = approvalLevelForDomain(pkgSetting, uri.getHost(), false, userId,
                    DEBUG_APPROVAL, uri.getHost());
            highestApproval = Math.max(highestApproval, approval);
            fillInfoMapForSamePackage(inputMap, packageName, approval);
        }

        return highestApproval;
    }

    private void fillInfoMapForSamePackage(@NonNull ArrayMap<ResolveInfo, Integer> inputMap,
            @NonNull String targetPackageName, @ApprovalLevel int level) {
        final int size = inputMap.size();
        for (int index = 0; index < size; index++) {
            final String packageName = inputMap.keyAt(index).getComponentInfo().packageName;
            if (Objects.equals(targetPackageName, packageName)) {
                inputMap.setValueAt(index, level);
            }
        }
    }

    @NonNull
    private void filterToLastFirstInstalled(@NonNull ArrayMap<ResolveInfo, Integer> inputMap,
            @NonNull Function<String, PackageStateInternal> pkgSettingFunction) {
        // First, find the package with the latest first install time
        String targetPackageName = null;
        long latestInstall = Long.MIN_VALUE;
        final int size = inputMap.size();
        for (int index = 0; index < size; index++) {
            ResolveInfo info = inputMap.keyAt(index);
            String packageName = info.getComponentInfo().packageName;
            PackageStateInternal pkgSetting = pkgSettingFunction.apply(packageName);
            if (pkgSetting == null) {
                continue;
            }

            long installTime = PackageStateUtils.getEarliestFirstInstallTime(
                    pkgSetting.getUserStates());
            if (installTime > latestInstall) {
                latestInstall = installTime;
                targetPackageName = packageName;
            }
        }

        // Then, remove all infos that don't match the package
        for (int index = inputMap.size() - 1; index >= 0; index--) {
            ResolveInfo info = inputMap.keyAt(index);
            if (!Objects.equals(targetPackageName, info.getComponentInfo().packageName)) {
                inputMap.removeAt(index);
            }
        }
    }

    @NonNull
    private void filterToLastDeclared(@NonNull List<ResolveInfo> inputList,
            @NonNull Function<String, PackageStateInternal> pkgSettingFunction) {
        // Must call size each time as the size of the list will decrease
        for (int index = 0; index < inputList.size(); index++) {
            ResolveInfo info = inputList.get(index);
            String targetPackageName = info.getComponentInfo().packageName;
            PackageStateInternal pkgSetting = pkgSettingFunction.apply(targetPackageName);
            AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
            if (pkg == null) {
                continue;
            }

            ResolveInfo result = info;
            int highestIndex = indexOfIntentFilterEntry(pkg, result);

            // Search backwards so that lower results can be removed as they're found
            for (int searchIndex = inputList.size() - 1; searchIndex >= index + 1; searchIndex--) {
                ResolveInfo searchInfo = inputList.get(searchIndex);
                if (!Objects.equals(targetPackageName, searchInfo.getComponentInfo().packageName)) {
                    continue;
                }

                int entryIndex = indexOfIntentFilterEntry(pkg, searchInfo);
                if (entryIndex > highestIndex) {
                    highestIndex = entryIndex;
                    result = searchInfo;
                }

                // Always remove the entry so that the current index
                // is left as the sole candidate of the target package
                inputList.remove(searchIndex);
            }

            // Swap the current index for the result, leaving this as
            // the only entry with the target package name
            inputList.set(index, result);
        }
    }

    private int indexOfIntentFilterEntry(@NonNull AndroidPackage pkg,
            @NonNull ResolveInfo target) {
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        for (int activityIndex = 0; activityIndex < activitiesSize; activityIndex++) {
            if (Objects.equals(activities.get(activityIndex).getComponentName(),
                    target.getComponentInfo().getComponentName())) {
                return activityIndex;
            }
        }

        return -1;
    }

    @Override
    public int approvalLevelForDomain(@NonNull PackageStateInternal pkgSetting,
            @NonNull Intent intent, @PackageManager.ResolveInfoFlagsBits long resolveInfoFlags,
            @UserIdInt int userId) {
        String packageName = pkgSetting.getPackageName();
        var debug = DEBUG_APPROVAL || (intent.getFlags() & Intent.FLAG_DEBUG_LOG_RESOLUTION) != 0;
        if (!DomainVerificationUtils.isDomainVerificationIntent(intent, resolveInfoFlags)) {
            if (debug) {
                debugApproval(packageName, intent, userId, false, "not valid intent");
            }
            return APPROVAL_LEVEL_NONE;
        }

        var approvalLevel = approvalLevelForDomain(pkgSetting, intent.getData().getHost(), false,
                userId, debug, intent);
        if (debug) {
            Slog.d(TAG + "Approval", "Final approval level for " + pkgSetting.getPackageName()
                    + " for host " + intent.getData().getHost() + " is " + approvalLevel);
        }
        return approvalLevel;
    }

    /**
     * @param includeNegative Whether to include negative values, which requires an expensive
     *                          domain comparison operation.
     * @param debugObject       Should be an {@link Intent} if checking for resolution or a
     *                          {@link String} otherwise.
     */
    private int approvalLevelForDomain(@NonNull PackageStateInternal pkgSetting,
            @NonNull String host, boolean includeNegative, @UserIdInt int userId, boolean debug,
            @NonNull Object debugObject) {
        int approvalLevel = approvalLevelForDomainInternal(pkgSetting, host, includeNegative,
                userId, debug, debugObject);
        if (includeNegative && approvalLevel == APPROVAL_LEVEL_NONE) {
            PackageUserStateInternal pkgUserState = pkgSetting.getUserStateOrDefault(userId);
            if (!pkgUserState.isInstalled()) {
                return APPROVAL_LEVEL_NOT_INSTALLED;
            }

            AndroidPackage pkg = pkgSetting.getPkg();
            if (pkg != null) {
                if (!PackageUserStateUtils.isPackageEnabled(pkgUserState, pkg)) {
                    return APPROVAL_LEVEL_DISABLED;
                } else if (mCollector.containsAutoVerifyDomain(pkgSetting.getPkg(), host)) {
                    return APPROVAL_LEVEL_UNVERIFIED;
                }
            }
        }

        return approvalLevel;
    }

    private int approvalLevelForDomainInternal(@NonNull PackageStateInternal pkgSetting,
            @NonNull String host, boolean includeNegative, @UserIdInt int userId, boolean debug,
            @NonNull Object debugObject) {
        String packageName = pkgSetting.getPackageName();
        final AndroidPackage pkg = pkgSetting.getPkg();

        if (pkg != null && includeNegative && !mCollector.containsWebDomain(pkg, host)) {
            if (debug) {
                debugApproval(packageName, debugObject, userId, false,
                        "domain not declared");
            }
            return APPROVAL_LEVEL_UNDECLARED;
        }

        final PackageUserStateInternal pkgUserState = pkgSetting.getUserStates().get(userId);
        if (pkgUserState == null) {
            if (debug) {
                debugApproval(packageName, debugObject, userId, false,
                        "PackageUserState unavailable");
            }
            return APPROVAL_LEVEL_NONE;
        }

        if (!pkgUserState.isInstalled()) {
            if (debug) {
                debugApproval(packageName, debugObject, userId, false,
                        "package not installed for user");
            }
            return APPROVAL_LEVEL_NONE;
        }

        if (!PackageUserStateUtils.isPackageEnabled(pkgUserState, pkg)) {
            if (debug) {
                debugApproval(packageName, debugObject, userId, false,
                        "package not enabled for user");
            }
            return APPROVAL_LEVEL_NONE;
        }

        if (pkgUserState.isSuspended()) {
            if (debug) {
                debugApproval(packageName, debugObject, userId, false,
                        "package suspended for user");
            }
            return APPROVAL_LEVEL_NONE;
        }

        // Should never be null, but if it is, skip this and assume that v2 is enabled
        if (pkg != null && !DomainVerificationUtils.isChangeEnabled(mPlatformCompat, pkg,
                SETTINGS_API_V2)) {
            switch (mLegacySettings.getUserState(packageName, userId)) {
                case PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED:
                    // If nothing specifically set, assume v2 rules
                    break;
                case PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER:
                    return APPROVAL_LEVEL_NONE;
                case PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ASK:
                case PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK:
                    return APPROVAL_LEVEL_LEGACY_ASK;
                case PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS:
                    return APPROVAL_LEVEL_LEGACY_ALWAYS;
            }
        }

        synchronized (mLock) {
            DomainVerificationPkgState pkgState = mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                if (debug) {
                    debugApproval(packageName, debugObject, userId, false, "pkgState unavailable");
                }
                return APPROVAL_LEVEL_NONE;
            }

            DomainVerificationInternalUserState userState = pkgState.getUserState(userId);

            if (userState != null && !userState.isLinkHandlingAllowed()) {
                if (debug) {
                    debugApproval(packageName, debugObject, userId, false,
                            "link handling not allowed");
                }
                return APPROVAL_LEVEL_NONE;
            }

            // The instant app branch must be run after the link handling check,
            // since that should also disable instant apps if toggled
            if (pkg != null) {
                // To allow an instant app to immediately open domains after being installed by the
                // user, auto approve them for any declared autoVerify domains.
                if (pkgSetting.getUserStateOrDefault(userId).isInstantApp()
                        && mCollector.collectValidAutoVerifyDomains(pkg).contains(host)) {
                    return APPROVAL_LEVEL_INSTANT_APP;
                }
            }

            ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
            // Check if the exact host matches
            Integer state = stateMap.get(host);
            if (state != null && DomainVerificationState.isVerified(state)) {
                if (debug) {
                    debugApproval(packageName, debugObject, userId, true,
                            "host verified exactly");
                }
                return APPROVAL_LEVEL_VERIFIED;
            }

            // Otherwise see if the host matches a verified domain by wildcard
            int stateMapSize = stateMap.size();
            for (int index = 0; index < stateMapSize; index++) {
                if (!DomainVerificationState.isVerified(stateMap.valueAt(index))) {
                    continue;
                }

                String domain = stateMap.keyAt(index);
                if (domain.startsWith("*.") && host.endsWith(domain.substring(2))) {
                    if (debug) {
                        debugApproval(packageName, debugObject, userId, true,
                                "host verified by wildcard");
                    }
                    return APPROVAL_LEVEL_VERIFIED;
                }
            }

            // Check user state if available
            if (userState == null) {
                if (debug) {
                    debugApproval(packageName, debugObject, userId, false, "userState unavailable");
                }
                return APPROVAL_LEVEL_NONE;
            }

            // See if the user has approved the exact host
            ArraySet<String> enabledHosts = userState.getEnabledHosts();
            if (enabledHosts.contains(host)) {
                if (debug) {
                    debugApproval(packageName, debugObject, userId, true,
                            "host enabled by user exactly");
                }
                return APPROVAL_LEVEL_SELECTION;
            }

            // See if the host matches a user selection by wildcard
            int enabledHostsSize = enabledHosts.size();
            for (int index = 0; index < enabledHostsSize; index++) {
                String domain = enabledHosts.valueAt(index);
                if (domain.startsWith("*.") && host.endsWith(domain.substring(2))) {
                    if (debug) {
                        debugApproval(packageName, debugObject, userId, true,
                                "host enabled by user through wildcard");
                    }
                    return APPROVAL_LEVEL_SELECTION;
                }
            }

            if (debug) {
                debugApproval(packageName, debugObject, userId, false, "not approved");
            }
            return APPROVAL_LEVEL_NONE;
        }
    }

    /**
     * @return the filtered list paired with the corresponding approval level
     */
    @GuardedBy("mLock")
    @NonNull
    private Pair<List<String>, Integer> getApprovedPackagesLocked(@NonNull String domain,
            @UserIdInt int userId, int minimumApproval, @NonNull Computer snapshot) {
        boolean includeNegative = minimumApproval < APPROVAL_LEVEL_NONE;
        int highestApproval = minimumApproval;
        List<String> approvedPackages = emptyList();

        final int size = mAttachedPkgStates.size();
        for (int index = 0; index < size; index++) {
            DomainVerificationPkgState pkgState = mAttachedPkgStates.valueAt(index);
            String packageName = pkgState.getPackageName();
            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            if (pkgSetting == null) {
                continue;
            }

            int level = approvalLevelForDomain(pkgSetting, domain, includeNegative, userId,
                    DEBUG_APPROVAL, domain);
            if (level < minimumApproval) {
                continue;
            }

            if (level > highestApproval) {
                approvedPackages.clear();
                approvedPackages = CollectionUtils.add(approvedPackages, packageName);
                highestApproval = level;
            } else if (level == highestApproval) {
                approvedPackages = CollectionUtils.add(approvedPackages, packageName);
            }
        }

        if (approvedPackages.isEmpty()) {
            return Pair.create(approvedPackages, APPROVAL_LEVEL_NONE);
        }

        List<String> filteredPackages = new ArrayList<>();
        long latestInstall = Long.MIN_VALUE;
        final int approvedSize = approvedPackages.size();
        for (int index = 0; index < approvedSize; index++) {
            String packageName = approvedPackages.get(index);
            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            if (pkgSetting == null) {
                continue;
            }
            long installTime = pkgSetting.getUserStateOrDefault(userId).getFirstInstallTimeMillis();
            if (installTime > latestInstall) {
                latestInstall = installTime;
                filteredPackages.clear();
                filteredPackages.add(packageName);
            } else if (installTime == latestInstall) {
                filteredPackages.add(packageName);
            }
        }

        return Pair.create(filteredPackages, highestApproval);
    }

    private void debugApproval(@NonNull String packageName, @NonNull Object debugObject,
            @UserIdInt int userId, boolean approved, @NonNull String reason) {
        String approvalString = approved ? "approved" : "denied";
        Slog.d(TAG + "Approval", packageName + " was " + approvalString + " for "
                + debugObject + " for user " + userId + ": " + reason);
    }

    private static class GetAttachedResult {

        @Nullable
        private final DomainVerificationPkgState mPkgState;

        private final int mErrorCode;

        GetAttachedResult(@Nullable DomainVerificationPkgState pkgState, int errorCode) {
            mPkgState = pkgState;
            mErrorCode = errorCode;
        }

        @NonNull
        static GetAttachedResult error(@DomainVerificationManager.Error int errorCode) {
            return new GetAttachedResult(null, errorCode);
        }

        @NonNull
        static GetAttachedResult success(@NonNull DomainVerificationPkgState pkgState) {
            return new GetAttachedResult(pkgState, DomainVerificationManager.STATUS_OK);
        }

        @NonNull
        DomainVerificationPkgState getPkgState() {
            return mPkgState;
        }

        boolean isError() {
            return mErrorCode != DomainVerificationManager.STATUS_OK;
        }

        public int getErrorCode() {
            return mErrorCode;
        }
    }
}
