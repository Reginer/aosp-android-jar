package com.android.clockwork.systemstatedisplay;

import android.content.Context;
import android.provider.Settings;

/** A class that contains a system state information. */
abstract class SystemStateProvider {
  private static final int PROVIDER_DISABLED_SETTINGS_VALUE = 0;
  private static final int PROVIDER_ENABLED_SETTINGS_VALUE = 1;
  private static final int DEFAULT_PROVIDER_SETTINGS_VALUE = PROVIDER_DISABLED_SETTINGS_VALUE;

  private final String mSettingsKey;
  private final Context mContext;

  SystemStateProvider(Context context, String settingsKey) {
    mContext = context;
    mSettingsKey = settingsKey;
  }

  /** Generates and returns the {@link SystemState} that this provider is responsible for. */
  abstract SystemState getSystemState();

  /** Returns {@code true} if this provider is enabled, or {@code false} otherwise. */
  final boolean isEnabled() {
    return Settings.Secure.getInt(
            mContext.getContentResolver(), mSettingsKey, DEFAULT_PROVIDER_SETTINGS_VALUE)
        == PROVIDER_ENABLED_SETTINGS_VALUE;
  }
}
