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

package android.adservices.adselection;

import static com.android.adservices.flags.Flags.FLAG_FLEDGE_GET_AD_SELECTION_DATA_SELLER_CONFIGURATION_ENABLED;

import android.adservices.common.AdTechIdentifier;
import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.os.OutcomeReceiver;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Contains a per buyer configuration which will be used as part of a {@link SellerConfiguration} in
 * a {@link GetAdSelectionDataRequest}.
 *
 * <p>This object will be created by the calling SDK as part of creating the seller configuration.
 */
@FlaggedApi(FLAG_FLEDGE_GET_AD_SELECTION_DATA_SELLER_CONFIGURATION_ENABLED)
public final class PerBuyerConfiguration implements Parcelable {
    @NonNull private final AdTechIdentifier mBuyer;
    private final int mTargetInputSizeBytes;

    private PerBuyerConfiguration(Parcel in) {
        mBuyer = AdTechIdentifier.CREATOR.createFromParcel(in);
        mTargetInputSizeBytes = in.readInt();
    }

    private PerBuyerConfiguration(@NonNull AdTechIdentifier buyer, int targetInputSizeBytes) {
        Objects.requireNonNull(buyer, "Buyer must be set.");

        mBuyer = buyer;
        mTargetInputSizeBytes = targetInputSizeBytes;
    }

    @NonNull
    public static final Creator<PerBuyerConfiguration> CREATOR =
            new Creator<PerBuyerConfiguration>() {
                @Override
                public PerBuyerConfiguration createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new PerBuyerConfiguration(in);
                }

                @Override
                public PerBuyerConfiguration[] newArray(int size) {
                    return new PerBuyerConfiguration[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        mBuyer.writeToParcel(dest, flags);
        dest.writeInt(mTargetInputSizeBytes);
    }

    /** Returns the buyer associated with this per buyer configuration. */
    @NonNull
    public AdTechIdentifier getBuyer() {
        return mBuyer;
    }

    /**
     * The service will make a best effort attempt to include this amount of bytes into the response
     * of {@link AdSelectionManager#getAdSelectionData(GetAdSelectionDataRequest, Executor,
     * OutcomeReceiver)} for this buyer.
     *
     * <p>If this is zero this buyer will share remaining space after other buyers' target sizes are
     * respected.
     */
    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public int getTargetInputSizeBytes() {
        return mTargetInputSizeBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PerBuyerConfiguration) {
            PerBuyerConfiguration perBuyerConfiguration = (PerBuyerConfiguration) o;
            return Objects.equals(mBuyer, perBuyerConfiguration.mBuyer)
                    && mTargetInputSizeBytes == perBuyerConfiguration.mTargetInputSizeBytes;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBuyer, mTargetInputSizeBytes);
    }

    /** Builder for {@link PerBuyerConfiguration} objects. */
    public static final class Builder {
        @NonNull private AdTechIdentifier mBuyer = null;
        private int mTargetInputSizeBytes;

        /** Creates a new {@link Builder}. */
        public Builder() {}

        /** Sets the buyer for this configuration. See {@link #getBuyer()} for more details. */
        @NonNull
        public Builder setBuyer(@NonNull AdTechIdentifier buyer) {
            Objects.requireNonNull(buyer);
            mBuyer = buyer;
            return this;
        }

        /**
         * Sets the target input size in bytes for this configuration.
         *
         * <p>If this is not explicitly set, this buyer will share remaining space after other
         * buyers' target sizes are respected. See {@link #getTargetInputSizeBytes()} for more
         * details.
         */
        @NonNull
        public Builder setTargetInputSizeBytes(
                @IntRange(from = 0, to = Integer.MAX_VALUE) int targetInputSizeB) {
            mTargetInputSizeBytes = targetInputSizeB;
            return this;
        }

        /** Builds a {@link PerBuyerConfiguration} instance. */
        @NonNull
        public PerBuyerConfiguration build() {
            Objects.requireNonNull(mBuyer);
            if (mTargetInputSizeBytes < 0) {
                throw new IllegalArgumentException("Target input size must be non-negative.");
            }

            return new PerBuyerConfiguration(mBuyer, mTargetInputSizeBytes);
        }
    }
}
