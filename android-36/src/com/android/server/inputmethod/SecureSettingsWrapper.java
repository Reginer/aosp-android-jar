/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.inputmethod;

import android.annotation.AnyThread;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.content.ContentResolver;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.internal.annotations.GuardedBy;
import com.android.server.LocalServices;
import com.android.server.pm.UserManagerInternal;

/**
 * A thread-safe utility class to encapsulate accesses to {@link Settings.Secure} that may need a
 * special handling for direct-boot support.
 *
 * <p>Any changes made until the user storage is unlocked are non-persistent and will be reset
 * to the persistent value when the user storage is unlocked.</p>
 */
final class SecureSettingsWrapper {

    private static final Object sMutationLock = new Object();

    @NonNull
    private static volatile ImmutableSparseArray<ReaderWriter> sUserMap =
            ImmutableSparseArray.empty();

    @Nullable
    private static volatile ContentResolver sContentResolver = null;

    private static volatile boolean sTestMode = false;

    /**
     * Can be called from unit tests to start the test mode, where a fake implementation will be
     * used instead.
     *
     * <p>The fake implementation is just an {@link ArrayMap}. By default it is empty, and the data
     * written can be read back later.</p>
     */
    @AnyThread
    static void startTestMode() {
        sTestMode = true;
    }

    /**
     * Can be called from unit tests to end the test mode, where a fake implementation will be used
     * instead.
     */
    @AnyThread
    static void endTestMode() {
        synchronized (sMutationLock) {
            sUserMap = ImmutableSparseArray.empty();
        }
        sTestMode = false;
    }

    /**
     * Not intended to be instantiated.
     */
    private SecureSettingsWrapper() {
    }

    private static final ArraySet<String> CLONE_TO_MANAGED_PROFILE = new ArraySet<>();
    static {
        Settings.Secure.getCloneToManagedProfileSettings(CLONE_TO_MANAGED_PROFILE);
    }

    @AnyThread
    @UserIdInt
    private static int getUserIdForClonedSettings(@NonNull String key, @UserIdInt int userId) {
        return CLONE_TO_MANAGED_PROFILE.contains(key)
                ? LocalServices.getService(UserManagerInternal.class).getProfileParentId(userId)
                : userId;
    }

    private interface ReaderWriter {
        @AnyThread
        void putString(@NonNull String key, @Nullable String value);

        @AnyThread
        @Nullable
        String getString(@NonNull String key, @Nullable String defaultValue);

        @AnyThread
        void putInt(String key, int value);

        @AnyThread
        int getInt(String key, int defaultValue);
    }

    private static class FakeReaderWriterImpl implements ReaderWriter {
        @GuardedBy("mNonPersistentKeyValues")
        private final ArrayMap<String, String> mNonPersistentKeyValues = new ArrayMap<>();

        @AnyThread
        @Override
        public void putString(String key, String value) {
            synchronized (mNonPersistentKeyValues) {
                mNonPersistentKeyValues.put(key, value);
            }
        }

        @AnyThread
        @Nullable
        @Override
        public String getString(String key, String defaultValue) {
            synchronized (mNonPersistentKeyValues) {
                if (mNonPersistentKeyValues.containsKey(key)) {
                    final String result = mNonPersistentKeyValues.get(key);
                    return result != null ? result : defaultValue;
                }
                return defaultValue;
            }
        }

        @AnyThread
        @Override
        public void putInt(String key, int value) {
            synchronized (mNonPersistentKeyValues) {
                mNonPersistentKeyValues.put(key, String.valueOf(value));
            }
        }

        @AnyThread
        @Override
        public int getInt(String key, int defaultValue) {
            synchronized (mNonPersistentKeyValues) {
                if (mNonPersistentKeyValues.containsKey(key)) {
                    final String result = mNonPersistentKeyValues.get(key);
                    return result != null ? Integer.parseInt(result) : defaultValue;
                }
                return defaultValue;
            }
        }
    }

    private static class UnlockedUserImpl implements ReaderWriter {
        @UserIdInt
        private final int mUserId;

        private final ContentResolver mContentResolver;

        UnlockedUserImpl(@UserIdInt int userId, @NonNull ContentResolver contentResolver) {
            mUserId = userId;
            mContentResolver = contentResolver;
        }

        @AnyThread
        @Override
        public void putString(String key, String value) {
            final int userId = getUserIdForClonedSettings(key, mUserId);
            Settings.Secure.putStringForUser(mContentResolver, key, value, userId);
        }

        @AnyThread
        @Nullable
        @Override
        public String getString(String key, String defaultValue) {
            final String result = Settings.Secure.getStringForUser(mContentResolver, key, mUserId);
            return result != null ? result : defaultValue;
        }

