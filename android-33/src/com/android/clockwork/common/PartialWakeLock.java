package com.android.clockwork.common;

import android.content.Context;
import android.os.PowerManager;

/**
 * Simple, mockable wrapper for a partial wakelock.
 */
public class PartialWakeLock {

    private final PowerManager.WakeLock mWakeLock;

    public PartialWakeLock(Context context, String tag) {
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
    }

    public void acquire() {
        mWakeLock.acquire();
    }

    public void release() {
        mWakeLock.release();
    }
}
