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
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;

import java.util.Objects;

/**
 * Represent input params to the GetAdSelectionData API.
 *
 * @hide
 */
public final class GetAdSelectionDataInput implements Parcelable {
    @Nullable private final AdTechIdentifier mSeller;
    @NonNull private final String mCallerPackageName;

    @Nullable private final Uri mCoordinatorOriginUri;
    @Nullable private final SellerConfiguration mSellerConfiguration;

    @NonNull
    public static final Creator<GetAdSelectionDataInput> CREATOR =
            new Creator<>() {
                public GetAdSelectionDataInput createFromParcel(Parcel in) {
                    return new GetAdSelectionDataInput(in);
                }

                public GetAdSelectionDataInput[] newArray(int size) {
                    return new GetAdSelectionDataInput[size];
                }
            };

    private GetAdSelectionDataInput(
            @Nullable AdTechIdentifier seller,
            @NonNull String callerPackageName,
            @Nullable Uri coordinatorOriginUri,
            @Nullable SellerConfiguration sellerConfiguration) {
        Objects.requireNonNull(callerPackageName);

        this.mSeller = seller;
        this.mCallerPackageName = callerPackageName;
        this.mCoordinatorOriginUri = coordinatorOriginUri;
        this.mSellerConfiguration = sellerConfiguration;
    }

    private GetAdSelectionDataInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        this.mSeller =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, AdTechIdentifier.CREATOR::createFromParcel);
        this.mCallerPackageName = in.readString();
        this.mCoordinatorOriginUri =
                AdServicesParcelableUtil.readNullableFromParcel(in, Uri.CREATOR::createFromParcel);
        this.mSellerConfiguration =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, SellerConfiguration.CREATOR::createFromParcel);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GetAdSelectionDataInput) {
            GetAdSelectionDataInput obj = (GetAdSelectionDataInput) o;
            return Objects.equals(mSeller, obj.mSeller)
                    && Objects.equals(mCallerPackageName, obj.mCallerPackageName)
                    && Objects.equals(mCoordinatorOriginUri, obj.mCoordinatorOriginUri)
                    && Objects.equals(mSellerConfiguration, obj.mSellerConfiguration);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mSeller, mCallerPackageName, mCoordinatorOriginUri, mSellerConfiguration);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mSeller,
                (targetParcel, sourceSignals) -> sourceSignals.writeToParcel(targetParcel, flags));
        dest.writeString(mCallerPackageName);
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mCoordinatorOriginUri,
                (targetParcel, sourceOrigin) -> sourceOrigin.writeToParcel(targetParcel, flags));
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mSellerConfiguration,
                (targetParcel, sourceOrigin) -> sourceOrigin.writeToParcel(targetParcel, flags));
    }

    /**
     * @return a AdTechIdentifier of the seller, for example "www.example-ssp.com"
     */
    @Nullable
    public AdTechIdentifier getSeller() {
        return mSeller;
    }

    /**
     * @return the caller package name
     */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    /**
     * @return the caller package name
     */
    @Nullable
    public Uri getCoordinatorOriginUri() {
        return mCoordinatorOriginUri;
    }

    /**
     * Returns the seller configuration that the calling SDK can use to optimize the payload. If
     * this is null, the service will default to the existing strategy of sending all the available
     * data.
     */
    @Nullable
    public SellerConfiguration getSellerConfiguration() {
        return mSellerConfiguration;
    }

    /**
     * Builder for {@link GetAdSelectionDataInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        @Nullable private AdTechIdentifier mSeller;
        @Nullable private String mCallerPackageName;
        @Nullable private Uri mCoordinatorOrigin;
        @Nullable private SellerConfiguration mSellerConfiguration;

        public Builder() {}

        /** Sets the seller {@link AdTechIdentifier}. */
        @NonNull
        public GetAdSelectionDataInput.Builder setSeller(@Nullable AdTechIdentifier seller) {
            this.mSeller = seller;
            return this;
        }

        /** Sets the caller's package name. */
        @NonNull
        public GetAdSelectionDataInput.Builder setCallerPackageName(
                @NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);

            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /** Sets the coordinator origin URI . */
        @NonNull
        public GetAdSelectionDataInput.Builder setCoordinatorOriginUri(
                @Nullable Uri coordinatorOrigin) {
            this.mCoordinatorOrigin = coordinatorOrigin;
            return this;
        }

        /**
         * Sets the {@link SellerConfiguration} See {@link #getSellerConfiguration()} for more
         * details.
         *
         * @hide
         */
        @NonNull
        public GetAdSelectionDataInput.Builder setSellerConfiguration(
                @Nullable SellerConfiguration sellerConfiguration) {
            this.mSellerConfiguration = sellerConfiguration;
            return this;
        }

        /**
         * Builds a {@link GetAdSelectionDataInput} instance.
         *
         * @throws NullPointerException if the CallerPackageName is null
         */
        @NonNull
        public GetAdSelectionDataInput build() {
            Objects.requireNonNull(mCallerPackageName);

            return new GetAdSelectionDataInput(
                    mSeller, mCallerPackageName, mCoordinatorOrigin, mSellerConfiguration);
        }
    }
}
