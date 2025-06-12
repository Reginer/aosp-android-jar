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

import android.aconfigd.Aconfigd.FlagOverride;
import android.aconfigd.Aconfigd.StorageRequestMessage;
import android.aconfigd.Aconfigd.StorageRequestMessage.FlagOverrideType;
import android.aconfigd.Aconfigd.StorageRequestMessage.RemoveOverrideType;
import android.aconfigd.Aconfigd.StorageRequestMessages;
import android.aconfigd.Aconfigd.StorageReturnMessage;
import android.aconfigd.Aconfigd.StorageReturnMessages;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.provider.flags.Flags;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides write access to aconfigd-backed flag storage.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
@SystemService(FlagManager.FLAG_SERVICE_NAME)
public final class FlagManager {
    /**
     * Create a new FlagManager.
     *
     * @hide
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public FlagManager(@NonNull Context unusedContext) {}

    /**
     * Use with {@link #getSystemService(String)} to retrieve a {@link
     * android.os.flagging.FlagManager} for pushing flag values to aconfig.
     *
     * @see Context#getSystemService(String)
     * @hide
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public static final String FLAG_SERVICE_NAME = "flag";

    /**
     * Stage flag values, to apply when the device boots into system build {@code buildFingerprint}.
     *
     * <p>The mapping persists across reboots, until the device finally boots into the system {@code
     * buildFingerprint}, when the mapping is cleared.
     *
     * <p>Only one {@code buildFingerprint} and map of flags can be stored at a time. Subsequent
     * calls will overwrite the existing mapping.
     *
     * <p>If overrides are staged for the next reboot, from {@link
     * WriteAconfig#setOverridesOnReboot}, and overrides are also staged for a {@code
     * buildFingerprint}, and the device boots into {@code buildFingerprint}, the {@code
     * buildFingerprint}-associated overrides will take precedence over the reboot-associated
     * overrides.
     *
     * @param buildFingerprint a system build fingerprint identifier.
     * @param flags map from flag qualified name to new value.
     * @throws AconfigStorageWriteException if the write fails.
     * @see android.os.Build.FINGERPRINT
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public void setBooleanOverridesOnSystemBuildFingerprint(
            @NonNull String buildFingerprint, @NonNull Map<String, Boolean> flags) {
        if (Flags.useProtoInputStream()) {
            try {
                (new AconfigdProtoStreamer()).sendOtaFlagOverrideRequests(flags, buildFingerprint);
            } catch (IOException e) {
                throw new AconfigStorageWriteException(
                        "failed to set boolean overrides on system build fingerprint", e);
            }
            return;
        }
        StorageRequestMessages requestMessages =
                buildOtaFlagStagingMessages(Flag.buildFlags(flags), buildFingerprint);
        sendMessages(requestMessages);
    }

    /**
     * Stage flag values, to apply when the device reboots.
     *
     * <p>These flags will be cleared on the next reboot, regardless of whether they take effect.
     * See {@link setBooleanOverridesOnSystemBuildFingerprint} for a thorough description of how the
     * set of flags to take effect is determined on the next boot.
     *
     * @param flags map from flag qualified name to new value.
     * @throws AconfigStorageWriteException if the write fails.
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public void setBooleanOverridesOnReboot(@NonNull Map<String, Boolean> flags) {
        if (Flags.useProtoInputStream()) {
            try {
                (new AconfigdProtoStreamer())
                        .sendFlagOverrideRequests(
                                flags,
                                android.internal.configinfra.aconfigd.x.Aconfigd
                                        .StorageRequestMessage.SERVER_ON_REBOOT);
            } catch (IOException e) {
                throw new AconfigStorageWriteException(
                        "failed to set boolean overrides on reboot", e);
            }
            return;
        }
        StorageRequestMessages requestMessages =
                buildFlagOverrideMessages(
                        Flag.buildFlags(flags), FlagOverrideType.SERVER_ON_REBOOT);
        sendMessages(requestMessages);
    }

    /**
     * Set local overrides, to apply on device reboot.
     *
     * <p>Local overrides take precedence over normal overrides. They must be cleared for normal
     * overrides to take effect again.
     *
     * @param flags map from flag qualified name to new value.
     * @see clearBooleanLocalOverridesOnReboot
     * @see clearBooleanLocalOverridesImmediately
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public void setBooleanLocalOverridesOnReboot(@NonNull Map<String, Boolean> flags) {
        if (Flags.useProtoInputStream()) {
            try {
                (new AconfigdProtoStreamer())
                        .sendFlagOverrideRequests(
                                flags,
                                android.internal.configinfra.aconfigd.x.Aconfigd
                                        .StorageRequestMessage.LOCAL_ON_REBOOT);
            } catch (IOException e) {
                throw new AconfigStorageWriteException(
                        "failed to set boolean overrides on reboot", e);
            }
            return;
        }
        StorageRequestMessages requestMessages =
                buildFlagOverrideMessages(Flag.buildFlags(flags), FlagOverrideType.LOCAL_ON_REBOOT);
        sendMessages(requestMessages);
    }

    /**
     * Set local overrides, to apply immediately.
     *
     * <p>Local overrides take precedence over normal overrides. They must be cleared for normal
     * overrides to take effect again.
     *
     * <p>Note that processes cache flag values, so a process restart or reboot is still required to
     * get the latest flag value.
     *
     * @param flags map from flag qualified name to new value.
     * @see clearBooleanLocalOverridesOnReboot
     * @see clearBooleanLocalOverridesImmediately
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public void setBooleanLocalOverridesImmediately(@NonNull Map<String, Boolean> flags) {
        if (Flags.useProtoInputStream()) {
            try {
                (new AconfigdProtoStreamer())
                        .sendFlagOverrideRequests(
                                flags,
                                android.internal.configinfra.aconfigd.x.Aconfigd
                                        .StorageRequestMessage.LOCAL_IMMEDIATE);
            } catch (IOException e) {
                throw new AconfigStorageWriteException(
                        "failed to set boolean overrides on reboot", e);
            }
            return;
        }
        StorageRequestMessages requestMessages =
                buildFlagOverrideMessages(Flag.buildFlags(flags), FlagOverrideType.LOCAL_IMMEDIATE);
        sendMessages(requestMessages);
    }

    /**
     * Clear local overrides, to take effect on reboot.
     *
     * <p>If {@code flags} is {@code null}, clear all local overrides.
     *
     * @param flags map from flag qualified name to new value.
     * @see setBooleanLocalOverridesOnReboot
     * @see setBooleanLocalOverridesImmediately
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public void clearBooleanLocalOverridesOnReboot(@Nullable Set<String> flags) {
        if (Flags.useProtoInputStream()) {
            try {
                (new AconfigdProtoStreamer())
                        .sendClearFlagOverrideRequests(
                                flags,
                                android.internal.configinfra.aconfigd.x.Aconfigd
                                        .StorageRequestMessage.REMOVE_LOCAL_ON_REBOOT);
            } catch (IOException e) {
                throw new AconfigStorageWriteException(
                        "failed to set boolean overrides on reboot", e);
            }
            return;
        }
        StorageRequestMessages requestMessages =
                buildClearFlagOverridesMessages(
                        Flag.buildFlagsWithoutValues(flags),
                        RemoveOverrideType.REMOVE_LOCAL_ON_REBOOT);
        sendMessages(requestMessages);
    }

    /**
     * Clear local overrides, to take effect immediately.
     *
     * <p>Note that processes cache flag values, so a process restart or reboot is still required to
     * get the latest flag value.
     *
     * <p>If {@code flags} is {@code null}, clear all local overrides.
     *
     * @param flags map from flag qualified name to new value.
     * @see setBooleanLocalOverridesOnReboot
     * @see setBooleanLocalOverridesImmediately
     */
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public void clearBooleanLocalOverridesImmediately(@Nullable Set<String> flags) {
        if (Flags.useProtoInputStream()) {
            try {
                (new AconfigdProtoStreamer())
                        .sendClearFlagOverrideRequests(
                                flags,
                                android.internal.configinfra.aconfigd.x.Aconfigd
                                        .StorageRequestMessage.REMOVE_LOCAL_IMMEDIATE);
            } catch (IOException e) {
                throw new AconfigStorageWriteException(
                        "failed to set boolean overrides on reboot", e);
            }
            return;
        }
        StorageRequestMessages requestMessages =
                buildClearFlagOverridesMessages(
                        Flag.buildFlagsWithoutValues(flags),
                        RemoveOverrideType.REMOVE_LOCAL_IMMEDIATE);
        sendMessages(requestMessages);
    }

