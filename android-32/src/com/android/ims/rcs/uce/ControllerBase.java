/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.ims.rcs.uce;

import com.android.ims.RcsFeatureManager;

/**
 * The base interface of each controllers.
 */
public interface ControllerBase {
    /**
     * The RcsFeature has been connected to the framework.
     */
    void onRcsConnected(RcsFeatureManager manager);

    /**
     * The framework has lost the binding to the RcsFeature.
     */
    void onRcsDisconnected();

    /**
     * Notify to destroy this instance. The UceController instance is unusable after destroyed.
     */
    void onDestroy();

    /**
     * Notify the controller that the Carrier Config has changed.
     */
    void onCarrierConfigChanged();
}
