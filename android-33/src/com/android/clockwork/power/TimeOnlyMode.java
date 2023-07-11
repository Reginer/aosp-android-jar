package com.android.clockwork.power;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.util.HashSet;

/**
 * Utility class for accessing Time Only Mode configuration.
 */
public class TimeOnlyMode implements PowerTracker.Listener {

    public interface Listener {
        void onTimeOnlyModeChanged(boolean timeOnly);
    }

    private static final String TAG = "TOMConfig";
    // keys are also documented at Settings.Global.TIME_ONLY_MODE_CONSTANTS
    /** If {@code true}, TOM features will be enabled. */
    @VisibleForTesting
    static final String KEY_ENABLED = "enabled";
    /**
     * If {@code true}, when TOM is enabled, Home should be disabled and replaced with a lightweight
     * version.
     */
    @VisibleForTesting
    static final String KEY_DISABLE_HOME = "disable_home";
    /** If {@code true}, when TOM is enabled, tilt-to-wake will be disabled. */
    @VisibleForTesting
    static final String KEY_DISABLE_TILT_TO_WAKE = "disable_tilt_to_wake";
    /** If {@code true}, when TOM is enabled, touch-to-wake will be disabled. */
    @VisibleForTesting
    static final String KEY_DISABLE_TOUCH_TO_WAKE = "disable_touch_to_wake";

    // set of default configs
    private static final boolean DEFAULT_ENABLED = true;
    private static final boolean DEFAULT_DISABLE_HOME = false;
    private static final boolean DEFAULT_DISABLE_TILT_TO_WAKE = true;
    private static final boolean DEFAULT_DISABLE_TOUCH_TO_WAKE = true;

    private final PowerTracker mPowerTracker;
    private final HashSet<Listener> mListeners = new HashSet<>();

    private final ContentResolver mContentResolver;
    private final KeyValueListParser mParser;

    public TimeOnlyMode(Context context) {
        this(
                context.getContentResolver(),
                new PowerTracker(context, context.getSystemService(PowerManager.class)));
    }

    /**
     * Instantiates TimeOnlyMode with the supplied ContentResolver and PowerTracker instance.
     *
     * <p>This constructor is preferable in when an existing PowerTracker instance is already in use
     * elsewhere.
     */
    public TimeOnlyMode(ContentResolver contentResolver, PowerTracker powerTracker) {
        mContentResolver = contentResolver;
        mPowerTracker = powerTracker;
        mParser = new KeyValueListParser(',');

        mPowerTracker.addListener(this);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public void onPowerSaveModeChanged() {
        if (isFeatureSupported()) {
            for (Listener listener : mListeners) {
                listener.onTimeOnlyModeChanged(isInTimeOnlyMode());
            }
        }
    }

    @Override
    public void onChargingStateChanged() {
        // TODO: do we stay in TimeOnlyMode even when device is on charger?
    }

    /** Update key value parser with new string from settings store. */
    protected void updateParser() {
        try {
            mParser.setString(Settings.Global.getString(
                    mContentResolver, Settings.Global.TIME_ONLY_MODE_CONSTANTS));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "could not parse TOM config", e);
        }
    }

    /** Returns {@code true} if Time Only Mode feature is enabled. */
    public boolean isFeatureSupported() {
        updateParser();
        return mParser.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    /** Returns {@code true} if Time Only Mode should disable home when it is enabled and active. */
    public boolean isDisableHomeFeatureEnabled() {
        updateParser();
        return mParser.getBoolean(KEY_DISABLE_HOME, DEFAULT_DISABLE_HOME);
    }

    /** Returns {@code true} if device is in Time Only Mode and should only show time. */
    public boolean isInTimeOnlyMode() {
        return isFeatureSupported() && mPowerTracker.isInPowerSave();
    }

    /** Returns {@code true} if device is in Time Only Mode and should disable Home. */
    public boolean isHomeDisabled() {
        return isInTimeOnlyMode() && mParser.getBoolean(KEY_DISABLE_HOME, DEFAULT_DISABLE_HOME);
    }

    /** Returns {@code true} if tilt-to-wake should be disabled because Time Only Mode is on. */
    public boolean isTiltToWakeDisabled() {
        updateParser();
        return mPowerTracker.isInPowerSave()
                && mParser.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
                && mParser.getBoolean(KEY_DISABLE_TILT_TO_WAKE, DEFAULT_DISABLE_TILT_TO_WAKE);
    }

    /** Returns {@code true} if touch-to-wake should be disabled because Time Only Mode is on. */
    public boolean isTouchToWakeDisabled() {
        updateParser();
        return mPowerTracker.isInPowerSave()
                && mParser.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
                && mParser.getBoolean(KEY_DISABLE_TOUCH_TO_WAKE, DEFAULT_DISABLE_TOUCH_TO_WAKE);
    }

    /** Register content observer for changes in time only mode configuration. */
    public void registerContentObserver(ContentObserver contentObserver) {
        Uri configUri = Settings.Global.getUriFor(Settings.Global.TIME_ONLY_MODE_CONSTANTS);
        if (configUri == null) {
            Log.e(TAG, "could not obtain TOM URI");
        } else {
            mContentResolver.registerContentObserver(configUri, false, contentObserver);
        }
    }

    /** Unregister content observer for changes in time only mode configuration. */
    public void unregisterContentObserver(ContentObserver contentObserver) {
        mContentResolver.unregisterContentObserver(contentObserver);
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.println("TimeOnlyMode [");
        ipw.increaseIndent();
        ipw.printPair("isInTimeOnlyMode", isInTimeOnlyMode());
        ipw.printPair("isHomeDisabled", isHomeDisabled());
        ipw.println();
        ipw.printPair("isTiltToWakeDisabled", isTiltToWakeDisabled());
        ipw.printPair("isTouchToWakeDisabled()", isTouchToWakeDisabled());
        ipw.decreaseIndent();
        ipw.println();
        ipw.println("]");
    }
}
