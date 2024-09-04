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

import static android.telephony.ServiceState.STATE_EMERGENCY_ONLY;
import static android.telephony.ServiceState.STATE_IN_SERVICE;
import static android.telephony.ServiceState.STATE_OUT_OF_SERVICE;
import static android.telephony.TelephonyManager.EXTRA_EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE;
import static android.telephony.TelephonyManager.EXTRA_EMERGENCY_CALL_TO_SATELLITE_LAUNCH_INTENT;
import static android.telephony.satellite.SatelliteManager.EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_SOS;
import static android.telephony.satellite.SatelliteManager.EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_T911;

import static com.android.internal.telephony.satellite.SatelliteController.INVALID_EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE;

import android.annotation.NonNull;
import android.annotation.Nullable;
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
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.RegistrationManager;
import android.telephony.satellite.ISatelliteProvisionStateCallback;
import android.telephony.satellite.SatelliteManager;
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
import com.android.internal.telephony.metrics.SatelliteStats;

import java.util.concurrent.atomic.AtomicBoolean;


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
    private ImsManager mImsManager;

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
    private final AtomicBoolean mIsSatelliteConnectedViaCarrierWithinHysteresisTime =
            new AtomicBoolean(false);
    @GuardedBy("mLock")
    private boolean mIsTimerTimedOut = false;
    protected int mCountOfTimerStarted = 0;
    private final Object mLock = new Object();

    /**
     * Create an instance of SatelliteSOSMessageRecommender.
     *
     * @param context The Context for the SatelliteSOSMessageRecommender.
     * @param looper The looper used with the handler of this class.
     */
    public SatelliteSOSMessageRecommender(@NonNull Context context, @NonNull Looper looper) {
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
            @NonNull SatelliteController satelliteController, ImsManager imsManager) {
        super(looper);
        mContext = context;
        mSatelliteController = satelliteController;
        mImsManager = imsManager;
        mOemEnabledTimeoutMillis =
                getOemEnabledEmergencyCallWaitForConnectionTimeoutMillis(context);
        mISatelliteProvisionStateCallback = new ISatelliteProvisionStateCallback.Stub() {
            @Override
            public void onSatelliteProvisionStateChanged(boolean provisioned) {
                logd("onSatelliteProvisionStateChanged: provisioned=" + provisioned);
                sendMessage(obtainMessage(EVENT_SATELLITE_PROVISIONED_STATE_CHANGED, provisioned));
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
                logd("handleMessage: unexpected message code: " + msg.what);
                break;
        }
    }

    /**
     * Inform SatelliteSOSMessageRecommender that an emergency call has just started.
     *
     * @param connection The connection created by TelephonyConnectionService for the emergency
     *                   call.
     */
    public void onEmergencyCallStarted(@NonNull Connection connection) {
        if (!mSatelliteController.isSatelliteSupportedViaOem()
                && !mSatelliteController.isSatelliteEmergencyMessagingSupportedViaCarrier()) {
            logd("onEmergencyCallStarted: satellite is not supported");
            return;
        }

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
        mIsSatelliteConnectedViaCarrierWithinHysteresisTime.set(
                mSatelliteController.isSatelliteConnectedViaCarrierWithinHysteresisTime());
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
        logd("callId=" + callId + ", state=" + state);
        if (!mSatelliteController.isSatelliteSupportedViaOem()
                && !mSatelliteController.isSatelliteEmergencyMessagingSupportedViaCarrier()) {
            logd("onEmergencyCallConnectionStateChanged: satellite is not supported");
            return;
        }
        Pair<String, Integer> argument = new Pair<>(callId, state);
        sendMessage(obtainMessage(EVENT_EMERGENCY_CALL_CONNECTION_STATE_CHANGED, argument));
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected ComponentName getDefaultSmsApp() {
        return SmsApplication.getDefaultSendToApplication(mContext, false);
    }

    private void handleEmergencyCallStartedEvent(@NonNull Connection connection) {
        if (sendEventDisplayEmergencyMessageForcefully(connection)) {
            return;
        }

        selectEmergencyCallWaitForConnectionTimeoutDuration();
        if (mEmergencyConnection == null) {
            handleStateChangedEventForHysteresisTimer();
            registerForInterestedStateChangedEvents();
        }
        mEmergencyConnection = connection;
        synchronized (mLock) {
            mCheckingAccessRestrictionInProgress = false;
            mIsSatelliteAllowedForCurrentLocation = false;
        }
    }

    private void handleSatelliteProvisionStateChangedEvent(boolean provisioned) {
        if (!provisioned) {
            cleanUpResources();
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
                loge("No emergency call is ongoing...");
                return;
            }

            if (!mIsTimerTimedOut || mCheckingAccessRestrictionInProgress) {
                logd("mIsTimerTimedOut=" + mIsTimerTimedOut
                        + ", mCheckingAccessRestrictionInProgress="
                        + mCheckingAccessRestrictionInProgress);
                return;
            }

            /*
             * The device might be connected to satellite after the emergency call started. Thus, we
             * need to do this check again so that we will have higher chance of sending the event
             * EVENT_DISPLAY_EMERGENCY_MESSAGE to Dialer.
             */
            updateSatelliteViaCarrierAvailability();

            boolean isDialerNotified = false;
            if (!isCellularAvailable()
                    && isSatelliteAllowed()
                    && (isSatelliteViaOemAvailable() || isSatelliteViaCarrierAvailable())
                    && shouldTrackCall(mEmergencyConnection.getState())) {
                logd("handleTimeoutEvent: Sent EVENT_DISPLAY_EMERGENCY_MESSAGE to Dialer");
                Bundle extras = createExtraBundleForEventDisplayEmergencyMessage();
                mEmergencyConnection.sendConnectionEvent(
                        TelephonyManager.EVENT_DISPLAY_EMERGENCY_MESSAGE, extras);
                isDialerNotified = true;

            }
            logd("handleTimeoutEvent: isImsRegistered=" + isImsRegistered()
                    + ", isCellularAvailable=" + isCellularAvailable()
                    + ", isSatelliteAllowed=" + isSatelliteAllowed()
                    + ", shouldTrackCall=" + shouldTrackCall(mEmergencyConnection.getState()));
            reportEsosRecommenderDecision(isDialerNotified);
            cleanUpResources();
        }
    }

    private boolean isSatelliteAllowed() {
        synchronized (mLock) {
            if (isSatelliteViaCarrierAvailable()) return true;
            return mIsSatelliteAllowedForCurrentLocation;
        }
    }

    private void updateSatelliteViaCarrierAvailability() {
        if (!mIsSatelliteConnectedViaCarrierWithinHysteresisTime.get()) {
            mIsSatelliteConnectedViaCarrierWithinHysteresisTime.set(
                    mSatelliteController.isSatelliteConnectedViaCarrierWithinHysteresisTime());
        }
    }

    /**
     * Check if satellite is available via OEM
     * @return {@code true} if satellite is provisioned via OEM else return {@code false}
     */
    @VisibleForTesting
    public boolean isSatelliteViaOemAvailable() {
        Boolean satelliteProvisioned = mSatelliteController.isSatelliteViaOemProvisioned();
        return satelliteProvisioned != null ? satelliteProvisioned : false;
    }

    private boolean isSatelliteViaCarrierAvailable() {
        return mIsSatelliteConnectedViaCarrierWithinHysteresisTime.get();
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
            /*
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
        } else {
            // Location service will enter emergency mode only when connection state changes to
            // STATE_DIALING
            if (state == Connection.STATE_DIALING
                    && mSatelliteController.isSatelliteSupportedViaOem()) {
                requestIsSatelliteAllowedForCurrentLocation();
            }
        }
    }

    private void reportEsosRecommenderDecision(boolean isDialerNotified) {
        SatelliteStats.getInstance().onSatelliteSosMessageRecommender(
                new SatelliteStats.SatelliteSosMessageRecommenderParams.Builder()
                        .setDisplaySosMessageSent(isDialerNotified)
                        .setCountOfTimerStarted(mCountOfTimerStarted)
                        .setImsRegistered(isImsRegistered())
                        .setCellularServiceState(getBestCellularServiceState())
                        .setIsMultiSim(isMultiSim())
                        .setRecommendingHandoverType(getEmergencyCallToSatelliteHandoverType())
                        .setIsSatelliteAllowedInCurrentLocation(isSatelliteAllowed())
                        .build());
    }

    private void cleanUpResources() {
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
        }
    }

    private void registerForInterestedStateChangedEvents() {
        mSatelliteController.registerForSatelliteProvisionStateChanged(
                SubscriptionManager.DEFAULT_SUBSCRIPTION_ID, mISatelliteProvisionStateCallback);
        for (Phone phone : PhoneFactory.getPhones()) {
            phone.registerForServiceStateChanged(
                    this, EVENT_SERVICE_STATE_CHANGED, null);
            registerForImsRegistrationStateChanged(phone);
        }
    }

    private void registerForImsRegistrationStateChanged(@NonNull Phone phone) {
        ImsManager imsManager = (mImsManager != null) ? mImsManager : ImsManager.getInstance(
                phone.getContext(), phone.getPhoneId());
        try {
            imsManager.addRegistrationCallback(
                    getOrCreateImsRegistrationCallback(phone.getPhoneId()), this::post);
        } catch (ImsException ex) {
            loge("registerForImsRegistrationStateChanged: ex=" + ex);
        }
    }

    private void unregisterForInterestedStateChangedEvents() {
        mSatelliteController.unregisterForSatelliteProvisionStateChanged(
                SubscriptionManager.DEFAULT_SUBSCRIPTION_ID, mISatelliteProvisionStateCallback);
        for (Phone phone : PhoneFactory.getPhones()) {
            phone.unregisterForServiceStateChanged(this);
            unregisterForImsRegistrationStateChanged(phone);
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
            loge("Phone ID=" + phone.getPhoneId() + " was not registered with ImsManager");
        }
    }

    private boolean isCellularAvailable() {
        for (Phone phone : PhoneFactory.getPhones()) {
            ServiceState serviceState = phone.getServiceState();
            if (serviceState != null) {
                int state = serviceState.getState();
                if ((state == STATE_IN_SERVICE || state == STATE_EMERGENCY_ONLY
                        || serviceState.isEmergencyOnly())
                        && !serviceState.isUsingNonTerrestrialNetwork()) {
                    logv("isCellularAvailable true");
                    return true;
                }
            }
        }
        logv("isCellularAvailable false");
        return false;
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
        if (!isCellularAvailable()) {
            startTimer();
        } else {
            logv("handleStateChangedEventForHysteresisTimer stopTimer");
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
            logd("startTimer mCountOfTimerStarted=" + mCountOfTimerStarted);
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
        if (mSatelliteController.isSatelliteEmergencyMessagingSupportedViaCarrier()) {
            mTimeoutMillis =
                    mSatelliteController.getCarrierEmergencyCallWaitForConnectionTimeoutMillis();
        } else {
            mTimeoutMillis = mOemEnabledTimeoutMillis;
        }
        logd("mTimeoutMillis = " + mTimeoutMillis);
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
            @NonNull Context context) {
        String action;
        try {
            action = context.getResources().getString(
                    R.string.config_satellite_emergency_handover_intent_action);
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

    @NonNull private Bundle createExtraBundleForEventDisplayEmergencyMessage() {
        int handoverType = EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_SOS;
        Pair<String, String> oemSatelliteMessagingApp =
                getOemEnabledSatelliteHandoverAppFromOverlayConfig(mContext);
        String packageName = oemSatelliteMessagingApp.first;
        String className = oemSatelliteMessagingApp.second;
        String action = getSatelliteEmergencyHandoverIntentActionFromOverlayConfig(mContext);

        if (isSatelliteViaCarrierAvailable()
                || isEmergencyCallToSatelliteHandoverTypeT911Enforced()) {
            ComponentName defaultSmsAppComponent = getDefaultSmsApp();
            handoverType = EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_T911;
            packageName = defaultSmsAppComponent.getPackageName();
            className = defaultSmsAppComponent.getClassName();
        }
        logd("EVENT_DISPLAY_EMERGENCY_MESSAGE: handoverType=" + handoverType + ", packageName="
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
            logd("emergencyNumber=" + emergencyNumber);

            Uri uri = Uri.parse("smsto:" + emergencyNumber);
            intent = new Intent(Intent.ACTION_SENDTO, uri);
        } else {
            intent = new Intent(action);
        }
        intent.setComponent(new ComponentName(packageName, className));
        return PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
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
        logd("Sent EVENT_DISPLAY_EMERGENCY_MESSAGE to Dialer forcefully.");
        mEmergencyConnection = connection;
        Bundle extras = createExtraBundleForEventDisplayEmergencyMessage();
        connection.sendConnectionEvent(TelephonyManager.EVENT_DISPLAY_EMERGENCY_MESSAGE, extras);
        mEmergencyConnection = null;
    }

    private boolean isMultiSim() {
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            loge("isMultiSim: telephonyManager is null");
            return false;
        }
        return telephonyManager.isMultiSimEnabled();
    }

    private int getEmergencyCallToSatelliteHandoverType() {
        if (isSatelliteViaCarrierAvailable()) {
            return EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_T911;
        } else {
            return EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE_SOS;
        }
    }

    private void requestIsSatelliteAllowedForCurrentLocation() {
        synchronized (mLock) {
            if (mCheckingAccessRestrictionInProgress) {
                logd("requestIsSatelliteCommunicationAllowedForCurrentLocation was already sent");
                return;
            }
            mCheckingAccessRestrictionInProgress = true;
        }

        OutcomeReceiver<Boolean, SatelliteManager.SatelliteException> callback =
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Boolean result) {
                        logd("requestIsSatelliteAllowedForCurrentLocation: result=" + result);
                        sendMessage(obtainMessage(
                                EVENT_SATELLITE_ACCESS_RESTRICTION_CHECKING_RESULT, result));
                    }

                    @Override
                    public void onError(SatelliteManager.SatelliteException ex) {
                        logd("requestIsSatelliteAllowedForCurrentLocation: onError, ex=" + ex);
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

    private static void logv(@NonNull String log) {
        Rlog.v(TAG, log);
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }
}
