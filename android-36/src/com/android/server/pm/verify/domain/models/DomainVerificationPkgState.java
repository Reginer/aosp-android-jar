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

package com.android.server.pm.verify.domain.models;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.content.UriRelativeFilterGroup;
import android.content.pm.Signature;
import android.content.pm.verify.domain.DomainVerificationState;
import android.util.ArrayMap;
import android.util.SparseArray;

import com.android.internal.util.DataClass;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * State for a single package for the domain verification APIs. Stores the state of each individual
 * domain declared by the package, including its verification state and user selection state.
 */
@DataClass(genToString = true, genEqualsHashCode = true)
public class DomainVerificationPkgState {

    @NonNull
    private final String mPackageName;

    @NonNull
    private final UUID mId;

    /**
     * Whether or not the package declares any autoVerify domains. This is separate from an empty
     * check on the map itself, because an empty map means no response recorded, not necessarily
     * no domains declared. When this is false, {@link #mStateMap} will be empty, but
     * {@link #mUserStates} may contain any domains the user has explicitly chosen to
     * allow this package to open, which may or may not be marked autoVerify.
     */
    private final boolean mHasAutoVerifyDomains;

    /**
     * Map of domains to state integers. Only domains that are not set to the default value of
     * {@link DomainVerificationState#STATE_NO_RESPONSE} are included.
     *
     * TODO(b/159952358): Hide the state map entirely from the caller, to allow optimizations,
     *  such as storing no state when the package is marked as a linked app in SystemConfig.
     */
    @NonNull
    private final ArrayMap<String, Integer> mStateMap;

    @NonNull
    private final SparseArray<DomainVerificationInternalUserState> mUserStates;

    /**
     * If previously recorded, the SHA-256 signing cert digest of the package to attach to.
     * When doing restoration of a previously backed up state, if the signature does not
     * match the package being scanned/installed on device, it will be rejected.
     *
     * It's assumed the domain verification agent will eventually re-verify this domain
     * and revoke if necessary.
     *
     * @see android.util.PackageUtils#computeSignaturesSha256Digest(Signature[])
     */
    @Nullable
    private final String mBackupSignatureHash;

    /**
     * List of {@link UriRelativeFilterGroup} for filtering domains.
     */
    @NonNull
    private final ArrayMap<String, List<UriRelativeFilterGroup>> mUriRelativeFilterGroupMap;

    public DomainVerificationPkgState(@NonNull String packageName, @NonNull UUID id,
            boolean hasAutoVerifyDomains) {
        this(packageName, id, hasAutoVerifyDomains, new ArrayMap<>(0), new SparseArray<>(0), null,
                new ArrayMap<>());
    }

    public DomainVerificationPkgState(@NonNull DomainVerificationPkgState pkgState,
            @NonNull UUID id, boolean hasAutoVerifyDomains) {
        this(pkgState.getPackageName(), id, hasAutoVerifyDomains, pkgState.getStateMap(),
                pkgState.getUserStates(), null, new ArrayMap<>());
    }

    public DomainVerificationPkgState(@NonNull String packageName, @NonNull UUID id,
            boolean hasAutoVerifyDomains, @NonNull ArrayMap<String, Integer> stateMap,
            @NonNull SparseArray<DomainVerificationInternalUserState> userStates,
            @Nullable String backupSignatureHash) {
        this(packageName, id, hasAutoVerifyDomains, stateMap, userStates, backupSignatureHash,
                new ArrayMap<>());
    }

    @Nullable
    public DomainVerificationInternalUserState getUserState(@UserIdInt int userId) {
        return mUserStates.get(userId);
    }

    @Nullable
    public DomainVerificationInternalUserState getOrCreateUserState(
            @UserIdInt int userId) {
        DomainVerificationInternalUserState userState = mUserStates.get(userId);
        if (userState == null) {
            userState = new DomainVerificationInternalUserState(userId);
            mUserStates.put(userId, userState);
        }
        return userState;
    }

    public void removeUser(@UserIdInt int userId) {
        mUserStates.remove(userId);
    }

