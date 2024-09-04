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

import static android.adservices.common.AdServicesStatusUtils.STATUS_SUCCESS;
import static android.adservices.common.AdServicesStatusUtils.StatusCode;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represents an abstract, generic response for AdServices APIs.
 *
 * @hide
 */
public class AdServicesResponse implements Parcelable {
    @NonNull
    public static final Creator<AdServicesResponse> CREATOR =
            new Parcelable.Creator<AdServicesResponse>() {
                @Override
                public AdServicesResponse createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new AdServicesResponse(in);
                }

                @Override
                public AdServicesResponse[] newArray(int size) {
                    return new AdServicesResponse[size];
                }
            };

    @StatusCode protected final int mStatusCode;
    @Nullable protected final String mErrorMessage;

    protected AdServicesResponse(@NonNull Builder builder) {
        mStatusCode = builder.mStatusCode;
        mErrorMessage = builder.mErrorMessage;
    }

    protected AdServicesResponse(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mStatusCode = in.readInt();
        mErrorMessage = in.readString();
    }

    protected AdServicesResponse(@StatusCode int statusCode, @Nullable String errorMessage) {
        mStatusCode = statusCode;
        mErrorMessage = errorMessage;
    }

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

    /** Returns one of the {@code STATUS} constants defined in {@link StatusCode}. */
    @StatusCode
    public int getStatusCode() {
        return mStatusCode;
    }

    /**
     * Returns {@code true} if {@link #getStatusCode} is {@link
     * AdServicesStatusUtils#STATUS_SUCCESS}.
     */
    public boolean isSuccess() {
        return getStatusCode() == STATUS_SUCCESS;
    }

    /** Returns the error message associated with this response. */
    @Nullable
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * Builder for {@link AdServicesResponse} objects.
     *
     * @hide
     */
    public static final class Builder {
        @StatusCode private int mStatusCode = STATUS_SUCCESS;
        @Nullable private String mErrorMessage;

        public Builder() {}

        /** Set the Status Code. */
        @NonNull
        public AdServicesResponse.Builder setStatusCode(@StatusCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Set the Error Message. */
        @NonNull
        public AdServicesResponse.Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Builds a {@link AdServicesResponse} instance. */
        @NonNull
        public AdServicesResponse build() {
            return new AdServicesResponse(this);
        }
    }
}
