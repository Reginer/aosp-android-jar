/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.server.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerInternal;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.XmlUtils;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for backup and restore of account access grants.
 */
public final class AccountManagerBackupHelper {
    private static final String TAG = "AccountManagerBackupHelper";

    private static final long PENDING_RESTORE_TIMEOUT_MILLIS = 60 * 60 * 1000; // 1 hour

    private static final String TAG_PERMISSIONS = "permissions";
    private static final String TAG_PERMISSION = "permission";
    private static final String ATTR_ACCOUNT_SHA_256 = "account-sha-256";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_DIGEST = "digest";

    private final Object mLock = new Object();

    private final AccountManagerService mAccountManagerService;
    private final AccountManagerInternal mAccountManagerInternal;

    @GuardedBy("mLock")
    private List<PendingAppPermission> mRestorePendingAppPermissions;

    @GuardedBy("mLock")
    private RestorePackageMonitor mRestorePackageMonitor;

    @GuardedBy("mLock")
    private Runnable mRestoreCancelCommand;

    public AccountManagerBackupHelper(AccountManagerService accountManagerService,
            AccountManagerInternal accountManagerInternal) {
        mAccountManagerService = accountManagerService;
        mAccountManagerInternal = accountManagerInternal;
    }

    private final class PendingAppPermission {
        private final @NonNull String accountDigest;
        private final @NonNull String packageName;
        private final @NonNull String certDigest;
        private final @IntRange(from = 0) int userId;

        public PendingAppPermission(String accountDigest, String packageName,
                String certDigest, int userId) {
            this.accountDigest = accountDigest;
            this.packageName = packageName;
            this.certDigest = certDigest;
            this.userId = userId;
        }

        public boolean apply(PackageManager packageManager) {
            Account account = null;
            AccountManagerService.UserAccounts accounts = mAccountManagerService
                    .getUserAccounts(userId);
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    for (Account[] accountsPerType : accounts.accountCache.values()) {
                        for (Account accountPerType : accountsPerType) {
                            if (accountDigest.equals(PackageUtils.computeSha256Digest(
                                    accountPerType.name.getBytes()))) {
                                account = accountPerType;
                                break;
                            }
                        }
                        if (account != null) {
                            break;
                        }
                    }
                }
            }
            if (account == null) {
                return false;
            }
            final PackageInfo packageInfo;
            try {
                packageInfo = packageManager.getPackageInfoAsUser(packageName,
                        PackageManager.GET_SIGNATURES, userId);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }

