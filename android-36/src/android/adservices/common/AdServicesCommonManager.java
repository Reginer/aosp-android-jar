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

package android.adservices.common;

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_STATE;
import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_STATE_COMPAT;
import static android.adservices.common.AdServicesPermissions.MODIFY_ADSERVICES_STATE;
import static android.adservices.common.AdServicesPermissions.MODIFY_ADSERVICES_STATE_COMPAT;
import static android.adservices.common.AdServicesPermissions.UPDATE_PRIVILEGED_AD_ID;
import static android.adservices.common.AdServicesPermissions.UPDATE_PRIVILEGED_AD_ID_COMPAT;
import static android.adservices.common.AndroidRCommonUtil.invokeCallbackOnErrorOnRvc;

import android.adservices.adid.AdId;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SystemApi;
import android.app.sdksandbox.SandboxedSdkContext;
import android.content.Context;
import android.os.Build;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.os.SystemClock;

import androidx.annotation.RequiresApi;

import com.android.adservices.AdServicesCommon;
import com.android.adservices.LogUtil;
import com.android.adservices.ServiceBinder;
import com.android.adservices.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * AdServicesCommonManager contains APIs common across the various AdServices. It provides two
 * SystemApis:
 *
 * <ul>
 *   <li>isAdServicesEnabled - allows to get AdServices state.
 *   <li>setAdServicesEntryPointEnabled - allows to control AdServices state.
 * </ul>
 *
 * <p>The instance of the {@link AdServicesCommonManager} can be obtained using {@link
 * Context#getSystemService} and {@link AdServicesCommonManager} class.
 *
 * @hide
 */
@SystemApi
public class AdServicesCommonManager {
    /** @hide */
    public static final String AD_SERVICES_COMMON_SERVICE = "ad_services_common_service";

    private final Context mContext;
    private final ServiceBinder<IAdServicesCommonService> mAdServicesCommonServiceBinder;

    // TODO(b/378923974): refactor all usages to reference these constants directly instead of
    //  derived ones in other classes.

    /** Don't show any notification during the enrollment. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int NOTIFICATION_NONE = 0;

    /** Shows ongoing notification during the enrollment, which user can not dismiss. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int NOTIFICATION_ONGOING = 1;

    /** Shows regular notification during the enrollment, which user can dismiss. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int NOTIFICATION_REGULAR = 2;

    /**
     * Result codes that are common across various APIs.
     *
     * @hide
     */
    @IntDef(value = {NOTIFICATION_NONE, NOTIFICATION_ONGOING, NOTIFICATION_REGULAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NotificationType {}

    /** Default user choice state */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int USER_CHOICE_UNKNOWN = 0;

    /** User opted in state */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int USER_CHOICE_OPTED_IN = 1;

    /** User opted out state */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int USER_CHOICE_OPTED_OUT = 2;

    /**
     * Result codes that are common across various modules.
     *
     * @hide
     */
    @IntDef(
            prefix = {""},
            value = {USER_CHOICE_UNKNOWN, USER_CHOICE_OPTED_IN, USER_CHOICE_OPTED_OUT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ModuleUserChoice {}

    /** Default module state */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_STATE_UNKNOWN = 0;

    /** Module is available on the device */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_STATE_ENABLED = 1;

    /** Module is not available on the device */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_STATE_DISABLED = 2;

    /**
     * Result codes that are common across various modules.
     *
     * @hide
     */
    @IntDef(
            prefix = {""},
            value = {MODULE_STATE_UNKNOWN, MODULE_STATE_ENABLED, MODULE_STATE_DISABLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ModuleState {}

    /** Measurement module. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_MEASUREMENT = 0;

    /** Privacy Sandbox module. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_PROTECTED_AUDIENCE = 1;

    /** Privacy Sandbox Attribution module. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_PROTECTED_APP_SIGNALS = 2;

    /** Topics module. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_TOPICS = 3;

    /** On-device Personalization(ODP) module. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_ON_DEVICE_PERSONALIZATION = 4;

    /** ADID module. */
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    public static final int MODULE_ADID = 5;

    /**
     * ModuleCode IntDef.
     *
     * @hide
     */
    @IntDef(
            value = {
                MODULE_MEASUREMENT,
                MODULE_PROTECTED_AUDIENCE,
                MODULE_PROTECTED_APP_SIGNALS,
                MODULE_TOPICS,
                MODULE_ON_DEVICE_PERSONALIZATION,
                MODULE_ADID
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Module {}

    /**
     * Returns {@code module} or throws an {@link IllegalArgumentException} if it's invalid.
     *
     * @param module module to validate
     * @hide
     */
    @Module
    public static int validateModule(@Module int module) {
        return switch (module) {
            case MODULE_ADID,
                            MODULE_MEASUREMENT,
                            MODULE_ON_DEVICE_PERSONALIZATION,
                            MODULE_PROTECTED_APP_SIGNALS,
                            MODULE_PROTECTED_AUDIENCE,
                            MODULE_TOPICS ->
                    module;
            default -> throw new IllegalArgumentException("Invalid Module:" + module);
        };
    }

    /**
     * Returns {@code moduleState} or throws an {@link IllegalArgumentException} if it's invalid.
     *
     * @param moduleState module state to validate
     * @hide
     */
    @ModuleState
    public static int validateModuleState(@ModuleState int moduleState) {
        return switch (moduleState) {
            case MODULE_STATE_UNKNOWN, MODULE_STATE_ENABLED, MODULE_STATE_DISABLED -> moduleState;
            default -> throw new IllegalArgumentException("Invalid Module State:" + moduleState);
        };
    }

    /**
     * Create AdServicesCommonManager.
     *
     * @hide
     */
    public AdServicesCommonManager(@NonNull Context context) {
        mContext = context;
        mAdServicesCommonServiceBinder =
                ServiceBinder.getServiceBinder(
                        context,
                        AdServicesCommon.ACTION_AD_SERVICES_COMMON_SERVICE,
                        IAdServicesCommonService.Stub::asInterface);
    }

    /**
     * Factory method for creating an instance of AdServicesCommonManager.
     *
     * @param context The {@link Context} to use
     * @return A {@link AdServicesCommonManager} instance
     */
    @NonNull
    public static AdServicesCommonManager get(@NonNull Context context) {
        // On T+, context.getSystemService() does more than just call constructor.
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? context.getSystemService(AdServicesCommonManager.class)
                : new AdServicesCommonManager(context);
    }

    @NonNull
    private IAdServicesCommonService getService() {
        IAdServicesCommonService service = mAdServicesCommonServiceBinder.getService();
        if (service == null) {
            throw new IllegalStateException("Unable to find the service");
        }
        return service;
    }

    /**
     * Get the AdService's enablement state which represents whether AdServices feature is enabled
     * or not. This API is for Android S+, which has the OutcomeReceiver class available.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {ACCESS_ADSERVICES_STATE, ACCESS_ADSERVICES_STATE_COMPAT})
    @RequiresApi(Build.VERSION_CODES.S)
    public void isAdServicesEnabled(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Boolean, Exception> callback) {
        final IAdServicesCommonService service = getService();
        try {
            service.isAdServicesEnabled(
                    new IAdServicesCommonCallback.Stub() {
                        @Override
                        public void onResult(IsAdServicesEnabledResult result) {
                            executor.execute(
                                    () -> {
                                        callback.onResult(result.getAdServicesEnabled());
                                    });
                        }

                        @Override
                        public void onFailure(int statusCode) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(statusCode)));
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            executor.execute(
                    () -> callback.onError(new IllegalStateException("Internal Error!", e)));
        }
    }

    /**
     * Get the AdService's enablement state which represents whether AdServices feature is enabled
     * or not. This API is for Android R, and uses the AdServicesOutcomeReceiver class because
     * OutcomeReceiver is not available.
     *
     * @deprecated use {@link #isAdServicesEnabled(Executor, OutcomeReceiver)} instead. Android R is
     *     no longer supported.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {ACCESS_ADSERVICES_STATE, ACCESS_ADSERVICES_STATE_COMPAT})
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    @SuppressWarnings("NewApi")
    public void isAdServicesEnabled(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Boolean, Exception> callback) {

        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        isAdServicesEnabled(executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Sets the AdService's enablement state based on the provided parameters.
     *
     * <p>As a result of the AdServices state, {@code adServicesEntryPointEnabled}, {@code
     * adIdEnabled}, appropriate notification may be displayed to the user. It's displayed only once
     * when all the following conditions are met:
     *
     * <ul>
     *   <li>AdServices state - enabled.
     *   <li>adServicesEntryPointEnabled - true.
     * </ul>
     *
     * @param adServicesEntryPointEnabled indicate entry point enabled or not
     * @param adIdEnabled indicate user opt-out of adid or not
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {MODIFY_ADSERVICES_STATE, MODIFY_ADSERVICES_STATE_COMPAT})
    public void setAdServicesEnabled(boolean adServicesEntryPointEnabled, boolean adIdEnabled) {
        final IAdServicesCommonService service = getService();
        try {
            service.setAdServicesEnabled(adServicesEntryPointEnabled, adIdEnabled);
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
        }
    }

    /**
     * Enable AdServices based on the AdServicesStates input parameter. This API is for Android S+,
     * which has the OutcomeReceiver class available.
     *
     * <p>Based on the provided {@code AdServicesStates}, AdServices may be enabled. Specifically,
     * users will be provided with an enrollment channel (such as notification) to become privacy
     * sandbox users when:
     *
     * <ul>
     *   <li>isAdServicesUiEnabled - true.
     *   <li>isU18Account | isAdultAccount - true.
     * </ul>
     *
     * @param {@code AdServicesStates} parcel containing relevant AdServices state variables.
     * @return false if API is disabled, true if the API call completed successfully. Otherwise, it
     *     would return one of the following exceptions to the user:
     *     <ul>
     *       <li>IllegalStateException - the default exception thrown when service crashes
     *           unexpectedly.
     *       <li>SecurityException - when the caller is not authorized to call this API.
     *       <li>TimeoutException - when the services takes too long to respond.
     *     </ul>
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {MODIFY_ADSERVICES_STATE, MODIFY_ADSERVICES_STATE_COMPAT})
    @RequiresApi(Build.VERSION_CODES.S)
    public void enableAdServices(
            @NonNull AdServicesStates adServicesStates,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Boolean, Exception> callback) {
        Objects.requireNonNull(adServicesStates);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        final IAdServicesCommonService service = getService();
        try {
            service.enableAdServices(
                    adServicesStates,
                    new IEnableAdServicesCallback.Stub() {
                        @Override
                        public void onResult(EnableAdServicesResponse response) {
                            executor.execute(
                                    () -> {
                                        if (!response.isApiEnabled()) {
                                            callback.onResult(false);
                                            return;
                                        }

                                        if (response.isSuccess()) {
                                            callback.onResult(true);
                                        } else {
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(
                                                            response.getStatusCode()));
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(int statusCode) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(statusCode)));
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            executor.execute(
                    () -> callback.onError(new IllegalStateException("Internal Error!", e)));
        }
    }

    /**
     * Broadcast action: notify that a consent notification has been displayed to the user, and the
     * user consent choices can be set by calling {@link #requestAdServicesModuleUserChoices()}.
     *
     * <p>The action must be defined as an intent-filter in AndroidManifest.xml in order to receive
     * Intents from the platform.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    @RequiresPermission(anyOf = {MODIFY_ADSERVICES_STATE, MODIFY_ADSERVICES_STATE_COMPAT})
    public static final String ACTION_ADSERVICES_NOTIFICATION_DISPLAYED =
            "android.adservices.common.action.ADSERVICES_NOTIFICATION_DISPLAYED";

    /**
     * Activity Action: Open the consent landing page activity. In the activity, user consent
     * choices can be set, depending on user action, by calling {@link
     * #requestAdServicesModuleUserChoices()}. The action must be defined as an intent-filter in
     * AndroidManifest.xml in order to receive Intents from the platform.
     *
     * <p>Input: nothing
     *
     * <p>Output: nothing
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    @RequiresPermission(anyOf = {MODIFY_ADSERVICES_STATE, MODIFY_ADSERVICES_STATE_COMPAT})
    public static final String ACTION_VIEW_ADSERVICES_CONSENT_PAGE =
            "android.adservices.common.action.VIEW_ADSERVICES_CONSENT_PAGE";

    /**
     * Sets overrides for the AdServices Module(s).
     *
     * <p>This API can enable/disable AdServices modules. Setting a module to off will hide the
     * settings controls for any PPAPIs (Privacy Preserving APIs) associated with it. In addition,
     * those PPAPIs will not operate for that user.
     *
     * <p>A notification type is also required to determine what type of notification should be
     * shown to the user to notify them of these changes. The NotificationType can be Ongoing,
     * Regular, or None.
     *
     * @param updateParams object containing state information for modules and notification type.
     * @param executor the executor for the callback.
     * @param callback callback function to confirm modules overrides is set up correctly.
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    @RequiresPermission(anyOf = {MODIFY_ADSERVICES_STATE, MODIFY_ADSERVICES_STATE_COMPAT})
    public void requestAdServicesModuleOverrides(
            @NonNull UpdateAdServicesModuleStatesParams updateParams,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Void, Exception> callback) {
        Objects.requireNonNull(updateParams, "updateParams cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        final IAdServicesCommonService service = getService();
        try {
            service.requestAdServicesModuleOverrides(
                    updateParams,
                    new IRequestAdServicesModuleOverridesCallback.Stub() {
                        @Override
                        public void onSuccess() throws RemoteException {
                            callback.onResult(null);
                        }

                        @Override
                        public void onFailure(int statusCode) throws RemoteException {
                            callback.onError(
                                    new IllegalStateException(
                                            "Internal Error! status code: " + statusCode));
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            executor.execute(
                    () -> callback.onError(new IllegalStateException("Internal Error!", e)));
        }
    }

    /**
     * Sets the user choices for AdServices Module(s).
     *
     * <p>This API sets the user consent value for each AdServices module (PAS, Measurement, Topic,
     * etc). The user consent controls whether the PPAPI associated with that module can operate or
     * not. If a module already has a user choice opt-in or opt-out, then only user choice unknown
     * will be accepted as a hard reset option, after which the user choice should be set to the
     * desired value as soon as possible.
     *
     * @param updateParams object containing user choices for modules.
     * @param executor the executor for the callback.
     * @param callback callback function to confirm module user choice is set up correctly.
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
    @RequiresPermission(anyOf = {MODIFY_ADSERVICES_STATE, MODIFY_ADSERVICES_STATE_COMPAT})
    public void requestAdServicesModuleUserChoices(
            @NonNull UpdateAdServicesUserChoicesParams updateParams,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Void, Exception> callback) {
        Objects.requireNonNull(updateParams, "updateParams cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        final IAdServicesCommonService service = getService();
        try {
            service.requestAdServicesModuleUserChoices(
                    updateParams,
                    new IRequestAdServicesModuleUserChoicesCallback.Stub() {
                        @Override
                        public void onSuccess() throws RemoteException {
                            callback.onResult(null);
                        }

                        @Override
                        public void onFailure(int statusCode) throws RemoteException {
                            callback.onError(
                                    new IllegalStateException(
                                            "Internal Error! status code: " + statusCode));
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            executor.execute(
                    () -> callback.onError(new IllegalStateException("Internal Error!", e)));
        }
    }

    /**
     * Enable AdServices based on the AdServicesStates input parameter. This API is for Android R,
     * and uses the AdServicesOutcomeReceiver class because OutcomeReceiver is not available.
     *
     * <p>Based on the provided {@code AdServicesStates}, AdServices may be enabled. Specifically,
     * users will be provided with an enrollment channel (such as notification) to become privacy
     * sandbox users when:
     *
     * <ul>
     *   <li>isAdServicesUiEnabled - true.
     *   <li>isU18Account | isAdultAccount - true.
     * </ul>
     *
     * @param adServicesStates parcel containing relevant AdServices state variables.
     * @deprecated use {@link #enableAdServices(AdServicesStates, Executor, OutcomeReceiver)}
     *     instead. Android R is no longer supported.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {MODIFY_ADSERVICES_STATE, MODIFY_ADSERVICES_STATE_COMPAT})
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    @SuppressWarnings("NewApi")
    public void enableAdServices(
            @NonNull AdServicesStates adServicesStates,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Boolean, Exception> callback) {

        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        enableAdServices(
                adServicesStates, executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Updates {@link AdId} in Adservices when the device changes {@link AdId}. This API is used by
     * AdIdProvider.
     *
     * @param updateAdIdRequest the request that contains {@link AdId} information to update.
     * @param executor the executor for the callback.
     * @param callback the callback in type {@link AdServicesOutcomeReceiver}, available on Android
     *     R and above.
     * @throws IllegalStateException when service is not available or the feature is not enabled, or
     *     if there is any {@code Binder} invocation error.
     * @throws SecurityException when the caller is not authorized to call this API.
     * @deprecated use {@link #updateAdId(UpdateAdIdRequest, Executor, OutcomeReceiver)} instead.
     *     Android R is no longer supported.
     * @hide
     */
    // TODO(b/295205476): Move exceptions into the callback.
    @SystemApi
    @RequiresPermission(anyOf = {UPDATE_PRIVILEGED_AD_ID, UPDATE_PRIVILEGED_AD_ID_COMPAT})
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    @SuppressWarnings("NewApi")
    public void updateAdId(
            @NonNull UpdateAdIdRequest updateAdIdRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Boolean, Exception> callback) {

        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        updateAdId(
                updateAdIdRequest, executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * Updates {@link AdId} in Adservices when the device changes {@link AdId}. This API is used by
     * AdIdProvider.
     *
     * @param updateAdIdRequest the request that contains {@link AdId} information to update.
     * @param executor the executor for the callback.
     * @param callback the callback in type {@link OutcomeReceiver}, available on Android S and
     *     above.
     * @throws IllegalStateException when service is not available or the feature is not enabled, or
     *     if there is any {@code Binder} invocation error.
     * @throws SecurityException when the caller is not authorized to call this API.
     * @hide
     */
    @SystemApi
    @RequiresPermission(anyOf = {UPDATE_PRIVILEGED_AD_ID, UPDATE_PRIVILEGED_AD_ID_COMPAT})
    @RequiresApi(Build.VERSION_CODES.S)
    public void updateAdId(
            @NonNull UpdateAdIdRequest updateAdIdRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Boolean, Exception> callback) {
        Objects.requireNonNull(updateAdIdRequest);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        IAdServicesCommonService service = getService();
        try {
            service.updateAdIdCache(
                    updateAdIdRequest,
                    new IUpdateAdIdCallback.Stub() {
                        @Override
                        public void onResult(String message) {
                            executor.execute(() -> callback.onResult(true));
                        }

                        @Override
                        public void onFailure(int statusCode) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(statusCode)));
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException calling updateAdIdCache with %s", updateAdIdRequest);
            executor.execute(
                    () -> callback.onError(new IllegalStateException("Internal Error!", e)));
        }
    }

    /**
     * Get the AdService's common states.
     *
     * @param executor the executor for the callback.
     * @param callback the callback in type {@link AdServicesOutcomeReceiver}, available on Android
     *     R and above.
     * @throws IllegalStateException if there is any {@code Binder} invocation error.
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_GET_ADSERVICES_COMMON_STATES_API_ENABLED)
    @RequiresPermission(anyOf = {ACCESS_ADSERVICES_STATE, ACCESS_ADSERVICES_STATE_COMPAT})
    public void getAdservicesCommonStates(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull
                    AdServicesOutcomeReceiver<AdServicesCommonStatesResponse, Exception> callback) {

        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        final IAdServicesCommonService service = getService();
        CallerMetadata callerMetadata =
                new CallerMetadata.Builder()
                        .setBinderElapsedTimestamp(SystemClock.elapsedRealtime())
                        .build();
        String appPackageName = "";
        String sdkPackageName = "";
        // First check if context is SandboxedSdkContext or not
        SandboxedSdkContext sandboxedSdkContext =
                SandboxedSdkContextUtils.getAsSandboxedSdkContext(mContext);
        if (sandboxedSdkContext != null) {
            // This is the case with the Sandbox.
            sdkPackageName = sandboxedSdkContext.getSdkPackageName();
            appPackageName = sandboxedSdkContext.getClientPackageName();
        } else {
            // This is the case without the Sandbox.
            appPackageName = mContext.getPackageName();
        }
        try {
            service.getAdServicesCommonStates(
                    new GetAdServicesCommonStatesParams.Builder(appPackageName, sdkPackageName)
                            .build(),
                    callerMetadata,
                    new IAdServicesCommonStatesCallback.Stub() {
                        @Override
                        public void onResult(AdServicesCommonStatesResponse result) {
                            executor.execute(
                                    () -> {
                                        callback.onResult(result);
                                    });
                        }

                        @Override
                        public void onFailure(int statusCode) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(statusCode)));
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            executor.execute(
                    () -> callback.onError(new IllegalStateException("Internal Error!", e)));
        }
    }
}
