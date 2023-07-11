package com.android.clockwork.wifi;

import static com.android.clockwork.wifi.WearWifiMediatorSettings.WIFI_SETTING_OFF;
import static com.android.clockwork.wifi.WearWifiMediatorSettings.WIFI_SETTING_ON;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;

import com.android.clockwork.bluetooth.CompanionTracker;
import com.android.clockwork.common.DeviceEnableSetting;
import com.android.clockwork.common.RadioToggler;
import com.android.clockwork.connectivity.WearConnectivityPackageManager;
import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.power.PowerTracker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
public class WearWifiMediatorTest {
    final ShadowApplication shadowApplication = ShadowApplication.getInstance();

    @Captor
    ArgumentCaptor<AlarmManager.OnAlarmListener> mAlarmListenerCaptor;
    @Mock
    AlarmManager mockAlarmManager;
    @Mock
    WearWifiMediatorSettings mockWifiSettings;
    @Mock
    CompanionTracker mockCompanionTracker;
    @Mock
    PowerTracker mockPowerTracker;
    @Mock
    DeviceEnableSetting mockDeviceEnableSetting;
    @Mock
    WearConnectivityPackageManager mockWearConnectivityPackageManager;
    @Mock
    BooleanFlag mockUserAbsentRadiosOffFlag;
    @Mock
    WifiBackoff mockWifiBackoff;
    @Mock
    WifiLogger mockWifiLogger;

    @Mock
    WifiManager mockWifiMgr;
    @Mock
    RadioToggler mockRadioToggler;
    @Mock
    NetworkInfo mockWifiNetworkInfo;
    @Mock
    WifiConfiguration mockWifiConfiguration;
    WearWifiMediator mWifiMediator;
    @Mock
    private TelephonyManager mMockTelephonyManager;
    private Context mContext;

    private BitSet mBitSet;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        // wifi is initially off for all test cases
        when(mockRadioToggler.getRadioEnabled()).thenReturn(false);

        mContext = RuntimeEnvironment.application;

        // Add Telephony Feature support for detecting emergency calls.
        ShadowPackageManager pm = Shadow.extract(mContext.getPackageManager());
        pm.setSystemFeature(PackageManager.FEATURE_TELEPHONY, true);
        ShadowContextImpl shadowContext = Shadow.extract(((Application) mContext).getBaseContext());
        shadowContext.setSystemService(Context.TELEPHONY_SERVICE, mMockTelephonyManager);
        when(mMockTelephonyManager.isEmergencyNumber("911")).thenReturn(true);

        mWifiMediator = new WearWifiMediator(
                mContext,
                mockAlarmManager,
                mockWifiSettings,
                mockCompanionTracker,
                mockPowerTracker,
                mockDeviceEnableSetting,
                mockWearConnectivityPackageManager,
                mockUserAbsentRadiosOffFlag,
                mockWifiBackoff,
                mockWifiMgr,
                mockWifiLogger);
        // disable wifi lingering to allow easier testing of when mediator should turn wifi off
        mWifiMediator.setWifiLingerDuration(-999);
        mWifiMediator.overrideRadioTogglerForTest(mockRadioToggler);

        // boot into default the setup, off charger, proxy connected
        bootWifiMediator();

        // ensures that all tests start with a clean slate for verification
        // onBootComplete will be tested specifically in its own test cases
        reset(mockRadioToggler);

