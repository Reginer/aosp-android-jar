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

package com.android.internal.telephony.data;

import android.annotation.CurrentTimeMillisLong;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.telephony.data.DataProfile;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The class to describe a data evaluation for whether allowing or disallowing certain operations
 * like setup a data network, sustaining existing data networks, or handover between IWLAN and
 * cellular.
 */
public class DataEvaluation {
    /** The reason for this evaluation */
    @NonNull
    private final DataEvaluationReason mDataEvaluationReason;

    /** Data disallowed reasons. There could be multiple reasons for not allowing data. */
    @NonNull
    private final Set<DataDisallowedReason> mDataDisallowedReasons = new HashSet<>();

    /** Data allowed reason. It is intended to only have one allowed reason. */
    @NonNull
    private DataAllowedReason mDataAllowedReason = DataAllowedReason.NONE;

    @Nullable
    private DataProfile mCandidateDataProfile = null;

    /** The timestamp of evaluation time */
    @CurrentTimeMillisLong
    private long mEvaluatedTime = 0;

    /**
     * Constructor
     *
     * @param reason The reason for this evaluation.
     */
    public DataEvaluation(@NonNull DataEvaluationReason reason) {
        mDataEvaluationReason = reason;
    }

    /**
     * Add a data disallowed reason. Note that adding a disallowed reason will clean up the
     * allowed reason because they are mutual exclusive.
     *
     * @param reason Disallowed reason.
     */
    public void addDataDisallowedReason(DataDisallowedReason reason) {
        mDataAllowedReason = DataAllowedReason.NONE;
        mDataDisallowedReasons.add(reason);
        mEvaluatedTime = System.currentTimeMillis();
    }

    /**
     * Remove a data disallowed reason if one exists.
     *
     * @param reason Disallowed reason.
     */
    public void removeDataDisallowedReason(DataDisallowedReason reason) {
        mDataDisallowedReasons.remove(reason);
        mEvaluatedTime = System.currentTimeMillis();
    }

    /**
     * Add a data allowed reason. Note that adding an allowed reason will clean up the disallowed
     * reasons because they are mutual exclusive.
     *
     * @param reason Allowed reason.
     */
    public void addDataAllowedReason(DataAllowedReason reason) {
        mDataDisallowedReasons.clear();

        // Only higher priority allowed reason can overwrite the old one. See
        // DataAllowedReason for the oder.
        if (reason.ordinal() > mDataAllowedReason.ordinal()) {
            mDataAllowedReason = reason;
        }
        mEvaluatedTime = System.currentTimeMillis();
    }

    /**
     * @return List of data disallowed reasons.
     */
    @NonNull
    public List<DataDisallowedReason> getDataDisallowedReasons() {
        return new ArrayList<>(mDataDisallowedReasons);
    }

    /**
     * @return The data allowed reason.
     */
    @NonNull
    public DataAllowedReason getDataAllowedReason() {
        return mDataAllowedReason;
    }

    /**
     * Set the candidate data profile for setup data network.
     *
     * @param dataProfile The candidate data profile.
     */
    public void setCandidateDataProfile(@NonNull DataProfile dataProfile) {
        mCandidateDataProfile = dataProfile;
    }

    /**
     * @return The candidate data profile for setup data network.
     */
    @Nullable
    public DataProfile getCandidateDataProfile() {
        return mCandidateDataProfile;
    }

    /**
     * @return {@code true} if the evaluation contains disallowed reasons.
     */
    public boolean containsDisallowedReasons() {
        return !mDataDisallowedReasons.isEmpty();
    }

    /**
     * Check if it contains a certain disallowed reason.
     *
     * @param reason The disallowed reason to check.
     * @return {@code true} if the provided reason matches one of the disallowed reasons.
     */
    public boolean contains(DataDisallowedReason reason) {
        return mDataDisallowedReasons.contains(reason);
    }

    /**
     * Check if only one disallowed reason prevent data connection.
     *
     * @param reason The given reason to check
     * @return {@code true} if the given reason is the only one that prevents data connection
     */
    public boolean containsOnly(DataDisallowedReason reason) {
        return mDataDisallowedReasons.size() == 1 && contains(reason);
    }

    /**
     * Check if all the disallowed reasons are a subset of the given reason.
     *
     * @param reasons The given reason to check
     * @return {@code true} if it doesn't contain any disallowed reasons other than the given
     * reasons.
     */
    public boolean isSubsetOf(DataDisallowedReason... reasons) {
        int matched = 0;
        for (DataDisallowedReason requestedReason : reasons) {
            if (mDataDisallowedReasons.contains(requestedReason)) matched++;
        }
        return matched == mDataDisallowedReasons.size();
    }

    /**
     * Check if the any of the disallowed reasons match one of the provided reason.
     *
     * @param reasons The given reasons to check.
     * @return {@code true} if any of the given reasons matches one of the disallowed reasons.
     */
    public boolean containsAny(DataDisallowedReason... reasons) {
        for (DataDisallowedReason reason : reasons) {
            if (mDataDisallowedReasons.contains(reason)) return true;
        }
        return false;
    }

