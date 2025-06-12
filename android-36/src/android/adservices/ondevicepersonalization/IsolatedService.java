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

package android.adservices.ondevicepersonalization;

import android.adservices.ondevicepersonalization.aidl.IDataAccessService;
import android.adservices.ondevicepersonalization.aidl.IFederatedComputeService;
import android.adservices.ondevicepersonalization.aidl.IIsolatedModelService;
import android.adservices.ondevicepersonalization.aidl.IIsolatedService;
import android.adservices.ondevicepersonalization.aidl.IIsolatedServiceCallback;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.OutcomeReceiver;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;

import com.android.ondevicepersonalization.internal.util.ExceptionInfo;
import com.android.ondevicepersonalization.internal.util.LoggerFactory;
import com.android.ondevicepersonalization.internal.util.OdpParceledListSlice;

import java.util.Objects;
import java.util.function.Function;

// TODO(b/289102463): Add a link to the public ODP developer documentation.
/**
 * Base class for services that are started by ODP on a call to
 * {@code OnDevicePersonalizationManager#execute(ComponentName, PersistableBundle,
 * java.util.concurrent.Executor, OutcomeReceiver)}
 * and run in an <a
 * href="https://developer.android.com/guide/topics/manifest/service-element#isolated">isolated
 * process</a>. The service can produce content to be displayed in a
 * {@link android.view.SurfaceView} in a calling app and write persistent results to on-device
 * storage, which can be consumed by Federated Analytics for cross-device statistical analysis or
 * by Federated Learning for model training.
 * Client apps use {@link OnDevicePersonalizationManager} to interact with an {@link
 * IsolatedService}.
 */
public abstract class IsolatedService extends Service {
    private static final String TAG = IsolatedService.class.getSimpleName();
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getLogger();
    private static final int MAX_EXCEPTION_CHAIN_DEPTH = 3;
    private IBinder mBinder;

    /** Creates a binder for an {@link IsolatedService}. */
    @Override
    public void onCreate() {
        mBinder = new ServiceBinder();
    }

    /**
     * Handles binding to the {@link IsolatedService}.
     *
     * @param intent The Intent that was used to bind to this service, as given to {@link
     *     android.content.Context#bindService Context.bindService}. Note that any extras that were
     *     included with the Intent at that point will <em>not</em> be seen here.
     */
    @Override
    @Nullable
    public IBinder onBind(@NonNull Intent intent) {
        return mBinder;
    }

    /**
     * Return an instance of an {@link IsolatedWorker} that handles client requests.
     *
     * @param requestToken an opaque token that identifies the current request to the service that
     *     must be passed to service methods that depend on per-request state.
     */
    @NonNull
    public abstract IsolatedWorker onRequest(@NonNull RequestToken requestToken);

    /**
     * Returns a Data Access Object for the REMOTE_DATA table. The REMOTE_DATA table is a read-only
     * key-value store that contains data that is periodically downloaded from an endpoint declared
     * in the <download> tag in the ODP manifest of the service, as shown in the following example.
     *
     * <pre>{@code
     * <!-- Contents of res/xml/OdpSettings.xml -->
     * <on-device-personalization>
     * <!-- Name of the service subclass -->
     * <service "com.example.odpsample.SampleService">
     *   <!-- If this tag is present, ODP will periodically poll this URL and
     *    download content to populate REMOTE_DATA. Adopters that do not need to
     *    download content from their servers can skip this tag. -->
     *   <download-settings url="https://example.com/get" />
     * </service>
     * </on-device-personalization>
     * }</pre>
     *
     * @param requestToken an opaque token that identifies the current request to the service.
     * @return A {@link KeyValueStore} object that provides access to the REMOTE_DATA table. The
     *     methods in the returned {@link KeyValueStore} are blocking operations and should be
     *     called from a worker thread and not the main thread or a binder thread.
     * @see #onRequest(RequestToken)
     */
    @NonNull
    public final KeyValueStore getRemoteData(@NonNull RequestToken requestToken) {
        return new RemoteDataImpl(requestToken.getDataAccessService());
    }

