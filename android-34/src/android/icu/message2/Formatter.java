/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package android.icu.message2;

import java.util.Map;

/**
 * The interface that must be implemented by all formatters
 * that can be used from {@link MessageFormatter}.
 *
 * @deprecated This API is for technology preview only.
 * @hide Only a subset of ICU is exposed in Android
 * @hide draft / provisional / internal are hidden on Android
 */
@Deprecated
public interface Formatter {
    /**
     * A method that takes the object to format and returns
     * the i18n-aware string representation.
     *
     * @param toFormat the object to format.
     * @param variableOptions options that are not know at build time.
     * @return the formatted string.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    String formatToString(Object toFormat, Map<String, Object> variableOptions);

    /**
     * A method that takes the object to format and returns
     * the i18n-aware formatted placeholder.
     *
     * @param toFormat the object to format.
     * @param variableOptions options that are not know at build time.
     * @return the formatted placeholder.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions);
}
