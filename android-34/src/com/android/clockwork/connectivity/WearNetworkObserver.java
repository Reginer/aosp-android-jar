package com.android.clockwork.connectivity;

import android.annotation.NonNull;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.os.Looper;
import android.util.Log;

import com.android.clockwork.common.DebugAssert;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.Set;

/**
 * A dummy NetworkFactory whose primary job is to monitor NetworkRequests as they come in and out of
 * ConnectivityService, so that it may keep track of the count of requests based on different
 * characteristics (i.e. unmetered network requests) and bounce that information to a registered
 * listener.
 */
public class WearNetworkObserver extends NetworkFactory {

    private static final String TAG = "WearNetworkObserver";
    private static final String NETWORK_TYPE = "WEAR_NETWORK_OBSERVER";

    // At its best performance, Bluetooth proxy provides 200kbps of bandwidth, so any request
    // for a bandwidth higher than this is considered "high bandwidth".
    @VisibleForTesting static final int HIGH_BANDWIDTH_KBPS = 200 * 1024;

    public interface Listener {
        void onUnmeteredRequestsChanged(int numUnmeteredRequests);

        void onHighBandwidthRequestsChanged(int numHighBandwidthRequests);

        void onWifiRequestsChanged(int numWifiRequests);

        void onCellularRequestsChanged(int numCellularRequests);
    }

    private final Set<NetworkRequest> mUnmeteredRequests = ConcurrentHashMap.newKeySet();
    private final Set<NetworkRequest> mHighBandwidthRequests = ConcurrentHashMap.newKeySet();
    private final Set<NetworkRequest> mWifiRequests = ConcurrentHashMap.newKeySet();
    private final Set<NetworkRequest> mCellularRequests = ConcurrentHashMap.newKeySet();
    private final WearConnectivityPackageManager mWearConnectivityPackageManager;
    private final Listener mListener;

    public WearNetworkObserver(
            final Context context,
            WearConnectivityPackageManager wearConnectivityPackageManager,
            Listener listener) {
        super(
                Looper.getMainLooper(),
                context,
                NETWORK_TYPE,
                makeNetworkCapatibilitiesFilterBuilder());
        DebugAssert.isMainThread();
        mWearConnectivityPackageManager = wearConnectivityPackageManager;
        mListener = listener;
    }

    private static NetworkCapabilities makeNetworkCapatibilitiesFilterBuilder() {
        NetworkCapabilities.Builder builder =
                new NetworkCapabilities.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                        .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                        .addTransportType(NetworkCapabilities.TRANSPORT_LOWPAN)
                        .addTransportType(NetworkCapabilities.TRANSPORT_TEST)
                        .addTransportType(NetworkCapabilities.TRANSPORT_USB);

        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED);
        return builder.build();
    }

    @Override
    protected void needNetworkFor(@NonNull final NetworkRequest networkRequest) {
        DebugAssert.isMainThread();
        verboseLog("WearNetworkObserver: needNetworkFor " + networkRequest);

        // TODO(b/268314549): handle high bandwidth network requests.

        if (!mUnmeteredRequests.contains(networkRequest)
                && networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            mUnmeteredRequests.add(networkRequest);
            mListener.onUnmeteredRequestsChanged(mUnmeteredRequests.size());
        }

        if (!mWifiRequests.contains(networkRequest)
                && networkRequest.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            mWifiRequests.add(networkRequest);
            mListener.onWifiRequestsChanged(mWifiRequests.size());
        }

        if (!mCellularRequests.contains(networkRequest)
                && networkRequest.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            if (mWearConnectivityPackageManager.isSuppressedCellularRequestor(
                    networkRequest.getRequestorPackageName())) {
                verboseLog(
                        String.format(
                                Locale.US,
                                "handleAddRequest - Suppressing cellular network request from %s",
                                networkRequest.getRequestorPackageName()));
            } else {
                mCellularRequests.add(networkRequest);
                mListener.onCellularRequestsChanged(mCellularRequests.size());
            }
        }

        verboseLog(
                String.format(
                        Locale.US,
                        "needNetworkFor - [unmetered %d // highband %d // wifi %d // cell %d ]",
                        mUnmeteredRequests.size(),
                        mHighBandwidthRequests.size(),
                        mWifiRequests.size(),
                        mCellularRequests.size()));
    }

    @Override
    protected void releaseNetworkFor(@NonNull final NetworkRequest networkRequest) {
        DebugAssert.isMainThread();
        verboseLog("WearNetworkObserver: handleRemoveRequest");
        if (mUnmeteredRequests.contains(networkRequest)) {
            mUnmeteredRequests.remove(networkRequest);
            mListener.onUnmeteredRequestsChanged(mUnmeteredRequests.size());
        }

        if (mHighBandwidthRequests.contains(networkRequest)) {
            mHighBandwidthRequests.remove(networkRequest);
            mListener.onHighBandwidthRequestsChanged(mHighBandwidthRequests.size());
        }

        if (mWifiRequests.contains(networkRequest)) {
            mWifiRequests.remove(networkRequest);
            mListener.onWifiRequestsChanged(mWifiRequests.size());
        }

        if (mCellularRequests.contains(networkRequest)) {
            mCellularRequests.remove(networkRequest);
            mListener.onCellularRequestsChanged(mCellularRequests.size());
        }

        verboseLog(
                String.format(
                        Locale.US,
                        "releaseNetworkFor - [unmetered %d // highband %d // wifi %d // cell %d ]",
                        mUnmeteredRequests.size(),
                        mHighBandwidthRequests.size(),
                        mWifiRequests.size(),
                        mCellularRequests.size()));
    }

    private void verboseLog(String msg) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, msg);
        }
    }

    /**
     * This method may dump memory-inconsistent values when called off the main thread. This is
     * preferable to synchronizing every call to needNetworkFor/releaseNetworkFor.
     */
    public void dump(IndentingPrintWriter ipw) {
        ipw.println("======== WearNetworkObserver ========");
        ipw.printPair("Outstanding unmetered network requests", mUnmeteredRequests.size());
        ipw.println();
        ipw.printPair("Outstanding high-bandwidth network requests", mHighBandwidthRequests.size());
        ipw.println();
        ipw.printPair("Outstanding wifi network requests", mWifiRequests.size());
        ipw.println();
        ipw.printPair("Outstanding cellular network requests", mCellularRequests.size());
        ipw.println();

        ipw.println("Unmetered requests: ");
        ipw.increaseIndent();
        for (NetworkRequest req : mUnmeteredRequests) {
            ipw.print(req.toString());
        }
        ipw.decreaseIndent();

        ipw.println();
        ipw.println("High-bandwidth requests: ");
        ipw.increaseIndent();
        for (NetworkRequest req : mHighBandwidthRequests) {
            ipw.print(req.toString());
        }
        ipw.decreaseIndent();

        ipw.println();
        ipw.println("Wifi requests: ");
        ipw.increaseIndent();
        for (NetworkRequest req : mWifiRequests) {
            ipw.print(req.toString());
        }
        ipw.decreaseIndent();

        ipw.println();
        ipw.println("Cellular requests: ");
        ipw.increaseIndent();
        for (NetworkRequest req : mCellularRequests) {
            ipw.print(req.toString());
        }
        ipw.decreaseIndent();
    }
}
