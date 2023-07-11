package com.android.clockwork.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import com.google.android.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class BluetoothScanModeEnforcerTest {
    final ShadowApplication shadowApplication = ShadowApplication.getInstance();

    private Context mContext;
    private BluetoothScanModeEnforcer mEnforcer;

    @Mock BluetoothAdapter mockBtAdapter;
    @Mock CompanionTracker mockCompanionTracker;

    @Mock BluetoothDevice androidPhone;
    @Mock BluetoothDevice iOSPhone;
    @Mock BluetoothDevice btClassicPeripheral;

    @Mock BluetoothClass phoneBluetoothClass;
    @Mock BluetoothClass peripheralBluetoothClass;

    private Intent bondingIntent;
    private Intent scanModeDisableIntent;
    private Intent unbondingIntent;

    @Before
    public void setUp() {
        initMocks(this);

        mContext = RuntimeEnvironment.application;
        mEnforcer = new BluetoothScanModeEnforcer(mContext, mockBtAdapter, mockCompanionTracker);

        when(mockBtAdapter.isEnabled()).thenReturn(true);

        when(phoneBluetoothClass.getMajorDeviceClass()).thenReturn(BluetoothClass.Device.Major.PHONE);
        when(peripheralBluetoothClass.getMajorDeviceClass()).thenReturn(BluetoothClass.Device.Major.PERIPHERAL);

        when(androidPhone.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(androidPhone.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        when(androidPhone.getBluetoothClass()).thenReturn(phoneBluetoothClass);

        when(btClassicPeripheral.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(btClassicPeripheral.getAddress()).thenReturn("MM:NN:OO:PP:QQ:RR");
        when(btClassicPeripheral.getBluetoothClass()).thenReturn(peripheralBluetoothClass);

        when(iOSPhone.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_DUAL);
        when(iOSPhone.getAddress()).thenReturn("GG:HH:II:JJ:KK:LL");
        when(iOSPhone.getBluetoothClass()).thenReturn(phoneBluetoothClass);

        // Build scan mode and bonding intents.
        scanModeDisableIntent = new Intent(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        scanModeDisableIntent.putExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
        bondingIntent = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bondingIntent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED);
        unbondingIntent = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        unbondingIntent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
    }

    @Test
    public void testConstructorRegistersReceiver() {
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)));
        Assert.assertTrue(shadowApplication.hasReceiverForIntent(
                new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED)));
    }

    @Test
    public void testScanModeRemainsEnabledOnPairingCancel() throws Exception {
        when(mockCompanionTracker.getCompanion()).thenReturn(null);

        mContext.sendBroadcast(unbondingIntent);

        verify(mockBtAdapter, never()).setScanMode(anyInt());
    }

    @Test
    public void testAndroidWithHeadsetScanModeRemainsEnabled() throws Exception {
        when(mockCompanionTracker.isCompanionBle()).thenReturn(false);
        when(mockCompanionTracker.getCompanion()).thenReturn(androidPhone);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(androidPhone, btClassicPeripheral));

        mContext.sendBroadcast(scanModeDisableIntent);

        verify(mockBtAdapter).setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
    }

    // TODO this test is actually testing the exact same thing as above because
    // btAdapter.getScanMode is never called or checked; which suggests that the
    // underlying code is doing the wrong thing
    @Test
    public void testAndroidWithHeadsetScanModeRemainsEnabledOnDelayedBroadcast() throws Exception {
        when(mockCompanionTracker.isCompanionBle()).thenReturn(false);
        when(mockCompanionTracker.getCompanion()).thenReturn(androidPhone);

        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(androidPhone, btClassicPeripheral));

        // when scan mode is disabled by another component unknowingly
        // (the ACTION_SCAN_MODE_CHANGED could be delayed, the current scan mode can be
        // SCAN_MODE_CONNECTABLE, but the ACTION_SCAN_MODE_CHANGED could report SCAN_MODE_NONE).
        when(mockBtAdapter.getScanMode()).thenReturn(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        mContext.sendBroadcast(scanModeDisableIntent);

        verify(mockBtAdapter).setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
    }

    @Test
    public void testIosWithHeadsetScanModeRemainsEnabled() throws Exception {
        when(mockCompanionTracker.isCompanionBle()).thenReturn(true);
        when(mockCompanionTracker.getCompanion()).thenReturn(iOSPhone);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(iOSPhone, btClassicPeripheral));

        mContext.sendBroadcast(scanModeDisableIntent);

        verify(mockBtAdapter).setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
    }

    @Test
    public void testAndroidScanModeEnabledAfterHeadsetBond() throws Exception {
        when(mockCompanionTracker.isCompanionBle()).thenReturn(false);
        when(mockCompanionTracker.getCompanion()).thenReturn(androidPhone);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(androidPhone, btClassicPeripheral));

        mContext.sendBroadcast(bondingIntent);

        verify(mockBtAdapter).setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
    }

    @Test
    public void testIosScanModeEnabledAfterHeadsetBond() throws Exception {
        when(mockCompanionTracker.isCompanionBle()).thenReturn(true);
        when(mockCompanionTracker.getCompanion()).thenReturn(iOSPhone);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(iOSPhone, btClassicPeripheral));

        mContext.sendBroadcast(bondingIntent);

        verify(mockBtAdapter).setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
    }

    @Test
    public void testAndroidScanModeStillEnabledAfterUnbond() throws Exception {
        when(mockCompanionTracker.isCompanionBle()).thenReturn(false);
        when(mockCompanionTracker.getCompanion()).thenReturn(androidPhone);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(androidPhone));

        mContext.sendBroadcast(unbondingIntent);

        verify(mockBtAdapter, never()).setScanMode(BluetoothAdapter.SCAN_MODE_NONE);
    }

    @Test
    public void testIosScanModeStillEnabledAfterUnbond() throws Exception {
        when(mockCompanionTracker.isCompanionBle()).thenReturn(true);
        when(mockCompanionTracker.getCompanion()).thenReturn(iOSPhone);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(iOSPhone));

        mContext.sendBroadcast(unbondingIntent);

        verify(mockBtAdapter, never()).setScanMode(BluetoothAdapter.SCAN_MODE_NONE);
    }
}
