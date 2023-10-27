/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.internal.net.ipsec.ike;

import static android.net.ipsec.ike.IkeManager.getIkeLog;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

import static com.android.internal.net.ipsec.ike.AbstractSessionStateMachine.CMD_LOCAL_REQUEST_CREATE_CHILD;
import static com.android.internal.net.ipsec.ike.AbstractSessionStateMachine.CMD_LOCAL_REQUEST_DELETE_CHILD;
import static com.android.internal.net.ipsec.ike.AbstractSessionStateMachine.CMD_LOCAL_REQUEST_MAX;
import static com.android.internal.net.ipsec.ike.AbstractSessionStateMachine.CMD_LOCAL_REQUEST_MIGRATE_CHILD;
import static com.android.internal.net.ipsec.ike.AbstractSessionStateMachine.CMD_LOCAL_REQUEST_MIN;
import static com.android.internal.net.ipsec.ike.AbstractSessionStateMachine.CMD_LOCAL_REQUEST_REKEY_CHILD;
import static com.android.internal.net.ipsec.ike.AbstractSessionStateMachine.CMD_LOCAL_REQUEST_REKEY_CHILD_MOBIKE;
import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.CMD_LOCAL_REQUEST_CREATE_IKE;
import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.CMD_LOCAL_REQUEST_DELETE_IKE;
import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.CMD_LOCAL_REQUEST_DPD;
import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.CMD_LOCAL_REQUEST_INFO;
import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.CMD_LOCAL_REQUEST_MOBIKE;
import static com.android.internal.net.ipsec.ike.IkeSessionStateMachine.CMD_LOCAL_REQUEST_REKEY_IKE;

import android.annotation.IntDef;
import android.content.Context;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.ChildSessionParams;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * IkeLocalRequestScheduler caches all local requests scheduled by an IKE Session and notify the IKE
 * Session to process the request when it is allowed.
 *
 * <p>LocalRequestScheduler is running on the IkeSessionStateMachine thread.
 */
public final class IkeLocalRequestScheduler {
    private static final String TAG = "IkeLocalRequestScheduler";

    @VisibleForTesting static final String LOCAL_REQUEST_WAKE_LOCK_TAG = "LocalRequestWakeLock";

    private static final int DEFAULT_REQUEST_QUEUE_SIZE = 1;

    private static final int REQUEST_ID_NOT_ASSIGNED = -1;

    // Local request that must be handled immediately. Ex: CMD_LOCAL_REQUEST_DELETE_IKE
    @VisibleForTesting static final int REQUEST_PRIORITY_URGENT = 0;

    // Local request that must be handled soon, but not necessarily immediately.
    // Ex: CMD_LOCAL_REQUEST_MOBIKE
    @VisibleForTesting static final int REQUEST_PRIORITY_HIGH = 1;

    // Local request that should be handled once nothing more urgent requires handling. Most
    // LocalRequests will have this priority.
    @VisibleForTesting static final int REQUEST_PRIORITY_NORMAL = 2;

