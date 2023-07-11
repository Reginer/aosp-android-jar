/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.telephony;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.NetworkTypeBitMask;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.telephony.Rlog;

import java.util.HashMap;
import java.util.Map;

/**
 * This contains Carrier specific logic based on the states/events
 * managed in ServiceStateTracker.
 * {@hide}
 */
public class CarrierServiceStateTracker extends Handler {
    private static final String LOG_TAG = "CSST";
    protected static final int CARRIER_EVENT_BASE = 100;
    protected static final int CARRIER_EVENT_VOICE_REGISTRATION = CARRIER_EVENT_BASE + 1;
    protected static final int CARRIER_EVENT_VOICE_DEREGISTRATION = CARRIER_EVENT_BASE + 2;
    protected static final int CARRIER_EVENT_DATA_REGISTRATION = CARRIER_EVENT_BASE + 3;
    protected static final int CARRIER_EVENT_DATA_DEREGISTRATION = CARRIER_EVENT_BASE + 4;
    protected static final int CARRIER_EVENT_IMS_CAPABILITIES_CHANGED = CARRIER_EVENT_BASE + 5;

    private static final int UNINITIALIZED_DELAY_VALUE = -1;
    private Phone mPhone;
    private ServiceStateTracker mSST;
    private final Map<Integer, NotificationType> mNotificationTypeMap = new HashMap<>();
    private int mPreviousSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    public static final int NOTIFICATION_PREF_NETWORK = 1000;
    public static final int NOTIFICATION_EMERGENCY_NETWORK = 1001;

    @VisibleForTesting
    public static final String EMERGENCY_NOTIFICATION_TAG = "EmergencyNetworkNotification";

    @VisibleForTesting
    public static final String PREF_NETWORK_NOTIFICATION_TAG = "PrefNetworkNotification";

    private long mAllowedNetworkType = -1;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    private TelephonyManager mTelephonyManager;

    /**
     * The listener for allowed network types changed
     */
    @VisibleForTesting
    public class AllowedNetworkTypesListener extends TelephonyCallback
            implements TelephonyCallback.AllowedNetworkTypesListener {
        @Override
        public void onAllowedNetworkTypesChanged(int reason, long newAllowedNetworkType) {
            if (reason != TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER) {
                return;
            }

            if (mAllowedNetworkType != newAllowedNetworkType) {
                mAllowedNetworkType = newAllowedNetworkType;
                handleAllowedNetworkTypeChanged();
            }
        }
    }

    public CarrierServiceStateTracker(Phone phone, ServiceStateTracker sst) {
        this.mPhone = phone;
        this.mSST = sst;
        mTelephonyManager = mPhone.getContext().getSystemService(
                TelephonyManager.class).createForSubscriptionId(mPhone.getSubId());
        phone.getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(
                CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));
        // Listen for subscriber changes
        SubscriptionManager.from(mPhone.getContext()).addOnSubscriptionsChangedListener(
                new OnSubscriptionsChangedListener(this.getLooper()) {
                    @Override
                    public void onSubscriptionsChanged() {
                        int subId = mPhone.getSubId();
                        if (mPreviousSubId != subId) {
                            mPreviousSubId = subId;
                            mTelephonyManager = mTelephonyManager.createForSubscriptionId(
                                    mPhone.getSubId());
                            registerAllowedNetworkTypesListener();
                        }
                    }
                });

        registerNotificationTypes();
        mAllowedNetworkType = RadioAccessFamily.getNetworkTypeFromRaf(
                (int) mPhone.getAllowedNetworkTypes(
                        TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
        mAllowedNetworkTypesListener = new AllowedNetworkTypesListener();
        registerAllowedNetworkTypesListener();
    }

    /**
     * Return preferred network mode listener
     */
    @VisibleForTesting
    public AllowedNetworkTypesListener getAllowedNetworkTypesChangedListener() {
        return mAllowedNetworkTypesListener;
    }

    private void registerAllowedNetworkTypesListener() {
        int subId = mPhone.getSubId();
        unregisterAllowedNetworkTypesListener();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            if (mTelephonyManager != null) {
                mTelephonyManager.registerTelephonyCallback(new HandlerExecutor(this),
                        mAllowedNetworkTypesListener);
            }
        }
    }

    private void unregisterAllowedNetworkTypesListener() {
        mTelephonyManager.unregisterTelephonyCallback(mAllowedNetworkTypesListener);
    }

    /**
     * Returns mNotificationTypeMap
     */
    @VisibleForTesting
    public Map<Integer, NotificationType> getNotificationTypeMap() {
        return mNotificationTypeMap;
    }

