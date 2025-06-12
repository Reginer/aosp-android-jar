/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.text.format;

import android.annotation.NonNull;
import android.app.compat.CompatChanges;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledSince;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.icu.text.DateFormatSymbols;
import android.icu.text.DateTimePatternGenerator;
import android.icu.util.ULocale;
import android.os.Build;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class for producing strings with formatted date/time.
 *
 * <p>Most callers should avoid supplying their own format strings to this
 * class' {@code format} methods and rely on the correctly localized ones
 * supplied by the system. This class' factory methods return
 * appropriately-localized {@link java.text.DateFormat} instances, suitable
 * for both formatting and parsing dates. For the canonical documentation
 * of format strings, see {@link java.text.SimpleDateFormat}.
 *
 * <p>In cases where the system does not provide a suitable pattern,
 * this class offers the {@link #getBestDateTimePattern} method.
 *
 * <p>The {@code format} methods in this class implement a subset of Unicode
 * <a href="http://www.unicode.org/reports/tr35/#Date_Format_Patterns">UTS #35</a> patterns.
 * The subset currently supported by this class includes the following format characters:
 * {@code acdEHhLKkLMmsyz}. Up to API level 17, only {@code adEhkMmszy} were supported.
 * Note that this class incorrectly implements {@code k} as if it were {@code H} for backwards
 * compatibility.
 *
 * <p>See {@link java.text.SimpleDateFormat} for more documentation
 * about patterns, or if you need a more complete or correct implementation.
 * Note that the non-{@code format} methods in this class are implemented by
 * {@code SimpleDateFormat}.
 */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public class DateFormat {
    /**
     * @deprecated Use a literal {@code '} instead.
     * @removed
     */
    @Deprecated
    public  static final char    QUOTE                  =    '\'';

    /**
     * @deprecated Use a literal {@code 'a'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    AM_PM                  =    'a';

    /**
     * @deprecated Use a literal {@code 'a'} instead; 'A' was always equivalent to 'a'.
     * @removed
     */
    @Deprecated
    public  static final char    CAPITAL_AM_PM          =    'A';

    /**
     * @deprecated Use a literal {@code 'd'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    DATE                   =    'd';

    /**
     * @deprecated Use a literal {@code 'E'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    DAY                    =    'E';

    /**
     * @deprecated Use a literal {@code 'h'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    HOUR                   =    'h';

    /**
     * @deprecated Use a literal {@code 'H'} (for compatibility with {@link SimpleDateFormat}
     * and Unicode) or {@code 'k'} (for compatibility with Android releases up to and including
     * Jelly Bean MR-1) instead. Note that the two are incompatible.
     *
     * @removed
     */
    @Deprecated
    public  static final char    HOUR_OF_DAY            =    'k';

    /**
     * @deprecated Use a literal {@code 'm'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    MINUTE                 =    'm';

    /**
     * @deprecated Use a literal {@code 'M'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    MONTH                  =    'M';

    /**
     * @deprecated Use a literal {@code 'L'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    STANDALONE_MONTH       =    'L';

    /**
     * @deprecated Use a literal {@code 's'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    SECONDS                =    's';

    /**
     * @deprecated Use a literal {@code 'z'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    TIME_ZONE              =    'z';

    /**
     * @deprecated Use a literal {@code 'y'} instead.
     * @removed
     */
    @Deprecated
    public  static final char    YEAR                   =    'y';


    private static final Object sLocaleLock = new Object();
    private static Locale sIs24HourLocale;
    private static boolean sIs24Hour;

    /**
     * {@link #getBestDateTimePattern(Locale, String)} does not allow non-consecutive repeated
     * symbol in the skeleton. For example, please use a skeleton of {@code "jmm"} or
     * {@code "hmma"} instead of {@code "ahmma"} or {@code "jmma"}, because the field 'j' could
     * mean using 12-hour in some locales and, in this case, is duplicated as the 'a' field.
     */
    @ChangeId
    @EnabledSince(targetSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    static final long DISALLOW_DUPLICATE_FIELD_IN_SKELETON = 170233598L;

    /**
     * Returns true if times should be formatted as 24 hour times, false if times should be
     * formatted as 12 hour (AM/PM) times. Based on the user's chosen locale and other preferences.
     * @param context the context to use for the content resolver
     * @return true if 24 hour time format is selected, false otherwise.
     */
    public static boolean is24HourFormat(Context context) {
        return is24HourFormat(context, context.getUserId());
    }

    /**
     * Returns true if times should be formatted as 24 hour times, false if times should be
     * formatted as 12 hour (AM/PM) times. Based on the user's chosen locale and other preferences.
     * @param context the context to use for the content resolver
     * @param userHandle the user handle of the user to query.
     * @return true if 24 hour time format is selected, false otherwise.
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static boolean is24HourFormat(Context context, int userHandle) {
        final String value = Settings.System.getStringForUser(context.getContentResolver(),
                Settings.System.TIME_12_24, userHandle);
        if (value != null) {
            return value.equals("24");
        }

        return is24HourLocale(context.getResources().getConfiguration().locale);
    }

    /**
     * Returns true if the specified locale uses a 24-hour time format by default, ignoring user
     * settings.
     * @param locale the locale to check
     * @return true if the locale uses a 24 hour time format by default, false otherwise
     * @hide
     */
    public static boolean is24HourLocale(@NonNull Locale locale) {
        synchronized (sLocaleLock) {
            if (sIs24HourLocale != null && sIs24HourLocale.equals(locale)) {
                return sIs24Hour;
            }
        }

        final java.text.DateFormat natural =
                java.text.DateFormat.getTimeInstance(java.text.DateFormat.LONG, locale);

        final boolean is24Hour;
        if (natural instanceof SimpleDateFormat) {
            final SimpleDateFormat sdf = (SimpleDateFormat) natural;
            final String pattern = sdf.toPattern();
            is24Hour = hasDesignator(pattern, 'H');
        } else {
            is24Hour = false;
        }

        synchronized (sLocaleLock) {
            sIs24HourLocale = locale;
            sIs24Hour = is24Hour;
        }

        return is24Hour;
    }

    /**
     * Returns the best possible localized form of the given skeleton for the given
     * locale. A skeleton is similar to, and uses the same format characters as, a Unicode
     * <a href="http://www.unicode.org/reports/tr35/#Date_Format_Patterns">UTS #35</a>
     * pattern.
     *
     * <p>One difference is that order is irrelevant. For example, "MMMMd" will return
     * "MMMM d" in the {@code en_US} locale, but "d. MMMM" in the {@code de_CH} locale.
     *
     * <p>Note also in that second example that the necessary punctuation for German was
     * added. For the same input in {@code es_ES}, we'd have even more extra text:
     * "d 'de' MMMM".
     *
     * <p>This method will automatically correct for grammatical necessity. Given the
     * same "MMMMd" input, this method will return "d LLLL" in the {@code fa_IR} locale,
     * where stand-alone months are necessary. Lengths are preserved where meaningful,
     * so "Md" would give a different result to "MMMd", say, except in a locale such as
     * {@code ja_JP} where there is only one length of month.
     *
     * <p>This method will only return patterns that are in CLDR, and is useful whenever
     * you know what elements you want in your format string but don't want to make your
     * code specific to any one locale.
     *
     * @param locale the locale into which the skeleton should be localized
     * @param skeleton a skeleton as described above
     * @return a string pattern suitable for use with {@link java.text.SimpleDateFormat}.
     */
    public static String getBestDateTimePattern(Locale locale, String skeleton) {
        ULocale uLocale = ULocale.forLocale(locale);
        DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance(uLocale);
        boolean allowDuplicateFields = !CompatChanges.isChangeEnabled(
                DISALLOW_DUPLICATE_FIELD_IN_SKELETON);
        String pattern = dtpg.getBestPattern(skeleton, DateTimePatternGenerator.MATCH_NO_OPTIONS,
                allowDuplicateFields);
        return getCompatibleEnglishPattern(uLocale, pattern);
    }

    /**
     * Returns a {@link java.text.DateFormat} object that can format the time according
     * to the context's locale and the user's 12-/24-hour clock preference.
     * @param context the application context
     * @return the {@link java.text.DateFormat} object that properly formats the time.
     */
    public static java.text.DateFormat getTimeFormat(Context context) {
        final Locale locale = context.getResources().getConfiguration().locale;
        return new java.text.SimpleDateFormat(getTimeFormatString(context), locale);
    }

    /**
     * Returns a String pattern that can be used to format the time according
     * to the context's locale and the user's 12-/24-hour clock preference.
     * @param context the application context
     * @hide
     */
    @UnsupportedAppUsage
    public static String getTimeFormatString(Context context) {
        return getTimeFormatString(context, context.getUserId());
    }

    /**
     * Returns a String pattern that can be used to format the time according
     * to the context's locale and the user's 12-/24-hour clock preference.
     * @param context the application context
     * @param userHandle the user handle of the user to query the format for
     * @hide
     */
    @UnsupportedAppUsage
    public static String getTimeFormatString(Context context, int userHandle) {
        ULocale uLocale = ULocale.forLocale(context.getResources().getConfiguration().locale);
        DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance(uLocale);
        String pattern = is24HourFormat(context, userHandle) ? dtpg.getBestPattern("Hm")
            : dtpg.getBestPattern("hm");
        return getCompatibleEnglishPattern(uLocale, pattern);
    }

    /**
     * Returns a {@link java.text.DateFormat} object that can format the date
     * in short form according to the context's locale.
     *
     * @param context the application context
     * @return the {@link java.text.DateFormat} object that properly formats the date.
     */
    public static java.text.DateFormat getDateFormat(Context context) {
        final Locale locale = context.getResources().getConfiguration().locale;
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale);
    }

    /**
     * Returns a {@link java.text.DateFormat} object that can format the date
     * in long form (such as {@code Monday, January 3, 2000}) for the context's locale.
     * @param context the application context
     * @return the {@link java.text.DateFormat} object that formats the date in long form.
     */
    public static java.text.DateFormat getLongDateFormat(Context context) {
        final Locale locale = context.getResources().getConfiguration().locale;
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, locale);
    }

    /**
     * Returns a {@link java.text.DateFormat} object that can format the date
     * in medium form (such as {@code Jan 3, 2000}) for the context's locale.
     * @param context the application context
     * @return the {@link java.text.DateFormat} object that formats the date in long form.
     */
    public static java.text.DateFormat getMediumDateFormat(Context context) {
        final Locale locale = context.getResources().getConfiguration().locale;
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale);
    }

    /**
     * Gets the current date format stored as a char array. Returns a 3 element
     * array containing the day ({@code 'd'}), month ({@code 'M'}), and year ({@code 'y'}))
     * in the order specified by the user's format preference.  Note that this order is
     * <i>only</i> appropriate for all-numeric dates; spelled-out (MEDIUM and LONG)
     * dates will generally contain other punctuation, spaces, or words,
     * not just the day, month, and year, and not necessarily in the same
     * order returned here.
     */
    public static char[] getDateFormatOrder(Context context) {
        return getDateFormatOrder(getDateFormatString(context));
    }

    /**
     * @hide Used by internal framework class {@link android.widget.DatePickerSpinnerDelegate}.
     */
    public static char[] getDateFormatOrder(String pattern) {
        char[] result = new char[3];
        int resultIndex = 0;
        boolean sawDay = false;
        boolean sawMonth = false;
        boolean sawYear = false;

        for (int i = 0; i < pattern.length(); ++i) {
            char ch = pattern.charAt(i);
            if (ch == 'd' || ch == 'L' || ch == 'M' || ch == 'y') {
                if (ch == 'd' && !sawDay) {
                    result[resultIndex++] = 'd';
                    sawDay = true;
                } else if ((ch == 'L' || ch == 'M') && !sawMonth) {
                    result[resultIndex++] = 'M';
                    sawMonth = true;
                } else if ((ch == 'y') && !sawYear) {
                    result[resultIndex++] = 'y';
                    sawYear = true;
                }
            } else if (ch == 'G') {
                // Ignore the era specifier, if present.
            } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                throw new IllegalArgumentException("Bad pattern character '" + ch + "' in "
                    + pattern);
            } else if (ch == '\'') {
                if (i < pattern.length() - 1 && pattern.charAt(i + 1) == '\'') {
                    ++i;
                } else {
                    i = pattern.indexOf('\'', i + 1);
                    if (i == -1) {
                        throw new IllegalArgumentException("Bad quoting in " + pattern);
                    }
                    ++i;
                }
            } else {
                // Ignore spaces and punctuation.
            }
        }
        return result;
    }

    private static String getDateFormatString(Context context) {
        final Locale locale = context.getResources().getConfiguration().locale;
        java.text.DateFormat df = java.text.DateFormat.getDateInstance(
                java.text.DateFormat.SHORT, locale);
        if (df instanceof SimpleDateFormat) {
            return ((SimpleDateFormat) df).toPattern();
        }

        throw new AssertionError("!(df instanceof SimpleDateFormat)");
    }

    /**
     * Given a format string and a time in milliseconds since Jan 1, 1970 GMT, returns a
     * CharSequence containing the requested date.
     * @param inFormat the format string, as described in {@link android.text.format.DateFormat}
     * @param inTimeInMillis in milliseconds since Jan 1, 1970 GMT
     * @return a {@link CharSequence} containing the requested text
     */
    public static CharSequence format(CharSequence inFormat, long inTimeInMillis) {
        return format(inFormat, new Date(inTimeInMillis));
    }

    /**
     * Given a format string and a {@link java.util.Date} object, returns a CharSequence containing
     * the requested date.
     * @param inFormat the format string, as described in {@link android.text.format.DateFormat}
     * @param inDate the date to format
     * @return a {@link CharSequence} containing the requested text
     */
    public static CharSequence format(CharSequence inFormat, Date inDate) {
        Calendar c = new GregorianCalendar();
        c.setTime(inDate);
        return format(inFormat, c);
    }

    /**
     * Indicates whether the specified format string contains seconds.
     *
     * Always returns false if the input format is null.
     *
     * @param inFormat the format string, as described in {@link android.text.format.DateFormat}
     *
     * @return true if the format string contains {@link #SECONDS}, false otherwise
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static boolean hasSeconds(CharSequence inFormat) {
        return hasDesignator(inFormat, SECONDS);
    }

    /**
     * Test if a format string contains the given designator. Always returns
     * {@code false} if the input format is {@code null}.
     *
     * Note that this is intended for searching for designators, not arbitrary
     * characters. So searching for a literal single quote would not work correctly.
     *
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static boolean hasDesignator(CharSequence inFormat, char designator) {
        if (inFormat == null) return false;

        final int length = inFormat.length();

        boolean insideQuote = false;
        for (int i = 0; i < length; i++) {
            final char c = inFormat.charAt(i);
            if (c == QUOTE) {
                insideQuote = !insideQuote;
            } else if (!insideQuote) {
                if (c == designator) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Given a format string and a {@link java.util.Calendar} object, returns a CharSequence
     * containing the requested date.
     * @param inFormat the format string, as described in {@link android.text.format.DateFormat}
     * @param inDate the date to format
     * @return a {@link CharSequence} containing the requested text
     */
    public static CharSequence format(CharSequence inFormat, Calendar inDate) {
        SpannableStringBuilder s = new SpannableStringBuilder(inFormat);
        int count;

        DateFormatSymbols dfs = getIcuDateFormatSymbols(Locale.getDefault());
        String[] amPm = dfs.getAmPmStrings();

        int len = inFormat.length();

        for (int i = 0; i < len; i += count) {
            count = 1;
            int c = s.charAt(i);

            if (c == QUOTE) {
                count = appendQuotedText(s, i);
                len = s.length();
                continue;
            }

            while ((i + count < len) && (s.charAt(i + count) == c)) {
                count++;
            }

            String replacement;
            switch (c) {
                case 'A':
                case 'a':
                    replacement = amPm[inDate.get(Calendar.AM_PM) - Calendar.AM];
                    break;
                case 'd':
                    replacement = zeroPad(inDate.get(Calendar.DATE), count);
                    break;
                case 'c':
                case 'E':
                    replacement = getDayOfWeekString(dfs,
                                                     inDate.get(Calendar.DAY_OF_WEEK), count, c);
                    break;
                case 'K': // hour in am/pm (0-11)
                case 'h': // hour in am/pm (1-12)
                    {
                        int hour = inDate.get(Calendar.HOUR);
                        if (c == 'h' && hour == 0) {
                            hour = 12;
                        }
                        replacement = zeroPad(hour, count);
                    }
                    break;
                case 'H': // hour in day (0-23)
                case 'k': // hour in day (1-24) [but see note below]
                    {
                        int hour = inDate.get(Calendar.HOUR_OF_DAY);
                        // Historically on Android 'k' was interpreted as 'H', which wasn't
                        // implemented, so pretty much all callers that want to format 24-hour
                        // times are abusing 'k'. http://b/8359981.
                        if (false && c == 'k' && hour == 0) {
                            hour = 24;
                        }
                        replacement = zeroPad(hour, count);
                    }
                    break;
                case 'L':
                case 'M':
                    replacement = getMonthString(dfs, inDate.get(Calendar.MONTH), count, c);
                    break;
                case 'm':
                    replacement = zeroPad(inDate.get(Calendar.MINUTE), count);
                    break;
                case 's':
                    replacement = zeroPad(inDate.get(Calendar.SECOND), count);
                    break;
                case 'y':
                    replacement = getYearString(inDate.get(Calendar.YEAR), count);
                    break;
                case 'z':
                    replacement = getTimeZoneString(inDate, count);
                    break;
                default:
                    replacement = null;
                    break;
            }

            if (replacement != null) {
                s.replace(i, i + count, replacement);
                count = replacement.length(); // CARE: count is used in the for loop above
                len = s.length();
            }
        }

        if (inFormat instanceof Spanned) {
            return new SpannedString(s);
        } else {
            return s.toString();
        }
    }

    private static String getDayOfWeekString(DateFormatSymbols dfs, int day, int count, int kind) {
        boolean standalone = (kind == 'c');
        int context = standalone ? DateFormatSymbols.STANDALONE : DateFormatSymbols.FORMAT;
        final int width;
        if (count == 5) {
            width = DateFormatSymbols.NARROW;
        } else if (count == 4) {
            width = DateFormatSymbols.WIDE;
        } else {
            width = DateFormatSymbols.ABBREVIATED;
        }
        return dfs.getWeekdays(context, width)[day];
    }

    private static String getMonthString(DateFormatSymbols dfs, int month, int count, int kind) {
        boolean standalone = (kind == 'L');
        int monthContext = standalone ? DateFormatSymbols.STANDALONE : DateFormatSymbols.FORMAT;
        if (count == 5) {
            return dfs.getMonths(monthContext, DateFormatSymbols.NARROW)[month];
        } else if (count == 4) {
            return dfs.getMonths(monthContext, DateFormatSymbols.WIDE)[month];
        } else if (count == 3) {
            return dfs.getMonths(monthContext, DateFormatSymbols.ABBREVIATED)[month];
        } else {
            // Calendar.JANUARY == 0, so add 1 to month.
            return zeroPad(month+1, count);
        }
    }

    private static String getTimeZoneString(Calendar inDate, int count) {
        TimeZone tz = inDate.getTimeZone();
        if (count < 2) { // FIXME: shouldn't this be <= 2 ?
            return formatZoneOffset(inDate.get(Calendar.DST_OFFSET) +
                                    inDate.get(Calendar.ZONE_OFFSET),
                                    count);
        } else {
            boolean dst = inDate.get(Calendar.DST_OFFSET) != 0;
            return tz.getDisplayName(dst, TimeZone.SHORT);
        }
    }

    private static String formatZoneOffset(int offset, int count) {
        offset /= 1000; // milliseconds to seconds
        StringBuilder tb = new StringBuilder();

        if (offset < 0) {
            tb.insert(0, "-");
            offset = -offset;
        } else {
            tb.insert(0, "+");
        }

        int hours = offset / 3600;
        int minutes = (offset % 3600) / 60;

        tb.append(zeroPad(hours, 2));
        tb.append(zeroPad(minutes, 2));
        return tb.toString();
    }

    private static String getYearString(int year, int count) {
        return (count <= 2) ? zeroPad(year % 100, 2)
                            : String.format(Locale.getDefault(), "%d", year);
    }


    /**
     * Strips quotation marks from the {@code formatString} and appends the result back to the
     * {@code formatString}.
     *
     * @param formatString the format string, as described in
     *                     {@link android.text.format.DateFormat}, to be modified
     * @param index        index of the first quote
     * @return the length of the quoted text that was appended.
     * @hide
     */
    public static int appendQuotedText(SpannableStringBuilder formatString, int index) {
        int length = formatString.length();
        if (index + 1 < length && formatString.charAt(index + 1) == QUOTE) {
            formatString.delete(index, index + 1);
            return 1;
        }

        int count = 0;

        // delete leading quote
        formatString.delete(index, index + 1);
        length--;

        while (index < length) {
            char c = formatString.charAt(index);

            if (c == QUOTE) {
                //  QUOTEQUOTE -> QUOTE
                if (index + 1 < length && formatString.charAt(index + 1) == QUOTE) {

                    formatString.delete(index, index + 1);
                    length--;
                    count++;
                    index++;
                } else {
                    //  Closing QUOTE ends quoted text copying
                    formatString.delete(index, index + 1);
                    break;
                }
            } else {
                index++;
                count++;
            }
        }

        return count;
    }

    private static String zeroPad(int inValue, int inMinDigits) {
        return String.format(Locale.getDefault(), "%0" + inMinDigits + "d", inValue);
    }

    /**
     * We use Gregorian calendar for date formats in android.text.format and various UI widget
     * historically. It's a utility method to get an {@link DateFormatSymbols} instance. Note that
     * {@link DateFormatSymbols} has cache, and external cache is not needed unless same instance is
     * requested repeatedly in the performance critical code.
     *
     * @hide
     */
    public static DateFormatSymbols getIcuDateFormatSymbols(Locale locale) {
        return new DateFormatSymbols(android.icu.util.GregorianCalendar.class, locale);
    }

    /**
     * See http://b/266731719. It mirrors the implementation in
     * {@link libcore.icu.SimpleDateFormatData.DateTimeFormatStringGenerator#postProcessPattern}
     */
    private static String getCompatibleEnglishPattern(ULocale locale, String pattern) {
        if (pattern == null || locale == null || !"en".equals(locale.getLanguage())) {
            return pattern;
        }

        String region = locale.getCountry();
        if (region != null && !region.isEmpty() && !"US".equals(region)) {
            return pattern;
        }

        return pattern.replace('\u202f', ' ');
    }
}
