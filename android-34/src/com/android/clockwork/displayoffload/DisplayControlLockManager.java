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

package com.android.clockwork.displayoffload;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;


public class DisplayControlLockManager {
    private static final String TAG = "DisplayControlLock";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.VERBOSE);

    private final ArrayMap<IBinder, DeathRecipient> mLocks = new ArrayMap<>();
    private final Runnable mOnFirstLockAcquired;
    private final Runnable mOnAllPendingLockRelease;

    DisplayControlLockManager(Runnable onFirstLockAcquired, Runnable onAllPendingLockRelease) {
        mOnFirstLockAcquired = onFirstLockAcquired;
        mOnAllPendingLockRelease = onAllPendingLockRelease;
    }

    boolean acquire(IBinder token, String name) {
        if (token == null) return false;
        boolean retVal = true;
        boolean firstLock = false;
        synchronized (this) {
            try {
                DeathRecipient lock = new DeathRecipient(token);
                if (mLocks.size() == 0) {
                    firstLock = true;
                }
                mLocks.put(token, lock);
            } catch (RemoteException e) {
                // Binder already dead, do nothing
                retVal = false;
            }
            if (DEBUG) {
                Log.d(TAG, "acquired: " + name + " " + token);
            }
        }

        if (firstLock) {
            Log.i(TAG, "First acquired");
            mOnFirstLockAcquired.run();
        }

        return retVal;
    }

    void release(IBinder token) {
        if (token == null) return;
        final boolean allReleased;
        synchronized (this) {
            DeathRecipient deathRecipient = mLocks.get(token);
            if (deathRecipient == null) {
                // Token is not acquired
                return;
            }
            token.unlinkToDeath(deathRecipient, 0);
            mLocks.remove(token);
            allReleased = mLocks.isEmpty();
            if (DEBUG) {
                Log.d(TAG, "released: " + token);
            }
        }

        if (allReleased) {
            Log.i(TAG, "All released");
            mOnAllPendingLockRelease.run();
        }
    }

    synchronized boolean shouldBlock() {
        // Block if there's unreleased locks
        return !mLocks.isEmpty();
    }

    private class DeathRecipient implements IBinder.DeathRecipient {
        public final IBinder mToken;

        DeathRecipient(IBinder token) throws RemoteException {
            mToken = token;
            mToken.linkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            release(mToken);
        }
    }
}
