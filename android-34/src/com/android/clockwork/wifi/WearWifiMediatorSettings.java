package com.android.clockwork.wifi;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Settings wrapper for WearWifiMediator.
 *
 * go/cw-wifi-settings-management-f
 */
public class WearWifiMediatorSettings {
    /** valid values for this key: "on", "off", "off_airplane" */
    static final String WIFI_SETTING_KEY = "clockwork_wifi_setting";
    static final String WIFI_SETTING_ON = "on";
    static final String WIFI_SETTING_OFF = "off";
    static final String WIFI_SETTING_OFF_AIRPLANE = "off_airplane";
    /** valid values for this key are 0 and 1 */
    static final String IN_WIFI_SETTINGS_KEY = "clockwork_in_wifi_settings";
    /**
     * Dev Option which can be used to prevent WifiMediator from being affected by
     * whether the device is charging or not.  This makes it possible to debug WifiMediator
     * with the device plugged in.
     */
    static final String ENABLE_WIFI_WHEN_CHARGING_KEY = "cw_enable_wifi_when_charging";
    /**
     * Dev Option for disabling WifiMediator. Used for testing only.
     */
    static final String DISABLE_WIFI_MEDIATOR_KEY = "cw_disable_wifimediator";
    /**
     * Hardware Low Power Mode causes WiFi to be disabled and prevents the user and all apps
     * from re-enabling WiFi.  This is used by device-specific software to prevent potential
     * brownouts caused by the power spikes from the WiFi adapter during very low-power states.
     */
    static final String HW_LOW_POWER_MODE_KEY = "wifi_hw_low_power_mode";
    /**
     * Time to delay turning on wifi after boot. This allows a chance for Bluetooth to provide
     * connectivity and avoids an unnecessary transition to ON_PROXY_DISCONNECTED.
     */
    static final String WIFI_ON_BOOT_DELAY_MS_KEY = "config.wifi_on_boot_delay_ms";
    static final long WIFI_ON_BOOT_DELAY_MS_DEFAULT = TimeUnit.SECONDS.toMillis(30);
    private static final String TAG = "WearWifiMediator";
    private final ContentResolver mContentResolver;
    private final HashSet<Listener> mListeners = new HashSet<>();
    private final SettingsObserver mSettingsObserver;
    public WearWifiMediatorSettings(ContentResolver contentResolver) {
        mContentResolver = contentResolver;

        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
        for (Uri uri : getObservedUris()) {
            Log.d(TAG, "Registering content observer for " + uri);
            mContentResolver.registerContentObserver(uri, false, mSettingsObserver);
        }
    }

    @VisibleForTesting
    SettingsObserver getSettingsObserver() {
        return mSettingsObserver;
    }

    @VisibleForTesting
    List<Uri> getObservedUris() {
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(Settings.System.getUriFor(WIFI_SETTING_KEY));
        uris.add(Settings.System.getUriFor(IN_WIFI_SETTINGS_KEY));
        uris.add(Settings.System.getUriFor(ENABLE_WIFI_WHEN_CHARGING_KEY));
        uris.add(Settings.Global.getUriFor(DISABLE_WIFI_MEDIATOR_KEY));
        uris.add(Settings.Global.getUriFor(HW_LOW_POWER_MODE_KEY));
        uris.add(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));
        uris.add(Settings.Global.getUriFor(Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED));
        return uris;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public boolean getWifiOnWhenProxyDisconnected() {
        return Settings.Global.getInt(mContentResolver,
                Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED, 1) == 1;
    }

