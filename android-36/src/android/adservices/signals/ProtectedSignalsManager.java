/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.adservices.signals;

import static android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_PROTECTED_SIGNALS;

import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.FledgeErrorResponse;
import android.adservices.common.SandboxedSdkContextUtils;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.app.sdksandbox.SandboxedSdkContext;
import android.content.Context;
import android.os.Build;
import android.os.LimitExceededException;
import android.os.OutcomeReceiver;
import android.os.RemoteException;

import com.android.adservices.AdServicesCommon;
import com.android.adservices.LoggerFactory;
import com.android.adservices.ServiceBinder;
import com.android.adservices.flags.Flags;

import java.util.Objects;
import java.util.concurrent.Executor;

/** ProtectedSignalsManager provides APIs for apps and ad-SDKs to manage their protected signals. */
@FlaggedApi(Flags.FLAG_PROTECTED_SIGNALS_ENABLED)
@RequiresApi(Build.VERSION_CODES.S)
public class ProtectedSignalsManager {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getFledgeLogger();
    /**
     * Constant that represents the service name for {@link ProtectedSignalsManager} to be used in
     * {@link android.adservices.AdServicesFrameworkInitializer#registerServiceWrappers}
     *
     * @hide
     */
    public static final String PROTECTED_SIGNALS_SERVICE = "protected_signals_service";

    @NonNull private Context mContext;
    @NonNull private ServiceBinder<IProtectedSignalsService> mServiceBinder;

    /**
     * Factory method for creating an instance of ProtectedSignalsManager.
     *
     * @param context The {@link Context} to use
     * @return A {@link ProtectedSignalsManager} instance
     */
    @SuppressLint("ManagerLookup")
    @NonNull
    // TODO(b/303896680): Investigate why this lint was not triggered for similar managers
    public static ProtectedSignalsManager get(@NonNull Context context) {
        // On T+, context.getSystemService() does more than just call constructor.
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? context.getSystemService(ProtectedSignalsManager.class)
                : new ProtectedSignalsManager(context);
    }

    /**
     * Create a service binder ProtectedSignalsManager
     *
     * @hide
     */
    public ProtectedSignalsManager(@NonNull Context context) {
        Objects.requireNonNull(context);

        // In case the ProtectedSignalsManager is initiated from inside a sdk_sandbox process the
        // fields will be immediately rewritten by the initialize method below.
        initialize(context);
    }

    /**
     * Initializes {@link ProtectedSignalsManager} with the given {@code context}.
     *
     * <p>This method is called by the {@link SandboxedSdkContext} to propagate the correct context.
     * For more information check the javadoc on the {@link
     * android.app.sdksandbox.SdkSandboxSystemServiceRegistry}.
     *
     * @hide
     * @see android.app.sdksandbox.SdkSandboxSystemServiceRegistry
     */
    public ProtectedSignalsManager initialize(@NonNull Context context) {
        Objects.requireNonNull(context);

        mContext = context;
        mServiceBinder =
                ServiceBinder.getServiceBinder(
                        context,
                        AdServicesCommon.ACTION_PROTECTED_SIGNALS_SERVICE,
                        IProtectedSignalsService.Stub::asInterface);
        return this;
    }

    @NonNull
    IProtectedSignalsService getService() {
        IProtectedSignalsService service = mServiceBinder.getService();
        if (service == null) {
            throw new IllegalStateException("Unable to find the service");
        }
        return service;
    }

    /**
     * The updateSignals API will retrieve a JSON from the URI that describes which signals to add
     * or remove. This API also allows registering the encoder endpoint. The endpoint is used to
     * download an encoding logic, which enables encoding the signals.
     *
     * <p>The top level keys for the JSON must correspond to one of 5 commands:
     *
     * <p>"put" - Adds a new signal, overwriting any existing signals with the same key. The value
     * for this is a JSON object where the keys are base 64 strings corresponding to the key to put
     * for and the values are base 64 string corresponding to the value to put.
     *
     * <p>"append" - Appends a new signal/signals to a time series of signals, removing the oldest
     * signals to make room for the new ones if the size of the series exceeds the given maximum.
     * The value for this is a JSON object where the keys are base 64 strings corresponding to the
     * key to append to and the values are objects with two fields: "values" and "maxSignals" .
     * "values" is a list of base 64 strings corresponding to signal values to append to the time
     * series. "maxSignals" is the maximum number of values that are allowed in this timeseries. If
     * the current number of signals associated with the key exceeds maxSignals the oldest signals
     * will be removed. Note that you can append to a key added by put. Not that appending more than
     * the maximum number of values will cause a failure.
     *
     * <p>"put_if_not_present" - Adds a new signal only if there are no existing signals with the
     * same key. The value for this is a JSON object where the keys are base 64 strings
     * corresponding to the key to put for and the values are base 64 string corresponding to the
     * value to put.
     *
     * <p>"remove" - Removes the signal for a key. The value of this is a list of base 64 strings
     * corresponding to the keys of signals that should be deleted.
     *
     * <p>"update_encoder" - Provides an action to update the endpoint, and a URI which can be used
     * to retrieve an encoding logic. The sub-key for providing an update action is "action" and the
     * values currently supported are:
     *
     * <ol>
     *   <li>"REGISTER" : Registers the encoder endpoint if provided for the first time or
     *       overwrites the existing one with the newly provided endpoint. Providing the "endpoint"
     *       is required for the "REGISTER" action.
     * </ol>
     *
     * <p>The sub-key for providing an encoder endpoint is "endpoint" and the value is the URI
     * string for the endpoint.
     *
     * <p>On success, the onResult method of the provided OutcomeReceiver will be called with an
     * empty Object. This Object has no significance and is used merely as a placeholder.
     *
     * <p>Key may only be operated on by one command per JSON. If two command attempt to operate on
     * the same key, this method will through an {@link IllegalArgumentException}
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
     *   <li>The JSON retrieved from the server is not valid.
     *   <li>The provided URI is invalid.
     * </ol>
     *
     * <p>This call fails with {@link LimitExceededException} if the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * <p>This call fails with an {@link IllegalStateException} if an internal service error is
     * encountered.
     */
    @RequiresPermission(ACCESS_ADSERVICES_PROTECTED_SIGNALS)
    public void updateSignals(
            @NonNull UpdateSignalsRequest updateSignalsRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        Objects.requireNonNull(updateSignalsRequest);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(receiver);

        try {
            final IProtectedSignalsService service = getService();

            service.updateSignals(
                    new UpdateSignalsInput.Builder(
                                    updateSignalsRequest.getUpdateUri(), getCallerPackageName())
                            .build(),
                    new UpdateSignalsCallback.Stub() {
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
