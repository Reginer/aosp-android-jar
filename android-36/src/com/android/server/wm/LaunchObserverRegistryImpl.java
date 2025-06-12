/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.wm;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.android.internal.util.function.pooled.PooledLambda;

import java.util.ArrayList;

/**
 * Multi-cast implementation of {@link ActivityMetricsLaunchObserver}.
 *
 * <br /><br />
 * If this class is called through the {@link ActivityMetricsLaunchObserver} interface,
 * then the call is forwarded to all registered observers at the time.
 *
 * <br /><br />
 * All calls are invoked asynchronously in-order on a background thread. This fulfills the
 * sequential ordering guarantee in {@link ActivityMetricsLaunchObserverRegistry}.
 *
 * @see ActivityTaskManagerInternal#getLaunchObserverRegistry()
 */
class LaunchObserverRegistryImpl extends ActivityMetricsLaunchObserver implements
        ActivityMetricsLaunchObserverRegistry {
    private final ArrayList<ActivityMetricsLaunchObserver> mList = new ArrayList<>();

    /**
     * All calls are posted to a handler because:
     *
     * 1. We don't know how long the observer will take to handle this call and we don't want
     *    to block the WM critical section on it.
     * 2. We don't know the lock ordering of the observer so we don't want to expose a chance
     *    of deadlock.
     */
    private final Handler mHandler;

    public LaunchObserverRegistryImpl(Looper looper) {
        mHandler = new Handler(looper);
    }

    @Override
    public void registerLaunchObserver(ActivityMetricsLaunchObserver launchObserver) {
        mHandler.sendMessage(PooledLambda.obtainMessage(
                LaunchObserverRegistryImpl::handleRegisterLaunchObserver, this, launchObserver));
    }

    @Override
    public void unregisterLaunchObserver(ActivityMetricsLaunchObserver launchObserver) {
        mHandler.sendMessage(PooledLambda.obtainMessage(
                LaunchObserverRegistryImpl::handleUnregisterLaunchObserver, this, launchObserver));
    }

    @Override
    public void onIntentStarted(Intent intent, long timestampNs) {
        mHandler.sendMessage(PooledLambda.obtainMessage(
                LaunchObserverRegistryImpl::handleOnIntentStarted,
                this,
                intent,
                timestampNs));
    }

    @Override
    public void onIntentFailed(long id) {
        mHandler.sendMessage(PooledLambda.obtainMessage(
                LaunchObserverRegistryImpl::handleOnIntentFailed, this, id));
    }

    @Override
    public void onActivityLaunched(long id, ComponentName name, int temperature, int userId) {
        mHandler.sendMessage(PooledLambda.obtainMessage(
                LaunchObserverRegistryImpl::handleOnActivityLaunched,
                this, id, name, temperature, userId));
    }

    @Override
    public void onActivityLaunchCancelled(long id) {
        mHandler.sendMessage(PooledLambda.obtainMessage(
                LaunchObserverRegistryImpl::handleOnActivityLaunchCancelled, this, id));
    }

    @Override
    public void onActivityLaunchFinished(long id, ComponentName name, long timestampNs,
            int launchMode) {
        mHandler.sendMessage(PooledLambda.obtainMessage(
                LaunchObserverRegistryImpl::handleOnActivityLaunchFinished,
                this, id, name, timestampNs, launchMode));
    }

    @Override
    public void onReportFullyDrawn(long id, long timestampNs) {
        mHandler.sendMessage(PooledLambda.obtainMessage(
                LaunchObserverRegistryImpl::handleOnReportFullyDrawn,
                this, id, timestampNs));
    }

    // Use PooledLambda.obtainMessage to invoke below methods. Every method reference must be
    // unbound (i.e. not capture any variables explicitly or implicitly) to fulfill the
    // singleton-lambda requirement.

    private void handleRegisterLaunchObserver(ActivityMetricsLaunchObserver observer) {
        mList.add(observer);
    }

    private void handleUnregisterLaunchObserver(ActivityMetricsLaunchObserver observer) {
        mList.remove(observer);
    }

    private void handleOnIntentStarted(Intent intent, long timestampNs) {
        // Traverse start-to-end to meet the registerLaunchObserver multi-cast order guarantee.
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).onIntentStarted(intent, timestampNs);
        }
    }

    private void handleOnIntentFailed(long id) {
        // Traverse start-to-end to meet the registerLaunchObserver multi-cast order guarantee.
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).onIntentFailed(id);
        }
    }

    private void handleOnActivityLaunched(long id, ComponentName name,
            @Temperature int temperature, int userId) {
        // Traverse start-to-end to meet the registerLaunchObserver multi-cast order guarantee.
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).onActivityLaunched(id, name, temperature, userId);
        }
    }

    private void handleOnActivityLaunchCancelled(long id) {
        // Traverse start-to-end to meet the registerLaunchObserver multi-cast order guarantee.
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).onActivityLaunchCancelled(id);
        }
    }

    private void handleOnActivityLaunchFinished(long id, ComponentName name, long timestampNs,
            int launchMode) {
        // Traverse start-to-end to meet the registerLaunchObserver multi-cast order guarantee.
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).onActivityLaunchFinished(id, name, timestampNs, launchMode);
        }
    }

    private void handleOnReportFullyDrawn(long id, long timestampNs) {
        // Traverse start-to-end to meet the registerLaunchObserver multi-cast order guarantee.
        for (int i = 0; i < mList.size(); i++) {
            mList.get(i).onReportFullyDrawn(id, timestampNs);
        }
    }
}
