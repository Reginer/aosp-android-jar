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

package com.android.ims.rcs.uce.presence.publish;

import android.util.Log;

import com.android.ims.rcs.uce.presence.publish.PublishController.PublishTriggerType;
import com.android.ims.rcs.uce.util.UceUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The helper class to manage the publish request parameters.
 */
public class PublishProcessorState {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "PublishProcessorState";

    /*
     * Manager the pending request flag and the trigger type of this pending request.
     */
    private static class PendingRequest {
        private boolean mPendingFlag;
        private Optional<Integer> mTriggerType;
        private final Object mLock = new Object();

        public PendingRequest() {
            mTriggerType = Optional.empty();
        }

        // Set the flag to indicate there is a pending request.
        public void setPendingRequest(@PublishTriggerType int triggerType) {
            synchronized (mLock) {
                mPendingFlag = true;
                mTriggerType = Optional.of(triggerType);
            }
        }

        // Clear the flag. The publish request is triggered and this flag can be cleared.
        public void clearPendingRequest() {
            synchronized (mLock) {
                mPendingFlag = false;
                mTriggerType = Optional.empty();
            }
        }

        // Check if there is pending request need to be executed.
        public boolean hasPendingRequest() {
            synchronized (mLock) {
                return mPendingFlag;
            }
        }

        // Get the trigger type of the pending request.
        public Optional<Integer> getPendingRequestTriggerType() {
            synchronized (mLock) {
                return mTriggerType;
            }
        }
    }

    /**
     * Manager when the PUBLISH request can be executed.
     */
    private static class PublishThrottle {
        // The unit time interval of the request retry.
        private static final int RETRY_BASE_PERIOD_MIN = 1;

        // The maximum number of the publication retries.
        private static final int PUBLISH_MAXIMUM_NUM_RETRIES = 3;

        // Get the minimum time that allows two PUBLISH requests can be executed continuously.
        // It is one of the calculation conditions for the next publish allowed time.
        private long mRcsPublishThrottle;

        // The number of times the PUBLISH failed to retry. It is one of the calculation conditions
        // for the next publish allowed time.
        private int mRetryCount;

        // The subscription ID associated with this throttle helper.
        private int mSubId;

        // The time when the last PUBLISH request is success. It is one of the calculation
        // conditions for the next publish allowed time.
        private Optional<Instant> mLastPublishedTime;

        // The time to allow to execute the publishing request.
        private Optional<Instant> mPublishAllowedTime;

        public PublishThrottle(int subId) {
            mSubId = subId;
            resetState();
        }

        // Set the time of the last successful PUBLISH request.
        public void setLastPublishedTime(Instant lastPublishedTime) {
            mLastPublishedTime = Optional.of(lastPublishedTime);
        }

        // Increase the retry count when the PUBLISH has failed and need to be retried.
        public void increaseRetryCount() {
            if (mRetryCount < PUBLISH_MAXIMUM_NUM_RETRIES) {
                mRetryCount++;
            }
            // Adjust the publish allowed time.
            calcLatestPublishAllowedTime();
        }

        // Reset the retry count when the PUBLISH request is success or it does not need to retry.
        public void resetRetryCount() {
            mRetryCount = 0;
            // Adjust the publish allowed time.
            calcLatestPublishAllowedTime();
        }

        // In the case that the ImsService is disconnected, reset state for when the service
        // reconnects
        public void resetState() {
            mLastPublishedTime = Optional.empty();
            mPublishAllowedTime = Optional.empty();
            mRcsPublishThrottle = UceUtils.getRcsPublishThrottle(mSubId);
            Log.d(LOG_TAG, "RcsPublishThrottle=" + mRcsPublishThrottle);
        }

        // Check if it has reached the maximum retries.
        public boolean isReachMaximumRetries() {
            return (mRetryCount >= PUBLISH_MAXIMUM_NUM_RETRIES) ? true : false;
        }

        // Update the RCS publish throttle
        public void updatePublishThrottle(int publishThrottle) {
            mRcsPublishThrottle = publishThrottle;
            calcLatestPublishAllowedTime();
        }

