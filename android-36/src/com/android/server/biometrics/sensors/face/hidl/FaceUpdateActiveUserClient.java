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

package com.android.server.biometrics.sensors.face.hidl;

import android.annotation.NonNull;
import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Slog;

import com.android.server.biometrics.BiometricsProto;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.StartUserClient;
import com.android.server.biometrics.sensors.face.aidl.AidlSession;

import java.io.File;
import java.util.Map;
import java.util.function.Supplier;

public class FaceUpdateActiveUserClient extends StartUserClient<IBiometricsFace, AidlSession> {
    private static final String TAG = "FaceUpdateActiveUserClient";
    private static final String FACE_DATA_DIR = "facedata";

    private final boolean mHasEnrolledBiometrics;
    @NonNull private final Map<Integer, Long> mAuthenticatorIds;

    FaceUpdateActiveUserClient(@NonNull Context context,
            @NonNull Supplier<IBiometricsFace> lazyDaemon, int userId, @NonNull String owner,
            int sensorId, @NonNull BiometricLogger logger,
            @NonNull BiometricContext biometricContext, boolean hasEnrolledBiometrics,
            @NonNull Map<Integer, Long> authenticatorIds) {
        this(context, lazyDaemon, (newUserId, newUser, halInterfaceVersion) -> {},
                userId, owner, sensorId, logger, biometricContext, hasEnrolledBiometrics,
                authenticatorIds);
    }

    FaceUpdateActiveUserClient(@NonNull Context context,
            @NonNull Supplier<IBiometricsFace> lazyDaemon, UserStartedCallback userStartedCallback,
            int userId, @NonNull String owner, int sensorId, @NonNull BiometricLogger logger,
            @NonNull BiometricContext biometricContext, boolean hasEnrolledBiometrics,
            @NonNull Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, null /* token */, userId, sensorId, logger, biometricContext,
                userStartedCallback);
        mHasEnrolledBiometrics = hasEnrolledBiometrics;
        mAuthenticatorIds = authenticatorIds;
    }

    @Override
    public void start(@NonNull ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override
    public void unableToStart() {
        // Nothing to do here
    }

    @Override
    protected void startHalOperation() {
        final File storePath = new File(Environment.getDataVendorDeDirectory(getTargetUserId()),
                FACE_DATA_DIR);
        if (!storePath.exists()) {
            Slog.e(TAG, "vold has not created the directory?");
            mCallback.onClientFinished(this, false /* success */);
            return;
        }

        try {
            final IBiometricsFace daemon = getFreshDaemon();
            daemon.setActiveUser(getTargetUserId(), storePath.getAbsolutePath());
            mAuthenticatorIds.put(getTargetUserId(),
                    mHasEnrolledBiometrics ? daemon.getAuthenticatorId().value : 0L);
            mUserStartedCallback.onUserStarted(getTargetUserId(), null, 0);
            mCallback.onClientFinished(this, true /* success */);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to setActiveUser: " + e);
            mCallback.onClientFinished(this, false /* success */);
        }
    }

    @Override
    public int getProtoEnum() {
        return BiometricsProto.CM_UPDATE_ACTIVE_USER;
    }
}
