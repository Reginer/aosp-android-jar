package com.android.clockwork.bluetooth.proxy;

import android.annotation.Nullable;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.android.clockwork.common.LogUtil;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;

/**
 * GATT server wrapper used to manage sysproxy connection on iOS.
 *
 * <p>See {@link ProxyGattService} for internal details of the communication protocol.
 *
 * <p>This class is not thread-safe. All public methods must be called from the same thread unless
 * documented otherwise.
 */
public class ProxyGattServer {
    private static final String TAG = "ProxyGattServer";

    private final Context mContext;
    @VisibleForTesting
    final BluetoothGattServerCallback mGattServerCallback;

    /**
     * The opened GATT server. null if server is closed
     */
    @Nullable
    private volatile BluetoothGattServer mGattServer;
    /**
     * The companion device this server should communicate with.
     */
    @Nullable
    private volatile BluetoothDevice mCompanionDevice;
    /**
     * The listener that is notified on Wear GATT server events
     */
    @Nullable
    private volatile Listener mListener;

    /**
     * Whether the companion is subscribed to notifications on the ping characteristic.
     */
    @VisibleForTesting
    volatile boolean mPingNotificationsEnabled;

    public ProxyGattServer(Context context) {
        mContext = context;
        mGattServerCallback = new BluetoothGattServerCallback();
    }

    /**
     * Interface for listening to server events.
     *
     * <p>The listener functions are called on arbitrary threads. However, the calls are never
     * concurrent.
     */
    public interface Listener {

        /**
         * Called when the iOS companion is ready to accept a new sysproxy connection.
         *
         * @param psm                    The psm value of the L2CAP socket the iOS companion is
         *                               listening on.
         * @param channelChangeID        Identifies the L2CAP channel change event. sysproxy should
         *                               reconnect to the channel again if this value changed since
         *                               the previous call.
         * @param minPingIntervalSeconds Minimum duration the watch needs to wait between pings.
         */
        void onProxyConfigUpdate(int psm, int channelChangeID, int minPingIntervalSeconds);
    }

    /**
     * Sets the listener that is notified on server events.
     *
     * <p>The listener must be set exactly once and this can be done on any thread. However, the
     * call must finish executing before any other public functions of the server is called.
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * Sets the companion device that is allowed to communication with this GATT server.
     *
     * <p>The server refuses to handle any read or write requests if the address of the requesting
     * device does not match the companion device. If a companion device is not set, it refuses all
     * requests.
     *
     * <p>The companion device does not change in practice. However, since the watch is unpared on
     * the initial boot, the server starts with a null device and the correct device is set later
     * when pairing is complete.
     */
    public void setCompanionDevice(BluetoothDevice companionDevice) {
        this.mCompanionDevice = companionDevice;
    }

    /**
     * Starts the proxy GATT server.
     *
     * <p>Bluetooth adapter should be on, otherwise opening the server fails. Moreover, when the
     * adapter turns off, the GATT server should be closed and reopened next time the adapter is
     * on.
     *
     * <p>If the server is already open, this method does not do anything.
     *
     * @return true iff opening the GATT server was successful or the server was already open.
     */
    public boolean start() {
        LogUtil.logDOrNotUser(TAG, "ProxyGattServer.start()");

        if (mGattServer != null) {
            LogUtil.logDOrNotUser(TAG, "Server is already open");
            return true;
        }

        BluetoothManager bluetoothManager = mContext.getSystemService(BluetoothManager.class);
        mGattServer = bluetoothManager.openGattServer(mContext, mGattServerCallback);

        if (mGattServer == null) {
            Log.e(TAG, "Failed to open GATT server");
            return false;
        }
        BluetoothGattService service = ProxyGattService.createGattService();
        if (service == null) {
            Log.e(TAG, "Failed to create GATT service");
            return false;
        }
        if (!mGattServer.addService(service)) {
            Log.e(TAG, "Failed to add the service");
            return false;
        }
        LogUtil.logDOrNotUser(TAG, "Started GATT service");
        return true;
    }

