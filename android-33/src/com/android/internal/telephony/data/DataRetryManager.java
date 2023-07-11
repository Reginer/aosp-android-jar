/*
 * Copyright 2021 The Android Open Source Project
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

import android.annotation.CallbackExecutor;
import android.annotation.ElapsedRealtimeLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkCapabilities;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.DataFailureCause;
import android.telephony.Annotation.NetCapability;
import android.telephony.AnomalyReporter;
import android.telephony.DataFailCause;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.ThrottleStatus;
import android.telephony.data.ThrottleStatus.RetryType;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.DataNetworkController.DataNetworkControllerCallback;
import com.android.internal.telephony.data.DataNetworkController.NetworkRequestList;
import com.android.internal.telephony.data.DataProfileManager.DataProfileManagerCallback;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DataRetryManager manages data network setup retry and its configurations.
 */
public class DataRetryManager extends Handler {
    private static final boolean VDBG = false;

    /** Event for data config updated. */
    private static final int EVENT_DATA_CONFIG_UPDATED = 1;

    /** Event for data setup retry. */
    private static final int EVENT_DATA_SETUP_RETRY = 3;

    /** Event for data handover retry. */
    private static final int EVENT_DATA_HANDOVER_RETRY = 4;

    /** Event for data profile/apn unthrottled. */
    private static final int EVENT_DATA_PROFILE_UNTHROTTLED = 6;

    /** Event for cancelling pending handover retry. */
    private static final int EVENT_CANCEL_PENDING_HANDOVER_RETRY = 7;

    /**
     * Event for radio on. This can happen when airplane mode is turned off, or RIL crashes and came
     * back online.
     */
    private static final int EVENT_RADIO_ON = 8;

    /** Event for modem reset. */
    private static final int EVENT_MODEM_RESET = 9;

    /** Event for tracking area code change. */
    private static final int EVENT_TAC_CHANGED = 10;

    /** The maximum entries to preserve. */
    private static final int MAXIMUM_HISTORICAL_ENTRIES = 100;

    @IntDef(prefix = {"RESET_REASON_"},
            value = {
                    RESET_REASON_DATA_PROFILES_CHANGED,
                    RESET_REASON_RADIO_ON,
                    RESET_REASON_MODEM_RESTART,
                    RESET_REASON_DATA_SERVICE_BOUND,
                    RESET_REASON_DATA_CONFIG_CHANGED,
                    RESET_REASON_TAC_CHANGED,
            })
    public @interface RetryResetReason {}

    /** Reset due to data profiles changed. */
    private static final int RESET_REASON_DATA_PROFILES_CHANGED = 1;

    /** Reset due to radio on. This could happen after airplane mode off or RIL restarted. */
    private static final int RESET_REASON_RADIO_ON = 2;

    /** Reset due to modem restarted. */
    private static final int RESET_REASON_MODEM_RESTART = 3;

    /**
     * Reset due to data service bound. This could happen when reboot or when data service crashed
     * and rebound.
     */
    private static final int RESET_REASON_DATA_SERVICE_BOUND = 4;

    /** Reset due to data config changed. */
    private static final int RESET_REASON_DATA_CONFIG_CHANGED = 5;

    /** Reset due to tracking area code changed. */
    private static final int RESET_REASON_TAC_CHANGED = 6;

    /** The phone instance. */
    private final @NonNull Phone mPhone;

    /** The RIL instance. */
    private final @NonNull CommandsInterface mRil;

    /** Logging tag. */
    private final @NonNull String mLogTag;

    /** Local log. */
    private final @NonNull LocalLog mLocalLog = new LocalLog(128);

    /**
     * The data retry callback. This is only used to notify {@link DataNetworkController} to retry
     * setup data network.
     */
    private @NonNull Set<DataRetryManagerCallback> mDataRetryManagerCallbacks = new ArraySet<>();

    /** Data service managers. */
    private @NonNull SparseArray<DataServiceManager> mDataServiceManagers;

    /** Data config manager instance. */
    private final @NonNull DataConfigManager mDataConfigManager;

    /** Data profile manager. */
    private final @NonNull DataProfileManager mDataProfileManager;

    /** Data setup retry rule list. */
    private @NonNull List<DataSetupRetryRule> mDataSetupRetryRuleList = new ArrayList<>();

    /** Data handover retry rule list. */
    private @NonNull List<DataHandoverRetryRule> mDataHandoverRetryRuleList = new ArrayList<>();

    /** Data retry entries. */
    private final @NonNull List<DataRetryEntry> mDataRetryEntries = new ArrayList<>();

    /**
     * Data throttling entries. Note this only stores throttling requested by networks. We intended
     * not to store frameworks-initiated throttling because they are not explicit/strong throttling
     * requests.
     */
    private final @NonNull List<DataThrottlingEntry> mDataThrottlingEntries = new ArrayList<>();

    /**
     * Represent a single data setup/handover throttling reported by networks.
     */
    public static class DataThrottlingEntry {
        /**
         * The data profile that is being throttled for setup/handover retry.
         */
        public final @NonNull DataProfile dataProfile;

        /**
         * The associated network request list when throttling happened. Should be {@code null} when
         * retry type is {@link ThrottleStatus#RETRY_TYPE_HANDOVER}.
         */
        public final @Nullable NetworkRequestList networkRequestList;

        /**
         * @param dataNetwork The data network that is being throttled for handover retry. Should be
         * {@code null} when retryType is {@link ThrottleStatus#RETRY_TYPE_NEW_CONNECTION}.
         */
        public final @Nullable DataNetwork dataNetwork;

        /** The transport that the data profile has been throttled on. */
        public final @TransportType int transport;

        /** The retry type when throttling expires. */
        public final @RetryType int retryType;

        /**
         * The expiration time of data throttling. This is the time retrieved from
         * {@link SystemClock#elapsedRealtime()}.
         */
        public final @ElapsedRealtimeLong long expirationTimeMillis;

        /**
         * Constructor.
         *
         * @param dataProfile The data profile that is being throttled for setup/handover retry.
         * @param networkRequestList The associated network request list when throttling happened.
         * Should be {@code null} when retry type is {@link ThrottleStatus#RETRY_TYPE_HANDOVER}.
         * @param dataNetwork The data network that is being throttled for handover retry.
         * Should be {@code null} when retryType is
         * {@link ThrottleStatus#RETRY_TYPE_NEW_CONNECTION}.
         * @param transport The transport that the data profile has been throttled on.
         * @param retryType The retry type when throttling expires.
         * @param expirationTimeMillis The expiration elapsed time of data throttling.
         */
        public DataThrottlingEntry(@NonNull DataProfile dataProfile,
                @Nullable NetworkRequestList networkRequestList,
                @Nullable DataNetwork dataNetwork, @TransportType int transport,
                @RetryType int retryType, @ElapsedRealtimeLong long expirationTimeMillis) {
            this.dataProfile = dataProfile;
            this.networkRequestList = networkRequestList;
            this.dataNetwork = dataNetwork;
            this.transport = transport;
            this.retryType = retryType;
            this.expirationTimeMillis = expirationTimeMillis;
        }

        @Override
        public @NonNull String toString() {
            return "[DataThrottlingEntry: dataProfile=" + dataProfile + ", request list="
                    + networkRequestList + ", dataNetwork=" + dataNetwork + ", transport="
                    + AccessNetworkConstants.transportTypeToString(transport) + ", expiration time="
                    + DataUtils.elapsedTimeToString(expirationTimeMillis) + "]";
        }
    }

    /**
     * Represent a data retry rule. A rule consists a retry type (e.g. either by capabilities,
     * fail cause, or both), and a retry interval.
     */
    public static class DataRetryRule {
        private static final String RULE_TAG_FAIL_CAUSES = "fail_causes";
        private static final String RULE_TAG_RETRY_INTERVAL = "retry_interval";
        private static final String RULE_TAG_MAXIMUM_RETRIES = "maximum_retries";

        /**
         * The data network setup retry interval. Note that if this is empty, then
         * {@link #getMaxRetries()} must return 0. Default retry interval is 5 seconds.
         */
        protected List<Long> mRetryIntervalsMillis = List.of(TimeUnit.SECONDS.toMillis(5));

