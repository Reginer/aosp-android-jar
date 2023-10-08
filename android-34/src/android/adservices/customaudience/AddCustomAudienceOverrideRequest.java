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

package android.adservices.customaudience;

import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.OutcomeReceiver;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * This POJO represents the {@link
 * TestCustomAudienceManager#overrideCustomAudienceRemoteInfo(AddCustomAudienceOverrideRequest,
 * Executor, OutcomeReceiver)} request.
 *
 * <p>It contains fields {@code buyer} and {@code name} which will serve as the identifier for the
 * override fields, {@code biddingLogicJs} and {@code trustedBiddingSignals}, which are used during
 * ad selection instead of querying external servers.
 */
public class AddCustomAudienceOverrideRequest {
    @NonNull private final AdTechIdentifier mBuyer;
    @NonNull private final String mName;
    @NonNull private final String mBiddingLogicJs;
    private final long mBiddingLogicJsVersion;
    @NonNull private final AdSelectionSignals mTrustedBiddingSignals;

    public AddCustomAudienceOverrideRequest(
            @NonNull AdTechIdentifier buyer,
            @NonNull String name,
            @NonNull String biddingLogicJs,
            @NonNull AdSelectionSignals trustedBiddingSignals) {
        this(buyer, name, biddingLogicJs, 0L, trustedBiddingSignals);
    }

    private AddCustomAudienceOverrideRequest(
            @NonNull AdTechIdentifier buyer,
            @NonNull String name,
            @NonNull String biddingLogicJs,
            long biddingLogicJsVersion,
            @NonNull AdSelectionSignals trustedBiddingSignals) {
        mBuyer = buyer;
        mName = name;
        mBiddingLogicJs = biddingLogicJs;
        mBiddingLogicJsVersion = biddingLogicJsVersion;
        mTrustedBiddingSignals = trustedBiddingSignals;
    }

    /** @return an {@link AdTechIdentifier} representing the buyer */
    @NonNull
    public AdTechIdentifier getBuyer() {
        return mBuyer;
    }

    /** @return name of the custom audience being overridden */
    @NonNull
    public String getName() {
        return mName;
    }

    /** @return the override JavaScript result that should be served during ad selection */
    @NonNull
    public String getBiddingLogicJs() {
        return mBiddingLogicJs;
    }

    /**
     * Returns the override bidding logic JavaScript version.
     *
     * <p>Default to be {@code 0L}, which will fall back to use default version(V1 or V2).
     *
     * @hide
     */
    public long getBiddingLogicJsVersion() {
        return mBiddingLogicJsVersion;
    }

    /** @return the override trusted bidding signals that should be served during ad selection */
    @NonNull
    public AdSelectionSignals getTrustedBiddingSignals() {
        return mTrustedBiddingSignals;
    }

    /** Builder for {@link AddCustomAudienceOverrideRequest} objects. */
    public static final class Builder {
        @Nullable private AdTechIdentifier mBuyer;
        @Nullable private String mName;
        @Nullable private String mBiddingLogicJs;
        private long mBiddingLogicJsVersion;
        @Nullable private AdSelectionSignals mTrustedBiddingSignals;

        public Builder() {}

        /** Sets the buyer {@link AdTechIdentifier} for the custom audience. */
        @NonNull
        public AddCustomAudienceOverrideRequest.Builder setBuyer(@NonNull AdTechIdentifier buyer) {
            Objects.requireNonNull(buyer);

            this.mBuyer = buyer;
            return this;
        }

        /** Sets the name for the custom audience to be overridden. */
        @NonNull
        public AddCustomAudienceOverrideRequest.Builder setName(@NonNull String name) {
            Objects.requireNonNull(name);

            this.mName = name;
            return this;
        }

        /** Sets the trusted bidding signals to be served during ad selection. */
        @NonNull
        public AddCustomAudienceOverrideRequest.Builder setTrustedBiddingSignals(
                @NonNull AdSelectionSignals trustedBiddingSignals) {
            Objects.requireNonNull(trustedBiddingSignals);

            this.mTrustedBiddingSignals = trustedBiddingSignals;
            return this;
        }

        /** Sets the bidding logic JavaScript that should be served during ad selection. */
        @NonNull
        public AddCustomAudienceOverrideRequest.Builder setBiddingLogicJs(
                @NonNull String biddingLogicJs) {
            Objects.requireNonNull(biddingLogicJs);

            this.mBiddingLogicJs = biddingLogicJs;
            return this;
        }

        /**
         * Sets the bidding logic JavaScript version.
         *
         * <p>Default to be {@code 0L}, which will fall back to use default version(V1 or V2).
         *
         * @hide
         */
        @NonNull
        public AddCustomAudienceOverrideRequest.Builder setBiddingLogicJsVersion(
                long biddingLogicJsVersion) {
            this.mBiddingLogicJsVersion = biddingLogicJsVersion;
            return this;
        }

        /** Builds a {@link AddCustomAudienceOverrideRequest} instance. */
        @NonNull
        public AddCustomAudienceOverrideRequest build() {
            Objects.requireNonNull(mBuyer);
            Objects.requireNonNull(mName);
            Objects.requireNonNull(mBiddingLogicJs);
            Objects.requireNonNull(mTrustedBiddingSignals);

            return new AddCustomAudienceOverrideRequest(
                    mBuyer, mName, mBiddingLogicJs, mBiddingLogicJsVersion, mTrustedBiddingSignals);
        }
    }
}
