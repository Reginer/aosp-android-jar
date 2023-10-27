/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app.sdksandbox;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Syncs specified keys in default {@link SharedPreferences} to Sandbox.
 *
 * <p>This class is a singleton since we want to maintain sync between app process and sandbox
 * process.
 *
 * @hide
 */
public class SharedPreferencesSyncManager {

    private static final String TAG = "SdkSandboxSyncManager";
    private static ArrayMap<String, SharedPreferencesSyncManager> sInstanceMap = new ArrayMap<>();
    private final ISdkSandboxManager mService;
    private final Context mContext;
    private final Object mLock = new Object();
    private final ISharedPreferencesSyncCallback mCallback = new SharedPreferencesSyncCallback();

    @GuardedBy("mLock")
    private boolean mWaitingForSandbox = false;

    // Set to a listener after initial bulk sync is successful
    @GuardedBy("mLock")
    private ChangeListener mListener = null;

    // Set of keys that this manager needs to keep in sync.
    @GuardedBy("mLock")
    private ArraySet<String> mKeysToSync = new ArraySet<>();

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public SharedPreferencesSyncManager(
            @NonNull Context context, @NonNull ISdkSandboxManager service) {
        mContext = context.getApplicationContext();
        mService = service;
    }

    /**
     * Returns a new instance of this class if there is a new package, otherewise returns a
     * singleton instance.
     */
    public static synchronized SharedPreferencesSyncManager getInstance(
            @NonNull Context context, @NonNull ISdkSandboxManager service) {
        final String packageName = context.getPackageName();
        if (!sInstanceMap.containsKey(packageName)) {
            sInstanceMap.put(packageName, new SharedPreferencesSyncManager(context, service));
        }
        return sInstanceMap.get(packageName);
    }

    /**
     * Adds keys for syncing from app's default {@link SharedPreferences} to SdkSandbox.
     *
     * @see SdkSandboxManager#addSyncedSharedPreferencesKeys(Set)
     */
    public void addSharedPreferencesSyncKeys(@NonNull Set<String> keyNames) {
        // TODO(b/239403323): Validate the parameters in SdkSandboxManager
        synchronized (mLock) {
            mKeysToSync.addAll(keyNames);

            if (mListener == null) {
                mListener = new ChangeListener();
                getDefaultSharedPreferences().registerOnSharedPreferenceChangeListener(mListener);
            }
            syncData();
        }
    }

