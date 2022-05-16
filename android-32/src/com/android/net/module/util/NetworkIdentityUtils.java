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

package com.android.net.module.util;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * Utilities to examine {@link android.net.NetworkIdentity}.
 * @hide
 */
public class NetworkIdentityUtils {
    /**
     * Scrub given IMSI on production builds.
     */
    @NonNull
    public static String scrubSubscriberId(@Nullable String subscriberId) {
        if (subscriberId != null) {
            // TODO: parse this as MCC+MNC instead of hard-coding
            return subscriberId.substring(0, Math.min(6, subscriberId.length())) + "...";
        } else {
            return "null";
        }
    }

    /**
     * Scrub given IMSI on production builds.
     */
    @Nullable
    public static String[] scrubSubscriberIds(@Nullable String[] subscriberIds) {
        if (subscriberIds == null) return null;
        final String[] res = new String[subscriberIds.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = scrubSubscriberId(subscriberIds[i]);
        }
        return res;
    }
}
