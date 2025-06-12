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

package com.android.server.connectivity;

import static android.util.TimeUtils.NANOS_PER_MS;

import android.annotation.Nullable;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetdEventCallback;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.metrics.ConnectStats;
import android.net.metrics.DnsEvent;
import android.net.metrics.NetworkMetrics;
import android.net.metrics.WakeupEvent;
import android.net.metrics.WakeupStats;
import android.os.BatteryStatsInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.BitUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.RingBuffer;
import com.android.internal.util.TokenBucket;
import com.android.net.module.util.BaseNetdEventListener;
import com.android.server.LocalServices;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass.IpConnectivityEvent;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Implementation of the INetdEventListener interface.
 */
public class NetdEventListenerService extends BaseNetdEventListener {

    public static final String SERVICE_NAME = "netd_listener";

    private static final String TAG = NetdEventListenerService.class.getSimpleName();
    private static final boolean DBG = false;

    // Rate limit connect latency logging to 1 measurement per 15 seconds (5760 / day) with maximum
    // bursts of 5000 measurements.
    private static final int CONNECT_LATENCY_BURST_LIMIT  = 5000;
    private static final int CONNECT_LATENCY_FILL_RATE    = 15 * (int) DateUtils.SECOND_IN_MILLIS;

    private static final long METRICS_SNAPSHOT_SPAN_MS = 5 * DateUtils.MINUTE_IN_MILLIS;
    private static final int METRICS_SNAPSHOT_BUFFER_SIZE = 48; // 4 hours

    @VisibleForTesting
    static final int WAKEUP_EVENT_BUFFER_LENGTH = 1024;
    // TODO: dedup this String constant with the one used in
    // ConnectivityService#wakeupModifyInterface().
    @VisibleForTesting
    static final String WAKEUP_EVENT_PREFIX_DELIM = ":";

    // Array of aggregated DNS and connect events sent by netd, grouped by net id.
    @GuardedBy("this")
    private final SparseArray<NetworkMetrics> mNetworkMetrics = new SparseArray<>();

    @GuardedBy("this")
    private final RingBuffer<NetworkMetricsSnapshot> mNetworkMetricsSnapshots =
            new RingBuffer<>(NetworkMetricsSnapshot.class, METRICS_SNAPSHOT_BUFFER_SIZE);
    @GuardedBy("this")
    private long mLastSnapshot = 0;

    // Array of aggregated wakeup event stats, grouped by interface name.
    @GuardedBy("this")
    private final ArrayMap<String, WakeupStats> mWakeupStats = new ArrayMap<>();
    // Ring buffer array for storing packet wake up events sent by Netd.
    @GuardedBy("this")
    private final RingBuffer<WakeupEvent> mWakeupEvents =
            new RingBuffer<>(WakeupEvent.class, WAKEUP_EVENT_BUFFER_LENGTH);

    private final ConnectivityManager mCm;

    @GuardedBy("this")
    private final TokenBucket mConnectTb =
            new TokenBucket(CONNECT_LATENCY_FILL_RATE, CONNECT_LATENCY_BURST_LIMIT);

    final TransportForNetIdNetworkCallback mCallback = new TransportForNetIdNetworkCallback();

    /**
     * There are only 3 possible callbacks.
     *
     * mNetdEventCallbackList[CALLBACK_CALLER_CONNECTIVITY_SERVICE]
     * Callback registered/unregistered by ConnectivityService.
     *
     * mNetdEventCallbackList[CALLBACK_CALLER_DEVICE_POLICY]
     * Callback registered/unregistered when logging is being enabled/disabled in DPM
     * by the device owner. It's DevicePolicyManager's responsibility to ensure that.
     *
     * mNetdEventCallbackList[CALLBACK_CALLER_NETWORK_WATCHLIST]
     * Callback registered/unregistered by NetworkWatchlistService.
     */
    @GuardedBy("this")
    private static final int[] ALLOWED_CALLBACK_TYPES = {
        INetdEventCallback.CALLBACK_CALLER_CONNECTIVITY_SERVICE,
        INetdEventCallback.CALLBACK_CALLER_DEVICE_POLICY,
        INetdEventCallback.CALLBACK_CALLER_NETWORK_WATCHLIST
    };

    @GuardedBy("this")
    private INetdEventCallback[] mNetdEventCallbackList =
            new INetdEventCallback[ALLOWED_CALLBACK_TYPES.length];

