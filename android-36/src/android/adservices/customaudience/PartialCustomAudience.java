/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.adservices.flags.Flags.FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED;

import android.adservices.common.AdSelectionSignals;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a partial custom audience that is passed along to the buyer ad tech, when scheduling a
 * delayed update for Custom Audience. Any field set by the caller cannot be overridden by the
 * custom audience fetched from the {@code updateUri}
 *
 * <p>Given multiple Custom Audiences could be returned by the buyer, we will match the override
 * restriction based on the name of Custom Audience. Thus name would be a required field.
 *
 * <p>Other nullable fields will not be overridden if left null
 *
 * <p>For more information about each field refer to {@link CustomAudience}.
 */
@FlaggedApi(FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED)
public final class PartialCustomAudience implements Parcelable {
    @NonNull private final String mName;
    @Nullable private final Instant mActivationTime;
    @Nullable private final Instant mExpirationTime;
    @Nullable private final AdSelectionSignals mUserBiddingSignals;

    private PartialCustomAudience(@NonNull PartialCustomAudience.Builder builder) {
        Objects.requireNonNull(builder);

        mName = builder.mName;
        mActivationTime = builder.mActivationTime;
        mExpirationTime = builder.mExpirationTime;
        mUserBiddingSignals = builder.mUserBiddingSignals;
    }

    @NonNull
    public static final Creator<PartialCustomAudience> CREATOR =
            new Creator<PartialCustomAudience>() {
                @NonNull
                @Override
                public PartialCustomAudience createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new PartialCustomAudience(in);
                }

                @NonNull
                @Override
                public PartialCustomAudience[] newArray(int size) {
                    return new PartialCustomAudience[size];
                }
            };

    private PartialCustomAudience(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mName = in.readString();
        mActivationTime =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel) -> Instant.ofEpochMilli(sourceParcel.readLong()));
        mExpirationTime =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel) -> Instant.ofEpochMilli(sourceParcel.readLong()));
        mUserBiddingSignals =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, AdSelectionSignals.CREATOR::createFromParcel);
    }

    /**
     * Reference {@link CustomAudience#getName()} for details.
     *
     * @return the {@link String} name of the custom audience to join.
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Reference {@link CustomAudience#getActivationTime()} for details. Will not be overridden if
     * left null.
     *
     * @return the {@link Instant} by which joining the custom audience will be delayed.
     */
    @Nullable
    public Instant getActivationTime() {
        return mActivationTime;
    }

    /**
     * Reference {@link CustomAudience#getExpirationTime()} for details. Will not be overridden if
     * left null.
     *
     * @return the {@link Instant} by when the membership to the custom audience will expire.
     */
    @Nullable
    public Instant getExpirationTime() {
        return mExpirationTime;
    }

    /**
     * Reference {@link CustomAudience#getUserBiddingSignals()} for details. Will not be overridden
     * if left null.
     *
     * @return the buyer signals to be consumed by the buyer-provided JavaScript when the custom
     *     audience participates in an ad selection.
     */
    @Nullable
    public AdSelectionSignals getUserBiddingSignals() {
        return mUserBiddingSignals;
    }

    /**
     * @return the hash of the {@link PartialCustomAudience} object's data.
     */
    @Override
    public int hashCode() {
        return Objects.hash(mName, mActivationTime, mExpirationTime, mUserBiddingSignals);
    }

    /**
     * @return {@code true} only if two {@link PartialCustomAudience} objects contain the same
     *     information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartialCustomAudience)) return false;
        PartialCustomAudience that = (PartialCustomAudience) o;
        return Objects.equals(mName, that.mName)
                && Objects.equals(mActivationTime, that.mActivationTime)
                && Objects.equals(mExpirationTime, that.mExpirationTime)
                && Objects.equals(mUserBiddingSignals, that.mUserBiddingSignals);
    }

    /**
     * @return a human-readable representation of {@link PartialCustomAudience}.
     */
    @Override
    public String toString() {
        return "PartialCustomAudience {"
                + "name="
                + mName
                + ", activationTime="
                + mActivationTime
                + ", expirationTime="
                + mExpirationTime
                + ", userBiddingSignals="
                + mUserBiddingSignals
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        dest.writeString(mName);
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mActivationTime,
                (targetParcel, sourceInstant) ->
                        targetParcel.writeLong(sourceInstant.toEpochMilli()));
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mExpirationTime,
                (targetParcel, sourceInstant) ->
                        targetParcel.writeLong(sourceInstant.toEpochMilli()));
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mUserBiddingSignals,
                (targetParcel, sourceSignals) -> sourceSignals.writeToParcel(targetParcel, flags));
    }

    /** Builder for {@link PartialCustomAudience} objects. */
    public static final class Builder {
        @NonNull private String mName;
        @Nullable private Instant mActivationTime;
        @Nullable private Instant mExpirationTime;
        @Nullable private AdSelectionSignals mUserBiddingSignals;

        /**
         * Instantiates a {@link PartialCustomAudience.Builder} with a {@link String} name for which
         * this Partial Custom Audience will be updated
         */
        public Builder(@NonNull String name) {
            Objects.requireNonNull(name);
            this.mName = name;
        }

        /**
         * Sets the {@link Instant} by which joining the custom audience will be delayed.
         *
         * <p>See {@link #getActivationTime()} for details.
         */
        @NonNull
        public PartialCustomAudience.Builder setActivationTime(@Nullable Instant activationTime) {
            this.mActivationTime = activationTime;
            return this;
        }

        /**
         * Sets the {@link Instant} by when the membership to the custom audience will expire.
         *
         * <p>See {@link #getExpirationTime()} for details.
         */
        @NonNull
        public PartialCustomAudience.Builder setExpirationTime(@Nullable Instant expirationTime) {
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
        public PartialCustomAudience.Builder setUserBiddingSignals(
                @Nullable AdSelectionSignals userBiddingSignals) {
            this.mUserBiddingSignals = userBiddingSignals;
            return this;
        }

        /**
         * Builds an instance of a {@link FetchAndJoinCustomAudienceRequest}.
         *
         * @throws NullPointerException if any non-null parameter is null.
         */
        @NonNull
        public PartialCustomAudience build() {
            Objects.requireNonNull(mName);
            return new PartialCustomAudience(this);
        }
    }
}
