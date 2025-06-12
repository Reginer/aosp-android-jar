/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/org/chromium/arc/EventLogTags.logtags
 */

package org.chromium.arc;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 300000 arc_system_event (event|3) */
  public static final int ARC_SYSTEM_EVENT = 300000;

  public static void writeArcSystemEvent(String event) {
    android.util.EventLog.writeEvent(ARC_SYSTEM_EVENT, event);
  }
}