    /**
     * Check if the allowed reason is the specified reason.
     *
     * @param reason The allowed reason.
     * @return {@code true} if the specified reason matches the allowed reason.
     */
    public boolean contains(DataAllowedReason reason) {
        return reason == mDataAllowedReason;
    }

    /**
     * @return {@code true} if the disallowed reasons contains hard reasons.
     */
    public boolean containsHardDisallowedReasons() {
        for (DataDisallowedReason reason : mDataDisallowedReasons) {
            if (reason.isHardReason()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The reason for evaluating unsatisfied network requests, existing data networks, and handover.
     */
    @VisibleForTesting
    public enum DataEvaluationReason {
        /** New request from the apps. */
        NEW_REQUEST(false),
        /** Data config changed. */
        DATA_CONFIG_CHANGED(true),
        /** SIM is loaded. */
        SIM_LOADED(true),
        /** SIM is removed. */
        SIM_REMOVAL(true),
        /** Data profiles changed. */
        DATA_PROFILES_CHANGED(true),
        /** When service state changes.(For now only considering data RAT and data registration). */
        DATA_SERVICE_STATE_CHANGED(true),
        /** When data is enabled or disabled (by user, carrier, thermal, etc...) */
        DATA_ENABLED_CHANGED(true),
        /** When data enabled overrides are changed (MMS always allowed, data on non-DDS sub). */
        DATA_ENABLED_OVERRIDE_CHANGED(true),
        /** When data roaming is enabled or disabled. */
        ROAMING_ENABLED_CHANGED(true),
        /** When voice call ended (for concurrent voice/data not supported RAT). */
        VOICE_CALL_ENDED(true),
        /** When network restricts or no longer restricts mobile data. */
        DATA_RESTRICTED_CHANGED(true),
        /** Network capabilities changed. The unsatisfied requests might have chances to attach. */
        DATA_NETWORK_CAPABILITIES_CHANGED(true),
        /** When emergency call started or ended. */
        EMERGENCY_CALL_CHANGED(true),
        /** When data disconnected, re-evaluate later to see if data could be brought up again. */
        RETRY_AFTER_DISCONNECTED(true),
        /** Data setup retry. */
        DATA_RETRY(false),
        /** For handover evaluation, or for network tearing down after handover succeeds/fails. */
        DATA_HANDOVER(true),
        /** Preferred transport changed. */
        PREFERRED_TRANSPORT_CHANGED(true),
        /** Slice config changed. */
        SLICE_CONFIG_CHANGED(true),
        /** SRVCC state changed. */
        SRVCC_STATE_CHANGED(true),
        /**
         * Single data network arbitration. On certain RATs, only one data network is allowed at the
         * same time.
         */
        SINGLE_DATA_NETWORK_ARBITRATION(true),
        /** Query from {@link TelephonyManager#isDataConnectivityPossible()}. */
        EXTERNAL_QUERY(false),
        /** Tracking area code changed. */
        TAC_CHANGED(true),
        /** Unsatisfied network request detached. */
        UNSATISFIED_REQUEST_DETACHED(true),
        /** track bootstrap sim data usage */
        CHECK_DATA_USAGE(false);

        /**
         * {@code true} if the evaluation is due to environmental changes (i.e. SIM removal,
         * registration state changes, etc....
         */
        private final boolean mIsConditionBased;

        /**
         * @return {@code true} if the evaluation is due to environmental changes (i.e. SIM removal,
         * registration state changes, etc....
         */
        public boolean isConditionBased() {
            return mIsConditionBased;
        }

        /**
         * Constructor
         *
         * @param isConditionBased {@code true} if the evaluation is due to environmental changes
         * (i.e. SIM removal, registration state changes, etc....)
         */
        DataEvaluationReason(boolean isConditionBased) {
            mIsConditionBased = isConditionBased;
        }
    }

    /** Disallowed reasons. There could be multiple reasons if it is not allowed. */
    public enum DataDisallowedReason {
        // Soft failure reasons. A soft reason means that in certain conditions, data is still
        // allowed. Normally those reasons are due to users settings.
        /** Data is disabled by the user or policy. */
        DATA_DISABLED(false),
        /** Data roaming is disabled by the user. */
        ROAMING_DISABLED(false),
        /** Default data not selected. */
        DEFAULT_DATA_UNSELECTED(false),

        // Belows are all hard failure reasons. A hard reason means no matter what the data should
        // not be allowed.
        /** Data registration state is not in service. */
        NOT_IN_SERVICE(true),
        /** Data config is not ready. */
        DATA_CONFIG_NOT_READY(true),
        /** SIM is not ready. */
        SIM_NOT_READY(true),
        /** Concurrent voice and data is not allowed. */
        CONCURRENT_VOICE_DATA_NOT_ALLOWED(true),
        /** Service option not supported. */
        SERVICE_OPTION_NOT_SUPPORTED(true),
        /** Carrier notified data should be restricted. */
        DATA_RESTRICTED_BY_NETWORK(true),
        /** Radio power is off (i.e. airplane mode on) */
        RADIO_POWER_OFF(true),
        /** Data setup now allowed due to pending tear down all networks. */
        PENDING_TEAR_DOWN_ALL(true),
        /** Airplane mode is forcibly turned on by the carrier. */
        RADIO_DISABLED_BY_CARRIER(true),
        /** Underlying data service is not bound. */
        DATA_SERVICE_NOT_READY(true),
        /** Unable to find a suitable data profile. */
        NO_SUITABLE_DATA_PROFILE(true),
        /** Current data network type not allowed. */
        DATA_NETWORK_TYPE_NOT_ALLOWED(true),
        /** Device is currently in CDMA ECBM. */
        CDMA_EMERGENCY_CALLBACK_MODE(true),
        /** There is already a retry setup/handover scheduled. */
        RETRY_SCHEDULED(true),
        /** Network has explicitly request to throttle setup attempt. */
        DATA_THROTTLED(true),
        /** Data profile becomes invalid. (could be removed by the user, or SIM refresh, etc..) */
        DATA_PROFILE_INVALID(true),
        /** Data profile not preferred (i.e. users switch preferred profile in APN editor.) */
        DATA_PROFILE_NOT_PREFERRED(true),
        /** Handover is not allowed by policy. */
        NOT_ALLOWED_BY_POLICY(true),
        /** Data network is not in the right state. */
        ILLEGAL_STATE(true),
        /** VoPS is not supported by the network. */
        VOPS_NOT_SUPPORTED(true),
        /** Only one data network is allowed at one time. */
        ONLY_ALLOWED_SINGLE_NETWORK(true),
        /** Data enabled settings are not ready. */
        DATA_SETTINGS_NOT_READY(true),
        /** Handover max retry stopped but network is not on the preferred transport. */
        HANDOVER_RETRY_STOPPED(true),
        /** BootStrap sim data limit reached. */
        DATA_LIMIT_REACHED(true),
        /** Data network connectivity transport not allowed. */
        DATA_NETWORK_TRANSPORT_NOT_ALLOWED(true);

        private final boolean mIsHardReason;

        /**
         * @return {@code true} if the disallowed reason is a hard reason.
         */
        public boolean isHardReason() {
            return mIsHardReason;
        }

        /**
         * Constructor
         *
         * @param isHardReason {@code true} if the disallowed reason is a hard reason. A hard reason
         * means no matter what the data should not be allowed. A soft reason means that in certain
         * conditions, data is still allowed.
         */
        DataDisallowedReason(boolean isHardReason) {
            mIsHardReason = isHardReason;
        }
    }

    /**
     * Data allowed reasons. There will be only one reason if data is allowed.
     */
    public enum DataAllowedReason {
        // Note that unlike disallowed reasons, we only have one allowed reason every time
        // when we check data is allowed or not. The order of these allowed reasons is very
        // important. The lower ones take precedence over the upper ones.
        /**
         * None. This is the initial value.
         */
        NONE,
        /**
         * The normal reason. This is the most common case.
         */
        NORMAL,
        /**
         * Data is allowed because an ongoing VoPS call depends on this network
         */
        IN_VOICE_CALL,
        /**
         * The network brought up by this network request is unmetered. Should allowed no matter
         * the user enables or disables data.
         */
        UNMETERED_USAGE,
        /**
         * The network request supports MMS and MMS is always allowed.
         */
        MMS_REQUEST,
        /**
         * The network request is restricted (i.e. Only privilege apps can access the network.)
         */
        RESTRICTED_REQUEST,
        /**
         * SUPL is allowed while emergency call is ongoing.
         */
        EMERGENCY_SUPL,
        /**
         * Data is allowed because the network request is for emergency. This should be always at
         * the bottom (i.e. highest priority)
         */
        EMERGENCY_REQUEST,
    }

    @Override
    public String toString() {
        StringBuilder evaluationStr = new StringBuilder();
        evaluationStr.append("Data evaluation: evaluation reason:")
                .append(mDataEvaluationReason).append(", ");
        if (!mDataDisallowedReasons.isEmpty()) {
            evaluationStr.append("Data disallowed reasons:");
            for (DataDisallowedReason reason : mDataDisallowedReasons) {
                evaluationStr.append(" ").append(reason);
            }
        } else {
            evaluationStr.append("Data allowed reason:");
            evaluationStr.append(" ").append(mDataAllowedReason);
        }
        evaluationStr.append(", candidate profile=").append(mCandidateDataProfile);
        evaluationStr.append(", time=").append(DataUtils.systemTimeToString(mEvaluatedTime));
        return evaluationStr.toString();
    }

}
