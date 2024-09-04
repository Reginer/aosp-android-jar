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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Result from the isAdServicesEnabled API.
 *
 * @hide
 */
public final class IsAdServicesEnabledResult implements Parcelable {
    @Nullable private final String mErrorMessage;
    private final boolean mAdServicesEnabled;

    private IsAdServicesEnabledResult(@Nullable String errorMessage, @NonNull boolean enabled) {
        mErrorMessage = errorMessage;
        mAdServicesEnabled = enabled;
    }

    private IsAdServicesEnabledResult(@NonNull Parcel in) {
        mErrorMessage = in.readString();
        mAdServicesEnabled = in.readBoolean();
    }

    public static final @NonNull Creator<IsAdServicesEnabledResult> CREATOR =
            new Creator<IsAdServicesEnabledResult>() {
                @Override
                public IsAdServicesEnabledResult createFromParcel(Parcel in) {
                    return new IsAdServicesEnabledResult(in);
                }

                @Override
                public IsAdServicesEnabledResult[] newArray(int size) {
                    return new IsAdServicesEnabledResult[size];
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
        out.writeString(mErrorMessage);
        out.writeBoolean(mAdServicesEnabled);
    }

    /** Returns the error message associated with this result. */
    @Nullable
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /** Returns the Adservices enabled status. */
    @NonNull
    public boolean getAdServicesEnabled() {
        return mAdServicesEnabled;
    }

    @Override
    public String toString() {
        return "GetAdserviceStatusResult{"
                + ", mErrorMessage='"
                + mErrorMessage
                + ", mAdservicesEnabled="
                + mAdServicesEnabled
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IsAdServicesEnabledResult)) {
            return false;
        }

        IsAdServicesEnabledResult that = (IsAdServicesEnabledResult) o;

        return Objects.equals(mErrorMessage, that.mErrorMessage)
                && mAdServicesEnabled == that.mAdServicesEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mErrorMessage, mAdServicesEnabled);
    }

    /**
     * Builder for {@link IsAdServicesEnabledResult} objects.
     *
     * @hide
     */
    public static final class Builder {
        @Nullable private String mErrorMessage;
        private boolean mAdServicesEnabled;

        public Builder() {}

        /** Set the Error Message. */
        public @NonNull Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Set the list of the returned Status */
        public @NonNull Builder setAdServicesEnabled(@NonNull boolean adServicesEnabled) {
            mAdServicesEnabled = adServicesEnabled;
            return this;
        }

        /**
         * Builds a {@link IsAdServicesEnabledResult} instance.
         *
         * <p>throws IllegalArgumentException if any of the params are null or there is any mismatch
         * in the size of ModelVersions and TaxonomyVersions.
         */
        public @NonNull IsAdServicesEnabledResult build() {
            return new IsAdServicesEnabledResult(mErrorMessage, mAdServicesEnabled);
        }
    }
}
