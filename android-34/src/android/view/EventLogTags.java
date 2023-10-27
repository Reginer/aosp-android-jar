/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/android/view/EventLogTags.logtags
 */

package android.view;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 32006 imf_ime_anim_start (token|3),(animation type|1),(alpha|5),(current insets|3),(shown insets|3),(hidden insets|3) */
  public static final int IMF_IME_ANIM_START = 32006;

  /** 32007 imf_ime_anim_finish (token|3),(animation type|1),(alpha|5),(shown|1),(insets|3) */
  public static final int IMF_IME_ANIM_FINISH = 32007;

  /** 32008 imf_ime_anim_cancel (token|3),(animation type|1),(pending insets|3) */
  public static final int IMF_IME_ANIM_CANCEL = 32008;

  /** 32009 imf_ime_remote_anim_start (token|3),(displayId|1),(direction|1),(alpha|5),(startY|5),(endY|5),(leash|3),(insets|3),(surface position|3),(ime frame|3) */
  public static final int IMF_IME_REMOTE_ANIM_START = 32009;

  /** 32010 imf_ime_remote_anim_end (token|3),(displayId|1),(direction|1),(endY|5),(leash|3),(insets|3),(surface position|3),(ime frame|3) */
  public static final int IMF_IME_REMOTE_ANIM_END = 32010;

  /** 32011 imf_ime_remote_anim_cancel (token|3),(displayId|1),(insets|3) */
  public static final int IMF_IME_REMOTE_ANIM_CANCEL = 32011;

  /** 62002 view_enqueue_input_event (eventType|3),(action|3) */
  public static final int VIEW_ENQUEUE_INPUT_EVENT = 62002;

  public static void writeImfImeAnimStart(String token, int animationType, float alpha, String currentInsets, String shownInsets, String hiddenInsets) {
    android.util.EventLog.writeEvent(IMF_IME_ANIM_START, token, animationType, alpha, currentInsets, shownInsets, hiddenInsets);
  }

  public static void writeImfImeAnimFinish(String token, int animationType, float alpha, int shown, String insets) {
    android.util.EventLog.writeEvent(IMF_IME_ANIM_FINISH, token, animationType, alpha, shown, insets);
  }

  public static void writeImfImeAnimCancel(String token, int animationType, String pendingInsets) {
    android.util.EventLog.writeEvent(IMF_IME_ANIM_CANCEL, token, animationType, pendingInsets);
  }

  public static void writeImfImeRemoteAnimStart(String token, int displayid, int direction, float alpha, float starty, float endy, String leash, String insets, String surfacePosition, String imeFrame) {
    android.util.EventLog.writeEvent(IMF_IME_REMOTE_ANIM_START, token, displayid, direction, alpha, starty, endy, leash, insets, surfacePosition, imeFrame);
  }

  public static void writeImfImeRemoteAnimEnd(String token, int displayid, int direction, float endy, String leash, String insets, String surfacePosition, String imeFrame) {
    android.util.EventLog.writeEvent(IMF_IME_REMOTE_ANIM_END, token, displayid, direction, endy, leash, insets, surfacePosition, imeFrame);
  }

  public static void writeImfImeRemoteAnimCancel(String token, int displayid, String insets) {
    android.util.EventLog.writeEvent(IMF_IME_REMOTE_ANIM_CANCEL, token, displayid, insets);
  }

  public static void writeViewEnqueueInputEvent(String eventtype, String action) {
    android.util.EventLog.writeEvent(VIEW_ENQUEUE_INPUT_EVENT, eventtype, action);
  }
}
