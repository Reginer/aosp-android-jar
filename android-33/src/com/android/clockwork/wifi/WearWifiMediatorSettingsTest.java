package com.android.clockwork.wifi;

import static com.android.clockwork.wifi.WearWifiMediatorSettings.DISABLE_WIFI_MEDIATOR_KEY;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.ENABLE_WIFI_WHEN_CHARGING_KEY;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.HW_LOW_POWER_MODE_KEY;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.IN_WIFI_SETTINGS_KEY;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.WIFI_ON_BOOT_DELAY_MS_DEFAULT;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.WIFI_ON_BOOT_DELAY_MS_KEY;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.WIFI_SETTING_KEY;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.WIFI_SETTING_OFF;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.WIFI_SETTING_OFF_AIRPLANE;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.WIFI_SETTING_ON;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.os.SystemProperties;
import android.provider.Settings;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WearWifiMediatorSettingsTest {
    ContentResolver cr;

    @Mock
    WearWifiMediatorSettings.Listener mockListener;
    WearWifiMediatorSettings mWifiSettings;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        cr = RuntimeEnvironment.application.getContentResolver();
        mWifiSettings = new WearWifiMediatorSettings(cr);
        mWifiSettings.addListener(mockListener);
    }

    // Shadows.shadowOf currently won't build in our tree for some really
    // weird reason.  This test should work otherwise. (b/34975736)
    // @Test
    // public void testContentObserverRegistered() {
    //    WearWifiMediatorSettings.SettingsObserver obs = mWifiSettings.getSettingsObserver();
    //     mWifiSettings.
    //     ShadowContentResolver scr = Shadows.shadowOf(cr);
    //     for (Uri uri : mWifiSettings.getObservedUris()) {
    //         Assert.assertEquals(1, scr.getContentObservers(uri).size());
    //         Assert.assertEquals(obs, scr.getContentObservers(uri).get(0));
    //     }
    // }

    @Test
    public void testGettersDefaultReturnValues() {
        // settings which default to ON
        Assert.assertEquals(WIFI_SETTING_ON, mWifiSettings.getWifiSetting());
        Assert.assertTrue(mWifiSettings.getEnableWifiWhileCharging());
        Assert.assertTrue(mWifiSettings.getWifiOnWhenProxyDisconnected());

        // settings which default to OFF
        Assert.assertFalse(mWifiSettings.getDisableWifiMediator());
        Assert.assertFalse(mWifiSettings.getInWifiSettings());
        Assert.assertFalse(mWifiSettings.getHardwareLowPowerMode());
        Assert.assertFalse(mWifiSettings.getIsInAirplaneMode());
    }

    @Test
    public void testGettersForNonDefaultValues() {
        Settings.System.putInt(cr, ENABLE_WIFI_WHEN_CHARGING_KEY, 0);
        Assert.assertFalse(mWifiSettings.getEnableWifiWhileCharging());

        Settings.Global.putInt(cr, DISABLE_WIFI_MEDIATOR_KEY, 1);
        Assert.assertTrue(mWifiSettings.getDisableWifiMediator());

        Settings.System.putInt(cr, IN_WIFI_SETTINGS_KEY, 1);
        Assert.assertTrue(mWifiSettings.getInWifiSettings());

        Settings.System.putString(cr, WIFI_SETTING_KEY, WIFI_SETTING_OFF);
        Assert.assertEquals(WIFI_SETTING_OFF, mWifiSettings.getWifiSetting());

        Settings.Global.putInt(cr, HW_LOW_POWER_MODE_KEY, 1);
        Assert.assertTrue(mWifiSettings.getHardwareLowPowerMode());

        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        Assert.assertTrue(mWifiSettings.getIsInAirplaneMode());

        Settings.Global.putInt(cr, Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED, 0);
        Assert.assertFalse(mWifiSettings.getWifiOnWhenProxyDisconnected());

        Assert.assertEquals(mWifiSettings.getWifiOnBootDelayMs(), WIFI_ON_BOOT_DELAY_MS_DEFAULT);
        SystemProperties.set(WIFI_ON_BOOT_DELAY_MS_KEY, "123");
        Assert.assertEquals(mWifiSettings.getWifiOnBootDelayMs(), 123);
    }

    @Test
    public void testNotifyListeners() {
        WearWifiMediatorSettings.SettingsObserver obs = mWifiSettings.getSettingsObserver();

        Settings.System.putInt(cr, ENABLE_WIFI_WHEN_CHARGING_KEY, 0);
        obs.onChange(false, Settings.System.getUriFor(ENABLE_WIFI_WHEN_CHARGING_KEY));
        verify(mockListener).onEnableWifiWhileChargingChanged(false);
        reset(mockListener);

        Settings.Global.putInt(cr, DISABLE_WIFI_MEDIATOR_KEY, 1);
        obs.onChange(false, Settings.Global.getUriFor(DISABLE_WIFI_MEDIATOR_KEY));
        verify(mockListener).onDisableWifiMediatorChanged(true);
        reset(mockListener);

        Settings.System.putInt(cr, IN_WIFI_SETTINGS_KEY, 1);
        obs.onChange(false, Settings.System.getUriFor(IN_WIFI_SETTINGS_KEY));
        verify(mockListener).onInWifiSettingsMenuChanged(true);
        reset(mockListener);

        Settings.System.putString(cr, WIFI_SETTING_KEY, WIFI_SETTING_OFF);
        obs.onChange(false, Settings.System.getUriFor(WIFI_SETTING_KEY));
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_OFF);
        reset(mockListener);

        Settings.Global.putInt(cr, HW_LOW_POWER_MODE_KEY, 1);
        obs.onChange(false, Settings.Global.getUriFor(HW_LOW_POWER_MODE_KEY));
        verify(mockListener).onHardwareLowPowerModeChanged(true);
        reset(mockListener);

        Settings.Global.putInt(cr, Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED, 0);
        obs.onChange(false, Settings.Global.getUriFor(
                Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED));
        verify(mockListener).onWifiOnWhenProxyDisconnectedChanged(false);
        reset(mockListener);
    }

    @Test
    public void testGetAndPutWifiSetting() {
        mWifiSettings.putWifiSetting(WIFI_SETTING_OFF_AIRPLANE);
        Assert.assertEquals(WIFI_SETTING_OFF_AIRPLANE, mWifiSettings.getWifiSetting());

        mWifiSettings.putWifiSetting(WIFI_SETTING_OFF);
        Assert.assertEquals(WIFI_SETTING_OFF, mWifiSettings.getWifiSetting());

        mWifiSettings.putWifiSetting(WIFI_SETTING_ON);
        Assert.assertEquals(WIFI_SETTING_ON, mWifiSettings.getWifiSetting());

        mWifiSettings.putWifiSetting(WIFI_SETTING_OFF_AIRPLANE);
        Assert.assertEquals(WIFI_SETTING_OFF_AIRPLANE, mWifiSettings.getWifiSetting());
    }

    @Test
    public void testAirplaneModeChangesWifiSettings() {
        WearWifiMediatorSettings.SettingsObserver obs = mWifiSettings.getSettingsObserver();

        // default setup is airplane mode OFF and WiFi Setting ON
        // now turn airplane mode ON and verify that WiFi goes to OFF
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));

        Assert.assertTrue(mWifiSettings.getIsInAirplaneMode());
        Assert.assertEquals(WIFI_SETTING_OFF, mWifiSettings.getWifiSetting());
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_OFF);
        reset(mockListener);

        // turn airplane mode back OFF and verify that WiFi returns to ON
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));

        Assert.assertFalse(mWifiSettings.getIsInAirplaneMode());
        Assert.assertEquals(WIFI_SETTING_ON, mWifiSettings.getWifiSetting());
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_ON);

        // now set WiFi Setting to be OFF before we turn on airplane mode again
        Settings.System.putString(cr, WIFI_SETTING_KEY, WIFI_SETTING_OFF);
        reset(mockListener);

        // WiFi should now be in OFF_AIRPLANE
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));

        Assert.assertTrue(mWifiSettings.getIsInAirplaneMode());
        Assert.assertEquals(WIFI_SETTING_OFF_AIRPLANE, mWifiSettings.getWifiSetting());
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_OFF_AIRPLANE);
        reset(mockListener);

        // turning airplane mode back OFF should switch wifi back to OFF
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));

        Assert.assertFalse(mWifiSettings.getIsInAirplaneMode());
        Assert.assertEquals(WIFI_SETTING_OFF, mWifiSettings.getWifiSetting());
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_OFF);
        reset(mockListener);
    }

    @Test
    public void testWifiChangesWhileInAirplaneMode() {
        WearWifiMediatorSettings.SettingsObserver obs = mWifiSettings.getSettingsObserver();

        // default setup is airplane mode OFF and WiFi Setting ON
        // now turn airplane mode ON and verify that WiFi goes to OFF
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));
        Assert.assertTrue(mWifiSettings.getIsInAirplaneMode());
        Assert.assertEquals(WIFI_SETTING_OFF, mWifiSettings.getWifiSetting());
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_OFF);
        reset(mockListener);

        // now toggle WiFi to ON while inside airplane mode
        Settings.System.putString(cr, WIFI_SETTING_KEY, WIFI_SETTING_ON);
        obs.onChange(false, Settings.System.getUriFor(WIFI_SETTING_KEY));
        Assert.assertEquals(WIFI_SETTING_ON, mWifiSettings.getWifiSetting());
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_ON);
        reset(mockListener);

        // toggling airplane mode back OFF should keep WiFi ON
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));
        Assert.assertEquals(WIFI_SETTING_ON, mWifiSettings.getWifiSetting());
        verify(mockListener, never()).onWifiSettingChanged(Matchers.anyString());

        // now get back to Airplane Mode and toggle WiFi to OFF_AIRPLANE
        // (WifiSettings will set to OFF_AIRPLANE if user turns WiFi off while
        // inside Airplane Mode)
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));
        reset(mockListener);
        Settings.System.putString(cr, WIFI_SETTING_KEY, WIFI_SETTING_OFF_AIRPLANE);
        obs.onChange(false, Settings.System.getUriFor(WIFI_SETTING_KEY));

        // at this point WiFi should be in OFF_AIRPLANE
        Assert.assertEquals(WIFI_SETTING_OFF_AIRPLANE, mWifiSettings.getWifiSetting());
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_OFF_AIRPLANE);
        reset(mockListener);

        // exiting Airplane Mode should keep WiFi set to OFF
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));
        Assert.assertFalse(mWifiSettings.getIsInAirplaneMode());
        Assert.assertEquals(WIFI_SETTING_OFF, mWifiSettings.getWifiSetting());
        verify(mockListener).onWifiSettingChanged(WIFI_SETTING_OFF);
    }

    @Test
    public void testWifiOnWhenProxyDisconnected() {
        WearWifiMediatorSettings.SettingsObserver obs = mWifiSettings.getSettingsObserver();

        Settings.Global.putInt(cr, Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED, 0);
        obs.onChange(false, Settings.Global.getUriFor(
                Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED));
        Assert.assertFalse(mWifiSettings.getWifiOnWhenProxyDisconnected());
        verify(mockListener).onWifiOnWhenProxyDisconnectedChanged(false);
        reset(mockListener);

        Settings.Global.putInt(cr, Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED, 1);
        obs.onChange(false, Settings.Global.getUriFor(
                Settings.Global.WIFI_ON_WHEN_PROXY_DISCONNECTED));
        Assert.assertTrue(mWifiSettings.getWifiOnWhenProxyDisconnected());
        verify(mockListener).onWifiOnWhenProxyDisconnectedChanged(true);
        reset(mockListener);
    }
}
