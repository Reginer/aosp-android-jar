package com.android.clockwork.connectivity;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import com.android.clockwork.common.DebugAssert;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.util.Locale;

/**
 * A dummy NetworkFactory whose primary job is to monitor NetworkRequests as they come in
 * and out of ConnectivityService, so that it may keep track of the count of requests based
 * on different characteristics (i.e. unmetered network requests) and bounce that information
 * to a registered listener.
 */
public class WearNetworkObserver extends NetworkFactory {

    private static final String TAG = "WearNetworkObserver";
    private static final String NETWORK_TYPE = "WEAR_NETWORK_OBSERVER";

    // At its best performance, Bluetooth proxy provides 200kbps of bandwidth, so any request
    // for a bandwidth higher than this is considered "high bandwidth".
    @VisibleForTesting
    static final int HIGH_BANDWIDTH_KBPS = 200 * 1024;

    public interface Listener {
        void onUnmeteredRequestsChanged(int numUnmeteredRequests);
        void onHighBandwidthRequestsChanged(int numHighBandwidthRequests);
        void onWifiRequestsChanged(int numWifiRequests);
        void onCellularRequestsChanged(int numCellularRequests);
    }

    private final SparseArray<NetworkRequest> mUnmeteredRequests = new SparseArray<>();
    private final SparseArray<NetworkRequest> mHighBandwidthRequests = new SparseArray<>();
    private final SparseArray<NetworkRequest> mWifiRequests = new SparseArray<>();
    private final SparseArray<NetworkRequest> mCellularRequests = new SparseArray<>();
    private final WearConnectivityPackageManager mWearConnectivityPackageManager;
    private final Listener mListener;

    public WearNetworkObserver(
        final Context context,
        WearConnectivityPackageManager wearConnectivityPackageManager,
        Listener listener) {
        super(Looper.getMainLooper(), context, NETWORK_TYPE, null);
        DebugAssert.isMainThread();
        mWearConnectivityPackageManager = wearConnectivityPackageManager;
        mListener = listener;
    }

    protected void handleAddRequest(NetworkRequest req, int score, int servingProviderId,
		    int requestId, NetworkCapabilities networkCapabilities){
        DebugAssert.isMainThread();
        verboseLog("WearNetworkObserver: handleAddRequest " + req + " id " + servingProviderId);

        if (mUnmeteredRequests.get(requestId) == null
                && req.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            mUnmeteredRequests.put(requestId, req);
            mListener.onUnmeteredRequestsChanged(mUnmeteredRequests.size());
        }

        if (mHighBandwidthRequests.get(requestId) == null
                && (networkCapabilities.getLinkDownstreamBandwidthKbps()
                        > HIGH_BANDWIDTH_KBPS)) {
            mHighBandwidthRequests.put(requestId, req);
            mListener.onHighBandwidthRequestsChanged(mHighBandwidthRequests.size());
        }

        if (mWifiRequests.get(requestId) == null
                && req.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            mWifiRequests.put(requestId, req);
            mListener.onWifiRequestsChanged(mWifiRequests.size());
        }

        if (mCellularRequests.get(requestId) == null
                && req.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            if (mWearConnectivityPackageManager.isSuppressedCellularRequestor(
                    req.getRequestorPackageName())) {
                verboseLog(String.format(Locale.US,
                        "handleAddRequest - Suppressing cellular network request from %s",
                        req.getRequestorPackageName()));
            } else {
                mCellularRequests.put(requestId, req);
                mListener.onCellularRequestsChanged(mCellularRequests.size());
            }
        }

        verboseLog(String.format(Locale.US,
                "handleAddRequest - [unmetered %d // highband %d // wifi %d // cell %d ]",
                mUnmeteredRequests.size(),
                mHighBandwidthRequests.size(),
                mWifiRequests.size(),
                mCellularRequests.size()));
    }

    protected void handleRemoveRequest(NetworkRequest req, int requestId) {
        DebugAssert.isMainThread();
        verboseLog("WearNetworkObserver: handleRemoveRequest");
        NetworkRequest unmeteredReq = mUnmeteredRequests.get(requestId);
        if (unmeteredReq != null) {
            mUnmeteredRequests.remove(requestId);
            mListener.onUnmeteredRequestsChanged(mUnmeteredRequests.size());
        }

        NetworkRequest highBandwidthReq = mHighBandwidthRequests.get(requestId);
        if (highBandwidthReq != null) {
            mHighBandwidthRequests.remove(requestId);
            mListener.onHighBandwidthRequestsChanged(mHighBandwidthRequests.size());
        }

        NetworkRequest wifiReq = mWifiRequests.get(requestId);
        if (wifiReq != null) {
            mWifiRequests.remove(requestId);
            mListener.onWifiRequestsChanged(mWifiRequests.size());
        }

        NetworkRequest cellReq = mCellularRequests.get(requestId);
        if (cellReq != null) {
            mCellularRequests.remove(requestId);
            mListener.onCellularRequestsChanged(mCellularRequests.size());
        }

        verboseLog(String.format(Locale.US,
                "handleRemoveRequest - [unmetered %d // highband %d // wifi %d // cell %d ]",
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
     * This method may dump memory-inconsistent values when called off the main thread.
     * This is preferable to synchronizing every call to handleAddRequest/handleRemoveRequest.
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
        for (int i = 0; i < mUnmeteredRequests.size(); i++) {
            NetworkRequest req = mUnmeteredRequests.valueAt(i);
            // extra null-guard in case the object was removed while we were iterating
            if (req != null) {
                ipw.print(req.toString());
            }
        }
        ipw.decreaseIndent();

        ipw.println();
        ipw.println("High-bandwidth requests: ");
        ipw.increaseIndent();
        for (int i = 0; i < mHighBandwidthRequests.size(); i++) {
            // extra null-guard in case the object was removed while we were iterating
            NetworkRequest req = mHighBandwidthRequests.valueAt(i);
            if (req != null) {
                ipw.print(req.toString());
            }
        }
        ipw.decreaseIndent();

        ipw.println();
        ipw.println("Wifi requests: ");
        ipw.increaseIndent();
        for (int i = 0; i < mWifiRequests.size(); i++) {
            // extra null-guard in case the object was removed while we were iterating
            NetworkRequest req = mWifiRequests.valueAt(i);
            if (req != null) {
                ipw.print(req.toString());
            }
        }
        ipw.decreaseIndent();

        ipw.println();
        ipw.println("Cellular requests: ");
        ipw.increaseIndent();
        for (int i = 0; i < mCellularRequests.size(); i++) {
            // extra null-guard in case the object was removed while we were iterating
            NetworkRequest req = mCellularRequests.valueAt(i);
            if (req != null) {
                ipw.print(req.toString());
            }
        }
        ipw.decreaseIndent();
    }
}