    public boolean getIsInAirplaneMode() {
        return Settings.Global.getInt(
                mContentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    public String getWifiSetting() {
        String value = Settings.System.getString(mContentResolver, WIFI_SETTING_KEY);
        if (value == null) {
            // Allow device overlay to provide default setting.
            // Default to ON if global setting is missing too.
            int wifiOn = Settings.System.getInt(mContentResolver,
                    Settings.Global.WIFI_ON, /* def = */ 1);
            value = wifiOn != 0 ? WIFI_SETTING_ON : WIFI_SETTING_OFF;
        }
        return value;
    }

    public void putWifiSetting(String wifiSetting) {
        Settings.System.putString(mContentResolver, WIFI_SETTING_KEY, wifiSetting);
    }

    public boolean getInWifiSettings() {
        return Settings.System.getInt(mContentResolver, IN_WIFI_SETTINGS_KEY, 0) != 0;
    }

    public boolean getEnableWifiWhileCharging() {
        return Settings.System.getInt(mContentResolver, ENABLE_WIFI_WHEN_CHARGING_KEY, 1) != 0;
    }

    public boolean getDisableWifiMediator() {
        return Settings.Global.getInt(mContentResolver, DISABLE_WIFI_MEDIATOR_KEY, 0) != 0;
    }

    public boolean getHardwareLowPowerMode() {
        return Settings.Global.getInt(mContentResolver, HW_LOW_POWER_MODE_KEY, 0) != 0;
    }

    public long getWifiOnBootDelayMs() {
        return SystemProperties.getLong(
                WIFI_ON_BOOT_DELAY_MS_KEY,
                WIFI_ON_BOOT_DELAY_MS_DEFAULT);
    }

    public String updateWifiSettingOnAirplaneModeChange(String currentWifiSetting,
            boolean airplaneModeOn) {
        String newSetting = currentWifiSetting;
        if (airplaneModeOn) {
            // when entering airplane mode, the WIFI_SETTING conversion should be:
            // ON -> OFF
            // OFF -> OFF_AIRPLANE
            if (WIFI_SETTING_ON.equals(currentWifiSetting)) {
                newSetting = WIFI_SETTING_OFF;
            } else {
                newSetting = WIFI_SETTING_OFF_AIRPLANE;
            }
        } else {
            // when leaving airplane mode, the WIFI_SETTING conversion should be:
            // ON -> ON
            // OFF -> ON
            // OFF_AIRPLANE -> OFF
            if (WIFI_SETTING_OFF.equals(currentWifiSetting)) {
                newSetting = WIFI_SETTING_ON;
            } else if (WIFI_SETTING_OFF_AIRPLANE.equals(currentWifiSetting)) {
                newSetting = WIFI_SETTING_OFF;
            }
        }

        if (!newSetting.equals(currentWifiSetting)) {
            putWifiSetting(newSetting);
        }
        return newSetting;
    }

    interface Listener {
        void onWifiSettingChanged(String newWifiSetting);

        void onInWifiSettingsMenuChanged(boolean inWifiSettingsMenu);

        void onEnableWifiWhileChargingChanged(boolean enableWifiWhileCharging);

        void onDisableWifiMediatorChanged(boolean disableWifiMediator);

        void onHardwareLowPowerModeChanged(boolean inHardwareLowPowerMode);

        void onWifiOnWhenProxyDisconnectedChanged(boolean wifiOnWhenProxyDisconnected);
    }

    @VisibleForTesting
    final class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "onChange called");
            if (uri.equals(Settings.System.getUriFor(WIFI_SETTING_KEY))) {
                String newWifiSetting = getWifiSetting();
                Log.d(TAG, "Wifi Setting changed to: " + newWifiSetting);
                for (Listener listener : mListeners) {
                    listener.onWifiSettingChanged(newWifiSetting);
                }
            } else if (uri.equals(Settings.System.getUriFor(IN_WIFI_SETTINGS_KEY))) {
                boolean inWifiSettings = getInWifiSettings();
                Log.d(TAG, "In WiFi Settings Menu state changed: " + inWifiSettings);
                for (Listener listener : mListeners) {
                    listener.onInWifiSettingsMenuChanged(inWifiSettings);
                }
            } else if (uri.equals(Settings.System.getUriFor(ENABLE_WIFI_WHEN_CHARGING_KEY))) {
                boolean enableWhileCharging = getEnableWifiWhileCharging();
                Log.d(TAG, "Enable WiFi while charging changed to: " + enableWhileCharging);
                for (Listener listener : mListeners) {
                    listener.onEnableWifiWhileChargingChanged(getEnableWifiWhileCharging());
                }
            } else if (uri.equals(Settings.Global.getUriFor(DISABLE_WIFI_MEDIATOR_KEY))) {
                boolean disableWifiMediator = getDisableWifiMediator();
                Log.d(TAG, "Disable WifiMediator set to: " + disableWifiMediator);
                for (Listener listener : mListeners) {
                    listener.onDisableWifiMediatorChanged(disableWifiMediator);
                }
            } else if (uri.equals(Settings.Global.getUriFor(HW_LOW_POWER_MODE_KEY))) {
                boolean hardwareLowPowerMode = getHardwareLowPowerMode();
                Log.d(TAG, "Hardware Low Power Mode changed to: " + hardwareLowPowerMode);
                for (Listener listener : mListeners) {
                    listener.onHardwareLowPowerModeChanged(hardwareLowPowerMode);
                }
            } else if (uri.equals(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON))) {
                boolean isAirplaneModeOn = getIsInAirplaneMode();
                Log.d(TAG, "Airplane Mode turned " + (isAirplaneModeOn ? "on" : "off"));

                String oldWifiSetting = getWifiSetting();
                String newWifiSetting = updateWifiSettingOnAirplaneModeChange(oldWifiSetting,
                        isAirplaneModeOn);

                if (!oldWifiSetting.equals(newWifiSetting)) {
                    for (Listener listener : mListeners) {
                        listener.onWifiSettingChanged(newWifiSetting);
                    }
                }
            } else if (uri.equals(Settings.Global.getUriFor(
                    Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED))) {
                boolean wifiOnWhenProxyDisconnected = getWifiOnWhenProxyDisconnected();
                Log.d(TAG, "Turn Wifi On After Proxy Disconnected changed to: "
                        + (wifiOnWhenProxyDisconnected ? "true" : "false"));
                for (Listener listener : mListeners) {
                    listener.onWifiOnWhenProxyDisconnectedChanged(wifiOnWhenProxyDisconnected);
                }
            }
        }
    }
}
