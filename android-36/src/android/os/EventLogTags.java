/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/android/os/EventLogTags.logtags
 */

package android.os;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 230000 service_manager_stats (call_count|1),(total_time|1|3),(duration|1|3) */
  public static final int SERVICE_MANAGER_STATS = 230000;

  /** 230001 service_manager_slow (time|1|3),(service|3) */
  public static final int SERVICE_MANAGER_SLOW = 230001;

  public static void writeServiceManagerStats(int callCount, int totalTime, int duration) {
    android.util.EventLog.writeEvent(SERVICE_MANAGER_STATS, callCount, totalTime, duration);
  }

  public static void writeServiceManagerSlow(int time, String service) {
    android.util.EventLog.writeEvent(SERVICE_MANAGER_SLOW, time, service);
  }
}
