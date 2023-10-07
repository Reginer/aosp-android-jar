/*
 * Copyright (c) 2019 The Android Open Source Project
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

package com.android.ims;

import android.content.Context;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.telephony.SubscriptionManager;
import android.util.Log;


public abstract class ImsCallbackAdapterManager<T extends IInterface> {
    private static final String TAG = "ImsCallbackAM";

    private final Context mContext;
    private final Object mLock;
    private final int mSlotId;
    private final int mSubId;

    // List of all active callbacks to ImsService
    private final RemoteCallbackList<T> mRemoteCallbacks = new RemoteCallbackList<>();

    public ImsCallbackAdapterManager(Context context, Object lock, int slotId, int subId) {
        mContext = context;
        mLock = lock;
        mSlotId = slotId;
        mSubId = subId;
    }

    // Add a callback to the ImsFeature associated with this manager (independent of the
    // current subscription).
    public final void addCallback(T localCallback) {
        synchronized (mLock) {
            // Skip registering to callback subscription map here, because we are registering
            // for the slot, independent of subscription (deprecated behavior).
            // Throws a IllegalStateException if this registration fails.
            registerCallback(localCallback);
            Log.i(TAG + " [" + mSlotId + "]", "Local callback added: " + localCallback);

            mRemoteCallbacks.register(localCallback);
        }
    }

    // Add a callback to be associated with a subscription.
    public void addCallbackForSubscription(T localCallback, int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.w(TAG + " [" + mSlotId + ", " + mSubId + "]", "add callback: invalid subId.");
            return;
        }
        if (mSubId != subId) {
            // In some cases, telephony has changed sub id and IMS is still catching up to the
            // state change. Ensure that the device does not try to register a callback on an
            // inactive subscription, because this can cause a condition where we remove the
            // callback invisibly when the new subscription loads. Instead, simulate the existing
            // IllegalStateException that happens when the ImsService is not ready/active for
            // backwards compatibility.
            Log.w(TAG + " [" + mSlotId + ", " + mSubId + "]", "add callback: inactive"
                    + " subID detected: " + subId);
            throw new IllegalStateException("ImsService is not available for the subscription "
                    + "specified.");
        }
        synchronized (mLock) {
            addCallback(localCallback);
        }
    }

    // Removes a callback associated with the ImsFeature.
    public final void removeCallback(T localCallback) {
        Log.i(TAG + " [" + mSlotId + "]", "Local callback removed: " + localCallback);
        synchronized (mLock) {
            if (mRemoteCallbacks.unregister(localCallback)) {
                // Will only occur if we have record of this callback in mRemoteCallbacks.
                unregisterCallback(localCallback);
            }
        }
    }

    // The ImsService these callbacks are registered to has become unavailable or crashed, or
    // the ImsResolver has switched to a new ImsService. In these cases, clean up all existing
    // callbacks.
    public final void close() {
        synchronized (mLock) {
            final int lastCallbackIndex = mRemoteCallbacks.getRegisteredCallbackCount() - 1;
            for(int ii = lastCallbackIndex; ii >= 0; ii --) {
                T callbackItem = mRemoteCallbacks.getRegisteredCallbackItem(ii);
                unregisterCallback(callbackItem);
                mRemoteCallbacks.unregister(callbackItem);
            }
            Log.i(TAG + " [" + mSlotId + "]", "Closing connection and clearing callbacks");
        }
    }

    // A callback has been registered. Register that callback with the ImsFeature.
    public abstract void registerCallback(T localCallback);

    // A callback has been removed, unregister that callback with the RcsFeature.
    public abstract void unregisterCallback(T localCallback);
}
