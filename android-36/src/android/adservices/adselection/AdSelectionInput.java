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

package android.adservices.adselection;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represent input params to the RunAdSelectionInput API.
 *
 * @hide
 */
public final class AdSelectionInput implements Parcelable {
    @Nullable private final AdSelectionConfig mAdSelectionConfig;
    @Nullable private final String mCallerPackageName;

    @NonNull
    public static final Creator<AdSelectionInput> CREATOR =
            new Creator<AdSelectionInput>() {
                public AdSelectionInput createFromParcel(Parcel in) {
                    return new AdSelectionInput(in);
                }

                public AdSelectionInput[] newArray(int size) {
                    return new AdSelectionInput[size];
                }
            };

    private AdSelectionInput(
            @NonNull AdSelectionConfig adSelectionConfig, @NonNull String callerPackageName) {
        Objects.requireNonNull(adSelectionConfig);

        this.mAdSelectionConfig = adSelectionConfig;
        this.mCallerPackageName = callerPackageName;
    }

    private AdSelectionInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        this.mAdSelectionConfig = AdSelectionConfig.CREATOR.createFromParcel(in);
        this.mCallerPackageName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        mAdSelectionConfig.writeToParcel(dest, flags);
        dest.writeString(mCallerPackageName);
    }

    /**
     * Returns the adSelectionConfig, one of the inputs to {@link AdSelectionInput} as noted in
     * {@code AdSelectionService}.
     */
    @NonNull
    public AdSelectionConfig getAdSelectionConfig() {
        return mAdSelectionConfig;
    }

    /** @return the caller package name */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    /**
     * Builder for {@link AdSelectionInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        @Nullable private AdSelectionConfig mAdSelectionConfig;
        @Nullable private String mCallerPackageName;

        public Builder() {}

        /** Set the AdSelectionConfig. */
        @NonNull
        public AdSelectionInput.Builder setAdSelectionConfig(
                @NonNull AdSelectionConfig adSelectionConfig) {
            Objects.requireNonNull(adSelectionConfig);

            this.mAdSelectionConfig = adSelectionConfig;
            return this;
        }

        /** Sets the caller's package name. */
        @NonNull
        public AdSelectionInput.Builder setCallerPackageName(@NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);

            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /** Builds a {@link AdSelectionInput} instance. */
        @NonNull
        public AdSelectionInput build() {
            Objects.requireNonNull(mAdSelectionConfig);
            Objects.requireNonNull(mCallerPackageName);

            return new AdSelectionInput(mAdSelectionConfig, mCallerPackageName);
        }
    }
}
