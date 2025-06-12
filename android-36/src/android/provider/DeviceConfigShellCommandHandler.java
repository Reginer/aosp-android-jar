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

package android.provider;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ResultReceiver;

import java.io.IOException;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Class that allows to pass shell commands sent to device_config service
 * (provided in the platform) to the service provided by this mainline module.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class DeviceConfigShellCommandHandler {
    private DeviceConfigShellCommandHandler() {
        // fully static class
    }

    /**
     * Runs a shell command in the service defined in this module.
     *
     * @param inPfd standard input
     * @param outPfd standard output
     * @param errPfd standard error
     * @param args arguments passed to the command. Can be empty. The first argument
     *             is typically a subcommand, such as {@code run} for
     *             {@code adb shell cmd jobscheduler run}.
     * @return the status code returned from the {@code cmd} command.
     */
    static public int handleShellCommand(
            @NonNull ParcelFileDescriptor inPfd,
            @NonNull ParcelFileDescriptor outPfd,
            @NonNull ParcelFileDescriptor errPfd,
            @NonNull String[] args) {
        Binder newBinder = (Binder) DeviceConfigInitializer
                .getDeviceConfigServiceManager()
                .getDeviceConfigUpdatableServiceRegisterer()
                .get();
        return newBinder.handleShellCommand(inPfd, outPfd, errPfd, args);
    }
}
