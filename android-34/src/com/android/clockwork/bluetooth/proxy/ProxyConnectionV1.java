package com.android.clockwork.bluetooth.proxy;

import android.annotation.WorkerThread;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

/**
 * Manages connection to the companion SysProxy network over legacy V1/1.5 protocol.
 */
public class ProxyConnectionV1 implements ProxyConnection {
    private static final String TAG = WearProxyConstants.LOG_TAG;

    private final int mVersionCode;
    private final Listener mListener;

    static native void classInitNative();
    @VisibleForTesting native int connectNative(int fd, int sysproxyVersion);
    @VisibleForTesting native int continueConnectNative();
    @VisibleForTesting native boolean disconnectNative();

    static {
        try {
            System.loadLibrary("wear-bluetooth-proxyv1-jni");
            classInitNative();
        } catch (UnsatisfiedLinkError e) {
            // Invoked during testing
            Log.e(TAG, "Unable to load wear bluetooth sysproxy jni native"
                    + " libraries");
        }
    }

    public ProxyConnectionV1(int versionCode, Listener listener) {
        mVersionCode = versionCode;
        mListener = listener;
    }

    @Override
    public int connect(ParcelFileDescriptor fd) {
        final int intFd = fd.detachFd();
        return connectNative(intFd, mVersionCode);
    }

    @Override
    public int continueConnect() {
        return continueConnectNative();
    }

    @Override
    public boolean disconnect() {
        return disconnectNative();
    }

    @Override
    public String getInterfaceName() {
        return "lo";
    }

    @Override
    public int getMtu() {
        return 1500;
    }

    /**
     * This method is called from JNI in a background thread when the companion proxy
     * network state changes on the phone.
     */
    @WorkerThread
    protected void onActiveNetworkState(@NetworkType final int networkType,
            final boolean isMetered) {
        mListener.onActiveNetworkState(networkType, isMetered);
    }

    /** This method is called from JNI in a background thread when the proxy has disconnected. */
    @WorkerThread
    protected void onDisconnect(final int status) {
        mListener.onDisconnect(status, "unknown");
    }

    @Override
    public void dump(IndentingPrintWriter ipw) {
    }
}
