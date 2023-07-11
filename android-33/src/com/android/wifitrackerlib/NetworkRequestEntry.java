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

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;


/**
 * WifiEntry representation of network requested through the NetworkRequest API,
 * uniquely identified by SSID, security, and profile key.
 */
@VisibleForTesting
public class NetworkRequestEntry extends StandardWifiEntry {
    // TODO(b/187073621): This isn't used but is still referenced by clients of WifiTrackerLib.
    //                    Remove once String keys are removed entirely.
    public static final String KEY_PREFIX = "NetworkRequestEntry:";

    NetworkRequestEntry(
            @NonNull WifiTrackerInjector injector,
            @NonNull Context context, @NonNull Handler callbackHandler,
            @NonNull StandardWifiEntryKey key, @NonNull WifiManager wifiManager,
            boolean forSavedNetworksPage) throws IllegalArgumentException {
        super(injector, context, callbackHandler, key, wifiManager, forSavedNetworksPage);
    }

    @Override
    public boolean isMetered() {
        return false;
    }

    @Override
    public boolean isSaved() {
        return false;
    }

    @Override
    public boolean isSuggestion() {
        return false;
    }

    @Override
    public boolean isSubscription() {
        return false;
    }

    @Override
    public WifiConfiguration getWifiConfiguration() {
        return null;
    }

    @Override
    public boolean canConnect() {
        return false;
    }

    @Override
    public void connect(@Nullable ConnectCallback callback) {
        return;
    }

    @Override
    public boolean canForget() {
        return false;
    }

    @Override
    public void forget(@Nullable ForgetCallback callback) {
        return;
    }

    @Override
    public boolean canShare() {
        return false;
    }

    @Override
    public boolean canEasyConnect() {
        return false;
    }

    @Override
    @MeteredChoice
    public int getMeteredChoice() {
        return METERED_CHOICE_AUTO;
    }

    @Override
    public boolean canSetMeteredChoice() {
        return false;
    }

    @Override
    public void setMeteredChoice(int meteredChoice) {
        return;
    }

    @Override
    public boolean canSetPrivacy() {
        return false;
    }

    @Override
    @Privacy
    public int getPrivacy() {
        return PRIVACY_RANDOMIZED_MAC;
    }

    @Override
    public void setPrivacy(int privacy) {
        return;
    }

    @Override
    public boolean isAutoJoinEnabled() {
        return true;
    }

    @Override
    public boolean canSetAutoJoinEnabled() {
        return false;
    }

    @Override
    public void setAutoJoinEnabled(boolean enabled) {
        return;
    }
}
