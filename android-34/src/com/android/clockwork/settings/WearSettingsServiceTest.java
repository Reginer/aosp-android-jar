package com.android.clockwork.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.location.LocationManager;
import android.os.SystemProperties;
import android.provider.Settings;

import com.android.server.SystemService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowSettings;

@RunWith(RobolectricTestRunner.class)
public final class WearSettingsServiceTest {
    @Mock private WearPersistentSettingsObserver mockObserver;

    private Context mContext;
    private WearSettingsService mService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mService = new WearSettingsService(mContext, mockObserver);
    }

    @Test
    public void bootComplete_startsSettingsObserver() {
        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

        verify(mockObserver).startObserving();
    }

    @Test
    public void bootComplete_setsPlatformMrNumber() {
        SystemProperties.set(WearSettingsService.PLATFORM_MR_PROP_KEY, "12");

        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

        int settingsPlatformMrNumber = Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.Wearable.WEAR_PLATFORM_MR_NUMBER, 0);
        assertThat(settingsPlatformMrNumber).isEqualTo(12);
    }

    @Test
    public void bootComplete_setDefaultValueForCombinedLocationEnabledIfNeeded() {
        LocationManager locationManager = mContext.getSystemService(LocationManager.class);
        locationManager.setLocationEnabledForUser(true, mContext.getUser());
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.Wearable.OBTAIN_PAIRED_DEVICE_LOCATION, 1);

        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.Wearable.COMBINED_LOCATION_ENABLED, 0)).isEqualTo(1);

        ShadowSettings.ShadowGlobal.reset();
        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.Wearable.COMBINED_LOCATION_ENABLED, 0)).isEqualTo(0);
    }
}
