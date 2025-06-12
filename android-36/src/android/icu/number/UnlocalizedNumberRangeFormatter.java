/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package android.icu.number;

import java.util.Locale;

import android.icu.util.ULocale;

/**
 * A NumberRangeFormatter that does not yet have a locale. In order to format, a locale must be specified.
 *
 * Instances of this class are immutable and thread-safe.
 *
 * @author sffc
 * @see NumberRangeFormatter
 */
public class UnlocalizedNumberRangeFormatter extends NumberRangeFormatterSettings<UnlocalizedNumberRangeFormatter> {

    /** Base constructor; called during startup only. */
    UnlocalizedNumberRangeFormatter() {
        super(null, KEY_MACROS, null);
    }

    UnlocalizedNumberRangeFormatter(NumberRangeFormatterSettings<?> parent, int key, Object value) {
        super(parent, key, value);
    }

    /**
     * Associate the given locale with the number range formatter. The locale is used for picking the
     * appropriate symbols, formats, and other data for number display.
     *
     * <p>
     * To use the Java default locale, call Locale.getDefault():
     *
     * <pre>
     * NumberFormatter.with(). ... .locale(Locale.getDefault())
     * </pre>
     *
     * @param locale
     *            The locale to use when loading data for number range formatting.
     * @return The fluent chain
     */
    public LocalizedNumberRangeFormatter locale(Locale locale) {
        return new LocalizedNumberRangeFormatter(this, KEY_LOCALE, ULocale.forLocale(locale));
    }

    /**
     * ULocale version of the {@link #locale(Locale)} setter above.
     *
     * @param locale
     *            The locale to use when loading data for number range formatting.
     * @return The fluent chain
     * @see #locale(Locale)
     */
    public LocalizedNumberRangeFormatter locale(ULocale locale) {
        return new LocalizedNumberRangeFormatter(this, KEY_LOCALE, locale);
    }

    @Override
    UnlocalizedNumberRangeFormatter create(int key, Object value) {
        return new UnlocalizedNumberRangeFormatter(this, key, value);
    }
}
