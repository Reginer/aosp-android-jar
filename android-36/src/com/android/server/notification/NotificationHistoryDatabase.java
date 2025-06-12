/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.server.notification;

import android.app.NotificationHistory;
import android.app.NotificationHistory.HistoricalNotification;
import android.os.Handler;
import android.util.AtomicFile;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Provides an interface to write and query for notification history data for a user from a Protocol
 * Buffer database.
 *
 * Periodically writes the buffered history to disk but can also accept force writes based on
 * outside changes (like a pending shutdown).
 */
public class NotificationHistoryDatabase {
    private static final int DEFAULT_CURRENT_VERSION = 1;

    private static final String TAG = "NotiHistoryDatabase";
    private static final boolean DEBUG = NotificationManagerService.DBG;
    private static final int HISTORY_RETENTION_DAYS = 1;
    private static final long WRITE_BUFFER_INTERVAL_MS = 1000 * 60 * 20;
    private static final long INVALID_FILE_TIME_MS = -1;

    private final Object mLock = new Object();
    private final Handler mFileWriteHandler;
    @VisibleForTesting
    // List of files holding history information, sorted newest to oldest
    final List<AtomicFile> mHistoryFiles;
    private final File mHistoryDir;
    private final File mVersionFile;
    // Current version of the database files schema
    private int mCurrentVersion;
    private final WriteBufferRunnable mWriteBufferRunnable;

    // Object containing posted notifications that have not yet been written to disk
    @VisibleForTesting
    NotificationHistory mBuffer;

    public NotificationHistoryDatabase(Handler fileWriteHandler, File dir) {
        mCurrentVersion = DEFAULT_CURRENT_VERSION;
        mFileWriteHandler = fileWriteHandler;
        mVersionFile = new File(dir, "version");
        mHistoryDir = new File(dir, "history");
        mHistoryFiles = new ArrayList<>();
        mBuffer = new NotificationHistory();
        mWriteBufferRunnable = new WriteBufferRunnable();
    }

    public void init() {
        synchronized (mLock) {
            try {
                if (!mHistoryDir.exists() && !mHistoryDir.mkdir()) {
                    throw new IllegalStateException("could not create history directory");
                }
                mVersionFile.createNewFile();
            } catch (Exception e) {
                Slog.e(TAG, "could not create needed files", e);
            }

            checkVersionAndBuildLocked();
            indexFilesLocked();
            prune();
        }
    }

    private void indexFilesLocked() {
        mHistoryFiles.clear();
        final File[] files = mHistoryDir.listFiles();
        if (files == null) {
            return;
        }

        // Sort with newest files first
        Arrays.sort(files, (lhs, rhs) -> Long.compare(safeParseLong(rhs.getName()),
                safeParseLong(lhs.getName())));

        for (File file : files) {
            mHistoryFiles.add(new AtomicFile(file));
        }
    }

