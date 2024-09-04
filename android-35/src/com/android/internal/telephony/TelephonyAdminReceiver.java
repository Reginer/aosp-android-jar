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

package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.telephony.Rlog;

/**
 * A BroadcastReceiver for device administration events.
 */
public class TelephonyAdminReceiver extends BroadcastReceiver {
    private static final String TAG = "TelephonyAdminReceiver";
    private final Phone mPhone;
    // We keep track of the last value to avoid updating when unrelated user restrictions change
    private boolean mDisallowCellular2gRestriction = false;
    private final Context mContext;
    private UserManager mUserManager;

    public TelephonyAdminReceiver(Context context, Phone phone) {
        mContext = context;
        mPhone = phone;
        mUserManager = null;
        if (ensureUserManagerExists()) {
            mDisallowCellular2gRestriction = mUserManager.hasUserRestriction(
                    UserManager.DISALLOW_CELLULAR_2G);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(UserManager.ACTION_USER_RESTRICTIONS_CHANGED);
        context.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Rlog.d(TAG, "Processing onReceive");
        if (context == null || intent == null) return;
        if (!intent.getAction().equals(UserManager.ACTION_USER_RESTRICTIONS_CHANGED)) {
            Rlog.d(TAG, "Ignoring unexpected action: " + intent.getAction());
            return;
        }
        if (!ensureUserManagerExists()) {
            return;
        }
        boolean shouldDisallow2g = mUserManager.hasUserRestriction(
                UserManager.DISALLOW_CELLULAR_2G);

        if (shouldDisallow2g != mDisallowCellular2gRestriction) {
            Rlog.i(TAG,
                    "Updating allowed network types with new admin 2g restriction. no_cellular_2g: "
                            + shouldDisallow2g);
            mDisallowCellular2gRestriction = shouldDisallow2g;
            mPhone.sendSubscriptionSettings(false);
        } else {
            Rlog.i(TAG, "Skipping update of allowed network types. Restriction no_cellular_2g "
                    + "unchanged: " + mDisallowCellular2gRestriction);
        }
    }

    /**
     * Returns the current state of the {@link UserManager#DISALLOW_CELLULAR_2G} user restriction.
     */
    public boolean isCellular2gDisabled() {
        return mDisallowCellular2gRestriction;
    }

    /**
     * Tries to resolve the user manager system service. Returns true if successful, false
     * otherwise.
     */
    private boolean ensureUserManagerExists() {
        if (mUserManager == null) {
            Rlog.d(TAG, "No user manager. Attempting to resolve one.");
            mUserManager = mContext.getSystemService(UserManager.class);
        }
        if (mUserManager == null) {
            Rlog.e(TAG,
                    "Could not get a user manager instance. All operations will be no-ops until "
                            + "one is resolved");
            return false;
        }
        return true;
    }
}
