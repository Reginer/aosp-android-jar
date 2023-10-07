/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.testutils;

import android.net.NetworkStats;
import android.net.netstats.provider.INetworkStatsProviderCallback;
import android.os.RemoteException;

/**
 * A shim class that allows {@link TestableNetworkStatsProviderCbBinder} to be built against
 * different SDK versions.
 */
public class NetworkStatsProviderCbStubCompat extends INetworkStatsProviderCallback.Stub {
    @Override
    public void notifyStatsUpdated(int token, NetworkStats ifaceStats, NetworkStats uidStats)
            throws RemoteException {}

    @Override
    public void notifyAlertReached() throws RemoteException {}

    /** Added in T. */
    public void notifyLimitReached() throws RemoteException {}

    /** Added in T. */
    public void notifyWarningReached() throws RemoteException {}

    /** Added in S, removed in T. */
    public void notifyWarningOrLimitReached() throws RemoteException {}

    @Override
    public void unregister() throws RemoteException {}
}
