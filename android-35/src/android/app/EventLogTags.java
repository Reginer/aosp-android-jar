/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/android/app/EventLogTags.logtags
 */

package android.app;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 30021 wm_on_paused_called (Token|1|5),(Component Name|3),(Reason|3),(time|2|3) */
  public static final int WM_ON_PAUSED_CALLED = 30021;

  /** 30022 wm_on_resume_called (Token|1|5),(Component Name|3),(Reason|3),(time|2|3) */
  public static final int WM_ON_RESUME_CALLED = 30022;

  /** 30049 wm_on_stop_called (Token|1|5),(Component Name|3),(Reason|3),(time|2|3) */
  public static final int WM_ON_STOP_CALLED = 30049;

  /** 30057 wm_on_create_called (Token|1|5),(Component Name|3),(Reason|3),(time|2|3) */
  public static final int WM_ON_CREATE_CALLED = 30057;

  /** 30058 wm_on_restart_called (Token|1|5),(Component Name|3),(Reason|3),(time|2|3) */
  public static final int WM_ON_RESTART_CALLED = 30058;

  /** 30059 wm_on_start_called (Token|1|5),(Component Name|3),(Reason|3),(time|2|3) */
  public static final int WM_ON_START_CALLED = 30059;

  /** 30060 wm_on_destroy_called (Token|1|5),(Component Name|3),(Reason|3),(time|2|3) */
  public static final int WM_ON_DESTROY_CALLED = 30060;

  /** 30062 wm_on_activity_result_called (Token|1|5),(Component Name|3),(Reason|3) */
  public static final int WM_ON_ACTIVITY_RESULT_CALLED = 30062;

  /** 30064 wm_on_top_resumed_gained_called (Token|1|5),(Component Name|3),(Reason|3) */
  public static final int WM_ON_TOP_RESUMED_GAINED_CALLED = 30064;

  /** 30065 wm_on_top_resumed_lost_called (Token|1|5),(Component Name|3),(Reason|3) */
  public static final int WM_ON_TOP_RESUMED_LOST_CALLED = 30065;

  public static void writeWmOnPausedCalled(int token, String componentName, String reason, long time) {
    android.util.EventLog.writeEvent(WM_ON_PAUSED_CALLED, token, componentName, reason, time);
  }

  public static void writeWmOnResumeCalled(int token, String componentName, String reason, long time) {
    android.util.EventLog.writeEvent(WM_ON_RESUME_CALLED, token, componentName, reason, time);
  }

  public static void writeWmOnStopCalled(int token, String componentName, String reason, long time) {
    android.util.EventLog.writeEvent(WM_ON_STOP_CALLED, token, componentName, reason, time);
  }

  public static void writeWmOnCreateCalled(int token, String componentName, String reason, long time) {
    android.util.EventLog.writeEvent(WM_ON_CREATE_CALLED, token, componentName, reason, time);
  }

  public static void writeWmOnRestartCalled(int token, String componentName, String reason, long time) {
    android.util.EventLog.writeEvent(WM_ON_RESTART_CALLED, token, componentName, reason, time);
  }

  public static void writeWmOnStartCalled(int token, String componentName, String reason, long time) {
    android.util.EventLog.writeEvent(WM_ON_START_CALLED, token, componentName, reason, time);
  }

  public static void writeWmOnDestroyCalled(int token, String componentName, String reason, long time) {
    android.util.EventLog.writeEvent(WM_ON_DESTROY_CALLED, token, componentName, reason, time);
  }

  public static void writeWmOnActivityResultCalled(int token, String componentName, String reason) {
    android.util.EventLog.writeEvent(WM_ON_ACTIVITY_RESULT_CALLED, token, componentName, reason);
  }

  public static void writeWmOnTopResumedGainedCalled(int token, String componentName, String reason) {
    android.util.EventLog.writeEvent(WM_ON_TOP_RESUMED_GAINED_CALLED, token, componentName, reason);
  }

  public static void writeWmOnTopResumedLostCalled(int token, String componentName, String reason) {
    android.util.EventLog.writeEvent(WM_ON_TOP_RESUMED_LOST_CALLED, token, componentName, reason);
  }
}
