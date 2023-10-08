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

import android.adservices.adselection.AdSelectionCallback;
import android.adservices.adselection.AdSelectionConfig;
import android.adservices.adselection.AdSelectionFromOutcomesConfig;
import android.adservices.adselection.AdSelectionFromOutcomesInput;
import android.adservices.adselection.AdSelectionInput;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.AdSelectionOverrideCallback;
import android.adservices.adselection.BuyersDecisionLogic;
import android.adservices.adselection.RemoveAdCounterHistogramOverrideInput;
import android.adservices.adselection.ReportImpressionCallback;
import android.adservices.adselection.ReportImpressionInput;
import android.adservices.adselection.SetAdCounterHistogramOverrideInput;
import android.adservices.adselection.SetAppInstallAdvertisersCallback;
import android.adservices.adselection.SetAppInstallAdvertisersInput;
import android.adservices.adselection.UpdateAdCounterHistogramCallback;
import android.adservices.adselection.UpdateAdCounterHistogramInput;
import android.adservices.adselection.ReportInteractionCallback;
import android.adservices.adselection.ReportInteractionInput;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.CallerMetadata;
import android.net.Uri;

import java.util.List;

/**
 * This is the Ad Selection Service, which defines the interface used for the Ad selection workflow
 * to orchestrate the on-device execution of
 * 1. Ad selection.
 * 2. Impression reporting.
 * 3. Interaction reporting.
 * 4. Ad event counting.
 *
 * @hide
 */
interface AdSelectionService {
    /**
     * This method orchestrates the buyer and seller side logic to pick the winning ad amongst all
     * the on-device remarketing ad candidates and seller provided contextual ad candidates. It will
     * execuate the ad tech-provided Javascript code based on the following sequence:
     * 1. Buy-side bidding logic execution
     * 2. Buy-side ad filtering and processing(i.e. brand safety check, frequency capping, generated
     *    bid validation etc.)
     * 3. Sell-side decision logic execution to determine a winning ad based on bidding loigic
     * outputs and business logic.
     *
     * An AdSelectionInput is a request object that contains a {@link AdSelectionConfig}
     * and a String callerPackageName
     *
     * The {@link AdSelectionConfig} is provided by the SDK and contains the required information
     * to execute the on-device ad selection and impression reporting.
     *
     * The (@link AdSelectionCallback} returns {@link AdSelectionResponse} if the asynchronous call
     * succeeds.
     * The (@link AdSelectionCallback} returns {@link FledgeErrorResponse} if the asynchronous call
     * fails.
     * If the ad selection is successful, the {@link AdSelectionResponse} contains
     * {@link AdSelectionId} and {@link RenderUri}
     * If the ad selection fails, the response contains only
     * {@link FledgeErrorResponse#RESULT_INTERNAL_ERROR} if an internal server error is encountered,
     * or {@link FledgeErrorResponse#RESULT_INVALID_ARGUMENT} if:
     * 1. The supplied {@link AdSelectionConfig} is invalid
     * 2. The {@link AdSelectionInput#getCallerPackageName} does not match the package name of the
     * application calling this API
     *
     * Otherwise, this call fails to send the response to the callback and throws a RemoteException.
     *
     * {@hide}
     */
    void selectAds(in AdSelectionInput request, in CallerMetadata callerMetadata,
            in AdSelectionCallback callback);

    /**
     * This method allows to select an ad from a list of ads that are winner outcomes of separate
     * runAdSelection. This method will execute a given logic and signals on given list of ads and
     * will return either one or none of the ads.
     *
     * {@code adOutcomes} is a list of {@link AdSelectionOutcome} where each element is a winner
     * from a call to {@code runAdSelection} method.
     * {@code selectionSignals} contains any signals ad-tech wish to use during the decision making.
     * {@code selectionLogicUri} points to a decision logic for selection. There are two types;
     * <lu>
     *     <li>
     *         An HTTPS url to download the java script logic from.
     *     </li>
     *     <li>
     *         A uri that points to the pre-built logics.
     *     </li>
     * </lu>
     *
     *
     * The (@link AdSelectionCallback} returns {@link AdSelectionResponse} if the asynchronous call
     * succeeds.
     * The (@link AdSelectionCallback} returns {@link FledgeErrorResponse} if the asynchronous call
     * fails.
     *
     * If the outcome selection is successful, the {@link AdSelectionResponse} contains
     * {@link AdSelectionId} and {@link RenderUri}
     * If the ad selection fails, the response contains only
     * {@link FledgeErrorResponse#RESULT_INTERNAL_ERROR} if an internal server error is encountered,
     * or {@link FledgeErrorResponse#RESULT_INVALID_ARGUMENT} if:
     * 1. The supplied list of {@link AdSelectionOutcomes} is invalid or
     * 2. The supplied {@link SelectionLogicUri} is invalid.
     *
     * Otherwise, this call fails to send the response to the callback and throws a RemoteException.
     *
     * {@hide}
     */
    void selectAdsFromOutcomes(in AdSelectionFromOutcomesInput inputParams,
            in CallerMetadata callerMetadata, in AdSelectionCallback callback);

