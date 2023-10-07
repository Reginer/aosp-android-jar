package com.android.clockwork.bluetooth;

import android.annotation.MainThread;
import android.annotation.NonNull;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.android.clockwork.bluetooth.proxy.ProxyServiceConfig;
import com.android.clockwork.common.LogUtil;
import com.android.clockwork.common.Util;
import com.android.clockwork.common.WearBluetoothSettings;
import com.android.internal.util.IndentingPrintWriter;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This class serves as a thin layer of separation between the lifecycle management of
 * the shards owned by WearBluetoothService and the underlying implementations of the
 * shards themselves.
 *
 */
public class BluetoothShardRunner {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;

    private final Context mContext;
    private final CompanionTracker mCompanionTracker;

    private int mCompanionShardStarts;
    private int mCompanionShardStops;
    private ProxyServiceConfig mProxyServiceConfig = new ProxyServiceConfig();

    // Kept for logging and dumpsys debug
    private List<InetAddress> mDnsServers;

    private final CompanionProxyShard mProxyShard;
    private HandsFreeClientShard mHfcShard;

    public BluetoothShardRunner(
            final Context context,
            final CompanionTracker companionTracker,
            final boolean isLocalEdition) {
        mContext = context;
        mCompanionTracker = companionTracker;
        mProxyShard = new CompanionProxyShard(
            mContext, mCompanionTracker, isLocalEdition);
        mDnsServers = new ArrayList<>();
    }

    @MainThread
    void startProxyShard(
            final int proxyScore,
            final List<InetAddress> dnsServers,
            final CompanionProxyShard.Listener listener,
            final String reason,
            final ProxyServiceConfig serviceConfig) {
        mProxyServiceConfig = serviceConfig;
        if (mCompanionTracker.getCompanion() == null) {
            Log.w(TAG, "BluetoothShardRunner Companion is unavailable for proxy: "
                    + mCompanionTracker.getCompanion());
            return;
        }
        mCompanionShardStarts += 1;
        mDnsServers.clear();
        mDnsServers.addAll(dnsServers);

        LogUtil.logDOrNotUser(TAG, "Start sysproxy [%s] with score(%d) and DnsServers[%s]: %s",
                mCompanionTracker.getCompanion(), proxyScore, inetAddressesToString(mDnsServers),
                reason);
        mProxyShard.startNetwork(proxyScore, mDnsServers, mProxyServiceConfig, listener);
    }

    @MainThread
    boolean isProxyShardStarted() {
        return mProxyShard.isStarted();
    }

    @MainThread
    void updateProxyShard(final int proxyScore) {
        mProxyShard.updateNetwork(proxyScore);
    }

    @MainThread
    void updateProxyShard(List<InetAddress> dnsServers) {
        LogUtil.logDOrNotUser(TAG, "Updating DNS Servers to: " + inetAddressesToString(dnsServers));
        mDnsServers.clear();
        mDnsServers.addAll(dnsServers);
        mProxyShard.updateNetwork(mDnsServers);
    }

    @MainThread
    void stopProxyShard() {
        mDnsServers.clear();
        LogUtil.logDOrNotUser(TAG, "Stop sysproxy");
        mProxyShard.stop();
        mCompanionShardStops += 1;
    }

    @MainThread
    void startHfcShard(BluetoothDevice companion) {
        if (companion == null) {
            return;
        }
        if (mHfcShard != null) {
            Log.w(TAG, "BluetoothShardRunner Tearing down orphan HfcShard before starting"
                    + " new shard.");
            stopHfcShard();
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "BluetoothShardRunner Starting HandsFreeClientShard for companion ["
                    + companion + "]");
        }
        mHfcShard = new HandsFreeClientShard(mContext, companion, this);
    }

    @MainThread
    void stopHfcShard() {
        if (mHfcShard != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "BluetoothShardRunner Stopping HandsFreeClientShard.");
            }
            Util.close(mHfcShard);
        }
        mHfcShard = null;
    }

    private static String inetAddressesToString(List<InetAddress> inetAddresses) {
        StringBuilder sb = new StringBuilder();
        for (InetAddress inetAddress : inetAddresses) {
            sb.append(inetAddress.toString()).append("; ");
        }
        return sb.toString();
    }

    void dumpShards(@NonNull final IndentingPrintWriter ipw) {
        ipw.printf("Dumping shard(s).\n");
        mProxyShard.dump(ipw);
        ipw.increaseIndent();
        ipw.printPair("companion shard starts", mCompanionShardStarts);
        ipw.printPair("companion shard stops", mCompanionShardStops);
        ipw.printPair("connectionConfig", mProxyServiceConfig);
        ipw.println();
        ipw.printPair("Dns Servers", inetAddressesToString(mDnsServers));
        ipw.decreaseIndent();
        ipw.println();
        if (mHfcShard != null) {
            mHfcShard.dump(ipw);
        }
    }
}
