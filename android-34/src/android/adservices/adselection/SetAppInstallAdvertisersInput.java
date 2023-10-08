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

import android.adservices.common.AdTechIdentifier;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;

import java.util.Objects;
import java.util.Set;

/**
 * Represent input params to the setAppInstallAdvertisers API.
 *
 * @hide
 */
public final class SetAppInstallAdvertisersInput implements Parcelable {
    @NonNull private final Set<AdTechIdentifier> mAdvertisers;
    @NonNull private final String mCallerPackageName;

    @NonNull
    public static final Creator<SetAppInstallAdvertisersInput> CREATOR =
            new Creator<SetAppInstallAdvertisersInput>() {
                @NonNull
                @Override
                public SetAppInstallAdvertisersInput createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new SetAppInstallAdvertisersInput(in);
                }

                @NonNull
                @Override
                public SetAppInstallAdvertisersInput[] newArray(int size) {
                    return new SetAppInstallAdvertisersInput[size];
                }
            };

    private SetAppInstallAdvertisersInput(
            @NonNull Set<AdTechIdentifier> advertisers, @NonNull String callerPackageName) {
        Objects.requireNonNull(advertisers);
        Objects.requireNonNull(callerPackageName);

        this.mAdvertisers = advertisers;
        this.mCallerPackageName = callerPackageName;
    }

    private SetAppInstallAdvertisersInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        this.mAdvertisers =
                AdServicesParcelableUtil.readSetFromParcel(in, AdTechIdentifier.CREATOR);
        this.mCallerPackageName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        AdServicesParcelableUtil.writeSetToParcel(dest, mAdvertisers);
        dest.writeString(mCallerPackageName);
    }

    /**
     * Returns the advertisers, one of the inputs to {@link SetAppInstallAdvertisersInput} as noted
     * in {@code AdSelectionService}.
     */
    @NonNull
    public Set<AdTechIdentifier> getAdvertisers() {
        return mAdvertisers;
    }

    /** @return the caller package name */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    /**
     * Builder for {@link SetAppInstallAdvertisersInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        @Nullable private Set<AdTechIdentifier> mAdvertisers;
        @Nullable private String mCallerPackageName;

        public Builder() {}

        /** Set the advertisers. */
        @NonNull
        public SetAppInstallAdvertisersInput.Builder setAdvertisers(
                @NonNull Set<AdTechIdentifier> advertisers) {
            Objects.requireNonNull(advertisers);
            this.mAdvertisers = advertisers;
            return this;
        }

        /** Sets the caller's package name. */
        @NonNull
        public SetAppInstallAdvertisersInput.Builder setCallerPackageName(
                @NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);

            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /** Builds a {@link SetAppInstallAdvertisersInput} instance. */
        @NonNull
        public SetAppInstallAdvertisersInput build() {
            Objects.requireNonNull(mAdvertisers);
            Objects.requireNonNull(mCallerPackageName);

            return new SetAppInstallAdvertisersInput(mAdvertisers, mCallerPackageName);
        }
    }
}
