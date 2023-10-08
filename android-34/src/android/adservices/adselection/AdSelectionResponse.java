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

import static android.adservices.adselection.AdSelectionOutcome.UNSET_AD_SELECTION_ID;
import static android.adservices.adselection.AdSelectionOutcome.UNSET_AD_SELECTION_ID_MESSAGE;

import android.annotation.NonNull;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * This class represents the response returned by the {@link AdSelectionManager} as the result of a
 * successful {@code selectAds} call.
 *
 * @hide
 */
public final class AdSelectionResponse implements Parcelable {
    private final long mAdSelectionId;
    @NonNull private final Uri mRenderUri;

    private AdSelectionResponse(long adSelectionId, @NonNull Uri renderUri) {
        Objects.requireNonNull(renderUri);

        mAdSelectionId = adSelectionId;
        mRenderUri = renderUri;
    }

    private AdSelectionResponse(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mAdSelectionId = in.readLong();
        mRenderUri = Uri.CREATOR.createFromParcel(in);
    }

    @NonNull
    public static final Creator<AdSelectionResponse> CREATOR =
            new Parcelable.Creator<AdSelectionResponse>() {
                @Override
                public AdSelectionResponse createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new AdSelectionResponse(in);
                }

                @Override
                public AdSelectionResponse[] newArray(int size) {
                    return new AdSelectionResponse[size];
                }
            };

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof AdSelectionResponse) {
            AdSelectionResponse adSelectionResponse = (AdSelectionResponse) o;
            return mAdSelectionId == adSelectionResponse.mAdSelectionId
                    && Objects.equals(mRenderUri, adSelectionResponse.mRenderUri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdSelectionId, mRenderUri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        dest.writeLong(mAdSelectionId);
        mRenderUri.writeToParcel(dest, flags);
    }

    @Override
    public String toString() {
        return "AdSelectionResponse{"
                + "mAdSelectionId="
                + mAdSelectionId
                + ", mRenderUri="
                + mRenderUri
                + '}';
    }

    /**
     * Builder for {@link AdSelectionResponse} objects.
     *
     * @hide
     */
    public static final class Builder {
        private long mAdSelectionId = UNSET_AD_SELECTION_ID;
        @NonNull private Uri mRenderUri;

        public Builder() {}

        /** Sets the mAdSelectionId. */
        @NonNull
        public AdSelectionResponse.Builder setAdSelectionId(long adSelectionId) {
            this.mAdSelectionId = adSelectionId;
            return this;
        }

        /** Sets the RenderUri. */
        @NonNull
        public AdSelectionResponse.Builder setRenderUri(@NonNull Uri renderUri) {
            Objects.requireNonNull(renderUri);

            mRenderUri = renderUri;
            return this;
        }

        /**
         * Builds a {@link AdSelectionResponse} instance.
         *
         * @throws IllegalArgumentException if the adSelectionIid is not set
         * @throws NullPointerException if the RenderUri is null
         */
        @NonNull
        public AdSelectionResponse build() {
            Objects.requireNonNull(mRenderUri);

            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

            return new AdSelectionResponse(mAdSelectionId, mRenderUri);
        }
    }
}
