/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package android.media;
/** {@hide} */
public @interface AudioUniqueIdUse {
  public static final int UNSPECIFIED = 0;
  public static final int SESSION = 1;
  // audio_session_t
  // for allocated sessions, not special AUDIO_SESSION_*
  public static final int MODULE = 2;
  // audio_module_handle_t
  public static final int EFFECT = 3;
  // audio_effect_handle_t
  public static final int PATCH = 4;
  // audio_patch_handle_t
  public static final int OUTPUT = 5;
  // audio_io_handle_t
  public static final int INPUT = 6;
  // audio_io_handle_t
  public static final int CLIENT = 7;
}
