/*
 * Copyright 2017 The Android Open Source Project
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

import static com.android.internal.telephony.nitz.TimeZoneLookupHelper.CountryResult.QUALITY_MULTIPLE_ZONES_DIFFERENT_OFFSETS;
import static com.android.internal.telephony.nitz.TimeZoneLookupHelper.CountryResult.QUALITY_MULTIPLE_ZONES_SAME_OFFSET;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.icu.util.TimeZone;
import android.text.TextUtils;
import android.timezone.CountryTimeZones;
import android.timezone.CountryTimeZones.OffsetResult;
import android.timezone.CountryTimeZones.TimeZoneMapping;
import android.timezone.TimeZoneFinder;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.NitzData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;

/**
 * An interface to various time zone lookup behaviors.
 */
@VisibleForTesting
public final class TimeZoneLookupHelper {

    /**
     * The result of looking up a time zone using country information.
     */
    @VisibleForTesting
    public static final class CountryResult {

        @IntDef({ QUALITY_SINGLE_ZONE, QUALITY_DEFAULT_BOOSTED, QUALITY_MULTIPLE_ZONES_SAME_OFFSET,
                QUALITY_MULTIPLE_ZONES_DIFFERENT_OFFSETS })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Quality {}

        public static final int QUALITY_SINGLE_ZONE = 1;
        public static final int QUALITY_DEFAULT_BOOSTED = 2;
        public static final int QUALITY_MULTIPLE_ZONES_SAME_OFFSET = 3;
        public static final int QUALITY_MULTIPLE_ZONES_DIFFERENT_OFFSETS = 4;

        /** A time zone to use for the country. */
        @NonNull
        public final String zoneId;

        /**
         * The quality of the match.
         */
        @Quality
        public final int quality;

        /**
         * Freeform information about why the value of {@link #quality} was chosen. Not used for
         * {@link #equals(Object)}.
         */
        private final String mDebugInfo;

        public CountryResult(@NonNull String zoneId, @Quality int quality, String debugInfo) {
            this.zoneId = Objects.requireNonNull(zoneId);
            this.quality = quality;
            mDebugInfo = debugInfo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CountryResult that = (CountryResult) o;
            return quality == that.quality
                    && zoneId.equals(that.zoneId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(zoneId, quality);
        }

        @Override
        public String toString() {
            return "CountryResult{"
                    + "zoneId='" + zoneId + '\''
                    + ", quality=" + quality
                    + ", mDebugInfo=" + mDebugInfo
                    + '}';
        }
    }

    /** The last CountryTimeZones object retrieved. */
    @Nullable
    private CountryTimeZones mLastCountryTimeZones;

    @VisibleForTesting
    public TimeZoneLookupHelper() {}

    /**
     * Looks for a time zone for the supplied NITZ and country information.
     *
     * <p><em>Note:</em> When there are multiple matching zones then one of the matching candidates
     * will be returned in the result. If the current device default zone matches it will be
     * returned in preference to other candidates. This method can return {@code null} if no
     * matching time zones are found.
     */
    @VisibleForTesting
    @Nullable
    public OffsetResult lookupByNitzCountry(
            @NonNull NitzData nitzData, @NonNull String isoCountryCode) {
        CountryTimeZones countryTimeZones = getCountryTimeZones(isoCountryCode);
        if (countryTimeZones == null) {
            return null;
        }
        TimeZone bias = TimeZone.getDefault();

        // Android NITZ time zone matching doesn't try to do a precise match using the DST offset
        // supplied by the carrier. It only considers whether or not the carrier suggests local time
        // is DST (if known). NITZ is limited in only being able to express DST offsets in whole
        // hours and the DST info is optional.
        Integer dstAdjustmentMillis = nitzData.getDstAdjustmentMillis();
        if (dstAdjustmentMillis == null) {
            return countryTimeZones.lookupByOffsetWithBias(
                    nitzData.getCurrentTimeInMillis(), bias, nitzData.getLocalOffsetMillis());

        } else {
            // We don't try to match the exact DST offset given, we just use it to work out if
            // the country is in DST.
            boolean isDst = dstAdjustmentMillis != 0;
            return countryTimeZones.lookupByOffsetWithBias(
                    nitzData.getCurrentTimeInMillis(), bias,
                    nitzData.getLocalOffsetMillis(), isDst);
        }
    }

