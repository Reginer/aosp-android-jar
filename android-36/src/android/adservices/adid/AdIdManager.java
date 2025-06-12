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
package android.adservices.adid;

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_AD_ID;
import static android.adservices.common.AdServicesStatusUtils.SERVICE_UNAVAILABLE_ERROR_MESSAGE;
import static android.adservices.common.AndroidRCommonUtil.invokeCallbackOnErrorOnRvc;

import android.adservices.common.AdServicesOutcomeReceiver;
import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.CallerMetadata;
import android.adservices.common.OutcomeReceiverConverter;
import android.adservices.common.SandboxedSdkContextUtils;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
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
import com.android.adservices.shared.common.exception.ServiceUnavailableException;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * AdId Manager provides APIs for app and ad-SDKs to access advertising ID. The advertising ID is a
 * unique, per-device, user-resettable ID for advertising. It gives users better controls and
 * provides developers with a simple, standard system to continue to monetize their apps via
 * personalized ads (formerly known as interest-based ads).
 */
public class AdIdManager {
    /**
     * Service used for registering AdIdManager in the system service registry.
     *
     * @hide
     */
    public static final String ADID_SERVICE = "adid_service";

    // When an app calls the AdId API directly, it sets the SDK name to empty string.
    static final String EMPTY_SDK = "";

    private Context mContext;
    private ServiceBinder<IAdIdService> mServiceBinder;

    /**
     * Factory method for creating an instance of AdIdManager.
     *
     * @param context The {@link Context} to use
     * @return A {@link AdIdManager} instance
     */
    @NonNull
    public static AdIdManager get(@NonNull Context context) {
        // On T+, context.getSystemService() does more than just call constructor.
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? context.getSystemService(AdIdManager.class)
                : new AdIdManager(context);
    }

    /**
     * Create AdIdManager
     *
     * @hide
     */
    public AdIdManager(Context context) {
        // In case the AdIdManager is initiated from inside a sdk_sandbox process the fields
        // will be immediately rewritten by the initialize method below.
        initialize(context);
    }

    /**
     * Initializes {@link AdIdManager} with the given {@code context}.
     *
     * <p>This method is called by the {@link SandboxedSdkContext} to propagate the correct context.
     * For more information check the javadoc on the {@link
     * android.app.sdksandbox.SdkSandboxSystemServiceRegistry}.
     *
     * @hide
     * @see android.app.sdksandbox.SdkSandboxSystemServiceRegistry
     */
    public AdIdManager initialize(Context context) {
        mContext = context;
        mServiceBinder =
                ServiceBinder.getServiceBinder(
                        context,
                        AdServicesCommon.ACTION_ADID_SERVICE,
                        IAdIdService.Stub::asInterface);
        return this;
    }

    @SuppressWarnings("NewApi")
    private IAdIdService getService(
            @CallbackExecutor Executor executor, OutcomeReceiver<AdId, Exception> callback) {
        IAdIdService service = null;
        try {
            service = mServiceBinder.getService();

            // Throw ServiceUnavailableException and set it to the callback.
            if (service == null) {
                throw new ServiceUnavailableException(SERVICE_UNAVAILABLE_ERROR_MESSAGE);
            }
        } catch (RuntimeException e) {
            LogUtil.e(e, "Failed binding to AdId service");
            executor.execute(() -> callback.onError(e));
        }

        return service;
    }

    private Context getContext() {
        return mContext;
    }

    /**
     * Return the AdId.
     *
     * @param executor The executor to run callback.
     * @param callback The callback that's called after adid are available or an error occurs.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(ACCESS_ADSERVICES_AD_ID)
    @NonNull
    public void getAdId(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<AdId, Exception> callback) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");
        CallerMetadata callerMetadata =
                new CallerMetadata.Builder()
                        .setBinderElapsedTimestamp(SystemClock.elapsedRealtime())
                        .build();
        String appPackageName = "";
        String sdkPackageName = "";
        // First check if context is SandboxedSdkContext or not
        Context getAdIdRequestContext = getContext();
        SandboxedSdkContext requestContext =
                SandboxedSdkContextUtils.getAsSandboxedSdkContext(getAdIdRequestContext);
        if (requestContext != null) {
            sdkPackageName = requestContext.getSdkPackageName();
            appPackageName = requestContext.getClientPackageName();
        } else { // This is the case without the Sandbox.
            appPackageName = getAdIdRequestContext.getPackageName();
        }

        try {
            IAdIdService service = getService(executor, callback);
            if (service == null) {
                LogUtil.w("Unable to find AdId service");
                return;
            }

            service.getAdId(
                    new GetAdIdParam.Builder()
                            .setAppPackageName(appPackageName)
                            .setSdkPackageName(sdkPackageName)
                            .build(),
                    callerMetadata,
                    new IGetAdIdCallback.Stub() {
                        @Override
                        public void onResult(GetAdIdResult resultParcel) {
                            executor.execute(
                                    () -> {
                                        if (resultParcel.isSuccess()) {
                                            callback.onResult(
                                                    new AdId(
                                                            resultParcel.getAdId(),
                                                            resultParcel.isLatEnabled()));
                                        } else {
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(
                                                            resultParcel));
                                        }
                                    });
                        }

                        @Override
                        public void onError(int resultCode) {
                            executor.execute(
                                    () ->
                                            callback.onError(
                                                    AdServicesStatusUtils.asException(resultCode)));
                        }
                    });
        } catch (RemoteException e) {
            LogUtil.e(e, "RemoteException");
            callback.onError(e);
        }
    }

    /**
     * Return the AdId. For use on Android R or lower.
     *
     * @param executor The executor to run callback.
     * @param callback The callback that's called after adid are available or an error occurs.
     * @deprecated use {@link #getAdId(Executor, OutcomeReceiver)} instead. Android R is no longer
     *     supported.
     */
    @RequiresPermission(ACCESS_ADSERVICES_AD_ID)
    @Deprecated
    @FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
    @SuppressWarnings("NewApi")
    @NonNull
    public void getAdId(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull AdServicesOutcomeReceiver<AdId, Exception> callback) {
        if (invokeCallbackOnErrorOnRvc(callback)) {
            return;
        }

        getAdId(executor, OutcomeReceiverConverter.toOutcomeReceiver(callback));
    }

    /**
     * If the service is in an APK (as opposed to the system service), unbind it from the service to
     * allow the APK process to die.
     *
     * @hide
     */
    // TODO: change to @VisibleForTesting
    public void unbindFromService() {
        mServiceBinder.unbindFromService();
    }
}
