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
 * IkeDefaultNetworkCallback is a network callback used to track the application default Network.
 *
 * <p>This NetworkCallback will notify IkeNetworkUpdater if:
 *
 * <ul>
 *   <li>the default Network changes, or
 *   <li>the local Address for the default Network is dropped,
 *   <li>the default Network dies with no alternatives available.
 * </ul>
 *
 * <p>In the case of default Network changes, the IkeNetworkUpdater will be notified after both
 * onCapabilitiesChanged and onLinkPropertiesChanged are called.
 *
 * <p>MUST be registered with {@link android.net.ConnectivityManager} and specify the
 * IkeSessionStateMachine's Handler to prevent races.
 */
public class IkeDefaultNetworkCallback extends IkeNetworkCallbackBase {
    public IkeDefaultNetworkCallback(
            IkeNetworkUpdater ikeNetworkUpdater,
            Network currNetwork,
            InetAddress currAddress,
            LinkProperties currLp,
            NetworkCapabilities currNc) {
        super(ikeNetworkUpdater, currNetwork, currAddress, currLp, currNc);
    }

    /**
     * This method will always immediately be followed by a call to {@link
     * #onCapabilitiesChanged(Network, NetworkCapabilities)} then by a call to {@link
     * #onLinkPropertiesChanged(Network, LinkProperties)}
     */
    @Override
    public void onAvailable(@NonNull Network network) {
        if (!network.equals(mCurrNetwork)) {
            logd("onAvailable: " + network);
            resetNetwork();
            mCurrNetwork = network;
        }
    }

    /**
     * This method will be called either on the current default network or after {@link
     * #onAvailable(Network)} when a new default network is brought up.
     */
    @Override
    public void onCapabilitiesChanged(
            @NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
        logd("onCapabilitiesChanged: " + network);

        if (!mCurrNetwork.equals(network)) {
            resetNetwork();
            logWtf("onCapabilitiesChanged for new network, without onAvailable being called first");
            return;
        }

        boolean isNcChangedForNewNetwork = isCallbackForNewNetwork();
        mCurrNc = networkCapabilities;
        if (isReadyForUpdate()) {
            if (isNcChangedForNewNetwork) {
                logd("Application default Network changed to " + network);
                mIkeNetworkUpdater.onUnderlyingNetworkUpdated(mCurrNetwork, mCurrLp, mCurrNc);
            } else {
                // Handling onCapabilitiesUpdated does not require IKE Session to support mobility
                mIkeNetworkUpdater.onCapabilitiesUpdated(mCurrNc);
            }
        }
    }

    /**
     * This method will be called either on the current default network or after {@link
     * #onAvailable(Network)} when a new default network is brought up.
     */
    @Override
    public void onLinkPropertiesChanged(
            @NonNull Network network, @NonNull LinkProperties linkProperties) {
        logd("onLinkPropertiesChanged: " + network);

        if (!mCurrNetwork.equals(network)) {
            resetNetwork();
            logWtf(
                    "onLinkPropertiesChanged for new network, without onAvailable being called"
                            + " first");
            return;
        }

        boolean isLpChangedForNewNetwork = isCallbackForNewNetwork();
        mCurrLp = linkProperties;
        if (isReadyForUpdate()) {
            if (isLpChangedForNewNetwork) {
                logd("Application default Network changed to " + network);
                mIkeNetworkUpdater.onUnderlyingNetworkUpdated(mCurrNetwork, mCurrLp, mCurrNc);
            } else if (isCurrentAddressLost(linkProperties)) {
                mIkeNetworkUpdater.onUnderlyingNetworkUpdated(mCurrNetwork, mCurrLp, mCurrNc);
            }
        }
    }

    private void resetNetwork() {
        mCurrNetwork = null;
        mCurrLp = null;
        mCurrNc = null;
        mCurrAddress = null;
    }

    private boolean isCallbackForNewNetwork() {
        return mCurrNetwork != null && (mCurrNc == null || mCurrLp == null);
    }

    private boolean isReadyForUpdate() {
        return mCurrNetwork != null && mCurrNc != null && mCurrLp != null;
    }
}
