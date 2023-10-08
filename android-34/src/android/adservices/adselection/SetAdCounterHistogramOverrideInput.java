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

import static android.adservices.adselection.SetAdCounterHistogramOverrideRequest.NULL_AD_COUNTER_KEY_MESSAGE;
import static android.adservices.adselection.SetAdCounterHistogramOverrideRequest.NULL_BUYER_MESSAGE;
import static android.adservices.adselection.SetAdCounterHistogramOverrideRequest.NULL_CUSTOM_AUDIENCE_NAME_MESSAGE;
import static android.adservices.adselection.SetAdCounterHistogramOverrideRequest.NULL_CUSTOM_AUDIENCE_OWNER_MESSAGE;
import static android.adservices.adselection.SetAdCounterHistogramOverrideRequest.NULL_HISTOGRAM_TIMESTAMPS_MESSAGE;
import static android.adservices.adselection.UpdateAdCounterHistogramRequest.UNSET_AD_EVENT_TYPE_MESSAGE;
import static android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_INVALID;

import android.adservices.common.AdTechIdentifier;
import android.adservices.common.FrequencyCapFilters;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;
import com.android.internal.util.Preconditions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Input object for setting ad counter histogram overrides.
 *
 * <p>Histogram overrides replace actual ad counter histograms used in ad selection. Overrides may
 * only be set in debuggable apps on phones running a debuggable OS build with developer options
 * enabled. Overrides are only available from the calling app.
 *
 * @hide
 */
public final class SetAdCounterHistogramOverrideInput implements Parcelable {
    @FrequencyCapFilters.AdEventType private final int mAdEventType;
    @NonNull private final String mAdCounterKey;
    @NonNull private final List<Instant> mHistogramTimestamps;
    @NonNull private final AdTechIdentifier mBuyer;
    @NonNull private final String mCustomAudienceOwner;
    @NonNull private final String mCustomAudienceName;

    @NonNull
    public static final Creator<SetAdCounterHistogramOverrideInput> CREATOR =
            new Creator<SetAdCounterHistogramOverrideInput>() {
                @Override
                public SetAdCounterHistogramOverrideInput createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);

