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

package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.annotation.NonNull;
import android.content.Context;
import android.hardware.biometrics.BiometricsProtoEnums;
import android.hardware.fingerprint.Fingerprint;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.InternalEnumerateClient;

import java.util.List;
import java.util.function.Supplier;

/**
 * Fingerprint-specific internal client supporting the
 * {@link android.hardware.biometrics.fingerprint.IFingerprint} AIDL interface.
 */
@VisibleForTesting
public class FingerprintInternalEnumerateClient extends InternalEnumerateClient<AidlSession> {
    private static final String TAG = "FingerprintInternalEnumerateClient";

    protected FingerprintInternalEnumerateClient(@NonNull Context context,
            @NonNull Supplier<AidlSession> lazyDaemon, @NonNull IBinder token, int userId,
            @NonNull String owner, @NonNull List<Fingerprint> enrolledList,
            @NonNull BiometricUtils<Fingerprint> utils, int sensorId,
            @NonNull BiometricLogger logger, @NonNull BiometricContext biometricContext) {
        super(context, lazyDaemon, token, userId, owner, enrolledList, utils, sensorId,
                logger, biometricContext);
    }

    @Override
    protected void startHalOperation() {
        try {
            getFreshDaemon().getSession().enumerateEnrollments();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting enumerate", e);
            mCallback.onClientFinished(this, false /* success */);
        }
    }

    @Override
    protected int getModality() {
        return BiometricsProtoEnums.MODALITY_FINGERPRINT;
    }
}
