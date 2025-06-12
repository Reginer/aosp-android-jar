/*
 * Copyright 2024 The Android Open Source Project
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

import static android.bluetooth.BluetoothSocket.SocketType;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.annotation.SystemApi;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Defines parameters for creating Bluetooth server and client socket channels.
 *
 * <p>Used with {@link BluetoothAdapter#listenUsingSocketSettings} to create a server socket and
 * {@link BluetoothDevice#createUsingSocketSettings} to create a client socket.
 *
 * @see BluetoothAdapter#listenUsingSocketSettings
 * @see BluetoothDevice#createUsingSocketSettings
 */
@FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
public final class BluetoothSocketSettings {

    private static final int L2CAP_PSM_UNSPECIFIED = -1;

    /**
     * Annotation to define the data path used for Bluetooth socket communication. This determines
     * how data flows between the application and the Bluetooth controller.
     *
     * @hide
     */
    @IntDef(
            prefix = {"DATA_PATH_"},
            value = {DATA_PATH_NO_OFFLOAD, DATA_PATH_HARDWARE_OFFLOAD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SocketDataPath {}

    /**
     * Non-offload data path where the application's socket data is processed by the main Bluetooth
     * stack.
     *
     * @hide
     */
    @SystemApi public static final int DATA_PATH_NO_OFFLOAD = 0;

    /**
     * Hardware offload data path where the application's socket data is processed by a offloaded
     * application running on the low-power processor.
     *
     * <p>Using this data path requires the {@code BLUETOOTH_PRIVILEGED} permission, which will be
     * checked when a socket connection or channel is created.
     *
     * @hide
     */
    @SystemApi public static final int DATA_PATH_HARDWARE_OFFLOAD = 1;

    /**
     * Maximum size (in bytes) of a data packet that can be received from the endpoint when using
     * {@link #DATA_PATH_HARDWARE_OFFLOAD}.
     */
    @SystemApi private static final int HARDWARE_OFFLOAD_PACKET_MAX_SIZE = 65535;

    /**
     * Maximum length (in bytes) of a socket name when using {@link #DATA_PATH_HARDWARE_OFFLOAD}.
     */
    @SystemApi private static final int HARDWARE_OFFLOAD_SOCKET_NAME_MAX_LENGTH = 127;

    /**
     * Constant representing an invalid hub ID. This value indicates that a hub ID has not been
     * assigned or is not valid.
     *
     * @hide
     */
    private static final long INVALID_HUB_ID = 0;

    /**
     * Constant representing an invalid hub endpoint ID. This value indicates that an endpoint ID
     * has not been assigned or is not valid.
     *
     * @hide
     */
    private static final long INVALID_ENDPOINT_ID = 0;

    /** Type of the Bluetooth socket */
    @SocketType private int mSocketType;

    /** Encryption requirement for the Bluetooth socket. */
    private boolean mEncryptionRequired;

    /** Authentication requirement for the Bluetooth socket. */
    private boolean mAuthenticationRequired;

    /** L2CAP Protocol/Service Multiplexer (PSM) for the Bluetooth Socket. */
    private int mL2capPsm;

    /** RFCOMM service name associated with the Bluetooth socket. */
    private String mRfcommServiceName;

    /** RFCOMM service UUID associated with the Bluetooth socket. */
    private UUID mRfcommUuid;

    /**
     * Specifies the data path used for this socket, influencing how data is transmitted and
     * processed. Select the appropriate data path based on performance and power consumption
     * requirements:
     *
     * <ul>
     *   <li>{@link #DATA_PATH_NO_OFFLOAD}: Suitable for applications that require the full
     *       processing capabilities of the main Bluetooth stack.
     *   <li>{@link #DATA_PATH_HARDWARE_OFFLOAD}: Optimized for lower power consumption by utilizing
     *       an offloaded application running on a dedicated low-power processor.
     * </ul>
     */
    @SocketDataPath private int mDataPath;

    /**
     * A user-friendly name for this socket, primarily for debugging and logging. This name should
     * be descriptive and can help identify the socket during development and troubleshooting.
     *
     * <p>When using {@link #DATA_PATH_HARDWARE_OFFLOAD}, this name is also passed to the offloaded
     * application running on the low-power processor. This allows the offloaded application to
     * identify and manage the socket.
     */
    private String mSocketName;

    /**
     * When using {@link #DATA_PATH_HARDWARE_OFFLOAD}, this identifies the hub hosting the endpoint.
     *
     * <p>Hub represents a logical/physical representation of multiple endpoints. A pair of {@code
     * mHubId} and {@code mEndpointId} uniquely identifies the endpoint globally.
     */
    private long mHubId;

    /**
     * When using {@link #DATA_PATH_HARDWARE_OFFLOAD}, this identifies the specific endpoint within
     * the hub that is associated with this socket.
     */
    private long mEndpointId;

    /**
     * The maximum size (in bytes) of a single data packet that can be received from the endpoint
     * when using {@link #DATA_PATH_HARDWARE_OFFLOAD}.
     */
    private int mMaximumPacketSize;

    /**
     * Returns the type of the Bluetooth socket.
     *
     * <p>Defaults to {@code BluetoothSocket#TYPE_RFCOMM}.
     */
    @RequiresNoPermission
    @SocketType
    public int getSocketType() {
        return mSocketType;
    }

    /** Returns the L2CAP PSM value used for a BluetoothSocket#TYPE_LE socket. */
    @RequiresNoPermission
    public @IntRange(from = 128, to = 255) int getL2capPsm() {
        return mL2capPsm;
    }

    /**
     * Returns the RFCOMM service name used for a BluetoothSocket#TYPE_RFCOMM socket.
     *
     * <p>Defaults to {@code null}.
     */
    @Nullable
    @RequiresNoPermission
    public String getRfcommServiceName() {
        return mRfcommServiceName;
    }

    /**
     * Returns the RFCOMM service UUID used for a BluetoothSocket#TYPE_RFCOMM socket.
     *
     * <p>Defaults to {@code null}.
     */
    @Nullable
    @RequiresNoPermission
    public UUID getRfcommUuid() {
        return mRfcommUuid;
    }

    /**
     * Checks if encryption is enabled for the Bluetooth socket.
     *
     * <p>Defaults to {@code false}.
     */
    @RequiresNoPermission
    public boolean isEncryptionRequired() {
        return mEncryptionRequired;
    }

    /**
     * Checks if authentication is enabled for the Bluetooth socket.
     *
     * <p>Defaults to {@code false}.
     */
    @RequiresNoPermission
    public boolean isAuthenticationRequired() {
        return mAuthenticationRequired;
    }

    /**
     * Returns the data path used for this socket. The data path determines how data is routed and
     * processed for the socket connection.
     *
     * <p>Defaults to {@link #DATA_PATH_NO_OFFLOAD}.
     *
     * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
     * available through the System API.
     *
     * @hide
     */
    @SystemApi
    @RequiresNoPermission
    public @SocketDataPath int getDataPath() {
        return mDataPath;
    }

    /**
     * Returns the user-friendly name assigned to this socket. This name is primarily used for
     * debugging and logging purposes.
     *
     * <p>When using {@link #DATA_PATH_HARDWARE_OFFLOAD}, this name is also passed to the offloaded
     * application running on the low-power processor.
     *
     * <p>Defaults to {@code null} if no name was explicitly set.
     *
     * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
     * available through the System API.
     *
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresNoPermission
    public String getSocketName() {
        return mSocketName;
    }

    /**
     * Returns the ID of the hub associated with this socket when using {@link
     * #DATA_PATH_HARDWARE_OFFLOAD}.
     *
     * <p>If the data path is not set to {@link #DATA_PATH_HARDWARE_OFFLOAD}, this method returns
     * {@link #INVALID_HUB_ID}.
     *
     * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
     * available through the System API.
     *
     * @hide
     */
    @SystemApi
    @RequiresNoPermission
    public long getHubId() {
        if (mDataPath != DATA_PATH_HARDWARE_OFFLOAD) {
            return INVALID_HUB_ID;
        }
        return mHubId;
    }

    /**
     * Returns the ID of the endpoint within the hub associated with this socket when using {@link
     * #DATA_PATH_HARDWARE_OFFLOAD}. An endpoint represents a specific point of communication within
     * the hub.
     *
     * <p>If the data path is not set to {@link #DATA_PATH_HARDWARE_OFFLOAD}, this method returns
     * {@link #INVALID_ENDPOINT_ID}.
     *
     * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
     * available through the System API.
     *
     * @hide
     */
    @SystemApi
    @RequiresNoPermission
    public long getEndpointId() {
        if (mDataPath != DATA_PATH_HARDWARE_OFFLOAD) {
            return INVALID_ENDPOINT_ID;
        }
        return mEndpointId;
    }

    /**
     * Returns the requested maximum size (in bytes) of a data packet that can be received from the
     * endpoint associated with this socket when using {@link #DATA_PATH_HARDWARE_OFFLOAD}.
     *
     * <p>Defaults to {@link #HARDWARE_OFFLOAD_PACKET_MAX_SIZE}.
     *
     * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
     * available through the System API.
     *
     * @hide
     */
    @SystemApi
    @RequiresNoPermission
    public int getRequestedMaximumPacketSize() {
        return mMaximumPacketSize;
    }

    /**
     * Returns a {@link String} that describes each BluetoothSocketSettings parameter current value.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("BluetoothSocketSettings{");
        builder.append("mSocketType=")
                .append(mSocketType)
                .append(", mEncryptionRequired=")
                .append(mEncryptionRequired)
                .append(", mAuthenticationRequired=")
                .append(mAuthenticationRequired);
        if (mSocketType == BluetoothSocket.TYPE_RFCOMM) {
            builder.append(", mRfcommServiceName=")
                    .append(mRfcommServiceName)
                    .append(", mRfcommUuid=")
                    .append(mRfcommUuid);
        } else {
            builder.append(", mL2capPsm=").append(mL2capPsm);
        }
        if (mDataPath == DATA_PATH_HARDWARE_OFFLOAD) {
            builder.append(", mDataPath=")
                    .append(mDataPath)
                    .append(", mSocketName=")
                    .append(mSocketName)
                    .append(", mHubId=")
                    .append(mHubId)
                    .append(", mEndpointId=")
                    .append(mEndpointId)
                    .append(", mMaximumPacketSize=")
                    .append(mMaximumPacketSize);
        }
        builder.append("}");
        return builder.toString();
    }

    private BluetoothSocketSettings(
            int socketType,
            int l2capPsm,
            boolean encryptionRequired,
            boolean authenticationRequired,
            String rfcommServiceName,
            UUID rfcommUuid,
            int dataPath,
            String socketName,
            long hubId,
            long endpointId,
            int maximumPacketSize) {
        mSocketType = socketType;
        mL2capPsm = l2capPsm;
        mEncryptionRequired = encryptionRequired;
        mAuthenticationRequired = authenticationRequired;
        mRfcommUuid = rfcommUuid;
        mRfcommServiceName = rfcommServiceName;
        mDataPath = dataPath;
        mSocketName = socketName;
        mHubId = hubId;
        mEndpointId = endpointId;
        mMaximumPacketSize = maximumPacketSize;
    }

    /** Builder for {@link BluetoothSocketSettings}. */
    @FlaggedApi(Flags.FLAG_SOCKET_SETTINGS_API)
    public static final class Builder {
        private int mSocketType = BluetoothSocket.TYPE_RFCOMM;
        private int mL2capPsm = L2CAP_PSM_UNSPECIFIED;
        private boolean mEncryptionRequired = false;
        private boolean mAuthenticationRequired = false;
        private String mRfcommServiceName = null;
        private UUID mRfcommUuid = null;
        private int mDataPath = DATA_PATH_NO_OFFLOAD;
        private String mSocketName = BluetoothSocket.DEFAULT_SOCKET_NAME;
        private long mHubId = INVALID_HUB_ID;
        private long mEndpointId = INVALID_ENDPOINT_ID;
        private int mMaximumPacketSize = HARDWARE_OFFLOAD_PACKET_MAX_SIZE;

        public Builder() {}

        /**
         * Sets the socket type.
         *
         * <p>Must be one of:
         *
         * <ul>
         *   <li>{@link BluetoothSocket#TYPE_RFCOMM}
         *   <li>{@link BluetoothSocket#TYPE_LE}
         * </ul>
         *
         * <p>Defaults to {@code BluetoothSocket#TYPE_RFCOMM}.
         *
         * @param socketType The type of socket.
         * @return This builder.
         * @throws IllegalArgumentException If {@code socketType} is invalid.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setSocketType(@SocketType int socketType) {
            if (socketType != BluetoothSocket.TYPE_RFCOMM
                    && socketType != BluetoothSocket.TYPE_LE) {
                throw new IllegalArgumentException("invalid socketType - " + socketType);
            }
            mSocketType = socketType;
            return this;
        }

        /**
         * Sets the L2CAP PSM (Protocol/Service Multiplexer) for the Bluetooth socket.
         *
         * <p>This is only used for {@link BluetoothSocket#TYPE_LE} sockets.
         *
         * <p>Valid PSM values for {@link BluetoothSocket#TYPE_LE} sockets is ranging from 128
         * (0x80) to 255 (0xFF).
         *
         * <p>Application using this API is responsible for obtaining protocol/service multiplexer
         * (PSM) value from remote device.
         *
         * @param l2capPsm The L2CAP PSM value.
         * @return This builder.
         * @throws IllegalArgumentException If l2cap PSM is not in given range.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setL2capPsm(@IntRange(from = 128, to = 255) int l2capPsm) {
            if (l2capPsm < 128 || l2capPsm > 255) {
                throw new IllegalArgumentException("invalid L2cap PSM - " + l2capPsm);
            }
            mL2capPsm = l2capPsm;
            return this;
        }

        /**
         * Sets the encryption requirement for the Bluetooth socket.
         *
         * <p>Defaults to {@code false}.
         *
         * @param encryptionRequired {@code true} if encryption is required for this socket, {@code
         *     false} otherwise.
         * @return This builder.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setEncryptionRequired(boolean encryptionRequired) {
            mEncryptionRequired = encryptionRequired;
            return this;
        }

        /**
         * Sets the authentication requirement for the Bluetooth socket.
         *
         * <p>Defaults to {@code false}.
         *
         * @param authenticationRequired {@code true} if authentication is required for this socket,
         *     {@code false} otherwise.
         * @return This builder.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setAuthenticationRequired(boolean authenticationRequired) {
            mAuthenticationRequired = authenticationRequired;
            return this;
        }

        /**
         * Sets the RFCOMM service name associated with the Bluetooth socket.
         *
         * <p>This name is used to identify the service when a remote device searches for it using
         * SDP.
         *
         * <p>This is only used for {@link BluetoothSocket#TYPE_RFCOMM} sockets.
         *
         * <p>Defaults to {@code null}.
         *
         * @param rfcommServiceName The RFCOMM service name.
         * @return This builder.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setRfcommServiceName(@NonNull String rfcommServiceName) {
            mRfcommServiceName = rfcommServiceName;
            return this;
        }

        /**
         * Sets the RFCOMM service UUID associated with the Bluetooth socket.
         *
         * <p>This UUID is used to uniquely identify the service when a remote device searches for
         * it using SDP.
         *
         * <p>This is only used for {@link BluetoothSocket#TYPE_RFCOMM} sockets.
         *
         * <p>Defaults to {@code null}.
         *
         * @param rfcommUuid The RFCOMM service UUID.
         * @return This builder.
         */
        @NonNull
        @RequiresNoPermission
        public Builder setRfcommUuid(@NonNull UUID rfcommUuid) {
            mRfcommUuid = rfcommUuid;
            return this;
        }

        /**
         * Sets the data path for this socket. The data path determines how data is routed and
         * processed for the socket connection.
         *
         * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
         * available through the System API.
         *
         * @param dataPath The desired data path for the socket.
         * @return This Builder object to allow for method chaining.
         * @throws IllegalArgumentException If {@code dataPath} is an invalid value.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setDataPath(@SocketDataPath int dataPath) {
            if (dataPath < DATA_PATH_NO_OFFLOAD || dataPath > DATA_PATH_HARDWARE_OFFLOAD) {
                throw new IllegalArgumentException("Invalid dataPath - " + dataPath);
            }
            mDataPath = dataPath;
            return this;
        }

        /**
         * Sets a user-friendly name for this socket. This name is primarily used for debugging and
         * logging purposes.
         *
         * <p>When using {@link #DATA_PATH_HARDWARE_OFFLOAD}, this name is also passed to the
         * offloaded application running on low-power processor.
         *
         * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
         * available through the System API.
         *
         * @param socketName The desired name for the socket. This should be a descriptive name that
         *     helps identify the socket during development and troubleshooting. The socket name
         *     cannot exceed {@link #HARDWARE_OFFLOAD_SOCKET_NAME_MAX_LENGTH} bytes in length when
         *     encoded in UTF-8.
         * @return This Builder object to allow for method chaining.
         * @throws IllegalArgumentException if the provided `socketName` exceeds {@link
         *     #HARDWARE_OFFLOAD_SOCKET_NAME_MAX_LENGTH} bytes when encoded in UTF-8.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setSocketName(@NonNull String socketName) {
            byte[] socketNameBytes = requireNonNull(socketName).getBytes(StandardCharsets.UTF_8);
            if (socketNameBytes.length > HARDWARE_OFFLOAD_SOCKET_NAME_MAX_LENGTH) {
                throw new IllegalArgumentException(
                        "Socket name cannot exceed "
                                + HARDWARE_OFFLOAD_SOCKET_NAME_MAX_LENGTH
                                + " bytes in length when encoded in UTF-8.");
            }
            mSocketName = requireNonNull(socketName);
            return this;
        }

        /**
         * Sets the ID of the hub to be associated with this socket when using {@link
         * #DATA_PATH_HARDWARE_OFFLOAD}.
         *
         * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
         * available through the System API.
         *
         * @param hubId The ID of the hub.
         * @return This Builder object to allow for method chaining.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setHubId(long hubId) {
            mHubId = hubId;
            return this;
        }

        /**
         * Sets the ID of the endpoint within the hub to be associated with this socket when using
         * {@link #DATA_PATH_HARDWARE_OFFLOAD}. An endpoint represents a specific point of
         * communication within the hub.
         *
         * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
         * available through the System API.
         *
         * @param endpointId The ID of the endpoint within the hub.
         * @return This Builder object to allow for method chaining.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setEndpointId(long endpointId) {
            mEndpointId = endpointId;
            return this;
        }

        /**
         * Sets the requested maximum size (in bytes) of a data packet that can be received from the
         * endpoint associated with this socket when using {@link #DATA_PATH_HARDWARE_OFFLOAD}.
         *
         * <p>The main Bluetooth stack may adjust this value based on the actual capabilities
         * negotiated with the peer device during connection establishment. To get the final
         * negotiated value, use {@link BluetoothSocket#getMaxReceivePacketSize()} after the socket
         * is connected.
         *
         * <p>This API is part of the System API because {@link #DATA_PATH_HARDWARE_OFFLOAD} is only
         * available through the System API.
         *
         * @param maximumPacketSize The maximum packet size in bytes.
         * @return This Builder object to allow for method chaining.
         * @hide
         */
        @SystemApi
        @NonNull
        @RequiresNoPermission
        public Builder setRequestedMaximumPacketSize(int maximumPacketSize) {
            mMaximumPacketSize = maximumPacketSize;
            return this;
        }

        /**
         * Builds a {@link BluetoothSocketSettings} object.
         *
         * @return A new {@link BluetoothSocketSettings} object with the configured parameters.
         * @throws IllegalArgumentException on invalid parameters
         */
        @NonNull
        @RequiresNoPermission
        public BluetoothSocketSettings build() {
            if (mSocketType == BluetoothSocket.TYPE_RFCOMM) {
                if (mRfcommUuid == null) {
                    throw new IllegalArgumentException("RFCOMM socket with missing uuid");
                }
                if (mL2capPsm != L2CAP_PSM_UNSPECIFIED) {
                    throw new IllegalArgumentException(
                            "Invalid Socket config: "
                                    + " Socket type: "
                                    + mSocketType
                                    + " L2cap PSM: "
                                    + mL2capPsm);
                }
            }
            if (mSocketType == BluetoothSocket.TYPE_LE) {
                if (mRfcommUuid != null) {
                    throw new IllegalArgumentException(
                            "Invalid Socket config: "
                                    + "Socket type: "
                                    + mSocketType
                                    + " Rfcomm Service Name: "
                                    + mRfcommServiceName
                                    + " Rfcomm Uuid: "
                                    + mRfcommUuid);
                }
            }
            if (mDataPath == DATA_PATH_HARDWARE_OFFLOAD) {
                if (mHubId == INVALID_HUB_ID || mEndpointId == INVALID_ENDPOINT_ID) {
                    throw new IllegalArgumentException(
                            "Hub ID and endpoint ID must be set for hardware data path");
                }
                if (mMaximumPacketSize < 0) {
                    throw new IllegalArgumentException("invalid packet size " + mMaximumPacketSize);
                }
            } else {
                if (mHubId != INVALID_HUB_ID || mEndpointId != INVALID_ENDPOINT_ID) {
                    throw new IllegalArgumentException(
                            "Hub ID and endpoint ID may not be set for software data path");
                }
            }
            return new BluetoothSocketSettings(
                    mSocketType,
                    mL2capPsm,
                    mEncryptionRequired,
                    mAuthenticationRequired,
                    mRfcommServiceName,
                    mRfcommUuid,
                    mDataPath,
                    mSocketName,
                    mHubId,
                    mEndpointId,
                    mMaximumPacketSize);
        }
    }
}
