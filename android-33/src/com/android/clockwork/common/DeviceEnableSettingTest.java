package com.android.clockwork.common;

import android.content.ContentResolver;
import android.provider.Settings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.android.clockwork.common.DeviceEnableSetting.DEVICE_ENABLED_SECURE_SETTINGS_KEY;
import static com.android.clockwork.common.DeviceEnableSetting.DEVICE_ENABLED_SETTING_DEFAULT;
import static com.android.clockwork.common.DeviceEnableSetting.STATE_DISABLED;
import static com.android.clockwork.common.DeviceEnableSetting.STATE_ENABLED;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(RobolectricTestRunner.class)
public class DeviceEnableSettingTest {
    ContentResolver cr;

    @Mock DeviceEnableSetting.Listener mockListener;
    DeviceEnableSetting mDeviceEnableSetting;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        cr = RuntimeEnvironment.application.getContentResolver();
        mDeviceEnableSetting = new DeviceEnableSetting(RuntimeEnvironment.application, cr);
        mDeviceEnableSetting.addListener(mockListener);
    }

    @Test
    public void testGettersReturnDefaultValues() {
        // settings which default to ON
        Assert.assertEquals(
                DEVICE_ENABLED_SETTING_DEFAULT == STATE_ENABLED,
                mDeviceEnableSetting.isDeviceEnabled());
    }

    @Test
    public void testGettersForNonDefaultValues() {
        Settings.Secure.putInt(cr, DEVICE_ENABLED_SECURE_SETTINGS_KEY, STATE_DISABLED);
        Assert.assertFalse(mDeviceEnableSetting.isDeviceEnabled());
        Settings.Secure.putInt(cr, DEVICE_ENABLED_SECURE_SETTINGS_KEY, STATE_ENABLED);
        Assert.assertTrue(mDeviceEnableSetting.isDeviceEnabled());
    }

    @Test
    public void testPopulateAffectedRadios() {
        mDeviceEnableSetting.populateAffectedRadios(new String[0]);
        Assert.assertFalse(mDeviceEnableSetting.affectsWifi());
        Assert.assertFalse(mDeviceEnableSetting.affectsCellular());

        mDeviceEnableSetting.populateAffectedRadios(new String[]{"wifi"});
        Assert.assertTrue(mDeviceEnableSetting.affectsWifi());
        Assert.assertFalse(mDeviceEnableSetting.affectsCellular());

        mDeviceEnableSetting.populateAffectedRadios(new String[]{"wifi", "cellular"});
        Assert.assertTrue(mDeviceEnableSetting.affectsWifi());
        Assert.assertTrue(mDeviceEnableSetting.affectsCellular());
    }

    @Test
    public void testPopulateAffectedRadios_invalidInputs() {
        mDeviceEnableSetting.populateAffectedRadios(new String[0]);
        Assert.assertFalse(mDeviceEnableSetting.affectsWifi());
        Assert.assertFalse(mDeviceEnableSetting.affectsCellular());

        mDeviceEnableSetting.populateAffectedRadios(new String[]{"asdfjk", "foobar"});
        Assert.assertFalse(mDeviceEnableSetting.affectsWifi());
        Assert.assertFalse(mDeviceEnableSetting.affectsCellular());

        mDeviceEnableSetting.populateAffectedRadios(
                new String[]{"asdfjkhkj", "wifi", "cellular", "sasafkjh"});
        Assert.assertTrue(mDeviceEnableSetting.affectsWifi());
        Assert.assertTrue(mDeviceEnableSetting.affectsCellular());
    }

    @Test
    public void testNotifyListeners() {
        DeviceEnableSetting.SettingsObserver obs = mDeviceEnableSetting.getSettingsObserver();

        Settings.Secure.putInt(cr, DEVICE_ENABLED_SECURE_SETTINGS_KEY, STATE_DISABLED);
        obs.onChange(false, Settings.Secure.getUriFor(DEVICE_ENABLED_SECURE_SETTINGS_KEY));
        verify(mockListener).onDeviceEnableChanged();
        reset(mockListener);

        Settings.Secure.putInt(cr, DEVICE_ENABLED_SECURE_SETTINGS_KEY, STATE_ENABLED);
        obs.onChange(false, Settings.Secure.getUriFor(DEVICE_ENABLED_SECURE_SETTINGS_KEY));
        verify(mockListener).onDeviceEnableChanged();
        reset(mockListener);
    }

    @Test
    public void testNotifyListeners_doNotNotifyIfSettingDidNotChange() {
        DeviceEnableSetting.SettingsObserver obs = mDeviceEnableSetting.getSettingsObserver();

        Settings.Secure.putInt(cr, DEVICE_ENABLED_SECURE_SETTINGS_KEY, STATE_DISABLED);
        obs.onChange(false, Settings.Secure.getUriFor(DEVICE_ENABLED_SECURE_SETTINGS_KEY));
        verify(mockListener).onDeviceEnableChanged();
        reset(mockListener);

        obs.onChange(false, Settings.Secure.getUriFor(DEVICE_ENABLED_SECURE_SETTINGS_KEY));
        obs.onChange(false, Settings.Secure.getUriFor(DEVICE_ENABLED_SECURE_SETTINGS_KEY));
        verifyNoMoreInteractions(mockListener);
    }
}
