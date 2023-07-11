package com.android.clockwork.common;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Exposes a Secure Settings hook to "Disable" a device based on a user actively selecting
 * "Disconnect [Watchname]" from the Companion app.
 *
 * <p>The expectation is that some process on the phone will relay this intent to disable the device
 * to some process on the watch, which will in turn write to the secure setting.
 *
 * <p>Can be simulated locally on the watch by adb:
 * <li>adb root</li>
 * <li>adb shell settings put secure cw_device_enabled 0</li>
 *
 * <p>By default it is assumed that a device is enabled unless the setting is explicitly written.
 */
public class DeviceEnableSetting {
    private static final String TAG = "DeviceEnableSetting";

    // This is the "API" of exposed by Framework via Settings, via this class.
    public static final String DEVICE_ENABLED_SECURE_SETTINGS_KEY =
            "cw_device_enabled";

    // Make these public so the Mediators can initialize with correct defaults.
    public static final int STATE_DISABLED = 0;
    public static final int STATE_ENABLED = 1;
    public static final int DEVICE_ENABLED_SETTING_DEFAULT = STATE_ENABLED;

    public interface Listener {
        void onDeviceEnableChanged();
    }

    private enum Radio {
        CELLULAR,
        WIFI
    }

    private final ContentResolver mContentResolver;
    private final SettingsObserver mSettingsObserver;
    private final Set<Radio> mAffectedRadios = new HashSet<>();

    private HashSet<Listener> mListeners = new HashSet<>();

    private final AtomicBoolean mDeviceEnabled = new AtomicBoolean(true);

    public DeviceEnableSetting(Context context, ContentResolver contentResolver) {
        mContentResolver = contentResolver;
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
        for (Uri uri : getObservedUris()) {
            mContentResolver.registerContentObserver(uri, false, mSettingsObserver);
        }
        Resources wearableResources = WearResourceUtil.getWearableResources(context);
        if (wearableResources != null) {
            populateAffectedRadios(wearableResources.getStringArray(
                    com.android.wearable.resources.R.array.config_wearDeviceEnableRadios));
        }
    }

    public boolean affectsWifi() {
        return mAffectedRadios.contains(Radio.WIFI);
    }

    public boolean affectsCellular() {
        return mAffectedRadios.contains(Radio.CELLULAR);
    }

    @VisibleForTesting
    void populateAffectedRadios(String radios[]) {
        if (radios == null) {
            return;
        }
        for (String radio : radios) {
            if ("wifi".equalsIgnoreCase(radio)) {
                mAffectedRadios.add(Radio.WIFI);
            } else if ("cellular".equalsIgnoreCase(radio)) {
                mAffectedRadios.add(Radio.CELLULAR);
            }
        }
    }

    @VisibleForTesting
    SettingsObserver getSettingsObserver() {
        return mSettingsObserver;
    }

    private List<Uri> getObservedUris() {
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(Settings.Secure.getUriFor(DEVICE_ENABLED_SECURE_SETTINGS_KEY));
        return uris;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public boolean isDeviceEnabled() {
        return Settings.Secure.getInt(
                mContentResolver,
                DEVICE_ENABLED_SECURE_SETTINGS_KEY,
                DEVICE_ENABLED_SETTING_DEFAULT) == STATE_ENABLED;
    }

    /** Returns true if the setting was changed. */
    private boolean updateDeviceEnabledFromSettings() {
        boolean prev = mDeviceEnabled.get();
        boolean newSetting = Settings.Secure.getInt(
                mContentResolver,
                DEVICE_ENABLED_SECURE_SETTINGS_KEY,
                DEVICE_ENABLED_SETTING_DEFAULT) == STATE_ENABLED;
        if (prev == newSetting) {
            return false;
        }
        mDeviceEnabled.set(newSetting);
        return true;
    }

    final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.Secure.getUriFor(DEVICE_ENABLED_SECURE_SETTINGS_KEY))) {
                if (updateDeviceEnabledFromSettings()) {
                    for (Listener listener : mListeners) {
                        listener.onDeviceEnableChanged();
                    }
                }
            }
        }
    }
}