        // Check if the PUBLISH request can be executed now.
        public boolean isPublishAllowedAtThisTime() {
            // If the allowed time has not been set, it means that it is not ready to PUBLISH.
            // It means that it has not received the publish request from the service.
            if (!mPublishAllowedTime.isPresent()) {
                return false;
            }

            // Check whether the current time has exceeded the allowed PUBLISH.
            return (Instant.now().isBefore(mPublishAllowedTime.get())) ? false : true;
        }

        // Update the PUBLISH allowed time with the given trigger type.
        public void updatePublishingAllowedTime(@PublishTriggerType int triggerType) {
            if (triggerType == PublishController.PUBLISH_TRIGGER_SERVICE) {
                // If the request is triggered by service, reset the retry count and allow to
                // execute the PUBLISH immediately.
                mRetryCount = 0;
                mPublishAllowedTime = Optional.of(Instant.now());
            } else if (triggerType != PublishController.PUBLISH_TRIGGER_RETRY) {
                // If the trigger type is not RETRY, it means that the device capabilities have
                // changed, reset the retry cout.
                resetRetryCount();
            }
        }

        // Get the delay time to allow to execute the PUBLISH request.
        public Optional<Long> getPublishingDelayTime() {
            // If the allowed time has not been set, it means that it is not ready to PUBLISH.
            // It means that it has not received the publish request from the service.
            if (!mPublishAllowedTime.isPresent()) {
                return Optional.empty();
            }

            // Setup the delay to the time which publish request is allowed to be executed.
            long delayTime = ChronoUnit.MILLIS.between(Instant.now(), mPublishAllowedTime.get());
            if (delayTime < 0) {
                delayTime = 0L;
            }
            return Optional.of(delayTime);
        }

        // Calculate the latest time allowed to PUBLISH
        private void calcLatestPublishAllowedTime() {
            final long retryDelay = getNextRetryDelayTime();
            if (!mLastPublishedTime.isPresent()) {
                // If the publish request has not been successful before, it does not need to
                // consider the PUBLISH throttle. The publish allowed time is decided by the retry
                // delay.
                mPublishAllowedTime = Optional.of(
                        Instant.now().plus(Duration.ofMillis(retryDelay)));
                Log.d(LOG_TAG, "calcLatestPublishAllowedTime: The last published time is empty");
            } else {
                // The default allowed time is the last published successful time plus the
                // PUBLISH throttle.
                Instant lastPublishedTime = mLastPublishedTime.get();
                Instant defaultAllowedTime = lastPublishedTime.plus(
                        Duration.ofMillis(mRcsPublishThrottle));

                if (retryDelay == 0) {
                    // If there is no delay time, the default allowed time is used.
                    mPublishAllowedTime = Optional.of(defaultAllowedTime);
                } else {
                    // When the retry count is updated and there is delay time, it needs to compare
                    // the default time and the retry delay time. The later time will be the
                    // final decision value.
                    Instant retryDelayTime = Instant.now().plus(Duration.ofMillis(retryDelay));
                    mPublishAllowedTime = Optional.of(
                            (retryDelayTime.isAfter(defaultAllowedTime))
                                    ? retryDelayTime : defaultAllowedTime);
                }
            }
            Log.d(LOG_TAG, "calcLatestPublishAllowedTime: " + mPublishAllowedTime.get());
        }

        // Get the milliseconds of the next retry delay.
        private long getNextRetryDelayTime() {
            // If the current retry count is zero, the delay time is also zero.
            if (mRetryCount == 0) return 0L;
            // Next retry delay time (minute)
            int power = mRetryCount - 1;
            Double delayTime = RETRY_BASE_PERIOD_MIN * Math.pow(2, power);
            // Convert to millis
            return TimeUnit.MINUTES.toMillis(delayTime.longValue());
        }
    }


    private long mTaskId;

    // Used to check whether the publish request is running now.
    private volatile boolean mIsPublishing;

    // Control the pending request flag.
    private final PendingRequest mPendingRequest;

    // Control the publish throttle
    private final PublishThrottle mPublishThrottle;

