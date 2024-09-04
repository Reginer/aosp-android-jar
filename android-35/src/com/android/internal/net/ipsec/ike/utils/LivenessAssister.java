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
package com.android.internal.net.ipsec.ike.utils;

import static android.net.ipsec.ike.IkeManager.getIkeLog;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.net.ipsec.ike.IkeSessionCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * The LivenessAssister helps the process from requesting liveness check to delivering a result, and
 * helps to handle status callbacks according to the results.
 *
 * <p>The liveness check has a simple process of receiving a request from a client, checking a
 * peer's liveness, and then reporting the results. Results can only be reported upon request, and
 * the LivenessAssister also provides callbacks for the results.
 *
 * <p>A process of state change as follows. Failure will be reported before closing the session.
 *
 * <ul>
 *   <li>Requested -> report (on-demand or background) Started -> report Success -> Clear Requested
 *   <li>Requested -> report (on-demand or background) Started -> report failure -> no longer used
 *   <li>Requested -> report (on-demand) Started -> if requested again -> report (on-demand) ongoing
 *       -> report Success -> Clear Requested
 *   <li>Requested -> report (on-demand) Started -> if requested again -> report (on-demand) ongoing
 *       -> report failure -> no longer used
 *   <li>Requested -> report (background) Started -> if requested again -> report (background)
 *       ongoing -> report Success -> NotRequested
 *   <li>Requested -> report (background) Started -> if requested again -> report (background)
 *       ongoing -> report failure -> no longer used
 * </ul>
 */
public class LivenessAssister {

    private static final String TAG = LivenessAssister.class.getSimpleName();

    /** Initial. */
    public static final int REQ_TYPE_INITIAL = 0;

    /** A liveness check request is performed as an on-demand task. */
    public static final int REQ_TYPE_ON_DEMAND = 1;

