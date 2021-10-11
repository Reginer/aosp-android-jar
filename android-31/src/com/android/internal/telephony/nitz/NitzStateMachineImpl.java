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
import android.app.timedetector.TelephonyTimeSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
import android.content.Context;
import android.os.TimestampedValue;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.NitzData;
import com.android.internal.telephony.NitzStateMachine;
import com.android.internal.telephony.Phone;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;

/**
 * An implementation of {@link NitzStateMachine} responsible for telephony time and time zone
 * detection.
 *
 * <p>This implementation has a number of notable characteristics:
 * <ul>
 *     <li>It is decomposed into multiple classes that perform specific, well-defined, usually
 *     stateless, testable behaviors.
 *     </li>
 *     <li>It splits responsibility for setting the device time zone with a "time zone detection
 *     service". The time zone detection service is stateful, recording the latest suggestion from
 *     several sources. The {@link NitzStateMachineImpl} actively signals when it has no answer
 *     for the current time zone, allowing the service to arbitrate between the multiple sources
 *     without polling each of them.
 *     </li>
 *     <li>Rate limiting of NITZ signals is performed for time zone as well as time detection.</li>
 * </ul>
 */
public final class NitzStateMachineImpl implements NitzStateMachine {

    /**
     * An interface for predicates applied to incoming NITZ signals to determine whether they must
     * be processed. See {@link NitzSignalInputFilterPredicateFactory#create(Context, DeviceState)}
     * for the real implementation. The use of an interface means the behavior can be tested
     * independently and easily replaced for tests.
     */
    @VisibleForTesting
    @FunctionalInterface
    public interface NitzSignalInputFilterPredicate {

        /**
         * See {@link NitzSignalInputFilterPredicate}.
         */
        boolean mustProcessNitzSignal(
                @Nullable TimestampedValue<NitzData> oldSignal,
                @NonNull TimestampedValue<NitzData> newSignal);
    }

    /**
     * An interface for the stateless component that generates suggestions using country and/or NITZ
     * information. The use of an interface means the behavior can be tested independently.
     */
    @VisibleForTesting
    public interface TimeZoneSuggester {

        /**
         * Generates a {@link TelephonyTimeZoneSuggestion} given the information available. This
         * method must always return a non-null {@link TelephonyTimeZoneSuggestion} but that object
         * does not have to contain a time zone if the available information is not sufficient to
         * determine one. {@link TelephonyTimeZoneSuggestion#getDebugInfo()} provides debugging /
         * logging information explaining the choice.
         */
        @NonNull
        TelephonyTimeZoneSuggestion getTimeZoneSuggestion(
                int slotIndex, @Nullable String countryIsoCode,
                @Nullable TimestampedValue<NitzData> nitzSignal);
    }

    static final String LOG_TAG = "NewNitzStateMachineImpl";
    static final boolean DBG = true;

    // Miscellaneous dependencies and helpers not related to detection state.
    private final int mSlotIndex;
    /** Applied to NITZ signals during input filtering. */
    private final NitzSignalInputFilterPredicate mNitzSignalInputFilter;
    /**
     * Creates a {@link TelephonyTimeZoneSuggestion} for passing to the time zone detection service.
     */
    private final TimeZoneSuggester mTimeZoneSuggester;
    /** A facade to the time / time zone detection services. */
    private final TimeServiceHelper mTimeServiceHelper;

    // Shared detection state.

    /**
     * The last / latest NITZ signal <em>processed</em> (i.e. after input filtering). It is used for
     * input filtering (e.g. rate limiting) and provides the NITZ information when time / time zone
     * needs to be recalculated when something else has changed.
     */
    @Nullable
    private TimestampedValue<NitzData> mLatestNitzSignal;

    // Time Zone detection state.

    /**
     * Records the country to use for time zone detection. It can be a valid ISO 3166 alpha-2 code
     * (lower case), empty (test network) or null (no country detected). A country code is required
     * to determine time zone except when on a test network.
     */
    private String mCountryIsoCode;

    /**
     * Creates an instance for the supplied {@link Phone}.
     */
    public static NitzStateMachineImpl createInstance(@NonNull Phone phone) {
        Objects.requireNonNull(phone);

        int slotIndex = phone.getPhoneId();
        DeviceState deviceState = new DeviceStateImpl(phone);
        TimeZoneLookupHelper timeZoneLookupHelper = new TimeZoneLookupHelper();
        TimeZoneSuggester timeZoneSuggester =
                new TimeZoneSuggesterImpl(deviceState, timeZoneLookupHelper);
        TimeServiceHelper newTimeServiceHelper = new TimeServiceHelperImpl(phone);
        NitzSignalInputFilterPredicate nitzSignalFilter =
                NitzSignalInputFilterPredicateFactory.create(phone.getContext(), deviceState);
        return new NitzStateMachineImpl(
                slotIndex, nitzSignalFilter, timeZoneSuggester, newTimeServiceHelper);
    }

