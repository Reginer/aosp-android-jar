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

package com.android.wifitrackerlib;

import static android.os.Build.VERSION_CODES;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.sharedconnectivity.app.HotspotNetwork;
import android.net.wifi.sharedconnectivity.app.HotspotNetworkConnectionStatus;
import android.net.wifi.sharedconnectivity.app.NetworkProviderInfo;
import android.net.wifi.sharedconnectivity.app.SharedConnectivityManager;
import android.os.Handler;
import android.text.BidiFormatter;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Objects;

/**
 * WifiEntry representation of a Hotspot Network provided via {@link SharedConnectivityManager}.
 */
@TargetApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
public class HotspotNetworkEntry extends WifiEntry {
    static final String TAG = "HotspotNetworkEntry";
    public static final String KEY_PREFIX = "HotspotNetworkEntry:";

    @NonNull private final WifiTrackerInjector mInjector;
    @NonNull private final Context mContext;
    @Nullable private final SharedConnectivityManager mSharedConnectivityManager;

    @Nullable private HotspotNetwork mHotspotNetworkData;
    @NonNull private HotspotNetworkEntryKey mKey;

    private boolean mServerInitiatedConnection = false;

    /**
     * If editing this IntDef also edit the definition in:
     * {@link android.net.wifi.sharedconnectivity.app.HotspotNetwork}
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            HotspotNetwork.NETWORK_TYPE_UNKNOWN,
            HotspotNetwork.NETWORK_TYPE_CELLULAR,
            HotspotNetwork.NETWORK_TYPE_WIFI,
            HotspotNetwork.NETWORK_TYPE_ETHERNET
    })
    public @interface NetworkType {} // TODO(b/271868642): Add IfThisThanThat lint

    /**
     * If editing this IntDef also edit the definition in:
     * {@link android.net.wifi.sharedconnectivity.app.NetworkProviderInfo}
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            NetworkProviderInfo.DEVICE_TYPE_UNKNOWN,
            NetworkProviderInfo.DEVICE_TYPE_PHONE,
            NetworkProviderInfo.DEVICE_TYPE_TABLET,
            NetworkProviderInfo.DEVICE_TYPE_LAPTOP,
            NetworkProviderInfo.DEVICE_TYPE_WATCH,
            NetworkProviderInfo.DEVICE_TYPE_AUTO
    })
    public @interface DeviceType {} // TODO(b/271868642): Add IfThisThanThat lint

    /**
     * If editing this IntDef also edit the definition in:
     * {@link android.net.wifi.sharedconnectivity.app.HotspotNetworkConnectionStatus}
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_UNKNOWN,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_ENABLING_HOTSPOT,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_UNKNOWN_ERROR,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_PROVISIONING_FAILED,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_TETHERING_TIMEOUT,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_TETHERING_UNSUPPORTED,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_NO_CELL_DATA,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_ENABLING_HOTSPOT_FAILED,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_ENABLING_HOTSPOT_TIMEOUT,
            HotspotNetworkConnectionStatus.CONNECTION_STATUS_CONNECT_TO_HOTSPOT_FAILED,
    })
    public @interface ConnectionStatus {} // TODO(b/271868642): Add IfThisThanThat lint

    /**
     * Create a HotspotNetworkEntry from HotspotNetwork data.
     */
    HotspotNetworkEntry(
            @NonNull WifiTrackerInjector injector,
            @NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull WifiManager wifiManager,
            @Nullable SharedConnectivityManager sharedConnectivityManager,
            @NonNull HotspotNetwork hotspotNetworkData) {
        super(callbackHandler, wifiManager, false /*forSavedNetworksPage*/);
        mInjector = injector;
        mContext = context;
        mSharedConnectivityManager = sharedConnectivityManager;
        mHotspotNetworkData = hotspotNetworkData;
        mKey = new HotspotNetworkEntryKey(hotspotNetworkData);
    }

    /**
     * Create a HotspotNetworkEntry from HotspotNetworkEntryKey.
     */
    HotspotNetworkEntry(
            @NonNull WifiTrackerInjector injector,
            @NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull WifiManager wifiManager,
            @Nullable SharedConnectivityManager sharedConnectivityManager,
            @NonNull HotspotNetworkEntryKey key) {
        super(callbackHandler, wifiManager, false /*forSavedNetworksPage*/);
        mInjector = injector;
        mContext = context;
        mSharedConnectivityManager = sharedConnectivityManager;
        mHotspotNetworkData = null;
        mKey = key;
    }

    @Override
    public String getKey() {
        return mKey.toString();
    }

    public HotspotNetworkEntryKey getHotspotNetworkEntryKey() {
        return mKey;
    }

    /**
     * Updates the hotspot data for this entry. Creates a new key when called.
     *
     * @param hotspotNetworkData An updated data set from SharedConnectivityService.
     */
    @WorkerThread
    protected synchronized void updateHotspotNetworkData(
            @NonNull HotspotNetwork hotspotNetworkData) {
        mHotspotNetworkData = hotspotNetworkData;
        mKey = new HotspotNetworkEntryKey(hotspotNetworkData);
        notifyOnUpdated();
    }

