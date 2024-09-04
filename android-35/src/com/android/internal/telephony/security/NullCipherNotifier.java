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

package com.android.internal.telephony.security;

import static android.telephony.SecurityAlgorithmUpdate.SECURITY_ALGORITHM_UNKNOWN;

import static com.android.internal.telephony.security.CellularNetworkSecuritySafetySource.NULL_CIPHER_STATE_ENCRYPTED;
import static com.android.internal.telephony.security.CellularNetworkSecuritySafetySource.NULL_CIPHER_STATE_NOTIFY_ENCRYPTED;
import static com.android.internal.telephony.security.CellularNetworkSecuritySafetySource.NULL_CIPHER_STATE_NOTIFY_NON_ENCRYPTED;

import android.annotation.IntDef;
import android.content.Context;
import android.telephony.SecurityAlgorithmUpdate;
import android.telephony.SecurityAlgorithmUpdate.ConnectionEvent;
import android.telephony.SecurityAlgorithmUpdate.SecurityAlgorithm;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Encapsulates logic to emit notifications to the user that a null cipher is in use. A null cipher
 * is one that does not attempt to implement any encryption.
 *
 * <p>This class will either emit notifications through SafetyCenterManager if SafetyCenter exists
 * on a device, or it will emit system notifications otherwise.
 *
 * TODO(b/319662115): handle radio availability, no service, SIM removal, etc.
 *
 * @hide
 */
public class NullCipherNotifier {
    private static final String TAG = "NullCipherNotifier";
    private static NullCipherNotifier sInstance;

    private final CellularNetworkSecuritySafetySource mSafetySource;
    private final HashMap<Integer, SubscriptionState> mSubscriptionState = new HashMap<>();
    private final HashMap<Integer, Integer> mActiveSubscriptions = new HashMap<>();

    private final Object mEnabledLock = new Object();
    @GuardedBy("mEnabledLock")
    private boolean mEnabled = false;

    // This is a single threaded executor. This is important because we want to ensure certain
    // events are strictly serialized.
    private ScheduledExecutorService mSerializedWorkQueue;

    /**
     * Gets a singleton NullCipherNotifier.
     */
    public static synchronized NullCipherNotifier getInstance(
            CellularNetworkSecuritySafetySource safetySource) {
        if (sInstance == null) {
            sInstance = new NullCipherNotifier(
                    Executors.newSingleThreadScheduledExecutor(),
                    safetySource);
        }
        return sInstance;
    }

    @VisibleForTesting
    public NullCipherNotifier(
            ScheduledExecutorService notificationQueue,
            CellularNetworkSecuritySafetySource safetySource) {
        mSerializedWorkQueue = notificationQueue;
        mSafetySource = safetySource;
    }

