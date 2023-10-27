/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/**
 * The spatialization level supported by the spatializer stage effect implementation.
 * Used by methods of the ISpatializer interface.
 * {@hide}
 */
public @interface SpatializationLevel {
  /** Spatialization is disabled. */
  public static final byte NONE = 0;
  /** The spatializer accepts audio with positional multichannel masks (e.g 5.1). */
  public static final byte SPATIALIZER_MULTICHANNEL = 1;
  /**
   * The spatializer accepts audio made of a channel bed of positional multichannels (e.g 5.1)
   * and audio objects positioned independently via meta data.
   */
  public static final byte SPATIALIZER_MCHAN_BED_PLUS_OBJECTS = 2;
}
