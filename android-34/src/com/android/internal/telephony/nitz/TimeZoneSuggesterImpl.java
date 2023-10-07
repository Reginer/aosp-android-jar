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

import static android.app.timezonedetector.TelephonyTimeZoneSuggestion.createEmptySuggestion;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
import android.text.TextUtils;
import android.timezone.CountryTimeZones.OffsetResult;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.NitzData;
import com.android.internal.telephony.NitzSignal;
import com.android.internal.telephony.NitzStateMachine.DeviceState;
import com.android.internal.telephony.nitz.NitzStateMachineImpl.TimeZoneSuggester;
import com.android.internal.telephony.nitz.TimeZoneLookupHelper.CountryResult;
import com.android.telephony.Rlog;

import java.util.Objects;

/**
 * The real implementation of {@link TimeZoneSuggester}.
 */
@VisibleForTesting
public class TimeZoneSuggesterImpl implements TimeZoneSuggester {

    private static final String LOG_TAG = NitzStateMachineImpl.LOG_TAG;

    private final DeviceState mDeviceState;
    private final TimeZoneLookupHelper mTimeZoneLookupHelper;

    @VisibleForTesting
    public TimeZoneSuggesterImpl(
            @NonNull DeviceState deviceState, @NonNull TimeZoneLookupHelper timeZoneLookupHelper) {
        mDeviceState = Objects.requireNonNull(deviceState);
        mTimeZoneLookupHelper = Objects.requireNonNull(timeZoneLookupHelper);
    }

    @Override
    @NonNull
    public TelephonyTimeZoneSuggestion getTimeZoneSuggestion(int slotIndex,
            @Nullable String countryIsoCode, @Nullable NitzSignal nitzSignal) {
        try {
            // Check for overriding NITZ-based signals from Android running in an emulator.
            TelephonyTimeZoneSuggestion overridingSuggestion = null;
            if (nitzSignal != null) {
                NitzData nitzData = nitzSignal.getNitzData();
                if (nitzData.getEmulatorHostTimeZone() != null) {
                    TelephonyTimeZoneSuggestion.Builder builder =
                            new TelephonyTimeZoneSuggestion.Builder(slotIndex)
                            .setZoneId(nitzData.getEmulatorHostTimeZone().getID())
                            .setMatchType(TelephonyTimeZoneSuggestion.MATCH_TYPE_EMULATOR_ZONE_ID)
                            .setQuality(TelephonyTimeZoneSuggestion.QUALITY_SINGLE_ZONE)
                            .addDebugInfo("Emulator time zone override: " + nitzData);
                    overridingSuggestion = builder.build();
                }
            }

            TelephonyTimeZoneSuggestion suggestion;
            if (overridingSuggestion != null) {
                suggestion = overridingSuggestion;
            } else if (countryIsoCode == null) {
                if (nitzSignal == null) {
                    suggestion = createEmptySuggestion(slotIndex,
                            "getTimeZoneSuggestion: nitzSignal=null, countryIsoCode=null");
                } else {
                    // NITZ only - wait until we have a country.
                    suggestion = createEmptySuggestion(slotIndex, "getTimeZoneSuggestion:"
                            + " nitzSignal=" + nitzSignal + ", countryIsoCode=null");
                }
            } else { // countryIsoCode != null
                if (nitzSignal == null) {
                    if (countryIsoCode.isEmpty()) {
                        // This is assumed to be a test network with no NITZ data to go on.
                        suggestion = createEmptySuggestion(slotIndex,
                                "getTimeZoneSuggestion: nitzSignal=null, countryIsoCode=\"\"");
                    } else {
                        // Country only
                        suggestion = findTimeZoneFromNetworkCountryCode(
                                slotIndex, countryIsoCode, mDeviceState.currentTimeMillis());
                    }
                } else { // nitzSignal != null
                    if (countryIsoCode.isEmpty()) {
                        // We have been told we have a country code but it's empty. This is most
                        // likely because we're on a test network that's using a bogus MCC
                        // (eg, "001"). Obtain a TimeZone based only on the NITZ parameters: without
                        // a country it will be arbitrary, but it should at least have the correct
                        // offset.
                        suggestion = findTimeZoneForTestNetwork(slotIndex, nitzSignal);
                    } else {
                        // We have both NITZ and Country code.
                        suggestion = findTimeZoneFromCountryAndNitz(
                                slotIndex, countryIsoCode, nitzSignal);
                    }
                }
            }

            // Ensure the return value is never null.
            Objects.requireNonNull(suggestion);

            return suggestion;
        } catch (RuntimeException e) {
            // This would suggest a coding error. Log at a high level and try to avoid leaving the
            // device in a bad state by making an "empty" suggestion.
            String message = "getTimeZoneSuggestion: Error during lookup: "
                    + " countryIsoCode=" + countryIsoCode
                    + ", nitzSignal=" + nitzSignal
                    + ", e=" + e.getMessage();
            TelephonyTimeZoneSuggestion errorSuggestion = createEmptySuggestion(slotIndex, message);
            Rlog.w(LOG_TAG, message, e);
            return errorSuggestion;
        }
    }

