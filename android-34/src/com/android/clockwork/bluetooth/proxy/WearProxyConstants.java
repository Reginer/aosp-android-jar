package com.android.clockwork.bluetooth.proxy;

import android.os.ParcelUuid;

/** Constants for wear proxy modules */
public class WearProxyConstants {
    private WearProxyConstants () { }

    public static final String LOG_TAG = "WearBluetoothProxy";

    public static final String PROXY_NETWORK_TYPE_NAME = "COMPANION_PROXY";
    public static final String PROXY_NETWORK_SUBTYPE_NAME = "";
    public static final int PROXY_NETWORK_SUBTYPE_ID = 0;
    public static final ParcelUuid PROXY_UUID_V1 =
        ParcelUuid.fromString("fafbdd20-83f0-4389-addf-917ac9dae5b2");

    /** Reasons for sysproxy connection or disconnection events */
    public static final class Reason {
        public static final String CLOSABLE = "Closable";
        public static final String SYSPROXY_WAS_CONNECTED = "Sysproxy Previously Connected";
        public static final String SYSPROXY_CONNECTED = "Sysproxy Connected";
        public static final String SYSPROXY_NO_INTERNET = "Phone no internet";
        public static final String SYSPROXY_DISCONNECTED = "Sysproxy Disconnected";
    }
}
