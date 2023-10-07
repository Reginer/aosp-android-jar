package com.android.networkstack.tethering.companionproxy.bt;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.android.internal.util.IndentingPrintWriter;
import com.google.protobuf.CodedOutputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBluetoothManager;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
@Config(
    shadows = {GattDiscoveryClientTest.ShadowDiscoveryBluetoothManager.class})
public class GattDiscoveryClientTest {
    private static final int REQUEST_ID = 7;
    private static final int ZERO_OFFSET = 0;

    @Mock BluetoothDevice mMockDevice;
    @Mock BluetoothGattServer mMockGattServer;
    @Mock IndentingPrintWriter mMockPrinter;
    @Mock GattDiscoveryClient.Listener mMockListener;

    private Context mContext;
    private BluetoothGattServerCallback mServerCallback;
    private BluetoothGattCharacteristic mConfigCharacteristic;
    private BluetoothGattCharacteristic mUnknownCharacteristic;
    private GattDiscoveryClient mClient;

    @Before
    public void setUp() {
        RuntimeEnvironment.application = spy(RuntimeEnvironment.application);
        mContext = RuntimeEnvironment.application.getApplicationContext();

        MockitoAnnotations.initMocks(this);

        BluetoothManager bluetoothManager =
            (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        ShadowDiscoveryBluetoothManager shadowBluetoothManager =
            (ShadowDiscoveryBluetoothManager) Shadows.shadowOf(bluetoothManager);
        shadowBluetoothManager.setGattServer(mMockGattServer);

        UUID serviceUuid = GattDiscoveryClient.DEFAULT_SERVICE_UUID;
        UUID configUuid = GattDiscoveryClient.DEFAULT_CONFIG_UUID;

        BluetoothGattService service =
            GattDiscoveryClient.createGattService(serviceUuid, configUuid);
        mConfigCharacteristic = service.getCharacteristic(configUuid);
        Assert.assertNotNull(mConfigCharacteristic);
        mUnknownCharacteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), 0, 0);

        mClient = new GattDiscoveryClient(mContext, mMockListener, serviceUuid, configUuid);

        mClient.setPeerDevice(mMockDevice);
        when(mMockGattServer.addService(any())).thenReturn(true);
        when(mMockGattServer.getService(configUuid)).thenReturn(service);
        when(mMockGattServer.notifyCharacteristicChanged(any(), any(), anyBoolean())).thenReturn(
                true);
        mClient.start();

        mServerCallback = shadowBluetoothManager.mServerCallback;
    }

    @Test
    public void testConfigWriteRequestUpdatesListenerAndReceivesSuccessfulResponse() {
        byte[] configData = getConfigData(192, 1234);
        mServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, true, ZERO_OFFSET,
                configData);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_SUCCESS, ZERO_OFFSET,
                        null);
        verify(mMockListener).onProxyConfigUpdate(192, 1234);
    }

    @Test
    public void testConfigWriteRequestReceivesFailureResponseIfCompanionIsNotSet() {
        mClient.setPeerDevice(null);
        mServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, true, ZERO_OFFSET,
                getConfigData(192, 0));
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        verifyNoMoreInteractions(mMockListener);
    }

    @Test
    public void testConfigWriteRequestReceivesFailureResponseIfUuidIsUnknown() {
        mServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mUnknownCharacteristic, false, true, ZERO_OFFSET,
                getConfigData(192, 0));
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        verifyNoMoreInteractions(mMockListener);
    }

    @Test
    public void testConfigWriteRequestWithInvalidDataReceivesFailureResponse() {
        byte[] configData = "not a proto".getBytes(StandardCharsets.UTF_8);
        mServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, true, ZERO_OFFSET,
                configData);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        verifyNoMoreInteractions(mMockListener);
    }

    @Test
    public void testConfigWriteRequestWithNullDataReceivesFailureResponse() {
        mServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, true, ZERO_OFFSET, null);
        verify(mMockGattServer)
                .sendResponse(mMockDevice, REQUEST_ID, BluetoothGatt.GATT_FAILURE, ZERO_OFFSET,
                        null);
        verifyNoMoreInteractions(mMockListener);
    }

    @Test
    public void testConfigWriteRequestDoesNotReceiveResponseIfNotNeeded() {
        mServerCallback.onCharacteristicWriteRequest(
                mMockDevice, REQUEST_ID, mConfigCharacteristic, false, false, ZERO_OFFSET,
                getConfigData(192, 0));
        verify(mMockGattServer, never()).sendResponse(any(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    public void testStopClosesGattServer() {
        mClient.shutdown();
        verify(mMockGattServer).close();
    }

    @Test
    public void testDumpWhenServerStarted() {
        // Server already starts in setup.
        String dumpString = getDumpString(mClient);
        Assert.assertThat(dumpString, containsString("Server status=Started"));
        Assert.assertThat(dumpString, containsString("Companion set=true"));
    }

    @Test
    public void testDumpWhenServerStopped() {
        mClient.shutdown();
        mClient.setPeerDevice(null);
        String dumpString = getDumpString(mClient);
        Assert.assertThat(dumpString, containsString("Server status=Stopped"));
        Assert.assertThat(dumpString, containsString("Companion set=false"));
    }

    private byte[] getConfigData(int psmValue, int channelChangeId) {
        ProxyConfig config = new ProxyConfig();
        config.psmValue = psmValue;
        config.channelChangeId = channelChangeId;

        byte[] data = new byte[20];

        try {
            CodedOutputStream out = CodedOutputStream.newInstance(data, 0, data.length);
            config.serializeTo(out);
            return Arrays.copyOfRange(data, 0, out.getTotalBytesWritten());
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    private static String getDumpString(GattDiscoveryClient server) {
        StringWriter stringWriter = new StringWriter();
        IndentingPrintWriter printWriter = new IndentingPrintWriter(stringWriter, "    ");
        server.dump(printWriter);
        return stringWriter.toString();
    }

    @Implements(value = BluetoothManager.class)
    public static class ShadowDiscoveryBluetoothManager extends ShadowBluetoothManager {
        private BluetoothGattServer mServer;
        private BluetoothGattServerCallback mServerCallback;

        @Implementation
        public BluetoothAdapter getAdapter() {
            return BluetoothAdapter.getDefaultAdapter();
        }

        @Implementation
        public BluetoothGattServer openGattServer(Context context,
                BluetoothGattServerCallback callback) {
             mServerCallback = callback;
             return mServer;
        }

        public void setGattServer(BluetoothGattServer server) {
            mServer = server;
        }
    }
}
