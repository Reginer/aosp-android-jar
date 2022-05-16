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
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ImsCallbackAdapterManager<T extends IInterface> {
    private static final String TAG = "ImsCallbackAM";

    private final Context mContext;
    private final Object mLock;
    private final int mSlotId;

    // Map of sub id -> List<callbacks> for sub id linked callbacks.
    private final SparseArray<Set<T>> mCallbackSubscriptionMap = new SparseArray<>();

    // List of all active callbacks to ImsService
    private final RemoteCallbackList<T> mRemoteCallbacks = new RemoteCallbackList<>();

    @VisibleForTesting
    public SubscriptionManager.OnSubscriptionsChangedListener mSubChangedListener;

    public ImsCallbackAdapterManager(Context context, Object lock, int slotId) {
        mContext = context;
        mLock = lock;
        mSlotId = slotId;

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        // Must be created after Looper.prepare() is called, or else we will get an exception.
        mSubChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                SubscriptionManager manager = mContext.getSystemService(SubscriptionManager.class);
                if (manager == null) {
                    Log.w(TAG + " [" + mSlotId + "]", "onSubscriptionsChanged: could not find "
                            + "SubscriptionManager.");
                    return;
                }

                List<SubscriptionInfo> subInfos = manager.getActiveSubscriptionInfoList(false);
                if (subInfos == null) {
                    subInfos = Collections.emptyList();
                }

                Set<Integer> newSubIds = subInfos.stream()
                        .map(SubscriptionInfo::getSubscriptionId)
                        .collect(Collectors.toSet());

                synchronized (mLock) {
                    Set<Integer> storedSubIds = new ArraySet<>(mCallbackSubscriptionMap.size());
                    for (int keyIndex = 0; keyIndex < mCallbackSubscriptionMap.size();
                            keyIndex++) {
                        storedSubIds.add(mCallbackSubscriptionMap.keyAt(keyIndex));
                    }

                    // Get the set of sub ids that are in storedSubIds that are not in newSubIds.
                    // This is the set of sub ids that need to be removed.
                    storedSubIds.removeAll(newSubIds);

                    for (Integer subId : storedSubIds) {
                        removeCallbacksForSubscription(subId);
                    }
                }
            }
        };
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

    // Add a callback to be associated with a subscription. If that subscription is removed,
    // remove the callback and notify the callback that the subscription has been removed.
    public void addCallbackForSubscription(T localCallback, int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.w(TAG + " [" + mSlotId + "]", "add callback: invalid subId " + subId);
            return;
        }
        synchronized (mLock) {
            addCallback(localCallback);
            linkCallbackToSubscription(localCallback, subId);
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

    // Remove an existing callback that has been linked to a subscription.
    public void removeCallbackForSubscription(T localCallback, int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.w(TAG + " [" + mSlotId + "]", "remove callback: invalid subId " + subId);
            return;
        }
        synchronized (mLock) {
            removeCallback(localCallback);
            unlinkCallbackFromSubscription(localCallback, subId);
        }
    }

    // Links a callback to be tracked by a subscription. If it goes away, emove.
    private void linkCallbackToSubscription(T callback, int subId) {
        synchronized (mLock) {
            if (mCallbackSubscriptionMap.size() == 0) {
                // we are about to add the first entry to the map, register for subscriptions
                //changed listener.
                registerForSubscriptionsChanged();
            }
            Set<T> callbacksPerSub = mCallbackSubscriptionMap.get(subId);
            if (callbacksPerSub == null) {
                // the callback list has not been created yet for this subscription.
                callbacksPerSub = new ArraySet<>();
                mCallbackSubscriptionMap.put(subId, callbacksPerSub);
            }
            callbacksPerSub.add(callback);
        }
    }

    // Unlink the callback from the associated subscription.
    private void unlinkCallbackFromSubscription(T callback, int subId) {
        synchronized (mLock) {
            Set<T> callbacksPerSub = mCallbackSubscriptionMap.get(subId);
            if (callbacksPerSub != null) {
                callbacksPerSub.remove(callback);
                if (callbacksPerSub.isEmpty()) {
                    mCallbackSubscriptionMap.remove(subId);
                }
            }
            if (mCallbackSubscriptionMap.size() == 0) {
                unregisterForSubscriptionsChanged();
            }
        }
    }

    // Removes all of the callbacks that have been registered to the subscription specified.
    // This happens when Telephony sends an indication that the subscriptions have changed.
    private void removeCallbacksForSubscription(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.w(TAG + " [" + mSlotId + "]", "remove all callbacks: invalid subId " + subId);
            return;
        }
        synchronized (mLock) {
            Set<T> callbacksPerSub = mCallbackSubscriptionMap.get(subId);
            if (callbacksPerSub == null) {
                // no callbacks registered for this subscription.
                return;
            }
            // clear all registered callbacks in the subscription map for this subscription.
            mCallbackSubscriptionMap.remove(subId);
            for (T callback : callbacksPerSub) {
                removeCallback(callback);
            }
            // If there are no more callbacks being tracked, remove subscriptions changed
            // listener.
            if (mCallbackSubscriptionMap.size() == 0) {
                unregisterForSubscriptionsChanged();
            }
        }
    }

    // Clear the Subscription -> Callback map because the ImsService connection is no longer
    // current.
    private void clearCallbacksForAllSubscriptions() {
        synchronized (mLock) {
            List<Integer> keys = new ArrayList<>();
            for (int keyIndex = 0; keyIndex < mCallbackSubscriptionMap.size(); keyIndex++) {
                keys.add(mCallbackSubscriptionMap.keyAt(keyIndex));
            }
            keys.forEach(this::removeCallbacksForSubscription);
        }
    }

    private void registerForSubscriptionsChanged() {
        SubscriptionManager manager = mContext.getSystemService(SubscriptionManager.class);
        if (manager != null) {
            manager.addOnSubscriptionsChangedListener(mSubChangedListener);
        } else {
            Log.w(TAG + " [" + mSlotId + "]", "registerForSubscriptionsChanged: could not find"
                    + " SubscriptionManager.");
        }
    }

    private void unregisterForSubscriptionsChanged() {
        SubscriptionManager manager = mContext.getSystemService(SubscriptionManager.class);
        if (manager != null) {
        manager.removeOnSubscriptionsChangedListener(mSubChangedListener);
        } else {
            Log.w(TAG + " [" + mSlotId + "]", "unregisterForSubscriptionsChanged: could not"
                    + " find SubscriptionManager.");
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
            clearCallbacksForAllSubscriptions();
            Log.i(TAG + " [" + mSlotId + "]", "Closing connection and clearing callbacks");
        }
    }

    // A callback has been registered. Register that callback with the ImsFeature.
    public abstract void registerCallback(T localCallback);

    // A callback has been removed, unregister that callback with the RcsFeature.
    public abstract void unregisterCallback(T localCallback);
}
