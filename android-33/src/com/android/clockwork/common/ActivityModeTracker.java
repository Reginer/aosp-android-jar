package com.android.clockwork.common;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.util.HashSet;
import java.util.Set;

public class ActivityModeTracker {
    private static final String TAG = "WearConnectivityService";

    public interface Listener {
        void onActivityModeChanged(boolean enabled);
    }

    /** valid values for this key are 0 and 1 */
    static final String ACTIVITY_MODE_SETTING_KEY = "clockwork_activity_mode";

    private enum ActivityModeRadio {
        BLUETOOTH,
        CELLULAR,
        WIFI
    }

    private final ContentResolver mContentResolver;
    private final SettingsObserver mSettingsObserver;
    private final Set<Listener> mListeners;
    private final Set<ActivityModeRadio> mAffectedRadios;
    private boolean mActivityModeEnabled;

    public ActivityModeTracker(final Context context) {
        mContentResolver = context.getContentResolver();
        mActivityModeEnabled = fetchActivityModeEnabled();
        mListeners = new HashSet<>();
        mAffectedRadios = new HashSet<>();
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
        mContentResolver.registerContentObserver(
                Settings.System.getUriFor(ACTIVITY_MODE_SETTING_KEY), false, mSettingsObserver);
        populatedAffectedRadios(WearResourceUtil.getWearableResources(context).getStringArray(
                com.android.wearable.resources.R.array.config_wearActivityModeRadios));
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public boolean isActivityModeEnabled() {
        return mActivityModeEnabled;
    }

    public boolean affectsBluetooth() {
        return mAffectedRadios.contains(ActivityModeRadio.BLUETOOTH);
    }

    public boolean affectsWifi() {
        return mAffectedRadios.contains(ActivityModeRadio.WIFI);
    }

    public boolean affectsCellular() {
        return mAffectedRadios.contains(ActivityModeRadio.CELLULAR);
    }

    private void populatedAffectedRadios(String radios[]) {
        for (String radio : radios) {
            if ("wifi".equalsIgnoreCase(radio)) {
                mAffectedRadios.add(ActivityModeRadio.WIFI);
            } else if ("cellular".equalsIgnoreCase(radio)) {
                mAffectedRadios.add(ActivityModeRadio.CELLULAR);
            } else if ("bluetooth".equalsIgnoreCase(radio)) {
                mAffectedRadios.add(ActivityModeRadio.BLUETOOTH);
            }
        }
    }

    private boolean fetchActivityModeEnabled() {
        return Settings.System.getInt(mContentResolver, ACTIVITY_MODE_SETTING_KEY, 0) != 0;
    }

    @VisibleForTesting
    final class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "onChange called");
            if (uri.equals(Settings.System.getUriFor(ACTIVITY_MODE_SETTING_KEY))) {
                boolean activityModeEnabled = fetchActivityModeEnabled();
                if (mActivityModeEnabled != activityModeEnabled) {
                    Log.d(TAG, "ActivityMode changed: " + activityModeEnabled);
                    mActivityModeEnabled = activityModeEnabled;
                    for (Listener listener : mListeners) {
                        listener.onActivityModeChanged(mActivityModeEnabled);
                    }
                }
            }
        }
    }

    public void dump(IndentingPrintWriter ipw) {
        StringBuilder radios = new StringBuilder("(");
        for (ActivityModeRadio radio : mAffectedRadios) {
            radios.append(radio.name());
            radios.append(", ");
        }
        radios.append(")");

        ipw.print("ActivityModeTracker [");
        ipw.printPair("Activity Mode Enabled", mActivityModeEnabled);
        ipw.printPair("Activity Mode Radios", radios.toString());
        ipw.print("]");
        ipw.println();
    }
}
