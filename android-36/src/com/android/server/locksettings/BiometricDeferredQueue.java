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

package com.android.server.locksettings;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.service.gatekeeper.IGateKeeperService;
import android.util.ArraySet;
import android.util.Slog;

import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.biometrics.BiometricHandlerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class that handles biometric-related work in the {@link LockSettingsService} area, for example
 * resetLockout.
 */
@SuppressWarnings("deprecation")
public class BiometricDeferredQueue {
    private static final String TAG = "BiometricDeferredQueue";

    @NonNull private final SyntheticPasswordManager mSpManager;
    @NonNull private final Handler mHandler;
    @Nullable private FingerprintManager mFingerprintManager;
    @Nullable private FaceManager mFaceManager;
    @Nullable private BiometricManager mBiometricManager;

    // Entries added by LockSettingsService once a user's synthetic password is known. At this point
    // things are still keyed by userId.
    @NonNull private final ArrayList<UserAuthInfo> mPendingResetLockoutsForFingerprint;
    @NonNull private final ArrayList<UserAuthInfo> mPendingResetLockoutsForFace;
    @NonNull private final ArrayList<UserAuthInfo> mPendingResetLockouts;

    /**
     * Authentication info for a successful user unlock via Synthetic Password. This can be used to
     * perform multiple operations (e.g. resetLockout for multiple HALs/Sensors) by sending the
     * Gatekeeper Password to Gatekeer multiple times, each with a sensor-specific challenge.
     */
    private static class UserAuthInfo {
        final int userId;
        @NonNull final byte[] gatekeeperPassword;

        UserAuthInfo(int userId, @NonNull byte[] gatekeeperPassword) {
            this.userId = userId;
            this.gatekeeperPassword = gatekeeperPassword;
        }
    }

    /**
     * Per-authentication callback.
     */
    private static class FaceResetLockoutTask implements FaceManager.GenerateChallengeCallback {
        interface FinishCallback {
            void onFinished();
        }

        @NonNull FinishCallback finishCallback;
        @NonNull FaceManager faceManager;
        @NonNull SyntheticPasswordManager spManager;
        @NonNull Set<Integer> sensorIds; // IDs of sensors waiting for challenge
        @NonNull List<UserAuthInfo> pendingResetLockuts;

        FaceResetLockoutTask(
                @NonNull FinishCallback finishCallback,
                @NonNull FaceManager faceManager,
                @NonNull SyntheticPasswordManager spManager,
                @NonNull Set<Integer> sensorIds,
                @NonNull List<UserAuthInfo> pendingResetLockouts) {
            this.finishCallback = finishCallback;
            this.faceManager = faceManager;
            this.spManager = spManager;
            this.sensorIds = sensorIds;
            this.pendingResetLockuts = pendingResetLockouts;
        }

        @Override
        public void onGenerateChallengeResult(int sensorId, int userId, long challenge) {
            if (!sensorIds.contains(sensorId)) {
                Slog.e(TAG, "Unknown sensorId received: " + sensorId);
                return;
            }

            // Challenge received for a sensor. For each sensor, reset lockout for all users.
            for (UserAuthInfo userAuthInfo : pendingResetLockuts) {
                Slog.d(TAG, "Resetting face lockout for sensor: " + sensorId
                        + ", user: " + userAuthInfo.userId);
                final byte[] hat = requestHatFromGatekeeperPassword(spManager, userAuthInfo,
                        challenge);
                if (hat != null) {
                    faceManager.resetLockout(sensorId, userAuthInfo.userId, hat);
                }
            }

            sensorIds.remove(sensorId);
            faceManager.revokeChallenge(sensorId, userId, challenge);

            if (sensorIds.isEmpty()) {
                Slog.d(TAG, "Done requesting resetLockout for all face sensors");
                finishCallback.onFinished();
            }
        }
    }

    @Nullable private FaceResetLockoutTask mFaceResetLockoutTask;
    private final FaceResetLockoutTask.FinishCallback mFaceFinishCallback = () -> {
        mFaceResetLockoutTask = null;
    };

