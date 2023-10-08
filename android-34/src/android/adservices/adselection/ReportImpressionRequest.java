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

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * Represent input parameters to the reportImpression API.
 */
public class ReportImpressionRequest {
    private final long mAdSelectionId;
    @NonNull private final AdSelectionConfig mAdSelectionConfig;

    public ReportImpressionRequest(
            long adSelectionId, @NonNull AdSelectionConfig adSelectionConfig) {
        Objects.requireNonNull(adSelectionConfig);
        Preconditions.checkArgument(
                adSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

        mAdSelectionId = adSelectionId;
        mAdSelectionConfig = adSelectionConfig;
    }

    /** Returns the adSelectionId, one of the inputs to {@link ReportImpressionRequest} */
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /** Returns the adSelectionConfig, one of the inputs to {@link ReportImpressionRequest} */
    @NonNull
    public AdSelectionConfig getAdSelectionConfig() {
        return mAdSelectionConfig;
    }
}
