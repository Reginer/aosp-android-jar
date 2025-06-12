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
import android.annotation.Nullable;

import android.annotation.SystemService;
import android.os.RemoteException;
import android.provider.aidl.IDeviceConfigManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@SystemService(DeviceConfig.SERVICE_NAME)
public class DeviceConfigManager {

    private IDeviceConfigManager mService;

    public DeviceConfigManager(@NonNull IDeviceConfigManager service) {
        mService = service;
    }

    @NonNull
    public DeviceConfig.Properties getProperties(@NonNull String namespace,
            @NonNull String... names) {
        try {
            Map<String, String> map = mService.getProperties(namespace, names);
            return new DeviceConfig.Properties(namespace, map);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setProperties(@NonNull String namespace, @NonNull Map<String, String> values) {
        try {
            return mService.setProperties(namespace, values);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setProperty(@NonNull String namespace, @NonNull String name,
            @Nullable String value, boolean makeDefault) {
        try {
            return mService.setProperty(namespace, name, value, makeDefault);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean deleteProperty(@NonNull String namespace, @NonNull String name) {
        try {
            return mService.deleteProperty(namespace, name);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
