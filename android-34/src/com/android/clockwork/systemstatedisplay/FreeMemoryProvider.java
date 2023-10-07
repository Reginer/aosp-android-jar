package com.android.clockwork.systemstatedisplay;

import static android.app.ActivityManager.MemoryInfo;

import android.app.ActivityManager;
import android.content.Context;

/** A class that provides {@link SystemState} for Free Memory. */
final class FreeMemoryProvider extends SystemStateProvider {
  private static final String SETTINGS_KEY = "FREE_MEM_OVERLAY_DISPLAY_SETTING_KEY";

  private final ActivityManager mActivityManager;
  private final long mTotalMemory;

  FreeMemoryProvider(Context context) {
    super(context, SETTINGS_KEY);
    mActivityManager = context.getSystemService(ActivityManager.class);
    mTotalMemory = getPopulatedMemoryInfo().totalMem;
  }

  @Override
  SystemState getSystemState() {
    if (!isEnabled()) return SystemState.INVALID_STATE;

    return new SystemState(
            /* title= */ "Free memory",
            /* value= */ String.format(
                    "%.2f%%", (getPopulatedMemoryInfo().availMem * 100f / mTotalMemory)));
  }

  private MemoryInfo getPopulatedMemoryInfo() {
    MemoryInfo memInfo = new MemoryInfo();
    mActivityManager.getMemoryInfo(memInfo);

    return memInfo;
  }
}
