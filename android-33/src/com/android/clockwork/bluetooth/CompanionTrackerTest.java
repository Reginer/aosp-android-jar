package com.android.clockwork.bluetooth;

import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_ANDROID;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_IOS;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_UNKNOWN;

import static com.android.clockwork.bluetooth.proxy.WearProxyConstants.PROXY_UUID_V1;
import static com.android.clockwork.bluetooth.proxy.WearProxyConstants.PROXY_UUID_V2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.MatrixCursor;
import android.os.ParcelUuid;
import android.provider.Settings;

import com.google.android.collect.Sets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;

@RunWith(RobolectricTestRunner.class)
public class CompanionTrackerTest {

    @Mock BluetoothAdapter mockBtAdapter;

    @Mock BluetoothDevice androidPhone;
    @Mock BluetoothDevice iOSPhone;
    @Mock BluetoothDevice btPeripheral;

    @Mock BluetoothClass phoneBluetoothClass;
    @Mock BluetoothClass peripheralBluetoothClass;

    @Mock CompanionTracker.Listener mockListener;

    private ContentResolver mContentResolver;
    private CompanionTracker mTracker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContentResolver = spy(RuntimeEnvironment.application.getContentResolver());
        mTracker = new CompanionTracker(mContentResolver, mockBtAdapter);
        mTracker.addListener(mockListener);

        when(phoneBluetoothClass.getMajorDeviceClass())
                .thenReturn(BluetoothClass.Device.Major.PHONE);
        when(peripheralBluetoothClass.getMajorDeviceClass())
                .thenReturn(BluetoothClass.Device.Major.PERIPHERAL);

        when(androidPhone.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(androidPhone.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        when(androidPhone.getBluetoothClass()).thenReturn(phoneBluetoothClass);

        when(btPeripheral.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_CLASSIC);
        when(btPeripheral.getAddress()).thenReturn("MM:NN:OO:PP:QQ:RR");
        when(btPeripheral.getBluetoothClass()).thenReturn(peripheralBluetoothClass);

        when(iOSPhone.getType()).thenReturn(BluetoothDevice.DEVICE_TYPE_DUAL);
        when(iOSPhone.getAddress()).thenReturn("GG:HH:II:JJ:KK:LL");
        when(iOSPhone.getBluetoothClass()).thenReturn(phoneBluetoothClass);
    }

    @Test
    public void testNullBtAdapter() {
        CompanionTracker nullBtTracker = new CompanionTracker(mContentResolver, null);
        nullBtTracker.addListener(mockListener);

        Assert.assertNull(nullBtTracker.getCompanion());
        Assert.assertFalse(nullBtTracker.isCompanionBle());

        nullBtTracker.onBluetoothAdapterReady();

        Assert.assertNull(nullBtTracker.getCompanion());
        Assert.assertFalse(nullBtTracker.isCompanionBle());

        mTracker.mSettingsObserver.onChange(false, null);

        nullBtTracker.onBluetoothAdapterReady();

        verifyNoMoreInteractions(mockListener);
    }

    @Test
    public void testPreAdapterEnabledGetters() {
        Assert.assertNull(mTracker.getCompanion());
        Assert.assertFalse(mTracker.isCompanionBle());
    }

    @Test
    public void testOnAndroidCompanionAddressChanged() {
        setupSettings(androidPhone.getAddress(), PAIRED_DEVICE_OS_TYPE_ANDROID);
        when(mockBtAdapter.getBondedDevices())
                .thenReturn(Sets.newHashSet(androidPhone, btPeripheral));

        mTracker.mSettingsObserver.onChange(false, null);

        Assert.assertEquals(androidPhone, mTracker.getCompanion());
        Assert.assertFalse(mTracker.isCompanionBle());
        verify(mockListener).onCompanionChanged();
    }

    @Test
    public void testOnIosCompanionAddressChanged() {
        setupSettings(iOSPhone.getAddress(), PAIRED_DEVICE_OS_TYPE_IOS);
        when(mockBtAdapter.getBondedDevices())
                .thenReturn(Sets.newHashSet(iOSPhone, btPeripheral));

        mTracker.mSettingsObserver.onChange(false, null);

        Assert.assertEquals(iOSPhone, mTracker.getCompanion());
        Assert.assertTrue(mTracker.isCompanionBle());
        verify(mockListener).onCompanionChanged();
    }

    @Test
    public void testAdapterReady_FreshUnpairedDevice() {
        setupSettings(null, -9999);

        when(mockBtAdapter.getBondedDevices()).thenReturn(new HashSet<>());

        mTracker.onBluetoothAdapterReady();

        Assert.assertNull(mTracker.getCompanion());
        Assert.assertFalse(mTracker.isCompanionBle());
    }

    @Test
    public void testAdapterReady_PairedAndroidDeviceNoMigrationNeeded() {
        setupSettings(androidPhone.getAddress(), -9999);

        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(androidPhone));

        mTracker.onBluetoothAdapterReady();

        Assert.assertEquals(androidPhone, mTracker.getCompanion());
        Assert.assertFalse(mTracker.isCompanionBle());
    }