    /**
     * Stops the GATT server.
     *
     * <p>Should be called when Bluetooth adapter is off to avoid keeping a stale GATT server
     * state. Does nothing if the server was already closed.
     */
    public void stop() {
        LogUtil.logDOrNotUser(TAG, "ProxyGattServer.stop()");
        if (mGattServer == null) {
            LogUtil.logDOrNotUser(TAG, "Server was not running");
            return;
        }
        mGattServer.clearServices();
        mGattServer.close();
        mGattServer = null;
        mPingNotificationsEnabled = false;
        LogUtil.logDOrNotUser(TAG, "Stopped GATT server");
    }

    /**
     * Returns whether the given device is the companion.
     */
    private boolean isCompanionDevice(BluetoothDevice device) {
        if (mCompanionDevice == null) {
            // Companion device is not set yet. Must be before pairing phase.
            return false;
        }
        return mCompanionDevice.equals(device);
    }

    /**
     * Sends a ping to the companion app.
     *
     * <p>Sending the request may fail immediately in the following conditions:
     *
     * <ol>
     *   <li>GATT server is closed.
     *   <li>Companion device is not set.
     *   <li>Companion device didn't enable notifications for the ping characteristic.
     *   <li>There is already another pending ping request.
     *   <li>BT stack returns an error.
     * </ol>
     *
     * @return Whether the notification was successfully queued by the BT stack.
     */
    public boolean sendPing() {
        BluetoothGattServer server = mGattServer;
        if (server == null) {
            Log.e(TAG, "Failed to notify. Server is closed.");
            return false;
        }

        if (mCompanionDevice == null) {
            Log.e(TAG, "Failed to notify. Companion device is unset.");
            return false;
        }

        BluetoothGattService service = server.getService(ProxyGattService.SERVICE_UUID);
        if (service == null) {
            Log.e(TAG, "Failed to notify. Service not found.");
            return false;
        }
        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(ProxyGattService.PING_UUID);
        if (characteristic == null) {
            Log.e(TAG, "Failed to notify. Ping characteristic not found.");
            return false;
        }
        if (!mPingNotificationsEnabled) {
            Log.e(TAG, "Failed to notify. No subscribers.");
            return false;
        }
        // Note: The payload is empty as this is just a ping notification.
        if (!characteristic.setValue(new byte[0])) {
            Log.e(TAG, "Failed to notify. Can't set value.");
            return false;
        }

        Log.d(TAG, "Will notify");
        if (!server.notifyCharacteristicChanged(mCompanionDevice, characteristic, false)) {
            Log.e(TAG, "Failed to notify");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Dumps the state of the server.
     */
    public void dump(IndentingPrintWriter ipw) {
        ipw.increaseIndent();
        ipw.printPair("Server status", mGattServer == null ? "Stopped" : "Started");
        ipw.printPair("Companion set", mCompanionDevice != null);
        ipw.printPair("Ping notifications enabled", mPingNotificationsEnabled);
        ipw.decreaseIndent();
    }

    /**
     * The callback that handles the GATT server events.
     *
     * <p>Note: Fluoride invokes the callback functions on arbitrary binder threads. However, it
     * never makes concurrent calls.
     */
    class BluetoothGattServerCallback extends android.bluetooth.BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            LogUtil.logDOrNotUser(TAG, "onConnectionStateChange() - status: "
                    + status
                    + " newState: "
                    + newState
                    + " isCompanion: "
                    + isCompanionDevice(device));
        }

        @Override
        public void onCharacteristicWriteRequest(
                BluetoothDevice device,
                int requestId,
                BluetoothGattCharacteristic characteristic,
                boolean preparedWrite,
                boolean responseNeeded,
                int offset,
                byte[] value) {
            LogUtil.logDOrNotUser(TAG, "onCharacteristicWriteRequest()");

            if (!isCompanionDevice(device)) {
                Log.e(TAG, "Received request from unknown device");
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            if (!ProxyGattService.CONFIG_UUID.equals(characteristic.getUuid())) {
                Log.e(TAG,
                        "Received write for unknown characteristic: " + characteristic.getUuid());
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            if (value == null) {
                Log.e(TAG, "Value not set");
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            try {
                ProxyConfig proxyConfig = ProxyConfig.parseFrom(value);
                mListener.onProxyConfigUpdate(
                        proxyConfig.getPsmValue(), proxyConfig.getChannelChangeId(),
                        proxyConfig.getMinPingIntervalSeconds());
                if (responseNeeded) {
                    sendSuccessResponse(device, requestId, null);
                }
            } catch (InvalidProtocolBufferException e) {
                Log.e(TAG, "Failed to parse data", e);
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
            }
        }

        @Override
        public void onDescriptorWriteRequest(
                BluetoothDevice device,
                int requestId,
                BluetoothGattDescriptor descriptor,
                boolean preparedWrite,
                boolean responseNeeded,
                int offset,
                byte[] value) {
            Log.d(TAG, "onDescriptorWriteRequest()");

            if (!isCompanionDevice(device)) {
                Log.e(TAG, "Received request from unknown device");
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            if (!ProxyGattService.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID.equals(
                    descriptor.getUuid())) {
                Log.e(TAG, "Unknown descriptor UUID: " + descriptor.getUuid());
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            if (!ProxyGattService.PING_UUID.equals(descriptor.getCharacteristic().getUuid())) {
                Log.e(TAG,
                        "Unknown characteristic UUID: " + descriptor.getCharacteristic().getUuid());
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                mPingNotificationsEnabled = true;
            } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                mPingNotificationsEnabled = false;
            } else {
                Log.e(TAG, "Unexpected descriptor value");
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            Log.d(TAG, "UUID: "
                    + descriptor.getCharacteristic().getUuid()
                    + ", enabled: "
                    + mPingNotificationsEnabled);
            if (responseNeeded) {
                sendSuccessResponse(device, requestId, null);
            }
        }

        @Override
        public void onDescriptorReadRequest(
                BluetoothDevice device, int requestId, int offset,
                BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "onDescriptorReadRequest()");

            if (!isCompanionDevice(device)) {
                Log.e(TAG, "Received request from unknown device");
                sendFailureResponse(device, requestId);
                return;
            }

            if (!ProxyGattService.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID.equals(
                    descriptor.getUuid())) {
                Log.e(TAG, "Unknown descriptor UUID: " + descriptor.getUuid());
                sendFailureResponse(device, requestId);
                return;
            }

            if (!ProxyGattService.PING_UUID.equals(descriptor.getCharacteristic().getUuid())) {
                Log.e(TAG,
                        "Unknown characteristic UUID: " + descriptor.getCharacteristic().getUuid());
                sendFailureResponse(device, requestId);
                return;
            }

            Log.d(TAG, "UUID: "
                    + descriptor.getCharacteristic().getUuid()
                    + ", enabled: "
                    + mPingNotificationsEnabled);
            sendSuccessResponse(
                    device,
                    requestId,
                    mPingNotificationsEnabled
                            ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        private void sendSuccessResponse(BluetoothDevice device, int requestId, byte[] value) {
            BluetoothGattServer server = mGattServer;
            if (server == null) {
                Log.d(TAG, "Ignoring response. Server is closed.");
                return;
            }

            boolean success =
                    server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            if (!success) {
                Log.e(TAG, "Failed to send GATT response");
            }
        }

        private void sendFailureResponse(BluetoothDevice device, int requestId) {
            BluetoothGattServer server = mGattServer;
            if (server == null) {
                Log.d(TAG, "Ignoring response. Server is closed.");
                return;
            }

            boolean success = server.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0,
                    null);
            if (!success) {
                Log.e(TAG, "Failed to send GATT failure response");
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            LogUtil.logDOrNotUser(TAG, "onNotificationSent() - status: " + status);
        }
    }
}