    private void checkVersionAndBuildLocked() {
        int version;
        try (BufferedReader reader = new BufferedReader(new FileReader(mVersionFile))) {
            version = Integer.parseInt(reader.readLine());
        } catch (NumberFormatException | IOException e) {
            version = 0;
        }

        if (version != mCurrentVersion && mVersionFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(mVersionFile))) {
                writer.write(Integer.toString(mCurrentVersion));
                writer.write("\n");
                writer.flush();
            } catch (IOException e) {
                Slog.e(TAG, "Failed to write new version");
                throw new RuntimeException(e);
            }
        }
    }

    public void forceWriteToDisk() {
        mFileWriteHandler.post(mWriteBufferRunnable);
    }

    public void onPackageRemoved(String packageName) {
        RemovePackageRunnable rpr = new RemovePackageRunnable(packageName);
        mFileWriteHandler.post(rpr);
    }

    public void deleteNotificationHistoryItem(String pkg, long postedTime) {
        RemoveNotificationRunnable rnr = new RemoveNotificationRunnable(pkg, postedTime);
        mFileWriteHandler.post(rnr);
    }

    public void deleteConversations(String pkg, Set<String> conversationIds) {
        RemoveConversationRunnable rcr = new RemoveConversationRunnable(pkg, conversationIds);
        mFileWriteHandler.post(rcr);
    }

    public void deleteNotificationChannel(String pkg, String channelId) {
        RemoveChannelRunnable rcr = new RemoveChannelRunnable(pkg, channelId);
        mFileWriteHandler.post(rcr);
    }

    public void addNotification(final HistoricalNotification notification) {
        synchronized (mLock) {
            mBuffer.addNewNotificationToWrite(notification);
            // Each time we have new history to write to disk, schedule a write in [interval] ms
            if (mBuffer.getHistoryCount() == 1) {
                mFileWriteHandler.postDelayed(mWriteBufferRunnable, WRITE_BUFFER_INTERVAL_MS);
            }
        }
    }

    public NotificationHistory readNotificationHistory() {
        synchronized (mLock) {
            NotificationHistory notifications = new NotificationHistory();
            notifications.addNotificationsToWrite(mBuffer);

            for (AtomicFile file : mHistoryFiles) {
                try {
                    readLocked(
                            file, notifications, new NotificationHistoryFilter.Builder().build());
                } catch (Exception e) {
                    Slog.e(TAG, "error reading " + file.getBaseFile().getAbsolutePath(), e);
                }
            }

            return notifications;
        }
    }

    public NotificationHistory readNotificationHistory(String packageName, String channelId,
            int maxNotifications) {
        synchronized (mLock) {
            NotificationHistory notifications = new NotificationHistory();

            for (AtomicFile file : mHistoryFiles) {
                try {
                    readLocked(file, notifications,
                            new NotificationHistoryFilter.Builder()
                                    .setPackage(packageName)
                                    .setChannel(packageName, channelId)
                                    .setMaxNotifications(maxNotifications)
                                    .build());
                    if (maxNotifications == notifications.getHistoryCount()) {
                        // No need to read any more files
                        break;
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "error reading " + file.getBaseFile().getAbsolutePath(), e);
                }
            }

            return notifications;
        }
    }

    public void disableHistory() {
        synchronized (mLock) {
            for (AtomicFile file : mHistoryFiles) {
                file.delete();
            }
            mHistoryDir.delete();
            mHistoryFiles.clear();
        }
    }

    /**
     * Remove any files that are too old.
     */
    void prune() {
        prune(HISTORY_RETENTION_DAYS, System.currentTimeMillis());
    }

    /**
     * Remove any files that are too old.
     */
    void prune(final int retentionDays, final long currentTimeMillis) {
        synchronized (mLock) {
            GregorianCalendar retentionBoundary = new GregorianCalendar();
            retentionBoundary.setTimeInMillis(currentTimeMillis);
            retentionBoundary.add(Calendar.DATE, -1 * retentionDays);

            for (int i = mHistoryFiles.size() - 1; i >= 0; i--) {
                final AtomicFile currentOldestFile = mHistoryFiles.get(i);
                final long creationTime = safeParseLong(
                        currentOldestFile.getBaseFile().getName());
                if (DEBUG) {
                    Slog.d(TAG, "File " + currentOldestFile.getBaseFile().getName()
                            + " created on " + creationTime);
                }

                if (creationTime <= retentionBoundary.getTimeInMillis()) {
                    deleteFile(currentOldestFile);
                }
            }
        }
    }

    /**
     * Remove the first entry from the list of history files whose file matches the given file path.
     *
     * This method is necessary for anything that only has an absolute file path rather than an
     * AtomicFile object from the list of history files.
     *
     * filePath should be an absolute path.
     */
    void removeFilePathFromHistory(String filePath) {
        if (filePath == null) {
            return;
        }

        Iterator<AtomicFile> historyFileItr = mHistoryFiles.iterator();
        while (historyFileItr.hasNext()) {
            final AtomicFile af = historyFileItr.next();
            if (af != null && filePath.equals(af.getBaseFile().getAbsolutePath())) {
                historyFileItr.remove();
                return;
            }
        }
    }

    private void deleteFile(AtomicFile file) {
        if (DEBUG) {
            Slog.d(TAG, "Removed " + file.getBaseFile().getName());
        }
        file.delete();
        // TODO: delete all relevant bitmaps, once they exist
        removeFilePathFromHistory(file.getBaseFile().getAbsolutePath());
    }

    private void writeLocked(AtomicFile file, NotificationHistory notifications)
            throws IOException {
        FileOutputStream fos = file.startWrite();
        try {
            NotificationHistoryProtoHelper.write(fos, notifications, mCurrentVersion);
            file.finishWrite(fos);
            fos = null;
        } finally {
            // When fos is null (successful write), this will no-op
            file.failWrite(fos);
        }
    }

    private static void readLocked(AtomicFile file, NotificationHistory notificationsOut,
            NotificationHistoryFilter filter) throws IOException {
        FileInputStream in = null;
        try {
            in = file.openRead();
            NotificationHistoryProtoHelper.read(in, notificationsOut, filter);
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "Cannot open " + file.getBaseFile().getAbsolutePath(), e);
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static long safeParseLong(String fileName) {
        // AtomicFile will create copies of the numeric files with ".new" and ".bak"
        // over the course of its processing. If these files still exist on boot we need to clean
        // them up
        try {
            return Long.parseLong(fileName);
        } catch (NumberFormatException e) {
            return INVALID_FILE_TIME_MS;
        }
    }

    final class WriteBufferRunnable implements Runnable {

        @Override
        public void run() {
            long time = System.currentTimeMillis();
            run(new AtomicFile(new File(mHistoryDir, String.valueOf(time))));
        }

        void run(AtomicFile file) {
            synchronized (mLock) {
                if (DEBUG) Slog.d(TAG, "WriteBufferRunnable "
                        + file.getBaseFile().getAbsolutePath());
                try {
                    writeLocked(file, mBuffer);
                    mHistoryFiles.add(0, file);
                    mBuffer = new NotificationHistory();
                } catch (IOException e) {
                    Slog.e(TAG, "Failed to write buffer to disk. not flushing buffer", e);
                }
            }
        }
    }

    private final class RemovePackageRunnable implements Runnable {
        private String mPkg;

        public RemovePackageRunnable(String pkg) {
            mPkg = pkg;
        }

        @Override
        public void run() {
            if (DEBUG) Slog.d(TAG, "RemovePackageRunnable " + mPkg);
            synchronized (mLock) {
                // Remove packageName entries from pending history
                mBuffer.removeNotificationsFromWrite(mPkg);

                Iterator<AtomicFile> historyFileItr = mHistoryFiles.iterator();
                while (historyFileItr.hasNext()) {
                    final AtomicFile af = historyFileItr.next();
                    try {
                        final NotificationHistory notifications = new NotificationHistory();
                        readLocked(af, notifications,
                                new NotificationHistoryFilter.Builder().build());
                        notifications.removeNotificationsFromWrite(mPkg);
                        writeLocked(af, notifications);
                    } catch (Exception e) {
                        Slog.e(TAG, "Cannot clean up file on pkg removal "
                                + af.getBaseFile().getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    final class RemoveNotificationRunnable implements Runnable {
        private String mPkg;
        private long mPostedTime;
        private NotificationHistory mNotificationHistory;

        public RemoveNotificationRunnable(String pkg, long postedTime) {
            mPkg = pkg;
            mPostedTime = postedTime;
        }

        @VisibleForTesting
        void setNotificationHistory(NotificationHistory nh) {
            mNotificationHistory = nh;
        }

        @Override
        public void run() {
            if (DEBUG) Slog.d(TAG, "RemoveNotificationRunnable");
            synchronized (mLock) {
                // Remove from pending history
                mBuffer.removeNotificationFromWrite(mPkg, mPostedTime);

                Iterator<AtomicFile> historyFileItr = mHistoryFiles.iterator();
                while (historyFileItr.hasNext()) {
                    final AtomicFile af = historyFileItr.next();
                    try {
                        NotificationHistory notificationHistory = mNotificationHistory != null
                                ? mNotificationHistory
                                : new NotificationHistory();
                        readLocked(af, notificationHistory,
                                new NotificationHistoryFilter.Builder().build());
                        if(notificationHistory.removeNotificationFromWrite(mPkg, mPostedTime)) {
                            writeLocked(af, notificationHistory);
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Cannot clean up file on notification removal "
                                + af.getBaseFile().getName(), e);
                    }
                }
            }
        }
    }

    final class RemoveConversationRunnable implements Runnable {
        private String mPkg;
        private Set<String> mConversationIds;
        private NotificationHistory mNotificationHistory;

        public RemoveConversationRunnable(String pkg, Set<String> conversationIds) {
            mPkg = pkg;
            mConversationIds = conversationIds;
        }

        @VisibleForTesting
        void setNotificationHistory(NotificationHistory nh) {
            mNotificationHistory = nh;
        }

        @Override
        public void run() {
            if (DEBUG) Slog.d(TAG, "RemoveConversationRunnable " + mPkg + " "  + mConversationIds);
            synchronized (mLock) {
                // Remove from pending history
                mBuffer.removeConversationsFromWrite(mPkg, mConversationIds);

                Iterator<AtomicFile> historyFileItr = mHistoryFiles.iterator();
                while (historyFileItr.hasNext()) {
                    final AtomicFile af = historyFileItr.next();
                    try {
                        NotificationHistory notificationHistory = mNotificationHistory != null
                                ? mNotificationHistory
                                : new NotificationHistory();
                        readLocked(af, notificationHistory,
                                new NotificationHistoryFilter.Builder().build());
                        if (notificationHistory.removeConversationsFromWrite(
                                mPkg, mConversationIds)) {
                            writeLocked(af, notificationHistory);
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Cannot clean up file on conversation removal "
                                + af.getBaseFile().getName(), e);
                    }
                }
            }
        }
    }

    final class RemoveChannelRunnable implements Runnable {
        private String mPkg;
        private String mChannelId;
        private NotificationHistory mNotificationHistory;

        RemoveChannelRunnable(String pkg, String channelId) {
            mPkg = pkg;
            mChannelId = channelId;
        }

        @VisibleForTesting
        void setNotificationHistory(NotificationHistory nh) {
            mNotificationHistory = nh;
        }

        @Override
        public void run() {
            if (DEBUG) Slog.d(TAG, "RemoveChannelRunnable");
            synchronized (mLock) {
                // Remove from pending history
                mBuffer.removeChannelFromWrite(mPkg, mChannelId);

                Iterator<AtomicFile> historyFileItr = mHistoryFiles.iterator();
                while (historyFileItr.hasNext()) {
                    final AtomicFile af = historyFileItr.next();
                    try {
                        NotificationHistory notificationHistory = mNotificationHistory != null
                                ? mNotificationHistory
                                : new NotificationHistory();
                        readLocked(af, notificationHistory,
                                new NotificationHistoryFilter.Builder().build());
                        if (notificationHistory.removeChannelFromWrite(mPkg, mChannelId)) {
                            writeLocked(af, notificationHistory);
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Cannot clean up file on channel removal "
                                + af.getBaseFile().getName(), e);
                    }
                }
            }
        }
    }
}
