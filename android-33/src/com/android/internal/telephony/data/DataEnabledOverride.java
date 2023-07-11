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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.telephony.Annotation.ApnType;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.data.DataEnabledOverride.OverrideConditions.Condition;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents the rules for overriding data enabled settings in different conditions.
 * When data is disabled by the user, data can still be turned on temporarily when conditions
 * satisfy any rule here.
 */
public class DataEnabledOverride {

    private final Set<OverrideRule> mRules = new HashSet<>();

    /**
     * The rule for allowing data during voice call.
     */
    private static final OverrideRule OVERRIDE_RULE_ALLOW_DATA_DURING_VOICE_CALL =
            new OverrideRule(ApnSetting.TYPE_ALL, OverrideConditions.CONDITION_IN_VOICE_CALL
                    | OverrideConditions.CONDITION_NON_DEFAULT
                    | OverrideConditions.CONDITION_DEFAULT_DATA_ENABLED
                    | OverrideConditions.CONDITION_DSDS_ENABLED);

    /**
     * The rule for always allowing mms. Without adding any condition to the rule, any condition can
     * satisfy this rule for mms.
     */
    private static final OverrideRule OVERRIDE_RULE_ALWAYS_ALLOW_MMS =
            new OverrideRule(ApnSetting.TYPE_MMS, OverrideConditions.CONDITION_UNCONDITIONALLY);

    /**
     * Data enabled override rule
     */
    private static class OverrideRule {
        /**
         * APN type of the rule. The rule is APN type specific. The override is applicable to the
         * specified APN type as well. For now we only support one APN type per rule. Can be
         * expanded to multiple APN types in the future.
         */
        private final @ApnType int mApnType;

        /** The required conditions for overriding */
        private final OverrideConditions mRequiredConditions;

        /**
         * Constructor
         *
         * @param rule The override rule string. For example, {@code mms=nonDefault} or
         * {@code default=voiceCall & nonDefault}
         */
        OverrideRule(@NonNull String rule) {
            String[] tokens = rule.trim().split("\\s*=\\s*");
            if (tokens.length != 2) {
                throw new IllegalArgumentException("Invalid data enabled override rule format: "
                        + rule);
            }

            if (TextUtils.isEmpty(tokens[0])) {
                throw new IllegalArgumentException("APN type can't be empty");
            }

            mApnType = ApnSetting.getApnTypesBitmaskFromString(tokens[0]);
            if (mApnType == ApnSetting.TYPE_NONE) {
                throw new IllegalArgumentException("Invalid APN type. Rule=" + rule);
            }

            mRequiredConditions = new OverrideConditions(tokens[1]);
        }

        /**
         * Constructor
         *
         * @param apnType APN type of the rule
         * @param requiredConditions The required conditions for the rule
         */
        private OverrideRule(int apnType, int requiredConditions) {
            mApnType = apnType;
            mRequiredConditions = new OverrideConditions(requiredConditions);
        }

        /**
         * Check if this rule can be satisfied by the given APN type and provided conditions.
         *
         * @param apnType APN type to check
         * @param providedConditions The provided conditions to check
         * @return {@code true} if satisfied
         */
        boolean isSatisfiedByConditions(@ApnType int apnType, @Condition int providedConditions) {
            return (mApnType == apnType || mApnType == ApnSetting.TYPE_ALL)
                    && mRequiredConditions.allMet(providedConditions);
        }

        @Override
        public String toString() {
            return ApnSetting.getApnTypeString(mApnType) + "=" + mRequiredConditions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OverrideRule that = (OverrideRule) o;
            return mApnType == that.mApnType
                    && Objects.equals(mRequiredConditions, that.mRequiredConditions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mApnType, mRequiredConditions);
        }
    }

    /**
     * Represent the conditions for overriding data enabled settings
     */
    static class OverrideConditions {
        // Possible values for data enabled override condition. Note these flags are bitmasks.
        /** Unconditionally override enabled settings */
        static final int CONDITION_UNCONDITIONALLY = 0;

        /** Enable data only on subscription that is not user selected default data subscription */
        static final int CONDITION_NON_DEFAULT = 1 << 0;

        /** Enable data only when device has ongoing voice call */
        static final int CONDITION_IN_VOICE_CALL = 1 << 1;

        /** Enable data only when default data is on */
        static final int CONDITION_DEFAULT_DATA_ENABLED = 1 << 2;

        /** Enable data only when device is in DSDS mode */
        static final int CONDITION_DSDS_ENABLED = 1 << 3;

        /** Enable data unconditionally in string format */
        static final String CONDITION_UNCONDITIONALLY_STRING = "unconditionally";

        /** Enable data only on subscription that is not default in string format */
        static final String CONDITION_NON_DEFAULT_STRING = "nonDefault";

