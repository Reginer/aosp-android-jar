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

import static android.adservices.adselection.AdSelectionOutcome.UNSET_AD_SELECTION_ID;
import static android.adservices.adselection.AdSelectionOutcome.UNSET_AD_SELECTION_ID_MESSAGE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * Represent input params to the reportImpression API.
 *
 * @hide
 */
public final class ReportImpressionInput implements Parcelable {
    private final long mAdSelectionId;
    @NonNull private final AdSelectionConfig mAdSelectionConfig;
    @NonNull private final String mCallerPackageName;

    @NonNull
    public static final Parcelable.Creator<ReportImpressionInput> CREATOR =
            new Parcelable.Creator<ReportImpressionInput>() {
                public ReportImpressionInput createFromParcel(Parcel in) {
                    return new ReportImpressionInput(in);
                }

                public ReportImpressionInput[] newArray(int size) {
                    return new ReportImpressionInput[size];
                }
            };

    private ReportImpressionInput(
            long adSelectionId,
            @NonNull AdSelectionConfig adSelectionConfig,
            @NonNull String callerPackageName) {
        Objects.requireNonNull(adSelectionConfig);

        this.mAdSelectionId = adSelectionId;
        this.mAdSelectionConfig = adSelectionConfig;
        this.mCallerPackageName = callerPackageName;
    }

    private ReportImpressionInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        this.mAdSelectionId = in.readLong();
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

        dest.writeLong(mAdSelectionId);
        mAdSelectionConfig.writeToParcel(dest, flags);
        dest.writeString(mCallerPackageName);
    }

    /**
     * Returns the adSelectionId, one of the inputs to {@link ReportImpressionInput} as noted in
     * {@code AdSelectionService}.
     */
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /**
     * Returns the adSelectionConfig, one of the inputs to {@link ReportImpressionInput} as noted in
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
     * Builder for {@link ReportImpressionInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        private long mAdSelectionId = UNSET_AD_SELECTION_ID;
        @Nullable private AdSelectionConfig mAdSelectionConfig;
        private String mCallerPackageName;

        public Builder() {}

        /** Set the mAdSelectionId. */
        @NonNull
        public ReportImpressionInput.Builder setAdSelectionId(long adSelectionId) {
            this.mAdSelectionId = adSelectionId;
            return this;
        }

        /** Set the AdSelectionConfig. */
        @NonNull
        public ReportImpressionInput.Builder setAdSelectionConfig(
                @NonNull AdSelectionConfig adSelectionConfig) {
            Objects.requireNonNull(adSelectionConfig);

            this.mAdSelectionConfig = adSelectionConfig;
            return this;
        }

        /** Sets the caller's package name. */
        @NonNull
        public ReportImpressionInput.Builder setCallerPackageName(
                @NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);

            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /** Builds a {@link ReportImpressionInput} instance. */
        @NonNull
        public ReportImpressionInput build() {
            Objects.requireNonNull(mAdSelectionConfig);
            Objects.requireNonNull(mCallerPackageName);

            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

            return new ReportImpressionInput(
                    mAdSelectionId, mAdSelectionConfig, mCallerPackageName);
        }
    }
}
