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

import static android.adservices.adselection.AdSelectionOutcome.UNSET_AD_SELECTION_ID;
import static android.adservices.adselection.AdSelectionOutcome.UNSET_AD_SELECTION_ID_MESSAGE;

import android.adservices.common.AdTechIdentifier;
import android.annotation.NonNull;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the response from persistAdSelectionResult.
 *
 * @hide
 */
public final class PersistAdSelectionResultResponse implements Parcelable {
    private final long mAdSelectionId;
    private final Uri mAdRenderUri;
    private final AdTechIdentifier mWinningSeller;
    private final List<Uri> mComponentAdUris;

    public static final Creator<PersistAdSelectionResultResponse> CREATOR =
            new Creator<>() {
                @Override
                public PersistAdSelectionResultResponse createFromParcel(Parcel in) {
                    Objects.requireNonNull(in);

                    return new PersistAdSelectionResultResponse(in);
                }

                @Override
                public PersistAdSelectionResultResponse[] newArray(int size) {
                    return new PersistAdSelectionResultResponse[size];
                }
            };

    private PersistAdSelectionResultResponse(
            long adSelectionId,
            Uri adRenderUri,
            AdTechIdentifier winningSeller,
            List<Uri> componentAdUris) {
        Objects.requireNonNull(adRenderUri);

        this.mAdSelectionId = adSelectionId;
        this.mAdRenderUri = adRenderUri;
        this.mWinningSeller = winningSeller;
        this.mComponentAdUris = componentAdUris;
    }

    private PersistAdSelectionResultResponse(Parcel in) {
        Objects.requireNonNull(in);

        this.mAdSelectionId = in.readLong();
        this.mAdRenderUri = Uri.CREATOR.createFromParcel(in);
        this.mWinningSeller = AdTechIdentifier.CREATOR.createFromParcel(in);
        this.mComponentAdUris = new ArrayList<>();
        in.readTypedList(mComponentAdUris, Uri.CREATOR);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistAdSelectionResultResponse) {
            PersistAdSelectionResultResponse response = (PersistAdSelectionResultResponse) o;
            return mAdSelectionId == response.mAdSelectionId
                    && Objects.equals(mAdRenderUri, response.mAdRenderUri)
                    && Objects.equals(mWinningSeller, response.mWinningSeller)
                    && Objects.equals(mComponentAdUris, response.mComponentAdUris);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdSelectionId, mAdRenderUri, mWinningSeller, mComponentAdUris);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Returns the adSelectionId that identifies the AdSelection. */
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /** Returns the adSelectionData that is collected from device. */
    public Uri getAdRenderUri() {
        return mAdRenderUri;
    }

    /** Returns the winning seller id */
    public AdTechIdentifier getWinningSeller() {
        return mWinningSeller;
    }

    /** Returns the list of component ad URIs. */
    public List<Uri> getComponentAdUris() {
        return mComponentAdUris;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        Objects.requireNonNull(mAdRenderUri, "Ad render uri cannot be null");
        Objects.requireNonNull(mWinningSeller, "Winning seller cannot be null");

        dest.writeLong(mAdSelectionId);
        mAdRenderUri.writeToParcel(dest, flags);
        mWinningSeller.writeToParcel(dest, flags);
        dest.writeTypedList(mComponentAdUris);
    }

    /**
     * Builder for {@link PersistAdSelectionResultResponse} objects.
     *
     * @hide
     */
    public static final class Builder {
        private long mAdSelectionId;
        private Uri mAdRenderUri;
        private AdTechIdentifier mWinningSeller;
        private List<Uri> mComponentAdUris = List.of();

        public Builder() {
            mWinningSeller = AdTechIdentifier.UNSET_AD_TECH_IDENTIFIER;
        }

        /** Sets the adSelectionId. */
        public PersistAdSelectionResultResponse.Builder setAdSelectionId(long adSelectionId) {
            this.mAdSelectionId = adSelectionId;
            return this;
        }

        /** Sets the adRenderUri. */
        public PersistAdSelectionResultResponse.Builder setAdRenderUri(Uri adRenderUri) {
            Objects.requireNonNull(adRenderUri);

            this.mAdRenderUri = adRenderUri;
            return this;
        }

        /** Sets the winningSeller that won the auction. */
        public PersistAdSelectionResultResponse.Builder setWinningSeller(
                AdTechIdentifier winningSeller) {
            this.mWinningSeller = winningSeller;
            return this;
        }

        /** Sets the list of component ad URIs. Takes a copy of the provided list. */
        public PersistAdSelectionResultResponse.Builder setComponentAdUris(
                List<Uri> componentAdUris) {
            Objects.requireNonNull(componentAdUris);
            this.mComponentAdUris = componentAdUris;
            return this;
        }

        /**
         * Builds a {@link PersistAdSelectionResultResponse} instance.
         *
         * @throws IllegalArgumentException if the adSelectionId is not set
         * @throws NullPointerException if the RenderUri is null
         */
        @NonNull
        public PersistAdSelectionResultResponse build() {
            Objects.requireNonNull(mAdRenderUri, "Ad render uri cannot be null");
            Objects.requireNonNull(mWinningSeller, "Winning seller cannot be null");

            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

            return new PersistAdSelectionResultResponse(
                    mAdSelectionId, mAdRenderUri, mWinningSeller, mComponentAdUris);
        }
    }
}
