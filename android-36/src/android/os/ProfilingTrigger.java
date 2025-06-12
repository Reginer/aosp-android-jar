/*
 * Copyright (C) 2024 The Android Open Source Project
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
package android.os;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.profiling.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Encapsulates a single profiling trigger.
 */
@FlaggedApi(Flags.FLAG_SYSTEM_TRIGGERED_PROFILING_NEW)
public final class ProfilingTrigger {

    /** No trigger. Used in {@link ProfilingResult} for non trigger caused results. */
    public static final int TRIGGER_TYPE_NONE = 0;

    /** Trigger occurs after {@link Activity#reportFullyDrawn} is called for a cold start. */
    public static final int TRIGGER_TYPE_APP_FULLY_DRAWN = 1;

    /**
     * Trigger occurs after an ANR has been identified, but before the system would attempt to kill
     * the app. The trigger does not necessarily indicate that the app was killed due to the ANR.
     */
    public static final int TRIGGER_TYPE_ANR = 2;

    /** @hide */
    @IntDef(value = {
        TRIGGER_TYPE_NONE,
        TRIGGER_TYPE_APP_FULLY_DRAWN,
        TRIGGER_TYPE_ANR,
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface TriggerType {}

    /** @see #getTriggerType */
    private final @TriggerType int mTriggerType;

    /** @see #getRateLimitingPeriodHours  */
    private final int mRateLimitingPeriodHours;

    private ProfilingTrigger(@TriggerType int triggerType, int rateLimitingPeriodHours) {
        mTriggerType = triggerType;
        mRateLimitingPeriodHours = rateLimitingPeriodHours;
    }

    /**
     * Builder class to create a {@link ProfilingTrigger} object.
     */
    @FlaggedApi(Flags.FLAG_SYSTEM_TRIGGERED_PROFILING_NEW)
    public static final class Builder {
        // Trigger type has to be set, so make it an object and set to null.
        private int mBuilderTriggerType;

        // Rate limiter period default is 0 which will make it do nothing.
        private int mBuilderRateLimitingPeriodHours = 0;

        /**
         * Create a new builder instance to create a {@link ProfilingTrigger} object.
         *
         * Requires a trigger type. An app can only have one registered trigger per trigger type.
         * Adding a new trigger with the same type will override the previously set one.
         *
         * @throws IllegalArgumentException if the trigger type is not valid.
         */
        public Builder(@TriggerType int triggerType) {
            if (!isValidRequestTriggerType(triggerType)) {
                throw new IllegalArgumentException("Invalid trigger type.");
            }

            mBuilderTriggerType = triggerType;
        }

        /** Build the {@link ProfilingTrigger} object. */
        @NonNull
        public ProfilingTrigger build() {
            return new ProfilingTrigger(mBuilderTriggerType,
                    mBuilderRateLimitingPeriodHours);
        }

        /**
         * Set a rate limiting period in hours.
         *
         * The period is the minimum time the system should wait before providing another
         * profiling result for the same trigger; actual time between events may be longer.
         *
         * If the rate limiting period is not provided or set to 0, no app-provided rate limiting
         * will be used.
         *
         * This rate limiting is in addition to any system level rate limiting that may be applied.
         *
         * @throws IllegalArgumentException if the value is less than 0.
         */
        @NonNull
        public Builder setRateLimitingPeriodHours(int rateLimitingPeriodHours) {
            if (rateLimitingPeriodHours < 0) {
                throw new IllegalArgumentException("Hours can't be negative. Try again.");
            }

            mBuilderRateLimitingPeriodHours = rateLimitingPeriodHours;
            return this;
        }
    }

    /** The trigger type indicates which event should trigger the requested profiling. */
    public @TriggerType int getTriggerType() {
        return mTriggerType;
    }

    /**
     * The requester set rate limiting period in hours.
     *
     * The period is the minimum time the system should wait before providing another
     * profiling result for the same trigger; actual time between events may be longer.
     *
     * If the rate limiting period is set to 0, no app-provided rate limiting will be used.
     *
     * This rate limiting is in addition to any system level rate limiting that may be applied.
     */
    public int getRateLimitingPeriodHours() {
        return mRateLimitingPeriodHours;
    }

    /**
     * Convert to value parcel. Used for binder.
     *
     * @hide
     */
    public ProfilingTriggerValueParcel toValueParcel() {
        ProfilingTriggerValueParcel valueParcel = new ProfilingTriggerValueParcel();

        valueParcel.triggerType = mTriggerType;
        valueParcel.rateLimitingPeriodHours = mRateLimitingPeriodHours;

        return valueParcel;
    }

    /**
     * Check whether the trigger type is valid for request use. Note that this means that a value of
     * {@link TRIGGER_TYPE_NONE} will return false.
     *
     * @hide
     */
    public static boolean isValidRequestTriggerType(int triggerType) {
        return triggerType == TRIGGER_TYPE_APP_FULLY_DRAWN
                || triggerType == TRIGGER_TYPE_ANR;
    }

}