    /** A liveness check request is performed in the background. */
    public static final int REQ_TYPE_BACKGROUND = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        REQ_TYPE_INITIAL,
        REQ_TYPE_ON_DEMAND,
        REQ_TYPE_BACKGROUND,
    })
    @interface LivenessRequestType {}

    private final IkeSessionCallback mCallback;
    private final Executor mUserCbExecutor;

    private int mLivenessCheckRequested;

    private LivenessMetricHelper mLivenessMetricHelper;

    public LivenessAssister(
            @NonNull IkeSessionCallback callback,
            @NonNull Executor executor,
            @NonNull IIkeMetricsCallback metricsCallback) {
        mCallback = callback;
        mUserCbExecutor = executor;
        mLivenessCheckRequested = REQ_TYPE_INITIAL;
        mLivenessMetricHelper = new LivenessMetricHelper(metricsCallback);
    }

    /**
     * Set the status as requested for the liveness check and notify the associated liveness status.
     *
     * <p>{@link IkeSessionCallback.LivenessStatus#LIVENESS_STATUS_ON_DEMAND_STARTED}: This status
     * is notified when liveness checking is started with a new on-demand DPD task.
     *
     * <p>{@link IkeSessionCallback.LivenessStatus#LIVENESS_STATUS_ON_DEMAND_ONGOING}: This status
     * is notified when liveness checking is already running in an on-demand DPD task.
     *
     * <p>{@link IkeSessionCallback.LivenessStatus#LIVENESS_STATUS_BACKGROUND_STARTED}: This status
     * is notified when liveness checking is started with joining an existing running task.
     *
     * <p>{@link IkeSessionCallback.LivenessStatus#LIVENESS_STATUS_BACKGROUND_ONGOING}: This status
     * is notified when liveness checking is already running with joining an existing running task.
     *
     * @param requestType request type of {@link LivenessRequestType#REQ_TYPE_ON_DEMAND} or {@link
     *     LivenessRequestType#REQ_TYPE_BACKGROUND}
     */
    public void livenessCheckRequested(@LivenessRequestType int requestType) {
        switch (mLivenessCheckRequested) {
            case REQ_TYPE_INITIAL:
                if (requestType == REQ_TYPE_ON_DEMAND) {
                    mLivenessCheckRequested = REQ_TYPE_ON_DEMAND;
                    invokeUserCallback(IkeSessionCallback.LIVENESS_STATUS_ON_DEMAND_STARTED);
                } else if (requestType == REQ_TYPE_BACKGROUND) {
                    mLivenessCheckRequested = REQ_TYPE_BACKGROUND;
                    invokeUserCallback(IkeSessionCallback.LIVENESS_STATUS_BACKGROUND_STARTED);
                }
                break;
            case REQ_TYPE_ON_DEMAND:
                invokeUserCallback(IkeSessionCallback.LIVENESS_STATUS_ON_DEMAND_ONGOING);
                break;
            case REQ_TYPE_BACKGROUND:
                invokeUserCallback(IkeSessionCallback.LIVENESS_STATUS_BACKGROUND_ONGOING);
                break;
        }
    }

    /**
     * Mark that the liveness check was successful.
     *
     * <p>and notifies a {@link IkeSessionCallback#LIVENESS_STATUS_SUCCESS}.
     */
    public void markPeerAsAlive() {
        if (mLivenessCheckRequested == REQ_TYPE_INITIAL) {
            return;
        }

        mLivenessCheckRequested = REQ_TYPE_INITIAL;
        invokeUserCallback(IkeSessionCallback.LIVENESS_STATUS_SUCCESS);
    }

    /**
     * Mark that the liveness check was failed.
     *
     * <p>and notifies a {@link IkeSessionCallback#LIVENESS_STATUS_FAILURE}.
     */
    public void markPeerAsDead() {
        if (mLivenessCheckRequested == REQ_TYPE_INITIAL) {
            return;
        }

        mLivenessCheckRequested = REQ_TYPE_INITIAL;
        invokeUserCallback(IkeSessionCallback.LIVENESS_STATUS_FAILURE);
    }

    /**
     * Returns whether the request has been received and the results have not yet been reported.
     *
     * @return {@code true} if the liveness check has been requested.
     */
    public boolean isLivenessCheckRequested() {
        return mLivenessCheckRequested != REQ_TYPE_INITIAL;
    }

    /**
     * Callbacks liveness status to clients.
     *
     * @param status {@link IkeSessionCallback.LivenessStatus} to be notified.
     */
    private void invokeUserCallback(@IkeSessionCallback.LivenessStatus int status) {
        try {
            mUserCbExecutor.execute(() -> mCallback.onLivenessStatusChanged(status));
            mLivenessMetricHelper.recordLivenessStatus(status);
        } catch (Exception e) {
            getIkeLog().e(TAG, "onLivenessStatusChanged execution failed", e);
        }
    }

    /** Interface for receiving values that make up atoms */
    public interface IIkeMetricsCallback {
        /** Notifies that the liveness check has been completed. */
        void onLivenessCheckCompleted(
                int elapsedTimeInMillis, int numberOfOnGoing, boolean resultSuccess);
    }

    private static class LivenessMetricHelper {

        /** To log metric information, call the function when ready to send it. */
        private final IIkeMetricsCallback mMetricsCallback;

        private long mTimeInMillisStartedStatus;
        private int mNumberOfOnGoing;

        LivenessMetricHelper(IIkeMetricsCallback metricsCallback) {
            clearVariables();
            mMetricsCallback = metricsCallback;
        }

        private void clearVariables() {
            mTimeInMillisStartedStatus = 0L;
            mNumberOfOnGoing = 0;
        }

        public void recordLivenessStatus(@IkeSessionCallback.LivenessStatus int status) {
            switch (status) {
                case IkeSessionCallback.LIVENESS_STATUS_ON_DEMAND_STARTED: // fallthrough
                case IkeSessionCallback.LIVENESS_STATUS_BACKGROUND_STARTED:
                    clearVariables();
                    mTimeInMillisStartedStatus = System.currentTimeMillis();
                    break;
                case IkeSessionCallback.LIVENESS_STATUS_ON_DEMAND_ONGOING: // fallthrough
                case IkeSessionCallback.LIVENESS_STATUS_BACKGROUND_ONGOING:
                    mNumberOfOnGoing++;
                    break;
                case IkeSessionCallback.LIVENESS_STATUS_SUCCESS:
                    onLivenessCheckCompleted(true);
                    break;
                case IkeSessionCallback.LIVENESS_STATUS_FAILURE:
                    onLivenessCheckCompleted(false);
                    break;
            }
        }

        private void onLivenessCheckCompleted(boolean resultSuccess) {
            long elapsedTimeInMillis = System.currentTimeMillis() - mTimeInMillisStartedStatus;
            if (elapsedTimeInMillis < 0L || elapsedTimeInMillis > Integer.MAX_VALUE) {
                getIkeLog()
                        .e(
                                TAG,
                                "onLivenessCheckCompleted, time exceeded failed. timeInMillies:"
                                        + elapsedTimeInMillis);
                clearVariables();
                return;
            }

            mMetricsCallback.onLivenessCheckCompleted(
                    (int) elapsedTimeInMillis, mNumberOfOnGoing, resultSuccess);
            clearVariables();
        }
    }
}
