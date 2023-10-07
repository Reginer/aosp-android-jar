package com.android.clockwork.settings;

import static android.provider.Settings.Global.Wearable.COMBINED_LOCATION_ENABLED;
import static android.provider.Settings.Global.Wearable.OBTAIN_PAIRED_DEVICE_LOCATION;
import static android.provider.Settings.Secure.LOCATION_CHANGER;
import static android.provider.Settings.Secure.LOCATION_CHANGER_SYSTEM_SETTINGS;
import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.location.LocationManager;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class CombinedLocationSettingObserverTest {
    private Context mContext;

    private CombinedLocationSettingObserver mObserver;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mObserver = new CombinedLocationSettingObserver(mContext);
    }

    @Test
    public void testUri() {
        assertThat(mObserver.getUri()).isEqualTo(
                Settings.Global.getUriFor(COMBINED_LOCATION_ENABLED));
    }

    @Test
    public void testOnChange() {
        ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, COMBINED_LOCATION_ENABLED, 1);

        mObserver.onChange();

        assertThat(Settings.Secure.getInt(contentResolver, LOCATION_CHANGER, 0))
                .isEqualTo(LOCATION_CHANGER_SYSTEM_SETTINGS);
        assertThat(Settings.Global.getInt(contentResolver, OBTAIN_PAIRED_DEVICE_LOCATION, 0))
                .isEqualTo(1);
        LocationManager locationManager = mContext.getSystemService(LocationManager.class);
        assertThat(locationManager.isLocationEnabled()).isTrue();
    }

    @Test
    public void testShouldTriggerObserverOnStart() {
        assertThat(mObserver.shouldTriggerObserverOnStart()).isFalse();
    }
}
