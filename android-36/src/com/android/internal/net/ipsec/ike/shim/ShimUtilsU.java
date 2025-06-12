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

package com.android.internal.net.ipsec.ike.shim;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Network;
import android.net.SocketKeepalive;

/** Shim utilities for SDK U and above */
public class ShimUtilsU extends ShimUtilsT {
    // Package protected constructor for ShimUtils to access
    ShimUtilsU() {
        super();
    }

    @Override
    public void startKeepalive(SocketKeepalive keepalive, int keepaliveDelaySeconds,
            int keepaliveOptions, Network underpinnedNetwork) {
        keepalive.start(keepaliveDelaySeconds, keepaliveOptions, underpinnedNetwork);
    }

    @Override
    public boolean shouldSkipIfSameNetwork(boolean skipIfSameNetwork) {
        return skipIfSameNetwork;
    }

    @Override
    public boolean supportsSameSocketKernelMigration(Context context) {
        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_IPSEC_TUNNEL_MIGRATION);
    }
}
