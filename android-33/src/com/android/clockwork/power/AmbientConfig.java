package com.android.clockwork.power;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.Settings;

import com.android.internal.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

/** Provides access to Ambient Mode's configuration. */
public class AmbientConfig {
    private static final String TAG = WearPowerConstants.LOG_TAG;

    public interface Listener {
        void onAmbientConfigChanged();
    }

    private static final int DEFAULT_IS_TOUCH_TO_WAKE = 1;

    private static final int DEFAULT_IS_TILT_TO_WAKE = 1;

    private static final int DEFAULT_IS_USER_TILT_TO_BRIGHT = 0;

    private static final int DEFAULT_IS_AMBIENT_ENABLED = 1;

    private static final int DEFAULT_IS_WATCHFACE_DECOMPOSABLE = 0;

    private final ContentResolver mContentResolver;
    private final List<Listener> mListeners = new ArrayList<>();

    public AmbientConfig(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    public boolean isTouchToWake() {
        return Settings.Global.getInt(
                        mContentResolver,
                        Settings.Global.Wearable.AMBIENT_TOUCH_TO_WAKE,
                        DEFAULT_IS_TOUCH_TO_WAKE)
                == 1;
    }

    public boolean isTiltToWake() {
        return Settings.Global.getInt(
                        mContentResolver,
                        Settings.Global.Wearable.AMBIENT_TILT_TO_WAKE,
                        DEFAULT_IS_TILT_TO_WAKE)
                == 1;
    }

    public boolean isUserTiltToBright() {
        return Settings.Global.getInt(
                        mContentResolver,
                        Settings.Global.Wearable.AMBIENT_TILT_TO_BRIGHT,
                        DEFAULT_IS_USER_TILT_TO_BRIGHT)
                == 1;
    }

    public boolean isAmbientEnabled() {
        return Settings.Global.getInt(
                        mContentResolver,
                        Settings.Global.Wearable.AMBIENT_ENABLED,
                        DEFAULT_IS_AMBIENT_ENABLED)
                == 1;
    }

    public boolean isWatchfaceDecomposable() {
        return Settings.Global.getInt(
                        mContentResolver,
                        Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE,
                        DEFAULT_IS_WATCHFACE_DECOMPOSABLE)
                == 1;
    }

    public void setTiltToWake(boolean enabled) {
        Settings.Global.putInt(
                mContentResolver, Settings.Global.Wearable.AMBIENT_TILT_TO_WAKE, enabled ? 1 : 0);
    }

    public void register() {
        final ContentObserver observer =
                new ContentObserver(null) {
                    @Override
                    public void onChange(boolean selfChange) {
                        onSettingsChanged();
                    }
                };
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.Wearable.AMBIENT_TOUCH_TO_WAKE),
                false /* notifyForDescendants */,
                observer);
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.Wearable.AMBIENT_TILT_TO_WAKE),
                false /* notifyForDescendants */,
                observer);
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.Wearable.AMBIENT_TILT_TO_BRIGHT),
                false /* notifyForDescendants */,
                observer);
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.Wearable.AMBIENT_ENABLED),
                false /* notifyForDescendants */,
                observer);
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE),
                false /* notifyForDescendants */,
                observer);
    }

    public void addListener(Listener listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    private void onSettingsChanged() {
        synchronized (mListeners) {
            for (Listener listener : mListeners) {
                listener.onAmbientConfigChanged();
            }
        }
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.print("AmbientConfig [ ");
        ipw.printPair("Enable", isAmbientEnabled());
        ipw.printPair("TiltToWake", isTiltToWake());
        ipw.printPair("TouchToWake", isTouchToWake());
        ipw.printPair("TiltToBright", isUserTiltToBright());
        ipw.printPair("DecomposableWatchface", isWatchfaceDecomposable());
        ipw.println("]");
    }
}
