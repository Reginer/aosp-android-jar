package com.android.clockwork.settings;

import static android.provider.Settings.Global.Wearable.COMBINED_LOCATION_ENABLED;

import android.content.ContentResolver;
import android.content.Context;
import android.location.LocationManager;
import android.os.UserHandle;
import android.net.Uri;
import android.provider.Settings;

final class CombinedLocationSettingObserver implements WearPersistentSettingsObserver.Observer {
    private final Context mContext;

    CombinedLocationSettingObserver(Context context) {
        mContext = context;
    }

    @Override
    public void onChange() {
        ContentResolver contentResolver = mContext.getContentResolver();
        int combinedLocationEnabled = Settings.Global.getInt(
                contentResolver, COMBINED_LOCATION_ENABLED, 0);

        // Enable main location setting
        int userId = UserHandle.myUserId();
        Settings.Secure.putIntForUser(
                contentResolver,
                Settings.Secure.LOCATION_CHANGER,
                Settings.Secure.LOCATION_CHANGER_SYSTEM_SETTINGS,
                userId);
        LocationManager locationManager = mContext.getSystemService(LocationManager.class);
        locationManager.setLocationEnabledForUser(
                combinedLocationEnabled == 1, UserHandle.of(userId));

        // Enable paired device location setting
        Settings.Global.putInt(contentResolver,
                Settings.Global.Wearable.OBTAIN_PAIRED_DEVICE_LOCATION,
                combinedLocationEnabled);
    }

    @Override
    public Uri getUri() {
        return Settings.Global.getUriFor(COMBINED_LOCATION_ENABLED);
    }
}