    public synchronized boolean addNetdEventCallback(int callerType, INetdEventCallback callback) {
        if (!isValidCallerType(callerType)) {
            Log.e(TAG, "Invalid caller type: " + callerType);
            return false;
        }
        mNetdEventCallbackList[callerType] = callback;
        return true;
    }

    public synchronized boolean removeNetdEventCallback(int callerType) {
        if (!isValidCallerType(callerType)) {
            Log.e(TAG, "Invalid caller type: " + callerType);
            return false;
        }
        mNetdEventCallbackList[callerType] = null;
        return true;
    }

    private static boolean isValidCallerType(int callerType) {
        for (int i = 0; i < ALLOWED_CALLBACK_TYPES.length; i++) {
            if (callerType == ALLOWED_CALLBACK_TYPES[i]) {
                return true;
            }
        }
        return false;
    }

    public NetdEventListenerService(Context context) {
        this(context.getSystemService(ConnectivityManager.class));
    }

    @VisibleForTesting
    public NetdEventListenerService(ConnectivityManager cm) {
        // We are started when boot is complete, so ConnectivityService should already be running.
        mCm = cm;
        // Clear all capabilities to listen all networks.
        mCm.registerNetworkCallback(new NetworkRequest.Builder().clearCapabilities().build(),
                mCallback);
    }

    private static long projectSnapshotTime(long timeMs) {
        return (timeMs / METRICS_SNAPSHOT_SPAN_MS) * METRICS_SNAPSHOT_SPAN_MS;
    }

    private NetworkMetrics getMetricsForNetwork(long timeMs, int netId) {
        NetworkMetrics metrics = mNetworkMetrics.get(netId);
        final NetworkCapabilities nc = mCallback.getNetworkCapabilities(netId);
        final long transports = (nc != null) ? BitUtils.packBits(nc.getTransportTypes()) : 0;
        final boolean forceCollect =
                (metrics != null && nc != null && metrics.transports != transports);
        collectPendingMetricsSnapshot(timeMs, forceCollect);
        if (metrics == null || forceCollect) {
            metrics = new NetworkMetrics(netId, transports, mConnectTb);
            mNetworkMetrics.put(netId, metrics);
        }
        return metrics;
    }

    private NetworkMetricsSnapshot[] getNetworkMetricsSnapshots() {
        collectPendingMetricsSnapshot(System.currentTimeMillis(), false /* forceCollect */);
        return mNetworkMetricsSnapshots.toArray();
    }

    private void collectPendingMetricsSnapshot(long timeMs, boolean forceCollect) {
        // Detects time differences larger than the snapshot collection period.
        // This is robust against clock jumps and long inactivity periods.
        if (!forceCollect && Math.abs(timeMs - mLastSnapshot) <= METRICS_SNAPSHOT_SPAN_MS) {
            return;
        }
        mLastSnapshot = projectSnapshotTime(timeMs);
        NetworkMetricsSnapshot snapshot =
                NetworkMetricsSnapshot.collect(mLastSnapshot, mNetworkMetrics);
        if (snapshot.stats.isEmpty()) {
            return;
        }
        mNetworkMetricsSnapshots.append(snapshot);
    }

