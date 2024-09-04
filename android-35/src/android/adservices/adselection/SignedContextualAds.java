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
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.flags.Flags;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Contains a list of buyer supplied {@link AdWithBid} bundle and its signature.
 *
 * <p>Instances of this class are created by SDKs to be injected as part of {@link
 * AdSelectionConfig} and passed to {@link AdSelectionManager#selectAds}
 *
 * <p>SignedContextualAds are signed using ECDSA algorithm with SHA256 hashing algorithm (aka
 * SHA256withECDSA). Keys used should belong to P-256 curve (aka “secp256r1” or “prime256v1”).
 *
 * <p>Signature should include the buyer, decisionLogicUri and adsWithBid fields.
 *
 * <p>While creating the signature a specific serialization rules must be followed as it's outlined
 * here:
 *
 * <ul>
 *   <li>{@code Objects} concatenate the serialized values of their fields with the {@code |} (pipe)
 *       in between each field
 *   <li>{@code All fields} are sorted by alphabetical order within the object
 *   <li>{@code Nullable fields} are skipped if they are null/unset
 *   <li>{@code Doubles} are converted to String preserving precision
 *   <li>{@code Integers} are converted to string values
 *   <li>{@code Sets} are sorted alphabetically
 *   <li>{@code Lists} keep the same order
 *   <li>{@code Strings} get encoded into byte[] using UTF-8 encoding
 * </ul>
 */
@FlaggedApi(Flags.FLAG_FLEDGE_AD_SELECTION_FILTERING_ENABLED)
public final class SignedContextualAds implements Parcelable {
    private static final String BUYER_CANNOT_BE_NULL = "Buyer cannot be null.";
    private static final String DECISION_LOGIC_URI_CANNOT_BE_NULL =
            "DecisionLogicUri cannot be null.";
    private static final String ADS_WITH_BID_CANNOT_BE_NULL = "AdsWithBid cannot be null.";
    private static final String SIGNATURE_CANNOT_BE_NULL = "Signature cannot be null.";
    @NonNull private final AdTechIdentifier mBuyer;
    @NonNull private final Uri mDecisionLogicUri;
    @NonNull private final List<AdWithBid> mAdsWithBid;
    @NonNull private final byte[] mSignature;

    @NonNull
    public static final Creator<SignedContextualAds> CREATOR =
            new Creator<>() {
                @Override
                public SignedContextualAds createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new SignedContextualAds(in);
                }

                @Override
                public SignedContextualAds[] newArray(int size) {
                    return new SignedContextualAds[0];
                }
            };

    private SignedContextualAds(
            @NonNull AdTechIdentifier buyer,
            @NonNull Uri decisionLogicUri,
            @NonNull List<AdWithBid> adsWithBid,
            @NonNull byte[] signature) {
        this.mBuyer = buyer;
        this.mDecisionLogicUri = decisionLogicUri;
        this.mAdsWithBid = adsWithBid;
        this.mSignature = signature;
    }

    private SignedContextualAds(@NonNull Parcel in) {
        Objects.requireNonNull(in);
        mBuyer = AdTechIdentifier.CREATOR.createFromParcel(in);
        mDecisionLogicUri = Uri.CREATOR.createFromParcel(in);
        mAdsWithBid = in.createTypedArrayList(AdWithBid.CREATOR);
        mSignature = in.createByteArray();
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
        dest.writeByteArray(mSignature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignedContextualAds)) return false;
        SignedContextualAds that = (SignedContextualAds) o;
        return Objects.equals(mBuyer, that.mBuyer)
                && Objects.equals(mDecisionLogicUri, that.mDecisionLogicUri)
                && Objects.equals(mAdsWithBid, that.mAdsWithBid)
                && Arrays.equals(mSignature, that.mSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBuyer, mDecisionLogicUri, mAdsWithBid, Arrays.hashCode(mSignature));
    }

    /**
     * @return the Ad tech identifier from which this contextual Ad would have been downloaded
     */
    @NonNull
    public AdTechIdentifier getBuyer() {
        return mBuyer;
    }

    /**
     * @return the URI used to retrieve the updateBid() and reportWin() function used during the ad
     *     selection and reporting process
     */
    @NonNull
    public Uri getDecisionLogicUri() {
        return mDecisionLogicUri;
    }

    /**
     * @return the Ad data with bid value associated with this ad
     */
    @NonNull
    public List<AdWithBid> getAdsWithBid() {
        return mAdsWithBid;
    }

    /**
     * Returns a copy of the signature for the contextual ads object.
     *
     * <p>See {@link SignedContextualAds} for more details.
     *
     * @return the signature
     */
    @NonNull
    public byte[] getSignature() {
        return Arrays.copyOf(mSignature, mSignature.length);
    }

    @Override
    public String toString() {
        return "SignedContextualAds{"
                + "mBuyer="
                + mBuyer
                + ", mDecisionLogicUri="
                + mDecisionLogicUri
                + ", mAdsWithBid="
                + mAdsWithBid
                + ", mSignature="
                + Arrays.toString(mSignature)
                + '}';
    }

    /** Builder for {@link SignedContextualAds} object */
    public static final class Builder {
        @Nullable private AdTechIdentifier mBuyer;
        @Nullable private Uri mDecisionLogicUri;
        @Nullable private List<AdWithBid> mAdsWithBid;
        @Nullable private byte[] mSignature;

        public Builder() {}

        /** Returns a {@link SignedContextualAds.Builder} from a {@link SignedContextualAds}. */
        public Builder(@NonNull SignedContextualAds signedContextualAds) {
            Objects.requireNonNull(signedContextualAds);

            this.mBuyer = signedContextualAds.getBuyer();
            this.mDecisionLogicUri = signedContextualAds.getDecisionLogicUri();
            this.mAdsWithBid = signedContextualAds.getAdsWithBid();
            this.mSignature = signedContextualAds.getSignature();
        }

        /**
         * Sets the buyer Ad tech Identifier
         *
         * <p>See {@link #getBuyer()} for more details
         */
        @NonNull
        public SignedContextualAds.Builder setBuyer(@NonNull AdTechIdentifier buyer) {
            Objects.requireNonNull(buyer, BUYER_CANNOT_BE_NULL);

            this.mBuyer = buyer;
            return this;
        }

        /**
         * Sets the URI to fetch the decision logic used in ad selection and reporting
         *
         * <p>See {@link #getDecisionLogicUri()} for more details
         */
        @NonNull
        public SignedContextualAds.Builder setDecisionLogicUri(@NonNull Uri decisionLogicUri) {
            Objects.requireNonNull(decisionLogicUri, DECISION_LOGIC_URI_CANNOT_BE_NULL);

            this.mDecisionLogicUri = decisionLogicUri;
            return this;
        }

        /**
         * Sets the Ads with pre-defined bid values
         *
         * <p>See {@link #getAdsWithBid()} for more details
         */
        @NonNull
        public SignedContextualAds.Builder setAdsWithBid(@NonNull List<AdWithBid> adsWithBid) {
            Objects.requireNonNull(adsWithBid, ADS_WITH_BID_CANNOT_BE_NULL);

            this.mAdsWithBid = adsWithBid;
            return this;
        }

        /** Sets the copied signature */
        @NonNull
        public SignedContextualAds.Builder setSignature(@NonNull byte[] signature) {
            Objects.requireNonNull(signature, SIGNATURE_CANNOT_BE_NULL);

            this.mSignature = Arrays.copyOf(signature, signature.length);
            return this;
        }

        /**
         * Builds a {@link SignedContextualAds} instance.
         *
         * @throws NullPointerException if any required params are null
         */
        @NonNull
        public SignedContextualAds build() {
            Objects.requireNonNull(mBuyer, BUYER_CANNOT_BE_NULL);
            Objects.requireNonNull(mDecisionLogicUri, DECISION_LOGIC_URI_CANNOT_BE_NULL);
            Objects.requireNonNull(mAdsWithBid, ADS_WITH_BID_CANNOT_BE_NULL);
            Objects.requireNonNull(mSignature, SIGNATURE_CANNOT_BE_NULL);

            return new SignedContextualAds(mBuyer, mDecisionLogicUri, mAdsWithBid, mSignature);
        }
    }
}
