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
import android.annotation.NonNull;

import java.util.Objects;
import java.util.Set;

/**
 * Represents input parameters to the setAppInstallAdvertiser API.
 *
 * @hide
 */
public class SetAppInstallAdvertisersRequest {
    @NonNull private final Set<AdTechIdentifier> mAdvertisers;

    public SetAppInstallAdvertisersRequest(@NonNull Set<AdTechIdentifier> advertisers) {
        Objects.requireNonNull(advertisers);

        mAdvertisers = advertisers;
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
}