    /**
     * Removes keys from set of keys that have been added using {@link
     * #addSharedPreferencesSyncKeys(Set)}
     *
     * @see SdkSandboxManager#removeSyncedSharedPreferencesKeys(Set)
     */
    public void removeSharedPreferencesSyncKeys(@NonNull Set<String> keys) {
        synchronized (mLock) {
            mKeysToSync.removeAll(keys);

            final ArrayList<SharedPreferencesKey> keysWithTypeBeingRemoved = new ArrayList<>();

            for (final String key : keys) {
                keysWithTypeBeingRemoved.add(
                        new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_STRING));
            }
            final SharedPreferencesUpdate update =
                    new SharedPreferencesUpdate(keysWithTypeBeingRemoved, new Bundle());
            try {
                mService.syncDataFromClient(
                        mContext.getPackageName(),
                        /*timeAppCalledSystemServer=*/ System.currentTimeMillis(),
                        update,
                        mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Couldn't connect to SdkSandboxManagerService: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the set of all keys that are being synced from app's default {@link
     * SharedPreferences} to sandbox.
     */
    public Set<String> getSharedPreferencesSyncKeys() {
        synchronized (mLock) {
            return new ArraySet(mKeysToSync);
        }
    }

    /**
     * Returns true if sync is in waiting state.
     *
     * <p>Sync transitions into waiting state whenever sdksandbox is unavailable. It resumes syncing
     * again when SdkSandboxManager notifies us that sdksandbox is available again.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean isWaitingForSandbox() {
        synchronized (mLock) {
            return mWaitingForSandbox;
        }
    }

    /**
     * Syncs data to SdkSandbox.
     *
     * <p>Syncs values of specified keys {@link #mKeysToSync} from the default {@link
     * SharedPreferences} of the app.
     *
     * <p>Once bulk sync is complete, it also registers listener for updates which maintains the
     * sync.
     */
    private void syncData() {
        synchronized (mLock) {
            // Do not sync if keys have not been specified by the client.
            if (mKeysToSync.isEmpty()) {
                return;
            }

            bulkSyncData();
        }
    }

    @GuardedBy("mLock")
    private void bulkSyncData() {
        // Collect data in a bundle
        final Bundle data = new Bundle();
        final SharedPreferences pref = getDefaultSharedPreferences();
        final Map<String, ?> allData = pref.getAll();
        final ArrayList<SharedPreferencesKey> keysWithTypeBeingSynced = new ArrayList<>();

        for (int i = 0; i < mKeysToSync.size(); i++) {
            final String key = mKeysToSync.valueAt(i);
            final Object value = allData.get(key);
            if (value == null) {
                // Keep the key missing from the bundle; that means key has been removed.
                // Type of missing key doesn't matter, so we use a random type.
                keysWithTypeBeingSynced.add(
                        new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_STRING));
                continue;
            }
            final SharedPreferencesKey keyWithTypeAdded = updateBundle(data, key, value);
            keysWithTypeBeingSynced.add(keyWithTypeAdded);
        }

        final SharedPreferencesUpdate update =
                new SharedPreferencesUpdate(keysWithTypeBeingSynced, data);
        try {
            mService.syncDataFromClient(
                    mContext.getPackageName(),
                    /*timeAppCalledSystemServer=*/ System.currentTimeMillis(),
                    update,
                    mCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't connect to SdkSandboxManagerService: " + e.getMessage());
        }
    }

    private SharedPreferences getDefaultSharedPreferences() {
        final Context appContext = mContext.getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    private class ChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences pref, @Nullable String key) {
            // Sync specified keys only
            synchronized (mLock) {
                // Do not sync if we are in waiting state
                if (mWaitingForSandbox) {
                    return;
                }

                if (key == null) {
                    // All keys have been cleared. Bulk sync so that we send null for every key.
                    bulkSyncData();
                    return;
                }

                if (!mKeysToSync.contains(key)) {
                    return;
                }

                final Bundle data = new Bundle();
                SharedPreferencesKey keyWithType;
                final Object value = pref.getAll().get(key);
                if (value != null) {
                    keyWithType = updateBundle(data, key, value);
                } else {
                    keyWithType =
                            new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_STRING);
                }

                final SharedPreferencesUpdate update =
                        new SharedPreferencesUpdate(List.of(keyWithType), data);
                try {
                    mService.syncDataFromClient(
                            mContext.getPackageName(),
                            /*timeAppCalledSystemServer=*/ System.currentTimeMillis(),
                            update,
                            mCallback);
                } catch (RemoteException e) {
                    Log.e(TAG, "Couldn't connect to SdkSandboxManagerService: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Adds key to bundle based on type of value
     *
     * @return SharedPreferenceKey of the key that has been added
     */
    @GuardedBy("mLock")
    private SharedPreferencesKey updateBundle(Bundle data, String key, Object value) {
        final String type = value.getClass().getSimpleName();
        try {
            switch (type) {
                case "String":
                    data.putString(key, value.toString());
                    return new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_STRING);
                case "Boolean":
                    data.putBoolean(key, (Boolean) value);
                    return new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_BOOLEAN);
                case "Integer":
                    data.putInt(key, (Integer) value);
                    return new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_INTEGER);
                case "Float":
                    data.putFloat(key, (Float) value);
                    return new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_FLOAT);
                case "Long":
                    data.putLong(key, (Long) value);
                    return new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_LONG);
                case "HashSet":
                    // TODO(b/239403323): Verify the set contains string
                    data.putStringArrayList(key, new ArrayList<>((Set<String>) value));
                    return new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_STRING_SET);
                default:
                    Log.e(
                            TAG,
                            "Unknown type found in default SharedPreferences for Key: "
                                    + key
                                    + " type: "
                                    + type);
            }
        } catch (ClassCastException ignore) {
            data.remove(key);
            Log.e(
                    TAG,
                    "Wrong type found in default SharedPreferences for Key: "
                            + key
                            + " Type: "
                            + type);
        }
        // By default, assume it's string
        return new SharedPreferencesKey(key, SharedPreferencesKey.KEY_TYPE_STRING);
    }

    private class SharedPreferencesSyncCallback extends ISharedPreferencesSyncCallback.Stub {
        @Override
        public void onSandboxStart() {
            synchronized (mLock) {
                if (mWaitingForSandbox) {
                    // Retry bulk sync if we were waiting for sandbox to start
                    mWaitingForSandbox = false;
                    bulkSyncData();
                }
            }
        }

        @Override
        public void onError(int errorCode, String errorMsg) {
            synchronized (mLock) {
                // Transition to waiting state when sandbox is unavailable
                if (!mWaitingForSandbox
                        && errorCode == ISharedPreferencesSyncCallback.SANDBOX_NOT_AVAILABLE) {
                    Log.w(TAG, "Waiting for SdkSandbox: " + errorMsg);
                    // Wait for sandbox to start. When it starts, server will call onSandboxStart
                    mWaitingForSandbox = true;
                    return;
                }
                Log.e(TAG, "errorCode: " + errorCode + " errorMsg: " + errorMsg);
            }
        }
    }
}
