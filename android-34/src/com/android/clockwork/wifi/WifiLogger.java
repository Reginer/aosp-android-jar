package com.android.clockwork.wifi;

import android.os.SystemClock;
import android.util.EventLog;
import android.util.Log;

/**
 * Handles all wifi-related EventLogging.
 */
public class WifiLogger {
    private static final String TAG = "WearWifiMediator";

    // WiFi adapter is OFF.
    private final int OFF = 0;
    // WiFi adapter is ON but IDLE (not trying to gain a network).
    private final int ON_IDLE = 1;
    // WiFi adapter is ON and SCANNING (trying to gain a network).
    private final int ON_SCANNING = 2;
    // WiFi adapter is ON and connected to a network.
    private final int ON_CONNECTED = 3;

    private int mCurrentState;
    private long mCurrentStateStartTimeMs;

    public WifiLogger() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "WifiLogger starting in OFF state.");
        }
        mCurrentState = OFF;
        mCurrentStateStartTimeMs = SystemClock.elapsedRealtime();
    }

    /**
     * This method will only record actual state transitions.
     */
    public void recordWifiState(boolean wifiAdapterOn, boolean wifiConnected, boolean wifiWanted) {
        long now = SystemClock.elapsedRealtime();

        int nextState;
        if (wifiAdapterOn) {
            if (wifiConnected) {
                nextState = ON_CONNECTED;
            } else {
                nextState = wifiWanted ? ON_SCANNING : ON_IDLE;
            }
        } else {
            nextState = OFF;
        }

        if (nextState != mCurrentState) {
            long duration = now - mCurrentStateStartTimeMs;
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Logging previous wifi session: " + stateString(mCurrentState)
                        + " ; duration(ms): " + duration);
            }
            EventLog.writeEvent(
                    EventLogTags.WIFI_SESSION,
                    mCurrentState,
                    duration);
            mCurrentState = nextState;
            mCurrentStateStartTimeMs = now;
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Begin next wifi session: " + stateString(mCurrentState));
            }
        }
    }

    public void recordWifiBackoffEvent() {
        EventLog.writeEvent(EventLogTags.WIFI_BACKOFF);
    }

    private String stateString(int state) {
        switch (state) {
            case OFF:
                return "OFF";
            case ON_IDLE:
                return "ON_IDLE";
            case ON_SCANNING:
                return "ON_SCANNING";
            case ON_CONNECTED:
                return "ON_CONNECTED";
            default:
                return "INVALID";
        }
    }
}
