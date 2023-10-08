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

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE;

import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.CallerMetadata;
import android.adservices.common.FledgeErrorResponse;
import android.adservices.common.SandboxedSdkContextUtils;
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.app.sdksandbox.SandboxedSdkContext;
import android.content.Context;
import android.os.Build;
import android.os.LimitExceededException;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.TransactionTooLargeException;

import androidx.annotation.RequiresApi;

import com.android.adservices.AdServicesCommon;
import com.android.adservices.LoggerFactory;
import com.android.adservices.ServiceBinder;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/**
 * AdSelection Manager provides APIs for app and ad-SDKs to run ad selection processes as well as
 * report impressions.
 */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public class AdSelectionManager {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getFledgeLogger();
    /**
     * Constant that represents the service name for {@link AdSelectionManager} to be used in {@link
     * android.adservices.AdServicesFrameworkInitializer#registerServiceWrappers}
     *
     * @hide
     */
    public static final String AD_SELECTION_SERVICE = "ad_selection_service";

    @NonNull private Context mContext;
    @NonNull private ServiceBinder<AdSelectionService> mServiceBinder;

    /**
     * Factory method for creating an instance of AdSelectionManager.
     *
     * @param context The {@link Context} to use
     * @return A {@link AdSelectionManager} instance
     */
    @NonNull
    public static AdSelectionManager get(@NonNull Context context) {
        // On T+, context.getSystemService() does more than just call constructor.
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? context.getSystemService(AdSelectionManager.class)
                : new AdSelectionManager(context);
    }

    /**
     * Create AdSelectionManager
     *
     * @hide
     */
    public AdSelectionManager(@NonNull Context context) {
        Objects.requireNonNull(context);

        // In case the AdSelectionManager is initiated from inside a sdk_sandbox process the
        // fields will be immediately rewritten by the initialize method below.
        initialize(context);
    }

    /**
     * Initializes {@link AdSelectionManager} with the given {@code context}.
     *
     * <p>This method is called by the {@link SandboxedSdkContext} to propagate the correct context.
     * For more information check the javadoc on the {@link
     * android.app.sdksandbox.SdkSandboxSystemServiceRegistry}.
     *
     * @hide
     * @see android.app.sdksandbox.SdkSandboxSystemServiceRegistry
     */
    public AdSelectionManager initialize(@NonNull Context context) {
        Objects.requireNonNull(context);

        mContext = context;
        mServiceBinder =
                ServiceBinder.getServiceBinder(
                        context,
                        AdServicesCommon.ACTION_AD_SELECTION_SERVICE,
                        AdSelectionService.Stub::asInterface);
        return this;
    }

    @NonNull
    public TestAdSelectionManager getTestAdSelectionManager() {
        return new TestAdSelectionManager(this);
    }

    @NonNull
    AdSelectionService getService() {
        return mServiceBinder.getService();
    }

    /**
     * Runs the ad selection process on device to select a remarketing ad for the caller
     * application.
     *
     * <p>The input {@code adSelectionConfig} is provided by the Ads SDK and the {@link
     * AdSelectionConfig} object is transferred via a Binder call. For this reason, the total size
     * of these objects is bound to the Android IPC limitations. Failures to transfer the {@link
     * AdSelectionConfig} will throws an {@link TransactionTooLargeException}.
     *
     * <p>The output is passed by the receiver, which either returns an {@link AdSelectionOutcome}
     * for a successful run, or an {@link Exception} includes the type of the exception thrown and
     * the corresponding error message.
     *
     * <p>If the {@link IllegalArgumentException} is thrown, it is caused by invalid input argument
     * the API received to run the ad selection.
     *
     * <p>If the {@link IllegalStateException} is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * <p>If the {@link TimeoutException} is thrown, it is caused when a timeout is encountered
     * during bidding, scoring, or overall selection process to find winning Ad.
     *
     * <p>If the {@link LimitExceededException} is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * <p>If the {@link SecurityException} is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void selectAds(
            @NonNull AdSelectionConfig adSelectionConfig,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<AdSelectionOutcome, Exception> receiver) {
        Objects.requireNonNull(adSelectionConfig);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getService();
            service.selectAds(
                    new AdSelectionInput.Builder()
                            .setAdSelectionConfig(adSelectionConfig)
                            .setCallerPackageName(getCallerPackageName())
                            .build(),
                    new CallerMetadata.Builder()
                            .setBinderElapsedTimestamp(SystemClock.elapsedRealtime())
                            .build(),
                    new AdSelectionCallback.Stub() {
                        @Override
                        public void onSuccess(AdSelectionResponse resultParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onResult(
                                                    new AdSelectionOutcome.Builder()
                                                            .setAdSelectionId(
                                                                    resultParcel.getAdSelectionId())
                                                            .setRenderUri(
                                                                    resultParcel.getRenderUri())
                                                            .build()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () -> {
                                        receiver.onError(
                                                AdServicesStatusUtils.asException(failureParcel));
                                    });
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Failure of AdSelection service.");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Selects an ad from the results of previously ran ad selections.
     *
     * <p>The input {@code adSelectionFromOutcomesConfig} is provided by the Ads SDK and the {@link
     * AdSelectionFromOutcomesConfig} object is transferred via a Binder call. For this reason, the
     * total size of these objects is bound to the Android IPC limitations. Failures to transfer the
     * {@link AdSelectionFromOutcomesConfig} will throws an {@link TransactionTooLargeException}.
     *
     * <p>The output is passed by the receiver, which either returns an {@link AdSelectionOutcome}
     * for a successful run, or an {@link Exception} includes the type of the exception thrown and
     * the corresponding error message.
     *
     * <p>The input {@code adSelectionFromOutcomesConfig} contains:
     *
     * <ul>
     *   <li>{@code Seller} is required to be a registered {@link
     *       android.adservices.common.AdTechIdentifier}. Otherwise, {@link IllegalStateException}
     *       will be thrown.
     *   <li>{@code List of ad selection ids} should exist and come from {@link
     *       AdSelectionManager#selectAds} calls originated from the same application. Otherwise,
     *       {@link IllegalArgumentException} for input validation will raise listing violating ad
     *       selection ids.
     *   <li>{@code Selection logic URI} that could follow either the HTTPS or Ad Selection Prebuilt
     *       schemas.
     *       <p>If the URI follows HTTPS schema then the host should match the {@code seller}.
     *       Otherwise, {@link IllegalArgumentException} will be thrown.
     *       <p>Prebuilt URIs are a way of substituting a generic pre-built logics for the required
     *       JavaScripts for {@code selectOutcome}. Prebuilt Uri for this endpoint should follow;
     *       <ul>
     *         <li>{@code
     *             ad-selection-prebuilt://ad-selection-from-outcomes/<name>?<script-generation-parameters>}
     *       </ul>
     *       <p>If an unsupported prebuilt URI is passed or prebuilt URI feature is disabled by the
     *       service then {@link IllegalArgumentException} will be thrown.
     *       <p>See {@link AdSelectionFromOutcomesConfig.Builder#setSelectionLogicUri} for supported
     *       {@code <name>} and required {@code <script-generation-parameters>}.
     * </ul>
     *
     * <p>If the {@link IllegalArgumentException} is thrown, it is caused by invalid input argument
     * the API received to run the ad selection.
     *
     * <p>If the {@link IllegalStateException} is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * <p>If the {@link TimeoutException} is thrown, it is caused when a timeout is encountered
     * during bidding, scoring, or overall selection process to find winning Ad.
     *
     * <p>If the {@link LimitExceededException} is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * <p>If the {@link SecurityException} is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * @hide
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void selectAds(
            @NonNull AdSelectionFromOutcomesConfig adSelectionFromOutcomesConfig,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<AdSelectionOutcome, Exception> receiver) {
        Objects.requireNonNull(adSelectionFromOutcomesConfig);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getService();
            service.selectAdsFromOutcomes(
                    new AdSelectionFromOutcomesInput.Builder()
                            .setAdSelectionFromOutcomesConfig(adSelectionFromOutcomesConfig)
                            .setCallerPackageName(getCallerPackageName())
                            .build(),
                    new CallerMetadata.Builder()
                            .setBinderElapsedTimestamp(SystemClock.elapsedRealtime())
                            .build(),
                    new AdSelectionCallback.Stub() {
                        @Override
                        public void onSuccess(AdSelectionResponse resultParcel) {
                            executor.execute(
                                    () -> {
                                        if (resultParcel == null) {
                                            receiver.onResult(AdSelectionOutcome.NO_OUTCOME);
                                        } else {
                                            receiver.onResult(
                                                    new AdSelectionOutcome.Builder()
                                                            .setAdSelectionId(
                                                                    resultParcel.getAdSelectionId())
                                                            .setRenderUri(
                                                                    resultParcel.getRenderUri())
                                                            .build());
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () -> {
                                        receiver.onError(
                                                AdServicesStatusUtils.asException(failureParcel));
                                    });
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Failure of AdSelection service.");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Notifies the service that there is a new impression to report for the ad selected by the
     * ad-selection run identified by {@code adSelectionId}. There is no guarantee about when the
     * impression will be reported. The impression reporting could be delayed and reports could be
     * batched.
     *
     * <p>To calculate the winning seller reporting URL, the service fetches the seller's JavaScript
     * logic from the {@link AdSelectionConfig#getDecisionLogicUri()} found at {@link
     * ReportImpressionRequest#getAdSelectionConfig()}. Then, the service executes one of the
     * functions found in the seller JS called {@code reportResult}, providing on-device signals as
     * well as {@link ReportImpressionRequest#getAdSelectionConfig()} as input parameters.
     *
     * <p>The function definition of {@code reportResult} is:
     *
     * <p>{@code function reportResult(ad_selection_config, render_url, bid, contextual_signals) {
     * return { 'status': status, 'results': {'signals_for_buyer': signals_for_buyer,
     * 'reporting_url': reporting_url } }; } }
     *
     * <p>To calculate the winning buyer reporting URL, the service fetches the winning buyer's
     * JavaScript logic which is fetched via the buyer's {@link
     * android.adservices.customaudience.CustomAudience#getBiddingLogicUri()}. Then, the service
     * executes one of the functions found in the buyer JS called {@code reportWin}, providing
     * on-device signals, {@code signals_for_buyer} calculated by {@code reportResult}, and specific
     * fields from {@link ReportImpressionRequest#getAdSelectionConfig()} as input parameters.
     *
     * <p>The function definition of {@code reportWin} is:
     *
     * <p>{@code function reportWin(ad_selection_signals, per_buyer_signals, signals_for_buyer,
     * contextual_signals, custom_audience_reporting_signals) { return {'status': 0, 'results':
     * {'reporting_url': reporting_url } }; } }
     *
     * <p>The output is passed by the {@code receiver}, which either returns an empty {@link Object}
     * for a successful run, or an {@link Exception} includes the type of the exception thrown and
     * the corresponding error message.
     *
     * <p>If the {@link IllegalArgumentException} is thrown, it is caused by invalid input argument
     * the API received to report the impression.
     *
     * <p>If the {@link IllegalStateException} is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * <p>If the {@link LimitExceededException} is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * <p>If the {@link SecurityException} is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * <p>Impressions will be reported at most once as a best-effort attempt.
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void reportImpression(
            @NonNull ReportImpressionRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getService();
            service.reportImpression(
                    new ReportImpressionInput.Builder()
                            .setAdSelectionId(request.getAdSelectionId())
                            .setAdSelectionConfig(request.getAdSelectionConfig())
                            .setCallerPackageName(getCallerPackageName())
                            .build(),
                    new ReportImpressionCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () -> {
                                        receiver.onError(
                                                AdServicesStatusUtils.asException(failureParcel));
                                    });
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Notifies PPAPI that there is a new interaction to report for the ad selected by the
     * ad-selection run identified by {@code adSelectionId}. There is no guarantee about when the
     * interaction will be reported. The interaction reporting could be delayed and interactions
     * could be batched.
     *
     * <p>The output is passed by the receiver, which either returns an empty {@link Object} for a
     * successful run, or an {@link Exception} includes the type of the exception thrown and the
     * corresponding error message.
     *
     * <p>If the {@link IllegalArgumentException} is thrown, it is caused by invalid input argument
     * the API received to report the interaction.
     *
     * <p>If the {@link IllegalStateException} is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * <p>If the {@link LimitExceededException} is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * <p>If the {@link SecurityException} is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * <p>Interactions will be reported at most once as a best-effort attempt.
     *
     * @hide
     */
    // TODO(b/261812140): Unhide for report interaction API review
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void reportInteraction(
            @NonNull ReportInteractionRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getService();
            service.reportInteraction(
                    new ReportInteractionInput.Builder()
                            .setAdSelectionId(request.getAdSelectionId())
                            .setInteractionKey(request.getInteractionKey())
                            .setInteractionData(request.getInteractionData())
                            .setReportingDestinations(request.getReportingDestinations())
                            .setCallerPackageName(getCallerPackageName())
                            .build(),
                    new ReportInteractionCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () -> {
                                        receiver.onError(
                                                AdServicesStatusUtils.asException(failureParcel));
                                    });
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Gives the provided list of adtechs the ability to do app install filtering on the calling
     * app.
     *
     * <p>The input {@code request} is provided by the Ads SDK and the {@code request} object is
     * transferred via a Binder call. For this reason, the total size of these objects is bound to
     * the Android IPC limitations. Failures to transfer the {@code advertisers} will throws an
     * {@link TransactionTooLargeException}.
     *
     * <p>The output is passed by the receiver, which either returns an empty {@link Object} for a
     * successful run, or an {@link Exception} includes the type of the exception thrown and the
     * corresponding error message.
     *
     * <p>If the {@link IllegalArgumentException} is thrown, it is caused by invalid input argument
     * the API received.
     *
     * <p>If the {@link IllegalStateException} is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * <p>If the {@link LimitExceededException} is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * <p>If the {@link SecurityException} is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * @hide
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void setAppInstallAdvertisers(
            @NonNull SetAppInstallAdvertisersRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getService();
            service.setAppInstallAdvertisers(
                    new SetAppInstallAdvertisersInput.Builder()
                            .setAdvertisers(request.getAdvertisers())
                            .setCallerPackageName(getCallerPackageName())
                            .build(),
                    new SetAppInstallAdvertisersCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () -> {
                                        receiver.onError(
                                                AdServicesStatusUtils.asException(failureParcel));
                                    });
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service.");
            receiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service.", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Failure of AdSelection service.", e));
        }
    }

    /**
     * Updates the counter histograms for an ad which was previously selected by a call to {@link
     * #selectAds(AdSelectionConfig, Executor, OutcomeReceiver)}.
     *
     * <p>The counter histograms are used in ad selection to inform frequency cap filtering on
     * candidate ads, where ads whose frequency caps are met or exceeded are removed from the
     * bidding process during ad selection.
     *
     * <p>Counter histograms can only be updated for ads specified by the given {@code
     * adSelectionId} returned by a recent call to FLEDGE ad selection from the same caller app.
     *
     * <p>A {@link SecurityException} is returned via the {@code outcomeReceiver} if:
     *
     * <ol>
     *   <li>the app has not declared the correct permissions in its manifest, or
     *   <li>the app or entity identified by the {@code callerAdTechIdentifier} are not authorized
     *       to use the API.
     * </ol>
     *
     * An {@link IllegalStateException} is returned via the {@code outcomeReceiver} if the call does
     * not come from an app with a foreground activity.
     *
     * <p>A {@link LimitExceededException} is returned via the {@code outcomeReceiver} if the call
     * exceeds the calling app's API throttle.
     *
     * <p>In all other failure cases, the {@code outcomeReceiver} will return an empty {@link
     * Object}. Note that to protect user privacy, internal errors will not be sent back via an
     * exception.
     *
     * @hide
     */
    // TODO(b/221876775): Unhide for frequency cap API review
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void updateAdCounterHistogram(
            @NonNull UpdateAdCounterHistogramRequest updateAdCounterHistogramRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> outcomeReceiver) {
        Objects.requireNonNull(updateAdCounterHistogramRequest, "Request must not be null");
        Objects.requireNonNull(executor, "Executor must not be null");
        Objects.requireNonNull(outcomeReceiver, "Outcome receiver must not be null");

        try {
            final AdSelectionService service = Objects.requireNonNull(getService());
            service.updateAdCounterHistogram(
                    new UpdateAdCounterHistogramInput.Builder()
                            .setAdEventType(updateAdCounterHistogramRequest.getAdEventType())
                            .setAdSelectionId(updateAdCounterHistogramRequest.getAdSelectionId())
                            .setCallerAdTech(updateAdCounterHistogramRequest.getCallerAdTech())
                            .setCallerPackageName(getCallerPackageName())
                            .build(),
                    new UpdateAdCounterHistogramCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> outcomeReceiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () -> {
                                        outcomeReceiver.onError(
                                                AdServicesStatusUtils.asException(failureParcel));
                                    });
                        }
                    });
        } catch (NullPointerException e) {
            sLogger.e(e, "Unable to find the AdSelection service");
            outcomeReceiver.onError(
                    new IllegalStateException("Unable to find the AdSelection service", e));
        } catch (RemoteException e) {
            sLogger.e(e, "Remote exception encountered while updating ad counter histogram");
            outcomeReceiver.onError(new IllegalStateException("Failure of AdSelection service", e));
        }
    }

    private String getCallerPackageName() {
        SandboxedSdkContext sandboxedSdkContext =
                SandboxedSdkContextUtils.getAsSandboxedSdkContext(mContext);
        return sandboxedSdkContext == null
                ? mContext.getPackageName()
                : sandboxedSdkContext.getClientPackageName();
    }
}
