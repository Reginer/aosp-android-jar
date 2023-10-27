/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** Defines the mixer behavior that can be used when setting mixer attributes. */
public @interface AudioMixerBehavior {
  /** The mixer behavior is invalid. */
  public static final int INVALID = -1;
  /**
   * The mixer behavior that follows platform default behavior, which is mixing audio from
   * different sources.
   */
  public static final int DEFAULT = 0;
  /** The audio data in the mixer will be bit-perfect as long as possible. */
  public static final int BIT_PERFECT = 1;
}
