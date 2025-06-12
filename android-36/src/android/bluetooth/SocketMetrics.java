/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.bluetooth;

import android.os.RemoteException;
import android.util.Log;

/** Utility class for socket metrics */
class SocketMetrics {
    private static final String TAG = SocketMetrics.class.getSimpleName();

    /*package*/ static final int SOCKET_NO_ERROR = -1;

    // Defined in BluetoothProtoEnums.L2capCocConnectionResult of proto logging
    private static final int RESULT_L2CAP_CONN_UNKNOWN = 0;
    /*package*/ static final int RESULT_L2CAP_CONN_SUCCESS = 1;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_SOCKET_CONNECTION_FAILED = 1000;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_SOCKET_CONNECTION_CLOSED = 1001;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_UNABLE_TO_SEND_RPC = 1002;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_NULL_BLUETOOTH_DEVICE = 1003;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_GET_SOCKET_MANAGER_FAILED = 1004;
    private static final int RESULT_L2CAP_CONN_BLUETOOTH_NULL_FILE_DESCRIPTOR = 1005;
    /*package*/ static final int RESULT_L2CAP_CONN_SERVER_FAILURE = 2000;

    // Defined in BluetoothRfcommProtoEnums.RfcommConnectionResult of proto logging
    private static final int RFCOMM_CONN_RESULT_FAILURE_UNKNOWN = 0;
    private static final int RFCOMM_CONN_RESULT_SUCCESS = 1;
    private static final int RFCOMM_CONN_RESULT_SOCKET_CONNECTION_FAILED = 2;
    private static final int RFCOMM_CONN_RESULT_SOCKET_CONNECTION_CLOSED = 3;
    private static final int RFCOMM_CONN_RESULT_UNABLE_TO_SEND_RPC = 4;
    private static final int RFCOMM_CONN_RESULT_NULL_BLUETOOTH_DEVICE = 5;
    private static final int RFCOMM_CONN_RESULT_GET_SOCKET_MANAGER_FAILED = 6;
    private static final int RFCOMM_CONN_RESULT_NULL_FILE_DESCRIPTOR = 7;

    static void logSocketConnect(
            int socketExceptionCode,
            long socketConnectionTimeNanos,
            int connType,
            BluetoothDevice device,
            int port,
            boolean auth,
            long socketCreationTimeNanos,
            long socketCreationLatencyNanos) {
        IBluetooth bluetoothProxy = BluetoothAdapter.getDefaultAdapter().getBluetoothService();
        if (bluetoothProxy == null) {
            Log.w(TAG, "logSocketConnect: bluetoothProxy is null");
            return;
        }
        if (connType == BluetoothSocket.TYPE_L2CAP_LE) {
            try {
                bluetoothProxy.logL2capcocClientConnection(
                        device,
                        port,
                        auth,
                        getL2capLeConnectStatusCode(socketExceptionCode),
                        socketCreationTimeNanos, // to calculate end to end latency
                        socketCreationLatencyNanos, // latency of the constructor
                        socketConnectionTimeNanos); // to calculate the latency of connect()
            } catch (RemoteException e) {
                Log.w(TAG, "logL2capcocServerConnection failed", e);
            }
        } else if (connType == BluetoothSocket.TYPE_RFCOMM) {
            boolean isSerialPort = true; // BluetoothSocket#connect API always uses serial port uuid
            try {
                bluetoothProxy.logRfcommConnectionAttempt(
                        device,
                        auth,
                        getRfcommConnectStatusCode(socketExceptionCode),
                        socketCreationTimeNanos, // to calculate end to end latency
                        isSerialPort);
            } catch (RemoteException e) {
                Log.w(TAG, "logL2capcocServerConnection failed", e);
            }
        } else {
            Log.d(TAG, "No metrics for connection type " + connType);
        }
    }

