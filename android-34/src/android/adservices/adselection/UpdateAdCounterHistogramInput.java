/*
 * Copyright (C) 2023 The Android Open Source Project
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
import static android.adservices.adselection.UpdateAdCounterHistogramRequest.DISALLOW_AD_EVENT_TYPE_WIN_MESSAGE;
import static android.adservices.adselection.UpdateAdCounterHistogramRequest.UNSET_AD_EVENT_TYPE_MESSAGE;
import static android.adservices.adselection.UpdateAdCounterHistogramRequest.UNSET_CALLER_ADTECH_MESSAGE;
import static android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_INVALID;
import static android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_WIN;

import android.adservices.common.AdTechIdentifier;
import android.adservices.common.FrequencyCapFilters;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * Input object wrapping the required arguments needed to update an ad counter histogram.
 *
 * <p>The ad counter histograms, which are historical logs of events which are associated with an ad
 * counter key and an ad event type, are used to inform frequency cap filtering in FLEDGE.
 *
 * @hide
 */
public final class UpdateAdCounterHistogramInput implements Parcelable {
    private static final String UNSET_CALLER_PACKAGE_NAME_MESSAGE =
            "Caller package name must not be null";

    private final long mAdSelectionId;
    @FrequencyCapFilters.AdEventType private final int mAdEventType;
    @NonNull private final AdTechIdentifier mCallerAdTech;
    @NonNull private final String mCallerPackageName;

    @NonNull
    public static final Creator<UpdateAdCounterHistogramInput> CREATOR =
            new Creator<UpdateAdCounterHistogramInput>() {
                @Override
                public UpdateAdCounterHistogramInput createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);

