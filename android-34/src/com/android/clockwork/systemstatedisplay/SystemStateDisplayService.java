package com.android.clockwork.systemstatedisplay;

import android.annotation.Nullable;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.android.server.SystemService;

/** A {@link SystemService} that fetches and displays system state related data for debugging. */
public class SystemStateDisplayService extends SystemService {
  private static final String TAG = "SystemStateDisplayService";

  // TODO(b/189107780): add this to Settings.Secure.
  private static final String SYSTEM_STATE_DISPLAY_SETTING_KEY = "SYSTEM_STATE_DISPLAY_SETTING_KEY";

  private static final int SYSTEM_STATE_DISPLAY_SETTING_ENABLED = 1;
  private static final int SYSTEM_STATE_DISPLAY_SETTING_DISABLED = 0;

  private static final int DEFAULT_SYSTEM_STATE_DISPLAY_SETTING =
      SYSTEM_STATE_DISPLAY_SETTING_DISABLED;

  private static final long UPDATED_INTERVAL_MILLIS = 5000L;

  private final Context mContext;

  @Nullable private SystemStateUpdateHandler mStateUpdateHandler;

  public SystemStateDisplayService(Context context) {
    super(context);
    mContext = context;
  }

  @Override
  public void onStart() {}

  @Override
  public void onBootPhase(int phase) {
    if (phase != SystemService.PHASE_BOOT_COMPLETED) {
      return;
    }

    mStateUpdateHandler =
        new SystemStateUpdateHandler(mContext, /* updateIntervalMillis= */ UPDATED_INTERVAL_MILLIS);

    mContext
        .getContentResolver()
        .registerContentObserver(
            Settings.Secure.getUriFor(SYSTEM_STATE_DISPLAY_SETTING_KEY),
            /* notifyDescendants= */ false,
            new SystemSettingDisplaySettingObserver(new Handler(Looper.getMainLooper())));

    if (isSystemStateDisplayEnabled()) {
      Log.d(TAG, "System state display enabled on boot. starting updates.");
      mStateUpdateHandler.startUpdates();
    }
  }

  private class SystemSettingDisplaySettingObserver extends ContentObserver {
    SystemSettingDisplaySettingObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      boolean systemStateDisplayEnabled = isSystemStateDisplayEnabled();
      Log.d(TAG, "System state display setting changed. enabled=" + systemStateDisplayEnabled);

      if (systemStateDisplayEnabled) {
        mStateUpdateHandler.startUpdates();
      } else {
        mStateUpdateHandler.stopUpdates();
      }
    }
  }

  private boolean isSystemStateDisplayEnabled() {
    return Settings.Secure.getInt(
            mContext.getContentResolver(),
            SYSTEM_STATE_DISPLAY_SETTING_KEY,
            DEFAULT_SYSTEM_STATE_DISPLAY_SETTING)
        == SYSTEM_STATE_DISPLAY_SETTING_ENABLED;
  }
}
