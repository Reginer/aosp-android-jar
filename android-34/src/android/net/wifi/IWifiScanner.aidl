/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Messenger;
import android.os.Bundle;
import android.os.WorkSource;
import android.net.wifi.WifiScanner;
import android.net.wifi.ScanResult;
import android.net.wifi.IWifiScannerListener;

/**
 * {@hide}
 */
interface IWifiScanner
{
    Bundle getAvailableChannels(int band, String packageName, String featureId, in Bundle extras);

    boolean isScanning();

    boolean setScanningEnabled(boolean enable, int tid, String packageName);

    void registerScanListener(in IWifiScannerListener listener, String packageName,
            String featureId);

    void unregisterScanListener(in IWifiScannerListener listener, String packageName,
           String featureId);

    void startBackgroundScan(in IWifiScannerListener listener,
            in WifiScanner.ScanSettings settings, in WorkSource workSource,
            String packageName, String featureId);

    void stopBackgroundScan(in IWifiScannerListener listener, String packageName, String featureId);

    boolean getScanResults(String packageName, String featureId);

    void startScan(in IWifiScannerListener listener,
            in WifiScanner.ScanSettings settings, in WorkSource workSource,
            String packageName, String featureId);

    void stopScan(in IWifiScannerListener listener, String packageName, String featureId);

    List<ScanResult> getSingleScanResults(String packageName, String featureId);

    void startPnoScan(in IWifiScannerListener listener,
            in WifiScanner.ScanSettings scanSettings,
            in WifiScanner.PnoSettings pnoSettings,
            String packageName, String featureId);

    void stopPnoScan(in IWifiScannerListener listener, String packageName, String featureId);

    void enableVerboseLogging(boolean enabled);
}
