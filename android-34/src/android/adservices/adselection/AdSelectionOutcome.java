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
import android.net.Uri;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * This class represents a field in the {@code OutcomeReceiver}, which is an input to the {@link
 * AdSelectionManager#selectAds} in the {@link AdSelectionManager}. This field is populated in the
 * case of a successful {@link AdSelectionManager#selectAds} call.
 *
 */
public class AdSelectionOutcome {
    /**
     * Represents an AdSelectionOutcome with empty results.
     *
     * @hide
     */
    @NonNull public static final AdSelectionOutcome NO_OUTCOME = new AdSelectionOutcome();

    /** @hide */
    public static final String UNSET_AD_SELECTION_ID_MESSAGE = "Ad selection ID must be set";

    /** @hide */
    public static final int UNSET_AD_SELECTION_ID = 0;

    private final long mAdSelectionId;
    @NonNull private final Uri mRenderUri;

    private AdSelectionOutcome() {
        mAdSelectionId = UNSET_AD_SELECTION_ID;
        mRenderUri = Uri.EMPTY;
    }

    private AdSelectionOutcome(long adSelectionId, @NonNull Uri renderUri) {
        Objects.requireNonNull(renderUri);

        mAdSelectionId = adSelectionId;
        mRenderUri = renderUri;
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
     * Returns whether the outcome contains results or empty. Empty outcomes' {@code render uris}
     * shouldn't be used.
     *
     * @hide
     */
    public boolean hasOutcome() {
        return !this.equals(NO_OUTCOME);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AdSelectionOutcome) {
            AdSelectionOutcome adSelectionOutcome = (AdSelectionOutcome) o;
            return mAdSelectionId == adSelectionOutcome.mAdSelectionId
                    && Objects.equals(mRenderUri, adSelectionOutcome.mRenderUri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdSelectionId, mRenderUri);
    }

    /**
     * Builder for {@link AdSelectionOutcome} objects.
     */
    public static final class Builder {
        private long mAdSelectionId = UNSET_AD_SELECTION_ID;
        @Nullable private Uri mRenderUri;

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
            Objects.requireNonNull(renderUri);

            mRenderUri = renderUri;
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

            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

            return new AdSelectionOutcome(mAdSelectionId, mRenderUri);
        }
    }
}
