package com.android.clockwork.common;

import android.app.AlarmManager;
import android.os.SystemClock;

import com.android.internal.util.IndentingPrintWriter;

/**
 * Debounce proxy connectivity status changes produce stable proxy connected signal for
 * Wifi and Cell radio policies.
 */
public class ProxyConnectivityDebounce {
    private static final String TAG = "WearConnectivityService";

    /** Listener of debounced proxy connectivity. */
    public interface Listener {
        /** called when proxy connectivity change observed. */
        void onProxyConnectedChange(boolean isConnected);
    }

    /** Status of companion proxy connection. */
    public static class ProxyStatus {
        public final boolean isConnected;
        public final boolean isBluetoothAdapterEnabled;
        public final boolean hasInternet;

        public ProxyStatus(boolean isConnected, boolean isBluetoothAdapterEnabled,
                boolean hasInternet) {
            this.isConnected = isConnected;
            this.isBluetoothAdapterEnabled = isBluetoothAdapterEnabled;
            this.hasInternet = hasInternet;
        }
    }

    private final String mName;
    private final AlarmManager mAlarmManager;
    private final Listener mListener;
    private final long mDelay;
    private AlarmManager.OnAlarmListener mDelayProxyStatusAlarm;

    public ProxyConnectivityDebounce(String name, AlarmManager alarmManager,
            Listener listener, long delay) {
        mName = name;
        mAlarmManager = alarmManager;
        mListener = listener;
        mDelay = delay;
    }

    /**
     * Process proxy status change.
     *
     * Run debounce logic if needed and update listeners about
     * changes to proxy connectivity.
     */
    public void updateProxyStatus(ProxyStatus proxyStatus) {
        if (mListener == null) {
            return;
        }

        if (mDelayProxyStatusAlarm != null) {
            mAlarmManager.cancel(mDelayProxyStatusAlarm);
            mDelayProxyStatusAlarm = null;
        }

        if (proxyStatus.isConnected || !proxyStatus.isBluetoothAdapterEnabled) {
            mListener.onProxyConnectedChange(proxyStatus.isConnected);
        } else {
            // delay BT disconnects that could be intermitten.
            mDelayProxyStatusAlarm = () -> {
                if (mDelayProxyStatusAlarm == null) {
                    return;
                }

                mListener.onProxyConnectedChange(false);
                mAlarmManager.cancel(mDelayProxyStatusAlarm);
                mDelayProxyStatusAlarm = null;
            };
            LogUtil.logD(TAG, "updateProxyStatus: %s delay by %s", mName, mDelay);
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + mDelay,
                    "ProxyStatusDelay." + mName, mDelayProxyStatusAlarm, null);
        }
    }

    /**
     * Extend delay timer.
     *
     * Extends delay of currently active proxy update by specified delay.
     * This can be used to modify delay for a specific instance of
     * proxy status update, for example to modify cell delay when wifi
     * was connected.
     */
    public void extend(long delayMillis) {
        if (mDelayProxyStatusAlarm != null) {
            mAlarmManager.cancel(mDelayProxyStatusAlarm);
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delayMillis,
                    "ProxyStatusDelay." + mName, mDelayProxyStatusAlarm, null);
            LogUtil.logD(TAG, "extend: %s delay by %s", mName, delayMillis);
        } else {
            LogUtil.logD(TAG, "extend: %s update expired", mName);
        }
    }

    /** Print debug information about state of the debounce */
    public void dump(IndentingPrintWriter ipw) {
        ipw.print("ProxyConnectivityDebounce ");
        ipw.print(mName);
        ipw.print("[");
        ipw.printPair("delay", mDelay);
        ipw.print(mDelayProxyStatusAlarm == null ? "inactive" : "update delayed");
        ipw.print("]");
        ipw.println();
    }
}