    static void logSocketAccept(
            BluetoothSocket acceptedSocket,
            BluetoothSocket socket,
            int connType,
            int channel,
            int timeout,
            int result,
            long socketCreationTimeMillis,
            long socketCreationLatencyMillis,
            long socketConnectionTimeMillis) {
        if (connType != BluetoothSocket.TYPE_L2CAP_LE) {
            return;
        }
        IBluetooth bluetoothProxy = BluetoothAdapter.getDefaultAdapter().getBluetoothService();
        if (bluetoothProxy == null) {
            Log.w(TAG, "logSocketConnect: bluetoothProxy is null");
            return;
        }
        try {
            bluetoothProxy.logL2capcocServerConnection(
                    acceptedSocket == null ? null : acceptedSocket.getRemoteDevice(),
                    channel,
                    socket.isAuth(),
                    result,
                    socketCreationTimeMillis, // pass creation time to calculate end to end latency
                    socketCreationLatencyMillis, // socket creation latency
                    socketConnectionTimeMillis, // send connection start time for connection latency
                    timeout);
        } catch (RemoteException e) {
            Log.w(TAG, "logL2capcocServerConnection failed", e);
        }
    }

    private static int getL2capLeConnectStatusCode(int socketExceptionCode) {
        switch (socketExceptionCode) {
            case (SOCKET_NO_ERROR):
                return RESULT_L2CAP_CONN_SUCCESS;
            case (BluetoothSocketException.NULL_DEVICE):
                return RESULT_L2CAP_CONN_BLUETOOTH_NULL_BLUETOOTH_DEVICE;
            case (BluetoothSocketException.SOCKET_MANAGER_FAILURE):
                return RESULT_L2CAP_CONN_BLUETOOTH_GET_SOCKET_MANAGER_FAILED;
            case (BluetoothSocketException.SOCKET_CLOSED):
                return RESULT_L2CAP_CONN_BLUETOOTH_SOCKET_CONNECTION_CLOSED;
            case (BluetoothSocketException.SOCKET_CONNECTION_FAILURE):
                return RESULT_L2CAP_CONN_BLUETOOTH_SOCKET_CONNECTION_FAILED;
            case (BluetoothSocketException.RPC_FAILURE):
                return RESULT_L2CAP_CONN_BLUETOOTH_UNABLE_TO_SEND_RPC;
            case (BluetoothSocketException.UNIX_FILE_SOCKET_CREATION_FAILURE):
                return RESULT_L2CAP_CONN_BLUETOOTH_NULL_FILE_DESCRIPTOR;
            default:
                return RESULT_L2CAP_CONN_UNKNOWN;
        }
    }

    private static int getRfcommConnectStatusCode(int socketExceptionCode) {
        switch (socketExceptionCode) {
            case (SOCKET_NO_ERROR):
                return RFCOMM_CONN_RESULT_SUCCESS;
            case (BluetoothSocketException.NULL_DEVICE):
                return RFCOMM_CONN_RESULT_NULL_BLUETOOTH_DEVICE;
            case (BluetoothSocketException.SOCKET_MANAGER_FAILURE):
                return RFCOMM_CONN_RESULT_GET_SOCKET_MANAGER_FAILED;
            case (BluetoothSocketException.SOCKET_CLOSED):
                return RFCOMM_CONN_RESULT_SOCKET_CONNECTION_CLOSED;
            case (BluetoothSocketException.SOCKET_CONNECTION_FAILURE):
                return RFCOMM_CONN_RESULT_SOCKET_CONNECTION_FAILED;
            case (BluetoothSocketException.RPC_FAILURE):
                return RFCOMM_CONN_RESULT_UNABLE_TO_SEND_RPC;
            case (BluetoothSocketException.UNIX_FILE_SOCKET_CREATION_FAILURE):
                return RFCOMM_CONN_RESULT_NULL_FILE_DESCRIPTOR;
            default:
                return RFCOMM_CONN_RESULT_FAILURE_UNKNOWN;
        }
    }
}
