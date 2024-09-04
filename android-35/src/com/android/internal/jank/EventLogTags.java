/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/com/android/internal/jank/EventLogTags.logtags
 */

package com.android.internal.jank;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 37001 jank_cuj_events_begin_request (CUJ Type|1|5),(Unix Time Ns|2|3),(Elapsed Time Ns|2|3),(Uptime Ns|2|3),(Tag|3) */
  public static final int JANK_CUJ_EVENTS_BEGIN_REQUEST = 37001;

  /** 37002 jank_cuj_events_end_request (CUJ Type|1|5),(Unix Time Ns|2|3),(Elapsed Time Ns|2|3),(Uptime Time Ns|2|3) */
  public static final int JANK_CUJ_EVENTS_END_REQUEST = 37002;

  /** 37003 jank_cuj_events_cancel_request (CUJ Type|1|5),(Unix Time Ns|2|3),(Elapsed Time Ns|2|3),(Uptime Time Ns|2|3) */
  public static final int JANK_CUJ_EVENTS_CANCEL_REQUEST = 37003;

  public static void writeJankCujEventsBeginRequest(int cujType, long unixTimeNs, long elapsedTimeNs, long uptimeNs, String tag) {
    android.util.EventLog.writeEvent(JANK_CUJ_EVENTS_BEGIN_REQUEST, cujType, unixTimeNs, elapsedTimeNs, uptimeNs, tag);
  }

  public static void writeJankCujEventsEndRequest(int cujType, long unixTimeNs, long elapsedTimeNs, long uptimeTimeNs) {
    android.util.EventLog.writeEvent(JANK_CUJ_EVENTS_END_REQUEST, cujType, unixTimeNs, elapsedTimeNs, uptimeTimeNs);
  }

  public static void writeJankCujEventsCancelRequest(int cujType, long unixTimeNs, long elapsedTimeNs, long uptimeTimeNs) {
    android.util.EventLog.writeEvent(JANK_CUJ_EVENTS_CANCEL_REQUEST, cujType, unixTimeNs, elapsedTimeNs, uptimeTimeNs);
  }
}