    /**
     * Looks for a time zone using only information present in the supplied {@link NitzData} object.
     *
     * <p><em>Note:</em> Because multiple time zones can have the same offset / DST state at a given
     * time this process is error prone; an arbitrary match is returned when there are multiple
     * candidates. The algorithm can also return a non-exact match by assuming that the DST
     * information provided by NITZ is incorrect. This method can return {@code null} if no matching
     * time zones are found.
     */
    @VisibleForTesting
    @Nullable
    public OffsetResult lookupByNitz(@NonNull NitzData nitzData) {
        int utcOffsetMillis = nitzData.getLocalOffsetMillis();
        long timeMillis = nitzData.getCurrentTimeInMillis();

        // Android NITZ time zone matching doesn't try to do a precise match using the DST offset
        // supplied by the carrier. It only considers whether or not the carrier suggests local time
        // is DST (if known). NITZ is limited in only being able to express DST offsets in whole
        // hours and the DST info is optional.
        Integer dstAdjustmentMillis = nitzData.getDstAdjustmentMillis();
        Boolean isDst = dstAdjustmentMillis == null ? null : dstAdjustmentMillis != 0;

        OffsetResult match = lookupByInstantOffsetDst(timeMillis, utcOffsetMillis, isDst);
        if (match == null && isDst != null) {
            // This branch is extremely unlikely and could probably be removed. The match above will
            // have searched the entire tzdb for a zone with the same total offset and isDst state.
            // Here we try another match but use "null" for isDst to indicate that only the total
            // offset should be considered. If, by the end of this, there isn't a match then the
            // current offset suggested by the carrier must be highly unusual.
            match = lookupByInstantOffsetDst(timeMillis, utcOffsetMillis, null /* isDst */);
        }
        return match;
    }

    /**
     * Returns information about the time zones used in a country at a given time.
     *
     * {@code null} can be returned if a problem occurs during lookup, e.g. if the country code is
     * unrecognized, if the country is uninhabited, or if there is a problem with the data.
     */
    @VisibleForTesting
    @Nullable
    public CountryResult lookupByCountry(@NonNull String isoCountryCode, long whenMillis) {
        CountryTimeZones countryTimeZones = getCountryTimeZones(isoCountryCode);
        if (countryTimeZones == null) {
            // Unknown country code.
            return null;
        }
        TimeZone countryDefaultZone = countryTimeZones.getDefaultTimeZone();
        if (countryDefaultZone == null) {
            // This is not expected: the country default should have been validated before.
            return null;
        }

        String debugInfo;
        int matchQuality;
        if (countryTimeZones.isDefaultTimeZoneBoosted()) {
            matchQuality = CountryResult.QUALITY_DEFAULT_BOOSTED;
            debugInfo = "Country default is boosted";
        } else {
            List<TimeZoneMapping> effectiveTimeZoneMappings =
                    countryTimeZones.getEffectiveTimeZoneMappingsAt(whenMillis);
            if (effectiveTimeZoneMappings.isEmpty()) {
                // This should never happen unless there's been an error loading the data.
                // Treat it the same as a low quality answer.
                matchQuality = QUALITY_MULTIPLE_ZONES_DIFFERENT_OFFSETS;
                debugInfo = "No effective time zones found at whenMillis=" + whenMillis;
            } else if (effectiveTimeZoneMappings.size() == 1) {
                // The default is the only zone so it's a good candidate.
                matchQuality = CountryResult.QUALITY_SINGLE_ZONE;
                debugInfo = "One effective time zone found at whenMillis=" + whenMillis;
            } else {
                boolean countryUsesDifferentOffsets = countryUsesDifferentOffsets(
                        whenMillis, effectiveTimeZoneMappings, countryDefaultZone);
                matchQuality = countryUsesDifferentOffsets
                        ? QUALITY_MULTIPLE_ZONES_DIFFERENT_OFFSETS
                        : QUALITY_MULTIPLE_ZONES_SAME_OFFSET;
                debugInfo = "countryUsesDifferentOffsets=" + countryUsesDifferentOffsets + " at"
                        + " whenMillis=" + whenMillis;
            }
        }
        return new CountryResult(countryDefaultZone.getID(), matchQuality, debugInfo);
    }