    BiometricDeferredQueue(@NonNull SyntheticPasswordManager spManager) {
        mSpManager = spManager;

        //Using a higher priority thread to avoid any delays and interruption of clients
        mHandler = BiometricHandlerProvider.getInstance().getBiometricCallbackHandler();
        mPendingResetLockoutsForFingerprint = new ArrayList<>();
        mPendingResetLockoutsForFace = new ArrayList<>();
        mPendingResetLockouts = new ArrayList<>();
    }

    public void systemReady(@Nullable FingerprintManager fingerprintManager,
            @Nullable FaceManager faceManager, @Nullable BiometricManager biometricManager) {
        mFingerprintManager = fingerprintManager;
        mFaceManager = faceManager;
        mBiometricManager = biometricManager;
    }

    /**
     * Adds a request for resetLockout on all biometric sensors for the user specified. The queue
     * owner must invoke {@link #processPendingLockoutResets()} at some point to kick off the
     * operations.
     *
     * Note that this should only ever be invoked for successful authentications, otherwise it will
     * consume a Gatekeeper authentication attempt and potentially wipe the user/device.
     *
     * @param userId             The user that the operation will apply for.
     * @param gatekeeperPassword The Gatekeeper Password
     */
    void addPendingLockoutResetForUser(int userId, @NonNull byte[] gatekeeperPassword) {
        mHandler.post(() -> {
            if (mFaceManager != null && mFaceManager.hasEnrolledTemplates(userId)) {
                Slog.d(TAG, "Face addPendingLockoutResetForUser: " + userId);
                mPendingResetLockoutsForFace.add(new UserAuthInfo(userId, gatekeeperPassword));
            }

            if (mFingerprintManager != null
                    && mFingerprintManager.hasEnrolledFingerprints(userId)) {
                Slog.d(TAG, "Fingerprint addPendingLockoutResetForUser: " + userId);
                mPendingResetLockoutsForFingerprint.add(new UserAuthInfo(userId,
                        gatekeeperPassword));
            }

            if (mBiometricManager != null) {
                Slog.d(TAG, "Fingerprint addPendingLockoutResetForUser: " + userId);
                mPendingResetLockouts.add(new UserAuthInfo(userId,
                        gatekeeperPassword));
            }
        });
    }

    void processPendingLockoutResets() {
        mHandler.post(() -> {
            if (!mPendingResetLockoutsForFace.isEmpty()) {
                Slog.d(TAG, "Processing pending resetLockout for face");
                processPendingLockoutsForFace(new ArrayList<>(mPendingResetLockoutsForFace));
                mPendingResetLockoutsForFace.clear();
            }

            if (!mPendingResetLockoutsForFingerprint.isEmpty()) {
                Slog.d(TAG, "Processing pending resetLockout for fingerprint");
                processPendingLockoutsForFingerprint(
                        new ArrayList<>(mPendingResetLockoutsForFingerprint));
                mPendingResetLockoutsForFingerprint.clear();
            }

            if (!mPendingResetLockouts.isEmpty()) {
                Slog.d(TAG, "Processing pending resetLockouts(Generic)");
                processPendingLockoutsGeneric(
                        new ArrayList<>(mPendingResetLockouts));
                mPendingResetLockouts.clear();
            }

        });
    }

    private void processPendingLockoutsForFingerprint(List<UserAuthInfo> pendingResetLockouts) {
        if (mFingerprintManager != null) {
            final List<FingerprintSensorPropertiesInternal> fingerprintSensorProperties =
                    mFingerprintManager.getSensorPropertiesInternal();
            for (FingerprintSensorPropertiesInternal prop : fingerprintSensorProperties) {
                if (!prop.resetLockoutRequiresHardwareAuthToken) {
                    for (UserAuthInfo user : pendingResetLockouts) {
                        mFingerprintManager.resetLockout(prop.sensorId, user.userId,
                                null /* hardwareAuthToken */);
                    }
                } else if (!prop.resetLockoutRequiresChallenge) {
                    for (UserAuthInfo user : pendingResetLockouts) {
                        Slog.d(TAG, "Resetting fingerprint lockout for sensor: " + prop.sensorId
                                + ", user: " + user.userId);
                        final byte[] hat = requestHatFromGatekeeperPassword(mSpManager, user,
                                0 /* challenge */);
                        if (hat != null) {
                            mFingerprintManager.resetLockout(prop.sensorId, user.userId, hat);
                        }
                    }
                } else {
                    Slog.w(TAG, "No fingerprint HAL interface requires HAT with challenge"
                            + ", sensorId: " + prop.sensorId);
                }
            }
        }
    }

