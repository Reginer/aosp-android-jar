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

package android.net.nsd;

import android.annotation.FlaggedApi;
import android.annotation.LongDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * OffloadEngine is an interface for mDns hardware offloading.
 *
 * An offloading engine can interact with the firmware code to instruct the hardware to
 * offload some of mDns network traffic before it reached android OS. This can improve the
 * power consumption performance of the host system by not always waking up the OS to handle
 * the mDns packet when the device is in low power mode.
 *
 * @hide
 */
@FlaggedApi("com.android.net.flags.register_nsd_offload_engine_api")
@SystemApi
public interface OffloadEngine {
    /**
     * Indicates that the OffloadEngine can generate replies to mDns queries.
     *
     * @see OffloadServiceInfo#getOffloadPayload()
     */
    int OFFLOAD_TYPE_REPLY = 1;
    /**
     * Indicates that the OffloadEngine can filter and drop mDns queries.
     */
    int OFFLOAD_TYPE_FILTER_QUERIES = 1 << 1;
    /**
     * Indicates that the OffloadEngine can filter and drop mDns replies. It can allow mDns packets
     * to be received even when no app holds a {@link android.net.wifi.WifiManager.MulticastLock}.
     */
    int OFFLOAD_TYPE_FILTER_REPLIES = 1 << 2;

    /**
     * Indicates that the OffloadEngine can bypass multicast lock.
     */
    int OFFLOAD_CAPABILITY_BYPASS_MULTICAST_LOCK = 1;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @LongDef(flag = true, prefix = {"OFFLOAD_TYPE"}, value = {
            OFFLOAD_TYPE_REPLY,
            OFFLOAD_TYPE_FILTER_QUERIES,
            OFFLOAD_TYPE_FILTER_REPLIES,
    })
    @interface OffloadType {}

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @LongDef(flag = true, prefix = {"OFFLOAD_CAPABILITY"}, value = {
            OFFLOAD_CAPABILITY_BYPASS_MULTICAST_LOCK
    })
    @interface OffloadCapability {}

    /**
     * To be called when the OffloadServiceInfo is added or updated.
     *
     * @param info The OffloadServiceInfo to add or update.
     */
    void onOffloadServiceUpdated(@NonNull OffloadServiceInfo info);

    /**
     * To be called when the OffloadServiceInfo is removed.
     *
     * @param info The OffloadServiceInfo to remove.
     */
    void onOffloadServiceRemoved(@NonNull OffloadServiceInfo info);
}
