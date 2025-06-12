/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.internal.configinfra.aconfigd.x.Aconfigd.StorageRequestMessage;
import android.internal.configinfra.aconfigd.x.Aconfigd.StorageRequestMessages;
import android.internal.configinfra.aconfigd.x.Aconfigd.StorageReturnMessage;
import android.internal.configinfra.aconfigd.x.Aconfigd.StorageReturnMessages;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Slog;
import android.util.configinfrastructure.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Writes messages to aconfigd, and parses responses.
 *
 * <p>Uses ProtoInputStream, rather than a proto lite lib.
 *
 * @hide
 */
public final class AconfigdProtoStreamer {
    private static final String SYSTEM_SOCKET_ADDRESS = "aconfigd_system";
    private static final String MAINLINE_SOCKET_ADDRESS = "aconfigd_mainline";

    private static final String TAG = "FlagManager";

    /**
     * Create a new AconfigdProtoStreamer.
     *
     * @hide
     */
    public AconfigdProtoStreamer() {}

    /**
     * Send override removal requests to aconfigd.
     *
     * @param flags the set of flag names to remove local overrides for
     * @param removeType the type of removal: immediately, or on reboot
     * @hide
     */
    public void sendClearFlagOverrideRequests(@NonNull Set<String> flags, long removeType)
            throws IOException {
        ProtoOutputStream requestOutputStream = new ProtoOutputStream();
        for (Flag flag : Flag.buildFlagsWithoutValues(flags)) {
            long msgsToken = requestOutputStream.start(StorageRequestMessages.MSGS);
            long msgToken =
                    requestOutputStream.start(StorageRequestMessage.REMOVE_LOCAL_OVERRIDE_MESSAGE);
            requestOutputStream.write(
                    StorageRequestMessage.RemoveLocalOverrideMessage.PACKAGE_NAME,
                    flag.packageName);
            requestOutputStream.write(
                    StorageRequestMessage.RemoveLocalOverrideMessage.FLAG_NAME, flag.flagName);
            requestOutputStream.write(
                    StorageRequestMessage.RemoveLocalOverrideMessage.REMOVE_ALL, false);
            requestOutputStream.write(
                    StorageRequestMessage.RemoveLocalOverrideMessage.REMOVE_OVERRIDE_TYPE,
                    removeType);
            requestOutputStream.end(msgToken);
            requestOutputStream.end(msgsToken);
        }

        sendBytesAndParseResponse(
                requestOutputStream.getBytes(), StorageReturnMessage.REMOVE_LOCAL_OVERRIDE_MESSAGE);
    }

    /**
     * Send OTA flag-staging requests to aconfigd.
     *
     * @param flags a map from flag names to the values that will be staged
     * @param buildFingerprint the build fingerprint on which the values will be un-staged
     * @hide
     */
    public void sendOtaFlagOverrideRequests(
            @NonNull Map<String, Boolean> flags, @NonNull String buildFingerprint)
            throws IOException {
        ProtoOutputStream requestOutputStream = new ProtoOutputStream();
        long msgsToken = requestOutputStream.start(StorageRequestMessages.MSGS);
        long msgToken = requestOutputStream.start(StorageRequestMessage.OTA_STAGING_MESSAGE);
        requestOutputStream.write(
                StorageRequestMessage.OTAFlagStagingMessage.BUILD_ID, buildFingerprint);
        for (Flag flag : Flag.buildFlags(flags)) {
            long flagOverrideMsgToken =
                    requestOutputStream.start(StorageRequestMessage.FLAG_OVERRIDE_MESSAGE);
            requestOutputStream.write(
                    StorageRequestMessage.FlagOverrideMessage.PACKAGE_NAME, flag.packageName);
            requestOutputStream.write(
                    StorageRequestMessage.FlagOverrideMessage.FLAG_NAME, flag.flagName);
            requestOutputStream.write(
                    StorageRequestMessage.FlagOverrideMessage.FLAG_VALUE, flag.value);
            requestOutputStream.end(flagOverrideMsgToken);
        }
        requestOutputStream.end(msgToken);
        requestOutputStream.end(msgsToken);

        sendBytesAndParseResponse(
                requestOutputStream.getBytes(), StorageReturnMessage.OTA_STAGING_MESSAGE);
    }

    /**
     * Send flag override requests to aconfigd, and parse the response.
     *
     * @hide
     */
    public void sendFlagOverrideRequests(@NonNull Map<String, Boolean> flags, long overrideType)
            throws IOException {
        ProtoOutputStream requestOutputStream = new ProtoOutputStream();
        for (Flag flag : Flag.buildFlags(flags)) {
            long msgsToken = requestOutputStream.start(StorageRequestMessages.MSGS);
            long msgToken = requestOutputStream.start(StorageRequestMessage.FLAG_OVERRIDE_MESSAGE);
            requestOutputStream.write(
                    StorageRequestMessage.FlagOverrideMessage.PACKAGE_NAME, flag.packageName);
            requestOutputStream.write(
                    StorageRequestMessage.FlagOverrideMessage.FLAG_NAME, flag.flagName);
            requestOutputStream.write(
                    StorageRequestMessage.FlagOverrideMessage.FLAG_VALUE, flag.value);
            requestOutputStream.write(
                    StorageRequestMessage.FlagOverrideMessage.OVERRIDE_TYPE, overrideType);
            requestOutputStream.end(msgToken);
            requestOutputStream.end(msgsToken);
        }

        sendBytesAndParseResponse(
                requestOutputStream.getBytes(), StorageReturnMessage.FLAG_OVERRIDE_MESSAGE);
    }