    private void processPendingLockoutsForFace(List<UserAuthInfo> pendingResetLockouts) {
        if (mFaceManager != null) {
            if (mFaceResetLockoutTask != null) {
                // This code will need to be updated if this problem ever occurs.
                Slog.w(TAG,
                        "mFaceGenerateChallengeCallback not null, previous operation may be stuck");
            }
            final List<FaceSensorPropertiesInternal> faceSensorProperties =
                    mFaceManager.getSensorPropertiesInternal();
            final Set<Integer> sensorIds = new ArraySet<>();
            for (FaceSensorPropertiesInternal prop : faceSensorProperties) {
                sensorIds.add(prop.sensorId);
            }

            mFaceResetLockoutTask = new FaceResetLockoutTask(mFaceFinishCallback, mFaceManager,
                    mSpManager, sensorIds, pendingResetLockouts);
            for (final FaceSensorPropertiesInternal prop : faceSensorProperties) {
                if (prop.resetLockoutRequiresHardwareAuthToken) {
                    for (UserAuthInfo user : pendingResetLockouts) {
                        if (prop.resetLockoutRequiresChallenge) {
                            Slog.d(TAG, "Generating challenge for sensor: " + prop.sensorId
                                    + ", user: " + user.userId);
                            mFaceManager.generateChallenge(prop.sensorId, user.userId,
                                    mFaceResetLockoutTask);
                        } else {
                            Slog.d(TAG, "Resetting face lockout for sensor: " + prop.sensorId
                                    + ", user: " + user.userId);
                            final byte[] hat = requestHatFromGatekeeperPassword(mSpManager, user,
                                    0 /* challenge */);
                            if (hat != null) {
                                mFaceManager.resetLockout(prop.sensorId, user.userId, hat);
                            }
                        }
                    }
                } else {
                    Slog.w(TAG, "Lockout is below the HAL for all face authentication interfaces"
                            + ", sensorId: " + prop.sensorId);
                }
            }
        }
    }

    private void processPendingLockoutsGeneric(List<UserAuthInfo> pendingResetLockouts) {
        for (UserAuthInfo user : pendingResetLockouts) {
            Slog.d(TAG, "Resetting biometric lockout for user: " + user.userId);
            final byte[] hat = requestHatFromGatekeeperPassword(mSpManager, user,
                    0 /* challenge */);
            if (hat != null) {
                mBiometricManager.resetLockout(user.userId, hat);
            }
        }
    }

    @Nullable
    private static byte[] requestHatFromGatekeeperPassword(
            @NonNull SyntheticPasswordManager spManager,
            @NonNull UserAuthInfo userAuthInfo, long challenge) {
        final VerifyCredentialResponse response = spManager.verifyChallengeInternal(
                getGatekeeperService(), userAuthInfo.gatekeeperPassword, challenge,
                userAuthInfo.userId);
        if (response == null) {
            Slog.wtf(TAG, "VerifyChallenge failed, null response");
            return null;
        }
        if (response.getResponseCode() != VerifyCredentialResponse.RESPONSE_OK) {
            Slog.wtf(TAG, "VerifyChallenge failed, response: "
                    + response.getResponseCode());
            return null;
        }
        if (response.getGatekeeperHAT() == null) {
            Slog.e(TAG, "Null HAT received from spManager");
        }

        return response.getGatekeeperHAT();
    }

    @Nullable
    private static synchronized IGateKeeperService getGatekeeperService() {
        final IBinder service = ServiceManager.waitForService(Context.GATEKEEPER_SERVICE);
        if (service == null) {
            Slog.e(TAG, "Unable to acquire GateKeeperService");
            return null;
        }
        return IGateKeeperService.Stub.asInterface(service);
    }
}
