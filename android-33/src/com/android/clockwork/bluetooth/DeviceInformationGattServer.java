package com.android.clockwork.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * This class is a Bluetooth LE GATT server that serves the Device Information Service (as defined
 * by the Bluetooth Specification: https://www.bluetooth.com/specifications/gatt/). It serves:
 *
 * <ul>
 *   <li>The device brand as Manufacturer Name.
 *   <li>The device model as Model Version.
 *   <li>The GMS Package version as Software Version.
 *   <li>The SDK version and MR release as Firmware Version.
 * </ul>
 */
public class DeviceInformationGattServer extends BluetoothGattServerCallback {
  private static final String TAG = "DeviceInformationGattServer";

  private Context mContext;
  private BluetoothGattServer mGattServer;
  private boolean mServerStarted;

  private static final UUID DEVICE_INFORMATION_SERVICE_UUID =
      UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
  private static final UUID FIRMWARE_REVISION_CHARACTERISTIC_UUID =
      UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
  private static final UUID SOFTWARE_REVISION_CHARACTERISTIC_UUID =
      UUID.fromString("00002A28-0000-1000-8000-00805F9B34FB");
  private static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID =
      UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
  private static final UUID MODEL_NUMBER_CHARACTERISTIC_UUID =
      UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");

  public DeviceInformationGattServer(Context context) {
    mContext = context;
  }

  /** Starts the Device Information Gatt Server. */
  public void start() {
    Log.d(TAG, "DeviceInformationService.start()");
    BluetoothManager bluetoothManager =
        (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
    mGattServer = bluetoothManager.openGattServer(mContext, this);
    registerService();
    mServerStarted = true;
  }

  /** Stops the Device Information Gatt Server. */
  public void stop() {
    mServerStarted = false;
    Log.d(TAG, "DeviceInformationService.stop()");
    if (mGattServer != null) {
      mGattServer.close();
    }
  }

  @Override
  public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
    if (newState == BluetoothProfile.STATE_CONNECTED && device != null) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Connection from: " + device.getAddress());
      }
    }
  }

  @Override
  public void onCharacteristicReadRequest(
      BluetoothDevice device,
      int requestId,
      int offset,
      BluetoothGattCharacteristic characteristic) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "onCharacteristicReadRequest()");
    }

    String characteristicInfo = null;
    if (characteristic.getUuid().equals(FIRMWARE_REVISION_CHARACTERISTIC_UUID)) {
      characteristicInfo = getFirmwareVersion();
    } else if (characteristic.getUuid().equals(SOFTWARE_REVISION_CHARACTERISTIC_UUID)) {
      characteristicInfo = getSoftwareVersion();
    } else if (characteristic.getUuid().equals(MANUFACTURER_NAME_CHARACTERISTIC_UUID)) {
      characteristicInfo = getManufacturerName();
    } else if (characteristic.getUuid().equals(MODEL_NUMBER_CHARACTERISTIC_UUID)) {
      characteristicInfo = getModel();
    }

    if (characteristicInfo != null && !characteristicInfo.isEmpty()) {
      byte[] characteristicInfoBytes = characteristicInfo.getBytes(StandardCharsets.UTF_8);
      mGattServer.sendResponse(
          device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristicInfoBytes);
    } else {
      mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
    }
  }

  /**
   * Register the Device Information Service and its associated characteristics to the GATT server.
   */
  private void registerService() {
    // Can be null if the device doesn't support the Bluetooth system service.
    if (mGattServer == null) {
      return;
    }
    BluetoothGattService deviceInformationService =
        new BluetoothGattService(
            DEVICE_INFORMATION_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    BluetoothGattCharacteristic firmwareVersionCharacteristic =
        new BluetoothGattCharacteristic(
            FIRMWARE_REVISION_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    BluetoothGattCharacteristic softwareVersionCharacteristic =
        new BluetoothGattCharacteristic(
            SOFTWARE_REVISION_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    BluetoothGattCharacteristic manufacturerNameCharacteristic =
        new BluetoothGattCharacteristic(
            MANUFACTURER_NAME_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    BluetoothGattCharacteristic modelNumberCharacteristic =
        new BluetoothGattCharacteristic(
            MODEL_NUMBER_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    deviceInformationService.addCharacteristic(firmwareVersionCharacteristic);
    deviceInformationService.addCharacteristic(softwareVersionCharacteristic);
    deviceInformationService.addCharacteristic(manufacturerNameCharacteristic);
    deviceInformationService.addCharacteristic(modelNumberCharacteristic);
    mGattServer.addService(deviceInformationService);
  }

  /** Returns a firmware version composed of the sdk version and the MR release. */
  private String getFirmwareVersion() {
    int buildVersion = SystemProperties.getInt("ro.build.version.sdk", 0);
    int clockworkBuildVersion = SystemProperties.getInt("ro.cw_build.platform_mr", 0);
    return "" + buildVersion + "." + clockworkBuildVersion;
  }

  /** Returns a software version based on the gms package version. */
  private String getSoftwareVersion() {
    String gmsPackageName = "com.google.android.gms";
    try {
      PackageManager packageManager = mContext.getPackageManager();
      PackageInfo packageInfo = packageManager.getPackageInfo(gmsPackageName, 0);
      if (packageInfo != null) {
        return String.valueOf(packageInfo.getLongVersionCode());
      }
      return null;
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }

  /** Returns a manufacturer name based on the device brand (i.e. Fossil instead of Compal). */
  private String getManufacturerName() {
    return SystemProperties.get("ro.product.vendor.brand");
  }

  /** Returns the model name. */
  private String getModel() {
    return SystemProperties.get("ro.product.vendor.model");
  }

  /** Dump the state of the server. */
  public void dump(IndentingPrintWriter ipw) {
    ipw.increaseIndent();
    ipw.printPair("Server status", mServerStarted ? "Started" : "Stopped");
    ipw.printPair("Advertised model", getModel());
    ipw.printPair("Advertised manufacturer", getManufacturerName());
    ipw.printPair("Advertised software version", getSoftwareVersion());
    ipw.printPair("Advertised firmware version", getFirmwareVersion());
    ipw.decreaseIndent();
  }
}
