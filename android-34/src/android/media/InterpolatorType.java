/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * Polynomial spline interpolators.
 * 
 * {@hide}
 */
public @interface InterpolatorType {
  /** Not continuous. */
  public static final int STEP = 0;
  /** C0. */
  public static final int LINEAR = 1;
  /** C1. */
  public static final int CUBIC = 2;
  /** C1 (to provide locally monotonic curves). */
  public static final int CUBIC_MONOTONIC = 3;
}
