/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.net.wifi.usd;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.net.wifi.flags.Flags;
import android.os.Build;


/**
 * USD subscribe session callbacks. Should be extended by applications wanting notifications.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public class SubscribeSessionCallback extends SessionCallback {

    /**
     * Called when a subscribe session cannot be created.
     *
     * @param reason reason code as defined in {@code FAILURE_XXX}
     */
    public void onSubscribeFailed(@FailureCode int reason) {
    }

    /**
     * Called when a subscribe operation is started successfully.
     *
     * @param session subscribe session
     */
    public void onSubscribeStarted(@NonNull SubscribeSession session) {
    }

    /**
     * Called when a subscribe operation results in a service discovery.
     *
     * @param discoveryResult A structure containing information of the discovery session and
     *                        discovered peer
     */
    public void onServiceDiscovered(@NonNull DiscoveryResult discoveryResult) {
    }
}
