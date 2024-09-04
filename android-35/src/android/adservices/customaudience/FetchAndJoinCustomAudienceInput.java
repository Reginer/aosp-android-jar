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
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;

import java.time.Instant;
import java.util.Objects;

/**
 * The input object wrapping the required and optional parameters needed to fetch a {@link
 * CustomAudience}.
 *
 * <p>Refer to {@link FetchAndJoinCustomAudienceRequest} for more information about the parameters.
 *
 * @hide
 */
public final class FetchAndJoinCustomAudienceInput implements Parcelable {
    @NonNull private final Uri mFetchUri;
    @NonNull private final String mCallerPackageName;
    @Nullable private final String mName;
    @Nullable private final Instant mActivationTime;
    @Nullable private final Instant mExpirationTime;
    @Nullable private final AdSelectionSignals mUserBiddingSignals;

    @NonNull
    public static final Creator<FetchAndJoinCustomAudienceInput> CREATOR =
            new Creator<FetchAndJoinCustomAudienceInput>() {
                @NonNull
                @Override
                public FetchAndJoinCustomAudienceInput createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new FetchAndJoinCustomAudienceInput(in);
                }

                @NonNull
                @Override
                public FetchAndJoinCustomAudienceInput[] newArray(int size) {
                    return new FetchAndJoinCustomAudienceInput[size];
                }
            };

    private FetchAndJoinCustomAudienceInput(
            @NonNull FetchAndJoinCustomAudienceInput.Builder builder) {
        Objects.requireNonNull(builder);

        mFetchUri = builder.mFetchUri;
        mName = builder.mName;
        mActivationTime = builder.mActivationTime;
        mExpirationTime = builder.mExpirationTime;
        mUserBiddingSignals = builder.mUserBiddingSignals;
        mCallerPackageName = builder.mCallerPackageName;
    }

    private FetchAndJoinCustomAudienceInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mFetchUri = Uri.CREATOR.createFromParcel(in);
        mName =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel -> in.readString()));
        mActivationTime =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel) -> Instant.ofEpochMilli(sourceParcel.readLong()));
        mExpirationTime =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel) -> Instant.ofEpochMilli(sourceParcel.readLong()));
        mUserBiddingSignals =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, AdSelectionSignals.CREATOR::createFromParcel);
        mCallerPackageName = in.readString();
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
     * @return the caller app's package name.
     */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        mFetchUri.writeToParcel(dest, flags);
        AdServicesParcelableUtil.writeNullableToParcel(dest, mName, Parcel::writeString);
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
        dest.writeString(mCallerPackageName);
    }

    /**
     * @return {@code true} only if two {@link FetchAndJoinCustomAudienceInput} objects contain the
     *     same information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FetchAndJoinCustomAudienceInput)) return false;
        FetchAndJoinCustomAudienceInput that = (FetchAndJoinCustomAudienceInput) o;
        return mFetchUri.equals(that.mFetchUri)
                && mCallerPackageName.equals(that.mCallerPackageName)
                && Objects.equals(mName, that.mName)
                && Objects.equals(mActivationTime, that.mActivationTime)
                && Objects.equals(mExpirationTime, that.mExpirationTime)
                && Objects.equals(mUserBiddingSignals, that.mUserBiddingSignals);
    }

    /**
     * @return the hash of the {@link FetchAndJoinCustomAudienceInput} object's data.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                mFetchUri,
                mCallerPackageName,
                mName,
                mActivationTime,
                mExpirationTime,
                mUserBiddingSignals);
    }

    /**
     * @return a human-readable representation of {@link FetchAndJoinCustomAudienceInput}.
     */
    @Override
    public String toString() {
        return "FetchAndJoinCustomAudienceInput{"
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
                + ", callerPackageName="
                + mCallerPackageName
                + '}';
    }

    /**
     * Builder for {@link FetchAndJoinCustomAudienceInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        @NonNull private Uri mFetchUri;
        @NonNull private String mCallerPackageName;
        @Nullable private String mName;
        @Nullable private Instant mActivationTime;
        @Nullable private Instant mExpirationTime;
        @Nullable private AdSelectionSignals mUserBiddingSignals;

        /**
         * Instantiates a {@link FetchAndJoinCustomAudienceInput.Builder} with the {@link Uri} from
         * which the custom audience is to be fetched and the caller app's package name.
         *
         * @throws NullPointerException if any non-null parameter is null.
         */
        public Builder(@NonNull Uri fetchUri, @NonNull String callerPackageName) {
            Objects.requireNonNull(fetchUri);
            Objects.requireNonNull(callerPackageName);

            this.mFetchUri = fetchUri;
            this.mCallerPackageName = callerPackageName;
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
         * Sets the caller app's package name.
         *
         * <p>See {@link #getCallerPackageName()} for details.
         */
        @NonNull
        public Builder setCallerPackageName(@NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);
            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /**
         * Builds an instance of a {@link FetchAndJoinCustomAudienceInput}.
         *
         * @throws NullPointerException if any non-null parameter is null.
         */
        @NonNull
        public FetchAndJoinCustomAudienceInput build() {
            Objects.requireNonNull(mFetchUri);
            Objects.requireNonNull(mCallerPackageName);

            return new FetchAndJoinCustomAudienceInput(this);
        }
    }
}
