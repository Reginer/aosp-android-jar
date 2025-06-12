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

package com.android.internal.telephony.metrics;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.telephony.Rlog;

/**
 * Monitors the system default network registration and tracks the currently available
 * {@link Network} and its {@link NetworkCapabilities}.
 */
public class DefaultNetworkMonitor extends Handler {

    private static final String TAG = DefaultNetworkMonitor.class.getSimpleName();

    @Nullable private NetworkCallback mDefaultNetworkCallback;
    @Nullable private Network mNetwork;
    @Nullable private NetworkCapabilities mNetworkCapabilities;

    private final class DefaultNetworkCallback extends NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            mNetwork = network;
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network,
                @NonNull NetworkCapabilities nc) {
            if (network == mNetwork) {
                mNetworkCapabilities = nc;
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            mNetwork = null;
            mNetworkCapabilities = null;
        }
    }

    DefaultNetworkMonitor(@NonNull Context context, @NonNull FeatureFlags featureFlags) {
        super(Looper.myLooper());
        registerSystemDefaultNetworkCallback(context);
    }

    private void registerSystemDefaultNetworkCallback(@NonNull Context context) {
        if (mDefaultNetworkCallback != null) {
            return;
        }
        ConnectivityManager connectivityManager =
                context.getSystemService(ConnectivityManager.class);
        if (connectivityManager != null) {
            mDefaultNetworkCallback = new DefaultNetworkCallback();
            connectivityManager.registerSystemDefaultNetworkCallback(
                    mDefaultNetworkCallback, this);
        } else {
            Rlog.e(TAG, "registerSystemDefaultNetworkCallback: ConnectivityManager is null!");
        }
    }

    @Nullable public NetworkCapabilities getNetworkCapabilities() {
        return mNetworkCapabilities;
    }
}
