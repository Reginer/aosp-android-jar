/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.net;

import android.os.Parcelable;
import android.net.ConnectivityMetricsEvent;
import android.net.INetdEventCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

/** {@hide} */
interface IIpConnectivityMetrics {

    /**
     * @return the number of remaining available slots in buffer,
     * or -1 if the event was dropped due to rate limiting.
     */
    int logEvent(in ConnectivityMetricsEvent event);

    void logDefaultNetworkValidity(boolean valid);
    void logDefaultNetworkEvent(in Network defaultNetwork, int score, boolean validated,
            in LinkProperties lp, in NetworkCapabilities nc, in Network previousDefaultNetwork,
            int previousScore, in LinkProperties previousLp, in NetworkCapabilities previousNc);

    /**
     * Callback can be registered by DevicePolicyManager or NetworkWatchlistService only.
     * @return status {@code true} if registering/unregistering of the callback was successful,
     *         {@code false} otherwise (might happen if IIpConnectivityMetrics is not available,
     *         if it happens make sure you call it when the service is up in the caller)
     */
    boolean addNetdEventCallback(in int callerType, in INetdEventCallback callback);
    boolean removeNetdEventCallback(in int callerType);
}
