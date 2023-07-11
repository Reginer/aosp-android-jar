/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.net.wifi.WifiInfo.DEFAULT_MAC_ADDRESS;
import static android.net.wifi.WifiInfo.sanitizeSsid;

import static com.android.wifitrackerlib.Utils.getVerboseLoggingDescription;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.StringJoiner;

/**
 * WifiEntry representation of merged carrier network, uniquely identified by subscription id.
 */
public class MergedCarrierEntry extends WifiEntry {
    static final String KEY_PREFIX = "MergedCarrierEntry:";

    private final int mSubscriptionId;
    @NonNull private final String mKey;
    @NonNull private final Context mContext;
    boolean mIsCellDefaultRoute;

    MergedCarrierEntry(@NonNull Handler callbackHandler,
            @NonNull WifiManager wifiManager,
            boolean forSavedNetworksPage,
            @NonNull Context context,
            int subscriptionId) throws IllegalArgumentException {
        super(callbackHandler, wifiManager, forSavedNetworksPage);
        mContext = context;
        mSubscriptionId = subscriptionId;
        mKey = KEY_PREFIX + subscriptionId;
    }

    @Override
    public String getKey() {
        return mKey;
    }

    @Override
    public String getSummary(boolean concise) {
        StringJoiner sj = new StringJoiner(mContext.getString(
                R.string.wifitrackerlib_summary_separator));
        if (!concise) {
            final String verboseLoggingDescription = getVerboseLoggingDescription(this);
            if (!TextUtils.isEmpty(verboseLoggingDescription)) {
                sj.add(verboseLoggingDescription);
            }
        }
        return sj.toString();
    }

    @Override
    public synchronized String getSsid() {
        if (mWifiInfo != null) {
            return sanitizeSsid(mWifiInfo.getSSID());
        }
        return null;
    }

    @Override
    public synchronized String getMacAddress() {
        if (mWifiInfo != null) {
            final String wifiInfoMac = mWifiInfo.getMacAddress();
            if (!TextUtils.isEmpty(wifiInfoMac)
                    && !TextUtils.equals(wifiInfoMac, DEFAULT_MAC_ADDRESS)) {
                return wifiInfoMac;
            }
        }
        return null;
    }

    @Override
    public synchronized boolean canConnect() {
        return getConnectedState() == CONNECTED_STATE_DISCONNECTED && !mIsCellDefaultRoute;
    }

    /**
     * Connect to this merged carrier network and show the "Wi-Fi won't autoconnect for now" toast.
     * @param callback callback for the connect result
     */
    @Override
    public synchronized void connect(@Nullable ConnectCallback callback) {
        connect(callback, true);
    }

    /**
     * Connect to this merged carrier network.
     * @param callback callback for the connect result
     * @param showToast show the "Wi-Fi won't autoconnect for now" toast if {@code true}
     */
    public synchronized void connect(@Nullable ConnectCallback callback, boolean showToast) {
        mConnectCallback = callback;
        mWifiManager.startRestrictingAutoJoinToSubscriptionId(mSubscriptionId);
        if (showToast) {
            Toast.makeText(mContext, R.string.wifitrackerlib_wifi_wont_autoconnect_for_now,
                    Toast.LENGTH_SHORT).show();
        }
        if (mConnectCallback != null) {
            mCallbackHandler.post(() -> {
                final ConnectCallback connectCallback = mConnectCallback;
                if (connectCallback != null) {
                    connectCallback.onConnectResult(ConnectCallback.CONNECT_STATUS_SUCCESS);
                }
            });
        }
    }

    @Override
    public boolean canDisconnect() {
        return getConnectedState() == CONNECTED_STATE_CONNECTED;
    }

    @Override
    public synchronized void disconnect(@Nullable DisconnectCallback callback) {
        mDisconnectCallback = callback;
        mWifiManager.stopRestrictingAutoJoinToSubscriptionId();
        mWifiManager.startScan();
        if (mDisconnectCallback != null) {
            mCallbackHandler.post(() -> {
                final DisconnectCallback disconnectCallback = mDisconnectCallback;
                if (disconnectCallback != null) {
                    disconnectCallback.onDisconnectResult(
                            DisconnectCallback.DISCONNECT_STATUS_SUCCESS);
                }
            });
        }
    }

    @WorkerThread
    protected boolean connectionInfoMatches(@NonNull WifiInfo wifiInfo,
            @NonNull NetworkInfo networkInfo) {
        return wifiInfo.isCarrierMerged() && mSubscriptionId == wifiInfo.getSubscriptionId();
    }

    /** Returns whether or not carrier network offload is enabled for this subscription **/
    public boolean isEnabled() {
        return mWifiManager.isCarrierNetworkOffloadEnabled(mSubscriptionId, true);
    }

    /** Enables/disables the carrier network */
    public void setEnabled(boolean enabled) {
        mWifiManager.setCarrierNetworkOffloadEnabled(mSubscriptionId, true, enabled);
        if (!enabled) {
            mWifiManager.stopRestrictingAutoJoinToSubscriptionId();
            mWifiManager.startScan();
        }
    }

    /* package */ int getSubscriptionId() {
        return mSubscriptionId;
    }

    /* package */ synchronized void updateIsCellDefaultRoute(boolean isCellDefaultRoute) {
        mIsCellDefaultRoute = isCellDefaultRoute;
        notifyOnUpdated();
    }
}
