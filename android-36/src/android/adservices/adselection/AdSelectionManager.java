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

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_AD_SELECTION;
import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE;
import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_PROTECTED_SIGNALS;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.adservices.adid.AdId;
import android.adservices.adid.AdIdManager;
import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.AssetFileDescriptorUtil;
import android.adservices.common.CallerMetadata;
import android.adservices.common.FledgeErrorResponse;
import android.adservices.common.SandboxedSdkContextUtils;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.app.sdksandbox.SandboxedSdkContext;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
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
import com.android.adservices.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AdSelection Manager provides APIs for app and ad-SDKs to run ad selection processes as well as
 * report impressions.
 */
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

    private static final long AD_ID_TIMEOUT_MS = 400;
    private static final String DEBUG_API_WARNING_MESSAGE =
            "To enable debug api, include ACCESS_ADSERVICES_AD_ID "
                    + "permission and enable advertising ID under device settings";
    private final Executor mAdIdExecutor = Executors.newCachedThreadPool();
    @NonNull private Context mContext;
    @NonNull private ServiceBinder<AdSelectionService> mServiceBinder;
    @NonNull private AdIdManager mAdIdManager;
    @NonNull private ServiceProvider mServiceProvider;

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
     * Factory method for creating an instance of AdSelectionManager.
     *
     * <p>Note: This is for testing only.
     *
     * @param context The {@link Context} to use
     * @param adIdManager The {@link AdIdManager} instance to use
     * @param adSelectionService The {@link AdSelectionService} instance to use
     * @return A {@link AdSelectionManager} instance
     * @hide
     */
    @VisibleForTesting
    @NonNull
    public static AdSelectionManager get(
            @NonNull Context context,
            @NonNull AdIdManager adIdManager,
            @NonNull AdSelectionService adSelectionService) {
        AdSelectionManager adSelectionManager = AdSelectionManager.get(context);
        adSelectionManager.mAdIdManager = adIdManager;
        adSelectionManager.mServiceProvider = () -> adSelectionService;
        return adSelectionManager;
    }

    /**
     * Create AdSelectionManager
     *
     * @hide
     */
    public AdSelectionManager(@NonNull Context context) {
        Objects.requireNonNull(context);

        // Initialize the default service provider
        mServiceProvider = this::doGetService;

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
        mAdIdManager = AdIdManager.get(context);
        return this;
    }

    @NonNull
    public TestAdSelectionManager getTestAdSelectionManager() {
        return new TestAdSelectionManager(this);
    }

    /**
     * Using this interface {@code getService}'s implementation is decoupled from the default {@link
     * #doGetService()}. This allows us to inject mock instances of {@link AdSelectionService} to
     * inspect and test the manager-service boundary.
     */
    interface ServiceProvider {
        @NonNull
        AdSelectionService getService();
    }

    @NonNull
    ServiceProvider getServiceProvider() {
        return mServiceProvider;
    }

    @NonNull
    AdSelectionService doGetService() {
        return mServiceBinder.getService();
    }

    /**
     * Collects custom audience data from device. Returns a compressed and encrypted blob to send to
     * auction servers for ad selection. For more details, please visit <a
     * href="https://developer.android.com/design-for-safety/privacy-sandbox/protected-audience-bidding-and-auction-services">Bidding
     * and Auction Services Explainer</a>.
     *
     * <p>Custom audience ads must have a {@code ad_render_id} to be eligible for to be collected.
     *
     * <p>See {@link AdSelectionManager#persistAdSelectionResult} for how to process the results of
     * the ad selection run on server-side with the blob generated by this API.
     *
     * <p>The output is passed by the receiver, which either returns an {@link
     * GetAdSelectionDataOutcome} for a successful run, or an {@link Exception} includes the type of
     * the exception thrown and the corresponding error message.
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
    @RequiresPermission(
            anyOf = {
                ACCESS_ADSERVICES_CUSTOM_AUDIENCE,
                ACCESS_ADSERVICES_PROTECTED_SIGNALS,
                ACCESS_ADSERVICES_AD_SELECTION
            })
    public void getAdSelectionData(
            @NonNull GetAdSelectionDataRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<GetAdSelectionDataOutcome, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);
        try {
            final AdSelectionService service = getServiceProvider().getService();
            service.getAdSelectionData(
                    new GetAdSelectionDataInput.Builder()
                            .setSeller(request.getSeller())
                            .setCallerPackageName(getCallerPackageName())
                            .setCoordinatorOriginUri(request.getCoordinatorOriginUri())
                            .setSellerConfiguration(request.getSellerConfiguration())
                            .build(),
                    new CallerMetadata.Builder()
                            .setBinderElapsedTimestamp(SystemClock.elapsedRealtime())
                            .build(),
                    new GetAdSelectionDataCallback.Stub() {
                        @Override
                        public void onSuccess(GetAdSelectionDataResponse resultParcel) {
                            executor.execute(
                                    () -> {
                                        byte[] adSelectionData;
                                        try {
                                            adSelectionData = getAdSelectionData(resultParcel);
                                        } catch (IOException e) {
                                            receiver.onError(
                                                    new IllegalStateException(
                                                            "Unable to return the AdSelectionData",
                                                            e));
                                            return;
                                        }
                                        receiver.onResult(
                                                new GetAdSelectionDataOutcome.Builder()
                                                        .setAdSelectionId(
                                                                resultParcel.getAdSelectionId())
                                                        .setAdSelectionData(adSelectionData)
                                                        .build());
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
     * Persists the ad selection results from the server-side. For more details, please visit <a
     * href="https://developer.android.com/design-for-safety/privacy-sandbox/protected-audience-bidding-and-auction-services">Bidding
     * and Auction Services Explainer</a>
     *
     * <p>See {@link AdSelectionManager#getAdSelectionData} for how to generate an encrypted blob to
     * run an ad selection on the server side.
     *
     * <p>The output is passed by the receiver, which either returns an {@link AdSelectionOutcome}
     * for a successful run, or an {@link Exception} includes the type of the exception thrown and
     * the corresponding error message. The {@link AdSelectionOutcome#getAdSelectionId()} is not
     * guaranteed to be the same as the {@link
     * PersistAdSelectionResultRequest#getAdSelectionDataId()} or the deprecated {@link
     * PersistAdSelectionResultRequest#getAdSelectionId()}.
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
    @RequiresPermission(
            anyOf = {
                ACCESS_ADSERVICES_CUSTOM_AUDIENCE,
                ACCESS_ADSERVICES_PROTECTED_SIGNALS,
                ACCESS_ADSERVICES_AD_SELECTION
            })
    public void persistAdSelectionResult(
            @NonNull PersistAdSelectionResultRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<AdSelectionOutcome, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getServiceProvider().getService();
            service.persistAdSelectionResult(
                    new PersistAdSelectionResultInput.Builder()
                            .setSeller(request.getSeller())
                            .setAdSelectionId(request.getAdSelectionId())
                            .setAdSelectionResult(request.getAdSelectionResult())
                            .setCallerPackageName(getCallerPackageName())
                            .build(),
                    new CallerMetadata.Builder()
                            .setBinderElapsedTimestamp(SystemClock.elapsedRealtime())
                            .build(),
                    new PersistAdSelectionResultCallback.Stub() {
                        @Override
                        public void onSuccess(PersistAdSelectionResultResponse resultParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onResult(
                                                    new AdSelectionOutcome.Builder()
                                                            .setAdSelectionId(
                                                                    resultParcel.getAdSelectionId())
                                                            .setRenderUri(
                                                                    resultParcel.getAdRenderUri())
                                                            .setWinningSeller(
                                                                    resultParcel.getWinningSeller())
                                                            .setComponentAdUris(
                                                                    resultParcel
                                                                            .getComponentAdUris())
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
     * Runs the ad selection process on device to select a remarketing ad for the caller
     * application.
     *
     * <p>The input {@code adSelectionConfig} is provided by the Ads SDK and the {@link
     * AdSelectionConfig} object is transferred via a Binder call. For this reason, the total size
     * of these objects is bound to the Android IPC limitations. Failures to transfer the {@link
     * AdSelectionConfig} will throws an {@link TransactionTooLargeException}.
     *
     * <p>The input {@code adSelectionConfig} contains {@code Decision Logic Uri} that could follow
     * either the HTTPS or Ad Selection Prebuilt schemas.
     *
     * <p>If the URI follows HTTPS schema then the host should match the {@code seller}. Otherwise,
     * {@link IllegalArgumentException} will be thrown.
     *
     * <p>Prebuilt URIs are a way of substituting a generic pre-built logics for the required
     * JavaScripts for {@code scoreAds}. Prebuilt Uri for this endpoint should follow;
     *
     * <ul>
     *   <li>{@code ad-selection-prebuilt://ad-selection/<name>?<script-generation-parameters>}
     * </ul>
     *
     * <p>If an unsupported prebuilt URI is passed or prebuilt URI feature is disabled by the
     * service then {@link IllegalArgumentException} will be thrown.
     *
     * <p>See {@link AdSelectionConfig.Builder#setDecisionLogicUri} for supported {@code <name>} and
     * required {@code <script-generation-parameters>}.
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
    @RequiresPermission(
            anyOf = {
                ACCESS_ADSERVICES_CUSTOM_AUDIENCE,
                ACCESS_ADSERVICES_PROTECTED_SIGNALS,
                ACCESS_ADSERVICES_AD_SELECTION
            })
    public void selectAds(
            @NonNull AdSelectionConfig adSelectionConfig,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<AdSelectionOutcome, Exception> receiver) {
        Objects.requireNonNull(adSelectionConfig);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getServiceProvider().getService();
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
                                                            .setWinningSeller(
                                                                    resultParcel.getWinningSeller())
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
     */
    @RequiresPermission(
            anyOf = {
                ACCESS_ADSERVICES_CUSTOM_AUDIENCE,
                ACCESS_ADSERVICES_PROTECTED_SIGNALS,
                ACCESS_ADSERVICES_AD_SELECTION
            })
    public void selectAds(
            @NonNull AdSelectionFromOutcomesConfig adSelectionFromOutcomesConfig,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<AdSelectionOutcome, Exception> receiver) {
        Objects.requireNonNull(adSelectionFromOutcomesConfig);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getServiceProvider().getService();
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
                                                            .setWinningSeller(
                                                                    resultParcel.getWinningSeller())
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
     * <p>In addition, buyers and sellers have the option to register to receive reports on specific
     * ad events. To do so, they can invoke the platform provided {@code registerAdBeacon} function
     * inside {@code reportWin} and {@code reportResult} for buyers and sellers, respectively.
     *
     * <p>The function definition of {@code registerBeacon} is:
     *
     * <p>{@code function registerAdBeacon(beacons)}, where {@code beacons} is a dict of string to
     * string pairs
     *
     * <p>For each ad event a buyer/seller is interested in reports for, they would add an {@code
     * event_key}: {@code event_reporting_uri} pair to the {@code beacons} dict, where {@code
     * event_key} is an identifier for that specific event. This {@code event_key} should match
     * {@link ReportEventRequest#getKey()} when the SDK invokes {@link #reportEvent}. In addition,
     * each {@code event_reporting_uri} should parse properly into a {@link android.net.Uri}. This
     * will be the {@link android.net.Uri} reported to when the SDK invokes {@link #reportEvent}.
     *
     * <p>When the buyer/seller has added all the pairings they want to receive events for, they can
     * invoke {@code registerAdBeacon(beacons)}, where {@code beacons} is the name of the dict they
     * added the pairs to.
     *
     * <p>{@code registerAdBeacon} will throw a {@code TypeError} in these situations:
     *
     * <ol>
     *   <li>{@code registerAdBeacon}is called more than once. If this error is caught in
     *       reportWin/reportResult, the original set of pairings will be registered
     *   <li>{@code registerAdBeacon} doesn't have exactly 1 dict argument.
     *   <li>The contents of the 1 dict argument are not all {@code String: String} pairings.
     * </ol>
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
    @RequiresPermission(
            anyOf = {
                ACCESS_ADSERVICES_CUSTOM_AUDIENCE,
                ACCESS_ADSERVICES_PROTECTED_SIGNALS,
                ACCESS_ADSERVICES_AD_SELECTION
            })
    public void reportImpression(
            @NonNull ReportImpressionRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getServiceProvider().getService();
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
     * Notifies the service that there is a new ad event to report for the ad selected by the
     * ad-selection run identified by {@code adSelectionId}. An ad event is any occurrence that
     * happens to an ad associated with the given {@code adSelectionId}. There is no guarantee about
     * when the ad event will be reported. The event reporting could be delayed and reports could be
     * batched.
     *
     * <p>Using {@link ReportEventRequest#getKey()}, the service will fetch the {@code reportingUri}
     * that was registered in {@code registerAdBeacon}. See documentation of {@link
     * #reportImpression} for more details regarding {@code registerAdBeacon}. Then, the service
     * will attach {@link ReportEventRequest#getData()} to the request body of a POST request and
     * send the request. The body of the POST request will have the {@code content-type} of {@code
     * text/plain}, and the data will be transmitted in {@code charset=UTF-8}.
     *
     * <p>The output is passed by the receiver, which either returns an empty {@link Object} for a
     * successful run, or an {@link Exception} includes the type of the exception thrown and the
     * corresponding error message.
     *
     * <p>If the {@link IllegalArgumentException} is thrown, it is caused by invalid input argument
     * the API received to report the ad event.
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
     * <p>Events will be reported at most once as a best-effort attempt.
     */
    @RequiresPermission(
            anyOf = {
                ACCESS_ADSERVICES_CUSTOM_AUDIENCE,
                ACCESS_ADSERVICES_PROTECTED_SIGNALS,
                ACCESS_ADSERVICES_AD_SELECTION
            })
    public void reportEvent(
            @NonNull ReportEventRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);
        try {
            ReportInteractionInput.Builder inputBuilder =
                    new ReportInteractionInput.Builder()
                            .setAdSelectionId(request.getAdSelectionId())
                            .setInteractionKey(request.getKey())
                            .setInteractionData(request.getData())
                            .setReportingDestinations(request.getReportingDestinations())
                            .setCallerPackageName(getCallerPackageName())
                            .setCallerSdkName(getCallerSdkName())
                            .setInputEvent(request.getInputEvent());

            getAdId((adIdValue) -> inputBuilder.setAdId(adIdValue));

            final AdSelectionService service = getServiceProvider().getService();
            service.reportInteraction(
                    inputBuilder.build(),
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
     */
    @FlaggedApi(Flags.FLAG_FLEDGE_AD_SELECTION_FILTERING_ENABLED)
    @RequiresPermission(
            anyOf = {
                ACCESS_ADSERVICES_CUSTOM_AUDIENCE,
                ACCESS_ADSERVICES_PROTECTED_SIGNALS,
                ACCESS_ADSERVICES_AD_SELECTION
            })
    public void setAppInstallAdvertisers(
            @NonNull SetAppInstallAdvertisersRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final AdSelectionService service = getServiceProvider().getService();
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
     */
    @RequiresPermission(
            anyOf = {
                ACCESS_ADSERVICES_CUSTOM_AUDIENCE,
                ACCESS_ADSERVICES_PROTECTED_SIGNALS,
                ACCESS_ADSERVICES_AD_SELECTION
            })
    public void updateAdCounterHistogram(
            @NonNull UpdateAdCounterHistogramRequest updateAdCounterHistogramRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> outcomeReceiver) {
        Objects.requireNonNull(updateAdCounterHistogramRequest, "Request must not be null");
        Objects.requireNonNull(executor, "Executor must not be null");
        Objects.requireNonNull(outcomeReceiver, "Outcome receiver must not be null");

        try {
            final AdSelectionService service = getServiceProvider().getService();
            Objects.requireNonNull(service);
            service.updateAdCounterHistogram(
                    new UpdateAdCounterHistogramInput.Builder(
                                    updateAdCounterHistogramRequest.getAdSelectionId(),
                                    updateAdCounterHistogramRequest.getAdEventType(),
                                    updateAdCounterHistogramRequest.getCallerAdTech(),
                                    getCallerPackageName())
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

    private byte[] getAdSelectionData(GetAdSelectionDataResponse response) throws IOException {
        if (Objects.nonNull(response.getAssetFileDescriptor())) {
            AssetFileDescriptor assetFileDescriptor = response.getAssetFileDescriptor();
            return AssetFileDescriptorUtil.readAssetFileDescriptorIntoBuffer(assetFileDescriptor);
        } else {
            return response.getAdSelectionData();
        }
    }

    private String getCallerSdkName() {
        SandboxedSdkContext sandboxedSdkContext =
                SandboxedSdkContextUtils.getAsSandboxedSdkContext(mContext);
        return sandboxedSdkContext == null ? "" : sandboxedSdkContext.getSdkPackageName();
    }

    private interface AdSelectionAdIdCallback {
        void onResult(@Nullable String adIdValue);
    }

    @SuppressLint("MissingPermission")
    private void getAdId(AdSelectionAdIdCallback adSelectionAdIdCallback) {
        try {
            CountDownLatch timer = new CountDownLatch(1);
            AtomicReference<String> adIdValue = new AtomicReference<>();
            mAdIdManager.getAdId(
                    mAdIdExecutor,
                    new android.adservices.common.AdServicesOutcomeReceiver<>() {
                        @Override
                        public void onResult(AdId adId) {
                            String id = adId.getAdId();
                            adIdValue.set(!AdId.ZERO_OUT.equals(id) ? id : null);
                            sLogger.v("AdId permission enabled: %b.", !AdId.ZERO_OUT.equals(id));
                            timer.countDown();
                        }

                        @Override
                        public void onError(Exception e) {
                            if (e instanceof IllegalStateException
                                    || e instanceof SecurityException) {
                                sLogger.w(DEBUG_API_WARNING_MESSAGE);
                            } else {
                                sLogger.w(e, DEBUG_API_WARNING_MESSAGE);
                            }
                            timer.countDown();
                        }
                    });

            boolean timedOut = false;
            try {
                timedOut = !timer.await(AD_ID_TIMEOUT_MS, MILLISECONDS);
            } catch (InterruptedException e) {
                sLogger.w(e, "Interrupted while getting the AdId.");
            }
            if (timedOut) {
                sLogger.w("AdId call timed out.");
            }
            adSelectionAdIdCallback.onResult(adIdValue.get());
        } catch (Exception e) {
            sLogger.d(e, "Could not get AdId.");
            adSelectionAdIdCallback.onResult(null);
        }
    }
}
