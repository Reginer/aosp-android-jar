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

import android.annotation.NonNull;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Message;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
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
import com.android.internal.telephony.flags.FeatureFlags;
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
    public static final String ACTION_NEVER_ASK_AGAIN =
            "com.android.internal.telephony.action.SILENCE_WIFI_CALLING_NOTIFICATION";
    public final NotificationActionReceiver mActionReceiver = new NotificationActionReceiver();

    @VisibleForTesting
    public static final String EMERGENCY_NOTIFICATION_TAG = "EmergencyNetworkNotification";

    @VisibleForTesting
    public static final String PREF_NETWORK_NOTIFICATION_TAG = "PrefNetworkNotification";

    private long mAllowedNetworkType = -1;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    private TelephonyManager mTelephonyManager;
    @NonNull private final FeatureFlags mFeatureFlags;

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

    public CarrierServiceStateTracker(Phone phone, ServiceStateTracker sst,
            @NonNull FeatureFlags featureFlags) {
        mFeatureFlags = featureFlags;
        this.mPhone = phone;
        this.mSST = sst;
        mTelephonyManager = mPhone.getContext().getSystemService(
                TelephonyManager.class).createForSubscriptionId(mPhone.getSubId());
        CarrierConfigManager ccm = mPhone.getContext().getSystemService(CarrierConfigManager.class);
        if (ccm != null) {
            ccm.registerCarrierConfigChangeListener(
                mPhone.getContext().getMainExecutor(),
                (slotIndex, subId, carrierId, specificCarrierId) -> {
                    if (slotIndex != mPhone.getPhoneId()) return;

                    Rlog.d(LOG_TAG, "onCarrierConfigChanged: slotIndex=" + slotIndex
                            + ", subId=" + subId + ", carrierId=" + carrierId);

                    // Only get carrier configs used for EmergencyNetworkNotification
                    // and PrefNetworkNotification
                    PersistableBundle b =
                            CarrierConfigManager.getCarrierConfigSubset(
                                    mPhone.getContext(),
                                    mPhone.getSubId(),
                                    CarrierConfigManager.KEY_EMERGENCY_NOTIFICATION_DELAY_INT,
                                    CarrierConfigManager.KEY_PREF_NETWORK_NOTIFICATION_DELAY_INT,
                                    CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL);
                    if (b.isEmpty()) return;

                    for (Map.Entry<Integer, NotificationType> entry :
                            mNotificationTypeMap.entrySet()) {
                        NotificationType notificationType = entry.getValue();
                        notificationType.setDelay(b);
                        notificationType.setEnabled(b);
                    }
                    handleConfigChanges();
                });
        }

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

        if (!mPhone.getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_WATCH)) {
            registerNotificationTypes();
        }

        mAllowedNetworkType = RadioAccessFamily.getNetworkTypeFromRaf(
                (int) mPhone.getAllowedNetworkTypes(
                        TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
        mAllowedNetworkTypesListener = new AllowedNetworkTypesListener();
        registerAllowedNetworkTypesListener();

        if (mFeatureFlags.stopSpammingEmergencyNotification()) {
            // register a receiver for notification actions
            mPhone.getContext().registerReceiver(
                    mActionReceiver,
                    new IntentFilter(ACTION_NEVER_ASK_AGAIN),
                    Context.RECEIVER_NOT_EXPORTED);
        }
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

        boolean isCarrierConfigEnabled = isCarrierConfigEnableNr();
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

    private boolean isCarrierConfigEnableNr() {
        PersistableBundle config =
                CarrierConfigManager.getCarrierConfigSubset(
                        mPhone.getContext(),
                        mPhone.getSubId(),
                        CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        if (config.isEmpty()) {
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

    /**
     * Post a notification to the NotificationManager for changing network type.
     */
    @VisibleForTesting
    public void sendNotification(NotificationType notificationType) {
        Context context = mPhone.getContext();

        if (!evaluateSendingMessage(notificationType)) {
            return;
        }

        if (mFeatureFlags.stopSpammingEmergencyNotification()
                && shouldSilenceEmrgNetNotif(notificationType, context)) {
            Rlog.i(LOG_TAG, "sendNotification: silencing NOTIFICATION_EMERGENCY_NETWORK");
            return;
        }

        Notification.Builder builder = getNotificationBuilder(notificationType);
        // set some common attributes
        builder.setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setSmallIcon(com.android.internal.R.drawable.stat_sys_warning)
                .setColor(context.getResources().getColor(
                       com.android.internal.R.color.system_notification_accent_color));
        getNotificationManager(context).notify(notificationType.getNotificationTag(),
                notificationType.getNotificationId(), builder.build());
    }

    /**
     * This helper checks if the user has set a flag to silence the notification permanently
     */
    private boolean shouldSilenceEmrgNetNotif(NotificationType notificationType, Context context) {
        return notificationType.getTypeId() == NOTIFICATION_EMERGENCY_NETWORK
                && PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(ACTION_NEVER_ASK_AGAIN, false);
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
         * Checks whether this Notification is enabled.
         * @return {@code true} if this Notification is enabled, false otherwise
         */
        boolean isEnabled();

        /**
         * Sets whether this Notification is enabled. If disabled, it will not build notification.
         * @param bundle PersistableBundle
         */
        void setEnabled(PersistableBundle bundle);

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
        private boolean mEnabled = false;

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

        /**
         * Checks whether this Notification is enabled.
         * @return {@code true} if this Notification is enabled, false otherwise
         */
        public boolean isEnabled() {
            return mEnabled;
        }

        /**
         * Sets whether this Notification is enabled. If disabled, it will not build notification.
         * @param bundle PersistableBundle
         */
        public void setEnabled(PersistableBundle bundle) {
            if (bundle == null) {
                Rlog.e(LOG_TAG, "bundle is null");
                return;
            }
            mEnabled = !bundle.getBoolean(
                    CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL);
            Rlog.i(LOG_TAG, "reading enabled notification pref network: " + mEnabled);
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
                    + "," + mEnabled + "," + isPhoneStillRegistered() + "," + mDelay
                    + "," + isGlobalMode() + "," + mSST.isRadioOn());
            if (!mEnabled || mDelay == UNINITIALIZED_DELAY_VALUE || isPhoneStillRegistered()
                    || isGlobalMode() || isRadioOffOrAirplaneMode()) {
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

        /**
         * Checks whether this Notification is enabled.
         * @return {@code true} if this Notification is enabled, false otherwise
         */
        public boolean isEnabled() {
            return true;
        }

        /**
         * Sets whether this Notification is enabled. If disabled, it will not build notification.
         * @param bundle PersistableBundle
         */
        public void setEnabled(PersistableBundle bundle) {
            // always allowed. There is no config to hide notifications.
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
            if (mFeatureFlags.stopSpammingEmergencyNotification()) {
                return new Notification.Builder(context)
                        .setContentTitle(title)
                        .setStyle(new Notification.BigTextStyle().bigText(details))
                        .setContentText(details)
                        .setOngoing(true)
                        .setActions(createDoNotShowAgainAction(context))
                        .setChannelId(NotificationChannelController.CHANNEL_ID_WFC);
            } else {
                return new Notification.Builder(context)
                        .setContentTitle(title)
                        .setStyle(new Notification.BigTextStyle().bigText(details))
                        .setContentText(details)
                        .setOngoing(true)
                        .setChannelId(NotificationChannelController.CHANNEL_ID_WFC);
            }
        }

        /**
         * add a button to the notification that has a broadcast intent embedded to silence the
         * notification
         */
        private Notification.Action createDoNotShowAgainAction(Context c) {
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    c,
                    0,
                    new Intent(ACTION_NEVER_ASK_AGAIN),
                    PendingIntent.FLAG_IMMUTABLE);
            CharSequence text = "Do Not Ask Again";
            if (c != null && mFeatureFlags.dynamicDoNotAskAgainText()) {
                text = c.getText(com.android.internal.R.string.emergency_calling_do_not_show_again);
            }
            return new Notification.Action.Builder(null, text, pendingIntent).build();
        }
    }

    /**
     * This receiver listens to notification actions and can be utilized to do things like silence
     * a notification that is spammy.
     */
    public class NotificationActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_NEVER_ASK_AGAIN)) {
                Rlog.i(LOG_TAG, "NotificationActionReceiver: ACTION_NEVER_ASK_AGAIN");
                dismissEmergencyCallingNotification();
                // insert a key to silence future notifications
                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putBoolean(ACTION_NEVER_ASK_AGAIN, true);
                editor.apply();
                // Note: If another action is added, unregistering here should be removed. However,
                // since there is no longer a reason to broadcasts, cleanup mActionReceiver.
                context.unregisterReceiver(mActionReceiver);
            }
        }

        /**
         * Dismiss the notification when the "Do Not Ask Again" button is clicked
         */
        private void dismissEmergencyCallingNotification() {
            if (!mFeatureFlags.stopSpammingEmergencyNotification()) {
                return;
            }
            try {
                NotificationType t = mNotificationTypeMap.get(NOTIFICATION_EMERGENCY_NETWORK);
                if (t != null) {
                    cancelNotification(t);
                }
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "dismissEmergencyCallingNotification", e);
            }
        }
    }
}
