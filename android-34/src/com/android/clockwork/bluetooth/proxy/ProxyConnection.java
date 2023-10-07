package com.android.clockwork.bluetooth.proxy;

import android.os.ParcelFileDescriptor;

import com.android.internal.util.IndentingPrintWriter;

/**
 * Manages connection to the SysProxy on the phone.
 *
 * This class handles SysProxy communication with the Phone using
 * Bluetooth.
 */
public interface ProxyConnection {
    /**
     * Current active value of {@link ConnectivityManager#TYPE} from
     * {@link NetworkInfo#getType} on phone.
     **/
    @interface NetworkType {}

    interface Listener {
        /**
         * This method is called in a background thread when the companion proxy
         * network state changes on the phone.
         */
        void onActiveNetworkState(@NetworkType int networkType, boolean isMetered);

        /** This method is called in a background thread when the proxy has disconnected. */
        void onDisconnect(int status, String reason);
    }

    public static final int CONNECT_RESULT_CONNECTED = 0;
    public static final int CONNECT_RESULT_TIMEOUT = 1;
    public static final int CONNECT_RESULT_FAILED = -1;

    /** Starts connection process over the given Bluetooth stream. */
    int connect(ParcelFileDescriptor fd);

    /** Continues connection process if connect() has returned CONNECT_RESULT_TIMEOUT. */
    int continueConnect();

    /**
     * Shuts down SysProxy connection.
     *
     * Returns false if the local SysProxy is inaccessible and should be considered
     * shut down. Returns true if local SysProxy is reachable.
     */
    boolean disconnect();

    /**
     * Returns the routing interface name.
     *
     * Safe to call any time and any thread.
     */
    String getInterfaceName();

    /**
     * Returns the routing interface's MTU.
     *
     * Safe to call any time and any thread.
     */
    int getMtu();

    void dump(IndentingPrintWriter ipw);
}
