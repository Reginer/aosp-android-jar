/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.adservices.common.AdServicesStatusUtils.StatusCode;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * Represent a generic response for FLEDGE API's.
 *
 * @hide
 */
public final class FledgeErrorResponse extends AdServicesResponse {

    private FledgeErrorResponse(@StatusCode int statusCode, @Nullable String errorMessage) {
        super(statusCode, errorMessage);
    }

    private FledgeErrorResponse(@NonNull Parcel in) {
        super(in);
    }

    @NonNull
    public static final Creator<FledgeErrorResponse> CREATOR =
            new Parcelable.Creator<FledgeErrorResponse>() {
                @Override
                public FledgeErrorResponse createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new FledgeErrorResponse(in);
                }

                @Override
                public FledgeErrorResponse[] newArray(int size) {
                    return new FledgeErrorResponse[size];
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
    }

    @Override
    public String toString() {
        return "FledgeErrorResponse{"
                + "mStatusCode="
                + mStatusCode
                + ", mErrorMessage='"
                + mErrorMessage
                + "'}";
    }

    /**
     * Builder for {@link FledgeErrorResponse} objects.
     *
     * @hide
     */
    public static final class Builder {
        @StatusCode private int mStatusCode = AdServicesStatusUtils.STATUS_UNSET;
        @Nullable private String mErrorMessage;

        public Builder() {}

        /** Set the Status Code. */
        @NonNull
        public FledgeErrorResponse.Builder setStatusCode(@StatusCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Set the Error Message. */
        @NonNull
        public FledgeErrorResponse.Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /**
         * Builds a {@link FledgeErrorResponse} instance.
         *
         * <p>throws IllegalArgumentException if any of the status code is null or error message is
         * not set for an unsuccessful status
         */
        @NonNull
        public FledgeErrorResponse build() {
            Preconditions.checkArgument(
                    mStatusCode != AdServicesStatusUtils.STATUS_UNSET,
                    "Status code has not been set!");

            return new FledgeErrorResponse(mStatusCode, mErrorMessage);
        }
    }
}
