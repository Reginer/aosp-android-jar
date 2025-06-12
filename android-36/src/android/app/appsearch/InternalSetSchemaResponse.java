/*
 * Copyright 2022 The Android Open Source Project
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
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * An internal wrapper class of {@link SetSchemaResponse}.
 *
 * <p>For public users, if the {@link android.app.appsearch.AppSearchSession#setSchema} failed, we
 * will directly throw an Exception. But AppSearch internal need to divert the incompatible changes
 * form other call flows. This class adds a {@link #isSuccess()} to indicate if the call fails
 * because of incompatible change.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "InternalSetSchemaResponseCreator")
public class InternalSetSchemaResponse extends AbstractSafeParcelable {

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final Parcelable.@NonNull Creator<InternalSetSchemaResponse> CREATOR =
            new InternalSetSchemaResponseCreator();

    @Field(id = 1, getter = "isSuccess")
    private final boolean mIsSuccess;

    @Field(id = 2, getter = "getSetSchemaResponse")
    private final SetSchemaResponse mSetSchemaResponse;

    @Field(id = 3, getter = "getErrorMessage")
    private final @Nullable String mErrorMessage;

    @Constructor
    public InternalSetSchemaResponse(
            @Param(id = 1) boolean isSuccess,
            @Param(id = 2) @NonNull SetSchemaResponse setSchemaResponse,
            @Param(id = 3) @Nullable String errorMessage) {
        Objects.requireNonNull(setSchemaResponse);
        mIsSuccess = isSuccess;
        mSetSchemaResponse = setSchemaResponse;
        mErrorMessage = errorMessage;
    }

    /**
     * Creates a new successful {@link InternalSetSchemaResponse}.
     *
     * @param setSchemaResponse The object this internal object represents.
     */
    public static @NonNull InternalSetSchemaResponse newSuccessfulSetSchemaResponse(
            @NonNull SetSchemaResponse setSchemaResponse) {
        return new InternalSetSchemaResponse(
                /* isSuccess= */ true, setSchemaResponse, /* errorMessage= */ null);
    }

    /**
     * Creates a new failed {@link InternalSetSchemaResponse}.
     *
     * @param setSchemaResponse The object this internal object represents.
     * @param errorMessage An string describing the reason or nature of the failure.
     */
    public static @NonNull InternalSetSchemaResponse newFailedSetSchemaResponse(
            @NonNull SetSchemaResponse setSchemaResponse, @NonNull String errorMessage) {
        return new InternalSetSchemaResponse(
                /* isSuccess= */ false, setSchemaResponse, errorMessage);
    }

    /** Returns {@code true} if the schema request is proceeded successfully. */
    public boolean isSuccess() {
        return mIsSuccess;
    }

    /**
     * Returns the {@link SetSchemaResponse} of the set schema call.
     *
     * <p>The call may or may not success. Check {@link #isSuccess()} before call this method.
     */
    public @NonNull SetSchemaResponse getSetSchemaResponse() {
        return mSetSchemaResponse;
    }

    /**
     * Returns the error message associated with this response.
     *
     * <p>If {@link #isSuccess} is {@code true}, the error message is always {@code null}.
     */
    public @Nullable String getErrorMessage() {
        return mErrorMessage;
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        InternalSetSchemaResponseCreator.writeToParcel(this, dest, flags);
    }
}
