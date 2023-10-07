package com.android.clockwork.common;

import android.os.Build;
import android.os.Looper;

/**
 * A class that throws if an assertion fails. These assertions are compiled away in user builds
 * similar to C assertions.
 */
public class DebugAssert {
    private static final boolean IS_USER_BUILD = "user".equals(Build.TYPE);

    // Do not instantiate.
    private DebugAssert() {}

    /**
     * Asserts that execution control is on the main thread.
     */
    public static void isMainThread() {
        if (!IS_USER_BUILD && Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new AssertionError("This function should be called from the main thread.");
        }
    }
}
