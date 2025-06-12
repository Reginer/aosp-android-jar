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

package com.android.server.wm;

import static com.android.server.wm.AbsAppSnapshotController.TAG;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.window.TaskSnapshot;

import com.android.internal.annotations.VisibleForTesting;
import com.android.window.flags.Flags;

import java.io.File;
import java.util.UUID;

class BaseAppSnapshotPersister {
    static final String LOW_RES_FILE_POSTFIX = "_reduced";
    static final String PROTO_EXTENSION = ".proto";
    static final String BITMAP_EXTENSION = ".jpg";

    // Shared with SnapshotPersistQueue
    protected final Object mLock;
    protected final SnapshotPersistQueue mSnapshotPersistQueue;
    @VisibleForTesting
    protected final PersistInfoProvider mPersistInfoProvider;

    BaseAppSnapshotPersister(SnapshotPersistQueue persistQueue,
            PersistInfoProvider persistInfoProvider) {
        mSnapshotPersistQueue = persistQueue;
        mPersistInfoProvider = persistInfoProvider;
        mLock = persistQueue.getLock();
    }

    /**
     * Persists a snapshot of a task to disk.
     *
     * @param id The id of the object that needs to be persisted.
     * @param userId The id of the user this tasks belongs to.
     * @param snapshot The snapshot to persist.
     */
    void persistSnapshot(int id, int userId, TaskSnapshot snapshot) {
        synchronized (mLock) {
            mSnapshotPersistQueue.sendToQueueLocked(mSnapshotPersistQueue
                    .createStoreWriteQueueItem(id, userId, snapshot, mPersistInfoProvider));
        }
    }

    /**
     * Called to remove the persisted file
     *
     * @param id The id of task that has been removed.
     * @param userId The id of the user the task belonged to.
     */
    void removeSnapshot(int id, int userId) {
        synchronized (mLock) {
            mSnapshotPersistQueue.sendToQueueLocked(mSnapshotPersistQueue
                    .createDeleteWriteQueueItem(id, userId, mPersistInfoProvider));
        }
    }

    interface DirectoryResolver {
        File getSystemDirectoryForUser(int userId);
    }

    /**
     * Persist information provider, the snapshot persister and loader can know where the file is,
     * and the scale of a snapshot, etc.
     */
    static class PersistInfoProvider {
        protected final DirectoryResolver mDirectoryResolver;
        private final String mDirName;
        private final boolean mEnableLowResSnapshots;
        private final float mLowResScaleFactor;
        private final boolean mUse16BitFormat;
        private final SparseBooleanArray mInitializedUsers = new SparseBooleanArray();
        private final SparseArray<File> mScrambleDirectories = new SparseArray<>();

        PersistInfoProvider(DirectoryResolver directoryResolver, String dirName,
                boolean enableLowResSnapshots, float lowResScaleFactor, boolean use16BitFormat) {
            mDirectoryResolver = directoryResolver;
            mDirName = dirName;
            mEnableLowResSnapshots = enableLowResSnapshots;
            mLowResScaleFactor = lowResScaleFactor;
            mUse16BitFormat = use16BitFormat;
        }

        @NonNull
        File getDirectory(int userId) {
            if (Flags.scrambleSnapshotFileName()) {
                final File directory = getOrInitScrambleDirectory(userId);
                if (directory != null) {
                    return directory;
                }
            }
            return getBaseDirectory(userId);
        }

        @NonNull
        private File getBaseDirectory(int userId) {
            return new File(mDirectoryResolver.getSystemDirectoryForUser(userId), mDirName);
        }

        @Nullable
        private File getOrInitScrambleDirectory(int userId) {
            synchronized (mScrambleDirectories) {
                if (mInitializedUsers.get(userId)) {
                    return mScrambleDirectories.get(userId);
                }
                mInitializedUsers.put(userId, true);
                final File scrambledDirectory = getScrambleDirectory(userId);
                final File baseDir = getBaseDirectory(userId);
                String newName = null;
                // If directory exists, rename
                if (scrambledDirectory.exists()) {
                    newName = UUID.randomUUID().toString();
                    final File scrambleTo = new File(baseDir, newName);
                    if (!scrambledDirectory.renameTo(scrambleTo)) {
                        Slog.w(TAG, "SnapshotPersister rename scramble folder fail.");
                        return null;
                    }
                } else {
                    // If directory not exists, mkDir.
                    if (!baseDir.exists() && !baseDir.mkdir()) {
                        Slog.w(TAG, "SnapshotPersister make base folder fail.");
                        return null;
                    }
                    if (!scrambledDirectory.mkdir()) {
                        Slog.e(TAG, "SnapshotPersister make scramble folder fail");
                        return null;
                    }
                    // Move any existing files to this folder.
                    final String[] files = baseDir.list();
                    if (files != null) {
                        for (String file : files) {
                            final File original = new File(baseDir, file);
                            if (original.isDirectory()) {
                                newName = file;
                            } else {
                                File to = new File(scrambledDirectory, file);
                                original.renameTo(to);
                            }
                        }
                    }
                }
                final File newFolder = new File(baseDir, newName);
                mScrambleDirectories.put(userId, newFolder);
                return newFolder;
            }
        }

        @NonNull
        private File getScrambleDirectory(int userId) {
            final File dir = getBaseDirectory(userId);
            final String[] directories = dir.list(
                    (current, name) -> new File(current, name).isDirectory());
            if (directories != null && directories.length > 0) {
                return new File(dir, directories[0]);
            } else {
                return new File(dir, UUID.randomUUID().toString());
            }
        }

        /**
         * Return if task snapshots are stored in 16 bit pixel format.
         *
         * @return true if task snapshots are stored in 16 bit pixel format.
         */
        boolean use16BitFormat() {
            return mUse16BitFormat;
        }

        boolean createDirectory(int userId) {
            final File dir = getDirectory(userId);
            return dir.exists() || dir.mkdir();
        }

        File getProtoFile(int index, int userId) {
            return new File(getDirectory(userId), index + PROTO_EXTENSION);
        }

        File getLowResolutionBitmapFile(int index, int userId) {
            return new File(getDirectory(userId), index + LOW_RES_FILE_POSTFIX + BITMAP_EXTENSION);
        }

        File getHighResolutionBitmapFile(int index, int userId) {
            return new File(getDirectory(userId), index + BITMAP_EXTENSION);
        }

        boolean enableLowResSnapshots() {
            return mEnableLowResSnapshots;
        }

        float lowResScaleFactor() {
            return mLowResScaleFactor;
        }
    }
}
