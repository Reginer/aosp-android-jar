package com.android.networkstack.tethering.companionproxy.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.networkstack.tethering.companionproxy.protocol.LogUtils;
import com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.util.UUID;

/**
 * Manages discovery and pings for the Companion Proxy L2CAP server.
 *
 * Contains "config" characteristic. When the peer is ready to receive L2CAP connection
 * from the client, it writes the connection configuration to this characteristic and
 * the client initiates a socket connection. The configuration includes the PSM value
 * of the L2CAP socket. The written value must be a valid ProxyConfig proto message.
 *
 * This service does not require encrypted connection.
 *
 * @hide
 */
public final class GattDiscoveryClient {
    private static final String TAG = LogUtils.TAG;

    static final UUID DEFAULT_SERVICE_UUID = UUID.fromString(
        "92cda598-78c1-4878-94ec-36fb0eda7e0d");
    static final UUID DEFAULT_CONFIG_UUID = UUID.fromString(
        "2164b0d4-8beb-4895-84da-fa27c7ab21f9");

    /**
     * Receives notifications on discovery data changes.
     * @hide
     */
    public interface Listener {
        /** Called when the other side is ready to accept a new L2CAP connection. */
        void onProxyConfigUpdate(int psmValue, int channelChangeId);
    }

    private final Context mContext;
    private final Listener mListener;
    private final UUID mServiceUuid;
    private final UUID mConfigUuid;
    private final BluetoothCallbackImpl mGattServerCallback;
    private BluetoothGattServer mGattServer;
    private BluetoothDevice mPeerDevice;

    public GattDiscoveryClient(Context context, Listener listener) {
        this(context, listener, DEFAULT_SERVICE_UUID, DEFAULT_CONFIG_UUID);
    }

    GattDiscoveryClient(Context context, Listener listener,
            UUID serviceUuid, UUID configUuid) {
        mContext = context;
        mListener = listener;
        mServiceUuid = serviceUuid;
        mConfigUuid = configUuid;

        mGattServerCallback = new BluetoothCallbackImpl();
    }

    /** Sets the peer device that is allowed to communication with this GATT. */
    public synchronized void setPeerDevice(BluetoothDevice peerDevice) {
        mPeerDevice = peerDevice;
    }

    private synchronized boolean isPermittedPeer(BluetoothDevice device) {
        return mPeerDevice != null && mPeerDevice.equals(device);
    }

    /**
     * Starts the proxy GATT server.
     *
     * Bluetooth adapter should be on, otherwise opening the server fails.
     * Moreover, when the adapter turns off, the GATT server should be closed and
     * reopened next time the adapter is on.
     */
    public synchronized boolean start() {
        if (mGattServer != null) {
            if (LogUtils.debug()) {
                Log.d(TAG, "GattDiscoveryClient already started");
            }
            return true;
        }

        if (LogUtils.debug()) {
            Log.d(TAG, "Starting GattDiscoveryClient");
        }

        BluetoothManager bluetoothManager = mContext.getSystemService(BluetoothManager.class);

        mGattServer = bluetoothManager.openGattServer(mContext, mGattServerCallback);
        if (mGattServer == null) {
            Log.e(TAG, "GattDiscoveryClient failed to open GATT server");
            return false;
        }

        BluetoothGattService service = createGattService(mServiceUuid, mConfigUuid);
        if (service == null) {
            Log.e(TAG, "GattDiscoveryClient failed to create GATT service");
            return false;
        }

        if (!mGattServer.addService(service)) {
            Log.e(TAG, "GattDiscoveryClient failed to add the service");
            return false;
        }

        if (LogUtils.debug()) {
            Log.d(TAG, "Started GattDiscoveryClient");
        }

        return true;
    }

    /**
     * Stops the GATT server.
     *
     * <p>Should be called when Bluetooth adapter is off to avoid keeping a stale GATT server
     * state. Does nothing if the server was already closed.
     */
    public void shutdown() {
        if (mGattServer == null) {
            return;
        }

        if (LogUtils.debug()) {
            Log.d(TAG, "Stopping GattDiscoveryClient");
        }

        mGattServer.clearServices();
        mGattServer.close();
        mGattServer = null;

        if (LogUtils.debug()) {
            Log.d(TAG, "Stopped GattDiscoveryClient");
        }
    }

    @VisibleForTesting
    static BluetoothGattService createGattService(UUID serviceUuid, UUID configUuid) {
        BluetoothGattCharacteristic configCharacteristic = new BluetoothGattCharacteristic(
              configUuid,
              BluetoothGattCharacteristic.PROPERTY_WRITE,
              BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattService service =
            new BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        if (!service.addCharacteristic(configCharacteristic)) {
            Log.e(TAG, "Failed to add config characteristic");
            return null;
        }

        return service;
    }

    public synchronized void dump(IndentingPrintWriter ipw) {
        ipw.print("GattDiscoveryClient [");
        ipw.increaseIndent();
        ipw.printPair("Server status", mGattServer == null ? "Stopped" : "Started");
        ipw.printPair("Companion set", mPeerDevice != null);
        ipw.decreaseIndent();
        ipw.println("]");
    }

    private final class BluetoothCallbackImpl extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (LogUtils.debug()) {
                Log.d(TAG, "onConnectionStateChange() - status: "
                    + status + " newState: " + newState +
                    " isPermittedPeer: " + isPermittedPeer(device));
            }
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
            if (!verifyBasicAccess(device, requestId, responseNeeded, "characteristic write")) {
                return;
            }

            if (!mConfigUuid.equals(characteristic.getUuid())) {
                Log.w(TAG, "Received write for unknown characteristic: "
                        + characteristic.getUuid());
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            if (value == null) {
                Log.w(TAG, "Value not set");
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            ProxyConfig proxyConfig;
            try {
                CodedInputStream input = CodedInputStream.newInstance(value, 0, value.length);
                proxyConfig = ProxyConfig.parseFrom(input);
            } catch (IOException e) {
                Log.e(TAG, "Failed to parse ProxyConfig", e);
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return;
            }

            if (LogUtils.debug()) {
                Log.d(TAG, "Received ProxyConfig psm=" + proxyConfig.psmValue
                        + ", changeId=" + proxyConfig.channelChangeId);
            }

            mListener.onProxyConfigUpdate(
                proxyConfig.psmValue, proxyConfig.channelChangeId);
            if (responseNeeded) {
                sendSuccessResponse(device, requestId, null);
            }
        }

        private boolean verifyBasicAccess(BluetoothDevice device,
                int requestId,
                boolean responseNeeded,
                String operationName) {
            if (!isPermittedPeer(device)) {
                Log.w(TAG, "Received " + operationName + " request from unknown device");
                if (responseNeeded) {
                    sendFailureResponse(device, requestId);
                }
                return false;
            }

            return true;
        }

        private void sendSuccessResponse(BluetoothDevice device, int requestId, byte[] value) {
            BluetoothGattServer server = mGattServer;
            if (server == null) {
                return;
            }

            boolean success = server.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            if (!success) {
                Log.e(TAG, "Failed to send GATT success response");
            }
        }

        private void sendFailureResponse(BluetoothDevice device, int requestId) {
            BluetoothGattServer server = mGattServer;
            if (server == null) {
                return;
            }

            boolean success = server.sendResponse(
                device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            if (!success) {
                Log.e(TAG, "Failed to send GATT failure response");
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            if (LogUtils.debug()) {
                Log.d(TAG, "onNotificationSent() - status: " + status);
            }
        }
    }
}
