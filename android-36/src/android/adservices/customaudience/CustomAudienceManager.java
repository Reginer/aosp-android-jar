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

package android.adservices.customaudience;

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE;

import static com.android.adservices.flags.Flags.FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED;

import android.adservices.common.AdServicesOutcomeReceiver;
import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.AdTechIdentifier;
import android.adservices.common.FledgeErrorResponse;
import android.adservices.common.SandboxedSdkContextUtils;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.app.sdksandbox.SandboxedSdkContext;
import android.content.Context;
import android.os.Build;
import android.os.LimitExceededException;
import android.os.OutcomeReceiver;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import com.android.adservices.AdServicesCommon;
import com.android.adservices.LoggerFactory;
import com.android.adservices.ServiceBinder;

import java.util.Objects;
import java.util.concurrent.Executor;

/** CustomAudienceManager provides APIs for app and ad-SDKs to join / leave custom audiences. */
@RequiresApi(Build.VERSION_CODES.S)
public class CustomAudienceManager {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getFledgeLogger();
    /**
     * Constant that represents the service name for {@link CustomAudienceManager} to be used in
     * {@link android.adservices.AdServicesFrameworkInitializer#registerServiceWrappers}
     *
     * @hide
     */
    public static final String CUSTOM_AUDIENCE_SERVICE = "custom_audience_service";

    @NonNull private Context mContext;
    @NonNull private ServiceBinder<ICustomAudienceService> mServiceBinder;

    /**
     * Factory method for creating an instance of CustomAudienceManager.
     *
     * @param context The {@link Context} to use
     * @return A {@link CustomAudienceManager} instance
     */
    @NonNull
    public static CustomAudienceManager get(@NonNull Context context) {
        // On T+, context.getSystemService() does more than just call constructor.
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? context.getSystemService(CustomAudienceManager.class)
                : new CustomAudienceManager(context);
    }

    /**
     * Create a service binder CustomAudienceManager
     *
     * @hide
     */
    public CustomAudienceManager(@NonNull Context context) {
        Objects.requireNonNull(context);

        // In case the CustomAudienceManager is initiated from inside a sdk_sandbox process the
        // fields will be immediately rewritten by the initialize method below.
        initialize(context);
    }

    /**
     * Initializes {@link CustomAudienceManager} with the given {@code context}.
     *
     * <p>This method is called by the {@link SandboxedSdkContext} to propagate the correct context.
     * For more information check the javadoc on the {@link
     * android.app.sdksandbox.SdkSandboxSystemServiceRegistry}.
     *
     * @hide
     * @see android.app.sdksandbox.SdkSandboxSystemServiceRegistry
     */
    public CustomAudienceManager initialize(@NonNull Context context) {
        Objects.requireNonNull(context);

        mContext = context;
        mServiceBinder =
                ServiceBinder.getServiceBinder(
                        context,
                        AdServicesCommon.ACTION_CUSTOM_AUDIENCE_SERVICE,
                        ICustomAudienceService.Stub::asInterface);
        return this;
    }

    /** Create a service with test-enabling APIs */
    @NonNull
    public TestCustomAudienceManager getTestCustomAudienceManager() {
        return new TestCustomAudienceManager(this, getCallerPackageName());
    }

    @NonNull
    ICustomAudienceService getService() {
        ICustomAudienceService service = mServiceBinder.getService();
        if (service == null) {
            throw new IllegalStateException("custom audience service is not available.");
        }
        return service;
    }

