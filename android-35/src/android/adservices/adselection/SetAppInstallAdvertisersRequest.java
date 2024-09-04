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

import android.adservices.common.AdTechIdentifier;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.adservices.flags.Flags;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Represents input parameters to the setAppInstallAdvertiser API. */
@FlaggedApi(Flags.FLAG_FLEDGE_AD_SELECTION_FILTERING_ENABLED)
public class SetAppInstallAdvertisersRequest {
    @NonNull private final Set<AdTechIdentifier> mAdvertisers;

    private SetAppInstallAdvertisersRequest(@NonNull Set<AdTechIdentifier> advertisers) {
        Objects.requireNonNull(advertisers);

        mAdvertisers = new HashSet<>(advertisers);
    }

    /**
     * Returns the set of advertisers that will be able to run app install filters based on this
     * app's presence on the device after a call to SetAppInstallAdvertisers is made with this as
     * input.
     */
    @NonNull
    public Set<AdTechIdentifier> getAdvertisers() {
        return mAdvertisers;
    }

    public static final class Builder {
        @Nullable private Set<AdTechIdentifier> mAdvertisers;

        public Builder() {}

        /**
         * Sets list of allowed advertisers. See {@link SetAppInstallAdvertisersRequest
         * #getAdvertisers()}
         */
        @NonNull
        public SetAppInstallAdvertisersRequest.Builder setAdvertisers(
                @NonNull Set<AdTechIdentifier> advertisers) {
            Objects.requireNonNull(advertisers);

            mAdvertisers = new HashSet<>(advertisers);
            return this;
        }

        /** Builds a {@link SetAppInstallAdvertisersRequest} instance. */
        @NonNull
        public SetAppInstallAdvertisersRequest build() {
            return new SetAppInstallAdvertisersRequest(mAdvertisers);
        }
    }
}