    /**
     * Adds a security algorithm update. If appropriate, this will trigger a user notification.
     */
    public void onSecurityAlgorithmUpdate(
            Context context, int phoneId, int subId, SecurityAlgorithmUpdate update) {
        Rlog.d(TAG, "Security algorithm update: subId = " + subId + " " + update);

        if (shouldIgnoreUpdate(update)) {
            return;
        }

        if (!isEnabled()) {
            Rlog.i(TAG, "Ignoring onSecurityAlgorithmUpdate. Notifier is disabled.");
            return;
        }

        try {
            mSerializedWorkQueue.execute(() -> {
                try {
                    maybeUpdateSubscriptionMapping(context, phoneId, subId);
                    SubscriptionState subState = mSubscriptionState.get(subId);
                    if (subState == null) {
                        subState = new SubscriptionState();
                        mSubscriptionState.put(subId, subState);
                    }

                    @CellularNetworkSecuritySafetySource.NullCipherState int nullCipherState =
                            subState.update(update);
                    mSafetySource.setNullCipherState(context, subId, nullCipherState);
                } catch (Throwable t) {
                    Rlog.e(TAG, "Failed to execute onSecurityAlgorithmUpdate " + t.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            Rlog.e(TAG, "Failed to schedule onSecurityAlgorithmUpdate: " + e.getMessage());
        }
    }

    /**
     * Set or update the current phoneId to subId mapping. When a new subId is mapped to a phoneId,
     * we update the safety source to clear state of the old subId.
     */
    public void setSubscriptionMapping(Context context, int phoneId, int subId) {

        if (!isEnabled()) {
            Rlog.i(TAG, "Ignoring setSubscriptionMapping. Notifier is disabled.");
        }

        try {
            mSerializedWorkQueue.execute(() -> {
                try {
                    maybeUpdateSubscriptionMapping(context, phoneId, subId);
                } catch (Throwable t) {
                    Rlog.e(TAG, "Failed to update subId mapping. phoneId: " + phoneId + " subId: "
                            + subId + ". " + t.getMessage());
                }
            });

        } catch (RejectedExecutionException e) {
            Rlog.e(TAG, "Failed to schedule setSubscriptionMapping: " + e.getMessage());
        }
    }

    private void maybeUpdateSubscriptionMapping(Context context, int phoneId, int subId) {
        final Integer oldSubId = mActiveSubscriptions.put(phoneId, subId);
        if (oldSubId == null || oldSubId == subId) {
            return;
        }

        // Our subId was updated for this phone, we should clear this subId's state.
        mSubscriptionState.remove(oldSubId);
        mSafetySource.clearNullCipherState(context, oldSubId);
    }


    /**
     * Enables null cipher notification; {@code onSecurityAlgorithmUpdate} will start handling
     * security algorithm updates and send notifications to the user when required.
     */
    public void enable(Context context) {
        synchronized (mEnabledLock) {
            Rlog.d(TAG, "enabled");
            mEnabled = true;
            scheduleOnEnabled(context, true);
        }
    }

    /**
     * Clear all internal state and prevent further notifications until re-enabled. This can be
     * used in response to a user disabling the feature for null cipher notifications. If
     * {@code onSecurityAlgorithmUpdate} is called while in a disabled state, security algorithm
     * updates will be dropped.
     */
    public void disable(Context context) {
        synchronized (mEnabledLock) {
            Rlog.d(TAG, "disabled");
            mEnabled = false;
            scheduleOnEnabled(context, false);
        }
    }

    /** Checks whether the null cipher notification is enabled. */
    public boolean isEnabled() {
        synchronized (mEnabledLock) {
            return mEnabled;
        }
    }

    private void scheduleOnEnabled(Context context, boolean enabled) {
        try {
            mSerializedWorkQueue.execute(() -> {
                Rlog.i(TAG, "On enable notifier. Enable value: " + enabled);
                mSafetySource.setNullCipherIssueEnabled(context, enabled);
            });
        } catch (RejectedExecutionException e) {
            Rlog.e(TAG, "Failed to schedule onEnableNotifier: " + e.getMessage());
        }

    }

    /** Returns whether the update should be dropped and the monitoring state left unchanged. */
    private static boolean shouldIgnoreUpdate(SecurityAlgorithmUpdate update) {
        // Ignore emergencies.
        if (update.isUnprotectedEmergency()) {
            return true;
        }

        switch (update.getConnectionEvent()) {
            // Ignore non-link layer protocols. Monitoring is only looking for data exposed
            // over-the-air so only the link layer protocols are tracked. Higher-level protocols can
            // protect data further into the network but that is out of scope.
            case SecurityAlgorithmUpdate.CONNECTION_EVENT_VOLTE_SIP:
            case SecurityAlgorithmUpdate.CONNECTION_EVENT_VOLTE_RTP:
            case SecurityAlgorithmUpdate.CONNECTION_EVENT_VONR_SIP:
            case SecurityAlgorithmUpdate.CONNECTION_EVENT_VONR_RTP:
            // Ignore emergencies.
            case SecurityAlgorithmUpdate.CONNECTION_EVENT_VOLTE_SIP_SOS:
            case SecurityAlgorithmUpdate.CONNECTION_EVENT_VOLTE_RTP_SOS:
            case SecurityAlgorithmUpdate.CONNECTION_EVENT_VONR_SIP_SOS:
            case SecurityAlgorithmUpdate.CONNECTION_EVENT_VONR_RTP_SOS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Determines whether an algorithm does not attempt to implement any encryption and is therefore
     * considered a null cipher.
     *
     * <p>Only the algorithms known to be null ciphers are classified as such. Explicitly unknown
     * algorithms, or algorithms that are unknown by means of values added to newer HALs, are
     * assumed not to be null ciphers.
     */
    private static boolean isNullCipher(@SecurityAlgorithm int algorithm) {
        switch (algorithm) {
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_A50:
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_GEA0:
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_UEA0:
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_EEA0:
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_NEA0:
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_IMS_NULL:
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_SIP_NULL:
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_SRTP_NULL:
            case SecurityAlgorithmUpdate.SECURITY_ALGORITHM_OTHER:
                return true;
            default:
                return false;
        }
    }

    /** The state of network connections for a subscription. */
    private static final class SubscriptionState {
        private @NetworkClass int mActiveNetworkClass = NETWORK_CLASS_UNKNOWN;
        private final HashMap<Integer, ConnectionState> mState = new HashMap<>();

        private @CellularNetworkSecuritySafetySource.NullCipherState int
                update(SecurityAlgorithmUpdate update) {
            boolean fromNullCipherState = hasNullCipher();

            @NetworkClass int networkClass = getNetworkClass(update.getConnectionEvent());
            if (networkClass != mActiveNetworkClass || networkClass == NETWORK_CLASS_UNKNOWN) {
                mState.clear();
                mActiveNetworkClass = networkClass;
            }

            ConnectionState fromState =
                    mState.getOrDefault(update.getConnectionEvent(), ConnectionState.UNKNOWN);
            ConnectionState toState = new ConnectionState(
                    update.getEncryption() == SECURITY_ALGORITHM_UNKNOWN
                            ? fromState.getEncryption()
                            : update.getEncryption(),
                    update.getIntegrity() == SECURITY_ALGORITHM_UNKNOWN
                            ? fromState.getIntegrity()
                            : update.getIntegrity());
            mState.put(update.getConnectionEvent(), toState);

            if (hasNullCipher()) {
                return NULL_CIPHER_STATE_NOTIFY_NON_ENCRYPTED;
            }
            if (!fromNullCipherState || mActiveNetworkClass == NETWORK_CLASS_UNKNOWN) {
                return NULL_CIPHER_STATE_ENCRYPTED;
            }
            return NULL_CIPHER_STATE_NOTIFY_ENCRYPTED;
        }

        private boolean hasNullCipher() {
            return mState.values().stream().anyMatch(ConnectionState::hasNullCipher);
        }

        private static final int NETWORK_CLASS_UNKNOWN = 0;
        private static final int NETWORK_CLASS_2G = 2;
        private static final int NETWORK_CLASS_3G = 3;
        private static final int NETWORK_CLASS_4G = 4;
        private static final int NETWORK_CLASS_5G = 5;

        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = {"NETWORK_CLASS_"}, value = {NETWORK_CLASS_UNKNOWN,
                NETWORK_CLASS_2G, NETWORK_CLASS_3G, NETWORK_CLASS_4G,
                NETWORK_CLASS_5G})
        private @interface NetworkClass {}

        private static @NetworkClass int getNetworkClass(
                @ConnectionEvent int connectionEvent) {
            switch (connectionEvent) {
                case SecurityAlgorithmUpdate.CONNECTION_EVENT_CS_SIGNALLING_GSM:
                case SecurityAlgorithmUpdate.CONNECTION_EVENT_PS_SIGNALLING_GPRS:
                    return NETWORK_CLASS_2G;
                case SecurityAlgorithmUpdate.CONNECTION_EVENT_CS_SIGNALLING_3G:
                case SecurityAlgorithmUpdate.CONNECTION_EVENT_PS_SIGNALLING_3G:
                    return NETWORK_CLASS_3G;
                case SecurityAlgorithmUpdate.CONNECTION_EVENT_NAS_SIGNALLING_LTE:
                case SecurityAlgorithmUpdate.CONNECTION_EVENT_AS_SIGNALLING_LTE:
                    return NETWORK_CLASS_4G;
                case SecurityAlgorithmUpdate.CONNECTION_EVENT_NAS_SIGNALLING_5G:
                case SecurityAlgorithmUpdate.CONNECTION_EVENT_AS_SIGNALLING_5G:
                    return NETWORK_CLASS_5G;
                default:
                    return NETWORK_CLASS_UNKNOWN;
            }
        }
    }

    /** The state of security algorithms for a network connection. */
    private static final class ConnectionState {
        private static final ConnectionState UNKNOWN =
                new ConnectionState(SECURITY_ALGORITHM_UNKNOWN, SECURITY_ALGORITHM_UNKNOWN);

        private final @SecurityAlgorithm int mEncryption;
        private final @SecurityAlgorithm int mIntegrity;

        private ConnectionState(
                @SecurityAlgorithm int encryption, @SecurityAlgorithm int integrity) {
            mEncryption = encryption;
            mIntegrity = integrity;
        }

        private @SecurityAlgorithm int getEncryption() {
            return mEncryption;
        }

        private @SecurityAlgorithm int getIntegrity() {
            return mIntegrity;
        }

        private boolean hasNullCipher() {
            return isNullCipher(mEncryption) || isNullCipher(mIntegrity);
        }
    };
}
