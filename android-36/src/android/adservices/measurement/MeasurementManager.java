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
import static android.adservices.common.AndroidRCommonUtil.invokeCallbackOnErrorOnRvc;

import android.adservices.common.AdServicesOutcomeReceiver;
import android.adservices.common.OutcomeReceiverConverter;
import android.adservices.exceptions.AdServicesException;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.app.sdksandbox.SandboxedSdkContext;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.OutcomeReceiver;
import android.view.InputEvent;

import androidx.annotation.RequiresApi;

import com.android.adservices.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.Executor;

/** MeasurementManager provides APIs to manage source and trigger registrations. */
@SuppressWarnings("NewApi")
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

    private MeasurementCompatibleManager mImpl;

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
     * Create MeasurementManager.
     *
     * @hide
     */
    public MeasurementManager(Context context) {
        // In case the MeasurementManager is initiated from inside a sdk_sandbox process the
        // fields will be immediately rewritten by the initialize method below.
        initialize(context);
    }

    /**
     * Creates MeasurementManager
     *
     * @param compatibleManager the underlying implementation that can be mocked for tests
     * @hide
     */
    @VisibleForTesting
    public MeasurementManager(@NonNull MeasurementCompatibleManager compatibleManager) {
        Objects.requireNonNull(compatibleManager);
        mImpl = compatibleManager;
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
        mImpl = MeasurementCompatibleManager.get(context);
        return this;
    }

    /**
     * Registers an attribution source (click or view).
     *
     * @param attributionSource the platform issues a request to this URI in order to fetch metadata
     *     associated with the attribution source. The source metadata is stored on device, making
     *     it eligible to be matched to future triggers.
     * @param inputEvent either an {@link InputEvent} object (for a click event) or null (for a view
     *     event).
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     * @throws IllegalArgumentException if the scheme for {@code attributionSource} is not HTTPS
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerSource(
            @NonNull Uri attributionSource,
            @Nullable InputEvent inputEvent,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        mImpl.registerSource(attributionSource, inputEvent, executor, callback);
    }

    /**
     * Registers an attribution source (click or view). For use on Android R or lower.
     *
     * @param attributionSource the platform issues a request to this URI in order to fetch metadata
     *     associated with the attribution source. The source metadata is stored on device, making
     *     it eligible to be matched to future triggers.
     * @param inputEvent either an {@link InputEvent} object (for a click event) or null (for a view
     *     event).
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     * @deprecated use {@link #registerSource(Uri, InputEvent, Executor, OutcomeReceiver)} instead.
     *     Android R is no longer supported.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    public void registerSource(
            @NonNull Uri attributionSource,
            @Nullable InputEvent inputEvent,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable AdServicesOutcomeReceiver<Object, Exception> callback) {
        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        mImpl.registerSource(
                attributionSource,
                inputEvent,
                executor,
                OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Registers attribution sources(click or view) from an app context.
     *
     * <p>This API will not process any redirects, all registration URLs should be supplied with the
     * request.
     *
     * @param request app source registration request
     * @param executor used by callback to dispatch results
     * @param callback intended to notify asynchronously the API result
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerSource(
            @NonNull SourceRegistrationRequest request,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        mImpl.registerSource(request, executor, callback);
    }

    /**
     * Registers attribution sources(click or view) from an app context.
     *
     * <p>This API will not process any redirects, all registration URLs should be supplied with the
     * request. For use on Android R or lower.
     *
     * @param request app source registration request
     * @param executor used by callback to dispatch results
     * @param callback intended to notify asynchronously the API result
     * @deprecated use {@link #registerSource(SourceRegistrationRequest, Executor, OutcomeReceiver)}
     *     instead. Android R is no longer supported.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    public void registerSource(
            @NonNull SourceRegistrationRequest request,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable AdServicesOutcomeReceiver<Object, Exception> callback) {
        if (invokeCallbackOnErrorOnRvc(callback)) {
            callback.onError(new AdServicesException("AdServices is not supported on Android R"));
            return;
        }

        mImpl.registerSource(
                request, executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Registers an attribution source(click or view) from web context.
     *
     * <p>This API will not process any redirects, all registration URLs should be supplied with the
     * request. At least one of appDestination or webDestination parameters are required to be
     * provided. If the registration is successful, {@code callback}'s {@link
     * OutcomeReceiver#onResult} is invoked with null. In case of failure, a {@link Exception} is
     * sent through {@code callback}'s {@link OutcomeReceiver#onError}. Both success and failure
     * feedback are executed on the provided {@link Executor}.
     *
     * @param request source registration request
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerWebSource(
            @NonNull WebSourceRegistrationRequest request,
            @Nullable Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        mImpl.registerWebSource(request, executor, callback);
    }

    /**
     * Registers an attribution source(click or view) from web context.
     *
     * <p>This API will not process any redirects, all registration URLs should be supplied with the
     * request. At least one of appDestination or webDestination parameters are required to be
     * provided. If the registration is successful, {@code callback}'s {@link
     * OutcomeReceiver#onResult} is invoked with null. In case of failure, a {@link Exception} is
     * sent through {@code callback}'s {@link OutcomeReceiver#onError}. Both success and failure
     * feedback are executed on the provided {@link Executor}.
     *
     * <p>For use on Android R or lower.
     *
     * @param request source registration request
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     * @deprecated use {@link #registerWebSource(WebSourceRegistrationRequest, Executor,
     *     OutcomeReceiver)} instead. Android R is no longer supported.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    public void registerWebSource(
            @NonNull WebSourceRegistrationRequest request,
            @Nullable Executor executor,
            @Nullable AdServicesOutcomeReceiver<Object, Exception> callback) {
        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        mImpl.registerWebSource(
                request, executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Registers an attribution trigger(click or view) from web context.
     *
     * <p>This API will not process any redirects, all registration URLs should be supplied with the
     * request. If the registration is successful, {@code callback}'s {@link
     * OutcomeReceiver#onResult} is invoked with null. In case of failure, a {@link Exception} is
     * sent through {@code callback}'s {@link OutcomeReceiver#onError}. Both success and failure
     * feedback are executed on the provided {@link Executor}.
     *
     * @param request trigger registration request
     * @param executor used by callback to dispatch results
     * @param callback intended to notify asynchronously the API result
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerWebTrigger(
            @NonNull WebTriggerRegistrationRequest request,
            @Nullable Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        mImpl.registerWebTrigger(request, executor, callback);
    }

    /**
     * Registers an attribution trigger(click or view) from web context.
     *
     * <p>This API will not process any redirects, all registration URLs should be supplied with the
     * request. If the registration is successful, {@code callback}'s {@link
     * OutcomeReceiver#onResult} is invoked with null. In case of failure, a {@link Exception} is
     * sent through {@code callback}'s {@link OutcomeReceiver#onError}. Both success and failure
     * feedback are executed on the provided {@link Executor}.
     *
     * <p>For use on Android R or lower.
     *
     * @param request trigger registration request
     * @param executor used by callback to dispatch results
     * @param callback intended to notify asynchronously the API result
     * @deprecated use {@link #registerWebTrigger(WebTriggerRegistrationRequest, Executor,
     *     OutcomeReceiver)} instead. Anrdoid R is no longer supported.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    public void registerWebTrigger(
            @NonNull WebTriggerRegistrationRequest request,
            @Nullable Executor executor,
            @Nullable AdServicesOutcomeReceiver<Object, Exception> callback) {
        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        mImpl.registerWebTrigger(
                request, executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Registers a trigger (conversion).
     *
     * @param trigger the API issues a request to this URI to fetch metadata associated with the
     *     trigger. The trigger metadata is stored on-device, and is eligible to be matched with
     *     sources during the attribution process.
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     * @throws IllegalArgumentException if the scheme for {@code trigger} is not HTTPS
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void registerTrigger(
            @NonNull Uri trigger,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable OutcomeReceiver<Object, Exception> callback) {
        mImpl.registerTrigger(trigger, executor, callback);
    }

    /**
     * Registers a trigger (conversion). For use on Android R or lower.
     *
     * @param trigger the API issues a request to this URI to fetch metadata associated with the
     *     trigger. The trigger metadata is stored on-device, and is eligible to be matched with
     *     sources during the attribution process.
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     * @deprecated use {@link #registerTrigger(Uri, Executor, OutcomeReceiver)} instead. Android R
     *     is no longer supported.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    public void registerTrigger(
            @NonNull Uri trigger,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable AdServicesOutcomeReceiver<Object, Exception> callback) {
        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        mImpl.registerTrigger(
                trigger, executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Deletes previous registrations.
     *
     * <p>If the deletion is successful, the callback's {@link OutcomeReceiver#onResult} is invoked
     * with null. In case of failure, a {@link Exception} is sent through the callback's {@link
     * OutcomeReceiver#onError}. Both success and failure feedback are executed on the provided
     * {@link Executor}.
     *
     * @param deletionRequest The request for deleting data.
     * @param executor The executor to run callback.
     * @param callback intended to notify asynchronously the API result.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public void deleteRegistrations(
            @NonNull DeletionRequest deletionRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> callback) {
        mImpl.deleteRegistrations(deletionRequest, executor, callback);
    }

    /**
     * Deletes previous registrations.
     *
     * <p>If the deletion is successful, the callback's {@link OutcomeReceiver#onResult} is invoked
     * with null. In case of failure, a {@link Exception} is sent through the callback's {@link
     * OutcomeReceiver#onError}. Both success and failure feedback are executed on the provided
     * {@link Executor}.
     *
     * <p>For use on Android R or lower.
     *
     * @param deletionRequest The request for deleting data.
     * @param executor The executor to run callback.
     * @param callback intended to notify asynchronously the API result.
     * @deprecated use {@link #deleteRegistrations(DeletionRequest, Executor, OutcomeReceiver)}
     *     instead. Android R is no longer supported.
     */
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    public void deleteRegistrations(
            @NonNull DeletionRequest deletionRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Object, Exception> callback) {
        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        mImpl.deleteRegistrations(
                deletionRequest, executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Gets Measurement API status.
     *
     * <p>The callback's {@code Integer} value is one of {@code MeasurementApiState}.
     *
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    public void getMeasurementApiStatus(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Integer, Exception> callback) {
        mImpl.getMeasurementApiStatus(executor, callback);
    }

    /**
     * Gets Measurement API status.
     *
     * <p>The callback's {@code Integer} value is one of {@code MeasurementApiState}.
     *
     * <p>For use on Android R or lower.
     *
     * @param executor used by callback to dispatch results.
     * @param callback intended to notify asynchronously the API result.
     * @deprecated use {@link #getMeasurementApiStatus(Executor, OutcomeReceiver)} instead. Android
     *     R is no longer supported.
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    public void getMeasurementApiStatus(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Integer, Exception> callback) {
        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        mImpl.getMeasurementApiStatus(
                executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
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
        mImpl.unbindFromService();
    }
}
