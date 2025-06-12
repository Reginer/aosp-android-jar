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

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.telephony.ServiceState.STATE_EMERGENCY_ONLY;
import static android.telephony.ServiceState.STATE_IN_SERVICE;
import static android.telephony.ServiceState.STATE_OUT_OF_SERVICE;
import static android.telephony.TelephonyManager.EXTRA_EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE;
import static android.telephony.TelephonyManager.EXTRA_EMERGENCY_CALL_TO_SATELLITE_LAUNCH_INTENT;
import static android.telephony.satellite.SatelliteManager.EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_SOS;
import static android.telephony.satellite.SatelliteManager.EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_T911;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DISALLOWED_REASON_NOT_PROVISIONED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DISALLOWED_REASON_NOT_SUPPORTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_DISALLOWED_REASON_UNSUPPORTED_DEFAULT_MSG_APP;

import static com.android.internal.telephony.satellite.SatelliteController.INVALID_EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.OutcomeReceiver;
import android.os.SystemProperties;
import android.provider.DeviceConfig;
import android.telecom.Connection;
import android.telephony.PersistentLogger;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.RegistrationManager;
import android.telephony.satellite.ISatelliteProvisionStateCallback;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteSubscriberProvisionStatus;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.TelephonyCountryDetector;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.metrics.SatelliteStats;

