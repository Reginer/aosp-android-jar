/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.math;

import static java.lang.Double.MIN_EXPONENT;
import static java.lang.Double.PRECISION;
import static java.lang.Double.SIZE;

/**
 * This class contains additional constants documenting limits of the
 * {@code double} type.
 *
 * @author Joseph D. Darcy
 */

public class DoubleConsts {
    /**
     * Don't let anyone instantiate this class.
     */
    private DoubleConsts() {}

    public static final double POSITIVE_INFINITY = java.lang.Double.POSITIVE_INFINITY;
    public static final double NEGATIVE_INFINITY = java.lang.Double.NEGATIVE_INFINITY;
    public static final double NaN = java.lang.Double.NaN;
    public static final double MAX_VALUE = java.lang.Double.MAX_VALUE;
    public static final double MIN_VALUE = java.lang.Double.MIN_VALUE;

    /**
     * A constant holding the smallest positive normal value of type
     * <code>double</code>, 2<sup>-1022</sup>.  It is equal to the
     * value returned by
     * <code>Double.longBitsToDouble(0x0010000000000000L)</code>.
     *
     * @since 1.5
     */
    public static final double  MIN_NORMAL      = 2.2250738585072014E-308;


    /**
     * The number of logical bits in the significand of a
     * {@code double} number, including the implicit bit.
     */
    public static final int SIGNIFICAND_WIDTH = PRECISION;

    /**
     * Maximum exponent a finite <code>double</code> number may have.
     * It is equal to the value returned by
     * <code>Math.ilogb(Double.MAX_VALUE)</code>.
     */
    public static final int     MAX_EXPONENT    = 1023;

    /**
     * Minimum exponent a normalized <code>double</code> number may
     * have.  It is equal to the value returned by
     * <code>Math.ilogb(Double.MIN_NORMAL)</code>.
     */
    public static final int     MIN_EXPONENT    = -1022;

    /**
     * The exponent the smallest positive {@code double}
     * subnormal value would have if it could be normalized..
     */
    public static final int MIN_SUB_EXPONENT =
            MIN_EXPONENT - (SIGNIFICAND_WIDTH - 1); // -1074

    /**
     * Bias used in representing a {@code double} exponent.
     */
    public static final int EXP_BIAS =
            (1 << (SIZE - SIGNIFICAND_WIDTH - 1)) - 1; // 1023

    /**
     * Bit mask to isolate the sign bit of a {@code double}.
     */
    public static final long SIGN_BIT_MASK = 1L << (SIZE - 1);

    /**
     * Bit mask to isolate the exponent field of a {@code double}.
     */
    public static final long EXP_BIT_MASK =
            ((1L << (SIZE - SIGNIFICAND_WIDTH)) - 1) << (SIGNIFICAND_WIDTH - 1);

    /**
     * Bit mask to isolate the significand field of a {@code double}.
     */
    public static final long SIGNIF_BIT_MASK = (1L << (SIGNIFICAND_WIDTH - 1)) - 1;

    /**
     * Bit mask to isolate the magnitude bits (combined exponent and
     * significand fields) of a {@code double}.
     */
    public static final long MAG_BIT_MASK = EXP_BIT_MASK | SIGNIF_BIT_MASK;

    static {
        // verify bit masks cover all bit positions and that the bit
        // masks are non-overlapping
        assert(((SIGN_BIT_MASK | EXP_BIT_MASK | SIGNIF_BIT_MASK) == ~0L) &&
               (((SIGN_BIT_MASK & EXP_BIT_MASK) == 0L) &&
                ((SIGN_BIT_MASK & SIGNIF_BIT_MASK) == 0L) &&
                ((EXP_BIT_MASK & SIGNIF_BIT_MASK) == 0L)) &&
                ((SIGN_BIT_MASK | MAG_BIT_MASK) == ~0L));
    }
}
