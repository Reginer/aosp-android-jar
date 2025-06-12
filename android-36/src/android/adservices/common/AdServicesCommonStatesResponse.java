/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.flags.Flags;

import java.util.Objects;

/**
 * Response parcel of the getAdservicesCommonStates API.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_GET_ADSERVICES_COMMON_STATES_API_ENABLED)
public final class AdServicesCommonStatesResponse implements Parcelable {

    private AdServicesCommonStates mAdServicesCommonStates;

    private AdServicesCommonStatesResponse(AdServicesCommonStates adservicesCommonStates) {
        mAdServicesCommonStates = adservicesCommonStates;
    }

    private AdServicesCommonStatesResponse(@NonNull Parcel in) {
        mAdServicesCommonStates = in.readParcelable(AdServicesCommonStates.class.getClassLoader());
    }

    @NonNull
    public AdServicesCommonStates getAdServicesCommonStates() {
        return mAdServicesCommonStates;
    }

    @NonNull
    public static final Creator<AdServicesCommonStatesResponse> CREATOR =
            new Creator<AdServicesCommonStatesResponse>() {
                @Override
                public AdServicesCommonStatesResponse createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new AdServicesCommonStatesResponse(in);
                }

                @Override
                public AdServicesCommonStatesResponse[] newArray(int size) {
                    return new AdServicesCommonStatesResponse[size];
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

        dest.writeParcelable(mAdServicesCommonStates, flags);
    }

    @Override
    public String toString() {
        return "EnableAdServicesResponse{"
                + "mAdservicesCommonStates="
                + mAdServicesCommonStates
                + "'}";
    }

    /**
     * Builder for {@link AdServicesCommonStatesResponse} objects.
     *
     * @hide
     */
    public static final class Builder {
        private AdServicesCommonStates mAdServicesCommonStates;

        public Builder(@NonNull AdServicesCommonStates adservicesCommonStates) {
            mAdServicesCommonStates = adservicesCommonStates;
        }

        /** Set the enableAdServices API response status Code. */
        @NonNull
        public AdServicesCommonStatesResponse.Builder setAdservicesCommonStates(
                AdServicesCommonStates adservicesCommonStates) {
            mAdServicesCommonStates = adservicesCommonStates;
            return this;
        }

        /**
         * Builds a {@link AdServicesCommonStatesResponse} instance.
         *
         * <p>throws IllegalArgumentException if any of the status code is null or error message is
         * not set for an unsuccessful status.
         */
        @NonNull
        public AdServicesCommonStatesResponse build() {
            return new AdServicesCommonStatesResponse(mAdServicesCommonStates);
        }
    }
}
