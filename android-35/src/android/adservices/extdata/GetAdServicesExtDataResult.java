/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.adservices.extdata;

import android.adservices.common.AdServicesResponse;
import android.adservices.common.AdServicesStatusUtils;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.os.Parcel;

import java.util.Objects;

/**
 * Represent the result from {@link AdServicesExtDataStorageService} API.
 *
 * @hide
 */
public final class GetAdServicesExtDataResult extends AdServicesResponse {
    @NonNull private final AdServicesExtDataParams mAdServicesExtDataParams;

    private GetAdServicesExtDataResult(
            @AdServicesStatusUtils.StatusCode int resultCode,
            @Nullable String errorMessage,
            @NonNull AdServicesExtDataParams adServicesExtDataParams) {
        super(resultCode, errorMessage);
        mAdServicesExtDataParams = Objects.requireNonNull(adServicesExtDataParams);
    }

    private GetAdServicesExtDataResult(@NonNull Parcel in) {
        super(in);
        Objects.requireNonNull(in);
        // Method deprecated starting from Android T; however, AdServicesExtDataStorageService is
        // intended to only be used on Android S-.
        mAdServicesExtDataParams =
                in.readParcelable(AdServicesExtDataParams.class.getClassLoader());
    }

    /** Creator for Parcelable. */
    @NonNull
    public static final Creator<GetAdServicesExtDataResult> CREATOR =
            new Creator<GetAdServicesExtDataResult>() {
                @Override
                public GetAdServicesExtDataResult createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new GetAdServicesExtDataResult(in);
                }

                @Override
                public GetAdServicesExtDataResult[] newArray(int size) {
                    return new GetAdServicesExtDataResult[size];
                }
            };

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mStatusCode);
        out.writeString(mErrorMessage);
        out.writeParcelable(mAdServicesExtDataParams, flags);
    }

    /**
     * Returns the error message associated with this result.
     *
     * <p>If {@link #isSuccess} is {@code true}, the error message is always {@code null}. The error
     * message may be {@code null} even if {@link #isSuccess} is {@code false}.
     */
    @Nullable
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /** Returns the AdServicesExtDataParams value. */
    @NonNull
    public AdServicesExtDataParams getAdServicesExtDataParams() {
        return mAdServicesExtDataParams;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format(
                "GetAdServicesExtIntDataResult{"
                        + "mResultCode=%d, "
                        + "mErrorMessage=%s, "
                        + "mAdServicesExtDataParams=%s}",
                mStatusCode, mErrorMessage, mAdServicesExtDataParams.toString());
    }

    /**
     * Builder for {@link GetAdServicesExtDataResult} objects.
     *
     * @hide
     */
    public static final class Builder {
        @AdServicesStatusUtils.StatusCode private int mStatusCode;

        @Nullable private String mErrorMessage;
        @NonNull private AdServicesExtDataParams mAdServicesExtDataParams;

        public Builder() {}

        /** Set the Result Code. */
        @NonNull
        public Builder setStatusCode(@AdServicesStatusUtils.StatusCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Set the Error Message. */
        @NonNull
        public Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Set the AdServicesExtDataParams */
        @NonNull
        public Builder setAdServicesExtDataParams(AdServicesExtDataParams adServicesExtDataParams) {
            mAdServicesExtDataParams = adServicesExtDataParams;
            return this;
        }

        /** Builds a {@link GetAdServicesExtDataResult} instance. */
        @NonNull
        public GetAdServicesExtDataResult build() {
            if (mAdServicesExtDataParams == null) {
                throw new IllegalArgumentException("AdServicesExtDataParams is null");
            }

            return new GetAdServicesExtDataResult(
                    mStatusCode, mErrorMessage, mAdServicesExtDataParams);
        }
    }
}
