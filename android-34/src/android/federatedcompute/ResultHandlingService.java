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

package android.federatedcompute;

import static android.federatedcompute.common.ClientConstants.STATUS_SUCCESS;

import android.app.Service;
import android.content.Intent;
import android.federatedcompute.aidl.IFederatedComputeCallback;
import android.federatedcompute.aidl.IResultHandlingService;
import android.federatedcompute.common.ExampleConsumption;
import android.federatedcompute.common.TrainingOptions;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.function.Consumer;

/**
 * The abstract base class that client apps need to implement to handle training results.
 *
 * <p>The client app will add a {@code <service>} entry to their manifest so that FederatedCompute
 * API can bind to the their implementation, like so:
 *
 * <pre>{@code
 * <application>
 *   <service android:enabled="true" android:exported="true" android:name=".YourServiceClass">
 *     <intent-filter>
 *       <action android:name="android.federatedcompute.COMPUTATION_RESULT" />
 *       <data android:scheme="app" />
 *     </intent-filter>
 *   </service>
 * </application>
 * }</pre>
 *
 * @hide
 */
public abstract class ResultHandlingService extends Service {
    private static final String TAG = "ResultHandlingService";
    private IBinder mIBinder;

    @Override
    public void onCreate() {
        mIBinder = new ServiceBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    private class ServiceBinder extends IResultHandlingService.Stub {
        @Override
        public void handleResult(
                TrainingOptions trainingOptions,
                boolean success,
                List<ExampleConsumption> exampleConsumptionList,
                IFederatedComputeCallback callback) {
            ResultHandlingService.this.handleResult(
                    trainingOptions,
                    success,
                    exampleConsumptionList,
                    new ResultHandlingCallback(callback));
        }
    }

    /** A callback for the user to communicate if the results handling is successful. */
    private static final class ResultHandlingCallback implements Consumer<Integer> {
        private final IFederatedComputeCallback mInternalCallback;

        /** Constructor for this */
        ResultHandlingCallback(IFederatedComputeCallback internalCallback) {
            this.mInternalCallback = internalCallback;
        }

        /**
         * Should be called when finished handling results in your {@link ResultHandlingService}
         * implementation.
         */
        @Override
        public void accept(Integer status) {
            try {
                if (status == STATUS_SUCCESS) {
                    mInternalCallback.onSuccess();
                    return;
                }
                mInternalCallback.onFailure(status);
            } catch (RemoteException e) {
                Log.w(TAG, "An error occurred when trying to communicate with FederatedCompute.");
            }
        }
    }

    /**
     * The client app needs to implement this method to handle results. After handling the results,
     * the client app should signal FederatedCompute via the ResultHandlingCallback.
     */
    public abstract void handleResult(
            TrainingOptions trainingOptions,
            boolean success,
            List<ExampleConsumption> exampleConsumptionList,
            Consumer<Integer> callback);
}
