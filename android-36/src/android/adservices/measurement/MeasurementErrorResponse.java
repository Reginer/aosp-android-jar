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

package android.adservices.measurement;

import static android.adservices.common.AdServicesStatusUtils.STATUS_SUCCESS;

import android.adservices.common.AdServicesResponse;
import android.adservices.common.AdServicesStatusUtils;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represents a generic response for Measurement APIs.
 *
 * @hide
 */
public final class MeasurementErrorResponse extends AdServicesResponse {
    @NonNull
    public static final Creator<MeasurementErrorResponse> CREATOR =
            new Parcelable.Creator<MeasurementErrorResponse>() {
                @Override
                public MeasurementErrorResponse createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new MeasurementErrorResponse(in);
                }

                @Override
                public MeasurementErrorResponse[] newArray(int size) {
                    return new MeasurementErrorResponse[size];
                }
            };

    protected MeasurementErrorResponse(@NonNull Builder builder) {
        super(builder.mStatusCode, builder.mErrorMessage);
    }

    protected MeasurementErrorResponse(@NonNull Parcel in) {
        super(in);
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

    /**
     * Builder for {@link MeasurementErrorResponse} objects.
     *
     * @hide
     */
    public static final class Builder {
        @AdServicesStatusUtils.StatusCode private int mStatusCode = STATUS_SUCCESS;
        @Nullable private String mErrorMessage;

        public Builder() {}

        /** Set the Status Code. */
        @NonNull
        public MeasurementErrorResponse.Builder setStatusCode(
                @AdServicesStatusUtils.StatusCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Set the Error Message. */
        @NonNull
        public MeasurementErrorResponse.Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Builds a {@link MeasurementErrorResponse} instance. */
        @NonNull
        public MeasurementErrorResponse build() {
            return new MeasurementErrorResponse(this);
        }
    }
}
