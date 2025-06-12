/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/services/core/java/com/android/server/wm/EventLogTags.logtags
 */

package com.android.server.wm;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 30001 wm_finish_activity (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3),(Reason|3) */
  public static final int WM_FINISH_ACTIVITY = 30001;

  /** 30002 wm_task_to_front (User|1|5),(Task|1|5),(Display Id|1|5) */
  public static final int WM_TASK_TO_FRONT = 30002;

  /** 30003 wm_new_intent (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3),(Action|3),(MIME Type|3),(URI|3),(Flags|1|5) */
  public static final int WM_NEW_INTENT = 30003;

  /** 30004 wm_create_task (User|1|5),(Task ID|1|5),(Root Task ID|1|5),(Display Id|1|5) */
  public static final int WM_CREATE_TASK = 30004;

  /** 30005 wm_create_activity (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3),(Action|3),(MIME Type|3),(URI|3),(Flags|1|5) */
  public static final int WM_CREATE_ACTIVITY = 30005;

  /** 30006 wm_restart_activity (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3) */
  public static final int WM_RESTART_ACTIVITY = 30006;

  /** 30007 wm_resume_activity (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3) */
  public static final int WM_RESUME_ACTIVITY = 30007;

  /** 30009 wm_activity_launch_time (User|1|5),(Token|1|5),(Component Name|3),(time|2|3) */
  public static final int WM_ACTIVITY_LAUNCH_TIME = 30009;

  /** 30012 wm_failed_to_pause (User|1|5),(Token|1|5),(Wanting to pause|3),(Currently pausing|3) */
  public static final int WM_FAILED_TO_PAUSE = 30012;

  /** 30013 wm_pause_activity (User|1|5),(Token|1|5),(Component Name|3),(User Leaving|3),(Reason|3) */
  public static final int WM_PAUSE_ACTIVITY = 30013;

  /** 30018 wm_destroy_activity (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3),(Reason|3) */
  public static final int WM_DESTROY_ACTIVITY = 30018;

  /** 30019 wm_relaunch_resume_activity (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3),(config mask|3) */
  public static final int WM_RELAUNCH_RESUME_ACTIVITY = 30019;

  /** 30020 wm_relaunch_activity (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3),(config mask|3) */
  public static final int WM_RELAUNCH_ACTIVITY = 30020;

  /** 30043 wm_set_resumed_activity (User|1|5),(Component Name|3),(Reason|3) */
  public static final int WM_SET_RESUMED_ACTIVITY = 30043;

  /** 30044 wm_focused_root_task (User|1|5),(Display Id|1|5),(Focused Root Task Id|1|5),(Last Focused Root Task Id|1|5),(Reason|3) */
  public static final int WM_FOCUSED_ROOT_TASK = 30044;

  /** 30048 wm_stop_activity (User|1|5),(Token|1|5),(Component Name|3) */
  public static final int WM_STOP_ACTIVITY = 30048;

  /** 30066 wm_add_to_stopping (User|1|5),(Token|1|5),(Component Name|3),(Reason|3) */
  public static final int WM_ADD_TO_STOPPING = 30066;

  /** 30067 wm_set_keyguard_shown (Display Id|1|5),(keyguardShowing|1),(aodShowing|1),(keyguardGoingAway|1),(occluded|1),(Reason|3) */
  public static final int WM_SET_KEYGUARD_SHOWN = 30067;

  /** 31000 wm_no_surface_memory (Window|3),(PID|1|5),(Operation|3) */
  public static final int WM_NO_SURFACE_MEMORY = 31000;

  /** 31001 wm_task_created (TaskId|1|5) */
  public static final int WM_TASK_CREATED = 31001;

  /** 31002 wm_task_moved (TaskId|1|5),(Root Task ID|1|5),(Display Id|1|5),(ToTop|1),(Index|1) */
  public static final int WM_TASK_MOVED = 31002;

  /** 31003 wm_task_removed (TaskId|1|5),(Root Task ID|1|5),(Display Id|1|5),(Reason|3) */
  public static final int WM_TASK_REMOVED = 31003;

  /** 31004 wm_tf_created (Token|1|5),(TaskId|1|5) */
  public static final int WM_TF_CREATED = 31004;

  /** 31005 wm_tf_removed (Token|1|5),(TaskId|1|5) */
  public static final int WM_TF_REMOVED = 31005;

  /** 31006 wm_set_requested_orientation (Orientation|1|5),(Component Name|3) */
  public static final int WM_SET_REQUESTED_ORIENTATION = 31006;

  /** 31007 wm_boot_animation_done (time|2|3) */
  public static final int WM_BOOT_ANIMATION_DONE = 31007;

  /** 31008 wm_set_keyguard_occluded (occluded|1),(animate|1),(transit|1),(Channel|3) */
  public static final int WM_SET_KEYGUARD_OCCLUDED = 31008;

  /** 31100 wm_back_navi_canceled (Reason|3) */
  public static final int WM_BACK_NAVI_CANCELED = 31100;

  /** 32003 imf_update_ime_parent (surface name|3) */
  public static final int IMF_UPDATE_IME_PARENT = 32003;

  /** 32004 imf_show_ime_screenshot (target window|3),(transition|1),(surface position|3) */
  public static final int IMF_SHOW_IME_SCREENSHOT = 32004;

  /** 32005 imf_remove_ime_screenshot (target window|3) */
  public static final int IMF_REMOVE_IME_SCREENSHOT = 32005;

  /** 33001 wm_wallpaper_surface (Display Id|1|5),(Visible|1),(Target|3) */
  public static final int WM_WALLPAPER_SURFACE = 33001;

  /** 38000 wm_enter_pip (User|1|5),(Token|1|5),(Component Name|3),(is Auto Enter|3) */
  public static final int WM_ENTER_PIP = 38000;

  /** 38200 wm_dim_created (Host|3),(Surface|1) */
  public static final int WM_DIM_CREATED = 38200;

  /** 38201 wm_dim_exit (Surface|1),(dimmingWindow|3),(hostIsVisible|1),(removeImmediately|1) */
  public static final int WM_DIM_EXIT = 38201;

  /** 38202 wm_dim_animate (Surface|1, (toAlpha|5), (toBlur|5)) */
  public static final int WM_DIM_ANIMATE = 38202;

  /** 38203 wm_dim_cancel_anim (Surface|1),(reason|3) */
  public static final int WM_DIM_CANCEL_ANIM = 38203;

  /** 38204 wm_dim_finish_anim (Surface|1) */
  public static final int WM_DIM_FINISH_ANIM = 38204;

  /** 38205 wm_dim_removed (Surface|1) */
  public static final int WM_DIM_REMOVED = 38205;

  public static void writeWmFinishActivity(int user, int token, int taskId, String componentName, String reason) {
    android.util.EventLog.writeEvent(WM_FINISH_ACTIVITY, user, token, taskId, componentName, reason);
  }

  public static void writeWmTaskToFront(int user, int task, int displayId) {
    android.util.EventLog.writeEvent(WM_TASK_TO_FRONT, user, task, displayId);
  }

  public static void writeWmNewIntent(int user, int token, int taskId, String componentName, String action, String mimeType, String uri, int flags) {
    android.util.EventLog.writeEvent(WM_NEW_INTENT, user, token, taskId, componentName, action, mimeType, uri, flags);
  }

  public static void writeWmCreateTask(int user, int taskId, int rootTaskId, int displayId) {
    android.util.EventLog.writeEvent(WM_CREATE_TASK, user, taskId, rootTaskId, displayId);
  }

  public static void writeWmCreateActivity(int user, int token, int taskId, String componentName, String action, String mimeType, String uri, int flags) {
    android.util.EventLog.writeEvent(WM_CREATE_ACTIVITY, user, token, taskId, componentName, action, mimeType, uri, flags);
  }

  public static void writeWmRestartActivity(int user, int token, int taskId, String componentName) {
    android.util.EventLog.writeEvent(WM_RESTART_ACTIVITY, user, token, taskId, componentName);
  }

  public static void writeWmResumeActivity(int user, int token, int taskId, String componentName) {
    android.util.EventLog.writeEvent(WM_RESUME_ACTIVITY, user, token, taskId, componentName);
  }

  public static void writeWmActivityLaunchTime(int user, int token, String componentName, long time) {
    android.util.EventLog.writeEvent(WM_ACTIVITY_LAUNCH_TIME, user, token, componentName, time);
  }

  public static void writeWmFailedToPause(int user, int token, String wantingToPause, String currentlyPausing) {
    android.util.EventLog.writeEvent(WM_FAILED_TO_PAUSE, user, token, wantingToPause, currentlyPausing);
  }

  public static void writeWmPauseActivity(int user, int token, String componentName, String userLeaving, String reason) {
    android.util.EventLog.writeEvent(WM_PAUSE_ACTIVITY, user, token, componentName, userLeaving, reason);
  }

  public static void writeWmDestroyActivity(int user, int token, int taskId, String componentName, String reason) {
    android.util.EventLog.writeEvent(WM_DESTROY_ACTIVITY, user, token, taskId, componentName, reason);
  }

  public static void writeWmRelaunchResumeActivity(int user, int token, int taskId, String componentName, String configMask) {
    android.util.EventLog.writeEvent(WM_RELAUNCH_RESUME_ACTIVITY, user, token, taskId, componentName, configMask);
  }

  public static void writeWmRelaunchActivity(int user, int token, int taskId, String componentName, String configMask) {
    android.util.EventLog.writeEvent(WM_RELAUNCH_ACTIVITY, user, token, taskId, componentName, configMask);
  }

  public static void writeWmSetResumedActivity(int user, String componentName, String reason) {
    android.util.EventLog.writeEvent(WM_SET_RESUMED_ACTIVITY, user, componentName, reason);
  }

  public static void writeWmFocusedRootTask(int user, int displayId, int focusedRootTaskId, int lastFocusedRootTaskId, String reason) {
    android.util.EventLog.writeEvent(WM_FOCUSED_ROOT_TASK, user, displayId, focusedRootTaskId, lastFocusedRootTaskId, reason);
  }

  public static void writeWmStopActivity(int user, int token, String componentName) {
    android.util.EventLog.writeEvent(WM_STOP_ACTIVITY, user, token, componentName);
  }

  public static void writeWmAddToStopping(int user, int token, String componentName, String reason) {
    android.util.EventLog.writeEvent(WM_ADD_TO_STOPPING, user, token, componentName, reason);
  }

  public static void writeWmSetKeyguardShown(int displayId, int keyguardshowing, int aodshowing, int keyguardgoingaway, int occluded, String reason) {
    android.util.EventLog.writeEvent(WM_SET_KEYGUARD_SHOWN, displayId, keyguardshowing, aodshowing, keyguardgoingaway, occluded, reason);
  }

  public static void writeWmNoSurfaceMemory(String window, int pid, String operation) {
    android.util.EventLog.writeEvent(WM_NO_SURFACE_MEMORY, window, pid, operation);
  }

  public static void writeWmTaskCreated(int taskid) {
    android.util.EventLog.writeEvent(WM_TASK_CREATED, taskid);
  }

  public static void writeWmTaskMoved(int taskid, int rootTaskId, int displayId, int totop, int index) {
    android.util.EventLog.writeEvent(WM_TASK_MOVED, taskid, rootTaskId, displayId, totop, index);
  }

  public static void writeWmTaskRemoved(int taskid, int rootTaskId, int displayId, String reason) {
    android.util.EventLog.writeEvent(WM_TASK_REMOVED, taskid, rootTaskId, displayId, reason);
  }

  public static void writeWmTfCreated(int token, int taskid) {
    android.util.EventLog.writeEvent(WM_TF_CREATED, token, taskid);
  }

  public static void writeWmTfRemoved(int token, int taskid) {
    android.util.EventLog.writeEvent(WM_TF_REMOVED, token, taskid);
  }

  public static void writeWmSetRequestedOrientation(int orientation, String componentName) {
    android.util.EventLog.writeEvent(WM_SET_REQUESTED_ORIENTATION, orientation, componentName);
  }

  public static void writeWmBootAnimationDone(long time) {
    android.util.EventLog.writeEvent(WM_BOOT_ANIMATION_DONE, time);
  }

  public static void writeWmSetKeyguardOccluded(int occluded, int animate, int transit, String channel) {
    android.util.EventLog.writeEvent(WM_SET_KEYGUARD_OCCLUDED, occluded, animate, transit, channel);
  }

  public static void writeWmBackNaviCanceled(String reason) {
    android.util.EventLog.writeEvent(WM_BACK_NAVI_CANCELED, reason);
  }

  public static void writeImfUpdateImeParent(String surfaceName) {
    android.util.EventLog.writeEvent(IMF_UPDATE_IME_PARENT, surfaceName);
  }

  public static void writeImfShowImeScreenshot(String targetWindow, int transition, String surfacePosition) {
    android.util.EventLog.writeEvent(IMF_SHOW_IME_SCREENSHOT, targetWindow, transition, surfacePosition);
  }

  public static void writeImfRemoveImeScreenshot(String targetWindow) {
    android.util.EventLog.writeEvent(IMF_REMOVE_IME_SCREENSHOT, targetWindow);
  }

  public static void writeWmWallpaperSurface(int displayId, int visible, String target) {
    android.util.EventLog.writeEvent(WM_WALLPAPER_SURFACE, displayId, visible, target);
  }

  public static void writeWmEnterPip(int user, int token, String componentName, String isAutoEnter) {
    android.util.EventLog.writeEvent(WM_ENTER_PIP, user, token, componentName, isAutoEnter);
  }

  public static void writeWmDimCreated(String host, int surface) {
    android.util.EventLog.writeEvent(WM_DIM_CREATED, host, surface);
  }

  public static void writeWmDimExit(int surface, String dimmingwindow, int hostisvisible, int removeimmediately) {
    android.util.EventLog.writeEvent(WM_DIM_EXIT, surface, dimmingwindow, hostisvisible, removeimmediately);
  }

  public static void writeWmDimAnimate(int surface, float toalpha, float toblur) {
    android.util.EventLog.writeEvent(WM_DIM_ANIMATE, surface, toalpha, toblur);
  }

  public static void writeWmDimCancelAnim(int surface, String reason) {
    android.util.EventLog.writeEvent(WM_DIM_CANCEL_ANIM, surface, reason);
  }

  public static void writeWmDimFinishAnim(int surface) {
    android.util.EventLog.writeEvent(WM_DIM_FINISH_ANIM, surface);
  }

  public static void writeWmDimRemoved(int surface) {
    android.util.EventLog.writeEvent(WM_DIM_REMOVED, surface);
  }
}