    @Override
    // Called concurrently by multiple binder threads.
    // This method must not block or perform long-running operations.
    public synchronized void onDnsEvent(int netId, int eventType, int returnCode, int latencyMs,
            String hostname, String[] ipAddresses, int ipAddressesCount, int uid) {
        long timestamp = System.currentTimeMillis();
        getMetricsForNetwork(timestamp, netId).addDnsResult(eventType, returnCode, latencyMs);

        for (INetdEventCallback callback : mNetdEventCallbackList) {
            if (callback != null) {
                try {
                    callback.onDnsEvent(netId, eventType, returnCode, hostname, ipAddresses,
                            ipAddressesCount, timestamp, uid);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    @Override
    // Called concurrently by multiple binder threads.
    // This method must not block or perform long-running operations.
    public synchronized void onNat64PrefixEvent(int netId,
            boolean added, String prefixString, int prefixLength) {
        for (INetdEventCallback callback : mNetdEventCallbackList) {
            if (callback != null) {
                try {
                    callback.onNat64PrefixEvent(netId, added, prefixString, prefixLength);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    @Override
    // Called concurrently by multiple binder threads.
    // This method must not block or perform long-running operations.
    public synchronized void onPrivateDnsValidationEvent(int netId,
            String ipAddress, String hostname, boolean validated) {
        for (INetdEventCallback callback : mNetdEventCallbackList) {
            if (callback != null) {
                try {
                    callback.onPrivateDnsValidationEvent(netId, ipAddress, hostname, validated);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    @Override
    // Called concurrently by multiple binder threads.
    // This method must not block or perform long-running operations.
    public synchronized void onConnectEvent(int netId, int error, int latencyMs, String ipAddr,
            int port, int uid) {
        long timestamp = System.currentTimeMillis();
        getMetricsForNetwork(timestamp, netId).addConnectResult(error, latencyMs, ipAddr);

        for (INetdEventCallback callback : mNetdEventCallbackList) {
            if (callback != null) {
                try {
                    callback.onConnectEvent(ipAddr, port, timestamp, uid);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private boolean hasWifiTransport(Network network) {
        final NetworkCapabilities nc = mCm.getNetworkCapabilities(network);
        return nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    @Override
    public synchronized void onWakeupEvent(String prefix, int uid, int ethertype, int ipNextHeader,
            byte[] dstHw, String srcIp, String dstIp, int srcPort, int dstPort, long timestampNs) {
        final String[] prefixParts = prefix.split(WAKEUP_EVENT_PREFIX_DELIM);
        if (prefixParts.length != 2) {
            throw new IllegalArgumentException("Prefix " + prefix
                    + " required in format <nethandle>:<interface>");
        }
        final long netHandle = Long.parseLong(prefixParts[0]);
        final Network network = Network.fromNetworkHandle(netHandle);

        final WakeupEvent event = new WakeupEvent();
        event.iface = prefixParts[1];
        event.uid = uid;
        event.ethertype = ethertype;
        if (ArrayUtils.isEmpty(dstHw)) {
            if (hasWifiTransport(network)) {
                Log.e(TAG, "Empty mac address on WiFi transport, network: " + network);
            }
            event.dstHwAddr = null;
        } else {
            event.dstHwAddr = MacAddress.fromBytes(dstHw);
        }
        event.srcIp = srcIp;
        event.dstIp = dstIp;
        event.ipNextHeader = ipNextHeader;
        event.srcPort = srcPort;
        event.dstPort = dstPort;
        if (timestampNs > 0) {
            event.timestampMs = timestampNs / NANOS_PER_MS;
        } else {
            event.timestampMs = System.currentTimeMillis();
        }
        addWakeupEvent(event);

        final BatteryStatsInternal bsi = LocalServices.getService(BatteryStatsInternal.class);
        if (bsi != null) {
            final long elapsedMs = SystemClock.elapsedRealtime() + event.timestampMs
                    - System.currentTimeMillis();
            bsi.noteCpuWakingNetworkPacket(network, elapsedMs, event.uid);
        }

        final String dstMac = String.valueOf(event.dstHwAddr);
        FrameworkStatsLog.write(FrameworkStatsLog.PACKET_WAKEUP_OCCURRED,
                uid, event.iface, ethertype, dstMac, srcIp, dstIp, ipNextHeader, srcPort, dstPort);
    }

    @Override
    public synchronized void onTcpSocketStatsEvent(int[] networkIds,
            int[] sentPackets, int[] lostPackets, int[] rttsUs, int[] sentAckDiffsMs) {
        if (networkIds.length != sentPackets.length
                || networkIds.length != lostPackets.length
                || networkIds.length != rttsUs.length
                || networkIds.length != sentAckDiffsMs.length) {
            Log.e(TAG, "Mismatched lengths of TCP socket stats data arrays");
            return;
        }

        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < networkIds.length; i++) {
            int netId = networkIds[i];
            int sent = sentPackets[i];
            int lost = lostPackets[i];
            int rttUs = rttsUs[i];
            int sentAckDiffMs = sentAckDiffsMs[i];
            getMetricsForNetwork(timestamp, netId)
                    .addTcpStatsResult(sent, lost, rttUs, sentAckDiffMs);
        }
    }

    @Override
    public int getInterfaceVersion() {
        return this.VERSION;
    }

    @Override
    public String getInterfaceHash() {
        return this.HASH;
    }

    private void addWakeupEvent(WakeupEvent event) {
        String iface = event.iface;
        mWakeupEvents.append(event);
        WakeupStats stats = mWakeupStats.get(iface);
        if (stats == null) {
            stats = new WakeupStats(iface);
            mWakeupStats.put(iface, stats);
        }
        stats.countEvent(event);
    }

    public synchronized void flushStatistics(List<IpConnectivityEvent> events) {
        for (int i = 0; i < mNetworkMetrics.size(); i++) {
            ConnectStats stats = mNetworkMetrics.valueAt(i).connectMetrics;
            if (stats.eventCount == 0) {
                continue;
            }
            events.add(IpConnectivityEventBuilder.toProto(stats));
        }
        for (int i = 0; i < mNetworkMetrics.size(); i++) {
            DnsEvent ev = mNetworkMetrics.valueAt(i).dnsMetrics;
            if (ev.eventCount == 0) {
                continue;
            }
            events.add(IpConnectivityEventBuilder.toProto(ev));
        }
        for (int i = 0; i < mWakeupStats.size(); i++) {
            events.add(IpConnectivityEventBuilder.toProto(mWakeupStats.valueAt(i)));
        }
        mNetworkMetrics.clear();
        mWakeupStats.clear();
    }

    public synchronized void list(PrintWriter pw) {
        pw.println("dns/connect events:");
        for (int i = 0; i < mNetworkMetrics.size(); i++) {
            pw.println(mNetworkMetrics.valueAt(i).connectMetrics);
        }
        for (int i = 0; i < mNetworkMetrics.size(); i++) {
            pw.println(mNetworkMetrics.valueAt(i).dnsMetrics);
        }
        pw.println("");
        pw.println("network statistics:");
        for (NetworkMetricsSnapshot s : getNetworkMetricsSnapshots()) {
            pw.println(s);
        }
        pw.println("");
        pw.println("packet wakeup events:");
        for (int i = 0; i < mWakeupStats.size(); i++) {
            pw.println(mWakeupStats.valueAt(i));
        }
        for (WakeupEvent wakeup : mWakeupEvents.toArray()) {
            pw.println(wakeup);
        }
    }

    /**
     * Convert events in the buffer to a list of IpConnectivityEvent protos
     */
    public synchronized List<IpConnectivityEvent> listAsProtos() {
        List<IpConnectivityEvent> list = new ArrayList<>();
        for (int i = 0; i < mNetworkMetrics.size(); i++) {
            list.add(IpConnectivityEventBuilder.toProto(mNetworkMetrics.valueAt(i).connectMetrics));
        }
        for (int i = 0; i < mNetworkMetrics.size(); i++) {
            list.add(IpConnectivityEventBuilder.toProto(mNetworkMetrics.valueAt(i).dnsMetrics));
        }
        for (int i = 0; i < mWakeupStats.size(); i++) {
            list.add(IpConnectivityEventBuilder.toProto(mWakeupStats.valueAt(i)));
        }
        return list;
    }

    /** Helper class for buffering summaries of NetworkMetrics at regular time intervals */
    static class NetworkMetricsSnapshot {

        public long timeMs;
        public List<NetworkMetrics.Summary> stats = new ArrayList<>();

        static NetworkMetricsSnapshot collect(long timeMs, SparseArray<NetworkMetrics> networkMetrics) {
            NetworkMetricsSnapshot snapshot = new NetworkMetricsSnapshot();
            snapshot.timeMs = timeMs;
            for (int i = 0; i < networkMetrics.size(); i++) {
                NetworkMetrics.Summary s = networkMetrics.valueAt(i).getPendingStats();
                if (s != null) {
                    snapshot.stats.add(s);
                }
            }
            return snapshot;
        }

        @Override
        public String toString() {
            StringJoiner j = new StringJoiner(", ");
            for (NetworkMetrics.Summary s : stats) {
                j.add(s.toString());
            }
            return String.format("%tT.%tL: %s", timeMs, timeMs, j.toString());
        }
    }

    private class TransportForNetIdNetworkCallback extends ConnectivityManager.NetworkCallback {
        private final SparseArray<NetworkCapabilities> mCapabilities = new SparseArray<>();

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
            synchronized (mCapabilities) {
                mCapabilities.put(network.getNetId(), nc);
            }
        }

        @Override
        public void onLost(Network network) {
            synchronized (mCapabilities) {
                mCapabilities.remove(network.getNetId());
            }
        }

        @Nullable
        public NetworkCapabilities getNetworkCapabilities(int netId) {
            synchronized (mCapabilities) {
                return mCapabilities.get(netId);
            }
        }
    }
}
