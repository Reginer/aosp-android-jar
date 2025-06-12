/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.os.flagging;

import android.aconfigd.Aconfigd.StorageRequestMessages;
import android.aconfigd.Aconfigd.StorageReturnMessages;
import android.annotation.NonNull;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Writes messages to aconfigd, and parses responses.
 *
 * @hide
 */
final class AconfigdSocketWriter {
    private static final String SOCKET_ADDRESS = "aconfigd_system";

    private final LocalSocket mSocket;

    /**
     * Create a new aconfigd socket connection.
     *
     * @hide
     */
    public AconfigdSocketWriter() throws IOException {
        mSocket = new LocalSocket();
        LocalSocketAddress address =
                new LocalSocketAddress(SOCKET_ADDRESS, LocalSocketAddress.Namespace.RESERVED);
        if (!mSocket.isConnected()) {
            mSocket.connect(address);
        }
    }

    /**
     * Serialize {@code messages}, send to aconfigd, then receive and parse response.
     *
     * @param messages messages to send to aconfigd
     * @return a {@code StorageReturnMessages} received from the socket
     * @throws IOException if there is an IOException communicating with the socket
     * @hide
     */
    public StorageReturnMessages sendMessages(@NonNull StorageRequestMessages messages)
            throws IOException {
        OutputStream outputStream = mSocket.getOutputStream();
        byte[] requestMessageBytes = messages.toByteArray();
        outputStream.write(ByteBuffer.allocate(4).putInt(requestMessageBytes.length).array());
        outputStream.write(requestMessageBytes);
        outputStream.flush();

        InputStream inputStream = mSocket.getInputStream();
        byte[] lengthBytes = new byte[4];
        int bytesRead = inputStream.read(lengthBytes);
        if (bytesRead != 4) {
            throw new IOException(
                    "Failed to read message length. Expected 4 bytes, read "
                            + bytesRead
                            + " bytes, with content: "
                            + Arrays.toString(lengthBytes));
        }
        int messageLength = ByteBuffer.wrap(lengthBytes).order(ByteOrder.BIG_ENDIAN).getInt();
        byte[] responseMessageBytes = new byte[messageLength];
        bytesRead = inputStream.read(responseMessageBytes);
        if (bytesRead != messageLength) {
            throw new IOException(
                    "Failed to read complete message. Expected "
                            + messageLength
                            + " bytes, read "
                            + bytesRead
                            + " bytes");
        }

        return StorageReturnMessages.parseFrom(responseMessageBytes);
    }
}
