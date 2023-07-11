package com.android.clockwork.bluetooth;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.android.internal.util.IndentingPrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemProperties;

/** Test for {@link DeviceInformationGattServer} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSystemProperties.class)
public class DeviceInformationGattServerTest {
  private static final UUID FIRMWARE_REVISION_CHARACTERISTIC_UUID =
      UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
  private static final UUID SOFTWARE_REVISION_CHARACTERISTIC_UUID =
      UUID.fromString("00002A28-0000-1000-8000-00805F9B34FB");
  private static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID =
      UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
  private static final UUID MODEL_NUMBER_CHARACTERISTIC_UUID =
      UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");

  @Mock Context mockContext;
  @Mock BluetoothDevice mockDevice;
  @Mock BluetoothGattCharacteristic mockCharacteristic;
  @Mock BluetoothGattServer mockGattServer;
  @Mock BluetoothManager mockBluetoothManager;
  @Mock PackageManager mockPackageManager;
  @Mock PackageInfo mockPackageInfo;
  @Mock IndentingPrintWriter mockPrinter;

  private DeviceInformationGattServer mServer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(mockContext.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(mockBluetoothManager);
    when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
    mServer = new DeviceInformationGattServer(mockContext);
    when(mockBluetoothManager.openGattServer(mockContext, mServer)).thenReturn(mockGattServer);
    mServer.start();
  }

  @After
  public void tearDown() {
    ShadowSystemProperties.reset();
  }

  @Test
  public void testStartedServerRespondsToConnectionChanged() {
    mServer.onConnectionStateChange(mockDevice, 0, BluetoothProfile.STATE_CONNECTED);
    verify(mockDevice).getAddress();
  }

  @Test
  public void testReadingFirmwareCharacteristicWithInfo() {
    ShadowSystemProperties.override("ro.build.version.sdk", "13");
    ShadowSystemProperties.override("ro.cw_build.platform_mr", "1");
    when(mockCharacteristic.getUuid()).thenReturn(FIRMWARE_REVISION_CHARACTERISTIC_UUID);
    mServer.onCharacteristicReadRequest(mockDevice, 0, 0, mockCharacteristic);
    byte[] expectedBytes = "13.1".getBytes(StandardCharsets.UTF_8);
    verify(mockGattServer)
        .sendResponse(
            eq(mockDevice), eq(0), eq(BluetoothGatt.GATT_SUCCESS), eq(0), eq(expectedBytes));
  }

  @Test
  public void testReadingFirmwareCharacteristicWithoutInfo() {
    ShadowSystemProperties.override("ro.build.version.sdk", null);
    ShadowSystemProperties.override("ro.cw_build.platform_mr", null);
    when(mockCharacteristic.getUuid()).thenReturn(FIRMWARE_REVISION_CHARACTERISTIC_UUID);
    mServer.onCharacteristicReadRequest(mockDevice, 0, 0, mockCharacteristic);
    byte[] expectedBytes = "0.0".getBytes(StandardCharsets.UTF_8);
    verify(mockGattServer)
        .sendResponse(
            eq(mockDevice), eq(0), eq(BluetoothGatt.GATT_SUCCESS), eq(0), eq(expectedBytes));
  }

  @Test
  @Ignore("b/155649427")
  public void testReadingSoftwareCharacteristicWithInfo()
      throws PackageManager.NameNotFoundException {
    when(mockPackageManager.getPackageInfo(eq("com.google.android.gms"), eq(0)))
        .thenReturn(mockPackageInfo);
    when(mockPackageInfo.getLongVersionCode()).thenReturn(1234L);

    when(mockCharacteristic.getUuid()).thenReturn(SOFTWARE_REVISION_CHARACTERISTIC_UUID);
    mServer.onCharacteristicReadRequest(mockDevice, 0, 0, mockCharacteristic);
    byte[] expectedBytes = "1234".getBytes(StandardCharsets.UTF_8);
    verify(mockGattServer)
        .sendResponse(
            eq(mockDevice), eq(0), eq(BluetoothGatt.GATT_SUCCESS), eq(0), eq(expectedBytes));
  }

  @Test
  public void testReadingSoftwareCharacteristicWithoutInfo()
      throws PackageManager.NameNotFoundException {
    when(mockPackageManager.getPackageInfo(anyString(), eq(0)))
        .thenThrow(PackageManager.NameNotFoundException.class);
    when(mockCharacteristic.getUuid()).thenReturn(SOFTWARE_REVISION_CHARACTERISTIC_UUID);
    mServer.onCharacteristicReadRequest(mockDevice, 0, 0, mockCharacteristic);
    verify(mockGattServer)
        .sendResponse(eq(mockDevice), eq(0), eq(BluetoothGatt.GATT_FAILURE), eq(0), eq(null));
  }

  @Test
  public void testReadingManufacturerCharacteristicWithInfo() {
    ShadowSystemProperties.override("ro.product.vendor.brand", "TestBrand");
    when(mockCharacteristic.getUuid()).thenReturn(MANUFACTURER_NAME_CHARACTERISTIC_UUID);
    mServer.onCharacteristicReadRequest(mockDevice, 0, 0, mockCharacteristic);
    byte[] expectedBytes = "TestBrand".getBytes(StandardCharsets.UTF_8);
    verify(mockGattServer)
        .sendResponse(
            eq(mockDevice), eq(0), eq(BluetoothGatt.GATT_SUCCESS), eq(0), eq(expectedBytes));
  }

  @Test
  public void testReadingManufacturerCharacteristicWithoutInfo() {
    ShadowSystemProperties.override("ro.product.vendor.brand", null);
    when(mockCharacteristic.getUuid()).thenReturn(MANUFACTURER_NAME_CHARACTERISTIC_UUID);
    mServer.onCharacteristicReadRequest(mockDevice, 0, 0, mockCharacteristic);
    verify(mockGattServer)
        .sendResponse(eq(mockDevice), eq(0), eq(BluetoothGatt.GATT_FAILURE), eq(0), eq(null));
  }

  @Test
  public void testReadingModelCharacteristicWithInfo() {
    ShadowSystemProperties.override("ro.product.vendor.model", "TestModel");
    when(mockCharacteristic.getUuid()).thenReturn(MODEL_NUMBER_CHARACTERISTIC_UUID);
    mServer.onCharacteristicReadRequest(mockDevice, 0, 0, mockCharacteristic);
    byte[] expectedBytes = "TestModel".getBytes(StandardCharsets.UTF_8);
    verify(mockGattServer)
        .sendResponse(
            eq(mockDevice), eq(0), eq(BluetoothGatt.GATT_SUCCESS), eq(0), eq(expectedBytes));
  }

  @Test
  public void testReadingModelCharacteristicWithoutInfo() {
    ShadowSystemProperties.override("ro.product.vendor.model", null);
    when(mockCharacteristic.getUuid()).thenReturn(MODEL_NUMBER_CHARACTERISTIC_UUID);
    mServer.onCharacteristicReadRequest(mockDevice, 0, 0, mockCharacteristic);
    verify(mockGattServer)
        .sendResponse(eq(mockDevice), eq(0), eq(BluetoothGatt.GATT_FAILURE), eq(0), eq(null));
  }

  @Test
  public void testDump() {
    ShadowSystemProperties.override("ro.product.vendor.brand", "TestBrand");
    ShadowSystemProperties.override("ro.product.vendor.model", "TestModel");
    ShadowSystemProperties.override("ro.build.version.sdk", "13");
    ShadowSystemProperties.override("ro.cw_build.platform_mr", "1");
    mServer.dump(mockPrinter);
    verify(mockPrinter).printPair(eq("Server status"), eq("Started"));
    verify(mockPrinter).printPair(eq("Advertised model"), eq("TestModel"));
    verify(mockPrinter).printPair(eq("Advertised manufacturer"), eq("TestBrand"));
    verify(mockPrinter).printPair(eq("Advertised firmware version"), eq("13.1"));
  }
}
