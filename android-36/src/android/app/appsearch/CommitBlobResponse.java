/*
 * Copyright 2024 The Android Open Source Project
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
import android.app.appsearch.aidl.AppSearchBatchResultParcelV2;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.appsearch.flags.Flags;

import java.util.Objects;

/**
 * The response to provide batch operation results of {@link AppSearchSession#commitBlob}.
 *
 * <p>This class is used to retrieve the result of a batch commit operation on a collection of blob
 * handles.
 */
@FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
@SafeParcelable.Class(creator = "CommitBlobResponseCreator")
public final class CommitBlobResponse extends AbstractSafeParcelable {

    public static final @NonNull Parcelable.Creator<CommitBlobResponse> CREATOR =
            new CommitBlobResponseCreator();

    @Field(id = 1, getter = "getResponseParcel")
    private final AppSearchBatchResultParcelV2<AppSearchBlobHandle, Void> mResultParcel;

    /** Creates a {@link CommitBlobResponse} with given {@link AppSearchBatchResult}. */
    public CommitBlobResponse(@NonNull AppSearchBatchResult<AppSearchBlobHandle, Void> result) {
        this(AppSearchBatchResultParcelV2.fromBlobHandleToVoid(result));
    }

    @Constructor
    CommitBlobResponse(
            @Param(id = 1) @NonNull
                    AppSearchBatchResultParcelV2<AppSearchBlobHandle, Void> resultParcel) {
        mResultParcel = Objects.requireNonNull(resultParcel);
    }

    /**
     * Returns the {@link AppSearchBatchResult} object containing the results of the commit
     * operation for each {@link AppSearchBlobHandle}.
     *
     * @return A {@link AppSearchBatchResult} maps {@link AppSearchBlobHandle}s which is a unique
     *     identifier for a specific blob being committed to the outcome of that commit. If the
     *     operation was successful, the result for that handle is {@code null}; if there was an
     *     error, the result contains an {@link AppSearchResult} with details of the failure.
     */
    public @NonNull AppSearchBatchResult<AppSearchBlobHandle, Void> getResult() {
        return mResultParcel.getResult();
    }

    /**
     * Retrieves the underlying parcel representation of the batch result.
     *
     * @hide
     */
    public @NonNull AppSearchBatchResultParcelV2<AppSearchBlobHandle, Void> getResponseParcel() {
        return mResultParcel;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CommitBlobResponseCreator.writeToParcel(this, dest, flags);
    }
}
