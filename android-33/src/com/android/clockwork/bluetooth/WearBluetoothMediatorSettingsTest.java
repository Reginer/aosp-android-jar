package com.android.clockwork.bluetooth;

import static com.android.clockwork.bluetooth.WearBluetoothMediatorSettings.BLUETOOTH_SETTINGS_PREF_KEY;
import static com.android.clockwork.bluetooth.WearBluetoothMediatorSettings.PROXY_DNS_SECURE_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.Settings;
import android.util.Base64;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.net.InetAddress;

/**
 * Test for {@link WearBluetoothMediatorSettings}
 */
@RunWith(RobolectricTestRunner.class)
public class WearBluetoothMediatorSettingsTest {
    ContentResolver cr;

    @Mock
    WearBluetoothMediatorSettings.Listener mockListener;
    WearBluetoothMediatorSettings mBluetoothSettings;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        cr = RuntimeEnvironment.application.getContentResolver();
        mBluetoothSettings = new WearBluetoothMediatorSettings(cr);
        mBluetoothSettings.addListener(mockListener);
    }

    @Test
    public void testContentObserverRegistered() {
        ShadowContentResolver scr = shadowOf(cr);
        for (Uri uri : mBluetoothSettings.getObservedUris()) {
            Assert.assertEquals(1, scr.getContentObservers(uri).size());
        }
    }

    @Ignore
    @Test
    public void testGettersDefaultReturnValues() {
        Assert.assertTrue(mBluetoothSettings.getIsSettingsPreferenceBluetoothOn());
        Assert.assertFalse(mBluetoothSettings.getIsInAirplaneMode());
    }

    @Test
    public void testGettersForNonDefaultValues() {
        Settings.System.putInt(cr, BLUETOOTH_SETTINGS_PREF_KEY, 0);
        Assert.assertFalse(mBluetoothSettings.getIsSettingsPreferenceBluetoothOn());

        Settings.Global.putInt(cr, Settings.Global.BLUETOOTH_ON, 2);

        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        Assert.assertTrue(mBluetoothSettings.getIsInAirplaneMode());
    }

    @Test
    public void testNotifyListeners() {
        WearBluetoothMediatorSettings.SettingsObserver obs =
                mBluetoothSettings.getSettingsObserver();

        Settings.System.putInt(cr, BLUETOOTH_SETTINGS_PREF_KEY, 1);
        obs.onChange(false, Settings.System.getUriFor(BLUETOOTH_SETTINGS_PREF_KEY));
        verify(mockListener).onSettingsPreferenceBluetoothSettingChanged(true);
        reset(mockListener);

        Settings.System.putInt(cr, BLUETOOTH_SETTINGS_PREF_KEY, 0);
        obs.onChange(false, Settings.System.getUriFor(BLUETOOTH_SETTINGS_PREF_KEY));
        verify(mockListener).onSettingsPreferenceBluetoothSettingChanged(false);
        reset(mockListener);

        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));
        verify(mockListener).onAirplaneModeSettingChanged(true);
        reset(mockListener);

        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));
        verify(mockListener).onAirplaneModeSettingChanged(false);
        reset(mockListener);
    }

    @Test
    public void testGetAndPutUserPreferenceBluetoothOn() {
        mBluetoothSettings.setSettingsPreferenceBluetoothOn(true);
        Assert.assertTrue(mBluetoothSettings.getIsSettingsPreferenceBluetoothOn());

        mBluetoothSettings.setSettingsPreferenceBluetoothOn(false);
        Assert.assertFalse(mBluetoothSettings.getIsSettingsPreferenceBluetoothOn());
    }

    @Test
    public void testAirplaneMode() {
        WearBluetoothMediatorSettings.SettingsObserver obs =
                mBluetoothSettings.getSettingsObserver();

        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));

        Assert.assertTrue(mBluetoothSettings.getIsInAirplaneMode());
        reset(mockListener);

        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0);
        obs.onChange(false, Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON));

        Assert.assertFalse(mBluetoothSettings.getIsInAirplaneMode());
    }

    @Test
    public void testGetDnsServersEmptySetting() {
        Settings.Global.putString(cr, PROXY_DNS_SECURE_SETTING, "");
        assertThat(mBluetoothSettings.getDnsServers()).isEmpty();
    }

    @Test
    public void testDnsServersChangedForIpV4() throws Exception {
        WearBluetoothMediatorSettings.SettingsObserver obs =
                mBluetoothSettings.getSettingsObserver();

        InetAddress ipv4Addr1 = InetAddress.getByName("8.8.8.8");
        InetAddress ipv4Addr2 = InetAddress.getByName("8.8.4.4");

        Settings.Secure.putString(
                cr, PROXY_DNS_SECURE_SETTING, inetAddressesToSettingsString(ipv4Addr1, ipv4Addr2));
        obs.onChange(false, Settings.Secure.getUriFor(PROXY_DNS_SECURE_SETTING));

        verify(mockListener).onDnsServersChanged();
        assertThat(mBluetoothSettings.getDnsServers()).containsExactly(ipv4Addr1, ipv4Addr2);
    }

    @Test
    public void testDnsServersChangedForMixedIpV4IpV6() throws Exception {
        WearBluetoothMediatorSettings.SettingsObserver obs =
                mBluetoothSettings.getSettingsObserver();

        InetAddress ipv4Addr = InetAddress.getByName("100.70.170.110");
        InetAddress ipv6Addr = InetAddress.getByName("2604:ca00:119:aee6::a60:64f8");
        Settings.Secure.putString(
                cr, PROXY_DNS_SECURE_SETTING, inetAddressesToSettingsString(ipv4Addr, ipv6Addr));
        obs.onChange(false, Settings.Secure.getUriFor(PROXY_DNS_SECURE_SETTING));

        verify(mockListener).onDnsServersChanged();
        assertThat(mBluetoothSettings.getDnsServers(true))
                .containsExactly(ipv4Addr, ipv6Addr);
    }

    @Test
    public void testGetDnsServersFilterIpV6() throws Exception {
        InetAddress ipv4Addr = InetAddress.getByName("100.70.170.110");
        InetAddress ipv6Addr = InetAddress.getByName("2604:ca00:119:aee6::a60:64f8");
        Settings.Secure.putString(
                cr, PROXY_DNS_SECURE_SETTING, inetAddressesToSettingsString(ipv4Addr, ipv6Addr));
        assertThat(mBluetoothSettings.getDnsServers(false))
                .containsExactly(ipv4Addr);
    }

    @Test
    public void testDnsServersWithIpv4MappedIpv6Addresses() throws Exception {
        // This is actually 103.30.217.152 mapped onto IPv6
        InetAddress ipv4Mappedv6Address = InetAddress.getByName("::ffff:f:f");
        Settings.Secure.putString(
                cr, PROXY_DNS_SECURE_SETTING, inetAddressesToSettingsString(ipv4Mappedv6Address));
        assertThat(mBluetoothSettings.getDnsServers(false))
                .containsExactly(ipv4Mappedv6Address);
    }

    private String inetAddressesToSettingsString(InetAddress... inetAddresses) {
        StringBuilder sb = new StringBuilder();
        for (InetAddress inetAddress : inetAddresses) {
            sb.append(Base64.encodeToString(inetAddress.getAddress(), Base64.NO_WRAP));
            sb.append(";");
        }
        return sb.toString();

    }
}
