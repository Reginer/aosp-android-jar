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
import android.adservices.ondevicepersonalization.aidl.IFederatedComputeCallback;
import android.adservices.ondevicepersonalization.aidl.IFederatedComputeService;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.WorkerThread;
import android.federatedcompute.common.TrainingOptions;
import android.os.RemoteException;

import com.android.adservices.ondevicepersonalization.flags.Flags;
import com.android.ondevicepersonalization.internal.util.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Handles scheduling federated compute jobs. See {@link
 * IsolatedService#getFederatedComputeScheduler}.
 */
@FlaggedApi(Flags.FLAG_ON_DEVICE_PERSONALIZATION_APIS_ENABLED)
public class FederatedComputeScheduler {
    private static final String TAG = FederatedComputeScheduler.class.getSimpleName();
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getLogger();

    private final IFederatedComputeService mFcService;
    private final IDataAccessService mDataAccessService;

    /** @hide */
    public FederatedComputeScheduler(
            IFederatedComputeService binder, IDataAccessService dataService) {
        mFcService = binder;
        mDataAccessService = dataService;
    }

    // TODO(b/300461799): add federated compute server document.
    // TODO(b/269665435): add sample code snippet.
    /**
     * Schedules a federated compute job. In {@link IsolatedService#onRequest}, the app can call
     * {@link IsolatedService#getFederatedComputeScheduler} to pass scheduler when construct {@link
     * IsolatedWorker}.
     *
     * @param params parameters related to job scheduling.
     * @param input the configuration of the federated compute. It should be consistent with the
     *     federated compute server setup.
     */
    @WorkerThread
    public void schedule(@NonNull Params params, @NonNull FederatedComputeInput input) {
        final long startTimeMillis = System.currentTimeMillis();
        int responseCode = Constants.STATUS_INTERNAL_ERROR;
        if (mFcService == null) {
            throw new IllegalStateException(
                    "FederatedComputeScheduler not available for this instance.");
        }

        android.federatedcompute.common.TrainingInterval trainingInterval =
                convertTrainingInterval(params.getTrainingInterval());
        TrainingOptions trainingOptions =
                new TrainingOptions.Builder()
                        .setPopulationName(input.getPopulationName())
                        .setTrainingInterval(trainingInterval)
                        .build();
        CountDownLatch latch = new CountDownLatch(1);
        final int[] err = {0};
        try {
            mFcService.schedule(
                    trainingOptions,
                    new IFederatedComputeCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(int i) {
                            err[0] = i;
                            latch.countDown();
                        }
                    });
            latch.await();
            if (err[0] != 0) {
                throw new IllegalStateException("Internal failure occurred while scheduling job");
            }
            responseCode = Constants.STATUS_SUCCESS;
        } catch (RemoteException | InterruptedException e) {
            sLogger.e(TAG + ": Failed to schedule federated compute job", e);
            throw new IllegalStateException(e);
        } finally {
            logApiCallStats(
                    Constants.API_NAME_FEDERATED_COMPUTE_SCHEDULE,
                    System.currentTimeMillis() - startTimeMillis,
                    responseCode);
        }
    }

    /**
     * Cancels a federated compute job with input training params. In {@link
     * IsolatedService#onRequest}, the app can call {@link
     * IsolatedService#getFederatedComputeScheduler} to pass scheduler when construct {@link
     * IsolatedWorker}.
     *
     * @param input the configuration of the federated compute. It should be consistent with the
     *     federated compute server setup.
     */
    @WorkerThread
    public void cancel(@NonNull FederatedComputeInput input) {
        final long startTimeMillis = System.currentTimeMillis();
        int responseCode = Constants.STATUS_INTERNAL_ERROR;
        if (mFcService == null) {
            throw new IllegalStateException(
                    "FederatedComputeScheduler not available for this instance.");
        }
        CountDownLatch latch = new CountDownLatch(1);
        final int[] err = {0};
        try {
            mFcService.cancel(
                    input.getPopulationName(),
                    new IFederatedComputeCallback.Stub() {
                        @Override
                        public void onSuccess() {
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(int i) {
                            err[0] = i;
                            latch.countDown();
                        }
                    });
            latch.await();
            if (err[0] != 0) {
                throw new IllegalStateException("Internal failure occurred while cancelling job");
            }
            responseCode = Constants.STATUS_SUCCESS;
        } catch (RemoteException | InterruptedException e) {
            sLogger.e(TAG + ": Failed to cancel federated compute job", e);
            throw new IllegalStateException(e);
        } finally {
            logApiCallStats(
                    Constants.API_NAME_FEDERATED_COMPUTE_CANCEL,
                    System.currentTimeMillis() - startTimeMillis,
                    responseCode);
        }
    }

    private android.federatedcompute.common.TrainingInterval convertTrainingInterval(
            TrainingInterval interval) {
        return new android.federatedcompute.common.TrainingInterval.Builder()
                .setMinimumIntervalMillis(interval.getMinimumInterval().toMillis())
                .setSchedulingMode(convertSchedulingMode(interval))
                .build();
    }

    private @android.federatedcompute.common.TrainingInterval.SchedulingMode int
            convertSchedulingMode(TrainingInterval interval) {
        switch (interval.getSchedulingMode()) {
            case TrainingInterval.SCHEDULING_MODE_ONE_TIME:
                return android.federatedcompute.common.TrainingInterval.SCHEDULING_MODE_ONE_TIME;
            case TrainingInterval.SCHEDULING_MODE_RECURRENT:
                return android.federatedcompute.common.TrainingInterval.SCHEDULING_MODE_RECURRENT;
            default:
                throw new IllegalStateException(
                        "Unsupported scheduling mode " + interval.getSchedulingMode());
        }
    }

    private void logApiCallStats(int apiName, long duration, int responseCode) {
        try {
            mDataAccessService.logApiCallStats(apiName, duration, responseCode);
        } catch (Exception e) {
            sLogger.d(e, TAG + ": failed to log metrics");
        }
    }

    /** The parameters related to job scheduling. */
    public static class Params {
        /**
         * If training interval is scheduled for recurrent tasks, the earliest time this task could
         * start is after the minimum training interval expires. E.g. If the task is set to run
         * maximum once per day, the first run of this task will be one day after this task is
         * scheduled. When a one time job is scheduled, the earliest next runtime is calculated
         * based on federated compute default interval.
         */
        @NonNull private final TrainingInterval mTrainingInterval;

        public Params(@NonNull TrainingInterval trainingInterval) {
            mTrainingInterval = trainingInterval;
        }

        @NonNull
        public TrainingInterval getTrainingInterval() {
            return mTrainingInterval;
        }
    }
}