    /**
     * Returns a Data Access Object for the LOCAL_DATA table. The LOCAL_DATA table is a persistent
     * key-value store that the service can use to store any data. The contents of this table are
     * visible only to the service running in an isolated process and cannot be sent outside the
     * device.
     *
     * @param requestToken an opaque token that identifies the current request to the service.
     * @return A {@link MutableKeyValueStore} object that provides access to the LOCAL_DATA table.
     *     The methods in the returned {@link MutableKeyValueStore} are blocking operations and
     *     should be called from a worker thread and not the main thread or a binder thread.
     * @see #onRequest(RequestToken)
     */
    @NonNull
    public final MutableKeyValueStore getLocalData(@NonNull RequestToken requestToken) {
        return new LocalDataImpl(requestToken.getDataAccessService());
    }

    /**
     * Returns a DAO for the REQUESTS and EVENTS tables that provides
     * access to the rows that are readable by the IsolatedService.
     *
     * @param requestToken an opaque token that identifies the current request to the service.
     * @return A {@link LogReader} object that provides access to the REQUESTS and EVENTS table.
     *     The methods in the returned {@link LogReader} are blocking operations and
     *     should be called from a worker thread and not the main thread or a binder thread.
     * @see #onRequest(RequestToken)
     */
    @NonNull
    public final LogReader getLogReader(@NonNull RequestToken requestToken) {
        return new LogReader(requestToken.getDataAccessService());
    }

    /**
     * Returns an {@link EventUrlProvider} for the current request. The {@link EventUrlProvider}
     * provides URLs that can be embedded in HTML. When the HTML is rendered in an
     * {@link android.webkit.WebView}, the platform intercepts requests to these URLs and invokes
     * {@code IsolatedWorker#onEvent(EventInput, Consumer)}.
     *
     * @param requestToken an opaque token that identifies the current request to the service.
     * @return An {@link EventUrlProvider} that returns event tracking URLs.
     * @see #onRequest(RequestToken)
     */
    @NonNull
    public final EventUrlProvider getEventUrlProvider(@NonNull RequestToken requestToken) {
        return new EventUrlProvider(requestToken.getDataAccessService());
    }

    /**
     * Returns the platform-provided {@link UserData} for the current request.
     *
     * @param requestToken an opaque token that identifies the current request to the service.
     * @return A {@link UserData} object.
     * @see #onRequest(RequestToken)
     */
    @Nullable
    public final UserData getUserData(@NonNull RequestToken requestToken) {
        return requestToken.getUserData();
    }

    /**
     * Returns an {@link FederatedComputeScheduler} for the current request. The {@link
     * FederatedComputeScheduler} can be used to schedule and cancel federated computation jobs. The
     * federated computation includes federated learning and federated analytics jobs.
     *
     * @param requestToken an opaque token that identifies the current request to the service.
     * @return An {@link FederatedComputeScheduler} that returns a federated computation job
     *     scheduler.
     * @see #onRequest(RequestToken)
     */
    @NonNull
    public final FederatedComputeScheduler getFederatedComputeScheduler(
            @NonNull RequestToken requestToken) {
        return new FederatedComputeScheduler(
                requestToken.getFederatedComputeService(),
                requestToken.getDataAccessService());
    }

    /**
     * Returns an {@link ModelManager} for the current request. The {@link ModelManager} can be used
     * to do model inference. It only supports Tensorflow Lite model inference now.
     *
     * @param requestToken an opaque token that identifies the current request to the service.
     * @return An {@link ModelManager} that can be used for model inference.
     */
    @NonNull
    public final ModelManager getModelManager(@NonNull RequestToken requestToken) {
        return new ModelManager(
                requestToken.getDataAccessService(), requestToken.getModelService());
    }

    // TODO(b/228200518): Add onBidRequest()/onBidResponse() methods.

