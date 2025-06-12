/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package android.icu.message2;

import java.util.Locale;
import java.util.Map;

/**
 * The interface that must be implemented for each selection function
 * that can be used from {@link MessageFormatter}.
 *
 * <p>The we use it to create and cache various selectors with various options.</p>
 *
 * @deprecated This API is for technology preview only.
 * @hide Only a subset of ICU is exposed in Android
 * @hide draft / provisional / internal are hidden on Android
 */
@Deprecated
public interface SelectorFactory {
    /**
     * The method that is called to create a selector.
     *
     * @param locale the locale to use for selection.
     * @param fixedOptions the options to use for selection. The keys and values are function dependent.
     * @return The Selector.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    Selector createSelector(Locale locale, Map<String, Object> fixedOptions);
}
