/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/android/webkit/EventLogTags.logtags
 */

package android.webkit;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 70101 browser_zoom_level_change (start level|1|5),(end level|1|5),(time|2|3) */
  public static final int BROWSER_ZOOM_LEVEL_CHANGE = 70101;

  /** 70102 browser_double_tap_duration (duration|1|3),(time|2|3) */
  public static final int BROWSER_DOUBLE_TAP_DURATION = 70102;

  /** 70150 browser_snap_center */
  public static final int BROWSER_SNAP_CENTER = 70150;

  /** 70151 exp_det_attempt_to_call_object_getclass (app_signature|3) */
  public static final int EXP_DET_ATTEMPT_TO_CALL_OBJECT_GETCLASS = 70151;

  public static void writeBrowserZoomLevelChange(int startLevel, int endLevel, long time) {
    android.util.EventLog.writeEvent(BROWSER_ZOOM_LEVEL_CHANGE, startLevel, endLevel, time);
  }

  public static void writeBrowserDoubleTapDuration(int duration, long time) {
    android.util.EventLog.writeEvent(BROWSER_DOUBLE_TAP_DURATION, duration, time);
  }

  public static void writeBrowserSnapCenter() {
    android.util.EventLog.writeEvent(BROWSER_SNAP_CENTER);
  }

  public static void writeExpDetAttemptToCallObjectGetclass(String appSignature) {
    android.util.EventLog.writeEvent(EXP_DET_ATTEMPT_TO_CALL_OBJECT_GETCLASS, appSignature);
  }
}
