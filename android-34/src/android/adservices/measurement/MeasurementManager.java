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
package android.adservices.measurement;

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION;
import static android.adservices.common.AdServicesStatusUtils.ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE;

import android.adservices.adid.AdId;
import android.adservices.adid.AdIdManager;
import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.CallerMetadata;
import android.adservices.common.SandboxedSdkContextUtils;
import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.app.sdksandbox.SandboxedSdkContext;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.InputEvent;

import androidx.annotation.RequiresApi;

import com.android.adservices.AdServicesCommon;
import com.android.adservices.LogUtil;
import com.android.adservices.ServiceBinder;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** MeasurementManager provides APIs to manage source and trigger registrations. */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public class MeasurementManager {
    /** @hide */
    public static final String MEASUREMENT_SERVICE = "measurement_service";

    /**
     * This state indicates that Measurement APIs are unavailable. Invoking them will result in an
     * {@link UnsupportedOperationException}.
     */
    public static final int MEASUREMENT_API_STATE_DISABLED = 0;

    /**
     * This state indicates that Measurement APIs are enabled.
     */
    public static final int MEASUREMENT_API_STATE_ENABLED = 1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "MEASUREMENT_API_STATE_",
            value = {
                MEASUREMENT_API_STATE_DISABLED,
                MEASUREMENT_API_STATE_ENABLED,
            })
    public @interface MeasurementApiState {}

    private interface MeasurementAdIdCallback {
        void onAdIdCallback(boolean isAdIdEnabled, @Nullable String adIdValue);
    }

    private final long AD_ID_TIMEOUT_MS = 400;

    private Context mContext;
    private ServiceBinder<IMeasurementService> mServiceBinder;
    private AdIdManager mAdIdManager;
    private Executor mAdIdExecutor = Executors.newCachedThreadPool();

    private static final String DEBUG_API_WARNING_MESSAGE =
            "To enable debug api, include ACCESS_ADSERVICES_AD_ID "
                    + "permission and enable advertising ID under device settings";

    /**
     * Factory method for creating an instance of MeasurementManager.
     *
     * @param context The {@link Context} to use
     * @return A {@link MeasurementManager} instance
     */
    @NonNull
     public static MeasurementManager get(@NonNull Context context) {
        // On T+, context.getSystemService() does more than just call constructor.
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? context.getSystemService(MeasurementManager.class)
                : new MeasurementManager(context);
    }

    /**
    * This is for test purposes, it helps to mock the adIdManager.
    *
    * @hide
    */
    @VisibleForTesting
    @NonNull
    public static MeasurementManager get(
            @NonNull Context context, @NonNull AdIdManager adIdManager) {
        MeasurementManager measurementManager = MeasurementManager.get(context);
        measurementManager.mAdIdManager = adIdManager;
        return measurementManager;
    }

    /**
     * Create MeasurementManager.
     *
     * @hide
     */
    public MeasurementManager(Context context) {
        // TODO(b/269798827): Enable for R.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw new IllegalStateException(ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE);
        }

        // In case the MeasurementManager is initiated from inside a sdk_sandbox process the
        // fields will be immediately rewritten by the initialize method below.
        initialize(context);
    }

    /**
     * Initializes {@link MeasurementManager} with the given {@code context}.
     *
     * <p>This method is called by the {@link SandboxedSdkContext} to propagate the correct context.
     * For more information check the javadoc on the {@link
     * android.app.sdksandbox.SdkSandboxSystemServiceRegistry}.
     *
     * @hide
     * @see android.app.sdksandbox.SdkSandboxSystemServiceRegistry
     */
    public MeasurementManager initialize(@NonNull Context context) {
        mContext = context;
        mServiceBinder = ServiceBinder.getServiceBinder(
                context,
                AdServicesCommon.ACTION_MEASUREMENT_SERVICE,
                IMeasurementService.Stub::asInterface);
        mAdIdManager = AdIdManager.get(context);
        return this;
    }

    /**
     * Retrieves an {@link IMeasurementService} implementation
     *
     * @hide
     */
    @VisibleForTesting
    @NonNull
    public IMeasurementService getService() throws IllegalStateException {
        IMeasurementService service = mServiceBinder.getService();
        if (service == null) {
            throw new IllegalStateException("Unable to find the service");
        }
        return service;
    }

    /** Checks if Ad ID permission is enabled. */
    private boolean isAdIdPermissionEnabled(AdId adId) {
        return !AdId.ZERO_OUT.equals(adId.getAdId());
    }

    /**
     * Register an attribution source / trigger.
     *
     * @hide
     */
    private void register(
            @NonNull RegistrationRequest registrationRequest,
            @NonNull IMeasurementService service,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        Objects.requireNonNull(registrationRequest);

        String registrationType = "source";
        if (registrationRequest.getRegistrationType() == RegistrationRequest.REGISTER_TRIGGER) {
            registrationType = "trigger";
        }
        LogUtil.d("Registering " + registrationType);

        try {
            service.register(
                    registrationRequest,
                    generateCallerMetadataWithCurrentTime(),
                    new IMeasurementCallback.Stub() {
                        @Override
                        public void onResult() {
                            if (callback != null && executor != null) {
                                executor.execute(() -> callback.onResult(new Object()));
                            }
                        }

                        @Override
                        public void onFailure(MeasurementErrorResponse failureParcel) {
                            if (callback != null && executor != null) {
                                executor.execute(
                                        () ->
                                                callback.onError(
                                                        AdServicesStatusUtils.asException(
                                                                failureParcel)));
                            }
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            if (callback != null && executor != null) {
                executor.execute(() -> callback.onError(new IllegalStateException(e)));
            }
        }
    }

    /**
     * Register an attribution source (click or view).
     *
     * @param attributionSource the platform issues a request to this URI in order to fetch metadata
     *     associated with the attribution source. The source metadata is stored on device, making
     *     it eligible to be matched to future triggers.
     * @param inputEvent either an {@link InputEvent} object (for a click event) or null (for a view
     *     event).
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerSource(
            @NonNull Uri attributionSource,
            @Nullable InputEvent inputEvent,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        Objects.requireNonNull(attributionSource);

        IMeasurementService service = getServiceWrapper(executor, callback);

        if (service == null) {
            LogUtil.d("Measurement service not found");
            return;
        }

        final RegistrationRequest.Builder builder =
                new RegistrationRequest.Builder(
                                RegistrationRequest.REGISTER_SOURCE,
                                attributionSource,
                                getAppPackageName(),
                                getSdkPackageName())
                        .setRequestTime(SystemClock.uptimeMillis())
                        .setInputEvent(inputEvent);
        // TODO(b/281546062): Can probably remove isAdIdEnabled, since whether adIdValue is null or
        //  not will determine if adId is enabled.
        getAdId(
                (isAdIdEnabled, adIdValue) ->
                        register(
                                builder.setAdIdPermissionGranted(isAdIdEnabled)
                                        .setAdIdValue(adIdValue)
                                        .build(),
                                service,
                                executor,
                                callback));
    }

    /**
     * Register an attribution source(click or view) from web context. This API will not process any
     * redirects, all registration URLs should be supplied with the request. At least one of
     * appDestination or webDestination parameters are required to be provided. If the registration
     * is successful, {@code callback}'s {@link OutcomeReceiver#onResult} is invoked with null. In
     * case of failure, a {@link Exception} is sent through {@code callback}'s {@link
     * OutcomeReceiver#onError}. Both success and failure feedback are executed on the provided
     * {@link Executor}.
     *
     * @param request source registration request
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerWebSource(
            @NonNull WebSourceRegistrationRequest request,
            @Nullable Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        Objects.requireNonNull(request);

        IMeasurementService service = getServiceWrapper(executor, callback);

        if (service == null) {
            LogUtil.d("Measurement service not found");
            return;
        }

        CallerMetadata callerMetadata = generateCallerMetadataWithCurrentTime();
        IMeasurementCallback measurementCallback =
                new IMeasurementCallback.Stub() {
                    @Override
                    public void onResult() {
                        if (callback != null && executor != null) {
                            executor.execute(() -> callback.onResult(new Object()));
                        }
                    }

                    @Override
                    public void onFailure(MeasurementErrorResponse failureParcel) {
                        if (callback != null && executor != null) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    }
                };

        final WebSourceRegistrationRequestInternal.Builder builder =
                new WebSourceRegistrationRequestInternal.Builder(
                        request,
                        getAppPackageName(),
                        getSdkPackageName(),
                        SystemClock.uptimeMillis());

        getAdId(
                (isAdIdEnabled, adIdValue) ->
                        registerWebSourceWrapper(
                                builder.setAdIdPermissionGranted(isAdIdEnabled).build(),
                                service,
                                executor,
                                callerMetadata,
                                measurementCallback,
                                callback));
    }

    /** Wrapper method for registerWebSource. */
    private void registerWebSourceWrapper(
            @NonNull WebSourceRegistrationRequestInternal request,
            @NonNull IMeasurementService service,
            @Nullable Executor executor,
            @NonNull CallerMetadata callerMetadata,
            @NonNull IMeasurementCallback measurementCallback,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        try {
            LogUtil.d("Registering web source");
            service.registerWebSource(request, callerMetadata, measurementCallback);
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            if (callback != null && executor != null) {
                executor.execute(() -> callback.onError(new IllegalStateException(e)));
            }
        }
    }

    /**
     * Register an attribution trigger(click or view) from web context. This API will not process
     * any redirects, all registration URLs should be supplied with the request. If the registration
     * is successful, {@code callback}'s {@link OutcomeReceiver#onResult} is invoked with null. In
     * case of failure, a {@link Exception} is sent through {@code callback}'s {@link
     * OutcomeReceiver#onError}. Both success and failure feedback are executed on the provided
     * {@link Executor}.
     *
     * @param request trigger registration request
     * @param executor used by callback to dispatch results
     * @param callback intended to notify asynchronously the API result
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerWebTrigger(
            @NonNull WebTriggerRegistrationRequest request,
            @Nullable Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        Objects.requireNonNull(request);

        IMeasurementService service = getServiceWrapper(executor, callback);

        if (service == null) {
            LogUtil.d("Measurement service not found");
            return;
        }

        CallerMetadata callerMetadata = generateCallerMetadataWithCurrentTime();
        IMeasurementCallback measurementCallback =
                new IMeasurementCallback.Stub() {
                    @Override
                    public void onResult() {
                        if (callback != null && executor != null) {
                            executor.execute(() -> callback.onResult(new Object()));
                        }
                    }

                    @Override
                    public void onFailure(MeasurementErrorResponse failureParcel) {
                        if (callback != null && executor != null) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(
                                                            failureParcel)));
                        }
                    }
                };

        WebTriggerRegistrationRequestInternal.Builder builder =
                new WebTriggerRegistrationRequestInternal.Builder(
                        request, getAppPackageName(), getSdkPackageName());

        getAdId(
                (isAdIdEnabled, adIdValue) ->
                        registerWebTriggerWrapper(
                                builder.setAdIdPermissionGranted(isAdIdEnabled).build(),
                                service,
                                executor,
                                callerMetadata,
                                measurementCallback,
                                callback));
    }

    /** Wrapper method for registerWebTrigger. */
    private void registerWebTriggerWrapper(
            @NonNull WebTriggerRegistrationRequestInternal request,
            @NonNull IMeasurementService service,
            @Nullable Executor executor,
            @NonNull CallerMetadata callerMetadata,
            @NonNull IMeasurementCallback measurementCallback,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        try {
            LogUtil.d("Registering web trigger");
            service.registerWebTrigger(request, callerMetadata, measurementCallback);
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            if (callback != null && executor != null) {
                executor.execute(() -> callback.onError(new IllegalStateException(e)));
            }
        }
    }

    /**
     * Register a trigger (conversion).
     *
     * @param trigger the API issues a request to this URI to fetch metadata associated with the
     *     trigger. The trigger metadata is stored on-device, and is eligible to be matched with
     *     sources during the attribution process.
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerTrigger(
            @NonNull Uri trigger,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        Objects.requireNonNull(trigger);

        IMeasurementService service = getServiceWrapper(executor, callback);

        if (service == null) {
            LogUtil.d("Measurement service not found");
            return;
        }

        final RegistrationRequest.Builder builder =
                new RegistrationRequest.Builder(
                        RegistrationRequest.REGISTER_TRIGGER,
                        trigger,
                        getAppPackageName(),
                        getSdkPackageName());
        // TODO(b/281546062)
        getAdId(
                (isAdIdEnabled, adIdValue) ->
                        register(
                                builder.setAdIdPermissionGranted(isAdIdEnabled)
                                        .setAdIdValue(adIdValue)
                                        .build(),
                                service,
                                executor,
                                callback));
    }

    /**
     * Delete previously registered data.
     *
     * @hide
     */
    private void deleteRegistrations(
            @NonNull DeletionParam deletionParam,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> callback) {
        Objects.requireNonNull(deletionParam);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        final IMeasurementService service = getServiceWrapper(executor, callback);

        if (service == null) {
            LogUtil.d("Measurement service not found");
            return;
        }

        try {
            service.deleteRegistrations(
                    deletionParam,
                    generateCallerMetadataWithCurrentTime(),
                    new IMeasurementCallback.Stub() {
                        @Override
                        public void onResult() {
                            executor.execute(() -> callback.onResult(new Object()));
                        }

                        @Override
                        public void onFailure(MeasurementErrorResponse failureParcel) {
                            executor.execute(
                                    () -> {
                                        callback.onError(
                                                AdServicesStatusUtils.asException(failureParcel));
                                    });
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            executor.execute(() -> callback.onError(new IllegalStateException(e)));
        }
    }

    /**
     * Delete previous registrations. If the deletion is successful, the callback's {@link
     * OutcomeReceiver#onResult} is invoked with null. In case of failure, a {@link Exception} is
     * sent through the callback's {@link OutcomeReceiver#onError}. Both success and failure
     * feedback are executed on the provided {@link Executor}.
     *
     * @param deletionRequest The request for deleting data.
     * @param executor The executor to run callback.
     * @param callback intended to notify asynchronously the API result.
     */
    public void deleteRegistrations(
            @NonNull DeletionRequest deletionRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> callback) {
        deleteRegistrations(
                new DeletionParam.Builder(
                                deletionRequest.getOriginUris(),
                                deletionRequest.getDomainUris(),
                                deletionRequest.getStart(),
                                deletionRequest.getEnd(),
                                getAppPackageName(),
                                getSdkPackageName())
                        .setDeletionMode(deletionRequest.getDeletionMode())
                        .setMatchBehavior(deletionRequest.getMatchBehavior())
                        .build(),
                executor,
                callback);
    }

    /**
     * Get Measurement API status.
     *
     * <p>The callback's {@code Integer} value is one of {@code MeasurementApiState}.
     *
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void getMeasurementApiStatus(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Integer, Exception> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        final IMeasurementService service;
        try {
            service = getService();
        } catch (IllegalStateException e) {
            LogUtil.e(e, "Failed to bind to measurement service");
            executor.execute(() -> callback.onResult(MEASUREMENT_API_STATE_DISABLED));
            return;
        } catch (RuntimeException e) {
            LogUtil.e(e, "Unknown failure while binding measurement service");
            executor.execute(() -> callback.onError(e));
            return;
        }

        try {
            service.getMeasurementApiStatus(
                    new StatusParam.Builder(getAppPackageName(), getSdkPackageName()).build(),
                    generateCallerMetadataWithCurrentTime(),
                    new IMeasurementApiStatusCallback.Stub() {
                        @Override
                        public void onResult(int result) {
                            executor.execute(() -> callback.onResult(result));
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            executor.execute(() -> callback.onResult(MEASUREMENT_API_STATE_DISABLED));
        } catch (RuntimeException e) {
            LogUtil.e(e, "Unknown failure while getting measurement status");
            executor.execute(() -> callback.onError(e));
        }
    }

    /**
     * If the service is in an APK (as opposed to the system service), unbind it from the service to
     * allow the APK process to die.
     *
     * @hide Not sure if we'll need this functionality in the final API. For now, we need it for
     *     performance testing to simulate "cold-start" situations.
     */
    @VisibleForTesting
    public void unbindFromService() {
        mServiceBinder.unbindFromService();
    }

    /** Returns the package name of the app from the SDK or app context */
    private String getAppPackageName() {
        SandboxedSdkContext sandboxedSdkContext =
                SandboxedSdkContextUtils.getAsSandboxedSdkContext(mContext);
        return sandboxedSdkContext == null
                ? mContext.getPackageName()
                : sandboxedSdkContext.getClientPackageName();
    }

    /** Returns the package name of the sdk from the SDK or empty if no SDK found */
    private String getSdkPackageName() {
        SandboxedSdkContext sandboxedSdkContext =
                SandboxedSdkContextUtils.getAsSandboxedSdkContext(mContext);
        return sandboxedSdkContext == null ? "" : sandboxedSdkContext.getSdkPackageName();
    }

    private CallerMetadata generateCallerMetadataWithCurrentTime() {
        return new CallerMetadata.Builder()
                .setBinderElapsedTimestamp(SystemClock.elapsedRealtime())
                .build();
    }

    /** Get Service wrapper, propagates error to the caller */
    @Nullable
    private IMeasurementService getServiceWrapper(
            @Nullable @CallbackExecutor Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        IMeasurementService service = null;
        try {
            service = getService();
        } catch (RuntimeException e) {
            LogUtil.e(e, "Failed binding to measurement service");
            if (callback != null && executor != null) {
                executor.execute(() -> callback.onError(e));
            }
        }
        return service;
    }

    /* Make AdId call with timeout */
    private void getAdId(MeasurementAdIdCallback measurementAdIdCallback) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicBoolean isAdIdEnabled = new AtomicBoolean();
        AtomicReference<String> adIdValue = new AtomicReference<>();
        mAdIdManager.getAdId(
                mAdIdExecutor,
                new OutcomeReceiver<AdId, Exception>() {
                    @Override
                    public void onResult(AdId adId) {
                        isAdIdEnabled.set(isAdIdPermissionEnabled(adId));
                        adIdValue.set(adId.getAdId().equals(AdId.ZERO_OUT) ? null : adId.getAdId());
                        LogUtil.d("AdId permission enabled %b", isAdIdEnabled.get());
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(Exception error) {
                        boolean isExpected =
                                error instanceof IllegalStateException
                                        || error instanceof SecurityException;
                        if (isExpected) {
                            LogUtil.w(DEBUG_API_WARNING_MESSAGE);
                        } else {
                            LogUtil.w(error, DEBUG_API_WARNING_MESSAGE);
                        }

                        countDownLatch.countDown();
                    }
                });

        boolean timedOut = false;
        try {
            timedOut = !countDownLatch.await(AD_ID_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LogUtil.w(e, "InterruptedException while waiting for AdId");
        }
        if (timedOut) {
            LogUtil.w("AdId call timed out");
        }
        measurementAdIdCallback.onAdIdCallback(isAdIdEnabled.get(), adIdValue.get());
    }
}
