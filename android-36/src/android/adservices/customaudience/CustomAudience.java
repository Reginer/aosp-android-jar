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

import static com.android.adservices.flags.Flags.FLAG_FLEDGE_ENABLE_CUSTOM_AUDIENCE_COMPONENT_ADS;
import static com.android.adservices.flags.Flags.FLAG_FLEDGE_GET_AD_SELECTION_DATA_SELLER_CONFIGURATION_ENABLED;

import android.adservices.adselection.GetAdSelectionDataRequest;
import android.adservices.common.AdData;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.common.ComponentAdData;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.os.OutcomeReceiver;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Represents the information necessary for a custom audience to participate in ad selection.
 *
 * <p>A custom audience is an abstract grouping of users with similar demonstrated interests. This
 * class is a collection of some data stored on a device that is necessary to serve advertisements
 * targeting a single custom audience.
 */
public final class CustomAudience implements Parcelable {
    /** @hide */
    public static final int FLAG_AUCTION_SERVER_REQUEST_DEFAULT = 0;

    /** @hide */
    public static final double PRIORITY_DEFAULT = 0.0;

    /**
     * This auction server request flag indicates to the service that ads for this {@link
     * CustomAudience} can be omitted in the server auction payload.
     */
    @FlaggedApi(
            "com.android.adservices.flags.fledge_custom_audience_auction_server_request_flags_enabled")
    public static final int FLAG_AUCTION_SERVER_REQUEST_OMIT_ADS = 1 << 0;

    @NonNull private final AdTechIdentifier mBuyer;
    @NonNull private final String mName;
    @Nullable private final Instant mActivationTime;
    @Nullable private final Instant mExpirationTime;
    @NonNull private final Uri mDailyUpdateUri;
    @Nullable private final AdSelectionSignals mUserBiddingSignals;
    @Nullable private final TrustedBiddingData mTrustedBiddingData;
    @NonNull private final Uri mBiddingLogicUri;
    @NonNull private final List<AdData> mAds;
    @NonNull private final List<ComponentAdData> mComponentAds;
    @AuctionServerRequestFlag private final int mAuctionServerRequestFlags;
    private final double mPriority;