    /**
     * Adds the user to the given {@link CustomAudience}.
     *
     * <p>An attempt to register the user for a custom audience with the same combination of {@code
     * ownerPackageName}, {@code buyer}, and {@code name} will cause the existing custom audience's
     * information to be overwritten, including the list of ads data.
     *
     * <p>Note that the ads list can be completely overwritten by the daily background fetch job.
     *
     * <p>This call fails with an {@link SecurityException} if
     *
     * <ol>
     *   <li>the {@code ownerPackageName} is not calling app's package name and/or
     *   <li>the buyer is not authorized to use the API.
     * </ol>
     *
     * <p>This call fails with an {@link IllegalArgumentException} if
     *
     * <ol>
     *   <li>the storage limit has been exceeded by the calling application and/or
     *   <li>any URI parameters in the {@link CustomAudience} given are not authenticated with the
     *       {@link CustomAudience} buyer.
     * </ol>
     *
     * <p>This call fails with {@link LimitExceededException} if the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * <p>This call fails with an {@link IllegalStateException} if an internal service error is
     * encountered.
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void joinCustomAudience(
            @NonNull JoinCustomAudienceRequest joinCustomAudienceRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(joinCustomAudienceRequest);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        final CustomAudience customAudience = joinCustomAudienceRequest.getCustomAudience();

        try {
            final ICustomAudienceService service = getService();

            service.joinCustomAudience(
                    customAudience,
                    getCallerPackageName(),
                    new ICustomAudienceCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Internal Error!", e));
        }
    }

    /**
     * Adds the user to the {@link CustomAudience} fetched from a {@code fetchUri}.
     *
     * <p>An attempt to register the user for a custom audience with the same combination of {@code
     * ownerPackageName}, {@code buyer}, and {@code name} will cause the existing custom audience's
     * information to be overwritten, including the list of ads data.
     *
     * <p>Note that the ads list can be completely overwritten by the daily background fetch job.
     *
     * <p>This call fails with an {@link SecurityException} if
     *
     * <ol>
     *   <li>the {@code ownerPackageName} is not calling app's package name and/or
     *   <li>the buyer is not authorized to use the API.
     * </ol>
     *
     * <p>This call fails with an {@link IllegalArgumentException} if
     *
     * <ol>
     *   <li>the storage limit has been exceeded by the calling application and/or
     *   <li>any URI parameters in the {@link CustomAudience} given are not authenticated with the
     *       {@link CustomAudience} buyer.
     * </ol>
     *
     * <p>This call fails with {@link LimitExceededException} if the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * <p>This call fails with an {@link IllegalStateException} if an internal service error is
     * encountered.
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void fetchAndJoinCustomAudience(
            @NonNull FetchAndJoinCustomAudienceRequest fetchAndJoinCustomAudienceRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(fetchAndJoinCustomAudienceRequest);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final ICustomAudienceService service = getService();

            service.fetchAndJoinCustomAudience(
                    new FetchAndJoinCustomAudienceInput.Builder(
                                    fetchAndJoinCustomAudienceRequest.getFetchUri(),
                                    getCallerPackageName())
                            .setName(fetchAndJoinCustomAudienceRequest.getName())
                            .setActivationTime(
                                    fetchAndJoinCustomAudienceRequest.getActivationTime())
                            .setExpirationTime(
                                    fetchAndJoinCustomAudienceRequest.getExpirationTime())
                            .setUserBiddingSignals(
                                    fetchAndJoinCustomAudienceRequest.getUserBiddingSignals())
                            .build(),
                    new FetchAndJoinCustomAudienceCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Internal Error!", e));
        }
    }

    /**
     * Attempts to remove a user from a custom audience by deleting any existing {@link
     * CustomAudience} data, identified by {@code ownerPackageName}, {@code buyer}, and {@code
     * name}.
     *
     * <p>This call fails with an {@link SecurityException} if
     *
     * <ol>
     *   <li>the {@code ownerPackageName} is not calling app's package name; and/or
     *   <li>the buyer is not authorized to use the API.
     * </ol>
     *
     * <p>This call fails with {@link LimitExceededException} if the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * <p>This call does not inform the caller whether the custom audience specified existed in
     * on-device storage. In other words, it will fail silently when a buyer attempts to leave a
     * custom audience that was not joined.
     */
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void leaveCustomAudience(
            @NonNull LeaveCustomAudienceRequest leaveCustomAudienceRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(leaveCustomAudienceRequest);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        final AdTechIdentifier buyer = leaveCustomAudienceRequest.getBuyer();
        final String name = leaveCustomAudienceRequest.getName();

        try {
            final ICustomAudienceService service = getService();

            service.leaveCustomAudience(
                    getCallerPackageName(),
                    buyer,
                    name,
                    new ICustomAudienceCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });
        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Internal Error!", e));
        }
    }

    /**
     * Allows the API caller to schedule a deferred Custom Audience update. For each update the user
     * will be able to join or leave a set of CustomAudiences.
     *
     * <p>This API only guarantees minimum delay to make the update, and does not guarantee a
     * maximum deadline within which the update request would be made. Scheduled updates could be
     * batched and queued together to preserve system resources, thus exact delay time is not
     * guaranteed.
     *
     * <p>If the provided {@code shouldReplacePendingUpdates} is true, all the currently scheduled
     * pending updates matching the {@code owner} i.e. calling app and {@code buyer} inferred from
     * Update Uri will be deleted.
     *
     * <p>In order to conserve system resources the API will make and update request only if the
     * following constraints are satisfied
     *
     * <ol>
     *   <li>The device is using an un-metered internet connection
     *   <li>The device battery is not low
     *   <li>The device storage is not low
     * </ol>
     *
     * <p>When the deferred update is triggered the API makes a POST request to the provided
     * updateUri with the request body containing a JSON of Partial Custom Audience list.
     *
     * <p>An example of request body containing list of Partial Custom Audiences would look like:
     *
     * <pre>{@code
     * {
     *     "partial_custom_audience_data": [
     *         {
     *             "name": "running_shoes",
     *             "activation_time": 1644375856883,
     *             "expiration_time": 1644375908397
     *         },
     *         {
     *             "name": "casual_shirt",
     *             "user_bidding_signals": {
     *                 "signal1": "value1"
     *             }
     *         }
     *     ]
     * }
     * }</pre>
     *
     * <p>In response the API expects a JSON in return with following keys:
     *
     * <ol>
     *   <li>"join" : Should contain list containing full data for a {@link CustomAudience} object
     *   <li>"leave" : List of {@link CustomAudience} names that user is intended to be removed from
     * </ol>
     *
     * <p>An example of JSON in response would look like:
     *
     * <pre>{@code
     * {
     *     "join": [
     *         {
     *             "name": "running-shoes",
     *             "activation_time": 1680603133,
     *             "expiration_time": 1680803133,
     *             "user_bidding_signals": {
     *                 "signal1": "value"
     *             },
     *             "trusted_bidding_data": {
     *                 "trusted_bidding_uri": "https://example-dsp.com/",
     *                 "trusted_bidding_keys": [
     *                     "k1",
     *                     "k2"
     *                 ]
     *             },
     *             "bidding_logic_uri": "https://example-dsp.com/...",
     *             "ads": [
     *                 {
     *                     "render_uri": "https://example-dsp.com/...",
     *                     "metadata": {},
     *                     "ad_filters": {
     *                         "frequency_cap": {
     *                             "win": [
     *                                 {
     *                                     "ad_counter_key": "key1",
     *                                     "max_count": 2,
     *                                     "interval_in_seconds": 60
     *                                 }
     *                             ],
     *                             "view": [
     *                                 {
     *                                     "ad_counter_key": "key2",
     *                                     "max_count": 10,
     *                                     "interval_in_seconds": 3600
     *                                 }
     *                             ]
     *                         },
     *                         "app_install": {
     *                             "package_names": [
     *                                 "package.name.one"
     *                             ]
     *                         }
     *                     }
     *                 }
     *             ]
     *         },
     *         {}
     *     ],
     *     "leave": [
     *         "tennis_shoes",
     *         "formal_shirt"
     *     ]
     * }
     * }</pre>
     *
     * <p>An attempt to register the user for a custom audience from the same application with the
     * same combination of {@code buyer} inferred from Update Uri, and {@code name} will cause the
     * existing custom audience's information to be overwritten, including the list of ads data.
     *
     * <p>In case information related to any of the CustomAudience to be joined is malformed, the
     * deferred update would silently ignore that single Custom Audience
     *
     * <p>When removing this API attempts to remove a user from a custom audience by deleting any
     * existing {@link CustomAudience} identified by owner i.e. calling app, {@code buyer} inferred
     * from Update Uri, and {@code name}
     *
     * <p>Any partial custom audience field set by the caller cannot be overridden by the custom
     * audience fetched from the {@code updateUri}. Given multiple Custom Audiences could be
     * returned by a buyer ad tech we will match the override restriction based on the names of the
     * Custom Audiences. A buyer may skip returning a full Custom Audience for any Partial Custom
     * Audience in request.
     *
     * <p>In case the API encounters transient errors while making the network call for update, like
     * 5xx, connection timeout, rate limit exceeded it would employ retries, with backoff up to a
     * 'retry limit' number of times. The API would also honor 'retry-after' header specifying the
     * min amount of seconds by which the next request should be delayed.
     *
     * <p>In a scenario where server responds with a '429 status code', signifying 'Too many
     * requests', API would place the deferred update and other updates for the same requester i.e.
     * caller package and buyer combination, in a quarantine. The quarantine records would be
     * referred before making any calls for requesters, and request will only be made once the
     * quarantine period has expired. The applications can leverage the `retry-after` header to
     * self-quarantine for traffic management to their servers and prevent being overwhelmed with
     * requests. The default quarantine value will be set to 30 minutes.
     *
     * <p>This call fails with an {@link SecurityException} if
     *
     * <ol>
     *   <li>the {@code ownerPackageName} is not calling app's package name; and/or
     *   <li>the buyer, inferred from {@code updateUri}, is not authorized to use the API.
     * </ol>
     *
     * <p>This call fails with an {@link IllegalArgumentException} if
     *
     * <ol>
     *   <li>the provided {@code updateUri} is invalid or malformed.
     *   <li>the provided {@code delayTime} is not within permissible bounds
     *   <li>the combined size of {@code partialCustomAudience} list is larger than allowed limits
     * </ol>
     *
     * <p>This call fails with {@link LimitExceededException} if the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * <p>This call fails with {@link IllegalStateException} if the provided {@code
     * shouldReplacePendingUpdates} is false, and there exists a pending update in the queue.
     */
    @FlaggedApi(FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED)
    @RequiresPermission(ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public void scheduleCustomAudienceUpdate(
            @NonNull ScheduleCustomAudienceUpdateRequest request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final ICustomAudienceService service = getService();
            service.scheduleCustomAudienceUpdate(
                    new ScheduleCustomAudienceUpdateInput.Builder(
                                    request.getUpdateUri(),
                                    getCallerPackageName(),
                                    request.getMinDelay(),
                                    request.getPartialCustomAudienceList())
                            .setShouldReplacePendingUpdates(request.shouldReplacePendingUpdates())
                            .build(),
                    new ScheduleCustomAudienceUpdateCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            executor.execute(() -> receiver.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(FledgeErrorResponse failureParcel) {
                            executor.execute(
                                    () ->
                                            receiver.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    });

        } catch (RemoteException e) {
            sLogger.e(e, "Exception");
            receiver.onError(new IllegalStateException("Internal Error!", e));
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
