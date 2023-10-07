package com.android.clockwork.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;

import com.android.clockwork.common.WearResourceUtil;
import com.android.clockwork.setup.PostSetupPackageHelper;
import com.android.internal.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Supports constant observation of {@link Settings} updates. */
class WearPersistentSettingsObserver {
    private final Context mContext;
    private final Set<Observer> mObservers;

    /** Entity that observes {@link Settings} updates. */
    static interface Observer {
        /** Called when the target {@link Settings} value change. */
        void onChange();
        /** The URI for the target {@link Settings}. */
        Uri getUri();
        /**
         * Determines whether or not the {@link Observer#onChange} logic should run at the start
         * of observing the Setting.
         *
         * @return {@code true} if the change-logic should run at start, {@code false} otherwise.
         */
        default boolean shouldTriggerObserverOnStart() {
            return false;
        }
    }

    @VisibleForTesting
    WearPersistentSettingsObserver(Context context, Set<Observer> observers) {
        mContext = context;
        mObservers = new HashSet<>();
        mObservers.addAll(observers);
    }

    @VisibleForTesting
    WearPersistentSettingsObserver(Context context, Resources resources) {
        this(context, getObserversToRegister(context, resources));
    }

    WearPersistentSettingsObserver(Context context) {
        this(context, WearResourceUtil.getWearableResources(context));
    }

    void startObserving() {
        // Map each target URI to a set of observers targeting it.
        Map<Uri, Set<Observer>> uriToObservers = new HashMap<>();
        mObservers.forEach(observer -> {
            if (observer.shouldTriggerObserverOnStart()) {
                observer.onChange();
            }
            Uri uri = observer.getUri();
            if (!uriToObservers.containsKey(uri)) {
                uriToObservers.put(uri, new HashSet<>());
            }
            uriToObservers.get(uri).add(observer);
        });

        ContentResolver contentResolver = mContext.getContentResolver();
        // Register a content observer per unique URI.
        uriToObservers.keySet().forEach(uri -> {
            contentResolver.registerContentObserver(uri,
                    false,
                    new ContentObserver(null) {
                        @Override
                        public void onChange(boolean selfChange) {
                            uriToObservers.get(uri).forEach(Observer::onChange);
                        }
                    });
        });
    }

    @VisibleForTesting
    Set<Observer> getObservers() {
        return mObservers;
    }

    private static Set<Observer> getObserversToRegister(Context context, Resources resources) {
        Set<Observer> observers = new HashSet<>();

        observers.add(new CombinedLocationSettingObserver(context));

        String[] postSetupPackageConfigs = PostSetupPackageHelper.getPackageConfigs(resources);
        if (postSetupPackageConfigs != null && postSetupPackageConfigs.length > 0) {
            observers.add(new PairedDeviceOsTypeObserver(context));
        }

        return observers;
    }
}