        @AnyThread
        @Override
        public void putInt(String key, int value) {
            final int userId = getUserIdForClonedSettings(key, mUserId);
            Settings.Secure.putIntForUser(mContentResolver, key, value, userId);
        }

        @AnyThread
        @Override
        public int getInt(String key, int defaultValue) {
            return Settings.Secure.getIntForUser(mContentResolver, key, defaultValue, mUserId);
        }
    }

    /**
     * For users whose storages are not unlocked yet, we do not want to update IME related Secure
     * Settings. Any write operations will be forwarded to
     * {@link LockedUserImpl#mNonPersistentKeyValues} so that we can return the volatile data until
     * the user storage is unlocked.
     */
    private static final class LockedUserImpl extends UnlockedUserImpl {
        @GuardedBy("mNonPersistentKeyValues")
        private final ArrayMap<String, String> mNonPersistentKeyValues = new ArrayMap<>();

        LockedUserImpl(@UserIdInt int userId, @NonNull ContentResolver contentResolver) {
            super(userId, contentResolver);
        }

        @AnyThread
        @Override
        public void putString(String key, String value) {
            synchronized (mNonPersistentKeyValues) {
                mNonPersistentKeyValues.put(key, value);
            }
        }

        @AnyThread
        @Nullable
        @Override
        public String getString(String key, String defaultValue) {
            synchronized (mNonPersistentKeyValues) {
                if (mNonPersistentKeyValues.containsKey(key)) {
                    final String result = mNonPersistentKeyValues.get(key);
                    return result != null ? result : defaultValue;
                }
                return super.getString(key, defaultValue);
            }
        }

        @AnyThread
        @Override
        public void putInt(String key, int value) {
            synchronized (mNonPersistentKeyValues) {
                mNonPersistentKeyValues.put(key, String.valueOf(value));
            }
        }

        @AnyThread
        @Override
        public int getInt(String key, int defaultValue) {
            synchronized (mNonPersistentKeyValues) {
                if (mNonPersistentKeyValues.containsKey(key)) {
                    final String result = mNonPersistentKeyValues.get(key);
                    return result != null ? Integer.parseInt(result) : defaultValue;
                }
                return super.getInt(key, defaultValue);
            }
        }
    }

    private static final ReaderWriter NOOP = new ReaderWriter() {
        @Override
        public void putString(String key, String str) {
        }

        @Override
        public String getString(String key, String defaultValue) {
            return defaultValue;
        }

        @Override
        public void putInt(String key, int value) {
        }

        @Override
        public int getInt(String key, int defaultValue) {
            return defaultValue;
        }
    };

    private static ReaderWriter createImpl(@NonNull UserManagerInternal userManagerInternal,
            @UserIdInt int userId) {
        if (sTestMode) {
            return new FakeReaderWriterImpl();
        }
        return userManagerInternal.isUserUnlockingOrUnlocked(userId)
                ? new UnlockedUserImpl(userId, sContentResolver)
                : new LockedUserImpl(userId, sContentResolver);
    }

    @NonNull
    @AnyThread
    private static ReaderWriter putOrGet(@UserIdInt int userId,
            @NonNull ReaderWriter readerWriter) {
        final boolean isUnlockedUserImpl = readerWriter instanceof UnlockedUserImpl;
        synchronized (sMutationLock) {
            final ReaderWriter current = sUserMap.get(userId);
            if (current == null) {
                sUserMap = sUserMap.cloneWithPutOrSelf(userId, readerWriter);
                return readerWriter;
            }
            // Upgrading from CopyOnWriteImpl to DirectImpl is allowed.
            if (current instanceof LockedUserImpl && isUnlockedUserImpl) {
                sUserMap = sUserMap.cloneWithPutOrSelf(userId, readerWriter);
                return readerWriter;
            }
            return current;
        }
    }

    @NonNull
    @AnyThread
    private static ReaderWriter get(@UserIdInt int userId) {
        final ReaderWriter readerWriter = sUserMap.get(userId);
        if (readerWriter != null) {
            return readerWriter;
        }
        if (sTestMode) {
            return putOrGet(userId, new FakeReaderWriterImpl());
        }
        final UserManagerInternal userManagerInternal =
                LocalServices.getService(UserManagerInternal.class);
        if (!userManagerInternal.exists(userId)) {
            return NOOP;
        }
        return putOrGet(userId, createImpl(userManagerInternal, userId));
    }

