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

package com.android.server.biometrics;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricsProtoEnums;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.biometrics.sensors.BiometricNotification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculate and collect on-device False Rejection Rates (FRR).
 * FRR = All [given biometric modality] unlock failures / all [given biometric modality] unlock
 * attempts.
 */
public class AuthenticationStatsCollector {

    private static final String TAG = "AuthenticationStatsCollector";

    // The minimum number of attempts that will calculate the FRR and trigger the notification.
    private static final int MINIMUM_ATTEMPTS = 150;
    // Upload the data every 50 attempts (average number of daily authentications).
    private static final int AUTHENTICATION_UPLOAD_INTERVAL = 50;
    // The maximum number of eligible biometric enrollment notification can be sent.
    @VisibleForTesting
    static final int MAXIMUM_ENROLLMENT_NOTIFICATIONS = Flags.frrDialogImprovement() ? 3 : 1;
    @VisibleForTesting
    static final Duration FRR_MINIMAL_DURATION = Duration.ofDays(7);

    public static final String ACTION_LAST_ENROLL_TIME_CHANGED = "last_enroll_time_changed";
    public static final String EXTRA_MODALITY = "modality";

    @NonNull private final Context mContext;
    @NonNull private final PackageManager mPackageManager;
    @Nullable private final FaceManager mFaceManager;
    @Nullable private final FingerprintManager mFingerprintManager;

    private final boolean mEnabled;
    private final float mThreshold;
    private final int mModality;