    /**
     * Creates an instance using the supplied components. Used during tests to supply fakes.
     * See {@link #createInstance(Phone)}
     */
    @VisibleForTesting
    public NitzStateMachineImpl(int slotIndex,
            @NonNull NitzSignalInputFilterPredicate nitzSignalInputFilter,
            @NonNull TimeZoneSuggester timeZoneSuggester,
            @NonNull TimeServiceHelper newTimeServiceHelper) {
        mSlotIndex = slotIndex;
        mTimeZoneSuggester = Objects.requireNonNull(timeZoneSuggester);
        mTimeServiceHelper = Objects.requireNonNull(newTimeServiceHelper);
        mNitzSignalInputFilter = Objects.requireNonNull(nitzSignalInputFilter);
    }

    @Override
    public void handleNetworkAvailable() {
        // We no longer do any useful work here: we assume handleNetworkUnavailable() is reliable.
        // TODO: Remove this method when all implementations do nothing.
    }

    @Override
    public void handleNetworkUnavailable() {
        String reason = "handleNetworkUnavailable()";
        clearNetworkStateAndRerunDetection(reason);
    }

    private void clearNetworkStateAndRerunDetection(String reason) {
        if (mLatestNitzSignal == null) {
            // The network state is already empty so there's no need to do anything.
            if (DBG) {
                Rlog.d(LOG_TAG, reason + ": mLatestNitzSignal was already null. Nothing to do.");
            }
            return;
        }

        // The previous NITZ signal received is now invalid so clear it.
        mLatestNitzSignal = null;

        // countryIsoCode can be assigned null here, in which case the doTimeZoneDetection() call
        // below will do nothing, which is ok as nothing will have changed.
        String countryIsoCode = mCountryIsoCode;
        if (DBG) {
            Rlog.d(LOG_TAG, reason + ": countryIsoCode=" + countryIsoCode);
        }

        // Generate a new time zone suggestion (which could be an empty suggestion) and update the
        // service as needed.
        doTimeZoneDetection(countryIsoCode, null /* nitzSignal */, reason);

        // Generate a new time suggestion and update the service as needed.
        doTimeDetection(null /* nitzSignal */, reason);
    }

    @Override
    public void handleCountryDetected(@NonNull String countryIsoCode) {
        if (DBG) {
            Rlog.d(LOG_TAG, "handleCountryDetected: countryIsoCode=" + countryIsoCode
                    + ", mLatestNitzSignal=" + mLatestNitzSignal);
        }

        String oldCountryIsoCode = mCountryIsoCode;
        mCountryIsoCode = Objects.requireNonNull(countryIsoCode);
        if (!Objects.equals(oldCountryIsoCode, mCountryIsoCode)) {
            // Generate a new time zone suggestion and update the service as needed.
            doTimeZoneDetection(countryIsoCode, mLatestNitzSignal,
                    "handleCountryDetected(\"" + countryIsoCode + "\")");
        }
    }

    @Override
    public void handleCountryUnavailable() {
        if (DBG) {
            Rlog.d(LOG_TAG, "handleCountryUnavailable:"
                    + " mLatestNitzSignal=" + mLatestNitzSignal);
        }
        mCountryIsoCode = null;

        // Generate a new time zone suggestion and update the service as needed.
        doTimeZoneDetection(null /* countryIsoCode */, mLatestNitzSignal,
                "handleCountryUnavailable()");
    }

    @Override
    public void handleNitzReceived(@NonNull TimestampedValue<NitzData> nitzSignal) {
        if (DBG) {
            Rlog.d(LOG_TAG, "handleNitzReceived: nitzSignal=" + nitzSignal);
        }
        Objects.requireNonNull(nitzSignal);

        // Perform input filtering to filter bad data and avoid processing signals too often.
        TimestampedValue<NitzData> previousNitzSignal = mLatestNitzSignal;
        if (!mNitzSignalInputFilter.mustProcessNitzSignal(previousNitzSignal, nitzSignal)) {
            return;
        }

        // Always store the latest valid NITZ signal to be processed.
        mLatestNitzSignal = nitzSignal;

        String reason = "handleNitzReceived(" + nitzSignal + ")";

        // Generate a new time zone suggestion and update the service as needed.
        String countryIsoCode = mCountryIsoCode;
        doTimeZoneDetection(countryIsoCode, nitzSignal, reason);

        // Generate a new time suggestion and update the service as needed.
        doTimeDetection(nitzSignal, reason);
    }

