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

package android.adservices.adid;

import android.adservices.common.AdServicesResponse;
import android.adservices.common.AdServicesStatusUtils;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represent the result from the getAdId API.
 *
 * @hide
 */
public final class GetAdIdResult extends AdServicesResponse {
    @NonNull private final String mAdId;
    private final boolean mLimitAdTrackingEnabled;

    private GetAdIdResult(
            @AdServicesStatusUtils.StatusCode int resultCode,
            @Nullable String errorMessage,
            @NonNull String adId,
            boolean isLimitAdTrackingEnabled) {
        super(resultCode, errorMessage);
        mAdId = adId;
        mLimitAdTrackingEnabled = isLimitAdTrackingEnabled;
    }

    private GetAdIdResult(@NonNull Parcel in) {
        super(in);
        Objects.requireNonNull(in);

        mAdId = in.readString();
        mLimitAdTrackingEnabled = in.readBoolean();
    }

    public static final @NonNull Creator<GetAdIdResult> CREATOR =
            new Parcelable.Creator<GetAdIdResult>() {
                @Override
                public GetAdIdResult createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new GetAdIdResult(in);
                }

                @Override
                public GetAdIdResult[] newArray(int size) {
                    return new GetAdIdResult[size];
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
        out.writeString(mAdId);
        out.writeBoolean(mLimitAdTrackingEnabled);
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

    /** Returns the advertising ID associated with this result. */
    @NonNull
    public String getAdId() {
        return mAdId;
    }

    /** Returns the Limited adtracking field associated with this result. */
    public boolean isLatEnabled() {
        return mLimitAdTrackingEnabled;
    }

    @Override
    public String toString() {
        return "GetAdIdResult{"
                + "mResultCode="
                + mStatusCode
                + ", mErrorMessage='"
                + mErrorMessage
                + '\''
                + ", mAdId="
                + mAdId
                + ", mLimitAdTrackingEnabled="
                + mLimitAdTrackingEnabled
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof GetAdIdResult)) {
            return false;
        }

        GetAdIdResult that = (GetAdIdResult) o;

        return mStatusCode == that.mStatusCode
                && Objects.equals(mErrorMessage, that.mErrorMessage)
                && Objects.equals(mAdId, that.mAdId)
                && (mLimitAdTrackingEnabled == that.mLimitAdTrackingEnabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStatusCode, mErrorMessage, mAdId, mLimitAdTrackingEnabled);
    }

    /**
     * Builder for {@link GetAdIdResult} objects.
     *
     * @hide
     */
    public static final class Builder {
        private @AdServicesStatusUtils.StatusCode int mStatusCode;
        @Nullable private String mErrorMessage;
        @NonNull private String mAdId;
        private boolean mLimitAdTrackingEnabled;

        public Builder() {}

        /** Set the Result Code. */
        public @NonNull Builder setStatusCode(@AdServicesStatusUtils.StatusCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Set the Error Message. */
        public @NonNull Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Set the adid. */
        public @NonNull Builder setAdId(@NonNull String adId) {
            mAdId = adId;
            return this;
        }

        /** Set the Limited AdTracking enabled field. */
        public @NonNull Builder setLatEnabled(boolean isLimitAdTrackingEnabled) {
            mLimitAdTrackingEnabled = isLimitAdTrackingEnabled;
            return this;
        }

        /** Builds a {@link GetAdIdResult} instance. */
        public @NonNull GetAdIdResult build() {
            if (mAdId == null) {
                throw new IllegalArgumentException("adId is null");
            }

            return new GetAdIdResult(mStatusCode, mErrorMessage, mAdId, mLimitAdTrackingEnabled);
        }
    }
}
