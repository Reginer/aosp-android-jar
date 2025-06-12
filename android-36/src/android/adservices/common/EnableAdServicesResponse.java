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

package android.adservices.common;

import static android.adservices.common.AdServicesStatusUtils.StatusCode;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * Response parcel of the enableAdServices API.
 *
 * @hide
 */
@SystemApi
public final class EnableAdServicesResponse implements Parcelable {

    @StatusCode private final int mStatusCode;

    private final String mErrorMessage;

    private final boolean mIsSuccess;

    private final boolean mIsApiEnabled;

    private EnableAdServicesResponse(
            @StatusCode int statusCode,
            @Nullable String errorMessage,
            boolean isSuccess,
            boolean isApiEnabled) {
        mStatusCode = statusCode;
        mErrorMessage = errorMessage;
        mIsSuccess = isSuccess;
        mIsApiEnabled = isApiEnabled;
    }

    private EnableAdServicesResponse(@NonNull Parcel in) {
        mStatusCode = in.readInt();
        mErrorMessage = in.readString();
        mIsSuccess = in.readBoolean();
        mIsApiEnabled = in.readBoolean();
    }

    /** Returns the response status code. */
    @StatusCode
    int getStatusCode() {
        return mStatusCode;
    }

    /** Returns whether the enableAdServices API finished successfully. */
    public boolean isSuccess() {
        return mIsSuccess;
    }

    /** Returns whether the enableAdServices API is enabled. */
    public boolean isApiEnabled() {
        return mIsApiEnabled;
    }

    @NonNull
    public static final Creator<EnableAdServicesResponse> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public EnableAdServicesResponse createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new EnableAdServicesResponse(in);
                }

                @Override
                public EnableAdServicesResponse[] newArray(int size) {
                    return new EnableAdServicesResponse[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        dest.writeInt(mStatusCode);
        dest.writeString(mErrorMessage);
        dest.writeBoolean(mIsSuccess);
        dest.writeBoolean(mIsApiEnabled);
    }

    @Override
    public String toString() {
        return "EnableAdServicesResponse{"
                + "mStatusCode="
                + mStatusCode
                + ", mErrorMessage="
                + mErrorMessage
                + ", mIsSuccess="
                + mIsSuccess
                + ", mIsApiEnabled="
                + mIsApiEnabled
                + "'}";
    }

    /**
     * Builder for {@link EnableAdServicesResponse} objects.
     *
     * @hide
     */
    public static final class Builder {
        @StatusCode
        private int mStatusCode = AdServicesStatusUtils.STATUS_UNSET;

        @Nullable
        private String mErrorMessage;

        private boolean mIsSuccess;

        private boolean mIsApiEnabled;

        public Builder() {
        }

        /** Set the enableAdServices API response status Code. */
        @NonNull
        public EnableAdServicesResponse.Builder setStatusCode(
                @AdServicesStatusUtils.StatusCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Set the error messaged passed by the enableAdServices API. */
        @NonNull
        public EnableAdServicesResponse.Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Set the isSuccess bit when enableAdServices API finishes successfully. */
        @NonNull
        public EnableAdServicesResponse.Builder setSuccess(boolean isSuccess) {
            mIsSuccess = isSuccess;
            return this;
        }

        /** Set the isApiEnabled bit when enableAdServices API is enabled. */
        @NonNull
        public EnableAdServicesResponse.Builder setApiEnabled(boolean isApiEnabled) {
            mIsApiEnabled = isApiEnabled;
            return this;
        }

        /**
         * Builds a {@link EnableAdServicesResponse} instance.
         *
         * <p>throws IllegalArgumentException if any of the status code is null or error message is
         * not set for an unsuccessful status.
         */
        @NonNull
        public EnableAdServicesResponse build() {
            Preconditions.checkArgument(
                    mStatusCode != AdServicesStatusUtils.STATUS_UNSET,
                    "Status code has not been set!");

            return new EnableAdServicesResponse(
                    mStatusCode, mErrorMessage, mIsSuccess, mIsApiEnabled);
        }
    }
}
