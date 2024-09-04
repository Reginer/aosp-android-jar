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


package com.android.internal.telephony.configupdate;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.concurrent.Executor;

public interface ConfigProviderAdaptor {

    String DOMAIN_SATELLITE = "satellite";

    /**
     * Get the config from the provider
     */
    @Nullable ConfigParser getConfigParser(String domain);

    class Callback {
        /**
         * The callback when the config is changed
         * @param config the config is changed
         */
        public void onChanged(@Nullable ConfigParser config) {}
    }

    /**
     * Register the callback to monitor the config change
     * @param executor The executor to execute the callback.
     * @param callback the callback to monitor the config change
     */
    void registerCallback(@NonNull Executor executor, @NonNull Callback callback);

    /**
     * Unregister the callback
     * @param callback the callback to be unregistered
     */
    void unregisterCallback(@NonNull Callback callback);
}