    @WorkerThread
    protected synchronized boolean connectionInfoMatches(@NonNull WifiInfo wifiInfo) {
        if (mKey.isVirtualEntry()) {
            return false;
        }
        return Objects.equals(mKey.getBssid(), wifiInfo.getBSSID());
    }

    @Override
    public String getTitle() {
        if (mHotspotNetworkData == null) {
            return "";
        }
        return mHotspotNetworkData.getNetworkProviderInfo().getDeviceName();
    }

    @Override
    public String getSummary(boolean concise) {
        if (mHotspotNetworkData == null) {
            return "";
        }
        if (getConnectedState() != CONNECTED_STATE_CONNECTED && mServerInitiatedConnection) {
            return mContext.getString(R.string.wifitrackerlib_hotspot_network_connecting);
        }
        return mContext.getString(R.string.wifitrackerlib_hotspot_network_summary,
                BidiFormatter.getInstance().unicodeWrap(mHotspotNetworkData.getNetworkName()),
                BidiFormatter.getInstance().unicodeWrap(
                        mHotspotNetworkData.getNetworkProviderInfo().getModelName()));
    }

    /**
     * Alternate summary string to be used on Network & internet page.
     *
     * @return Display string.
     */
    public String getAlternateSummary() {
        if (mHotspotNetworkData == null) {
            return "";
        }
        return mContext.getString(R.string.wifitrackerlib_hotspot_network_alternate,
                BidiFormatter.getInstance().unicodeWrap(mHotspotNetworkData.getNetworkName()),
                BidiFormatter.getInstance().unicodeWrap(
                        mHotspotNetworkData.getNetworkProviderInfo().getDeviceName()));
    }

    /**
     * Connection strength between the host device and the internet.
     *
     * @return Displayed connection strength in the range 0 to 4.
     */
    @IntRange(from = 0, to = 4)
    public int getUpstreamConnectionStrength() {
        if (mHotspotNetworkData == null) {
            return 0;
        }
        return mHotspotNetworkData.getNetworkProviderInfo().getConnectionStrength();
    }

    /**
     * Network type used by the host device to connect to the internet.
     *
     * @return NetworkType enum.
     */
    @NetworkType
    public int getNetworkType() {
        if (mHotspotNetworkData == null) {
            return HotspotNetwork.NETWORK_TYPE_UNKNOWN;
        }
        return mHotspotNetworkData.getHostNetworkType();
    }

    /**
     * Device type of the host device.
     *
     * @return DeviceType enum.
     */
    @DeviceType
    public int getDeviceType() {
        if (mHotspotNetworkData == null) {
            return NetworkProviderInfo.DEVICE_TYPE_UNKNOWN;
        }
        return mHotspotNetworkData.getNetworkProviderInfo().getDeviceType();
    }

    /**
     * The battery percentage of the host device.
     */
    @IntRange(from = 0, to = 100)
    public int getBatteryPercentage() {
        if (mHotspotNetworkData == null) {
            return 0;
        }
        return mHotspotNetworkData.getNetworkProviderInfo().getBatteryPercentage();
    }

    /**
     * If the host device is currently charging its battery.
     */
    public boolean isBatteryCharging() {
        if (mHotspotNetworkData == null) {
            return false;
        }
        return mHotspotNetworkData.getExtras().getBoolean("is_battery_charging", false);
    }

    @Override
    public boolean canConnect() {
        return getConnectedState() == CONNECTED_STATE_DISCONNECTED;
    }

    @Override
    public void connect(@Nullable ConnectCallback callback) {
        mConnectCallback = callback;
        if (mSharedConnectivityManager == null) {
            if (callback != null) {
                mCallbackHandler.post(() -> callback.onConnectResult(
                        ConnectCallback.CONNECT_STATUS_FAILURE_UNKNOWN));
            }
            return;
        }
        mSharedConnectivityManager.connectHotspotNetwork(mHotspotNetworkData);
    }

    @Override
    public boolean canDisconnect() {
        return getConnectedState() != CONNECTED_STATE_DISCONNECTED;
    }

    @Override
    public void disconnect(@Nullable DisconnectCallback callback) {
        mDisconnectCallback = callback;
        if (mSharedConnectivityManager == null) {
            if (callback != null) {
                mCallbackHandler.post(() -> callback.onDisconnectResult(
                        DisconnectCallback.DISCONNECT_STATUS_FAILURE_UNKNOWN));
            }
            return;
        }
        mSharedConnectivityManager.disconnectHotspotNetwork(mHotspotNetworkData);
    }

    @Nullable
    public HotspotNetwork getHotspotNetworkData() {
        return mHotspotNetworkData;
    }

