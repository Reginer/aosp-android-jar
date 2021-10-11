/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.setupwizardlib;

import java.util.Map;

/**
 * Listener interface for callbacks from asynchronous calls made through the {@link
 * InitialLockSetupClient} interface.
 */
public interface InitialLockListener {
    /**
     * Method to be called when the connection to the lock service is
     * finished. Will pass through whether or not the connection was
     * successful. This connection will fail if there is a lock already set.
     */
    void onConnectionAttemptFinished(boolean successful);

    /**
     * Returns a map from the
     * {@link com.android.car.setupwizardlib.InitialLockSetupConstants.LockTypes}
     * to the corresponding {@link LockConfig}
     */
    void onGetLockConfigs(Map<Integer, LockConfig> lockConfigMap);

    /**
     * Method to be called when the lock validation has been completed, passes through whether
     * the lock is valid to be saved or if not, the error reason.
     */
    void onLockValidated(@InitialLockSetupConstants.ValidateLockFlags int valid);

    /**
     * Method to be called when the attempt to save the lock is finished. Will
     * pass through whether or not the lock was set successfully.
     */
    void onSetLockFinished(@InitialLockSetupConstants.SetLockCodes int result);

    /**
     * Method to be called when the service disconnects.
     */
    void onDisconnect();
}