    private void registerNotificationTypes() {
        mNotificationTypeMap.put(NOTIFICATION_PREF_NETWORK,
                new PrefNetworkNotification(NOTIFICATION_PREF_NETWORK));
        mNotificationTypeMap.put(NOTIFICATION_EMERGENCY_NETWORK,
                new EmergencyNetworkNotification(NOTIFICATION_EMERGENCY_NETWORK));
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case CARRIER_EVENT_VOICE_REGISTRATION:
            case CARRIER_EVENT_DATA_REGISTRATION:
            case CARRIER_EVENT_VOICE_DEREGISTRATION:
            case CARRIER_EVENT_DATA_DEREGISTRATION:
                handleConfigChanges();
                break;
            case CARRIER_EVENT_IMS_CAPABILITIES_CHANGED:
                handleImsCapabilitiesChanged();
                break;
            case NOTIFICATION_EMERGENCY_NETWORK:
            case NOTIFICATION_PREF_NETWORK:
                Rlog.d(LOG_TAG, "sending notification after delay: " + msg.what);
                NotificationType notificationType = mNotificationTypeMap.get(msg.what);
                if (notificationType != null) {
                    sendNotification(notificationType);
                }
                break;
        }
    }

    private boolean isPhoneStillRegistered() {
        if (mSST.mSS == null) {
            return true; //something has gone wrong, return true and not show the notification.
        }
        return (mSST.mSS.getState() == ServiceState.STATE_IN_SERVICE
                || mSST.mSS.getDataRegistrationState() == ServiceState.STATE_IN_SERVICE);
    }

    private boolean isPhoneRegisteredForWifiCalling() {
        Rlog.d(LOG_TAG, "isPhoneRegisteredForWifiCalling: " + mPhone.isWifiCallingEnabled());
        return mPhone.isWifiCallingEnabled();
    }

    /**
     * Returns true if the radio is off or in Airplane Mode else returns false.
     */
    @VisibleForTesting
    public boolean isRadioOffOrAirplaneMode() {
        Context context = mPhone.getContext();
        int airplaneMode = -1;
        try {
            airplaneMode = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0);
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Unable to get AIRPLACE_MODE_ON.");
            return true;
        }
        return (!mSST.isRadioOn() || (airplaneMode != 0));
    }

    /**
     * Returns true if the preferred network is set to 'Global'.
     */
    private boolean isGlobalMode() {
        int preferredNetworkSetting = -1;
        try {
            preferredNetworkSetting = PhoneFactory.calculatePreferredNetworkType(
                    mPhone.getPhoneId());
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Unable to get PREFERRED_NETWORK_MODE.");
            return true;
        }

        if (isNrSupported()) {
            return (preferredNetworkSetting
                    == RadioAccessFamily.getRafFromNetworkType(
                    RILConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA));
        } else {
            return (preferredNetworkSetting == RadioAccessFamily.getRafFromNetworkType(
                    RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
        }
    }

    private boolean isNrSupported() {
        Context context = mPhone.getContext();
        TelephonyManager tm = ((TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE)).createForSubscriptionId(mPhone.getSubId());

        boolean isCarrierConfigEnabled = isCarrierConfigEnableNr(context);
        boolean isRadioAccessFamilySupported = checkSupportedBitmask(
                tm.getSupportedRadioAccessFamily(), TelephonyManager.NETWORK_TYPE_BITMASK_NR);
        boolean isNrNetworkTypeAllowed = checkSupportedBitmask(
                tm.getAllowedNetworkTypesForReason(
                        TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER),
                TelephonyManager.NETWORK_TYPE_BITMASK_NR);

        Rlog.i(LOG_TAG, "isNrSupported: " + " carrierConfigEnabled: " + isCarrierConfigEnabled
                + ", AccessFamilySupported: " + isRadioAccessFamilySupported
                + ", isNrNetworkTypeAllowed: " + isNrNetworkTypeAllowed);

        return (isCarrierConfigEnabled && isRadioAccessFamilySupported && isNrNetworkTypeAllowed);
    }

    private boolean isCarrierConfigEnableNr(Context context) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager)
                context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (carrierConfigManager == null) {
            Rlog.e(LOG_TAG, "isCarrierConfigEnableNr: CarrierConfigManager is null");
            return false;
        }
        PersistableBundle config = carrierConfigManager.getConfigForSubId(mPhone.getSubId());
        if (config == null) {
            Rlog.e(LOG_TAG, "isCarrierConfigEnableNr: Cannot get config " + mPhone.getSubId());
            return false;
        }
        int[] nrAvailabilities = config.getIntArray(
                CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        return !ArrayUtils.isEmpty(nrAvailabilities);
    }

    private boolean checkSupportedBitmask(@NetworkTypeBitMask long supportedBitmask,
            @NetworkTypeBitMask long targetBitmask) {
        return (targetBitmask & supportedBitmask) == targetBitmask;
    }

    private void handleConfigChanges() {
        for (Map.Entry<Integer, NotificationType> entry : mNotificationTypeMap.entrySet()) {
            NotificationType notificationType = entry.getValue();
            evaluateSendingMessageOrCancelNotification(notificationType);
        }
    }

    private void handleAllowedNetworkTypeChanged() {
        NotificationType notificationType = mNotificationTypeMap.get(NOTIFICATION_PREF_NETWORK);
        if (notificationType != null) {
            evaluateSendingMessageOrCancelNotification(notificationType);
        }
    }

    private void handleImsCapabilitiesChanged() {
        NotificationType notificationType = mNotificationTypeMap
                .get(NOTIFICATION_EMERGENCY_NETWORK);
        if (notificationType != null) {
            evaluateSendingMessageOrCancelNotification(notificationType);
        }
    }

    private void evaluateSendingMessageOrCancelNotification(NotificationType notificationType) {
        if (evaluateSendingMessage(notificationType)) {
            Message notificationMsg = obtainMessage(notificationType.getTypeId(), null);
            Rlog.i(LOG_TAG, "starting timer for notifications." + notificationType.getTypeId());
            sendMessageDelayed(notificationMsg, getDelay(notificationType));
        } else {
            cancelNotification(notificationType);
            Rlog.i(LOG_TAG, "canceling notifications: " + notificationType.getTypeId());
        }
    }

    /**
     * This method adds a level of indirection, and was created so we can unit the class.
     **/
    @VisibleForTesting
    public boolean evaluateSendingMessage(NotificationType notificationType) {
        return notificationType.sendMessage();
    }

    /**
     * This method adds a level of indirection, and was created so we can unit the class.
     **/
    @VisibleForTesting
    public int getDelay(NotificationType notificationType) {
        return notificationType.getDelay();
    }

    /**
     * This method adds a level of indirection, and was created so we can unit the class.
     **/
    @VisibleForTesting
    public Notification.Builder getNotificationBuilder(NotificationType notificationType) {
        return notificationType.getNotificationBuilder();
    }

    /**
     * This method adds a level of indirection, and was created so we can unit the class.
     **/
    @VisibleForTesting
    public NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager)
                    context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = carrierConfigManager.getConfigForSubId(mPhone.getSubId());

            for (Map.Entry<Integer, NotificationType> entry : mNotificationTypeMap.entrySet()) {
                NotificationType notificationType = entry.getValue();
                notificationType.setDelay(b);
            }
            handleConfigChanges();
        }
    };

    /**
     * Post a notification to the NotificationManager for changing network type.
     */
    @VisibleForTesting
    public void sendNotification(NotificationType notificationType) {
        if (!evaluateSendingMessage(notificationType)) {
            return;
        }

        Context context = mPhone.getContext();
        Notification.Builder builder = getNotificationBuilder(notificationType);
        // set some common attributes
        builder.setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSmallIcon(com.android.internal.R.drawable.stat_sys_warning)
                .setColor(context.getResources().getColor(
                       com.android.internal.R.color.system_notification_accent_color));
        getNotificationManager(context).notify(notificationType.getNotificationTag(),
                notificationType.getNotificationId(), builder.build());
    }

    /**
     * Cancel notifications if a registration is pending or has been sent.
     **/
    public void cancelNotification(NotificationType notificationType) {
        Context context = mPhone.getContext();
        removeMessages(notificationType.getTypeId());
        getNotificationManager(context).cancel(
                notificationType.getNotificationTag(), notificationType.getNotificationId());
    }

    /**
     * Dispose the CarrierServiceStateTracker.
     */
    public void dispose() {
        unregisterAllowedNetworkTypesListener();
    }

    /**
     * Class that defines the different types of notifications.
     */
    public interface NotificationType {

        /**
         * decides if the message should be sent, Returns boolean
         **/
        boolean sendMessage();

        /**
         * returns the interval by which the message is delayed.
         **/
        int getDelay();

        /** sets the interval by which the message is delayed.
         * @param bundle PersistableBundle
        **/
        void setDelay(PersistableBundle bundle);

        /**
         * returns notification type id.
         **/
        int getTypeId();

        /**
         * returns notification id.
         **/
        int getNotificationId();

        /**
         * returns notification tag.
         **/
        String getNotificationTag();

        /**
         * returns the notification builder, for the notification to be displayed.
         **/
        Notification.Builder getNotificationBuilder();
    }

    /**
     * Class that defines the network notification, which is shown when the phone cannot camp on
     * a network, and has 'preferred mode' set to global.
     */
    public class PrefNetworkNotification implements NotificationType {

        private final int mTypeId;
        private int mDelay = UNINITIALIZED_DELAY_VALUE;

        PrefNetworkNotification(int typeId) {
            this.mTypeId = typeId;
        }

        /** sets the interval by which the message is delayed.
         * @param bundle PersistableBundle
         **/
        public void setDelay(PersistableBundle bundle) {
            if (bundle == null) {
                Rlog.e(LOG_TAG, "bundle is null");
                return;
            }
            this.mDelay = bundle.getInt(
                    CarrierConfigManager.KEY_PREF_NETWORK_NOTIFICATION_DELAY_INT);
            Rlog.i(LOG_TAG, "reading time to delay notification pref network: " + mDelay);
        }

        public int getDelay() {
            return mDelay;
        }

        public int getTypeId() {
            return mTypeId;
        }

        public int getNotificationId() {
            return mPhone.getSubId();
        }

        public String getNotificationTag() {
            return PREF_NETWORK_NOTIFICATION_TAG;
        }

        /**
         * Contains logic on sending notifications.
         */
        public boolean sendMessage() {
            Rlog.i(LOG_TAG, "PrefNetworkNotification: sendMessage() w/values: "
                    + "," + isPhoneStillRegistered() + "," + mDelay + "," + isGlobalMode()
                    + "," + mSST.isRadioOn());
            if (mDelay == UNINITIALIZED_DELAY_VALUE || isPhoneStillRegistered() || isGlobalMode()
                    || isRadioOffOrAirplaneMode()) {
                return false;
            }
            return true;
        }

        /**
         * Builds a partial notificaiton builder, and returns it.
         */
        public Notification.Builder getNotificationBuilder() {
            Context context = mPhone.getContext();
            Intent notificationIntent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
            notificationIntent.putExtra("expandable", true);
            PendingIntent settingsIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
            Resources res = SubscriptionManager.getResourcesForSubId(context, mPhone.getSubId());
            CharSequence title = res.getText(
                    com.android.internal.R.string.NetworkPreferenceSwitchTitle);
            CharSequence details = res.getText(
                    com.android.internal.R.string.NetworkPreferenceSwitchSummary);
            return new Notification.Builder(context)
                    .setContentTitle(title)
                    .setStyle(new Notification.BigTextStyle().bigText(details))
                    .setContentText(details)
                    .setChannelId(NotificationChannelController.CHANNEL_ID_ALERT)
                    .setContentIntent(settingsIntent);
        }
    }

    /**
     * Class that defines the emergency notification, which is shown when Wi-Fi Calling is
     * available.
     */
    public class EmergencyNetworkNotification implements NotificationType {

        private final int mTypeId;
        private int mDelay = UNINITIALIZED_DELAY_VALUE;

        EmergencyNetworkNotification(int typeId) {
            this.mTypeId = typeId;
        }

        /** sets the interval by which the message is delayed.
         * @param bundle PersistableBundle
         **/
        public void setDelay(PersistableBundle bundle) {
            if (bundle == null) {
                Rlog.e(LOG_TAG, "bundle is null");
                return;
            }
            this.mDelay = bundle.getInt(
                    CarrierConfigManager.KEY_EMERGENCY_NOTIFICATION_DELAY_INT);
            Rlog.i(LOG_TAG, "reading time to delay notification emergency: " + mDelay);
        }

        public int getDelay() {
            return mDelay;
        }

        public int getTypeId() {
            return mTypeId;
        }

        public int getNotificationId() {
            return mPhone.getSubId();
        }

        public String getNotificationTag() {
            return EMERGENCY_NOTIFICATION_TAG;
        }

        /**
         * Contains logic on sending notifications,
         */
        public boolean sendMessage() {
            Rlog.i(LOG_TAG, "EmergencyNetworkNotification: sendMessage() w/values: "
                    + "," + mDelay + "," + isPhoneRegisteredForWifiCalling() + ","
                    + mSST.isRadioOn());
            if (mDelay == UNINITIALIZED_DELAY_VALUE || !isPhoneRegisteredForWifiCalling()) {
                return false;
            }
            return true;
        }

        /**
         * Builds a partial notificaiton builder, and returns it.
         */
        public Notification.Builder getNotificationBuilder() {
            Context context = mPhone.getContext();
            Resources res = SubscriptionManager.getResourcesForSubId(context, mPhone.getSubId());
            CharSequence title = res.getText(
                    com.android.internal.R.string.EmergencyCallWarningTitle);
            CharSequence details = res.getText(
                    com.android.internal.R.string.EmergencyCallWarningSummary);
            return new Notification.Builder(context)
                    .setContentTitle(title)
                    .setStyle(new Notification.BigTextStyle().bigText(details))
                    .setContentText(details)
                    .setFlag(Notification.FLAG_NO_CLEAR, true)
                    .setChannelId(NotificationChannelController.CHANNEL_ID_WFC);
        }
    }
}
