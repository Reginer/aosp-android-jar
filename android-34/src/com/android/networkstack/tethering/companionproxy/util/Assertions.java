package com.android.networkstack.tethering.companionproxy.util;

import android.os.Build;

/**
 * Implements basic assert functions for runtime error-checking.
 *
 * @hide
 */
public final class Assertions {
    public static final boolean IS_USER_BUILD = "user".equals(Build.TYPE);

    public static void throwsIfOutOfBounds(int totalLength, int pos, int len) {
        if (!IS_USER_BUILD && ((totalLength | pos | len) < 0 || pos > totalLength - len)) {
            throw new ArrayIndexOutOfBoundsException(
                "length=" + totalLength + "; regionStart=" + pos + "; regionLength=" + len);
        }
    }

    public static void throwsIfOutOfBounds(byte[] buffer, int pos, int len) {
        throwsIfOutOfBounds(buffer != null ? buffer.length : 0, pos, len);
    }

    private Assertions() {}
}