    /**
     * Creates a {@link TelephonyTimeZoneSuggestion} using only NITZ. This happens when the device
     * is attached to a test cell with an unrecognized MCC. In these cases we try to return a
     * suggestion for an arbitrary time zone that matches the NITZ offset information.
     */
    @NonNull
    private TelephonyTimeZoneSuggestion findTimeZoneForTestNetwork(
            int slotIndex, @NonNull NitzSignal nitzSignal) {
        Objects.requireNonNull(nitzSignal);
        NitzData nitzData = Objects.requireNonNull(nitzSignal.getNitzData());

        TelephonyTimeZoneSuggestion.Builder suggestionBuilder =
                new TelephonyTimeZoneSuggestion.Builder(slotIndex);
        suggestionBuilder.addDebugInfo("findTimeZoneForTestNetwork: nitzSignal=" + nitzSignal);
        OffsetResult lookupResult =
                mTimeZoneLookupHelper.lookupByNitz(nitzData);
        if (lookupResult == null) {
            suggestionBuilder.addDebugInfo("findTimeZoneForTestNetwork: No zone found");
        } else {
            suggestionBuilder.setZoneId(lookupResult.getTimeZone().getID());
            suggestionBuilder.setMatchType(
                    TelephonyTimeZoneSuggestion.MATCH_TYPE_TEST_NETWORK_OFFSET_ONLY);
            int quality = lookupResult.isOnlyMatch()
                    ? TelephonyTimeZoneSuggestion.QUALITY_SINGLE_ZONE
                    : TelephonyTimeZoneSuggestion.QUALITY_MULTIPLE_ZONES_WITH_SAME_OFFSET;
            suggestionBuilder.setQuality(quality);
            suggestionBuilder.addDebugInfo(
                    "findTimeZoneForTestNetwork: lookupResult=" + lookupResult);
        }
        return suggestionBuilder.build();
    }

    /**
     * Creates a {@link TelephonyTimeZoneSuggestion} using network country code and NITZ.
     */
    @NonNull
    private TelephonyTimeZoneSuggestion findTimeZoneFromCountryAndNitz(
            int slotIndex, @NonNull String countryIsoCode,
            @NonNull NitzSignal nitzSignal) {
        Objects.requireNonNull(countryIsoCode);
        Objects.requireNonNull(nitzSignal);

        TelephonyTimeZoneSuggestion.Builder suggestionBuilder =
                new TelephonyTimeZoneSuggestion.Builder(slotIndex);
        suggestionBuilder.addDebugInfo("findTimeZoneFromCountryAndNitz:"
                + " countryIsoCode=" + countryIsoCode
                + ", nitzSignal=" + nitzSignal);
        NitzData nitzData = Objects.requireNonNull(nitzSignal.getNitzData());
        if (isNitzSignalOffsetInfoBogus(countryIsoCode, nitzData)) {
            suggestionBuilder.addDebugInfo(
                    "findTimeZoneFromCountryAndNitz: NITZ signal looks bogus");
            return suggestionBuilder.build();
        }

        // Try to find a match using both country + NITZ signal.
        OffsetResult lookupResult =
                mTimeZoneLookupHelper.lookupByNitzCountry(nitzData, countryIsoCode);
        if (lookupResult != null) {
            suggestionBuilder.setZoneId(lookupResult.getTimeZone().getID());
            suggestionBuilder.setMatchType(
                    TelephonyTimeZoneSuggestion.MATCH_TYPE_NETWORK_COUNTRY_AND_OFFSET);
            int quality = lookupResult.isOnlyMatch()
                    ? TelephonyTimeZoneSuggestion.QUALITY_SINGLE_ZONE
                    : TelephonyTimeZoneSuggestion.QUALITY_MULTIPLE_ZONES_WITH_SAME_OFFSET;
            suggestionBuilder.setQuality(quality);
            suggestionBuilder.addDebugInfo("findTimeZoneFromCountryAndNitz:"
                    + " lookupResult=" + lookupResult);
            return suggestionBuilder.build();
        }

        // The country + offset provided no match, so see if the country by itself would be enough.
        CountryResult countryResult = mTimeZoneLookupHelper.lookupByCountry(
                countryIsoCode, nitzData.getCurrentTimeInMillis());
        if (countryResult == null) {
            // Country not recognized.
            suggestionBuilder.addDebugInfo(
                    "findTimeZoneFromCountryAndNitz: lookupByCountry() country not recognized");
            return suggestionBuilder.build();
        }

        // If the country has a single zone, or it has multiple zones but the default zone is
        // "boosted" (i.e. the country default is considered a good suggestion in most cases) then
        // use it.
        if (countryResult.quality == CountryResult.QUALITY_SINGLE_ZONE
                || countryResult.quality == CountryResult.QUALITY_DEFAULT_BOOSTED) {
            suggestionBuilder.setZoneId(countryResult.zoneId);
            suggestionBuilder.setMatchType(
                    TelephonyTimeZoneSuggestion.MATCH_TYPE_NETWORK_COUNTRY_ONLY);
            suggestionBuilder.setQuality(TelephonyTimeZoneSuggestion.QUALITY_SINGLE_ZONE);
            suggestionBuilder.addDebugInfo(
                    "findTimeZoneFromCountryAndNitz: high quality country-only suggestion:"
                            + " countryResult=" + countryResult);
            return suggestionBuilder.build();
        }

        // Quality is not high enough to set the zone using country only.
        suggestionBuilder.addDebugInfo("findTimeZoneFromCountryAndNitz: country-only suggestion"
                + " quality not high enough. countryResult=" + countryResult);
        return suggestionBuilder.build();
    }