                    return new SetAdCounterHistogramOverrideInput(in);
                }

                @Override
                public SetAdCounterHistogramOverrideInput[] newArray(int size) {
                    return new SetAdCounterHistogramOverrideInput[size];
                }
            };

    private SetAdCounterHistogramOverrideInput(@NonNull Builder builder) {
        Objects.requireNonNull(builder);

        mAdEventType = builder.mAdEventType;
        mAdCounterKey = builder.mAdCounterKey;
        mHistogramTimestamps = builder.mHistogramTimestamps;
        mBuyer = builder.mBuyer;
        mCustomAudienceOwner = builder.mCustomAudienceOwner;
        mCustomAudienceName = builder.mCustomAudienceName;
    }

    private SetAdCounterHistogramOverrideInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mAdEventType = in.readInt();
        mAdCounterKey = in.readString();
        mHistogramTimestamps = AdServicesParcelableUtil.readInstantListFromParcel(in);
        mBuyer = AdTechIdentifier.fromString(in.readString());
        mCustomAudienceOwner = in.readString();
        mCustomAudienceName = in.readString();
    }

    /**
     * Gets the {@link FrequencyCapFilters.AdEventType} for the ad counter histogram override.
     *
     * <p>The ad event type is used with the ad counter key from {@link #getAdCounterKey()} and the
     * buyer adtech from {@link #getBuyer()} to specify which histogram to use in ad selection
     * filtering. The ad event type would normally be specified by an app/SDK after a
     * FLEDGE-selected ad is rendered.
     */
    @FrequencyCapFilters.AdEventType
    public int getAdEventType() {
        return mAdEventType;
    }

    /**
     * Gets the ad counter key for the ad counter histogram override.
     *
     * <p>The ad counter key is used with the ad event type from {@link #getAdEventType()} and the
     * buyer adtech from {@link #getBuyer()} to specify which histogram to use in ad selection
     * filtering. The ad counter key would normally be specified by a custom audience ad to
     * represent a grouping to filter on.
     */
    @NonNull
    public String getAdCounterKey() {
        return mAdCounterKey;
    }

    /**
     * Gets the list of {@link Instant} objects for the ad counter histogram override.
     *
     * <p>When set, this list of timestamps is used to populate the override histogram, which is
     * used instead of actual histograms for ad selection filtering.
     */
    @NonNull
    public List<Instant> getHistogramTimestamps() {
        return mHistogramTimestamps;
    }

    /**
     * Gets the {@link AdTechIdentifier} for the buyer which owns the ad counter histogram.
     *
     * <p>During filtering in FLEDGE ad selection, ads can only use ad counter histogram data
     * generated by the same buyer. For {@link FrequencyCapFilters#AD_EVENT_TYPE_WIN}, ad counter
     * histogram data is further restricted to ads from the same custom audience, which is
     * identified by the buyer, the custom audience's owner app package name from {@link
     * #getCustomAudienceOwner()}, and the custom audience name from {@link
     * #getCustomAudienceName()}.
     */
    @NonNull
    public AdTechIdentifier getBuyer() {
        return mBuyer;
    }

    /**
     * Gets the package name for the app which generated the custom audience which is associated
     * with the overridden ad counter histogram data.
     *
     * <p>For {@link FrequencyCapFilters#AD_EVENT_TYPE_WIN}, ad counter histogram data is restricted
     * to ads from the same custom audience, which is identified by the buyer from {@link
     * #getBuyer()}, the custom audience's owner app package name, and the custom audience name from
     * {@link #getCustomAudienceName()}.
     */
    @NonNull
    public String getCustomAudienceOwner() {
        return mCustomAudienceOwner;
    }

    /**
     * Gets the buyer-generated name for the custom audience which is associated with the overridden
     * ad counter histogram data.
     *
     * <p>For {@link FrequencyCapFilters#AD_EVENT_TYPE_WIN}, ad counter histogram data is restricted
     * to ads from the same custom audience, which is identified by the buyer from {@link
     * #getBuyer()}, the custom audience's owner app package name from {@link
     * #getCustomAudienceOwner()}, and the custom audience name.
     */
    @NonNull
    public String getCustomAudienceName() {
        return mCustomAudienceName;
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "SetAdCounterHistogramOverrideInput{"
                + "mAdEventType="
                + mAdEventType
                + ", mAdCounterKey='"
                + mAdCounterKey
                + "', mHistogramTimestamps="
                + mHistogramTimestamps
                + ", mBuyer="
                + mBuyer
                + ", mCustomAudienceOwner='"
                + mCustomAudienceOwner
                + "', mCustomAudienceName='"
                + mCustomAudienceName
                + "'}";
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        dest.writeInt(mAdEventType);
        dest.writeString(mAdCounterKey);
        AdServicesParcelableUtil.writeInstantListToParcel(dest, mHistogramTimestamps);
        dest.writeString(mBuyer.toString());
        dest.writeString(mCustomAudienceOwner);
        dest.writeString(mCustomAudienceName);
    }

    /** Builder for {@link SetAdCounterHistogramOverrideInput} objects. */
    public static final class Builder {
        @FrequencyCapFilters.AdEventType private int mAdEventType = AD_EVENT_TYPE_INVALID;
        @Nullable private String mAdCounterKey;
        @NonNull private List<Instant> mHistogramTimestamps = new ArrayList<>();
        @Nullable private AdTechIdentifier mBuyer;
        @Nullable private String mCustomAudienceOwner;
        @Nullable private String mCustomAudienceName;

        public Builder() {}

        /**
         * Sets the {@link FrequencyCapFilters.AdEventType} for the ad counter histogram override.
         *
         * <p>See {@link #getAdEventType()} for more information.
         */
        @NonNull
        public Builder setAdEventType(@FrequencyCapFilters.AdEventType int adEventType) {
            mAdEventType = adEventType;
            return this;
        }

        /**
         * Sets the ad counter key for the ad counter histogram override.
         *
         * <p>See {@link #getAdCounterKey()} for more information.
         */
        @NonNull
        public Builder setAdCounterKey(@NonNull String adCounterKey) {
            Objects.requireNonNull(adCounterKey, NULL_AD_COUNTER_KEY_MESSAGE);
            mAdCounterKey = adCounterKey;
            return this;
        }

        /**
         * Sets the list of {@link Instant} objects for the ad counter histogram override.
         *
         * <p>See {@link #getHistogramTimestamps()} for more information.
         */
        @NonNull
        public Builder setHistogramTimestamps(@NonNull List<Instant> histogramTimestamps) {
            Objects.requireNonNull(histogramTimestamps, NULL_HISTOGRAM_TIMESTAMPS_MESSAGE);
            mHistogramTimestamps = histogramTimestamps;
            return this;
        }

        /**
         * Sets the {@link AdTechIdentifier} for the buyer which owns the ad counter histogram.
         *
         * <p>See {@link #getBuyer()} for more information.
         */
        @NonNull
        public Builder setBuyer(@NonNull AdTechIdentifier buyer) {
            Objects.requireNonNull(buyer, NULL_BUYER_MESSAGE);
            mBuyer = buyer;
            return this;
        }

        /**
         * Sets the package name for the app which generated the custom audience which is associated
         * with the overridden ad counter histogram data.
         *
         * <p>See {@link #getCustomAudienceOwner()} for more information.
         */
        @NonNull
        public Builder setCustomAudienceOwner(@NonNull String customAudienceOwner) {
            Objects.requireNonNull(customAudienceOwner, NULL_CUSTOM_AUDIENCE_OWNER_MESSAGE);
            mCustomAudienceOwner = customAudienceOwner;
            return this;
        }

        /**
         * Sets the buyer-generated name for the custom audience which is associated with the
         * overridden ad counter histogram data.
         *
         * <p>See {@link #getCustomAudienceName()} for more information.
         */
        @NonNull
        public Builder setCustomAudienceName(@NonNull String customAudienceName) {
            Objects.requireNonNull(customAudienceName, NULL_CUSTOM_AUDIENCE_NAME_MESSAGE);
            mCustomAudienceName = customAudienceName;
            return this;
        }

        /**
         * Builds the {@link SetAdCounterHistogramOverrideInput} object.
         *
         * @throws NullPointerException if any parameters are not set
         * @throws IllegalArgumentException if the ad event type is invalid
         */
        @NonNull
        public SetAdCounterHistogramOverrideInput build()
                throws NullPointerException, IllegalArgumentException {
            Preconditions.checkArgument(
                    mAdEventType != AD_EVENT_TYPE_INVALID, UNSET_AD_EVENT_TYPE_MESSAGE);
            Objects.requireNonNull(mAdCounterKey, NULL_AD_COUNTER_KEY_MESSAGE);
            Objects.requireNonNull(mBuyer, NULL_BUYER_MESSAGE);
            Objects.requireNonNull(mCustomAudienceOwner, NULL_CUSTOM_AUDIENCE_OWNER_MESSAGE);
            Objects.requireNonNull(mCustomAudienceName, NULL_CUSTOM_AUDIENCE_NAME_MESSAGE);

            return new SetAdCounterHistogramOverrideInput(this);
        }
    }
}