    private void sendMessages(StorageRequestMessages messages) {
        try {
            StorageReturnMessages returnMessages =
                    (new AconfigdSocketWriter()).sendMessages(messages);

            String errorMessage = "";
            for (StorageReturnMessage message : returnMessages.getMsgsList()) {
                if (message.hasErrorMessage()) {
                    errorMessage += message.getErrorMessage() + "\n";
                }
            }

            if (!errorMessage.isEmpty()) {
                throw new AconfigStorageWriteException(
                        "error(s) writing aconfig flags: " + errorMessage);
            }
        } catch (IOException e) {
            throw new AconfigStorageWriteException("IO error writing aconfig flags", e);
        }
    }

    private static class Flag {
        public final String packageName;
        public final String flagName;
        public final String value;

        public Flag(@NonNull String qualifiedName, @Nullable Boolean value) {
            packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
            flagName = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
            this.value = Boolean.toString(value);
        }

        public static Set<Flag> buildFlags(@NonNull Map<String, Boolean> flags) {
            HashSet<Flag> flagSet = new HashSet();
            for (Map.Entry<String, Boolean> flagAndValue : flags.entrySet()) {
                flagSet.add(new Flag(flagAndValue.getKey(), flagAndValue.getValue()));
            }
            return flagSet;
        }