                    return new UpdateAdCounterHistogramInput(in);
                }

                @Override
                public UpdateAdCounterHistogramInput[] newArray(int size) {
                    return new UpdateAdCounterHistogramInput[size];
                }
            };

    private UpdateAdCounterHistogramInput(@NonNull Builder builder) {
        Objects.requireNonNull(builder);

        mAdSelectionId = builder.mAdSelectionId;
        mAdEventType = builder.mAdEventType;
        mCallerAdTech = builder.mCallerAdTech;
        mCallerPackageName = builder.mCallerPackageName;
    }

    private UpdateAdCounterHistogramInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mAdSelectionId = in.readLong();
        mAdEventType = in.readInt();
        mCallerAdTech = AdTechIdentifier.CREATOR.createFromParcel(in);
        mCallerPackageName = in.readString();
    }

    /**
     * Gets the ad selection ID with which the rendered ad's events are associated.
     *
     * <p>The ad must have been selected from FLEDGE ad selection in the last 24 hours, and the ad
     * selection call must have been initiated from the same app as the current calling app. Event
     * histograms for all ad counter keys associated with the ad specified by the ad selection ID
     * will be updated for the ad event type from {@link #getAdEventType()}, to be used in FLEDGE
     * frequency cap filtering.
     */
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /**
     * Gets the {@link android.adservices.common.FrequencyCapFilters.AdEventType} which, along with
     * an ad's counter keys, identifies which histogram should be updated.
     *
     * <p>See {@link android.adservices.common.FrequencyCapFilters.AdEventType} for more
     * information.
     */
    @FrequencyCapFilters.AdEventType
    public int getAdEventType() {
        return mAdEventType;
    }

    /**
     * Gets the caller adtech entity's {@link AdTechIdentifier}.
     *
     * <p>The adtech using this {@link UpdateAdCounterHistogramInput} object must have enrolled with
     * the Privacy Sandbox and be allowed to act on behalf of the calling app. The specified adtech
     * is not required to be the same adtech as either the buyer which owns the rendered ad or the
     * seller which initiated the ad selection associated with the ID returned by {@link
     * #getAdSelectionId()}.
     */
    @NonNull
    public AdTechIdentifier getCallerAdTech() {
        return mCallerAdTech;
    }

    /**
     * Gets the caller app's package name.
     *
     * <p>The package name must match the caller package name for the FLEDGE ad selection
     * represented by the ID returned by {@link #getAdSelectionId()}.
     */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mAdSelectionId);
        dest.writeInt(mAdEventType);
        mCallerAdTech.writeToParcel(dest, flags);
        dest.writeString(mCallerPackageName);
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Checks whether the {@link UpdateAdCounterHistogramInput} objects contain the same
     * information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateAdCounterHistogramInput)) return false;
        UpdateAdCounterHistogramInput that = (UpdateAdCounterHistogramInput) o;
        return mAdSelectionId == that.mAdSelectionId
                && mAdEventType == that.mAdEventType
                && mCallerAdTech.equals(that.mCallerAdTech)
                && mCallerPackageName.equals(that.mCallerPackageName);
    }

    /** Returns the hash of the {@link UpdateAdCounterHistogramInput} object's data. */
    @Override
    public int hashCode() {
        return Objects.hash(mAdSelectionId, mAdEventType, mCallerAdTech, mCallerPackageName);
    }

    @Override
    public String toString() {
        return "UpdateAdCounterHistogramInput{"
                + "mAdSelectionId="
                + mAdSelectionId
                + ", mAdEventType="
                + mAdEventType
                + ", mCallerAdTech="
                + mCallerAdTech
                + ", mCallerPackageName='"
                + mCallerPackageName
                + '\''
                + '}';
    }

    /** Builder for {@link UpdateAdCounterHistogramInput} objects. */
    public static final class Builder {
        private long mAdSelectionId = UNSET_AD_SELECTION_ID;
        @FrequencyCapFilters.AdEventType private int mAdEventType = AD_EVENT_TYPE_INVALID;
        @Nullable private AdTechIdentifier mCallerAdTech;
        @Nullable private String mCallerPackageName;

        public Builder() {}

        /**
         * Gets the ad selection ID with which the rendered ad's events are associated.
         *
         * <p>See {@link #getAdSelectionId()} for more information.
         */
        @NonNull
        public Builder setAdSelectionId(long adSelectionId) {
            mAdSelectionId = adSelectionId;
            return this;
        }

        /**
         * Sets the {@link android.adservices.common.FrequencyCapFilters.AdEventType} which, along
         * with an ad's counter keys, identifies which histogram should be updated.
         *
         * <p>See {@link #getAdEventType()} for more information.
         */
        @NonNull
        public Builder setAdEventType(@FrequencyCapFilters.AdEventType int adEventType) {
            Preconditions.checkArgument(
                    adEventType != AD_EVENT_TYPE_WIN, DISALLOW_AD_EVENT_TYPE_WIN_MESSAGE);
            mAdEventType = adEventType;
            return this;
        }

        /**
         * Sets the caller adtech entity's {@link AdTechIdentifier}.
         *
         * <p>See {@link #getCallerAdTech()} for more information.
         */
        @NonNull
        public Builder setCallerAdTech(@NonNull AdTechIdentifier callerAdTech) {
            Objects.requireNonNull(callerAdTech, UNSET_CALLER_ADTECH_MESSAGE);
            mCallerAdTech = callerAdTech;
            return this;
        }

        /**
         * Sets the caller app's package name.
         *
         * <p>See {@link #getCallerPackageName()} for more information.
         */
        @NonNull
        public Builder setCallerPackageName(@NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName, UNSET_CALLER_PACKAGE_NAME_MESSAGE);
            mCallerPackageName = callerPackageName;
            return this;
        }

        /**
         * Builds the {@link UpdateAdCounterHistogramInput} object.
         *
         * @throws NullPointerException if the caller's {@link AdTechIdentifier} or package name are
         *     not set
         * @throws IllegalArgumentException if the ad selection ID is not set
         */
        @NonNull
        public UpdateAdCounterHistogramInput build()
                throws NullPointerException, IllegalArgumentException {
            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);
            Preconditions.checkArgument(
                    mAdEventType != AD_EVENT_TYPE_INVALID, UNSET_AD_EVENT_TYPE_MESSAGE);
            Objects.requireNonNull(mCallerAdTech, UNSET_CALLER_ADTECH_MESSAGE);
            Objects.requireNonNull(mCallerPackageName, UNSET_CALLER_PACKAGE_NAME_MESSAGE);

            return new UpdateAdCounterHistogramInput(this);
        }
    }
}