        mBitSet = new BitSet(PowerTracker.MAX_DOZE_MODE_INDEX);
        when(mockPowerTracker.getDozeModeAllowListedFeatures())
                .thenReturn(mBitSet);
    }

    /**
     * The default setup all tests begin with is the most common starting setup for most devices:
     * -- WiFi Setting is ON (Automatic)
     * -- Enable WiFi while Charging is ON
     * -- WiFi Adapter is DISABLED
     * -- Not in Wifi Backoff
     * -- One saved WiFi network. (this is non-standard but currently simplifies test cases)
     *
     * All test cases in this class assume this initial setup.
     */
    private void bootWifiMediator() {
        // default WiFi Settings values for every test
        when(mockWifiSettings.getIsInAirplaneMode()).thenReturn(false);
        when(mockWifiSettings.getWifiSetting()).thenReturn(WIFI_SETTING_ON);
        when(mockWifiSettings.getEnableWifiWhileCharging()).thenReturn(true);
        when(mockWifiSettings.getDisableWifiMediator()).thenReturn(false);
        when(mockWifiSettings.getWifiOnWhenProxyDisconnected()).thenReturn(true);

        when(mockWifiBackoff.isInBackoff()).thenReturn(false);

        when(mockPowerTracker.isCharging()).thenReturn(false);
        when(mockPowerTracker.isInPowerSave()).thenReturn(false);

        when(mockDeviceEnableSetting.affectsWifi()).thenReturn(true);
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);

        when(mockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(true);

        ArrayList<WifiConfiguration> mockWifiConfigs = new ArrayList<>();
        mockWifiConfigs.add(mockWifiConfiguration);
        when(mockWifiMgr.getConfiguredNetworks()).thenReturn(mockWifiConfigs);

        mWifiMediator.onBootCompleted(true);
    }

    /**
     * Verifies that at least one call to toggle to value enabled is called, but that no call
     * is ever made for !enabled.
     *
     * In addition to verifying, this also updates the mockRadioToggler to reflect the
     * the state that we expect it to be in.
     */
    private void verifyWifiWanted(boolean enable) {
        verify(mockRadioToggler, atLeastOnce()).toggleRadio(enable);
        verify(mockRadioToggler, never()).toggleRadio(!enable);
        reset(mockRadioToggler);
        when(mockRadioToggler.getRadioEnabled()).thenReturn(enable);
    }

    @Test
    public void testConstructorRegistersAppropriateReceiversAndListeners() {
        verify(mockWifiSettings).addListener(mWifiMediator);
        verify(mockPowerTracker).addListener(mWifiMediator);
        verify(mockUserAbsentRadiosOffFlag).addListener(any());

        IntentFilter intentFilter = mWifiMediator.getBroadcastReceiverIntentFilter();
        for (Iterator<String> it = intentFilter.actionsIterator(); it.hasNext(); ) {
            String action = it.next();
            Assert.assertTrue("BroadcastReceiver not registered for action: " + action,
                    shadowApplication.hasReceiverForIntent(new Intent(action)));
        }
    }

    @Test
    public void testNoWifiUpdatesBeforeOnBootCompleted() {
        // the default setUp for tests calls onBootCompleted, so we cannot use
        // mWifiMediator and mRadioToggler for this test
        RadioToggler radioToggler = Mockito.mock(RadioToggler.class);
        WearWifiMediator wifiMediator = new WearWifiMediator(
                mContext,
                mockAlarmManager,
                mockWifiSettings,
                mockCompanionTracker,
                mockPowerTracker,
                mockDeviceEnableSetting,
                mockWearConnectivityPackageManager,
                mockUserAbsentRadiosOffFlag,
                mockWifiBackoff,
                mockWifiMgr,
                mockWifiLogger);
        // disable wifi lingering to allow easier testing of when mediator should turn wifi off
        wifiMediator.setWifiLingerDuration(-999);
        wifiMediator.overrideRadioTogglerForTest(mockRadioToggler);

        // now trigger some broadcasts, listeners, etc.
        when(mockPowerTracker.isCharging()).thenReturn(true);
        wifiMediator.onChargingStateChanged();
        wifiMediator.updateNumWifiRequests(1);
        wifiMediator.updateNumWifiRequests(0);
        when(mockPowerTracker.isCharging()).thenReturn(true);
        wifiMediator.onChargingStateChanged();
        final Intent wifiOnIntent = new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiOnIntent.putExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_ENABLED);
        mContext.sendBroadcast(wifiOnIntent);

        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);
        wifiMediator.onDeviceEnableChanged();
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);
        wifiMediator.onDeviceEnableChanged();

        verify(radioToggler, never()).toggleRadio(anyBoolean());
    }

    @Test
    public void testOnBootCompletedOffCharger() {
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(WearWifiMediator.ACTION_EXIT_WIFI_LINGER)));

        mWifiMediator.onBootCompleted(true);
        verifyWifiWanted(false);
    }

    @Test
    public void testOnBootCompletedOnCharger() {
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(WearWifiMediator.ACTION_EXIT_WIFI_LINGER)));

        when(mockPowerTracker.isCharging()).thenReturn(true);

        mWifiMediator.onBootCompleted(true);
        verifyWifiWanted(true);
    }

    @Test
    public void testOnBootCompletedProxyDisconnectedEnablesWifi() {
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(WearWifiMediator.ACTION_EXIT_WIFI_LINGER)));

        mWifiMediator.onBootCompleted(false);
        verifyWifiWanted(true);
    }

    @Test
    public void testOnBootCompletedProxyDisonnectedNotEnableWifi() {
        // when WIFI_ON_WHEN_PROXY_DISCONNECTED option is disabled
        // we expect not enabling wifi when proxy is disconnected
        when(mockWifiSettings.getWifiOnWhenProxyDisconnected()).thenReturn(false);

        mWifiMediator.onBootCompleted(false);
        verifyWifiWanted(false);
    }

    @Test
    public void testOnBootCompletedWifiOnBootDelay() {
        long wifiOnBootDelayMs = 1L;
        when(mockWifiSettings.getWifiOnBootDelayMs()).thenReturn(wifiOnBootDelayMs);

        mWifiMediator.onBootCompleted(false);

        verifyWifiWanted(false);
        verify(mockAlarmManager).set(eq(AlarmManager.ELAPSED_REALTIME_WAKEUP),
                anyLong(), anyString(), mAlarmListenerCaptor.capture(), eq(null));
        SystemClock.sleep(wifiOnBootDelayMs);
        mAlarmListenerCaptor.getValue().onAlarm();
        verifyWifiWanted(true);
    }

    @Test
    public void testUpdateProxyConnected() {
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);

        mWifiMediator.updateProxyConnected(true);
        verifyWifiWanted(false);

        // when WIFI_ON_WHEN_PROXY_DISCONNECTED option is disabled
        // we expect not enabling wifi when proxy is disconnected
        mWifiMediator.onWifiOnWhenProxyDisconnectedChanged(false);

        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(false);

        mWifiMediator.updateProxyConnected(true);
        verifyWifiWanted(false);
    }

    @Test
    public void testUpdateActivityMode() {
        // disconnect proxy first to enable wifi
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);

        // now enter activity mode
        mWifiMediator.updateActivityMode(true);
        verifyWifiWanted(false);

        // exiting activity mode should cause wifi to re-enable (b/c proxy is disconnected)
        mWifiMediator.updateActivityMode(false);
        verifyWifiWanted(true);
    }

    @Test
    public void testUpdateCellOnlyMode() {
        // disconnect proxy first to enable wifi
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);

        mWifiMediator.updateCellOnlyMode(true);
        verifyWifiWanted(false);

        // exiting cell only mode should cause wifi to re-enable (b/c proxy is disconnected)
        mWifiMediator.updateCellOnlyMode(false);
        verifyWifiWanted(true);
    }

    @Test
    public void testDeviceIdleUserAbsent() {
        // disconnect proxy first to enable wifi
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mWifiMediator.onDeviceIdleModeChanged();
        verifyWifiWanted(false);

        when(mockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(false);
        mWifiMediator.onUserAbsentRadiosOffChanged(false);
        verifyWifiWanted(true);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        mWifiMediator.onDeviceIdleModeChanged();
        verifyWifiWanted(true);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mWifiMediator.onDeviceIdleModeChanged();
        verifyWifiWanted(true);

        when(mockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(true);
        mWifiMediator.onUserAbsentRadiosOffChanged(true);
        verifyWifiWanted(false);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        mWifiMediator.onDeviceIdleModeChanged();
        verifyWifiWanted(true);
    }

    @Test
    public void testDeviceIdleAllowListedFeature() {
        // disconnect proxy first to enable wifi
        mWifiMediator.updateProxyConnected(false);
        mBitSet.set(PowerTracker.DOZE_MODE_WIFI_INDEX);
        verifyWifiWanted(true);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mWifiMediator.onDeviceIdleModeChanged();
        verify(mockRadioToggler, never()).toggleRadio(anyBoolean());
    }

    @Test
    public void testDeviceIdleBlockListedFeature() {
        // disconnect proxy first to enable wifi
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mWifiMediator.onDeviceIdleModeChanged();
        verifyWifiWanted(false);
    }

    @Test
    public void testUpdateNetworkRequests() {
        mWifiMediator.updateNumWifiRequests(1);
        verifyWifiWanted(true);

        mWifiMediator.updateNumWifiRequests(0);
        verifyWifiWanted(false);

        mWifiMediator.updateNumHighBandwidthRequests(1);
        verifyWifiWanted(true);

        mWifiMediator.updateNumHighBandwidthRequests(0);
        verifyWifiWanted(false);

        // onUnmeteredRequest behavior depends on whether we're in BLE mode or not
        when(mockCompanionTracker.isCompanionBle()).thenReturn(false);
        mWifiMediator.updateNumUnmeteredRequests(1);
        verifyWifiWanted(false);

        mWifiMediator.updateNumUnmeteredRequests(0);
        verifyWifiWanted(false);

        when(mockCompanionTracker.isCompanionBle()).thenReturn(true);
        mWifiMediator.updateNumUnmeteredRequests(1);
        verifyWifiWanted(true);

        mWifiMediator.updateNumUnmeteredRequests(0);
        verifyWifiWanted(false);
    }

    @Test
    public void testUpdateNetworkRequestsProxyDisonnectedNotEnableWifi() {
        // when WIFI_ON_WHEN_PROXY_DISCONNECTED option is disabled
        // we expect not enabling wifi when proxy is disconnected
        mWifiMediator.onWifiOnWhenProxyDisconnectedChanged(false);

        // When proxy disconnected, wifi should remain off
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(false);

        // When network requests present, turn wifi on
        mWifiMediator.updateNumWifiRequests(1);
        verifyWifiWanted(true);

        mWifiMediator.updateNumWifiRequests(0);
        verifyWifiWanted(false);

        // onUnmeteredRequest behavior depends on whether we're in BLE mode or not
        when(mockCompanionTracker.isCompanionBle()).thenReturn(false);
        mWifiMediator.updateNumUnmeteredRequests(1);
        verifyWifiWanted(false);

        mWifiMediator.updateNumUnmeteredRequests(0);
        verifyWifiWanted(false);

        when(mockCompanionTracker.isCompanionBle()).thenReturn(true);
        mWifiMediator.updateNumUnmeteredRequests(1);
        verifyWifiWanted(true);

        mWifiMediator.updateNumUnmeteredRequests(0);
        verifyWifiWanted(false);
    }

    @Test
    public void testWifiLinger() {
        mWifiMediator.setWifiLingerDuration(5000L);

        mWifiMediator.updateNumWifiRequests(1);
        verifyWifiWanted(true);

        mWifiMediator.updateNumWifiRequests(0);
        verify(mockAlarmManager).setWindow(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyLong(), eq(mWifiMediator.exitWifiLingerIntent));
        verify(mockRadioToggler, never()).toggleRadio(anyBoolean());

        reset(mockAlarmManager, mockWifiMgr);
        mWifiMediator.updateNumWifiRequests(1);
        verify(mockAlarmManager).cancel(mWifiMediator.exitWifiLingerIntent);
        verifyWifiWanted(true);

        reset(mockAlarmManager, mockWifiMgr);
    }

    @Test
    public void testWifiLingerIntentHandling() {
        mWifiMediator.setWifiLingerDuration(5000L);

        // when wifi is off (and not lingering), if the alarm goes off, nothing should happen
        mContext.sendBroadcast(
                new Intent(WearWifiMediator.ACTION_EXIT_WIFI_LINGER));
        verify(mockRadioToggler, never()).toggleRadio(anyBoolean());

        // when wifi is on (and not lingering), if the alarm goes off, nothing should happen
        reset(mockWifiMgr);
        mWifiMediator.updateNumWifiRequests(1);
        verifyWifiWanted(true);
        mContext.sendBroadcast(
                new Intent(WearWifiMediator.ACTION_EXIT_WIFI_LINGER));
        verify(mockRadioToggler, never()).toggleRadio(anyBoolean());

        // when wifi is on and lingering, if the alarm goes off, wifi should get disabled
        reset(mockWifiMgr);
        mWifiMediator.updateNumWifiRequests(0);
        mContext.sendBroadcast(
                new Intent(WearWifiMediator.ACTION_EXIT_WIFI_LINGER));
        verifyWifiWanted(false);
    }

    @Test
    public void testWifiSettingsChanges() {
        // enable WiFi lingering for this test, to ensure that setting WiFi to OFF will bypass it
        mWifiMediator.setWifiLingerDuration(5000L);
        // disconnect proxy to enable WiFi
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);

        // turning off WiFi should disable the adapter immediately
        mWifiMediator.onWifiSettingChanged(WIFI_SETTING_OFF);
        verifyWifiWanted(false);

        // plug in the power, get some network requests going, disconnect proxy, and
        // enter wifi settings -- all of these should result in no change to WiFi state
        mContext.sendBroadcast(new Intent(Intent.ACTION_POWER_CONNECTED));
        mWifiMediator.updateProxyConnected(false);
        mWifiMediator.updateNumHighBandwidthRequests(3);
        mWifiMediator.updateNumUnmeteredRequests(3);
        mWifiMediator.updateNumWifiRequests(3);
        mWifiMediator.onInWifiSettingsMenuChanged(true);

        verifyWifiWanted(false);

        // now turn WiFi back ON, and the adapter should get enabled
        mWifiMediator.onWifiSettingChanged(WIFI_SETTING_ON);
        verifyWifiWanted(true);
    }

    @Test
    public void testInWifiSettingsBehavior() {
        mWifiMediator.onInWifiSettingsMenuChanged(true);
        verifyWifiWanted(true);

        mWifiMediator.onInWifiSettingsMenuChanged(false);
        verifyWifiWanted(false);

        // if WiFi Setting is set to Off, then we don't turn WiFi on when in the menu
        mWifiMediator.onWifiSettingChanged(WIFI_SETTING_OFF);
        mWifiMediator.onInWifiSettingsMenuChanged(true);
        verifyWifiWanted(false);
    }

    @Test
    public void testEnableWifiWhileCharging() {
        when(mockPowerTracker.isCharging()).thenReturn(true);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(true);

        when(mockPowerTracker.isCharging()).thenReturn(false);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(false);

        mWifiMediator.onEnableWifiWhileChargingChanged(false);

        when(mockPowerTracker.isCharging()).thenReturn(true);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(false);

        when(mockPowerTracker.isCharging()).thenReturn(false);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(false);
    }

    @Test
    public void testEnableWifiWhileChargingProxyDisonnectedNotEnableWifi() {
        // when WIFI_ON_WHEN_PROXY_DISCONNECTED option is disabled
        // we expect not enabling wifi when proxy is disconnected
        mWifiMediator.onWifiOnWhenProxyDisconnectedChanged(false);

        // When proxy disconnected, wifi should remain off
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(false);

        when(mockPowerTracker.isCharging()).thenReturn(true);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(true);

        when(mockPowerTracker.isCharging()).thenReturn(false);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(false);

        mWifiMediator.onEnableWifiWhileChargingChanged(false);

        when(mockPowerTracker.isCharging()).thenReturn(true);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(false);

        when(mockPowerTracker.isCharging()).thenReturn(false);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(false);
    }

    @Test
    public void testPowerSaveMode() {
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);

        when(mockPowerTracker.isInPowerSave()).thenReturn(true);
        mWifiMediator.onPowerSaveModeChanged();
        verifyWifiWanted(false);

        when(mockPowerTracker.isInPowerSave()).thenReturn(false);
        mWifiMediator.onPowerSaveModeChanged();
        verifyWifiWanted(true);
    }

    @Test
    public void testHardwareLowPowerMode() {
        // enable WiFi lingering for this test, to ensure that enabling HLPM bypasses it
        mWifiMediator.setWifiLingerDuration(5000L);
        // disconnect proxy to enable WiFi
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);

        // turning on HLPM should cause WiFi to go down immediately
        mWifiMediator.onHardwareLowPowerModeChanged(true);
        verifyWifiWanted(false);

        // plug in the power, get some network requests going, disconnect proxy, and
        // enter wifi settings -- all of these should result in no change to WiFi state
        mContext.sendBroadcast(new Intent(Intent.ACTION_POWER_CONNECTED));
        mWifiMediator.updateProxyConnected(false);
        mWifiMediator.updateNumHighBandwidthRequests(3);
        mWifiMediator.updateNumUnmeteredRequests(3);
        mWifiMediator.updateNumWifiRequests(3);
        mWifiMediator.onInWifiSettingsMenuChanged(true);

        verifyWifiWanted(false);

        // now disable HLPM; WiFi should come on immediately
        mWifiMediator.onHardwareLowPowerModeChanged(false);
        verifyWifiWanted(true);
    }

    /**
     * Ensures that WifiMediator correctly monitors any changes to the WiFi Adapter state
     * and forces the adapter back to the correct state if a state change which is inconsistent
     * with the current WifiMediator decision is detected.
     */
    @Test
    public void testWifiMediatorTracksAdapterStateChanges() {
        final Intent wifiOnIntent = new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiOnIntent.putExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_ENABLED);
        final Intent wifiOffIntent = new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiOffIntent.putExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);

        // WiFi Setting is ON; adapter should be OFF;  if we hear that WiFi got turned on,
        // WifiMediator should turn it back off
        mContext.sendBroadcast(wifiOnIntent);

        // Make sure the radioToggler knows about the change
        verifyWifiWanted(false);

        // WiFi Setting is ON; adapter is ON; if we hear that WiFi got turned on,
        // WifiMediator should flip WiFi back on
        mWifiMediator.updateNumWifiRequests(5);
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);
        mContext.sendBroadcast(wifiOffIntent);
        verifyWifiWanted(true);

        // WiFi Setting is OFF; adapter is OFF; if we hear that WiFi got turned on,
        // WifiMediator will now allow the adapter to stay on, but ensure that the WiFi Setting
        // is correctly toggled back to ON/AUTO
        mWifiMediator.onWifiSettingChanged(WIFI_SETTING_OFF);
        verifyWifiWanted(false);
        mContext.sendBroadcast(wifiOnIntent);
        verify(mockWifiSettings).putWifiSetting(WIFI_SETTING_ON);
    }

    @Test
    public void testDisableWifiMediator() {
        mWifiMediator.onDisableWifiMediatorChanged(true);

        // plug in the power, get some network requests going, disconnect proxy, and
        // enter wifi settings -- all of these should result in no change to WiFi state
        mContext.sendBroadcast(new Intent(Intent.ACTION_POWER_CONNECTED));
        mWifiMediator.updateProxyConnected(false);
        mWifiMediator.updateNumHighBandwidthRequests(3);
        mWifiMediator.updateNumUnmeteredRequests(3);
        mWifiMediator.updateNumWifiRequests(3);
        mWifiMediator.onInWifiSettingsMenuChanged(true);

        verify(mockRadioToggler, never()).toggleRadio(anyBoolean());
    }

    @Test
    public void testWifiBackoff() {
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);
        verify(mockWifiBackoff).scheduleBackoff();

        when(mockWifiBackoff.isInBackoff()).thenReturn(true);
        mWifiMediator.onWifiBackoffChanged();
        verifyWifiWanted(false);

        reset(mockWifiBackoff);
        when(mockWifiBackoff.isInBackoff()).thenReturn(false);
        mWifiMediator.onWifiBackoffChanged();
        verifyWifiWanted(true);
        verify(mockWifiBackoff).scheduleBackoff();

        reset(mockWifiBackoff);
        Intent i = new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        i.putExtra(WifiManager.EXTRA_NETWORK_INFO, mockWifiNetworkInfo);
        when(mockWifiNetworkInfo.isConnected()).thenReturn(true);
        mContext.sendBroadcast(i);
        verify(mockWifiBackoff).cancelBackoff();

        reset(mockWifiBackoff);
        when(mockWifiNetworkInfo.isConnected()).thenReturn(false);
        mContext.sendBroadcast(i);
        verify(mockWifiBackoff).scheduleBackoff();
    }

    @Test
    public void testNoWifiNetworksConfigured() {
        mWifiMediator.setNumConfiguredNetworks(0);

        // Any changes to proxy connectivity should not cause WiFi to be enabled.
        mWifiMediator.updateProxyConnected(false);
        mWifiMediator.updateProxyConnected(true);
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(false);

        // But being in WiFi Settings, being on charger, or NetworkRequests
        // may allow WiFi to be brought up.
        mWifiMediator.onInWifiSettingsMenuChanged(true);
        verifyWifiWanted(true);

        mWifiMediator.onInWifiSettingsMenuChanged(false);
        verifyWifiWanted(false);

        when(mockPowerTracker.isCharging()).thenReturn(true);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(true);

        when(mockPowerTracker.isCharging()).thenReturn(false);
        mWifiMediator.onChargingStateChanged();
        verifyWifiWanted(false);

        mWifiMediator.updateNumWifiRequests(1);
        verifyWifiWanted(true);

        mWifiMediator.updateNumWifiRequests(0);
        verifyWifiWanted(false);
    }

    @Test
    public void testDeviceEnableSettings() {
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);
        mWifiMediator.onDeviceEnableChanged();
        verifyWifiWanted(false);

        // NetworkRequests should be ignored.
        mWifiMediator.updateNumWifiRequests(1);
        verifyWifiWanted(false);

        mWifiMediator.updateNumWifiRequests(0);
        verifyWifiWanted(false);

        // Any changes to proxy connectivity should not cause WiFi to be enabled.
        mWifiMediator.updateProxyConnected(false);
        mWifiMediator.updateProxyConnected(true);
        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(false);

        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);
        mWifiMediator.onDeviceEnableChanged();
        verifyWifiWanted(true);
    }

    @Test
    public void testDeviceEnableSettings_wifiNotAffected() {
        when(mockDeviceEnableSetting.affectsWifi()).thenReturn(false);
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);
        mWifiMediator.onDeviceEnableChanged();

        mWifiMediator.updateNumWifiRequests(1);
        verifyWifiWanted(true);

        mWifiMediator.updateNumWifiRequests(0);
        verifyWifiWanted(false);

        mWifiMediator.updateProxyConnected(false);
        verifyWifiWanted(true);
    }

    @Test
    public void testWifiEmergencyCall_wifiTurnsOnForEmergencyCall() {
        Intent intent = new Intent(Intent.ACTION_NEW_OUTGOING_CALL);
        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, "911");

        mContext.sendBroadcast(intent);
        verifyWifiWanted(true);

        mWifiMediator.mPhoneStateListener.onCallStateChanged(
                TelephonyManager.CALL_STATE_IDLE, /* phoneNumber= */ null);
        verifyWifiWanted(false);
    }

    @Test
    public void testWifiEmergencyCall_wifiDoesNotTurnOnForNonEmergencyCall() {
        Intent intent = new Intent(Intent.ACTION_NEW_OUTGOING_CALL);
        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, "1234567890");

        mContext.sendBroadcast(intent);

        verify(mockRadioToggler, never()).toggleRadio(anyBoolean());
    }
}
