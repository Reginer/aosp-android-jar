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

package android.adservices.adselection;

import android.annotation.NonNull;

/**
 * This POJO represents the {@link TestAdSelectionManager
 * #removeAdSelectionFromOutcomesConfigRemoteInfoOverride(
 * RemoveAdSelectionFromOutcomesOverrideRequest, Executor, OutcomeReceiver)} request
 *
 * <p>It contains one field, a {@link AdSelectionFromOutcomesConfig} which serves as the identifier
 * of the override to be removed
 *
 * @hide
 */
public class RemoveAdSelectionFromOutcomesOverrideRequest {
    @NonNull private final AdSelectionFromOutcomesConfig mAdSelectionFromOutcomesConfig;

    /** Builds a {@link RemoveAdSelectionOverrideRequest} instance. */
    public RemoveAdSelectionFromOutcomesOverrideRequest(
            @NonNull AdSelectionFromOutcomesConfig config) {
        mAdSelectionFromOutcomesConfig = config;
    }

    /** @return AdSelectionConfig, the configuration of the ad selection process. */
    @NonNull
    public AdSelectionFromOutcomesConfig getAdSelectionFromOutcomesConfig() {
        return mAdSelectionFromOutcomesConfig;
    }
}
