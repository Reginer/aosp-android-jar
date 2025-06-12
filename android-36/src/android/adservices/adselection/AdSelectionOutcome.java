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
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.os.OutcomeReceiver;

import com.android.adservices.flags.Flags;
import com.android.internal.util.Preconditions;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * This class represents a field in the {@code OutcomeReceiver}, which is an input to the {@link
 * AdSelectionManager#selectAds} in the {@link AdSelectionManager}. This field is populated in the
 * case of a successful {@link AdSelectionManager#selectAds} call.
 *
 * <p>Empty outcome may be returned from {@link
 * AdSelectionManager#selectAds(AdSelectionFromOutcomesConfig, Executor, OutcomeReceiver)}. Use
 * {@link AdSelectionOutcome#hasOutcome()} to check if an instance has a valid outcome. When {@link
 * AdSelectionOutcome#hasOutcome()} returns {@code false}, results from {@link AdSelectionOutcome
 * #getAdSelectionId()} and {@link AdSelectionOutcome#getRenderUri()} are invalid and shouldn't be
 * used.
 */
public class AdSelectionOutcome {
    /** Represents an AdSelectionOutcome with empty results. */
    @NonNull public static final AdSelectionOutcome NO_OUTCOME = new AdSelectionOutcome();

    /** @hide */
    public static final String UNSET_AD_SELECTION_ID_MESSAGE =
            "Non-zero ad selection ID must be set";

    /** @hide */
    public static final int UNSET_AD_SELECTION_ID = 0;

    private final long mAdSelectionId;
    @NonNull private final Uri mRenderUri;
    @NonNull private final AdTechIdentifier mWinningSeller;
    @NonNull private final List<Uri> mComponentAdUris;

    private AdSelectionOutcome() {
        mAdSelectionId = UNSET_AD_SELECTION_ID;
        mRenderUri = Uri.EMPTY;
        mWinningSeller = AdTechIdentifier.UNSET_AD_TECH_IDENTIFIER;
        mComponentAdUris = List.of();
    }

    private AdSelectionOutcome(
            long adSelectionId,
            @NonNull Uri renderUri,
            @NonNull AdTechIdentifier winningSeller,
            @NonNull List<Uri> componentAdUris) {
        mAdSelectionId = adSelectionId;
        mRenderUri = Objects.requireNonNull(renderUri, "Render uri cannot be null");
        mWinningSeller = Objects.requireNonNull(winningSeller, "Winning seller cannot be null");
        mComponentAdUris =
                Objects.requireNonNull(componentAdUris, "Component ad uris cannot be null");
    }

    /** Returns the renderUri that the AdSelection returns. */
    @NonNull
    public Uri getRenderUri() {
        return mRenderUri;
    }

    /** Returns the adSelectionId that identifies the AdSelection. */
    @NonNull
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /**
     * Returns the willing seller that won the auction.
     *
     */
    @FlaggedApi(Flags.FLAG_FLEDGE_ENABLE_WINNING_SELLER_ID_IN_AD_SELECTION_OUTCOME)
    @NonNull
    public AdTechIdentifier getWinningSeller() {
        return mWinningSeller;
    }

    /**
     * Returns the ad component renderUris that are returned by this auction.
     *
     * <p>These URIs represent individual components of a multi-part ad creative. Seller ad tech
     * SDKs are expected to fetch and render these components in their ad display logic.
     *
     * <p>This approach allows for flexible and dynamic ad creatives where different parts of the ad
     * can be loaded and rendered independently.
     */
    @FlaggedApi(Flags.FLAG_FLEDGE_ENABLE_CUSTOM_AUDIENCE_COMPONENT_ADS)
    @NonNull
    public List<Uri> getComponentAdUris() {
        return mComponentAdUris;
    }

    /**
     * Returns whether the outcome contains results or empty. Empty outcomes' {@code render uris}
     * shouldn't be used.
     */
    public boolean hasOutcome() {
        return !this.equals(NO_OUTCOME);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AdSelectionOutcome) {
            AdSelectionOutcome adSelectionOutcome = (AdSelectionOutcome) o;
            return mAdSelectionId == adSelectionOutcome.mAdSelectionId
                    && Objects.equals(mRenderUri, adSelectionOutcome.mRenderUri)
                    && Objects.equals(mWinningSeller, adSelectionOutcome.mWinningSeller)
                    && Objects.equals(mComponentAdUris, adSelectionOutcome.mComponentAdUris);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdSelectionId, mRenderUri, mWinningSeller, mComponentAdUris);
    }

    /**
     * Builder for {@link AdSelectionOutcome} objects.
     */
    public static final class Builder {
        private long mAdSelectionId = UNSET_AD_SELECTION_ID;
        @Nullable private Uri mRenderUri;
        @NonNull private List<Uri> mComponentAdUris;

        @NonNull
        private AdTechIdentifier mWinningSeller = AdTechIdentifier.UNSET_AD_TECH_IDENTIFIER;

        public Builder() {}

        /** Sets the mAdSelectionId. */
        @NonNull
        public AdSelectionOutcome.Builder setAdSelectionId(long adSelectionId) {
            this.mAdSelectionId = adSelectionId;
            return this;
        }

        /** Sets the RenderUri. */
        @NonNull
        public AdSelectionOutcome.Builder setRenderUri(@NonNull Uri renderUri) {
            mRenderUri = Objects.requireNonNull(renderUri, "Render uri cannot be null");
            return this;
        }

        /**
         * Sets the winning seller.
         *
         * @hide
         */
        @NonNull
        public AdSelectionOutcome.Builder setWinningSeller(
                @NonNull AdTechIdentifier winningSeller) {
            mWinningSeller = Objects.requireNonNull(winningSeller, "Winning seller cannot be null");
            return this;
        }

        /**
         * Sets the list of ad component renderUris.
         */
        @FlaggedApi(Flags.FLAG_FLEDGE_ENABLE_CUSTOM_AUDIENCE_COMPONENT_ADS)
        @NonNull
        public AdSelectionOutcome.Builder setComponentAdUris(@NonNull List<Uri> componentAdUris) {
            mComponentAdUris =
                    Objects.requireNonNull(componentAdUris, "Component ad Uris cannot be null!");
            return this;
        }

        /**
         * Builds a {@link AdSelectionOutcome} instance.
         *
         * @throws IllegalArgumentException if the adSelectionIid is not set
         * @throws NullPointerException if the RenderUri is null
         */
        @NonNull
        public AdSelectionOutcome build() {
            Objects.requireNonNull(mRenderUri);
            Objects.requireNonNull(mWinningSeller);
            if (Objects.isNull(mComponentAdUris)) {
                mComponentAdUris = List.of();
            }

            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

            return new AdSelectionOutcome(
                    mAdSelectionId, mRenderUri, mWinningSeller, mComponentAdUris);
        }
    }
}