    /**
     * Creates a {@link TelephonyTimeZoneSuggestion} using only network country code; works well on
     * countries which only have one time zone or multiple zones with the same offset.
     *
     * @param countryIsoCode country code from network MCC
     * @param whenMillis the time to use when looking at time zone rules data
     */
    @NonNull
    private TelephonyTimeZoneSuggestion findTimeZoneFromNetworkCountryCode(
            int slotIndex, @NonNull String countryIsoCode, long whenMillis) {
        Objects.requireNonNull(countryIsoCode);
        if (TextUtils.isEmpty(countryIsoCode)) {
            throw new IllegalArgumentException("countryIsoCode must not be empty");
        }

        TelephonyTimeZoneSuggestion.Builder suggestionBuilder =
                new TelephonyTimeZoneSuggestion.Builder(slotIndex);
        suggestionBuilder.addDebugInfo("findTimeZoneFromNetworkCountryCode:"
                + " whenMillis=" + whenMillis + ", countryIsoCode=" + countryIsoCode);
        CountryResult lookupResult = mTimeZoneLookupHelper.lookupByCountry(
                countryIsoCode, whenMillis);
        if (lookupResult != null) {
            suggestionBuilder.setZoneId(lookupResult.zoneId);
            suggestionBuilder.setMatchType(
                    TelephonyTimeZoneSuggestion.MATCH_TYPE_NETWORK_COUNTRY_ONLY);

            int quality;
            if (lookupResult.quality == CountryResult.QUALITY_SINGLE_ZONE
                    || lookupResult.quality == CountryResult.QUALITY_DEFAULT_BOOSTED) {
                quality = TelephonyTimeZoneSuggestion.QUALITY_SINGLE_ZONE;
            } else if (lookupResult.quality == CountryResult.QUALITY_MULTIPLE_ZONES_SAME_OFFSET) {
                quality = TelephonyTimeZoneSuggestion.QUALITY_MULTIPLE_ZONES_WITH_SAME_OFFSET;
            } else if (lookupResult.quality
                    == CountryResult.QUALITY_MULTIPLE_ZONES_DIFFERENT_OFFSETS) {
                quality = TelephonyTimeZoneSuggestion.QUALITY_MULTIPLE_ZONES_WITH_DIFFERENT_OFFSETS;
            } else {
                // This should never happen.
                throw new IllegalArgumentException(
                        "lookupResult.quality not recognized: countryIsoCode=" + countryIsoCode
                                + ", whenMillis=" + whenMillis + ", lookupResult=" + lookupResult);
            }
            suggestionBuilder.setQuality(quality);
            suggestionBuilder.addDebugInfo(
                    "findTimeZoneFromNetworkCountryCode: lookupResult=" + lookupResult);
        } else {
            suggestionBuilder.addDebugInfo(
                    "findTimeZoneFromNetworkCountryCode: Country not recognized?");
        }
        return suggestionBuilder.build();
    }

    /**
     * Returns true if the NITZ signal is definitely bogus, assuming that the country is correct.
     */
    private boolean isNitzSignalOffsetInfoBogus(String countryIsoCode, NitzData nitzData) {
        if (TextUtils.isEmpty(countryIsoCode)) {
            // We cannot say for sure.
            return false;
        }

        boolean zeroOffsetNitz = nitzData.getLocalOffsetMillis() == 0;
        return zeroOffsetNitz && !countryUsesUtc(countryIsoCode, nitzData);
    }

    private boolean countryUsesUtc(String countryIsoCode, NitzData nitzData) {
        return mTimeZoneLookupHelper.countryUsesUtc(
                countryIsoCode, nitzData.getCurrentTimeInMillis());
    }
}
