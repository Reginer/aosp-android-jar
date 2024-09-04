/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/com/android/internal/app/EventLogTags.logtags
 */

package com.android.internal.app;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 53000 harmful_app_warning_uninstall (package_name|3) */
  public static final int HARMFUL_APP_WARNING_UNINSTALL = 53000;

  /** 53001 harmful_app_warning_launch_anyway (package_name|3) */
  public static final int HARMFUL_APP_WARNING_LAUNCH_ANYWAY = 53001;

  public static void writeHarmfulAppWarningUninstall(String packageName) {
    android.util.EventLog.writeEvent(HARMFUL_APP_WARNING_UNINSTALL, packageName);
  }

  public static void writeHarmfulAppWarningLaunchAnyway(String packageName) {
    android.util.EventLog.writeEvent(HARMFUL_APP_WARNING_LAUNCH_ANYWAY, packageName);
  }
}
