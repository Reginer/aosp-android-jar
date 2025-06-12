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

package com.android.ims.rcs.uce;

import android.annotation.IntDef;
import android.content.Context;
import android.telephony.ims.RcsUceAdapter.ErrorCode;
import android.util.Log;

import com.android.ims.rcs.uce.UceController.RequestType;
import com.android.ims.rcs.uce.UceController.UceControllerCallback;
import com.android.ims.rcs.uce.util.NetworkSipCode;
import com.android.ims.rcs.uce.util.UceUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manager the device state to determine whether the device is allowed to execute UCE requests or
 * not.
 */
public class UceDeviceState {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "UceDeviceState";

    /**
     * The device is allowed to execute UCE requests.
     */
    private static final int DEVICE_STATE_OK = 0;

    /**
     * The device will be in the forbidden state when the network response SIP code is 403
     */
    private static final int DEVICE_STATE_FORBIDDEN = 1;

    /**
     * The device will be in the PROVISION error state when the PUBLISH request fails and the
     * SIP code is 404 NOT FOUND.
     */
    private static final int DEVICE_STATE_PROVISION_ERROR = 2;

    /**
     * When the network response SIP code is 489 and the carrier config also indicates that needs
     * to handle the SIP code 489, the device will be in the BAD EVENT state.
     */
    private static final int DEVICE_STATE_BAD_EVENT = 3;

    /**
     * The device will be in the NO_RETRY error state when the PUBLISH request fails and the
     * SIP code is 413 REQUEST ENTITY TOO LARGE.
     */
    private static final int DEVICE_STATE_NO_RETRY = 4;

    @IntDef(value = {
            DEVICE_STATE_OK,
            DEVICE_STATE_FORBIDDEN,
            DEVICE_STATE_PROVISION_ERROR,
            DEVICE_STATE_BAD_EVENT,
            DEVICE_STATE_NO_RETRY,
    }, prefix="DEVICE_STATE_")
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeviceStateType {}

    private static final Map<Integer, String> DEVICE_STATE_DESCRIPTION = new HashMap<>();
    static {
        DEVICE_STATE_DESCRIPTION.put(DEVICE_STATE_OK, "DEVICE_STATE_OK");
        DEVICE_STATE_DESCRIPTION.put(DEVICE_STATE_FORBIDDEN, "DEVICE_STATE_FORBIDDEN");
        DEVICE_STATE_DESCRIPTION.put(DEVICE_STATE_PROVISION_ERROR, "DEVICE_STATE_PROVISION_ERROR");
        DEVICE_STATE_DESCRIPTION.put(DEVICE_STATE_BAD_EVENT, "DEVICE_STATE_BAD_EVENT");
        DEVICE_STATE_DESCRIPTION.put(DEVICE_STATE_NO_RETRY, "DEVICE_STATE_NO_RETRY");
    }

    /**
     * The result of the current device state.
     */
    public static class DeviceStateResult {
        final @DeviceStateType int mDeviceState;
        final @ErrorCode Optional<Integer> mErrorCode;
        final Optional<Instant> mRequestRetryTime;
        final Optional<Instant> mExitStateTime;

        public DeviceStateResult(int deviceState, Optional<Integer> errorCode,
                Optional<Instant> requestRetryTime, Optional<Instant> exitStateTime) {
            mDeviceState = deviceState;
            mErrorCode = errorCode;
            mRequestRetryTime = requestRetryTime;
            mExitStateTime = exitStateTime;
        }