        /**
         * The maximum retry times. After reaching the retry times, data retry will not be scheduled
         * with timer. Only environment changes (e.g. Airplane mode, SIM state, RAT, registration
         * state changes, etc..) can trigger the retry.
         */
        protected int mMaxRetries = 10;

        /**
         * The network capabilities. Each data setup must be
         * associated with at least one network request. If that network request contains network
         * capabilities specified here, then retry will happen. Empty set indicates the retry rule
         * is not using network capabilities.
         */
        protected @NonNull @NetCapability Set<Integer> mNetworkCapabilities = new ArraySet<>();

        /**
         * The fail causes. If data setup failed with certain fail causes, then retry will happen.
         * Empty set indicates the retry rule is not using the fail causes.
         */
        protected @NonNull @DataFailureCause Set<Integer> mFailCauses = new ArraySet<>();

        public DataRetryRule(@NonNull String ruleString) {
            if (TextUtils.isEmpty(ruleString)) {
                throw new IllegalArgumentException("illegal rule " + ruleString);
            }
            ruleString = ruleString.trim().toLowerCase(Locale.ROOT);
            String[] expressions = ruleString.split("\\s*,\\s*");
            for (String expression : expressions) {
                String[] tokens = expression.trim().split("\\s*=\\s*");
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("illegal rule " + ruleString);
                }
                String key = tokens[0].trim();
                String value = tokens[1].trim();
                try {
                    switch (key) {
                        case RULE_TAG_FAIL_CAUSES:
                            mFailCauses = Arrays.stream(value.split("\\s*\\|\\s*"))
                                    .map(String::trim)
                                    .map(Integer::valueOf)
                                    .collect(Collectors.toSet());
                            break;
                        case RULE_TAG_RETRY_INTERVAL:
                            mRetryIntervalsMillis = Arrays.stream(value.split("\\s*\\|\\s*"))
                                    .map(String::trim)
                                    .map(Long::valueOf)
                                    .collect(Collectors.toList());
                            break;
                        case RULE_TAG_MAXIMUM_RETRIES:
                            mMaxRetries = Integer.parseInt(value);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("illegal rule " + ruleString + ", e=" + e);
                }
            }

            if (mMaxRetries == 0) {
                mRetryIntervalsMillis = Collections.emptyList();
            }

            if (mMaxRetries < 0) {
                throw new IllegalArgumentException("Max retries should not be less than 0. "
                        + "mMaxRetries=" + mMaxRetries);
            }

            if (mRetryIntervalsMillis.stream().anyMatch(i -> i <= 0)) {
                throw new IllegalArgumentException("Retry interval should not be less than 0. "
                        + "mRetryIntervalsMillis=" + mRetryIntervalsMillis);
            }
        }

        /**
         * @return The data network setup retry intervals in milliseconds. If this is empty, then
         * {@link #getMaxRetries()} must return 0.
         */
        public @NonNull List<Long> getRetryIntervalsMillis() {
            return mRetryIntervalsMillis;
        }

        /**
         * @return The maximum retry times. After reaching the retry times, data retry will not be
         * scheduled with timer. Only environment changes (e.g. Airplane mode, SIM state, RAT,
         * registration state changes, etc..) can trigger the retry. Note that if max retries
         * is 0, then {@link #getRetryIntervalsMillis()} must be {@code null}.
         */
        public int getMaxRetries() {
            return mMaxRetries;
        }

        /**
         * @return The fail causes. If data setup failed with certain fail causes, then retry will
         * happen. Empty set indicates the retry rule is not using the fail causes.
         */
        @VisibleForTesting
        public @NonNull @DataFailureCause Set<Integer> getFailCauses() {
            return mFailCauses;
        }
    }

    /**
     * Represent a setup data network retry rule.
     *
     * The syntax of the retry rule:
     * 1. Retry based on {@link NetworkCapabilities}. Note that only APN-type network
     * capabilities are supported.
     * "capabilities=[netCaps1|netCaps2|...], [retry_interval=n1|n2|n3|n4...],
     * [maximum_retries=n]"
     *
     * 2. Retry based on {@link DataFailCause}
     * "fail_causes=[cause1|cause2|cause3|..], [retry_interval=n1|n2|n3|n4...],
     * [maximum_retries=n]"
     *
     * 3. Retry based on {@link NetworkCapabilities} and {@link DataFailCause}. Note that only
     *    APN-type network capabilities are supported.
     * "capabilities=[netCaps1|netCaps2|...], fail_causes=[cause1|cause2|cause3|...],
     *     [retry_interval=n1|n2|n3|n4...], [maximum_retries=n]"
     *
     * For example,
     * "capabilities=eims, retry_interval=1000, maximum_retries=20" means if the attached
     * network request is emergency, then retry data network setup every 1 second for up to 20
     * times.
     *
     * "fail_causes=8|27|28|29|30|32|33|35|50|51|111|-5|-6|65537|65538|-3|2253|2254
     * , maximum_retries=0" means for those fail causes, never retry with timers. Note that
     * when environment changes, retry can still happen.
     *
     * "capabilities=internet|enterprise|dun|ims|fota, retry_interval=2500|3000|"
     * "5000|10000|15000|20000|40000|60000|120000|240000|600000|1200000|1800000"
     * "1800000, maximum_retries=20" means for those capabilities, retry happens in 2.5s, 3s,
     * 5s, 10s, 15s, 20s, 40s, 1m, 2m, 4m, 10m, 20m, 30m, 30m, 30m, until reaching 20 retries.
     */
    public static class DataSetupRetryRule extends DataRetryRule {
        private static final String RULE_TAG_CAPABILITIES = "capabilities";
        /**
         * Constructor
         *
         * @param ruleString The retry rule in string format.
         * @throws IllegalArgumentException if the string can't be parsed to a retry rule.
         */
        public DataSetupRetryRule(@NonNull String ruleString) {
            super(ruleString);

            ruleString = ruleString.trim().toLowerCase(Locale.ROOT);
            String[] expressions = ruleString.split("\\s*,\\s*");
            for (String expression : expressions) {
                String[] tokens = expression.trim().split("\\s*=\\s*");
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("illegal rule " + ruleString);
                }
                String key = tokens[0].trim();
                String value = tokens[1].trim();
                if (key.equals(RULE_TAG_CAPABILITIES)) {
                    mNetworkCapabilities = DataUtils.getNetworkCapabilitiesFromString(value);
                }
            }

            if (mFailCauses.isEmpty() && (mNetworkCapabilities.isEmpty()
                    || mNetworkCapabilities.contains(-1))) {
                throw new IllegalArgumentException("illegal rule " + ruleString
                            + ". Should have either valid network capabilities or fail causes.");
            }
        }

        /**
         * @return The network capabilities. Each data setup must be associated with at least one
         * network request. If that network request contains network capabilities specified here,
         * then retry will happen. Empty set indicates the retry rule is not using network
         * capabilities.
         */
        @VisibleForTesting
        public @NonNull @NetCapability Set<Integer> getNetworkCapabilities() {
            return mNetworkCapabilities;
        }

        /**
         * Check if this rule can be matched.
         *
         * @param networkCapability The network capability for matching.
         * @param cause Fail cause from previous setup data request.
         * @return {@code true} if the retry rule can be matched.
         */
        public boolean canBeMatched(@NonNull @NetCapability int networkCapability,
                @DataFailureCause int cause) {
            if (!mFailCauses.isEmpty() && !mFailCauses.contains(cause)) {
                return false;
            }

            return mNetworkCapabilities.isEmpty()
                    || mNetworkCapabilities.contains(networkCapability);
        }

