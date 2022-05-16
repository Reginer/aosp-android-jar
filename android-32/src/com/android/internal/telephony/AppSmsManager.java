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

package com.android.internal.telephony;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 *  Manager for app specific SMS requests. This can be used to implement SMS based
 *  communication channels (e.g. for SMS based phone number verification) without needing the
 *  {@link Manifest.permission#RECEIVE_SMS} permission.
 *
 *  {@link #createAppSpecificSmsRequest} allows an application to provide a {@link PendingIntent}
 *  that is triggered when an incoming SMS is received that contains the provided token.
 */
public class AppSmsManager {
    private static final String LOG_TAG = "AppSmsManager";

    private static final long TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private final SecureRandom mRandom;
    private final Context mContext;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final Map<String, AppRequestInfo> mTokenMap;
    @GuardedBy("mLock")
    private final Map<String, AppRequestInfo> mPackageMap;

    public AppSmsManager(Context context) {
        mRandom = new SecureRandom();
        mTokenMap = new ArrayMap<>();
        mPackageMap = new ArrayMap<>();
        mContext = context;
    }

    /**
     * Create an app specific incoming SMS request for the the calling package.
     *
     * This method returns a token that if included in a subsequent incoming SMS message the
     * {@link Intents.SMS_RECEIVED_ACTION} intent will be delivered only to the calling package and
     * will not require the application have the {@link Manifest.permission#RECEIVE_SMS} permission.
     *
     * An app can only have one request at a time, if the app already has a request it will be
     * dropped and the new one will be added.
     *
     * @return Token to include in an SMS to have it delivered directly to the app.
     */
    public String createAppSpecificSmsToken(String callingPkg, PendingIntent intent) {
        // Check calling uid matches callingpkg.
        AppOpsManager appOps = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        appOps.checkPackage(Binder.getCallingUid(), callingPkg);

        // Generate a nonce to store the request under.
        String token = generateNonce();
        synchronized (mLock) {
            // Only allow one request in flight from a package.
            if (mPackageMap.containsKey(callingPkg)) {
                removeRequestLocked(mPackageMap.get(callingPkg));
            }
            // Store state.
            AppRequestInfo info = new AppRequestInfo(callingPkg, intent, token);
            addRequestLocked(info);
        }
        return token;
    }

    /**
     * Create an app specific incoming SMS request for the the calling package.
     *
     * This method returns a token that if included in a subsequent incoming SMS message the
     * {@link Intents.SMS_RECEIVED_ACTION} intent will be delivered only to the calling package and
     * will not require the application have the {@link Manifest.permission#RECEIVE_SMS} permission.
     *
     * An app can only have one request at a time, if the app already has a request it will be
     * dropped and the new one will be added.
     *
     * @return Token to include in an SMS to have it delivered directly to the app.
     */
    public String createAppSpecificSmsTokenWithPackageInfo(int subId,
            @NonNull String callingPackageName,
            @Nullable String prefixes,
            @NonNull PendingIntent intent) {
        Preconditions.checkStringNotEmpty(callingPackageName,
                "callingPackageName cannot be null or empty.");
        Preconditions.checkNotNull(intent, "intent cannot be null");
        // Check calling uid matches callingpkg.
        AppOpsManager appOps = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        appOps.checkPackage(Binder.getCallingUid(), callingPackageName);

        // Generate a token to store the request under.
        String token = PackageBasedTokenUtil.generateToken(mContext, callingPackageName);
        if (token != null) {
            synchronized (mLock) {
                // Only allow one request in flight from a package.
                if (mPackageMap.containsKey(callingPackageName)) {
                    removeRequestLocked(mPackageMap.get(callingPackageName));
                }
                // Store state.
                AppRequestInfo info = new AppRequestInfo(
                        callingPackageName, intent, token, prefixes, subId, true);
                addRequestLocked(info);
            }
        }
        return token;
    }

    /**
     * Handle an incoming SMS_DELIVER_ACTION intent if it is an app-only SMS.
     */
    public boolean handleSmsReceivedIntent(Intent intent) {
        // Correctness check the action.
        if (intent.getAction() != Intents.SMS_DELIVER_ACTION) {
            Log.wtf(LOG_TAG, "Got intent with incorrect action: " + intent.getAction());
            return false;
        }

        synchronized (mLock) {
            removeExpiredTokenLocked();

            String message = extractMessage(intent);
            if (TextUtils.isEmpty(message)) {
                return false;
            }

            AppRequestInfo info = findAppRequestInfoSmsIntentLocked(message);
            if (info == null) {
                // The message didn't contain a token -- nothing to do.
                return false;
            }

            try {
                Intent fillIn = new Intent()
                        .putExtras(intent.getExtras())
                        .putExtra(SmsManager.EXTRA_STATUS, SmsManager.RESULT_STATUS_SUCCESS)
                        .putExtra(SmsManager.EXTRA_SMS_MESSAGE, message)
                        .putExtra(SmsManager.EXTRA_SIM_SUBSCRIPTION_ID, info.subId)
                        .addFlags(Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS);

                info.pendingIntent.send(mContext, 0, fillIn);
            } catch (PendingIntent.CanceledException e) {
                // The pending intent is canceled, send this SMS as normal.
                removeRequestLocked(info);
                return false;
            }

            removeRequestLocked(info);
            return true;
        }
    }

    private void removeExpiredTokenLocked() {
        final long currentTimeMillis = System.currentTimeMillis();

        Iterator<Map.Entry<String, AppRequestInfo>> iterator = mTokenMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AppRequestInfo> entry = iterator.next();
            AppRequestInfo request = entry.getValue();
            if (request.packageBasedToken
                    && (currentTimeMillis - TIMEOUT_MILLIS > request.timestamp)) {
                // Send the provided intent with SMS retriever status
                try {
                    Intent fillIn = new Intent()
                            .putExtra(SmsManager.EXTRA_STATUS,
                                    SmsManager.RESULT_STATUS_TIMEOUT)
                            .addFlags(Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS);
                    request.pendingIntent.send(mContext, 0, fillIn);
                } catch (PendingIntent.CanceledException e) {
                    // do nothing
                }
                // Remove from mTokenMap and mPackageMap
                iterator.remove();
                mPackageMap.remove(entry.getValue().packageName);
            }
        }
    }

    private String extractMessage(Intent intent) {
        SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
        if (messages == null) {
            return null;
        }
        StringBuilder fullMessageBuilder = new StringBuilder();
        for (SmsMessage message : messages) {
            if (message == null || message.getMessageBody() == null) {
                continue;
            }
            fullMessageBuilder.append(message.getMessageBody());
        }

        return fullMessageBuilder.toString();
    }

    private AppRequestInfo findAppRequestInfoSmsIntentLocked(String fullMessage) {
        // Look for any tokens in the full message.
        for (String token : mTokenMap.keySet()) {
            if (fullMessage.trim().contains(token) && hasPrefix(token, fullMessage)) {
                return mTokenMap.get(token);
            }
        }
        return null;
    }

    private String generateNonce() {
        byte[] bytes = new byte[8];
        mRandom.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private boolean hasPrefix(String token, String message) {
        AppRequestInfo request = mTokenMap.get(token);
        if (TextUtils.isEmpty(request.prefixes)) {
            return true;
        }

        String[] prefixes = request.prefixes.split(SmsManager.REGEX_PREFIX_DELIMITER);
        for (String prefix : prefixes) {
            if (message.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void removeRequestLocked(AppRequestInfo info) {
        mTokenMap.remove(info.token);
        mPackageMap.remove(info.packageName);
    }

    private void addRequestLocked(AppRequestInfo info) {
        mTokenMap.put(info.token, info);
        mPackageMap.put(info.packageName, info);
    }

    private final class AppRequestInfo {
        public final String packageName;
        public final PendingIntent pendingIntent;
        public final String token;
        public final long timestamp;
        public final String prefixes;
        public final int subId;
        public final boolean packageBasedToken;

        AppRequestInfo(String packageName, PendingIntent pendingIntent, String token) {
          this(packageName, pendingIntent, token, null,
                  SubscriptionManager.INVALID_SUBSCRIPTION_ID, false);
        }

        AppRequestInfo(String packageName, PendingIntent pendingIntent, String token,
                String prefixes, int subId, boolean packageBasedToken) {
            this.packageName = packageName;
            this.pendingIntent = pendingIntent;
            this.token = token;
            this.timestamp = System.currentTimeMillis();
            this.prefixes = prefixes;
            this.subId = subId;
            this.packageBasedToken = packageBasedToken;
        }
    }

}