    // Local request that has an unknown priority. This shouldn't happen in normal processing.
    @VisibleForTesting static final int REQUEST_PRIORITY_UNKNOWN = Integer.MAX_VALUE;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        REQUEST_PRIORITY_URGENT,
        REQUEST_PRIORITY_HIGH,
        REQUEST_PRIORITY_NORMAL,
        REQUEST_PRIORITY_UNKNOWN
    })
    @interface RequestPriority {}

    public static int SPI_NOT_INCLUDED = 0;

    private final PowerManager mPowerManager;

    private final PriorityQueue<LocalRequest> mRequestQueue =
            new PriorityQueue<>(DEFAULT_REQUEST_QUEUE_SIZE, new LocalRequestComparator());

    private final IProcedureConsumer mConsumer;

    private int mNextRequestId;

    /**
     * Construct an instance of IkeLocalRequestScheduler
     *
     * @param consumer the interface to initiate new procedure.
     */
    public IkeLocalRequestScheduler(IProcedureConsumer consumer, Context context) {
        mConsumer = consumer;
        mPowerManager = context.getSystemService(PowerManager.class);

        mNextRequestId = 0;
    }

    /** Add a new local request to the queue. */
    public void addRequest(LocalRequest request) {
        request.acquireWakeLock(mPowerManager);
        request.setRequestId(mNextRequestId++);
        mRequestQueue.offer(request);
    }

    /**
     * Notifies the scheduler that the caller is ready for a new procedure
     *
     * <p>Synchronously triggers the call to onNewProcedureReady.
     *
     * @return whether or not a new procedure was scheduled.
     */
    public boolean readyForNextProcedure() {
        if (!mRequestQueue.isEmpty()) {
            mConsumer.onNewProcedureReady(mRequestQueue.poll());
            return true;
        }
        return false;
    }

    /** Release WakeLocks of all LocalRequests in the queue */
    public void releaseAllLocalRequestWakeLocks() {
        for (LocalRequest req : mRequestQueue) {
            req.releaseWakeLock();
        }
        mRequestQueue.clear();
    }

    /**
     * This class represents the common information of procedures that will be locally initiated.
     */
    public abstract static class LocalRequest {
        public final int procedureType;

        // Priority of this LocalRequest. Note that a lower 'priority' means higher urgency.
        @RequestPriority private final int mPriority;

        // ID used to preserve insertion-order between requests in IkeLocalRequestScheduler with the
        // same priority. Set when the LocalRequest is added to the IkeLocalRequestScheduler.
        private int mRequestId = REQUEST_ID_NOT_ASSIGNED;
        private WakeLock mWakeLock;

        LocalRequest(int type, int priority) {
            validateTypeOrThrow(type);
            procedureType = type;
            mPriority = priority;
        }

        @VisibleForTesting
        int getPriority() {
            return mPriority;
        }

        private void setRequestId(int requestId) {
            mRequestId = requestId;
        }

        @VisibleForTesting
        int getRequestId() {
            return mRequestId;
        }

        /**
         * Acquire a WakeLock for the LocalRequest.
         *
         * <p>This method will only be called from IkeLocalRequestScheduler#addRequest or
         * IkeLocalRequestScheduler#addRequestAtFront
         */
        private void acquireWakeLock(PowerManager powerManager) {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                getIkeLog().wtf(TAG, "This LocalRequest already acquired a WakeLock");
                return;
            }

            mWakeLock =
                    powerManager.newWakeLock(
                            PARTIAL_WAKE_LOCK,
                            TAG + LOCAL_REQUEST_WAKE_LOCK_TAG + "_" + procedureType);
            mWakeLock.setReferenceCounted(false);
            mWakeLock.acquire();
        }

        /** Release WakeLock of the LocalRequest */
        public void releaseWakeLock() {
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
        }

        protected abstract void validateTypeOrThrow(int type);

        protected abstract boolean isChildRequest();
    }

    /** LocalRequestComparator is a comparator for comparing LocalRequest instances. */
    private class LocalRequestComparator implements Comparator<LocalRequest> {
        @Override
        public int compare(LocalRequest requestA, LocalRequest requestB) {
            int relativePriorities =
                    Integer.compare(requestA.getPriority(), requestB.getPriority());
            if (relativePriorities != 0) return relativePriorities;

            return Integer.compare(requestA.getRequestId(), requestB.getRequestId());
        }
    }

    /**
     * This class represents a user requested or internally scheduled IKE procedure that will be
     * initiated locally.
     */
    public static class IkeLocalRequest extends LocalRequest {
        public long remoteSpi;

        /** Schedule a request for an IKE SA that is identified by the remoteIkeSpi */
        private IkeLocalRequest(int type, long remoteIkeSpi, int priority) {
            super(type, priority);
            remoteSpi = remoteIkeSpi;
        }

        @Override
        protected void validateTypeOrThrow(int type) {
            if (type >= CMD_LOCAL_REQUEST_CREATE_IKE && type <= CMD_LOCAL_REQUEST_MOBIKE) return;
            throw new IllegalArgumentException("Invalid IKE procedure type: " + type);
        }

        @Override
        protected boolean isChildRequest() {
            return false;
        }
    }

    /**
     * This class represents a user requested or internally scheduled Child procedure that will be
     * initiated locally.
     */
    public static class ChildLocalRequest extends LocalRequest {
        public int remoteSpi;
        public final ChildSessionCallback childSessionCallback;
        public final ChildSessionParams childSessionParams;

        private ChildLocalRequest(
                int type,
                int remoteChildSpi,
                ChildSessionCallback childCallback,
                ChildSessionParams childParams,
                int priority) {
            super(type, priority);
            childSessionParams = childParams;
            childSessionCallback = childCallback;
            remoteSpi = remoteChildSpi;
        }

        @Override
        protected void validateTypeOrThrow(int type) {
            if (type >= CMD_LOCAL_REQUEST_MIN && type <= CMD_LOCAL_REQUEST_MAX) {
                return;
            }

            throw new IllegalArgumentException("Invalid Child procedure type: " + type);
        }

        @Override
        protected boolean isChildRequest() {
            return true;
        }
    }

    /** Interface to initiate a new IKE procedure */
    public interface IProcedureConsumer {
        /**
         * Called when a new IKE procedure can be initiated.
         *
         * @param localRequest the request to be initiated.
         */
        void onNewProcedureReady(LocalRequest localRequest);
    }

    /** package-protected */
    static class LocalRequestFactory {
        /** Create a request for the IKE Session */
        IkeLocalRequest getIkeLocalRequest(int type) {
            return getIkeLocalRequest(type, SPI_NOT_INCLUDED);
        }

        /** Create a request for an IKE SA that is identified by the remoteIkeSpi */
        IkeLocalRequest getIkeLocalRequest(int type, long remoteIkeSpi) {
            return new IkeLocalRequest(type, remoteIkeSpi, procedureTypeToPriority(type));
        }

        /** Create a request for a Child Session that is identified by the childCallback */
        ChildLocalRequest getChildLocalRequest(
                int type, ChildSessionCallback childCallback, ChildSessionParams childParams) {
            return new ChildLocalRequest(
                    type,
                    SPI_NOT_INCLUDED,
                    childCallback,
                    childParams,
                    procedureTypeToPriority(type));
        }

        /** Create a request for a Child SA that is identified by the remoteChildSpi */
        ChildLocalRequest getChildLocalRequest(int type, int remoteChildSpi) {
            return new ChildLocalRequest(
                    type,
                    remoteChildSpi,
                    null /*childCallback*/,
                    null /*childParams*/,
                    procedureTypeToPriority(type));
        }

        /** Returns the request priority for the specified procedure type. */
        @VisibleForTesting
        @RequestPriority
        static int procedureTypeToPriority(int procedureType) {
            switch (procedureType) {
                case CMD_LOCAL_REQUEST_DELETE_IKE:
                    return REQUEST_PRIORITY_URGENT;

                case CMD_LOCAL_REQUEST_MOBIKE:
                case CMD_LOCAL_REQUEST_REKEY_CHILD_MOBIKE:
                case CMD_LOCAL_REQUEST_MIGRATE_CHILD:
                    return REQUEST_PRIORITY_HIGH;

                case CMD_LOCAL_REQUEST_CREATE_IKE: // Fallthrough
                case CMD_LOCAL_REQUEST_REKEY_IKE: // Fallthrough
                case CMD_LOCAL_REQUEST_INFO: // Fallthrough
                case CMD_LOCAL_REQUEST_DPD: // Fallthrough
                case CMD_LOCAL_REQUEST_CREATE_CHILD: // Fallthrough
                case CMD_LOCAL_REQUEST_DELETE_CHILD: // Fallthrough
                case CMD_LOCAL_REQUEST_REKEY_CHILD:
                    return REQUEST_PRIORITY_NORMAL;

                default:
                    // unknown procedure type - assign it the lowest priority
                    getIkeLog().wtf(TAG, "Unknown procedureType: " + procedureType);
                    return REQUEST_PRIORITY_UNKNOWN;
            }
        }
    }
}
