/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.android.telephony.Rlog;

import java.util.HashMap;
import java.util.Map;

/**
 * The class to describe settings observer
 */
public class SettingsObserver extends ContentObserver {
    private final Map<Uri, Integer> mUriEventMap;
    private final Context mContext;
    private final Handler mHandler;
    private static final String TAG = "SettingsObserver";

    public SettingsObserver(Context context, Handler handler) {
        super(null);
        mUriEventMap = new HashMap<>();
        mContext = context;
        mHandler = handler;
    }

    /**
     * Start observing a content.
     * @param uri Content URI
     * @param what The event to fire if the content changes
     */
    public void observe(Uri uri, int what) {
        mUriEventMap.put(uri, what);
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(uri, false, this);
    }

    /**
     * Stop observing a content.
     */
    public void unobserve() {
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.unregisterContentObserver(this);
    }

    @Override
    public void onChange(boolean selfChange) {
        Rlog.e(TAG, "Should never be reached.");
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        final Integer what = mUriEventMap.get(uri);
        if (what != null) {
            mHandler.obtainMessage(what.intValue()).sendToTarget();
        } else {
            Rlog.e(TAG, "No matching event to send for URI=" + uri);
        }
    }
}
