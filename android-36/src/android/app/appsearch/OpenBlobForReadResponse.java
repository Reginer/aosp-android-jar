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
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import com.android.appsearch.flags.Flags;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * The response to provide batch operation results of {@link AppSearchSession#openBlobForRead}.
 *
 * <p>This class is used to retrieve the result of a batch read operation on a collection of blob
 * handles.
 *
 * <p class="caution">The returned {@link android.os.ParcelFileDescriptor} must be closed after use
 * to avoid resource leaks. Failing to close the descriptor will result in system resource
 * exhaustion, as each open {@link android.os.ParcelFileDescriptor} occupies a limited file
 * descriptor in the system.
 */
@FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
@SafeParcelable.Class(creator = "OpenBlobForReadResponseCreator")
public final class OpenBlobForReadResponse extends AbstractSafeParcelable implements Closeable {

    public static final @NonNull Parcelable.Creator<OpenBlobForReadResponse> CREATOR =
            new OpenBlobForReadResponseCreator();

    @Field(id = 1)
    final AppSearchBatchResultParcelV2<AppSearchBlobHandle, ParcelFileDescriptor> mResultParcel;

    /** Creates a {@link OpenBlobForReadResponse} with given {@link AppSearchBatchResult}. */
    public OpenBlobForReadResponse(
            @NonNull AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> result) {
        this(AppSearchBatchResultParcelV2.fromBlobHandleToPfd(result));
    }

    @Constructor
    OpenBlobForReadResponse(
            @AbstractSafeParcelable.Param(id = 1) @NonNull
                    AppSearchBatchResultParcelV2<AppSearchBlobHandle, ParcelFileDescriptor>
                            resultParcel) {
        mResultParcel = Objects.requireNonNull(resultParcel);
    }

    /**
     * Returns the {@link AppSearchBatchResult} object containing the results of the read blob for
     * read operation for each {@link AppSearchBlobHandle}.
     *
     * @return A {@link AppSearchBatchResult} maps {@link AppSearchBlobHandle}s which is a unique
     *     identifier for a specific blob being committed to the outcome of that read operation. If
     *     the operation was successful, the result for that handle is {@link ParcelFileDescriptor};
     *     if there was an error, the result contains an {@link AppSearchResult} with details of the
     *     failure.
     */
    public @NonNull AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> getResult() {
        return mResultParcel.getResult();
    }

    @Override
    public void close() {
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> batchResult =
                mResultParcel.getResult();
        for (ParcelFileDescriptor pfd : batchResult.getSuccesses().values()) {
            try {
                pfd.close();
            } catch (IOException ignored) {
                // The file may be already removed, just ignoring any checked exceptions.
            }
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        OpenBlobForReadResponseCreator.writeToParcel(this, dest, flags);
    }
}