    public void removeAllUsers() {
        mUserStates.clear();
    }

    private int userStatesHashCode() {
        return mUserStates.contentHashCode();
    }

    private boolean userStatesEquals(
            @NonNull SparseArray<DomainVerificationInternalUserState> other) {
        return mUserStates.contentEquals(other);
    }



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/services/core/java/com/android/server/pm/verify/domain/models/DomainVerificationPkgState.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    /**
     * Creates a new DomainVerificationPkgState.
     *
     * @param hasAutoVerifyDomains
     *   Whether or not the package declares any autoVerify domains. This is separate from an empty
     *   check on the map itself, because an empty map means no response recorded, not necessarily
     *   no domains declared. When this is false, {@link #mStateMap} will be empty, but
     *   {@link #mUserStates} may contain any domains the user has explicitly chosen to
     *   allow this package to open, which may or may not be marked autoVerify.
     * @param stateMap
     *   Map of domains to state integers. Only domains that are not set to the default value of
     *   {@link DomainVerificationState#STATE_NO_RESPONSE} are included.
     *
     *   TODO(b/159952358): Hide the state map entirely from the caller, to allow optimizations,
     *    such as storing no state when the package is marked as a linked app in SystemConfig.
     * @param backupSignatureHash
     *   If previously recorded, the SHA-256 signing cert digest of the package to attach to.
     *   When doing restoration of a previously backed up state, if the signature does not
     *   match the package being scanned/installed on device, it will be rejected.
     *
     *   It's assumed the domain verification agent will eventually re-verify this domain
     *   and revoke if necessary.
     * @param uriRelativeFilterGroupMap
     *   List of {@link UriRelativeFilterGroup} for filtering domains.
     */
    @DataClass.Generated.Member
    public DomainVerificationPkgState(
            @NonNull String packageName,
            @NonNull UUID id,
            boolean hasAutoVerifyDomains,
            @NonNull ArrayMap<String,Integer> stateMap,
            @NonNull SparseArray<DomainVerificationInternalUserState> userStates,
            @Nullable String backupSignatureHash,
            @NonNull ArrayMap<String,List<UriRelativeFilterGroup>> uriRelativeFilterGroupMap) {
        this.mPackageName = packageName;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mPackageName);
        this.mId = id;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mId);
        this.mHasAutoVerifyDomains = hasAutoVerifyDomains;
        this.mStateMap = stateMap;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mStateMap);
        this.mUserStates = userStates;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mUserStates);
        this.mBackupSignatureHash = backupSignatureHash;
        this.mUriRelativeFilterGroupMap = uriRelativeFilterGroupMap;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mUriRelativeFilterGroupMap);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    @DataClass.Generated.Member
    public @NonNull UUID getId() {
        return mId;
    }

    /**
     * Whether or not the package declares any autoVerify domains. This is separate from an empty
     * check on the map itself, because an empty map means no response recorded, not necessarily
     * no domains declared. When this is false, {@link #mStateMap} will be empty, but
     * {@link #mUserStates} may contain any domains the user has explicitly chosen to
     * allow this package to open, which may or may not be marked autoVerify.
     */
    @DataClass.Generated.Member
    public boolean isHasAutoVerifyDomains() {
        return mHasAutoVerifyDomains;
    }

    /**
     * Map of domains to state integers. Only domains that are not set to the default value of
     * {@link DomainVerificationState#STATE_NO_RESPONSE} are included.
     *
     * TODO(b/159952358): Hide the state map entirely from the caller, to allow optimizations,
     *  such as storing no state when the package is marked as a linked app in SystemConfig.
     */
    @DataClass.Generated.Member
    public @NonNull ArrayMap<String,Integer> getStateMap() {
        return mStateMap;
    }

    @DataClass.Generated.Member
    public @NonNull SparseArray<DomainVerificationInternalUserState> getUserStates() {
        return mUserStates;
    }

    /**
     * If previously recorded, the SHA-256 signing cert digest of the package to attach to.
     * When doing restoration of a previously backed up state, if the signature does not
     * match the package being scanned/installed on device, it will be rejected.
     *
     * It's assumed the domain verification agent will eventually re-verify this domain
     * and revoke if necessary.
     *
     * @see android.util.PackageUtils#computeSignaturesSha256Digest(Signature[])
     */
    @DataClass.Generated.Member
    public @Nullable String getBackupSignatureHash() {
        return mBackupSignatureHash;
    }

    /**
     * List of {@link UriRelativeFilterGroup} for filtering domains.
     */
    @DataClass.Generated.Member
    public @NonNull ArrayMap<String,List<UriRelativeFilterGroup>> getUriRelativeFilterGroupMap() {
        return mUriRelativeFilterGroupMap;
    }

    @Override
    @DataClass.Generated.Member
    public String toString() {
        // You can override field toString logic by defining methods like:
        // String fieldNameToString() { ... }

        return "DomainVerificationPkgState { " +
                "packageName = " + mPackageName + ", " +
                "id = " + mId + ", " +
                "hasAutoVerifyDomains = " + mHasAutoVerifyDomains + ", " +
                "stateMap = " + mStateMap + ", " +
                "userStates = " + mUserStates + ", " +
                "backupSignatureHash = " + mBackupSignatureHash + ", " +
                "uriRelativeFilterGroupMap = " + mUriRelativeFilterGroupMap +
        " }";
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(DomainVerificationPkgState other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        DomainVerificationPkgState that = (DomainVerificationPkgState) o;
        //noinspection PointlessBooleanExpression
        return true
                && Objects.equals(mPackageName, that.mPackageName)
                && Objects.equals(mId, that.mId)
                && mHasAutoVerifyDomains == that.mHasAutoVerifyDomains
                && Objects.equals(mStateMap, that.mStateMap)
                && userStatesEquals(that.mUserStates)
                && Objects.equals(mBackupSignatureHash, that.mBackupSignatureHash)
                && Objects.equals(mUriRelativeFilterGroupMap, that.mUriRelativeFilterGroupMap);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + Objects.hashCode(mPackageName);
        _hash = 31 * _hash + Objects.hashCode(mId);
        _hash = 31 * _hash + Boolean.hashCode(mHasAutoVerifyDomains);
        _hash = 31 * _hash + Objects.hashCode(mStateMap);
        _hash = 31 * _hash + userStatesHashCode();
        _hash = 31 * _hash + Objects.hashCode(mBackupSignatureHash);
        _hash = 31 * _hash + Objects.hashCode(mUriRelativeFilterGroupMap);
        return _hash;
    }

    @DataClass.Generated(
            time = 1707351734724L,
            codegenVersion = "1.0.23",
            sourceFile = "frameworks/base/services/core/java/com/android/server/pm/verify/domain/models/DomainVerificationPkgState.java",
            inputSignatures = "private final @android.annotation.NonNull java.lang.String mPackageName\nprivate final @android.annotation.NonNull java.util.UUID mId\nprivate final  boolean mHasAutoVerifyDomains\nprivate final @android.annotation.NonNull android.util.ArrayMap<java.lang.String,java.lang.Integer> mStateMap\nprivate final @android.annotation.NonNull android.util.SparseArray<com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState> mUserStates\nprivate final @android.annotation.Nullable java.lang.String mBackupSignatureHash\nprivate final @android.annotation.NonNull android.util.ArrayMap<java.lang.String,java.util.List<android.content.UriRelativeFilterGroup>> mUriRelativeFilterGroupMap\npublic @android.annotation.Nullable com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState getUserState(int)\npublic @android.annotation.Nullable com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState getOrCreateUserState(int)\npublic  void removeUser(int)\npublic  void removeAllUsers()\nprivate  int userStatesHashCode()\nprivate  boolean userStatesEquals(android.util.SparseArray<com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState>)\nclass DomainVerificationPkgState extends java.lang.Object implements []\n@com.android.internal.util.DataClass(genToString=true, genEqualsHashCode=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
