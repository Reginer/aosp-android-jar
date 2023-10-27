/*
 * Copyright (C) 2009 The Android Open Source Project
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

package libcore.icu;

import com.android.icu.util.ExtendedCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import sun.util.locale.LanguageTag;

/**
 * Pattern cache for {@link SimpleDateFormat}
 *
 * @hide
 */
public class SimpleDateFormatData {

    // TODO(http://b/217881004): Replace this with a LRU cache.
    private static final ConcurrentHashMap<String, SimpleDateFormatData> CACHE =
            new ConcurrentHashMap<>(/* initialCapacity */ 3);


    private final Locale locale;
    /** See {@link #isBug266731719Locale} for details */
    private final boolean usesAsciiSpace;

    private final String fullTimeFormat;
    private final String longTimeFormat;
    private final String mediumTimeFormat;
    private final String shortTimeFormat;

    private final String fullDateFormat;
    private final String longDateFormat;
    private final String mediumDateFormat;
    private final String shortDateFormat;

    private SimpleDateFormatData(Locale locale) {
        this.locale = locale;
        this.usesAsciiSpace = isBug266731719Locale(locale);
        // libcore's java.text supports Gregorian calendar only.
        ExtendedCalendar calendar = ICU.getExtendedCalendar(locale, "gregorian");

        String tmpFullTimeFormat = getDateTimePattern(calendar,
                android.icu.text.DateFormat.NONE, android.icu.text.DateFormat.FULL);

        // Fix up a couple of patterns.
        if (tmpFullTimeFormat != null) {
            // There are some full time format patterns in ICU that use the pattern character 'v'.
            // Java doesn't accept this, so we replace it with 'z' which has about the same result
            // as 'v', the timezone name.
            // 'v' -> "PT", 'z' -> "PST", v is the generic timezone and z the standard tz
            // "vvvv" -> "Pacific Time", "zzzz" -> "Pacific Standard Time"
            tmpFullTimeFormat = tmpFullTimeFormat.replace('v', 'z');
        }
        fullTimeFormat = tmpFullTimeFormat;

        longTimeFormat = getDateTimePattern(calendar,
                android.icu.text.DateFormat.NONE, android.icu.text.DateFormat.LONG);
        mediumTimeFormat = getDateTimePattern(calendar,
                android.icu.text.DateFormat.NONE, android.icu.text.DateFormat.MEDIUM);
        shortTimeFormat = getDateTimePattern(calendar,
                android.icu.text.DateFormat.NONE, android.icu.text.DateFormat.SHORT);
        fullDateFormat = getDateTimePattern(calendar,
                android.icu.text.DateFormat.FULL, android.icu.text.DateFormat.NONE);
        longDateFormat = getDateTimePattern(calendar,
                android.icu.text.DateFormat.LONG, android.icu.text.DateFormat.NONE);
        mediumDateFormat = getDateTimePattern(calendar,
                android.icu.text.DateFormat.MEDIUM, android.icu.text.DateFormat.NONE);
        shortDateFormat = getDateTimePattern(calendar,
                android.icu.text.DateFormat.SHORT, android.icu.text.DateFormat.NONE);
    }

    /**
     * Returns an instance.
     *
     * @param locale can't be null
     * @throws NullPointerException if {@code locale} is null
     * @return a {@link SimpleDateFormatData} instance
     */
    public static SimpleDateFormatData getInstance(Locale locale) {
        Objects.requireNonNull(locale, "locale can't be null");

        locale = LocaleData.getCompatibleLocaleForBug159514442(locale);

        final String languageTag = locale.toLanguageTag();

        SimpleDateFormatData data = CACHE.get(languageTag);
        if (data != null) {
            return data;
        }

        data = new SimpleDateFormatData(locale);
        SimpleDateFormatData prev = CACHE.putIfAbsent(languageTag, data);
        if (prev != null) {
            return prev;
        }
        return data;
    }

    /**
     * Ensure that we pull in the locale data for the root locale, en_US, and the user's default
     * locale. All devices must support the root locale and en_US, and they're used for various
     * system things. Pre-populating the cache is especially useful on Android because
     * we'll share this via the Zygote.
     */
    public static void initializeCacheInZygote() {
        getInstance(Locale.ROOT);
        getInstance(Locale.US);
        getInstance(Locale.getDefault());
    }

