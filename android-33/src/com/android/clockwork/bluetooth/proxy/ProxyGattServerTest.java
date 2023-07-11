package com.android.clockwork.bluetooth.proxy;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.android.internal.util.IndentingPrintWriter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Test for {@link ProxyGattServer}
 */
@RunWith(RobolectricTestRunner.class)
public class ProxyGattServerTest {

    private static final int REQUEST_ID = 7;
    private static final int ZERO_OFFSET = 0;

    @Mock
    Context mMockContext;
    @Mock
    BluetoothDevice mMockDevice;
    @Mock
    BluetoothGattServer mMockGattServer;
    @Mock
    BluetoothManager mMockBluetoothManager;
    @Mock
    PackageManager mMockPackageManager;
    @Mock
    PackageInfo mMockPackageInfo;
    @Mock
    IndentingPrintWriter mMockPrinter;
    @Mock
    ProxyGattServer.Listener mMockListener;

    private BluetoothGattCharacteristic mConfigCharacteristic;
    private BluetoothGattCharacteristic mPingCharacteristic;
    private BluetoothGattCharacteristic mUnknownCharacteristic;
    private BluetoothGattDescriptor mPingCccdDescriptor;
    private BluetoothGattDescriptor mUnknownDescriptor;
    private BluetoothGattDescriptor mUnknownCccdDescriptor;
    private ProxyGattServer mServer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mMockContext.getSystemService(BluetoothManager.class)).thenReturn(
                mMockBluetoothManager);
        when(mMockContext.getPackageManager()).thenReturn(mMockPackageManager);

        BluetoothGattService service = ProxyGattService.createGattService();
        mConfigCharacteristic = service.getCharacteristic(ProxyGattService.CONFIG_UUID);
        Assert.assertNotNull(mConfigCharacteristic);
        mPingCharacteristic = service.getCharacteristic(ProxyGattService.PING_UUID);
        Assert.assertNotNull(mPingCharacteristic);
        mPingCccdDescriptor =
                mPingCharacteristic.getDescriptor(
                        ProxyGattService.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID);
        Assert.assertNotNull(mPingCccdDescriptor);
        mUnknownCharacteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), 0, 0);
        mUnknownDescriptor = new BluetoothGattDescriptor(UUID.randomUUID(), 0);
        mUnknownCccdDescriptor =
                new BluetoothGattDescriptor(
                        ProxyGattService.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID, 0);
        mUnknownCharacteristic.addDescriptor(mUnknownDescriptor);
        mUnknownCharacteristic.addDescriptor(mUnknownCccdDescriptor);

        mServer = new ProxyGattServer(mMockContext);
        mServer.setListener(mMockListener);
        mServer.setCompanionDevice(mMockDevice);
        when(mMockBluetoothManager.openGattServer(mMockContext, mServer.mGattServerCallback))
                .thenReturn(mMockGattServer);
        when(mMockGattServer.addService(any())).thenReturn(true);
        when(mMockGattServer.getService(ProxyGattService.SERVICE_UUID)).thenReturn(service);
        when(mMockGattServer.notifyCharacteristicChanged(any(), any(), anyBoolean())).thenReturn(
                true);
        mServer.start();
    }

    @Test
    public void testConfigWriteRequestUpdatesListenerAndReceivesSuccessfulResponse() {
        byte[] configData =
                ProxyConfig.newBuilder()
                        .setPsmValue(192)
                        .setChannelChangeId(1234)
                        .setMinPingIntervalSeconds(10)
                        .build()
                        .toByteArray();
        mServer.mGattServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, true, ZERO_OFFSET,
                configData);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_SUCCESS, ZERO_OFFSET,
                        null);
        verify(mMockListener).onProxyConfigUpdate(192, 1234, 10);
    }

    @Test
    public void testConfigWriteRequestReceivesFailureResponseIfCompanionIsNotSet() {
        mServer.setCompanionDevice(null);
        mServer.mGattServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, true, ZERO_OFFSET,
                getConfigData());
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        verifyZeroInteractions(mMockListener);
    }

    @Test
    public void testConfigWriteRequestReceivesFailureResponseIfUuidIsUnknown() {
        mServer.mGattServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mUnknownCharacteristic, false, true, ZERO_OFFSET,
                getConfigData());
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        verifyZeroInteractions(mMockListener);
    }

    @Test
    public void testConfigWriteRequestWithInvalidDataReceivesFailureResponse() {
        byte[] configData = "not a proto".getBytes(StandardCharsets.UTF_8);
        mServer.mGattServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, true, ZERO_OFFSET,
                configData);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        verifyZeroInteractions(mMockListener);
    }

    @Test
    public void testConfigWriteRequestWithNullDataReceivesFailureResponse() {
        mServer.mGattServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, true, ZERO_OFFSET, null);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        verifyZeroInteractions(mMockListener);
    }

    @Test
    public void testConfigWriteRequestDoesNotReceiveResponseIfNotNeeded() {
        mServer.mGattServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, false, ZERO_OFFSET,
                getConfigData());
        verify(mMockGattServer, never()).sendResponse(any(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    public void testDescriptorWriteRequestUpdatesNotificationStateAndReceivesSuccessfulResponse() {
        mServer.mGattServerCallback.onDescriptorWriteRequest(
                mMockDevice,
                REQUEST_ID,
                mPingCccdDescriptor,
                false,
                true,
                ZERO_OFFSET,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        Assert.assertTrue(mServer.mPingNotificationsEnabled);

        mServer.mGattServerCallback.onDescriptorWriteRequest(
                mMockDevice,
                REQUEST_ID,
                mPingCccdDescriptor,
                false,
                true,
                ZERO_OFFSET,
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        Assert.assertFalse(mServer.mPingNotificationsEnabled);

        verify(mMockGattServer, times(2))
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_SUCCESS, ZERO_OFFSET,
                        null);
    }

    @Test
    public void testDescriptorWriteRequestReceivesFailureIfCompanionIsNotSet() {
        mServer.setCompanionDevice(null);
        mServer.mGattServerCallback.onDescriptorWriteRequest(
                mMockDevice,
                REQUEST_ID,
                mPingCccdDescriptor,
                false,
                true,
                ZERO_OFFSET,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        Assert.assertFalse(mServer.mPingNotificationsEnabled);
    }

    @Test
    public void testDescriptorWriteRequestWithUnknownDescriptorUuidReceivesFailure() {
        mServer.mGattServerCallback.onDescriptorWriteRequest(
                mMockDevice,
                REQUEST_ID,
                mUnknownDescriptor,
                false,
                true,
                ZERO_OFFSET,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        Assert.assertFalse(mServer.mPingNotificationsEnabled);
    }

    @Test
    public void testDescriptorWriteRequestWithUnknownCharacteristicUuidReceivesFailure() {
        mServer.mGattServerCallback.onDescriptorWriteRequest(
                mMockDevice,
                REQUEST_ID,
                mUnknownCccdDescriptor,
                false,
                true,
                ZERO_OFFSET,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        Assert.assertFalse(mServer.mPingNotificationsEnabled);
    }

    @Test
    public void testDescriptorWriteRequestWithNullDataReceivesFailure() {
        mServer.mGattServerCallback.onDescriptorWriteRequest(
                mMockDevice, REQUEST_ID, mPingCccdDescriptor, false, true, ZERO_OFFSET, null);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        Assert.assertFalse(mServer.mPingNotificationsEnabled);
    }

    @Test
    public void testDescriptorWriteRequestWithInvalidDataReceivesFailure() {
        byte[] value = "invalid value".getBytes(StandardCharsets.UTF_8);
        mServer.mGattServerCallback.onDescriptorWriteRequest(
                mMockDevice, REQUEST_ID, mPingCccdDescriptor, false, true, ZERO_OFFSET, value);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        Assert.assertFalse(mServer.mPingNotificationsEnabled);
    }

    @Test
    public void testDescriptorWriteRequestDoesNotSendResponseIfNotNeeded() {
        mServer.mGattServerCallback.onDescriptorWriteRequest(
                mMockDevice,
                REQUEST_ID,
                mPingCccdDescriptor,
                false,
                false,
                ZERO_OFFSET,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        Assert.assertTrue(mServer.mPingNotificationsEnabled);
        verify(mMockGattServer, never()).sendResponse(any(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    public void testDescriptorReadRequestReceivesEnabledResponseWhenNotificationsEnabled() {
        mServer.mPingNotificationsEnabled = true;
        mServer.mGattServerCallback.onDescriptorReadRequest(
                mMockDevice, REQUEST_ID, ZERO_OFFSET, mPingCccdDescriptor);
        verify(mMockGattServer)
                .sendResponse(
                        mMockDevice,
                        REQUEST_ID,
                        BluetoothGatt.GATT_SUCCESS,
                        ZERO_OFFSET,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }

    @Test
    public void testDescriptorReadRequestReceivesDisabledResponseWhenNotificationsDisabled() {
        mServer.mPingNotificationsEnabled = false;
        mServer.mGattServerCallback.onDescriptorReadRequest(
                mMockDevice, REQUEST_ID, ZERO_OFFSET, mPingCccdDescriptor);
        verify(mMockGattServer)
                .sendResponse(
                        mMockDevice,
                        REQUEST_ID,
                        BluetoothGatt.GATT_SUCCESS,
                        ZERO_OFFSET,
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    }

    @Test
    public void testDescriptorReadRequestReceivesFailureResponseIfCompanionIsNotSet() {
        mServer.setCompanionDevice(null);
        mServer.mGattServerCallback.onDescriptorReadRequest(
                mMockDevice, REQUEST_ID, ZERO_OFFSET, mPingCccdDescriptor);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
    }

    @Test
    public void testDescriptorReadRequestWithUnknownDescriptorUuidReceivesFailureResponse() {
        mServer.setCompanionDevice(null);
        mServer.mGattServerCallback.onDescriptorReadRequest(
                mMockDevice, REQUEST_ID, ZERO_OFFSET, mUnknownDescriptor);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
    }

    @Test
    public void testDescriptorReadRequestWithUnknownCharacteristicUuidReceivesFailureResponse() {
        mServer.setCompanionDevice(null);
        mServer.mGattServerCallback.onDescriptorReadRequest(
                mMockDevice, REQUEST_ID, ZERO_OFFSET, mUnknownCccdDescriptor);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
    }

    @Test
    public void testSendPingNotifiesPingCharacteristic() {
        mServer.mPingNotificationsEnabled = true;
        Assert.assertTrue(mServer.sendPing());
        verify(mMockGattServer).notifyCharacteristicChanged(mMockDevice, mPingCharacteristic,
                false);
        Assert.assertArrayEquals(mPingCharacteristic.getValue(), new byte[0]);
    }

    @Test
    public void testSendPingFailsIfServerStopped() {
        mServer.mPingNotificationsEnabled = true;
        mServer.stop();
        Assert.assertFalse(mServer.sendPing());
        verify(mMockGattServer, never()).notifyCharacteristicChanged(any(), any(), anyBoolean());
    }

    @Test
    public void testSendPingFailsIfCompanionIsNotSet() {
        mServer.mPingNotificationsEnabled = true;
        mServer.setCompanionDevice(null);
        Assert.assertFalse(mServer.sendPing());
        verify(mMockGattServer, never()).notifyCharacteristicChanged(any(), any(), anyBoolean());
    }

    @Test
    public void testSendPingFailsIfServiceIsNotFound() {
        mServer.mPingNotificationsEnabled = true;
        when(mMockGattServer.getService(ProxyGattService.SERVICE_UUID)).thenReturn(null);
        Assert.assertFalse(mServer.sendPing());
        verify(mMockGattServer, never()).notifyCharacteristicChanged(any(), any(), anyBoolean());
    }

    @Test
    public void testSendPingFailsIfPingCharacteristicIsNotFound() {
        mServer.mPingNotificationsEnabled = true;
        // Can't remove the characteristics from the existing service so a new service without the
        // ping characteristic is used instead.
        BluetoothGattService emptyService =
                new BluetoothGattService(
                        ProxyGattService.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        when(mMockGattServer.getService(ProxyGattService.SERVICE_UUID)).thenReturn(emptyService);
        Assert.assertFalse(mServer.sendPing());
        verify(mMockGattServer, never()).notifyCharacteristicChanged(any(), any(), anyBoolean());
    }

    @Test
    public void testSendPingFailsIfNotificationsAreDisabled() {
        mServer.mPingNotificationsEnabled = false;
        Assert.assertFalse(mServer.sendPing());
        verify(mMockGattServer, never()).notifyCharacteristicChanged(any(), any(), anyBoolean());
    }

    @Test
    public void testSendPingFailsIfNotifyingFails() {
        mServer.mPingNotificationsEnabled = true;
        when(mMockGattServer.notifyCharacteristicChanged(any(), any(), anyBoolean())).thenReturn(
                false);
        Assert.assertFalse(mServer.sendPing());
    }

    @Test
    public void testStopClosesGattServer() {
        mServer.stop();
        verify(mMockGattServer).close();
    }

    @Test
    public void testDumpWhenServerStarted() {
        // Server already starts in setup.
        mServer.mPingNotificationsEnabled = true;
        String dumpString = getDumpString(mServer);
        Assert.assertThat(dumpString, containsString("Server status=Started"));
        Assert.assertThat(dumpString, containsString("Ping notifications enabled=true"));
        Assert.assertThat(dumpString, containsString("Companion set=true"));
    }

    @Test
    public void testDumpWhenServerStopped() {
        mServer.stop();
        mServer.setCompanionDevice(null);
        String dumpString = getDumpString(mServer);
        Assert.assertThat(dumpString, containsString("Server status=Stopped"));
        Assert.assertThat(dumpString, containsString("Ping notifications enabled=false"));
        Assert.assertThat(dumpString, containsString("Companion set=false"));
    }

    private byte[] getConfigData() {
        return ProxyConfig.newBuilder()
                .setPsmValue(192)
                .setMinPingIntervalSeconds(10)
                .build()
                .toByteArray();
    }

    private static String getDumpString(ProxyGattServer mServer) {
        StringWriter stringWriter = new StringWriter();
        IndentingPrintWriter printWriter = new IndentingPrintWriter(stringWriter, "    ");
        mServer.dump(printWriter);
        return stringWriter.toString();
    }
}