        /** Enable data only when device has ongoing voice call in string format */
        static final String CONDITION_VOICE_CALL_STRING = "inVoiceCall";

        /** Enable data only when default data is on in string format */
        static final String CONDITION_DEFAULT_DATA_ENABLED_STRING = "DefaultDataOn";

        /** Enable data only when device is in DSDS mode in string format */
        static final String CONDITION_DSDS_ENABLED_STRING = "dsdsEnabled";

        /** @hide */
        @IntDef(flag = true, prefix = { "OVERRIDE_CONDITION_" }, value = {
                CONDITION_NON_DEFAULT,
                CONDITION_IN_VOICE_CALL,
                CONDITION_DEFAULT_DATA_ENABLED,
                CONDITION_DSDS_ENABLED
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Condition {}

        private static final Map<Integer, String> OVERRIDE_CONDITION_INT_MAP = new ArrayMap<>();
        private static final Map<String, Integer> OVERRIDE_CONDITION_STRING_MAP = new ArrayMap<>();

        static {
            OVERRIDE_CONDITION_INT_MAP.put(CONDITION_NON_DEFAULT,
                    CONDITION_NON_DEFAULT_STRING);
            OVERRIDE_CONDITION_INT_MAP.put(CONDITION_IN_VOICE_CALL,
                    CONDITION_VOICE_CALL_STRING);
            OVERRIDE_CONDITION_INT_MAP.put(CONDITION_DEFAULT_DATA_ENABLED,
                    CONDITION_DEFAULT_DATA_ENABLED_STRING);
            OVERRIDE_CONDITION_INT_MAP.put(CONDITION_DSDS_ENABLED,
                    CONDITION_DSDS_ENABLED_STRING);

            OVERRIDE_CONDITION_STRING_MAP.put(CONDITION_UNCONDITIONALLY_STRING,
                    CONDITION_UNCONDITIONALLY);
            OVERRIDE_CONDITION_STRING_MAP.put(CONDITION_NON_DEFAULT_STRING,
                    CONDITION_NON_DEFAULT);
            OVERRIDE_CONDITION_STRING_MAP.put(CONDITION_VOICE_CALL_STRING,
                    CONDITION_IN_VOICE_CALL);
            OVERRIDE_CONDITION_STRING_MAP.put(CONDITION_DEFAULT_DATA_ENABLED_STRING,
                    CONDITION_DEFAULT_DATA_ENABLED);
            OVERRIDE_CONDITION_STRING_MAP.put(CONDITION_DSDS_ENABLED_STRING,
                    CONDITION_DSDS_ENABLED);
        }

        private final @Condition int mConditions;

        /**
         * Conditions for overriding data enabled setting
         *
         * @param conditions Conditions in string format
         */
        OverrideConditions(@NonNull String conditions) {
            mConditions = getBitmaskFromString(conditions);
        }

        /**
         * Conditions for overriding data enabled setting
         *
         * @param conditions Conditions in bitmask
         */
        OverrideConditions(@Condition int conditions) {
            mConditions = conditions;
        }

        private static String getStringFromBitmask(@Condition int conditions) {
            if (conditions == CONDITION_UNCONDITIONALLY) {
                return CONDITION_UNCONDITIONALLY_STRING;
            }
            List<String> conditionsStrings = new ArrayList<>();
            for (Integer condition : OVERRIDE_CONDITION_INT_MAP.keySet()) {
                if ((conditions & condition) == condition) {
                    conditionsStrings.add(OVERRIDE_CONDITION_INT_MAP.get(condition));
                }
            }
            return TextUtils.join("&", conditionsStrings);
        }

        private static @Condition int getBitmaskFromString(@NonNull String str) {
            if (TextUtils.isEmpty(str)) {
                throw new IllegalArgumentException("Empty rule string");
            }

            String[] conditionStrings = str.trim().split("\\s*&\\s*");
            int bitmask = 0;

            for (String conditionStr : conditionStrings) {
                if (!TextUtils.isEmpty(conditionStr)) {
                    if (!OVERRIDE_CONDITION_STRING_MAP.containsKey(conditionStr)) {
                        throw new IllegalArgumentException("Invalid conditions: " + str);
                    }
                    bitmask |= OVERRIDE_CONDITION_STRING_MAP.get(conditionStr);
                }
            }

            return bitmask;
        }

        /**
         * Check if provided conditions can meet all conditions in the rule.
         *
         * @param providedConditions The provided conditions
         * @return {@code true} if all conditions are met.
         */
        boolean allMet(@Condition int providedConditions) {
            return (providedConditions & mConditions) == mConditions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OverrideConditions that = (OverrideConditions) o;
            return mConditions == that.mConditions;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mConditions);
        }

        @Override
        public String toString() {
            return getStringFromBitmask(mConditions);
        }
    }

