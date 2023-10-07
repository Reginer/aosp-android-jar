package com.android.clockwork.bluetooth;

import static com.android.clockwork.common.WearBluetoothSettings.PROXY_SCORE_BLE;
import static com.android.clockwork.common.WearBluetoothSettings.PROXY_SCORE_CLASSIC;
import static com.android.clockwork.common.WearBluetoothSettings.PROXY_SCORE_ON_CHARGER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.android.clockwork.bluetooth.proxy.ProxyGattServer;
import com.android.clockwork.bluetooth.proxy.ProxyPinger;
import com.android.clockwork.bluetooth.proxy.ProxyServiceConfig;
import com.android.clockwork.common.DeviceEnableSetting;
import com.android.clockwork.common.EventHistory;
import com.android.clockwork.common.ThermalEmergencyTracker;
import com.android.clockwork.common.ThermalEmergencyTracker.ThermalEmergencyMode;
import com.android.clockwork.common.WearResourceUtil;
import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.power.PowerTracker;
import com.android.clockwork.power.TimeOnlyMode;
import com.android.internal.util.IndentingPrintWriter;

import com.google.android.collect.Lists;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowBluetoothManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

import java.net.InetAddress;
import java.time.Duration;
import java.util.BitSet;
import java.util.List;

/**
 * Test for {@link WearBluetoothMediator}
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    shadows = {WearBluetoothMediatorTest.ShadowDiscoveryBluetoothManager.class})
@LooperMode(LooperMode.Mode.LEGACY)
public class WearBluetoothMediatorTest {
    private static final String REASON = "";
    private static final int FAKE_VALID_PSM_VALUE = 192;
    private static final int CHANNEL_CHANGE_ID = 1234;
    private static final int PING_INTERVAL_SECONDS = 10;

    private static final ProxyServiceConfig PROXY_CONFIG_ANDROID_V1 =
        ProxyServiceConfig.forAndroidV1();
    private static final ProxyServiceConfig PROXY_CONFIG_IOS_V1 =
        ProxyServiceConfig.forIosV1(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID);

    private final ShadowApplication shadowApplication = ShadowApplication.getInstance();

    @Captor
    ArgumentCaptor<Message> mMsgCaptor;
    @Mock
    AlarmManager mMockAlarmManager;
    @Mock
    BluetoothAdapter mMockBtAdapter;
    @Mock
    BluetoothClass mMockPeripheralBluetoothClass;
    @Mock
    BluetoothClass mMockPhoneBluetoothClass;
    @Mock
    BluetoothDevice mMockBtPeripheral;
    @Mock
    BluetoothDevice mMockBtPhone;
    @Mock
    BluetoothLogger mMockBtLogger;
    @Mock
    BluetoothShardRunner mMockShardRunner;
    @Mock
    CompanionTracker mMockCompanionTracker;
    @Mock
    Handler mMockHandler;
    @Mock
    IndentingPrintWriter mMockIndentingPrintWriter;
    @Mock
    PowerTracker mMockPowerTracker;
    @Mock
    BooleanFlag mMockUserAbsentRadiosOffFlag;
    @Mock
    WearBluetoothMediatorSettings mMockWearBluetoothMediatorSettings;
    @Mock
    TimeOnlyMode mMockTimeOnlyMode;
    @Mock
    DeviceInformationGattServer mMockDeviceInformationServer;
    @Mock
    ProxyGattServer mMockProxyGattServer;
    @Mock
    ProxyPinger mMockProxyPinger;
    @Mock
    DeviceEnableSetting mMockDeviceEnableSetting;
    @Mock
    BluetoothGattServer mMockGattServer;

    private Context mContext;
    private WearBluetoothMediator mMediator;
    private BitSet mBitSet;
    private Resources mResources;

    // Can't be static final because init requires exception handling
    private List<InetAddress> mFakeDnsServers;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        BluetoothManager bluetoothManager =
            (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        ShadowDiscoveryBluetoothManager shadowBluetoothManager =
            (ShadowDiscoveryBluetoothManager) Shadows.shadowOf(bluetoothManager);
        shadowBluetoothManager.setGattServer(mMockGattServer);

        when(mMockBtPhone.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(mMockBtPhone.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        when(mMockBtPhone.getBluetoothClass()).thenReturn(mMockPhoneBluetoothClass);
        when(mMockBtPhone.isConnected()).thenReturn(true);

        when(mMockBtPeripheral.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(mMockBtPeripheral.getAddress()).thenReturn("12:34:56:78:90:12");
        when(mMockBtPeripheral.getBluetoothClass()).thenReturn(mMockPeripheralBluetoothClass);

        when(mMockPhoneBluetoothClass.getMajorDeviceClass())
                .thenReturn(BluetoothClass.Device.Major.PHONE);
        when(mMockPeripheralBluetoothClass.getMajorDeviceClass())
                .thenReturn(BluetoothClass.Device.Major.PERIPHERAL);

        when(mMockBtAdapter.isEnabled()).thenReturn(true);
        // any non-zero timeout; most tests don't rely on actual timeout
        when(mMockBtAdapter.getDiscoverableTimeout()).thenReturn(Duration.ofMinutes(2));
        when(mMockCompanionTracker.getCompanion()).thenReturn(mMockBtPhone);
        when(mMockCompanionTracker.getBluetoothClassicCompanion()).thenReturn(mMockBtPhone);

        when(mMockCompanionTracker.getCompanionAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        when(mMockCompanionTracker.isCompanionBle()).thenReturn(false);
        when(mMockPowerTracker.isCharging()).thenReturn(false);

        when(mMockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(true);

        when(mMockWearBluetoothMediatorSettings.getIsInAirplaneMode()).thenReturn(false);
        when(mMockWearBluetoothMediatorSettings.getIsSettingsPreferenceBluetoothOn()).thenReturn(
                true);

        mFakeDnsServers = Lists.newArrayList(
                InetAddress.getByName("1.2.3.4"),
                InetAddress.getByName("5.6.7.8"));
        when(mMockWearBluetoothMediatorSettings.getDnsServers()).thenReturn(mFakeDnsServers);

        when(mMockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);

        mResources = Resources.create(mContext, true);
        shadowOf(mContext.getPackageManager()).addPackage(WearResourceUtil.WEAR_RESOURCE_PACKAGE);
        ShadowPackageManager.resources.put(WearResourceUtil.WEAR_RESOURCE_PACKAGE, mResources);

        mMediator = new WearBluetoothMediator(
                mContext,
                mMockAlarmManager,
                mMockWearBluetoothMediatorSettings,
                mMockBtAdapter,
                mMockBtLogger,
                mMockShardRunner,
                mMockCompanionTracker,
                mMockPowerTracker,
                mMockDeviceEnableSetting,
                mMockUserAbsentRadiosOffFlag,
                mMockTimeOnlyMode,
                mMockDeviceInformationServer,
                mMockProxyGattServer,
                mMockProxyPinger
        );
        mBitSet = new BitSet(PowerTracker.MAX_DOZE_MODE_INDEX);
        when(mMockPowerTracker.getDozeModeAllowListedFeatures())
                .thenReturn(mBitSet);
    }

    @Test
    public void testConstructorAndOnBootCompleted() {
        verify(mMockCompanionTracker).addListener(mMediator);
        verify(mMockPowerTracker).addListener(mMediator);
        verify(mMockTimeOnlyMode).addListener(mMediator);
        verify(mMockUserAbsentRadiosOffFlag).addListener(any());

        mMediator.onBootCompleted();

        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(BluetoothDevice.ACTION_ACL_CONNECTED)));
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED)));
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(BluetoothAdapter.ACTION_STATE_CHANGED)));
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED)));
    }

    @Test
    public void testOnBootCompletedWhenAdapterEnabled() {
        mMediator.onBootCompleted();
        verify(mMockCompanionTracker).onBluetoothAdapterReady();
        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
        verify(mMockDeviceInformationServer).start();
    }

    @Test
    public void testOnBootCompletedWhenAdapterDisabled() {
        when(mMockBtAdapter.isEnabled()).thenReturn(false);
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        mMediator.onBootCompleted();

        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_BOOT_AUTO);
        verify(mMockWearBluetoothMediatorSettings).setSettingsPreferenceBluetoothOn(true);
        verifyNoMoreInteractions(mMockShardRunner);
        verify(mMockCompanionTracker, never()).onBluetoothAdapterReady();
        verify(mMockProxyGattServer, never()).start();
    }

    @Test
    public void testAdapterEnabledWithoutPairedDeviceDoesNotStartShards() {
        when(mMockCompanionTracker.getCompanion()).thenReturn(null);
        when(mMockBtAdapter.isEnabled()).thenReturn(false);
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        sendBluetoothAdapterOnIntents();

        verify(mMockCompanionTracker).onBluetoothAdapterReady();
        verifyNoMoreInteractions(mMockShardRunner);
    }

    @Test
    public void testAdapterEnabledWithoutPairedDeviceStartsGattServer() {
        when(mMockCompanionTracker.getCompanion()).thenReturn(null);
        when(mMockBtAdapter.isEnabled()).thenReturn(false);
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        sendBluetoothAdapterOnIntents();

        verify(mMockProxyGattServer).start();
    }

    @Test
    public void testAdapterEnabledWithLePairedDeviceStartsGattServer() {
        when(mMockCompanionTracker.isCompanionBle()).thenReturn(true);
        when(mMockBtAdapter.isEnabled()).thenReturn(false);
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        sendBluetoothAdapterOnIntents();

        verify(mMockProxyGattServer).start();
    }

    @Test
    public void testAdapterEnabledWithClassicPairedDeviceDoesNotStartGattServer() {
        when(mMockCompanionTracker.isCompanionBle()).thenReturn(false);
        when(mMockBtAdapter.isEnabled()).thenReturn(false);
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        sendBluetoothAdapterOnIntents();

        verify(mMockProxyGattServer, never()).start();
    }

    @Test
    public void testAdapterEnabledWithInfiniteTimeoutAndPaired() {
        reset(mMockBtAdapter);
        when(mMockBtAdapter.isEnabled()).thenReturn(true);
        when(mMockBtAdapter.getDiscoverableTimeout()).thenReturn(Duration.ZERO);

        mMediator.onBootCompleted();

        verify(mMockBtAdapter).setDiscoverableTimeout(
                Duration.ofSeconds(WearBluetoothMediator.DEFAULT_DISCOVERABLE_TIMEOUT_SECS));
    }

    @Test
    public void testAdapterEnabledWithFiniteTimeoutAndPaired() {
        when(mMockBtAdapter.getDiscoverableTimeout()).thenReturn(Duration.ofMinutes(12));
        mMediator.onBootCompleted();
        verify(mMockBtAdapter, never()).setDiscoverableTimeout(any(Duration.class));
    }

    @Test
    public void testAdapterEnabledWithInfiniteTimeoutAndUnpaired() {
        when(mMockCompanionTracker.getCompanion()).thenReturn(null);
        when(mMockBtAdapter.getDiscoverableTimeout()).thenReturn(Duration.ZERO);

        mMediator.onBootCompleted();

        verify(mMockBtAdapter, never()).setDiscoverableTimeout(any(Duration.class));
    }

    @Test
    public void testAdapterEnabledWithInfiniteTimeout2ndBoot() {
        reset(mMockBtAdapter);
        when(mMockBtAdapter.isEnabled()).thenReturn(true);
        when(mMockBtAdapter.getDiscoverableTimeout()).thenReturn(Duration.ZERO);
        mMediator.onBootCompleted();
        reset(mMockBtAdapter);
        when(mMockBtAdapter.isEnabled()).thenReturn(true);
        when(mMockBtAdapter.getDiscoverableTimeout()).thenReturn(Duration.ZERO);

        mMediator.onBootCompleted();

        verify(mMockBtAdapter, never()).setDiscoverableTimeout(any(Duration.class));
    }

    @Test
    public void testFirstAdapterEnableStartsNoShard() {
        when(mMockBtAdapter.isEnabled()).thenReturn(false);
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        sendBluetoothAdapterOnIntents();

        verify(mMockCompanionTracker).onBluetoothAdapterReady();

        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
        reset(mMockCompanionTracker, mMockShardRunner, mMockAlarmManager);

        when(mMockCompanionTracker.getCompanion()).thenReturn(mMockBtPhone);

        // the second broadcast should only call startHfcShard and do nothing else
        sendBluetoothAdapterOnIntents();
        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
        verifyNoMoreInteractions(mMockShardRunner);
        verify(mMockCompanionTracker, never()).onBluetoothAdapterReady();
        verify(mMockAlarmManager, never()).set(anyInt(), anyLong(), any(PendingIntent.class));
    }

    @Test
    public void testAdapterDisableStopsBothShards() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        sendBluetoothAdapterOffIntents();

        verify(mMockShardRunner).stopHfcShard();
        verify(mMockShardRunner).stopProxyShard();
    }

    @Test
    public void testAdapterDisableDoesNotStopGattServers() {
        mMediator.onBootCompleted();
        reset(
                mMockShardRunner,
                mMockAlarmManager,
                mMockDeviceInformationServer,
                mMockProxyGattServer);

        sendBluetoothAdapterOffIntents();

        verify(mMockDeviceInformationServer, never()).stop();
        verify(mMockProxyGattServer, never()).stop();
    }

    @Test
    public void testAdapterTemporarilyTurningOffStopsAndStartsGattServers() {
        when(mMockCompanionTracker.isCompanionBle()).thenReturn(true);
        mMediator.onBootCompleted();
        reset(
                mMockShardRunner,
                mMockAlarmManager,
                mMockDeviceInformationServer,
                mMockProxyGattServer);

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        mContext.sendBroadcast(intent);

        intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        mContext.sendBroadcast(intent);

        verify(mMockDeviceInformationServer).stop();
        verify(mMockDeviceInformationServer).start();
        verify(mMockProxyGattServer).stop();
        verify(mMockProxyGattServer).start();
    }

    @Test
    public void testEnableHfp_updatesBluetoothDisabledProfiles_startsBtReboot() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        toggleHfp(true);

        assertTrue(isHfpEnabled());
        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_HFP_ENABLE);
    }

    @Test
    public void testEnableHfp_hfpAlreadyEnabled_doesNotRebootBt() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        assertTrue(isHfpEnabled()); // a confirmation that HFP was already enabled before the toggle
        toggleHfp(/* enable= */ true, /* firstToggleToOppositeState= */ false);

        verify(mMockHandler, never()).sendMessage(any());
    }

    @Test
    public void testDisableHfp_updatesBluetoothDisabledProfiles() {
        mMediator.onBootCompleted();

        toggleHfp(false);

        assertFalse(isHfpEnabled());
    }

    @Test
    public void testEnableHfp_withoutPermission_doesNotEnableHfp() {
        mMediator.onBootCompleted();

        toggleHfp(false);
        toggleHfp(/* enable= */ true, /* fromUser= */ true, /* includePermission= */ false);

        assertFalse(isHfpEnabled());
    }

    @Test
    public void testDisableHfp_withoutPermission_doesNotDisableHfp() {
        mMediator.onBootCompleted();

        toggleHfp(true);
        toggleHfp(/* enable= */ false, /* fromUser= */ true, /* includePermission= */ false);

        assertTrue(isHfpEnabled());
    }

    @Test
    public void testToggleHfp_notSetByUser() {
        mMediator.onBootCompleted();

        toggleHfp(/* enable= */ false, /* fromUser= */ false, /* includePermission= */ true);

        assertEquals(Settings.Global.Wearable.HFP_CLIENT_UNSET, getUserHfpClientSetting());
    }

    @Test
    public void testEnableHfp_setByUser() {
        mMediator.onBootCompleted();

        toggleHfp(/* enable= */ true, /* fromUser= */ true, /* includePermission= */ true);

        assertEquals(Settings.Global.Wearable.HFP_CLIENT_ENABLED, getUserHfpClientSetting());
    }

    @Test
    public void testDisableHfp_setByUser() {
        mMediator.onBootCompleted();

        toggleHfp(/* enable= */ false, /* fromUser= */ true, /* includePermission= */ true);

        assertEquals(Settings.Global.Wearable.HFP_CLIENT_DISABLED, getUserHfpClientSetting());
    }

    @Test
    public void testAttemptEnablingBluetoothAfterEnablingHfp() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        toggleHfp(true);
        // Simulate BT state change from being on, to turning off.
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        mContext.sendBroadcast(intent);

        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_HFP_ENABLE);
    }

    @Test
    public void testPairedWithBluetoothPhoneStartsShards() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        when(mMockCompanionTracker.getCompanion()).thenReturn(mMockBtPhone);
        when(mMockCompanionTracker.getBluetoothClassicCompanion()).thenReturn(mMockBtPhone);
        mMediator.onCompanionChanged();

        verify(mMockShardRunner).startHfcShard(mMockBtPhone);
        verify(mMockShardRunner).startProxyShard(
                PROXY_SCORE_CLASSIC, mFakeDnsServers, mMediator, "Companion Found",
                PROXY_CONFIG_ANDROID_V1);
    }

    @Test
    public void testPairedWithBleAndBtStartsHfcShard() {
        setupBleCompanion();

        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        when(mMockCompanionTracker.getBluetoothClassicCompanion()).thenReturn(mMockBtPhone);
        mMediator.onCompanionChanged();

        verify(mMockShardRunner).startHfcShard(mMockBtPhone);
        verify(mMockShardRunner, never()).startProxyShard(
                anyInt(), any(), any(), anyString(), any());
    }

    @Test
    public void testPairedWithBlePhoneOnlyDoesntStartHfcShard() {
        setupBleCompanion();

        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        when(mMockCompanionTracker.getCompanion()).thenReturn(mMockBtPhone);
        mMediator.onCompanionChanged();

        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
    }

    @Test
    public void testPairedWithBlePhoneSetsProxyGattServerCompanionDevice() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        setupBleCompanion();
        when(mMockCompanionTracker.getCompanion()).thenReturn(mMockBtPhone);
        mMediator.onCompanionChanged();

        verify(mMockProxyGattServer).setCompanionDevice(mMockBtPhone);
    }

    @Test
    public void testAclEventsStartAndStopBothShard() {
        mMediator.onBootCompleted();
        mMediator.onUserUnlocked();
        reset(mMockShardRunner, mMockAlarmManager);

        Intent aclConnected = new Intent(BluetoothDevice.ACTION_ACL_CONNECTED);
        aclConnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        when(mMockBtPhone.isConnected()).thenReturn(true);

        mContext.sendBroadcast(aclConnected);
        verify(mMockShardRunner).startProxyShard(PROXY_SCORE_CLASSIC, mFakeDnsServers, mMediator,
                "Companion Connected", PROXY_CONFIG_ANDROID_V1);
        verify(mMockShardRunner).startHfcShard(mMockBtPhone);

        reset(mMockShardRunner);
        Intent aclDisconnected = new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        aclDisconnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        when(mMockBtPhone.isConnected()).thenReturn(false);
        mContext.sendBroadcast(aclDisconnected);
        verify(mMockShardRunner).stopProxyShard();
        verify(mMockShardRunner).stopHfcShard();
    }

    @Test
    public void testAclEventsDoNotStartProxyShardWhenPairedWithBlePhone() {
        setupBleCompanion();

        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        Intent aclConnected = new Intent(BluetoothDevice.ACTION_ACL_CONNECTED);
        aclConnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        when(mMockBtPhone.isConnected()).thenReturn(true);

        mContext.sendBroadcast(aclConnected);
        verify(mMockShardRunner, never()).startProxyShard(anyInt(), any(), any(), any(), any());
    }

    @Test
    public void testAclEventsForNonCompanionDeviceDoNothing() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        Intent aclConnected = new Intent(BluetoothDevice.ACTION_ACL_CONNECTED);
        aclConnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPeripheral);
        when(mMockBtPeripheral.isConnected()).thenReturn(true);

        mContext.sendBroadcast(aclConnected);
        verify(mMockShardRunner, never())
                .startProxyShard(
                        PROXY_SCORE_CLASSIC,
                        mFakeDnsServers,
                        mMediator,
                        REASON,
                        PROXY_CONFIG_ANDROID_V1);

        reset(mMockShardRunner);
        Intent aclDisconnected = new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        aclDisconnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPeripheral);
        when(mMockBtPeripheral.isConnected()).thenReturn(true);
        mContext.sendBroadcast(aclDisconnected);
        verify(mMockShardRunner, never()).stopProxyShard();
    }

    @Test
    public void testTimeOnlyModeDisablesBluetooth() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        mMediator.onTimeOnlyModeChanged(true);
        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_TIME_ONLY_MODE);

        mMediator.onTimeOnlyModeChanged(false);
        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_AUTO);
    }

    @Test
    public void testActivityModeDisablesBluetooth() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        mMediator.updateActivityMode(true);
        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_ACTIVITY_MODE);

        mMediator.updateActivityMode(false);
        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_AUTO);
    }

    @Test
    public void testCellOnlyModeDisablesBluetooth() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        mMediator.updateCellOnlyMode(true);
        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_CELL_ONLY_MODE);

        mMediator.updateCellOnlyMode(false);
        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_AUTO);
    }

    @Test
    public void testThermalEmergencyDisablesBluetooth() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        mMediator.updateThermalEmergencyMode(
                new ThermalEmergencyMode(ThermalEmergencyTracker.THERMAL_EMERGENCY_LEVEL_BT));
        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_THERMAL_EMERGENCY);

        mMediator.updateThermalEmergencyMode(new ThermalEmergencyMode(0));
        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_AUTO);
    }

    @Test
    public void testDisabledInAirplaneMode() {
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;
        mMediator.onAirplaneModeSettingChanged(true);
        mMediator.onSettingsPreferenceBluetoothSettingChanged(true);

        mMediator.onDeviceIdleModeChanged();

        mMediator.onSettingsPreferenceBluetoothSettingChanged(false);
        mMediator.onDeviceIdleModeChanged();

        verify(mMockHandler, never()).sendMessage(any());
    }

    @Test
    public void testDisabledUserPreferenceSettings() {
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;
        mMediator.onAirplaneModeSettingChanged(false);
        mMediator.onSettingsPreferenceBluetoothSettingChanged(false);

        mMediator.onDeviceIdleModeChanged();

        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_SETTINGS_PREFERENCE);
    }

    @Test
    public void testDeviceIdle() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        when(mMockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_USER_ABSENT);

        when(mMockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(false);
        mMediator.onUserAbsentRadiosOffChanged(false);
        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_AUTO);

        when(mMockPowerTracker.isDeviceIdle()).thenReturn(false);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_AUTO);

        when(mMockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_AUTO);

        when(mMockUserAbsentRadiosOffFlag.isEnabled()).thenReturn(true);
        mMediator.onUserAbsentRadiosOffChanged(true);
        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_USER_ABSENT);

        when(mMockPowerTracker.isDeviceIdle()).thenReturn(false);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.ON_AUTO);
    }

    @Test
    public void testDeviceIdleAllowListedFeature() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;
        mBitSet.set(PowerTracker.DOZE_MODE_BT_INDEX);

        when(mMockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verify(mMockHandler, never()).sendMessage(mMsgCaptor.capture());
    }

    @Test
    public void testDeviceIdleBlockListedFeature() {
        mMediator.onBootCompleted();
        // Replace handler with mock
        mMediator.mRadioPowerHandler = mMockHandler;

        when(mMockPowerTracker.isDeviceIdle()).thenReturn(true);
        mMediator.onDeviceIdleModeChanged();
        verifyPowerChange(WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_USER_ABSENT);
    }

    @Test
    public void testLogCompanionPairing() {
        mMediator.onBootCompleted();
        when(mMockCompanionTracker.isCompanionBle()).thenReturn(true);
        mMediator.onCompanionChanged();
        verify(mMockBtLogger).logCompanionPairingEvent(true);
        reset(mMockBtLogger);

        when(mMockCompanionTracker.isCompanionBle()).thenReturn(false);
        mMediator.onCompanionChanged();
        verify(mMockBtLogger).logCompanionPairingEvent(false);
    }

    @Test
    public void testLogProxyConnectionChanges() {
        mMediator.onBootCompleted();
        mMediator.onProxyConnectionChange(true, PROXY_SCORE_CLASSIC, false);
        verify(mMockBtLogger).logProxyConnectionChange(true);
        reset(mMockBtLogger);

        mMediator.onProxyConnectionChange(false, PROXY_SCORE_CLASSIC, false);
        verify(mMockBtLogger).logProxyConnectionChange(false);
        reset(mMockBtLogger);
    }

    @Test
    public void testLogUnexpectedPairing() {
        mMediator.onBootCompleted();
        Intent unexpectedBondEvent = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        unexpectedBondEvent.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        unexpectedBondEvent.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                BluetoothDevice.BOND_BONDED);
        unexpectedBondEvent.putExtra(BluetoothDevice.EXTRA_BOND_STATE,
                BluetoothDevice.BOND_BONDING);
        mContext.sendBroadcast(unexpectedBondEvent);
        verify(mMockBtLogger).logUnexpectedPairingEvent(mMockBtPhone);

        reset(mMockBtLogger);
        Intent validBondEvent = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        validBondEvent.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        validBondEvent.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                BluetoothDevice.BOND_NONE);
        validBondEvent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING);
        mContext.sendBroadcast(validBondEvent);
        verify(mMockBtLogger, never()).logUnexpectedPairingEvent(any(BluetoothDevice.class));
    }

    @Test
    public void testBondingConnectsDeviceAndStartsShards() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);

        Intent bondEvent = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bondEvent.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        bondEvent.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                BluetoothDevice.BOND_BONDING);
        bondEvent.putExtra(BluetoothDevice.EXTRA_BOND_STATE,
                BluetoothDevice.BOND_BONDED);

        mContext.sendBroadcast(bondEvent);
        verify(mMockCompanionTracker).receivedBondedAction(mMockBtPhone);
        mMediator.onCompanionChanged();

        verify(mMockShardRunner).startProxyShard(anyInt(), any(), any(), any(), any());
    }

    @Test
    public void testOnCompanionChanged_notConnected() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner, mMockAlarmManager);
        when(mMockBtPhone.isConnected()).thenReturn(false);

        mMediator.onCompanionChanged();

        verify(mMockShardRunner, never()).startProxyShard(anyInt(), any(), any(), any(), any());
    }

    private void verifyPowerChange(int what, WearBluetoothMediator.Reason reason) {
        verify(mMockHandler, atLeastOnce()).sendMessage(mMsgCaptor.capture());
        Assert.assertEquals(what, mMsgCaptor.getValue().what);
        Assert.assertEquals(reason, mMsgCaptor.getValue().obj);
    }

    @Test
    public void testBcastAcl_AclAlreadyConnected() {
        mMediator.onBootCompleted();
        mMediator.onProxyConnectionChange(true, PROXY_SCORE_CLASSIC, false);

        Intent aclConnected = new Intent(BluetoothDevice.ACTION_ACL_CONNECTED);
        aclConnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        when(mMockBtPhone.isConnected()).thenReturn(true);
        mContext.sendBroadcast(aclConnected);

        verify(mMockShardRunner, never()).startProxyShard(
                anyInt(), any(), any(), anyString(), any());
    }

    @Test
    public void testBcastAcl_ConnectedButNoCompanion() {
        mMediator.onBootCompleted();

        when(mMockCompanionTracker.getCompanion()).thenReturn(null);

        Intent aclConnected = new Intent(BluetoothDevice.ACTION_ACL_CONNECTED);
        aclConnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        when(mMockBtPhone.isConnected()).thenReturn(true);
        mContext.sendBroadcast(aclConnected);

        verify(mMockShardRunner, never()).startProxyShard(
                anyInt(), any(), any(), anyString(), any());
    }

    @Test
    public void testBcastAcl_AclDisconnect() {
        mMediator.onBootCompleted();
        mMediator.onProxyConnectionChange(true, PROXY_SCORE_CLASSIC, false);
        reset(mMockShardRunner);

        Intent aclDisconnected = new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        aclDisconnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        when(mMockBtPhone.isConnected()).thenReturn(false);
        mContext.sendBroadcast(aclDisconnected);

        verify(mMockShardRunner).stopProxyShard();
    }

    @Test
    public void testBcastAcl_AclDisconnectButDeviceActuallyConnected() {
        mMediator.onBootCompleted();
        mMediator.onProxyConnectionChange(true, PROXY_SCORE_CLASSIC, false);
        reset(mMockShardRunner);

        Intent aclDisconnected = new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        aclDisconnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        when(mMockBtPhone.isConnected()).thenReturn(true);
        mContext.sendBroadcast(aclDisconnected);

        verify(mMockShardRunner, never()).stopProxyShard();
    }

    @Test
    public void testBcastBluetoothStateChange_StateOn() {
        mMediator.onBootCompleted();

        sendBluetoothAdapterOnIntents();

        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
    }

    @Test
    public void testBcastBluetoothStateChange_StateOff() {
        mMediator.onBootCompleted();

        sendBluetoothAdapterOffIntents();

        verify(mMockShardRunner, times(1)).stopHfcShard();
    }

    @Test
    public void testBcastBluetoothStateChange_StateOffToTurningOn() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner);

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_ON);
        mContext.sendBroadcast(intent);

        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
        verify(mMockShardRunner, never()).stopHfcShard();
    }

    @Test
    public void testBcastBluetoothStateChange_StateTurningOnToOn() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner);

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_TURNING_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        mContext.sendBroadcast(intent);

        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
        verify(mMockShardRunner, never()).stopHfcShard();
    }

    @Test
    public void testBcastBluetoothStateChange_StateOnToTurningOff() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner);

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        mContext.sendBroadcast(intent);

        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
        verify(mMockShardRunner, times(1)).stopHfcShard();
    }

    @Test
    public void testBcastBluetoothStateChange_StateTurningOffToOff() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner);

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        mContext.sendBroadcast(intent);

        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
        verify(mMockShardRunner, never()).stopHfcShard();
    }

    @Test
    public void testBcastBluetoothStateChange_StateUnknown() {
        mMediator.onBootCompleted();

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, -1);
        mContext.sendBroadcast(intent);

        verify(mMockShardRunner, never()).startHfcShard(mMockBtPhone);
        verify(mMockShardRunner, never()).stopHfcShard();
    }


    @Test
    public void testChargingStateChanged_IsCharging() {
        mMediator.onBootCompleted();

        when(mMockPowerTracker.isCharging()).thenReturn(true);

        mMediator.onChargingStateChanged();

        verify(mMockShardRunner).updateProxyShard(anyInt());
    }

    @Test
    public void testChargingStateChanged_onBattery() {
        mMediator.onBootCompleted();
        when(mMockPowerTracker.isCharging()).thenReturn(false);

        mMediator.onChargingStateChanged();

        verify(mMockShardRunner).updateProxyShard(anyInt());
    }

    @Test
    public void testRadioPowerHandler_EnableBluetooth() {
        Message msg = Message.obtain(mMediator.mRadioPowerHandler,
                WearBluetoothMediator.MSG_ENABLE_BT,
                WearBluetoothMediator.Reason.OFF_TIME_ONLY_MODE);
        mMediator.mRadioPowerHandler.sendMessage(msg);

        assertTrue(mMediator.mRadioPowerHandler.hasMessages(WearBluetoothMediator.MSG_ENABLE_BT));

        ShadowLooper shadowLooper = shadowOf(mMediator.mRadioPowerThread.getLooper());
        shadowLooper.runToEndOfTasks();

        verify(mMockBtAdapter).enable();
    }

    @Test
    public void testRadioPowerHandler_DisableBluetooth() {
        Message msg = Message.obtain(mMediator.mRadioPowerHandler,
                WearBluetoothMediator.MSG_DISABLE_BT,
                WearBluetoothMediator.Reason.OFF_TIME_ONLY_MODE);
        mMediator.mRadioPowerHandler.sendMessage(msg);

        ShadowLooper shadowLooper = shadowOf(mMediator.mRadioPowerThread.getLooper());
        shadowLooper.runToEndOfTasks();

        verify(mMockBtAdapter).disable();
    }

    @Test
    public void testOnBootCompleted_AclAlreadyConnected() {
        mMediator.onCompanionChanged();

        mMediator.onBootCompleted();

        verify(mMockBtAdapter, never()).isEnabled();
    }

    @Test
    public void testOnBootCompleted_AdapterDisabledAirplaneModeOn() {
        when(mMockWearBluetoothMediatorSettings.getIsInAirplaneMode()).thenReturn(true);
        when(mMockBtAdapter.isEnabled()).thenReturn(false);

        mMediator.onBootCompleted();

        verify(mMockBtAdapter, never()).enable();
    }

    @Test
    public void testOnBootCompleted_AdapterDisabledAirplaneModeOff() {
        when(mMockWearBluetoothMediatorSettings.getIsInAirplaneMode()).thenReturn(false);
        when(mMockBtAdapter.isEnabled()).thenReturn(false);

        mMediator.onBootCompleted();

        verify(mMockWearBluetoothMediatorSettings).setSettingsPreferenceBluetoothOn(anyBoolean());
        verify(mMockBtAdapter, never()).enable();
    }

    @Test
    public void testOnBootCompleted_SysProxyAclAlreadyConnected() {
        mMediator.onProxyConnectionChange(true, 0, false);

        mMediator.onBootCompleted();

        verify(mMockBtAdapter, never()).isEnabled();
    }

    @Test
    public void testOnBootCompleted_NoShardsIfDeviceDisabled() {
        reset(mMockDeviceEnableSetting);
        when(mMockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);

        mMediator.onBootCompleted();

        verifyNoMoreInteractions(mMockShardRunner);
    }

    @Test
    public void testDeviceEnabled_DoesNotAffectShards() {
        mMediator.onBootCompleted();
        reset(mMockShardRunner);

        when(mMockDeviceEnableSetting.isDeviceEnabled()).thenReturn(true);
        mMediator.onDeviceEnableChanged();

        verifyNoMoreInteractions(mMockShardRunner);
    }

    @Test
    public void testDeviceDisabled_ShutsDownRunningShards() {
        mMediator.onBootCompleted();

        when(mMockDeviceEnableSetting.isDeviceEnabled()).thenReturn(false);
        mMediator.onDeviceEnableChanged();

        verify(mMockShardRunner).stopHfcShard();
        verify(mMockShardRunner).stopProxyShard();
    }

    @Test
    public void testBtDecision() {
        WearBluetoothMediator.BtDecision btDecision = mMediator.new BtDecision(
                WearBluetoothMediator.Reason.OFF_TIME_ONLY_MODE);
        assertEquals(WearBluetoothMediator.Reason.OFF_TIME_ONLY_MODE.name(), btDecision.getName());

        WearBluetoothMediator.BtDecision btDecision1 =
                mMediator.new BtDecision(WearBluetoothMediator.Reason.OFF_TIME_ONLY_MODE);
        WearBluetoothMediator.BtDecision btDecision2 =
                mMediator.new BtDecision(WearBluetoothMediator.Reason.ON_AUTO);

        assertTrue(btDecision.isDuplicateOf(btDecision1));
        assertFalse(btDecision.isDuplicateOf(btDecision2));
    }

    @Test
    public void testProxyConnectionEvent_NoInternet() {
        WearBluetoothMediator.ProxyConnectionEvent event = mMediator.new ProxyConnectionEvent(true,
                false, 111);
        assertEquals("CON", event.getName().substring(0, 3));
        assertEquals(111, event.score);

        WearBluetoothMediator.ProxyConnectionEvent disconnectEvent =
                mMediator.new ProxyConnectionEvent(false, false, 111);
        assertEquals("DIS", disconnectEvent.getName().substring(0, 3));
        assertEquals(111, disconnectEvent.score);

        WearBluetoothMediator.ProxyConnectionEvent event1 = mMediator.new ProxyConnectionEvent(true,
                false, 111);
        WearBluetoothMediator.ProxyConnectionEvent event2 = mMediator.new ProxyConnectionEvent(true,
                false, 222);
        WearBluetoothMediator.ProxyConnectionEvent event3 = mMediator.new ProxyConnectionEvent(
                false, false, 111);

        assertTrue(event.isDuplicateOf(event1));
        assertTrue(event.isDuplicateOf(event2));
        assertFalse(event.isDuplicateOf(event3));
        assertFalse(event.isDuplicateOf(new BogusEvent()));
    }

    @Test
    public void testProxyConnectionEvent_WithInternet() {
        WearBluetoothMediator.ProxyConnectionEvent event = mMediator.new ProxyConnectionEvent(true,
                true, 111);
        assertEquals("CON", event.getName().substring(0, 3));
        assertEquals(111, event.score);

        WearBluetoothMediator.ProxyConnectionEvent disconnectEvent =
                mMediator.new ProxyConnectionEvent(false, true, 111);
        assertEquals("DIS", disconnectEvent.getName().substring(0, 3));
        assertEquals(111, disconnectEvent.score);

        WearBluetoothMediator.ProxyConnectionEvent event1 = mMediator.new ProxyConnectionEvent(true,
                true, 111);
        WearBluetoothMediator.ProxyConnectionEvent event2 = mMediator.new ProxyConnectionEvent(true,
                true, 222);
        WearBluetoothMediator.ProxyConnectionEvent event3 = mMediator.new ProxyConnectionEvent(
                false, true, 111);

        assertTrue(event.isDuplicateOf(event1));
        assertFalse(event.isDuplicateOf(event2));
        assertFalse(event.isDuplicateOf(event3));
        assertFalse(event.isDuplicateOf(new BogusEvent()));
    }

    @Test
    public void testUpdateProxyWhenDnsServersChange() {
        mMediator.onDnsServersChanged();
        verify(mMockShardRunner).updateProxyShard(mFakeDnsServers);
    }

    @Test
    public void testUpdateProxyWhenNoDnsServersAvailable() {
        List<InetAddress> dnsServers = Lists.newArrayList();
        when(mMockWearBluetoothMediatorSettings.getDnsServers()).thenReturn(dnsServers);
        mMediator.onDnsServersChanged();
        verify(mMockShardRunner).updateProxyShard(dnsServers);
    }

    @Test
    public void testOnProxyConfigUpdateStartsProxyShard() {
        setupBleCompanion();

        mMediator.onProxyConfigUpdate(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID,
                PING_INTERVAL_SECONDS);

        verify(mMockShardRunner).startProxyShard(PROXY_SCORE_BLE, mFakeDnsServers, mMediator,
                "PSM Update Received", PROXY_CONFIG_IOS_V1);
    }

    @Test
    public void testOnProxyConfigUpdateDoesNotRestartProxyShardIfAlreadyConnectedWithSameConfig() {
        setupBleCompanion();

        mMediator.onProxyConnectionChange(true, 0, true);
        mMediator.onProxyConfigUpdate(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID,
                PING_INTERVAL_SECONDS);
        mMediator.onProxyConfigUpdate(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID,
                PING_INTERVAL_SECONDS);

        verify(mMockShardRunner, times(1)).startProxyShard(anyInt(), any(), any(), any(), any());
    }

    @Test
    public void testOnProxyConfigUpdateRestartsProxyShardIfDisconnectedWithSameConfig() {
        setupBleCompanion();

        mMediator.onProxyConnectionChange(false, 0, true);
        mMediator.onProxyConfigUpdate(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID,
                PING_INTERVAL_SECONDS);
        mMediator.onProxyConfigUpdate(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID,
                PING_INTERVAL_SECONDS);

        verify(mMockShardRunner, times(2)).startProxyShard(anyInt(), any(), any(), any(), any());
    }

    @Test
    public void testOnProxyConfigUpdateRestartsProxyShardIfAlreadyConnectedWithDifferentConfig() {
        setupBleCompanion();

        mMediator.onProxyConnectionChange(true, 0, true);
        mMediator.onProxyConfigUpdate(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID,
                PING_INTERVAL_SECONDS);
        mMediator.onProxyConfigUpdate(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID + 1,
                PING_INTERVAL_SECONDS);

        verify(mMockShardRunner, times(2)).startProxyShard(anyInt(), any(), any(), any(), any());
    }

    @Test
    public void testOnProxyConfigUpdateStartsProxyShardIfAlreadyConnectedWithDifferentPsm() {
        setupBleCompanion();

        mMediator.onProxyConnectionChange(true, 0, true);
        mMediator.onProxyConfigUpdate(193, CHANNEL_CHANGE_ID, PING_INTERVAL_SECONDS);

        verify(mMockShardRunner).startProxyShard(PROXY_SCORE_BLE, mFakeDnsServers, mMediator,
                "PSM Update Received", ProxyServiceConfig.forIosV1(193, CHANNEL_CHANGE_ID));
    }

    @Test
    public void testOnProxyConfigUpdateSetsMinPingInterval() {
        setupBleCompanion();

        mMediator.onProxyConnectionChange(false, 0, false);
        mMediator.onProxyConfigUpdate(FAKE_VALID_PSM_VALUE, CHANNEL_CHANGE_ID, 10);

        verify(mMockProxyPinger).setMinPingIntervalMs(10000);
    }

    @Test
    public void testPingsWhenBleDataIsDetected() {
        mMediator.onProxyBleData();
        verify(mMockProxyPinger).pingIfNeeded();
    }

    @Test
    public void testPingsBleCompanionWhenSysproxyDisconnects() {
        setupBleCompanion();
        mMediator.onProxyConnectionChange(false, 0, true);

        verify(mMockProxyPinger).ping();
    }

    @Test
    public void testDoesNotPingNonBleCompanionWhenSysproxyDisconnects() {
        mMediator.onProxyConnectionChange(false, 0, true);

        verify(mMockProxyPinger, never()).ping();
    }

    @Test
    public void testDoesNotPingBleCompanionWhenSysproxyConnects() {
        setupBleCompanion();
        mMediator.onProxyConnectionChange(true, 0, true);

        verify(mMockProxyPinger, never()).ping();
    }

    @Test
    public void testProxyScoreWhenCharging() {
        mMediator.onBootCompleted();
        when(mMockPowerTracker.isCharging()).thenReturn(true);
        mMediator.onCompanionChanged();

        verify(mMockShardRunner).startProxyShard(
                PROXY_SCORE_ON_CHARGER, mFakeDnsServers, mMediator, "Companion Found",
                PROXY_CONFIG_ANDROID_V1);
    }

    @Test
    public void testProxyScoreWhenNotOnCharger() {
        mMediator.onBootCompleted();
        when(mMockPowerTracker.isCharging()).thenReturn(false);
        mMediator.onCompanionChanged();

        verify(mMockShardRunner).startProxyShard(
                PROXY_SCORE_CLASSIC, mFakeDnsServers, mMediator, "Companion Found",
                PROXY_CONFIG_ANDROID_V1);
    }

    @Test
    public void testDump() {
        // Companion connected or not
        mMediator.dump(mMockIndentingPrintWriter);

        when(mMockCompanionTracker.getCompanion()).thenReturn(null);
        mMediator.dump(mMockIndentingPrintWriter);
        when(mMockCompanionTracker.getCompanion()).thenReturn(mMockBtPhone);

        // Ble or Classic
        when(mMockCompanionTracker.isCompanionBle()).thenReturn(true);
        mMediator.dump(mMockIndentingPrintWriter);

        when(mMockCompanionTracker.isCompanionBle()).thenReturn(false);
        mMediator.dump(mMockIndentingPrintWriter);

        // Acl connected or not
        mMediator.onCompanionChanged();
        mMediator.dump(mMockIndentingPrintWriter);

        Intent aclDisconnected = new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        aclDisconnected.putExtra(BluetoothDevice.EXTRA_DEVICE, mMockBtPhone);
        when(mMockBtPhone.isConnected()).thenReturn(false);
        mContext.sendBroadcast(aclDisconnected);
        mMediator.dump(mMockIndentingPrintWriter);

        // Proxy connected or not
        mMediator.onProxyConnectionChange(true, 0, false);
        mMediator.dump(mMockIndentingPrintWriter);

        mMediator.onProxyConnectionChange(false, 0, false);
        when(mMockCompanionTracker.isCompanionBle()).thenReturn(false);

        mMediator.dump(mMockIndentingPrintWriter);
        verify(mMockDeviceInformationServer, atLeastOnce()).dump(eq(mMockIndentingPrintWriter));

        mMediator.dump(mMockIndentingPrintWriter);
        verify(mMockProxyGattServer, atLeastOnce()).dump(eq(mMockIndentingPrintWriter));
    }

    private void setupBleCompanion() {
        when(mMockBtPhone.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);

        when(mMockBtPeripheral.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_LE);

        when(mMockCompanionTracker.isCompanionBle()).thenReturn(true);

        when(mMockCompanionTracker.getBluetoothClassicCompanion()).thenReturn(mMockBtPeripheral);
    }

    private void sendBluetoothAdapterOnIntents() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_ON);
        mContext.sendBroadcast(intent);

        intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_TURNING_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        mContext.sendBroadcast(intent);
    }

    private void sendBluetoothAdapterOffIntents() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        mContext.sendBroadcast(intent);

        intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        mContext.sendBroadcast(intent);
    }

    private void toggleHfp(boolean enable) {
        toggleHfp(enable, /* firstToggleToOppositeState= */ true);
    }

    private void toggleHfp(boolean enable, boolean firstToggleToOppositeState) {
        toggleHfp(enable,
                /* fromUser= */ true,
                /* includePermission= */ true,
                firstToggleToOppositeState);
    }

    private void toggleHfp(boolean enable, boolean fromUser, boolean includePermission) {
        toggleHfp(enable, fromUser, includePermission, /* firstToggleToOppositeState= */ true);
    }

    private void toggleHfp(boolean enable,
            boolean fromUser,
            boolean includePermission,
            boolean firstToggleToOppositeState) {
        Intent intent = new Intent(WearBluetoothMediator.ACTION_TOGGLE_HFP);

        if (firstToggleToOppositeState) {
            intent.putExtra(WearBluetoothMediator.EXTRA_HFP_ENABLE, !enable);
            intent.putExtra(WearBluetoothMediator.EXTRA_SET_BY_USER, fromUser);
            mContext.sendBroadcast(
                    intent, includePermission ? android.Manifest.permission.BLUETOOTH_ADMIN : null);

            intent = new Intent(WearBluetoothMediator.ACTION_TOGGLE_HFP);
        }

        intent.putExtra(WearBluetoothMediator.EXTRA_HFP_ENABLE, enable);
        intent.putExtra(WearBluetoothMediator.EXTRA_SET_BY_USER, fromUser);
        mContext.sendBroadcast(
                intent, includePermission ? android.Manifest.permission.BLUETOOTH_ADMIN : null);

    }

    private boolean isHfpEnabled() {
        return (Settings.Global.getLong(mContext.getContentResolver(),
                Settings.Global.BLUETOOTH_DISABLED_PROFILES, 0)
                        & (1 << BluetoothProfile.HEADSET_CLIENT)) == 0;
    }

    private int getUserHfpClientSetting() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.Wearable.USER_HFP_CLIENT_SETTING,
                Settings.Global.Wearable.HFP_CLIENT_UNSET);
    }

    class BogusEvent extends EventHistory.Event {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public long getTimestampMs() {
            return 0;
        }

        @Override
        public boolean isDuplicateOf(EventHistory.Event event) {
            return false;
        }
    }

    public static class Resources extends android.content.res.Resources {
        boolean mEnableSysproxyV2;

        private Resources(
                boolean enableSysproxyV2,
                AssetManager assets,
                DisplayMetrics metrics,
                Configuration config) {
            super(assets, metrics, config);
            mEnableSysproxyV2 = enableSysproxyV2;
        }

        public static Resources create(Context context, boolean enableSysproxyV2) {
            android.content.res.Resources res = context.getResources();
            return new Resources(
                    enableSysproxyV2,
                    res.getAssets(),
                    res.getDisplayMetrics(),
                    res.getConfiguration());
        }

        @NonNull
        @Override
        public boolean getBoolean(int id) {
            if (id == com.android.wearable.resources.R.bool.config_enableSysproxyV2) {
                return mEnableSysproxyV2;
            }
            throw new NotFoundException();
        }
    }

    @Implements(value = BluetoothManager.class)
    public static class ShadowDiscoveryBluetoothManager extends ShadowBluetoothManager {
        private BluetoothGattServer mServer;

        @Implementation
        public BluetoothAdapter getAdapter() {
            return BluetoothAdapter.getDefaultAdapter();
        }

        @Implementation
        public BluetoothGattServer openGattServer(Context context,
                BluetoothGattServerCallback callback) {
             return mServer;
        }

        public void setGattServer(BluetoothGattServer server) {
            mServer = server;
        }
    }
}