        public static Set<Flag> buildFlagsWithoutValues(@NonNull Set<String> flags) {
            HashSet<Flag> flagSet = new HashSet();
            for (String flag : flags) {
                flagSet.add(new Flag(flag, null));
            }
            return flagSet;
        }
    }

    private static StorageRequestMessages buildFlagOverrideMessages(
            @NonNull Set<Flag> flagSet, FlagOverrideType overrideType) {
        StorageRequestMessages.Builder requestMessagesBuilder = StorageRequestMessages.newBuilder();
        for (Flag flag : flagSet) {
            StorageRequestMessage.FlagOverrideMessage message =
                    StorageRequestMessage.FlagOverrideMessage.newBuilder()
                            .setPackageName(flag.packageName)
                            .setFlagName(flag.flagName)
                            .setFlagValue(flag.value)
                            .setOverrideType(overrideType)
                            .build();
            StorageRequestMessage requestMessage =
                    StorageRequestMessage.newBuilder().setFlagOverrideMessage(message).build();
            requestMessagesBuilder.addMsgs(requestMessage);
        }
        return requestMessagesBuilder.build();
    }

    private static StorageRequestMessages buildOtaFlagStagingMessages(
            @NonNull Set<Flag> flagSet, @NonNull String buildFingerprint) {
        StorageRequestMessage.OTAFlagStagingMessage.Builder otaMessageBuilder =
                StorageRequestMessage.OTAFlagStagingMessage.newBuilder()
                        .setBuildId(buildFingerprint);
        for (Flag flag : flagSet) {
            FlagOverride override =
                    FlagOverride.newBuilder()
                            .setPackageName(flag.packageName)
                            .setFlagName(flag.flagName)
                            .setFlagValue(flag.value)
                            .build();
            otaMessageBuilder.addOverrides(override);
        }
        StorageRequestMessage.OTAFlagStagingMessage otaMessage = otaMessageBuilder.build();
        StorageRequestMessage requestMessage =
                StorageRequestMessage.newBuilder().setOtaStagingMessage(otaMessage).build();
        StorageRequestMessages.Builder requestMessagesBuilder = StorageRequestMessages.newBuilder();
        requestMessagesBuilder.addMsgs(requestMessage);
        return requestMessagesBuilder.build();
    }

    private static StorageRequestMessages buildClearFlagOverridesMessages(
            @Nullable Set<Flag> flagSet, RemoveOverrideType removeOverrideType) {
        StorageRequestMessages.Builder requestMessagesBuilder = StorageRequestMessages.newBuilder();

        if (flagSet == null) {
            StorageRequestMessage.RemoveLocalOverrideMessage message =
                    StorageRequestMessage.RemoveLocalOverrideMessage.newBuilder()
                            .setRemoveAll(true)
                            .setRemoveOverrideType(removeOverrideType)
                            .build();
            StorageRequestMessage requestMessage =
                    StorageRequestMessage.newBuilder()
                            .setRemoveLocalOverrideMessage(message)
                            .build();
            requestMessagesBuilder.addMsgs(requestMessage);
            return requestMessagesBuilder.build();
        }

        for (Flag flag : flagSet) {
            StorageRequestMessage.RemoveLocalOverrideMessage message =
                    StorageRequestMessage.RemoveLocalOverrideMessage.newBuilder()
                            .setPackageName(flag.packageName)
                            .setFlagName(flag.flagName)
                            .setRemoveOverrideType(removeOverrideType)
                            .setRemoveAll(false)
                            .build();
            StorageRequestMessage requestMessage =
                    StorageRequestMessage.newBuilder()
                            .setRemoveLocalOverrideMessage(message)
                            .build();
            requestMessagesBuilder.addMsgs(requestMessage);
        }
        return requestMessagesBuilder.build();
    }
}
