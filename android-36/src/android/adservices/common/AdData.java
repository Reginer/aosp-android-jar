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
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;
import com.android.internal.util.Preconditions;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Represents data specific to an ad that is necessary for ad selection and rendering. */
public final class AdData implements Parcelable {
    /** @hide */
    public static final String NUM_AD_COUNTER_KEYS_EXCEEDED_FORMAT =
            "AdData should have no more than %d ad counter keys";
    /** @hide */
    public static final int MAX_NUM_AD_COUNTER_KEYS = 10;

    @NonNull private final Uri mRenderUri;
    @NonNull private final String mMetadata;
    @NonNull private final Set<Integer> mAdCounterKeys;
    @Nullable private final AdFilters mAdFilters;
    @Nullable private final String mAdRenderId;

    @NonNull
    public static final Creator<AdData> CREATOR =
            new Creator<AdData>() {
                @Override
                public AdData createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);

                    return new AdData(in);
                }

                @Override
                public AdData[] newArray(int size) {
                    return new AdData[size];
                }
            };

    private AdData(@NonNull AdData.Builder builder) {
        Objects.requireNonNull(builder);

        mRenderUri = builder.mRenderUri;
        mMetadata = builder.mMetadata;
        mAdCounterKeys = builder.mAdCounterKeys;
        mAdFilters = builder.mAdFilters;
        mAdRenderId = builder.mAdRenderId;
    }

    private AdData(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mRenderUri = Uri.CREATOR.createFromParcel(in);
        mMetadata = in.readString();
        mAdCounterKeys =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, AdServicesParcelableUtil::readIntegerSetFromParcel);
        mAdFilters =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, AdFilters.CREATOR::createFromParcel);
        mAdRenderId = in.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        mRenderUri.writeToParcel(dest, flags);
        dest.writeString(mMetadata);
        AdServicesParcelableUtil.writeNullableToParcel(
                dest, mAdCounterKeys, AdServicesParcelableUtil::writeIntegerSetToParcel);
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mAdFilters,
                (targetParcel, sourceFilters) -> sourceFilters.writeToParcel(targetParcel, flags));
        dest.writeString(mAdRenderId);
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Gets the URI that points to the ad's rendering assets. The URI must use HTTPS. */
    @NonNull
    public Uri getRenderUri() {
        return mRenderUri;
    }

    /**
     * Gets the buyer ad metadata used during the ad selection process.
     *
     * <p>The metadata should be a valid JSON object serialized as a string. Metadata represents
     * ad-specific bidding information that will be used during ad selection as part of bid
     * generation and used in buyer JavaScript logic, which is executed in an isolated execution
     * environment.
     *
     * <p>If the metadata is not a valid JSON object that can be consumed by the buyer's JS, the ad
     * will not be eligible for ad selection.
     */
    @NonNull
    public String getMetadata() {
        return mMetadata;
    }

    /**
     * Gets the set of keys used in counting events.
     *
     * <p>No more than 10 ad counter keys may be associated with an ad.
     *
     * <p>The keys and counts per key are used in frequency cap filtering during ad selection to
     * disqualify associated ads from being submitted to bidding.
     *
     * <p>Note that these keys can be overwritten along with the ads and other bidding data for a
     * custom audience during the custom audience's daily update.
     */
    @NonNull
    public Set<Integer> getAdCounterKeys() {
        return mAdCounterKeys;
    }

    /**
     * Gets all {@link AdFilters} associated with the ad.
     *
     * <p>The filters, if met or exceeded, exclude the associated ad from participating in ad
     * selection. They are optional and if {@code null} specify that no filters apply to this ad.
     */
    @Nullable
    public AdFilters getAdFilters() {
        return mAdFilters;
    }

    /**
     * Gets the ad render id for server auctions.
     *
     * <p>Ad render id is collected for each {@link AdData} when server auction request is received.
     *
     * <p>Any {@link AdData} without ad render id will be ineligible for server-side auction.
     *
     * <p>The overall size of the CA is limited. The size of this field is considered using
     * {@link String#getBytes()} in {@code UTF-8} encoding.
     */
    @Nullable
    public String getAdRenderId() {
        return mAdRenderId;
    }

    /** Checks whether two {@link AdData} objects contain the same information. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdData)) return false;
        AdData adData = (AdData) o;
        return mRenderUri.equals(adData.mRenderUri)
                && mMetadata.equals(adData.mMetadata)
                && mAdCounterKeys.equals(adData.mAdCounterKeys)
                && Objects.equals(mAdFilters, adData.mAdFilters)
                && Objects.equals(mAdRenderId, adData.mAdRenderId);
    }

    /** Returns the hash of the {@link AdData} object's data. */
    @Override
    public int hashCode() {
        return Objects.hash(mRenderUri, mMetadata, mAdCounterKeys, mAdFilters);
    }

    @Override
    public String toString() {
        return "AdData{"
                + "mRenderUri="
                + mRenderUri
                + ", mMetadata='"
                + mMetadata
                + '\''
                + ", mAdCounterKeys="
                + mAdCounterKeys
                + ", mAdFilters="
                + mAdFilters
                + ", mAdRenderId='"
                + mAdRenderId
                + '\''
                + '}';
    }

    /** Builder for {@link AdData} objects. */
    public static final class Builder {
        @Nullable private Uri mRenderUri;
        @Nullable private String mMetadata;
        @NonNull private Set<Integer> mAdCounterKeys = new HashSet<>();
        @Nullable private AdFilters mAdFilters;
        @Nullable private String mAdRenderId;

        // TODO(b/232883403): We may need to add @NonNUll members as args.
        public Builder() {}

        /**
         * Sets the URI that points to the ad's rendering assets. The URI must use HTTPS.
         *
         * <p>See {@link #getRenderUri()} for detail.
         */
        @NonNull
        public AdData.Builder setRenderUri(@NonNull Uri renderUri) {
            Objects.requireNonNull(renderUri);
            mRenderUri = renderUri;
            return this;
        }

        /**
         * Sets the buyer ad metadata used during the ad selection process.
         *
         * <p>The metadata should be a valid JSON object serialized as a string. Metadata represents
         * ad-specific bidding information that will be used during ad selection as part of bid
         * generation and used in buyer JavaScript logic, which is executed in an isolated execution
         * environment.
         *
         * <p>If the metadata is not a valid JSON object that can be consumed by the buyer's JS, the
         * ad will not be eligible for ad selection.
         *
         * <p>See {@link #getMetadata()} for detail.
         */
        @NonNull
        public AdData.Builder setMetadata(@NonNull String metadata) {
            Objects.requireNonNull(metadata);
            mMetadata = metadata;
            return this;
        }

        /**
         * Sets the set of keys used in counting events.
         *
         * <p>No more than 10 ad counter keys may be associated with an ad.
         *
         * <p>See {@link #getAdCounterKeys()} for more information.
         */
        @NonNull
        public AdData.Builder setAdCounterKeys(@NonNull Set<Integer> adCounterKeys) {
            Objects.requireNonNull(adCounterKeys);
            Preconditions.checkArgument(
                    !adCounterKeys.contains(null), "Ad counter keys must not contain null value");
            Preconditions.checkArgument(
                    adCounterKeys.size() <= MAX_NUM_AD_COUNTER_KEYS,
                    NUM_AD_COUNTER_KEYS_EXCEEDED_FORMAT,
                    MAX_NUM_AD_COUNTER_KEYS);
            mAdCounterKeys = adCounterKeys;
            return this;
        }

        /**
         * Sets all {@link AdFilters} associated with the ad.
         *
         * <p>See {@link #getAdFilters()} for more information.
         */
        @NonNull
        public AdData.Builder setAdFilters(@Nullable AdFilters adFilters) {
            mAdFilters = adFilters;
            return this;
        }

        /**
         * Sets the ad render id for server auction
         *
         * <p>See {@link AdData#getAdRenderId()} for more information.
         */
        @NonNull
        public AdData.Builder setAdRenderId(@Nullable String adRenderId) {
            mAdRenderId = adRenderId;
            return this;
        }

        /**
         * Builds the {@link AdData} object.
         *
         * @throws NullPointerException if any required parameters are {@code null} when built
         */
        @NonNull
        public AdData build() {
            Objects.requireNonNull(mRenderUri, "The render URI has not been provided");
            // TODO(b/231997523): Add JSON field validation.
            Objects.requireNonNull(mMetadata, "The metadata has not been provided");

            return new AdData(this);
        }
    }
}