    /**
     * Constructor
     *
     * @param rules Data enabled override rules
     */
    public DataEnabledOverride(@NonNull String rules) {
        updateRules(rules);
    }

    /**
     * Update the data enabled override rules.
     *
     * @param newRules New override rules
     */
    @VisibleForTesting
    public void updateRules(@NonNull String newRules) {
        mRules.clear();
        String[] rulesString = newRules.trim().split("\\s*,\\s*");
        for (String rule : rulesString) {
            if (!TextUtils.isEmpty(rule)) {
                mRules.add(new OverrideRule(rule));
            }
        }
    }

    /**
     * Set always allowing MMS
     *
     * @param allow {@code true} if always allowing, otherwise {@code false}.
     */
    public void setAlwaysAllowMms(boolean allow) {
        if (allow) {
            mRules.add(OVERRIDE_RULE_ALWAYS_ALLOW_MMS);
        } else {
            mRules.remove(OVERRIDE_RULE_ALWAYS_ALLOW_MMS);
        }
    }

    /**
     * Set allowing mobile data during voice call. This is used for allowing data on the non-default
     * data SIM. When a voice call is placed on the non-default data SIM on DSDS devices, users will
     * not be able to use mobile data. By calling this API, data will be temporarily enabled on the
     * non-default data SIM during the life cycle of the voice call.
     *
     * @param allow {@code true} if allowing using data during voice call, {@code false} if
     * disallowed.
     */
    public void setDataAllowedInVoiceCall(boolean allow) {
        if (allow) {
            mRules.add(OVERRIDE_RULE_ALLOW_DATA_DURING_VOICE_CALL);
        } else {
            mRules.remove(OVERRIDE_RULE_ALLOW_DATA_DURING_VOICE_CALL);
        }
    }

    /**
     * Check if data is allowed during voice call.
     *
     * @return {@code true} if data is allowed during voice call.
     */
    public boolean isDataAllowedInVoiceCall() {
        return mRules.contains(OVERRIDE_RULE_ALLOW_DATA_DURING_VOICE_CALL);
    }

    public boolean isMmsAlwaysAllowed() {
        return mRules.contains(OVERRIDE_RULE_ALWAYS_ALLOW_MMS);
    }

    private boolean canSatisfyAnyRule(@ApnType int apnType,
                                      @Condition int providedConditions) {
        for (OverrideRule rule : mRules) {
            if (rule.isSatisfiedByConditions(apnType, providedConditions)) {
                return true;
            }
        }
        return false;
    }

    private @Condition int getCurrentConditions(Phone phone) {
        int conditions = 0;

        if (phone != null) {
            // Check if the device is on voice call
            if (phone.getState() != PhoneConstants.State.IDLE) {
                conditions |= OverrideConditions.CONDITION_IN_VOICE_CALL;
            }

            int defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();

            if (phone.getSubId() != defaultDataSubId) {
                conditions |= OverrideConditions.CONDITION_NON_DEFAULT;
            }

            if (defaultDataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                int phoneId = SubscriptionController.getInstance().getPhoneId(defaultDataSubId);
                try {
                    Phone defaultDataPhone = PhoneFactory.getPhone(phoneId);
                    if (defaultDataPhone != null && defaultDataPhone.isUserDataEnabled()) {
                        conditions |= OverrideConditions.CONDITION_DEFAULT_DATA_ENABLED;
                    }
                } catch (IllegalStateException e) {
                    //ignore the exception and do not add the condition
                    Log.d("DataEnabledOverride", e.getMessage());
                }
            }

            if (TelephonyManager.from(phone.getContext()).isMultiSimEnabled()) {
                conditions |= OverrideConditions.CONDITION_DSDS_ENABLED;
            }
        }

        return conditions;
    }

    /**
     * Check for given APN type if we should enable data.
     *
     * @param phone Phone object
     * @param apnType APN type
     * @return {@code true} if data should be enabled for the current condition.
     */
    public boolean shouldOverrideDataEnabledSettings(Phone phone, @ApnType int apnType) {
        return canSatisfyAnyRule(apnType, getCurrentConditions(phone));
    }

    /**
     * Get data enabled override rules.
     *
     * @return Get data enabled override rules in string format
     */
    @NonNull
    public String getRules() {
        List<String> ruleStrings = new ArrayList<>();
        for (OverrideRule rule : mRules) {
            ruleStrings.add(rule.toString());
        }
        return TextUtils.join(",", ruleStrings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataEnabledOverride that = (DataEnabledOverride) o;
        return mRules.equals(that.mRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRules);
    }

    @Override
    public String toString() {
        return "DataEnabledOverride: [rules=\"" + getRules() + "\"]";
    }
}
