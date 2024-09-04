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
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;
import com.android.internal.util.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents input params to the PersistAdSelectionResult API.
 *
 * @hide
 */
public final class PersistAdSelectionResultInput implements Parcelable {
    private final long mAdSelectionId;
    @Nullable private final AdTechIdentifier mSeller;
    @Nullable private final byte[] mAdSelectionResult;
    @NonNull private final String mCallerPackageName;

    @NonNull
    public static final Creator<PersistAdSelectionResultInput> CREATOR =
            new Creator<>() {
                public PersistAdSelectionResultInput createFromParcel(Parcel in) {
                    return new PersistAdSelectionResultInput(in);
                }

                public PersistAdSelectionResultInput[] newArray(int size) {
                    return new PersistAdSelectionResultInput[size];
                }
            };

    private PersistAdSelectionResultInput(
            long adSelectionId,
            @Nullable AdTechIdentifier seller,
            @Nullable byte[] adSelectionResult,
            @NonNull String callerPackageName) {
        Objects.requireNonNull(callerPackageName);

        this.mAdSelectionId = adSelectionId;
        this.mSeller = seller;
        this.mAdSelectionResult = adSelectionResult;
        this.mCallerPackageName = callerPackageName;
    }

    private PersistAdSelectionResultInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        this.mAdSelectionId = in.readLong();
        this.mSeller =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, AdTechIdentifier.CREATOR::createFromParcel);
        this.mAdSelectionResult = in.createByteArray();
        this.mCallerPackageName = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistAdSelectionResultInput) {
            PersistAdSelectionResultInput obj = (PersistAdSelectionResultInput) o;
            return mAdSelectionId == obj.mAdSelectionId
                    && Objects.equals(mSeller, obj.mSeller)
                    && Arrays.equals(mAdSelectionResult, obj.mAdSelectionResult)
                    && Objects.equals(mCallerPackageName, obj.mCallerPackageName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mAdSelectionId, mSeller, Arrays.hashCode(mAdSelectionResult), mCallerPackageName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        dest.writeLong(mAdSelectionId);
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mSeller,
                (targetParcel, sourceSignals) -> sourceSignals.writeToParcel(targetParcel, flags));
        dest.writeByteArray(mAdSelectionResult);
        dest.writeString(mCallerPackageName);
    }

    /**
     * @return an ad selection id.
     */
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /**
     * @return a seller.
     */
    @Nullable
    public AdTechIdentifier getSeller() {
        return mSeller;
    }

    /**
     * @return an ad selection result.
     */
    @Nullable
    public byte[] getAdSelectionResult() {
        if (Objects.isNull(mAdSelectionResult)) {
            return null;
        } else {
            return Arrays.copyOf(mAdSelectionResult, mAdSelectionResult.length);
        }
    }

    /**
     * @return the caller package name
     */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    /**
     * Builder for {@link PersistAdSelectionResultInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        private long mAdSelectionId;
        @Nullable private AdTechIdentifier mSeller;
        @Nullable private byte[] mAdSelectionResult;
        @Nullable private String mCallerPackageName;

        public Builder() {}

        /** Sets the ad selection id {@link Long}. */
        @NonNull
        public PersistAdSelectionResultInput.Builder setAdSelectionId(long adSelectionId) {
            this.mAdSelectionId = adSelectionId;
            return this;
        }

        /** Sets the seller {@link AdTechIdentifier}. */
        @NonNull
        public PersistAdSelectionResultInput.Builder setSeller(@Nullable AdTechIdentifier seller) {
            this.mSeller = seller;
            return this;
        }

        /** Sets the ad selection result {@link String}. */
        @NonNull
        public PersistAdSelectionResultInput.Builder setAdSelectionResult(
                @Nullable byte[] adSelectionResult) {
            if (!Objects.isNull(adSelectionResult)) {
                this.mAdSelectionResult =
                        Arrays.copyOf(adSelectionResult, adSelectionResult.length);
            } else {
                this.mAdSelectionResult = null;
            }
            return this;
        }

        /** Sets the caller's package name. */
        @NonNull
        public PersistAdSelectionResultInput.Builder setCallerPackageName(
                @NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);

            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /**
         * Builds a {@link PersistAdSelectionResultInput} instance.
         *
         * @throws IllegalArgumentException if the adSelectionId is not set
         * @throws NullPointerException if the CallerPackageName is null
         */
        @NonNull
        public PersistAdSelectionResultInput build() {
            Objects.requireNonNull(mCallerPackageName);
            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

            return new PersistAdSelectionResultInput(
                    mAdSelectionId, mSeller, mAdSelectionResult, mCallerPackageName);
        }
    }
}
