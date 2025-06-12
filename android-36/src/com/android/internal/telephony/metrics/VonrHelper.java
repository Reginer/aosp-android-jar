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

package com.android.internal.telephony.metrics;

import static com.android.internal.telephony.metrics.MetricsCollector.getPhonesIfAny;
import static com.android.internal.telephony.metrics.PerSimStatus.isVonrEnabled;

import android.annotation.NonNull;
import android.os.Handler;
import android.os.HandlerThread;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.flags.FeatureFlags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vonr state handler.
 *
 * <p>This class is instantiated in {@link MetricsCollector}.
*/
public class VonrHelper {
    private final @NonNull FeatureFlags mFlags;

    private Handler mHandler;
    private Handler mHandlerThread;
    private Map<Integer, Boolean> mPhoneVonrState = new ConcurrentHashMap<>();

    public VonrHelper(@NonNull FeatureFlags featureFlags) {
        this.mFlags = featureFlags;
        if (mFlags.threadShred()) {
            mHandler = new Handler(BackgroundThread.get().getLooper());
        } else {
            HandlerThread mHandlerThread = new HandlerThread("VonrHelperThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }
    }

    /** Update vonr_enabled state */
    public void updateVonrEnabledState() {
        mHandler.post(mVonrRunnable);
    }

    @VisibleForTesting
    protected Runnable mVonrRunnable =
            new Runnable() {
                @Override
                public void run() {
                    mPhoneVonrState.clear();
                    for (Phone phone : getPhonesIfAny()) {
                        mPhoneVonrState.put(phone.getSubId(), isVonrEnabled(phone));
                    }
                }
            };

    /** Get vonr_enabled per subId */
    public boolean getVonrEnabled(int subId) {
        return mPhoneVonrState.getOrDefault(subId, false);
    }
}
