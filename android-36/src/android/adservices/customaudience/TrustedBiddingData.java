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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Objects;

/**
 * Represents data used during the ad selection process to fetch buyer bidding signals from a
 * trusted key/value server. The fetched data is used during the ad selection process and consumed
 * by buyer JavaScript logic running in an isolated execution environment.
 */
public final class TrustedBiddingData implements Parcelable {
    @NonNull private final Uri mTrustedBiddingUri;
    @NonNull
    private final List<String> mTrustedBiddingKeys;

    @NonNull
    public static final Creator<TrustedBiddingData> CREATOR = new Creator<TrustedBiddingData>() {
        @Override
        public TrustedBiddingData createFromParcel(@NonNull Parcel in) {
            Objects.requireNonNull(in);
            return new TrustedBiddingData(in);
        }

        @Override
        public TrustedBiddingData[] newArray(int size) {
            return new TrustedBiddingData[size];
        }
    };

    private TrustedBiddingData(@NonNull TrustedBiddingData.Builder builder) {
        mTrustedBiddingUri = builder.mTrustedBiddingUri;
        mTrustedBiddingKeys = builder.mTrustedBiddingKeys;
    }

    private TrustedBiddingData(@NonNull Parcel in) {
        Objects.requireNonNull(in);
        mTrustedBiddingUri = Uri.CREATOR.createFromParcel(in);
        mTrustedBiddingKeys = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        mTrustedBiddingUri.writeToParcel(dest, flags);
        dest.writeStringList(mTrustedBiddingKeys);
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @return the URI pointing to the trusted key-value server holding bidding signals. The URI
     *     must use HTTPS.
     */
    @NonNull
    public Uri getTrustedBiddingUri() {
        return mTrustedBiddingUri;
    }

    /**
     * @return the list of keys to query from the trusted key-value server holding bidding signals
     */
    @NonNull
    public List<String> getTrustedBiddingKeys() {
        return mTrustedBiddingKeys;
    }

    /**
     * @return {@code true} if two {@link TrustedBiddingData} objects contain the same information
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrustedBiddingData)) return false;
        TrustedBiddingData that = (TrustedBiddingData) o;
        return mTrustedBiddingUri.equals(that.mTrustedBiddingUri)
                && mTrustedBiddingKeys.equals(that.mTrustedBiddingKeys);
    }

    /**
     * @return the hash of the {@link TrustedBiddingData} object's data
     */
    @Override
    public int hashCode() {
        return Objects.hash(mTrustedBiddingUri, mTrustedBiddingKeys);
    }

    /** Builder for {@link TrustedBiddingData} objects. */
    public static final class Builder {
        @Nullable private Uri mTrustedBiddingUri;
        @Nullable private List<String> mTrustedBiddingKeys;

        // TODO(b/232883403): We may need to add @NonNUll members as args.
        public Builder() {
        }

        /**
         * Sets the URI pointing to a trusted key-value server used to fetch bidding signals during
         * the ad selection process. The URI must use HTTPS.
         */
        @NonNull
        public Builder setTrustedBiddingUri(@NonNull Uri trustedBiddingUri) {
            Objects.requireNonNull(trustedBiddingUri);
            mTrustedBiddingUri = trustedBiddingUri;
            return this;
        }

        /**
         * Sets the list of keys to query the trusted key-value server with.
         * <p>
         * This list is permitted to be empty, but it must not be null.
         */
        @NonNull
        public Builder setTrustedBiddingKeys(@NonNull List<String> trustedBiddingKeys) {
            Objects.requireNonNull(trustedBiddingKeys);
            mTrustedBiddingKeys = trustedBiddingKeys;
            return this;
        }

        /**
         * Builds the {@link TrustedBiddingData} object.
         *
         * @throws NullPointerException if any parameters are null when built
         */
        @NonNull
        public TrustedBiddingData build() {
            Objects.requireNonNull(mTrustedBiddingUri);
            // Note that the list of keys is allowed to be empty, but not null
            Objects.requireNonNull(mTrustedBiddingKeys);

            return new TrustedBiddingData(this);
        }
    }
}
