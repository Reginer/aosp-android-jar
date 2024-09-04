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
import android.os.OutcomeReceiver;

import com.android.internal.util.Preconditions;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Represent input parameters to the reportImpression API.
 */
public class ReportImpressionRequest {
    private final long mAdSelectionId;
    @NonNull private final AdSelectionConfig mAdSelectionConfig;

    /**
     * Ctor for on-device ad selection reporting request.
     *
     * <p>If your {@code adSelectionId} is for a on-device auction run using {@link
     * AdSelectionManager#selectAds(AdSelectionConfig, Executor, OutcomeReceiver)} then your
     * impression reporting request must include your {@link AdSelectionConfig}.
     *
     * @param adSelectionId received from {@link AdSelectionManager#selectAds(AdSelectionConfig,
     *     Executor, OutcomeReceiver)}
     * @param adSelectionConfig same {@link AdSelectionConfig} used to trigger {@link
     *     AdSelectionManager#selectAds(AdSelectionConfig, Executor, OutcomeReceiver)}
     */
    public ReportImpressionRequest(
            long adSelectionId, @NonNull AdSelectionConfig adSelectionConfig) {
        Objects.requireNonNull(adSelectionConfig);
        Preconditions.checkArgument(
                adSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

        mAdSelectionId = adSelectionId;
        mAdSelectionConfig = adSelectionConfig;
    }

    /**
     * Ctor for auction server ad selection reporting request.
     *
     * <p>If your {@code adSelectionId} is for a server auction run where device info collected by
     * {@link AdSelectionManager#getAdSelectionData} then your impression reporting request should
     * only include the ad selection id.
     *
     * <p>{@link AdSelectionManager#persistAdSelectionResult} must be called with the encrypted
     * result blob from servers before making impression reporting request.
     *
     * @param adSelectionId received from {@link AdSelectionManager#getAdSelectionData}
     */
    public ReportImpressionRequest(long adSelectionId) {
        Preconditions.checkArgument(
                adSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

        mAdSelectionId = adSelectionId;
        mAdSelectionConfig = AdSelectionConfig.EMPTY;
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
