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

import android.adservices.common.AdTechIdentifier;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Objects;

/**
 * Contains Ads supplied by Seller for the Contextual Path
 *
 * <p>Instances of this class are created by SDKs to be injected as part of {@link
 * AdSelectionConfig} and passed to {@link AdSelectionManager#selectAds}
 *
 * @hide
 */
public final class ContextualAds implements Parcelable {
    @NonNull private final AdTechIdentifier mBuyer;
    @NonNull private final Uri mDecisionLogicUri;
    @NonNull private final List<AdWithBid> mAdsWithBid;

    @NonNull
    public static final Creator<ContextualAds> CREATOR =
            new Creator<ContextualAds>() {
                @Override
                public ContextualAds createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new ContextualAds(in);
                }

                @Override
                public ContextualAds[] newArray(int size) {
                    return new ContextualAds[0];
                }
            };

    private ContextualAds(
            @NonNull AdTechIdentifier buyer,
            @NonNull Uri decisionLogicUri,
            @NonNull List<AdWithBid> adsWithBid) {
        this.mBuyer = buyer;
        this.mDecisionLogicUri = decisionLogicUri;
        this.mAdsWithBid = adsWithBid;
    }

    private ContextualAds(@NonNull Parcel in) {
        Objects.requireNonNull(in);
        mBuyer = AdTechIdentifier.CREATOR.createFromParcel(in);
        mDecisionLogicUri = Uri.CREATOR.createFromParcel(in);
        mAdsWithBid = in.createTypedArrayList(AdWithBid.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        mBuyer.writeToParcel(dest, flags);
        mDecisionLogicUri.writeToParcel(dest, flags);
        dest.writeTypedList(mAdsWithBid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextualAds)) return false;
        ContextualAds that = (ContextualAds) o;
        return Objects.equals(mBuyer, that.mBuyer)
                && Objects.equals(mDecisionLogicUri, that.mDecisionLogicUri)
                && Objects.equals(mAdsWithBid, that.mAdsWithBid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBuyer, mDecisionLogicUri, mAdsWithBid);
    }

    /** @return the Ad tech identifier from which this contextual Ad would have been downloaded */
    @NonNull
    public AdTechIdentifier getBuyer() {
        return mBuyer;
    }

    /**
     * @return the URI used for to retrieve the updateBid() and reportWin() function used during the
     *     ad selection and reporting process
     */
    @NonNull
    public Uri getDecisionLogicUri() {
        return mDecisionLogicUri;
    }

    /** @return the Ad data with bid value associated with this ad */
    @NonNull
    public List<AdWithBid> getAdsWithBid() {
        return mAdsWithBid;
    }

    /** Builder for {@link ContextualAds} object */
    public static final class Builder {
        @Nullable private AdTechIdentifier mBuyer;
        @Nullable private Uri mDecisionLogicUri;
        @Nullable private List<AdWithBid> mAdsWithBid;

        public Builder() {}

        /**
         * Sets the buyer Ad tech Identifier
         *
         * <p>See {@link #getBuyer()} for more details
         */
        @NonNull
        public ContextualAds.Builder setBuyer(@NonNull AdTechIdentifier buyer) {
            Objects.requireNonNull(buyer);

            this.mBuyer = buyer;
            return this;
        }

        /**
         * Sets the URI to fetch the decision logic used in ad selection and reporting
         *
         * <p>See {@link #getDecisionLogicUri()} for more details
         */
        @NonNull
        public ContextualAds.Builder setDecisionLogicUri(@NonNull Uri decisionLogicUri) {
            Objects.requireNonNull(decisionLogicUri);

            this.mDecisionLogicUri = decisionLogicUri;
            return this;
        }

        /**
         * Sets the Ads with pre-defined bid values
         *
         * <p>See {@link #getAdsWithBid()} for more details
         */
        @NonNull
        public ContextualAds.Builder setAdsWithBid(@NonNull List<AdWithBid> adsWithBid) {
            Objects.requireNonNull(adsWithBid);

            this.mAdsWithBid = adsWithBid;
            return this;
        }

        /**
         * Builds a {@link ContextualAds} instance.
         *
         * @throws NullPointerException if any required params are null
         */
        @NonNull
        public ContextualAds build() {
            Objects.requireNonNull(mBuyer);
            Objects.requireNonNull(mDecisionLogicUri);
            Objects.requireNonNull(mAdsWithBid);
            return new ContextualAds(mBuyer, mDecisionLogicUri, mAdsWithBid);
        }
    }
}
