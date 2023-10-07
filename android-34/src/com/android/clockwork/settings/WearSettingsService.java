package com.android.clockwork.settings;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.SystemProperties;
import android.provider.Settings;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.SystemService;

/** Service for Wear Settings related features. */
public class WearSettingsService extends SystemService {
    @VisibleForTesting
    static final String PLATFORM_MR_PROP_KEY = "ro.cw_build.platform_mr";

    private final Context mContext;
    private final WearPersistentSettingsObserver mSettingsObserver;

    @VisibleForTesting
    WearSettingsService(Context context, WearPersistentSettingsObserver settingsObserver) {
        super(context);
        mContext = context;
        mSettingsObserver = settingsObserver;
    }

    public WearSettingsService(Context context) {
        this(context, new WearPersistentSettingsObserver(context));
    }

    @Override
    public void onStart() {}

    private void setDefaultValueForCombinedLocationEnabledIfNeeded() {
        if (Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.Wearable.COMBINED_LOCATION_ENABLED) != null) {
            return;
        }

        LocationManager locationManager = mContext.getSystemService(LocationManager.class);
        boolean isMainLocationSettingsEnabled = locationManager
                .isLocationEnabled();
        boolean isWatchLocationEnabled = isMainLocationSettingsEnabled &&
                mContext.getPackageManager()
                        .hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isPhoneLocationEnabled = isMainLocationSettingsEnabled  &&
                Settings.Global.getInt(mContext.getContentResolver(),
                        Settings.Global.Wearable.OBTAIN_PAIRED_DEVICE_LOCATION, 0) == 1;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.Wearable.COMBINED_LOCATION_ENABLED,
                (isWatchLocationEnabled || isPhoneLocationEnabled) ? 1 : 0);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != SystemService.PHASE_BOOT_COMPLETED) {
            return;
        }

        setDefaultValueForCombinedLocationEnabledIfNeeded();

        // Set platform MR number
        int platformMrNumber = SystemProperties.getInt(PLATFORM_MR_PROP_KEY, 0);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.Wearable.WEAR_PLATFORM_MR_NUMBER, platformMrNumber);

        // Start observing Setting value changes
        mSettingsObserver.startObserving();
    }
}
