/*
 * Copyright 2020 The Android Open Source Project
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
import android.app.appsearch.util.BundleUtil;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.appsearch.flags.Flags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates a request to retrieve documents by namespace and IDs from the {@link
 * AppSearchSession} database.
 *
 * @see AppSearchSession#getByDocumentId
 */
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
@SafeParcelable.Class(creator = "GetByDocumentIdRequestCreator")
public final class GetByDocumentIdRequest extends AbstractSafeParcelable {

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<GetByDocumentIdRequest> CREATOR =
            new GetByDocumentIdRequestCreator();

    /**
     * Schema type to be used in {@link GetByDocumentIdRequest.Builder#addProjection} to apply
     * property paths to all results, excepting any types that have had their own, specific property
     * paths set.
     */
    public static final String PROJECTION_SCHEMA_TYPE_WILDCARD = "*";

    @Field(id = 1, getter = "getNamespace")
    private final @NonNull String mNamespace;

    @Field(id = 2)
    final @NonNull List<String> mIds;

    @Field(id = 3)
    final @NonNull Bundle mTypePropertyPaths;

    /** Cache of the ids. Comes from inflating mIds at first use. */
    private @Nullable Set<String> mIdsCached;

    @Constructor
    GetByDocumentIdRequest(
            @Param(id = 1) @NonNull String namespace,
            @Param(id = 2) @NonNull List<String> ids,
            @Param(id = 3) @NonNull Bundle typePropertyPaths) {
        mNamespace = Objects.requireNonNull(namespace);
        mIds = Objects.requireNonNull(ids);
        mTypePropertyPaths = Objects.requireNonNull(typePropertyPaths);
    }

    /** Returns the namespace attached to the request. */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    /** Returns the set of document IDs attached to the request. */
    public @NonNull Set<String> getIds() {
        if (mIdsCached == null) {
            mIdsCached = Collections.unmodifiableSet(new ArraySet<>(mIds));
        }
        return mIdsCached;
    }

    /**
     * Returns a map from schema type to property paths to be used for projection.
     *
     * <p>If the map is empty, then all properties will be retrieved for all results.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned by this
     * function, rather than calling it multiple times.
     */
    public @NonNull Map<String, List<String>> getProjections() {
        Set<String> schemas = mTypePropertyPaths.keySet();
        Map<String, List<String>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            List<String> propertyPaths = mTypePropertyPaths.getStringArrayList(schema);
            if (propertyPaths != null) {
                typePropertyPathsMap.put(schema, Collections.unmodifiableList(propertyPaths));
            }
        }
        return typePropertyPathsMap;
    }

    /**
     * Returns a map from schema type to property paths to be used for projection.
     *
     * <p>If the map is empty, then all properties will be retrieved for all results.
     *
     * <p>Calling this function repeatedly is inefficient. Prefer to retain the Map returned by this
     * function, rather than calling it multiple times.
     */
    public @NonNull Map<String, List<PropertyPath>> getProjectionPaths() {
        Set<String> schemas = mTypePropertyPaths.keySet();
        Map<String, List<PropertyPath>> typePropertyPathsMap = new ArrayMap<>(schemas.size());
        for (String schema : schemas) {
            List<String> paths = mTypePropertyPaths.getStringArrayList(schema);
            if (paths != null) {
                int pathsSize = paths.size();
                List<PropertyPath> propertyPathList = new ArrayList<>(pathsSize);
                for (int i = 0; i < pathsSize; i++) {
                    propertyPathList.add(new PropertyPath(paths.get(i)));
                }
                typePropertyPathsMap.put(schema, Collections.unmodifiableList(propertyPathList));
            }
        }
        return typePropertyPathsMap;
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        GetByDocumentIdRequestCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link GetByDocumentIdRequest} objects. */
    public static final class Builder {
        private final String mNamespace;
        private List<String> mIds = new ArrayList<>();
        private Bundle mProjectionTypePropertyPaths = new Bundle();
        private boolean mBuilt = false;

        /** Creates a {@link GetByDocumentIdRequest.Builder} instance. */
        public Builder(@NonNull String namespace) {
            mNamespace = Objects.requireNonNull(namespace);
        }

        /** Adds one or more document IDs to the request. */
        @CanIgnoreReturnValue
        public @NonNull Builder addIds(@NonNull String... ids) {
            Objects.requireNonNull(ids);
            resetIfBuilt();
            return addIds(Arrays.asList(ids));
        }

        /** Adds a collection of IDs to the request. */
        @CanIgnoreReturnValue
        public @NonNull Builder addIds(@NonNull Collection<String> ids) {
            Objects.requireNonNull(ids);
            resetIfBuilt();
            mIds.addAll(ids);
            return this;
        }

        /**
         * Adds property paths for the specified type to be used for projection. If property paths
         * are added for a type, then only the properties referred to will be retrieved for results
         * of that type. If a property path that is specified isn't present in a result, it will be
         * ignored for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of results
         * of that type will be retrieved.
         *
         * <p>If property path is added for the {@link
         * GetByDocumentIdRequest#PROJECTION_SCHEMA_TYPE_WILDCARD}, then those property paths will
         * apply to all results, excepting any types that have their own, specific property paths
         * set.
         *
         * @see SearchSpec.Builder#addProjectionPaths
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addProjection(
                @NonNull String schemaType, @NonNull Collection<String> propertyPaths) {
            Objects.requireNonNull(schemaType);
            Objects.requireNonNull(propertyPaths);
            resetIfBuilt();
            ArrayList<String> propertyPathsList = new ArrayList<>(propertyPaths.size());
            for (String propertyPath : propertyPaths) {
                Objects.requireNonNull(propertyPath);
                propertyPathsList.add(propertyPath);
            }
            mProjectionTypePropertyPaths.putStringArrayList(schemaType, propertyPathsList);
            return this;
        }

        /**
         * Adds property paths for the specified type to be used for projection. If property paths
         * are added for a type, then only the properties referred to will be retrieved for results
         * of that type. If a property path that is specified isn't present in a result, it will be
         * ignored for that result. Property paths cannot be null.
         *
         * <p>If no property paths are added for a particular type, then all properties of results
         * of that type will be retrieved.
         *
         * <p>If property path is added for the {@link
         * GetByDocumentIdRequest#PROJECTION_SCHEMA_TYPE_WILDCARD}, then those property paths will
         * apply to all results, excepting any types that have their own, specific property paths
         * set.
         *
         * @see SearchSpec.Builder#addProjectionPaths
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addProjectionPaths(
                @NonNull String schemaType, @NonNull Collection<PropertyPath> propertyPaths) {
            Objects.requireNonNull(schemaType);
            Objects.requireNonNull(propertyPaths);
            List<String> propertyPathsList = new ArrayList<>(propertyPaths.size());
            for (PropertyPath propertyPath : propertyPaths) {
                propertyPathsList.add(propertyPath.toString());
            }
            return addProjection(schemaType, propertyPathsList);
        }

        /** Builds a new {@link GetByDocumentIdRequest}. */
        public @NonNull GetByDocumentIdRequest build() {
            mBuilt = true;
            return new GetByDocumentIdRequest(mNamespace, mIds, mProjectionTypePropertyPaths);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mIds = new ArrayList<>(mIds);
                // No need to clone each propertyPathsList inside mProjectionTypePropertyPaths since
                // the builder only replaces it, never adds to it. So even if the builder is used
                // again, the previous one will remain with the object.
                mProjectionTypePropertyPaths = BundleUtil.deepCopy(mProjectionTypePropertyPaths);
                mBuilt = false;
            }
        }
    }
}
