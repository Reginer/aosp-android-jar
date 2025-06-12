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

package android.net.wifi.twt;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.StringDef;
import android.annotation.SystemApi;
import android.os.Bundle;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Defines a target wake time (TWT) session.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
public interface TwtSession {
    /**
     * Bundle key to get average number of received packets in each wake duration
     */
    String TWT_STATS_KEY_INT_AVERAGE_RX_PACKET_COUNT = "key_avg_rx_pkt_count";
    /**
     * Bundle key to get average number of transmitted packets in each wake duration
     */
    String TWT_STATS_KEY_INT_AVERAGE_TX_PACKET_COUNT = "key_avg_tx_pkt_count";
    /**
     * Bundle key to get average bytes per received packets in each wake duration
     */
    String TWT_STATS_KEY_INT_AVERAGE_RX_PACKET_SIZE = "key_avg_rx_pkt_size";
    /**
     * Bundle key to get average bytes per transmitted packets in each wake duration
     */
    String TWT_STATS_KEY_INT_AVERAGE_TX_PACKET_SIZE = "key_avg_tx_pkt_size";
    /**
     * Bundle key to get average end of service period in microseconds
     */
    String TWT_STATS_KEY_INT_AVERAGE_EOSP_DURATION_MICROS = "key_avg_eosp_dur";
    /**
     * Bundle key to get count of early termination. Value will be -1 if not available.
     */
    String TWT_STATS_KEY_INT_EOSP_COUNT = "key_eosp_count";

    /** @hide */
    @StringDef(prefix = { "TWT_STATS_KEY_"}, value = {
            TWT_STATS_KEY_INT_AVERAGE_RX_PACKET_COUNT,
            TWT_STATS_KEY_INT_AVERAGE_TX_PACKET_COUNT,
            TWT_STATS_KEY_INT_AVERAGE_RX_PACKET_SIZE,
            TWT_STATS_KEY_INT_AVERAGE_TX_PACKET_SIZE,
            TWT_STATS_KEY_INT_AVERAGE_EOSP_DURATION_MICROS,
            TWT_STATS_KEY_INT_EOSP_COUNT
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface TwtStats {}
    /**
     * Get TWT session wake duration in microseconds.
     *
     * @return wake duration in microseconds.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    int getWakeDurationMicros();

    /**
     * Get TWT session wake interval in microseconds.
     *
     * @return wake interval in microseconds.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    long getWakeIntervalMicros();

    /**
     * Get MLO link id if the station connection is Wi-Fi 7, otherwise returns
     * {@link android.net.wifi.MloLink#INVALID_MLO_LINK_ID}.
     *
     * @return MLO link id
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    int getMloLinkId();

    /**
     * Get stats of the session.
     *
     * Note: If the command fails or not available, -1 will be returned for all stats values.
     *
     * @param executor The executor on which callback will be invoked.
     * @param resultCallback An asynchronous callback that will return a bundle for target wake time
     *                       stats. See {@link TwtStats} for the string keys for the bundle.
     * @throws SecurityException if the caller does not have permission.
     * @throws NullPointerException if the caller provided null inputs.
     * @throws UnsupportedOperationException if the API is not supported.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    void getStats(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Bundle> resultCallback);

    /**
     * Teardown the session. See {@link TwtSessionCallback#onTeardown(int)}. Also closes this
     * session, relinquishing any underlying resources.
     *
     * @throws SecurityException if the caller does not have permission.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    void teardown();
}
