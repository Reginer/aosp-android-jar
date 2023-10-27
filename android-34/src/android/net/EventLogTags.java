/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/core/java/android/net/EventLogTags.logtags
 */

package android.net;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 50080 ntp_success (server|3),(rtt|2),(offset|2) */
  public static final int NTP_SUCCESS = 50080;

  /** 50081 ntp_failure (server|3),(msg|3) */
  public static final int NTP_FAILURE = 50081;

  public static void writeNtpSuccess(String server, long rtt, long offset) {
    android.util.EventLog.writeEvent(NTP_SUCCESS, server, rtt, offset);
  }

  public static void writeNtpFailure(String server, String msg) {
    android.util.EventLog.writeEvent(NTP_FAILURE, server, msg);
  }
}