    private void sendBytesAndParseResponse(byte[] requestBytes, long responseMessageToken)
            throws IOException {
        try {
            LocalSocket systemSocket = new LocalSocket();
            LocalSocketAddress systemAddress =
                    new LocalSocketAddress(
                            SYSTEM_SOCKET_ADDRESS, LocalSocketAddress.Namespace.RESERVED);
            if (!systemSocket.isConnected()) {
                systemSocket.connect(systemAddress);
            }

            InputStream inputStream = sendBytesOverSocket(requestBytes, systemSocket);
            parseAconfigdResponse(inputStream, responseMessageToken);

            systemSocket.shutdownInput();
            systemSocket.shutdownOutput();
            systemSocket.close();

        } catch (IOException systemException) {
            Slog.i(
                    TAG,
                    "failed to send request to system socket; trying mainline socket",
                    systemException);

            LocalSocket mainlineSocket = new LocalSocket();
            LocalSocketAddress mainlineAddress =
                    new LocalSocketAddress(
                            MAINLINE_SOCKET_ADDRESS, LocalSocketAddress.Namespace.RESERVED);
            if (!mainlineSocket.isConnected()) {
                mainlineSocket.connect(mainlineAddress);
            }

            InputStream inputStream = sendBytesOverSocket(requestBytes, mainlineSocket);
            parseAconfigdResponse(inputStream, responseMessageToken);

            mainlineSocket.shutdownInput();
            mainlineSocket.shutdownOutput();
            mainlineSocket.close();
        }
    }

    private InputStream sendBytesOverSocket(byte[] requestBytes, LocalSocket socket)
            throws IOException {
        InputStream responseInputStream = null;

        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.writeInt(requestBytes.length);
        outputStream.write(requestBytes);
        responseInputStream = socket.getInputStream();

        return responseInputStream;
    }

    private void parseAconfigdResponse(InputStream inputStream, long responseMessageToken)
            throws IOException {
        ProtoInputStream proto = new ProtoInputStream(inputStream);
        while (true) {
            long currentToken = proto.nextField();

            if (currentToken == ProtoInputStream.NO_MORE_FIELDS) {
                return;
            }

            if (currentToken != ((int) StorageReturnMessages.MSGS)) {
                continue;
            }

            long msgsToken = proto.start(StorageReturnMessages.MSGS);
            long nextToken = proto.nextField();

            if (nextToken == ((int) responseMessageToken)) {
                long msgToken = proto.start(responseMessageToken);
                proto.end(msgToken);
            } else if (nextToken == ((int) StorageReturnMessage.ERROR_MESSAGE)) {
                String errmsg = proto.readString(StorageReturnMessage.ERROR_MESSAGE);
                throw new IOException("override request failed: " + errmsg);
            } else if (nextToken == ProtoInputStream.NO_MORE_FIELDS) {
                // Do nothing.
            } else {
                throw new IOException(
                        "invalid message type, expecting only return message"
                                + " or error message");
            }

            proto.end(msgsToken);
        }
    }

    private static class Flag {
        public final String packageName;
        public final String flagName;
        public final String value;

        public Flag(
                @NonNull String packageName, @NonNull String flagName, @Nullable Boolean value) {
            this.packageName = packageName;
            this.flagName = flagName;

            if (value != null) {
                this.value = Boolean.toString(value);
            } else {
                this.value = null;
            }
        }

        public static Set<Flag> buildFlags(@NonNull Map<String, Boolean> flags) {
            HashSet<Flag> flagSet = new HashSet();
            for (Map.Entry<String, Boolean> flagAndValue : flags.entrySet()) {
                String packageName = "";
                String flagName = flagAndValue.getKey();

                int periodIndex = flagName.lastIndexOf(".");
                if (periodIndex != -1) {
                    packageName = flagName.substring(0, flagName.lastIndexOf("."));
                    flagName = flagName.substring(flagName.lastIndexOf(".") + 1);
                }

                flagSet.add(new Flag(packageName, flagName, flagAndValue.getValue()));
            }
            return flagSet;
        }

        public static Set<Flag> buildFlagsWithoutValues(@NonNull Set<String> flags) {
            HashSet<Flag> flagSet = new HashSet();
            for (String qualifiedFlagName : flags) {
                String packageName = "";
                String flagName = qualifiedFlagName;
                int periodIndex = qualifiedFlagName.lastIndexOf(".");
                if (periodIndex != -1) {
                    packageName =
                            qualifiedFlagName.substring(0, qualifiedFlagName.lastIndexOf("."));
                    flagName = qualifiedFlagName.substring(qualifiedFlagName.lastIndexOf(".") + 1);
                }

                flagSet.add(new Flag(packageName, flagName, null));
            }
            return flagSet;
        }
    }
}