    @NonNull private final Map<Integer, AuthenticationStats> mUserAuthenticationStatsMap;
    @NonNull private AuthenticationStatsPersister mAuthenticationStatsPersister;
    @NonNull private BiometricNotification mBiometricNotification;
    @NonNull private final Clock mClock;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, @NonNull Intent intent) {
            final int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL);

            if (userId != UserHandle.USER_NULL
                    && intent.getAction().equals(Intent.ACTION_USER_REMOVED)) {
                Slog.d(TAG, "Removing data for user: " + userId);
                onUserRemoved(userId);
            }
        }
    };

    private final BroadcastReceiver mEnrollTimeUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, @NonNull Intent intent) {
            int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL);
            int modality = intent.getIntExtra(EXTRA_MODALITY,
                    BiometricsProtoEnums.MODALITY_UNKNOWN);
            if (modality == mModality) {
                updateAuthenticationStatsMapIfNeeded(userId);
                AuthenticationStats authenticationStats =
                        mUserAuthenticationStatsMap.get(userId);
                Slog.d(TAG, "Update enroll time for user: " + userId);
                authenticationStats.updateLastEnrollmentTime(mClock.millis());
            }
        }
    };

    public AuthenticationStatsCollector(@NonNull Context context, int modality,
            @NonNull BiometricNotification biometricNotification, @NonNull Clock clock) {
        mContext = context;
        mEnabled = context.getResources().getBoolean(R.bool.config_biometricFrrNotificationEnabled);
        mThreshold = context.getResources()
                .getFraction(R.fraction.config_biometricNotificationFrrThreshold, 1, 1);
        mUserAuthenticationStatsMap = new HashMap<>();
        mModality = modality;
        mBiometricNotification = biometricNotification;
        mClock = clock;

        mPackageManager = context.getPackageManager();
        mFaceManager = mContext.getSystemService(FaceManager.class);
        mFingerprintManager = mContext.getSystemService(FingerprintManager.class);

        mAuthenticationStatsPersister = new AuthenticationStatsPersister(mContext);

        initializeUserAuthenticationStatsMap();
        mAuthenticationStatsPersister.persistFrrThreshold(mThreshold);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_USER_REMOVED);
        context.registerReceiver(mBroadcastReceiver, intentFilter);

        IntentFilter enrollTimeChangedFilter = new IntentFilter();
        enrollTimeChangedFilter.addAction(ACTION_LAST_ENROLL_TIME_CHANGED);
        context.registerReceiver(mEnrollTimeUpdatedReceiver, enrollTimeChangedFilter,
                Context.RECEIVER_NOT_EXPORTED);
    }

    private void initializeUserAuthenticationStatsMap() {
        for (AuthenticationStats stats :
                mAuthenticationStatsPersister.getAllFrrStats(mModality)) {
            mUserAuthenticationStatsMap.put(stats.getUserId(), stats);
        }
    }

    /** Update total authentication and rejected attempts. */
    public void authenticate(int userId, boolean authenticated) {

        // Don't collect data if the feature is disabled.
        if (!mEnabled) {
            return;
        }

        // Don't collect data for single-modality devices or user has both biometrics enrolled.
        if (isSingleModalityDevice()
                || (hasEnrolledFace(userId) && hasEnrolledFingerprint(userId))) {
            return;
        }

        updateAuthenticationStatsMapIfNeeded(userId);

        AuthenticationStats authenticationStats = mUserAuthenticationStatsMap.get(userId);
        if (authenticationStats.getEnrollmentNotifications() >= MAXIMUM_ENROLLMENT_NOTIFICATIONS) {
            return;
        }

        authenticationStats.authenticate(authenticated);

        sendNotificationIfNeeded(userId);

        persistDataIfNeeded(userId);
    }

    private void updateAuthenticationStatsMapIfNeeded(int userId) {
        // SharedPreference is not ready when starting system server, initialize
        // mUserAuthenticationStatsMap in authentication to ensure SharedPreference
        // is ready for application use.
        if (mUserAuthenticationStatsMap.isEmpty()) {
            initializeUserAuthenticationStatsMap();
        }
        // Check if this is a new user.
        if (!mUserAuthenticationStatsMap.containsKey(userId)) {
            mUserAuthenticationStatsMap.put(userId, new AuthenticationStats(userId, mModality));
        }
    }

    /** Check if a notification should be sent after a calculation cycle. */
    private void sendNotificationIfNeeded(int userId) {
        AuthenticationStats authenticationStats = mUserAuthenticationStatsMap.get(userId);
        if (authenticationStats.getTotalAttempts() < MINIMUM_ATTEMPTS) {
            return;
        }

        boolean showFrr;
        if (Flags.frrDialogImprovement()) {
            long lastFrrOrEnrollTime = Math.max(authenticationStats.getLastEnrollmentTime(),
                    authenticationStats.getLastFrrNotificationTime());
            showFrr = authenticationStats.getEnrollmentNotifications()
                            < MAXIMUM_ENROLLMENT_NOTIFICATIONS
                    && authenticationStats.getFrr() >= mThreshold
                    && isFrrMinimalDurationPassed(lastFrrOrEnrollTime);
        } else {
            showFrr = authenticationStats.getEnrollmentNotifications()
                            < MAXIMUM_ENROLLMENT_NOTIFICATIONS
                    && authenticationStats.getFrr() >= mThreshold;
        }

        // Don't send notification if FRR below the threshold.
        if (!showFrr) {
            authenticationStats.resetData();
            return;
        }

        authenticationStats.resetData();

        if (Flags.frrDialogImprovement()
                && mModality == BiometricsProtoEnums.MODALITY_FINGERPRINT) {
            boolean sent = mBiometricNotification.sendCustomizeFpFrrNotification(mContext);
            if (sent) {
                authenticationStats.updateLastFrrNotificationTime(mClock.millis());
                authenticationStats.updateNotificationCounter();
                return;
            }
        }

        final boolean hasEnrolledFace = hasEnrolledFace(userId);
        final boolean hasEnrolledFingerprint = hasEnrolledFingerprint(userId);

        if (hasEnrolledFace && !hasEnrolledFingerprint) {
            mBiometricNotification.sendFpEnrollNotification(mContext);
            authenticationStats.updateLastFrrNotificationTime(mClock.millis());
            authenticationStats.updateNotificationCounter();
        } else if (!hasEnrolledFace && hasEnrolledFingerprint) {
            mBiometricNotification.sendFaceEnrollNotification(mContext);
            authenticationStats.updateLastFrrNotificationTime(mClock.millis());
            authenticationStats.updateNotificationCounter();
        }
    }

    private boolean isFrrMinimalDurationPassed(long previousMillis) {
        Instant previous = Instant.ofEpochMilli(previousMillis);
        long nowMillis = mClock.millis();
        Instant now = Instant.ofEpochMilli(nowMillis);

        if (now.isAfter(previous)) {
            Duration between = Duration.between(/* startInclusive= */ previous,
                    /* endExclusive= */ now);
            if (between.compareTo(FRR_MINIMAL_DURATION) > 0) {
                return true;
            } else {
                Slog.d(TAG, "isFrrMinimalDurationPassed, duration too short");
            }
        } else {
            Slog.d(TAG, "isFrrMinimalDurationPassed, date not match");
        }
        return false;
    }

    private void persistDataIfNeeded(int userId) {
        AuthenticationStats authenticationStats = mUserAuthenticationStatsMap.get(userId);
        if (authenticationStats.getTotalAttempts() % AUTHENTICATION_UPLOAD_INTERVAL == 0) {
            mAuthenticationStatsPersister.persistFrrStats(authenticationStats.getUserId(),
                    authenticationStats.getTotalAttempts(),
                    authenticationStats.getRejectedAttempts(),
                    authenticationStats.getEnrollmentNotifications(),
                    authenticationStats.getLastEnrollmentTime(),
                    authenticationStats.getLastFrrNotificationTime(),
                    authenticationStats.getModality());
        }
    }

    /**
     * This is meant for debug purposes only, this will bypass many checks.
     * The origination of this call should be from an adb shell command sent from
     * FaceService.
     *
     * adb shell cmd face notification
     */
    public void sendFaceReEnrollNotification() {
        mBiometricNotification.sendFaceEnrollNotification(mContext);
    }

    /**
     * This is meant for debug purposes only, this will bypass many checks.
     * The origination of this call should be from an adb shell command sent from
     * FingerprintService.
     *
     * adb shell cmd fingerprint notification
     */
    public void sendFingerprintReEnrollNotification() {
        mBiometricNotification.sendFpEnrollNotification(mContext);
    }

    private void onUserRemoved(final int userId) {
        mUserAuthenticationStatsMap.remove(userId);
        mAuthenticationStatsPersister.removeFrrStats(userId);
    }

    private boolean isSingleModalityDevice() {
        return !mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                || !mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE);
    }

    private boolean hasEnrolledFace(int userId) {
        return mFaceManager != null && mFaceManager.hasEnrolledTemplates(userId);
    }

    private boolean hasEnrolledFingerprint(int userId) {
        return mFingerprintManager != null && mFingerprintManager.hasEnrolledTemplates(userId);
    }

    /**
     * Only being used in tests. Callers should not make any changes to the returned
     * authentication stats.
     *
     * @return AuthenticationStats of the user, or null if the stats doesn't exist.
     */
    @Nullable
    @VisibleForTesting
    AuthenticationStats getAuthenticationStatsForUser(int userId) {
        return mUserAuthenticationStatsMap.getOrDefault(userId, null);
    }

    @VisibleForTesting
    void setAuthenticationStatsForUser(int userId, AuthenticationStats authenticationStats) {
        mUserAuthenticationStatsMap.put(userId, authenticationStats);
    }
}
