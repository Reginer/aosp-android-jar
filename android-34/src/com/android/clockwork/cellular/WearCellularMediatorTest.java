package com.android.clockwork.cellular;

import static com.android.clockwork.cellular.WearCellularMediator.ACTION_ESIM_TEST_MODE;
import static com.android.clockwork.cellular.WearCellularMediator.CELL_AUTO_OFF;
import static com.android.clockwork.cellular.WearCellularMediator.CELL_AUTO_ON;
import static com.android.clockwork.cellular.WearCellularMediator.ENABLE_CELLULAR_ON_BOOT_URI;
import static com.android.clockwork.cellular.WearCellularMediator.EXTRA_IN_ESIM_TEST_MODE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.util.Log;

import com.android.clockwork.common.DeviceEnableSetting;
import com.android.clockwork.common.ThermalEmergencyTracker;
import com.android.clockwork.common.ThermalEmergencyTracker.ThermalEmergencyMode;
import com.android.clockwork.connectivity.WearConnectivityPackageManager;
import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.power.PowerTracker;
import com.android.ims.ImsManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.google.android.clockwork.signaldetector.SignalStateDetector;
import com.google.android.clockwork.signaldetector.SignalStateModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.BitSet;

/** Test for {@link WearCellularMediator} */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class WearCellularMediatorTest {
    private static final String CELL_AUTO_SETTING_KEY = "clockwork_cell_auto_setting";
    private static final Uri CELL_AUTO_SETTING_URI =
            Settings.System.getUriFor(CELL_AUTO_SETTING_KEY);
    private static final Uri CELL_ON_URI = Settings.Global.getUriFor(Settings.Global.CELL_ON);
    final ShadowApplication shadowApplication = ShadowApplication.getInstance();
    private final ArrayList<SubscriptionInfo> activeSubInfoList = new ArrayList<>();
    @Captor
    ArgumentCaptor<Message> msgCaptor;
    @Captor
    ArgumentCaptor<AlarmManager.OnAlarmListener> mAlarmListenerCaptor;
    @Mock
    Handler mockHandler;
    @Mock
    PowerTracker mockPowerTracker;
    @Mock
    DeviceEnableSetting mockDeviceEnableSetting;
    @Mock
    WearConnectivityPackageManager mockWearConnectivityPackageManager;
    @Mock
    BooleanFlag mockUserAbsentRadiosOffFlag;
    @Mock
    SignalStateDetector mMockSignalStateDetector;
    @Mock
    AlarmManager mockAlarmManager;
    @Mock
    TelephonyManager mockTelephonyManager;
    @Mock
    EuiccManager mockEuiccManager;
    @Mock
    SubscriptionManager mockSubscriptionManager;
    @Mock
    WearCellularMediatorSettings mockSettings;
    @Mock
    SubscriptionInfo mockEsimSubInfo;
    @Mock
    SubscriptionInfo mockPsimSubInfo;
    @Mock
    TwinnedNumberBlocker mTwinNumberBlocker;
    @Mock ImsManager mImsManager;
    private ContentResolver mContentResolver;
    private Context mContext;
    private WearCellularMediator mMediator;

    private BitSet mBitSet;

    @Before
    public void setUp() {
        ShadowLog.setLoggable("WearCellularMediator", Log.VERBOSE);
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();

        initMockSubscriptions();
        when(mockSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(activeSubInfoList);
        when(mockSettings.isVoiceTwinningEnabled()).thenReturn(true);
        when(mockSettings.isWearEsimDevice()).thenReturn(false);
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_ON);
        when(mockSettings.getCellState()).thenReturn(PhoneConstants.CELL_ON_FLAG);
        when(mockSettings.getRadioOnState()).thenReturn(
                WearCellularMediator.RADIO_ON_STATE_UNKNOWN);
        when(mockSettings.shouldTurnCellularOffDuringPowerSave()).thenReturn(true);
        when(mockSettings.shouldTurnCellularOffWhenWifiConnected()).thenReturn(true);
        when(mockSettings.getMobileSignalDetectorAllowed()).thenReturn(true);
        when(mockPowerTracker.isInPowerSave()).thenReturn(false);

        when(mockDeviceEnableSetting.affectsCellular()).thenReturn(true);
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);

        when(mockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(true);
        when(mImsManager.isWfcEnabledByPlatform()).thenReturn(false);
        when(mImsManager.isWfcEnabledByUser()).thenReturn(false);
        when(mImsManager.isWfcProvisionedOnDevice()).thenReturn(false);

        mMediator = new WearCellularMediator(
                mContext,
                mContentResolver,
                mockAlarmManager,
                mockTelephonyManager,
                mockEuiccManager,
                mockSubscriptionManager,
                mockSettings,
                mockPowerTracker,
                mockDeviceEnableSetting,
                mockWearConnectivityPackageManager,
                mockUserAbsentRadiosOffFlag,
                mMockSignalStateDetector,
                mTwinNumberBlocker);
        mMediator.mHandler = mockHandler;
        mMediator.mImsManagerForTesting = mImsManager;

        // disable cell lingering to allow easier testing for most test cases
        mMediator.setCellLingerDuration(-999);
        mMediator.onBootCompleted(true);
        mMediator.onUserUnlocked();
        when(mMockSignalStateDetector.isStarted()).thenReturn(true);

        mBitSet = new BitSet(PowerTracker.MAX_DOZE_MODE_INDEX);
        when(mockPowerTracker.getDozeModeAllowListedFeatures())
                .thenReturn(mBitSet);
    }

    private void initMockSubscriptions() {
        when(mockEsimSubInfo.isEmbedded()).thenReturn(true);
        when(mockPsimSubInfo.isEmbedded()).thenReturn(false);
    }

    @Test
    public void testOnBootComplete() {
        assertEquals(0, Settings.Global.getInt(
                mContentResolver, Settings.Global.ENABLE_CELLULAR_ON_BOOT, 1));

        verify(mockUserAbsentRadiosOffFlag).addListener(any());

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_PROXY_CONNECTED);
    }

    @Test
    public void testInitWithContext() {
        verify(mockPowerTracker).addListener(mMediator);

        assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(WearCellularMediator.ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED)));
        assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(WearCellularMediator.ACTION_EXIT_CELL_LINGER)));
        assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(ACTION_ESIM_TEST_MODE)));
        assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)));
    }

    @Test
    public void testEnforceCellularOnBoot() {
        Settings.Global.putInt(mContentResolver, Settings.Global.ENABLE_CELLULAR_ON_BOOT, 999);
        mContentResolver.notifyChange(ENABLE_CELLULAR_ON_BOOT_URI, null);
        assertEquals(0, Settings.Global.getInt(
                mContentResolver, Settings.Global.ENABLE_CELLULAR_ON_BOOT, 1));
    }

    @Test
    public void testTurnCellAutoOff() {
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_OFF);
        mContentResolver.notifyChange(CELL_AUTO_SETTING_URI, null);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NO_CELL_AUTO);
    }

    @Test
    public void testHighBandwidthRequest() {
        mMediator.updateNumHighBandwidthRequests(1);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);
    }

    @Test
    public void testCellularTransportRequest() {
        mMediator.updateNumCellularRequests(1);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        // Going into power save mode should turn cell off.
        when(mockPowerTracker.isInPowerSave()).thenReturn(true);
        mMediator.onPowerSaveModeChanged();

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_POWER_SAVE);
    }

    @Test
    public void testActivityMode() {
        mMediator.updateNumCellularRequests(1);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        mMediator.updateActivityMode(true);
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_ACTIVITY_MODE);

        mMediator.updateActivityMode(false);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);
    }

    @Test
    public void testActivityMode_cellOnNoAuto() {
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_OFF);
        mMediator.onBootCompleted(true);
        reset(mockHandler);

        mMediator.updateActivityMode(true);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NO_CELL_AUTO);
    }

    @Test
    public void testThermalEmergencyDisablesCell() {
        //set cell to known state
        mMediator.updateNumCellularRequests(1);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        mMediator.updateThermalEmergencyMode(
                new ThermalEmergencyMode(ThermalEmergencyTracker.THERMAL_EMERGENCY_LEVEL_CELL));
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_THERMAL_EMERGENCY);

        mMediator.updateThermalEmergencyMode(new ThermalEmergencyMode(0));
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);
    }

    @Test
    public void testDeviceIdleUserAbsent() {
        mMediator.updateNumCellularRequests(1);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_USER_ABSENT);

        when(mockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(false);
        mMediator.onUserAbsentRadiosOffChanged(false);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        when(mockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(true);
        mMediator.onUserAbsentRadiosOffChanged(true);
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_USER_ABSENT);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);
    }

    @Test
    public void testReturnFromDeviceIdle() {
        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        mMediator.onProxyConnectedChange(false);
        reset(mockHandler); // reset tracking radio changes

        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        mMediator.onDeviceIdleModeChanged();

        // delay cell radio
        verify(mockHandler, never()).sendMessage(any()); // no radio changes
        verify(mockAlarmManager).set(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyString(), mAlarmListenerCaptor.capture(), eq(null));

        mAlarmListenerCaptor.getValue().onAlarm();
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);

    }

    @Test
    public void testReturnFromDeviceIdleCancelDelay() {
        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        mMediator.onProxyConnectedChange(false);
        reset(mockHandler); // reset tracking radio changes

        when(mockPowerTracker.isDeviceIdle()).thenReturn(false);
        mMediator.onDeviceIdleModeChanged();

        // delay entered
        verify(mockAlarmManager).set(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyString(), mAlarmListenerCaptor.capture(), eq(null));

        mMediator.onProxyConnectedChange(true); // Proxy was connected before idle
        verify(mockAlarmManager).cancel(eq(mAlarmListenerCaptor.getValue()));
    }

    @Test
    public void testBootCompletedAfterSimReady_ProxyDisconnected() {
        reset(mockAlarmManager);
        mMediator = new WearCellularMediator(
                mContext,
                mContentResolver,
                mockAlarmManager,
                mockTelephonyManager,
                mockEuiccManager,
                mockSubscriptionManager,
                mockSettings,
                mockPowerTracker,
                mockDeviceEnableSetting,
                mockWearConnectivityPackageManager,
                mockUserAbsentRadiosOffFlag,
                mMockSignalStateDetector,
                mTwinNumberBlocker);
        mMediator.mHandler = mockHandler;
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);
        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_LOADED);
        mContext.sendBroadcast(simIntent);

        reset(mockHandler); // reset tracking radio changes

        // assert cell radio delayed
        doAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            assertTrue(message.what != WearCellularMediator.MSG_ENABLE_CELL);
            return null;
        }).when(mockHandler).sendMessage(any());

        mMediator.onBootCompleted(false);
        mMediator.onUserUnlocked();
        verify(mockAlarmManager).set(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyString(), mAlarmListenerCaptor.capture(), eq(null));

        reset(mockHandler);

        mAlarmListenerCaptor.getValue().onAlarm();
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testBootCompleted_cellOn() {
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_OFF);
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        reset(mockHandler);

        mMediator.onBootCompleted(false);

        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);
        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_LOADED);
        mContext.sendBroadcast(simIntent);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NO_CELL_AUTO);
    }

    @Test
    public void testDeviceIdleAllowListedFeature() {
        mMediator.updateNumCellularRequests(1);
        mBitSet.set(PowerTracker.DOZE_MODE_CELLULAR_INDEX);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);
        verify(mockHandler, atLeastOnce()).sendMessage(msgCaptor.capture());

        reset(mockHandler);
        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());
    }

    @Test
    public void testDeviceIdleBlockListedFeature() {
        mMediator.updateNumCellularRequests(1);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        when(mockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_USER_ABSENT);
    }

    @Test
    public void testInPhoneCall() {
        Intent intent = new Intent(WearCellularMediator.ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED);
        intent.putExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.EXTRA_STATE_OFFHOOK);
        mContext.sendBroadcast(intent);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PHONE_CALL);
    }

    @Test
    public void testEsimTestMode() {
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        Intent intent = new Intent(ACTION_ESIM_TEST_MODE);
        intent.putExtra(EXTRA_IN_ESIM_TEST_MODE, true);
        mContext.sendBroadcast(intent);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_ESIM_TEST_MODE);
    }

    @Test
    public void testProxyDisconnect() {
        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_Present() {
        activeSubInfoList.add(mockEsimSubInfo);
        mMediator.updateActiveEsimSubscriptionStatus();
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_LOADED);
        mContext.sendBroadcast(simIntent);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.ESIM_PROFILE_ACTIVATION_SETTING_URI, null);
        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_PresentButDeactivated() {
        activeSubInfoList.add(mockEsimSubInfo);
        mMediator.updateActiveEsimSubscriptionStatus();
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(true);

        mContentResolver.notifyChange(
                WearCellularMediatorSettings.ESIM_PROFILE_ACTIVATION_SETTING_URI, null);
        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_ESIM_DEACTIVATED);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_NoEsimOrPsimInstalled() {
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_ABSENT);
        mContext.sendBroadcast(simIntent);

        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_SIM_ABSENT);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_NoEsimOrPsimInstalled_cellOn() {
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_OFF);
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        mMediator.onBootCompleted(false);
        reset(mockHandler);

        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);
        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_ABSENT);
        mContext.sendBroadcast(simIntent);

        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NO_CELL_AUTO);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_NoEsimAndPsimNotReady() {
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_NOT_READY);
        mContext.sendBroadcast(simIntent);

        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_SIM_ABSENT);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_NoEsimAndPsimUnknown() {
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_UNKNOWN);
        mContext.sendBroadcast(simIntent);

        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_SIM_ABSENT);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_PsimInstalledButNoEsim() {
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_READY);
        mContext.sendBroadcast(simIntent);

        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_EsimInstalledButNoPsim() {
        activeSubInfoList.add(mockPsimSubInfo);
        activeSubInfoList.add(mockEsimSubInfo);
        mMediator.updateActiveEsimSubscriptionStatus();
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_ABSENT);
        mContext.sendBroadcast(simIntent);

        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_OnlyDeletedSubscriptionsPresent() {
        activeSubInfoList.add(mockPsimSubInfo);
        mMediator.updateActiveEsimSubscriptionStatus();
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_ABSENT);
        mContext.sendBroadcast(simIntent);
        mMediator.onProxyConnectedChange(false);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_SIM_ABSENT);
    }

    /**
     * As of b/243985741, mediator behavior no longer changes when voice twinning changes.
     */
    @Test
    public void testToggleVoiceTwinning_DoesNothing() {
        when(mockSettings.isVoiceTwinningEnabled()).thenReturn(false);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);

        verify(mockSettings, never()).setCellAutoSetting(CELL_AUTO_OFF);
        verify(mockSettings, never()).setCellAutoSetting(CELL_AUTO_ON);

        reset(mockSettings);
        when(mockSettings.isVoiceTwinningEnabled()).thenReturn(true);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);

        verify(mockSettings, never()).setCellAutoSetting(CELL_AUTO_OFF);
        verify(mockSettings, never()).setCellAutoSetting(CELL_AUTO_ON);
    }

    @Test
    public void testTurnCellStateOff() {
        // Turn radio on first.
        mMediator.onProxyConnectedChange(false);

        reset(mockTelephonyManager);
        when(mockSettings.getCellState()).thenReturn(PhoneConstants.CELL_OFF_FLAG);
        mContentResolver.notifyChange(WearCellularMediator.CELL_ON_URI, null);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_CELL_SETTING);

        // Cell radio stays off even if proxy disconnect.
        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_CELL_SETTING);

        // Cell radio stays off even if network request.
        mMediator.updateNumHighBandwidthRequests(1);
        mMediator.updateNumCellularRequests(1);
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_CELL_SETTING);

        // Cell radio should be turned on if in call broadcast received.
        Intent intent = new Intent(WearCellularMediator.ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED);
        intent.putExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.EXTRA_STATE_OFFHOOK);
        mContext.sendBroadcast(intent);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PHONE_CALL);
    }

    @Test
    public void testInPowerSave() {
        // Turn radio on first.
        mMediator.onProxyConnectedChange(false);

        when(mockPowerTracker.isInPowerSave()).thenReturn(true);
        mMediator.onPowerSaveModeChanged();

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_POWER_SAVE);

        // But the incall broadcast should turn it back on.
        Intent intent = new Intent(WearCellularMediator.ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED);
        intent.putExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.EXTRA_STATE_OFFHOOK);
        mContext.sendBroadcast(intent);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PHONE_CALL);
    }

    @Test
    public void testInPowerSave_CellOnNoAuto() {
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_OFF);
        mMediator.onBootCompleted(true);
        when(mockPowerTracker.isInPowerSave()).thenReturn(true);
        reset(mockHandler);

        mMediator.onPowerSaveModeChanged();

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NO_CELL_AUTO);
    }

    @Test
    public void tesCellularOffDuringPowerSaveSettingIsFalse() {
        // Turn radio on first.
        mMediator.onProxyConnectedChange(false);

        when(mockSettings.shouldTurnCellularOffDuringPowerSave()).thenReturn(false);
        when(mockPowerTracker.isInPowerSave()).thenReturn(true);

        mMediator.onPowerSaveModeChanged();

        // Because power save change should have been ignored, last change should have been due to
        // proxy disconnecting.
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testSimCardAbsent() {
        // Turn radio on first.
        mMediator.onProxyConnectedChange(false);

        reset(mockTelephonyManager);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_ABSENT);
        mContext.sendBroadcast(simIntent);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_SIM_ABSENT);

        // Incall broadcast should turn it back on.
        Intent intent = new Intent(WearCellularMediator.ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED);
        intent.putExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.EXTRA_STATE_OFFHOOK);
        mContext.sendBroadcast(intent);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PHONE_CALL);
    }

    @Test
    public void testEsimInitialBoot_noIccState_radioOff() {
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        // Turn radio on first.
        mMediator.onProxyConnectedChange(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                WearCellularMediator.ICC_WEAR_INITIAL_BOOT);
        mContext.sendBroadcast(simIntent);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_INITIAL_BOOT);
    }

    @Test
    public void testEsimInitialBoot_noIccState_cellOn() {
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_OFF);
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        mMediator.onBootCompleted(true);
        reset(mockHandler);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                WearCellularMediator.ICC_WEAR_INITIAL_BOOT);
        mContext.sendBroadcast(simIntent);


        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NO_CELL_AUTO);
    }

    @Test
    public void testDeviceDisabled_cellularNotAffected() {
        when(mockDeviceEnableSetting.affectsCellular()).thenReturn(false);
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);
        mMediator.onDeviceEnableChanged();
        verify(mockTelephonyManager, never()).setRadioPower(anyBoolean());

        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
        verify(mockTelephonyManager, never()).setRadioPower(anyBoolean());
    }

    @Test
    public void testDeviceDisabled_thenProxyDisconnect() {
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);
        mMediator.onDeviceEnableChanged();
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_DEVICE_DISABLED);

        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_DEVICE_DISABLED);
        verify(mockTelephonyManager, never()).setRadioPower(anyBoolean());
    }

    @Test
    public void testProxyDisconnect_thenDeviceDisabled() {
        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);

        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);
        mMediator.onDeviceEnableChanged();
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_DEVICE_DISABLED);

        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);
        mMediator.onDeviceEnableChanged();
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testProxyDisconnect_thenWifiConnectChange() {
        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);

        // Radio turned off when Wifi connected.
        mMediator.onWifiConnectedChange(true);
        verifyPowerChange(
                WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_WIFI_CONNECTED);

        // Radio turned back on when Wifi disconnected.
        mMediator.onWifiConnectedChange(false);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testWifiConnect_thenProxyDisconnect() {
        mMediator.onWifiConnectedChange(true);
        verifyPowerChange(
                WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_PROXY_CONNECTED);

        // Radio is kept off since Wifi is still connected.
        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(
                WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_WIFI_CONNECTED);

        // Radio turned on when Wifi disconnected.
        mMediator.onWifiConnectedChange(false);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testWifiConnect_cellularMediatingOff_doesNothing() {
        // Disable the mediating behavior.
        when(mockSettings.shouldTurnCellularOffWhenWifiConnected()).thenReturn(false);

        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);

        // Radio is kept on since the flag is disabled.
        mMediator.onWifiConnectedChange(true);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);

        // Radio is kept on since the flag is disabled.
        mMediator.onWifiConnectedChange(false);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testWifiConnect_noActiveVoWifi_doesNothing() {
        // Disable per device flag to test VoWIFI enabled status.
        when(mockSettings.shouldTurnCellularOffWhenWifiConnected()).thenReturn(false);

        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);

        // Radio is kept on since per device flag is disabled and VoWIFI is not yet active.
        mMediator.onWifiConnectedChange(true);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testVoWifiActive_thenWifiConnected() {
        // Disable per device flag to test VoWIFI enabled status.
        when(mockSettings.shouldTurnCellularOffWhenWifiConnected()).thenReturn(false);
        when(mImsManager.isWfcEnabledByPlatform()).thenReturn(true);
        when(mImsManager.isWfcEnabledByUser()).thenReturn(true);
        when(mImsManager.isWfcProvisionedOnDevice()).thenReturn(true);
        activeSubInfoList.add(mockEsimSubInfo);
        mMediator.updateActiveEsimSubscriptionStatus();

        mMediator.onProxyConnectedChange(false);
        verifyPowerChange(
                WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);

        // Radio is turned off since VoWIFI is active and WIFI is connected now.
        mMediator.onWifiConnectedChange(true);
        verifyPowerChange(
                WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_WIFI_CONNECTED);
    }

    @Test
    public void testDeviceDisabled_onBoot() {
        mMediator = new WearCellularMediator(
                mContext,
                mContentResolver,
                mockAlarmManager,
                mockTelephonyManager,
                mockEuiccManager,
                mockSubscriptionManager,
                mockSettings,
                mockPowerTracker,
                mockDeviceEnableSetting,
                mockWearConnectivityPackageManager,
                mockUserAbsentRadiosOffFlag,
                mMockSignalStateDetector,
                mTwinNumberBlocker);
        mMediator.mHandler = mockHandler;

        // Disable the device.
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);

        // disable cell lingering to allow easier testing for most test cases
        mMediator.setCellLingerDuration(-999);
        mMediator.onBootCompleted(false);
        mMediator.onUserUnlocked();

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_DEVICE_DISABLED);
        verify(mockTelephonyManager, never()).setRadioPower(anyBoolean());

        // Enabling the device should allow the radio to come on again.
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);
        mMediator.onDeviceEnableChanged();
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
    }

    @Test
    public void testGetRadioOnState() {
        reset(mockTelephonyManager);
        // We replaced mMediator.mHandler in setUp() so instantiate it again here so that we can
        // test the getRadioOnState() in the real Handler.
        mMediator = new WearCellularMediator(
                mContext,
                mockAlarmManager,
                mockTelephonyManager,
                mockEuiccManager,
                mockSubscriptionManager,
                mockSettings,
                mockPowerTracker,
                mockDeviceEnableSetting,
                mockWearConnectivityPackageManager,
                mockUserAbsentRadiosOffFlag);
        Message simAbsentMsg = Message.obtain(mMediator.mHandler,
                WearCellularMediator.MSG_DISABLE_CELL,
                new WearCellularMediator.RadioStateChangeReqInfo(
                        WearCellularMediator.Reason.OFF_SIM_ABSENT,
                        WearCellularMediator.TriggerEvent.SIM_STATE_CHANGED));
        mMediator.mHandler.handleMessage(simAbsentMsg);

        verify(mockWearConnectivityPackageManager).onCellularRadioState(false);
        verify(mockTelephonyManager).setRadioPower(false);
        verifyLatestDecision(WearCellularMediator.Reason.OFF_SIM_ABSENT);
    }

    @Test
    public void testOnSignalStateChanged_noProxy_noSignal() {
        mMediator.onProxyConnectedChange(false);
        reset(mockHandler);

        mMediator.onSignalStateChanged(SignalStateModel.STATE_NO_SIGNAL);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_NO_SIGNAL);
    }

    @Test
    public void testNoSignalWithProxyConnected_disablesRadio() {
        mMediator.onSignalStateChanged(SignalStateModel.STATE_NO_SIGNAL);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_NO_SIGNAL);
    }

    @Test
    public void testUnstableSignalWithProxyConnected_disablesRadio() {
        mMediator.onSignalStateChanged(SignalStateModel.STATE_UNSTABLE_SIGNAL);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_UNSTABLE_SIGNAL);
    }

    @Test
    public void testUnstableSignal_cellOnNoAuto() {
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_OFF);
        mMediator.onBootCompleted(true);
        reset(mockHandler);

        mMediator.onSignalStateChanged(SignalStateModel.STATE_UNSTABLE_SIGNAL);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NO_CELL_AUTO);
    }

    @Test
    public void testNoSignal_cellOnNoAuto() {
        when(mockSettings.getCellAutoSetting()).thenReturn(CELL_AUTO_OFF);
        mMediator.onBootCompleted(true);
        reset(mockHandler);

        mMediator.onSignalStateChanged(SignalStateModel.STATE_NO_SIGNAL);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NO_CELL_AUTO);
    }

    @Test
    public void testDetectorDisallowed_stopsDetector() {
        when(mockSettings.getMobileSignalDetectorAllowed()).thenReturn(false);
        mContentResolver.notifyChange(WearCellularMediator.MOBILE_SIGNAL_DETECTOR_URI, null);

        verify(mMockSignalStateDetector).stopDetector();
    }

    @Test
    public void testChangesBeforeBootDoNothing() {
        // Reinstantiate so we can test without boot completed
        mMediator = new WearCellularMediator(
                mContext,
                mContentResolver,
                mockAlarmManager,
                mockTelephonyManager,
                mockEuiccManager,
                mockSubscriptionManager,
                mockSettings,
                mockPowerTracker,
                mockDeviceEnableSetting,
                mockWearConnectivityPackageManager,
                mockUserAbsentRadiosOffFlag,
                mMockSignalStateDetector,
                mTwinNumberBlocker);
        // Disable cell linger for simpler verification
        mMediator.setCellLingerDuration(-999L);
        mMediator.mHandler = mockHandler;
        reset(mockHandler);

        // Poke at the various settings, ensure never trying to update state
        mMediator.onProxyConnectedChange(false);
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());

        mMediator.onProxyConnectedChange(true);
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());

        mMediator.updateNumHighBandwidthRequests(1);
        mMediator.updateNumCellularRequests(1);
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());

        mMediator.updateNumHighBandwidthRequests(0);
        mMediator.updateNumCellularRequests(0);
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());

        mMediator.onDeviceEnableChanged();
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());

        mMediator.onBootCompleted(true);
        mMediator.onUserUnlocked();

        // Verify state changes occur after boot completed
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_PROXY_CONNECTED);
    }

    @Test
    public void testDirectBoot_proxyDisconnected() {
        // Reinstantiate so we can test without boot completed
        mMediator = new WearCellularMediator(
                mContext,
                mContentResolver,
                mockAlarmManager,
                mockTelephonyManager,
                mockEuiccManager,
                mockSubscriptionManager,
                mockSettings,
                mockPowerTracker,
                mockDeviceEnableSetting,
                mockWearConnectivityPackageManager,
                mockUserAbsentRadiosOffFlag,
                mMockSignalStateDetector,
                mTwinNumberBlocker);
        // Disable cell linger for simpler verification
        mMediator.setCellLingerDuration(-999L);
        mMediator.mHandler = mockHandler;
        reset(mockHandler);

        mMediator.onBootCompleted(false);

        mMediator.onProxyConnectedChange(false);

        // Verify state changes occur after boot completed
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_DIRECTBOOT);
    }

    @Test
    public void testCellLingerWhenProxyConnected() {
        mMediator.setCellLingerDuration(5000L);
        reset(mockHandler);

        mMediator.onProxyConnectedChange(true);

        // verify that instead of toggling cell directly, we set an alarm to do so
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());
        verify(mockAlarmManager).setWindow(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyLong(), eq(mMediator.exitCellLingerIntent));

        // when the alarm hits, then we turn off the radio
        mContext.sendBroadcast(new Intent(WearCellularMediator.ACTION_EXIT_CELL_LINGER));
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_PROXY_CONNECTED);
    }

    /**
     * Even if we might turn on the radio for other reasons, if the reason we're keeping the
     * radio off reduces to PROXY_CONNECTED, then lingering is enforced by implication.
     */
    @Test
    public void testImpliedCellLingering() {
        mMediator.onProxyConnectedChange(true);
        reset(mockHandler);

        mMediator.setCellLingerDuration(5000L);
        mMediator.updateNumCellularRequests(1);
        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_NETWORK_REQUEST);

        reset(mockHandler);
        mMediator.updateNumCellularRequests(0);
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());
        verify(mockAlarmManager).setWindow(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyLong(), eq(mMediator.exitCellLingerIntent));

        // when the alarm hits, then we turn off the radio
        mContext.sendBroadcast(new Intent(WearCellularMediator.ACTION_EXIT_CELL_LINGER));
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_PROXY_CONNECTED);
    }

    @Test
    public void testCellLingerOnlySetsOneAlarm() {
        mMediator.setCellLingerDuration(5000L);
        reset(mockHandler);

        // call update a few times in a row
        mMediator.onProxyConnectedChange(true);
        mMediator.onProxyConnectedChange(true);
        mMediator.onProxyConnectedChange(true);
        mMediator.onProxyConnectedChange(true);

        verify(mockAlarmManager, times(1)).setWindow(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyLong(), eq(mMediator.exitCellLingerIntent));

        // but after the alarm hits, we can set another one again (but only one)
        mContext.sendBroadcast(new Intent(WearCellularMediator.ACTION_EXIT_CELL_LINGER));
        mMediator.onProxyConnectedChange(true);
        mMediator.onProxyConnectedChange(true);
        mMediator.onProxyConnectedChange(true);
        verify(mockAlarmManager, times(2)).setWindow(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyLong(), eq(mMediator.exitCellLingerIntent));
    }

    private void verifyLatestDecision(WearCellularMediator.Reason reason) {
        assertEquals(reason, mMediator.getDecisionHistory().getMostRecentEvent().reason);
    }

    private void verifyPowerChange(int what, WearCellularMediator.Reason reason) {
        verify(mockHandler, atLeastOnce()).sendMessage(msgCaptor.capture());
        WearCellularMediator.Reason r;
        r = ((WearCellularMediator.RadioStateChangeReqInfo) msgCaptor.getValue().obj).mReason;
        assertEquals("Failure reason: " + r.name(), what, msgCaptor.getValue().what);
        assertEquals(reason, r);
    }
}
