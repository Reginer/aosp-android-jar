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

import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * The config class that holds all required permissions for a caller need to hold to access the
 * schema which the outer {@link SchemaVisibilityConfig} represents.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "VisibilityPermissionConfigCreator")
public final class VisibilityPermissionConfig extends AbstractSafeParcelable {
    public static final Parcelable.@NonNull Creator<VisibilityPermissionConfig> CREATOR =
            new VisibilityPermissionConfigCreator();

    /**
     * The Schema type for documents that hold AppSearch's metadata, such as visibility settings.
     */
    public static final String SCHEMA_TYPE = "VisibilityPermissionType";

    /** Property that holds the required permissions to access the schema. */
    public static final String ALL_REQUIRED_PERMISSIONS_PROPERTY = "allRequiredPermissions";

    /**
     * Schema for the VisibilityStore's documents.
     *
     * <p>NOTE: If you update this, also update schema version number in
     * VisibilityToDocumentConverter
     */
    public static final AppSearchSchema SCHEMA =
            new AppSearchSchema.Builder(SCHEMA_TYPE)
                    .addProperty(
                            new AppSearchSchema.LongPropertyConfig.Builder(
                                            ALL_REQUIRED_PERMISSIONS_PROPERTY)
                                    .setCardinality(
                                            AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                                    .build())
                    .build();

    @Field(id = 1)
    final int @Nullable [] mAllRequiredPermissions;

    // We still need to convert this class to a GenericDocument until we completely treat it
    // differently in AppSearchImpl.
    // TODO(b/298118943) Remove this once internally we don't use GenericDocument to store
    //  visibility information.
    private @Nullable GenericDocument mGenericDocument;

    private @Nullable Integer mHashCode;

    @Constructor
    VisibilityPermissionConfig(@Param(id = 1) int @Nullable [] allRequiredPermissions) {
        mAllRequiredPermissions = allRequiredPermissions;
    }

    /**
     * Sets a set of Android Permissions that caller must hold to access the schema that the outer
     * {@link SchemaVisibilityConfig} represents.
     */
    public VisibilityPermissionConfig(@NonNull Set<Integer> allRequiredPermissions) {
        mAllRequiredPermissions = toInts(Objects.requireNonNull(allRequiredPermissions));
    }

    /**
     * Returns an array of Android Permissions that caller mush hold to access the schema that the
     * outer {@link SchemaVisibilityConfig} represents.
     */
    public @Nullable Set<Integer> getAllRequiredPermissions() {
        return toIntegerSet(mAllRequiredPermissions);
    }

    private static int @NonNull [] toInts(@NonNull Set<Integer> properties) {
        int[] outputs = new int[properties.size()];
        int i = 0;
        for (int property : properties) {
            outputs[i++] = property;
        }
        return outputs;
    }

    private static @Nullable Set<Integer> toIntegerSet(int @Nullable [] properties) {
        if (properties == null) {
            return null;
        }
        Set<Integer> outputs = new ArraySet<>(properties.length);
        for (int property : properties) {
            outputs.add(property);
        }
        return outputs;
    }

    /**
     * Generates a {@link GenericDocument} from the current class.
     *
     * <p>This conversion is needed until we don't treat Visibility related documents as {@link
     * GenericDocument}s internally.
     */
    public @NonNull GenericDocument toGenericDocument() {
        if (mGenericDocument == null) {
            // This is used as a nested document, we do not need a namespace or id.
            GenericDocument.Builder<?> builder =
                    new GenericDocument.Builder<>(/* namespace= */ "", /* id= */ "", SCHEMA_TYPE);

            if (mAllRequiredPermissions != null) {
                // GenericDocument only supports long, so int[] needs to be converted to
                // long[] here.
                long[] longs = new long[mAllRequiredPermissions.length];
                for (int i = 0; i < mAllRequiredPermissions.length; ++i) {
                    longs[i] = mAllRequiredPermissions[i];
                }
                builder.setPropertyLong(ALL_REQUIRED_PERMISSIONS_PROPERTY, longs);
            }

            // The creationTimestamp doesn't matter for Visibility documents.
            // But to make tests pass, we set it 0 so two GenericDocuments generated from
            // the same VisibilityPermissionConfig can be same.
            builder.setCreationTimestampMillis(0L);

            mGenericDocument = builder.build();
        }
        return mGenericDocument;
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Arrays.hashCode(mAllRequiredPermissions);
        }
        return mHashCode;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VisibilityPermissionConfig)) {
            return false;
        }
        VisibilityPermissionConfig otherVisibilityPermissionConfig =
                (VisibilityPermissionConfig) other;
        return Arrays.equals(
                mAllRequiredPermissions, otherVisibilityPermissionConfig.mAllRequiredPermissions);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        VisibilityPermissionConfigCreator.writeToParcel(this, dest, flags);
    }
}