    /**
     * Notifies PPAPI that there is a new impression to report for the
     * ad selected by the ad-selection run identified by {@code adSelectionId}.
     * There is no guarantee about when the event will be reported. The event
     * reporting could be delayed and events could be batched.
     *
     * The call will fail with a status of
     * {@link FledgeErrorResponse#STATUS_INVALID_ARGUMENT} if:
     * 1. There is no ad selection matching the provided {@link
     * ReportImpressionInput#getAdSelectionId()}
     * 2. The supplied {@link ReportImpressionInput#getAdSelectionConfig()} is invalid
     * 3. The {@link AdSelectionConfig#getCallerPackageName} does not match the package name of the
     * application calling this API The call will fail with status
     * {@link FledgeErrorResponse#STATUS_INTERNAL_ERROR} if an
     * internal server error is encountered.
     *
     * The reporting guarantee is at-most-once, any error during the connection to
     * the seller and/or buyer reporting URIs might be retried but we won't
     * guarantee the completion.
     *
     * {@hide}
     */
    void reportImpression(in ReportImpressionInput request, in ReportImpressionCallback callback);


    /**
     * Notifies PPAPI that there is a new interaction to report for the
     * ad selected by the ad-selection run identified by {@code adSelectionId}.
     */
    void reportInteraction(in ReportInteractionInput inputParams,
            in ReportInteractionCallback callback);

    /**
     * Updates the counter histograms for the ad event counters associated with a FLEDGE-selected
     * ad.
     */
    void updateAdCounterHistogram(in UpdateAdCounterHistogramInput inputParams,
            in UpdateAdCounterHistogramCallback callback);

    /**
     * This method is intended to be called before {@code runAdSelection}
     * and {@code reportImpression} using the same
     * {@link AdSelectionConfig} in order to configure
     * PPAPI to avoid to fetch info from remote servers and use the
     * data provided.
     *
     * The call will throw a SecurityException if the API hasn't been enabled
     * by developer options or by an adb command or if the calling
     * application manifest is not setting Android:debuggable to true.
     */
    void overrideAdSelectionConfigRemoteInfo(in AdSelectionConfig adSelectionConfig,
            in String decisionLogicJS, in AdSelectionSignals trustedScoringSignals,
            in BuyersDecisionLogic buyersDecisionLogic,
            in AdSelectionOverrideCallback callback);
    /**
     * Gives the provided list of adtechs permission do app install filtering based on the presence
     * of the calling app.
     * @hide
     */
    void setAppInstallAdvertisers(
            in SetAppInstallAdvertisersInput request, in SetAppInstallAdvertisersCallback callback);

    /**
     * Deletes any override created by calling
     * {@code overrideAdSelectionConfigRemoteInfo} for the given
     * AdSelectionConfig
     *
     * The call will throw a SecurityException if:
     * the API hasn't been enabled by developer options or by an adb command
     * or if the calling application manifest is not setting Android:debuggable to true.
     */
    void removeAdSelectionConfigRemoteInfoOverride(
            in AdSelectionConfig adSelectionConfig, in AdSelectionOverrideCallback callback);

    /**
     * Deletes any override created by calling
     * {@code overrideAdSelectionConfigRemoteInfo} from this application
     *
     * The call will throw a SecurityException if:
     * the API hasn't been enabled by developer options or by an adb command
     * or if the calling application manifest is not setting Android:debuggable to true.
     */
    void resetAllAdSelectionConfigRemoteOverrides(in AdSelectionOverrideCallback callback);

    /**
     * This method is intended to be called before {@code selectAdsFromOutcomes}
     * using the same {@link AdSelectionFromOutcomesConfig} in order to configure
     * PPAPI to avoid to fetch info from remote servers and use the
     * data provided.
     *
     * The call will throw a SecurityException if the API hasn't been enabled
     * by developer options or by an adb command or if the calling
     * application manifest is not setting Android:debuggable to true.
     */
    void overrideAdSelectionFromOutcomesConfigRemoteInfo(
            in AdSelectionFromOutcomesConfig adSelectionFromOutcomesConfig,
            in String selectionLogicJs, in AdSelectionSignals selectionSignals,
            in AdSelectionOverrideCallback callback);

    /**
     * Deletes any override created by calling
     * {@code overrideAdSelectionFromOutcomesConfigRemoteInfo} for the given
     * AdSelectionFromOutcomesConfig
     *
     * The call will throw a SecurityException if:
     * the API hasn't been enabled by developer options or by an adb command
     * or if the calling application manifest is not setting Android:debuggable to true.
     */
    void removeAdSelectionFromOutcomesConfigRemoteInfoOverride(
            in AdSelectionFromOutcomesConfig adSelectionFromOutcomesConfig,
            in AdSelectionOverrideCallback callback);

    /**
     * Deletes any override created by calling
     * {@code overrideAdSelectionFromOutcomesConfigRemoteInfo} from this application
     *
     * The call will throw a SecurityException if:
     * the API hasn't been enabled by developer options or by an adb command
     * or if the calling application manifest is not setting Android:debuggable to true.
     */
    void resetAllAdSelectionFromOutcomesConfigRemoteOverrides(
            in AdSelectionOverrideCallback callback);

    /**
     * Manually overrides the histogram to be used in ad selection with the specified event
     * histogram.
     */
    void setAdCounterHistogramOverride(in SetAdCounterHistogramOverrideInput inputParams,
            in AdSelectionOverrideCallback callback);

    /**
     * Removes a previously set histogram override used in ad selection.
     */
    void removeAdCounterHistogramOverride(in RemoveAdCounterHistogramOverrideInput inputParams,
            in AdSelectionOverrideCallback callback);

    /**
     * Removes all previously set histogram overrides used in ad selection which were set by the
     * caller application.
     */
    void resetAllAdCounterHistogramOverrides(in AdSelectionOverrideCallback callback);
}
