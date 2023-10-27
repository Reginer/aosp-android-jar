/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package android.icu.message2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.AttributedCharacterIterator;

import android.icu.text.ConstrainedFieldPosition;
import android.icu.text.FormattedValue;

/**
 * Very-very rough implementation of FormattedValue, packaging a string.
 * Expect it to change.
 *
 * @deprecated This API is for unit testing only.
 * @hide Only a subset of ICU is exposed in Android
 * @hide draft / provisional / internal are hidden on Android
 */
@Deprecated
public class PlainStringFormattedValue implements FormattedValue {
    private final String value;

    /**
     * Constructor, taking the string to store.
     *
     * @param value the string value to store
     *
     * @deprecated This API is for unit testing only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public PlainStringFormattedValue(String value) {
        if (value == null) {
            throw new IllegalAccessError("Should not try to wrap a null in a formatted value");
        }
        this.value = value;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This API is for unit testing only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    @Override
    public int length() {
        return value == null ? 0 : value.length();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This API is for unit testing only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This API is for unit testing only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This API is for unit testing only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    @Override
    public <A extends Appendable> A appendTo(A appendable) {
        try {
            appendable.append(value);
        } catch (IOException e) {
            throw new UncheckedIOException("problem appending", e);
        }
        return appendable;
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @deprecated This API is for unit testing only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    @Override
    public boolean nextPosition(ConstrainedFieldPosition cfpos) {
        throw new RuntimeException("nextPosition not yet implemented");
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @deprecated This API is for unit testing only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    @Override
    public AttributedCharacterIterator toCharacterIterator() {
        throw new RuntimeException("toCharacterIterator not yet implemented");
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This API is for unit testing only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    @Override
    public String toString() {
        return value;
    }
}
