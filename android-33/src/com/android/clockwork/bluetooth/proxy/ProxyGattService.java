package com.android.clockwork.bluetooth.proxy;

import android.annotation.Nullable;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * The GATT service definition for managing sysproxy configuration from iOS companion app.
 *
 * <p>The actual proxy traffic is sent over an L2CAP channel which is separate from this service.
 *
 * <p>Contains two characteristics:
 *
 * <ol>
 *   <li>config: When iOS companion is ready to receive L2CAP connection from sysproxy it writes the
 *       connection configuration to this characteristic and the watch initiates a socket
 *       connection. The configuration includes the PSM value of the L2CAP socket.
 *       <p>The new config is ignored if the sysproxy is already connected and the PSM value is
 *       unchanged. Therefore, the companion should close the previously connected socket before
 *       sending the config for the new connection.
 *       <p>The written value must be a valid ProxyConfig proto message in binary encoding.
 *   <li>ping: An empty notification is sent on this characteristic before watch writes data to or
 *       after the watch receives data on the sysproxy L2CAP socket. The iOS companion can ignore
 *       the notification but it needs to subscribe using the descriptor to ensure iOS wakes it up
 *       when a ping is sent.
 *       <p>This behavior is needed since L2CAP socket itself does not wake the companion app up.
 *       See go/sysproxy-ios-state-restoration.
 * </ol>
 *
 * This service does not require encrypted connection.
 *
 * <p>sysproxy connection flow on iOS:
 *
 * <ol>
 *   <li>Companion starts serving an L2CAP socket for sysproxy if Bluetooth is on/allowed and there
 *       is paired watch (the watch may not be connected).
 *   <li>Upon watch connection, companion discovers the proxy config service and sends a write
 *       request with the proxy connection configuration.
 *   <li>Watch connects to the L2CAP socket with the PSM value in the received configuration.
 *   <li>Companion and watch follows the sysproxy protocol while communication with this socket. See
 *       http://google3/googlemac/iPhone/Wear/WearLibrary/Proxy/sysproxy.proto
 *   <li>Watch occasionally sends a notification on the ping characteristic to keep the iOS
 *       companion running as long as there is traffic.
 * </ol>
 *
 * The connection is considered lost only when the L2CAP socket is closed. If this GATT service
 * becomes unavailable the already connected socket can be still used for communication. When the
 * service becomes available again, the iOS companion does not need to send the connection config
 * again if the socket already has the watch connected.
 *
 * <p>Note: The service is designed to work with only one companion device and should reject all
 * requests from other devices.
 *
 * <p>This flow is only used when the watch has the peripheral role. See go/wear-dd-ios-roles
 */
public final class ProxyGattService {
  private static final String TAG = "ProxyGattService";

  /** Proxy config service and characterstics UUIDs. */
  static final UUID SERVICE_UUID = UUID.fromString("3675C0FF-21A6-4F96-9984-09D4F10C64F9");

  static final UUID CONFIG_UUID = UUID.fromString("C8625A0B-28C8-4460-874D-0CF9A7538B46");
  static final UUID PING_UUID = UUID.fromString("BEDA5ED3-F6EA-4EBA-81B6-71314980E3A8");
  /** Standard CCCD UUID to support subscribing to notifications. */
  static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID =
      UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private ProxyGattService() {}

  /**
   * Returns a new GATT service for proxy management. May return null if adding any required
   * characteristics or descriptors fail.
   */
  @Nullable
  public static BluetoothGattService createGattService() {
    BluetoothGattService service =
        new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    if (!service.addCharacteristic(
        new BluetoothGattCharacteristic(
            CONFIG_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE))) {
      // Adding characteristics or descriptors never fails in current implementation but may change
      // in future Android versions.
      Log.e(TAG, "Failed to add connection config characteristic");
      return null;
    }

    BluetoothGattCharacteristic pingCharacteristic =
        new BluetoothGattCharacteristic(
            PING_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ);
    if (!pingCharacteristic.addDescriptor(
        new BluetoothGattDescriptor(
            CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE))) {
      Log.e(TAG, "Failed to add ping CCCD");
      return null;
    }
    if (!service.addCharacteristic(pingCharacteristic)) {
      Log.e(TAG, "Failed to add ping characteristic");
      return null;
    }

    return service;
  }
}
