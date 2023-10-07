package com.android.clockwork.common;

import static android.os.Temperature.THROTTLING_CRITICAL;
import static android.os.Temperature.THROTTLING_EMERGENCY;
import static android.os.Temperature.THROTTLING_NONE;
import static android.os.Temperature.THROTTLING_SEVERE;
import static android.os.Temperature.THROTTLING_SHUTDOWN;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.OnThermalStatusChangedListener;

import com.android.internal.util.IndentingPrintWriter;

import java.util.HashSet;
import java.util.Set;

/** Load and track update to thermal emergency wear settings. */
public class ThermalEmergencyTracker {
    private static final String TAG = "ThermalEmergencyTracker";

    private PowerManager mPowerManager;

    public static final int THERMAL_EMERGENCY_LEVEL_BT = 1;
    public static final int THERMAL_EMERGENCY_LEVEL_WIFI = 2;
    public static final int THERMAL_EMERGENCY_LEVEL_CELL = 4;

    /** Listens to changes in thermal emergency mode. */
    public interface Listener {
        /** Callback to notify thermal emergency mode change. */
        void onThermalEmergencyChanged(ThermalEmergencyMode mode);
    }

    /** Thermal Emergency mode value. */
    public static class ThermalEmergencyMode {
        private int mLevel;

        public ThermalEmergencyMode(int level) {
            mLevel = level;
        }

        /** Set mLevel */
        public void setLevel(int level) {
            mLevel = level;
        }

        /** Return true if emergency mode is enabled */
        public boolean isEnabled() {
            return mLevel != 0;
        }

        /** Return true if emergency mode should have Bluetooth turned off. */
        public boolean isBtEffected() {
            return (mLevel & THERMAL_EMERGENCY_LEVEL_BT) != 0;
        }

        /** Return true if emergency mode should have Wifi turned off. */
        public boolean isWifiEffected() {
            return (mLevel & THERMAL_EMERGENCY_LEVEL_WIFI) != 0;
        }

        /** Return true if emergency mode should have Cellular turned off. */
        public boolean isCellEffected() {
            return (mLevel & THERMAL_EMERGENCY_LEVEL_CELL) != 0;
        }
    }

    private final Set<Listener> mListeners;
    private ThermalEmergencyMode mThermalEmergencyMode;

    public ThermalEmergencyTracker(final Context context) {
        mListeners = new HashSet<>();
        mThermalEmergencyMode = new ThermalEmergencyMode(/* level= */ 0);
        mPowerManager = context.getSystemService(PowerManager.class);
        // Use MainExecutor
        mPowerManager.addThermalStatusListener(new OnThermalStatusChangedListener() {
            @Override
            public void onThermalStatusChanged(int status) {
                int mode_level = 0;
                LogUtil.logD(TAG, "observer onChange %d", status);
                switch(status) {
                    case THROTTLING_SHUTDOWN: // fall-through (trigger same emergency condition)
                    case THROTTLING_EMERGENCY:
                        mode_level = THERMAL_EMERGENCY_LEVEL_BT
                            | THERMAL_EMERGENCY_LEVEL_WIFI
                            | THERMAL_EMERGENCY_LEVEL_CELL;
                        break;
                    case THROTTLING_CRITICAL: // fall-through
                    case THROTTLING_SEVERE: // fall-through
                    case THROTTLING_NONE:
                        break;
                    default:
                        LogUtil.logD(TAG, "Received unknown thermal status");
                        break;
                }
                mThermalEmergencyMode.setLevel(mode_level);
                for (Listener listener : mListeners) {
                    listener.onThermalEmergencyChanged(mThermalEmergencyMode);
                }
            }
        });
    }

    /** Register listtener to receive thermal emergency mode changes. */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /** Return current thermal emergency mode. */
    public ThermalEmergencyMode getThermalEmergencyMode() {
        return mThermalEmergencyMode;
    }



    /** Prints state of the thermal emergency tracker. */
    public void dump(IndentingPrintWriter ipw) {
        ipw.print("ThermalEmergencyTracker [");
        ipw.printPair("Level", mThermalEmergencyMode.mLevel);
        ipw.print("]");
        ipw.println();
    }
}
