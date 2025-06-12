/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.app.appsearch.aidl;

import static android.app.appsearch.ParcelableUtil.WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL;
import static android.app.appsearch.ParcelableUtil.WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB;
import static android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.AppSearchResult;
import android.app.appsearch.CommitBlobResponse;
import android.app.appsearch.OpenBlobForReadResponse;
import android.app.appsearch.OpenBlobForWriteResponse;
import android.app.appsearch.ParcelableUtil;
import android.app.appsearch.RemoveBlobResponse;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Parcelable wrapper around {@link AppSearchResult}.
 *
 * <p>{@link AppSearchResult} can contain any value, including non-parcelable values. For the
 * specific case of sending {@link AppSearchResult} across Binder, this class wraps an {@link
 * AppSearchResult} that contains a parcelable type and provides parcelability of the whole
 * structure.
 *
 * <p>Compare to deprecated {@link AppSearchResultParcel}, this class could config how to write it
 * to the parcel. Therefore binder objects and {@link ParcelFileDescriptor} is supported in this
 * class.
 *
 * @see ParcelableUtil#WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB
 * @see ParcelableUtil#WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL
 * @param <ValueType> The type of result object for successful calls. Must be a parcelable type.
 * @hide
 */
@SafeParcelable.Class(creator = "AppSearchResultParcelV2Creator", creatorIsFinal = false)
public final class AppSearchResultParcelV2<ValueType> extends AbstractSafeParcelable {
    private static final String TAG = "AppSearchResultParcelV2";

    @NonNull
    @SuppressWarnings("rawtypes")
    public static final Parcelable.Creator<AppSearchResultParcelV2> CREATOR =
            new AppSearchResultParcelV2Creator() {
                @Override
                public AppSearchResultParcelV2 createFromParcel(Parcel in) {
                    int writeParcelModel = in.readInt();
                    switch (writeParcelModel) {
                        case WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB:
                            byte[] dataBlob = Objects.requireNonNull(ParcelableUtil.readBlob(in));
                            Parcel unmarshallParcel = Parcel.obtain();
                            try {
                                unmarshallParcel.unmarshall(dataBlob, 0, dataBlob.length);
                                unmarshallParcel.setDataPosition(0);
                                return directlyReadFromParcel(unmarshallParcel);
                            } finally {
                                unmarshallParcel.recycle();
                            }
                        case WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL:
                            return directlyReadFromParcel(in);
                        default:
                            throw new UnsupportedOperationException(
                                    "Cannot read AppSearchResultParcelV2 from Parcel with "
                                            + "unknown model: "
                                            + writeParcelModel);
                    }
                }
            };

    @NonNull
    private static final Parcelable.Creator<AppSearchResultParcelV2> CREATOR_WITHOUT_BLOB =
            new AppSearchResultParcelV2Creator();

    static AppSearchResultParcelV2<?> directlyReadFromParcel(@NonNull Parcel data) {
        return CREATOR_WITHOUT_BLOB.createFromParcel(data);
    }

    static void directlyWriteToParcel(
            @NonNull AppSearchResultParcelV2<?> result, @NonNull Parcel data, int flags) {
        AppSearchResultParcelV2Creator.writeToParcel(result, data, flags);
    }

    /**
     * The flags indicate how we write this object to parcel and read it.
     *
     * @see ParcelableUtil#WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB
     * @see ParcelableUtil#WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL
     */
    @Field(id = 1)
    @ParcelableUtil.WriteParcelMode
    int mWriteParcelMode;

    @Field(id = 2)
    @AppSearchResult.ResultCode
    int mResultCode;

    @Field(id = 3)
    @Nullable
    String mErrorMessage;

    @Field(id = 4)
    @Nullable
    ParcelFileDescriptor mParcelFileDescriptor;

    @Field(id = 5)
    @Nullable
    OpenBlobForWriteResponse mOpenBlobForWriteResponse;

    @Field(id = 6)
    @Nullable
    CommitBlobResponse mCommitBlobResponse;

    @Field(id = 7)
    @Nullable
    OpenBlobForReadResponse mOpenBlobForReadResponse;

    @Field(id = 8)
    @Nullable
    RemoveBlobResponse mRemoveBlobResponse;

    @NonNull AppSearchResult<ValueType> mResultCached;

