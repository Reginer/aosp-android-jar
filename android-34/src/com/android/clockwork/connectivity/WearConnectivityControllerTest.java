package com.android.clockwork.connectivity;

import static android.net.ConnectivityManager.NetworkCallback;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm;

import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;

import com.android.clockwork.bluetooth.WearBluetoothMediator;
import com.android.clockwork.cellular.WearCellularMediator;
import com.android.clockwork.common.ActivityModeTracker;
import com.android.clockwork.common.CellOnlyMode;
import com.android.clockwork.common.ProxyConnectivityDebounce.ProxyStatus;
import com.android.clockwork.common.ThermalEmergencyTracker;
import com.android.clockwork.common.WearResourceUtil;
import com.android.clockwork.wifi.WearWifiMediator;
import com.android.wearable.resources.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WearConnectivityControllerTest {
    final ShadowApplication shadowApplication = ShadowApplication.getInstance();

    private ShadowAlarmManager mShadowAlarmManager;

    @Mock WearBluetoothMediator mockBtMediator;
    @Mock WearWifiMediator mockWifiMediator;
    @Mock WearCellularMediator mockCellMediator;
    @Mock WearConnectivityPackageManager mockWearConnectivityPackageManager;

    @Mock WearProxyNetworkAgent mockProxyNetworkAgent;

    @Mock ActivityModeTracker mockActivityModeTracker;
    @Mock CellOnlyMode mockCellOnlyMode;
    @Mock Resources mMockWearResources;
    @Mock ThermalEmergencyTracker mMockThermalEmergencyTracker;

    @Captor
    ArgumentCaptor<WearBluetoothMediator.ProxyStatusListener> mProxyStatusListenerCaptor;
    @Captor
    ArgumentCaptor<AlarmManager.OnAlarmListener> mAlarmListenerCaptor;

    WearConnectivityController mController;

    private static final long TEST_START_TIME = 1000000L;
    private static final int TEST_WIFI_DELAY = 50;
    private static final int TEST_CELL_DELAY = 30000;
    private static final int TEST_EXTEND_CELL = 999000;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        SystemClock.setCurrentTimeMillis(TEST_START_TIME);


        Context context = ApplicationProvider.getApplicationContext();
        shadowOf(context.getPackageManager()).addPackage(WearResourceUtil.WEAR_RESOURCE_PACKAGE);
        ShadowPackageManager.resources.put(WearResourceUtil.WEAR_RESOURCE_PACKAGE,
                mMockWearResources);
        when(mMockWearResources.getInteger(R.integer.proxy_connectivity_delay_wifi)).thenReturn(
                TEST_WIFI_DELAY);
        when(mMockWearResources.getInteger(R.integer.proxy_connectivity_delay_cell)).thenReturn(
                TEST_CELL_DELAY);
        when(mMockWearResources.getInteger(R.integer.wifi_connectivity_extend_cell_delay))
                .thenReturn(TEST_EXTEND_CELL);
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        mShadowAlarmManager = shadowOf(alarmManager);

        mController = new WearConnectivityController(
                RuntimeEnvironment.application,
                alarmManager,
                mockBtMediator,
                mockWifiMediator,
                mockCellMediator,
                mockWearConnectivityPackageManager,
                mockProxyNetworkAgent,
                mockActivityModeTracker,
                mockCellOnlyMode,
                mMockThermalEmergencyTracker);

        verify(mockActivityModeTracker).addListener(mController);
        verify(mockCellOnlyMode).addListener(mController);

        verify(mockBtMediator).setProxyStatusListener(mProxyStatusListenerCaptor.capture());

        // initial controller state
        mController.onBootCompleted();
    }

    @Test
    public void testOnBootCompleted_proxyDisconnected() {
        reset(mockBtMediator, mockWifiMediator, mockCellMediator);
        when(mockBtMediator.isProxyConnected()).thenReturn(false);

        mController.onBootCompleted();

        verify(mockBtMediator).onBootCompleted();
        verify(mockWifiMediator).onBootCompleted(false);
        verify(mockCellMediator).onBootCompleted(false);
    }

    @Test
    public void testOnBootCompleted_proxyConnected() {
        reset(mockBtMediator, mockWifiMediator, mockCellMediator);
        when(mockBtMediator.isProxyConnected()).thenReturn(true);

        mController.onBootCompleted();

        verify(mockBtMediator).onBootCompleted();
        verify(mockWifiMediator).onBootCompleted(true);
        verify(mockCellMediator).onBootCompleted(true);
    }

    @Test
    public void testProxyConnectionStateForwarding_connected() {
        mProxyStatusListenerCaptor.getValue().onProxyStatusChange(
                new ProxyStatus(true, true, true));

        verify(mockWifiMediator).onProxyConnectedChange(true);
        verify(mockCellMediator).onProxyConnectedChange(true);
    }

    @Test
    public void testProxyConnectionStateForwarding_disabled() {
        mProxyStatusListenerCaptor.getValue().onProxyStatusChange(
                new ProxyStatus(false, false, false));

        verify(mockWifiMediator).onProxyConnectedChange(false);
        verify(mockCellMediator).onProxyConnectedChange(false);
    }

    @Test
    public void testProxyConnectionStateForwarding_disconnected_debounced() {
        mProxyStatusListenerCaptor.getValue().onProxyStatusChange(
                new ProxyStatus(false, true, false));
        mProxyStatusListenerCaptor.getValue().onProxyStatusChange(
                new ProxyStatus(true, true, true));

        verify(mockWifiMediator, never()).onProxyConnectedChange(false);
        verify(mockCellMediator, never()).onProxyConnectedChange(false);
    }

    @Test
    public void testProxyConnectionStateForwarding_disconnected() {
        mProxyStatusListenerCaptor.getValue().onProxyStatusChange(
                new ProxyStatus(false, true, false));

        verify(mockWifiMediator, never()).onProxyConnectedChange(anyBoolean());
        verify(mockCellMediator, never()).onProxyConnectedChange(anyBoolean());
        // fire alarms for Wifi delay
        advanceTo(TEST_WIFI_DELAY);
        verify(mockWifiMediator).onProxyConnectedChange(false);

        verify(mockCellMediator, never()).onProxyConnectedChange(false);
        // fire alarms for Cell delay
        advanceTo(TEST_CELL_DELAY);
        verify(mockCellMediator).onProxyConnectedChange(false);
    }

    @Test
    public void testOnProxyStatusChange_disconnected_hasWifi() {
        mProxyStatusListenerCaptor.getValue().onProxyStatusChange(
                new ProxyStatus(false, true, false));

        wifiNetworkConnectedChange(true);

        advanceTo(TEST_CELL_DELAY);
        verify(mockCellMediator, never()).onProxyConnectedChange(false);

        advanceTo(999000L);
        verify(mockCellMediator).onProxyConnectedChange(false);
    }

    @Test
    public void testOnProxyStatusChange_disconnected_lateWifi() {
        mProxyStatusListenerCaptor.getValue().onProxyStatusChange(
                new ProxyStatus(false, true, false));

        advanceTo(TEST_CELL_DELAY);
        reset(mockCellMediator);
        // debounce for cell already triggered update; late Wifi network
        // connect is ignored and should not cause another proxy update.
        wifiNetworkConnectedChange(true);

        advanceTo(999000L);
        verify(mockCellMediator, never()).onProxyConnectedChange(false);
    }

    @Test
    public void testOnWifiConnectedChange() {
        wifiNetworkConnectedChange(true);

        verify(mockCellMediator).onWifiConnectedChange(true);
    }

    @Test
    public void testOnWifiConnectedChange_disconnected() {
        wifiNetworkConnectedChange(true);
        wifiNetworkConnectedChange(false);

        verify(mockCellMediator).onWifiConnectedChange(false);
    }

    @Test
    public void testNetworkRequestForwarding() {
        mController.onWifiRequestsChanged(1);
        mController.onCellularRequestsChanged(2);
        mController.onHighBandwidthRequestsChanged(3);
        mController.onUnmeteredRequestsChanged(4);

        verify(mockWifiMediator).updateNumWifiRequests(1);
        verify(mockWifiMediator).updateNumHighBandwidthRequests(3);
        verify(mockWifiMediator).updateNumUnmeteredRequests(4);

        verify(mockCellMediator).updateNumCellularRequests(2);
        verify(mockCellMediator).updateNumHighBandwidthRequests(3);
    }

    @Test
    public void testActivityModeChanges() {
        reset(mockWifiMediator, mockCellMediator, mockBtMediator);
        when(mockActivityModeTracker.affectsBluetooth()).thenReturn(false);
        when(mockActivityModeTracker.affectsWifi()).thenReturn(false);
        when(mockActivityModeTracker.affectsCellular()).thenReturn(false);

        mController.onActivityModeChanged(true);
        verifyNoMoreInteractions(mockWifiMediator);
        verifyNoMoreInteractions(mockCellMediator);
        verifyNoMoreInteractions(mockBtMediator);

        // TODO set up various radio matrix/configurations and test that they get toggled
    }

    @Test
    public void testCellOnlyModeChanges() {
        reset(mockWifiMediator, mockCellMediator, mockBtMediator);

        mController.onCellOnlyModeChanged(true);
        verify(mockBtMediator).updateCellOnlyMode(true);
        verify(mockWifiMediator).updateCellOnlyMode(true);
        verify(mockCellMediator).updateCellOnlyMode(true);

        mController.onCellOnlyModeChanged(false);
        verify(mockBtMediator).updateCellOnlyMode(false);
        verify(mockWifiMediator).updateCellOnlyMode(false);
        verify(mockCellMediator).updateCellOnlyMode(false);
    }

    private long getCurrentTimeForAlarmType(int type) {
        return (type == AlarmManager.RTC || type == AlarmManager.RTC_WAKEUP)
                ? 0 : SystemClock.elapsedRealtime();
    }

    private void alarmsFired() {
        // copy list since onAlarm callback can cancel and modify list of scheduled alarm.
        List<ScheduledAlarm> scheduledAlarms = new ArrayList<>(
                mShadowAlarmManager.getScheduledAlarms());

        for (ScheduledAlarm alarm : scheduledAlarms) {
            if (alarm.onAlarmListener != null
                    && alarm.triggerAtTime <= getCurrentTimeForAlarmType(alarm.type)) {
                alarm.onAlarmListener.onAlarm();
            }
        }
    }

    private void advanceTo(long timeMillis) {
        SystemClock.setCurrentTimeMillis(TEST_START_TIME + timeMillis);
        alarmsFired();
    }

    private void wifiNetworkConnectedChange(boolean isWifiConnected) {
        Context context = ApplicationProvider.getApplicationContext();
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);

        // ShadowConnectivityManager drops info about requests; here assume that only Wifi network
        // request was registered for callbacks. It doesn't  matter which instance of network is
        // mocking wifi; pretend first instance in the list is the wifi one.
        Network wifiNetwork = cm.getAllNetworks()[0];
        for (NetworkCallback cb : shadowOf(cm).getNetworkCallbacks()) {
            if (isWifiConnected) {
                cb.onAvailable(wifiNetwork);
            } else {
                cb.onLost(wifiNetwork);
            }
        }
    }
}