        @Override
        public String toString() {
            return "[DataSetupRetryRule: Network capabilities:"
                    + DataUtils.networkCapabilitiesToString(mNetworkCapabilities.stream()
                    .mapToInt(Number::intValue).toArray())
                    + ", Fail causes=" + mFailCauses
                    + ", Retry intervals=" + mRetryIntervalsMillis + ", Maximum retries="
                    + mMaxRetries + "]";
        }
    }

    /**
     * Represent a handover data network retry rule.
     *
     * The syntax of the retry rule:
     * 1. Retry when handover fails.
     * "retry_interval=[n1|n2|n3|...], [maximum_retries=n]"
     *
     * For example,
     * "retry_interval=1000|3000|5000, maximum_retries=10" means handover retry will happen in 1s,
     * 3s, 5s, 5s, 5s....up to 10 times.
     *
     * 2. Retry when handover fails with certain fail causes.
     * "retry_interval=[n1|n2|n3|...], fail_causes=[cause1|cause2|cause3|...], [maximum_retries=n]
     *
     * For example,
     * "retry_interval=1000, maximum_retries=3, fail_causes=5" means handover retry every 1 second
     * for up to 3 times when handover fails with the cause 5.
     *
     * "maximum_retries=0, fail_causes=6|10|67" means handover retry should not happen for those
     * causes.
     */
    public static class DataHandoverRetryRule extends DataRetryRule {
        /**
         * Constructor
         *
         * @param ruleString The retry rule in string format.
         * @throws IllegalArgumentException if the string can't be parsed to a retry rule.
         */
        public DataHandoverRetryRule(@NonNull String ruleString) {
            super(ruleString);
        }

        @Override
        public String toString() {
            return "[DataHandoverRetryRule: Retry intervals=" + mRetryIntervalsMillis
                    + ", Fail causes=" + mFailCauses + ", Maximum retries=" + mMaxRetries + "]";
        }
    }

    /**
     * Represent a data retry entry.
     */
    public static class DataRetryEntry {
        /** Indicates the retry is not happened yet. */
        public static final int RETRY_STATE_NOT_RETRIED = 1;

        /** Indicates the retry happened, but still failed to setup/handover the data network. */
        public static final int RETRY_STATE_FAILED = 2;

        /** Indicates the retry happened and succeeded. */
        public static final int RETRY_STATE_SUCCEEDED = 3;

        /** Indicates the retry was cancelled. */
        public static final int RETRY_STATE_CANCELLED = 4;

        @IntDef(prefix = {"RETRY_STATE_"},
                value = {
                        RETRY_STATE_NOT_RETRIED,
                        RETRY_STATE_FAILED,
                        RETRY_STATE_SUCCEEDED,
                        RETRY_STATE_CANCELLED
                })
        public @interface DataRetryState {}

        /** The rule used for this data retry. {@code null} if the retry is requested by network. */
        public final @Nullable DataRetryRule appliedDataRetryRule;

        /** The retry delay in milliseconds. */
        public final long retryDelayMillis;

        /**
         * Retry elapsed time. This is the system elapsed time retrieved from
         * {@link SystemClock#elapsedRealtime()}.
         */
        public final @ElapsedRealtimeLong long retryElapsedTime;

        /** The retry state. */
        protected int mRetryState = RETRY_STATE_NOT_RETRIED;

        /** Timestamp when a state is set. For debugging purposes only. */
        protected @ElapsedRealtimeLong long mRetryStateTimestamp = 0;

        /**
         * Constructor
         *
         * @param appliedDataRetryRule The applied data retry rule.
         * @param retryDelayMillis The retry delay in milliseconds.
         */
        public DataRetryEntry(@Nullable DataRetryRule appliedDataRetryRule, long retryDelayMillis) {
            this.appliedDataRetryRule = appliedDataRetryRule;
            this.retryDelayMillis = retryDelayMillis;

            mRetryStateTimestamp = SystemClock.elapsedRealtime();
            retryElapsedTime =  mRetryStateTimestamp + retryDelayMillis;
        }

        /**
         * Set the state of a data retry.
         *
         * @param state The retry state.
         */
        public void setState(@DataRetryState int state) {
            mRetryState = state;
            mRetryStateTimestamp = SystemClock.elapsedRealtime();
        }

        /**
         * @return Get the retry state.
         */
        public @DataRetryState int getState() {
            return mRetryState;
        }

        /**
         * Convert retry state to string.
         *
         * @param retryState Retry state.
         * @return Retry state in string format.
         */
        public static String retryStateToString(@DataRetryState int retryState) {
            switch (retryState) {
                case RETRY_STATE_NOT_RETRIED: return "NOT_RETRIED";
                case RETRY_STATE_FAILED: return "FAILED";
                case RETRY_STATE_SUCCEEDED: return "SUCCEEDED";
                case RETRY_STATE_CANCELLED: return "CANCELLED";
                default: return "Unknown(" + retryState + ")";
            }
        }

        /**
         * The generic builder for retry entry.
         *
         * @param <T> The type of extended retry entry builder.
         */
        public static class Builder<T extends Builder<T>> {
            /**
             * The retry delay in milliseconds. Default is 5 seconds.
             */
            protected long mRetryDelayMillis = TimeUnit.SECONDS.toMillis(5);

            /** The applied data retry rule. */
            protected @Nullable DataRetryRule mAppliedDataRetryRule;

            /**
             * Set the data retry delay.
             *
             * @param retryDelayMillis The retry delay in milliseconds.
             * @return This builder.
             */
            public @NonNull T setRetryDelay(long retryDelayMillis) {
                mRetryDelayMillis = retryDelayMillis;
                return (T) this;
            }

            /**
             * Set the applied retry rule.
             *
             * @param dataRetryRule The rule that used for this data retry.
             * @return This builder.
             */
            public @NonNull T setAppliedRetryRule(@NonNull DataRetryRule dataRetryRule) {
                mAppliedDataRetryRule = dataRetryRule;
                return (T) this;
            }
        }
    }

    /**
     * Represent a setup data retry entry.
     */
    public static class DataSetupRetryEntry extends DataRetryEntry {
        /**
         * Retry type is unknown. This should be only used for initialized value.
         */
        public static final int RETRY_TYPE_UNKNOWN = 0;
        /**
         * To retry setup data with the same data profile.
         */
        public static final int RETRY_TYPE_DATA_PROFILE = 1;

        /**
         * To retry satisfying the network request(s). Could be using a
         * different data profile.
         */
        public static final int RETRY_TYPE_NETWORK_REQUESTS = 2;

        @IntDef(prefix = {"RETRY_TYPE_"},
                value = {
                        RETRY_TYPE_UNKNOWN,
                        RETRY_TYPE_DATA_PROFILE,
                        RETRY_TYPE_NETWORK_REQUESTS,
                })
        public @interface SetupRetryType {}

        /** Setup retry type. Could be retry by same data profile or same capability. */
        public final @SetupRetryType int setupRetryType;

        /** The network requests to satisfy when retry happens. */
        public final @NonNull NetworkRequestList networkRequestList;

        /** The data profile that will be used for retry. */
        public final @Nullable DataProfile dataProfile;

        /** The transport to retry data setup. */
        public final @TransportType int transport;

        /**
         * Constructor
         *
         * @param setupRetryType Data retry type. Could be retry by same data profile or same
         * capabilities.
         * @param networkRequestList The network requests to satisfy when retry happens.
         * @param dataProfile The data profile that will be used for retry.
         * @param transport The transport to retry data setup.
         * @param appliedDataSetupRetryRule The applied data setup retry rule.
         * @param retryDelayMillis The retry delay in milliseconds.
         */
        private DataSetupRetryEntry(@SetupRetryType int setupRetryType,
                @Nullable NetworkRequestList networkRequestList, @NonNull DataProfile dataProfile,
                @TransportType int transport,
                @Nullable DataSetupRetryRule appliedDataSetupRetryRule, long retryDelayMillis) {
            super(appliedDataSetupRetryRule, retryDelayMillis);
            this.setupRetryType = setupRetryType;
            this.networkRequestList = networkRequestList;
            this.dataProfile = dataProfile;
            this.transport = transport;
        }

        /**
         * Convert retry type to string.
         *
         * @param setupRetryType Data setup retry type.
         * @return Retry type in string format.
         */
        private static String retryTypeToString(@SetupRetryType int setupRetryType) {
            switch (setupRetryType) {
                case RETRY_TYPE_DATA_PROFILE: return "BY_PROFILE";
                case RETRY_TYPE_NETWORK_REQUESTS: return "BY_NETWORK_REQUESTS";
                default: return "Unknown(" + setupRetryType + ")";
            }
        }

        @Override
        public String toString() {
            return "[DataSetupRetryEntry: delay=" + retryDelayMillis + "ms, retry time:"
                    + DataUtils.elapsedTimeToString(retryElapsedTime) + ", " + dataProfile
                    + ", transport=" + AccessNetworkConstants.transportTypeToString(transport)
                    + ", retry type=" + retryTypeToString(setupRetryType) + ", retry requests="
                    + networkRequestList + ", applied rule=" + appliedDataRetryRule + ", state="
                    + retryStateToString(mRetryState) + ", timestamp="
                    + DataUtils.elapsedTimeToString(mRetryStateTimestamp) + "]";
        }

        /**
         * The builder of {@link DataSetupRetryEntry}.
         *
         * @param <T> Type of the builder.
         */
        public static class Builder<T extends Builder<T>> extends DataRetryEntry.Builder<T> {
            /** Data setup retry type. Could be retry by same data profile or same capabilities. */
            private @SetupRetryType int mSetupRetryType = RETRY_TYPE_UNKNOWN;

            /** The network requests to satisfy when retry happens. */
            private @NonNull NetworkRequestList mNetworkRequestList;

            /** The data profile that will be used for retry. */
            private @Nullable DataProfile mDataProfile;

            /** The transport to retry data setup. */
            private @TransportType int mTransport = AccessNetworkConstants.TRANSPORT_TYPE_INVALID;

            /**
             * Set the data retry type.
             *
             * @param setupRetryType Data retry type. Could be retry by same data profile or same
             * capabilities.
             * @return This builder.
             */
            public @NonNull Builder<T> setSetupRetryType(@SetupRetryType int setupRetryType) {
                mSetupRetryType = setupRetryType;
                return this;
            }

            /**
             * Set the network capability to satisfy when retry happens.
             *
             * @param networkRequestList The network requests to satisfy when retry happens.
             * @return This builder.
             */
            public @NonNull Builder<T> setNetworkRequestList(
                    @NonNull NetworkRequestList networkRequestList) {
                mNetworkRequestList = networkRequestList;
                return this;
            }

            /**
             * Set the data profile that will be used for retry.
             *
             * @param dataProfile The data profile that will be used for retry.
             * @return This builder.
             */
            public @NonNull Builder<T> setDataProfile(@NonNull DataProfile dataProfile) {
                mDataProfile = dataProfile;
                return this;
            }

            /**
             * Set the transport of the data setup retry.
             *
             * @param transport The transport to retry data setup.
             * @return This builder.
             */
            public @NonNull Builder<T> setTransport(@TransportType int transport) {
                mTransport = transport;
                return this;
            }

            /**
             * Build the instance of {@link DataSetupRetryEntry}.
             *
             * @return The instance of {@link DataSetupRetryEntry}.
             */
            public @NonNull DataSetupRetryEntry build() {
                if (mNetworkRequestList == null) {
                    throw new IllegalArgumentException("network request list is not specified.");
                }
                if (mTransport != AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                        && mTransport != AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                    throw new IllegalArgumentException("Invalid transport type " + mTransport);
                }
                if (mSetupRetryType != RETRY_TYPE_DATA_PROFILE
                        && mSetupRetryType != RETRY_TYPE_NETWORK_REQUESTS) {
                    throw new IllegalArgumentException("Invalid setup retry type "
                            + mSetupRetryType);
                }
                return new DataSetupRetryEntry(mSetupRetryType, mNetworkRequestList, mDataProfile,
                        mTransport, (DataSetupRetryRule) mAppliedDataRetryRule, mRetryDelayMillis);
            }
        }
    }

    /**
     * Represent a data handover retry entry.
     */
    public static class DataHandoverRetryEntry extends DataRetryEntry {
        /** The data network to be retried for handover. */
        public final @NonNull DataNetwork dataNetwork;

        /**
         * Constructor.
         *
         * @param dataNetwork The data network to be retried for handover.
         * @param appliedDataHandoverRetryRule The applied data retry rule.
         * @param retryDelayMillis The retry delay in milliseconds.
         */
        public DataHandoverRetryEntry(@NonNull DataNetwork dataNetwork,
                @Nullable DataHandoverRetryRule appliedDataHandoverRetryRule,
                long retryDelayMillis) {
            super(appliedDataHandoverRetryRule, retryDelayMillis);
            this.dataNetwork = dataNetwork;
        }

        @Override
        public String toString() {
            return "[DataHandoverRetryEntry: delay=" + retryDelayMillis + "ms, retry time:"
                    + DataUtils.elapsedTimeToString(retryElapsedTime) + ", " + dataNetwork
                     + ", applied rule=" + appliedDataRetryRule + ", state="
                    + retryStateToString(mRetryState) + ", timestamp="
                    + DataUtils.elapsedTimeToString(mRetryStateTimestamp) + "]";
        }

        /**
         * The builder of {@link DataHandoverRetryEntry}.
         *
         * @param <T> Type of the builder.
         */
        public static class Builder<T extends Builder<T>> extends DataRetryEntry.Builder<T> {
            /** The data network to be retried for handover. */
            public @NonNull DataNetwork mDataNetwork;

            /**
             * Set the data retry type.
             *
             * @param dataNetwork The data network to be retried for handover.
             *
             * @return This builder.
             */
            public @NonNull Builder<T> setDataNetwork(@NonNull DataNetwork dataNetwork) {
                mDataNetwork = dataNetwork;
                return this;
            }

            /**
             * Build the instance of {@link DataHandoverRetryEntry}.
             *
             * @return The instance of {@link DataHandoverRetryEntry}.
             */
            public @NonNull DataHandoverRetryEntry build() {
                return new DataHandoverRetryEntry(mDataNetwork,
                        (DataHandoverRetryRule) mAppliedDataRetryRule, mRetryDelayMillis);
            }
        }
    }

    /** Data retry callback. */
    public static class DataRetryManagerCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public DataRetryManagerCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when data setup retry occurs.
         *
         * @param dataSetupRetryEntry The data setup retry entry.
         */
        public void onDataNetworkSetupRetry(@NonNull DataSetupRetryEntry dataSetupRetryEntry) {}

        /**
         * Called when data handover retry occurs.
         *
         * @param dataHandoverRetryEntry The data handover retry entry.
         */
        public void onDataNetworkHandoverRetry(
                @NonNull DataHandoverRetryEntry dataHandoverRetryEntry) {}

        /**
         * Called when retry manager determines that the retry will no longer be performed on
         * this data network.
         *
         * @param dataNetwork The data network that will never be retried handover.
         */
        public void onDataNetworkHandoverRetryStopped(@NonNull DataNetwork dataNetwork) {}

        /**
         * Called when throttle status changed reported from network.
         *
         * @param throttleStatusList List of throttle status.
         */
        public void onThrottleStatusChanged(@NonNull List<ThrottleStatus> throttleStatusList) {}
    }

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param dataRetryManagerCallback Data retry callback.
     */
    public DataRetryManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull SparseArray<DataServiceManager> dataServiceManagers,
            @NonNull Looper looper, @NonNull DataRetryManagerCallback dataRetryManagerCallback) {
        super(looper);
        mPhone = phone;
        mRil = phone.mCi;
        mLogTag = "DRM-" + mPhone.getPhoneId();
        mDataRetryManagerCallbacks.add(dataRetryManagerCallback);

        mDataServiceManagers = dataServiceManagers;
        mDataConfigManager = dataNetworkController.getDataConfigManager();
        mDataProfileManager = dataNetworkController.getDataProfileManager();
        mDataConfigManager.registerForConfigUpdate(this, EVENT_DATA_CONFIG_UPDATED);

        mDataServiceManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .registerForApnUnthrottled(this, EVENT_DATA_PROFILE_UNTHROTTLED);
        if (!mPhone.getAccessNetworksManager().isInLegacyMode()) {
            mDataServiceManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                    .registerForApnUnthrottled(this, EVENT_DATA_PROFILE_UNTHROTTLED);
        }
        mDataProfileManager.registerCallback(new DataProfileManagerCallback(this::post) {
            @Override
            public void onDataProfilesChanged() {
                onReset(RESET_REASON_DATA_PROFILES_CHANGED);
            }
        });
        dataNetworkController.registerDataNetworkControllerCallback(
                new DataNetworkControllerCallback(this::post) {
                    @Override
                    public void onDataServiceBound(@TransportType int transport) {
                        onReset(RESET_REASON_DATA_SERVICE_BOUND);
                    }
                });
        mRil.registerForOn(this, EVENT_RADIO_ON, null);
        mRil.registerForModemReset(this, EVENT_MODEM_RESET, null);

        if (mDataConfigManager.shouldResetDataThrottlingWhenTacChanges()) {
            mPhone.getServiceStateTracker().registerForAreaCodeChanged(this, EVENT_TAC_CHANGED,
                    null);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        switch (msg.what) {
            case EVENT_DATA_CONFIG_UPDATED:
                onDataConfigUpdated();
                break;
            case EVENT_DATA_SETUP_RETRY:
                DataSetupRetryEntry dataSetupRetryEntry = (DataSetupRetryEntry) msg.obj;
                Objects.requireNonNull(dataSetupRetryEntry);
                mDataRetryManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                        () -> callback.onDataNetworkSetupRetry(dataSetupRetryEntry)));
                break;
            case EVENT_DATA_HANDOVER_RETRY:
                DataHandoverRetryEntry dataHandoverRetryEntry = (DataHandoverRetryEntry) msg.obj;
                Objects.requireNonNull(dataHandoverRetryEntry);
                if (mDataRetryEntries.contains(dataHandoverRetryEntry)) {
                    mDataRetryManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                            () -> callback.onDataNetworkHandoverRetry(dataHandoverRetryEntry)));
                } else {
                    log("Handover was cancelled earlier. " + dataHandoverRetryEntry);
                }
                break;
            case EVENT_RADIO_ON:
                onReset(RESET_REASON_RADIO_ON);
                break;
            case EVENT_MODEM_RESET:
                onReset(RESET_REASON_MODEM_RESTART);
                break;
            case EVENT_TAC_CHANGED:
                onReset(RESET_REASON_TAC_CHANGED);
                break;
            case EVENT_DATA_PROFILE_UNTHROTTLED:
                ar = (AsyncResult) msg.obj;
                int transport = (int) ar.userObj;
                String apn = null;
                DataProfile dataProfile = null;
                // 1.6 or older HAL
                if (ar.result instanceof String) {
                    apn = (String) ar.result;
                } else if (ar.result instanceof DataProfile) {
                    dataProfile = (DataProfile) ar.result;
                }
                onDataProfileUnthrottled(dataProfile, apn, transport, true);
                break;
            case EVENT_CANCEL_PENDING_HANDOVER_RETRY:
                onCancelPendingHandoverRetry((DataNetwork) msg.obj);
                break;
            default:
                loge("Unexpected message " + msg.what);
        }
    }

    /**
     * Called when data config is updated.
     */
    private void onDataConfigUpdated() {
        onReset(RESET_REASON_DATA_CONFIG_CHANGED);
        mDataSetupRetryRuleList = mDataConfigManager.getDataSetupRetryRules();
        mDataHandoverRetryRuleList = mDataConfigManager.getDataHandoverRetryRules();
        log("onDataConfigUpdated: mDataSetupRetryRuleList=" + mDataSetupRetryRuleList
                + ", mDataHandoverRetryRuleList=" + mDataHandoverRetryRuleList);
    }

    /**
     * Evaluate if data setup retry is needed or not. If needed, retry will be scheduled
     * automatically after evaluation.
     *
     * @param dataProfile The data profile that has been used in the previous data network setup.
     * @param transport The transport to retry data setup.
     * @param requestList The network requests attached to the previous data network setup.
     * @param cause The fail cause of previous data network setup.
     * @param retryDelayMillis The retry delay in milliseconds suggested by the network/data
     * service. {@link android.telephony.data.DataCallResponse#RETRY_DURATION_UNDEFINED}
     * indicates network/data service did not suggest retry or not. Telephony frameworks would use
     * its logic to perform data retry.
     */
    public void evaluateDataSetupRetry(@NonNull DataProfile dataProfile,
            @TransportType int transport, @NonNull NetworkRequestList requestList,
            @DataFailureCause int cause, long retryDelayMillis) {
        post(() -> onEvaluateDataSetupRetry(dataProfile, transport, requestList, cause,
                retryDelayMillis));
    }

    private void onEvaluateDataSetupRetry(@NonNull DataProfile dataProfile,
            @TransportType int transport, @NonNull NetworkRequestList requestList,
            @DataFailureCause int cause, long retryDelayMillis) {
        logl("onEvaluateDataSetupRetry: " + dataProfile + ", transport="
                + AccessNetworkConstants.transportTypeToString(transport) + ", cause="
                + DataFailCause.toString(cause) + ", retryDelayMillis=" + retryDelayMillis + "ms"
                + ", " + requestList);
        // Check if network suggested never retry for this data profile. Note that for HAL 1.5
        // and older, Integer.MAX_VALUE indicates never retry. For 1.6 or above, Long.MAX_VALUE
        // indicates never retry.
        if (retryDelayMillis == Long.MAX_VALUE || retryDelayMillis == Integer.MAX_VALUE) {
            logl("Network suggested never retry for " + dataProfile);
            // Note that RETRY_TYPE_NEW_CONNECTION is intended here because
            // when unthrottling happens, we still want to retry and we'll need
            // a type there so we know what to retry. Using RETRY_TYPE_NONE
            // ThrottleStatus is just for API backwards compatibility reason.
            updateThrottleStatus(dataProfile, requestList, null,
                    ThrottleStatus.RETRY_TYPE_NEW_CONNECTION, transport, Long.MAX_VALUE);
        } else if (retryDelayMillis != DataCallResponse.RETRY_DURATION_UNDEFINED) {
            // Network specifically asks retry the previous data profile again.
            DataSetupRetryEntry dataSetupRetryEntry = new DataSetupRetryEntry.Builder<>()
                    .setRetryDelay(retryDelayMillis)
                    .setSetupRetryType(DataSetupRetryEntry.RETRY_TYPE_DATA_PROFILE)
                    .setNetworkRequestList(requestList)
                    .setDataProfile(dataProfile)
                    .setTransport(transport)
                    .build();
            updateThrottleStatus(dataProfile, requestList, null,
                    ThrottleStatus.RETRY_TYPE_NEW_CONNECTION, transport,
                    dataSetupRetryEntry.retryElapsedTime);
            schedule(dataSetupRetryEntry);
        } else {
            // Network did not suggest any retry. Use the configured rules to perform retry.
            logv("mDataSetupRetryRuleList=" + mDataSetupRetryRuleList);

            // Support the legacy permanent failure configuration
            if (DataFailCause.isPermanentFailure(mPhone.getContext(), cause, mPhone.getSubId())) {
                log("Stopped timer-based retry. cause=" + DataFailCause.toString(cause));
                return;
            }

            boolean retryScheduled = false;
            List<NetworkRequestList> groupedNetworkRequestLists =
                    DataUtils.getGroupedNetworkRequestList(requestList);
            for (NetworkRequestList networkRequestList : groupedNetworkRequestLists) {
                int capability = networkRequestList.get(0).getApnTypeNetworkCapability();

                for (DataSetupRetryRule retryRule : mDataSetupRetryRuleList) {
                    if (retryRule.canBeMatched(capability, cause)) {
                        // Check if there is already a similar network request retry scheduled.
                        if (isSimilarNetworkRequestRetryScheduled(
                                networkRequestList.get(0), transport)) {
                            log(networkRequestList.get(0) + " already had similar retry "
                                    + "scheduled.");
                            return;
                        }

                        int failedCount = getRetryFailedCount(capability, retryRule);
                        log("For capability " + DataUtils.networkCapabilityToString(capability)
                                + ", found matching rule " + retryRule + ", failed count="
                                + failedCount);
                        if (failedCount == retryRule.getMaxRetries()) {
                            log("Data retry failed for " + failedCount + " times. Stopped "
                                    + "timer-based data retry for "
                                    + DataUtils.networkCapabilityToString(capability)
                                    + ". Condition-based retry will still happen when condition "
                                    + "changes.");
                            return;
                        }

                        retryDelayMillis = retryRule.getRetryIntervalsMillis().get(
                                Math.min(failedCount, retryRule
                                        .getRetryIntervalsMillis().size() - 1));

                        // Schedule a data retry.
                        schedule(new DataSetupRetryEntry.Builder<>()
                                .setRetryDelay(retryDelayMillis)
                                .setAppliedRetryRule(retryRule)
                                .setSetupRetryType(DataSetupRetryEntry.RETRY_TYPE_NETWORK_REQUESTS)
                                .setTransport(transport)
                                .setNetworkRequestList(networkRequestList)
                                .build());
                        retryScheduled = true;
                        break;
                    }
                }
            }

            if (!retryScheduled) {
                log("onEvaluateDataSetupRetry: Did not match any retry rule. Stop timer-based "
                        + "retry.");
            }
        }
    }

    /**
     * Evaluate if data handover retry is needed or not. If needed, retry will be scheduled
     * automatically after evaluation.
     *
     * @param dataNetwork The data network to be retried for handover.
     * @param cause The fail cause of previous data network handover.
     * @param retryDelayMillis The retry delay in milliseconds suggested by the network/data
     * service. {@link android.telephony.data.DataCallResponse#RETRY_DURATION_UNDEFINED}
     * indicates network/data service did not suggest retry or not. Telephony frameworks would use
     * its logic to perform handover retry.
     */
    public void evaluateDataHandoverRetry(@NonNull DataNetwork dataNetwork,
            @DataFailureCause int cause, long retryDelayMillis) {
        post(() -> onEvaluateDataHandoverRetry(dataNetwork, cause, retryDelayMillis));
    }

    private void onEvaluateDataHandoverRetry(@NonNull DataNetwork dataNetwork,
            @DataFailureCause int cause, long retryDelayMillis) {
        logl("onEvaluateDataHandoverRetry: " + dataNetwork + ", cause="
                + DataFailCause.toString(cause) + ", retryDelayMillis=" + retryDelayMillis + "ms");
        int targetTransport = DataUtils.getTargetTransport(dataNetwork.getTransport());
        if (retryDelayMillis == Long.MAX_VALUE || retryDelayMillis == Integer.MAX_VALUE) {
            logl("Network suggested never retry handover for " + dataNetwork);
            // Note that RETRY_TYPE_HANDOVER is intended here because
            // when unthrottling happens, we still want to retry and we'll need
            // a type there so we know what to retry. Using RETRY_TYPE_NONE
            // ThrottleStatus is just for API backwards compatibility reason.
            updateThrottleStatus(dataNetwork.getDataProfile(),
                    dataNetwork.getAttachedNetworkRequestList(), dataNetwork,
                    ThrottleStatus.RETRY_TYPE_HANDOVER, targetTransport, Long.MAX_VALUE);
        } else if (retryDelayMillis != DataCallResponse.RETRY_DURATION_UNDEFINED) {
            // Network specifically asks retry the previous data profile again.
            DataHandoverRetryEntry dataHandoverRetryEntry = new DataHandoverRetryEntry.Builder<>()
                    .setRetryDelay(retryDelayMillis)
                    .setDataNetwork(dataNetwork)
                    .build();

            updateThrottleStatus(dataNetwork.getDataProfile(),
                    dataNetwork.getAttachedNetworkRequestList(), dataNetwork,
                    ThrottleStatus.RETRY_TYPE_HANDOVER, targetTransport,
                    dataHandoverRetryEntry.retryElapsedTime);
            schedule(dataHandoverRetryEntry);
        } else {
            // Network did not suggest any retry. Use the configured rules to perform retry.

            // Matching the rule in configured order.
            for (DataHandoverRetryRule retryRule : mDataHandoverRetryRuleList) {
                if (retryRule.getFailCauses().isEmpty()
                        || retryRule.getFailCauses().contains(cause)) {
                    int failedCount = getRetryFailedCount(dataNetwork, retryRule);
                    log("Found matching rule " + retryRule + ", failed count=" + failedCount);
                    if (failedCount == retryRule.getMaxRetries()) {
                        log("Data handover retry failed for " + failedCount + " times. Stopped "
                                + "handover retry.");
                        mDataRetryManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                                () -> callback.onDataNetworkHandoverRetryStopped(dataNetwork)));
                        return;
                    }

                    retryDelayMillis = retryRule.getRetryIntervalsMillis().get(
                            Math.min(failedCount, retryRule
                                    .getRetryIntervalsMillis().size() - 1));
                    schedule(new DataHandoverRetryEntry.Builder<>()
                            .setRetryDelay(retryDelayMillis)
                            .setDataNetwork(dataNetwork)
                            .setAppliedRetryRule(retryRule)
                            .build());
                }
            }
        }
    }

    /** Cancel all retries and throttling entries. */
    private void onReset(@RetryResetReason int reason) {
        logl("Remove all retry and throttling entries, reason=" + resetReasonToString(reason));
        removeMessages(EVENT_DATA_SETUP_RETRY);
        removeMessages(EVENT_DATA_HANDOVER_RETRY);
        mDataRetryEntries.stream()
                .filter(entry -> entry.getState() == DataRetryEntry.RETRY_STATE_NOT_RETRIED)
                .forEach(entry -> entry.setState(DataRetryEntry.RETRY_STATE_CANCELLED));

        for (DataThrottlingEntry dataThrottlingEntry : mDataThrottlingEntries) {
            DataProfile dataProfile = dataThrottlingEntry.dataProfile;
            String apn = dataProfile.getApnSetting() != null
                    ? dataProfile.getApnSetting().getApnName() : null;
            onDataProfileUnthrottled(dataProfile, apn, dataThrottlingEntry.transport, false);
        }

        mDataThrottlingEntries.clear();
    }

    /**
     * Count how many times the same setup retry rule has been used for this data network but
     * failed.
     *
     * @param dataNetwork The data network to check.
     * @param dataRetryRule The data retry rule.
     * @return The failed count since last successful data setup.
     */
    private int getRetryFailedCount(@NonNull DataNetwork dataNetwork,
            @NonNull DataHandoverRetryRule dataRetryRule) {
        int count = 0;
        for (int i = mDataRetryEntries.size() - 1; i >= 0; i--) {
            if (mDataRetryEntries.get(i) instanceof DataHandoverRetryEntry) {
                DataHandoverRetryEntry entry = (DataHandoverRetryEntry) mDataRetryEntries.get(i);
                if (entry.dataNetwork == dataNetwork
                        && dataRetryRule.equals(entry.appliedDataRetryRule)) {
                    if (entry.getState() == DataRetryEntry.RETRY_STATE_SUCCEEDED
                            || entry.getState() == DataRetryEntry.RETRY_STATE_CANCELLED) {
                        break;
                    }
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Count how many times the same setup retry rule has been used for the capability since
     * last success data setup.
     *
     * @param networkCapability The network capability to check.
     * @param dataRetryRule The data retry rule.
     * @return The failed count since last successful data setup.
     */
    private int getRetryFailedCount(@NetCapability int networkCapability,
            @NonNull DataSetupRetryRule dataRetryRule) {
        int count = 0;
        for (int i = mDataRetryEntries.size() - 1; i >= 0; i--) {
            if (mDataRetryEntries.get(i) instanceof DataSetupRetryEntry) {
                DataSetupRetryEntry entry = (DataSetupRetryEntry) mDataRetryEntries.get(i);
                // count towards the last succeeded data setup.
                if (entry.setupRetryType == DataSetupRetryEntry.RETRY_TYPE_NETWORK_REQUESTS) {
                    if (entry.networkRequestList.isEmpty()) {
                        String msg = "Invalid data retry entry detected";
                        logl(msg);
                        loge("mDataRetryEntries=" + mDataRetryEntries);
                        AnomalyReporter.reportAnomaly(
                                UUID.fromString("afeab78c-c0b0-49fc-a51f-f766814d7aa6"),
                                msg,
                                mPhone.getCarrierId());
                        continue;
                    }
                    if (entry.networkRequestList.get(0).getApnTypeNetworkCapability()
                            == networkCapability
                            && entry.appliedDataRetryRule.equals(dataRetryRule)) {
                        if (entry.getState() == DataRetryEntry.RETRY_STATE_SUCCEEDED
                                || entry.getState() == DataRetryEntry.RETRY_STATE_CANCELLED) {
                            break;
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Schedule the data retry.
     *
     * @param dataRetryEntry The data retry entry.
     */
    private void schedule(@NonNull DataRetryEntry dataRetryEntry) {
        logl("Scheduled data retry: " + dataRetryEntry);
        mDataRetryEntries.add(dataRetryEntry);
        if (mDataRetryEntries.size() >= MAXIMUM_HISTORICAL_ENTRIES) {
            // Discard the oldest retry entry.
            mDataRetryEntries.remove(0);
        }

        // Using delayed message instead of alarm manager to schedule data retry is intentional.
        // When the device enters doze mode, the handler message might be extremely delayed than the
        // original scheduled time. There is no need to wake up the device to perform data retry in
        // that case.
        sendMessageDelayed(obtainMessage(dataRetryEntry instanceof DataSetupRetryEntry
                        ? EVENT_DATA_SETUP_RETRY : EVENT_DATA_HANDOVER_RETRY, dataRetryEntry),
                dataRetryEntry.retryDelayMillis);
    }

    /**
     * Add the latest throttling request and report it to the clients.
     *
     * @param dataProfile The data profile that is being throttled for setup/handover retry.
     * @param networkRequestList The associated network request list when throttling happened.
     * Can be {@code null} when retry type is {@link ThrottleStatus#RETRY_TYPE_HANDOVER}.
     * @param dataNetwork The data network that is being throttled for handover retry.
     * Must be {@code null} when retryType is
     * {@link ThrottleStatus#RETRY_TYPE_NEW_CONNECTION}.
     * @param retryType The retry type when throttling expires.
     * @param transport The transport that the data profile has been throttled on.
     * @param expirationTime The expiration time of data throttling. This is the time retrieved from
     * {@link SystemClock#elapsedRealtime()}.
     */
    private void updateThrottleStatus(@NonNull DataProfile dataProfile,
            @Nullable NetworkRequestList networkRequestList,
            @Nullable DataNetwork dataNetwork, @RetryType int retryType,
            @TransportType int transport, @ElapsedRealtimeLong long expirationTime) {
        DataThrottlingEntry entry = new DataThrottlingEntry(dataProfile, networkRequestList,
                dataNetwork, transport, retryType, expirationTime);
        if (mDataThrottlingEntries.size() >= MAXIMUM_HISTORICAL_ENTRIES) {
            mDataThrottlingEntries.remove(0);
        }

        // Remove previous entry that contains the same data profile.
        mDataThrottlingEntries.removeIf(
                throttlingEntry -> dataProfile.equals(throttlingEntry.dataProfile));


        logl("Add throttling entry " + entry);
        mDataThrottlingEntries.add(entry);

        // For backwards compatibility, we use RETRY_TYPE_NONE if network suggests never retry.
        final int dataRetryType = expirationTime == Long.MAX_VALUE
                ? ThrottleStatus.RETRY_TYPE_NONE : retryType;

        // Report to the clients.
        final List<ThrottleStatus> throttleStatusList = new ArrayList<>();
        if (dataProfile.getApnSetting() != null) {
            throttleStatusList.addAll(dataProfile.getApnSetting().getApnTypes().stream()
                    .map(apnType -> new ThrottleStatus.Builder()
                            .setApnType(apnType)
                            .setRetryType(dataRetryType)
                            .setSlotIndex(mPhone.getPhoneId())
                            .setThrottleExpiryTimeMillis(expirationTime)
                            .setTransportType(transport)
                            .build())
                    .collect(Collectors.toList()));
        }

        mDataRetryManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onThrottleStatusChanged(throttleStatusList)));
    }

    /**
     * Called when network/modem informed to cancelling the previous throttling request.
     *
     * @param dataProfile The data profile to be unthrottled. Note this is only supported on HAL
     * with AIDL interface. When this is set, {@code apn} must be {@code null}.
     * @param apn The apn to be unthrottled. Note this should be only used for HIDL 1.6 or below.
     * When this is set, {@code dataProfile} must be {@code null}.
     * @param transport The transport that this unthrottling request is on.
     * @param remove Whether to remove unthrottled entries from the list of entries.
     */
    private void onDataProfileUnthrottled(@Nullable DataProfile dataProfile, @Nullable String apn,
            int transport, boolean remove) {
        log("onDataProfileUnthrottled: data profile=" + dataProfile + ", apn=" + apn
                + ", transport=" + AccessNetworkConstants.transportTypeToString(transport)
                + ", remove=" + remove);

        long now = SystemClock.elapsedRealtime();
        List<DataThrottlingEntry> dataUnthrottlingEntries = new ArrayList<>();
        if (dataProfile != null) {
            // For AIDL-based HAL. There should be only one entry containing this data profile.
            // Note that the data profile reconstructed from DataProfileInfo.aidl will not be
            // equal to the data profiles kept in data profile manager (due to some fields missing
            // in DataProfileInfo.aidl), so we need to get the equivalent data profile from data
            // profile manager.
            final DataProfile dp = mDataProfileManager.getDataProfile(
                    dataProfile.getApnSetting() != null
                            ? dataProfile.getApnSetting().getApnName() : null,
                    dataProfile.getTrafficDescriptor());
            log("onDataProfileUnthrottled: getDataProfile=" + dp);
            if (dp != null) {
                dataUnthrottlingEntries = mDataThrottlingEntries.stream()
                        .filter(entry -> entry.expirationTimeMillis > now
                                && entry.dataProfile.equals(dp)
                                && entry.transport == transport)
                        .collect(Collectors.toList());
            }
        } else if (apn != null) {
            // For HIDL 1.6 or below
            dataUnthrottlingEntries = mDataThrottlingEntries.stream()
                    .filter(entry -> entry.expirationTimeMillis > now
                            && entry.dataProfile.getApnSetting() != null
                            && apn.equals(entry.dataProfile.getApnSetting().getApnName())
                            && entry.transport == transport)
                    .collect(Collectors.toList());
        }

        if (dataUnthrottlingEntries.isEmpty()) {
            log("onDataProfileUnthrottled: Nothing to unthrottle.");
            return;
        }

        // Report to the clients.
        final List<ThrottleStatus> throttleStatusList = new ArrayList<>();
        DataProfile unthrottledProfile = null;
        int retryType = ThrottleStatus.RETRY_TYPE_NONE;
        if (dataUnthrottlingEntries.get(0).retryType == ThrottleStatus.RETRY_TYPE_NEW_CONNECTION) {
            unthrottledProfile = dataUnthrottlingEntries.get(0).dataProfile;
            retryType = ThrottleStatus.RETRY_TYPE_NEW_CONNECTION;
        } else if (dataUnthrottlingEntries.get(0).retryType == ThrottleStatus.RETRY_TYPE_HANDOVER) {
            unthrottledProfile = dataUnthrottlingEntries.get(0).dataNetwork.getDataProfile();
            retryType = ThrottleStatus.RETRY_TYPE_HANDOVER;
        }

        // Make it final so it can be used in the lambda function below.
        final int dataRetryType = retryType;

        if (unthrottledProfile != null && unthrottledProfile.getApnSetting() != null) {
            throttleStatusList.addAll(unthrottledProfile.getApnSetting().getApnTypes().stream()
                    .map(apnType -> new ThrottleStatus.Builder()
                            .setApnType(apnType)
                            .setSlotIndex(mPhone.getPhoneId())
                            .setNoThrottle()
                            .setRetryType(dataRetryType)
                            .setTransportType(transport)
                            .build())
                    .collect(Collectors.toList()));
        }

        mDataRetryManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onThrottleStatusChanged(throttleStatusList)));

        logl("onDataProfileUnthrottled: Removing the following throttling entries. "
                + dataUnthrottlingEntries);
        for (DataThrottlingEntry entry : dataUnthrottlingEntries) {
            if (entry.retryType == ThrottleStatus.RETRY_TYPE_NEW_CONNECTION) {
                // Immediately retry after unthrottling.
                schedule(new DataSetupRetryEntry.Builder<>()
                        .setDataProfile(entry.dataProfile)
                        .setTransport(entry.transport)
                        .setSetupRetryType(DataSetupRetryEntry.RETRY_TYPE_DATA_PROFILE)
                        .setNetworkRequestList(entry.networkRequestList)
                        .setRetryDelay(0)
                        .build());
            } else if (entry.retryType == ThrottleStatus.RETRY_TYPE_HANDOVER) {
                schedule(new DataHandoverRetryEntry.Builder<>()
                        .setDataNetwork(entry.dataNetwork)
                        .setRetryDelay(0)
                        .build());
            }
        }
        if (remove) {
            mDataThrottlingEntries.removeAll(dataUnthrottlingEntries);
        }
    }

    /**
     * Check if there is any similar network request scheduled to retry. The definition of similar
     * is that network requests have same APN capability and on the same transport.
     *
     * @param networkRequest The network request to check.
     * @param transport The transport that this request is on.
     * @return {@code true} if similar network request scheduled to retry.
     */
    public boolean isSimilarNetworkRequestRetryScheduled(
            @NonNull TelephonyNetworkRequest networkRequest, @TransportType int transport) {
        for (int i = mDataRetryEntries.size() - 1; i >= 0; i--) {
            if (mDataRetryEntries.get(i) instanceof DataSetupRetryEntry) {
                DataSetupRetryEntry entry = (DataSetupRetryEntry) mDataRetryEntries.get(i);
                if (entry.getState() == DataRetryEntry.RETRY_STATE_NOT_RETRIED
                        && entry.setupRetryType
                        == DataSetupRetryEntry.RETRY_TYPE_NETWORK_REQUESTS) {
                    if (entry.networkRequestList.isEmpty()) {
                        String msg = "Invalid data retry entry detected";
                        logl(msg);
                        loge("mDataRetryEntries=" + mDataRetryEntries);
                        AnomalyReporter.reportAnomaly(
                                UUID.fromString("781af571-f55d-476d-b510-7a5381f633dc"),
                                msg,
                                mPhone.getCarrierId());
                        continue;
                    }
                    if (entry.networkRequestList.get(0).getApnTypeNetworkCapability()
                            == networkRequest.getApnTypeNetworkCapability()
                            && entry.transport == transport) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if there is any data setup retry scheduled with specified data profile.
     *
     * @param dataProfile The data profile to retry.
     * @param transport The transport that the request is on.
     * @return {@code true} if there is retry scheduled for this data profile.
     */
    public boolean isAnySetupRetryScheduled(@NonNull DataProfile dataProfile,
            @TransportType int transport) {
        return mDataRetryEntries.stream()
                .filter(DataSetupRetryEntry.class::isInstance)
                .map(DataSetupRetryEntry.class::cast)
                .anyMatch(entry -> entry.getState() == DataRetryEntry.RETRY_STATE_NOT_RETRIED
                        && dataProfile.equals(entry.dataProfile)
                        && entry.transport == transport);
    }

    /**
     * Check if a specific data profile is explicitly throttled by the network.
     *
     * @param dataProfile The data profile to check.
     * @param transport The transport that the request is on.
     * @return {@code true} if the data profile is currently throttled.
     */
    public boolean isDataProfileThrottled(@NonNull DataProfile dataProfile,
            @TransportType int transport) {
        long now = SystemClock.elapsedRealtime();
        return mDataThrottlingEntries.stream().anyMatch(
                entry -> entry.dataProfile.equals(dataProfile) && entry.expirationTimeMillis > now
                        && entry.transport == transport);
    }

    /**
     * Cancel pending scheduled handover retry entries.
     *
     * @param dataNetwork The data network that was originally scheduled for handover retry.
     */
    public void cancelPendingHandoverRetry(@NonNull DataNetwork dataNetwork) {
        sendMessage(obtainMessage(EVENT_CANCEL_PENDING_HANDOVER_RETRY, dataNetwork));
    }

    /**
     * Called when cancelling pending scheduled handover retry entries.
     *
     * @param dataNetwork The data network that was originally scheduled for handover retry.
     */
    private void onCancelPendingHandoverRetry(@NonNull DataNetwork dataNetwork) {
        mDataRetryEntries.removeIf(entry -> entry instanceof DataHandoverRetryEntry
                && ((DataHandoverRetryEntry) entry).dataNetwork == dataNetwork);
        mDataThrottlingEntries.removeIf(entry -> entry.dataNetwork == dataNetwork);
    }

    /**
     * Check if there is any data handover retry scheduled.
     *
     * @param dataNetwork The network network to retry handover.
     * @return {@code true} if there is retry scheduled for this network capability.
     */
    public boolean isAnyHandoverRetryScheduled(@NonNull DataNetwork dataNetwork) {
        return mDataRetryEntries.stream()
                .filter(DataHandoverRetryEntry.class::isInstance)
                .map(DataHandoverRetryEntry.class::cast)
                .anyMatch(entry -> entry.getState() == DataRetryEntry.RETRY_STATE_NOT_RETRIED
                        && entry.dataNetwork == dataNetwork);
    }

    /**
     * Register the callback for receiving information from {@link DataRetryManager}.
     *
     * @param callback The callback.
     */
    public void registerCallback(@NonNull DataRetryManagerCallback callback) {
        mDataRetryManagerCallbacks.add(callback);
    }

    /**
     * Unregister the previously registered {@link DataRetryManagerCallback}.
     *
     * @param callback The callback to unregister.
     */
    public void unregisterCallback(@NonNull DataRetryManagerCallback callback) {
        mDataRetryManagerCallbacks.remove(callback);
    }

    /**
     * Convert reset reason to string
     *
     * @param reason The reason
     * @return The reason in string format.
     */
    private static @NonNull String resetReasonToString(int reason) {
        switch (reason) {
            case RESET_REASON_DATA_PROFILES_CHANGED:
                return "DATA_PROFILES_CHANGED";
            case RESET_REASON_RADIO_ON:
                return "RADIO_ON";
            case RESET_REASON_MODEM_RESTART:
                return "MODEM_RESTART";
            case RESET_REASON_DATA_SERVICE_BOUND:
                return "DATA_SERVICE_BOUND";
            case RESET_REASON_DATA_CONFIG_CHANGED:
                return "DATA_CONFIG_CHANGED";
            case RESET_REASON_TAC_CHANGED:
                return "TAC_CHANGED";
            default:
                return "UNKNOWN(" + reason + ")";
        }
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }

    /**
     * Log verbose messages.
     * @param s debug messages.
     */
    private void logv(@NonNull String s) {
        if (VDBG) Rlog.v(mLogTag, s);
    }

    /**
     * Log debug messages and also log into the local log.
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of DataRetryManager.
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(DataRetryManager.class.getSimpleName() + "-" + mPhone.getPhoneId() + ":");
        pw.increaseIndent();
        pw.println("Data Setup Retry rules:");
        pw.increaseIndent();
        mDataSetupRetryRuleList.forEach(pw::println);
        pw.decreaseIndent();
        pw.println("Data Handover Retry rules:");
        pw.increaseIndent();
        mDataHandoverRetryRuleList.forEach(pw::println);
        pw.decreaseIndent();

        pw.println("Retry entries:");
        pw.increaseIndent();
        for (DataRetryEntry entry : mDataRetryEntries) {
            pw.println(entry);
        }
        pw.decreaseIndent();

        pw.println("Throttling entries:");
        pw.increaseIndent();
        for (DataThrottlingEntry entry : mDataThrottlingEntries) {
            pw.println(entry);
        }
        pw.decreaseIndent();

        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}
