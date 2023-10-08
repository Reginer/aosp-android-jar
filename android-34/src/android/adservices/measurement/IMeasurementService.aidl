/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.adservices.measurement;

import android.adservices.common.CallerMetadata;
import android.adservices.measurement.DeletionParam;
import android.adservices.measurement.IMeasurementApiStatusCallback;
import android.adservices.measurement.IMeasurementCallback;
import android.adservices.measurement.RegistrationRequest;
import android.adservices.measurement.StatusParam;
import android.adservices.measurement.WebSourceRegistrationRequestInternal;
import android.adservices.measurement.WebTriggerRegistrationRequestInternal;

/**
 * Measurement service.
 * {@hide}
 */
interface IMeasurementService {
    void register(in RegistrationRequest params, in CallerMetadata callerMetadata,
            in IMeasurementCallback callback);
    void registerWebSource(in WebSourceRegistrationRequestInternal params,
            in CallerMetadata callerMetadata, in IMeasurementCallback callback);
    void registerWebTrigger(in WebTriggerRegistrationRequestInternal params,
            in CallerMetadata callerMetadata, in IMeasurementCallback callback);
    void getMeasurementApiStatus(in StatusParam statusParam, in CallerMetadata callerMetadata,
            in IMeasurementApiStatusCallback callback);
    void deleteRegistrations(in DeletionParam params, in CallerMetadata callerMetadata,
            in IMeasurementCallback callback);
}
