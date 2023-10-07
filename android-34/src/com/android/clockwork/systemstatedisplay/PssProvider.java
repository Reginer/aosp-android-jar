package com.android.clockwork.systemstatedisplay;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug.MemoryInfo;
import java.util.Arrays;

/** A class that provides {@link SystemState} for PSS. */
final class PssProvider extends SystemStateProvider {
  private static final String SETTINGS_KEY = "PSS_OVERLAY_DISPLAY_SETTING_KEY";

  private final ActivityManager mActivityManager;

  PssProvider(Context context) {
    super(context, SETTINGS_KEY);
    mActivityManager = context.getSystemService(ActivityManager.class);
  }

  @Override
  SystemState getSystemState() {
    if (!isEnabled()) return SystemState.INVALID_STATE;

    MemoryInfo[] memInfos =
        mActivityManager.getProcessMemoryInfo(
            mActivityManager.getRunningAppProcesses().stream().mapToInt(app -> app.pid).toArray());
    int pss = Arrays.stream(memInfos).mapToInt(MemoryInfo::getTotalPss).sum();

    return new SystemState(/* title= */ "PSS", /* value= */ Integer.toString(pss));
  }
}
