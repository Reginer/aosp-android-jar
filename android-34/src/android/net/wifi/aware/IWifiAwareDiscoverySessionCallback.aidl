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

package android.net.wifi.aware;

import android.net.wifi.aware.AwarePairingConfig;

/**
 * Callback interface that WifiAwareManager implements
 *
 * {@hide}
 */
oneway interface IWifiAwareDiscoverySessionCallback
{
    void onSessionStarted(int discoverySessionId);
    void onSessionConfigSuccess();
    void onSessionConfigFail(int reason);
    void onSessionTerminated(int reason);
    void onSessionSuspendSucceeded();
    void onSessionSuspendFail(int reason);
    void onSessionResumeSucceeded();
    void onSessionResumeFail(int reason);

    void onMatch(int peerId, in byte[] serviceSpecificInfo, in byte[] matchFilter,
            int peerCipherSuite, in byte[] scid, String pairingAlias,
            in AwarePairingConfig pairingConfig);
    void onMatchWithDistance(int peerId, in byte[] serviceSpecificInfo, in byte[] matchFilter,
            int distanceMm, int peerCipherSuite, in byte[] scid, String pairingAlias,
            in AwarePairingConfig pairingConfig);

    void onMessageSendSuccess(int messageId);
    void onMessageSendFail(int messageId, int reason);
    void onMessageReceived(int peerId, in byte[] message);
    void onMatchExpired(int peerId);
    void onPairingSetupRequestReceived(int peerId, int requestId);
    void onPairingSetupConfirmed(int peerId, boolean accept, String alias);
    void onPairingVerificationConfirmed(int peerId, boolean accept, String alias);
    void onBootstrappingVerificationConfirmed(int peerId, boolean accept, int method);
}
