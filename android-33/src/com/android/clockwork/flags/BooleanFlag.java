package com.android.clockwork.flags;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * An observer for a simple boolean flag.
 *
 * <p>If you only need to track a simple boolean flag, you can use use an instance of this class
 * instead of subclassing FeatureFlagsObserver yourself.
 */
public class BooleanFlag extends FeatureFlagsObserver<BooleanFlag.Listener> {
    private static final String TAG = FeatureFlagsObserver.LOG_TAG;

    private final String mFeature;
    private final int mDefault;

    public interface Listener {
        void onFlagChanged(boolean isEnabled);
    }

    /**
     * @param feature    the name of the setting, probably from {@link android.provider.Settings}
     * @param defaultVal the default value to return if the setting hasn't been explicitly populated
     */
    public BooleanFlag(ContentResolver contentResolver, String feature, boolean defaultVal) {
        super(contentResolver);
        mFeature = feature;
        mDefault = defaultVal ? 1 : 0;
    }

    public void register() {
        register(mFeature);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (!featureMatchesUri(mFeature, uri)) {
            Log.w(TAG, String.format(
                "Unexpected feature flag uri encountered for feature %s: %s", mFeature, uri));
            return;
        }

        final boolean enabled = isEnabled();

        if (!Build.TYPE.equals("user") || Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format("Feature flag changed%s: %s=%b",
                            selfChange ? " (self)" : "",
                            mFeature, enabled));
        }

        for (Listener listener : getListeners()) {
            try {
                listener.onFlagChanged(enabled);
            } catch (Exception e) {
                Log.e(TAG, String.format("Listener to %s generated exception", mFeature), e);
            }
        }
    }

    public boolean isEnabled() {
        return getGlobalSettingsInt(mFeature, mDefault) != 0;
    }
}
