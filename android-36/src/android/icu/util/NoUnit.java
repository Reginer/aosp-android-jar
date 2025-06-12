/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package android.icu.util;

import android.icu.number.NumberFormatter;

/**
 * Dimensionless unit for percent and permille.
 * @see NumberFormatter
 * @hide Only a subset of ICU is exposed in Android
 */
public final class NoUnit {
    /**
     * Constant for the base unit (dimensionless and no scaling).
     *
     * Prior to ICU 68, this constant equaled an instance of NoUnit.
     *
     * Since ICU 68, this constant equals null.
     */
    public static final MeasureUnit BASE = null;

    /**
     * Constant for the percent unit, or 1/100 of a base unit.
     *
     * Prior to ICU 68, this constant equaled an instance of NoUnit.
     *
     * Since ICU 68, this constant is equivalent to MeasureUnit.PERCENT.
     */
    public static final MeasureUnit PERCENT = MeasureUnit.PERCENT;

    /**
     * Constant for the permille unit, or 1/100 of a base unit.
     *
     * Prior to ICU 68, this constant equaled an instance of NoUnit.
     *
     * Since ICU 68, this constant is equivalent to MeasureUnit.PERMILLE.
     */
    public static final MeasureUnit PERMILLE = MeasureUnit.PERMILLE;

    // This class is a namespace not intended to be instantiated:
    private NoUnit() {}
}
