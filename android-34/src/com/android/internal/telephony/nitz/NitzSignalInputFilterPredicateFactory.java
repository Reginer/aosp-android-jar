/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.internal.telephony.nitz;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.NitzData;
import com.android.internal.telephony.NitzSignal;
import com.android.internal.telephony.NitzStateMachine.DeviceState;
import com.android.internal.telephony.nitz.NitzStateMachineImpl.NitzSignalInputFilterPredicate;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.Objects;

/**
 * A factory class for the {@link NitzSignalInputFilterPredicate} instance used by
 * {@link NitzStateMachineImpl}. This class is exposed for testing and provides access to various
 * internal components.
 */
@VisibleForTesting
public final class NitzSignalInputFilterPredicateFactory {

    private static final String LOG_TAG = NitzStateMachineImpl.LOG_TAG;
    private static final boolean DBG = NitzStateMachineImpl.DBG;
    private static final String WAKELOCK_TAG = "NitzSignalInputFilterPredicateFactory";

    private NitzSignalInputFilterPredicateFactory() {}

    /**
     * Returns the real {@link NitzSignalInputFilterPredicate} to use for NITZ signal input
     * filtering.
     */
    @NonNull
    public static NitzSignalInputFilterPredicate create(
            @NonNull Context context, @NonNull DeviceState deviceState) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(deviceState);