    /**
     * Trigger ConnectCallback with data from SharedConnectivityService.
     * @param status HotspotNetworkConnectionStatus#ConnectionStatus enum.
     */
    public void onConnectionStatusChanged(@ConnectionStatus int status) {
        if (mConnectCallback == null) return;
        switch (status) {
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_ENABLING_HOTSPOT:
                mServerInitiatedConnection = true;
                notifyOnUpdated();
                break;
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_UNKNOWN_ERROR:
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_PROVISIONING_FAILED:
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_TETHERING_TIMEOUT:
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_TETHERING_UNSUPPORTED:
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_NO_CELL_DATA:
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_ENABLING_HOTSPOT_FAILED:
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_ENABLING_HOTSPOT_TIMEOUT:
            case HotspotNetworkConnectionStatus.CONNECTION_STATUS_CONNECT_TO_HOTSPOT_FAILED:
                mCallbackHandler.post(() -> mConnectCallback.onConnectResult(
                        ConnectCallback.CONNECT_STATUS_FAILURE_UNKNOWN));
                mServerInitiatedConnection = false;
                notifyOnUpdated();
                break;
            default:
                // Do nothing
        }
    }

    static class HotspotNetworkEntryKey {
        private static final String KEY_IS_VIRTUAL_ENTRY_KEY = "IS_VIRTUAL_ENTRY_KEY";
        private static final String KEY_DEVICE_ID_KEY = "DEVICE_ID_KEY";
        private static final String KEY_BSSID_KEY = "BSSID_KEY";
        private static final String KEY_SCAN_RESULT_KEY = "SCAN_RESULT_KEY";

        private boolean mIsVirtualEntry;
        private long mDeviceId;
        @Nullable
        private String mBssid;
        @Nullable
        private StandardWifiEntry.ScanResultKey mScanResultKey;

        /**
         * Creates a HotspotNetworkEntryKey based on a {@link HotspotNetwork} parcelable object.
         *
         * @param hotspotNetworkData A {@link HotspotNetwork} object from SharedConnectivityService.
         */
        HotspotNetworkEntryKey(@NonNull HotspotNetwork hotspotNetworkData) {
            mDeviceId = hotspotNetworkData.getDeviceId();
            if (hotspotNetworkData.getHotspotSsid() == null
                    || (hotspotNetworkData.getHotspotBssid() == null)
                    || (hotspotNetworkData.getHotspotSecurityTypes() == null)) {
                mIsVirtualEntry = true;
                mBssid = null;
                mScanResultKey = null;
            } else {
                mIsVirtualEntry = false;
                mBssid = hotspotNetworkData.getHotspotBssid();
                mScanResultKey = new StandardWifiEntry.ScanResultKey(
                        hotspotNetworkData.getHotspotSsid(),
                        new ArrayList<>(hotspotNetworkData.getHotspotSecurityTypes()));
            }
        }

        /**
         * Creates a HotspotNetworkEntryKey from its String representation.
         */
        HotspotNetworkEntryKey(@NonNull String string) {
            mScanResultKey = null;
            if (!string.startsWith(KEY_PREFIX)) {
                Log.e(TAG, "String key does not start with key prefix!");
                return;
            }
            try {
                final JSONObject keyJson = new JSONObject(
                        string.substring(KEY_PREFIX.length()));
                if (keyJson.has(KEY_IS_VIRTUAL_ENTRY_KEY)) {
                    mIsVirtualEntry = keyJson.getBoolean(KEY_IS_VIRTUAL_ENTRY_KEY);
                }
                if (keyJson.has(KEY_DEVICE_ID_KEY)) {
                    mDeviceId = keyJson.getLong(KEY_DEVICE_ID_KEY);
                }
                if (keyJson.has(KEY_BSSID_KEY)) {
                    mBssid = keyJson.getString(KEY_BSSID_KEY);
                }
                if (keyJson.has(KEY_SCAN_RESULT_KEY)) {
                    mScanResultKey = new StandardWifiEntry.ScanResultKey(keyJson.getString(
                            KEY_SCAN_RESULT_KEY));
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException while converting HotspotNetworkEntryKey to string: " + e);
            }
        }

        /**
         * Returns the JSON String representation of this HotspotNetworkEntryKey.
         */
        @Override
        public String toString() {
            final JSONObject keyJson = new JSONObject();
            try {
                keyJson.put(KEY_IS_VIRTUAL_ENTRY_KEY, mIsVirtualEntry);
                keyJson.put(KEY_DEVICE_ID_KEY, mDeviceId);
                if (mBssid != null) {
                    keyJson.put(KEY_BSSID_KEY, mBssid);
                }
                if (mScanResultKey != null) {
                    keyJson.put(KEY_SCAN_RESULT_KEY, mScanResultKey.toString());
                }
            } catch (JSONException e) {
                Log.wtf(TAG,
                        "JSONException while converting HotspotNetworkEntryKey to string: " + e);
            }
            return KEY_PREFIX + keyJson.toString();
        }

        public boolean isVirtualEntry() {
            return mIsVirtualEntry;
        }

        public long getDeviceId() {
            return mDeviceId;
        }

        /**
         * Returns the BSSID of this HotspotNetworkEntryKey to match against wifiInfo
         */
        @Nullable
        String getBssid() {
            return mBssid;
        }

        /**
         * Returns the ScanResultKey of this HotspotNetworkEntryKey to match against ScanResults
         */
        @Nullable
        StandardWifiEntry.ScanResultKey getScanResultKey() {
            return mScanResultKey;
        }
    }
}
