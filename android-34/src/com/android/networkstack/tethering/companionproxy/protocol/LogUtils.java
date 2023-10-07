package com.android.networkstack.tethering.companionproxy.protocol;

import android.util.Log;

import java.util.Locale;

/**
 * Implements helper functions for all logging in Companion Proxy code.
 *
 * @hide
 */
public final class LogUtils {
    public static final String TAG = "CompanionProxy";

    public static boolean verbose() {
        return Log.isLoggable(TAG, Log.VERBOSE);
    }

    public static boolean debug() {
        return Log.isLoggable(TAG, Log.DEBUG);
    }

    public static String bytesToString(byte[] data, int pos, int len, int maxLen) {
        StringBuilder sb = new StringBuilder();
        final int printLen = Math.min(len, maxLen);
        for (int i = 0; i < printLen; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append("0x");
            sb.append(Integer.toHexString(data[pos + i] & 0xFF).toUpperCase(Locale.US));
        }
        return sb.toString();
    }

    private LogUtils() {}
}