        TrivalentPredicate[] components = new TrivalentPredicate[] {
                // Disables NITZ processing entirely: can return false or null.
                createIgnoreNitzPropertyCheck(deviceState),
                // Filters bad reference times from new signals: can return false or null.
                createBogusElapsedRealtimeCheck(context, deviceState),
                // Ensures oldSignal == null is always processed: can return true or null.
                createNoOldSignalCheck(),
                // Adds rate limiting: can return true or false.
                createRateLimitCheck(deviceState),
        };
        return new NitzSignalInputFilterPredicateImpl(components);
    }

    /**
     * A filtering function that can give a {@code true} (must process), {@code false} (must not
     * process) and a {@code null} (no opinion) response given a previous NITZ signal and a new
     * signal. The previous signal may be {@code null} (unless ruled out by a prior
     * {@link TrivalentPredicate}).
     */
    @VisibleForTesting
    @FunctionalInterface
    public interface TrivalentPredicate {

        /**
         * See {@link TrivalentPredicate}.
         */
        @Nullable
        Boolean mustProcessNitzSignal(
                @Nullable NitzSignal previousSignal,
                @NonNull NitzSignal newSignal);
    }

    /**
     * Returns a {@link TrivalentPredicate} function that implements a check for the
     * "gsm.ignore-nitz" Android system property. The function can return {@code false} or
     * {@code null}.
     */
    @VisibleForTesting
    @NonNull
    public static TrivalentPredicate createIgnoreNitzPropertyCheck(
            @NonNull DeviceState deviceState) {
        return (oldSignal, newSignal) -> {
            boolean ignoreNitz = deviceState.getIgnoreNitz();
            if (ignoreNitz) {
                if (DBG) {
                    Rlog.d(LOG_TAG, "mustProcessNitzSignal: Not processing NITZ signal because"
                            + " gsm.ignore-nitz is set");
                }
                return false;
            }
            return null;
        };
    }

    /**
     * Returns a {@link TrivalentPredicate} function that implements a check for a bad reference
     * time associated with {@code newSignal}. The function can return {@code false} or
     * {@code null}.
     */
    @VisibleForTesting
    @NonNull
    public static TrivalentPredicate createBogusElapsedRealtimeCheck(
            @NonNull Context context, @NonNull DeviceState deviceState) {
        PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final WakeLock wakeLock =
                powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);

        return (oldSignal, newSignal) -> {
            Objects.requireNonNull(newSignal);

            // Validate the newSignal to reject obviously bogus elapsedRealtime values.
            try {
                // Acquire the wake lock as we are reading the elapsed realtime clock below.
                wakeLock.acquire();

                long elapsedRealtime = deviceState.elapsedRealtimeMillis();
                long millisSinceNitzReceived =
                        elapsedRealtime - newSignal.getReceiptElapsedRealtimeMillis();
                if (millisSinceNitzReceived < 0 || millisSinceNitzReceived > Integer.MAX_VALUE) {
                    if (DBG) {
                        Rlog.d(LOG_TAG, "mustProcessNitzSignal: Not processing NITZ signal"
                                + " because unexpected elapsedRealtime=" + elapsedRealtime
                                + " nitzSignal=" + newSignal);
                    }
                    return false;
                }
                return null;
            } finally {
                wakeLock.release();
            }
        };
    }

    /**
     * Returns a {@link TrivalentPredicate} function that implements a check for a {@code null}
     * {@code oldSignal} (indicating there's no history). The function can return {@code true}
     * or {@code null}.
     */
    @VisibleForTesting
    @NonNull
    public static TrivalentPredicate createNoOldSignalCheck() {
        // Always process a signal when there was no previous signal.
        return (oldSignal, newSignal) -> oldSignal == null ? true : null;
    }

    /**
     * Returns a {@link TrivalentPredicate} function that implements filtering using
     * {@code oldSignal} and {@code newSignal}. The function can return {@code true} or
     * {@code false} and so is intended as the final function in a chain.
     *
     * Function detail: if an NITZ signal received that is too similar to a previous one
     * it should be disregarded if it's received within a configured time period.
     * The general contract for {@link TrivalentPredicate} allows {@code previousSignal} to be
     * {@code null}, but previous functions are expected to prevent it in this case.
     */
    @VisibleForTesting
    @NonNull
    public static TrivalentPredicate createRateLimitCheck(@NonNull DeviceState deviceState) {
        return new TrivalentPredicate() {
            @Override
            @NonNull
            public Boolean mustProcessNitzSignal(
                    @NonNull NitzSignal previousSignal,
                    @NonNull NitzSignal newSignal) {
                Objects.requireNonNull(newSignal);
                Objects.requireNonNull(newSignal.getNitzData());
                Objects.requireNonNull(previousSignal);
                Objects.requireNonNull(previousSignal.getNitzData());

                NitzData newNitzData = newSignal.getNitzData();
                NitzData previousNitzData = previousSignal.getNitzData();

                // Compare the discrete NitzData fields associated with local time offset. Any
                // difference and we should process the signal regardless of how recent the last one
                // was.
                if (!offsetInfoIsTheSame(previousNitzData, newNitzData)) {
                    return true;
                }

                // Check the time-related NitzData fields to see if they are sufficiently different.

                // See if the NITZ signals have been received sufficiently far apart. If yes, we
                // want to process the new one.
                int nitzUpdateSpacing = deviceState.getNitzUpdateSpacingMillis();
                long elapsedRealtimeSinceLastSaved = newSignal.getReceiptElapsedRealtimeMillis()
                        - previousSignal.getReceiptElapsedRealtimeMillis();
                if (elapsedRealtimeSinceLastSaved > nitzUpdateSpacing) {
                    return true;
                }

                // See if the NITZ signals have sufficiently different encoded Unix epoch times. If
                // yes, then we want to process the new one.
                int nitzUpdateDiff = deviceState.getNitzUpdateDiffMillis();

                // Calculate the Unix epoch difference between the time the two signals hold,
                // accounting for any difference in receipt time and age.
                long unixEpochTimeDifferenceMillis = newNitzData.getCurrentTimeInMillis()
                        - previousNitzData.getCurrentTimeInMillis();
                long ageAdjustedElapsedRealtimeDifferenceMillis =
                        newSignal.getAgeAdjustedElapsedRealtimeMillis()
                                - previousSignal.getAgeAdjustedElapsedRealtimeMillis();

                // In ideal conditions, the difference between
                // ageAdjustedElapsedRealtimeSinceLastSaved and unixEpochTimeDifferenceMillis will
                // be zero if two NITZ signals are consistent and if the elapsed realtime clock is
                // ticking at the correct rate.
                long millisGainedOrLost = Math.abs(
                        unixEpochTimeDifferenceMillis - ageAdjustedElapsedRealtimeDifferenceMillis);
                if (millisGainedOrLost > nitzUpdateDiff) {
                    return true;
                }

                if (DBG) {
                    Rlog.d(LOG_TAG, "mustProcessNitzSignal: NITZ signal filtered"
                            + " previousSignal=" + previousSignal
                            + ", newSignal=" + newSignal
                            + ", nitzUpdateSpacing=" + nitzUpdateSpacing
                            + ", nitzUpdateDiff=" + nitzUpdateDiff);
                }
                return false;
            }

            private boolean offsetInfoIsTheSame(NitzData one, NitzData two) {
                return Objects.equals(two.getDstAdjustmentMillis(), one.getDstAdjustmentMillis())
                        && Objects.equals(
                                two.getEmulatorHostTimeZone(), one.getEmulatorHostTimeZone())
                        && two.getLocalOffsetMillis() == one.getLocalOffsetMillis();
            }
        };
    }

    /**
     * An implementation of {@link NitzSignalInputFilterPredicate} that tries a series of
     * {@link TrivalentPredicate} instances until one provides a {@code true} or {@code false}
     * response indicating that the {@code newSignal} should be processed or not. If all return
     * {@code null} then a default of {@code true} is returned.
     */
    @VisibleForTesting
    public static class NitzSignalInputFilterPredicateImpl
            implements NitzSignalInputFilterPredicate {

        @NonNull
        private final TrivalentPredicate[] mComponents;

        @VisibleForTesting
        public NitzSignalInputFilterPredicateImpl(@NonNull TrivalentPredicate[] components) {
            this.mComponents = Arrays.copyOf(components, components.length);
        }

        @Override
        public boolean mustProcessNitzSignal(@Nullable NitzSignal oldSignal,
                @NonNull NitzSignal newSignal) {
            Objects.requireNonNull(newSignal);

            for (TrivalentPredicate component : mComponents) {
                Boolean result = component.mustProcessNitzSignal(oldSignal, newSignal);
                if (result != null) {
                    return result;
                }
            }
            // The default is to process.
            return true;
        }
    }
}
