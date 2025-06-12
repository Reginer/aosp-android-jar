/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.pm;

import static android.content.pm.PackageManager.INSTALL_INTERNAL;
import static android.content.pm.PackageManager.MOVE_FAILED_3RD_PARTY_NOT_ALLOWED_ON_INTERNAL;
import static android.content.pm.PackageManager.MOVE_FAILED_DEVICE_ADMIN;
import static android.content.pm.PackageManager.MOVE_FAILED_DOESNT_EXIST;
import static android.content.pm.PackageManager.MOVE_FAILED_INTERNAL_ERROR;
import static android.content.pm.PackageManager.MOVE_FAILED_LOCKED_USER;
import static android.content.pm.PackageManager.MOVE_FAILED_OPERATION_PENDING;
import static android.content.pm.PackageManager.MOVE_FAILED_SYSTEM_PACKAGE;

import static com.android.server.pm.PackageManagerService.DEBUG_INSTALL;
import static com.android.server.pm.PackageManagerService.TAG;

import android.app.ApplicationExitInfo;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.UserInfo;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.VolumeInfo;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseIntArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class MovePackageHelper {
    final PackageManagerService mPm;

    // TODO(b/198166813): remove PMS dependency
    public MovePackageHelper(PackageManagerService pm) {
        mPm = pm;
    }

    public void movePackageInternal(final String packageName, final String volumeUuid,
            final int moveId, final int callingUid, UserHandle user)
            throws PackageManagerException {
        final StorageManager storage = mPm.mInjector.getSystemService(StorageManager.class);
        final PackageManager pm = mPm.mContext.getPackageManager();

        Computer snapshot = mPm.snapshotComputer();
        final PackageStateInternal packageState = snapshot.getPackageStateForInstalledAndFiltered(
                packageName, callingUid, user.getIdentifier());
        if (packageState == null || packageState.getPkg() == null) {
            throw new PackageManagerException(MOVE_FAILED_DOESNT_EXIST, "Missing package");
        }
        final int[] installedUserIds = PackageStateUtils.queryInstalledUsers(packageState,
                mPm.mUserManager.getUserIds(), true);
        final UserHandle userForMove;
        if (installedUserIds.length > 0) {
            userForMove = UserHandle.of(installedUserIds[0]);
        } else {
            throw new PackageManagerException(MOVE_FAILED_DOESNT_EXIST,
                    "Package is not installed for any user");
        }
        for (int userId : installedUserIds) {
            if (snapshot.shouldFilterApplicationIncludingUninstalled(packageState, callingUid,
                    userId)) {
                throw new PackageManagerException(MOVE_FAILED_DOESNT_EXIST, "Missing package");
            }
        }
        final AndroidPackage pkg = packageState.getPkg();
        if (packageState.isSystem()) {
            throw new PackageManagerException(MOVE_FAILED_SYSTEM_PACKAGE,
                    "Cannot move system application");
        }

        final boolean isInternalStorage = VolumeInfo.ID_PRIVATE_INTERNAL.equals(volumeUuid);
        final boolean allow3rdPartyOnInternal = mPm.mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_allow3rdPartyAppOnInternal);
        if (isInternalStorage && !allow3rdPartyOnInternal) {
            throw new PackageManagerException(MOVE_FAILED_3RD_PARTY_NOT_ALLOWED_ON_INTERNAL,
                    "3rd party apps are not allowed on internal storage");
        }

        final File probe = new File(pkg.getPath());
        if (!probe.isDirectory()) {
            throw new PackageManagerException(MOVE_FAILED_INTERNAL_ERROR,
                    "Move only supported for modern cluster style installs");
        }

        final String currentVolumeUuid = packageState.getVolumeUuid();
        if (Objects.equals(currentVolumeUuid, volumeUuid)) {
            throw new PackageManagerException(MOVE_FAILED_INTERNAL_ERROR,
                    "Package already moved to " + volumeUuid);
        }
        if (!pkg.isExternalStorage()
                && mPm.isPackageDeviceAdminOnAnyUser(snapshot, packageName)) {
            throw new PackageManagerException(MOVE_FAILED_DEVICE_ADMIN,
                    "Device admin cannot be moved");
        }

        if (snapshot.getFrozenPackages().containsKey(packageName)) {
            throw new PackageManagerException(MOVE_FAILED_OPERATION_PENDING,
                    "Failed to move already frozen package");
        }

        final boolean isCurrentLocationExternal = pkg.isExternalStorage();
        final File codeFile = new File(pkg.getPath());
        final InstallSource installSource = packageState.getInstallSource();
        final String packageAbiOverride = packageState.getCpuAbiOverride();
        final int appId = UserHandle.getAppId(pkg.getUid());
        final String seinfo = packageState.getSeInfo();
        final String label = String.valueOf(pm.getApplicationLabel(
                AndroidPackageUtils.generateAppInfoWithoutState(pkg)));
        final int targetSdkVersion = pkg.getTargetSdkVersion();
        final String fromCodePath;
        if (codeFile.getParentFile().getName().startsWith(
                PackageManagerService.RANDOM_DIR_PREFIX)) {
            fromCodePath = codeFile.getParentFile().getAbsolutePath();
        } else {
            fromCodePath = codeFile.getAbsolutePath();
        }

        final PackageFreezer freezer;
        synchronized (mPm.mLock) {
            freezer = mPm.freezePackage(packageName, UserHandle.USER_ALL,
                    "movePackageInternal", ApplicationExitInfo.REASON_USER_REQUESTED,
                    null /* request */);
        }

        final Bundle extras = new Bundle();
        extras.putString(Intent.EXTRA_PACKAGE_NAME, packageName);
        extras.putString(Intent.EXTRA_TITLE, label);
        mPm.mMoveCallbacks.notifyCreated(moveId, extras);

        int installFlags;
        final boolean moveCompleteApp;
        final File measurePath;

        installFlags = INSTALL_INTERNAL;
        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
            moveCompleteApp = true;
            measurePath = Environment.getDataAppDirectory(volumeUuid);
        } else if (Objects.equals(StorageManager.UUID_PRIMARY_PHYSICAL, volumeUuid)) {
            moveCompleteApp = false;
            measurePath = storage.getPrimaryPhysicalVolume().getPath();
        } else {
            final VolumeInfo volume = storage.findVolumeByUuid(volumeUuid);
            if (volume == null || volume.getType() != VolumeInfo.TYPE_PRIVATE
                    || !volume.isMountedWritable()) {
                freezer.close();
                throw new PackageManagerException(MOVE_FAILED_INTERNAL_ERROR,
                        "Move location not mounted private volume");
            }

            moveCompleteApp = true;
            measurePath = Environment.getDataAppDirectory(volumeUuid);
        }

        // If we're moving app data around, we need all the users unlocked
        if (moveCompleteApp) {
            for (int userId : installedUserIds) {
                if (StorageManager.isFileEncrypted()
                        && !StorageManager.isCeStorageUnlocked(userId)) {
                    freezer.close();
                    throw new PackageManagerException(MOVE_FAILED_LOCKED_USER,
                            "User " + userId + " must be unlocked");
                }
            }
        }

        final PackageStats stats = new PackageStats(null, -1);
        try (PackageManagerTracedLock installLock = mPm.mInstallLock.acquireLock()) {
            for (int userId : installedUserIds) {
                if (!getPackageSizeInfoLI(packageName, userId, stats)) {
                    freezer.close();
                    throw new PackageManagerException(MOVE_FAILED_INTERNAL_ERROR,
                            "Failed to measure package size");
                }
            }
        }

        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Measured code size " + stats.codeSize + ", data size "
                    + stats.dataSize);
        }

        final long startFreeBytes = measurePath.getUsableSpace();
        final long sizeBytes;
        if (moveCompleteApp) {
            sizeBytes = stats.codeSize + stats.dataSize;
        } else {
            sizeBytes = stats.codeSize;
        }

        if (sizeBytes > storage.getStorageBytesUntilLow(measurePath)) {
            freezer.close();
            throw new PackageManagerException(MOVE_FAILED_INTERNAL_ERROR,
                    "Not enough free space to move");
        }

        try {
            prepareUserStorageForMove(currentVolumeUuid, volumeUuid, installedUserIds);
        } catch (RuntimeException e) {
            freezer.close();
            throw new PackageManagerException(MOVE_FAILED_INTERNAL_ERROR,
                    "Failed to prepare user storage while moving app");
        }

        mPm.mMoveCallbacks.notifyStatusChanged(moveId, 10);

        final CountDownLatch installedLatch = new CountDownLatch(1);
        final IPackageInstallObserver2 installObserver = new IPackageInstallObserver2.Stub() {
            @Override
            public void onUserActionRequired(Intent intent) throws RemoteException {
                freezer.close();
                throw new IllegalStateException();
            }

            @Override
            public void onPackageInstalled(String basePackageName, int returnCode, String msg,
                    Bundle extras) throws RemoteException {
                if (DEBUG_INSTALL) {
                    Slog.d(TAG, "Install result for move: "
                            + PackageManager.installStatusToString(returnCode, msg));
                }

                installedLatch.countDown();
                freezer.close();

                final int status = PackageManager.installStatusToPublicStatus(returnCode);
                switch (status) {
                    case PackageInstaller.STATUS_SUCCESS:
                        mPm.mMoveCallbacks.notifyStatusChanged(moveId,
                                PackageManager.MOVE_SUCCEEDED);
                        logAppMovedStorage(packageName, isCurrentLocationExternal);
                        break;
                    case PackageInstaller.STATUS_FAILURE_STORAGE:
                        mPm.mMoveCallbacks.notifyStatusChanged(moveId,
                                PackageManager.MOVE_FAILED_INSUFFICIENT_STORAGE);
                        break;
                    default:
                        mPm.mMoveCallbacks.notifyStatusChanged(moveId,
                                PackageManager.MOVE_FAILED_INTERNAL_ERROR);
                        break;
                }
            }
        };

        final MoveInfo move;
        if (moveCompleteApp) {
            // Kick off a thread to report progress estimates
            new Thread(() -> {
                while (true) {
                    try {
                        if (installedLatch.await(1, TimeUnit.SECONDS)) {
                            break;
                        }
                    } catch (InterruptedException ignored) {
                    }

                    final long deltaFreeBytes = startFreeBytes - measurePath.getUsableSpace();
                    final int progress = 10 + (int) MathUtils.constrain(
                            ((deltaFreeBytes * 80) / sizeBytes), 0, 80);
                    mPm.mMoveCallbacks.notifyStatusChanged(moveId, progress);
                }
            }).start();

            move = new MoveInfo(moveId, currentVolumeUuid, volumeUuid, packageName,
                    appId, seinfo, targetSdkVersion, fromCodePath);
        } else {
            move = null;
        }

        installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;

        final OriginInfo origin = OriginInfo.fromExistingFile(codeFile);
        final ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        final ParseResult<PackageLite> ret = ApkLiteParseUtils.parsePackageLite(input,
                new File(origin.mResolvedPath), /* flags */ 0);
        final PackageLite lite = ret.isSuccess() ? ret.getResult() : null;
        final InstallingSession installingSession = new InstallingSession(origin, move,
                installObserver, installFlags, /* developmentInstallFlags= */ 0, installSource,
                volumeUuid, userForMove, packageAbiOverride,
                PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED, lite, mPm);
        installingSession.movePackage();
    }

    /**
     * Logs that an app has been moved from internal to external storage and vice versa.
     * @param packageName The package that was moved.
     */
    private void logAppMovedStorage(String packageName, boolean isPreviousLocationExternal) {
        final Computer snapshot = mPm.snapshotComputer();
        final AndroidPackage pkg = snapshot.getPackage(packageName);
        if (pkg == null) {
            return;
        }

        final StorageManager storage = mPm.mInjector.getSystemService(StorageManager.class);
        VolumeInfo volume = storage.findVolumeByUuid(
                StorageManager.convert(pkg.getVolumeUuid()).toString());
        int packageExternalStorageType = PackageManagerServiceUtils.getPackageExternalStorageType(
                volume, pkg.isExternalStorage());

        if (!isPreviousLocationExternal && pkg.isExternalStorage()) {
            // Move from internal to external storage.
            FrameworkStatsLog.write(FrameworkStatsLog.APP_MOVED_STORAGE_REPORTED,
                    packageExternalStorageType,
                    FrameworkStatsLog.APP_MOVED_STORAGE_REPORTED__MOVE_TYPE__TO_EXTERNAL,
                    packageName);
        } else if (isPreviousLocationExternal && !pkg.isExternalStorage()) {
            // Move from external to internal storage.
            FrameworkStatsLog.write(FrameworkStatsLog.APP_MOVED_STORAGE_REPORTED,
                    packageExternalStorageType,
                    FrameworkStatsLog.APP_MOVED_STORAGE_REPORTED__MOVE_TYPE__TO_INTERNAL,
                    packageName);
        }
    }

    @GuardedBy("mPm.mInstallLock")
    private boolean getPackageSizeInfoLI(String packageName, int userId, PackageStats stats) {
        final Computer snapshot = mPm.snapshotComputer();
        final PackageStateInternal packageStateInternal =
                snapshot.getPackageStateInternal(packageName);
        if (packageStateInternal == null) {
            Slog.w(TAG, "Failed to find settings for " + packageName);
            return false;
        }

        final String[] packageNames = { packageName };
        final long[] ceDataInodes = {
                packageStateInternal.getUserStateOrDefault(userId).getCeDataInode() };
        final String[] codePaths = { packageStateInternal.getPathString() };

        try {
            mPm.mInstaller.getAppSize(packageStateInternal.getVolumeUuid(), packageNames, userId,
                    0, packageStateInternal.getAppId(), ceDataInodes, codePaths, stats);

            // For now, ignore code size of packages on system partition
            if (PackageManagerServiceUtils.isSystemApp(packageStateInternal)
                    && !PackageManagerServiceUtils.isUpdatedSystemApp(packageStateInternal)) {
                stats.codeSize = 0;
            }

            // External clients expect these to be tracked separately
            stats.dataSize -= stats.cacheSize;

        } catch (Installer.InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
            return false;
        }

        return true;
    }

    private void prepareUserStorageForMove(String fromVolumeUuid, String toVolumeUuid,
            int[] userIds) {
        if (DEBUG_INSTALL) {
            Slog.d(TAG, "Preparing user directories before moving app, from UUID " + fromVolumeUuid
                    + " to UUID " + toVolumeUuid);
        }
        final StorageManagerInternal smInternal =
                mPm.mInjector.getLocalService(StorageManagerInternal.class);
        final ArrayList<UserInfo> users = new ArrayList<>();
        for (int userId : userIds) {
            final UserInfo user = mPm.mUserManager.getUserInfo(userId);
            users.add(user);
        }
        smInternal.prepareUserStorageForMove(fromVolumeUuid, toVolumeUuid, users);
    }

    public static class MoveCallbacks extends Handler {
        private static final int MSG_CREATED = 1;
        private static final int MSG_STATUS_CHANGED = 2;

        private final RemoteCallbackList<IPackageMoveObserver>
                mCallbacks = new RemoteCallbackList<>();

        public final SparseIntArray mLastStatus = new SparseIntArray();

        public MoveCallbacks(Looper looper) {
            super(looper);
        }

        public void register(IPackageMoveObserver callback) {
            mCallbacks.register(callback);
        }

        public void unregister(IPackageMoveObserver callback) {
            mCallbacks.unregister(callback);
        }

        @Override
        public void handleMessage(Message msg) {
            final SomeArgs args = (SomeArgs) msg.obj;
            final int n = mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                final IPackageMoveObserver callback = mCallbacks.getBroadcastItem(i);
                try {
                    invokeCallback(callback, msg.what, args);
                } catch (RemoteException ignored) {
                }
            }
            mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IPackageMoveObserver callback, int what, SomeArgs args)
                throws RemoteException {
            switch (what) {
                case MSG_CREATED: {
                    callback.onCreated(args.argi1, (Bundle) args.arg2);
                    break;
                }
                case MSG_STATUS_CHANGED: {
                    callback.onStatusChanged(args.argi1, args.argi2, (long) args.arg3);
                    break;
                }
            }
        }

        public void notifyCreated(int moveId, Bundle extras) {
            Slog.v(TAG, "Move " + moveId + " created " + extras.toString());

            final SomeArgs args = SomeArgs.obtain();
            args.argi1 = moveId;
            args.arg2 = extras;
            obtainMessage(MSG_CREATED, args).sendToTarget();
        }

        public void notifyStatusChanged(int moveId, int status) {
            notifyStatusChanged(moveId, status, -1);
        }

        public void notifyStatusChanged(int moveId, int status, long estMillis) {
            Slog.v(TAG, "Move " + moveId + " status " + status);

            final SomeArgs args = SomeArgs.obtain();
            args.argi1 = moveId;
            args.argi2 = status;
            args.arg3 = estMillis;
            obtainMessage(MSG_STATUS_CHANGED, args).sendToTarget();

            synchronized (mLastStatus) {
                mLastStatus.put(moveId, status);
            }
        }
    }
}
