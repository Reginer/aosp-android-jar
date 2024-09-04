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

package com.android.internal.net.ipsec.ike.net;

import android.annotation.NonNull;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.net.InetAddress;

/**
 * IkeSpecificNetworkCallback is a network callback used to track a caller-specified network.
 *
 * <p>This NetworkCallback will notify IkeSessionStateMachine if:
 *
 * <ul>
 *   <li>the local Address for the caller-specified Network is dropped, or
 *   <li>the caller-specified Network dies.
 * </ul>
 *
 * <p>MUST be registered with {@link android.net.ConnectivityManager} and specify the
 * IkeSessionStateMachine's Handler to prevent races.
 */
public class IkeSpecificNetworkCallback extends IkeNetworkCallbackBase {
    public IkeSpecificNetworkCallback(
            IkeNetworkUpdater ikeNetworkUpdater,
            Network currNetwork,
            InetAddress currAddress,
            LinkProperties currLp,
            NetworkCapabilities currNc) {
        super(ikeNetworkUpdater, currNetwork, currAddress, currLp, currNc);
    }

    @Override
    public void onCapabilitiesChanged(
            @NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
        // This LinkProperties update is only meaningful if it's for the current Network
        if (!mCurrNetwork.equals(network)) {
            return;
        }

        logd("onCapabilitiesChanged: " + network + "networkCapabilities " + networkCapabilities);
        mCurrNc = networkCapabilities;
        mIkeNetworkUpdater.onCapabilitiesUpdated(mCurrNc);
    }

    @Override
    public void onLinkPropertiesChanged(
            @NonNull Network network, @NonNull LinkProperties linkProperties) {
        // This LinkProperties update is only meaningful if it's for the current Network
        if (!mCurrNetwork.equals(network)) {
            return;
        }

        logd("onLinkPropertiesChanged: " + network);
        mCurrLp = linkProperties;
        if (isCurrentAddressLost(linkProperties)) {
            mIkeNetworkUpdater.onUnderlyingNetworkUpdated(mCurrNetwork, mCurrLp, mCurrNc);
        }
    }
}
