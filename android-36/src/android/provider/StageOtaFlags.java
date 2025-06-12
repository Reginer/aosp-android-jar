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
package android.provider;

import android.aconfigd.Aconfigd.FlagOverride;
import android.aconfigd.Aconfigd.StorageRequestMessage;
import android.aconfigd.Aconfigd.StorageRequestMessages;
import android.aconfigd.Aconfigd.StorageReturnMessages;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.flagging.AconfigdProtoStreamer;
import android.provider.flags.Flags;
import android.util.AndroidRuntimeException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/** @hide */
@SystemApi
@FlaggedApi(Flags.FLAG_STAGE_FLAGS_FOR_BUILD)
public final class StageOtaFlags {
    private static String LOG_TAG = "StageOtaFlags";
    private static final String SOCKET_ADDRESS = "aconfigd_system";
    private static final String STORAGE_MARKER_FILE_PATH =
            "/metadata/aconfig/boot/enable_only_new_storage";

    /** Aconfig storage is disabled and unavailable for writes. @hide */
    @SystemApi public static final int STATUS_STORAGE_NOT_ENABLED = -1;

    /** Stage request was successful. @hide */
    @SystemApi public static final int STATUS_STAGE_SUCCESS = 0;

    /** @hide */
    @IntDef(
            prefix = {"STATUS_"},
            value = {
                STATUS_STORAGE_NOT_ENABLED,
                STATUS_STAGE_SUCCESS,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StageStatus {}

    private StageOtaFlags() {}

    /**
     * Stage aconfig flags to be applied when booting into {@code buildId}.
     *
     * <p>Only a single {@code buildId} and its corresponding flags are stored at once. Every
     * invocation of this method will overwrite whatever mapping was previously stored.
     *
     * <p>It is an implementation error to call this if the storage is not initialized and ready to
     * receive writes. Callers must ensure that it is available before invoking.
     *
     * <p>TODO(b/361783454): create an isStorageAvailable API and mention it in this docstring.
     *
     * @param flags a map from {@code <packagename>.<flagname>} to flag values
     * @param buildId when the device boots into buildId, it will apply {@code flags}
     * @throws AndroidRuntimeException if communication with aconfigd fails
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_STAGE_FLAGS_FOR_BUILD)
    @StageStatus
    public static int stageBooleanAconfigFlagsForBuild(
            @NonNull Map<String, Boolean> flags, @NonNull String buildId) {
        int flagCount = flags.size();
        Log.d(LOG_TAG, "stageFlagsForBuild invoked for " + flagCount + " flags");
        if (Flags.useProtoInputStream()) {
            try {
                (new AconfigdProtoStreamer()).sendOtaFlagOverrideRequests(flags, buildId);
            } catch (IOException e) {
                throw new AndroidRuntimeException(e);
            }
            return STATUS_STAGE_SUCCESS;
        }

        try {
            LocalSocket socket = new LocalSocket();
            LocalSocketAddress address =
                    new LocalSocketAddress(SOCKET_ADDRESS, LocalSocketAddress.Namespace.RESERVED);
            if (!socket.isConnected()) {
                socket.connect(address);
            }
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            StorageRequestMessages requestMessages = buildRequestMessages(flags, buildId);

            writeToSocket(outputStream, requestMessages);
            readFromSocket(inputStream);
        } catch (IOException e) {
            throw new AndroidRuntimeException(e);
        }

        return STATUS_STAGE_SUCCESS;
    }

    private static void writeToSocket(
            OutputStream outputStream, StorageRequestMessages requestMessages) throws IOException {
        byte[] messageBytes = requestMessages.toByteArray();
        outputStream.write(ByteBuffer.allocate(4).putInt(messageBytes.length).array());
        outputStream.write(messageBytes);
        outputStream.flush();
    }

    private static StorageReturnMessages readFromSocket(InputStream inputStream)
            throws IOException {
        byte[] lengthBytes = new byte[4];
        int bytesRead = inputStream.read(lengthBytes);
        if (bytesRead != 4) {
            throw new IOException("Failed to read message length");
        }

        int messageLength = ByteBuffer.wrap(lengthBytes).order(ByteOrder.BIG_ENDIAN).getInt();

        byte[] messageBytes = new byte[messageLength];
        bytesRead = inputStream.read(messageBytes);
        if (bytesRead != messageLength) {
            throw new IOException("Failed to read complete message");
        }

        return StorageReturnMessages.parseFrom(messageBytes);
    }

    private static StorageRequestMessages buildRequestMessages(
            @NonNull Map<String, Boolean> flags, @NonNull String buildId) {
        StorageRequestMessage.OTAFlagStagingMessage.Builder otaMessageBuilder =
                StorageRequestMessage.OTAFlagStagingMessage.newBuilder().setBuildId(buildId);
        for (Map.Entry<String, Boolean> flagAndValue : flags.entrySet()) {
            String qualifiedFlagName = flagAndValue.getKey();

            // aconfig flags follow a package_name [dot] flag_name convention and will always have
            // a [dot] character in the flag name.
            //
            // If a [dot] character wasn't found it's likely because this was a legacy flag. We make
            // no
            // assumptions here and still attempt to stage these flags with aconfigd and let it
            // decide
            // whether to use the flag / discard it.
            String packageName = "";
            String flagName = qualifiedFlagName;
            int idx = qualifiedFlagName.lastIndexOf(".");
            if (idx != -1) {
                packageName = qualifiedFlagName.substring(0, qualifiedFlagName.lastIndexOf("."));
                flagName = qualifiedFlagName.substring(qualifiedFlagName.lastIndexOf(".") + 1);
            }

            String value = flagAndValue.getValue() ? "true" : "false";
            FlagOverride override =
                    FlagOverride.newBuilder()
                            .setPackageName(packageName)
                            .setFlagName(flagName)
                            .setFlagValue(value)
                            .build();
            otaMessageBuilder.addOverrides(override);
        }
        StorageRequestMessage.OTAFlagStagingMessage otaMessage = otaMessageBuilder.build();
        StorageRequestMessage requestMessage =
                StorageRequestMessage.newBuilder().setOtaStagingMessage(otaMessage).build();
        StorageRequestMessages requestMessages =
                StorageRequestMessages.newBuilder().addMsgs(requestMessage).build();
        return requestMessages;
    }
}
