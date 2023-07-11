package com.android.clockwork.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;

import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.wearable.resources.R;

import java.util.BitSet;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class provides a single place to fetch or subscribe to power-related info such as whether
 * the device is plugged in or whether we are in power save mode.
 */
public class PowerTracker {
    /* Used in config_wearDozeModeAllowListedFeatures to keep features enabled during full doze */
    public static final int DOZE_MODE_BT_INDEX = 0;
    public static final int DOZE_MODE_WIFI_INDEX = 1;
    public static final int DOZE_MODE_CELLULAR_INDEX = 2;
    public static final int DOZE_MODE_TOUCH_INDEX = 3;
    public static final int MAX_DOZE_MODE_INDEX = 4;
    private static final String TAG = WearPowerConstants.LOG_TAG;
    private final Context mContext;
    private final PowerManager mPowerManager;
    private final AtomicBoolean mIsCharging = new AtomicBoolean(false);
    private final HashSet<Listener> mListeners = new HashSet<>();
    private final BroadcastReceiver chargingStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean newState = false;
            switch (intent.getAction()) {
                case Intent.ACTION_POWER_CONNECTED:
                    newState = true;
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    newState = false;
                    break;
                default:
                    return;
            }

            final boolean prevState = mIsCharging.getAndSet(newState);
            if (prevState != newState) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, String.format("Informing %d listeners of charging state change",
                            mListeners.size()));
                }
                for (Listener listener : mListeners) {
                    listener.onChargingStateChanged();
                }
            }
        }
    };
    private final BroadcastReceiver powerSaveModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED.equals(intent.getAction())) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, String.format("Informing %d listeners of power save mode change",
                            mListeners.size()));
                }
                for (Listener listener : mListeners) {
                    listener.onPowerSaveModeChanged();
                }
            }
        }
    };
    private final BroadcastReceiver deviceIdleModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED.equals(intent.getAction())) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, String.format("Informing %d listeners of device idle mode change",
                            mListeners.size()));
                }
                for (Listener listener : mListeners) {
                    listener.onDeviceIdleModeChanged();
                }
            }
        }
    };
    private final BitSet mDozeModeAllowListedFeatures = new BitSet(MAX_DOZE_MODE_INDEX);

    public PowerTracker(Context context, PowerManager powerManager) {
        mContext = context;
        mPowerManager = powerManager;
    }

    public void onBootCompleted() {
        mIsCharging.set(fetchInitialChargingState(mContext));

        IntentFilter powerIntentFilter = new IntentFilter();
        powerIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        powerIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        mContext.registerReceiver(chargingStateChangeReceiver, powerIntentFilter);

        mContext.registerReceiver(powerSaveModeReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));

        mContext.registerReceiver(deviceIdleModeReceiver,
                new IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED));
        Resources wearableResources = WearResourceUtil.getWearableResources(mContext);
        if (wearableResources != null) {
            populateAllowListedFeatures(wearableResources.getStringArray(
                    R.array.config_wearDozeModeAllowListedFeatures));
        }
    }

    @VisibleForTesting
    void populateAllowListedFeatures(String[] features) {
        for (String feature : features) {
            if ("wifi".equalsIgnoreCase(feature)) {
                mDozeModeAllowListedFeatures.set(DOZE_MODE_WIFI_INDEX);
            } else if ("cellular".equalsIgnoreCase(feature)) {
                mDozeModeAllowListedFeatures.set(DOZE_MODE_CELLULAR_INDEX);
            } else if ("bluetooth".equalsIgnoreCase(feature)) {
                mDozeModeAllowListedFeatures.set(DOZE_MODE_BT_INDEX);
            } else if ("touch".equalsIgnoreCase(feature)) {
                mDozeModeAllowListedFeatures.set(DOZE_MODE_TOUCH_INDEX);
            }
        }
    }

    public BitSet getDozeModeAllowListedFeatures() {
        return mDozeModeAllowListedFeatures;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public boolean isInPowerSave() {
        return mPowerManager.isPowerSaveMode();
    }

    public boolean isCharging() {
        return mIsCharging.get();
    }

    public boolean isDeviceIdle() {
        return mPowerManager.isDeviceIdleMode();
    }

    private boolean fetchInitialChargingState(Context context) {
        // Read the ACTION_BATTERY_CHANGED sticky broadcast for the current
        // battery status.
        Intent batteryStatus =
                context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return false;
        }
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        return (plugged & BatteryManager.BATTERY_PLUGGED_ANY) != 0;
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.print("PowerTracker [ ");
        ipw.printPair("Charging", mIsCharging);
        ipw.printPair("InPowerSaveMode", mPowerManager.isPowerSaveMode());
        ipw.printPair("InDeviceIdleMode", mPowerManager.isDeviceIdleMode());
        ipw.println("]");
    }

    public interface Listener {
        default void onPowerSaveModeChanged() {
        }

        default void onChargingStateChanged() {
        }

        default void onDeviceIdleModeChanged() {
        }
    }
}
