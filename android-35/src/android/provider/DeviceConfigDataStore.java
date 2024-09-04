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

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.DeviceConfig;

import java.util.concurrent.Executor;
import java.util.Map;

/**
 *  @hide
 */
public interface DeviceConfigDataStore {
    @NonNull Map<String, String> getAllProperties();

    @NonNull DeviceConfig.Properties getProperties(@NonNull String namespace, @NonNull String ... names);

    boolean setProperties(@NonNull DeviceConfig.Properties properties) throws
            DeviceConfig.BadConfigException;

    boolean setProperty(@NonNull String namespace, @NonNull String name,
            @Nullable String value, boolean makeDefault);

    boolean deleteProperty(@NonNull String namespace, @NonNull String name);

    void resetToDefaults(int resetMode, @Nullable String namespace);

    void setSyncDisabledMode(int syncDisabledMode);
    int getSyncDisabledMode();

    void setMonitorCallback(
            @NonNull ContentResolver resolver,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull DeviceConfig.MonitorCallback callback);

    void clearMonitorCallback(@NonNull ContentResolver resolver);

    void registerContentObserver(@NonNull String namespace, boolean notifyForescendants,
            ContentObserver contentObserver);

    void unregisterContentObserver(@NonNull ContentObserver contentObserver);
}
