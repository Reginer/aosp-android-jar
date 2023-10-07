package com.android.clockwork.flags;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;

import java.util.HashSet;
import java.util.Set;

/**
 * Common functionality for tracking feature flags from Global Settings.
 *
 * <p>Allows for registration of Listeners and takes care of the details of dealing with feature
 * names.
 *
 * <p>If your feature is controlled by a single flag consider using one of the generic observers in
 * this package (e.g. {@link com.android.clockwork.flags.BooleanFlag}) or creating a new one.
 */
public abstract class FeatureFlagsObserver<Listener> extends ContentObserver {
    public static final String LOG_TAG = "WearFlags";

    private final ContentResolver mContentResolver;
    private final Set<Listener> mListeners;

    FeatureFlagsObserver(ContentResolver contentResolver) {
        super(null);
        mContentResolver = contentResolver;
        mListeners = new HashSet<>();
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    protected Set<Listener> getListeners() {
        return mListeners;
    }

    protected void register(String feature) {
        mContentResolver.registerContentObserver(Settings.Global.getUriFor(feature), false, this);
    }

    protected static boolean featureMatchesUri(String feature, Uri uri) {
        return Settings.Global.getUriFor(feature).equals(uri);
    }

    protected int getGlobalSettingsInt(String key, int fallback) {
        return Settings.Global.getInt(mContentResolver, key, fallback);
    }

    protected long getGlobalSettingsLong(String key, long fallback) {
        return Settings.Global.getLong(mContentResolver, key, fallback);
    }

    @Override
    public abstract void onChange(boolean selfChange, Uri uri);
}