            // Before we used only the first signature to compute the SHA 256 but some
            // apps could be singed by multiple certs and the cert order is undefined.
            // We prefer the modern computation procedure where all certs are taken
            // into account but also allow the value from the old computation to allow
            // restoring backed up grants on an older platform version.
            final String[] signaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(
                    packageInfo.signatures);
            final String signaturesSha256Digest = PackageUtils.computeSignaturesSha256Digest(
                    signaturesSha256Digests);
            if (!certDigest.equals(signaturesSha256Digest) && (packageInfo.signatures.length <= 1
                    || !certDigest.equals(signaturesSha256Digests[0]))) {
                return false;
            }
            final int uid = packageInfo.applicationInfo.uid;
            if (!mAccountManagerInternal.hasAccountAccess(account, uid)) {
                mAccountManagerService.grantAppPermission(account,
                        AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE, uid);
            }
            return true;
        }
    }

    public byte[] backupAccountAccessPermissions(int userId) {
        final AccountManagerService.UserAccounts accounts = mAccountManagerService
                .getUserAccounts(userId);
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                List<Pair<String, Integer>> allAccountGrants = accounts.accountsDb
                        .findAllAccountGrants();
                if (allAccountGrants.isEmpty()) {
                    return null;
                }
                try {
                    ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                    final TypedXmlSerializer serializer = Xml.newFastSerializer();
                    serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
                    serializer.startDocument(null, true);
                    serializer.startTag(null, TAG_PERMISSIONS);

                    PackageManager packageManager = mAccountManagerService.mContext
                            .getPackageManager();
                    for (Pair<String, Integer> grant : allAccountGrants) {
                        final String accountName = grant.first;
                        final int uid = grant.second;

                        final String[] packageNames = packageManager.getPackagesForUid(uid);
                        if (packageNames == null) {
                            continue;
                        }

                        for (String packageName : packageNames) {
                            final PackageInfo packageInfo;
                            try {
                                packageInfo = packageManager.getPackageInfoAsUser(packageName,
                                        PackageManager.GET_SIGNATURES, userId);
                            } catch (PackageManager.NameNotFoundException e) {
                                Slog.i(TAG, "Skipping backup of account access grant for"
                                        + " non-existing package: " + packageName);
                                continue;
                            }
                            final String digest = PackageUtils.computeSignaturesSha256Digest(
                                    packageInfo.signatures);
                            if (digest != null) {
                                serializer.startTag(null, TAG_PERMISSION);
                                serializer.attribute(null, ATTR_ACCOUNT_SHA_256,
                                        PackageUtils.computeSha256Digest(accountName.getBytes()));
                                serializer.attribute(null, ATTR_PACKAGE, packageName);
                                serializer.attribute(null, ATTR_DIGEST, digest);
                                serializer.endTag(null, TAG_PERMISSION);
                            }
                        }
                    }
                    serializer.endTag(null, TAG_PERMISSIONS);
                    serializer.endDocument();
                    serializer.flush();
                    return dataStream.toByteArray();
                } catch (IOException e) {
                    Log.e(TAG, "Error backing up account access grants", e);
                    return null;
                }
            }
        }
    }

    public void restoreAccountAccessPermissions(byte[] data, int userId) {
        try {
            ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
            TypedXmlPullParser parser = Xml.newFastPullParser();
            parser.setInput(dataStream, StandardCharsets.UTF_8.name());
            PackageManager packageManager = mAccountManagerService.mContext.getPackageManager();

            final int permissionsOuterDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, permissionsOuterDepth)) {
                if (!TAG_PERMISSIONS.equals(parser.getName())) {
                    continue;
                }
                final int permissionOuterDepth = parser.getDepth();
                while (XmlUtils.nextElementWithin(parser, permissionOuterDepth)) {
                    if (!TAG_PERMISSION.equals(parser.getName())) {
                        continue;
                    }
                    String accountDigest = parser.getAttributeValue(null, ATTR_ACCOUNT_SHA_256);
                    if (TextUtils.isEmpty(accountDigest)) {
                        XmlUtils.skipCurrentTag(parser);
                    }
                    String packageName = parser.getAttributeValue(null, ATTR_PACKAGE);
                    if (TextUtils.isEmpty(packageName)) {
                        XmlUtils.skipCurrentTag(parser);
                    }
                    String digest =  parser.getAttributeValue(null, ATTR_DIGEST);
                    if (TextUtils.isEmpty(digest)) {
                        XmlUtils.skipCurrentTag(parser);
                    }

                    PendingAppPermission pendingAppPermission = new PendingAppPermission(
                            accountDigest, packageName, digest, userId);

                    if (!pendingAppPermission.apply(packageManager)) {
                        synchronized (mLock) {
                            // Start watching before add pending to avoid a missed signal
                            if (mRestorePackageMonitor == null) {
                                mRestorePackageMonitor = new RestorePackageMonitor();
                                mRestorePackageMonitor.register(mAccountManagerService.mContext,
                                        mAccountManagerService.mHandler.getLooper(), true);
                            }
                            if (mRestorePendingAppPermissions == null) {
                                mRestorePendingAppPermissions = new ArrayList<>();
                            }
                            mRestorePendingAppPermissions.add(pendingAppPermission);
                        }
                    }
                }
            }

            // Make sure we eventually prune the in-memory pending restores
            mRestoreCancelCommand = new CancelRestoreCommand();
            mAccountManagerService.mHandler.postDelayed(mRestoreCancelCommand,
                    PENDING_RESTORE_TIMEOUT_MILLIS);
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Error restoring app permissions", e);
        }
    }

    private final class RestorePackageMonitor extends PackageMonitor {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            synchronized (mLock) {
                if (mRestorePendingAppPermissions == null) {
                    return;
                }
                if (UserHandle.getUserId(uid) != UserHandle.USER_SYSTEM) {
                    return;
                }
                final int count = mRestorePendingAppPermissions.size();
                for (int i = count - 1; i >= 0; i--) {
                    PendingAppPermission pendingAppPermission =
                            mRestorePendingAppPermissions.get(i);
                    if (!pendingAppPermission.packageName.equals(packageName)) {
                        continue;
                    }
                    if (pendingAppPermission.apply(
                            mAccountManagerService.mContext.getPackageManager())) {
                        mRestorePendingAppPermissions.remove(i);
                    }
                }
                if (mRestorePendingAppPermissions.isEmpty()
                        && mRestoreCancelCommand != null) {
                    mAccountManagerService.mHandler.removeCallbacks(mRestoreCancelCommand);
                    mRestoreCancelCommand.run();
                    mRestoreCancelCommand = null;
                }
            }
        }
    }

    private final class CancelRestoreCommand implements Runnable {
        @Override
        public void run() {
            synchronized (mLock) {
                mRestorePendingAppPermissions = null;
                if (mRestorePackageMonitor != null) {
                    mRestorePackageMonitor.unregister();
                    mRestorePackageMonitor = null;
                }
            }
        }
    }
}