    private final Object mLock = new Object();

    public PublishProcessorState(int subId) {
        mPendingRequest = new PendingRequest();
        mPublishThrottle = new PublishThrottle(subId);
    }

    /**
     * @return A unique task Id for this request.
     */
    public long generatePublishTaskId() {
        synchronized (mLock) {
            mTaskId = UceUtils.generateTaskId();
            return mTaskId;
        }
    }

    /**
     * @return The current valid PUBLISH task ID.
     */
    public long getCurrentTaskId() {
        synchronized (mLock) {
            return mTaskId;
        }
    }

    /**
     * Set the publishing flag to indicate whether it is executing a PUBLISH request or not.
     */
    public void setPublishingFlag(boolean flag) {
        mIsPublishing = flag;
    }

    /**
     * @return true if it is executing a PUBLISH request now.
     */
    public boolean isPublishingNow() {
        return mIsPublishing;
    }

    /**
     * Set the flag to indicate there is a pending request waiting to be executed.
     */
    public void setPendingRequest(@PublishTriggerType int triggerType) {
        mPendingRequest.setPendingRequest(triggerType);
    }

    /**
     * Clear the flag. It means a new publish request is triggered and the pending request flag
     * can be cleared.
     */
    public void clearPendingRequest() {
        mPendingRequest.clearPendingRequest();
    }

    /**
     * @return true if there is pending request to be executed.
     */
    public boolean hasPendingRequest() {
        return mPendingRequest.hasPendingRequest();
    }

    /**
     * @return The trigger type of the pending request. If there is no pending request, it will
     * return Optional.empty
     */
    public Optional<Integer> getPendingRequestTriggerType() {
        return mPendingRequest.getPendingRequestTriggerType();
    }

    /**
     * Set the time of the last successful PUBLISH request.
     * @param lastPublishedTime The time when the last PUBLISH request is success
     */
    public void setLastPublishedTime(Instant lastPublishedTime) {
        synchronized (mLock) {
            mPublishThrottle.setLastPublishedTime(lastPublishedTime);
        }
    }

    /**
     * Increase the retry count when the PUBLISH has failed and need to retry.
     */
    public void increaseRetryCount() {
        synchronized (mLock) {
            mPublishThrottle.increaseRetryCount();
        }
    }

    /**
     * Reset the retry count when the PUBLISH request is success or it does not need to retry.
     */
    public void resetRetryCount() {
        synchronized (mLock) {
            mPublishThrottle.resetRetryCount();
        }
    }

    /**
     * Reset the retry count and related time when the publication status has
     * changed to not_published.
     */
    public void resetState() {
        synchronized (mLock) {
            mPublishThrottle.resetState();
        }
    }

    /*
     * Check if it has reached the maximum retry count.
     */
    public boolean isReachMaximumRetries() {
        synchronized (mLock) {
            return mPublishThrottle.isReachMaximumRetries();
        }
    }

    /*
     * Check if the PUBLISH can be executed now.
     */
    public boolean isPublishAllowedAtThisTime() {
        synchronized (mLock) {
            return mPublishThrottle.isPublishAllowedAtThisTime();
        }
    }

    /**
     * Update the PUBLISH allowed time with the given trigger type.
     * @param triggerType The trigger type of this PUBLISH request
     */
    public void updatePublishingAllowedTime(@PublishTriggerType int triggerType) {
        synchronized (mLock) {
            mPublishThrottle.updatePublishingAllowedTime(triggerType);
        }
    }

    // Get the delay time to allow to execute the PUBLISH request.
    public Optional<Long> getPublishingDelayTime() {
        synchronized (mLock) {
            return mPublishThrottle.getPublishingDelayTime();
        }
    }

    public void updatePublishThrottle(int publishThrottle) {
        synchronized (mLock) {
            mPublishThrottle.updatePublishThrottle(publishThrottle);
        }
    }

    public void onRcsDisconnected() {
        synchronized (mLock) {
            setPublishingFlag(false /*isPublishing*/);
            clearPendingRequest();
            mPublishThrottle.resetState();
        }
    }
}
