/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.server.ethernet;

import android.annotation.Nullable;
import android.net.IpConfiguration;
import android.os.Environment;
import android.util.ArrayMap;

import com.android.server.net.IpConfigStore;


/**
 * This class provides an API to store and manage Ethernet network configuration.
 */
public class EthernetConfigStore {
    private static final String ipConfigFile = Environment.getDataDirectory() +
            "/misc/ethernet/ipconfig.txt";

    private IpConfigStore mStore = new IpConfigStore();
    private ArrayMap<String, IpConfiguration> mIpConfigurations;
    private IpConfiguration mIpConfigurationForDefaultInterface;
    private final Object mSync = new Object();

    public EthernetConfigStore() {
        mIpConfigurations = new ArrayMap<>(0);
    }

    public void read() {
        synchronized (mSync) {
            ArrayMap<String, IpConfiguration> configs =
                    IpConfigStore.readIpConfigurations(ipConfigFile);

            // This configuration may exist in old file versions when there was only a single active
            // Ethernet interface.
            if (configs.containsKey("0")) {
                mIpConfigurationForDefaultInterface = configs.remove("0");
            }

            mIpConfigurations = configs;
        }
    }

    public void write(String iface, IpConfiguration config) {
        boolean modified;

        synchronized (mSync) {
            if (config == null) {
                modified = mIpConfigurations.remove(iface) != null;
            } else {
                IpConfiguration oldConfig = mIpConfigurations.put(iface, config);
                modified = !config.equals(oldConfig);
            }

            if (modified) {
                mStore.writeIpConfigurations(ipConfigFile, mIpConfigurations);
            }
        }
    }

    public ArrayMap<String, IpConfiguration> getIpConfigurations() {
        synchronized (mSync) {
            return new ArrayMap<>(mIpConfigurations);
        }
    }

    @Nullable
    public IpConfiguration getIpConfigurationForDefaultInterface() {
        synchronized (mSync) {
            return mIpConfigurationForDefaultInterface == null
                    ? null : new IpConfiguration(mIpConfigurationForDefaultInterface);
        }
    }
}
