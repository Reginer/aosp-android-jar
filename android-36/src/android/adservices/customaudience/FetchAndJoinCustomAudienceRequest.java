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

package android.adservices.customaudience;

import android.adservices.common.AdSelectionSignals;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;

import java.time.Instant;
import java.util.Objects;

/**
 * The request object wrapping the required and optional parameters needed to fetch a {@link
 * CustomAudience}.
 *
 * <p>{@code fetchUri} is the only required parameter. It represents the URI to fetch a custom
 * audience from. {@code name}, {@code activationTime}, {@code expirationTime} and {@code
 * userBiddingSignals} are optional parameters. They represent a partial custom audience which can
 * be used by the caller to inform the choice of the custom audience the user should be added to.
 * Any field set by the caller cannot be overridden by the custom audience fetched from the {@code
 * fetchUri}. For more information about each field refer to {@link CustomAudience}.
 */
public final class FetchAndJoinCustomAudienceRequest {
    @NonNull private final Uri mFetchUri;
    @Nullable private final String mName;
    @Nullable private final Instant mActivationTime;
    @Nullable private final Instant mExpirationTime;
    @Nullable private final AdSelectionSignals mUserBiddingSignals;

    private FetchAndJoinCustomAudienceRequest(
            @NonNull FetchAndJoinCustomAudienceRequest.Builder builder) {
        Objects.requireNonNull(builder.mFetchUri);

        mFetchUri = builder.mFetchUri;
        mName = builder.mName;
        mActivationTime = builder.mActivationTime;
        mExpirationTime = builder.mExpirationTime;
        mUserBiddingSignals = builder.mUserBiddingSignals;
    }

    /**
     * @return the {@link Uri} from which the custom audience is to be fetched.
     */
    @NonNull
    public Uri getFetchUri() {
        return mFetchUri;
    }

    /**
     * Reference {@link CustomAudience#getName()} for details.
     *
     * @return the {@link String} name of the custom audience to join.
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Reference {@link CustomAudience#getActivationTime()} for details.
     *
     * @return the {@link Instant} by which joining the custom audience will be delayed.
     */
    @Nullable
    public Instant getActivationTime() {
        return mActivationTime;
    }

    /**
     * Reference {@link CustomAudience#getExpirationTime()} for details.
     *
     * @return the {@link Instant} by when the membership to the custom audience will expire.
     */
    @Nullable
    public Instant getExpirationTime() {
        return mExpirationTime;
    }

    /**
     * Reference {@link CustomAudience#getUserBiddingSignals()} for details.
     *
     * @return the buyer signals to be consumed by the buyer-provided JavaScript when the custom
     *     audience participates in an ad selection.
     */
    @Nullable
    public AdSelectionSignals getUserBiddingSignals() {
        return mUserBiddingSignals;
    }

    /**
     * @return {@code true} only if two {@link FetchAndJoinCustomAudienceRequest} objects contain
     *     the same information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FetchAndJoinCustomAudienceRequest)) return false;
        FetchAndJoinCustomAudienceRequest that = (FetchAndJoinCustomAudienceRequest) o;
        return mFetchUri.equals(that.mFetchUri)
                && Objects.equals(mName, that.mName)
                && Objects.equals(mActivationTime, that.mActivationTime)
                && Objects.equals(mExpirationTime, that.mExpirationTime)
                && Objects.equals(mUserBiddingSignals, that.mUserBiddingSignals);
    }

    /**
     * @return the hash of the {@link FetchAndJoinCustomAudienceRequest} object's data.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                mFetchUri, mName, mActivationTime, mExpirationTime, mUserBiddingSignals);
    }

    /**
     * @return a human-readable representation of {@link FetchAndJoinCustomAudienceRequest}.
     */
    @Override
    public String toString() {
        return "FetchAndJoinCustomAudienceRequest{"
                + "fetchUri="
                + mFetchUri
                + ", name="
                + mName
                + ", activationTime="
                + mActivationTime
                + ", expirationTime="
                + mExpirationTime
                + ", userBiddingSignals="
                + mUserBiddingSignals
                + '}';
    }

    /** Builder for {@link FetchAndJoinCustomAudienceRequest} objects. */
    public static final class Builder {
        @NonNull private Uri mFetchUri;
        @Nullable private String mName;
        @Nullable private Instant mActivationTime;
        @Nullable private Instant mExpirationTime;
        @Nullable private AdSelectionSignals mUserBiddingSignals;

        /**
         * Instantiates a {@link FetchAndJoinCustomAudienceRequest.Builder} with the {@link Uri}
         * from which the custom audience is to be fetched.
         */
        public Builder(@NonNull Uri fetchUri) {
            Objects.requireNonNull(fetchUri);
            this.mFetchUri = fetchUri;
        }

        /**
         * Sets the {@link Uri} from which the custom audience is to be fetched.
         *
         * <p>See {@link #getFetchUri()} ()} for details.
         */
        @NonNull
        public Builder setFetchUri(@NonNull Uri fetchUri) {
            Objects.requireNonNull(fetchUri);
            this.mFetchUri = fetchUri;
            return this;
        }

        /**
         * Sets the {@link String} name of the custom audience to join.
         *
         * <p>See {@link #getName()} for details.
         */
        @NonNull
        public Builder setName(@Nullable String name) {
            this.mName = name;
            return this;
        }

        /**
         * Sets the {@link Instant} by which joining the custom audience will be delayed.
         *
         * <p>See {@link #getActivationTime()} for details.
         */
        @NonNull
        public Builder setActivationTime(@Nullable Instant activationTime) {
            this.mActivationTime = activationTime;
            return this;
        }

        /**
         * Sets the {@link Instant} by when the membership to the custom audience will expire.
         *
         * <p>See {@link #getExpirationTime()} for details.
         */
        @NonNull
        public Builder setExpirationTime(@Nullable Instant expirationTime) {
            this.mExpirationTime = expirationTime;
            return this;
        }

        /**
         * Sets the buyer signals to be consumed by the buyer-provided JavaScript when the custom
         * audience participates in an ad selection.
         *
         * <p>See {@link #getUserBiddingSignals()} for details.
         */
        @NonNull
        public Builder setUserBiddingSignals(@Nullable AdSelectionSignals userBiddingSignals) {
            this.mUserBiddingSignals = userBiddingSignals;
            return this;
        }

        /**
         * Builds an instance of a {@link FetchAndJoinCustomAudienceRequest}.
         *
         * @throws NullPointerException if any non-null parameter is null.
         */
        @NonNull
        public FetchAndJoinCustomAudienceRequest build() {
            Objects.requireNonNull(mFetchUri);
            return new FetchAndJoinCustomAudienceRequest(this);
        }
    }
}
