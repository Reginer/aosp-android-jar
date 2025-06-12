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

import static com.android.adservices.flags.Flags.FLAG_FLEDGE_GET_AD_SELECTION_DATA_SELLER_CONFIGURATION_ENABLED;

import android.adservices.common.AdTechIdentifier;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;

import com.android.adservices.flags.Flags;

/**
 * Represents a request containing the information to get ad selection data.
 *
 * <p>Instances of this class are created by SDKs to be provided as arguments to the {@link
 * AdSelectionManager#getAdSelectionData} methods in {@link AdSelectionManager}.
 */
public final class GetAdSelectionDataRequest {
    @Nullable private final AdTechIdentifier mSeller;

    @Nullable private final Uri mCoordinatorOriginUri;

    @Nullable private final SellerConfiguration mSellerConfiguration;

    private GetAdSelectionDataRequest(
            @Nullable AdTechIdentifier seller,
            @Nullable Uri coordinatorOriginUri,
            @Nullable SellerConfiguration sellerConfiguration) {
        this.mSeller = seller;
        this.mCoordinatorOriginUri = coordinatorOriginUri;
        this.mSellerConfiguration = sellerConfiguration;
    }

    /**
     * @return a AdTechIdentifier of the seller, for example "www.example-ssp.com"
     */
    @Nullable
    public AdTechIdentifier getSeller() {
        return mSeller;
    }

    /**
     * @return the coordinator origin Uri where the public keys for encryption are fetched from
     *     <p>See {@link Builder#setCoordinatorOriginUri(Uri)} for more details on the coordinator
     *     origin
     */
    @Nullable
    @FlaggedApi(Flags.FLAG_FLEDGE_SERVER_AUCTION_MULTI_CLOUD_ENABLED)
    public Uri getCoordinatorOriginUri() {
        return mCoordinatorOriginUri;
    }

    /**
     * Returns the seller ad tech's requested payload configuration, set by the calling SDK, to
     * optimize the payload.
     *
     * <p>If this is {@code null}, the service will send all data available.
     */
    @FlaggedApi(FLAG_FLEDGE_GET_AD_SELECTION_DATA_SELLER_CONFIGURATION_ENABLED)
    @Nullable
    public SellerConfiguration getSellerConfiguration() {
        return mSellerConfiguration;
    }

    /**
     * Builder for {@link GetAdSelectionDataRequest} objects.
     */
    public static final class Builder {
        @Nullable private AdTechIdentifier mSeller;

        @Nullable private Uri mCoordinatorOriginUri;

        @Nullable private SellerConfiguration mSellerConfiguration;

        public Builder() {}

        /** Sets the seller {@link AdTechIdentifier}. */
        @NonNull
        public GetAdSelectionDataRequest.Builder setSeller(@Nullable AdTechIdentifier seller) {
            this.mSeller = seller;
            return this;
        }

        /**
         * Sets the coordinator origin from which PPAPI should fetch the public key for payload
         * encryption. The origin must use HTTPS URI.
         *
         * <p>The origin will only contain the scheme, hostname and port of the URL. If the origin
         * is not provided or is null, PPAPI will use the default coordinator URI.
         *
         * <p>The origin must belong to a list of pre-approved coordinator origins. Otherwise,
         * {@link AdSelectionManager#getAdSelectionData} will throw an IllegalArgumentException
         */
        @NonNull
        @FlaggedApi(Flags.FLAG_FLEDGE_SERVER_AUCTION_MULTI_CLOUD_ENABLED)
        public GetAdSelectionDataRequest.Builder setCoordinatorOriginUri(
                @Nullable Uri coordinatorOriginUri) {
            this.mCoordinatorOriginUri = coordinatorOriginUri;
            return this;
        }

        /**
         * Builds a {@link GetAdSelectionDataRequest} instance.
         */
        @NonNull
        public GetAdSelectionDataRequest build() {
            return new GetAdSelectionDataRequest(
                    mSeller, mCoordinatorOriginUri, mSellerConfiguration);
        }

        /**
         * Sets the {@link SellerConfiguration}. See {@link #getSellerConfiguration()} for more
         * details.
         */
        @FlaggedApi(FLAG_FLEDGE_GET_AD_SELECTION_DATA_SELLER_CONFIGURATION_ENABLED)
        @NonNull
        public GetAdSelectionDataRequest.Builder setSellerConfiguration(
                @Nullable SellerConfiguration sellerConfiguration) {
            this.mSellerConfiguration = sellerConfiguration;
            return this;
        }
    }
}