    @Test
    public void testAdapterReady_PairedIOsDeviceNoMigrationNeeded() {
        setupSettings(iOSPhone.getAddress(), -9999);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(iOSPhone));

        mTracker.onBluetoothAdapterReady();

        Assert.assertEquals(iOSPhone, mTracker.getCompanion());
        Assert.assertTrue(mTracker.isCompanionBle());
    }

    @Test
    public void testAdapterReady_MigrationNeededPairedAndroidDevice() {
        setupSettings(null, PAIRED_DEVICE_OS_TYPE_ANDROID);

        when(mockBtAdapter.getBondedDevices())
                .thenReturn(Sets.newHashSet(androidPhone, btPeripheral));

        mTracker.onBluetoothAdapterReady();

        assertCompanionAddressUpdated(androidPhone.getAddress());
        Assert.assertEquals(androidPhone, mTracker.getCompanion());
        Assert.assertFalse(mTracker.isCompanionBle());
    }

    @Test
    public void testAdapterReady_MigrationNeededPairedIOsDevice() {
        setupSettings(null, PAIRED_DEVICE_OS_TYPE_IOS);

        when(mockBtAdapter.getBondedDevices())
                .thenReturn(Sets.newHashSet(iOSPhone, btPeripheral));

        mTracker.onBluetoothAdapterReady();

        assertCompanionAddressUpdated(iOSPhone.getAddress());
        Assert.assertEquals(iOSPhone, mTracker.getCompanion());
        Assert.assertTrue(mTracker.isCompanionBle());
    }

    @Test
    public void testAdapterReady_MigrationNeededPairedUnknownButHasPairedAndroidDevice() {
        setupSettings(null, PAIRED_DEVICE_OS_TYPE_UNKNOWN);

        when(mockBtAdapter.getBondedDevices())
                .thenReturn(Sets.newHashSet(androidPhone, btPeripheral));

        mTracker.onBluetoothAdapterReady();

        assertCompanionAddressUpdated(androidPhone.getAddress());
        Assert.assertEquals(androidPhone, mTracker.getCompanion());
        Assert.assertFalse(mTracker.isCompanionBle());
    }

    @Test
    public void testAdapterReady_MigrationNeededPairedUnknownButHasPairedIOsDevice() {
        setupSettings(null, PAIRED_DEVICE_OS_TYPE_UNKNOWN);

        when(mockBtAdapter.getBondedDevices())
                .thenReturn(Sets.newHashSet(iOSPhone, btPeripheral));

        mTracker.onBluetoothAdapterReady();

        assertCompanionAddressUpdated(iOSPhone.getAddress());
        Assert.assertEquals(iOSPhone, mTracker.getCompanion());
        Assert.assertTrue(mTracker.isCompanionBle());
    }

    @Test
    public void testPairedDeviceTypeBeatsAdapterType() {
        setupSettings(iOSPhone.getAddress(), PAIRED_DEVICE_OS_TYPE_ANDROID);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(iOSPhone));

        mTracker.onBluetoothAdapterReady();

        // even though the BluetoothDevice is BLE, the tracker should return false for
        // isCompanionBle because of the PAIRED_DEVICE_OS_TYPE field
        Assert.assertEquals(iOSPhone, mTracker.getCompanion());
        Assert.assertFalse(mTracker.isCompanionBle());
    }

    @Test
    public void testPairedDeviceUnknownFallback() {
        setupSettings(iOSPhone.getAddress(), PAIRED_DEVICE_OS_TYPE_UNKNOWN);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(iOSPhone));

        mTracker.onBluetoothAdapterReady();

        // since the paired device OS type is unknown, it should return the type given by the device
        Assert.assertEquals(iOSPhone, mTracker.getCompanion());
        Assert.assertTrue(mTracker.isCompanionBle());
    }

    @Test
    public void testReceiveBondedAction() {
        setupSettings(androidPhone.getAddress(), PAIRED_DEVICE_OS_TYPE_ANDROID);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(androidPhone));


        mTracker.receivedBondedAction(androidPhone);

        Assert.assertEquals(androidPhone, mTracker.getCompanion());
        Assert.assertFalse(mTracker.isCompanionBle());
        verify(mockListener).onCompanionChanged();
    }

    @Test
    public void testReceiveBondedAction_OsAddressUnknown() {
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(androidPhone));

        mTracker.receivedBondedAction(androidPhone);

        verifyNoMoreInteractions(mockListener);
    }

    @Test
    public void testCompanionChanged_osTypeUnknown() {
        setupSettings(androidPhone.getAddress(), -1);
        when(mockBtAdapter.getBondedDevices()).thenReturn(Sets.newHashSet(androidPhone));


        mTracker.mSettingsObserver.onChange(false, null);

        verifyNoMoreInteractions(mockListener);
    }

    @Test
    public void testIsUuidPresent() {
        reset(androidPhone);
        when(androidPhone.getUuids()).thenReturn(new ParcelUuid[] { PROXY_UUID_V1 });
        assertTrue(CompanionTracker.isUuidPresent(androidPhone, PROXY_UUID_V1));
        assertFalse(CompanionTracker.isUuidPresent(androidPhone, PROXY_UUID_V2));

        reset(androidPhone);
        when(androidPhone.getUuids()).thenReturn(new ParcelUuid[] { PROXY_UUID_V2 });
        assertTrue(CompanionTracker.isUuidPresent(androidPhone, PROXY_UUID_V2));
        assertFalse(CompanionTracker.isUuidPresent(androidPhone, PROXY_UUID_V1));
    }

    private void setupSettings(String companionAddress, int pairedDeviceOSType) {
        if (companionAddress != null) {
            MatrixCursor cursor =
                    new MatrixCursor(
                            new String[] {
                                WearBluetoothSettings.SETTINGS_COLUMN_KEY,
                                WearBluetoothSettings.SETTINGS_COLUMN_VALUE
                            });
            cursor.addRow(
                    new Object[] {WearBluetoothSettings.KEY_COMPANION_ADDRESS, companionAddress});
            when(mContentResolver.query(
                            WearBluetoothSettings.BLUETOOTH_URI, null, null, null, null))
                    .thenReturn(cursor);
        }
        if (pairedDeviceOSType >= 0) {
            WearBluetoothSettings.putInt(
                    mContentResolver,
                    Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE,
                    pairedDeviceOSType);
        }
    }

    private void assertCompanionAddressUpdated(String val) {
        ArgumentCaptor<ContentValues> contentValues = ArgumentCaptor.forClass(ContentValues.class);
        verify(mContentResolver, atLeastOnce())
                .update(
                        eq(WearBluetoothSettings.BLUETOOTH_URI),
                        contentValues.capture(),
                        isNull(),
                        isNull());
        Assert.assertEquals(
                val,
                contentValues.getValue().getAsString(WearBluetoothSettings.KEY_COMPANION_ADDRESS));
    }
}
