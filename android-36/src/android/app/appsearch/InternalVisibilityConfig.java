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
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import com.android.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An expanded version of {@link SchemaVisibilityConfig} which includes fields for internal use by
 * AppSearch.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "InternalVisibilityConfigCreator")
public final class InternalVisibilityConfig extends AbstractSafeParcelable {

    public static final Parcelable.@NonNull Creator<InternalVisibilityConfig> CREATOR =
            new InternalVisibilityConfigCreator();

    /** Build the List of {@link InternalVisibilityConfig}s from given {@link SetSchemaRequest}. */
    public static @NonNull List<InternalVisibilityConfig> toInternalVisibilityConfigs(
            @NonNull SetSchemaRequest setSchemaRequest) {
        Set<AppSearchSchema> searchSchemas = setSchemaRequest.getSchemas();
        Set<String> schemasNotDisplayedBySystem = setSchemaRequest.getSchemasNotDisplayedBySystem();
        Map<String, Set<PackageIdentifier>> schemasVisibleToPackages =
                setSchemaRequest.getSchemasVisibleToPackages();
        Map<String, Set<Set<Integer>>> schemasVisibleToPermissions =
                setSchemaRequest.getRequiredPermissionsForSchemaTypeVisibility();
        Map<String, PackageIdentifier> publiclyVisibleSchemas =
                setSchemaRequest.getPubliclyVisibleSchemas();
        Map<String, Set<SchemaVisibilityConfig>> schemasVisibleToConfigs =
                setSchemaRequest.getSchemasVisibleToConfigs();

        List<InternalVisibilityConfig> result = new ArrayList<>(searchSchemas.size());
        for (AppSearchSchema searchSchema : searchSchemas) {
            String schemaType = searchSchema.getSchemaType();
            InternalVisibilityConfig.Builder builder =
                    new InternalVisibilityConfig.Builder(schemaType)
                            .setNotDisplayedBySystem(
                                    schemasNotDisplayedBySystem.contains(schemaType));

            Set<PackageIdentifier> visibleToPackages = schemasVisibleToPackages.get(schemaType);
            if (visibleToPackages != null) {
                for (PackageIdentifier packageIdentifier : visibleToPackages) {
                    builder.addVisibleToPackage(packageIdentifier);
                }
            }

            Set<Set<Integer>> visibleToPermissionSets = schemasVisibleToPermissions.get(schemaType);
            if (visibleToPermissionSets != null) {
                for (Set<Integer> visibleToPermissions : visibleToPermissionSets) {
                    builder.addVisibleToPermissions(visibleToPermissions);
                }
            }

            PackageIdentifier publiclyVisibleTargetPackage = publiclyVisibleSchemas.get(schemaType);
            if (publiclyVisibleTargetPackage != null) {
                builder.setPubliclyVisibleTargetPackage(publiclyVisibleTargetPackage);
            }

            Set<SchemaVisibilityConfig> visibleToConfigs = schemasVisibleToConfigs.get(schemaType);
            if (visibleToConfigs != null) {
                for (SchemaVisibilityConfig schemaVisibilityConfig : visibleToConfigs) {
                    builder.addVisibleToConfig(schemaVisibilityConfig);
                }
            }

            result.add(builder.build());
        }
        return result;
    }

    /**
     * Build the List of {@link InternalVisibilityConfig}s from given {@link
     * SetBlobVisibilityRequest}.
     */
    public static @NonNull List<InternalVisibilityConfig> toInternalVisibilityConfigs(
            @NonNull SetBlobVisibilityRequest setBlobVisibilityRequest) {

        Set<String> blobNamespacesNotDisplayedBySystem =
                setBlobVisibilityRequest.getNamespacesNotDisplayedBySystem();
        Map<String, Set<SchemaVisibilityConfig>> blobNamespacesVisibleToConfigs =
                setBlobVisibilityRequest.getNamespacesVisibleToConfigs();

        Set<String> allBlobNamespaces = new ArraySet<>(blobNamespacesNotDisplayedBySystem);
        allBlobNamespaces.addAll(blobNamespacesVisibleToConfigs.keySet());

        List<InternalVisibilityConfig> result = new ArrayList<>();
        for (String namespace : allBlobNamespaces) {
            InternalVisibilityConfig.Builder builder =
                    new InternalVisibilityConfig.Builder(namespace)
                            .setNotDisplayedBySystem(
                                    blobNamespacesNotDisplayedBySystem.contains(namespace));

            Set<SchemaVisibilityConfig> visibleToConfigs =
                    blobNamespacesVisibleToConfigs.get(namespace);
            if (visibleToConfigs != null) {
                for (SchemaVisibilityConfig schemaVisibilityConfig : visibleToConfigs) {
                    builder.addVisibleToConfig(schemaVisibilityConfig);
                }
            }

            result.add(builder.build());
        }
        return result;
    }

