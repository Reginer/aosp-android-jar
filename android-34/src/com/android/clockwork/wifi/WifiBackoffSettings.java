package com.android.clockwork.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.Settings;

import com.android.clockwork.common.LogUtil;
import com.android.clockwork.common.WearResourceUtil;

/**
 * Configuration settings for Wifi backoff.
 *
 * Settings define backoff parameters, like entry delay and backoff duration; also
 * settings value can direct to disable wifi backoff
 */
public class WifiBackoffSettings {
    private static final String TAG = "WifiBackoffSettings";

    private static final String WIFI_BACKOFF_DELAY_KEY = "cw_wifi_backoff_delay";
    private static final String WIFI_BACKOFF_DURATION_KEY = "cw_wifi_backoff_duration";

    public final long backoffDelayMs;
    public final long backoffDurationMs;

    private WifiBackoffSettings(long backoffDelayMs, long backoffDurationMs) {
        this.backoffDelayMs = backoffDelayMs;
        this.backoffDurationMs = backoffDurationMs;
    }

    /** Register observer to receive callback when wifi backoff settings change. */
    public static void registerSettingsObserver(Context context, ContentObserver observer) {
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(WIFI_BACKOFF_DELAY_KEY),
                false,
                observer);
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(WIFI_BACKOFF_DURATION_KEY),
                false,
                observer);
    }


    /** Factory method to load wifi backoff parameters from Settings. */
    public static WifiBackoffSettings loadWifiBackoffSettings(Context context) {
        long defaultBackoffDelay = WearResourceUtil.getWearableResources(context).getInteger(
                com.android.wearable.resources.R.integer.def_wifi_backoff_delay);
        long defaultBackoffDuration = WearResourceUtil.getWearableResources(context).getInteger(
                com.android.wearable.resources.R.integer.def_wifi_backoff_duration);

        long backoffDelayMs = Settings.System.getLong(context.getContentResolver(),
                WIFI_BACKOFF_DELAY_KEY, defaultBackoffDelay);
        long backoffDurationMs = Settings.System.getLong(context.getContentResolver(),
                WIFI_BACKOFF_DURATION_KEY, defaultBackoffDuration);
        LogUtil.logD(TAG, "loadWifiBackoffSettings delay: %s, def: %s",
                backoffDelayMs, defaultBackoffDelay);
        return new WifiBackoffSettings(backoffDelayMs, backoffDurationMs);
    }

    public boolean isBackoffEnabled() {
        return backoffDelayMs > 0 && backoffDurationMs > 0;
    }
}