        /**
         * Check current state to see if the UCE request is allowed to be executed.
         */
        public boolean isRequestForbidden() {
            switch(mDeviceState) {
                case DEVICE_STATE_FORBIDDEN:
                case DEVICE_STATE_PROVISION_ERROR:
                case DEVICE_STATE_BAD_EVENT:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Check current state to see if only the PUBLISH request is allowed to be executed.
         */
        public boolean isPublishRequestBlocked() {
            switch(mDeviceState) {
                case DEVICE_STATE_NO_RETRY:
                    return true;
                default:
                    return false;
            }
        }

        public int getDeviceState() {
            return mDeviceState;
        }

        public Optional<Integer> getErrorCode() {
            return mErrorCode;
        }

        public Optional<Instant> getRequestRetryTime() {
            return mRequestRetryTime;
        }

        public long getRequestRetryAfterMillis() {
            if (!mRequestRetryTime.isPresent()) {
                return 0L;
            }
            long retryAfter = ChronoUnit.MILLIS.between(Instant.now(), mRequestRetryTime.get());
            return (retryAfter < 0L) ? 0L : retryAfter;
        }

        public Optional<Instant> getExitStateTime() {
            return mExitStateTime;
        }

        /**
         * Check if the given DeviceStateResult is equal to current DeviceStateResult instance.
         */
        public boolean isDeviceStateEqual(DeviceStateResult otherDeviceState) {
            if ((mDeviceState == otherDeviceState.getDeviceState()) &&
                    mErrorCode.equals(otherDeviceState.getErrorCode()) &&
                    mRequestRetryTime.equals(otherDeviceState.getRequestRetryTime()) &&
                    mExitStateTime.equals(otherDeviceState.getExitStateTime())) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("DeviceState=").append(DEVICE_STATE_DESCRIPTION.get(getDeviceState()))
                    .append(", ErrorCode=").append(getErrorCode())
                    .append(", RetryTime=").append(getRequestRetryTime())
                    .append(", retryAfterMillis=").append(getRequestRetryAfterMillis())
                    .append(", ExitStateTime=").append(getExitStateTime());
            return builder.toString();
        }
    }

    private final int mSubId;
    private final Context mContext;
    private final UceControllerCallback mUceCtrlCallback;

    private @DeviceStateType int mDeviceState;
    private @ErrorCode Optional<Integer> mErrorCode;
    private Optional<Instant> mRequestRetryTime;
    private Optional<Instant> mExitStateTime;

    public UceDeviceState(int subId, Context context, UceControllerCallback uceCtrlCallback) {
        mSubId = subId;
        mContext = context;
        mUceCtrlCallback = uceCtrlCallback;

        // Try to restore the device state from the shared preference.
        boolean restoreFromPref = false;
        Optional<DeviceStateResult> deviceState = UceUtils.restoreDeviceState(mContext, mSubId);
        if (deviceState.isPresent()) {
            restoreFromPref = true;
            mDeviceState = deviceState.get().getDeviceState();
            mErrorCode = deviceState.get().getErrorCode();
            mRequestRetryTime = deviceState.get().getRequestRetryTime();
            mExitStateTime = deviceState.get().getExitStateTime();
        } else {
            mDeviceState = DEVICE_STATE_OK;
            mErrorCode = Optional.empty();
            mRequestRetryTime = Optional.empty();
            mExitStateTime = Optional.empty();
        }
        logd("UceDeviceState: restore from sharedPref=" + restoreFromPref + ", " +
                getCurrentState());
    }

    /**
     * Check and setup the timer to exit the request disallowed state. This method is called when
     * the DeviceState has been initialized completed and need to restore the timer.
     */
    public synchronized void checkSendResetDeviceStateTimer() {
        logd("checkSendResetDeviceStateTimer: time=" + mExitStateTime);
        if (!mExitStateTime.isPresent()) {
            return;
        }
        long expirySec = ChronoUnit.SECONDS.between(Instant.now(), mExitStateTime.get());
        if (expirySec < 0) {
            expirySec = 0;
        }
        // Setup timer to exit the request disallowed state.
        mUceCtrlCallback.setupResetDeviceStateTimer(expirySec);
    }

    /**
     * @return The current device state.
     */
    public synchronized DeviceStateResult getCurrentState() {
        return new DeviceStateResult(mDeviceState, mErrorCode, mRequestRetryTime, mExitStateTime);
    }

    /**
     * Update the device state to determine whether the device is allowed to send requests or not.
     *  @param sipCode The SIP CODE of the request result.
     *  @param reason The reason from the network response.
     *  @param requestType The type of the request.
     */
    public synchronized void refreshDeviceState(int sipCode, String reason,
            @RequestType int requestType) {
        logd("refreshDeviceState: sipCode=" + sipCode + ", reason=" + reason +
                ", requestResponseType=" + UceController.REQUEST_TYPE_DESCRIPTION.get(requestType));

        // Get the current device status before updating the state.
        DeviceStateResult previousState = getCurrentState();

        // Update the device state based on the given sip code.
        switch (sipCode) {
            case NetworkSipCode.SIP_CODE_FORBIDDEN:   // sip 403
            case NetworkSipCode.SIP_CODE_SERVER_TIMEOUT: // sip 504
                if (requestType == UceController.REQUEST_TYPE_PUBLISH) {
                    // Provisioning error for publish request.
                    setDeviceState(DEVICE_STATE_PROVISION_ERROR);
                    updateErrorCode(sipCode, reason, requestType);
                    // There is no request retry time for SIP code 403
                    removeRequestRetryTime();
                    // No timer to exit the forbidden state.
                    removeExitStateTimer();
                }
                break;

            case NetworkSipCode.SIP_CODE_NOT_FOUND:  // sip 404
                // DeviceState only handles 404 NOT FOUND error for PUBLISH request.
                if (requestType == UceController.REQUEST_TYPE_PUBLISH) {
                    setDeviceState(DEVICE_STATE_PROVISION_ERROR);
                    updateErrorCode(sipCode, reason, requestType);
                    // There is no request retry time for SIP code 404
                    removeRequestRetryTime();
                    // No timer to exit this state.
                    removeExitStateTimer();
                }
                break;

            case NetworkSipCode.SIP_CODE_BAD_EVENT:   // sip 489
                if (UceUtils.isRequestForbiddenBySip489(mContext, mSubId)) {
                    setDeviceState(DEVICE_STATE_BAD_EVENT);
                    updateErrorCode(sipCode, reason, requestType);
                    // Setup the request retry time.
                    setupRequestRetryTime();
                    // Setup the timer to exit the BAD EVENT state.
                    setupExitStateTimer();
                }
                break;

            case NetworkSipCode.SIP_CODE_OK:
            case NetworkSipCode.SIP_CODE_ACCEPTED:
                // Reset the device state when the network response is OK.
                resetInternal();
                break;

            case NetworkSipCode.SIP_CODE_REQUEST_ENTITY_TOO_LARGE:   // sip 413
            case NetworkSipCode.SIP_CODE_TEMPORARILY_UNAVAILABLE:   // sip 480
            case NetworkSipCode.SIP_CODE_BUSY:   // sip 486
            case NetworkSipCode.SIP_CODE_SERVER_INTERNAL_ERROR:   // sip 500
            case NetworkSipCode.SIP_CODE_SERVICE_UNAVAILABLE:   // sip 503
            case NetworkSipCode.SIP_CODE_BUSY_EVERYWHERE:   // sip 600
            case NetworkSipCode.SIP_CODE_DECLINE:   // sip 603
                if (requestType == UceController.REQUEST_TYPE_PUBLISH) {
                    setDeviceState(DEVICE_STATE_NO_RETRY);
                    // There is no request retry time for SIP code 413
                    removeRequestRetryTime();
                    // No timer to exit this state.
                    removeExitStateTimer();
                }
                break;
        }

        // Get the updated device state.
        DeviceStateResult currentState = getCurrentState();

        // Remove the device state from the shared preference if the device is allowed to execute
        // UCE requests. Otherwise, save the new state into the shared preference when the device
        // state has changed.
        if (!currentState.isRequestForbidden()) {
            removeDeviceStateFromPreference();
        } else if (!currentState.isDeviceStateEqual(previousState)) {
            saveDeviceStateToPreference(currentState);
        }

        logd("refreshDeviceState: previous: " + previousState + ", current: " + currentState);
    }

    /**
     * Reset the device state. This method is called when the ImsService triggers to send the
     * PUBLISH request.
     */
    public synchronized void resetDeviceState() {
        DeviceStateResult previousState = getCurrentState();
        resetInternal();
        DeviceStateResult currentState = getCurrentState();

        // Remove the device state from shared preference because the device state has been reset.
        removeDeviceStateFromPreference();

        logd("resetDeviceState: previous=" + previousState + ", current=" + currentState);
    }

    /**
     * The internal method to reset the device state. This method doesn't
     */
    private void resetInternal() {
        setDeviceState(DEVICE_STATE_OK);
        resetErrorCode();
        removeRequestRetryTime();
        removeExitStateTimer();
    }

    private void setDeviceState(@DeviceStateType int latestState) {
        if (mDeviceState != latestState) {
            mDeviceState = latestState;
        }
    }

    private void updateErrorCode(int sipCode, String reason, @RequestType int requestType) {
        Optional<Integer> newErrorCode = Optional.of(NetworkSipCode.getCapabilityErrorFromSipCode(
                    sipCode, reason, requestType));
        if (!mErrorCode.equals(newErrorCode)) {
            mErrorCode = newErrorCode;
        }
    }

    private void resetErrorCode() {
        if (mErrorCode.isPresent()) {
            mErrorCode = Optional.empty();
        }
    }

    private void setupRequestRetryTime() {
        /*
         * Update the request retry time when A) it has not been assigned yet or B) it has past the
         * current time and need to be re-assigned a new retry time.
         */
        if (!mRequestRetryTime.isPresent() || mRequestRetryTime.get().isAfter(Instant.now())) {
            long retryInterval = UceUtils.getRequestRetryInterval(mContext, mSubId);
            mRequestRetryTime = Optional.of(Instant.now().plusMillis(retryInterval));
        }
    }

    private void removeRequestRetryTime() {
        if (mRequestRetryTime.isPresent()) {
            mRequestRetryTime = Optional.empty();
        }
    }

    /**
     * Set the timer to exit the device disallowed state and then trigger a PUBLISH request.
     */
    private void setupExitStateTimer() {
        if (!mExitStateTime.isPresent()) {
            long expirySec = UceUtils.getNonRcsCapabilitiesCacheExpiration(mContext, mSubId);
            mExitStateTime = Optional.of(Instant.now().plusSeconds(expirySec));
            logd("setupExitStateTimer: expirationSec=" + expirySec + ", time=" + mExitStateTime);

            // Setup timer to exit the request disallowed state.
            mUceCtrlCallback.setupResetDeviceStateTimer(expirySec);
        }
    }

    /**
     * Remove the exit state timer.
     */
    private void removeExitStateTimer() {
        if (mExitStateTime.isPresent()) {
            mExitStateTime = Optional.empty();
            mUceCtrlCallback.clearResetDeviceStateTimer();
        }
    }

    /**
     * Save the given device sate to the shared preference.
     * @param deviceState
     */
    private void saveDeviceStateToPreference(DeviceStateResult deviceState) {
        boolean result = UceUtils.saveDeviceStateToPreference(mContext, mSubId, deviceState);
        logd("saveDeviceStateToPreference: result=" + result + ", state= " + deviceState);
    }

    /**
     * Remove the device state information from the shared preference because the device is allowed
     * execute UCE requests.
     */
    private void removeDeviceStateFromPreference() {
        boolean result = UceUtils.removeDeviceStateFromPreference(mContext, mSubId);
        logd("removeDeviceStateFromPreference: result=" + result);
    }

    private void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId);
        builder.append("] ");
        return builder;
    }
}
