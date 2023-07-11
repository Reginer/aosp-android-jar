package com.android.clockwork.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Log;

/**
 * Handles all bluetooth-related EventLogging.
 */
public class BluetoothLogger {

    private static final String TAG = WearBluetoothSettings.LOG_TAG;
    private final int COMPANION_TYPE_CLASSIC = 1;
    private final int COMPANION_TYPE_LE = 2;

    private long mCurrentAclConnectionStartTimeMs = -1L;
    private long mCurrentProxyConnectionStartTimeMs = -1L;

    public void logAclConnectionChange(boolean aclConnected) {
        long now = SystemClock.elapsedRealtime();
        if (aclConnected) {
            mCurrentAclConnectionStartTimeMs = now;
        } else {
            if (mCurrentAclConnectionStartTimeMs > 0) {
                long connectionDuration = now - mCurrentAclConnectionStartTimeMs;
                EventLog.writeEvent(EventLogTags.BT_ACL_DISCONNECT, connectionDuration);

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Logging end of ACL Connection session with duration(ms): "
                            + connectionDuration);
                }

                // we should only log one disconnect until the next connect event
                mCurrentAclConnectionStartTimeMs = -1L;
            }
        }
    }

    public void logProxyConnectionChange(boolean isConnected) {
        long now = SystemClock.elapsedRealtime();
        if (isConnected) {
            mCurrentProxyConnectionStartTimeMs = now;
        } else {
            if (mCurrentProxyConnectionStartTimeMs > 0) {
                long connectionDuration = now - mCurrentProxyConnectionStartTimeMs;
                EventLog.writeEvent(EventLogTags.BT_PROXY_DISCONNECT, connectionDuration);

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Logging end of BTProxy session with duration(ms): "
                            + connectionDuration);
                }

                // we should only log one disconnect until the next connect event
                mCurrentProxyConnectionStartTimeMs = -1L;
            }
        }
    }

    /**
     * We probably should not log device names or addresses for a common event such as this.
     */
    public void logCompanionPairingEvent(boolean isBle) {
        int companionType = isBle ? COMPANION_TYPE_LE : COMPANION_TYPE_CLASSIC;
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Logging Companion Pair event of type: " + (isBle ? "BLE" : "CLASSIC"));
        }
        EventLog.writeEvent(EventLogTags.BT_COMPANION_PAIRED, companionType);
    }

    public void logUnexpectedPairingEvent(BluetoothDevice device) {
        EventLog.writeEvent(
                EventLogTags.BT_UNEXPECTED_PAIRING, device.getName(), device.getAddress());
    }
}