    /**
     * Called when the system is starting.
     *
     * @param contentResolver the {@link ContentResolver} to be used
     */
    @AnyThread
    static void setContentResolver(@NonNull ContentResolver contentResolver) {
        sContentResolver = contentResolver;
    }

    /**
     * Called when a user is starting.
     *
     * @param userId the ID of the user who is starting.
     */
    @AnyThread
    static void onUserStarting(@UserIdInt int userId) {
        if (sTestMode) {
            putOrGet(userId, new FakeReaderWriterImpl());
            return;
        }
        putOrGet(userId, createImpl(LocalServices.getService(UserManagerInternal.class), userId));
    }

    /**
     * Called when a user is being unlocked.
     *
     * @param userId the ID of the user whose storage is being unlocked.
     */
    @AnyThread
    static void onUserUnlocking(@UserIdInt int userId) {
        if (sTestMode) {
            putOrGet(userId, new FakeReaderWriterImpl());
            return;
        }
        final ReaderWriter readerWriter = new UnlockedUserImpl(userId, sContentResolver);
        putOrGet(userId, readerWriter);
    }

    /**
     * Called when a user is stopped, which changes the user storage to the locked state again.
     *
     * @param userId the ID of the user whose storage is being locked again.
     */
    @AnyThread
    static void onUserStopped(@UserIdInt int userId) {
        final LockedUserImpl lockedUserImpl = new LockedUserImpl(userId, sContentResolver);
        synchronized (sMutationLock) {
            final ReaderWriter current = sUserMap.get(userId);
            if (current == null || current instanceof LockedUserImpl) {
                return;
            }
            sUserMap = sUserMap.cloneWithPutOrSelf(userId, lockedUserImpl);
        }
    }

    /**
     * Called when a user is being removed.
     *
     * @param userId the ID of the user whose storage is being removed
     */
    @AnyThread
    static void onUserRemoved(@UserIdInt int userId) {
        synchronized (sMutationLock) {
            sUserMap = sUserMap.cloneWithRemoveOrSelf(userId);
        }
    }

    /**
     * Put the given string {@code value} to {@code key}.
     *
     * @param key a secure settings key.
     * @param value a secure settings value.
     * @param userId the ID of a user whose secure settings will be updated.
     * @see Settings.Secure#putStringForUser(ContentResolver, String, String, int)
     */
    @AnyThread
    static void putString(String key, String value, @UserIdInt int userId) {
        get(userId).putString(key, value);
    }

    /**
     * Get a string value with the given {@code key}
     *
     * @param key a secure settings key.
     * @param defaultValue the default value when the value is not found.
     * @param userId the ID of a user whose secure settings will be updated.
     * @return The string value if it is found. {@code defaultValue} otherwise.
     * @see Settings.Secure#getStringForUser(ContentResolver, String, int)
     */
    @AnyThread
    @Nullable
    static String getString(String key, String defaultValue, @UserIdInt int userId) {
        return get(userId).getString(key, defaultValue);
    }

    /**
     * Put the given integer {@code value} to {@code key}.
     *
     * @param key a secure settings key.
     * @param value a secure settings value.
     * @param userId the ID of a user whose secure settings will be updated.
     * @see Settings.Secure#putIntForUser(ContentResolver, String, int, int)
     */
    @AnyThread
    static void putInt(String key, int value, @UserIdInt int userId) {
        get(userId).putInt(key, value);
    }

    /**
     * Get an integer value with the given {@code key}
     *
     * @param key a secure settings key.
     * @param defaultValue the default value when the value is not found.
     * @param userId the ID of a user whose secure settings will be updated.
     * @return The integer value if it is found. {@code defaultValue} otherwise.
c     */
    @AnyThread
    static int getInt(String key, int defaultValue, @UserIdInt int userId) {
        return get(userId).getInt(key, defaultValue);
    }

    /**
     * Put the given boolean {@code value} to {@code key}.
     *
     * @param key a secure settings key.
     * @param value a secure settings value.
     * @param userId the ID of a user whose secure settings will be updated.
     */
    @AnyThread
    static void putBoolean(String key, boolean value, @UserIdInt int userId) {
        get(userId).putInt(key, value ? 1 : 0);
    }

    /**
     * Get a boolean value with the given {@code key}
     *
     * @param key a secure settings key.
     * @param defaultValue the default value when the value is not found.
     * @param userId the ID of a user whose secure settings will be updated.
     * @return The boolean value if it is found. {@code defaultValue} otherwise.
     */
    @AnyThread
    static boolean getBoolean(String key, boolean defaultValue, @UserIdInt int userId) {
        return get(userId).getInt(key, defaultValue ? 1 : 0) == 1;
    }
}
