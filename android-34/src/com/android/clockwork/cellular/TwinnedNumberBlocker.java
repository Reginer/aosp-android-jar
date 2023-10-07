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

package com.android.clockwork.cellular;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BlockedNumberContract;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TwinnedNumberBlocker determines if watch needs to set the Google Fi's special rerouting
 * caller id as a spam caller id. Once it is set as a spam caller id, the dark number on watch will
 * not be reachable (Call and SMS).
 *
 * Design doc: go/fi-dark-number-spam-detection-on-wear
 */
public class TwinnedNumberBlocker {
    public static final String TAG = TwinnedNumberBlocker.class.getSimpleName();
    public static final String FI_SPAM_CALLER_ID = "14082560700";
    public static final int FI_CARRIER_ID = 1989;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private TelephonyManager mTelephonyManager;
    private ContentResolver mContentResolver;
    private Context mContext;

    public TwinnedNumberBlocker(Context context) {
        this.mContext = context;
    }

    void onActiveSubscriptionChanged(@Nullable SubscriptionInfo info) {
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mContentResolver = mContext.getContentResolver();
        if (mTelephonyManager == null || mContentResolver == null) {
            Log.e(TAG, "Some system services are not ready yet -> "
                    + " TelephonyManager : " + (mTelephonyManager != null)
                    + " ContentResolver : " + (mContentResolver != null));
            return;
        }
        EXECUTOR.submit(() -> {
            setFiSpamBlockingEnabled(info != null && info.getCarrierId() == FI_CARRIER_ID);
        });
    }

    /**
     * Deletes or insert the Fi routing caller id to Telecom component's spam number database.
     * @param enable insert the caller id if true, and delete otherwise.
     */
    private void setFiSpamBlockingEnabled(boolean enable) {
        ContentValues values = new ContentValues();
        values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, FI_SPAM_CALLER_ID);
        Uri uri = mContentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
        if (!enable) {
            mContentResolver.delete(uri, null, null);
            Log.i(TAG, "Removing Fi spam caller id");
        } else {
            Log.i(TAG, "Writing Fi spam caller id");
        }
    }

}
