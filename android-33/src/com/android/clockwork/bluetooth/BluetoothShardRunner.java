package com.android.clockwork.bluetooth;

import android.annotation.MainThread;
import android.annotation.NonNull;
import android.content.Context;
import android.util.Log;

import com.android.clockwork.bluetooth.proxy.ProxyServiceHelper;
import com.android.clockwork.common.LogUtil;
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
    private final ProxyServiceHelper mProxyServiceHelper;

    private int mCompanionShardStarts;
    private int mCompanionShardStops;
    private int mConnectionPort;

    // Kept for logging and dumpsys debug
    private List<InetAddress> mDnsServers;

    private final CompanionProxyShard mProxyShard;

    public BluetoothShardRunner(
            final Context context,
            final CompanionTracker companionTracker,
            final ProxyServiceHelper proxyServiceHelper) {
        mContext = context;
        mCompanionTracker = companionTracker;
        mProxyServiceHelper = proxyServiceHelper;
        mProxyShard = new CompanionProxyShard(mContext, mProxyServiceHelper, mCompanionTracker);
        mDnsServers = new ArrayList<>();
    }

    @MainThread
    void startProxyShard(
            final int proxyScore,
            final List<InetAddress> dnsServers,
            final CompanionProxyShard.Listener listener,
            final String reason,
            final int connectionPort) {
        mConnectionPort = connectionPort;
        if (mCompanionTracker.getCompanion() == null) {
            Log.w(TAG, "BluetoothShardRunner Companion is unavailable for proxy: "
                    + mCompanionTracker.getCompanion());
            return;
        }
        mCompanionShardStarts += 1;
        mDnsServers.clear();
        mDnsServers.addAll(dnsServers);

        LogUtil.logDOrNotUser(TAG, "Start sysproxy [%s] with score(%d) and DnsServers[%s]",
                mCompanionTracker.getCompanion(), proxyScore, inetAddressesToString(mDnsServers));
        mProxyShard.startNetwork(proxyScore, mDnsServers, connectionPort, listener);
    }

    @MainThread
    public int getConnectionPort() {
        return mConnectionPort;
    }

    @MainThread
    void updateProxyShard(final int proxyScore) {
        if (mProxyShard != null) {
            mProxyShard.updateNetwork(proxyScore);
        }
    }

    @MainThread
    void updateProxyShard(List<InetAddress> dnsServers) {
        LogUtil.logDOrNotUser(TAG, "Updating DNS Servers to: " + inetAddressesToString(dnsServers));
        mDnsServers.clear();
        mDnsServers.addAll(dnsServers);
        if (mProxyShard != null) {
            mProxyShard.updateNetwork(mDnsServers);
        }
    }

    @MainThread
    void stopProxyShard() {
        mDnsServers.clear();
        if (mProxyShard != null) {
            LogUtil.logDOrNotUser(TAG, "Stop sysproxy");
            mProxyShard.stop();
            mCompanionShardStops += 1;
        }
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
        if (mProxyShard != null) {
            mProxyShard.dump(ipw);
        }
        ipw.increaseIndent();
        ipw.printPair("companion shard starts", mCompanionShardStarts);
        ipw.printPair("companion shard stops", mCompanionShardStops);
        ipw.printPair("connectionPort", mConnectionPort);
        ipw.println();
        ipw.printPair("Dns Servers", inetAddressesToString(mDnsServers));
        ipw.decreaseIndent();
        ipw.println();
    }
}
