/* This file is auto-generated.  DO NOT MODIFY.
 * Source file: frameworks/base/services/core/java/com/android/server/policy/EventLogTags.logtags
 */

package com.android.server.policy;

/**
 * @hide
 */
public class EventLogTags {
  private EventLogTags() { }  // don't instantiate

  /** 70000 screen_toggled (screen_state|1|5) */
  public static final int SCREEN_TOGGLED = 70000;

  /** 70001 intercept_power (action|3),(mPowerKeyHandled|1),(mPowerKeyPressCounter|1) */
  public static final int INTERCEPT_POWER = 70001;

  public static void writeScreenToggled(int screenState) {
    android.util.EventLog.writeEvent(SCREEN_TOGGLED, screenState);
  }

  public static void writeInterceptPower(String action, int mpowerkeyhandled, int mpowerkeypresscounter) {
    android.util.EventLog.writeEvent(INTERCEPT_POWER, action, mpowerkeyhandled, mpowerkeypresscounter);
  }
}
