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

import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledAfter;
import android.compat.annotation.UnsupportedAppUsage;
import android.icu.text.DateFormatSymbols;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;

import dalvik.system.VMRuntime;
import sun.util.locale.provider.CalendarDataUtility;

import java.util.HashMap;
import java.util.Locale;
import libcore.util.Objects;

/**
 * Passes locale-specific from ICU native code to Java.
 * <p>
 * Note that you share these; you must not alter any of the fields, nor their array elements
 * in the case of arrays. If you ever expose any of these things to user code, you must give
 * them a clone rather than the original.
 * @hide
 */
public final class LocaleData {

    /**
     * @see #USE_REAL_ROOT_LOCALE
     */
    private static final Locale LOCALE_EN_US_POSIX = new Locale("en", "US", "POSIX");


    // In Android Q or before, when this class tries to load {@link Locale#ROOT} data, en_US_POSIX
    // locale data is incorrectly loaded due to a bug b/159514442 (public bug b/159047832).
    //
    // This class used to pass "und" string as BCP47 language tag to our jni code, which then
    // passes the string as as ICU Locale ID to ICU4C. ICU4C 63 or older version doesn't recognize
    // "und" as a valid locale id, and fallback the default locale. The default locale is
    // normally selected in the Locale picker in the Settings app by the user and set via
    // frameworks. But this class statically cached the ROOT locale data before the
    // default locale being set by framework, and without initialization, ICU4C uses en_US_POSIX
    // as default locale. Thus, in Q or before, en_US_POSIX data is loaded.
    //
    // ICU version 64.1 resolved inconsistent behavior of
    // "root", "und" and "" (empty) Locale ID which libcore previously relied on, and they are
    // recognized correctly as {@link Locale#ROOT} since Android R. This ChangeId gated the change,
    // and fallback to the old behavior by checking targetSdkVersion version.
    //
    // The below javadoc is shown in http://developer.android.com for consumption by app developers.
    /**
     * Since Android 11, formatter classes, e.g. java.text.SimpleDateFormat, no longer
     * provide English data when Locale.ROOT format is requested. Please use
     * Locale.ENGLISH to format in English.
     *
     * Note that Locale.ROOT is used as language/country neutral locale or fallback locale,
     * and does not guarantee to represent English locale.
     *
     * This flag is only for documentation and can't be overridden by app. Please use
     * {@code targetSdkVersion} to enable the new behavior.
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion=29 /* Android Q */)
    public static final long USE_REAL_ROOT_LOCALE = 159047832L;

    // TODO(http://b/217881004): Replace this with a LRU cache.
    // A cache for the locale-specific data.
    private static final HashMap<String, LocaleData> localeDataCache = new HashMap<String, LocaleData>();
    static {
        // Ensure that we pull in the locale data for the root locale, en_US, and the
        // user's default locale. (All devices must support the root locale and en_US,
        // and they're used for various system things like HTTP headers.) Pre-populating
        // the cache is especially useful on Android because we'll share this via the Zygote.
        get(Locale.ROOT);
        get(Locale.US);
        get(Locale.getDefault());
    }

    // Used by Calendar.
    @UnsupportedAppUsage
    public Integer firstDayOfWeek;
    @UnsupportedAppUsage
    public Integer minimalDaysInFirstWeek;

    // Used by DateFormatSymbols.
    public String[] amPm; // "AM", "PM".
    public String[] eras; // "BC", "AD".

    public String[] longMonthNames; // "January", ...
    @UnsupportedAppUsage
    public String[] shortMonthNames; // "Jan", ...
    public String[] tinyMonthNames; // "J", ...
    public String[] longStandAloneMonthNames; // "January", ...
    @UnsupportedAppUsage
    public String[] shortStandAloneMonthNames; // "Jan", ...
    public String[] tinyStandAloneMonthNames; // "J", ...

    public String[] longWeekdayNames; // "Sunday", ...
    public String[] shortWeekdayNames; // "Sun", ...
    public String[] tinyWeekdayNames; // "S", ...
    @UnsupportedAppUsage
    public String[] longStandAloneWeekdayNames; // "Sunday", ...
    @UnsupportedAppUsage
    public String[] shortStandAloneWeekdayNames; // "Sun", ...
    public String[] tinyStandAloneWeekdayNames; // "S", ...

    // zeroDigit, today and tomorrow is only kept for @UnsupportedAppUsage.
    // Their value is hard-coded, not localized.
    @UnsupportedAppUsage
    public String today; // "Today".
    @UnsupportedAppUsage
    public String tomorrow; // "Tomorrow".
    @UnsupportedAppUsage
    public char zeroDigit; // '0'

    // timeFormat_hm and timeFormat_Hm are only kept for @UnsupportedAppUsage.
    // Their value is hard-coded, not localized.
    @UnsupportedAppUsage
    public String timeFormat_hm;
    @UnsupportedAppUsage
    public String timeFormat_Hm;

    private LocaleData() {
        today = "Today";
        tomorrow = "Tomorrow";
        zeroDigit = '0';
        timeFormat_hm = "h:mm a";
        timeFormat_Hm = "HH:mm";
    }

    @UnsupportedAppUsage
    public static Locale mapInvalidAndNullLocales(Locale locale) {
        if (locale == null) {
            return Locale.getDefault();
        }

        if ("und".equals(locale.toLanguageTag())) {
            return Locale.ROOT;
        }

        return locale;
    }

    /**
     * Normally, this utility function is used by secondary cache above {@link LocaleData},
     * because the cache needs a correct key.
     * @see #USE_REAL_ROOT_LOCALE
     * @return a compatible locale for the bug b/159514442
     */
    public static Locale getCompatibleLocaleForBug159514442(Locale locale) {
        if (Locale.ROOT.equals(locale)) {
            int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
            // Don't use Compatibility.isChangeEnabled(USE_REAL_ROOT_LOCALE) because the app compat
            // framework lives in libcore and can depend on this class via various format methods,
            // e.g. String.format(). See b/160912695.
            if (targetSdkVersion <= 29 /* Android Q */) {
                locale = LOCALE_EN_US_POSIX;
            }
        }
        return locale;
    }

    /**
     * Returns a shared LocaleData for the given locale.
     */
    @UnsupportedAppUsage
    public static LocaleData get(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }

        locale = getCompatibleLocaleForBug159514442(locale);

        final String languageTag = locale.toLanguageTag();
        synchronized (localeDataCache) {
            LocaleData localeData = localeDataCache.get(languageTag);
            if (localeData != null) {
                return localeData;
            }
        }
        LocaleData newLocaleData = initLocaleData(locale);
        synchronized (localeDataCache) {
            LocaleData localeData = localeDataCache.get(languageTag);
            if (localeData != null) {
                return localeData;
            }
            localeDataCache.put(languageTag, newLocaleData);
            return newLocaleData;
        }
    }

    @Override public String toString() {
        return Objects.toString(this);
    }

    /*
     * This method is made public for testing
     */
    public static LocaleData initLocaleData(Locale locale) {
        LocaleData localeData = new LocaleData();

        localeData.initializeDateFormatData(locale);
        localeData.initializeCalendarData(locale);

        return localeData;
    }

    private void initializeDateFormatData(Locale locale) {
        DateFormatSymbols dfs = new DateFormatSymbols(GregorianCalendar.class, locale);

        longMonthNames = dfs.getMonths(DateFormatSymbols.FORMAT, DateFormatSymbols.WIDE);
        shortMonthNames = dfs.getMonths(DateFormatSymbols.FORMAT, DateFormatSymbols.ABBREVIATED);
        tinyMonthNames = dfs.getMonths(DateFormatSymbols.FORMAT, DateFormatSymbols.NARROW);
        longWeekdayNames = dfs.getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.WIDE);
        shortWeekdayNames = dfs
            .getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.ABBREVIATED);
        tinyWeekdayNames = dfs.getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.NARROW);

        longStandAloneMonthNames = dfs
            .getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE);
        shortStandAloneMonthNames = dfs
            .getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.ABBREVIATED);
        tinyStandAloneMonthNames = dfs
            .getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.NARROW);
        longStandAloneWeekdayNames = dfs
            .getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE);
        shortStandAloneWeekdayNames = dfs
            .getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.ABBREVIATED);
        tinyStandAloneWeekdayNames = dfs
            .getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.NARROW);

        amPm = dfs.getAmPmStrings();
        eras = dfs.getEras();

    }

    private void initializeCalendarData(Locale locale) {
        Calendar calendar = Calendar.getInstance(locale);

        firstDayOfWeek = CalendarDataUtility.retrieveFirstDayOfWeek(locale,
                calendar.getFirstDayOfWeek());
        minimalDaysInFirstWeek = calendar.getMinimalDaysInFirstWeek();
    }
}