    /**
     * @throws AssertionError if style is not one of the 4 styles specified in {@link DateFormat}
     * @return a date pattern string
     */
    public String getDateFormat(int style) {
        switch (style) {
            case DateFormat.SHORT:
                return shortDateFormat;
            case DateFormat.MEDIUM:
                return mediumDateFormat;
            case DateFormat.LONG:
                return longDateFormat;
            case DateFormat.FULL:
                return fullDateFormat;
        }
        // TODO: fix this legacy behavior of throwing AssertionError introduced in
        //  the commit 6ca85c4.
        throw new AssertionError();
    }

    /**
     * @throws AssertionError if style is not one of the 4 styles specified in {@link DateFormat}
     * @return a time pattern string
     */
    public String getTimeFormat(int style) {
        // Do not cache ICU.getTimePattern() return value in the LocaleData instance
        // because most users do not enable this setting, hurts performance in critical path,
        // e.g. b/161846393, and ICU.getBestDateTimePattern will cache it in  ICU.CACHED_PATTERNS
        // on demand.
        switch (style) {
            case DateFormat.SHORT:
                if (DateFormat.is24Hour == null) {
                    return shortTimeFormat;
                } else {
                    return getTimePattern(DateFormat.is24Hour, false);
                }
            case DateFormat.MEDIUM:
                if (DateFormat.is24Hour == null) {
                    return mediumTimeFormat;
                } else {
                    return getTimePattern(DateFormat.is24Hour, true);
                }
            case DateFormat.LONG:
                // CLDR doesn't really have anything we can use to obey the 12-/24-hour preference.
                return longTimeFormat;
            case DateFormat.FULL:
                // CLDR doesn't really have anything we can use to obey the 12-/24-hour preference.
                return fullTimeFormat;
        }
        // TODO: fix this legacy behavior of throwing AssertionError introduced in
        //  the commit 6ca85c4.
        throw new AssertionError();
    }

    /**
     * Returns the date and/or time pattern.
     *
     * @param dateStyle {@link android.icu.text.DateFormat} date style
     * @param timeStyle {@link android.icu.text.DateFormat} time style
     */
    private String getDateTimePattern(ExtendedCalendar calendar, int dateStyle, int timeStyle) {
        String pattern = ICU.transformIcuDateTimePattern_forJavaText(
                calendar.getDateTimePattern(dateStyle, timeStyle));

        return postProcessPattern(pattern);
    }

    private String getTimePattern(boolean is24Hour, boolean withSecond) {
        String pattern = ICU.getTimePattern(locale, is24Hour, withSecond);

        return postProcessPattern(pattern);
    }

    private String postProcessPattern(String pattern) {
        if (pattern == null || !usesAsciiSpace) {
            return pattern;
        }

        return pattern.replace('\u202f', ' ');
    }

    /**
     * Returns {@code true} if the locale is "en" or "en-US" or "en-US-*" or
     * "en-<unknown_region>-*"
     *
     * The first 2 locales, i.e. {@link Locale#ENGLISH} and {@link Locale#US}, are commonly
     * hard-coded by developers for serialization and deserialization as shown in
     * the bug http://b/266731719. The date time formats in these locales are basically frozen
     * because they should be both backward and forward-compatible.
     *
     * We change both formatting and parsing behavior because the serialized string could be
     * parsed via a network request and thus breaking Android apps. We could consider changing
     * the formatted date / time, but the parser has to be compatible with both the old and new
     * formatted string.
     *
     * This method returns true for "en-US-*" and "en-<unknown_region>-*" because the behavior
     * should be consistent with the locale "en-US" and "en". The other English locales are not
     * expected to be stable in this bug.
     */
    private static boolean isBug266731719Locale(Locale locale) {
        if (locale == null) {
            return false;
        }

        String language = locale.getLanguage();
        if (language == null) {
            return false;
        }
        // Use LanguageTag.canonicalizeLanguage(s) instead of String.toUpperCase(s) to avoid
        // non-ASCII character conversion.
        language = LanguageTag.canonicalizeLanguage(language);
        if (!("en".equals(language))) {
            return false;
        }

        String region = locale.getCountry();
        if (region == null || region.isEmpty()) {
            return true;
        }
        region = LanguageTag.canonicalizeRegion(region);
        return "US".equals(region);
    }
}