    @Field(id = 1, getter = "getSchemaType")
    private final @NonNull String mSchemaType;

    @Field(id = 2, getter = "isNotDisplayedBySystem")
    private final boolean mIsNotDisplayedBySystem;

    /** The public visibility settings available in VisibilityConfig. */
    @Field(id = 3, getter = "getVisibilityConfig")
    private final @NonNull SchemaVisibilityConfig mVisibilityConfig;

    /** Extended visibility settings from {@link SetSchemaRequest#getSchemasVisibleToConfigs()} */
    @Field(id = 4)
    final @NonNull List<SchemaVisibilityConfig> mVisibleToConfigs;

    @Constructor
    InternalVisibilityConfig(
            @Param(id = 1) @NonNull String schemaType,
            @Param(id = 2) boolean isNotDisplayedBySystem,
            @Param(id = 3) @NonNull SchemaVisibilityConfig schemaVisibilityConfig,
            @Param(id = 4) @NonNull List<SchemaVisibilityConfig> visibleToConfigs) {
        mIsNotDisplayedBySystem = isNotDisplayedBySystem;
        mSchemaType = Objects.requireNonNull(schemaType);
        mVisibilityConfig = Objects.requireNonNull(schemaVisibilityConfig);
        mVisibleToConfigs = Objects.requireNonNull(visibleToConfigs);
    }

    /**
     * Gets the schemaType for this VisibilityConfig.
     *
     * <p>This is being used as the document id when we convert a {@link InternalVisibilityConfig}
     * to a {@link GenericDocument}.
     */
    public @NonNull String getSchemaType() {
        return mSchemaType;
    }

    /** Returns whether this schema is visible to the system. */
    public boolean isNotDisplayedBySystem() {
        return mIsNotDisplayedBySystem;
    }

    /**
     * Returns the visibility settings stored in the public {@link SchemaVisibilityConfig} object.
     */
    public @NonNull SchemaVisibilityConfig getVisibilityConfig() {
        return mVisibilityConfig;
    }

