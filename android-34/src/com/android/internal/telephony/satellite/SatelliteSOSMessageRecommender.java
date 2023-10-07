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

package com.android.internal.telephony.satellite;

import static android.telephony.satellite.SatelliteManager.KEY_SATELLITE_COMMUNICATION_ALLOWED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_ERROR_NONE;

import android.annotation.NonNull;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.provider.DeviceConfig;
import android.telecom.Call;
import android.telecom.Connection;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.RegistrationManager;
import android.telephony.satellite.ISatelliteProvisionStateCallback;
import android.util.Pair;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.metrics.SatelliteStats;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This module is responsible for monitoring the cellular service state and IMS registration state
 * during an emergency call and notify Dialer when Telephony is not able to find any network and
 * the call likely will not get connected so that Dialer will prompt the user if they would like to
 * switch to satellite messaging.
 */
public class SatelliteSOSMessageRecommender extends Handler {
    private static final String TAG = "SatelliteSOSMessageRecommender";

    /**
     * Device config for the timeout duration in milliseconds to determine whether to recommend
     * Dialer to show the SOS button to users.
     * <p>
     * The timer is started when there is an ongoing emergency call, and the IMS is not registered,
     * and cellular service is not available. When the timer expires, SatelliteSOSMessageRecommender
     * will send the event EVENT_DISPLAY_SOS_MESSAGE to Dialer, which will then prompt user to
     * switch to using satellite SOS messaging.
     */
    public static final String EMERGENCY_CALL_TO_SOS_MSG_HYSTERESIS_TIMEOUT_MILLIS =
            "emergency_call_to_sos_msg_hysteresis_timeout_millis";
    /**
     * The default value of {@link #EMERGENCY_CALL_TO_SOS_MSG_HYSTERESIS_TIMEOUT_MILLIS} when it is
     * not provided in the device config.
     */
    public static final long DEFAULT_EMERGENCY_CALL_TO_SOS_MSG_HYSTERESIS_TIMEOUT_MILLIS = 20000;

    private static final int EVENT_EMERGENCY_CALL_STARTED = 1;
    protected static final int EVENT_CELLULAR_SERVICE_STATE_CHANGED = 2;
    private static final int EVENT_IMS_REGISTRATION_STATE_CHANGED = 3;
    protected static final int EVENT_TIME_OUT = 4;
    private static final int EVENT_SATELLITE_PROVISIONED_STATE_CHANGED = 5;
    private static final int EVENT_EMERGENCY_CALL_CONNECTION_STATE_CHANGED = 6;

    @NonNull
    private final SatelliteController mSatelliteController;
    private ImsManager mImsManager;

    private Connection mEmergencyConnection = null;
    /* The phone used for emergency call */
    private Phone mPhone = null;
    private final ISatelliteProvisionStateCallback mISatelliteProvisionStateCallback;
    @ServiceState.RegState
    private AtomicInteger mCellularServiceState = new AtomicInteger();
    private AtomicBoolean mIsImsRegistered = new AtomicBoolean();
    private AtomicBoolean mIsSatelliteAllowedInCurrentLocation = new AtomicBoolean();
    private final ResultReceiver mReceiverForRequestIsSatelliteAllowedForCurrentLocation;
    private final long mTimeoutMillis;
    protected int mCountOfTimerStarted = 0;

    private RegistrationManager.RegistrationCallback mImsRegistrationCallback =
            new RegistrationManager.RegistrationCallback() {
                @Override
                public void onRegistered(ImsRegistrationAttributes attributes) {
                    sendMessage(obtainMessage(EVENT_IMS_REGISTRATION_STATE_CHANGED, true));
                }

                @Override
                public void onUnregistered(ImsReasonInfo info) {
                    sendMessage(obtainMessage(EVENT_IMS_REGISTRATION_STATE_CHANGED, false));
                }
            };

