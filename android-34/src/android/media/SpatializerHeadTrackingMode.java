/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * The head tracking mode supported by the spatializer effect implementation.
 * Used by methods of the ISpatializer interface.
 * {@hide}
 */
public @interface SpatializerHeadTrackingMode {
  /** Head tracking is active in a mode not listed below (forward compatibility) */
  public static final byte OTHER = 0;
  /** Head tracking is disabled */
  public static final byte DISABLED = 1;
  /** Head tracking is performed relative to the real work environment */
  public static final byte RELATIVE_WORLD = 2;
  /** Head tracking is performed relative to the device's screen */
  public static final byte RELATIVE_SCREEN = 3;
}