    /**
     * Returns required {@link SchemaVisibilityConfig} sets for a caller need to match to access the
     * schema this {@link InternalVisibilityConfig} represents.
     */
    public @NonNull Set<SchemaVisibilityConfig> getVisibleToConfigs() {
        return new ArraySet<>(mVisibleToConfigs);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        InternalVisibilityConfigCreator.writeToParcel(this, dest, flags);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof InternalVisibilityConfig)) {
            return false;
        }
        InternalVisibilityConfig that = (InternalVisibilityConfig) o;
        return mIsNotDisplayedBySystem == that.mIsNotDisplayedBySystem
                && Objects.equals(mSchemaType, that.mSchemaType)
                && Objects.equals(mVisibilityConfig, that.mVisibilityConfig)
                && Objects.equals(mVisibleToConfigs, that.mVisibleToConfigs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mIsNotDisplayedBySystem, mSchemaType, mVisibilityConfig, mVisibleToConfigs);
    }

    /** The builder class of {@link InternalVisibilityConfig}. */
    @FlaggedApi(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS)
    public static final class Builder {
        private String mSchemaType;
        private boolean mIsNotDisplayedBySystem;
        private SchemaVisibilityConfig.Builder mVisibilityConfigBuilder;
        private List<SchemaVisibilityConfig> mVisibleToConfigs = new ArrayList<>();
        private boolean mBuilt;

        /**
         * Creates a {@link Builder} for a {@link InternalVisibilityConfig}.
         *
         * @param schemaType The SchemaType of the {@link AppSearchSchema} that this {@link
         *     InternalVisibilityConfig} represents. The package and database prefix will be added
         *     in server side. We are using prefixed schema type to be the final id of this {@link
         *     InternalVisibilityConfig}. This will be used as as an AppSearch id.
         * @see GenericDocument#getId
         */
        public Builder(@NonNull String schemaType) {
            mSchemaType = Objects.requireNonNull(schemaType);
            mVisibilityConfigBuilder = new SchemaVisibilityConfig.Builder();
        }

        /** Creates a {@link Builder} from an existing {@link InternalVisibilityConfig} */
        public Builder(@NonNull InternalVisibilityConfig internalVisibilityConfig) {
            Objects.requireNonNull(internalVisibilityConfig);
            mSchemaType = internalVisibilityConfig.mSchemaType;
            mIsNotDisplayedBySystem = internalVisibilityConfig.mIsNotDisplayedBySystem;
            mVisibilityConfigBuilder =
                    new SchemaVisibilityConfig.Builder(
                            internalVisibilityConfig.getVisibilityConfig());
            mVisibleToConfigs = internalVisibilityConfig.mVisibleToConfigs;
        }

        /** Sets schemaType, which will be as the id when converting to {@link GenericDocument}. */
        @CanIgnoreReturnValue
        public @NonNull Builder setSchemaType(@NonNull String schemaType) {
            resetIfBuilt();
            mSchemaType = Objects.requireNonNull(schemaType);
            return this;
        }

        /**
         * Resets all values contained in the VisibilityConfig with the values from the given
         * VisibiltiyConfig.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setVisibilityConfig(
                @NonNull SchemaVisibilityConfig schemaVisibilityConfig) {
            resetIfBuilt();
            mVisibilityConfigBuilder = new SchemaVisibilityConfig.Builder(schemaVisibilityConfig);
            return this;
        }

        /** Sets whether this schema has opted out of platform surfacing. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNotDisplayedBySystem(boolean notDisplayedBySystem) {
            resetIfBuilt();
            mIsNotDisplayedBySystem = notDisplayedBySystem;
            return this;
        }

        /**
         * Add {@link PackageIdentifier} of packages which has access to this schema.
         *
         * @see SchemaVisibilityConfig.Builder#addAllowedPackage
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addVisibleToPackage(@NonNull PackageIdentifier packageIdentifier) {
            resetIfBuilt();
            mVisibilityConfigBuilder.addAllowedPackage(packageIdentifier);
            return this;
        }

        /**
         * Clears the list of packages which have access to this schema.
         *
         * @see SchemaVisibilityConfig.Builder#clearAllowedPackages
         */
        @CanIgnoreReturnValue
        public @NonNull Builder clearVisibleToPackages() {
            resetIfBuilt();
            mVisibilityConfigBuilder.clearAllowedPackages();
            return this;
        }

        /**
         * Adds a set of required Android {@link android.Manifest.permission} combination a package
         * needs to hold to access the schema.
         *
         * @see SchemaVisibilityConfig.Builder#addRequiredPermissions
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addVisibleToPermissions(
                @NonNull Set<Integer> visibleToPermissions) {
            resetIfBuilt();
            mVisibilityConfigBuilder.addRequiredPermissions(visibleToPermissions);
            return this;
        }

        /**
         * Clears all required permissions combinations set to this {@link SchemaVisibilityConfig}.
         *
         * @see SchemaVisibilityConfig.Builder#clearRequiredPermissions
         */
        @CanIgnoreReturnValue
        public @NonNull Builder clearVisibleToPermissions() {
            resetIfBuilt();
            mVisibilityConfigBuilder.clearRequiredPermissions();
            return this;
        }

        /**
         * Specify that this schema should be publicly available, to the same packages that have
         * visibility to the package passed as a parameter. This visibility is determined by the
         * result of {@link android.content.pm.PackageManager#canPackageQuery}.
         *
         * @see SchemaVisibilityConfig.Builder#setPubliclyVisibleTargetPackage
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setPubliclyVisibleTargetPackage(
                @Nullable PackageIdentifier packageIdentifier) {
            resetIfBuilt();
            mVisibilityConfigBuilder.setPubliclyVisibleTargetPackage(packageIdentifier);
            return this;
        }

        /**
         * Add the {@link SchemaVisibilityConfig} for a caller need to match to access the schema
         * this {@link InternalVisibilityConfig} represents.
         *
         * <p>You can call this method repeatedly to add multiple {@link SchemaVisibilityConfig},
         * and the querier will have access if they match ANY of the {@link SchemaVisibilityConfig}.
         *
         * @param schemaVisibilityConfig The {@link SchemaVisibilityConfig} hold all requirements
         *     that a call must match to access the schema.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addVisibleToConfig(
                @NonNull SchemaVisibilityConfig schemaVisibilityConfig) {
            Objects.requireNonNull(schemaVisibilityConfig);
            resetIfBuilt();
            mVisibleToConfigs.add(schemaVisibilityConfig);
            return this;
        }

        /** Clears the set of {@link SchemaVisibilityConfig} which have access to this schema. */
        @CanIgnoreReturnValue
        public @NonNull Builder clearVisibleToConfig() {
            resetIfBuilt();
            mVisibleToConfigs.clear();
            return this;
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mVisibleToConfigs = new ArrayList<>(mVisibleToConfigs);
                mBuilt = false;
            }
        }

        /** Build a {@link InternalVisibilityConfig} */
        public @NonNull InternalVisibilityConfig build() {
            mBuilt = true;
            return new InternalVisibilityConfig(
                    mSchemaType,
                    mIsNotDisplayedBySystem,
                    mVisibilityConfigBuilder.build(),
                    mVisibleToConfigs);
        }
    }
}
