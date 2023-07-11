package com.android.clockwork.cellular;

import static com.android.clockwork.cellular.WearCellularMediator.ACTION_ESIM_TEST_MODE;
import static com.android.clockwork.cellular.WearCellularMediator.CELL_AUTO_OFF;
import static com.android.clockwork.cellular.WearCellularMediator.CELL_AUTO_ON;
import static com.android.clockwork.cellular.WearCellularMediator.ENABLE_CELLULAR_ON_BOOT_URI;
import static com.android.clockwork.cellular.WearCellularMediator.EXTRA_IN_ESIM_TEST_MODE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;

import com.android.clockwork.common.DeviceEnableSetting;
import com.android.clockwork.connectivity.WearConnectivityPackageManager;
import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.power.PowerTracker;
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
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.BitSet;

/** Test for {@link WearCellularMediator} */
@RunWith(RobolectricTestRunner.class)
public class WearCellularMediatorTest {
    private static final String CELL_AUTO_SETTING_KEY = "clockwork_cell_auto_setting";
    private static final Uri CELL_AUTO_SETTING_URI =
            Settings.System.getUriFor(CELL_AUTO_SETTING_KEY);
    private static final Uri CELL_ON_URI = Settings.Global.getUriFor(Settings.Global.CELL_ON);
    final ShadowApplication shadowApplication = ShadowApplication.getInstance();
    private final ArrayList<SubscriptionInfo> activeSubInfoList = new ArrayList<>();
    @Captor
    ArgumentCaptor<Message> msgCaptor;
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
    private ContentResolver mContentResolver;
    private Context mContext;
    private WearCellularMediator mMediator;

    private BitSet mBitSet;

    @Before
    public void setUp() {
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
        when(mockSettings.getMobileSignalDetectorAllowed()).thenReturn(true);
        when(mockPowerTracker.isInPowerSave()).thenReturn(false);

        when(mockDeviceEnableSetting.affectsCellular()).thenReturn(true);
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);

        when(mockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(true);

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
                mMockSignalStateDetector);
        mMediator.mHandler = mockHandler;

        // disable cell lingering to allow easier testing for most test cases
        mMediator.setCellLingerDuration(-999);
        mMediator.onBootCompleted(true);
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
        mMediator.updateProxyConnected(false);

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
        mMediator.updateProxyConnected(false);

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
        mMediator.updateProxyConnected(false);

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

