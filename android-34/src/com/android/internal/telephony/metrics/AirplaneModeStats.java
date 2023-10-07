/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony.metrics;

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;
import static android.telephony.TelephonyManager.UNKNOWN_CARRIER_ID;

import static com.android.internal.telephony.TelephonyStatsLog.AIRPLANE_MODE;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.telephony.Rlog;

/** Metrics for the usage of airplane mode. */
public class AirplaneModeStats extends ContentObserver {
    private static final String TAG = AirplaneModeStats.class.getSimpleName();

    /** Ignore airplane mode events occurring in the first 30 seconds. */
    private static final long GRACE_PERIOD_MILLIS = 30000L;

    /** An airplane mode toggle is considered short if under 10 seconds. */
    private static final long SHORT_TOGGLE_MILLIS = 10000L;

    private long mLastActivationTime = 0L;

    private final Context mContext;
    private final Uri mAirplaneModeSettingUri;

    public AirplaneModeStats(Context context) {
        super(new Handler(Looper.getMainLooper()));

        mContext = context;
        mAirplaneModeSettingUri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);

        context.getContentResolver().registerContentObserver(mAirplaneModeSettingUri, false, this);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (uri.equals(mAirplaneModeSettingUri)) {
            onAirplaneModeChanged(isAirplaneModeOn());
        }
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /** Generate metrics when airplane mode is enabled or disabled. */
    private void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        Rlog.d(TAG, "Airplane mode change. Value: " + isAirplaneModeOn);
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime < GRACE_PERIOD_MILLIS) {
            return;
        }

        boolean isShortToggle = calculateShortToggle(currentTime, isAirplaneModeOn);
        int carrierId = getCarrierId();

        Rlog.d(TAG, "Airplane mode: " + isAirplaneModeOn + ", short=" + isShortToggle
                + ", carrierId=" + carrierId);
        TelephonyStatsLog.write(AIRPLANE_MODE, isAirplaneModeOn, isShortToggle, carrierId);
    }


    /* Keep tracks of time and returns if it was a short toggle. */
    private boolean calculateShortToggle(long currentTime, boolean isAirplaneModeOn) {
        boolean isShortToggle = false;
        if (isAirplaneModeOn) {
            // When airplane mode is enabled, track the time.
            if (mLastActivationTime == 0L) {
                mLastActivationTime = currentTime;
            }
            return false;
        } else {
            // When airplane mode is disabled, reset the time and check if it was a short toggle.
            long duration = currentTime - mLastActivationTime;
            mLastActivationTime = 0L;
            return duration > 0 && duration < SHORT_TOGGLE_MILLIS;
        }
    }

    /**
     * Returns the carrier ID of the active data subscription. If this is not available,
     * it returns the carrier ID of the first phone.
     */
    private static int getCarrierId() {
        int dataSubId = SubscriptionManager.getActiveDataSubscriptionId();
        int phoneId = dataSubId != INVALID_SUBSCRIPTION_ID
                ? SubscriptionManager.getPhoneId(dataSubId) : 0;
        Phone phone = PhoneFactory.getPhone(phoneId);
        return phone != null ? phone.getCarrierId() : UNKNOWN_CARRIER_ID;
    }
}
