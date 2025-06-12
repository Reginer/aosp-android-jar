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

package android.os;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.SystemApi.Client;
import android.os.profiling.Flags;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

/**
 * Class for system to interact with {@link ProfilingService} to notify of trigger occurrences.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_SYSTEM_TRIGGERED_PROFILING_NEW)
@SystemApi(client = Client.MODULE_LIBRARIES)
public class ProfilingServiceHelper {
    private static final String TAG = ProfilingServiceHelper.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final Object sLock = new Object();

    @Nullable
    @GuardedBy("sLock")
    private static ProfilingServiceHelper sInstance;

    private final Object mLock = new Object();

    @NonNull
    @GuardedBy("mLock")
    private final IProfilingService mProfilingService;

    private ProfilingServiceHelper(@NonNull IProfilingService service) {
        mProfilingService = service;
    }

    /**
     * Returns an instance of {@link ProfilingServiceHelper}.
     *
     * @throws IllegalStateException if called before ProfilingService is set up.
     */
    @NonNull
    public static ProfilingServiceHelper getInstance() {
        synchronized (sLock) {
            if (sInstance != null) {
                return sInstance;
            }

            IProfilingService service = Flags.telemetryApis() ? IProfilingService.Stub.asInterface(
                    ProfilingFrameworkInitializer.getProfilingServiceManager()
                            .getProfilingServiceRegisterer().get()) : null;

            if (service == null) {
                throw new IllegalStateException("ProfilingService not yet set up.");
            }

            sInstance = new ProfilingServiceHelper(service);

            return sInstance;
        }
    }

    /** Send a trigger to {@link ProfilingService}. */
    public void onProfilingTriggerOccurred(int uid, @NonNull String packageName, int triggerType) {
        synchronized (mLock) {
            try {
                mProfilingService.processTrigger(uid, packageName, triggerType);
            } catch (RemoteException e) {
                // Exception sending trigger to service. Nothing to do here, trigger will be lost.
                if (DEBUG) Log.e(TAG, "Exception sending trigger", e);
            }
        }
    }
}
