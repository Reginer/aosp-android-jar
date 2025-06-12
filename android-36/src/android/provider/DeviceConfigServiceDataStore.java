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
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.IBinder;
import android.provider.aidl.IDeviceConfigManager;
import android.util.Slog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executor;

/**
 * @hide
 */
public class DeviceConfigServiceDataStore /*implements DeviceConfigDataStore*/ {

    private static final boolean DEBUG = false;
    private static final String TAG = "DeviceConfigServiceDataStore";
    private static final Object mLock = new Object();

    // TODO(b/265948914): finish implementation of this data store and make it actually implement
    // the interface
    private DeviceConfigManager mManager;

    public DeviceConfigServiceDataStore() {

    }

    public DeviceConfigServiceDataStore(DeviceConfigManager deviceConfigManager) {
        mManager = deviceConfigManager;
    }

    private @Nullable DeviceConfigManager createManagerIfNeeded() {
        if (mManager != null) {
            return mManager;
        }
        synchronized (mLock) {
            if (mManager != null) {
                return mManager;
            }

            IBinder binder = DeviceConfigInitializer.getDeviceConfigServiceManager()
                    .getDeviceConfigUpdatableServiceRegisterer()
                    .get();

            if (binder != null) {
                IDeviceConfigManager manager = IDeviceConfigManager.Stub.asInterface(
                        binder);
                mManager = new DeviceConfigManager(manager);
            }
            return mManager;
        }
    }

//    @Override
    @NonNull
    public DeviceConfig.Properties getProperties(@NonNull String namespace,
          @NonNull String... names) {
       createManagerIfNeeded();

       if (mManager == null) {
           if(DEBUG) {
               Slog.d(TAG, "ServiceDS - getProperties before ready " + namespace + " "
                       + Arrays.toString(names));
           }
          return new DeviceConfig.Properties(namespace, new HashMap<>());
       }
        return mManager.getProperties(namespace, names);
    }

//    @Override
   public boolean setProperties(@NonNull DeviceConfig.Properties properties)
         throws DeviceConfig.BadConfigException {
      createManagerIfNeeded();
      if (mManager == null) {
          if(DEBUG) {
              Slog.d(TAG, "ServiceDS - setProperties before ready " + properties.getNamespace() + " " + properties);
          }
         return false;
      }
       return mManager.setProperties(properties.getNamespace(), properties.getPropertyValues());
   }
//
//    @Override
    public boolean setProperty(@NonNull String namespace, @NonNull String name,
          @Nullable String value, boolean makeDefault) {
       createManagerIfNeeded();
       if (mManager == null) {
           if (DEBUG) {
               Slog.d(TAG, "ServiceDS - setProperty before ready " + namespace + " " + name);
           }
          return false;
       }
        return mManager.setProperty(namespace, name, value, makeDefault);
    }
//
//    @Override
   public boolean deleteProperty(@NonNull String namespace, @NonNull String name) {
       createManagerIfNeeded();
       if (mManager == null) {
           if (DEBUG) {
               Slog.d(TAG, "ServiceDS - setProperty before ready " + namespace + " " + name);
           }
           return false;
       }
       return mManager.deleteProperty(namespace, name);
   }
//
//    @Override
   public void resetToDefaults(int resetMode, @Nullable String namespace) {
//        mManager.resetToDefaults(resetMode, namespace);
   }
//
//    @Override
   public void setSyncDisabledMode(int syncDisabledMode) {
//        mManager.setSyncDisabledMode(syncDisabledMode);
   }
//
//    @Override
   public int getSyncDisabledMode() {
       //        return mManager.getSyncDisabledMode();
       return 0;
   }
//
//    @Override
   public void setMonitorCallback(@NonNull ContentResolver resolver, @NonNull Executor executor,
           @NonNull DeviceConfig.MonitorCallback callback) {
//        mManager.setMonitorCallback(resolver, executor, callback);
   }
//
//    @Override
   public void clearMonitorCallback(@NonNull ContentResolver resolver) {
//        mManager.clearMonitorCallback(resolver);
   }
//
//    @Override
   public void registerContentObserver(@NonNull String namespace, boolean notifyForDescendants,
           ContentObserver contentObserver) {
//        mManager.registerContentObserver(namespace, notifyForescendants, contentObserver);
   }
//
//    @Override
   public void unregisterContentObserver(@NonNull ContentObserver contentObserver) {
//        mManager.unregisterContentObserver(contentObserver);
   }
}
