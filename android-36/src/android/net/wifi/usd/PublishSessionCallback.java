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
 * USD publish session callbacks. Should be extended by applications wanting notifications.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public class PublishSessionCallback extends SessionCallback {

    /**
     * Called when a publish session cannot be created.
     *
     * @param reason reason code as defined in {@code FAILURE_XXX}
     */
    public void onPublishFailed(@FailureCode int reason) {
    }

    /**
     * Called when a publish operation is started successfully.
     *
     * @param session publish session
     */
    public void onPublishStarted(@NonNull PublishSession session) {
    }

    /**
     * Called for each solicited publish transmission if
     * {@link PublishConfig.Builder#setEventsEnabled(boolean)} is enabled.
     */
    public void onPublishReplied(@NonNull DiscoveryResult discoveryResult) {
    }
}
