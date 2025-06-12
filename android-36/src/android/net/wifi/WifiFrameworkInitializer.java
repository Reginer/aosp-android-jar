/*
 * Copyright (C) 2019 The Android Open Source Project
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
package android.net.wifi;

import android.annotation.SystemApi;
import android.app.SystemServiceRegistry;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.aware.IWifiAwareManager;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.flags.Flags;
import android.net.wifi.p2p.IWifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.rtt.IWifiRttManager;
import android.net.wifi.rtt.WifiRttManager;
import android.net.wifi.usd.IUsdManager;
import android.net.wifi.usd.UsdManager;
import android.net.wifi.util.Environment;

/**
 * Class for performing registration for all Wifi services.
 *
 * @hide
 */
@SystemApi
public class WifiFrameworkInitializer {
    private WifiFrameworkInitializer() {}

    /**
     * Called by {@link SystemServiceRegistry}'s static initializer and registers all Wifi services
     * to {@link Context}, so that {@link Context#getSystemService} can return them.
     *
     * @throws IllegalStateException if this is called from anywhere besides
     * {@link SystemServiceRegistry}
     */
    public static void registerServiceWrappers() {
        SystemServiceRegistry.registerContextAwareService(
                Context.WIFI_SERVICE,
                WifiManager.class,
                (context, serviceBinder) -> {
                    if (!context.getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_WIFI)) {
                        return null;
                    }
                    IWifiManager service = IWifiManager.Stub.asInterface(serviceBinder);
                    return new WifiManager(context, service);
                }
        );
        SystemServiceRegistry.registerContextAwareService(
                Context.WIFI_P2P_SERVICE,
                WifiP2pManager.class,
                (context, serviceBinder) -> {
                    if (!context.getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_WIFI_DIRECT)) {
                        return null;
                    }
                    IWifiP2pManager service = IWifiP2pManager.Stub.asInterface(serviceBinder);
                    return new WifiP2pManager(service);
                }
        );
        SystemServiceRegistry.registerContextAwareService(
                Context.WIFI_AWARE_SERVICE,
                WifiAwareManager.class,
                (context, serviceBinder) -> {
                    if (!context.getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_WIFI_AWARE)) {
                        return null;
                    }
                    IWifiAwareManager service = IWifiAwareManager.Stub.asInterface(serviceBinder);
                    return new WifiAwareManager(context, service);
                }
        );
        SystemServiceRegistry.registerContextAwareService(
                Context.WIFI_SCANNING_SERVICE,
                WifiScanner.class,
                (context, serviceBinder) -> {
                    if (!context.getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_WIFI)) {
                        return null;
                    }
                    IWifiScanner service = IWifiScanner.Stub.asInterface(serviceBinder);
                    return new WifiScanner(context, service);
                }
        );
        SystemServiceRegistry.registerContextAwareService(
                Context.WIFI_RTT_RANGING_SERVICE,
                WifiRttManager.class,
                (context, serviceBinder) -> {
                    if (!context.getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_WIFI_RTT)) {
                        return null;
                    }
                    IWifiRttManager service = IWifiRttManager.Stub.asInterface(serviceBinder);
                    return new WifiRttManager(context, service);
                }
        );
        SystemServiceRegistry.registerContextAwareService(
                Context.WIFI_RTT_SERVICE,
                RttManager.class,
                context -> {
                    if (!context.getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_WIFI_RTT)) {
                        return null;
                    }
                    WifiRttManager wifiRttManager = context.getSystemService(WifiRttManager.class);
                    return new RttManager(context, wifiRttManager);
                }
        );
        if (Flags.usd() && Environment.isSdkAtLeastB()) {
            SystemServiceRegistry.registerContextAwareService(
                    Context.WIFI_USD_SERVICE,
                    UsdManager.class, (context, serviceBinder) -> {
                        if (!context.getResources().getBoolean(
                                context.getResources().getIdentifier("config_deviceSupportsWifiUsd",
                                        "bool", "android"))) {
                            return null;
                        }
                        IUsdManager service = IUsdManager.Stub.asInterface(serviceBinder);
                        return new UsdManager(context, service);
                    }
            );
        }
    }
}
