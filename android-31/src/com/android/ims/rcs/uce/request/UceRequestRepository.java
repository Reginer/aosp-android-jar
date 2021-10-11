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

package com.android.ims.rcs.uce.request;

import com.android.ims.rcs.uce.request.UceRequestManager.RequestManagerCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for storing the capabilities request.
 */
public class UceRequestRepository {

    // Dispatch the UceRequest to be executed.
    private final UceRequestDispatcher mDispatcher;

    // Store all the capabilities requests
    private final Map<Long, UceRequestCoordinator> mRequestCoordinators;

    private volatile boolean mDestroyed = false;

    public UceRequestRepository(int subId, RequestManagerCallback callback) {
        mRequestCoordinators = new HashMap<>();
        mDispatcher = new UceRequestDispatcher(subId, callback);
    }

    /**
     * Clear the collection when the instance is destroyed.
     */
    public synchronized void onDestroy() {
        mDestroyed = true;
        mDispatcher.onDestroy();
        mRequestCoordinators.forEach((taskId, requestCoord) -> requestCoord.onFinish());
        mRequestCoordinators.clear();
    }

    /**
     * Add new UceRequestCoordinator and notify the RequestDispatcher to check whether the given
     * requests can be executed or not.
     */
    public synchronized void addRequestCoordinator(UceRequestCoordinator coordinator) {
        if (mDestroyed) return;
        mRequestCoordinators.put(coordinator.getCoordinatorId(), coordinator);
        mDispatcher.addRequest(coordinator.getCoordinatorId(),
                coordinator.getActivatedRequestTaskIds());
    }

    /**
     * Remove the RequestCoordinator from the RequestCoordinator collection.
     */
    public synchronized UceRequestCoordinator removeRequestCoordinator(Long coordinatorId) {
        return mRequestCoordinators.remove(coordinatorId);

    }

    /**
     * Retrieve the RequestCoordinator associated with the given coordinatorId.
     */
    public synchronized UceRequestCoordinator getRequestCoordinator(Long coordinatorId) {
        return mRequestCoordinators.get(coordinatorId);
    }

    public synchronized UceRequest getUceRequest(Long taskId) {
        for (UceRequestCoordinator coordinator : mRequestCoordinators.values()) {
            UceRequest request = coordinator.getUceRequest(taskId);
            if (request != null) {
                return request;
            }
        }
        return null;
    }

    // Notify that the task is finished.
    public synchronized void notifyRequestFinished(Long taskId) {
        mDispatcher.onRequestFinished(taskId);
    }
}
