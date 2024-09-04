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

import android.adservices.adid.AdId;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
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
        isAdServicesEnabled(
                executor, OutcomeReceiverConverter.toAdServicesOutcomeReceiver(callback));
    }

    /**
     * Get the AdService's enablement state which represents whether AdServices feature is enabled
     * or not. This API is for Android R, and uses the AdServicesOutcomeReceiver class because
     * OutcomeReceiver is not available.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_ADSERVICES_ENABLEMENT_CHECK_ENABLED)
    @RequiresPermission(anyOf = {ACCESS_ADSERVICES_STATE, ACCESS_ADSERVICES_STATE_COMPAT})
    public void isAdServicesEnabled(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Boolean, Exception> callback) {
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
        enableAdServices(
                adServicesStates,
                executor,
                OutcomeReceiverConverter.toAdServicesOutcomeReceiver(callback));
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
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_ENABLE_ADSERVICES_API_ENABLED)
    @RequiresPermission(anyOf = {MODIFY_ADSERVICES_STATE, MODIFY_ADSERVICES_STATE_COMPAT})
    public void enableAdServices(
            @NonNull AdServicesStates adServicesStates,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Boolean, Exception> callback) {
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
     * @hide
     */
    // TODO(b/295205476): Move exceptions into the callback.
    @SystemApi
    @FlaggedApi(Flags.FLAG_AD_ID_CACHE_ENABLED)
    @RequiresPermission(anyOf = {UPDATE_PRIVILEGED_AD_ID, UPDATE_PRIVILEGED_AD_ID_COMPAT})
    public void updateAdId(
            @NonNull UpdateAdIdRequest updateAdIdRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<Boolean, Exception> callback) {
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
    @FlaggedApi(Flags.FLAG_AD_ID_CACHE_ENABLED)
    @RequiresPermission(anyOf = {UPDATE_PRIVILEGED_AD_ID, UPDATE_PRIVILEGED_AD_ID_COMPAT})
    @RequiresApi(Build.VERSION_CODES.S)
    public void updateAdId(
            @NonNull UpdateAdIdRequest updateAdIdRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Boolean, Exception> callback) {
        updateAdId(
                updateAdIdRequest,
                executor,
                OutcomeReceiverConverter.toAdServicesOutcomeReceiver(callback));
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
