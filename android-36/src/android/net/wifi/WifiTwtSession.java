/*
 * Copyright (C) 2024 The Android Open Source Project
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
package android.net.wifi;

import android.net.wifi.twt.TwtSession;
import android.os.Binder;
import android.os.Bundle;
import android.util.CloseGuard;
import android.util.Log;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Implementation of the interface {@link TwtSession}
 *
 * @hide
 */
public class WifiTwtSession implements TwtSession {
    private static final String TAG = "WifiTwtSession";
    public static final int MAX_TWT_SESSIONS = 8;
    private final int mWakeDurationMicros;
    private final long mWakeIntervalMicros;
    private final int mMloLinkId;
    private final int mOwner;
    private final int mSessionId;
    private final WeakReference<WifiManager> mMgr;
    private final CloseGuard mCloseGuard = new CloseGuard();

    @Override
    public int getWakeDurationMicros() {
        return mWakeDurationMicros;
    }

    @Override
    public long getWakeIntervalMicros() {
        return mWakeIntervalMicros;
    }

    @Override
    public int getMloLinkId() {
        return mMloLinkId;
    }

    public int getOwner() {
        return mOwner;
    }

    public int getSessionId() {
        return mSessionId;
    }

    @Override
    public void getStats(Executor executor, Consumer<Bundle> resultCallback) {
        WifiManager mgr = mMgr.get();
        if (mgr == null) {
            Log.e(TAG, "getStats: called post garbage collection");
            return;
        }
        if (Binder.getCallingUid() != mOwner) {
            throw new SecurityException("TWT session is not owned by the caller");
        }
        mgr.getStatsTwtSession(mSessionId, executor, resultCallback);
    }

    public WifiTwtSession(WifiManager wifiManager, int wakeDurationMicros, long wakeIntervalMicros,
            int mloLinkId, int owner, int sessionId) {
        mMgr = new WeakReference<>(wifiManager);
        mWakeDurationMicros = wakeDurationMicros;
        mWakeIntervalMicros = wakeIntervalMicros;
        mMloLinkId = mloLinkId;
        mOwner = owner;
        mSessionId = sessionId;
        mCloseGuard.open("teardown");
    }

    @Override
    public void teardown() {
        try {
            WifiManager mgr = mMgr.get();
            if (mgr == null) {
                Log.w(TAG, "close: called post garbage collection");
                return;
            }
            mgr.teardownTwtSession(mSessionId);
            mMgr.clear();
            mCloseGuard.close();
        } finally {
            Reference.reachabilityFence(this);
        }
    }
}
