/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.adservices.ondevicepersonalization.aidl.IIsolatedModelService;
import android.adservices.ondevicepersonalization.aidl.IIsolatedModelServiceCallback;
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.WorkerThread;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.os.RemoteException;

import com.android.ondevicepersonalization.internal.util.ByteArrayUtil;
import com.android.ondevicepersonalization.internal.util.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Handles model inference and only support TFLite model inference now. See {@link
 * IsolatedService#getModelManager}.
 */
public class ModelManager {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getLogger();
    private static final String TAG = ModelManager.class.getSimpleName();
    @NonNull private final IDataAccessService mDataService;

    @NonNull private final IIsolatedModelService mModelService;

    /** @hide */
    public ModelManager(
            @NonNull IDataAccessService dataService, @NonNull IIsolatedModelService modelService) {
        mDataService = dataService;
        mModelService = modelService;
    }

    /**
     * Run a single model inference. Only supports TFLite model inference now.
     *
     * @param input contains all the information needed for a run of model inference.
     * @param executor the {@link Executor} on which to invoke the callback.
     * @param receiver this returns a {@link InferenceOutput} which contains model inference result
     *     or {@link Exception} on failure.
     */
    @WorkerThread
    public void run(
            @NonNull InferenceInput input,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<InferenceOutput, Exception> receiver) {
        final long startTimeMillis = System.currentTimeMillis();
        Objects.requireNonNull(input);
        Bundle bundle = new Bundle();
        bundle.putBinder(Constants.EXTRA_DATA_ACCESS_SERVICE_BINDER, mDataService.asBinder());
        bundle.putParcelable(Constants.EXTRA_INFERENCE_INPUT, new InferenceInputParcel(input));
        try {
            mModelService.runInference(
                    bundle,
                    new IIsolatedModelServiceCallback.Stub() {
                        @Override
                        public void onSuccess(Bundle result) {
                            executor.execute(
                                    () -> {
                                        int responseCode = Constants.STATUS_SUCCESS;
                                        long endTimeMillis = System.currentTimeMillis();
                                        try {
                                            InferenceOutputParcel outputParcel =
                                                    Objects.requireNonNull(
                                                            result.getParcelable(
                                                                    Constants.EXTRA_RESULT,
                                                                    InferenceOutputParcel.class));
                                            // Set output result to both fields for LiteRT model
                                            // before Map field is deprecated.
                                            Map<Integer, Object> outputMap = new HashMap<>();
                                            try {
                                                outputMap =
                                                        (Map<Integer, Object>)
                                                                ByteArrayUtil.deserializeObject(
                                                                        outputParcel.getData());
                                            } catch (ClassCastException e) {
                                                // TODO: add logging
                                            }
                                            InferenceOutput output =
                                                    new InferenceOutput(
                                                            outputMap, outputParcel.getData());
                                            endTimeMillis = System.currentTimeMillis();
                                            receiver.onResult(output);
                                        } catch (Exception e) {
                                            endTimeMillis = System.currentTimeMillis();
                                            responseCode = Constants.STATUS_INTERNAL_ERROR;
                                            receiver.onError(e);
                                        } finally {
                                            logApiCallStats(
                                                    Constants.API_NAME_MODEL_MANAGER_RUN,
                                                    endTimeMillis - startTimeMillis,
                                                    responseCode);
                                        }
                                    });
                        }

                        @Override
                        public void onError(int errorCode) {
                            executor.execute(
                                    () -> {
                                        long endTimeMillis = System.currentTimeMillis();
                                        if (OnDevicePersonalizationException.isValidErrorCode(
                                                errorCode)) {
                                            receiver.onError(
                                                    new OnDevicePersonalizationException(
                                                            errorCode));
                                        } else {
                                            receiver.onError(
                                                    new IllegalStateException(
                                                            "Error: " + errorCode));
                                        }
                                        logApiCallStats(
                                                Constants.API_NAME_MODEL_MANAGER_RUN,
                                                endTimeMillis - startTimeMillis,
                                                errorCode);
                                    });
                        }
                    });
        } catch (RemoteException e) {
            receiver.onError(new IllegalStateException(e));
        }
    }

    private void logApiCallStats(int apiName, long duration, int responseCode) {
        try {
            mDataService.logApiCallStats(apiName, duration, responseCode);
        } catch (Exception e) {
            sLogger.d(e, TAG + ": failed to log metrics");
        }
    }
}
