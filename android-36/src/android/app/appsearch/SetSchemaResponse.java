/*
 * Copyright 2021 The Android Open Source Project
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
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import com.android.appsearch.flags.Flags;
import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The response class of {@link AppSearchSession#setSchema} */
@SafeParcelable.Class(creator = "SetSchemaResponseCreator")
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class SetSchemaResponse extends AbstractSafeParcelable {

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<SetSchemaResponse> CREATOR =
            new SetSchemaResponseCreator();

    @Field(id = 1)
    final List<String> mDeletedTypes;

    @Field(id = 2)
    final List<String> mIncompatibleTypes;

    @Field(id = 3)
    final List<String> mMigratedTypes;

    /**
     * The migrationFailures won't be saved as a SafeParcelable field. Since:
     *
     * <ul>
     *   <li>{@link MigrationFailure} is generated in {@link AppSearchSession} which will be the SDK
     *       side in platform. We don't need to pass it from service side via binder as a part of
     *       {@link SetSchemaResponse}.
     *   <li>Writing multiple {@link MigrationFailure}s to SafeParcelable in {@link Builder} and
     *       then back in constructor will be a huge waste.
     * </ul>
     */
    private final List<MigrationFailure> mMigrationFailures;

    /**
     * Cache of the inflated deleted schema types. Comes from inflating mDeletedTypes at first use
     */
    private @Nullable Set<String> mDeletedTypesCached;

    /**
     * Cache of the inflated migrated schema types. Comes from inflating mMigratedTypes at first
     * use.
     */
    private @Nullable Set<String> mMigratedTypesCached;

    /**
     * Cache of the inflated incompatible schema types. Comes from inflating mIncompatibleTypes at
     * first use.
     */
    private @Nullable Set<String> mIncompatibleTypesCached;

    @Constructor
    SetSchemaResponse(
            @Param(id = 1) @NonNull List<String> deletedTypes,
            @Param(id = 2) @NonNull List<String> incompatibleTypes,
            @Param(id = 3) @NonNull List<String> migratedTypes) {
        mDeletedTypes = deletedTypes;
        mIncompatibleTypes = incompatibleTypes;
        mMigratedTypes = migratedTypes;
        mMigrationFailures = Collections.emptyList();
    }

    SetSchemaResponse(
            @NonNull List<String> deletedTypes,
            @NonNull List<String> incompatibleTypes,
            @NonNull List<String> migratedTypes,
            @NonNull List<MigrationFailure> migrationFailures) {
        mDeletedTypes = deletedTypes;
        mIncompatibleTypes = incompatibleTypes;
        mMigratedTypes = migratedTypes;
        mMigrationFailures = Objects.requireNonNull(migrationFailures);
    }

    /**
     * Returns a {@link List} of all failed {@link MigrationFailure}.
     *
     * <p>A {@link MigrationFailure} will be generated if the system trying to save a post-migrated
     * {@link GenericDocument} but fail.
     *
     * <p>{@link MigrationFailure} contains the namespace, id and schemaType of the post-migrated
     * {@link GenericDocument} and the error reason. Mostly it will be mismatch the schema it
     * migrated to.
     */
    public @NonNull List<MigrationFailure> getMigrationFailures() {
        return Collections.unmodifiableList(mMigrationFailures);
    }

    /**
     * Returns a {@link Set} of deleted schema types.
     *
     * <p>A "deleted" type is a schema type that was previously a part of the database schema but
     * was not present in the {@link SetSchemaRequest} object provided in the {@link
     * AppSearchSession#setSchema} call.
     *
     * <p>Documents for a deleted type are removed from the database.
     */
    public @NonNull Set<String> getDeletedTypes() {
        if (mDeletedTypesCached == null) {
            mDeletedTypesCached = new ArraySet<>(Objects.requireNonNull(mDeletedTypes));
        }
        return Collections.unmodifiableSet(mDeletedTypesCached);
    }

    /**
     * Returns a {@link Set} of schema type that were migrated by the {@link
     * AppSearchSession#setSchema} call.
     *
     * <p>A "migrated" type is a schema type that has triggered a {@link Migrator} instance to
     * migrate documents of the schema type to another schema type, or to another version of the
     * schema type.
     *
     * <p>If a document fails to be migrated, a {@link MigrationFailure} will be generated for that
     * document.
     *
     * @see Migrator
     */
    public @NonNull Set<String> getMigratedTypes() {
        if (mMigratedTypesCached == null) {
            mMigratedTypesCached = new ArraySet<>(Objects.requireNonNull(mMigratedTypes));
        }
        return Collections.unmodifiableSet(mMigratedTypesCached);
    }

    /**
     * Returns a {@link Set} of schema type whose new definitions set in the {@link
     * AppSearchSession#setSchema} call were incompatible with the pre-existing schema.
     *
     * <p>If a {@link Migrator} is provided for this type and the migration is success triggered.
     * The type will also appear in {@link #getMigratedTypes()}.
     *
     * @see SetSchemaRequest
     * @see AppSearchSession#setSchema
     * @see SetSchemaRequest.Builder#setForceOverride
     */
    public @NonNull Set<String> getIncompatibleTypes() {
        if (mIncompatibleTypesCached == null) {
            mIncompatibleTypesCached = new ArraySet<>(Objects.requireNonNull(mIncompatibleTypes));
        }
        return Collections.unmodifiableSet(mIncompatibleTypesCached);
    }

    /** Builder for {@link SetSchemaResponse} objects. */
    public static final class Builder {
        private List<MigrationFailure> mMigrationFailures = new ArrayList<>();
        private ArrayList<String> mDeletedTypes = new ArrayList<>();
        private ArrayList<String> mMigratedTypes = new ArrayList<>();
        private ArrayList<String> mIncompatibleTypes = new ArrayList<>();
        private boolean mBuilt = false;

        /**
         * Creates a new {@link SetSchemaResponse.Builder} from the given SetSchemaResponse.
         *
         * @hide
         */
        public Builder(@NonNull SetSchemaResponse setSchemaResponse) {
            Objects.requireNonNull(setSchemaResponse);
            mDeletedTypes.addAll(setSchemaResponse.getDeletedTypes());
            mIncompatibleTypes.addAll(setSchemaResponse.getIncompatibleTypes());
            mMigratedTypes.addAll(setSchemaResponse.getMigratedTypes());
            mMigrationFailures.addAll(setSchemaResponse.getMigrationFailures());
        }

        /** Create a {@link Builder} object} */
        public Builder() {}

        /** Adds {@link MigrationFailure}s to the list of migration failures. */
        @CanIgnoreReturnValue
        public @NonNull Builder addMigrationFailures(
                @NonNull Collection<MigrationFailure> migrationFailures) {
            Objects.requireNonNull(migrationFailures);
            resetIfBuilt();
            mMigrationFailures.addAll(migrationFailures);
            return this;
        }

        /** Adds a {@link MigrationFailure} to the list of migration failures. */
        @CanIgnoreReturnValue
        public @NonNull Builder addMigrationFailure(@NonNull MigrationFailure migrationFailure) {
            Objects.requireNonNull(migrationFailure);
            resetIfBuilt();
            mMigrationFailures.add(migrationFailure);
            return this;
        }

        /** Adds {@code deletedTypes} to the list of deleted schema types. */
        @CanIgnoreReturnValue
        public @NonNull Builder addDeletedTypes(@NonNull Collection<String> deletedTypes) {
            Objects.requireNonNull(deletedTypes);
            resetIfBuilt();
            mDeletedTypes.addAll(deletedTypes);
            return this;
        }

        /** Adds one {@code deletedType} to the list of deleted schema types. */
        @CanIgnoreReturnValue
        public @NonNull Builder addDeletedType(@NonNull String deletedType) {
            Objects.requireNonNull(deletedType);
            resetIfBuilt();
            mDeletedTypes.add(deletedType);
            return this;
        }

        /** Adds {@code incompatibleTypes} to the list of incompatible schema types. */
        @CanIgnoreReturnValue
        public @NonNull Builder addIncompatibleTypes(
                @NonNull Collection<String> incompatibleTypes) {
            Objects.requireNonNull(incompatibleTypes);
            resetIfBuilt();
            mIncompatibleTypes.addAll(incompatibleTypes);
            return this;
        }

        /** Adds one {@code incompatibleType} to the list of incompatible schema types. */
        @CanIgnoreReturnValue
        public @NonNull Builder addIncompatibleType(@NonNull String incompatibleType) {
            Objects.requireNonNull(incompatibleType);
            resetIfBuilt();
            mIncompatibleTypes.add(incompatibleType);
            return this;
        }

        /** Adds {@code migratedTypes} to the list of migrated schema types. */
        @CanIgnoreReturnValue
        public @NonNull Builder addMigratedTypes(@NonNull Collection<String> migratedTypes) {
            Objects.requireNonNull(migratedTypes);
            resetIfBuilt();
            mMigratedTypes.addAll(migratedTypes);
            return this;
        }

        /** Adds one {@code migratedType} to the list of migrated schema types. */
        @CanIgnoreReturnValue
        public @NonNull Builder addMigratedType(@NonNull String migratedType) {
            Objects.requireNonNull(migratedType);
            resetIfBuilt();
            mMigratedTypes.add(migratedType);
            return this;
        }

        /** Builds a {@link SetSchemaResponse} object. */
        public @NonNull SetSchemaResponse build() {
            mBuilt = true;
            // Avoid converting the potential thousands of MigrationFailures to Pracelable and
            // back just for put in bundle. In platform, we should set MigrationFailures in
            // AppSearchSession after we pass SetSchemaResponse via binder.
            return new SetSchemaResponse(
                    mDeletedTypes, mIncompatibleTypes, mMigratedTypes, mMigrationFailures);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mMigrationFailures = new ArrayList<>(mMigrationFailures);
                mDeletedTypes = new ArrayList<>(mDeletedTypes);
                mMigratedTypes = new ArrayList<>(mMigratedTypes);
                mIncompatibleTypes = new ArrayList<>(mIncompatibleTypes);
                mBuilt = false;
            }
        }
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SetSchemaResponseCreator.writeToParcel(this, dest, flags);
    }

    /**
     * The class represents a post-migrated {@link GenericDocument} that failed to be saved by
     * {@link AppSearchSession#setSchema}.
     */
    @SafeParcelable.Class(creator = "MigrationFailureCreator")
    @SuppressWarnings("HiddenSuperclass")
    public static class MigrationFailure extends AbstractSafeParcelable {

        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        public static final @NonNull Parcelable.Creator<MigrationFailure> CREATOR =
                new MigrationFailureCreator();

        @Field(id = 1, getter = "getNamespace")
        private final String mNamespace;

        @Field(id = 2, getter = "getDocumentId")
        private final String mDocumentId;

        @Field(id = 3, getter = "getSchemaType")
        private final String mSchemaType;

        @Field(id = 4)
        final @Nullable String mErrorMessage;

        @Field(id = 5)
        final int mResultCode;

        @Constructor
        MigrationFailure(
                @Param(id = 1) @NonNull String namespace,
                @Param(id = 2) @NonNull String documentId,
                @Param(id = 3) @NonNull String schemaType,
                @Param(id = 4) @Nullable String errorMessage,
                @Param(id = 5) int resultCode) {
            mNamespace = namespace;
            mDocumentId = documentId;
            mSchemaType = schemaType;
            mErrorMessage = errorMessage;
            mResultCode = resultCode;
        }

        /**
         * Constructs a new {@link MigrationFailure}.
         *
         * @param namespace The namespace of the document which failed to be migrated.
         * @param documentId The id of the document which failed to be migrated.
         * @param schemaType The type of the document which failed to be migrated.
         * @param failedResult The reason why the document failed to be indexed.
         * @throws IllegalArgumentException if the provided {@code failedResult} was not a failure.
         */
        public MigrationFailure(
                @NonNull String namespace,
                @NonNull String documentId,
                @NonNull String schemaType,
                @NonNull AppSearchResult<?> failedResult) {
            mNamespace = namespace;
            mDocumentId = documentId;
            mSchemaType = schemaType;

            Objects.requireNonNull(failedResult);
            Preconditions.checkArgument(
                    !failedResult.isSuccess(), "failedResult was actually successful");
            mErrorMessage = failedResult.getErrorMessage();
            mResultCode = failedResult.getResultCode();
        }

        /** Returns the namespace of the {@link GenericDocument} that failed to be migrated. */
        public @NonNull String getNamespace() {
            return mNamespace;
        }

        /** Returns the id of the {@link GenericDocument} that failed to be migrated. */
        public @NonNull String getDocumentId() {
            return mDocumentId;
        }

        /** Returns the schema type of the {@link GenericDocument} that failed to be migrated. */
        public @NonNull String getSchemaType() {
            return mSchemaType;
        }

        /**
         * Returns the {@link AppSearchResult} that indicates why the post-migration {@link
         * GenericDocument} failed to be indexed.
         */
        public @NonNull AppSearchResult<Void> getAppSearchResult() {
            return AppSearchResult.newFailedResult(mResultCode, mErrorMessage);
        }

        @Override
        public @NonNull String toString() {
            return "MigrationFailure { schemaType: "
                    + getSchemaType()
                    + ", namespace: "
                    + getNamespace()
                    + ", documentId: "
                    + getDocumentId()
                    + ", appSearchResult: "
                    + getAppSearchResult().toString()
                    + "}";
        }

        @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            MigrationFailureCreator.writeToParcel(this, dest, flags);
        }
    }
}
