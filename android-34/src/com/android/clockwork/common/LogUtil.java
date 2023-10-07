package com.android.clockwork.common;

import android.os.Build;
import android.util.Log;

/** Methods for writing to logcat. */
public final class LogUtil {

  private LogUtil() {}

  /** Logs message as debug if level is set or if the build type is not user. */
  public static void logDOrNotUser(String tag, String message) {
    if (isDorNotUser(tag)) {
      Log.d(tag, message);
    }
  }

  /** Logs message as debug if level is set or if the build type is not user. */
  public static void logDOrNotUser(String tag, String message, Object... params) {
    if (isDorNotUser(tag)) {
      Log.d(tag, String.format(message, params));
    }
  }

  /** Logs message as debug if level is set. */
  public static void logD(String tag, String message) {
    logDOrNotUser(tag, message);
  }

  /** Logs message as debug if level is set. */
  public static void logD(String tag, String message, Object... params) {
    logDOrNotUser(tag, message, params);
  }

  /** Return true if tag has DEBUG log level or build is not a -user build. */
  public static boolean isDorNotUser(String tag) {
    return Log.isLoggable(tag, Log.DEBUG) || !isUserBuild();
  }

  private static boolean isUserBuild() {
    return Build.TYPE.equals("user");
  }
}
