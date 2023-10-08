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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.net.MacAddress;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/**
 * Base class for Aware identity/cluster changes callbacks. Should be extended by applications and
 * set when calling {@link WifiAwareManager#attach(AttachCallback, IdentityChangedListener,
 * android.os.Handler)}. These are callbacks applying to the Aware connection as a whole - not to
 * specific publish or subscribe sessions - for that see {@link DiscoverySessionCallback}.
 */
public class IdentityChangedListener {
    /** @hide */
    @IntDef(prefix = {"CLUSTER_CHANGE_EVENT_"}, value = {
        CLUSTER_CHANGE_EVENT_STARTED,
        CLUSTER_CHANGE_EVENT_JOINED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ClusterChangeEvent {}

    /**
     * Wi-Fi Aware cluster change event type when starting a cluster.
     */
    public static final int CLUSTER_CHANGE_EVENT_STARTED = 0;
    /**
     * Wi-Fi Aware cluster change event type when joining a cluster.
     */
    public static final int CLUSTER_CHANGE_EVENT_JOINED = 1;

    /**
     * Identity change may be due to device joining a cluster, starting a cluster, or discovery
     * interface change (addresses are randomized at regular intervals). The implication is that
     * peers you've been communicating with may no longer recognize you and you need to re-establish
     * your identity - e.g. by starting a discovery session.
     *
     * @param mac The MAC address of the Aware discovery interface. The application must have the
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} to get the actual MAC address,
     *            otherwise all 0's will be provided.
     */
    public void onIdentityChanged(byte[] mac) {
        /* empty */
    }

    /**
     * Cluster ID changes could be trigger by either cluster started event or cluster joined event.
     * @param clusterEventType The type of events that triggered the change of the cluster ID.
     * @param clusterId The cluster id that the device just joined.
     */
    public void onClusterIdChanged(@ClusterChangeEvent int clusterEventType,
            @NonNull MacAddress clusterId) {
        /* empty */
    }
}
