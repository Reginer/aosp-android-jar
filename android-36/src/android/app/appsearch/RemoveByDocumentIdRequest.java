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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import com.android.appsearch.flags.Flags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates a request to remove documents by namespace and IDs from the {@link AppSearchSession}
 * database.
 *
 * @see AppSearchSession#remove
 */
@SafeParcelable.Class(creator = "RemoveByDocumentIdRequestCreator")
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
public final class RemoveByDocumentIdRequest extends AbstractSafeParcelable {
    /** Creator class for {@link android.app.appsearch.RemoveByDocumentIdRequest}. */
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<RemoveByDocumentIdRequest> CREATOR =
            new RemoveByDocumentIdRequestCreator();

    @Field(id = 1, getter = "getNamespace")
    private final @NonNull String mNamespace;

    @Field(id = 2)
    final @NonNull List<String> mIds;

    private @Nullable Set<String> mIdsCached;

    /**
     * Removes documents by ID.
     *
     * @param namespace Namespace of the document to remove.
     * @param ids The IDs of the documents to delete
     */
    @Constructor
    RemoveByDocumentIdRequest(
            @Param(id = 1) @NonNull String namespace, @Param(id = 2) @NonNull List<String> ids) {
        mNamespace = Objects.requireNonNull(namespace);
        mIds = Objects.requireNonNull(ids);
    }

    /** Returns the namespace to remove documents from. */
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

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        RemoveByDocumentIdRequestCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link RemoveByDocumentIdRequest} objects. */
    public static final class Builder {
        private final String mNamespace;
        private ArraySet<String> mIds = new ArraySet<>();
        private boolean mBuilt = false;

        /** Creates a {@link RemoveByDocumentIdRequest.Builder} instance. */
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

        /** Builds a new {@link RemoveByDocumentIdRequest}. */
        public @NonNull RemoveByDocumentIdRequest build() {
            mBuilt = true;
            return new RemoveByDocumentIdRequest(mNamespace, new ArrayList<>(mIds));
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mIds = new ArraySet<>(mIds);
                mBuilt = false;
            }
        }
    }
}