    /**
     * Create an instance of SatelliteSOSMessageRecommender.
     *
     * @param looper The looper used with the handler of this class.
     */
    public SatelliteSOSMessageRecommender(@NonNull Looper looper) {
        this(looper, SatelliteController.getInstance(), null,
                getEmergencyCallToSosMsgHysteresisTimeoutMillis());
    }

    /**
     * Create an instance of SatelliteSOSMessageRecommender. This constructor should be used in
     * only unit tests.
     *
     * @param looper The looper used with the handler of this class.
     * @param satelliteController The SatelliteController singleton instance.
     * @param imsManager The ImsManager instance associated with the phone, which is used for making
     *                   the emergency call. This argument is not null only in unit tests.
     * @param timeoutMillis The timeout duration of the timer.
     */
    @VisibleForTesting
    protected SatelliteSOSMessageRecommender(@NonNull Looper looper,
            @NonNull SatelliteController satelliteController, ImsManager imsManager,
            long timeoutMillis) {
        super(looper);
        mSatelliteController = satelliteController;
        mImsManager = imsManager;
        mTimeoutMillis = timeoutMillis;
        mISatelliteProvisionStateCallback = new ISatelliteProvisionStateCallback.Stub() {
            @Override
            public void onSatelliteProvisionStateChanged(boolean provisioned) {
                logd("onSatelliteProvisionStateChanged: provisioned=" + provisioned);
                sendMessage(obtainMessage(EVENT_SATELLITE_PROVISIONED_STATE_CHANGED, provisioned));
            }
        };
        mReceiverForRequestIsSatelliteAllowedForCurrentLocation = new ResultReceiver(this) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == SATELLITE_ERROR_NONE) {
                    if (resultData.containsKey(KEY_SATELLITE_COMMUNICATION_ALLOWED)) {
                        boolean isSatelliteCommunicationAllowed =
                                resultData.getBoolean(KEY_SATELLITE_COMMUNICATION_ALLOWED);
                        mIsSatelliteAllowedInCurrentLocation.set(isSatelliteCommunicationAllowed);
                        if (!isSatelliteCommunicationAllowed) {
                            logd("Satellite is not allowed for current location.");
                            cleanUpResources();
                        }
                    } else {
                        loge("KEY_SATELLITE_COMMUNICATION_ALLOWED does not exist.");
                        mIsSatelliteAllowedInCurrentLocation.set(false);
                        cleanUpResources();
                    }
                } else {
                    loge("requestIsSatelliteCommunicationAllowedForCurrentLocation() resultCode="
                            + resultCode);
                    mIsSatelliteAllowedInCurrentLocation.set(false);
                    cleanUpResources();
                }
            }
        };
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case EVENT_EMERGENCY_CALL_STARTED:
                handleEmergencyCallStartedEvent((Pair<Connection, Phone>) msg.obj);
                break;
            case EVENT_TIME_OUT:
                handleTimeoutEvent();
                break;
            case EVENT_SATELLITE_PROVISIONED_STATE_CHANGED:
                handleSatelliteProvisionStateChangedEvent((boolean) msg.obj);
                break;
            case EVENT_EMERGENCY_CALL_CONNECTION_STATE_CHANGED:
                handleEmergencyCallConnectionStateChangedEvent((Pair<String, Integer>) msg.obj);
                break;
            case EVENT_IMS_REGISTRATION_STATE_CHANGED:
                handleImsRegistrationStateChangedEvent((boolean) msg.obj);
                break;
            case EVENT_CELLULAR_SERVICE_STATE_CHANGED:
                AsyncResult ar = (AsyncResult) msg.obj;
                handleCellularServiceStateChangedEvent((ServiceState) ar.result);
                break;
            default:
                logd("handleMessage: unexpected message code: " + msg.what);
                break;
        }
    }

    /**
     * Inform SatelliteSOSMessageRecommender that an emergency call has just started.
     *
     * @param connection The connection created by TelephonyConnectionService for the emergency
     *                   call.
     * @param phone The phone used for the emergency call.
     */
    public void onEmergencyCallStarted(@NonNull Connection connection, @NonNull Phone phone) {
        if (!mSatelliteController.isSatelliteSupported()) {
            logd("onEmergencyCallStarted: satellite is not supported");
            return;
        }
        Pair<Connection, Phone> argument = new Pair<>(connection, phone);
        sendMessage(obtainMessage(EVENT_EMERGENCY_CALL_STARTED, argument));
    }

    /**
     * Inform SatelliteSOSMessageRecommender that the state of the emergency call connection has
     * changed.
     *
     * @param callId The ID of the emergency call.
     * @param state The connection state of the emergency call.
     */
    public void onEmergencyCallConnectionStateChanged(
            String callId, @Connection.ConnectionState int state) {
        Pair<String, Integer> argument = new Pair<>(callId, state);
        sendMessage(obtainMessage(EVENT_EMERGENCY_CALL_CONNECTION_STATE_CHANGED, argument));
    }

    private void handleEmergencyCallStartedEvent(@NonNull Pair<Connection, Phone> arg) {
        mSatelliteController.requestIsSatelliteCommunicationAllowedForCurrentLocation(
                SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                mReceiverForRequestIsSatelliteAllowedForCurrentLocation);
        if (mPhone != null) {
            logd("handleEmergencyCallStartedEvent: new emergency call started while there is "
                    + " an ongoing call");
            unregisterForInterestedStateChangedEvents(mPhone);
        }
        mPhone = arg.second;
        mEmergencyConnection = arg.first;
        mCellularServiceState.set(mPhone.getServiceState().getState());
        mIsImsRegistered.set(mPhone.isImsRegistered());
        handleStateChangedEventForHysteresisTimer();
        registerForInterestedStateChangedEvents(mPhone);
    }

    private void handleSatelliteProvisionStateChangedEvent(boolean provisioned) {
        if (!provisioned) {
            cleanUpResources();
        }
    }

    private void handleTimeoutEvent() {
        boolean isDialerNotified = false;
        if (!mIsImsRegistered.get() && !isCellularAvailable()
                && mIsSatelliteAllowedInCurrentLocation.get()
                && mSatelliteController.isSatelliteProvisioned()
                && shouldTrackCall(mEmergencyConnection.getState())) {
            logd("handleTimeoutEvent: Sending EVENT_DISPLAY_SOS_MESSAGE to Dialer...");
            mEmergencyConnection.sendConnectionEvent(Call.EVENT_DISPLAY_SOS_MESSAGE, null);
            isDialerNotified = true;
        }
        reportEsosRecommenderDecision(isDialerNotified);
        cleanUpResources();
    }

    private void handleEmergencyCallConnectionStateChangedEvent(
            @NonNull Pair<String, Integer> arg) {
        if (mEmergencyConnection == null) {
            // Either the call was not created or the timer already timed out.
            return;
        }

        String callId = arg.first;
        int state = arg.second;
        if (!mEmergencyConnection.getTelecomCallId().equals(callId)) {
            loge("handleEmergencyCallConnectionStateChangedEvent: unexpected state changed event "
                    + ", mEmergencyConnection=" + mEmergencyConnection + ", callId=" + callId
                    + ", state=" + state);
            /**
             * TelephonyConnectionService sent us a connection state changed event for a call that
             * we're not tracking. There must be some unexpected things happened in
             * TelephonyConnectionService. Thus, we need to clean up the resources.
             */
            cleanUpResources();
            return;
        }

        if (!shouldTrackCall(state)) {
            reportEsosRecommenderDecision(false);
            cleanUpResources();
        }
    }

    private void handleImsRegistrationStateChangedEvent(boolean registered) {
        if (registered != mIsImsRegistered.get()) {
            mIsImsRegistered.set(registered);
            handleStateChangedEventForHysteresisTimer();
        }
    }

    private void handleCellularServiceStateChangedEvent(@NonNull ServiceState serviceState) {
        int state = serviceState.getState();
        if (mCellularServiceState.get() != state) {
            mCellularServiceState.set(state);
            handleStateChangedEventForHysteresisTimer();
        }
    }

    private void reportEsosRecommenderDecision(boolean isDialerNotified) {
        SatelliteStats.getInstance().onSatelliteSosMessageRecommender(
                new SatelliteStats.SatelliteSosMessageRecommenderParams.Builder()
                        .setDisplaySosMessageSent(isDialerNotified)
                        .setCountOfTimerStarted(mCountOfTimerStarted)
                        .setImsRegistered(mIsImsRegistered.get())
                        .setCellularServiceState(mCellularServiceState.get())
                        .build());
    }

    private void cleanUpResources() {
        stopTimer();
        if (mPhone != null) {
            unregisterForInterestedStateChangedEvents(mPhone);
            mPhone = null;
        }
        mEmergencyConnection = null;
        mCountOfTimerStarted = 0;
    }

    private void registerForInterestedStateChangedEvents(@NonNull Phone phone) {
        mSatelliteController.registerForSatelliteProvisionStateChanged(
                SubscriptionManager.DEFAULT_SUBSCRIPTION_ID, mISatelliteProvisionStateCallback);
        phone.registerForServiceStateChanged(this, EVENT_CELLULAR_SERVICE_STATE_CHANGED, null);
        registerForImsRegistrationStateChanged(phone);
    }

    private void registerForImsRegistrationStateChanged(@NonNull Phone phone) {
        ImsManager imsManager = (mImsManager != null) ? mImsManager : ImsManager.getInstance(
                phone.getContext(), phone.getPhoneId());
        try {
            imsManager.addRegistrationCallback(mImsRegistrationCallback, this::post);
        } catch (ImsException ex) {
            loge("registerForImsRegistrationStateChanged: ex=" + ex);
        }
    }

    private void unregisterForInterestedStateChangedEvents(@NonNull Phone phone) {
        mSatelliteController.unregisterForSatelliteProvisionStateChanged(
                SubscriptionManager.DEFAULT_SUBSCRIPTION_ID, mISatelliteProvisionStateCallback);
        phone.unregisterForServiceStateChanged(this);
        unregisterForImsRegistrationStateChanged(phone);
    }

    private void unregisterForImsRegistrationStateChanged(@NonNull Phone phone) {
        ImsManager imsManager = (mImsManager != null) ? mImsManager : ImsManager.getInstance(
                phone.getContext(), phone.getPhoneId());
        imsManager.removeRegistrationListener(mImsRegistrationCallback);
    }

    private boolean isCellularAvailable() {
        return (mCellularServiceState.get() == ServiceState.STATE_IN_SERVICE
                || mCellularServiceState.get() == ServiceState.STATE_EMERGENCY_ONLY);
    }

    private void handleStateChangedEventForHysteresisTimer() {
        if (!mIsImsRegistered.get() && !isCellularAvailable()) {
            startTimer();
        } else {
            stopTimer();
        }
    }

    private void startTimer() {
        if (hasMessages(EVENT_TIME_OUT)) {
            return;
        }
        sendMessageDelayed(obtainMessage(EVENT_TIME_OUT), mTimeoutMillis);
        mCountOfTimerStarted++;
    }

    private void stopTimer() {
        removeMessages(EVENT_TIME_OUT);
    }

    private static long getEmergencyCallToSosMsgHysteresisTimeoutMillis() {
        return DeviceConfig.getLong(DeviceConfig.NAMESPACE_TELEPHONY,
                EMERGENCY_CALL_TO_SOS_MSG_HYSTERESIS_TIMEOUT_MILLIS,
                DEFAULT_EMERGENCY_CALL_TO_SOS_MSG_HYSTERESIS_TIMEOUT_MILLIS);
    }

    private boolean shouldTrackCall(int connectionState) {
        /**
         * An active connection state means both parties are connected to the call and can actively
         * communicate. A disconnected connection state means the emergency call has ended. In both
         * cases, we don't need to track the call anymore.
         */
        return (connectionState != Connection.STATE_ACTIVE
                && connectionState != Connection.STATE_DISCONNECTED);
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }
}
