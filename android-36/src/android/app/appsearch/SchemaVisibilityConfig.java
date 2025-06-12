/*
 * Copyright 2023 The Android Open Source Project
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

package android.app.appsearch;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.PackageIdentifierParcel;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import com.android.appsearch.flags.Flags;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class to hold a all necessary Visibility information corresponding to the same schema. This
 * pattern allows for easier association of these documents.
 *
 * <p>This does not correspond to any schema, the properties held in this class are kept in two
 * separate schemas, VisibilityConfig and PublicAclOverlay.
 */
@FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
@SafeParcelable.Class(creator = "VisibilityConfigCreator")
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class SchemaVisibilityConfig extends AbstractSafeParcelable {

    public static final @NonNull Parcelable.Creator<SchemaVisibilityConfig> CREATOR =
            new VisibilityConfigCreator();

    @Field(id = 1)
    final @NonNull List<PackageIdentifierParcel> mAllowedPackages;

    @Field(id = 2)
    final @NonNull List<VisibilityPermissionConfig> mRequiredPermissions;

    @Field(id = 3)
    final @Nullable PackageIdentifierParcel mPubliclyVisibleTargetPackage;

    private @Nullable Integer mHashCode;
    private @Nullable List<PackageIdentifier> mAllowedPackagesCached;
    private @Nullable Set<Set<Integer>> mRequiredPermissionsCached;

    @Constructor
    SchemaVisibilityConfig(
            @Param(id = 1) @NonNull List<PackageIdentifierParcel> allowedPackages,
            @Param(id = 2) @NonNull List<VisibilityPermissionConfig> requiredPermissions,
            @Param(id = 3) @Nullable PackageIdentifierParcel publiclyVisibleTargetPackage) {
        mAllowedPackages = Objects.requireNonNull(allowedPackages);
        mRequiredPermissions = Objects.requireNonNull(requiredPermissions);
        mPubliclyVisibleTargetPackage = publiclyVisibleTargetPackage;
    }

    /** Returns a list of {@link PackageIdentifier}s of packages that can access this schema. */
    public @NonNull List<PackageIdentifier> getAllowedPackages() {
        if (mAllowedPackagesCached == null) {
            mAllowedPackagesCached = new ArrayList<>(mAllowedPackages.size());
            for (int i = 0; i < mAllowedPackages.size(); i++) {
                mAllowedPackagesCached.add(new PackageIdentifier(mAllowedPackages.get(i)));
            }
        }
        return mAllowedPackagesCached;
    }

    /**
     * Returns an array of Integers representing Android Permissions that the caller must hold to
     * access the schema this {@link SchemaVisibilityConfig} represents.
     *
     * @see SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility(String, Set)
     */
    public @NonNull Set<Set<Integer>> getRequiredPermissions() {
        if (mRequiredPermissionsCached == null) {
            mRequiredPermissionsCached = new ArraySet<>(mRequiredPermissions.size());
            for (int i = 0; i < mRequiredPermissions.size(); i++) {
                VisibilityPermissionConfig permissionConfig = mRequiredPermissions.get(i);
                Set<Integer> requiredPermissions = permissionConfig.getAllRequiredPermissions();
                if (mRequiredPermissionsCached != null && requiredPermissions != null) {
                    mRequiredPermissionsCached.add(requiredPermissions);
                }
            }
        }
        // Added for nullness checker as it is @Nullable, we initialize it above if it is null.
        return Objects.requireNonNull(mRequiredPermissionsCached);
    }

    /**
     * Returns the {@link PackageIdentifier} of the package that will be used as the target package
     * in a call to {@link android.content.pm.PackageManager#canPackageQuery} to determine which
     * packages can access this publicly visible schema. Returns null if the schema is not publicly
     * visible.
     */
    public @Nullable PackageIdentifier getPubliclyVisibleTargetPackage() {
        if (mPubliclyVisibleTargetPackage == null) {
            return null;
        }
        return new PackageIdentifier(mPubliclyVisibleTargetPackage);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VisibilityConfigCreator.writeToParcel(this, dest, flags);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof SchemaVisibilityConfig)) {
            return false;
        }
        SchemaVisibilityConfig that = (SchemaVisibilityConfig) o;
        return Objects.equals(mAllowedPackages, that.mAllowedPackages)
                && Objects.equals(mRequiredPermissions, that.mRequiredPermissions)
                && Objects.equals(
                        mPubliclyVisibleTargetPackage, that.mPubliclyVisibleTargetPackage);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode =
                    Objects.hash(
                            mAllowedPackages, mRequiredPermissions, mPubliclyVisibleTargetPackage);
        }
        return mHashCode;
    }

    /** The builder class of {@link SchemaVisibilityConfig}. */
    @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
    public static final class Builder {
        private List<PackageIdentifierParcel> mAllowedPackages = new ArrayList<>();
        private List<VisibilityPermissionConfig> mRequiredPermissions = new ArrayList<>();
        private @Nullable PackageIdentifierParcel mPubliclyVisibleTargetPackage;
        private boolean mBuilt;

        /** Creates a {@link Builder} for a {@link SchemaVisibilityConfig}. */
        public Builder() {}

        /**
         * Creates a {@link Builder} copying the values from an existing {@link
         * SchemaVisibilityConfig}.
         *
         * @hide
         */
        public Builder(@NonNull SchemaVisibilityConfig schemaVisibilityConfig) {
            Objects.requireNonNull(schemaVisibilityConfig);
            mAllowedPackages = new ArrayList<>(schemaVisibilityConfig.mAllowedPackages);
            mRequiredPermissions = new ArrayList<>(schemaVisibilityConfig.mRequiredPermissions);
            mPubliclyVisibleTargetPackage = schemaVisibilityConfig.mPubliclyVisibleTargetPackage;
        }

        /** Add {@link PackageIdentifier} of packages which has access to this schema. */
        @CanIgnoreReturnValue
        public @NonNull Builder addAllowedPackage(@NonNull PackageIdentifier packageIdentifier) {
            Objects.requireNonNull(packageIdentifier);
            resetIfBuilt();
            mAllowedPackages.add(packageIdentifier.getPackageIdentifierParcel());
            return this;
        }

        /** Clears the list of packages which have access to this schema. */
        @CanIgnoreReturnValue
        public @NonNull Builder clearAllowedPackages() {
            resetIfBuilt();
            mAllowedPackages.clear();
            return this;
        }

        /**
         * Adds a set of required Android {@link android.Manifest.permission} combination a package
         * needs to hold to access the schema this {@link SchemaVisibilityConfig} represents.
         *
         * <p>If the querier holds ALL of the required permissions in this combination, they will
         * have access to read {@link GenericDocument} objects of the given schema type.
         *
         * <p>You can call this method repeatedly to add multiple permission combinations, and the
         * querier will have access if they holds ANY of the combinations.
         *
         * <p>Merged Set available from {@link #getRequiredPermissions()}.
         *
         * @see SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility for supported
         *     Permissions.
         */
        @SuppressWarnings("RequiresPermission") // No permission required to call this method
        @CanIgnoreReturnValue
        public @NonNull Builder addRequiredPermissions(@NonNull Set<Integer> visibleToPermissions) {
            Objects.requireNonNull(visibleToPermissions);
            resetIfBuilt();
            mRequiredPermissions.add(new VisibilityPermissionConfig(visibleToPermissions));
            return this;
        }

        /**
         * Clears all required permissions combinations set to this {@link SchemaVisibilityConfig}.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder clearRequiredPermissions() {
            resetIfBuilt();
            mRequiredPermissions.clear();
            return this;
        }

        /**
         * Specify that this schema should be publicly available, to the same packages that have
         * visibility to the package passed as a parameter. This visibility is determined by the
         * result of {@link android.content.pm.PackageManager#canPackageQuery}.
         *
         * <p>It is possible for the packageIdentifier parameter to be different from the package
         * performing the indexing. This might happen in the case of an on-device indexer processing
         * information about various packages. The visibility will be the same regardless of which
         * package indexes the document, as the visibility is based on the packageIdentifier
         * parameter.
         *
         * <p>Calling this with packageIdentifier set to null is valid, and will remove public
         * visibility for the schema.
         *
         * @param packageIdentifier the {@link PackageIdentifier} of the package that will be used
         *     as the target package in a call to {@link
         *     android.content.pm.PackageManager#canPackageQuery} to determine which packages can
         *     access this publicly visible schema.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setPubliclyVisibleTargetPackage(
                @Nullable PackageIdentifier packageIdentifier) {
            resetIfBuilt();
            if (packageIdentifier == null) {
                mPubliclyVisibleTargetPackage = null;
            } else {
                mPubliclyVisibleTargetPackage = packageIdentifier.getPackageIdentifierParcel();
            }
            return this;
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mAllowedPackages = new ArrayList<>(mAllowedPackages);
                mRequiredPermissions = new ArrayList<>(mRequiredPermissions);
                mBuilt = false;
            }
        }

        /** Build a {@link SchemaVisibilityConfig} */
        public @NonNull SchemaVisibilityConfig build() {
            mBuilt = true;
            return new SchemaVisibilityConfig(
                    mAllowedPackages, mRequiredPermissions, mPubliclyVisibleTargetPackage);
        }
    }
}