    class ServiceBinder extends IIsolatedService.Stub {
        @Override
        public void onRequest(
                int operationCode,
                @NonNull Bundle params,
                @NonNull IIsolatedServiceCallback resultCallback) {
            Objects.requireNonNull(params);
            Objects.requireNonNull(resultCallback);
            final long token = Binder.clearCallingIdentity();
            // TODO(b/228200518): Ensure that caller is ODP Service.
            // TODO(b/323592348): Add model inference in other flows.
            try {
                performRequest(operationCode, params, resultCallback);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        private void performRequest(
                int operationCode,
                @NonNull Bundle params,
                @NonNull IIsolatedServiceCallback resultCallback) {

            if (operationCode == Constants.OP_EXECUTE) {
                performExecute(params, resultCallback);
            } else if (operationCode == Constants.OP_DOWNLOAD) {
                performDownload(params, resultCallback);
            } else if (operationCode == Constants.OP_RENDER) {
                performRender(params, resultCallback);
            } else if (operationCode == Constants.OP_WEB_VIEW_EVENT) {
                performOnWebViewEvent(params, resultCallback);
            } else if (operationCode == Constants.OP_TRAINING_EXAMPLE) {
                performOnTrainingExample(params, resultCallback);
            } else if (operationCode == Constants.OP_WEB_TRIGGER) {
                performOnWebTrigger(params, resultCallback);
            } else {
                throw new IllegalArgumentException("Invalid op code: " + operationCode);
            }
        }

        private void performOnWebTrigger(
                @NonNull Bundle params, @NonNull IIsolatedServiceCallback resultCallback) {
            try {
                WebTriggerInputParcel inputParcel =
                        Objects.requireNonNull(
                                params.getParcelable(
                                        Constants.EXTRA_INPUT, WebTriggerInputParcel.class),
                                () ->
                                        String.format(
                                                "Missing '%s' from input params!",
                                                Constants.EXTRA_INPUT));
                WebTriggerInput input = new WebTriggerInput(inputParcel);
                IDataAccessService binder = getDataAccessService(params);
                UserData userData = params.getParcelable(Constants.EXTRA_USER_DATA, UserData.class);
                RequestToken requestToken = new RequestToken(binder, null, null, userData);
                IsolatedWorker isolatedWorker = IsolatedService.this.onRequest(requestToken);
                isolatedWorker.onWebTrigger(
                        input,
                        new WrappedCallback<WebTriggerOutput, WebTriggerOutputParcel>(
                                resultCallback, requestToken, v -> new WebTriggerOutputParcel(v)));
            } catch (Exception e) {
                sLogger.e(e, TAG + ": Exception during Isolated Service web trigger operation.");
                sendError(resultCallback, Constants.STATUS_INTERNAL_ERROR, e);
            }
        }

        private void performOnTrainingExample(
                @NonNull Bundle params, @NonNull IIsolatedServiceCallback resultCallback) {
            try {
                TrainingExamplesInputParcel inputParcel =
                        Objects.requireNonNull(
                                params.getParcelable(
                                        Constants.EXTRA_INPUT, TrainingExamplesInputParcel.class),
                                () ->
                                        String.format(
                                                "Missing '%s' from input params!",
                                                Constants.EXTRA_INPUT));
                TrainingExamplesInput input = new TrainingExamplesInput(inputParcel);
                IDataAccessService binder = getDataAccessService(params);
                UserData userData = params.getParcelable(Constants.EXTRA_USER_DATA, UserData.class);
                RequestToken requestToken = new RequestToken(binder, null, null, userData);
                IsolatedWorker isolatedWorker = IsolatedService.this.onRequest(requestToken);
                isolatedWorker.onTrainingExamples(
                        input,
                        new WrappedCallback<TrainingExamplesOutput, TrainingExamplesOutputParcel>(
                                resultCallback,
                                requestToken,
                                v ->
                                        new TrainingExamplesOutputParcel.Builder()
                                                .setTrainingExampleRecords(
                                                        new OdpParceledListSlice<
                                                                TrainingExampleRecord>(
                                                                v.getTrainingExampleRecords()))
                                                .build()));
            } catch (Exception e) {
                sLogger.e(e,
                        TAG + ": Exception during Isolated Service training example operation.");
                sendError(resultCallback, Constants.STATUS_INTERNAL_ERROR, e);
            }
        }

        private void performOnWebViewEvent(
                @NonNull Bundle params, @NonNull IIsolatedServiceCallback resultCallback) {
            try {
                EventInputParcel inputParcel =
                        Objects.requireNonNull(
                                params.getParcelable(Constants.EXTRA_INPUT, EventInputParcel.class),
                                () ->
                                        String.format(
                                                "Missing '%s' from input params!",
                                                Constants.EXTRA_INPUT));
                EventInput input = new EventInput(inputParcel);
                IDataAccessService binder = getDataAccessService(params);
                UserData userData = params.getParcelable(Constants.EXTRA_USER_DATA, UserData.class);
                RequestToken requestToken = new RequestToken(binder, null, null, userData);
                IsolatedWorker isolatedWorker = IsolatedService.this.onRequest(requestToken);
                isolatedWorker.onEvent(
                        input,
                        new WrappedCallback<EventOutput, EventOutputParcel>(
                                resultCallback, requestToken, v -> new EventOutputParcel(v)));
            } catch (Exception e) {
                sLogger.e(e, TAG + ": Exception during Isolated Service web view event operation.");
                sendError(resultCallback, Constants.STATUS_INTERNAL_ERROR, e);
            }
        }

        private void performRender(
                @NonNull Bundle params, @NonNull IIsolatedServiceCallback resultCallback) {
            try {
                RenderInputParcel inputParcel =
                        Objects.requireNonNull(
                                params.getParcelable(
                                        Constants.EXTRA_INPUT, RenderInputParcel.class),
                                () ->
                                        String.format(
                                                "Missing '%s' from input params!",
                                                Constants.EXTRA_INPUT));
                RenderInput input = new RenderInput(inputParcel);
                Objects.requireNonNull(input.getRenderingConfig());
                IDataAccessService binder = getDataAccessService(params);
                RequestToken requestToken = new RequestToken(binder, null, null, null);
                IsolatedWorker isolatedWorker = IsolatedService.this.onRequest(requestToken);
                isolatedWorker.onRender(
                        input,
                        new WrappedCallback<RenderOutput, RenderOutputParcel>(
                                resultCallback, requestToken, v -> new RenderOutputParcel(v)));
            } catch (Exception e) {
                sLogger.e(e, TAG + ": Exception during Isolated Service render operation.");
                sendError(resultCallback, Constants.STATUS_INTERNAL_ERROR, e);
            }
        }

        private void performDownload(
                @NonNull Bundle params, @NonNull IIsolatedServiceCallback resultCallback) {
            try {
                DownloadInputParcel inputParcel =
                        Objects.requireNonNull(
                                params.getParcelable(
                                        Constants.EXTRA_INPUT, DownloadInputParcel.class),
                                () ->
                                        String.format(
                                                "Missing '%s' from input params!",
                                                Constants.EXTRA_INPUT));
                KeyValueStore downloadedContents =
                        new RemoteDataImpl(
                                IDataAccessService.Stub.asInterface(
                                        Objects.requireNonNull(
                                                inputParcel.getDataAccessServiceBinder(),
                            "Failed to get IDataAccessService binder from the input params!")));

                DownloadCompletedInput input =
                        new DownloadCompletedInput(downloadedContents);

                IDataAccessService binder = getDataAccessService(params);

                IFederatedComputeService fcBinder = getFederatedComputeService(params);
                UserData userData = params.getParcelable(Constants.EXTRA_USER_DATA, UserData.class);
                RequestToken requestToken = new RequestToken(binder, fcBinder, null, userData);
                IsolatedWorker isolatedWorker = IsolatedService.this.onRequest(requestToken);
                isolatedWorker.onDownloadCompleted(
                        input,
                        new WrappedCallback<DownloadCompletedOutput, DownloadCompletedOutputParcel>(
                                resultCallback,
                                requestToken,
                                v -> new DownloadCompletedOutputParcel(v)));
            } catch (Exception e) {
                sLogger.e(e, TAG + ": Exception during Isolated Service download operation.");
                sendError(resultCallback, Constants.STATUS_INTERNAL_ERROR, e);
            }
        }

        private static IIsolatedModelService getIsolatedModelService(@NonNull Bundle params) {
            IIsolatedModelService modelServiceBinder =
                    IIsolatedModelService.Stub.asInterface(
                            Objects.requireNonNull(
                                    params.getBinder(Constants.EXTRA_MODEL_SERVICE_BINDER),
                                    () ->
                                            String.format(
                                                    "Missing '%s' from input params!",
                                                    Constants.EXTRA_MODEL_SERVICE_BINDER)));
            Objects.requireNonNull(
                    modelServiceBinder,
                    "Failed to get IIsolatedModelService binder from the input params!");
            return modelServiceBinder;
        }

        private static IFederatedComputeService getFederatedComputeService(@NonNull Bundle params) {
            IFederatedComputeService fcBinder =
                    IFederatedComputeService.Stub.asInterface(
                            Objects.requireNonNull(
                                    params.getBinder(
                                            Constants.EXTRA_FEDERATED_COMPUTE_SERVICE_BINDER),
                                    () ->
                                            String.format(
                                                    "Missing '%s' from input params!",
                                                    Constants
                                                        .EXTRA_FEDERATED_COMPUTE_SERVICE_BINDER)));
            Objects.requireNonNull(
                    fcBinder,
                    "Failed to get IFederatedComputeService binder from the input params!");
            return fcBinder;
        }

        private static IDataAccessService getDataAccessService(@NonNull Bundle params) {
            IDataAccessService binder =
                    IDataAccessService.Stub.asInterface(
                            Objects.requireNonNull(
                                    params.getBinder(Constants.EXTRA_DATA_ACCESS_SERVICE_BINDER),
                                    () ->
                                            String.format(
                                                    "Missing '%s' from input params!",
                                                    Constants.EXTRA_DATA_ACCESS_SERVICE_BINDER)));
            Objects.requireNonNull(
                    binder, "Failed to get IDataAccessService binder from the input params!");
            return binder;
        }

        private void performExecute(
                @NonNull Bundle params, @NonNull IIsolatedServiceCallback resultCallback) {
            try {
                ExecuteInputParcel inputParcel =
                        Objects.requireNonNull(
                                params.getParcelable(
                                        Constants.EXTRA_INPUT, ExecuteInputParcel.class),
                                () ->
                                        String.format(
                                                "Missing '%s' from input params!",
                                                Constants.EXTRA_INPUT));
                ExecuteInput input = new ExecuteInput(inputParcel);
                Objects.requireNonNull(
                        input.getAppPackageName(),
                        "Failed to get AppPackageName from the input params!");
                IDataAccessService binder = getDataAccessService(params);
                IFederatedComputeService fcBinder = getFederatedComputeService(params);
                IIsolatedModelService modelServiceBinder = getIsolatedModelService(params);
                UserData userData = params.getParcelable(Constants.EXTRA_USER_DATA, UserData.class);
                RequestToken requestToken =
                        new RequestToken(binder, fcBinder, modelServiceBinder, userData);
                IsolatedWorker isolatedWorker = IsolatedService.this.onRequest(requestToken);
                isolatedWorker.onExecute(
                        input,
                        new WrappedCallback<ExecuteOutput, ExecuteOutputParcel>(
                                resultCallback, requestToken, v -> new ExecuteOutputParcel(v)));
            } catch (Exception e) {
                sLogger.e(e, TAG + ": Exception during Isolated Service execute operation.");
                sendError(resultCallback, Constants.STATUS_INTERNAL_ERROR, e);
            }
        }
    }

    private void sendError(IIsolatedServiceCallback resultCallback, int errorCode, Throwable t) {
        try {
            resultCallback.onError(
                    errorCode, 0, ExceptionInfo.toByteArray(t, MAX_EXCEPTION_CHAIN_DEPTH));
        } catch (RemoteException re) {
            sLogger.e(re, TAG + ": Isolated Service Callback failed.");
        }
    }

    private static class WrappedCallback<T, U extends Parcelable>
                implements OutcomeReceiver<T, IsolatedServiceException> {
        @NonNull private final IIsolatedServiceCallback mCallback;
        @NonNull private final RequestToken mRequestToken;
        @NonNull private final Function<T, U> mConverter;

        WrappedCallback(
                IIsolatedServiceCallback callback,
                RequestToken requestToken,
                Function<T, U> converter) {
            mCallback = Objects.requireNonNull(callback);
            mRequestToken = Objects.requireNonNull(requestToken);
            mConverter = Objects.requireNonNull(converter);
        }

        @Override
        public void onResult(T result) {
            long elapsedTimeMillis =
                    SystemClock.elapsedRealtime() - mRequestToken.getStartTimeMillis();
            if (result == null) {
                sendError(0, new IllegalArgumentException("missing result"));
            } else {
                Bundle bundle = new Bundle();
                U wrappedResult = mConverter.apply(result);
                bundle.putParcelable(Constants.EXTRA_RESULT, wrappedResult);
                bundle.putParcelable(Constants.EXTRA_CALLEE_METADATA,
                        new CalleeMetadata.Builder()
                            .setElapsedTimeMillis(elapsedTimeMillis)
                            .build());
                try {
                    mCallback.onSuccess(bundle);
                } catch (RemoteException e) {
                    sLogger.w(TAG + ": Callback failed.", e);
                }
            }
        }

        @Override
        public void onError(IsolatedServiceException e) {
            sendError(e.getErrorCode(), e);
        }

        private void sendError(int isolatedServiceErrorCode, Throwable t) {
            try {
                // TODO(b/324478256): Log and report the error code from e.
                mCallback.onError(
                        Constants.STATUS_SERVICE_FAILED,
                        isolatedServiceErrorCode,
                        ExceptionInfo.toByteArray(t, MAX_EXCEPTION_CHAIN_DEPTH));
            } catch (RemoteException re) {
                sLogger.w(TAG + ": Callback failed.", re);
            }
        }
    }
}
