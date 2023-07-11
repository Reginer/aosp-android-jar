package com.android.clockwork.bluetooth.proxy;

import android.annotation.MainThread;
import android.annotation.NonNull;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.os.Looper;
import android.util.Log;
import com.android.clockwork.common.DebugAssert;
import com.android.internal.util.IndentingPrintWriter;

/**
 * {@link NetworkFactory} that represents bluetooth companion proxy network agents.
 */
class ProxyNetworkFactory extends NetworkFactory {
    private static final String TAG = WearProxyConstants.LOG_TAG;

    @NonNull private final NetworkCapabilities mCapabilities;

    private int mNetworkScoreFilter;

    protected ProxyNetworkFactory(
            @NonNull final Context context,
            @NonNull final NetworkCapabilities capabilities) {
        super(Looper.getMainLooper(), context, WearProxyConstants.PROXY_NETWORK_TYPE_NAME,
                capabilities);
        DebugAssert.isMainThread();

        mCapabilities = capabilities;

        // This is in a system service so we never unregister() the factory
        this.register();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Created proxy network factory");
        }
    }

    /** Invoking this may force a network evaluation of default network
     *
     * This factory will advertise the highest possible score that
     * this network is capable of delivering.
     *
     * If the factory network score goes up, network requests may be fulfilled by
     * this bearer factory network during the evaluation phase.  This results in
     * the proxy network being started up.
     *
     * If the factory network score goes down, existing network requests accustomed
     * to the higher score that are connected will be stopped and re-evaluated to
     * see which requests may be fulfilled by the bearer.
     *
     * If the proxy network factory maintains the highest factory network score filter
     * the current connections remain connected and are not re-evaluated and the
     * network is not stopped.
     *
     */
    @MainThread
    protected void setNetworkScore(final int networkScore) {
        DebugAssert.isMainThread();
        if (networkScore > mNetworkScoreFilter) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Changing network score filter from " + mNetworkScoreFilter
                        + " to " + networkScore);
            }
            mNetworkScoreFilter = networkScore;
            this.setScoreFilter(mNetworkScoreFilter);
        }
    }

    public void dump(@NonNull final IndentingPrintWriter ipw) {
        ipw.printPair("Network factory filter score", mNetworkScoreFilter);
        ipw.printPair("capabilities", mCapabilities);
        ipw.println();
    }
}