    /** @hide */
    @IntDef(
            flag = true,
            prefix = {"FLAG_AUCTION_SERVER_REQUEST"},
            value = {FLAG_AUCTION_SERVER_REQUEST_DEFAULT, FLAG_AUCTION_SERVER_REQUEST_OMIT_ADS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AuctionServerRequestFlag {}

    @NonNull
    public static final Creator<CustomAudience> CREATOR = new Creator<CustomAudience>() {
        @Override
        public CustomAudience createFromParcel(@NonNull Parcel in) {
            Objects.requireNonNull(in);

            return new CustomAudience(in);
        }

        @Override
        public CustomAudience[] newArray(int size) {
            return new CustomAudience[size];
        }
    };

    private CustomAudience(@NonNull CustomAudience.Builder builder) {
        Objects.requireNonNull(builder);

        mBuyer = builder.mBuyer;
        mName = builder.mName;
        mActivationTime = builder.mActivationTime;
        mExpirationTime = builder.mExpirationTime;
        mDailyUpdateUri = builder.mDailyUpdateUri;
        mUserBiddingSignals = builder.mUserBiddingSignals;
        mTrustedBiddingData = builder.mTrustedBiddingData;
        mBiddingLogicUri = builder.mBiddingLogicUri;
        mAds = builder.mAds;
        mComponentAds = builder.mComponentAds;
        mAuctionServerRequestFlags = builder.mAuctionServerRequestFlags;
        mPriority = builder.mPriority;
    }

    private CustomAudience(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mBuyer = AdTechIdentifier.CREATOR.createFromParcel(in);
        mName = in.readString();
        mActivationTime =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel) -> Instant.ofEpochMilli(sourceParcel.readLong()));
        mExpirationTime =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel) -> Instant.ofEpochMilli(sourceParcel.readLong()));
        mDailyUpdateUri = Uri.CREATOR.createFromParcel(in);
        mUserBiddingSignals =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, AdSelectionSignals.CREATOR::createFromParcel);
        mTrustedBiddingData =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, TrustedBiddingData.CREATOR::createFromParcel);
        mBiddingLogicUri = Uri.CREATOR.createFromParcel(in);
        mAds = in.createTypedArrayList(AdData.CREATOR);
        mComponentAds = in.createTypedArrayList(ComponentAdData.CREATOR);
        mAuctionServerRequestFlags = in.readInt();
        mPriority = in.readDouble();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        mBuyer.writeToParcel(dest, flags);
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
        mDailyUpdateUri.writeToParcel(dest, flags);
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mUserBiddingSignals,
                (targetParcel, sourceSignals) -> sourceSignals.writeToParcel(targetParcel, flags));
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mTrustedBiddingData,
                (targetParcel, sourceData) -> sourceData.writeToParcel(targetParcel, flags));
        mBiddingLogicUri.writeToParcel(dest, flags);
        dest.writeTypedList(mAds);
        dest.writeTypedList(mComponentAds);
        dest.writeInt(mAuctionServerRequestFlags);
        dest.writeDouble(mPriority);
    }

    @Override
    public String toString() {
        return "CustomAudience{"
                + "mBuyer="
                + mBuyer
                + ", mName='"
                + mName
                + ", mActivationTime="
                + mActivationTime
                + ", mExpirationTime="
                + mExpirationTime
                + ", mDailyUpdateUri="
                + mDailyUpdateUri
                + ", mUserBiddingSignals="
                + mUserBiddingSignals
                + ", mTrustedBiddingData="
                + mTrustedBiddingData
                + ", mBiddingLogicUri="
                + mBiddingLogicUri
                + ", mAds="
                + mAds
                + ", mComponentAds="
                + mComponentAds
                + ", mAuctionServerRequestFlags="
                + mAuctionServerRequestFlags
                + ", mPriority="
                + mPriority
                + '}';
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * A buyer is identified by a domain in the form "buyerexample.com".
     *
     * @return an {@link AdTechIdentifier} containing the custom audience's buyer's domain
     */
    @NonNull
    public AdTechIdentifier getBuyer() {
        return mBuyer;
    }

    /**
     * The custom audience's name is an arbitrary string provided by the owner and buyer on creation
     * of the {@link CustomAudience} object.
     *
     * <p>The overall size of the CA is limited and the size of this field is considered using
     * {@link String#getBytes()} in {@code UTF-8} encoding.
     *
     * @return the String name of the custom audience
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * On creation of the {@link CustomAudience} object, an optional activation time may be set in
     * the future, in order to serve a delayed activation. If the field is not set, the {@link
     * CustomAudience} will be activated at the time of joining.
     *
     * <p>For example, a custom audience for lapsed users may not activate until a threshold of
     * inactivity is reached, at which point the custom audience's ads will participate in the ad
     * selection process, potentially redirecting lapsed users to the original owner application.
     *
     * <p>The maximum delay in activation is 60 days from initial creation.
     *
     * <p>If specified, the activation time must be an earlier instant than the expiration time.
     *
     * @return the timestamp {@link Instant}, truncated to milliseconds, after which the custom
     *     audience is active
     */
    @Nullable
    public Instant getActivationTime() {
        return mActivationTime;
    }

    /**
     * Once the expiration time has passed, a custom audience is no longer eligible for daily
     * ad/bidding data updates or participation in the ad selection process. The custom audience
     * will then be deleted from memory by the next daily update.
     *
     * <p>If no expiration time is provided on creation of the {@link CustomAudience}, expiry will
     * default to 60 days from activation.
     *
     * <p>The maximum expiry is 60 days from initial activation.
     *
     * @return the timestamp {@link Instant}, truncated to milliseconds, after which the custom
     *     audience should be removed
     */
    @Nullable
    public Instant getExpirationTime() {
        return mExpirationTime;
    }

    /**
     * This URI points to a buyer-operated server that hosts updated bidding data and ads metadata
     * to be used in the on-device ad selection process. The URI must use HTTPS.
     *
     * @return the custom audience's daily update URI
     */
    @NonNull
    public Uri getDailyUpdateUri() {
        return mDailyUpdateUri;
    }

    /**
     * User bidding signals are optionally provided by buyers to be consumed by buyer-provided
     * JavaScript during ad selection in an isolated execution environment.
     *
     * <p>If the user bidding signals are not a valid JSON object that can be consumed by the
     * buyer's JS, the custom audience will not be eligible for ad selection.
     *
     * <p>If not specified, the {@link CustomAudience} will not participate in ad selection until
     * user bidding signals are provided via the daily update for the custom audience.
     *
     * @return an {@link AdSelectionSignals} object representing the user bidding signals for the
     *     custom audience
     */
    @Nullable
    public AdSelectionSignals getUserBiddingSignals() {
        return mUserBiddingSignals;
    }

    /**
     * Trusted bidding data consists of a URI pointing to a trusted server for buyers' bidding data
     * and a list of keys to query the server with. Note that the keys are arbitrary identifiers
     * that will only be used to query the trusted server for a buyer's bidding logic during ad
     * selection.
     *
     * <p>If not specified, the {@link CustomAudience} will not participate in ad selection until
     * trusted bidding data are provided via the daily update for the custom audience.
     *
     * @return a {@link TrustedBiddingData} object containing the custom audience's trusted bidding
     *     data
     */
    @Nullable
    public TrustedBiddingData getTrustedBiddingData() {
        return mTrustedBiddingData;
    }

    /**
     * Returns the target URI used to fetch bidding logic when a custom audience participates in the
     * ad selection process. The URI must use HTTPS.
     *
     * @return the URI for fetching buyer bidding logic
     */
    @NonNull
    public Uri getBiddingLogicUri() {
        return mBiddingLogicUri;
    }

    /**
     * This list of {@link AdData} objects is a full and complete list of the ads that will be
     * served by this {@link CustomAudience} during the ad selection process.
     *
     * <p>If not specified, or if an empty list is provided, the {@link CustomAudience} will not
     * participate in ad selection until a valid list of ads are provided via the daily update for
     * the custom audience.
     *
     * <p>The combined ads size of the CA is limited and the sizes of each ad's string fields are
     * considered using {@link String#getBytes()} in {@code UTF-8} encoding.
     *
     * @return a {@link List} of {@link AdData} objects representing ads currently served by the
     *     custom audience
     */
    @NonNull
    public List<AdData> getAds() {
        return mAds;
    }

    /**
     * This list of {@link ComponentAdData} objects is a full and complete list of the ad components
     * that will be served by this {@link CustomAudience} during the ad selection process.
     *
     * <p>The combined ads size of the CA is limited and the sizes of each ad's string fields are
     * considered using {@link String#getBytes()} in {@code UTF-8} encoding.
     *
     * @return a {@link List} of {@link ComponentAdData} objects representing component ads
     *     currently served by the custom audience
     */
    @FlaggedApi(FLAG_FLEDGE_ENABLE_CUSTOM_AUDIENCE_COMPONENT_ADS)
    @NonNull
    public List<ComponentAdData> getComponentAds() {
        return mComponentAds;
    }

    /**
     * Returns the bitfield of auction server request flags. These are flags that influence the
     * creation of the payload generated by the {@link
     * android.adservices.adselection.AdSelectionManager#getAdSelectionData(GetAdSelectionDataRequest,
     * Executor, OutcomeReceiver)} API.
     *
     * <p>To create this bitfield, place an {@code |} bitwise operator between each {@link
     * AuctionServerRequestFlag} to be enabled.
     */
    @FlaggedApi(
            "com.android.adservices.flags.fledge_custom_audience_auction_server_request_flags_enabled")
    @AuctionServerRequestFlag
    public int getAuctionServerRequestFlags() {
        return mAuctionServerRequestFlags;
    }

    /**
     * Returns the priority of this CustomAudience with respect to other CustomAudiences for this
     * buyer.
     *
     * <p>This means if there is insufficient space during buyer input generation in the {@link
     * android.adservices.adselection.AdSelectionManager#getAdSelectionData(GetAdSelectionDataRequest,
     * Executor, OutcomeReceiver)} call, the service will attempt to include the highest priority
     * custom audiences first.
     *
     * <p>The default value if this field is not set is 0.0.
     */
    @FlaggedApi(FLAG_FLEDGE_GET_AD_SELECTION_DATA_SELLER_CONFIGURATION_ENABLED)
    public double getPriority() {
        return mPriority;
    }

    /**
     * Checks whether two {@link CustomAudience} objects contain the same information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomAudience)) return false;
        CustomAudience that = (CustomAudience) o;
        return mBuyer.equals(that.mBuyer)
                && mName.equals(that.mName)
                && Objects.equals(mActivationTime, that.mActivationTime)
                && Objects.equals(mExpirationTime, that.mExpirationTime)
                && mDailyUpdateUri.equals(that.mDailyUpdateUri)
                && Objects.equals(mUserBiddingSignals, that.mUserBiddingSignals)
                && Objects.equals(mTrustedBiddingData, that.mTrustedBiddingData)
                && mBiddingLogicUri.equals(that.mBiddingLogicUri)
                && mAds.equals(that.mAds)
                && mComponentAds.equals(that.mComponentAds)
                && mAuctionServerRequestFlags == that.mAuctionServerRequestFlags
                && Double.compare(mPriority, that.mPriority) == 0
                && mComponentAds.equals(that.mComponentAds);
    }

    /** Returns the hash of the {@link CustomAudience} object's data. */
    @Override
    public int hashCode() {
        return Objects.hash(
                mBuyer,
                mName,
                mActivationTime,
                mExpirationTime,
                mDailyUpdateUri,
                mUserBiddingSignals,
                mTrustedBiddingData,
                mBiddingLogicUri,
                mAds,
                mComponentAds,
                mAuctionServerRequestFlags,
                mPriority);
    }

    /** Builder for {@link CustomAudience} objects. */
    public static final class Builder {
        @Nullable private AdTechIdentifier mBuyer;
        @Nullable private String mName;
        @Nullable private Instant mActivationTime;
        @Nullable private Instant mExpirationTime;
        @Nullable private Uri mDailyUpdateUri;
        @Nullable private AdSelectionSignals mUserBiddingSignals;
        @Nullable private TrustedBiddingData mTrustedBiddingData;
        @Nullable private Uri mBiddingLogicUri;
        @Nullable private List<AdData> mAds;
        @Nullable private List<ComponentAdData> mComponentAds;
        @AuctionServerRequestFlag private int mAuctionServerRequestFlags;
        private double mPriority;

        // TODO(b/232883403): We may need to add @NonNUll members as args.
        public Builder() {
        }

        /**
         * Sets the buyer {@link AdTechIdentifier}.
         *
         * <p>See {@link #getBuyer()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setBuyer(@NonNull AdTechIdentifier buyer) {
            Objects.requireNonNull(buyer);
            mBuyer = buyer;
            return this;
        }

        /**
         * Sets the {@link CustomAudience} object's name.
         * <p>
         * See {@link #getName()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setName(@NonNull String name) {
            Objects.requireNonNull(name);
            mName = name;
            return this;
        }

        /**
         * Sets the time, truncated to milliseconds, after which the {@link CustomAudience} will
         * serve ads.
         *
         * <p>Set to {@code null} in order for this {@link CustomAudience} to be immediately active
         * and participate in ad selection.
         *
         * <p>See {@link #getActivationTime()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setActivationTime(@Nullable Instant activationTime) {
            mActivationTime = activationTime;
            return this;
        }

        /**
         * Sets the time, truncated to milliseconds, after which the {@link CustomAudience} should
         * be removed.
         * <p>
         * See {@link #getExpirationTime()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setExpirationTime(@Nullable Instant expirationTime) {
            mExpirationTime = expirationTime;
            return this;
        }

        /**
         * Sets the daily update URI. The URI must use HTTPS.
         *
         * <p>See {@link #getDailyUpdateUri()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setDailyUpdateUri(@NonNull Uri dailyUpdateUri) {
            Objects.requireNonNull(dailyUpdateUri);
            mDailyUpdateUri = dailyUpdateUri;
            return this;
        }

        /**
         * Sets the user bidding signals used in the ad selection process.
         *
         * <p>See {@link #getUserBiddingSignals()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setUserBiddingSignals(
                @Nullable AdSelectionSignals userBiddingSignals) {
            mUserBiddingSignals = userBiddingSignals;
            return this;
        }

        /**
         * Sets the trusted bidding data to be queried and used in the ad selection process.
         * <p>
         * See {@link #getTrustedBiddingData()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setTrustedBiddingData(
                @Nullable TrustedBiddingData trustedBiddingData) {
            mTrustedBiddingData = trustedBiddingData;
            return this;
        }

        /**
         * Sets the URI to fetch bidding logic from for use in the ad selection process. The URI
         * must use HTTPS.
         *
         * <p>See {@link #getBiddingLogicUri()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setBiddingLogicUri(@NonNull Uri biddingLogicUri) {
            Objects.requireNonNull(biddingLogicUri);
            mBiddingLogicUri = biddingLogicUri;
            return this;
        }

        /**
         * Sets the initial remarketing ads served by the custom audience. Will be assigned with an
         * empty list if not provided.
         *
         * <p>See {@link #getAds()} for more information.
         */
        @NonNull
        public CustomAudience.Builder setAds(@Nullable List<AdData> ads) {
            mAds = ads;
            return this;
        }

        /**
         * Sets the components ads served by the custom audience.
         *
         * <p>See {@link #getComponentAds()} for more information.
         */
        @FlaggedApi(FLAG_FLEDGE_ENABLE_CUSTOM_AUDIENCE_COMPONENT_ADS)
        @NonNull
        public CustomAudience.Builder setComponentAds(@NonNull List<ComponentAdData> componentAds) {
            mComponentAds =
                    Objects.requireNonNull(componentAds, "The component ads provided are null");
            return this;
        }

        /**
         * Sets the bitfield of auction server request flags.
         *
         * <p>See {@link #getAuctionServerRequestFlags()} for more information.
         */
        @FlaggedApi(
                "com.android.adservices.flags.fledge_custom_audience_auction_server_request_flags_enabled")
        @NonNull
        public CustomAudience.Builder setAuctionServerRequestFlags(
                @AuctionServerRequestFlag int auctionServerRequestFlags) {
            mAuctionServerRequestFlags = auctionServerRequestFlags;
            return this;
        }

        /**
         * Sets the priority for this custom audience.
         *
         * <p>See {@link #getPriority()} for further details.
         */
        @FlaggedApi(FLAG_FLEDGE_GET_AD_SELECTION_DATA_SELLER_CONFIGURATION_ENABLED)
        @NonNull
        public CustomAudience.Builder setPriority(double priority) {
            mPriority = priority;
            return this;
        }

        /**
         * Builds an instance of a {@link CustomAudience}.
         *
         * @throws NullPointerException     if any non-null parameter is null
         * @throws IllegalArgumentException if the expiration time occurs before activation time
         * @throws IllegalArgumentException if the expiration time is set before the current time
         */
        @NonNull
        public CustomAudience build() {
            Objects.requireNonNull(mBuyer, "The buyer has not been provided");
            Objects.requireNonNull(mName, "The name has not been provided");
            Objects.requireNonNull(mDailyUpdateUri, "The daily update URI has not been provided");
            Objects.requireNonNull(mBiddingLogicUri, "The bidding logic URI has not been provided");

            // To pass the API lint, we should not allow null Collection.
            if (mAds == null) {
                mAds = List.of();
            }
            if (mComponentAds == null) {
                mComponentAds = List.of();
            }

            return new CustomAudience(this);
        }
    }
}
