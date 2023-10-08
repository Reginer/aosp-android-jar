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
 * Represents input parameters to the {@link
 * com.android.adservices.service.adselection.AdSelectionServiceImpl#selectAdsFromOutcomes} API.
 *
 * @hide
 */
public final class AdSelectionFromOutcomesInput implements Parcelable {
    @NonNull private final AdSelectionFromOutcomesConfig mAdSelectionFromOutcomesConfig;
    @NonNull private final String mCallerPackageName;

    @NonNull
    public static final Creator<AdSelectionFromOutcomesInput> CREATOR =
            new Creator<AdSelectionFromOutcomesInput>() {
                @Override
                public AdSelectionFromOutcomesInput createFromParcel(@NonNull Parcel source) {
                    return new AdSelectionFromOutcomesInput(source);
                }

                @Override
                public AdSelectionFromOutcomesInput[] newArray(int size) {
                    return new AdSelectionFromOutcomesInput[size];
                }
            };

    private AdSelectionFromOutcomesInput(
            @NonNull AdSelectionFromOutcomesConfig adSelectionFromOutcomesConfig,
            @NonNull String callerPackageName) {
        Objects.requireNonNull(adSelectionFromOutcomesConfig);
        Objects.requireNonNull(callerPackageName);

        this.mAdSelectionFromOutcomesConfig = adSelectionFromOutcomesConfig;
        this.mCallerPackageName = callerPackageName;
    }

    private AdSelectionFromOutcomesInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        this.mAdSelectionFromOutcomesConfig =
                AdSelectionFromOutcomesConfig.CREATOR.createFromParcel(in);
        this.mCallerPackageName = in.readString();
    }

    @NonNull
    public AdSelectionFromOutcomesConfig getAdSelectionFromOutcomesConfig() {
        return mAdSelectionFromOutcomesConfig;
    }

    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdSelectionFromOutcomesInput)) return false;
        AdSelectionFromOutcomesInput that = (AdSelectionFromOutcomesInput) o;
        return Objects.equals(
                        this.mAdSelectionFromOutcomesConfig, that.mAdSelectionFromOutcomesConfig)
                && Objects.equals(this.mCallerPackageName, that.mCallerPackageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdSelectionFromOutcomesConfig, mCallerPackageName);
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable instance's marshaled
     * representation. For example, if the object will include a file descriptor in the output of
     * {@link #writeToParcel(Parcel, int)}, the return value of this method must include the {@link
     * #CONTENTS_FILE_DESCRIPTOR} bit.
     *
     * @return a bitmask indicating the set of special object types marshaled by this Parcelable
     *     object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written. May be 0 or {@link
     *     #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        mAdSelectionFromOutcomesConfig.writeToParcel(dest, flags);
        dest.writeString(mCallerPackageName);
    }

    /**
     * Builder for {@link AdSelectionFromOutcomesInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        @Nullable private AdSelectionFromOutcomesConfig mAdSelectionFromOutcomesConfig;
        @Nullable private String mCallerPackageName;

        public Builder() {}

        /** Sets the {@link AdSelectionFromOutcomesConfig}. */
        @NonNull
        public AdSelectionFromOutcomesInput.Builder setAdSelectionFromOutcomesConfig(
                @NonNull AdSelectionFromOutcomesConfig adSelectionFromOutcomesConfig) {
            Objects.requireNonNull(adSelectionFromOutcomesConfig);

            this.mAdSelectionFromOutcomesConfig = adSelectionFromOutcomesConfig;
            return this;
        }

        /** Sets the caller's package name. */
        @NonNull
        public AdSelectionFromOutcomesInput.Builder setCallerPackageName(
                @NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);

            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /** Builds a {@link AdSelectionFromOutcomesInput} instance. */
        @NonNull
        public AdSelectionFromOutcomesInput build() {
            Objects.requireNonNull(mAdSelectionFromOutcomesConfig);
            Objects.requireNonNull(mCallerPackageName);

            return new AdSelectionFromOutcomesInput(
                    mAdSelectionFromOutcomesConfig, mCallerPackageName);
        }
    }
}