        mMediator.updateProxyConnected(false);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_SIM_ABSENT);
    }

    @Test
    public void testProxyDisconnect_EsimDevice_NoEsimAndPsimNotReady() {
        when(mockSettings.isWearEsimDevice()).thenReturn(true);
        when(mockSettings.isEsimProfileDeactivated()).thenReturn(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_NOT_READY);
        mContext.sendBroadcast(simIntent);

        mMediator.updateProxyConnected(false);

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

        mMediator.updateProxyConnected(false);

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

        mMediator.updateProxyConnected(false);

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

        mMediator.updateProxyConnected(false);

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
        mMediator.updateProxyConnected(false);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_SIM_ABSENT);
    }

    @Test
    public void testEnableVoiceTwinning_DoesNothingWhenAlreadyEnabled() {
        when(mockSettings.isVoiceTwinningEnabled()).thenReturn(true);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);

        verify(mockSettings, never()).setCellAutoSetting(CELL_AUTO_OFF);
        verify(mockSettings, never()).setCellAutoSetting(CELL_AUTO_ON);
    }

    @Test
    public void testToggleVoiceTwinning_ChangesCellAuto() {
        when(mockSettings.isVoiceTwinningEnabled()).thenReturn(false);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);

        verify(mockSettings).setCellAutoSetting(CELL_AUTO_OFF);

        reset(mockSettings);
        when(mockSettings.isVoiceTwinningEnabled()).thenReturn(true);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);

        verify(mockSettings).setCellAutoSetting(anyInt());
    }

    @Test
    public void testToggleVoiceTwinning_ChangesCellAutoExactlyOnce() {
        when(mockSettings.isVoiceTwinningEnabled()).thenReturn(false);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);

        verify(mockSettings, times(1)).setCellAutoSetting(CELL_AUTO_OFF);

        reset(mockSettings);
        when(mockSettings.isVoiceTwinningEnabled()).thenReturn(true);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);
        mContentResolver.notifyChange(
                WearCellularMediatorSettings.VOICE_TWINNING_SETTING_URI, null);

        verify(mockSettings, times(1)).setCellAutoSetting(CELL_AUTO_ON);
    }

    @Test
    public void testTurnCellStateOff() {
        // Turn radio on first.
        mMediator.updateProxyConnected(false);

        reset(mockTelephonyManager);
        when(mockSettings.getCellState()).thenReturn(PhoneConstants.CELL_OFF_FLAG);
        mContentResolver.notifyChange(WearCellularMediator.CELL_ON_URI, null);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_CELL_SETTING);

        // Cell radio stays off even if proxy disconnect.
        mMediator.updateProxyConnected(false);
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
        mMediator.updateProxyConnected(false);

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
    public void tesCellularOffDuringPowerSaveSettingIsFalse() {
        // Turn radio on first.
        mMediator.updateProxyConnected(false);

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
        mMediator.updateProxyConnected(false);

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
        mMediator.updateProxyConnected(false);

        Intent simIntent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        simIntent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                WearCellularMediator.ICC_WEAR_INITIAL_BOOT);
        mContext.sendBroadcast(simIntent);

        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_INITIAL_BOOT);
    }

    @Test
    public void testDeviceDisabled_cellularNotAffected() {
        when(mockDeviceEnableSetting.affectsCellular()).thenReturn(false);
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);
        mMediator.onDeviceEnableChanged();
        verify(mockTelephonyManager, never()).setRadioPower(anyBoolean());

        mMediator.updateProxyConnected(false);
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

        mMediator.updateProxyConnected(false);
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_DEVICE_DISABLED);
        verify(mockTelephonyManager, never()).setRadioPower(anyBoolean());
    }

    @Test
    public void testProxyDisconnect_thenDeviceDisabled() {
        mMediator.updateProxyConnected(false);
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
                mMockSignalStateDetector);
        mMediator.mHandler = mockHandler;

        // Disable the device.
        when(mockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);

        // disable cell lingering to allow easier testing for most test cases
        mMediator.setCellLingerDuration(-999);
        mMediator.onBootCompleted(false);

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
        // Trying to turn radio power off when radio is off.
        when(mockSettings.getRadioOnState()).thenReturn(WearCellularMediator.RADIO_ON_STATE_OFF);
        Message simAbsentMsg = Message.obtain(mMediator.mHandler,
                WearCellularMediator.MSG_DISABLE_CELL,
                new WearCellularMediator.RadioStateChangeReqInfo(
                        WearCellularMediator.Reason.OFF_SIM_ABSENT,
                        WearCellularMediator.TriggerEvent.SIM_STATE_CHANGED));
        mMediator.mHandler.handleMessage(simAbsentMsg);

        // Don't change radio power since it's already off
        verify(mockTelephonyManager, never()).setRadioPower(false);

        // Trying to turn radio power off when radio is on.
        reset(mockTelephonyManager);
        when(mockSettings.getRadioOnState()).thenReturn(WearCellularMediator.RADIO_ON_STATE_ON);
        mMediator.mHandler.handleMessage(simAbsentMsg);

        verify(mockWearConnectivityPackageManager, times(2)).onCellularRadioState(false);
        verify(mockTelephonyManager).setRadioPower(false);
        verifyLatestDecision(WearCellularMediator.Reason.OFF_SIM_ABSENT);
    }

    @Test
    public void testBadSignalWithProxyDisconnected_doesNothing() {
        mMediator.updateProxyConnected(false);

        mMediator.onSignalStateChanged(SignalStateModel.STATE_NO_SIGNAL);
        mMediator.onSignalStateChanged(SignalStateModel.STATE_UNSTABLE_SIGNAL);

        verifyPowerChange(WearCellularMediator.MSG_ENABLE_CELL,
                WearCellularMediator.Reason.ON_PROXY_DISCONNECTED);
        verify(mockTelephonyManager, never()).setRadioPower(anyBoolean());
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
                mMockSignalStateDetector);
        // Disable cell linger for simpler verification
        mMediator.setCellLingerDuration(-999L);
        mMediator.mHandler = mockHandler;
        reset(mockHandler);

        // Poke at the various settings, ensure never trying to update state
        mMediator.updateProxyConnected(false);
        verify(mockHandler, never()).sendMessage(msgCaptor.capture());

        mMediator.updateProxyConnected(true);
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

        // Verify state changes occur after boot completed
        verifyPowerChange(WearCellularMediator.MSG_DISABLE_CELL,
                WearCellularMediator.Reason.OFF_PROXY_CONNECTED);
    }

    @Test
    public void testCellLingerWhenProxyConnected() {
        mMediator.setCellLingerDuration(5000L);
        reset(mockHandler);

        mMediator.updateProxyConnected(true);

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
        mMediator.updateProxyConnected(true);
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
        mMediator.updateProxyConnected(true);
        mMediator.updateProxyConnected(true);
        mMediator.updateProxyConnected(true);
        mMediator.updateProxyConnected(true);

        verify(mockAlarmManager, times(1)).setWindow(eq(AlarmManager.ELAPSED_REALTIME),
                anyLong(), anyLong(), eq(mMediator.exitCellLingerIntent));

        // but after the alarm hits, we can set another one again (but only one)
        mContext.sendBroadcast(new Intent(WearCellularMediator.ACTION_EXIT_CELL_LINGER));
        mMediator.updateProxyConnected(true);
        mMediator.updateProxyConnected(true);
        mMediator.updateProxyConnected(true);
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