import java.util.Arrays;
import java.util.List;
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
    private static final String ALLOW_MOCK_MODEM_PROPERTY = "persist.radio.allow_mock_modem";
    private static final String BOOT_ALLOW_MOCK_MODEM_PROPERTY = "ro.boot.radio.allow_mock_modem";
    private static final int EVENT_EMERGENCY_CALL_STARTED = 1;
    protected static final int EVENT_SERVICE_STATE_CHANGED = 2;
    protected static final int EVENT_TIME_OUT = 3;
    private static final int EVENT_SATELLITE_PROVISIONED_STATE_CHANGED = 4;
    private static final int EVENT_EMERGENCY_CALL_CONNECTION_STATE_CHANGED = 5;
    private static final int CMD_SEND_EVENT_DISPLAY_EMERGENCY_MESSAGE_FORCEFULLY = 6;
    private static final int EVENT_SATELLITE_ACCESS_RESTRICTION_CHECKING_RESULT = 7;

    @NonNull private final Context mContext;
    @NonNull
    private final SatelliteController mSatelliteController;
    @NonNull
    private final TelephonyCountryDetector mCountryDetector;
    private ImsManager mImsManager;
    @NonNull
    private final FeatureFlags mFeatureFlags;

    private Connection mEmergencyConnection = null;
    private final ISatelliteProvisionStateCallback mISatelliteProvisionStateCallback;
    /** Key: Phone ID; Value: IMS RegistrationCallback */
    private SparseArray<RegistrationManager.RegistrationCallback>
            mImsRegistrationCallbacks = new SparseArray<>();
    @GuardedBy("mLock")
    private boolean mIsSatelliteAllowedForCurrentLocation = false;
    @GuardedBy("mLock")
    private boolean mCheckingAccessRestrictionInProgress = false;
    protected long mTimeoutMillis = 0;
    private final long mOemEnabledTimeoutMillis;
    protected final AtomicBoolean mIsSatelliteConnectedViaCarrierWithinHysteresisTime =
            new AtomicBoolean(false);
    protected final AtomicInteger mSubIdOfSatelliteConnectedViaCarrierWithinHysteresisTime =
            new AtomicInteger(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    @GuardedBy("mLock")
    private boolean mIsTimerTimedOut = false;
    protected int mCountOfTimerStarted = 0;
    private final Object mLock = new Object();

    @Nullable private PersistentLogger mPersistentLogger = null;

    private boolean mIsTestEmergencyNumber = false;

    /**
     * Create an instance of SatelliteSOSMessageRecommender.
     *
     * @param context The Context for the SatelliteSOSMessageRecommender.
     * @param looper The looper used with the handler of this class.
     */
    public SatelliteSOSMessageRecommender(@NonNull Context context,
            @NonNull Looper looper) {
        this(context, looper, SatelliteController.getInstance(), null);
    }

    /**
     * Create an instance of SatelliteSOSMessageRecommender. This constructor should be used in
     * only unit tests.
     *
     * @param context The Context for the SatelliteSOSMessageRecommender.
     * @param looper The looper used with the handler of this class.
     * @param satelliteController The SatelliteController singleton instance.
     * @param imsManager The ImsManager instance associated with the phone, which is used for making
     *                   the emergency call. This argument is not null only in unit tests.
     */
    @VisibleForTesting
    protected SatelliteSOSMessageRecommender(@NonNull Context context, @NonNull Looper looper,
            @NonNull SatelliteController satelliteController,
            ImsManager imsManager) {
        super(looper);
        mPersistentLogger = SatelliteServiceUtils.getPersistentLogger(context);
        mContext = context;
        mSatelliteController = satelliteController;
        mFeatureFlags = mSatelliteController.getFeatureFlags();
        mCountryDetector = TelephonyCountryDetector.getInstance(context, mFeatureFlags);
        mImsManager = imsManager;
        mOemEnabledTimeoutMillis =
                getOemEnabledEmergencyCallWaitForConnectionTimeoutMillis(context);
        mISatelliteProvisionStateCallback = new ISatelliteProvisionStateCallback.Stub() {
            @Override
            public void onSatelliteProvisionStateChanged(boolean provisioned) {
                plogd("onSatelliteProvisionStateChanged: provisioned=" + provisioned);
                sendMessage(obtainMessage(EVENT_SATELLITE_PROVISIONED_STATE_CHANGED, provisioned));
            }

            @Override
            public void onSatelliteSubscriptionProvisionStateChanged(
                    List<SatelliteSubscriberProvisionStatus> satelliteSubscriberProvisionStatus) {
                plogd("onSatelliteSubscriptionProvisionStateChanged: "
                        + satelliteSubscriberProvisionStatus);
            }
        };
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case EVENT_EMERGENCY_CALL_STARTED:
                handleEmergencyCallStartedEvent((Connection) msg.obj);
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
            case EVENT_SERVICE_STATE_CHANGED:
                handleStateChangedEventForHysteresisTimer();
                break;
            case CMD_SEND_EVENT_DISPLAY_EMERGENCY_MESSAGE_FORCEFULLY:
                handleCmdSendEventDisplayEmergencyMessageForcefully((Connection) msg.obj);
                break;
            case EVENT_SATELLITE_ACCESS_RESTRICTION_CHECKING_RESULT:
                handleSatelliteAccessRestrictionCheckingResult((boolean) msg.obj);
                break;
            default:
                plogd("handleMessage: unexpected message code: " + msg.what);
                break;
        }
    }

    /**
     * Inform SatelliteSOSMessageRecommender that an emergency call has just started.
     *
     * @param connection The connection created by TelephonyConnectionService for the emergency
     *                   call.
     */
    public void onEmergencyCallStarted(@NonNull Connection connection,
            boolean isTestEmergencyNumber) {
        if (!isSatelliteSupported()) {
            plogd("onEmergencyCallStarted: satellite is not supported");
            return;
        }
        mIsTestEmergencyNumber = isTestEmergencyNumber;

        if (hasMessages(EVENT_EMERGENCY_CALL_STARTED)) {
            logd("onEmergencyCallStarted: Ignoring due to ongoing event:");
            return;
        }

        /*
         * Right now, assume that the device is connected to satellite via carrier within hysteresis
         * time. However, this might not be correct when the monitoring timer expires. Thus, we
         * should do this check now so that we have higher chance of sending the event
         * EVENT_DISPLAY_EMERGENCY_MESSAGE to Dialer.
         */
        updateSatelliteConnectedViaCarrierWithinHysteresisTimeState();
        sendMessage(obtainMessage(EVENT_EMERGENCY_CALL_STARTED, connection));
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
        plogd("callId=" + callId + ", state=" + state);
        if (!isSatelliteSupported()) {
            plogd("onEmergencyCallConnectionStateChanged: satellite is not supported");
            return;
        }
        Pair<String, Integer> argument = new Pair<>(callId, state);
        sendMessage(obtainMessage(EVENT_EMERGENCY_CALL_CONNECTION_STATE_CHANGED, argument));
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected ComponentName getDefaultSmsApp() {
        return SmsApplication.getDefaultSendToApplication(mContext, false);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected boolean updateAndGetProvisionState() {
        mSatelliteController.updateSatelliteProvisionedStatePerSubscriberId();
        return isDeviceProvisioned();
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected boolean isSatelliteAllowedByReasons() {
        SatelliteManager satelliteManager = mContext.getSystemService(SatelliteManager.class);
        int[] disallowedReasons = satelliteManager.getSatelliteDisallowedReasons();
        if (Arrays.stream(disallowedReasons).anyMatch(r ->
                (r == SATELLITE_DISALLOWED_REASON_UNSUPPORTED_DEFAULT_MSG_APP
                        || r == SATELLITE_DISALLOWED_REASON_NOT_PROVISIONED
                        || r == SATELLITE_DISALLOWED_REASON_NOT_SUPPORTED))) {
            plogd("isAllowedForDefaultMessageApp:false, disallowedReasons="
                    + Arrays.toString(disallowedReasons));
            return false;
        }
        return true;
    }

    private void handleEmergencyCallStartedEvent(@NonNull Connection connection) {
        plogd("handleEmergencyCallStartedEvent: connection=" + connection);
        mSatelliteController.setLastEmergencyCallTime();

        if (sendEventDisplayEmergencyMessageForcefully(connection)) {
            return;
        }

        selectEmergencyCallWaitForConnectionTimeoutDuration();
        if (mEmergencyConnection == null) {
            registerForInterestedStateChangedEvents();
        }
        mEmergencyConnection = connection;
        handleStateChangedEventForHysteresisTimer();

        synchronized (mLock) {
            mCheckingAccessRestrictionInProgress = false;
            mIsSatelliteAllowedForCurrentLocation = false;
        }
    }

    private void handleSatelliteProvisionStateChangedEvent(boolean provisioned) {
        if (!provisioned
                && !isSatelliteConnectedViaCarrierWithinHysteresisTime()) {
            cleanUpResources(false);
        }
    }

    private void handleTimeoutEvent() {
        synchronized (mLock) {
            mIsTimerTimedOut = true;
            evaluateSendingConnectionEventDisplayEmergencyMessage();
        }
    }

    private void evaluateSendingConnectionEventDisplayEmergencyMessage() {
        synchronized (mLock) {
            if (mEmergencyConnection == null) {
                ploge("No emergency call is ongoing...");
                return;
            }

            if (!mIsTimerTimedOut || mCheckingAccessRestrictionInProgress) {
                plogd("mIsTimerTimedOut=" + mIsTimerTimedOut
                        + ", mCheckingAccessRestrictionInProgress="
                        + mCheckingAccessRestrictionInProgress);
                return;
            }

            updateAndGetProvisionState();

            /*
             * The device might be connected to satellite after the emergency call started. Thus, we
             * need to do this check again so that we will have higher chance of sending the event
             * EVENT_DISPLAY_EMERGENCY_MESSAGE to Dialer.
             */
            updateSatelliteViaCarrierAvailability();

            boolean isDialerNotified = false;
            boolean isCellularAvailable = SatelliteServiceUtils.isCellularAvailable();
            if (!isCellularAvailable
                    && isSatelliteAllowed()
                    && ((isDeviceProvisioned() && isSatelliteAllowedByReasons())
                    || isSatelliteConnectedViaCarrierWithinHysteresisTime())
                    && shouldTrackCall(mEmergencyConnection.getState())) {
                plogd("handleTimeoutEvent: Sent EVENT_DISPLAY_EMERGENCY_MESSAGE to Dialer");
                Bundle extras = createExtraBundleForEventDisplayEmergencyMessage(
                        mIsTestEmergencyNumber);
                mEmergencyConnection.sendConnectionEvent(
                        TelephonyManager.EVENT_DISPLAY_EMERGENCY_MESSAGE, extras);
                isDialerNotified = true;

            }
            plogd("handleTimeoutEvent: isImsRegistered=" + isImsRegistered()
                    + ", isCellularAvailable=" + isCellularAvailable
                    + ", isSatelliteAllowed=" + isSatelliteAllowed()
                    + ", shouldTrackCall=" + shouldTrackCall(mEmergencyConnection.getState()));
            cleanUpResources(isDialerNotified);
        }
    }

    private boolean isSatelliteAllowed() {
        synchronized (mLock) {
            if (isSatelliteConnectedViaCarrierWithinHysteresisTime()) return true;
            return mIsSatelliteAllowedForCurrentLocation;
        }
    }

    private void updateSatelliteViaCarrierAvailability() {
        if (!mIsSatelliteConnectedViaCarrierWithinHysteresisTime.get()) {
            updateSatelliteConnectedViaCarrierWithinHysteresisTimeState();
        }
    }

    /**
     * Check if satellite is available via OEM
     * @return {@code true} if satellite is provisioned via OEM else return {@code false}
     */
    @VisibleForTesting
    public boolean isDeviceProvisioned() {
        Boolean satelliteProvisioned = mSatelliteController.isDeviceProvisioned();
        return satelliteProvisioned != null ? satelliteProvisioned : false;
    }

    private boolean isSatelliteConnectedViaCarrierWithinHysteresisTime() {
        return mIsSatelliteConnectedViaCarrierWithinHysteresisTime.get();
    }

    private void handleEmergencyCallConnectionStateChangedEvent(
            @NonNull Pair<String, Integer> arg) {
        mSatelliteController.setLastEmergencyCallTime();
        if (mEmergencyConnection == null) {
            // Either the call was not created or the timer already timed out.
            return;
        }

        String callId = arg.first;
        int state = arg.second;
        if (!mEmergencyConnection.getTelecomCallId().equals(callId)) {
            ploge("handleEmergencyCallConnectionStateChangedEvent: unexpected state changed event "
                    + ", mEmergencyConnection=" + mEmergencyConnection + ", callId=" + callId
                    + ", state=" + state);
            /*
             * TelephonyConnectionService sent us a connection state changed event for a call that
             * we're not tracking. There must be some unexpected things happened in
             * TelephonyConnectionService. Thus, we need to clean up the resources.
             */
            cleanUpResources(false);
            return;
        }

        if (!shouldTrackCall(state)) {
            cleanUpResources(false);
        } else {
            // Location service will enter emergency mode only when connection state changes to
            // STATE_DIALING
            if (state == Connection.STATE_DIALING
                    && mSatelliteController.isSatelliteSupportedViaOem()) {
                requestIsSatelliteAllowedForCurrentLocation();
            }
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected void reportESosRecommenderDecision(boolean isDialerNotified) {
        SatelliteStats.getInstance().onSatelliteSosMessageRecommender(
                new SatelliteStats.SatelliteSosMessageRecommenderParams.Builder()
                        .setDisplaySosMessageSent(isDialerNotified)
                        .setCountOfTimerStarted(mCountOfTimerStarted)
                        .setImsRegistered(isImsRegistered())
                        .setCellularServiceState(getBestCellularServiceState())
                        .setIsMultiSim(isMultiSim())
                        .setRecommendingHandoverType(getEmergencyCallToSatelliteHandoverType())
                        .setIsSatelliteAllowedInCurrentLocation(isSatelliteAllowed())
                        .setIsWifiConnected(mCountryDetector.isWifiNetworkConnected())
                        .setCarrierId(mSatelliteController.getSatelliteCarrierId())
                        .setIsNtnOnlyCarrier(mSatelliteController.isNtnOnlyCarrier()).build());
    }

    private void cleanUpResources(boolean isDialerNotified) {
        plogd("cleanUpResources");
        reportESosRecommenderDecision(isDialerNotified);
        synchronized (mLock) {
            stopTimer();
            if (mEmergencyConnection != null) {
                unregisterForInterestedStateChangedEvents();
            }
            mEmergencyConnection = null;
            mCountOfTimerStarted = 0;
            mIsTimerTimedOut = false;
            mCheckingAccessRestrictionInProgress = false;
            mIsSatelliteAllowedForCurrentLocation = false;
            mIsTestEmergencyNumber = false;
        }
    }

    private void registerForInterestedStateChangedEvents() {
        mSatelliteController.registerForSatelliteProvisionStateChanged(
                mISatelliteProvisionStateCallback);
        for (Phone phone : PhoneFactory.getPhones()) {
            phone.registerForServiceStateChanged(
                    this, EVENT_SERVICE_STATE_CHANGED, null);
        }
    }

    private void registerForImsRegistrationStateChanged(@NonNull Phone phone) {
        ImsManager imsManager = (mImsManager != null) ? mImsManager : ImsManager.getInstance(
                phone.getContext(), phone.getPhoneId());
        try {
            imsManager.addRegistrationCallback(
                    getOrCreateImsRegistrationCallback(phone.getPhoneId()), this::post);
        } catch (ImsException ex) {
            ploge("registerForImsRegistrationStateChanged: ex=" + ex);
        }
    }

    private void unregisterForInterestedStateChangedEvents() {
        mSatelliteController.unregisterForSatelliteProvisionStateChanged(
                mISatelliteProvisionStateCallback);
        for (Phone phone : PhoneFactory.getPhones()) {
            phone.unregisterForServiceStateChanged(this);
        }
    }

    private void unregisterForImsRegistrationStateChanged(@NonNull Phone phone) {
        if (mImsRegistrationCallbacks.contains(phone.getPhoneId())) {
            ImsManager imsManager =
                    (mImsManager != null) ? mImsManager : ImsManager.getInstance(
                            phone.getContext(), phone.getPhoneId());
            imsManager.removeRegistrationListener(
                    mImsRegistrationCallbacks.get(phone.getPhoneId()));
        } else {
            ploge("Phone ID=" + phone.getPhoneId() + " was not registered with ImsManager");
        }
    }

    /**
     * @return {@link ServiceState#STATE_IN_SERVICE} if any subscription is in this state; else
     * {@link ServiceState#STATE_EMERGENCY_ONLY} if any subscription is in this state; else
     * {@link ServiceState#STATE_OUT_OF_SERVICE}.
     */
    private int getBestCellularServiceState() {
        boolean isStateOutOfService = true;
        for (Phone phone : PhoneFactory.getPhones()) {
            ServiceState serviceState = phone.getServiceState();
            if (serviceState != null) {
                int state = serviceState.getState();
                if (!serviceState.isUsingNonTerrestrialNetwork()) {
                    if ((state == STATE_IN_SERVICE)) {
                        return STATE_IN_SERVICE;
                    } else if (state == STATE_EMERGENCY_ONLY) {
                        isStateOutOfService = false;
                    }
                }
            }
        }
        return isStateOutOfService ? STATE_OUT_OF_SERVICE : STATE_EMERGENCY_ONLY;
    }

    private boolean isImsRegistered() {
        for (Phone phone : PhoneFactory.getPhones()) {
            if (phone.isImsRegistered()) return true;
        }
        return false;
    }

    private synchronized void handleStateChangedEventForHysteresisTimer() {
        if (!SatelliteServiceUtils.isCellularAvailable() && mEmergencyConnection != null) {
            startTimer();
        } else {
            plogd("handleStateChangedEventForHysteresisTimer stopTimer, mEmergencyConnection="
                    + mEmergencyConnection);
            stopTimer();
        }
    }

    private void startTimer() {
        synchronized (mLock) {
            if (hasMessages(EVENT_TIME_OUT)) {
                return;
            }
            sendMessageDelayed(obtainMessage(EVENT_TIME_OUT), mTimeoutMillis);
            mCountOfTimerStarted++;
            mIsTimerTimedOut = false;
            plogd("startTimer mCountOfTimerStarted=" + mCountOfTimerStarted);
        }
    }

    private void stopTimer() {
        synchronized (mLock) {
            removeMessages(EVENT_TIME_OUT);
        }
    }

    private void handleSatelliteAccessRestrictionCheckingResult(boolean satelliteAllowed) {
        synchronized (mLock) {
            mIsSatelliteAllowedForCurrentLocation = satelliteAllowed;
            mCheckingAccessRestrictionInProgress = false;
            evaluateSendingConnectionEventDisplayEmergencyMessage();
        }
    }

    private void selectEmergencyCallWaitForConnectionTimeoutDuration() {
        if (isSatelliteConnectedViaCarrierWithinHysteresisTime()) {
            int satelliteSubId = mSubIdOfSatelliteConnectedViaCarrierWithinHysteresisTime.get();
            mTimeoutMillis =
                    mSatelliteController.getCarrierEmergencyCallWaitForConnectionTimeoutMillis(
                            satelliteSubId);
        } else {
            int satelliteSubId = mSatelliteController.getSelectedSatelliteSubId();
            if (!SatelliteServiceUtils.isNtnOnlySubscriptionId(satelliteSubId)) {
                mTimeoutMillis =
                    mSatelliteController.getCarrierEmergencyCallWaitForConnectionTimeoutMillis(
                        satelliteSubId);
            } else {
                mTimeoutMillis = mOemEnabledTimeoutMillis;
            }
        }
        plogd("mTimeoutMillis = " + mTimeoutMillis);
    }

    private static long getOemEnabledEmergencyCallWaitForConnectionTimeoutMillis(
            @NonNull Context context) {
        return context.getResources().getInteger(
                R.integer.config_emergency_call_wait_for_connection_timeout_millis);
    }

    /**
     * @return The Pair(PackageName, ClassName) of the oem-enabled satellite handover app.
     */
    @NonNull
    private static Pair<String, String> getOemEnabledSatelliteHandoverAppFromOverlayConfig(
            @NonNull Context context) {
        String app = null;
        try {
            app = context.getResources().getString(
                    R.string.config_oem_enabled_satellite_sos_handover_app);
        } catch (Resources.NotFoundException ex) {
            loge("getOemEnabledSatelliteHandoverAppFromOverlayConfig: ex=" + ex);
        }
        if (TextUtils.isEmpty(app) && isMockModemAllowed()) {
            logd("getOemEnabledSatelliteHandoverAppFromOverlayConfig: Read "
                    + "config_oem_enabled_satellite_sos_handover_app from device config");
            app = DeviceConfig.getString(DeviceConfig.NAMESPACE_TELEPHONY,
                    "config_oem_enabled_satellite_sos_handover_app", "");
        }
        if (TextUtils.isEmpty(app)) return new Pair<>("", "");

        String[] appComponent = app.split(";");
        if (appComponent.length == 2) {
            return new Pair<>(appComponent[0], appComponent[1]);
        } else {
            loge("getOemEnabledSatelliteHandoverAppFromOverlayConfig: invalid configured app="
                    + app);
        }
        return new Pair<>("", "");
    }


    @Nullable
    private static String getSatelliteEmergencyHandoverIntentActionFromOverlayConfig(
            @NonNull Context context, boolean isTestEmergencyNumber) {
        String action;
        try {
            int actionIntent = isTestEmergencyNumber
                    ? R.string.config_satellite_test_with_esp_replies_intent_action
                    : R.string.config_satellite_emergency_handover_intent_action;
            action = context.getResources().getString(actionIntent);
        } catch (Resources.NotFoundException ex) {
            loge("getSatelliteEmergencyHandoverIntentFilterActionFromOverlayConfig: ex=" + ex);
            action = null;
        }
        if (TextUtils.isEmpty(action) && isMockModemAllowed()) {
            logd("getSatelliteEmergencyHandoverIntentActionFromOverlayConfig: Read "
                    + "config_satellite_emergency_handover_intent_action from device config");
            action = DeviceConfig.getString(DeviceConfig.NAMESPACE_TELEPHONY,
                    "config_satellite_emergency_handover_intent_action", null);
        }
        return action;
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

    @NonNull
    private RegistrationManager.RegistrationCallback getOrCreateImsRegistrationCallback(
            int phoneId) {
        RegistrationManager.RegistrationCallback callback =
                mImsRegistrationCallbacks.get(phoneId);
        if (callback == null) {
            callback = new RegistrationManager.RegistrationCallback() {
                @Override
                public void onRegistered(ImsRegistrationAttributes attributes) {
                    sendMessage(obtainMessage(EVENT_SERVICE_STATE_CHANGED));
                }

                @Override
                public void onUnregistered(ImsReasonInfo info) {
                    sendMessage(obtainMessage(EVENT_SERVICE_STATE_CHANGED));
                }
            };
            mImsRegistrationCallbacks.put(phoneId, callback);
        }
        return callback;
    }

    @NonNull private Bundle createExtraBundleForEventDisplayEmergencyMessage(
            boolean isTestEmergencyNumber) {
        int handoverType = getEmergencyCallToSatelliteHandoverType();
        Pair<String, String> oemSatelliteMessagingApp =
                getOemEnabledSatelliteHandoverAppFromOverlayConfig(mContext);
        String packageName = oemSatelliteMessagingApp.first;
        String className = oemSatelliteMessagingApp.second;
        String action = getSatelliteEmergencyHandoverIntentActionFromOverlayConfig(mContext,
                isTestEmergencyNumber);

        if (handoverType == EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_T911) {
            ComponentName defaultSmsAppComponent = getDefaultSmsApp();
            packageName = defaultSmsAppComponent.getPackageName();
            className = defaultSmsAppComponent.getClassName();
        }
        plogd("EVENT_DISPLAY_EMERGENCY_MESSAGE: handoverType=" + handoverType + ", packageName="
                + packageName + ", className=" + className + ", action=" + action);

        Bundle result = new Bundle();
        result.putInt(EXTRA_EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE, handoverType);
        if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
            result.putParcelable(EXTRA_EMERGENCY_CALL_TO_SATELLITE_LAUNCH_INTENT,
                    createHandoverAppLaunchPendingIntent(
                            handoverType, packageName, className, action));
        }
        return result;
    }

    @NonNull private PendingIntent createHandoverAppLaunchPendingIntent(int handoverType,
            @NonNull String packageName, @NonNull String className, @Nullable String action) {
        Intent intent;
        if (handoverType == EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_T911) {
            String emergencyNumber = "911";
            if (mEmergencyConnection != null) {
                emergencyNumber = mEmergencyConnection.getAddress().getSchemeSpecificPart();
            }
            plogd("emergencyNumber=" + emergencyNumber);

            Uri uri = Uri.parse("smsto:" + emergencyNumber);
            intent = new Intent(Intent.ACTION_SENDTO, uri);
        } else {
            intent = new Intent(action);
            intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
        }
        Bundle activityOptions = ActivityOptions.makeBasic()
                .setPendingIntentCreatorBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle();
        intent.setComponent(new ComponentName(packageName, className));
        return PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE, activityOptions);
    }

    private boolean isEmergencyCallToSatelliteHandoverTypeT911Enforced() {
        return (mSatelliteController.getEnforcedEmergencyCallToSatelliteHandoverType()
                == EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_T911);
    }

    private boolean sendEventDisplayEmergencyMessageForcefully(@NonNull Connection connection) {
        if (mSatelliteController.getEnforcedEmergencyCallToSatelliteHandoverType()
                == INVALID_EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE) {
            return false;
        }

        long delaySeconds = mSatelliteController.getDelayInSendingEventDisplayEmergencyMessage();
        sendMessageDelayed(
                obtainMessage(CMD_SEND_EVENT_DISPLAY_EMERGENCY_MESSAGE_FORCEFULLY, connection),
                delaySeconds * 1000);
        return true;
    }

    private void handleCmdSendEventDisplayEmergencyMessageForcefully(
            @NonNull Connection connection) {
        plogd("Sent EVENT_DISPLAY_EMERGENCY_MESSAGE to Dialer forcefully.");
        mEmergencyConnection = connection;
        Bundle extras = createExtraBundleForEventDisplayEmergencyMessage(
                /* isTestEmergencyNumber= */ true);
        connection.sendConnectionEvent(TelephonyManager.EVENT_DISPLAY_EMERGENCY_MESSAGE, extras);
        mEmergencyConnection = null;
    }

    private boolean isMultiSim() {
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            ploge("isMultiSim: telephonyManager is null");
            return false;
        }
        return telephonyManager.isMultiSimEnabled();
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public int getEmergencyCallToSatelliteHandoverType() {
        if (isSatelliteConnectedViaCarrierWithinHysteresisTime()) {
            int satelliteSubId = mSubIdOfSatelliteConnectedViaCarrierWithinHysteresisTime.get();
            return mSatelliteController.getCarrierRoamingNtnEmergencyCallToSatelliteHandoverType(
                    satelliteSubId);
        } else {
            int satelliteSubId = mSatelliteController.getSelectedSatelliteSubId();
            if (!SatelliteServiceUtils.isNtnOnlySubscriptionId(satelliteSubId)) {
                return mSatelliteController
                    .getCarrierRoamingNtnEmergencyCallToSatelliteHandoverType(satelliteSubId);
            } else {
                return EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_SOS;
            }
        }
    }

    private void requestIsSatelliteAllowedForCurrentLocation() {
        synchronized (mLock) {
            if (mCheckingAccessRestrictionInProgress) {
                plogd("requestIsSatelliteCommunicationAllowedForCurrentLocation was already sent");
                return;
            }
            mCheckingAccessRestrictionInProgress = true;
        }

        OutcomeReceiver<Boolean, SatelliteManager.SatelliteException> callback =
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Boolean result) {
                        plogd("requestIsSatelliteAllowedForCurrentLocation: result=" + result);
                        sendMessage(obtainMessage(
                                EVENT_SATELLITE_ACCESS_RESTRICTION_CHECKING_RESULT, result));
                    }

                    @Override
                    public void onError(SatelliteManager.SatelliteException ex) {
                        plogd("requestIsSatelliteAllowedForCurrentLocation: onError, ex=" + ex);
                        sendMessage(obtainMessage(
                                EVENT_SATELLITE_ACCESS_RESTRICTION_CHECKING_RESULT, false));
                    }
                };
        requestIsSatelliteCommunicationAllowedForCurrentLocation(callback);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected void requestIsSatelliteCommunicationAllowedForCurrentLocation(
            @NonNull OutcomeReceiver<Boolean, SatelliteManager.SatelliteException> callback) {
        SatelliteManager satelliteManager = mContext.getSystemService(SatelliteManager.class);
        satelliteManager.requestIsCommunicationAllowedForCurrentLocation(
                this::post, callback);
    }

    private static boolean isMockModemAllowed() {
        return (SystemProperties.getBoolean(ALLOW_MOCK_MODEM_PROPERTY, false)
                || SystemProperties.getBoolean(BOOT_ALLOW_MOCK_MODEM_PROPERTY, false));
    }

    private boolean isSatelliteSupported() {
        if (mSatelliteController.isSatelliteEmergencyMessagingSupportedViaCarrier()) return true;
        if (mSatelliteController.isSatelliteSupportedViaOem() && isSatelliteViaOemProvisioned()) {
            return true;
        }
        return false;
    }

    private boolean isSatelliteViaOemProvisioned() {
        Boolean provisioned = mSatelliteController.isDeviceProvisioned();
        return (provisioned != null) && provisioned;
    }

    private void updateSatelliteConnectedViaCarrierWithinHysteresisTimeState() {
        Pair<Boolean, Integer> satelliteConnectedState =
                mSatelliteController.isSatelliteConnectedViaCarrierWithinHysteresisTime();
        mIsSatelliteConnectedViaCarrierWithinHysteresisTime.set(satelliteConnectedState.first);
        if (satelliteConnectedState.first) {
            mSubIdOfSatelliteConnectedViaCarrierWithinHysteresisTime.set(
                    satelliteConnectedState.second);
        } else {
            mSubIdOfSatelliteConnectedViaCarrierWithinHysteresisTime.set(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
    }

    private static void logv(@NonNull String log) {
        Rlog.v(TAG, log);
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }

    private void plogd(@NonNull String log) {
        Rlog.d(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.debug(TAG, log);
        }
    }

    private void ploge(@NonNull String log) {
        Rlog.e(TAG, log);
        if (mPersistentLogger != null) {
            mPersistentLogger.error(TAG, log);
        }
    }
}