    @Override
    public void handleAirplaneModeChanged(boolean on) {
        // Treat entry / exit from airplane mode as a strong signal that the user wants to clear
        // cached state. If the user really is boarding a plane they won't want cached state from
        // before their flight influencing behavior.
        //
        // State is cleared on entry AND exit: on entry because the detection code shouldn't be
        // opinionated while in airplane mode, and on exit to avoid any unexpected signals received
        // while in airplane mode from influencing behavior afterwards.
        //
        // After clearing detection state, the time zone detection should work out from first
        // principles what the time / time zone is. This assumes calls like handleNetworkAvailable()
        // will be made after airplane mode is re-enabled as the device re-establishes network
        // connectivity.

        // Clear country detection state.
        mCountryIsoCode = null;

        String reason = "handleAirplaneModeChanged(" + on + ")";
        clearNetworkStateAndRerunDetection(reason);
    }

    /**
     * Perform a round of time zone detection and notify the time zone detection service as needed.
     */
    private void doTimeZoneDetection(
            @Nullable String countryIsoCode, @Nullable TimestampedValue<NitzData> nitzSignal,
            @NonNull String reason) {
        try {
            Objects.requireNonNull(reason);

            TelephonyTimeZoneSuggestion suggestion = mTimeZoneSuggester.getTimeZoneSuggestion(
                    mSlotIndex, countryIsoCode, nitzSignal);
            suggestion.addDebugInfo("Detection reason=" + reason);

            if (DBG) {
                Rlog.d(LOG_TAG, "doTimeZoneDetection: countryIsoCode=" + countryIsoCode
                        + ", nitzSignal=" + nitzSignal + ", suggestion=" + suggestion
                        + ", reason=" + reason);
            }
            mTimeServiceHelper.maybeSuggestDeviceTimeZone(suggestion);
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "doTimeZoneDetection: Exception thrown"
                    + " mSlotIndex=" + mSlotIndex
                    + ", countryIsoCode=" + countryIsoCode
                    + ", nitzSignal=" + nitzSignal
                    + ", reason=" + reason
                    + ", ex=" + ex, ex);
        }
    }

    /**
     * Perform a round of time detection and notify the time detection service as needed.
     */
    private void doTimeDetection(@Nullable TimestampedValue<NitzData> nitzSignal,
            @NonNull String reason) {
        try {
            Objects.requireNonNull(reason);

            TelephonyTimeSuggestion.Builder builder =
                    new TelephonyTimeSuggestion.Builder(mSlotIndex);
            if (nitzSignal == null) {
                builder.addDebugInfo("Clearing time suggestion"
                        + " reason=" + reason);
            } else {
                TimestampedValue<Long> newNitzTime = new TimestampedValue<>(
                        nitzSignal.getReferenceTimeMillis(),
                        nitzSignal.getValue().getCurrentTimeInMillis());
                builder.setUtcTime(newNitzTime);
                builder.addDebugInfo("Sending new time suggestion"
                        + " nitzSignal=" + nitzSignal
                        + ", reason=" + reason);
            }
            mTimeServiceHelper.suggestDeviceTime(builder.build());
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "doTimeDetection: Exception thrown"
                    + " mSlotIndex=" + mSlotIndex
                    + ", nitzSignal=" + nitzSignal
                    + ", reason=" + reason
                    + ", ex=" + ex, ex);
        }
    }

    @Override
    public void dumpState(PrintWriter pw) {
        pw.println(" NitzStateMachineImpl.mLatestNitzSignal=" + mLatestNitzSignal);
        pw.println(" NitzStateMachineImpl.mCountryIsoCode=" + mCountryIsoCode);
        mTimeServiceHelper.dumpState(pw);
        pw.flush();
    }

    @Override
    public void dumpLogs(FileDescriptor fd, IndentingPrintWriter ipw, String[] args) {
        mTimeServiceHelper.dumpLogs(ipw);
    }

    @Nullable
    public NitzData getCachedNitzData() {
        return mLatestNitzSignal != null ? mLatestNitzSignal.getValue() : null;
    }
}