    private static boolean countryUsesDifferentOffsets(
            long whenMillis, @NonNull List<TimeZoneMapping> effectiveTimeZoneMappings,
            @NonNull TimeZone countryDefaultZone) {
        String countryDefaultId = countryDefaultZone.getID();
        int countryDefaultOffset = countryDefaultZone.getOffset(whenMillis);
        for (TimeZoneMapping timeZoneMapping : effectiveTimeZoneMappings) {
            if (timeZoneMapping.getTimeZoneId().equals(countryDefaultId)) {
                continue;
            }

            TimeZone timeZone = timeZoneMapping.getTimeZone();
            int candidateOffset = timeZone.getOffset(whenMillis);
            if (countryDefaultOffset != candidateOffset) {
                return true;
            }
        }
        return false;
    }

    private static OffsetResult lookupByInstantOffsetDst(long timeMillis, int utcOffsetMillis,
            @Nullable Boolean isDst) {

        // Use java.util.TimeZone and not android.icu.util.TimeZone to find candidate zone IDs: ICU
        // references some non-standard zone IDs that can be rejected by java.util.TimeZone. There
        // is a CTS test (in com.android.i18n.test.timezone.TimeZoneIntegrationTest) that confirms
        // that ICU can interpret all IDs that are known to java.util.TimeZone.
        String[] zones = java.util.TimeZone.getAvailableIDs();
        TimeZone match = null;
        boolean isOnlyMatch = true;
        for (String zone : zones) {
            TimeZone tz = TimeZone.getFrozenTimeZone(zone);
            if (offsetMatchesAtTime(tz, utcOffsetMillis, isDst, timeMillis)) {
                if (match == null) {
                    match = tz;
                } else {
                    isOnlyMatch = false;
                    break;
                }
            }
        }

        if (match == null) {
            return null;
        }
        return new OffsetResult(match, isOnlyMatch);
    }

    /**
     * Returns {@code true} if the specified {@code totalOffset} and {@code isDst} would be valid in
     * the {@code timeZone} at time {@code whenMillis}. {@code totalOffetMillis} is always matched.
     * If {@code isDst} is {@code null} this means the DST state is unknown so DST state is ignored.
     * If {@code isDst} is not {@code null} then it is also matched.
     */
    private static boolean offsetMatchesAtTime(@NonNull TimeZone timeZone, int totalOffsetMillis,
            @Nullable Boolean isDst, long whenMillis) {
        int[] offsets = new int[2];
        timeZone.getOffset(whenMillis, false /* local */, offsets);

        if (totalOffsetMillis != (offsets[0] + offsets[1])) {
            return false;
        }

        return isDst == null || isDst == (offsets[1] != 0);
    }

    /**
     * Returns {@code true} if the supplied (lower-case) ISO country code is for a country known to
     * use a raw offset of zero from UTC at the time specified.
     */
    @VisibleForTesting
    public boolean countryUsesUtc(@NonNull String isoCountryCode, long whenMillis) {
        if (TextUtils.isEmpty(isoCountryCode)) {
            return false;
        }

        CountryTimeZones countryTimeZones = getCountryTimeZones(isoCountryCode);
        return countryTimeZones != null && countryTimeZones.hasUtcZone(whenMillis);
    }

    @Nullable
    private CountryTimeZones getCountryTimeZones(@NonNull String isoCountryCode) {
        Objects.requireNonNull(isoCountryCode);

        // A single entry cache of the last CountryTimeZones object retrieved since there should
        // be strong consistency across calls.
        synchronized (this) {
            if (mLastCountryTimeZones != null) {
                if (mLastCountryTimeZones.matchesCountryCode(isoCountryCode)) {
                    return mLastCountryTimeZones;
                }
            }

            // Perform the lookup. It's very unlikely to return null, but we won't cache null.
            CountryTimeZones countryTimeZones =
                    TimeZoneFinder.getInstance().lookupCountryTimeZones(isoCountryCode);
            if (countryTimeZones != null) {
                mLastCountryTimeZones = countryTimeZones;
            }
            return countryTimeZones;
        }
    }
}
