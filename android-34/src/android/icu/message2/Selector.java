/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package android.icu.message2;

import java.util.Map;

/**
 * The interface that must be implemented by all selectors
 * that can be used from {@link MessageFormatter}.
 *
 * <p>Selectors are used to choose between different message variants,
 * similar to <code>plural</code>, <code>selectordinal</code>,
 * and <code>select</code> in {@link android.icu.text.MessageFormat}.</p>
 *
 * @deprecated This API is for technology preview only.
 * @hide Only a subset of ICU is exposed in Android
 * @hide draft / provisional / internal are hidden on Android
 */
@Deprecated
public interface Selector {
    /**
     * A method that is invoked for the object to match and each key.
     *
     * <p>For example an English plural {@code matches} would return {@code true}
     * for {@code matches(1, "1")}, {@code matches(1, "one")}, and {@code matches(1, "*")}.</p>
     *
     * @param value the value to select on.
     * @param key the key to test for matching.
     * @param variableOptions options that are not know at build time.
     * @return the formatted string.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    boolean matches(Object value, String key, Map<String, Object> variableOptions);
}
