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

import android.net.netstats.provider.INetworkStatsProvider;

/**
 * A shim class that allows {@link TestableNetworkStatsProviderBinder} to be built against
 * different SDK versions.
 */
public class NetworkStatsProviderStubCompat extends INetworkStatsProvider.Stub {
    @Override
    public void onRequestStatsUpdate(int token) {}

    // Removed and won't be called in S+.
    public void onSetLimit(String iface, long quotaBytes) {}

    @Override
    public void onSetAlert(long bytes) {}

    // Added in S.
    public void onSetWarningAndLimit(String iface, long warningBytes, long limitBytes) {}
}
