package com.android.clockwork.bluetooth;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Settings wrapper for WearBluetoothMediator.
 */
public class WearBluetoothMediatorSettings {
    /**
     * Expectation for the value of this Setting is a semicolon-separated list of Strings which
     * are Base64-NOWRAP encoded byte arrays.  The byte arrays are InetAddress.getAddress() and
     * can be used to reconstruct an InetAddress using InetAddress.getByAddress().
     */
    @VisibleForTesting
    static final String PROXY_DNS_SECURE_SETTING = "cw_proxy_dns_servers";
    @VisibleForTesting
    static final String BLUETOOTH_SETTINGS_PREF_KEY = "cw_bt_settings_pref";
    private static final String TAG = WearBluetoothSettings.LOG_TAG;
    private static final int BLUETOOTH_SETTINGS_PREF_OFF = 0;
    private static final int BLUETOOTH_SETTINGS_PREF_ON = 1;
    private static final int AIRPLANE_MODE_OFF = 0;
    private static final int AIRPLANE_MODE_ON = 1;
    private final ContentResolver mContentResolver;
    private final SettingsObserver mSettingsObserver;
    private final HashSet<Listener> mListeners = new HashSet<>();
    public WearBluetoothMediatorSettings(ContentResolver contentResolver) {
        mContentResolver = contentResolver;

        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
        for (Uri uri : getObservedUris()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Registering content observer for " + uri);
            }
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
        uris.add(Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));
        uris.add(Settings.Secure.getUriFor(PROXY_DNS_SECURE_SETTING));
        uris.add(Settings.System.getUriFor(BLUETOOTH_SETTINGS_PREF_KEY));
        return uris;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * By default, don't include IPv6 DNS servers because sysproxy does not currently support IPv6.
     */
    List<InetAddress> getDnsServers() {
        return getDnsServers(false);
    }

    List<InetAddress> getDnsServers(boolean includeIpV6) {
        List<InetAddress> dnsServers = new ArrayList<>();
        String settingString = Settings.Secure.getString(
                mContentResolver, PROXY_DNS_SECURE_SETTING);
        if (TextUtils.isEmpty(settingString)) {
            return dnsServers;
        }
        for (String encodedServerString : settingString.split(";")) {
            try {
                byte[] inetAddressBytes = Base64.decode(encodedServerString.trim(), Base64.NO_WRAP);
                InetAddress inetAddress = InetAddress.getByAddress(inetAddressBytes);
                if (includeIpV6 || inetAddress instanceof Inet4Address) {
                    dnsServers.add(inetAddress);
                }
            } catch (UnknownHostException e) {
                Log.w(TAG, "Received invalid InetAddress from setting: " + encodedServerString);
            }
        }
        return dnsServers;
    }

    boolean getIsSettingsPreferenceBluetoothOn() {
        return Settings.System.getInt(
                mContentResolver, BLUETOOTH_SETTINGS_PREF_KEY, BLUETOOTH_SETTINGS_PREF_ON)
                == BLUETOOTH_SETTINGS_PREF_ON;
    }

    void setSettingsPreferenceBluetoothOn(final boolean isSettingsPreferenceBluetoothOn) {
        Settings.System.putInt(mContentResolver, BLUETOOTH_SETTINGS_PREF_KEY,
                isSettingsPreferenceBluetoothOn
                        ? BLUETOOTH_SETTINGS_PREF_ON
                        : BLUETOOTH_SETTINGS_PREF_OFF);
    }

    boolean getIsInAirplaneMode() {
        return Settings.Global.getInt(
                mContentResolver, Settings.Global.AIRPLANE_MODE_ON, AIRPLANE_MODE_OFF)
                == AIRPLANE_MODE_ON;
    }

    interface Listener {
        void onAirplaneModeSettingChanged(boolean isAirplaneModeOn);

        void onSettingsPreferenceBluetoothSettingChanged(
                boolean isSettingsPreferenceBluetoothOn);

        void onDnsServersChanged();
    }

    @VisibleForTesting
    final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON).equals(uri)) {
                final boolean isAirplaneModeOn = getIsInAirplaneMode();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onChange Airplane mode turned "
                            + (isAirplaneModeOn ? "on" : "off"));
                }
                for (Listener listener : mListeners) {
                    listener.onAirplaneModeSettingChanged(isAirplaneModeOn);
                }
            } else if (Settings.Secure.getUriFor(PROXY_DNS_SECURE_SETTING).equals(uri)) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DNS Settings changed");
                }
                for (Listener listener : mListeners) {
                    listener.onDnsServersChanged();
                }
            } else if (Settings.System.getUriFor(BLUETOOTH_SETTINGS_PREF_KEY).equals(uri)) {
                final boolean settingsPreferenceBluetoothOn = getIsSettingsPreferenceBluetoothOn();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onChange settings bluetooth preference radio turned "
                            + (settingsPreferenceBluetoothOn ? "on" : "off"));
                }
                for (Listener listener : mListeners) {
                    listener.onSettingsPreferenceBluetoothSettingChanged(
                            settingsPreferenceBluetoothOn);
                }
            }
        }
    }
}
