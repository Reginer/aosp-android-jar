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

package android.adservices.cobalt;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Abstract Base class for provider service to implement uploading of data to Cobalt's backend.
 *
 * <p>The implementor of this service needs to override the onUploadEncryptedCobaltEnvelope method.
 *
 * <p>Cobalt is a telemetry system with built-in support for differential privacy. See
 * https://fuchsia.googlesource.com/cobalt for a comprehensive overview of the project and the
 * Fuchsia client implementation.
 *
 * @hide
 */
@SystemApi
public abstract class AdServicesCobaltUploadService extends Service {
    /** The intent that the service must respond to. Add it to the intent filter of the service. */
    @SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE =
            "android.adservices.cobalt.AdServicesCobaltUploadService";

    /** Abstract method which will be overridden by the sender to upload the data */
    public abstract void onUploadEncryptedCobaltEnvelope(
            @NonNull EncryptedCobaltEnvelopeParams params);

    private final IAdServicesCobaltUploadService mInterface =
            new IAdServicesCobaltUploadService.Stub() {
                /**
                 * Send an encrypted envelope to Cobalt's backend.
                 *
                 * <p>Errors in this method execution, both because of problems within the binder
                 * call and in the service execution, will cause a RuntimeException to be thrown.
                 */
                @Override
                public void uploadEncryptedCobaltEnvelope(EncryptedCobaltEnvelopeParams params) {
                    onUploadEncryptedCobaltEnvelope(params);
                }
            };

    @Nullable
    @Override
    public final IBinder onBind(@Nullable Intent intent) {
        return mInterface.asBinder();
    }
}