    /**
     * Creates an AppSearchResultParcelV2 for given value type.
     *
     * @param resultCode A {@link AppSearchResult} result code for {@link IAppSearchManager} API
     *     response.
     * @param errorMessage An error message in case of a failed response.
     */
    @Constructor
    AppSearchResultParcelV2(
            @Param(id = 1) int writeParcelMode,
            @Param(id = 2) @AppSearchResult.ResultCode int resultCode,
            @Param(id = 3) @Nullable String errorMessage,
            @Param(id = 4) @Nullable ParcelFileDescriptor parcelFileDescriptor,
            @Param(id = 5) @Nullable OpenBlobForWriteResponse openBlobForWriteResponse,
            @Param(id = 6) @Nullable CommitBlobResponse commitBlobResponse,
            @Param(id = 7) @Nullable OpenBlobForReadResponse openBlobForReadResponse,
            @Param(id = 8) @Nullable RemoveBlobResponse removeBlobResponse) {
        mWriteParcelMode = writeParcelMode;
        mResultCode = resultCode;
        mErrorMessage = errorMessage;
        if (resultCode == AppSearchResult.RESULT_OK) {
            mParcelFileDescriptor = parcelFileDescriptor;
            mOpenBlobForWriteResponse = openBlobForWriteResponse;
            mCommitBlobResponse = commitBlobResponse;
            mOpenBlobForReadResponse = openBlobForReadResponse;
            mRemoveBlobResponse = removeBlobResponse;
            if (mParcelFileDescriptor != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mParcelFileDescriptor);
            } else if (mOpenBlobForWriteResponse != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mOpenBlobForWriteResponse);
            } else if (mCommitBlobResponse != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mCommitBlobResponse);
            } else if (mOpenBlobForReadResponse != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mOpenBlobForReadResponse);
            } else if (mRemoveBlobResponse != null) {
                mResultCached =
                        (AppSearchResult<ValueType>)
                                AppSearchResult.newSuccessfulResult(mRemoveBlobResponse);
            } else {
                // Default case where code is OK and value is null.
                mResultCached = AppSearchResult.newSuccessfulResult(null);
            }
        } else {
            mResultCached = AppSearchResult.newFailedResult(mResultCode, mErrorMessage);
        }
    }

    /**
     * Creates a new {@link AppSearchResultParcelV2} from the given result in case a successful Void
     * response.
     */
    public static AppSearchResultParcelV2 fromVoid() {
        // We can marshall a void results, but since it is always a small object, we can directly
        // write it to parcel.
        return new AppSearchResultParcelV2.Builder<>(
                        WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL, AppSearchResult.RESULT_OK)
                .build();
    }

    /** Creates a new failed {@link AppSearchResultParcelV2} from result code and error message. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static AppSearchResultParcelV2 fromFailedResult(AppSearchResult failedResult) {
        if (failedResult.isSuccess()) {
            throw new IllegalStateException(
                    "Creating a failed AppSearchResultParcelV2 from a " + "successful response");
        }
        // We can marshall a failed results, but since it is always a small object, we can directly
        // write it to parcel.
        return new AppSearchResultParcelV2.Builder<>(
                        WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL, failedResult.getResultCode())
                .setErrorMessage(failedResult.getErrorMessage())
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcelV2} from the given result in case a successful
     * {@link ParcelFileDescriptor}.
     */
    public static AppSearchResultParcelV2<ParcelFileDescriptor> fromParcelFileDescriptor(
            ParcelFileDescriptor parcelFileDescriptor) {
        // We CANNOT marshall a FD, we have to directly write it to parcel.
        return new AppSearchResultParcelV2.Builder<ParcelFileDescriptor>(
                        WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL, AppSearchResult.RESULT_OK)
                .setParcelFileDescriptor(parcelFileDescriptor)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcelV2} from the given result in case a successful
     * {@link OpenBlobForWriteResponse}.
     */
    public static AppSearchResultParcelV2<OpenBlobForWriteResponse> fromOpenBlobForWriteResponse(
            OpenBlobForWriteResponse openBlobForWriteResponse) {
        // We CANNOT marshall OpenBlobForWriteResponse, since it contains FD, we have to directly
        // write it to parcel.
        return new AppSearchResultParcelV2.Builder<OpenBlobForWriteResponse>(
                        WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL, AppSearchResult.RESULT_OK)
                .setOpenBlobForWriteResponse(openBlobForWriteResponse)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcelV2} from the given result in case a successful
     * {@link RemoveBlobResponse}.
     */
    public static AppSearchResultParcelV2<RemoveBlobResponse> fromRemoveBlobResponseParcel(
            RemoveBlobResponse removeBlobResponse) {
        return new AppSearchResultParcelV2.Builder<RemoveBlobResponse>(
                        WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB, AppSearchResult.RESULT_OK)
                .setRemoveBlobResponse(removeBlobResponse)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcelV2} from the given result in case a successful
     * {@link CommitBlobResponse}.
     */
    public static AppSearchResultParcelV2<CommitBlobResponse> fromCommitBlobResponseParcel(
            CommitBlobResponse commitBlobResponse) {
        return new AppSearchResultParcelV2.Builder<CommitBlobResponse>(
                        WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB, AppSearchResult.RESULT_OK)
                .setCommitBlobResponse(commitBlobResponse)
                .build();
    }

    /**
     * Creates a new {@link AppSearchResultParcelV2} from the given result in case a successful
     * {@link OpenBlobForReadResponse}.
     */
    public static AppSearchResultParcelV2<OpenBlobForReadResponse> fromOpenBlobForReadResponse(
            OpenBlobForReadResponse OpenBlobForReadResponse) {
        // We CANNOT marshall OpenBlobForReadResponse, since it contains FD, we have to directly
        // write it to parcel.
        return new AppSearchResultParcelV2.Builder<OpenBlobForReadResponse>(
                        WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL, AppSearchResult.RESULT_OK)
                .setOpenBlobForReadResponse(OpenBlobForReadResponse)
                .build();
    }

    @NonNull
    public AppSearchResult<ValueType> getResult() {
        return mResultCached;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mWriteParcelMode);
        switch (mWriteParcelMode) {
            case WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB:
                byte[] bytes;
                // Create a parcel object to serialize results. So that we can use
                // Parcel.writeBlob() to
                // send data. WriteBlob() could take care of whether to pass data via binder
                // directly or
                // Android shared memory if the data is large.
                Parcel data = Parcel.obtain();
                try {
                    directlyWriteToParcel(this, data, flags);
                    bytes = data.marshall();
                } finally {
                    data.recycle();
                }
                ParcelableUtil.writeBlob(dest, bytes);
                break;
            case WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL:
                // It's important to add the PARCELABLE_WRITE_RETURN_VALUE flags to ensure
                // resources, such as ParcelFileDescriptor, are released on the sender's side.
                // Normally, PARCELABLE_WRITE_RETURN_VALUE is automatically added when a parcelable
                // object is directly returned in a binder call.
                // However, since AppSearch uses a callback mechanism and a void binder call
                // pattern, we need to manually add the PARCELABLE_WRITE_RETURN_VALUE flag when
                // parceling this object to invoke the callback.
                directlyWriteToParcel(this, dest, flags | PARCELABLE_WRITE_RETURN_VALUE);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Cannot write AppSearchResultParcelV2 to Parcel with unknown model: "
                                + mWriteParcelMode);
        }
    }

    /**
     * Builder for {@link AppSearchResultParcelV2} objects.
     *
     * @param <ValueType> The type of the result objects for successful results.
     */
    static final class Builder<ValueType> {

        @ParcelableUtil.WriteParcelMode private final int mWriteParcelMode;
        @AppSearchResult.ResultCode private final int mResultCode;
        @Nullable private String mErrorMessage;
        @Nullable private ParcelFileDescriptor mParcelFileDescriptor;
        @Nullable private OpenBlobForWriteResponse mOpenBlobForWriteResponse;
        @Nullable private CommitBlobResponse mCommitBlobResponse;
        @Nullable private OpenBlobForReadResponse mOpenBlobForReadResponse;
        @Nullable private RemoveBlobResponse mRemoveBlobResponse;

        /** Builds an {@link AppSearchResultParcelV2.Builder}. */
        Builder(@ParcelableUtil.WriteParcelMode int writeParcelMode, int resultCode) {
            mWriteParcelMode = writeParcelMode;
            mResultCode = resultCode;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setParcelFileDescriptor(ParcelFileDescriptor parcelFileDescriptor) {
            mParcelFileDescriptor = parcelFileDescriptor;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setOpenBlobForWriteResponse(
                OpenBlobForWriteResponse openBlobForWriteResponse) {
            mOpenBlobForWriteResponse = openBlobForWriteResponse;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setRemoveBlobResponse(RemoveBlobResponse removeBlobResponse) {
            mRemoveBlobResponse = removeBlobResponse;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setCommitBlobResponse(CommitBlobResponse commitBlobResponse) {
            mCommitBlobResponse = commitBlobResponse;
            return this;
        }

        @CanIgnoreReturnValue
        Builder<ValueType> setOpenBlobForReadResponse(
                OpenBlobForReadResponse OpenBlobForReadResponse) {
            mOpenBlobForReadResponse = OpenBlobForReadResponse;
            return this;
        }

        /**
         * Builds an {@link AppSearchResultParcelV2} object from the contents of this {@link
         * AppSearchResultParcelV2.Builder}.
         */
        @NonNull
        AppSearchResultParcelV2<ValueType> build() {
            return new AppSearchResultParcelV2<>(
                    mWriteParcelMode,
                    mResultCode,
                    mErrorMessage,
                    mParcelFileDescriptor,
                    mOpenBlobForWriteResponse,
                    mCommitBlobResponse,
                    mOpenBlobForReadResponse,
                    mRemoveBlobResponse);
        }
    }
}
