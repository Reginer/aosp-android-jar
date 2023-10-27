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

import android.annotation.NonNull;
import android.app.Service;
import android.content.Intent;
import android.federatedcompute.aidl.IExampleStoreCallback;
import android.federatedcompute.aidl.IExampleStoreService;
import android.os.Bundle;
import android.os.IBinder;

/**
 * The abstract base class that client apps hosting their own Example Stores must implement.
 *
 * <p>The FederatedCompute will call into client apps' implementations to fetch data to use during
 * the training of new models or get the aggregation analytic result. Apps must add a {@code
 * <service>} entry to their manifest so that the FederatedCompute can bind to their implementation,
 * like so:
 *
 * <pre>{@code
 * <application>
 *   <service android:enabled="true" android:exported="true" android:name=".YourServiceClass">
 *     <intent-filter>
 *       <action android:name="com.android.federatedcompute.EXAMPLE_STORE"/>
 *       <data android:scheme="app"/>
 *     </intent-filter>
 *   </service>
 * </application>
 * }</pre>
 *
 * @hide
 */
public abstract class ExampleStoreService extends Service {
    private static final String TAG = "ExampleStoreService";
    private IBinder mIBinder;

    @Override
    public void onCreate() {
        mIBinder = new ServiceBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    class ServiceBinder extends IExampleStoreService.Stub {
        @Override
        public void startQuery(Bundle params, IExampleStoreCallback callback) {
            ExampleStoreService.this.startQuery(
                    params, new ExampleStoreQueryCallbackImpl(callback));
        }
    }
    /**
     * The abstract method that client apps should implement to start a new example store query
     * using the given selection criteria.
     */
    public abstract void startQuery(@NonNull Bundle params, @NonNull QueryCallback callback);
    /**
     * The client apps use this callback to return their ExampleStoreIterator implementation to the
     * federated training service.
     */
    public interface QueryCallback {
        /** Called when the iterator is ready for use. */
        void onStartQuerySuccess(@NonNull ExampleStoreIterator iterator);
        /** Called when an error occurred and the iterator cannot not be created. */
        void onStartQueryFailure(int errorCode);
    }
}
