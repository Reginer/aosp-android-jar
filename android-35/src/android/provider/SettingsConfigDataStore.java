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

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.ContentObserver;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.HashMap;
import java.util.Map;


/**
 * TODO: want to change the package of this class
 *
 * @hide
 */
public class SettingsConfigDataStore implements DeviceConfigDataStore {
    @Override
    public @NonNull Map<String, String> getAllProperties() {
        return Settings.Config.getAllStrings();
    }

    @Override
    public @NonNull DeviceConfig.Properties getProperties(@NonNull String namespace,
            @NonNull String... names) {
        return new DeviceConfig.Properties(namespace,
                Settings.Config.getStrings(namespace, Arrays.asList(names)));
    }

    @Override
    public boolean setProperties(@NonNull DeviceConfig.Properties properties)
            throws DeviceConfig.BadConfigException {
        return Settings.Config.setStrings(properties.getNamespace(),
                properties.getPropertyValues());
    }

    @Override
    public boolean setProperty(@NonNull String namespace, @NonNull String name,
            @Nullable String value, boolean makeDefault) {
        return Settings.Config.putString(namespace, name, value, makeDefault);
    }

    @Override
    public boolean deleteProperty(@NonNull String namespace, @NonNull String name) {
        return Settings.Config.deleteString(namespace, name);
    }

    @Override
    public void resetToDefaults(int resetMode, @Nullable String namespace) {
        Settings.Config.resetToDefaults(resetMode, namespace);
    }

    @Override
    public void setSyncDisabledMode(int syncDisabledMode) {
        Settings.Config.setSyncDisabledMode(syncDisabledMode);
    }

    @Override
    public int getSyncDisabledMode() {
        return Settings.Config.getSyncDisabledMode();
    }

    @Override
    public void setMonitorCallback(@NonNull ContentResolver resolver, @NonNull Executor executor,
            @NonNull DeviceConfig.MonitorCallback callback) {
        Settings.Config.setMonitorCallback(resolver, executor, callback);
    }

    @Override
    public void clearMonitorCallback(@NonNull ContentResolver resolver) {
        Settings.Config.clearMonitorCallback(resolver);
    }

    @Override
    public void registerContentObserver(@NonNull String namespace, boolean notifyForescendants,
            ContentObserver contentObserver) {
        Settings.Config.registerContentObserver(namespace, true, contentObserver);
    }

    @Override
    public void unregisterContentObserver(@NonNull ContentObserver contentObserver) {
        Settings.Config.unregisterContentObserver(contentObserver);
    }
}
